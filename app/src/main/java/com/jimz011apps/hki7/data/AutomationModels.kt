package com.jimz011apps.hki7.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A native Home Assistant automation plus whether it is safe to save through the config API. */
data class HAAutomationDocument(
    val id: String?,
    val entityId: String?,
    val config: JsonObject,
    val editable: Boolean
)

fun registeredAutomationEntityId(
    configId: String,
    registry: List<HAEntityRegistryEntry>
): String? = registry.firstOrNull { entry ->
    entry.entity_id.startsWith("automation.") &&
        entry.unique_id == configId
}?.entity_id

/** Resolves a saved config id to a live entity across HA versions. Newer versions expose `id` in
 * state attributes; older versions may only expose the same id as the registry unique id. */
fun loadedAutomationEntityId(
    configId: String,
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): String? {
    entities.firstOrNull { entity ->
        entity.entity_id.startsWith("automation.") &&
            entity.attributes?.get("id")?.jsonPrimitive?.contentOrNull == configId
    }?.let { return it.entity_id }
    val registeredId = registeredAutomationEntityId(configId, registry) ?: return null
    return registeredId.takeIf { id -> entities.any { it.entity_id == id } }
}

/** Combines live automation states with entity-registry records. Home Assistant keeps registry
 * records for disabled or not-currently-loaded automations, which makes this the only supported
 * way to make those entries discoverable without parsing HA's filesystem from the app. */
fun automationsIncludingRegistry(
    liveEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): List<HAEntity> {
    val liveAutomations = liveEntities.filter { it.entity_id.startsWith("automation.") }
    val liveIds = liveAutomations.mapTo(mutableSetOf()) { it.entity_id }
    val registeredOnly = registry.asSequence()
        .filter { it.entity_id.startsWith("automation.") && it.entity_id !in liveIds }
        .map { entry ->
            val fallbackName = entry.entity_id.substringAfter('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            HAEntity(
                entity_id = entry.entity_id,
                state = "unavailable",
                attributes = buildJsonObject {
                    put("friendly_name", fallbackName)
                    entry.unique_id?.takeIf(String::isNotBlank)?.let { put("id", it) }
                }
            )
        }
        .toList()
    return liveAutomations + registeredOnly
}

enum class AutomationSection(
    val pluralKey: String,
    val legacyKey: String,
    val title: String
) {
    TRIGGER("triggers", "trigger", "When"),
    CONDITION("conditions", "condition", "And if"),
    ACTION("actions", "action", "Then")
}

enum class AutomationRecipe(val title: String, val description: String) {
    BLANK("Start empty", "Build an automation from an empty canvas"),
    ENTITY_STATE("Entity changes", "Run an action when an entity reaches a state"),
    SCHEDULE("At a time", "Run an action at a fixed time every day"),
    SUNSET("Around sunset", "Run an action at sunset"),
    MOTION_LIGHTS("Motion lighting", "Turn on a light when motion is detected"),
    SUNRISE_LIGHTS_OFF("Lights off at sunrise", "Turn off a light when the sun rises"),
    ARRIVE_HOME("Welcome home", "Activate a scene when someone arrives home"),
    LEAVE_HOME("Leaving home", "Turn off a light when someone leaves home"),
    BEDTIME("Bedtime lights", "Turn off a light at a chosen bedtime"),
    MORNING_SCENE("Morning scene", "Activate a scene at a chosen morning time"),
    LOCK_AT_NIGHT("Lock up at night", "Lock a door at a chosen time"),
    OPEN_COVERS_AT_SUNRISE("Open covers at sunrise", "Open a cover when the sun rises"),
    CLOSE_COVERS_AT_SUNSET("Close covers at sunset", "Close a cover when the sun sets")
}

fun automationItems(config: JsonObject, section: AutomationSection): List<JsonObject> {
    return automationElements(config, section).filterIsInstance<JsonObject>()
}

fun automationElements(config: JsonObject, section: AutomationSection): List<JsonElement> {
    val value = config[section.pluralKey] ?: config[section.legacyKey] ?: return emptyList()
    return (value as? JsonArray)?.toList() ?: listOf(value)
}

/** Payload shape required by Home Assistant's current validate_config WebSocket command. */
fun automationValidationPayload(config: JsonObject): Map<String, JsonElement> =
    AutomationSection.entries.associate { section ->
        section.pluralKey to JsonArray(automationElements(config, section))
    }

/** Replaces one editor section while retaining every unrelated/unknown top-level config key. */
fun withAutomationItems(
    config: JsonObject,
    section: AutomationSection,
    items: List<JsonObject>
): JsonObject {
    val updated = config.toMutableMap()
    updated.remove(section.legacyKey)
    // The visual editor only manipulates object blocks. Preserve valid shorthand primitives in
    // their original relative position so editing a neighboring block can never erase HA config.
    val replacement = items.iterator()
    val merged = buildList {
        automationElements(config, section).forEach { original ->
            if (original is JsonObject) {
                if (replacement.hasNext()) add(replacement.next())
            } else {
                add(original)
            }
        }
        while (replacement.hasNext()) add(replacement.next())
    }
    updated[section.pluralKey] = JsonArray(merged)
    return JsonObject(updated)
}

fun withAutomationText(config: JsonObject, key: String, value: String): JsonObject {
    val updated = config.toMutableMap()
    if (value.isBlank() && key == "description") updated.remove(key)
    else updated[key] = JsonPrimitive(value)
    return JsonObject(updated)
}

fun automationBlockKind(section: AutomationSection, block: JsonObject): String? = when (section) {
    AutomationSection.TRIGGER ->
        (block["trigger"] as? JsonPrimitive)?.contentOrNull
            ?: (block["platform"] as? JsonPrimitive)?.contentOrNull
    AutomationSection.CONDITION -> (block["condition"] as? JsonPrimitive)?.contentOrNull
    AutomationSection.ACTION -> when {
        block.containsKey("action") || block.containsKey("service") -> "action"
        else -> null
    }
}

fun isSupportedAutomationBlock(section: AutomationSection, block: JsonObject): Boolean =
    automationBlockKind(section, block) in when (section) {
        AutomationSection.TRIGGER -> setOf("state", "time", "sun")
        AutomationSection.CONDITION -> setOf("state", "time")
        AutomationSection.ACTION -> setOf("action")
    }

fun automationBlockSummary(section: AutomationSection, block: JsonObject): String {
    fun text(key: String): String? = (block[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    fun entityText(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .joinToString().takeIf { it.isNotBlank() }
        else -> null
    }
    return when (section to automationBlockKind(section, block)) {
        AutomationSection.TRIGGER to "state" -> {
            val state = text("to")?.let { " becomes $it" }.orEmpty()
            "${text("entity_id") ?: "Choose an entity"}$state"
        }
        AutomationSection.TRIGGER to "time" -> "At ${text("at") ?: "a time"}"
        AutomationSection.TRIGGER to "sun" -> text("event")?.replaceFirstChar { it.uppercase() } ?: "Sun event"
        AutomationSection.CONDITION to "state" ->
            "${text("entity_id") ?: "Choose an entity"} is ${text("state") ?: "a state"}"
        AutomationSection.CONDITION to "time" ->
            listOfNotNull(text("after")?.let { "after $it" }, text("before")?.let { "before $it" })
                .joinToString(" and ").ifBlank { "Within a time window" }
        AutomationSection.ACTION to "action" -> {
            val action = text("action") ?: text("service") ?: "Choose an action"
            val target = (block["target"] as? JsonObject)
                ?.get("entity_id")?.let(::entityText)
                ?: text("entity_id")
            if (target == null) action else "$action → $target"
        }
        else -> "Advanced Home Assistant block (kept unchanged)"
    }
}

fun newAutomationBlock(section: AutomationSection, kind: String): JsonObject = buildJsonObject {
    when (section) {
        AutomationSection.TRIGGER -> {
            put("trigger", kind)
            when (kind) {
                "state" -> put("entity_id", "")
                "time" -> put("at", "08:00:00")
                "sun" -> put("event", "sunset")
            }
        }
        AutomationSection.CONDITION -> {
            put("condition", kind)
            when (kind) {
                "state" -> {
                    put("entity_id", "")
                    put("state", "on")
                }
                "time" -> {
                    put("after", "08:00:00")
                    put("before", "22:00:00")
                }
            }
        }
        AutomationSection.ACTION -> {
            put("action", "")
        }
    }
}

fun newAutomationConfig(recipe: AutomationRecipe): JsonObject {
    val trigger = when (recipe) {
        AutomationRecipe.BLANK -> null
        AutomationRecipe.ENTITY_STATE -> newAutomationBlock(AutomationSection.TRIGGER, "state")
        AutomationRecipe.SCHEDULE -> newAutomationBlock(AutomationSection.TRIGGER, "time")
        AutomationRecipe.SUNSET,
        AutomationRecipe.CLOSE_COVERS_AT_SUNSET ->
            newAutomationBlock(AutomationSection.TRIGGER, "sun").withString("event", "sunset")
        AutomationRecipe.SUNRISE_LIGHTS_OFF,
        AutomationRecipe.OPEN_COVERS_AT_SUNRISE ->
            newAutomationBlock(AutomationSection.TRIGGER, "sun").withString("event", "sunrise")
        AutomationRecipe.MOTION_LIGHTS ->
            newAutomationBlock(AutomationSection.TRIGGER, "state").withString("to", "on")
        AutomationRecipe.ARRIVE_HOME ->
            newAutomationBlock(AutomationSection.TRIGGER, "state").withString("to", "home")
        AutomationRecipe.LEAVE_HOME ->
            newAutomationBlock(AutomationSection.TRIGGER, "state").withString("to", "not_home")
        AutomationRecipe.BEDTIME,
        AutomationRecipe.LOCK_AT_NIGHT ->
            newAutomationBlock(AutomationSection.TRIGGER, "time").withString("at", "22:00:00")
        AutomationRecipe.MORNING_SCENE ->
            newAutomationBlock(AutomationSection.TRIGGER, "time").withString("at", "07:00:00")
    }
    val action = when (recipe) {
        AutomationRecipe.BLANK -> null
        AutomationRecipe.SUNRISE_LIGHTS_OFF,
        AutomationRecipe.LEAVE_HOME,
        AutomationRecipe.BEDTIME -> recipeAction("light.turn_off")
        AutomationRecipe.ARRIVE_HOME,
        AutomationRecipe.MORNING_SCENE -> recipeAction("scene.turn_on")
        AutomationRecipe.LOCK_AT_NIGHT -> recipeAction("lock.lock")
        AutomationRecipe.OPEN_COVERS_AT_SUNRISE -> recipeAction("cover.open_cover")
        AutomationRecipe.CLOSE_COVERS_AT_SUNSET -> recipeAction("cover.close_cover")
        else -> recipeAction("light.turn_on")
    }
    return buildJsonObject {
        put("alias", recipe.title)
        put("description", "Created with HKI7 Flows")
        put("triggers", buildJsonArray { trigger?.let(::add) })
        put("conditions", buildJsonArray { })
        put("actions", buildJsonArray { action?.let(::add) })
        put("mode", "single")
    }
}

private fun recipeAction(action: String): JsonObject =
    newAutomationBlock(AutomationSection.ACTION, "action").withString("action", action)

fun JsonObject.stringValue(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

fun JsonObject.withString(key: String, value: String): JsonObject {
    val updated = toMutableMap()
    if (value.isBlank()) updated.remove(key) else updated[key] = JsonPrimitive(value)
    return JsonObject(updated)
}

fun JsonObject.withElement(key: String, value: JsonElement): JsonObject =
    JsonObject(toMutableMap().apply { put(key, value) })

fun JsonObject.without(vararg keys: String): JsonObject =
    JsonObject(toMutableMap().apply { keys.forEach(::remove) })

/** Friendly state choices for the visual state trigger/condition editor. The entity's live state
 * and integration-provided select/HVAC options are always included. */
fun suggestedAutomationStates(entity: HAEntity?): List<String> {
    if (entity == null) return emptyList()
    val domain = entity.entity_id.substringBefore('.')
    val integrationOptions = when (domain) {
        "select", "input_select" -> (entity.attributes?.get("options") as? JsonArray)
        "climate" -> (entity.attributes?.get("hvac_modes") as? JsonArray)
        else -> null
    }?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
    val domainOptions = when (domain) {
        "light", "switch", "fan", "input_boolean", "automation", "script", "remote" -> listOf("on", "off")
        "binary_sensor" -> listOf("on", "off")
        "cover" -> listOf("open", "closed", "opening", "closing")
        "lock" -> listOf("locked", "unlocked", "locking", "unlocking", "jammed")
        "person", "device_tracker" -> listOf("home", "not_home")
        "media_player" -> listOf("playing", "paused", "idle", "standby", "on", "off")
        "alarm_control_panel" -> listOf("disarmed", "armed_home", "armed_away", "armed_night", "triggered")
        "vacuum" -> listOf("cleaning", "docked", "returning", "paused", "idle", "error")
        "sun" -> listOf("above_horizon", "below_horizon")
        else -> emptyList()
    }
    return (integrationOptions + domainOptions + entity.state)
        .filter { it.isNotBlank() && it !in setOf("unknown", "unavailable") }
        .distinct()
}

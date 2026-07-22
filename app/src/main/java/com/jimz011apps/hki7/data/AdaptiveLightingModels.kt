package com.jimz011apps.hki7.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private fun adaptiveLightingEntityIds(value: JsonElement?): Set<String> = when (value) {
    is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
    is JsonPrimitive -> setOfNotNull(value.contentOrNull?.takeIf(String::isNotBlank))
    else -> emptySet()
}

/** Extract the HA-owned light membership from an Adaptive Lighting options-flow form. */
fun adaptiveLightingLightsFromOptionsForm(form: JsonObject): Set<String>? {
    val schema = form["data_schema"] as? JsonArray ?: return null
    val lightsField = schema.asSequence()
        .mapNotNull { it as? JsonObject }
        .firstOrNull { (it["name"] as? JsonPrimitive)?.contentOrNull == "lights" }
        ?: return null
    return adaptiveLightingEntityIds(lightsField["suggested_value"] ?: lightsField["default"])
}

/** Resolve each installed profile to its current HA-owned member list. Options-flow values take
 * precedence because the integration may not repeat its complete configuration in live state. */
fun adaptiveLightingProfileMembers(
    registry: List<HAEntityRegistryEntry>,
    entitiesById: Map<String, HAEntity>,
    optionsForms: Map<String, JsonObject>
): Map<String, Set<String>> {
    val entryIds = registry.asSequence()
        .filter { it.platform == "adaptive_lighting" && it.disabled_by == null }
        .mapNotNull(HAEntityRegistryEntry::config_entry_id)
        .distinct()
        .toList()
    return entryIds.associateWith { entryId ->
        optionsForms[entryId]?.let(::adaptiveLightingLightsFromOptionsForm)
            ?: registry.asSequence()
                .filter { it.config_entry_id == entryId }
                .mapNotNull { entitiesById[it.entity_id] }
                .mapNotNull { entity ->
                    (entity.attributes?.get("configuration") as? JsonObject)?.get("lights")
                }
                .map(::adaptiveLightingEntityIds)
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
    }
}

/** Expand light groups recursively while retaining the group ids themselves. */
fun expandedLightEntityIds(
    entityIds: Collection<String>,
    entitiesById: Map<String, HAEntity>
): Set<String> = buildSet {
    val pending = ArrayDeque(entityIds)
    while (pending.isNotEmpty()) {
        val id = pending.removeFirst()
        if (!add(id)) continue
        entitiesById[id]?.childEntityIds.orEmpty().forEach(pending::addLast)
    }
}

/** Find only profiles that actually contain a light assigned to [areaId]. */
fun adaptiveLightingProfileIdsForArea(
    areaId: String,
    profileMembers: Map<String, Set<String>>,
    areaByEntity: Map<String, String?>,
    entitiesById: Map<String, HAEntity>
): List<String> = profileMembers.asSequence()
    .filter { (_, memberIds) -> memberIds.isNotEmpty() }
    .filter { (_, memberIds) ->
        expandedLightEntityIds(memberIds, entitiesById).any { entityId ->
            areaByEntity[entityId] == areaId
        }
    }
    .map(Map.Entry<String, Set<String>>::key)
    .sorted()
    .toList()

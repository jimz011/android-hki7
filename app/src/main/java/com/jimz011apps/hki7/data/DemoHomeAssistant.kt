@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Offline demo mode (required so Google Play reviewers can explore the app without a Home
 * Assistant server). Entering the demo stores [DEMO_SERVER_URL] as the server, which makes the
 * view model build a [DemoHomeAssistantClient] instead of a networked client — everything else
 * (polling, realtime updates, auto dashboard generation, service calls) runs unchanged against
 * the in-memory sample home below.
 */
// ".invalid" is an RFC-reserved TLD: anything that accidentally treats this as a real server
// fails fast in DNS instead of reaching an actual host.
const val DEMO_SERVER_URL = "https://demo-home.hki7.invalid"
const val DEMO_ACCESS_TOKEN = "hki7-demo"
private const val DEMO_USER_ID = "hki7demo0000000000000000000000ab"

fun isDemoServerUrl(url: String?): Boolean =
    url?.trim()?.trimEnd('/').equals(DEMO_SERVER_URL, ignoreCase = true)

class DemoHomeAssistantClient : HomeAssistantClient(DEMO_SERVER_URL, DEMO_ACCESS_TOKEN) {

    private val lock = Any()
    private val states = LinkedHashMap<String, HAEntity>().apply {
        demoEntities().forEach { put(it.entity_id, it) }
    }
    private val changes = MutableSharedFlow<HAStateChange>(extraBufferCapacity = 256)
    private val demoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Gentle background drift so the demo feels live: playback advances and sensors wander.
        demoScope.launch {
            while (isActive) {
                delay(20.seconds)
                advanceAmbientState()
            }
        }
    }

    private fun nowIso(): String = OffsetDateTime.now(ZoneOffset.UTC).toString()

    private fun snapshot(): List<HAEntity> = synchronized(lock) { states.values.toList() }

    private fun update(entityId: String, transform: (HAEntity) -> HAEntity?) {
        val updated = synchronized(lock) {
            val current = states[entityId] ?: return
            val next = transform(current) ?: return
            val stamped = next.copy(last_changed = nowIso())
            states[entityId] = stamped
            stamped
        }
        changes.tryEmit(HAStateChange(entityId, updated))
    }

    private fun HAEntity.withAttributes(vararg pairs: Pair<String, JsonElement?>): HAEntity {
        val merged = attributes?.toMutableMap() ?: mutableMapOf()
        pairs.forEach { (key, value) -> if (value == null) merged.remove(key) else merged[key] = value }
        return copy(attributes = JsonObject(merged))
    }

    // ── Read APIs ────────────────────────────────────────────────────────────

    override suspend fun getEntities(): List<HAEntity> = snapshot()

    override suspend fun getCoreState(): HACoreState = HACoreState.RUNNING

    override suspend fun checkConnection() = Unit

    override fun isConnectedViaLocalAddress(): Boolean = true

    override suspend fun getAreas(): List<HAArea> = demoAreas()

    override suspend fun getFloors(): List<HAFloor> = demoFloors()

    override suspend fun getEntityRegistry(): List<HAEntityRegistryEntry> = demoEntityRegistry()

    override suspend fun getDeviceRegistry(): List<HADeviceRegistryEntry> = demoDeviceRegistry()

    override suspend fun getConfigEntries(domain: String?): List<HAConfigEntry> = emptyList()

    override suspend fun getEnergyPreferences(): JsonObject? = null

    override suspend fun getEnergySolarForecasts(): JsonObject? = null

    override suspend fun browseMedia(entityId: String, contentId: String?, contentType: String?): HAMediaBrowseItem? = null

    override suspend fun getActionDefinitions(): List<HAActionDefinition> = emptyList()

    override suspend fun getCurrentUserName(): String = "Demo"

    override suspend fun getCurrentUser(): HAUser = HAUser(DEMO_USER_ID, "Demo", "demo", true)

    override suspend fun getUsers(): List<HAUser> = listOf(HAUser(DEMO_USER_ID, "Demo", "demo", true))

    override suspend fun getWeatherForecast(entityId: String, type: String): List<HAWeatherForecast> =
        demoForecast(type)

    override suspend fun getCalendarEvents(
        entityIds: List<String>,
        startMillis: Long,
        endMillis: Long
    ): Map<String, List<HACalendarEvent>> = entityIds.distinct().associateWith { entityId ->
        if (entityId == "calendar.family") demoCalendarEvents(startMillis, endMillis) else emptyList()
    }

    override fun subscribeStateChanges(): Flow<HAStateChange> = changes

    override fun subscribePushNotifications(webhookId: String): Flow<JsonObject> = flow { awaitCancellation() }

    override fun closeSession() = Unit

    override fun dispose() {
        demoScope.cancel()
        super.dispose()
    }

    // ── History & statistics (synthesized, deterministic per entity) ────────

    override suspend fun getEntityHistory(entityId: String, hours: Long): List<HAHistoryEntry> {
        val entity = synchronized(lock) { states[entityId] } ?: return emptyList()
        val end = OffsetDateTime.now(ZoneOffset.UTC)
        val span = hours.coerceAtLeast(1)
        val numeric = entity.state.toDoubleOrNull()
        if (numeric != null) {
            val points = (span * 4).toInt().coerceIn(24, 96)
            val amplitude = max(abs(numeric) * 0.12, 0.8)
            val random = Random(entityId.hashCode())
            val offsets = List(points + 1) { (random.nextDouble() - 0.5) * amplitude * 0.5 }
            return (points downTo 0).map { index ->
                val at = end.minus(index * span * 60L / points, ChronoUnit.MINUTES)
                val wave = sin((points - index) / points.toDouble() * 2 * PI) * amplitude
                val value = if (index == 0) numeric else numeric + wave + offsets[index]
                HAHistoryEntry(
                    state = String.format(java.util.Locale.US, "%.1f", value),
                    last_changed = at.toString()
                )
            }
        }
        // Discrete states: a few alternating blocks that end in the current state.
        val other = when (entity.state) {
            "on" -> "off"; "off" -> "on"
            "open" -> "closed"; "closed" -> "open"
            "locked" -> "unlocked"; "unlocked" -> "locked"
            "home" -> "not_home"; "not_home" -> "home"
            "playing" -> "paused"; "paused" -> "playing"
            else -> return listOf(HAHistoryEntry(state = entity.state, last_changed = end.minusHours(span).toString()))
        }
        val blocks = 6
        return (blocks downTo 0).map { index ->
            HAHistoryEntry(
                state = if (index % 2 == 0) entity.state else other,
                last_changed = end.minus(index * span * 60L / (blocks + 1), ChronoUnit.MINUTES).toString(),
                actorName = if (index % 2 == 0) "Demo" else null
            )
        }
    }

    override suspend fun getStatistics(
        statisticIds: List<String>,
        startMillis: Long,
        period: String,
        endMillis: Long?
    ): Map<String, List<HAStatPoint>> {
        val stepMs = when (period) {
            "hour" -> 3_600_000L
            "day" -> 86_400_000L
            else -> 86_400_000L * 30
        }
        val end = endMillis ?: System.currentTimeMillis()
        return statisticIds.associateWith { id ->
            val solar = id.contains("solar")
            val points = mutableListOf<HAStatPoint>()
            var bucket = (startMillis / stepMs) * stepMs
            while (bucket < end && points.size < 800) {
                val random = Random(id.hashCode().toLong() * 31 + bucket / stepMs)
                val hourOfDay = ((bucket / 3_600_000L) % 24).toInt()
                val activity = when {
                    solar -> (sin((hourOfDay - 6) / 12.0 * PI).coerceAtLeast(0.0))
                    period == "hour" -> 0.35 + 0.65 * dailyUsageCurve(hourOfDay)
                    else -> 0.8 + random.nextDouble() * 0.4
                }
                val change = when (period) {
                    "hour" -> 0.15 + activity * 0.55
                    "day" -> 6.5 + activity * 4.5
                    else -> 210.0 + activity * 60.0
                } * (0.9 + random.nextDouble() * 0.2)
                val mean = (90.0 + activity * 340.0) * (0.9 + random.nextDouble() * 0.2)
                points += HAStatPoint(
                    startMs = bucket,
                    mean = mean.toFloat(),
                    change = if (solar && activity == 0.0) 0f else change.toFloat()
                )
                bucket += stepMs
            }
            points
        }
    }

    private fun dailyUsageCurve(hour: Int): Double = when (hour) {
        in 0..5 -> 0.15
        in 6..8 -> 0.65
        in 9..16 -> 0.4
        in 17..21 -> 1.0
        else -> 0.35
    }

    // ── Service calls (mutate the in-memory home) ────────────────────────────

    override suspend fun toggleEntity(entityId: String) {
        applyService(entityId.substringBefore('.'), "toggle", HAServiceCall(entity_id = entityId))
    }

    override suspend fun callService(domain: String, service: String, serviceCall: HAServiceCall) {
        applyService(domain, service, serviceCall)
    }

    override suspend fun callServiceRaw(domain: String, service: String, payload: JsonObject) {
        val targets = when (val idElement = payload["entity_id"]) {
            is JsonPrimitive -> listOfNotNull(idElement.contentOrNull)
            is JsonArray -> idElement.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            else -> emptyList()
        }
        targets.forEach { target ->
            applyService(
                domain,
                service,
                HAServiceCall(
                    entity_id = target,
                    brightness = payload["brightness"]?.jsonPrimitive?.intOrNull,
                    temperature = payload["temperature"]?.jsonPrimitive?.doubleOrNull?.toFloat(),
                    position = payload["position"]?.jsonPrimitive?.intOrNull,
                    option = payload["option"]?.jsonPrimitive?.contentOrNull,
                    value = payload["value"]?.jsonPrimitive?.contentOrNull
                )
            )
        }
    }

    private fun applyService(domain: String, service: String, call: HAServiceCall) {
        val entityId = call.entity_id
        when (domain) {
            "light" -> applyLightService(service, call)
            "switch", "input_boolean", "siren" -> applyOnOff(entityId, service)
            "fan" -> applyFanService(service, call)
            "climate" -> applyClimateService(service, call)
            "cover" -> applyCoverService(service, call)
            "lock" -> update(entityId) { it.copy(state = if (service == "unlock" || service == "open") "unlocked" else "locked") }
            "alarm_control_panel" -> update(entityId) {
                it.copy(
                    state = when (service) {
                        "alarm_disarm" -> "disarmed"
                        "alarm_arm_home" -> "armed_home"
                        "alarm_arm_away" -> "armed_away"
                        "alarm_arm_night" -> "armed_night"
                        "alarm_arm_vacation" -> "armed_vacation"
                        "alarm_arm_custom_bypass" -> "armed_custom_bypass"
                        "alarm_trigger" -> "triggered"
                        else -> it.state
                    }
                )
            }
            "media_player" -> applyMediaPlayerService(service, call)
            "vacuum" -> update(entityId) {
                it.copy(
                    state = when (service) {
                        "start" -> "cleaning"
                        "pause" -> "paused"
                        "stop" -> "idle"
                        "return_to_base" -> "docked"
                        else -> it.state
                    }
                )
            }
            "humidifier" -> when (service) {
                "turn_on" -> applyOnOff(entityId, "turn_on")
                "turn_off" -> applyOnOff(entityId, "turn_off")
                "toggle" -> applyOnOff(entityId, "toggle")
                "set_humidity" -> update(entityId) { it.withAttributes("humidity" to call.humidity?.let(::JsonPrimitive)) }
                "set_mode" -> update(entityId) { it.withAttributes("mode" to call.mode?.let(::JsonPrimitive)) }
            }
            "scene" -> applyScene(entityId)
            "script", "automation" -> update(entityId) { it.withAttributes("last_triggered" to JsonPrimitive(nowIso())) }
            "button", "input_button" -> update(entityId) { it.copy(state = nowIso()) }
            "number", "input_number" -> call.value?.let { value -> update(entityId) { it.copy(state = value) } }
            "select", "input_select" -> call.option?.let { option -> update(entityId) { it.copy(state = option) } }
        }
    }

    private fun applyOnOff(entityId: String, service: String) {
        update(entityId) { entity ->
            entity.copy(
                state = when (service) {
                    "turn_on" -> "on"
                    "turn_off" -> "off"
                    else -> if (entity.state == "on") "off" else "on"
                }
            )
        }
    }

    private fun applyLightService(service: String, call: HAServiceCall) {
        update(call.entity_id) { entity ->
            val turningOn = service == "turn_on" || (service == "toggle" && entity.state != "on")
            if (!turningOn) {
                entity.copy(state = "off").withAttributes("brightness" to null)
            } else {
                val brightness = call.brightness ?: entity.brightness ?: 230
                entity.copy(state = "on").withAttributes(
                    "brightness" to JsonPrimitive(brightness.coerceIn(1, 255)),
                    "color_temp_kelvin" to (call.color_temp_kelvin?.let(::JsonPrimitive)
                        ?: entity.attributes?.get("color_temp_kelvin")),
                    "rgb_color" to (call.rgb_color?.let { rgb -> buildJsonArray { rgb.forEach { add(JsonPrimitive(it)) } } }
                        ?: entity.attributes?.get("rgb_color")),
                    "effect" to (call.effect?.let(::JsonPrimitive) ?: entity.attributes?.get("effect"))
                )
            }
        }
    }

    private fun applyFanService(service: String, call: HAServiceCall) {
        when (service) {
            "turn_on", "turn_off", "toggle" -> applyOnOff(call.entity_id, service)
            "set_percentage" -> update(call.entity_id) {
                val percentage = call.percentage ?: return@update null
                it.copy(state = if (percentage > 0) "on" else "off")
                    .withAttributes("percentage" to JsonPrimitive(percentage))
            }
            "set_preset_mode" -> update(call.entity_id) { it.withAttributes("preset_mode" to call.preset_mode?.let(::JsonPrimitive)) }
            "oscillate" -> update(call.entity_id) { it.withAttributes("oscillating" to call.oscillating?.let(::JsonPrimitive)) }
            "set_direction" -> update(call.entity_id) { it.withAttributes("direction" to call.direction?.let(::JsonPrimitive)) }
        }
    }

    private fun applyClimateService(service: String, call: HAServiceCall) {
        when (service) {
            "set_temperature" -> update(call.entity_id) {
                it.withAttributes("temperature" to call.temperature?.let { t -> JsonPrimitive((t * 2).roundToInt() / 2.0) })
            }
            "set_hvac_mode" -> update(call.entity_id) { entity ->
                call.hvac_mode?.let { entity.copy(state = it) }
            }
            "turn_off" -> update(call.entity_id) { it.copy(state = "off") }
            "turn_on" -> update(call.entity_id) { it.copy(state = "heat") }
            "set_fan_mode" -> update(call.entity_id) { it.withAttributes("fan_mode" to call.fan_mode?.let(::JsonPrimitive)) }
            "set_swing_mode" -> update(call.entity_id) { it.withAttributes("swing_mode" to call.swing_mode?.let(::JsonPrimitive)) }
        }
    }

    private fun applyCoverService(service: String, call: HAServiceCall) {
        update(call.entity_id) { entity ->
            when (service) {
                "open_cover" -> entity.copy(state = "open").withAttributes("current_position" to JsonPrimitive(100))
                "close_cover" -> entity.copy(state = "closed").withAttributes("current_position" to JsonPrimitive(0))
                "toggle" -> if (entity.state == "open") {
                    entity.copy(state = "closed").withAttributes("current_position" to JsonPrimitive(0))
                } else {
                    entity.copy(state = "open").withAttributes("current_position" to JsonPrimitive(100))
                }
                "set_cover_position" -> {
                    val position = (call.position ?: 0).coerceIn(0, 100)
                    entity.copy(state = if (position > 0) "open" else "closed")
                        .withAttributes("current_position" to JsonPrimitive(position))
                }
                "set_cover_tilt_position" -> entity.withAttributes(
                    "current_tilt_position" to JsonPrimitive((call.tilt_position ?: 0).coerceIn(0, 100))
                )
                "stop_cover" -> entity
                else -> entity
            }
        }
    }

    private fun applyMediaPlayerService(service: String, call: HAServiceCall) {
        when (service) {
            "media_play" -> update(call.entity_id) { it.copy(state = "playing").withAttributes("media_position_updated_at" to JsonPrimitive(nowIso())) }
            "media_pause" -> update(call.entity_id) { it.copy(state = "paused") }
            "media_play_pause" -> update(call.entity_id) {
                it.copy(state = if (it.state == "playing") "paused" else "playing")
                    .withAttributes("media_position_updated_at" to JsonPrimitive(nowIso()))
            }
            "media_stop", "turn_off" -> update(call.entity_id) { it.copy(state = "idle") }
            "turn_on" -> update(call.entity_id) { it.copy(state = "paused") }
            "volume_set" -> update(call.entity_id) { it.withAttributes("volume_level" to call.volume_level?.let(::JsonPrimitive)) }
            "volume_mute" -> update(call.entity_id) { it.withAttributes("is_volume_muted" to call.is_volume_muted?.let(::JsonPrimitive)) }
            "shuffle_set" -> update(call.entity_id) { it.withAttributes("shuffle" to call.shuffle?.let(::JsonPrimitive)) }
            "repeat_set" -> update(call.entity_id) { it.withAttributes("repeat" to call.repeat?.let(::JsonPrimitive)) }
            "select_source" -> update(call.entity_id) { it.withAttributes("source" to call.source?.let(::JsonPrimitive)) }
            "media_next_track" -> advanceTrack(call.entity_id, 1)
            "media_previous_track" -> advanceTrack(call.entity_id, -1)
            "media_seek" -> update(call.entity_id) {
                it.withAttributes(
                    "media_position" to call.seek_position?.let(::JsonPrimitive),
                    "media_position_updated_at" to JsonPrimitive(nowIso())
                )
            }
        }
    }

    private fun advanceTrack(entityId: String, step: Int) {
        update(entityId) { entity ->
            val currentTitle = entity.mediaTitle
            val index = demoPlaylist.indexOfFirst { it.first == currentTitle }
            val next = demoPlaylist[((index + step) % demoPlaylist.size + demoPlaylist.size) % demoPlaylist.size]
            entity.withAttributes(
                "media_title" to JsonPrimitive(next.first),
                "media_artist" to JsonPrimitive(next.second),
                "media_duration" to JsonPrimitive(next.third),
                "media_position" to JsonPrimitive(0),
                "media_position_updated_at" to JsonPrimitive(nowIso())
            )
        }
    }

    private fun applyScene(sceneId: String) {
        update(sceneId) { it.copy(state = nowIso()) }
        when (sceneId) {
            "scene.movie_time" -> {
                applyLightService("turn_on", HAServiceCall(entity_id = "light.living_room_ceiling", brightness = 36))
                applyLightService("turn_on", HAServiceCall(entity_id = "light.living_room_lamp", brightness = 60, rgb_color = listOf(255, 148, 82)))
                applyMediaPlayerService("media_play", HAServiceCall(entity_id = "media_player.living_room_tv"))
                applyCoverService("close_cover", HAServiceCall(entity_id = "cover.living_room_curtains"))
            }
            "scene.good_morning" -> {
                applyLightService("turn_on", HAServiceCall(entity_id = "light.bedroom_ceiling", brightness = 255))
                applyLightService("turn_on", HAServiceCall(entity_id = "light.kitchen_spots", brightness = 255))
                applyCoverService("open_cover", HAServiceCall(entity_id = "cover.bedroom_blinds"))
                applyCoverService("open_cover", HAServiceCall(entity_id = "cover.living_room_curtains"))
            }
            "scene.relax" -> {
                applyLightService("turn_on", HAServiceCall(entity_id = "light.living_room_lamp", brightness = 110, rgb_color = listOf(186, 122, 255)))
                applyLightService("turn_on", HAServiceCall(entity_id = "light.living_room_ceiling", brightness = 90))
            }
        }
    }

    /** Advances playback position and lets a few sensors wander so the dashboard feels alive. */
    private fun advanceAmbientState() {
        val random = Random(System.currentTimeMillis())
        listOf(
            "sensor.living_room_temperature", "sensor.kitchen_temperature", "sensor.office_temperature",
            "sensor.bedroom_temperature", "sensor.garden_temperature"
        ).forEach { id ->
            update(id) { entity ->
                val value = entity.state.toDoubleOrNull() ?: return@update null
                val next = (value + (random.nextDouble() - 0.5) * 0.2).coerceIn(value - 1.5, value + 1.5)
                entity.copy(state = String.format(java.util.Locale.US, "%.1f", next))
            }
        }
        update("sensor.home_power") { entity ->
            val value = entity.state.toDoubleOrNull() ?: return@update null
            val next = (value + (random.nextDouble() - 0.5) * 60).coerceIn(120.0, 900.0)
            entity.copy(state = next.roundToInt().toString())
        }
        update("media_player.living_room_tv") { entity ->
            if (entity.state != "playing") return@update null
            val duration = entity.mediaDuration ?: return@update null
            val position = ((entity.mediaPosition ?: 0.0) + 20.0)
            if (position >= duration) return@update null // ticker leaves track changes to the user
            entity.withAttributes(
                "media_position" to JsonPrimitive(position),
                "media_position_updated_at" to JsonPrimitive(nowIso())
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sample home dataset
// ─────────────────────────────────────────────────────────────────────────────

private val demoPlaylist = listOf(
    Triple("Golden Hour", "Aurora Fields", 214.0),
    Triple("Northern Lights", "Midnight Parade", 187.0),
    Triple("Slow Sunrise", "The Analog Garden", 243.0),
    Triple("Paper Planes at Dusk", "Cass & June", 201.0)
)

private fun demoAreas(): List<HAArea> = listOf(
    HAArea("living_room", "Living Room", icon = "mdi:sofa", floor_id = "ground_floor"),
    HAArea("kitchen", "Kitchen", icon = "mdi:countertop", floor_id = "ground_floor"),
    HAArea("office", "Office", icon = "mdi:desk", floor_id = "ground_floor"),
    HAArea("garden", "Garden", icon = "mdi:tree", floor_id = "ground_floor"),
    HAArea("bedroom", "Bedroom", icon = "mdi:bed", floor_id = "first_floor"),
    HAArea("bathroom", "Bathroom", icon = "mdi:shower", floor_id = "first_floor")
)

private fun demoFloors(): List<HAFloor> = listOf(
    HAFloor("ground_floor", "Ground Floor", level = 0, icon = "mdi:home-floor-0"),
    HAFloor("first_floor", "First Floor", level = 1, icon = "mdi:home-floor-1")
)

private class DemoDevice(val id: String, val areaId: String, val name: String, val model: String)

private val demoDevices = listOf(
    DemoDevice("demo_dev_living_hub", "living_room", "Living Room Multisensor", "Aeon MultiSensor 7"),
    DemoDevice("demo_dev_kitchen_hub", "kitchen", "Kitchen Multisensor", "Aeon MultiSensor 7"),
    DemoDevice("demo_dev_office_hub", "office", "Office Multisensor", "Aeon MultiSensor 7"),
    DemoDevice("demo_dev_bedroom_hub", "bedroom", "Bedroom Multisensor", "Aeon MultiSensor 7"),
    DemoDevice("demo_dev_garden_hub", "garden", "Garden Weather Station", "Ecowitt GW2000"),
    DemoDevice("demo_dev_front_door", "living_room", "Front Door Lock", "Nuki Smart Lock 4")
)

private fun demoDeviceRegistry(): List<HADeviceRegistryEntry> = demoDevices.map {
    HADeviceRegistryEntry(id = it.id, area_id = it.areaId, name = it.name, manufacturer = "HKI Demo", model = it.model)
}

/** entity id → (area id, owning demo device). House-wide entities map to null. */
private val demoEntityPlacement: Map<String, Pair<String?, String?>> = buildMap {
    fun place(entityId: String, areaId: String?, deviceId: String? = null) = put(entityId, areaId to deviceId)

    place("light.living_room_ceiling", "living_room")
    place("light.living_room_lamp", "living_room")
    place("media_player.living_room_tv", "living_room")
    place("climate.living_room", "living_room")
    place("cover.living_room_curtains", "living_room")
    place("sensor.living_room_temperature", "living_room", "demo_dev_living_hub")
    place("sensor.living_room_humidity", "living_room", "demo_dev_living_hub")
    place("binary_sensor.living_room_motion", "living_room", "demo_dev_living_hub")
    place("sensor.living_room_multisensor_battery", "living_room", "demo_dev_living_hub")
    place("vacuum.robot_vacuum", "living_room")

    place("light.kitchen_spots", "kitchen")
    place("switch.coffee_machine", "kitchen")
    place("media_player.kitchen_speaker", "kitchen")
    place("sensor.kitchen_temperature", "kitchen", "demo_dev_kitchen_hub")
    place("binary_sensor.kitchen_window", "kitchen", "demo_dev_kitchen_hub")
    place("sensor.kitchen_multisensor_battery", "kitchen", "demo_dev_kitchen_hub")

    place("light.office_desk_lamp", "office")
    place("switch.office_monitors", "office")
    place("sensor.office_temperature", "office", "demo_dev_office_hub")
    place("binary_sensor.office_motion", "office", "demo_dev_office_hub")
    place("sensor.office_multisensor_battery", "office", "demo_dev_office_hub")

    place("light.bedroom_ceiling", "bedroom")
    place("light.bedroom_nightstand", "bedroom")
    place("cover.bedroom_blinds", "bedroom")
    place("climate.bedroom", "bedroom")
    place("media_player.bedroom_speaker", "bedroom")
    place("sensor.bedroom_temperature", "bedroom", "demo_dev_bedroom_hub")
    place("sensor.bedroom_humidity", "bedroom", "demo_dev_bedroom_hub")
    place("sensor.bedroom_multisensor_battery", "bedroom", "demo_dev_bedroom_hub")

    place("light.bathroom_mirror", "bathroom")
    place("fan.bathroom_fan", "bathroom")
    place("sensor.bathroom_humidity", "bathroom")
    place("binary_sensor.bathroom_motion", "bathroom")

    place("light.garden_path", "garden")
    place("switch.garden_irrigation", "garden")
    place("sensor.garden_temperature", "garden", "demo_dev_garden_hub")
    place("sensor.garden_station_battery", "garden", "demo_dev_garden_hub")

    place("lock.front_door", "living_room", "demo_dev_front_door")
    place("binary_sensor.front_door", "living_room", "demo_dev_front_door")
    place("sensor.front_door_lock_battery", "living_room", "demo_dev_front_door")
}

private fun demoEntityRegistry(): List<HAEntityRegistryEntry> =
    demoEntities().map { entity ->
        val placement = demoEntityPlacement[entity.entity_id]
        HAEntityRegistryEntry(
            entity_id = entity.entity_id,
            area_id = placement?.first,
            device_id = placement?.second,
            platform = "hki_demo",
            unique_id = "demo_${entity.entity_id.replace('.', '_')}"
        )
    }

private fun demoEntities(): List<HAEntity> {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    fun iso(minutesAgo: Long) = now.minusMinutes(minutesAgo).toString()

    fun entity(id: String, state: String, minutesAgo: Long = 30, attributes: JsonObject? = null) =
        HAEntity(entity_id = id, state = state, attributes = attributes, last_changed = iso(minutesAgo))

    fun dimmableLight(id: String, name: String, on: Boolean, brightness: Int, kelvin: Int? = 3400) = entity(
        id, if (on) "on" else "off", if (on) 12 else 95,
        buildJsonObject {
            put("friendly_name", name)
            put("supported_color_modes", buildJsonArray { add(JsonPrimitive("color_temp")) })
            if (on) put("brightness", brightness)
            kelvin?.let {
                put("color_temp_kelvin", it)
                put("min_color_temp_kelvin", 2000)
                put("max_color_temp_kelvin", 6500)
            }
        }
    )

    fun colorLight(id: String, name: String, on: Boolean, brightness: Int, rgb: List<Int>) = entity(
        id, if (on) "on" else "off", if (on) 22 else 130,
        buildJsonObject {
            put("friendly_name", name)
            put("supported_color_modes", buildJsonArray { add(JsonPrimitive("rgb")); add(JsonPrimitive("color_temp")) })
            if (on) {
                put("brightness", brightness)
                put("rgb_color", buildJsonArray { rgb.forEach { add(JsonPrimitive(it)) } })
            }
            put("min_color_temp_kelvin", 2000)
            put("max_color_temp_kelvin", 6500)
            put("effect_list", buildJsonArray { add(JsonPrimitive("Colorloop")); add(JsonPrimitive("Candle")) })
        }
    )

    fun temperatureSensor(id: String, name: String, value: String) = entity(
        id, value, 7,
        buildJsonObject {
            put("friendly_name", name)
            put("device_class", "temperature")
            put("state_class", "measurement")
            put("unit_of_measurement", "°C")
        }
    )

    fun humiditySensor(id: String, name: String, value: String) = entity(
        id, value, 9,
        buildJsonObject {
            put("friendly_name", name)
            put("device_class", "humidity")
            put("state_class", "measurement")
            put("unit_of_measurement", "%")
        }
    )

    fun batterySensor(id: String, name: String, value: String) = entity(
        id, value, 55,
        buildJsonObject {
            put("friendly_name", name)
            put("device_class", "battery")
            put("state_class", "measurement")
            put("unit_of_measurement", "%")
        }
    )

    fun binary(id: String, name: String, deviceClass: String, on: Boolean, minutesAgo: Long) = entity(
        id, if (on) "on" else "off", minutesAgo,
        buildJsonObject {
            put("friendly_name", name)
            put("device_class", deviceClass)
        }
    )

    fun speaker(id: String, name: String, state: String) = entity(
        id, state, 45,
        buildJsonObject {
            put("friendly_name", name)
            put("supported_features", 319423)
            put("volume_level", 0.3)
            put("is_volume_muted", false)
        }
    )

    return listOf(
        // Living room
        dimmableLight("light.living_room_ceiling", "Ceiling Light", on = true, brightness = 200),
        colorLight("light.living_room_lamp", "Corner Lamp", on = true, brightness = 150, rgb = listOf(255, 170, 96)),
        entity(
            "media_player.living_room_tv", "playing", 18,
            buildJsonObject {
                put("friendly_name", "Living Room TV")
                put("supported_features", 319423)
                put("volume_level", 0.35)
                put("is_volume_muted", false)
                put("media_title", demoPlaylist[0].first)
                put("media_artist", demoPlaylist[0].second)
                put("media_duration", demoPlaylist[0].third)
                put("media_position", 74.0)
                put("media_position_updated_at", iso(0))
                put("app_name", "Music")
                put("shuffle", false)
                put("repeat", "off")
                put("source", "Streaming")
                put("source_list", buildJsonArray { add(JsonPrimitive("Streaming")); add(JsonPrimitive("HDMI 1")); add(JsonPrimitive("HDMI 2")) })
            }
        ),
        entity(
            "climate.living_room", "heat", 240,
            buildJsonObject {
                put("friendly_name", "Living Room Thermostat")
                put("hvac_modes", buildJsonArray { listOf("off", "heat", "cool", "auto").forEach { add(JsonPrimitive(it)) } })
                put("temperature", 21.0)
                put("current_temperature", 20.6)
                put("min_temp", 7.0)
                put("max_temp", 30.0)
                put("target_temp_step", 0.5)
                put("hvac_action", "heating")
            }
        ),
        entity(
            "cover.living_room_curtains", "open", 320,
            buildJsonObject {
                put("friendly_name", "Curtains")
                put("device_class", "curtain")
                put("current_position", 100)
                put("supported_features", 15)
            }
        ),
        temperatureSensor("sensor.living_room_temperature", "Living Room Temperature", "20.6"),
        humiditySensor("sensor.living_room_humidity", "Living Room Humidity", "48"),
        binary("binary_sensor.living_room_motion", "Living Room Motion", "motion", on = true, minutesAgo = 2),
        batterySensor("sensor.living_room_multisensor_battery", "Living Room Multisensor Battery", "84"),
        entity(
            "vacuum.robot_vacuum", "docked", 400,
            buildJsonObject {
                put("friendly_name", "Robot Vacuum")
                put("battery_level", 100)
                put("fan_speed", "standard")
                put("fan_speed_list", buildJsonArray { listOf("quiet", "standard", "turbo").forEach { add(JsonPrimitive(it)) } })
                put("supported_features", 16383)
            }
        ),

        // Kitchen
        dimmableLight("light.kitchen_spots", "Kitchen Spots", on = false, brightness = 255),
        entity("switch.coffee_machine", "on", 33, buildJsonObject { put("friendly_name", "Coffee Machine"); put("device_class", "outlet") }),
        speaker("media_player.kitchen_speaker", "Kitchen Speaker", "idle"),
        temperatureSensor("sensor.kitchen_temperature", "Kitchen Temperature", "21.8"),
        binary("binary_sensor.kitchen_window", "Kitchen Window", "window", on = false, minutesAgo = 540),
        batterySensor("sensor.kitchen_multisensor_battery", "Kitchen Multisensor Battery", "67"),

        // Office
        dimmableLight("light.office_desk_lamp", "Desk Lamp", on = true, brightness = 240, kelvin = 4300),
        entity("switch.office_monitors", "on", 65, buildJsonObject { put("friendly_name", "Monitors"); put("device_class", "switch") }),
        temperatureSensor("sensor.office_temperature", "Office Temperature", "22.4"),
        binary("binary_sensor.office_motion", "Office Motion", "motion", on = true, minutesAgo = 1),
        batterySensor("sensor.office_multisensor_battery", "Office Multisensor Battery", "19"),

        // Bedroom
        dimmableLight("light.bedroom_ceiling", "Bedroom Ceiling", on = false, brightness = 180),
        colorLight("light.bedroom_nightstand", "Nightstand Lamp", on = false, brightness = 90, rgb = listOf(255, 190, 120)),
        entity(
            "cover.bedroom_blinds", "closed", 700,
            buildJsonObject {
                put("friendly_name", "Bedroom Blinds")
                put("device_class", "blind")
                put("current_position", 0)
                put("supported_features", 15)
            }
        ),
        entity(
            "climate.bedroom", "off", 800,
            buildJsonObject {
                put("friendly_name", "Bedroom Thermostat")
                put("hvac_modes", buildJsonArray { listOf("off", "heat", "auto").forEach { add(JsonPrimitive(it)) } })
                put("temperature", 18.5)
                put("current_temperature", 18.9)
                put("min_temp", 7.0)
                put("max_temp", 28.0)
                put("target_temp_step", 0.5)
            }
        ),
        speaker("media_player.bedroom_speaker", "Bedroom Speaker", "paused"),
        temperatureSensor("sensor.bedroom_temperature", "Bedroom Temperature", "18.9"),
        humiditySensor("sensor.bedroom_humidity", "Bedroom Humidity", "52"),
        batterySensor("sensor.bedroom_multisensor_battery", "Bedroom Multisensor Battery", "58"),

        // Bathroom
        dimmableLight("light.bathroom_mirror", "Mirror Light", on = false, brightness = 255, kelvin = 5000),
        entity(
            "fan.bathroom_fan", "off", 200,
            buildJsonObject {
                put("friendly_name", "Bathroom Fan")
                put("percentage", 0)
                put("preset_modes", buildJsonArray { listOf("auto", "boost").forEach { add(JsonPrimitive(it)) } })
                put("supported_features", 57)
            }
        ),
        humiditySensor("sensor.bathroom_humidity", "Bathroom Humidity", "61"),
        binary("binary_sensor.bathroom_motion", "Bathroom Motion", "motion", on = false, minutesAgo = 75),

        // Garden
        dimmableLight("light.garden_path", "Path Lights", on = false, brightness = 220, kelvin = 2700),
        entity("switch.garden_irrigation", "off", 1300, buildJsonObject { put("friendly_name", "Irrigation"); put("device_class", "switch") }),
        temperatureSensor("sensor.garden_temperature", "Garden Temperature", "17.2"),
        batterySensor("sensor.garden_station_battery", "Garden Station Battery", "41"),

        // Front door & security
        entity("lock.front_door", "locked", 95, buildJsonObject { put("friendly_name", "Front Door") }),
        binary("binary_sensor.front_door", "Front Door", "door", on = false, minutesAgo = 95),
        batterySensor("sensor.front_door_lock_battery", "Front Door Lock Battery", "76"),
        entity(
            "alarm_control_panel.home", "disarmed", 480,
            buildJsonObject {
                put("friendly_name", "Home Alarm")
                put("supported_features", 63)
                put("code_arm_required", false)
            }
        ),

        // Whole home
        entity(
            "weather.home", "partlycloudy", 15,
            buildJsonObject {
                put("friendly_name", "Home")
                put("temperature", 19.5)
                put("humidity", 62.0)
                put("pressure", 1014.0)
                put("wind_speed", 11.2)
                put("forecast", buildJsonArray {
                    demoForecast("daily").forEach { day ->
                        add(buildJsonObject {
                            put("datetime", day.datetime)
                            put("condition", day.condition)
                            put("temperature", day.temperature)
                            put("templow", day.templow)
                            put("precipitation", day.precipitation)
                        })
                    }
                })
            }
        ),
        entity("sun.sun", "above_horizon", 250, buildJsonObject { put("friendly_name", "Sun"); put("elevation", 32.4) }),
        entity(
            "sensor.home_power", "412", 1,
            buildJsonObject {
                put("friendly_name", "Home Power")
                put("device_class", "power")
                put("state_class", "measurement")
                put("unit_of_measurement", "W")
            }
        ),
        entity(
            "sensor.home_energy", "8341.6", 1,
            buildJsonObject {
                put("friendly_name", "Home Energy")
                put("device_class", "energy")
                put("state_class", "total_increasing")
                put("unit_of_measurement", "kWh")
            }
        ),
        entity("person.alex", "home", 180, buildJsonObject { put("friendly_name", "Alex"); put("user_id", DEMO_USER_ID) }),
        entity("person.sam", "not_home", 60, buildJsonObject { put("friendly_name", "Sam") }),
        entity("calendar.family", "off", 300, buildJsonObject { put("friendly_name", "Family Calendar") }),
        entity("scene.movie_time", iso(2000), 2000, buildJsonObject { put("friendly_name", "Movie Time"); put("icon", "mdi:movie-open") }),
        entity("scene.good_morning", iso(600), 600, buildJsonObject { put("friendly_name", "Good Morning"); put("icon", "mdi:weather-sunset-up") }),
        entity("scene.relax", iso(3100), 3100, buildJsonObject { put("friendly_name", "Relax"); put("icon", "mdi:sofa-single") }),
        entity(
            "automation.goodnight_routine", "on", 950,
            buildJsonObject { put("friendly_name", "Goodnight Routine"); put("last_triggered", iso(950)) }
        ),
        entity("input_boolean.guest_mode", "off", 2600, buildJsonObject { put("friendly_name", "Guest Mode"); put("icon", "mdi:account-group") })
    )
}

private fun demoForecast(type: String): List<HAWeatherForecast> {
    val conditions = listOf("sunny", "partlycloudy", "cloudy", "rainy", "partlycloudy", "sunny", "cloudy")
    return if (type == "hourly") {
        val start = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
        (0 until 24).map { hour ->
            val at = start.plusHours(hour.toLong())
            HAWeatherForecast(
                datetime = at.toString(),
                condition = conditions[(hour / 4) % conditions.size],
                temperature = 15.0 + 6.0 * sin((at.hour - 5) / 24.0 * 2 * PI).coerceAtLeast(-0.4),
                precipitation = if (conditions[(hour / 4) % conditions.size] == "rainy") 0.8 else 0.0
            )
        }
    } else {
        val today = LocalDate.now()
        (0 until 7).map { day ->
            HAWeatherForecast(
                datetime = today.plusDays(day.toLong()).atStartOfDay().atOffset(ZoneOffset.UTC).toString(),
                condition = conditions[day % conditions.size],
                temperature = listOf(21.0, 19.5, 17.0, 15.5, 18.0, 22.5, 19.0)[day % 7],
                templow = listOf(12.0, 11.0, 9.5, 8.0, 10.0, 13.5, 11.0)[day % 7],
                precipitation = if (conditions[day % conditions.size] == "rainy") 4.2 else 0.0
            )
        }
    }
}

private fun demoCalendarEvents(startMillis: Long, endMillis: Long): List<HACalendarEvent> {
    val today = LocalDate.now()
    fun event(dayOffset: Long, startHour: Int, durationMinutes: Long, title: String, location: String? = null): HACalendarEvent {
        val start = today.plusDays(dayOffset).atTime(startHour, 0).atOffset(OffsetDateTime.now().offset)
        return HACalendarEvent(
            summary = title,
            start = HACalendarDateTime(dateTime = start.toString()),
            end = HACalendarDateTime(dateTime = start.plusMinutes(durationMinutes).toString()),
            location = location,
            entityId = "calendar.family"
        )
    }
    return listOf(
        event(0, 18, 90, "Family dinner"),
        event(1, 9, 60, "Yoga class", "Studio One"),
        event(2, 14, 45, "Project review call"),
        event(4, 10, 120, "Weekend market", "Town square")
    ).filter { candidate ->
        val start = candidate.start?.dateTime?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
        start != null && start in startMillis..endMillis
    }
}

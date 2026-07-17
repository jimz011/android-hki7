package com.example.hki7.ui

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAreaConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomStatusStateTest {
    @Test
    fun `discovery classifies only exact supported device classes in display order`() {
        val door = entity("binary_sensor.patio", deviceClass = "door")
        val garageDoor = entity("binary_sensor.garage", deviceClass = "garage_door")
        val window = entity("binary_sensor.window", deviceClass = "window")
        val motion = entity("binary_sensor.motion", deviceClass = "motion")
        val moving = entity("binary_sensor.moving", deviceClass = "moving")
        val vibration = entity("binary_sensor.vibration", deviceClass = "vibration")
        val occupancy = entity("binary_sensor.occupied", deviceClass = "occupancy")
        val presence = entity("binary_sensor.present", deviceClass = "presence")
        val light = entity("light.ceiling")
        val device = entity("switch.television")
        val smoke = entity("binary_sensor.smoke", deviceClass = "smoke")
        val gas = entity("binary_sensor.co", deviceClass = "carbon_monoxide")
        val fire = entity("binary_sensor.fire", deviceClass = "fire")
        val heat = entity("binary_sensor.heat", deviceClass = "heat")
        val safety = entity("binary_sensor.safety", deviceClass = "safety")

        val result = discoverRoomStatus(
            listOf(
                fire, light, door, garageDoor, device, window, motion, moving, vibration,
                occupancy, presence, smoke, gas, heat, safety, door
            )
        )

        assertEquals(RoomStatusRoles.ORDERED, result.entityIds.keys.toList())
        assertEquals(listOf(door.entity_id, garageDoor.entity_id), result.entityIds[RoomStatusRoles.DOORS])
        assertEquals(listOf(window.entity_id), result.entityIds[RoomStatusRoles.WINDOWS])
        assertEquals(listOf(motion.entity_id, moving.entity_id, vibration.entity_id), result.entityIds[RoomStatusRoles.MOTION])
        assertEquals(listOf(occupancy.entity_id, presence.entity_id), result.entityIds[RoomStatusRoles.PRESENCE])
        assertEquals(listOf(light.entity_id), result.entityIds[RoomStatusRoles.LIGHTS])
        assertEquals(listOf(device.entity_id), result.entityIds[RoomStatusRoles.DEVICES])
        assertEquals(listOf(smoke.entity_id), result.entityIds[RoomStatusRoles.SMOKE])
        assertEquals(listOf(gas.entity_id), result.entityIds[RoomStatusRoles.GAS])
        assertEquals(listOf(fire.entity_id, heat.entity_id, safety.entity_id), result.entityIds[RoomStatusRoles.FIRE])
    }

    @Test
    fun `discovery rejects generic openings mismatched domains and unsupported classes`() {
        val result = discoverRoomStatus(
            listOf(
                entity("fan.bedroom", state = "on"),
                entity("media_player.tv", state = "on"),
                entity("binary_sensor.generic_opening", state = "on", deviceClass = "opening"),
                entity("binary_sensor.battery", state = "on", deviceClass = "battery"),
                entity("sensor.named_door", state = "on", deviceClass = "door"),
                entity("cover.unclassified", state = "open"),
                entity("sensor.power", state = "12", deviceClass = "power")
            )
        )

        assertTrue(result.entityIds.isEmpty())
        assertTrue(result.temperatureEntityIds.isEmpty())
        assertTrue(result.humidityEntityIds.isEmpty())
    }

    @Test
    fun `discovery includes only semantically exact door and window covers`() {
        val door = entity("cover.door", deviceClass = "door")
        val garage = entity("cover.garage", deviceClass = "garage")
        val garageDoor = entity("cover.garage_door", deviceClass = "garage_door")
        val gate = entity("cover.gate", deviceClass = "gate")
        val window = entity("cover.skylight", deviceClass = "window")
        val shutter = entity("cover.shutter", deviceClass = "shutter")

        val result = discoverRoomStatus(listOf(door, garage, garageDoor, gate, window, shutter))

        assertEquals(
            listOf(door.entity_id, garage.entity_id, garageDoor.entity_id, gate.entity_id),
            result.entityIds[RoomStatusRoles.DOORS]
        )
        assertEquals(listOf(window.entity_id), result.entityIds[RoomStatusRoles.WINDOWS])
    }

    @Test
    fun `discovery excludes window sensors belonging to climate devices`() {
        val climateWindow = entity("binary_sensor.climate_open_window", deviceClass = "window")
        val climateWindowCover = entity("cover.climate_open_window", deviceClass = "window")
        val actualWindow = entity("binary_sensor.actual_window", deviceClass = "window")

        val result = discoverRoomStatus(
            listOf(climateWindow, climateWindowCover, actualWindow),
            excludedWindowEntityIds = setOf(climateWindow.entity_id, climateWindowCover.entity_id)
        )

        assertEquals(listOf(actualWindow.entity_id), result.entityIds[RoomStatusRoles.WINDOWS])
    }

    @Test
    fun `climate measurements win and discovery returns every climate with a current attribute`() {
        val firstClimate = entity(
            "climate.first",
            attributes = attributes(
                "current_temperature" to JsonPrimitive(21.25),
                "current_humidity" to JsonPrimitive(44)
            )
        )
        val secondClimate = entity(
            "climate.second",
            attributes = attributes(
                "current_temperature" to JsonPrimitive(22.75),
                "current_humidity" to JsonPrimitive(52)
            )
        )
        val temperatureSensor = entity("sensor.temperature", state = "30", deviceClass = "temperature")
        val humiditySensor = entity("sensor.humidity", state = "30", deviceClass = "humidity")
        val climateWithoutCurrentValues = entity("climate.empty")

        val result = discoverRoomStatus(
            listOf(temperatureSensor, firstClimate, humiditySensor, climateWithoutCurrentValues, secondClimate)
        )

        assertEquals(listOf(firstClimate.entity_id, secondClimate.entity_id), result.temperatureEntityIds)
        assertEquals(listOf(firstClimate.entity_id, secondClimate.entity_id), result.humidityEntityIds)
    }

    @Test
    fun `dedicated measurements are all returned only when climate readings are absent`() {
        val firstTemperature = entity("sensor.temperature_one", state = "20", deviceClass = "temperature")
        val unavailableTemperature = entity("sensor.temperature_two", state = "unavailable", deviceClass = "temperature")
        val firstHumidity = entity("sensor.humidity_one", state = "40", deviceClass = "humidity")
        val secondHumidity = entity("sensor.humidity_two", state = "50", deviceClass = "humidity")
        val similarlyNamed = entity("sensor.temperature_name_only", state = "99", deviceClass = "measurement")

        val result = discoverRoomStatus(
            listOf(firstTemperature, unavailableTemperature, firstHumidity, secondHumidity, similarlyNamed)
        )

        assertEquals(listOf(firstTemperature.entity_id, unavailableTemperature.entity_id), result.temperatureEntityIds)
        assertEquals(listOf(firstHumidity.entity_id, secondHumidity.entity_id), result.humidityEntityIds)
    }

    @Test
    fun `resolution emits active counts in fixed order and never double counts an id`() {
        val config = HKIAreaConfig(
            roomStatusEntityIds = mapOf(
                RoomStatusRoles.FIRE to listOf("binary_sensor.fire"),
                RoomStatusRoles.DOORS to listOf("binary_sensor.front", "binary_sensor.front", "binary_sensor.back"),
                RoomStatusRoles.LIGHTS to listOf("light.one", "light.two"),
                RoomStatusRoles.PRESENCE to listOf("binary_sensor.occupancy")
            )
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity("binary_sensor.front", "open"),
                entity("binary_sensor.back", "closed"),
                entity("light.one", "on"),
                entity("light.two", "off"),
                entity("binary_sensor.occupancy", "occupied"),
                entity("binary_sensor.fire", "detected")
            )
        )

        assertEquals(
            listOf(
                RoomStatusIndicator(RoomStatusRoles.DOORS, 1),
                RoomStatusIndicator(RoomStatusRoles.PRESENCE, 1),
                RoomStatusIndicator(RoomStatusRoles.LIGHTS, 1),
                RoomStatusIndicator(RoomStatusRoles.FIRE, 1)
            ),
            summary.indicators
        )
    }

    @Test
    fun `unknown unavailable and inactive states never produce indicators`() {
        val config = HKIAreaConfig(
            roomStatusEntityIds = RoomStatusRoles.ORDERED.associateWith { role -> listOf("binary_sensor.$role") }
        )
        val states = listOf("unknown", "unavailable", "off", "closed", "clear", "idle", "away", "not_home", "none")

        states.forEach { state ->
            val entities = RoomStatusRoles.ORDERED.map { role -> entity("binary_sensor.$role", state) }
            assertTrue("Expected no active indicators for $state", resolveRoomStatus(config, entities).indicators.isEmpty())
        }
    }

    @Test
    fun `openings support transition states and partially open covers`() {
        val config = HKIAreaConfig(
            roomStatusEntityIds = mapOf(
                RoomStatusRoles.DOORS to listOf("cover.door", "binary_sensor.opening"),
                RoomStatusRoles.WINDOWS to listOf("cover.window")
            )
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity(
                    "cover.door",
                    state = "closed",
                    attributes = attributes("current_position" to JsonPrimitive(37))
                ),
                entity("binary_sensor.opening", state = "closing"),
                entity(
                    "cover.window",
                    state = "closed",
                    attributes = attributes("current_position" to JsonPrimitive(0))
                )
            )
        )

        assertEquals(listOf(RoomStatusIndicator(RoomStatusRoles.DOORS, 2)), summary.indicators)
    }

    @Test
    fun `motion presence and hazards recognize Home Assistant detected vocabularies`() {
        val cases = listOf(
            Triple(RoomStatusRoles.MOTION, "vibrating", true),
            Triple(RoomStatusRoles.MOTION, "triggered", true),
            Triple(RoomStatusRoles.PRESENCE, "home", true),
            Triple(RoomStatusRoles.PRESENCE, "present", true),
            Triple(RoomStatusRoles.SMOKE, "alarm", true),
            Triple(RoomStatusRoles.GAS, "leak", true),
            Triple(RoomStatusRoles.FIRE, "hot", true),
            Triple(RoomStatusRoles.FIRE, "normal", false)
        )

        cases.forEachIndexed { index, (role, state, expected) ->
            val id = "binary_sensor.case_$index"
            val summary = resolveRoomStatus(
                HKIAreaConfig(roomStatusEntityIds = mapOf(role to listOf(id))),
                listOf(entity(id, state))
            )
            assertEquals("$role with state $state", expected, summary.indicators.isNotEmpty())
        }
    }

    @Test
    fun `lights and switches require exact on state`() {
        val config = HKIAreaConfig(
            roomStatusEntityIds = mapOf(
                RoomStatusRoles.LIGHTS to listOf("light.room", "light.fake"),
                RoomStatusRoles.DEVICES to listOf("switch.tv", "switch.starting")
            )
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity("light.room", "on"),
                entity("light.fake", "active"),
                entity("switch.tv", "ON"),
                entity("switch.starting", "opening")
            )
        )

        assertEquals(
            listOf(
                RoomStatusIndicator(RoomStatusRoles.LIGHTS, 1),
                RoomStatusIndicator(RoomStatusRoles.DEVICES, 1)
            ),
            summary.indicators
        )
    }

    @Test
    fun `multiple climate devices average current attributes and ignore their states`() {
        val config = HKIAreaConfig(
            roomTemperatureEntityIds = listOf("climate.first", "climate.second"),
            roomHumidityEntityIds = listOf("climate.first", "climate.second")
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity(
                    "climate.first",
                    state = "99",
                    attributes = attributes(
                        "current_temperature" to JsonPrimitive(21.25),
                        "current_humidity" to JsonPrimitive(44),
                        "temperature_unit" to JsonPrimitive("°C")
                    )
                ),
                entity(
                    "climate.second",
                    state = "1",
                    attributes = attributes(
                        "current_temperature" to JsonPrimitive(22.75),
                        "current_humidity" to JsonPrimitive(52),
                        "temperature_unit" to JsonPrimitive("°C")
                    )
                )
            )
        )

        assertEquals("22°C", summary.temperature)
        assertEquals("48%", summary.humidity)
        assertEquals("22°C / 48%", summary.environmentText)
    }

    @Test
    fun `multiple dedicated sensors average numeric states and ignore current attributes`() {
        val config = HKIAreaConfig(
            roomTemperatureEntityIds = listOf("sensor.temperature_one", "sensor.temperature_two"),
            roomHumidityEntityIds = listOf("sensor.humidity_one", "sensor.humidity_two")
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity(
                    "sensor.temperature_one",
                    state = "20",
                    attributes = attributes(
                        "current_temperature" to JsonPrimitive(90),
                        "unit_of_measurement" to JsonPrimitive("°C")
                    )
                ),
                entity("sensor.temperature_two", state = "21"),
                entity(
                    "sensor.humidity_one",
                    state = "40",
                    attributes = attributes("current_humidity" to JsonPrimitive(90))
                ),
                entity("sensor.humidity_two", state = "50")
            )
        )

        assertEquals("20.5°C", summary.temperature)
        assertEquals("45%", summary.humidity)
    }

    @Test
    fun `manually mixed source lists use only climate entities`() {
        val config = HKIAreaConfig(
            roomTemperatureEntityIds = listOf("sensor.temperature", "climate.room"),
            roomHumidityEntityIds = listOf("sensor.humidity", "climate.room")
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity("sensor.temperature", "30"),
                entity("sensor.humidity", "70"),
                entity(
                    "climate.room",
                    state = "heat",
                    attributes = attributes(
                        "current_temperature" to JsonPrimitive(20),
                        "current_humidity" to JsonPrimitive(40),
                        "temperature_unit" to JsonPrimitive("°C")
                    )
                )
            )
        )

        assertEquals("20°C", summary.temperature)
        assertEquals("40%", summary.humidity)
    }

    @Test
    fun `selected climate source prevents fallback when its live entity is missing`() {
        val summary = resolveRoomStatus(
            HKIAreaConfig(
                roomTemperatureEntityIds = listOf("sensor.temperature", "climate.missing")
            ),
            listOf(entity("sensor.temperature", "30"))
        )

        assertNull(summary.temperature)
    }

    @Test
    fun `legacy singular environment fields still resolve current values`() {
        val climate = entity(
            "climate.room",
            state = "not-a-reading",
            attributes = attributes(
                "current_temperature" to JsonPrimitive(19.0),
                "current_humidity" to JsonPrimitive(47),
                "temperature_unit" to JsonPrimitive("°F")
            )
        )
        val summary = resolveRoomStatus(
            HKIAreaConfig(
                roomTemperatureEntityId = climate.entity_id,
                roomHumidityEntityId = climate.entity_id
            ),
            listOf(climate)
        )

        assertEquals("19°F", summary.temperature)
        assertEquals("47%", summary.humidity)
    }

    @Test
    fun `plural environment fields override legacy singular fields`() {
        val summary = resolveRoomStatus(
            HKIAreaConfig(
                roomTemperatureEntityId = "sensor.legacy",
                roomTemperatureEntityIds = listOf("sensor.plural")
            ),
            listOf(
                entity("sensor.legacy", "99"),
                entity("sensor.plural", "21")
            )
        )

        assertEquals("21", summary.temperature)
    }

    @Test
    fun `invalid values are omitted from averages`() {
        val config = HKIAreaConfig(
            roomTemperatureEntityIds = listOf("sensor.good", "sensor.unavailable"),
            roomHumidityEntityIds = listOf("sensor.nan")
        )
        val summary = resolveRoomStatus(
            config,
            listOf(
                entity("sensor.good", "21"),
                entity("sensor.unavailable", "unavailable"),
                entity("sensor.nan", "NaN")
            )
        )

        assertEquals("21", summary.temperature)
        assertNull(summary.humidity)
    }

    @Test
    fun `room entity ids include every configured source once in stable order`() {
        val config = HKIAreaConfig(
            roomStatusEntityIds = linkedMapOf(
                RoomStatusRoles.LIGHTS to listOf("light.room", "shared.entity"),
                RoomStatusRoles.DOORS to listOf("binary_sensor.door", "shared.entity"),
                "future_role" to listOf("sensor.future")
            ),
            roomTemperatureEntityId = "sensor.legacy_temperature",
            roomTemperatureEntityIds = listOf("sensor.temperature", "shared.entity"),
            roomHumidityEntityId = "sensor.legacy_humidity",
            roomHumidityEntityIds = listOf("sensor.humidity", "shared.entity")
        )

        assertEquals(
            listOf(
                "binary_sensor.door",
                "shared.entity",
                "light.room",
                "sensor.future",
                "sensor.temperature",
                "sensor.humidity"
            ),
            config.roomEntityIds()
        )
    }

    @Test
    fun `new persisted fields have backward-compatible defaults`() {
        val config = Json.decodeFromString<HKIAreaConfig>("{}")

        assertTrue(config.roomStatusEntityIds.isEmpty())
        assertNull(config.roomTemperatureEntityId)
        assertNull(config.roomHumidityEntityId)
        assertTrue(config.roomTemperatureEntityIds.isEmpty())
        assertTrue(config.roomHumidityEntityIds.isEmpty())
        assertFalse(config.roomEntitiesCustomized)
    }

    private fun entity(
        id: String,
        state: String = "off",
        deviceClass: String? = null,
        attributes: JsonObject? = null
    ): HAEntity {
        val mergedAttributes = buildJsonObject {
            attributes?.forEach { (key, value) -> put(key, value) }
            deviceClass?.let { put("device_class", it) }
        }.takeIf { it.isNotEmpty() }
        return HAEntity(entity_id = id, state = state, attributes = mergedAttributes)
    }

    private fun attributes(vararg entries: Pair<String, JsonPrimitive>): JsonObject =
        buildJsonObject { entries.forEach { (key, value) -> put(key, value) } }
}

package com.example.hki7.ui.screens

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIClimateConfig
import com.example.hki7.data.HKISecurityConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClimateSceneActivityTest {
    @Test
    fun `climate scene counts live door and window openings`() {
        val entities = listOf(
            openingEntity("binary_sensor.front_door", "on", "door"),
            openingEntity("binary_sensor.back_door", "off", "door"),
            openingEntity("binary_sensor.office_window", "open", "window"),
            openingEntity("cover.bedroom_window", "closed", "window", currentPosition = 38),
            openingEntity("cover.offline_window", "unavailable", "window", currentPosition = 80)
        )
        val state = entities.climateOpeningState(
            HKISecurityConfig(
                extraEntityIds = mapOf(
                    "windows" to listOf("cover.bedroom_window", "cover.offline_window")
                )
            )
        )

        assertEquals(1, state.openDoors)
        assertEquals(2, state.openWindows)
    }

    @Test
    fun `climate openings respect manual security groups and hidden contacts`() {
        val hiddenDoor = openingEntity("binary_sensor.hidden_door", "on", "door")
        val manualDoor = openingEntity("cover.patio_entry", "open", null)
        val manualWindow = openingEntity("binary_sensor.custom_contact", "on", null)
        val config = HKISecurityConfig(
            extraEntityIds = mapOf(
                "doors" to listOf(manualDoor.entity_id),
                "windows" to listOf(manualWindow.entity_id)
            ),
            hiddenEntityIds = listOf(hiddenDoor.entity_id)
        )

        val state = listOf(hiddenDoor, manualDoor, manualWindow).climateOpeningState(config)

        assertEquals(1, state.openDoors)
        assertEquals(1, state.openWindows)
    }

    @Test
    fun `explicit cooling action animates cooling`() {
        assertEquals(
            ClimateSceneActivity.COOLING,
            climateEntity(state = "cool", action = "cooling").climateSceneActivity()
        )
    }

    @Test
    fun `explicit idle action overrides selected cool mode`() {
        assertNull(climateEntity(state = "cool", action = "idle").climateSceneActivity())
    }

    @Test
    fun `cool mode animates integrations without hvac action`() {
        assertEquals(
            ClimateSceneActivity.COOLING,
            climateEntity(state = "cool").climateSceneActivity()
        )
    }

    @Test
    fun `extended Home Assistant actions are represented`() {
        assertEquals(
            ClimateSceneActivity.HEATING,
            climateEntity(state = "heat", action = "preheating").climateSceneActivity()
        )
        assertEquals(
            ClimateSceneActivity.DRYING,
            climateEntity(state = "dry", action = "drying").climateSceneActivity()
        )
        assertEquals(
            ClimateSceneActivity.DEFROSTING,
            climateEntity(state = "heat", action = "defrosting").climateSceneActivity()
        )
    }

    @Test
    fun `custom integration hvac mode is used when state is generic`() {
        assertEquals(
            ClimateSceneActivity.COOLING,
            climateEntity(state = "on", hvacMode = "cool").climateSceneActivity()
        )
    }

    @Test
    fun `unknown custom action falls back to selected mode`() {
        assertEquals(
            ClimateSceneActivity.COOLING,
            climateEntity(state = "cool", action = "running").climateSceneActivity()
        )
    }

    @Test
    fun `unavailable entity ignores stale action`() {
        assertNull(climateEntity(state = "unavailable", action = "cooling").climateSceneActivity())
    }

    @Test
    fun `automatic mode infers activity from target range when action is absent`() {
        val entity = HAEntity(
            entity_id = "climate.test",
            state = "heat_cool",
            attributes = buildJsonObject {
                put("current_temperature", 25.0)
                put("target_temp_low", 19.0)
                put("target_temp_high", 22.0)
            }
        )

        assertEquals(ClimateSceneActivity.COOLING, entity.climateSceneActivity())
    }

    @Test
    fun `climate view defaults to half width thermostat dials`() {
        val config = Json.decodeFromString<HKIClimateConfig>("{}")

        assertEquals("dial", config.defaultDeviceCardStyle)
        assertEquals("half", config.defaultDeviceCardWidth)
    }

    @Test
    fun `explicit restored climate appearance remains a user choice`() {
        val config = Json.decodeFromString<HKIClimateConfig>(
            """{"defaultDeviceCardStyle":"card","defaultDeviceCardWidth":"full"}"""
        )

        assertEquals("card", config.defaultDeviceCardStyle)
        assertEquals("full", config.defaultDeviceCardWidth)
    }

    private fun climateEntity(
        state: String,
        action: String? = null,
        hvacMode: String? = null
    ) = HAEntity(
        entity_id = "climate.test",
        state = state,
        attributes = buildJsonObject {
            action?.let { put("hvac_action", it) }
            hvacMode?.let { put("hvac_mode", it) }
        }
    )

    private fun openingEntity(
        id: String,
        state: String,
        deviceClass: String?,
        currentPosition: Int? = null
    ) = HAEntity(
        entity_id = id,
        state = state,
        attributes = buildJsonObject {
            deviceClass?.let { put("device_class", it) }
            currentPosition?.let { put("current_position", it) }
        }
    )
}

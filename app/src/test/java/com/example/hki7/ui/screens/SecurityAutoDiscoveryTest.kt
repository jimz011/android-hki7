package com.example.hki7.ui.screens

import com.example.hki7.data.HAEntity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAutoDiscoveryTest {
    @Test
    fun `auto import includes every security group`() {
        val expected = setOf(
            "doors", "windows", "openings", "garage_doors", "covers", "locks",
            "motion", "presence", "fire", "smoke", "gas", "co2", "moisture",
            "sound", "light", "alarms", "cameras"
        )

        assertTrue(AUTO_SECURITY_GROUP_KEYS.toSet() == expected)
    }

    @Test
    fun `new security categories match their Home Assistant device classes`() {
        val matches = listOf(
            Triple("openings", "binary_sensor.garage_contact", "garage_door"),
            Triple("garage_doors", "cover.garage", "garage_door"),
            Triple("covers", "cover.shutter", "shutter"),
            Triple("fire", "binary_sensor.high_temperature", "heat"),
            Triple("fire", "binary_sensor.low_temperature", "cold"),
            Triple("fire", "binary_sensor.safety", "safety"),
            Triple("smoke", "binary_sensor.smoke", "smoke"),
            Triple("gas", "binary_sensor.gas", "gas"),
            Triple("gas", "binary_sensor.carbon_monoxide", "carbon_monoxide"),
            Triple("co2", "sensor.carbon_dioxide", "carbon_dioxide"),
            Triple("moisture", "binary_sensor.water_leak", "moisture"),
            Triple("sound", "binary_sensor.glass_break", "sound"),
            Triple("light", "binary_sensor.light_beam", "light")
        )

        matches.forEach { (group, entityId, deviceClass) ->
            assertTrue(
                "$entityId ($deviceClass) should be discovered in $group",
                entity(entityId, deviceClass).isAutoSecurityEntityFor(group)
            )
        }
    }

    @Test
    fun `device class matches remain domain specific`() {
        assertFalse(entity("sensor.smoke", "smoke").isAutoSecurityEntityFor("smoke"))
        assertFalse(entity("binary_sensor.carbon_dioxide", "carbon_dioxide").isAutoSecurityEntityFor("co2"))
        assertFalse(entity("switch.garage", "garage_door").isAutoSecurityEntityFor("garage_doors"))
    }

    private fun entity(entityId: String, deviceClass: String) = HAEntity(
        entity_id = entityId,
        state = "off",
        attributes = buildJsonObject { put("device_class", deviceClass) }
    )
}

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimateEntityFilterTest {
    @Test
    fun `ordinary room temperature sensors are imported`() {
        assertTrue(temperature("sensor.living_room_temperature", "Living room temperature")
            .isAutoClimateSensorFor("temperature"))
    }

    @Test
    fun `hardware and diagnostic temperatures are excluded`() {
        assertFalse(temperature("sensor.server_disk_temperature", "Server disk temperature")
            .isAutoClimateSensorFor("temperature"))
        assertFalse(temperature("sensor.host_thermal_zone", "Host thermal zone")
            .isAutoClimateSensorFor(
                "temperature",
                HAEntityRegistryEntry("sensor.host_thermal_zone", entity_category = "diagnostic")
            ))
    }

    @Test
    fun `weather provider sensors are excluded from climate groups`() {
        val weather = temperature("sensor.outside_temperature", "Outside temperature")
        val registry = HAEntityRegistryEntry(weather.entity_id, platform = "openweathermap")

        assertFalse(weather.isAutoClimateSensorFor("temperature", registry))
        assertFalse(temperature("sensor.openweathermap_temperature", "Temperature")
            .isAutoClimateSensorFor("temperature"))
    }

    private fun temperature(entityId: String, name: String) = HAEntity(
        entity_id = entityId,
        state = "21.5",
        attributes = buildJsonObject {
            put("friendly_name", name)
            put("device_class", "temperature")
        }
    )
}

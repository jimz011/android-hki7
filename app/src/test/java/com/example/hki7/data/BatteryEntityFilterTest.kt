package com.example.hki7.data

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryEntityFilterTest {
    private fun entity(
        id: String = "sensor.test_battery",
        state: String = "50",
        deviceClass: String? = "battery",
        unit: String? = "%"
    ) = HAEntity(
        entity_id = id,
        state = state,
        attributes = buildJsonObject {
            deviceClass?.let { put("device_class", JsonPrimitive(it)) }
            unit?.let { put("unit_of_measurement", JsonPrimitive(it)) }
        }
    )

    @Test
    fun `battery import requires device class and percentage unit`() {
        assertTrue(entity().isBatteryPercentageSensor())
        assertTrue(entity(state = "unavailable").isBatteryPercentageSensor())
        assertFalse(entity(unit = null).isBatteryPercentageSensor())
        assertFalse(entity(deviceClass = null).isBatteryPercentageSensor())
        assertFalse(entity(deviceClass = "humidity").isBatteryPercentageSensor())
        assertFalse(entity(id = "binary_sensor.test_battery").isBatteryPercentageSensor())
    }
}

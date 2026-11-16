package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PowerFormatTest {
    private fun sensor(state: String, unit: String?) = HAEntity(
        entity_id = "sensor.power",
        state = state,
        attributes = buildJsonObject { if (unit != null) put("unit_of_measurement", JsonPrimitive(unit)) }
    )

    @Test
    fun `labels keep at most one decimal`() {
        assertEquals("440W", formatPowerWatts(440.0))
        assertEquals("12.5W", formatPowerWatts(12.46))
        assertEquals("300W", formatPowerWatts(299.96))
        assertEquals("0W", formatPowerWatts(-0.01))
        assertEquals("2350W", formatPowerWatts(2350.0))
        assertEquals("12.3kW", formatPowerWatts(12_340.0))
    }

    @Test
    fun `readings convert to watts and skip non power`() {
        assertEquals(440.0, sensor("440", "W").powerWatts()!!, 0.0)
        assertEquals(1500.0, sensor("1.5", "kW").powerWatts()!!, 0.0)
        assertNull(sensor("unavailable", "W").powerWatts())
        assertNull(sensor("12", "kWh").powerWatts())
        assertEquals(740.0, totalPowerWatts(listOf(sensor("440", "W"), sensor("0.3", "kW"), sensor("x", "W")))!!, 0.0)
        assertNull(totalPowerWatts(listOf(sensor("unknown", "W"))))
    }
}

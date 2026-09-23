package com.jimz011apps.hki7.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyMeasurementFormatTest {
    @Test
    fun `large watt readings are shown as kilowatts`() {
        assertEquals("3.5 kW", formatEnergyMeasurement(3_540f, "W"))
    }

    @Test
    fun `energy units are not mistaken for power`() {
        assertEquals("3540 Wh", formatEnergyMeasurement(3_540f, "Wh"))
    }
}

package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryDeviceSiblingSwitchTest {

    @Test
    fun `switch siblings of primary device controls are excluded`() {
        val roomEntityIds = listOf(
            // Tuya light that also exposes a paired relay switch.
            "light.desk",
            "switch.desk_relay",
            // Climate unit exposing a child-lock switch.
            "climate.living",
            "switch.living_child_lock",
            // Air purifier (fan) with a display/sleep switch.
            "fan.purifier",
            "switch.purifier_display",
            // Cover with a calibration switch.
            "cover.blinds",
            "switch.blinds_calibrate",
            // A standalone smart plug on its own device stays.
            "switch.coffee_maker"
        )
        val deviceByEntity = mapOf(
            "light.desk" to "light-device",
            "switch.desk_relay" to "light-device",
            "climate.living" to "climate-device",
            "switch.living_child_lock" to "climate-device",
            "fan.purifier" to "fan-device",
            "switch.purifier_display" to "fan-device",
            "cover.blinds" to "cover-device",
            "switch.blinds_calibrate" to "cover-device",
            "switch.coffee_maker" to "plug-device"
        )
        val registry = roomEntityIds.map { id ->
            HAEntityRegistryEntry(entity_id = id, device_id = deviceByEntity.getValue(id))
        }

        assertEquals(
            setOf(
                "switch.desk_relay",
                "switch.living_child_lock",
                "switch.purifier_display",
                "switch.blinds_calibrate"
            ),
            primaryDeviceAuxiliarySwitchEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `primary controls and non-switch siblings are never excluded`() {
        val roomEntityIds = listOf(
            "climate.bedroom",
            "fan.bedroom_ceiling",
            "light.bedroom_lamp",
            "sensor.bedroom_temperature"
        )
        val registry = roomEntityIds.map { id ->
            HAEntityRegistryEntry(entity_id = id, device_id = "combo-device")
        }

        assertEquals(
            emptySet<String>(),
            primaryDeviceAuxiliarySwitchEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `a switch on its own device is kept`() {
        val roomEntityIds = listOf("light.hall", "switch.hall_fan")
        val registry = listOf(
            HAEntityRegistryEntry("light.hall", device_id = "light-device"),
            HAEntityRegistryEntry("switch.hall_fan", device_id = "switch-device")
        )

        assertEquals(
            emptySet<String>(),
            primaryDeviceAuxiliarySwitchEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `without registry device ownership no switch is suppressed`() {
        val roomEntityIds = listOf("light.hall", "switch.hall_relay")
        val registry = listOf(
            HAEntityRegistryEntry("light.hall"),
            HAEntityRegistryEntry("switch.hall_relay")
        )

        assertEquals(
            emptySet<String>(),
            primaryDeviceAuxiliarySwitchEntityIdsToExclude(roomEntityIds, registry)
        )
    }
}

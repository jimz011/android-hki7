package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryDeviceSiblingSwitchTest {

    @Test
    fun `appliance sibling controls (light, switch, child lock) are excluded`() {
        val roomEntityIds = listOf(
            // Air conditioner with a panel light, a sleep switch, and a child lock.
            "climate.living_ac",
            "light.living_ac_panel",
            "switch.living_ac_sleep",
            "lock.living_ac_child_lock",
            // Tuya light that also exposes a paired relay switch.
            "light.desk",
            "switch.desk_relay",
            // Blind (cover) that imports its siblings.
            "cover.blinds",
            "light.blinds_led",
            "switch.blinds_calibrate",
            "lock.blinds_child_lock",
            // Air purifier (fan) with a child lock + display switch — but its own light stays.
            "fan.purifier",
            "switch.purifier_display",
            "lock.purifier_child_lock",
            // Standalone smart plug and a real door lock, each on its own device.
            "switch.coffee_maker",
            "lock.front_door"
        )
        val deviceByEntity = mapOf(
            "climate.living_ac" to "ac-device",
            "light.living_ac_panel" to "ac-device",
            "switch.living_ac_sleep" to "ac-device",
            "lock.living_ac_child_lock" to "ac-device",
            "light.desk" to "light-device",
            "switch.desk_relay" to "light-device",
            "cover.blinds" to "cover-device",
            "light.blinds_led" to "cover-device",
            "switch.blinds_calibrate" to "cover-device",
            "lock.blinds_child_lock" to "cover-device",
            "fan.purifier" to "fan-device",
            "switch.purifier_display" to "fan-device",
            "lock.purifier_child_lock" to "fan-device",
            "switch.coffee_maker" to "plug-device",
            "lock.front_door" to "door-lock-device"
        )
        val registry = roomEntityIds.map { id ->
            HAEntityRegistryEntry(entity_id = id, device_id = deviceByEntity.getValue(id))
        }

        assertEquals(
            setOf(
                "light.living_ac_panel",
                "switch.living_ac_sleep",
                "lock.living_ac_child_lock",
                "switch.desk_relay",
                "light.blinds_led",
                "switch.blinds_calibrate",
                "lock.blinds_child_lock",
                "switch.purifier_display",
                "lock.purifier_child_lock"
            ),
            applianceDeviceSiblingControlEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `a ceiling fan's own light is kept`() {
        val roomEntityIds = listOf("fan.ceiling", "light.ceiling")
        val registry = roomEntityIds.map { HAEntityRegistryEntry(entity_id = it, device_id = "ceiling-device") }

        assertEquals(
            emptySet<String>(),
            applianceDeviceSiblingControlEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `a standalone door lock is never excluded`() {
        val roomEntityIds = listOf("lock.front_door", "light.hall")
        val registry = listOf(
            HAEntityRegistryEntry("lock.front_door", device_id = "lock-device"),
            HAEntityRegistryEntry("light.hall", device_id = "light-device")
        )

        assertEquals(
            emptySet<String>(),
            applianceDeviceSiblingControlEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `primary controls and non-suppressed siblings are never excluded`() {
        val roomEntityIds = listOf(
            "climate.bedroom",
            "fan.bedroom_ceiling",
            "sensor.bedroom_temperature"
        )
        val registry = roomEntityIds.map { HAEntityRegistryEntry(entity_id = it, device_id = "combo-device") }

        assertEquals(
            emptySet<String>(),
            applianceDeviceSiblingControlEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `without registry device ownership nothing is suppressed`() {
        val roomEntityIds = listOf("climate.ac", "switch.ac_sleep", "lock.ac_child_lock")
        val registry = roomEntityIds.map { HAEntityRegistryEntry(entity_id = it) }

        assertEquals(
            emptySet<String>(),
            applianceDeviceSiblingControlEntityIdsToExclude(roomEntityIds, registry)
        )
    }
}

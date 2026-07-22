package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HADeviceRegistryEntry
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomCounterDeviceAutofillTest {
    @Test
    fun `espresence setup switches are excluded by entity or device name`() {
        val entities = listOf(
            HAEntity("switch.espresence_hall_radar", "on"),
            HAEntity("switch.office_radar_gain", "on"),
            HAEntity("switch.normal_lamp", "on")
        )
        val registry = listOf(
            HAEntityRegistryEntry("switch.espresence_hall_radar", device_id = "hall"),
            HAEntityRegistryEntry("switch.office_radar_gain", device_id = "office"),
            HAEntityRegistryEntry("switch.normal_lamp", device_id = "lamp")
        )
        val devices = listOf(
            HADeviceRegistryEntry("hall", name = "Hall sensor"),
            HADeviceRegistryEntry("office", name = "ESPresence Office"),
            HADeviceRegistryEntry("lamp", name = "Desk lamp")
        )

        assertEquals(
            setOf("switch.espresence_hall_radar", "switch.office_radar_gain"),
            espresenceSwitchEntityIdsToExclude(entities, registry, devices)
        )
    }

    @Test
    fun `light and switch siblings of sensor counter devices are excluded from room auto fill`() {
        val areaEntityIds = listOf(
            "binary_sensor.hall_motion",
            "switch.hall_motion_led",
            "light.hall_motion_nightlight",
            "binary_sensor.office_presence",
            "switch.office_presence_sensitivity",
            "binary_sensor.utility_smoke",
            "switch.utility_smoke_alarm",
            "switch.unrelated"
        )
        val roomStatusEntityIds = mapOf(
            RoomStatusRoles.MOTION to listOf("binary_sensor.hall_motion"),
            RoomStatusRoles.PRESENCE to listOf("binary_sensor.office_presence"),
            RoomStatusRoles.SMOKE to listOf("binary_sensor.utility_smoke"),
            RoomStatusRoles.DEVICES to listOf("switch.unrelated")
        )
        val deviceByEntity = mapOf(
            "binary_sensor.hall_motion" to "motion-device",
            "switch.hall_motion_led" to "motion-device",
            "light.hall_motion_nightlight" to "motion-device",
            "binary_sensor.office_presence" to "presence-device",
            "switch.office_presence_sensitivity" to "presence-device",
            "binary_sensor.utility_smoke" to "smoke-device",
            "switch.utility_smoke_alarm" to "smoke-device",
            "switch.unrelated" to "other-device"
        )
        val registry = areaEntityIds.map { entityId ->
            HAEntityRegistryEntry(entity_id = entityId, device_id = deviceByEntity.getValue(entityId))
        }

        assertEquals(
            setOf(
                "switch.hall_motion_led",
                "light.hall_motion_nightlight",
                "switch.office_presence_sensitivity",
                "switch.utility_smoke_alarm"
            ),
            roomCounterDeviceControlEntityIdsToExclude(areaEntityIds, roomStatusEntityIds, registry)
        )
    }

    @Test
    fun `counter entity outside the room does not suppress a room switch`() {
        val areaEntityIds = listOf("switch.hall_motion_led")
        val roomStatusEntityIds = mapOf(
            RoomStatusRoles.MOTION to listOf("binary_sensor.hall_motion")
        )
        val registry = listOf(
            HAEntityRegistryEntry("binary_sensor.hall_motion", device_id = "motion-device"),
            HAEntityRegistryEntry("switch.hall_motion_led", device_id = "motion-device")
        )

        assertEquals(
            emptySet<String>(),
            roomCounterDeviceControlEntityIdsToExclude(areaEntityIds, roomStatusEntityIds, registry)
        )
    }

    @Test
    fun `counter without device ownership does not use entity name heuristics`() {
        val areaEntityIds = listOf(
            "binary_sensor.hall_motion",
            "switch.hall_motion_led",
            "light.hall_motion_nightlight"
        )
        val roomStatusEntityIds = mapOf(
            RoomStatusRoles.MOTION to listOf("binary_sensor.hall_motion")
        )
        val registry = listOf(
            HAEntityRegistryEntry("binary_sensor.hall_motion"),
            HAEntityRegistryEntry("switch.hall_motion_led", device_id = "motion-device"),
            HAEntityRegistryEntry("light.hall_motion_nightlight", device_id = "motion-device")
        )

        assertEquals(
            emptySet<String>(),
            roomCounterDeviceControlEntityIdsToExclude(areaEntityIds, roomStatusEntityIds, registry)
        )
    }
}

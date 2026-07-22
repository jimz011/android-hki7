package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraDeviceRoomAutofillTest {

    @Test
    fun `camera device siblings are excluded from generated lights switches and badges`() {
        val roomEntityIds = listOf(
            "camera.entry",
            "switch.entry_privacy",
            "fan.entry_siren",
            "humidifier.entry_control",
            "cover.entry_shutter",
            "lock.entry_lock",
            "climate.entry_temperature",
            "light.entry_floodlight",
            "sensor.entry_signal",
            "switch.unrelated"
        )
        val registry = roomEntityIds.map { entityId ->
            HAEntityRegistryEntry(
                entity_id = entityId,
                device_id = if (entityId == "switch.unrelated") "other-device" else "camera-device"
            )
        }

        assertEquals(
            setOf(
                "light.entry_floodlight",
                "switch.entry_privacy",
                "fan.entry_siren",
                "humidifier.entry_control",
                "cover.entry_shutter",
                "lock.entry_lock",
                "climate.entry_temperature"
            ),
            cameraDeviceSiblingEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `camera outside the room does not suppress controls in this room`() {
        val roomEntityIds = listOf("switch.entry_privacy", "climate.entry_temperature")
        val registry = listOf(
            HAEntityRegistryEntry("camera.entry", device_id = "camera-device"),
            HAEntityRegistryEntry("switch.entry_privacy", device_id = "camera-device"),
            HAEntityRegistryEntry("climate.entry_temperature", device_id = "camera-device")
        )

        assertEquals(
            emptySet<String>(),
            cameraDeviceSiblingEntityIdsToExclude(roomEntityIds, registry)
        )
    }

    @Test
    fun `camera without registry device ownership does not use name heuristics`() {
        val roomEntityIds = listOf("camera.entry", "switch.entry_privacy")
        val registry = listOf(
            HAEntityRegistryEntry("camera.entry"),
            HAEntityRegistryEntry("switch.entry_privacy", device_id = "camera-device")
        )

        assertEquals(
            emptySet<String>(),
            cameraDeviceSiblingEntityIdsToExclude(roomEntityIds, registry)
        )
    }
}

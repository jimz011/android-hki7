package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomImportMetadataTest {
    @Test
    fun `window on the same direct device as climate is excluded`() {
        val climate = HAEntity("climate.thermostat", "heat")
        val window = windowEntity("binary_sensor.thermostat_open_window")
        val windowCover = windowEntity("cover.thermostat_open_window")
        val registry = listOf(
            HAEntityRegistryEntry(climate.entity_id, device_id = "thermostat-device"),
            HAEntityRegistryEntry(window.entity_id, device_id = "thermostat-device"),
            HAEntityRegistryEntry(windowCover.entity_id, device_id = "thermostat-device")
        )

        assertEquals(
            setOf(window.entity_id, windowCover.entity_id),
            climateOwnedWindowEntityIds(listOf(climate, window, windowCover), registry)
        )
    }

    @Test
    fun `real window on another device is retained`() {
        val climate = HAEntity("climate.thermostat", "heat")
        val window = windowEntity("binary_sensor.bedroom_window")
        val registry = listOf(
            HAEntityRegistryEntry(climate.entity_id, device_id = "thermostat-device"),
            HAEntityRegistryEntry(window.entity_id, device_id = "window-device")
        )

        assertTrue(climateOwnedWindowEntityIds(listOf(climate, window), registry).isEmpty())
    }

    @Test
    fun `registry-only climate still identifies its window detection entity`() {
        val window = windowEntity("binary_sensor.thermostat_open_window")
        val registry = listOf(
            HAEntityRegistryEntry("climate.disabled_thermostat", device_id = "thermostat-device"),
            HAEntityRegistryEntry(window.entity_id, device_id = "thermostat-device")
        )

        assertEquals(
            setOf(window.entity_id),
            climateOwnedWindowEntityIds(listOf(window), registry)
        )
    }

    @Test
    fun `device-less entities and names without window class are retained`() {
        val climate = HAEntity("climate.thermostat", "heat")
        val deviceLessWindow = windowEntity("binary_sensor.window")
        val namedWindow = HAEntity("binary_sensor.window_detection", "on")
        val registry = listOf(
            HAEntityRegistryEntry(climate.entity_id, device_id = "thermostat-device"),
            HAEntityRegistryEntry(namedWindow.entity_id, device_id = "thermostat-device")
        )

        assertTrue(
            climateOwnedWindowEntityIds(
                listOf(climate, deviceLessWindow, namedWindow),
                registry
            ).isEmpty()
        )
    }

    private fun windowEntity(entityId: String): HAEntity = HAEntity(
        entity_id = entityId,
        state = "on",
        attributes = buildJsonObject { put("device_class", "window") }
    )
}

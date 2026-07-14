package com.example.hki7.ui

import com.example.hki7.data.HAConfigEntry
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnergyCarbonImportTest {
    @Test
    fun `matches fossil percentage through enabled Electricity Maps config entry`() {
        val result = resolveHomeAssistantEnergyCarbonEntity(
            configEntries = listOf(
                HAConfigEntry("disabled", "co2signal", state = "loaded", disabled_by = "user"),
                HAConfigEntry("enabled", "co2signal", state = "loaded")
            ),
            registry = listOf(
                HAEntityRegistryEntry(
                    entity_id = "sensor.disabled_fossil",
                    platform = "co2signal",
                    config_entry_id = "disabled",
                    translation_key = "fossil_fuel_percentage"
                ),
                HAEntityRegistryEntry(
                    entity_id = "sensor.enabled_carbon_intensity",
                    platform = "co2signal",
                    config_entry_id = "enabled",
                    translation_key = "carbon_intensity",
                    unit_of_measurement = "gCO2eq/kWh"
                ),
                HAEntityRegistryEntry(
                    entity_id = "sensor.enabled_fossil",
                    platform = "co2signal",
                    config_entry_id = "enabled",
                    translation_key = "fossil_fuel_percentage"
                )
            ),
            liveEntities = emptyList()
        )

        assertEquals("sensor.enabled_fossil", result)
    }

    @Test
    fun `uses stable translation key without requiring a live state`() {
        val result = resolveHomeAssistantEnergyCarbonEntity(
            configEntries = emptyList(),
            registry = listOf(
                HAEntityRegistryEntry(
                    entity_id = "sensor.user_renamed_this",
                    platform = "co2signal",
                    translation_key = "fossil_fuel_percentage"
                )
            ),
            liveEntities = emptyList()
        )

        assertEquals("sensor.user_renamed_this", result)
    }

    @Test
    fun `falls back to live percentage unit for older entity registries`() {
        val entityId = "sensor.legacy_electricity_maps_value"
        val result = resolveHomeAssistantEnergyCarbonEntity(
            configEntries = emptyList(),
            registry = listOf(
                HAEntityRegistryEntry(entity_id = entityId, platform = "co2signal")
            ),
            liveEntities = listOf(
                HAEntity(
                    entity_id = entityId,
                    state = "34.2",
                    attributes = buildJsonObject { put("unit_of_measurement", "%") }
                )
            )
        )

        assertEquals(entityId, result)
    }

    @Test
    fun `does not import a disabled registry entity`() {
        val result = resolveHomeAssistantEnergyCarbonEntity(
            configEntries = listOf(HAConfigEntry("enabled", "co2signal", state = "loaded")),
            registry = listOf(
                HAEntityRegistryEntry(
                    entity_id = "sensor.disabled_fossil",
                    platform = "co2signal",
                    config_entry_id = "enabled",
                    translation_key = "fossil_fuel_percentage",
                    disabled_by = "user"
                )
            ),
            liveEntities = emptyList()
        )

        assertNull(result)
    }

    @Test
    fun `does not fall back to an entity owned by a disabled Electricity Maps entry`() {
        val result = resolveHomeAssistantEnergyCarbonEntity(
            configEntries = listOf(
                HAConfigEntry("disabled", "co2signal", state = "not_loaded", disabled_by = "user")
            ),
            registry = listOf(
                HAEntityRegistryEntry(
                    entity_id = "sensor.old_fossil",
                    platform = "co2signal",
                    config_entry_id = "disabled",
                    translation_key = "fossil_fuel_percentage"
                )
            ),
            liveEntities = emptyList()
        )

        assertNull(result)
    }
}

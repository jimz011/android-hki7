package com.example.hki7.ui

import com.example.hki7.data.HAConfigEntry
import com.example.hki7.data.HADeviceRegistryEntry
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAEntityRegistryEntry
import com.example.hki7.data.HKIEnergyConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnergyCarbonImportTest {
    @Test
    fun `energy preference source imports related entities from the same device`() {
        fun entity(id: String, name: String, deviceClass: String, unit: String) = HAEntity(
            entity_id = id,
            state = "1",
            attributes = buildJsonObject {
                put("friendly_name", name)
                put("device_class", deviceClass)
                put("unit_of_measurement", unit)
            }
        )
        val source = entity("sensor.meter_import", "Grid import", "energy", "kWh")
        val gridPower = entity("sensor.meter_power", "Grid power", "power", "W")
        val phasePower = entity("sensor.meter_phase_1", "Grid power phase 1", "power", "W")
        val phaseCurrent = entity("sensor.meter_current_1", "Grid current phase 1", "current", "A")
        val tariff = entity("sensor.meter_import_t1", "Grid import tariff 1", "energy", "kWh")
        val unrelated = entity("sensor.other_power", "Other power", "power", "W")
        val live = listOf(source, gridPower, phasePower, phaseCurrent, tariff, unrelated)
        val registry = live.map { entity ->
            HAEntityRegistryEntry(
                entity_id = entity.entity_id,
                device_id = if (entity == unrelated) "other-device" else "meter-device"
            )
        }

        val result = importRelatedHomeAssistantEnergyEntities(
            config = HKIEnergyConfig(),
            sourceEntityIds = mapOf("electricity" to listOf(source.entity_id)),
            registry = registry,
            devices = listOf(
                HADeviceRegistryEntry(id = "meter-device"),
                HADeviceRegistryEntry(id = "other-device")
            ),
            liveEntities = live
        )

        assertEquals("meter-device", result.electricityDeviceId)
        assertEquals(gridPower.entity_id, result.gridPowerEntityId)
        assertEquals(phasePower.entity_id, result.powerPhase1EntityId)
        assertEquals(phaseCurrent.entity_id, result.currentPhase1EntityId)
        assertEquals(tariff.entity_id, result.gridImportTariff1EntityId)
    }

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

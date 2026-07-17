package com.example.hki7.ui

import com.example.hki7.data.HAConfigEntry
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
        val gridExport = entity("sensor.meter_export", "Grid export", "energy", "kWh")
        val gridPower = entity("sensor.meter_power", "Grid power", "power", "W")
        val homePower = entity("sensor.home_load", "Home load", "power", "W")
        val phasePower = entity("sensor.meter_phase_1", "Grid power phase 1", "power", "W")
        val phaseCurrent = entity("sensor.meter_current_1", "Grid current phase 1", "current", "A")
        val tariff = entity("sensor.meter_import_t1", "Grid import tariff 1", "energy", "kWh")
        val cost = entity("sensor.meter_cost", "Electricity cost", "monetary", "EUR")
        val unrelated = entity("sensor.other_power", "Other power", "power", "W")
        val live = listOf(source, gridExport, gridPower, homePower, phasePower, phaseCurrent, tariff, cost, unrelated)
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
            liveEntities = live
        )

        assertEquals("meter-device", result.electricityDeviceId)
        assertEquals(source.entity_id, result.gridImportEntityId)
        assertEquals(gridExport.entity_id, result.gridExportEntityId)
        assertEquals(gridPower.entity_id, result.gridPowerEntityId)
        assertEquals(homePower.entity_id, result.homePowerEntityId)
        assertEquals(phasePower.entity_id, result.powerPhase1EntityId)
        assertEquals(phaseCurrent.entity_id, result.currentPhase1EntityId)
        assertEquals(tariff.entity_id, result.gridImportTariff1EntityId)
        assertEquals(cost.entity_id, result.energyCostEntityId)
    }

    @Test
    fun `related device discovery refreshes a stale non-customized guess`() {
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
        val discoveredPower = entity("sensor.meter_power", "Grid power", "power", "W")
        val retainedPower = "sensor.manually_selected_power"
        val live = listOf(source, discoveredPower)

        val result = importRelatedHomeAssistantEnergyEntities(
            config = HKIEnergyConfig(gridPowerEntityId = retainedPower),
            sourceEntityIds = mapOf("electricity" to listOf(source.entity_id)),
            registry = live.map { HAEntityRegistryEntry(it.entity_id, device_id = "meter-device") },
            liveEntities = live
        )

        assertEquals(discoveredPower.entity_id, result.gridPowerEntityId)
        assertEquals(source.entity_id, result.gridImportEntityId)
    }

    @Test
    fun `explicitly cleared role is not repopulated by a source device guess`() {
        val source = HAEntity(
            entity_id = "sensor.meter_import",
            state = "1",
            attributes = buildJsonObject {
                put("friendly_name", "Grid import")
                put("device_class", "energy")
                put("unit_of_measurement", "kWh")
            }
        )
        val phase = HAEntity(
            entity_id = "sensor.meter_phase_1",
            state = "100",
            attributes = buildJsonObject {
                put("friendly_name", "Grid power phase 1")
                put("device_class", "power")
                put("unit_of_measurement", "W")
            }
        )

        val result = importRelatedHomeAssistantEnergyEntities(
            config = HKIEnergyConfig(customizedEntityRoles = setOf("phase1")),
            sourceEntityIds = mapOf("electricity" to listOf(source.entity_id)),
            registry = listOf(source, phase).map {
                HAEntityRegistryEntry(it.entity_id, device_id = "meter-device")
            },
            liveEntities = listOf(source, phase)
        )

        assertNull(result.powerPhase1EntityId)
    }

    @Test
    fun `related discovery does not import entities from child or secondary devices`() {
        fun entity(id: String, name: String, deviceClass: String, unit: String) = HAEntity(
            entity_id = id,
            state = "1",
            attributes = buildJsonObject {
                put("friendly_name", name)
                put("device_class", deviceClass)
                put("unit_of_measurement", unit)
            }
        )
        val source = entity("sensor.parent_import", "Grid import", "energy", "kWh")
        val parentPower = entity("sensor.parent_power", "Grid power", "power", "W")
        val childPower = entity("sensor.child_power", "Grid power", "power", "W")
        val secondarySource = entity("sensor.secondary_import", "Grid import", "energy", "kWh")
        val secondaryPhase = entity("sensor.secondary_phase_1", "Grid power phase 1", "power", "W")
        val live = listOf(source, parentPower, childPower, secondarySource, secondaryPhase)
        val registry = listOf(
            HAEntityRegistryEntry(source.entity_id, device_id = "parent-device"),
            HAEntityRegistryEntry(parentPower.entity_id, device_id = "parent-device"),
            HAEntityRegistryEntry(childPower.entity_id, device_id = "child-device"),
            HAEntityRegistryEntry(secondarySource.entity_id, device_id = "secondary-device"),
            HAEntityRegistryEntry(secondaryPhase.entity_id, device_id = "secondary-device")
        )

        val result = importRelatedHomeAssistantEnergyEntities(
            config = HKIEnergyConfig(),
            sourceEntityIds = mapOf(
                "electricity" to listOf(source.entity_id, secondarySource.entity_id)
            ),
            registry = registry,
            liveEntities = live
        )

        assertEquals(parentPower.entity_id, result.gridPowerEntityId)
        assertNull(result.powerPhase1EntityId)
    }

    @Test
    fun `supporting power entity from another device is rejected`() {
        val registry = listOf(
            HAEntityRegistryEntry("sensor.p1_energy", device_id = "p1-device"),
            HAEntityRegistryEntry("sensor.p1_power", device_id = "p1-device"),
            HAEntityRegistryEntry("sensor.old_dsmr_power", device_id = "old-dsmr-device")
        )

        assertEquals(
            "sensor.p1_power",
            supportingEnergyEntityForDevice("sensor.p1_power", "p1-device", registry)
        )
        assertNull(
            supportingEnergyEntityForDevice("sensor.old_dsmr_power", "p1-device", registry)
        )
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

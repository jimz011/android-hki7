package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jimz011apps.hki7.data.HADeviceRegistryEntry
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.ui.screens.resolveVacuumDeviceEntities

/** Device-first vacuum setup, with the raw vacuum entity picker retained as a fallback. */
@Composable
fun VacuumWidgetSetupDialog(
    allEntities: List<HAEntity>,
    entityRegistry: List<HAEntityRegistryEntry>,
    deviceRegistry: List<HADeviceRegistryEntry>,
    onDismiss: () -> Unit,
    onSelected: (entityId: String, config: HKIButtonConfig) -> Unit
) {
    var useEntityFallback by remember { mutableStateOf(false) }
    val vacuumEntries = remember(entityRegistry) {
        entityRegistry.filter {
            it.entity_id.startsWith("vacuum.") && it.device_id != null && it.disabled_by == null
        }
    }
    val vacuumDeviceIds = remember(vacuumEntries) { vacuumEntries.mapNotNull { it.device_id }.toSet() }
    val vacuumDevices = remember(deviceRegistry, vacuumDeviceIds) {
        deviceRegistry.filter { it.id in vacuumDeviceIds }
    }

    if (useEntityFallback) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("vacuum.") },
            title = "Select Vacuum Entity",
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = { useEntityFallback = false },
            onEntitiesSelected = { ids ->
                ids.firstOrNull()?.let { onSelected(it, HKIButtonConfig()) }
            }
        )
    } else {
        DevicePickerDialogWithAlternative(
            devices = vacuumDevices,
            currentId = null,
            onDismiss = onDismiss,
            onSelected = { deviceId ->
                if (deviceId == null) return@DevicePickerDialogWithAlternative
                val vacuumId = vacuumEntries.firstOrNull { it.device_id == deviceId }?.entity_id
                    ?: return@DevicePickerDialogWithAlternative
                val helpers = resolveVacuumDeviceEntities(deviceId, allEntities, entityRegistry)
                onSelected(
                    vacuumId,
                    HKIButtonConfig(
                        vacuumDeviceId = deviceId,
                        vacuumMapEntityId = helpers.map?.entity_id,
                        vacuumBatteryEntityId = helpers.battery?.entity_id,
                        vacuumWaterEntityId = helpers.water?.entity_id,
                        vacuumEmptyBinEntityId = helpers.emptyBin?.entity_id
                    )
                )
            },
            alternativeLabel = "Choose a vacuum entity instead",
            onAlternative = { useEntityFallback = true }
        )
    }
}

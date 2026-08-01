package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HADeviceRegistryEntry
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/** One auxiliary humidifier entity slot: how it is labelled, which entity ids match it during
 *  device auto-fill, and which domains its manual picker is limited to. */
data class HumidifierAuxSlot(
    val key: String,
    @StringRes val labelRes: Int,
    val matchKeys: List<String>,
    val domains: List<String>
)

/** Ordered set of auxiliary humidifier entities surfaced in the humidifier dialog. Mirrors the
 *  helper entities typical dehumidifier integrations (e.g. Midea) expose per device. */
val HumidifierAuxSlots: List<HumidifierAuxSlot> = listOf(
    HumidifierAuxSlot("current_humidity", R.string.core_current_humidity, listOf("humidity"), listOf("sensor")),
    HumidifierAuxSlot("tank_level", R.string.core_tank_water_level, listOf("tank", "water_level", "water"), listOf("sensor", "binary_sensor")),
    HumidifierAuxSlot("pm25", R.string.core_pm25, listOf("pm2_5", "pm25", "pm2.5"), listOf("sensor")),
    HumidifierAuxSlot("error", R.string.core_error_code, listOf("error"), listOf("sensor", "binary_sensor")),
    HumidifierAuxSlot("bucket_full", R.string.core_bucket_full, listOf("bucket_full", "tank_full", "full"), listOf("binary_sensor")),
    HumidifierAuxSlot("clean_filter", R.string.core_clean_filter, listOf("clean_filter", "filter"), listOf("binary_sensor", "sensor")),
    HumidifierAuxSlot("defrost", R.string.ui_defrosting_c3b2817, listOf("defrost"), listOf("binary_sensor", "sensor")),
    HumidifierAuxSlot("ionizer", R.string.core_ionizer, listOf("ionizer", "anion", "ion"), listOf("switch")),
    HumidifierAuxSlot("pump", R.string.core_pump, listOf("pump"), listOf("switch")),
    HumidifierAuxSlot("sleep", R.string.cr_mode_sleep, listOf("sleep"), listOf("switch")),
    HumidifierAuxSlot("beep", R.string.core_beep_on_command, listOf("beep", "buzzer"), listOf("switch"))
)

/** Resolves the auxiliary humidifier entities for [deviceId] by matching that device's registry
 *  entities against each slot's [HumidifierAuxSlot.matchKeys]. Only fills slots with a match. */
fun autofillHumidifierAux(
    deviceId: String,
    entityRegistry: List<HAEntityRegistryEntry>
): Map<String, String> {
    val deviceEntities = entityRegistry.filter { it.device_id == deviceId }
    val result = mutableMapOf<String, String>()
    HumidifierAuxSlots.forEach { slot ->
        val match = deviceEntities.firstOrNull { entry ->
            val objectId = entry.entity_id.substringAfter('.')
            val domain = entry.entity_id.substringBefore('.')
            domain in slot.domains && slot.matchKeys.any { objectId.contains(it) }
        }
        if (match != null) result[slot.key] = match.entity_id
    }
    return result
}

/** Also returns the linked fan entity guessed from a device (first fan.* on the device), if any. */
fun autofillHumidifierFan(deviceId: String, entityRegistry: List<HAEntityRegistryEntry>): String? =
    entityRegistry.firstOrNull { it.device_id == deviceId && it.entity_id.startsWith("fan.") }?.entity_id

/**
 * Shared "Entity integrations" editor for a humidifier: pick a device to auto-fill its helper
 * entities, link a speed control, and fine-tune each auxiliary entity manually. Used by both the
 * button settings dialog and the header-pill (badge) settings dialog.
 */
@Composable
fun HumidifierIntegrationSettings(
    deviceId: String?,
    fanEntityId: String?,
    auxEntityIds: Map<String, String>,
    allEntities: List<HAEntity>,
    devices: List<HADeviceRegistryEntry>,
    entityRegistry: List<HAEntityRegistryEntry>,
    onChange: (deviceId: String?, fanEntityId: String?, auxEntityIds: Map<String, String>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var showDevicePicker by remember { mutableStateOf(false) }
    var showFanPicker by remember { mutableStateOf(false) }
    var auxPickerSlot by remember { mutableStateOf<String?>(null) }

    fun nameOf(id: String?) = id?.let { e -> allEntities.find { it.entity_id == e }?.friendlyName ?: e }

    SettingsSubcategory(stringResource(R.string.ui_entity_integrations_852a3e0), stringResource(R.string.ui_link_a_device_to_auto_fill_the_humidifier_s_6f65010))

    // Device selector (auto-fill)
    val deviceName = deviceId?.let { id -> devices.find { it.id == id }?.let { it.name_by_user ?: it.name } ?: id }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_device_a5a74a6), style = MaterialTheme.typography.labelLarge)
            Text(
                deviceName ?: stringResource(R.string.ui_select_to_auto_fill_helper_entities_below_84a8833),
                style = MaterialTheme.typography.bodySmall,
                color = if (deviceName != null) MaterialTheme.colorScheme.primary else appColors.onMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = { showDevicePicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
        if (deviceId != null) TextButton(onClick = { onChange(null, fanEntityId, auxEntityIds) }) { Text(stringResource(R.string.ui_clear_719ea39)) }
    }

    // Linked speed control (fan / select / input_select)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_speed_control_89a2786), style = MaterialTheme.typography.labelLarge)
            Text(
                nameOf(fanEntityId) ?: stringResource(R.string.ui_none_modes_stay_as_the_only_control_b62bff7),
                style = MaterialTheme.typography.bodySmall,
                color = if (fanEntityId != null) MaterialTheme.colorScheme.primary else appColors.onMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = { showFanPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
        if (fanEntityId != null) TextButton(onClick = { onChange(deviceId, null, auxEntityIds) }) { Text(stringResource(R.string.ui_clear_719ea39)) }
    }

    // Per-slot auxiliary entities
    HumidifierAuxSlots.forEach { slot ->
        val current = auxEntityIds[slot.key]
        val slotLabel = stringResource(slot.labelRes)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(slotLabel, style = MaterialTheme.typography.labelLarge)
                Text(
                    nameOf(current) ?: stringResource(R.string.ui_not_set_93039e6),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (current != null) MaterialTheme.colorScheme.primary else appColors.onMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = { auxPickerSlot = slot.key }) { Text(stringResource(R.string.ui_change_64fbd99)) }
            if (current != null) TextButton(onClick = { onChange(deviceId, fanEntityId, auxEntityIds - slot.key) }) { Text(stringResource(R.string.ui_clear_719ea39)) }
        }
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = devices,
            currentId = deviceId,
            onDismiss = { showDevicePicker = false },
            onSelected = { id ->
                showDevicePicker = false
                if (id == null) {
                    onChange(null, fanEntityId, auxEntityIds)
                } else {
                    val filled = autofillHumidifierAux(id, entityRegistry)
                    val fan = fanEntityId ?: autofillHumidifierFan(id, entityRegistry)
                    // Device auto-fill overlays discovered slots but keeps any the user set by hand.
                    onChange(id, fan, filled + auxEntityIds)
                }
            }
        )
    }
    if (showFanPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("fan.") || it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.") },
            title = stringResource(R.string.ui_select_speed_control_20f7efa),
            singleSelect = true,
            preselectedIds = setOfNotNull(fanEntityId),
            onDismiss = { showFanPicker = false },
            onEntitiesSelected = { ids -> onChange(deviceId, ids.firstOrNull(), auxEntityIds); showFanPicker = false }
        )
    }
    auxPickerSlot?.let { key ->
        val slot = HumidifierAuxSlots.first { it.key == key }
        val slotLabel = stringResource(slot.labelRes)
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { e -> slot.domains.any { e.entity_id.startsWith("$it.") } },
            title = stringResource(R.string.ui_select_0fc8ec5, slotLabel),
            singleSelect = true,
            preselectedIds = setOfNotNull(auxEntityIds[key]),
            onDismiss = { auxPickerSlot = null },
            onEntitiesSelected = { ids ->
                val id = ids.firstOrNull()
                onChange(deviceId, fanEntityId, if (id == null) auxEntityIds - key else auxEntityIds + (key to id))
                auxPickerSlot = null
            }
        )
    }
}

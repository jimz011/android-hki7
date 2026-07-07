package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAServiceCall
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** The HA device an entity belongs to, or null while registries load / for device-less entities. */
@Composable
fun rememberEntityDeviceId(entity: HAEntity, viewModel: MainViewModel): String? {
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    return remember(entityRegistry, entity.entity_id) {
        entityRegistry.find { it.entity_id == entity.entity_id }?.device_id
    }
}

/**
 * Device-centric dialog view: the HA device the entity belongs to, plus live values of every other
 * entity on that device (battery level, contact sensors, extra controls of a garage door, ...).
 * Controllable entities (switches, locks, covers, selects, numbers, text, buttons) are operated
 * directly from their row. Entities stay the fallback — this only renders when a device is known.
 */
@Composable
fun DeviceEntitiesView(
    deviceId: String,
    sourceEntity: HAEntity,
    viewModel: MainViewModel
) {
    val appColors = LocalHKIAppColors.current
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    val allEntities by viewModel.entities.collectAsState()

    val device = deviceRegistry.find { it.id == deviceId }
    val deviceName = device?.let { it.name_by_user ?: it.name }
    val deviceDetail = listOfNotNull(device?.manufacturer, device?.model)
        .joinToString(" · ").ifBlank { null }

    // Sibling entities on the same device: primary entities first, diagnostics after,
    // battery pinned to the top of the diagnostics.
    val siblings = remember(entityRegistry, allEntities, deviceId, sourceEntity.entity_id) {
        val entries = entityRegistry.filter {
            it.device_id == deviceId && it.entity_id != sourceEntity.entity_id &&
                it.disabled_by == null && it.hidden_by == null
        }
        val byId = allEntities.associateBy { it.entity_id }
        entries.mapNotNull { entry ->
            byId[entry.entity_id]?.let { e -> entry to e }
        }.sortedWith(
            compareBy(
                { (entry, e) -> if (e.deviceClass == "battery") 0 else if (entry.entity_category == null) 1 else 2 },
                { (_, e) -> e.friendlyName ?: e.entity_id }
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text(
                    deviceName ?: "Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.onSurface, fontWeight = FontWeight.Bold
                )
                if (deviceDetail != null) {
                    Text(deviceDetail, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                }
                device?.sw_version?.takeIf { it.isNotBlank() }?.let {
                    Text("Firmware $it", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted.copy(alpha = 0.7f))
                }
            }
        }
        if (siblings.isEmpty()) {
            item {
                Text(
                    "No other entities on this device.",
                    style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                )
            }
        } else {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = appColors.subtleSurface) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        siblings.forEachIndexed { idx, (entry, e) ->
                            DeviceEntityRow(
                                entity = e,
                                diagnostic = entry.entity_category != null,
                                viewModel = viewModel
                            )
                            if (idx < siblings.lastIndex) {
                                HorizontalDivider(
                                    color = appColors.onMuted.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private val TOGGLE_DOMAINS = setOf("switch", "light", "input_boolean", "fan", "siren", "humidifier", "automation")

@Composable
private fun DeviceEntityRow(entity: HAEntity, diagnostic: Boolean, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val domain = entity.entity_id.substringBefore(".")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).background(appColors.elevated, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            MdiIcon(
                entityMdiIcon(entity),
                contentDescription = null,
                tint = if (diagnostic) appColors.onMuted else MaterialTheme.colorScheme.primary,
                size = 16.dp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            entity.friendlyName ?: entity.entity_id,
            style = MaterialTheme.typography.labelLarge, color = appColors.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))

        // Controllable entities get their control inline; everything else shows the live value.
        when {
            domain in TOGGLE_DOMAINS -> Switch(
                checked = entity.state == "on",
                onCheckedChange = { viewModel.toggleEntity(entity.entity_id) }
            )
            domain == "lock" -> Switch(
                checked = entity.state == "locked",
                onCheckedChange = { viewModel.toggleLock(entity.entity_id) }
            )
            domain == "cover" -> {
                val isOpen = entity.state == "open" || entity.state == "opening"
                Text(entityStateDisplay(entity), style = MaterialTheme.typography.labelMedium, color = appColors.onMuted)
                IconButton(onClick = {
                    viewModel.callService(
                        "cover", if (isOpen) "close_cover" else "open_cover",
                        HAServiceCall(entity_id = entity.entity_id)
                    )
                }) {
                    MdiIcon(
                        if (isOpen) "arrow-down-bold" else "arrow-up-bold",
                        contentDescription = if (isOpen) "Close" else "Open",
                        tint = MaterialTheme.colorScheme.primary, size = 18.dp
                    )
                }
            }
            domain == "button" || domain == "input_button" -> IconButton(onClick = {
                viewModel.callService(domain, "press", HAServiceCall(entity_id = entity.entity_id))
            }) {
                MdiIcon("gesture-tap-button", contentDescription = "Press",
                    tint = MaterialTheme.colorScheme.primary, size = 18.dp)
            }
            domain == "scene" || domain == "script" -> IconButton(onClick = {
                viewModel.callService(domain, "turn_on", HAServiceCall(entity_id = entity.entity_id))
            }) {
                MdiIcon("play", contentDescription = "Run",
                    tint = MaterialTheme.colorScheme.primary, size = 18.dp)
            }
            domain == "select" || domain == "input_select" -> SelectControl(entity, domain, viewModel)
            domain == "number" || domain == "input_number" -> NumberControl(entity, domain, viewModel)
            domain == "text" || domain == "input_text" -> TextControl(entity, domain, viewModel)
            else -> Text(
                entityStateDisplay(entity),
                style = MaterialTheme.typography.labelLarge,
                color = appColors.onSurface, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SelectControl(entity: HAEntity, domain: String, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val options = remember(entity) {
        entity.attributes?.get("options")?.let { el ->
            runCatching { el.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }.getOrNull()
        } ?: emptyList()
    }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = options.isNotEmpty()) { menuOpen = true }
                .padding(vertical = 6.dp)
        ) {
            Text(
                entityStateDisplay(entity),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            MdiIcon("chevron-down", contentDescription = null, tint = appColors.onMuted, size = 14.dp)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        menuOpen = false
                        viewModel.callService(
                            domain, "select_option",
                            HAServiceCall(entity_id = entity.entity_id, option = opt)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberControl(entity: HAEntity, domain: String, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    fun attr(name: String) = entity.attributes?.get(name)?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
    val step = attr("step")?.takeIf { it > 0f } ?: 1f
    val minV = attr("min")
    val maxV = attr("max")
    val current = entity.state.toFloatOrNull()
    fun send(target: Float) {
        var v = target
        minV?.let { v = v.coerceAtLeast(it) }
        maxV?.let { v = v.coerceAtMost(it) }
        val s = if (v % 1f == 0f) v.toInt().toString() else v.toString()
        viewModel.callService(domain, "set_value", HAServiceCall(entity_id = entity.entity_id, value = s))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { current?.let { send(it - step) } }, enabled = current != null) {
            MdiIcon("minus", contentDescription = "Decrease", tint = appColors.onSurface, size = 16.dp)
        }
        Text(
            entityStateDisplay(entity),
            style = MaterialTheme.typography.labelLarge,
            color = appColors.onSurface, fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = { current?.let { send(it + step) } }, enabled = current != null) {
            MdiIcon("plus", contentDescription = "Increase", tint = appColors.onSurface, size = 16.dp)
        }
    }
}

@Composable
private fun TextControl(entity: HAEntity, domain: String, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    Text(
        entity.state.ifBlank { "—" },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable { editing = true }.padding(vertical = 6.dp)
    )
    if (editing) {
        var text by remember(entity.entity_id) { mutableStateOf(entity.state) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(entity.friendlyName ?: entity.entity_id) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.callService(domain, "set_value", HAServiceCall(entity_id = entity.entity_id, value = text))
                    editing = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}

/** Best-effort MDI icon name for an entity: its own icon attribute, else a domain/class default. */
fun entityMdiIcon(entity: HAEntity): String {
    entity.attributes?.get("icon")?.jsonPrimitive?.contentOrNull
        ?.removePrefix("mdi:")?.takeIf { it.isNotBlank() }?.let { return it }
    val domain = entity.entity_id.substringBefore(".")
    return when {
        entity.deviceClass == "battery" -> "battery"
        entity.deviceClass == "temperature" -> "thermometer"
        entity.deviceClass == "humidity" -> "water-percent"
        entity.deviceClass == "power" -> "flash"
        entity.deviceClass == "energy" -> "lightning-bolt"
        entity.deviceClass == "voltage" -> "sine-wave"
        entity.deviceClass == "current" -> "current-ac"
        entity.deviceClass == "signal_strength" -> "wifi"
        entity.deviceClass == "door" || entity.deviceClass == "garage_door" -> "garage"
        entity.deviceClass == "window" -> "window-closed"
        entity.deviceClass == "motion" -> "motion-sensor"
        entity.deviceClass == "illuminance" -> "brightness-5"
        domain == "light" -> "lightbulb"
        domain == "switch" -> "toggle-switch"
        domain == "lock" -> "lock"
        domain == "cover" -> "window-shutter"
        domain == "climate" -> "thermostat"
        domain == "fan" -> "fan"
        domain == "binary_sensor" -> "checkbox-blank-circle-outline"
        domain == "button" || domain == "input_button" -> "gesture-tap-button"
        domain == "update" -> "update"
        domain == "select" || domain == "input_select" -> "format-list-bulleted"
        domain == "number" || domain == "input_number" -> "ray-vertex"
        domain == "text" || domain == "input_text" -> "form-textbox"
        domain == "scene" -> "palette"
        domain == "script" -> "script-text"
        else -> "gauge"
    }
}

/** Human state: numbers keep their unit, on/off & friends get capitalized words. */
fun entityStateDisplay(entity: HAEntity): String {
    val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
    val num = entity.state.toFloatOrNull()
    if (num != null) {
        val v = if (num % 1f == 0f) "%.0f".format(num) else "%.1f".format(num)
        return if (unit.isNullOrBlank()) v else "$v $unit"
    }
    return entity.state.replace("_", " ").replaceFirstChar { it.uppercase() }
}

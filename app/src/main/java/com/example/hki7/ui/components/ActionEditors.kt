@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.hki7.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAArea
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAction
import com.example.hki7.data.HKIActionButton
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.UUID

// Selectable action types shown in the editor. "default" defers to the domain-based heuristic.
private val ACTION_TYPES = listOf(
    "default" to "Default",
    "none" to "None",
    "toggle" to "Toggle",
    "more_info" to "More-info",
    "call_service" to "Service",
    "navigate" to "Navigate",
    "url" to "URL"
)

/** Fixed in-app navigation targets plus one entry per Home Assistant area. */
fun navTargetOptions(areas: List<HAArea>): List<Pair<String, String>> = buildList {
    add("home" to "Home")
    add("rooms" to "Rooms")
    add("energy" to "Energy")
    add("climate" to "Climate")
    add("security" to "Security")
    add("battery" to "Battery")
    add("settings" to "Settings")
    areas.sortedBy { it.name }.forEach { add("room:${it.area_id}" to "Room · ${it.name}") }
}

private fun parseJsonObjectOrNull(text: String): JsonObject? =
    if (text.isBlank()) null else  runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()

private fun entityLabel(entityId: String?, allEntities: List<HAEntity>): String? =
    entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }

/** The MDI slug a custom button actually renders: the configured icon, else the entity's own HA
 *  icon, else the domain default — so "auto" still shows a real (theme-tinted) icon. Null for the
 *  entity-picture sentinel (handled by the caller). */
private fun effectiveButtonIconSlug(button: HKIActionButton, allEntities: List<HAEntity>): String? {
    if (button.icon == ENTITY_PICTURE_ICON) return null
    button.icon?.takeUnless { it.isBlank() }?.let { return it }
    val entity = allEntities.find { it.entity_id == button.entityId }
    return entity?.icon?.substringAfter(":")?.takeUnless { it.isBlank() } ?: entity?.let { defaultEntityIconSlug(it) }
}

/** Editor for a single tap/hold/double-tap [HKIAction]: a type selector plus the fields that
 *  the chosen type needs (service + data, target entity, navigate target, or URL). */
@Composable
fun ActionEditor(
    label: String,
    action: HKIAction,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    onChange: (HKIAction) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    // Free-form service data is edited as raw JSON text; kept locally so invalid in-progress input
    // isn't discarded (and so typing isn't reformatted on each keystroke), parsed into the action as
    // the user types. Keyed on the action type so switching types re-seeds it.
    var dataText by remember(action.type) { mutableStateOf(action.data?.toString() ?: "") }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showMoreInfoPicker by remember { mutableStateOf(false) }
    var navMenuOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label action", style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ACTION_TYPES.forEach { (value, text) ->
                FilterChip(
                    selected = action.type == value,
                    onClick = { onChange(HKIAction(type = value)) },
                    label = { Text(text) }
                )
            }
        }

        when (action.type) {
            "call_service" -> {
                OutlinedTextField(
                    value = action.service ?: "",
                    onValueChange = { onChange(action.copy(service = it.ifBlank { null })) },
                    label = { Text("Service (e.g. light.turn_on)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TargetRow(
                    labelText = "Target entity (optional)",
                    valueText = entityLabel(action.targetEntityId, allEntities),
                    onPick = { showTargetPicker = true },
                    onClear = action.targetEntityId?.let { { onChange(action.copy(targetEntityId = null)) } }
                )
                OutlinedTextField(
                    value = dataText,
                    onValueChange = { dataText = it; onChange(action.copy(data = parseJsonObjectOrNull(it))) },
                    label = { Text("Service data (JSON, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (dataText.isNotBlank() && parseJsonObjectOrNull(dataText) == null) {
                    Text("Invalid JSON", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            "more_info" -> TargetRow(
                labelText = "Show entity (default: this one)",
                valueText = entityLabel(action.moreInfoEntityId, allEntities),
                onPick = { showMoreInfoPicker = true },
                onClear = action.moreInfoEntityId?.let { { onChange(action.copy(moreInfoEntityId = null)) } }
            )
            "toggle" -> TargetRow(
                labelText = "Target entity (default: this one)",
                valueText = entityLabel(action.targetEntityId, allEntities),
                onPick = { showTargetPicker = true },
                onClear = action.targetEntityId?.let { { onChange(action.copy(targetEntityId = null)) } }
            )
            "navigate" -> {
                val options = remember(areas) { navTargetOptions(areas) }
                val current = options.find { it.first == action.navigationTarget }?.second ?: "Select destination"
                Column {
                    OutlinedButton(onClick = { navMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(current)
                    }
                    androidx.compose.material3.DropdownMenu(expanded = navMenuOpen, onDismissRequest = { navMenuOpen = false }) {
                        options.forEach { (value, text) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(text) },
                                onClick = { onChange(action.copy(navigationTarget = value)); navMenuOpen = false }
                            )
                        }
                    }
                }
            }
            "url" -> OutlinedTextField(
                value = action.url ?: "",
                onValueChange = { onChange(action.copy(url = it.ifBlank { null })) },
                label = { Text("URL (https://…)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTargetPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showTargetPicker = false },
            onEntitiesSelected = { ids -> onChange(action.copy(targetEntityId = ids.firstOrNull())); showTargetPicker = false },
            title = "Select Target Entity",
            singleSelect = true,
            preselectedIds = action.targetEntityId?.let { setOf(it) } ?: emptySet()
        )
    }
    if (showMoreInfoPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showMoreInfoPicker = false },
            onEntitiesSelected = { ids -> onChange(action.copy(moreInfoEntityId = ids.firstOrNull())); showMoreInfoPicker = false },
            title = "Select Entity",
            singleSelect = true,
            preselectedIds = action.moreInfoEntityId?.let { setOf(it) } ?: emptySet()
        )
    }
}

@Composable
private fun TargetRow(labelText: String, valueText: String?, onPick: () -> Unit, onClear: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(labelText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valueText ?: "None",
                style = MaterialTheme.typography.bodySmall,
                color = if (valueText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onPick) { Text("Change") }
        if (onClear != null) TextButton(onClick = onClear) { Text("Clear") }
    }
}

/** Editor for a dialog's custom nav-bar buttons. Each button targets an entity and carries its own
 *  tap/hold/double-tap actions. Up to 10 buttons; a hint nudges toward the recommended 4–10 range. */
@Composable
fun CustomButtonsEditor(
    buttons: List<HKIActionButton>,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    onChange: (List<HKIActionButton>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var showAddPicker by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom buttons", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
        Text(
            "Add 4–10 entity buttons to this dialog's nav bar.",
            style = MaterialTheme.typography.labelSmall, color = appColors.onMuted
        )
        if (buttons.isNotEmpty() && buttons.size < 4) {
            Text("Add ${4 - buttons.size} more for a full row.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }

        buttons.forEach { button ->
            val expanded = expandedId == button.id
            Card(colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (button.icon == ENTITY_PICTURE_ICON) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        } else effectiveButtonIconSlug(button, allEntities)?.let { slug ->
                            MdiIcon(slug, tint = appColors.onSurface, size = 18.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            button.name ?: entityLabel(button.entityId, allEntities) ?: button.entityId,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface, fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { expandedId = if (expanded) null else button.id }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Edit", tint = appColors.onSurface)
                        }
                        IconButton(onClick = { onChange(buttons.filterNot { it.id == button.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = appColors.onSurface)
                        }
                    }
                    if (expanded) {
                        fun update(newButton: HKIActionButton) = onChange(buttons.map { if (it.id == button.id) newButton else it })
                        CustomButtonInlineEditor(button = button, allEntities = allEntities, areas = areas, onChange = ::update)
                    }
                }
            }
        }

        if (buttons.size < 10) {
            OutlinedButton(onClick = { showAddPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add button")
            }
        }
    }

    if (showAddPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showAddPicker = false },
            onEntitiesSelected = { ids ->
                val toAdd = ids.take(10 - buttons.size).map { HKIActionButton(id = UUID.randomUUID().toString(), entityId = it) }
                onChange(buttons + toAdd)
                showAddPicker = false
            },
            title = "Add Custom Buttons",
            singleSelect = false
        )
    }
}

@Composable
private fun CustomButtonInlineEditor(
    button: HKIActionButton,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    onChange: (HKIActionButton) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var showIconPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = button.name ?: "",
            onValueChange = { onChange(button.copy(name = it.ifBlank { null })) },
            label = { Text("Name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Preview the effective icon (default when "auto") in the theme color.
            if (button.icon == ENTITY_PICTURE_ICON) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(20.dp))
            } else effectiveButtonIconSlug(button, allEntities)?.let { slug ->
                MdiIcon(slug, tint = appColors.onSurface, size = 20.dp)
            }
            val iconLabel = when {
                button.icon == ENTITY_PICTURE_ICON -> "Entity picture"
                !button.icon.isNullOrBlank() -> button.icon
                else -> "Auto"
            }
            Text("Icon: $iconLabel", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            TextButton(onClick = { showIconPicker = true }) { Text("Change") }
            if (!button.icon.isNullOrBlank()) TextButton(onClick = { onChange(button.copy(icon = null)) }) { Text("Clear") }
        }
        ActionEditor("Tap", button.tapAction, allEntities, areas) { onChange(button.copy(tapAction = it)) }
        ActionEditor("Hold", button.holdAction, allEntities, areas) { onChange(button.copy(holdAction = it)) }
        ActionEditor("Double", button.doubleTapAction, allEntities, areas) { onChange(button.copy(doubleTapAction = it)) }
    }
    if (showIconPicker) {
        MdiIconPickerDialog(
            current = button.icon ?: "",
            onDismiss = { showIconPicker = false },
            onSelect = { slug -> onChange(button.copy(icon = slug.ifEmpty { null })); showIconPicker = false },
            allowEntityPicture = true
        )
    }
}

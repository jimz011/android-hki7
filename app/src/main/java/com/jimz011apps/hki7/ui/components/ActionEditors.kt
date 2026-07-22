@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAActionDefinition
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIAction
import com.jimz011apps.hki7.data.HKIActionButton
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.util.UUID

// Selectable action types shown in the editor. "default" defers to the domain-based heuristic.
private val ACTION_TYPES = listOf(
    "default" to "Default",
    "none" to "None",
    "toggle" to "Toggle",
    "more_info" to "More-info",
    "call_service" to "Action",
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

internal fun withActionDataText(data: JsonObject?, key: String, value: String): JsonObject? {
    val updated = data.orEmpty().toMutableMap()
    if (value.isBlank()) updated.remove(key) else updated[key] = JsonPrimitive(value)
    return JsonObject(updated).takeIf { it.isNotEmpty() }
}

/** Metadata-driven fields for common action data plus a lossless advanced JSON object editor. */
@Composable
internal fun HomeAssistantActionDataEditor(
    definition: HAActionDefinition?,
    data: JsonObject?,
    onDataChange: (JsonObject?) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val textFields = remember(definition) { definition?.fields.orEmpty().filter { it.acceptsText } }
    var showAdvanced by remember(definition?.key) { mutableStateOf(false) }
    var dataText by remember(definition?.key) { mutableStateOf(data?.toString().orEmpty()) }
    var invalidData by remember(definition?.key) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        textFields.forEach { field ->
            val value = (data?.get(field.key) as? JsonPrimitive)?.contentOrNull.orEmpty()
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val updated = withActionDataText(data, field.key, newValue)
                    dataText = updated?.toString().orEmpty()
                    invalidData = false
                    onDataChange(updated)
                },
                label = { Text(field.name + if (field.required) " (required)" else "") },
                supportingText = field.description.takeIf(String::isNotBlank)?.let { description ->
                    { Text(description) }
                },
                minLines = if (field.multiline) 3 else 1,
                singleLine = !field.multiline,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
            Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            Spacer(Modifier.width(8.dp))
            Text("Advanced action data (JSON)")
        }
        if (showAdvanced) {
            Text(
                "Enter the same data object Home Assistant shows in YAML. JSON is used here so values keep their exact type.",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onMuted
            )
            OutlinedTextField(
                value = dataText,
                onValueChange = { text ->
                    dataText = text
                    if (text.isBlank()) {
                        invalidData = false
                        onDataChange(null)
                    } else {
                        val parsed = parseJsonObjectOrNull(text)
                        invalidData = parsed == null
                        if (parsed != null) onDataChange(parsed)
                    }
                },
                label = { Text("Action data object") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                isError = invalidData
            )
            if (invalidData) {
                Text("Enter a valid JSON object", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

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
 *  the chosen type needs (Home Assistant action + data, target entity, navigate target, or URL). */
@Composable
fun ActionEditor(
    label: String,
    action: HKIAction,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    viewModel: MainViewModel,
    onChange: (HKIAction) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var showTargetPicker by remember { mutableStateOf(false) }
    var showMoreInfoPicker by remember { mutableStateOf(false) }
    var navMenuOpen by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }
    var actionDetailsDraft by remember { mutableStateOf<HKIAction?>(null) }
    var actionDefinitions by remember { mutableStateOf<List<HAActionDefinition>>(emptyList()) }
    var actionLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        runCatching { viewModel.getAutomationActions() }
            .onSuccess { actionDefinitions = it; actionLoadError = null }
            .onFailure { actionLoadError = it.message ?: "Could not load Home Assistant actions" }
    }

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
                val selectedAction = actionDefinitions.firstOrNull { it.key == action.service }
                LaunchedEffect(selectedAction?.key) {
                    if (selectedAction != null && !selectedAction.supportsTarget && action.targetMode != "none") {
                        onChange(action.copy(targetEntityId = null, targetMode = "none"))
                    }
                }
                OutlinedButton(
                    onClick = { showActionPicker = true },
                    enabled = actionDefinitions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(
                            selectedAction?.name ?: action.service ?: if (
                                actionDefinitions.isEmpty() && actionLoadError == null
                            ) "Loading actions..." else "Choose an action",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        action.service?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                actionLoadError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                if (selectedAction != null) {
                    OutlinedButton(
                        onClick = { actionDetailsDraft = action },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("Action details", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                actionDetailsSummary(action, selectedAction, allEntities),
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.onMuted
                            )
                        }
                    }
                    if (!selectedAction.supportsTarget && selectedAction.fields.isEmpty() && action.data.isNullOrEmpty()) {
                        Text(
                            "This action has no required target or additional fields.",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.onMuted
                        )
                    }
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
            onEntitiesSelected = { ids ->
                onChange(action.copy(targetEntityId = ids.firstOrNull(), targetMode = "entity"))
                showTargetPicker = false
            },
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
    if (showActionPicker) {
        HomeAssistantActionPickerDialog(
            actions = actionDefinitions,
            selected = action.service.orEmpty(),
            preferredDomain = action.targetEntityId
                ?.takeIf { it.contains('.') }
                ?.substringBefore('.'),
            onDismiss = { showActionPicker = false },
            onSelected = { selected ->
                val sameAction = action.service == selected.key
                val updated = action.copy(
                    service = selected.key,
                    targetEntityId = if (sameAction) action.targetEntityId else null,
                    targetMode = when {
                        !selected.supportsTarget -> "none"
                        sameAction && action.targetMode != "none" -> action.targetMode
                        else -> "owner"
                    },
                    data = if (sameAction) action.data else null
                )
                onChange(updated)
                showActionPicker = false
                actionDetailsDraft = updated
            }
        )
    }
    actionDetailsDraft?.let { draft ->
        val definition = actionDefinitions.firstOrNull { it.key == draft.service }
        if (definition != null) {
            HomeAssistantActionDetailsDialog(
                action = draft,
                definition = definition,
                allEntities = allEntities,
                onDismiss = { actionDetailsDraft = null },
                onSave = { updated ->
                    onChange(updated)
                    actionDetailsDraft = null
                }
            )
        }
    }
}

private fun actionDetailsSummary(
    action: HKIAction,
    definition: HAActionDefinition,
    allEntities: List<HAEntity>
): String {
    val target = when {
        !definition.supportsTarget || action.targetMode == "none" -> "No target"
        action.targetMode == "entity" -> entityLabel(action.targetEntityId, allEntities) ?: "Choose target"
        else -> "This entity"
    }
    val dataCount = action.data?.size ?: 0
    return if (dataCount > 0) "$target · $dataCount data value${if (dataCount == 1) "" else "s"}" else target
}

@Composable
private fun HomeAssistantActionDetailsDialog(
    action: HKIAction,
    definition: HAActionDefinition,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIAction) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var draft by remember(action, definition.key) { mutableStateOf(action) }
    var showTargetPicker by remember { mutableStateOf(false) }

    ModernAlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = { ModernSettingsDialogTitle(definition.name, "Target and action data") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(definition.key, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                if (definition.supportsTarget) {
                    SettingsSubcategory("Target", "Choose what this action should control")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = draft.targetMode == "owner" && draft.targetEntityId == null,
                            onClick = { draft = draft.copy(targetEntityId = null, targetMode = "owner") },
                            label = { Text("This entity") }
                        )
                        FilterChip(
                            selected = draft.targetMode == "entity" && draft.targetEntityId != null,
                            onClick = { showTargetPicker = true },
                            label = { Text(entityLabel(draft.targetEntityId, allEntities) ?: "Choose entity") }
                        )
                        FilterChip(
                            selected = draft.targetMode == "none",
                            onClick = { draft = draft.copy(targetEntityId = null, targetMode = "none") },
                            label = { Text("No target") }
                        )
                    }
                } else {
                    Text(
                        "This action does not use an entity target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                }

                SettingsSubcategory("Action data", "Optional fields passed to Home Assistant")
                HomeAssistantActionDataEditor(
                    definition = definition,
                    data = draft.data,
                    onDataChange = { draft = draft.copy(data = it) }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showTargetPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showTargetPicker = false },
            onEntitiesSelected = { ids ->
                draft = draft.copy(targetEntityId = ids.firstOrNull(), targetMode = "entity")
                showTargetPicker = false
            },
            title = "Select Target Entity",
            singleSelect = true,
            preselectedIds = draft.targetEntityId?.let { setOf(it) } ?: emptySet()
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
    viewModel: MainViewModel,
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
                        CustomButtonInlineEditor(button = button, allEntities = allEntities, areas = areas, viewModel = viewModel, onChange = ::update)
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
    viewModel: MainViewModel,
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
        ActionEditor("Tap", button.tapAction, allEntities, areas, viewModel) { onChange(button.copy(tapAction = it)) }
        ActionEditor("Hold", button.holdAction, allEntities, areas, viewModel) { onChange(button.copy(holdAction = it)) }
        ActionEditor("Double", button.doubleTapAction, allEntities, areas, viewModel) { onChange(button.copy(doubleTapAction = it)) }
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

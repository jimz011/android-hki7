@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
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
import com.jimz011apps.hki7.data.HKICustomPopup
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.util.UUID

// Values are stable action tokens; labels follow the app locale.
@Composable
private fun actionTypes() = listOf(
    "default" to stringResource(R.string.dlg_default),
    "none" to stringResource(R.string.dlg_none),
    "toggle" to stringResource(R.string.dlg_toggle),
    "more_info" to stringResource(R.string.dlg_more_information),
    "call_service" to stringResource(R.string.dlg_action),
    "navigate" to stringResource(R.string.dlg_navigate),
    "url" to stringResource(R.string.dlg_url),
    "custom_popup" to stringResource(R.string.popup_custom_popup),
)

/** Fixed in-app navigation targets plus one entry per Home Assistant area. */
@Composable
fun navTargetOptions(areas: List<HAArea>): List<Pair<String, String>> =
    listOf(
        "home" to stringResource(R.string.dlg_home),
        "rooms" to stringResource(R.string.dlg_rooms),
        "energy" to stringResource(R.string.dlg_energy),
        "climate" to stringResource(R.string.dlg_climate),
        "security" to stringResource(R.string.dlg_security),
        "battery" to stringResource(R.string.dlg_battery),
        "settings" to stringResource(R.string.dlg_settings),
    ) + areas.sortedBy { it.name }.map {
        "room:${it.area_id}" to stringResource(R.string.dlg_room_value, it.name)
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
                label = { Text(field.name + if (field.required) stringResource(R.string.dlg_required) else "") },
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
            Text(stringResource(R.string.dlg_advanced_action_data_json))
        }
        if (showAdvanced) {
            Text(
                stringResource(R.string.dlg_enter_the_same_data_object_home_assistant_shows_in),
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
                label = { Text(stringResource(R.string.dlg_action_data_object)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                isError = invalidData
            )
            if (invalidData) {
                Text(stringResource(R.string.dlg_enter_a_valid_json_object), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
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
    val actionLoadFallback = stringResource(R.string.dlg_could_not_load_home_assistant_actions)

    LaunchedEffect(viewModel) {
        runCatching { viewModel.getAutomationActions() }
            .onSuccess { actionDefinitions = it; actionLoadError = null }
            .onFailure { actionLoadError = it.message ?: actionLoadFallback }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.dlg_action_count, label), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actionTypes().forEach { (value, text) ->
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
                            ) stringResource(R.string.dlg_loading_actions) else stringResource(R.string.dlg_choose_an_action),
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
                            Text(stringResource(R.string.dlg_action_details), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                actionDetailsSummary(action, selectedAction, allEntities),
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.onMuted
                            )
                        }
                    }
                    if (!selectedAction.supportsTarget && selectedAction.fields.isEmpty() && action.data.isNullOrEmpty()) {
                        Text(
                            stringResource(R.string.dlg_this_action_has_no_required_target_or_additional_fields),
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.onMuted
                        )
                    }
                }
            }
            "more_info" -> TargetRow(
                labelText = stringResource(R.string.dlg_show_entity_default_this_one),
                valueText = entityLabel(action.moreInfoEntityId, allEntities),
                onPick = { showMoreInfoPicker = true },
                onClear = action.moreInfoEntityId?.let { { onChange(action.copy(moreInfoEntityId = null)) } }
            )
            "toggle" -> TargetRow(
                labelText = stringResource(R.string.dlg_target_entity_default_this_one),
                valueText = entityLabel(action.targetEntityId, allEntities),
                onPick = { showTargetPicker = true },
                onClear = action.targetEntityId?.let { { onChange(action.copy(targetEntityId = null)) } }
            )
            "navigate" -> {
                val options = navTargetOptions(areas)
                val current = options.find { it.first == action.navigationTarget }?.second
                    ?: stringResource(R.string.dlg_select_destination)
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
                label = { Text(stringResource(R.string.dlg_url_https)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            "custom_popup" -> CustomPopupActionEditor(
                action = action,
                allEntities = allEntities,
                viewModel = viewModel,
                onChange = onChange
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
            title = stringResource(R.string.dlg_select_target_entity),
            singleSelect = true,
            preselectedIds = action.targetEntityId?.let { setOf(it) } ?: emptySet()
        )
    }
    if (showMoreInfoPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showMoreInfoPicker = false },
            onEntitiesSelected = { ids -> onChange(action.copy(moreInfoEntityId = ids.firstOrNull())); showMoreInfoPicker = false },
            title = stringResource(R.string.dlg_select_entity),
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

/** Picks (or creates) the popup a `custom_popup` action opens. Popups are shared across the
 *  dashboard, so this only selects one and offers a shortcut into it; the widgets themselves are
 *  arranged inside the popup dialog, where the whole widget canvas is available. */
@Composable
private fun CustomPopupActionEditor(
    action: HKIAction,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    onChange: (HKIAction) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val popups by viewModel.customPopups.collectAsState()
    val selected = popups.find { it.id == action.popupId }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsDraft by remember { mutableStateOf<HKICustomPopup?>(null) }
    val newPopupName = stringResource(R.string.popup_default_name)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            OutlinedButton(
                onClick = { menuOpen = true },
                enabled = popups.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                selected?.icon?.takeUnless { it.isBlank() }?.let { slug ->
                    MdiIcon(slug, tint = appColors.onSurface, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    selected?.name ?: stringResource(
                        if (popups.isEmpty()) R.string.popup_none_yet else R.string.popup_choose
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                popups.forEach { popup ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(popup.name) },
                        onClick = { onChange(action.copy(popupId = popup.id)); menuOpen = false }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val created = viewModel.createCustomPopup(newPopupName)
                    onChange(action.copy(popupId = created.id))
                    settingsDraft = created
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.popup_new))
            }
            if (selected != null) {
                OutlinedButton(onClick = { settingsDraft = selected }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dlg_edit))
                }
            }
        }
        if (selected != null) {
            OutlinedButton(
                onClick = { viewModel.openCustomPopup(selected.id, startInEditMode = true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.popup_edit_contents))
            }
            Text(
                stringResource(R.string.popup_shared_hint),
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onMuted
            )
        }
    }

    settingsDraft?.let { draft ->
        CustomPopupSettingsDialog(
            popup = draft,
            allEntities = allEntities,
            onDismiss = { settingsDraft = null },
            onSave = { updated -> viewModel.updateCustomPopup(updated); settingsDraft = null },
            onDelete = {
                viewModel.deleteCustomPopup(draft.id)
                if (action.popupId == draft.id) onChange(action.copy(popupId = null))
                settingsDraft = null
            }
        )
    }
}

/** Name, icon, and optional status entity of a popup. The status entity also decides whether the
 *  dialog offers its history/activity view. Shared by the action editor and
 *  Settings › Appearance › Popups. */
@Composable
fun CustomPopupSettingsDialog(
    popup: HKICustomPopup,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKICustomPopup) -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var draft by remember(popup.id) { mutableStateOf(popup) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }

    ModernAlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                stringResource(R.string.popup_custom_popup),
                stringResource(R.string.popup_settings_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text(stringResource(R.string.popup_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.icon?.takeUnless { it.isBlank() }?.let { slug ->
                        MdiIcon(slug, tint = appColors.onSurface, size = 20.dp)
                    }
                    Text(
                        stringResource(
                            R.string.dlg_icon_value,
                            draft.icon?.takeUnless { it.isBlank() } ?: stringResource(R.string.dlg_none)
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.dlg_change)) }
                    if (!draft.icon.isNullOrBlank()) {
                        TextButton(onClick = { draft = draft.copy(icon = null) }) { Text(stringResource(R.string.dlg_clear)) }
                    }
                }
                TargetRow(
                    labelText = stringResource(R.string.popup_status_entity),
                    valueText = entityLabel(draft.statusEntityId, allEntities),
                    onPick = { showStatusPicker = true },
                    onClear = draft.statusEntityId?.let { { draft = draft.copy(statusEntityId = null) } }
                )
                Text(
                    stringResource(R.string.popup_status_entity_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted
                )
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.popup_delete))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(draft.copy(name = draft.name.ifBlank { popup.name })) }
            ) { Text(stringResource(R.string.dlg_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    )

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = draft.icon ?: "",
            onDismiss = { showIconPicker = false },
            onSelect = { slug -> draft = draft.copy(icon = slug.ifEmpty { null }); showIconPicker = false }
        )
    }
    if (showStatusPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showStatusPicker = false },
            onEntitiesSelected = { ids -> draft = draft.copy(statusEntityId = ids.firstOrNull()); showStatusPicker = false },
            title = stringResource(R.string.popup_status_entity),
            singleSelect = true,
            preselectedIds = draft.statusEntityId?.let { setOf(it) } ?: emptySet()
        )
    }
}

@Composable
private fun actionDetailsSummary(
    action: HKIAction,
    definition: HAActionDefinition,
    allEntities: List<HAEntity>
): String {
    val target = when {
        !definition.supportsTarget || action.targetMode == "none" -> stringResource(R.string.dlg_no_target)
        action.targetMode == "entity" -> entityLabel(action.targetEntityId, allEntities)
            ?: stringResource(R.string.dlg_choose_target)
        else -> stringResource(R.string.dlg_this_entity)
    }
    val dataCount = action.data?.size ?: 0
    return if (dataCount > 0) {
        pluralStringResource(
            R.plurals.dlg_action_data_value_summary,
            dataCount,
            target,
            dataCount,
        )
    } else {
        target
    }
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
        title = { ModernSettingsDialogTitle(definition.name, stringResource(R.string.dlg_target_and_action_data)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(definition.key, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                if (definition.supportsTarget) {
                    SettingsSubcategory(stringResource(R.string.dlg_target), stringResource(R.string.dlg_choose_what_this_action_should_control))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = draft.targetMode == "owner" && draft.targetEntityId == null,
                            onClick = { draft = draft.copy(targetEntityId = null, targetMode = "owner") },
                            label = { Text(stringResource(R.string.dlg_this_entity)) }
                        )
                        FilterChip(
                            selected = draft.targetMode == "entity" && draft.targetEntityId != null,
                            onClick = { showTargetPicker = true },
                            label = { Text(entityLabel(draft.targetEntityId, allEntities) ?: stringResource(R.string.dlg_choose_entity)) }
                        )
                        FilterChip(
                            selected = draft.targetMode == "none",
                            onClick = { draft = draft.copy(targetEntityId = null, targetMode = "none") },
                            label = { Text(stringResource(R.string.dlg_no_target)) }
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.dlg_this_action_does_not_use_an_entity_target),
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                }

                SettingsSubcategory(stringResource(R.string.dlg_action_data), stringResource(R.string.dlg_optional_fields_passed_to_home_assistant))
                HomeAssistantActionDataEditor(
                    definition = definition,
                    data = draft.data,
                    onDataChange = { draft = draft.copy(data = it) }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.dlg_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    )

    if (showTargetPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showTargetPicker = false },
            onEntitiesSelected = { ids ->
                draft = draft.copy(targetEntityId = ids.firstOrNull(), targetMode = "entity")
                showTargetPicker = false
            },
            title = stringResource(R.string.dlg_select_target_entity),
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
                valueText ?: stringResource(R.string.dlg_none),
                style = MaterialTheme.typography.bodySmall,
                color = if (valueText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onPick) { Text(stringResource(R.string.dlg_change)) }
        if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.dlg_clear)) }
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
        Text(stringResource(R.string.dlg_custom_buttons), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
        Text(
            stringResource(R.string.dlg_add_4_10_entity_buttons_to_this_dialog_s),
            style = MaterialTheme.typography.labelSmall, color = appColors.onMuted
        )
        if (buttons.isNotEmpty() && buttons.size < 4) {
            Text(stringResource(R.string.dlg_add_more_for_a_full_row, 4 - buttons.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = stringResource(R.string.dlg_edit), tint = appColors.onSurface)
                        }
                        IconButton(onClick = { onChange(buttons.filterNot { it.id == button.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.dlg_remove), tint = appColors.onSurface)
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
                Text(stringResource(R.string.dlg_add_button))
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
            title = stringResource(R.string.dlg_add_custom_buttons),
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
            label = { Text(stringResource(R.string.dlg_name_optional)) },
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
                button.icon == ENTITY_PICTURE_ICON -> stringResource(R.string.dlg_entity_picture)
                !button.icon.isNullOrBlank() -> button.icon
                else -> stringResource(R.string.dlg_auto)
            }
            Text(stringResource(R.string.dlg_icon_value, iconLabel), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.dlg_change)) }
            if (!button.icon.isNullOrBlank()) TextButton(onClick = { onChange(button.copy(icon = null)) }) { Text(stringResource(R.string.dlg_clear)) }
        }
        ActionEditor(stringResource(R.string.dlg_tap), button.tapAction, allEntities, areas, viewModel) { onChange(button.copy(tapAction = it)) }
        ActionEditor(stringResource(R.string.dlg_hold), button.holdAction, allEntities, areas, viewModel) { onChange(button.copy(holdAction = it)) }
        ActionEditor(stringResource(R.string.dlg_double_tap), button.doubleTapAction, allEntities, areas, viewModel) { onChange(button.copy(doubleTapAction = it)) }
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

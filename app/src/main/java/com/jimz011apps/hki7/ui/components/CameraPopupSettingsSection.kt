package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.CameraPopupRule
import com.jimz011apps.hki7.data.CameraPopupSettings
import com.jimz011apps.hki7.data.DEFAULT_CAMERA_POPUP_TIMEOUT_SECONDS
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.MAX_CAMERA_POPUP_TIMEOUT_SECONDS
import com.jimz011apps.hki7.data.MIN_CAMERA_POPUP_TIMEOUT_SECONDS
import com.jimz011apps.hki7.data.displayName
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlin.math.roundToInt

@Composable
fun CameraPopupSettingsSection(
    settings: CameraPopupSettings,
    entities: List<HAEntity>,
    onChange: (CameraPopupSettings) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var triggerPickerFor by remember { mutableStateOf<String?>(null) }
    var cameraPickerFor by remember { mutableStateOf<String?>(null) }
    var helperPickerFor by remember { mutableStateOf<String?>(null) }

    SettingsGroup {
        Text(
            stringResource(R.string.settings_camera_popup_title),
            color = appColors.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.settings_camera_popup_hint),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_camera_popup_master),
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.onSurface,
                )
                Text(
                    stringResource(R.string.settings_camera_popup_master_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted,
                )
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = { onChange(settings.copy(enabled = it)) },
            )
        }
        settings.rules.forEach { rule ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { editingId = if (editingId == rule.id) null else rule.id },
                    ) {
                        Text(rule.displayName(), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                        Text(
                            ruleSummary(rule, entities),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { enabled ->
                            onChange(settings.copy(rules = settings.rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it }))
                        },
                    )
                    IconButton(
                        onClick = {
                            if (editingId == rule.id) editingId = null
                            onChange(settings.copy(rules = settings.rules.filterNot { it.id == rule.id }))
                        }
                    ) {
                        Icon(Icons.Default.Delete, stringResource(R.string.settings_camera_popup_delete), tint = appColors.onMuted)
                    }
                }
                if (editingId == rule.id) {
                    CameraPopupRuleEditor(
                        rule = rule,
                        entities = entities,
                        onChange = { updated ->
                            onChange(settings.copy(rules = settings.rules.map { if (it.id == updated.id) updated else it }))
                        },
                        onPickTrigger = { triggerPickerFor = rule.id },
                        onPickCamera = { cameraPickerFor = rule.id },
                        onPickHelper = { helperPickerFor = rule.id },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = {
                val created = CameraPopupRule()
                onChange(settings.copy(rules = settings.rules + created))
                editingId = created.id
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = itemCornerShape(),
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_camera_popup_add))
        }
    }

    val pickerRuleId = triggerPickerFor ?: cameraPickerFor ?: helperPickerFor
    if (pickerRuleId != null) {
        val candidates = when {
            triggerPickerFor != null -> entities.filter { entity ->
                val domain = entity.entity_id.substringBefore('.')
                domain == "binary_sensor" || domain == "event"
            }
            cameraPickerFor != null -> entities.filter { it.entity_id.startsWith("camera.") }
            else -> entities.filter { it.entity_id.startsWith("input_boolean.") }
        }
        val current = settings.rules.firstOrNull { it.id == pickerRuleId }
        AdvancedEntitySearchDialog(
            allEntities = candidates,
            title = when {
                triggerPickerFor != null -> stringResource(R.string.settings_camera_popup_pick_trigger)
                cameraPickerFor != null -> stringResource(R.string.settings_camera_popup_pick_camera)
                else -> stringResource(R.string.settings_camera_popup_pick_helper)
            },
            singleSelect = true,
            preselectedIds = setOfNotNull(
                when {
                    triggerPickerFor != null -> current?.triggerEntityId
                    cameraPickerFor != null -> current?.cameraEntityId
                    else -> current?.enableEntityId
                }?.takeIf { it.isNotBlank() }
            ),
            onDismiss = {
                triggerPickerFor = null
                cameraPickerFor = null
                helperPickerFor = null
            },
            onEntitiesSelected = { ids ->
                val selected = ids.firstOrNull().orEmpty()
                onChange(
                    settings.copy(
                        rules = settings.rules.map { rule ->
                            if (rule.id != pickerRuleId) rule
                            else when {
                                triggerPickerFor != null -> rule.copy(triggerEntityId = selected)
                                cameraPickerFor != null -> rule.copy(cameraEntityId = selected)
                                else -> rule.copy(enableEntityId = selected.takeIf { it.isNotBlank() })
                            }
                        }
                    )
                )
                triggerPickerFor = null
                cameraPickerFor = null
                helperPickerFor = null
            },
        )
    }
}

@Composable
private fun CameraPopupRuleEditor(
    rule: CameraPopupRule,
    entities: List<HAEntity>,
    onChange: (CameraPopupRule) -> Unit,
    onPickTrigger: () -> Unit,
    onPickCamera: () -> Unit,
    onPickHelper: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var timeout by remember(rule.id, rule.timeoutSeconds) {
        mutableFloatStateOf(rule.timeoutSeconds.toFloat())
    }
    OutlinedTextField(
        value = rule.name,
        onValueChange = { onChange(rule.copy(name = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.settings_camera_popup_name)) },
        singleLine = true,
    )
    EntityPickRow(
        label = stringResource(R.string.settings_camera_popup_trigger),
        value = entityLabel(entities, rule.triggerEntityId, stringResource(R.string.settings_camera_popup_choose)),
        onClick = onPickTrigger,
    )
    EntityPickRow(
        label = stringResource(R.string.settings_camera_popup_camera),
        value = entityLabel(entities, rule.cameraEntityId, stringResource(R.string.settings_camera_popup_choose)),
        onClick = onPickCamera,
    )
    Text(
        stringResource(R.string.settings_camera_popup_timeout, timeout.roundToInt()),
        color = appColors.onSurface,
        style = MaterialTheme.typography.titleSmall,
    )
    HKISlider(
        value = timeout,
        onValueChange = { timeout = it },
        onValueChangeFinished = { onChange(rule.copy(timeoutSeconds = timeout.roundToInt())) },
        valueRange = MIN_CAMERA_POPUP_TIMEOUT_SECONDS.toFloat()..MAX_CAMERA_POPUP_TIMEOUT_SECONDS.toFloat(),
        steps = MAX_CAMERA_POPUP_TIMEOUT_SECONDS - MIN_CAMERA_POPUP_TIMEOUT_SECONDS - 1,
    )
    EntityPickRow(
        label = stringResource(R.string.settings_camera_popup_helper),
        value = entityLabel(
            entities,
            rule.enableEntityId.orEmpty(),
            stringResource(R.string.settings_camera_popup_helper_none),
        ),
        onClick = onPickHelper,
    )
    if (!rule.enableEntityId.isNullOrBlank()) {
        TextButton(onClick = { onChange(rule.copy(enableEntityId = null)) }) {
            Text(stringResource(R.string.settings_camera_popup_helper_clear))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = rule.timeAfter.orEmpty(),
            onValueChange = { onChange(rule.copy(timeAfter = it.ifBlank { null })) },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.settings_camera_popup_after)) },
            placeholder = { Text(stringResource(R.string.settings_camera_popup_time_example_after)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = rule.timeBefore.orEmpty(),
            onValueChange = { onChange(rule.copy(timeBefore = it.ifBlank { null })) },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.settings_camera_popup_before)) },
            placeholder = { Text(stringResource(R.string.settings_camera_popup_time_example_before)) },
            singleLine = true,
        )
    }
    Text(
        stringResource(R.string.settings_camera_popup_time_hint),
        color = appColors.onMuted,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun EntityPickRow(label: String, value: String, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun entityLabel(entities: List<HAEntity>, entityId: String, fallback: String): String {
    if (entityId.isBlank()) return fallback
    return entities.firstOrNull { it.entity_id == entityId }?.friendlyName ?: entityId
}

private fun ruleSummary(rule: CameraPopupRule, entities: List<HAEntity>): String {
    val trigger = entityLabel(entities, rule.triggerEntityId, "—")
    val camera = entityLabel(entities, rule.cameraEntityId, "—")
    val seconds = rule.timeoutSeconds.takeIf { it > 0 } ?: DEFAULT_CAMERA_POPUP_TIMEOUT_SECONDS
    return "$trigger → $camera · ${seconds}s"
}

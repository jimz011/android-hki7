package com.jimz011apps.hki7.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.*
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.ActionEditor
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.defaultEntityIconSlug
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

/** What a quick action is called on a compact surface: the override, else the entity's own name. */
internal fun quickActionLabel(quickAction: HKIQuickAction, entity: HAEntity?): String =
    quickAction.name?.takeIf { it.isNotBlank() }
        ?: entity?.friendlyName?.takeIf { it.isNotBlank() }
        ?: quickAction.entityId

/** The icon slug to draw for a quick action, falling back to the same domain icon the dashboard
 *  would pick so an entry the user never gave an icon still looks like the thing it controls. */
internal fun quickActionIconSlug(quickAction: HKIQuickAction, entity: HAEntity?): String =
    quickAction.icon?.takeIf { it.isNotBlank() }
        ?: entity?.let { defaultEntityIconSlug(it) }
        ?: "lightning-bolt"

/** One row of the quick-action list: what it is, whether the car shows it, and a way in. */
@Composable
internal fun QuickActionRow(
    quickAction: HKIQuickAction,
    entity: HAEntity?,
    carAvailable: Boolean,
    onToggleCar: (Boolean) -> Unit,
    onToggleWatch: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    // An entry whose action resolves to nothing would silently do nothing in the car, where there
    // is no way to find out why. Say so here, the only place it can be fixed.
    val unsupported = quickAction.resolvedKind() == QuickActionKind.UNSUPPORTED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MdiIcon(
            quickActionIconSlug(quickAction, entity),
            tint = if (unsupported) appColors.onMuted else appColors.onSurface,
            size = 22.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(
                quickActionLabel(quickAction, entity),
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unsupported) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        stringResource(R.string.quick_actions_unsupported),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    quickAction.entityId,
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Two surfaces, two toggles. Icon buttons rather than a pair of switches: a row this
        // narrow cannot hold two switches and a label, and the car/watch glyphs say which is which
        // without one. Dimmed means hidden from that surface.
        SurfaceToggle(
            icon = Icons.Default.DirectionsCar,
            label = stringResource(R.string.quick_actions_show_in_car),
            enabled = quickAction.showInCar,
            // Not merely off: unreachable on this install, so it must not read as a setting the
            // user simply has not switched on yet.
            available = carAvailable,
            onToggle = onToggleCar,
        )
        SurfaceToggle(
            icon = Icons.Default.Watch,
            label = stringResource(R.string.quick_actions_show_on_watch),
            enabled = quickAction.showOnWatch,
            onToggle = onToggleWatch,
        )
    }
}

/** One surface's on/off state for a quick action, as a tappable icon. */
@Composable
private fun SurfaceToggle(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    /** False when this surface cannot be reached at all on this install. */
    available: Boolean = true,
) {
    val appColors = LocalHKIAppColors.current
    IconButton(onClick = { onToggle(!enabled) }, enabled = available) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                !available -> appColors.onMuted.copy(alpha = 0.3f)
                enabled -> appColors.accent
                else -> appColors.onMuted.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Editor for one quick action: its label, its icon, and what a tap does.
 *
 * Reuses [ActionEditor] rather than growing a parallel editor, so the full Home Assistant action
 * picker and its per-field editors come along. The type list is narrowed to the three that mean
 * anything away from the dashboard — the others route to in-app UI that Android Auto cannot draw.
 */
@Composable
internal fun QuickActionEditDialog(
    quickAction: HKIQuickAction,
    entity: HAEntity?,
    carAvailable: Boolean,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIQuickAction) -> Unit,
    onDelete: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var draft by remember(quickAction.id) { mutableStateOf(quickAction) }
    var showIconPicker by remember { mutableStateOf(false) }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = draft.icon.orEmpty(),
            onDismiss = { showIconPicker = false },
            onSelect = { slug ->
                draft = draft.copy(icon = slug.takeIf { it.isNotBlank() })
                showIconPicker = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_actions_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    draft.entityId,
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = draft.name.orEmpty(),
                    onValueChange = { draft = draft.copy(name = it.takeIf(String::isNotBlank)) },
                    label = { Text(stringResource(R.string.quick_actions_name)) },
                    placeholder = { Text(entity?.friendlyName ?: draft.entityId) },
                    supportingText = { Text(stringResource(R.string.quick_actions_name_hint)) },
                    singleLine = true,
                    colors = settingsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { showIconPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MdiIcon(quickActionIconSlug(draft, entity), size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.quick_actions_icon), modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                ActionEditor(
                    label = stringResource(R.string.quick_actions_when_tapped),
                    action = draft.action,
                    allEntities = allEntities,
                    areas = areas,
                    viewModel = viewModel,
                    allowedTypes = listOf("default", "toggle", "call_service"),
                    onChange = { draft = draft.copy(action = it) },
                )
                if (draft.resolvedKind() == QuickActionKind.UNSUPPORTED) {
                    Text(
                        stringResource(R.string.quick_actions_unsupported),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.quick_actions_show_in_car),
                        color = if (carAvailable) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = draft.showInCar,
                        enabled = carAvailable,
                        onCheckedChange = { draft = draft.copy(showInCar = it) },
                    )
                }
                if (!carAvailable) {
                    Text(
                        stringResource(R.string.quick_actions_car_needs_play),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.quick_actions_show_on_watch),
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = draft.showOnWatch,
                        onCheckedChange = { draft = draft.copy(showOnWatch = it) },
                    )
                }
                if (!draft.showOnWatch && (!draft.showInCar || !carAvailable)) {
                    Text(
                        stringResource(R.string.quick_actions_hidden_everywhere),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.dlg_save)) } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.quick_actions_remove), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) }
            }
        },
    )
}

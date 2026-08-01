package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.jimz011apps.hki7.data.HKIButtonConfig
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A Material 3 date-and-time picker in the same style as the Energy view's date picker: the standard
 * [DatePickerDialog] with a Date/Time toggle so the caller gets a full [LocalDateTime] back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val base = remember { (initial ?: LocalDateTime.now()).withSecond(0).withNano(0) }
    // The date picker stores UTC-midnight millis, so keep the whole round-trip on UTC to avoid a
    // timezone shifting the chosen day.
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = base.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val timeState = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
    var tab by remember { mutableStateOf("date") }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = dateState.selectedDateMillis ?: return@TextButton
                val date: LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onConfirm(LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)))
            }) { Text(stringResource(R.string.dlg_done)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.dlg_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) }
            }
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tab == "date", onClick = { tab = "date" }, label = { Text(stringResource(R.string.dlg_date)) })
                FilterChip(selected = tab == "time", onClick = { tab = "time" }, label = { Text(stringResource(R.string.dlg_time)) })
            }
            if (tab == "date") {
                DatePicker(state = dateState)
            } else {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        }
    }
}

/** Portable hide/schedule/condition rule for a button, badge, or widget (see `isVisibleAt`). */
data class VisibilitySpec(
    val hidden: Boolean = false,
    val start: String? = null,
    val end: String? = null,
    val rangeMode: String = "show",
    val recurrence: String = "none",
    /** Optional entity-state condition, like a Home Assistant conditional card: when set, this item
     * is also gated on whether [conditionEntityId]'s current state does/doesn't equal
     * [conditionState], per [conditionNegate]. */
    val conditionEntityId: String? = null,
    val conditionState: String? = null,
    val conditionNegate: Boolean = false,
)

/** Human-readable label for a scheduled bound stored as an ISO local date-time. */
@Composable
fun formatVisibilityBound(iso: String?): String {
    if (iso.isNullOrBlank()) return stringResource(R.string.dlg_any)
    return runCatching {
        LocalDateTime.parse(iso).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        )
    }.getOrDefault(iso)
}

/**
 * Inline hide/schedule editor embedded in a button's or badge's Appearance settings. Mode chips
 * (Always / Hidden / Scheduled); the scheduled mode exposes a visible/hidden window with optional
 * daily/weekly/monthly/yearly recurrence via the graphical [DateTimePickerDialog].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisibilityEditor(spec: VisibilitySpec, onChange: (VisibilitySpec) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val mode = when {
        !spec.start.isNullOrBlank() || !spec.end.isNullOrBlank() -> "scheduled"
        spec.hidden -> "hidden"
        else -> "always"
    }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "always", onClick = { onChange(VisibilitySpec()) }, label = { Text(stringResource(R.string.dlg_always)) })
            FilterChip(selected = mode == "hidden", onClick = { onChange(VisibilitySpec(hidden = true)) }, label = { Text(stringResource(R.string.dlg_hidden)) })
            FilterChip(
                selected = mode == "scheduled",
                onClick = { onChange(spec.copy(hidden = false, start = spec.start ?: LocalDateTime.now().withSecond(0).withNano(0).toString())) },
                label = { Text(stringResource(R.string.dlg_scheduled)) }
            )
        }
        if (mode == "scheduled") {
            Text(stringResource(R.string.dlg_when), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = spec.rangeMode == "show", onClick = { onChange(spec.copy(rangeMode = "show")) }, label = { Text(stringResource(R.string.dlg_visible_during)) })
                FilterChip(selected = spec.rangeMode == "hide", onClick = { onChange(spec.copy(rangeMode = "hide")) }, label = { Text(stringResource(R.string.dlg_hidden_during)) })
            }
            Text(stringResource(R.string.dlg_repeat), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "none" to stringResource(R.string.dlg_once),
                    "daily" to stringResource(R.string.dlg_daily),
                    "weekly" to stringResource(R.string.dlg_weekly),
                    "monthly" to stringResource(R.string.dlg_monthly),
                    "yearly" to stringResource(R.string.dlg_yearly),
                ).forEach { (value, txt) ->
                    FilterChip(selected = spec.recurrence == value, onClick = { onChange(spec.copy(recurrence = value)) }, label = { Text(txt) })
                }
            }
            OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.dlg_start, formatVisibilityBound(spec.start)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.dlg_end, formatVisibilityBound(spec.end)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                when (spec.recurrence) {
                    "daily" -> stringResource(R.string.dlg_repeats_every_day_only_the_time_of_day_is)
                    "weekly" -> stringResource(R.string.dlg_repeats_every_week_only_the_weekday_and_time_are)
                    "monthly" -> stringResource(R.string.dlg_repeats_every_month_only_the_day_of_the_month)
                    "yearly" -> stringResource(R.string.dlg_repeats_every_year_the_year_is_ignored_so_e)
                    else -> stringResource(R.string.dlg_the_exact_dates_you_pick_leave_a_bound_as)
                },
                style = MaterialTheme.typography.bodySmall,
                color = appColors.onMuted
            )
        }
        androidx.compose.material3.HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
        Text(stringResource(R.string.dlg_condition), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
        Text(
            stringResource(R.string.dlg_condition_hint),
            style = MaterialTheme.typography.bodySmall,
            color = appColors.onMuted
        )
        OutlinedTextField(
            value = spec.conditionEntityId.orEmpty(),
            onValueChange = { onChange(spec.copy(conditionEntityId = it.ifBlank { null })) },
            label = { Text(stringResource(R.string.dlg_condition_entity_id)) },
            placeholder = { Text("light.living_room") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (!spec.conditionEntityId.isNullOrBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !spec.conditionNegate, onClick = { onChange(spec.copy(conditionNegate = false)) }, label = { Text(stringResource(R.string.dlg_condition_is)) })
                FilterChip(selected = spec.conditionNegate, onClick = { onChange(spec.copy(conditionNegate = true)) }, label = { Text(stringResource(R.string.dlg_condition_is_not)) })
            }
            OutlinedTextField(
                value = spec.conditionState.orEmpty(),
                onValueChange = { onChange(spec.copy(conditionState = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.dlg_condition_state)) },
                placeholder = { Text("on") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (pickingStart) {
        DateTimePickerDialog(
            initial = spec.start?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingStart = false },
            onClear = { onChange(spec.copy(start = null)); pickingStart = false },
            onConfirm = { onChange(spec.copy(start = it.toString())); pickingStart = false }
        )
    }
    if (pickingEnd) {
        DateTimePickerDialog(
            initial = spec.end?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingEnd = false },
            onClear = { onChange(spec.copy(end = null)); pickingEnd = false },
            onConfirm = { onChange(spec.copy(end = it.toString())); pickingEnd = false }
        )
    }
}

fun HKIButtonConfig.toVisibilitySpec(): VisibilitySpec = VisibilitySpec(
    hidden, visibilityStart, visibilityEnd, visibilityRangeMode.ifBlank { "show" }, visibilityRecurrence.ifBlank { "none" },
    visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate
)

fun HKIButtonConfig.withVisibilitySpec(spec: VisibilitySpec): HKIButtonConfig = copy(
    hidden = spec.hidden, visibilityStart = spec.start, visibilityEnd = spec.end,
    visibilityRangeMode = spec.rangeMode, visibilityRecurrence = spec.recurrence,
    visibilityConditionEntityId = spec.conditionEntityId, visibilityConditionState = spec.conditionState,
    visibilityConditionNegate = spec.conditionNegate
)

/** Small standalone dialog for editing one item's hide/schedule/condition rule inside a multi-item
 * widget (e.g. one sensor line, one calendar, one carrier account) — reuses [VisibilityEditor]. */
@Composable
fun ItemVisibilityDialog(
    label: String,
    config: HKIButtonConfig,
    onDismiss: () -> Unit,
    onSave: (HKIButtonConfig) -> Unit,
) {
    var spec by remember(config) { mutableStateOf(config.toVisibilitySpec()) }
    ModernAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = { VisibilityEditor(spec) { spec = it } },
        confirmButton = { Button(onClick = { onSave(config.withVisibilitySpec(spec)); onDismiss() }) { Text(stringResource(R.string.ui_done_e9b450d)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    )
}

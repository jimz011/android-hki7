package com.jimz011apps.hki7.ui.components

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            }) { Text("Done") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onClear != null) TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tab == "date", onClick = { tab = "date" }, label = { Text("Date") })
                FilterChip(selected = tab == "time", onClick = { tab = "time" }, label = { Text("Time") })
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

/** Portable hide/schedule rule for a button or badge (see `isVisibleAt`). */
data class VisibilitySpec(
    val hidden: Boolean = false,
    val start: String? = null,
    val end: String? = null,
    val rangeMode: String = "show",
    val recurrence: String = "none",
)

/** Human-readable label for a scheduled bound stored as an ISO local date-time, or "Any" if blank. */
fun formatVisibilityBound(iso: String?): String {
    if (iso.isNullOrBlank()) return "Any"
    return runCatching {
        LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
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
            FilterChip(selected = mode == "always", onClick = { onChange(VisibilitySpec()) }, label = { Text("Always") })
            FilterChip(selected = mode == "hidden", onClick = { onChange(VisibilitySpec(hidden = true)) }, label = { Text("Hidden") })
            FilterChip(
                selected = mode == "scheduled",
                onClick = { onChange(spec.copy(hidden = false, start = spec.start ?: LocalDateTime.now().withSecond(0).withNano(0).toString())) },
                label = { Text("Scheduled") }
            )
        }
        if (mode == "scheduled") {
            Text("When", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = spec.rangeMode == "show", onClick = { onChange(spec.copy(rangeMode = "show")) }, label = { Text("Visible during") })
                FilterChip(selected = spec.rangeMode == "hide", onClick = { onChange(spec.copy(rangeMode = "hide")) }, label = { Text("Hidden during") })
            }
            Text("Repeat", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("none" to "Once", "daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly").forEach { (value, txt) ->
                    FilterChip(selected = spec.recurrence == value, onClick = { onChange(spec.copy(recurrence = value)) }, label = { Text(txt) })
                }
            }
            OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start: ${formatVisibilityBound(spec.start)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("End: ${formatVisibilityBound(spec.end)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                when (spec.recurrence) {
                    "daily" -> "Repeats every day — only the time of day is used."
                    "weekly" -> "Repeats every week — only the weekday and time are used."
                    "monthly" -> "Repeats every month — only the day of the month and time are used."
                    "yearly" -> "Repeats every year — the year is ignored, so e.g. 24–26 Dec recurs each Christmas."
                    else -> "The exact dates you pick. Leave a bound as \"Any\" for an open-ended range."
                },
                style = MaterialTheme.typography.bodySmall,
                color = appColors.onMuted
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

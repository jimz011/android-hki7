package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

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

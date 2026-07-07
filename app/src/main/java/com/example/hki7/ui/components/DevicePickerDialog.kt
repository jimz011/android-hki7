package com.example.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HADeviceRegistryEntry
import com.example.hki7.ui.theme.LocalHKIAppColors

/** Searchable picker over the HA device registry (device-first configuration flows). */
@Composable
fun DevicePickerDialog(
    devices: List<HADeviceRegistryEntry>,
    currentId: String?,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(devices, query) {
        devices
            .filter { !(it.name_by_user ?: it.name).isNullOrBlank() }
            .filter { query.isBlank() || (it.name_by_user ?: it.name)!!.contains(query, ignoreCase = true) }
            .sortedBy { (it.name_by_user ?: it.name)!!.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Device") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search devices") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    if (currentId != null) {
                        item {
                            TextButton(onClick = { onSelected(null) }) { Text("Clear selection") }
                        }
                    }
                    items(filtered.size) { i ->
                        val d = filtered[i]
                        val name = d.name_by_user ?: d.name ?: d.id
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelected(d.id) }.padding(vertical = 10.dp)
                        ) {
                            Text(
                                name, style = MaterialTheme.typography.bodyMedium,
                                color = if (d.id == currentId) MaterialTheme.colorScheme.primary else appColors.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

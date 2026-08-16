package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/**
 * Picks the room-presence sensor tracking one person's phone.
 *
 * Candidates are `sensor.*` and `device_tracker.*` entities, which is where ESPresense and HA's
 * `mqtt_room` platform put theirs. Each row shows the entity's current state, because that state
 * *is* the room name — seeing "Living Room" next to a sensor is the fastest way to confirm you
 * picked the right person's phone.
 */
@Composable
fun RoomFollowSensorDialog(
    allEntities: List<HAEntity>,
    selected: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var query by remember { mutableStateOf("") }

    val candidates = remember(allEntities) {
        allEntities
            .filter { it.entity_id.startsWith("sensor.") || it.entity_id.startsWith("device_tracker.") }
            .sortedBy { (it.friendlyName ?: it.entity_id).lowercase() }
    }
    val filtered = remember(candidates, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) candidates
        else candidates.filter {
            it.entity_id.lowercase().contains(needle) ||
                (it.friendlyName ?: "").lowercase().contains(needle) ||
                it.state.lowercase().contains(needle)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_extra_room_follow_sensor),
                style = MaterialTheme.typography.titleMedium,
                color = appColors.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.ui_search_bce0641), style = MaterialTheme.typography.bodySmall)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.onSurface,
                        unfocusedTextColor = appColors.onSurface,
                        focusedContainerColor = appColors.subtleSurface,
                        unfocusedContainerColor = appColors.subtleSurface
                    )
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item(key = "__none__") {
                        SensorRow(
                            title = stringResource(R.string.settings_extra_room_follow_none),
                            subtitle = null,
                            checked = selected == null,
                            onClick = { onSelect(null) }
                        )
                    }
                    items(filtered, key = { it.entity_id }) { entity ->
                        SensorRow(
                            title = entity.friendlyName ?: entity.entity_id,
                            // The state is the room name; showing it makes the right sensor obvious.
                            subtitle = "${entity.entity_id} · ${entity.state}",
                            checked = entity.entity_id == selected,
                            onClick = { onSelect(entity.entity_id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_done_e9b450d))
            }
        }
    )
}

/**
 * Maps one sensor state onto an area, for the states the area names could not resolve by
 * themselves. Choosing the automatic option removes the override rather than storing a guess.
 */
@Composable
fun RoomFollowRoomDialog(
    state: String,
    areas: List<HAArea>,
    selected: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "__auto__") {
                    SensorRow(
                        title = stringResource(R.string.settings_extra_room_follow_rooms_matched),
                        subtitle = null,
                        checked = selected == null,
                        onClick = { onSelect(null) }
                    )
                }
                items(areas, key = { it.area_id }) { area ->
                    SensorRow(
                        title = area.name,
                        subtitle = null,
                        checked = area.area_id == selected,
                        onClick = { onSelect(area.area_id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_done_e9b450d)) }
        }
    )
}

@Composable
private fun SensorRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else appColors.subtleSurface,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = appColors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    it,
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (checked) {
            Icon(
                Icons.Default.Check,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

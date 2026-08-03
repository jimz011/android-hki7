package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/**
 * Offers to follow the user into the room they have moved to.
 *
 * Deliberately a question rather than an automatic jump: a room sensor is a guess about where
 * someone is, and pulling the view out from under them when that guess is wrong — or when they
 * walked off mid-task — is worse than doing nothing. The dwell window in
 * [com.jimz011apps.hki7.ui.RoomDwellTracker] is what keeps this from appearing every few seconds.
 *
 * There is no "always switch" shortcut here on purpose; that is what the per-person "Ask before
 * switching rooms" setting in Family Sharing is for. Dismissing by tapping outside means staying.
 */
@Composable
fun RoomMovePrompt(
    roomName: String,
    onSwitch: () -> Unit,
    onStay: () -> Unit,
    onSilenceUntilRestart: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    AlertDialog(
        onDismissRequest = onStay,
        shape = itemCornerShape(),
        containerColor = appColors.elevated,
        icon = {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.room_follow_moved_title, roomName),
                style = MaterialTheme.typography.titleMedium,
                color = appColors.onSurface
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.room_follow_moved_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.onMuted
                )
                Spacer(Modifier.height(14.dp))
                // The escape hatch for someone being asked more often than they want. A bordered,
                // full-width button because as a borderless line of muted text it read as a caption
                // and went unnoticed — and a non-admin cannot reach the Family Sharing toggle.
                OutlinedButton(
                    onClick = onSilenceUntilRestart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemCornerShape()
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.room_follow_moved_silence),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSwitch) {
                Text(stringResource(R.string.room_follow_moved_switch))
            }
        },
        dismissButton = {
            TextButton(onClick = onStay) {
                Text(stringResource(R.string.room_follow_moved_stay), color = appColors.onMuted)
            }
        }
    )
}

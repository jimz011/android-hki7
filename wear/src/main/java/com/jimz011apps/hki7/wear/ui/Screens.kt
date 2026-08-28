package com.jimz011apps.hki7.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.SwitchButton as ToggleChip
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.wear.R
import com.jimz011apps.hki7.data.WearRoom
import com.jimz011apps.hki7.wear.ui.theme.HkiWearColors

/** States that read as "this thing is currently doing something", for the active card treatment. */
private val ACTIVE_STATES = setOf("on", "open", "unlocked", "playing", "cleaning", "home", "heat", "cool")

internal fun isActive(entity: HAEntity?): Boolean =
    entity != null && entity.state.lowercase() in ACTIVE_STATES

internal fun stateLabel(entity: HAEntity?): String? =
    entity?.state?.replace('_', ' ')?.replaceFirstChar(Char::uppercase)

/**
 * The watch's home screen: the curated favourites first, rooms underneath.
 *
 * This mirrors how the Home Assistant watch app is organised — favourites, then somewhere to
 * browse everything else — because that ordering is simply correct for a watch: the thing you
 * came to do should need no scrolling, and the long tail should still be reachable.
 */
@Composable
fun QuickActionsScreen(
    quickActions: List<HKIQuickAction>,
    rooms: List<WearRoom>,
    entities: Map<String, HAEntity>,
    refreshing: Boolean,
    error: String?,
    onRun: (HKIQuickAction) -> Unit,
    onOpenRoom: (WearRoom) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(
                text = stringRes(R.string.wear_app_name),
                color = HkiWearColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (refreshing && entities.isEmpty()) {
            item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
        }
        error?.let {
            item { HkiWearMessage(stringRes(R.string.wear_unreachable), color = HkiWearColors.Danger) }
        }

        if (quickActions.isEmpty()) {
            item { HkiWearMessage(stringRes(R.string.wear_no_quick_actions)) }
        } else {
            item { HkiWearSectionTitle(stringRes(R.string.wear_quick_actions)) }
            items(quickActions, key = { it.id }) { action ->
                val entity = entities[action.entityId]
                HkiWearCard(
                    title = action.name?.takeIf { it.isNotBlank() }
                        ?: entity?.friendlyName?.takeIf { it.isNotBlank() }
                        ?: action.entityId,
                    subtitle = stateLabel(entity),
                    iconSlug = action.icon ?: entity?.icon,
                    active = isActive(entity),
                    onClick = { onRun(action) },
                )
            }
        }

        if (rooms.isNotEmpty()) {
            item { HkiWearSectionTitle(stringRes(R.string.wear_rooms)) }
            items(rooms, key = { it.id }) { room ->
                HkiWearCard(
                    title = room.name,
                    subtitle = null,
                    iconSlug = room.icon ?: "sofa",
                    active = false,
                    onClick = { onOpenRoom(room) },
                )
            }
        }

        item {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Text(stringRes(R.string.wear_settings))
            }
        }
    }
}

/** The entities inside one room, each toggling directly. */
@Composable
fun RoomScreen(
    room: WearRoom,
    entities: Map<String, HAEntity>,
    onToggle: (String) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(
                text = room.name,
                color = HkiWearColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (room.entityIds.isEmpty()) {
            item { HkiWearMessage(stringRes(R.string.wear_room_empty)) }
        }
        items(room.entityIds, key = { it }) { entityId ->
            val entity = entities[entityId]
            HkiWearCard(
                title = entity?.friendlyName?.takeIf { it.isNotBlank() } ?: entityId,
                subtitle = stateLabel(entity),
                iconSlug = entity?.icon,
                active = isActive(entity),
                onClick = { onToggle(entityId) },
            )
        }
    }
}

/**
 * Shown until the watch has a Home Assistant session, by either route.
 *
 * Two ways forward, in the order most people will want them. "Check again" asks the phone app to
 * hand over its session — no typing, but it needs HKI 7 installed there. Signing in runs Home
 * Assistant's own login page on the paired phone's browser, so it works with no phone app at all;
 * only the server address is typed here, and the password and any two-factor step happen on the
 * phone where there is a real keyboard and a password manager.
 */
@Composable
fun SetupScreen(
    onResync: () -> Unit,
    onSignIn: (String) -> Unit,
    busy: Boolean,
    signingIn: Boolean,
    error: String?,
) {
    var serverUrl by remember { mutableStateOf("") }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = stringRes(
                    if (signingIn) R.string.wear_setup_continue_on_phone_title
                    else R.string.wear_setup_title,
                ),
                color = HkiWearColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (signingIn) {
            // The phone is showing the login page; nothing here can move it along.
            item { HkiWearMessage(stringRes(R.string.wear_setup_continue_on_phone)) }
            item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
            return@ScalingLazyColumn
        }

        item { HkiWearMessage(stringRes(R.string.wear_setup_body)) }
        error?.let { item { HkiWearMessage(it, color = HkiWearColors.Danger) } }

        item {
            Button(onClick = onResync, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (busy) stringRes(R.string.wear_setup_checking)
                    else stringRes(R.string.wear_setup_retry),
                )
            }
        }
        item { HkiWearSectionTitle(stringRes(R.string.wear_setup_or_sign_in)) }
        item {
            BasicTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = HkiWearColors.OnSurface),
                cursorBrush = SolidColor(HkiWearColors.Accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onSignIn(serverUrl) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(HkiWearColors.CornerRadius))
                    .background(HkiWearColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (serverUrl.isEmpty()) {
                        Text(
                            stringRes(R.string.wear_setup_url_hint),
                            color = HkiWearColors.OnMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    inner()
                },
            )
        }
        item {
            Button(
                onClick = { onSignIn(serverUrl) },
                enabled = serverUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringRes(R.string.wear_setup_sign_in))
            }
        }
    }
}

/** Settings: what the watch is connected to, and the few controls worth having here. */
@Composable
fun SettingsScreen(
    onResync: () -> Unit,
    onRefresh: () -> Unit,
    busy: Boolean,
    sensorsEnabled: Boolean,
    onSensorsEnabledChange: (Boolean) -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                text = stringRes(R.string.wear_settings),
                color = HkiWearColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item { HkiWearMessage(stringRes(R.string.wear_settings_body)) }
        item { HkiWearSectionTitle(stringRes(R.string.wear_sensors_title)) }
        item {
            // Off by default and stated plainly: a watch reporting anything to a server is the
            // user's decision, not a default they discover later.
            ToggleChip(
                checked = sensorsEnabled,
                onCheckedChange = onSensorsEnabledChange,
                label = { Text(stringRes(R.string.wear_sensors_battery)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { HkiWearMessage(stringRes(R.string.wear_sensors_body)) }
        item {
            Button(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringRes(R.string.wear_refresh))
            }
        }
        item {
            Button(onClick = onResync, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringRes(R.string.wear_resync))
            }
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

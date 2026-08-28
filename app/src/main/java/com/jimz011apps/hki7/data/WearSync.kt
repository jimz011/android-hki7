package com.jimz011apps.hki7.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Sends the watch everything it needs: where Home Assistant is, a token to reach it with, the
 * curated quick actions, and the user's rooms.
 *
 * The watch talks to Home Assistant itself rather than proxying calls back through the phone, so
 * this runs once per change rather than on every tap — a watch whose phone is in another room
 * still works, and neither device pays for a constant link.
 *
 * The refresh token does travel over the Data Layer, which Play services encrypts between the
 * paired devices. That is the same trade the official Home Assistant watch app makes. A watch
 * with no phone app to hand it one signs itself in instead, through the phone's browser.
 */
object WearSync {

    private const val TAG = "WearSync"

    /**
     * Pushes the current configuration to any paired watch.
     *
     * Safe to call with no watch paired: it resolves to nothing. Failures are logged rather than
     * thrown, because every caller is a side effect of something the user actually asked for
     * (saving a quick action, finishing sign-in) and none of them should fail because a watch
     * happened to be out of range.
     */
    suspend fun push(context: Context) {
        runCatching {
            val prefs = PreferencesManager(context)
            val url = prefs.serverUrl.firstNonBlank() ?: prefs.internalUrl.firstNonBlank()
            // The refresh token, so the watch can keep its own session alive. Sending the
            // access token would give it half an hour of life and no way to renew.
            val refreshToken = prefs.refreshToken.firstNonBlank()
            // Filtered by the same household policy the phone and car obey, so a restricted
            // entity does not reach the watch by a side door.
            val quickActions = QuickActions.forWatch(context)
            val rooms = prefs.wearRooms.first()
            // Re-checked against the policy: a thermostat an admin has since restricted
            // must stop reaching the watch, exactly as a quick action would.
            val policy = prefs.enforcedSearchPolicyNow()
            val thermostats = prefs.wearThermostatEntityIds.first()
                .filter { policy.canSearchEntity(it) }

            val request = PutDataMapRequest.create(WearHandover.CONFIG_PATH).apply {
                dataMap.putString(WearHandover.KEY_SERVER_URL, url.orEmpty())
                dataMap.putString(WearHandover.KEY_REFRESH_TOKEN, refreshToken.orEmpty())
                dataMap.putString(
                    WearHandover.KEY_QUICK_ACTIONS,
                    appJson.encodeToString(quickActions),
                )
                dataMap.putString(WearHandover.KEY_ROOMS, appJson.encodeToString(rooms))
                dataMap.putString(
                    WearHandover.KEY_THERMOSTAT_ENTITY_IDS,
                    thermostats.joinToString(","),
                )
                // The Data Layer drops a write whose bytes match what is already stored, so an
                // unchanged config re-sent after the watch asked for it would never be delivered.
                dataMap.putLong(WearHandover.KEY_UPDATED_AT, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request).await()
        }.onFailure { Log.w(TAG, "Could not send configuration to the watch: ${it.message}") }
    }

    private suspend fun kotlinx.coroutines.flow.Flow<String?>.firstNonBlank(): String? =
        first()?.takeIf { it.isNotBlank() }
}

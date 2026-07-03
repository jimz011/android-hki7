package com.example.hki7.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fires when the device crosses a Home Assistant zone boundary (registered by [GeofenceManager]).
 * Pushes one fresh location report so HA presence updates immediately — no need to keep location
 * running the rest of the time. Runs as a plain background report (no foreground service needed).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE_EVENT) return
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) return

        val appContext = context.applicationContext
        // goAsync() keeps the receiver alive while we fetch a location and post it (background
        // broadcasts get a generous window; we cap it so we never risk an ANR).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(REPORT_TIMEOUT_MS) {
                    // Force an accurate current fix so the zone change lands at the right place/time.
                    runCatching { reportTelemetryNow(appContext, PreferencesManager(appContext), freshLocation = true) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "com.example.hki7.GEOFENCE_EVENT"
        private const val REPORT_TIMEOUT_MS = 30_000L
    }
}

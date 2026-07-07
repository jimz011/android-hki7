package com.example.hki7.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Background location pipeline mirroring the official HA app's normal mode: a balanced-power
 * fused-location request delivered to a broadcast PendingIntent (no service). It does two jobs:
 *  1. every delivered fix is reported to HA, so the device_tracker moves between geofence events
 *     and periodic worker runs;
 *  2. the standing request keeps the fused location provider actively sampling, which is what
 *     makes the OS detect geofence transitions promptly — with no location client registered,
 *     geofence detection relies on opportunistic scans and can lag by tens of minutes.
 * While the app is in the background the OS throttles delivery to a handful of updates per hour
 * (identical to the official app), so battery cost stays negligible; this is NOT continuous
 * tracking and is unrelated to the opt-in High Accuracy foreground service.
 */
class BackgroundLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOCATION_UPDATE) return
        val location = LocationResult.extractResult(intent)?.lastLocation ?: return
        val appContext = context.applicationContext
        // goAsync() keeps the receiver alive while the report posts (capped well below the ANR window).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(REPORT_TIMEOUT_MS) {
                    runCatching {
                        reportTelemetryNow(appContext, PreferencesManager(appContext), providedLocation = location)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOCATION_UPDATE = "com.example.hki7.BACKGROUND_LOCATION"
        private const val REPORT_TIMEOUT_MS = 30_000L

        // Official-HA-app cadence: ~1 min interval, 30 s fastest, generous batching so the OS can
        // deliver several queued fixes in one process wake-up.
        private const val INTERVAL_MS = 60_000L
        private const val FASTEST_MS = 30_000L
        private const val MAX_DELAY_MS = 200_000L

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, BackgroundLocationReceiver::class.java)
                .setAction(ACTION_LOCATION_UPDATE)
            // Mutable so the fused provider can attach the LocationResult extras.
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        /** (Re)registers the standing request. Idempotent; requests are lost on force-stop/update,
         *  so this is re-armed from the periodic worker as well. */
        @SuppressLint("MissingPermission")
        fun register(context: Context) {
            if (!hasBackgroundPermission(context)) return
            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_MS)
                .setMaxUpdateDelayMillis(MAX_DELAY_MS)
                .build()
            runCatching {
                LocationServices.getFusedLocationProviderClient(context)
                    .requestLocationUpdates(request, pendingIntent(context))
            }
        }

        fun unregister(context: Context) {
            runCatching {
                LocationServices.getFusedLocationProviderClient(context)
                    .removeLocationUpdates(pendingIntent(context))
            }
        }

        private fun hasBackgroundPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val background = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            return fine && background
        }
    }
}

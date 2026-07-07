package com.example.hki7.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms background presence after a reboot. Geofences don't survive a reboot, so we re-register
 * them and re-schedule the periodic telemetry job via WorkManager. No foreground service is started
 * (normal mode doesn't use one); the worker and geofence sync both no-op when the user is logged out.
 * High Accuracy continuous tracking resumes the next time the app is opened.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                LocationWork.schedule(context)
                LocationWork.syncNow(context)
                // Fused-location requests don't survive a reboot either; re-arm right away so
                // geofence detection is responsive before the first worker run.
                BackgroundLocationReceiver.register(context)
                // Bring the persistent notification connection back up if the user opted in.
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (PreferencesManager(context).backgroundPushEnabled.first()) {
                            PushForegroundService.start(context)
                        }
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}

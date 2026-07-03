package com.example.hki7.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
            }
        }
    }
}

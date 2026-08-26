package com.jimz011apps.hki7.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Pushes the interactive binary sensor as soon as the screen turns on or off, instead of waiting
 * for the next 15-minute telemetry cycle.
 */
class DevicePanelScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF -> LocationWork.syncNow(context)
        }
    }

    companion object {
        @Volatile private var registered: DevicePanelScreenReceiver? = null

        fun sync(context: Context, extraSensorsEnabled: Boolean) {
            val app = context.applicationContext
            if (extraSensorsEnabled) register(app) else unregister(app)
        }

        private fun register(context: Context) {
            if (registered != null) return
            val receiver = DevicePanelScreenReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = receiver
        }

        private fun unregister(context: Context) {
            val receiver = registered ?: return
            runCatching { context.unregisterReceiver(receiver) }
            registered = null
        }
    }
}

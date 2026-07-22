package com.jimz011apps.hki7.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manifest-registered receiver for charger plug/unplug. Because it's declared in the manifest (not
 * registered at runtime by a living component), it fires even when the app process has been killed.
 * The Charging and Battery Level entities therefore update immediately on a power change, like the
 * official HA app's sensor receiver. It pushes one report without needing a foreground service.
 */
class PowerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED &&
            intent.action != Intent.ACTION_POWER_DISCONNECTED
        ) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(REPORT_TIMEOUT_MS.milliseconds) {
                    runCatching { reportTelemetryNow(appContext, PreferencesManager(appContext)) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val REPORT_TIMEOUT_MS = 20_000L
    }
}

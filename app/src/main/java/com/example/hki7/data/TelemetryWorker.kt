package com.example.hki7.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic battery/location + geofence refresh, run under WorkManager instead of a persistent
 * foreground service. This is the official HA app's battery-friendly model: the OS batches the work
 * into Doze maintenance windows rather than keeping the process awake 24/7. Presence still updates
 * instantly on zone crossings via [GeofenceBroadcastReceiver]; this just keeps battery/location
 * fresh and re-registers geofences (which don't survive reboots) periodically. No-op when logged out.
 */
class TelemetryWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        if (prefs.serverUrl.first().isNullOrBlank() && prefs.internalUrl.first().isNullOrBlank()) {
            // Logged out: stop waking up until the app re-arms us on next login.
            LocationWork.cancel(applicationContext)
            BackgroundLocationReceiver.unregister(applicationContext)
            return Result.success()
        }
        runCatching { reportTelemetryNow(applicationContext, prefs) }
        runCatching { GeofenceManager(applicationContext).sync(prefs) }
        // Re-arm the passive background location request (lost on force-stop/app update); it keeps
        // the fused provider sampling so geofence transitions are detected promptly.
        runCatching { BackgroundLocationReceiver.register(applicationContext) }
        return Result.success()
    }
}

/** Schedules/cancels the background telemetry work that replaces the always-on foreground service. */
object LocationWork {
    private const val PERIODIC_NAME = "hki7_telemetry_periodic"
    private const val ONESHOT_NAME = "hki7_telemetry_now"
    private const val PERIOD_MINUTES = 15L

    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Arms the recurring battery/location + geofence refresh. Idempotent (keeps any existing one). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<TelemetryWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs one telemetry + geofence refresh as soon as constraints allow (e.g. right after boot). */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<TelemetryWorker>()
            .setConstraints(constraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
    }
}

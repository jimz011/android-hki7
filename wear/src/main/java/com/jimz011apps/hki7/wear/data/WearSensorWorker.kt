package com.jimz011apps.hki7.wear.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Sends the watch's battery to Home Assistant on a schedule.
 *
 * Fifteen minutes is WorkManager's floor for periodic work and also about right: a watch battery
 * moves slowly, and reporting it more often would cost more power than the reading is worth. The
 * system batches this with other deferred work, so it rarely wakes the watch on its own account.
 */
class WearSensorWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = WearPreferences(applicationContext)
        if (!prefs.sensorsEnabledOnce()) {
            // Switched off since this was scheduled; stop rather than keep waking up.
            cancel(applicationContext)
            return Result.success()
        }
        val sent = WearSensors.report(applicationContext, prefs, WearSession(prefs))
        // Retry rather than fail: an unreachable server now is usually reachable next time, and a
        // failed run must not stop the periodic schedule.
        return if (sent) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "hki7-wear-sensors"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WearSensorWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP: re-enqueuing on every app open would reset the interval and mean it never
                // actually ran on a watch that is opened often.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

package com.jimz011apps.hki7.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub for a newer release once a day and notifies when there is one.
 *
 * Daily rather than on every launch: a release lands every few weeks at most, and the app's whole
 * battery story is that nothing runs on a schedule it does not need. WorkManager batches this with
 * whatever else the device is already awake for.
 */
class UpdateCheckWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        if (!prefs.updateChecksEnabled.first()) return Result.success()

        // Before trusting the marker, drop one left over from the era when it was written even for
        // a notification that never posted. Runs once per install and is a no-op on a fresh one.
        prefs.clearStaleNotifiedUpdateVersionOnce()

        val available = GithubReleaseChecker.check()
        if (available == null) {
            // Current again: retire the panel notice, which is exempt from the ordinary purge and
            // would otherwise outlive the update it announces, and forget the version so a
            // re-published release would be announced afresh.
            GithubReleaseChecker.clearRecorded(applicationContext)
            prefs.saveLastPanelUpdateVersion(null)
            return Result.success()
        }
        // The panel notice goes in once per version, like the system one. The update may sit there
        // for weeks, and whatever the reader does with it — read it, archive it, delete it — has to
        // stick, which it cannot if the next check puts it straight back.
        if (prefs.lastPanelUpdateVersion.first() != available.versionName) {
            GithubReleaseChecker.record(applicationContext, available)
            prefs.saveLastPanelUpdateVersion(available.versionName)
        }
        // The system notification, by contrast, is posted once per version. Without this the same
        // release would be announced every day until the user got round to installing it.
        if (prefs.lastNotifiedUpdateVersion.first() == available.versionName) return Result.success()

        // Only mark it announced if it was: notifications may be switched off, and recording an
        // attempt that never reached the shade would suppress this version for good — the check
        // above would short-circuit every following day, so granting the permission later would
        // never bring it back.
        if (GithubReleaseChecker.notify(applicationContext, available)) {
            prefs.saveLastNotifiedUpdateVersion(available.versionName)
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "hki7_update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
        }
    }
}

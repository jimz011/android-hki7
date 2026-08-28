package com.jimz011apps.hki7.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for the Android Auto surface.
 *
 * Android Auto never runs HKI 7's own UI: the head unit draws templates the host renders itself,
 * so nothing from the Compose dashboard reaches this surface. It shows the user's curated
 * quick-action list instead — see [com.jimz011apps.hki7.data.HKIQuickAction].
 */
class HKI7CarAppService : CarAppService() {

    /**
     * Which car hosts may drive this app.
     *
     * Debug builds allow any host so the Desktop Head Unit can connect. Release builds use the
     * library's own allowlist, which is Google's signed hosts only — allowing all hosts in a
     * shipped build would let any app on the phone drive the car surface.
     */
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(): Session = HKI7CarSession()
}

/** One connection to a car head unit. Nothing is cached across sessions: the quick-action list and
 *  the credentials behind it can both have changed while the phone was out of the car. */
class HKI7CarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = QuickActionsCarScreen(carContext)
}

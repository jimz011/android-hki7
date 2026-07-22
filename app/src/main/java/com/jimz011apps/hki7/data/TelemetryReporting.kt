package com.jimz011apps.hki7.data

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.first

/**
 * Builds the active Home Assistant client (honoring internal/external URL switching by Wi-Fi SSID)
 * and pushes one telemetry report. Shared by the foreground service (heartbeat + location callbacks)
 * and geofence transitions so they all report identically. No-op when the user isn't logged in.
 */
suspend fun reportTelemetryNow(
    context: Context,
    prefs: PreferencesManager,
    providedLocation: Location? = null,
    freshLocation: Boolean = false,
    locationOnly: Boolean = false,
    log: (String) -> Unit = {}
) {
    prefs.ensureHomeAssistantInstanceStore()
    val instances = prefs.homeAssistantInstances.first()
        .filter { instance ->
            instance.isAuthenticated && if (locationOnly) instance.locationEnabled
            else instance.locationEnabled || instance.notificationsEnabled
        }
    if (instances.isEmpty()) return

    // Ask Android for one physical fix, then fan that same sample out to every server. This avoids
    // multiplying GPS/radio work as homes are added and gives every HA instance the same timestamp.
    val locationNeeded = instances.any { it.locationEnabled }
    val sharedLocation = if (!locationNeeded) null else providedLocation
        ?: DeviceTelemetryReporter(context.applicationContext, prefs).resolveLocation(freshLocation)
    val ssid = currentWifiSsid(context)
    val deviceName = prefs.mobileDeviceName.first()
    instances.forEach { instance ->
        val scopedPrefs = prefs.forInstance(instance.id)
        val primary = instance.primaryUrl ?: return@forEach
        val active = resolveHomeAssistantUrl(
            instance.externalUrl,
            instance.internalUrl,
            instance.homeSsids,
            ssid
        ) ?: primary
        val token = instance.accessToken.orEmpty()
        runCatching {
            val client = HomeAssistantClient(active, token)
            try {
                DeviceTelemetryReporter(context.applicationContext, scopedPrefs).report(
                    client = client,
                    registrationUrl = primary,
                    webhookBaseUrl = active,
                    configuredDeviceName = deviceName,
                    providedLocation = sharedLocation.takeIf { instance.locationEnabled },
                    freshLocation = false,
                    locationOnly = locationOnly,
                    lookupLocationIfMissing = false,
                    log = { message -> log("${instance.name}: $message") }
                )
            } finally {
                client.closeSession()
            }
        }.onFailure { error -> log("${instance.name}: telemetry failed: ${error.message}") }
    }
}

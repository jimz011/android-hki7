package com.example.hki7.data

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
    val external = prefs.serverUrl.first()?.takeIf { it.isNotBlank() } ?: return
    val internal = prefs.internalUrl.first()
    val ssids = prefs.homeSsids.first()
    val ssid = currentWifiSsid(context)
    val active = resolveHomeAssistantUrl(external, internal, ssids, ssid) ?: external
    val token = prefs.accessToken.first().orEmpty()
    val deviceName = prefs.mobileDeviceName.first()
    val client = HomeAssistantClient(active, token)
    DeviceTelemetryReporter(context.applicationContext, prefs)
        .report(client, external, active, deviceName, providedLocation = providedLocation, freshLocation = freshLocation, locationOnly = locationOnly, log = log)
}

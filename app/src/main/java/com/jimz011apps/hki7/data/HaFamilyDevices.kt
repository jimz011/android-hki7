package com.jimz011apps.hki7.data

import android.content.Context
import android.os.Build
import com.jimz011apps.hki7.BuildConfig
import kotlinx.coroutines.flow.first

/**
 * Which HKI version each family device is running, via the `hki7` companion component.
 *
 * Every install reports itself over the WebSocket connection the app already authenticates on, so
 * this covers a phone whether or not it does anything else with Home Assistant. The earlier
 * approach published the version as a `mobile_app` diagnostic sensor, which only existed for
 * devices doing location or notification telemetry — a phone with both switched off never
 * registered with `mobile_app` at all and was therefore invisible.
 *
 * The component decides which Home Assistant account a report belongs to from the authenticated
 * connection, so a device can only ever report itself. Requires HKI 7 Cloud 0.7.0.
 */
object HaFamilyDevices {

    /** Minimum companion component version that understands the `hki7/device` commands. */
    const val MIN_COMPONENT_VERSION = "0.7.0"

    /** Records this device's version. Cheap enough to call on every foreground: the component only
     * writes to disk when something actually changed, or once an hour to refresh "last seen". */
    suspend fun report(context: Context, prefs: PreferencesManager): Boolean {
        val deviceId = prefs.familyDeviceId().takeIf { it.isNotBlank() } ?: return false
        val deviceName = resolveHkiDeviceName(context, prefs.mobileDeviceName.first())
        return Hki7Endpoint.withClient(context) { client ->
            client.hki7ReportDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                osVersion = Build.VERSION.RELEASE,
                model = Build.MODEL,
            )
        } ?: false
    }

    /** Every install reported in this household (admin only). Null when the component can't answer
     * — too old, or the caller isn't an admin — which the caller reports as such rather than
     * showing an empty list that would read as "nobody is running HKI". */
    suspend fun list(context: Context): List<Hki7FamilyDevice>? =
        Hki7Endpoint.withClient(context) { it.hki7ListDevices() }

    /** Forgets a replaced or uninstalled phone (admin only). One still in use reports itself again
     * the next time its app opens, so this can't permanently hide an active device. */
    suspend fun forget(context: Context, device: Hki7FamilyDevice): Boolean =
        Hki7Endpoint.withClient(context) { it.hki7ForgetDevice(device.userId, device.deviceId) } ?: false
}

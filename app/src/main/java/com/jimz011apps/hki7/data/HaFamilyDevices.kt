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

    /** Minimum component version for the household minimum and per-device update requests. */
    const val MIN_UPDATE_POLICY_VERSION = "0.8.0"

    /** Records this device's version and caches whatever version it is being asked to run.
     *
     * Cheap enough for every foreground: the component only writes to disk when something actually
     * changed, or once an hour to refresh "last seen", and it answers with the update requirement
     * in the same round trip rather than making this a second command.
     *
     * A failed call deliberately leaves the cached requirement alone — an unreachable Home
     * Assistant is not the same as an admin lifting the requirement, and treating it as such would
     * make the prompt flicker away every time the connection dropped. */
    suspend fun report(context: Context, prefs: PreferencesManager): Boolean {
        val deviceId = prefs.familyDeviceId().takeIf { it.isNotBlank() } ?: return false
        val deviceName = resolveHkiDeviceName(context, prefs.mobileDeviceName.first())
        val result = Hki7Endpoint.withClient(context) { client ->
            client.hki7ReportDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                osVersion = Build.VERSION.RELEASE,
                model = Build.MODEL,
            )
        } ?: return false
        prefs.saveRequiredAppUpdate(requiredUpdate(result))
        return true
    }

    /** Picks what this device is actually being asked for. A request aimed at this one device wins
     *  over the household minimum when it asks for more; otherwise the household floor applies. */
    private fun requiredUpdate(result: Hki7DeviceReportResult): Hki7RequiredUpdate? {
        val household = result.requiredVersionCode
            ?.takeIf { it > 0 }
            ?.let { Hki7RequiredUpdate(it, result.requiredVersionName, deviceSpecific = false) }
        val nudge = result.nudgeVersionCode
            ?.takeIf { it > 0 }
            ?.let { Hki7RequiredUpdate(it, result.nudgeVersionName, deviceSpecific = true) }
        return listOfNotNull(household, nudge).maxByOrNull { it.versionCode }
    }

    /** The household's current minimum, for the admin editor. Null when the component predates it. */
    suspend fun updatePolicy(context: Context): Hki7AppUpdatePolicy? =
        Hki7Endpoint.withClient(context) { it.hki7GetAppUpdatePolicy() }

    /** Sets or clears (null) the household minimum. False when the component refuses — including a
     * version no device has reported, which it rejects rather than let an admin demand something
     * nobody can install. */
    suspend fun setUpdatePolicy(context: Context, minVersionCode: Int?, minVersionName: String): Boolean =
        Hki7Endpoint.withClient(context) { it.hki7SetAppUpdatePolicy(minVersionCode, minVersionName) } ?: false

    /** Asks one device to update, or clears that request with a null [versionCode]. */
    suspend fun nudge(
        context: Context,
        device: Hki7FamilyDevice,
        versionCode: Int?,
        versionName: String,
    ): Boolean = Hki7Endpoint.withClient(context) {
        it.hki7NudgeDevice(device.userId, device.deviceId, versionCode, versionName)
    } ?: false

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

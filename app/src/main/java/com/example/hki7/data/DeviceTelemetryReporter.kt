@file:Suppress("SpellCheckingInspection")

package com.example.hki7.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.hki7.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Reports device location + battery to Home Assistant via the mobile_app integration (webhook),
 * which creates persistent entities (device_tracker + sensors) that survive HA restarts.
 */
class DeviceTelemetryReporter(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    /**
     * @param registrationUrl stable primary URL used as the registration identity (so switching
     *   between internal/external URLs does not re-register and create duplicate trackers).
     * @param webhookBaseUrl the URL to actually post telemetry to right now (internal on home Wi-Fi).
     * @param locationOnly official-app behavior for per-fix triggers (background location broadcasts,
     *   zone transitions, High Accuracy stream): post update_location only. Sensors — battery,
     *   charging, geocoded address — refresh solely on the periodic sensor cycle and explicit
     *   sensor events, never per fix.
     */
    suspend fun report(
        client: HomeAssistantClient,
        registrationUrl: String,
        webhookBaseUrl: String,
        configuredDeviceName: String? = null,
        providedLocation: Location? = null,
        freshLocation: Boolean = false,
        locationOnly: Boolean = false,
        log: (String) -> Unit = {}
    ) {
        if (webhookBaseUrl.isBlank()) {
            log("Telemetry skipped: no base URL")
            return
        }
        val deviceName = resolveDeviceName(configuredDeviceName)
        val slug = slugify(deviceName)

        val webhookId = ensureRegistered(client, registrationUrl, deviceName, log) ?: return
        val cloudhook = prefs.mobileAppCloudhookUrl.first()
        val webhookUrl = cloudhook?.takeIf { it.isNotBlank() }
            ?: "${webhookBaseUrl.removeSuffix("/")}/api/webhook/$webhookId"

        val batteryLevel = batteryLevel()
        val location = providedLocation ?: currentLocation(freshLocation)
        if (location == null) log("Telemetry: no location available (GPS/last-known both null)")

        if (locationOnly) {
            if (location != null) sendLocationUpdate(client, webhookUrl, location, batteryLevel, log)
            return
        }

        val charging = isCharging()
        val address = location?.let { geocodeThrottled(it) }

        // Upgrade pre-existing registrations (made before push support) so HA creates the
        // notify.mobile_app_<device> service. Once per process; harmless when already set.
        if (!pushChannelEnsured) {
            val ok = post(client, webhookUrl, buildJsonObject {
                put("type", "update_registration")
                put("data", buildJsonObject {
                    put("app_version", BuildConfig.VERSION_NAME)
                    put("device_name", deviceName)
                    put("manufacturer", Build.MANUFACTURER)
                    put("model", Build.MODEL)
                    put("os_version", Build.VERSION.RELEASE)
                    put("app_data", buildJsonObject { put("push_websocket_channel", true) })
                })
            }, "update_registration (push channel)", log)
            if (ok) pushChannelEnsured = true
        }

        // Register sensors once per webhook (they persist in HA); plain state updates suffice after.
        if (prefs.mobileAppSensorsWebhookId.first() != webhookId) {
            val registered = registerSensors(client, webhookUrl, slug, deviceName, batteryLevel, charging, address, log)
            if (registered) prefs.saveMobileAppSensorsRegistered(webhookId)
        }

        // HA's update_sensor_states requires "type" on every entry (matching the registered type),
        // otherwise it rejects the whole payload with "required key not provided @ data[0]['type']".
        val sensorStates = buildJsonArray {
            add(buildJsonObject {
                put("unique_id", "${slug}_battery_level")
                put("type", "sensor")
                put("state", batteryLevel)
                put("icon", if (charging) "mdi:battery-charging" else "mdi:battery")
            })
            add(buildJsonObject {
                put("unique_id", "${slug}_charging")
                put("type", "binary_sensor")
                put("state", charging)
            })
            // Only when an address actually resolved — the official app never writes raw
            // coordinates here, and skipping the entry keeps HA's recorder free of churn from
            // GPS jitter (an omitted sensor simply keeps its previous state).
            if (address != null) {
                add(buildJsonObject {
                    put("unique_id", "${slug}_geocoded_location")
                    put("type", "sensor")
                    put("state", address.take(255))
                    put("icon", "mdi:map-marker")
                })
            }
        }
        val updatePayload = buildJsonObject {
            put("type", "update_sensor_states")
            put("data", sensorStates)
        }
        val (updateStatus, updateBody) = runCatching { client.postWebhook(webhookUrl, updatePayload) }
            .getOrElse { log("update_sensor_states failed: ${it.message}"); -1 to "" }
        when {
            updateStatus !in 200..299 -> log("update_sensor_states -> HTTP $updateStatus")
            // HA returns 200 with a per-sensor body; an unrecognized sensor reports success=false.
            // Without re-registering here the entity stays frozen at its initial registered value.
            sensorsNeedReRegistration(updateBody) -> {
                log("HA doesn't recognize the sensors — re-registering and retrying")
                registerSensors(client, webhookUrl, slug, deviceName, batteryLevel, charging, address, log)
                prefs.saveMobileAppSensorsRegistered(webhookId)
                runCatching { client.postWebhook(webhookUrl, updatePayload) }
            }
        }

        if (location != null) sendLocationUpdate(client, webhookUrl, location, batteryLevel, log)
    }

    /**
     * Posts update_location with the official app's gates:
     *  - fixes with accuracy worse than [MIN_ACCURACY_M] are disregarded entirely (a stationary
     *    device flip-flopping between Wi-Fi and cell fixes would otherwise wander in HA);
     *  - fixes not newer than the last one sent are skipped (fused's cached fix gets redelivered);
     *  - an identical repeat of the last sent coordinates isn't re-posted.
     */
    private suspend fun sendLocationUpdate(
        client: HomeAssistantClient,
        webhookUrl: String,
        location: Location,
        batteryLevel: Int,
        log: (String) -> Unit
    ) {
        if (!location.hasAccuracy() || location.accuracy > MIN_ACCURACY_M) {
            log("Location accuracy ${location.accuracy.toInt()}m exceeds ${MIN_ACCURACY_M}m, disregarding")
            return
        }
        if (location.time <= lastSentLocationTime) return
        if (location.latitude == lastSentLat && location.longitude == lastSentLon) {
            lastSentLocationTime = location.time
            return
        }
        val sent = post(client, webhookUrl, buildJsonObject {
            put("type", "update_location")
            put("data", buildJsonObject {
                putJsonArray("gps") { add(location.latitude); add(location.longitude) }
                put("gps_accuracy", location.accuracy.toInt())
                put("battery", batteryLevel)
                if (location.hasSpeed()) put("speed", location.speed.toInt())
                if (location.hasAltitude()) put("altitude", location.altitude.toInt())
                if (location.hasBearing()) put("course", location.bearing.toInt())
            })
        }, "update_location", log)
        if (sent) {
            lastSentLocationTime = location.time
            lastSentLat = location.latitude
            lastSentLon = location.longitude
        }
    }

    // Serialized so two concurrent reports (e.g. several fire right after login) can't each POST a
    // fresh /registrations and create duplicate devices — the second waits and reuses the first's id.
    private suspend fun ensureRegistered(
        client: HomeAssistantClient,
        baseUrl: String,
        deviceName: String,
        log: (String) -> Unit
    ): String? = registrationMutex.withLock {
        val storedWebhook = prefs.mobileAppWebhookId.first()
        val storedUrl = prefs.mobileAppRegisteredUrl.first()
        if (!storedWebhook.isNullOrBlank() && storedUrl == baseUrl) return@withLock storedWebhook

        val deviceId = prefs.mobileAppDeviceId.first()
            ?: UUID.randomUUID().toString().also { prefs.saveMobileAppDeviceId(it) }
        val body = buildJsonObject {
            put("device_id", deviceId)
            put("app_id", "hki7")
            put("app_name", "HKI7")
            put("app_version", BuildConfig.VERSION_NAME)
            put("device_name", deviceName)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("os_name", "Android")
            put("os_version", Build.VERSION.RELEASE)
            put("supports_encryption", false)
            // Advertise the websocket push channel so HA creates notify.mobile_app_<device>.
            put("app_data", buildJsonObject { put("push_websocket_channel", true) })
        }
        val registration = runCatching { client.registerMobileApp(body) }.getOrElse {
            log("mobile_app registration failed: ${it.message}")
            return@withLock null
        }
        prefs.saveMobileAppRegistration(
            registration.webhook_id,
            registration.cloudhook_url,
            registration.remote_ui_url,
            baseUrl
        )
        log("mobile_app registered (device_tracker will appear in HA)")
        return@withLock registration.webhook_id
    }

    /** HA's update_sensor_states reply is `{ "<unique_id>": { "success": true|false, ... }, ... }`.
     *  Any success=false means HA doesn't know that sensor (restart, fresh webhook, etc.) and it must
     *  be re-registered — the official app does this; otherwise the entity is stuck at its initial value. */
    private fun sensorsNeedReRegistration(body: String): Boolean {
        if (body.isBlank()) return false
        return runCatching {
            json.parseToJsonElement(body).jsonObject.values.any { entry ->
                (entry as? JsonObject)?.get("success")?.jsonPrimitive?.booleanOrNull == false
            }
        }.getOrDefault(false)
    }

    private suspend fun registerSensors(
        client: HomeAssistantClient,
        webhookUrl: String,
        slug: String,
        deviceName: String,
        batteryLevel: Int,
        charging: Boolean,
        address: String?,
        log: (String) -> Unit
    ): Boolean {
        val batteryOk = post(client, webhookUrl, buildJsonObject {
            put("type", "register_sensor")
            put("data", buildJsonObject {
                put("unique_id", "${slug}_battery_level")
                put("name", "$deviceName Battery Level")
                put("state", batteryLevel)
                put("type", "sensor")
                put("device_class", "battery")
                put("unit_of_measurement", "%")
                put("entity_category", "diagnostic")
                put("icon", "mdi:battery")
            })
        }, "register battery", log)
        val chargingOk = post(client, webhookUrl, buildJsonObject {
            put("type", "register_sensor")
            put("data", buildJsonObject {
                put("unique_id", "${slug}_charging")
                put("name", "$deviceName Charging")
                put("state", charging)
                put("type", "binary_sensor")
                put("device_class", "battery_charging")
                put("entity_category", "diagnostic")
            })
        }, "register charging", log)
        val geocodedOk = post(client, webhookUrl, buildJsonObject {
            put("type", "register_sensor")
            put("data", buildJsonObject {
                put("unique_id", "${slug}_geocoded_location")
                put("name", "$deviceName Geocoded Location")
                put("state", (address ?: "unknown").take(255))
                put("type", "sensor")
                put("icon", "mdi:map-marker")
            })
        }, "register geocoded", log)
        return batteryOk && chargingOk && geocodedOk
    }

    private suspend fun post(
        client: HomeAssistantClient,
        webhookUrl: String,
        payload: JsonObject,
        label: String,
        log: (String) -> Unit
    ): Boolean {
        return runCatching { client.postWebhook(webhookUrl, payload) }
            .map { (status, body) ->
                if (status !in 200..299) log("$label -> HTTP $status: ${body.take(200)}")
                status in 200..299
            }
            .getOrElse { log("$label failed: ${it.message}"); false }
    }

    private fun resolveDeviceName(configuredDeviceName: String?): String {
        return configuredDeviceName?.takeIf { it.isNotBlank() }
            ?: Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: Build.MODEL
    }

    private fun slugify(name: String): String =
        name.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "hki_device" }

    private fun batteryLevel(): Int {
        val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Reverse-geocodes with the official app's geocode-sensor gates. On API 33+ the Geocoder hits
     * the network, so every avoided lookup is an avoided radio wakeup:
     *  - fixes with accuracy worse than [MIN_ACCURACY_M] never geocode (a cell-tower fix can sit
     *    hundreds of meters off and would defeat the movement check below on every report);
     *  - within [GEOCODE_MIN_MOVE_M] of the last resolved address the cache is reused — and the
     *    cache is persisted, so a recycled process doesn't re-query for a device that hasn't moved;
     *  - actual Geocoder calls are spaced at least [GEOCODE_MIN_INTERVAL_MS] apart (the official
     *    app only refreshes this sensor on its ~15-minute sensor cycle), which also stops a failing
     *    Geocoder from being retried on every report.
     */
    private suspend fun geocodeThrottled(location: Location): String? {
        loadGeocodeCache()
        if (!location.hasAccuracy() || location.accuracy > MIN_ACCURACY_M) return lastGeocodeAddress

        val lat = lastGeocodeLat
        val lon = lastGeocodeLon
        if (lat != null && lon != null && lastGeocodeAddress != null) {
            val moved = FloatArray(1)
            Location.distanceBetween(lat, lon, location.latitude, location.longitude, moved)
            if (moved[0] < GEOCODE_MIN_MOVE_M) return lastGeocodeAddress
        }

        val now = System.currentTimeMillis()
        val lastAttempt = lastGeocodeAttemptMs
        if (lastAttempt != null && now - lastAttempt < GEOCODE_MIN_INTERVAL_MS) return lastGeocodeAddress
        lastGeocodeAttemptMs = now

        val resolved = geocode(location)
        if (resolved != null) {
            lastGeocodeLat = location.latitude
            lastGeocodeLon = location.longitude
            lastGeocodeAddress = resolved
            prefs.saveGeocodedAddress(resolved, location.latitude, location.longitude)
        }
        return resolved ?: lastGeocodeAddress
    }

    /** Seeds the in-memory geocode cache from the persisted copy once per process. */
    private suspend fun loadGeocodeCache() {
        if (geocodeCacheLoaded) return
        geocodeCacheLoaded = true
        if (lastGeocodeAddress == null) {
            lastGeocodeAddress = prefs.geocodedAddress.first()
            lastGeocodeLat = prefs.geocodedLat.first()
            lastGeocodeLon = prefs.geocodedLon.first()
        }
    }

    /** Resolves a street address from coordinates using the async Geocoder (works on API 33+). */
    private suspend fun geocode(location: Location): String? {
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(5.seconds) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    Geocoder(context, Locale.getDefault()).getFromLocation(
                        location.latitude,
                        location.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull()?.getAddressLine(0))
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    )
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
    }

    /**
     * Gets a location for a report. Periodic/sensor reports ([fresh] = false) NEVER wake the radio:
     * like the official app they use only the Fused Location Provider's cached fix (kept reasonably
     * fresh by the standing background request in [BackgroundLocationReceiver]). Only a zone
     * transition ([fresh] = true) requests an accurate on-the-spot fix, so HA records the right
     * zone at the right moment — the cached fix is the main reason zone-change times were off.
     */
    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(fresh: Boolean = false): Location? {
        if (!hasLocationPermission()) return null
        val fused = LocationServices.getFusedLocationProviderClient(context)

        if (fresh) {
            val cts = CancellationTokenSource()
            val freshFix = try {
                withTimeoutOrNull(15.seconds) {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).awaitOrNull()
                }
            } finally {
                cts.cancel()
            }
            if (freshFix != null) return freshFix
        }

        withTimeoutOrNull(3.seconds) { fused.lastLocation.awaitOrNull() }?.let { return it }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lastKnownLocation(manager)
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager): Location? {
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    companion object {
        // Official app's DEFAULT_MINIMUM_ACCURACY: fixes worse than this are disregarded for both
        // location updates and reverse-geocoding.
        private const val MIN_ACCURACY_M = 200f

        // Cache the last reverse-geocode so reports within this radius reuse it instead of waking
        // the radio for a fresh lookup. Backed by prefs so it survives process death.
        private const val GEOCODE_MIN_MOVE_M = 100f
        // Floor between actual Geocoder queries — the official app refreshes this sensor only on
        // its ~15-minute sensor cycle. Also keeps a failing Geocoder from retrying every report.
        private const val GEOCODE_MIN_INTERVAL_MS = 15 * 60_000L
        @Volatile private var lastGeocodeLat: Double? = null
        @Volatile private var lastGeocodeLon: Double? = null
        @Volatile private var lastGeocodeAddress: String? = null
        @Volatile private var lastGeocodeAttemptMs: Long? = null
        @Volatile private var geocodeCacheLoaded = false

        // Last update_location actually sent (process lifetime), for the stale/duplicate gates.
        @Volatile private var lastSentLocationTime = 0L
        @Volatile private var lastSentLat: Double? = null
        @Volatile private var lastSentLon: Double? = null

        private val registrationMutex = Mutex()
        private val json = Json { ignoreUnknownKeys = true }
        @Volatile private var pushChannelEnsured = false
    }
}

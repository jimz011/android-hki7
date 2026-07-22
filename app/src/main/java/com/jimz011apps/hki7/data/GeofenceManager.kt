package com.jimz011apps.hki7.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers a geofence around each Home Assistant zone so the OS — not the app — watches for home
 * arrival/departure. On a boundary crossing the [GeofenceBroadcastReceiver] pushes one fresh location
 * to HA. This is dramatically cheaper than continuous tracking: the radio only wakes on transitions,
 * which is how the official app keeps presence accurate without draining the battery.
 */
class GeofenceManager(private val context: Context) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    /** Fetches HA zones and (re)registers them as geofences. Safe to call repeatedly. */
    @SuppressLint("MissingPermission")
    suspend fun sync(prefs: PreferencesManager, log: (String) -> Unit = {}) {
        // Geofences require background location; without it the OS silently drops them.
        if (!hasBackgroundLocationPermission()) {
            log("Geofencing skipped: no background location permission")
            return
        }
        prefs.ensureHomeAssistantInstanceStore()
        val ssid = currentWifiSsid(context)
        val instances = prefs.homeAssistantInstances.first()
            .filter { it.locationEnabled && it.isAuthenticated }
        val allGeofences = instances.flatMap { instance ->
            val primary = instance.primaryUrl ?: return@flatMap emptyList()
            val active = resolveHomeAssistantUrl(
                instance.externalUrl,
                instance.internalUrl,
                instance.homeSsids,
                ssid
            ) ?: primary
            val client = HomeAssistantClient(active, instance.accessToken.orEmpty())
            val zones = try {
                runCatching { client.getEntities() }
                    .getOrElse {
                        log("${instance.name}: zone fetch failed: ${it.message}")
                        emptyList()
                    }
                    .filter { it.entity_id.startsWith("zone.") }
            } finally {
                client.closeSession()
            }
            zones.mapNotNull { zone ->
                zone.toGeofence("${instance.id.take(12)}:${zone.entity_id}".take(100))
            }
        }
        val geofences = allGeofences.take(MAX_GEOFENCES)

        // Always clear the old merged set first, including when every zone was removed or location
        // sharing was disabled for every instance.
        client.removeGeofences(pendingIntent()).awaitSuccess()
        if (geofences.isEmpty()) {
            log("Geofencing: no usable zones found")
            return
        }

        val request = GeofencingRequest.Builder()
            // Fire an immediate ENTER if we're already inside a zone when registering.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        // Replace any previous set so removed/edited zones don't linger.
        val added = client.addGeofences(request, pendingIntent()).awaitSuccess()
        val truncated = if (allGeofences.size > MAX_GEOFENCES) " (Android limit reached)" else ""
        log(if (added) "Geofencing: registered ${geofences.size} zone(s)$truncated" else "Geofencing: registration failed")
    }

    private fun HAEntity.toGeofence(requestId: String): Geofence? {
        // Passive zones are for automations, not presence — skip them.
        val attributes = attributes ?: return null
        if (attributes["passive"]?.jsonPrimitive?.booleanOrNull == true) return null
        val lat = attributes["latitude"]?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = attributes["longitude"]?.jsonPrimitive?.doubleOrNull ?: return null
        val radius = (attributes["radius"]?.jsonPrimitive?.doubleOrNull ?: DEFAULT_RADIUS_M).toFloat()
        return Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(lat, lon, radius.coerceAtLeast(MIN_RADIUS_M))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            .setAction(GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT)
        // Geofencing requires a mutable PendingIntent so the system can attach the transition.
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    companion object {
        private const val DEFAULT_RADIUS_M = 100.0
        // Geofences smaller than ~100 m trigger unreliably, so clamp up.
        private const val MIN_RADIUS_M = 100f
        private const val MAX_GEOFENCES = 100
    }
}

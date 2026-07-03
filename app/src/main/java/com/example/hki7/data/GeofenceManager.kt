package com.example.hki7.data

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
        val external = prefs.serverUrl.first()?.takeIf { it.isNotBlank() } ?: return
        val internal = prefs.internalUrl.first()
        val ssids = prefs.homeSsids.first()
        val active = resolveHomeAssistantUrl(external, internal, ssids, currentWifiSsid(context)) ?: external
        val token = prefs.accessToken.first().orEmpty()

        val zones = runCatching { HomeAssistantClient(active, token).getEntities() }
            .getOrElse { log("Geofencing: zone fetch failed: ${it.message}"); return }
            .filter { it.entity_id.startsWith("zone.") }

        val geofences = zones.mapNotNull { it.toGeofence() }
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
        client.removeGeofences(pendingIntent()).awaitSuccess()
        val added = client.addGeofences(request, pendingIntent()).awaitSuccess()
        log(if (added) "Geofencing: registered ${geofences.size} zone(s)" else "Geofencing: registration failed")
    }

    private fun HAEntity.toGeofence(): Geofence? {
        // Passive zones are for automations, not presence — skip them.
        if (attributes?.get("passive")?.jsonPrimitive?.booleanOrNull == true) return null
        val lat = attributes?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = attributes?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: return null
        val radius = (attributes?.get("radius")?.jsonPrimitive?.doubleOrNull ?: DEFAULT_RADIUS_M).toFloat()
        return Geofence.Builder()
            .setRequestId(entity_id)
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
    }
}

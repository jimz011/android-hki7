package com.jimz011apps.hki7.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs ONLY while the user has opted into High Accuracy continuous tracking, streaming frequent GPS
 * fixes to Home Assistant. Normal-mode presence/battery does not use this service at all: zone
 * geofences (see [GeofenceManager]) handle arrival/departure and the [TelemetryWorker] WorkManager
 * job refreshes battery/location periodically — the official HA app's battery-friendly model, where
 * the OS can Doze the process instead of it being held awake by a persistent service.
 * Telemetry goes through the mobile_app webhook, which needs no access token once registered.
 */
class LocationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observeJob: Job? = null
    private lateinit var prefs: PreferencesManager
    private lateinit var fusedClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            scope.launch { runCatching { reportOnce(location) } }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        if (observeJob?.isActive != true) {
            observeJob = scope.launch {
                prefs.ensureHomeAssistantInstanceStore()
                val loggedIn = prefs.homeAssistantInstances.first().any { it.locationEnabled && it.isAuthenticated }
                if (!loggedIn) {
                    stopSelf()
                    return@launch
                }
                // This service exists ONLY for opt-in High Accuracy continuous tracking. Normal-mode
                // presence (geofences) and the periodic battery refresh run without a service via
                // WorkManager, so if High Accuracy is off we tear the service down immediately.
                prefs.highAccuracyLocation.collect { highAccuracy ->
                    if (highAccuracy) registerLocationUpdates()
                    else stopSelf()
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdates() {
        if (!hasLocationPermission()) return
        // Always clear any previous continuous request first (e.g. when leaving high-accuracy mode).
        runCatching { fusedClient.removeLocationUpdates(locationCallback) }

        // Normal mode mirrors the official HA app: presence comes from zone geofences (enter/exit)
        // plus the periodic heartbeat, so we deliberately hold NO continuous location request. A
        // 24/7 once-a-minute loop was the main battery drain and is redundant with the geofences.
        // Continuous high-frequency GPS is opt-in only via the High Accuracy setting.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, HIGH_INTERVAL_MS)
            .setMinUpdateIntervalMillis(HIGH_INTERVAL_MS)
            .setMinUpdateDistanceMeters(HIGH_DISTANCE_M)
            .setWaitForAccurateLocation(false)
            .build()

        runCatching {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private suspend fun reportOnce(location: Location?) {
        // Each streamed fix posts update_location only (official-app behavior); the full sensor
        // report would otherwise run — geocode included — every few seconds.
        reportTelemetryNow(applicationContext, prefs, providedLocation = location, locationOnly = true)
    }

    override fun onDestroy() {
        runCatching { fusedClient.removeLocationUpdates(locationCallback) }
        scope.cancel()
        super.onDestroy()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Location sharing", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps your device location updated in Home Assistant"
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Home Assistant location")
            .setContentText("Sharing your location with Home Assistant")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hki7_location"
        private const val NOTIFICATION_ID = 4711

        // High accuracy (opt-in only): GPS every few seconds (very responsive, heavier on battery).
        // This service runs only in that mode; normal-mode presence/battery is handled without a
        // service by geofences + the WorkManager telemetry job ([LocationWork]).
        private const val HIGH_INTERVAL_MS = 5_000L
        private const val HIGH_DISTANCE_M = 0f

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LocationForegroundService::class.java)
                )
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, LocationForegroundService::class.java)) }
        }
    }
}

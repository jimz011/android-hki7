package com.example.hki7.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.IBinder
import com.example.hki7.R
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/** Process-wide visibility shared by the UI websocket and optional push foreground service. */
object AppVisibilityTracker {
    @Volatile var isVisible: Boolean = false
}

/**
 * Turns a websocket push-channel event (a `notify.mobile_app_<device>` service call) into an
 * Android system notification plus an entry in the on-device history. Mirrors the official app's
 * behavior for the common payload fields: `title`, `message`, `data.tag` (replace/cancel),
 * `data.channel` (Android notification channel) and the `clear_notification` special message.
 * Shared by the in-app websocket (app open) and the persistent foreground service (app closed).
 */
class PushNotificationHandler(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    suspend fun handle(event: JsonObject) {
        val message = event["message"]?.jsonPrimitive?.contentOrNull ?: return
        val title = event["title"]?.jsonPrimitive?.contentOrNull
        val data = event["data"] as? JsonObject
        val tag = data?.get("tag")?.jsonPrimitive?.contentOrNull

        if (message.trim().equals("clear_notification", ignoreCase = true)) {
            if (tag != null) notificationManager().cancel(tag.hashCode())
            return
        }

        // The in-app banner is enough while HKI is visible. Keep Android notifications enabled
        // for this app in the background, where the banner cannot be seen.
        if (!AppVisibilityTracker.isVisible) {
            postSystemNotification(title, message, data, tag)
        }
        appendHistory(
            HKINotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                tag = tag
            )
        )
    }

    private fun postSystemNotification(title: String?, message: String, data: JsonObject?, tag: String?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val channelName = data?.get("channel")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "General"
        val channelId = "ha_" + channelName.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_")
        val manager = notificationManager()
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Home Assistant: $channelName"
                }
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val sticky = data?.get("sticky")?.jsonPrimitive?.contentOrNull == "true"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.hki_logo_round))
            .setContentTitle(title ?: "Home Assistant")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(!sticky)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Like the official app: the same tag replaces the previous notification.
        manager.notify(tag?.hashCode() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private suspend fun appendHistory(entry: HKINotification) = historyMutex.withLock {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        val current = prefs.notificationHistory.first()
            .filter { it.archived || it.timestamp >= cutoff }
        prefs.saveNotificationHistory((listOf(entry) + current).take(HISTORY_CAP))
    }

    private fun notificationManager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val HISTORY_CAP = 200
        /** Non-archived notifications are dropped after 48 hours. */
        const val RETENTION_MS = 48L * 60 * 60 * 1000
        // One writer at a time: the ViewModel channel and the foreground service share the store.
        private val historyMutex = Mutex()
    }
}

/**
 * Opt-in persistent connection for push notifications while the app is closed — the official HA
 * app's "persistent connection" mode. Holds one websocket subscribed to the mobile_app push
 * channel and posts notifications via [PushNotificationHandler]. Runs ONLY while the user has the
 * background-notifications toggle on; the default path (app open) needs no service at all.
 */
class PushForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    private lateinit var prefs: PreferencesManager
    private lateinit var handler: PushNotificationHandler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
        handler = PushNotificationHandler(applicationContext, prefs)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (loopJob?.isActive != true) {
            loopJob = scope.launch {
                // Tear down immediately if the user turns the toggle off while we're running.
                launch {
                    prefs.backgroundPushEnabled.collect { enabled -> if (!enabled) stopSelf() }
                }
                runPushLoop()
            }
        }
        return START_STICKY
    }

    private suspend fun runPushLoop() {
        while (currentCoroutineContext().isActive) {
            val webhookId = prefs.mobileAppWebhookId.first()
            val url = resolveHomeAssistantUrl(
                prefs.serverUrl.first(), prefs.internalUrl.first(),
                prefs.homeSsids.first(), currentWifiSsid(applicationContext)
            )
            var token = prefs.accessToken.first()
            if (webhookId.isNullOrBlank() || url.isNullOrBlank() || token.isNullOrBlank()) {
                delay(30.seconds)
                continue
            }
            val client = HomeAssistantClient(url, token)
            try {
                client.subscribePushNotifications(webhookId).collect { event ->
                    runCatching { handler.handle(event) }
                }
            } catch (e: Exception) {
                if (e.message == "AUTH_EXPIRED") {
                    val refresh = prefs.refreshToken.first()
                    if (!refresh.isNullOrBlank()) {
                        runCatching {
                            val fresh = HomeAssistantClient.refreshAccessToken(prefs.serverUrl.first() ?: url, refresh)
                            prefs.saveConnectionDetails(prefs.serverUrl.first() ?: url, fresh.access_token, expiresInSeconds = fresh.expires_in)
                            token = fresh.access_token
                        }
                    }
                }
            } finally {
                client.closeSession()
            }
            delay(10.seconds) // backoff before reconnecting
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Notification connection", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps a connection to Home Assistant for instant notifications"
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // Android insists foreground services show a notification (and bumps MIN-importance
        // channels to LOW for them), so this is as quiet as it can legally be: silent, hidden on
        // the lock screen, no timestamp, and its appearance deferred. The user can hide it fully
        // by disabling this one channel (see the settings shortcut) — the service keeps running.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Connected to Home Assistant")
            .setContentText("Listening for notifications")
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "hki7_push_connection"
        private const val NOTIFICATION_ID = 4712

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, PushForegroundService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, PushForegroundService::class.java)) }
        }
    }
}

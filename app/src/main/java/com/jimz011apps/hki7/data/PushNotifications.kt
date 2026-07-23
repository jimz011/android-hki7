package com.jimz011apps.hki7.data

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
import com.jimz011apps.hki7.R
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

const val EXTRA_HA_INSTANCE_ID = "com.jimz011apps.hki7.extra.HA_INSTANCE_ID"

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
    private val prefs: PreferencesManager,
    private val sourceInstanceId: String? = null,
    private val sourceInstanceName: String? = null
) {
    suspend fun handle(event: JsonObject) {
        val message = event["message"]?.jsonPrimitive?.contentOrNull ?: return
        val title = event["title"]?.jsonPrimitive?.contentOrNull
        val data = event["data"] as? JsonObject
        val tag = data?.get("tag")?.jsonPrimitive?.contentOrNull
        val activeSource = if (sourceInstanceId == null) prefs.activeHomeAssistantInstance.first() else null
        val instanceId = sourceInstanceId ?: activeSource?.id
        val instanceName = sourceInstanceName ?: activeSource?.name

        if (message.trim().equals("clear_notification", ignoreCase = true)) {
            if (tag != null) notificationManager().cancel(notificationId(instanceId, tag))
            return
        }

        // The in-app banner is enough while HKI is visible. Keep Android notifications enabled
        // for this app in the background, where the banner cannot be seen.
        if (!AppVisibilityTracker.isVisible) {
            postSystemNotification(title, message, data, tag, instanceId, instanceName)
        }
        appendHistory(
            HKINotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                tag = tag,
                instanceId = instanceId,
                instanceName = instanceName
            )
        )
    }

    private fun postSystemNotification(
        title: String?,
        message: String,
        data: JsonObject?,
        tag: String?,
        instanceId: String?,
        instanceName: String?
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val channelName = data?.get("channel")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "General"
        val sourcePrefix = instanceId?.take(12)?.replace(Regex("[^A-Za-z0-9]+"), "_") ?: "default"
        val channelId = "ha_${sourcePrefix}_" + channelName.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_")
        val manager = notificationManager()
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    listOfNotNull(instanceName, channelName).joinToString(" · "),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = listOfNotNull("Home Assistant", instanceName, channelName).joinToString(" · ")
                }
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            instanceId?.let { putExtra(EXTRA_HA_INSTANCE_ID, it) }
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                instanceId?.hashCode() ?: 0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val sticky = data?.get("sticky")?.jsonPrimitive?.contentOrNull == "true"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.hki_logo_round))
            .setContentTitle(title ?: instanceName ?: "Home Assistant")
            .setContentText(message)
            .setSubText(instanceName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(!sticky)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Like the official app: the same tag replaces the previous notification.
        manager.notify(
            tag?.let { notificationId(instanceId, it) }
                ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    private fun notificationId(instanceId: String?, tag: String): Int =
        "${instanceId.orEmpty()}:$tag".hashCode()

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            // Android 15+ forbids starting a dataSync foreground service from contexts like a
            // BOOT_COMPLETED receiver (ForegroundServiceStartNotAllowedException). Rather than
            // crash, bow out quietly: the connection re-establishes the next time the app is
            // opened, which re-invokes start() from a foreground-allowed context.
            stopSelf()
            return START_NOT_STICKY
        }
        if (loopJob?.isActive != true) {
            loopJob = scope.launch {
                // Tear down immediately if the user turns the toggle off while we're running.
                launch {
                    prefs.shouldUsePushService.collect { enabled -> if (!enabled) stopSelf() }
                }
                runPushSupervisor()
            }
        }
        return START_STICKY
    }

    private suspend fun runPushSupervisor() {
        prefs.homeAssistantInstances.collectLatest { instances ->
            supervisorScope {
                instances.filter { it.notificationsEnabled && it.isAuthenticated }.forEach { instance ->
                    launch { runInstancePushLoop(instance.id) }
                }
            }
        }
    }

    private suspend fun runInstancePushLoop(instanceId: String) {
        val scopedPrefs = prefs.forInstance(instanceId)
        while (currentCoroutineContext().isActive) {
            val profile = prefs.homeAssistantInstances.first().firstOrNull { it.id == instanceId }
                ?: return
            if (!profile.notificationsEnabled || !profile.isAuthenticated) return
            val webhookId = scopedPrefs.mobileAppWebhookId.first()
            val url = resolveHomeAssistantUrl(
                scopedPrefs.serverUrl.first(), scopedPrefs.internalUrl.first(),
                scopedPrefs.homeSsids.first(), currentWifiSsid(applicationContext)
            )
            val token = scopedPrefs.accessToken.first()
            if (webhookId.isNullOrBlank() || url.isNullOrBlank() || token.isNullOrBlank()) {
                delay(30.seconds)
                continue
            }
            val client = HomeAssistantClient(url, token)
            val handler = PushNotificationHandler(applicationContext, prefs, profile.id, profile.name)
            try {
                client.subscribePushNotifications(webhookId).collect { event ->
                    runCatching { handler.handle(event) }
                }
            } catch (e: Exception) {
                if (e.message == "AUTH_EXPIRED") {
                    val refresh = scopedPrefs.refreshToken.first()
                    if (!refresh.isNullOrBlank()) {
                        runCatching {
                            val refreshUrl = scopedPrefs.serverUrl.first()?.takeIf { it.isNotBlank() } ?: url
                            val fresh = HomeAssistantClient.refreshAccessToken(refreshUrl, refresh)
                            scopedPrefs.saveAuthTokens(fresh.access_token, expiresInSeconds = fresh.expires_in)
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

        /**
         * Boot-time variant. On Android 15+ the OS forbids launching a dataSync foreground service
         * from a BOOT_COMPLETED receiver, so we don't try — the connection comes up the next time
         * the user opens the app. Pre-15 devices restore the persistent connection immediately.
         */
        fun startFromBoot(context: Context) {
            if (android.os.Build.VERSION.SDK_INT >= 35) return
            start(context)
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, PushForegroundService::class.java)) }
        }
    }
}

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
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.jimz011apps.hki7.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

const val EXTRA_HA_INSTANCE_ID = "com.jimz011apps.hki7.extra.HA_INSTANCE_ID"

/**
 * Whether the app may post notifications. POST_NOTIFICATIONS only exists from API 33; below that
 * the platform grants notifications by default and the only "no" is the user switching them off in
 * system settings, which is what [NotificationManagerCompat.areNotificationsEnabled] reports.
 */
fun notificationsAllowed(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

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

        val actions = parseNotificationActions(data)
        val clickAction = data?.get("clickAction")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val historyId = UUID.randomUUID().toString()

        // The in-app banner and panel carry the actions while HKI is visible. Keep Android
        // notifications for the background, where neither can be seen.
        if (!AppVisibilityTracker.isVisible) {
            postSystemNotification(title, message, data, tag, instanceId, instanceName, actions, clickAction, historyId)
        }
        appendHistory(
            HKINotification(
                id = historyId,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                tag = tag,
                instanceId = instanceId,
                instanceName = instanceName,
                actions = actions,
                clickAction = clickAction
            )
        )
    }

    private fun postSystemNotification(
        title: String?,
        message: String,
        data: JsonObject?,
        tag: String?,
        instanceId: String?,
        instanceName: String?,
        actions: List<HKINotificationAction>,
        clickAction: String?,
        historyId: String
    ) {
        if (!notificationsAllowed(context)) return

        val requestedChannel = data?.get("channel")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val channelName = requestedChannel ?: context.getString(R.string.background_notification_channel_general)
        val sourcePrefix = instanceId?.take(12)?.replace(Regex("[^A-Za-z0-9]+"), "_") ?: "default"
        val channelIdPart = requestedChannel ?: "general"
        val channelId = "ha_${sourcePrefix}_" + channelIdPart.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")
        val manager = notificationManager()
        // Recreate to refresh the localized fallback name after an app-language change. Android
        // retains the user's importance and other channel preferences for an existing ID.
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                listOfNotNull(instanceName, channelName).joinToString(" · "),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = listOfNotNull("Home Assistant", instanceName, channelName).joinToString(" · ")
            }
        )

        val systemId = tag?.let { notificationId(instanceId, it) }
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val sticky = data?.get("sticky")?.jsonPrimitive?.contentOrNull == "true"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.hki_logo_round))
            .setContentTitle(title ?: instanceName ?: "Home Assistant")
            .setContentText(message)
            .setSubText(instanceName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(bodyIntent(clickAction, instanceId))
            .setAutoCancel(!sticky)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        actions.forEachIndexed { index, action ->
            builder.addAction(buildAction(action, index, systemId, instanceId, historyId))
        }

        // Like the official app: the same tag replaces the previous notification.
        manager.notify(systemId, builder.build())
    }

    /**
     * What tapping the notification body does. `noAction` suppresses the tap entirely and a web
     * URL opens the browser; everything else (HA's `/lovelace/…` paths, `entityId:…`) opens HKI on
     * the sending server, since HKI's own dashboards don't share Lovelace's URL space.
     */
    private fun bodyIntent(clickAction: String?, instanceId: String?): PendingIntent? {
        if (clickAction.equals("noAction", ignoreCase = true)) return null
        webIntent(clickAction)?.let {
            return PendingIntent.getActivity(
                context, clickAction.hashCode(), it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            instanceId?.let { putExtra(EXTRA_HA_INSTANCE_ID, it) }
        } ?: return null
        return PendingIntent.getActivity(
            context, instanceId?.hashCode() ?: 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildAction(
        action: HKINotificationAction,
        index: Int,
        systemId: Int,
        instanceId: String?,
        historyId: String
    ): NotificationCompat.Action {
        val requestCode = 31 * systemId + index
        // A URI action must go straight to an activity: since Android 12 a broadcast receiver may
        // not start one on the notification's behalf (the "notification trampoline" ban).
        webIntent(action.uri)?.let { intent ->
            return NotificationCompat.Action.Builder(
                0, action.title,
                PendingIntent.getActivity(
                    context, requestCode, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).build()
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            // PendingIntent equality ignores extras, so make the intent itself unique per action.
            this.action = "com.jimz011apps.hki7.action.NOTIFICATION_ACTION/$historyId/${action.action}"
            putExtra(EXTRA_NOTIFICATION_ACTION, action.action)
            putExtra(EXTRA_NOTIFICATION_ACTION_DATA, NotificationActions.encodeActionData(action.actionData))
            putExtra(EXTRA_NOTIFICATION_HISTORY_ID, historyId)
            putExtra(EXTRA_NOTIFICATION_SYSTEM_ID, systemId)
            instanceId?.let { putExtra(EXTRA_HA_INSTANCE_ID, it) }
        }
        // Mutable is required for reply actions: the system writes the typed text into the intent.
        val flags = if (action.isReply) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        }
        val builder = NotificationCompat.Action.Builder(
            0, action.title, PendingIntent.getBroadcast(context, requestCode, intent, flags)
        )
        if (action.isReply) {
            builder.addRemoteInput(
                RemoteInput.Builder(NotificationActions.REPLY_RESULT_KEY)
                    .setLabel(action.title)
                    .build()
            ).setAllowGeneratedReplies(true)
        }
        return builder.build()
    }

    /** An `ACTION_VIEW` intent for a web link, or null when [uri] isn't one. */
    private fun webIntent(uri: String?): Intent? {
        val target = uri?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        } ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun notificationId(instanceId: String?, tag: String): Int =
        "${instanceId.orEmpty()}:$tag".hashCode()

    private suspend fun appendHistory(entry: HKINotification) = notificationHistoryMutex.withLock {
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
    }
}

// One writer at a time: the ViewModel channel, the foreground service and notification-action
// taps all share the history store.
private val notificationHistoryMutex = Mutex()

/** Reads HA's `data.actions` into the model. Entries without an `action` key are unusable. */
internal fun parseNotificationActions(data: JsonObject?): List<HKINotificationAction> {
    val entries = data?.get("actions") as? JsonArray ?: return emptyList()
    return entries.mapNotNull { element ->
        val entry = element as? JsonObject ?: return@mapNotNull null
        val action = entry["action"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        HKINotificationAction(
            action = action,
            title = entry["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: action,
            uri = entry["uri"]?.jsonPrimitive?.contentOrNull,
            actionData = entry["action_data"] as? JsonObject,
            behavior = entry["behavior"]?.jsonPrimitive?.contentOrNull
        )
    }.take(NotificationActions.MAX_ACTIONS)
}

/** Records that an action was used so the in-app panel stops offering it. */
internal suspend fun markNotificationActionFired(
    context: Context,
    historyId: String,
    action: String
) = notificationHistoryMutex.withLock {
    val prefs = PreferencesManager(context.applicationContext)
    val history = prefs.notificationHistory.first()
    if (history.none { it.id == historyId }) return@withLock
    prefs.saveNotificationHistory(
        history.map { if (it.id == historyId) it.copy(firedAction = action) else it }
    )
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
            isRunning = true
        } catch (e: Exception) {
            // Android 15+ forbids starting a dataSync foreground service from contexts like a
            // BOOT_COMPLETED receiver (ForegroundServiceStartNotAllowedException). Rather than
            // crash, bow out quietly and leave [isRunning] false: the in-app subscription checks
            // that flag and takes the channel over, so a refused start no longer means nobody is
            // subscribed at all.
            isRunning = false
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
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.background_push_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.background_push_channel_description)
            }
        )
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // The escape hatch belongs on the notification itself. Settings has the same shortcut, but
        // someone bothered by this is looking at the shade, not hunting through Settings — and it
        // switches itself on for multi-home setups, so they may never have opted into it at all.
        // Straight to an activity: a broadcast receiver may not start one on a notification's
        // behalf since Android 12.
        val hideIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val hidePendingIntent = PendingIntent.getActivity(
            this, 1, hideIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Android insists foreground services show a notification (and bumps MIN-importance
        // channels to LOW for them), so this is as quiet as it can legally be: silent, hidden on
        // the lock screen, no timestamp, and its appearance deferred. The user can hide it fully
        // by disabling this one channel (see the settings shortcut) — the service keeps running.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.background_push_notification_title))
            .setContentText(getString(R.string.background_push_notification_text))
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.background_push_hide_action), hidePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "hki7_push_connection"
        private const val NOTIFICATION_ID = 4712

        /**
         * Whether the service currently holds the push subscription.
         *
         * The in-app subscription stands aside for this service, and used to do so purely because
         * the *settings* said the service should run. When Android refused to start it — a boot on
         * 15+, a killed service, a blocked start — nothing was subscribed at all, and Home
         * Assistant answered every notify with "not connected to local push notifications" even
         * with the app open on screen. Intent is not the same as reality, so this reports reality.
         */
        @Volatile
        var isRunning: Boolean = false
            private set

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

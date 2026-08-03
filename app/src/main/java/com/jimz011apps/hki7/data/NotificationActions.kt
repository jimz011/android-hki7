package com.jimz011apps.hki7.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

const val EXTRA_NOTIFICATION_ACTION = "com.jimz011apps.hki7.extra.NOTIFICATION_ACTION"
const val EXTRA_NOTIFICATION_ACTION_DATA = "com.jimz011apps.hki7.extra.NOTIFICATION_ACTION_DATA"
const val EXTRA_NOTIFICATION_HISTORY_ID = "com.jimz011apps.hki7.extra.NOTIFICATION_HISTORY_ID"
const val EXTRA_NOTIFICATION_SYSTEM_ID = "com.jimz011apps.hki7.extra.NOTIFICATION_SYSTEM_ID"

/** Outcome of one dispatch attempt — a missing webhook is permanent, a failed POST is not. */
enum class ActionDispatchResult { SENT, RETRY, UNCONFIGURED }

/**
 * Sends a notification action back to Home Assistant as a `mobile_app_notification_action` event,
 * the same event the official app fires, so existing HA automations work unchanged.
 *
 * Both delivery paths land here: [NotificationActionReceiver] when the tap came from the system
 * shade (app closed), and the notification panel via MainViewModel when the app is open. HKI
 * suppresses system notifications while visible, so the in-app buttons are not a convenience —
 * they are the only actions the user ever sees in that state.
 */
object NotificationActions {
    const val EVENT_TYPE = "mobile_app_notification_action"
    const val REPLY_RESULT_KEY = "hki7_notification_reply"

    /** Android's shade renders at most three action buttons; the panel matches so both agree. */
    const val MAX_ACTIONS = 3

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fire(
        context: Context,
        instanceId: String?,
        action: String,
        actionData: JsonObject? = null,
        replyText: String? = null
    ): ActionDispatchResult {
        val appContext = context.applicationContext
        val root = PreferencesManager(appContext)
        root.ensureHomeAssistantInstanceStore()
        val prefs = instanceId?.let { root.forInstance(it) } ?: root

        val webhookId = prefs.mobileAppWebhookId.first()?.takeIf { it.isNotBlank() }
            ?: return ActionDispatchResult.UNCONFIGURED
        // Same endpoint selection as telemetry: a cloudhook works from anywhere, otherwise the
        // internal URL on home Wi-Fi and the external one elsewhere.
        val baseUrl = resolveHomeAssistantUrl(
            prefs.serverUrl.first(), prefs.internalUrl.first(),
            prefs.homeSsids.first(), currentWifiSsid(appContext)
        )
        val webhookUrl = prefs.mobileAppCloudhookUrl.first()?.takeIf { it.isNotBlank() }
            ?: baseUrl?.takeIf { it.isNotBlank() }?.let { "${it.removeSuffix("/")}/api/webhook/$webhookId" }
            ?: return ActionDispatchResult.UNCONFIGURED

        val payload = buildJsonObject {
            put("type", "fire_event")
            put("data", buildJsonObject {
                put("event_type", EVENT_TYPE)
                put("event_data", buildJsonObject {
                    put("action", action)
                    replyText?.let { put("reply_text", it) }
                    // action_data is merged in flat, matching the official app's event shape.
                    actionData?.forEach { (key, value) -> put(key, value) }
                })
            })
        }

        val client = HomeAssistantClient(baseUrl ?: webhookUrl, prefs.accessToken.first().orEmpty())
        return try {
            val (status, _) = client.postWebhook(webhookUrl, payload)
            if (status in 200..299) ActionDispatchResult.SENT else ActionDispatchResult.RETRY
        } catch (_: Exception) {
            ActionDispatchResult.RETRY
        } finally {
            client.dispose()
        }
    }

    fun encodeActionData(actionData: JsonObject?): String? = actionData?.toString()

    fun decodeActionData(raw: String?): JsonObject? =
        raw?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
}

/**
 * Receives taps on the system notification's action buttons. Android 12+ forbids a receiver from
 * starting an activity ("notification trampoline"), so nothing here launches UI — `URI` actions get
 * their own activity PendingIntent instead (see PushNotificationHandler).
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(EXTRA_NOTIFICATION_ACTION) ?: return
        val instanceId = intent.getStringExtra(EXTRA_HA_INSTANCE_ID)
        val actionData = intent.getStringExtra(EXTRA_NOTIFICATION_ACTION_DATA)
        val historyId = intent.getStringExtra(EXTRA_NOTIFICATION_HISTORY_ID)
        val systemId = intent.getIntExtra(EXTRA_NOTIFICATION_SYSTEM_ID, 0)
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationActions.REPLY_RESULT_KEY)?.toString()?.takeIf { it.isNotBlank() }

        val appContext = context.applicationContext
        if (systemId != 0) {
            (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(systemId)
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (historyId != null) {
                    runCatching { markNotificationActionFired(appContext, historyId, action) }
                }
                // Try inline first so a tap feels immediate; a broadcast gets ~10s before the OS
                // kills it, so anything slower or offline falls back to retryable work.
                val result = withTimeoutOrNull(8.seconds) {
                    NotificationActions.fire(appContext, instanceId, action, NotificationActions.decodeActionData(actionData), replyText)
                }
                if (result == null || result == ActionDispatchResult.RETRY) {
                    NotificationActionWorker.enqueue(appContext, instanceId, action, actionData, replyText)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Retry path for an action that could not be delivered while the user tapped it (offline, VPN
 *  still coming up, server restarting). Waits for connectivity, then backs off exponentially. */
class NotificationActionWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.success()
        val result = NotificationActions.fire(
            applicationContext,
            inputData.getString(KEY_INSTANCE_ID),
            action,
            NotificationActions.decodeActionData(inputData.getString(KEY_ACTION_DATA)),
            inputData.getString(KEY_REPLY_TEXT)
        )
        return when (result) {
            ActionDispatchResult.SENT, ActionDispatchResult.UNCONFIGURED -> Result.success()
            // Give up after the default backoff ladder rather than firing a stale action hours late.
            ActionDispatchResult.RETRY -> if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val KEY_INSTANCE_ID = "instance_id"
        private const val KEY_ACTION = "action"
        private const val KEY_ACTION_DATA = "action_data"
        private const val KEY_REPLY_TEXT = "reply_text"
        private const val MAX_ATTEMPTS = 4

        fun enqueue(
            context: Context,
            instanceId: String?,
            action: String,
            actionData: String?,
            replyText: String?
        ) {
            val request = OneTimeWorkRequestBuilder<NotificationActionWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_INSTANCE_ID, instanceId)
                        .putString(KEY_ACTION, action)
                        .putString(KEY_ACTION_DATA, actionData)
                        .putString(KEY_REPLY_TEXT, replyText)
                        .build()
                )
                .build()
            // Unique per action so two different buttons tapped in quick succession both survive.
            WorkManager.getInstance(context)
                .enqueueUniqueWork("hki7_notification_action_$action", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}

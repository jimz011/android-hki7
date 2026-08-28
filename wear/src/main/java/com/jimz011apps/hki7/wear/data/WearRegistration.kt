package com.jimz011apps.hki7.wear.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Registers the watch with Home Assistant as its own `mobile_app` device.
 *
 * This is what makes the watch a device in Home Assistant rather than a remote control for one:
 * it gets its own entities, so the watch's battery can drive automations ("remind me to charge my
 * watch"), and it appears alongside the phone rather than being invisible.
 *
 * A separate registration from the phone's, deliberately — same Home Assistant, two devices, two
 * device_ids. Sharing the phone's webhook would make the watch overwrite the phone's battery
 * sensor, which is worse than not reporting at all.
 *
 * Opt-in. Nothing here runs until the user turns watch sensors on, because a watch reporting its
 * battery every quarter of an hour is a cost the user should choose rather than inherit.
 */
object WearRegistration {

    /** One registration at a time, so two triggers cannot create two devices in Home Assistant. */
    private val mutex = Mutex()

    @Serializable
    data class RegistrationResponse(
        @SerialName("webhook_id") val webhookId: String,
        @SerialName("cloudhook_url") val cloudhookUrl: String? = null,
        @SerialName("remote_ui_url") val remoteUiUrl: String? = null,
    )

    /**
     * Returns this watch's webhook id, registering if it has not been registered against
     * [serverUrl] before. Null when Home Assistant refuses.
     */
    suspend fun ensureRegistered(
        context: Context,
        prefs: WearPreferences,
        session: WearSession,
        serverUrl: String,
    ): String? = mutex.withLock {
        prefs.webhookIdFor(serverUrl)?.let { return@withLock it }

        val deviceId = prefs.deviceId() ?: UUID.randomUUID().toString().also { prefs.saveDeviceId(it) }
        val body = buildJsonObject {
            put("device_id", deviceId)
            // A distinct app_id from the phone's "hki7": Home Assistant keys the integration on it,
            // and reusing the phone's would make the watch look like a second copy of the phone.
            put("app_id", "hki7_wear")
            put("app_name", "HKI 7 (Wear OS)")
            put("app_version", com.jimz011apps.hki7.wear.BuildConfig.VERSION_NAME)
            put("device_name", watchName())
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("os_name", "Wear OS")
            put("os_version", Build.VERSION.RELEASE)
            put("supports_encryption", false)
        }

        val response = runCatching {
            post(serverUrl, "/api/mobile_app/registrations", body, session.accessToken())
        }.getOrNull() ?: return@withLock null

        val registration = runCatching {
            wearJson.decodeFromString(RegistrationResponse.serializer(), response)
        }.getOrNull() ?: return@withLock null

        prefs.saveRegistration(serverUrl, registration.webhookId)
        registration.webhookId
    }

    /** Posts a webhook message. Webhooks are unauthenticated — the id is the credential. */
    suspend fun postWebhook(serverUrl: String, webhookId: String, payload: JsonObject): Boolean =
        runCatching {
            post(serverUrl, "/api/webhook/$webhookId", payload, token = null)
            true
        }.getOrDefault(false)

    private fun watchName(): String =
        Build.MODEL?.takeIf { it.isNotBlank() } ?: "Wear OS watch"

    private suspend fun post(
        serverUrl: String,
        path: String,
        body: JsonObject,
        token: String?,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(serverUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 15_000
                readTimeout = 20_000
            }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}

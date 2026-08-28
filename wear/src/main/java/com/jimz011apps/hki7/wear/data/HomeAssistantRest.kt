package com.jimz011apps.hki7.wear.data

import com.jimz011apps.hki7.data.HAEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The watch's Home Assistant client: REST over `HttpURLConnection`.
 *
 * Deliberately not the phone's [com.jimz011apps.hki7.data.HomeAssistantClient]. That one is built
 * around a live WebSocket session; bringing it here would drag Ktor and OkHttp into the watch APK
 * for calls that are all request/response. Live updates are a separate concern — see the note in
 * WearViewModel about what the watch does while a screen is actually visible.
 *
 * Every request takes a freshly validated access token from [WearSession] and retries once on a
 * 401, so a token that expired mid-session recovers without the user seeing anything.
 *
 * On a watch with no Wi-Fi or LTE of its own, Wear OS routes this through the paired phone, so it
 * works on Bluetooth-only watches too.
 */
class HomeAssistantRest(
    private val serverUrl: String,
    private val session: WearSession,
) {
    private val baseUrl = serverUrl.trimEnd('/')

    /** One entity, or null if Home Assistant does not know it. */
    suspend fun state(entityId: String): HAEntity? = withContext(Dispatchers.IO) {
        val body = request("GET", "/api/states/$entityId", null, allowNotFound = true)
            ?: return@withContext null
        runCatching { wearJson.decodeFromString(HAEntity.serializer(), body) }.getOrNull()
    }

    /** Every entity Home Assistant knows. Only used where the set is genuinely unknown. */
    suspend fun states(): List<HAEntity> = withContext(Dispatchers.IO) {
        val body = request("GET", "/api/states", null) ?: return@withContext emptyList()
        wearJson.decodeFromString(ListSerializer(HAEntity.serializer()), body)
    }

    suspend fun callService(domain: String, service: String, payload: JsonObject) {
        withContext(Dispatchers.IO) {
            request("POST", "/api/services/$domain/$service", payload.toString())
        }
    }

    suspend fun toggle(entityId: String) {
        callService(entityId.substringBefore('.'), "toggle", buildEntityPayload(entityId))
    }

    /**
     * Performs one request, renewing the access token and retrying once if Home Assistant rejects
     * it. A 401 after a successful renewal is a real authorization failure and is not retried again.
     */
    private suspend fun request(
        method: String,
        path: String,
        body: String?,
        allowNotFound: Boolean = false,
    ): String? {
        var token = session.accessToken()
        repeat(2) { attempt ->
            val connection = open(method, path, token)
            try {
                body?.let { payload ->
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(payload.toByteArray()) }
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED && attempt == 0) {
                    connection.disconnect()
                    token = session.forceRenew()
                    return@repeat
                }
                if (allowNotFound && code == HttpURLConnection.HTTP_NOT_FOUND) return null
                if (code !in 200..299) throw IOException("HTTP $code")
                return connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Unauthorized")
    }

    private fun open(method: String, path: String, token: String): HttpURLConnection =
        (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doOutput = method == "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            // Long enough to ride out a Bluetooth-proxied connection, short enough that a dead
            // server does not hang the screen.
            connectTimeout = 15_000
            readTimeout = 20_000
        }
}

package com.jimz011apps.hki7.wear.data

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAStateChange
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Live entity updates on the watch, over Home Assistant's WebSocket API.
 *
 * Open only while a screen is actually visible. That is the distinction that matters on a watch:
 * a socket held open all day would keep the radio awake for nothing, but a watch screen lives for
 * a few seconds at a time, and for those seconds the user expects what they are looking at to be
 * true. A light someone switches at the wall moves here without being asked.
 *
 * Tiles and complications deliberately do not use this — they are pull-based on their own refresh
 * schedule, because they update with no one looking.
 *
 * Written directly against OkHttp rather than reusing the phone's Ktor client: this needs one
 * socket and three message types, and Ktor plus its engine would be a large addition to a watch
 * APK for that.
 */
class HomeAssistantSocket(
    private val serverUrl: String,
    private val session: WearSession,
) {
    private val client = OkHttpClient.Builder()
        // Home Assistant sends nothing while the house is quiet; without pings a dozing proxy
        // silently drops the connection and updates just stop arriving.
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    /**
     * Emits every `state_changed` Home Assistant reports, for as long as it is collected.
     *
     * Reconnects on failure with a bounded backoff: a watch that loses Bluetooth for a moment
     * should recover on its own rather than needing the screen reopened.
     */
    fun stateChanges(): Flow<HAStateChange> = callbackFlow {
        val token = session.accessToken()
        val request = Request.Builder().url(websocketUrl()).build()
        var nextId = 1

        val socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = runCatching {
                        wearJson.parseToJsonElement(text).jsonObject
                    }.getOrNull() ?: return
                    when (message["type"]?.jsonPrimitive?.contentOrNull) {
                        // Home Assistant speaks first, and will not accept anything until it
                        // has been answered.
                        "auth_required" -> webSocket.send(
                            buildJsonObject {
                                put("type", "auth")
                                put("access_token", token)
                            }.toString(),
                        )
                        "auth_ok" -> webSocket.send(
                            buildJsonObject {
                                put("id", nextId++)
                                put("type", "subscribe_events")
                                put("event_type", "state_changed")
                            }.toString(),
                        )
                        "auth_invalid" -> close(IllegalStateException("auth_invalid"))
                        "event" -> parseStateChange(message)?.let { trySend(it) }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    close(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    close()
                }
            },
        )

        awaitClose { socket.cancel() }
    }.retryWhen { cause, attempt ->
        // A rejected token is not something reconnecting can fix; everything else usually is.
        if (cause is IllegalStateException) return@retryWhen false
        delay(minOf(RECONNECT_BASE_MILLIS * (attempt + 1), RECONNECT_MAX_MILLIS))
        true
    }

    private fun parseStateChange(message: JsonObject): HAStateChange? {
        val data = message["event"]?.jsonObject?.get("data")?.jsonObject ?: return null
        val entityId = data["entity_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val newState = data["new_state"]
            ?.takeUnless { it is JsonNull }
            ?.let { runCatching { wearJson.decodeFromJsonElement(HAEntity.serializer(), it) }.getOrNull() }
        return HAStateChange(entityId, newState)
    }

    /** `http(s)` becomes `ws(s)`; everything else about the address is the user's. */
    private fun websocketUrl(): String {
        val base = serverUrl.trimEnd('/')
        val socketBase = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }
        return "$socketBase/api/websocket"
    }

    /** Releases the connection pool. The flow itself stops when collection does. */
    fun dispose() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private companion object {
        const val RECONNECT_BASE_MILLIS = 2_000L
        const val RECONNECT_MAX_MILLIS = 30_000L
    }
}

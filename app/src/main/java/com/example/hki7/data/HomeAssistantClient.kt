@file:Suppress("SpellCheckingInspection")

package com.example.hki7.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class HomeAssistantClient(
    serverUrl: String,
    private val accessToken: String
) {
    private val baseUrl = serverUrl.removeSuffix("/")
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        // Ktor 3 defaults to expectSuccess=false, so a 401 never throws ResponseException on its
        // own — it would surface as a JSON parse error of the "401: Unauthorized" body and the
        // token-refresh path would never trigger. Map it explicitly, but only for authorized
        // calls (webhook POSTs are unauthenticated and inspect their status manually).
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized &&
                    response.call.request.headers.contains(HttpHeaders.Authorization)
                ) {
                    throw Exception("AUTH_EXPIRED")
                }
            }
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
        engine {
            config {
                pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    /** One live websocket plus the response channels registered on it. Channels are scoped to the
     *  connection so a dying socket only unblocks its own waiters — never a newer connection's. */
    private class WsConnection(val session: DefaultClientWebSocketSession) {
        val channels = ConcurrentHashMap<Int, Channel<JsonObject>>()
    }

    private val connectMutex = Mutex()
    @Volatile private var connection: WsConnection? = null
    private val messageId = AtomicInteger(1)

    suspend fun getEntities(): List<HAEntity> {
        return withAuthHandling {
            val responseText: String = client.get("$baseUrl/api/states") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }.body()
            json.decodeFromString(ListSerializer(HAEntity.serializer()), responseText)
        }
    }

    suspend fun getAreas(): List<HAArea> {
        return withWebSocket {
            val response = sendCommand("config/area_registry/list")
            val result = response["result"]?.jsonArray ?: return@withWebSocket emptyList()
            json.decodeFromJsonElement(ListSerializer(HAArea.serializer()), result)
        }
    }

    suspend fun getFloors(): List<HAFloor> {
        return withWebSocket {
            val response = sendCommand("config/floor_registry/list")
            val result = response["result"]?.jsonArray ?: return@withWebSocket emptyList()
            json.decodeFromJsonElement(ListSerializer(HAFloor.serializer()), result)
        }
    }

    suspend fun getEntityRegistry(): List<HAEntityRegistryEntry> {
        return withWebSocket {
            val response = sendCommand("config/entity_registry/list")
            val result = response["result"]?.jsonArray ?: return@withWebSocket emptyList()
            json.decodeFromJsonElement(ListSerializer(HAEntityRegistryEntry.serializer()), result)
        }
    }

    suspend fun getDeviceRegistry(): List<HADeviceRegistryEntry> {
        return withWebSocket {
            val response = sendCommand("config/device_registry/list")
            val result = response["result"]?.jsonArray ?: return@withWebSocket emptyList()
            json.decodeFromJsonElement(ListSerializer(HADeviceRegistryEntry.serializer()), result)
        }
    }

    /** Read-only copy of the entities configured in Home Assistant's Energy dashboard. */
    suspend fun getEnergyPreferences(): JsonObject? {
        return withWebSocket {
            val response = sendCommand("energy/get_prefs")
            if (response["success"]?.jsonPrimitive?.booleanOrNull != true) return@withWebSocket null
            response["result"]?.jsonObject
        }
    }

    /** Hourly solar forecasts from providers configured in Home Assistant's Energy dashboard. */
    suspend fun getEnergySolarForecasts(): JsonObject? {
        return withWebSocket {
            val response = sendCommand("energy/solar_forecast")
            if (response["success"]?.jsonPrimitive?.booleanOrNull != true) return@withWebSocket null
            response["result"]?.jsonObject
        }
    }

    /** Browses a media player's library (root when contentId is null). */
    suspend fun browseMedia(entityId: String, contentId: String? = null, contentType: String? = null): HAMediaBrowseItem? {
        return withWebSocket {
            val data = buildMap<String, JsonElement> {
                put("entity_id", JsonPrimitive(entityId))
                if (contentId != null) put("media_content_id", JsonPrimitive(contentId))
                if (contentType != null) put("media_content_type", JsonPrimitive(contentType))
            }
            val response = sendCommand("media_player/browse_media", data)
            response["result"]?.let { json.decodeFromJsonElement(HAMediaBrowseItem.serializer(), it) }
        }
    }

    suspend fun getWeatherForecast(entityId: String, type: String = "daily"): List<HAWeatherForecast> {
        return withWebSocket {
            val response = sendCommand(
                "call_service",
                mapOf(
                    "domain" to JsonPrimitive("weather"),
                    "service" to JsonPrimitive("get_forecasts"),
                    "service_data" to buildJsonObject {
                        put("type", type)
                    },
                    "target" to buildJsonObject {
                        put("entity_id", entityId)
                    },
                    "return_response" to JsonPrimitive(true)
                )
            )
            val responseObject = response["result"]?.jsonObject?.get("response")?.jsonObject
                ?: response["result"]?.jsonObject
                ?: return@withWebSocket emptyList()
            val forecast = responseObject[entityId]?.jsonObject?.get("forecast")?.jsonArray
                ?: responseObject.values.firstNotNullOfOrNull { element ->
                    runCatching { element.jsonObject["forecast"]?.jsonArray }.getOrNull()
                }
                ?: return@withWebSocket emptyList()
            forecast.map { it.asHAWeatherForecast() }
        }
    }

    suspend fun getCalendarEvents(
        entityIds: List<String>,
        startMillis: Long,
        endMillis: Long
    ): Map<String, List<HACalendarEvent>> {
        val start = Instant.ofEpochMilli(startMillis).atOffset(ZoneOffset.UTC)
        val end = Instant.ofEpochMilli(endMillis).atOffset(ZoneOffset.UTC)
        return entityIds.distinct().associateWith { entityId ->
            val responseText: String = withAuthHandling {
                client.get("$baseUrl/api/calendars/$entityId") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    parameter("start", start.toString())
                    parameter("end", end.toString())
                }.body()
            }
            json.decodeFromString(ListSerializer(HACalendarEvent.serializer()), responseText)
                .map { it.copy(entityId = entityId) }
        }
    }

    suspend fun getEntityHistory(entityId: String, hours: Long = 24): List<HAHistoryEntry> {
        val end = OffsetDateTime.now(ZoneOffset.UTC)
        val start = end.minusHours(hours.coerceAtLeast(1))
        val responseText: String = withAuthHandling {
            client.get("$baseUrl/api/history/period/${start}") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("filter_entity_id", entityId)
                parameter("end_time", end.toString())
                parameter("significant_changes_only", "1")
            }.body()
        }

        val nestedList = json.decodeFromString<List<List<HAHistoryEntry>>>(responseText)
        val history = nestedList.flatten().reversed()
        val logbook = runCatching { getEntityLogbook(entityId, start, end) }.getOrDefault(emptyList())
        val userNamesById = getUserNamesById()
        return history.map { entry: HAHistoryEntry -> entry.withActor(entityId, logbook, userNamesById) }
    }

    private suspend fun getEntityLogbook(
        entityId: String,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<HALogbookEntry> {
        val responseText: String = withAuthHandling {
            client.get("$baseUrl/api/logbook/${start}") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("entity", entityId)
                parameter("end_time", end.toString())
            }.body()
        }
        return json.decodeFromString(ListSerializer(HALogbookEntry.serializer()), responseText)
    }

    suspend fun getCurrentUserName(): String? {
        return getCurrentUser()?.displayName
    }

    suspend fun getCurrentUser(): HAUser? {
        return withWebSocket {
            val response = sendCommand("auth/current_user")
            val result = response["result"]?.jsonObject ?: return@withWebSocket null
            val id = result["id"]?.jsonPrimitive?.contentOrNull ?: return@withWebSocket null
            HAUser(
                id = id,
                name = result["name"]?.jsonPrimitive?.contentOrNull,
                username = result["username"]?.jsonPrimitive?.contentOrNull,
                is_admin = result["is_admin"]?.jsonPrimitive?.booleanOrNull
            )
        }
    }

    suspend fun getUsers(): List<HAUser> {
        return withWebSocket {
            val response = sendCommand("config/auth/list")
            if (response["success"]?.jsonPrimitive?.booleanOrNull == false) {
                return@withWebSocket emptyList()
            }
            val result = response["result"]?.jsonArray ?: return@withWebSocket emptyList()
            json.decodeFromJsonElement(ListSerializer(HAUser.serializer()), result)
        }
    }

    private suspend fun <T> withWebSocket(block: suspend () -> T): T {
        ensureConnected()
        return block()
    }

    /** Establishes (or reuses) the single live websocket. Serialized so concurrent callers —
     *  realtime sync, the push channel, parallel registry fetches — share one connection instead
     *  of racing to create sockets that stomp on and leak each other. */
    private suspend fun ensureConnected(): WsConnection {
        connection?.takeIf { it.session.isActive }?.let { return it }
        connectMutex.withLock {
            connection?.takeIf { it.session.isActive }?.let { return it }
            dropConnection()

            val activeSession = withTimeout(10.seconds) {
                client.webSocketSession(webSocketUrl())
            }
            try {
                val authMsg = withTimeout(10.seconds) {
                    activeSession.incoming.receive()
                } as? Frame.Text
                    ?: throw Exception("WS connection failed")
                val authType = json.parseToJsonElement(authMsg.readText())
                    .jsonObject["type"]?.jsonPrimitive?.content

                if (authType == "auth_required") {
                    activeSession.send(buildJsonObject {
                        put("type", "auth")
                        put("access_token", accessToken)
                    }.toString())

                    val authResult = withTimeout(10.seconds) {
                        activeSession.incoming.receive()
                    } as? Frame.Text
                        ?: throw Exception("Auth failed")
                    val resultType = json.parseToJsonElement(authResult.readText())
                        .jsonObject["type"]?.jsonPrimitive?.content
                    if (resultType == "auth_invalid") throw Exception("AUTH_EXPIRED")
                    if (resultType != "auth_ok") throw Exception("Auth rejected: $resultType")
                }
            } catch (e: Exception) {
                runCatching { activeSession.close() }
                throw e
            }

            val conn = WsConnection(activeSession)
            connection = conn
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (frame in activeSession.incoming) {
                        if (frame is Frame.Text) {
                            val response = json.parseToJsonElement(frame.readText()).jsonObject
                            val id = response["id"]?.jsonPrimitive?.intOrNull
                            if (id != null) {
                                conn.channels[id]?.send(response)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (connection === conn) connection = null
                    // Unblock this connection's request/subscription waiters so they can error out and reconnect.
                    conn.channels.values.forEach { runCatching { it.close() } }
                    conn.channels.clear()
                }
            }
            return conn
        }
    }

    /** Drops the current connection (if any): unblocks its waiters and closes the socket. */
    private fun dropConnection() {
        val current = connection ?: return
        connection = null
        current.channels.values.forEach { runCatching { it.close() } }
        current.channels.clear()
        CoroutineScope(Dispatchers.IO).launch { runCatching { current.session.close() } }
    }

    private fun webSocketUrl(): String {
        return when {
            baseUrl.startsWith("https://") -> baseUrl.replaceFirst("https://", "wss://")
            baseUrl.startsWith("http://") -> baseUrl.replaceFirst("http://", "ws://")
            else -> baseUrl
        } + "/api/websocket"
    }

    private suspend fun sendCommand(type: String, data: Map<String, JsonElement> = emptyMap()): JsonObject {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                return sendCommandOnce(type, data)
            } catch (e: Exception) {
                lastError = e
                if (e.message == "AUTH_EXPIRED" || attempt == 1) throw e
                delay(150.milliseconds)
            }
        }
        throw lastError ?: Exception("WebSocket command failed")
    }

    private suspend fun sendCommandOnce(
        type: String,
        data: Map<String, JsonElement> = emptyMap()
    ): JsonObject {
        val conn = ensureConnected()
        val id = messageId.getAndIncrement()
        val command = buildJsonObject {
            put("id", id)
            put("type", type)
            data.forEach { (key, value) -> put(key, value) }
        }

        val channel = Channel<JsonObject>(1)
        conn.channels[id] = channel

        try {
            conn.session.send(command.toString())
            return withTimeout(5.seconds) {
                channel.receive()
            }
        } catch (e: Exception) {
            // A send failure or response timeout means this socket is suspect; drop it (if it is
            // still the current one) so the retry reconnects instead of reusing it.
            if (connection === conn) dropConnection()
            throw e
        } finally {
            conn.channels.remove(id)
        }
    }

    /** Streams live `state_changed` events from a single websocket subscription. The flow completes
     *  (rather than erroring) when the socket drops, so the caller can simply re-collect to reconnect. */
    fun subscribeStateChanges(): Flow<HAStateChange> = flow {
        val conn = ensureConnected()
        val id = messageId.getAndIncrement()
        val channel = Channel<JsonObject>(Channel.UNLIMITED)
        conn.channels[id] = channel
        try {
            conn.session.send(buildJsonObject {
                put("id", id)
                put("type", "subscribe_events")
                put("event_type", "state_changed")
            }.toString())

            for (message in channel) {
                if (message["type"]?.jsonPrimitive?.contentOrNull != "event") continue
                val data = message["event"]?.jsonObject?.get("data")?.jsonObject ?: continue
                val entityId = data["entity_id"]?.jsonPrimitive?.contentOrNull ?: continue
                val newState = data["new_state"]
                    ?.takeUnless { it is JsonNull }
                    ?.let { runCatching { json.decodeFromJsonElement(HAEntity.serializer(), it) }.getOrNull() }
                emit(HAStateChange(entityId, newState))
            }
        } finally {
            conn.channels.remove(id)
            runCatching {
                conn.session.send(buildJsonObject {
                    put("id", messageId.getAndIncrement())
                    put("type", "unsubscribe_events")
                    put("subscription", id)
                }.toString())
            }
        }
    }

    /** Long-term statistics (recorder): pre-aggregated per-hour/per-day mean and change values —
     *  the same source HA's own energy dashboard uses. Tiny payloads compared to raw history,
     *  which for a per-second P1 meter can run into millions of rows over a month. */
    suspend fun getStatistics(
        statisticIds: List<String>,
        startMillis: Long,
        period: String,   // "hour" | "day" | "month"
        endMillis: Long? = null
    ): Map<String, List<HAStatPoint>> {
        return withWebSocket {
            val args = buildMap {
                put("start_time", JsonPrimitive(Instant.ofEpochMilli(startMillis).toString()))
                endMillis?.let { put("end_time", JsonPrimitive(Instant.ofEpochMilli(it).toString())) }
                put("period", JsonPrimitive(period))
                put("statistic_ids", JsonArray(statisticIds.map { JsonPrimitive(it) }))
                put("types", JsonArray(listOf("mean", "change").map { JsonPrimitive(it) }))
            }
            val response = sendCommand("recorder/statistics_during_period", args)
            val result = response["result"] as? JsonObject ?: return@withWebSocket emptyMap()
            result.mapValues { (_, points) ->
                (points as? JsonArray)?.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    // "start" is epoch millis on current HA; older cores sent ISO strings.
                    val start = obj["start"]?.jsonPrimitive?.longOrNull
                        ?: obj["start"]?.jsonPrimitive?.contentOrNull?.let { parseHaInstant(it)?.toEpochMilli() }
                        ?: return@mapNotNull null
                    HAStatPoint(
                        startMs = start,
                        mean = obj["mean"]?.jsonPrimitive?.doubleOrNull?.toFloat(),
                        change = obj["change"]?.jsonPrimitive?.doubleOrNull?.toFloat()
                    )
                } ?: emptyList()
            }
        }
    }

    /** Streams push notifications from HA's mobile_app websocket push channel — the official app's
     *  "local push" transport. HA delivers anything sent to `notify.mobile_app_<device>` here while
     *  the subscription is up, and we confirm each delivery so HA knows it arrived. The flow
     *  completes when the socket drops; re-collect to reconnect. */
    fun subscribePushNotifications(webhookId: String): Flow<JsonObject> = flow {
        val conn = ensureConnected()
        val id = messageId.getAndIncrement()
        val channel = Channel<JsonObject>(Channel.UNLIMITED)
        conn.channels[id] = channel
        try {
            conn.session.send(buildJsonObject {
                put("id", id)
                put("type", "mobile_app/push_notification_channel")
                put("webhook_id", webhookId)
                put("support_confirm", true)
            }.toString())

            for (message in channel) {
                if (message["type"]?.jsonPrimitive?.contentOrNull != "event") continue
                val event = message["event"]?.jsonObject ?: continue
                event["hass_confirm_id"]?.jsonPrimitive?.contentOrNull?.let { confirmId ->
                    runCatching {
                        conn.session.send(buildJsonObject {
                            put("id", messageId.getAndIncrement())
                            put("type", "mobile_app/push_notification_confirm")
                            put("webhook_id", webhookId)
                            put("confirm_id", confirmId)
                        }.toString())
                    }
                }
                emit(event)
            }
        } finally {
            conn.channels.remove(id)
        }
    }

    /** Closes the live websocket (used when sync stops). Safe to call from a non-suspend context. */
    fun closeSession() {
        dropConnection()
    }

    /** POSTs a payload to a mobile_app webhook URL (unauthenticated). Returns (httpStatus, bodyText);
     *  does not throw on non-2xx so callers can inspect the per-sensor response body. */
    suspend fun postWebhook(webhookUrl: String, payload: JsonObject): Pair<Int, String> {
        val response: HttpResponse = client.post(webhookUrl) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(payload.toString())
        }
        return response.status.value to response.bodyAsText()
    }

    /** Registers this device as a mobile_app integration so HA creates a device_tracker + sensors. */
    suspend fun registerMobileApp(body: JsonObject): MobileAppRegistration {
        return withAuthHandling {
            val response: HttpResponse = client.post("$baseUrl/api/mobile_app/registrations") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body.toString())
            }
            if (!response.status.isSuccess()) {
                throw Exception("mobile_app registration HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
            }
            json.decodeFromString(MobileAppRegistration.serializer(), response.bodyAsText())
        }
    }

    suspend fun toggleEntity(entityId: String) {
        val domain = entityId.split(".").first()
        withAuthHandling {
            val response: HttpResponse = client.post("$baseUrl/api/services/$domain/toggle") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(HAServiceCall(entity_id = entityId))
            }
            if (!response.status.isSuccess()) {
                throw Exception("Service call failed: ${response.status.value} ${response.bodyAsText().take(200)}")
            }
        }
    }

    suspend fun callService(domain: String, service: String, serviceCall: HAServiceCall) {
        withAuthHandling {
            val response: HttpResponse = client.post("$baseUrl/api/services/$domain/$service") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(serviceCall)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Service call failed: ${response.status.value} ${response.bodyAsText().take(200)}")
            }
        }
    }

    /** Calls an arbitrary service with a free-form JSON payload (target + service data), for
     *  user-configured custom actions where the fixed [HAServiceCall] fields aren't enough. */
    suspend fun callServiceRaw(domain: String, service: String, payload: JsonObject) {
        withAuthHandling {
            val response: HttpResponse = client.post("$baseUrl/api/services/$domain/$service") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(payload)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Service call failed: ${response.status.value} ${response.bodyAsText().take(200)}")
            }
        }
    }

    private suspend fun <T> withAuthHandling(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            if (e is ResponseException && e.response.status == HttpStatusCode.Unauthorized) {
                throw Exception("AUTH_EXPIRED")
            }
            throw e
        }
    }

    private suspend fun getUserNamesById(): Map<String, String> {
        val userNames = mutableMapOf<String, String>()
        val currentUser = runCatching { getCurrentUser() }.getOrNull()
        currentUser?.let { user ->
            userNames[user.id] = user.displayName
        }
        if (currentUser?.is_admin != false) {
            runCatching { getUsers() }.getOrDefault(emptyList()).forEach { user ->
                userNames[user.id] = user.displayName
            }
        }
        return userNames
    }

    private fun HAHistoryEntry.withActor(
        entityId: String,
        logbookEntries: List<HALogbookEntry>,
        userNamesById: Map<String, String>
    ): HAHistoryEntry {
        val historyTime = parseHaInstant(last_changed)
        val matchingLogbook = historyTime?.let { target ->
            logbookEntries
                .filter { it.entity_id == null || it.entity_id == entityId }
                .mapNotNull { logbook ->
                    val logTime = parseHaInstant(logbook.time) ?: return@mapNotNull null
                    logbook to abs(Duration.between(target, logTime).toMillis())
                }
                .filter { pair -> pair.second <= 5000 }
                .minByOrNull { pair -> pair.second }
                ?.first
        }

        val userId = context_user_id
            ?: context?.user_id
            ?: attributes?.get("context_user_id")?.jsonPrimitive?.contentOrNull
            ?: matchingLogbook?.context_user_id

        val actorName = userId?.let { userNamesById[it] ?: "User ${it.take(8)}" }
            ?: matchingLogbook?.sourceName(entityId)

        return copy(actorId = userId, actorName = actorName)
    }

    private fun HALogbookEntry.sourceName(entityId: String): String? {
        val entityDomain = entityId.substringBefore(".")
        val sourceDomain = domain?.takeIf { it.isNotBlank() && it != entityDomain } ?: return null
        return name?.takeIf { it.isNotBlank() }
            ?: sourceDomain.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun parseHaInstant(value: String): Instant? {
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { Instant.parse(value) }
            .getOrNull()
    }

    private val HAUser.displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "User ${id.take(8)}"

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
        }

        suspend fun getAccessToken(serverUrl: String, code: String): HATokenResponse {
            return client.submitForm(
                url = "${serverUrl.removeSuffix("/")}/auth/token",
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("client_id", "https://home-assistant.io/android")
                    append("redirect_uri", "homeassistant://auth-callback")
                }
            ).body()
        }

        suspend fun refreshAccessToken(serverUrl: String, refreshToken: String): HATokenResponse {
            val response: HttpResponse = client.submitForm(
                url = "${serverUrl.removeSuffix("/")}/auth/token",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                    append("client_id", "https://home-assistant.io/android")
                }
            )
            if (response.status.isSuccess()) return response.body()
            val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
            // The server only returns 400 invalid_grant when the refresh token is truly dead; treat
            // everything else (5xx, timeouts surfaced as other statuses) as transient and retryable.
            val invalidGrant = response.status == HttpStatusCode.BadRequest &&
                bodyText.contains("invalid_grant", ignoreCase = true)
            throw TokenRefreshException(invalidGrant, "Token refresh failed: ${response.status.value} ${bodyText.take(200)}")
        }
    }
}

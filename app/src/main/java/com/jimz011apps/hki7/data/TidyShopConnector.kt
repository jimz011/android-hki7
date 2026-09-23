package com.jimz011apps.hki7.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────────────────────────────

/** What this device may do with a shared list. Mirrors the connector's own three levels. */
const val TIDYSHOP_PERMISSION_VIEW = "view"
const val TIDYSHOP_PERMISSION_CHECK = "check"
const val TIDYSHOP_PERMISSION_EDIT = "edit"

/**
 * One item on a TidyShop list.
 *
 * [quantity], [size] and [unit] are TidyShop's own optional measures — "2 x", "500 g". They are
 * carried through untouched even when HKI 7 has nothing to show for them, because the connector
 * replaces a list wholesale and a dropped field would be a silent edit to somebody else's data.
 */
data class TidyShopItem(
    val id: String,
    val name: String,
    val checked: Boolean = false,
    val quantity: Int = 0,
    val size: Double = 0.0,
    val unit: String = "",
    val updatedAt: Long = 0L,
) {
    /** "2 x 500 g", or an empty string when the item carries no measures. */
    val measureLabel: String
        get() = buildString {
            if (quantity > 1) append("${quantity}x")
            if (size > 0.0) {
                if (isNotEmpty()) append(' ')
                val rounded = if (size % 1.0 == 0.0) size.toLong().toString() else size.toString()
                append(rounded)
                if (unit.isNotBlank()) append(unit)
            } else if (unit.isNotBlank() && quantity > 1) {
                append(' ').append(unit)
            }
        }
}

/**
 * One shared list as the connector holds it.
 *
 * Every field the connector overwrites on `PUT /v1/lists/{id}` is kept here so an edit can send
 * the list back whole. Dropping [quickAddRows] and friends would reset settings this app does not
 * even display, and would additionally fail the connector's check-only guard for a member who may
 * tick items but not edit them.
 */
data class TidyShopList(
    val id: String,
    val title: String,
    val icon: String = "🛒",
    val householdId: String = "",
    val ownerId: String = "",
    val items: List<TidyShopItem> = emptyList(),
    val permissions: Map<String, String> = emptyMap(),
    val storeCountryCode: String = "",
    val storeCountryName: String = "",
    val quickAddRows: Int = 2,
    val maxQuickSuggestions: Int = 36,
    val suggestionIncludesMeasures: Boolean = false,
    val updatedAt: Long = 0L,
) {
    val remaining: Int get() = items.count { !it.checked }

    /** What [userId] may do here: the owner always edits, everyone else holds a granted level. */
    fun permissionFor(userId: String?): String {
        if (userId.isNullOrBlank()) return TIDYSHOP_PERMISSION_VIEW
        if (ownerId == userId) return TIDYSHOP_PERMISSION_EDIT
        return permissions[userId] ?: TIDYSHOP_PERMISSION_VIEW
    }

    fun canCheck(userId: String?): Boolean =
        permissionFor(userId) in setOf(TIDYSHOP_PERMISSION_CHECK, TIDYSHOP_PERMISSION_EDIT)

    fun canEdit(userId: String?): Boolean = permissionFor(userId) == TIDYSHOP_PERMISSION_EDIT
}

/** A member of the household, as the connector's hand-written household view reports them. */
data class TidyShopMember(
    val userId: String,
    val displayName: String,
    val role: String,
)

data class TidyShopHousehold(
    val id: String,
    val name: String,
    val members: List<TidyShopMember> = emptyList(),
)

/** What a successful `POST /v1/devices/enrol` hands back. */
data class TidyShopEnrolment(
    val deviceId: String,
    val userId: String,
    val household: TidyShopHousehold?,
    /** Shown once and never stored — the only way back in for a member with no second device. */
    val recoveryCode: String?,
)

/** Raised when the connector says this device is no longer part of the family. */
class TidyShopRevokedException(message: String) : IllegalStateException(message)

private fun connectorFailure(status: Int, payload: String): Throwable {
    val detail = runCatching { JSONObject(payload).optString("error") }.getOrNull().orEmpty()
    val revoked = (status == 401 && (
        detail == "this device is not enrolled" ||
            detail == "this device belongs to an identity that no longer exists"
        )) || (status == 403 && detail == "user is not enrolled on this family server")
    return if (revoked) {
        TidyShopRevokedException(detail.ifBlank { "This device was removed from its family server." })
    } else {
        IllegalStateException(detail.ifBlank { "TidyShop connector returned HTTP $status" })
    }
}

/** Trims a user-typed connector address into an origin this client can append paths to. */
fun normalizeTidyShopUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
    else "https://$trimmed"
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Thin TidyShop-Connector client built on [HttpURLConnection], the same way [CloudBackupWorker]
 * talks to Google Drive — the Ktor client in this app is configured around Home Assistant's
 * websocket and auth, and none of that applies here.
 *
 * The connector serialises its Go structs with capitalised field names, except for the two that
 * carry explicit json tags (`permissions` and `itemHistory`) and the hand-written views for
 * households, invites and devices. The mapping below is deliberately literal because of that.
 */
class TidyShopConnector(
    private val baseUrl: () -> String,
    /** Identifies this device on every request so the SSE stream can skip the echo of our own edit. */
    private val clientId: () -> String,
    /** A freshly signed device assertion, or null when this device is not enrolled. */
    private val accessToken: suspend () -> String?,
) {
    /** The live event stream, kept so [closeEventStream] can unblock its read loop. */
    @Volatile
    private var eventConnection: HttpURLConnection? = null

    suspend fun fetchLists(): Result<List<TidyShopList>> = request("GET", "/v1/lists") { body ->
        val array = JSONArray(body)
        buildList {
            for (index in 0 until array.length()) add(array.getJSONObject(index).toTidyShopList())
        }
    }

    suspend fun fetchHouseholds(): Result<List<TidyShopHousehold>> =
        request("GET", "/v1/households") { body ->
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) add(array.getJSONObject(index).toHousehold())
            }
        }

    /** Replaces one list. The connector rejects this for a view-only member, and for a member with
     *  check permission whose payload changes anything but tick states. */
    suspend fun updateList(list: TidyShopList): Result<TidyShopList> =
        request("PUT", "/v1/lists/${list.id}", list.toJson().toString()) { body ->
            JSONObject(body).toTidyShopList()
        }

    /** Records an added item against the list's shared suggestion history, so a family member's
     *  next "milk" is suggested in TidyShop too. Best-effort: it needs edit permission, and a
     *  refusal here must not fail the add that already happened. */
    suspend fun recordSuggestion(listId: String, name: String, eventId: String): Result<Unit> =
        request(
            "POST",
            "/v1/lists/$listId/suggestions",
            JSONObject().put("Name", name).put("EventID", eventId).toString(),
        ) { }

    /**
     * Enrols this device. Unauthenticated by definition — it is the call that establishes who we
     * are — so it does not go through [request].
     */
    suspend fun enrol(
        code: String,
        pairingKey: String,
        deviceName: String,
        displayName: String,
    ): Result<TidyShopEnrolment> = withContext(Dispatchers.IO) {
        runCatching {
            TidyShopIdentity.ensureKey().getOrThrow()
            val jwk = TidyShopIdentity.publicJwk()
                ?: throw IllegalStateException("This device could not create a security key.")
            val payload = JSONObject()
                .put("PublicKey", jwk)
                .put("DeviceName", deviceName)
                .put("DisplayName", displayName)
            if (code.isNotBlank()) payload.put("Code", code.trim())
            if (pairingKey.isNotBlank()) payload.put("PairingKey", pairingKey.trim())

            val connection = open("POST", "/v1/devices/enrol", authorize = false, body = true)
            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                val body = readBody(connection)
                val json = JSONObject(body)
                TidyShopEnrolment(
                    deviceId = json.optString("deviceId"),
                    userId = json.optString("userId"),
                    household = json.optJSONObject("household")?.toHousehold(),
                    recoveryCode = json.optString("recoveryCode").takeIf { it.isNotBlank() },
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Holds `/v1/events` open and reports frames until the coroutine is cancelled or the link
     * drops, then returns the reason — null for a clean close. Never returns while healthy, so
     * the caller owns the reconnect policy.
     */
    suspend fun streamEvents(
        onOpen: () -> Unit,
        onEvent: (event: String, data: JSONObject) -> Unit,
    ): Throwable? = withContext(Dispatchers.IO) {
        val connection: HttpURLConnection
        try {
            connection = open("GET", "/v1/events", authorize = true, body = false).apply {
                connectTimeout = 15_000
                // Longer than the connector's 25s heartbeat, so a genuinely dead link is still
                // detected but a quiet stream is not torn down.
                readTimeout = 70_000
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Cache-Control", "no-cache")
                // Compression would make the platform buffer whole frames.
                setRequestProperty("Accept-Encoding", "identity")
            }
            eventConnection = connection
            val code = connection.responseCode
            if (code !in 200..299) {
                val payload = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                return@withContext connectorFailure(code, payload)
            }
            onOpen()
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                var event = "message"
                val data = StringBuilder()
                while (currentCoroutineContext().isActive) {
                    val line = reader.readLine() ?: break
                    when {
                        // A blank line terminates a frame.
                        line.isEmpty() -> {
                            if (data.isNotEmpty()) {
                                runCatching { JSONObject(data.toString()) }
                                    .onSuccess { onEvent(event, it) }
                            }
                            event = "message"
                            data.setLength(0)
                        }
                        // Comment, used for the heartbeat.
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(line.removePrefix("data:").trim())
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            return@withContext error
        } finally {
            val open = eventConnection
            eventConnection = null
            runCatching { open?.disconnect() }
        }
        null
    }

    /** Drops the event stream, unblocking the read in [streamEvents]. */
    fun closeEventStream() {
        val connection = eventConnection
        eventConnection = null
        runCatching { connection?.disconnect() }
    }

    private suspend fun open(
        method: String,
        path: String,
        authorize: Boolean,
        body: Boolean,
    ): HttpURLConnection {
        val root = baseUrl()
        if (root.isBlank()) throw IllegalStateException("No TidyShop server is configured.")
        val connection = (URL("$root$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Client-Id", clientId())
            if (body) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (authorize) {
            val token = accessToken()
                ?: throw IllegalStateException("This device is not connected to a TidyShop family.")
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        return connection
    }

    private fun readBody(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val payload = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw connectorFailure(code, payload)
        return payload
    }

    private suspend fun <T> request(
        method: String,
        path: String,
        body: String? = null,
        parse: (String) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(method, path, authorize = true, body = body != null)
            try {
                if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
                parse(readBody(connection))
            } finally {
                connection.disconnect()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JSON mapping
// ─────────────────────────────────────────────────────────────────────────────

internal fun JSONObject.toTidyShopList(): TidyShopList {
    val itemArray = optJSONArray("Items") ?: JSONArray()
    val items = buildList {
        for (index in 0 until itemArray.length()) {
            val item = itemArray.getJSONObject(index)
            add(
                TidyShopItem(
                    id = item.optString("ID"),
                    name = item.optString("Name"),
                    checked = item.optBoolean("Checked"),
                    quantity = item.optInt("Quantity"),
                    size = item.optDouble("Size", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                    unit = item.optString("Unit"),
                    updatedAt = item.optLong("UpdatedAt"),
                )
            )
        }
    }
    val permissionObject = optJSONObject("permissions")
    val permissions = buildMap {
        permissionObject?.keys()?.forEach { userId -> put(userId, permissionObject.optString(userId)) }
    }
    return TidyShopList(
        id = optString("ID"),
        title = optString("Title"),
        icon = optString("Icon").ifBlank { "🛒" },
        householdId = optString("HouseholdID"),
        ownerId = optString("OwnerID"),
        items = items,
        permissions = permissions,
        storeCountryCode = optString("StoreCountryCode"),
        storeCountryName = optString("StoreCountryName"),
        quickAddRows = optInt("QuickAddRows").takeIf { it in 1..7 } ?: 2,
        maxQuickSuggestions = optInt("MaxQuickSuggestions").takeIf { it in 1..120 } ?: 36,
        suggestionIncludesMeasures = optBoolean("SuggestionIncludesMeasures", false),
        updatedAt = optLong("UpdatedAt"),
    )
}

internal fun TidyShopList.toJson(): JSONObject = JSONObject().apply {
    put("ID", id)
    put("HouseholdID", householdId)
    put("OwnerID", ownerId)
    put("Title", title)
    put("Icon", icon)
    put("StoreCountryCode", storeCountryCode)
    put("StoreCountryName", storeCountryName)
    put("QuickAddRows", quickAddRows)
    put("MaxQuickSuggestions", maxQuickSuggestions)
    put("SuggestionIncludesMeasures", suggestionIncludesMeasures)
    put("UpdatedAt", updatedAt)
    put(
        "Items",
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject()
                        .put("ID", item.id)
                        .put("Name", item.name)
                        .put("Checked", item.checked)
                        .put("Quantity", item.quantity)
                        .put("Size", item.size)
                        .put("Unit", item.unit)
                        .put("UpdatedAt", item.updatedAt)
                )
            }
        }
    )
}

private fun JSONObject.toHousehold(): TidyShopHousehold {
    val memberArray = optJSONArray("members") ?: JSONArray()
    val members = buildList {
        for (index in 0 until memberArray.length()) {
            val member = memberArray.getJSONObject(index)
            add(
                TidyShopMember(
                    userId = member.optString("userId"),
                    displayName = member.optString("displayName"),
                    role = member.optString("role"),
                )
            )
        }
    }
    return TidyShopHousehold(id = optString("id"), name = optString("name"), members = members)
}

/** Reads the single list carried by an SSE `list.updated` frame. */
internal fun JSONObject.tidyShopListFromFrame(): TidyShopList? =
    optJSONObject("list")?.toTidyShopList()

/** Reads the full set carried by the `sync` frame the connector sends on connect. */
internal fun JSONObject.tidyShopListsFromSnapshot(): List<TidyShopList> {
    val array = optJSONArray("lists") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            runCatching { array.getJSONObject(index).toTidyShopList() }.getOrNull()?.let { add(it) }
        }
    }
}

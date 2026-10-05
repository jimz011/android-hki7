package com.jimz011apps.hki7.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * HA-local backup destination: stores the same UI backup blob as the Google Drive
 * path ([CloudBackupStorage]), but on the user's own Home Assistant instance via the
 * optional `hki7` companion component. This is an addition to Google Drive, never a
 * replacement — both can be enabled at once.
 *
 * Every call builds a short-lived [HomeAssistantClient] from the active instance's
 * stored credentials (mirroring [GeofenceManager]) and closes it afterwards, so this
 * works from a background WorkManager job with no live view-model client.
 */
/**
 * Builds short-lived [HomeAssistantClient]s from the active instance's stored
 * credentials (mirroring [GeofenceManager]) so the `hki7` companion features work
 * from background jobs and settings screens without a live view-model client.
 */
internal object Hki7Endpoint {
    private data class Endpoint(val url: String, val token: String)

    /**
     * @param rejectedToken an access token Home Assistant just answered 401 to. Its stored expiry
     *  can still look healthy (the main app's auth probe exists for the same reason), so the
     *  expiry check alone would hand the same dead token back; a rejected token forces a refresh.
     */
    private suspend fun endpoint(
        context: Context,
        instanceId: String,
        rejectedToken: String? = null,
    ): Endpoint? {
        val rootPrefs = PreferencesManager(context)
        val prefs = rootPrefs.forInstance(instanceId)
        // Prefer the external URL (reachable off the home network, e.g. for the daily job);
        // fall back to the internal URL for local-only setups.
        val url = prefs.serverUrl.first()?.takeIf { it.isNotBlank() }
            ?: prefs.internalUrl.first()?.takeIf { it.isNotBlank() }
            ?: return null
        var token = prefs.accessToken.first()?.takeIf { it.isNotBlank() }
        val refreshToken = prefs.refreshToken.first()?.takeIf { it.isNotBlank() }
        val expiry = prefs.accessTokenExpiry.first()
        val now = System.currentTimeMillis()

        val decision = if (rejectedToken != null) {
            if (refreshToken == null) EndpointAuthDecision.LOGIN_REQUIRED else EndpointAuthDecision.REFRESH
        } else {
            endpointAuthDecision(token, refreshToken, expiry, now)
        }
        when (decision) {
            EndpointAuthDecision.LOGIN_REQUIRED -> {
                if (isKnownExpiredWithoutRefreshToken(token, refreshToken, expiry, now)) {
                    prefs.clearAuth()
                }
                return null
            }
            EndpointAuthDecision.REFRESH -> {
                // With a rejected token the coordinator reuses a token another caller already
                // refreshed to, rather than spending the refresh token a second time.
                when (val refreshed = HomeAssistantAuthRefreshCoordinator.refresh(url, prefs, rejectedToken ?: token)) {
                    is CoordinatedTokenRefreshResult.Success -> token = refreshed.accessToken
                    is CoordinatedTokenRefreshResult.LoginRequired -> return null
                    is CoordinatedTokenRefreshResult.AccessForbidden -> throw refreshed.cause
                        ?: HomeAssistantForbiddenException()
                    is CoordinatedTokenRefreshResult.RetryableFailure -> throw refreshed.cause
                }
            }
            EndpointAuthDecision.USE_ACCESS_TOKEN -> Unit
        }
        return Endpoint(url, token ?: return null)
    }

    /** Runs [block] with a fresh client, releasing it afterwards. Null if no credentials.
     *
     *  Disposes rather than only closing the session: the client is built per call and no caller
     *  can reach it again afterwards, so leaving its HTTP connection pool and dispatcher threads
     *  alive leaks them once per call. Barely visible for the daily backup job, but the Android
     *  Auto surface comes through here on every refresh and every tap. */
    suspend fun <T> withClient(
        context: Context,
        block: suspend (HomeAssistantClient) -> T,
    ): T? {
        val prefs = PreferencesManager(context)
        prefs.ensureHomeAssistantInstanceStore()
        val instanceId = prefs.activeHomeAssistantInstanceId.first() ?: return null
        return withClient(context, instanceId, block)
    }

    /**
     * Runs [block] once, and once more with a refreshed token if Home Assistant rejected the stored
     * one. A 401 means the request was not carried out, so repeating a toggle here cannot run it
     * twice. Without the retry, a surface with no view model of its own (Android Auto, above all)
     * failed every call until the phone app was opened and refreshed the session for it.
     */
    suspend fun <T> withClient(
        context: Context,
        instanceId: String,
        block: suspend (HomeAssistantClient) -> T,
    ): T? {
        val first = endpoint(context, instanceId) ?: return null
        try {
            return useClient(first, block)
        } catch (error: Exception) {
            if (!isAuthExpired(error)) throw error
        }
        val retry = endpoint(context, instanceId, rejectedToken = first.token) ?: return null
        return useClient(retry, block)
    }

    private suspend fun <T> useClient(
        endpoint: Endpoint,
        block: suspend (HomeAssistantClient) -> T,
    ): T {
        val client = clientFor(endpoint)
        return try {
            block(client)
        } finally {
            client.dispose()
        }
    }

    private fun isAuthExpired(error: Throwable): Boolean = error.message == "AUTH_EXPIRED"

    /**
     * A client the caller owns and must [HomeAssistantClient.dispose] itself.
     *
     * For a surface that keeps one connection alive across many calls rather than making a single
     * request — the Android Auto screen holds a state-change subscription open for as long as the
     * car session lasts, which is the whole point of it not needing a refresh button.
     */
    suspend fun createClient(context: Context): HomeAssistantClient? {
        val prefs = PreferencesManager(context)
        prefs.ensureHomeAssistantInstanceStore()
        val instanceId = prefs.activeHomeAssistantInstanceId.first() ?: return null
        return createClient(context, instanceId)
    }

    suspend fun createClient(context: Context, instanceId: String): HomeAssistantClient? =
        endpoint(context, instanceId)?.let(::clientFor)

    /**
     * As [createClient], but proves the token first and refreshes it if Home Assistant rejects it.
     * For a long-lived client — the car screen's subscription — whose first failure would
     * otherwise be swallowed, leaving the grid stale until the phone app happened to be opened.
     */
    suspend fun createVerifiedClient(context: Context, instanceId: String): HomeAssistantClient? {
        val first = endpoint(context, instanceId) ?: return null
        val client = clientFor(first)
        try {
            client.checkConnection()
            return client
        } catch (error: Exception) {
            client.dispose()
            if (!isAuthExpired(error)) throw error
        }
        val retry = endpoint(context, instanceId, rejectedToken = first.token) ?: return null
        return clientFor(retry)
    }

    private fun clientFor(endpoint: Endpoint): HomeAssistantClient {
        // Mirrors MainViewModel's own client construction: on the demo home there is no server
        // behind the saved URL, so a real client here would send requests at demo-home.hki7.invalid.
        // The Android Auto surface reaches Home Assistant only through here, and demo mode is how
        // someone without a Home Assistant server sees the car screen work at all.
        return if (isDemoServerUrl(endpoint.url)) {
            DemoHomeAssistantClient()
        } else {
            HomeAssistantClient(endpoint.url, endpoint.token)
        }
    }
}

internal enum class EndpointAuthDecision { USE_ACCESS_TOKEN, REFRESH, LOGIN_REQUIRED }

internal fun endpointAuthDecision(
    accessToken: String?,
    refreshToken: String?,
    expiryMillis: Long?,
    nowMillis: Long,
): EndpointAuthDecision = when {
    accessToken.isNullOrBlank() && refreshToken.isNullOrBlank() -> EndpointAuthDecision.LOGIN_REQUIRED
    isKnownExpiredWithoutRefreshToken(accessToken, refreshToken, expiryMillis, nowMillis) ->
        EndpointAuthDecision.LOGIN_REQUIRED
    accessToken.isNullOrBlank() || shouldRefreshBeforeAuthenticatedWork(
        refreshToken, expiryMillis, nowMillis
    ) -> EndpointAuthDecision.REFRESH
    else -> EndpointAuthDecision.USE_ACCESS_TOKEN
}

object HaBackupStorage {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> withClient(
        context: Context,
        block: suspend (HomeAssistantClient) -> T,
    ): T? = try {
        Hki7Endpoint.withClient(context, block)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun <T> withClient(
        context: Context,
        instanceId: String,
        block: suspend (HomeAssistantClient) -> T,
    ): T? = try {
        Hki7Endpoint.withClient(context, instanceId, block)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    /** True if the companion component answered `hki7/whoami` on this instance. */
    suspend fun isAvailable(context: Context): Boolean =
        withClient(context) { it.hki7WhoAmI() != null } ?: false

    /** Writes the current UI backup to Home Assistant. Returns true on success. */
    suspend fun write(context: Context): Boolean {
        val prefs = PreferencesManager(context)
        prefs.ensureHomeAssistantInstanceStore()
        val instanceId = prefs.activeHomeAssistantInstanceId.first() ?: return false
        // Capture the payload before token refresh/network work. If the user switches while it is
        // being captured, abort rather than uploading the newly selected home's UI to the old home.
        val raw = prefs.exportUiBackup()
        if (prefs.activeHomeAssistantInstanceId.first() != instanceId) return false
        val payload = json.parseToJsonElement(raw) as? JsonObject ?: return false
        return withClient(context, instanceId) { client ->
            client.hki7PutBackup(payload, label = hki7BackupName())
        } ?: false
    }

    /** Lists the current user's HA-local backups, newest first. */
    suspend fun list(context: Context): List<Hki7BackupMeta> =
        withClient(context) { it.hki7ListBackups() } ?: emptyList()

    /** Fetches one backup payload as raw JSON, ready for [PreferencesManager.restoreUiBackup]. */
    suspend fun read(context: Context, backupId: String): String? =
        withClient(context) { it.hki7GetBackup(backupId) }
}

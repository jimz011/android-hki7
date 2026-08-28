package com.jimz011apps.hki7.data

import android.content.Context
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

    private suspend fun endpoint(context: Context): Endpoint? {
        val prefs = PreferencesManager(context)
        val token = prefs.accessToken.first()?.takeIf { it.isNotBlank() } ?: return null
        // Prefer the external URL (reachable off the home network, e.g. for the daily job);
        // fall back to the internal URL for local-only setups.
        val url = prefs.serverUrl.first()?.takeIf { it.isNotBlank() }
            ?: prefs.internalUrl.first()?.takeIf { it.isNotBlank() }
            ?: return null
        return Endpoint(url, token)
    }

    /** Runs [block] with a fresh client, releasing it afterwards. Null if no credentials.
     *
     *  Disposes rather than only closing the session: the client is built per call and no caller
     *  can reach it again afterwards, so leaving its HTTP connection pool and dispatcher threads
     *  alive leaks them once per call. Barely visible for the daily backup job, but the Android
     *  Auto surface comes through here on every refresh and every tap. */
    suspend fun <T> withClient(context: Context, block: suspend (HomeAssistantClient) -> T): T? {
        val client = createClient(context) ?: return null
        return try {
            block(client)
        } finally {
            client.dispose()
        }
    }

    /**
     * A client the caller owns and must [HomeAssistantClient.dispose] itself.
     *
     * For a surface that keeps one connection alive across many calls rather than making a single
     * request — the Android Auto screen holds a state-change subscription open for as long as the
     * car session lasts, which is the whole point of it not needing a refresh button.
     */
    suspend fun createClient(context: Context): HomeAssistantClient? {
        val endpoint = endpoint(context) ?: return null
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

object HaBackupStorage {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> withClient(context: Context, block: suspend (HomeAssistantClient) -> T): T? =
        Hki7Endpoint.withClient(context, block)

    /** True if the companion component answered `hki7/whoami` on this instance. */
    suspend fun isAvailable(context: Context): Boolean =
        withClient(context) { it.hki7WhoAmI() != null } ?: false

    /** Writes the current UI backup to Home Assistant. Returns true on success. */
    suspend fun write(context: Context): Boolean = withClient(context) { client ->
        val raw = PreferencesManager(context).exportUiBackup()
        val payload = json.parseToJsonElement(raw) as? JsonObject ?: return@withClient false
        client.hki7PutBackup(payload, label = hki7BackupName())
    } ?: false

    /** Lists the current user's HA-local backups, newest first. */
    suspend fun list(context: Context): List<Hki7BackupMeta> =
        withClient(context) { it.hki7ListBackups() } ?: emptyList()

    /** Fetches one backup payload as raw JSON, ready for [PreferencesManager.restoreUiBackup]. */
    suspend fun read(context: Context, backupId: String): String? =
        withClient(context) { it.hki7GetBackup(backupId) }
}

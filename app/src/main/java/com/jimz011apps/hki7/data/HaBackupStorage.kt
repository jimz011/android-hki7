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

    /** Runs [block] with a fresh client, closing it afterwards. Null if no credentials. */
    suspend fun <T> withClient(context: Context, block: suspend (HomeAssistantClient) -> T): T? {
        val endpoint = endpoint(context) ?: return null
        val client = HomeAssistantClient(endpoint.url, endpoint.token)
        return try {
            block(client)
        } finally {
            client.closeSession()
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
        client.hki7PutBackup(payload)
    } ?: false

    /** Lists the current user's HA-local backups, newest first. */
    suspend fun list(context: Context): List<Hki7BackupMeta> =
        withClient(context) { it.hki7ListBackups() } ?: emptyList()

    /** Fetches one backup payload as raw JSON, ready for [PreferencesManager.restoreUiBackup]. */
    suspend fun read(context: Context, backupId: String): String? =
        withClient(context) { it.hki7GetBackup(backupId) }
}

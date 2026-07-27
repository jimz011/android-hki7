package com.jimz011apps.hki7.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Family dashboard sharing via the `hki7` companion component. The admin publishes
 * a dashboard (a serialised [HKIDashboard]) with a list of HA user ids to share it
 * with; recipients pull the dashboards shared with them and import them locally.
 *
 * Sync is pull-on-open: [listSharedForMe] is fetched when the sharing screen opens,
 * and importing is an explicit action — no background polling.
 */
object HaDashboardSharing {
    private val json = Json { ignoreUnknownKeys = true }

    /** Sentinel accepted by the component's `shared_with` to mean "everyone". */
    const val EVERYONE = "*"

    /** The current HA user's identity, or null if the companion component isn't installed. */
    suspend fun whoami(context: Context): Hki7Identity? =
        Hki7Endpoint.withClient(context) { it.hki7WhoAmI() }

    /** HA users the admin can share with. Empty if the caller isn't an admin or the component is absent. */
    suspend fun listUsers(context: Context): List<Hki7User> =
        Hki7Endpoint.withClient(context) { it.hki7ListUsers() } ?: emptyList()

    /** Publishes the given local dashboard by id. Returns the shared metadata, or null on failure. */
    suspend fun publish(
        context: Context,
        prefs: PreferencesManager,
        localDashboardId: String,
        name: String,
        sharedWith: List<String>,
        existingSharedId: String? = null,
    ): Hki7SharedDashboardMeta? {
        val raw = prefs.exportDashboard(localDashboardId) ?: return null
        val payload = json.parseToJsonElement(raw) as? JsonObject ?: return null
        return Hki7Endpoint.withClient(context) { client ->
            client.hki7PublishDashboard(name, payload, sharedWith, existingSharedId)
        }
    }

    /** Removes a shared dashboard (admin only). */
    suspend fun unpublish(context: Context, sharedId: String): Boolean =
        Hki7Endpoint.withClient(context) { it.hki7UnpublishDashboard(sharedId) } ?: false

    /** Dashboards visible to the current user (own + shared-with-them + everyone). */
    suspend fun listSharedForMe(context: Context): List<Hki7SharedDashboardMeta> =
        Hki7Endpoint.withClient(context) { it.hki7ListSharedDashboards() } ?: emptyList()

    /** Imports one shared dashboard into the local dashboard list. Returns the local id, or null. */
    suspend fun import(
        context: Context,
        prefs: PreferencesManager,
        meta: Hki7SharedDashboardMeta,
    ): String? {
        val raw = Hki7Endpoint.withClient(context) { it.hki7GetDashboard(meta.id) } ?: return null
        return prefs.importSharedDashboard(meta.id, raw, nameOverride = meta.name, updatedAt = meta.updated)
    }

    /** Pulls newer versions of every locally-imported shared dashboard and merges them in, preserving
     * the recipient's own aesthetic changes. Unchanged dashboards (same `updated` timestamp) are
     * skipped. Returns true when the currently-active dashboard was refreshed, so the caller can
     * reload its in-memory view. */
    suspend fun syncUpdates(context: Context, prefs: PreferencesManager): Boolean {
        val locals = prefs.dashboards.first().filter { it.id.startsWith("shared-") }
        if (locals.isEmpty()) return false
        val sharedByLocalId = listSharedForMe(context).associateBy { "shared-${it.id}" }
        var activeChanged = false
        for (local in locals) {
            val meta = sharedByLocalId[local.id] ?: continue
            if (meta.updated.isNotBlank() && meta.updated == local.sharedUpdatedAt) continue
            val raw = Hki7Endpoint.withClient(context) { it.hki7GetDashboard(meta.id) } ?: continue
            if (prefs.applySharedDashboardUpdate(local.id, raw, meta.updated)) activeChanged = true
        }
        return activeChanged
    }
}

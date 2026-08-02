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
        val imported = Hki7Endpoint.withClient(context) { client ->
            val identity = client.hki7WhoAmI() ?: return@withClient null
            val policy = if (identity.isAdmin || identity.isOwner) Hki7Policy() else client.hki7GetMyPolicy()
            val raw = client.hki7GetDashboard(meta.id) ?: return@withClient null
            raw to policy
        } ?: return null
        val (raw, policy) = imported
        // Cache the policy before activating the subscription so every storage-level permission
        // check observes the administrator's current values immediately.
        prefs.saveEnforcedPolicy(policy)
        val localId = prefs.importSharedDashboard(
            meta.id,
            raw,
            nameOverride = meta.name,
            updatedAt = meta.updated,
        ) ?: return null
        prefs.markFamilyDashboardSubscribed()
        return localId
    }

    /** Outcome of a shared-dashboard sync. */
    data class SyncResult(val activeChanged: Boolean, val accessLost: Boolean)

    /** Reconciles this user's imported shared dashboards with the cloud: merges newer versions in
     * (preserving the recipient's aesthetic changes) and removes any the admin unpublished. If the
     * cloud can't be reached the local dashboards are left completely untouched, so a transient outage
     * never deletes anything. When the removal empties the dashboard list, [SyncResult.accessLost]
     * asks the caller to return to the permission-aware dashboard chooser. */
    suspend fun syncUpdates(context: Context, prefs: PreferencesManager): SyncResult {
        val locals = prefs.dashboards.first().filter { it.id.startsWith("shared-") }
        if (locals.isEmpty()) return SyncResult(activeChanged = false, accessLost = false)
        // null (not empty) means the component is unreachable — do not prune on a failed call.
        val shared = Hki7Endpoint.withClient(context) { it.hki7ListSharedDashboards() }
            ?: return SyncResult(activeChanged = false, accessLost = false)
        val sharedByLocalId = shared.associateBy { "shared-${it.id}" }
        // The current user owns some of these; those are the source of truth and are pushed by
        // pushOwnedUpdates, never pulled — pulling would merge the (older) cloud copy back over a
        // fresh local edit and appear to "revert" the dashboard. Only pull dashboards owned by others.
        val myUserId = Hki7Endpoint.withClient(context) { it.hki7WhoAmI() }?.userId
        var activeChanged = false
        for (local in locals) {
            val meta = sharedByLocalId[local.id] ?: continue
            if (myUserId != null && meta.ownerId == myUserId) continue
            if (meta.updated.isNotBlank() && meta.updated == local.sharedUpdatedAt) continue
            val raw = Hki7Endpoint.withClient(context) { it.hki7GetDashboard(meta.id) } ?: continue
            if (prefs.applySharedDashboardUpdate(local.id, raw, meta.updated)) activeChanged = true
        }
        val prune = prefs.pruneUnpublishedSharedDashboards(sharedByLocalId.keys)
        return SyncResult(
            activeChanged = activeChanged || prune.activeReplaced,
            accessLost = prune.needsDashboardChoice,
        )
    }

    /** Deletes a published dashboard from the cloud (admin only). Alias of [unpublish] for clarity. */
    suspend fun delete(context: Context, sharedId: String): Boolean = unpublish(context, sharedId)

    /** Re-publishes every dashboard the current user owns and has shared, so recipients pick up the
     * owner's latest edits. Only dashboards whose local content actually changed are pushed (their
     * recipient list is preserved), so unchanged dashboards don't needlessly bump their timestamp or
     * trigger re-merges. Returns the number pushed, or 0 if the component is unreachable. */
    suspend fun pushOwnedUpdates(context: Context, prefs: PreferencesManager): Int {
        val me = Hki7Endpoint.withClient(context) { it.hki7WhoAmI() } ?: return 0
        val shared = Hki7Endpoint.withClient(context) { it.hki7ListSharedDashboards() } ?: return 0
        val localById = prefs.dashboards.first().associateBy { it.id }
        var pushed = 0
        for (meta in shared) {
            if (meta.ownerId != me.userId) continue
            // The original publishing device keeps the source id; after a reinstall the owner may
            // re-import that same cloud dashboard, which deliberately uses "shared-<id>" locally.
            val local = localById[meta.id] ?: localById["shared-${meta.id}"] ?: continue
            val currentRaw = prefs.exportDashboard(meta.id) ?: continue
            val currentJson = runCatching { json.parseToJsonElement(currentRaw) }.getOrNull() as? JsonObject ?: continue
            // Skip when the cloud copy already matches the local content (structural comparison, so
            // formatting differences from the round-trip don't cause a spurious push).
            val cloudRaw = Hki7Endpoint.withClient(context) { it.hki7GetDashboard(meta.id) }
            val cloudJson = cloudRaw?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            if (cloudJson != null && cloudJson == currentJson) continue
            val result = Hki7Endpoint.withClient(context) { client ->
                client.hki7PublishDashboard(local.name, currentJson, meta.sharedWith, meta.id)
            }
            if (result != null) pushed++
        }
        return pushed
    }
}

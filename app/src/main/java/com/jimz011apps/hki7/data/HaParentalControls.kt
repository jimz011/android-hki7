package com.jimz011apps.hki7.data

import android.content.Context

/**
 * Parental controls via the `hki7` companion component.
 *
 * This is **UX-level hiding for a friendlier dashboard, not a Home Assistant security
 * boundary** — a determined user could still reach entities through Home Assistant
 * directly. It hides views (nav routes) and rooms (area ids) that an admin has chosen
 * to hide from a given person.
 *
 * The signed-in user's own policy is cached in [PreferencesManager] so the UI filters
 * synchronously and keeps hiding while briefly offline; [refreshForCurrentUser] pulls
 * the latest from the component. Admins and owners are never restricted.
 */
object HaParentalControls {

    /**
     * Fetches the current user's policy and caches it. Admins/owners get an empty policy.
     * On failure (no component, no credentials, offline) the cached policy is left as-is,
     * so a transient outage never silently unlocks a restricted view.
     */
    suspend fun refreshForCurrentUser(context: Context, prefs: PreferencesManager) {
        val result = Hki7Endpoint.withClient(context) { client ->
            val identity = client.hki7WhoAmI() ?: return@withClient null
            if (identity.isAdmin || identity.isOwner) Hki7Policy() else client.hki7GetMyPolicy()
        } ?: return
        prefs.saveEnforcedPolicy(result.hiddenViews, result.hiddenRooms)
    }

    // ── Admin editor ────────────────────────────────────────────────────

    /** Every stored policy keyed by user id (admin only). */
    suspend fun listPolicies(context: Context): Map<String, Hki7Policy> =
        Hki7Endpoint.withClient(context) { it.hki7ListPolicies() } ?: emptyMap()

    /** Sets one user's hidden views/rooms (admin only). */
    suspend fun setPolicy(
        context: Context,
        userId: String,
        hiddenViews: List<String>,
        hiddenRooms: List<String>,
    ): Boolean = Hki7Endpoint.withClient(context) {
        it.hki7SetPolicy(userId, hiddenViews, hiddenRooms)
    } ?: false
}

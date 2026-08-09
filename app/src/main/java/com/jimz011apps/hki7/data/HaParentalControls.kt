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

    /** Component version that first stored the event roster and per-person event visibility. */
    const val MIN_EVENTS_COMPONENT_VERSION = "0.9.0"

    /** Component version that first stored whole domains on the event roster. */
    const val MIN_EVENT_DOMAINS_COMPONENT_VERSION = "0.10.0"

    /**
     * Fetches the current user's policy and caches it. Admins/owners get an empty policy.
     * On failure (no component, no credentials, offline) the cached policy is left as-is,
     * so a transient outage never silently unlocks a restricted view.
     */
    suspend fun refreshForCurrentUser(context: Context, prefs: PreferencesManager) {
        val result = Hki7Endpoint.withClient(context) { client ->
            val identity = client.hki7WhoAmI() ?: return@withClient null
            val policy = client.hki7GetMyPolicy()
            // Admins and owners are never restricted, but they carry a phone like everyone else:
            // keep their room following and drop only the restrictions.
            if (identity.isAdmin || identity.isOwner) Hki7Policy(roomFollow = policy.roomFollow) else policy
        } ?: return
        prefs.saveEnforcedPolicy(result)
    }

    /** Refreshes the household's room-presence sensor roster used by the people-per-room counters.
     *  Leaves the cache untouched when the component can't answer, so a transient outage doesn't
     *  blank every counter. */
    suspend fun refreshRoomFollowRoster(context: Context, prefs: PreferencesManager) {
        val sensors = Hki7Endpoint.withClient(context) { it.hki7RoomFollowRoster() } ?: return
        prefs.saveRoomFollowRoster(sensors)
    }

    /** The household event-timeline roster as it applies to the signed-in user.
     *
     *  The component has already removed whatever this person is not allowed to see, so the
     *  returned ids can be subscribed to directly. Null when the component can't answer (absent,
     *  or older than 0.9.0), which the caller shows as "the timeline isn't set up" rather than as
     *  an empty roster. */
    suspend fun eventsRoster(context: Context): Hki7EventsRoster? =
        Hki7Endpoint.withClient(context) { it.hki7EventsRoster() }

    // ── Admin editor ────────────────────────────────────────────────────

    /** Replaces the household's event roster (admin only). Returns what was stored — the component
     *  caps entities and domains separately, so this can be shorter than what was sent. Null when
     *  the save didn't happen. */
    suspend fun setEventsRoster(
        context: Context,
        entityIds: List<String>,
        domains: List<String>,
    ): Hki7EventsRoster? =
        Hki7Endpoint.withClient(context) { it.hki7SetEventsRoster(entityIds, domains) }

    /** Every stored policy keyed by user id (admin only). */
    suspend fun listPolicies(context: Context): Map<String, Hki7Policy> =
        Hki7Endpoint.withClient(context) { it.hki7ListPolicies() } ?: emptyMap()

    /** Sets one user's full policy — hidden views/rooms and edit/visibility permissions (admin only).
     *  Reports [Hki7PolicySaveResult.SAVED_WITHOUT_SEARCH_ACCESS] when an out-of-date component
     *  stored the permissions but not the Visible/Invisible lists. */
    suspend fun setPolicy(
        context: Context,
        userId: String,
        policy: Hki7Policy,
    ): Hki7PolicySaveResult = Hki7Endpoint.withClient(context) {
        it.hki7SetPolicy(userId, policy)
    } ?: Hki7PolicySaveResult.FAILED
}

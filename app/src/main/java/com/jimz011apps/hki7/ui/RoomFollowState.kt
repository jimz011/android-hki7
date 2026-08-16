package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.Hki7RoomFollow
import java.util.Locale

/**
 * Turns a room-presence sensor's state into a Home Assistant area.
 *
 * ESPresense (and HA's `mqtt_room` platform) publish the *room name* as the sensor's state, so
 * this is plain string matching — no MQTT involved anywhere in the app. Most households name their
 * ESPresense rooms after their areas, so the slug match below resolves them with no configuration;
 * [Hki7RoomFollow.stateRooms] only carries the leftovers an admin had to map by hand.
 */

/** States a room-presence sensor uses to say "nowhere", which must never resolve to a room. */
private val AWAY_STATES = setOf(
    "", "not_home", "away", "unknown", "unavailable", "none", "null", "no_one", "not home"
)

/** Lowercased, punctuation-collapsed form so "Living Room", "living_room" and "living-room" match. */
internal fun roomSlug(value: String): String =
    value.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')

/**
 * The area a sensor state refers to, or null when the state names no room (away/unknown) or
 * matches nothing. An explicit override in [Hki7RoomFollow.stateRooms] always wins over the
 * automatic match, so an admin can correct a wrong guess.
 */
internal fun resolveFollowedArea(
    state: String?,
    areas: List<HAArea>,
    follow: Hki7RoomFollow
): String? {
    val raw = state?.trim() ?: return null
    if (raw.lowercase(Locale.ROOT) in AWAY_STATES) return null

    follow.stateRooms[raw]?.let { override -> return override.takeIf { id -> areas.any { it.area_id == id } } }
    val slug = roomSlug(raw)
    if (slug.isEmpty()) return null
    // Overrides may have been stored against a differently-cased spelling of the same state.
    follow.stateRooms.entries.firstOrNull { roomSlug(it.key) == slug }?.let { override ->
        return override.value.takeIf { id -> areas.any { it.area_id == id } }
    }
    return areas.firstOrNull { roomSlug(it.area_id) == slug }?.area_id
        ?: areas.firstOrNull { roomSlug(it.name) == slug }?.area_id
}

/**
 * How many tracked people each area currently holds, keyed by area id. Areas with nobody in them
 * are absent rather than zero, so the counter badge can simply skip them.
 *
 * Counts only the sensors in [rosterEntityIds] — the household roster the admin configured. A
 * person whose sensor is missing from the state snapshot, or whose state names no room, is not
 * counted anywhere.
 */
internal fun peopleCountByArea(
    rosterEntityIds: Collection<String>,
    entitiesById: Map<String, HAEntity>,
    areas: List<HAArea>,
    follow: Hki7RoomFollow
): Map<String, Int> {
    if (rosterEntityIds.isEmpty()) return emptyMap()
    val counts = mutableMapOf<String, Int>()
    rosterEntityIds.distinct().forEach { entityId ->
        val areaId = resolveFollowedArea(entitiesById[entityId]?.state, areas, follow) ?: return@forEach
        counts[areaId] = (counts[areaId] ?: 0) + 1
    }
    return counts
}

/**
 * Who is in each room, rather than only how many. Keyed by area id, values are the roster sensor
 * entity ids resolved to that room, in roster order.
 *
 * The counters were a dead end: a room saying "2" with no way to find out who, and a home with no
 * way to see where everyone is short of opening every room in turn. This is what the counter
 * dialogs read.
 */
internal fun peopleByArea(
    rosterEntityIds: Collection<String>,
    entitiesById: Map<String, HAEntity>,
    areas: List<HAArea>,
    follow: Hki7RoomFollow
): Map<String, List<String>> {
    if (rosterEntityIds.isEmpty()) return emptyMap()
    val byArea = linkedMapOf<String, MutableList<String>>()
    rosterEntityIds.distinct().forEach { entityId ->
        val areaId = resolveFollowedArea(entitiesById[entityId]?.state, areas, follow) ?: return@forEach
        byArea.getOrPut(areaId) { mutableListOf() } += entityId
    }
    return byArea
}

/**
 * Every distinct state the tracked sensors have reported, for the admin's mapping table. Away and
 * unknown states are left out — they intentionally resolve to no room and are not mappable.
 */
internal fun observedRoomStates(
    rosterEntityIds: Collection<String>,
    entitiesById: Map<String, HAEntity>
): List<String> = rosterEntityIds.distinct()
    .mapNotNull { entitiesById[it]?.state?.trim() }
    .filter { it.lowercase(Locale.ROOT) !in AWAY_STATES }
    .distinct()
    .sorted()

/**
 * Decides whether a room change has settled enough to act on.
 *
 * A room-presence sensor bouncing between two adjacent rooms would otherwise prompt the user every
 * few seconds, which is the main way this feature could become unbearable. A candidate room must
 * hold for [Hki7RoomFollow.dwellSeconds] before it counts as a real move.
 */
internal class RoomDwellTracker(private val dwellSeconds: Int) {
    private var candidateAreaId: String? = null
    private var candidateSince: Long = 0L

    /**
     * Feeds the latest resolved area in. Returns the area id once it has held long enough to be a
     * confirmed move, otherwise null. Returns a given move only once.
     */
    fun update(areaId: String?, nowMillis: Long): String? {
        if (areaId == null) {
            candidateAreaId = null
            return null
        }
        if (areaId != candidateAreaId) {
            candidateAreaId = areaId
            candidateSince = nowMillis
        }
        if (nowMillis - candidateSince < dwellSeconds * 1000L) return null
        // Hold the candidate so a steady state doesn't re-confirm on every state tick.
        candidateSince = Long.MAX_VALUE / 2
        return areaId
    }
}

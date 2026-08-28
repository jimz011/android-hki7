package com.jimz011apps.hki7.data

import kotlinx.serialization.Serializable

/**
 * One room as the watch knows it.
 *
 * Lives in `:core` because both sides handle it: the phone builds the list, the watch renders it.
 *
 * Rooms are sent over rather than fetched. Home Assistant's area registry is reachable only over
 * its WebSocket API, and holding a socket open on a watch to read a list that changes once a month
 * would cost battery all day for nothing. More importantly HKI 7's rooms are a dashboard concept —
 * the user's own arrangement, names and icons — so taking them from the phone is what makes the
 * watch show *their* home rather than a raw area list.
 */
@Serializable
data class WearRoom(
    val id: String,
    val name: String,
    /** Pack-qualified icon slug, as on the phone. */
    val icon: String? = null,
    /** Controllable entities in this room, already filtered by the phone to things worth a tap. */
    val entityIds: List<String> = emptyList(),
)

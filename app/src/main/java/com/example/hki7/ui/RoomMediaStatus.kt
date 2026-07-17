package com.example.hki7.ui

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAreaConfig

/**
 * Presentation-neutral media status shared by room tiles and room headers.
 *
 * [representative] lets callers choose an icon from a real player state without coupling this
 * aggregation logic to Compose. When any player is active it is the first active player;
 * otherwise it is the first configured player.
 */
internal data class RoomMediaSummary(
    val text: String?,
    val representative: HAEntity?
)

private val inactiveRoomMediaStates = setOf("", "off", "idle", "unknown", "unavailable")

/** Effective room media players, preferring the grouped field while retaining old dashboards. */
internal fun HKIAreaConfig.roomMediaPlayerIds(): List<String> {
    val groupedIds = mediaPlayerEntityIds.filter(String::isNotBlank).distinct()
    return groupedIds.ifEmpty {
        listOfNotNull(mediaPlayerEntityId?.takeIf(String::isNotBlank))
    }
}

/**
 * Resolves a room's configured media players into one concise line of text.
 *
 * A single active player uses its normal status, even when other configured players are inactive.
 * A count is shown only when two or more players are active.
 */
internal fun resolveRoomMediaStatus(entities: List<HAEntity>): RoomMediaSummary {
    if (entities.isEmpty()) return RoomMediaSummary(text = null, representative = null)

    val activePlayers = entities.filter { entity ->
        entity.state.trim().lowercase() !in inactiveRoomMediaStates
    }
    val representative = activePlayers.firstOrNull() ?: entities.first()

    if (activePlayers.isEmpty()) {
        return RoomMediaSummary(
            text = "No Media is Playing",
            representative = representative
        )
    }

    if (activePlayers.size > 1) {
        val count = activePlayers.size
        return RoomMediaSummary(
            text = "$count Media Players are Playing",
            representative = representative
        )
    }

    return RoomMediaSummary(
        text = singlePlayerStatus(activePlayers.single()),
        representative = representative
    )
}

private fun singlePlayerStatus(entity: HAEntity): String {
    val state = entity.state.trim().lowercase()
    val title = entity.mediaTitle?.takeIf { it.isNotBlank() }

    if ((state == "playing" || state == "paused") && title != null) {
        val artist = entity.mediaArtist?.takeIf { it.isNotBlank() }
        val prefix = if (state == "paused") "Paused: " else ""
        return if (artist != null) "$prefix$title • $artist" else "$prefix$title"
    }

    return state
        .replace('_', ' ')
        .replaceFirstChar { character -> character.uppercase() }
}

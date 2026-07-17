package com.example.hki7.ui

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAreaConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RoomMediaStatusTest {
    @Test
    fun `all off or idle players show no media message case insensitively`() {
        val first = player("media_player.first", "off")
        val second = player("media_player.second", "IDLE")

        val summary = resolveRoomMediaStatus(listOf(first, second))

        assertEquals("No Media is Playing", summary.text)
        assertSame(first, summary.representative)
    }

    @Test
    fun `multiple active players show active player count`() {
        val first = player("media_player.first", "playing", title = "First song")
        val second = player("media_player.second", "paused", title = "Second song")

        val summary = resolveRoomMediaStatus(listOf(first, second))

        assertEquals("2 Media Players are Playing", summary.text)
        assertSame(first, summary.representative)
    }

    @Test
    fun `one active player among several shows that player's status`() {
        val off = player("media_player.off", "off")
        val playing = player("media_player.playing", "playing", title = "Only song")
        val unavailable = player("media_player.unavailable", "unavailable")

        val summary = resolveRoomMediaStatus(listOf(off, playing, unavailable))

        assertEquals("Only song", summary.text)
        assertSame(playing, summary.representative)
    }

    @Test
    fun `single idle player shows no media message`() {
        val idle = player("media_player.speaker", "idle")

        val summary = resolveRoomMediaStatus(listOf(idle))

        assertEquals("No Media is Playing", summary.text)
        assertSame(idle, summary.representative)
    }

    @Test
    fun `single playing player shows title and artist`() {
        val playing = player(
            id = "media_player.speaker",
            state = "playing",
            title = "Night Drive",
            artist = "The Composers"
        )

        val summary = resolveRoomMediaStatus(listOf(playing))

        assertEquals("Night Drive • The Composers", summary.text)
        assertSame(playing, summary.representative)
    }

    @Test
    fun `single paused player prefixes track status`() {
        val paused = player(
            id = "media_player.speaker",
            state = "paused",
            title = "Night Drive",
            artist = "The Composers"
        )

        val summary = resolveRoomMediaStatus(listOf(paused))

        assertEquals("Paused: Night Drive • The Composers", summary.text)
        assertSame(paused, summary.representative)
    }

    @Test
    fun `unavailable and unknown players are not counted as playing`() {
        val unavailable = player("media_player.first", "unavailable")
        val unknown = player("media_player.second", "unknown")
        val off = player("media_player.third", "off")

        val summary = resolveRoomMediaStatus(listOf(unavailable, unknown, off))

        assertEquals("No Media is Playing", summary.text)
        assertSame(unavailable, summary.representative)
    }

    @Test
    fun `empty player list has no media status`() {
        val summary = resolveRoomMediaStatus(emptyList())

        assertNull(summary.text)
        assertNull(summary.representative)
    }

    @Test
    fun `grouped media ids override the legacy single id`() {
        val config = HKIAreaConfig(
            mediaPlayerEntityId = "media_player.legacy",
            mediaPlayerEntityIds = listOf("media_player.first", "media_player.second")
        )

        assertEquals(
            listOf("media_player.first", "media_player.second"),
            config.roomMediaPlayerIds()
        )
    }

    @Test
    fun `legacy media id remains supported`() {
        val config = HKIAreaConfig(mediaPlayerEntityId = "media_player.legacy")

        assertEquals(listOf("media_player.legacy"), config.roomMediaPlayerIds())
    }

    private fun player(
        id: String,
        state: String,
        title: String? = null,
        artist: String? = null
    ): HAEntity = HAEntity(
        entity_id = id,
        state = state,
        attributes = buildJsonObject {
            title?.let { put("media_title", it) }
            artist?.let { put("media_artist", it) }
        }
    )
}

package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.Hki7RoomFollow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomFollowStateTest {

    private val areas = listOf(
        HAArea(area_id = "living_room", name = "Living Room"),
        HAArea(area_id = "kitchen", name = "Kitchen"),
        HAArea(area_id = "office", name = "Study")
    )

    private fun entity(id: String, state: String) = HAEntity(entity_id = id, state = state)

    // ── resolving a state to an area ────────────────────────────────────

    @Test
    fun `ESPresense room names match areas without any configuration`() {
        val follow = Hki7RoomFollow(sensorEntityId = "sensor.phone", enabled = true)

        assertEquals("living_room", resolveFollowedArea("living_room", areas, follow))
        assertEquals("living_room", resolveFollowedArea("Living Room", areas, follow))
        assertEquals("living_room", resolveFollowedArea("living-room", areas, follow))
        assertEquals("kitchen", resolveFollowedArea("  Kitchen  ", areas, follow))
    }

    @Test
    fun `an area is matched by its name when the id differs`() {
        val follow = Hki7RoomFollow(sensorEntityId = "sensor.phone", enabled = true)

        // The area id is "office" but the sensor reports the display name.
        assertEquals("office", resolveFollowedArea("Study", areas, follow))
    }

    @Test
    fun `away and unknown states never resolve to a room`() {
        val follow = Hki7RoomFollow(sensorEntityId = "sensor.phone", enabled = true)

        listOf("not_home", "away", "unknown", "unavailable", "none", "", "   ").forEach { state ->
            assertNull("state '$state' must not resolve", resolveFollowedArea(state, areas, follow))
        }
        assertNull(resolveFollowedArea(null, areas, follow))
    }

    @Test
    fun `an unmatched state resolves to nothing rather than guessing`() {
        val follow = Hki7RoomFollow(sensorEntityId = "sensor.phone", enabled = true)

        assertNull(resolveFollowedArea("kantoor", areas, follow))
    }

    @Test
    fun `an override maps a state the area names cannot`() {
        val follow = Hki7RoomFollow(
            sensorEntityId = "sensor.phone",
            enabled = true,
            stateRooms = mapOf("kantoor" to "office")
        )

        assertEquals("office", resolveFollowedArea("kantoor", areas, follow))
        // Case and punctuation differences in the stored override still match.
        assertEquals("office", resolveFollowedArea("Kantoor", areas, follow))
    }

    @Test
    fun `an override wins over the automatic match`() {
        val follow = Hki7RoomFollow(
            sensorEntityId = "sensor.phone",
            enabled = true,
            stateRooms = mapOf("kitchen" to "living_room")
        )

        assertEquals("living_room", resolveFollowedArea("kitchen", areas, follow))
    }

    @Test
    fun `an override pointing at a deleted area resolves to nothing`() {
        val follow = Hki7RoomFollow(
            sensorEntityId = "sensor.phone",
            enabled = true,
            stateRooms = mapOf("kantoor" to "demolished_room")
        )

        assertNull(resolveFollowedArea("kantoor", areas, follow))
    }

    // ── counting people ─────────────────────────────────────────────────

    @Test
    fun `people are counted per area and empty rooms are absent`() {
        val roster = listOf("sensor.a", "sensor.b", "sensor.c", "sensor.d")
        val entities = mapOf(
            "sensor.a" to entity("sensor.a", "living_room"),
            "sensor.b" to entity("sensor.b", "Living Room"),
            "sensor.c" to entity("sensor.c", "kitchen"),
            "sensor.d" to entity("sensor.d", "not_home")
        )

        val counts = peopleCountByArea(roster, entities, areas, Hki7RoomFollow())

        assertEquals(mapOf("living_room" to 2, "kitchen" to 1), counts)
    }

    @Test
    fun `sensors missing from the state snapshot are not counted`() {
        val counts = peopleCountByArea(
            listOf("sensor.a", "sensor.gone"),
            mapOf("sensor.a" to entity("sensor.a", "kitchen")),
            areas,
            Hki7RoomFollow()
        )

        assertEquals(mapOf("kitchen" to 1), counts)
    }

    @Test
    fun `a duplicated roster entry counts one person`() {
        val counts = peopleCountByArea(
            listOf("sensor.a", "sensor.a"),
            mapOf("sensor.a" to entity("sensor.a", "kitchen")),
            areas,
            Hki7RoomFollow()
        )

        assertEquals(mapOf("kitchen" to 1), counts)
    }

    @Test
    fun `an empty roster counts nobody`() {
        assertEquals(emptyMap<String, Int>(), peopleCountByArea(emptyList(), emptyMap(), areas, Hki7RoomFollow()))
    }

    @Test
    fun `observed states list what the admin can map and skip away states`() {
        val entities = mapOf(
            "sensor.a" to entity("sensor.a", "kantoor"),
            "sensor.b" to entity("sensor.b", "kitchen"),
            "sensor.c" to entity("sensor.c", "not_home"),
            "sensor.d" to entity("sensor.d", "kantoor")
        )

        assertEquals(
            listOf("kantoor", "kitchen"),
            observedRoomStates(listOf("sensor.a", "sensor.b", "sensor.c", "sensor.d"), entities)
        )
    }

    // ── dwell debounce ──────────────────────────────────────────────────

    @Test
    fun `a move is confirmed only after the room holds for the dwell time`() {
        val tracker = RoomDwellTracker(dwellSeconds = 20)

        assertNull(tracker.update("kitchen", 0L))
        assertNull("still inside the dwell window", tracker.update("kitchen", 19_000L))
        assertEquals("kitchen", tracker.update("kitchen", 20_000L))
    }

    @Test
    fun `flapping between adjacent rooms never confirms a move`() {
        val tracker = RoomDwellTracker(dwellSeconds = 20)
        var now = 0L

        // The failure this guards against: a sensor bouncing every few seconds prompting endlessly.
        repeat(20) {
            assertNull(tracker.update("kitchen", now))
            now += 5_000L
            assertNull(tracker.update("living_room", now))
            now += 5_000L
        }
    }

    @Test
    fun `a settled room is confirmed once, not on every state tick`() {
        val tracker = RoomDwellTracker(dwellSeconds = 10)

        assertNull(tracker.update("kitchen", 0L))
        assertEquals("kitchen", tracker.update("kitchen", 10_000L))
        assertNull(tracker.update("kitchen", 15_000L))
        assertNull(tracker.update("kitchen", 90_000L))
    }

    @Test
    fun `leaving and returning confirms the room again`() {
        val tracker = RoomDwellTracker(dwellSeconds = 10)

        assertNull(tracker.update("kitchen", 0L))
        assertEquals("kitchen", tracker.update("kitchen", 10_000L))
        assertNull(tracker.update("office", 20_000L))
        assertEquals("office", tracker.update("office", 30_000L))
        assertNull(tracker.update("kitchen", 40_000L))
        assertEquals("kitchen", tracker.update("kitchen", 50_000L))
    }

    @Test
    fun `going away clears the candidate so the next room starts its own window`() {
        val tracker = RoomDwellTracker(dwellSeconds = 10)

        assertNull(tracker.update("kitchen", 0L))
        assertNull(tracker.update(null, 5_000L))
        // The kitchen's earlier partial window must not carry over.
        assertNull(tracker.update("kitchen", 9_000L))
        assertEquals("kitchen", tracker.update("kitchen", 19_000L))
    }

    /**
     * The tracker itself still honours a zero window, but nothing in the app builds one any more:
     * MainViewModel floors the dwell at [Hki7RoomFollow.MIN_DWELL_SECONDS] and the settings slider
     * starts there. A zero dwell makes the first reading of a room a confirmed move, which is
     * precisely the flapping the window exists to absorb.
     */
    @Test
    fun `a zero dwell confirms immediately, which is why the app floors it`() {
        val tracker = RoomDwellTracker(dwellSeconds = 0)

        assertEquals("kitchen", tracker.update("kitchen", 0L))
        assertTrue(
            "the floor must be high enough to outlast a sensor flap",
            Hki7RoomFollow.MIN_DWELL_SECONDS >= 5
        )
    }

    @Test
    fun `the shipped default dwell is long enough to survive a flap`() {
        val tracker = RoomDwellTracker(dwellSeconds = Hki7RoomFollow.DEFAULT_DWELL_SECONDS)
        var now = 0L
        // Sensor bounces every 5s for a minute: nothing may confirm.
        repeat(6) {
            assertNull(tracker.update("kitchen", now))
            now += 5_000L
            assertNull(tracker.update("living_room", now))
            now += 5_000L
        }
    }
}

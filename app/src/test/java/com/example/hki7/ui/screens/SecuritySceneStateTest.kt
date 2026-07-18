package com.example.hki7.ui.screens

import com.example.hki7.data.HAEntity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySceneStateTest {
    @Test
    fun `triggered alarm takes precedence over every other alarm panel state`() {
        val state = scene(
            "alarms" to listOf(
                entity("alarm_control_panel.downstairs", "disarmed"),
                entity("alarm_control_panel.upstairs", "armed_away"),
                entity("alarm_control_panel.garage", "pending"),
                entity("alarm_control_panel.house", "triggered")
            )
        )

        assertEquals(SecurityAlarmMode.TRIGGERED, state.alarmMode)
        assertEquals(1, state.alertCount)
        assertFalse(state.isArmed)
    }

    @Test
    fun `alarm transition takes precedence over armed and disarmed panels`() {
        val state = scene(
            "alarms" to listOf(
                entity("alarm_control_panel.house", "armed_home"),
                entity("alarm_control_panel.garage", "disarmed"),
                entity("alarm_control_panel.shed", "arming")
            )
        )

        assertEquals(SecurityAlarmMode.TRANSITIONING, state.alarmMode)
        assertFalse(state.isArmed)
    }

    @Test
    fun `pending alarm remains distinct from a requested mode transition`() {
        val state = scene(
            "alarms" to listOf(
                entity("alarm_control_panel.house", "armed_away"),
                entity("alarm_control_panel.entry_delay", "pending")
            )
        )

        assertEquals(SecurityAlarmMode.PENDING, state.alarmMode)
        assertEquals(2, state.alarmCount)
        assertEquals(2, state.availableAlarmCount)
    }

    @Test
    fun `openings use both state and current cover position`() {
        val state = scene(
            "doors" to listOf(
                entity("binary_sensor.front_door", "on"),
                entity("binary_sensor.back_door", "off")
            ),
            "windows" to listOf(entity("binary_sensor.window", "opening")),
            "openings" to listOf(
                entity("binary_sensor.garage_contact", "on", deviceClass = "garage_door")
            ),
            "garage_doors" to listOf(
                entity("cover.garage", "closed", currentPosition = 37),
                entity("cover.offline_garage", "unavailable", currentPosition = 80)
            ),
            "covers" to listOf(
                entity("cover.gate", "closing", deviceClass = "gate"),
                entity("cover.blind", "opening", deviceClass = "blind")
            )
        )

        assertEquals(1, state.openDoors)
        assertEquals(1, state.openWindows)
        assertEquals(1, state.openGarageDoors)
        assertEquals(1, state.openCovers)
        assertEquals(4, state.openingCount)
        assertEquals(6, state.openingEntityCount)
        assertEquals(1, state.securityCoverCount)
    }

    @Test
    fun `locks distinguish secure unlocked jammed and unavailable states`() {
        val state = scene(
            "locks" to listOf(
                entity("lock.front", "locked"),
                entity("lock.back", "unlocked"),
                entity("lock.garage", "jammed"),
                entity("lock.shed", "unavailable")
            )
        )

        assertEquals(4, state.lockCount)
        assertEquals(1, state.lockedLocks)
        assertEquals(1, state.unlockedLocks)
        assertEquals(1, state.jammedLocks)
        assertEquals(1, state.alertCount)
        assertEquals(1, state.unavailableCount)
    }

    @Test
    fun `camera availability treats operational states as online`() {
        val state = scene(
            "cameras" to listOf(
                entity("camera.idle", "idle"),
                entity("camera.streaming", "streaming"),
                entity("camera.recording", "recording"),
                entity("camera.off", "off"),
                entity("camera.unknown", "unknown"),
                entity("camera.unavailable", "unavailable")
            )
        )

        assertEquals(6, state.cameras)
        assertEquals(3, state.onlineCameras)
        assertEquals(2, state.unavailableCount)
    }

    @Test
    fun `presence and occupancy are separate from motion activity`() {
        val state = scene(
            "presence" to listOf(
                entity("person.alex", "home"),
                entity("person.sam", "not_home"),
                entity("device_tracker.phone", "work")
            ),
            "occupancy" to listOf(
                entity("binary_sensor.office_occupied", "on"),
                entity("binary_sensor.guest_occupied", "off")
            ),
            "motion" to listOf(
                entity("binary_sensor.hall_motion", "detected"),
                entity("sensor.custom_motion", "active"),
                entity("binary_sensor.garden_motion", "off")
            )
        )

        assertEquals(2, state.peopleHome)
        assertEquals(2, state.motionCount)
    }

    @Test
    fun `only occupancy binary sensors auto-discover as presence`() {
        val person = entity("person.alex", "home")
        val tracker = entity("device_tracker.alex_phone", "home")
        val occupancy = entity("binary_sensor.office_occupied", "on", deviceClass = "occupancy")
        val legacyPresence = entity("binary_sensor.legacy_presence", "on", deviceClass = "presence")

        assertTrue(occupancy.isAutoSecurityEntityFor("presence"))
        assertFalse(person.isAutoSecurityEntityFor("presence"))
        assertFalse(tracker.isAutoSecurityEntityFor("presence"))
        assertFalse(legacyPresence.isAutoSecurityEntityFor("presence"))
        assertFalse(person.isAutoSecurityEntityFor("motion"))
    }

    @Test
    fun `security auto-discovery uses exact requested classes and domains`() {
        assertTrue(entity("binary_sensor.front_door", "off", deviceClass = "door").isAutoSecurityEntityFor("doors"))
        assertFalse(entity("binary_sensor.garage", "off", deviceClass = "garage_door").isAutoSecurityEntityFor("doors"))
        assertTrue(entity("binary_sensor.bedroom_window", "off", deviceClass = "window").isAutoSecurityEntityFor("windows"))
        assertTrue(entity("binary_sensor.hall_motion", "off", deviceClass = "motion").isAutoSecurityEntityFor("motion"))
        assertFalse(entity("binary_sensor.washer_moving", "off", deviceClass = "moving").isAutoSecurityEntityFor("motion"))
        assertTrue(entity("lock.front_door", "locked").isAutoSecurityEntityFor("locks"))
        assertFalse(entity("binary_sensor.front_door_lock", "off", deviceClass = "lock").isAutoSecurityEntityFor("locks"))
    }

    @Test
    fun `safety and leak hazards count common Home Assistant and custom states`() {
        val state = scene(
            "fire" to listOf(entity("binary_sensor.heat_alarm", "on")),
            "smoke" to listOf(entity("sensor.custom_smoke", "smoke")),
            "gas" to listOf(
                entity("binary_sensor.co_alarm", "triggered"),
                entity("binary_sensor.offline_gas", "unavailable")
            ),
            "moisture" to listOf(
                entity("sensor.utility_leak", "wet"),
                entity("binary_sensor.kitchen_leak", "off")
            ),
            "co2" to listOf(entity("sensor.co2", "1800"))
        )

        assertEquals(3, state.safetyAlerts)
        assertEquals(1, state.leakAlerts)
        assertEquals(4, state.alertCount)
        assertTrue(state.unavailableCount > 0)
    }

    @Test
    fun `unavailable alarm panel is not reported as no configured alarm`() {
        val state = scene(
            "alarms" to listOf(entity("alarm_control_panel.house", "unavailable"))
        )

        assertEquals(1, state.alarmCount)
        assertEquals(0, state.availableAlarmCount)
        assertEquals(SecurityAlarmMode.NONE, state.alarmMode)
        assertEquals(1, state.unavailableCount)
    }

    @Test
    fun `sound and beam activity prevent an all-clear scene without becoming safety alarms`() {
        val state = scene(
            "sound" to listOf(entity("binary_sensor.glass_break", "on")),
            "light" to listOf(entity("binary_sensor.driveway_beam", "detected"))
        )

        assertEquals(2, state.sensorActivityCount)
        assertEquals(0, state.alertCount)
    }

    private fun scene(vararg groups: Pair<String, List<HAEntity>>): SecuritySceneState =
        mapOf(*groups).toSecuritySceneState()

    private fun entity(
        id: String,
        state: String,
        currentPosition: Int? = null,
        deviceClass: String? = null
    ) = HAEntity(
        entity_id = id,
        state = state,
        attributes = buildJsonObject {
            currentPosition?.let { put("current_position", it) }
            deviceClass?.let { put("device_class", it) }
        }
    )
}

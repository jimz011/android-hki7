package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPopupTest {

    @Test
    fun binarySensorFiresOnRisingEdgeOnly() {
        assertTrue(isCameraPopupTriggerFiring("binary_sensor.gate", "binary_sensor.gate", "off", "on"))
        assertFalse(isCameraPopupTriggerFiring("binary_sensor.gate", "binary_sensor.gate", "on", "on"))
        assertFalse(isCameraPopupTriggerFiring("binary_sensor.gate", "binary_sensor.gate", "on", "off"))
        assertFalse(isCameraPopupTriggerFiring("binary_sensor.gate", "binary_sensor.motion", "off", "on"))
    }

    @Test
    fun eventEntityFiresOnNewState() {
        assertTrue(
            isCameraPopupTriggerFiring(
                "event.front_person",
                "event.front_person",
                "2026-08-22T10:00:00+00:00",
                "2026-08-22T10:01:00+00:00",
            )
        )
        assertFalse(
            isCameraPopupTriggerFiring(
                "event.front_person",
                "event.front_person",
                "2026-08-22T10:00:00+00:00",
                "2026-08-22T10:00:00+00:00",
            )
        )
        assertFalse(
            isCameraPopupTriggerFiring("event.front_person", "event.front_person", "idle", "unavailable")
        )
    }

    @Test
    fun enableHelperMustBeOnWhenSet() {
        assertTrue(isCameraPopupEnableSatisfied(null, "off"))
        assertTrue(isCameraPopupEnableSatisfied("", "off"))
        assertTrue(isCameraPopupEnableSatisfied("input_boolean.tillat_popup_varsler", "on"))
        assertFalse(isCameraPopupEnableSatisfied("input_boolean.tillat_popup_varsler", "off"))
    }

    @Test
    fun sixAmUntilMidnightWindow() {
        // Matches the HA automation: after 06:00, before 00:00.
        assertFalse(isWithinCameraPopupTimeWindow("06:00", "00:00", 5 * 60 + 59))
        assertTrue(isWithinCameraPopupTimeWindow("06:00", "00:00", 6 * 60))
        assertTrue(isWithinCameraPopupTimeWindow("06:00", "00:00", 23 * 60 + 59))
        assertFalse(isWithinCameraPopupTimeWindow("06:00", "00:00", 0))
    }

    @Test
    fun daytimeWindowDoesNotWrap() {
        assertTrue(isWithinCameraPopupTimeWindow("08:00", "22:00", 12 * 60))
        assertFalse(isWithinCameraPopupTimeWindow("08:00", "22:00", 23 * 60))
        assertFalse(isWithinCameraPopupTimeWindow("08:00", "22:00", 7 * 60))
    }

    @Test
    fun equalAfterAndBeforeIsAnEmptyWindow() {
        assertFalse(isWithinCameraPopupTimeWindow("08:00", "08:00", 8 * 60))
        assertFalse(isWithinCameraPopupTimeWindow("08:00", "08:00", 8 * 60 + 1))
        assertFalse(isWithinCameraPopupTimeWindow("08:00", "08:00", 7 * 60 + 59))
    }

    @Test
    fun matchingUsesAllGates() {
        val settings = CameraPopupSettings(
            rules = listOf(
                CameraPopupRule(
                    id = "front",
                    name = "Front door",
                    triggerEntityId = "binary_sensor.front_person",
                    cameraEntityId = "camera.front",
                    enableEntityId = "input_boolean.tillat_popup_varsler",
                    timeAfter = "06:00",
                    timeBefore = "00:00",
                )
            )
        )
        val hits = matchingCameraPopupRules(
            settings = settings,
            changedEntityId = "binary_sensor.front_person",
            previousState = "off",
            nextState = "on",
            enableStateFor = { if (it == "input_boolean.tillat_popup_varsler") "on" else null },
            nowMinutes = 18 * 60,
        )
        assertEquals(1, hits.size)
        assertEquals("front", hits.single().id)

        val blocked = matchingCameraPopupRules(
            settings = settings,
            changedEntityId = "binary_sensor.front_person",
            previousState = "off",
            nextState = "on",
            enableStateFor = { "off" },
            nowMinutes = 18 * 60,
        )
        assertTrue(blocked.isEmpty())
    }

    @Test
    fun fullscreenIsTheDefaultForANewRule() {
        assertTrue(CameraPopupRule().fullscreen)
    }
}

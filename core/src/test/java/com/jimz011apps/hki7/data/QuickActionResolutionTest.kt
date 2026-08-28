package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The quick-action rules the phone, the car and the watch all run.
 *
 * These matter more than most model tests: the same shortcut is resolved independently on three
 * devices, so a rule that drifts would make one shortcut behave differently depending on which
 * one the user reached for — and that would only ever be noticed on real hardware.
 */
class QuickActionResolutionTest {

    private fun action(entityId: String, action: HKIAction = HKIAction()) =
        HKIQuickAction(id = "test", entityId = entityId, action = action)

    @Test
    fun `default on a light is a toggle`() {
        assertEquals(QuickActionKind.TOGGLE, action("light.kitchen").resolvedKind())
    }

    @Test
    fun `default on a scene turns it on rather than toggling it`() {
        val resolved = action("scene.movie_night").resolvedAction()
        assertEquals("call_service", resolved.type)
        assertEquals("scene.turn_on", resolved.service)
    }

    @Test
    fun `default on a script runs it rather than stopping a running one`() {
        // script.toggle would stop a script that is already running, which is never what someone
        // tapping a shortcut from a car or a watch means.
        assertEquals("script.turn_on", action("script.goodnight").resolvedAction().service)
    }

    @Test
    fun `default on a button presses it`() {
        assertEquals("button.press", action("button.doorbell").resolvedAction().service)
        assertEquals("input_button.press", action("input_button.test").resolvedAction().service)
    }

    @Test
    fun `default on a lock is unsupported rather than guessing a direction`() {
        // A lock has no unambiguous one-tap meaning and no lock.toggle service. Guessing while
        // someone is driving is the wrong answer; making them choose is the right one.
        assertEquals(QuickActionKind.UNSUPPORTED, action("lock.front_door").resolvedKind())
    }

    @Test
    fun `default on a sensor is unsupported`() {
        assertEquals(QuickActionKind.UNSUPPORTED, action("sensor.temperature").resolvedKind())
    }

    @Test
    fun `dashboard-only action types are unsupported off the dashboard`() {
        // These all route to the phone app's own UI, which neither a car nor a watch can draw.
        for (type in listOf("more_info", "navigate", "url", "custom_popup", "none")) {
            assertEquals(
                "expected $type to be unsupported",
                QuickActionKind.UNSUPPORTED,
                action("light.kitchen", HKIAction(type = type)).resolvedKind(),
            )
        }
    }

    @Test
    fun `an explicit lock action is supported`() {
        val locking = action("lock.front_door", HKIAction(type = "call_service", service = "lock.lock"))
        assertEquals(QuickActionKind.CALL_SERVICE, locking.resolvedKind())
    }

    @Test
    fun `a call_service without a domain-qualified service is unsupported`() {
        val broken = action("light.kitchen", HKIAction(type = "call_service", service = "turn_on"))
        assertEquals(QuickActionKind.UNSUPPORTED, broken.resolvedKind())
    }

    @Test
    fun `an explicit target wins over the entry's own entity`() {
        val redirected = action(
            "switch.button",
            HKIAction(type = "toggle", targetEntityId = "light.hallway"),
        )
        assertEquals("light.hallway", redirected.targetEntityId())
    }

    @Test
    fun `a blank entity has no target and cannot toggle`() {
        val empty = HKIQuickAction(id = "test", entityId = "")
        assertNull(empty.targetEntityId())
        assertEquals(QuickActionKind.UNSUPPORTED, empty.resolvedKind())
    }

    @Test
    fun `service payload targets the entry's entity by default`() {
        val resolved = action("scene.movie_night").resolvedAction()
        val payload = buildHKIActionServicePayload(resolved, "scene.movie_night")
        assertEquals("\"scene.movie_night\"", payload["entity_id"].toString())
    }

    @Test
    fun `both surfaces are offered by default`() {
        val fresh = HKIQuickAction(entityId = "light.kitchen")
        assertEquals(true, fresh.showInCar)
        assertEquals(true, fresh.showOnWatch)
    }
}

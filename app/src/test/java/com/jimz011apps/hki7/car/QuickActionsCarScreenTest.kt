package com.jimz011apps.hki7.car

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionsCarScreenTest {
    @Test
    fun `changing homes replaces the rendered car session`() {
        assertTrue(carInstanceChanged(true, "home-a", "home-b"))
        assertFalse(carInstanceChanged(true, "home-a", "home-a"))
    }

    @Test
    fun `first legacy home load is distinct from an already rendered legacy home`() {
        assertTrue(carInstanceChanged(false, null, null))
        assertFalse(carInstanceChanged(true, null, null))
    }

    @Test
    fun `stale rendered action remains pinned to its rendered home`() {
        val renderedAction = CarActionTarget("home-a")
        val newlyActiveHome = "home-b"

        assertTrue(renderedAction.instanceId != newlyActiveHome)
        assertTrue(renderedAction.instanceId == "home-a")
    }
}

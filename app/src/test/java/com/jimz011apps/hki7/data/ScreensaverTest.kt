package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreensaverTest {
    @Test
    fun timeoutIsClamped() {
        assertEquals(15, ScreensaverSettings(timeoutSeconds = 1).clampedTimeoutSeconds())
        assertEquals(900, ScreensaverSettings(timeoutSeconds = 10_000).clampedTimeoutSeconds())
        assertEquals(120, ScreensaverSettings().clampedTimeoutSeconds())
    }

    @Test
    fun clockPanelIsTheDefaultLayout() {
        assertTrue(ScreensaverSettings().isClockPanel())
        assertFalse(ScreensaverSettings(layout = SCREENSAVER_LAYOUT_CLOCK_ONLY).isClockPanel())
    }
}

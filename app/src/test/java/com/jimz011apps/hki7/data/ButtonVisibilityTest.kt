package com.jimz011apps.hki7.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ButtonVisibilityTest {

    private val dec25 = LocalDateTime.parse("2026-12-25T10:00")
    private val jul01 = LocalDateTime.parse("2026-07-01T10:00")

    @Test
    fun `no rule is always visible`() {
        assertTrue(isButtonVisibleAt(HKIButtonConfig(), dec25))
    }

    @Test
    fun `plain hidden is never visible`() {
        assertFalse(isButtonVisibleAt(HKIButtonConfig(hidden = true), dec25))
    }

    @Test
    fun `show-during window is visible inside and hidden outside`() {
        val xmas = HKIButtonConfig(
            visibilityStart = "2026-12-24T00:00",
            visibilityEnd = "2026-12-26T23:59",
            visibilityRangeMode = "show"
        )
        assertTrue(isButtonVisibleAt(xmas, dec25))
        assertFalse(isButtonVisibleAt(xmas, jul01))
    }

    @Test
    fun `hide-during window is hidden inside and visible outside`() {
        val summerOff = HKIButtonConfig(
            visibilityStart = "2026-12-24T00:00",
            visibilityEnd = "2026-12-26T23:59",
            visibilityRangeMode = "hide"
        )
        assertFalse(isButtonVisibleAt(summerOff, dec25))
        assertTrue(isButtonVisibleAt(summerOff, jul01))
    }

    @Test
    fun `open-ended start-only window is a half-open range`() {
        val fromDec = HKIButtonConfig(visibilityStart = "2026-12-01T00:00", visibilityRangeMode = "show")
        assertTrue(isButtonVisibleAt(fromDec, dec25))
        assertFalse(isButtonVisibleAt(fromDec, jul01))
    }

    @Test
    fun `unparseable bounds are ignored rather than hiding forever`() {
        val bad = HKIButtonConfig(visibilityStart = "not-a-date", visibilityRangeMode = "show")
        assertTrue(isButtonVisibleAt(bad, dec25))
    }
}

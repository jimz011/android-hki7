package com.jimz011apps.hki7.ui.components

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonTimingTest {
    @Test
    fun `astronomical countdown uses the next equinox or solstice`() {
        val timing = astronomicalSeasonTiming("summer", LocalDate.of(2026, 7, 21))

        assertEquals("summer", timing.current)
        assertEquals("autumn", timing.next)
        assertEquals(63L, timing.daysUntilNext)
    }

    @Test
    fun `meteorological face uses calendar season boundaries`() {
        val timing = meteorologicalSeasonTiming("summer", LocalDate.of(2026, 7, 21))

        assertEquals("summer", timing.current)
        assertEquals("autumn", timing.next)
        assertEquals(42L, timing.daysUntilNext)
    }

    @Test
    fun `southern hemisphere is inferred from the astronomical state`() {
        val timing = astronomicalSeasonTiming("winter", LocalDate.of(2026, 7, 21))

        assertEquals("winter", timing.current)
        assertEquals("spring", timing.next)
        assertEquals(63L, timing.daysUntilNext)
    }

    @Test
    fun `transition day reports zero while the old season is still active`() {
        val timing = astronomicalSeasonTiming("autumn", LocalDate.of(2026, 12, 21))

        assertEquals("winter", timing.next)
        assertEquals(0L, timing.daysUntilNext)
    }
}

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

    @Test
    fun `yearly recurrence ignores the year`() {
        // Window authored in 2020 still applies in 2026.
        val xmas = HKIButtonConfig(
            visibilityStart = "2020-12-24T00:00",
            visibilityEnd = "2020-12-26T23:59",
            visibilityRangeMode = "show",
            visibilityRecurrence = "yearly"
        )
        assertTrue(isButtonVisibleAt(xmas, LocalDateTime.parse("2026-12-25T10:00")))
        assertTrue(isButtonVisibleAt(xmas, LocalDateTime.parse("2030-12-25T10:00")))
        assertFalse(isButtonVisibleAt(xmas, LocalDateTime.parse("2026-07-01T10:00")))
    }

    @Test
    fun `yearly recurrence handles a window that wraps new year`() {
        val newYear = HKIButtonConfig(
            visibilityStart = "2020-12-30T00:00",
            visibilityEnd = "2020-01-02T23:59", // month-day only; wraps the year boundary
            visibilityRangeMode = "show",
            visibilityRecurrence = "yearly"
        )
        assertTrue(isButtonVisibleAt(newYear, LocalDateTime.parse("2026-12-31T12:00")))
        assertTrue(isButtonVisibleAt(newYear, LocalDateTime.parse("2027-01-01T12:00")))
        assertFalse(isButtonVisibleAt(newYear, LocalDateTime.parse("2026-06-15T12:00")))
    }

    @Test
    fun `daily recurrence uses only the time of day`() {
        val nightly = HKIButtonConfig(
            visibilityStart = "2020-01-01T22:00",
            visibilityEnd = "2020-01-01T23:30",
            visibilityRangeMode = "show",
            visibilityRecurrence = "daily"
        )
        assertTrue(isButtonVisibleAt(nightly, LocalDateTime.parse("2026-07-15T22:30")))
        assertFalse(isButtonVisibleAt(nightly, LocalDateTime.parse("2026-07-15T09:00")))
    }

    @Test
    fun `mixed connectors use AND precedence within OR groups`() {
        val config = HKIButtonConfig(
            visibilityConditions = listOf(
                HKIVisibilityCondition(entityId = "binary_sensor.a", state = "on"),
                HKIVisibilityCondition(connector = VISIBILITY_CONNECTOR_OR, entityId = "binary_sensor.b", state = "on"),
                HKIVisibilityCondition(connector = VISIBILITY_CONNECTOR_AND, entityId = "binary_sensor.c", state = "on"),
            )
        )
        val states = mapOf("binary_sensor.a" to "off", "binary_sensor.b" to "on", "binary_sensor.c" to "off")
        assertFalse(isButtonVisibleAt(config, dec25, resolveEntityState = { states[it] }))
        assertTrue(isButtonVisibleAt(config, dec25, resolveEntityState = { id -> if (id == "binary_sensor.a") "on" else states[id] }))
    }

}

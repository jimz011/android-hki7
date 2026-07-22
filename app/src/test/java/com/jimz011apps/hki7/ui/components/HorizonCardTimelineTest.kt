package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class HorizonCardTimelineTest {

    @Test
    fun `daytime uses previous rise and today's setting`() {
        val sun = sunEntity(
            state = "above_horizon",
            rising = false,
            nextRising = "2026-07-15T05:40:00+02:00",
            nextSetting = "2026-07-14T21:50:00+02:00",
            nextDawn = "2026-07-15T04:55:00+02:00",
            nextNoon = "2026-07-15T13:45:00+02:00",
            nextDusk = "2026-07-14T22:35:00+02:00"
        )

        val timeline = calculateSunTimeline(
            sun,
            ZonedDateTime.parse("2026-07-14T15:00:00+02:00[Europe/Amsterdam]")
        )

        assertNotNull(timeline)
        timeline!!
        assertEquals("2026-07-14T05:40+02:00", timeline.sunrise.toOffsetDateTime().toString())
        assertEquals("2026-07-14T21:50+02:00", timeline.sunset.toOffsetDateTime().toString())
        assertEquals("2026-07-14T13:45+02:00", timeline.solarNoon.toOffsetDateTime().toString())
        assertEquals("2026-07-14T04:55+02:00", timeline.dawn?.toOffsetDateTime().toString())
        assertEquals("2026-07-14T22:35+02:00", timeline.dusk?.toOffsetDateTime().toString())
        assertTrue(timeline.currentPhase in 0.5..0.7)
        assertTrue(timeline.sunProgress in timeline.sunriseProgress..timeline.sunsetProgress)
        assertTrue(timeline.isAboveHorizon)
    }

    @Test
    fun `pre-dawn rising state selects the upcoming daylight cycle`() {
        val sun = sunEntity(
            state = "below_horizon",
            rising = true,
            nextRising = "2026-07-14T05:40:00+02:00",
            nextSetting = "2026-07-14T21:50:00+02:00",
            nextDawn = "2026-07-14T04:55:00+02:00",
            nextNoon = "2026-07-14T13:45:00+02:00",
            nextDusk = "2026-07-14T22:35:00+02:00"
        )

        val timeline = calculateSunTimeline(
            sun,
            ZonedDateTime.parse("2026-07-14T04:00:00+02:00[Europe/Amsterdam]")
        )!!

        assertEquals("2026-07-14T05:40+02:00", timeline.sunrise.toOffsetDateTime().toString())
        assertEquals("2026-07-14T21:50+02:00", timeline.sunset.toOffsetDateTime().toString())
        assertTrue(timeline.currentPhase < 0.0)
        assertTrue(timeline.sunProgress < timeline.sunriseProgress)
        assertFalse(timeline.isAboveHorizon)
    }

    @Test
    fun `post-sunset falling state selects the completed daylight cycle`() {
        val sun = sunEntity(
            state = "below_horizon",
            rising = false,
            nextRising = "2026-07-15T05:40:00+02:00",
            nextSetting = "2026-07-15T21:49:00+02:00",
            nextDawn = "2026-07-15T04:55:00+02:00",
            nextNoon = "2026-07-15T13:45:00+02:00",
            nextDusk = "2026-07-15T22:34:00+02:00"
        )

        val timeline = calculateSunTimeline(
            sun,
            ZonedDateTime.parse("2026-07-14T22:30:00+02:00[Europe/Amsterdam]")
        )!!

        assertEquals("2026-07-14T05:40+02:00", timeline.sunrise.toOffsetDateTime().toString())
        assertEquals("2026-07-14T21:49+02:00", timeline.sunset.toOffsetDateTime().toString())
        assertTrue(timeline.currentPhase > 1.0)
        assertTrue(timeline.sunProgress > timeline.sunsetProgress)
        assertFalse(timeline.isAboveHorizon)
    }

    @Test
    fun `timeline exposes Home Assistant azimuth and elevation`() {
        val sun = sunEntity(
            state = "above_horizon",
            rising = true,
            nextRising = "2026-07-15T05:40:00+02:00",
            nextSetting = "2026-07-14T21:50:00+02:00",
            nextDawn = null,
            nextNoon = null,
            nextDusk = null,
            azimuth = 131.07,
            elevation = 3.53
        )

        val timeline = calculateSunTimeline(
            sun,
            ZonedDateTime.parse("2026-07-14T08:00:00+02:00[Europe/Amsterdam]")
        )!!

        assertEquals(131.07, timeline.azimuth!!, 0.0001)
        assertEquals(3.53, timeline.elevation!!, 0.0001)
        assertTrue(timeline.solarNoon.isAfter(timeline.sunrise))
        assertTrue(timeline.solarNoon.isBefore(timeline.sunset))
    }

    @Test
    fun `offset timestamps are converted to the requested zone without changing instants`() {
        val sun = sunEntity(
            state = "above_horizon",
            rising = true,
            nextRising = "2026-10-26T06:30:00Z",
            nextSetting = "2026-10-25T16:30:00Z",
            nextDawn = null,
            nextNoon = "2026-10-26T11:30:00Z",
            nextDusk = null
        )

        val timeline = calculateSunTimeline(
            sun,
            ZonedDateTime.parse("2026-10-25T12:00:00+01:00[Europe/Amsterdam]")
        )!!

        assertEquals("Europe/Amsterdam", timeline.sunrise.zone.id)
        assertEquals("2026-10-25T07:30+01:00", timeline.sunrise.toOffsetDateTime().toString())
        assertEquals("2026-10-25T17:30+01:00", timeline.sunset.toOffsetDateTime().toString())
    }

    @Test
    fun `missing rise or setting returns no timeline`() {
        val missingSetting = HAEntity(
            entity_id = "sun.sun",
            state = "above_horizon",
            attributes = buildJsonObject { put("next_rising", "2026-07-15T05:40:00+02:00") }
        )

        assertNull(
            calculateSunTimeline(
                missingSetting,
                ZonedDateTime.parse("2026-07-14T15:00:00+02:00[Europe/Amsterdam]")
            )
        )
        assertNull(
            calculateSunTimeline(
                null,
                ZonedDateTime.parse("2026-07-14T15:00:00+02:00[Europe/Amsterdam]")
            )
        )
    }

    private fun sunEntity(
        state: String,
        rising: Boolean,
        nextRising: String,
        nextSetting: String,
        nextDawn: String?,
        nextNoon: String?,
        nextDusk: String?,
        azimuth: Double = 180.0,
        elevation: Double = 30.0
    ) = HAEntity(
        entity_id = "sun.sun",
        state = state,
        attributes = buildJsonObject {
            put("next_rising", nextRising)
            put("next_setting", nextSetting)
            nextDawn?.let { put("next_dawn", it) }
            nextNoon?.let { put("next_noon", it) }
            nextDusk?.let { put("next_dusk", it) }
            put("rising", JsonPrimitive(rising))
            put("azimuth", JsonPrimitive(azimuth))
            put("elevation", JsonPrimitive(elevation))
        }
    )
}

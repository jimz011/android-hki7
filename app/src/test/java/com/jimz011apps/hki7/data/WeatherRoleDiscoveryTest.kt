package com.jimz011apps.hki7.data

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRoleDiscoveryTest {
    @Test
    fun prefersAstronomicalSeasonAndUsAirVisualAqi() {
        val entities = listOf(
            HAEntity("sensor.moon_phase", "full_moon"),
            HAEntity("sensor.meteorological_season", "summer"),
            HAEntity("sensor.astronomical_season", "summer"),
            aqi("sensor.chinese_air_quality_index", "18"),
            aqi("sensor.us_air_quality_index", "42")
        )
        val registry = listOf(
            HAEntityRegistryEntry("sensor.moon_phase", platform = "moon", translation_key = "phase"),
            HAEntityRegistryEntry("sensor.meteorological_season", platform = "season", config_entry_id = "met"),
            HAEntityRegistryEntry("sensor.astronomical_season", platform = "season", config_entry_id = "astro"),
            HAEntityRegistryEntry(
                "sensor.chinese_air_quality_index",
                platform = "airvisual",
                unique_id = "station_cn_air_quality_index"
            ),
            HAEntityRegistryEntry(
                "sensor.us_air_quality_index",
                platform = "airvisual",
                unique_id = "station_us_air_quality_index"
            )
        )

        val result = discoverWeatherRoleEntities(
            entities,
            registry,
            listOf(
                HAConfigEntry("met", "season", title = "Meteorological seasons"),
                HAConfigEntry("astro", "season", title = "Astronomical seasons")
            )
        )

        assertEquals("sensor.moon_phase", result.moonEntityId)
        assertEquals("sensor.astronomical_season", result.seasonEntityId)
        assertEquals("sensor.us_air_quality_index", result.aqiEntityId)
        assertEquals(true, result.seasonIsExplicitlyAstronomical)
    }

    private fun aqi(entityId: String, state: String) = HAEntity(
        entityId,
        state,
        buildJsonObject { put("device_class", JsonPrimitive("aqi")) }
    )
}

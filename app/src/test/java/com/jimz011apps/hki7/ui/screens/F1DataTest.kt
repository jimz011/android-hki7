package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The f1_sensor payloads are third-party and season-dependent, so these tests pin the shapes the
 * widget relies on and prove a missing or malformed field degrades rather than throws.
 */
class F1DataTest {

    private fun entity(id: String, state: String, attrs: String = "{}") = HAEntity(
        entity_id = id,
        state = state,
        attributes = Json.parseToJsonElement(attrs) as JsonObject
    )

    private fun registryEntry(
        id: String,
        platform: String?,
        key: String?,
        device: String? = "dev1",
        disabled: String? = null
    ) = HAEntityRegistryEntry(
        entity_id = id,
        platform = platform,
        translation_key = key,
        device_id = device,
        disabled_by = disabled
    )

    // ── Discovery ────────────────────────────────────────────────────────────

    @Test
    fun `finds f1 entities by translation key regardless of entity id`() {
        // Localized naming means the entity id can be anything at all.
        val registry = listOf(
            registryEntry("sensor.formule_1_volgende_race", "f1_sensor", "next_race"),
            registryEntry("sensor.f1_coureursklassement", "f1_sensor", "driver_standings"),
            registryEntry("sensor.something_else", "weather", "next_race")
        )
        val found = findF1Entities(registry)

        assertEquals("sensor.formule_1_volgende_race", found[F1Keys.NEXT_RACE])
        assertEquals("sensor.f1_coureursklassement", found[F1Keys.DRIVER_STANDINGS])
        assertEquals(2, found.size)
    }

    @Test
    fun `ignores disabled and unrelated entities`() {
        val registry = listOf(
            registryEntry("sensor.a", "f1_sensor", "next_race", disabled = "user"),
            registryEntry("sensor.b", "f1_sensor", "not_a_key_we_use"),
            registryEntry("sensor.c", "other", "driver_standings")
        )
        assertTrue(findF1Entities(registry).isEmpty())
    }

    /**
     * The widget no longer asks which config entry to use — it reads everything F1 Sensor publishes,
     * so a second entry contributes any key the first one did not have rather than replacing it.
     */
    @Test
    fun `several config entries merge into one view of the data`() {
        val registry = listOf(
            registryEntry("sensor.a", "f1_sensor", "next_race", device = "dev1"),
            registryEntry("sensor.b", "f1_sensor", "next_race", device = "dev2"),
            registryEntry("sensor.c", "f1_sensor", "driver_standings", device = "dev2")
        )
        val found = findF1Entities(registry)

        // First match per key wins; the second entry still contributes what the first lacked.
        assertEquals("sensor.a", found[F1Keys.NEXT_RACE])
        assertEquals("sensor.c", found[F1Keys.DRIVER_STANDINGS])
    }

    // ── Next race ────────────────────────────────────────────────────────────

    @Test
    fun `parses the next race with its session schedule in weekend order`() {
        val race = parseNextRace(
            entity(
                "sensor.next_race", "2026-03-08T15:00:00+00:00",
                """
                {
                  "race_name": "Bahrain Grand Prix",
                  "round": "1", "season": "2026",
                  "circuit_name": "Bahrain International Circuit",
                  "circuit_locality": "Sakhir", "circuit_country": "Bahrain",
                  "country_flag_url": "https://flags/bh.png",
                  "first_practice_start_utc": "2026-03-06T11:30:00+00:00",
                  "third_practice_start_utc": "2026-03-07T12:30:00+00:00",
                  "qualifying_start_utc": "2026-03-07T16:00:00+00:00",
                  "race_start_utc": "2026-03-08T15:00:00+00:00"
                }
                """.trimIndent()
            )
        )

        requireNotNull(race)
        assertEquals("Bahrain Grand Prix", race.raceName)
        assertEquals("Sakhir", race.locality)
        // Second practice is absent (a sprint weekend, or simply not published yet) and is skipped,
        // but the rest must stay in running order rather than attribute order.
        assertEquals(
            listOf("first_practice", "third_practice", "qualifying", "race"),
            race.sessions.map { it.id }
        )
        assertEquals(2026, race.raceStart?.year)
    }

    @Test
    fun `counts down only until the race has started`() {
        val race = parseNextRace(
            entity("sensor.next_race", "2026-03-08T15:00:00+00:00")
        )
        requireNotNull(race)
        val before = java.time.ZonedDateTime.parse("2026-03-07T15:00:00+00:00")
        val after = java.time.ZonedDateTime.parse("2026-03-08T16:00:00+00:00")

        assertEquals(24L, race.timeUntil(before)?.toHours())
        assertNull(race.timeUntil(after))
    }

    @Test
    fun `survives a next race sensor with no attributes at all`() {
        val race = parseNextRace(entity("sensor.next_race", "unknown"))
        requireNotNull(race)
        assertNull(race.raceName)
        assertNull(race.raceStart)
        assertTrue(race.sessions.isEmpty())
    }

    // ── Standings ────────────────────────────────────────────────────────────

    @Test
    fun `parses driver standings including the nested driver and constructor`() {
        val standings = parseDriverStandings(
            entity(
                "sensor.driver_standings", "20",
                """
                {"driver_standings": [
                  {"position": "1", "points": "575", "wins": "19",
                   "Driver": {"givenName": "Max", "familyName": "Verstappen",
                              "code": "VER", "permanentNumber": "1"},
                   "Constructors": [{"name": "Red Bull"}]}
                ]}
                """.trimIndent()
            )
        )

        val first = standings.single()
        assertEquals("1", first.position)
        assertEquals("Max Verstappen", first.driverName)
        assertEquals("VER", first.driverCode)
        assertEquals("Red Bull", first.constructor)
        assertEquals("575", first.points)
    }

    @Test
    fun `skips malformed standing rows instead of failing the list`() {
        val standings = parseDriverStandings(
            entity(
                "sensor.driver_standings", "2",
                """{"driver_standings": ["not an object", {"position": "2"}]}"""
            )
        )
        assertEquals(1, standings.size)
        assertEquals("2", standings.single().position)
        assertNull(standings.single().driverName)
    }

    @Test
    fun `parses constructor standings`() {
        val teams = parseConstructorStandings(
            entity(
                "sensor.constructor_standings", "10",
                """
                {"constructor_standings": [
                  {"position": "1", "points": "860", "wins": "21",
                   "Constructor": {"name": "Red Bull", "nationality": "Austrian"}}
                ]}
                """.trimIndent()
            )
        )
        assertEquals("Red Bull", teams.single().name)
        assertEquals("Austrian", teams.single().nationality)
    }

    @Test
    fun `returns an empty list when the standings attribute is missing`() {
        assertTrue(parseDriverStandings(entity("sensor.x", "0")).isEmpty())
        assertTrue(parseDriverStandings(null).isEmpty())
    }

    // ── Results ──────────────────────────────────────────────────────────────

    @Test
    fun `parses last race results with lowercase nested keys`() {
        val race = parseLastRace(
            entity(
                "sensor.last_race", "Bahrain Grand Prix",
                """
                {"race_name": "Bahrain Grand Prix", "round": "1", "season": "2026",
                 "circuit_name": "Bahrain International Circuit",
                 "results": [
                   {"position": "1", "grid": "3", "points": "25", "status": "Finished",
                    "driver": {"givenName": "Max", "familyName": "Verstappen", "code": "VER"},
                    "constructor": {"name": "Red Bull"}}
                 ]}
                """.trimIndent()
            )
        )

        requireNotNull(race)
        assertEquals("Bahrain Grand Prix", race.raceName)
        val row = race.results.single()
        assertEquals("Max Verstappen", row.driverName)
        assertEquals("Red Bull", row.constructor)
        // Started 3rd, finished 1st: two places gained.
        assertEquals(2, row.gridDelta)
    }

    @Test
    fun `grid delta is negative when places were lost and absent for a pit lane start`() {
        val lost = F1RaceResult("5", "2", "10", "Finished", "A", "AAA", "T")
        assertEquals(-3, lost.gridDelta)

        val pitLane = F1RaceResult("8", "0", "4", "Finished", "B", "BBB", "T")
        assertNull(pitLane.gridDelta)

        val retired = F1RaceResult(null, "4", "0", "Engine", "C", "CCC", "T")
        assertNull(retired.gridDelta)
    }

    @Test
    fun `last race is null when there is nothing to show`() {
        assertNull(parseLastRace(entity("sensor.last_race", "unknown")))
        assertNull(parseLastRace(null))
    }

    // ── Weather ──────────────────────────────────────────────────────────────

    @Test
    fun `reads weather from the state with attribute fallbacks`() {
        val weather = parseWeather(
            entity(
                "sensor.weather", "24.5",
                """{"precipitation_probability": 30, "wind_speed": 12.0, "humidity": 55,
                    "temperature_unit": "°C", "wind_speed_unit": "km/h"}"""
            )
        )
        requireNotNull(weather)
        assertEquals(24.5, weather.temperature!!, 0.001)
        assertEquals(30.0, weather.precipitationProbability!!, 0.001)
        assertEquals("°C", weather.temperatureUnit)
    }

    @Test
    fun `weather is null when the sensor carries no usable numbers`() {
        assertNull(parseWeather(entity("sensor.weather", "unknown")))
    }

    // ── Track flag ───────────────────────────────────────────────────────────

    @Test
    fun `maps track status wording and codes to flags`() {
        // The live feed writes CamelCase without separators; the integration writes spaced words.
        assertEquals(F1Flag.GREEN, parseTrackFlag(entity("s", "AllClear")))
        assertEquals(F1Flag.GREEN, parseTrackFlag(entity("s", "All Clear")))
        assertEquals(F1Flag.GREEN, parseTrackFlag(entity("s", "1")))
        assertEquals(F1Flag.SAFETY_CAR, parseTrackFlag(entity("s", "SCDeployed")))
        assertEquals(F1Flag.VIRTUAL_SAFETY_CAR, parseTrackFlag(entity("s", "VSCDeployed")))
        assertEquals(F1Flag.YELLOW, parseTrackFlag(entity("s", "Yellow")))
        assertEquals(F1Flag.RED, parseTrackFlag(entity("s", "Red")))
        assertEquals(F1Flag.SAFETY_CAR, parseTrackFlag(entity("s", "Safety Car")))
        // "Virtual safety car" also contains "safety car", so ordering of the checks matters.
        assertEquals(F1Flag.VIRTUAL_SAFETY_CAR, parseTrackFlag(entity("s", "Virtual Safety Car")))
        assertEquals(F1Flag.CHEQUERED, parseTrackFlag(entity("s", "Chequered")))
        assertEquals(F1Flag.UNKNOWN, parseTrackFlag(entity("s", "something new")))
        assertEquals(F1Flag.UNKNOWN, parseTrackFlag(null))
    }

    // ── Race control ─────────────────────────────────────────────────────────

    @Test
    fun `parses race control newest first and accepts either attribute key`() {
        val viaMessages = parseRaceControl(
            entity(
                "sensor.rc", "3",
                """{"messages": [
                     {"message": "GREEN LIGHT", "category": "Flag", "lap": "1"},
                     {"message": "CAR 4 UNDER INVESTIGATION", "category": "Other", "lap": "12"}
                   ]}"""
            )
        )
        assertEquals("CAR 4 UNDER INVESTIGATION", viaMessages.first().message)
        assertEquals("12", viaMessages.first().lap)

        val viaLegacyKey = parseRaceControl(
            entity("sensor.rc", "1", """{"race_control_messages": [{"Message": "YELLOW"}]}""")
        )
        assertEquals("YELLOW", viaLegacyKey.single().message)
    }

    @Test
    fun `race control tolerates rows without a message`() {
        val messages = parseRaceControl(
            entity("sensor.rc", "2", """{"messages": [{"category": "Flag"}, {"message": "OK"}]}""")
        )
        assertEquals(listOf("OK"), messages.map { it.message })
    }

    // ── Timestamps ───────────────────────────────────────────────────────────

    @Test
    fun `parses offset and zoned timestamps and rejects junk`() {
        assertEquals(2026, parseF1Instant("2026-03-08T15:00:00+00:00")?.year)
        assertEquals(2026, parseF1Instant("2026-03-08T15:00:00Z[UTC]")?.year)
        assertNull(parseF1Instant("not a date"))
        assertNull(parseF1Instant(""))
        assertNull(parseF1Instant(null))
    }

    // ── Season calendar ──────────────────────────────────────────────────────

    @Test
    fun `reads the season year from the season attribute, not the race-count state`() {
        // sensor.f1_current_season's state is the race COUNT, e.g. "24" - the year lives in the
        // "season" attribute instead, and the dialog title badge must show the latter.
        val season = parseCurrentSeason(
            entity(
                "sensor.current_season", "24",
                """
                {"season": "2026", "races": [
                  {"round": "1", "raceName": "Bahrain Grand Prix",
                   "date": "2026-03-08", "time": "15:00:00Z",
                   "Circuit": {"circuitName": "Bahrain International Circuit",
                               "Location": {"locality": "Sakhir", "country": "Bahrain"}},
                   "country_flag_url": "https://flags/bh.png"}
                ]}
                """.trimIndent()
            )
        )
        requireNotNull(season)
        assertEquals("2026", season.year)
        val race = season.races.single()
        assertEquals("Bahrain Grand Prix", race.raceName)
        assertEquals("Sakhir", race.locality)
        assertEquals("Bahrain International Circuit", race.circuitName)
        assertEquals(2026, race.raceStart?.year)
    }

    @Test
    fun `season is null with nothing to show and tolerates malformed race rows`() {
        assertNull(parseCurrentSeason(entity("sensor.x", "0")))
        assertNull(parseCurrentSeason(null))

        val season = parseCurrentSeason(
            entity("sensor.x", "1", """{"season": "2026", "races": ["not an object"]}""")
        )
        requireNotNull(season)
        assertTrue(season.races.isEmpty())
    }

    // ── Starting grid ────────────────────────────────────────────────────────

    @Test
    fun `parses a confirmed starting grid with the qualifying-to-grid delta`() {
        val grid = parseStartingGrid(
            entity(
                "sensor.starting_grid", "confirmed",
                """
                {"grid_context": "race", "target_session_name": "Race",
                 "grid": [
                   {"grid_position": 1, "qualifying_position": 3, "racing_number": "1",
                    "tla": "VER", "driver_name": "Max Verstappen", "team_name": "Red Bull",
                    "grid_delta": 2, "changed_from_qualifying": true}
                 ]}
                """.trimIndent()
            )
        )
        requireNotNull(grid)
        assertEquals("confirmed", grid.status)
        assertEquals("race", grid.context)
        val row = grid.grid.single()
        assertEquals(1, row.gridPosition)
        assertEquals(3, row.qualifyingPosition)
        assertEquals(2, row.gridDelta)
        assertEquals(true, row.changedFromQualifying)
    }

    @Test
    fun `starting grid is empty while waiting for qualifying, not null`() {
        val grid = parseStartingGrid(entity("sensor.starting_grid", "waiting_for_qualifying"))
        requireNotNull(grid)
        assertEquals("waiting_for_qualifying", grid.status)
        assertTrue(grid.grid.isEmpty())
    }

    // ── Championship prediction ──────────────────────────────────────────────

    @Test
    fun `parses a plausible championship prediction payload`() {
        val rows = parseChampionshipPrediction(
            entity(
                "sensor.prediction", "20",
                """
                {"predictions": [
                  {"position": "1", "driver_name": "Max Verstappen",
                   "current_points": "575", "predicted_points": "620"}
                ]}
                """.trimIndent()
            )
        )
        val row = rows.single()
        assertEquals("Max Verstappen", row.name)
        assertEquals("575", row.currentPoints)
        assertEquals("620", row.predictedPoints)
    }

    @Test
    fun `unrecognized prediction payloads degrade to an empty list rather than throwing`() {
        assertTrue(parseChampionshipPrediction(entity("sensor.x", "0")).isEmpty())
        assertTrue(parseChampionshipPrediction(null).isEmpty())
        assertTrue(
            parseChampionshipPrediction(entity("sensor.x", "1", """{"weird_key": []}""")).isEmpty()
        )
    }

    // ── Live timing ──────────────────────────────────────────────────────────

    @Test
    fun `joins live driver positions with tyre data by racing number`() {
        val positions = entity(
            "sensor.driver_positions", "12",
            """{"total_laps": 57, "drivers": [
                 {"racing_number": "1", "tla": "VER", "name": "Max Verstappen",
                  "current_position": "1", "gap_to_leader": "", "status": "on_track"},
                 {"racing_number": "44", "tla": "HAM", "name": "Lewis Hamilton",
                  "current_position": "2", "gap_to_leader": "+3.201", "status": "pit_in"}
               ]}"""
        )
        val tyres = entity(
            "sensor.current_tyres", "2",
            """{"drivers": [
                 {"racing_number": "1", "compound_short": "M", "stint_laps": 15},
                 {"racing_number": "44", "compound_short": "H", "stint_laps": 3}
               ]}"""
        )
        val drivers = parseLiveDrivers(positions, tyres)
        assertEquals(listOf("1", "2"), drivers.map { it.position })
        assertEquals("M", drivers[0].tyreCompound)
        assertEquals(15, drivers[0].tyreStintLaps)
        assertEquals("pit_in", drivers[1].status)

        val lapCount = parseLapCount(positions)
        requireNotNull(lapCount)
        assertEquals(12, lapCount.currentLap)
        assertEquals(57, lapCount.totalLaps)
    }

    @Test
    fun `live timing degrades gracefully with no tyre entity or no drivers`() {
        val positions = entity("sensor.driver_positions", "1", """{"drivers": [{"racing_number": "1"}]}""")
        val drivers = parseLiveDrivers(positions, null)
        assertEquals(1, drivers.size)
        assertNull(drivers.single().tyreCompound)

        assertTrue(parseLiveDrivers(null, null).isEmpty())
        assertNull(parseLapCount(null))
    }
}

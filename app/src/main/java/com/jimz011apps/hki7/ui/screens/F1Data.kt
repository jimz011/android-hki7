@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * Reading the `f1_sensor` integration (github.com/Nicxe/f1_sensor).
 *
 * Kept free of Compose so the parsing can be unit tested on the JVM: these are third-party payloads
 * that change between seasons and between the live and historical APIs, and every field here is
 * optional in practice.
 */
const val F1_PLATFORM = "f1_sensor"

/** Stable, integration-defined keys. Entity ids are not usable — see [com.jimz011apps.hki7.data.HKIF1Widget]. */
object F1Keys {
    const val NEXT_RACE = "next_race"
    const val DRIVER_STANDINGS = "driver_standings"
    const val CONSTRUCTOR_STANDINGS = "constructor_standings"
    const val LAST_RACE = "last_race_results"
    const val SEASON_RESULTS = "season_results"
    const val WEATHER = "weather"
    const val TRACK_STATUS = "track_status"
    const val SESSION_STATUS = "session_status"
    const val RACE_CONTROL = "race_control"
    const val CURRENT_SEASON = "current_season"
    const val STARTING_GRID = "starting_grid"
    const val CHAMPIONSHIP_PREDICTION_DRIVERS = "championship_prediction_drivers"
    const val CHAMPIONSHIP_PREDICTION_TEAMS = "championship_prediction_teams"
    const val DRIVER_POSITIONS = "driver_positions"
    const val CURRENT_TYRES = "current_tyres"

    /** Everything the widget reads; used to keep the state subscription as narrow as possible. */
    val ALL = listOf(
        NEXT_RACE, DRIVER_STANDINGS, CONSTRUCTOR_STANDINGS, LAST_RACE, SEASON_RESULTS,
        WEATHER, TRACK_STATUS, SESSION_STATUS, RACE_CONTROL, CURRENT_SEASON,
        STARTING_GRID, CHAMPIONSHIP_PREDICTION_DRIVERS, CHAMPIONSHIP_PREDICTION_TEAMS,
        DRIVER_POSITIONS, CURRENT_TYRES
    )
}

/**
 * Maps `translation_key -> entity_id` for the F1 integration.
 *
 * Every enabled F1 entity is considered, whichever config entry owns it: the widget shows all the
 * data there is, so narrowing to one entry would only hide some of it. With more than one entry the
 * first match for a given key wins and the others fill in keys it did not provide.
 */
fun findF1Entities(registry: List<HAEntityRegistryEntry>): Map<String, String> = buildMap {
    registry.asSequence()
        .filter { it.platform == F1_PLATFORM && it.disabled_by == null }
        .forEach { entry ->
            val key = entry.translation_key ?: return@forEach
            if (key in F1Keys.ALL) putIfAbsent(key, entry.entity_id)
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Attribute helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun HAEntity.attr(key: String): JsonObject? =
    attributes?.get(key)?.let { runCatching { it.jsonObject }.getOrNull() }

internal fun HAEntity.str(key: String): String? =
    attributes?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
        ?.takeIf { it.isNotBlank() && it != "null" }

internal fun HAEntity.num(key: String): Double? =
    attributes?.get(key)?.let { runCatching { it.jsonPrimitive.doubleOrNull }.getOrNull() }

internal fun HAEntity.arr(key: String): JsonArray? =
    attributes?.get(key)?.let { runCatching { it.jsonArray }.getOrNull() }

private fun JsonObject.text(vararg path: String): String? {
    var current: JsonObject? = this
    path.dropLast(1).forEach { step ->
        current = current?.get(step)?.let { runCatching { it.jsonObject }.getOrNull() }
    }
    return current?.get(path.last())
        ?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
        ?.takeIf { it.isNotBlank() && it != "null" }
}

private fun JsonObject.number(vararg path: String): Double? {
    var current: JsonObject? = this
    path.dropLast(1).forEach { step ->
        current = current?.get(step)?.let { runCatching { it.jsonObject }.getOrNull() }
    }
    return current?.get(path.last())?.let { runCatching { it.jsonPrimitive.doubleOrNull }.getOrNull() }
}

private fun JsonObject.boolean(vararg path: String): Boolean? {
    var current: JsonObject? = this
    path.dropLast(1).forEach { step ->
        current = current?.get(step)?.let { runCatching { it.jsonObject }.getOrNull() }
    }
    return current?.get(path.last())?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() }
}

private fun JsonObject.firstArrayText(key: String, field: String): String? =
    get(key)?.let { runCatching { it.jsonArray }.getOrNull() }
        ?.firstOrNull()
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?.text(field)

/**
 * Parses an ISO timestamp, tolerating both offset and zoned forms. The integration emits UTC
 * instants for `*_utc` attributes and already-localized strings for `*_local`, so only the former
 * are safe to parse as instants.
 */
internal fun parseF1Instant(raw: String?): ZonedDateTime? {
    val value = raw?.takeIf { it.isNotBlank() } ?: return null
    runCatching { return OffsetDateTime.parse(value).toZonedDateTime() }
    runCatching { return ZonedDateTime.parse(value) }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
// Parsed views
// ─────────────────────────────────────────────────────────────────────────────

data class F1Session(
    /** Stable id: "race", "qualifying", "sprint", "first_practice", … */
    val id: String,
    val startsAt: ZonedDateTime?
)

data class F1NextRace(
    val raceName: String?,
    val round: String?,
    val season: String?,
    val circuitName: String?,
    val locality: String?,
    val country: String?,
    val countryFlagUrl: String?,
    val circuitOutlineUrl: String?,
    val circuitMapUrl: String?,
    val raceStart: ZonedDateTime?,
    val sessions: List<F1Session>
) {
    /** Time until lights out, or null once it has started. */
    fun timeUntil(now: ZonedDateTime): Duration? {
        val start = raceStart ?: return null
        val d = Duration.between(now, start)
        return if (d.isNegative) null else d
    }
}

/** The session-start attributes, in the order a race weekend actually runs. */
private val SESSION_ORDER = listOf(
    "first_practice", "second_practice", "third_practice",
    "sprint_qualifying", "sprint", "qualifying", "race"
)

fun parseNextRace(entity: HAEntity?): F1NextRace? {
    val e = entity ?: return null
    val sessions = SESSION_ORDER.mapNotNull { id ->
        // `<session>_start_utc`, except the race whose attribute is `race_start_utc`.
        val instant = parseF1Instant(e.str("${id}_start_utc"))
        if (instant == null) null else F1Session(id, instant)
    }
    // The sensor's own state is the race start as a timestamp; fall back to the attribute.
    val raceStart = parseF1Instant(e.state) ?: parseF1Instant(e.str("race_start_utc"))
    return F1NextRace(
        raceName = e.str("race_name"),
        round = e.str("round"),
        season = e.str("season"),
        circuitName = e.str("circuit_name"),
        locality = e.str("circuit_locality"),
        country = e.str("circuit_country"),
        countryFlagUrl = e.str("country_flag_url"),
        circuitOutlineUrl = e.str("circuit_outline_url"),
        circuitMapUrl = e.str("circuit_map_url"),
        raceStart = raceStart,
        sessions = sessions
    )
}

data class F1DriverStanding(
    val position: String?,
    val points: String?,
    val wins: String?,
    val driverName: String?,
    val driverCode: String?,
    val number: String?,
    val constructor: String?
)

fun parseDriverStandings(entity: HAEntity?): List<F1DriverStanding> {
    val rows = entity?.arr(F1Keys.DRIVER_STANDINGS) ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val given = row.text("Driver", "givenName")
        val family = row.text("Driver", "familyName")
        F1DriverStanding(
            position = row.text("position"),
            points = row.text("points"),
            wins = row.text("wins"),
            driverName = listOfNotNull(given, family).joinToString(" ").takeIf { it.isNotBlank() },
            driverCode = row.text("Driver", "code"),
            number = row.text("Driver", "permanentNumber"),
            constructor = row.firstArrayText("Constructors", "name")
        )
    }
}

data class F1ConstructorStanding(
    val position: String?,
    val points: String?,
    val wins: String?,
    val name: String?,
    val nationality: String?
)

fun parseConstructorStandings(entity: HAEntity?): List<F1ConstructorStanding> {
    val rows = entity?.arr(F1Keys.CONSTRUCTOR_STANDINGS) ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        F1ConstructorStanding(
            position = row.text("position"),
            points = row.text("points"),
            wins = row.text("wins"),
            name = row.text("Constructor", "name"),
            nationality = row.text("Constructor", "nationality")
        )
    }
}

data class F1RaceResult(
    val position: String?,
    val grid: String?,
    val points: String?,
    val status: String?,
    val driverName: String?,
    val driverCode: String?,
    val constructor: String?
) {
    /** Positions gained (+) or lost (−) versus the starting grid, when both are known. */
    val gridDelta: Int?
        get() {
            val start = grid?.toIntOrNull() ?: return null
            val end = position?.toIntOrNull() ?: return null
            // A grid slot of 0 means a pit lane start, which no delta describes usefully.
            if (start == 0) return null
            return start - end
        }
}

data class F1LastRace(
    val raceName: String?,
    val round: String?,
    val season: String?,
    val circuitName: String?,
    val results: List<F1RaceResult>
)

fun parseLastRace(entity: HAEntity?): F1LastRace? {
    val e = entity ?: return null
    val results = e.arr("results").orEmpty().mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val given = row.text("driver", "givenName")
        val family = row.text("driver", "familyName")
        F1RaceResult(
            position = row.text("position"),
            grid = row.text("grid"),
            points = row.text("points"),
            status = row.text("status"),
            driverName = listOfNotNull(given, family).joinToString(" ").takeIf { it.isNotBlank() },
            driverCode = row.text("driver", "code"),
            constructor = row.text("constructor", "name")
        )
    }
    if (results.isEmpty() && e.str("race_name") == null) return null
    return F1LastRace(
        raceName = e.str("race_name"),
        round = e.str("round"),
        season = e.str("season"),
        circuitName = e.str("circuit_name"),
        results = results
    )
}

data class F1Weather(
    val temperature: Double?,
    val temperatureUnit: String?,
    val precipitationProbability: Double?,
    val windSpeed: Double?,
    val windSpeedUnit: String?,
    val humidity: Double?
)

fun parseWeather(entity: HAEntity?): F1Weather? {
    val e = entity ?: return null
    val temp = e.state.toDoubleOrNull() ?: e.num("temperature")
    val hasAny = temp != null || e.num("precipitation_probability") != null ||
        e.num("wind_speed") != null || e.num("humidity") != null
    if (!hasAny) return null
    return F1Weather(
        temperature = temp,
        temperatureUnit = e.str("temperature_unit") ?: e.str("unit_of_measurement"),
        precipitationProbability = e.num("precipitation_probability"),
        windSpeed = e.num("wind_speed"),
        windSpeedUnit = e.str("wind_speed_unit"),
        humidity = e.num("humidity")
    )
}

/**
 * Track status maps to the flag being shown. The integration reports both a numeric code and a
 * message; the code is the reliable half, since the message wording varies by feed.
 */
enum class F1Flag { GREEN, YELLOW, RED, SAFETY_CAR, VIRTUAL_SAFETY_CAR, CHEQUERED, UNKNOWN }

fun parseTrackFlag(entity: HAEntity?): F1Flag {
    val state = entity?.state?.trim()?.lowercase() ?: return F1Flag.UNKNOWN
    // The live feed uses CamelCase with no separators ("AllClear", "SCDeployed") while the
    // integration's own wording is spaced ("Safety Car"). Collapsing whitespace lets one set of
    // checks cover both instead of silently falling through to UNKNOWN.
    val raw = state.replace(" ", "").replace("_", "").replace("-", "")
    return when {
        raw.contains("allclear") || raw == "1" || raw == "green" -> F1Flag.GREEN
        // Must precede the plain safety-car check: "virtualsafetycar" contains "safetycar".
        raw.contains("virtual") || raw.startsWith("vsc") || raw == "6" || raw == "7" -> F1Flag.VIRTUAL_SAFETY_CAR
        raw.contains("safetycar") || raw.startsWith("sc") || raw == "4" -> F1Flag.SAFETY_CAR
        // Before the red check: "chequered" contains "red", so testing red first swallows it.
        raw.contains("chequered") || raw.contains("checkered") -> F1Flag.CHEQUERED
        raw.contains("red") || raw == "5" -> F1Flag.RED
        raw.contains("yellow") || raw == "2" -> F1Flag.YELLOW
        else -> F1Flag.UNKNOWN
    }
}

/** A race-control message, newest first. */
data class F1RaceControlMessage(val message: String, val category: String?, val lap: String?)

fun parseRaceControl(entity: HAEntity?): List<F1RaceControlMessage> {
    val e = entity ?: return emptyList()
    // The integration has used both keys across versions; accept either rather than showing nothing.
    val rows = e.arr("messages") ?: e.arr("race_control_messages") ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val text = row.text("message") ?: row.text("Message") ?: return@mapNotNull null
        F1RaceControlMessage(
            message = text,
            category = row.text("category") ?: row.text("Category"),
            lap = row.text("lap") ?: row.text("Lap")
        )
    }.reversed()
}

// ── Season calendar ─────────────────────────────────────────────────────────

data class F1CalendarRace(
    val round: String?,
    val raceName: String?,
    val circuitName: String?,
    val locality: String?,
    val country: String?,
    val countryFlagUrl: String?,
    val raceStart: ZonedDateTime?
)

/** [year] is the championship year; [CURRENT_SEASON]'s own state is the race *count*, not this. */
data class F1Season(val year: String?, val races: List<F1CalendarRace>)

fun parseCurrentSeason(entity: HAEntity?): F1Season? {
    val e = entity ?: return null
    val races = e.arr("races").orEmpty().mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val date = row.text("date")
        val time = row.text("time")
        val instant = parseF1Instant(
            if (date != null && time != null) "${date}T$time" else date
        )
        F1CalendarRace(
            round = row.text("round"),
            raceName = row.text("raceName"),
            circuitName = row.text("Circuit", "circuitName"),
            locality = row.text("Circuit", "Location", "locality"),
            country = row.text("Circuit", "Location", "country"),
            countryFlagUrl = row.text("country_flag_url"),
            raceStart = instant
        )
    }
    val year = e.str("season")
    if (year == null && races.isEmpty()) return null
    return F1Season(year = year, races = races)
}

// ── Starting grid ────────────────────────────────────────────────────────────

data class F1GridRow(
    val gridPosition: Int?,
    val qualifyingPosition: Int?,
    val number: String?,
    val tla: String?,
    val driverName: String?,
    val teamName: String?,
    val gridDelta: Int?,
    val changedFromQualifying: Boolean?
)

data class F1StartingGrid(
    val status: String?,
    val context: String?,
    val targetSessionName: String?,
    val grid: List<F1GridRow>
)

fun parseStartingGrid(entity: HAEntity?): F1StartingGrid? {
    val e = entity ?: return null
    val rows = e.arr("grid").orEmpty().mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        F1GridRow(
            gridPosition = row.number("grid_position")?.toInt(),
            qualifyingPosition = row.number("qualifying_position")?.toInt(),
            number = row.text("racing_number"),
            tla = row.text("tla"),
            driverName = row.text("driver_name"),
            teamName = row.text("team_name"),
            gridDelta = row.number("grid_delta")?.toInt(),
            changedFromQualifying = row.boolean("changed_from_qualifying")
        )
    }
    return F1StartingGrid(
        status = e.state.takeIf { it.isNotBlank() && it != "unknown" },
        context = e.str("grid_context"),
        targetSessionName = e.str("target_session_name"),
        grid = rows
    )
}

// ── Championship prediction ──────────────────────────────────────────────────

/**
 * Best-effort: the integration does not publish an attribute schema for these entities anywhere
 * in its docs, and the feature itself needs optional F1TV Auth, so most installs never populate
 * it. Every field below tries a few plausible names and degrades to null/empty rather than
 * throwing — this may need a follow-up fix once seen against a real payload.
 */
data class F1PredictionRow(
    val position: String?,
    val name: String?,
    val currentPoints: String?,
    val predictedPoints: String?
)

fun parseChampionshipPrediction(entity: HAEntity?): List<F1PredictionRow> {
    val e = entity ?: return emptyList()
    val rows = e.arr("predictions") ?: e.arr("drivers") ?: e.arr("teams") ?: e.arr("results")
        ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val given = row.text("Driver", "givenName")
        val family = row.text("Driver", "familyName")
        val driverName = listOfNotNull(given, family).joinToString(" ").takeIf { it.isNotBlank() }
        val name = driverName
            ?: row.text("driver_name")
            ?: row.text("name")
            ?: row.text("Constructor", "name")
            ?: row.text("team_name")
            ?: row.firstArrayText("Constructors", "name")
        val current = row.text("current_points") ?: row.text("currentPoints") ?: row.text("points")
        val predicted = row.text("predicted_points") ?: row.text("predictedPoints")
            ?: row.text("final_points") ?: row.text("finalPoints")
        if (name == null && predicted == null) return@mapNotNull null
        F1PredictionRow(
            position = row.text("position"),
            name = name,
            currentPoints = current,
            predictedPoints = predicted
        )
    }
}

// ── Live timing ──────────────────────────────────────────────────────────────

data class F1LiveDriver(
    val position: String?,
    val tla: String?,
    val name: String?,
    val gapToLeader: String?,
    val intervalAhead: String?,
    val status: String?,
    val tyreCompound: String?,
    val tyreStintLaps: Int?
)

/** Leader's current lap and the race distance, when the session reports them. */
data class F1LapCount(val currentLap: Int?, val totalLaps: Int?)

fun parseLapCount(positions: HAEntity?): F1LapCount? {
    val e = positions ?: return null
    val current = e.state.toIntOrNull()
    val total = e.num("total_laps")?.toInt()
    if (current == null && total == null) return null
    return F1LapCount(current, total)
}

fun parseLiveDrivers(positions: HAEntity?, tyres: HAEntity?): List<F1LiveDriver> {
    val rows = positions?.arr("drivers") ?: return emptyList()
    val tyresByNumber = tyres?.arr("drivers").orEmpty().mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val number = row.text("racing_number") ?: return@mapNotNull null
        number to row
    }.toMap()
    return rows.mapNotNull { element ->
        val row = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val number = row.text("racing_number")
        val tyre = number?.let { tyresByNumber[it] }
        F1LiveDriver(
            position = row.text("current_position"),
            tla = row.text("tla"),
            name = row.text("name"),
            gapToLeader = row.text("gap_to_leader"),
            intervalAhead = row.text("interval_to_position_ahead"),
            status = row.text("status"),
            tyreCompound = tyre?.text("compound_short"),
            tyreStintLaps = tyre?.number("stint_laps")?.toInt()
        )
    }.sortedBy { it.position?.toIntOrNull() ?: Int.MAX_VALUE }
}

@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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

    /** Everything the widget reads; used to keep the state subscription as narrow as possible. */
    val ALL = listOf(
        NEXT_RACE, DRIVER_STANDINGS, CONSTRUCTOR_STANDINGS, LAST_RACE, SEASON_RESULTS,
        WEATHER, TRACK_STATUS, SESSION_STATUS, RACE_CONTROL, CURRENT_SEASON
    )
}

/**
 * Maps `translation_key -> entity_id` for the F1 integration.
 *
 * [deviceId] narrows to one config entry when several exist; without it the first match per key
 * wins, which is the right answer for the overwhelmingly common single-entry setup.
 */
fun findF1Entities(
    registry: List<HAEntityRegistryEntry>,
    deviceId: String? = null
): Map<String, String> = buildMap {
    registry.asSequence()
        .filter { it.platform == F1_PLATFORM && it.disabled_by == null }
        .filter { deviceId == null || it.device_id == deviceId }
        .forEach { entry ->
            val key = entry.translation_key ?: return@forEach
            if (key in F1Keys.ALL) putIfAbsent(key, entry.entity_id)
        }
}

/** Device ids that own F1 entities, for the settings picker when there is more than one. */
fun f1DeviceIds(registry: List<HAEntityRegistryEntry>): List<String> =
    registry.filter { it.platform == F1_PLATFORM && it.disabled_by == null }
        .mapNotNull { it.device_id }
        .distinct()

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

package com.example.hki7.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Times and positions for one sunrise-to-sunset cycle.
 *
 * Home Assistant exposes the *next* occurrence of each solar event. [calculateSunTimeline]
 * normalizes those values to the daylight cycle nearest [now], including the previous day's
 * events when the sun is already above the horizon or has just set.
 */
data class SunTimeline(
    val now: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val sunset: ZonedDateTime,
    val dawn: ZonedDateTime?,
    val solarNoon: ZonedDateTime,
    val dusk: ZonedDateTime?,
    val currentPhase: Double,
    val dawnPhase: Double?,
    val solarNoonPhase: Double,
    val duskPhase: Double?,
    val windowStartPhase: Double,
    val windowEndPhase: Double,
    val isAboveHorizon: Boolean,
    val azimuth: Double?,
    val elevation: Double?
) {
    /** Maps a solar phase to the card's extended night/day/night horizontal axis. */
    fun horizontalProgress(phase: Double): Float {
        val span = (windowEndPhase - windowStartPhase).coerceAtLeast(0.0001)
        return ((phase - windowStartPhase) / span).coerceIn(0.0, 1.0).toFloat()
    }

    val sunriseProgress: Float get() = horizontalProgress(0.0)
    val sunsetProgress: Float get() = horizontalProgress(1.0)
    val sunProgress: Float get() = horizontalProgress(currentPhase)
}

/**
 * Builds a deterministic timeline from the actual `sun` entity attributes emitted by Home
 * Assistant. The function has no Compose or clock dependency so it can be unit-tested directly.
 */
fun calculateSunTimeline(sun: HAEntity?, now: ZonedDateTime): SunTimeline? {
    if (sun == null) return null
    val zone = now.zone
    val nextRise = sun.sunEvent("next_rising", zone) ?: return null
    val nextSet = sun.sunEvent("next_setting", zone) ?: return null
    val cycles = buildCandidateCycles(nextRise, nextSet, zone)
    if (cycles.isEmpty()) return null

    val nowInstant = now.toInstant()
    val isAbove = sun.state.equals("above_horizon", ignoreCase = true)
    val rising = sun.attributes?.get("rising")?.jsonPrimitive?.booleanOrNull
    val cycle = chooseCycle(cycles, nowInstant, isAbove, rising) ?: return null
    val sunrise = cycle.sunrise
    val sunset = cycle.sunset
    val dayMillis = Duration.between(sunrise, sunset).toMillis().coerceAtLeast(1L)

    fun phaseAt(time: ZonedDateTime): Double =
        Duration.between(sunrise, time).toMillis().toDouble() / dayMillis.toDouble()

    val midpoint = sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2))
    val dawn = normalizeEventBefore(
        event = sun.sunEvent("next_dawn", zone),
        boundary = sunrise,
        zone = zone,
        maxDistance = Duration.ofHours(6)
    )
    val dusk = normalizeEventAfter(
        event = sun.sunEvent("next_dusk", zone),
        boundary = sunset,
        zone = zone,
        maxDistance = Duration.ofHours(6)
    )
    val solarNoon = normalizeEventInside(
        event = sun.sunEvent("next_noon", zone),
        start = sunrise,
        end = sunset,
        target = midpoint,
        zone = zone
    ) ?: midpoint

    val currentPhase = phaseAt(now.withZoneSameInstant(zone))
    val dawnPhase = dawn?.let(::phaseAt)
    val noonPhase = phaseAt(solarNoon).coerceIn(0.08, 0.92)
    val duskPhase = dusk?.let(::phaseAt)

    // Keep sunrise and sunset comfortably inside the chart while expanding the relevant night
    // wing far enough to place the current marker accurately before dawn or after dusk.
    var windowStart = min(-0.22, dawnPhase ?: -0.08)
    var windowEnd = max(1.22, duskPhase ?: 1.08)
    if (currentPhase < 0.0) windowStart = min(windowStart, currentPhase - 0.06)
    if (currentPhase > 1.0) windowEnd = max(windowEnd, currentPhase + 0.06)

    return SunTimeline(
        now = now.withZoneSameInstant(zone),
        sunrise = sunrise,
        sunset = sunset,
        dawn = dawn,
        solarNoon = solarNoon,
        dusk = dusk,
        currentPhase = currentPhase,
        dawnPhase = dawnPhase,
        solarNoonPhase = noonPhase,
        duskPhase = duskPhase,
        windowStartPhase = windowStart,
        windowEndPhase = windowEnd,
        isAboveHorizon = isAbove,
        azimuth = sun.attributes?.get("azimuth")?.jsonPrimitive?.doubleOrNull,
        elevation = sun.attributes?.get("elevation")?.jsonPrimitive?.doubleOrNull
    )
}

/** A modern, responsive sunrise/sunset card shared by the weather dialog and weather widgets. */
@Composable
fun HorizonCard(
    sun: HAEntity?,
    use24h: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24
) {
    val appColors = LocalHKIAppColors.current
    val zone = remember { ZoneId.systemDefault() }
    var now by remember(zone) { mutableStateOf(ZonedDateTime.now(zone)) }

    LaunchedEffect(zone) {
        while (currentCoroutineContext().isActive) {
            now = ZonedDateTime.now(zone)
            val millisIntoMinute = System.currentTimeMillis() % 60_000L
            delay((60_000L - millisIntoMinute).coerceAtLeast(250L) + 25L)
        }
    }

    val timeline = remember(sun, now) { calculateSunTimeline(sun, now) }
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(use24h, locale) {
        DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD45A).copy(alpha = 0.12f), appColors.elevated.copy(alpha = 0.96f))
                    )
                )
        ) {
            val compact = maxWidth < 300.dp
            if (timeline == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 156.dp else 196.dp).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sunrise and sunset data unavailable",
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                return@BoxWithConstraints
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 12.dp else 18.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    HorizonTimeLabel(
                        label = "Sunrise",
                        time = timeline.sunrise.format(timeFormatter),
                        alignEnd = false,
                        compact = compact
                    )
                    if (!compact) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFFFD45A).copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFFFD45A),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    HorizonTimeLabel(
                        label = "Sunset",
                        time = timeline.sunset.format(timeFormatter),
                        alignEnd = true,
                        compact = compact
                    )
                }
    
                Spacer(Modifier.height(8.dp))
                HorizonPlot(timeline = timeline, modifier = Modifier.fillMaxWidth().height(if (compact) 96.dp else 126.dp))
    
                if (compact) {
                    Text(
                        text = "Solar noon ${timeline.solarNoon.format(timeFormatter)}",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HorizonEventLabel("Dawn", timeline.dawn?.format(timeFormatter) ?: "--")
                        HorizonEventLabel("Solar noon", timeline.solarNoon.format(timeFormatter))
                        HorizonEventLabel("Dusk", timeline.dusk?.format(timeFormatter) ?: "--", alignEnd = true)
                    }
                }
    
                if (!compact) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizonMetric(
                            label = "Azimuth",
                            value = timeline.azimuth.asDegrees(),
                            modifier = Modifier.weight(1f)
                        )
                        HorizonMetric(
                            label = "Elevation",
                            value = timeline.elevation.asDegrees(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizonPlot(timeline: SunTimeline, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    val dayColor = Color(0xFF9FC5F8)
    val nightColor = Color(0xFF34346D)
    val sunColor = Color(0xFFFFD45A)

    Canvas(modifier = modifier) {
        val horizontalPadding = 5.dp.toPx()
        val plotWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(1f)
        val horizonY = size.height * 0.64f
        val dayAmplitude = size.height * 0.48f
        val nightAmplitude = size.height * 0.22f
        val samples = 180

        fun xFor(phase: Double): Float =
            horizontalPadding + timeline.horizontalProgress(phase) * plotWidth

        fun curveValue(phase: Double): Double = when {
            phase < 0.0 -> {
                val wing = abs(timeline.windowStartPhase).coerceAtLeast(0.01)
                -sin(((-phase / wing).coerceIn(0.0, 1.0)) * PI / 2.0)
            }
            phase > 1.0 -> {
                val wing = (timeline.windowEndPhase - 1.0).coerceAtLeast(0.01)
                -sin((((phase - 1.0) / wing).coerceIn(0.0, 1.0)) * PI / 2.0)
            }
            phase <= timeline.solarNoonPhase -> {
                sin((phase / timeline.solarNoonPhase.coerceAtLeast(0.01)) * PI / 2.0)
            }
            else -> {
                val afterNoon = (phase - timeline.solarNoonPhase) /
                    (1.0 - timeline.solarNoonPhase).coerceAtLeast(0.01)
                sin(PI / 2.0 + afterNoon * PI / 2.0)
            }
        }

        fun yFor(phase: Double): Float {
            val value = curveValue(phase)
            return if (value >= 0.0) {
                horizonY - value.toFloat() * dayAmplitude
            } else {
                horizonY - value.toFloat() * nightAmplitude
            }
        }

        fun sampledPath(from: Double, to: Double, closeAtHorizon: Boolean): Path {
            val path = Path()
            if (closeAtHorizon) path.moveTo(xFor(from), horizonY)
            for (index in 0..samples) {
                val fraction = index / samples.toDouble()
                val phase = from + (to - from) * fraction
                val x = xFor(phase)
                val y = yFor(phase)
                if (index == 0 && !closeAtHorizon) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (closeAtHorizon) {
                path.lineTo(xFor(to), horizonY)
                path.close()
            }
            return path
        }

        // Night wings are intentionally distinct from the travelled daylight fill.
        drawPath(
            path = sampledPath(timeline.windowStartPhase, 0.0, closeAtHorizon = true),
            brush = Brush.verticalGradient(
                colors = listOf(nightColor.copy(alpha = 0.22f), nightColor.copy(alpha = 0.72f)),
                startY = horizonY,
                endY = size.height
            )
        )
        drawPath(
            path = sampledPath(1.0, timeline.windowEndPhase, closeAtHorizon = true),
            brush = Brush.verticalGradient(
                colors = listOf(nightColor.copy(alpha = 0.22f), nightColor.copy(alpha = 0.72f)),
                startY = horizonY,
                endY = size.height
            )
        )

        val filledDayEnd = timeline.currentPhase.coerceIn(0.0, 1.0)
        if (filledDayEnd > 0.001) {
            drawPath(
                path = sampledPath(0.0, filledDayEnd, closeAtHorizon = true),
                brush = Brush.verticalGradient(
                    colors = listOf(dayColor.copy(alpha = 0.82f), dayColor.copy(alpha = 0.28f)),
                    startY = 0f,
                    endY = horizonY
                )
            )
        }

        val fullPath = sampledPath(
            timeline.windowStartPhase,
            timeline.windowEndPhase,
            closeAtHorizon = false
        )
        drawPath(
            path = fullPath,
            color = appColors.onMuted.copy(alpha = 0.34f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = appColors.onMuted.copy(alpha = 0.22f),
            start = Offset(horizontalPadding, horizonY),
            end = Offset(size.width - horizontalPadding, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        fun eventMarker(phase: Double) {
            val x = xFor(phase)
            drawLine(
                color = appColors.onMuted.copy(alpha = 0.23f),
                start = Offset(x, horizonY - 35.dp.toPx()),
                end = Offset(x, horizonY + 9.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        eventMarker(0.0)
        eventMarker(1.0)

        val sunPhase = timeline.currentPhase.coerceIn(
            timeline.windowStartPhase,
            timeline.windowEndPhase
        )
        val sunCenter = Offset(xFor(sunPhase), yFor(sunPhase))
        drawCircle(
            color = sunColor.copy(alpha = if (timeline.isAboveHorizon) 0.18f else 0.08f),
            radius = 16.dp.toPx(),
            center = sunCenter
        )
        drawCircle(
            color = sunColor.copy(alpha = if (timeline.isAboveHorizon) 1f else 0.52f),
            radius = 9.dp.toPx(),
            center = sunCenter
        )
    }
}

@Composable
private fun HorizonTimeLabel(label: String, time: String, alignEnd: Boolean, compact: Boolean) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            color = appColors.onMuted,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
        )
        Text(
            time,
            color = appColors.onSurface,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HorizonEventLabel(label: String, time: String, alignEnd: Boolean = false) {
    val appColors = LocalHKIAppColors.current
    Column(
        horizontalAlignment = when {
            alignEnd -> Alignment.End
            label == "Solar noon" -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    ) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Text(time, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HorizonMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = modifier
            .background(appColors.subtleSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = appColors.onMuted,
            modifier = Modifier.size(17.dp)
        )
        Column {
            Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private data class CandidateCycle(val sunrise: ZonedDateTime, val sunset: ZonedDateTime)

private fun buildCandidateCycles(
    nextRise: ZonedDateTime,
    nextSet: ZonedDateTime,
    zone: ZoneId
): List<CandidateCycle> {
    val rises = shiftedCandidates(nextRise, zone)
    val sets = shiftedCandidates(nextSet, zone)
    return rises.mapNotNull { rise ->
        val set = sets
            .asSequence()
            .filter { it.toInstant().isAfter(rise.toInstant()) }
            .minByOrNull { it.toInstant() }
            ?: return@mapNotNull null
        val length = Duration.between(rise, set)
        if (length > Duration.ofMinutes(30) && length < Duration.ofHours(30)) {
            CandidateCycle(rise, set)
        } else {
            null
        }
    }.distinctBy { it.sunrise.toInstant() to it.sunset.toInstant() }
}

private fun chooseCycle(
    cycles: List<CandidateCycle>,
    now: Instant,
    isAbove: Boolean,
    rising: Boolean?
): CandidateCycle? {
    if (isAbove) {
        cycles.filter { !now.isBefore(it.sunrise.toInstant()) && !now.isAfter(it.sunset.toInstant()) }
            .minByOrNull { boundaryDistanceMillis(it, now) }
            ?.let { return it }
    }

    val previous = cycles
        .filter { !it.sunset.toInstant().isAfter(now) }
        .maxByOrNull { it.sunset.toInstant() }
    val upcoming = cycles
        .filter { !it.sunrise.toInstant().isBefore(now) }
        .minByOrNull { it.sunrise.toInstant() }

    if (!isAbove) {
        when (rising) {
            true -> upcoming?.let { return it }
            false -> previous?.let { return it }
            null -> Unit
        }
    }
    if (previous == null) return upcoming
    if (upcoming == null) return previous
    val sinceSet = abs(Duration.between(previous.sunset.toInstant(), now).toMillis())
    val untilRise = abs(Duration.between(now, upcoming.sunrise.toInstant()).toMillis())
    return if (untilRise <= sinceSet) upcoming else previous
}

private fun boundaryDistanceMillis(cycle: CandidateCycle, now: Instant): Long = min(
    abs(Duration.between(cycle.sunrise.toInstant(), now).toMillis()),
    abs(Duration.between(cycle.sunset.toInstant(), now).toMillis())
)

private fun shiftedCandidates(event: ZonedDateTime, zone: ZoneId): List<ZonedDateTime> {
    val local = event.withZoneSameInstant(zone)
    return (-3L..3L).map { local.plusDays(it) }.distinctBy { it.toInstant() }.sortedBy { it.toInstant() }
}

private fun normalizeEventBefore(
    event: ZonedDateTime?,
    boundary: ZonedDateTime,
    zone: ZoneId,
    maxDistance: Duration
): ZonedDateTime? = event?.let {
    shiftedCandidates(it, zone)
        .filter { candidate ->
            !candidate.toInstant().isAfter(boundary.toInstant()) &&
                Duration.between(candidate, boundary) <= maxDistance
        }
        .maxByOrNull { candidate -> candidate.toInstant() }
}

private fun normalizeEventAfter(
    event: ZonedDateTime?,
    boundary: ZonedDateTime,
    zone: ZoneId,
    maxDistance: Duration
): ZonedDateTime? = event?.let {
    shiftedCandidates(it, zone)
        .filter { candidate ->
            !candidate.toInstant().isBefore(boundary.toInstant()) &&
                Duration.between(boundary, candidate) <= maxDistance
        }
        .minByOrNull { candidate -> candidate.toInstant() }
}

private fun normalizeEventInside(
    event: ZonedDateTime?,
    start: ZonedDateTime,
    end: ZonedDateTime,
    target: ZonedDateTime,
    zone: ZoneId
): ZonedDateTime? = event?.let {
    shiftedCandidates(it, zone)
        .filter { candidate ->
            !candidate.toInstant().isBefore(start.toInstant()) &&
                !candidate.toInstant().isAfter(end.toInstant())
        }
        .minByOrNull { candidate -> abs(Duration.between(candidate, target).toMillis()) }
}

private fun HAEntity.sunEvent(name: String, zone: ZoneId): ZonedDateTime? {
    val raw = attributes?.get(name)?.jsonPrimitive?.contentOrNull ?: return null
    return parseSunEvent(raw, zone)
}

private fun parseSunEvent(value: String, zone: ZoneId): ZonedDateTime? {
    return runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }
        .recoverCatching { Instant.parse(value).atZone(zone) }
        .recoverCatching { ZonedDateTime.parse(value).withZoneSameInstant(zone) }
        .recoverCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).atZone(zone) }
        .getOrNull()
}

private fun Double?.asDegrees(): String = this?.let {
    String.format(Locale.getDefault(), "%.1f\u00B0", it)
} ?: "--"

package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAHistoryEntry
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Selectable history windows, in hours, shared by graphs and history dialogs. */
val HistoryRangeOptions = listOf(6, 12, 24, 48, 72)

/** A single point on a timeline: an absolute time plus a numeric value and the raw state label. */
data class HistoryPoint(val timeMillis: Long, val value: Float, val label: String)

/**
 * Value-hue gradient stops for a sensor's `device_class`, high value first (painted at the top of
 * the plotted range) down to low value last (painted at the bottom). Returns null for domains
 * without a bespoke palette, in which case the graph keeps its plain single-color look.
 */
fun sensorGradientColors(deviceClass: String?): List<Color>? = when (deviceClass) {
    "temperature" -> listOf(Color(0xFFEF5350), Color(0xFF66BB6A))              // hot red -> cool green
    "humidity" -> listOf(Color(0xFF0D47A1), Color(0xFFB3E5FC))                 // humid deep blue -> dry pale blue
    "battery" -> listOf(Color(0xFF66BB6A), Color(0xFFEF5350))                  // full green -> low red
    "illuminance" -> listOf(Color(0xFFFFF176), Color(0xFF37474F))              // bright yellow -> dark
    "carbon_dioxide" -> listOf(Color(0xFFEF5350), Color(0xFF66BB6A))           // high CO2 red -> low green
    "atmospheric_pressure", "pressure" -> listOf(Color(0xFF7E57C2), Color(0xFF5C6BC0))
    "power", "energy" -> listOf(Color(0xFFFF7043), Color(0xFFFFC107))
    else -> null
}

private val activeStates = setOf(
    "on", "open", "opened", "home", "detected", "playing", "active",
    "locked", "heating", "cooling", "cleaning", "running", "wet", "moist", "true"
)
private val inactiveStates = setOf(
    "off", "closed", "not_home", "away", "clear", "idle", "standby", "paused",
    "unlocked", "docked", "off_line", "false", "ok", "normal"
)

private val statePalette = listOf(
    Color(0xFF4A90E2), Color(0xFF50B98E), Color(0xFFE5A23A), Color(0xFFBE73CC),
    Color(0xFFE26A6A), Color(0xFF5FB0C9), Color(0xFFB0B845), Color(0xFFD97FB0)
)

/** Maps an HA state string to a stable color: active -> accent, inactive -> muted, else palette. */
fun colorForHistoryState(state: String, accent: Color, muted: Color): Color {
    val key = state.lowercase().trim()
    return when {
        key in activeStates -> accent
        key in inactiveStates -> muted
        key == "unavailable" || key == "unknown" || key.isBlank() -> muted.copy(alpha = 0.4f)
        else -> statePalette[(abs(key.hashCode())) % statePalette.size]
    }
}

/** Parses an HA timestamp (ISO-8601 with offset, or local) into epoch millis. */
fun parseHistoryMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching {
            LocalDateTime.parse(value.substringBefore("+").substringBefore("."), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        .getOrNull()
}

private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val clockDateFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

fun formatHistoryClock(millis: Long, withDate: Boolean = false): String {
    val zoned = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return zoned.format(if (withDate) clockDateFormatter else clockFormatter)
}

/** "Last seen just now" / "Last seen 36m ago" from an entity's `last_changed` timestamp. */
fun formatLastSeen(lastChanged: String?): String? {
    val millis = parseHistoryMillis(lastChanged) ?: return null
    val diffSeconds = ((System.currentTimeMillis() - millis) / 1000).coerceAtLeast(0)
    val relative = when {
        diffSeconds < 60 -> "just now"
        diffSeconds < 3_600 -> "${diffSeconds / 60}m ago"
        diffSeconds < 86_400 -> "${diffSeconds / 3_600}h ago"
        diffSeconds < 604_800 -> "${diffSeconds / 86_400}d ago"
        else -> "${diffSeconds / 604_800}w ago"
    }
    return "Last seen $relative"
}

private fun trimSensorGraphValue(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)

/**
 * Compact line graph for an auxiliary sensor (e.g. a climate entity's configured temperature or
 * humidity sensor), meant to be embedded inside another entity's Activity tab.
 */
@Composable
fun EntitySensorGraphCard(
    sensorEntity: HAEntity,
    viewModel: MainViewModel,
    selectedHours: Int,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val historyMapping by viewModel.historyMapping.collectAsState()
    val history = historyMapping[sensorEntity.entity_id].orEmpty()
    val unit = sensorEntity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()

    val graphPoints = remember(history, unit) {
        history.mapNotNull { entry ->
            val millis = parseHistoryMillis(entry.last_changed) ?: return@mapNotNull null
            val value = entry.state.toFloatOrNull() ?: return@mapNotNull null
            HistoryPoint(millis, value, trimSensorGraphValue(value) + if (unit.isNotBlank()) " $unit" else "")
        }.sortedBy { it.timeMillis }
    }
    val values = graphPoints.map { it.value }
    val minValue = values.minOrNull()
    val maxValue = values.maxOrNull()
    val avgValue = if (values.isNotEmpty()) values.average().toFloat() else null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(10.dp).background(lineColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                sensorEntity.friendlyName ?: sensorEntity.entity_id,
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            graphPoints.lastOrNull()?.let { Text(it.label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(18.dp),
            color = appColors.subtleSurface,
            border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
        ) {
            if (graphPoints.size < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Not enough data", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                InteractiveLineGraph(
                    points = graphPoints,
                    lineColor = lineColor,
                    stepped = false,
                    modifier = Modifier.fillMaxSize(),
                    gradientColors = sensorGradientColors(sensorEntity.deviceClass)
                )
            }
        }
        if (minValue != null && maxValue != null) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SensorGraphStat("Min", trimSensorGraphValue(minValue), unit)
                avgValue?.let { SensorGraphStat("Avg", trimSensorGraphValue(it), unit) }
                SensorGraphStat("Max", trimSensorGraphValue(maxValue), unit)
            }
        }
    }
}

@Composable
private fun SensorGraphStat(label: String, value: String, unit: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (unit.isNotBlank()) "$value $unit" else value,
            color = appColors.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Horizontal pill selector for the history window (6/12/24/48/72h). */
@Composable
fun HistoryRangeChips(
    selectedHours: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryRangeOptions.forEach { hours ->
            val isSelected = hours == selectedHours
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) accent.copy(alpha = 0.22f) else appColors.subtleSurface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accent else appColors.onMuted.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(hours) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${hours}h",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) appColors.onSurface else appColors.onMuted
                )
            }
        }
    }
}

/** Small circular avatar for the actor that triggered an event. Shows their person picture when available. */
@Composable
fun ActorAvatar(actorName: String?, modifier: Modifier = Modifier, size: Dp = 28.dp, pictureUrl: String? = null) {
    val appColors = LocalHKIAppColors.current
    val isSystem = actorName.isNullOrBlank() || actorName.equals("System", ignoreCase = true)
    val initials = actorName
        ?.split(" ", ".", "_")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercase() }
        ?.take(2)
        .orEmpty()
    val bg = if (isSystem) appColors.subtleSurface
    else statePalette[(abs((actorName ?: "").hashCode())) % statePalette.size].copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (!pictureUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = pictureUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isSystem || initials.isBlank()) {
            Icon(
                imageVector = if (isSystem) Icons.Default.SmartToy else Icons.Default.Person,
                contentDescription = null,
                tint = appColors.onMuted,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * HA-style horizontal history bar. Renders state-over-time as colored segments across the
 * selected window. Tap/drag scrubs to inspect the exact state and time at a position.
 */
@Composable
fun StateTimelineBar(
    entries: List<HAHistoryEntry>,
    windowStartMillis: Long,
    windowEndMillis: Long,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val muted = appColors.onMuted.copy(alpha = 0.55f)

    // Build ascending segments clamped to the window.
    val segments = remember(entries, windowStartMillis, windowEndMillis) {
        val sorted = entries
            .mapNotNull { e -> parseHistoryMillis(e.last_changed)?.let { it to e } }
            .sortedBy { it.first }
        val out = mutableListOf<TimelineSegment>()
        val span = (windowEndMillis - windowStartMillis).coerceAtLeast(1L)
        sorted.forEachIndexed { index, (millis, entry) ->
            val rawStart = millis
            val end = if (index < sorted.lastIndex) sorted[index + 1].first else windowEndMillis
            val clampedStart = rawStart.coerceIn(windowStartMillis, windowEndMillis)
            val clampedEnd = end.coerceIn(windowStartMillis, windowEndMillis)
            if (clampedEnd <= clampedStart) return@forEachIndexed
            out += TimelineSegment(
                startFraction = (clampedStart - windowStartMillis).toFloat() / span,
                endFraction = (clampedEnd - windowStartMillis).toFloat() / span,
                state = entry.state,
                startMillis = clampedStart
            )
        }
        out
    }

    var scrubFraction by remember(segments) { mutableStateOf<Float?>(null) }
    val selected = scrubFraction?.let { frac ->
        segments.firstOrNull { frac >= it.startFraction && frac <= it.endFraction } ?: segments.lastOrNull()
    }
    val cursorTime = scrubFraction?.let {
        windowStartMillis + ((windowEndMillis - windowStartMillis) * it).roundToInt()
    }

    Column(modifier = modifier) {
        // Tooltip (reserves height so the bar doesn't jump when scrubbing)
        Box(modifier = Modifier.fillMaxWidth().height(22.dp)) {
            if (selected != null && cursorTime != null) {
                Text(
                    text = "${selected.state.replaceFirstChar { it.uppercase() }} · ${formatHistoryClock(cursorTime)}",
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(appColors.subtleSurface)
                .pointerInput(segments) {
                    if (segments.isEmpty()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        scrubFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { p ->
                                scrubFraction = (p.x / size.width).coerceIn(0f, 1f)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            segments.forEach { seg ->
                val x = seg.startFraction * size.width
                val w = (seg.endFraction - seg.startFraction) * size.width
                drawRect(
                    color = colorForHistoryState(seg.state, accent, muted),
                    topLeft = Offset(x, 0f),
                    size = Size(w.coerceAtLeast(0.5f), size.height)
                )
            }
            scrubFraction?.let { frac ->
                val cx = frac * size.width
                drawRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(cx - 1.dp.toPx(), 0f),
                    size = Size(2.dp.toPx(), size.height)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatHistoryClock(windowStartMillis, withDate = true),
                color = appColors.onMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Now",
                color = appColors.onMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private data class TimelineSegment(
    val startFraction: Float,
    val endFraction: Float,
    val state: String,
    val startMillis: Long
)

/**
 * Interactive line/step graph for numeric or binary history. Tap or drag anywhere on the plot
 * to pin a crosshair to the nearest sample and read its exact time and value, like HA graphs.
 */
@Composable
fun InteractiveLineGraph(
    points: List<HistoryPoint>,
    lineColor: Color,
    stepped: Boolean,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>? = null
) {
    val appColors = LocalHKIAppColors.current
    val density = LocalDensity.current
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val minValue = points.minOfOrNull { it.value } ?: 0f
    val maxValue = points.maxOfOrNull { it.value } ?: 1f
    val range = (maxValue - minValue).takeIf { it > 0.0001f } ?: 1f

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 18.dp.toPx() }
        val plotW = (widthPx - 2 * padPx).coerceAtLeast(1f)
        val plotH = (heightPx - 2 * padPx).coerceAtLeast(1f)

        fun xForIndex(i: Int): Float =
            if (points.size <= 1) padPx + plotW / 2f
            else padPx + plotW * i / (points.size - 1)

        fun yForValue(v: Float): Float =
            padPx + (1f - ((v - minValue) / range).coerceIn(0f, 1f)) * plotH

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    if (points.isEmpty()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        selected = nearestIndex(down.position.x, padPx, plotW, points.size)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { p ->
                                selected = nearestIndex(p.x, padPx, plotW, points.size)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (points.isEmpty()) return@Canvas

            // Horizontal grid lines
            repeat(4) { row ->
                val y = padPx + plotH * (row + 1) / 5f
                drawLine(
                    Color.White.copy(alpha = 0.07f),
                    Offset(padPx, y),
                    Offset(padPx + plotW, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val line = Path()
            val fill = Path()
            points.forEachIndexed { index, point ->
                val x = xForIndex(index)
                val y = yForValue(point.value)
                if (index == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, padPx + plotH)
                    fill.lineTo(x, y)
                } else {
                    if (stepped) {
                        val prevY = yForValue(points[index - 1].value)
                        line.lineTo(x, prevY)
                        fill.lineTo(x, prevY)
                    }
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(xForIndex(points.lastIndex), padPx + plotH)
            fill.close()

            val hueGradientBrush = if (gradientColors != null && gradientColors.size >= 2) {
                Brush.verticalGradient(gradientColors, startY = padPx, endY = padPx + plotH)
            } else null
            val fillBrush = if (hueGradientBrush != null) {
                Brush.verticalGradient(gradientColors!!.map { it.copy(alpha = 0.28f) }, startY = padPx, endY = padPx + plotH)
            } else {
                Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent))
            }
            drawPath(fill, fillBrush)
            if (hueGradientBrush != null) {
                drawPath(line, hueGradientBrush, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
            } else {
                drawPath(line, lineColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
            }

            // Latest point marker
            val lastX = xForIndex(points.lastIndex)
            val lastY = yForValue(points.last().value)
            if (hueGradientBrush != null) {
                drawCircle(hueGradientBrush, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
            } else {
                drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
            }
            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))

            // Selection crosshair
            selected?.let { idx ->
                val sx = xForIndex(idx)
                val sy = yForValue(points[idx].value)
                drawLine(
                    Color.White.copy(alpha = 0.6f),
                    Offset(sx, padPx),
                    Offset(sx, padPx + plotH),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(sx, sy))
                if (hueGradientBrush != null) {
                    drawCircle(hueGradientBrush, radius = 3.5.dp.toPx(), center = Offset(sx, sy))
                } else {
                    drawCircle(lineColor, radius = 3.5.dp.toPx(), center = Offset(sx, sy))
                }
            }
        }

        // Tooltip overlay
        selected?.let { idx ->
            val point = points[idx]
            val sxPx = xForIndex(idx)
            val tooltipWidth = 132.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val rawX = sxPx - tooltipWidthPx / 2f
            val clampedX = rawX.coerceIn(0f, (widthPx - tooltipWidthPx).coerceAtLeast(0f))
            Column(
                modifier = Modifier
                    .offset { IntOffset(clampedX.roundToInt(), with(density) { 4.dp.roundToPx() }) }
                    .width(tooltipWidth)
                    .clip(RoundedCornerShape(10.dp))
                    .background(appColors.elevated.copy(alpha = 0.96f))
                    .border(1.dp, appColors.onMuted.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = point.label,
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatHistoryClock(point.timeMillis, withDate = true),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun nearestIndex(x: Float, padPx: Float, plotW: Float, count: Int): Int {
    if (count <= 1) return 0
    val frac = ((x - padPx) / plotW).coerceIn(0f, 1f)
    return (frac * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

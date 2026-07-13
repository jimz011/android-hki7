package com.example.hki7.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKISensorGraphStack
import com.example.hki7.data.HKISensorGraphWidget
import com.example.hki7.ui.MainViewModel
import java.util.UUID
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.HistoryPoint
import com.example.hki7.ui.components.HistoryRangeOptions
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.formatHistoryClock
import com.example.hki7.ui.components.parseHistoryMillis
import com.example.hki7.ui.components.sensorGradientColors
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.delay
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

/** Stable per-series colors (first sensor gets the classic graph blue). */
private val sensorSeriesPalette = listOf(
    Color(0xFF4A90E2), Color(0xFFEF5350), Color(0xFF66BB6A), Color(0xFFFFB300),
    Color(0xFF7E57C2), Color(0xFF26C6DA), Color(0xFFD97FB0), Color(0xFFB0B845)
)

private data class SensorGraphSeries(
    val entityId: String,
    val name: String,
    val color: Color,
    /** Value-hue gradient (temperature-style) — only applied when it is the sole series. */
    val gradient: List<Color>?,
    val unit: String,
    val points: List<HistoryPoint>
)

private fun trimGraphValue(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun labelWithUnit(value: Float, unit: String): String =
    trimGraphValue(value) + if (unit.isNotBlank()) " $unit" else ""

/** History graph widget: one or more sensors as lines (temperature-graph style) or bars. */
@Composable
fun SensorGraphWidgetItem(
    widget: HKISensorGraphWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        SensorGraphCardView(widget, viewModel)
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(28.dp)) {
                Icon(Icons.Default.Settings, "Graph settings", tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A collapsible stack of sensor graphs, header-styled like the energy/climate stacks. */
@Composable
fun SensorGraphStackWidgetItem(
    stack: HKISensorGraphStack,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onToggleCollapsed: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (stack.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val collapsed = stack.collapsible && (stack.isCollapsed ?: stack.defaultCollapsed)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                MdiIcon(stack.icon ?: "chart-line", tint = Color.Gray, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(stack.title ?: "Sensor Graphs", color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (stack.collapsible) {
                    IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(24.dp)) {
                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!collapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stack.graphs.isEmpty()) {
                        Surface(shape = RoundedCornerShape(stack.cornerRadius.dp), color = appColors.elevated) {
                            Text("No graphs yet — open the stack settings to add sensor graphs.",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                    stack.graphs.forEach { graph ->
                        SensorGraphCardView(
                            graph.copy(width = "full", isSquare = false, cornerRadius = stack.cornerRadius),
                            viewModel
                        )
                    }
                }
            }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp))
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(24.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = appColors.onSurface, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** The graph card itself (title, legend, plot, stats) without any edit-mode chrome. */
@Composable
private fun SensorGraphCardView(
    widget: HKISensorGraphWidget,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val ids = widget.entityIds
    val entityFlow = remember(viewModel, ids) {
        viewModel.entitiesMatching("sensor_graph:${ids.joinToString(",")}") { it.entity_id in ids }
    }
    val entities by entityFlow.collectAsState()
    val historyMapping by viewModel.historyMapping.collectAsState()

    // Keep the window fresh: refetch on config changes and every 5 minutes while shown.
    LaunchedEffect(ids, widget.hours) {
        while (true) {
            ids.forEach { viewModel.fetchEntityHistory(it, widget.hours.toLong()) }
            delay(5.minutes)
        }
    }

    val endMs = remember(historyMapping, ids, widget.hours) { System.currentTimeMillis() }
    val startMs = endMs - widget.hours * 3_600_000L
    val series = remember(ids, entities, historyMapping, startMs, endMs) {
        ids.mapIndexed { index, entityId ->
            val entity: HAEntity? = entities.find { it.entity_id == entityId }
            val unit = entity?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
            val raw = historyMapping[entityId].orEmpty().mapNotNull { entry ->
                val millis = parseHistoryMillis(entry.last_changed) ?: return@mapNotNull null
                val value = entry.state.toFloatOrNull() ?: return@mapNotNull null
                HistoryPoint(millis, value, labelWithUnit(value, unit))
            }.sortedBy { it.timeMillis }
            // Carry the last value from before the window to the window start so lines span the card.
            val before = raw.lastOrNull { it.timeMillis < startMs }
            val inWindow = raw.filter { it.timeMillis in startMs..endMs }
            val points = if (before != null && (inWindow.isEmpty() || inWindow.first().timeMillis > startMs)) {
                listOf(before.copy(timeMillis = startMs)) + inWindow
            } else inWindow
            SensorGraphSeries(
                entityId = entityId,
                name = entity?.friendlyName ?: entityId,
                color = sensorSeriesPalette[index % sensorSeriesPalette.size],
                gradient = sensorGradientColors(entity?.deviceClass),
                unit = unit,
                points = points
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth()
            .then(if (widget.isSquare) Modifier.aspectRatio(1f) else Modifier),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = appColors.elevated
    ) {
        Column(Modifier.padding(14.dp).then(if (widget.isSquare) Modifier.fillMaxSize() else Modifier)) {
            if (!widget.title.isNullOrBlank() || !widget.icon.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    if (!widget.icon.isNullOrBlank()) {
                        MdiIcon(widget.icon, tint = appColors.onSurface, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!widget.title.isNullOrBlank()) {
                        Text(widget.title, color = appColors.onSurface,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
                    if (series.isEmpty()) {
                        Text(
                            "No sensors selected — open the widget settings in edit mode.",
                            style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                        )
                        return@Column
                    }
                    // Legend: dot + name + latest value per sensor.
                    series.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Box(Modifier.size(10.dp).background(s.color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.name,
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            s.points.lastOrNull()?.let {
                                Text(it.label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .then(if (widget.isSquare) Modifier.weight(1f) else Modifier.height(150.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = appColors.subtleSurface,
                        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
                    ) {
                        if (series.all { it.points.size < 2 }) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Not enough data", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        } else if (widget.style == "bar") {
                            MultiSensorBarGraph(series, startMs, endMs, widget.hours, Modifier.fillMaxSize())
                        } else {
                            MultiSensorLineGraph(series, startMs, endMs, Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatHistoryClock(startMs, withDate = true), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                        Text("Now", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    // Min/Avg/Max like the climate sensor cards, but only for a single sensor.
                    if (series.size == 1) {
                        val values = series[0].points.map { it.value }
                        if (values.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SensorGraphWidgetStat("Min", labelWithUnit(values.min(), series[0].unit))
                                SensorGraphWidgetStat("Avg", labelWithUnit(values.average().toFloat(), series[0].unit))
                                SensorGraphWidgetStat("Max", labelWithUnit(values.max(), series[0].unit))
                            }
                        }
                    }
        }
    }
}

@Composable
private fun SensorGraphWidgetStat(label: String, value: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Time-based multi-series line graph in the InteractiveLineGraph (temperature card) style:
 * grid lines, 3.5dp round-cap strokes, latest-point markers, and a scrub crosshair whose
 * tooltip lists every series' value at the cursor time. A sole series keeps its value-hue
 * gradient stroke and soft fill, exactly like the climate sensor graphs.
 */
@Composable
private fun MultiSensorLineGraph(
    series: List<SensorGraphSeries>,
    startMs: Long,
    endMs: Long,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val density = LocalDensity.current
    var cursorFraction by remember(series) { mutableStateOf<Float?>(null) }

    val drawn = series.filter { it.points.size >= 2 }
    val allValues = drawn.flatMap { s -> s.points.map { it.value } }
    val minValue = allValues.minOrNull() ?: 0f
    val maxValue = allValues.maxOrNull() ?: 1f
    val range = (maxValue - minValue).takeIf { it > 0.0001f } ?: 1f
    val span = (endMs - startMs).coerceAtLeast(1L)

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 18.dp.toPx() }
        val plotW = (widthPx - 2 * padPx).coerceAtLeast(1f)
        val plotH = (heightPx - 2 * padPx).coerceAtLeast(1f)

        fun xForTime(t: Long): Float = padPx + plotW * ((t - startMs).toFloat() / span)
        fun yForValue(v: Float): Float = padPx + (1f - ((v - minValue) / range).coerceIn(0f, 1f)) * plotH

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawn) {
                    if (drawn.isEmpty()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        cursorFraction = ((down.position.x - padPx) / plotW).coerceIn(0f, 1f)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { p ->
                                cursorFraction = ((p.x - padPx) / plotW).coerceIn(0f, 1f)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            repeat(4) { row ->
                val y = padPx + plotH * (row + 1) / 5f
                drawLine(Color.White.copy(alpha = 0.07f), Offset(padPx, y), Offset(padPx + plotW, y), strokeWidth = 1.dp.toPx())
            }

            val soloGradient = if (drawn.size == 1) drawn[0].gradient?.takeIf { it.size >= 2 } else null
            drawn.forEach { s ->
                val line = Path()
                s.points.forEachIndexed { index, point ->
                    val x = xForTime(point.timeMillis)
                    val y = yForValue(point.value)
                    if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                if (drawn.size == 1) {
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(xForTime(s.points.last().timeMillis), padPx + plotH)
                        lineTo(xForTime(s.points.first().timeMillis), padPx + plotH)
                        close()
                    }
                    val fillBrush = if (soloGradient != null) {
                        Brush.verticalGradient(soloGradient.map { it.copy(alpha = 0.28f) }, startY = padPx, endY = padPx + plotH)
                    } else {
                        Brush.verticalGradient(listOf(s.color.copy(alpha = 0.28f), Color.Transparent))
                    }
                    drawPath(fill, fillBrush)
                }
                if (soloGradient != null) {
                    val strokeBrush = Brush.verticalGradient(soloGradient, startY = padPx, endY = padPx + plotH)
                    drawPath(line, strokeBrush, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                } else {
                    drawPath(line, s.color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                }
                val last = s.points.last()
                val lastCenter = Offset(xForTime(last.timeMillis), yForValue(last.value))
                drawCircle(s.color, radius = 5.dp.toPx(), center = lastCenter)
                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = lastCenter)
            }

            cursorFraction?.let { frac ->
                val cx = padPx + plotW * frac
                drawLine(Color.White.copy(alpha = 0.6f), Offset(cx, padPx), Offset(cx, padPx + plotH), strokeWidth = 1.5.dp.toPx())
                val cursorTime = startMs + (span * frac).toLong()
                drawn.forEach { s ->
                    val at = s.points.lastOrNull { it.timeMillis <= cursorTime } ?: s.points.first()
                    val center = Offset(cx, yForValue(at.value))
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = center)
                    drawCircle(s.color, radius = 3.5.dp.toPx(), center = center)
                }
            }
        }

        cursorFraction?.let { frac ->
            val cursorTime = startMs + (span * frac).toLong()
            val tooltipWidth = 168.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val rawX = padPx + plotW * frac - tooltipWidthPx / 2f
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
                    formatHistoryClock(cursorTime, withDate = true),
                    color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                )
                drawn.forEach { s ->
                    val at = s.points.lastOrNull { it.timeMillis <= cursorTime } ?: s.points.first()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(s.color, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            at.label,
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Energy-style bar rendering of the same data: the window is split into time buckets and each
 * bucket shows one rounded bar per sensor (bucket mean). Tap/drag to inspect bucket values.
 */
@Composable
private fun MultiSensorBarGraph(
    series: List<SensorGraphSeries>,
    startMs: Long,
    endMs: Long,
    hours: Int,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val density = LocalDensity.current
    var selectedBucket by remember(series) { mutableStateOf<Int?>(null) }

    val drawn = series.filter { it.points.isNotEmpty() }
    val bucketCount = when {
        hours <= 24 -> hours
        hours <= 48 -> hours / 2
        else -> hours / 3
    }.coerceIn(6, 48)
    val span = (endMs - startMs).coerceAtLeast(1L)
    val bucketMs = span / bucketCount

    // bucket means per series; NaN marks empty buckets (carried value from the previous sample).
    val bucketed = remember(drawn, startMs, endMs, bucketCount) {
        drawn.map { s ->
            FloatArray(bucketCount) { bucket ->
                val from = startMs + bucket * bucketMs
                val to = from + bucketMs
                val inBucket = s.points.filter { it.timeMillis in from until to }
                if (inBucket.isNotEmpty()) inBucket.map { it.value }.average().toFloat()
                else s.points.lastOrNull { it.timeMillis < to }?.value ?: Float.NaN
            }
        }
    }
    val allValues = bucketed.flatMap { arr -> arr.filter { !it.isNaN() } }
    val maxValue = (allValues.maxOrNull() ?: 1f).coerceAtLeast(0f)
    val minValue = (allValues.minOrNull() ?: 0f).coerceAtMost(0f)
    val range = (maxValue - minValue).takeIf { it > 0.0001f } ?: 1f

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 14.dp.toPx() }
        val plotW = (widthPx - 2 * padPx).coerceAtLeast(1f)
        val plotH = (heightPx - 2 * padPx).coerceAtLeast(1f)

        fun yForValue(v: Float): Float = padPx + (1f - ((v - minValue) / range).coerceIn(0f, 1f)) * plotH
        val zeroY = yForValue(0f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bucketed) {
                    if (bucketed.isEmpty()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        selectedBucket = (((down.position.x - padPx) / plotW) * bucketCount).toInt().coerceIn(0, bucketCount - 1)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { p ->
                                selectedBucket = (((p.x - padPx) / plotW) * bucketCount).toInt().coerceIn(0, bucketCount - 1)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            repeat(4) { row ->
                val y = padPx + plotH * (row + 1) / 5f
                drawLine(Color.White.copy(alpha = 0.07f), Offset(padPx, y), Offset(padPx + plotW, y), strokeWidth = 1.dp.toPx())
            }

            val slotW = plotW / bucketCount
            val groupW = slotW * 0.72f
            val barW = (groupW / drawn.size.coerceAtLeast(1)).coerceAtLeast(1.5f)
            val radius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
            for (bucket in 0 until bucketCount) {
                val slotStart = padPx + bucket * slotW + (slotW - groupW) / 2f
                bucketed.forEachIndexed { seriesIndex, values ->
                    val value = values[bucket]
                    if (value.isNaN()) return@forEachIndexed
                    val yValue = yForValue(value)
                    val top = minOf(yValue, zeroY)
                    val barH = (kotlin.math.abs(yValue - zeroY)).coerceAtLeast(1.5f)
                    val dim = selectedBucket != null && selectedBucket != bucket
                    drawRoundRect(
                        color = drawn[seriesIndex].color.copy(alpha = if (dim) 0.35f else 0.9f),
                        topLeft = Offset(slotStart + seriesIndex * barW, top),
                        size = Size(barW * 0.88f, barH),
                        cornerRadius = radius
                    )
                }
            }

            selectedBucket?.let { bucket ->
                val cx = padPx + bucket * slotW + slotW / 2f
                drawLine(Color.White.copy(alpha = 0.5f), Offset(cx, padPx), Offset(cx, padPx + plotH), strokeWidth = 1.dp.toPx())
            }
        }

        selectedBucket?.let { bucket ->
            val bucketTime = startMs + bucket * bucketMs
            val tooltipWidth = 168.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val slotW = plotW / bucketCount
            val rawX = padPx + bucket * slotW + slotW / 2f - tooltipWidthPx / 2f
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
                    formatHistoryClock(bucketTime, withDate = true),
                    color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                )
                drawn.forEachIndexed { index, s ->
                    val value = bucketed[index][bucket]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(s.color, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (value.isNaN()) "—" else labelWithUnit(value, s.unit),
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorGraphWidgetSettingsDialog(
    widget: HKISensorGraphWidget,
    allEntities: List<HAEntity>,
    // Stack children inherit the stack's width/shape/corners, so hide those options for them.
    showLayoutOptions: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (HKISensorGraphWidget) -> Unit
) {
    var entityIds by remember(widget) { mutableStateOf(widget.entityIds) }
    var title by remember(widget) { mutableStateOf(widget.title.orEmpty()) }
    var style by remember(widget) { mutableStateOf(widget.style) }
    var hours by remember(widget) { mutableIntStateOf(widget.hours) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var square by remember(widget) { mutableStateOf(widget.isSquare) }
    var radius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        val sensors = allEntities.filter { it.entity_id.startsWith("sensor.") || it.entity_id.startsWith("number.") || it.entity_id.startsWith("input_number.") }
        AdvancedEntitySearchDialog(
            allEntities = sensors.ifEmpty { allEntities },
            title = "Select Sensors",
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { picking = false },
            onEntitiesSelected = { ids -> entityIds = ids; picking = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sensor Graph") },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Sensors", style = MaterialTheme.typography.labelLarge)
                entityIds.forEach { id ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            allEntities.find { it.entity_id == id }?.friendlyName ?: id,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { entityIds = entityIds - id }) { Icon(Icons.Default.Close, "Remove") }
                    }
                }
                TextButton(onClick = { picking = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (entityIds.isEmpty()) "Add sensors" else "Edit sensors")
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Graph Style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = style == "line", onClick = { style = "line" }, label = { Text("Lines") })
                    FilterChip(selected = style == "bar", onClick = { style = "bar" }, label = { Text("Bars") })
                }
                Text("Time Range", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryRangeOptions.forEach { option ->
                        FilterChip(selected = hours == option, onClick = { hours = option }, label = { Text("${option}h") })
                    }
                }
                if (showLayoutOptions) {
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text("Shape", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !square, onClick = { square = false }, label = { Text("Standard") })
                        FilterChip(selected = square, onClick = { square = true }, label = { Text("Square") })
                    }
                    Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(8 to "Sharp", 20 to "Modern", 28 to "Round").forEach { (value, label) ->
                            FilterChip(selected = radius == value, onClick = { radius = value }, label = { Text(label) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(
                    entityIds = entityIds, title = title.ifBlank { null }, style = style,
                    hours = hours, width = width, isSquare = square, cornerRadius = radius
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SensorGraphStackSettingsDialog(
    stack: HKISensorGraphStack,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKISensorGraphStack) -> Unit
) {
    var title by remember(stack) { mutableStateOf(stack.title.orEmpty()) }
    var width by remember(stack) { mutableStateOf(if (stack.width == "third") "half" else stack.width) }
    var radius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var graphs by remember(stack) { mutableStateOf(stack.graphs) }
    var editingGraph by remember { mutableStateOf<HKISensorGraphWidget?>(null) }
    var addingGraph by remember { mutableStateOf(false) }

    if (addingGraph) {
        val sensors = allEntities.filter {
            it.entity_id.startsWith("sensor.") || it.entity_id.startsWith("number.") || it.entity_id.startsWith("input_number.")
        }
        AdvancedEntitySearchDialog(
            allEntities = sensors.ifEmpty { allEntities },
            title = "Select Sensors",
            singleSelect = false,
            preselectedIds = emptySet(),
            onDismiss = { addingGraph = false },
            onEntitiesSelected = { ids ->
                if (ids.isNotEmpty()) {
                    graphs = graphs + HKISensorGraphWidget(id = UUID.randomUUID().toString(), entityIds = ids)
                }
                addingGraph = false
            }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    editingGraph?.let { graph ->
        SensorGraphWidgetSettingsDialog(
            widget = graph,
            allEntities = allEntities,
            showLayoutOptions = false,
            onDismiss = { editingGraph = null },
            onSave = { updated ->
                graphs = graphs.map { if (it.id == updated.id) updated else it }
                editingGraph = null
            }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sensor Graph Stack") },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Graphs", style = MaterialTheme.typography.labelLarge)
                graphs.forEachIndexed { index, graph ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            graph.title ?: allEntities.find { it.entity_id == graph.entityIds.firstOrNull() }?.friendlyName
                                ?.let { first -> if (graph.entityIds.size > 1) "$first +${graph.entityIds.size - 1}" else first }
                                ?: "Graph ${index + 1} · ${graph.entityIds.size} sensor${if (graph.entityIds.size == 1) "" else "s"}",
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { editingGraph = graph }) { Text("Edit") }
                        IconButton(onClick = { graphs = graphs.filterNot { it.id == graph.id } }) {
                            Icon(Icons.Default.Close, "Remove")
                        }
                    }
                }
                TextButton(onClick = { addingGraph = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add graph")
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Collapsible", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = collapsible, onCheckedChange = { collapsible = it })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8 to "Sharp", 20 to "Modern", 28 to "Round").forEach { (value, label) ->
                        FilterChip(selected = radius == value, onClick = { radius = value }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(stack.copy(
                    title = title.ifBlank { null }, width = width, cornerRadius = radius,
                    collapsible = collapsible, graphs = graphs
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

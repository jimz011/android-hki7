@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.ValetudoLayer
import com.jimz011apps.hki7.data.ValetudoMap
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Palette
// ─────────────────────────────────────────────────────────────────────────────

private val MapBackground = Color(0xFF1A1A2E)
private val FloorColor = Color(0xFF2E3A5C)
private val WallColor = Color(0xFF8794C4)
private val PathColor = Color(0xFFE8ECFA)
private val RobotColor = Color(0xFF66BB6A)
private val ChargerColor = Color(0xFF42A5F5)

/** Rooms are only distinguishable by colour, so the palette cycles by segment order. */
private val SegmentColors = listOf(
    Color(0xFF3D6DB5), Color(0xFF4E9A6B), Color(0xFF9A6B4E),
    Color(0xFF6B4E9A), Color(0xFF4E8A9A), Color(0xFF9A4E6B),
    Color(0xFF7A8A3D), Color(0xFF3D5A8A)
)

// ─────────────────────────────────────────────────────────────────────────────
// Rasterisation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A map rasterised once into a bitmap, plus the lookup a tap needs.
 *
 * A typical Valetudo map is tens of thousands of pixels across its layers. Drawing each as its own
 * rect every frame would make panning and zooming unusable, so the layers are baked into an
 * [ImageBitmap] on a background thread and the Canvas only blits it. [segmentIds] is the parallel
 * hit-test grid: one entry per bitmap pixel, holding the segment id covering it.
 */
class ValetudoMapRender(
    val bitmap: ImageBitmap,
    /** Map-pixel coordinate of the bitmap's top-left, so entities can use the same transform. */
    val originX: Int,
    val originY: Int,
    val width: Int,
    val height: Int,
    val pixelSize: Int,
    private val segmentIds: Array<String?>,
    /** Segment id in draw order, for stable colours and for listing rooms outside the canvas. */
    val segments: List<ValetudoSegmentInfo>,
    /** Entity positions, resolved once here so the draw pass never re-scans the entity list. */
    val robotPoint: Pair<Int, Int>? = null,
    val robotAngle: Float? = null,
    val chargerPoint: Pair<Int, Int>? = null,
    val pathPoints: List<Int>? = null,
    val predictedPathPoints: List<Int>? = null
) {
    fun segmentAt(mapX: Int, mapY: Int): String? {
        val x = mapX - originX
        val y = mapY - originY
        if (x < 0 || y < 0 || x >= width || y >= height) return null
        return segmentIds[y * width + x]
    }
}

data class ValetudoSegmentInfo(
    val segmentId: String,
    val name: String?,
    val color: Color,
    /** Centre of the segment in map-pixel coordinates — where its label belongs. */
    val centerX: Float,
    val centerY: Float
)

/**
 * Bakes [map]'s layers into a [ValetudoMapRender]. Pure and Android-bitmap-bound, so it must run
 * off the main thread; [rememberValetudoMapRender] handles that for callers.
 */
fun rasterizeValetudoMap(map: ValetudoMap): ValetudoMapRender? {
    val layers = map.layers.filter { it.type.isNotEmpty() }
    if (layers.isEmpty()) return null

    // Bounds come from the pixels themselves rather than `size`, because `size` describes the
    // robot's whole coordinate space and is mostly empty — using it wastes memory and shrinks the
    // visible map to a speck in the middle.
    var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE
    layers.forEach { layer ->
        layer.forEachPixel { x, y ->
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }
    }
    if (minX > maxX || minY > maxY) return null

    val width = maxX - minX + 1
    val height = maxY - minY + 1
    if (width <= 0 || height <= 0 || width.toLong() * height > MAX_RASTER_PIXELS) return null

    val pixels = IntArray(width * height)
    val segmentIds = arrayOfNulls<String>(width * height)
    val segments = mutableListOf<ValetudoSegmentInfo>()

    fun paint(layer: ValetudoLayer, color: Int, segmentId: String? = null) {
        layer.forEachPixel { x, y ->
            val index = (y - minY) * width + (x - minX)
            if (index in pixels.indices) {
                pixels[index] = color
                if (segmentId != null) segmentIds[index] = segmentId
            }
        }
    }

    // Order matters: floor underneath, then rooms, then walls on top so they read as outlines.
    map.floors.forEach { paint(it, FloorColor.toArgb()) }

    var colorIndex = 0
    map.segments.forEach { layer ->
        val segmentId = layer.metaData.segmentId
        val color = SegmentColors[colorIndex % SegmentColors.size]
        colorIndex++
        // An actively cleaning segment is lifted rather than recoloured, so its identity colour and
        // the "this room is running" cue can both be read at once.
        val shade = if (layer.metaData.active) lightenColor(color) else color
        paint(layer, shade.toArgb(), segmentId)

        if (segmentId != null) {
            var sumX = 0L; var sumY = 0L; var count = 0
            layer.forEachPixel { x, y -> sumX += x; sumY += y; count++ }
            if (count > 0) {
                segments += ValetudoSegmentInfo(
                    segmentId = segmentId,
                    name = layer.metaData.name?.takeIf { it.isNotBlank() },
                    color = color,
                    centerX = sumX.toFloat() / count,
                    centerY = sumY.toFloat() / count
                )
            }
        }
    }

    map.walls.forEach { paint(it, WallColor.toArgb()) }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    val robot = map.entity(ValetudoMap.ENTITY_ROBOT)
    val charger = map.entity(ValetudoMap.ENTITY_CHARGER)

    return ValetudoMapRender(
        bitmap = bitmap.asImageBitmap(),
        originX = minX,
        originY = minY,
        width = width,
        height = height,
        pixelSize = map.pixelSize.coerceAtLeast(1),
        segmentIds = segmentIds,
        segments = segments,
        robotPoint = robot?.firstPoint(),
        robotAngle = robot?.metaData?.angle,
        chargerPoint = charger?.firstPoint(),
        pathPoints = map.entity(ValetudoMap.ENTITY_PATH)?.points?.takeIf { it.size >= 4 },
        predictedPathPoints = map.entity(ValetudoMap.ENTITY_PREDICTED_PATH)?.points?.takeIf { it.size >= 4 }
    )
}

private fun com.jimz011apps.hki7.data.ValetudoEntity.firstPoint(): Pair<Int, Int>? =
    if (points.size >= 2) points[0] to points[1] else null

/** Guards against a malformed map allocating hundreds of MB; ~64 MP is far beyond any real robot. */
private const val MAX_RASTER_PIXELS = 64L * 1024 * 1024

private fun lightenColor(color: Color): Color = Color(
    red = min(1f, color.red + 0.18f),
    green = min(1f, color.green + 0.18f),
    blue = min(1f, color.blue + 0.18f),
    alpha = color.alpha
)

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

/** Everything the map pane needs, resolved asynchronously. */
@Immutable
data class ValetudoMapState(
    val render: ValetudoMapRender? = null,
    /** `segment_id -> Home Assistant area_id`, from HA's segment mapping. Empty when unmapped. */
    val segmentAreas: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    /**
     * Whether the configured camera turned out to carry a Valetudo payload. Null until the first
     * probe finishes. Callers that auto-detect use this to fall back to a plain camera image, and
     * once it is false the probe stops repeating — an ordinary camera will never start carrying map
     * data, and re-fetching its PNG every refresh tick would be pure waste.
     */
    val isValetudo: Boolean? = null
)

/**
 * Fetches, decodes and rasterises the map, re-running on [refreshTick] so the caller controls the
 * cadence (faster while the robot is cleaning). All work happens off the main thread.
 */
@Composable
fun rememberValetudoMapState(
    cameraEntityId: String?,
    vacuumEntityId: String,
    viewModel: MainViewModel,
    refreshTick: Int
): ValetudoMapState {
    var state by remember(cameraEntityId) { mutableStateOf(ValetudoMapState()) }

    // The area mapping is user configuration, not live data — fetching it per refresh would spam
    // the websocket for a value that changes only when HA's mapping dialog is used.
    LaunchedEffect(vacuumEntityId) {
        val areas = viewModel.vacuumSegmentAreas(vacuumEntityId)
        state = state.copy(segmentAreas = areas)
    }

    LaunchedEffect(cameraEntityId, refreshTick) {
        if (cameraEntityId.isNullOrBlank()) {
            state = state.copy(loading = false, failed = true, isValetudo = false)
            return@LaunchedEffect
        }
        // Settled as "not a Valetudo camera" — don't re-probe on later ticks.
        if (state.isValetudo == false) return@LaunchedEffect

        val result = viewModel.loadValetudoMap(cameraEntityId)
        val render = result.getOrNull()?.let {
            withContext(Dispatchers.Default) { rasterizeValetudoMap(it) }
        }
        state = state.copy(
            // A single failed refresh keeps the previous frame rather than blanking the map.
            render = render ?: state.render,
            loading = false,
            failed = render == null && state.render == null,
            isValetudo = when {
                render != null || state.render != null -> true
                // A fetch that never reached the camera says nothing about what it serves, so leave
                // the verdict open and probe again next tick.
                result.isFailure -> state.isValetudo
                else -> false
            }
        )
    }

    return state
}

// ─────────────────────────────────────────────────────────────────────────────
// The pane
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a decoded Valetudo map and turns a tap on a room into a clean of that room.
 *
 * Tapping calls `vacuum.clean_area`, which targets Home Assistant **areas**. When the user has not
 * mapped segments to areas in Home Assistant there is nothing to call, so the pane says so instead
 * of failing silently on every tap.
 */
@Composable
fun ValetudoMapPane(
    state: ValetudoMapState,
    onSegmentClick: (segmentId: String) -> Unit,
    modifier: Modifier = Modifier,
    /** False on the small stack tile, where pan/zoom would swallow the card's own tap. */
    interactive: Boolean = true
) {
    val appColors = LocalHKIAppColors.current
    val render = state.render

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var selected by remember { mutableStateOf<String?>(null) }

    // Canvas-space placement of the bitmap, recomputed on every draw and reused by the tap handler
    // so hit-testing can never drift from what is on screen.
    val placement = remember { mutableStateOf<MapPlacement?>(null) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            render != null -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!interactive) Modifier else Modifier
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 6f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                                .pointerInput(render) {
                                    detectTapGestures { tap ->
                                        val current = placement.value ?: return@detectTapGestures
                                        val segment = current.segmentAt(render, tap)
                                        if (segment != null) {
                                            selected = segment
                                            onSegmentClick(segment)
                                        }
                                    }
                                }
                        )
                ) {
                    drawRect(MapBackground)

                    val fit = min(size.width / render.width, size.height / render.height)
                    val drawScale = fit * scale
                    val drawWidth = render.width * drawScale
                    val drawHeight = render.height * drawScale
                    val left = (size.width - drawWidth) / 2f + offsetX
                    val top = (size.height - drawHeight) / 2f + offsetY

                    val current = MapPlacement(left, top, drawScale)
                    placement.value = current

                    drawImage(
                        image = render.bitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(render.width, render.height),
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
                        // Nearest-neighbour: the map is already at its native resolution, and
                        // smoothing turns crisp wall lines into grey mush when zoomed in.
                        filterQuality = FilterQuality.None
                    )

                    selected?.let { id ->
                        render.segments.firstOrNull { it.segmentId == id }?.let { info ->
                            drawSelectionRing(info, current, render)
                        }
                    }

                    drawPath(render, current)
                    drawCharger(render, current)
                    drawRobot(render, current)
                }
            }

            state.loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp))

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Map, null,
                    tint = appColors.onMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    stringResource(R.string.widgets_vacuum_valetudo_decode_failed),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Where the map bitmap currently sits on the canvas, in canvas pixels. */
private class MapPlacement(val left: Float, val top: Float, val scale: Float) {
    fun toCanvas(mapX: Float, mapY: Float, render: ValetudoMapRender): Offset = Offset(
        left + (mapX - render.originX) * scale,
        top + (mapY - render.originY) * scale
    )

    /** Entity points are in raw map units; layer pixels are in `pixelSize` units. */
    fun entityToCanvas(pointX: Int, pointY: Int, render: ValetudoMapRender): Offset =
        toCanvas(
            pointX.toFloat() / render.pixelSize,
            pointY.toFloat() / render.pixelSize,
            render
        )

    fun segmentAt(render: ValetudoMapRender, tap: Offset): String? {
        if (scale <= 0f) return null
        val mapX = (tap.x - left) / scale + render.originX
        val mapY = (tap.y - top) / scale + render.originY
        return render.segmentAt(mapX.roundToInt(), mapY.roundToInt())
    }
}

private fun DrawScope.drawSelectionRing(
    info: ValetudoSegmentInfo,
    placement: MapPlacement,
    render: ValetudoMapRender
) {
    val center = placement.toCanvas(info.centerX, info.centerY, render)
    val radius = max(12f, 18f * placement.scale)
    drawCircle(Color.White.copy(alpha = 0.9f), radius, center, style = Stroke(max(2f, placement.scale)))
    drawCircle(Color.White.copy(alpha = 0.18f), radius, center)
}

private fun DrawScope.drawPath(render: ValetudoMapRender, placement: MapPlacement) {
    // Travelled and predicted paths are both simple polylines of x,y pairs.
    listOfNotNull(render.pathPoints, render.predictedPathPoints).forEach { series ->
        var i = 0
        var previous: Offset? = null
        while (i + 1 < series.size) {
            val point = placement.entityToCanvas(series[i], series[i + 1], render)
            previous?.let {
                drawLine(PathColor.copy(alpha = 0.75f), it, point, strokeWidth = max(1f, placement.scale * 0.5f))
            }
            previous = point
            i += 2
        }
    }
}

private fun DrawScope.drawCharger(render: ValetudoMapRender, placement: MapPlacement) {
    val points = render.chargerPoint ?: return
    val center = placement.entityToCanvas(points.first, points.second, render)
    val radius = max(5f, 4f * placement.scale)
    drawCircle(ChargerColor.copy(alpha = 0.28f), radius * 1.9f, center)
    drawCircle(ChargerColor, radius, center)
}

private fun DrawScope.drawRobot(render: ValetudoMapRender, placement: MapPlacement) {
    val points = render.robotPoint ?: return
    val center = placement.entityToCanvas(points.first, points.second, render)
    val radius = max(6f, 5f * placement.scale)

    drawCircle(RobotColor.copy(alpha = 0.25f), radius * 2f, center)
    drawCircle(RobotColor, radius, center)
    drawCircle(Color.White.copy(alpha = 0.85f), radius, center, style = Stroke(max(1f, placement.scale * 0.4f)))

    // Heading indicator. Valetudo's angle is degrees clockwise from +x, matching Canvas' y-down axis.
    render.robotAngle?.let { angle ->
        val radians = Math.toRadians(angle.toDouble())
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(
                center.x + (radius * 1.8f) * cos(radians).toFloat(),
                center.y + (radius * 1.8f) * sin(radians).toFloat()
            ),
            strokeWidth = max(1.5f, placement.scale * 0.5f)
        )
    }
}


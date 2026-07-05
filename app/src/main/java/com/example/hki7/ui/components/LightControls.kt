package com.example.hki7.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlin.math.*

/** Shared height for every vertical slider/switch control (fan speed, humidifier target, climate
 * temp, brightness, color temp, lock, switch, cover position/tilt) so they all look consistent
 * regardless of which dialog hosts them — matches the original light dialog slider height. */
val VerticalControlHeight = 300.dp

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier.height(VerticalControlHeight),
    gradient: Brush? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.DarkGray.copy(alpha = 0.5f)
) {
    BoxWithConstraints(
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onValueChangeFinished() }
                ) { change, _ ->
                    val newValue = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newValue = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        val constraintsScope = this
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0.01f, 1f))
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .background(gradient ?: SolidColor(activeColor))
        )
        
        // Value indicator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 8.dp)
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .offset(y = -(constraintsScope.maxHeight * value) + 2.dp)
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun ColorWheel(
    selectedRgb: List<Int>?,
    onColorSelected: (List<Int>) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }
    var isDraggingWheel by remember { mutableStateOf(false) }

    fun colorAt(offset: Offset): List<Int>? {
        val currentRadius = radius.takeIf { it > 0f && it.isFinite() } ?: return null
        val relativeOffset = offset - center
        val distance = relativeOffset.getDistance()
        if (!distance.isFinite() || distance > currentRadius) return null

        val angle = atan2(relativeOffset.y, relativeOffset.x) * 180 / PI
        val normalizedAngle = if (angle < 0) (angle + 360).toFloat() else angle.toFloat()
        val saturation = (distance / currentRadius).coerceIn(0f, 1f)
        val color = Color.hsv(normalizedAngle, saturation, 1f)
        return listOf(
            (color.red * 255).toInt().coerceIn(0, 255),
            (color.green * 255).toInt().coerceIn(0, 255),
            (color.blue * 255).toInt().coerceIn(0, 255)
        )
    }

    Canvas(
        modifier = modifier
            .size(280.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDraggingWheel = colorAt(offset) != null
                    },
                    onDragEnd = {
                        if (isDraggingWheel) onValueChangeFinished()
                        isDraggingWheel = false
                    },
                    onDragCancel = { isDraggingWheel = false }
                ) { change, _ ->
                    if (isDraggingWheel) {
                        colorAt(change.position)?.let {
                            change.consume()
                            onColorSelected(it)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    colorAt(offset)?.let {
                        onColorSelected(it)
                        onValueChangeFinished()
                    }
                }
            }
    ) {
        center = Offset(size.width / 2, size.height / 2)
        radius = size.minDimension / 2

        // Draw the color spectrum
        val colors = List(360) { Color.hsv(it.toFloat(), 1f, 1f) }
        drawCircle(
            brush = Brush.sweepGradient(colors, center),
            radius = radius
        )
        // Layer white in the center for saturation
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius
        )
        
        // Selector Position Calculation
        val hsv = FloatArray(3)
        if (selectedRgb != null && selectedRgb.size >= 3) {
            android.graphics.Color.RGBToHSV(
                selectedRgb[0].coerceIn(0, 255),
                selectedRgb[1].coerceIn(0, 255),
                selectedRgb[2].coerceIn(0, 255),
                hsv
            )
        } else {
            // Default to center if no color
            hsv[0] = 0f
            hsv[1] = 0f
            hsv[2] = 1f
        }
        
        val angleRad = hsv[0] * PI / 180
        val saturation = hsv[1]
        val selectorX = center.x + cos(angleRad).toFloat() * saturation * radius
        val selectorY = center.y + sin(angleRad).toFloat() * saturation * radius
        
        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = Offset(selectorX, selectorY),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal brightness bar — drag or tap to set position; row handles toggle.
// Fill width = brightness; white handlebar marks current position.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HorizontalLightBar(
    entity: HAEntity,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val brightness = if (isOn) (entity.brightness ?: 0) / 255f else 0f

    var localBrightness by remember(entity.entity_id) { mutableFloatStateOf(brightness) }
    LaunchedEffect(entity.brightness, entity.state) {
        localBrightness = if (isOn) (entity.brightness ?: 0) / 255f else 0f
    }

    val currentColor = lightStateColor(entity) ?: Color(0xFFFFB35C)
    val barGradient = Brush.horizontalGradient(listOf(currentColor.copy(alpha = 0.2f), currentColor))

    val handlebarWidth = 3.dp
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(appColors.elevated)
            .pointerInput(entity.entity_id + "_drag") {
                detectHorizontalDragGestures(
                    onDragEnd = { viewModel.setBrightness(entity.entity_id, localBrightness.coerceAtLeast(0.04f)) },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        val newVal = (localBrightness + amount / size.width).coerceIn(0.04f, 1f)
                        localBrightness = newVal
                        viewModel.setOptimisticBrightness(entity.entity_id, newVal)
                    }
                )
            }
            .pointerInput(entity.entity_id + "_tap") {
                detectTapGestures { offset ->
                    // Tap sets brightness at that position instead of toggling
                    val newVal = (offset.x / size.width).coerceIn(0.04f, 1f)
                    localBrightness = newVal
                    viewModel.setBrightness(entity.entity_id, newVal)
                }
            }
    ) {
        if (localBrightness > 0f) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(localBrightness).background(barGradient))
        }
        // Handlebar at current position
        if (isOn && localBrightness > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.6f)
                    .width(handlebarWidth)
                    .absoluteOffset(x = (maxWidth - handlebarWidth) * localBrightness)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }
        if (!isOn) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Off", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal color-temperature bar — full cool→warm gradient, marker at current CT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HorizontalColorTempBar(
    entity: HAEntity,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    if (!entity.supportsColorTemp) {
        Box(modifier.clip(RoundedCornerShape(12.dp)).background(appColors.elevated))
        return
    }
    val minK = (entity.minKelvin ?: 2000).toFloat()
    val maxK = (entity.maxKelvin ?: 6500).toFloat()
    val currentK = entity.colorTempKelvin?.toFloat() ?: ((minK + maxK) / 2f)

    var localKelvin by remember(entity.entity_id) { mutableFloatStateOf(currentK) }
    LaunchedEffect(entity.colorTempKelvin) {
        localKelvin = entity.colorTempKelvin?.toFloat() ?: ((minK + maxK) / 2f)
    }

    val position = ((localKelvin - minK) / (maxK - minK)).coerceIn(0f, 1f)
    // minK (low Kelvin) is warm/orange; maxK (high Kelvin) is cool/blue.
    val ctGradient = Brush.horizontalGradient(listOf(Color(0xFFFFB35C), Color(0xFFBFD9FF)))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ctGradient)
            .pointerInput(entity.entity_id + "_drag") {
                detectHorizontalDragGestures(
                    onDragEnd = { viewModel.setColorTemp(entity.entity_id, localKelvin.toInt()) },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        localKelvin = (localKelvin + amount / size.width * (maxK - minK)).coerceIn(minK, maxK)
                    }
                )
            }
            .pointerInput(entity.entity_id + "_tap") {
                detectTapGestures { offset ->
                    val newK = minK + (offset.x / size.width) * (maxK - minK)
                    localKelvin = newK.coerceIn(minK, maxK)
                    viewModel.setColorTemp(entity.entity_id, localKelvin.toInt())
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val markerX = position * size.width
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(markerX, 4f),
                end = Offset(markerX, size.height - 4f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal hue bar — full rainbow, marker at current hue
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HorizontalHueBar(
    entity: HAEntity,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    if (!entity.supportsColor) {
        Box(modifier.clip(RoundedCornerShape(12.dp)).background(appColors.elevated))
        return
    }

    fun rgbToHue(rgb: List<Int>?): Float {
        if (rgb == null || rgb.size < 3) return 0f
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(rgb[0].coerceIn(0,255), rgb[1].coerceIn(0,255), rgb[2].coerceIn(0,255), hsv)
        return hsv[0]
    }

    val currentHue = remember(entity.rgbColor) { rgbToHue(entity.rgbColor) }
    var localHue by remember(entity.entity_id) { mutableFloatStateOf(currentHue) }
    LaunchedEffect(entity.rgbColor) { localHue = rgbToHue(entity.rgbColor) }

    val hueGradient = Brush.horizontalGradient(List(13) { Color.hsv(it * 30f, 1f, 1f) })

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(hueGradient)
            .pointerInput(entity.entity_id + "_drag") {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val c = Color.hsv(localHue, 1f, 1f)
                        viewModel.setRgbColor(entity.entity_id, listOf((c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt()))
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        localHue = ((localHue + amount / size.width * 360f) % 360f + 360f) % 360f
                    }
                )
            }
            .pointerInput(entity.entity_id + "_tap") {
                detectTapGestures { offset ->
                    localHue = ((offset.x / size.width) * 360f).coerceIn(0f, 359.9f)
                    val c = Color.hsv(localHue, 1f, 1f)
                    viewModel.setRgbColor(entity.entity_id, listOf((c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt()))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val markerX = (localHue / 360f) * size.width
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(markerX, 4f),
                end = Offset(markerX, size.height - 4f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

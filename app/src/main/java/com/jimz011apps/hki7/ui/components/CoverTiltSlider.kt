@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Vertical tilt-angle control for covers that support `current_tilt_position`, drawn as venetian
 * blind slats (tight/near-opaque when closed, wide gaps when open) under a fixed headrail cap —
 * meant to sit next to the position [VerticalSlider]. Same drag/tap gesture convention (bottom =
 * 0, top = 1) as [VerticalSlider] for consistency between the two.
 */
@Composable
fun CoverTiltSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    BoxWithConstraints(
        modifier = modifier
            .height(VerticalControlHeight)
            .width(100.dp)
            .clip(itemCornerShape())
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { onValueChangeFinished() }) { change, _ ->
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
        Canvas(
            modifier = Modifier
                .padding(top = 26.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)
                .align(Alignment.TopCenter)
                .then(Modifier.height(maxHeight - 36.dp).width(80.dp))
        ) {
            val slatCount = 16
            val gapFraction = value.coerceIn(0f, 1f)
            val rowHeight = size.height / slatCount
            val minGap = 1.dp.toPx()
            repeat(slatCount) { i ->
                val slatThickness = (rowHeight - minGap - (rowHeight - minGap) * gapFraction * 0.75f)
                    .coerceAtLeast(2.dp.toPx())
                val top = i * rowHeight
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.85f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, slatThickness),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
        // Fixed decorative headrail — the mounting box sits at the top regardless of tilt.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(52.dp)
                .height(10.dp)
                .clip(itemCornerShape())
                .background(Color.White.copy(alpha = 0.9f))
        )
    }
}

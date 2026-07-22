package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Softly fades the top and/or bottom edge of a vertically scrolling container to hint that more
 * content lies off-screen. The fade only appears on an edge that can still be scrolled toward, so a
 * fully-scrolled (or non-scrolling) container shows a crisp edge.
 *
 * Apply *before* the scroll modifier so it composites the already-scrolled content:
 * ```
 * Column(Modifier.fadingEdges(scrollState).verticalScroll(scrollState)) { … }
 * LazyColumn(Modifier.fadingEdges(listState), state = listState) { … }
 * ```
 *
 * Works on any background because it fades the content itself to transparent (BlendMode.DstIn)
 * rather than painting a solid scrim over it.
 */
fun Modifier.fadingEdges(scrollState: ScrollState, edgeLength: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val lengthPx = edgeLength.toPx().coerceAtLeast(1f)
        val topFade = (scrollState.value / lengthPx).coerceIn(0f, 1f)
        val bottomFade = ((scrollState.maxValue - scrollState.value) / lengthPx).coerceIn(0f, 1f)
        drawVerticalFade(topFade, bottomFade, lengthPx)
    }

/** [fadingEdges] for a `LazyColumn` or another lazy list. */
fun Modifier.fadingEdges(state: LazyListState, edgeLength: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val lengthPx = edgeLength.toPx().coerceAtLeast(1f)
        val topFade = if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / lengthPx).coerceIn(0f, 1f)
        val bottomFade = if (state.canScrollForward) 1f else 0f
        drawVerticalFade(topFade, bottomFade, lengthPx)
    }

/** [fadingEdges] for a `LazyVerticalGrid` or another lazy grid. */
fun Modifier.fadingEdges(state: LazyGridState, edgeLength: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val lengthPx = edgeLength.toPx().coerceAtLeast(1f)
        val topFade = if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / lengthPx).coerceIn(0f, 1f)
        val bottomFade = if (state.canScrollForward) 1f else 0f
        drawVerticalFade(topFade, bottomFade, lengthPx)
    }

// Offscreen compositing (set by callers) is required for DstIn to erase already-drawn content.
private fun DrawScope.drawVerticalFade(topFade: Float, bottomFade: Float, lengthPx: Float) {
    if (topFade > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 1f - topFade), Color.Black),
                startY = 0f,
                endY = lengthPx
            ),
            blendMode = BlendMode.DstIn
        )
    }
    if (bottomFade > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - bottomFade)),
                startY = size.height - lengthPx,
                endY = size.height
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

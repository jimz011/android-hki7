package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/** Width of one tab while the bar is scrollable; equal-width [Modifier.weight] tabs are used
 *  otherwise. Callers must size their tabs with this so the scroll-into-view maths below lines up. */
val HKIBottomBarTabWidth: Dp = 68.dp

/** Gap between tabs in scrollable mode. */
private val TabSpacing = 4.dp

/** Inset at both ends of the scrollable row. */
private val TabRowPadding = 10.dp

@Composable
fun HKIBottomBar(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 64.dp,
    containerColor: Color? = null,
    scrollable: Boolean = false,
    showContainer: Boolean = true,
    /** Index of the selected tab in [content]. When the bar scrolls, that tab is kept centred so it
     *  never sits off-screen — and its neighbours stay visible as a hint that there is more to reach.
     *  Null (or non-scrollable mode) leaves the scroll position alone. */
    selectedIndex: Int? = null,
    content: @Composable RowScope.() -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val barColor = containerColor ?: appColors.surface.copy(alpha = 0.9f)
    val barShape = itemCornerShape()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    // Tabs are fixed-width in scrollable mode, so the target offset is arithmetic — no per-item
    // onGloballyPositioned bookkeeping needed. maxValue is a key because the row is measured after
    // the first composition: without it the very first selection would scroll against a 0 range.
    if (scrollable && selectedIndex != null) {
        LaunchedEffect(selectedIndex, scrollState.maxValue, viewportWidthPx) {
            if (scrollState.maxValue <= 0 || viewportWidthPx <= 0) return@LaunchedEffect
            val tabWidthPx = with(density) { HKIBottomBarTabWidth.toPx() }
            val spacingPx = with(density) { TabSpacing.toPx() }
            val paddingPx = with(density) { TabRowPadding.toPx() }
            val tabStart = paddingPx + selectedIndex * (tabWidthPx + spacingPx)
            val centred = tabStart - (viewportWidthPx - tabWidthPx) / 2f
            scrollState.animateScrollTo(centred.toInt().coerceIn(0, scrollState.maxValue))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, bottom = 15.dp)
            .height(64.dp)
            .then(
                if (showContainer) {
                    // Soft shadow + hairline border lift the floating bar off busy page content.
                    Modifier
                        .shadow(10.dp, barShape)
                        .clip(barShape)
                        .background(surfaceGradient(barColor))
                        .border(1.dp, appColors.onMuted.copy(alpha = 0.10f), barShape)
                } else Modifier
            )
    ) {
        // weight()-based equal-width tabs can't live in a scrollable Row (unbounded width),
        // so scrollable mode uses fixed-width tabs with spacing instead of SpaceEvenly.
        Row(
            modifier = if (scrollable) {
                Modifier
                    .fillMaxHeight()
                    .onSizeChanged { viewportWidthPx = it.width }
                    .horizontalScroll(scrollState)
                    .padding(horizontal = TabRowPadding)
            } else {
                Modifier.fillMaxSize()
            },
            horizontalArrangement = if (scrollable) Arrangement.spacedBy(TabSpacing) else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Edge affordances: shown only while the bar actually overflows, and only on the side that
        // still has content left to reach — no left chevron at the start, none on the right at the end.
        if (scrollable) {
            ScrollEdgeChevron(
                visible = scrollState.canScrollBackward,
                icon = Icons.Default.ChevronLeft,
                barColor = barColor,
                fromStart = true,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            ScrollEdgeChevron(
                visible = scrollState.canScrollForward,
                icon = Icons.Default.ChevronRight,
                barColor = barColor,
                fromStart = false,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/** Chevron + fade at one edge of a scrollable bar, marking that there is more to scroll that way. */
@Composable
private fun ScrollEdgeChevron(
    visible: Boolean,
    icon: ImageVector,
    barColor: Color,
    fromStart: Boolean,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(30.dp)
                .background(
                    Brush.horizontalGradient(
                        // Fade the bar colour over the content it overlaps so tabs slide out of
                        // sight instead of being abruptly clipped by the chevron.
                        if (fromStart) listOf(barColor, Color.Transparent)
                        else listOf(Color.Transparent, barColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = appColors.onMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

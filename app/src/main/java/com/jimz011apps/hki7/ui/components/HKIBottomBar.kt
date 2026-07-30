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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

@Composable
fun HKIBottomBar(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 64.dp,
    containerColor: Color? = null,
    scrollable: Boolean = false,
    showContainer: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val barColor = containerColor ?: appColors.surface.copy(alpha = 0.9f)
    val barShape = itemCornerShape()
    val scrollState = rememberScrollState()

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
                Modifier.fillMaxHeight().horizontalScroll(scrollState).padding(horizontal = 10.dp)
            } else {
                Modifier.fillMaxSize()
            },
            horizontalArrangement = if (scrollable) Arrangement.spacedBy(4.dp) else Arrangement.SpaceEvenly,
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

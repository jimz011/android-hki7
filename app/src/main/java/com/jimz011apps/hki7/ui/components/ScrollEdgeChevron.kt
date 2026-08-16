package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/**
 * Chevron + fade at one edge of a horizontally scrolling bar, marking that there is more to reach
 * that way. Pass [visible] from the matching `canScrollBackward`/`canScrollForward` so each side
 * only appears while that direction still has somewhere to go — a chevron that is always there says
 * nothing about whether you are at the end.
 *
 * Shared by the bottom navigation bar and the pull-down header menu, so a bar that overflows is
 * advertised the same way wherever it happens.
 */
@Composable
internal fun ScrollEdgeChevron(
    visible: Boolean,
    /** Colour the bar fades to under the chevron, so content slides out of sight rather than being
     *  abruptly clipped. Use a translucent scrim where the backdrop is artwork rather than a flat
     *  surface. */
    fadeColor: Color,
    /** Which edge this sits on, in layout terms. The arrow direction is derived from it, so a
     *  right-to-left layout points the reader at the content that is actually off-screen. */
    fromStart: Boolean,
    modifier: Modifier = Modifier,
    /** Defaults to the muted on-surface colour; override where the bar sits on its own backdrop. */
    contentColor: Color? = null
) {
    val appColors = LocalHKIAppColors.current
    val pointsLeft = if (LocalLayoutDirection.current == LayoutDirection.Ltr) fromStart else !fromStart
    val icon = if (pointsLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight
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
                        if (fromStart) listOf(fadeColor, Color.Transparent)
                        else listOf(Color.Transparent, fadeColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor ?: appColors.onMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

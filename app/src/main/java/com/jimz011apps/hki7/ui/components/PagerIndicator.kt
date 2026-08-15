package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/** Past this many pages the dots stop being countable at a glance and a "4 / 19" pill says the same
 *  thing in less space. */
private const val MaxDots = 8

/** Vertical space [PagerIndicator] occupies, including its own bottom gap. Surfaces that float above
 *  scrolling content add this to their scroll reserve; keep it in step with the padding below. */
val PagerIndicatorHeight: Dp = 38.dp

/**
 * Where you are among a set of pages, and — as much to the point — that there are others to swipe
 * to. Shared by the room pager and by dialog tab strips, so the same gesture is advertised the same
 * way wherever it exists.
 */
@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    /** Flat against the surface it sits on, for indicators inside a dialog rather than floating
     *  over page content. */
    elevated: Boolean = true
) {
    if (pageCount < 2) return
    val appColors = LocalHKIAppColors.current
    val active = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (elevated) appColors.surface.copy(alpha = 0.9f) else androidx.compose.ui.graphics.Color.Transparent,
        contentColor = appColors.onSurface,
        shadowElevation = if (elevated) 6.dp else 0.dp
    ) {
        if (pageCount <= MaxDots) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val selected = index == currentPage
                    // The current page is a stadium rather than a bigger dot, so position reads
                    // without relying on a size difference alone.
                    val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "pagerDotWidth")
                    Box(
                        Modifier
                            .size(width = width, height = 6.dp)
                            .background(
                                if (selected) active else appColors.onMuted.copy(alpha = 0.35f),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        } else {
            Text(
                text = "${currentPage + 1} / $pageCount",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = appColors.onSurface
            )
        }
    }
}

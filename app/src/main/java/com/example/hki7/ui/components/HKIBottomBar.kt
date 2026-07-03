package com.example.hki7.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hki7.ui.theme.LocalHKIAppColors

@Composable
fun HKIBottomBar(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 64.dp,
    containerColor: Color? = null,
    scrollable: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val barColor = containerColor ?: appColors.surface.copy(alpha = 0.9f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, bottom = 15.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(barColor)
    ) {
        // weight()-based equal-width tabs can't live in a scrollable Row (unbounded width),
        // so scrollable mode uses fixed-width tabs with spacing instead of SpaceEvenly.
        Row(
            modifier = if (scrollable) {
                Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp)
            } else {
                Modifier.fillMaxSize()
            },
            horizontalArrangement = if (scrollable) Arrangement.spacedBy(4.dp) else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

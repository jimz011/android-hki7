package com.jimz011apps.hki7.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Number of full-width dashboard lanes available at [width].
 *
 * These are the shared Home/room-detail breakpoints. Specialised dashboards use the same lanes so
 * a foldable or tablet gains useful columns without changing the established phone layout.
 */
fun responsiveDashboardColumnCount(width: Dp): Int = when {
    width >= 900.dp -> 3
    width >= 600.dp -> 2
    else -> 1
}

/** Auto-fit compact tiles without letting a newly added column make every tile unreadably narrow. */
fun responsiveDashboardTileCount(
    width: Dp,
    minimumTileWidth: Dp = 150.dp,
    spacing: Dp = 10.dp,
    maximumColumns: Int = 6,
): Int = ((width + spacing) / (minimumTileWidth + spacing)).toInt().coerceIn(1, maximumColumns)

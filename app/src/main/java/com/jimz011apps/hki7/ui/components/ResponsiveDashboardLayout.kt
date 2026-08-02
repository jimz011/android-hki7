package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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

/**
 * Height-aware dashboard grid used by the wider specialist views.
 *
 * Unlike a collection of Rows, a short card does not inherit the height of the tallest card next
 * to it. [itemSpans] also lets Climate retain its full/half/third card widths; every item is placed
 * in the consecutive slots whose current bottom edge is highest, choosing the lowest resulting Y.
 */
@Composable
fun DashboardMasonryLayout(
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 0.dp,
    itemSpans: List<Int> = emptyList(),
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val columnCount = columns.coerceAtLeast(1)
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()
        val availableWidth = (constraints.maxWidth - horizontalSpacingPx * (columnCount - 1)).coerceAtLeast(0)
        val columnWidth = availableWidth / columnCount
        val remainder = availableWidth % columnCount
        val columnStarts = IntArray(columnCount)
        val columnWidths = IntArray(columnCount) { index -> columnWidth + if (index < remainder) 1 else 0 }
        for (index in 1 until columnCount) {
            columnStarts[index] = columnStarts[index - 1] + columnWidths[index - 1] + horizontalSpacingPx
        }

        val bottoms = IntArray(columnCount)
        data class PositionedItem(val x: Int, val y: Int, val placeable: androidx.compose.ui.layout.Placeable)
        val positioned = measurables.mapIndexed { index, measurable ->
            val span = itemSpans.getOrNull(index)?.coerceIn(1, columnCount) ?: 1
            var bestColumn = 0
            var bestY = Int.MAX_VALUE
            for (start in 0..columnCount - span) {
                val candidateY = (start until start + span).maxOf { bottoms[it] }
                if (candidateY < bestY) {
                    bestY = candidateY
                    bestColumn = start
                }
            }
            val itemWidth = (bestColumn until bestColumn + span).sumOf { columnWidths[it] } +
                horizontalSpacingPx * (span - 1)
            val placeable = measurable.measure(
                constraints.copy(minWidth = itemWidth, maxWidth = itemWidth, minHeight = 0)
            )
            val nextBottom = bestY + placeable.height + verticalSpacingPx
            for (slot in bestColumn until bestColumn + span) bottoms[slot] = nextBottom
            PositionedItem(columnStarts[bestColumn], bestY, placeable)
        }
        val measuredHeight = (bottoms.maxOrNull() ?: 0).let {
            if (positioned.isEmpty()) 0 else (it - verticalSpacingPx).coerceAtLeast(0)
        }
        layout(constraints.maxWidth, measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            positioned.forEach { it.placeable.placeRelative(it.x, it.y) }
        }
    }
}

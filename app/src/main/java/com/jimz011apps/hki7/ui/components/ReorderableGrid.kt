@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

enum class ReorderAxis { Grid, Vertical }

@Composable
fun <T> ReorderableGrid(
    items: List<T>,
    canReorder: Boolean,
    onReorder: (Int, Int) -> Unit,
    key: (T) -> Any,
    columns: androidx.compose.foundation.lazy.grid.GridCells,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    isNested: Boolean = false,
    dragHandleHeight: Int? = null,
    axis: ReorderAxis = ReorderAxis.Grid,
    state: LazyGridState = rememberLazyGridState(),
    autoScrollState: ScrollState? = null,
    span: (T) -> Int = { 1 },
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val localDensity = LocalDensity.current
    val density = localDensity.density
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialDraggedIndex by remember { mutableStateOf<Int?>(null) }
    var pendingReorderTarget by remember { mutableStateOf<Int?>(null) }
    // Window-space Y of the dragged item, used to drive edge auto-scroll of an outer container.
    var dragWindowY by remember { mutableStateOf<Float?>(null) }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    
    // Stable list management to prevent resets during polling
    var currentList by remember { mutableStateOf(items) }
    val onReorderState = rememberUpdatedState(onReorder)

    // Resync to the authoritative items whenever items change OR reorder is toggled
    // (e.g. exiting edit mode) so the grid never shows a stale local order after saving.
    LaunchedEffect(items, canReorder) {
        if (draggedIndex == null) {
            currentList = items
        }
    }

    fun swapTargetFor(dragPosition: Offset, currentIndex: Int, amount: Offset): Int? {
        val visible = state.layoutInfo.visibleItemsInfo
        val current = visible.find { it.index == currentIndex } ?: return null
        if (axis == ReorderAxis.Vertical) {
            val targetIndex = when {
                amount.y < 0 -> currentIndex - 1
                amount.y > 0 -> currentIndex + 1
                else -> return null
            }.takeIf { it in currentList.indices } ?: return null
            val target = visible.find { it.index == targetIndex } ?: return null
            val targetCenterY = target.offset.y + target.size.height / 2f
            val crossed = if (targetIndex < currentIndex) {
                dragPosition.y < targetCenterY
            } else {
                dragPosition.y > targetCenterY
            }
            return if (crossed) targetIndex else null
        }
        val columnsCount = visible
            .filter { kotlin.math.abs(it.offset.y - current.offset.y) < current.size.height / 2 }
            .size
            .coerceAtLeast(1)

        val targetIndex = when {
            kotlin.math.abs(amount.y) >= kotlin.math.abs(amount.x) && amount.y < 0 -> currentIndex - columnsCount
            kotlin.math.abs(amount.y) >= kotlin.math.abs(amount.x) && amount.y > 0 -> currentIndex + columnsCount
            amount.x < 0 -> currentIndex - 1
            amount.x > 0 -> currentIndex + 1
            else -> return null
        }.takeIf { it in currentList.indices } ?: return null

        val target = visible.find { it.index == targetIndex } ?: return null
        val targetCenter = Offset(
            target.offset.x + target.size.width / 2f,
            target.offset.y + target.size.height / 2f
        )
        val crossed = when {
            targetIndex < currentIndex && kotlin.math.abs(amount.y) >= kotlin.math.abs(amount.x) -> dragPosition.y < targetCenter.y
            targetIndex > currentIndex && kotlin.math.abs(amount.y) >= kotlin.math.abs(amount.x) -> dragPosition.y > targetCenter.y
            targetIndex < currentIndex -> dragPosition.x < targetCenter.x
            else -> dragPosition.x > targetCenter.x
        }
        return if (crossed) targetIndex else null
    }

    val pointerModifier = if (canReorder) {
        Modifier.pointerInput(isNested, dragHandleHeight, axis) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    state.layoutInfo.visibleItemsInfo
                        .firstOrNull { item ->
                            val isWithinX = offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width)
                            val isWithinY = offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                            
                            if (isWithinX && isWithinY) {
                                if (dragHandleHeight != null) {
                                    val relativeY = offset.y - item.offset.y
                                    relativeY < dragHandleHeight * density
                                } else true
                            } else false
                        }?.also {
                            draggedIndex = it.index
                            initialDraggedIndex = it.index
                        }
                },
                onDrag = { change, amount ->
                    if (draggedIndex != null) {
                        change.consume()
                        dragOffset += amount
                        
                        val currentIndex = draggedIndex!!
                        val itemInView = state.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
                        val dragPosition = itemInView?.let {
                            Offset(
                                it.offset.x + dragOffset.x + it.size.width / 2f,
                                it.offset.y + dragOffset.y + it.size.height / 2f
                            )
                        }
                        if (dragPosition != null) {
                            if (autoScrollState != null) {
                                gridCoords?.let { coords ->
                                    dragWindowY = coords.localToWindow(dragPosition).y
                                }
                            }
                            val targetIndex = swapTargetFor(dragPosition, currentIndex, amount)
                            if (targetIndex != null && targetIndex != currentIndex) {
                                val targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
                                val newList = currentList.toMutableList()
                                val item = newList.removeAt(currentIndex)
                                val safeTarget = targetIndex.coerceIn(0, newList.size)
                                newList.add(safeTarget, item)
                                currentList = newList
                                draggedIndex = safeTarget
                                pendingReorderTarget = safeTarget
                                dragOffset = if (targetItem != null) {
                                    Offset(
                                        x = itemInView.offset.x + dragOffset.x - targetItem.offset.x,
                                        y = itemInView.offset.y + dragOffset.y - targetItem.offset.y
                                    )
                                } else {
                                    dragOffset
                                }
                            }
                        }
                    }
                },
                onDragEnd = {
                    val from = initialDraggedIndex
                    val to = pendingReorderTarget
                    if (from != null && to != null && from != to) {
                        onReorderState.value(from, to)
                    }
                    draggedIndex = null
                    initialDraggedIndex = null
                    pendingReorderTarget = null
                    dragOffset = Offset.Zero
                    dragWindowY = null
                },
                onDragCancel = {
                    currentList = items
                    draggedIndex = null
                    initialDraggedIndex = null
                    pendingReorderTarget = null
                    dragOffset = Offset.Zero
                    dragWindowY = null
                }
            )
        }
    } else Modifier

    // Edge auto-scroll: while dragging, if the dragged item nears the top/bottom of the screen,
    // scroll the provided outer container and keep the item under the finger.
    if (autoScrollState != null) {
        LaunchedEffect(draggedIndex) {
            if (draggedIndex == null) return@LaunchedEffect
            val edgePx = with(localDensity) { 130.dp.toPx() }
            val maxStep = with(localDensity) { 16.dp.toPx() }
            while (draggedIndex != null) {
                val y = dragWindowY
                if (y != null) {
                    val delta = when {
                        y < edgePx -> -maxStep * ((edgePx - y) / edgePx).coerceIn(0f, 1f)
                        y > screenHeightPx - edgePx -> maxStep * ((y - (screenHeightPx - edgePx)) / edgePx).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    if (delta != 0f) {
                        val consumed = autoScrollState.scrollBy(delta)
                        if (consumed != 0f) {
                            dragOffset = Offset(dragOffset.x, dragOffset.y + consumed)
                        }
                    }
                }
                withFrameNanos { }
            }
        }
    }

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = modifier
            .then(pointerModifier)
            .then(if (autoScrollState != null) Modifier.onGloballyPositioned { gridCoords = it } else Modifier),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        userScrollEnabled = !isNested
    ) {
        items(
            count = currentList.size,
            key = { index -> key(currentList[index]) },
            span = { index -> GridItemSpan(span(currentList[index]).coerceIn(1, maxLineSpan)) }
        ) { index ->
            val item = currentList[index]
            val isDragging = draggedIndex == index
            
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 10f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.02f
                            scaleY = 1.02f
                            alpha = 0.8f
                        } else {
                            scaleX = 1f
                            scaleY = 1f
                        }
                    }
            ) {
                itemContent(item, isDragging)
            }
        }
    }
}

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlin.math.abs
import kotlin.math.max

/**
 * Observes horizontal swipes for tab/page navigation without ever consuming pointer events.
 *
 * Reliability rules (all decided from the raw finger movement, so a vertically scrolling child
 * that consumes events can no longer freeze the tally and silently swallow the swipe):
 * - The gesture's axis is locked once the raw travel crosses touch slop. A vertically dominant
 *   gesture never navigates; a horizontally dominant one keeps counting even if a vertical list
 *   nibbles at it afterward.
 * - When [respectChildGestures] is set, only a child that consumes *horizontal* movement (sliders,
 *   nested pagers, horizontal rows) owns the gesture and suppresses navigation. Consuming the down
 *   — which every tap target does — or consuming vertical scroll does not count, so a swipe that
 *   starts on a card or button pages the same as one starting on empty space.
 * - Navigation fires mid-gesture as soon as the finger travels [commitDistancePx], instead of
 *   waiting for the finger to lift — this is what makes paging feel immediate.
 * - Short flicks that lift before the commit distance still navigate when the tracked velocity
 *   (measured with [VelocityTracker], not a whole-gesture average) exceeds
 *   [flingVelocityPxPerSecond] after at least [flingDistancePx] of travel.
 *
 * [onDown] reports the touch position at gesture start. [onMove] observes every raw drag total
 * and may return true to claim the gesture for the caller (e.g. an edge-drawer region), which
 * suppresses navigation while keeping the observation loop alive.
 */
suspend fun PointerInputScope.awaitHorizontalTabSwipes(
    respectChildGestures: Boolean,
    pass: PointerEventPass,
    commitDistancePx: Float,
    flingDistancePx: Float,
    flingVelocityPxPerSecond: Float,
    onDown: ((Offset) -> Unit)? = null,
    onMove: ((totalX: Float, totalY: Float) -> Boolean)? = null,
    onSwipe: (forward: Boolean) -> Unit
) {
    val touchSlopPx = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
        onDown?.invoke(down.position)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPointerInputChange(down)
        var change = down
        var totalX = 0f
        var totalY = 0f
        var horizontalLocked = false
        var verticalLocked = false
        var claimedX = 0f
        var claimed = false
        var committed = false

        while (change.pressed) {
            val event = awaitPointerEvent(pass)
            change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChangeIgnoreConsumed()
            totalX += delta.x
            totalY += delta.y
            velocityTracker.addPointerInputChange(change)
            if (onMove?.invoke(totalX, totalY) == true) claimed = true
            // Only horizontally dominant movement a child actually swallowed counts as that child
            // owning the drag — a brightness slider keeps its gesture, while a card consuming the
            // down for its tap and a list consuming vertical scroll leave paging alone.
            if (respectChildGestures && change.isConsumed && abs(delta.x) > abs(delta.y)) {
                claimedX += delta.x
                if (abs(claimedX) >= touchSlopPx) claimed = true
            }
            if (!horizontalLocked && !verticalLocked) {
                if (max(abs(totalX), abs(totalY)) >= touchSlopPx) {
                    if (abs(totalX) > abs(totalY)) horizontalLocked = true else verticalLocked = true
                }
            }
            if (!committed && !claimed && horizontalLocked && abs(totalX) >= commitDistancePx) {
                committed = true
                onSwipe(totalX < 0f)
            }
        }

        if (committed || claimed || !horizontalLocked) return@awaitEachGesture
        val horizontalVelocity = velocityTracker.calculateVelocity().x
        if (abs(totalX) >= flingDistancePx && abs(horizontalVelocity) >= flingVelocityPxPerSecond) {
            onSwipe(totalX < 0f)
        }
    }
}

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlin.math.abs
import kotlin.math.max

/**
 * Observes horizontal swipes for tab navigation without ever consuming pointer events.
 *
 * Used where a real pager cannot be: dialog tab strips, whose pages are `when`-branches of one
 * composable rather than a list, and the app's edge panels, which only need [onDown]/[onMove]. The
 * bottom-bar tabs and the room pager use `HorizontalPager` instead and do not come through here.
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
 * - Navigation is decided when the finger lifts, from where it ended up. Committing mid-gesture
 *   made the first [commitDistancePx] of travel irreversible: dragging back the other way without
 *   lifting could not undo it, which is not how anything else on the platform behaves.
 * - A short flick that lifts before [commitDistancePx] still navigates when the tracked velocity
 *   (measured with [VelocityTracker], not a whole-gesture average) exceeds
 *   [flingVelocityPxPerSecond] after at least [flingDistancePx] of travel.
 *
 * [onDown] reports the touch position at gesture start. [onMove] observes every raw drag total
 * and may return true to claim the gesture for the caller (e.g. an edge-drawer region), which
 * suppresses navigation while keeping the observation loop alive. [onSwipe] may be null for
 * callers that only want the observation hooks.
 */
/**
 * Keeps a horizontal scrollable inside a page from handing the rest of its gesture to the pager.
 *
 * Compose's nested scrolling passes whatever a child could not use up to its parent, so a mode
 * selector, swipe row or swiping-stack widget that ran out of room mid-drag would quietly become a
 * page change — which is worst exactly where it matters, picking the first or last item in a row.
 * Leftover horizontal movement (and any fling that follows it) is swallowed here instead. Vertical
 * is passed through untouched, so page scrolling and the collapsing header still work.
 *
 * Belongs on the pager's page content, not on the pager: a connection above the pager would sit on
 * the far side of the pager's own scrollable and never see the child's leftovers first.
 */
@Composable
fun rememberPagerHandoffBlocker(): NestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = if (source == NestedScrollSource.UserInput) {
            Offset(available.x, 0f)
        } else {
            Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            Velocity(available.x, 0f)
    }
}

suspend fun PointerInputScope.awaitHorizontalTabSwipes(
    respectChildGestures: Boolean,
    pass: PointerEventPass,
    commitDistancePx: Float,
    flingDistancePx: Float,
    flingVelocityPxPerSecond: Float,
    onDown: ((Offset) -> Unit)? = null,
    onMove: ((totalX: Float, totalY: Float) -> Boolean)? = null,
    /** True when the UI is mirrored, so that "forward" is a swipe to the right rather than left. */
    rtl: Boolean = false,
    onSwipe: ((forward: Boolean) -> Unit)? = null
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
        var claimedX = 0f
        var claimed = false

        while (change.pressed) {
            val event = awaitPointerEvent(pass)
            change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChangeIgnoreConsumed()
            totalX += delta.x
            totalY += delta.y
            velocityTracker.addPointerInputChange(change)
            if (onMove?.invoke(totalX, totalY) == true) claimed = true
            // Only *nearly level* movement a child swallowed counts as that child owning the drag.
            // Consumption is a single flag with no axis to it, so "more horizontal than vertical"
            // was not enough to tell a slider apart from a vertical list: on any swipe with a
            // thumb's worth of downward arc the list engages, consumes, and the drag was written
            // off as belonging to a horizontal child. Something genuinely horizontal — a slider, a
            // swipe row — is dragged far flatter than that.
            if (respectChildGestures &&
                change.isConsumed &&
                abs(delta.x) > abs(delta.y) * HorizontalClaimRatio
            ) {
                claimedX += delta.x
                if (abs(claimedX) >= touchSlopPx) claimed = true
            }
        }

        if (onSwipe == null || claimed) return@awaitEachGesture
        // Judged from the whole gesture rather than from its first few pixels. Locking the axis the
        // moment slop was crossed meant a swipe that set off even slightly downwards — which is most
        // of them, on a page that also scrolls — was written off as a scroll and did nothing, however
        // far across it went afterwards. That is what made these swipes feel unreliable.
        val horizontalVelocity = velocityTracker.calculateVelocity().x
        val horizontal = abs(totalX) > abs(totalY) * VerticalTolerance
        val travelled = abs(totalX) >= commitDistancePx
        val flung = abs(totalX) >= flingDistancePx && abs(horizontalVelocity) >= flingVelocityPxPerSecond
        // Forward is towards the end of the reading order: leftwards in English, rightwards in a
        // mirrored language, where the tabs themselves are laid out the other way round.
        if (horizontal && (travelled || flung)) onSwipe(if (rtl) totalX > 0f else totalX < 0f)
    }
}

/** How much more horizontal than vertical a drag must be to count as a sideways swipe. Above 1 so
 *  that a genuine vertical scroll with a little sideways drift is never mistaken for one. */
private const val VerticalTolerance = 1.4f

/** How flat a consumed movement must be before it is read as a horizontal child taking the drag,
 *  rather than a vertical list consuming its share of a diagonal one. */
private const val HorizontalClaimRatio = 3f

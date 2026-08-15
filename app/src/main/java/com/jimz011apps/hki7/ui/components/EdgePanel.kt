package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Which screen edge a panel is anchored to and drags in from, in reading order rather than in
 * pixels: [Start] is the left edge in English and the right edge in Arabic. Everything below works
 * from this plus the layout direction, so the panels mirror as a whole — the alignment, the
 * direction they slide, and the edge the gesture starts from all move together. Mixing the two
 * (a start-aligned sheet that slides off to the left) is how a mirrored drawer ends up sliding the
 * wrong way.
 */
enum class PanelEdge { Start, End }

/** Sign of "towards the [edge]" along X, for the current reading direction. */
private fun hiddenDirection(edge: PanelEdge, rtl: Boolean): Float {
    val startIsLeft = !rtl
    val onLeft = (edge == PanelEdge.Start) == startIsLeft
    return if (onLeft) -1f else 1f
}

/**
 * Open-ness of an edge panel as a 0..1 value the drag writes to directly, so the panel tracks the
 * finger instead of jumping between states.
 *
 * The panels used to be opened by a threshold — 24dp of travel and the sheet animated itself the
 * rest of the way regardless of what the finger did next. That gave no sense of dragging something,
 * and a swipe begun by accident could not be taken back.
 */
class EdgePanelState(initial: Float = 0f) {
    val progress = Animatable(initial)

    val isOpen: Boolean get() = progress.targetValue > 0.5f
    val isIdleClosed: Boolean get() = progress.value == 0f && !progress.isRunning

    suspend fun open() = progress.animateTo(1f, OpenSpec)
    suspend fun close() = progress.animateTo(0f, CloseSpec)

    /** Settles to whichever end the gesture is closest to, letting a flick win over distance. */
    suspend fun settle(velocityFraction: Float) {
        val flung = abs(velocityFraction) > FlingThreshold
        val target = when {
            flung -> if (velocityFraction > 0f) 1f else 0f
            progress.value > 0.5f -> 1f
            else -> 0f
        }
        progress.animateTo(target, if (target == 1f) OpenSpec else CloseSpec, velocityFraction)
    }

    private companion object {
        val OpenSpec = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
        val CloseSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 420f)

        /** Fractions of panel width per second past which a flick decides the outcome. */
        const val FlingThreshold = 1.2f
    }
}

/**
 * Watches the two screen edges and drags [left]/[right] open as the finger moves.
 *
 * Runs on the Initial pass and consumes movement once the drag is clearly horizontal, so an edge
 * drag belongs to the panel rather than being shared with the tab pager underneath it — the two
 * would otherwise both act on the same gesture and the page would slide while the panel opened.
 * A drag starting anywhere but the strips is left entirely alone.
 */
suspend fun PointerInputScope.awaitEdgePanelDrags(
    scope: CoroutineScope,
    stripWidthPx: Float,
    startStrip: ClosedFloatingPointRange<Float>,
    endStrip: ClosedFloatingPointRange<Float>,
    start: EdgePanelState?,
    end: EdgePanelState?,
    rtl: Boolean
) {
    val slop = viewConfiguration.touchSlop
    // Which physical edge each panel lives on, once reading direction is taken into account.
    val leftPanel = if (rtl) end else start
    val rightPanel = if (rtl) start else end
    val leftStrip = if (rtl) endStrip else startStrip
    val rightStrip = if (rtl) startStrip else endStrip
    awaitEachGesture {
        // Initial, not Main: this detector sits above the pager in the tree, and Compose delivers
        // the Main pass child-first. On Main the pager would already have claimed the drag at touch
        // slop, and an edge swipe would page the content instead of opening the panel. Initial goes
        // parent-first, which is the only pass where this can take the gesture.
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val fromLeft = leftPanel != null &&
            down.position.x <= stripWidthPx &&
            down.position.y in leftStrip
        val fromRight = rightPanel != null &&
            down.position.x >= size.width - stripWidthPx &&
            down.position.y in rightStrip
        if (!fromLeft && !fromRight) return@awaitEachGesture

        val panel = if (fromLeft) leftPanel!! else rightPanel!!
        // Dragging the panel open moves it inward: rightwards from the left edge, leftwards from
        // the right one. One sign flip keeps the rest of this symmetric.
        val direction = if (fromLeft) 1f else -1f
        // Exactly the width the sheet is laid out at, so dragging across the visible panel is one
        // full open. Deriving progress from the screen instead made a drag the full width of a
        // 380dp panel count as a fraction of one, and it sprang shut after ample movement.
        val panelWidthPx = panelWidth(size.width.toDp()).toPx().coerceAtLeast(1f)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPointerInputChange(down)

        var change = down
        var totalX = 0f
        var totalY = 0f
        var owned = false

        while (change.pressed) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChange()
            totalX += delta.x
            totalY += delta.y
            velocityTracker.addPointerInputChange(change)

            if (!owned) {
                // Give up on a gesture that turns out to be a vertical scroll, so lists near the
                // edge still scroll normally.
                if (abs(totalY) > slop && abs(totalY) > abs(totalX)) return@awaitEachGesture
                if (abs(totalX) > slop && totalX * direction > 0f) owned = true
            }
            if (owned) {
                change.consume()
                val next = (panel.progress.value + delta.x * direction / panelWidthPx)
                scope.launch { panel.progress.snapTo(next.coerceIn(0f, 1f)) }
            }
        }

        if (!owned) return@awaitEachGesture
        val velocityFraction = velocityTracker.calculateVelocity().x * direction / panelWidthPx
        scope.launch { panel.settle(velocityFraction) }
    }
}

/**
 * Drags the sheet itself back toward its edge to close it, tracking the finger the same way opening
 * does. Only a drag heading *towards* the edge claims the gesture, so the panel cannot be nudged
 * further open, and a vertical drag is released immediately so lists inside still scroll.
 *
 * Because this owns horizontal movement on the sheet, a panel hosting it cannot also swipe between
 * its own tabs — on an edge sheet, dismissing is the gesture people reach for first.
 */
fun Modifier.edgePanelCloseDrag(
    state: EdgePanelState,
    edge: PanelEdge,
    scope: CoroutineScope,
    rtl: Boolean
): Modifier = this.pointerInput(edge, state, rtl) {
    val slop = viewConfiguration.touchSlop
    // Opening moves the sheet away from its edge, so closing is the opposite of hiding it.
    val direction = -hiddenDirection(edge, rtl)
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // The sheet's own width, so a drag across it is exactly one full close.
        val panelWidthPx = size.width.toFloat().coerceAtLeast(1f)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPointerInputChange(down)
        var change = down
        var totalX = 0f
        var totalY = 0f
        var owned = false

        while (change.pressed) {
            val event = awaitPointerEvent()
            change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChange()
            totalX += delta.x
            totalY += delta.y
            velocityTracker.addPointerInputChange(change)

            if (!owned) {
                if (abs(totalY) > slop && abs(totalY) > abs(totalX)) return@awaitEachGesture
                if (abs(totalX) > slop && totalX * direction < 0f) owned = true
            }
            if (owned) {
                change.consume()
                val next = state.progress.value + delta.x * direction / panelWidthPx
                scope.launch { state.progress.snapTo(next.coerceIn(0f, 1f)) }
            }
        }

        if (!owned) return@awaitEachGesture
        scope.launch {
            state.settle(velocityTracker.calculateVelocity().x * direction / panelWidthPx)
        }
    }
}

private const val PanelWidthFraction = 0.86f

/** Maximum sheet width; wide screens get a panel rather than a half-empty page. */
private val PanelMaxWidth = 380.dp

/** The sheet's real width for a given available width. The gesture converts finger travel into
 *  progress with this, and the sheet is laid out at it, so a drag across the visible panel is
 *  exactly one full open. Note `fillMaxWidth(f).widthIn(max = …)` cannot express this: the fraction
 *  fixes the constraint before widthIn can clamp it, so on a wide screen the cap never binds. */
internal fun panelWidth(available: Dp): Dp = minOf(available * PanelWidthFraction, PanelMaxWidth)

/**
 * A sheet anchored to one screen edge whose position is [state]'s progress, plus the scrim behind
 * it. Renders nothing at all while fully closed, so a panel that is not in use costs neither
 * composition nor touch handling.
 */
@Composable
fun EdgePanel(
    state: EdgePanelState,
    edge: PanelEdge,
    scope: CoroutineScope,
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    if (state.isIdleClosed) return
    val progress = state.progress.value
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f * progress))
            .pointerInput(Unit) {
                // Tapping the dimmed area closes the panel, and the same tap must not reach the
                // page underneath it.
                awaitEachGesture {
                    awaitFirstDown().also { it.consume() }
                    scope.launch { state.close() }
                }
            }
    ) {
        Surface(
            modifier = Modifier
                // CenterStart/CenterEnd mirror on their own; the slide has to be told to, which is
                // what hiddenDirection does — otherwise a mirrored sheet aligns to one edge and
                // slides off towards the other.
                .align(if (edge == PanelEdge.Start) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(panelWidth(maxWidth))
                .graphicsLayer {
                    val hidden = size.width * (1f - progress)
                    translationX = hidden * hiddenDirection(edge, rtl)
                }
                .edgePanelCloseDrag(state, edge, scope, rtl)
                .pointerInput(Unit) {
                    // Consume the touch, so a tap landing on an inert part of the sheet stops here
                    // rather than reaching the scrim behind it. Without the consume the scrim saw
                    // every such tap and shut the panel — tapping the panel closed it. Children
                    // have already had this event (Main pass runs child-first), so their own
                    // clicks and scrolls are untouched.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).also { it.consume() }
                    }
                },
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 22.dp
        ) {
            content()
        }
    }
}

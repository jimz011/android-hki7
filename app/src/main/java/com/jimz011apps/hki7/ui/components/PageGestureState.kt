package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True while the top-level pager (or the room pager) is being dragged.
 *
 * A page keeps its own pull-down header state, so swiping away from a page with the header open
 * left it open — and it was still open when you came back, having outlived the reason it was
 * showing. Pages collapse it when this turns true, which is the "close the original one" half of
 * the choice; showing it on every page would mean one page's gesture silently changing another's
 * state, which is worse.
 */
val LocalPageSwipeInProgress = staticCompositionLocalOf { false }

/**
 * Lets a top-level view tell the host that it has opened a page of its own — Energy's Solar tab,
 * Climate's Gas group, a Security category.
 *
 * While one is open the pager stops accepting horizontal drags: the same gesture meant two things
 * depending on how deep you were, and sliding sideways out of a detail page into an unrelated tab
 * is not something anyone asks for. Back (or the header's arrow) still leaves the sub-page, and
 * paging resumes once it does. Reported per route, because the pager composes its neighbours: a
 * page sitting off-screen in a sub-page must not lock the page you are actually looking at.
 */
val LocalSubPageReporter = staticCompositionLocalOf<((route: String, open: Boolean) -> Unit)?> { null }

/**
 * Bumped for a route when its tab is tapped while already open.
 *
 * Views remember where you scrolled to and never forget it on a timer — a position that expires
 * against a clock the user cannot see reads as a bug, and every platform that does this keeps the
 * position indefinitely. Tapping the current tab again is the escape hatch that makes that safe,
 * and is what App Store, Instagram and the Play Store all do: it returns to the top.
 *
 * Per route rather than one counter, because the pager keeps neighbours composed and they must not
 * throw away a position the user never asked to lose.
 */
val LocalScrollToTopSignals = staticCompositionLocalOf<Map<String, Int>> { emptyMap() }

/**
 * Runs [onScrollToTop] when [route]'s tab is re-tapped, and only then.
 *
 * The counter is state, not an event, so reacting to "it is greater than zero" fires again every
 * time the view re-enters composition — which the pager does constantly, since it keeps only the
 * current page and its neighbours. That is why views two tabs away kept losing their scroll
 * position. Remembering which value was already acted on makes a re-entry a no-op, and surviving
 * process death means a restored view does not scroll itself either.
 */
@Composable
fun ScrollToTopOnTabReselect(route: String, onScrollToTop: suspend () -> Unit) {
    val signal = LocalScrollToTopSignals.current[route] ?: 0
    var handled by rememberSaveable(route) { mutableIntStateOf(signal) }
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            onScrollToTop()
        }
    }
}

/**
 * Switches to a top-level tab by route, exactly as tapping it in the navigation bar does.
 *
 * A widget that wants to show its full view should land you *on that tab* — swiping left and right
 * keeps working, there is no back arrow that does not belong to a tab, and back follows the same
 * visited-tab history as any other tab change. Pushing the view as a detail screen instead gave it
 * a back button and took paging away.
 *
 * Null when no pager is hosting, and a no-op for a route the user has hidden from the bar, so
 * callers keep whatever fallback they had.
 */
val LocalOpenTopLevelRoute = staticCompositionLocalOf<((route: String) -> Boolean)?> { null }

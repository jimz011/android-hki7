package com.jimz011apps.hki7.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipe
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.components.ModernAlertDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsDialogFrame
import com.jimz011apps.hki7.ui.components.SettingsTabRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * A settings dialog with tabs is supposed to change tab on a sideways swipe anywhere on it, not only
 * on the tab strip. Swiping the inert space below the content is the case that was reported as doing
 * nothing, and it is the one hardest to be sure about by reading the gesture code, so it is pinned
 * here instead.
 */
class DialogTabSwipeTest {

    @get:Rule
    val compose = createComposeRule()

    private val tabs = listOf("first" to "First", "second" to "Second", "third" to "Third")

    /** A settings dialog shaped like the real ones: tab strip, scrolling body, inert space below. */
    @Composable
    private fun TabDialog(initial: String, onTab: (String) -> Unit) {
        var tab by remember { mutableStateOf(initial) }
        onTab(tab)
        ModernSettingsDialogFrame(
            title = "Test",
            subtitle = "Test",
            onDismiss = {},
            footer = {}
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                Text("Content for $tab")
                Box(Modifier.testTag("emptyArea").fillMaxWidth().height(220.dp))
            }
        }
    }

    @Test
    fun swiping_the_empty_area_below_the_content_changes_tab() {
        var selected = "first"
        compose.setContent {
            var tab by remember { mutableStateOf("first") }
            selected = tab
            ModernSettingsDialogFrame(
                title = "Test",
                subtitle = "Test",
                onDismiss = {},
                footer = {}
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                    Text("Content for $tab")
                    // The inert region the report was about: no control, no scrollable of its own.
                    Box(
                        Modifier
                            .testTag("emptyArea")
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color.Transparent)
                    )
                }
            }
        }

        compose.onNodeWithTag("emptyArea").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertEquals("a swipe on the dialog's empty area should advance the tab", "second", selected)
    }

    /** A real sideways swipe on a phone is never level — the thumb arcs. These pin how much drift is
     *  tolerated, which is the difference between the gesture feeling reliable and feeling ignored. */
    @Test
    fun a_swipe_that_drifts_downwards_still_changes_tab() {
        var selected = "first"
        compose.setContent { TabDialog(initial = "first") { selected = it } }

        compose.onNodeWithTag("emptyArea").performTouchInput {
            // 240px across, 150px down: a thumb arc, not a scroll.
            swipe(start = center, end = center + Offset(-240f, 150f), durationMillis = 200)
        }
        compose.waitForIdle()

        assertEquals("a swipe with a thumb's worth of drift should still page", "second", selected)
    }

    @Test
    fun a_mostly_vertical_drag_does_not_change_tab() {
        var selected = "first"
        compose.setContent { TabDialog(initial = "first") { selected = it } }

        compose.onNodeWithTag("emptyArea").performTouchInput {
            swipe(start = center, end = center + Offset(-90f, 320f), durationMillis = 200)
        }
        compose.waitForIdle()

        assertEquals("a scroll that drifts sideways must not page", "first", selected)
    }

    /**
     * KNOWN FAILING — pins a reproduced, unfixed bug.
     *
     * Widget settings (the to-do list, battery, calendar, …) are built on [ModernAlertDialog], not
     * on [ModernSettingsDialogFrame], and this is the frame that was reported as unswipeable. With a
     * *scrolling* body no gesture reaches the frame at all: instrumenting the detector showed it
     * never receives a touch-down, while the same frame with a plain body (below) receives it and
     * pages correctly. So it is specific to a scrolling body in this frame, and the cause is not yet
     * identified. Left red on purpose rather than deleted — it is the reproduction.
     */
    @Test
    fun alert_dialog_level_swipe_changes_tab() {
        var selected = "first"
        compose.setContent { AlertTabDialog { selected = it } }

        compose.onNodeWithTag("emptyArea").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertEquals("second", selected)
    }

    /** Same frame, but a plain non-scrolling body: separates "the swipe modifier never took" from
     *  "the scrolling body swallowed it". */
    @Test
    fun alert_dialog_plain_body_swipe_changes_tab() {
        var selected = "first"
        compose.setContent {
            var tab by remember { mutableStateOf("first") }
            selected = tab
            ModernAlertDialog(
                onDismissRequest = {},
                confirmButton = { Text("Save") },
                title = { Text("Widget") },
                stableHeight = true,
                text = {
                    Column(Modifier.fillMaxSize()) {
                        SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                        Box(Modifier.testTag("emptyArea").fillMaxWidth().height(260.dp))
                    }
                }
            )
        }

        compose.onNodeWithTag("emptyArea").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertEquals("second", selected)
    }

    @Composable
    private fun AlertTabDialog(onTab: (String) -> Unit) {
        var tab by remember { mutableStateOf("first") }
        onTab(tab)
        ModernAlertDialog(
            onDismissRequest = {},
            confirmButton = { Text("Save") },
            title = { Text("Widget") },
            stableHeight = true,
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                    Text("Content for $tab")
                    Box(Modifier.testTag("emptyArea").fillMaxWidth().height(260.dp))
                }
            }
        )
    }

    @Test
    fun alert_dialog_swipe_with_drift_changes_tab() {
        var selected = "first"
        compose.setContent {
            var tab by remember { mutableStateOf("first") }
            selected = tab
            ModernAlertDialog(
                onDismissRequest = {},
                confirmButton = { Text("Save") },
                title = { Text("Widget") },
                stableHeight = true,
                text = {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                        Text("Content for $tab")
                        Box(Modifier.testTag("emptyArea").fillMaxWidth().height(260.dp))
                    }
                }
            )
        }

        compose.onNodeWithTag("emptyArea").performTouchInput {
            swipe(start = center, end = center + Offset(-240f, 150f), durationMillis = 200)
        }
        compose.waitForIdle()

        assertEquals("widget settings should page on a drifting swipe too", "second", selected)
    }

    @Test
    fun swiping_back_returns_to_the_previous_tab() {
        var selected = "first"
        compose.setContent {
            var tab by remember { mutableStateOf("second") }
            selected = tab
            ModernSettingsDialogFrame(
                title = "Test",
                subtitle = "Test",
                onDismiss = {},
                footer = {}
            ) {
                Column(Modifier.fillMaxSize()) {
                    SettingsTabRow(tabs = tabs, selected = tab, onSelect = { tab = it })
                    Box(Modifier.testTag("emptyArea").fillMaxWidth().height(220.dp))
                }
            }
        }

        compose.onNodeWithTag("emptyArea").performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertEquals("first", selected)
    }
}

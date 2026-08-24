package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveHeaderTitleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dutchGreetingShrinksToOneCompleteLineBesidePeople() {
        var layout: TextLayoutResult? = null

        composeRule.setContent {
            Box(Modifier.width(220.dp)) {
                AdaptiveHeaderTitle(
                    text = "Goedemiddag",
                    color = Color.White,
                    onTextLayout = { layout = it }
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(1, layout?.lineCount)
        assertFalse(layout?.hasVisualOverflow ?: true)
    }
}

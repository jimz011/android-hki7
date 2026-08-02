package com.jimz011apps.hki7.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticColorContrastTest {
    @Test
    fun `similar red theme and state colors produce a readable red shade or tint`() {
        val themeRed = Color(0xFFE53935)
        val stateRed = Color(0xFFEF5350)

        val resolved = semanticColorForBackground(stateRed, themeRed)

        assertTrue(colorContrastRatio(resolved, themeRed) >= 3f)
        assertTrue(resolved.red > resolved.green)
        assertTrue(resolved.red > resolved.blue)
    }

    @Test
    fun `already readable semantic color remains unchanged`() {
        val blue = Color(0xFF1565C0)
        val background = Color.White

        assertEquals(blue, semanticColorForBackground(blue, background))
    }

    @Test
    fun `adjusted blue remains within the blue color family`() {
        val background = Color(0xFF42A5F5)
        val blue = Color(0xFF64B5F6)

        val resolved = semanticColorForBackground(blue, background)

        assertTrue(colorContrastRatio(resolved, background) >= 3f)
        assertTrue(resolved.blue > resolved.red)
        assertTrue(resolved.blue > resolved.green)
    }
}

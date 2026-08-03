package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconSizeTest {

    @Test
    fun `auto shrinks the glyph as a stack gains columns`() {
        val sizes = (1..6).map { iconOnlyIconSizeDp(columns = it) }

        // The bug this guards: a fixed 40dp glyph overflowing a narrow six-column popup button.
        assertEquals(listOf(40, 40, 34, 28, 24, 20), sizes)
        assertTrue("must never grow as columns increase", sizes.zipWithNext().all { (a, b) -> b <= a })
    }

    @Test
    fun `a column count beyond the offered range keeps the smallest size`() {
        assertEquals(20, iconOnlyIconSizeDp(columns = 12))
    }

    @Test
    fun `a column count below one is treated as one`() {
        assertEquals(40, iconOnlyIconSizeDp(columns = 0))
        assertEquals(40, iconOnlyIconSizeDp(columns = -3))
    }

    @Test
    fun `the stack default overrides the column-derived size`() {
        assertEquals(48, iconOnlyIconSizeDp(stackIconSize = 48, columns = 6))
    }

    @Test
    fun `a button's own size beats the stack default`() {
        assertEquals(24, iconOnlyIconSizeDp(buttonIconSize = 24, stackIconSize = 48, columns = 2))
    }

    @Test
    fun `auto on both levels falls through to the column size`() {
        assertEquals(
            28,
            iconOnlyIconSizeDp(
                buttonIconSize = ICON_SIZE_AUTO,
                stackIconSize = ICON_SIZE_AUTO,
                columns = 4
            )
        )
    }

    @Test
    fun `buttons and stacks default to auto so saved layouts are unchanged`() {
        assertEquals(ICON_SIZE_AUTO, HKIButtonConfig().iconSize)
        assertEquals(ICON_SIZE_AUTO, HKIButtonStack(id = "s").childIconSize)
        assertEquals(false, HKIButtonStack(id = "s").childIconOnly)
    }
}

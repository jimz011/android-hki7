package com.jimz011apps.hki7.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveDashboardLayoutTest {
    @Test
    fun `dashboard columns follow phone foldable and tablet breakpoints`() {
        assertEquals(1, responsiveDashboardColumnCount(599.dp))
        assertEquals(2, responsiveDashboardColumnCount(600.dp))
        assertEquals(2, responsiveDashboardColumnCount(899.dp))
        assertEquals(3, responsiveDashboardColumnCount(900.dp))
    }

    @Test
    fun `compact tiles add columns without shrinking below their target width`() {
        assertEquals(1, responsiveDashboardTileCount(300.dp))
        assertEquals(2, responsiveDashboardTileCount(310.dp))
        assertEquals(3, responsiveDashboardTileCount(470.dp))
        assertEquals(5, responsiveDashboardTileCount(790.dp))
        assertEquals(6, responsiveDashboardTileCount(1400.dp))
    }
}

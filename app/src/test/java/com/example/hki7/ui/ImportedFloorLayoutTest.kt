package com.example.hki7.ui

import com.example.hki7.data.HAFloor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedFloorLayoutTest {
    @Test
    fun `new imported floor uses room layout defaults`() {
        val imported = HAFloor(
            floor_id = "ground",
            name = "Ground floor",
            columns = 4,
            isSquare = true,
            compactTiles = false,
            width = "half"
        ).withDefaultRoomLayout()

        assertEquals(1, imported.columns)
        assertEquals("full", imported.width)
        assertFalse(imported.isSquare)
        assertTrue(imported.compactTiles)
    }
}

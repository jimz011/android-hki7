package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HAFloorDefaultsTest {
    @Test
    fun `new floor stacks use one full-width compact standard column`() {
        val floor = HAFloor(floor_id = "ground", name = "Ground floor")

        assertEquals(1, floor.columns)
        assertEquals("full", floor.width)
        assertFalse(floor.isSquare)
        assertTrue(floor.compactTiles)
    }
}

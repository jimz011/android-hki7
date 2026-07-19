package com.example.hki7.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomStackOrderTest {
    @Test
    fun `lights are ordered before switches regardless of entity response order`() {
        assertEquals(
            listOf("light", "switch"),
            orderedAutoRoomStackDomains(listOf("switch", "light"))
        )
    }

    @Test
    fun `missing stack domains are omitted`() {
        assertEquals(
            listOf("switch"),
            orderedAutoRoomStackDomains(listOf("sensor", "switch"))
        )
    }
}

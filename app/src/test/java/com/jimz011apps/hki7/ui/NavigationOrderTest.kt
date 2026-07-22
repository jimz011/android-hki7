package com.jimz011apps.hki7.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationOrderTest {
    @Test
    fun `default navigation order follows primary home overview flow`() {
        assertEquals(
            listOf("home", "rooms", "climate", "security", "energy", "battery"),
            NavBarConfig.visibleTabs(emptyList(), emptyList()).map(Screen::route)
        )
    }

    @Test
    fun `saved configurable order still wins over the default`() {
        assertEquals(
            listOf("home", "rooms", "battery", "energy", "climate", "security"),
            NavBarConfig.visibleTabs(
                listOf("battery", "energy", "climate", "security"),
                emptyList()
            ).map(Screen::route)
        )
    }
}

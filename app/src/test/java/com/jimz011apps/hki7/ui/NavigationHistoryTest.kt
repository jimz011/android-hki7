package com.jimz011apps.hki7.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val HOME = 0
private const val ROOMS = 1
private const val ENERGY = 2
private const val CLIMATE = 3

private fun tab(index: Int) = VisitedPlace.Tab(index)
private fun room(id: String) = VisitedPlace.Room(id)

class NavigationHistoryTest {

    /**
     * The sequence this was reported against: home, rooms, office, bedroom, energy, climate.
     * Back has to retrace it exactly, one step per press, and then stop at home.
     */
    @Test
    fun `back retraces every page in order`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ROOMS))
        history.visit(room("office"))
        history.visit(room("bedroom"))
        history.visit(tab(ENERGY))
        history.visit(tab(CLIMATE))

        val walked = generateSequence { history.back() }.toList()

        assertEquals(
            listOf(tab(ENERGY), room("bedroom"), room("office"), tab(ROOMS), tab(HOME)),
            walked
        )
    }

    /** Arriving somewhere is recorded once, however many times the screen recomposes. */
    @Test
    fun `repeating the current place is not a navigation`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(HOME))
        history.visit(room("office"))
        history.visit(room("office"))

        assertEquals(listOf(tab(HOME), room("office")), history.entries)
    }

    /** Revisiting is a new step, not a reshuffle: this is the case deduplication used to collapse. */
    @Test
    fun `revisiting a place pushes it again`() {
        val history = NavigationHistory()
        history.visit(tab(ROOMS))
        history.visit(room("office"))
        history.visit(tab(ROOMS))
        history.visit(room("office"))

        assertEquals(4, history.entries.size)
        assertEquals(tab(ROOMS), history.back())
        assertEquals(room("office"), history.back())
        assertEquals(tab(ROOMS), history.back())
        assertNull(history.back())
    }

    @Test
    fun `back returns null once the history is spent`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        assertNull(history.back())
        assertNull(history.back())
    }

    @Test
    fun `rooms tab reopens the room it was left in`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ROOMS))
        history.visit(room("office"))
        history.visit(tab(ENERGY))

        assertEquals("office", history.roomToReopen(ROOMS))
    }

    /** Backing out to the list means the tab was left on the list, so it must not reopen a room. */
    @Test
    fun `rooms tab shows the list when that is where it was left`() {
        val history = NavigationHistory()
        history.visit(tab(ROOMS))
        history.visit(room("office"))
        history.visit(tab(ROOMS))
        history.visit(tab(ENERGY))

        assertNull(history.roomToReopen(ROOMS))
    }

    @Test
    fun `rooms tab shows the list when no room has been opened`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ROOMS))

        assertNull(history.roomToReopen(ROOMS))
    }

    /** The most recent room wins when several were visited without returning to the list. */
    @Test
    fun `rooms tab reopens the last room of several`() {
        val history = NavigationHistory()
        history.visit(tab(ROOMS))
        history.visit(room("office"))
        history.visit(room("bedroom"))
        history.visit(tab(ENERGY))

        assertEquals("bedroom", history.roomToReopen(ROOMS))
    }

    @Test
    fun `history is capped and drops the oldest`() {
        val history = NavigationHistory(maxEntries = 3)
        history.visit(tab(HOME))
        history.visit(tab(ROOMS))
        history.visit(tab(ENERGY))
        history.visit(tab(CLIMATE))

        assertEquals(listOf(tab(ROOMS), tab(ENERGY), tab(CLIMATE)), history.entries)
    }
}

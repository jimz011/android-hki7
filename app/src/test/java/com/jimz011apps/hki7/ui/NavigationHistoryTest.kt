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

    /**
     * The contract the screen has to keep: after back, the only thing it may record is the place
     * back sent it to.
     *
     * Arriving there is a no-op, so the history stays put and the next back moves on. Recording
     * anything else — the page still showing while the navigation propagates, which is what the
     * screen used to do — pushes a step back onto the stack that back has just taken off, and the
     * two then trade places forever.
     */
    @Test
    fun `arriving where back sent us leaves the history alone`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ROOMS))
        history.visit(room("office"))

        val target = history.back()
        assertEquals(tab(ROOMS), target)
        history.visit(target!!)

        assertEquals(listOf(tab(HOME), tab(ROOMS)), history.entries)
        assertEquals(tab(HOME), history.back())
    }

    /** The same, walked all the way down: every back moves on, none of them repeat. */
    @Test
    fun `walking back with an arrival recorded at each step never repeats`() {
        val history = NavigationHistory()
        listOf(tab(HOME), tab(ROOMS), room("office"), room("bedroom"), tab(ENERGY), tab(CLIMATE))
            .forEach(history::visit)

        val walked = buildList {
            while (true) {
                val target = history.back() ?: break
                history.visit(target)
                add(target)
            }
        }

        assertEquals(
            listOf(tab(ENERGY), room("bedroom"), room("office"), tab(ROOMS), tab(HOME)),
            walked
        )
    }

    /**
     * The real sequence of events on a device, including the part that broke it four times: the
     * screen keeps reporting the page it is leaving for a frame or more after back has moved on.
     *
     * Each step here reports the page being left *and then* the page arrived at, in that order,
     * which is what actually happens. Back still has to walk the path exactly.
     */
    @Test
    fun `back walks the path even when the screen reports the old page late`() {
        val history = NavigationHistory()
        val path = listOf(tab(HOME), tab(ROOMS), room("office"), room("bedroom"), tab(ENERGY), tab(CLIMATE))
        path.forEach(history::visit)

        val walked = buildList {
            var leaving = path.last()
            while (true) {
                val target = history.back() ?: break
                history.visit(leaving)   // the straggling report of the page just left
                history.visit(target)    // and then the page actually arrived at
                add(target)
                leaving = target
            }
        }

        assertEquals(
            listOf(tab(ENERGY), room("bedroom"), room("office"), tab(ROOMS), tab(HOME)),
            walked
        )
    }

    /** A late report is swallowed once. Genuinely returning there afterwards still counts. */
    @Test
    fun `only one late report is ignored`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ENERGY))

        assertEquals(tab(HOME), history.back())
        history.visit(tab(ENERGY))                 // late report, ignored
        assertEquals(listOf(tab(HOME)), history.entries)

        history.visit(tab(ENERGY))                 // the user really went back to Energy
        assertEquals(listOf(tab(HOME), tab(ENERGY)), history.entries)
    }

    /** Tapping the tab you just backed out of is a real navigation, not an echo. */
    @Test
    fun `a deliberate visit is never mistaken for a late report`() {
        val history = NavigationHistory()
        history.visit(tab(HOME))
        history.visit(tab(ENERGY))

        assertEquals(tab(HOME), history.back())
        history.forgetLastLeft()
        history.visit(tab(ENERGY))

        assertEquals(listOf(tab(HOME), tab(ENERGY)), history.entries)
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

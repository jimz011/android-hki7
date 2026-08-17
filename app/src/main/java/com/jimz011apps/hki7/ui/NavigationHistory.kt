package com.jimz011apps.hki7.ui

/** Somewhere the user has been. Tabs are pager pages, so neither is a platform back-stack entry. */
sealed interface VisitedPlace {
    data class Tab(val index: Int) : VisitedPlace
    /** Held by area rather than by pager page, so it survives the room list changing. */
    data class Room(val areaId: String) : VisitedPlace
}

/**
 * Where the user has been, and where back should take them.
 *
 * Pulled out of the screen and made a plain class so it can be tested. Back navigation here spans
 * a tab pager, a room pager and the nav graph, none of which agree on what a "page" is, and three
 * attempts at getting it right by reading the composable were three wrong answers.
 */
class NavigationHistory(private val maxEntries: Int = 60) {
    private val places = mutableListOf<VisitedPlace>()

    /**
     * The place [back] has just left, still to be reported by the screen.
     *
     * Navigation is not instant: the screen goes on describing the page it is leaving for a frame
     * or more after back has moved on. That late report used to be recorded as a fresh visit,
     * putting the page straight back on the stack that back had just taken it off, so back walked
     * into it, out of it, and into it again forever.
     *
     * Naming the page back left, and ignoring exactly one report of it, settles that without any
     * reference to timing — no waiting, no suppressing, nothing to get a frame wrong.
     */
    private var justLeft: VisitedPlace? = null

    val entries: List<VisitedPlace> get() = places.toList()

    /**
     * Records arrival somewhere.
     *
     * A repeat of where we already are is ignored: that is a recomposition, not a navigation.
     * Anything else is pushed, including somewhere visited before — this is a stack, not a
     * most-recently-used list, so Rooms → a room → Rooms → a room really is four steps back.
     */
    fun visit(place: VisitedPlace) {
        if (places.lastOrNull() == place) return
        if (place == justLeft) {
            // The straggling report of the page back left. Cleared as it is swallowed, so a
            // genuine return to that page a moment later still counts.
            justLeft = null
            return
        }
        justLeft = null
        places += place
        if (places.size > maxEntries) places.removeAt(0)
    }

    /**
     * Drops any pending late report, so the next visit counts whatever it is.
     *
     * Called when the user asks for somewhere outright — tapping the tab they just backed out of
     * is a navigation, not the echo of one.
     */
    fun forgetLastLeft() {
        justLeft = null
    }

    /** What back would go to, without going there. Null when this is the last place recorded. */
    fun previous(): VisitedPlace? = places.getOrNull(places.lastIndex - 1)

    /**
     * Leaves the current place and returns the one behind it, or null when there is none —
     * the caller's cue to fall back to Home.
     */
    fun back(): VisitedPlace? {
        justLeft = places.removeLastOrNull()
        return places.lastOrNull()
    }

    /**
     * The room the Rooms tab should reopen, or null when it should show the list.
     *
     * Scans back for whichever came last: a room, or the Rooms tab itself. Taking simply "the most
     * recent room anywhere in the history" reopened a room even after the user had backed out to
     * the list, because the list being visited afterwards did not erase the room behind it.
     */
    fun roomToReopen(roomsTabIndex: Int): String? {
        for (place in places.asReversed()) {
            when {
                place is VisitedPlace.Room -> return place.areaId
                place is VisitedPlace.Tab && place.index == roomsTabIndex -> return null
            }
        }
        return null
    }
}

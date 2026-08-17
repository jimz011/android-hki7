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

    val entries: List<VisitedPlace> get() = places.toList()

    /**
     * Records arrival somewhere.
     *
     * A repeat of where we already are is ignored: that is a recomposition, not a navigation, and
     * it is also what makes [back] safe to call without the caller having to suppress the visit it
     * causes. Anything else is pushed, including somewhere visited before — this is a stack, not a
     * most-recently-used list, so Rooms → a room → Rooms → a room really is four steps back.
     */
    fun visit(place: VisitedPlace) {
        if (places.lastOrNull() == place) return
        places += place
        if (places.size > maxEntries) places.removeAt(0)
    }

    /** What back would go to, without going there. Null when this is the last place recorded. */
    fun previous(): VisitedPlace? = places.getOrNull(places.lastIndex - 1)

    /**
     * Leaves the current place and returns the one behind it, or null when there is none —
     * the caller's cue to fall back to Home.
     */
    fun back(): VisitedPlace? {
        if (places.isNotEmpty()) places.removeAt(places.lastIndex)
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

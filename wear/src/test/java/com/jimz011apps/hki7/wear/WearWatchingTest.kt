package com.jimz011apps.hki7.wear

import com.jimz011apps.hki7.data.HKIQuickAction
import org.junit.Assert.assertEquals
import org.junit.Test

class WearWatchingTest {
    @Test
    fun `returning home watches every configured quick action exactly once`() {
        val actions = listOf(
            HKIQuickAction(id = "1", entityId = "light.kitchen"),
            HKIQuickAction(id = "2", entityId = "light.kitchen"),
            HKIQuickAction(id = "3", entityId = "lock.front_door"),
            HKIQuickAction(id = "4", entityId = ""),
        )

        assertEquals(
            setOf("light.kitchen", "lock.front_door"),
            watchedQuickActionIds(actions),
        )
    }
}

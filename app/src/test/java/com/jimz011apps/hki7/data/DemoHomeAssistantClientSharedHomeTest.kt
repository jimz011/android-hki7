package com.jimz011apps.hki7.data

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The Android Auto screen watches the demo home through one client while every tap runs through a
 * short-lived client of its own. Both must see the same home, or the tap changes a copy nobody is
 * watching and the car grid never moves — which is what a Play reviewer sees as a broken demo.
 */
class DemoHomeAssistantClientSharedHomeTest {

    @Test
    fun `a change made through one demo client reaches another client's subscription`() = runBlocking {
        val watcher = DemoHomeAssistantClient()
        val actor = DemoHomeAssistantClient()
        try {
            // The home is shared for the whole process, so assert a change, not a fixed start state.
            val before = watcher.getEntityState(ENTITY)!!.state
            val change = async(start = CoroutineStart.UNDISPATCHED) {
                watcher.subscribeStateChanges().first { it.entityId == ENTITY }
            }

            actor.toggleEntity(ENTITY)

            val received = withTimeout(5_000) { change.await() }
            assertNotEquals(before, received.newState?.state)
            assertEquals(received.newState?.state, watcher.getEntityState(ENTITY)?.state)
        } finally {
            watcher.dispose()
            actor.dispose()
        }
    }

    private companion object {
        const val ENTITY = "light.kitchen_spots"
    }
}

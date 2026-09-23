package com.jimz011apps.hki7.wear.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WearDemoTest {

    @Test
    fun `demo includes every advertised surface`() = runBlocking {
        assertNotNull(WearDemo.quickActions.firstOrNull())
        assertNotNull(WearDemo.rooms.firstOrNull())
        assertNotNull(WearDemoClient.state(WearDemo.thermostatEntityId))
    }

    @Test
    fun `quick actions change local state`() = runBlocking {
        val entityId = WearDemo.quickActions.first().entityId
        val before = WearDemoClient.state(entityId)?.state

        WearDemoClient.toggle(entityId)
        val after = WearDemoClient.state(entityId)?.state

        assertNotEquals(before, after)
        WearDemoClient.toggle(entityId)
        assertEquals(before, WearDemoClient.state(entityId)?.state)
    }

    @Test
    fun `thermostat tile changes local target`() = runBlocking {
        val entityId = WearDemo.thermostatEntityId
        val before = WearDemoClient.state(entityId)?.temperature
        val changed = requireNotNull(before) + 0.5

        WearDemoClient.callService(
            "climate",
            "set_temperature",
            buildJsonObject {
                put("entity_id", entityId)
                put("temperature", changed)
            },
        )

        assertEquals(changed, WearDemoClient.state(entityId)?.temperature)
        WearDemoClient.callService(
            "climate",
            "set_temperature",
            buildJsonObject {
                put("entity_id", entityId)
                put("temperature", before)
            },
        )
    }
}

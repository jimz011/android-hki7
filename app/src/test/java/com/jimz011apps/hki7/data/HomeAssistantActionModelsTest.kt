package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAssistantActionModelsTest {
    @Test
    fun `action metadata distinguishes targets and editable text fields`() {
        val message = HAActionFieldDefinition(
            key = "message",
            name = "Message",
            required = true,
            selector = buildJsonObject {
                put("text", buildJsonObject { put("multiline", true) })
            }
        )
        val notification = HAActionDefinition(
            key = "notify.notify",
            name = "Send notification",
            fields = listOf(message)
        )
        val light = HAActionDefinition(
            key = "light.turn_on",
            name = "Turn on",
            target = JsonObject(emptyMap())
        )

        assertFalse(notification.supportsTarget)
        assertTrue(message.acceptsText)
        assertTrue(message.multiline)
        assertTrue(light.supportsTarget)
    }

    @Test
    fun `old persisted actions keep owner target compatibility`() {
        val decoded = Json.decodeFromString<HKIAction>(
            """{"type":"call_service","service":"light.turn_on"}"""
        )

        assertEquals("owner", decoded.targetMode)
    }

    @Test
    fun `service payload omits or includes entity target according to target mode`() {
        val withoutTarget = buildHKIActionServicePayload(
            HKIAction(
                type = "call_service",
                service = "notify.notify",
                targetMode = "none",
                data = buildJsonObject { put("message", "Hello") }
            ),
            ownerEntityId = "light.owner"
        )
        val withOwner = buildHKIActionServicePayload(
            HKIAction(type = "call_service", service = "light.turn_on"),
            ownerEntityId = "light.owner"
        )

        assertFalse(withoutTarget.containsKey("entity_id"))
        assertEquals("Hello", withoutTarget["message"]?.jsonPrimitive?.content)
        assertEquals("light.owner", withOwner["entity_id"]?.jsonPrimitive?.content)
    }
}

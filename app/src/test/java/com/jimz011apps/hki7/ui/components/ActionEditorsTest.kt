package com.jimz011apps.hki7.ui.components

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionEditorsTest {
    @Test
    fun `text action data preserves other fields and removes blank values`() {
        val initial = kotlinx.serialization.json.JsonObject(
            mapOf("title" to JsonPrimitive("Alert"))
        )

        val withMessage = withActionDataText(initial, "message", "Door opened")
        assertEquals("Alert", withMessage?.get("title")?.jsonPrimitive?.content)
        assertEquals("Door opened", withMessage?.get("message")?.jsonPrimitive?.content)

        val withoutMessage = withActionDataText(withMessage, "message", "")
        assertEquals("Alert", withoutMessage?.get("title")?.jsonPrimitive?.content)
        assertNull(withActionDataText(withoutMessage, "title", ""))
    }
}

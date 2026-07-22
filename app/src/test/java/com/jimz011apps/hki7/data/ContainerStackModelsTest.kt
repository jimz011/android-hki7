package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContainerStackModelsTest {
    @Test
    fun `legacy swiping stacks remain unnamed and iconless`() {
        val stack = Json.decodeFromString<HKISwipingStack>(
            """{"id":"legacy-swipe","widgets":[]}"""
        )

        assertNull(stack.title)
        assertNull(stack.icon)
    }

    @Test
    fun `swiping stack stores its name and icon`() {
        val stack = HKISwipingStack(
            id = "named-swipe",
            title = "Climate overview",
            icon = "home-thermometer"
        )
        val restored = Json.decodeFromString<HKISwipingStack>(
            Json.encodeToString(HKISwipingStack.serializer(), stack)
        )

        assertEquals("Climate overview", restored.title)
        assertEquals("home-thermometer", restored.icon)
    }
}

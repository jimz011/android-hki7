package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HKIButtonStack
import com.jimz011apps.hki7.data.HKIEmptyStack
import com.jimz011apps.hki7.data.HKIRoomWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomStackOrderTest {
    @Test
    fun `lights are ordered before switches regardless of entity response order`() {
        assertEquals(
            listOf("light", "switch"),
            orderedAutoRoomStackDomains(listOf("switch", "light"))
        )
    }

    @Test
    fun `missing stack domains are omitted`() {
        assertEquals(
            listOf("switch"),
            orderedAutoRoomStackDomains(listOf("sensor", "switch"))
        )
    }

    @Test
    fun `adaptive lighting auto fill uses a named standard one column empty stack`() {
        var id = 0
        val generated = requireNotNull(buildAutoAdaptiveLightingRoomStack(
            profileIds = listOf("living", "evening"),
            existingWidgets = emptyList(),
            newId = { "generated-${++id}" }
        ))

        assertEquals("Adaptive Lighting", generated.title)
        assertEquals("auto-awesome", generated.icon)
        assertEquals(1, generated.columns)
        assertFalse(generated.isSquare)
        assertTrue(generated.collapsible)
        assertFalse(generated.defaultCollapsed)
        assertEquals(1, generated.widgets.size)

        val adaptiveWidget = generated.widgets.single() as HKIButtonStack
        assertEquals("adaptive_lighting", adaptiveWidget.stackType)
        assertFalse(adaptiveWidget.showName)
        assertTrue(adaptiveWidget.adaptiveLightingRoomScoped)
        assertEquals("double_row", adaptiveWidget.adaptiveLightingLayout)
        assertFalse(adaptiveWidget.adaptiveLightingCenterActions)
        assertEquals(listOf("living", "evening"), adaptiveWidget.adaptiveLightingProfileIds)
    }

    @Test
    fun `adaptive lighting auto fill migrates the old standalone widget into the container`() {
        val legacy = HKIButtonStack(
            id = "legacy-adaptive",
            stackType = "adaptive_lighting",
            icon = "custom-icon",
            showName = true
        )

        val generated = requireNotNull(buildAutoAdaptiveLightingRoomStack(
            profileIds = listOf("office"),
            existingWidgets = listOf<HKIRoomWidget>(legacy),
            newId = { "container" }
        ))
        val child = generated.widgets.single() as HKIButtonStack

        assertEquals("container", generated.id)
        assertEquals("legacy-adaptive", child.id)
        assertEquals("custom-icon", child.icon)
        assertFalse(child.showName)
        assertEquals("full", child.adaptiveLightingLayout)
        assertFalse(child.adaptiveLightingCenterActions)
    }

    @Test
    fun `adaptive lighting auto fill is omitted without room profiles`() {
        assertNull(buildAutoAdaptiveLightingRoomStack(emptyList(), listOf(HKIEmptyStack("existing"))))
    }

    @Test
    fun `adaptive lighting auto fill preserves an existing container icon`() {
        val existing = HKIEmptyStack(
            id = "adaptive-container",
            title = "Adaptive Lighting",
            icon = "theme-light-dark",
            widgets = listOf(HKIButtonStack(id = "adaptive-child", stackType = "adaptive_lighting"))
        )

        val generated = requireNotNull(
            buildAutoAdaptiveLightingRoomStack(listOf("living"), listOf(existing))
        )

        assertEquals("theme-light-dark", generated.icon)
    }
}

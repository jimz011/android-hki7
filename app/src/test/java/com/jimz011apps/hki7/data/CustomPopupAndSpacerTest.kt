package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPopupAndSpacerTest {

    @Test
    fun `spacer ids are recognised and unique per button`() {
        val first = newSpacerEntityId()
        val second = newSpacerEntityId()

        assertTrue(isSpacerEntityId(first))
        assertTrue(isSpacerEntityId(second))
        assertNotEquals(first, second)
        assertFalse(isSpacerEntityId("light.kitchen"))
        // A real entity must never be mistaken for a spacer just by sharing the prefix as a name.
        assertFalse(isSpacerEntityId("sensor.hki7_spacer_count"))
    }

    @Test
    fun `a popup keeps its widgets under its own widget area`() {
        val popup = HKICustomPopup(id = "abc", name = "Kitchen")

        assertEquals("__popup_abc__", customPopupWidgetAreaId(popup.id))
        assertNotEquals(customPopupWidgetAreaId("abc"), customPopupWidgetAreaId("abd"))
    }

    @Test
    fun `popup actions survive a serialization round trip`() {
        val json = Json { ignoreUnknownKeys = true }
        val action = HKIAction(type = "custom_popup", popupId = "abc")

        val restored = json.decodeFromString<HKIAction>(json.encodeToString(action))

        assertEquals("custom_popup", restored.type)
        assertEquals("abc", restored.popupId)
    }

    @Test
    fun `dashboards saved before popups existed still decode`() {
        val json = Json { ignoreUnknownKeys = true }
        val legacy = """{"id":"d1","name":"Home"}"""

        val dashboard = json.decodeFromString<HKIDashboard>(legacy)

        assertEquals(emptyList<HKICustomPopup>(), dashboard.customPopups)
    }

    @Test
    fun `shared dashboards take the owner's popups while keeping local renames`() {
        val local = HKIDashboard(
            id = "shared-family",
            name = "Local",
            customPopups = listOf(
                HKICustomPopup(id = "p1", name = "My name for it", icon = "star")
            )
        )
        val incoming = HKIDashboard(
            id = "family",
            name = "Owner",
            customPopups = listOf(
                HKICustomPopup(id = "p1", name = "Owner name"),
                HKICustomPopup(id = "p2", name = "Added by owner")
            )
        )

        val merged = mergeSharedDashboardAesthetics(local, incoming)

        assertEquals(2, merged.customPopups.size)
        assertEquals("My name for it", merged.customPopups.first { it.id == "p1" }.name)
        assertEquals("star", merged.customPopups.first { it.id == "p1" }.icon)
        // A popup the owner added is picked up as-is.
        assertEquals("Added by owner", merged.customPopups.first { it.id == "p2" }.name)
    }
}

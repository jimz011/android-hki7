package com.jimz011apps.hki7.data

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A shared family dashboard published by a newer app version can contain a widget `type` this
 *  build predates. Decoding must not fail the whole dashboard — see the `appJson` default
 *  deserializer this test exercises, and [HKIUnknownWidget]. */
class HKIRoomWidgetFallbackTest {
    private val dashboardJson = """
        {
          "id": "shared-1",
          "name": "Family",
          "areaWidgets": {
            "living_room": [
              { "type": "button_stack", "id": "known-1", "entityIds": ["light.kitchen"] },
              { "type": "widget_from_a_future_beta", "id": "future-1", "width": "half" }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `unrecognized widget type falls back instead of failing the whole dashboard`() {
        val dashboard = appJson.decodeFromString<HKIDashboard>(dashboardJson)

        val widgets = dashboard.areaWidgets.getValue("living_room")
        assertEquals(2, widgets.size)
        assertTrue(widgets[0] is HKIButtonStack)
        assertEquals("known-1", widgets[0].id)
        assertTrue(widgets[1] is HKIUnknownWidget)
        assertEquals("future-1", widgets[1].id)
        assertEquals("half", widgets[1].width)
    }

    @Test
    fun `fallback widget round-trips without crashing`() {
        val dashboard = appJson.decodeFromString<HKIDashboard>(dashboardJson)

        val reEncoded = appJson.encodeToString(dashboard)
        val reDecoded = appJson.decodeFromString<HKIDashboard>(reEncoded)

        assertEquals(2, reDecoded.areaWidgets.getValue("living_room").size)
    }
}

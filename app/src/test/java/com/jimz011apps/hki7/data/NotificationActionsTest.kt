package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionsTest {

    private fun data(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun parsesActionsWithTitleUriAndActionData() {
        val actions = parseNotificationActions(
            data(
                """
                {"actions":[
                  {"action":"TURN_ON","title":"Lights on","action_data":{"entity_id":"light.hall"}},
                  {"action":"URI","title":"Open cam","uri":"https://example.com/cam"},
                  {"action":"REPLY","title":"Reply"}
                ]}
                """
            )
        )

        assertEquals(3, actions.size)
        assertEquals("TURN_ON", actions[0].action)
        assertEquals("Lights on", actions[0].title)
        assertEquals("light.hall", actions[0].actionData?.get("entity_id")?.jsonPrimitive?.content)
        assertFalse(actions[0].isUri)
        assertFalse(actions[0].isReply)

        assertTrue(actions[1].isUri)
        assertTrue(actions[2].isReply)
    }

    @Test
    fun titleFallsBackToActionKeyAndUnusableEntriesAreDropped() {
        val actions = parseNotificationActions(
            data("""{"actions":[{"action":"ALARM_OFF"},{"title":"No action key"},"not an object"]}""")
        )

        assertEquals(1, actions.size)
        assertEquals("ALARM_OFF", actions[0].action)
        assertEquals("ALARM_OFF", actions[0].title)
    }

    @Test
    fun actionListIsCappedToWhatTheShadeCanRender() {
        val actions = parseNotificationActions(
            data("""{"actions":[{"action":"A"},{"action":"B"},{"action":"C"},{"action":"D"}]}""")
        )

        assertEquals(NotificationActions.MAX_ACTIONS, actions.size)
        assertEquals(listOf("A", "B", "C"), actions.map { it.action })
    }

    @Test
    fun missingOrMalformedActionsYieldNothing() {
        assertTrue(parseNotificationActions(null).isEmpty())
        assertTrue(parseNotificationActions(data("""{"tag":"door"}""")).isEmpty())
        assertTrue(parseNotificationActions(data("""{"actions":"nope"}""")).isEmpty())
    }

    @Test
    fun crossPlatformTextInputBehaviorCountsAsReply() {
        val actions = parseNotificationActions(
            data("""{"actions":[{"action":"ANSWER","title":"Answer","behavior":"textInput"}]}""")
        )

        assertTrue(actions.single().isReply)
    }

    @Test
    fun uriActionNeedsBothTheReservedNameAndATarget() {
        val actions = parseNotificationActions(
            data("""{"actions":[{"action":"URI","title":"Broken"},{"action":"OPEN","uri":"https://x"}]}""")
        )

        assertFalse("URI without a uri cannot be opened", actions[0].isUri)
        assertFalse("a uri on a normal action still fires an event", actions[1].isUri)
    }

    @Test
    fun historyWrittenBeforeActionsExistedStillDecodes() {
        val decoded = Json.decodeFromString<HKINotification>(
            """{"id":"1","message":"Door opened","timestamp":42}"""
        )

        assertTrue(decoded.actions.isEmpty())
        assertNull(decoded.clickAction)
        assertNull(decoded.firedAction)
    }

    @Test
    fun actionDataSurvivesTheRoundTripThroughAPendingIntentExtra() {
        val original = parseNotificationActions(
            data("""{"actions":[{"action":"A","action_data":{"id":7,"name":"hall"}}]}""")
        ).single().actionData

        val restored = NotificationActions.decodeActionData(NotificationActions.encodeActionData(original))

        assertEquals(original, restored)
        assertNull(NotificationActions.decodeActionData(null))
        assertNull(NotificationActions.decodeActionData("not json"))
    }
}

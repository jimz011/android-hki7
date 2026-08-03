package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nested conditions, the shape Home Assistant's own editor writes for "A and (B or C)". These
 * automations arrive from HA, so reading them correctly matters as much as writing them.
 */
class AutomationGroupTest {

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun state(entity: String) =
        obj("""{"condition":"state","entity_id":"$entity","state":"on"}""")

    private val nested = listOf(
        state("binary_sensor.motion"),
        obj(
            """
            {"condition":"or","conditions":[
              {"condition":"state","entity_id":"sun.sun","state":"below_horizon"},
              {"condition":"and","conditions":[
                {"condition":"state","entity_id":"input_boolean.guest","state":"on"}
              ]}
            ]}
            """
        )
    )

    // ── reading groups ──────────────────────────────────────────────────

    @Test
    fun `group kinds are recognised as supported so HA-made flows stay editable`() {
        listOf("or", "and", "not").forEach { kind ->
            val block = obj("""{"condition":"$kind","conditions":[]}""")
            assertTrue(
                "$kind must be editable, not an opaque advanced block",
                isSupportedAutomationBlock(AutomationSection.CONDITION, block)
            )
        }
    }

    @Test
    fun `a leaf condition is not a group`() {
        assertNull(automationGroupChildren(AutomationSection.CONDITION, state("light.x")))
    }

    @Test
    fun `groups are only groups in the condition section`() {
        val block = obj("""{"condition":"or","conditions":[]}""")
        assertNull(automationGroupChildren(AutomationSection.TRIGGER, block))
        assertNull(automationGroupChildren(AutomationSection.ACTION, block))
    }

    @Test
    fun `a single child written as a bare object is read as one child`() {
        // HA accepts this shorthand, so refusing it would drop a real condition.
        val block = obj("""{"condition":"or","conditions":{"condition":"state","entity_id":"a.b","state":"on"}}""")

        assertEquals(1, automationGroupChildren(AutomationSection.CONDITION, block)?.size)
    }

    @Test
    fun `a group with no conditions key reads as empty rather than failing`() {
        val block = obj("""{"condition":"or"}""")

        assertEquals(emptyList<JsonObject>(), automationGroupChildren(AutomationSection.CONDITION, block))
    }

    // ── walking paths ───────────────────────────────────────────────────

    @Test
    fun `an empty path is the section's own list`() {
        assertEquals(nested, automationBlocksAtPath(nested, emptyList()))
    }

    @Test
    fun `a path descends into nested groups`() {
        val insideOr = automationBlocksAtPath(nested, listOf(1))
        assertEquals(2, insideOr.size)

        val insideNestedAnd = automationBlocksAtPath(nested, listOf(1, 1))
        assertEquals(1, insideNestedAnd.size)
        assertEquals("input_boolean.guest", insideNestedAnd.single().stringValue("entity_id"))
    }

    @Test
    fun `a path through a leaf or past the end resolves to nothing`() {
        assertEquals(emptyList<JsonObject>(), automationBlocksAtPath(nested, listOf(0)))
        assertEquals(emptyList<JsonObject>(), automationBlocksAtPath(nested, listOf(9)))
        assertEquals(emptyList<JsonObject>(), automationBlocksAtPath(nested, listOf(1, 0, 0)))
    }

    @Test
    fun `path existence distinguishes a live group from a stale one`() {
        assertTrue(automationPathExists(nested, listOf(1)))
        assertTrue(automationPathExists(nested, listOf(1, 1)))
        assertFalse(automationPathExists(nested, listOf(0)))
        assertFalse(automationPathExists(nested, listOf(4)))
    }

    // ── writing back ────────────────────────────────────────────────────

    @Test
    fun `replacing a nested list rebuilds the groups above it`() {
        val updated = withAutomationBlocksAtPath(nested, listOf(1, 1), listOf(state("light.new")))

        // The untouched sibling survives.
        assertEquals("binary_sensor.motion", updated[0].stringValue("entity_id"))
        val insideAnd = automationBlocksAtPath(updated, listOf(1, 1))
        assertEquals("light.new", insideAnd.single().stringValue("entity_id"))
        // The or-group keeps its other child.
        assertEquals(2, automationBlocksAtPath(updated, listOf(1)).size)
    }

    @Test
    fun `adding to a nested group leaves everything else alone`() {
        val siblings = automationBlocksAtPath(nested, listOf(1))
        val updated = withAutomationBlocksAtPath(nested, listOf(1), siblings + state("lock.front"))

        assertEquals(3, automationBlocksAtPath(updated, listOf(1)).size)
        assertEquals(2, updated.size)
        assertEquals("lock.front", automationBlocksAtPath(updated, listOf(1))[2].stringValue("entity_id"))
    }

    @Test
    fun `removing from a nested group keeps the group itself`() {
        val siblings = automationBlocksAtPath(nested, listOf(1)).filterIndexed { i, _ -> i != 0 }
        val updated = withAutomationBlocksAtPath(nested, listOf(1), siblings)

        assertEquals(1, automationBlocksAtPath(updated, listOf(1)).size)
        assertEquals("or", automationBlockKind(AutomationSection.CONDITION, updated[1]))
    }

    @Test
    fun `an empty path replaces the whole list`() {
        val replaced = withAutomationBlocksAtPath(nested, emptyList(), listOf(state("a.b")))

        assertEquals(1, replaced.size)
    }

    @Test
    fun `writing through a path that no longer exists changes nothing`() {
        assertEquals(nested, withAutomationBlocksAtPath(nested, listOf(7), listOf(state("a.b"))))
    }

    @Test
    fun `a new group starts empty and keeps its kind`() {
        val group = newAutomationBlock(AutomationSection.CONDITION, "or")

        assertEquals("or", automationBlockKind(AutomationSection.CONDITION, group))
        assertEquals(0, group["conditions"]?.jsonArray?.size)
        assertEquals(emptyList<JsonObject>(), automationGroupChildren(AutomationSection.CONDITION, group))
    }

    @Test
    fun `round-tripping a nested flow through the section writer preserves the structure`() {
        val config = obj("""{"alias":"x","triggers":[],"conditions":[],"actions":[]}""")
        val saved = withAutomationItems(config, AutomationSection.CONDITION, nested)
        val reread = automationItems(saved, AutomationSection.CONDITION)

        assertEquals(nested, reread)
        assertEquals(
            "input_boolean.guest",
            automationBlocksAtPath(reread, listOf(1, 1)).single().stringValue("entity_id")
        )
    }
}

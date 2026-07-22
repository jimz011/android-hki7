package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationModelsTest {
    @Test
    fun replacingSectionPreservesUnknownConfigAndBlocks() {
        val config = Json.parseToJsonElement(
            """{
              "alias":"Test",
              "trace":{"stored_traces":20},
              "triggers":[{"trigger":"mqtt","topic":"door"}],
              "conditions":[],
              "actions":[{"choose":[{"conditions":[],"sequence":[]}]}]
            }"""
        ) as JsonObject

        val updated = withAutomationItems(
            config,
            AutomationSection.CONDITION,
            listOf(newAutomationBlock(AutomationSection.CONDITION, "state"))
        )

        assertNotNull(updated["trace"])
        assertEquals(config["triggers"], updated["triggers"])
        assertEquals(config["actions"], updated["actions"])
        assertEquals(1, (updated["conditions"] as JsonArray).size)
    }

    @Test
    fun legacyKeysAreReadAndOnlyMigratedForEditedSection() {
        val config = Json.parseToJsonElement(
            """{"trigger":{"platform":"state","entity_id":"light.kitchen"},"action":{"service":"light.turn_on"}}"""
        ) as JsonObject

        assertEquals(1, automationItems(config, AutomationSection.TRIGGER).size)
        val updated = withAutomationItems(config, AutomationSection.TRIGGER, automationItems(config, AutomationSection.TRIGGER))

        assertFalse(updated.containsKey("trigger"))
        assertTrue(updated.containsKey("triggers"))
        assertTrue(updated.containsKey("action"))
    }

    @Test
    fun recipesUseCurrentHomeAssistantSchema() {
        assertTrue("Flows should offer at least eleven recipes", AutomationRecipe.entries.size >= 11)
        AutomationRecipe.entries.forEach { recipe ->
            val config = newAutomationConfig(recipe)
            assertTrue(config.containsKey("triggers"))
            assertTrue(config.containsKey("conditions"))
            assertTrue(config.containsKey("actions"))
            assertFalse(config.containsKey("trigger"))
            assertEquals("single", config["mode"]?.jsonPrimitive?.content)
            if (recipe == AutomationRecipe.BLANK) {
                assertTrue(automationItems(config, AutomationSection.TRIGGER).isEmpty())
                assertTrue(automationItems(config, AutomationSection.ACTION).isEmpty())
            } else {
                assertTrue(automationItems(config, AutomationSection.TRIGGER).isNotEmpty())
                assertTrue(automationItems(config, AutomationSection.ACTION).isNotEmpty())
            }
        }
    }

    @Test
    fun usefulRecipesPreconfigureTheirTriggerAndAction() {
        val arrival = newAutomationConfig(AutomationRecipe.ARRIVE_HOME)
        val arrivalTrigger = automationItems(arrival, AutomationSection.TRIGGER).single()
        val arrivalAction = automationItems(arrival, AutomationSection.ACTION).single()
        assertEquals("home", arrivalTrigger["to"]?.jsonPrimitive?.content)
        assertEquals("scene.turn_on", arrivalAction["action"]?.jsonPrimitive?.content)

        val sunrise = newAutomationConfig(AutomationRecipe.OPEN_COVERS_AT_SUNRISE)
        val sunriseTrigger = automationItems(sunrise, AutomationSection.TRIGGER).single()
        val sunriseAction = automationItems(sunrise, AutomationSection.ACTION).single()
        assertEquals("sunrise", sunriseTrigger["event"]?.jsonPrimitive?.content)
        assertEquals("cover.open_cover", sunriseAction["action"]?.jsonPrimitive?.content)
    }

    @Test
    fun unsupportedBlocksRemainMarkedAdvanced() {
        val block = Json.parseToJsonElement("""{"choose":[]}""") as JsonObject
        assertFalse(isSupportedAutomationBlock(AutomationSection.ACTION, block))
        assertTrue(automationBlockSummary(AutomationSection.ACTION, block).contains("kept unchanged"))
    }

    @Test
    fun shorthandElementsSurviveVisualSectionEdits() {
        val config = Json.parseToJsonElement(
            """{"conditions":["{{ is_state('sun.sun', 'above_horizon') }}",{"condition":"state","entity_id":"light.desk","state":"on"}]}"""
        ) as JsonObject
        val editedObject = newAutomationBlock(AutomationSection.CONDITION, "time")

        val updated = withAutomationItems(config, AutomationSection.CONDITION, listOf(editedObject))
        val conditions = automationElements(updated, AutomationSection.CONDITION)

        assertEquals(2, conditions.size)
        assertTrue(conditions.first().jsonPrimitive.content.contains("sun.sun"))
        assertEquals(editedObject, conditions.last())
    }

    @Test
    fun validationPayloadUsesCurrentPluralWebSocketKeys() {
        val payload = automationValidationPayload(newAutomationConfig(AutomationRecipe.ENTITY_STATE))

        assertEquals(setOf("triggers", "conditions", "actions"), payload.keys)
        assertFalse(payload.containsKey("trigger"))
        assertFalse(payload.containsKey("action"))
    }

    @Test
    fun stateSuggestionsIncludeDomainAndIntegrationOptions() {
        val select = HAEntity(
            entity_id = "input_select.house_mode",
            state = "Home",
            attributes = JsonObject(mapOf("options" to JsonArray(listOf(
                kotlinx.serialization.json.JsonPrimitive("Home"),
                kotlinx.serialization.json.JsonPrimitive("Away")
            ))))
        )
        val lock = HAEntity(entity_id = "lock.front_door", state = "locked")

        assertEquals(listOf("Home", "Away"), suggestedAutomationStates(select))
        assertTrue("unlocked" in suggestedAutomationStates(lock))
        assertTrue("locked" in suggestedAutomationStates(lock))
    }

    @Test
    fun savedAutomationCanBeResolvedThroughEntityRegistry() {
        val entity = HAEntity(entity_id = "automation.start_empty", state = "on")
        val registry = listOf(
            HAEntityRegistryEntry(
                entity_id = entity.entity_id,
                platform = "automation",
                unique_id = "1721500000000"
            )
        )

        assertEquals(
            entity.entity_id,
            loadedAutomationEntityId("1721500000000", listOf(entity), registry)
        )
    }

    @Test
    fun automationListIncludesRegisteredEntriesWithoutLiveState() {
        val live = HAEntity(entity_id = "automation.package_entry", state = "on")
        val registry = listOf(
            HAEntityRegistryEntry(
                entity_id = live.entity_id,
                platform = "automation",
                unique_id = "package-entry"
            ),
            HAEntityRegistryEntry(
                entity_id = "automation.built_in_entry",
                platform = "automation",
                unique_id = "built-in-entry"
            )
        )

        val combined = automationsIncludingRegistry(listOf(live), registry)

        assertEquals(2, combined.size)
        assertEquals("on", combined.first { it.entity_id == live.entity_id }.state)
        val registeredOnly = combined.first { it.entity_id == "automation.built_in_entry" }
        assertEquals("unavailable", registeredOnly.state)
        assertEquals("Built in entry", registeredOnly.friendlyName)
        assertEquals("built-in-entry", registeredOnly.attributes?.get("id")?.jsonPrimitive?.content)
    }
}

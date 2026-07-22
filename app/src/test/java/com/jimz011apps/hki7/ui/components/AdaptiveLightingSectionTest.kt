package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdaptiveLightingSectionTest {
    @Test
    fun profilesAreGroupedByConfigEntryAndNotEntityNames() {
        val registry = listOf(
            entry("switch.renamed_profile", "living_room"),
            entry("switch.renamed_sleep", "living_room_sleep_mode"),
            entry("switch.renamed_brightness", "living_room_adapt_brightness"),
            entry("switch.renamed_color", "living_room_adapt_color")
        )
        val entities = registry.associate { registryEntry ->
            registryEntry.entity_id to HAEntity(
                entity_id = registryEntry.entity_id,
                state = "on",
                attributes = buildJsonObject {
                    if (registryEntry.unique_id == "living_room") {
                        put("friendly_name", "Adaptive Lighting: Living Room")
                    }
                }
            )
        }

        val profiles = resolveAdaptiveLightingProfiles(registry, entities)

        assertEquals(1, profiles.size)
        assertEquals("Living Room", profiles.single().name)
        assertEquals("switch.renamed_profile", profiles.single().main.entity_id)
        assertEquals("switch.renamed_sleep", profiles.single().sleepMode?.entity_id)
        assertEquals("switch.renamed_brightness", profiles.single().adaptBrightness?.entity_id)
        assertEquals("switch.renamed_color", profiles.single().adaptColor?.entity_id)
    }

    @Test
    fun mainSwitchAloneStillProducesAProfile() {
        val registry = listOf(entry("switch.adaptive_lighting_office", "office"))
        val main = HAEntity(entity_id = registry.single().entity_id, state = "off")

        val profile = resolveAdaptiveLightingProfiles(registry, mapOf(main.entity_id to main)).singleOrNull()

        assertNotNull(profile)
        assertEquals("office", profile?.configEntryId)
    }

    @Test
    fun optionsFormProvidesConfiguredProfileMembers() {
        val form = buildJsonObject {
            put("data_schema", buildJsonArray {
                add(buildJsonObject {
                    put("name", "lights")
                    put("default", buildJsonArray {
                        add(JsonPrimitive("light.desk"))
                        add(JsonPrimitive("light.ceiling"))
                    })
                })
            })
        }

        assertEquals(
            setOf("light.desk", "light.ceiling"),
            adaptiveLightingLightsFromOptionsForm(form)
        )
    }

    @Test
    fun profileMembershipExpandsLightGroups() {
        val desk = HAEntity(entity_id = "light.desk", state = "on")
        val room = HAEntity(
            entity_id = "light.office",
            state = "on",
            attributes = buildJsonObject {
                put("entity_id", buildJsonArray { add(JsonPrimitive(desk.entity_id)) })
            }
        )
        val profile = AdaptiveLightingProfile(
            configEntryId = "office",
            name = "Office",
            main = HAEntity(entity_id = "switch.adaptive_lighting_office", state = "on"),
            sleepMode = null,
            adaptBrightness = null,
            adaptColor = null,
            configuredLightIds = setOf(room.entity_id)
        )

        val matches = adaptiveLightingProfilesForLight(
            desk,
            listOf(profile),
            mapOf(desk.entity_id to desk, room.entity_id to room)
        )

        assertEquals(listOf(profile), matches)
    }

    private fun entry(entityId: String, uniqueId: String) = HAEntityRegistryEntry(
        entity_id = entityId,
        platform = "adaptive_lighting",
        config_entry_id = "office",
        unique_id = uniqueId
    )
}

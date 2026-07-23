package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLightingModelsTest {
    @Test
    fun adaptiveLightingWidgetNameIsVisibleForExistingSavedWidgets() {
        val widget = Json.decodeFromString<HKIButtonStack>(
            """{"id":"adaptive","stackType":"adaptive_lighting"}"""
        )

        assertTrue(widget.showName)
    }

    @Test
    fun roomProfilesExpandConfiguredLightGroupsAndExcludeOtherRooms() {
        val livingLight = HAEntity("light.living_lamp", "off")
        val bedroomLight = HAEntity("light.bedroom_lamp", "off")
        val livingGroup = HAEntity(
            "light.downstairs",
            "off",
            buildJsonObject {
                put("entity_id", buildJsonArray {
                    add(JsonPrimitive(livingLight.entity_id))
                    add(JsonPrimitive(bedroomLight.entity_id))
                })
            }
        )
        val entities = listOf(livingLight, bedroomLight, livingGroup).associateBy(HAEntity::entity_id)

        assertEquals(
            listOf("shared-profile"),
            adaptiveLightingProfileIdsForArea(
                areaId = "living",
                profileMembers = mapOf(
                    "shared-profile" to setOf(livingGroup.entity_id),
                    "empty-profile" to emptySet()
                ),
                areaByEntity = mapOf(
                    livingLight.entity_id to "living",
                    bedroomLight.entity_id to "bedroom"
                ),
                entitiesById = entities
            )
        )
    }
}

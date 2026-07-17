package com.example.hki7.ui

import com.example.hki7.data.HAArea
import com.example.hki7.data.HADeviceRegistryEntry
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAEntityRegistryEntry
import com.example.hki7.data.HAFloor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardRegistrySnapshotTest {
    private val entities = listOf(HAEntity(entity_id = "light.desk", state = "on"))
    private val areas = listOf(HAArea(area_id = "office", name = "Office", floor_id = "ground"))
    private val floors = listOf(HAFloor(floor_id = "ground", name = "Ground floor"))
    private val registry = listOf(HAEntityRegistryEntry(entity_id = "light.desk", area_id = "office"))
    private val devices = listOf(HADeviceRegistryEntry(id = "dev1", area_id = "office"))

    @Test
    fun `full snapshot is complete`() {
        assertTrue(isCompleteDashboardRegistrySnapshot(entities, areas, floors, registry, devices))
    }

    @Test
    fun `snapshot without any areas or floors is still complete`() {
        // A server whose user never configured areas legitimately imports no rooms.
        assertTrue(isCompleteDashboardRegistrySnapshot(entities, emptyList(), emptyList(), registry, devices))
    }

    @Test
    fun `areas without floors is complete`() {
        assertTrue(isCompleteDashboardRegistrySnapshot(entities, areas, emptyList(), registry, devices))
    }

    @Test
    fun `floors without areas is incomplete`() {
        // Floors exist only to group areas; floors arriving with zero areas means the area
        // registry response was dropped — importing it shows floors with no rooms.
        assertFalse(isCompleteDashboardRegistrySnapshot(entities, emptyList(), floors, registry, devices))
    }

    @Test
    fun `empty entity registry is incomplete`() {
        // Without registry entries no entity can be mapped to a room: every imported room
        // would be empty even though areas and floors look fine.
        assertFalse(isCompleteDashboardRegistrySnapshot(entities, areas, floors, emptyList(), devices))
    }

    @Test
    fun `empty device registry is incomplete`() {
        assertFalse(isCompleteDashboardRegistrySnapshot(entities, areas, floors, registry, emptyList()))
    }

    @Test
    fun `empty states list is incomplete`() {
        assertFalse(isCompleteDashboardRegistrySnapshot(emptyList(), areas, floors, registry, devices))
    }
}

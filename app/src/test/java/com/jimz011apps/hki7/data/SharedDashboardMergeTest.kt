package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedDashboardMergeTest {

    @Test
    fun `structural updates apply while local aesthetics are preserved`() {
        val local = HKIDashboard(
            id = "shared-abc",
            name = "My copy",
            areaConfigs = mapOf(
                "living" to HKIAreaConfig(icon = "local-icon", wallpaper = "local-wall")
            ),
            areaWidgets = mapOf(
                "living" to listOf(
                    HKIButtonStack(
                        id = "w1",
                        title = "My Lights",
                        icon = "local-star",
                        columns = 4,
                        entityIds = listOf("light.a"),
                        buttonConfigs = mapOf(
                            "light.a" to HKIButtonConfig(name = "My Lamp", icon = "local-bulb", tapAction = "toggle")
                        )
                    )
                )
            )
        )
        val incoming = HKIDashboard(
            id = "abc",
            name = "Family dashboard",
            areaConfigs = mapOf(
                "living" to HKIAreaConfig(icon = "owner-icon", wallpaper = "owner-wall", climateEntityId = "climate.x")
            ),
            areaWidgets = mapOf(
                "living" to listOf(
                    HKIButtonStack(
                        id = "w1",
                        title = "Lights",
                        icon = "owner-star",
                        columns = 2,
                        // Owner added a second light and changed the tap action (structural).
                        entityIds = listOf("light.a", "light.b"),
                        buttonConfigs = mapOf(
                            "light.a" to HKIButtonConfig(name = "Lamp", icon = "owner-bulb", tapAction = "more_info"),
                            "light.b" to HKIButtonConfig(name = "Lamp B")
                        )
                    )
                )
            )
        )

        val merged = mergeSharedDashboardAesthetics(local, incoming)

        // Identity + name stay local.
        assertEquals("shared-abc", merged.id)
        assertEquals("My copy", merged.name)

        // Room aesthetics preserved; structural binding taken from the owner.
        val area = merged.areaConfigs.getValue("living")
        assertEquals("local-icon", area.icon)
        assertEquals("local-wall", area.wallpaper)
        assertEquals("climate.x", area.climateEntityId)

        val stack = merged.areaWidgets.getValue("living").single() as HKIButtonStack
        // Aesthetic fields from local.
        assertEquals("My Lights", stack.title)
        assertEquals("local-star", stack.icon)
        assertEquals(4, stack.columns)
        // Structure from the owner.
        assertEquals(listOf("light.a", "light.b"), stack.entityIds)
        // Existing button: visual from local, action from the owner.
        val cfgA = stack.buttonConfigs.getValue("light.a")
        assertEquals("My Lamp", cfgA.name)
        assertEquals("local-bulb", cfgA.icon)
        assertEquals("more_info", cfgA.tapAction)
        // Newly added button carried over from the owner.
        assertTrue(stack.buttonConfigs.containsKey("light.b"))
    }

    @Test
    fun `configs for owner-removed entities are dropped`() {
        val local = HKIDashboard(
            id = "shared-x",
            name = "Local",
            areaWidgets = mapOf(
                "room" to listOf(
                    HKIButtonStack(
                        id = "w",
                        entityIds = listOf("switch.a", "switch.b"),
                        buttonConfigs = mapOf(
                            "switch.a" to HKIButtonConfig(name = "A"),
                            "switch.b" to HKIButtonConfig(name = "B")
                        )
                    )
                )
            )
        )
        val incoming = HKIDashboard(
            id = "x",
            name = "Owner",
            areaWidgets = mapOf(
                "room" to listOf(
                    HKIButtonStack(id = "w", entityIds = listOf("switch.a"))
                )
            )
        )

        val stack = mergeSharedDashboardAesthetics(local, incoming)
            .areaWidgets.getValue("room").single() as HKIButtonStack
        assertEquals(listOf("switch.a"), stack.entityIds)
        // switch.a keeps the recipient's name; switch.b (removed by owner) is gone.
        assertEquals("A", stack.buttonConfigs["switch.a"]?.name)
        assertTrue(!stack.buttonConfigs.containsKey("switch.b"))
    }
}

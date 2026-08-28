package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearRoomBuilderTest {

    private val kitchen = HAArea(area_id = "kitchen", name = "Kitchen", icon = "mdi:silverware")
    private val hall = HAArea(area_id = "hall", name = "Hall")

    private fun stack(
        vararg entityIds: String,
        hidden: Boolean = false,
        buttonConfigs: Map<String, HKIButtonConfig> = emptyMap(),
    ) = HKIButtonStack(
        id = "stack-${entityIds.joinToString("-")}",
        entityIds = entityIds.toList(),
        isHidden = hidden,
        buttonConfigs = buttonConfigs,
    )

    private fun build(
        areas: List<HAArea> = listOf(kitchen, hall),
        widgets: Map<String, List<HKIRoomWidget>> = emptyMap(),
        configs: Map<String, HKIAreaConfig> = emptyMap(),
        known: Set<String>? = null,
        isAllowed: (String) -> Boolean = { true },
    ): List<WearRoom> {
        val discovered = known ?: buildSet {
            widgets.values.flatten().forEach { widget ->
                if (widget is HKIButtonStack) addAll(widget.entityIds)
                if (widget is HKISingleEntityWidget) add(widget.entityId)
                if (widget is HKIMediaPlayerWidget) add(widget.entityId)
                if (widget is HKISwipingStack) {
                    widget.widgets.filterIsInstance<HKIButtonStack>().forEach { addAll(it.entityIds) }
                }
            }
            configs.values.forEach { config ->
                addAll(config.climateEntityIds)
                config.climateEntityId?.let(::add)
                addAll(config.lockEntityIds)
                addAll(config.blindEntityIds)
                addAll(config.mediaPlayerEntityIds)
            }
        }
        return buildWearRooms(areas, widgets, configs, discovered, isAllowed)
    }

    @Test
    fun `takes a room's buttons from the dashboard`() {
        val rooms = build(widgets = mapOf("kitchen" to listOf(stack("light.ceiling", "switch.kettle"))))
        assertEquals(listOf("light.ceiling", "switch.kettle"), rooms.single().entityIds)
        assertEquals("Kitchen", rooms.single().name)
    }

    @Test
    fun `a button hidden on the dashboard is hidden on the watch`() {
        // The whole point of reading the dashboard: a motion switch the user hid stays hidden.
        val rooms = build(
            widgets = mapOf(
                "kitchen" to listOf(
                    stack(
                        "light.ceiling",
                        "switch.motion_detection",
                        buttonConfigs = mapOf("switch.motion_detection" to HKIButtonConfig(hidden = true)),
                    ),
                ),
            ),
        )
        assertEquals(listOf("light.ceiling"), rooms.single().entityIds)
    }

    @Test
    fun `a hidden widget contributes nothing`() {
        val rooms = build(
            widgets = mapOf("kitchen" to listOf(stack("light.ceiling", hidden = true))),
        )
        assertTrue(rooms.isEmpty())
    }

    @Test
    fun `thermostats come from the area config, where the dashboard keeps them`() {
        // Climate entities are never button-stack entries; auto-populate files them here.
        val rooms = build(
            widgets = mapOf("kitchen" to listOf(stack("light.ceiling"))),
            configs = mapOf("kitchen" to HKIAreaConfig(climateEntityIds = listOf("climate.kitchen"))),
        )
        assertTrue("climate.kitchen" in rooms.single().entityIds)
    }

    @Test
    fun `a room with only a thermostat still appears`() {
        val rooms = build(
            configs = mapOf("hall" to HKIAreaConfig(climateEntityIds = listOf("climate.hall"))),
        )
        assertEquals(listOf("climate.hall"), rooms.single().entityIds)
    }

    @Test
    fun `nested swiping stacks are followed`() {
        val rooms = build(
            widgets = mapOf(
                "kitchen" to listOf(
                    HKISwipingStack(id = "swipe", widgets = listOf(stack("light.ceiling"))),
                ),
            ),
        )
        assertEquals(listOf("light.ceiling"), rooms.single().entityIds)
    }

    @Test
    fun `single entity and media player widgets contribute their entity`() {
        val rooms = build(
            widgets = mapOf(
                "kitchen" to listOf(
                    HKISingleEntityWidget(id = "one", entityId = "light.spot"),
                    HKIMediaPlayerWidget(id = "mp", entityId = "media_player.radio"),
                ),
            ),
        )
        assertEquals(listOf("light.spot", "media_player.radio"), rooms.single().entityIds)
    }

    @Test
    fun `domains a tap cannot act on are left out`() {
        // The dashboard shows these under headings that explain them; a watch row cannot.
        val rooms = build(
            widgets = mapOf(
                "kitchen" to listOf(
                    stack("light.ceiling", "sensor.temperature", "binary_sensor.motion", "camera.door"),
                ),
            ),
        )
        assertEquals(listOf("light.ceiling"), rooms.single().entityIds)
    }

    @Test
    fun `entities the dashboard lists but Home Assistant no longer has are dropped`() {
        val rooms = build(
            widgets = mapOf("kitchen" to listOf(stack("light.removed"))),
            known = emptySet(),
        )
        assertTrue(rooms.isEmpty())
    }

    @Test
    fun `honours the household policy filter`() {
        val rooms = build(
            widgets = mapOf("kitchen" to listOf(stack("light.ceiling", "lock.front"))),
            isAllowed = { it != "lock.front" },
        )
        assertEquals(listOf("light.ceiling"), rooms.single().entityIds)
    }

    @Test
    fun `an entity listed twice appears once`() {
        val rooms = build(
            widgets = mapOf("kitchen" to listOf(stack("light.ceiling"), stack("light.ceiling"))),
        )
        assertEquals(listOf("light.ceiling"), rooms.single().entityIds)
    }

    @Test
    fun `caps a room so it does not become a scroll on a watch`() {
        val many = (1..40).map { "light.bulb$it" }
        val rooms = build(widgets = mapOf("kitchen" to listOf(stack(*many.toTypedArray()))))
        assertEquals(20, rooms.single().entityIds.size)
    }

    @Test
    fun `rooms are sorted by name and empty ones omitted`() {
        val rooms = build(
            widgets = mapOf(
                "kitchen" to listOf(stack("light.a")),
                "hall" to listOf(stack("light.b")),
            ),
        )
        assertEquals(listOf("Hall", "Kitchen"), rooms.map { it.name })
    }

    @Test
    fun `carries the area icon through so the watch looks like the phone`() {
        val rooms = build(widgets = mapOf("kitchen" to listOf(stack("light.ceiling"))))
        assertEquals("mdi:silverware", rooms.single().icon)
    }

    @Test
    fun `no areas means nothing to send`() {
        assertTrue(build(areas = emptyList(), widgets = mapOf("kitchen" to listOf(stack("light.a")))).isEmpty())
    }
}

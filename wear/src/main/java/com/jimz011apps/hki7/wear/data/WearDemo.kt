package com.jimz011apps.hki7.wear.data

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.data.WearRoom
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A URL that can never resolve, used only as a durable marker for the offline watch demo. */
const val WEAR_DEMO_SERVER_URL = "https://wear-demo.hki7.invalid"
const val WEAR_DEMO_REFRESH_TOKEN = "hki7-wear-demo"

/** Stable sample content shared by the app, tiles and complication during Play review. */
object WearDemo {
    val quickActions = listOf(
        HKIQuickAction(
            id = "demo-living-light",
            entityId = "light.living_room",
            name = "Living room",
            icon = "mdi:ceiling-light",
        ),
        HKIQuickAction(
            id = "demo-coffee",
            entityId = "switch.coffee_machine",
            name = "Coffee machine",
            icon = "mdi:coffee-maker",
        ),
        HKIQuickAction(
            id = "demo-bedroom-light",
            entityId = "light.bedroom",
            name = "Bedroom",
            icon = "mdi:bed",
        ),
    )

    val rooms = listOf(
        WearRoom(
            id = "demo-living-room",
            name = "Living room",
            icon = "mdi:sofa",
            entityIds = listOf(
                "light.living_room",
                "switch.coffee_machine",
                "climate.living_room",
            ),
        ),
        WearRoom(
            id = "demo-bedroom",
            name = "Bedroom",
            icon = "mdi:bed",
            entityIds = listOf("light.bedroom"),
        ),
    )

    const val thermostatEntityId = "climate.living_room"

    private val mutex = Mutex()
    private val entities = initialEntities().associateByTo(linkedMapOf(), HAEntity::entity_id)

    suspend fun state(entityId: String): HAEntity? = mutex.withLock { entities[entityId] }

    suspend fun toggle(entityId: String) = mutex.withLock {
        val current = entities[entityId] ?: return@withLock
        val next = if (entityId.startsWith("climate.")) {
            if (current.state == "off") "heat" else "off"
        } else {
            if (current.state == "on") "off" else "on"
        }
        entities[entityId] = current.copy(state = next)
    }

    suspend fun callService(domain: String, service: String, payload: JsonObject) = mutex.withLock {
        val entityId = payload["entity_id"]?.jsonPrimitive?.content ?: return@withLock
        if (domain == "climate" && service == "set_temperature") {
            val target = payload["temperature"]?.jsonPrimitive?.doubleOrNull ?: return@withLock
            val current = entities[entityId] ?: return@withLock
            val attributes = buildJsonObject {
                current.attributes?.forEach { (key, value) -> put(key, value) }
                put("temperature", target)
            }
            entities[entityId] = current.copy(attributes = attributes)
        }
    }

    private fun initialEntities(): List<HAEntity> = listOf(
        entity("light.living_room", "on", "Living room light", "mdi:ceiling-light"),
        entity("switch.coffee_machine", "off", "Coffee machine", "mdi:coffee-maker"),
        entity("light.bedroom", "off", "Bedroom light", "mdi:bed"),
        HAEntity(
            entity_id = thermostatEntityId,
            state = "heat",
            attributes = buildJsonObject {
                put("friendly_name", "Living room thermostat")
                put("icon", "mdi:thermostat")
                put("temperature", 21.0)
                put("current_temperature", 20.4)
                put("min_temp", 7.0)
                put("max_temp", 30.0)
                put("target_temp_step", 0.5)
            },
        ),
    )

    private fun entity(id: String, state: String, name: String, icon: String): HAEntity =
        HAEntity(
            entity_id = id,
            state = state,
            attributes = buildJsonObject {
                put("friendly_name", name)
                put("icon", icon)
            },
        )
}

/** Small transport boundary so production REST and the offline demo exercise the same UI. */
interface WearHomeClient {
    suspend fun state(entityId: String): HAEntity?
    suspend fun callService(domain: String, service: String, payload: JsonObject)
    suspend fun toggle(entityId: String)
}

object WearDemoClient : WearHomeClient {
    override suspend fun state(entityId: String): HAEntity? = WearDemo.state(entityId)

    override suspend fun callService(domain: String, service: String, payload: JsonObject) {
        WearDemo.callService(domain, service, payload)
    }

    override suspend fun toggle(entityId: String) {
        WearDemo.toggle(entityId)
    }
}

suspend fun createWearHomeClient(
    prefs: WearPreferences,
    session: WearSession,
): WearHomeClient? {
    val serverUrl = prefs.serverUrlOnce() ?: return null
    if (serverUrl == WEAR_DEMO_SERVER_URL) return WearDemoClient
    if (!session.isAuthenticated()) return null
    return HomeAssistantRest(serverUrl, session)
}

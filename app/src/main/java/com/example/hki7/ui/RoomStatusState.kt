package com.example.hki7.ui

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAreaConfig
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/** Stable keys used to persist entity groups in [HKIAreaConfig.roomStatusEntityIds]. */
internal object RoomStatusRoles {
    const val DOORS = "doors"
    const val WINDOWS = "windows"
    const val MOTION = "motion"
    const val PRESENCE = "presence"
    const val LIGHTS = "lights"
    const val DEVICES = "devices"
    const val SMOKE = "smoke"
    const val GAS = "gas"
    const val FIRE = "fire"

    val ORDERED: List<String> = listOf(
        DOORS,
        WINDOWS,
        MOTION,
        PRESENCE,
        LIGHTS,
        DEVICES,
        SMOKE,
        GAS,
        FIRE
    )
}

internal data class RoomStatusDiscovery(
    val entityIds: Map<String, List<String>>,
    val temperatureEntityIds: List<String>,
    val humidityEntityIds: List<String>
)

internal data class RoomStatusIndicator(
    val role: String,
    val count: Int
)

internal data class RoomStatusSummary(
    val indicators: List<RoomStatusIndicator>,
    val temperature: String?,
    val humidity: String?
) {
    val environmentText: String?
        get() = listOfNotNull(temperature, humidity)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")
}

/**
 * Finds the default room-summary sources among entities already assigned to one HA area.
 * Classification intentionally uses entity metadata rather than current state so a temporarily
 * unavailable dedicated sensor remains selected and starts working again when HA recovers it.
 * Climate measurements take priority because they describe the actual climate controller reading.
 */
internal fun discoverRoomStatus(
    entities: List<HAEntity>,
    excludedWindowEntityIds: Set<String> = emptySet()
): RoomStatusDiscovery {
    val uniqueEntities = entities.distinctBy { it.entity_id }
    val byRole = RoomStatusRoles.ORDERED.associateWith { mutableListOf<String>() }

    uniqueEntities.forEach { entity ->
        val domain = entity.domain
        val role = when {
            domain == "light" -> RoomStatusRoles.LIGHTS
            domain == "switch" -> RoomStatusRoles.DEVICES
            domain == "cover" && entity.normalizedDeviceClass in setOf("door", "garage", "garage_door", "gate") -> RoomStatusRoles.DOORS
            domain == "cover" && entity.normalizedDeviceClass == "window" &&
                entity.entity_id !in excludedWindowEntityIds -> RoomStatusRoles.WINDOWS
            domain != "binary_sensor" -> null
            else -> when (entity.normalizedDeviceClass) {
                "door", "garage_door" -> RoomStatusRoles.DOORS
                "window" -> RoomStatusRoles.WINDOWS.takeIf {
                    entity.entity_id !in excludedWindowEntityIds
                }
                "motion", "moving", "vibration" -> RoomStatusRoles.MOTION
                "occupancy", "presence" -> RoomStatusRoles.PRESENCE
                "smoke" -> RoomStatusRoles.SMOKE
                "gas", "carbon_monoxide" -> RoomStatusRoles.GAS
                "fire", "heat", "safety" -> RoomStatusRoles.FIRE
                else -> null
            }
        }
        if (role != null) byRole.getValue(role).add(entity.entity_id)
    }

    val climateTemperatureIds = uniqueEntities.filter {
        it.domain == "climate" && it.attributeNumber("current_temperature") != null
    }.map(HAEntity::entity_id)
    val climateHumidityIds = uniqueEntities.filter {
        it.domain == "climate" && it.attributeNumber("current_humidity") != null
    }.map(HAEntity::entity_id)
    val temperatureSensorIds = uniqueEntities.filter {
        it.domain == "sensor" && it.normalizedDeviceClass == "temperature"
    }.map(HAEntity::entity_id)
    val humiditySensorIds = uniqueEntities.filter {
        it.domain == "sensor" && it.normalizedDeviceClass == "humidity"
    }.map(HAEntity::entity_id)

    return RoomStatusDiscovery(
        entityIds = RoomStatusRoles.ORDERED.mapNotNull { role ->
            byRole.getValue(role).distinct().takeIf { it.isNotEmpty() }?.let { role to it }
        }.toMap(linkedMapOf()),
        temperatureEntityIds = climateTemperatureIds.ifEmpty { temperatureSensorIds },
        humidityEntityIds = climateHumidityIds.ifEmpty { humiditySensorIds }
    )
}

/** Resolves configured room entities against the latest HA state snapshot. */
internal fun resolveRoomStatus(
    config: HKIAreaConfig,
    entities: List<HAEntity>
): RoomStatusSummary {
    val entitiesById = entities.associateBy { it.entity_id }
    val indicators = RoomStatusRoles.ORDERED.mapNotNull { role ->
        val count = config.roomStatusEntityIds[role]
            .orEmpty()
            .distinct()
            .mapNotNull(entitiesById::get)
            .count { it.isActiveForRoomRole(role) }
        count.takeIf { it > 0 }?.let { RoomStatusIndicator(role, it) }
    }

    return RoomStatusSummary(
        indicators = indicators,
        temperature = averageRoomMeasurement(
            entityIds = config.roomTemperatureEntityIds
                .takeIf { it.isNotEmpty() }
                ?: listOfNotNull(config.roomTemperatureEntityId),
            entitiesById = entitiesById,
            type = RoomMeasurementType.TEMPERATURE
        ),
        humidity = averageRoomMeasurement(
            entityIds = config.roomHumidityEntityIds
                .takeIf { it.isNotEmpty() }
                ?: listOfNotNull(config.roomHumidityEntityId),
            entitiesById = entitiesById,
            type = RoomMeasurementType.HUMIDITY
        )
    )
}

/** Every entity needed to keep a room summary live, in stable display order without duplicates. */
internal fun HKIAreaConfig.roomEntityIds(): List<String> = buildList {
    RoomStatusRoles.ORDERED.forEach { role ->
        addAll(roomStatusEntityIds[role].orEmpty())
    }
    roomStatusEntityIds.keys
        .filterNot(RoomStatusRoles.ORDERED::contains)
        .sorted()
        .forEach { role -> addAll(roomStatusEntityIds[role].orEmpty()) }
    addAll(
        roomTemperatureEntityIds.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(roomTemperatureEntityId)
    )
    addAll(
        roomHumidityEntityIds.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(roomHumidityEntityId)
    )
}.filter(String::isNotBlank).distinct()

private val HAEntity.domain: String
    get() = entity_id.substringBefore('.', missingDelimiterValue = "").lowercase(Locale.ROOT)

private val HAEntity.normalizedDeviceClass: String?
    get() = deviceClass?.trim()?.lowercase(Locale.ROOT)

private fun HAEntity.attributeNumber(name: String): Double? =
    attributes?.get(name)?.jsonPrimitive?.doubleOrNull?.takeIf(Double::isFinite)

private fun HAEntity.isActiveForRoomRole(role: String): Boolean {
    val normalizedState = state.trim().lowercase(Locale.ROOT)
    if (normalizedState in UNAVAILABLE_STATES) return false

    return when (role) {
        RoomStatusRoles.DOORS,
        RoomStatusRoles.WINDOWS -> {
            normalizedState in OPENING_STATES ||
                (domain == "cover" && (attributeNumber("current_position") ?: 0.0) > 0.0)
        }
        RoomStatusRoles.MOTION -> normalizedState in MOTION_STATES
        RoomStatusRoles.PRESENCE -> normalizedState in PRESENCE_STATES
        RoomStatusRoles.LIGHTS,
        RoomStatusRoles.DEVICES -> normalizedState == "on"
        RoomStatusRoles.SMOKE -> normalizedState in SMOKE_STATES
        RoomStatusRoles.GAS -> normalizedState in GAS_STATES
        RoomStatusRoles.FIRE -> normalizedState in FIRE_STATES
        else -> false
    }
}

private enum class RoomMeasurementType {
    TEMPERATURE,
    HUMIDITY
}

private data class RoomMeasurement(
    val value: Double,
    val unit: String?
)

private fun averageRoomMeasurement(
    entityIds: List<String>,
    entitiesById: Map<String, HAEntity>,
    type: RoomMeasurementType
): String? {
    val configuredIds = entityIds.distinct()
    val climateIds = configuredIds.filter {
        it.substringBefore('.').equals("climate", ignoreCase = true)
    }
    val winningIds = climateIds.takeIf { it.isNotEmpty() }
        ?: configuredIds.filter { it.substringBefore('.').equals("sensor", ignoreCase = true) }
    val winningEntities = winningIds.mapNotNull(entitiesById::get)
    val measurements = winningEntities.mapNotNull { it.roomMeasurement(type) }
    if (measurements.isEmpty()) return null

    val average = measurements.map(RoomMeasurement::value).average()
        .takeIf(Double::isFinite)
        ?: return null
    val firstUnit = measurements.firstNotNullOfOrNull { measurement ->
        measurement.unit?.trim()?.takeIf(String::isNotEmpty)
    } ?: "%".takeIf { type == RoomMeasurementType.HUMIDITY }
    return formatRoomMeasurement(average, firstUnit)
}

private fun HAEntity.roomMeasurement(type: RoomMeasurementType): RoomMeasurement? {
    val value = when {
        domain == "climate" && type == RoomMeasurementType.TEMPERATURE -> {
            attributeNumber("current_temperature")
        }
        domain == "climate" && type == RoomMeasurementType.HUMIDITY -> {
            attributeNumber("current_humidity")
        }
        domain == "sensor" -> state.toFiniteDoubleOrNull()
        else -> null
    } ?: return null
    val unit = when {
        domain == "climate" && type == RoomMeasurementType.TEMPERATURE -> {
            attributes?.get("temperature_unit")?.jsonPrimitive?.contentOrNull
                ?: attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
        }
        domain == "climate" && type == RoomMeasurementType.HUMIDITY -> {
            attributes?.get("humidity_unit")?.jsonPrimitive?.contentOrNull
                ?: attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
        }
        else -> attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
    }
    return RoomMeasurement(value, unit)
}

private fun String.toFiniteDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf(Double::isFinite)

private fun formatRoomMeasurement(value: Double, rawUnit: String?): String {
    val compactValue = String.format(Locale.US, "%.2f", value)
        .trimEnd('0')
        .trimEnd('.')
    val unit = rawUnit?.trim().orEmpty()
    if (unit.isEmpty()) return compactValue
    val separator = if (unit == "%" || unit.startsWith("°")) "" else " "
    return "$compactValue$separator$unit"
}

private val UNAVAILABLE_STATES = setOf("", "unknown", "unavailable", "none", "null")
private val OPENING_STATES = setOf("on", "open", "opening", "closing")
private val MOTION_STATES = setOf("on", "active", "detected", "motion", "moving", "vibrating", "vibration", "triggered")
private val PRESENCE_STATES = setOf("home", "on", "present", "occupied", "detected")
private val SMOKE_STATES = setOf("on", "active", "alarm", "detected", "smoke", "smoking", "triggered", "unsafe")
private val GAS_STATES = setOf("on", "active", "alarm", "detected", "gas", "leak", "triggered", "unsafe")
private val FIRE_STATES = setOf("on", "active", "alarm", "detected", "fire", "heat", "hot", "triggered", "unsafe")

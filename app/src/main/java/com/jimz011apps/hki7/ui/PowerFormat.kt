package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * A power sensor's reading in watts, or null when it is not a usable power reading (unavailable,
 * non-numeric, or a unit that is not power). kW and MW are converted so sources mix freely.
 */
internal fun HAEntity.powerWatts(): Double? {
    val value = state.trim().toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
    val unit = attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull?.trim()
    return when (unit?.lowercase(Locale.ROOT)) {
        null, "", "w" -> value
        "kw" -> value * 1_000
        "mw" -> if (unit == "mW") value / 1_000 else value * 1_000_000
        else -> null
    }
}

/** Sum of every usable reading in [entities], or null when none of them reads. */
internal fun totalPowerWatts(entities: List<HAEntity>): Double? =
    entities.mapNotNull { it.powerWatts() }.takeIf { it.isNotEmpty() }?.sum()

/**
 * Compact power label with at most one decimal and no space: "440W", "12.5W", "1500W", "12.3kW".
 * Watts up to 10 kW, since a room or a single device rarely goes beyond that and "2350W" reads
 * better beside a temperature than "2.4kW" does.
 */
internal fun formatPowerWatts(watts: Double): String {
    val (value, unit) = if (kotlin.math.abs(watts) >= 10_000) watts / 1_000 to "kW" else watts to "W"
    val text = String.format(Locale.US, "%.1f", value).removeSuffix(".0")
    return if (text == "-0") "0$unit" else "$text$unit"
}

/** A sensor that reports power (not energy): device_class power, or a W/kW/MW unit. */
internal fun HAEntity.isPowerSensor(): Boolean {
    if (!entity_id.startsWith("sensor.")) return false
    if (deviceClass?.equals("power", ignoreCase = true) == true) return true
    val unit = attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull?.trim()?.lowercase(Locale.ROOT)
    return unit == "w" || unit == "kw" || unit == "mw"
}

/** "300W" for the button's chosen power sensor, or null when none is set or it does not read. */
internal fun powerLabel(powerEntityId: String?, allEntities: List<HAEntity>): String? {
    val id = powerEntityId?.takeIf(String::isNotBlank) ?: return null
    return allEntities.firstOrNull { it.entity_id == id }?.powerWatts()?.let(::formatPowerWatts)
}

/**
 * The power sensor that most likely measures [entityId]: a PowerCalc sensor made for it (PowerCalc
 * names its source in the `source_entity` attribute, or shares the source's device), else a power
 * sensor on the same device, as a smart plug has. Null when nothing fits.
 */
internal fun guessPowerEntityId(
    entityId: String?,
    allEntities: List<HAEntity>,
    registry: List<com.jimz011apps.hki7.data.HAEntityRegistryEntry>
): String? {
    if (entityId.isNullOrBlank() || entityId.startsWith("sensor.")) return null
    val byId = registry.associateBy { it.entity_id }
    val deviceId = byId[entityId]?.device_id?.takeIf(String::isNotBlank)
    val powerSensors = allEntities.filter { it.isPowerSensor() }
    fun HAEntity.sourceEntity() = attributes?.get("source_entity")?.jsonPrimitive?.contentOrNull
    fun HAEntity.isPowerCalc() = byId[entity_id]?.platform == "powercalc"
    val sameDevice = { e: HAEntity -> deviceId != null && byId[e.entity_id]?.device_id == deviceId }
    return (powerSensors.firstOrNull { it.isPowerCalc() && it.sourceEntity() == entityId }
        ?: powerSensors.firstOrNull { it.sourceEntity() == entityId }
        ?: powerSensors.firstOrNull { it.isPowerCalc() && sameDevice(it) }
        ?: powerSensors.firstOrNull(sameDevice))
        ?.entity_id
}

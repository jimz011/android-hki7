package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry

/**
 * Finds window-class entities exposed by the same physical HA device as a climate entity.
 * Thermostats commonly expose these as open-window detection signals; they are not real room
 * windows and therefore should not become room opening indicators. Only direct registry device
 * ownership is considered—hub/bridge relationships are intentionally irrelevant here.
 */
internal fun climateOwnedWindowEntityIds(
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): Set<String> {
    val deviceIdByEntityId = registry.asSequence()
        .mapNotNull { entry -> entry.device_id?.let { entry.entity_id to it } }
        .toMap()
    val climateDeviceIds = registry.asSequence()
        .filter { it.entity_id.substringBefore('.').equals("climate", ignoreCase = true) }
        .mapNotNull(HAEntityRegistryEntry::device_id)
        .toSet()
    if (climateDeviceIds.isEmpty()) return emptySet()

    return entities.asSequence()
        .filter {
            val domain = it.entity_id.substringBefore('.').lowercase()
            (domain == "binary_sensor" || domain == "cover") &&
                it.deviceClass.equals("window", ignoreCase = true)
        }
        .filter { deviceIdByEntityId[it.entity_id] in climateDeviceIds }
        .map(HAEntity::entity_id)
        .toSet()
}

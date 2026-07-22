@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HKIButtonConfig

data class VacuumEntities(
    val map: HAEntity?,
    val battery: HAEntity?,
    val water: HAEntity?,
    val emptyBin: HAEntity?
)

/** Auto-detects the vacuum's helper entities (map camera, battery, water level, empty bin)
 *  among the entities registered to the given device. */
fun resolveVacuumDeviceEntities(
    deviceId: String?,
    allEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): VacuumEntities {
    val byId = allEntities.associateBy { it.entity_id }
    val deviceEntities = deviceId?.let { id ->
        registry.asSequence().filter { it.device_id == id }.mapNotNull { byId[it.entity_id] }.toList()
    }.orEmpty()
    fun text(e: HAEntity) = "${e.entity_id} ${e.friendlyName.orEmpty()}".lowercase()
    val map = deviceEntities.firstOrNull {
        it.entity_id.startsWith("camera.") && listOf("map", "kaart", "floor").any(text(it)::contains)
    } ?: deviceEntities.firstOrNull { it.entity_id.startsWith("camera.") }
    val battery = deviceEntities.firstOrNull {
        it.deviceClass == "battery" || "battery" in text(it) || "accu" in text(it)
    }
    val water = deviceEntities.firstOrNull {
        (it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.")) &&
            listOf("water", "mop", "dweil").any(text(it)::contains)
    }
    val empty = deviceEntities.firstOrNull {
        (it.entity_id.startsWith("button.") || it.entity_id.startsWith("switch.")) &&
            listOf("empty", "dust bin", "dustbin", "stofbak", "auto empty").any(text(it)::contains)
    }
    return VacuumEntities(map, battery, water, empty)
}

fun resolveVacuumEntities(
    config: HKIButtonConfig?,
    allEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): VacuumEntities {
    val byId = allEntities.associateBy { it.entity_id }
    fun manual(id: String?) = id?.let(byId::get)
    val auto = resolveVacuumDeviceEntities(config?.vacuumDeviceId, allEntities, registry)
    // A manually-picked entity always wins over device auto-detection, so the dialog matches the
    // card (which reads the manual ids directly). Auto-detection only fills the unset slots.
    return VacuumEntities(
        map = manual(config?.vacuumMapEntityId) ?: auto.map,
        battery = manual(config?.vacuumBatteryEntityId) ?: auto.battery,
        water = manual(config?.vacuumWaterEntityId) ?: auto.water,
        emptyBin = manual(config?.vacuumEmptyBinEntityId) ?: auto.emptyBin
    )
}

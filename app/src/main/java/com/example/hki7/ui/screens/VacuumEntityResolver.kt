package com.example.hki7.ui.screens

import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAEntityRegistryEntry
import com.example.hki7.data.HKIButtonConfig

data class VacuumEntities(
    val map: HAEntity?,
    val battery: HAEntity?,
    val water: HAEntity?,
    val emptyBin: HAEntity?
)

fun resolveVacuumEntities(
    config: HKIButtonConfig?,
    allEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>
): VacuumEntities {
    val byId = allEntities.associateBy { it.entity_id }
    val deviceEntities = config?.vacuumDeviceId?.let { deviceId ->
        registry.asSequence().filter { it.device_id == deviceId }.mapNotNull { byId[it.entity_id] }.toList()
    }.orEmpty()
    fun text(e: HAEntity) = "${e.entity_id} ${e.friendlyName.orEmpty()}".lowercase()
    fun manual(id: String?) = id?.let(byId::get)
    val map = deviceEntities.firstOrNull {
        it.entity_id.startsWith("camera.") && listOf("map", "kaart", "floor").any(text(it)::contains)
    } ?: deviceEntities.firstOrNull { it.entity_id.startsWith("camera.") } ?: manual(config?.vacuumMapEntityId)
    val battery = deviceEntities.firstOrNull {
        it.deviceClass == "battery" || "battery" in text(it) || "accu" in text(it)
    } ?: manual(config?.vacuumBatteryEntityId)
    val water = deviceEntities.firstOrNull {
        (it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.")) &&
            listOf("water", "mop", "dweil").any(text(it)::contains)
    } ?: manual(config?.vacuumWaterEntityId)
    val empty = deviceEntities.firstOrNull {
        (it.entity_id.startsWith("button.") || it.entity_id.startsWith("switch.")) &&
            listOf("empty", "dust bin", "dustbin", "stofbak", "auto empty").any(text(it)::contains)
    } ?: manual(config?.vacuumEmptyBinEntityId)
    return VacuumEntities(map, battery, water, empty)
}

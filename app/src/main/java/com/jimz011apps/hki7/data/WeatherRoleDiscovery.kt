@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.data

data class WeatherRoleDiscovery(
    val moonEntityId: String? = null,
    val seasonEntityId: String? = null,
    val aqiEntityId: String? = null,
    val seasonIsExplicitlyAstronomical: Boolean = false
)

/** HA does not expose Season's private config-entry `type` over its websocket API. User-facing
 * names/titles are therefore the only definitive public signal when both helper types exist. */
fun isExplicitAstronomicalSeason(
    entityId: String?,
    entitiesById: Map<String, HAEntity>,
    registryByEntity: Map<String, HAEntityRegistryEntry>,
    configEntriesById: Map<String, HAConfigEntry>
): Boolean {
    entityId ?: return false
    val entry = registryByEntity[entityId]
    val searchable = listOfNotNull(
        entityId,
        entitiesById[entityId]?.friendlyName,
        entry?.config_entry_id?.let(configEntriesById::get)?.title
    ).joinToString(" ").lowercase()
    return "astronomical" in searchable
}

/** Resolve weather extras by integration-owned registry metadata, not editable entity names. */
fun discoverWeatherRoleEntities(
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
    seasonConfigEntries: List<HAConfigEntry> = emptyList()
): WeatherRoleDiscovery {
    val entitiesById = entities.associateBy(HAEntity::entity_id)
    val liveEntries = registry.filter { entry ->
        entry.disabled_by == null && entry.entity_id in entitiesById
    }

    val moon = liveEntries.asSequence()
        .filter { it.platform == "moon" }
        .sortedByDescending { it.translation_key == "phase" }
        .map(HAEntityRegistryEntry::entity_id)
        .firstOrNull()

    val registryByEntity = liveEntries.associateBy(HAEntityRegistryEntry::entity_id)
    val seasonEntriesById = seasonConfigEntries.associateBy(HAConfigEntry::entry_id)
    val seasonOrder = seasonConfigEntries.mapIndexed { index, entry -> entry.entry_id to index }.toMap()
    val season = liveEntries.asSequence()
        .filter { it.platform == "season" }
        .sortedWith(compareByDescending<HAEntityRegistryEntry> { entry ->
            val searchable = listOfNotNull(
                entry.entity_id,
                entitiesById[entry.entity_id]?.friendlyName,
                entry.config_entry_id?.let(seasonEntriesById::get)?.title
            ).joinToString(" ").lowercase()
            when {
                "astronomical" in searchable -> 3
                entry.entity_id == "sensor.season" -> 2
                "meteorological" in searchable -> 0
                else -> 1
            }
        }.thenBy { seasonOrder[it.config_entry_id] ?: Int.MAX_VALUE }.thenBy { it.entity_id })
        .map(HAEntityRegistryEntry::entity_id)
        .firstOrNull()

    val aqi = liveEntries.asSequence()
        .filter { it.platform == "airvisual" }
        .filter { entry -> entitiesById[entry.entity_id]?.deviceClass == "aqi" }
        .sortedWith(
            compareByDescending<HAEntityRegistryEntry> {
                it.unique_id?.contains("_us_air_quality_index", ignoreCase = true) == true
            }.thenByDescending {
                val entity = entitiesById[it.entity_id]
                it.entity_id.contains("us_", ignoreCase = true) ||
                    entity?.friendlyName?.contains("U.S.", ignoreCase = true) == true
            }.thenBy { it.entity_id }
        )
        .map(HAEntityRegistryEntry::entity_id)
        .firstOrNull()

    return WeatherRoleDiscovery(
        moonEntityId = moon,
        seasonEntityId = season,
        aqiEntityId = aqi,
        seasonIsExplicitlyAstronomical = isExplicitAstronomicalSeason(
            season,
            entitiesById,
            registryByEntity,
            seasonEntriesById
        )
    )
}

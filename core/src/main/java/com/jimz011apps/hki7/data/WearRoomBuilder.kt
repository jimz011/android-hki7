package com.jimz011apps.hki7.data

/** Keeps a room from becoming a scroll: the long tail belongs on the phone. */
private const val MAX_ENTITIES_PER_ROOM = 20

/**
 * Builds the room list to hand to a paired watch, from **the dashboard the user actually has**.
 *
 * The earlier version derived rooms from the entity registry and a domain allow-list. That was
 * wrong in both directions at once: it showed entities the dashboard deliberately hides — the
 * espresense switches, camera device siblings, child locks and appliance siblings that
 * `autoPopulateDashboard` filters out, plus anything the user hid by hand — and it missed
 * thermostats entirely, because climate entities are not button-stack entries at all; they live in
 * [HKIAreaConfig.climateEntityIds] and surface as room badges.
 *
 * Reading the dashboard instead of re-deriving it means every one of those rules is inherited for
 * free, and stays inherited when they change. A second implementation of "what belongs in a room"
 * would drift from the first the moment either moved.
 */
fun buildWearRooms(
    areas: List<HAArea>,
    /** The dashboard's per-area widgets, as the phone renders them. */
    areaWidgets: Map<String, List<HKIRoomWidget>>,
    /** Per-area configuration, which is where climate and other badge entities live. */
    areaConfigs: Map<String, HKIAreaConfig>,
    /** Entity ids that currently exist; a dashboard can outlive the integration behind it. */
    knownEntityIds: Set<String>,
    /** Hidden by household policy, or otherwise not this user's to see. */
    isAllowed: (String) -> Boolean = { true },
): List<WearRoom> {
    if (areas.isEmpty()) return emptyList()

    return areas
        .mapNotNull { area ->
            val fromWidgets = areaWidgets[area.area_id].orEmpty().flatMap(::entityIdsOf)
            val fromConfig = badgeEntityIdsOf(areaConfigs[area.area_id])

            val entities = (fromWidgets + fromConfig)
                .distinct()
                .filter { it.isNotBlank() && it in knownEntityIds && isAllowed(it) }
                // Only what a tap can do something with. A room's temperature and humidity sensors
                // are worth reading on the phone, where they sit under a heading that explains
                // them; on a watch list they would be indistinguishable from the controls.
                .filter { it.substringBefore('.') in TAPPABLE_DOMAINS }
                .take(MAX_ENTITIES_PER_ROOM)

            if (entities.isEmpty()) {
                null
            } else {
                WearRoom(
                    id = area.area_id,
                    name = area.name,
                    icon = area.icon,
                    entityIds = entities,
                )
            }
        }
        .sortedBy { it.name.lowercase() }
}

/**
 * Domains a single tap has a meaning for.
 *
 * The dashboard shows sensors and cameras in a room too, but a watch row is a name, a state and a
 * tap. A camera needs a picture and a sensor needs no interaction at all, so both would be dead
 * rows taking space from the things that do something.
 */
private val TAPPABLE_DOMAINS = setOf(
    "light", "switch", "fan", "cover", "lock", "climate", "media_player",
    "input_boolean", "scene", "script", "vacuum", "valve", "siren", "humidifier",
)

/** Entity ids a widget puts on screen, following nested widgets and honouring hidden flags. */
private fun entityIdsOf(widget: HKIRoomWidget): List<String> {
    // A hidden widget is hidden everywhere. Time and condition rules are deliberately not applied:
    // they are evaluated per-render on the phone, and a snapshot pushed to the watch would freeze
    // whatever happened to be true at push time — worse than showing the row and letting the tap
    // decide.
    if (widget.isHidden) return emptyList()

    return when (widget) {
        is HKIButtonStack -> widget.entityIds.filterNot { entityId ->
            widget.buttonConfigs[entityId]?.hidden == true
        }
        is HKISwipingStack -> widget.widgets.flatMap(::entityIdsOf)
        is HKISingleEntityWidget -> listOf(widget.entityId)
        is HKIMediaPlayerWidget -> listOf(widget.entityId)
        // Climate cards reference the Climate screen by card key rather than naming an entity, so
        // there is nothing to read here — the room's thermostats come from its area config.
        else -> emptyList()
    }
}

/** Entities a room shows as badges rather than buttons — thermostats, locks, covers. */
private fun badgeEntityIdsOf(config: HKIAreaConfig?): List<String> {
    if (config == null) return emptyList()
    return buildList {
        addAll(config.climateEntityIds)
        config.climateEntityId?.let(::add)
        addAll(config.lockEntityIds)
        addAll(config.blindEntityIds)
        addAll(config.mediaPlayerEntityIds)
        config.mediaPlayerEntityId?.let(::add)
    }
}

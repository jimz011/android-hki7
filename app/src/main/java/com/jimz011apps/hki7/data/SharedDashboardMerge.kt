package com.jimz011apps.hki7.data

/**
 * Merges an incoming shared-dashboard update onto the recipient's local copy.
 *
 * Structural changes from the owner — which rooms, widgets, and buttons exist, their entities and
 * actions — are applied, while the recipient's own aesthetic tweaks (icons, names, layout shape,
 * wallpaper) are preserved. Matching is by stable id (area id, widget id, entity id).
 *
 * Global appearance (theme, colors, fonts, corner radius) is a per-user preference that never travels
 * inside a dashboard, so it is untouched by sharing regardless of this merge.
 */
fun mergeSharedDashboardAesthetics(local: HKIDashboard, incoming: HKIDashboard): HKIDashboard {
    val mergedAreaConfigs = incoming.areaConfigs.mapValues { (areaId, cfg) ->
        val lc = local.areaConfigs[areaId] ?: return@mapValues cfg
        // Room icon and wallpaper are aesthetic; everything else (entity bindings, badges) is the
        // owner's structure.
        cfg.copy(icon = lc.icon, wallpaper = lc.wallpaper)
    }
    val mergedAreaWidgets = incoming.areaWidgets.mapValues { (areaId, widgets) ->
        val localById = local.areaWidgets[areaId].orEmpty().associateBy { it.id }
        widgets.map { w -> localById[w.id]?.let { mergeWidgetAesthetics(w, it) } ?: w }
    }
    val mergedPageConfigs = incoming.pageConfigs.mapValues { (key, pc) ->
        val lp = local.pageConfigs[key] ?: return@mapValues pc
        pc.copy(wallpaper = lp.wallpaper, headerColor = lp.headerColor)
    }
    return incoming.copy(
        id = local.id,        // keep the local ("shared-<id>") id
        name = local.name,    // keep the recipient's chosen name
        areaConfigs = mergedAreaConfigs,
        areaWidgets = mergedAreaWidgets,
        pageConfigs = mergedPageConfigs,
    )
}

private fun mergeWidgetAesthetics(incoming: HKIRoomWidget, local: HKIRoomWidget): HKIRoomWidget {
    if (incoming::class != local::class) return incoming
    return when (incoming) {
        is HKIButtonStack -> {
            local as HKIButtonStack
            incoming.copy(
                title = local.title, icon = local.icon, width = local.width,
                columns = local.columns, isSquare = local.isSquare, cornerRadius = local.cornerRadius,
                buttonStyle = local.buttonStyle, showBadge = local.showBadge, showName = local.showName,
                isHidden = local.isHidden, isCollapsed = local.isCollapsed, defaultCollapsed = local.defaultCollapsed,
                buttonConfigs = mergeButtonConfigs(incoming.entityIds, incoming.buttonConfigs, local.buttonConfigs),
            )
        }
        is HKISingleEntityWidget -> {
            local as HKISingleEntityWidget
            incoming.copy(
                width = local.width, isSquare = local.isSquare, cornerRadius = local.cornerRadius,
                buttonStyle = local.buttonStyle,
                config = mergeButtonConfig(incoming.config, local.config),
            )
        }
        is HKIEmptyStack -> {
            local as HKIEmptyStack
            incoming.copy(
                title = local.title, icon = local.icon, width = local.width, columns = local.columns,
                isSquare = local.isSquare, cornerRadius = local.cornerRadius, showBadge = local.showBadge,
                isHidden = local.isHidden, isCollapsed = local.isCollapsed, defaultCollapsed = local.defaultCollapsed,
                widgets = mergeChildWidgets(incoming.widgets, local.widgets),
            )
        }
        is HKISwipingStack -> {
            local as HKISwipingStack
            incoming.copy(
                title = local.title, icon = local.icon, width = local.width,
                isSquare = local.isSquare, cornerRadius = local.cornerRadius,
                isHidden = local.isHidden, isCollapsed = local.isCollapsed, defaultCollapsed = local.defaultCollapsed,
                widgets = mergeChildWidgets(incoming.widgets, local.widgets),
            )
        }
        else -> incoming
    }
}

private fun mergeChildWidgets(incoming: List<HKIRoomWidget>, local: List<HKIRoomWidget>): List<HKIRoomWidget> {
    val byId = local.associateBy { it.id }
    return incoming.map { w -> byId[w.id]?.let { mergeWidgetAesthetics(w, it) } ?: w }
}

/** Keeps a config for every entity the owner still exposes: structural fields (actions, vacuum/lock
 * bindings, …) come from the owner; the recipient's visual fields (name, icon, animation, secondary
 * value) win. Configs for entities the owner removed are dropped. */
private fun mergeButtonConfigs(
    entityIds: List<String>,
    incoming: Map<String, HKIButtonConfig>,
    local: Map<String, HKIButtonConfig>,
): Map<String, HKIButtonConfig> {
    val keys = LinkedHashSet<String>().apply {
        addAll(incoming.keys)
        addAll(entityIds)
    }
    val out = LinkedHashMap<String, HKIButtonConfig>()
    for (key in keys) {
        val inc = incoming[key]
        val loc = local[key]
        val merged = when {
            inc != null && loc != null -> mergeButtonConfig(inc, loc)
            inc != null -> inc
            loc != null -> loc
            else -> null
        }
        if (merged != null) out[key] = merged
    }
    return out
}

private fun mergeButtonConfig(incoming: HKIButtonConfig, local: HKIButtonConfig): HKIButtonConfig =
    incoming.copy(
        name = local.name,
        icon = local.icon,
        iconAnimation = local.iconAnimation,
        stateAttribute = local.stateAttribute,
        stateUnit = local.stateUnit,
        stateAsTimer = local.stateAsTimer,
    )

package com.jimz011apps.hki7.data

/**
 * Applies owner-controlled structure and behavior while retaining the subscriber's local styling.
 * Matching uses stable area, widget, badge, graph, and entity ids.
 */
fun mergeSharedDashboardAesthetics(local: HKIDashboard, incoming: HKIDashboard): HKIDashboard {
    val mergedAreaConfigs = incoming.areaConfigs.mapValues { (areaId, config) ->
        local.areaConfigs[areaId]?.let { mergeAreaConfigAesthetics(config, it) } ?: config
    }
    val mergedAreaWidgets = incoming.areaWidgets.mapValues { (areaId, widgets) ->
        val localById = local.areaWidgets[areaId].orEmpty().associateBy { it.id }
        widgets.map { widget -> localById[widget.id]?.let { mergeWidgetAesthetics(widget, it) } ?: widget }
    }
    val mergedPageConfigs = incoming.pageConfigs.mapValues { (key, config) ->
        local.pageConfigs[key]?.let { mergePageConfigAesthetics(config, it) } ?: config
    }
    val localCustomPages = local.customPages.associateBy { it.id }
    val localCustomPopups = local.customPopups.associateBy { it.id }
    return incoming.copy(
        id = local.id,
        name = local.name,
        areaConfigs = mergedAreaConfigs,
        areaWidgets = mergedAreaWidgets,
        pageConfigs = mergedPageConfigs,
        customPages = incoming.customPages.map { page -> localCustomPages[page.id] ?: page },
        // Like custom pages, the owner decides which popups exist while a local rename/icon sticks.
        customPopups = incoming.customPopups.map { popup -> localCustomPopups[popup.id] ?: popup },
        navBarOrder = local.navBarOrder,
        navBarHidden = local.navBarHidden,
        mediaPlayerNames = incoming.mediaPlayerNames + local.mediaPlayerNames,
    )
}

internal fun mergeAreaConfigAesthetics(incoming: HKIAreaConfig, local: HKIAreaConfig): HKIAreaConfig =
    incoming.copy(
        name = local.name,
        icon = local.icon,
        wallpaper = local.wallpaper,
        headerColor = local.headerColor,
        lockIcon = local.lockIcon,
        climateIcon = local.climateIcon,
        cameraIcon = local.cameraIcon,
        blindIcon = local.blindIcon,
    )

internal fun mergePageConfigAesthetics(incoming: HKIPageConfig, local: HKIPageConfig): HKIPageConfig =
    incoming.copy(
        wallpaper = local.wallpaper,
        headerColor = local.headerColor,
        energyConfig = mergeEnergyConfigAesthetics(incoming.energyConfig, local.energyConfig),
        climateConfig = mergeClimateConfigAesthetics(incoming.climateConfig, local.climateConfig),
        securityConfig = mergeSecurityConfigAesthetics(incoming.securityConfig, local.securityConfig),
        batteryConfig = mergeBatteryConfigAesthetics(incoming.batteryConfig, local.batteryConfig),
    )

private fun mergeEnergyConfigAesthetics(incoming: HKIEnergyConfig?, local: HKIEnergyConfig?): HKIEnergyConfig? =
    if (incoming == null || local == null) incoming else incoming.copy(
        cardOrder = local.cardOrder,
        customNames = local.customNames,
    )

private fun mergeClimateConfigAesthetics(incoming: HKIClimateConfig?, local: HKIClimateConfig?): HKIClimateConfig? =
    if (incoming == null || local == null) incoming else incoming.copy(
        entityOrder = local.entityOrder,
        customNames = local.customNames,
        customIcons = local.customIcons,
        defaultDeviceCardStyle = local.defaultDeviceCardStyle,
        defaultDeviceCardWidth = local.defaultDeviceCardWidth,
        deviceCardStyles = local.deviceCardStyles,
        deviceCardWidths = local.deviceCardWidths,
        deviceCardShapes = local.deviceCardShapes,
    )

private fun mergeSecurityConfigAesthetics(incoming: HKISecurityConfig?, local: HKISecurityConfig?): HKISecurityConfig? {
    if (incoming == null || local == null) return incoming
    return incoming.copy(
        entityOrder = local.entityOrder,
        customNames = local.customNames,
        customIcons = local.customIcons,
        cameraConfigs = incoming.cameraConfigs.mapValues { (id, config) ->
            local.cameraConfigs[id]?.let { mergeButtonConfig(config, it) } ?: config
        },
    )
}

private fun mergeBatteryConfigAesthetics(incoming: HKIBatteryConfig?, local: HKIBatteryConfig?): HKIBatteryConfig? =
    if (incoming == null || local == null) incoming else incoming.copy(
        entityOrder = local.entityOrder,
        customNames = local.customNames,
    )

internal fun mergeWidgetAesthetics(incoming: HKIRoomWidget, local: HKIRoomWidget): HKIRoomWidget {
    if (incoming::class != local::class) return incoming
    return when (incoming) {
        is HKIButtonStack -> {
            local as HKIButtonStack
            incoming.copy(
                title = local.title,
                icon = local.icon,
                width = local.width,
                columns = local.columns,
                showBadge = local.showBadge,
                showName = local.showName,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                buttonStyle = local.buttonStyle,
                adaptiveLightingLayout = local.adaptiveLightingLayout,
                buttonConfigs = mergeButtonConfigs(incoming.entityIds, incoming.buttonConfigs, local.buttonConfigs),
            )
        }
        is HKISingleEntityWidget -> {
            local as HKISingleEntityWidget
            incoming.copy(
                width = local.width,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                buttonStyle = local.buttonStyle,
                config = mergeButtonConfig(incoming.config, local.config),
            )
        }
        is HKIEmptyStack -> {
            local as HKIEmptyStack
            incoming.copy(
                title = local.title,
                icon = local.icon,
                width = local.width,
                columns = local.columns,
                showBadge = local.showBadge,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                widgets = mergeChildWidgets(incoming.widgets, local.widgets),
            )
        }
        is HKISwipingStack -> {
            local as HKISwipingStack
            incoming.copy(
                title = local.title,
                icon = local.icon,
                width = local.width,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                animationDurationMs = local.animationDurationMs,
                animation = local.animation,
                widgets = mergeChildWidgets(incoming.widgets, local.widgets),
            )
        }
        is HKISubtitleWidget -> incoming.copy(width = (local as HKISubtitleWidget).width, icon = local.icon)
        is HKIEnergyCardWidget -> {
            local as HKIEnergyCardWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKIEnergyStack -> {
            local as HKIEnergyStack
            incoming.copy(width = local.width, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius)
        }
        is HKIClimateCardWidget -> {
            local as HKIClimateCardWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius, isSquare = local.isSquare, backgroundUrl = local.backgroundUrl)
        }
        is HKIClimateStack -> {
            local as HKIClimateStack
            incoming.copy(width = local.width, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius)
        }
        is HKIMediaPlayerWidget -> {
            local as HKIMediaPlayerWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, isSquare = local.isSquare, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKISensorGraphWidget -> {
            local as HKISensorGraphWidget
            incoming.copy(
                width = local.width,
                title = local.title,
                icon = local.icon,
                style = local.style,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                backgroundUrl = local.backgroundUrl,
                itemConfigs = mergeButtonConfigs(incoming.entityIds, incoming.itemConfigs, local.itemConfigs),
            )
        }
        is HKISensorGraphStack -> {
            local as HKISensorGraphStack
            val localGraphs = local.graphs.associateBy { it.id }
            incoming.copy(
                width = local.width,
                title = local.title,
                icon = local.icon,
                cornerRadius = local.cornerRadius,
                graphs = incoming.graphs.map { graph ->
                    localGraphs[graph.id]?.let { mergeWidgetAesthetics(graph, it) as HKISensorGraphWidget } ?: graph
                },
            )
        }
        is HKIMarkdownWidget -> {
            local as HKIMarkdownWidget
            incoming.copy(width = local.width, isSquare = local.isSquare, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKIIframeWidget -> {
            local as HKIIframeWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, aspectRatio = local.aspectRatio, cornerRadius = local.cornerRadius)
        }
        is HKIWeatherWidget -> {
            local as HKIWeatherWidget
            incoming.copy(width = local.width, style = local.style, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKICalendarWidget -> {
            local as HKICalendarWidget
            incoming.copy(width = local.width, view = local.view, isSquare = local.isSquare, title = local.title, icon = local.icon, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKIWasteCollectionWidget -> {
            local as HKIWasteCollectionWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, imageStyle = local.imageStyle, isSquare = local.isSquare, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKIBatteryCardWidget -> {
            local as HKIBatteryCardWidget
            incoming.copy(width = local.width, title = local.title, icon = local.icon, isSquare = local.isSquare, cornerRadius = local.cornerRadius, backgroundUrl = local.backgroundUrl)
        }
        is HKIParcelsWidget -> {
            local as HKIParcelsWidget
            incoming.copy(
                width = local.width,
                carrierImageUrls = local.carrierImageUrls,
                carrierNames = local.carrierNames,
                title = local.title,
                icon = local.icon,
                isSquare = local.isSquare,
                cornerRadius = local.cornerRadius,
                backgroundUrl = local.backgroundUrl,
            )
        }
    }
}

private fun mergeChildWidgets(incoming: List<HKIRoomWidget>, local: List<HKIRoomWidget>): List<HKIRoomWidget> {
    val byId = local.associateBy { it.id }
    return incoming.map { widget -> byId[widget.id]?.let { mergeWidgetAesthetics(widget, it) } ?: widget }
}

private fun mergeButtonConfigs(
    entityIds: List<String>,
    incoming: Map<String, HKIButtonConfig>,
    local: Map<String, HKIButtonConfig>,
): Map<String, HKIButtonConfig> {
    val keys = LinkedHashSet<String>().apply {
        addAll(incoming.keys)
        addAll(entityIds)
    }
    return buildMap {
        for (key in keys) {
            val incomingConfig = incoming[key]
            val localConfig = local[key]
            when {
                incomingConfig != null && localConfig != null -> put(key, mergeButtonConfig(incomingConfig, localConfig))
                incomingConfig != null -> put(key, incomingConfig)
                localConfig != null -> put(key, localConfig)
            }
        }
    }
}

private fun mergeButtonConfig(incoming: HKIButtonConfig, local: HKIButtonConfig): HKIButtonConfig =
    incoming.copy(
        name = local.name,
        icon = local.icon,
        iconAnimation = local.iconAnimation,
    )

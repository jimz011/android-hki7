@file:Suppress("KotlinConstantConditions", "UnusedBoxWithConstraintsScope")

package com.example.hki7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.data.HKIBatteryCardWidget
import com.example.hki7.data.HKICalendarWidget
import com.example.hki7.data.HKIWasteCollectionWidget
import com.example.hki7.data.HKIParcelsWidget
import com.example.hki7.data.HKIEmptyStack
import com.example.hki7.data.HKIRoomWidget
import com.example.hki7.data.HKIEnergyCardWidget
import com.example.hki7.data.HKIEnergyStack
import com.example.hki7.data.HKISingleEntityWidget
import com.example.hki7.data.HKISwipingStack
import com.example.hki7.data.HKISubtitleWidget
import com.example.hki7.data.HKIWeatherWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.DevicePickerDialog
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.HKICameraDialog
import com.example.hki7.ui.components.HKILightDialog
import com.example.hki7.ui.components.HKIFanDialog
import com.example.hki7.ui.components.HKIHumidifierDialog
import com.example.hki7.ui.components.HKIAlarmDialog
import com.example.hki7.ui.components.buildWebRtcApiUrl
import com.example.hki7.ui.components.buildCameraRefreshModel
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.ReorderAxis
import com.example.hki7.ui.components.PersonDetailDialog
import com.example.hki7.ui.components.resolveEntityCameraUrl
import com.example.hki7.ui.components.resolveCameraUrl
import androidx.navigation.NavController
import java.util.UUID

private const val HOME_WIDGET_AREA = "__home__"

@Composable
fun HAHomeScreen(viewModel: MainViewModel, navController: NavController) {
    var selectedPerson by remember { mutableStateOf<HAEntity?>(null) }
    val widgets by viewModel.areaWidgetsMapping.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val homeWidgets = widgets[HOME_WIDGET_AREA].orEmpty()
    val widgetGridState = rememberLazyGridState()
    var showAddWidget by remember { mutableStateOf(false) }
    var addingToStackId by remember { mutableStateOf<String?>(null) }
    var cameraAddMode by remember { mutableStateOf<String?>(null) }
    var customCameraUrl by remember { mutableStateOf("") }
    var editingStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    var editingSwipingStack by remember { mutableStateOf<HKISwipingStack?>(null) }
    var editingEmptyStack by remember { mutableStateOf<HKIEmptyStack?>(null) }
    var editingSubtitle by remember { mutableStateOf<HKISubtitleWidget?>(null) }
    var editingWeather by remember { mutableStateOf<HKIWeatherWidget?>(null) }
    var addingToSwipingStackId by remember { mutableStateOf<String?>(null) }
    var addingToNestedStack by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingSingleWidgetKind by remember { mutableStateOf<String?>(null) }
    var pendingSingleWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingWeatherWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingWeatherWidgetEntityId by remember { mutableStateOf<String?>(null) }
    var choosingWeatherWidgetStyle by remember { mutableStateOf(false) }
    var selectedButtonSettings by remember { mutableStateOf<Pair<HKIButtonStack, String>?>(null) }
    var selectedSingleWidgetSettings by remember { mutableStateOf<Pair<String?, HKISingleEntityWidget>?>(null) }
    var editingEnergyCard by remember { mutableStateOf<Pair<String?, HKIEnergyCardWidget>?>(null) }
    var editingEnergyStack by remember { mutableStateOf<Pair<String?, HKIEnergyStack>?>(null) }
    var editingCalendarWidget by remember { mutableStateOf<Pair<String?, HKICalendarWidget>?>(null) }
    var editingWasteWidget by remember { mutableStateOf<Pair<String?, HKIWasteCollectionWidget>?>(null) }
    var pendingWasteWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingParcelsWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var editingBatteryWidget by remember { mutableStateOf<Pair<String?, HKIBatteryCardWidget>?>(null) }
    var editingParcelsWidget by remember { mutableStateOf<Pair<String?, HKIParcelsWidget>?>(null) }
    var pendingCalendarWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var selectedChildStackSettings by remember { mutableStateOf<Pair<String, HKIButtonStack>?>(null) }
    var orderingStack by remember { mutableStateOf<Pair<String?, HKIButtonStack>?>(null) }
    var selectedChildButtonSettings by remember { mutableStateOf<Triple<String, HKIButtonStack, String>?>(null) }
    var editingChildEmptyStack by remember { mutableStateOf<Pair<String, HKIEmptyStack>?>(null) }
    var editingChildSubtitle by remember { mutableStateOf<Pair<String, HKISubtitleWidget>?>(null) }
    var editingChildWeather by remember { mutableStateOf<Pair<String, HKIWeatherWidget>?>(null) }
    var selectedGenericEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedLightEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedCameraId by remember { mutableStateOf<String?>(null) }
    var selectedCameraStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    // Universal stack dialog state
    var selectedStackEntities by remember { mutableStateOf<List<HAEntity>>(emptyList()) }
    var selectedStackConfigs by remember { mutableStateOf<Map<String, HKIButtonConfig>>(emptyMap()) }
    var selectedStackStart by remember { mutableIntStateOf(0) }
    var selectedBadgeStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    var selectedFanEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedFanConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var selectedHumidifierEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedHumidifierConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var selectedAlarmEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedAlarmConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var selectedVacuumEntityId by remember { mutableStateOf<String?>(null) }
    var selectedMediaPlayerId by remember { mutableStateOf<String?>(null) }

    // Home has its own stack editor (separate from RoomDetailScreen). Its old conditional snapshot
    // was created before HA finished loading and was never replaced when a picker opened.
    val liveEntities by viewModel.entities.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchRegistries()
        if (viewModel.entities.value.isEmpty()) viewModel.refreshEntities(isSilent = true, includeDashboardRefresh = false)
    }
    val entities = remember(liveEntities, entityRegistry) {
        val liveById = liveEntities.associateBy { it.entity_id }
        (liveEntities + entityRegistry.asSequence()
            .filterNot { it.entity_id in liveById }
            .map { HAEntity(entity_id = it.entity_id, state = "unavailable") }
            .toList())
            .distinctBy { it.entity_id }
    }

    fun newButtonStack(title: String?, icon: String?) = HKIButtonStack(id = UUID.randomUUID().toString(), title = title, icon = icon, columns = 3, isSquare = true)
    fun newCameraStack(title: String?, icon: String?) = HKIButtonStack(id = UUID.randomUUID().toString(), title = title, icon = icon, columns = 2, isSquare = true, stackType = "camera")
    fun newVacuumStack(title: String?, icon: String?) = HKIButtonStack(id = UUID.randomUUID().toString(), title = title, icon = icon, columns = 2, isSquare = true, stackType = "vacuum")
    fun newWeatherStack(title: String?, icon: String?) = HKIButtonStack(id = UUID.randomUUID().toString(), title = title, icon = icon, columns = 1, isSquare = false, showBadge = false, cornerRadius = 24, stackType = "weather")
    fun newEmptyStack() = HKIEmptyStack(id = UUID.randomUUID().toString())
    fun newSingleEntityWidget(kind: String, entityId: String) = HKISingleEntityWidget(id = UUID.randomUUID().toString(), entityId = entityId, kind = kind, isSquare = kind != "camera")
    fun newCalendarWidget(entityIds: List<String>) = HKICalendarWidget(id = UUID.randomUUID().toString(), entityIds = entityIds, width = "full")
    fun newWasteWidget(entityIds: List<String>) = HKIWasteCollectionWidget(id = UUID.randomUUID().toString(), entityIds = entityIds, width = "full")
    fun addChildToSwipingStack(stackId: String, child: HKIRoomWidget) {
        val swipe = homeWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = homeWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(HOME_WIDGET_AREA, swipe.copy(widgets = swipe.widgets + child))
            empty != null -> viewModel.updateWidget(HOME_WIDGET_AREA, empty.copy(widgets = empty.widgets + child))
        }
    }
    fun updateChildInSwipingStack(stackId: String, child: HKIRoomWidget) {
        val swipe = homeWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = homeWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(HOME_WIDGET_AREA, swipe.copy(widgets = swipe.widgets.map { if (it.id == child.id) child else it }))
            empty != null -> viewModel.updateWidget(HOME_WIDGET_AREA, empty.copy(widgets = empty.widgets.map { if (it.id == child.id) child else it }))
        }
    }
    fun deleteChildFromSwipingStack(stackId: String, childId: String) {
        val swipe = homeWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = homeWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(HOME_WIDGET_AREA, swipe.copy(widgets = swipe.widgets.filterNot { it.id == childId }))
            empty != null -> viewModel.updateWidget(HOME_WIDGET_AREA, empty.copy(widgets = empty.widgets.filterNot { it.id == childId }))
        }
    }

    fun openStackDialog(stack: HKIButtonStack, entityId: String) {
        // Stacks no longer aggregate: open just the tapped entity (aggregation lives in the badge bar).
        val tapped = entities.find { it.entity_id == entityId } ?: return
        selectedStackEntities = listOf(tapped)
        selectedStackConfigs = stack.buttonConfigs
        selectedStackStart = 0
    }

    fun openEntityDialog(entityId: String, stack: HKIButtonStack? = null) {
        if (entityId.startsWith("fan.")) {
            selectedFanEntity = entities.find { it.entity_id == entityId }
            selectedFanConfig = stack?.buttonConfigs?.get(entityId)
            return
        }
        if (entityId.startsWith("humidifier.")) {
            selectedHumidifierEntity = entities.find { it.entity_id == entityId }
            selectedHumidifierConfig = stack?.buttonConfigs?.get(entityId)
            return
        }
        if (entityId.startsWith("alarm_control_panel.")) {
            selectedAlarmEntity = entities.find { it.entity_id == entityId }
            selectedAlarmConfig = stack?.buttonConfigs?.get(entityId)
            return
        }
        if (entityId.startsWith("person.")) {
            selectedPerson = entities.find { it.entity_id == entityId }
            return
        }
        if (entityId.startsWith("vacuum.")) {
            selectedVacuumEntityId = entityId
            return
        }
        if (entityId.startsWith("media_player.")) {
            selectedMediaPlayerId = entityId
            return
        }
        if (stack != null && stack.stackType != "camera") {
            openStackDialog(stack, entityId)
            return
        }
        entities.find { it.entity_id == entityId }?.let { entity ->
            when {
                entity.entity_id.startsWith("camera.") -> { selectedCameraId = entityId; selectedCameraStack = null }
                else -> selectedGenericEntity = entity
            }
        }
    }

    @Composable
    fun RenderSwipingChild(
        parent: HKISwipingStack,
        child: HKIRoomWidget,
        modifier: Modifier = Modifier,
        styleOverride: WidgetStyleOverride? = null
    ) {
        Box(modifier) {
        when (child) {
            is HKIButtonStack -> ButtonStackItem(
                stack = styleOverride?.let { child.copy(isSquare = it.isSquare, cornerRadius = it.cornerRadius, width = "full") } ?: child,
                viewModel = viewModel,
                currentUrl = currentUrl,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(HOME_WIDGET_AREA, child.id, entityId, "tap")
                        if (action == "more_info") openEntityDialog(entityId, child)
                    }
                },
                onEntityDoubleClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(HOME_WIDGET_AREA, child.id, entityId, "double")
                        if (action == "more_info") openEntityDialog(entityId, child)
                    }
                },
                onEntityLongClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(HOME_WIDGET_AREA, child.id, entityId, "hold")
                        if (action == "more_info") openEntityDialog(entityId, child)
                    }
                },
                onBadgeClick = {},
                onSettingsClick = { selectedChildStackSettings = parent.id to child },
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDeleteClick = { deleteChildFromSwipingStack(parent.id, child.id) },
                onHideClick = { updateChildInSwipingStack(parent.id, child.copy(isHidden = !child.isHidden)) },
                onAddClick = { addingToNestedStack = parent.id to child.id },
                onManageOrder = { orderingStack = parent.id to child },
                onButtonSettings = { entityId -> selectedChildButtonSettings = Triple(parent.id, child, entityId) },
                onRemoveEntity = { entityId -> updateChildInSwipingStack(parent.id, child.copy(entityIds = child.entityIds - entityId)) },
                onCameraClick = { entityId -> selectedCameraId = entityId; selectedCameraStack = child },
                onReorderEntities = { from, to ->
                    updateChildInSwipingStack(parent.id, child.copy(entityIds = child.entityIds.toMutableList().apply { add(to, removeAt(from)) }))
                }
            )
            is HKISubtitleWidget -> SubtitleWidget(child.copy(width = "full"), isEditMode, onDelete = { deleteChildFromSwipingStack(parent.id, child.id) }, onSettings = { editingChildSubtitle = parent.id to child })
            is HKIWeatherWidget -> WeatherRoomWidget(
                widget = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingChildWeather = parent.id to child }
            )
            is HKICalendarWidget -> CalendarWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingCalendarWidget = parent.id to child }
            )
            is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                registry = entityRegistry,
                devices = deviceRegistry,
                isEditMode = isEditMode,
                onOpen = { navController.navigate(Screen.Battery.route) },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingBatteryWidget = parent.id to child }
            )
            is HKIWasteCollectionWidget -> WasteCollectionWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingWasteWidget = parent.id to child },
                onUpdate = { updateChildInSwipingStack(parent.id, it) }
            )
            is HKIParcelsWidget -> ParcelsWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel, isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingParcelsWidget = parent.id to child }
            )
            is HKISingleEntityWidget -> SingleEntityWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                currentUrl = currentUrl,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = { entityId -> if (child.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                onEntityDoubleClick = { entityId -> if (child.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                onEntityLongClick = { entityId -> if (child.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                onDeleteClick = { deleteChildFromSwipingStack(parent.id, child.id) },
                onHideClick = { updateChildInSwipingStack(parent.id, child.copy(isHidden = !child.isHidden)) },
                onSettingsClick = { selectedSingleWidgetSettings = parent.id to child }
            )
            is HKISwipingStack -> EmptyStackHint()
            is HKIEnergyCardWidget -> EnergyCardWidgetItem(
                widget = child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingEnergyCard = parent.id to child }
            )
            is HKIEnergyStack -> EnergyStackWidgetItem(
                stack = child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingEnergyStack = parent.id to child }
            )
            is HKIEmptyStack -> EmptyStackItem(
                stack = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                isEditMode = isEditMode,
                onSettingsClick = { editingChildEmptyStack = parent.id to child },
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDeleteClick = { deleteChildFromSwipingStack(parent.id, child.id) },
                onHideClick = { updateChildInSwipingStack(parent.id, child.copy(isHidden = !child.isHidden)) },
                onAddClick = { addingToSwipingStackId = child.id },
                content = { grandChild, childModifier ->
                    RenderSwipingChild(
                        parent,
                        grandChild,
                        childModifier,
                        null
                    )
                }
            )
        }
        }
    }

    HKIPage(
        viewModel = viewModel,
        areaId = null,
        title = null,
        showPeople = true,
        onPeopleClick = { person -> selectedPerson = person },
        pageKey = "home",
        pageSettingsTitle = "Home Settings"
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Number of full-width columns. Scales up on larger screens (fold/tablet)
            // so widgets tile into several columns instead of one wide stack.
            val widgetColumnCount = when {
                maxWidth >= 900.dp -> 3
                maxWidth >= 600.dp -> 2
                else -> 1
            }
            // Six grid cells per column so widgets can span a full column (6), half (3), or third (2).
            val widgetGridColumns = widgetColumnCount * 6
            fun widgetSpan(widget: HKIRoomWidget): Int = when (widget.width) {
                "third" -> 2
                "half" -> 3
                else -> 6
            }
            Column(modifier = Modifier.fillMaxSize()) {
                if (homeWidgets.isEmpty() && !isEditMode) {
                    EmptyEditHint(Modifier.weight(1f))
                } else if (!isEditMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(widgetGridColumns),
                        state = widgetGridState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            count = homeWidgets.size,
                            key = { index -> homeWidgets[index].id },
                            contentType = { index -> homeWidgets[index]::class.simpleName ?: "widget" },
                            span = { index -> GridItemSpan(widgetSpan(homeWidgets[index])) }
                        ) { index ->
                            when (val widget = homeWidgets[index]) {
                                is HKIButtonStack -> ButtonStackItem(
                                stack = widget,
                                viewModel = viewModel,
                                currentUrl = currentUrl,
                                isEditMode = false,
                                onEntityClick = {
                                    if (widget.stackType == "vacuum") {
                                        openStackDialog(widget, it)
                                    } else {
                                        val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "tap")
                                        if (action == "more_info") {
                                            if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                            else openEntityDialog(it, widget)
                                        }
                                    }
                                },
                                onEntityDoubleClick = {
                                    val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "double")
                                    if (action == "more_info") {
                                        if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                        else openEntityDialog(it, widget)
                                    }
                                },
                                onEntityLongClick = {
                                    val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "hold")
                                    if (action == "more_info") {
                                        if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                        else openEntityDialog(it, widget)
                                    }
                                },
                                onBadgeClick = { selectedBadgeStack = widget },
                                onSettingsClick = {},
                                onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                onDeleteClick = {},
                                onHideClick = {},
                                onAddClick = {},
                                onButtonSettings = {},
                                onRemoveEntity = {},
                                onCameraClick = { entityId -> selectedCameraId = entityId; selectedCameraStack = widget },
                                onReorderEntities = { _, _ -> }
                                )
                                is HKISubtitleWidget -> SubtitleWidget(widget, false, onDelete = {}, onSettings = {})
                                is HKIWeatherWidget -> WeatherRoomWidget(
                                    widget = widget,
                                    viewModel = viewModel,
                                    isEditMode = false,
                                    onDelete = {},
                                    onSettings = {}
                                )
                                is HKICalendarWidget -> CalendarWidgetItem(
                                    widget = widget,
                                    viewModel = viewModel,
                                    isEditMode = false,
                                    onDelete = {},
                                    onSettings = {}
                                )
                                is HKISingleEntityWidget -> SingleEntityWidgetItem(
                                    widget = widget,
                                    viewModel = viewModel,
                                    currentUrl = currentUrl,
                                    accessToken = accessToken,
                                    isEditMode = false,
                                    onEntityClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                    onEntityDoubleClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                    onEntityLongClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                    onDeleteClick = {},
                                    onHideClick = {},
                                    onSettingsClick = {}
                                )
                                is HKISwipingStack -> SwipingStackItem(
                                    stack = widget,
                                    isEditMode = false,
                                    onSettingsClick = {},
                                    onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDeleteClick = {},
                                    onHideClick = {},
                                    onAddClick = {},
                                    content = { child -> RenderSwipingChild(widget, child) }
                                )
                                is HKIEmptyStack -> EmptyStackItem(
                                    stack = widget,
                                    isEditMode = false,
                                    onSettingsClick = {},
                                    onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDeleteClick = {},
                                    onHideClick = {},
                                    onAddClick = {},
                                    content = { child, childModifier ->
                                        RenderSwipingChild(
                                            HKISwipingStack(id = widget.id, widgets = widget.widgets),
                                            child,
                                            childModifier,
                                            null
                                        )
                                    }
                                )
                                is HKIEnergyCardWidget -> EnergyCardWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                                    widget = widget,
                                    viewModel = viewModel,
                                    registry = entityRegistry,
                                    devices = deviceRegistry,
                                    isEditMode = false,
                                    onOpen = { navController.navigate(Screen.Battery.route) },
                                    onDelete = {},
                                    onSettings = {}
                                )
                                is HKIWasteCollectionWidget -> WasteCollectionWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onDelete = {}, onSettings = {}, onUpdate = {}
                                )
                                is HKIParcelsWidget -> ParcelsWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIEnergyStack -> EnergyStackWidgetItem(
                                    stack = widget, viewModel = viewModel, isEditMode = false,
                                    onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDelete = {}, onSettings = {}
                                )
                            }
                        }
                    }
                } else {
                    ReorderableGrid(
                        items = homeWidgets,
                        canReorder = true,
                        onReorder = { from, to -> viewModel.moveWidgetInArea(HOME_WIDGET_AREA, from, to) },
                        key = { it.id },
                        columns = GridCells.Fixed(widgetGridColumns),
                        span = { widgetSpan(it) },
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 156.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        axis = ReorderAxis.Vertical,
                        state = widgetGridState,
                        modifier = Modifier.weight(1f)
                    ) { widget, _ ->
                        when (widget) {
                            is HKIButtonStack -> ButtonStackItem(
                            stack = widget,
                            viewModel = viewModel,
                            currentUrl = currentUrl,
                            isEditMode = isEditMode,
                            onEntityClick = {
                                if (widget.stackType == "vacuum") { openStackDialog(widget, it) }
                                else {
                                    val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "tap")
                                    if (action == "more_info") {
                                        if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                        else openEntityDialog(it, widget)
                                    }
                                }
                            },
                            onEntityDoubleClick = {
                                val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "double")
                                if (action == "more_info") {
                                    if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                    else openEntityDialog(it, widget)
                                }
                            },
                            onEntityLongClick = {
                                val action = viewModel.performButtonAction(HOME_WIDGET_AREA, widget.id, it, "hold")
                                if (action == "more_info") {
                                    if (widget.stackType == "camera") { selectedCameraId = it; selectedCameraStack = widget }
                                    else openEntityDialog(it, widget)
                                }
                            },
                            onBadgeClick = { selectedBadgeStack = widget },
                            onSettingsClick = { editingStack = widget },
                            onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDeleteClick = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                            onHideClick = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isHidden = !widget.isHidden)) },
                            onAddClick = { addingToStackId = widget.id },
                            onManageOrder = { orderingStack = null to widget },
                            onButtonSettings = { entityId -> selectedButtonSettings = widget to entityId },
                            onRemoveEntity = { entityId -> viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(entityIds = widget.entityIds - entityId)) },
                            onCameraClick = { entityId ->
                                selectedCameraId = entityId
                                selectedCameraStack = widget
                            },
                            onReorderEntities = { from, to ->
                                viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(entityIds = widget.entityIds.toMutableList().apply { add(to, removeAt(from)) }))
                            }
                            )
                            is HKISubtitleWidget -> SubtitleWidget(
                                widget,
                                isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingSubtitle = widget }
                            )
                            is HKIWeatherWidget -> WeatherRoomWidget(
                                widget = widget,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingWeather = widget }
                            )
                            is HKICalendarWidget -> CalendarWidgetItem(
                                widget = widget,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingCalendarWidget = null to widget }
                            )
                            is HKISingleEntityWidget -> SingleEntityWidgetItem(
                                widget = widget,
                                viewModel = viewModel,
                                currentUrl = currentUrl,
                                accessToken = accessToken,
                                isEditMode = isEditMode,
                                onEntityClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                onEntityDoubleClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                onEntityLongClick = { entityId -> if (widget.kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null) },
                                onDeleteClick = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onHideClick = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isHidden = !widget.isHidden)) },
                                onSettingsClick = { selectedSingleWidgetSettings = null to widget }
                            )
                            is HKISwipingStack -> SwipingStackItem(
                                stack = widget,
                                isEditMode = isEditMode,
                                onSettingsClick = { editingSwipingStack = widget },
                                onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                onDeleteClick = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onHideClick = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isHidden = !widget.isHidden)) },
                                onAddClick = { addingToSwipingStackId = widget.id },
                                content = { child -> RenderSwipingChild(widget, child) }
                            )
                            is HKIEmptyStack -> EmptyStackItem(
                                stack = widget,
                                isEditMode = isEditMode,
                                onSettingsClick = { editingEmptyStack = widget },
                                onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                onDeleteClick = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onHideClick = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isHidden = !widget.isHidden)) },
                                onAddClick = { addingToSwipingStackId = widget.id },
                                content = { child, childModifier ->
                                    RenderSwipingChild(
                                        HKISwipingStack(id = widget.id, widgets = widget.widgets),
                                        child,
                                        childModifier,
                                        null
                                    )
                                }
                            )
                            is HKIEnergyCardWidget -> EnergyCardWidgetItem(
                                widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingEnergyCard = null to widget }
                            )
                            is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                                widget = widget,
                                viewModel = viewModel,
                                registry = entityRegistry,
                                devices = deviceRegistry,
                                isEditMode = isEditMode,
                                onOpen = { navController.navigate(Screen.Battery.route) },
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingBatteryWidget = null to widget }
                            )
                            is HKIWasteCollectionWidget -> WasteCollectionWidgetItem(
                                widget = widget,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingWasteWidget = null to widget },
                                onUpdate = { viewModel.updateWidget(HOME_WIDGET_AREA, it) }
                            )
                            is HKIParcelsWidget -> ParcelsWidgetItem(
                                widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingParcelsWidget = null to widget }
                            )
                            is HKIEnergyStack -> EnergyStackWidgetItem(
                                stack = widget, viewModel = viewModel, isEditMode = isEditMode,
                                onToggleCollapsed = { viewModel.updateWidget(HOME_WIDGET_AREA, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingEnergyStack = null to widget }
                            )
                        }
                    }
                }
            }
            if (isEditMode) {
                Button(
                    onClick = { showAddWidget = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 87.dp)
                        .height(52.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Widget")
                }
            }
        }
    }

    // Universal stack dialog
    if (selectedStackEntities.isNotEmpty()) {
        val liveEntities = selectedStackEntities.map { e -> entities.find { it.entity_id == e.entity_id } ?: e }
        UniversalStackDialog(
            entities = liveEntities,
            startIndex = selectedStackStart,
            buttonConfigs = selectedStackConfigs,
            allEntities = entities,
            currentUrl = currentUrl,
            viewModel = viewModel,
            onDismiss = { selectedStackEntities = emptyList() }
        )
    }

    // Vacuum dialog: swipe between all vacuums in the same stack, with per-entity config.
    selectedVacuumEntityId?.let { vId ->
        val nestedWidgets = homeWidgets.filterIsInstance<HKISwipingStack>().flatMap { it.widgets } +
            homeWidgets.filterIsInstance<HKIEmptyStack>().flatMap { it.widgets }
        val stack = (homeWidgets + nestedWidgets).filterIsInstance<HKIButtonStack>().find { it.entityIds.contains(vId) }
        val single = (homeWidgets + nestedWidgets).filterIsInstance<HKISingleEntityWidget>().find { it.entityId == vId }
        val vacEntities = (stack?.entityIds ?: listOf(vId))
            .filter { it.startsWith("vacuum.") }
            .mapNotNull { id -> entities.find { it.entity_id == id } }
        if (vacEntities.isNotEmpty()) {
            VacuumStackDialog(
                entities = vacEntities,
                startIndex = vacEntities.indexOfFirst { it.entity_id == vId }.coerceAtLeast(0),
                buttonConfigs = stack?.buttonConfigs ?: single?.let { mapOf(vId to it.config) } ?: emptyMap(),
                allEntities = entities,
                currentUrl = currentUrl,
                viewModel = viewModel,
                onDismiss = { selectedVacuumEntityId = null }
            )
        } else {
            selectedVacuumEntityId = null
        }
    }

    selectedMediaPlayerId?.let { id ->
        val player = entities.find { it.entity_id == id }
        if (player != null) {
            com.example.hki7.ui.components.HKIMediaPlayerDialog(player, viewModel, currentUrl) { selectedMediaPlayerId = null }
        } else {
            selectedMediaPlayerId = null
        }
    }

    // Badge list dialog
    selectedBadgeStack?.let { stack: HKIButtonStack ->
        val live = stack.entityIds.mapNotNull { id -> entities.find { it.entity_id == id } }
        GroupEntityDialog(
            stack = stack,
            entities = live,
            viewModel = viewModel,
            onDismiss = { selectedBadgeStack = null }
        )
    }

    if (showAddWidget) {
        AddRoomWidgetDialog(
            onDismiss = { showAddWidget = false },
            onAddStack = { title, icon -> viewModel.addStackToArea(HOME_WIDGET_AREA, title, icon); showAddWidget = false },
            onAddCameraStack = { (title, icon) -> viewModel.addCameraStackToArea(HOME_WIDGET_AREA, title, icon); showAddWidget = false },
            onAddVacuumStack = { (title, icon) -> viewModel.addVacuumStackToArea(HOME_WIDGET_AREA, title, icon); showAddWidget = false },
            onAddSubtitle = { text, icon -> viewModel.addSubtitleToArea(HOME_WIDGET_AREA, text, icon); showAddWidget = false },
            onAddWeatherStack = { (title, icon) -> viewModel.addWeatherStackToArea(HOME_WIDGET_AREA, title, icon); showAddWidget = false },
            onAddSwipingStack = { viewModel.addSwipingStackToArea(HOME_WIDGET_AREA); showAddWidget = false },
            onAddEmptyStack = { viewModel.addEmptyStackToArea(HOME_WIDGET_AREA); showAddWidget = false },
            onAddButtonWidget = { pendingSingleWidgetKind = "button"; pendingSingleWidgetContainerId = null; showAddWidget = false },
            onAddCameraWidget = { pendingSingleWidgetKind = "camera"; pendingSingleWidgetContainerId = null; showAddWidget = false },
            onAddVacuumWidget = { pendingSingleWidgetKind = "vacuum"; pendingSingleWidgetContainerId = null; showAddWidget = false },
            onAddWeatherWidget = { pendingWeatherWidgetContainerId = "__top__"; showAddWidget = false },
            onAddCalendarWidget = { pendingCalendarWidgetContainerId = "__top__"; showAddWidget = false },
            onAddWasteWidget = { pendingWasteWidgetContainerId = "__top__"; showAddWidget = false },
            onAddParcelsWidget = {
                pendingParcelsWidgetContainerId = "__top__"
                showAddWidget = false
            },
            onAddEnergyCard = { keys -> keys.forEach { viewModel.addWidgetToArea(HOME_WIDGET_AREA, HKIEnergyCardWidget(id = UUID.randomUUID().toString(), cardKey = it)) } },
            onAddEnergyStack = { keys -> viewModel.addWidgetToArea(HOME_WIDGET_AREA, HKIEnergyStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddBatteryCard = { useNotes ->
                viewModel.addWidgetToArea(HOME_WIDGET_AREA, HKIBatteryCardWidget(id = UUID.randomUUID().toString(), useBatteryNotes = useNotes))
                showAddWidget = false
            },
            allEntities = entities
        )
    }

    addingToSwipingStackId?.let { stackId ->
        AddRoomWidgetDialog(
            onDismiss = { addingToSwipingStackId = null },
            onAddStack = { title, icon -> addChildToSwipingStack(stackId, newButtonStack(title, icon)); addingToSwipingStackId = null },
            onAddCameraStack = { (title, icon) -> addChildToSwipingStack(stackId, newCameraStack(title, icon)); addingToSwipingStackId = null },
            onAddVacuumStack = { (title, icon) -> addChildToSwipingStack(stackId, newVacuumStack(title, icon)); addingToSwipingStackId = null },
            onAddSubtitle = { text, icon -> addChildToSwipingStack(stackId, HKISubtitleWidget(id = UUID.randomUUID().toString(), text = text, icon = icon)); addingToSwipingStackId = null },
            onAddWeatherStack = { (title, icon) -> addChildToSwipingStack(stackId, newWeatherStack(title, icon)); addingToSwipingStackId = null },
            onAddEmptyStack = { addChildToSwipingStack(stackId, newEmptyStack()); addingToSwipingStackId = null },
            onAddButtonWidget = { pendingSingleWidgetKind = "button"; pendingSingleWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddCameraWidget = { pendingSingleWidgetKind = "camera"; pendingSingleWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddVacuumWidget = { pendingSingleWidgetKind = "vacuum"; pendingSingleWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddWeatherWidget = { pendingWeatherWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddCalendarWidget = { pendingCalendarWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddWasteWidget = { pendingWasteWidgetContainerId = stackId; addingToSwipingStackId = null },
            onAddParcelsWidget = {
                pendingParcelsWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddEnergyCard = { keys -> keys.forEach { addChildToSwipingStack(stackId, HKIEnergyCardWidget(id = UUID.randomUUID().toString(), cardKey = it)) } },
            onAddEnergyStack = { keys -> addChildToSwipingStack(stackId, HKIEnergyStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddBatteryCard = { useNotes ->
                addChildToSwipingStack(stackId, HKIBatteryCardWidget(id = UUID.randomUUID().toString(), useBatteryNotes = useNotes))
                addingToSwipingStackId = null
            },
            allEntities = entities
        )
    }

    pendingSingleWidgetKind?.let { kind ->
        val candidates = when (kind) {
            "camera" -> entities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) }
            "vacuum" -> entities.filter { it.entity_id.startsWith("vacuum.") }
            else -> entities
        }
        AdvancedEntitySearchDialog(
            allEntities = candidates,
            title = when (kind) {
                "camera" -> "Select Camera"
                "vacuum" -> "Select Vacuum"
                else -> "Select Entity"
            },
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (pendingSingleWidgetKind != null) {
                    val containerId = pendingSingleWidgetContainerId
                    pendingSingleWidgetKind = null
                    pendingSingleWidgetContainerId = null
                    if (containerId == null) showAddWidget = true else addingToSwipingStackId = containerId
                }
            },
            onEntitiesSelected = { entityIds ->
                val entityId = entityIds.firstOrNull()
                if (entityId != null) {
                    val containerId = pendingSingleWidgetContainerId
                    if (containerId == null) {
                        viewModel.addSingleEntityWidgetToArea(HOME_WIDGET_AREA, kind, entityId)
                    } else {
                        addChildToSwipingStack(containerId, newSingleEntityWidget(kind, entityId))
                    }
                }
                pendingSingleWidgetKind = null
                pendingSingleWidgetContainerId = null
            }
        )
    }

    if (pendingWeatherWidgetContainerId != null && pendingWeatherWidgetEntityId == null) {
        val weatherEntities = entities.filter { it.entity_id.startsWith("weather.") }
        AdvancedEntitySearchDialog(
            allEntities = weatherEntities.ifEmpty { entities },
            title = "Select Weather",
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (!choosingWeatherWidgetStyle) {
                    val containerId = pendingWeatherWidgetContainerId
                    pendingWeatherWidgetContainerId = null
                    pendingWeatherWidgetEntityId = null
                    if (containerId == "__top__") showAddWidget = true else if (containerId != null) addingToSwipingStackId = containerId
                }
            },
            onEntitiesSelected = { entityIds ->
                val entityId = entityIds.firstOrNull()
                if (entityId != null) {
                    pendingWeatherWidgetEntityId = entityId
                    choosingWeatherWidgetStyle = true
                }
            }
        )
    }

    if (choosingWeatherWidgetStyle && pendingWeatherWidgetContainerId != null && pendingWeatherWidgetEntityId != null) {
        AlertDialog(
            onDismissRequest = {
                choosingWeatherWidgetStyle = false
                pendingWeatherWidgetEntityId = null
            },
            title = { Text("Weather Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weatherWidgetStyles.sortedBy { it.second }.forEach { (style, label) ->
                        WidgetChoice(
                            icon = Icons.Default.WbSunny,
                            title = label,
                            subtitle = "",
                            onClick = {
                                val target = pendingWeatherWidgetContainerId
                                val entityId = pendingWeatherWidgetEntityId
                                if (target != null && entityId != null) {
                                    if (target == "__top__") {
                                        viewModel.addWeatherToArea(HOME_WIDGET_AREA, entityId, style)
                                    } else {
                                        addChildToSwipingStack(target, HKIWeatherWidget(id = UUID.randomUUID().toString(), entityId = entityId, style = style))
                                    }
                                }
                                choosingWeatherWidgetStyle = false
                                pendingWeatherWidgetContainerId = null
                                pendingWeatherWidgetEntityId = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = {
                    choosingWeatherWidgetStyle = false
                    pendingWeatherWidgetEntityId = null
                }) { Text("Back") }
            }
        )
    }

    addingToNestedStack?.let { (swipingStackId, childStackId) ->
        val parentWidgets = homeWidgets.filterIsInstance<HKISwipingStack>().find { it.id == swipingStackId }?.widgets
            ?: homeWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == swipingStackId }?.widgets
        val targetStack = parentWidgets?.filterIsInstance<HKIButtonStack>()?.find { it.id == childStackId }
        when (targetStack?.stackType) {
            "weather" -> WeatherItemDialog(
                initial = null,
                allEntities = entities,
                onDismiss = { addingToNestedStack = null },
                onSave = { config ->
                    val itemId = "weather_${System.currentTimeMillis()}"
                    updateChildInSwipingStack(
                        swipingStackId,
                        targetStack.copy(
                            entityIds = targetStack.entityIds + itemId,
                            buttonConfigs = targetStack.buttonConfigs + (itemId to config)
                        )
                    )
                    addingToNestedStack = null
                }
            )
            else -> AdvancedEntitySearchDialog(
                allEntities = when (targetStack?.stackType) {
                    "vacuum", "single_vacuum" -> entities.filter { it.entity_id.startsWith("vacuum.") }
                    "camera", "single_camera" -> entities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) }
                    else -> entities
                },
                preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
                onDismiss = { addingToNestedStack = null },
                onEntitiesSelected = { entityIds ->
                    targetStack?.let { stack ->
                        val updatedIds = if (stack.stackType in listOf("single_button", "single_camera", "single_vacuum")) entityIds.take(1) else (stack.entityIds + entityIds).distinct()
                        updateChildInSwipingStack(swipingStackId, stack.copy(entityIds = updatedIds))
                    }
                    addingToNestedStack = null
                }
            )
        }
    }

    if (addingToStackId != null && cameraAddMode == null) {
        val targetStack = homeWidgets.find { it.id == addingToStackId } as? HKIButtonStack
        when (targetStack?.stackType) {
            "vacuum" -> AdvancedEntitySearchDialog(
                allEntities = entities.filter { it.entity_id.startsWith("vacuum.") },
                title = "Select Vacuums",
                preselectedIds = targetStack.entityIds.toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { ids ->
                    viewModel.updateWidget(HOME_WIDGET_AREA, targetStack.copy(entityIds = (targetStack.entityIds + ids).distinct()))
                    addingToStackId = null
                }
            )
            "single_vacuum" -> AdvancedEntitySearchDialog(
                allEntities = entities.filter { it.entity_id.startsWith("vacuum.") },
                title = "Select Vacuum",
                preselectedIds = targetStack.entityIds.take(1).toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { ids ->
                    viewModel.updateWidget(HOME_WIDGET_AREA, targetStack.copy(entityIds = ids.take(1)))
                    addingToStackId = null
                }
            )
            "single_camera" -> AdvancedEntitySearchDialog(
                allEntities = entities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) },
                title = "Select Camera",
                preselectedIds = targetStack.entityIds.take(1).toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { ids ->
                    viewModel.updateWidget(HOME_WIDGET_AREA, targetStack.copy(entityIds = ids.take(1)))
                    addingToStackId = null
                }
            )
            "camera" -> AlertDialog(
                onDismissRequest = { addingToStackId = null },
                title = { Text("Add Camera") },
                text = { Text("Would you like to add an existing camera entity or a custom URL?") },
                confirmButton = {
                    Button(onClick = { cameraAddMode = "entity" }) { Text("Existing Camera") }
                },
                dismissButton = {
                    TextButton(onClick = { cameraAddMode = "custom" }) { Text("Custom URL") }
                }
            )
            "weather" -> WeatherItemDialog(
                initial = null,
                allEntities = entities,
                onDismiss = { addingToStackId = null },
                onSave = { config ->
                    targetStack.let { stack ->
                        val itemId = "weather_${System.currentTimeMillis()}"
                        viewModel.updateWidget(
                            HOME_WIDGET_AREA,
                            stack.copy(
                                entityIds = stack.entityIds + itemId,
                                buttonConfigs = stack.buttonConfigs + (itemId to config)
                            )
                        )
                    }
                    addingToStackId = null
                }
            )
            else -> AdvancedEntitySearchDialog(
                allEntities = entities,
                preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    targetStack?.let { stack ->
                        viewModel.updateWidget(
                            HOME_WIDGET_AREA,
                            stack.copy(entityIds = if (stack.stackType == "single_button") entityIds.take(1) else (stack.entityIds + entityIds).distinct())
                        )
                    }
                    addingToStackId = null
                }
            )
        }
    } else if (addingToStackId != null && cameraAddMode == "entity") {
        val targetStack = homeWidgets.find { it.id == addingToStackId } as? HKIButtonStack
        val cameraEntities = entities.filter {
            it.entity_id.substringBefore(".").equals("camera", ignoreCase = true)
        }
        val fallbackStackCameraEntities = targetStack
            ?.entityIds
            ?.filter { it.startsWith("camera.") }
            ?.filterNot { id -> cameraEntities.any { it.entity_id == id } }
            ?.map { HAEntity(entity_id = it, state = "unavailable") }
            .orEmpty()
        AdvancedEntitySearchDialog(
            allEntities = (cameraEntities + fallbackStackCameraEntities).distinctBy { it.entity_id },
            preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
            onDismiss = { cameraAddMode = null },
            onEntitiesSelected = { entityIds ->
                targetStack?.let { stack ->
                    viewModel.updateWidget(
                        HOME_WIDGET_AREA,
                        stack.copy(entityIds = (stack.entityIds + entityIds).distinct())
                    )
                }
                addingToStackId = null
                cameraAddMode = null
            }
        )
    } else if (addingToStackId != null && cameraAddMode == "custom") {
        val targetStack = homeWidgets.find { it.id == addingToStackId } as? HKIButtonStack
        AlertDialog(
            onDismissRequest = { cameraAddMode = null; customCameraUrl = "" },
            title = { Text("Add Custom Camera URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customCameraUrl,
                        onValueChange = { customCameraUrl = it },
                        label = { Text("Camera URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Enter HTTP URL, relative path, or WebRTC path (e.g., url: poort_hd)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (customCameraUrl.isNotBlank()) {
                        targetStack?.let { stack ->
                            val customId = "custom_camera_${System.currentTimeMillis()}"
                            viewModel.updateWidget(
                                HOME_WIDGET_AREA,
                                stack.copy(
                                    entityIds = stack.entityIds + customId,
                                    buttonConfigs = stack.buttonConfigs + (customId to HKIButtonConfig(
                                        name = "Custom Camera",
                                        cameraUrl = customCameraUrl,
                                        isCustomUrl = true,
                                        cameraRefreshInterval = 5
                                    ))
                                )
                            )
                        }
                    }
                    addingToStackId = null
                    cameraAddMode = null
                    customCameraUrl = ""
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { cameraAddMode = null; customCameraUrl = "" }) { Text("Back") }
            }
        )
    }

    editingStack?.let { stack ->
        StackSettingsDialog(
            stack = stack,
            onDismiss = { editingStack = null },
            onUpdate = {
                viewModel.updateWidget(HOME_WIDGET_AREA, it)
                editingStack = null
            }
        )
    }

    editingSwipingStack?.let { stack ->
        SwipingStackSettingsDialog(
            stack = stack,
            onDismiss = { editingSwipingStack = null },
            onUpdate = {
                viewModel.updateWidget(HOME_WIDGET_AREA, stack.withChangedChildStyle(it))
                editingSwipingStack = null
            }
        )
    }

    editingEmptyStack?.let { stack ->
        EmptyStackSettingsDialog(
            stack = stack,
            onDismiss = { editingEmptyStack = null },
            onUpdate = {
                viewModel.updateWidget(HOME_WIDGET_AREA, stack.withChangedChildStyle(it))
                editingEmptyStack = null
            }
        )
    }

    selectedChildStackSettings?.let { (containerId, stack) ->
        StackSettingsDialog(
            stack = stack,
            onDismiss = { selectedChildStackSettings = null },
            onUpdate = {
                updateChildInSwipingStack(containerId, it)
                selectedChildStackSettings = null
            }
        )
    }

    orderingStack?.let { (containerId, stack) ->
        StackOrderDialog(
            stack = stack,
            allEntities = entities,
            onDismiss = { orderingStack = null },
            onSave = { orderedIds ->
                if (containerId == null) {
                    val latest = homeWidgets.filterIsInstance<HKIButtonStack>().find { it.id == stack.id } ?: stack
                    viewModel.updateWidget(HOME_WIDGET_AREA, latest.copy(entityIds = orderedIds))
                } else {
                    updateChildInSwipingStack(containerId, stack.copy(entityIds = orderedIds))
                }
                orderingStack = null
            }
        )
    }

    editingChildEmptyStack?.let { (containerId, stack) ->
        EmptyStackSettingsDialog(
            stack = stack,
            onDismiss = { editingChildEmptyStack = null },
            onUpdate = {
                updateChildInSwipingStack(containerId, stack.withChangedChildStyle(it))
                editingChildEmptyStack = null
            }
        )
    }

    editingChildSubtitle?.let { (containerId, widget) ->
        HeaderTextSettingsDialog(
            widget = widget,
            onDismiss = { editingChildSubtitle = null },
            onSave = {
                updateChildInSwipingStack(containerId, it)
                editingChildSubtitle = null
            }
        )
    }

    editingChildWeather?.let { (containerId, widget) ->
        WeatherWidgetSettingsDialog(
            widget = widget,
            allEntities = entities,
            onDismiss = { editingChildWeather = null },
            onSave = {
                updateChildInSwipingStack(containerId, it)
                editingChildWeather = null
            }
        )
    }

    editingSubtitle?.let { widget ->
        HeaderTextSettingsDialog(
            widget = widget,
            onDismiss = { editingSubtitle = null },
            onSave = {
                viewModel.updateWidget(HOME_WIDGET_AREA, it)
                editingSubtitle = null
            }
        )
    }

    editingWeather?.let { widget ->
        WeatherWidgetSettingsDialog(
            widget = widget,
            allEntities = entities,
            onDismiss = { editingWeather = null },
            onSave = {
                viewModel.updateWidget(HOME_WIDGET_AREA, it)
                editingWeather = null
            }
        )
    }

    selectedSingleWidgetSettings?.let { (containerId, widget) ->
        ButtonConfigDialog(
            entity = entities.find { it.entity_id == widget.entityId },
            config = widget.config,
            isCameraItem = widget.kind == "camera",
            isVacuumItem = widget.kind == "vacuum" || widget.entityId.startsWith("vacuum."),
            allEntities = entities,
            entityRegistry = entityRegistry,
            deviceRegistry = deviceRegistry,
            onDismiss = { selectedSingleWidgetSettings = null },
            widgetAppearance = WidgetAppearance(widget.isSquare, widget.cornerRadius, widget.width),
            onSaveWithAppearance = { config, a ->
                if (containerId == null) {
                    val latest = homeWidgets.filterIsInstance<HKISingleEntityWidget>().find { it.id == widget.id } ?: widget
                    viewModel.updateWidget(
                        HOME_WIDGET_AREA,
                        latest.copy(config = config, isSquare = a.isSquare, cornerRadius = a.cornerRadius, width = a.width)
                    )
                } else {
                    updateChildInSwipingStack(
                        containerId,
                        widget.copy(config = config, isSquare = a.isSquare, cornerRadius = a.cornerRadius, width = a.width)
                    )
                }
                selectedSingleWidgetSettings = null
            },
            onSave = { config ->
                if (containerId == null) {
                    val latest = homeWidgets.filterIsInstance<HKISingleEntityWidget>().find { it.id == widget.id } ?: widget
                    viewModel.updateWidget(HOME_WIDGET_AREA, latest.copy(config = config))
                } else {
                    updateChildInSwipingStack(containerId, widget.copy(config = config))
                }
                selectedSingleWidgetSettings = null
            }
        )
    }

    selectedChildButtonSettings?.let { (containerId, stack, entityId) ->
        if (stack.stackType == "weather") {
            WeatherItemDialog(
                initial = stack.buttonConfigs[entityId],
                allEntities = entities,
                onDismiss = { selectedChildButtonSettings = null },
                onSave = { config ->
                    updateChildInSwipingStack(containerId, stack.copy(buttonConfigs = stack.buttonConfigs + (entityId to config)))
                    selectedChildButtonSettings = null
                }
            )
        } else {
            ButtonConfigDialog(
                entity = entities.find { it.entity_id == entityId },
                config = stack.buttonConfigs[entityId] ?: HKIButtonConfig(),
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = entities,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
                onDismiss = { selectedChildButtonSettings = null },
                onSave = { config ->
                    updateChildInSwipingStack(containerId, stack.copy(buttonConfigs = stack.buttonConfigs + (entityId to config)))
                    selectedChildButtonSettings = null
                }
            )
        }
    }

    selectedButtonSettings?.let { (stack, entityId) ->
        if (stack.stackType == "weather") {
            WeatherItemDialog(
                initial = stack.buttonConfigs[entityId],
                allEntities = entities,
                onDismiss = { selectedButtonSettings = null },
                onSave = { config ->
                    val latestStack = homeWidgets.find { it.id == stack.id } as? HKIButtonStack ?: stack
                    viewModel.updateWidget(
                        HOME_WIDGET_AREA,
                        latestStack.copy(buttonConfigs = latestStack.buttonConfigs + (entityId to config))
                    )
                    selectedButtonSettings = null
                }
            )
        } else {
            ButtonConfigDialog(
                entity = entities.find { it.entity_id == entityId },
                config = stack.buttonConfigs[entityId] ?: HKIButtonConfig(),
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = entities,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
                onDismiss = { selectedButtonSettings = null },
                onSave = { config ->
                    val latestStack = homeWidgets.find { it.id == stack.id } as? HKIButtonStack ?: stack
                    viewModel.updateWidget(
                        HOME_WIDGET_AREA,
                        latestStack.copy(buttonConfigs = latestStack.buttonConfigs + (entityId to config))
                    )
                    selectedButtonSettings = null
                }
            )
        }
    }

    selectedGenericEntity?.let { entity ->
        GenericEntityDialog(
            entity = entities.find { it.entity_id == entity.entity_id } ?: entity,
            viewModel = viewModel,
            onDismiss = { selectedGenericEntity = null }
        )
    }

    selectedLightEntity?.let { entity ->
        HKILightDialog(
            entity = entities.find { it.entity_id == entity.entity_id } ?: entity,
            viewModel = viewModel,
            onDismiss = { selectedLightEntity = null }
        )
    }

    selectedFanEntity?.let { entity ->
        HKIFanDialog(
            entity = entities.find { it.entity_id == entity.entity_id } ?: entity,
            viewModel = viewModel,
            titleOverride = selectedFanConfig?.name,
            iconName = selectedFanConfig?.icon,
            spinIcon = selectedFanConfig?.spinIcon == true,
            onDismiss = { selectedFanEntity = null; selectedFanConfig = null }
        )
    }

    selectedHumidifierEntity?.let { entity ->
        HKIHumidifierDialog(
            entity = entities.find { it.entity_id == entity.entity_id } ?: entity,
            viewModel = viewModel,
            titleOverride = selectedHumidifierConfig?.name,
            iconName = selectedHumidifierConfig?.icon,
            spinIcon = selectedHumidifierConfig?.spinIcon == true,
            onDismiss = { selectedHumidifierEntity = null; selectedHumidifierConfig = null }
        )
    }

    editingEnergyCard?.let { (containerId, w) ->
        EnergyCardWidgetSettingsDialog(w, onDismiss = { editingEnergyCard = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyCard = null
        }
    }
    editingEnergyStack?.let { (containerId, s) ->
        EnergyStackSettingsDialog(s, onDismiss = { editingEnergyStack = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyStack = null
        }
    }
    editingCalendarWidget?.let { (containerId, widget) ->
        CalendarWidgetSettingsDialog(
            widget = widget,
            allEntities = entities,
            onDismiss = { editingCalendarWidget = null },
            onSave = { updated ->
                if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
                else updateChildInSwipingStack(containerId, updated)
                editingCalendarWidget = null
            }
        )
    }
    editingBatteryWidget?.let { (containerId, widget) ->
        BatteryCardWidgetSettingsDialog(
            widget = widget,
            onDismiss = { editingBatteryWidget = null },
            onSave = { updated ->
                if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
                else updateChildInSwipingStack(containerId, updated)
                editingBatteryWidget = null
            }
        )
    }
    pendingCalendarWidgetContainerId?.let { target ->
        CalendarEntityPickerDialog(
            allEntities = entities,
            onDismiss = {
                pendingCalendarWidgetContainerId = null
                if (target == "__top__") showAddWidget = true else addingToSwipingStackId = target
            },
            onSelected = { ids ->
                val widget = newCalendarWidget(ids)
                if (target == "__top__") {
                    viewModel.addWidgetToArea(HOME_WIDGET_AREA, widget)
                } else {
                    addChildToSwipingStack(target, widget)
                }
                pendingCalendarWidgetContainerId = null
            }
        )
    }
    editingWasteWidget?.let { (containerId, widget) ->
        WasteCollectionSettingsDialog(
            widget = widget,
            allEntities = entities,
            onDismiss = { editingWasteWidget = null },
            onSave = { updated ->
                if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
                else updateChildInSwipingStack(containerId, updated)
                editingWasteWidget = null
            }
        )
    }
    editingParcelsWidget?.let { (containerId, widget) ->
        ParcelsWidgetSettingsDialog(widget, viewModel, onDismiss = { editingParcelsWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(HOME_WIDGET_AREA, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingParcelsWidget = null
        }
    }
    pendingParcelsWidgetContainerId?.let { target ->
        ParcelDevicePickerDialog(viewModel, null, onDismiss = {
            pendingParcelsWidgetContainerId = null
            if (target == "__top__") showAddWidget = true else addingToSwipingStackId = target
        }) { deviceId ->
            if (deviceId != null) {
                val widget = HKIParcelsWidget(id = UUID.randomUUID().toString(), deviceIds = listOf(deviceId))
                if (target == "__top__") viewModel.addWidgetToArea(HOME_WIDGET_AREA, widget)
                else addChildToSwipingStack(target, widget)
            }
            pendingParcelsWidgetContainerId = null
        }
    }
    pendingWasteWidgetContainerId?.let { target ->
        WasteEntityPickerDialog(
            allEntities = entities,
            onDismiss = {
                pendingWasteWidgetContainerId = null
                if (target == "__top__") showAddWidget = true else addingToSwipingStackId = target
            },
            onSelected = { ids ->
                val widget = newWasteWidget(ids)
                if (target == "__top__") {
                    viewModel.addWidgetToArea(HOME_WIDGET_AREA, widget)
                } else {
                    addChildToSwipingStack(target, widget)
                }
                pendingWasteWidgetContainerId = null
            }
        )
    }

    selectedAlarmEntity?.let { entity ->
        HKIAlarmDialog(
            entity = entities.find { it.entity_id == entity.entity_id } ?: entity,
            viewModel = viewModel,
            titleOverride = selectedAlarmConfig?.name,
            iconName = selectedAlarmConfig?.icon,
            spinIcon = selectedAlarmConfig?.spinIcon == true,
            onDismiss = { selectedAlarmEntity = null; selectedAlarmConfig = null }
        )
    }

    if (selectedCameraId != null) {
        val entity = entities.find { it.entity_id == selectedCameraId }
        val config = selectedCameraStack?.buttonConfigs?.get(selectedCameraId)
        val refreshInterval = config?.cameraRefreshInterval ?: 5
        val fallbackEntityUrl = if (selectedCameraId?.startsWith("camera.") == true) {
            val path = if (refreshInterval == 0) "camera_proxy_stream" else "camera_proxy"
            "${currentUrl.removeSuffix("/")}/api/$path/$selectedCameraId"
        } else {
            null
        }
        val streamUrl = config?.cameraUrl?.takeIf { it.isNotBlank() }
            ?: resolveEntityCameraUrl(entity, currentUrl, preferLive = refreshInterval == 0)
            ?: fallbackEntityUrl
        val label = config?.name ?: entity?.friendlyName ?: selectedCameraId ?: "Camera"
        val liveWebUrl = when {
            entity != null -> resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
            fallbackEntityUrl != null -> fallbackEntityUrl
            else -> buildWebRtcApiUrl(config?.cameraUrl, currentUrl)
        }

        HKICameraDialog(
            title = label,
            imageUrl = buildCameraRefreshModel(resolveCameraUrl(streamUrl, currentUrl), refreshInterval, 0),
            refreshIntervalSeconds = refreshInterval,
            liveWebUrl = liveWebUrl,
            authToken = accessToken,
            statusText = "Live",
            entity = entity,
            viewModel = viewModel,
            onDismiss = {
                selectedCameraId = null
                selectedCameraStack = null
            }
        )
    }

    if (selectedPerson != null) {
        PersonDetailDialog(
            person = selectedPerson!!,
            viewModel = viewModel,
            onDismiss = { selectedPerson = null }
        )
    }
}

@file:Suppress("KotlinConstantConditions")

package com.example.hki7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.data.HKISubtitleWidget
import com.example.hki7.data.HKIWeatherWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
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

private const val HOME_WIDGET_AREA = "__home__"

@Composable
fun HAHomeScreen(viewModel: MainViewModel) {
    var selectedPerson by remember { mutableStateOf<HAEntity?>(null) }
    val widgets by viewModel.areaWidgetsMapping.collectAsState()
    val entities by viewModel.entities.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val homeWidgets = widgets[HOME_WIDGET_AREA].orEmpty()
    val widgetListState = rememberLazyListState()
    val widgetGridState = rememberLazyGridState()
    // Keep list and grid scroll positions in sync when toggling edit mode.
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            widgetGridState.scrollToItem(
                widgetListState.firstVisibleItemIndex,
                widgetListState.firstVisibleItemScrollOffset
            )
        } else {
            widgetListState.scrollToItem(
                widgetGridState.firstVisibleItemIndex,
                widgetGridState.firstVisibleItemScrollOffset
            )
        }
    }
    var showAddWidget by remember { mutableStateOf(false) }
    var addingToStackId by remember { mutableStateOf<String?>(null) }
    var cameraAddMode by remember { mutableStateOf<String?>(null) }
    var customCameraUrl by remember { mutableStateOf("") }
    var editingStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    var editingSubtitle by remember { mutableStateOf<HKISubtitleWidget?>(null) }
    var editingWeather by remember { mutableStateOf<HKIWeatherWidget?>(null) }
    var selectedButtonSettings by remember { mutableStateOf<Pair<HKIButtonStack, String>?>(null) }
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

    HKIPage(
        viewModel = viewModel,
        areaId = null,
        title = null,
        showPeople = true,
        onPeopleClick = { person -> selectedPerson = person },
        pageKey = "home",
        pageSettingsTitle = "Home Settings"
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (homeWidgets.isEmpty() && !isEditMode) {
                    EmptyEditHint(Modifier.weight(1f))
                } else if (!isEditMode) {
                    LazyColumn(
                        state = widgetListState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = homeWidgets,
                            key = { it.id },
                            contentType = { it::class.simpleName ?: "widget" }
                        ) { widget ->
                            when (widget) {
                                is HKIButtonStack -> ButtonStackItem(
                                stack = widget,
                                allEntities = entities,
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
                                    allEntities = entities,
                                    viewModel = viewModel,
                                    isEditMode = false,
                                    onDelete = {},
                                    onSettings = {}
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
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 156.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        axis = ReorderAxis.Vertical,
                        state = widgetGridState,
                        modifier = Modifier.weight(1f)
                    ) { widget, _ ->
                        when (widget) {
                            is HKIButtonStack -> ButtonStackItem(
                            stack = widget,
                            allEntities = entities,
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
                                allEntities = entities,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(HOME_WIDGET_AREA, widget.id) },
                                onSettings = { editingWeather = widget }
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
                        .height(52.dp),
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
            onAddWeather = { entityId, style -> viewModel.addWeatherToArea(HOME_WIDGET_AREA, entityId, style); showAddWidget = false },
            allEntities = entities
        )
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
            else -> AdvancedEntitySearchDialog(
                allEntities = entities,
                preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    targetStack?.let { stack ->
                        viewModel.updateWidget(
                            HOME_WIDGET_AREA,
                            stack.copy(entityIds = (stack.entityIds + entityIds).distinct())
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
            onDismiss = { addingToStackId = null; cameraAddMode = null },
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
            onDismissRequest = { addingToStackId = null; cameraAddMode = null },
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
                                        cameraRefreshInterval = 0
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
                TextButton(onClick = { addingToStackId = null; cameraAddMode = null; customCameraUrl = "" }) { Text("Cancel") }
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

    selectedButtonSettings?.let { (stack, entityId) ->
        ButtonConfigDialog(
            entity = entities.find { it.entity_id == entityId },
            config = stack.buttonConfigs[entityId] ?: HKIButtonConfig(),
            isCameraItem = stack.stackType == "camera",
            isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
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
        val refreshInterval = config?.cameraRefreshInterval ?: 0
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

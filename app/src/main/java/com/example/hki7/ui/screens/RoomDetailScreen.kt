@file:Suppress(
    "unused",
    "KotlinConstantConditions",
    "MoveLambdaOutsideParentheses",
    "CascadeIf",
    "SpellCheckingInspection",
    "UnusedBoxWithConstraintsScope",
    "GrazieInspection"
)

package com.example.hki7.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Switch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAServiceCall
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIEmptyStack
import com.example.hki7.data.HKIAreaConfig
import com.example.hki7.data.HKIRoomWidget
import com.example.hki7.data.HKIEnergyCardWidget
import com.example.hki7.data.HKIEnergyStack
import com.example.hki7.data.HKISingleEntityWidget
import com.example.hki7.data.HKISwipingStack
import com.example.hki7.data.HKISubtitleWidget
import com.example.hki7.data.HKIWeatherWidget
import androidx.compose.animation.core.tween
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.EntityCard
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.mediaPlayerStatus
import com.example.hki7.ui.components.mediaPlayerStateIcon
import com.example.hki7.ui.components.defaultEntityIconSlug
import com.example.hki7.ui.components.coverAccentColor
import com.example.hki7.ui.components.HKIDialog
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.HistoryPoint
import com.example.hki7.ui.components.HistoryRangeChips
import com.example.hki7.ui.components.InteractiveLineGraph
import com.example.hki7.ui.components.parseHistoryMillis
import com.example.hki7.ui.components.HKICameraDialog
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.components.HKILightDialog
import com.example.hki7.ui.components.buildWebRtcApiUrl
import com.example.hki7.ui.components.buildCameraRefreshModel
import com.example.hki7.ui.components.lightStateColor
import com.example.hki7.ui.components.resolveEntityCameraUrl
import com.example.hki7.ui.components.resolveCameraUrl
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.ReorderAxis
import com.example.hki7.ui.components.VerticalMasterSwitch
import com.example.hki7.ui.components.VerticalSlider
import com.example.hki7.ui.components.VerticalControlHeight
import com.example.hki7.ui.components.hvacColor
import com.example.hki7.ui.components.climateModeLabel
import com.example.hki7.ui.components.climateModeIcon
import com.example.hki7.ui.components.ClimateModesButton
import com.example.hki7.ui.components.ClimateModesList
import com.example.hki7.ui.components.CoverTiltSlider
import com.example.hki7.ui.components.HKIFanDialog
import com.example.hki7.ui.components.HKIHumidifierDialog
import com.example.hki7.ui.components.sensorGradientColors
import com.example.hki7.ui.components.HKIAlarmDialog
import com.example.hki7.ui.components.PersonDetailDialog
import com.example.hki7.ui.components.HKIBottomBar
import com.example.hki7.ui.components.HorizontalLightBar
import com.example.hki7.ui.components.HorizontalColorTempBar
import com.example.hki7.ui.components.HorizontalHueBar
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

data class WidgetStyleOverride(
    val isSquare: Boolean,
    val cornerRadius: Int
)

val swipingStackAnimationTypes = listOf(
    "swipe" to "Swipe",
    "fade" to "Fade",
    "zoom" to "Zoom",
    "slide_fade" to "Slide + fade",
    "cube" to "Cube",
    "instant" to "Instant"
)

fun HKIRoomWidget.withStackChildStyle(isSquare: Boolean, cornerRadius: Int): HKIRoomWidget = when (this) {
    is HKIButtonStack -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKISingleEntityWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIWeatherWidget -> copy(cornerRadius = cornerRadius)
    is HKIEmptyStack -> copy(
        isSquare = isSquare,
        cornerRadius = cornerRadius,
        widgets = widgets.map { it.withStackChildStyle(isSquare, cornerRadius) }
    )
    is HKISwipingStack -> copy(
        isSquare = isSquare,
        cornerRadius = cornerRadius,
        widgets = widgets.map { it.withStackChildStyle(isSquare, cornerRadius) }
    )
    else -> this
}

fun HKISwipingStack.withChangedChildStyle(updated: HKISwipingStack): HKISwipingStack {
    val styleChanged = isSquare != updated.isSquare || cornerRadius != updated.cornerRadius
    return if (styleChanged) updated.copy(
        widgets = updated.widgets.map { it.withStackChildStyle(updated.isSquare, updated.cornerRadius) }
    ) else updated
}

fun HKIEmptyStack.withChangedChildStyle(updated: HKIEmptyStack): HKIEmptyStack {
    val styleChanged = isSquare != updated.isSquare || cornerRadius != updated.cornerRadius
    return if (styleChanged) updated.copy(
        widgets = updated.widgets.map { it.withStackChildStyle(updated.isSquare, updated.cornerRadius) }
    ) else updated
}

@Composable
fun RoomDetailScreen(
    areaId: String,
    viewModel: MainViewModel,
    navController: NavController
) {
    val areas by viewModel.areas.collectAsState()
    val area = areas.find { it.area_id == areaId }
    val allEntities by viewModel.entities.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val areaWidgetsMapping by viewModel.areaWidgetsMapping.collectAsState()
    val areaConfigsMapping by viewModel.areaConfigsMapping.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val dashboardMode by viewModel.dashboardMode.collectAsState()
    val uiRevision by viewModel.uiRevision.collectAsState()

    val areaWidgets = areaWidgetsMapping[areaId] ?: emptyList()
    val widgetGridState = rememberLazyGridState()
    val areaConfig = areaConfigsMapping[areaId] ?: HKIAreaConfig()

    var showClimateDialog by remember { mutableStateOf(false) }
    var selectedClimateEntity by remember { mutableStateOf<HAEntity?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var selectedLockEntity by remember { mutableStateOf<HAEntity?>(null) }
    var showBlindDialog by remember { mutableStateOf(false) }
    var selectedBlindEntity by remember { mutableStateOf<HAEntity?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var selectedCameraEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedLightEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedLightConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showLightDialog by remember { mutableStateOf(false) }
    var showGroupLightDialog by remember { mutableStateOf(false) }
    var selectedGroupLightStackId by remember { mutableStateOf<String?>(null) }
    var selectedButtonSettings by remember { mutableStateOf<Pair<HKIButtonStack, String>?>(null) }
    var selectedSingleWidgetSettings by remember { mutableStateOf<Pair<String?, HKISingleEntityWidget>?>(null) }
    var editingEnergyCard by remember { mutableStateOf<Pair<String?, HKIEnergyCardWidget>?>(null) }
    var editingEnergyStack by remember { mutableStateOf<Pair<String?, HKIEnergyStack>?>(null) }
    var showEnergyCardPicker by remember { mutableStateOf(false) }
    var showEnergyStackPicker by remember { mutableStateOf(false) }
    var addingToStackId by remember { mutableStateOf<String?>(null) }
    var cameraAddMode by remember { mutableStateOf<String?>(null) } // "entity", "custom", or null
    var customCameraUrl by remember { mutableStateOf("") }
    var selectedCameraId by remember { mutableStateOf<String?>(null) }
    var selectedCameraStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    var editingStack by remember { mutableStateOf<HKIButtonStack?>(null) }
    var editingSwipingStack by remember { mutableStateOf<HKISwipingStack?>(null) }
    var editingEmptyStack by remember { mutableStateOf<HKIEmptyStack?>(null) }
    var editingSubtitle by remember { mutableStateOf<HKISubtitleWidget?>(null) }
    var editingWeather by remember { mutableStateOf<HKIWeatherWidget?>(null) }
    var selectedGenericEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedGenericConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showGenericDialog by remember { mutableStateOf(false) }
    var selectedVacuumEntityId by remember { mutableStateOf<String?>(null) }
    var selectedFanEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedFanConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showFanDialog by remember { mutableStateOf(false) }
    var selectedHumidifierEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedHumidifierConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showHumidifierDialog by remember { mutableStateOf(false) }
    var selectedAlarmEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedAlarmConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var selectedPersonEntity by remember { mutableStateOf<HAEntity?>(null) }
    var showPersonDialog by remember { mutableStateOf(false) }
    var showAddWidgetDialog by remember { mutableStateOf(false) }
    var addingToSwipingStackId by remember { mutableStateOf<String?>(null) }
    var addingToNestedStack by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingSingleWidgetKind by remember { mutableStateOf<String?>(null) }
    var pendingSingleWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingWeatherWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingWeatherWidgetEntityId by remember { mutableStateOf<String?>(null) }
    var choosingWeatherWidgetStyle by remember { mutableStateOf(false) }
    var selectedChildStackSettings by remember { mutableStateOf<Pair<String, HKIButtonStack>?>(null) }
    var selectedChildButtonSettings by remember { mutableStateOf<Triple<String, HKIButtonStack, String>?>(null) }
    var editingChildEmptyStack by remember { mutableStateOf<Pair<String, HKIEmptyStack>?>(null) }
    var editingChildSubtitle by remember { mutableStateOf<Pair<String, HKISubtitleWidget>?>(null) }
    var editingChildWeather by remember { mutableStateOf<Pair<String, HKIWeatherWidget>?>(null) }
    var showAutoWidgetInfo by remember { mutableStateOf(false) }
    var showAutoDeleteStackInfo by remember { mutableStateOf(false) }
    fun newButtonStack(title: String?, icon: String?) = HKIButtonStack(
        id = UUID.randomUUID().toString(),
        title = title,
        icon = icon,
        columns = 3,
        isSquare = true
    )
    fun newCameraStack(title: String?, icon: String?) = HKIButtonStack(
        id = UUID.randomUUID().toString(),
        title = title,
        icon = icon,
        columns = 2,
        isSquare = true,
        stackType = "camera"
    )
    fun newVacuumStack(title: String?, icon: String?) = HKIButtonStack(
        id = UUID.randomUUID().toString(),
        title = title,
        icon = icon,
        columns = 2,
        isSquare = true,
        stackType = "vacuum"
    )
    fun newWeatherStack(title: String?, icon: String?) = HKIButtonStack(
        id = UUID.randomUUID().toString(),
        title = title,
        icon = icon,
        columns = 1,
        isSquare = false,
        showBadge = false,
        cornerRadius = 24,
        stackType = "weather"
    )
    fun newEmptyStack() = HKIEmptyStack(id = UUID.randomUUID().toString())
    fun newSingleEntityWidget(kind: String, entityId: String) = HKISingleEntityWidget(
        id = UUID.randomUUID().toString(),
        entityId = entityId,
        kind = kind,
        isSquare = kind != "camera"
    )
    fun addChildToSwipingStack(stackId: String, child: HKIRoomWidget) {
        val swipe = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = areaWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(areaId, swipe.copy(widgets = swipe.widgets + child))
            empty != null -> viewModel.updateWidget(areaId, empty.copy(widgets = empty.widgets + child))
        }
    }
    fun updateChildInSwipingStack(stackId: String, child: HKIRoomWidget) {
        val swipe = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = areaWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(areaId, swipe.copy(widgets = swipe.widgets.map { if (it.id == child.id) child else it }))
            empty != null -> viewModel.updateWidget(areaId, empty.copy(widgets = empty.widgets.map { if (it.id == child.id) child else it }))
        }
    }
    fun deleteChildFromSwipingStack(stackId: String, childId: String) {
        val swipe = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = areaWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(areaId, swipe.copy(widgets = swipe.widgets.filterNot { it.id == childId }))
            empty != null -> viewModel.updateWidget(areaId, empty.copy(widgets = empty.widgets.filterNot { it.id == childId }))
        }
    }
    var openEntityFromSwipingChild: (String, HKIButtonStack) -> Unit = { _, _ -> }
    var openEntityFromSingleWidget: (String, String) -> Unit = { _, _ -> }
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
                allEntities = allEntities,
                viewModel = viewModel,
                currentUrl = currentUrl,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(areaId, child.id, entityId, "tap")
                        if (action == "more_info") openEntityFromSwipingChild(entityId, child)
                    }
                },
                onEntityDoubleClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(areaId, child.id, entityId, "double")
                        if (action == "more_info") openEntityFromSwipingChild(entityId, child)
                    }
                },
                onEntityLongClick = { entityId ->
                    if (!isEditMode) {
                        val action = viewModel.performButtonAction(areaId, child.id, entityId, "hold")
                        if (action == "more_info") openEntityFromSwipingChild(entityId, child)
                    }
                },
                onSettingsClick = { selectedChildStackSettings = parent.id to child },
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDeleteClick = { deleteChildFromSwipingStack(parent.id, child.id) },
                onHideClick = { updateChildInSwipingStack(parent.id, child.copy(isHidden = !child.isHidden)) },
                onAddClick = { addingToNestedStack = parent.id to child.id },
                onButtonSettings = { entityId -> selectedChildButtonSettings = Triple(parent.id, child, entityId) },
                onRemoveEntity = { entityId -> updateChildInSwipingStack(parent.id, child.copy(entityIds = child.entityIds - entityId)) },
                onCameraClick = { entityId -> selectedCameraId = entityId; selectedCameraStack = child },
                onBadgeClick = {},
                onReorderEntities = { from, to ->
                    updateChildInSwipingStack(
                        parent.id,
                        child.copy(entityIds = child.entityIds.toMutableList().apply { add(to, removeAt(from)) })
                    )
                }
            )
            is HKISubtitleWidget -> {
                SubtitleWidget(
                    child.copy(width = "full"),
                    isEditMode,
                    onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                    onSettings = { editingChildSubtitle = parent.id to child }
                )
            }
            is HKIWeatherWidget -> WeatherRoomWidget(
                widget = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                allEntities = allEntities,
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingChildWeather = parent.id to child }
            )
            is HKISingleEntityWidget -> SingleEntityWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                allEntities = allEntities,
                currentUrl = currentUrl,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = { entityId -> openEntityFromSingleWidget(entityId, child.kind) },
                onEntityDoubleClick = { entityId -> openEntityFromSingleWidget(entityId, child.kind) },
                onEntityLongClick = { entityId -> openEntityFromSingleWidget(entityId, child.kind) },
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
    fun openEntityDialog(entityId: String, stack: HKIButtonStack? = null) {
        val entity = allEntities.find { it.entity_id == entityId } ?: return
        when {
            entityId.startsWith("light.") -> {
                if (entity.supportsBrightness) {
                    selectedLightEntity = entity
                    selectedLightConfig = stack?.buttonConfigs?.get(entityId)
                    showLightDialog = true
                } else {
                    selectedGenericEntity = entity
                    selectedGenericConfig = stack?.buttonConfigs?.get(entityId)
                    showGenericDialog = true
                }
            }
            entityId.startsWith("climate.") -> {
                selectedClimateEntity = entity
                showClimateDialog = true
            }
            entityId.startsWith("lock.") -> {
                selectedLockEntity = entity
                showLockDialog = true
            }
            entityId.startsWith("cover.") -> {
                selectedBlindEntity = entity
                showBlindDialog = true
            }
            entityId.startsWith("camera.") -> {
                selectedCameraEntity = entity
                showCameraDialog = true
            }
            entityId.startsWith("vacuum.") -> {
                selectedVacuumEntityId = entityId
            }
            entityId.startsWith("fan.") -> {
                selectedFanEntity = entity
                selectedFanConfig = stack?.buttonConfigs?.get(entityId)
                showFanDialog = true
            }
            entityId.startsWith("humidifier.") -> {
                selectedHumidifierEntity = entity
                selectedHumidifierConfig = stack?.buttonConfigs?.get(entityId)
                showHumidifierDialog = true
            }
            entityId.startsWith("alarm_control_panel.") -> {
                selectedAlarmEntity = entity
                selectedAlarmConfig = stack?.buttonConfigs?.get(entityId)
                showAlarmDialog = true
            }
            entityId.startsWith("person.") -> {
                selectedPersonEntity = entity
                showPersonDialog = true
            }
            else -> {
                selectedGenericEntity = entity
                selectedGenericConfig = stack?.buttonConfigs?.get(entityId)
                showGenericDialog = true
            }
        }
    }

    openEntityFromSwipingChild = { entityId, stack -> openEntityDialog(entityId, stack) }
    openEntityFromSingleWidget = { entityId, kind ->
        if (kind == "camera") selectedCameraId = entityId else openEntityDialog(entityId, null)
    }
    val mediaPlayerEntity = areaConfig.mediaPlayerEntityId?.let { id -> allEntities.find { it.entity_id == id } }
    HKIPage(
        viewModel = viewModel,
        areaId = areaId,
        title = areaConfig.name ?: area?.name ?: "Room",
        subtitle = mediaPlayerStatus(mediaPlayerEntity) ?: "Room Details",
        subtitleIcon = mediaPlayerStateIcon(mediaPlayerEntity),
        backgroundImage = if (!areaConfig.headerColor.isNullOrBlank()) null else areaConfig.wallpaper ?: area?.picture,
        headerColor = areaConfig.headerColor,
        onBack = { navController.popBackStack() }
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
            key(isEditMode, uiRevision) {
                if (!isEditMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(widgetGridColumns),
                        state = widgetGridState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            count = areaWidgets.size,
                            key = { index -> areaWidgets[index].id },
                            contentType = { index -> areaWidgets[index]::class.simpleName ?: "widget" },
                            span = { index -> GridItemSpan(widgetSpan(areaWidgets[index])) }
                        ) { index ->
                            when (val widget = areaWidgets[index]) {
                                is HKIButtonStack -> {
                                    ButtonStackItem(
                                        stack = widget,
                                        allEntities = allEntities,
                                        viewModel = viewModel,
                                        currentUrl = currentUrl,
                                        accessToken = accessToken,
                                        isEditMode = false,
                                        onEntityClick = { entityId ->
                                            val action = viewModel.performButtonAction(areaId, widget.id, entityId, "tap")
                                            if (action == "more_info") openEntityDialog(entityId, widget)
                                        },
                                        onEntityDoubleClick = { entityId ->
                                            val action = viewModel.performButtonAction(areaId, widget.id, entityId, "double")
                                            if (action == "more_info") openEntityDialog(entityId, widget)
                                        },
                                        onEntityLongClick = { entityId ->
                                            val configuredAction = viewModel.performButtonAction(areaId, widget.id, entityId, "hold")
                                            if (configuredAction == "more_info") openEntityDialog(entityId, widget)
                                        },
                                        onSettingsClick = {},
                                        onToggleCollapsed = {
                                            viewModel.updateWidget(
                                                areaId,
                                                widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))
                                            )
                                        },
                                        onDeleteClick = {},
                                        onHideClick = {},
                                        onAddClick = {},
                                        onButtonSettings = {},
                                        onRemoveEntity = {},
                                        onCameraClick = { entityId ->
                                            selectedCameraId = entityId
                                            selectedCameraStack = widget
                                        },
                                        onBadgeClick = {
                                            selectedGroupLightStackId = widget.id
                                            showGroupLightDialog = true
                                        },
                                        onReorderEntities = { _, _ -> }
                                    )
                                }
                                is HKISubtitleWidget -> SubtitleWidget(
                                widget = widget,
                                isEditMode = false,
                                onDelete = {},
                                onSettings = {}
                            )
                                is HKIWeatherWidget -> WeatherRoomWidget(
                                    widget = widget,
                                    allEntities = allEntities,
                                    viewModel = viewModel,
                                    isEditMode = false,
                                    onDelete = {},
                                    onSettings = {}
                                )
                                is HKISingleEntityWidget -> SingleEntityWidgetItem(
                                    widget = widget,
                                    allEntities = allEntities,
                                    currentUrl = currentUrl,
                                    accessToken = accessToken,
                                    isEditMode = false,
                                    onEntityClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                                    onEntityDoubleClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                                    onEntityLongClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                                    onDeleteClick = {},
                                    onHideClick = {},
                                    onSettingsClick = {}
                                )
                                is HKISwipingStack -> SwipingStackItem(
                                    stack = widget,
                                    isEditMode = false,
                                    onSettingsClick = {},
                                    onToggleCollapsed = {
                                        viewModel.updateWidget(
                                            areaId,
                                            widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))
                                        )
                                    },
                                    onDeleteClick = {},
                                    onHideClick = {},
                                    onAddClick = {},
                                    content = { child -> RenderSwipingChild(widget, child) }
                                )
                                is HKIEmptyStack -> EmptyStackItem(
                                    stack = widget,
                                    isEditMode = false,
                                    onSettingsClick = {},
                                    onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
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
                                is HKIEnergyStack -> EnergyStackWidgetItem(
                                    stack = widget, viewModel = viewModel, isEditMode = false,
                                    onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDelete = {}, onSettings = {}
                                )
                            }
                        }
                    }
                } else {
                ReorderableGrid(
                    items = areaWidgets,
                    canReorder = true,
                    onReorder = { from, to -> viewModel.moveWidgetInArea(areaId, from, to) },
                    key = { it.id },
                    columns = GridCells.Fixed(widgetGridColumns),
                    span = { widgetSpan(it) },
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 156.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    axis = ReorderAxis.Vertical,
                    state = widgetGridState,
                    modifier = Modifier.weight(1f)
                ) { widget, _ ->
                    when (widget) {
                        is HKIButtonStack -> {
                            ButtonStackItem(
                                stack = widget,
                                allEntities = allEntities,
                                viewModel = viewModel,
                                currentUrl = currentUrl,
                                accessToken = accessToken,
                                isEditMode = isEditMode,
                            onEntityClick = { entityId ->
                                if (!isEditMode) {
                                    val action = viewModel.performButtonAction(areaId, widget.id, entityId, "tap")
                                    if (action == "more_info") openEntityDialog(entityId, widget)
                                }
                            },
                            onEntityDoubleClick = { entityId ->
                                if (!isEditMode) {
                                    val action = viewModel.performButtonAction(areaId, widget.id, entityId, "double")
                                    if (action == "more_info") openEntityDialog(entityId, widget)
                                }
                            },
                            onEntityLongClick = { entityId ->
                                if (!isEditMode) {
                                    val configuredAction = viewModel.performButtonAction(areaId, widget.id, entityId, "hold")
                                    if (configuredAction == "more_info") openEntityDialog(entityId, widget)
                                }
                                },
                                onSettingsClick = { editingStack = widget },
                                onToggleCollapsed = {
                                    viewModel.updateWidget(
                                        areaId,
                                        widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))
                                    )
                                },
                                onDeleteClick = {
                                    if (dashboardMode == "auto") showAutoDeleteStackInfo = true
                                    else viewModel.deleteWidget(areaId, widget.id)
                                },
                                onHideClick = { viewModel.updateWidget(areaId, widget.copy(isHidden = !widget.isHidden)) },
                                onAddClick = { addingToStackId = widget.id },
                                onButtonSettings = { entityId -> selectedButtonSettings = widget to entityId },
                                onRemoveEntity = { entityId ->
                                    viewModel.updateWidget(
                                        areaId,
                                        widget.copy(entityIds = widget.entityIds.filterNot { it == entityId })
                                    )
                                },
                                onCameraClick = { entityId ->
                                    selectedCameraId = entityId
                                    selectedCameraStack = widget
                                },
                                onBadgeClick = {
                                    selectedGroupLightStackId = widget.id
                                    showGroupLightDialog = true
                                },
                                onReorderEntities = { from, to ->
                                    val updated = widget.copy(
                                        entityIds = widget.entityIds.toMutableList().apply {
                                            add(to, removeAt(from))
                                        }
                                    )
                                    viewModel.updateWidget(areaId, updated)
                                }
                            )
                        }
                        is HKISubtitleWidget -> {
                            SubtitleWidget(
                                widget = widget,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                                onSettings = { editingSubtitle = widget }
                            )
                        }
                        is HKIWeatherWidget -> {
                            WeatherRoomWidget(
                                widget = widget,
                                allEntities = allEntities,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                                onSettings = { editingWeather = widget }
                            )
                        }
                        is HKISingleEntityWidget -> SingleEntityWidgetItem(
                            widget = widget,
                            allEntities = allEntities,
                            currentUrl = currentUrl,
                            accessToken = accessToken,
                            isEditMode = isEditMode,
                            onEntityClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                            onEntityDoubleClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                            onEntityLongClick = { entityId -> openEntityFromSingleWidget(entityId, widget.kind) },
                            onDeleteClick = { viewModel.deleteWidget(areaId, widget.id) },
                            onHideClick = { viewModel.updateWidget(areaId, widget.copy(isHidden = !widget.isHidden)) },
                            onSettingsClick = { selectedSingleWidgetSettings = null to widget }
                        )
                        is HKISwipingStack -> SwipingStackItem(
                            stack = widget,
                            isEditMode = isEditMode,
                            onSettingsClick = { editingSwipingStack = widget },
                            onToggleCollapsed = {
                                viewModel.updateWidget(
                                    areaId,
                                    widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))
                                )
                            },
                            onDeleteClick = {
                                if (dashboardMode == "auto") showAutoDeleteStackInfo = true
                                else viewModel.deleteWidget(areaId, widget.id)
                            },
                            onHideClick = { viewModel.updateWidget(areaId, widget.copy(isHidden = !widget.isHidden)) },
                            onAddClick = { addingToSwipingStackId = widget.id },
                            content = { child -> RenderSwipingChild(widget, child) }
                        )
                        is HKIEmptyStack -> EmptyStackItem(
                            stack = widget,
                            isEditMode = isEditMode,
                            onSettingsClick = { editingEmptyStack = widget },
                            onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDeleteClick = {
                                if (dashboardMode == "auto") showAutoDeleteStackInfo = true
                                else viewModel.deleteWidget(areaId, widget.id)
                            },
                            onHideClick = { viewModel.updateWidget(areaId, widget.copy(isHidden = !widget.isHidden)) },
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
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingEnergyCard = null to widget }
                        )
                        is HKIEnergyStack -> EnergyStackWidgetItem(
                            stack = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingEnergyStack = null to widget }
                        )
                    }
                }
                }
            }
        }

            if (isEditMode) {
                Button(
                    onClick = {
                        if (dashboardMode == "auto") showAutoWidgetInfo = true else showAddWidgetDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 87.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Widget")
                }
            }
        }
    }

    if (showAutoWidgetInfo) {
        AlertDialog(
            onDismissRequest = { showAutoWidgetInfo = false },
            title = { Text("Auto dashboard") },
            text = { Text("Rooms in auto mode are imported from Home Assistant. Switch to manual or take over the current dashboard to add widgets by hand.") },
            confirmButton = { Button(onClick = { showAutoWidgetInfo = false }) { Text("OK") } }
        )
    }

    if (showAutoDeleteStackInfo) {
        AlertDialog(
            onDismissRequest = { showAutoDeleteStackInfo = false },
            title = { Text("Auto dashboard") },
            text = { Text("Imported stacks cannot be removed in auto mode. Hide the stack instead, or take over / switch to manual mode to delete it permanently.") },
            confirmButton = { Button(onClick = { showAutoDeleteStackInfo = false }) { Text("OK") } }
        )
    }

    if (showAddWidgetDialog) {
        AddRoomWidgetDialog(
            onDismiss = { showAddWidgetDialog = false },
            onAddStack = { title, icon ->
                viewModel.addStackToArea(areaId, title, icon)
                showAddWidgetDialog = false
            },
            onAddCameraStack = {
                viewModel.addCameraStackToArea(areaId, it.first, it.second)
                showAddWidgetDialog = false
            },
            onAddVacuumStack = {
                viewModel.addVacuumStackToArea(areaId, it.first, it.second)
                showAddWidgetDialog = false
            },
            onAddSubtitle = { text, icon ->
                viewModel.addSubtitleToArea(areaId, text, icon)
                showAddWidgetDialog = false
            },
            onAddWeatherStack = { (title, icon) ->
                viewModel.addWeatherStackToArea(areaId, title, icon)
                showAddWidgetDialog = false
            },
            onAddSwipingStack = {
                viewModel.addSwipingStackToArea(areaId)
                showAddWidgetDialog = false
            },
            onAddEmptyStack = {
                viewModel.addEmptyStackToArea(areaId)
                showAddWidgetDialog = false
            },
            onAddButtonWidget = {
                pendingSingleWidgetKind = "button"
                pendingSingleWidgetContainerId = null
                showAddWidgetDialog = false
            },
            onAddCameraWidget = {
                pendingSingleWidgetKind = "camera"
                pendingSingleWidgetContainerId = null
                showAddWidgetDialog = false
            },
            onAddVacuumWidget = {
                pendingSingleWidgetKind = "vacuum"
                pendingSingleWidgetContainerId = null
                showAddWidgetDialog = false
            },
            onAddWeatherWidget = {
                pendingWeatherWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddEnergyCard = { showEnergyCardPicker = true; showAddWidgetDialog = false },
            onAddEnergyStack = { showEnergyStackPicker = true; showAddWidgetDialog = false },
            allEntities = allEntities
        )
    }

    if (showEnergyCardPicker) {
        EnergyCardPickerDialog(
            multiSelect = true, title = "Add Energy Cards",
            onDismiss = { showEnergyCardPicker = false },
            onSelected = { keys ->
                keys.forEach {
                    viewModel.addWidgetToArea(areaId, HKIEnergyCardWidget(id = UUID.randomUUID().toString(), cardKey = it))
                }
                showEnergyCardPicker = false
            }
        )
    }
    if (showEnergyStackPicker) {
        EnergyCardPickerDialog(
            multiSelect = true, title = "Energy Stack Cards",
            onDismiss = { showEnergyStackPicker = false },
            onSelected = { keys ->
                viewModel.addWidgetToArea(areaId, HKIEnergyStack(id = UUID.randomUUID().toString(), cardKeys = keys))
                showEnergyStackPicker = false
            }
        )
    }
    editingEnergyCard?.let { (containerId, w) ->
        EnergyCardWidgetSettingsDialog(w, onDismiss = { editingEnergyCard = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyCard = null
        }
    }
    editingEnergyStack?.let { (containerId, s) ->
        EnergyStackSettingsDialog(s, onDismiss = { editingEnergyStack = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyStack = null
        }
    }

    addingToSwipingStackId?.let { stackId ->
        AddRoomWidgetDialog(
            onDismiss = { addingToSwipingStackId = null },
            onAddStack = { title, icon ->
                addChildToSwipingStack(stackId, newButtonStack(title, icon))
                addingToSwipingStackId = null
            },
            onAddCameraStack = { (title, icon) ->
                addChildToSwipingStack(stackId, newCameraStack(title, icon))
                addingToSwipingStackId = null
            },
            onAddVacuumStack = { (title, icon) ->
                addChildToSwipingStack(stackId, newVacuumStack(title, icon))
                addingToSwipingStackId = null
            },
            onAddSubtitle = { text, icon ->
                addChildToSwipingStack(stackId, HKISubtitleWidget(id = UUID.randomUUID().toString(), text = text, icon = icon))
                addingToSwipingStackId = null
            },
            onAddWeatherStack = { (title, icon) ->
                addChildToSwipingStack(stackId, newWeatherStack(title, icon))
                addingToSwipingStackId = null
            },
            onAddEmptyStack = {
                addChildToSwipingStack(stackId, newEmptyStack())
                addingToSwipingStackId = null
            },
            onAddButtonWidget = {
                pendingSingleWidgetKind = "button"
                pendingSingleWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddCameraWidget = {
                pendingSingleWidgetKind = "camera"
                pendingSingleWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddVacuumWidget = {
                pendingSingleWidgetKind = "vacuum"
                pendingSingleWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddWeatherWidget = {
                pendingWeatherWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            allEntities = allEntities
        )
    }

    pendingSingleWidgetKind?.let { kind ->
        val candidates = when (kind) {
            "camera" -> allEntities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) }
            "vacuum" -> allEntities.filter { it.entity_id.startsWith("vacuum.") }
            else -> allEntities
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
                pendingSingleWidgetKind = null
                pendingSingleWidgetContainerId = null
            },
            onEntitiesSelected = { entityIds ->
                val entityId = entityIds.firstOrNull()
                if (entityId != null) {
                    val containerId = pendingSingleWidgetContainerId
                    if (containerId == null) {
                        viewModel.addSingleEntityWidgetToArea(areaId, kind, entityId)
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
        val weatherEntities = allEntities.filter { it.entity_id.startsWith("weather.") }
        AdvancedEntitySearchDialog(
            allEntities = weatherEntities.ifEmpty { allEntities },
            title = "Select Weather",
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (!choosingWeatherWidgetStyle) {
                    pendingWeatherWidgetContainerId = null
                    pendingWeatherWidgetEntityId = null
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
                pendingWeatherWidgetContainerId = null
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
                                        viewModel.addWeatherToArea(areaId, entityId, style)
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
                TextButton(onClick = {
                    choosingWeatherWidgetStyle = false
                    pendingWeatherWidgetContainerId = null
                    pendingWeatherWidgetEntityId = null
                }) { Text("Cancel") }
            }
        )
    }

    addingToNestedStack?.let { (swipingStackId, childStackId) ->
        val parent = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == swipingStackId }
        val targetStack = parent?.widgets?.filterIsInstance<HKIButtonStack>()?.find { it.id == childStackId }
        when (targetStack?.stackType) {
            "camera", "single_camera" -> AdvancedEntitySearchDialog(
                allEntities = allEntities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) },
                preselectedIds = targetStack.entityIds.toSet(),
                onDismiss = { addingToNestedStack = null },
                onEntitiesSelected = { entityIds ->
                    val updatedIds = if (targetStack.stackType == "single_camera") entityIds.take(1) else (targetStack.entityIds + entityIds).distinct()
                    updateChildInSwipingStack(swipingStackId, targetStack.copy(entityIds = updatedIds))
                    addingToNestedStack = null
                }
            )
            "weather" -> WeatherItemDialog(
                initial = null,
                allEntities = allEntities,
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
                allEntities = if (targetStack?.stackType in listOf("vacuum", "single_vacuum")) allEntities.filter { it.entity_id.startsWith("vacuum.") } else allEntities,
                preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
                onDismiss = { addingToNestedStack = null },
                onEntitiesSelected = { entityIds ->
                    targetStack?.let { stack ->
                        val updatedIds = if (stack.stackType in listOf("single_button", "single_vacuum")) entityIds.take(1) else (stack.entityIds + entityIds).distinct()
                        updateChildInSwipingStack(swipingStackId, stack.copy(entityIds = updatedIds))
                    }
                    addingToNestedStack = null
                }
            )
        }
    }

    if (addingToStackId != null && cameraAddMode == null) {
        val targetStack = areaWidgets.find { it.id == addingToStackId } as? HKIButtonStack
        if (targetStack?.stackType == "camera") {
            // Show choice dialog for camera stacks
            AlertDialog(
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
        } else if (targetStack?.stackType == "single_camera") {
            AdvancedEntitySearchDialog(
                allEntities = allEntities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) },
                preselectedIds = targetStack.entityIds.take(1).toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    viewModel.updateWidget(areaId, targetStack.copy(entityIds = entityIds.take(1)))
                    addingToStackId = null
                }
            )
        } else if (targetStack?.stackType == "single_vacuum") {
            AdvancedEntitySearchDialog(
                allEntities = allEntities.filter { it.entity_id.startsWith("vacuum.") },
                preselectedIds = targetStack.entityIds.take(1).toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    viewModel.updateWidget(areaId, targetStack.copy(entityIds = entityIds.take(1)))
                    addingToStackId = null
                }
            )
        } else if (targetStack?.stackType == "weather") {
            WeatherItemDialog(
                initial = null,
                allEntities = allEntities,
                onDismiss = { addingToStackId = null },
                onSave = { config ->
                    targetStack.let { stack ->
                        val itemId = "weather_${System.currentTimeMillis()}"
                        viewModel.updateWidget(
                            areaId,
                            stack.copy(
                                entityIds = stack.entityIds + itemId,
                                buttonConfigs = stack.buttonConfigs + (itemId to config)
                            )
                        )
                    }
                    addingToStackId = null
                }
            )
        } else {
            AdvancedEntitySearchDialog(
                allEntities = allEntities,
                preselectedIds = targetStack?.entityIds?.toSet().orEmpty(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    targetStack?.let { stack ->
                        viewModel.updateWidget(
                            areaId,
                            stack.copy(entityIds = if (stack.stackType == "single_button") entityIds.take(1) else (stack.entityIds + entityIds).distinct())
                        )
                    }
                    addingToStackId = null
                }
            )
        }
    } else if (addingToStackId != null && cameraAddMode == "entity") {
        val targetStack = areaWidgets.find { it.id == addingToStackId } as? HKIButtonStack
        val cameraEntities = allEntities.filter {
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
                        areaId,
                        stack.copy(entityIds = (stack.entityIds + entityIds).distinct())
                    )
                }
                addingToStackId = null
                cameraAddMode = null
            }
        )
    } else if (addingToStackId != null && cameraAddMode == "custom") {
        val targetStack = areaWidgets.find { it.id == addingToStackId } as? HKIButtonStack
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
                    Text("Enter HTTP URL, relative path, or WebRTC ID (e.g., poort_hd)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (customCameraUrl.isNotBlank()) {
                        targetStack?.let { stack ->
                            val customId = "custom_camera_${System.currentTimeMillis()}"
                            viewModel.updateWidget(
                                areaId,
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
            dismissButton = { TextButton(onClick = { addingToStackId = null; cameraAddMode = null; customCameraUrl = "" }) { Text("Cancel") } }
        )
    }

    if (editingStack != null) {
        StackSettingsDialog(
            stack = editingStack!!,
            onDismiss = { editingStack = null },
            onUpdate = { updated ->
                viewModel.updateWidget(areaId, updated)
                editingStack = null
            }
        )
    }

    editingSwipingStack?.let { stack ->
        SwipingStackSettingsDialog(
            stack = stack,
            onDismiss = { editingSwipingStack = null },
            onUpdate = { updated ->
                viewModel.updateWidget(areaId, stack.withChangedChildStyle(updated))
                editingSwipingStack = null
            }
        )
    }

    editingEmptyStack?.let { stack ->
        EmptyStackSettingsDialog(
            stack = stack,
            onDismiss = { editingEmptyStack = null },
            onUpdate = { updated ->
                viewModel.updateWidget(areaId, stack.withChangedChildStyle(updated))
                editingEmptyStack = null
            }
        )
    }

    selectedChildStackSettings?.let { (containerId, stack) ->
        StackSettingsDialog(
            stack = stack,
            onDismiss = { selectedChildStackSettings = null },
            onUpdate = { updated ->
                updateChildInSwipingStack(containerId, updated)
                selectedChildStackSettings = null
            }
        )
    }

    editingChildEmptyStack?.let { (containerId, stack) ->
        EmptyStackSettingsDialog(
            stack = stack,
            onDismiss = { editingChildEmptyStack = null },
            onUpdate = { updated ->
                updateChildInSwipingStack(containerId, stack.withChangedChildStyle(updated))
                editingChildEmptyStack = null
            }
        )
    }

    editingChildSubtitle?.let { (containerId, widget) ->
        HeaderTextSettingsDialog(
            widget = widget,
            onDismiss = { editingChildSubtitle = null },
            onSave = { updated ->
                updateChildInSwipingStack(containerId, updated)
                editingChildSubtitle = null
            }
        )
    }

    editingChildWeather?.let { (containerId, widget) ->
        WeatherWidgetSettingsDialog(
            widget = widget,
            allEntities = allEntities,
            onDismiss = { editingChildWeather = null },
            onSave = { updated ->
                updateChildInSwipingStack(containerId, updated)
                editingChildWeather = null
            }
        )
    }

    editingSubtitle?.let { widget ->
        HeaderTextSettingsDialog(
            widget = widget,
            onDismiss = { editingSubtitle = null },
            onSave = { updated ->
                viewModel.updateWidget(areaId, updated)
                editingSubtitle = null
            }
        )
    }

    editingWeather?.let { widget ->
        WeatherWidgetSettingsDialog(
            widget = widget,
            allEntities = allEntities,
            onDismiss = { editingWeather = null },
            onSave = { updated ->
                viewModel.updateWidget(areaId, updated)
                editingWeather = null
            }
        )
    }

    selectedSingleWidgetSettings?.let { (containerId, widget) ->
        val entity = allEntities.find { it.entity_id == widget.entityId }
        ButtonConfigDialog(
            entity = entity,
            config = widget.config,
            isCameraItem = widget.kind == "camera",
            isVacuumItem = widget.kind == "vacuum" || widget.entityId.startsWith("vacuum."),
            allEntities = allEntities,
            onDismiss = { selectedSingleWidgetSettings = null },
            widgetAppearance = WidgetAppearance(widget.isSquare, widget.cornerRadius, widget.width),
            onSaveWithAppearance = { config, a ->
                if (containerId == null) {
                    val latest = areaWidgets.filterIsInstance<HKISingleEntityWidget>().find { it.id == widget.id } ?: widget
                    viewModel.updateWidget(
                        areaId,
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
                val updated = widget.copy(config = config)
                if (containerId == null) {
                    val latest = areaWidgets.filterIsInstance<HKISingleEntityWidget>().find { it.id == widget.id } ?: widget
                    viewModel.updateWidget(areaId, latest.copy(config = config))
                } else {
                    updateChildInSwipingStack(containerId, updated)
                }
                selectedSingleWidgetSettings = null
            }
        )
    }

    selectedChildButtonSettings?.let { (containerId, stack, entityId) ->
        val entity = allEntities.find { it.entity_id == entityId }
        if (stack.stackType == "weather") {
            WeatherItemDialog(
                initial = stack.buttonConfigs[entityId],
                allEntities = allEntities,
                onDismiss = { selectedChildButtonSettings = null },
                onSave = { config ->
                    updateChildInSwipingStack(
                        containerId,
                        stack.copy(buttonConfigs = stack.buttonConfigs + (entityId to config))
                    )
                    selectedChildButtonSettings = null
                }
            )
        } else {
            ButtonConfigDialog(
                entity = entity,
                config = stack.buttonConfigs[entityId] ?: HKIButtonConfig(),
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = allEntities,
                onDismiss = { selectedChildButtonSettings = null },
                onSave = { config ->
                    updateChildInSwipingStack(
                        containerId,
                        stack.copy(buttonConfigs = stack.buttonConfigs + (entityId to config))
                    )
                    selectedChildButtonSettings = null
                }
            )
        }
    }

    selectedButtonSettings?.let { (stack, entityId) ->
        val entity = allEntities.find { it.entity_id == entityId }
        if (stack.stackType == "weather") {
            WeatherItemDialog(
                initial = stack.buttonConfigs[entityId],
                allEntities = allEntities,
                onDismiss = { selectedButtonSettings = null },
                onSave = { config ->
                    val latestStack = areaWidgets.find { it.id == stack.id } as? HKIButtonStack ?: stack
                    viewModel.updateWidget(
                        areaId,
                        latestStack.copy(buttonConfigs = latestStack.buttonConfigs + (entityId to config))
                    )
                    selectedButtonSettings = null
                }
            )
        } else {
            ButtonConfigDialog(
                entity = entity,
                config = stack.buttonConfigs[entityId] ?: HKIButtonConfig(),
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = allEntities,
                onDismiss = { selectedButtonSettings = null },
                onSave = { config ->
                    val latestStack = areaWidgets.find { it.id == stack.id } as? HKIButtonStack ?: stack
                    viewModel.updateWidget(
                        areaId,
                        latestStack.copy(buttonConfigs = latestStack.buttonConfigs + (entityId to config))
                    )
                    selectedButtonSettings = null
                }
            )
        }
    }

    if (selectedCameraId != null && selectedCameraStack != null) {
        val entity = allEntities.find { it.entity_id == selectedCameraId }
        val config = selectedCameraStack!!.buttonConfigs[selectedCameraId]
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
            onDismiss = { selectedCameraId = null; selectedCameraStack = null }
        )
    }

    if (showLightDialog && selectedLightEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedLightEntity!!.entity_id } ?: selectedLightEntity!!
        // Group button only shows for actual group entities (e.g. light.living_room), whose
        // `entity_id` attribute lists their own members — not the dashboard stack it sits in.
        val childEntities = entity.childEntityIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
        HKILightDialog(
            entity = entity,
            viewModel = viewModel,
            onDismiss = { showLightDialog = false; selectedLightConfig = null },
            titleOverride = selectedLightConfig?.name,
            iconName = selectedLightConfig?.icon,
            spinIcon = selectedLightConfig?.spinIcon == true,
            groupContent = if (childEntities.isNotEmpty()) {
                { GroupMembersContent(entities = childEntities, viewModel = viewModel) }
            } else null
        )
    }

    if (showFanDialog && selectedFanEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedFanEntity!!.entity_id } ?: selectedFanEntity!!
        HKIFanDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = selectedFanConfig?.name,
            iconName = selectedFanConfig?.icon,
            spinIcon = selectedFanConfig?.spinIcon == true,
            onDismiss = { showFanDialog = false; selectedFanConfig = null }
        )
    }

    if (showHumidifierDialog && selectedHumidifierEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedHumidifierEntity!!.entity_id } ?: selectedHumidifierEntity!!
        HKIHumidifierDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = selectedHumidifierConfig?.name,
            iconName = selectedHumidifierConfig?.icon,
            spinIcon = selectedHumidifierConfig?.spinIcon == true,
            onDismiss = { showHumidifierDialog = false; selectedHumidifierConfig = null }
        )
    }

    if (showAlarmDialog && selectedAlarmEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedAlarmEntity!!.entity_id } ?: selectedAlarmEntity!!
        HKIAlarmDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = selectedAlarmConfig?.name,
            iconName = selectedAlarmConfig?.icon,
            spinIcon = selectedAlarmConfig?.spinIcon == true,
            onDismiss = { showAlarmDialog = false; selectedAlarmConfig = null }
        )
    }

    if (showPersonDialog && selectedPersonEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedPersonEntity!!.entity_id } ?: selectedPersonEntity!!
        PersonDetailDialog(person = entity, viewModel = viewModel, onDismiss = { showPersonDialog = false })
    }

    if (showGroupLightDialog && selectedGroupLightStackId != null) {
        val stack = areaWidgets.filterIsInstance<HKIButtonStack>().find { it.id == selectedGroupLightStackId }
        // Activity badge opens the entire group so the user can manage / turn off individual entities.
        val groupEntities = stack?.entityIds
            ?.mapNotNull { id -> allEntities.find { it.entity_id == id } }
            .orEmpty()
        if (stack != null && groupEntities.isNotEmpty()) {
            GroupEntityDialog(
                stack = stack,
                entities = groupEntities,
                viewModel = viewModel,
                onDismiss = {
                    showGroupLightDialog = false
                    selectedGroupLightStackId = null
                }
            )
        } else {
            showGroupLightDialog = false
            selectedGroupLightStackId = null
        }
    }

    if (showClimateDialog && selectedClimateEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedClimateEntity!!.entity_id } ?: selectedClimateEntity!!
        val ids = areaConfig.climateEntityIds.ifEmpty { listOfNotNull(areaConfig.climateEntityId) }
        val entities = allEntities.filter { it.entity_id in ids }.ifEmpty { listOf(entity) }
        // Climate entities aren't stack buttons themselves, but if one happens to also sit in a
        // stack on this dashboard, reuse that stack's button settings (e.g. configured sensors).
        val climateButtonConfigs = areaWidgets.filterIsInstance<HKIButtonStack>()
            .flatMap { it.buttonConfigs.entries }
            .associate { it.key to it.value }
        PagedRoleDialog("climate", entities, viewModel, { showClimateDialog = false }, climateButtonConfigs)
    }

    if (showLockDialog && selectedLockEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedLockEntity!!.entity_id } ?: selectedLockEntity!!
        val ids = areaConfig.lockEntityIds.ifEmpty { listOfNotNull(areaConfig.lockEntityId) }
        val entities = allEntities.filter { it.entity_id in ids }.ifEmpty { listOf(entity) }
        val buttonConfigs = areaWidgets.filterIsInstance<HKIButtonStack>()
            .flatMap { it.buttonConfigs.entries }
            .associate { it.key to it.value }
        PagedRoleDialog("lock", entities, viewModel, { showLockDialog = false }, buttonConfigs)
    }

    if (showBlindDialog && selectedBlindEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedBlindEntity!!.entity_id } ?: selectedBlindEntity!!
        val ids = areaConfig.blindEntityIds.ifEmpty { listOfNotNull(areaConfig.blindEntityId) }
        val entities = allEntities.filter { it.entity_id in ids }.ifEmpty { listOf(entity) }
        val buttonConfigs = areaWidgets.filterIsInstance<HKIButtonStack>()
            .flatMap { it.buttonConfigs.entries }
            .associate { it.key to it.value }
        PagedRoleDialog("cover", entities, viewModel, { showBlindDialog = false }, buttonConfigs)
    }

    if (showCameraDialog && selectedCameraEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedCameraEntity!!.entity_id } ?: selectedCameraEntity!!
        val streamUrl = resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
        HKICameraDialog(
            title = entity.friendlyName ?: entity.entity_id,
            imageUrl = buildCameraRefreshModel(streamUrl, 0, 0),
            refreshIntervalSeconds = 0,
            liveWebUrl = streamUrl,
            authToken = accessToken,
            statusText = "Live",
            onDismiss = { showCameraDialog = false }
        )
    }

    if (showGenericDialog && selectedGenericEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedGenericEntity!!.entity_id } ?: selectedGenericEntity!!
        GenericEntityDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = selectedGenericConfig?.name,
            iconName = selectedGenericConfig?.icon,
            spinIcon = selectedGenericConfig?.spinIcon == true,
            onDismiss = { showGenericDialog = false; selectedGenericConfig = null }
        )
    }

    selectedVacuumEntityId?.let { vId ->
        // Open the vacuum dialog with all vacuums in the same stack (swipe between them), using per-entity config.
        val stack = areaWidgets.filterIsInstance<HKIButtonStack>().find { it.entityIds.contains(vId) }
        val vacEntities = (stack?.entityIds ?: listOf(vId))
            .filter { it.startsWith("vacuum.") }
            .mapNotNull { id -> allEntities.find { it.entity_id == id } }
        if (vacEntities.isNotEmpty()) {
            VacuumStackDialog(
                entities = vacEntities,
                startIndex = vacEntities.indexOfFirst { it.entity_id == vId }.coerceAtLeast(0),
                buttonConfigs = stack?.buttonConfigs ?: emptyMap(),
                allEntities = allEntities,
                currentUrl = currentUrl,
                viewModel = viewModel,
                onDismiss = { selectedVacuumEntityId = null }
            )
        } else {
            selectedVacuumEntityId = null
        }
    }
}

@Composable
fun AddRoomWidgetDialog(
    onDismiss: () -> Unit,
    onAddStack: (String?, String?) -> Unit,
    onAddCameraStack: (Pair<String?, String?>) -> Unit,
    onAddSubtitle: (String, String?) -> Unit,
    onAddVacuumStack: (Pair<String?, String?>) -> Unit = {},
    onAddWeatherStack: (Pair<String?, String?>) -> Unit = {},
    onAddSwipingStack: (() -> Unit)? = null,
    onAddEmptyStack: (() -> Unit)? = null,
    onAddButtonWidget: (() -> Unit)? = null,
    onAddCameraWidget: (() -> Unit)? = null,
    onAddVacuumWidget: (() -> Unit)? = null,
    onAddWeatherWidget: (() -> Unit)? = null,
    onAddEnergyCard: (() -> Unit)? = null,
    onAddEnergyStack: (() -> Unit)? = null,
    allEntities: List<HAEntity> = emptyList()
) {
    val appColors = LocalHKIAppColors.current
    var stackTitle by remember { mutableStateOf("Buttons") }
    var stackIcon by remember { mutableStateOf("Lightbulb") }
    var cameraTitle by remember { mutableStateOf("Cameras") }
    var cameraIcon by remember { mutableStateOf("CameraAlt") }
    var vacuumTitle by remember { mutableStateOf("Vacuum") }
    var vacuumIcon by remember { mutableStateOf("CleaningServices") }
    var headerText by remember { mutableStateOf("Header Text") }
    var headerIcon by remember { mutableStateOf("None") }
    var weatherTitle by remember { mutableStateOf("Weather") }
    var weatherIcon by remember { mutableStateOf("weather-partly-cloudy") }
    var configureWidget by remember { mutableStateOf<String?>(null) }
    var widgetGroup by remember { mutableStateOf<String?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }

    if (showIconPicker) {
        val currentForPicker = when (configureWidget) {
            "button", "button_stack" -> stackIcon; "camera", "camera_stack" -> cameraIcon
            "vacuum", "vacuum_stack" -> vacuumIcon; "weather", "weather_stack" -> weatherIcon; else -> headerIcon
        }.takeUnless { it == "None" } ?: ""
        MdiIconPickerDialog(
            current = currentForPicker,
            onDismiss = { showIconPicker = false },
            onSelect = { slug ->
                val name = slug.ifEmpty { "None" }
                when (configureWidget) {
                    "button", "button_stack" -> stackIcon = name; "camera", "camera_stack" -> cameraIcon = name
                    "vacuum", "vacuum_stack" -> vacuumIcon = name; "weather", "weather_stack" -> weatherIcon = name; else -> headerIcon = name
                }
                showIconPicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Widget", color = appColors.onSurface, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = appColors.onSurface) }
                }
                if (configureWidget == null && widgetGroup == null) {
                    WidgetChoice(
                        icon = Icons.Default.Lightbulb,
                        title = "Button",
                        subtitle = "A single configurable entity control",
                        onClick = { onAddButtonWidget?.invoke() ?: run { stackTitle = ""; stackIcon = "None"; configureWidget = "button" } }
                    )
                    WidgetChoice(
                        icon = Icons.Default.CameraAlt,
                        title = "Camera",
                        subtitle = "A single live camera tile or stream URL",
                        onClick = { onAddCameraWidget?.invoke() ?: run { cameraTitle = ""; cameraIcon = "None"; configureWidget = "camera" } }
                    )
                    WidgetChoice(
                        icon = Icons.AutoMirrored.Filled.ShortText,
                        title = "Header Text",
                        subtitle = "Add a section heading",
                        onClick = { configureWidget = "header" }
                    )
                    WidgetChoice(
                        icon = Icons.AutoMirrored.Filled.ViewQuilt,
                        title = "Stacks",
                        subtitle = "Button, camera, vacuum, weather, swipe, and empty stacks",
                        onClick = { widgetGroup = "stacks" }
                    )
                    WidgetChoice(
                        icon = Icons.Default.CleaningServices,
                        title = "Vacuum",
                        subtitle = "A single robot vacuum control",
                        onClick = { onAddVacuumWidget?.invoke() ?: run { vacuumTitle = ""; vacuumIcon = "None"; configureWidget = "vacuum" } }
                    )
                    WidgetChoice(
                        icon = Icons.Default.WbSunny,
                        title = "Weather",
                        subtitle = "A single weather card",
                        onClick = { onAddWeatherWidget?.invoke() ?: run { weatherTitle = ""; weatherIcon = "None"; configureWidget = "weather" } }
                    )
                    if (onAddEnergyCard != null) {
                        WidgetChoice(
                            icon = Icons.Default.ElectricBolt,
                            title = "Energy Card",
                            subtitle = "Any card from the Energy view",
                            onClick = { onAddEnergyCard(); onDismiss() }
                        )
                    }
                } else if (widgetGroup == "stacks" && configureWidget == null) {
                    if (onAddEmptyStack != null) {
                        WidgetChoice(
                            icon = Icons.AutoMirrored.Filled.ViewQuilt,
                            title = "Empty Stack",
                            subtitle = "Place widgets inside a configurable stack",
                            onClick = {
                                onAddEmptyStack()
                                onDismiss()
                            }
                        )
                    }
                    Text("Predefined Stacks", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                    WidgetChoice(Icons.AutoMirrored.Filled.ViewQuilt, "Button Stack", "Group entities into configurable controls") { stackTitle = "Buttons"; stackIcon = "Lightbulb"; configureWidget = "button_stack" }
                    WidgetChoice(Icons.Default.CameraAlt, "Camera Stack", "Show live camera tiles or a custom stream URL") { cameraTitle = "Cameras"; cameraIcon = "CameraAlt"; configureWidget = "camera_stack" }
                    if (onAddSwipingStack != null) {
                        WidgetChoice(
                            icon = Icons.AutoMirrored.Filled.ViewQuilt,
                            title = "Swiping Stack",
                            subtitle = "Swipe horizontally through nested widgets",
                            onClick = {
                                onAddSwipingStack()
                                onDismiss()
                            }
                        )
                    }
                    WidgetChoice(Icons.Default.CleaningServices, "Vacuum Stack", "Control robot vacuums with map and battery") { vacuumTitle = "Vacuum"; vacuumIcon = "CleaningServices"; configureWidget = "vacuum_stack" }
                    WidgetChoice(Icons.Default.WbSunny, "Weather Stack", "Group weather cards: conditions, forecast, wind, or rain map") { weatherTitle = "Weather"; weatherIcon = "weather-partly-cloudy"; configureWidget = "weather_stack" }
                    if (onAddEnergyStack != null) {
                        WidgetChoice(
                            icon = Icons.Default.ElectricBolt,
                            title = "Energy Stack",
                            subtitle = "Group energy cards: usage, solar, gas, water, ...",
                            onClick = { onAddEnergyStack(); onDismiss() }
                        )
                    }
                    OutlinedButton(onClick = { widgetGroup = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
                } else {
                    Text(
                        when (configureWidget) {
                            "button", "button_stack" -> if (configureWidget == "button") "Button" else "Button Stack"
                            "camera", "camera_stack" -> if (configureWidget == "camera") "Camera" else "Camera Stack"
                            "vacuum", "vacuum_stack" -> if (configureWidget == "vacuum") "Vacuum" else "Vacuum Stack"
                            "weather", "weather_stack" -> if (configureWidget == "weather") "Weather" else "Weather Stack"
                            else -> "Header Text"
                        },
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = when (configureWidget) {
                            "button", "button_stack" -> stackTitle
                            "camera", "camera_stack" -> cameraTitle
                            "vacuum", "vacuum_stack" -> vacuumTitle
                            "weather", "weather_stack" -> weatherTitle
                            else -> headerText
                        },
                        onValueChange = {
                            when (configureWidget) {
                                "button", "button_stack" -> stackTitle = it
                                "camera", "camera_stack" -> cameraTitle = it
                                "vacuum", "vacuum_stack" -> vacuumTitle = it
                                "weather", "weather_stack" -> weatherTitle = it
                                else -> headerText = it
                            }
                        },
                        label = { Text(if (configureWidget == "header") "Header text" else "Stack title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.onSurface,
                            unfocusedTextColor = appColors.onSurface,
                            focusedLabelColor = appColors.onSurface.copy(alpha = 0.8f),
                            unfocusedLabelColor = appColors.onMuted,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = appColors.onMuted
                        )
                    )
                    Text("Icon", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                    val currentWidgetIcon = when (configureWidget) {
                        "button", "button_stack" -> stackIcon; "camera", "camera_stack" -> cameraIcon
                        "vacuum", "vacuum_stack" -> vacuumIcon; "weather", "weather_stack" -> weatherIcon; else -> headerIcon
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentWidgetIcon != "None") {
                            MdiIcon(currentWidgetIcon, size = 24.dp, tint = appColors.onSurface)
                        }
                        Text(
                            currentWidgetIcon.takeUnless { it == "None" } ?: "None",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        TextButton(onClick = { showIconPicker = true }) { Text("Change") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { configureWidget = null }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                if (configureWidget == "button" || configureWidget == "button_stack") {
                                    onAddStack(stackTitle.ifBlank { null }, stackIcon.takeUnless { it == "None" })
                                } else if (configureWidget == "camera" || configureWidget == "camera_stack") {
                                    onAddCameraStack(cameraTitle.ifBlank { null } to cameraIcon.takeUnless { it == "None" })
                                } else if (configureWidget == "vacuum" || configureWidget == "vacuum_stack") {
                                    onAddVacuumStack(vacuumTitle.ifBlank { null } to vacuumIcon.takeUnless { it == "None" })
                                } else if (configureWidget == "weather" || configureWidget == "weather_stack") {
                                    onAddWeatherStack(weatherTitle.ifBlank { null } to weatherIcon.takeUnless { it == "None" })
                                } else {
                                    onAddSubtitle(headerText.ifBlank { "Header Text" }, headerIcon.takeUnless { it == "None" })
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = appColors.subtleSurface
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = appColors.onSurface, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = appColors.onMuted)
        }
    }
}

/** Shape/size of a standalone (non-stacked) widget, edited from its button settings dialog. */
data class WidgetAppearance(val isSquare: Boolean, val cornerRadius: Int, val width: String)

@Composable
fun ButtonConfigDialog(
    entity: HAEntity?,
    config: HKIButtonConfig,
    isCameraItem: Boolean = false,
    isVacuumItem: Boolean = false,
    allEntities: List<HAEntity> = emptyList(),
    onDismiss: () -> Unit,
    // Standalone widgets only: shape/roundness/width are edited here since there is no stack
    // to inherit them from. When set, Save reports through onSaveWithAppearance instead.
    widgetAppearance: WidgetAppearance? = null,
    onSaveWithAppearance: ((HKIButtonConfig, WidgetAppearance) -> Unit)? = null,
    onSave: (HKIButtonConfig) -> Unit
) {
    var appearIsSquare by remember(widgetAppearance) { mutableStateOf(widgetAppearance?.isSquare ?: true) }
    var appearRadius by remember(widgetAppearance) { mutableIntStateOf(widgetAppearance?.cornerRadius ?: 28) }
    var appearWidth by remember(widgetAppearance) { mutableStateOf(widgetAppearance?.width ?: "half") }
    var name by remember(config) { mutableStateOf(config.name ?: entity?.friendlyName ?: entity?.entity_id ?: "") }
    var label by remember(config) { mutableStateOf(config.label ?: "") }
    var cameraUrl by remember(config) { mutableStateOf(config.cameraUrl ?: "") }
    var refreshInterval by remember(config) { mutableIntStateOf(config.cameraRefreshInterval) }
    var iconName by remember(config) { mutableStateOf(config.icon ?: "None") }
    var spinIcon by remember(config) { mutableStateOf(config.spinIcon) }
    var tapAction by remember(config) { mutableStateOf(config.tapAction) }
    var doubleAction by remember(config) { mutableStateOf(config.doubleTapAction) }
    var holdAction by remember(config) { mutableStateOf(config.holdAction) }
    // Lock door sensor
    val isLockEntity = entity?.entity_id?.startsWith("lock.") == true
    var doorEntityId by remember(config) { mutableStateOf(config.doorEntityId) }
    var showDoorPicker by remember { mutableStateOf(false) }
    var showIconPickerBtn by remember { mutableStateOf(false) }
    // Climate temp/humidity sensors
    val isClimateEntity = entity?.entity_id?.startsWith("climate.") == true
    var climateTempSensorEntityId by remember(config) { mutableStateOf(config.climateTempSensorEntityId) }
    var climateHumiditySensorEntityId by remember(config) { mutableStateOf(config.climateHumiditySensorEntityId) }
    var showTempSensorPicker by remember { mutableStateOf(false) }
    var showHumiditySensorPicker by remember { mutableStateOf(false) }

    if (showIconPickerBtn) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPickerBtn = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPickerBtn = false
            }
        )
    }

    // Vacuum settings (inline; pickers hoisted below the dialog so they render on top)
    var vacuumDisplayMode by remember(config) { mutableStateOf(config.vacuumDisplayMode) }
    var vacuumMapEntityId by remember(config) { mutableStateOf(config.vacuumMapEntityId) }
    var vacuumBatteryEntityId by remember(config) { mutableStateOf(config.vacuumBatteryEntityId) }
    var vacuumImageUrl by remember(config) { mutableStateOf(config.vacuumImageUrl ?: "") }
    var showMapPicker by remember { mutableStateOf(false) }
    var showBattPicker by remember { mutableStateOf(false) }
    val actions = listOf("toggle", "more_info", "none")
    val refreshOptions = listOf("Live" to 0, "5s" to 5, "10s" to 10, "15s" to 15, "30s" to 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isVacuumItem) "Vacuum Settings" else "Button Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!isVacuumItem) {
                    OutlinedTextField(label, { label = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (isCameraItem) {
                    if (config.isCustomUrl) {
                        OutlinedTextField(
                            value = cameraUrl,
                            onValueChange = { cameraUrl = it },
                            label = { Text("Custom camera URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("HTTP URL, relative path, or WebRTC name (e.g., url: poort_hd)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Refresh interval", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        refreshOptions.forEach { (label, seconds) ->
                            FilterChip(
                                selected = refreshInterval == seconds,
                                onClick = { refreshInterval = seconds },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                if (isVacuumItem) {
                    Text("Button image", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("static" to "Robot icon", "camera" to "Map camera", "external" to "External URL").forEach { (v, l) ->
                            FilterChip(selected = vacuumDisplayMode == v, onClick = { vacuumDisplayMode = v }, label = { Text(l) })
                        }
                    }
                    if (vacuumDisplayMode == "external") {
                        OutlinedTextField(
                            value = vacuumImageUrl,
                            onValueChange = { vacuumImageUrl = it },
                            label = { Text("Image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("Map camera (for dialog)", style = MaterialTheme.typography.labelLarge)
                    val mapName = vacuumMapEntityId?.takeIf { it.isNotBlank() }?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(mapName ?: "Not set", style = MaterialTheme.typography.bodySmall, color = if (mapName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showMapPicker = true }) { Text("Change") }
                        if (!vacuumMapEntityId.isNullOrBlank()) { TextButton(onClick = { vacuumMapEntityId = null }) { Text("Clear") } }
                    }
                    Text("Battery sensor (optional)", style = MaterialTheme.typography.labelLarge)
                    val battName = vacuumBatteryEntityId?.takeIf { it.isNotBlank() }?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(battName ?: "Built-in", style = MaterialTheme.typography.bodySmall, color = if (battName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showBattPicker = true }) { Text("Change") }
                        if (!vacuumBatteryEntityId.isNullOrBlank()) { TextButton(onClick = { vacuumBatteryEntityId = null }) { Text("Clear") } }
                    }
                }
                if (!isCameraItem && !isVacuumItem) {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            iconName.takeUnless { it == "None" } ?: "None",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { showIconPickerBtn = true }) { Text("Change") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Spin icon", style = MaterialTheme.typography.labelLarge)
                            Text("Rotates continuously while the entity isn't off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = spinIcon, onCheckedChange = { spinIcon = it })
                    }
                }
                if (isLockEntity) {
                    Text("Door sensor (turns lock card red when open)", style = MaterialTheme.typography.labelLarge)
                    val doorName = doorEntityId?.takeIf { it.isNotBlank() }
                        ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(doorName ?: "None", style = MaterialTheme.typography.bodySmall, color = if (doorName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showDoorPicker = true }) { Text("Change") }
                        if (!doorEntityId.isNullOrBlank()) { TextButton(onClick = { doorEntityId = null }) { Text("Clear") } }
                    }
                }
                if (isClimateEntity) {
                    Text("Temperature sensor (graphed in Activity)", style = MaterialTheme.typography.labelLarge)
                    val tempName = climateTempSensorEntityId?.takeIf { it.isNotBlank() }
                        ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tempName ?: "None", style = MaterialTheme.typography.bodySmall, color = if (tempName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showTempSensorPicker = true }) { Text("Change") }
                        if (!climateTempSensorEntityId.isNullOrBlank()) { TextButton(onClick = { climateTempSensorEntityId = null }) { Text("Clear") } }
                    }
                    Text("Humidity sensor (graphed in Activity)", style = MaterialTheme.typography.labelLarge)
                    val humidityName = climateHumiditySensorEntityId?.takeIf { it.isNotBlank() }
                        ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(humidityName ?: "None", style = MaterialTheme.typography.bodySmall, color = if (humidityName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showHumiditySensorPicker = true }) { Text("Change") }
                        if (!climateHumiditySensorEntityId.isNullOrBlank()) { TextButton(onClick = { climateHumiditySensorEntityId = null }) { Text("Clear") } }
                    }
                }
                if (!isVacuumItem) {
                    ActionChips("Tap", tapAction, actions) { tapAction = it }
                    ActionChips("Double", doubleAction, actions) { doubleAction = it }
                    ActionChips("Hold", holdAction, actions) { holdAction = it }
                }
                if (widgetAppearance != null) {
                    Text("Shape", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !appearIsSquare, onClick = { appearIsSquare = false }, label = { Text("Standard") })
                        FilterChip(selected = appearIsSquare, onClick = { appearIsSquare = true }, label = { Text("Square") })
                    }
                    Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = appearRadius == 8, onClick = { appearRadius = 8 }, label = { Text("Sharp") })
                        FilterChip(selected = appearRadius == 20, onClick = { appearRadius = 20 }, label = { Text("Modern") })
                        FilterChip(selected = appearRadius == 28, onClick = { appearRadius = 28 }, label = { Text("Round") })
                    }
                    WidgetWidthSelector(width = appearWidth, onWidthChange = { appearWidth = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val save: (HKIButtonConfig) -> Unit = { newConfig ->
                    if (widgetAppearance != null && onSaveWithAppearance != null) {
                        onSaveWithAppearance(
                            newConfig,
                            WidgetAppearance(appearIsSquare, appearRadius, appearWidth)
                        )
                    } else onSave(newConfig)
                }
                save(
                    config.copy(
                        name = name.ifBlank { null },
                        label = if (isVacuumItem) config.label else label.ifBlank { null },
                        icon = if (isCameraItem || isVacuumItem) config.icon else iconName.takeUnless { it == "None" },
                        spinIcon = if (isCameraItem || isVacuumItem) config.spinIcon else spinIcon,
                        cameraUrl = if (isCameraItem && config.isCustomUrl) cameraUrl.ifBlank { null } else config.cameraUrl,
                        cameraRefreshInterval = if (isCameraItem) refreshInterval else config.cameraRefreshInterval,
                        isCustomUrl = config.isCustomUrl,
                        tapAction = tapAction,
                        doubleTapAction = doubleAction,
                        holdAction = holdAction,
                        doorEntityId = if (isLockEntity) doorEntityId else config.doorEntityId,
                        vacuumDisplayMode = if (isVacuumItem) vacuumDisplayMode else config.vacuumDisplayMode,
                        vacuumMapEntityId = if (isVacuumItem) vacuumMapEntityId else config.vacuumMapEntityId,
                        vacuumBatteryEntityId = if (isVacuumItem) vacuumBatteryEntityId else config.vacuumBatteryEntityId,
                        vacuumImageUrl = if (isVacuumItem && vacuumDisplayMode == "external") vacuumImageUrl.ifBlank { null } else if (isVacuumItem) null else config.vacuumImageUrl,
                        climateTempSensorEntityId = if (isClimateEntity) climateTempSensorEntityId else config.climateTempSensorEntityId,
                        climateHumiditySensorEntityId = if (isClimateEntity) climateHumiditySensorEntityId else config.climateHumiditySensorEntityId
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    // ── Entity pickers hoisted AFTER the AlertDialog so they render on top of it ──
    if (showDoorPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("binary_sensor.") },
            title = "Select Door Sensor",
            singleSelect = true,
            preselectedIds = setOfNotNull(doorEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showDoorPicker = false },
            onEntitiesSelected = { ids -> doorEntityId = ids.firstOrNull(); showDoorPicker = false }
        )
    }
    if (showMapPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("camera.") },
            title = "Select Map Camera",
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumMapEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showMapPicker = false },
            onEntitiesSelected = { ids -> vacuumMapEntityId = ids.firstOrNull(); showMapPicker = false }
        )
    }
    if (showBattPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("sensor.") },
            title = "Select Battery Sensor",
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumBatteryEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showBattPicker = false },
            onEntitiesSelected = { ids -> vacuumBatteryEntityId = ids.firstOrNull(); showBattPicker = false }
        )
    }
    if (showTempSensorPicker) {
        val sensors = allEntities.filter { it.entity_id.startsWith("sensor.") }
        AdvancedEntitySearchDialog(
            allEntities = sensors.filter { it.deviceClass == "temperature" }.ifEmpty { sensors },
            title = "Select Temperature Sensor",
            singleSelect = true,
            preselectedIds = setOfNotNull(climateTempSensorEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showTempSensorPicker = false },
            onEntitiesSelected = { ids -> climateTempSensorEntityId = ids.firstOrNull(); showTempSensorPicker = false }
        )
    }
    if (showHumiditySensorPicker) {
        val sensors = allEntities.filter { it.entity_id.startsWith("sensor.") }
        AdvancedEntitySearchDialog(
            allEntities = sensors.filter { it.deviceClass == "humidity" }.ifEmpty { sensors },
            title = "Select Humidity Sensor",
            singleSelect = true,
            preselectedIds = setOfNotNull(climateHumiditySensorEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showHumiditySensorPicker = false },
            onEntitiesSelected = { ids -> climateHumiditySensorEntityId = ids.firstOrNull(); showHumiditySensorPicker = false }
        )
    }
}

@Composable
private fun ActionChips(label: String, selected: String, actions: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label action", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { action ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = { Text(action.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

@Composable
fun PagedRoleDialog(
    role: String,
    entities: List<HAEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    buttonConfigs: Map<String, HKIButtonConfig> = emptyMap()
) {
    var page by remember(entities.map { it.entity_id }) { mutableIntStateOf(0) }
    val entity = entities[page.coerceIn(0, entities.lastIndex)]
    var dragAmount by remember { mutableFloatStateOf(0f) }

    fun next() {
        if (entities.size > 1) page = (page + 1).coerceAtMost(entities.lastIndex)
    }

    fun previous() {
        if (entities.size > 1) page = (page - 1).coerceAtLeast(0)
    }

    val icon = when (role) {
        "climate" -> Icons.Default.Thermostat
        "lock" -> if (entity.state == "locked") Icons.Default.Lock else Icons.Default.LockOpen
        "cover" -> Icons.Default.Window
        "camera" -> Icons.Default.CameraAlt
        else -> Icons.Default.Power
    }
    val hvacTone = hvacColor(
        entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
            ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
            ?: entity.state
    )
    var selectedClimateMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { selectedClimateMode = entity.state }
    var showClimateModes by remember(entity.entity_id) { mutableStateOf(false) }
    val climateTabs = buildList {
        entity.hvacModes.ifEmpty { listOf("cool", "heat", "auto", "off") }.forEach { mode ->
            add(Triple(climateModeLabel(mode), climateModeIcon(mode), {
                selectedClimateMode = mode
                viewModel.setHvacMode(entity.entity_id, mode)
            }))
        }
    }
    var selectedCoverAction by remember(entity.entity_id) {
        mutableStateOf(
            when (entity.state) {
                "opening", "open" -> "Open"
                "closing", "closed" -> "Close"
                else -> "Stop"
            }
        )
    }
    val coverTabs = listOf(
        Triple("Open", Icons.Default.ArrowUpward, {
            selectedCoverAction = "Open"
            viewModel.controlCover(entity.entity_id, "open_cover")
        }),
        Triple("Stop", Icons.Default.Stop, {
            selectedCoverAction = "Stop"
            viewModel.controlCover(entity.entity_id, "stop_cover")
        }),
        Triple("Close", Icons.Default.ArrowDownward, {
            selectedCoverAction = "Close"
            viewModel.controlCover(entity.entity_id, "close_cover")
        })
    )

    val headerConfig = buttonConfigs[entity.entity_id]
    val climateConfig = buttonConfigs[entity.entity_id]
    val extraGraphEntityIds = if (role == "climate") {
        listOfNotNull(climateConfig?.climateTempSensorEntityId, climateConfig?.climateHumiditySensorEntityId)
    } else emptyList()

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = icon,
        iconTint = when (role) {
            "climate" -> hvacTone
            "cover" -> coverAccentColor(entity)
            else -> Color(0xFFFFA500)
        },
        titleOverride = headerConfig?.name,
        iconName = headerConfig?.icon?.takeUnless { it.isBlank() } ?: defaultEntityIconSlug(entity),
        spinIcon = headerConfig?.spinIcon == true,
        statusText = if (entities.size > 1) "${page + 1}/${entities.size} - ${entity.state.uppercase()}" else entity.state.uppercase(),
        extraGraphEntityIds = extraGraphEntityIds,
        tabs = when (role) {
            "climate" -> climateTabs.takeIf { it.size > 1 } ?: emptyList()
            "cover" -> coverTabs
            else -> emptyList()
        },
        currentTab = when (role) {
            "climate" -> climateModeLabel(selectedClimateMode)
            "cover" -> selectedCoverAction
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(entities.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmount > 80f) previous()
                            if (dragAmount < -80f) next()
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragAmount += amount
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (role) {
                    "climate" -> ClimateControlContent(entity, viewModel, showModes = showClimateModes, onToggleModes = { showClimateModes = it })
                    "lock" -> LockControlContent(entity, viewModel)
                    "cover" -> BlindControlContent(entity, viewModel)
                    "camera" -> CameraContent(entity, viewModel)
                }
            }
            if (entities.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    entities.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == page) 8.dp else 6.dp)
                                .background(if (index == page) Color.White else Color.Gray, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StackSettingsDialog(
    stack: HKIButtonStack,
    onDismiss: () -> Unit,
    onUpdate: (HKIButtonStack) -> Unit
) {
    var title by remember(stack) { mutableStateOf(stack.title ?: "") }
    var iconName by remember(stack) { mutableStateOf(stack.icon ?: "Lightbulb") }
    var width by remember(stack) { mutableStateOf(stack.width) }
    var columns by remember(stack) { mutableIntStateOf(stack.columns.coerceIn(1, 3)) }
    var showBadge by remember(stack) { mutableStateOf(stack.showBadge) }
    var isSquare by remember(stack) { mutableStateOf(stack.isSquare) }
    var cornerRadius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var defaultCollapsed by remember(stack) { mutableStateOf(stack.defaultCollapsed) }
    var cameraAspect by remember(stack) { mutableFloatStateOf(stack.cameraAspectRatio) }
    var showIconPickerStack by remember { mutableStateOf(false) }

    if (showIconPickerStack) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPickerStack = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPickerStack = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stack Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Stack Title") }, singleLine = true)
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (iconName != "None") {
                        MdiIcon(iconName, size = 24.dp)
                    }
                    Text(
                        iconName.takeUnless { it == "None" } ?: "None",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showIconPickerStack = true }) { Text("Change") }
                }
                Text("Columns", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { count ->
                        FilterChip(selected = columns == count, onClick = { columns = count }, label = { Text("$count") })
                    }
                }
                if (stack.stackType != "weather") {
                    Text("Shape", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                    }
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cornerRadius == 8, onClick = { cornerRadius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = cornerRadius == 20, onClick = { cornerRadius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = cornerRadius == 28, onClick = { cornerRadius = 28 }, label = { Text("Round") })
                }
                if (stack.stackType !in listOf("camera", "weather")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showBadge, onCheckedChange = { showBadge = it })
                        Text("Show activity badge")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text("Collapsible")
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text("Collapsed by default")
                    }
                }
                if (stack.stackType == "camera") {
                    Text("Camera aspect ratio", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16:9" to (16f / 9f), "4:3" to (4f / 3f), "1:1" to 1f).forEach { (label, value) ->
                            FilterChip(
                                selected = cameraAspect == value,
                                onClick = { cameraAspect = value },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(
                        stack.copy(
                            title = title.ifBlank { null },
                            width = width,
                            icon = iconName.takeUnless { it == "None" },
                            columns = columns.coerceIn(1, 3),
                            showBadge = if (stack.stackType in listOf("camera", "weather")) false else showBadge,
                            isSquare = if (stack.stackType == "weather") false else isSquare,
                            cornerRadius = cornerRadius,
                            collapsible = collapsible,
                            defaultCollapsed = defaultCollapsed,
                            isCollapsed = defaultCollapsed,
                            cameraAspectRatio = cameraAspect
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ButtonStackItem(
    stack: HKIButtonStack,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    currentUrl: String = "",
    accessToken: String? = null,
    isEditMode: Boolean,
    onEntityClick: (String) -> Unit,
    onEntityDoubleClick: (String) -> Unit,
    onEntityLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onDeleteClick: () -> Unit,
    onHideClick: () -> Unit,
    onAddClick: () -> Unit,
    onButtonSettings: (String) -> Unit,
    onRemoveEntity: (String) -> Unit,
    onCameraClick: (String) -> Unit = {},
    onBadgeClick: () -> Unit = {},
    onReorderEntities: (Int, Int) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    if (stack.isHidden && !isEditMode) return
    val entityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }
    val entities = remember(stack.entityIds, entityById) { stack.entityIds.mapNotNull(entityById::get) }
    val buttonConfigs = stack.buttonConfigs
    // Domain-aware "active" so the activity badge works for locks/covers/etc, not just lights.
    val activeCount = remember(entities) {
        entities.count { e ->
            val s = e.state.lowercase()
            when (e.entity_id.substringBefore(".")) {
                "lock"    -> s != "locked" && s != "unavailable"
                "cover"   -> s != "closed" && s != "unavailable"
                "climate" -> s != "off" && s != "unavailable"
                "vacuum"  -> s == "cleaning" || s == "returning" || s == "paused" || s == "error"
                "person"  -> s == "home"
                else      -> s == "on"
            }
        }
    }
    val canCollapse = stack.collapsible
    val isCollapsed = canCollapse && (stack.isCollapsed ?: stack.defaultCollapsed)
    Column(modifier = Modifier.fillMaxWidth()) {
        val hasLabel = !stack.title.isNullOrBlank() || !stack.icon.isNullOrBlank()
        // Show the collapse chevron when the stack can collapse and has a header anchor.
        // Also show it in edit mode so large stacks can be folded away while reordering.
        // If collapsed, keep the chevron visible so it can always be expanded.
        val showChevron = canCollapse && (hasLabel || isEditMode || isCollapsed)
        val showHeaderLabel = hasLabel || isEditMode || isCollapsed || (stack.showBadge && activeCount > 0)
        if (showHeaderLabel) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = showChevron) { onToggleCollapsed() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                if (!stack.icon.isNullOrBlank()) {
                MdiIcon(stack.icon, tint = Color.Gray, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                }
                if (!stack.title.isNullOrBlank()) {
                    Text(stack.title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
                if (showChevron) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expand stack" else "Collapse stack",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (stack.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text("Hidden", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                }

                if (isEditMode) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (stack.isHidden) "Unhide stack" else "Hide stack",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add entity", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                } else if (stack.showBadge && activeCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.clickable { onBadgeClick() }
                    ) {
                        Text(
                            "$activeCount",
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!isCollapsed) {
            if (stack.stackType in listOf("single_button", "single_camera", "single_vacuum")) {
                val entity = entities.firstOrNull()
                if (entity == null) {
                    EmptyStackHint(isEditMode = isEditMode, onAdd = onAddClick)
                    return@Column
                }
                val cfg = buttonConfigs[entity.entity_id]
                when (stack.stackType) {
                    "single_camera" -> {
                        CameraStackContent(
                            stack = stack.copy(columns = 1),
                            entities = listOf(entity),
                            currentUrl = currentUrl,
                            accessToken = accessToken,
                            isEditMode = isEditMode,
                            onEntityClick = onCameraClick,
                            onButtonSettings = onButtonSettings,
                            onRemoveEntity = onRemoveEntity,
                            onReorderEntities = onReorderEntities
                        )
                    }
                    "single_vacuum" -> {
                        VacuumEntityCard(
                            entity = entity,
                            config = cfg,
                            allEntities = allEntities,
                            currentUrl = currentUrl,
                            isSquare = stack.isSquare,
                            cornerRadius = stack.cornerRadius,
                            aspectRatio = stack.cameraAspectRatio,
                            onClick = { onEntityClick(entity.entity_id) }
                        )
                    }
                    else -> {
                        val doorOpen = cfg?.doorEntityId?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
                        EntityCard(
                            entity = entity,
                            displayName = cfg?.name,
                            label = cfg?.label,
                            iconName = cfg?.icon,
                            spinIcon = cfg?.spinIcon == true,
                            onClick = { onEntityClick(entity.entity_id) },
                            onLongClick = { onEntityLongClick(entity.entity_id) },
                            onDoubleClick = { onEntityDoubleClick(entity.entity_id) },
                            isSquare = stack.isSquare,
                            cornerRadius = stack.cornerRadius,
                            doorOpen = doorOpen
                        )
                    }
                }
                return@Column
            }
            if (stack.stackType !in listOf("camera", "vacuum", "weather") && entities.isEmpty()) {
                EmptyStackHint(isEditMode = isEditMode, onAdd = onAddClick)
                return@Column
            }
            if (stack.stackType == "weather") {
                WeatherStackContent(
                    stack = stack,
                    allEntities = allEntities,
                    viewModel = viewModel,
                    isEditMode = isEditMode,
                    onItemSettings = onButtonSettings,
                    onRemoveItem = onRemoveEntity,
                    onReorder = onReorderEntities
                )
                return@Column
            }
            if (stack.stackType == "camera") {
                CameraStackContent(
                    stack = stack,
                    entities = entities,
                    currentUrl = currentUrl,
                    accessToken = accessToken,
                    isEditMode = isEditMode,
                    onEntityClick = onCameraClick,
                    onButtonSettings = onButtonSettings,
                    onRemoveEntity = onRemoveEntity,
                    onReorderEntities = onReorderEntities
                )
                return@Column
            }
            if (stack.stackType == "vacuum") {
                VacuumStackContent(
                    stack = stack,
                    entities = entities,
                    allEntities = allEntities,
                    currentUrl = currentUrl,
                    isEditMode = isEditMode,
                    onEntityClick = onEntityClick,
                    onButtonSettings = onButtonSettings,
                    onRemoveEntity = onRemoveEntity,
                    onReorderEntities = onReorderEntities
                )
                return@Column
            }
            if (!isEditMode) {
                val columns = stack.columns.coerceIn(1, 3)
                val entityRows = remember(entities, columns) { entities.chunked(columns) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entityRows.forEach { rowEntities ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowEntities.forEach { entity ->
                                val cfg = buttonConfigs[entity.entity_id]
                                val doorOpen = cfg?.doorEntityId?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
                                EntityCard(
                                    entity = entity,
                                    displayName = cfg?.name,
                                    label = cfg?.label,
                                    iconName = cfg?.icon,
                                    spinIcon = cfg?.spinIcon == true,
                                    onClick = { onEntityClick(entity.entity_id) },
                                    onLongClick = { onEntityLongClick(entity.entity_id) },
                                    onDoubleClick = { onEntityDoubleClick(entity.entity_id) },
                                    isSquare = stack.isSquare,
                                    cornerRadius = stack.cornerRadius,
                                    doorOpen = doorOpen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat((columns - rowEntities.size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                val columns = stack.columns.coerceIn(1, 3)
                ReorderableGrid(
                    items = entities,
                    canReorder = true,
                    onReorder = { from, to -> onReorderEntities(from, to) },
                    key = { it.entity_id },
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    isNested = true,
                    modifier = Modifier.heightIn(max = 2000.dp).fillMaxWidth()
                ) { entity, _ ->
                    Box {
                        EntityCard(
                            entity = entity,
                            displayName = buttonConfigs[entity.entity_id]?.name,
                            label = buttonConfigs[entity.entity_id]?.label,
                            iconName = buttonConfigs[entity.entity_id]?.icon,
                            spinIcon = buttonConfigs[entity.entity_id]?.spinIcon == true,
                            onClick = { onEntityClick(entity.entity_id) },
                            onLongClick = { onEntityLongClick(entity.entity_id) },
                            onDoubleClick = { onEntityDoubleClick(entity.entity_id) },
                            isSquare = stack.isSquare,
                            cornerRadius = stack.cornerRadius,
                            interactionsEnabled = false
                        )
                        IconButton(
                            onClick = { onButtonSettings(entity.entity_id) },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Button settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                        }
                        EditRemoveBadge(
                            onClick = { onRemoveEntity(entity.entity_id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleEntityWidgetItem(
    widget: HKISingleEntityWidget,
    allEntities: List<HAEntity>,
    currentUrl: String,
    accessToken: String? = null,
    isEditMode: Boolean,
    onEntityClick: (String) -> Unit,
    onEntityDoubleClick: (String) -> Unit,
    onEntityLongClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onHideClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val appColors = LocalHKIAppColors.current
    if (widget.isHidden && !isEditMode) return
    val entity = allEntities.find { it.entity_id == widget.entityId }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (entity == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(widget.cornerRadius.dp),
                color = appColors.subtleSurface,
                border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Text(
                    "Entity unavailable",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(18.dp)
                )
            }
            return@Column
        }

        Box {
            when (widget.kind) {
                "camera" -> {
                    val stack = HKIButtonStack(
                        id = widget.id,
                        entityIds = listOf(widget.entityId),
                        columns = 1,
                        isSquare = widget.isSquare,
                        cornerRadius = widget.cornerRadius,
                        stackType = "camera",
                        cameraAspectRatio = widget.cameraAspectRatio,
                        buttonConfigs = mapOf(widget.entityId to widget.config)
                    )
                    CameraStackContent(
                        stack = stack,
                        entities = listOf(entity),
                        currentUrl = currentUrl,
                        accessToken = accessToken,
                        isEditMode = false,
                        onEntityClick = { onEntityClick(widget.entityId) },
                        onButtonSettings = {},
                        onRemoveEntity = {},
                        onReorderEntities = { _, _ -> }
                    )
                }
                "vacuum" -> VacuumEntityCard(
                    entity = entity,
                    config = widget.config,
                    allEntities = allEntities,
                    currentUrl = currentUrl,
                    isSquare = widget.isSquare,
                    cornerRadius = widget.cornerRadius,
                    aspectRatio = widget.cameraAspectRatio,
                    onClick = { onEntityClick(widget.entityId) }
                )
                else -> {
                    val doorOpen = widget.config.doorEntityId?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
                    EntityCard(
                        entity = entity,
                        displayName = widget.config.name,
                        label = widget.config.label,
                        iconName = widget.config.icon,
                        spinIcon = widget.config.spinIcon,
                        onClick = { onEntityClick(widget.entityId) },
                        onLongClick = { onEntityLongClick(widget.entityId) },
                        onDoubleClick = { onEntityDoubleClick(widget.entityId) },
                        isSquare = widget.isSquare,
                        cornerRadius = widget.cornerRadius,
                        doorOpen = doorOpen,
                        interactionsEnabled = !isEditMode
                    )
                }
            }
            if (isEditMode) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                }
                EditRemoveBadge(
                    onClick = onDeleteClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipingStackItem(
    stack: HKISwipingStack,
    isEditMode: Boolean,
    onSettingsClick: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onDeleteClick: () -> Unit,
    onHideClick: () -> Unit,
    onAddClick: () -> Unit,
    content: @Composable (HKIRoomWidget) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    if (stack.isHidden && !isEditMode) return
    val canCollapse = stack.collapsible
    val isCollapsed = canCollapse && (stack.isCollapsed ?: stack.defaultCollapsed)
    val pagerState = rememberPagerState(pageCount = { stack.widgets.size.coerceAtLeast(1) })
    val animationType = stack.animation.takeIf { type -> swipingStackAnimationTypes.any { it.first == type } } ?: "swipe"
    val duration = when (animationType) {
        "instant" -> 0
        "fade" -> 500
        "zoom" -> 550
        "slide_fade" -> 500
        "cube" -> 650
        else -> 450
    }

    LaunchedEffect(stack.autoplay, stack.autoplayIntervalSeconds, animationType, stack.widgets.size, isEditMode) {
        if (!stack.autoplay || isEditMode || stack.widgets.size < 2) return@LaunchedEffect
        while (true) {
            delay(stack.autoplayIntervalSeconds.coerceIn(1, 120).seconds)
            val nextPage = (pagerState.currentPage + 1) % stack.widgets.size
            if (animationType == "instant") pagerState.scrollToPage(nextPage)
            else pagerState.animateScrollToPage(nextPage, animationSpec = tween(durationMillis = duration))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isEditMode || isCollapsed || stack.isHidden) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = canCollapse) { onToggleCollapsed() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Swiping Stack", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    if (canCollapse) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isCollapsed) "Expand stack" else "Collapse stack",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (stack.isHidden) {
                        Spacer(Modifier.width(8.dp))
                        Text("Hidden", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (isEditMode) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (stack.isHidden) "Unhide stack" else "Hide stack",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add widget", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!isCollapsed) {
            if (stack.widgets.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditMode) { onAddClick() },
                    shape = RoundedCornerShape(stack.cornerRadius.dp),
                    color = appColors.subtleSurface,
                    border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Tap here or use the + button while edit mode is active to add widgets to this stack.",
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        pageSpacing = 12.dp
                    ) { page ->
                        val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    when (animationType) {
                                        "fade" -> {
                                            alpha = 1f - (pageOffset * 0.65f)
                                        }
                                        "zoom" -> {
                                            val scale = 0.86f + ((1f - pageOffset) * 0.14f)
                                            alpha = 1f - (pageOffset * 0.45f)
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        "slide_fade" -> {
                                            alpha = 1f - (pageOffset * 0.55f)
                                            translationX = rawOffset * size.width * 0.08f
                                        }
                                        "cube" -> {
                                            alpha = 1f - (pageOffset * 0.25f)
                                            rotationY = rawOffset * -55f
                                            cameraDistance = 18f
                                        }
                                    }
                                }
                        ) {
                            content(stack.widgets[page])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipingStackSettingsDialog(
    stack: HKISwipingStack,
    onDismiss: () -> Unit,
    onUpdate: (HKISwipingStack) -> Unit
) {
    var width by remember(stack) { mutableStateOf(stack.width) }
    var isSquare by remember(stack) { mutableStateOf(stack.isSquare) }
    var cornerRadius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var defaultCollapsed by remember(stack) { mutableStateOf(stack.defaultCollapsed) }
    var autoplay by remember(stack) { mutableStateOf(stack.autoplay) }
    var intervalText by remember(stack) { mutableStateOf(stack.autoplayIntervalSeconds.toString()) }
    var animation by remember(stack) {
        mutableStateOf(stack.animation.takeIf { type -> swipingStackAnimationTypes.any { it.first == type } } ?: "swipe")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Swiping Stack Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cornerRadius == 8, onClick = { cornerRadius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = cornerRadius == 20, onClick = { cornerRadius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = cornerRadius == 28, onClick = { cornerRadius = 28 }, label = { Text("Round") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text("Collapsible")
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text("Collapsed by default")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoplay, onCheckedChange = { autoplay = it })
                    Text("Autoplay")
                }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Interval seconds") },
                    singleLine = true
                )
                Text("Animation", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    swipingStackAnimationTypes.forEach { (value, label) ->
                        FilterChip(
                            selected = animation == value,
                            onClick = { animation = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(
                    stack.copy(
                        width = width,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        collapsible = collapsible,
                        defaultCollapsed = defaultCollapsed,
                        isCollapsed = defaultCollapsed,
                        autoplay = autoplay,
                        autoplayIntervalSeconds = intervalText.toIntOrNull()?.coerceIn(1, 120) ?: 5,
                        animation = animation
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyStackItem(
    stack: HKIEmptyStack,
    isEditMode: Boolean,
    onSettingsClick: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onDeleteClick: () -> Unit,
    onHideClick: () -> Unit,
    onAddClick: () -> Unit,
    content: @Composable (HKIRoomWidget, Modifier) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    if (stack.isHidden && !isEditMode) return
    val isCollapsed = stack.collapsible && (stack.isCollapsed ?: stack.defaultCollapsed)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = stack.collapsible) { onToggleCollapsed() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Empty Stack", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                if (stack.collapsible) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expand stack" else "Collapse stack",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (stack.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text("Hidden", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isEditMode) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (stack.isHidden) "Unhide stack" else "Hide stack",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add widget", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!isCollapsed) {
            if (stack.widgets.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditMode) { onAddClick() },
                    shape = RoundedCornerShape(stack.cornerRadius.dp),
                    color = appColors.subtleSurface,
                    border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Tap here or use the + button while edit mode is active to add widgets to this stack.",
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val columns = stack.columns.coerceIn(1, 3)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stack.widgets.chunked(columns).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { child ->
                                content(child, Modifier.weight(1f))
                            }
                            repeat((columns - row.size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStackSettingsDialog(
    stack: HKIEmptyStack,
    onDismiss: () -> Unit,
    onUpdate: (HKIEmptyStack) -> Unit
) {
    var width by remember(stack) { mutableStateOf(stack.width) }
    var columns by remember(stack) { mutableIntStateOf(stack.columns.coerceIn(1, 3)) }
    var showBadge by remember(stack) { mutableStateOf(stack.showBadge) }
    var isSquare by remember(stack) { mutableStateOf(stack.isSquare) }
    var cornerRadius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var defaultCollapsed by remember(stack) { mutableStateOf(stack.defaultCollapsed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Empty Stack Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Columns", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { count ->
                        FilterChip(selected = columns == count, onClick = { columns = count }, label = { Text("$count") })
                    }
                }
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cornerRadius == 8, onClick = { cornerRadius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = cornerRadius == 20, onClick = { cornerRadius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = cornerRadius == 28, onClick = { cornerRadius = 28 }, label = { Text("Round") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBadge, onCheckedChange = { showBadge = it })
                    Text("Show activity badge")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text("Collapsible")
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text("Collapsed by default")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(
                    stack.copy(
                        width = width,
                        columns = columns.coerceIn(1, 3),
                        showBadge = showBadge,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        collapsible = collapsible,
                        defaultCollapsed = defaultCollapsed,
                        isCollapsed = defaultCollapsed
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun EmptyStackHint(
    isEditMode: Boolean = false,
    onAdd: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEditMode && onAdd != null) { onAdd?.invoke() },
        shape = RoundedCornerShape(22.dp),
        color = appColors.subtleSurface,
        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Tap here or use the + button while edit mode is active to add entities to this stack.",
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CameraStackContent(
    stack: HKIButtonStack,
    entities: List<HAEntity>,
    currentUrl: String,
    accessToken: String?,
    isEditMode: Boolean,
    onEntityClick: (String) -> Unit,
    onButtonSettings: (String) -> Unit,
    onRemoveEntity: (String) -> Unit,
    onReorderEntities: (Int, Int) -> Unit
) {
    val entityById = remember(entities) { entities.associateBy { it.entity_id } }
    val cameraSources = remember(stack.entityIds, stack.buttonConfigs, entityById, currentUrl) {
        stack.entityIds.mapNotNull { entityId ->
            val entity = entityById[entityId]
            val config = stack.buttonConfigs[entityId]
            val refreshInterval = config?.cameraRefreshInterval ?: 0
            val fallbackEntityUrl = if (entityId.startsWith("camera.")) {
                val path = if (refreshInterval == 0) "camera_proxy_stream" else "camera_proxy"
                "${currentUrl.removeSuffix("/")}/api/$path/$entityId"
            } else {
                null
            }
            val rawUrl = when {
                config?.cameraUrl?.isNotBlank() == true -> config.cameraUrl
                entity != null -> resolveEntityCameraUrl(entity, currentUrl, preferLive = refreshInterval == 0)
                fallbackEntityUrl != null -> fallbackEntityUrl
                else -> null
            }
            val resolvedUrl = resolveCameraUrl(rawUrl, currentUrl) ?: return@mapNotNull null
            val label = config?.name ?: entity?.friendlyName ?: if (config?.isCustomUrl == true) "Custom Camera" else entityId
            val liveWebUrl = when {
                refreshInterval > 0 -> null
                entity != null -> resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
                else -> buildWebRtcApiUrl(config?.cameraUrl, currentUrl)
            }
            CameraSourceItem(
                id = entityId,
                label = label,
                imageUrl = resolvedUrl,
                refreshIntervalSeconds = refreshInterval,
                liveWebUrl = liveWebUrl
            )
        }
    }

    if (cameraSources.isEmpty()) {
        val aspectRatio = if (stack.isSquare) 1f else stack.cameraAspectRatio
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
            shape = RoundedCornerShape(stack.cornerRadius.dp),
            color = Color.Black
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No camera selected", color = Color.Gray)
            }
        }
        return
    }

    if (isEditMode) {
        ReorderableGrid(
            items = cameraSources,
            canReorder = true,
            onReorder = onReorderEntities,
            key = { it.id },
            columns = GridCells.Fixed(stack.columns.coerceIn(1, 3)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            isNested = true,
            modifier = Modifier.heightIn(max = 2000.dp).fillMaxWidth()
        ) { source, _ ->
            CameraStackCard(
                source = source,
                stack = stack,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = onEntityClick,
                onButtonSettings = onButtonSettings,
                onRemoveEntity = onRemoveEntity
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cameraSources.chunked(stack.columns.coerceAtLeast(1)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { source ->
                    CameraStackCard(
                        source = source,
                        stack = stack,
                        accessToken = accessToken,
                        isEditMode = isEditMode,
                        onEntityClick = onEntityClick,
                        onButtonSettings = onButtonSettings,
                        onRemoveEntity = onRemoveEntity,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((stack.columns - row.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        }
    }
}

private data class CameraSourceItem(
    val id: String,
    val label: String,
    val imageUrl: String,
    val refreshIntervalSeconds: Int,
    val liveWebUrl: String?
)

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CameraStackCard(
    source: CameraSourceItem,
    stack: HKIButtonStack,
    accessToken: String?,
    isEditMode: Boolean,
    onEntityClick: (String) -> Unit,
    onButtonSettings: (String) -> Unit,
    onRemoveEntity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    var refreshTick by remember(source.id, source.refreshIntervalSeconds, source.imageUrl) { mutableIntStateOf(0) }
    LaunchedEffect(source.id, source.refreshIntervalSeconds, source.imageUrl, source.liveWebUrl) {
        refreshTick = 0
        if (source.refreshIntervalSeconds > 0 && source.liveWebUrl.isNullOrBlank()) {
            while (true) {
                delay(source.refreshIntervalSeconds.seconds)
                refreshTick += 1
            }
        }
    }

    val model = remember(source.imageUrl, refreshTick, source.refreshIntervalSeconds) {
        buildCameraRefreshModel(source.imageUrl, source.refreshIntervalSeconds, refreshTick)
    }
    var lastSuccessfulModel by remember(source.id) { mutableStateOf(model) }
    val aspectRatio = if (stack.isSquare) 1f else stack.cameraAspectRatio

    Surface(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clickable(enabled = !isEditMode) { onEntityClick(source.id) },
        shape = RoundedCornerShape(stack.cornerRadius.dp),
        color = Color.Black
    ) {
        Box {
            if (!source.liveWebUrl.isNullOrBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            webViewClient = WebViewClient()
                            val headers = if (!accessToken.isNullOrBlank()) {
                                mapOf("Authorization" to "Bearer $accessToken")
                            } else {
                                emptyMap()
                            }
                            loadUrl(source.liveWebUrl, headers)
                        }
                    },
                    update = { webView ->
                        if (webView.url != source.liveWebUrl) {
                            val headers = if (!accessToken.isNullOrBlank()) {
                                mapOf("Authorization" to "Bearer $accessToken")
                            } else {
                                emptyMap()
                            }
                            webView.loadUrl(source.liveWebUrl, headers)
                        }
                    }
                )
            } else if (model != null) {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = source.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    loading = {
                        val fallback = lastSuccessfulModel
                        if (!fallback.isNullOrBlank()) {
                            AsyncImage(
                                model = fallback,
                                contentDescription = source.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    },
                    success = {
                        lastSuccessfulModel = model
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Stream Available", color = Color.Gray)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(source.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (source.refreshIntervalSeconds > 0) "${source.refreshIntervalSeconds}s refresh" else "Live",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (isEditMode) {
                IconButton(
                    onClick = { onButtonSettings(source.id) },
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Button settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                }
                EditRemoveBadge(
                    onClick = { onRemoveEntity(source.id) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

/**
 * The temperature slider and fan/swing modes list share one top-anchored layout.
 * This keeps the modes toggle button in the same spot across views.
 * The middle region absorbs size differences via Modifier.weight.
 */
@Composable
private fun ClimateControlContent(entity: HAEntity, viewModel: MainViewModel, showModes: Boolean, onToggleModes: (Boolean) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val minTemp = entity.attributes?.get("min_temp")?.jsonPrimitive?.doubleOrNull ?: 15.0
    val maxTemp = entity.attributes?.get("max_temp")?.jsonPrimitive?.doubleOrNull ?: 30.0
    val targetTemp = entity.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull ?: 21.0
    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull

    var localTarget by remember(targetTemp) { mutableFloatStateOf(targetTemp.toFloat()) }
    var localMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { localMode = entity.state }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (showModes) 32.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (showModes) "Modes" else String.format(Locale.getDefault(), "%.1f\u00B0", localTarget),
            color = appColors.onSurface,
            style = if (showModes) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showModes) {
                ClimateModesList(entity, viewModel)
            } else {
                VerticalSlider(
                    value = ((localTarget - minTemp.toFloat()) / (maxTemp.toFloat() - minTemp.toFloat())).coerceIn(0f, 1f),
                    onValueChange = { localTarget = minTemp.toFloat() + it * (maxTemp.toFloat() - minTemp.toFloat()) },
                    onValueChangeFinished = { viewModel.setClimateTemp(entity.entity_id, localTarget) },
                    gradient = hvacModeGradient(hvacAction ?: localMode)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (showModes) "MODES" else "TARGET", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        ClimateModesButton(entity, onClick = { onToggleModes(!showModes) }, selected = showModes)
    }
}

private fun hvacModeGradient(mode: String): Brush {
    val color = hvacColor(mode)
    return Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color))
}

@Composable
private fun LockControlContent(entity: HAEntity, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (entity.state == "locked") "Door\nLocked" else "Door\nOpen",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VerticalMasterSwitch(
                isOn = entity.state == "locked",
                onToggle = { viewModel.toggleLock(entity.entity_id) }
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("LOCK", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BlindControlContent(entity: HAEntity, viewModel: MainViewModel, activeColor: Color = coverAccentColor(entity)) {
    val appColors = LocalHKIAppColors.current
    val currentPosition = entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0
    var localPosition by remember(currentPosition) { mutableFloatStateOf(currentPosition / 100f) }
    val hasTilt = entity.supportsTilt
    var localTilt by remember(entity.tiltPosition) { mutableFloatStateOf((entity.tiltPosition ?: 0) / 100f) }
    val trackColor = activeColor.copy(alpha = 0.18f)
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${(localPosition * 100).toInt()}%",
            color = appColors.onSurface,
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VerticalSlider(
                    value = localPosition,
                    onValueChange = { localPosition = it },
                    onValueChangeFinished = { viewModel.setCoverPosition(entity.entity_id, (localPosition * 100).toInt()) },
                    activeColor = activeColor,
                    trackColor = trackColor
                )
                if (hasTilt) {
                    CoverTiltSlider(
                        value = localTilt,
                        onValueChange = {
                            localTilt = it
                            viewModel.setOptimisticCoverTilt(entity.entity_id, (it * 100).toInt())
                        },
                        onValueChangeFinished = { viewModel.setCoverTiltPosition(entity.entity_id, (localTilt * 100).toInt()) },
                        activeColor = activeColor
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (hasTilt) 44.dp else 0.dp)) {
            Text("POSITION", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            if (hasTilt) Text("TILT", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Multi-entity cover dialog for badges. Swipe between covers; when a cover is flagged as a "door"
 * the icon and slider reflect state color (green closed / red open / orange moving).
 */
@Composable
fun AggregatedCoverDialog(
    entities: List<HAEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    iconName: String? = null,
    spinIcon: Boolean = false
) {
    if (entities.isEmpty()) { onDismiss(); return }
    var page by remember(entities.map { it.entity_id }) { mutableIntStateOf(0) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val entity = entities[page.coerceIn(0, entities.lastIndex)]
    val pos = entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0
    val accent = coverAccentColor(entity)

    var selectedAction by remember(entity.entity_id) {
        mutableStateOf(when (entity.state) { "opening", "open" -> "Open"; "closing", "closed" -> "Close"; else -> "Stop" })
    }
    val tabs = listOf(
        Triple("Open", Icons.Default.ArrowUpward, { selectedAction = "Open"; viewModel.controlCover(entity.entity_id, "open_cover") }),
        Triple("Stop", Icons.Default.Stop, { selectedAction = "Stop"; viewModel.controlCover(entity.entity_id, "stop_cover") }),
        Triple("Close", Icons.Default.ArrowDownward, { selectedAction = "Close"; viewModel.controlCover(entity.entity_id, "close_cover") })
    )
    val status = if (entities.size > 1) "${page + 1}/${entities.size} - ${pos}% - ${entity.state.uppercase()}"
                 else "${pos}% - ${entity.state.uppercase()}"

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Window,
        iconTint = accent,
        iconName = iconName?.takeUnless { it.isBlank() } ?: defaultEntityIconSlug(entity),
        spinIcon = spinIcon,
        statusText = status,
        tabs = tabs,
        currentTab = selectedAction
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(entities.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmount > 80f && page > 0) page--
                            if (dragAmount < -80f && page < entities.lastIndex) page++
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, amount -> change.consume(); dragAmount += amount }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BlindControlContent(entity, viewModel, activeColor = accent)
            }
            if (entities.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    entities.indices.forEach { i ->
                        Box(modifier = Modifier.size(if (i == page) 8.dp else 6.dp)
                            .background(if (i == page) Color.White else Color.Gray, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraContent(entity: HAEntity, viewModel: MainViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val streamUrl = entity.attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull?.let {
        if (it.startsWith("http")) it else "$currentUrl$it"
    }
    CameraFrame(streamUrl)
}

@Composable
private fun CameraFrame(streamUrl: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp).aspectRatio(16 / 9f),
        shape = RoundedCornerShape(24.dp),
        color = Color.Black
    ) {
        if (streamUrl != null) {
            AsyncImage(
                model = streamUrl,
                contentDescription = "Camera Feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Stream Available", color = Color.Gray)
            }
        }
    }
}

/**
 * Body of the "show group" view embedded inside an entity's own [HKIDialog] (like the History
 * toggle) so siblings can be browsed/controlled without leaving that dialog window.
 */
@Composable
fun GroupMembersContent(
    entities: List<HAEntity>,
    viewModel: MainViewModel
) {
    val appColors = LocalHKIAppColors.current
    val lightEntities = remember(entities) { entities.filter { it.entity_id.startsWith("light.") } }
    val hasColorTemp = remember(lightEntities) { lightEntities.any { it.supportsColorTemp } }
    val hasColor = remember(lightEntities) { lightEntities.any { it.supportsColor } }

    val tabs = remember(lightEntities, hasColorTemp, hasColor) {
        buildList {
            add("Power" to Icons.Default.Power)
            if (lightEntities.isNotEmpty()) add("Bright" to Icons.Default.LightMode)
            if (hasColorTemp) add("Temp" to Icons.Default.Thermostat)
            if (hasColor) add("Color" to Icons.Default.Palette)
        }
    }
    var currentTab by remember(entities.map { it.entity_id }) { mutableStateOf(if (lightEntities.isNotEmpty()) "Bright" else "Power") }

    fun labelFor(entity: HAEntity) = entity.friendlyName ?: entity.entity_id
    @Composable
    fun tintFor(entity: HAEntity): Color {
        val isOn = entity.state == "on"
        return if (isOn) lightStateColor(entity) ?: MaterialTheme.colorScheme.primary else appColors.onMuted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (tabs.size > 1) 88.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentTab) {
                "Bright" -> items(lightEntities.size) { index ->
                    val entity = lightEntities[index]
                    GroupLightControlRow(label = labelFor(entity), icon = defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                        HorizontalLightBar(
                            entity = entity,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
                "Temp" -> {
                    val list = lightEntities.filter { it.supportsColorTemp }
                    items(list.size) { index ->
                        val entity = list[index]
                        GroupLightControlRow(label = labelFor(entity), icon = defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                            HorizontalColorTempBar(
                                entity = entity,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }
                    }
                }
                "Color" -> {
                    val list = lightEntities.filter { it.supportsColor }
                    items(list.size) { index ->
                        val entity = list[index]
                        GroupLightControlRow(label = labelFor(entity), icon = defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                            HorizontalHueBar(
                                entity = entity,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }
                    }
                }
                else -> items(entities.size) { index ->
                    val entity = entities[index]
                    val isOn = entity.state == "on"
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = appColors.subtleSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MdiIcon(defaultEntityIconSlug(entity), contentDescription = null, tint = tintFor(entity), size = 24.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                val rowTextColor = appColors.onSurface
                                Text(labelFor(entity), color = rowTextColor, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (isOn) "On" else "Off",
                                    color = rowTextColor,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleEntity(entity.entity_id) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                            ) {
                                Icon(
                                    if (isOn) Icons.Default.Power else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tabs.size > 1) {
            GroupDialogTabBar(
                tabs = tabs,
                currentTab = currentTab,
                onSelect = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun GroupDialogTabBar(
    tabs: List<Pair<String, ImageVector>>,
    currentTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val denseTabs = tabs.size > 5
    HKIBottomBar(
        modifier = modifier,
        horizontalPadding = if (denseTabs) 16.dp else 32.dp,
        scrollable = false
    ) {
        tabs.forEach { (tabLabel, tabIcon) ->
            val isSelected = currentTab == tabLabel
            Column(
                modifier = Modifier.weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(tabLabel) }
                    .padding(vertical = if (denseTabs) 10.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tabIcon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                    modifier = Modifier.size(if (denseTabs) 22.dp else 24.dp)
                )
                Text(
                    tabLabel,
                    color = if (isSelected) appColors.onSurface else appColors.onMuted,
                    style = if (denseTabs) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GroupLightControlRow(label: String, icon: String?, iconTint: Color, content: @Composable () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = appColors.subtleSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MdiIcon(icon, tint = iconTint, size = 24.dp)
                Spacer(Modifier.width(12.dp))
                Text(label, color = appColors.onSurface, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun GroupEntityDialog(
    stack: HKIButtonStack,
    entities: List<HAEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    fun isActive(e: HAEntity): Boolean {
        val s = e.state.lowercase()
        return when (e.entity_id.substringBefore(".")) {
            "lock"    -> s != "locked" && s != "unavailable"
            "cover"   -> s != "closed" && s != "unavailable"
            "climate" -> s != "off"    && s != "unavailable"
            "vacuum"  -> s == "cleaning" || s == "returning" || s == "paused" || s == "error"
            "person"  -> s == "home"
            else      -> s == "on"
        }
    }
    val displayEntities = entities.filter { isActive(it) }
    val activeCount = displayEntities.size
    // Auto-dismiss when every entity in the group has been turned off.
    LaunchedEffect(displayEntities.isEmpty()) {
        if (displayEntities.isEmpty()) onDismiss()
    }
    fun turnOff(entity: HAEntity) {
        when (val dom = entity.entity_id.substringBefore(".")) {
            "climate" -> viewModel.setHvacMode(entity.entity_id, "off")
            "cover"   -> viewModel.callService("cover", "close_cover", HAServiceCall(entity_id = entity.entity_id))
            else      -> viewModel.callService(dom, "turn_off", HAServiceCall(entity_id = entity.entity_id))
        }
    }

    val lightEntities = remember(displayEntities) { displayEntities.filter { it.entity_id.startsWith("light.") } }
    val hasColorTemp = remember(lightEntities) { lightEntities.any { it.supportsColorTemp } }
    val hasColor = remember(lightEntities) { lightEntities.any { it.supportsColor } }
    val tabs = remember(lightEntities, hasColorTemp, hasColor) {
        buildList {
            add("Power" to Icons.Default.Power)
            if (lightEntities.isNotEmpty()) add("Bright" to Icons.Default.LightMode)
            if (hasColorTemp) add("Temp" to Icons.Default.Thermostat)
            if (hasColor) add("Color" to Icons.Default.Palette)
        }
    }
    var currentTab by remember(stack.id) { mutableStateOf("Power") }

    fun labelFor(entity: HAEntity) = stack.buttonConfigs[entity.entity_id]?.name ?: entity.friendlyName ?: entity.entity_id
    @Composable
    fun tintFor(entity: HAEntity): Color {
        val isOn = entity.state == "on"
        return if (isOn) lightStateColor(entity) ?: MaterialTheme.colorScheme.primary else appColors.onMuted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.elevated)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MdiIcon(stack.icon, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stack.title ?: "Entities", style = MaterialTheme.typography.titleLarge, color = appColors.onSurface)
                            Text(
                                if (activeCount > 0) "$activeCount/${entities.size} active" else "All off",
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onMuted
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(appColors.elevated.copy(alpha = 0.85f), CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = appColors.onSurface, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { displayEntities.forEach { turnOff(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text("Turn off all")
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = if (tabs.size > 1) 88.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (currentTab) {
                            "Bright" -> items(lightEntities.size) { index ->
                                val entity = lightEntities[index]
                                GroupLightControlRow(label = labelFor(entity), icon = stack.buttonConfigs[entity.entity_id]?.icon ?: defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                                    HorizontalLightBar(
                                        entity = entity,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                }
                            }
                            "Temp" -> {
                                val list = lightEntities.filter { it.supportsColorTemp }
                                items(list.size) { index ->
                                    val entity = list[index]
                                    GroupLightControlRow(label = labelFor(entity), icon = stack.buttonConfigs[entity.entity_id]?.icon ?: defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                                        HorizontalColorTempBar(
                                            entity = entity,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        )
                                    }
                                }
                            }
                            "Color" -> {
                                val list = lightEntities.filter { it.supportsColor }
                                items(list.size) { index ->
                                    val entity = list[index]
                                    GroupLightControlRow(label = labelFor(entity), icon = stack.buttonConfigs[entity.entity_id]?.icon ?: defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                                        HorizontalHueBar(
                                            entity = entity,
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        )
                                    }
                                }
                            }
                            else -> items(displayEntities.size) { index ->
                                val entity = displayEntities[index]
                                val config = stack.buttonConfigs[entity.entity_id]
                                val label = labelFor(entity)
                                val dom = entity.entity_id.substringBefore(".")
                                val s = entity.state.lowercase()
                                val isItemActive = when (dom) {
                                    "lock"    -> s != "locked" && s != "unavailable"
                                    "cover"   -> s != "closed" && s != "unavailable"
                                    "climate" -> s != "off"    && s != "unavailable"
                                    "vacuum"  -> s == "cleaning" || s == "returning" || s == "paused" || s == "error"
                                    "person"  -> s == "home"
                                    else      -> s == "on"
                                }
                                val itemIcon = config?.icon ?: defaultEntityIconSlug(entity)
                                val itemIconColor = when (dom) {
                                    "light" -> if (isItemActive) lightStateColor(entity) ?: MaterialTheme.colorScheme.primary else appColors.onMuted
                                    "climate" -> hvacColor(
                                        entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                                            ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                                            ?: entity.state
                                    )
                                    else -> if (isItemActive) MaterialTheme.colorScheme.primary else appColors.onMuted
                                }
                                val stateLabel = when (dom) {
                                    "light" -> if (isItemActive) {
                                        val bp = ((entity.brightness ?: 0) / 255f * 100).toInt()
                                        if (entity.supportsBrightness) "On - ${bp}%" else "On"
                                    } else {
                                        entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                    }
                                    "climate" -> {
                                        val mode = (entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                                            ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                                            ?: entity.state)
                                            .split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                        val temp = entity.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull
                                        if (temp != null && isItemActive) "$mode - ${temp.toInt()}\u00B0C" else mode
                                    }
                                    else -> entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = appColors.subtleSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MdiIcon(itemIcon, contentDescription = null, tint = itemIconColor, size = 24.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            val rowTextColor = appColors.onSurface
                                            Text(label, color = rowTextColor, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                stateLabel,
                                                color = rowTextColor,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        IconButton(
                                            onClick = { turnOff(entity) },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.PowerSettingsNew,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }

                if (tabs.size > 1) {
                    GroupDialogTabBar(
                        tabs = tabs,
                        currentTab = currentTab,
                        onSelect = { currentTab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun GenericEntityDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    titleOverride: String? = null,
    iconName: String? = null,
    spinIcon: Boolean = false
) {
    val domain = entity.entity_id.substringBefore(".")
    val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
    val valueText = listOfNotNull(entity.state, unit).joinToString(" ")
    val isSwitchLike = domain in listOf("light", "switch", "input_boolean", "fan", "automation")
    val isGraphLike = domain in listOf("sensor", "binary_sensor")
    val icon = when (domain) {
        "switch", "input_boolean" -> Icons.Default.PowerSettingsNew
        "binary_sensor" -> Icons.Default.Security
        "sensor" -> Icons.Default.Sensors
        "fan" -> Icons.Default.Air
        else -> Icons.Default.Power
    }

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = icon,
        iconTint = if (isSwitchLike) Color(0xFFBE73CC) else Color(0xFF4A90E2),
        titleOverride = titleOverride,
        iconName = iconName,
        spinIcon = spinIcon,
        showHistoryButton = !isGraphLike,
        statusText = valueText.uppercase()
    ) {
        val appColors = LocalHKIAppColors.current
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isSwitchLike) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (entity.state == "on") "On" else "Off",
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        VerticalMasterSwitch(
                            isOn = entity.state == "on",
                            onToggle = { viewModel.toggleEntity(entity.entity_id) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("SWITCH", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            } else if (isGraphLike) {
                SensorGraphContent(entity = entity, viewModel = viewModel, valueText = valueText)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = valueText,
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = domain.replace("_", " ").uppercase(),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(32.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = appColors.subtleSurface,
                        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Open history for graph", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorGraphContent(entity: HAEntity, viewModel: MainViewModel, valueText: String) {
    val historyMapping by viewModel.historyMapping.collectAsState()
    val history = historyMapping[entity.entity_id].orEmpty()
    val domain = entity.entity_id.substringBefore(".")
    val isBinary = domain == "binary_sensor"
    val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
    val lineColor = if (isBinary) Color(0xFFBE73CC) else Color(0xFF4A90E2)
    val appColors = LocalHKIAppColors.current

    var selectedHours by remember { mutableIntStateOf(24) }
    var loadedHours by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(entity.entity_id, selectedHours) {
        loadedHours = null
        viewModel.fetchEntityHistory(entity.entity_id, selectedHours.toLong())
        loadedHours = selectedHours
    }

    val graphPoints = remember(history, selectedHours, isBinary, unit) {
        history.mapNotNull { entry ->
            val millis = parseHistoryMillis(entry.last_changed) ?: return@mapNotNull null
            val value = if (isBinary) {
                when (entry.state.lowercase()) {
                    "on", "open", "detected", "home", "true" -> 1f
                    "off", "closed", "clear", "not_home", "false" -> 0f
                    else -> return@mapNotNull null
                }
            } else entry.state.toFloatOrNull() ?: return@mapNotNull null
            val label = if (isBinary) (if (value == 1f) "On" else "Off")
            else trimGraphValue(value) + if (unit.isNotBlank()) " $unit" else ""
            HistoryPoint(millis, value, label)
        }.sortedBy { it.timeMillis }
    }

    val values = graphPoints.map { it.value }
    val minValue = values.minOrNull()
    val maxValue = values.maxOrNull()
    val avgValue = if (values.isNotEmpty()) values.average().toFloat() else null

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valueText,
            color = appColors.onSurface,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isBinary) "STATE" else "SENSOR",
            color = appColors.onMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(20.dp))
        HistoryRangeChips(
            selectedHours = selectedHours,
            onSelect = { selectedHours = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(24.dp),
            color = appColors.subtleSurface,
            border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
        ) {
            if (graphPoints.size < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (loadedHours == selectedHours) {
                        Text("Not enough data in this period", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                InteractiveLineGraph(
                    points = graphPoints,
                    lineColor = lineColor,
                    stepped = isBinary,
                    modifier = Modifier.fillMaxSize(),
                    gradientColors = if (isBinary) null else sensorGradientColors(entity.deviceClass)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Legend: series name + summary stats
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(lineColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = entity.friendlyName ?: domain.replace("_", " ").replaceFirstChar { it.uppercase() },
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("Last ${selectedHours}h", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
        if (!isBinary && minValue != null && maxValue != null) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GraphStat("Min", trimGraphValue(minValue), unit)
                avgValue?.let { GraphStat("Avg", trimGraphValue(it), unit) }
                GraphStat("Max", trimGraphValue(maxValue), unit)
                graphPoints.lastOrNull()?.let { GraphStat("Now", trimGraphValue(it.value), unit) }
            }
        }
    }
}

@Composable
private fun GraphStat(label: String, value: String, unit: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (unit.isNotBlank()) "$value $unit" else value,
            color = appColors.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun trimGraphValue(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)
}

@Composable
fun SubtitleWidget(
    widget: HKISubtitleWidget,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        if (!widget.icon.isNullOrBlank()) {
            MdiIcon(widget.icon, tint = appColors.onMuted, size = 22.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = widget.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = appColors.onSurface
        )
        if (isEditMode) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = appColors.onMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = appColors.onMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun HeaderTextSettingsDialog(
    widget: HKISubtitleWidget,
    onDismiss: () -> Unit,
    onSave: (HKISubtitleWidget) -> Unit
) {
    var text by remember(widget) { mutableStateOf(widget.text) }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "") }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var showIconPickerHeader by remember { mutableStateOf(false) }

    if (showIconPickerHeader) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPickerHeader = false },
            onSelect = { iconName = it; showIconPickerHeader = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Header Text") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    if (iconName.isNotEmpty()) MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPickerHeader = true }) { Text(if (iconName.isEmpty()) "Choose" else "Change") }
                    if (iconName.isNotEmpty()) TextButton(onClick = { iconName = "" }) { Text("None") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(text = text.ifBlank { "Header Text" }, width = width, icon = iconName.ifEmpty { null }))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

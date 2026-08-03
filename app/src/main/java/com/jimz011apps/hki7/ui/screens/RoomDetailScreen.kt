@file:Suppress(
    "unused",
    "KotlinConstantConditions",
    "MoveLambdaOutsideParentheses",
    "CascadeIf",
    "SpellCheckingInspection",
    "GrazieInspection"
)

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Switch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Schedule
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
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.isSpacerEntityId
import com.jimz011apps.hki7.data.newSpacerEntityId
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HADeviceRegistryEntry
import com.jimz011apps.hki7.data.HAServiceCall
import com.jimz011apps.hki7.data.HKIAction
import com.jimz011apps.hki7.data.HKIButtonStack
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.VISIBILITY_MATCH_ALL
import com.jimz011apps.hki7.data.isButtonVisibleNow
import com.jimz011apps.hki7.data.visibilityConditionEntityIds
import com.jimz011apps.hki7.data.HKIBatteryCardWidget
import com.jimz011apps.hki7.data.HKICalendarWidget
import com.jimz011apps.hki7.data.HKIWasteCollectionWidget
import com.jimz011apps.hki7.data.HKIParcelsWidget
import com.jimz011apps.hki7.data.HKIEmptyStack
import com.jimz011apps.hki7.data.HKIAreaConfig
import com.jimz011apps.hki7.data.HKIRoomWidget
import com.jimz011apps.hki7.data.HKIClimateCardWidget
import com.jimz011apps.hki7.data.HKIClimateStack
import com.jimz011apps.hki7.data.HKIEnergyCardWidget
import com.jimz011apps.hki7.data.HKIEnergyConfig
import com.jimz011apps.hki7.data.HKIEnergyStack
import com.jimz011apps.hki7.data.HKIIframeWidget
import com.jimz011apps.hki7.data.HKIMarkdownWidget
import com.jimz011apps.hki7.data.HKIMediaPlayerWidget
import com.jimz011apps.hki7.data.HKISensorGraphStack
import com.jimz011apps.hki7.data.HKISensorGraphWidget
import com.jimz011apps.hki7.data.HKISingleEntityWidget
import com.jimz011apps.hki7.data.HKISwipingStack
import com.jimz011apps.hki7.data.HKISubtitleWidget
import com.jimz011apps.hki7.data.HKIWeatherWidget
import com.jimz011apps.hki7.ui.components.DevicePickerDialog
import androidx.compose.animation.core.tween
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.resolveRoomMediaStatus
import com.jimz011apps.hki7.ui.localizedText
import com.jimz011apps.hki7.ui.resolveRoomStatus
import com.jimz011apps.hki7.ui.displayedRoomControlEntityIds
import com.jimz011apps.hki7.ui.roomMediaPlayerIds
import com.jimz011apps.hki7.ui.Screen
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.AdaptiveLightingWidget
import com.jimz011apps.hki7.ui.components.rememberAdaptiveLightingProfiles
import com.jimz011apps.hki7.ui.components.VacuumWidgetSetupDialog
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.EntityCard
import com.jimz011apps.hki7.ui.components.SpacerButtonCard
import com.jimz011apps.hki7.ui.components.LocalMaxStackColumns
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.ModernSettingsHeader
import com.jimz011apps.hki7.ui.components.ModernSettingsMenuItem
import com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle
import com.jimz011apps.hki7.ui.components.SettingsSubcategory
import com.jimz011apps.hki7.ui.components.SettingsTabRow
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.mediaPlayerStateIcon
import com.jimz011apps.hki7.ui.components.COMMON_STATE_UNITS
import com.jimz011apps.hki7.ui.components.defaultEntityIconSlug
import com.jimz011apps.hki7.ui.components.selectableEntityAttributes
import com.jimz011apps.hki7.ui.components.coverAccentColor
import com.jimz011apps.hki7.ui.components.entityStateIconColor
import com.jimz011apps.hki7.ui.components.HKIDialog
import com.jimz011apps.hki7.ui.components.HKIPage
import com.jimz011apps.hki7.ui.components.RoomStatusIndicators
import com.jimz011apps.hki7.ui.components.GradientActionButton
import com.jimz011apps.hki7.ui.components.LocalItemCornerRadius
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.withGlobalCornerRadius
import com.jimz011apps.hki7.ui.components.HistoryPoint
import com.jimz011apps.hki7.ui.components.HistoryRangeChips
import com.jimz011apps.hki7.ui.components.HistoryView
import com.jimz011apps.hki7.ui.components.InteractiveLineGraph
import com.jimz011apps.hki7.ui.components.parseHistoryMillis
import com.jimz011apps.hki7.ui.components.HKICameraDialog
import com.jimz011apps.hki7.ui.components.ZoomableCameraImage
import com.jimz011apps.hki7.ui.components.DateTimePickerDialog
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.utils.handleActionOutcome
import com.jimz011apps.hki7.ui.components.ActionEditor
import com.jimz011apps.hki7.ui.components.CustomButtonsEditor
import com.jimz011apps.hki7.ui.components.LocalDialogCustomButtons
import com.jimz011apps.hki7.ui.components.LocalDialogNavController
import androidx.compose.runtime.CompositionLocalProvider
import com.jimz011apps.hki7.ui.components.HKILightDialog
import com.jimz011apps.hki7.ui.components.buildWebRtcApiUrl
import com.jimz011apps.hki7.ui.components.buildCameraRefreshModel
import com.jimz011apps.hki7.ui.components.lightStateColor
import com.jimz011apps.hki7.ui.components.resolveEntityCameraUrl
import com.jimz011apps.hki7.ui.components.resolveCameraUrl
import com.jimz011apps.hki7.ui.components.ReorderableGrid
import com.jimz011apps.hki7.ui.components.ReorderAxis
import com.jimz011apps.hki7.ui.components.VerticalMasterSwitch
import com.jimz011apps.hki7.ui.components.VerticalSlider
import com.jimz011apps.hki7.ui.components.VerticalControlHeight
import com.jimz011apps.hki7.ui.components.hvacColor
import com.jimz011apps.hki7.ui.components.hvacGradient
import com.jimz011apps.hki7.ui.components.localizedClimateModeLabel
import com.jimz011apps.hki7.ui.components.climateModeIcon
import com.jimz011apps.hki7.ui.components.ClimateModesButton
import com.jimz011apps.hki7.ui.components.ClimateModesList
import com.jimz011apps.hki7.ui.components.CoverTiltSlider
import com.jimz011apps.hki7.ui.components.HKIFanDialog
import com.jimz011apps.hki7.ui.components.HKIHumidifierDialog
import com.jimz011apps.hki7.ui.components.sensorGradientColors
import com.jimz011apps.hki7.ui.components.HKIAlarmDialog
import com.jimz011apps.hki7.ui.components.PersonDetailDialog
import com.jimz011apps.hki7.ui.components.HKIBottomBar
import com.jimz011apps.hki7.ui.components.HorizontalLightBar
import com.jimz011apps.hki7.ui.components.HorizontalColorTempBar
import com.jimz011apps.hki7.ui.components.HorizontalHueBar
import com.jimz011apps.hki7.ui.components.responsiveDashboardColumnCount
import com.jimz011apps.hki7.ui.components.swipeToAdjacentTab
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class WidgetStyleOverride(
    val isSquare: Boolean,
    val cornerRadius: Int
)

val swipingStackAnimationTypes = listOf(
    "swipe" to R.string.cr_animation_swipe,
    "fade" to R.string.cr_animation_fade,
    "zoom" to R.string.cr_animation_zoom,
    "slide_fade" to R.string.cr_animation_slide_fade,
    "cube" to R.string.cr_animation_cube,
    "instant" to R.string.cr_animation_instant
)

private const val BUTTON_LOCK_DOUBLE_TAP = "double_tap"
private const val BUTTON_LOCK_PIN = "pin"
private const val DEFAULT_BUTTON_RELOCK_SECONDS = 30

fun HKIRoomWidget.withStackChildStyle(isSquare: Boolean, cornerRadius: Int): HKIRoomWidget = when (this) {
    is HKIButtonStack -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKISingleEntityWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIWeatherWidget -> copy(cornerRadius = cornerRadius)
    is HKICalendarWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIBatteryCardWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIWasteCollectionWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIParcelsWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIClimateCardWidget -> copy(cornerRadius = cornerRadius)
    is HKIClimateStack -> copy(cornerRadius = cornerRadius)
    is HKIMediaPlayerWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIMarkdownWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKIIframeWidget -> copy(cornerRadius = cornerRadius)
    is HKISensorGraphWidget -> copy(isSquare = isSquare, cornerRadius = cornerRadius)
    is HKISensorGraphStack -> copy(cornerRadius = cornerRadius)
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
    val context = LocalContext.current
    val areas by viewModel.areas.collectAsState()
    val area = areas.find { it.area_id == areaId }
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    val areaWidgetsMapping by viewModel.areaWidgetsMapping.collectAsState()
    val areaConfigsMapping by viewModel.areaConfigsMapping.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    // Aesthetics-only recipients (Family Sharing) keep visual edits but can't add/remove structure.
    val aestheticsOnly by viewModel.aestheticsOnlyEditing.collectAsState()
    val structuralEditing = isEditMode && !aestheticsOnly
    val dashboardMode by viewModel.dashboardMode.collectAsState()
    val uiRevision by viewModel.uiRevision.collectAsState()

    val itemCornerRadius = LocalItemCornerRadius.current
    val areaWidgets = remember(areaWidgetsMapping, areaId, itemCornerRadius) {
        areaWidgetsMapping[areaId].orEmpty().map { it.withGlobalCornerRadius(itemCornerRadius) }
    }
    val widgetGridState = rememberLazyGridState()
    val areaConfig = areaConfigsMapping[areaId] ?: HKIAreaConfig()
    val defaultMarkdownContent = stringResource(R.string.cr_default_markdown_content)

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
    var editingCalendarWidget by remember { mutableStateOf<Pair<String?, HKICalendarWidget>?>(null) }
    var editingWasteWidget by remember { mutableStateOf<Pair<String?, HKIWasteCollectionWidget>?>(null) }
    var pendingWasteWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingParcelsWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var editingBatteryWidget by remember { mutableStateOf<Pair<String?, HKIBatteryCardWidget>?>(null) }
    var editingParcelsWidget by remember { mutableStateOf<Pair<String?, HKIParcelsWidget>?>(null) }
    var editingClimateCard by remember { mutableStateOf<Pair<String?, HKIClimateCardWidget>?>(null) }
    var editingClimateStack by remember { mutableStateOf<Pair<String?, HKIClimateStack>?>(null) }
    // Newly added cards awaiting their entity selection; created only once configured.
    var configuringEnergyCards by remember { mutableStateOf<List<Pair<String?, HKIEnergyCardWidget>>>(emptyList()) }
    var configuringClimateCards by remember { mutableStateOf<List<Pair<String?, HKIClimateCardWidget>>>(emptyList()) }
    var editingMediaPlayerWidget by remember { mutableStateOf<Pair<String?, HKIMediaPlayerWidget>?>(null) }
    var editingMarkdownWidget by remember { mutableStateOf<Pair<String?, HKIMarkdownWidget>?>(null) }
    var editingIframeWidget by remember { mutableStateOf<Pair<String?, HKIIframeWidget>?>(null) }
    var editingSensorGraphWidget by remember { mutableStateOf<Pair<String?, HKISensorGraphWidget>?>(null) }
    var editingSensorGraphStack by remember { mutableStateOf<Pair<String?, HKISensorGraphStack>?>(null) }
    var pendingMediaPlayerWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingSensorGraphWidgetContainerId by remember { mutableStateOf<String?>(null) }
    var pendingSensorGraphStackContainerId by remember { mutableStateOf<String?>(null) }
    var pendingCalendarWidgetContainerId by remember { mutableStateOf<String?>(null) }
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
    var selectedMediaPlayerId by remember { mutableStateOf<String?>(null) }
    var selectedFanEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedFanConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showFanDialog by remember { mutableStateOf(false) }
    var selectedHumidifierEntity by remember { mutableStateOf<HAEntity?>(null) }
    var selectedHumidifierConfig by remember { mutableStateOf<HKIButtonConfig?>(null) }
    var showHumidifierDialog by remember { mutableStateOf(false) }
    // Entities behind a tapped room-status counter, listed in the aggregated (badge-style) dialog.
    var activityDialogRole by remember { mutableStateOf<String?>(null) }
    var activityDialogEntityIds by remember { mutableStateOf<List<String>>(emptyList()) }
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
    var orderingStack by remember { mutableStateOf<Pair<String?, HKIButtonStack>?>(null) }
    var selectedChildButtonSettings by remember { mutableStateOf<Triple<String, HKIButtonStack, String>?>(null) }
    var editingChildEmptyStack by remember { mutableStateOf<Pair<String, HKIEmptyStack>?>(null) }
    var editingChildSubtitle by remember { mutableStateOf<Pair<String, HKISubtitleWidget>?>(null) }
    var editingChildWeather by remember { mutableStateOf<Pair<String, HKIWeatherWidget>?>(null) }
    var showAutoWidgetInfo by remember { mutableStateOf(false) }
    var showAutoDeleteStackInfo by remember { mutableStateOf(false) }
    // Configuration pickers must not depend solely on the state stream: that stream can still be
    // seeding/reconnecting while the entity registry is already available. Merge both sources so
    // every registered entity remains selectable, replacing registry placeholders with live states.
    val liveEntities by viewModel.entities.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchRegistries()
        if (viewModel.entities.value.isEmpty()) viewModel.refreshEntities(isSilent = true, includeDashboardRefresh = false)
    }
    val allEntities = remember(liveEntities, entityRegistry) {
        val liveById = liveEntities.associateBy { it.entity_id }
        (liveEntities + entityRegistry.asSequence()
            .filterNot { it.entity_id in liveById }
            .map { HAEntity(entity_id = it.entity_id, state = "unavailable") }
            .toList())
            .distinctBy { it.entity_id }
    }
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
    val adaptiveLightingTitle = stringResource(R.string.ui_adaptive_lighting_e2cffbd)
    fun newAdaptiveLightingWidget() = HKIButtonStack(
        id = UUID.randomUUID().toString(),
        title = adaptiveLightingTitle,
        icon = "auto-awesome",
        isSquare = false,
        stackType = "adaptive_lighting",
        buttonStyle = "tile",
        collapsible = false
    )
    fun newEmptyStack() = HKIEmptyStack(id = UUID.randomUUID().toString())
    // Half width and square: the footprint of a normal button widget, which is what it stands in for.
    fun newSpacerWidget() = HKISingleEntityWidget(
        id = UUID.randomUUID().toString(),
        entityId = newSpacerEntityId(),
        kind = "button",
        width = "half",
        isSquare = true
    )
    fun newSingleEntityWidget(kind: String, entityId: String, config: HKIButtonConfig = HKIButtonConfig()) = HKISingleEntityWidget(
        id = UUID.randomUUID().toString(),
        entityId = entityId,
        kind = kind,
        isSquare = kind != "camera",
        config = config
    )
    fun newCalendarWidget(entityIds: List<String>) = HKICalendarWidget(
        id = UUID.randomUUID().toString(),
        entityIds = entityIds,
        width = "full"
    )
    fun newWasteWidget(entityIds: List<String>) = HKIWasteCollectionWidget(
        id = UUID.randomUUID().toString(),
        entityIds = entityIds,
        width = "full"
    )
    fun newMarkdownWidget() = HKIMarkdownWidget(
        id = UUID.randomUUID().toString(),
        content = defaultMarkdownContent
    )
    fun newIframeWidget() = HKIIframeWidget(id = UUID.randomUUID().toString())
    fun addChildToSwipingStack(stackId: String, child: HKIRoomWidget) {
        if (aestheticsOnly) return
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
        if (aestheticsOnly) return
        val swipe = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == stackId }
        val empty = areaWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == stackId }
        when {
            swipe != null -> viewModel.updateWidget(areaId, swipe.copy(widgets = swipe.widgets.filterNot { it.id == childId }))
            empty != null -> viewModel.updateWidget(areaId, empty.copy(widgets = empty.widgets.filterNot { it.id == childId }))
        }
    }
    var openEntityFromSwipingChild: (String, HKIButtonStack) -> Unit = { _, _ -> }
    var openEntityFromSingleWidget: (String, String) -> Unit = { _, _ -> }
    // Runs a single-entity widget's configured tap/hold/double action (falls back to opening the
    // entity dialog for more_info / cameras). Assigned once openEntityDialog is in scope below.
    var runSingleWidgetAction: (HKISingleEntityWidget, String) -> Unit = { _, _ -> }
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
                    if (!isEditMode) handleActionOutcome(
                        viewModel.performButtonAction(areaId, child.id, entityId, "tap"), context, navController
                    ) { openEntityFromSwipingChild(it, child) }
                },
                onEntityDoubleClick = { entityId ->
                    if (!isEditMode) handleActionOutcome(
                        viewModel.performButtonAction(areaId, child.id, entityId, "double"), context, navController
                    ) { openEntityFromSwipingChild(it, child) }
                },
                onEntityLongClick = { entityId ->
                    if (!isEditMode) handleActionOutcome(
                        viewModel.performButtonAction(areaId, child.id, entityId, "hold"), context, navController
                    ) { openEntityFromSwipingChild(it, child) }
                },
                onSettingsClick = { selectedChildStackSettings = parent.id to child },
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDeleteClick = { deleteChildFromSwipingStack(parent.id, child.id) },
                onHideClick = { updateChildInSwipingStack(parent.id, child.copy(isHidden = !child.isHidden)) },
                onAddClick = { addingToNestedStack = parent.id to child.id },
                onManageOrder = { orderingStack = parent.id to child },
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
            is HKIClimateCardWidget -> ClimateCardWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingClimateCard = parent.id to child }
            )
            is HKIClimateStack -> ClimateStackWidgetItem(
                stack = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingClimateStack = parent.id to child }
            )
            is HKIMediaPlayerWidget -> MediaPlayerWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onOpen = { entityId -> openEntityFromSingleWidget(entityId, "button") },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingMediaPlayerWidget = parent.id to child }
            )
            is HKIIframeWidget -> IframeWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingIframeWidget = parent.id to child },
            )
            is HKIMarkdownWidget -> MarkdownWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingMarkdownWidget = parent.id to child },
                currentUrl = currentUrl
            )
            is HKISensorGraphWidget -> SensorGraphWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingSensorGraphWidget = parent.id to child }
            )
            is HKISensorGraphStack -> SensorGraphStackWidgetItem(
                stack = styleOverride?.let { child.copy(width = "full", cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onToggleCollapsed = { updateChildInSwipingStack(parent.id, child.copy(isCollapsed = !(child.isCollapsed ?: child.defaultCollapsed))) },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingSensorGraphStack = parent.id to child }
            )
            is HKISingleEntityWidget -> SingleEntityWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                currentUrl = currentUrl,
                accessToken = accessToken,
                isEditMode = isEditMode,
                onEntityClick = { runSingleWidgetAction(child, "tap") },
                onEntityDoubleClick = { runSingleWidgetAction(child, "double") },
                onEntityLongClick = { runSingleWidgetAction(child, "hold") },
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
            is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                widget = styleOverride?.let { child.copy(width = "full", isSquare = it.isSquare, cornerRadius = it.cornerRadius) } ?: child.copy(width = "full"),
                viewModel = viewModel,
                registry = entityRegistry,
                devices = deviceRegistry,
                isEditMode = isEditMode,
                onOpen = { navController.navigate(Screen.Battery.WIDGET_ROUTE) },
                onDelete = { deleteChildFromSwipingStack(parent.id, child.id) },
                onSettings = { editingBatteryWidget = parent.id to child }
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
            entityId.startsWith("media_player.") -> {
                selectedMediaPlayerId = entityId
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
    runSingleWidgetAction = { widget, trigger ->
        if (widget.kind == "camera") {
            openEntityFromSingleWidget(widget.entityId, "camera")
        } else {
            val action = viewModel.resolveButtonAction(widget.config, widget.entityId, trigger)
            handleActionOutcome(
                viewModel.executeAction(action, widget.entityId, trigger), context, navController
            ) { openEntityFromSingleWidget(it, widget.kind) }
        }
    }
    val mediaPlayerIds = remember(areaConfig) { areaConfig.roomMediaPlayerIds() }
    val mediaPlayers = remember(mediaPlayerIds, allEntities) {
        val byId = allEntities.associateBy(HAEntity::entity_id)
        mediaPlayerIds.map { id -> byId[id] ?: HAEntity(entity_id = id, state = "unavailable") }
    }
    val mediaSummary = remember(mediaPlayers) { resolveRoomMediaStatus(mediaPlayers) }
    val mediaStatus = mediaSummary.localizedText()
    val roomSummary = remember(areaConfig, allEntities, areaWidgets) {
        resolveRoomStatus(areaConfig, allEntities, displayedRoomControlEntityIds(areaWidgets))
    }
    HKIPage(
        viewModel = viewModel,
        areaId = areaId,
        title = areaConfig.name ?: area?.name ?: stringResource(R.string.cr_room),
        subtitle = mediaStatus ?: stringResource(R.string.cr_room_details),
        subtitleIcon = mediaPlayerStateIcon(mediaSummary.representative),
        backgroundImage = if (!areaConfig.headerColor.isNullOrBlank()) null else areaConfig.wallpaper ?: area?.picture,
        headerColor = areaConfig.headerColor,
        onBack = { navController.popBackStack() },
        headerTrailingContent = if (roomSummary.indicators.isNotEmpty()) {
            { _ ->
                RoomStatusIndicators(
                    summary = roomSummary,
                    compact = false,
                    onIndicatorClick = { role, ids -> activityDialogRole = role; activityDialogEntityIds = ids }
                )
            }
        } else null,
        headerBottomContent = roomSummary.environmentText?.let { environment ->
            { color ->
                Text(
                    text = environment,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        navController = navController
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Number of full-width columns. Scales up on larger screens (fold/tablet)
        // so widgets tile into several columns instead of one wide stack.
        val widgetColumnCount = responsiveDashboardColumnCount(maxWidth)
        // Six grid cells per column so widgets can span a full column (6), half (3), or third (2).
        val widgetGridColumns = widgetColumnCount * 6
        fun widgetSpan(widget: HKIRoomWidget): Int = when (widget.width) {
            "third" -> 2
            "half" -> 3
            else -> 6
        }
        Column(modifier = Modifier.fillMaxSize()) {
            key(uiRevision) {
                if (!isEditMode) {
                    if (areaWidgets.isEmpty()) {
                        EmptyEditHint(
                            Modifier.fillMaxSize(),
                            stringResource(R.string.cr_empty_room)
                        )
                    } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(widgetGridColumns),
                        state = widgetGridState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current),
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
                                        viewModel = viewModel,
                                        currentUrl = currentUrl,
                                        accessToken = accessToken,
                                        isEditMode = false,
                                        onEntityClick = { entityId ->
                                            handleActionOutcome(
                                                viewModel.performButtonAction(areaId, widget.id, entityId, "tap"), context, navController
                                            ) { openEntityDialog(it, widget) }
                                        },
                                        onEntityDoubleClick = { entityId ->
                                            handleActionOutcome(
                                                viewModel.performButtonAction(areaId, widget.id, entityId, "double"), context, navController
                                            ) { openEntityDialog(it, widget) }
                                        },
                                        onEntityLongClick = { entityId ->
                                            handleActionOutcome(
                                                viewModel.performButtonAction(areaId, widget.id, entityId, "hold"), context, navController
                                            ) { openEntityDialog(it, widget) }
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
                                    onEntityClick = { runSingleWidgetAction(widget, "tap") },
                                    onEntityDoubleClick = { runSingleWidgetAction(widget, "double") },
                                    onEntityLongClick = { runSingleWidgetAction(widget, "hold") },
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
                                is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                                    widget = widget,
                                    viewModel = viewModel,
                                    registry = entityRegistry,
                                    devices = deviceRegistry,
                                    isEditMode = false,
                                    onOpen = { navController.navigate(Screen.Battery.WIDGET_ROUTE) },
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
                                    onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIClimateCardWidget -> ClimateCardWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIClimateStack -> ClimateStackWidgetItem(
                                    stack = widget, viewModel = viewModel, isEditMode = false,
                                    onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIMediaPlayerWidget -> MediaPlayerWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onOpen = { entityId -> openEntityDialog(entityId, null) },
                                    onDelete = {}, onSettings = {}
                                )
                                is HKIMarkdownWidget -> MarkdownWidgetItem(
                                    widget = widget, isEditMode = false,
                                    onDelete = {}, onSettings = {}, currentUrl = currentUrl
                                )
                                is HKIIframeWidget -> IframeWidgetItem(
                                    widget = widget, isEditMode = false, onDelete = {}, onSettings = {}
                                )
                                is HKISensorGraphWidget -> SensorGraphWidgetItem(
                                    widget = widget, viewModel = viewModel, isEditMode = false,
                                    onDelete = {}, onSettings = {}
                                )
                                is HKISensorGraphStack -> SensorGraphStackWidgetItem(
                                    stack = widget, viewModel = viewModel, isEditMode = false,
                                    onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                                    onDelete = {}, onSettings = {}
                                )
                            }
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
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 156.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current),
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
                                viewModel = viewModel,
                                currentUrl = currentUrl,
                                accessToken = accessToken,
                                isEditMode = isEditMode,
                            onEntityClick = { entityId ->
                                if (!isEditMode) handleActionOutcome(
                                    viewModel.performButtonAction(areaId, widget.id, entityId, "tap"), context, navController
                                ) { openEntityDialog(it, widget) }
                            },
                            onEntityDoubleClick = { entityId ->
                                if (!isEditMode) handleActionOutcome(
                                    viewModel.performButtonAction(areaId, widget.id, entityId, "double"), context, navController
                                ) { openEntityDialog(it, widget) }
                            },
                            onEntityLongClick = { entityId ->
                                if (!isEditMode) handleActionOutcome(
                                    viewModel.performButtonAction(areaId, widget.id, entityId, "hold"), context, navController
                                ) { openEntityDialog(it, widget) }
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
                                onManageOrder = { orderingStack = null to widget },
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
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                                onSettings = { editingWeather = widget }
                            )
                        }
                        is HKICalendarWidget -> {
                            CalendarWidgetItem(
                                widget = widget,
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                                onSettings = { editingCalendarWidget = null to widget }
                            )
                        }
                        is HKISingleEntityWidget -> SingleEntityWidgetItem(
                            widget = widget,
                            viewModel = viewModel,
                            currentUrl = currentUrl,
                            accessToken = accessToken,
                            isEditMode = isEditMode,
                            onEntityClick = { runSingleWidgetAction(widget, "tap") },
                            onEntityDoubleClick = { runSingleWidgetAction(widget, "double") },
                            onEntityLongClick = { runSingleWidgetAction(widget, "hold") },
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
                        is HKIBatteryCardWidget -> BatteryCardWidgetItem(
                            widget = widget,
                            viewModel = viewModel,
                            registry = entityRegistry,
                            devices = deviceRegistry,
                            isEditMode = isEditMode,
                            onOpen = { navController.navigate(Screen.Battery.WIDGET_ROUTE) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingBatteryWidget = null to widget }
                        )
                        is HKIWasteCollectionWidget -> WasteCollectionWidgetItem(
                            widget = widget,
                            viewModel = viewModel,
                            isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingWasteWidget = null to widget },
                            onUpdate = { viewModel.updateWidget(areaId, it) }
                        )
                        is HKIParcelsWidget -> ParcelsWidgetItem(
                            widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingParcelsWidget = null to widget }
                        )
                        is HKIEnergyStack -> EnergyStackWidgetItem(
                            stack = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingEnergyStack = null to widget }
                        )
                        is HKIClimateCardWidget -> ClimateCardWidgetItem(
                            widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingClimateCard = null to widget }
                        )
                        is HKIClimateStack -> ClimateStackWidgetItem(
                            stack = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingClimateStack = null to widget }
                        )
                        is HKIMediaPlayerWidget -> MediaPlayerWidgetItem(
                            widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onOpen = { entityId -> openEntityDialog(entityId, null) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingMediaPlayerWidget = null to widget }
                        )
                        is HKIMarkdownWidget -> MarkdownWidgetItem(
                            widget = widget, isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingMarkdownWidget = null to widget },
                            currentUrl = currentUrl
                        )
                        is HKIIframeWidget -> IframeWidgetItem(
                            widget = widget, isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingIframeWidget = null to widget },
                        )
                        is HKISensorGraphWidget -> SensorGraphWidgetItem(
                            widget = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingSensorGraphWidget = null to widget }
                        )
                        is HKISensorGraphStack -> SensorGraphStackWidgetItem(
                            stack = widget, viewModel = viewModel, isEditMode = isEditMode,
                            onToggleCollapsed = { viewModel.updateWidget(areaId, widget.copy(isCollapsed = !(widget.isCollapsed ?: widget.defaultCollapsed))) },
                            onDelete = { viewModel.deleteWidget(areaId, widget.id) },
                            onSettings = { editingSensorGraphStack = null to widget }
                        )
                    }
                }
                }
            }
        }

            if (structuralEditing) {
                GradientActionButton(
                    onClick = {
                        if (dashboardMode == "auto") showAutoWidgetInfo = true else showAddWidgetDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        // Lift clear of the system three-button nav bar (0 under gesture nav) so the
                        // button never overlaps it — matching the mini-media-player fix on the nav bar.
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 87.dp,
                            bottom = 87.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current
                        )
                        .height(52.dp)
                        .shadow(10.dp, itemCornerShape()),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_add_widget_a9df350))
                }
            }
        }
    }

    if (showAutoWidgetInfo) {
        AlertDialog(
            onDismissRequest = { showAutoWidgetInfo = false },
            title = { Text(stringResource(R.string.ui_auto_dashboard_c7a509a)) },
            text = { Text(stringResource(R.string.ui_rooms_in_auto_mode_are_imported_from_home_assistant_c583d84)) },
            confirmButton = { Button(onClick = { showAutoWidgetInfo = false }) { Text(stringResource(R.string.ui_ok_9ce3bd4)) } }
        )
    }

    if (showAutoDeleteStackInfo) {
        AlertDialog(
            onDismissRequest = { showAutoDeleteStackInfo = false },
            title = { Text(stringResource(R.string.ui_auto_dashboard_c7a509a)) },
            text = { Text(stringResource(R.string.ui_imported_stacks_cannot_be_removed_in_auto_mode_hide_608f9ed)) },
            confirmButton = { Button(onClick = { showAutoDeleteStackInfo = false }) { Text(stringResource(R.string.ui_ok_9ce3bd4)) } }
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
            onAddSpacerWidget = {
                viewModel.addWidgetToArea(areaId, newSpacerWidget())
                showAddWidgetDialog = false
            },
            onAddAdaptiveLightingWidget = if (entityRegistry.any { it.platform == "adaptive_lighting" }) {
                {
                    viewModel.addWidgetToArea(areaId, newAdaptiveLightingWidget())
                    showAddWidgetDialog = false
                }
            } else null,
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
            onAddCalendarWidget = {
                pendingCalendarWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddWasteWidget = {
                pendingWasteWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddParcelsWidget = {
                pendingParcelsWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddEnergyCard = { keys ->
                configuringEnergyCards = keys.map {
                    null to HKIEnergyCardWidget(id = UUID.randomUUID().toString(), cardKey = it, energyConfig = HKIEnergyConfig())
                }
            },
            onAddEnergyStack = { keys -> viewModel.addWidgetToArea(areaId, HKIEnergyStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddClimateCard = { keys ->
                configuringClimateCards = keys.map {
                    null to HKIClimateCardWidget(id = UUID.randomUUID().toString(), cardKey = it)
                }
            },
            onAddClimateStack = { keys -> viewModel.addWidgetToArea(areaId, HKIClimateStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddMediaPlayerWidget = {
                pendingMediaPlayerWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddMarkdownWidget = {
                viewModel.addWidgetToArea(areaId, newMarkdownWidget())
                showAddWidgetDialog = false
            },
            onAddIframeWidget = {
                viewModel.addWidgetToArea(areaId, newIframeWidget())
                showAddWidgetDialog = false
            },
            onAddSensorGraphWidget = {
                pendingSensorGraphWidgetContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddSensorGraphStack = {
                pendingSensorGraphStackContainerId = "__top__"
                showAddWidgetDialog = false
            },
            onAddBatteryCard = { useNotes ->
                viewModel.addWidgetToArea(areaId, HKIBatteryCardWidget(id = UUID.randomUUID().toString(), useBatteryNotes = useNotes))
                showAddWidgetDialog = false
            },
            allEntities = allEntities
        )
    }

    editingEnergyCard?.let { (containerId, w) ->
        EnergyCardWidgetSettingsDialog(w, viewModel, onDismiss = { editingEnergyCard = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyCard = null
        }
    }
    editingEnergyStack?.let { (containerId, s) ->
        EnergyStackSettingsDialog(s, viewModel, onDismiss = { editingEnergyStack = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingEnergyStack = null
        }
    }
    editingCalendarWidget?.let { (containerId, widget) ->
        CalendarWidgetSettingsDialog(
            widget = widget,
            allEntities = allEntities,
            onDismiss = { editingCalendarWidget = null },
            onSave = { updated ->
                if (containerId == null) viewModel.updateWidget(areaId, updated)
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
                if (containerId == null) viewModel.updateWidget(areaId, updated)
                else updateChildInSwipingStack(containerId, updated)
                editingBatteryWidget = null
            }
        )
    }
    editingWasteWidget?.let { (containerId, widget) ->
        WasteCollectionSettingsDialog(
            widget = widget,
            allEntities = allEntities,
            onDismiss = { editingWasteWidget = null },
            onSave = { updated ->
                if (containerId == null) viewModel.updateWidget(areaId, updated)
                else updateChildInSwipingStack(containerId, updated)
                editingWasteWidget = null
            }
        )
    }
    editingParcelsWidget?.let { (containerId, widget) ->
        ParcelsWidgetSettingsDialog(widget, viewModel, onDismiss = { editingParcelsWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingParcelsWidget = null
        }
    }
    editingClimateCard?.let { (containerId, w) ->
        ClimateCardWidgetSettingsDialog(w, viewModel, onDismiss = { editingClimateCard = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingClimateCard = null
        }
    }
    editingClimateStack?.let { (containerId, s) ->
        ClimateStackSettingsDialog(s, viewModel, onDismiss = { editingClimateStack = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingClimateStack = null
        }
    }
    // Just-added energy cards: pick their entities first, the card is only created on save.
    configuringEnergyCards.firstOrNull()?.let { (containerId, widget) ->
        EnergyCardWidgetSettingsDialog(widget, viewModel, onDismiss = { configuringEnergyCards = configuringEnergyCards.drop(1) }) { configured ->
            if (containerId == null) viewModel.addWidgetToArea(areaId, configured)
            else addChildToSwipingStack(containerId, configured)
            configuringEnergyCards = configuringEnergyCards.drop(1)
        }
    }
    // Just-added climate cards: same, with the card-specific entity picker.
    configuringClimateCards.firstOrNull()?.let { (containerId, widget) ->
        ClimateCardEntityPickerDialog(
            cardKey = widget.cardKey,
            viewModel = viewModel,
            onDismiss = { configuringClimateCards = configuringClimateCards.drop(1) },
            onSelected = { ids ->
                val configured = widget.copy(entityIds = ids)
                if (containerId == null) viewModel.addWidgetToArea(areaId, configured)
                else addChildToSwipingStack(containerId, configured)
                configuringClimateCards = configuringClimateCards.drop(1)
            }
        )
    }
    editingMediaPlayerWidget?.let { (containerId, widget) ->
        MediaPlayerWidgetSettingsDialog(widget, allEntities, onDismiss = { editingMediaPlayerWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingMediaPlayerWidget = null
        }
    }
    editingIframeWidget?.let { (containerId, widget) ->
        IframeWidgetSettingsDialog(widget, onDismiss = { editingIframeWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingIframeWidget = null
        }
    }
    editingMarkdownWidget?.let { (containerId, widget) ->
        MarkdownWidgetSettingsDialog(widget, onDismiss = { editingMarkdownWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingMarkdownWidget = null
        }
    }
    editingSensorGraphWidget?.let { (containerId, widget) ->
        SensorGraphWidgetSettingsDialog(widget, allEntities, onDismiss = { editingSensorGraphWidget = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingSensorGraphWidget = null
        }
    }
    editingSensorGraphStack?.let { (containerId, stack) ->
        SensorGraphStackSettingsDialog(stack, allEntities, onDismiss = { editingSensorGraphStack = null }) { updated ->
            if (containerId == null) viewModel.updateWidget(areaId, updated)
            else updateChildInSwipingStack(containerId, updated)
            editingSensorGraphStack = null
        }
    }
    pendingSensorGraphStackContainerId?.let { target ->
        val sensors = allEntities.filter {
            it.entity_id.startsWith("sensor.") || it.entity_id.startsWith("number.") || it.entity_id.startsWith("input_number.")
        }
        AdvancedEntitySearchDialog(
            allEntities = sensors.ifEmpty { allEntities },
            title = stringResource(R.string.ui_select_sensors_5141d75),
            singleSelect = false,
            preselectedIds = emptySet(),
            onDismiss = {
                if (pendingSensorGraphStackContainerId != null) {
                    pendingSensorGraphStackContainerId = null
                    if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
                }
            },
            onEntitiesSelected = { ids ->
                if (ids.isNotEmpty()) {
                    val stack = HKISensorGraphStack(
                        id = UUID.randomUUID().toString(),
                        graphs = listOf(HKISensorGraphWidget(id = UUID.randomUUID().toString(), entityIds = ids))
                    )
                    if (target == "__top__") viewModel.addWidgetToArea(areaId, stack)
                    else addChildToSwipingStack(target, stack)
                }
                pendingSensorGraphStackContainerId = null
            }
        )
    }
    pendingSensorGraphWidgetContainerId?.let { target ->
        val sensors = allEntities.filter {
            it.entity_id.startsWith("sensor.") || it.entity_id.startsWith("number.") || it.entity_id.startsWith("input_number.")
        }
        AdvancedEntitySearchDialog(
            allEntities = sensors.ifEmpty { allEntities },
            title = stringResource(R.string.ui_select_sensors_5141d75),
            singleSelect = false,
            preselectedIds = emptySet(),
            onDismiss = {
                if (pendingSensorGraphWidgetContainerId != null) {
                    pendingSensorGraphWidgetContainerId = null
                    if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
                }
            },
            onEntitiesSelected = { ids ->
                if (ids.isNotEmpty()) {
                    val widget = HKISensorGraphWidget(id = UUID.randomUUID().toString(), entityIds = ids)
                    if (target == "__top__") viewModel.addWidgetToArea(areaId, widget)
                    else addChildToSwipingStack(target, widget)
                }
                pendingSensorGraphWidgetContainerId = null
            }
        )
    }
    pendingMediaPlayerWidgetContainerId?.let { target ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("media_player.") },
            title = stringResource(R.string.ui_select_media_player_73f4f5b),
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (pendingMediaPlayerWidgetContainerId != null) {
                    pendingMediaPlayerWidgetContainerId = null
                    if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
                }
            },
            onEntitiesSelected = { ids ->
                ids.firstOrNull()?.let { entityId ->
                    val widget = HKIMediaPlayerWidget(id = UUID.randomUUID().toString(), entityId = entityId)
                    if (target == "__top__") viewModel.addWidgetToArea(areaId, widget)
                    else addChildToSwipingStack(target, widget)
                }
                pendingMediaPlayerWidgetContainerId = null
            }
        )
    }
    pendingParcelsWidgetContainerId?.let { target ->
        ParcelDevicePickerDialog(viewModel, null, onDismiss = {
            pendingParcelsWidgetContainerId = null
            if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
        }) { deviceId ->
            if (deviceId != null) {
                val widget = HKIParcelsWidget(id = UUID.randomUUID().toString(), deviceIds = listOf(deviceId))
                if (target == "__top__") viewModel.addWidgetToArea(areaId, widget)
                else addChildToSwipingStack(target, widget)
            }
            pendingParcelsWidgetContainerId = null
        }
    }

    pendingWasteWidgetContainerId?.let { target ->
        WasteEntityPickerDialog(
            allEntities = allEntities,
            onDismiss = {
                if (pendingWasteWidgetContainerId != null) {
                    pendingWasteWidgetContainerId = null
                    if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
                }
            },
            onSelected = { ids ->
                val widget = newWasteWidget(ids)
                if (target == "__top__") {
                    viewModel.addWidgetToArea(areaId, widget)
                } else {
                    addChildToSwipingStack(target, widget)
                }
                pendingWasteWidgetContainerId = null
            }
        )
    }

    pendingCalendarWidgetContainerId?.let { target ->
        CalendarEntityPickerDialog(
            allEntities = allEntities,
            onDismiss = {
                if (pendingCalendarWidgetContainerId != null) {
                    pendingCalendarWidgetContainerId = null
                    if (target == "__top__") showAddWidgetDialog = true else addingToSwipingStackId = target
                }
            },
            onSelected = { ids ->
                val widget = newCalendarWidget(ids)
                if (target == "__top__") {
                    viewModel.addWidgetToArea(areaId, widget)
                } else {
                    addChildToSwipingStack(target, widget)
                }
                pendingCalendarWidgetContainerId = null
            }
        )
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
            onAddSpacerWidget = {
                addChildToSwipingStack(stackId, newSpacerWidget())
                addingToSwipingStackId = null
            },
            onAddAdaptiveLightingWidget = if (entityRegistry.any { it.platform == "adaptive_lighting" }) {
                {
                    addChildToSwipingStack(stackId, newAdaptiveLightingWidget())
                    addingToSwipingStackId = null
                }
            } else null,
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
            onAddCalendarWidget = {
                pendingCalendarWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddWasteWidget = {
                pendingWasteWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddParcelsWidget = {
                pendingParcelsWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddEnergyCard = { keys ->
                configuringEnergyCards = keys.map {
                    stackId to HKIEnergyCardWidget(id = UUID.randomUUID().toString(), cardKey = it, energyConfig = HKIEnergyConfig())
                }
                addingToSwipingStackId = null
            },
            onAddEnergyStack = { keys -> addChildToSwipingStack(stackId, HKIEnergyStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddClimateCard = { keys ->
                configuringClimateCards = keys.map {
                    stackId to HKIClimateCardWidget(id = UUID.randomUUID().toString(), cardKey = it)
                }
                addingToSwipingStackId = null
            },
            onAddClimateStack = { keys -> addChildToSwipingStack(stackId, HKIClimateStack(id = UUID.randomUUID().toString(), cardKeys = keys)) },
            onAddMediaPlayerWidget = {
                pendingMediaPlayerWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddMarkdownWidget = {
                addChildToSwipingStack(stackId, newMarkdownWidget())
                addingToSwipingStackId = null
            },
            onAddIframeWidget = {
                addChildToSwipingStack(stackId, newIframeWidget())
                addingToSwipingStackId = null
            },
            onAddSensorGraphWidget = {
                pendingSensorGraphWidgetContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddSensorGraphStack = {
                pendingSensorGraphStackContainerId = stackId
                addingToSwipingStackId = null
            },
            onAddBatteryCard = { useNotes ->
                addChildToSwipingStack(stackId, HKIBatteryCardWidget(id = UUID.randomUUID().toString(), useBatteryNotes = useNotes))
                addingToSwipingStackId = null
            },
            allEntities = allEntities
        )
    }

    pendingSingleWidgetKind?.let { kind ->
        if (kind == "vacuum") {
            VacuumWidgetSetupDialog(
                allEntities = allEntities,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
                onDismiss = {
                    val containerId = pendingSingleWidgetContainerId
                    pendingSingleWidgetKind = null
                    pendingSingleWidgetContainerId = null
                    if (containerId == null) showAddWidgetDialog = true else addingToSwipingStackId = containerId
                },
                onSelected = { entityId, config ->
                    val containerId = pendingSingleWidgetContainerId
                    if (containerId == null) {
                        viewModel.addSingleEntityWidgetToArea(areaId, kind, entityId, config)
                    } else {
                        addChildToSwipingStack(containerId, newSingleEntityWidget(kind, entityId, config))
                    }
                    pendingSingleWidgetKind = null
                    pendingSingleWidgetContainerId = null
                }
            )
            return@let
        }
        val candidates = when (kind) {
            "camera" -> allEntities.filter { it.entity_id.substringBefore(".").equals("camera", ignoreCase = true) }
            else -> allEntities
        }
        AdvancedEntitySearchDialog(
            allEntities = candidates,
            title = when (kind) {
                "camera" -> stringResource(R.string.cr_select_camera)
                else -> stringResource(R.string.cr_select_entity)
            },
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (pendingSingleWidgetKind != null) {
                    val containerId = pendingSingleWidgetContainerId
                    pendingSingleWidgetKind = null
                    pendingSingleWidgetContainerId = null
                    if (containerId == null) showAddWidgetDialog = true else addingToSwipingStackId = containerId
                }
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
            title = stringResource(R.string.ui_select_weather_2357e9d),
            singleSelect = true,
            preselectedIds = emptySet(),
            onDismiss = {
                if (!choosingWeatherWidgetStyle) {
                    val containerId = pendingWeatherWidgetContainerId
                    pendingWeatherWidgetContainerId = null
                    pendingWeatherWidgetEntityId = null
                    if (containerId == "__top__") showAddWidgetDialog = true else if (containerId != null) addingToSwipingStackId = containerId
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
            title = { Text(stringResource(R.string.ui_weather_type_fe0b7cc)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weatherWidgetStyleIds
                        .map { style -> style to weatherStyleLabel(style) }
                        .sortedBy { it.second }
                        .forEach { (style, label) ->
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
                OutlinedButton(onClick = {
                    choosingWeatherWidgetStyle = false
                    pendingWeatherWidgetEntityId = null
                }) { Text(stringResource(R.string.ui_back_b52b36b)) }
            }
        )
    }

    addingToNestedStack?.let { (swipingStackId, childStackId) ->
        val parentWidgets = areaWidgets.filterIsInstance<HKISwipingStack>().find { it.id == swipingStackId }?.widgets
            ?: areaWidgets.filterIsInstance<HKIEmptyStack>().find { it.id == swipingStackId }?.widgets
        val targetStack = parentWidgets?.filterIsInstance<HKIButtonStack>()?.find { it.id == childStackId }
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
        if (targetStack?.stackType == "vacuum") {
            AdvancedEntitySearchDialog(
                allEntities = allEntities.filter { it.entity_id.startsWith("vacuum.") },
                title = stringResource(R.string.ui_select_vacuums_7c2d22a),
                preselectedIds = targetStack.entityIds.toSet(),
                onDismiss = { addingToStackId = null },
                onEntitiesSelected = { entityIds ->
                    viewModel.updateWidget(
                        areaId,
                        targetStack.copy(entityIds = (targetStack.entityIds + entityIds).distinct())
                    )
                    addingToStackId = null
                }
            )
        } else if (targetStack?.stackType == "camera") {
            // Show choice dialog for camera stacks
            AlertDialog(
                onDismissRequest = { addingToStackId = null },
                title = { Text(stringResource(R.string.ui_add_camera_cf03528)) },
                text = { Text(stringResource(R.string.ui_would_you_like_to_add_an_existing_camera_entity_00d5bc3)) },
                confirmButton = {
                    Button(onClick = { cameraAddMode = "entity" }) { Text(stringResource(R.string.ui_existing_camera_5fb3653)) }
                },
                dismissButton = {
                    TextButton(onClick = { cameraAddMode = "custom" }) { Text(stringResource(R.string.ui_custom_url_9c155e9)) }
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
                },
                extraAction = if (targetStack != null && targetStack.stackType == "buttons") {
                    stringResource(R.string.spacer_add_empty_button) to {
                        viewModel.updateWidget(
                            areaId,
                            targetStack.copy(entityIds = targetStack.entityIds + newSpacerEntityId())
                        )
                        addingToStackId = null
                    }
                } else null
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
            onDismiss = { cameraAddMode = null },
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
        val customCameraLabel = stringResource(R.string.cr_custom_camera)
        AlertDialog(
            onDismissRequest = { cameraAddMode = null; customCameraUrl = "" },
            title = { Text(stringResource(R.string.ui_add_custom_camera_url_aab29ef)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customCameraUrl,
                        onValueChange = { customCameraUrl = it },
                        label = { Text(stringResource(R.string.ui_camera_url_0eebe87)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.ui_enter_http_url_relative_path_or_webrtc_id_e_bb736eb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        name = customCameraLabel,
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
                }) { Text(stringResource(R.string.ui_add_61cc55a)) }
            },
            dismissButton = { OutlinedButton(onClick = { cameraAddMode = null; customCameraUrl = "" }) { Text(stringResource(R.string.ui_back_b52b36b)) } }
        )
    }

    if (editingStack != null) {
        StackSettingsDialog(
            stack = editingStack!!,
            viewModel = viewModel,
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
            viewModel = viewModel,
            onDismiss = { selectedChildStackSettings = null },
            onUpdate = { updated ->
                updateChildInSwipingStack(containerId, updated)
                selectedChildStackSettings = null
            }
        )
    }

    orderingStack?.let { (containerId, stack) ->
        StackOrderDialog(
            stack = stack,
            allEntities = allEntities,
            onDismiss = { orderingStack = null },
            onSave = { orderedIds, updatedConfigs ->
                if (containerId == null) {
                    val latest = areaWidgets.filterIsInstance<HKIButtonStack>().find { it.id == stack.id } ?: stack
                    viewModel.updateWidget(areaId, latest.copy(entityIds = orderedIds, buttonConfigs = updatedConfigs))
                } else {
                    updateChildInSwipingStack(containerId, stack.copy(entityIds = orderedIds, buttonConfigs = updatedConfigs))
                }
                orderingStack = null
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
            viewModel = viewModel,
            isCameraItem = widget.kind == "camera",
            isVacuumItem = widget.kind == "vacuum" || widget.entityId.startsWith("vacuum."),
            allEntities = allEntities,
            areas = areas,
            entityRegistry = entityRegistry,
            deviceRegistry = deviceRegistry,
            onDismiss = { selectedSingleWidgetSettings = null },
            widgetAppearance = WidgetAppearance(widget.isSquare, widget.cornerRadius, widget.width, widget.buttonStyle),
            onSaveWithAppearance = { config, a ->
                if (containerId == null) {
                    val latest = areaWidgets.filterIsInstance<HKISingleEntityWidget>().find { it.id == widget.id } ?: widget
                    viewModel.updateWidget(
                        areaId,
                        latest.copy(config = config, isSquare = a.isSquare, cornerRadius = a.cornerRadius, width = a.width, buttonStyle = a.buttonStyle)
                    )
                } else {
                    updateChildInSwipingStack(
                        containerId,
                        widget.copy(config = config, isSquare = a.isSquare, cornerRadius = a.cornerRadius, width = a.width, buttonStyle = a.buttonStyle)
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
                viewModel = viewModel,
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = allEntities,
                areas = areas,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
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
                viewModel = viewModel,
                isCameraItem = stack.stackType == "camera",
                isVacuumItem = stack.stackType == "vacuum" || entityId.startsWith("vacuum."),
                allEntities = allEntities,
                areas = areas,
                entityRegistry = entityRegistry,
                deviceRegistry = deviceRegistry,
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
        val fallbackEntityUrl = if (selectedCameraId?.startsWith("camera.") == true) {
            "${currentUrl.removeSuffix("/")}/api/camera_proxy_stream/$selectedCameraId"
        } else {
            null
        }
        val streamUrl = config?.cameraUrl?.takeIf { it.isNotBlank() }
            ?: resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
            ?: fallbackEntityUrl
        val label = config?.name ?: entity?.friendlyName ?: selectedCameraId ?: stringResource(R.string.ui_camera_4da9c9a)
        val liveWebUrl = when {
            entity != null -> resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
            fallbackEntityUrl != null -> fallbackEntityUrl
            else -> buildWebRtcApiUrl(config?.cameraUrl, currentUrl)
        }
        val cameraIds = selectedCameraStack!!.entityIds
        val cameraIndex = cameraIds.indexOf(selectedCameraId)
        val hasCameraNavigation = cameraIds.size > 1 && cameraIndex >= 0

        HKICameraDialog(
            title = label,
            imageUrl = resolveCameraUrl(streamUrl, currentUrl),
            liveWebUrl = liveWebUrl,
            authToken = accessToken,
            statusText = stringResource(R.string.cr_live),
            entity = entity,
            viewModel = viewModel,
            onPrevious = if (hasCameraNavigation) {
                { selectedCameraId = cameraIds[(cameraIndex - 1 + cameraIds.size) % cameraIds.size] }
            } else null,
            onNext = if (hasCameraNavigation) {
                { selectedCameraId = cameraIds[(cameraIndex + 1) % cameraIds.size] }
            } else null,
            positionText = if (hasCameraNavigation) "${cameraIndex + 1} / ${cameraIds.size}" else null,
            onDismiss = { selectedCameraId = null; selectedCameraStack = null }
        )
    }

    // Custom nav-bar buttons + navigation for whichever config-based entity dialog is open (only one
    // is ever shown at a time, so this matches the visible dialog). Provided once for the block below.
    val activeDialogCustomButtons = (when {
        showGenericDialog -> selectedGenericConfig
        showLightDialog -> selectedLightConfig
        showFanDialog -> selectedFanConfig
        showHumidifierDialog -> selectedHumidifierConfig
        showAlarmDialog -> selectedAlarmConfig
        else -> null
    })?.customButtons ?: emptyList()
    CompositionLocalProvider(
        LocalDialogCustomButtons provides activeDialogCustomButtons,
        LocalDialogNavController provides navController
    ) {
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
            onDismiss = { showFanDialog = false; selectedFanConfig = null }
        )
    }

    if (activityDialogRole != null && activityDialogEntityIds.isNotEmpty()) {
        val role = activityDialogRole!!
        val groupEntities = activityDialogEntityIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
        val groupTitle = com.jimz011apps.hki7.ui.components.roomStatusGroupTitle(role)
        val syntheticStack = remember(role, activityDialogEntityIds, groupTitle) {
            HKIButtonStack(
                id = "room-status-$role",
                title = groupTitle,
                icon = com.jimz011apps.hki7.ui.components.roomStatusMdiSlug(role),
                entityIds = activityDialogEntityIds
            )
        }
        val closeActivity = { activityDialogRole = null; activityDialogEntityIds = emptyList() }
        if (groupEntities.isNotEmpty()) {
            GroupEntityDialog(
                stack = syntheticStack,
                entities = groupEntities,
                viewModel = viewModel,
                onDismiss = closeActivity
            )
        } else closeActivity()
    }

    if (showHumidifierDialog && selectedHumidifierEntity != null) {
        val entity = allEntities.find { it.entity_id == selectedHumidifierEntity!!.entity_id } ?: selectedHumidifierEntity!!
        HKIHumidifierDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = selectedHumidifierConfig?.name,
            iconName = selectedHumidifierConfig?.icon,
            fanEntity = selectedHumidifierConfig?.humidifierFanEntityId?.let { id -> allEntities.find { it.entity_id == id } },
            auxEntities = selectedHumidifierConfig?.humidifierAuxEntityIds.orEmpty().mapNotNull { (k, id) -> allEntities.find { it.entity_id == id }?.let { k to it } }.toMap(),
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
            liveWebUrl = streamUrl,
            authToken = accessToken,
            statusText = stringResource(R.string.cr_live),
            entity = entity,
            viewModel = viewModel,
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
            onDismiss = { showGenericDialog = false; selectedGenericConfig = null }
        )
    }
    }

    selectedMediaPlayerId?.let { id ->
        val player = allEntities.find { it.entity_id == id }
        if (player != null) {
            com.jimz011apps.hki7.ui.components.HKIMediaPlayerDialog(player, viewModel, currentUrl) { selectedMediaPlayerId = null }
        } else {
            selectedMediaPlayerId = null
        }
    }

    selectedVacuumEntityId?.let { vId ->
        // Open the vacuum dialog with all vacuums in the same stack (swipe between them), using per-entity config.
        val nestedWidgets = areaWidgets.filterIsInstance<HKISwipingStack>().flatMap { it.widgets } +
            areaWidgets.filterIsInstance<HKIEmptyStack>().flatMap { it.widgets }
        val stack = (areaWidgets + nestedWidgets).filterIsInstance<HKIButtonStack>().find { it.entityIds.contains(vId) }
        val single = (areaWidgets + nestedWidgets).filterIsInstance<HKISingleEntityWidget>().find { it.entityId == vId }
        val vacEntities = (stack?.entityIds ?: listOf(vId))
            .filter { it.startsWith("vacuum.") }
            .mapNotNull { id -> allEntities.find { it.entity_id == id } }
        if (vacEntities.isNotEmpty()) {
            VacuumStackDialog(
                entities = vacEntities,
                startIndex = vacEntities.indexOfFirst { it.entity_id == vId }.coerceAtLeast(0),
                buttonConfigs = stack?.buttonConfigs ?: single?.let { mapOf(vId to it.config) } ?: emptyMap(),
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

/** One entry in the Add-Widget picker; [keywords] add extra terms that the search field matches on. */
private data class PickerWidget(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val keywords: String,
    val category: WidgetPickerCategory,
    val isStack: Boolean = false,
    val onSelect: () -> Unit
) {
    fun matches(query: String, title: String, subtitle: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            subtitle.contains(query, ignoreCase = true) ||
            keywords.contains(query, ignoreCase = true)
}

private enum class WidgetPickerCategory(
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    CONTROLS(
        "controls",
        R.string.cr_widget_category_controls,
        R.string.cr_widget_category_controls_description,
        Icons.Default.Lightbulb
    ),
    CAMERAS(
        "cameras",
        R.string.cr_widget_category_cameras,
        R.string.cr_widget_category_cameras_description,
        Icons.Default.CameraAlt
    ),
    CLIMATE(
        "climate",
        R.string.cr_widget_category_climate,
        R.string.cr_widget_category_climate_description,
        Icons.Default.WbSunny
    ),
    ENERGY(
        "energy",
        R.string.cr_widget_category_energy,
        R.string.cr_widget_category_energy_description,
        Icons.Default.ElectricBolt
    ),
    INFORMATION(
        "information",
        R.string.cr_widget_category_information,
        R.string.cr_widget_category_information_description,
        Icons.AutoMirrored.Filled.Notes
    ),
    LAYOUT(
        "layout",
        R.string.cr_widget_category_layout,
        R.string.cr_widget_category_layout_description,
        Icons.AutoMirrored.Filled.ViewQuilt
    )
}

private fun categoryGroup(category: WidgetPickerCategory): String = "category:${category.key}"

private fun categoryFromGroup(group: String?): WidgetPickerCategory? {
    val key = group?.removePrefix("category:")?.takeIf { group.startsWith("category:") } ?: return null
    return WidgetPickerCategory.entries.firstOrNull { it.key == key }
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
    /** Adds an empty (transparent) button: a placeholder that only reserves layout space. */
    onAddSpacerWidget: (() -> Unit)? = null,
    onAddAdaptiveLightingWidget: (() -> Unit)? = null,
    onAddCameraWidget: (() -> Unit)? = null,
    onAddVacuumWidget: (() -> Unit)? = null,
    onAddWeatherWidget: (() -> Unit)? = null,
    onAddCalendarWidget: (() -> Unit)? = null,
    onAddWasteWidget: (() -> Unit)? = null,
    onAddParcelsWidget: (() -> Unit)? = null,
    onAddEnergyCard: ((List<String>) -> Unit)? = null,
    onAddEnergyStack: ((List<String>) -> Unit)? = null,
    onAddClimateCard: ((List<String>) -> Unit)? = null,
    onAddClimateStack: ((List<String>) -> Unit)? = null,
    onAddMediaPlayerWidget: (() -> Unit)? = null,
    onAddMarkdownWidget: (() -> Unit)? = null,
    onAddIframeWidget: (() -> Unit)? = null,
    onAddSensorGraphWidget: (() -> Unit)? = null,
    onAddSensorGraphStack: (() -> Unit)? = null,
    onAddBatteryCard: ((Boolean) -> Unit)? = null,
    allEntities: List<HAEntity> = emptyList()
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val buttonsDefault = stringResource(R.string.cr_widget_buttons)
    val camerasDefault = stringResource(R.string.cr_widget_cameras)
    val vacuumDefault = stringResource(R.string.cr_widget_vacuum)
    val headerTextDefault = stringResource(R.string.cr_widget_header_text)
    val weatherDefault = stringResource(R.string.cr_widget_weather)
    var stackTitle by remember { mutableStateOf(buttonsDefault) }
    var stackIcon by remember { mutableStateOf("Lightbulb") }
    var cameraTitle by remember { mutableStateOf(camerasDefault) }
    var cameraIcon by remember { mutableStateOf("CameraAlt") }
    var energyPickerSelection by remember { mutableStateOf<List<String>>(emptyList()) }
    var climatePickerSelection by remember { mutableStateOf<List<String>>(emptyList()) }
    var vacuumTitle by remember { mutableStateOf(vacuumDefault) }
    var vacuumIcon by remember { mutableStateOf("CleaningServices") }
    var headerText by remember { mutableStateOf(headerTextDefault) }
    var headerIcon by remember { mutableStateOf("None") }
    var weatherTitle by remember { mutableStateOf(weatherDefault) }
    var weatherIcon by remember { mutableStateOf("weather-partly-cloudy") }
    var batteryNotesInstalled by remember { mutableStateOf(false) }
    var configureWidget by remember { mutableStateOf<String?>(null) }
    var widgetGroup by remember { mutableStateOf<String?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    // Single source of truth for the picker: the default view groups these, the search field
    // flattens them. Keywords add synonyms that aren't in the visible title/description.
    // Both groups are shown alphabetically by title.
    val topWidgets = buildList {
        if (onAddAdaptiveLightingWidget != null) add(
            PickerWidget(
                Icons.Default.LightMode,
                R.string.cr_widget_adaptive_lighting,
                R.string.cr_widget_adaptive_lighting_description,
                "circadian profile brightness color temperature sleep adapt",
                WidgetPickerCategory.CONTROLS
            ) { onAddAdaptiveLightingWidget.invoke(); onDismiss() }
        )
        add(PickerWidget(Icons.Default.Lightbulb, R.string.cr_widget_button, R.string.cr_widget_button_description, "toggle switch control single entity light", WidgetPickerCategory.CONTROLS) { onAddButtonWidget?.invoke() ?: run { stackTitle = ""; stackIcon = "None"; configureWidget = "button" } })
        if (onAddCalendarWidget != null) add(PickerWidget(Icons.Default.CalendarMonth, R.string.cr_widget_calendar, R.string.cr_widget_calendar_description, "events schedule date agenda month week appointments", WidgetPickerCategory.INFORMATION) { onAddCalendarWidget.invoke(); onDismiss() })
        if (onAddWasteWidget != null) add(PickerWidget(Icons.Default.DeleteSweep, R.string.cr_widget_waste_collection, R.string.cr_widget_waste_collection_description, "waste afval trash garbage gft pmd papier rest collection pickup afvalbeheer", WidgetPickerCategory.INFORMATION) { onAddWasteWidget.invoke(); onDismiss() })
        if (onAddParcelsWidget != null) add(PickerWidget(Icons.Default.LocalShipping, R.string.cr_widget_parcels, R.string.cr_widget_parcels_description, "packages delivery mail carrier tracking shipment", WidgetPickerCategory.INFORMATION) { onAddParcelsWidget.invoke(); onDismiss() })
        add(PickerWidget(Icons.Default.CameraAlt, R.string.cr_widget_camera, R.string.cr_widget_camera_description, "video stream live cctv feed", WidgetPickerCategory.CAMERAS) { onAddCameraWidget?.invoke() ?: run { cameraTitle = ""; cameraIcon = "None"; configureWidget = "camera" } })
        if (onAddClimateCard != null) add(PickerWidget(Icons.Default.Thermostat, R.string.cr_widget_climate_card, R.string.cr_widget_climate_card_description, "climate temperature humidity thermostat heating cooling sensors air", WidgetPickerCategory.CLIMATE) { climatePickerSelection = emptyList(); widgetGroup = "climate_card" })
        if (onAddEnergyCard != null) add(PickerWidget(Icons.Default.ElectricBolt, R.string.cr_widget_energy_card, R.string.cr_widget_energy_card_description, "power usage solar gas water consumption electricity", WidgetPickerCategory.ENERGY) { energyPickerSelection = emptyList(); widgetGroup = "energy_card" })
        if (onAddBatteryCard != null) add(PickerWidget(Icons.Default.BatteryAlert, R.string.cr_widget_battery_levels, R.string.cr_widget_battery_levels_description, "battery notes charge level power", WidgetPickerCategory.ENERGY) { widgetGroup = "battery_card" })
        if (onAddSpacerWidget != null) add(PickerWidget(Icons.Default.CropSquare, R.string.spacer_empty_button, R.string.spacer_empty_button_description, "empty blank spacer placeholder transparent gap align", WidgetPickerCategory.LAYOUT) { onAddSpacerWidget.invoke(); onDismiss() })
        add(PickerWidget(Icons.AutoMirrored.Filled.ShortText, R.string.cr_widget_header_text, R.string.cr_widget_header_text_description, "title subtitle label heading text divider", WidgetPickerCategory.LAYOUT) { configureWidget = "header" })
        if (onAddMarkdownWidget != null) add(PickerWidget(Icons.AutoMirrored.Filled.Notes, R.string.cr_widget_markdown, R.string.cr_widget_markdown_description, "text note markdown card content notes writing", WidgetPickerCategory.INFORMATION) { onAddMarkdownWidget.invoke(); onDismiss() })
        if (onAddIframeWidget != null) add(PickerWidget(Icons.Default.Public, R.string.cr_widget_iframe, R.string.cr_widget_iframe_description, "web page website url embed browser iframe html", WidgetPickerCategory.INFORMATION) { onAddIframeWidget.invoke(); onDismiss() })
        if (onAddMediaPlayerWidget != null) add(PickerWidget(Icons.Default.MusicNote, R.string.cr_widget_media_player, R.string.cr_widget_media_player_description, "music speaker sonos spotify tv cast album art player", WidgetPickerCategory.INFORMATION) { onAddMediaPlayerWidget.invoke(); onDismiss() })
        if (onAddSensorGraphWidget != null) add(PickerWidget(Icons.AutoMirrored.Filled.ShowChart, R.string.cr_widget_sensor_graph, R.string.cr_widget_sensor_graph_description, "graph chart history sensor temperature humidity line bar plot statistics", WidgetPickerCategory.INFORMATION) { onAddSensorGraphWidget.invoke(); onDismiss() })
        add(PickerWidget(Icons.Default.CleaningServices, R.string.cr_widget_vacuum, R.string.cr_widget_vacuum_description, "robot cleaner mop hoover", WidgetPickerCategory.CONTROLS) { onAddVacuumWidget?.invoke() ?: run { vacuumTitle = ""; vacuumIcon = "None"; configureWidget = "vacuum" } })
        add(PickerWidget(Icons.Default.WbSunny, R.string.cr_widget_weather, R.string.cr_widget_weather_description, "forecast temperature conditions rain sun", WidgetPickerCategory.CLIMATE) { onAddWeatherWidget?.invoke() ?: run { weatherTitle = ""; weatherIcon = "None"; configureWidget = "weather" } })
    }.sortedBy { context.getString(it.titleRes).lowercase(Locale.getDefault()) }
    val stackWidgets = buildList {
        if (onAddEmptyStack != null) add(PickerWidget(Icons.AutoMirrored.Filled.ViewQuilt, R.string.cr_widget_empty_stack, R.string.cr_widget_empty_stack_description, "container group blank custom", WidgetPickerCategory.LAYOUT, isStack = true) { onAddEmptyStack.invoke(); onDismiss() })
        add(PickerWidget(Icons.AutoMirrored.Filled.ViewQuilt, R.string.cr_widget_button_stack, R.string.cr_widget_button_stack_description, "buttons group entities controls", WidgetPickerCategory.CONTROLS, isStack = true) { stackTitle = buttonsDefault; stackIcon = "Lightbulb"; configureWidget = "button_stack" })
        add(PickerWidget(Icons.Default.CameraAlt, R.string.cr_widget_camera_stack, R.string.cr_widget_camera_stack_description, "cameras group video stream", WidgetPickerCategory.CAMERAS, isStack = true) { cameraTitle = camerasDefault; cameraIcon = "CameraAlt"; configureWidget = "camera_stack" })
        if (onAddClimateStack != null) add(PickerWidget(Icons.Default.Thermostat, R.string.cr_widget_climate_stack, R.string.cr_widget_climate_stack_description, "climate group temperature humidity thermostat sensors", WidgetPickerCategory.CLIMATE, isStack = true) { climatePickerSelection = emptyList(); widgetGroup = "climate_stack" })
        if (onAddEnergyStack != null) add(PickerWidget(Icons.AutoMirrored.Filled.ViewQuilt, R.string.cr_widget_energy_stack, R.string.cr_widget_energy_stack_description, "power group solar gas water usage", WidgetPickerCategory.ENERGY, isStack = true) { energyPickerSelection = emptyList(); widgetGroup = "energy_stack" })
        if (onAddSensorGraphStack != null) add(PickerWidget(Icons.AutoMirrored.Filled.ShowChart, R.string.cr_widget_sensor_graph_stack, R.string.cr_widget_sensor_graph_stack_description, "graph chart group history sensor statistics", WidgetPickerCategory.INFORMATION, isStack = true) { onAddSensorGraphStack.invoke(); onDismiss() })
        if (onAddSwipingStack != null) add(PickerWidget(Icons.AutoMirrored.Filled.ViewQuilt, R.string.cr_widget_swiping_stack, R.string.cr_widget_swiping_stack_description, "carousel swipe pager horizontal nested", WidgetPickerCategory.LAYOUT, isStack = true) { onAddSwipingStack.invoke(); onDismiss() })
        add(PickerWidget(Icons.Default.CleaningServices, R.string.cr_widget_vacuum_stack, R.string.cr_widget_vacuum_stack_description, "robot cleaner group map", WidgetPickerCategory.CONTROLS, isStack = true) { vacuumTitle = vacuumDefault; vacuumIcon = "CleaningServices"; configureWidget = "vacuum_stack" })
        add(PickerWidget(Icons.Default.WbSunny, R.string.cr_widget_weather_stack, R.string.cr_widget_weather_stack_description, "forecast group wind rain conditions", WidgetPickerCategory.CLIMATE, isStack = true) { weatherTitle = weatherDefault; weatherIcon = "weather-partly-cloudy"; configureWidget = "weather_stack" })
    }.sortedBy { context.getString(it.titleRes).lowercase(Locale.getDefault()) }

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

    val widgetPickerScroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false)
    ) {
        androidx.activity.compose.BackHandler {
            when {
                configureWidget != null -> configureWidget = null
                widgetGroup == "energy_stack" || widgetGroup == "energy_card" || widgetGroup == "battery_card" ->
                    widgetGroup = categoryGroup(WidgetPickerCategory.ENERGY)
                widgetGroup == "climate_stack" || widgetGroup == "climate_card" ->
                    widgetGroup = categoryGroup(WidgetPickerCategory.CLIMATE)
                widgetGroup != null -> widgetGroup = null
                else -> onDismiss()
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = appColors.elevated,
                contentColor = appColors.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(min = 520.dp, max = 560.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                appColors.elevated,
                                appColors.elevated
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModernSettingsHeader(
                    title = when {
                        configureWidget != null -> stringResource(R.string.cr_configure_widget)
                        categoryFromGroup(widgetGroup) != null ->
                            stringResource(categoryFromGroup(widgetGroup)!!.labelRes)
                        widgetGroup != null -> stringResource(R.string.cr_widget_options)
                        else -> stringResource(R.string.cr_add_widget)
                    },
                    subtitle = when {
                        configureWidget != null -> stringResource(R.string.cr_configure_widget_subtitle)
                        categoryFromGroup(widgetGroup) != null ->
                            stringResource(categoryFromGroup(widgetGroup)!!.descriptionRes)
                        widgetGroup != null -> stringResource(R.string.cr_widget_options_subtitle)
                        else -> stringResource(R.string.cr_add_widget_subtitle)
                    },
                    icon = categoryFromGroup(widgetGroup)?.icon ?: Icons.Default.Add,
                    canGoBack = configureWidget != null || widgetGroup != null,
                    onBack = {
                        when {
                            configureWidget != null -> configureWidget = null
                            widgetGroup == "energy_stack" || widgetGroup == "energy_card" || widgetGroup == "battery_card" -> widgetGroup = categoryGroup(WidgetPickerCategory.ENERGY)
                            widgetGroup == "climate_stack" || widgetGroup == "climate_card" -> widgetGroup = categoryGroup(WidgetPickerCategory.CLIMATE)
                            else -> widgetGroup = null
                        }
                    },
                    onClose = onDismiss
                )

                if (configureWidget == null && widgetGroup == null) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(stringResource(R.string.ui_search_widgets_9525879)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = appColors.onMuted) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ui_clear_search_67300d0), tint = appColors.onMuted)
                                }
                            }
                        },
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
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (configureWidget == null && widgetGroup == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(widgetPickerScroll)
                                .verticalScroll(widgetPickerScroll),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    val query = search.trim()
                    if (query.isEmpty()) {
                        val availableWidgets = topWidgets + stackWidgets
                        WidgetPickerCategory.entries.forEach { category ->
                            val count = availableWidgets.count { it.category == category }
                            if (count > 0) {
                            WidgetChoice(
                                    icon = category.icon,
                                    title = stringResource(category.labelRes),
                                    subtitle = stringResource(
                                        R.string.cr_category_with_options,
                                        stringResource(category.descriptionRes),
                                        pluralStringResource(R.plurals.cr_widget_option_count, count, count)
                                    ),
                                    onClick = { widgetGroup = categoryGroup(category) }
                            )
                            }
                        }
                    } else {
                        val results = (topWidgets + stackWidgets)
                            .filter {
                                it.matches(
                                    query,
                                    context.getString(it.titleRes),
                                    context.getString(it.subtitleRes)
                                )
                            }
                            .sortedBy { context.getString(it.titleRes).lowercase(Locale.getDefault()) }
                        if (results.isEmpty()) {
                            Text(
                                stringResource(R.string.ui_no_widgets_match_73a3c20, query),
                                color = appColors.onMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        } else {
                            results.forEach { w ->
                                WidgetChoice(
                                    w.icon,
                                    stringResource(w.titleRes),
                                    stringResource(w.subtitleRes),
                                    w.onSelect
                                )
                            }
                        }
                    }
                        }
                } else if (categoryFromGroup(widgetGroup) != null && configureWidget == null) {
                        val category = categoryFromGroup(widgetGroup)!!
                        val categoryWidgets = (topWidgets + stackWidgets).filter { it.category == category }
                        val (stacks, individual) = categoryWidgets.partition(PickerWidget::isStack)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(widgetPickerScroll)
                                .verticalScroll(widgetPickerScroll),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (individual.isNotEmpty()) {
                                SettingsSubcategory(stringResource(R.string.ui_individual_widgets_ff1526f), stringResource(R.string.ui_add_one_focused_card_c2f2ed8))
                                individual.forEach { widget ->
                                    WidgetChoice(
                                        widget.icon,
                                        stringResource(widget.titleRes),
                                        stringResource(widget.subtitleRes),
                                        widget.onSelect
                                    )
                                }
                            }
                            if (stacks.isNotEmpty()) {
                                SettingsSubcategory(stringResource(R.string.ui_stacks_249f056), stringResource(R.string.ui_group_related_cards_in_one_section_f978d2b))
                                stacks.forEach { widget ->
                                    WidgetChoice(
                                        widget.icon,
                                        stringResource(widget.titleRes),
                                        stringResource(widget.subtitleRes),
                                        widget.onSelect
                                    )
                                }
                            }
                        }
                } else if (widgetGroup == "energy_card" && configureWidget == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(stringResource(R.string.ui_energy_cards_c84abda), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    EnergyCardPickerList(
                        selected = energyPickerSelection,
                        onToggle = { key ->
                            energyPickerSelection = if (key in energyPickerSelection) energyPickerSelection - key else energyPickerSelection + key
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                } else if (widgetGroup == "energy_stack" && configureWidget == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(stringResource(R.string.ui_energy_stack_cards_5ac4b4c), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    EnergyCardPickerList(
                        selected = energyPickerSelection,
                        onToggle = { key ->
                            energyPickerSelection = if (key in energyPickerSelection) energyPickerSelection - key else energyPickerSelection + key
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                } else if (widgetGroup == "climate_card" && configureWidget == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(stringResource(R.string.ui_climate_cards_f4996fd), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    ClimateCardPickerList(
                        selected = climatePickerSelection,
                        onToggle = { key ->
                            climatePickerSelection = if (key in climatePickerSelection) climatePickerSelection - key else climatePickerSelection + key
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                } else if (widgetGroup == "climate_stack" && configureWidget == null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(stringResource(R.string.ui_climate_stack_cards_dcff178), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    ClimateCardPickerList(
                        selected = climatePickerSelection,
                        onToggle = { key ->
                            climatePickerSelection = if (key in climatePickerSelection) climatePickerSelection - key else climatePickerSelection + key
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                } else if (widgetGroup == "battery_card" && configureWidget == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(widgetPickerScroll)
                                .verticalScroll(widgetPickerScroll),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(stringResource(R.string.ui_battery_levels_7c61b65), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_battery_battery_notes_only_14e5936), color = appColors.onSurface)
                            Text(stringResource(R.string.ui_only_show_battery_entities_and_include_battery_type_detail_b216867), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = batteryNotesInstalled, onCheckedChange = { batteryNotesInstalled = it })
                    }
                    }
                } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(widgetPickerScroll)
                                .verticalScroll(widgetPickerScroll),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                    Text(
                        when (configureWidget) {
                            "button", "button_stack" -> if (configureWidget == "button") stringResource(R.string.ui_button_794145f) else stringResource(R.string.ui_button_stack_ee9e7f8)
                            "camera", "camera_stack" -> if (configureWidget == "camera") stringResource(R.string.ui_camera_4da9c9a) else stringResource(R.string.ui_camera_stack_645ea0f)
                            "vacuum", "vacuum_stack" -> if (configureWidget == "vacuum") stringResource(R.string.ui_vacuum_5fe15df) else stringResource(R.string.ui_vacuum_stack_b93d3b7)
                            "weather", "weather_stack" -> if (configureWidget == "weather") stringResource(R.string.ui_weather_284af3e) else stringResource(R.string.ui_weather_stack_4d4f763)
                            else -> stringResource(R.string.ui_header_text_f7593e7)
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
                        label = { Text(if (configureWidget == "header") stringResource(R.string.ui_header_text_7f71314) else stringResource(R.string.ui_stack_title_46ccc2d)) },
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
                    Text(stringResource(R.string.ui_icon_716f63b), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
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
                            currentWidgetIcon.takeUnless { it == "None" } ?: stringResource(R.string.cr_none),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                        }
                    }
                }

                if (widgetGroup == "energy_card" && configureWidget == null) {
                    Button(
                        onClick = { onAddEnergyCard?.invoke(energyPickerSelection); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                } else if (widgetGroup == "energy_stack" && configureWidget == null) {
                    Button(
                        onClick = { onAddEnergyStack?.invoke(energyPickerSelection); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                } else if (widgetGroup == "climate_card" && configureWidget == null) {
                    Button(
                        onClick = { onAddClimateCard?.invoke(climatePickerSelection); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                } else if (widgetGroup == "climate_stack" && configureWidget == null) {
                    Button(
                        onClick = { onAddClimateStack?.invoke(climatePickerSelection); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                } else if (widgetGroup == "battery_card" && configureWidget == null) {
                    Button(
                        onClick = { onAddBatteryCard?.invoke(batteryNotesInstalled); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                } else if (configureWidget != null) {
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
                                onAddSubtitle(headerText.ifBlank { headerTextDefault }, headerIcon.takeUnless { it == "None" })
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ui_add_61cc55a))
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ModernSettingsMenuItem(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
}

@Composable
private fun VacuumBindingRow(
    entityId: String?,
    allEntities: List<HAEntity>,
    onChange: () -> Unit,
    onClear: () -> Unit
) {
    val name = entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name ?: stringResource(R.string.ui_auto_unavailable_33737ba), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onChange) { Text(stringResource(R.string.ui_change_64fbd99)) }
        if (entityId != null) TextButton(onClick = onClear) { Text(stringResource(R.string.ui_clear_719ea39)) }
    }
}

/** Shape/size of a standalone (non-stacked) widget, edited from its button settings dialog. */
data class WidgetAppearance(val isSquare: Boolean, val cornerRadius: Int, val width: String, val buttonStyle: String = "")

@Composable
fun ButtonConfigDialog(
    entity: HAEntity?,
    config: HKIButtonConfig,
    viewModel: MainViewModel,
    isCameraItem: Boolean = false,
    isVacuumItem: Boolean = false,
    allEntities: List<HAEntity> = emptyList(),
    areas: List<HAArea> = emptyList(),
    entityRegistry: List<HAEntityRegistryEntry> = emptyList(),
    deviceRegistry: List<HADeviceRegistryEntry> = emptyList(),
    onDismiss: () -> Unit,
    // Standalone widgets only: shape/roundness/width are edited here since there is no stack
    // to inherit them from. When set, Save reports through onSaveWithAppearance instead.
    widgetAppearance: WidgetAppearance? = null,
    onSaveWithAppearance: ((HKIButtonConfig, WidgetAppearance) -> Unit)? = null,
    onSave: (HKIButtonConfig) -> Unit
) {
    var appearIsSquare by remember(widgetAppearance) { mutableStateOf(widgetAppearance?.isSquare ?: true) }
    var appearButtonStyle by remember(widgetAppearance) {
        mutableStateOf(widgetAppearance?.buttonStyle?.takeIf { it.isNotBlank() } ?: if (widgetAppearance?.isSquare == true) "square" else "standard")
    }
    var appearRadius by remember(widgetAppearance) { mutableIntStateOf(widgetAppearance?.cornerRadius ?: 28) }
    var appearWidth by remember(widgetAppearance) { mutableStateOf(widgetAppearance?.width ?: "half") }
    var name by remember(config) { mutableStateOf(config.name ?: entity?.friendlyName ?: entity?.entity_id ?: "") }
    var stateAttribute by remember(config) { mutableStateOf(config.stateAttribute) }
    var stateUnit by remember(config) { mutableStateOf(config.stateUnit) }
    var stateAsTimer by remember(config) { mutableStateOf(config.stateAsTimer) }
    var timerStateEntityId by remember(config) { mutableStateOf(config.timerStateEntityId) }
    var showTimerStatePicker by remember { mutableStateOf(false) }
    var cameraUrl by remember(config) { mutableStateOf(config.cameraUrl ?: "") }
    var refreshInterval by remember(config) { mutableIntStateOf(config.cameraRefreshInterval) }
    var iconName by remember(config) { mutableStateOf(config.icon ?: "None") }
    var iconAnimation by remember(config) { mutableStateOf(config.iconAnimation) }
    var visSpec by remember(config) { mutableStateOf(config.toVisibilitySpec()) }
    val isLightEntity = entity?.entity_id?.startsWith("light.") == true
    var iconOnly by remember(config) { mutableStateOf(config.iconOnly) }
    var showBrightnessSlider by remember(config) { mutableStateOf(config.showBrightnessSlider) }
    var tapAction by remember(config) { mutableStateOf(config.tapActionEx ?: HKIAction(type = config.tapAction)) }
    var doubleAction by remember(config) { mutableStateOf(config.doubleTapActionEx ?: HKIAction(type = config.doubleTapAction)) }
    var holdAction by remember(config) { mutableStateOf(config.holdActionEx ?: HKIAction(type = config.holdAction)) }
    var customButtons by remember(config) { mutableStateOf(config.customButtons) }
    var lockEnabled by remember(config) { mutableStateOf(config.lockEnabled) }
    var lockUnlockMode by remember(config) {
        mutableStateOf(config.lockUnlockMode.takeIf { it == BUTTON_LOCK_PIN || it == BUTTON_LOCK_DOUBLE_TAP } ?: BUTTON_LOCK_DOUBLE_TAP)
    }
    var lockPin by remember(config) { mutableStateOf(config.lockPin ?: "") }
    var lockRelockSecondsText by remember(config) {
        mutableStateOf(config.lockRelockSeconds.coerceAtLeast(5).toString())
    }
    // Lock door sensor
    val isLockEntity = entity?.entity_id?.startsWith("lock.") == true
    val isHumidifierEntity = entity?.entity_id?.startsWith("humidifier.") == true
    var humidifierFanEntityId by remember(config) { mutableStateOf(config.humidifierFanEntityId) }
    var humidifierDeviceId by remember(config) { mutableStateOf(config.humidifierDeviceId) }
    var humidifierAuxEntityIds by remember(config) { mutableStateOf(config.humidifierAuxEntityIds) }
    var doorEntityId by remember(config) { mutableStateOf(config.doorEntityId) }
    var showDoorPicker by remember { mutableStateOf(false) }
    var showIconPickerBtn by remember { mutableStateOf(false) }
    // Climate temp/humidity sensors
    val isClimateEntity = entity?.entity_id?.startsWith("climate.") == true
    var climateTempSensorEntityId by remember(config) { mutableStateOf(config.climateTempSensorEntityId) }
    var climateHumiditySensorEntityId by remember(config) { mutableStateOf(config.climateHumiditySensorEntityId) }
    var climateDialogControl by remember(config) { mutableStateOf(config.climateDialogControl) }
    var showTempSensorPicker by remember { mutableStateOf(false) }
    var showHumiditySensorPicker by remember { mutableStateOf(false) }

    if (showIconPickerBtn) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPickerBtn = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPickerBtn = false
            },
            allowEntityPicture = true
        )
    }

    // Vacuum settings (inline; pickers hoisted below the dialog so they render on top)
    var vacuumDisplayMode by remember(config) { mutableStateOf(config.vacuumDisplayMode) }
    var vacuumDeviceId by remember(config) { mutableStateOf(config.vacuumDeviceId) }
    var vacuumMapEntityId by remember(config) { mutableStateOf(config.vacuumMapEntityId) }
    var vacuumBatteryEntityId by remember(config) { mutableStateOf(config.vacuumBatteryEntityId) }
    var vacuumWaterEntityId by remember(config) { mutableStateOf(config.vacuumWaterEntityId) }
    var vacuumEmptyBinEntityId by remember(config) { mutableStateOf(config.vacuumEmptyBinEntityId) }
    var vacuumImageUrl by remember(config) { mutableStateOf(config.vacuumImageUrl ?: "") }
    var showMapPicker by remember { mutableStateOf(false) }
    var showBattPicker by remember { mutableStateOf(false) }
    var showVacuumDevicePicker by remember { mutableStateOf(false) }
    var showWaterPicker by remember { mutableStateOf(false) }
    var showEmptyBinPicker by remember { mutableStateOf(false) }
    val refreshOptions = listOf(
        stringResource(R.string.cr_live) to 0,
        "5s" to 5,
        "10s" to 10,
        "15s" to 15,
        "30s" to 30
    )
    val lockRelockSeconds = lockRelockSecondsText.toIntOrNull()?.coerceIn(5, 86400) ?: DEFAULT_BUTTON_RELOCK_SECONDS
    val showButtonLockSettings = !isCameraItem && !isVacuumItem
    val lockPinMissing = showButtonLockSettings && lockEnabled && lockUnlockMode == BUTTON_LOCK_PIN && lockPin.isBlank()
    var settingsPage by remember(isCameraItem, isVacuumItem) { mutableStateOf("general") }
    val settingsTabs = buildList {
        add("general" to stringResource(R.string.cr_general))
        if (isCameraItem) add("camera" to stringResource(R.string.cr_widget_camera))
        if (isVacuumItem) add("vacuum" to stringResource(R.string.cr_widget_vacuum))
        if (!isCameraItem && !isVacuumItem || widgetAppearance != null) {
            add("appearance" to stringResource(R.string.cr_appearance))
        }
        if (!isVacuumItem) add("actions" to stringResource(R.string.cr_actions))
        if (showButtonLockSettings) add("protection" to stringResource(R.string.cr_protection))
        // Its own tab (and always last) so buttons match every other item type. Camera and vacuum
        // items keep their owning widget's visibility rule instead of one of their own.
        if (!isCameraItem && !isVacuumItem) add("visibility" to stringResource(R.string.ui_visibility_7d9ff4f))
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                title = when {
                    isCameraItem -> stringResource(R.string.cr_camera_settings)
                    isVacuumItem -> stringResource(R.string.cr_vacuum_settings)
                    else -> stringResource(R.string.cr_button_settings)
                },
                subtitle = stringResource(R.string.ui_focused_groups_keep_advanced_options_easy_to_find_44ac86e),
                icon = when {
                    isCameraItem -> Icons.Default.CameraAlt
                    isVacuumItem -> Icons.Default.CleaningServices
                    else -> Icons.Default.Lightbulb
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsTabRow(tabs = settingsTabs, selected = settingsPage, onSelect = { settingsPage = it })
                if (settingsPage == "general") {
                    SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_the_text_shown_on_the_dashboard_fff5c98))
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.ui_name_709a232)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (!isCameraItem && !isVacuumItem) {
                        val attributes = remember(entity) { selectableEntityAttributes(entity) }
                        SettingsSubcategory(stringResource(R.string.ui_secondary_line_ca81690), stringResource(R.string.ui_show_the_entity_s_state_or_one_of_its_1008a4d))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = stateAttribute == null,
                                onClick = { stateAttribute = null },
                                label = { Text(stringResource(R.string.ui_state_a725020)) }
                            )
                            attributes.forEach { attr ->
                                FilterChip(
                                    selected = stateAttribute == attr,
                                    onClick = { stateAttribute = attr },
                                    label = { Text(attr.replace('_', ' ')) }
                                )
                            }
                        }
                        if (attributes.isEmpty()) {
                            Text(
                                stringResource(R.string.ui_this_entity_has_no_attributes_to_show_b09f4c9),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.ui_countdown_timer_282cb17), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    stringResource(R.string.ui_when_the_value_is_a_completion_time_e_g_7dce367),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = stateAsTimer, onCheckedChange = {
                                stateAsTimer = it
                                // Try to auto-pick a machine/operation-state sibling the first time it's enabled.
                                if (it && timerStateEntityId == null) {
                                    timerStateEntityId = com.jimz011apps.hki7.ui.components.guessMachineStateEntityId(entity?.entity_id, allEntities, entityRegistry)
                                }
                            })
                        }
                        if (stateAsTimer) {
                            val stateName = timerStateEntityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.ui_running_state_entity_3c8ffde), style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        stateName ?: stringResource(R.string.ui_none_timer_follows_the_completion_time_only_09d470a),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (stateName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showTimerStatePicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                                if (timerStateEntityId != null) TextButton(onClick = { timerStateEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) }
                            }
                        }
                        if (stateAttribute != null) {
                            Text(stringResource(R.string.ui_unit_f6b935a), style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                COMMON_STATE_UNITS.forEach { unit ->
                                    val selected = (stateUnit ?: "") == unit
                                    FilterChip(
                                        selected = selected,
                                        onClick = { stateUnit = unit.ifBlank { null } },
                                        label = { Text(if (unit.isBlank()) stringResource(R.string.ui_none_6eef664) else unit) }
                                    )
                                }
                            }
                        }
                    }
                    // ── Entity integrations (moved here from the former "Entity" tab) ──
                    if (isLockEntity) {
                        androidx.compose.material3.HorizontalDivider(color = LocalHKIAppColors.current.onMuted.copy(alpha = 0.15f))
                        SettingsSubcategory(stringResource(R.string.ui_entity_integrations_852a3e0), stringResource(R.string.ui_optional_context_shown_by_the_lock_card_03f6814))
                        Text(stringResource(R.string.ui_door_sensor_turns_lock_card_red_when_open_9bc1372), style = MaterialTheme.typography.labelLarge)
                        val doorName = doorEntityId?.takeIf { it.isNotBlank() }
                            ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(doorName ?: stringResource(R.string.ui_none_6eef664), style = MaterialTheme.typography.bodySmall, color = if (doorName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { showDoorPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                            if (!doorEntityId.isNullOrBlank()) { TextButton(onClick = { doorEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                        }
                    }
                    if (isClimateEntity) {
                        androidx.compose.material3.HorizontalDivider(color = LocalHKIAppColors.current.onMuted.copy(alpha = 0.15f))
                        SettingsSubcategory(stringResource(R.string.ui_entity_integrations_852a3e0), stringResource(R.string.ui_controls_and_history_sensors_used_by_the_dialog_f71675d))
                        Text(stringResource(R.string.ui_dialog_control_63e18cf), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = climateDialogControl != "dial",
                                onClick = { climateDialogControl = "slider" },
                                label = { Text(stringResource(R.string.ui_vertical_slider_e09cef1)) }
                            )
                            FilterChip(
                                selected = climateDialogControl == "dial",
                                onClick = { climateDialogControl = "dial" },
                                label = { Text(stringResource(R.string.ui_thermostat_dial_e6af91b)) }
                            )
                        }
                        Text(stringResource(R.string.ui_temperature_sensor_graphed_in_activity_1088c3d), style = MaterialTheme.typography.labelLarge)
                        val tempName = climateTempSensorEntityId?.takeIf { it.isNotBlank() }
                            ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(tempName ?: stringResource(R.string.ui_none_6eef664), style = MaterialTheme.typography.bodySmall, color = if (tempName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { showTempSensorPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                            if (!climateTempSensorEntityId.isNullOrBlank()) { TextButton(onClick = { climateTempSensorEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                        }
                        Text(stringResource(R.string.ui_humidity_sensor_graphed_in_activity_5b792e1), style = MaterialTheme.typography.labelLarge)
                        val humidityName = climateHumiditySensorEntityId?.takeIf { it.isNotBlank() }
                            ?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(humidityName ?: stringResource(R.string.ui_none_6eef664), style = MaterialTheme.typography.bodySmall, color = if (humidityName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { showHumiditySensorPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                            if (!climateHumiditySensorEntityId.isNullOrBlank()) { TextButton(onClick = { climateHumiditySensorEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                        }
                    }
                    if (isHumidifierEntity) {
                        androidx.compose.material3.HorizontalDivider(color = LocalHKIAppColors.current.onMuted.copy(alpha = 0.15f))
                        com.jimz011apps.hki7.ui.components.HumidifierIntegrationSettings(
                            deviceId = humidifierDeviceId,
                            fanEntityId = humidifierFanEntityId,
                            auxEntityIds = humidifierAuxEntityIds,
                            allEntities = allEntities,
                            devices = deviceRegistry,
                            entityRegistry = entityRegistry,
                            onChange = { dev, fan, aux ->
                                humidifierDeviceId = dev
                                humidifierFanEntityId = fan
                                humidifierAuxEntityIds = aux
                            }
                        )
                    }
                }
                if (settingsPage == "camera" && isCameraItem) {
                    SettingsSubcategory(stringResource(R.string.ui_stream_df06386), stringResource(R.string.ui_source_and_card_refresh_behavior_0a26324))
                    if (config.isCustomUrl) {
                        OutlinedTextField(
                            value = cameraUrl,
                            onValueChange = { cameraUrl = it },
                            label = { Text(stringResource(R.string.ui_custom_camera_url_8191b15)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(stringResource(R.string.ui_http_url_relative_path_or_webrtc_name_e_g_dfc56c4), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.ui_refresh_interval_f086d9d), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        refreshOptions.forEach { (label, seconds) ->
                            FilterChip(
                                selected = refreshInterval == seconds,
                                onClick = { refreshInterval = seconds },
                                label = { Text(label) },
                                shape = itemCornerShape()
                            )
                        }
                    }
                }
                if (settingsPage == "vacuum" && isVacuumItem) {
                    SettingsSubcategory(stringResource(R.string.ui_device_helpers_6a8b199), stringResource(R.string.ui_map_status_sensors_and_control_bindings_8cc2dbd))
                    Text(stringResource(R.string.ui_vacuum_device_3deae52), style = MaterialTheme.typography.labelLarge)
                    val deviceName = vacuumDeviceId?.let { id -> deviceRegistry.find { it.id == id }?.let { it.name_by_user ?: it.name } ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(deviceName ?: stringResource(R.string.ui_select_to_auto_detect_map_battery_and_controls_72bc9d7), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { showVacuumDevicePicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                    Text(stringResource(R.string.ui_button_image_2483e32), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "static" to R.string.cr_robot_image,
                            "camera" to R.string.cr_map_camera,
                            "external" to R.string.cr_external_url
                        ).forEach { (value, labelRes) ->
                            FilterChip(
                                selected = vacuumDisplayMode == value,
                                onClick = { vacuumDisplayMode = value },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    if (vacuumDisplayMode == "external") {
                        OutlinedTextField(
                            value = vacuumImageUrl,
                            onValueChange = { vacuumImageUrl = it },
                            label = { Text(stringResource(R.string.ui_image_url_or_path_ddd8d74)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(stringResource(R.string.ui_full_url_or_a_path_on_your_home_assistant_66f9c59), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.ui_map_camera_for_dialog_2bc4344), style = MaterialTheme.typography.labelLarge)
                    val mapName = vacuumMapEntityId?.takeIf { it.isNotBlank() }?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(mapName ?: stringResource(R.string.ui_not_set_93039e6), style = MaterialTheme.typography.bodySmall, color = if (mapName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showMapPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                        if (!vacuumMapEntityId.isNullOrBlank()) { TextButton(onClick = { vacuumMapEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                    }
                    Text(stringResource(R.string.ui_water_level_optional_fallback_61b2be5), style = MaterialTheme.typography.labelLarge)
                    VacuumBindingRow(vacuumWaterEntityId, allEntities, { showWaterPicker = true }, { vacuumWaterEntityId = null })
                    Text(stringResource(R.string.ui_empty_bin_optional_fallback_e40b857), style = MaterialTheme.typography.labelLarge)
                    VacuumBindingRow(vacuumEmptyBinEntityId, allEntities, { showEmptyBinPicker = true }, { vacuumEmptyBinEntityId = null })
                    Text(stringResource(R.string.ui_battery_sensor_optional_0e261e6), style = MaterialTheme.typography.labelLarge)
                    val battName = vacuumBatteryEntityId?.takeIf { it.isNotBlank() }?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(battName ?: stringResource(R.string.ui_built_in_20f409c), style = MaterialTheme.typography.bodySmall, color = if (battName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showBattPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                        if (!vacuumBatteryEntityId.isNullOrBlank()) { TextButton(onClick = { vacuumBatteryEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                    }
                }
                if (settingsPage == "appearance" && !isCameraItem && !isVacuumItem) {
                    SettingsSubcategory(stringResource(R.string.ui_icon_feedback_180b871), stringResource(R.string.ui_how_this_control_communicates_state_ad17a3d))
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            iconName.takeUnless { it == "None" } ?: stringResource(R.string.ui_auto_c614ba7),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { showIconPickerBtn = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                    Text(stringResource(R.string.ui_icon_animation_5b1faae), style = MaterialTheme.typography.labelLarge)
                    Text(
                        stringResource(R.string.ui_how_this_icon_animates_while_the_device_is_active_dffdaf5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "auto" to R.string.cr_mode_auto,
                            "off" to R.string.cr_state_off,
                            "glow" to R.string.cr_animation_glow,
                            "spin" to R.string.cr_animation_spin,
                            "pulse" to R.string.cr_animation_pulse
                        ).forEach { (value, labelRes) ->
                            FilterChip(
                                selected = iconAnimation == value,
                                onClick = { iconAnimation = value },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    // Cameras and vacuums draw their own card, which has no name/state line to drop.
                    if (!isCameraItem && !isVacuumItem) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.button_icon_only), style = MaterialTheme.typography.labelLarge)
                                Text(stringResource(R.string.button_icon_only_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = iconOnly, onCheckedChange = { iconOnly = it })
                        }
                    }
                    if (isLightEntity) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.ui_brightness_slider_a427c80), style = MaterialTheme.typography.labelLarge)
                                Text(stringResource(R.string.ui_drag_horizontally_across_the_full_button_547ea8c), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = showBrightnessSlider, onCheckedChange = { showBrightnessSlider = it })
                        }
                    }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
                if (settingsPage == "actions" && !isVacuumItem) {
                    SettingsSubcategory(stringResource(R.string.ui_interactions_0b3583e), stringResource(R.string.ui_choose_what_tap_double_tap_and_hold_should_do_0bbe9c8))
                    ActionEditor(stringResource(R.string.cr_action_tap), tapAction, allEntities, areas, viewModel) { tapAction = it }
                    ActionEditor(stringResource(R.string.cr_action_double_tap), doubleAction, allEntities, areas, viewModel) { doubleAction = it }
                    ActionEditor(stringResource(R.string.cr_action_hold), holdAction, allEntities, areas, viewModel) { holdAction = it }
                    CustomButtonsEditor(customButtons, allEntities, areas, viewModel) { customButtons = it }
                }
                if (settingsPage == "protection" && showButtonLockSettings) {
                    SettingsSubcategory(stringResource(R.string.ui_accidental_touch_protection_e2683f4), stringResource(R.string.ui_require_an_extra_gesture_or_pin_before_actions_run_19ae393))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_button_lock_28d87b1), style = MaterialTheme.typography.labelLarge)
                        }
                        Switch(checked = lockEnabled, onCheckedChange = { lockEnabled = it })
                    }
                    if (lockEnabled) {
                        Text(stringResource(R.string.ui_unlock_method_dff380e), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = lockUnlockMode == BUTTON_LOCK_DOUBLE_TAP,
                                onClick = { lockUnlockMode = BUTTON_LOCK_DOUBLE_TAP },
                                label = { Text(stringResource(R.string.ui_double_tap_a268e89)) }
                            )
                            FilterChip(
                                selected = lockUnlockMode == BUTTON_LOCK_PIN,
                                onClick = { lockUnlockMode = BUTTON_LOCK_PIN },
                                label = { Text(stringResource(R.string.ui_pin_3adadd3)) }
                            )
                        }
                        if (lockUnlockMode == BUTTON_LOCK_PIN) {
                            OutlinedTextField(
                                value = lockPin,
                                onValueChange = { lockPin = it.filter { c -> c.isDigit() }.take(12) },
                                label = { Text(stringResource(R.string.ui_pin_3adadd3)) },
                                singleLine = true,
                                isError = lockPinMissing,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (lockPinMissing) {
                                Text(stringResource(R.string.ui_enter_a_pin_4e6a960), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(stringResource(R.string.ui_relock_after_4db5c55), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15 to "15s", 30 to "30s", 60 to "1m", 300 to "5m").forEach { (seconds, label) ->
                                FilterChip(
                                    selected = lockRelockSeconds == seconds,
                                    onClick = { lockRelockSecondsText = seconds.toString() },
                                    label = { Text(label) },
                                    shape = itemCornerShape()
                                )
                            }
                        }
                        OutlinedTextField(
                            value = lockRelockSecondsText,
                            onValueChange = { lockRelockSecondsText = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text(stringResource(R.string.ui_seconds_5fb1db5)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (settingsPage == "appearance" && widgetAppearance != null) {
                    SettingsSubcategory(stringResource(R.string.ui_card_layout_f83b655), stringResource(R.string.ui_shape_and_width_on_the_dashboard_c89fefa))
                    Text(stringResource(R.string.ui_type_3deb745), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = appearButtonStyle == "standard", onClick = { appearButtonStyle = "standard"; appearIsSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = appearButtonStyle == "square", onClick = { appearButtonStyle = "square"; appearIsSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                        FilterChip(selected = appearButtonStyle == "tile", onClick = { appearButtonStyle = "tile"; appearIsSquare = false }, label = { Text(stringResource(R.string.ui_tile_2dd2c66)) })
                    }
                    WidgetWidthSelector(width = appearWidth, onWidthChange = { appearWidth = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !lockPinMissing,
                onClick = {
                    val save: (HKIButtonConfig) -> Unit = { newConfig ->
                        if (widgetAppearance != null && onSaveWithAppearance != null) {
                            onSaveWithAppearance(
                                newConfig,
                                WidgetAppearance(appearIsSquare, appearRadius, appearWidth, appearButtonStyle)
                            )
                        } else onSave(newConfig)
                    }
                    save(
                        config.copy(
                            name = name.ifBlank { null },
                            stateAttribute = if (isCameraItem || isVacuumItem) config.stateAttribute else stateAttribute,
                            stateUnit = if (isCameraItem || isVacuumItem) config.stateUnit else stateUnit,
                            stateAsTimer = if (isCameraItem || isVacuumItem) config.stateAsTimer else stateAsTimer,
                            timerStateEntityId = if (isCameraItem || isVacuumItem) config.timerStateEntityId else timerStateEntityId,
                            icon = if (isCameraItem || isVacuumItem) config.icon else iconName.takeUnless { it == "None" },
                            iconAnimation = iconAnimation,
                            iconOnly = iconOnly,
                            showBrightnessSlider = if (isLightEntity) showBrightnessSlider else false,
                            cameraUrl = if (isCameraItem && config.isCustomUrl) cameraUrl.ifBlank { null } else config.cameraUrl,
                            cameraRefreshInterval = if (isCameraItem) refreshInterval else config.cameraRefreshInterval,
                            isCustomUrl = config.isCustomUrl,
                            tapActionEx = if (isVacuumItem) config.tapActionEx else tapAction,
                            doubleTapActionEx = if (isVacuumItem) config.doubleTapActionEx else doubleAction,
                            holdActionEx = if (isVacuumItem) config.holdActionEx else holdAction,
                            customButtons = if (isVacuumItem) config.customButtons else customButtons,
                            lockEnabled = if (showButtonLockSettings) lockEnabled else config.lockEnabled,
                            lockUnlockMode = if (showButtonLockSettings) lockUnlockMode else config.lockUnlockMode,
                            lockPin = if (showButtonLockSettings) lockPin.ifBlank { null } else config.lockPin,
                            lockRelockSeconds = if (showButtonLockSettings) lockRelockSeconds else config.lockRelockSeconds,
                            doorEntityId = if (isLockEntity) doorEntityId else config.doorEntityId,
                            humidifierFanEntityId = if (isHumidifierEntity) humidifierFanEntityId else config.humidifierFanEntityId,
                            humidifierDeviceId = if (isHumidifierEntity) humidifierDeviceId else config.humidifierDeviceId,
                            humidifierAuxEntityIds = if (isHumidifierEntity) humidifierAuxEntityIds else config.humidifierAuxEntityIds,
                            vacuumDisplayMode = if (isVacuumItem) vacuumDisplayMode else config.vacuumDisplayMode,
                            vacuumDeviceId = if (isVacuumItem) vacuumDeviceId else config.vacuumDeviceId,
                            vacuumMapEntityId = if (isVacuumItem) vacuumMapEntityId else config.vacuumMapEntityId,
                            vacuumBatteryEntityId = if (isVacuumItem) vacuumBatteryEntityId else config.vacuumBatteryEntityId,
                            vacuumWaterEntityId = if (isVacuumItem) vacuumWaterEntityId else config.vacuumWaterEntityId,
                            vacuumEmptyBinEntityId = if (isVacuumItem) vacuumEmptyBinEntityId else config.vacuumEmptyBinEntityId,
                            vacuumImageUrl = if (isVacuumItem && vacuumDisplayMode == "external") vacuumImageUrl.ifBlank { null } else if (isVacuumItem) null else config.vacuumImageUrl,
                            climateTempSensorEntityId = if (isClimateEntity) climateTempSensorEntityId else config.climateTempSensorEntityId,
                            climateHumiditySensorEntityId = if (isClimateEntity) climateHumiditySensorEntityId else config.climateHumiditySensorEntityId,
                            climateDialogControl = if (isClimateEntity) climateDialogControl else config.climateDialogControl,
                            hidden = if (isCameraItem || isVacuumItem) config.hidden else visSpec.hidden,
                            visibilityStart = if (isCameraItem || isVacuumItem) config.visibilityStart else visSpec.start,
                            visibilityEnd = if (isCameraItem || isVacuumItem) config.visibilityEnd else visSpec.end,
                            visibilityRangeMode = if (isCameraItem || isVacuumItem) config.visibilityRangeMode else visSpec.rangeMode,
                            visibilityRecurrence = if (isCameraItem || isVacuumItem) config.visibilityRecurrence else visSpec.recurrence,
                            visibilityConditionEntityId = if (isCameraItem || isVacuumItem) config.visibilityConditionEntityId else visSpec.conditionEntityId,
                            visibilityConditionState = if (isCameraItem || isVacuumItem) config.visibilityConditionState else visSpec.conditionState,
                            visibilityConditionNegate = if (isCameraItem || isVacuumItem) config.visibilityConditionNegate else visSpec.conditionNegate,
                            visibilityConditions = if (isCameraItem || isVacuumItem) config.visibilityConditions else visSpec.conditions,
                            visibilityMatch = if (isCameraItem || isVacuumItem) config.visibilityMatch else visSpec.match
                        )
                    )
                }
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )

    // ── Entity pickers hoisted AFTER the AlertDialog so they render on top of it ──
    if (showTimerStatePicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            title = stringResource(R.string.ui_select_running_state_entity_136482a),
            singleSelect = true,
            preselectedIds = setOfNotNull(timerStateEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showTimerStatePicker = false },
            onEntitiesSelected = { ids -> timerStateEntityId = ids.firstOrNull(); showTimerStatePicker = false }
        )
    }
    if (showDoorPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("binary_sensor.") },
            title = stringResource(R.string.ui_select_door_sensor_f345fbf),
            singleSelect = true,
            preselectedIds = setOfNotNull(doorEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showDoorPicker = false },
            onEntitiesSelected = { ids -> doorEntityId = ids.firstOrNull(); showDoorPicker = false }
        )
    }
    if (showMapPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("camera.") },
            title = stringResource(R.string.ui_select_map_camera_d5ae262),
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumMapEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showMapPicker = false },
            onEntitiesSelected = { ids -> vacuumMapEntityId = ids.firstOrNull(); showMapPicker = false }
        )
    }
    if (showBattPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("sensor.") },
            title = stringResource(R.string.ui_select_battery_sensor_51f1222),
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumBatteryEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showBattPicker = false },
            onEntitiesSelected = { ids -> vacuumBatteryEntityId = ids.firstOrNull(); showBattPicker = false }
        )
    }
    if (showVacuumDevicePicker) {
        DevicePickerDialog(deviceRegistry, vacuumDeviceId, { showVacuumDevicePicker = false }) { deviceId ->
            vacuumDeviceId = deviceId
            if (deviceId != null) {
                // Auto-fill the helper entity fields from the device, like the Energy view does.
                val auto = resolveVacuumDeviceEntities(deviceId, allEntities, entityRegistry)
                auto.map?.let { vacuumMapEntityId = it.entity_id }
                auto.battery?.let { vacuumBatteryEntityId = it.entity_id }
                auto.water?.let { vacuumWaterEntityId = it.entity_id }
                auto.emptyBin?.let { vacuumEmptyBinEntityId = it.entity_id }
            }
            showVacuumDevicePicker = false
        }
    }
    if (showWaterPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.") },
            title = stringResource(R.string.ui_select_water_level_562845a), singleSelect = true, preselectedIds = setOfNotNull(vacuumWaterEntityId),
            onDismiss = { showWaterPicker = false }, onEntitiesSelected = { vacuumWaterEntityId = it.firstOrNull(); showWaterPicker = false }
        )
    }
    if (showEmptyBinPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("button.") || it.entity_id.startsWith("switch.") },
            title = stringResource(R.string.ui_select_empty_bin_control_d992252), singleSelect = true, preselectedIds = setOfNotNull(vacuumEmptyBinEntityId),
            onDismiss = { showEmptyBinPicker = false }, onEntitiesSelected = { vacuumEmptyBinEntityId = it.firstOrNull(); showEmptyBinPicker = false }
        )
    }
    if (showTempSensorPicker) {
        val sensors = allEntities.filter { it.entity_id.startsWith("sensor.") }
        AdvancedEntitySearchDialog(
            allEntities = sensors.filter { it.deviceClass == "temperature" }.ifEmpty { sensors },
            title = stringResource(R.string.ui_select_temperature_sensor_821f54c),
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
            title = stringResource(R.string.ui_select_humidity_sensor_8192d0e),
            singleSelect = true,
            preselectedIds = setOfNotNull(climateHumiditySensorEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showHumiditySensorPicker = false },
            onEntitiesSelected = { ids -> climateHumiditySensorEntityId = ids.firstOrNull(); showHumiditySensorPicker = false }
        )
    }
}

@Composable
fun PagedRoleDialog(
    role: String,
    entities: List<HAEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    buttonConfigs: Map<String, HKIButtonConfig> = emptyMap(),
    useClimateDial: Boolean = false
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
    // Keep the dialog control tied to the exact same state color used by entity cards,
    // badges, and custom dialog buttons. This is especially visible for lock dialogs opened
    // from a custom action: locked/unlocked/open must stay green/orange/red respectively.
    val entityStateTone = entityStateIconColor(entity)
    var selectedClimateMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { selectedClimateMode = entity.state }
    var showClimateModes by remember(entity.entity_id) { mutableStateOf(false) }
    val climateTabs = buildList {
        entity.hvacModes.ifEmpty { listOf("cool", "heat", "auto", "off") }.forEach { mode ->
            add(Triple(localizedClimateModeLabel(mode), climateModeIcon(mode), {
                selectedClimateMode = mode
                viewModel.setHvacMode(entity.entity_id, mode)
            }))
        }
    }
    val openLabel = stringResource(R.string.cr_open)
    val stopLabel = stringResource(R.string.cr_stop)
    val closeLabel = stringResource(R.string.cr_close)
    var selectedCoverAction by remember(entity.entity_id, openLabel, stopLabel, closeLabel) {
        mutableStateOf(
            when (entity.state) {
                "opening", "open" -> openLabel
                "closing", "closed" -> closeLabel
                else -> stopLabel
            }
        )
    }
    val coverTabs = listOf(
        Triple(openLabel, Icons.Default.ArrowUpward, {
            selectedCoverAction = openLabel
            viewModel.controlCover(entity.entity_id, "open_cover")
        }),
        Triple(stopLabel, Icons.Default.Stop, {
            selectedCoverAction = stopLabel
            viewModel.controlCover(entity.entity_id, "stop_cover")
        }),
        Triple(closeLabel, Icons.Default.ArrowDownward, {
            selectedCoverAction = closeLabel
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
            "lock" -> entityStateTone
            "cover" -> coverAccentColor(entity)
            else -> Color(0xFFFFA500)
        },
        titleOverride = headerConfig?.name,
        iconName = headerConfig?.icon?.takeUnless { it.isBlank() } ?: defaultEntityIconSlug(entity),
        statusText = if (role == "climate") {
            val optimisticState = localizedClimateModeLabel(selectedClimateMode).uppercase(Locale.getDefault())
            if (entities.size > 1) "${page + 1}/${entities.size} - $optimisticState" else optimisticState
        } else if (entities.size > 1) {
            stringResource(
                R.string.cr_paged_status,
                page + 1,
                entities.size,
                localizedEntityState(entity).uppercase(Locale.getDefault())
            )
        } else {
            localizedEntityState(entity).uppercase(Locale.getDefault())
        },
        extraGraphEntityIds = extraGraphEntityIds,
        tabs = when (role) {
            "climate" -> climateTabs.takeIf { it.size > 1 } ?: emptyList()
            "cover" -> coverTabs
            else -> emptyList()
        },
        currentTab = when (role) {
            "climate" -> localizedClimateModeLabel(selectedClimateMode)
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
                    "climate" -> ClimateControlContent(
                        entity,
                        viewModel,
                        showModes = showClimateModes,
                        onToggleModes = { showClimateModes = it },
                        useDial = useClimateDial || climateConfig?.climateDialogControl == "dial"
                    )
                    "lock" -> LockControlContent(entity, viewModel, entityStateTone)
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
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onUpdate: (HKIButtonStack) -> Unit
) {
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val maxColumns = LocalMaxStackColumns.current
    var title by remember(stack) { mutableStateOf(stack.title ?: "") }
    var showName by remember(stack) { mutableStateOf(stack.showName) }
    var iconName by remember(stack) { mutableStateOf(stack.icon ?: "Lightbulb") }
    var width by remember(stack) { mutableStateOf(stack.width) }
    var columns by remember(stack, maxColumns) { mutableIntStateOf(stack.columns.coerceIn(1, maxColumns)) }
    var showBadge by remember(stack) { mutableStateOf(stack.showBadge) }
    var isSquare by remember(stack) { mutableStateOf(stack.isSquare) }
    var buttonStyle by remember(stack) {
        mutableStateOf(stack.buttonStyle.takeIf { it.isNotBlank() } ?: if (stack.isSquare) "square" else "standard")
    }
    var cornerRadius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var defaultCollapsed by remember(stack) { mutableStateOf(stack.defaultCollapsed) }
    var adaptiveLayout by remember(stack) { mutableStateOf(stack.adaptiveLightingLayout) }
    var adaptiveCenterActions by remember(stack) { mutableStateOf(stack.adaptiveLightingCenterActions) }
    var cameraAspect by remember(stack) { mutableFloatStateOf(stack.cameraAspectRatio) }
    var showIconPickerStack by remember { mutableStateOf(false) }
    var settingsPage by remember(stack) { mutableStateOf("identity") }
    var visSpec by remember(stack) {
        mutableStateOf(
            stack.toVisibilitySpec()
        )
    }
    val adaptiveProfiles = rememberAdaptiveLightingProfiles(viewModel)
    val selectableAdaptiveProfiles = remember(stack.adaptiveLightingRoomScoped, stack.adaptiveLightingProfileIds, adaptiveProfiles) {
        if (stack.adaptiveLightingRoomScoped) {
            adaptiveProfiles.filter { it.configEntryId in stack.adaptiveLightingProfileIds }
        } else adaptiveProfiles
    }
    var adaptiveProfileIds by remember(stack, selectableAdaptiveProfiles) {
        mutableStateOf(
            if (stack.adaptiveLightingProfileIds.isEmpty()) {
                selectableAdaptiveProfiles.map { it.configEntryId }.toSet()
            } else {
                stack.adaptiveLightingProfileIds.toSet()
            }
        )
    }
    val isAdaptiveLighting = stack.stackType == "adaptive_lighting"

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
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                if (isAdaptiveLighting) {
                    stringResource(R.string.cr_adaptive_lighting_widget)
                } else {
                    stringResource(R.string.cr_widget_button_stack)
                },
                if (isAdaptiveLighting) {
                    stringResource(R.string.cr_adaptive_lighting_widget_subtitle)
                } else {
                    stringResource(R.string.cr_button_stack_dialog_subtitle)
                }
            )
        },
        text = {
            val settingsScroll = rememberScrollState()
            Column(
                modifier = Modifier.heightIn(max = 480.dp).fadingEdges(settingsScroll).verticalScroll(settingsScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsTabRow(
                    tabs = buildList {
                        add("identity" to stringResource(R.string.cr_identity))
                        if (isAdaptiveLighting) add("profiles" to stringResource(R.string.cr_profiles))
                        add("layout" to stringResource(R.string.cr_layout))
                        if (!isAdaptiveLighting) add("behavior" to stringResource(R.string.cr_behavior))
                        add("visibility" to stringResource(R.string.ui_visibility_7d9ff4f))
                    },
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "identity") {
                SettingsSubcategory(
                    stringResource(R.string.ui_identity_7e5a975),
                    if (isAdaptiveLighting) stringResource(R.string.ui_name_width_and_icon_a4d694b) else stringResource(R.string.ui_title_width_and_icon_41ec073)
                )
                if (isAdaptiveLighting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showName, onCheckedChange = { showName = it })
                        Text(stringResource(R.string.ui_show_widget_name_5ad7fc9))
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_widget_name_55e5340)) },
                        enabled = showName,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_stack_title_cc2f1ed)) },
                        singleLine = true
                    )
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (iconName != "None") {
                        MdiIcon(iconName, size = 24.dp)
                    }
                    Text(
                        iconName.takeUnless { it == "None" } ?: stringResource(R.string.cr_none),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showIconPickerStack = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                }
                }
                if (settingsPage == "profiles" && isAdaptiveLighting) {
                    SettingsSubcategory(stringResource(R.string.ui_profiles_0c2a930), stringResource(R.string.ui_choose_which_profiles_this_widget_can_control_2cd9b2b))
                    if (selectableAdaptiveProfiles.isEmpty()) {
                        Text(stringResource(R.string.ui_no_adaptive_lighting_profiles_are_currently_available_6d29642), color = LocalHKIAppColors.current.onMuted)
                    } else {
                        selectableAdaptiveProfiles.forEach { profile ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = profile.configEntryId in adaptiveProfileIds,
                                    onCheckedChange = { checked ->
                                        adaptiveProfileIds = if (checked) {
                                            adaptiveProfileIds + profile.configEntryId
                                        } else {
                                            adaptiveProfileIds - profile.configEntryId
                                        }
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name, color = LocalHKIAppColors.current.onSurface)
                                    Text(
                                        pluralStringResource(
                                            R.plurals.cr_configured_lights,
                                            profile.configuredLightIds.size,
                                            profile.configuredLightIds.size
                                        ),
                                        color = LocalHKIAppColors.current.onMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
                if (settingsPage == "layout") {
                SettingsSubcategory(stringResource(R.string.ui_layout_972ad8d), if (isAdaptiveLighting) stringResource(R.string.ui_widget_presentation_a3a8cdc) else stringResource(R.string.ui_grid_density_and_button_presentation_b7907e5))
                if (isAdaptiveLighting) {
                    Text(stringResource(R.string.ui_control_layout_4c3b047), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = adaptiveLayout != "double_row",
                            onClick = { adaptiveLayout = "full" },
                            label = { Text(stringResource(R.string.ui_full_controls_68749a5)) }
                        )
                        FilterChip(
                            selected = adaptiveLayout == "double_row",
                            onClick = { adaptiveLayout = "double_row"; isSquare = false },
                            label = { Text(stringResource(R.string.ui_two_rows_1b1737f)) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = adaptiveCenterActions, onCheckedChange = { adaptiveCenterActions = it })
                        Text(stringResource(R.string.ui_center_adapt_now_and_pause_buttons_81074b2))
                    }
                }
                if (!isAdaptiveLighting) {
                    Text(stringResource(R.string.ui_columns_cf723c5), style = MaterialTheme.typography.labelLarge)
                    // FlowRow: six chips no longer fit one row on a phone-width dialog.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..maxColumns).forEach { count ->
                            FilterChip(selected = columns == count, onClick = { columns = count }, label = { Text(stringResource(R.string.ui_text_c79f712, count)) })
                        }
                    }
                }
                if (stack.stackType != "weather") {
                    Text(if (isAdaptiveLighting) stringResource(R.string.ui_widget_height_b26de24) else stringResource(R.string.ui_button_type_890a578), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = buttonStyle == "standard", onClick = { buttonStyle = "standard"; isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = buttonStyle == "square", onClick = { buttonStyle = "square"; isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                        FilterChip(selected = buttonStyle == "tile", onClick = { buttonStyle = "tile"; isSquare = false }, label = { Text(stringResource(R.string.ui_compact_1df39aa)) })
                    }
                }
                }
                if (settingsPage == "behavior") {
                SettingsSubcategory(stringResource(R.string.ui_behavior_70cb647), stringResource(R.string.ui_activity_indication_and_collapsing_0764a94))
                if (stack.stackType !in listOf("camera", "weather", "adaptive_lighting")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showBadge, onCheckedChange = { showBadge = it })
                        Text(stringResource(R.string.ui_show_activity_badge_f5d56bb))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text(stringResource(R.string.ui_collapsible_c932fac))
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text(stringResource(R.string.ui_collapsed_by_default_65a57c6))
                    }
                }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
                if (settingsPage == "layout" && stack.stackType == "camera") {
                    SettingsSubcategory(stringResource(R.string.ui_camera_display_20a9c82), stringResource(R.string.ui_aspect_ratio_used_by_every_stream_275bb52))
                    Text(stringResource(R.string.ui_camera_aspect_ratio_928bf09), style = MaterialTheme.typography.labelLarge)
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
                            showName = if (isAdaptiveLighting) showName else stack.showName,
                            width = width,
                            icon = iconName.takeUnless { it == "None" },
                            columns = columns.coerceIn(1, maxColumns),
                            showBadge = if (stack.stackType in listOf("camera", "weather")) false else showBadge,
                            isSquare = if (stack.stackType == "weather") false else isSquare,
                            buttonStyle = if (stack.stackType in listOf("weather", "camera", "vacuum")) stack.buttonStyle else buttonStyle,
                            cornerRadius = cornerRadius,
                            isHidden = if (isAdaptiveLighting) false else visSpec.hidden,
                            visibilityStart = if (isAdaptiveLighting) null else visSpec.start,
                            visibilityEnd = if (isAdaptiveLighting) null else visSpec.end,
                            visibilityRangeMode = if (isAdaptiveLighting) "show" else visSpec.rangeMode,
                            visibilityRecurrence = if (isAdaptiveLighting) "none" else visSpec.recurrence,
                            visibilityConditionEntityId = if (isAdaptiveLighting) null else visSpec.conditionEntityId,
                            visibilityConditionState = if (isAdaptiveLighting) null else visSpec.conditionState,
                            visibilityConditionNegate = if (isAdaptiveLighting) false else visSpec.conditionNegate,
                            visibilityConditions = if (isAdaptiveLighting) emptyList() else visSpec.conditions,
                            visibilityMatch = if (isAdaptiveLighting) VISIBILITY_MATCH_ALL else visSpec.match,
                            collapsible = collapsible,
                            defaultCollapsed = defaultCollapsed,
                            isCollapsed = defaultCollapsed,
                            cameraAspectRatio = cameraAspect,
                            adaptiveLightingProfileIds = if (isAdaptiveLighting) adaptiveProfileIds.toList().sorted() else stack.adaptiveLightingProfileIds,
                            adaptiveLightingRoomScoped = stack.adaptiveLightingRoomScoped,
                            adaptiveLightingLayout = if (isAdaptiveLighting) adaptiveLayout else stack.adaptiveLightingLayout,
                            adaptiveLightingCenterActions = if (isAdaptiveLighting) adaptiveCenterActions else stack.adaptiveLightingCenterActions
                        )
                    )
                }
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

private fun normalizedButtonLockMode(config: HKIButtonConfig): String =
    if (config.lockUnlockMode == BUTTON_LOCK_PIN) BUTTON_LOCK_PIN else BUTTON_LOCK_DOUBLE_TAP

private fun isButtonCurrentlyLocked(
    config: HKIButtonConfig?,
    nowMillis: Long,
    unlockedUntilMillis: Long?
): Boolean = config?.lockEnabled == true && (unlockedUntilMillis ?: 0L) <= nowMillis

private fun buttonRelockMillis(config: HKIButtonConfig): Long =
    config.lockRelockSeconds.coerceIn(5, 86400) * 1000L

@Composable
private fun ButtonLockBadge(
    config: HKIButtonConfig?,
    locked: Boolean,
    modifier: Modifier = Modifier
) {
    if (config?.lockEnabled != true) return
    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = if (locked) Color.Black.copy(alpha = 0.58f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) {
                    stringResource(R.string.cr_locked)
                } else {
                    stringResource(R.string.cr_unlocked)
                },
                tint = if (locked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ButtonUnlockPinDialog(
    config: HKIButtonConfig,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    var pin by remember(config) { mutableStateOf("") }
    var hasError by remember(config) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_unlock_button_ad521c4)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(12)
                        hasError = false
                    },
                    label = { Text(stringResource(R.string.ui_pin_3adadd3)) },
                    singleLine = true,
                    isError = hasError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasError) {
                    Text(stringResource(R.string.ui_incorrect_pin_2de1d18), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == config.lockPin.orEmpty()) {
                        onUnlock()
                    } else {
                        hasError = true
                    }
                }
            ) { Text(stringResource(R.string.ui_unlock_1526a17)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

@Composable
fun StackOrderDialog(
    stack: HKIButtonStack,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (List<String>, Map<String, HKIButtonConfig>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val entityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }
    var orderedIds by remember(stack.id, stack.entityIds) { mutableStateOf(stack.entityIds) }
    var configs by remember(stack.id) { mutableStateOf(stack.buttonConfigs) }
    val listHeight = ((orderedIds.size * 76).coerceIn(96, 460)).dp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_manage_items_d854c4b)) },
        text = {
            if (orderedIds.isEmpty()) {
                Text(stringResource(R.string.ui_this_stack_has_no_items_0f92c8d), color = appColors.onMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.ui_drag_to_reorder_use_the_eye_button_to_hide_cd28b4c),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    ReorderableGrid(
                        items = orderedIds,
                        canReorder = true,
                        onReorder = { from, to ->
                            orderedIds = orderedIds.toMutableList().apply {
                                add(to.coerceIn(0, size - 1), removeAt(from))
                            }
                        },
                        key = { it },
                        columns = GridCells.Fixed(1),
                        axis = ReorderAxis.Vertical,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listHeight)
                    ) { entityId, isDragging ->
                        StackOrderRow(
                            entityId = entityId,
                            entity = entityById[entityId],
                            config = configs[entityId],
                            isDragging = isDragging
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(orderedIds, configs) }) {
                Text(stringResource(R.string.ui_save_efc007a))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel_77dfd21))
            }
        }
    )

}

@Composable
private fun ButtonVisibilityDialog(
    label: String,
    config: HKIButtonConfig,
    onDismiss: () -> Unit,
    onSave: (HKIButtonConfig) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var mode by remember {
        mutableStateOf(
            when {
                !config.visibilityStart.isNullOrBlank() || !config.visibilityEnd.isNullOrBlank() -> "scheduled"
                config.hidden -> "hidden"
                else -> "always"
            }
        )
    }
    var rangeMode by remember { mutableStateOf(config.visibilityRangeMode.ifBlank { "show" }) }
    var recurrence by remember { mutableStateOf(config.visibilityRecurrence.ifBlank { "none" }) }
    var start by remember { mutableStateOf(config.visibilityStart ?: "") }
    var end by remember { mutableStateOf(config.visibilityEnd ?: "") }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_visibility_7d9ff4f)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "always", onClick = { mode = "always" }, label = { Text(stringResource(R.string.ui_always_a91bcce)) })
                    FilterChip(selected = mode == "hidden", onClick = { mode = "hidden" }, label = { Text(stringResource(R.string.ui_hidden_d4c2792)) })
                    FilterChip(selected = mode == "scheduled", onClick = { mode = "scheduled" }, label = { Text(stringResource(R.string.ui_scheduled_1cd1bda)) })
                }
                if (mode == "scheduled") {
                    Text(stringResource(R.string.ui_when_769bb19), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = rangeMode == "show", onClick = { rangeMode = "show" }, label = { Text(stringResource(R.string.ui_visible_during_791f42c)) })
                        FilterChip(selected = rangeMode == "hide", onClick = { rangeMode = "hide" }, label = { Text(stringResource(R.string.ui_hidden_during_c0afbbc)) })
                    }
                    Text(stringResource(R.string.ui_repeat_659eba1), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "none" to R.string.cr_once,
                            "daily" to R.string.cr_daily,
                            "weekly" to R.string.cr_weekly,
                            "monthly" to R.string.cr_monthly,
                            "yearly" to R.string.cr_yearly
                        ).forEach { (value, labelRes) ->
                            FilterChip(
                                selected = recurrence == value,
                                onClick = { recurrence = value },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                R.string.ui_start_24758a5,
                                formatScheduleLabel(start, stringResource(R.string.cr_any))
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                R.string.ui_end_2a002eb,
                                formatScheduleLabel(end, stringResource(R.string.cr_any))
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        when (recurrence) {
                            "none" -> stringResource(R.string.ui_the_exact_dates_you_pick_leave_a_bound_as_19357e8)
                            "daily" -> stringResource(R.string.ui_repeats_every_day_only_the_time_of_day_is_a71f502)
                            "weekly" -> stringResource(R.string.ui_repeats_every_week_only_the_weekday_and_time_are_8f36757)
                            "monthly" -> stringResource(R.string.ui_repeats_every_month_only_the_day_of_the_month_93ca445)
                            else -> stringResource(R.string.ui_repeats_every_year_the_year_is_ignored_so_e_a661f32)
                        } + stringResource(R.string.ui_this_travels_with_the_dashboard_so_cloud_backups_and_1dd603d),
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updated = when (mode) {
                    "hidden" -> config.copy(hidden = true, visibilityStart = null, visibilityEnd = null)
                    "scheduled" -> config.copy(
                        hidden = false,
                        visibilityStart = start.trim().ifBlank { null },
                        visibilityEnd = end.trim().ifBlank { null },
                        visibilityRangeMode = rangeMode,
                        visibilityRecurrence = recurrence
                    )
                    else -> config.copy(hidden = false, visibilityStart = null, visibilityEnd = null, visibilityRecurrence = "none")
                }
                onSave(updated)
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )

    if (pickingStart) {
        DateTimePickerDialog(
            initial = start.takeIf { it.isNotBlank() }?.let { runCatching { java.time.LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingStart = false },
            onClear = { start = ""; pickingStart = false },
            onConfirm = { start = it.toString(); pickingStart = false }
        )
    }
    if (pickingEnd) {
        DateTimePickerDialog(
            initial = end.takeIf { it.isNotBlank() }?.let { runCatching { java.time.LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingEnd = false },
            onClear = { end = ""; pickingEnd = false },
            onConfirm = { end = it.toString(); pickingEnd = false }
        )
    }
}

/** Human-readable label for a scheduled bound stored as an ISO local date-time, or "Any" if blank. */
private fun formatScheduleLabel(iso: String, anyLabel: String): String {
    if (iso.isBlank()) return anyLabel
    return runCatching {
        java.time.LocalDateTime.parse(iso)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
    }.getOrDefault(iso)
}

@Composable
private fun StackOrderRow(
    entityId: String,
    entity: HAEntity?,
    config: HKIButtonConfig?,
    isDragging: Boolean,
    onVisibility: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    val isSpacer = isSpacerEntityId(entityId)
    val scheduled = !config?.visibilityStart.isNullOrBlank() || !config?.visibilityEnd.isNullOrBlank()
    val plainHidden = config?.hidden == true
    val label = when {
        isSpacer -> stringResource(R.string.spacer_empty_button)
        else -> config?.name?.takeIf { it.isNotBlank() }
            ?: entity?.friendlyName
            ?: if (config?.isCustomUrl == true) stringResource(R.string.cr_custom_camera) else entityId
    }
    val spacerSecondary = stringResource(R.string.spacer_empty_button_description)
    val secondary = if (isSpacer) spacerSecondary else buildList {
        add(entity?.entity_id ?: entityId)
        entity?.state?.takeIf { it.isNotBlank() && it != "unknown" && it != "unavailable" }?.let { add(it) }
    }.joinToString(" - ")
    val iconName = when {
        isSpacer -> "crop-square"
        else -> config?.icon?.takeIf { it.isNotBlank() } ?: entity?.let { defaultEntityIconSlug(it) }
    }

    Surface(
        shape = itemCornerShape(),
        color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else appColors.subtleSurface,
        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = if (isDragging) 0.28f else 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.SwapVert,
                contentDescription = null,
                tint = appColors.onMuted,
                modifier = Modifier.size(20.dp)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconName != null) {
                        MdiIcon(iconName, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    } else {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (plainHidden) appColors.onMuted else appColors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        plainHidden -> stringResource(R.string.ui_hidden_d4c2792)
                        scheduled -> stringResource(R.string.ui_scheduled_1cd1bda)
                        else -> secondary
                    },
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onVisibility != null) {
                IconButton(onClick = onVisibility) {
                    Icon(
                        when {
                            plainHidden -> Icons.Default.VisibilityOff
                            scheduled -> Icons.Default.Schedule
                            else -> Icons.Default.Visibility
                        },
                        contentDescription = stringResource(R.string.ui_visibility_7d9ff4f),
                        tint = if (plainHidden || scheduled) MaterialTheme.colorScheme.primary else appColors.onMuted
                    )
                }
            }
        }
    }
}

/** Stand-in entity for an empty button, so spacers travel through the same rendering path as real
 *  buttons without needing a Home Assistant entity. */
internal fun spacerPlaceholderEntity(entityId: String): HAEntity? =
    if (isSpacerEntityId(entityId)) HAEntity(entity_id = entityId, state = "") else null

@Composable
fun ButtonStackItem(
    stack: HKIButtonStack,
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
    onReorderEntities: (Int, Int) -> Unit,
    onManageOrder: () -> Unit = {}
) {
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val maxColumns = LocalMaxStackColumns.current
    if (!isWidgetVisibleNow(stack) && stack.stackType != "adaptive_lighting" && !isEditMode) return
    if (stack.stackType == "adaptive_lighting") {
        AdaptiveLightingWidget(
            stack = stack,
            viewModel = viewModel,
            isEditMode = isEditMode,
            onDelete = onDeleteClick,
            onSettings = onSettingsClick
        )
        return
    }
    // An unconfigured (empty) stack is only useful in edit mode; hide it entirely otherwise.
    if (stack.entityIds.isEmpty() && !isEditMode) return
    val dependencyIds = remember(stack.entityIds, stack.buttonConfigs) {
        buildSet {
            addAll(stack.entityIds)
            stack.buttonConfigs.values.forEach { config ->
                listOfNotNull(
                    config.doorEntityId,
                    config.vacuumMapEntityId,
                    config.vacuumBatteryEntityId,
                    config.climateTempSensorEntityId,
                    config.climateHumiditySensorEntityId,
                    config.weatherEntityId
                ).forEach(::add)
                addAll(config.visibilityConditionEntityIds())
            }
        }.toList()
    }
    val dependencyFlow = remember(viewModel, dependencyIds, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(dependencyIds) else viewModel.entitiesFor(dependencyIds)
    }
    val allEntities by dependencyFlow.collectAsState()
    val entityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }
    // A button's own id is its entity id, so a per-user hide targets it the same way.
    val parentalHiddenItemIds by viewModel.prefs.parentalHiddenItemIds.collectAsState(initial = emptyList())
    // Outside edit mode, drop buttons hidden, outside their scheduled window, or hidden from this
    // user; in edit mode show them all so the user can still reach the visibility settings.
    val entities = remember(stack.entityIds, stack.buttonConfigs, entityById, isEditMode, parentalHiddenItemIds) {
        stack.entityIds
            .filter {
                isEditMode || (
                    it !in parentalHiddenItemIds &&
                        isButtonVisibleNow(
                            stack.buttonConfigs[it] ?: HKIButtonConfig(),
                            resolveEntityState = { id -> entityById[id]?.state }
                        )
                    )
            }
            // Empty buttons have no Home Assistant entity behind them; a stateless placeholder keeps
            // them in the layout so they still occupy their grid cell.
            .mapNotNull { id -> entityById[id] ?: spacerPlaceholderEntity(id) }
    }
    val buttonConfigs = stack.buttonConfigs
    var unlockedUntilByEntity by remember(stack.id) { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var lockNow by remember(stack.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingPinUnlock by remember(stack.id) { mutableStateOf<Pair<String, HKIButtonConfig>?>(null) }

    LaunchedEffect(unlockedUntilByEntity) {
        while (unlockedUntilByEntity.values.any { it > System.currentTimeMillis() }) {
            delay(1.seconds)
            lockNow = System.currentTimeMillis()
        }
        lockNow = System.currentTimeMillis()
    }

    fun unlockButton(entityId: String, config: HKIButtonConfig) {
        unlockedUntilByEntity = unlockedUntilByEntity + (entityId to (System.currentTimeMillis() + buttonRelockMillis(config)))
        lockNow = System.currentTimeMillis()
    }

    fun handleButtonInteraction(entityId: String, config: HKIButtonConfig?, trigger: String, action: () -> Unit) {
        if (config?.lockEnabled != true || !isButtonCurrentlyLocked(config, System.currentTimeMillis(), unlockedUntilByEntity[entityId])) {
            action()
            return
        }
        when (normalizedButtonLockMode(config)) {
            BUTTON_LOCK_PIN -> pendingPinUnlock = entityId to config
            else -> if (trigger == "double") unlockButton(entityId, config)
        }
    }

    pendingPinUnlock?.let { (entityId, config) ->
        ButtonUnlockPinDialog(
            config = config,
            onDismiss = { pendingPinUnlock = null },
            onUnlock = {
                unlockButton(entityId, config)
                pendingPinUnlock = null
            }
        )
    }
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
                        contentDescription = if (isCollapsed) {
                            stringResource(R.string.cr_expand_stack)
                        } else {
                            stringResource(R.string.cr_collapse_stack)
                        },
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (stack.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_hidden_d4c2792), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                }

                if (isEditMode) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_f6fdbe4), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (stack.isHidden) {
                                stringResource(R.string.cr_unhide_stack)
                            } else {
                                stringResource(R.string.cr_hide_stack)
                            },
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.ui_settings_c7f73bb), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onManageOrder,
                        enabled = stack.entityIds.size > 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = stringResource(R.string.ui_manage_order_411f2d9),
                            tint = Color.Gray.copy(alpha = if (stack.entityIds.size > 1) 1f else 0.38f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_add_entity_f99dc1d), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                } else if (stack.showBadge && activeCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = itemCornerShape(),
                        modifier = Modifier.clickable { onBadgeClick() }
                    ) {
                        Text(
                            stringResource(R.string.ui_text_c79f712, activeCount),
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
                        val isLocked = isButtonCurrentlyLocked(cfg, lockNow, unlockedUntilByEntity[entity.entity_id])
                        Box {
                            EntityCard(
                                entity = entity,
                                displayName = cfg?.name,
                                stateAttribute = cfg?.stateAttribute,
                                stateAsTimer = cfg?.stateAsTimer ?: false,
                                timerMachineRunning = cfg?.timerStateEntityId?.let { id -> com.jimz011apps.hki7.ui.components.isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true,
                                iconName = cfg?.icon,
                                iconAnimation = cfg?.iconAnimation ?: "auto",
                                iconOnly = cfg?.iconOnly == true,
                                onClick = { handleButtonInteraction(entity.entity_id, cfg, "tap") { onEntityClick(entity.entity_id) } },
                                onLongClick = { handleButtonInteraction(entity.entity_id, cfg, "hold") { onEntityLongClick(entity.entity_id) } },
                                onDoubleClick = { handleButtonInteraction(entity.entity_id, cfg, "double") { onEntityDoubleClick(entity.entity_id) } },
                                isSquare = stack.isSquare,
                                cornerRadius = stack.cornerRadius,
                                buttonStyle = stack.buttonStyle.takeIf { it.isNotBlank() } ?: if (stack.isSquare) "square" else "standard",
                                showBrightnessSlider = cfg?.showBrightnessSlider == true,
                                onBrightnessChange = { viewModel.setOptimisticBrightness(entity.entity_id, it) },
                                onBrightnessChangeFinished = { viewModel.setBrightness(entity.entity_id, it) },
                                doorOpen = doorOpen,
                                currentUrl = currentUrl
                            )
                            ButtonLockBadge(
                                config = cfg,
                                locked = isLocked,
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                            )
                        }
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
                val columns = stack.columns.coerceIn(1, maxColumns)
                val entityRows = remember(entities, columns) { entities.chunked(columns) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entityRows.forEach { rowEntities ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowEntities.forEach { entity ->
                                if (isSpacerEntityId(entity.entity_id)) {
                                    SpacerButtonCard(
                                        isSquare = stack.isSquare,
                                        buttonStyle = stack.buttonStyle.takeIf { it.isNotBlank() }
                                            ?: if (stack.isSquare) "square" else "standard",
                                        cornerRadius = stack.cornerRadius,
                                        showOutline = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                    return@forEach
                                }
                                val cfg = buttonConfigs[entity.entity_id]
                                val doorOpen = cfg?.doorEntityId?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
                                val isLocked = isButtonCurrentlyLocked(cfg, lockNow, unlockedUntilByEntity[entity.entity_id])
                                Box(modifier = Modifier.weight(1f)) {
                                    EntityCard(
                                        entity = entity,
                                        displayName = cfg?.name,
                                        stateAttribute = cfg?.stateAttribute,
                                        stateAsTimer = cfg?.stateAsTimer ?: false,
                                        timerMachineRunning = cfg?.timerStateEntityId?.let { id -> com.jimz011apps.hki7.ui.components.isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true,
                                        iconName = cfg?.icon,
                                        iconAnimation = cfg?.iconAnimation ?: "auto",
                                        iconOnly = cfg?.iconOnly == true,
                                        onClick = { handleButtonInteraction(entity.entity_id, cfg, "tap") { onEntityClick(entity.entity_id) } },
                                        onLongClick = { handleButtonInteraction(entity.entity_id, cfg, "hold") { onEntityLongClick(entity.entity_id) } },
                                        onDoubleClick = { handleButtonInteraction(entity.entity_id, cfg, "double") { onEntityDoubleClick(entity.entity_id) } },
                                        isSquare = stack.isSquare,
                                        cornerRadius = stack.cornerRadius,
                                        buttonStyle = stack.buttonStyle.takeIf { it.isNotBlank() } ?: if (stack.isSquare) "square" else "standard",
                                        showBrightnessSlider = cfg?.showBrightnessSlider == true,
                                        onBrightnessChange = { viewModel.setOptimisticBrightness(entity.entity_id, it) },
                                        onBrightnessChangeFinished = { viewModel.setBrightness(entity.entity_id, it) },
                                        doorOpen = doorOpen,
                                        currentUrl = currentUrl
                                    )
                                    ButtonLockBadge(
                                        config = cfg,
                                        locked = isLocked,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                                    )
                                }
                            }
                            repeat((columns - rowEntities.size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                val columns = stack.columns.coerceIn(1, maxColumns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entities.chunked(columns).forEach { rowEntities ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowEntities.forEach { entity ->
                                if (isSpacerEntityId(entity.entity_id)) {
                                    // An empty button has nothing to configure, so edit mode only
                                    // outlines it and offers removal.
                                    Box(modifier = Modifier.weight(1f)) {
                                        SpacerButtonCard(
                                            isSquare = stack.isSquare,
                                            buttonStyle = stack.buttonStyle.takeIf { it.isNotBlank() }
                                                ?: if (stack.isSquare) "square" else "standard",
                                            cornerRadius = stack.cornerRadius,
                                            showOutline = true
                                        )
                                        EditRemoveBadge(
                                            onClick = { onRemoveEntity(entity.entity_id) },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        )
                                    }
                                    return@forEach
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    EntityCard(
                                        entity = entity,
                                        displayName = buttonConfigs[entity.entity_id]?.name,
                                        stateAttribute = buttonConfigs[entity.entity_id]?.stateAttribute,
                                        stateAsTimer = buttonConfigs[entity.entity_id]?.stateAsTimer ?: false,
                                        timerMachineRunning = buttonConfigs[entity.entity_id]?.timerStateEntityId?.let { id -> com.jimz011apps.hki7.ui.components.isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true,
                                        iconName = buttonConfigs[entity.entity_id]?.icon,
                                        iconAnimation = buttonConfigs[entity.entity_id]?.iconAnimation ?: "auto",
                                        iconOnly = buttonConfigs[entity.entity_id]?.iconOnly == true,
                                        onClick = { onEntityClick(entity.entity_id) },
                                        onLongClick = { onEntityLongClick(entity.entity_id) },
                                        onDoubleClick = { onEntityDoubleClick(entity.entity_id) },
                                        isSquare = stack.isSquare,
                                        cornerRadius = stack.cornerRadius,
                                        buttonStyle = stack.buttonStyle.takeIf { it.isNotBlank() } ?: if (stack.isSquare) "square" else "standard",
                                        showBrightnessSlider = buttonConfigs[entity.entity_id]?.showBrightnessSlider == true,
                                        onBrightnessChange = { viewModel.setOptimisticBrightness(entity.entity_id, it) },
                                        onBrightnessChangeFinished = { viewModel.setBrightness(entity.entity_id, it) },
                                        interactionsEnabled = false,
                                        currentUrl = currentUrl
                                    )
                                    EditSettingsButton(
                                        onClick = { onButtonSettings(entity.entity_id) },
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    EditRemoveBadge(
                                        onClick = { onRemoveEntity(entity.entity_id) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                }
                            }
                            repeat((columns - rowEntities.size).coerceAtLeast(0)) {
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
fun SingleEntityWidgetItem(
    widget: HKISingleEntityWidget,
    viewModel: MainViewModel,
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
    // Two independent visibility entry points: the widget-level quick hide/schedule toggle, and the
    // per-button visibility editor inside its ButtonConfigDialog (config.hidden/schedule) — either
    // one hiding it is enough.
    if ((!isWidgetVisibleNow(widget) || !isButtonVisibleNow(widget.config)) && !isEditMode) return
    val dependencyIds = remember(widget.entityId, widget.config) {
        listOfNotNull(
            widget.entityId,
            widget.config.doorEntityId,
            widget.config.vacuumMapEntityId,
            widget.config.vacuumBatteryEntityId,
            widget.config.climateTempSensorEntityId,
            widget.config.climateHumiditySensorEntityId,
            widget.config.weatherEntityId
        ).distinct()
    }
    val dependencyFlow = remember(viewModel, dependencyIds, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(dependencyIds) else viewModel.entitiesFor(dependencyIds)
    }
    val allEntities by dependencyFlow.collectAsState()
    val entity = allEntities.find { it.entity_id == widget.entityId }
        ?: spacerPlaceholderEntity(widget.entityId)

    // A standalone empty button is pure spacing: it reserves its cell and, in edit mode, offers the
    // usual delete/hide controls so it can be moved or removed like any other widget.
    if (isSpacerEntityId(widget.entityId)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SpacerButtonCard(
                isSquare = widget.isSquare,
                buttonStyle = widget.buttonStyle.takeIf { it.isNotBlank() }
                    ?: if (widget.isSquare) "square" else "standard",
                cornerRadius = widget.cornerRadius,
                showOutline = isEditMode
            )
            if (isEditMode) {
                EditRemoveBadge(onClick = onDeleteClick, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (entity == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(widget.cornerRadius.dp),
                color = appColors.subtleSurface,
                border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Text(
                    stringResource(R.string.ui_entity_unavailable_3974ddb),
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
                        isEditMode = isEditMode,
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
                    var unlockedUntilMillis by remember(widget.id, widget.entityId) { mutableLongStateOf(0L) }
                    var lockNowMillis by remember(widget.id, widget.entityId) { mutableLongStateOf(System.currentTimeMillis()) }
                    var showPinUnlock by remember(widget.id, widget.entityId) { mutableStateOf(false) }

                    LaunchedEffect(unlockedUntilMillis) {
                        while (unlockedUntilMillis > System.currentTimeMillis()) {
                            delay(1.seconds)
                            lockNowMillis = System.currentTimeMillis()
                        }
                        lockNowMillis = System.currentTimeMillis()
                    }

                    fun unlockButton() {
                        unlockedUntilMillis = System.currentTimeMillis() + buttonRelockMillis(widget.config)
                        lockNowMillis = System.currentTimeMillis()
                    }

                    fun handleSingleButtonInteraction(trigger: String, action: () -> Unit) {
                        if (!widget.config.lockEnabled || !isButtonCurrentlyLocked(widget.config, System.currentTimeMillis(), unlockedUntilMillis)) {
                            action()
                            return
                        }
                        when (normalizedButtonLockMode(widget.config)) {
                            BUTTON_LOCK_PIN -> showPinUnlock = true
                            else -> if (trigger == "double") unlockButton()
                        }
                    }

                    if (showPinUnlock) {
                        ButtonUnlockPinDialog(
                            config = widget.config,
                            onDismiss = { showPinUnlock = false },
                            onUnlock = {
                                unlockButton()
                                showPinUnlock = false
                            }
                        )
                    }

                    val doorOpen = widget.config.doorEntityId?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
                    val isLocked = isButtonCurrentlyLocked(widget.config, lockNowMillis, unlockedUntilMillis)
                    EntityCard(
                        entity = entity,
                        displayName = widget.config.name,
                        stateAttribute = widget.config.stateAttribute,
                        stateAsTimer = widget.config.stateAsTimer,
                        timerMachineRunning = widget.config.timerStateEntityId?.let { id -> com.jimz011apps.hki7.ui.components.isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true,
                        iconName = widget.config.icon,
                        iconAnimation = widget.config.iconAnimation,
                        iconOnly = widget.config.iconOnly,
                        onClick = { handleSingleButtonInteraction("tap") { onEntityClick(widget.entityId) } },
                        onLongClick = { handleSingleButtonInteraction("hold") { onEntityLongClick(widget.entityId) } },
                        onDoubleClick = { handleSingleButtonInteraction("double") { onEntityDoubleClick(widget.entityId) } },
                        isSquare = widget.isSquare,
                        cornerRadius = widget.cornerRadius,
                        buttonStyle = widget.buttonStyle.takeIf { it.isNotBlank() } ?: if (widget.isSquare) "square" else "standard",
                        showBrightnessSlider = widget.config.showBrightnessSlider,
                        onBrightnessChange = { viewModel.setOptimisticBrightness(widget.entityId, it) },
                        onBrightnessChangeFinished = { viewModel.setBrightness(widget.entityId, it) },
                        doorOpen = doorOpen,
                        interactionsEnabled = !isEditMode,
                        currentUrl = currentUrl
                    )
                    if (!isEditMode) {
                        ButtonLockBadge(
                            config = widget.config,
                            locked = isLocked,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                        )
                    }
                }
            }
            if (isEditMode) {
                EditSettingsButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.Center)
                )
                EditRemoveBadge(
                    onClick = onDeleteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

internal fun shouldRenderContainerHeader(
    title: String?,
    icon: String?,
    isEditMode: Boolean,
    isCollapsed: Boolean,
    isHidden: Boolean
): Boolean = !title.isNullOrBlank() || !icon.isNullOrBlank() || isEditMode || isCollapsed || isHidden

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
    if (!isWidgetVisibleNow(stack) && !isEditMode) return
    // An unconfigured (childless) container only matters in edit mode; hide it entirely otherwise.
    if (stack.widgets.isEmpty() && !isEditMode) return
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
        val hasIdentity = !stack.title.isNullOrBlank() || !stack.icon.isNullOrBlank()
        val showChevron = canCollapse && (hasIdentity || isEditMode || isCollapsed)
        if (shouldRenderContainerHeader(stack.title, stack.icon, isEditMode, isCollapsed, stack.isHidden)) {
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
                        if (!stack.title.isNullOrBlank() || isEditMode || isCollapsed || stack.isHidden) {
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    val headerTitle = stack.title?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.cr_widget_swiping_stack)
                            .takeIf { isEditMode || isCollapsed || stack.isHidden }
                    headerTitle?.let {
                        Text(it, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                    if (showChevron) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isCollapsed) {
                                stringResource(R.string.cr_expand_stack)
                            } else {
                                stringResource(R.string.cr_collapse_stack)
                            },
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (stack.isHidden) {
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_hidden_d4c2792), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (isEditMode) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_f6fdbe4), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (stack.isHidden) {
                                stringResource(R.string.cr_unhide_stack)
                            } else {
                                stringResource(R.string.cr_hide_stack)
                            },
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.ui_settings_c7f73bb), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_add_widget_dd4416e), tint = Color.Gray, modifier = Modifier.size(16.dp))
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
                            stringResource(R.string.ui_tap_here_or_use_the_button_while_edit_mode_8baf490),
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
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)
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
    var title by remember(stack) { mutableStateOf(stack.title.orEmpty()) }
    var iconName by remember(stack) { mutableStateOf(stack.icon ?: "None") }
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
    var settingsPage by remember(stack) { mutableStateOf("identity") }
    var showIconPicker by remember { mutableStateOf(false) }
    var visSpec by remember(stack) {
        mutableStateOf(
            stack.toVisibilitySpec()
        )
    }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" }.orEmpty(),
            onDismiss = { showIconPicker = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPicker = false
            }
        )
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                stringResource(R.string.cr_widget_swiping_stack),
                stringResource(R.string.cr_swiping_stack_dialog_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTabRow(
                    tabs = listOf(
                        "identity" to stringResource(R.string.cr_identity),
                        "layout" to stringResource(R.string.cr_layout),
                        "playback" to stringResource(R.string.cr_playback),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "identity") {
                    SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_optional_name_and_icon_shown_above_this_stack_272f237))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_stack_name_7507e93)) },
                        placeholder = { Text(stringResource(R.string.ui_no_heading_162947f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            iconName.takeUnless { it == "None" } ?: stringResource(R.string.cr_none),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                }
                if (settingsPage == "layout") {
                SettingsSubcategory(stringResource(R.string.ui_layout_972ad8d), stringResource(R.string.ui_width_shape_and_collapse_behavior_7abc6f4))
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text(stringResource(R.string.ui_collapsible_c932fac))
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text(stringResource(R.string.ui_collapsed_by_default_65a57c6))
                    }
                }
                }
                if (settingsPage == "playback") {
                SettingsSubcategory(stringResource(R.string.ui_playback_c8e3087), stringResource(R.string.ui_automatic_paging_and_transition_style_792709a))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoplay, onCheckedChange = { autoplay = it })
                    Text(stringResource(R.string.ui_autoplay_95619b5))
                }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.ui_interval_seconds_eaeeb46)) },
                    singleLine = true
                )
                Text(stringResource(R.string.ui_animation_62afd21), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    swipingStackAnimationTypes.forEach { (value, labelRes) ->
                        FilterChip(
                            selected = animation == value,
                            onClick = { animation = value },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(
                    stack.copy(
                        title = title.trim().ifBlank { null },
                        icon = iconName.takeUnless { it == "None" },
                        width = width,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        collapsible = collapsible,
                        defaultCollapsed = defaultCollapsed,
                        isCollapsed = defaultCollapsed,
                        autoplay = autoplay,
                        autoplayIntervalSeconds = intervalText.toIntOrNull()?.coerceIn(1, 120) ?: 5,
                        animation = animation,
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
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
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val maxColumns = LocalMaxStackColumns.current
    if (!isWidgetVisibleNow(stack) && !isEditMode) return
    // An unconfigured (childless) container only matters in edit mode; hide it entirely otherwise.
    if (stack.widgets.isEmpty() && !isEditMode) return
    val canCollapse = stack.collapsible
    val isCollapsed = canCollapse && (stack.isCollapsed ?: stack.defaultCollapsed)
    Column(modifier = Modifier.fillMaxWidth()) {
        val hasIdentity = !stack.title.isNullOrBlank() || !stack.icon.isNullOrBlank()
        val showChevron = canCollapse && (hasIdentity || isEditMode || isCollapsed)
        if (shouldRenderContainerHeader(stack.title, stack.icon, isEditMode, isCollapsed, stack.isHidden)) {
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
                    if (!stack.title.isNullOrBlank() || isEditMode || isCollapsed || stack.isHidden) {
                        Spacer(Modifier.width(8.dp))
                    }
                }
                val headerTitle = stack.title?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.cr_widget_empty_stack)
                        .takeIf { isEditMode || isCollapsed || stack.isHidden }
                headerTitle?.let {
                    Text(it, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
                if (showChevron) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) {
                            stringResource(R.string.cr_expand_stack)
                        } else {
                            stringResource(R.string.cr_collapse_stack)
                        },
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (stack.isHidden) {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_hidden_d4c2792), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isEditMode) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_f6fdbe4), tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onHideClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (stack.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (stack.isHidden) {
                            stringResource(R.string.cr_unhide_stack)
                        } else {
                            stringResource(R.string.cr_hide_stack)
                        },
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.ui_settings_c7f73bb), tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_add_widget_dd4416e), tint = Color.Gray, modifier = Modifier.size(16.dp))
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
                            stringResource(R.string.ui_tap_here_or_use_the_button_while_edit_mode_8baf490),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val columns = stack.columns.coerceIn(1, maxColumns)
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
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val maxColumns = LocalMaxStackColumns.current
    var title by remember(stack) { mutableStateOf(stack.title.orEmpty()) }
    var iconName by remember(stack) { mutableStateOf(stack.icon ?: "None") }
    var width by remember(stack) { mutableStateOf(stack.width) }
    var columns by remember(stack, maxColumns) { mutableIntStateOf(stack.columns.coerceIn(1, maxColumns)) }
    var showBadge by remember(stack) { mutableStateOf(stack.showBadge) }
    var isSquare by remember(stack) { mutableStateOf(stack.isSquare) }
    var cornerRadius by remember(stack) { mutableIntStateOf(stack.cornerRadius) }
    var collapsible by remember(stack) { mutableStateOf(stack.collapsible) }
    var defaultCollapsed by remember(stack) { mutableStateOf(stack.defaultCollapsed) }
    var settingsPage by remember(stack) { mutableStateOf("identity") }
    var showIconPicker by remember { mutableStateOf(false) }
    var visSpec by remember(stack) {
        mutableStateOf(
            stack.toVisibilitySpec()
        )
    }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" }.orEmpty(),
            onDismiss = { showIconPicker = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPicker = false
            }
        )
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                stringResource(R.string.cr_widget_empty_stack),
                stringResource(R.string.cr_empty_stack_dialog_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTabRow(
                    tabs = listOf(
                        "identity" to stringResource(R.string.cr_identity),
                        "layout" to stringResource(R.string.cr_layout),
                        "behavior" to stringResource(R.string.cr_behavior),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "identity") {
                    SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_name_and_icon_shown_above_this_container_9076bde))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_stack_name_7507e93)) },
                        placeholder = { Text(stringResource(R.string.ui_empty_stack_79d38d5)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            iconName.takeUnless { it == "None" } ?: stringResource(R.string.cr_none),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                }
                if (settingsPage == "layout") {
                SettingsSubcategory(stringResource(R.string.ui_layout_972ad8d), stringResource(R.string.ui_width_columns_and_container_shape_984a191))
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text(stringResource(R.string.ui_columns_cf723c5), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..maxColumns).forEach { count ->
                        FilterChip(selected = columns == count, onClick = { columns = count }, label = { Text(stringResource(R.string.ui_text_c79f712, count)) })
                    }
                }
                Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                }
                }
                if (settingsPage == "behavior") {
                SettingsSubcategory(stringResource(R.string.ui_behavior_70cb647), stringResource(R.string.ui_activity_indication_and_collapsing_0764a94))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBadge, onCheckedChange = { showBadge = it })
                    Text(stringResource(R.string.ui_show_activity_badge_f5d56bb))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = collapsible, onCheckedChange = { collapsible = it })
                    Text(stringResource(R.string.ui_collapsible_c932fac))
                }
                if (collapsible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = defaultCollapsed, onCheckedChange = { defaultCollapsed = it })
                        Text(stringResource(R.string.ui_collapsed_by_default_65a57c6))
                    }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(
                    stack.copy(
                        title = title.trim().ifBlank { null },
                        icon = iconName.takeUnless { it == "None" },
                        width = width,
                        columns = columns.coerceIn(1, maxColumns),
                        showBadge = showBadge,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        collapsible = collapsible,
                        defaultCollapsed = defaultCollapsed,
                        isCollapsed = defaultCollapsed,
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
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
        shape = itemCornerShape(),
        color = appColors.subtleSurface,
        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.ui_tap_here_or_use_the_button_while_edit_mode_b06aac5),
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CameraStackContent(
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
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val maxColumns = LocalMaxStackColumns.current
    val customCameraLabel = stringResource(R.string.ui_custom_camera_ada9096)
    val cameraSources = remember(stack.entityIds, stack.buttonConfigs, entityById, currentUrl, customCameraLabel) {
        stack.entityIds.mapNotNull { entityId ->
            val entity = entityById[entityId]
            val config = stack.buttonConfigs[entityId]
            val refreshInterval = config?.cameraRefreshInterval ?: 5
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
            val label = config?.name ?: entity?.friendlyName ?: if (config?.isCustomUrl == true) customCameraLabel else entityId
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
                liveWebUrl = liveWebUrl,
                state = entity?.state
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
                Text(stringResource(R.string.ui_no_camera_selected_717ad35), color = Color.Gray)
            }
        }
        return
    }

    if (isEditMode) {
        val columns = stack.columns.coerceIn(1, maxColumns)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cameraSources.chunked(columns).forEach { row ->
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
                    repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
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
    val liveWebUrl: String?,
    val state: String? = null
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
    val currentOnEntityClick by rememberUpdatedState(onEntityClick)
    val currentSourceId by rememberUpdatedState(source.id)
    var mediaReady by remember(source.id, isEditMode) { mutableStateOf(false) }
    LaunchedEffect(source.id, isEditMode) {
        // Let the grid finish its first layout before image decoders/WebViews compete for CPU, and
        // spread multiple visible cameras across frames instead of initializing them in one burst.
        val stagger = (source.id.hashCode().ushr(1) % 6) * 120L
        delay(((if (isEditMode) 120L else 250L) + stagger).milliseconds)
        mediaReady = true
    }
    var refreshTick by remember(source.id, source.refreshIntervalSeconds, source.imageUrl) { mutableIntStateOf(0) }
    LaunchedEffect(source.id, source.refreshIntervalSeconds, source.imageUrl, source.liveWebUrl) {
        refreshTick = 0
        if (!isEditMode && source.refreshIntervalSeconds > 0 && source.liveWebUrl.isNullOrBlank()) {
            while (true) {
                delay(source.refreshIntervalSeconds.seconds)
                refreshTick += 1
            }
        }
    }

    val model = remember(source.imageUrl, refreshTick, source.refreshIntervalSeconds) {
        buildCameraRefreshModel(source.imageUrl, source.refreshIntervalSeconds, refreshTick)
    }
    val displayedModel = remember(model, isEditMode) {
        if (isEditMode) model?.replace("/camera_proxy_stream/", "/camera_proxy/") else model
    }
    val aspectRatio = if (stack.isSquare) 1f else stack.cameraAspectRatio

    Box(
        modifier = modifier.aspectRatio(aspectRatio)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = !isEditMode) { onEntityClick(source.id) },
            // Match the corner radius of every other widget instead of the camera's own default.
            shape = itemCornerShape(),
            color = Color.Black
        ) {
            Box {
            if (!mediaReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.ui_loading_camera_50a8065), color = Color.Gray)
                }
            } else if (!isEditMode && !source.liveWebUrl.isNullOrBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                            var downX = 0f
                            var downY = 0f
                            var tapCandidate = false
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setOnTouchListener { view, event ->
                                when (event.actionMasked) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        downX = event.x
                                        downY = event.y
                                        tapCandidate = true
                                    }
                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        if (kotlin.math.abs(event.x - downX) > touchSlop ||
                                            kotlin.math.abs(event.y - downY) > touchSlop
                                        ) tapCandidate = false
                                    }
                                    android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                                        tapCandidate = false
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    android.view.MotionEvent.ACTION_UP -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                        view.performClick()
                                        if (tapCandidate) currentOnEntityClick(currentSourceId)
                                        tapCandidate = false
                                    }
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        tapCandidate = false
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }
                            // Crop the stream to fill the tile (no letterbox bars) by forcing the
                            // MJPEG <img>/<video> to cover the viewport once the page has loaded.
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String?) {
                                    view.evaluateJavascript(
                                        "(function(){document.documentElement.style.height='100%';document.body.style.height='100%';document.body.style.margin='0';document.body.style.background='#000';document.querySelectorAll('img,video').forEach(function(e){e.style.width='100%';e.style.height='100%';e.style.objectFit='cover';});})();",
                                        null
                                    )
                                }
                            }
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
            } else if (displayedModel != null) {
                ZoomableCameraImage(
                    imageUrl = displayedModel,
                    contentDescription = source.label,
                    onTap = { if (!isEditMode) currentOnEntityClick(currentSourceId) },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.ui_no_stream_available_6bfd14a), color = Color.Gray)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                // Match the name/state pill on the vacuum/waste/parcel widgets (global item corner).
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(source.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    // Recording state gets a red dot in front of the state text.
                    val recording = source.state.equals("recording", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (recording) {
                            Box(Modifier.size(6.dp).background(Color(0xFFEF5350), CircleShape))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            buildString {
                                if (recording) append(stringResource(R.string.ui_recording_09b5b61))
                                append(if (source.refreshIntervalSeconds > 0) stringResource(R.string.ui_s_refresh_10a9b0b, source.refreshIntervalSeconds) else stringResource(R.string.ui_live_65c821a))
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            }
        }
        if (isEditMode) {
            EditSettingsButton(
                onClick = { onButtonSettings(source.id) },
                modifier = Modifier.align(Alignment.Center)
            )
            EditRemoveBadge(
                onClick = { onRemoveEntity(source.id) },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * The temperature slider and fan/swing modes list share one top-anchored layout.
 * This keeps the modes toggle button in the same spot across views.
 * The middle region absorbs size differences via Modifier.weight.
 */
@Composable
private fun ClimateControlContent(entity: HAEntity, viewModel: MainViewModel, showModes: Boolean, onToggleModes: (Boolean) -> Unit, useDial: Boolean = false) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val minTemp = entity.attributes?.get("min_temp")?.jsonPrimitive?.doubleOrNull ?: 15.0
    val maxTemp = entity.attributes?.get("max_temp")?.jsonPrimitive?.doubleOrNull ?: 30.0
    val targetTemp = entity.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull ?: 21.0
    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull

    var localTarget by remember(targetTemp) { mutableFloatStateOf(targetTemp.toFloat()) }
    var localMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { localMode = entity.state }
    val activeHvacMode = hvacAction ?: localMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (showModes) 32.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showModes || !useDial) {
            Text(
                text = if (showModes) stringResource(R.string.ui_modes_79f5b22) else String.format(locale, "%.1f\u00B0", localTarget),
                color = appColors.onSurface,
                style = if (showModes) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(24.dp))
        }
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showModes) {
                ClimateModesList(entity, viewModel)
            } else if (useDial) {
                ClimateDialogDial(
                    value = localTarget,
                    minValue = minTemp.toFloat(),
                    maxValue = maxTemp.toFloat(),
                    step = (entity.attributes?.get("target_temp_step")?.jsonPrimitive?.doubleOrNull ?: 0.5).toFloat(),
                    activeColor = hvacColor(activeHvacMode),
                    activeGradient = hvacGradient(activeHvacMode),
                    onValueChange = { localTarget = it },
                    onValueChangeFinished = { viewModel.setClimateTemp(entity.entity_id, localTarget) }
                )
            } else {
                VerticalSlider(
                    value = ((localTarget - minTemp.toFloat()) / (maxTemp.toFloat() - minTemp.toFloat())).coerceIn(0f, 1f),
                    onValueChange = { localTarget = minTemp.toFloat() + it * (maxTemp.toFloat() - minTemp.toFloat()) },
                    onValueChangeFinished = { viewModel.setClimateTemp(entity.entity_id, localTarget) },
                    gradient = hvacGradient(activeHvacMode)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (showModes) stringResource(R.string.ui_modes_6fc3ca8) else stringResource(R.string.ui_target_f762108), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        ClimateModesButton(entity, onClick = { onToggleModes(!showModes) }, selected = showModes)
    }
}

@Composable
private fun ClimateDialogDial(
    value: Float,
    minValue: Float,
    maxValue: Float,
    step: Float,
    activeColor: Color,
    activeGradient: Brush,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val ringWidth = 38.dp
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(minValue, maxValue, step) {
                var dragging = false
                fun valueAt(position: Offset): Float {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    var angle = Math.toDegrees(atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    val sweep = when {
                        angle >= 135f -> angle - 135f
                        angle <= 45f -> angle + 225f
                        angle < 90f -> 270f
                        else -> 0f
                    }
                    val raw = minValue + (sweep / 270f).coerceIn(0f, 1f) * (maxValue - minValue)
                    return ((raw / step).roundToInt() * step).coerceIn(minValue, maxValue)
                }
                detectDragGestures(
                    onDragStart = { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val distance = (position - center).getDistance()
                        val radius = minOf(size.width, size.height) / 2f
                        dragging = distance >= radius - ringWidth.toPx() * 1.6f
                        if (dragging) onValueChange(valueAt(position))
                    },
                    onDrag = { change, _ ->
                        if (dragging) {
                            change.consume()
                            onValueChange(valueAt(change.position))
                        }
                    },
                    onDragEnd = { if (dragging) onValueChangeFinished(); dragging = false },
                    onDragCancel = { dragging = false }
                )
            }
        ) {
            val strokePx = ringWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            drawArc(appColors.onMuted.copy(alpha = 0.18f), 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(strokePx))
            drawArc(activeGradient, 135f, 270f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(strokePx))
            val angle = Math.toRadians((135f + 270f * fraction).toDouble())
            val radius = (size.minDimension - strokePx) / 2f
            val handle = Offset(
                size.width / 2f + (radius * cos(angle)).toFloat(),
                size.height / 2f + (radius * sin(angle)).toFloat()
            )
            drawCircle(Color.White, strokePx * 0.28f, handle)
            drawCircle(activeColor, strokePx * 0.16f, handle)
        }
        Surface(shape = CircleShape, color = appColors.surface, shadowElevation = 8.dp, modifier = Modifier.fillMaxSize(0.58f)) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.ui_target_f762108), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    Text(stringResource(R.string.ui_1f_b07a377).format(value), style = MaterialTheme.typography.displayMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LockControlContent(
    entity: HAEntity,
    viewModel: MainViewModel,
    accentColor: Color = entityStateIconColor(entity)
) {
    val appColors = LocalHKIAppColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (entity.state == "locked") stringResource(R.string.ui_door_locked_4b46012) else stringResource(R.string.ui_door_open_74899a4),
            color = appColors.onSurface,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VerticalMasterSwitch(
                isOn = entity.state == "locked",
                onToggle = { viewModel.toggleLock(entity.entity_id) },
                accentColor = accentColor
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ui_lock_c37eb4c), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun BlindControlContent(entity: HAEntity, viewModel: MainViewModel, activeColor: Color = coverAccentColor(entity)) {
    val appColors = LocalHKIAppColors.current
    val currentPosition = entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0
    var localPosition by remember(currentPosition) { mutableFloatStateOf(currentPosition / 100f) }
    val hasTilt = entity.supportsTilt
    var localTilt by remember(entity.tiltPosition) { mutableFloatStateOf((entity.tiltPosition ?: 0) / 100f) }
    val trackColor = activeColor.copy(alpha = 0.18f)
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.ui_text_fc9db15, (localPosition * 100).toInt()),
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
            Text(stringResource(R.string.ui_position_d0b0314), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            if (hasTilt) Text(stringResource(R.string.ui_tilt_aceae6f), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
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
    iconName: String? = null
) {
    if (entities.isEmpty()) { onDismiss(); return }
    var page by remember(entities.map { it.entity_id }) { mutableIntStateOf(0) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val entity = entities[page.coerceIn(0, entities.lastIndex)]
    val pos = entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0
    val accent = coverAccentColor(entity)
    val openLabel = stringResource(R.string.cr_open)
    val stopLabel = stringResource(R.string.cr_stop)
    val closeLabel = stringResource(R.string.cr_close)

    var selectedAction by remember(entity.entity_id, openLabel, stopLabel, closeLabel) {
        mutableStateOf(
            when (entity.state) {
                "opening", "open" -> openLabel
                "closing", "closed" -> closeLabel
                else -> stopLabel
            }
        )
    }
    val tabs = listOf(
        Triple(openLabel, Icons.Default.ArrowUpward, { selectedAction = openLabel; viewModel.controlCover(entity.entity_id, "open_cover") }),
        Triple(stopLabel, Icons.Default.Stop, { selectedAction = stopLabel; viewModel.controlCover(entity.entity_id, "stop_cover") }),
        Triple(closeLabel, Icons.Default.ArrowDownward, { selectedAction = closeLabel; viewModel.controlCover(entity.entity_id, "close_cover") })
    )
    val localizedState = localizedEntityState(entity).uppercase(Locale.getDefault())
    val status = if (entities.size > 1) {
        stringResource(R.string.cr_paged_cover_status, page + 1, entities.size, pos, localizedState)
    } else {
        stringResource(R.string.cr_cover_status, pos, localizedState)
    }

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Window,
        iconTint = accent,
        iconName = iconName?.takeUnless { it.isBlank() } ?: defaultEntityIconSlug(entity),
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
        shape = itemCornerShape(),
        color = Color.Black
    ) {
        if (streamUrl != null) {
            AsyncImage(
                model = streamUrl,
                contentDescription = stringResource(R.string.ui_camera_feed_cc1f19e),
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ui_no_stream_available_6bfd14a), color = Color.Gray)
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

    val tabs = buildList {
        add(GroupDialogTab("power", stringResource(R.string.cr_power), Icons.Default.Power))
        if (lightEntities.isNotEmpty()) {
            add(GroupDialogTab("brightness", stringResource(R.string.cr_brightness), Icons.Default.LightMode))
        }
        if (hasColorTemp) {
            add(GroupDialogTab("temperature", stringResource(R.string.cr_temperature), Icons.Default.Thermostat))
        }
        if (hasColor) {
            add(GroupDialogTab("color", stringResource(R.string.cr_color), Icons.Default.Palette))
        }
    }
    var currentTab by remember(entities.map { it.entity_id }) {
        mutableStateOf(if (lightEntities.isNotEmpty()) "brightness" else "power")
    }

    fun labelFor(entity: HAEntity) = entity.friendlyName ?: entity.entity_id
    @Composable
    fun tintFor(entity: HAEntity): Color {
        val isOn = entity.state == "on"
        return if (isOn) lightStateColor(entity) ?: MaterialTheme.colorScheme.primary else appColors.onMuted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeToAdjacentTab(
                tabs = tabs.map(GroupDialogTab::key),
                selected = currentTab,
                onSelect = { currentTab = it }
            )
    ) {
        val coverListState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (tabs.size > 1) 88.dp else 0.dp)
                .fadingEdges(coverListState),
            state = coverListState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentTab) {
                "brightness" -> items(lightEntities.size) { index ->
                    val entity = lightEntities[index]
                    GroupLightControlRow(label = labelFor(entity), icon = defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                        HorizontalLightBar(
                            entity = entity,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
                "temperature" -> {
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
                "color" -> {
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
                        shape = itemCornerShape(),
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
                                    if (isOn) stringResource(R.string.ui_on_e0049a6) else stringResource(R.string.ui_off_e3de5ab),
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
    tabs: List<GroupDialogTab>,
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
        tabs.forEach { tab ->
            val isSelected = currentTab == tab.key
            Column(
                modifier = Modifier.weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(tab.key) }
                    .padding(vertical = if (denseTabs) 10.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tab.icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                    modifier = Modifier.size(if (denseTabs) 22.dp else 24.dp)
                )
                Text(
                    tab.label,
                    color = if (isSelected) appColors.onSurface else appColors.onMuted,
                    style = if (denseTabs) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

private data class GroupDialogTab(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun GroupLightControlRow(label: String, icon: String?, iconTint: Color, content: @Composable () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = itemCornerShape(),
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
    // Read-only domains (sensors, presence trackers) have no turn-off service; hide the controls
    // entirely rather than show a button that does nothing.
    fun isControllable(e: HAEntity) = e.entity_id.substringBefore(".") !in setOf("binary_sensor", "sensor", "person", "device_tracker")
    val displayEntities = entities.filter { isActive(it) }
    val activeCount = displayEntities.size
    val anyControllable = displayEntities.any(::isControllable)
    // Auto-dismiss when every entity in the group is no longer active (turned off here, or a
    // read-only sensor like an open door going back to closed on its own).
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
    val tabs = buildList {
        add(GroupDialogTab("power", stringResource(R.string.cr_power), Icons.Default.Power))
        if (lightEntities.isNotEmpty()) {
            add(GroupDialogTab("brightness", stringResource(R.string.cr_brightness), Icons.Default.LightMode))
        }
        if (hasColorTemp) {
            add(GroupDialogTab("temperature", stringResource(R.string.cr_temperature), Icons.Default.Thermostat))
        }
        if (hasColor) {
            add(GroupDialogTab("color", stringResource(R.string.cr_color), Icons.Default.Palette))
        }
    }
    var currentTab by remember(stack.id) { mutableStateOf("power") }

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
            colors = CardDefaults.cardColors(
                containerColor = appColors.elevated,
                contentColor = appColors.onSurface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeToAdjacentTab(
                        tabs = tabs.map(GroupDialogTab::key),
                        selected = currentTab,
                        onSelect = { currentTab = it }
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                appColors.elevated,
                                appColors.elevated
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MdiIcon(stack.icon, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stack.title ?: stringResource(R.string.ui_entities_f7638a2), style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                            Text(
                                if (activeCount > 0) stringResource(R.string.ui_active_25ff32f, activeCount, entities.size) else stringResource(R.string.ui_all_off_97b7833),
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onMuted
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(appColors.subtleSurface, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ui_close_bbfa773), tint = appColors.onSurface, modifier = Modifier.size(24.dp))
                        }
                    }
                    if (anyControllable) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { displayEntities.filter(::isControllable).forEach { turnOff(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(stringResource(R.string.ui_turn_off_all_3a7a2b0))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val groupListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = if (tabs.size > 1) 88.dp else 0.dp)
                            .fadingEdges(groupListState),
                        state = groupListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (currentTab) {
                            "brightness" -> items(lightEntities.size) { index ->
                                val entity = lightEntities[index]
                                GroupLightControlRow(label = labelFor(entity), icon = stack.buttonConfigs[entity.entity_id]?.icon ?: defaultEntityIconSlug(entity), iconTint = tintFor(entity)) {
                                    HorizontalLightBar(
                                        entity = entity,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                }
                            }
                            "temperature" -> {
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
                            "color" -> {
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
                                        if (entity.supportsBrightness) stringResource(R.string.ui_on_12469c7, bp) else stringResource(R.string.ui_on_e0049a6)
                                    } else {
                                        localizedEntityState(entity)
                                    }
                                    "climate" -> {
                                        val mode = localizedClimateModeLabel(
                                            entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                                            ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                                            ?: entity.state
                                        )
                                        val temp = entity.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull
                                        if (temp != null && isItemActive) stringResource(R.string.ui_c_3d3c16f, mode, temp.toInt()) else mode
                                    }
                                    "binary_sensor" -> localizedBinarySensorFriendlyState(entity)
                                    else -> localizedEntityState(entity)
                                }
                                Surface(
                                    shape = itemCornerShape(),
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
                                        if (isControllable(entity)) {
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

/** Home Assistant's binary_sensor device classes translate raw on/off state into class-appropriate
 *  wording in HA's own frontend (e.g. a door reads "Open"/"Closed", not "On"/"Off"). */
@Composable
private fun localizedBinarySensorFriendlyState(entity: HAEntity): String {
    if (entity.state != "on" && entity.state != "off") {
        return localizedStateToken(entity.state)
    }
    val on = entity.state == "on"
    return when (entity.deviceClass) {
        "door", "garage_door", "window", "opening" ->
            stringResource(if (on) R.string.cr_open else R.string.cr_closed)
        "lock" -> stringResource(if (on) R.string.cr_unlocked else R.string.cr_locked)
        "moisture" -> stringResource(if (on) R.string.cr_wet else R.string.cr_dry)
        "motion", "moving", "occupancy", "presence" ->
            stringResource(if (on) R.string.cr_detected else R.string.cr_clear)
        "problem", "safety", "tamper" ->
            stringResource(if (on) R.string.cr_problem else R.string.cr_ok)
        "smoke", "gas", "co" ->
            stringResource(if (on) R.string.cr_detected else R.string.cr_clear)
        "battery" -> stringResource(if (on) R.string.cr_low else R.string.cr_normal)
        "connectivity" ->
            stringResource(if (on) R.string.cr_connected else R.string.cr_disconnected)
        "plug", "power" ->
            stringResource(if (on) R.string.cr_plugged_in else R.string.cr_unplugged)
        else -> stringResource(if (on) R.string.cr_state_on else R.string.cr_state_off)
    }
}

@Composable
private fun localizedEntityState(entity: HAEntity): String {
    if (entity.entity_id.startsWith("binary_sensor.")) {
        return localizedBinarySensorFriendlyState(entity)
    }
    return localizedStateToken(entity.state)
}

@Composable
private fun localizedStateToken(state: String): String =
    when (state.trim().lowercase(Locale.ROOT)) {
        "on" -> stringResource(R.string.cr_state_on)
        "off" -> stringResource(R.string.cr_state_off)
        "open" -> stringResource(R.string.cr_open)
        "closed" -> stringResource(R.string.cr_closed)
        "opening" -> stringResource(R.string.cr_opening)
        "closing" -> stringResource(R.string.cr_closing)
        "locked" -> stringResource(R.string.cr_locked)
        "unlocked" -> stringResource(R.string.cr_unlocked)
        "unavailable" -> stringResource(R.string.cr_unavailable)
        "unknown" -> stringResource(R.string.cr_unknown)
        "idle" -> stringResource(R.string.cr_idle)
        "home" -> stringResource(R.string.cr_home)
        "not_home", "away" -> stringResource(R.string.cr_away)
        "cleaning" -> stringResource(R.string.cr_cleaning)
        "returning" -> stringResource(R.string.cr_returning)
        "paused" -> stringResource(R.string.cr_paused)
        "error" -> stringResource(R.string.cr_error)
        "playing" -> stringResource(R.string.cr_playing)
        "buffering" -> stringResource(R.string.cr_buffering)
        "standby" -> stringResource(R.string.cr_standby)
        "humidifying" -> stringResource(R.string.cr_humidifying)
        "drying" -> stringResource(R.string.cr_drying)
        else -> state.replace('_', ' ').replaceFirstChar(Char::uppercase)
    }

@Composable
fun GenericEntityDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    titleOverride: String? = null,
    iconName: String? = null
) {
    val domain = entity.entity_id.substringBefore(".")
    val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
    val valueText = if (domain == "binary_sensor") {
        localizedBinarySensorFriendlyState(entity)
    } else {
        listOfNotNull(localizedEntityState(entity), unit).joinToString(" ")
    }
    val isSwitchLike = domain in listOf("light", "switch", "input_boolean", "fan", "automation", "group", "remote", "siren", "humidifier")
    val isGraphLike = domain == "sensor"
    val isBinary = domain == "binary_sensor"
    val isSelectLike = domain == "select" || domain == "input_select"
    // select/input_select expose their choices via the "options" attribute; the current state is
    // the active option. The dialog defaults to a pickable list, mirroring the light effects list.
    val selectOptions = remember(entity) {
        (entity.attributes?.get("options") as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    }
    val icon = when (domain) {
        "switch", "input_boolean" -> Icons.Default.PowerSettingsNew
        "binary_sensor" -> Icons.Default.Security
        "sensor" -> Icons.Default.Sensors
        "fan" -> Icons.Default.Air
        "select", "input_select" -> Icons.AutoMirrored.Filled.FormatListBulleted
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
        showHistoryButton = !isGraphLike && !isBinary && !isSelectLike,
        statusText = valueText.uppercase()
    ) {
        val appColors = LocalHKIAppColors.current
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isSelectLike) {
                SelectOptionsContent(
                    options = selectOptions,
                    activeOption = entity.state,
                    onSelect = { option ->
                        viewModel.callService(
                            domain, "select_option",
                            HAServiceCall(entity_id = entity.entity_id, option = option)
                        )
                    }
                )
            } else if (isSwitchLike) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (entity.state == "on") stringResource(R.string.ui_on_e0049a6) else stringResource(R.string.ui_off_e3de5ab),
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
                    Text(stringResource(R.string.ui_switch_b02cbd9), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            } else if (isBinary) {
                HistoryView(entity = entity, viewModel = viewModel)
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
                        shape = itemCornerShape(),
                        color = appColors.subtleSurface,
                        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.ui_open_history_for_graph_ae801d5), color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Pickable options list for select/input_select entities, styled like the light effects list. */
@Composable
private fun SelectOptionsContent(
    options: List<String>,
    activeOption: String?,
    onSelect: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .fadingEdges(scrollState)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.ui_options_6bf5da9), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ui_select_an_option_08d7aa3), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        if (options.isEmpty()) {
            Text(stringResource(R.string.ui_no_options_available_7a19b6d), color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
        }
        options.forEach { option ->
            val selected = option == activeOption
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(option) },
                shape = itemCornerShape(),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else appColors.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else appColors.onMuted.copy(alpha = 0.16f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = option.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    val onLabel = stringResource(R.string.ui_on_e0049a6)
    val offLabel = stringResource(R.string.ui_off_e3de5ab)

    var selectedHours by remember { mutableIntStateOf(24) }
    var loadedHours by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(entity.entity_id, selectedHours) {
        loadedHours = null
        viewModel.fetchEntityHistory(entity.entity_id, selectedHours.toLong())
        loadedHours = selectedHours
    }

    val graphPoints = remember(history, selectedHours, isBinary, unit, onLabel, offLabel) {
        history.mapNotNull { entry ->
            val millis = parseHistoryMillis(entry.last_changed) ?: return@mapNotNull null
            val value = if (isBinary) {
                when (entry.state.lowercase()) {
                    "on", "open", "detected", "home", "true" -> 1f
                    "off", "closed", "clear", "not_home", "false" -> 0f
                    else -> return@mapNotNull null
                }
            } else entry.state.toFloatOrNull() ?: return@mapNotNull null
            val label = if (isBinary) (if (value == 1f) onLabel else offLabel)
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
            text = if (isBinary) stringResource(R.string.ui_state_bef84bb) else stringResource(R.string.ui_sensor_efb9fbf),
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
            shape = itemCornerShape(),
            color = appColors.subtleSurface,
            border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
        ) {
            if (graphPoints.size < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (loadedHours == selectedHours) {
                        Text(stringResource(R.string.ui_not_enough_data_in_this_period_48188c4), color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
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
            Text(stringResource(R.string.ui_last_h_3faaee1, selectedHours), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
        if (!isBinary && minValue != null && maxValue != null) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GraphStat(stringResource(R.string.cr_min), trimGraphValue(minValue), unit)
                avgValue?.let { GraphStat(stringResource(R.string.cr_avg), trimGraphValue(it), unit) }
                GraphStat(stringResource(R.string.cr_max), trimGraphValue(maxValue), unit)
                graphPoints.lastOrNull()?.let {
                    GraphStat(stringResource(R.string.cr_now), trimGraphValue(it.value), unit)
                }
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
            text = if (unit.isNotBlank()) stringResource(R.string.ui_text_78c505f, value, unit) else value,
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
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
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
            EditSettingsButton(onClick = onSettings)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_f6fdbe4), tint = appColors.onMuted, modifier = Modifier.size(18.dp))
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
    var settingsPage by remember(widget) { mutableStateOf("content") }
    var visSpec by remember(widget) {
        mutableStateOf(
            widget.toVisibilitySpec()
        )
    }
    val defaultHeaderText = stringResource(R.string.cr_widget_header_text)

    if (showIconPickerHeader) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPickerHeader = false },
            onSelect = { iconName = it; showIconPickerHeader = false }
        )
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                stringResource(R.string.cr_widget_header_text),
                stringResource(R.string.cr_header_text_dialog_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTabRow(
                    tabs = listOf(
                        "content" to stringResource(R.string.cr_content),
                        "appearance" to stringResource(R.string.cr_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                SettingsSubcategory(stringResource(R.string.ui_content_4f9be05), stringResource(R.string.ui_the_heading_shown_between_dashboard_sections_b03805f))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.ui_text_c3328c3)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                }
                if (settingsPage == "appearance") {
                SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_width_and_optional_icon_671e2ef))
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    if (iconName.isNotEmpty()) MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPickerHeader = true }) { Text(if (iconName.isEmpty()) stringResource(R.string.ui_choose_78b7c9f) else stringResource(R.string.ui_change_64fbd99)) }
                    if (iconName.isNotEmpty()) TextButton(onClick = { iconName = "" }) { Text(stringResource(R.string.ui_none_6eef664)) }
                }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        text = text.ifBlank { defaultHeaderText },
                        width = width,
                        icon = iconName.ifEmpty { null },
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

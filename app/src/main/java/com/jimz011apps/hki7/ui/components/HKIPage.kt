@file:Suppress("MoveLambdaOutsideParentheses", "SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import android.app.Activity
import androidx.navigation.NavController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIAreaConfig
import com.jimz011apps.hki7.data.HKIBadgeBarConfig
import com.jimz011apps.hki7.data.HKICustomPage
import com.jimz011apps.hki7.data.HKIPageConfig
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.GreetingPeriod
import com.jimz011apps.hki7.ui.ConnectionStatus
import com.jimz011apps.hki7.ui.localizedStateLabel
import com.jimz011apps.hki7.ui.screens.SettingsDialog
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

/** Lower = more urgent; the pill shows the most urgent of the selected alarms. */
private fun alarmDisplayPriority(state: String): Int = when (state.lowercase()) {
    "triggered" -> 0
    "pending", "arming", "disarming" -> 1
    "armed_home", "armed_away", "armed_night", "armed_vacation", "armed_custom_bypass" -> 2
    "disarmed" -> 3
    else -> 4
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun greetingText(period: GreetingPeriod): String = when (period) {
    GreetingPeriod.MORNING -> stringResource(R.string.core_greeting_morning)
    GreetingPeriod.AFTERNOON -> stringResource(R.string.core_greeting_afternoon)
    GreetingPeriod.EVENING -> stringResource(R.string.core_greeting_evening)
    GreetingPeriod.NIGHT -> stringResource(R.string.core_greeting_night)
}

@Composable
fun HKIPage(
    viewModel: MainViewModel,
    areaId: String? = null,
    title: String? = null,
    subtitle: String? = null,
    subtitleIcon: ImageVector? = null,
    showPeople: Boolean = false,
    onPeopleClick: ((HAEntity) -> Unit)? = null,
    backgroundImage: String? = null,
    headerColor: String? = null,
    pageKey: String? = null,
    pageSettingsTitle: String? = null,
    customPage: HKICustomPage? = null,
    onCustomPageSave: (HKICustomPage) -> Unit = {},
    extraPageSettingsSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit>? = null,
    additionalPageSettingsSections: List<Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit>> = emptyList(),
    onBack: (() -> Unit)? = null,
    showBadgeBar: Boolean = true,
    /** Whether the header summary and unread edge marker are shown on this page. */
    showNotificationStatus: Boolean = true,
    /** Pinned bar between the header and the scrolling content (e.g. the energy time filter). */
    headerBar: (@Composable () -> Unit)? = null,
    /** Optional compact content beside the title, rendered inside the header. */
    headerTrailingContent: (@Composable (Color) -> Unit)? = null,
    /** Optional secondary content below the subtitle, rendered inside the header. */
    headerBottomContent: (@Composable (Color) -> Unit)? = null,
    /** Optional NavController so badge actions can navigate within the app. */
    navController: NavController? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val greeting = greetingText(viewModel.greetingPeriod)
    val weather by viewModel.weather.collectAsState()
    val people by viewModel.people.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val aestheticsOnly by viewModel.aestheticsOnlyEditing.collectAsState()
    val status by viewModel.status.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationCount = notifications.count { !it.read && !it.archived }
    val appColors = LocalHKIAppColors.current
    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val pageConfig = pageKey?.let { pageConfigs[it] } ?: HKIPageConfig()
    val areaConfigs by viewModel.areaConfigsMapping.collectAsState()
    val areaConfig = areaId?.let { areaConfigs[it] }
    val alarmEntityFlow = remember(viewModel) {
        viewModel.entitiesMatching("domain:alarm_control_panel") { it.entity_id.startsWith("alarm_control_panel.") }
    }
    val allEntities by alarmEntityFlow.collectAsState()
    var previewBadgeBarConfig by remember { mutableStateOf<HKIBadgeBarConfig?>(null) }
    val savedBadgeBarConfig = areaConfig?.badgeBar ?: pageConfig.badgeBar
    val badgeBarConfig: HKIBadgeBarConfig = previewBadgeBarConfig ?: savedBadgeBarConfig ?: HKIBadgeBarConfig()
    val prefs = viewModel.prefs
    val headerVisible by prefs.headerVisible.collectAsState(initial = true)
    
    var showWeatherDialog by remember { mutableStateOf(false) }
    var showLeftPillSettings by remember { mutableStateOf(false) }
    var showRightPillSettings by remember { mutableStateOf(false) }
    var headerAlarmDialogEntityIds by remember { mutableStateOf<List<String>?>(null) }
    var showRoomConfig by remember { mutableStateOf(false) }
    var showPageConfig by remember { mutableStateOf(false) }
    var previewHeaderColor by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showFlows by remember { mutableStateOf(false) }

    val maxPull = 450f
    // Animatable, not a bare Float: the panel has to settle to an anchor when the finger lifts.
    // Assigning the raw value teleported the header — a partial pull resolved to fully open or fully
    // closed in a single frame, and anything past the old 400f threshold simply stayed wherever the
    // finger stopped because nothing ever moved it the rest of the way.
    val pullAnimatable = remember { Animatable(0f) }
    val pullScope = rememberCoroutineScope()
    val pullOffset = pullAnimatable.value
    /** Settles to whichever anchor the gesture is closest to, carrying fling velocity into the
     *  decision so a quick flick opens the menu without travelling the full distance. */
    fun settlePull(velocity: Float) {
        pullScope.launch {
            val flung = abs(velocity) > 900f
            val target = when {
                flung -> if (velocity > 0f) maxPull else 0f
                pullAnimatable.value > maxPull / 2f -> maxPull
                else -> 0f
            }
            pullAnimatable.animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
                initialVelocity = velocity
            )
        }
    }
    /** Closes the menu with the same animation the gesture uses, for the action buttons. */
    fun closePull() {
        pullScope.launch {
            pullAnimatable.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f))
        }
    }
    // Swiping to another page collapses the pull-down: it belongs to the page that was showing
    // when it was opened, and leaving it open outlives that.
    val pageSwiping = LocalPageSwipeInProgress.current
    LaunchedEffect(pageSwiping) {
        if (pageSwiping && pullAnimatable.value > 0f) closePull()
    }
    val pullOffsetDp = (pullOffset / 3f).dp
    val menuVisible = pullOffset > 120f
    val headerColorSource = previewHeaderColor ?: headerColor ?: pageConfig.headerColor
    val headerColorValue = parseHexColor(headerColorSource)
    val effectiveBackground = if (!headerColorSource.isNullOrBlank()) null else backgroundImage ?: pageConfig.wallpaper
    val isDarkAppearance = appColors.background.luminance() < 0.5f
    // Recreate the exact primary-container/header tint Theme.kt would generate if this custom
    // color had been selected globally.
    val customHeaderStart = headerColorValue?.copy(
        alpha = if (isDarkAppearance) 0.45f else if (headerColorValue.luminance() < 0.35f) 0.28f else 0.18f
    )
    // Contrast the header text against what it actually renders on. With a custom color, read that
    // color; with a wallpaper image, white over the scrim. With neither, the header fades into the
    // page (there is no opaque fallback panel behind the text), so use the adaptive page text color —
    // exactly like the pull-down menu below. Using the fallback tint here made a light-mode header
    // pick white text on the light page.
    val headerBackdrop = customHeaderStart?.compositeOver(appColors.background)
    val headerTextColor = when {
        effectiveBackground != null -> Color.White
        headerBackdrop != null -> if (headerBackdrop.luminance() < 0.5f) Color.White else Color(0xFF1C1B1F)
        else -> appColors.onSurface
    }
    val headerMutedColor = when {
        effectiveBackground != null -> Color.White.copy(alpha = 0.8f)
        headerBackdrop != null -> headerTextColor.copy(alpha = 0.75f)
        else -> appColors.onMuted
    }
    // Use the same light/dark translucent surface family as room counters. A dedicated gradient is
    // used below because the general surfaceGradient intentionally converts colors to opaque.
    val pillColor = appColors.elevated.copy(alpha = 0.90f)
    val pillContentColor = appColors.onSurface
    // The saved false value now means compact rather than absent, preserving existing preferences
    // while guaranteeing every page keeps a pull-down surface for Search, Flows, Edit, and Settings.
    val headerHeight = if (headerVisible) 236.dp else 76.dp
    val density = LocalDensity.current
    var scrollingBadgeHeightPx by remember { mutableIntStateOf(0) }
    var hiddenBadgeHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(headerVisible, showBadgeBar) {
        if (headerVisible || !showBadgeBar) hiddenBadgeHeightPx = 0f
    }
    val badgeScrollConnection = remember(headerVisible, showBadgeBar, scrollingBadgeHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (headerVisible || !showBadgeBar || scrollingBadgeHeightPx <= 0 || available.y >= 0f) return Offset.Zero
                val consumed = (-available.y).coerceAtMost(scrollingBadgeHeightPx - hiddenBadgeHeightPx)
                hiddenBadgeHeightPx += consumed
                return Offset(0f, -consumed)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (headerVisible || !showBadgeBar || available.y <= 0f || hiddenBadgeHeightPx <= 0f) return Offset.Zero
                val revealed = available.y.coerceAtMost(hiddenBadgeHeightPx)
                hiddenBadgeHeightPx -= revealed
                return Offset(0f, revealed)
            }
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val useDarkStatusBarIcons = when {
                effectiveBackground != null -> false
                else -> appColors.background.luminance() > 0.5f
            }
            // The status bar stays transparent under edge-to-edge (the page background/hero image
            // draws behind it); only the icon tint is adjusted for contrast. The deprecated
            // statusBarColor / isStatusBarContrastEnforced setters are no-ops on Android 15+.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkStatusBarIcons
        }
    }
    val visiblePeople = remember(people, pageConfig) {
        val sorted = when (pageConfig.peopleSort) {
            "custom" -> people.sortedWith(
                compareBy<HAEntity> {
                    val index = pageConfig.customPeopleOrder.indexOf(it.entity_id)
                    if (index == -1) Int.MAX_VALUE else index
                }.thenBy { it.friendlyName ?: it.entity_id }
            )
            "name" -> people.sortedBy { it.friendlyName ?: it.entity_id }
            "name_desc" -> people.sortedByDescending { it.friendlyName ?: it.entity_id }
            else -> people.sortedWith(
                compareBy<HAEntity> { if (it.state == "home") 0 else 1 }
                    .thenByDescending { it.last_changed.orEmpty() }
            )
        }
        if (pageConfig.showPeople) sorted.filterNot { it.entity_id in pageConfig.hiddenPeople } else emptyList()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(appColors.background)
        .pointerInput(Unit) {
            // Tracks velocity so a flick settles the way the finger was going, and stops any
            // in-flight settle on touch-down so the panel can be caught and re-dragged mid-animation.
            val velocityTracker = VelocityTracker()
            detectVerticalDragGestures(
                onDragStart = {
                    velocityTracker.resetTracking()
                    pullScope.launch { pullAnimatable.stop() }
                },
                onDragEnd = { settlePull(velocityTracker.calculateVelocity().y) },
                onDragCancel = { settlePull(0f) },
                onVerticalDrag = { change, dragAmount ->
                    val isHeaderGesture = change.position.y < 260.dp.toPx()
                    val current = pullAnimatable.value
                    val isPullingMenu = current > 0f || dragAmount > 0f
                    if ((isHeaderGesture || current > 0f) && isPullingMenu) {
                        change.consume()
                        velocityTracker.addPointerInputChange(change)
                        // Rubber-band past the anchor instead of stopping dead, so overshoot reads
                        // as resistance rather than a broken gesture.
                        val next = current + dragAmount
                        val damped = if (next > maxPull) maxPull + (next - maxPull) * 0.25f else next
                        pullScope.launch {
                            pullAnimatable.snapTo(damped.coerceIn(0f, maxPull * 1.15f))
                        }
                    }
                }
            )
        }
    ) {
        if (effectiveBackground != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight + pullOffsetDp)
                    .align(Alignment.TopCenter)
                    .clipToBounds()
            ) {
                AsyncImage(
                    model = if (effectiveBackground.startsWith("http")) effectiveBackground else "$currentUrl$effectiveBackground",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.42f),
                                    0.5f to Color.Black.copy(alpha = 0.19f),
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // The pull-down controls share the header artwork while it expands. Images have a dark scrim;
        // a custom color can be light or dark. With no custom color/image the controls are revealed
        // over the page surface (there is no opaque fallback panel behind them), so contrast against
        // that surface — the fallback tint made light-mode labels come out white on a light page.
        val menuBackdropColor = customHeaderStart?.compositeOver(appColors.background) ?: appColors.elevated
        val menuUsesLightContent = effectiveBackground != null || menuBackdropColor.luminance() < 0.5f
        val menuButtonSurfaceColor = if (menuUsesLightContent) {
            Color.Black.copy(alpha = 0.46f)
        } else {
            Color.White.copy(alpha = 0.72f)
        }
        val menuButtonContentColor = if (menuUsesLightContent) Color.White else Color(0xFF1C1B1F)
        // The menu is revealed over header artwork as often as over a flat surface, so the edge
        // chevrons fade to a scrim rather than a solid colour — a solid one would smear over an image.
        val menuEdgeFadeColor = if (menuUsesLightContent) {
            Color.Black.copy(alpha = 0.55f)
        } else {
            Color.White.copy(alpha = 0.70f)
        }
        // Admin-set per-user permissions (Settings › Family Sharing). Defaults allow everything, so
        // these only restrict when an admin has locked something down for this user.
        val allowEdit by prefs.enforcedAllowEdit.collectAsState(initial = true)
        val showGlobalSearchAllowed by prefs.enforcedShowGlobalSearch.collectAsState(initial = true)
        val showFlowsAllowed by prefs.enforcedShowFlows.collectAsState(initial = true)
        val openNotificationsPanel = LocalOpenNotifications.current
        val headerMenuActions = buildList {
            // The notification panel used to be reachable only by a left-edge swipe, which meant the
            // gesture-exclusion strip that made it work had to be large enough to find. With a button
            // here that strip could shrink back out of the system back gesture's way.
            add(HeaderMenuAction(Icons.Default.Notifications, stringResource(R.string.ui_notifications_753a22b)) {
                closePull()
                openNotificationsPanel?.invoke()
            })
            if (showGlobalSearchAllowed) {
                add(HeaderMenuAction(Icons.Default.Search, stringResource(R.string.ui_search_bce0641)) {
                    showSearch = true
                    closePull()
                })
            }
            if (showFlowsAllowed) {
                add(HeaderMenuAction(Icons.Default.AccountTree, stringResource(R.string.ui_flows_1242655)) {
                    showFlows = true
                    closePull()
                })
            }
            if (allowEdit) {
                add(HeaderMenuAction(
                    if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Edit,
                    if (isEditMode) stringResource(R.string.ui_done_e9b450d)
                    else stringResource(R.string.ui_edit_5301648)
                ) {
                    viewModel.toggleEditMode()
                    closePull()
                })
            }
            if (pageKey != null && pageSettingsTitle != null) {
                add(HeaderMenuAction(Icons.Default.Tune, pageSettingsTitle) {
                    showPageConfig = true
                    closePull()
                })
            }
            if (title != null && areaId != null) {
                add(HeaderMenuAction(Icons.Default.Tune, stringResource(R.string.room_config_title)) {
                    showRoomConfig = true
                    closePull()
                })
            }
            add(HeaderMenuAction(Icons.Default.Settings, stringResource(R.string.ui_settings_c7f73bb)) {
                showSettings = true
                closePull()
            })
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
                .graphicsLayer {
                    alpha = ((pullOffset - 90f) / 180f).coerceIn(0f, 1f)
                }
        ) {
            val fitsWithoutScrolling = maxWidth >= 64.dp * headerMenuActions.size
            val menuScrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = if (fitsWithoutScrolling) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.fillMaxWidth().horizontalScroll(menuScrollState).padding(horizontal = 12.dp)
                    },
                    horizontalArrangement = if (fitsWithoutScrolling) Arrangement.SpaceEvenly else Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerMenuActions.forEach { action ->
                        MenuButton(
                            icon = action.icon,
                            label = action.label,
                            enabled = menuVisible,
                            surfaceColor = menuButtonSurfaceColor,
                            contentColor = menuButtonContentColor,
                            onClick = action.onClick
                        )
                    }
                }
                if (!fitsWithoutScrolling) {
                    // Same affordance the bottom bar uses when it overflows. matchParentSize takes
                    // the row's measured height without influencing it, so the chevrons track the
                    // buttons rather than the 144dp pull-down area they sit in.
                    Box(modifier = Modifier.matchParentSize()) {
                        ScrollEdgeChevron(
                            visible = menuScrollState.canScrollBackward,
                            fadeColor = menuEdgeFadeColor,
                            contentColor = menuButtonContentColor,
                            fromStart = true,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        ScrollEdgeChevron(
                            visible = menuScrollState.canScrollForward,
                            fadeColor = menuEdgeFadeColor,
                            contentColor = menuButtonContentColor,
                            fromStart = false,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = pullOffsetDp)
                .nestedScroll(badgeScrollConnection)
        ) {
            // HKI Header
            if (headerVisible) Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
                    .zIndex(1f)
            ) {
                if (effectiveBackground != null) {
                    Spacer(Modifier.fillMaxSize())
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.30f)
                                        customHeaderStart != null -> customHeaderStart
                                        else -> appColors.headerFallbackStart
                                    },
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.135f)
                                        customHeaderStart != null -> customHeaderStart.copy(alpha = customHeaderStart.alpha * 0.45f)
                                        else -> appColors.headerFallbackStart.copy(alpha = appColors.headerFallbackStart.alpha * 0.45f)
                                    },
                                    appColors.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onBack != null) {
                                val backShape = itemCornerShape()
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(backShape)
                                        .background(translucentHeaderControlGradient(pillColor))
                                        .clickable(onClick = onBack),
                                    color = Color.Transparent,
                                    shape = backShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.ui_back_b52b36b),
                                            tint = pillContentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                val leftDisplayType by viewModel.headerLeftDisplayType.collectAsState()
                                val leftAlarmIds by viewModel.headerLeftAlarmEntityIds.collectAsState()
                                val leftAlarmEntities = leftAlarmIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
                                    .ifEmpty { listOfNotNull(allEntities.firstOrNull { it.entity_id.startsWith("alarm_control_panel.") }) }
                                // Show the most urgent alarm on the pill; tapping opens all of them.
                                val leftAlarmEntity = leftAlarmEntities.minByOrNull { alarmDisplayPriority(it.state) }
                                val use24h by viewModel.use24hFormat.collectAsState()
                                val useFullDayName by viewModel.useFullDayName.collectAsState()
                                HeaderStatusPill(
                                    displayType = leftDisplayType,
                                    weather = weather,
                                    alarm = leftAlarmEntity,
                                    use24hFormat = use24h,
                                    useFullDayName = useFullDayName,
                                    isEditMode = isEditMode && !aestheticsOnly,
                                    pillColor = pillColor,
                                    textColor = pillContentColor,
                                    editSurfaceColor = appColors.surface.copy(alpha = 0.7f),
                                    onSettingsClick = { showLeftPillSettings = true },
                                    onClick = {
                                        when (leftDisplayType) {
                                            "Weather", "DateTime" -> showWeatherDialog = true
                                            "Alarm" -> if (leftAlarmEntities.isNotEmpty())
                                                headerAlarmDialogEntityIds = leftAlarmEntities.map { it.entity_id }
                                        }
                                    }
                                )
                            }

                            val weatherDisplayType by viewModel.weatherDisplayType.collectAsState()
                            val rightAlarmIds by viewModel.headerAlarmEntityIds.collectAsState()
                            val rightAlarmEntities = rightAlarmIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
                                .ifEmpty { listOfNotNull(allEntities.firstOrNull { it.entity_id.startsWith("alarm_control_panel.") }) }
                            val rightAlarmEntity = rightAlarmEntities.minByOrNull { alarmDisplayPriority(it.state) }
                            val rightUse24h by viewModel.use24hFormat.collectAsState()
                            val rightUseFullDayName by viewModel.useFullDayName.collectAsState()
                            HeaderStatusPill(
                                displayType = weatherDisplayType,
                                weather = weather,
                                alarm = rightAlarmEntity,
                                use24hFormat = rightUse24h,
                                useFullDayName = rightUseFullDayName,
                                isEditMode = isEditMode && !aestheticsOnly,
                                pillColor = pillColor,
                                textColor = pillContentColor,
                                editSurfaceColor = appColors.surface.copy(alpha = 0.7f),
                                onSettingsClick = { showRightPillSettings = true },
                                onClick = {
                                    when (weatherDisplayType) {
                                        "Weather", "DateTime" -> showWeatherDialog = true
                                        "Alarm" -> if (rightAlarmEntities.isNotEmpty()) {
                                            headerAlarmDialogEntityIds =
                                                rightAlarmEntities.map { it.entity_id }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val showPeopleRow = showPeople && visiblePeople.isNotEmpty()
                            val avatarSize = 44.dp
                            val avatarOverlap = 8.dp
                            val avatarStep = avatarSize - avatarOverlap
                            fun avatarRowWidth(count: Int) = if (count <= 0) 0.dp else avatarSize + avatarStep * (count - 1)
                            fun rowCapacity(availableWidth: androidx.compose.ui.unit.Dp): Int {
                                if (!showPeopleRow || availableWidth <= 0.dp) return 0
                                var count = 0
                                for (candidate in 1..visiblePeople.size) {
                                    if (avatarRowWidth(candidate) <= availableWidth) count = candidate
                                }
                                return count
                            }
                            val rawInlineCapacity = rowCapacity(maxWidth - 228.dp)
                            // Never degrade to a one-avatar-wide vertical stack: with 2+ people always
                            // lay them out at least two across. A compact grid beside the greeting reads
                            // far better than a tall column of single faces.
                            val perRow = when {
                                !showPeopleRow -> 0
                                visiblePeople.size == 1 -> 1
                                maxWidth >= 280.dp -> rawInlineCapacity.coerceAtLeast(2)
                                else -> rawInlineCapacity
                            }
                            // Cap at two rows; anything beyond collapses into a "+N" bubble so a family
                            // of 8-10 can't turn the header into a wall of faces.
                            val maxAvatarRows = 2
                            val avatarCapacity = (perRow * maxAvatarRows).coerceAtLeast(1)
                            val overflowCount = (visiblePeople.size - avatarCapacity).coerceAtLeast(0)
                            val shownPeople =
                                if (overflowCount > 0) visiblePeople.take(avatarCapacity - 1) else visiblePeople
                            val avatarRows = if (perRow > 0) shownPeople.chunked(perRow) else emptyList()

                            // The "+N" bubble used to be inert, which made the hidden people simply
                            // unreachable on a narrow header. Tapping it lists everyone, and picking
                            // someone opens the same detail dialog their avatar would have.
                            var showAllPeople by remember { mutableStateOf(false) }
                            if (showAllPeople) {
                                AllPeopleDialog(
                                    people = visiblePeople,
                                    currentUrl = currentUrl,
                                    onDismiss = { showAllPeople = false },
                                    onPersonClick = { person ->
                                        showAllPeople = false
                                        onPeopleClick?.invoke(person)
                                    }
                                )
                            }
                            val wrappedPeopleCapacity = rowCapacity(maxWidth).coerceAtLeast(1)
                            // Follow the person layout's responsive principle: reduce the trailing
                            // counter columns as the header narrows. Badges wrap before they can take
                            // enough horizontal space to force the page title onto two lines.
                            val trailingContentWidth = when {
                                maxWidth >= 400.dp -> 180.dp // 3 x 53dp pills + two 6dp gaps
                                maxWidth >= 280.dp -> 112.dp // 2 x 53dp pills + one 6dp gap
                                else -> 53.dp
                            }

                            if (perRow == 0) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = if (headerTrailingContent != null) 12.dp else 0.dp)
                                        ) {
                                            Text(
                                                text = title ?: greeting,
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = headerTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 40.sp,
                                                lineHeight = 44.sp
                                            )
                                            HeaderSubtitle(
                                                text = subtitle ?: if (title == null) {
                                                    displayName
                                                } else if (status == ConnectionStatus.ERROR) {
                                                    stringResource(R.string.connection_error_title)
                                                } else {
                                                    stringResource(R.string.connection_all_systems_normal)
                                                },
                                                icon = subtitleIcon,
                                                color = headerMutedColor
                                            )
                                            if (headerBottomContent != null) {
                                                Spacer(Modifier.height(6.dp))
                                                headerBottomContent(headerMutedColor)
                                            }
                                            if (title == null && showNotificationStatus) {
                                                Spacer(Modifier.height(8.dp))
                                                HeaderNotificationSummary(unreadNotificationCount, headerMutedColor)
                                            }
                                        }
                                        if (headerTrailingContent != null) {
                                            Box(
                                                modifier = Modifier
                                                    .width(trailingContentWidth)
                                                    .padding(top = 4.dp),
                                                contentAlignment = Alignment.TopEnd
                                            ) {
                                                headerTrailingContent(headerTextColor)
                                            }
                                        }
                                    }
                                    if (showPeopleRow) {
                                        Spacer(Modifier.height(8.dp))
                                        visiblePeople.chunked(wrappedPeopleCapacity).forEach { rowPeople ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy((-8).dp, Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                rowPeople.forEach { person ->
                                                    PersonAvatar(
                                                        person = person,
                                                        currentUrl = currentUrl,
                                                        isEditMode = isEditMode,
                                                        headerTextColor = headerTextColor,
                                                        onClick = { onPeopleClick?.invoke(person) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = if (showPeopleRow) 12.dp else 0.dp)
                                    ) {
                                        Text(
                                            text = title ?: greeting,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = headerTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 40.sp,
                                            lineHeight = 44.sp
                                        )
                                        HeaderSubtitle(
                                            text = subtitle ?: if (title == null) {
                                                displayName
                                            } else if (status == ConnectionStatus.ERROR) {
                                                stringResource(R.string.connection_error_title)
                                            } else {
                                                stringResource(R.string.connection_all_systems_normal)
                                            },
                                            icon = subtitleIcon,
                                            color = headerMutedColor
                                        )
                                        if (headerBottomContent != null) {
                                            Spacer(Modifier.height(6.dp))
                                            headerBottomContent(headerMutedColor)
                                        }
                                        if (title == null && showNotificationStatus) {
                                            Spacer(Modifier.height(8.dp))
                                            HeaderNotificationSummary(unreadNotificationCount, headerMutedColor)
                                        }
                                    }

                                    if (showPeopleRow) {
                                        Column(
                                            modifier = Modifier.width(avatarRowWidth(perRow)),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            avatarRows.forEachIndexed { rowIndex, rowPeople ->
                                                if (rowIndex > 0) Spacer(Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .then(if (rowIndex == 0) Modifier.padding(top = 2.dp) else Modifier),
                                                    horizontalArrangement = Arrangement.spacedBy((-8).dp, Alignment.End),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    rowPeople.forEach { person ->
                                                        PersonAvatar(
                                                            person = person,
                                                            currentUrl = currentUrl,
                                                            isEditMode = isEditMode,
                                                            headerTextColor = headerTextColor,
                                                            onClick = { onPeopleClick?.invoke(person) }
                                                        )
                                                    }
                                                    if (overflowCount > 0 && rowIndex == avatarRows.lastIndex) {
                                                        PersonOverflowAvatar(overflowCount, headerTextColor) {
                                                            showAllPeople = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.30f)
                                        customHeaderStart != null -> customHeaderStart
                                        else -> appColors.headerFallbackStart
                                    },
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.18f)
                                        customHeaderStart != null -> customHeaderStart.copy(alpha = customHeaderStart.alpha * 0.62f)
                                        else -> appColors.headerFallbackStart.copy(alpha = appColors.headerFallbackStart.alpha * 0.62f)
                                    },
                                    appColors.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBack != null) {
                            val backShape = itemCornerShape()
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(backShape)
                                    .background(translucentHeaderControlGradient(pillColor))
                                    .clickable(onClick = onBack),
                                color = Color.Transparent,
                                shape = backShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.ui_back_b52b36b),
                                        tint = pillContentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = title ?: greeting,
                            modifier = Modifier.weight(1f),
                            color = headerTextColor,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val compactRightDisplayType by viewModel.weatherDisplayType.collectAsState()
                        val compactRightAlarmIds by viewModel.headerAlarmEntityIds.collectAsState()
                        val compactRightAlarmEntities = compactRightAlarmIds
                            .mapNotNull { id -> allEntities.find { it.entity_id == id } }
                            .ifEmpty { listOfNotNull(allEntities.firstOrNull { it.entity_id.startsWith("alarm_control_panel.") }) }
                        val compactRightAlarm = compactRightAlarmEntities.minByOrNull { alarmDisplayPriority(it.state) }
                        val compactUse24h by viewModel.use24hFormat.collectAsState()
                        val compactUseFullDayName by viewModel.useFullDayName.collectAsState()
                        HeaderStatusPill(
                            displayType = compactRightDisplayType,
                            weather = weather,
                            alarm = compactRightAlarm,
                            use24hFormat = compactUse24h,
                            useFullDayName = compactUseFullDayName,
                            isEditMode = isEditMode && !aestheticsOnly,
                            pillColor = pillColor,
                            textColor = pillContentColor,
                            editSurfaceColor = appColors.surface.copy(alpha = 0.7f),
                            onSettingsClick = { showRightPillSettings = true },
                            onClick = {
                                when (compactRightDisplayType) {
                                    "Weather", "DateTime" -> showWeatherDialog = true
                                    "Alarm" -> if (compactRightAlarmEntities.isNotEmpty()) {
                                        headerAlarmDialogEntityIds = compactRightAlarmEntities.map { it.entity_id }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (showBadgeBar) {
                val badgeContent: @Composable () -> Unit = {
                    HKIBadgeBar(
                        badgeBarConfig = badgeBarConfig,
                            isEditMode = isEditMode && !aestheticsOnly,
                        viewModel = viewModel,
                        navController = navController,
                        onConfigChange = { newBarConfig ->
                            if (areaId != null) {
                                viewModel.updateAreaConfig(
                                    areaId,
                                    (areaConfig ?: HKIAreaConfig()).copy(badgeBar = newBarConfig)
                                )
                            } else if (pageKey != null) {
                                viewModel.updatePageConfig(pageKey, pageConfig.copy(badgeBar = newBarConfig))
                            }
                        }
                    )
                }
                if (headerVisible) {
                    badgeContent()
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val visibleBadgeHeight = (scrollingBadgeHeightPx - hiddenBadgeHeightPx)
                            .coerceAtLeast(0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (scrollingBadgeHeightPx > 0) {
                                        Modifier.height(with(density) { visibleBadgeHeight.toDp() })
                                    } else Modifier
                                )
                                .clipToBounds()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (scrollingBadgeHeightPx > 0) {
                                            Modifier.requiredHeight(with(density) { scrollingBadgeHeightPx.toDp() })
                                        } else Modifier
                                    )
                                    .graphicsLayer { translationY = -hiddenBadgeHeightPx }
                                    .onSizeChanged { size ->
                                        if (size.height > scrollingBadgeHeightPx) scrollingBadgeHeightPx = size.height
                                    }
                            ) { badgeContent() }
                        }
                    }
                }
            }

            headerBar?.invoke()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(PaddingValues())
            }
        }

        if (headerVisible && showNotificationStatus && unreadNotificationCount > 0 && onBack == null) {
            val openNotifications = LocalOpenNotifications.current
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = headerHeight / 2 - 32.dp)
                    .width(8.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { openNotifications?.invoke() }
                    .zIndex(4f)
            )
        }
        
        // Done Editing FAB removed to avoid overlap with widget selector

        if (showWeatherDialog && weather != null) {
            HKIWeatherDialog(
                weather = weather!!, 
                onDismiss = { showWeatherDialog = false },
                viewModel = viewModel
            )
        }

        if (!aestheticsOnly && showLeftPillSettings && weather != null) {
            val leftDisplayType by viewModel.headerLeftDisplayType.collectAsState()
            val leftAlarmEntityIds by viewModel.headerLeftAlarmEntityIds.collectAsState()
            HKIWeatherDialog(
                weather = weather!!,
                onDismiss = { showLeftPillSettings = false },
                viewModel = viewModel,
                settingsTitle = stringResource(R.string.header_pill_left),
                displayType = leftDisplayType,
                alarmEntityIds = leftAlarmEntityIds,
                onDisplayTypeSelected = { viewModel.setHeaderLeftDisplayType(it) },
                onAlarmEntitiesSelected = { viewModel.setHeaderLeftAlarmEntities(it) }
            )
        }

        if (!aestheticsOnly && showRightPillSettings && weather != null) {
            val rightDisplayType by viewModel.weatherDisplayType.collectAsState()
            val rightAlarmEntityIds by viewModel.headerAlarmEntityIds.collectAsState()
            HKIWeatherDialog(
                weather = weather!!,
                onDismiss = { showRightPillSettings = false },
                viewModel = viewModel,
                settingsTitle = stringResource(R.string.header_pill_right),
                displayType = rightDisplayType,
                alarmEntityIds = rightAlarmEntityIds,
                onDisplayTypeSelected = { viewModel.setWeatherDisplayType(it) },
                onAlarmEntitiesSelected = { viewModel.setHeaderAlarmEntities(it) }
            )
        }

        headerAlarmDialogEntityIds?.let { ids ->
            val alarmEntities = ids.mapNotNull { id -> allEntities.find { it.entity_id == id } }
            if (alarmEntities.isNotEmpty()) {
                HKIAlarmDialog(
                    entity = alarmEntities.first(),
                    entities = alarmEntities,
                    viewModel = viewModel,
                    onDismiss = { headerAlarmDialogEntityIds = null }
                )
            }
        }
        
        if (showSettings) {
            SettingsDialog(
                prefs = prefs,
                viewModel = viewModel,
                onDismiss = { showSettings = false }
            )
        }

        if (showSearch) {
            GlobalSearchDialog(viewModel = viewModel, onDismiss = { showSearch = false })
        }

        if (showFlows) {
            FlowsDialog(viewModel = viewModel, onDismiss = { showFlows = false })
        }

        if (showRoomConfig && areaId != null) {
            RoomConfigDialog(
                areaId = areaId,
                viewModel = viewModel,
                onHeaderColorPreview = { previewHeaderColor = it },
                onBadgeBarPreview = { previewBadgeBarConfig = it },
                onDismiss = {
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showRoomConfig = false
                }
            )
        }
        if (showPageConfig && pageKey != null) {
            PageSettingsDialog(
                title = pageSettingsTitle ?: stringResource(R.string.page_settings_default_title),
                config = pageConfig,
                people = people,
                showPeopleSettings = showPeople,
                showBadgeBarSettings = showBadgeBar,
                customPage = customPage,
                onCustomPageSave = onCustomPageSave,
                extraSections = listOfNotNull(extraPageSettingsSection) + additionalPageSettingsSections,
                aestheticsOnly = aestheticsOnly,
                onHeaderColorPreview = { previewHeaderColor = it },
                onBadgeBarPreview = { previewBadgeBarConfig = it },
                onDismiss = {
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showPageConfig = false
                },
                onSave = { config ->
                    viewModel.updatePageConfig(pageKey, config)
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showPageConfig = false
                }
            )
        }
    }
}

/** "+N" bubble closing the avatar grid when more people exist than the two-row cap allows. */
@Composable
private fun PersonOverflowAvatar(count: Int, headerTextColor: Color, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onClick() },
        shape = CircleShape,
        border = BorderStroke(1.dp, headerTextColor.copy(alpha = 0.7f)),
        color = appColors.elevated
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.ui_text_f168b71, count),
                style = MaterialTheme.typography.labelMedium,
                color = appColors.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Everyone in the header, including the faces the "+N" bubble stands in for. */
@Composable
private fun AllPeopleDialog(
    people: List<HAEntity>,
    currentUrl: String,
    onDismiss: () -> Unit,
    onPersonClick: (HAEntity) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val scroll = rememberScrollState()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: java.util.Locale.getDefault()
    ModernAlertDialog(
        onDismissRequest = onDismiss,
        dismissOnTapOutside = true,
        title = { Text(stringResource(R.string.ui_people_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                people.forEach { person ->
                    val imageUrl = person.entityPicture?.let {
                        if (it.startsWith("http") || it.startsWith("content:") || it.startsWith("file:")) it
                        else "$currentUrl$it"
                    }
                    val isHome = person.state == "home"
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(itemCornerShape()).clickable { onPersonClick(person) },
                        shape = itemCornerShape(),
                        color = appColors.subtleSurface
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = appColors.elevated) {
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = person.friendlyName,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = if (!isHome) grayscaleFilter else null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = appColors.onMuted)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                person.friendlyName ?: person.entity_id,
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                person.state.replaceFirstChar { it.titlecase(locale) },
                                color = if (isHome) MaterialTheme.colorScheme.primary else appColors.onMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PersonAvatar(
    person: HAEntity,
    currentUrl: String,
    isEditMode: Boolean,
    headerTextColor: Color,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val imageUrl = person.entityPicture?.let {
        if (it.startsWith("http") || it.startsWith("content:") || it.startsWith("file:")) it else "$currentUrl$it"
    }
    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable { if (!isEditMode) onClick() },
            shape = CircleShape,
            border = BorderStroke(1.dp, headerTextColor.copy(alpha = 0.7f)),
            color = appColors.elevated
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = person.friendlyName,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (person.state != "home") grayscaleFilter else null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = appColors.onMuted)
            }
        }

        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onClick() },
                color = appColors.surface.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EditSettingsButton(onClick = onClick)
                }
            }
        }
    }
}

@Composable
fun PageSettingsDialog(
    title: String,
    config: HKIPageConfig,
    people: List<HAEntity>,
    showPeopleSettings: Boolean,
    showBadgeBarSettings: Boolean = true,
    customPage: HKICustomPage? = null,
    onCustomPageSave: (HKICustomPage) -> Unit = {},
    extraSections: List<Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit>> = emptyList(),
    aestheticsOnly: Boolean = false,
    onHeaderColorPreview: (String?) -> Unit = {},
    onBadgeBarPreview: (HKIBadgeBarConfig?) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (HKIPageConfig) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var wallpaper by remember(config) { mutableStateOf(config.wallpaper ?: "") }
    var headerColorText by remember(config) { mutableStateOf(config.headerColor ?: "") }
    var headerRgb by remember(config) { mutableStateOf(hexToRgb(config.headerColor) ?: listOf(155, 83, 83)) }
    var customPageName by remember(customPage) { mutableStateOf(customPage?.name.orEmpty()) }
    var customPageSubtitle by remember(customPage) { mutableStateOf(customPage?.subtitle.orEmpty()) }
    var customPageIcon by remember(customPage) { mutableStateOf(customPage?.icon ?: "view-dashboard") }
    var showCustomPageIconPicker by remember { mutableStateOf(false) }
    var showPeople by remember(config) { mutableStateOf(config.showPeople) }
    var peopleSort by remember(config) { mutableStateOf(config.peopleSort) }
    var hiddenPeople by remember(config) { mutableStateOf(config.hiddenPeople) }
    var badgeBarEnabled by remember(config) { mutableStateOf(config.badgeBar?.visible ?: true) }
    var badgeAlignment by remember(config) { mutableStateOf(config.badgeBar?.alignment ?: "split") }
    var badgeSpanIcons by remember(config) { mutableStateOf(config.badgeBar?.spanIcons ?: false) }
    var badgeLeftOverflow by remember(config) { mutableStateOf(config.badgeBar?.leftOverflow ?: false) }
    var badgeRightOverflow by remember(config) { mutableStateOf(config.badgeBar?.rightOverflow ?: false) }
    var section by remember { mutableStateOf("menu") }
    // Where the section list was left, so returning from a sub-page doesn't snap back to the top.
    var menuScrollOffset by remember { mutableIntStateOf(0) }
    var extraSectionInnerBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    var customOrder by remember(config, people) {
        mutableStateOf(
            (config.customPeopleOrder + people.map { it.entity_id })
                .distinct()
                .filter { id -> people.any { it.entity_id == id } }
        )
    }

    fun navigateBack() {
        val innerBack = extraSectionInnerBack
        if (section.startsWith("extra:") && innerBack != null) innerBack()
        else if (section != "menu") {
            section = "menu"
            extraSectionInnerBack = null
        } else onDismiss()
    }
    if (showCustomPageIconPicker) {
        MdiIconPickerDialog(
            current = customPageIcon,
            onDismiss = { showCustomPageIconPicker = false },
            onSelect = { icon ->
                customPageIcon = icon.ifBlank { "view-dashboard" }
                showCustomPageIconPicker = false
            }
        )
    }
    val extraIndex = section.removePrefix("extra:").toIntOrNull()
    val currentTitle = when (section) {
        "menu" -> title
        "page" -> stringResource(R.string.ui_page_settings_0a44afc)
        "header" -> stringResource(R.string.ui_header_31341c6)
        "badgebar" -> stringResource(R.string.page_settings_badge_bar)
        "persons" -> stringResource(R.string.page_settings_persons)
        else -> extraIndex?.let { extraSections.getOrNull(it)?.first } ?: section.replaceFirstChar { it.uppercase() }
    }
    ModernSettingsDialogFrame(
        title = currentTitle,
        subtitle = if (section == "menu") {
            stringResource(R.string.page_settings_choose_area)
        } else {
            stringResource(R.string.page_settings_focused_options)
        },
        onDismiss = onDismiss,
        onBack = if (section == "menu") null else ::navigateBack,
        content = {
            val settingsScrollState = rememberScrollState()
            // One scroll state serves every section here, so it needs the same save/restore the
            // main settings dialog does — otherwise a deep scroll in one sub-page carries straight
            // into the next one. This hierarchy is only two levels: back always means "menu".
            LaunchedEffect(section) {
                val target = if (section == "menu") menuScrollOffset else 0
                if (target > 0) {
                    withTimeoutOrNull(1.seconds) {
                        snapshotFlow { settingsScrollState.maxValue }.first { it > 0 }
                    }
                    settingsScrollState.scrollTo(target.coerceAtMost(settingsScrollState.maxValue))
                } else {
                    settingsScrollState.scrollTo(0)
                }
                if (section == "menu") {
                    snapshotFlow { settingsScrollState.value }.collect { menuScrollOffset = it }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().fadingEdges(settingsScrollState).verticalScroll(settingsScrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (section == "menu") {
                    if (aestheticsOnly) {
                        SettingsGroup {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.family_aesthetics_only_explanation), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    SettingsSubcategory(stringResource(R.string.ui_page_areas_1068b64), stringResource(R.string.ui_each_group_controls_one_part_of_this_page_286a40b))
                    SettingsMenuChoice(
                        Icons.Default.Image,
                        stringResource(R.string.ui_header_31341c6),
                        stringResource(R.string.page_settings_header_description)
                    ) { section = "header" }
                    if (customPage != null) {
                        SettingsMenuChoice(
                            Icons.Default.DashboardCustomize,
                            stringResource(R.string.page_settings_default_title),
                            stringResource(R.string.page_settings_custom_page_description)
                        ) { section = "page" }
                    }
                    if (showBadgeBarSettings && !aestheticsOnly) {
                        SettingsMenuChoice(
                            Icons.Default.ViewStream,
                            stringResource(R.string.page_settings_badge_bar),
                            stringResource(R.string.page_settings_badge_description)
                        ) { section = "badgebar" }
                    }
                    if (showPeopleSettings && !aestheticsOnly) {
                        SettingsMenuChoice(
                            Icons.Default.Person,
                            stringResource(R.string.page_settings_persons),
                            stringResource(R.string.page_settings_persons_description)
                        ) { section = "persons" }
                    }
                    extraSections.forEachIndexed { index, extra ->
                        SettingsMenuChoice(
                            Icons.Default.Tune,
                            extra.first,
                            stringResource(R.string.page_settings_configure)
                        ) { section = "extra:$index" }
                    }
                }
                if (section == "page" && customPage != null) {
                    SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_name_subtitle_and_navigation_icon_3f2e8c4))
                    OutlinedTextField(
                        value = customPageName,
                        onValueChange = { customPageName = it },
                        label = { Text(stringResource(R.string.ui_page_name_c99f51a)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customPageSubtitle,
                        onValueChange = { customPageSubtitle = it },
                        label = { Text(stringResource(R.string.ui_page_subtitle_8a04084)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showCustomPageIconPicker = true },
                        shape = itemCornerShape(),
                        color = appColors.subtleSurface
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            MdiIcon(customPageIcon, contentDescription = null, tint = appColors.onSurface, size = 24.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.ui_page_icon_b58a288), color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                                Text(customPageIcon, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted)
                        }
                    }
                }
                if (section == "header") {
                    SettingsSubcategory(stringResource(R.string.ui_header_appearance_40327c9), stringResource(R.string.ui_wallpaper_and_an_optional_custom_color_f4f12f1))
                    OutlinedTextField(
                        value = wallpaper,
                        onValueChange = { wallpaper = it },
                        label = { Text(stringResource(R.string.ui_header_wallpaper_url_or_path_25cf2b6)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerColorText,
                        onValueChange = {
                            headerColorText = it
                            onHeaderColorPreview(it.ifBlank { null })
                        },
                        label = { Text(stringResource(R.string.ui_header_custom_color_rrggbb_1887d0a)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorWheel(
                        selectedRgb = headerRgb,
                        onColorSelected = { rgb ->
                            headerRgb = rgb
                            headerColorText = rgbToHex(rgb)
                            onHeaderColorPreview(headerColorText)
                        },
                        onValueChangeFinished = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(220.dp)
                    )
                }
                if (section == "persons" && showPeopleSettings && !aestheticsOnly) {
                    SettingsSubcategory(stringResource(R.string.ui_people_b37554f), stringResource(R.string.ui_visibility_and_ordering_in_the_page_header_0f9e3d8))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPeople, onCheckedChange = { showPeople = it })
                        Text(stringResource(R.string.ui_show_persons_c4e81a0))
                    }
                    Text(stringResource(R.string.ui_persons_order_9fe91dc), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = peopleSort == "custom",
                            onClick = { peopleSort = "custom" },
                            label = { Text(stringResource(R.string.ui_custom_081ae3f)) }
                        )
                    }
                    if (peopleSort == "custom") {
                        ReorderableGrid(
                            items = customOrder,
                            canReorder = true,
                            onReorder = { from, to ->
                                customOrder = customOrder.toMutableList().apply {
                                    add(to, removeAt(from))
                                }
                            },
                            key = { it },
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            isNested = true
                        ) { personId, _ ->
                            people.find { it.entity_id == personId }?.let { person ->
                                Surface(
                                    shape = itemCornerShape(),
                                    color = appColors.subtleSurface
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = appColors.onSurface.copy(alpha = 0.75f))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(person.friendlyName ?: person.entity_id, color = appColors.onSurface)
                                            Text(person.state, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Icon(Icons.Default.DragIndicator, null, tint = appColors.onMuted)
                                    }
                                }
                            }
                        }
                    }
                    if (people.isNotEmpty()) {
                        Text(stringResource(R.string.ui_visible_persons_4b7ae6c), style = MaterialTheme.typography.labelLarge)
                        people.forEach { person ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = person.entity_id !in hiddenPeople,
                                    onCheckedChange = { checked ->
                                        hiddenPeople = if (checked) hiddenPeople - person.entity_id else (hiddenPeople + person.entity_id).distinct()
                                    }
                                )
                                Text(person.friendlyName ?: person.entity_id)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = peopleSort == "changed",
                            onClick = { peopleSort = "changed" },
                            label = { Text(stringResource(R.string.ui_state_a725020)) }
                        )
                        FilterChip(
                            selected = peopleSort == "name",
                            onClick = { peopleSort = "name" },
                            label = { Text(stringResource(R.string.ui_name_709a232)) }
                        )
                        FilterChip(
                            selected = peopleSort == "name_desc",
                            onClick = { peopleSort = "name_desc" },
                            label = { Text(stringResource(R.string.ui_reverse_57f9933)) }
                        )
                    }
                }
                if (section.startsWith("extra:")) {
                    section.removePrefix("extra:").toIntOrNull()?.let { index ->
                        extraSections.getOrNull(index)?.second?.invoke(this) { extraSectionInnerBack = it }
                    }
                }
                if (section == "badgebar" && showBadgeBarSettings && !aestheticsOnly) {
                    SettingsSubcategory(stringResource(R.string.ui_badge_bar_layout_4b4dfdd), stringResource(R.string.ui_visibility_alignment_and_overflow_behavior_467f44f))
                    if (!aestheticsOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.ui_show_badge_bar_827499b), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = badgeBarEnabled,
                            onCheckedChange = {
                                badgeBarEnabled = it
                                onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = it, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                            }
                        )
                    }
                    }
                    Text(stringResource(R.string.ui_alignment_7f8c517), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "split" to stringResource(R.string.alignment_split),
                            "left" to stringResource(R.string.ui_left_8ae1c34),
                            "center" to stringResource(R.string.alignment_center),
                            "right" to stringResource(R.string.ui_right_954daa8)
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = badgeAlignment == value,
                                onClick = {
                                    badgeAlignment = value
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = value, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (badgeAlignment == "center") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.ui_span_badges_58c73f2), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeSpanIcons,
                                onCheckedChange = {
                                    badgeSpanIcons = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = it, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                                }
                            )
                        }
                    }
                    if (badgeAlignment == "split") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.ui_left_side_overflows_right_fc8533c), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeLeftOverflow,
                                onCheckedChange = {
                                    badgeLeftOverflow = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = it, rightOverflow = badgeRightOverflow))
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.ui_right_side_overflows_left_d75893d), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeRightOverflow,
                                onCheckedChange = {
                                    badgeRightOverflow = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = it))
                                }
                            )
                        }
                    }
                }
            }
        },
        footer = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
            Button(onClick = {
                customPage?.let {
                    onCustomPageSave(
                        it.copy(
                            name = customPageName.trim(),
                            subtitle = customPageSubtitle.trim(),
                            icon = customPageIcon
                        )
                    )
                }
                onSave(
                    config.copy(
                        wallpaper = wallpaper.ifBlank { null },
                        headerColor = headerColorText.ifBlank { null },
                        showPeople = if (aestheticsOnly) config.showPeople else showPeople,
                        peopleSort = if (aestheticsOnly) config.peopleSort else peopleSort,
                        customPeopleOrder = if (aestheticsOnly) config.customPeopleOrder else customOrder,
                        hiddenPeople = if (aestheticsOnly) config.hiddenPeople else hiddenPeople,
                        badgeBar = if (aestheticsOnly) {
                            config.badgeBar
                        } else if (showBadgeBarSettings) {
                            (config.badgeBar ?: HKIBadgeBarConfig()).copy(
                                visible = badgeBarEnabled,
                                alignment = badgeAlignment,
                                spanIcons = badgeSpanIcons,
                                leftOverflow = badgeLeftOverflow,
                                rightOverflow = badgeRightOverflow
                            )
                        } else config.badgeBar
                    )
                )
            }, enabled = customPage == null || customPageName.isNotBlank()) { Text(stringResource(R.string.ui_save_efc007a)) }
        }
    )
}

@Composable
private fun SettingsMenuChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ModernSettingsMenuItem(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
}

private data class HeaderMenuAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

private fun translucentHeaderControlGradient(base: Color): Brush = Brush.verticalGradient(
    listOf(
        base.copy(alpha = (base.alpha + 0.04f).coerceAtMost(1f)),
        base,
        base.copy(alpha = (base.alpha - 0.06f).coerceAtLeast(0f))
    )
)

@Composable
fun MenuButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    surfaceColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val resolvedSurface = surfaceColor ?: appColors.subtleSurface
    val resolvedContent = contentColor ?: appColors.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).clickable(enabled = enabled) { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = resolvedSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = resolvedContent)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = resolvedContent,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeaderStatusPill(
    displayType: String,
    weather: HAEntity?,
    alarm: HAEntity?,
    use24hFormat: Boolean,
    useFullDayName: Boolean,
    isEditMode: Boolean,
    pillColor: Color,
    textColor: Color,
    editSurfaceColor: Color,
    onSettingsClick: () -> Unit,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val showPill = displayType != "None"
    Box(
        modifier = if (!showPill && isEditMode) Modifier.size(36.dp) else Modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showPill) {
            val now = LocalDateTime.now()
            val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: java.util.Locale.getDefault()
            val timeSkeleton = if (use24hFormat) "Hm" else "hma"
            val daySkeleton = if (useFullDayName) "EEEE" else "EEE"
            fun pattern(skeleton: String) =
                android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            val displayText = when (displayType) {
                "Date" -> now.format(DateTimeFormatter.ofPattern(pattern("${daySkeleton}MMMd"), locale))
                "Time" -> now.format(DateTimeFormatter.ofPattern(pattern(timeSkeleton), locale))
                "DateTime" -> now.format(
                    DateTimeFormatter.ofPattern(pattern("${daySkeleton}MMMd$timeSkeleton"), locale)
                )
                "Alarm" -> alarm?.localizedStateLabel() ?: stringResource(R.string.ui_alarm_25f8c55)
                else -> stringResource(
                    R.string.ui_c_286a95c,
                    weather?.state?.let { localizedWeatherStateLabel(it) }
                        ?: stringResource(R.string.weather_cloudy),
                    weather?.temperature?.toInt() ?: 12
                )
            }
            val pillShape = itemCornerShape()
            val resolvedPillBackground = pillColor.compositeOver(appColors.background)
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(pillShape)
                    .background(translucentHeaderControlGradient(pillColor))
                    .clickable { if (!isEditMode) onClick() },
                color = Color.Transparent,
                shape = pillShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayType == "Alarm") {
                        val alarmIconColor = semanticColorForBackground(
                            alarmStateColor(alarm?.state.orEmpty()),
                            resolvedPillBackground,
                        )
                        MdiIcon(
                            name = alarm?.let { defaultEntityIconSlug(it) } ?: "shield-home",
                            contentDescription = null,
                            tint = alarmIconColor,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        if (displayType == "Weather" || displayType == "DateTime") {
                            WeatherStateIcon(
                                state = weather?.state,
                                size = 20.dp,
                                surface = WeatherAnimationSurface.PILL,
                                fallbackTint = semanticColorForBackground(
                                    weatherStateColor(weather?.state),
                                    resolvedPillBackground,
                                ),
                                contentDescription = weather?.state?.let {
                                    localizedWeatherStateLabel(it)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(displayText, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (isEditMode) {
            val overlayModifier = if (showPill) Modifier.matchParentSize() else Modifier.fillMaxSize()
            val pillShape = itemCornerShape()
            Surface(
                modifier = overlayModifier
                    .clip(pillShape)
                    .clickable { onSettingsClick() },
                color = editSurfaceColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EditSettingsButton(onClick = onSettingsClick)
                }
            }
        }
    }
}

@Composable
private fun HeaderSubtitle(text: String, icon: ImageVector?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun HeaderNotificationSummary(count: Int, color: Color) {
    if (count == 0) {
        Text(stringResource(R.string.ui_no_notifications_a72159d), color = color, style = MaterialTheme.typography.labelMedium)
        return
    }
    val openNotifications = LocalOpenNotifications.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .height(22.dp)
                .widthIn(min = 22.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clip(CircleShape)
                .clickable { openNotifications?.invoke() }
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                count.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(if (count == 1) stringResource(R.string.ui_notification_c18f8f2) else stringResource(R.string.ui_notifications_753a22b), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

private fun parseHexColor(value: String?): Color? {
    val hex = value?.takeIf { it.isNotBlank() } ?: return null
    val normalized = if (hex.startsWith("#")) hex else "#$hex"
    return runCatching { Color(normalized.toColorInt()) }.getOrNull()
}

private fun hexToRgb(value: String?): List<Int>? {
    val hex = value?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
    return runCatching {
        listOf(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16))
    }.getOrNull()
}

private fun rgbToHex(rgb: List<Int>): String {
    val safe = List(3) { index -> rgb.getOrNull(index)?.coerceIn(0, 255) ?: 0 }
    return "#%02X%02X%02X".format(safe[0], safe[1], safe[2])
}

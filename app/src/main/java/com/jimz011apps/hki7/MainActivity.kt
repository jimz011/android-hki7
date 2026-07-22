@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jimz011apps.hki7.data.HomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.HomeAssistantInstance
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.PushForegroundService
import com.jimz011apps.hki7.data.EXTRA_HA_INSTANCE_ID
import com.jimz011apps.hki7.data.isDemoServerUrl
import com.jimz011apps.hki7.data.withDisplayName
import com.jimz011apps.hki7.ui.ConnectionStatus
import com.jimz011apps.hki7.ui.connectionIssueGraceMillis
import com.jimz011apps.hki7.ui.HomeAssistantRestartPhase
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.NavBarConfig
import com.jimz011apps.hki7.ui.homeAssistantConnectionStatusLabel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.jimz011apps.hki7.ui.Screen
import com.jimz011apps.hki7.ui.components.HKIBottomBar
import com.jimz011apps.hki7.ui.components.awaitHorizontalTabSwipes
import com.jimz011apps.hki7.ui.components.HKIMediaPlayerDialog
import com.jimz011apps.hki7.ui.components.MediaPlayerMiniBar
import com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset
import com.jimz011apps.hki7.ui.components.LocalItemCornerRadius
import com.jimz011apps.hki7.ui.components.LocalOpenNotifications
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.components.NotificationPanel
import com.jimz011apps.hki7.ui.components.NotificationBannerHost
import com.jimz011apps.hki7.ui.components.QuickStartGuideDialog
import com.jimz011apps.hki7.ui.components.CameraFullscreenHost
import com.jimz011apps.hki7.ui.components.CameraFullscreenRequest
import com.jimz011apps.hki7.ui.components.LocalCameraFullscreenLauncher
import com.jimz011apps.hki7.ui.screens.*
import com.jimz011apps.hki7.ui.theme.HKI7Theme
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    private var forceHighRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyPreferredRefreshRate()
        val prefs = PreferencesManager(this)
        lifecycleScope.launch {
            prefs.ensureHomeAssistantInstanceStore()
            intent?.getStringExtra(EXTRA_HA_INSTANCE_ID)?.let { prefs.switchHomeAssistantInstance(it) }
            if (prefs.shouldUsePushService.first()) PushForegroundService.start(applicationContext)
        }
        
        setContent {
            val forceHighRefresh by prefs.forceHighRefreshRate.collectAsState(initial = false)
            LaunchedEffect(forceHighRefresh) { setForceHighRefresh(forceHighRefresh) }
            val themeColor by prefs.themeColor.collectAsState(initial = "system")
            val themeMode by prefs.themeMode.collectAsState(initial = "system")
            val systemLightThemeColor by prefs.systemLightThemeColor.collectAsState(initial = "auto")
            val systemDarkThemeColor by prefs.systemDarkThemeColor.collectAsState(initial = "auto")
            val fontScale by prefs.fontScale.collectAsState(initial = 1f)
            val fontWeightAdjust by prefs.fontWeightAdjust.collectAsState(initial = 0)
            val fontFamily by prefs.fontFamily.collectAsState(initial = "default")
            val itemCornerRadius by prefs.itemCornerRadius.collectAsState(initial = 20)
            HKI7Theme(
                themeColor = themeColor,
                themeMode = themeMode,
                systemLightThemeColor = systemLightThemeColor,
                systemDarkThemeColor = systemDarkThemeColor,
                fontScale = fontScale,
                fontWeightAdjust = fontWeightAdjust,
                fontFamily = fontFamily,
                itemCornerRadius = itemCornerRadius
            ) {
                CompositionLocalProvider(LocalItemCornerRadius provides itemCornerRadius) {
                val appColors = LocalHKIAppColors.current
                val loading = "__hki_loading__"
                val serverUrl by prefs.serverUrl.collectAsState(initial = loading)
                val internalUrl by prefs.internalUrl.collectAsState(initial = loading)
                val accessToken by prefs.accessToken.collectAsState(initial = loading)
                val refreshToken by prefs.refreshToken.collectAsState(initial = loading)
                val instances by prefs.homeAssistantInstances.collectAsState(initial = emptyList())
                val activeInstanceId by prefs.activeHomeAssistantInstanceId.collectAsState(initial = null)
                var forceLogin by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Create viewModel early to observe forced logout
                val viewModel: MainViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return MainViewModel(prefs, applicationContext) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                    }
                })

                val forcedLogoutReason by viewModel.forcedLogoutReason.collectAsState()
                LaunchedEffect(forcedLogoutReason) {
                    val reason = forcedLogoutReason
                    if (reason != null) {
                        viewModel.clearForcedLogoutReason()
                        val fallback = instances.firstOrNull {
                            it.id != activeInstanceId && it.isAuthenticated
                        }
                        if (fallback != null) {
                            viewModel.switchHomeAssistantInstance(fallback.id)
                        } else {
                            forceLogin = true
                        }
                        // A blank reason is a user-initiated re-login; no need to explain it.
                        if (reason.isNotBlank()) {
                            snackbarHostState.showSnackbar(message = reason, duration = SnackbarDuration.Long)
                        }
                    }
                }

                val isLoading = serverUrl == loading || internalUrl == loading || accessToken == loading || refreshToken == loading
                val hasConnectionUrl = !serverUrl.isNullOrBlank() || !internalUrl.isNullOrBlank()
                val loggedIn = hasConnectionUrl && (!accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank())
                // Latch onboarding on once we know the user needs to log in, and keep it on through the
                // login + permission steps (saving the token mid-flow would otherwise jump to the app).
                var onboardingActive by remember { mutableStateOf(false) }
                var onboardingStartsAtLogin by remember { mutableStateOf(false) }
                LaunchedEffect(isLoading, loggedIn, forceLogin, instances, activeInstanceId) {
                    val fallback = instances.firstOrNull {
                        it.id != activeInstanceId && it.isAuthenticated
                    }
                    if (!isLoading && !loggedIn && !forceLogin && fallback != null) {
                        viewModel.switchHomeAssistantInstance(fallback.id)
                        return@LaunchedEffect
                    }
                    if (!isLoading && (forceLogin || !loggedIn) && !onboardingActive) {
                        // Decide this once, before OAuth writes the server/token preferences. If it
                        // were recomputed after the token save, a first-time flow would suddenly be
                        // mistaken for re-login and jump back to the login WebView.
                        onboardingStartsAtLogin = forceLogin || hasConnectionUrl
                        onboardingActive = true
                    }
                }
                when {
                    isLoading && !onboardingActive -> {
                        Box(
                            modifier = Modifier.fillMaxSize().background(appColors.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    onboardingActive -> {
                        // A saved server URL means this is a re-login (forced, or a "keep config"
                        // logout), so jump straight to the login step instead of full onboarding.
                        Box {
                            OnboardingScreen(prefs = prefs, startAtLogin = onboardingStartsAtLogin, onComplete = {
                                forceLogin = false
                                onboardingActive = false
                                viewModel.completeInitialDashboardSetup()
                            })
                            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    }
                    else -> {
                        MainApp(prefs, viewModel)
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-assert the preference in case the system reset it while backgrounded.
        applyPreferredRefreshRate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_HA_INSTANCE_ID)?.let { instanceId ->
            lifecycleScope.launch { PreferencesManager(this@MainActivity).switchHomeAssistantInstance(instanceId) }
        }
    }

    fun setForceHighRefresh(force: Boolean) {
        forceHighRefresh = force
        applyPreferredRefreshRate()
    }

    /** Lift scrolling out of the 60Hz throttle. When [forceHighRefresh] is on, hard-lock the panel's
     *  highest mode (overrides the system peak-rate setting); otherwise use a soft hint that the
     *  system clamps to the user's chosen refresh-rate setting. */
    private fun applyPreferredRefreshRate() {
        val activeDisplay = display ?: return
        val current = activeDisplay.mode
        val best = activeDisplay.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            if (forceHighRefresh) {
                preferredDisplayModeId = best.modeId
                preferredRefreshRate = best.refreshRate
            } else {
                preferredDisplayModeId = 0
                preferredRefreshRate = if (best.refreshRate > current.refreshRate + 1f) best.refreshRate else 0f
            }
        }
    }
}

@Composable
fun MainApp(prefs: PreferencesManager, sharedViewModel: MainViewModel? = null) {
    val navController = rememberNavController()
    val appColors = LocalHKIAppColors.current
    val appCtx = LocalContext.current.applicationContext
    val quickStartGuidePending by prefs.quickStartGuidePending.collectAsState(initial = false)
    val autoGenerationPending by prefs.pendingAutoTakeover.collectAsState(initial = false)
    val headerVisible by prefs.headerVisible.collectAsState(initial = true)
    val homeAssistantInstances by prefs.homeAssistantInstances.collectAsState(initial = emptyList())
    val activeHomeAssistantInstanceId by prefs.activeHomeAssistantInstanceId.collectAsState(initial = null)
    val quickStartScope = rememberCoroutineScope()
    var instancePanelOpen by remember { mutableStateOf(false) }
    var showAddHomeAssistantInstance by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = sharedViewModel ?: viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(prefs, appCtx) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    })
    
    val connectionStatus by viewModel.status.collectAsState()
    val homeAssistantRestartPhase by viewModel.homeAssistantRestartPhase.collectAsState()
    var hasConnectedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(connectionStatus) { if (connectionStatus == ConnectionStatus.CONNECTED) hasConnectedOnce = true }
    var showConnectionIssueUi by remember { mutableStateOf(false) }
    LaunchedEffect(connectionStatus, hasConnectedOnce, homeAssistantRestartPhase, autoGenerationPending) {
        when {
            homeAssistantRestartPhase != HomeAssistantRestartPhase.NONE ->
                showConnectionIssueUi = true
            connectionStatus == ConnectionStatus.CONNECTED -> showConnectionIssueUi = false
            else -> {
                // Keep the cached dashboard steady through brief websocket resubscriptions. Auto
                // generation gets extra time because its registry work can briefly delay reconnects.
                showConnectionIssueUi = false
                val graceMillis = connectionIssueGraceMillis(autoGenerationPending, hasConnectedOnce)
                if (graceMillis > 0L) delay(graceMillis.milliseconds)
                showConnectionIssueUi = true
            }
        }
    }
    var switchedConnectionRoute by remember { mutableStateOf<HomeAssistantConnectionRoute?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.connectionRouteSwitches.collectLatest { route ->
            switchedConnectionRoute = route
            delay(4.seconds)
            if (switchedConnectionRoute == route) switchedConnectionRoute = null
        }
    }
    // Mini media player: any playing/paused media_player shows a swipeable bar above the nav bar.
    // Custom names and per-player visibility come from Settings → Appearance → Media Players.
    val currentUrl by viewModel.currentUrl.collectAsState()
    val mediaPlayerNames by prefs.mediaPlayerCustomNames.collectAsState(initial = emptyMap())
    val mediaBarHidden by prefs.mediaPlayerBarHidden.collectAsState(initial = emptyList())
    val mediaPlayers by remember(viewModel) {
        viewModel.entitiesMatching("domain:media_player") { it.entity_id.startsWith("media_player.") }
    }.collectAsState()
    val activeMediaPlayers = remember(mediaPlayers, mediaPlayerNames, mediaBarHidden) {
        mediaPlayers.filter { it.state == "playing" || it.state == "paused" }
            .filter { it.entity_id !in mediaBarHidden }
            .map { it.withDisplayName(mediaPlayerNames[it.entity_id]) }
            .sortedBy { it.entity_id }
    }
    val showConnectionBar = homeAssistantRestartPhase != HomeAssistantRestartPhase.NONE ||
        ((hasConnectedOnce || autoGenerationPending) &&
            connectionStatus != ConnectionStatus.CONNECTED &&
            (autoGenerationPending || showConnectionIssueUi))
    var mediaDialogEntityId by remember { mutableStateOf<String?>(null) }
    // Transient: swipe the media bar down to tuck it away; swipe up from the nav bar to bring it back.
    var mediaBarDismissed by remember { mutableStateOf(false) }
    var mediaBarRevealGeneration by remember { mutableIntStateOf(0) }
    var fullscreenCamera by remember { mutableStateOf<CameraFullscreenRequest?>(null) }
    val launchFullscreenCamera = remember {
        { request: CameraFullscreenRequest -> fullscreenCamera = request }
    }

    if (quickStartGuidePending) {
        QuickStartGuideDialog(
            onComplete = { quickStartScope.launch { prefs.acknowledgeQuickStartGuide() } }
        )
    }
    val mediaBarPlaybackKey = activeMediaPlayers.joinToString(separator = "|") { player ->
        val mediaMetadata = player.attributes?.entries
            ?.asSequence()
            ?.filter { (key, _) ->
                (key.startsWith("media_") || key == "entity_picture") &&
                    key != "media_position" && key != "media_position_updated_at"
            }
            ?.sortedBy { it.key }
            ?.joinToString(separator = ",") { (key, value) -> "$key=$value" }
            .orEmpty()
        listOf(
            player.entity_id,
            player.state,
            mediaMetadata
        ).joinToString(separator = ":")
    }
    LaunchedEffect(mediaBarPlaybackKey, mediaBarRevealGeneration) {
        if (mediaBarPlaybackKey.isNotEmpty()) {
            mediaBarDismissed = false
            delay(10.seconds)
            mediaBarDismissed = true
        } else {
            mediaBarDismissed = false
        }
    }

    val navBarOrder by prefs.navBarOrder.collectAsState(initial = emptyList())
    val navBarHidden by prefs.navBarHidden.collectAsState(initial = emptyList())
    val customPages by prefs.customPages.collectAsState(initial = emptyList())
    val screens = remember(navBarOrder, navBarHidden, customPages) {
        NavBarConfig.visibleTabs(navBarOrder, navBarHidden, customPages)
    }
    val isEditMode by viewModel.isEditMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentAreaId = navBackStackEntry?.arguments?.getString("areaId")
    var swipeNavigationDirection by remember { mutableIntStateOf(0) }
    LaunchedEffect(navBackStackEntry) {
        if (swipeNavigationDirection != 0) {
            delay(280.milliseconds)
            swipeNavigationDirection = 0
        }
    }
    val currentTopLevelIndex = screens.indexOfFirst { screen ->
        if (screen is Screen.Custom) {
            currentRoute == Screen.CUSTOM_PAGE_ROUTE &&
                navBackStackEntry?.arguments?.getString("pageId") == screen.page.id
        } else {
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }
    }
    val navigateToTopLevel: (Screen) -> Unit = { screen ->
        when (screen) {
            Screen.Climate, Screen.Energy -> {
                // These screens keep detail pages as local UI state, so entering a tab recreates its root.
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                    launchSingleTop = false
                    restoreState = false
                }
            }
            Screen.Home, Screen.Rooms, Screen.Security -> {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            else -> {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    // Reads the *current* route/areas/screens at fire time, so the gesture detector below can key
    // only on edit mode and never restarts (or misses) mid-navigation.
    val handlePageSwipe by rememberUpdatedState<(Boolean) -> Unit> { forward ->
        when (currentRoute) {
            Screen.RoomDetail.route -> {
                if (areas.size > 1) {
                    val currentIndex = areas.indexOfFirst { it.area_id == currentAreaId }
                    if (currentIndex >= 0) {
                        val step = if (forward) 1 else -1
                        val targetIndex = (currentIndex + step + areas.size) % areas.size
                        val currentDestinationId = navController.currentDestination?.id
                        swipeNavigationDirection = if (forward) 1 else -1
                        navController.navigate(Screen.RoomDetail.createRoute(areas[targetIndex].area_id)) {
                            currentDestinationId?.let { destinationId ->
                                popUpTo(destinationId) { inclusive = true }
                            }
                        }
                    }
                }
            }
            else -> {
                if (currentTopLevelIndex >= 0) {
                    val targetIndex = currentTopLevelIndex + if (forward) 1 else -1
                    screens.getOrNull(targetIndex)?.let { target ->
                        swipeNavigationDirection = if (forward) 1 else -1
                        navigateToTopLevel(target)
                    }
                }
            }
        }
    }
    val context = LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.startLocationReporting(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setAppVisible(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppVisible(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        // Demo sessions have no server to report to and must not open with surprise permission
        // prompts (Google Play reviewers see the demo first).
        val needsRealLocation = prefs.homeAssistantInstances.first().any { instance ->
            instance.locationEnabled && !isDemoServerUrl(instance.primaryUrl)
        }
        if (!needsRealLocation) return@LaunchedEffect
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasLocation = permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasLocation) viewModel.startLocationReporting(context) else permissionLauncher.launch(permissions)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // No battery-optimization exemption prompt: presence is event-driven (geofences) and the
        // periodic refresh runs under WorkManager, both of which work while the app is Dozed. Letting
        // the OS power-manage us normally is the point — it's how the official app sips battery.
    }

    // Left-edge swipe opens the notification panel (history of notify.mobile_app_* messages).
    // The drawer's own gestures stay off while closed so the panel is completely hidden; opening
    // is driven by our edge detector below, which works alongside the system back gesture: the
    // upper-left edge strip is excluded from the back gesture (Android honors up to 200dp of
    // exclusion per edge), so a swipe starting there opens the panel instead of navigating back.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }
    androidx.activity.compose.BackHandler(enabled = instancePanelOpen) {
        instancePanelOpen = false
    }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    // Height the three-button navigation bar occupies (0 under gesture navigation): pages add it to
    // their bottom scroll reserve so content still clears the floating bar once that bar moves up.
    val systemButtonBarInset = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()
    val edgeStripWidthPx = with(density) { 28.dp.toPx() }
    val edgeStripTopPx = with(density) { 56.dp.toPx() }
    val edgeStripHeightPx = with(density) { 200.dp.toPx() }
    val instanceStripHeightPx = with(density) { (if (headerVisible) 200.dp else 72.dp).toPx() }
    val screenWidthPx = windowInfo.containerSize.width.toFloat()
    CompositionLocalProvider(
        LocalOpenNotifications provides { drawerScope.launch { drawerState.open() } },
        LocalCameraFullscreenLauncher provides launchFullscreenCamera
    ) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen && !isEditMode,
        drawerContent = {
            // Keep the default sheet width: this Material3 version slides the closed drawer out
            // by a fixed 360dp, so a wider sheet would leave its right edge permanently visible
            // on screen (and swallow touches on the left edge).
            ModalDrawerSheet(
                drawerContainerColor = appColors.background,
                drawerContentColor = appColors.onSurface
            ) {
                NotificationPanel(viewModel)
            }
        }
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Match the page background so the mini-player inset never shows as a gray strip.
            .background(appColors.background)
            .systemGestureExclusion {
                // Carve the upper-left edge out of the system back gesture (Pixel-style gesture
                // nav) so swipes starting there reach the app and open the notification panel.
                Rect(0f, edgeStripTopPx, edgeStripWidthPx, edgeStripTopPx + edgeStripHeightPx)
            }
            .systemGestureExclusion {
                // Mirror the notification gesture on the right side for the instance switcher.
                Rect(screenWidthPx - edgeStripWidthPx, edgeStripTopPx, screenWidthPx, edgeStripTopPx + instanceStripHeightPx)
            }
            .pointerInput(isEditMode) {
                if (isEditMode) return@pointerInput
                // Runs on the content root AFTER children (Final pass) and never consumes pointer
                // movement itself, so taps, scrolling, and sliders are unaffected. Axis locking,
                // mid-gesture commits, and tracked fling velocity live in awaitHorizontalTabSwipes.
                val drawerThresholdPx = 24.dp.toPx()
                var opensDrawer = false
                var drawerOpened = false
                var opensInstances = false
                var instancesOpened = false
                awaitHorizontalTabSwipes(
                    respectChildGestures = true,
                    pass = PointerEventPass.Final,
                    commitDistancePx = 48.dp.toPx(),
                    flingDistancePx = 18.dp.toPx(),
                    flingVelocityPxPerSecond = 550.dp.toPx(),
                    onDown = { position ->
                        opensDrawer = position.x <= edgeStripWidthPx && position.y < size.height * 0.55f
                        drawerOpened = false
                        opensInstances = position.x >= size.width - edgeStripWidthPx &&
                            position.y in edgeStripTopPx..(edgeStripTopPx + instanceStripHeightPx)
                        instancesOpened = false
                    },
                    onMove = { totalX, totalY ->
                        if (opensDrawer && !drawerOpened && totalX >= drawerThresholdPx && abs(totalX) > abs(totalY)) {
                            drawerOpened = true
                            drawerScope.launch { drawerState.open() }
                        }
                        if (opensInstances && !instancesOpened && totalX <= -drawerThresholdPx && abs(totalX) > abs(totalY)) {
                            instancesOpened = true
                            instancePanelOpen = true
                        }
                        opensDrawer || opensInstances
                    },
                    onSwipe = { forward -> handlePageSwipe(forward) }
                )
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { contentPadding ->
            CompositionLocalProvider(
                LocalMediaPlayerBarInset provides systemButtonBarInset + if (!isEditMode) {
                    (if (activeMediaPlayers.isNotEmpty() && !mediaBarDismissed) 86.dp else 0.dp) + (if (showConnectionBar) 62.dp else 0.dp)
                } else 0.dp
            ) {
                NavHost(
                    navController,
                    startDestination = Screen.Home.route,
                    // Pages add this overlay height to their scroll content, not their background.
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    enterTransition = {
                        when (swipeNavigationDirection) {
                            1 -> slideInHorizontally(tween(230)) { it } + fadeIn(tween(150))
                            -1 -> slideInHorizontally(tween(230)) { -it } + fadeIn(tween(150))
                            else -> EnterTransition.None
                        }
                    },
                    exitTransition = {
                        when (swipeNavigationDirection) {
                            1 -> slideOutHorizontally(tween(230)) { -it } + fadeOut(tween(150))
                            -1 -> slideOutHorizontally(tween(230)) { it } + fadeOut(tween(150))
                            else -> ExitTransition.None
                        }
                    },
                    popEnterTransition = {
                        when (swipeNavigationDirection) {
                            1 -> slideInHorizontally(tween(230)) { it } + fadeIn(tween(150))
                            -1 -> slideInHorizontally(tween(230)) { -it } + fadeIn(tween(150))
                            else -> EnterTransition.None
                        }
                    },
                    popExitTransition = {
                        when (swipeNavigationDirection) {
                            1 -> slideOutHorizontally(tween(230)) { -it } + fadeOut(tween(150))
                            -1 -> slideOutHorizontally(tween(230)) { it } + fadeOut(tween(150))
                            else -> ExitTransition.None
                        }
                    }
                ) {
                composable(Screen.Home.route)     { HAHomeScreen(viewModel, navController) }
                composable(Screen.Rooms.route)    { RoomsScreen(viewModel, navController) }
                composable(Screen.Security.route) { SecurityScreen(viewModel) }
                composable(Screen.Energy.route)   { EnergyScreen(viewModel) }
                composable(Screen.Climate.route)  { ClimateScreen(viewModel) }
                composable(Screen.Battery.route)  { BatteryScreen(viewModel, navController, showBackButton = false) }
                composable(Screen.Battery.WIDGET_ROUTE) { BatteryScreen(viewModel, navController, showBackButton = true) }
                composable(
                    route = Screen.CUSTOM_PAGE_ROUTE,
                    arguments = listOf(navArgument("pageId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pageId = backStackEntry.arguments?.getString("pageId").orEmpty()
                    val customPage = customPages.firstOrNull { it.id == pageId }
                    HAHomeScreen(
                        viewModel,
                        navController,
                        widgetAreaId = "__custom_page_${pageId}__",
                        customPage = customPage
                    )
                }
                composable(
                    route = Screen.RoomDetail.route,
                    arguments = listOf(navArgument("areaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
                    RoomDetailScreen(areaId, viewModel, navController)
                }
                }
            }
        }

        NotificationBannerHost(viewModel, Modifier.align(Alignment.TopCenter))

        // Opaque strip behind three-button navigation, painted over the page but under the floating
        // bar, so scrolling content no longer shows through the system buttons. Collapses to nothing
        // under gesture navigation, where the inset is 0 and content is meant to run to the edge.
        if (systemButtonBarInset > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(systemButtonBarInset)
                    .background(appColors.background)
            )
        }

        // When the bar is too narrow for every tab (small screens / many tabs), fall back to
        // fixed-width tabs in a horizontally scrollable row instead of squeezing weight()-tabs.
        val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
        val navBarScrollable = !isEditMode && (screenWidth - 64.dp) < 64.dp * screens.size
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Lift the whole bottom stack clear of three-button navigation, tucking it 8dp back
                // down so it sits close to the system buttons rather than floating well above them
                // (the bar carries its own 15dp gap). Zero under gesture navigation, where
                // tappableElement reports no inset and the bar keeps its usual position.
                .padding(bottom = (systemButtonBarInset - 8.dp).coerceAtLeast(0.dp))
        ) {
        if (!isEditMode) {
            when {
                showConnectionBar -> HomeAssistantConnectionBar(
                    status = connectionStatus,
                    restartPhase = homeAssistantRestartPhase,
                    isAutoGenerating = autoGenerationPending,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                )
                switchedConnectionRoute != null -> HomeAssistantConnectionSwitchBar(
                    switchedConnectionRoute!!,
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = activeMediaPlayers.isNotEmpty() && !isEditMode && !mediaBarDismissed,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            MediaPlayerMiniBar(
                players = activeMediaPlayers,
                currentUrl = currentUrl,
                viewModel = viewModel,
                onOpen = { mediaDialogEntityId = it.entity_id },
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                    // Swipe the bar down to dismiss it (vertical only; the pager still takes horizontal swipes).
                    .pointerInput(Unit) {
                        var drag = 0f
                        detectVerticalDragGestures(
                            onDragEnd = { drag = 0f },
                            onDragCancel = { drag = 0f },
                            onVerticalDrag = { _, amount ->
                                drag += amount
                                if (drag > 40.dp.toPx()) {
                                    mediaBarDismissed = true
                                    drag = 0f
                                }
                            }
                        )
                    }
            )
        }
        val canRestoreMediaBar = activeMediaPlayers.isNotEmpty() && !isEditMode && mediaBarDismissed
        Box(contentAlignment = Alignment.TopCenter) {
        HKIBottomBar(
            horizontalPadding = 32.dp,
            scrollable = navBarScrollable,
            // While the media bar is tucked away, a swipe up on the nav bar brings it back.
            modifier = if (canRestoreMediaBar) Modifier.pointerInput(Unit) {
                var drag = 0f
                detectVerticalDragGestures(
                    onDragEnd = { drag = 0f },
                    onDragCancel = { drag = 0f },
                    onVerticalDrag = { _, amount ->
                        drag += amount
                        if (drag < -40.dp.toPx()) {
                            mediaBarDismissed = false
                            mediaBarRevealGeneration++
                            drag = 0f
                        }
                    }
                )
            } else Modifier
        ) {
            if (isEditMode) {
                EditNavButton(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo) { viewModel.undo() }
                EditNavButton(Icons.AutoMirrored.Filled.Redo, "Redo", enabled = canRedo) { viewModel.redo() }
                EditNavButton(Icons.Default.CheckCircle, "Done") { viewModel.toggleEditMode() }
            } else {
                screens.forEach { screen ->
                        val isSelected = when (screen) {
                            is Screen.Custom ->
                                currentDestination?.route == Screen.CUSTOM_PAGE_ROUTE &&
                                    navBackStackEntry?.arguments?.getString("pageId") == screen.page.id
                            Screen.Rooms ->
                                currentDestination?.route == Screen.RoomDetail.route ||
                                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            Screen.Battery ->
                                currentDestination?.route == Screen.Battery.WIDGET_ROUTE ||
                                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            else -> currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }

                        Column(
                            modifier = Modifier
                                .then(
                                    // weight() needs a bounded row; scrollable rows use fixed-width tabs.
                                    if (navBarScrollable) Modifier.width(68.dp) else Modifier.weight(1f)
                                )
                                .fillMaxHeight()
                                .clickable { navigateToTopLevel(screen) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 32.dp)
                                    .clip(itemCornerShape())
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted
                                if (screen.mdiIcon != null) {
                                    MdiIcon(
                                        name = screen.mdiIcon,
                                        tint = iconTint,
                                        size = 24.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) appColors.onSurface else appColors.onMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
            }
        }
        // Handlebar affordance: shows when the media bar is tucked away; swipe up here to restore it.
        if (canRestoreMediaBar) {
            Box(
                Modifier
                    .padding(top = 7.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
            )
        }
        }
        }

        mediaDialogEntityId?.let { id ->
            val player = mediaPlayers.find { it.entity_id == id }?.withDisplayName(mediaPlayerNames[id])
            if (player != null) {
                HKIMediaPlayerDialog(player, viewModel, currentUrl) { mediaDialogEntityId = null }
            } else {
                mediaDialogEntityId = null
            }
        }

        // A hard connection failure always needs an actionable screen. Cached entities and a
        // connection that succeeded earlier in this Activity must not hide the failure on a real
        // device, where both are much more likely than on a clean emulator install.
        if (connectionStatus == ConnectionStatus.ERROR &&
            showConnectionIssueUi &&
            homeAssistantRestartPhase == HomeAssistantRestartPhase.NONE
        ) {
            ConnectionErrorOverlay(viewModel)
        }

        InstanceSwitcherPanel(
            visible = instancePanelOpen,
            instances = homeAssistantInstances,
            activeInstanceId = activeHomeAssistantInstanceId,
            onDismiss = { instancePanelOpen = false },
            onSelect = { instanceId ->
                instancePanelOpen = false
                viewModel.switchHomeAssistantInstance(instanceId)
                navigateToTopLevel(Screen.Home)
            },
            onAdd = {
                instancePanelOpen = false
                showAddHomeAssistantInstance = true
            }
        )

        if (showAddHomeAssistantInstance) {
            AddHomeAssistantInstanceDialog(
                prefs = prefs,
                onDismiss = { showAddHomeAssistantInstance = false },
                onAdded = {
                    showAddHomeAssistantInstance = false
                    viewModel.completeInitialDashboardSetup()
                    navigateToTopLevel(Screen.Home)
                }
            )
        }

        CameraFullscreenHost(
            request = fullscreenCamera,
            onDismiss = { fullscreenCamera = null }
        )
    }
    }
    }
}

@Composable
private fun InstanceSwitcherPanel(
    visible: Boolean,
    instances: List<HomeAssistantInstance>,
    activeInstanceId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)) + slideInHorizontally(tween(230)) { it / 3 },
        exit = fadeOut(tween(140)) + slideOutHorizontally(tween(210)) { it / 3 }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onDismiss)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 380.dp)
                    .clickable { },
                color = appColors.background,
                contentColor = appColors.onSurface,
                shadowElevation = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Switch home", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Choose the Home Assistant shown in HKI 7", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    }
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.20f))
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        instances.forEach { instance ->
                            val selected = instance.id == activeInstanceId
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { if (!selected) onSelect(instance.id) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else appColors.elevated,
                                border = if (selected) androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                ) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else appColors.onMuted
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(instance.name, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Text(
                                            instance.primaryUrl.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = appColors.onMuted,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    if (selected) Icon(Icons.Default.CheckCircle, "Active", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Home Assistant")
                    }
                    Text(
                        "Open this panel by swiping left from the upper-right edge of any page header.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.onMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionErrorOverlay(viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize().background(appColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.CloudOff, null, tint = appColors.onMuted, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(18.dp))
            Text("Unable to connect", style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                if (currentUrl.isBlank()) "Couldn't reach your Home Assistant server."
                else "Couldn't reach $currentUrl",
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.onMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = !retrying,
                onClick = {
                    retrying = true
                    scope.launch {
                        viewModel.retryConnection()
                        retrying = false
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp)
            ) {
                if (retrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Connecting…")
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                enabled = !retrying,
                onClick = { viewModel.requestRelogin() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log in again")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Logging in again keeps your dashboard and settings.",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onMuted
            )
        }
    }
}

@Composable
private fun HomeAssistantConnectionBar(
    status: ConnectionStatus,
    restartPhase: HomeAssistantRestartPhase,
    isAutoGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val label = homeAssistantConnectionStatusLabel(status, restartPhase, isAutoGenerating)
    val title = if (isAutoGenerating) "Building your dashboard" else "Home Assistant"
    val statusColor = if (
        isAutoGenerating ||
        restartPhase != HomeAssistantRestartPhase.NONE ||
        status == ConnectionStatus.CONNECTING
    ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = itemCornerShape(),
        color = appColors.surface.copy(alpha = .96f),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Column {
                Text(title, color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                Text(
                    label,
                    color = statusColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeAssistantConnectionSwitchBar(
    route: HomeAssistantConnectionRoute,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = itemCornerShape(),
        color = appColors.surface.copy(alpha = .96f),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF6AC36A),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text("Home Assistant", color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Connection switched to ${route.displayName}",
                    color = Color(0xFF6AC36A),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EditNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val color = if (enabled) appColors.onSurface else appColors.onMuted.copy(alpha = 0.42f)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}

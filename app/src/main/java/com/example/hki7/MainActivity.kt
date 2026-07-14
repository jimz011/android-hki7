package com.example.hki7

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.hki7.data.PreferencesManager
import com.example.hki7.data.withDisplayName
import com.example.hki7.ui.ConnectionStatus
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.NavBarConfig
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.HKIBottomBar
import com.example.hki7.ui.components.HKIMediaPlayerDialog
import com.example.hki7.ui.components.MediaPlayerMiniBar
import com.example.hki7.ui.components.LocalMediaPlayerBarInset
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.components.NotificationPanel
import com.example.hki7.ui.components.NotificationBannerHost
import com.example.hki7.ui.screens.*
import com.example.hki7.ui.theme.HKI7Theme
import com.example.hki7.ui.theme.LocalHKIAppColors

class MainActivity : ComponentActivity() {
    private var forceHighRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyPreferredRefreshRate()
        val prefs = PreferencesManager(this)
        
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
            HKI7Theme(
                themeColor = themeColor,
                themeMode = themeMode,
                systemLightThemeColor = systemLightThemeColor,
                systemDarkThemeColor = systemDarkThemeColor,
                fontScale = fontScale,
                fontWeightAdjust = fontWeightAdjust,
                fontFamily = fontFamily
            ) {
                val appColors = LocalHKIAppColors.current
                val loading = "__hki_loading__"
                val serverUrl by prefs.serverUrl.collectAsState(initial = loading)
                val accessToken by prefs.accessToken.collectAsState(initial = loading)
                val refreshToken by prefs.refreshToken.collectAsState(initial = loading)
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
                        forceLogin = true
                        viewModel.clearForcedLogoutReason()
                        // A blank reason is a user-initiated re-login; no need to explain it.
                        if (reason.isNotBlank()) {
                            snackbarHostState.showSnackbar(message = reason, duration = SnackbarDuration.Long)
                        }
                    }
                }

                val isLoading = serverUrl == loading || accessToken == loading || refreshToken == loading
                val loggedIn = !serverUrl.isNullOrBlank() && (!accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank())
                // Latch onboarding on once we know the user needs to log in, and keep it on through the
                // login + permission steps (saving the token mid-flow would otherwise jump to the app).
                var onboardingActive by remember { mutableStateOf(false) }
                LaunchedEffect(isLoading, loggedIn, forceLogin) {
                    if (!isLoading && (forceLogin || !loggedIn)) onboardingActive = true
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
                        val hasServerConfig = serverUrl != loading && !serverUrl.isNullOrBlank()
                        Box {
                            OnboardingScreen(prefs = prefs, startAtLogin = forceLogin || hasServerConfig, onComplete = {
                                forceLogin = false
                                onboardingActive = false
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

    override fun onResume() {
        super.onResume()
        // Re-assert the preference in case the system reset it while backgrounded.
        applyPreferredRefreshRate()
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
    var hasConnectedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(connectionStatus) { if (connectionStatus == ConnectionStatus.CONNECTED) hasConnectedOnce = true }
    val hasEntities by remember(viewModel) {
        viewModel.entities.map { it.isNotEmpty() }.distinctUntilChanged()
    }.collectAsState(initial = false)

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
    val showConnectionBar = hasConnectedOnce && connectionStatus != ConnectionStatus.CONNECTED
    var mediaDialogEntityId by remember { mutableStateOf<String?>(null) }
    // Transient: swipe the media bar down to tuck it away; swipe up from the nav bar to bring it back.
    var mediaBarDismissed by remember { mutableStateOf(false) }

    val navBarOrder by prefs.navBarOrder.collectAsState(initial = emptyList())
    val navBarHidden by prefs.navBarHidden.collectAsState(initial = emptyList())
    val screens = remember(navBarOrder, navBarHidden) { NavBarConfig.visibleTabs(navBarOrder, navBarHidden) }
        val isEditMode by viewModel.isEditMode.collectAsState()
        val canUndo by viewModel.canUndo.collectAsState()
        val canRedo by viewModel.canRedo.collectAsState()
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
    val density = LocalDensity.current
    val edgeStripWidthPx = with(density) { 28.dp.toPx() }
    val edgeStripTopPx = with(density) { 56.dp.toPx() }
    val edgeStripHeightPx = with(density) { 200.dp.toPx() }
    androidx.compose.runtime.CompositionLocalProvider(
        com.example.hki7.ui.components.LocalOpenNotifications provides { drawerScope.launch { drawerState.open() } }
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
            .pointerInput(isEditMode) {
                if (isEditMode) return@pointerInput
                // Runs on the content root AFTER children, so it only sees horizontal drags no
                // child consumed — taps, scrolling, and sliders are unaffected.
                var armed = false
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        armed = offset.x <= edgeStripWidthPx && offset.y < size.height * 0.55f
                        dragged = 0f
                    },
                    onDragEnd = { armed = false },
                    onDragCancel = { armed = false },
                    onHorizontalDrag = { _, amount ->
                        if (armed) {
                            dragged += amount
                            if (dragged > 24.dp.toPx()) {
                                armed = false
                                drawerScope.launch { drawerState.open() }
                            }
                        }
                    }
                )
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { contentPadding ->
            CompositionLocalProvider(
                LocalMediaPlayerBarInset provides if (!isEditMode) {
                    (if (activeMediaPlayers.isNotEmpty() && !mediaBarDismissed) 86.dp else 0.dp) + (if (showConnectionBar) 62.dp else 0.dp)
                } else 0.dp
            ) {
                NavHost(
                    navController,
                    startDestination = Screen.Home.route,
                    // Pages add this overlay height to their scroll content, not their background.
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                composable(Screen.Home.route)     { HAHomeScreen(viewModel, navController) }
                composable(Screen.Rooms.route)    { RoomsScreen(viewModel, navController) }
                composable(Screen.Security.route) { SecurityScreen(viewModel) }
                composable(Screen.Energy.route)   { EnergyScreen(viewModel) }
                composable(Screen.Climate.route)  { ClimateScreen(viewModel) }
                composable(Screen.Battery.route)  { BatteryScreen(viewModel, navController) }
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

        // When the bar is too narrow for every tab (small screens / many tabs), fall back to
        // fixed-width tabs in a horizontally scrollable row instead of squeezing weight()-tabs.
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val navBarScrollable = !isEditMode && (configuration.screenWidthDp - 64) < screens.size * 64
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        if (showConnectionBar && !isEditMode) {
            HomeAssistantConnectionBar(connectionStatus, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp))
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        Column(
                            modifier = Modifier
                                .then(
                                    // weight() needs a bounded row; scrollable rows use fixed-width tabs.
                                    if (navBarScrollable) Modifier.width(68.dp) else Modifier.weight(1f)
                                )
                                .fillMaxHeight()
                                .clickable {
                                    if (screen == Screen.Home || screen == Screen.Rooms || screen == Screen.Security) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
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

        // Startup connection failure (like the official HA app): full-screen retry. Only shown
        // while nothing has loaded yet — once connected, transient drops keep the dashboard up.
        if (connectionStatus == ConnectionStatus.ERROR && !hasConnectedOnce && !hasEntities) {
            ConnectionErrorOverlay(viewModel)
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
private fun HomeAssistantConnectionBar(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    val label = when (status) {
        ConnectionStatus.CONNECTING -> "Restarting or reconnecting…"
        ConnectionStatus.ERROR -> "Unavailable · Retrying…"
        ConnectionStatus.IDLE -> "Connection paused"
        ConnectionStatus.CONNECTED -> "Connected"
    }
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
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
                Text("Home Assistant", color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                Text(label, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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

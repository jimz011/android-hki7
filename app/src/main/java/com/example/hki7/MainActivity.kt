package com.example.hki7

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.NavBarConfig
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.HKIBottomBar
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.components.NotificationPanel
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
            HKI7Theme(
                themeColor = themeColor,
                themeMode = themeMode,
                systemLightThemeColor = systemLightThemeColor,
                systemDarkThemeColor = systemDarkThemeColor
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
                    if (forcedLogoutReason != null) {
                        forceLogin = true
                        snackbarHostState.showSnackbar(
                            message = forcedLogoutReason ?: "You have been logged out",
                            duration = SnackbarDuration.Long
                        )
                        viewModel.clearForcedLogoutReason()
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
                        Box {
                            OnboardingScreen(prefs = prefs, onComplete = {
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
            NavHost(
                navController,
                startDestination = Screen.Home.route,
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

        // When the bar is too narrow for every tab (small screens / many tabs), fall back to
        // fixed-width tabs in a horizontally scrollable row instead of squeezing weight()-tabs.
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val navBarScrollable = !isEditMode && (configuration.screenWidthDp - 64) < screens.size * 64
        HKIBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalPadding = 32.dp,
            scrollable = navBarScrollable
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

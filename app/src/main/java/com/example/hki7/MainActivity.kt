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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.HKIBottomBar
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
    
    val screens = listOf(Screen.Home, Screen.Rooms, Screen.Security, Screen.Energy)
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

    LaunchedEffect(Unit) {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasLocation = permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasLocation) viewModel.startLocationReporting(context) else permissionLauncher.launch(permissions)
        // No battery-optimization exemption prompt: presence is event-driven (geofences) and the
        // periodic refresh runs under WorkManager, both of which work while the app is Dozed. Letting
        // the OS power-manage us normally is the point — it's how the official app sips battery.
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavHost(
                navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Home.route)     { HAHomeScreen(viewModel) }
                composable(Screen.Rooms.route)    { RoomsScreen(viewModel, navController) }
                composable(Screen.Security.route) { SecurityScreen(viewModel) }
                composable(Screen.Energy.route)   { EnergyScreen(viewModel) }
                composable(
                    route = Screen.RoomDetail.route,
                    arguments = listOf(navArgument("areaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
                    RoomDetailScreen(areaId, viewModel, navController)
                }
            }
        }

        HKIBottomBar(modifier = Modifier.align(Alignment.BottomCenter), horizontalPadding = 32.dp) {
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
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    if (screen == Screen.Rooms || screen == Screen.Security) {
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
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                                    modifier = Modifier.size(24.dp)
                                )
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

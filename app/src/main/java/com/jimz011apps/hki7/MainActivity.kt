@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import android.os.Build
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.ui.input.pointer.pointerInput
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
import com.jimz011apps.hki7.data.withStoredAppLocale
import com.jimz011apps.hki7.ui.ConnectionStatus
import com.jimz011apps.hki7.ui.connectionIssueGraceMillis
import com.jimz011apps.hki7.ui.HomeAssistantRestartPhase
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.data.HaParentalControls
import com.jimz011apps.hki7.data.HaDashboardSharing
import com.jimz011apps.hki7.data.VisibilityUserSession
import com.jimz011apps.hki7.ui.components.AppUpdateRequiredScreen
import com.jimz011apps.hki7.ui.components.IconEffectDefaults
import com.jimz011apps.hki7.ui.components.RoomMovePrompt
import com.jimz011apps.hki7.ui.components.LocalEntityCatalogProvider
import com.jimz011apps.hki7.ui.components.LocalVisibilityFamilyContext
import com.jimz011apps.hki7.ui.components.VisibilityFamilyContext
import com.jimz011apps.hki7.ui.components.LocalIconAnimationsEnabled
import com.jimz011apps.hki7.ui.components.LocalWeatherAnimations
import com.jimz011apps.hki7.ui.components.WeatherAnimationSettings
import com.jimz011apps.hki7.ui.NavBarConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.jimz011apps.hki7.ui.Screen
import com.jimz011apps.hki7.ui.localizedTitle
import com.jimz011apps.hki7.ui.localizedName
import com.jimz011apps.hki7.ui.components.EdgePanel
import com.jimz011apps.hki7.ui.components.EdgePanelState
import com.jimz011apps.hki7.ui.components.PanelEdge
import com.jimz011apps.hki7.ui.components.awaitEdgePanelDrags
import com.jimz011apps.hki7.ui.components.HKIBottomBar
import com.jimz011apps.hki7.ui.components.HKIBottomBarTabWidth
import com.jimz011apps.hki7.ui.components.rememberPagerHandoffBlocker
import com.jimz011apps.hki7.ui.components.PagerIndicator
import com.jimz011apps.hki7.ui.components.PagerIndicatorHeight
import com.jimz011apps.hki7.ui.components.HKIMediaPlayerDialog
import com.jimz011apps.hki7.ui.components.MediaPlayerMiniBar
import com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset
import com.jimz011apps.hki7.ui.components.LocalItemCornerRadius
import com.jimz011apps.hki7.ui.components.LocalOpenNotifications
import com.jimz011apps.hki7.ui.components.LocalOpenSettingsRoute
import com.jimz011apps.hki7.ui.screens.SettingsDialog
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.utils.IconPack
import com.jimz011apps.hki7.ui.utils.IconPreferences
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.components.CustomPopupHost
import com.jimz011apps.hki7.ui.components.NotificationPanel
import com.jimz011apps.hki7.ui.components.NotificationBannerHost
import com.jimz011apps.hki7.ui.components.QuickStartGuideDialog
import com.jimz011apps.hki7.ui.components.WhatsNewDialog
import com.jimz011apps.hki7.ui.components.hasChangelogForCurrentVersion
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

    // Applies the stored per-app language below API 33, where the platform has no LocaleManager to
    // do it for us. A no-op on API 33+ — see AppLocale.withStoredAppLocale.
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(newBase.withStoredAppLocale())
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Follow the system's rotation-lock setting rather than hard-locking to portrait: FULL_USER
        // rotates freely with the sensor when the user allows rotation, and behaves like a lock when
        // they've disabled auto-rotate. Deliberately set at runtime rather than via
        // android:screenOrientation: Play builds its device catalogue from the manifest only, and a
        // declared portrait lock drops every landscape-only form factor (it cost us all car devices
        // and a tablet). A runtime setting is invisible to that catalogue while behaving identically
        // on phones. Note Android 16+ ignores orientation locks on large screens either way.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
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
            val iconAnimationsEnabled by prefs.iconAnimationsEnabled.collectAsState(initial = false)
            val weatherAnimatePill by prefs.weatherAnimatePill.collectAsState(initial = true)
            val weatherAnimateDialog by prefs.weatherAnimateDialog.collectAsState(initial = true)
            val weatherAnimateForecast by prefs.weatherAnimateForecast.collectAsState(initial = true)
            val weatherAnimateWidget by prefs.weatherAnimateWidget.collectAsState(initial = true)
            val weatherAnimations = remember(
                weatherAnimatePill, weatherAnimateDialog, weatherAnimateForecast, weatherAnimateWidget
            ) {
                WeatherAnimationSettings(
                    pill = weatherAnimatePill,
                    dialog = weatherAnimateDialog,
                    forecast = weatherAnimateForecast,
                    widget = weatherAnimateWidget,
                )
            }
            val iconEffectDefaults by prefs.iconEffectDefaults.collectAsState(initial = emptyMap())
            LaunchedEffect(iconEffectDefaults) { IconEffectDefaults.byGroup = iconEffectDefaults }
            val defaultIconPack by prefs.defaultIconPack.collectAsState(initial = "mdi")
            LaunchedEffect(defaultIconPack) {
                IconPreferences.defaultPack = IconPack.fromId(defaultIconPack)
            }
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
                CompositionLocalProvider(
                    LocalItemCornerRadius provides itemCornerRadius,
                    LocalIconAnimationsEnabled provides iconAnimationsEnabled,
                    LocalWeatherAnimations provides weatherAnimations,
                ) {
                val appColors = LocalHKIAppColors.current
                val loading = "__hki_loading__"
                val serverUrl by prefs.serverUrl.collectAsState(initial = loading)
                val internalUrl by prefs.internalUrl.collectAsState(initial = loading)
                val accessToken by prefs.accessToken.collectAsState(initial = loading)
                val refreshToken by prefs.refreshToken.collectAsState(initial = loading)
                val instances by prefs.homeAssistantInstances.collectAsState(initial = emptyList())
                val activeInstanceId by prefs.activeHomeAssistantInstanceId.collectAsState(initial = null)
                var forceLogin by rememberSaveable { mutableStateOf(false) }
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
                val aestheticsOnlyEditing by viewModel.aestheticsOnlyEditing.collectAsState()
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
                var visibilityFamilyContext by remember(activeInstanceId) {
                    mutableStateOf(VisibilityFamilyContext())
                }
                LaunchedEffect(loggedIn, activeInstanceId) {
                    if (!loggedIn) {
                        visibilityFamilyContext = VisibilityFamilyContext()
                        VisibilityUserSession.update(null)
                    } else {
                        // Never carry one HA account's identity across an instance switch while the
                        // new companion-component identity is still being resolved.
                        visibilityFamilyContext = VisibilityFamilyContext()
                        VisibilityUserSession.update(null)
                        val identity = runCatching {
                            HaDashboardSharing.whoami(applicationContext)
                        }.getOrNull()
                        val users = if (identity?.isAdmin == true || identity?.isOwner == true) {
                            runCatching {
                                HaDashboardSharing.listUsers(applicationContext)
                            }.getOrDefault(emptyList())
                        } else {
                            emptyList()
                        }
                        visibilityFamilyContext = VisibilityFamilyContext(
                            currentUserId = identity?.userId,
                            isAdmin = identity?.isAdmin == true || identity?.isOwner == true,
                            users = users,
                            componentChecked = true,
                            componentAvailable = identity != null,
                        )
                        VisibilityUserSession.update(identity?.userId)
                    }
                }
                // Latch onboarding on once we know the user needs to log in, and keep it on through the
                // login + permission steps (saving the token mid-flow would otherwise jump to the app).
                // rememberSaveable so backgrounding on the permission step and returning to a recreated
                // Activity doesn't drop the latch — with the token already saved, loggedIn would be true
                // and onboarding would be silently skipped (no permissions, no dashboard choice, no guide).
                var onboardingActive by rememberSaveable { mutableStateOf(false) }
                var onboardingStartsAtLogin by rememberSaveable { mutableStateOf(false) }
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
                        CompositionLocalProvider(
                            LocalEntityCatalogProvider provides { viewModel.entities.value },
                            LocalVisibilityFamilyContext provides visibilityFamilyContext,
                            com.jimz011apps.hki7.ui.components.LocalAestheticsOnlyEditing provides aestheticsOnlyEditing,
                        ) {
                            MainApp(prefs, viewModel)
                        }
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

/** The single nav destination that hosts every bottom-bar tab as a page of one [HorizontalPager]. */
private const val TABS_ROUTE = "tabs"

/**
 * Somewhere the user has been, in the order they went there.
 *
 * Tabs are pages of one pager rather than back-stack entries, so back through them has to be
 * tracked by hand. Detail screens *are* back-stack entries — but tapping a tab from one pops it,
 * because the page change must not happen behind it, and that pop is what used to lose it. A
 * room the user tabbed away from is recorded here so back can return to the room itself instead
 * of stranding them on the Rooms overview.
 */
private sealed interface VisitedPlace {
    data class Tab(val index: Int) : VisitedPlace
    data class Detail(val route: String) : VisitedPlace
}

/** Tab routes kept in the graph purely as redirects into the pager, so route-based navigation from
 *  dashboard actions, badges, and popups (via `navRouteForTarget`) keeps resolving. Custom pages
 *  are handled separately because their route carries a `pageId` argument. */
private val topLevelRedirectRoutes: List<String> = listOf(
    Screen.Home.route,
    Screen.Rooms.route,
    Screen.Security.route,
    Screen.Energy.route,
    Screen.Climate.route,
    Screen.Battery.route,
)

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
    // Registered before the two full-screen gates below, both of which return early. They still
    // need the view model going through its normal foreground path — connecting, refreshing an
    // expired token, and re-reading what the admin currently requires — or a device could be shown
    // a gate it has no way of ever getting out of, because nothing would fetch the change that
    // lifts it.
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

    // "Your family expects a newer HKI." Shown ahead of everything else, including the dashboard
    // chooser, because being on a version the household has moved past is exactly the case where
    // the rest of the app may misbehave. Dismissal lives in this composable rather than in storage:
    // it lasts the session, and the next launch re-checks whether Play can serve the update now.
    val requiredAppUpdate by prefs.requiredAppUpdate.collectAsState(initial = null)
    var updatePromptDismissed by remember { mutableStateOf(false) }
    requiredAppUpdate?.takeIf { it.applies && !updatePromptDismissed }?.let { required ->
        AppUpdateRequiredScreen(
            required = required,
            prefs = prefs,
            onContinue = { updatePromptDismissed = true },
        )
        return
    }

    val familyDashboardAccessLost by prefs.familyDashboardAccessLost.collectAsState(initial = false)
    if (familyDashboardAccessLost) {
        OnboardingScreen(
            prefs = prefs,
            startAtDashboard = true,
            familyAccessLost = true,
            onComplete = { viewModel.completeInitialDashboardSetup() },
        )
        return
    }
    
    val connectionStatus by viewModel.status.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
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

    // "What's new": shown only to users who *updated*, and only once per release.
    // firstInstallTime vs lastUpdateTime identifies a first install directly from the package
    // manager, so a brand-new device can never see it — no matter what DataStore holds, and with
    // no race against onboarding writing its own state.
    val lastSeenVersionCode by prefs.lastSeenVersionCode.collectAsState(initial = -1)
    var showWhatsNew by remember { mutableStateOf(false) }
    LaunchedEffect(lastSeenVersionCode, quickStartGuidePending) {
        if (lastSeenVersionCode < 0) return@LaunchedEffect            // DataStore not read yet
        if (lastSeenVersionCode == BuildConfig.VERSION_CODE) return@LaunchedEffect  // already seen
        if (quickStartGuidePending) return@LaunchedEffect             // never stack on the first-run guide
        // Only a previously-recorded version proves this is a genuine update. A fresh install has
        // lastSeenVersionCode 0 (nothing recorded yet), so it adopts the current version silently and
        // never sees the notes. This is more reliable than package first-install/last-update times,
        // which can differ by a few ms even on a first Play install.
        val isUpdate = lastSeenVersionCode in 1 until BuildConfig.VERSION_CODE
        if (isUpdate && hasChangelogForCurrentVersion()) {
            showWhatsNew = true
        } else {
            // Fresh install, or nothing to announce: adopt this version as the baseline silently
            // so the next update is the first one that actually shows notes.
            prefs.saveLastSeenVersionCode(BuildConfig.VERSION_CODE)
        }
    }
    if (showWhatsNew) {
        WhatsNewDialog(
            onDismiss = {
                showWhatsNew = false
                quickStartScope.launch { prefs.saveLastSeenVersionCode(BuildConfig.VERSION_CODE) }
            }
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
    // Parental controls: routes an admin hid from this user. `home` is never removed so the
    // user always lands somewhere. This is UX-level hiding, not a security boundary.
    val parentalHiddenViews by prefs.parentalHiddenViews.collectAsState(initial = emptyList())
    val screens = remember(navBarOrder, navBarHidden, customPages, parentalHiddenViews) {
        val hiddenByParent = parentalHiddenViews.toSet() - Screen.Home.route
        NavBarConfig.visibleTabs(navBarOrder, navBarHidden, customPages)
            .filter { it.route !in hiddenByParent }
    }
    // Refresh this user's policy from the hki7 component whenever we're authenticated.
    LaunchedEffect(Unit) {
        runCatching { HaParentalControls.refreshForCurrentUser(appCtx, prefs) }
        runCatching { HaParentalControls.refreshRoomFollowRoster(appCtx, prefs) }
        // Idempotent — WorkManager keeps one daily job whatever the launch count.
        if (prefs.updateChecksEnabled.first()) {
            runCatching { com.jimz011apps.hki7.data.UpdateCheckWorker.schedule(appCtx) }
        }
        // A release that adds a sensor bumps SENSOR_SET_REVISION, and the marker stored against
        // the webhook then no longer matches. Kicking one telemetry run here means the new entity
        // appears in Home Assistant on the next launch rather than up to fifteen minutes later,
        // and without anyone having to re-register the device by hand.
        runCatching {
            if (com.jimz011apps.hki7.data.sensorRegistrationStale(prefs)) {
                com.jimz011apps.hki7.data.LocationWork.syncNow(appCtx)
            }
        }
    }
    val isEditMode by viewModel.isEditMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // ── Room following ──────────────────────────────────────────────────
    val roomFollow by viewModel.roomFollow.collectAsState()
    val followedAreaId by viewModel.followedAreaId.collectAsState()
    val pendingRoomMove by viewModel.pendingRoomMove.collectAsState()
    val parentalHiddenRooms by prefs.parentalHiddenRooms.collectAsState(initial = emptyList())
    val hiddenRoomIds = remember(parentalHiddenRooms) { parentalHiddenRooms.toSet() }
    // The rooms a swipe can actually reach. Rooms an admin hid for this person are excluded here
    // rather than only at the point of opening one, so paging can never land on a hidden room —
    // the old index-stepping swipe walked the unfiltered list and could.
    val visibleRooms = remember(areas, hiddenRoomIds) { areas.filter { it.area_id !in hiddenRoomIds } }
    val roomPagerState = rememberPagerState(pageCount = { visibleRooms.size })
    val onRoomDetail = currentRoute == Screen.RoomDetail.route
    // Read both by the bottom stack (to draw it) and by the page inset (to reserve space for it),
    // so the two can never disagree about whether it is on screen.
    val showRoomPagerIndicator = onRoomDetail && !isEditMode && visibleRooms.size > 1
    // Which room is on screen is now the pager's answer, not the route argument's: paging changes
    // the room without changing the route, and room following compares against this to decide
    // whether a move is worth acting on.
    val currentAreaId = if (onRoomDetail) {
        visibleRooms.getOrNull(roomPagerState.currentPage)?.area_id
    } else null
    // A room the admin hid from this person must never be opened for them, however they got there.
    fun canOpenRoom(areaId: String?): Boolean =
        areaId != null && areaId !in hiddenRoomIds && areas.any { it.area_id == areaId }

    var openedFollowedRoom by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(roomFollow, followedAreaId, areas) {
        // Once per app start: a later move is the move prompt's job, not a silent jump.
        if (openedFollowedRoom || !roomFollow.isActive || !roomFollow.openOnLaunch) return@LaunchedEffect
        if (areas.isEmpty()) return@LaunchedEffect
        val target = followedAreaId
        if (!canOpenRoom(target)) return@LaunchedEffect
        openedFollowedRoom = true
        navController.navigate(Screen.RoomDetail.createRoute(target!!))
    }

    // Confirming a move is about elapsed time, not about the sensor repeating itself, so this
    // ticks rather than reacting to state changes. The room on screen must be read through
    // rememberUpdatedState: navigating between rooms changes none of the effect's keys, so a
    // captured value would stay stuck on whatever was open when the loop started and the
    // "already in that room" check below would never match.
    val latestAreaId by rememberUpdatedState(currentAreaId)
    LaunchedEffect(roomFollow.isActive, roomFollow.continueAfterLaunch, roomFollow.promptOnMove, isEditMode) {
        // Turning following off, entering edit mode, or turning off continued tracking must also
        // retire a prompt already on screen — otherwise it sits there waiting for an answer to a
        // question no longer asked. Turning *prompting* off is not a reason to stop: the tracker
        // keeps running and the move is opened silently instead (see observeRoomPresence).
        if (!roomFollow.isActive || !roomFollow.continueAfterLaunch || isEditMode) {
            viewModel.cancelRoomMovePrompt()
            return@LaunchedEffect
        }
        if (!roomFollow.promptOnMove) viewModel.cancelRoomMovePrompt()
        while (true) {
            viewModel.observeRoomPresence(latestAreaId)
            delay(2.seconds)
        }
    }

    // Prompting off: a confirmed move opens straight away, with the same guards the prompt path
    // applies — never into a room the admin hid, and never into the room already on screen.
    val autoRoomMove by viewModel.autoRoomMove.collectAsState()
    LaunchedEffect(autoRoomMove, isEditMode, roomFollow.isActive) {
        val target = autoRoomMove ?: return@LaunchedEffect
        // Re-checked here and not just where the move was confirmed: following can be switched off
        // between confirming a move and this effect running, and navigating then would look exactly
        // like the toggle being ignored.
        if (isEditMode || !roomFollow.isActive) return@LaunchedEffect
        if (canOpenRoom(target) && target != latestAreaId) {
            navController.navigate(Screen.RoomDetail.createRoute(target))
        }
        viewModel.consumeAutoRoomMove(target)
    }

    pendingRoomMove?.let { targetAreaId ->
        val roomName = areas.firstOrNull { it.area_id == targetAreaId }?.name
        if (roomName == null || !canOpenRoom(targetAreaId)) {
            LaunchedEffect(targetAreaId) { viewModel.resolveRoomMove(accepted = false) }
        } else {
            RoomMovePrompt(
                roomName = roomName,
                onSwitch = {
                    viewModel.resolveRoomMove(accepted = true)
                    navController.navigate(Screen.RoomDetail.createRoute(targetAreaId))
                },
                onStay = { viewModel.resolveRoomMove(accepted = false) },
                onSilenceUntilRestart = { viewModel.silenceRoomMovePromptUntilRestart() }
            )
        }
    }
    // Sits between either pager and its pages, so a horizontal row that runs out of room stops
    // there instead of turning the rest of the drag into a page change.
    val pagerHandoffBlocker = rememberPagerHandoffBlocker()
    // The pager owns which tab is showing; the bottom bar and the nav graph both defer to it.
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val pagerScope = rememberCoroutineScope()
    val onTabsDestination = currentRoute == TABS_ROUTE
    // Which top-level views currently have a sub-page open (Energy > Solar and friends).
    val openSubPages = remember { mutableStateMapOf<String, Boolean>() }
    val subPageReporter: (String, Boolean) -> Unit = remember(openSubPages) {
        { route, open -> if (open) openSubPages[route] = true else openSubPages.remove(route) }
    }
    // Tabs the user has actually visited, oldest first. Back walks this rather than jumping
    // straight home, so it undoes where you have been instead of discarding it.
    // Re-tapping the tab you are already on returns that view to the top.
    val scrollToTopSignals = remember { mutableStateMapOf<String, Int>() }
    val visitedPages = remember { mutableStateListOf<VisitedPlace>() }
    // Rooms visited, oldest first. Backing out of a room walks this, so it retraces the rooms the
    // user actually opened rather than dropping them back into whichever tab they came from.
    val visitedRooms = remember { mutableStateListOf<String>() }
    LaunchedEffect(roomPagerState, visibleRooms) {
        snapshotFlow { roomPagerState.settledPage }.distinctUntilChanged().collect { page ->
            val areaId = visibleRooms.getOrNull(page)?.area_id ?: return@collect
            visitedRooms.remove(areaId)
            visitedRooms.add(areaId)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            // Revisiting a tab moves it to the top rather than stacking another copy, so back can
            // never walk the same pair of tabs over and over.
            visitedPages.removeAll { it is VisitedPlace.Tab && it.index == page }
            visitedPages.add(VisitedPlace.Tab(page))
        }
    }
    var pageSwipeInProgress by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState, roomPagerState) {
        snapshotFlow { pagerState.isScrollInProgress || roomPagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { pageSwipeInProgress = it }
    }
    val currentTopLevelIndex = if (onTabsDestination) pagerState.currentPage else -1
    // A pager holds several pages composed at once, so a state update landing mid-swipe recomposes
    // every one of them. Room pages are heavy enough that this costs frames — which is why room
    // swiping stuttered while tab swiping (lighter pages) did not. Updates wait for the swipe.
    LaunchedEffect(pagerState, roomPagerState, viewModel) {
        snapshotFlow { pagerState.isScrollInProgress || roomPagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> viewModel.setUiUpdatesPaused(scrolling) }
    }
    // Hiding a tab (or an admin revoking one) can leave the pager past the end of a now-shorter list.
    LaunchedEffect(screens.size) {
        if (pagerState.currentPage > screens.lastIndex && screens.isNotEmpty()) {
            pagerState.scrollToPage(screens.lastIndex)
        }
    }
    val navigateToTopLevel: (Screen) -> Unit = { screen ->
        val index = screens.indexOfFirst { it.route == screen.route }
        if (index >= 0 && onTabsDestination && pagerState.currentPage == index) {
            // Already here: the tap means "take me back to the top", not "go to this tab".
            scrollToTopSignals[screen.route] = (scrollToTopSignals[screen.route] ?: 0) + 1
        } else if (index >= 0) {
            // Read before the pop, because after it there is no detail screen left to ask. The
            // room pager owns which room is showing, so the areaId the route was opened with may
            // be several swipes stale — take the room actually on screen.
            val leaving: String? = when {
                onTabsDestination -> null
                onRoomDetail -> visibleRooms.getOrNull(roomPagerState.currentPage)
                    ?.area_id?.let(Screen.RoomDetail::createRoute)
                // Anything else parameterless (the battery screen) can be navigated back to as-is.
                // Routes still carrying a `{placeholder}` are redirects into the pager, not places.
                currentRoute != null && !currentRoute.contains('{') -> currentRoute
                else -> null
            }
            pagerScope.launch {
                // A tab tap from a detail screen (room, battery widget) has to come back to the
                // pager first, otherwise the page change would happen behind that screen.
                if (!onTabsDestination) {
                    leaving?.let { route ->
                        visitedPages.removeAll { it is VisitedPlace.Detail && it.route == route }
                        visitedPages.add(VisitedPlace.Detail(route))
                    }
                    navController.popBackStack(TABS_ROUTE, inclusive = false)
                }
                pagerState.animateScrollToPage(index)
            }
        }
    }
    // Tab paging is the pager's job and room paging is the room pager's, so nothing drives a
    // route-level swipe any more. What is left is the ordinary push/pop into a room or the battery
    // widget, which slides in from the trailing edge like any detail screen.
    val context = LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.startLocationReporting(context)
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Google Play prominent disclosure: never launch a location permission prompt cold. If location
    // reporting is enabled but the permission is missing, the disclosure dialog is shown first and
    // the request only launches on "Agree".
    var showLocationDisclosure by remember { mutableStateOf(false) }
    val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (showLocationDisclosure) {
        com.jimz011apps.hki7.ui.components.LocationDisclosureDialog(
            onAgree = {
                showLocationDisclosure = false
                permissionLauncher.launch(locationPermissions)
            },
            onDismiss = { showLocationDisclosure = false }
        )
    }

    LaunchedEffect(Unit) {
        // Demo sessions have no server to report to and must not open with surprise permission
        // prompts (Google Play reviewers see the demo first).
        val needsRealLocation = prefs.homeAssistantInstances.first().any { instance ->
            instance.locationEnabled && !isDemoServerUrl(instance.primaryUrl)
        }
        if (!needsRealLocation) return@LaunchedEffect
        val hasLocation = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasLocation) viewModel.startLocationReporting(context) else showLocationDisclosure = true
        // Only API 33+ has a notification permission to ask for; below that they're already on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
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
    // Both panels are dragged rather than toggled: their progress is what the edge gesture writes
    // to, so the sheet is wherever the finger has taken it and settles when the finger lifts.
    val notificationPanel = remember { EdgePanelState() }
    val instancePanel = remember { EdgePanelState() }
    val drawerScope = rememberCoroutineScope()
    // Back from any tab returns to the first one before leaving the app. Tab changes are pager
    // scrolls, not back-stack entries, so without this back would exit straight from a deep tab —
    // and retracing every tab the user had swiped through would be worse, not better.
    androidx.activity.compose.BackHandler(enabled = onTabsDestination && visitedPages.size > 1) {
        pagerScope.launch {
            // Drop where we are, go back to where we were. Falls through to leaving the app once
            // there is nothing behind the current tab.
            visitedPages.removeLastOrNull()
            when (val previous = visitedPages.lastOrNull()) {
                is VisitedPlace.Tab ->
                    if (previous.index in screens.indices) pagerState.animateScrollToPage(previous.index)
                // A detail screen the user tabbed away from. Taken off the list as we go to it:
                // it becomes the current destination, and its own back handling takes over. Left
                // on the list it would be offered again after leaving the room, which is how
                // backing out of a room used to bounce straight back to the tab.
                is VisitedPlace.Detail -> {
                    visitedPages.remove(previous)
                    navController.navigate(previous.route)
                }
                null -> Unit
            }
        }
    }
    // Back out of a room goes to the room before it, or to the room list — never to the tab the
    // room happened to be opened from. Without this the NavHost's own pop lands on whatever page
    // the pager is parked on, so opening a room from Energy and pressing back twice ping-ponged
    // between the room and Energy instead of ending up somewhere sensible.
    androidx.activity.compose.BackHandler(enabled = onRoomDetail && !isEditMode) {
        pagerScope.launch {
            visitedRooms.removeLastOrNull()
            val previousRoom = visitedRooms.lastOrNull()
                ?.let { areaId -> visibleRooms.indexOfFirst { it.area_id == areaId } }
                ?.takeIf { it >= 0 }
            if (previousRoom != null) {
                roomPagerState.animateScrollToPage(previousRoom)
            } else {
                // Nothing behind this room: leave for the room list, and make sure the pager is
                // actually showing it rather than whatever tab was last open underneath.
                visitedPages.removeAll { it is VisitedPlace.Detail }
                navController.popBackStack(TABS_ROUTE, inclusive = false)
                screens.indexOfFirst { it.route == Screen.Rooms.route }
                    .takeIf { it >= 0 }
                    ?.let { pagerState.scrollToPage(it) }
            }
        }
    }
    // isOpen reads the settle *target*, so back works from the instant a drag commits rather than
    // only once the sheet has finished arriving. Keyed on the target and not the settled value, this
    // also closes the window where back used to fall through to the NavHost mid-animation and — from
    // the first tab — close the app instead of the panel that had just been opened.
    androidx.activity.compose.BackHandler(enabled = notificationPanel.isOpen) {
        drawerScope.launch { notificationPanel.close() }
    }
    androidx.activity.compose.BackHandler(enabled = instancePanel.isOpen) {
        drawerScope.launch { instancePanel.close() }
    }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    // Arabic mirrors the whole app, and the edge panels are defined by reading direction rather
    // than by pixels — so the strips they open from, and the way they slide, follow this.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Height the three-button navigation bar occupies (0 under gesture navigation): pages add it to
    // their bottom scroll reserve so content still clears the floating bar once that bar moves up.
    val systemButtonBarInset = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()
    val edgeStripWidthPx = with(density) { 28.dp.toPx() }
    val edgeStripTopPx = with(density) { 56.dp.toPx() }
    // Was 200dp. Every pixel of this strip is a pixel where the system back gesture does not work,
    // and it sat exactly where the on-screen back arrow is — so back appeared broken on the edge
    // people reach for first. Trimmed to a short band beside the header, with the panel also
    // reachable from the pull-down header menu (a swipe is no longer the only way in).
    val edgeStripHeightPx = with(density) { 88.dp.toPx() }
    val instanceStripHeightPx = with(density) { (if (headerVisible) 88.dp else 72.dp).toPx() }
    val screenWidthPx = windowInfo.containerSize.width.toFloat()
    // Hosted here rather than on the page, because the drawer sheet is a sibling of the page and
    // cannot see anything the page provides — and the drawer is where the deep link comes from.
    var settingsRoute by remember { mutableStateOf<String?>(null) }
    settingsRoute?.let { route ->
        SettingsDialog(
            prefs = prefs,
            viewModel = viewModel,
            onDismiss = { settingsRoute = null },
            initialRoute = route,
        )
    }
    CompositionLocalProvider(
        LocalOpenNotifications provides { drawerScope.launch { notificationPanel.open() } },
        LocalOpenSettingsRoute provides { route: String ->
            drawerScope.launch { notificationPanel.close() }
            settingsRoute = route
        },
        LocalCameraFullscreenLauncher provides launchFullscreenCamera,
        // Covers the room pager too: rooms are their own navigation destination, so a provider
        // scoped to the tab pager never reached them and their header stayed open on swipe.
        com.jimz011apps.hki7.ui.components.LocalPageSwipeInProgress provides pageSwipeInProgress,
        com.jimz011apps.hki7.ui.components.LocalOpenTopLevelRoute provides { route: String ->
            val target = screens.firstOrNull { it.route == route }
            if (target != null) {
                navigateToTopLevel(target)
            } else if (route == Screen.Battery.route) {
                // Hidden from the bar: there is no tab to land on, so the pushed view is all that
                // is left — with its own back arrow, which is the behaviour being avoided above.
                navController.navigate(Screen.Battery.WIDGET_ROUTE)
            }
            true
        }
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Match the page background so the mini-player inset never shows as a gray strip.
            .background(appColors.background)
            .systemGestureExclusion {
                // Carve the notification panel's edge out of the system back gesture (Pixel-style
                // gesture nav) so swipes starting there reach the app. It follows reading direction
                // with the panel itself: the leading edge is the left in English, the right in
                // Arabic, and the exclusion has to be on whichever side the panel actually opens
                // from or the gesture is blocked on one side and dead on the other.
                val leading = if (isRtl) screenWidthPx - edgeStripWidthPx else 0f
                Rect(leading, edgeStripTopPx, leading + edgeStripWidthPx, edgeStripTopPx + edgeStripHeightPx)
            }
            .systemGestureExclusion {
                // The instance switcher mirrors it on the trailing edge.
                val trailing = if (isRtl) 0f else screenWidthPx - edgeStripWidthPx
                Rect(trailing, edgeStripTopPx, trailing + edgeStripWidthPx, edgeStripTopPx + instanceStripHeightPx)
            }
            .pointerInput(isEditMode) {
                if (isEditMode) return@pointerInput
                // Paging belongs to the HorizontalPager below; this only watches the two edge
                // strips, and drags the matching panel open under the finger rather than firing it
                // open once a threshold is passed. It consumes movement once the drag is committed,
                // which is what stops the pager acting on the same gesture.
                awaitEdgePanelDrags(
                    scope = drawerScope,
                    stripWidthPx = edgeStripWidthPx,
                    startStrip = edgeStripTopPx..(edgeStripTopPx + edgeStripHeightPx),
                    endStrip = edgeStripTopPx..(edgeStripTopPx + instanceStripHeightPx),
                    start = notificationPanel,
                    end = instancePanel,
                    rtl = isRtl
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
                    (if (activeMediaPlayers.isNotEmpty() && !mediaBarDismissed) 86.dp else 0.dp) +
                        (if (showConnectionBar) 62.dp else 0.dp) +
                        // The room indicator is one more floating thing in the bottom stack, so the
                        // page has to reserve room for it too — otherwise a room's last row of
                        // content scrolls to a stop underneath the pill.
                        (if (showRoomPagerIndicator) PagerIndicatorHeight else 0.dp)
                } else 0.dp
            ) {
                NavHost(
                    navController,
                    startDestination = TABS_ROUTE,
                    // Pages add this overlay height to their scroll content, not their background.
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    // Detail screens push in from the trailing edge and pop back out the same way.
                    // The redirect destinations below never render anything, so giving them the
                    // same transition costs nothing.
                    enterTransition = { slideInHorizontally(tween(230)) { it } + fadeIn(tween(150)) },
                    exitTransition = { slideOutHorizontally(tween(230)) { -it / 4 } + fadeOut(tween(150)) },
                    popEnterTransition = { slideInHorizontally(tween(230)) { -it / 4 } + fadeIn(tween(150)) },
                    popExitTransition = { slideOutHorizontally(tween(230)) { it } + fadeOut(tween(150)) }
                ) {
                // Every bottom-bar tab lives inside one pager destination now, so a horizontal drag
                // moves the pages under the finger and can be reversed mid-gesture — the previous
                // detector committed at a fixed distance and then ignored the rest of the drag.
                composable(TABS_ROUTE) {
                    CompositionLocalProvider(
                        com.jimz011apps.hki7.ui.components.LocalSubPageReporter provides subPageReporter,
                        com.jimz011apps.hki7.ui.components.LocalScrollToTopSignals provides scrollToTopSignals
                    ) {
                    HorizontalPager(
                        state = pagerState,
                        // Same as the room pager: composing the neighbour while idle rather than in
                        // the first frames of the drag is what makes paging feel fluid. This was 0
                        // only because Energy used to start fetching as soon as it was composed —
                        // its root effects now wait for `isActive` below, so the cost is gone.
                        beyondViewportPageCount = 1,
                        // Only the page on screen decides: a neighbour parked in a sub-page must
                        // not lock the one being looked at.
                        userScrollEnabled = !isEditMode &&
                            openSubPages[screens.getOrNull(pagerState.currentPage)?.route] != true,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // settledPage, not currentPage: currentPage flips at the halfway point of a
                        // drag, which would start a page's loads for a swipe the user reverses.
                        val isSettled = pagerState.settledPage == page
                        Box(Modifier.fillMaxSize().nestedScroll(pagerHandoffBlocker)) {
                            when (val screen = screens.getOrNull(page)) {
                                Screen.Home -> HAHomeScreen(viewModel, navController)
                                Screen.Rooms -> RoomsScreen(viewModel, navController)
                                Screen.Security -> SecurityScreen(viewModel)
                                Screen.Energy -> EnergyScreen(viewModel, isActive = isSettled)
                                Screen.Climate -> ClimateScreen(viewModel)
                                Screen.Battery -> BatteryScreen(viewModel, navController, showBackButton = false)
                                is Screen.Custom -> HAHomeScreen(
                                    viewModel,
                                    navController,
                                    widgetAreaId = "__custom_page_${screen.page.id}__",
                                    customPage = screen.page
                                )
                                else -> Unit
                            }
                        }
                    }
                }
                // The tab routes stay registered as redirects. Dashboard buttons, badges, and popups
                // all navigate by route through handleActionOutcome/navRouteForTarget, and keeping
                // them resolvable means none of those call sites had to learn about the pager.
                topLevelRedirectRoutes.forEach { route ->
                    composable(route) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                            screens.indexOfFirst { it.route == route }
                                .takeIf { it >= 0 }
                                ?.let { pagerState.animateScrollToPage(it) }
                        }
                    }
                    }
                }
                composable(Screen.Battery.WIDGET_ROUTE) { BatteryScreen(viewModel, navController, showBackButton = true) }
                composable(
                    route = Screen.CUSTOM_PAGE_ROUTE,
                    arguments = listOf(navArgument("pageId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pageId = backStackEntry.arguments?.getString("pageId").orEmpty()
                    LaunchedEffect(pageId) {
                        navController.popBackStack()
                        screens.indexOfFirst { it is Screen.Custom && it.page.id == pageId }
                            .takeIf { it >= 0 }
                            ?.let { pagerState.animateScrollToPage(it) }
                    }
                }
                composable(
                    route = Screen.RoomDetail.route,
                    arguments = listOf(navArgument("areaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
                    // The route argument only picks the starting room; from there the pager owns
                    // which one is showing. Every entry point (room list, room following, dashboard
                    // actions) therefore lands on the right page without knowing about the pager.
                    LaunchedEffect(areaId, visibleRooms) {
                        visibleRooms.indexOfFirst { it.area_id == areaId }
                            .takeIf { it >= 0 && it != roomPagerState.currentPage }
                            ?.let { roomPagerState.scrollToPage(it) }
                    }
                    if (visibleRooms.isEmpty()) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        HorizontalPager(
                            state = roomPagerState,
                            // A room screen is by far the heaviest page in the app. At 0 the pager
                            // composes the neighbour during the first frames of the drag, which is
                            // exactly when there is no budget for it and is what made room paging
                            // feel choppy next to the tabs. Composing it one page ahead moves that
                            // work into the idle time after the previous swipe settles.
                            beyondViewportPageCount = 1,
                            // Identity follows the room, not the slot, so reordering or hiding a
                            // room re-associates pages instead of rebuilding the neighbours.
                            key = { page -> visibleRooms.getOrNull(page)?.area_id ?: page.toString() },
                            userScrollEnabled = !isEditMode,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            visibleRooms.getOrNull(page)?.let { room ->
                                Box(Modifier.fillMaxSize().nestedScroll(pagerHandoffBlocker)) {
                                    RoomDetailScreen(room.area_id, viewModel, navController)
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        NotificationBannerHost(viewModel, Modifier.align(Alignment.TopCenter))

        // Popup actions can fire from any surface (buttons, badges, dialog nav bars), so their
        // dialog is hosted here once instead of being threaded through every screen.
        CustomPopupHost(viewModel, navController)

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
        // 64.dp is the bar's own horizontal padding (32.dp a side); the tabs themselves are measured
        // at the width the scrollable row actually gives them.
        val navBarScrollable = !isEditMode && (screenWidth - 64.dp) < HKIBottomBarTabWidth * screens.size
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Lift the whole bottom stack clear of three-button navigation, tucking it 8dp back
                // down so it sits close to the system buttons rather than floating well above them
                // (the bar carries its own 15dp gap). Zero under gesture navigation, where
                // tappableElement reports no inset and the bar keeps its usual position.
                .padding(bottom = (systemButtonBarInset - 8.dp).coerceAtLeast(0.dp))
        ) {
        // How many rooms there are and where you are among them — the one thing the room swipe
        // never told anyone. A sibling of the media and nav bars rather than an overlay on them, so
        // the bottom stack lays out in order and the Column's own inset keeps the whole lot clear
        // of three-button navigation.
        if (showRoomPagerIndicator) {
            PagerIndicator(
                pageCount = visibleRooms.size,
                currentPage = roomPagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
            )
        }
        if (!isEditMode) {
            when {
                showConnectionBar -> HomeAssistantConnectionBar(
                    status = connectionStatus,
                    restartPhase = homeAssistantRestartPhase,
                    isAutoGenerating = autoGenerationPending,
                    connectionError = connectionError,
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
            // Keeps the active tab centred once the bar overflows, so paging to the last tab no
            // longer leaves the indicator off-screen. Edit mode shows undo/redo/done, not tabs.
            selectedIndex = if (isEditMode) null else currentTopLevelIndex.takeIf { it >= 0 },
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
                EditNavButton(
                    Icons.AutoMirrored.Filled.Undo,
                    stringResource(R.string.action_undo),
                    enabled = canUndo
                ) { viewModel.undo() }
                EditNavButton(
                    Icons.AutoMirrored.Filled.Redo,
                    stringResource(R.string.action_redo),
                    enabled = canRedo
                ) { viewModel.redo() }
                EditNavButton(
                    Icons.Default.CheckCircle,
                    stringResource(R.string.ui_done_e9b450d)
                ) { viewModel.toggleEditMode() }
            } else {
                screens.forEachIndexed { index, screen ->
                        // Selection follows the pager while the tabs are showing. A detail screen
                        // pushed on top keeps its originating tab lit — a room belongs to Rooms, the
                        // battery widget to Battery — so the bar never reads as "nothing selected".
                        val isSelected = when {
                            currentRoute == Screen.RoomDetail.route -> screen == Screen.Rooms
                            currentRoute == Screen.Battery.WIDGET_ROUTE -> screen == Screen.Battery
                            else -> onTabsDestination && pagerState.currentPage == index
                        }

                        Column(
                            modifier = Modifier
                                .then(
                                    // weight() needs a bounded row; scrollable rows use fixed-width tabs.
                                    // The width must stay HKIBottomBarTabWidth — the bar computes its
                                    // scroll-into-view offset from it.
                                    if (navBarScrollable) Modifier.width(HKIBottomBarTabWidth) else Modifier.weight(1f)
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
                                text = screen.localizedTitle(),
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

        // Both edge panels are drawn here, above the page and below the dialogs, so their scrim
        // covers the content while the sheet itself stays clear of anything modal.
        EdgePanel(
            state = notificationPanel,
            edge = PanelEdge.Start,
            scope = drawerScope,
            containerColor = appColors.background,
            contentColor = appColors.onSurface
        ) {
            // The panel is composed only while it is at least partly on screen, but the Events
            // tab's logbook subscription should not start for a drag that gets abandoned — so it
            // waits until the panel is actually being looked at rather than merely peeking.
            NotificationPanel(viewModel, isVisible = notificationPanel.isOpen)
        }

        EdgePanel(
            state = instancePanel,
            edge = PanelEdge.End,
            scope = drawerScope,
            containerColor = appColors.background,
            contentColor = appColors.onSurface
        ) {
            InstanceSwitcherContent(
                instances = homeAssistantInstances,
                activeInstanceId = activeHomeAssistantInstanceId,
                onDismiss = { drawerScope.launch { instancePanel.close() } },
                onSelect = { instanceId ->
                    drawerScope.launch { instancePanel.close() }
                    viewModel.switchHomeAssistantInstance(instanceId)
                    navigateToTopLevel(Screen.Home)
                },
                onAdd = {
                    drawerScope.launch { instancePanel.close() }
                    showAddHomeAssistantInstance = true
                }
            )
        }

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

/** Contents of the home switcher. The sheet, scrim and drag behaviour belong to [EdgePanel], which
 *  the notification panel shares — the two used to slide in differently for no reason anyone could
 *  have named. */
@Composable
private fun InstanceSwitcherContent(
    instances: List<HomeAssistantInstance>,
    activeInstanceId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
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
                    Text(stringResource(R.string.ui_switch_home_e6ceda2), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(stringResource(R.string.ui_choose_the_home_assistant_shown_in_hki_7_b29bc26), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.ui_close_bbfa773))
                }
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
                            if (selected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    stringResource(R.string.ui_active_a733b80),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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
                Text(stringResource(R.string.ui_add_home_assistant_a6ccea8))
            }
            Text(
                stringResource(R.string.ui_open_this_panel_by_swiping_left_from_the_upper_671e63c),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
}

@Composable
private fun ConnectionErrorOverlay(viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
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
            Text(stringResource(R.string.ui_unable_to_connect_8207f1b), style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                if (currentUrl.isBlank()) stringResource(R.string.ui_couldn_t_reach_your_home_assistant_server_2a17c09)
                else stringResource(R.string.ui_couldn_t_reach_a30cc1b, currentUrl),
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.onMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            connectionError?.takeIf { it.isNotBlank() }?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    localizedConnectionError(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
                    Text(stringResource(R.string.ui_connecting_fd3e796))
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_refresh_56e3bad))
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
                Text(stringResource(R.string.ui_log_in_again_1ee6e18))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ui_logging_in_again_keeps_your_dashboard_and_settings_9a8ed6f),
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onMuted
            )
        }
    }
}

@Composable
private fun localizedConnectionError(error: String): String = when (error) {
    "Server address could not be found" -> stringResource(R.string.connection_server_not_found)
    "Connection timed out" -> stringResource(R.string.connection_timed_out)
    "No network route to Home Assistant" -> stringResource(R.string.connection_no_route)
    "Could not connect to Home Assistant" -> stringResource(R.string.connection_could_not_connect)
    "Secure connection failed" -> stringResource(R.string.connection_secure_failed)
    "Session expired · Refreshing login…" -> stringResource(R.string.connection_session_expired_refreshing)
    "Connection was interrupted" -> stringResource(R.string.connection_interrupted)
    else -> error
}

@Composable
private fun localizedConnectionStatusLabel(
    status: ConnectionStatus,
    restartPhase: HomeAssistantRestartPhase,
    isAutoGenerating: Boolean,
    connectionError: String?
): String = when {
    restartPhase == HomeAssistantRestartPhase.STOPPING ->
        stringResource(R.string.connection_stopping_for_restart)
    restartPhase == HomeAssistantRestartPhase.STARTING ->
        stringResource(R.string.connection_starting)
    restartPhase == HomeAssistantRestartPhase.RESTORING ->
        stringResource(R.string.connection_restoring_dashboard)
    restartPhase == HomeAssistantRestartPhase.RESTARTING ->
        stringResource(R.string.connection_restarting)
    status != ConnectionStatus.CONNECTED && !connectionError.isNullOrBlank() ->
        localizedConnectionError(connectionError)
    isAutoGenerating -> stringResource(R.string.connection_auto_generating)
    status == ConnectionStatus.CONNECTING -> stringResource(R.string.connection_reconnecting)
    status == ConnectionStatus.ERROR -> stringResource(R.string.connection_unavailable_retrying)
    status == ConnectionStatus.IDLE -> stringResource(R.string.connection_paused)
    else -> stringResource(R.string.connection_connected)
}

@Composable
private fun HomeAssistantConnectionBar(
    status: ConnectionStatus,
    restartPhase: HomeAssistantRestartPhase,
    isAutoGenerating: Boolean,
    connectionError: String?,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val label = localizedConnectionStatusLabel(
        status,
        restartPhase,
        isAutoGenerating,
        connectionError
    )
    val title = if (isAutoGenerating) stringResource(R.string.ui_building_your_dashboard_2943961) else stringResource(R.string.ui_home_assistant_c8fd3bb)
    val showingError = restartPhase == HomeAssistantRestartPhase.NONE &&
        status != ConnectionStatus.CONNECTED &&
        !connectionError.isNullOrBlank()
    val statusColor = if (
        isAutoGenerating ||
        restartPhase != HomeAssistantRestartPhase.NONE ||
        (status == ConnectionStatus.CONNECTING && !showingError)
    ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = itemCornerShape(),
        color = appColors.surface.copy(alpha = .96f),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showingError) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                Text(
                    label,
                    color = statusColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                Text(stringResource(R.string.ui_home_assistant_c8fd3bb), color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.ui_connection_switched_to_8f04753, route.localizedName()),
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

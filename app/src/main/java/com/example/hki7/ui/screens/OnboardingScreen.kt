@file:Suppress("SetJavaScriptEnabled")

package com.example.hki7.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.hki7.data.HomeAssistantClient
import com.example.hki7.data.LocationWork
import com.example.hki7.data.PreferencesManager
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.launch
import java.net.URLEncoder

private enum class OnboardStep { WELCOME, SERVER, NAME, LOGIN, PERMISSIONS }

/**
 * First-run onboarding, modeled on the official Home Assistant app: welcome → auto-discover/enter the
 * server → OAuth login (which registers the device with the mobile_app integration right away) →
 * notification + location permission steps (location asks for the "Allow all the time" upgrade too).
 * Calls [onComplete] when finished so the host can show the main app.
 */
@Composable
fun OnboardingScreen(prefs: PreferencesManager, startAtLogin: Boolean = false, onComplete: () -> Unit) {
    // Re-login mode (e.g. session expired or the dashboard stopped connecting): the server is
    // already known, so jump straight to the login step and skip the rest of onboarding.
    val loadingSentinel = "__hki_loading__"
    val savedServerUrl by prefs.serverUrl.collectAsState(initial = loadingSentinel)
    if (startAtLogin && savedServerUrl == loadingSentinel) {
        Box(Modifier.fillMaxSize().background(LocalHKIAppColors.current.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    // The host latches startAtLogin for the lifetime of this onboarding run. Keep this state stable
    // too: the OAuth token save must not recreate the flow at LOGIN before it advances.
    val loginOnly = remember { startAtLogin && !savedServerUrl.isNullOrBlank() }
    var step by remember { mutableStateOf(if (loginOnly) OnboardStep.LOGIN else OnboardStep.WELCOME) }
    var serverUrl by remember {
        mutableStateOf(if (loginOnly) savedServerUrl.orEmpty().removeSuffix("/") else "")
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboard_step"
    ) { current ->
        when (current) {
            OnboardStep.WELCOME -> WelcomeStep(onNext = { step = OnboardStep.SERVER })
            OnboardStep.SERVER -> ServerStep(
                onBack = { step = OnboardStep.WELCOME },
                onServerChosen = { url -> serverUrl = url.removeSuffix("/"); step = OnboardStep.NAME }
            )
            OnboardStep.NAME -> NameStep(
                prefs = prefs,
                onBack = { step = OnboardStep.SERVER },
                onNext = { step = OnboardStep.LOGIN }
            )
            OnboardStep.LOGIN -> LoginStep(
                serverUrl = serverUrl,
                prefs = prefs,
                // Re-login still allows stepping back to pick a different server if needed.
                onBack = { step = if (loginOnly) OnboardStep.SERVER else OnboardStep.NAME },
                onLoggedIn = { if (loginOnly) onComplete() else step = OnboardStep.PERMISSIONS }
            )
            OnboardStep.PERMISSIONS -> PermissionsStep(onFinish = onComplete)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1: Welcome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        appColors.background
                    )
                )
            )
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Welcome to HKI 7", style = MaterialTheme.typography.displaySmall, color = appColors.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "A fast, native dashboard for your Home Assistant.\nLet's connect to your server.",
                style = MaterialTheme.typography.bodyLarge,
                color = appColors.onMuted,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2: Server discovery / manual entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServerStep(onBack: () -> Unit, onServerChosen: (String) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val discovered = rememberHaDiscovery(active = true)
    var manualUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Find your server", style = MaterialTheme.typography.headlineMedium, color = appColors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text("Home Assistant instances on your network appear automatically.", style = MaterialTheme.typography.bodyMedium, color = appColors.onMuted)

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Discovered", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            Spacer(Modifier.width(8.dp))
            if (discovered.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Scanning…", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (discovered.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No servers found yet. Make sure you're on the same Wi-Fi, or enter the address below.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.onMuted
                )
            }
        } else {
            discovered.forEach { server ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = appColors.subtleSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onServerChosen(server.baseUrl) }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(server.name, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
                            Text(server.baseUrl, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = appColors.onMuted)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Or enter manually", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = manualUrl,
            onValueChange = { manualUrl = it },
            label = { Text("Server URL") },
            placeholder = { Text("http://homeassistant.local:8123") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))
        Row {
            OutlinedButton(onClick = onBack, modifier = Modifier.height(52.dp)) { Text("Back") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (manualUrl.isNotBlank()) onServerChosen(manualUrl.trim()) },
                enabled = manualUrl.isNotBlank(),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Connect")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3: Device name (saved before login so it's used at registration)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NameStep(prefs: PreferencesManager, onBack: () -> Unit, onNext: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultName = remember {
        runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: android.os.Build.MODEL
    }
    var name by remember { mutableStateOf(defaultName) }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Name this device", style = MaterialTheme.typography.headlineMedium, color = appColors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "This is how the device appears in Home Assistant. It's set when the device registers, so pick something recognizable now.",
            style = MaterialTheme.typography.bodyMedium,
            color = appColors.onMuted
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Device name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.weight(1f))
        Row {
            OutlinedButton(onClick = onBack, modifier = Modifier.height(52.dp)) { Text("Back") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && !saving) {
                        saving = true
                        scope.launch { prefs.saveMobileDeviceName(name.trim()); onNext() }
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4: OAuth login (registers with mobile_app on success)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginStep(serverUrl: String, prefs: PreferencesManager, onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authInProgress by remember { mutableStateOf(false) }
    val authUrl = "${serverUrl.removeSuffix("/")}/auth/authorize?client_id=${URLEncoder.encode("https://home-assistant.io/android", "UTF-8")}&redirect_uri=${URLEncoder.encode("homeassistant://auth-callback", "UTF-8")}"

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: ""
                            if (url.startsWith("homeassistant://auth-callback")) {
                                val code = request?.url?.getQueryParameter("code")
                                val callbackError = request?.url?.getQueryParameter("error_description")
                                    ?: request?.url?.getQueryParameter("error")
                                if (callbackError != null) {
                                    errorMessage = "Login failed: $callbackError"
                                } else if (code != null && !authInProgress) {
                                    authInProgress = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            val response = HomeAssistantClient.getAccessToken(serverUrl, code)
                                            prefs.saveConnectionDetails(serverUrl, response.access_token, response.refresh_token, response.expires_in)
                                            // Register with the mobile_app integration immediately after auth
                                            // (like the official app) — reads prefs directly, so it doesn't
                                            // depend on location permission or the websocket being up yet.
                                            val appCtx = context.applicationContext
                                            LocationWork.schedule(appCtx)
                                            LocationWork.syncNow(appCtx)
                                            onLoggedIn()
                                        } catch (e: Exception) {
                                            errorMessage = "Login failed: ${e.message}"
                                            authInProgress = false
                                        }
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(authUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
        }
        errorMessage?.let { msg ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(msg, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4: Permissions (notifications + location, incl. "Allow all the time")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionsStep(onFinish: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current

    fun hasPerm(perm: String) = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    // Re-read permission state whenever a request returns or we come back from system settings.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    val notifGranted = remember(refresh) { hasPerm(Manifest.permission.POST_NOTIFICATIONS) }
    val fineGranted = remember(refresh) { hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION) }
    val backgroundGranted = remember(refresh) { hasPerm(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
    val batteryUnrestricted = remember(refresh) { powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refresh++
        // After foreground location is granted, immediately ask for the "Allow all the time" upgrade,
        // exactly like the official app's flow.
        if (result.values.any { it }) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    fun openAppSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
        }
    }

    fun requestBatteryUnrestricted() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }.onFailure {
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Finishing up", style = MaterialTheme.typography.headlineMedium, color = appColors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text("Grant a couple of permissions so HKI 7 can match the official app.", style = MaterialTheme.typography.bodyMedium, color = appColors.onMuted)

        Spacer(Modifier.height(24.dp))

        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Let Home Assistant send alerts and actionable notifications to this device.",
            granted = notifGranted,
            actionLabel = "Enable",
            onAction = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            icon = Icons.Default.LocationOn,
            title = "Location — Allow all the time",
            description = "Powers presence detection and zone automations. Choose \"Allow all the time\" so it works in the background, like the official app.",
            granted = fineGranted && backgroundGranted,
            actionLabel = when {
                !fineGranted -> "Enable location"
                !backgroundGranted -> "Set to Always"
                else -> "Enabled"
            },
            onAction = {
                when {
                    !fineGranted -> foregroundLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                    !backgroundGranted -> backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    else -> {}
                }
            },
            secondaryLabel = if (fineGranted && !backgroundGranted) "Open Settings" else null,
            onSecondary = { openAppSettings() }
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            icon = Icons.Default.BatterySaver,
            title = "Unrestricted background",
            description = "Lets battery, charging and presence keep updating reliably in the background — the official app asks for this too. Without it, Android delays updates while idle.",
            granted = batteryUnrestricted,
            actionLabel = "Allow",
            onAction = { requestBatteryUnrestricted() }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "You can change these anytime in Android Settings.",
            style = MaterialTheme.typography.labelSmall,
            color = appColors.onMuted
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (notifGranted && fineGranted && backgroundGranted) "Done" else "Continue")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = RoundedCornerShape(20.dp), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (granted) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (granted) Icons.Default.Check else icon,
                            contentDescription = null,
                            tint = if (granted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            if (!granted) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(actionLabel) }
                    if (secondaryLabel != null) {
                        Spacer(Modifier.width(10.dp))
                        TextButton(onClick = onSecondary) { Text(secondaryLabel) }
                    }
                }
            }
        }
    }
}

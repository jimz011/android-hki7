@file:Suppress("SetJavaScriptEnabled", "SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jimz011apps.hki7.data.HomeAssistantClient
import com.jimz011apps.hki7.data.HomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.LocationWork
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.PushForegroundService
import com.jimz011apps.hki7.data.classifyHomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.splitHomeAssistantConnectionUrl
import com.jimz011apps.hki7.ui.components.LocationDisclosureDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsHeader
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.net.URLEncoder

private enum class OnboardStep { WELCOME, SERVER, NAME, LOGIN, PERMISSIONS, CONNECTION, DASHBOARD }
private enum class AddInstanceStep { SERVER, LOGIN }

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
    val savedInternalUrl by prefs.internalUrl.collectAsState(initial = loadingSentinel)
    if (startAtLogin && (savedServerUrl == loadingSentinel || savedInternalUrl == loadingSentinel)) {
        Box(Modifier.fillMaxSize().background(LocalHKIAppColors.current.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    // The host latches startAtLogin for the lifetime of this onboarding run. Keep this state stable
    // too: the OAuth token save must not recreate the flow at LOGIN before it advances.
    val savedLoginUrl = savedServerUrl?.takeIf { it.isNotBlank() }
        ?: savedInternalUrl?.takeIf { it.isNotBlank() }
    val loginOnly = remember { startAtLogin && !savedLoginUrl.isNullOrBlank() }
    LaunchedEffect(loginOnly) {
        if (!loginOnly) prefs.prepareForInitialDashboardChoice()
    }
    var step by remember { mutableStateOf(if (loginOnly) OnboardStep.LOGIN else OnboardStep.WELCOME) }
    var serverUrl by remember {
        mutableStateOf(if (loginOnly) savedLoginUrl.orEmpty().removeSuffix("/") else "")
    }
    val scope = rememberCoroutineScope()
    var enteringDemo by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboard_step"
    ) { current ->
        when (current) {
            OnboardStep.WELCOME -> WelcomeStep(
                onNext = { step = OnboardStep.SERVER },
                onDemo = {
                    // Offline sample home (also what Google Play reviewers use). Configures the
                    // demo session plus an auto-generated dashboard, then jumps straight in.
                    if (!enteringDemo) {
                        enteringDemo = true
                        scope.launch {
                            prefs.enterDemoMode()
                            prefs.configureInitialDashboard(autoGenerate = true)
                            onComplete()
                        }
                    }
                }
            )
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
                initialConnection = !loginOnly,
                // Re-login still allows stepping back to pick a different server if needed.
                onBack = { step = if (loginOnly) OnboardStep.SERVER else OnboardStep.NAME },
                onLoggedIn = { if (loginOnly) onComplete() else step = OnboardStep.PERMISSIONS }
            )
            OnboardStep.PERMISSIONS -> PermissionsStep(onFinish = { step = OnboardStep.CONNECTION })
            OnboardStep.CONNECTION -> ConnectionInfoStep(serverUrl, onContinue = { step = OnboardStep.DASHBOARD })
            OnboardStep.DASHBOARD -> DashboardSetupStep(prefs, onComplete)
        }
    }
}

/** Reuses discovery and OAuth from first-run onboarding without disturbing the current instance.
 * The newly authenticated home becomes active and receives its own auto-generated dashboard. */
@Composable
fun AddHomeAssistantInstanceDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var step by remember { mutableStateOf(AddInstanceStep.SERVER) }
    var serverUrl by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        androidx.activity.compose.BackHandler {
            if (step == AddInstanceStep.LOGIN) step = AddInstanceStep.SERVER else onDismiss()
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "add_instance_step"
        ) { current ->
            when (current) {
                AddInstanceStep.SERVER -> ServerStep(
                    onBack = onDismiss,
                    onServerChosen = { chosen ->
                        serverUrl = chosen.removeSuffix("/")
                        step = AddInstanceStep.LOGIN
                    }
                )
                AddInstanceStep.LOGIN -> LoginStep(
                    serverUrl = serverUrl,
                    prefs = prefs,
                    initialConnection = false,
                    addingInstance = true,
                    onBack = { step = AddInstanceStep.SERVER },
                    onLoggedIn = onAdded
                )
            }
        }
    }
}

@Composable
private fun ConnectionInfoStep(serverUrl: String, onContinue: () -> Unit) {
    val colors = LocalHKIAppColors.current
    val connectionUrls = remember(serverUrl) { splitHomeAssistantConnectionUrl(serverUrl) }
    val route = remember(serverUrl, connectionUrls) {
        classifyHomeAssistantConnectionRoute(
            activeUrl = serverUrl,
            internalUrl = connectionUrls.internal,
            connectedViaLocalAddress = false
        )
    }
    val content = when (route) {
        HomeAssistantConnectionRoute.LOCAL -> ConnectionInfoContent(
            title = "Local Only",
            subtitle = "Connected through your home network",
            icon = Icons.Default.Wifi,
            paragraphs = listOf(
                "Your smart home is currently available to HKI 7 only while this device can reach your local Home Assistant network.",
                "To connect while you're away from home, add a Nabu Casa or external URL later in Settings > Connection."
            )
        )
        HomeAssistantConnectionRoute.NABU_CASA -> ConnectionInfoContent(
            title = "Remote access is ready.",
            subtitle = "Connected through Home Assistant Cloud",
            icon = Icons.Default.Cloud,
            paragraphs = listOf(
                "HKI 7 can reach your smart home both at home and while you're away through your Nabu Casa URL.",
                "For a faster direct connection at home, you can also add an internal URL and your home Wi-Fi networks later in Settings > Connection."
            )
        )
        HomeAssistantConnectionRoute.EXTERNAL -> ConnectionInfoContent(
            title = "Remote access is ready.",
            subtitle = "Connected through your external Home Assistant address",
            icon = Icons.Default.Public,
            paragraphs = listOf(
                "HKI 7 can reach your smart home both at home and while you're away through the external URL you entered.",
                "For a faster direct connection at home, you can also add an internal URL and your home Wi-Fi networks later in Settings > Connection."
            )
        )
    }

    OnboardingDialogFrame(
        title = content.title,
        subtitle = content.subtitle,
        icon = content.icon,
        footer = {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Got it", fontWeight = FontWeight.Bold) }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        content.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            content.paragraphs.forEachIndexed { index, paragraph ->
                if (index > 0) Spacer(Modifier.height(18.dp))
                Text(
                    paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class ConnectionInfoContent(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val paragraphs: List<String>
)

/** Keeps each setup step on the same fixed, modern surface as the rest of the app's dialogs. */
@Composable
private fun OnboardingDialogFrame(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: (() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalHKIAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        colors.background,
                        colors.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(32.dp),
            color = colors.elevated,
            contentColor = colors.onSurface,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                colors.elevated,
                                colors.elevated
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ModernSettingsHeader(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    canGoBack = onBack != null,
                    onBack = { onBack?.invoke() }
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
                footer?.let { footerContent ->
                    HorizontalDivider(color = colors.onMuted.copy(alpha = 0.22f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        footerContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSetupStep(prefs: PreferencesManager, onComplete: () -> Unit) {
    val colors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    var savingMode by remember { mutableStateOf<Boolean?>(null) }
    fun finish(auto: Boolean) {
        if (savingMode != null) return
        savingMode = auto
        scope.launch {
            prefs.configureInitialDashboard(auto)
            onComplete()
        }
    }
    OnboardingDialogFrame(
        title = "Choose your dashboard",
        subtitle = "Pick a starting point; everything remains editable",
        icon = Icons.Default.DashboardCustomize
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardChoiceCard(
                title = "Auto generate",
                subtitle = "Let HKI 7 create the first version for you.",
                icon = Icons.Default.AutoAwesome,
                recommended = true,
                bullets = listOf(
                    "Creates rooms and floors from Home Assistant areas",
                    "Finds suitable Climate, Security, Energy and Battery entities",
                    "Everything can be changed afterward in Edit mode"
                ),
                buttonText = if (savingMode == true) "Building dashboard…" else "Auto generate",
                enabled = savingMode == null,
                onClick = { finish(true) }
            )
            DashboardChoiceCard(
                title = "Start empty",
                subtitle = "Build the interface entirely your way.",
                icon = Icons.Default.DashboardCustomize,
                recommended = false,
                bullets = listOf(
                    "Starts without imported rooms, widgets or view entities",
                    "Add everything yourself using Edit mode"
                ),
                buttonText = if (savingMode == false) "Preparing dashboard…" else "Start empty",
                enabled = savingMode == null,
                onClick = { finish(false) }
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.subtleSurface
            ) {
                Text(
                    "Auto generation is a one-time starting point. The Home page starts empty in either mode, and you can re-import Home Assistant data later from individual views.",
                    modifier = Modifier.padding(14.dp),
                    color = colors.onMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DashboardChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    recommended: Boolean,
    bullets: List<String>,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (recommended) accent.copy(alpha = 0.10f) else colors.subtleSurface,
        border = if (recommended) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.32f)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = if (recommended) accent.copy(alpha = 0.20f) else colors.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        if (recommended) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = 0.18f)) {
                                Text(
                                    "RECOMMENDED",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            bullets.forEach { bullet ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(top = 1.dp).size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(bullet, style = MaterialTheme.typography.bodySmall, color = colors.onMuted, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            if (recommended) {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(buttonText, fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(buttonText, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1: Welcome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit, onDemo: () -> Unit) {
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
                "A fast, auto-generating dashboard for Home Assistant\nLet's connect to your server.",
                style = MaterialTheme.typography.bodyLarge,
                color = appColors.onMuted,
                textAlign = TextAlign.Center
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Get Started")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            TextButton(onClick = onDemo, modifier = Modifier.padding(top = 6.dp)) {
                Text("Try the demo home — no server needed")
            }
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

    OnboardingDialogFrame(
        title = "Find Home Assistant locally",
        subtitle = "We'll check your home network first; you can also enter an address",
        icon = Icons.Default.Home,
        onBack = onBack,
        footer = {
            Button(
                onClick = { if (manualUrl.isNotBlank()) onServerChosen(manualUrl.trim()) },
                enabled = manualUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Connect")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Discovered", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
                Spacer(Modifier.width(8.dp))
                if (discovered.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning…", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                }
            }

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

            Spacer(Modifier.height(8.dp))
            Text("Enter an address manually", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://homeassistant.local:8123") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
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

    OnboardingDialogFrame(
        title = "Name this device",
        subtitle = "Choose how this phone or tablet appears in Home Assistant",
        icon = Icons.Default.PhoneAndroid,
        onBack = onBack,
        footer = {
            Button(
                onClick = {
                    if (name.isNotBlank() && !saving) {
                        saving = true
                        scope.launch { prefs.saveMobileDeviceName(name.trim()); onNext() }
                    }
                },
                enabled = name.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (saving) "Saving…" else "Continue")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = appColors.subtleSurface,
                contentColor = appColors.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Text(
                        "Use a recognizable name, such as ‘Kitchen tablet’ or ‘Jimmy’s phone’.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.onMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Device name") },
                supportingText = { Text("This name is used when the device registers with Home Assistant.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4: OAuth login (registers with mobile_app on success)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginStep(
    serverUrl: String,
    prefs: PreferencesManager,
    initialConnection: Boolean,
    addingInstance: Boolean = false,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appColors = LocalHKIAppColors.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authInProgress by remember { mutableStateOf(false) }
    // Without these states, a slow or unreachable server (or a stale-session redirect) left the
    // WebView blank with only the close button visible — the "X instead of a login screen" bug.
    var pageLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val authUrl = "${serverUrl.removeSuffix("/")}/auth/authorize?client_id=${URLEncoder.encode("https://home-assistant.io/android", "UTF-8")}&redirect_uri=${URLEncoder.encode("homeassistant://auth-callback", "UTF-8")}"

    fun handleAuthCallback(rawUrl: String?): Boolean {
        val callbackUri = rawUrl?.let { runCatching { it.toUri() }.getOrNull() } ?: return false
        if (!callbackUri.scheme.equals("homeassistant", ignoreCase = true) ||
            !callbackUri.host.equals("auth-callback", ignoreCase = true)
        ) return false

        pageLoading = false
        val code = callbackUri.getQueryParameter("code")
        val callbackError = callbackUri.getQueryParameter("error_description")
            ?: callbackUri.getQueryParameter("error")
        if (callbackError != null) {
            errorMessage = "Login failed: $callbackError"
        } else if (code != null && !authInProgress) {
            authInProgress = true
            errorMessage = null
            scope.launch {
                try {
                    val response = HomeAssistantClient.getAccessToken(serverUrl, code)
                    if (addingInstance) {
                        prefs.addHomeAssistantInstance(
                            serverUrl,
                            response.access_token,
                            response.refresh_token,
                            response.expires_in
                        )
                    } else if (initialConnection) {
                        prefs.saveInitialConnectionDetails(serverUrl, response.access_token, response.refresh_token, response.expires_in)
                    } else {
                        prefs.saveConnectionDetails(serverUrl, response.access_token, response.refresh_token, response.expires_in)
                    }
                    // Register with the mobile_app integration immediately after auth (like the
                    // official app) — reads prefs directly, so it doesn't depend on location
                    // permission or the websocket being up yet.
                    val appCtx = context.applicationContext
                    LocationWork.schedule(appCtx)
                    LocationWork.syncNow(appCtx)
                    if (prefs.shouldUsePushService.first()) PushForegroundService.start(appCtx)
                    onLoggedIn()
                } catch (e: Exception) {
                    errorMessage = "Login failed: ${e.message}"
                    authInProgress = false
                }
            }
        } else if (code == null) {
            errorMessage = "Login failed: Home Assistant returned no authorization code."
        }
        return true
    }

    Box(modifier = Modifier.fillMaxSize().background(appColors.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return handleAuthCallback(request?.url?.toString())
                        }

                        @Deprecated("Deprecated WebView callback")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return handleAuthCallback(url)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            // Some WebView versions skip shouldOverrideUrlLoading for a custom
                            // scheme in a redirect chain. Catch it before HA treats the callback as
                            // a server URL and displays "Unable to fetch auth providers".
                            if (handleAuthCallback(url)) view?.stopLoading()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoading = false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                loadError = error?.description?.toString() ?: "Connection failed"
                                pageLoading = false
                            }
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                loadError = "The server answered with HTTP ${errorResponse?.statusCode}"
                                pageLoading = false
                            }
                        }
                    }
                    webViewRef = this
                    // A leftover frontend session cookie makes /auth/authorize silently hand out a
                    // new code without ever showing the credential form — after a logout that
                    // rendered as a blank page. Always start the login step with a clean session.
                    CookieManager.getInstance().removeAllCookies {
                        WebStorage.getInstance().deleteAllData()
                        loadUrl(authUrl)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                webViewRef = null
                it.stopLoading()
                it.loadUrl("about:blank")
                it.destroy()
            }
        )

        if (pageLoading && loadError == null && !authInProgress) {
            Column(
                modifier = Modifier.fillMaxSize().background(appColors.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Contacting your Home Assistant server…", color = appColors.onMuted)
                Spacer(Modifier.height(6.dp))
                Text(serverUrl, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            }
        }

        loadError?.let { message ->
            Column(
                modifier = Modifier.fillMaxSize().background(appColors.background).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Can't reach the server",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = appColors.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(serverUrl, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                Spacer(Modifier.height(12.dp))
                Text(message, textAlign = TextAlign.Center, color = appColors.onMuted)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        loadError = null
                        pageLoading = true
                        webViewRef?.loadUrl(authUrl)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Try again") }
                TextButton(onClick = onBack) { Text("Choose a different server") }
            }
        }

        if (authInProgress) {
            Column(
                modifier = Modifier.fillMaxSize().background(appColors.background.copy(alpha = 0.94f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Signing you in…", color = appColors.onMuted)
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
        }
        errorMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
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

    // Re-read permission state after a request returns and when we come back from system settings.
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
        // exactly like the official app's flow. The prominent disclosure was already accepted before
        // the foreground request, and it covers background collection.
        if (result.values.any { it }) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    // Google Play prominent disclosure: location permission requests may only launch after the user
    // accepts the disclosure dialog. Holds the request to run on "Agree".
    var pendingLocationRequest by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingLocationRequest?.let { request ->
        LocationDisclosureDialog(
            onAgree = {
                pendingLocationRequest = null
                request()
            },
            onDismiss = { pendingLocationRequest = null }
        )
    }

    fun openAppSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            )
        }
    }

    fun requestBatteryUnrestricted() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        }.onFailure {
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    val locationGranted = fineGranted && backgroundGranted
    val enabledCount = listOf(notifGranted, locationGranted, batteryUnrestricted).count { it }
    val permissionScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        appColors.background,
                        appColors.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(32.dp),
            color = appColors.elevated,
            contentColor = appColors.onSurface,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                appColors.elevated,
                                appColors.elevated
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ModernSettingsHeader(
                    title = "Permissions",
                    subtitle = "Enable the features HKI 7 may use in the background",
                    icon = Icons.Default.Notifications
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = appColors.subtleSurface,
                    contentColor = appColors.onSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Setup progress", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("$enabledCount of 3 enabled", style = MaterialTheme.typography.labelMedium, color = appColors.onMuted)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .background(appColors.onMuted.copy(alpha = 0.18f), CircleShape)
                        ) {
                            if (enabledCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(enabledCount / 3f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(permissionScroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Receive Home Assistant alerts and actionable notifications on this device.",
                        granted = notifGranted,
                        actionLabel = "Enable",
                        onAction = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )

                    PermissionCard(
                        icon = Icons.Default.LocationOn,
                        title = "Background location",
                        description = "Keeps presence detection and zone automations working. Android should show ‘Allow all the time’.",
                        granted = locationGranted,
                        actionLabel = when {
                            !fineGranted -> "Enable location"
                            !backgroundGranted -> "Allow all the time"
                            else -> "Enabled"
                        },
                        onAction = {
                            when {
                                !fineGranted -> pendingLocationRequest = {
                                    foregroundLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                                !backgroundGranted -> pendingLocationRequest = {
                                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                                else -> {}
                            }
                        },
                        secondaryLabel = if (fineGranted && !backgroundGranted) "Open Settings" else null,
                        onSecondary = { openAppSettings() }
                    )

                    PermissionCard(
                        icon = Icons.Default.BatterySaver,
                        title = "Unrestricted background",
                        description = "Prevents Android from delaying battery, charging, and presence updates while the device is idle.",
                        granted = batteryUnrestricted,
                        actionLabel = "Allow",
                        onAction = { requestBatteryUnrestricted() }
                    )

                    Text(
                        "These permissions are optional and can be changed later in Android Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.22f))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (notifGranted && locationGranted) "Done" else "Continue")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
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
    val successColor = Color(0xFF4CAF50)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = appColors.subtleSurface,
        contentColor = appColors.onSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (granted) successColor.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (granted) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (granted) Icons.Default.Check else icon,
                            contentDescription = null,
                            tint = if (granted) successColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = appColors.onSurface
                    )
                    Text(
                        if (granted) "Ready" else "Permission needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (granted) successColor else appColors.onMuted
                    )
                }
                if (granted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = successColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            "ENABLED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = successColor
                        )
                    }
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            if (!granted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (secondaryLabel != null) {
                        TextButton(onClick = onSecondary) { Text(secondaryLabel) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(actionLabel) }
                }
            }
        }
    }
}

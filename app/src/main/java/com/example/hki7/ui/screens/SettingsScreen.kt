package com.example.hki7.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.hki7.BuildConfig
import com.example.hki7.data.PreferencesManager
import com.example.hki7.data.PushForegroundService
import com.example.hki7.ui.ConnectionStatus
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.NavBarConfig
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.ColorWheel
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.launch

private enum class SettingsSection {
    MENU, CONNECTION, PROFILE, LOCATION, NOTIFICATIONS, APPEARANCE, THEME, NAV_BAR, DASHBOARD, ACCOUNT
}

private fun sectionTitle(section: SettingsSection): String = when (section) {
    SettingsSection.MENU -> "Settings"
    SettingsSection.NAV_BAR -> "Navigation Bar"
    SettingsSection.APPEARANCE -> "Appearance"
    else -> section.name.lowercase().replaceFirstChar { it.uppercase() }
}

// Sub-sections nested under Appearance return there on back; everything else returns to the menu.
private fun parentSection(section: SettingsSection): SettingsSection = when (section) {
    SettingsSection.THEME, SettingsSection.NAV_BAR -> SettingsSection.APPEARANCE
    else -> SettingsSection.MENU
}

@Composable
fun SettingsDialog(
    prefs: PreferencesManager,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val serverUrl by prefs.serverUrl.collectAsState(initial = "")
    val displayName by viewModel.displayName.collectAsState()
    val themeColor by prefs.themeColor.collectAsState(initial = "system")
    val themeMode by prefs.themeMode.collectAsState(initial = "system")
    val systemLightThemeColor by prefs.systemLightThemeColor.collectAsState(initial = "auto")
    val systemDarkThemeColor by prefs.systemDarkThemeColor.collectAsState(initial = "auto")
    val status by viewModel.status.collectAsState()
    val dashboardMode by viewModel.dashboardMode.collectAsState()
    val hasForegroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasBackgroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    val powerManager = context.getSystemService(android.os.PowerManager::class.java)
    val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
    val isBackgroundRestricted = activityManager?.isBackgroundRestricted ?: false

    var section by remember { mutableStateOf(SettingsSection.MENU) }
    var showNewConfigConfirm by remember { mutableStateOf(false) }
    var showTakeoverConfirm by remember { mutableStateOf(false) }
    var setupChangedMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.76f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SettingsHeader(
                    title = sectionTitle(section),
                    canGoBack = section != SettingsSection.MENU,
                    onBack = { section = parentSection(section) },
                    onDismiss = onDismiss
                )

                val contentScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fadingEdges(contentScroll)
                        .verticalScroll(contentScroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (section) {
                        SettingsSection.MENU -> {
                            SettingsChoice(Icons.Default.SettingsEthernet, "Connection", connectionText(status)) { section = SettingsSection.CONNECTION }
                            SettingsChoice(Icons.Default.Person, "Profile", displayName) { section = SettingsSection.PROFILE }
                            SettingsChoice(Icons.Default.MyLocation, "Location", "Device tracker and geocoded location") { section = SettingsSection.LOCATION }
                            SettingsChoice(Icons.Default.Notifications, "Notifications", "Push delivery and history") { section = SettingsSection.NOTIFICATIONS }
                            SettingsChoice(Icons.Default.Palette, "Appearance", "Theme and navigation bar") { section = SettingsSection.APPEARANCE }
                            SettingsChoice(Icons.Default.Dashboard, "Dashboard", dashboardMode.replaceFirstChar { it.uppercase() }) { section = SettingsSection.DASHBOARD }
                            SettingsChoice(Icons.AutoMirrored.Filled.Logout, "Account", "Logout and reset") { section = SettingsSection.ACCOUNT }
                        }
                        SettingsSection.CONNECTION -> {
                            val internalUrl by prefs.internalUrl.collectAsState(initial = null)
                            val homeSsids by prefs.homeSsids.collectAsState(initial = emptyList())
                            val currentSsid by viewModel.currentSsid.collectAsState()
                            var internalUrlInput by remember(internalUrl) { mutableStateOf(internalUrl.orEmpty()) }
                            var ssidsInput by remember(homeSsids) { mutableStateOf(homeSsids.joinToString(", ")) }
                            SettingsPanel {
                                val (icon, color, text) = when (status) {
                                    ConnectionStatus.CONNECTED -> Triple(Icons.Default.CheckCircle, Color(0xFF6AC36A), "Connected")
                                    ConnectionStatus.ERROR -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Error")
                                    else -> Triple(Icons.Default.Sync, Color.Gray, "Connecting...")
                                }
                                SettingsTile(icon, text, serverUrl ?: "", iconTint = color)
                                Button(
                                    onClick = { viewModel.refreshEntities() },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text("Refresh Connection") }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Local network (optional)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = appColors.onSurface
                                )
                                SettingsTile(
                                    Icons.Default.Wifi,
                                    "Current Wi-Fi",
                                    currentSsid ?: "Not connected / no location permission"
                                )
                                OutlinedTextField(
                                    value = internalUrlInput,
                                    onValueChange = { internalUrlInput = it },
                                    label = { Text("Internal URL") },
                                    placeholder = { Text("http://homeassistant.local:8123") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = ssidsInput,
                                    onValueChange = { ssidsInput = it },
                                    label = { Text("Home Wi-Fi names (comma separated)") },
                                    placeholder = { Text("MyWiFi, MyWiFi-5G") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                currentSsid?.let { ssid ->
                                    TextButton(onClick = {
                                        val updated = (ssidsInput.split(",").map { it.trim() }.filter { it.isNotBlank() } + ssid).distinct()
                                        ssidsInput = updated.joinToString(", ")
                                    }) { Text("Add current network \"$ssid\"") }
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            prefs.saveInternalUrl(internalUrlInput.ifBlank { null })
                                            prefs.saveHomeSsids(ssidsInput.split(",").map { it.trim() }.filter { it.isNotBlank() })
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text("Save Local Network") }
                                Text(
                                    "On these Wi-Fi networks the app connects via the internal URL; everywhere else it uses the main server URL above.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )
                            }
                        }
                        SettingsSection.PROFILE -> {
                            SettingsPanel {
                                SettingsTile(Icons.Default.Person, displayName, serverUrl ?: "")
                            }
                        }
                        SettingsSection.LOCATION -> {
                            val highAccuracy by prefs.highAccuracyLocation.collectAsState(initial = false)
                            SettingsPanel {
                                SettingsTile(Icons.Default.MyLocation, "Device Tracker", "Name used for location, geocoded location, battery and charging entities")
                                SettingsTile(
                                    icon = Icons.Default.MyLocation,
                                    title = "Android Location Permission",
                                    subtitle = when {
                                        hasBackgroundLocation -> "Allowed all the time"
                                        hasForegroundLocation -> "Allowed only while using the app"
                                        else -> "Not allowed"
                                    },
                                    iconTint = if (hasBackgroundLocation) Color(0xFF6AC36A) else Color.Gray
                                )
                                SettingsTile(
                                    icon = Icons.Default.BatterySaver,
                                    title = "Battery Optimization",
                                    subtitle = if (isIgnoringBatteryOptimizations) "Unrestricted (recommended)" else "Optimized — may interrupt background sync",
                                    iconTint = if (isIgnoringBatteryOptimizations) Color(0xFF6AC36A) else Color.Gray
                                )
                                SettingsTile(
                                    icon = Icons.Default.PhoneAndroid,
                                    title = "Background Usage",
                                    subtitle = if (!isBackgroundRestricted) "Unrestricted (recommended)" else "Restricted — background sync will not work",
                                    iconTint = if (!isBackgroundRestricted) Color(0xFF6AC36A) else Color.Gray
                                )
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Allow All The Time")
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                            )
                                        }.onFailure {
                                            context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.BatterySaver, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Disable Battery Optimization")
                                }
                                Button(
                                    onClick = { viewModel.reportDeviceTelemetry(context) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Sync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Update Location Now")
                                }
                                SettingsToggle(
                                    title = "High accuracy mode",
                                    subtitle = "Continuous GPS for live tracking — uses much more battery. Leave off for official-app-like behavior.",
                                    checked = highAccuracy,
                                    onCheckedChange = { scope.launch { prefs.saveHighAccuracyLocation(it) } }
                                )
                            }
                        }
                        SettingsSection.NOTIFICATIONS -> {
                            val backgroundPush by prefs.backgroundPushEnabled.collectAsState(initial = false)
                            SettingsPanel {
                                Text(
                                    "Notifications are delivered over the app's live connection whenever it is open — send them from Home Assistant with the notify service for this device. Swipe in from the left edge to see the history.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsToggle(
                                    title = "Background notifications",
                                    subtitle = "Keeps a persistent connection to Home Assistant while the app is closed (like the official app's persistent connection — uses more battery).",
                                    checked = backgroundPush,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            prefs.saveBackgroundPushEnabled(enabled)
                                            if (enabled) PushForegroundService.start(context)
                                            else PushForegroundService.stop(context)
                                        }
                                    }
                                )
                                if (backgroundPush) {
                                    // Android requires a visible notification for the connection
                                    // service, but the user may turn off just that channel — the
                                    // service keeps running with the notification fully hidden.
                                    Button(
                                        onClick = {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, PushForegroundService.CHANNEL_ID)
                                            }
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Hide Connection Notification")
                                    }
                                    Text(
                                        "Turn the \"Notification connection\" channel off on the next screen — the connection keeps working, only its notification disappears.",
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        SettingsSection.APPEARANCE -> {
                            SettingsChoice(Icons.Default.Palette, "Theme", "Colors and light/dark mode") { section = SettingsSection.THEME }
                            SettingsChoice(Icons.Default.Menu, "Navigation Bar", "Reorder and hide tabs") { section = SettingsSection.NAV_BAR }
                        }
                        SettingsSection.NAV_BAR -> {
                            val navBarOrder by prefs.navBarOrder.collectAsState(initial = emptyList())
                            val navBarHidden by prefs.navBarHidden.collectAsState(initial = emptyList())
                            val configurable = remember(navBarOrder) { NavBarConfig.orderedConfigurable(navBarOrder) }
                            val hiddenSet = navBarHidden.toSet()
                            SettingsPanel {
                                Text(
                                    "Home and Rooms are always shown. Reorder the other tabs with the arrows and toggle each on or off.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                NavBarConfig.fixed.forEach { screen ->
                                    NavTabRow(
                                        screen = screen,
                                        fixed = true,
                                        visible = true,
                                        canMoveUp = false,
                                        canMoveDown = false,
                                        onToggleVisible = {},
                                        onMoveUp = {},
                                        onMoveDown = {}
                                    )
                                }
                                configurable.forEachIndexed { index, screen ->
                                    NavTabRow(
                                        screen = screen,
                                        fixed = false,
                                        visible = screen.route !in hiddenSet,
                                        canMoveUp = index > 0,
                                        canMoveDown = index < configurable.lastIndex,
                                        onToggleVisible = {
                                            val newHidden = if (screen.route in hiddenSet) navBarHidden - screen.route
                                                            else navBarHidden + screen.route
                                            scope.launch { prefs.saveNavBarHidden(newHidden) }
                                        },
                                        onMoveUp = {
                                            val routes = configurable.map { it.route }.toMutableList()
                                            if (index > 0) {
                                                routes.add(index - 1, routes.removeAt(index))
                                                scope.launch { prefs.saveNavBarOrder(routes) }
                                            }
                                        },
                                        onMoveDown = {
                                            val routes = configurable.map { it.route }.toMutableList()
                                            if (index < routes.lastIndex) {
                                                routes.add(index + 1, routes.removeAt(index))
                                                scope.launch { prefs.saveNavBarOrder(routes) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        SettingsSection.THEME -> {
                            val forceHighRefresh by prefs.forceHighRefreshRate.collectAsState(initial = false)
                            SettingsPanel {
                                SettingsToggle(
                                    title = "Force high refresh rate",
                                    subtitle = "Locks the screen to its highest refresh rate while the app is open (uses more battery).",
                                    checked = forceHighRefresh,
                                    onCheckedChange = { scope.launch { prefs.saveForceHighRefreshRate(it) } }
                                )
                                val customRgb = remember(themeColor) { themeColorToRgb(themeColor) }
                                var localCustomRgb by remember(themeColor) { mutableStateOf(customRgb ?: listOf(155, 83, 83)) }
                                var customPickerOpen by remember(themeColor) { mutableStateOf(themeColor.startsWith("custom:")) }
                                Text("Mode", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                SettingsChipRow(
                                    options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                                    selected = themeMode,
                                    onSelect = { scope.launch { prefs.saveThemeMode(it) } }
                                )
                                Text("Color", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                SettingsChipRow(
                                    options = listOf("system" to "System", "rose" to "Rose", "green" to "Green", "blue" to "Blue", "amber" to "Amber", "custom" to "Custom"),
                                    selected = if (themeColor.startsWith("custom:")) "custom" else themeColor,
                                    onSelect = {
                                        customPickerOpen = it == "custom"
                                        scope.launch {
                                            prefs.saveThemeColor(if (it == "custom") rgbToThemeColor(localCustomRgb) else it)
                                        }
                                    }
                                )
                                if (customPickerOpen || themeColor.startsWith("custom:")) {
                                    ColorWheel(
                                        selectedRgb = localCustomRgb,
                                        onColorSelected = { rgb ->
                                            localCustomRgb = rgb
                                        },
                                        onValueChangeFinished = {
                                            scope.launch { prefs.saveThemeColor(rgbToThemeColor(localCustomRgb)) }
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                                if (themeColor == "system") {
                                    val lightRgb = remember(systemLightThemeColor) { themeColorToRgb(systemLightThemeColor) }
                                    var localLightRgb by remember(systemLightThemeColor) { mutableStateOf(lightRgb ?: listOf(155, 83, 83)) }
                                    var lightCustomOpen by remember(systemLightThemeColor) { mutableStateOf(systemLightThemeColor.startsWith("custom:")) }
                                    val darkRgb = remember(systemDarkThemeColor) { themeColorToRgb(systemDarkThemeColor) }
                                    var localDarkRgb by remember(systemDarkThemeColor) { mutableStateOf(darkRgb ?: listOf(155, 83, 83)) }
                                    var darkCustomOpen by remember(systemDarkThemeColor) { mutableStateOf(systemDarkThemeColor.startsWith("custom:")) }

                                    Text("System Light Theme", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                    SettingsChipRow(
                                        options = systemThemeOptions(),
                                        selected = if (systemLightThemeColor.startsWith("custom:")) "custom" else systemLightThemeColor,
                                        onSelect = {
                                            lightCustomOpen = it == "custom"
                                            scope.launch { prefs.saveSystemLightThemeColor(if (it == "custom") rgbToThemeColor(localLightRgb) else it) }
                                        }
                                    )
                                    if (lightCustomOpen || systemLightThemeColor.startsWith("custom:")) {
                                        ColorWheel(
                                            selectedRgb = localLightRgb,
                                            onColorSelected = { rgb ->
                                                localLightRgb = rgb
                                            },
                                            onValueChangeFinished = {
                                                scope.launch { prefs.saveSystemLightThemeColor(rgbToThemeColor(localLightRgb)) }
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }

                                    Text("System Dark Theme", color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                    SettingsChipRow(
                                        options = systemThemeOptions(),
                                        selected = if (systemDarkThemeColor.startsWith("custom:")) "custom" else systemDarkThemeColor,
                                        onSelect = {
                                            darkCustomOpen = it == "custom"
                                            scope.launch { prefs.saveSystemDarkThemeColor(if (it == "custom") rgbToThemeColor(localDarkRgb) else it) }
                                        }
                                    )
                                    if (darkCustomOpen || systemDarkThemeColor.startsWith("custom:")) {
                                        ColorWheel(
                                            selectedRgb = localDarkRgb,
                                            onColorSelected = { rgb ->
                                                localDarkRgb = rgb
                                            },
                                            onValueChangeFinished = {
                                                scope.launch { prefs.saveSystemDarkThemeColor(rgbToThemeColor(localDarkRgb)) }
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }
                        SettingsSection.DASHBOARD -> {
                            SettingsPanel {
                                SettingsTile(Icons.Default.Dashboard, "Mode", dashboardMode.replaceFirstChar { it.uppercase() })
                                OutlinedButton(
                                    onClick = { showTakeoverConfirm = true },
                                    enabled = dashboardMode == "auto",
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Lock, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Take Over Current")
                                }
                                Button(
                                    onClick = { showNewConfigConfirm = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Config")
                                }
                            }
                        }
                        SettingsSection.ACCOUNT -> {
                            SettingsPanel {
                                OutlinedButton(
                                    onClick = { viewModel.logout(keepConfig = true); onDismiss() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Logout (Keep Config)")
                                }
                                TextButton(
                                    onClick = { viewModel.logout(keepConfig = false); onDismiss() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reset Everything", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Text(
                    "Created by Jimz011 - 2026 • HKI 7 v${BuildConfig.VERSION_NAME}",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showNewConfigConfirm) {
        AlertDialog(
            onDismissRequest = { showNewConfigConfirm = false },
            title = { Text("Start new dashboard?") },
            text = { Text("This clears dashboard widgets, room order, room settings, and floor layout. Auto will import Home Assistant rooms and floors again. Manual starts empty and will not auto-import.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.startNewDashboard(auto = true)
                        showNewConfigConfirm = false
                        setupChangedMessage = "Dashboard changed to auto import."
                    }) { Text("Auto") }
                    Button(onClick = {
                        viewModel.startNewDashboard(auto = false)
                        showNewConfigConfirm = false
                        setupChangedMessage = "Dashboard changed to manual setup."
                    }) { Text("Manual") }
                }
            },
            dismissButton = { TextButton(onClick = { showNewConfigConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showTakeoverConfirm) {
        AlertDialog(
            onDismissRequest = { showTakeoverConfirm = false },
            title = { Text("Take over dashboard?") },
            text = { Text("This keeps the current dashboard exactly as it is and stops automatic Home Assistant room/floor imports until you explicitly start auto mode again.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.takeOverCurrentDashboard()
                    showTakeoverConfirm = false
                    setupChangedMessage = "Dashboard takeover complete. Auto import is now disabled."
                }) { Text("Take Over") }
            },
            dismissButton = { TextButton(onClick = { showTakeoverConfirm = false }) { Text("Cancel") } }
        )
    }

    setupChangedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { setupChangedMessage = null },
            title = { Text("Setup changed") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { setupChangedMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun SettingsHeader(title: String, canGoBack: Boolean, onBack: () -> Unit, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (canGoBack) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back")
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = appColors.onSurface) }
    }
}

@Composable
private fun SettingsChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
                Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NavTabRow(
    screen: Screen,
    fixed: Boolean,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleVisible: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = appColors.subtleSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                if (screen.mdiIcon != null) {
                    MdiIcon(name = screen.mdiIcon, tint = appColors.onSurface, size = 24.dp)
                } else {
                    Icon(screen.icon, null, tint = appColors.onSurface, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                screen.title,
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (fixed) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Fixed tab",
                    tint = appColors.onMuted,
                    modifier = Modifier.size(20.dp).padding(end = 12.dp)
                )
            } else {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) "Hide tab" else "Show tab",
                        tint = if (visible) MaterialTheme.colorScheme.primary else appColors.onMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = appColors.elevated.copy(alpha = 0.86f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SettingsTile(icon: ImageVector, title: String, subtitle: String, iconTint: Color = Color.White) {
    val appColors = LocalHKIAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (iconTint == Color.White) appColors.onSurface else iconTint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(options) { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

private fun connectionText(status: ConnectionStatus): String = when (status) {
    ConnectionStatus.CONNECTED -> "Connected"
    ConnectionStatus.ERROR -> "Error"
    else -> "Connecting..."
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalHKIAppColors.current.onSurface,
    unfocusedTextColor = LocalHKIAppColors.current.onSurface,
    focusedLabelColor = LocalHKIAppColors.current.onSurface.copy(alpha = 0.8f),
    unfocusedLabelColor = LocalHKIAppColors.current.onMuted,
    focusedPlaceholderColor = LocalHKIAppColors.current.onMuted,
    unfocusedPlaceholderColor = LocalHKIAppColors.current.onMuted,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = LocalHKIAppColors.current.onMuted
)

private fun themeColorToRgb(themeColor: String): List<Int>? {
    val hex = themeColor.substringAfter("custom:", missingDelimiterValue = "").removePrefix("#")
    if (hex.length != 6) return null
    return runCatching {
        listOf(
            hex.substring(0, 2).toInt(16),
            hex.substring(2, 4).toInt(16),
            hex.substring(4, 6).toInt(16)
        )
    }.getOrNull()
}

private fun rgbToThemeColor(rgb: List<Int>): String {
    val safe = List(3) { index -> rgb.getOrNull(index)?.coerceIn(0, 255) ?: 0 }
    return "custom:#%02X%02X%02X".format(safe[0], safe[1], safe[2])
}

private fun systemThemeOptions(): List<Pair<String, String>> = listOf(
    "auto" to "Auto",
    "rose" to "Rose",
    "green" to "Green",
    "blue" to "Blue",
    "amber" to "Amber",
    "custom" to "Custom"
)

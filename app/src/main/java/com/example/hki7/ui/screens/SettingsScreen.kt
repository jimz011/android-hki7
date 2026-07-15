package com.example.hki7.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.hki7.BuildConfig
import androidx.compose.ui.text.style.TextOverflow
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKICustomPage
import com.example.hki7.data.PreferencesManager
import com.example.hki7.data.PushForegroundService
import com.example.hki7.ui.components.RenameCardDialog
import com.example.hki7.ui.ConnectionStatus
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.NavBarConfig
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.ColorWheel
import com.example.hki7.ui.components.HKISlider
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.itemCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.theme.AppFontFamilyOptions
import com.example.hki7.ui.theme.appFontFamily
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.UUID

private enum class SettingsSection {
    MENU, CONNECTION, PROFILE, LOCATION, NOTIFICATIONS, APPEARANCE, THEME, FONTS, NAV_BAR, MEDIA_PLAYERS, DASHBOARD, BACKUP_RESTORE, ACCOUNT
}

private fun sectionTitle(section: SettingsSection): String = when (section) {
    SettingsSection.MENU -> "Settings"
    SettingsSection.NAV_BAR -> "Navigation Bar"
    SettingsSection.APPEARANCE -> "Appearance"
    SettingsSection.MEDIA_PLAYERS -> "Media Players"
    SettingsSection.BACKUP_RESTORE -> "Backup and Restore"
    else -> section.name.lowercase().replaceFirstChar { it.uppercase() }
}

// Sub-sections nested under Appearance return there on back; everything else returns to the menu.
private fun parentSection(section: SettingsSection): SettingsSection = when (section) {
    SettingsSection.THEME, SettingsSection.FONTS, SettingsSection.NAV_BAR, SettingsSection.MEDIA_PLAYERS -> SettingsSection.APPEARANCE
    SettingsSection.CONNECTION, SettingsSection.PROFILE, SettingsSection.LOCATION -> SettingsSection.ACCOUNT
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
    val dashboards by viewModel.dashboards.collectAsState()
    val activeDashboardId by viewModel.activeDashboardId.collectAsState()
    val defaultDashboardId by viewModel.defaultDashboardId.collectAsState()
    val hasForegroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasBackgroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    val powerManager = context.getSystemService(android.os.PowerManager::class.java)
    val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
    val isBackgroundRestricted = activityManager?.isBackgroundRestricted ?: false

    var section by remember { mutableStateOf(SettingsSection.MENU) }
    var showNewConfigConfirm by remember { mutableStateOf(false) }
    var newDashboardName by remember { mutableStateOf("") }
    var dashboardEditMode by remember { mutableStateOf(false) }
    var renameDashboard by remember { mutableStateOf<com.example.hki7.data.HKIDashboard?>(null) }
    var deleteDashboard by remember { mutableStateOf<com.example.hki7.data.HKIDashboard?>(null) }
    var setupChangedMessage by remember { mutableStateOf<String?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { prefs.saveProfileAvatar(it.toString()) }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(prefs.exportUiBackup()) }
                        ?: error("Could not open the selected file")
                }.onSuccess { setupChangedMessage = "Dashboard backup saved." }
                    .onFailure { error -> setupChangedMessage = "Backup failed: ${error.message}" }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val raw = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                        ?: error("Could not open the selected file")
                    prefs.restoreUiBackup(raw)
                }.onSuccess { setupChangedMessage = "Dashboard configuration restored." }
                    .onFailure { error -> setupChangedMessage = "Restore failed: ${error.message}" }
            }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        androidx.activity.compose.BackHandler {
            if (section == SettingsSection.MENU) onDismiss() else section = parentSection(section)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.76f),
            shape = itemCornerShape(),
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
                            SettingsChoice(Icons.Default.Person, "Account", displayName) { section = SettingsSection.ACCOUNT }
                            SettingsChoice(Icons.Default.Dashboard, "Dashboard", dashboardMode.replaceFirstChar { it.uppercase() }) { section = SettingsSection.DASHBOARD }
                            SettingsChoice(Icons.Default.Palette, "Appearance", "Theme and navigation bar") { section = SettingsSection.APPEARANCE }
                            SettingsChoice(Icons.Default.Notifications, "Notifications", "Push delivery and history") { section = SettingsSection.NOTIFICATIONS }
                            SettingsChoice(Icons.Default.Backup, "Backup and Restore", "Save or restore dashboard configuration") { section = SettingsSection.BACKUP_RESTORE }
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
                                    shape = itemCornerShape()
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
                                    shape = itemCornerShape()
                                ) { Text("Save Local Network") }
                                Text(
                                    "On these Wi-Fi networks the app connects via the internal URL; everywhere else it uses the main server URL above.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )
                            }
                        }
                        SettingsSection.PROFILE -> {
                            val avatar by prefs.profileAvatar.collectAsState(initial = null)
                            val birthday by prefs.profileBirthday.collectAsState(initial = null)
                            val profilePersonId by prefs.profilePersonEntityId.collectAsState(initial = null)
                            val people by viewModel.people.collectAsState()
                            var nameInput by remember(displayName) { mutableStateOf(displayName) }
                            var birthdayInput by remember(birthday) { mutableStateOf(birthday.orEmpty()) }
                            var personInput by remember(profilePersonId, people) { mutableStateOf(profilePersonId ?: people.singleOrNull()?.entity_id) }
                            var personMenuOpen by remember { mutableStateOf(false) }
                            SettingsPanel {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { personMenuOpen = true },
                                        shape = itemCornerShape(),
                                        color = appColors.subtleSurface
                                    ) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(people.find { it.entity_id == personInput }?.friendlyName ?: "Choose person entity", modifier = Modifier.weight(1f), color = appColors.onSurface)
                                            Icon(Icons.Default.KeyboardArrowDown, null, tint = appColors.onMuted)
                                        }
                                    }
                                    DropdownMenu(expanded = personMenuOpen, onDismissRequest = { personMenuOpen = false }) {
                                        people.forEach { person ->
                                            DropdownMenuItem(
                                                text = { Text(person.friendlyName ?: person.entity_id) },
                                                onClick = { personInput = person.entity_id; personMenuOpen = false }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = birthdayInput,
                                    onValueChange = { birthdayInput = it },
                                    label = { Text("Birthday (YYYY-MM-DD)") },
                                    leadingIcon = { Icon(Icons.Default.Cake, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Person, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (avatar == null) "Choose avatar image" else "Change avatar image")
                                }
                                if (avatar != null) {
                                    TextButton(onClick = { scope.launch { prefs.saveProfileAvatar(null) } }, modifier = Modifier.fillMaxWidth()) { Text("Remove avatar") }
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            prefs.saveDisplayName(nameInput.trim().ifBlank { "User" })
                                            prefs.saveProfileBirthday(birthdayInput.trim().ifBlank { null })
                                            prefs.saveProfilePersonEntityId(personInput)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Save profile") }
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
                                    shape = itemCornerShape()
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
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.BatterySaver, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Disable Battery Optimization")
                                }
                                Button(
                                    onClick = { viewModel.reportDeviceTelemetry(context) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
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
                                        shape = itemCornerShape()
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
                            SettingsChoice(Icons.Default.TextFields, "Fonts", "Text size, boldness and font family") { section = SettingsSection.FONTS }
                            SettingsChoice(Icons.Default.Menu, "Navigation Bar", "Reorder and hide tabs") { section = SettingsSection.NAV_BAR }
                            SettingsChoice(Icons.Default.MusicNote, "Media Players", "Rename players and mini player visibility") { section = SettingsSection.MEDIA_PLAYERS }
                        }
                        SettingsSection.FONTS -> {
                            val fontScale by prefs.fontScale.collectAsState(initial = 1f)
                            val fontWeightAdjust by prefs.fontWeightAdjust.collectAsState(initial = 0)
                            val fontFamily by prefs.fontFamily.collectAsState(initial = "default")
                            var localScale by remember(fontScale) { mutableStateOf(fontScale) }
                            var localWeight by remember(fontWeightAdjust) { mutableStateOf(fontWeightAdjust.toFloat()) }
                            SettingsPanel {
                                Text(
                                    "Font size · ${(localScale * 100).toInt()}%",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                HKISlider(
                                    value = localScale,
                                    onValueChange = { localScale = it },
                                    onValueChangeFinished = {
                                        // Snap to 5% steps so the label and stored value stay tidy.
                                        val snapped = (localScale * 20).roundToInt() / 20f
                                        localScale = snapped
                                        scope.launch { prefs.saveFontScale(snapped) }
                                    },
                                    valueRange = 0.8f..1.4f
                                )
                                val weightLabel = when (localWeight.roundToInt()) {
                                    -200 -> "Thinner (-200)"
                                    -100 -> "Thin (-100)"
                                    0 -> "Default"
                                    100 -> "Bold (+100)"
                                    200 -> "Bolder (+200)"
                                    else -> "Boldest (+300)"
                                }
                                Text(
                                    "Boldness · $weightLabel",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                HKISlider(
                                    value = localWeight,
                                    onValueChange = { localWeight = it },
                                    onValueChangeFinished = {
                                        val snapped = (localWeight / 100f).roundToInt() * 100
                                        localWeight = snapped.toFloat()
                                        scope.launch { prefs.saveFontWeightAdjust(snapped) }
                                    },
                                    valueRange = -200f..300f,
                                    steps = 4
                                )
                                Text("Font family", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                var familyMenuOpen by remember { mutableStateOf(false) }
                                val familyOptions = AppFontFamilyOptions
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { familyMenuOpen = true },
                                        shape = itemCornerShape(),
                                        color = appColors.subtleSurface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                familyOptions.firstOrNull { it.first == fontFamily }?.second ?: "Default",
                                                color = appColors.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(Icons.Default.KeyboardArrowDown, null, tint = appColors.onMuted)
                                        }
                                    }
                                    DropdownMenu(expanded = familyMenuOpen, onDismissRequest = { familyMenuOpen = false }) {
                                        familyOptions.forEach { (value, label) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        label,
                                                        fontFamily = appFontFamily(value),
                                                        fontWeight = if (value == fontFamily) FontWeight.Bold else null
                                                    )
                                                },
                                                onClick = {
                                                    familyMenuOpen = false
                                                    scope.launch { prefs.saveFontFamily(value) }
                                                }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "The quick brown fox jumps over the lazy dog — 0123456789",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        SettingsSection.MEDIA_PLAYERS -> {
                            val customNames by prefs.mediaPlayerCustomNames.collectAsState(initial = emptyMap())
                            val barHidden by prefs.mediaPlayerBarHidden.collectAsState(initial = emptyList())
                            val playersFlow = remember(viewModel) {
                                viewModel.entitiesMatching("domain:media_player") { it.entity_id.startsWith("media_player.") }
                            }
                            val players by playersFlow.collectAsState()
                            var renamingPlayer by remember { mutableStateOf<HAEntity?>(null) }
                            renamingPlayer?.let { player ->
                                RenameCardDialog(
                                    currentName = customNames[player.entity_id].orEmpty(),
                                    defaultName = player.friendlyName ?: player.entity_id,
                                    onDismiss = { renamingPlayer = null }
                                ) { name ->
                                    val names = if (name == null) customNames - player.entity_id
                                        else customNames + (player.entity_id to name)
                                    scope.launch { prefs.saveMediaPlayerCustomNames(names) }
                                    renamingPlayer = null
                                }
                            }
                            SettingsPanel {
                                Text(
                                    "Rename players and choose which ones may show the mini player above the navigation bar while playing.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val sorted = players.sortedBy { (customNames[it.entity_id] ?: it.friendlyName ?: it.entity_id).lowercase() }
                                if (sorted.isEmpty()) {
                                    Text("No media players found.", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                sorted.forEach { player ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                customNames[player.entity_id] ?: player.friendlyName ?: player.entity_id,
                                                color = appColors.onSurface,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                player.entity_id,
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(onClick = { renamingPlayer = player }) { Text("Rename") }
                                        Switch(
                                            checked = player.entity_id !in barHidden,
                                            onCheckedChange = { show ->
                                                val newHidden = if (show) barHidden - player.entity_id
                                                    else (barHidden + player.entity_id).distinct()
                                                scope.launch { prefs.saveMediaPlayerBarHidden(newHidden) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        SettingsSection.NAV_BAR -> {
                            val navBarOrder by prefs.navBarOrder.collectAsState(initial = emptyList())
                            val navBarHidden by prefs.navBarHidden.collectAsState(initial = emptyList())
                            val customPages by prefs.customPages.collectAsState(initial = emptyList())
                            val configurable = remember(navBarOrder, customPages) {
                                NavBarConfig.orderedConfigurable(navBarOrder, customPages)
                            }
                            val hiddenSet = navBarHidden.toSet()
                            var showPageEditor by remember { mutableStateOf(false) }
                            var editingPage by remember { mutableStateOf<HKICustomPage?>(null) }
                            SettingsPanel {
                                Text(
                                    "Home and Rooms are always shown. Reorder the other tabs with the arrows and toggle each on or off.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { editingPage = null; showPageEditor = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create custom page")
                                }
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
                                        },
                                        onEdit = if (screen is Screen.Custom) {
                                            { editingPage = screen.page; showPageEditor = true }
                                        } else null
                                    )
                                }
                            }
                            if (showPageEditor) {
                                CustomPageDialog(
                                    page = editingPage,
                                    onDismiss = { showPageEditor = false },
                                    onSave = { saved ->
                                        val updated = if (editingPage == null) customPages + saved
                                            else customPages.map { if (it.id == saved.id) saved else it }
                                        scope.launch { prefs.saveCustomPages(updated) }
                                        showPageEditor = false
                                    }
                                )
                            }
                        }
                        SettingsSection.THEME -> {
                            val forceHighRefresh by prefs.forceHighRefreshRate.collectAsState(initial = false)
                            val itemCornerRadius by prefs.itemCornerRadius.collectAsState(initial = 20)
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
                                Text("Item corner roundness", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Applies to all dashboard buttons, widgets, stacks, rooms, and cards.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(8 to "Sharp", 20 to "Modern", 28 to "Round").forEach { (radius, label) ->
                                        FilterChip(
                                            selected = itemCornerRadius == radius,
                                            onClick = { scope.launch { prefs.saveItemCornerRadius(radius) } },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                        SettingsSection.DASHBOARD -> {
                            SettingsPanel {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dashboards", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { dashboardEditMode = !dashboardEditMode }) {
                                        Icon(if (dashboardEditMode) Icons.Default.CheckCircle else Icons.Default.Edit, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (dashboardEditMode) "Done" else "Edit")
                                    }
                                }
                                dashboards.forEach { dashboard ->
                                    Surface(
                                        Modifier.fillMaxWidth().clickable(enabled = dashboard.id != activeDashboardId) { viewModel.switchDashboard(dashboard.id) },
                                        shape = itemCornerShape(),
                                        color = if (dashboard.id == activeDashboardId) MaterialTheme.colorScheme.primaryContainer else appColors.subtleSurface
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(dashboard.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                Text(if (dashboard.id == activeDashboardId) "Currently loaded" else "Tap to load", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = { viewModel.setDefaultDashboard(dashboard.id) }) {
                                                Icon(if (dashboard.id == defaultDashboardId) Icons.Default.Star else Icons.Default.StarBorder, "Set as default")
                                            }
                                            if (dashboardEditMode) {
                                                IconButton(onClick = { renameDashboard = dashboard }) { Icon(Icons.Default.Edit, "Rename") }
                                                IconButton(onClick = { deleteDashboard = dashboard }, enabled = dashboards.size > 1) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { newDashboardName = "Dashboard ${dashboards.size + 1}"; showNewConfigConfirm = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Dashboard")
                                }
                            }
                        }
                        SettingsSection.BACKUP_RESTORE -> {
                            SettingsPanel {
                                Text(
                                    "Backups contain dashboard and appearance configuration only. Connection details, app permissions, location state, and notification history are not included.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { backupLauncher.launch("hki7-dashboard-backup.json") },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Backup, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Backup")
                                }
                                OutlinedButton(
                                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Sync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restore")
                                }
                            }
                        }
                        SettingsSection.ACCOUNT -> {
                            SettingsChoice(Icons.Default.Person, "Profile", displayName) { section = SettingsSection.PROFILE }
                            SettingsChoice(Icons.Default.SettingsEthernet, "Connection", connectionText(status)) { section = SettingsSection.CONNECTION }
                            SettingsChoice(Icons.Default.MyLocation, "Location", "Device tracker and geocoded location") { section = SettingsSection.LOCATION }
                            SettingsPanel {
                                OutlinedButton(
                                    onClick = { viewModel.logout(keepConfig = true); onDismiss() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = itemCornerShape(),
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
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create a separate dashboard. Auto Generate imports once and then becomes editable; Start Empty only keeps persons available.")
                    OutlinedTextField(newDashboardName, { newDashboardName = it }, label = { Text("Dashboard name") }, singleLine = true)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.createDashboard(newDashboardName, auto = true)
                        showNewConfigConfirm = false
                        setupChangedMessage = "New dashboard is being auto generated."
                    }) { Text("Auto Generate") }
                    Button(onClick = {
                        viewModel.createDashboard(newDashboardName, auto = false)
                        showNewConfigConfirm = false
                        setupChangedMessage = "Created an empty dashboard."
                    }) { Text("Start Empty") }
                }
            },
            dismissButton = { TextButton(onClick = { showNewConfigConfirm = false }) { Text("Cancel") } }
        )
    }

    renameDashboard?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { renameDashboard = null },
            title = { Text("Rename dashboard") },
            text = { OutlinedTextField(newDashboardName, { newDashboardName = it }, label = { Text("Name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameDashboard(dashboard.id, newDashboardName)
                    renameDashboard = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameDashboard = null }) { Text("Cancel") } }
        )
    }

    LaunchedEffect(renameDashboard?.id) { renameDashboard?.let { newDashboardName = it.name } }

    deleteDashboard?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { deleteDashboard = null },
            title = { Text("Delete ${dashboard.name}?") },
            text = { Text("This permanently removes this dashboard and its room and page configuration.") },
            confirmButton = { Button(onClick = { viewModel.deleteDashboard(dashboard.id); deleteDashboard = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteDashboard = null }) { Text("Cancel") } }
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
        shape = itemCornerShape(),
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
private fun CustomPageDialog(
    page: HKICustomPage?,
    onDismiss: () -> Unit,
    onSave: (HKICustomPage) -> Unit
) {
    var name by remember(page) { mutableStateOf(page?.name.orEmpty()) }
    var subtitle by remember(page) { mutableStateOf(page?.subtitle.orEmpty()) }
    var icon by remember(page) { mutableStateOf(page?.icon ?: "view-dashboard") }
    var showIconPicker by remember { mutableStateOf(false) }
    val appColors = LocalHKIAppColors.current

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = icon,
            onDismiss = { showIconPicker = false },
            onSelect = { selected ->
                icon = selected.ifBlank { "view-dashboard" }
                showIconPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (page == null) "Create custom page" else "Edit custom page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Page name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Page subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MdiIcon(name = icon, tint = appColors.onSurface, size = 26.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Page icon", modifier = Modifier.weight(1f), color = appColors.onSurface)
                        TextButton(onClick = { showIconPicker = true }) { Text("Change") }
                    }
                }
                Text(
                    "The page starts empty with its own header and page settings, while keeping the Home widget catalogue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.onMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        HKICustomPage(
                            id = page?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            subtitle = subtitle.trim(),
                            icon = icon
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
    onMoveDown: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemCornerShape(),
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
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit page", tint = appColors.onSurface)
                    }
                }
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
        shape = itemCornerShape(),
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

@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.R
import androidx.compose.ui.text.style.TextOverflow
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.HomeAssistantInstance
import com.jimz011apps.hki7.data.CloudBackupStorage
import com.jimz011apps.hki7.data.CloudBackupFile
import com.jimz011apps.hki7.data.CloudBackupWork
import com.jimz011apps.hki7.data.driveAuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.jimz011apps.hki7.data.HKICustomPage
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.PushForegroundService
import com.jimz011apps.hki7.data.LocationWork
import com.jimz011apps.hki7.ui.components.RenameCardDialog
import com.jimz011apps.hki7.ui.ConnectionStatus
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.NavBarConfig
import com.jimz011apps.hki7.ui.Screen
import com.jimz011apps.hki7.ui.components.ColorWheel
import com.jimz011apps.hki7.ui.components.HKISlider
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsHeader
import com.jimz011apps.hki7.ui.components.ModernSettingsMenuItem
import com.jimz011apps.hki7.ui.components.SettingsGroup
import com.jimz011apps.hki7.ui.components.SettingsChoiceChip
import com.jimz011apps.hki7.ui.components.SettingsSubcategory
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.theme.AppFontFamilyOptions
import com.jimz011apps.hki7.ui.theme.appFontFamily
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import java.util.UUID
import coil3.compose.AsyncImage

private enum class SettingsSection {
    MENU, CONNECTION, PROFILE, LOCATION, NOTIFICATIONS, APPEARANCE, HEADER, THEME, FONTS, NAV_BAR, MEDIA_PLAYERS, DASHBOARD, BACKUP_RESTORE, ACCOUNT, ABOUT, LICENSE, SUPPORT
}

private fun sectionTitle(section: SettingsSection): String = when (section) {
    SettingsSection.MENU -> "Settings"
    SettingsSection.NAV_BAR -> "Navigation Bar"
    SettingsSection.APPEARANCE -> "Appearance"
    SettingsSection.HEADER -> "Header"
    SettingsSection.MEDIA_PLAYERS -> "Media Players"
    SettingsSection.BACKUP_RESTORE -> "Backup and Restore"
    SettingsSection.ABOUT -> "About HKI 7"
    SettingsSection.LICENSE -> "License"
    SettingsSection.SUPPORT -> "Support"
    else -> section.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun sectionSubtitle(section: SettingsSection): String = when (section) {
    SettingsSection.MENU -> "Everything that shapes your HKI 7 experience"
    SettingsSection.ACCOUNT -> "Your profile and this app installation"
    SettingsSection.PROFILE -> "Personal details used throughout your dashboard"
    SettingsSection.CONNECTION -> "Server routes and local-network preferences"
    SettingsSection.LOCATION -> "Presence updates and background access"
    SettingsSection.DASHBOARD -> "Create, switch, rename, and organize dashboards"
    SettingsSection.APPEARANCE -> "Make the dashboard feel like yours"
    SettingsSection.HEADER -> "Choose whether the dashboard header is shown"
    SettingsSection.THEME -> "Color, contrast, mode, and corner styling"
    SettingsSection.FONTS -> "Size, weight, and typeface readability"
    SettingsSection.NAV_BAR -> "Choose which destinations are always within reach"
    SettingsSection.MEDIA_PLAYERS -> "Names and mini-player visibility"
    SettingsSection.NOTIFICATIONS -> "Push delivery, history, and service behavior"
    SettingsSection.BACKUP_RESTORE -> "Protect or move your dashboard configuration"
    SettingsSection.ABOUT -> "The project, technology, and people behind HKI 7"
    SettingsSection.LICENSE -> "Open-source community core and optional premium materials"
    SettingsSection.SUPPORT -> "Ways to help HKI 7 grow without buying Premium"
}

private fun sectionIcon(section: SettingsSection): ImageVector = when (section) {
    SettingsSection.MENU -> Icons.Default.SettingsEthernet
    SettingsSection.ACCOUNT, SettingsSection.PROFILE -> Icons.Default.Person
    SettingsSection.CONNECTION -> Icons.Default.SettingsEthernet
    SettingsSection.LOCATION -> Icons.Default.MyLocation
    SettingsSection.DASHBOARD -> Icons.Default.Dashboard
    SettingsSection.APPEARANCE, SettingsSection.THEME -> Icons.Default.Palette
    SettingsSection.HEADER -> Icons.Default.Tune
    SettingsSection.FONTS -> Icons.Default.TextFields
    SettingsSection.NAV_BAR -> Icons.Default.Menu
    SettingsSection.MEDIA_PLAYERS -> Icons.Default.MusicNote
    SettingsSection.NOTIFICATIONS -> Icons.Default.Notifications
    SettingsSection.BACKUP_RESTORE -> Icons.Default.Backup
    SettingsSection.ABOUT -> Icons.Default.Info
    SettingsSection.LICENSE -> Icons.Default.Description
    SettingsSection.SUPPORT -> Icons.Default.Favorite
}

// Subsections nested under Appearance return there on back; everything else returns to the menu.
private fun parentSection(section: SettingsSection): SettingsSection = when (section) {
    SettingsSection.HEADER, SettingsSection.THEME, SettingsSection.FONTS, SettingsSection.NAV_BAR, SettingsSection.MEDIA_PLAYERS -> SettingsSection.APPEARANCE
    SettingsSection.PROFILE -> SettingsSection.ACCOUNT
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
    val internalUrl by prefs.internalUrl.collectAsState(initial = null)
    val currentUrl by viewModel.currentUrl.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val themeColor by prefs.themeColor.collectAsState(initial = "system")
    val themeMode by prefs.themeMode.collectAsState(initial = "system")
    val systemLightThemeColor by prefs.systemLightThemeColor.collectAsState(initial = "auto")
    val systemDarkThemeColor by prefs.systemDarkThemeColor.collectAsState(initial = "auto")
    val status by viewModel.status.collectAsState()
    val currentConnectionRoute by viewModel.connectionRoute.collectAsState()
    val dashboardMode by viewModel.dashboardMode.collectAsState()
    val dashboards by viewModel.dashboards.collectAsState()
    val activeDashboardId by viewModel.activeDashboardId.collectAsState()
    val defaultDashboardId by viewModel.defaultDashboardId.collectAsState()
    val homeAssistantInstances by prefs.homeAssistantInstances.collectAsState(initial = emptyList())
    val activeHomeAssistantInstanceId by prefs.activeHomeAssistantInstanceId.collectAsState(initial = null)
    val cloudBackupEnabled by prefs.cloudBackupEnabled.collectAsState(initial = false)
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
    var renameDashboard by remember { mutableStateOf<com.jimz011apps.hki7.data.HKIDashboard?>(null) }
    var deleteDashboard by remember { mutableStateOf<com.jimz011apps.hki7.data.HKIDashboard?>(null) }
    var setupChangedMessage by remember { mutableStateOf<String?>(null) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var restartBusy by remember { mutableStateOf(false) }
    var homeAssistantMessage by remember { mutableStateOf<String?>(null) }
    var showAddHomeAssistantInstance by remember { mutableStateOf(false) }
    var renameHomeAssistantInstance by remember { mutableStateOf<HomeAssistantInstance?>(null) }
    var deleteHomeAssistantInstance by remember { mutableStateOf<HomeAssistantInstance?>(null) }
    var homeAssistantInstanceName by remember { mutableStateOf("") }
    var showRestoreSource by remember { mutableStateOf(false) }
    var showCloudRestore by remember { mutableStateOf(false) }
    var cloudRestoreFiles by remember { mutableStateOf(emptyList<CloudBackupFile>()) }
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
    val enableCloudBackup = {
        scope.launch {
            prefs.saveCloudBackup(true)
            CloudBackupWork.schedule(context)
            setupChangedMessage = "Automatic cloud backup enabled."
        }
    }
    val driveAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorization ->
                if (authorization.accessToken != null) enableCloudBackup()
                else setupChangedMessage = "Google Drive authorization did not return an access token."
            }.onFailure { setupChangedMessage = "Google Drive authorization failed: ${it.message}" }
        } else {
            setupChangedMessage = "Google Drive authorization was cancelled."
        }
    }
    val requestDriveAuthorization = {
        Identity.getAuthorizationClient(context).authorize(driveAuthorizationRequest())
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) {
                        driveAuthorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else setupChangedMessage = "Google Drive authorization could not be opened."
                } else if (authorization.accessToken != null) {
                    enableCloudBackup()
                } else setupChangedMessage = "Google Drive authorization did not return an access token."
            }
            .addOnFailureListener { setupChangedMessage = "Google Drive authorization failed: ${it.message}" }
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
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = appColors.elevated,
                contentColor = appColors.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                appColors.elevated,
                                appColors.elevated
                            )
                        )
                    )
                    .padding(24.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SettingsHeader(
                    title = sectionTitle(section),
                    subtitle = sectionSubtitle(section),
                    icon = sectionIcon(section),
                    canGoBack = section != SettingsSection.MENU,
                    onBack = { section = parentSection(section) },
                    onDismiss = onDismiss
                )

                val contentScroll = rememberScrollState()
                LaunchedEffect(section) { contentScroll.scrollTo(0) }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fadingEdges(contentScroll)
                        .verticalScroll(contentScroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (section) {
                        SettingsSection.MENU -> {
                            SettingsSubcategory("Your home", "Identity, connection, and dashboard management")
                            SettingsChoice(Icons.Default.Person, "Account", displayName) { section = SettingsSection.ACCOUNT }
                            SettingsChoice(Icons.Default.SettingsEthernet, "Connection", connectionText(status, currentConnectionRoute)) { section = SettingsSection.CONNECTION }
                            SettingsChoice(Icons.Default.MyLocation, "Location", "Device tracker and geocoded location") { section = SettingsSection.LOCATION }
                            SettingsChoice(Icons.Default.Dashboard, "Dashboard", dashboardMode.replaceFirstChar { it.uppercase() }) { section = SettingsSection.DASHBOARD }
                            SettingsSubcategory("Personalize", "Visual style and everyday navigation")
                            SettingsChoice(Icons.Default.Palette, "Appearance", "Theme and navigation bar") { section = SettingsSection.APPEARANCE }
                            SettingsSubcategory("Services & data", "Messages, safety, and portability")
                            SettingsChoice(Icons.Default.Notifications, "Notifications", "Push delivery and history") { section = SettingsSection.NOTIFICATIONS }
                            SettingsChoice(Icons.Default.Backup, "Backup and Restore", "Save or restore dashboard configuration") { section = SettingsSection.BACKUP_RESTORE }
                            SettingsSubcategory("HKI 7", "Project information, licensing, and community support")
                            SettingsChoice(Icons.Default.Info, "About", "What HKI 7 is and how it is built") { section = SettingsSection.ABOUT }
                            SettingsChoice(Icons.Default.Description, "License", "Open-source and premium licensing") { section = SettingsSection.LICENSE }
                            SettingsChoice(Icons.Default.Favorite, "Support", "Help the project without buying Premium") { section = SettingsSection.SUPPORT }
                        }
                        SettingsSection.CONNECTION -> {
                            val homeSsids by prefs.homeSsids.collectAsState(initial = emptyList())
                            val currentSsid by viewModel.currentSsid.collectAsState()
                            var externalUrlInput by remember(serverUrl) { mutableStateOf(serverUrl.orEmpty()) }
                            var internalUrlInput by remember(internalUrl) { mutableStateOf(internalUrl.orEmpty()) }
                            var ssidsInput by remember(homeSsids) { mutableStateOf(homeSsids.joinToString(", ")) }
                            SettingsSubcategory(
                                "Home Assistant instances",
                                "Each home keeps its own login, dashboard, notification connection, and location registration"
                            )
                            SettingsPanel {
                                homeAssistantInstances.forEach { instance ->
                                    val isActive = instance.id == activeHomeAssistantInstanceId
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!isActive) viewModel.switchHomeAssistantInstance(instance.id)
                                            },
                                        shape = itemCornerShape(),
                                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else appColors.subtleSurface
                                    ) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.SettingsEthernet,
                                                    contentDescription = null,
                                                    tint = if (isActive) MaterialTheme.colorScheme.primary else appColors.onMuted
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        instance.name,
                                                        color = appColors.onSurface,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        instance.primaryUrl.orEmpty(),
                                                        color = appColors.onMuted,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (isActive) {
                                                    Icon(Icons.Default.CheckCircle, "Active instance", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = {
                                                    homeAssistantInstanceName = instance.name
                                                    renameHomeAssistantInstance = instance
                                                }) {
                                                    Icon(Icons.Default.Edit, "Rename ${instance.name}", tint = appColors.onMuted)
                                                }
                                            }
                                            InstanceCapabilityToggle(
                                                title = "Notifications",
                                                checked = instance.notificationsEnabled,
                                                onCheckedChange = { enabled ->
                                                    scope.launch {
                                                        prefs.setHomeAssistantInstanceCapabilities(instance.id, notificationsEnabled = enabled)
                                                        if (enabled) LocationWork.schedule(context)
                                                        LocationWork.syncNow(context)
                                                        if (prefs.shouldUsePushService.first()) PushForegroundService.start(context)
                                                    }
                                                }
                                            )
                                            InstanceCapabilityToggle(
                                                title = "Location",
                                                checked = instance.locationEnabled,
                                                onCheckedChange = { enabled ->
                                                    scope.launch {
                                                        prefs.setHomeAssistantInstanceCapabilities(instance.id, locationEnabled = enabled)
                                                        if (enabled) LocationWork.schedule(context)
                                                        LocationWork.syncNow(context)
                                                    }
                                                }
                                            )
                                            if (homeAssistantInstances.size > 1) {
                                                TextButton(
                                                    onClick = { deleteHomeAssistantInstance = instance },
                                                    modifier = Modifier.align(Alignment.End)
                                                ) {
                                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(17.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Remove")
                                                }
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { showAddHomeAssistantInstance = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Home Assistant instance")
                                }
                                Text(
                                    "Swipe left from the upper-right edge of a page header to switch homes at any time.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )
                            }
                            SettingsSubcategory("Active connection", "Network routes for ${homeAssistantInstances.firstOrNull { it.id == activeHomeAssistantInstanceId }?.name ?: "this home"}")
                            SettingsPanel {
                                val (icon, color, text) = when (status) {
                                    ConnectionStatus.CONNECTED -> Triple(
                                        Icons.Default.CheckCircle,
                                        Color(0xFF6AC36A),
                                        "Connected via ${currentConnectionRoute?.displayName ?: "Unknown"}"
                                    )
                                    ConnectionStatus.ERROR -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Error")
                                    else -> Triple(Icons.Default.Sync, Color.Gray, "Connecting...")
                                }
                                SettingsTile(icon, text, currentUrl.ifBlank { serverUrl ?: "" }, iconTint = color)
                                Button(
                                    onClick = { viewModel.refreshEntities() },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) { Text("Refresh Connection") }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Remote access (optional)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = appColors.onSurface
                                )
                                OutlinedTextField(
                                    value = externalUrlInput,
                                    onValueChange = { externalUrlInput = it },
                                    label = { Text("External URL or Nabu Casa URL") },
                                    placeholder = { Text("https://example.ui.nabu.casa") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                Button(
                                    onClick = {
                                        scope.launch { prefs.saveExternalUrl(externalUrlInput.ifBlank { null }) }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) { Text("Save Remote Access") }
                                Text(
                                    "Leave this empty for local-only access. Add an external or Nabu Casa URL to use HKI 7 away from home.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )

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
                            SettingsSubcategory("Maintenance", "Administrative controls for your Home Assistant server")
                            SettingsPanel {
                                OutlinedButton(
                                    onClick = { showRestartConfirm = true },
                                    enabled = status == ConnectionStatus.CONNECTED,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.PowerSettingsNew, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restart Home Assistant")
                                }
                                Text(
                                    "Home Assistant will be unavailable briefly while it restarts. Administrator access is required.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.labelSmall
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
                                                data = "package:${context.packageName}".toUri()
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
                                                Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
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
                            val multiInstancePush = homeAssistantInstances.size > 1 &&
                                homeAssistantInstances.any { it.notificationsEnabled && it.isAuthenticated }
                            SettingsPanel {
                                Text(
                                    "Notifications are delivered over the app's live connection whenever it is open — send them from Home Assistant with the notify service for this device. Swipe in from the left edge to see the history.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsToggle(
                                    title = "Background notifications",
                                    subtitle = if (multiInstancePush) {
                                        "Required while multiple homes receive notifications; manage each home under Connection."
                                    } else {
                                        "Keeps a persistent connection to Home Assistant while the app is closed (uses more battery)."
                                    },
                                    checked = backgroundPush || multiInstancePush,
                                    enabled = !multiInstancePush,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            prefs.saveBackgroundPushEnabled(enabled)
                                            if (enabled) PushForegroundService.start(context)
                                            else PushForegroundService.stop(context)
                                        }
                                    }
                                )
                                if (backgroundPush || multiInstancePush) {
                                    // Android requires a visible notification for the connection
                                    // service, but the user may turn off just that channel — the
                                    // service keeps running with the notification fully hidden.
                                    Button(
                                        onClick = {
                                            val intent = Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                                putExtra(AndroidSettings.EXTRA_CHANNEL_ID, PushForegroundService.CHANNEL_ID)
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
                            SettingsSubcategory("Visual style", "Color, typography, and component shape")
                            SettingsChoice(Icons.Default.Palette, "Theme", "Colors and light/dark mode") { section = SettingsSection.THEME }
                            SettingsChoice(Icons.Default.TextFields, "Fonts", "Text size, boldness and font family") { section = SettingsSection.FONTS }
                            SettingsChoice(Icons.Default.Tune, "Header", "Choose an expanded or compact dashboard header") { section = SettingsSection.HEADER }
                            SettingsSubcategory("Everyday navigation", "Tabs and media controls shown throughout the app")
                            SettingsChoice(Icons.Default.Menu, "Navigation Bar", "Reorder and hide tabs") { section = SettingsSection.NAV_BAR }
                            SettingsChoice(Icons.Default.MusicNote, "Media Players", "Rename players and mini player visibility") { section = SettingsSection.MEDIA_PLAYERS }
                        }
                        SettingsSection.HEADER -> {
                            val headerVisible by prefs.headerVisible.collectAsState(initial = true)
                            SettingsSubcategory("Dashboard header", "Choose between the full header and a compact navigation bar")
                            SettingsPanel {
                                SettingsToggle(
                                    title = "Compact header",
                                    subtitle = "Keep only the title, right header pill, and back button when a page has one.",
                                    checked = !headerVisible,
                                    onCheckedChange = { compact -> scope.launch { prefs.saveHeaderVisible(!compact) } }
                                )
                                Text(
                                    "Compact mode hides the left header pill, persons, subtitle/status information, and room counters. Swipe down on the compact bar to open Search, Flows, Edit, and Settings.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        SettingsSection.FONTS -> {
                            val fontScale by prefs.fontScale.collectAsState(initial = 1f)
                            val fontWeightAdjust by prefs.fontWeightAdjust.collectAsState(initial = 0)
                            val fontFamily by prefs.fontFamily.collectAsState(initial = "default")
                            var localScale by remember(fontScale) { mutableFloatStateOf(fontScale) }
                            var localWeight by remember(fontWeightAdjust) { mutableFloatStateOf(fontWeightAdjust.toFloat()) }
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
                                        SettingsChoiceChip(
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
                                    Text("Dashboards", color = appColors.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                                Text("On this device", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
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
                                    onClick = { showRestoreSource = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Sync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restore")
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Automatic cloud backup", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Uses HKI 7's private Google Drive storage. Enable it once to create an immediate backup and then back up automatically every day.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Enable cloud backup", color = appColors.onSurface)
                                        Text(
                                            if (cloudBackupEnabled) "Daily backup is active" else "Cloud backup is off",
                                            color = appColors.onMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = cloudBackupEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) requestDriveAuthorization() else scope.launch {
                                                prefs.saveCloudBackup(false)
                                                CloudBackupWork.cancel(context)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        SettingsSection.ABOUT -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.hki_logo_round),
                                    contentDescription = "HKI 7 logo",
                                    modifier = Modifier.size(96.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    "HKI 7",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "A modern, touch-first Android dashboard and companion for Home Assistant.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            SettingsSubcategory("What it does")
                            SettingsPanel {
                                Text(
                                    "HKI 7 turns your native Home Assistant entities, rooms, automations, media, energy data, and services into a configurable mobile dashboard. It is designed to stay connected to Home Assistant as the source of truth while offering a distinct HKI interface.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            SettingsSubcategory("Technology", "Languages and frameworks used by the Android app")
                            SettingsPanel {
                                SettingsTile(Icons.Default.PhoneAndroid, "Kotlin", "Primary application language")
                                SettingsTile(Icons.Default.Description, "XML and Kotlin DSL", "Android resources and build configuration")
                                Text(
                                    "The interface is built with Jetpack Compose. Coroutines and Flow handle live state, while JSON and YAML-compatible data keep HKI 7 connected with Home Assistant.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory("Created openly")
                            SettingsPanel {
                                Text(
                                    "Created by Jimz011 with help from AI-assisted development tools. Product direction, design choices, testing, and responsibility for the released app remain with the project creator.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { openGitHub(context, HKI7_GITHUB_URL) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    MdiIcon("github", tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("View on GitHub")
                                }
                            }
                        }
                        SettingsSection.LICENSE -> {
                            SettingsSubcategory("Community source", "Mozilla Public License 2.0 (MPL-2.0)")
                            SettingsPanel {
                                Text(
                                    "Copyright © 2026 Jimz011",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "The HKI 7 community source code is free and open source under the Mozilla Public License 2.0. You may use, study, modify, and redistribute it. When you distribute modified MPL-covered files, those files and their source must remain available under MPL-2.0.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, you can obtain one at mozilla.org/MPL/2.0/.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = { openExternalUrl(context, "https://www.mozilla.org/MPL/2.0/") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Read the full MPL 2.0")
                                }
                            }
                            SettingsSubcategory("Optional Premium", "Separate commercial content")
                            SettingsPanel {
                                Text(
                                    "Premium icon packs, animated icons and artwork, premium themes, entitlement services, and any separately marked premium modules are not covered by MPL-2.0. They remain proprietary and are licensed for personal use through a valid Premium entitlement.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "The open-source community core remains usable without Premium. Premium purchases fund extra visual content and continued development; they do not remove the freedoms granted for MPL-covered files.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory("Brand and third-party work")
                            SettingsPanel {
                                Text(
                                    "The HKI 7 name and logos are project trademarks and are not granted for misleading redistribution. Home Assistant and other third-party names, libraries, fonts, and artwork remain subject to their respective licenses and trademarks. The software is provided without warranty, as described by MPL-2.0.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        SettingsSection.SUPPORT -> {
                            SettingsSubcategory("Support without Premium", "Every contribution helps, and payment is never required")
                            SettingsPanel {
                                Text(
                                    "You can help by testing new builds, reporting reproducible bugs, suggesting thoughtful improvements, helping with translations or documentation, sharing HKI 7 with other Home Assistant users, and giving constructive feedback.",
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "When reporting a problem, include your Android version, Home Assistant version, the affected entity or integration, and clear steps to reproduce it. Removing private URLs, tokens, names, and location data first helps keep support safe.",
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory("Leave a tip", "Optional support for development and testing costs")
                            SupportLinkCard(
                                imageUrl = "https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png",
                                imageDescription = "Buy Me a Coffee",
                                label = "Support via Buy Me a Coffee",
                                onClick = { openExternalUrl(context, "https://www.buymeacoffee.com/w8Jnf6Hit") }
                            )
                            SupportLinkCard(
                                imageUrl = "https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg",
                                imageDescription = "PayPal",
                                label = "Support via PayPal",
                                onClick = { openExternalUrl(context, "https://paypal.me/JimmySchings") }
                            )
                            Text(
                                "These links open in your browser. Tips do not unlock Premium features and do not create an obligation to provide individual support.",
                                color = appColors.onMuted,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            )
                        }
                        SettingsSection.ACCOUNT -> {
                            SettingsSubcategory("Identity", "Personal information used throughout HKI 7")
                            SettingsChoice(Icons.Default.Person, "Profile", displayName) { section = SettingsSection.PROFILE }
                            val isDemoSession = com.jimz011apps.hki7.data.isDemoServerUrl(viewModel.currentUrl.collectAsState().value)
                            if (isDemoSession) {
                                SettingsSubcategory("Demo mode", "You're exploring the built-in sample home")
                                SettingsPanel {
                                    OutlinedButton(
                                        onClick = { viewModel.logout(keepConfig = false); onDismiss() },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = itemCornerShape(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Exit Demo Mode")
                                    }
                                    Text(
                                        "Exiting removes the sample home and returns to the welcome screen, where you can connect a real Home Assistant server.",
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                SettingsSubcategory("Session", "Sign out safely or reset this installation")
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
                }

                Text(
                    "Created by Jimz011 - 2026 • HKI 7 v${BuildConfig.VERSION_NAME}",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showRestoreSource) {
        AlertDialog(
            onDismissRequest = { showRestoreSource = false },
            title = { Text("Restore backup") },
            text = { Text("Choose where to restore the dashboard configuration from.") },
            confirmButton = {
                Button(onClick = {
                    showRestoreSource = false
                    restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                }) { Text("Local") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreSource = false
                    scope.launch {
                        runCatching { CloudBackupStorage.backups(context) }
                            .onSuccess { backups ->
                                cloudRestoreFiles = backups
                                if (backups.isEmpty()) setupChangedMessage = "No cloud backups were found."
                                else showCloudRestore = true
                            }
                            .onFailure { setupChangedMessage = "Could not load cloud backups: ${it.message}" }
                    }
                }) { Text("Cloud") }
            }
        )
    }

    if (showCloudRestore) {
        AlertDialog(
            onDismissRequest = { showCloudRestore = false },
            title = { Text("Restore from cloud") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cloudRestoreFiles.forEach { file ->
                        TextButton(
                            onClick = {
                                showCloudRestore = false
                                scope.launch {
                                    runCatching {
                                        val raw = CloudBackupStorage.read(context, file.id)
                                        prefs.restoreUiBackup(raw)
                                    }.onSuccess { setupChangedMessage = "Dashboard configuration restored." }
                                        .onFailure { setupChangedMessage = "Restore failed: ${it.message}" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(file.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCloudRestore = false }) { Text("Cancel") } }
        )
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

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { if (!restartBusy) showRestartConfirm = false },
            title = { Text("Restart Home Assistant?") },
            text = { Text("Your automations and devices will be unavailable briefly. HKI7 will reconnect automatically when Home Assistant is ready.") },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }, enabled = !restartBusy) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = !restartBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        restartBusy = true
                        scope.launch {
                            runCatching { viewModel.restartHomeAssistant() }
                                .onSuccess {
                                    showRestartConfirm = false
                                    // MainApp now shows the live Core restart phase immediately;
                                    // do not cover that progress with a second acknowledgement dialog.
                                    homeAssistantMessage = null
                                }
                                .onFailure {
                                    showRestartConfirm = false
                                    homeAssistantMessage = it.message ?: "Could not restart Home Assistant."
                                }
                            restartBusy = false
                        }
                    }
                ) { Text(if (restartBusy) "Restarting…" else "Restart") }
            }
        )
    }

    homeAssistantMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { homeAssistantMessage = null },
            title = { Text("Home Assistant") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { homeAssistantMessage = null }) { Text("OK") } }
        )
    }

    if (showAddHomeAssistantInstance) {
        AddHomeAssistantInstanceDialog(
            prefs = prefs,
            onDismiss = { showAddHomeAssistantInstance = false },
            onAdded = {
                showAddHomeAssistantInstance = false
                viewModel.completeInitialDashboardSetup()
            }
        )
    }

    renameHomeAssistantInstance?.let { instance ->
        AlertDialog(
            onDismissRequest = { renameHomeAssistantInstance = null },
            title = { Text("Rename Home Assistant") },
            text = {
                OutlinedTextField(
                    value = homeAssistantInstanceName,
                    onValueChange = { homeAssistantInstanceName = it },
                    label = { Text("Instance name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = homeAssistantInstanceName.isNotBlank(),
                    onClick = {
                        scope.launch { prefs.renameHomeAssistantInstance(instance.id, homeAssistantInstanceName) }
                        renameHomeAssistantInstance = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameHomeAssistantInstance = null }) { Text("Cancel") } }
        )
    }

    deleteHomeAssistantInstance?.let { instance ->
        AlertDialog(
            onDismissRequest = { deleteHomeAssistantInstance = null },
            title = { Text("Remove ${instance.name}?") },
            text = { Text("This removes its login, mobile-app registration details, and HKI dashboards from this device. The Home Assistant server itself is not changed.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.removeHomeAssistantInstance(instance.id)
                        deleteHomeAssistantInstance = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteHomeAssistantInstance = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InstanceCapabilityToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    ModernSettingsHeader(
        title = title,
        subtitle = subtitle,
        icon = icon,
        canGoBack = canGoBack,
        onBack = onBack,
        onClose = onDismiss
    )
}

@Composable
private fun SettingsChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ModernSettingsMenuItem(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
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
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                if (page == null) "Create custom page" else "Edit custom page",
                "Identity and navigation appearance"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSubcategory("Page identity", "Name, subtitle, and navigation icon")
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
private fun SupportLinkCard(
    imageUrl: String,
    imageDescription: String,
    label: String,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = itemCornerShape(),
        color = appColors.subtleSurface,
        contentColor = appColors.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.width(142.dp).height(58.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                // These logos come from third-party CDNs, so they can fail to load (offline, a
                // moved asset, a blocked request) and would otherwise leave a blank white card.
                // Fall back to the brand name so the button always reads as a button.
                coil3.compose.SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = imageDescription,
                    contentScale = ContentScale.Fit,
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                imageDescription,
                                color = Color.Black,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(6.dp)
                )
            }
            Text(
                label,
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link", tint = appColors.onMuted)
        }
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

const val HKI7_GITHUB_URL = "https://github.com/jimz011/android-hki7"

/**
 * Opens the repository in the GitHub app when it is installed, otherwise falls back to the normal
 * browser intent. Targeting the package explicitly is what stops Android from showing a chooser (or
 * silently preferring the browser) when GitHub is present.
 */
private fun openGitHub(context: android.content.Context, url: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        .setPackage("com.github.android")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(appIntent); true }.getOrDefault(false)
    if (!opened) openExternalUrl(context, url)
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    SettingsGroup(content = content)
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
            SettingsChoiceChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

private fun connectionText(status: ConnectionStatus, route: HomeAssistantConnectionRoute?): String = when (status) {
    ConnectionStatus.CONNECTED -> "Connected via ${route?.displayName ?: "Unknown"}"
    ConnectionStatus.ERROR -> "Error"
    else -> "Connecting..."
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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

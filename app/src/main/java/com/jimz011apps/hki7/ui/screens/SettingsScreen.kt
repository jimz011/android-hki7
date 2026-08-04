@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings as AndroidSettings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.jimz011apps.hki7.data.HaBackupStorage
import com.jimz011apps.hki7.data.hki7BackupName
import com.jimz011apps.hki7.data.HaDashboardSharing
import androidx.compose.foundation.layout.FlowRow
import com.jimz011apps.hki7.data.HaParentalControls
import com.jimz011apps.hki7.ui.components.DefaultIconEffectByGroup
import com.jimz011apps.hki7.ui.components.IconEffectGroups
import com.jimz011apps.hki7.data.driveAuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.jimz011apps.hki7.data.HKICustomPage
import com.jimz011apps.hki7.data.HKICustomPopup
import com.jimz011apps.hki7.data.Hki7PolicySaveResult
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.PushForegroundService
import com.jimz011apps.hki7.data.LocationWork
import com.jimz011apps.hki7.data.SYSTEM_LANGUAGE_TAG
import com.jimz011apps.hki7.data.currentAppLanguage
import com.jimz011apps.hki7.data.setAppLanguage
import com.jimz011apps.hki7.ui.components.RenameCardDialog
import com.jimz011apps.hki7.ui.ConnectionStatus
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.NavBarConfig
import com.jimz011apps.hki7.ui.Screen
import com.jimz011apps.hki7.ui.localizedTitle
import com.jimz011apps.hki7.ui.localizedName
import com.jimz011apps.hki7.ui.components.ColorWheel
import com.jimz011apps.hki7.ui.components.HKISlider
import com.jimz011apps.hki7.ui.components.Hki7CloudInstallCard
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsHeader
import com.jimz011apps.hki7.ui.components.ModernSettingsMenuItem
import com.jimz011apps.hki7.ui.components.SettingsGroup
import com.jimz011apps.hki7.ui.components.SettingsChoiceChip
import com.jimz011apps.hki7.ui.components.SettingsSubcategory
import com.jimz011apps.hki7.ui.components.SettingsTabRow
import com.jimz011apps.hki7.ui.components.SearchAccessSelection
import com.jimz011apps.hki7.ui.components.SearchAccessSelectionDialog
import com.jimz011apps.hki7.ui.components.WhatsNewDialog
import com.jimz011apps.hki7.data.Hki7Policy
import com.jimz011apps.hki7.data.Hki7RoomFollow
import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.ui.components.RoomFollowRoomDialog
import com.jimz011apps.hki7.ui.components.RoomFollowSensorDialog
import com.jimz011apps.hki7.ui.observedRoomStates
import com.jimz011apps.hki7.ui.resolveFollowedArea
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.CustomPopupSettingsDialog
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
    MENU, CONNECTION, PROFILE, LOCATION, NOTIFICATIONS, APPEARANCE, HEADER, THEME, FONTS, LANGUAGE, CORNERS, ICONS, NAV_BAR, MEDIA_PLAYERS, POPUPS, DASHBOARD, FAMILY_SHARING, BACKUP_RESTORE, ACCOUNT, ABOUT, LICENSE, SUPPORT
}

/** Human-friendly "5 minutes ago" / "yesterday" label for the last-backup subtitle. */
@Composable
private fun relativeBackupTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val elapsed = (now - epochMillis).coerceAtLeast(0L)
    if (elapsed < DateUtils.MINUTE_IN_MILLIS) {
        return stringResource(R.string.settings_just_now)
    }
    if (elapsed < DateUtils.HOUR_IN_MILLIS) {
        val minutes = (elapsed / DateUtils.MINUTE_IN_MILLIS).toInt()
        return pluralStringResource(R.plurals.settings_extra_minutes_ago, minutes, minutes)
    }
    if (elapsed < DateUtils.DAY_IN_MILLIS) {
        val hours = (elapsed / DateUtils.HOUR_IN_MILLIS).toInt()
        return pluralStringResource(R.plurals.settings_extra_hours_ago, hours, hours)
    }
    if (elapsed < 7 * DateUtils.DAY_IN_MILLIS) {
        val days = (elapsed / DateUtils.DAY_IN_MILLIS).toInt()
        return pluralStringResource(R.plurals.settings_extra_days_ago, days, days)
    }
    val locale = LocalContext.current.resources.configuration.locales[0]
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(
            java.time.format.DateTimeFormatter
                .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
                .withLocale(locale)
        )
}

/** Formats a shared-dashboard "updated" timestamp (stored by the component as a UTC ISO-8601 string,
 * e.g. "2026-07-29T11:38:01+00:00") in the device's local time zone, so it matches the wall clock —
 * the previous raw first-19-chars display always showed UTC, off by the local offset (incl. DST). */
@Composable
private fun formatSharedUpdated(iso: String): String {
    if (iso.isBlank()) return ""
    val locale = LocalContext.current.resources.configuration.locales[0]
    return runCatching {
        java.time.OffsetDateTime.parse(iso)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(
                java.time.format.DateTimeFormatter
                    .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
                    .withLocale(locale)
            )
    }.getOrElse { iso.take(19).replace('T', ' ') }
}

@Composable
private fun sectionTitle(section: SettingsSection): String = stringResource(when (section) {
    SettingsSection.MENU -> R.string.nav_settings
    SettingsSection.CONNECTION -> R.string.settings_title_connection
    SettingsSection.PROFILE -> R.string.settings_title_profile
    SettingsSection.LOCATION -> R.string.settings_title_location
    SettingsSection.NOTIFICATIONS -> R.string.settings_title_notifications
    SettingsSection.APPEARANCE -> R.string.settings_title_appearance
    SettingsSection.HEADER -> R.string.settings_title_header
    SettingsSection.THEME -> R.string.settings_title_theme
    SettingsSection.FONTS -> R.string.settings_title_fonts
    SettingsSection.LANGUAGE -> R.string.language_title
    SettingsSection.CORNERS -> R.string.settings_title_corners
    SettingsSection.ICONS -> R.string.settings_title_icons
    SettingsSection.NAV_BAR -> R.string.settings_title_nav_bar
    SettingsSection.MEDIA_PLAYERS -> R.string.settings_title_media_players
    SettingsSection.POPUPS -> R.string.popup_settings_title
    SettingsSection.DASHBOARD -> R.string.settings_title_dashboard
    SettingsSection.FAMILY_SHARING -> R.string.settings_title_family_sharing
    SettingsSection.BACKUP_RESTORE -> R.string.settings_title_backup_restore
    SettingsSection.ACCOUNT -> R.string.settings_title_account
    SettingsSection.ABOUT -> R.string.settings_title_about
    SettingsSection.LICENSE -> R.string.settings_title_license
    SettingsSection.SUPPORT -> R.string.settings_title_support
})

@Composable
private fun sectionSubtitle(section: SettingsSection): String = stringResource(when (section) {
    SettingsSection.MENU -> R.string.settings_subtitle_menu
    SettingsSection.ACCOUNT -> R.string.settings_subtitle_account
    SettingsSection.PROFILE -> R.string.settings_subtitle_profile
    SettingsSection.CONNECTION -> R.string.settings_subtitle_connection
    SettingsSection.LOCATION -> R.string.settings_subtitle_location
    SettingsSection.DASHBOARD -> R.string.settings_subtitle_dashboard
    SettingsSection.APPEARANCE -> R.string.settings_subtitle_appearance
    SettingsSection.CORNERS -> R.string.settings_subtitle_corners
    SettingsSection.ICONS -> R.string.settings_subtitle_icons
    SettingsSection.HEADER -> R.string.settings_subtitle_header
    SettingsSection.THEME -> R.string.settings_subtitle_theme
    SettingsSection.FONTS -> R.string.settings_subtitle_fonts
    SettingsSection.LANGUAGE -> R.string.language_display_subtitle
    SettingsSection.NAV_BAR -> R.string.settings_subtitle_nav_bar
    SettingsSection.MEDIA_PLAYERS -> R.string.settings_subtitle_media_players
    SettingsSection.POPUPS -> R.string.popup_settings_subtitle_section
    SettingsSection.NOTIFICATIONS -> R.string.settings_subtitle_notifications
    SettingsSection.BACKUP_RESTORE -> R.string.settings_subtitle_backup_restore
    SettingsSection.FAMILY_SHARING -> R.string.settings_subtitle_family_sharing
    SettingsSection.ABOUT -> R.string.settings_subtitle_about
    SettingsSection.LICENSE -> R.string.settings_subtitle_license
    SettingsSection.SUPPORT -> R.string.settings_subtitle_support
})

private fun sectionIcon(section: SettingsSection): ImageVector = when (section) {
    SettingsSection.MENU -> Icons.Default.SettingsEthernet
    SettingsSection.ACCOUNT, SettingsSection.PROFILE -> Icons.Default.Person
    SettingsSection.CONNECTION -> Icons.Default.SettingsEthernet
    SettingsSection.LOCATION -> Icons.Default.MyLocation
    SettingsSection.DASHBOARD -> Icons.Default.Dashboard
    SettingsSection.APPEARANCE, SettingsSection.THEME -> Icons.Default.Palette
    SettingsSection.CORNERS -> Icons.Default.RoundedCorner
    SettingsSection.ICONS -> Icons.Default.AutoAwesome
    SettingsSection.HEADER -> Icons.Default.Tune
    SettingsSection.FONTS -> Icons.Default.TextFields
    SettingsSection.NAV_BAR -> Icons.Default.Menu
    SettingsSection.MEDIA_PLAYERS -> Icons.Default.MusicNote
    SettingsSection.POPUPS -> Icons.Default.OpenInNew
    SettingsSection.LANGUAGE -> Icons.Default.Language
    SettingsSection.NOTIFICATIONS -> Icons.Default.Notifications
    SettingsSection.BACKUP_RESTORE -> Icons.Default.Backup
    SettingsSection.FAMILY_SHARING -> Icons.Default.Shield
    SettingsSection.ABOUT -> Icons.Default.Info
    SettingsSection.LICENSE -> Icons.Default.Description
    SettingsSection.SUPPORT -> Icons.Default.Favorite
}

// Subsections nested under Appearance return there on back; everything else returns to the menu.
private fun parentSection(section: SettingsSection): SettingsSection = when (section) {
    SettingsSection.HEADER, SettingsSection.THEME, SettingsSection.FONTS, SettingsSection.LANGUAGE, SettingsSection.CORNERS, SettingsSection.ICONS, SettingsSection.NAV_BAR, SettingsSection.MEDIA_PLAYERS, SettingsSection.POPUPS -> SettingsSection.APPEARANCE
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
    val familyDashboardSubscribed by viewModel.familyDashboardSubscribed.collectAsState()
    val allowDashboardSwitch by viewModel.allowDashboardSwitch.collectAsState()
    val allowDashboardCreate by viewModel.allowDashboardCreate.collectAsState()
    val dashboardSettingsLocked = familyDashboardSubscribed && !allowDashboardSwitch && !allowDashboardCreate
    val homeAssistantInstances by prefs.homeAssistantInstances.collectAsState(initial = emptyList())
    val activeHomeAssistantInstanceId by prefs.activeHomeAssistantInstanceId.collectAsState(initial = null)
    val cloudBackupEnabled by prefs.cloudBackupEnabled.collectAsState(initial = false)
    val haBackupEnabled by prefs.haBackupEnabled.collectAsState(initial = false)
    val cloudBackupLastAt by prefs.cloudBackupLastAt.collectAsState(initial = null)
    val haBackupLastAt by prefs.haBackupLastAt.collectAsState(initial = null)
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
    var copyDashboard by remember { mutableStateOf<com.jimz011apps.hki7.data.HKIDashboard?>(null) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var pendingUnpublish by remember { mutableStateOf<com.jimz011apps.hki7.data.Hki7SharedDashboardMeta?>(null) }
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
    var showHaRestore by remember { mutableStateOf(false) }
    var haRestoreFiles by remember { mutableStateOf(emptyList<com.jimz011apps.hki7.data.Hki7BackupMeta>()) }
    var haBackupBusy by remember { mutableStateOf(false) }
    var cloudBackupNowBusy by remember { mutableStateOf(false) }
    var haBackupNowBusy by remember { mutableStateOf(false) }
    // ── Family dashboard sharing ──
    var shareDashboard by remember { mutableStateOf<com.jimz011apps.hki7.data.HKIDashboard?>(null) }
    var shareUsers by remember { mutableStateOf(emptyList<com.jimz011apps.hki7.data.Hki7User>()) }
    var shareSelected by remember { mutableStateOf(setOf<String>()) }
    var shareEveryone by remember { mutableStateOf(false) }
    var shareBusy by remember { mutableStateOf(false) }
    var sharedWithMe by remember { mutableStateOf(emptyList<com.jimz011apps.hki7.data.Hki7SharedDashboardMeta>()) }
    var sharingAvailable by remember { mutableStateOf(false) }
    var isHaAdmin by remember { mutableStateOf(false) }
    var currentHaUserId by remember { mutableStateOf<String?>(null) }
    val familySettingsLocked = sharingAvailable && !isHaAdmin
    // ── Parental controls (admin editor) ──
    var parentalUsers by remember { mutableStateOf(emptyList<com.jimz011apps.hki7.data.Hki7User>()) }
    var parentalPolicies by remember { mutableStateOf(emptyMap<String, com.jimz011apps.hki7.data.Hki7Policy>()) }
    var parentalExpandedUser by remember { mutableStateOf<String?>(null) }
    // Probe the hki7 component once so the menu can show admin-only entries (Parental Controls).
    LaunchedEffect(Unit) {
        val id = runCatching { HaDashboardSharing.whoami(context) }.getOrNull()
        sharingAvailable = id != null
        isHaAdmin = id?.isAdmin == true
        currentHaUserId = id?.userId
    }
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
                        ?: error(context.getString(R.string.settings_extra_selected_file_open_failed))
                }.onSuccess {
                    setupChangedMessage = context.getString(R.string.settings_extra_dashboard_backup_saved)
                }.onFailure { error ->
                    setupChangedMessage = context.getString(
                        R.string.settings_extra_backup_failed,
                        error.message ?: context.getString(R.string.settings_extra_unknown_error)
                    )
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val raw = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                        ?: error(context.getString(R.string.settings_extra_selected_file_open_failed))
                    prefs.restoreUiBackup(raw)
                }.onSuccess {
                    setupChangedMessage = context.getString(R.string.settings_extra_dashboard_restored)
                }.onFailure { error ->
                    setupChangedMessage = context.getString(
                        R.string.settings_extra_restore_failed,
                        error.message ?: context.getString(R.string.settings_extra_unknown_error)
                    )
                }
            }
        }
    }
    val enableCloudBackup = {
        scope.launch {
            prefs.saveCloudBackup(true)
            CloudBackupWork.schedule(context)
            setupChangedMessage = context.getString(R.string.settings_extra_automatic_cloud_backup_enabled)
        }
    }
    val driveAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorization ->
                if (authorization.accessToken != null) enableCloudBackup()
                else setupChangedMessage = context.getString(R.string.settings_extra_drive_access_token_missing)
            }.onFailure {
                setupChangedMessage = context.getString(
                    R.string.settings_extra_drive_authorization_failed,
                    it.message ?: context.getString(R.string.settings_extra_unknown_error)
                )
            }
        } else {
            setupChangedMessage = context.getString(R.string.settings_extra_drive_authorization_cancelled)
        }
    }
    val requestDriveAuthorization = {
        Identity.getAuthorizationClient(context).authorize(driveAuthorizationRequest())
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) {
                        driveAuthorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else setupChangedMessage = context.getString(R.string.settings_extra_drive_authorization_open_failed)
                } else if (authorization.accessToken != null) {
                    enableCloudBackup()
                } else setupChangedMessage = context.getString(R.string.settings_extra_drive_access_token_missing)
            }
            .addOnFailureListener {
                setupChangedMessage = context.getString(
                    R.string.settings_extra_drive_authorization_failed,
                    it.message ?: context.getString(R.string.settings_extra_unknown_error)
                )
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
                            SettingsSubcategory(stringResource(R.string.ui_your_home_72412fa), stringResource(R.string.ui_identity_and_connection_bee53f8))
                            SettingsChoice(Icons.Default.Person, stringResource(R.string.ui_account_85dfa32), displayName) { section = SettingsSection.ACCOUNT }
                            SettingsChoice(Icons.Default.SettingsEthernet, stringResource(R.string.ui_connection_6512ee1), connectionText(status, currentConnectionRoute)) { section = SettingsSection.CONNECTION }
                            SettingsChoice(Icons.Default.MyLocation, stringResource(R.string.ui_location_d219c68), stringResource(R.string.ui_device_tracker_and_geocoded_location_f9e2d34)) { section = SettingsSection.LOCATION }
                            SettingsSubcategory(stringResource(R.string.ui_personalize_7602b35), stringResource(R.string.ui_dashboards_visual_style_and_everyday_navigation_17f3ac2))
                            SettingsChoice(
                                if (dashboardSettingsLocked) Icons.Default.Lock else Icons.Default.Dashboard,
                                stringResource(R.string.ui_dashboard_d87f47b),
                                if (dashboardSettingsLocked) {
                                    stringResource(R.string.family_dashboard_tab_locked)
                                } else if (dashboardMode == "auto") {
                                    stringResource(R.string.settings_extra_dashboard_mode_auto)
                                } else {
                                    stringResource(R.string.settings_extra_dashboard_mode_manual)
                                },
                                enabled = !dashboardSettingsLocked,
                            ) { section = SettingsSection.DASHBOARD }
                            SettingsChoice(Icons.Default.Palette, stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_theme_and_navigation_bar_474ee6b)) { section = SettingsSection.APPEARANCE }
                            SettingsSubcategory(stringResource(R.string.ui_services_data_7864c0a), stringResource(R.string.ui_messages_safety_and_portability_ee58dfe))
                            SettingsChoice(Icons.Default.Notifications, stringResource(R.string.ui_notifications_753a22b), stringResource(R.string.ui_push_delivery_and_history_aa3e29d)) { section = SettingsSection.NOTIFICATIONS }
                            SettingsChoice(Icons.Default.Backup, stringResource(R.string.ui_backup_and_restore_a593246), stringResource(R.string.ui_save_or_restore_dashboard_configuration_be8f39f)) { section = SettingsSection.BACKUP_RESTORE }
                            SettingsChoice(
                                if (familySettingsLocked) Icons.Default.Lock else Icons.Default.Shield,
                                stringResource(R.string.ui_family_sharing_160fddb),
                                if (!sharingAvailable) stringResource(R.string.ui_parental_controls_sharing_needs_the_hki_7_cloud_component_7929459)
                                else if (isHaAdmin) stringResource(R.string.ui_parental_controls_dashboard_sharing_and_permissions_b74e0c0)
                                else stringResource(R.string.family_settings_admin_only),
                                enabled = !familySettingsLocked,
                            ) { section = SettingsSection.FAMILY_SHARING }
                            SettingsSubcategory(stringResource(R.string.ui_hki_7_68a9e17), stringResource(R.string.ui_project_information_licensing_and_community_support_9fc47d6))
                            SettingsChoice(Icons.Default.Info, stringResource(R.string.ui_about_6b21fb7), stringResource(R.string.ui_what_hki_7_is_and_how_it_is_built_247bace)) { section = SettingsSection.ABOUT }
                            SettingsChoice(Icons.Default.Description, stringResource(R.string.ui_license_3229609), stringResource(R.string.ui_open_source_and_premium_licensing_0328125)) { section = SettingsSection.LICENSE }
                            SettingsChoice(Icons.Default.Favorite, stringResource(R.string.ui_support_f32d5a3), stringResource(R.string.ui_help_the_project_without_buying_premium_e960d10)) { section = SettingsSection.SUPPORT }
                        }
                        SettingsSection.CONNECTION -> {
                            val homeSsids by prefs.homeSsids.collectAsState(initial = emptyList())
                            val currentSsid by viewModel.currentSsid.collectAsState()
                            var externalUrlInput by remember(serverUrl) { mutableStateOf(serverUrl.orEmpty()) }
                            var internalUrlInput by remember(internalUrl) { mutableStateOf(internalUrl.orEmpty()) }
                            var ssidsInput by remember(homeSsids) { mutableStateOf(homeSsids.joinToString(", ")) }
                            SettingsSubcategory(
                                stringResource(R.string.ui_home_assistant_instances_b0de2f3),
                                stringResource(R.string.ui_each_home_keeps_its_own_login_dashboard_notification_conne_c261f94)
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
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        stringResource(R.string.settings_extra_active_instance),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    homeAssistantInstanceName = instance.name
                                                    renameHomeAssistantInstance = instance
                                                }) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        stringResource(R.string.settings_extra_rename_instance, instance.name),
                                                        tint = appColors.onMuted
                                                    )
                                                }
                                            }
                                            InstanceCapabilityToggle(
                                                title = stringResource(R.string.ui_notifications_753a22b),
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
                                                title = stringResource(R.string.ui_location_d219c68),
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
                                                    Text(stringResource(R.string.ui_remove_e963907))
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
                                    Text(stringResource(R.string.ui_add_home_assistant_instance_625838a))
                                }
                                Text(
                                    stringResource(R.string.ui_swipe_left_from_the_upper_right_edge_of_a_416c467),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )
                            }
                            SettingsSubcategory(
                                stringResource(R.string.ui_active_connection_54db7a1),
                                stringResource(
                                    R.string.ui_network_routes_for_1d18efd,
                                    homeAssistantInstances.firstOrNull { it.id == activeHomeAssistantInstanceId }?.name
                                        ?: stringResource(R.string.settings_this_home)
                                )
                            )
                            SettingsPanel {
                                val (icon, color, text) = when (status) {
                                    ConnectionStatus.CONNECTED -> Triple(
                                        Icons.Default.CheckCircle,
                                        Color(0xFF6AC36A),
                                        stringResource(
                                            R.string.settings_connection_via,
                                            currentConnectionRoute?.localizedName()
                                                ?: stringResource(R.string.settings_connection_unknown)
                                        )
                                    )
                                    ConnectionStatus.ERROR -> Triple(
                                        Icons.Default.Error,
                                        MaterialTheme.colorScheme.error,
                                        stringResource(R.string.connection_error_title)
                                    )
                                    else -> Triple(
                                        Icons.Default.Sync,
                                        Color.Gray,
                                        stringResource(R.string.settings_connection_connecting)
                                    )
                                }
                                SettingsTile(icon, text, currentUrl.ifBlank { serverUrl ?: "" }, iconTint = color)
                                Button(
                                    onClick = { viewModel.refreshEntities() },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) { Text(stringResource(R.string.ui_refresh_connection_46966aa)) }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.ui_remote_access_optional_75dd54d),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = appColors.onSurface
                                )
                                OutlinedTextField(
                                    value = externalUrlInput,
                                    onValueChange = { externalUrlInput = it },
                                    label = { Text(stringResource(R.string.ui_external_url_or_nabu_casa_url_d2e1eed)) },
                                    placeholder = { Text(stringResource(R.string.ui_https_example_ui_nabu_casa_4d97501)) },
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
                                ) { Text(stringResource(R.string.ui_save_remote_access_05740bc)) }
                                Text(
                                    stringResource(R.string.ui_leave_this_empty_for_local_only_access_add_an_cc6bf85),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.ui_local_network_optional_42aba01),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = appColors.onSurface
                                )
                                SettingsTile(
                                    Icons.Default.Wifi,
                                    stringResource(R.string.settings_current_wifi),
                                    currentSsid ?: stringResource(R.string.settings_wifi_unavailable)
                                )
                                OutlinedTextField(
                                    value = internalUrlInput,
                                    onValueChange = { internalUrlInput = it },
                                    label = { Text(stringResource(R.string.ui_internal_url_acab9b2)) },
                                    placeholder = { Text(stringResource(R.string.ui_http_homeassistant_local_8123_67b2235)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = ssidsInput,
                                    onValueChange = { ssidsInput = it },
                                    label = { Text(stringResource(R.string.ui_home_wi_fi_names_comma_separated_b71c6ab)) },
                                    placeholder = { Text(stringResource(R.string.ui_mywifi_mywifi_5g_2a163c1)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                currentSsid?.let { ssid ->
                                    TextButton(onClick = {
                                        val updated = (ssidsInput.split(",").map { it.trim() }.filter { it.isNotBlank() } + ssid).distinct()
                                        ssidsInput = updated.joinToString(", ")
                                    }) { Text(stringResource(R.string.ui_add_current_network_64cabf3, ssid)) }
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
                                ) { Text(stringResource(R.string.ui_save_local_network_7f0247d)) }
                                Text(
                                    stringResource(R.string.ui_on_these_wi_fi_networks_the_app_connects_via_82d3f1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.onMuted
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_maintenance_94de303), stringResource(R.string.ui_administrative_controls_for_your_home_assistant_server_e56ebd9))
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
                                    Text(stringResource(R.string.ui_restart_home_assistant_551322c))
                                }
                                Text(
                                    stringResource(R.string.ui_home_assistant_will_be_unavailable_briefly_while_it_restar_df30b45),
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
                                    label = { Text(stringResource(R.string.ui_name_709a232)) },
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
                                            Text(people.find { it.entity_id == personInput }?.friendlyName ?: stringResource(R.string.ui_choose_person_entity_977db64), modifier = Modifier.weight(1f), color = appColors.onSurface)
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
                                    label = { Text(stringResource(R.string.ui_birthday_yyyy_mm_dd_d1a9b03)) },
                                    leadingIcon = { Icon(Icons.Default.Cake, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = settingsTextFieldColors()
                                )
                                OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Person, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (avatar == null) stringResource(R.string.ui_choose_avatar_image_bcd5b00) else stringResource(R.string.ui_change_avatar_image_217fbce))
                                }
                                if (avatar != null) {
                                    TextButton(onClick = { scope.launch { prefs.saveProfileAvatar(null) } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_remove_avatar_58e0c56)) }
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            prefs.saveDisplayName(
                                                nameInput.trim().ifBlank {
                                                    context.getString(R.string.settings_extra_default_user_name)
                                                }
                                            )
                                            prefs.saveProfileBirthday(birthdayInput.trim().ifBlank { null })
                                            prefs.saveProfilePersonEntityId(personInput)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(R.string.ui_save_profile_f597c0e)) }
                            }
                        }
                        SettingsSection.LOCATION -> {
                            val highAccuracy by prefs.highAccuracyLocation.collectAsState(initial = false)
                            SettingsPanel {
                                SettingsTile(
                                    Icons.Default.MyLocation,
                                    stringResource(R.string.settings_extra_device_tracker),
                                    stringResource(R.string.settings_extra_device_tracker_subtitle)
                                )
                                SettingsTile(
                                    icon = Icons.Default.MyLocation,
                                    title = stringResource(R.string.ui_android_location_permission_2ff3839),
                                    subtitle = when {
                                        hasBackgroundLocation -> stringResource(R.string.settings_extra_location_allowed_always)
                                        hasForegroundLocation -> stringResource(R.string.settings_extra_location_allowed_foreground)
                                        else -> stringResource(R.string.settings_extra_location_not_allowed)
                                    },
                                    iconTint = if (hasBackgroundLocation) Color(0xFF6AC36A) else Color.Gray
                                )
                                SettingsTile(
                                    icon = Icons.Default.BatterySaver,
                                    title = stringResource(R.string.ui_battery_optimization_7676ca5),
                                    subtitle = if (isIgnoringBatteryOptimizations) {
                                        stringResource(R.string.settings_extra_unrestricted_recommended)
                                    } else {
                                        stringResource(R.string.settings_extra_battery_optimized_warning)
                                    },
                                    iconTint = if (isIgnoringBatteryOptimizations) Color(0xFF6AC36A) else Color.Gray
                                )
                                SettingsTile(
                                    icon = Icons.Default.PhoneAndroid,
                                    title = stringResource(R.string.ui_background_usage_fd8ed73),
                                    subtitle = if (!isBackgroundRestricted) {
                                        stringResource(R.string.settings_extra_unrestricted_recommended)
                                    } else {
                                        stringResource(R.string.settings_extra_background_restricted_warning)
                                    },
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
                                    Text(stringResource(R.string.ui_allow_all_the_time_d91ab85))
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
                                    Text(stringResource(R.string.ui_disable_battery_optimization_d3201bf))
                                }
                                Button(
                                    onClick = { viewModel.reportDeviceTelemetry(context) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Sync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_update_location_now_d338c2c))
                                }
                                SettingsToggle(
                                    title = stringResource(R.string.ui_high_accuracy_mode_954d2c5),
                                    subtitle = stringResource(R.string.ui_continuous_gps_for_live_tracking_uses_much_more_battery_fdcd80d),
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
                                    stringResource(R.string.ui_notifications_are_delivered_over_the_app_s_live_connection_d2aa965),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsToggle(
                                    title = stringResource(R.string.ui_background_notifications_563a463),
                                    subtitle = if (multiInstancePush) {
                                        stringResource(R.string.settings_extra_notifications_multi_home_required)
                                    } else {
                                        stringResource(R.string.settings_extra_notifications_persistent_connection)
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
                                        Text(stringResource(R.string.ui_hide_connection_notification_dcee06a))
                                    }
                                    Text(
                                        stringResource(R.string.ui_turn_the_notification_connection_channel_off_on_the_next_cdddec0),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        SettingsSection.APPEARANCE -> {
                            SettingsSubcategory(stringResource(R.string.ui_visual_style_bc567f3), stringResource(R.string.ui_color_typography_and_component_shape_b82e582))
                            SettingsChoice(Icons.Default.Palette, stringResource(R.string.ui_theme_a797e30), stringResource(R.string.ui_colors_and_light_dark_mode_37de9d5)) { section = SettingsSection.THEME }
                            SettingsChoice(Icons.Default.TextFields, stringResource(R.string.ui_fonts_ffe688a), stringResource(R.string.ui_text_size_boldness_and_font_family_365f489)) { section = SettingsSection.FONTS }
                            SettingsChoice(Icons.Default.Language, stringResource(R.string.language_title), stringResource(R.string.language_display_subtitle)) { section = SettingsSection.LANGUAGE }
                            SettingsChoice(Icons.Default.RoundedCorner, stringResource(R.string.ui_corners_f1fb139), stringResource(R.string.ui_roundness_of_buttons_cards_and_widgets_3573fff)) { section = SettingsSection.CORNERS }
                            SettingsChoice(Icons.Default.AutoAwesome, stringResource(R.string.ui_icons_edb8f6c), stringResource(R.string.ui_icon_animations_and_effects_db0dea4)) { section = SettingsSection.ICONS }
                            SettingsChoice(Icons.Default.Tune, stringResource(R.string.ui_header_31341c6), stringResource(R.string.ui_choose_an_expanded_or_compact_dashboard_header_815902d)) { section = SettingsSection.HEADER }
                            SettingsSubcategory(stringResource(R.string.ui_everyday_navigation_e2f1711), stringResource(R.string.ui_tabs_and_media_controls_shown_throughout_the_app_5d9c1ff))
                            SettingsChoice(Icons.Default.Menu, stringResource(R.string.ui_navigation_bar_e90e3de), stringResource(R.string.ui_reorder_and_hide_tabs_39de701)) { section = SettingsSection.NAV_BAR }
                            SettingsChoice(Icons.Default.MusicNote, stringResource(R.string.ui_media_players_ec25525), stringResource(R.string.ui_rename_players_and_mini_player_visibility_8d0e1f7)) { section = SettingsSection.MEDIA_PLAYERS }
                            SettingsChoice(Icons.Default.OpenInNew, stringResource(R.string.popup_settings_title), stringResource(R.string.popup_settings_subtitle_section)) { section = SettingsSection.POPUPS }
                        }
                        SettingsSection.HEADER -> {
                            val headerVisible by prefs.headerVisible.collectAsState(initial = true)
                            SettingsSubcategory(stringResource(R.string.ui_dashboard_header_36d8656), stringResource(R.string.ui_choose_between_the_full_header_and_a_compact_navigation_90ec55e))
                            SettingsPanel {
                                SettingsToggle(
                                    title = stringResource(R.string.ui_compact_header_50275e8),
                                    subtitle = stringResource(R.string.ui_keep_only_the_title_right_header_pill_and_back_7777651),
                                    checked = !headerVisible,
                                    onCheckedChange = { compact -> scope.launch { prefs.saveHeaderVisible(!compact) } }
                                )
                                Text(
                                    stringResource(R.string.ui_compact_mode_hides_the_left_header_pill_persons_subtitle_da84968),
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
                                    stringResource(R.string.ui_font_size_ef89fc7, (localScale * 100).toInt()),
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
                                    -200 -> stringResource(R.string.ui_thinner_200_6ae7a6f)
                                    -100 -> stringResource(R.string.ui_thin_100_9e7a443)
                                    0 -> stringResource(R.string.ui_default_808d7dc)
                                    100 -> stringResource(R.string.ui_bold_100_1b94255)
                                    200 -> stringResource(R.string.ui_bolder_200_9cc5d65)
                                    else -> stringResource(R.string.ui_boldest_300_c5a490d)
                                }
                                Text(
                                    stringResource(R.string.ui_boldness_10b6938, weightLabel),
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
                                Text(stringResource(R.string.ui_font_family_54a1848), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
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
                                                localizedFontFamilyName(fontFamily),
                                                color = appColors.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(Icons.Default.KeyboardArrowDown, null, tint = appColors.onMuted)
                                        }
                                    }
                                    DropdownMenu(expanded = familyMenuOpen, onDismissRequest = { familyMenuOpen = false }) {
                                        familyOptions.forEach { (value, _) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        localizedFontFamilyName(value),
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
                                    stringResource(R.string.ui_the_quick_brown_fox_jumps_over_the_lazy_dog_2204ad2),
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
                                    stringResource(R.string.ui_rename_players_and_choose_which_ones_may_show_the_212864f),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val sorted = players.sortedBy { (customNames[it.entity_id] ?: it.friendlyName ?: it.entity_id).lowercase() }
                                if (sorted.isEmpty()) {
                                    Text(stringResource(R.string.ui_no_media_players_found_1248a89), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
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
                                        TextButton(onClick = { renamingPlayer = player }) { Text(stringResource(R.string.ui_rename_d3f4cb8)) }
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
                        SettingsSection.POPUPS -> {
                            val popups by viewModel.customPopups.collectAsState()
                            val popupEntities by viewModel.entities.collectAsState()
                            var editingPopup by remember { mutableStateOf<HKICustomPopup?>(null) }
                            val newPopupName = stringResource(R.string.popup_default_name)
                            editingPopup?.let { draft ->
                                CustomPopupSettingsDialog(
                                    popup = draft,
                                    allEntities = popupEntities,
                                    onDismiss = { editingPopup = null },
                                    onSave = { updated -> viewModel.updateCustomPopup(updated); editingPopup = null },
                                    onDelete = { viewModel.deleteCustomPopup(draft.id); editingPopup = null }
                                )
                            }
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.popup_settings_hint),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (popups.isEmpty()) {
                                    Text(
                                        stringResource(R.string.popup_none_yet),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                popups.sortedBy { it.name.lowercase() }.forEach { popup ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        popup.icon?.takeUnless { it.isBlank() }?.let { slug ->
                                            MdiIcon(slug, tint = appColors.onSurface, size = 20.dp)
                                            Spacer(Modifier.width(10.dp))
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                popup.name,
                                                color = appColors.onSurface,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            popup.statusEntityId?.let { statusId ->
                                                Text(
                                                    statusId,
                                                    color = appColors.onMuted,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        // Opens the popup itself, ready to arrange — the widgets live
                                        // in the dialog, not in a separate editor.
                                        TextButton(onClick = { viewModel.openCustomPopup(popup.id, startInEditMode = true) }) {
                                            Text(stringResource(R.string.popup_edit_contents))
                                        }
                                        TextButton(onClick = { editingPopup = popup }) {
                                            Text(stringResource(R.string.dlg_edit))
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = { editingPopup = viewModel.createCustomPopup(newPopupName) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.popup_new))
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
                                    stringResource(R.string.ui_home_and_rooms_are_always_shown_reorder_the_other_bea984a),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { editingPage = null; showPageEditor = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_create_custom_page_d1f53e7))
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
                        SettingsSection.LANGUAGE -> {
                            val selectedLanguage = currentAppLanguage(context)
                            val selectedSuffix = stringResource(R.string.language_selected)
                            val languages = listOf(
                                Triple(SYSTEM_LANGUAGE_TAG, stringResource(R.string.language_system), ""),
                                Triple("en", stringResource(R.string.settings_extra_language_english), ""),
                                Triple("nl", stringResource(R.string.settings_extra_language_dutch), stringResource(R.string.settings_extra_language_dutch_hint)),
                                Triple("de", stringResource(R.string.settings_extra_language_german), stringResource(R.string.settings_extra_language_german_hint)),
                                Triple("fr", stringResource(R.string.settings_extra_language_french), stringResource(R.string.settings_extra_language_french_hint)),
                                Triple("es", stringResource(R.string.settings_extra_language_spanish), stringResource(R.string.settings_extra_language_spanish_hint)),
                                Triple("it", stringResource(R.string.settings_extra_language_italian), stringResource(R.string.settings_extra_language_italian_hint)),
                                Triple("tr", stringResource(R.string.settings_extra_language_turkish), stringResource(R.string.settings_extra_language_turkish_hint)),
                                Triple("pt", stringResource(R.string.settings_extra_language_portuguese), stringResource(R.string.settings_extra_language_portuguese_hint)),
                                Triple("pt-BR", stringResource(R.string.settings_extra_language_portuguese_brazil), stringResource(R.string.settings_extra_language_portuguese_brazil_hint)),
                                Triple("es-419", stringResource(R.string.settings_extra_language_spanish_latam), stringResource(R.string.settings_extra_language_spanish_latam_hint)),
                                Triple("ja", stringResource(R.string.settings_extra_language_japanese), stringResource(R.string.settings_extra_language_japanese_hint)),
                                Triple("ko", stringResource(R.string.settings_extra_language_korean), stringResource(R.string.settings_extra_language_korean_hint)),
                                Triple("zh-CN", stringResource(R.string.settings_extra_language_chinese_simplified), stringResource(R.string.settings_extra_language_chinese_simplified_hint)),
                                Triple("zh-TW", stringResource(R.string.settings_extra_language_chinese_traditional), stringResource(R.string.settings_extra_language_chinese_traditional_hint))
                            )
                            SettingsSubcategory(
                                stringResource(R.string.language_display_title),
                                stringResource(R.string.language_display_subtitle)
                            )
                            languages.forEach { (tag, label, hint) ->
                                val subtitle = buildString {
                                    append(hint)
                                    if (tag == selectedLanguage) {
                                        if (isNotEmpty()) append(" · ")
                                        append(selectedSuffix)
                                    }
                                }
                                SettingsChoice(Icons.Default.Language, label, subtitle) {
                                    setAppLanguage(context, tag)
                                }
                            }
                        }
                        SettingsSection.THEME -> {
                            val forceHighRefresh by prefs.forceHighRefreshRate.collectAsState(initial = false)
                            val itemCornerRadius by prefs.itemCornerRadius.collectAsState(initial = 20)
                            SettingsPanel {
                                SettingsToggle(
                                    title = stringResource(R.string.ui_force_high_refresh_rate_214de79),
                                    subtitle = stringResource(R.string.ui_locks_the_screen_to_its_highest_refresh_rate_while_585256f),
                                    checked = forceHighRefresh,
                                    onCheckedChange = { scope.launch { prefs.saveForceHighRefreshRate(it) } }
                                )
                                val customRgb = remember(themeColor) { themeColorToRgb(themeColor) }
                                var localCustomRgb by remember(themeColor) { mutableStateOf(customRgb ?: listOf(155, 83, 83)) }
                                var customPickerOpen by remember(themeColor) { mutableStateOf(themeColor.startsWith("custom:")) }
                                Text(stringResource(R.string.ui_mode_a7b93d2), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                SettingsChipRow(
                                    options = listOf(
                                        "system" to stringResource(R.string.settings_extra_theme_system),
                                        "light" to stringResource(R.string.settings_extra_theme_light),
                                        "dark" to stringResource(R.string.settings_extra_theme_dark)
                                    ),
                                    selected = themeMode,
                                    onSelect = { scope.launch { prefs.saveThemeMode(it) } }
                                )
                                Text(stringResource(R.string.ui_color_1d0c830), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
                                SettingsChipRow(
                                    options = listOf(
                                        "system" to stringResource(R.string.settings_extra_theme_system),
                                        "rose" to stringResource(R.string.settings_extra_theme_rose),
                                        "green" to stringResource(R.string.settings_extra_theme_green),
                                        "blue" to stringResource(R.string.settings_extra_theme_blue),
                                        "amber" to stringResource(R.string.settings_extra_theme_amber),
                                        "custom" to stringResource(R.string.settings_extra_theme_custom)
                                    ),
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

                                    Text(stringResource(R.string.ui_system_light_theme_5bc7623), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
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

                                    Text(stringResource(R.string.ui_system_dark_theme_c7e4ff9), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
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
                        SettingsSection.CORNERS -> {
                            val itemCornerRadius by prefs.itemCornerRadius.collectAsState(initial = 20)
                            SettingsPanel {
                                Text(stringResource(R.string.ui_item_corner_roundness_5032003), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(R.string.ui_applies_to_all_dashboard_buttons_widgets_stacks_rooms_and_9715deb),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        8 to stringResource(R.string.settings_extra_corner_sharp),
                                        20 to stringResource(R.string.settings_extra_corner_modern),
                                        28 to stringResource(R.string.settings_extra_corner_round)
                                    ).forEach { (radius, label) ->
                                        SettingsChoiceChip(
                                            selected = itemCornerRadius == radius,
                                            onClick = { scope.launch { prefs.saveItemCornerRadius(radius) } },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                        SettingsSection.ICONS -> {
                            val iconAnimationsEnabled by prefs.iconAnimationsEnabled.collectAsState(initial = false)
                            val iconEffectDefaults by prefs.iconEffectDefaults.collectAsState(initial = emptyMap())
                            SettingsPanel {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.ui_animated_icons_aa39cfe), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            stringResource(R.string.ui_entity_icons_gently_glow_spin_or_pulse_while_the_b3a8910),
                                            color = appColors.onMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = iconAnimationsEnabled,
                                        onCheckedChange = { scope.launch { prefs.saveIconAnimationsEnabled(it) } }
                                    )
                                }
                            }
                            if (iconAnimationsEnabled) {
                                SettingsPanel {
                                    Text(stringResource(R.string.ui_default_effect_per_type_a4ce3c4), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(R.string.ui_the_effect_used_when_a_button_s_animation_is_7cce891),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconEffectGroups.forEach { (group, _) ->
                                        Text(
                                            localizedIconEffectGroup(group),
                                            color = appColors.onSurface,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        val current = iconEffectDefaults[group] ?: DefaultIconEffectByGroup.getValue(group)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                "glow" to stringResource(R.string.settings_extra_effect_glow),
                                                "spin" to stringResource(R.string.settings_extra_effect_spin),
                                                "pulse" to stringResource(R.string.settings_extra_effect_pulse),
                                                "none" to stringResource(R.string.settings_extra_effect_none)
                                            ).forEach { (value, chip) ->
                                                SettingsChoiceChip(
                                                    selected = current == value,
                                                    onClick = { scope.launch { prefs.saveIconEffectDefault(group, value) } },
                                                    label = { Text(chip) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.DASHBOARD -> {
                            LaunchedEffect(Unit) {
                                val id = runCatching { HaDashboardSharing.whoami(context) }.getOrNull()
                                sharingAvailable = id != null
                                isHaAdmin = id?.isAdmin == true
                                currentHaUserId = id?.userId
                                sharedWithMe = if (id != null)
                                    runCatching { HaDashboardSharing.listSharedForMe(context) }.getOrDefault(emptyList())
                                else emptyList()
                            }
                            SettingsPanel {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.ui_dashboards_197565b), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { dashboardEditMode = !dashboardEditMode }) {
                                        Icon(if (dashboardEditMode) Icons.Default.CheckCircle else Icons.Default.Edit, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (dashboardEditMode) stringResource(R.string.ui_done_e9b450d) else stringResource(R.string.ui_edit_5301648))
                                    }
                                }
                                dashboards.forEach { dashboard ->
                                    Surface(
                                        Modifier.fillMaxWidth().clickable(
                                            enabled = dashboard.id != activeDashboardId && (!familyDashboardSubscribed || allowDashboardSwitch)
                                        ) { viewModel.switchDashboard(dashboard.id) },
                                        shape = itemCornerShape(),
                                        color = if (dashboard.id == activeDashboardId) MaterialTheme.colorScheme.primaryContainer else appColors.subtleSurface
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(dashboard.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                Text(if (dashboard.id == activeDashboardId) stringResource(R.string.ui_currently_loaded_69ef6fb) else stringResource(R.string.ui_tap_to_load_0c49cab), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(
                                                onClick = { viewModel.setDefaultDashboard(dashboard.id) },
                                                enabled = !familyDashboardSubscribed || allowDashboardSwitch,
                                            ) {
                                                Icon(
                                                    if (dashboard.id == defaultDashboardId) Icons.Default.Star else Icons.Default.StarBorder,
                                                    stringResource(R.string.settings_extra_set_dashboard_default, dashboard.name)
                                                )
                                            }
                                            if (dashboardEditMode) {
                                                IconButton(onClick = { renameDashboard = dashboard }) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        stringResource(R.string.settings_extra_rename_dashboard, dashboard.name)
                                                    )
                                                }
                                                if (!familyDashboardSubscribed || allowDashboardCreate) {
                                                    IconButton(onClick = { copyDashboard = dashboard }) {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            stringResource(R.string.settings_extra_copy_dashboard, dashboard.name)
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { deleteDashboard = dashboard }, enabled = dashboards.size > 1) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        stringResource(R.string.settings_extra_delete_dashboard, dashboard.name),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!familyDashboardSubscribed || allowDashboardCreate) {
                                    Button(
                                        onClick = {
                                            newDashboardName = context.getString(
                                                R.string.settings_extra_dashboard_default_name,
                                                dashboards.size + 1
                                            )
                                            showNewConfigConfirm = true
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = itemCornerShape()
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.ui_new_dashboard_4d3c071))
                                    }
                                }
                            }
                            if (sharingAvailable && sharedWithMe.isNotEmpty()) {
                                SettingsPanel {
                                    Text(stringResource(R.string.ui_shared_with_me_f40ca84), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(R.string.ui_dashboards_shared_by_your_family_import_to_add_a_8a6d06e),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    sharedWithMe.forEach { meta ->
                                        Surface(
                                            Modifier.fillMaxWidth(),
                                            shape = itemCornerShape(),
                                            color = appColors.subtleSurface
                                        ) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(meta.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                    val updated = formatSharedUpdated(meta.updated)
                                                    Text(
                                                        if (updated.isNotBlank()) stringResource(R.string.ui_updated_62d2331, updated) else stringResource(R.string.ui_shared_dashboard_86876c0),
                                                        color = appColors.onMuted,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                TextButton(
                                                    enabled = !shareBusy,
                                                    onClick = {
                                                        shareBusy = true
                                                        scope.launch {
                                                            val localId = runCatching { HaDashboardSharing.import(context, prefs, meta) }.getOrNull()
                                                            setupChangedMessage = if (localId != null) {
                                                                context.getString(R.string.settings_extra_shared_dashboard_imported, meta.name)
                                                            } else {
                                                                context.getString(R.string.settings_extra_shared_dashboard_import_failed, meta.name)
                                                            }
                                                            shareBusy = false
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Download, null)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(stringResource(R.string.ui_import_d6fbc9d))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.FAMILY_SHARING -> {
                            val pcAreas by viewModel.areas.collectAsState()
                            val pcEntities by viewModel.entities.collectAsState()
                            // Every sensor the admin has assigned to somebody, so the mapping table
                            // can show the states actually being reported rather than a blank form.
                            val presenceRoster = remember(parentalPolicies) {
                                parentalPolicies.values.mapNotNull { it.roomFollow.sensorEntityId }.distinct()
                            }
                            val pcCustomPages by prefs.customPages.collectAsState(initial = emptyList())
                            // (route, label) — Home is always visible; Settings is not a nav tab.
                            val viewOptions = listOf(
                                "rooms" to stringResource(R.string.settings_extra_view_rooms),
                                "climate" to stringResource(R.string.settings_extra_view_climate),
                                "security" to stringResource(R.string.settings_extra_view_security),
                                "energy" to stringResource(R.string.settings_extra_view_energy),
                                "battery" to stringResource(R.string.settings_extra_view_battery)
                            ) + pcCustomPages.map { "custom_page/${it.id}" to it.name }
                            var familyTab by remember { mutableStateOf("parental") }
                            var searchAccessPicker by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
                            LaunchedEffect(Unit) {
                                val id = runCatching { HaDashboardSharing.whoami(context) }.getOrNull()
                                sharingAvailable = id != null
                                isHaAdmin = id?.isAdmin == true
                                sharedWithMe = if (id != null)
                                    runCatching { HaDashboardSharing.listSharedForMe(context) }.getOrDefault(emptyList())
                                else emptyList()
                                parentalUsers = runCatching { HaDashboardSharing.listUsers(context) }.getOrDefault(emptyList())
                                parentalPolicies = runCatching { HaParentalControls.listPolicies(context) }.getOrDefault(emptyMap())
                            }
                            val savePolicy: (String, Hki7Policy) -> Unit = { uid, policy ->
                                scope.launch {
                                    val result = runCatching { HaParentalControls.setPolicy(context, uid, policy) }
                                        .getOrDefault(Hki7PolicySaveResult.FAILED)
                                    if (result.isSaved) {
                                        parentalPolicies = parentalPolicies + (uid to policy)
                                        // Saving only wrote to the component. This device runs from
                                        // its own cached copy of its own policy, so without pulling
                                        // it back the change does not take effect here until the
                                        // next launch — which is why turning room following off, or
                                        // changing its dwell time, appeared to be ignored on the
                                        // very device the admin was editing from.
                                        runCatching {
                                            HaParentalControls.refreshForCurrentUser(context, prefs)
                                        }
                                    }
                                    setupChangedMessage = when (result) {
                                        Hki7PolicySaveResult.SAVED -> null
                                        // An out-of-date component saves the permissions but silently
                                        // drops the lists; say so instead of implying nothing saved.
                                        Hki7PolicySaveResult.SAVED_WITHOUT_SEARCH_ACCESS ->
                                            context.getString(R.string.settings_extra_policy_needs_component_update)
                                        Hki7PolicySaveResult.SAVED_WITHOUT_ROOM_FOLLOW ->
                                            context.getString(R.string.settings_extra_policy_needs_component_update_room_follow)
                                        Hki7PolicySaveResult.FAILED ->
                                            context.getString(R.string.settings_extra_policy_update_failed)
                                    }
                                }
                            }
                            searchAccessPicker?.let { (userId, visible) ->
                                val policy = parentalPolicies[userId] ?: Hki7Policy()
                                SearchAccessSelectionDialog(
                                    allEntities = pcEntities,
                                    title = stringResource(
                                        if (visible) R.string.parental_search_visible_title
                                        else R.string.parental_search_invisible_title
                                    ),
                                    initialSelection = SearchAccessSelection(
                                        domains = (if (visible) policy.visibleSearchDomains else policy.hiddenSearchDomains).toSet(),
                                        entityIds = (if (visible) policy.visibleSearchEntityIds else policy.hiddenSearchEntityIds).toSet(),
                                    ),
                                    onDismiss = { searchAccessPicker = null },
                                    onSave = { selection ->
                                        savePolicy(
                                            userId,
                                            if (visible) {
                                                policy.copy(
                                                    visibleSearchDomains = selection.domains.sorted(),
                                                    visibleSearchEntityIds = selection.entityIds.sorted(),
                                                )
                                            } else {
                                                policy.copy(
                                                    hiddenSearchDomains = selection.domains.sorted(),
                                                    hiddenSearchEntityIds = selection.entityIds.sorted(),
                                                )
                                            }
                                        )
                                    },
                                )
                            }
                            when {
                                !sharingAvailable -> {
                                    // The component isn't installed. The option is still shown so people
                                    // know it exists; it just explains what an admin needs to do first.
                                    SettingsPanel {
                                        Text(stringResource(R.string.ui_use_a_family_shared_dashboard_9b8ab51), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            stringResource(R.string.ui_an_admin_can_share_one_dashboard_with_the_whole_5a5434d),
                                            color = appColors.onMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        OutlinedButton(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            shape = itemCornerShape()
                                        ) {
                                            Icon(Icons.Default.Download, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.ui_use_family_shared_dashboard_28e06c2))
                                        }
                                        Hki7CloudInstallCard()
                                    }
                                }
                                !isHaAdmin -> {
                                    // Sibling (non-admin) users can't author family sharing — it's admin-only —
                                    // but they can choose to use a dashboard an admin shared with them, which
                                    // applies that dashboard's admin-set permissions to their app.
                                    SettingsPanel {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.Lock, null, tint = appColors.onMuted)
                                            Column(Modifier.weight(1f)) {
                                                Text(stringResource(R.string.ui_managed_by_an_admin_805ae65), color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    stringResource(R.string.ui_family_sharing_settings_are_controlled_by_a_home_assistant_cdb35a2),
                                                    color = appColors.onMuted,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    SettingsPanel {
                                        Text(stringResource(R.string.ui_use_a_family_shared_dashboard_9b8ab51), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                        if (sharedWithMe.isEmpty()) {
                                            Text(
                                                stringResource(R.string.ui_no_dashboards_have_been_shared_with_you_yet_ask_877080a),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            Text(
                                                stringResource(R.string.ui_using_a_shared_dashboard_applies_the_permissions_the_admin_a337a76),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            sharedWithMe.forEach { meta ->
                                                Surface(Modifier.fillMaxWidth(), shape = itemCornerShape(), color = appColors.subtleSurface) {
                                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(meta.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                            val updated = formatSharedUpdated(meta.updated)
                                                            Text(
                                                                if (updated.isNotBlank()) stringResource(R.string.ui_updated_62d2331, updated) else stringResource(R.string.ui_shared_dashboard_86876c0),
                                                                color = appColors.onMuted,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                        TextButton(
                                                            enabled = !shareBusy,
                                                            onClick = {
                                                                shareBusy = true
                                                                scope.launch {
                                                                    val localId = runCatching { HaDashboardSharing.import(context, prefs, meta) }.getOrNull()
                                                                    if (localId != null) {
                                                                        viewModel.useFamilyDashboard(localId)
                                                                        setupChangedMessage = context.getString(
                                                                            R.string.settings_extra_shared_dashboard_now_using,
                                                                            meta.name
                                                                        )
                                                                    } else {
                                                                        setupChangedMessage = context.getString(
                                                                            R.string.settings_extra_shared_dashboard_use_failed,
                                                                            meta.name
                                                                        )
                                                                    }
                                                                    shareBusy = false
                                                                }
                                                            }
                                                        ) {
                                                            Icon(Icons.Default.Download, null)
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(stringResource(R.string.ui_use_1d4d43c))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    SettingsPanel {
                                        SettingsTabRow(
                                            tabs = listOf(
                                                "parental" to stringResource(R.string.settings_extra_tab_parental_controls),
                                                "dashboards" to stringResource(R.string.settings_extra_tab_dashboards),
                                                "permissions" to stringResource(R.string.settings_extra_tab_permissions),
                                                "presence" to stringResource(R.string.settings_extra_tab_presence)
                                            ),
                                            selected = familyTab,
                                            onSelect = { familyTab = it }
                                        )
                                    }
                                    if (familyTab == "parental") {
                                        SettingsPanel {
                                            Text(stringResource(R.string.ui_parental_controls_c4f61d0), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                stringResource(R.string.ui_hide_certain_views_or_rooms_from_specific_people_to_f10458c),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            val nonAdmin = parentalUsers.filter { !it.isAdmin }
                                            if (nonAdmin.isEmpty()) {
                                                Text(stringResource(R.string.ui_no_non_admin_users_found_on_this_home_assistant_e9e665d), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            nonAdmin.forEach { user ->
                                                val policy = parentalPolicies[user.id] ?: com.jimz011apps.hki7.data.Hki7Policy()
                                                val expanded = parentalExpandedUser == user.id
                                                val hiddenCount = policy.hiddenViews.size + policy.hiddenRooms.size +
                                                    policy.hiddenSearchDomains.size + policy.hiddenSearchEntityIds.size
                                                Surface(
                                                    Modifier.fillMaxWidth(),
                                                    shape = itemCornerShape(),
                                                    color = appColors.subtleSurface
                                                ) {
                                                    Column(Modifier.padding(12.dp)) {
                                                        Row(
                                                            Modifier.fillMaxWidth().clickable {
                                                                parentalExpandedUser = if (expanded) null else user.id
                                                            },
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(Modifier.weight(1f)) {
                                                                Text(user.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                                Text(
                                                                    if (hiddenCount == 0) stringResource(R.string.ui_nothing_hidden_fd1cdd3) else stringResource(R.string.ui_hidden_1364716, hiddenCount),
                                                                    color = appColors.onMuted,
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = appColors.onMuted)
                                                        }
                                                        if (expanded) {
                                                            Spacer(Modifier.height(8.dp))
                                                            Text(stringResource(R.string.ui_hidden_views_3e16d85), color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                                                            viewOptions.forEach { (route, label) ->
                                                                val hidden = route in policy.hiddenViews
                                                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(label, color = appColors.onSurface, modifier = Modifier.weight(1f))
                                                                    Switch(
                                                                        checked = hidden,
                                                                        onCheckedChange = {
                                                                            val nv = if (hidden) policy.hiddenViews - route else policy.hiddenViews + route
                                                                            savePolicy(user.id, policy.copy(hiddenViews = nv))
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            if (pcAreas.isNotEmpty()) {
                                                                Spacer(Modifier.height(4.dp))
                                                                Text(stringResource(R.string.ui_hidden_rooms_f621ba4), color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                                                                pcAreas.forEach { area ->
                                                                    val hidden = area.area_id in policy.hiddenRooms
                                                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(area.name, color = appColors.onSurface, modifier = Modifier.weight(1f))
                                                                        Switch(
                                                                            checked = hidden,
                                                                            onCheckedChange = {
                                                                                val nr = if (hidden) policy.hiddenRooms - area.area_id else policy.hiddenRooms + area.area_id
                                                                                savePolicy(user.id, policy.copy(hiddenRooms = nr))
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(Modifier.height(8.dp))
                                                            Text(stringResource(R.string.parental_search_access_title), color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
                                                            Text(
                                                                stringResource(R.string.parental_search_access_hint),
                                                                color = appColors.onMuted,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                            ParentalSearchAccessList(
                                                                title = stringResource(R.string.parental_search_visible_title),
                                                                domains = policy.visibleSearchDomains,
                                                                entityIds = policy.visibleSearchEntityIds,
                                                                allEntities = pcEntities,
                                                                emptyText = stringResource(R.string.parental_search_visible_empty),
                                                                onChange = { searchAccessPicker = user.id to true },
                                                            )
                                                            ParentalSearchAccessList(
                                                                title = stringResource(R.string.parental_search_invisible_title),
                                                                domains = policy.hiddenSearchDomains,
                                                                entityIds = policy.hiddenSearchEntityIds,
                                                                allEntities = pcEntities,
                                                                emptyText = stringResource(R.string.parental_search_invisible_empty),
                                                                onChange = { searchAccessPicker = user.id to false },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (familyTab == "dashboards") {
                                        SettingsPanel {
                                            Text(stringResource(R.string.ui_share_dashboards_0917ff1), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                stringResource(R.string.ui_publish_one_of_your_dashboards_to_your_family_recipients_88c1d49),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            dashboards.forEach { dashboard ->
                                                Surface(
                                                    Modifier.fillMaxWidth(),
                                                    shape = itemCornerShape(),
                                                    color = appColors.subtleSurface
                                                ) {
                                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(dashboard.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                            Text(stringResource(R.string.ui_tap_share_to_publish_or_update_d185341), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        TextButton(onClick = {
                                                            shareDashboard = dashboard
                                                            shareSelected = emptySet()
                                                            shareEveryone = false
                                                            shareUsers = emptyList()
                                                        }) {
                                                            Icon(Icons.Default.Share, null)
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(stringResource(R.string.ui_share_09ca55c))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        // Already-published dashboards live in the cloud, so they show up
                                        // here even after a reinstall — the admin can re-import them to
                                        // edit, or delete them from the cloud entirely.
                                        SettingsPanel {
                                            Text(stringResource(R.string.ui_published_to_your_family_33fc8da), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                            if (sharedWithMe.isEmpty()) {
                                                Text(stringResource(R.string.ui_nothing_is_shared_yet_fca61d0), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                            } else {
                                                Text(
                                                    stringResource(R.string.ui_these_are_stored_in_the_cloud_import_one_to_558c62d),
                                                    color = appColors.onMuted,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Button(
                                                    enabled = !shareBusy,
                                                    onClick = {
                                                        shareBusy = true
                                                        scope.launch {
                                                            val pushed = runCatching { HaDashboardSharing.pushOwnedUpdates(context, prefs) }.getOrDefault(0)
                                                            sharedWithMe = runCatching { HaDashboardSharing.listSharedForMe(context) }.getOrDefault(sharedWithMe)
                                                            setupChangedMessage = when {
                                                                pushed > 0 -> context.resources.getQuantityString(
                                                                    R.plurals.settings_extra_shared_dashboards_pushed,
                                                                    pushed,
                                                                    pushed
                                                                )
                                                                else -> context.getString(R.string.settings_extra_shared_dashboards_up_to_date)
                                                            }
                                                            shareBusy = false
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(if (shareBusy) stringResource(R.string.ui_pushing_3c52088) else stringResource(R.string.ui_push_my_changes_now_ddff3f6))
                                                }
                                            }
                                            sharedWithMe.forEach { meta ->
                                                Surface(Modifier.fillMaxWidth(), shape = itemCornerShape(), color = appColors.subtleSurface) {
                                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(meta.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                            val updated = formatSharedUpdated(meta.updated)
                                                            Text(
                                                                if (updated.isNotBlank()) stringResource(R.string.ui_updated_62d2331, updated) else stringResource(R.string.ui_shared_dashboard_86876c0),
                                                                color = appColors.onMuted,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                        TextButton(
                                                            enabled = !shareBusy,
                                                            onClick = {
                                                                shareBusy = true
                                                                scope.launch {
                                                                    val localId = runCatching { HaDashboardSharing.import(context, prefs, meta) }.getOrNull()
                                                                    setupChangedMessage = if (localId != null) {
                                                                        context.getString(R.string.settings_extra_shared_dashboard_imported, meta.name)
                                                                    } else {
                                                                        context.getString(R.string.settings_extra_shared_dashboard_import_failed, meta.name)
                                                                    }
                                                                    shareBusy = false
                                                                }
                                                            }
                                                        ) { Text(stringResource(R.string.ui_import_d6fbc9d)) }
                                                        if (meta.ownerId == currentHaUserId) {
                                                            TextButton(
                                                                enabled = !shareBusy,
                                                                onClick = { pendingUnpublish = meta }
                                                            ) { Text(stringResource(R.string.ui_delete_f6fdbe4), color = MaterialTheme.colorScheme.error) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (familyTab == "permissions") {
                                        SettingsPanel {
                                            Text(stringResource(R.string.ui_permissions_d06d555), color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                stringResource(R.string.ui_set_what_each_person_may_do_per_user_like_aa86ea2),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            val nonAdmin = parentalUsers.filter { !it.isAdmin }
                                            if (nonAdmin.isEmpty()) {
                                                Text(stringResource(R.string.ui_no_non_admin_users_found_on_this_home_assistant_e9e665d), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            nonAdmin.forEach { user ->
                                                val policy = parentalPolicies[user.id] ?: Hki7Policy()
                                                Surface(
                                                    Modifier.fillMaxWidth(),
                                                    shape = itemCornerShape(),
                                                    color = appColors.subtleSurface
                                                ) {
                                                    Column(Modifier.padding(12.dp)) {
                                                        Text(user.name, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.family_permission_switch_dashboards),
                                                            subtitle = stringResource(R.string.family_permission_switch_dashboards_subtitle),
                                                            checked = policy.allowDashboardSwitch
                                                        ) { savePolicy(user.id, policy.copy(allowDashboardSwitch = it)) }
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.family_permission_create_dashboards),
                                                            subtitle = stringResource(R.string.family_permission_create_dashboards_subtitle),
                                                            checked = policy.allowDashboardCreate
                                                        ) { savePolicy(user.id, policy.copy(allowDashboardCreate = it)) }
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.family_permission_reimport),
                                                            subtitle = stringResource(R.string.family_permission_reimport_subtitle),
                                                            checked = policy.allowReimport
                                                        ) { savePolicy(user.id, policy.copy(allowReimport = it)) }
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.ui_allow_editing_24f2b54),
                                                            subtitle = stringResource(R.string.ui_let_this_person_enter_edit_mode_667df39),
                                                            checked = policy.allowEdit
                                                        ) { savePolicy(user.id, policy.copy(allowEdit = it)) }
                                                        if (policy.allowEdit) {
                                                            FamilyPermissionRow(
                                                                title = stringResource(R.string.ui_aesthetic_changes_only_f31f444),
                                                                subtitle = stringResource(R.string.ui_allow_theme_colors_icons_names_and_wallpaper_but_not_efc8a97),
                                                                checked = policy.aestheticsOnly
                                                            ) { savePolicy(user.id, policy.copy(aestheticsOnly = it)) }
                                                        }
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.ui_show_global_search_e7aefbe),
                                                            subtitle = stringResource(R.string.ui_show_the_global_search_button_0517d8b),
                                                            checked = policy.showGlobalSearch
                                                        ) { savePolicy(user.id, policy.copy(showGlobalSearch = it)) }
                                                        FamilyPermissionRow(
                                                            title = stringResource(R.string.ui_show_flows_button_9e888be),
                                                            subtitle = stringResource(R.string.ui_show_the_automations_flows_button_16254ce),
                                                            checked = policy.showFlows
                                                        ) { savePolicy(user.id, policy.copy(showFlows = it)) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (familyTab == "presence") {
                                        SettingsPanel {
                                            Text(
                                                stringResource(R.string.settings_extra_room_follow_title),
                                                color = appColors.onSurface,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                stringResource(R.string.settings_extra_room_follow_description),
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            // Unlike the other tabs this lists admins too: an admin
                                            // carries a phone and gets followed like anyone else.
                                            if (parentalUsers.isEmpty()) {
                                                Text(
                                                    stringResource(R.string.ui_no_non_admin_users_found_on_this_home_assistant_e9e665d),
                                                    color = appColors.onMuted,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            parentalUsers.forEach { user ->
                                                val policy = parentalPolicies[user.id] ?: Hki7Policy()
                                                RoomFollowUserCard(
                                                    userName = user.name,
                                                    follow = policy.roomFollow,
                                                    allEntities = pcEntities,
                                                    areas = pcAreas,
                                                    roster = presenceRoster,
                                                    onChange = { updated ->
                                                        savePolicy(user.id, policy.copy(roomFollow = updated))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.BACKUP_RESTORE -> {
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_backups_contain_dashboard_and_appearance_configuration_onl_d5856be),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(stringResource(R.string.ui_on_this_device_a7f9620), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Button(
                                    onClick = { backupLauncher.launch(hki7BackupName()) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Backup, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_backup_dd96994))
                                }
                                OutlinedButton(
                                    onClick = { showRestoreSource = true },
                                    enabled = !familyDashboardSubscribed || allowDashboardSwitch,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.Sync, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_restore_3cbe6d6))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.ui_automatic_cloud_backup_494d882), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(R.string.ui_uses_hki_7_s_private_google_drive_storage_enable_0559ced),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.ui_enable_cloud_backup_32fe82b), color = appColors.onSurface)
                                        Text(
                                            when {
                                                !cloudBackupEnabled -> stringResource(R.string.ui_cloud_backup_is_off_71dfae1)
                                                cloudBackupLastAt != null -> stringResource(R.string.ui_last_backup_05509f8, relativeBackupTime(cloudBackupLastAt!!))
                                                else -> stringResource(R.string.ui_daily_backup_is_active_111a631)
                                            },
                                            color = appColors.onMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = cloudBackupEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) requestDriveAuthorization() else scope.launch {
                                                prefs.saveCloudBackup(false)
                                                if (!haBackupEnabled) CloudBackupWork.cancel(context)
                                            }
                                        }
                                    )
                                }
                                if (cloudBackupEnabled) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                cloudBackupNowBusy = true
                                                runCatching { CloudBackupStorage.write(context, prefs.exportUiBackup()) }
                                                    .onSuccess {
                                                        prefs.saveCloudBackupLastAt(System.currentTimeMillis())
                                                        setupChangedMessage = context.getString(R.string.settings_extra_cloud_backup_created)
                                                    }
                                                    .onFailure {
                                                        setupChangedMessage = context.getString(
                                                            R.string.settings_extra_cloud_backup_failed,
                                                            it.message ?: context.getString(R.string.settings_extra_unknown_error)
                                                        )
                                                    }
                                                cloudBackupNowBusy = false
                                            }
                                        },
                                        enabled = !cloudBackupNowBusy,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = itemCornerShape()
                                    ) {
                                        Icon(Icons.Default.Backup, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (cloudBackupNowBusy) stringResource(R.string.ui_backing_up_f600558) else stringResource(R.string.ui_back_up_now_527bf1a))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.ui_automatic_local_cloud_backup_7aef407), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(R.string.ui_automatically_backs_up_your_dashboard_and_appearance_setti_cd89812),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (!sharingAvailable) {
                                    Hki7CloudInstallCard()
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.ui_enable_home_assistant_backup_bd7cf62), color = appColors.onSurface)
                                        Text(
                                            when {
                                                haBackupBusy -> stringResource(R.string.ui_checking_for_the_hki_7_cloud_component_c76112a)
                                                haBackupEnabled && haBackupLastAt != null -> stringResource(R.string.ui_last_backup_05509f8, relativeBackupTime(haBackupLastAt!!))
                                                haBackupEnabled -> stringResource(R.string.ui_daily_backup_is_active_111a631)
                                                else -> stringResource(R.string.ui_home_assistant_backup_is_off_4524f2e)
                                            },
                                            color = appColors.onMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = haBackupEnabled,
                                        enabled = !haBackupBusy,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                haBackupBusy = true
                                                scope.launch {
                                                    val available = runCatching { HaBackupStorage.isAvailable(context) }.getOrDefault(false)
                                                    if (available) {
                                                        prefs.saveHaBackup(true)
                                                        CloudBackupWork.schedule(context)
                                                        setupChangedMessage = context.getString(R.string.settings_extra_ha_backup_enabled)
                                                    } else {
                                                        setupChangedMessage = context.getString(R.string.settings_extra_cloud_component_missing)
                                                    }
                                                    haBackupBusy = false
                                                }
                                            } else scope.launch {
                                                prefs.saveHaBackup(false)
                                                if (!cloudBackupEnabled) CloudBackupWork.cancel(context)
                                            }
                                        }
                                    )
                                }
                                if (haBackupEnabled) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                haBackupNowBusy = true
                                                val ok = runCatching { HaBackupStorage.write(context) }.getOrDefault(false)
                                                if (ok) {
                                                    prefs.saveHaBackupLastAt(System.currentTimeMillis())
                                                    setupChangedMessage = context.getString(R.string.settings_extra_ha_backup_created)
                                                } else {
                                                    setupChangedMessage = context.getString(R.string.settings_extra_ha_backup_failed)
                                                }
                                                haBackupNowBusy = false
                                            }
                                        },
                                        enabled = !haBackupNowBusy,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = itemCornerShape()
                                    ) {
                                        Icon(Icons.Default.Backup, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (haBackupNowBusy) stringResource(R.string.ui_backing_up_f600558) else stringResource(R.string.ui_back_up_now_527bf1a))
                                    }
                                }
                            }
                        }
                        SettingsSection.ABOUT -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // The current app logo — the adaptive launcher icon composed as a
                                // rounded tile (blue gradient background + the house/7/HKI foreground),
                                // replacing the older standalone hki_logo_round artwork.
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF2B72D4), Color(0xFF123A96)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = stringResource(R.string.ui_hki_7_logo_a445c44),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    stringResource(R.string.ui_hki_7_68a9e17),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.ui_a_modern_touch_first_android_dashboard_and_companion_for_814cbdb),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_what_it_does_ca032b5))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_hki_7_turns_your_native_home_assistant_entities_rooms_c95b345),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_technology_d018b08), stringResource(R.string.ui_languages_and_frameworks_used_by_the_android_app_fb4ac97))
                            SettingsPanel {
                                SettingsTile(
                                    Icons.Default.PhoneAndroid,
                                    stringResource(R.string.settings_extra_technology_kotlin),
                                    stringResource(R.string.settings_primary_app_language)
                                )
                                SettingsTile(
                                    Icons.Default.Description,
                                    stringResource(R.string.settings_extra_technology_xml_kotlin_dsl),
                                    stringResource(R.string.settings_android_build_configuration)
                                )
                                Text(
                                    stringResource(R.string.ui_the_interface_is_built_with_jetpack_compose_coroutines_and_fe7f52d),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_created_openly_1cc16d4))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_created_by_jimz011_with_help_from_ai_assisted_development_47c9a41),
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
                                    Text(stringResource(R.string.ui_view_on_github_0c77991))
                                }
                                OutlinedButton(
                                    onClick = { showWhatsNew = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_what_s_new_4d8dc5f))
                                }
                                OutlinedButton(
                                    onClick = { openGitHub(context, HKI7_CHANGELOG_URL) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = itemCornerShape()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_full_changelog_ae838e7))
                                }
                            }
                        }
                        SettingsSection.LICENSE -> {
                            SettingsSubcategory(stringResource(R.string.ui_community_source_716abd9), stringResource(R.string.ui_mozilla_public_license_2_0_mpl_2_0_4ec8335))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_copyright_2026_jimz011_b8f2c24),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.ui_the_hki_7_community_source_code_is_free_and_f4f3f51),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.ui_this_source_code_form_is_subject_to_the_terms_c8e4e24),
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
                                    Text(stringResource(R.string.ui_read_the_full_mpl_2_0_4d68c7d))
                                }
                            }
                            SettingsSubcategory(stringResource(R.string.ui_optional_premium_5ae8183), stringResource(R.string.ui_separate_commercial_content_0408620))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_premium_icon_packs_animated_icons_and_artwork_premium_them_6363fef),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.ui_the_open_source_community_core_remains_usable_without_prem_d83ff8a),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_brand_and_third_party_work_8d6d517))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_the_hki_7_name_and_logos_are_project_trademarks_985ffec),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        SettingsSection.SUPPORT -> {
                            SettingsSubcategory(stringResource(R.string.ui_support_without_premium_0a4aebd), stringResource(R.string.ui_every_contribution_helps_and_payment_is_never_required_1bc44f2))
                            SettingsPanel {
                                Text(
                                    stringResource(R.string.ui_you_can_help_by_testing_new_builds_reporting_reproducible_5310461),
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.ui_when_reporting_a_problem_include_your_android_version_home_784a4e6),
                                    color = appColors.onMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SettingsSubcategory(stringResource(R.string.ui_leave_a_tip_5f90f10), stringResource(R.string.ui_optional_support_for_development_and_testing_costs_5d6af95))
                            SupportLinkCard(
                                imageUrl = "https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png",
                                imageDescription = stringResource(R.string.settings_extra_buy_me_a_coffee_logo),
                                label = stringResource(R.string.ui_support_via_buy_me_a_coffee_d7dadd1),
                                onClick = { openExternalUrl(context, "https://www.buymeacoffee.com/w8Jnf6Hit") }
                            )
                            SupportLinkCard(
                                imageUrl = "https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg",
                                imageDescription = stringResource(R.string.settings_extra_paypal_logo),
                                label = stringResource(R.string.ui_support_via_paypal_7184a5d),
                                onClick = { openExternalUrl(context, "https://paypal.me/JimmySchings") }
                            )
                            Text(
                                stringResource(R.string.ui_these_links_open_in_your_browser_tips_do_not_049b881),
                                color = appColors.onMuted,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            )
                        }
                        SettingsSection.ACCOUNT -> {
                            SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_personal_information_used_throughout_hki_7_b7d1b08))
                            SettingsChoice(Icons.Default.Person, stringResource(R.string.ui_profile_ff4fc02), displayName) { section = SettingsSection.PROFILE }
                            val isDemoSession = com.jimz011apps.hki7.data.isDemoServerUrl(viewModel.currentUrl.collectAsState().value)
                            if (isDemoSession) {
                                SettingsSubcategory(stringResource(R.string.ui_demo_mode_a85b728), stringResource(R.string.ui_you_re_exploring_the_built_in_sample_home_63c2ad1))
                                SettingsPanel {
                                    OutlinedButton(
                                        onClick = { viewModel.logout(keepConfig = false); onDismiss() },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = itemCornerShape(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.ui_exit_demo_mode_ab94122))
                                    }
                                    Text(
                                        stringResource(R.string.ui_exiting_removes_the_sample_home_and_returns_to_the_05548e1),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                SettingsSubcategory(stringResource(R.string.ui_session_f7f1997), stringResource(R.string.ui_sign_out_safely_or_reset_this_installation_681fac2))
                                SettingsPanel {
                                    OutlinedButton(
                                        onClick = { viewModel.logout(keepConfig = true); onDismiss() },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = itemCornerShape(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.ui_logout_keep_config_32a0cfb))
                                    }
                                    TextButton(
                                        onClick = { viewModel.logout(keepConfig = false); onDismiss() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.ui_reset_everything_2d6cd07), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.ui_created_by_jimz011_2026_hki_7_v_20cbca0, BuildConfig.VERSION_NAME),
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
            title = { Text(stringResource(R.string.ui_restore_backup_a65eaa8)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.ui_choose_where_to_restore_the_dashboard_configuration_from_bb40b34))
                    TextButton(
                        onClick = {
                            showRestoreSource = false
                            restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.ui_local_file_576d5ac)) }
                    TextButton(
                        onClick = {
                            showRestoreSource = false
                            scope.launch {
                                runCatching { CloudBackupStorage.backups(context) }
                                    .onSuccess { backups ->
                                        cloudRestoreFiles = backups
                                        if (backups.isEmpty()) {
                                            setupChangedMessage = context.getString(R.string.settings_extra_no_drive_backups)
                                        }
                                        else showCloudRestore = true
                                    }
                                    .onFailure {
                                        setupChangedMessage = context.getString(
                                            R.string.settings_extra_drive_backups_load_failed,
                                            it.message ?: context.getString(R.string.settings_extra_unknown_error)
                                        )
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.ui_google_drive_07c2964)) }
                    TextButton(
                        onClick = {
                            showRestoreSource = false
                            scope.launch {
                                runCatching { HaBackupStorage.list(context) }
                                    .onSuccess { backups ->
                                        haRestoreFiles = backups
                                        if (backups.isEmpty()) {
                                            setupChangedMessage = context.getString(R.string.settings_extra_no_ha_backups)
                                        }
                                        else showHaRestore = true
                                    }
                                    .onFailure {
                                        setupChangedMessage = context.getString(
                                            R.string.settings_extra_ha_backups_load_failed,
                                            it.message ?: context.getString(R.string.settings_extra_unknown_error)
                                        )
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.ui_home_assistant_c8fd3bb)) }
                }
            },
            confirmButton = { TextButton(onClick = { showRestoreSource = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    if (showHaRestore) {
        AlertDialog(
            onDismissRequest = { showHaRestore = false },
            title = { Text(stringResource(R.string.ui_restore_from_home_assistant_13aec1d)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    haRestoreFiles.forEach { meta ->
                        val label = formatSharedUpdated(meta.created).ifBlank { meta.id }
                        TextButton(
                            onClick = {
                                showHaRestore = false
                                scope.launch {
                                    runCatching {
                                        val raw = HaBackupStorage.read(context, meta.id)
                                            ?: error(context.getString(R.string.settings_extra_backup_unreadable))
                                        prefs.restoreUiBackup(raw)
                                    }.onSuccess {
                                        setupChangedMessage = context.getString(R.string.settings_extra_dashboard_restored)
                                    }.onFailure {
                                        setupChangedMessage = context.getString(
                                            R.string.settings_extra_restore_failed,
                                            it.message ?: context.getString(R.string.settings_extra_unknown_error)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (meta.label.isNotBlank()) stringResource(R.string.ui_text_c1aacd9, meta.label, label) else label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHaRestore = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    shareDashboard?.let { dash ->
        LaunchedEffect(dash.id) {
            shareUsers = runCatching { HaDashboardSharing.listUsers(context) }.getOrDefault(emptyList())
        }
        AlertDialog(
            onDismissRequest = { if (!shareBusy) shareDashboard = null },
            title = { Text(stringResource(R.string.ui_share_b300e5f, dash.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.ui_choose_who_can_see_this_dashboard_they_can_import_2f5e49d),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        Modifier.fillMaxWidth().clickable { shareEveryone = !shareEveryone },
                        shape = itemCornerShape(),
                        color = if (shareEveryone) MaterialTheme.colorScheme.primaryContainer else appColors.subtleSurface
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ui_everyone_c756f6a), color = appColors.onSurface, modifier = Modifier.weight(1f))
                            if (shareEveryone) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (!shareEveryone) {
                        shareUsers.forEach { user ->
                            val selected = user.id in shareSelected
                            Surface(
                                Modifier.fillMaxWidth().clickable {
                                    shareSelected = if (selected) shareSelected - user.id else shareSelected + user.id
                                },
                                shape = itemCornerShape(),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else appColors.subtleSurface
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (user.isAdmin) stringResource(R.string.ui_admin_b38222e, user.name) else user.name,
                                        color = appColors.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (shareUsers.isEmpty()) {
                            Text(stringResource(R.string.ui_no_other_users_found_on_this_home_assistant_cb89d3d), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !shareBusy && (shareEveryone || shareSelected.isNotEmpty()),
                    onClick = {
                        shareBusy = true
                        val recipients = if (shareEveryone) listOf(HaDashboardSharing.EVERYONE) else shareSelected.toList()
                        scope.launch {
                            val meta = runCatching {
                                HaDashboardSharing.publish(
                                    context, prefs,
                                    localDashboardId = dash.id,
                                    name = dash.name,
                                    sharedWith = recipients,
                                    existingSharedId = dash.id,
                                )
                            }.getOrNull()
                            setupChangedMessage = if (meta != null) {
                                context.getString(R.string.settings_extra_dashboard_shared, dash.name)
                            } else {
                                context.getString(R.string.settings_extra_dashboard_sharing_failed)
                            }
                            shareBusy = false
                            shareDashboard = null
                        }
                    }
                ) { Text(if (shareBusy) stringResource(R.string.ui_sharing_ad00590) else stringResource(R.string.ui_share_09ca55c)) }
            },
            dismissButton = { TextButton(enabled = !shareBusy, onClick = { shareDashboard = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    if (showCloudRestore) {
        AlertDialog(
            onDismissRequest = { showCloudRestore = false },
            title = { Text(stringResource(R.string.ui_restore_from_cloud_52b6663)) },
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
                                    }.onSuccess {
                                        setupChangedMessage = context.getString(R.string.settings_extra_dashboard_restored)
                                    }.onFailure {
                                        setupChangedMessage = context.getString(
                                            R.string.settings_extra_restore_failed,
                                            it.message ?: context.getString(R.string.settings_extra_unknown_error)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(file.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCloudRestore = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    if (showNewConfigConfirm) {
        AlertDialog(
            onDismissRequest = { showNewConfigConfirm = false },
            title = { Text(stringResource(R.string.ui_start_new_dashboard_8c71127)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ui_create_a_separate_dashboard_auto_generate_imports_once_and_a5f0ef1))
                    OutlinedTextField(newDashboardName, { newDashboardName = it }, label = { Text(stringResource(R.string.ui_dashboard_name_466f3af)) }, singleLine = true)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.createDashboard(newDashboardName, auto = true)
                        showNewConfigConfirm = false
                        setupChangedMessage = context.getString(R.string.settings_extra_dashboard_generating)
                    }) { Text(stringResource(R.string.ui_auto_generate_0f86c24)) }
                    Button(onClick = {
                        viewModel.createDashboard(newDashboardName, auto = false)
                        showNewConfigConfirm = false
                        setupChangedMessage = context.getString(R.string.settings_extra_empty_dashboard_created)
                    }) { Text(stringResource(R.string.ui_start_empty_888db50)) }
                }
            },
            dismissButton = { TextButton(onClick = { showNewConfigConfirm = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    renameDashboard?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { renameDashboard = null },
            title = { Text(stringResource(R.string.ui_rename_dashboard_ceb1d6d)) },
            text = { OutlinedTextField(newDashboardName, { newDashboardName = it }, label = { Text(stringResource(R.string.ui_name_709a232)) }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameDashboard(dashboard.id, newDashboardName)
                    renameDashboard = null
                }) { Text(stringResource(R.string.ui_save_efc007a)) }
            },
            dismissButton = { TextButton(onClick = { renameDashboard = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    LaunchedEffect(renameDashboard?.id) { renameDashboard?.let { newDashboardName = it.name } }

    copyDashboard?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { copyDashboard = null },
            title = { Text(stringResource(R.string.ui_copy_dashboard_2ea354e)) },
            text = {
                Column {
                    Text(stringResource(R.string.ui_creates_a_full_duplicate_of_including_all_rooms_widgets_b96bbb1, dashboard.name))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(newDashboardName, { newDashboardName = it }, label = { Text(stringResource(R.string.ui_name_709a232)) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.copyDashboard(dashboard.id, newDashboardName)
                    copyDashboard = null
                }) { Text(stringResource(R.string.ui_copy_af74f7c)) }
            },
            dismissButton = { TextButton(onClick = { copyDashboard = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    LaunchedEffect(copyDashboard?.id) {
        copyDashboard?.let {
            newDashboardName = context.getString(R.string.settings_extra_dashboard_copy_name, it.name)
        }
    }

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }

    pendingUnpublish?.let { meta ->
        AlertDialog(
            onDismissRequest = { pendingUnpublish = null },
            title = { Text(stringResource(R.string.ui_delete_0c6013a, meta.name)) },
            text = { Text(stringResource(R.string.ui_this_removes_the_shared_dashboard_from_the_cloud_for_4bab01c)) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = meta
                        pendingUnpublish = null
                        shareBusy = true
                        scope.launch {
                            val ok = runCatching { HaDashboardSharing.delete(context, target.id) }.getOrDefault(false)
                            sharedWithMe = runCatching { HaDashboardSharing.listSharedForMe(context) }.getOrDefault(sharedWithMe)
                            setupChangedMessage = if (ok) {
                                context.getString(R.string.settings_extra_shared_dashboard_deleted, target.name)
                            } else {
                                context.getString(R.string.settings_extra_shared_dashboard_delete_failed, target.name)
                            }
                            shareBusy = false
                        }
                    }
                ) { Text(stringResource(R.string.ui_delete_f6fdbe4)) }
            },
            dismissButton = { TextButton(onClick = { pendingUnpublish = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    deleteDashboard?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { deleteDashboard = null },
            title = { Text(stringResource(R.string.ui_delete_137cdc2, dashboard.name)) },
            text = { Text(stringResource(R.string.ui_this_permanently_removes_this_dashboard_and_its_room_and_8087734)) },
            confirmButton = { Button(onClick = { viewModel.deleteDashboard(dashboard.id); deleteDashboard = null }) { Text(stringResource(R.string.ui_delete_f6fdbe4)) } },
            dismissButton = { TextButton(onClick = { deleteDashboard = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    setupChangedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { setupChangedMessage = null },
            title = { Text(stringResource(R.string.ui_setup_changed_b184bf0)) },
            text = { Text(message) },
            confirmButton = { Button(onClick = { setupChangedMessage = null }) { Text(stringResource(R.string.ui_ok_9ce3bd4)) } }
        )
    }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { if (!restartBusy) showRestartConfirm = false },
            title = { Text(stringResource(R.string.ui_restart_home_assistant_0206bf0)) },
            text = { Text(stringResource(R.string.ui_your_automations_and_devices_will_be_unavailable_briefly_h_8a8572e)) },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }, enabled = !restartBusy) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
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
                                    homeAssistantMessage = it.message
                                        ?: context.getString(R.string.settings_extra_restart_home_assistant_failed)
                                }
                            restartBusy = false
                        }
                    }
                ) { Text(if (restartBusy) stringResource(R.string.ui_restarting_b86eee1) else stringResource(R.string.ui_restart_b134bd5)) }
            }
        )
    }

    homeAssistantMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { homeAssistantMessage = null },
            title = { Text(stringResource(R.string.ui_home_assistant_c8fd3bb)) },
            text = { Text(message) },
            confirmButton = { Button(onClick = { homeAssistantMessage = null }) { Text(stringResource(R.string.ui_ok_9ce3bd4)) } }
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
            title = { Text(stringResource(R.string.ui_rename_home_assistant_f1a0d6f)) },
            text = {
                OutlinedTextField(
                    value = homeAssistantInstanceName,
                    onValueChange = { homeAssistantInstanceName = it },
                    label = { Text(stringResource(R.string.ui_instance_name_e98a2eb)) },
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
                ) { Text(stringResource(R.string.ui_save_efc007a)) }
            },
            dismissButton = { TextButton(onClick = { renameHomeAssistantInstance = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    deleteHomeAssistantInstance?.let { instance ->
        AlertDialog(
            onDismissRequest = { deleteHomeAssistantInstance = null },
            title = { Text(stringResource(R.string.ui_remove_436e1a0, instance.name)) },
            text = { Text(stringResource(R.string.ui_this_removes_its_login_mobile_app_registration_details_and_69afc19)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.removeHomeAssistantInstance(instance.id)
                        deleteHomeAssistantInstance = null
                    }
                ) { Text(stringResource(R.string.ui_remove_e963907)) }
            },
            dismissButton = { TextButton(onClick = { deleteHomeAssistantInstance = null }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
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
private fun SettingsChoice(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ModernSettingsMenuItem(icon = icon, title = title, subtitle = subtitle, enabled = enabled, onClick = onClick)
}

@Composable
private fun FamilyPermissionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * One person's room-following setup: which sensor tracks their phone, what should happen when it
 * moves, and any sensor states the area names could not resolve on their own.
 */
@Composable
private fun RoomFollowUserCard(
    userName: String,
    follow: Hki7RoomFollow,
    allEntities: List<HAEntity>,
    areas: List<HAArea>,
    roster: List<String>,
    onChange: (Hki7RoomFollow) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var showSensorPicker by remember { mutableStateOf(false) }
    var mappingState by remember { mutableStateOf<String?>(null) }

    val sensorLabel = follow.sensorEntityId?.let { id ->
        allEntities.firstOrNull { it.entity_id == id }?.friendlyName ?: id
    } ?: stringResource(R.string.settings_extra_room_follow_none)

    if (showSensorPicker) {
        RoomFollowSensorDialog(
            allEntities = allEntities,
            selected = follow.sensorEntityId,
            onDismiss = { showSensorPicker = false },
            onSelect = { picked ->
                showSensorPicker = false
                // Clearing the sensor must also stop following; the component enforces this too,
                // but doing it here keeps the switches from showing a state that cannot be saved.
                onChange(
                    if (picked == null) follow.copy(sensorEntityId = null, enabled = false)
                    else follow.copy(sensorEntityId = picked)
                )
            }
        )
    }

    mappingState?.let { state ->
        RoomFollowRoomDialog(
            state = state,
            areas = areas,
            selected = follow.stateRooms[state],
            onDismiss = { mappingState = null },
            onSelect = { areaId ->
                mappingState = null
                val updated = follow.stateRooms.toMutableMap()
                if (areaId == null) updated.remove(state) else updated[state] = areaId
                onChange(follow.copy(stateRooms = updated))
            }
        )
    }

    Surface(Modifier.fillMaxWidth(), shape = itemCornerShape(), color = appColors.subtleSurface) {
        Column(Modifier.padding(12.dp)) {
            Text(userName, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showSensorPicker = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_extra_room_follow_sensor),
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        sensorLabel,
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(20.dp))
            }

            if (follow.sensorEntityId != null) {
                FamilyPermissionRow(
                    title = stringResource(R.string.settings_extra_room_follow_enabled),
                    subtitle = stringResource(R.string.settings_extra_room_follow_description),
                    checked = follow.enabled
                ) { onChange(follow.copy(enabled = it)) }

                if (follow.enabled) {
                    FamilyPermissionRow(
                        title = stringResource(R.string.settings_extra_room_follow_open_on_launch),
                        subtitle = stringResource(R.string.settings_extra_room_follow_open_on_launch),
                        checked = follow.openOnLaunch
                    ) { onChange(follow.copy(openOnLaunch = it)) }

                    FamilyPermissionRow(
                        title = stringResource(R.string.settings_extra_room_follow_continue_after_launch),
                        subtitle = stringResource(R.string.settings_extra_room_follow_continue_after_launch_hint),
                        checked = follow.continueAfterLaunch
                    ) { onChange(follow.copy(continueAfterLaunch = it)) }
                }

                if (follow.enabled && follow.continueAfterLaunch) {
                    FamilyPermissionRow(
                        title = stringResource(R.string.settings_extra_room_follow_prompt_on_move),
                        subtitle = stringResource(R.string.settings_extra_room_follow_dwell_hint),
                        checked = follow.promptOnMove
                    ) { onChange(follow.copy(promptOnMove = it)) }

                    // Settable whether or not prompting is on, so the dwell time can be dialled in
                    // before switching prompts on rather than only after.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_extra_room_follow_dwell),
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.settings_extra_room_follow_dwell_value, follow.dwellSeconds),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = follow.dwellSeconds.coerceIn(
                            Hki7RoomFollow.MIN_DWELL_SECONDS, 120
                        ).toFloat(),
                        // Rounded, not truncated: a slider step lands on 19.999999 often enough that
                        // toInt() quietly turned 20 seconds into 19.
                        onValueChange = {
                            onChange(
                                follow.copy(
                                    dwellSeconds = kotlin.math.round(it).toInt()
                                        .coerceAtLeast(Hki7RoomFollow.MIN_DWELL_SECONDS)
                                )
                            )
                        },
                        // Starts at the minimum rather than 0: a zero-second dwell means the very
                        // first sensor reading counts as a move, so the room flips on every flap
                        // between adjacent rooms — which is exactly what a dwell window is for.
                        valueRange = Hki7RoomFollow.MIN_DWELL_SECONDS.toFloat()..120f,
                        steps = 22,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (follow.enabled) {
                    // Only the states the area names could not resolve need a decision, so matched
                    // ones are shown as already handled rather than asking for input twice — and this
                    // stays visible even with continued tracking off, since launch placement still
                    // needs it to resolve which room a sensor state means.
                    val states = remember(roster, allEntities) {
                        observedRoomStates(roster, allEntities.associateBy { it.entity_id })
                    }
                    Text(
                        stringResource(R.string.settings_extra_room_follow_rooms),
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    if (states.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_extra_room_follow_no_states),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    states.forEach { state ->
                        val resolved = resolveFollowedArea(state, areas, follow)
                        val areaName = areas.firstOrNull { it.area_id == resolved }?.name
                        val isOverride = follow.stateRooms.keys.any { it.equals(state, ignoreCase = true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clickable { mappingState = state },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                state,
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                areaName ?: stringResource(R.string.settings_extra_room_follow_rooms_unmatched),
                                color = if (areaName != null) appColors.onMuted else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (areaName != null && !isOverride) {
                                Text(
                                    stringResource(R.string.settings_extra_room_follow_rooms_matched),
                                    color = appColors.onMuted.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
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
                if (page == null) {
                    stringResource(R.string.settings_extra_create_custom_page)
                } else {
                    stringResource(R.string.settings_extra_edit_custom_page)
                },
                stringResource(R.string.settings_extra_custom_page_identity_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSubcategory(stringResource(R.string.ui_page_identity_dafa8dd), stringResource(R.string.ui_name_subtitle_and_navigation_icon_3f2e8c4))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ui_page_name_c99f51a)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(stringResource(R.string.ui_page_subtitle_8a04084)) },
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
                        Text(stringResource(R.string.ui_page_icon_b58a288), modifier = Modifier.weight(1f), color = appColors.onSurface)
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                }
                Text(
                    stringResource(R.string.ui_the_page_starts_empty_with_its_own_header_and_5a325c0),
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
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
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
                screen.localizedTitle(),
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (fixed) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = stringResource(R.string.ui_fixed_tab_10fe998),
                    tint = appColors.onMuted,
                    modifier = Modifier.size(20.dp).padding(end = 12.dp)
                )
            } else {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.ui_edit_page_17e45f8), tint = appColors.onSurface)
                    }
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.ui_move_up_b4f57cd),
                        tint = if (canMoveUp) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.ui_move_down_260ff8a),
                        tint = if (canMoveDown) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) {
                            stringResource(R.string.settings_extra_hide_tab)
                        } else {
                            stringResource(R.string.settings_extra_show_tab)
                        },
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
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.ui_open_link_d2de1a2), tint = appColors.onMuted)
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
const val HKI7_CHANGELOG_URL = "https://github.com/jimz011/android-hki7/blob/main/CHANGELOG.md"
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
private fun ParentalSearchAccessList(
    title: String,
    domains: List<String>,
    entityIds: List<String>,
    allEntities: List<HAEntity>,
    emptyText: String,
    onChange: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val names = remember(allEntities) { allEntities.associate { it.entity_id to (it.friendlyName ?: it.entity_id) } }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = itemCornerShape(),
        color = appColors.elevated,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onChange) { Text(stringResource(R.string.ui_change_64fbd99)) }
            }
            if (domains.isEmpty() && entityIds.isEmpty()) {
                Text(emptyText, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    domains.sorted().forEach { domain ->
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text(
                                stringResource(R.string.parental_search_domain_chip, domain.replace('_', ' ').replaceFirstChar(Char::uppercase)),
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                    entityIds.sortedBy { names[it] ?: it }.forEach { entityId ->
                        Surface(shape = RoundedCornerShape(50), color = appColors.subtleSurface) {
                            Text(
                                names[entityId] ?: entityId,
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
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

@Composable
private fun connectionText(status: ConnectionStatus, route: HomeAssistantConnectionRoute?): String = when (status) {
    ConnectionStatus.CONNECTED -> stringResource(
        R.string.settings_connection_via,
        route?.localizedName() ?: stringResource(R.string.settings_connection_unknown)
    )
    ConnectionStatus.ERROR -> stringResource(R.string.connection_error_title)
    else -> stringResource(R.string.settings_connection_connecting)
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

@Composable
private fun systemThemeOptions(): List<Pair<String, String>> = listOf(
    "auto" to stringResource(R.string.settings_extra_theme_auto),
    "rose" to stringResource(R.string.settings_extra_theme_rose),
    "green" to stringResource(R.string.settings_extra_theme_green),
    "blue" to stringResource(R.string.settings_extra_theme_blue),
    "amber" to stringResource(R.string.settings_extra_theme_amber),
    "custom" to stringResource(R.string.settings_extra_theme_custom)
)

@Composable
private fun localizedFontFamilyName(key: String): String = stringResource(
    when (key) {
        "sans" -> R.string.settings_extra_font_sans_serif
        "serif" -> R.string.settings_extra_font_serif
        "monospace" -> R.string.settings_extra_font_monospace
        "cursive" -> R.string.settings_extra_font_cursive
        "nunito" -> R.string.settings_extra_font_nunito
        "comfortaa" -> R.string.settings_extra_font_comfortaa
        "space_grotesk" -> R.string.settings_extra_font_space_grotesk
        "bree_serif" -> R.string.settings_extra_font_bree_serif
        "patrick_hand" -> R.string.settings_extra_font_patrick_hand
        "atkinson" -> R.string.settings_extra_font_atkinson
        else -> R.string.settings_extra_font_default
    }
)

@Composable
private fun localizedIconEffectGroup(group: String): String = stringResource(
    when (group) {
        "lights" -> R.string.settings_extra_effect_group_lights
        "fans" -> R.string.settings_extra_effect_group_fans
        "media" -> R.string.settings_extra_effect_group_media
        "climate" -> R.string.settings_extra_effect_group_climate
        "alerts" -> R.string.settings_extra_effect_group_alerts
        else -> R.string.settings_extra_effect_group_other
    }
)

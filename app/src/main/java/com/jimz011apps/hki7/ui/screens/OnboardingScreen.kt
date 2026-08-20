@file:Suppress("SetJavaScriptEnabled", "SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.jimz011apps.hki7.data.notificationsAllowed
import com.jimz011apps.hki7.data.HaDashboardSharing
import com.jimz011apps.hki7.data.HaBackupStorage
import com.jimz011apps.hki7.data.CloudBackupFile
import com.jimz011apps.hki7.data.CloudBackupStorage
import com.jimz011apps.hki7.data.Hki7BackupMeta
import com.jimz011apps.hki7.data.Hki7SharedDashboardMeta
import com.jimz011apps.hki7.data.HomeAssistantClient
import com.jimz011apps.hki7.data.HomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.LocationWork
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.PushForegroundService
import com.jimz011apps.hki7.data.classifyHomeAssistantConnectionRoute
import com.jimz011apps.hki7.data.canAccessLocalNetwork
import com.jimz011apps.hki7.data.ANDROID_17_API_LEVEL
import com.jimz011apps.hki7.data.LOCAL_NETWORK_PERMISSION
import com.jimz011apps.hki7.data.splitHomeAssistantConnectionUrl
import com.jimz011apps.hki7.data.driveAuthorizationRequest
import com.jimz011apps.hki7.ui.components.LocationDisclosureDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsHeader
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.net.URLEncoder
import com.google.android.gms.auth.api.identity.Identity

private enum class OnboardStep { WELCOME, SERVER, NAME, LOGIN, PERMISSIONS, CONNECTION, DASHBOARD }
private enum class AddInstanceStep { SERVER, LOGIN }

/**
 * First-run onboarding, modeled on the official Home Assistant app: welcome → auto-discover/enter the
 * server → OAuth login (which registers the device with the mobile_app integration right away) →
 * notification + location permission steps (location asks for the "Allow all the time" upgrade too).
 * Calls [onComplete] when finished so the host can show the main app.
 */
@Composable
fun OnboardingScreen(
    prefs: PreferencesManager,
    startAtLogin: Boolean = false,
    startAtDashboard: Boolean = false,
    familyAccessLost: Boolean = false,
    onComplete: () -> Unit,
) {
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
    LaunchedEffect(loginOnly, startAtDashboard) {
        if (!loginOnly && !startAtDashboard) prefs.prepareForInitialDashboardChoice()
    }
    // Saveable so that backgrounding mid-onboarding (e.g. on the permission step) and returning to a
    // recreated Activity resumes the same step instead of being lost.
    var step by rememberSaveable {
        mutableStateOf(if (startAtDashboard) OnboardStep.DASHBOARD else if (loginOnly) OnboardStep.LOGIN else OnboardStep.WELCOME)
    }
    var serverUrl by rememberSaveable {
        mutableStateOf(if (loginOnly) savedLoginUrl.orEmpty().removeSuffix("/") else "")
    }
    val scope = rememberCoroutineScope()
    var enteringDemo by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = stringResource(R.string.ui_onboard_step_fefaac1)
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
            OnboardStep.DASHBOARD -> DashboardSetupStep(prefs, familyAccessLost, onComplete = onComplete)
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
            label = stringResource(R.string.ui_add_instance_step_df337b3)
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
            title = stringResource(R.string.ui_local_only_05ed059),
            subtitle = stringResource(R.string.ui_connected_through_your_home_network_cdf4e0f),
            icon = Icons.Default.Wifi,
            paragraphs = listOf(
                stringResource(R.string.uif_onboarding_local_access_explanation),
                stringResource(R.string.uif_onboarding_add_remote_url_later),
            )
        )
        HomeAssistantConnectionRoute.NABU_CASA -> ConnectionInfoContent(
            title = stringResource(R.string.ui_remote_access_is_ready_0e26b5a),
            subtitle = stringResource(R.string.ui_connected_through_home_assistant_cloud_47a54c0),
            icon = Icons.Default.Cloud,
            paragraphs = listOf(
                stringResource(R.string.uif_onboarding_nabu_casa_access_explanation),
                stringResource(R.string.uif_onboarding_add_internal_url_later),
            )
        )
        HomeAssistantConnectionRoute.EXTERNAL -> ConnectionInfoContent(
            title = stringResource(R.string.ui_remote_access_is_ready_0e26b5a),
            subtitle = stringResource(R.string.ui_connected_through_your_external_home_assistant_address_966b0e6),
            icon = Icons.Default.Public,
            paragraphs = listOf(
                stringResource(R.string.uif_onboarding_external_access_explanation),
                stringResource(R.string.uif_onboarding_add_internal_url_later),
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
            ) { Text(stringResource(R.string.ui_got_it_5b8027f), fontWeight = FontWeight.Bold) }
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
internal fun DashboardSetupStep(
    prefs: PreferencesManager,
    familyAccessLost: Boolean = false,
    /**
     * Non-null when this is adding a dashboard from Settings rather than setting up the first one.
     * The choices are identical, but each creates a dashboard beside the current one instead of
     * configuring it, nothing claims the default, and the quick-start guide is not queued again.
     */
    additionalDashboardName: String? = null,
    onBack: (() -> Unit)? = null,
    onComplete: () -> Unit,
) {
    val colors = LocalHKIAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingMode by remember { mutableStateOf<Boolean?>(null) }
    // Family-shared dashboards: null while probing the cloud component, then the list shared with me.
    var cloudAvailable by remember { mutableStateOf<Boolean?>(null) }
    var sharedList by remember { mutableStateOf<List<Hki7SharedDashboardMeta>>(emptyList()) }
    var usingSharedId by remember { mutableStateOf<String?>(null) }
    var familyError by remember { mutableStateOf<String?>(null) }
    // Which shared dashboards this device already holds, so the list can say so rather than
    // silently re-importing over the copy already there.
    val localDashboards by prefs.dashboards.collectAsState(initial = emptyList())
    var showRestoreSource by remember { mutableStateOf(false) }
    var showCloudRestore by remember { mutableStateOf(false) }
    var showHaRestore by remember { mutableStateOf(false) }
    var cloudRestoreFiles by remember { mutableStateOf<List<CloudBackupFile>>(emptyList()) }
    var haRestoreFiles by remember { mutableStateOf<List<Hki7BackupMeta>>(emptyList()) }
    var restoreBusy by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    val allowDashboardCreate by prefs.enforcedAllowDashboardCreate.collectAsState(initial = true)
    val allowDashboardSwitch by prefs.enforcedAllowDashboardSwitch.collectAsState(initial = true)
    var showAccessLostMessage by remember(familyAccessLost) { mutableStateOf(familyAccessLost) }
    val importFailedTemplate = stringResource(R.string.uif_onboarding_shared_import_failed)
    val unknownError = stringResource(R.string.settings_extra_unknown_error)

    fun restoreRaw(raw: String) {
        if (restoreBusy || savingMode != null) return
        restoreBusy = true
        restoreError = null
        scope.launch {
            runCatching {
                if (additionalDashboardName != null) {
                    // Land the backup in a dashboard of its own; the one in use is written back to
                    // the store first and left alone.
                    checkNotNull(prefs.createDashboard(additionalDashboardName, autoGenerate = false)) {
                        "Dashboard creation is disabled by family permissions"
                    }
                    prefs.restoreUiBackup(raw)
                    prefs.commitRestoredBackupIntoActiveDashboard(additionalDashboardName)
                } else {
                    prefs.restoreUiBackup(raw)
                    prefs.useRestoredBackupAsInitial()
                    prefs.clearFamilyDashboardSubscription()
                }
            }.onSuccess {
                onComplete()
            }.onFailure { error ->
                restoreError = context.getString(
                    R.string.settings_extra_restore_failed,
                    error.message ?: unknownError,
                )
                restoreBusy = false
            }
        }
    }

    val localRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                        ?: error(context.getString(R.string.settings_extra_selected_file_open_failed))
                }.onSuccess(::restoreRaw).onFailure { error ->
                    restoreError = context.getString(
                        R.string.settings_extra_restore_failed,
                        error.message ?: unknownError,
                    )
                }
            }
        }
    }

    val loadDriveBackups: () -> Unit = {
        restoreBusy = true
        restoreError = null
        scope.launch {
            runCatching { CloudBackupStorage.backups(context) }
                .onSuccess { backups ->
                    cloudRestoreFiles = backups
                    restoreBusy = false
                    if (backups.isEmpty()) {
                        restoreError = context.getString(R.string.settings_extra_no_drive_backups)
                    } else {
                        showCloudRestore = true
                    }
                }
                .onFailure { error ->
                    restoreBusy = false
                    restoreError = context.getString(
                        R.string.settings_extra_drive_backups_load_failed,
                        error.message ?: unknownError,
                    )
                }
        }
    }
    val driveAuthorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorization ->
                if (authorization.accessToken != null) loadDriveBackups()
                else {
                    restoreBusy = false
                    restoreError = context.getString(R.string.settings_extra_drive_access_token_missing)
                }
            }.onFailure { error ->
                restoreBusy = false
                restoreError = context.getString(
                    R.string.settings_extra_drive_authorization_failed,
                    error.message ?: unknownError,
                )
            }
        } else {
            restoreBusy = false
            restoreError = context.getString(R.string.settings_extra_drive_authorization_cancelled)
        }
    }
    val requestDriveRestore = {
        restoreBusy = true
        restoreError = null
        Identity.getAuthorizationClient(context).authorize(driveAuthorizationRequest())
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    authorization.pendingIntent?.let { pendingIntent ->
                        driveAuthorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } ?: run {
                        restoreBusy = false
                        restoreError = context.getString(R.string.settings_extra_drive_authorization_open_failed)
                    }
                } else if (authorization.accessToken != null) {
                    loadDriveBackups()
                } else {
                    restoreBusy = false
                    restoreError = context.getString(R.string.settings_extra_drive_access_token_missing)
                }
            }
            .addOnFailureListener { error ->
                restoreBusy = false
                restoreError = context.getString(
                    R.string.settings_extra_drive_authorization_failed,
                    error.message ?: unknownError,
                )
            }
    }
    LaunchedEffect(Unit) {
        do {
            runCatching { com.jimz011apps.hki7.data.HaParentalControls.refreshForCurrentUser(context, prefs) }
            val id = runCatching { HaDashboardSharing.whoami(context) }.getOrNull()
            cloudAvailable = id != null
            sharedList = if (id != null)
                runCatching { HaDashboardSharing.listSharedForMe(context) }.getOrDefault(emptyList())
            else emptyList()
            // While access is blocked, keep the chooser live so a newly shared dashboard or relaxed
            // permission appears without requiring the user to restart the app.
            if (familyAccessLost) delay(5_000)
        } while (familyAccessLost)
    }
    fun finish(auto: Boolean) {
        if (savingMode != null || !allowDashboardCreate) return
        savingMode = auto
        scope.launch {
            if (additionalDashboardName != null) {
                // createDashboard writes the open dashboard back to the store before switching, so
                // the one being used is preserved rather than replaced.
                if (prefs.createDashboard(additionalDashboardName, auto) == null) {
                    savingMode = null
                    return@launch
                }
                onComplete()
            } else if (familyAccessLost) {
                val name = if (auto) "Default (auto generated)" else "Default"
                val created = prefs.createDashboard(name, auto)
                if (created == null) {
                    savingMode = null
                    return@launch
                }
                prefs.clearFamilyDashboardSubscription()
                onComplete()
            } else {
                prefs.clearFamilyDashboardSubscription()
                prefs.configureInitialDashboard(auto)
                onComplete()
            }
        }
    }
    fun useShared(meta: Hki7SharedDashboardMeta) {
        if (savingMode != null || usingSharedId != null) return
        usingSharedId = meta.id
        familyError = null
        scope.launch {
            val localId = runCatching { HaDashboardSharing.import(context, prefs, meta) }.getOrNull()
            if (localId != null) {
                if (additionalDashboardName != null) {
                    // Adding, not adopting: import already stored it beside the others, so this
                    // only opens it. Claiming the default or discarding the rest is first-run
                    // behaviour and would contradict "nothing is overwritten".
                    prefs.switchDashboard(localId)
                } else {
                    val discardOtherDashboards = !prefs.enforcedAllowDashboardSwitch.first()
                    prefs.useSharedDashboardAsInitial(localId, discardOtherDashboards)
                }
                onComplete()
            } else {
                familyError = importFailedTemplate.format(meta.name)
                usingSharedId = null
            }
        }
    }
    OnboardingDialogFrame(
        title = if (additionalDashboardName != null) stringResource(R.string.ui_new_dashboard_4d3c071)
            else stringResource(R.string.ui_choose_your_dashboard_1ec85e9),
        subtitle = stringResource(R.string.ui_pick_a_starting_point_everything_remains_editable_340924e),
        icon = Icons.Default.DashboardCustomize,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardChoiceCard(
                title = stringResource(R.string.ui_auto_generate_1502534),
                subtitle = stringResource(R.string.ui_let_hki_7_create_the_first_version_for_you_dc36439),
                icon = Icons.Default.AutoAwesome,
                recommended = true,
                bullets = listOf(
                    stringResource(R.string.uif_onboarding_auto_rooms_floors),
                    stringResource(R.string.uif_onboarding_auto_entities),
                    stringResource(R.string.uif_onboarding_everything_editable),
                ),
                buttonText = if (savingMode == true) {
                    stringResource(R.string.uif_onboarding_building_dashboard)
                } else {
                    stringResource(R.string.uif_onboarding_auto_generate)
                },
                enabled = savingMode == null && allowDashboardCreate,
                disabledReason = if (!allowDashboardCreate) {
                    stringResource(R.string.family_dashboard_create_disabled_reason)
                } else null,
                onClick = { finish(true) }
            )
            DashboardChoiceCard(
                title = stringResource(R.string.ui_start_empty_25336ee),
                subtitle = stringResource(R.string.ui_build_the_interface_entirely_your_way_e94582f),
                icon = Icons.Default.DashboardCustomize,
                recommended = false,
                bullets = listOf(
                    stringResource(R.string.uif_onboarding_starts_empty),
                    stringResource(R.string.uif_onboarding_add_in_edit_mode),
                ),
                buttonText = if (savingMode == false) {
                    stringResource(R.string.uif_onboarding_preparing_dashboard)
                } else {
                    stringResource(R.string.uif_onboarding_start_empty)
                },
                enabled = savingMode == null && allowDashboardCreate,
                disabledReason = if (!allowDashboardCreate) {
                    stringResource(R.string.family_dashboard_create_disabled_reason)
                } else null,
                onClick = { finish(false) }
            )
            DashboardChoiceCard(
                title = stringResource(R.string.ui_restore_backup_a65eaa8),
                subtitle = stringResource(R.string.ui_choose_where_to_restore_the_dashboard_configuration_from_bb40b34),
                icon = Icons.Default.Restore,
                recommended = false,
                bullets = buildList {
                    add(stringResource(R.string.ui_local_file_576d5ac))
                    add(stringResource(R.string.ui_google_drive_07c2964))
                    if (cloudAvailable == true) add(stringResource(R.string.ui_home_assistant_c8fd3bb))
                },
                buttonText = if (restoreBusy) {
                    stringResource(R.string.connection_restoring_dashboard)
                } else {
                    stringResource(R.string.ui_restore_backup_a65eaa8)
                },
                enabled = savingMode == null && !restoreBusy && allowDashboardSwitch,
                disabledReason = if (!allowDashboardSwitch) {
                    stringResource(R.string.family_dashboard_restore_disabled_reason)
                } else null,
                onClick = { showRestoreSource = true }
            )
            restoreError?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // Import from family: shown whenever the option is relevant, with guidance when the
            // cloud component isn't set up yet. Available when adding a dashboard as well, since a
            // family can publish more than one — the adoption path above stays non-destructive.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.subtleSurface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = colors.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_import_from_family_f9eb88d), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            Text(stringResource(R.string.ui_use_a_dashboard_an_admin_has_shared_with_you_8b1cfdb), style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    when {
                        cloudAvailable == null -> {
                            Text(stringResource(R.string.ui_checking_for_shared_dashboards_5f2f823), style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
                        }
                        cloudAvailable == false -> {
                            Text(
                                stringResource(R.string.ui_an_admin_needs_to_install_the_hki_7_cloud_0e9241b),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onMuted
                            )
                        }
                        sharedList.isEmpty() -> {
                            Text(
                                stringResource(R.string.ui_no_dashboards_have_been_shared_with_you_yet_ask_38b2c0d),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onMuted
                            )
                        }
                        else -> {
                            sharedList.forEach { meta ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surface
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(meta.name, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                                            val updated = meta.updated.take(19).replace('T', ' ')
                                            Text(
                                                if (updated.isNotBlank()) stringResource(R.string.ui_updated_62d2331, updated) else stringResource(R.string.ui_shared_dashboard_86876c0),
                                                color = colors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        val alreadyAdded = localDashboards.any { it.id == "shared-${meta.id}" }
                                        Button(
                                            enabled = savingMode == null && usingSharedId == null && !alreadyAdded,
                                            onClick = { useShared(meta) },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            if (!alreadyAdded) {
                                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            Text(
                                                when {
                                                    alreadyAdded -> stringResource(R.string.uif_onboarding_shared_already_added)
                                                    usingSharedId == meta.id -> stringResource(R.string.ui_importing_820599d)
                                                    else -> stringResource(R.string.ui_use_1d4d43c)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    familyError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.subtleSurface
            ) {
                Text(
                    stringResource(R.string.ui_auto_generation_is_a_one_time_starting_point_the_6b317fe),
                    modifier = Modifier.padding(14.dp),
                    color = colors.onMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    OnboardingRestoreDialogs(
        showSource = showRestoreSource,
        showCloud = showCloudRestore,
        showHomeAssistant = showHaRestore,
        cloudComponentAvailable = cloudAvailable == true,
        busy = restoreBusy,
        cloudFiles = cloudRestoreFiles,
        homeAssistantFiles = haRestoreFiles,
        onDismissSource = { showRestoreSource = false },
        onDismissCloud = { showCloudRestore = false },
        onDismissHomeAssistant = { showHaRestore = false },
        onLocal = {
            showRestoreSource = false
            localRestoreLauncher.launch(arrayOf("application/json", "text/plain"))
        },
        onGoogleDrive = {
            showRestoreSource = false
            requestDriveRestore()
        },
        onHomeAssistant = {
            showRestoreSource = false
            restoreBusy = true
            restoreError = null
            scope.launch {
                runCatching { HaBackupStorage.list(context) }
                    .onSuccess { backups ->
                        haRestoreFiles = backups
                        restoreBusy = false
                        if (backups.isEmpty()) {
                            restoreError = context.getString(R.string.settings_extra_no_ha_backups)
                        } else {
                            showHaRestore = true
                        }
                    }
                    .onFailure { error ->
                        restoreBusy = false
                        restoreError = context.getString(
                            R.string.settings_extra_ha_backups_load_failed,
                            error.message ?: unknownError,
                        )
                    }
            }
        },
        onCloudFile = { file ->
            showCloudRestore = false
            restoreBusy = true
            scope.launch {
                runCatching { CloudBackupStorage.read(context, file.id) }
                    .onSuccess { raw ->
                        restoreBusy = false
                        restoreRaw(raw)
                    }
                    .onFailure { error ->
                        restoreBusy = false
                        restoreError = context.getString(
                            R.string.settings_extra_restore_failed,
                            error.message ?: unknownError,
                        )
                    }
            }
        },
        onHomeAssistantFile = { meta ->
            showHaRestore = false
            restoreBusy = true
            scope.launch {
                runCatching {
                    HaBackupStorage.read(context, meta.id)
                        ?: error(context.getString(R.string.settings_extra_backup_unreadable))
                }.onSuccess { raw ->
                    restoreBusy = false
                    restoreRaw(raw)
                }.onFailure { error ->
                    restoreBusy = false
                    restoreError = context.getString(
                        R.string.settings_extra_restore_failed,
                        error.message ?: unknownError,
                    )
                }
            }
        },
    )

    if (showAccessLostMessage) {
        AlertDialog(
            onDismissRequest = { showAccessLostMessage = false },
            title = { Text(stringResource(R.string.family_dashboard_access_lost_title)) },
            text = { Text(stringResource(R.string.family_dashboard_access_lost_message)) },
            confirmButton = {
                Button(onClick = { showAccessLostMessage = false }) {
                    Text(stringResource(R.string.ui_continue_2e02623))
                }
            },
        )
    }
}

@Composable
private fun OnboardingRestoreDialogs(
    showSource: Boolean,
    showCloud: Boolean,
    showHomeAssistant: Boolean,
    cloudComponentAvailable: Boolean,
    busy: Boolean,
    cloudFiles: List<CloudBackupFile>,
    homeAssistantFiles: List<Hki7BackupMeta>,
    onDismissSource: () -> Unit,
    onDismissCloud: () -> Unit,
    onDismissHomeAssistant: () -> Unit,
    onLocal: () -> Unit,
    onGoogleDrive: () -> Unit,
    onHomeAssistant: () -> Unit,
    onCloudFile: (CloudBackupFile) -> Unit,
    onHomeAssistantFile: (Hki7BackupMeta) -> Unit,
) {
    val colors = LocalHKIAppColors.current
    if (showSource) {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismissSource() },
            title = { Text(stringResource(R.string.ui_restore_backup_a65eaa8)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.ui_choose_where_to_restore_the_dashboard_configuration_from_bb40b34))
                    TextButton(enabled = !busy, onClick = onLocal, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_local_file_576d5ac))
                    }
                    TextButton(enabled = !busy, onClick = onGoogleDrive, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_google_drive_07c2964))
                    }
                    if (cloudComponentAvailable) {
                        TextButton(enabled = !busy, onClick = onHomeAssistant, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.ui_home_assistant_c8fd3bb))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = onDismissSource) {
                    Text(stringResource(R.string.ui_close_bbfa773))
                }
            },
        )
    }
    if (showCloud) {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismissCloud() },
            title = { Text(stringResource(R.string.ui_restore_from_cloud_52b6663)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cloudFiles.forEach { file ->
                        TextButton(
                            enabled = !busy,
                            onClick = { onCloudFile(file) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(file.name)
                                Text(
                                    file.modifiedTime.take(19).replace('T', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onMuted,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = onDismissCloud) {
                    Text(stringResource(R.string.ui_close_bbfa773))
                }
            },
        )
    }
    if (showHomeAssistant) {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismissHomeAssistant() },
            title = { Text(stringResource(R.string.ui_restore_from_home_assistant_13aec1d)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    homeAssistantFiles.forEach { meta ->
                        TextButton(
                            enabled = !busy,
                            onClick = { onHomeAssistantFile(meta) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(meta.label.ifBlank { meta.created.take(19).replace('T', ' ') })
                                if (meta.label.isNotBlank()) {
                                    Text(
                                        meta.created.take(19).replace('T', ' '),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = onDismissHomeAssistant) {
                    Text(stringResource(R.string.ui_close_bbfa773))
                }
            },
        )
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
    disabledReason: String? = null,
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
                                    stringResource(R.string.ui_recommended_e37d21e),
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
            if (!enabled && disabledReason != null) {
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = colors.onMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(disabledReason, style = MaterialTheme.typography.bodySmall, color = colors.onMuted, modifier = Modifier.weight(1f))
                }
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
            Text(stringResource(R.string.ui_welcome_to_hki_7_9e317bb), style = MaterialTheme.typography.displaySmall, color = appColors.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ui_a_fast_auto_generating_dashboard_for_home_assistant_let_0fad4a9),
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
                Text(stringResource(R.string.ui_get_started_bd2cb05))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            TextButton(onClick = onDemo, modifier = Modifier.padding(top = 6.dp)) {
                Text(stringResource(R.string.ui_try_the_demo_home_no_server_needed_bc58247))
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
    val context = LocalContext.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val localNetworkGranted = remember(permissionRefresh) { canAccessLocalNetwork(context) }
    val localNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionRefresh++ }
    LaunchedEffect(Unit) {
        if (!localNetworkGranted) {
            localNetworkLauncher.launch(LOCAL_NETWORK_PERMISSION)
        }
    }
    val discovered = rememberHaDiscovery(active = localNetworkGranted)
    var manualUrl by remember { mutableStateOf("") }

    fun openAppSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            )
        }
    }

    OnboardingDialogFrame(
        title = stringResource(R.string.ui_find_home_assistant_locally_c992bdf),
        subtitle = stringResource(R.string.ui_we_ll_check_your_home_network_first_you_can_c7ca0f0),
        icon = Icons.Default.Home,
        onBack = onBack,
        footer = {
            Button(
                onClick = { if (manualUrl.isNotBlank()) onServerChosen(manualUrl.trim()) },
                enabled = manualUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.ui_connect_b65463c))
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
                Text(stringResource(R.string.ui_discovered_acbb9e0), style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
                Spacer(Modifier.width(8.dp))
                if (localNetworkGranted && discovered.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_scanning_36c4a06), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                }
            }

            if (!localNetworkGranted) {
                PermissionCard(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.uif_local_network_access),
                    description = stringResource(R.string.uif_local_network_access_description),
                    granted = false,
                    actionLabel = stringResource(R.string.uif_enable),
                    onAction = { localNetworkLauncher.launch(LOCAL_NETWORK_PERMISSION) },
                    secondaryLabel = stringResource(R.string.uif_open_settings),
                    onSecondary = ::openAppSettings,
                )
            } else if (discovered.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.ui_no_servers_found_yet_make_sure_you_re_on_7e6832d),
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
            Text(stringResource(R.string.ui_enter_an_address_manually_5eda99d), style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                label = { Text(stringResource(R.string.ui_server_url_1d5d1ef)) },
                placeholder = { Text(stringResource(R.string.ui_http_homeassistant_local_8123_67b2235)) },
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
        title = stringResource(R.string.ui_name_this_device_4d603e2),
        subtitle = stringResource(R.string.ui_choose_how_this_phone_or_tablet_appears_in_home_419da67),
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
                Text(if (saving) stringResource(R.string.ui_saving_56a2285) else stringResource(R.string.ui_continue_2e02623))
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
                        stringResource(R.string.ui_use_a_recognizable_name_such_as_kitchen_tablet_or_ca9f431),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.onMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.ui_device_name_79d7a15)) },
                supportingText = { Text(stringResource(R.string.ui_this_name_is_used_when_the_device_registers_with_8aba2f2)) },
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
    val loginFailedTemplate = stringResource(R.string.uif_onboarding_login_failed)
    val noAuthorizationCode = stringResource(R.string.uif_onboarding_no_authorization_code)
    val connectionFailed = stringResource(R.string.uif_onboarding_connection_failed)
    val serverHttpErrorTemplate = stringResource(R.string.uif_onboarding_server_http_error)
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
            errorMessage = loginFailedTemplate.format(callbackError)
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
                    errorMessage = loginFailedTemplate.format(e.message.orEmpty())
                    authInProgress = false
                }
            }
        } else if (code == null) {
            errorMessage = noAuthorizationCode
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
                                loadError = error?.description?.toString() ?: connectionFailed
                                pageLoading = false
                            }
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                loadError = serverHttpErrorTemplate.format(errorResponse?.statusCode ?: 0)
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
                Text(stringResource(R.string.ui_contacting_your_home_assistant_server_e0a53d1), color = appColors.onMuted)
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
                    stringResource(R.string.ui_can_t_reach_the_server_73079bd),
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
                ) { Text(stringResource(R.string.ui_try_again_042c862)) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.ui_choose_a_different_server_47e6ce6)) }
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
                Text(stringResource(R.string.ui_signing_you_in_10d0636), color = appColors.onMuted)
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
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ui_back_b52b36b), tint = Color.White)
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
    val notifGranted = remember(refresh) { notificationsAllowed(context) }
    val localNetworkGranted = remember(refresh) { canAccessLocalNetwork(context) }
    val fineGranted = remember(refresh) { hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) || hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION) }
    val backgroundGranted = remember(refresh) { hasPerm(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
    val batteryUnrestricted = remember(refresh) { powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val localNetworkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
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

    /** Below API 33 there is no notification permission to request — switching them back on is
     *  something only the user can do from system settings. */
    fun openNotificationSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }.onFailure { openAppSettings() }
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
    val showsLocalNetworkPermission = Build.VERSION.SDK_INT >= ANDROID_17_API_LEVEL
    val permissionStates = buildList {
        if (showsLocalNetworkPermission) add(localNetworkGranted)
        add(notifGranted)
        add(locationGranted)
        add(batteryUnrestricted)
    }
    val enabledCount = permissionStates.count { it }
    val permissionCount = permissionStates.size
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
                    title = stringResource(R.string.ui_permissions_d06d555),
                    subtitle = stringResource(R.string.ui_enable_the_features_hki_7_may_use_in_the_52e596d),
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
                            Text(stringResource(R.string.ui_setup_progress_1a8adef), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.uif_of_enabled, enabledCount, permissionCount), style = MaterialTheme.typography.labelMedium, color = appColors.onMuted)
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
                                        .fillMaxWidth(enabledCount.toFloat() / permissionCount)
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
                    if (showsLocalNetworkPermission) {
                        PermissionCard(
                            icon = Icons.Default.Wifi,
                            title = stringResource(R.string.uif_local_network_access),
                            description = stringResource(R.string.uif_local_network_access_description),
                            granted = localNetworkGranted,
                            actionLabel = stringResource(R.string.uif_enable),
                            onAction = { localNetworkLauncher.launch(LOCAL_NETWORK_PERMISSION) },
                            secondaryLabel = stringResource(R.string.uif_open_settings),
                            onSecondary = { openAppSettings() },
                        )
                    }

                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.ui_notifications_753a22b),
                        description = stringResource(R.string.ui_receive_home_assistant_alerts_and_actionable_notifications_386435f),
                        granted = notifGranted,
                        actionLabel = stringResource(R.string.uif_enable),
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openNotificationSettings()
                            }
                        }
                    )

                    PermissionCard(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.ui_background_location_5f56f21),
                        description = stringResource(R.string.ui_keeps_presence_detection_and_zone_automations_working_andr_c48598a),
                        granted = locationGranted,
                        actionLabel = when {
                            !fineGranted -> stringResource(R.string.uif_enable_location)
                            !backgroundGranted -> stringResource(R.string.uif_allow_all_the_time)
                            else -> stringResource(R.string.uif_enabled)
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
                        secondaryLabel = if (fineGranted && !backgroundGranted) {
                            stringResource(R.string.uif_open_settings)
                        } else null,
                        onSecondary = { openAppSettings() }
                    )

                    PermissionCard(
                        icon = Icons.Default.BatterySaver,
                        title = stringResource(R.string.ui_unrestricted_background_cebdb1e),
                        description = stringResource(R.string.ui_prevents_android_from_delaying_battery_charging_and_presen_623b898),
                        granted = batteryUnrestricted,
                        actionLabel = stringResource(R.string.uif_allow),
                        onAction = { requestBatteryUnrestricted() }
                    )

                    Text(
                        stringResource(R.string.uif_permissions_can_be_changed_later),
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
                    Text(if (localNetworkGranted && notifGranted && locationGranted) stringResource(R.string.ui_done_e9b450d) else stringResource(R.string.ui_continue_2e02623))
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
                        if (granted) stringResource(R.string.ui_ready_20c7c55) else stringResource(R.string.ui_permission_needed_7c7a8e5),
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
                            stringResource(R.string.ui_enabled_e22fb09),
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

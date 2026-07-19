@file:Suppress("UnusedBoxWithConstraintsScope", "DEPRECATION", "KotlinConstantConditions", "SpellCheckingInspection")

package com.example.hki7.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIActionButton
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.screens.GenericEntityDialog
import com.example.hki7.ui.screens.PagedRoleDialog
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.utils.handleActionOutcome
import androidx.compose.runtime.compositionLocalOf

/** Custom nav-bar buttons for the currently-open dialog. Provided at the dialog's open site so every
 *  HKIDialog wrapper (light, lock, person, …) inherits them without threading an extra parameter. */
val LocalDialogCustomButtons = compositionLocalOf { emptyList<HKIActionButton>() }

/** NavController for dialog custom-button "navigate" actions; null on surfaces without navigation. */
val LocalDialogNavController = compositionLocalOf<NavController?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HKIDialog(
    entity: HAEntity,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    icon: ImageVector = Icons.Default.Circle,
    iconTint: Color = Color(0xFFFFA500),
    titleOverride: String? = null,
    iconName: String? = null,
    spinIcon: Boolean = false,
    headerImageUrl: String? = null,
    headerIconContent: (@Composable () -> Unit)? = null,
    statusText: String? = null,
    allowInEditMode: Boolean = false,
    showHistoryButton: Boolean = true,
    extraGraphEntityIds: List<String> = emptyList(),
    groupContent: (@Composable () -> Unit)? = null,
    tabs: List<Triple<String, ImageVector, () -> Unit>> = emptyList(),
    currentTab: String? = null,
    // User-added quick-access buttons rendered on wrapped rows below the default nav bar. Defaults
    // to the value provided at the open site via LocalDialogCustomButtons.
    customButtons: List<HKIActionButton> = LocalDialogCustomButtons.current,
    navController: NavController? = LocalDialogNavController.current,
    content: @Composable ColumnScope.(Boolean) -> Unit
) {
    val editMode by viewModel.isEditMode.collectAsState()
    LaunchedEffect(editMode, allowInEditMode) { if (editMode && !allowInEditMode) onDismiss() }
    if (editMode && !allowInEditMode) return
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    // "Use entity picture" as the header icon resolves to the circular header-image path below.
    val resolvedHeaderImage = headerImageUrl
        ?: if (iconName == ENTITY_PICTURE_ICON) resolveEntityPictureUrl(entity, currentUrl) else null
    // Live entities for the custom buttons (icon/state); nested more-info host for their actions.
    val customButtonIds = remember(customButtons) { customButtons.map { it.entityId } }
    val customEntitiesFlow = remember(viewModel, customButtonIds) { viewModel.entitiesFor(customButtonIds) }
    val customEntities by customEntitiesFlow.collectAsState()
    var moreInfoEntityId by remember { mutableStateOf<String?>(null) }
    val dialogNavigationBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 230 / 255f).toArgb()
    var showHistory by remember { mutableStateOf(false) }
    var showGroup by remember { mutableStateOf(false) }
    var showDevice by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }

    // Device-first dialogs: resolve the entity's HA device so we can offer the device view
    // (battery level, extra sensors, sibling controls). Registries are cached in the view model.
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }
    val deviceId = rememberEntityDeviceId(entity, viewModel)

    LaunchedEffect(Unit) {
        dialogVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
                )
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.9f)
                window.setWindowAnimations(com.example.hki7.R.style.HKI7DialogBottomAnimation)
                window.statusBarColor = android.graphics.Color.argb(230, 0, 0, 0)
                window.navigationBarColor = dialogNavigationBarColor
                window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(32)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 32
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .combinedClickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val isPhone = maxWidth < 600.dp
            AnimatedVisibility(
                visible = dialogVisible,
                enter = slideInVertically(animationSpec = tween(260), initialOffsetY = { it }) + fadeIn(tween(180))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(if (isPhone) 0.78f else 0.85f)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .combinedClickable(onClick = {}),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.elevated,
                        contentColor = appColors.onSurface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                        appColors.elevated,
                                        appColors.elevated
                                    )
                                )
                            )
                    ) {
                        val dialogBottomControlsLift = 7.dp
                        // Bottom controls: the default nav bar (tabs) on the first row, then any custom
                        // buttons wrapped onto additional rows (≤5 per row → up to 2 more rows).
                        val customRows = remember(customButtons) { customButtons.chunked(5) }
                        val bottomRowCount = (if (tabs.isNotEmpty()) 1 else 0) + customRows.size
                        val showBottomControls = bottomRowCount > 0 && !showHistory && !showGroup && !showDevice
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (resolvedHeaderImage != null) {
                                    Surface(
                                        modifier = Modifier.size(50.dp),
                                        shape = RoundedCornerShape(17.dp),
                                        color = appColors.subtleSurface
                                    ) {
                                        AsyncImage(
                                            model = resolvedHeaderImage,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else if (headerIconContent != null) {
                                    Surface(
                                        modifier = Modifier.size(50.dp),
                                        shape = RoundedCornerShape(17.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { headerIconContent() }
                                    }
                                } else {
                                    val spinRotation = rememberIconSpinRotation(spinIcon && entity.state.lowercase() != "off")
                                    val iconModifier = Modifier
                                        .size(24.dp)
                                        .rotate(spinRotation)
                                    // Drop the picture sentinel here (no picture available) so it falls back to the icon.
                                    val effectiveIconName = iconName?.takeUnless { it.isBlank() || it == ENTITY_PICTURE_ICON }
                                    Surface(
                                        modifier = Modifier.size(50.dp),
                                        shape = RoundedCornerShape(17.dp),
                                        color = iconTint.copy(alpha = 0.16f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (effectiveIconName != null) {
                                                MdiIcon(effectiveIconName, contentDescription = null, tint = iconTint, size = 24.dp, modifier = iconModifier)
                                            } else {
                                                Icon(icon, contentDescription = null, tint = iconTint, modifier = iconModifier)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        titleOverride?.takeUnless { it.isBlank() } ?: entity.friendlyName ?: entity.entity_id,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = appColors.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        statusText ?: entity.state.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appColors.onMuted
                                    )
                                    formatLastSeen(entity.last_changed)?.let { lastSeen ->
                                        Text(
                                            lastSeen,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = appColors.onMuted.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (deviceId != null) {
                                    IconButton(
                                        onClick = { showDevice = !showDevice; if (showDevice) { showHistory = false; showGroup = false } },
                                        modifier = Modifier
                                            .background(
                                                if (showDevice) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                else appColors.subtleSurface,
                                                CircleShape
                                            )
                                            .size(48.dp)
                                    ) {
                                        MdiIcon(
                                            "devices",
                                            contentDescription = "Device details",
                                            tint = appColors.onSurface,
                                            size = 24.dp
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                if (groupContent != null) {
                                    IconButton(
                                        onClick = { showGroup = !showGroup; if (showGroup) { showHistory = false; showDevice = false } },
                                        modifier = Modifier
                                            .background(
                                                if (showGroup) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                else appColors.subtleSurface,
                                                CircleShape
                                            )
                                            .size(48.dp)
                                    ) {
                                        MdiIcon(
                                            "view-list",
                                            contentDescription = "Show group",
                                            tint = appColors.onSurface,
                                            size = 24.dp
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                if (showHistoryButton) {
                                    IconButton(
                                        onClick = { showHistory = !showHistory; if (showHistory) { showGroup = false; showDevice = false } },
                                        modifier = Modifier
                                            .background(
                                                if (showHistory) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                else appColors.subtleSurface,
                                                CircleShape
                                            )
                                            .size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "History",
                                            tint = appColors.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .background(appColors.subtleSurface, CircleShape)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = appColors.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(bottom = if (showBottomControls) (bottomRowCount * 78).dp + dialogBottomControlsLift else 0.dp)
                            ) {
                                if (showHistory) {
                                    HistoryView(entity = entity, viewModel = viewModel, extraGraphEntityIds = extraGraphEntityIds)
                                } else if (showDevice && deviceId != null) {
                                    DeviceEntitiesView(deviceId = deviceId, sourceEntity = entity, viewModel = viewModel)
                                } else if (showGroup && groupContent != null) {
                                    groupContent()
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        content(showHistory)
                                    }
                                }
                            }
                        }

                        if (showBottomControls) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = dialogBottomControlsLift),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (tabs.isNotEmpty()) {
                                    val denseTabs = tabs.size > 5
                                    HKIBottomBar(
                                        horizontalPadding = if (denseTabs) 16.dp else if (isPhone) 64.dp else 16.dp,
                                        scrollable = false
                                    ) {
                                        tabs.forEach { (label, tabIcon, action) ->
                                            val isSelected = currentTab == label
                                            Column(
                                                modifier = Modifier.weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(itemCornerShape())
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                                    .combinedClickable(onClick = action)
                                                    .padding(vertical = if (denseTabs) 10.dp else 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    tabIcon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                                                    modifier = Modifier.size(if (denseTabs) 22.dp else 24.dp)
                                                )
                                                Text(
                                                    label,
                                                    color = if (isSelected) appColors.onSurface else appColors.onMuted,
                                                    style = if (denseTabs) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                customRows.forEach { rowButtons ->
                                    HKIBottomBar(
                                        horizontalPadding = if (isPhone) 32.dp else 16.dp,
                                        scrollable = false
                                    ) {
                                        val emptyWeight = (5 - rowButtons.size).coerceAtLeast(0) / 2f
                                        if (emptyWeight > 0f) Spacer(Modifier.weight(emptyWeight))
                                        rowButtons.forEach { button ->
                                            CustomNavButton(
                                                button = button,
                                                entity = customEntities.find { it.entity_id == button.entityId },
                                                currentUrl = currentUrl,
                                                modifier = Modifier.weight(1f),
                                                onTrigger = { trigger ->
                                                    val action = when (trigger) {
                                                        "hold" -> button.holdAction
                                                        "double" -> button.doubleTapAction
                                                        else -> button.tapAction
                                                    }
                                                    handleActionOutcome(
                                                        viewModel.executeAction(action, button.entityId, trigger),
                                                        context, navController
                                                    ) { moreInfoEntityId = it }
                                                }
                                            )
                                        }
                                        if (emptyWeight > 0f) Spacer(Modifier.weight(emptyWeight))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Nested more-info opened by a custom nav-bar button. Reset the custom-buttons local so the
    // nested dialog doesn't inherit (and repeat) this dialog's buttons.
    moreInfoEntityId?.let { id ->
        val target = customEntities.find { it.entity_id == id }
        if (target != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDialogCustomButtons provides emptyList()
            ) {
                EntityMoreInfoDialog(entity = target, viewModel = viewModel, onDismiss = { moreInfoEntityId = null })
            }
        } else {
            moreInfoEntityId = null
        }
    }
}

/** Routes an entity opened from a dialog's custom nav button to its domain-specific more-info
 *  dialog (light/climate/lock/cover/fan/…), falling back to the generic dialog for plain domains. */
@Composable
fun EntityMoreInfoDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val id = entity.entity_id
    when {
        id.startsWith("light.") && entity.supportsBrightness ->
            HKILightDialog(entity = entity, onDismiss = onDismiss, viewModel = viewModel)
        id.startsWith("climate.") -> PagedRoleDialog("climate", listOf(entity), viewModel, onDismiss)
        id.startsWith("lock.")    -> PagedRoleDialog("lock", listOf(entity), viewModel, onDismiss)
        id.startsWith("cover.")   -> PagedRoleDialog("cover", listOf(entity), viewModel, onDismiss)
        id.startsWith("fan.")         -> HKIFanDialog(entity = entity, viewModel = viewModel, onDismiss = onDismiss)
        id.startsWith("humidifier.")  -> HKIHumidifierDialog(entity = entity, viewModel = viewModel, onDismiss = onDismiss)
        id.startsWith("alarm_control_panel.") -> HKIAlarmDialog(entity = entity, viewModel = viewModel, onDismiss = onDismiss)
        id.startsWith("person.")      -> PersonDetailDialog(person = entity, viewModel = viewModel, onDismiss = onDismiss)
        id.startsWith("media_player.") -> HKIMediaPlayerDialog(entity, viewModel, currentUrl, onDismiss)
        else -> GenericEntityDialog(entity = entity, viewModel = viewModel, onDismiss = onDismiss)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomNavButton(
    button: HKIActionButton,
    entity: HAEntity?,
    currentUrl: String,
    modifier: Modifier = Modifier,
    onTrigger: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val tint = entity?.let { entityStateIconColor(it, appColors.onMuted) } ?: appColors.onMuted
    val label = button.name ?: entity?.friendlyName ?: button.entityId.substringAfter(".")
    val pictureUrl = if (button.icon == ENTITY_PICTURE_ICON && entity != null && currentUrl.isNotBlank())
        resolveEntityPictureUrl(entity, currentUrl) else null
    // Configured icon → the entity's own HA icon → the domain default (so a button always has an icon).
    val iconName = button.icon?.takeUnless { it.isBlank() || it == ENTITY_PICTURE_ICON }
        ?: entity?.icon?.substringAfter(":")?.takeUnless { it.isBlank() }
        ?: entity?.let { defaultEntityIconSlug(it) }
    // Dialog actions stay flat. Entity state is communicated by the icon, just like tiles and badges.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemCornerShape())
            .combinedClickable(
                onClick = { onTrigger("tap") },
                onDoubleClick = { onTrigger("double") },
                onLongClick = { onTrigger("hold") }
            )
            .padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            pictureUrl != null -> AsyncImage(model = pictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(CircleShape))
            iconName != null -> MdiIcon(iconName, contentDescription = null, tint = tint, size = 24.dp)
            else -> Icon(Icons.Default.Circle, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            color = appColors.onSurface,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HistoryView(entity: HAEntity, viewModel: MainViewModel, extraGraphEntityIds: List<String> = emptyList()) {
    val appColors = LocalHKIAppColors.current
    val historyMapping by viewModel.historyMapping.collectAsState()
    val historyEntityFlow = remember(viewModel, extraGraphEntityIds) {
        viewModel.entitiesMatching {
            it.entity_id.startsWith("person.") || it.entity_id in extraGraphEntityIds
        }
    }
    val allEntities by historyEntityFlow.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val entries = historyMapping[entity.entity_id] ?: emptyList()
    var selectedHours by remember { mutableIntStateOf(24) }
    var loadedHours by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(entity.entity_id, selectedHours, extraGraphEntityIds) {
        loadedHours = null
        viewModel.fetchEntityHistory(entity.entity_id, selectedHours.toLong())
        extraGraphEntityIds.forEach { id -> viewModel.fetchEntityHistory(id, selectedHours.toLong()) }
        loadedHours = selectedHours
    }

    // Match logbook actors (HA user id) to a person.* entity so their picture can be shown.
    val personByUserId = remember(allEntities) {
        allEntities.filter { it.entity_id.startsWith("person.") }
            .mapNotNull { p -> p.userId?.let { uid -> uid to p } }
            .toMap()
    }
    fun avatarUrlFor(userId: String?): String? {
        val pic = userId?.let { personByUserId[it]?.entityPicture } ?: return null
        return if (pic.startsWith("http")) pic else "$currentUrl$pic"
    }

    val graphColors = listOf(Color(0xFFFF8C00), Color(0xFF1E90FF), Color(0xFF4A90E2), Color(0xFFBE73CC))
    val extraGraphEntities = remember(extraGraphEntityIds, allEntities) {
        extraGraphEntityIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
    }

    val windowEnd = remember(entries, selectedHours) { System.currentTimeMillis() }
    val windowStart = windowEnd - selectedHours.toLong() * 3_600_000L

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        HistoryRangeChips(
            selectedHours = selectedHours,
            onSelect = { selectedHours = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Text("History", style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        Spacer(Modifier.height(10.dp))
        StateTimelineBar(
            entries = entries,
            windowStartMillis = windowStart,
            windowEndMillis = windowEnd,
            modifier = Modifier.fillMaxWidth()
        )

        extraGraphEntities.forEachIndexed { index, sensorEntity ->
            Spacer(Modifier.height(20.dp))
            EntitySensorGraphCard(
                sensorEntity = sensorEntity,
                viewModel = viewModel,
                selectedHours = selectedHours,
                lineColor = graphColors[index % graphColors.size]
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Activity", style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loadedHours == selectedHours) {
                    Text("No activity in this period", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn {
                items(entries.size) { index ->
                    val entry = entries[index]
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActorAvatar(actorName = entry.actorName, pictureUrl = avatarUrlFor(entry.actorId))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val stateName = entry.state.replaceFirstChar { it.uppercase() }
                            val who = entry.actorName?.takeIf { it.isNotBlank() } ?: "System"
                            Text(stateName, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Text(text = who, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        val timeLabel = parseHistoryMillis(entry.last_changed)?.let { formatHistoryClock(it) }
                            ?: entry.last_changed.substringAfter("T").take(8)
                        Text(timeLabel, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

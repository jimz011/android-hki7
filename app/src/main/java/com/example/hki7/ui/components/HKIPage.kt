@file:Suppress("MoveLambdaOutsideParentheses", "UnusedBoxWithConstraintsScope", "SpellCheckingInspection")

package com.example.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import coil.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIAreaConfig
import com.example.hki7.data.HKIBadgeBarConfig
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.ConnectionStatus
import com.example.hki7.ui.screens.SettingsDialog
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HKIPage(
    viewModel: MainViewModel,
    areaId: String? = null,
    title: String? = null,
    subtitle: String? = null,
    subtitleIcon: ImageVector? = null,
    showPeople: Boolean = false,
    onPeopleClick: ((HAEntity) -> Unit)? = null,
    backgroundImage: String? = null,
    headerColor: String? = null,
    pageKey: String? = null,
    pageSettingsTitle: String? = null,
    extraPageSettingsSection: Pair<String, @Composable ColumnScope.() -> Unit>? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val weather by viewModel.weather.collectAsState()
    val people by viewModel.people.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val status by viewModel.status.collectAsState()
    val appColors = LocalHKIAppColors.current
    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val pageConfig = pageKey?.let { pageConfigs[it] } ?: HKIPageConfig()
    val areaConfigs by viewModel.areaConfigsMapping.collectAsState()
    val areaConfig = areaId?.let { areaConfigs[it] }
    val allEntities by viewModel.entities.collectAsState()
    var previewBadgeBarConfig by remember { mutableStateOf<HKIBadgeBarConfig?>(null) }
    val savedBadgeBarConfig = areaConfig?.badgeBar ?: pageConfig.badgeBar
    val badgeBarConfig: HKIBadgeBarConfig = previewBadgeBarConfig ?: savedBadgeBarConfig ?: HKIBadgeBarConfig()
    val prefs = viewModel.prefs
    
    var showWeatherDialog by remember { mutableStateOf(false) }
    var showLeftPillSettings by remember { mutableStateOf(false) }
    var showRightPillSettings by remember { mutableStateOf(false) }
    var headerAlarmDialogEntityId by remember { mutableStateOf<String?>(null) }
    var showRoomConfig by remember { mutableStateOf(false) }
    var showPageConfig by remember { mutableStateOf(false) }
    var previewHeaderColor by remember { mutableStateOf<String?>(null) }
    var pullOffset by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }
    
    val maxPull = 450f 
    val pullOffsetDp = (pullOffset / 3f).dp
    val menuVisible = pullOffset > 120f
    val headerColorSource = previewHeaderColor ?: headerColor ?: pageConfig.headerColor
    val headerColorValue = parseHexColor(headerColorSource)
    val effectiveBackground = if (!headerColorSource.isNullOrBlank()) null else backgroundImage ?: pageConfig.wallpaper
    val hasHeaderMedia = effectiveBackground != null || headerColorValue != null
    val headerContentColor = headerColorValue?.let { if (it.luminance() < 0.45f) Color.White else Color(0xFF111111) }
    val headerTextColor = headerContentColor ?: if (hasHeaderMedia) Color.White else appColors.onSurface
    val headerMutedColor = headerContentColor?.copy(alpha = 0.75f) ?: if (hasHeaderMedia) Color.White.copy(alpha = 0.8f) else appColors.onMuted
    val pillColor = headerContentColor?.copy(alpha = 0.16f) ?: if (hasHeaderMedia) Color.Black.copy(alpha = 0.3f) else appColors.surface.copy(alpha = 0.78f)
    val headerHeight = 236.dp
    val visiblePeople = remember(people, pageConfig) {
        val sorted = when (pageConfig.peopleSort) {
            "custom" -> people.sortedWith(
                compareBy<HAEntity> {
                    val index = pageConfig.customPeopleOrder.indexOf(it.entity_id)
                    if (index == -1) Int.MAX_VALUE else index
                }.thenBy { it.friendlyName ?: it.entity_id }
            )
            "name" -> people.sortedBy { it.friendlyName ?: it.entity_id }
            "name_desc" -> people.sortedByDescending { it.friendlyName ?: it.entity_id }
            else -> people.sortedWith(
                compareBy<HAEntity> { if (it.state == "home") 0 else 1 }
                    .thenByDescending { it.last_changed.orEmpty() }
            )
        }
        if (pageConfig.showPeople) sorted.filterNot { it.entity_id in pageConfig.hiddenPeople } else emptyList()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(appColors.background)
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (pullOffset < 400f) pullOffset = 0f
                },
                onVerticalDrag = { change, dragAmount ->
                    val isHeaderGesture = change.position.y < 260.dp.toPx()
                    val isPullingMenu = pullOffset > 0f || dragAmount > 0f
                    if ((isHeaderGesture || pullOffset > 0f) && isPullingMenu) {
                        change.consume()
                        pullOffset = (pullOffset + dragAmount).coerceIn(0f, maxPull)
                    }
                }
            )
        }
    ) {
        if (effectiveBackground != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight + pullOffsetDp)
                    .align(Alignment.TopCenter)
                    .clipToBounds()
            ) {
                AsyncImage(
                    model = if (effectiveBackground.startsWith("http")) effectiveBackground else "$currentUrl$effectiveBackground",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.42f),
                                    0.5f to Color.Black.copy(alpha = 0.19f),
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Long-Pull Menu - Shifted down 20px
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 40.dp)
                .graphicsLayer {
                    alpha = ((pullOffset - 90f) / 180f).coerceIn(0f, 1f)
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(Icons.Default.Refresh, "Refresh", enabled = menuVisible) { 
                viewModel.refreshEntities()
                pullOffset = 0f
            }
            MenuButton(if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Edit, if (isEditMode) "Done" else "Edit", enabled = menuVisible) { 
                viewModel.toggleEditMode()
                pullOffset = 0f
            }
            if (pageKey != null && pageSettingsTitle != null) {
                MenuButton(Icons.Default.Tune, pageSettingsTitle, enabled = menuVisible) {
                    showPageConfig = true
                    pullOffset = 0f
                }
            }
            if (title != null && title != viewModel.greeting && areaId != null) {
                MenuButton(Icons.Default.Tune, "Room Config", enabled = menuVisible) {
                    showRoomConfig = true
                    pullOffset = 0f
                }
            }
            MenuButton(Icons.Default.Settings, "Settings", enabled = menuVisible) {
                showSettings = true
                pullOffset = 0f
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = pullOffsetDp)
        ) {
            // HKI Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
                    .zIndex(1f)
            ) {
                if (effectiveBackground != null) {
                    Spacer(Modifier.fillMaxSize())
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.30f)
                                        headerColorValue != null -> headerColorValue.copy(alpha = appColors.headerFallbackStart.alpha)
                                        else -> appColors.headerFallbackStart
                                    },
                                    when {
                                        effectiveBackground != null -> Color.Black.copy(alpha = 0.135f)
                                        headerColorValue != null -> headerColorValue.copy(alpha = appColors.headerFallbackStart.alpha * 0.45f)
                                        else -> appColors.headerFallbackStart.copy(alpha = appColors.headerFallbackStart.alpha * 0.45f)
                                    },
                                    appColors.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onBack != null) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp).background(pillColor, CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                val leftDisplayType by viewModel.headerLeftDisplayType.collectAsState()
                                val leftAlarmEntityId by viewModel.headerLeftAlarmEntityId.collectAsState()
                                val leftAlarmEntity = allEntities.find { it.entity_id == leftAlarmEntityId }
                                    ?: allEntities.firstOrNull { it.entity_id.startsWith("alarm_control_panel.") }
                                val use24h by viewModel.use24hFormat.collectAsState()
                                val useFullDayName by viewModel.useFullDayName.collectAsState()
                                HeaderStatusPill(
                                    displayType = leftDisplayType,
                                    weather = weather,
                                    alarm = leftAlarmEntity,
                                    use24hFormat = use24h,
                                    useFullDayName = useFullDayName,
                                    isEditMode = isEditMode,
                                    pillColor = pillColor,
                                    textColor = headerTextColor,
                                    editSurfaceColor = appColors.surface.copy(alpha = 0.7f),
                                    onSettingsClick = { showLeftPillSettings = true },
                                    onClick = {
                                        when (leftDisplayType) {
                                            "Weather", "DateTime" -> showWeatherDialog = true
                                            "Alarm" -> leftAlarmEntity?.let { headerAlarmDialogEntityId = it.entity_id }
                                        }
                                    }
                                )
                            }

                            val weatherDisplayType by viewModel.weatherDisplayType.collectAsState()
                            val rightAlarmEntityId by viewModel.headerAlarmEntityId.collectAsState()
                            val rightAlarmEntity = allEntities.find { it.entity_id == rightAlarmEntityId }
                                ?: allEntities.firstOrNull { it.entity_id.startsWith("alarm_control_panel.") }
                            val showPill = weatherDisplayType != "None"
                            Box(
                                modifier = if (!showPill && isEditMode) Modifier.size(36.dp) else Modifier,
                                contentAlignment = Alignment.Center
                            ) {
                                if (showPill) {
                                    Surface(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                if (!isEditMode) {
                                                    when (weatherDisplayType) {
                                                        "Weather", "DateTime" -> showWeatherDialog = true
                                                        "Alarm" -> rightAlarmEntity?.let { headerAlarmDialogEntityId = it.entity_id }
                                                    }
                                                }
                                            },
                                        color = pillColor,
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val now = LocalDateTime.now()
                                            val use24h by viewModel.use24hFormat.collectAsState()
                                            val useFullDayName by viewModel.useFullDayName.collectAsState()
                                            val timePattern = if (use24h) "HH:mm" else "hh:mm a"
                                            val dayPattern = if (useFullDayName) "EEEE" else "EEE"
                                            
                                            val displayStr = when(weatherDisplayType) {
                                                "Date" -> now.format(DateTimeFormatter.ofPattern("$dayPattern, MMM d"))
                                                "Time" -> now.format(DateTimeFormatter.ofPattern(timePattern))
                                                "DateTime" -> now.format(DateTimeFormatter.ofPattern("$dayPattern d, $timePattern"))
                                                "Alarm" -> rightAlarmEntity?.state?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Alarm"
                                                else -> "${weather?.state?.let { formatWeatherState(it) } ?: "Cloudy"} ${weather?.temperature?.toInt() ?: 12}°C"
                                            }

                                            if (weatherDisplayType == "Weather" || weatherDisplayType == "DateTime" || weatherDisplayType == "Alarm") {
                                                if (weatherDisplayType == "Alarm") {
                                                    MdiIcon(
                                                        name = rightAlarmEntity?.let { defaultEntityIconSlug(it) } ?: "shield-home",
                                                        contentDescription = null,
                                                        tint = alarmStateColor(rightAlarmEntity?.state.orEmpty()),
                                                        size = 18.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = when (weather?.state?.lowercase()) {
                                                            "cloudy" -> Icons.Default.Cloud
                                                            "rainy" -> Icons.Default.CloudQueue
                                                            "sunny" -> Icons.Default.WbSunny
                                                            else -> Icons.Default.Cloud
                                                        },
                                                        contentDescription = null,
                                                        tint = headerTextColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                if (displayStr.isNotEmpty()) Spacer(Modifier.width(8.dp))
                                            }
                                            
                                            if (displayStr.isNotEmpty()) {
                                                Text(
                                                    text = displayStr,
                                                    color = headerTextColor,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (isEditMode) {
                                    val overlayModifier = if (showPill) Modifier.matchParentSize() else Modifier.fillMaxSize()
                                    Surface(
                                        modifier = overlayModifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { showRightPillSettings = true },
                                        color = appColors.surface.copy(alpha = 0.7f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Settings, null, tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val showPeopleRow = showPeople && visiblePeople.isNotEmpty()
                            val avatarSize = 44.dp
                            val avatarOverlap = 8.dp
                            val avatarStep = avatarSize - avatarOverlap
                            fun avatarRowWidth(count: Int) = if (count <= 0) 0.dp else avatarSize + avatarStep * (count - 1)
                            fun rowCapacity(availableWidth: androidx.compose.ui.unit.Dp): Int {
                                if (!showPeopleRow || availableWidth <= 0.dp) return 0
                                var count = 0
                                for (candidate in 1..visiblePeople.size) {
                                    if (avatarRowWidth(candidate) <= availableWidth) count = candidate
                                }
                                return count
                            }
                            val inlinePeopleCapacity = rowCapacity(maxWidth - 228.dp)
                            val inlinePeople = if (showPeopleRow) visiblePeople.take(inlinePeopleCapacity) else emptyList()
                            val wrappedPeople = if (showPeopleRow) visiblePeople.drop(inlinePeopleCapacity) else emptyList()
                            val wrappedPeopleCapacity = rowCapacity(maxWidth).coerceAtLeast(1)
                            val wrappedPeopleRows = wrappedPeople.chunked(if (inlinePeopleCapacity > 0) inlinePeopleCapacity else wrappedPeopleCapacity)

                            if (inlinePeopleCapacity == 0) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = title ?: viewModel.greeting,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = headerTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 40.sp,
                                            lineHeight = 44.sp
                                        )
                                        HeaderSubtitle(
                                            text = subtitle ?: (if (title == null) displayName else (if (status == ConnectionStatus.ERROR) "Connection Error" else "All systems normal")),
                                            icon = subtitleIcon,
                                            color = headerMutedColor
                                        )
                                    }
                                    if (showPeopleRow) {
                                        Spacer(Modifier.height(8.dp))
                                        visiblePeople.chunked(wrappedPeopleCapacity).forEach { rowPeople ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy((-8).dp, Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                rowPeople.forEach { person ->
                                                    PersonAvatar(
                                                        person = person,
                                                        currentUrl = currentUrl,
                                                        isEditMode = isEditMode,
                                                        headerTextColor = headerTextColor,
                                                        onClick = { onPeopleClick?.invoke(person) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = if (showPeopleRow) 12.dp else 0.dp)
                                    ) {
                                        Text(
                                            text = title ?: viewModel.greeting,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = headerTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 40.sp,
                                            lineHeight = 44.sp
                                        )
                                        HeaderSubtitle(
                                            text = subtitle ?: (if (title == null) displayName else (if (status == ConnectionStatus.ERROR) "Connection Error" else "All systems normal")),
                                            icon = subtitleIcon,
                                            color = headerMutedColor
                                        )
                                    }

                                    if (showPeopleRow) {
                                        Column(
                                            modifier = Modifier.width(avatarRowWidth(inlinePeopleCapacity)),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy((-8).dp, Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                inlinePeople.forEach { person ->
                                                    PersonAvatar(
                                                        person = person,
                                                        currentUrl = currentUrl,
                                                        isEditMode = isEditMode,
                                                        headerTextColor = headerTextColor,
                                                        onClick = { onPeopleClick?.invoke(person) }
                                                    )
                                                }
                                            }
                                            wrappedPeopleRows.forEach { rowPeople ->
                                                Spacer(Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy((-8).dp, Alignment.End),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    rowPeople.forEach { person ->
                                                        PersonAvatar(
                                                            person = person,
                                                            currentUrl = currentUrl,
                                                            isEditMode = isEditMode,
                                                            headerTextColor = headerTextColor,
                                                            onClick = { onPeopleClick?.invoke(person) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            HKIBadgeBar(
                badgeBarConfig = badgeBarConfig,
                allEntities    = allEntities,
                isEditMode     = isEditMode,
                viewModel      = viewModel,
                onConfigChange = { newBarConfig ->
                    if (areaId != null) {
                        viewModel.updateAreaConfig(
                            areaId,
                            (areaConfig ?: HKIAreaConfig()).copy(badgeBar = newBarConfig)
                        )
                    } else if (pageKey != null) {
                        viewModel.updatePageConfig(pageKey, pageConfig.copy(badgeBar = newBarConfig))
                    }
                }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content(PaddingValues())
            }
        }
        
        // Done Editing FAB removed to avoid overlap with widget selector

        if (showWeatherDialog && weather != null) {
            HKIWeatherDialog(
                weather = weather!!, 
                onDismiss = { showWeatherDialog = false },
                viewModel = viewModel
            )
        }

        if (showLeftPillSettings && weather != null) {
            val leftDisplayType by viewModel.headerLeftDisplayType.collectAsState()
            val leftAlarmEntityId by viewModel.headerLeftAlarmEntityId.collectAsState()
            HKIWeatherDialog(
                weather = weather!!,
                onDismiss = { showLeftPillSettings = false },
                viewModel = viewModel,
                settingsTitle = "Left Header Pill",
                displayType = leftDisplayType,
                alarmEntityId = leftAlarmEntityId,
                onDisplayTypeSelected = { viewModel.setHeaderLeftDisplayType(it) },
                onAlarmEntitySelected = { viewModel.setHeaderLeftAlarmEntity(it) }
            )
        }

        if (showRightPillSettings && weather != null) {
            val rightDisplayType by viewModel.weatherDisplayType.collectAsState()
            val rightAlarmEntityId by viewModel.headerAlarmEntityId.collectAsState()
            HKIWeatherDialog(
                weather = weather!!,
                onDismiss = { showRightPillSettings = false },
                viewModel = viewModel,
                settingsTitle = "Right Header Pill",
                displayType = rightDisplayType,
                alarmEntityId = rightAlarmEntityId,
                onDisplayTypeSelected = { viewModel.setWeatherDisplayType(it) },
                onAlarmEntitySelected = { viewModel.setHeaderAlarmEntity(it) }
            )
        }

        headerAlarmDialogEntityId?.let { entityId ->
            allEntities.find { it.entity_id == entityId }?.let { entity ->
                HKIAlarmDialog(
                    entity = entity,
                    viewModel = viewModel,
                    onDismiss = { headerAlarmDialogEntityId = null }
                )
            }
        }
        
        if (showSettings) {
            SettingsDialog(
                prefs = prefs,
                viewModel = viewModel,
                onDismiss = { showSettings = false }
            )
        }

        if (showRoomConfig && areaId != null) {
            RoomConfigDialog(
                areaId = areaId,
                viewModel = viewModel,
                onHeaderColorPreview = { previewHeaderColor = it },
                onBadgeBarPreview = { previewBadgeBarConfig = it },
                onDismiss = {
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showRoomConfig = false
                }
            )
        }
        if (showPageConfig && pageKey != null) {
            PageSettingsDialog(
                title = pageSettingsTitle ?: "Page Settings",
                config = pageConfig,
                people = people,
                showPeopleSettings = showPeople,
                extraSection = extraPageSettingsSection,
                onHeaderColorPreview = { previewHeaderColor = it },
                onBadgeBarPreview = { previewBadgeBarConfig = it },
                onDismiss = {
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showPageConfig = false
                },
                onSave = { config ->
                    viewModel.updatePageConfig(pageKey, config)
                    previewHeaderColor = null
                    previewBadgeBarConfig = null
                    showPageConfig = false
                }
            )
        }
    }
}

@Composable
private fun PersonAvatar(
    person: HAEntity,
    currentUrl: String,
    isEditMode: Boolean,
    headerTextColor: Color,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val imageUrl = person.entityPicture?.let {
        if (it.startsWith("http")) it else "$currentUrl$it"
    }
    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable { if (!isEditMode) onClick() },
            shape = CircleShape,
            border = BorderStroke(1.dp, headerTextColor.copy(alpha = 0.7f)),
            color = appColors.elevated
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = person.friendlyName,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (person.state != "home") grayscaleFilter else null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = appColors.onMuted)
            }
        }

        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onClick() },
                color = appColors.surface.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, null, tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PageSettingsDialog(
    title: String,
    config: HKIPageConfig,
    people: List<HAEntity>,
    showPeopleSettings: Boolean,
    extraSection: Pair<String, @Composable ColumnScope.() -> Unit>? = null,
    onHeaderColorPreview: (String?) -> Unit = {},
    onBadgeBarPreview: (HKIBadgeBarConfig?) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (HKIPageConfig) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var wallpaper by remember(config) { mutableStateOf(config.wallpaper ?: "") }
    var headerColorText by remember(config) { mutableStateOf(config.headerColor ?: "") }
    var headerRgb by remember(config) { mutableStateOf(hexToRgb(config.headerColor) ?: listOf(155, 83, 83)) }
    var showPeople by remember(config) { mutableStateOf(config.showPeople) }
    var peopleSort by remember(config) { mutableStateOf(config.peopleSort) }
    var hiddenPeople by remember(config) { mutableStateOf(config.hiddenPeople) }
    var badgeBarEnabled by remember(config) { mutableStateOf(config.badgeBar?.visible ?: true) }
    var badgeAlignment by remember(config) { mutableStateOf(config.badgeBar?.alignment ?: "split") }
    var badgeSpanIcons by remember(config) { mutableStateOf(config.badgeBar?.spanIcons ?: false) }
    var badgeLeftOverflow by remember(config) { mutableStateOf(config.badgeBar?.leftOverflow ?: false) }
    var badgeRightOverflow by remember(config) { mutableStateOf(config.badgeBar?.rightOverflow ?: false) }
    var section by remember { mutableStateOf("menu") }
    var customOrder by remember(config, people) {
        mutableStateOf(
            (config.customPeopleOrder + people.map { it.entity_id })
                .distinct()
                .filter { id -> people.any { it.entity_id == id } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (section == "menu") title else if (section == "extra") extraSection?.first ?: title else section.replaceFirstChar { it.uppercase() }) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (section == "menu") {
                    SettingsMenuChoice(Icons.Default.Image, "Header", "Wallpaper and custom header color") { section = "header" }
                    SettingsMenuChoice(Icons.Default.ViewStream, "Badge Bar", "Visibility, alignment, and display options") { section = "badgebar" }
                    if (showPeopleSettings) {
                        SettingsMenuChoice(Icons.Default.Person, "Persons", "Visibility and ordering") { section = "persons" }
                    }
                    if (extraSection != null) {
                        SettingsMenuChoice(Icons.Default.Tune, extraSection.first, "Configure") { section = "extra" }
                    }
                } else {
                    TextButton(onClick = { section = "menu" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                if (section == "header") {
                    OutlinedTextField(
                        value = wallpaper,
                        onValueChange = { wallpaper = it },
                        label = { Text("Header wallpaper URL or path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerColorText,
                        onValueChange = {
                            headerColorText = it
                            onHeaderColorPreview(it.ifBlank { null })
                        },
                        label = { Text("Header custom color (#RRGGBB)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorWheel(
                        selectedRgb = headerRgb,
                        onColorSelected = { rgb ->
                            headerRgb = rgb
                            headerColorText = rgbToHex(rgb)
                            onHeaderColorPreview(headerColorText)
                        },
                        onValueChangeFinished = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(220.dp)
                    )
                }
                if (section == "persons" && showPeopleSettings) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPeople, onCheckedChange = { showPeople = it })
                        Text("Show persons")
                    }
                    Text("Persons order", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = peopleSort == "custom",
                            onClick = { peopleSort = "custom" },
                            label = { Text("Custom") }
                        )
                    }
                    if (peopleSort == "custom") {
                        ReorderableGrid(
                            items = customOrder,
                            canReorder = true,
                            onReorder = { from, to ->
                                customOrder = customOrder.toMutableList().apply {
                                    add(to, removeAt(from))
                                }
                            },
                            key = { it },
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            isNested = true
                        ) { personId, _ ->
                            people.find { it.entity_id == personId }?.let { person ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = appColors.subtleSurface
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = appColors.onSurface.copy(alpha = 0.75f))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(person.friendlyName ?: person.entity_id, color = appColors.onSurface)
                                            Text(person.state, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Icon(Icons.Default.DragIndicator, null, tint = appColors.onMuted)
                                    }
                                }
                            }
                        }
                    }
                    if (people.isNotEmpty()) {
                        Text("Hidden persons", style = MaterialTheme.typography.labelLarge)
                        people.forEach { person ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = person.entity_id !in hiddenPeople,
                                    onCheckedChange = { checked ->
                                        hiddenPeople = if (checked) hiddenPeople - person.entity_id else (hiddenPeople + person.entity_id).distinct()
                                    }
                                )
                                Text(person.friendlyName ?: person.entity_id)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = peopleSort == "changed",
                            onClick = { peopleSort = "changed" },
                            label = { Text("State") }
                        )
                        FilterChip(
                            selected = peopleSort == "name",
                            onClick = { peopleSort = "name" },
                            label = { Text("Name") }
                        )
                        FilterChip(
                            selected = peopleSort == "name_desc",
                            onClick = { peopleSort = "name_desc" },
                            label = { Text("Reverse") }
                        )
                    }
                }
                if (section == "extra" && extraSection != null) {
                    extraSection.second(this)
                }
                if (section == "badgebar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show badge bar", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = badgeBarEnabled,
                            onCheckedChange = {
                                badgeBarEnabled = it
                                onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = it, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                            }
                        )
                    }
                    Text("Alignment", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("split" to "Split", "left" to "Left", "center" to "Center", "right" to "Right").forEach { (value, label) ->
                            FilterChip(
                                selected = badgeAlignment == value,
                                onClick = {
                                    badgeAlignment = value
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = value, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (badgeAlignment == "center") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Span badges", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeSpanIcons,
                                onCheckedChange = {
                                    badgeSpanIcons = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = it, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                                }
                            )
                        }
                    }
                    if (badgeAlignment == "split") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Left side overflows right", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeLeftOverflow,
                                onCheckedChange = {
                                    badgeLeftOverflow = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = it, rightOverflow = badgeRightOverflow))
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Right side overflows left", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = badgeRightOverflow,
                                onCheckedChange = {
                                    badgeRightOverflow = it
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = it))
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    config.copy(
                        wallpaper = wallpaper.ifBlank { null },
                        headerColor = headerColorText.ifBlank { null },
                        showPeople = showPeople,
                        peopleSort = peopleSort,
                        customPeopleOrder = customOrder,
                        hiddenPeople = hiddenPeople,
                        badgeBar = (config.badgeBar ?: HKIBadgeBarConfig()).copy(
                            visible = badgeBarEnabled,
                            alignment = badgeAlignment,
                            spanIcons = badgeSpanIcons,
                            leftOverflow = badgeLeftOverflow,
                            rightOverflow = badgeRightOverflow
                        )
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsMenuChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted)
        }
    }
}

@Composable
fun MenuButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = enabled) { onClick() }) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = appColors.subtleSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = appColors.onSurface)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = appColors.onSurface, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeaderStatusPill(
    displayType: String,
    weather: HAEntity?,
    alarm: HAEntity?,
    use24hFormat: Boolean,
    useFullDayName: Boolean,
    isEditMode: Boolean,
    pillColor: Color,
    textColor: Color,
    editSurfaceColor: Color,
    onSettingsClick: () -> Unit,
    onClick: () -> Unit
) {
    val showPill = displayType != "None"
    Box(
        modifier = if (!showPill && isEditMode) Modifier.size(36.dp) else Modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showPill) {
            val now = LocalDateTime.now()
            val timePattern = if (use24hFormat) "HH:mm" else "hh:mm a"
            val dayPattern = if (useFullDayName) "EEEE" else "EEE"
            val displayText = when (displayType) {
                "Date" -> now.format(DateTimeFormatter.ofPattern("$dayPattern, MMM d"))
                "Time" -> now.format(DateTimeFormatter.ofPattern(timePattern))
                "DateTime" -> now.format(DateTimeFormatter.ofPattern("$dayPattern d, $timePattern"))
                "Alarm" -> alarm?.state?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Alarm"
                else -> "${weather?.state?.let { formatWeatherState(it) } ?: "Cloudy"} ${weather?.temperature?.toInt() ?: 12}°C"
            }
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { if (!isEditMode) onClick() },
                color = pillColor,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayType == "Alarm") {
                        MdiIcon(
                            name = alarm?.let { defaultEntityIconSlug(it) } ?: "shield-home",
                            contentDescription = null,
                            tint = alarmStateColor(alarm?.state.orEmpty()),
                            size = 18.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        val icon = when (displayType) {
                            "Weather", "DateTime" -> weatherIcon(weather?.state.orEmpty())
                            else -> null
                        }
                        if (icon != null) {
                            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    Text(displayText, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (isEditMode) {
            val overlayModifier = if (showPill) Modifier.matchParentSize() else Modifier.fillMaxSize()
            Surface(
                modifier = overlayModifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSettingsClick() },
                color = editSurfaceColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, null, tint = textColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderSubtitle(text: String, icon: ImageVector?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

private fun parseHexColor(value: String?): Color? {
    val hex = value?.takeIf { it.isNotBlank() } ?: return null
    val normalized = if (hex.startsWith("#")) hex else "#$hex"
    return runCatching { Color(normalized.toColorInt()) }.getOrNull()
}

private fun hexToRgb(value: String?): List<Int>? {
    val hex = value?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
    return runCatching {
        listOf(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16))
    }.getOrNull()
}

private fun rgbToHex(rgb: List<Int>): String {
    val safe = List(3) { index -> rgb.getOrNull(index)?.coerceIn(0, 255) ?: 0 }
    return "#%02X%02X%02X".format(safe[0], safe[1], safe[2])
}

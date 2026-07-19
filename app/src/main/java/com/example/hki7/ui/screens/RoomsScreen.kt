package com.example.hki7.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudDownload
import com.example.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import com.example.hki7.data.HAArea
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.mediaPlayerStateIcon
import com.example.hki7.data.HAFloor
import com.example.hki7.data.HKIAreaConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.RoomStatusRoles
import com.example.hki7.ui.resolveRoomMediaStatus
import com.example.hki7.ui.resolveRoomStatus
import com.example.hki7.ui.roomMediaPlayerIds
import com.example.hki7.ui.roomEntityIds
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.GradientActionButton
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.RoomConfigDialog
import com.example.hki7.ui.components.RoomEnvironmentSummary
import com.example.hki7.ui.components.RoomStatusIndicators
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.LocalItemCornerRadius
import com.example.hki7.ui.components.itemCornerShape
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlin.math.max

@Composable
fun RoomsScreen(viewModel: MainViewModel, navController: NavController) {
    val areas by viewModel.areas.collectAsState()
    val floors by viewModel.floors.collectAsState()
    val configs by viewModel.areaConfigsMapping.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val dashboardMode by viewModel.dashboardMode.collectAsState()
    val autoGenerationPending by viewModel.prefs.pendingAutoTakeover.collectAsState(initial = false)
    val collapsedFloorIds by viewModel.collapsedFloorIds.collectAsState()

    var showAutoInfo by remember { mutableStateOf(false) }
    var showAddRoom by remember { mutableStateOf(false) }
    var showAddFloor by remember { mutableStateOf(false) }
    var editingAreaId by remember { mutableStateOf<String?>(null) }
    var editingFloor by remember { mutableStateOf<HAFloor?>(null) }
    var showRoomsReimport by remember { mutableStateOf(false) }
    var showClearRooms by remember { mutableStateOf(false) }

    val roomsImportSettings: Pair<String, @Composable androidx.compose.foundation.layout.ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Re-import" to { _ ->
            Text("Fetch rooms, floors, and their entities from Home Assistant again.", color = LocalHKIAppColors.current.onMuted)
            Button(onClick = { showRoomsReimport = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Re-import Rooms")
            }
            OutlinedButton(onClick = { showClearRooms = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear Rooms View", color = MaterialTheme.colorScheme.error)
            }
        }

    val groupedFloors = remember(areas, floors, configs) {
        buildFloorSections(areas, floors, configs)
    }
    val roomsScrollState = rememberScrollState()

    HKIPage(
        viewModel = viewModel,
        title = "Rooms",
        showPeople = false,
        pageKey = "rooms",
        pageSettingsTitle = "Rooms Settings",
        extraPageSettingsSection = roomsImportSettings,
        navController = navController
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (groupedFloors.isEmpty() && !isEditMode) {
                if (autoGenerationPending) {
                    RoomsImportProgress(Modifier.fillMaxSize(), centered = true)
                } else {
                    EmptyEditHint(
                        Modifier.fillMaxSize(),
                        "This is an empty rooms view. You can add floors and rooms by swiping down on the header and enabling edit mode."
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(roomsScrollState)
                        .padding(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = (if (isEditMode) 156.dp else 96.dp) + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current
                        ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (autoGenerationPending) {
                        RoomsImportProgress(Modifier.fillMaxWidth())
                    }
                    packFloorRows(groupedFloors).forEach { floorRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            floorRow.forEach { section ->
                                val units = if (section.floor?.width == "half") 1f else 2f
                                FloorSection(
                                floor = section.floor,
                                areas = section.areas,
                                configs = configs,
                                viewModel = viewModel,
                                baseUrl = currentUrl,
                                isEditMode = isEditMode,
                                dashboardMode = dashboardMode,
                                scrollState = roomsScrollState,
                                isCollapsed = section.key in collapsedFloorIds,
                                onToggleCollapsed = { viewModel.toggleFloorCollapsed(section.key) },
                                onDeleteFloor = { section.floor?.takeUnless { it.floor_id == "__rooms__" }?.let { viewModel.deleteFloor(it.floor_id) } },
                                onSettingsFloor = { editingFloor = section.floor ?: HAFloor(floor_id = "__rooms__", name = "Rooms") },
                                onMoveArea = { from, to ->
                                    val fromId = section.areas.getOrNull(from)?.area_id ?: return@FloorSection
                                    val toId = section.areas.getOrNull(to)?.area_id ?: return@FloorSection
                                    val allFrom = areas.indexOfFirst { it.area_id == fromId }
                                    val allTo = areas.indexOfFirst { it.area_id == toId }
                                    if (allFrom >= 0 && allTo >= 0) viewModel.moveArea(allFrom, allTo)
                                },
                                onDeleteArea = { viewModel.deleteArea(it) },
                                onSettingsArea = { editingAreaId = it },
                                onClickArea = { areaId ->
                                    if (!isEditMode) navController.navigate(Screen.RoomDetail.createRoute(areaId))
                                },
                                    modifier = Modifier.weight(units)
                                )
                            }
                            val usedUnits = floorRow.sumOf { if (it.floor?.width == "half") 1 else 2 }
                            if (usedUnits < 2) Spacer(Modifier.weight((2 - usedUnits).toFloat()))
                        }
                    }
                }
            }

            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 87.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AddAreaCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (dashboardMode == "auto") showAutoInfo = true else showAddRoom = true
                        }
                    )
                    AddFloorCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (dashboardMode == "auto") showAutoInfo = true else showAddFloor = true
                        }
                    )
                }
            }
        }
    }

    if (showAutoInfo) {
        AlertDialog(
            onDismissRequest = { showAutoInfo = false },
            title = { Text("Rooms are imported") },
            text = { Text("This dashboard is currently being generated from Home Assistant. You can edit rooms as soon as the one-time import finishes.") },
            confirmButton = { Button(onClick = { showAutoInfo = false }) { Text("OK") } }
        )
    }

    if (showRoomsReimport) {
        AlertDialog(
            onDismissRequest = { showRoomsReimport = false },
            title = { Text("Re-import rooms") },
            text = { Text("Import only rooms and entities that have not been edited, or remove all edited rooms and floors and import everything from scratch.") },
            confirmButton = { Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { viewModel.reimportRooms(false); showRoomsReimport = false }) { Text("Import unedited") }
                TextButton(onClick = { viewModel.reimportRooms(true); showRoomsReimport = false }) { Text("Remove edits and import all", color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(onClick = { showRoomsReimport = false }) { Text("Cancel") } }
        )
    }
    if (showClearRooms) {
        AlertDialog(
            onDismissRequest = { showClearRooms = false },
            title = { Text("Clear rooms view?") },
            text = { Text("This removes all imported rooms and floors from this view.") },
            confirmButton = { TextButton(onClick = { viewModel.clearRoomImports(); showClearRooms = false }) { Text("Clear", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showClearRooms = false }) { Text("Cancel") } }
        )
    }

    if (showAddRoom) {
        AddRoomDialog(
            floors = floors,
            onDismiss = { showAddRoom = false },
            onCreate = { roomName, floorId, newFloorName ->
                val targetFloorId = if (newFloorName.isNotBlank()) {
                    viewModel.addManualFloor(newFloorName.trim())?.floor_id
                } else {
                    floorId
                }
                viewModel.addManualArea(roomName.trim(), targetFloorId)
                showAddRoom = false
            }
        )
    }

    if (showAddFloor) {
        AddFloorDialog(
            onDismiss = { showAddFloor = false },
            onCreate = { name ->
                viewModel.addManualFloor(name.trim())
                showAddFloor = false
            }
        )
    }

    editingAreaId?.let { areaId ->
        RoomConfigDialog(
            areaId = areaId,
            viewModel = viewModel,
            onDismiss = { editingAreaId = null }
        )
    }

    editingFloor?.let { floor ->
        FloorSettingsDialog(
            floor = floor,
            onDismiss = { editingFloor = null },
            onSave = {
                viewModel.updateFloor(it)
                editingFloor = null
            }
        )
    }
}

@Composable
private fun RoomsImportProgress(modifier: Modifier = Modifier, centered: Boolean = false) {
    val appColors = LocalHKIAppColors.current
    Box(modifier = modifier, contentAlignment = if (centered) Alignment.Center else Alignment.TopCenter) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = appColors.subtleSurface,
            modifier = if (centered) Modifier.padding(24.dp) else Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Column {
                    Text("Generating your rooms", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
                    Text(
                        "Importing areas and entities from Home Assistant…",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                }
            }
        }
    }
}

private data class FloorSectionData(val key: String, val floor: HAFloor?, val areas: List<HAArea>)

// Packs floor sections into rows: a "full" floor takes a whole row, while "half" floors pair up
// two per row (like full/half-row widgets), so the whole section resizes — not the cards inside.
private fun packFloorRows(sections: List<FloorSectionData>): List<List<FloorSectionData>> {
    val rows = mutableListOf<List<FloorSectionData>>()
    var current = mutableListOf<FloorSectionData>()
    var used = 0
    for (section in sections) {
        val units = if (section.floor?.width == "half") 1 else 2
        if (used + units > 2 && current.isNotEmpty()) {
            rows.add(current.toList())
            current = mutableListOf()
            used = 0
        }
        current.add(section)
        used += units
    }
    if (current.isNotEmpty()) rows.add(current.toList())
    return rows
}

private fun buildFloorSections(
    areas: List<HAArea>,
    floors: List<HAFloor>,
    configs: Map<String, HKIAreaConfig>
): List<FloorSectionData> {
    val layoutOnlyFloor = floors.firstOrNull { it.floor_id == "__rooms__" }
    val importedFloors = floors.filterNot { it.floor_id == "__rooms__" }
    val knownFloorIds = importedFloors.map { it.floor_id }.toSet()
    val byFloor = areas.groupBy { area -> configs[area.area_id]?.floorId ?: area.floor_id }
    val sections = importedFloors.map { floor -> FloorSectionData(floor.floor_id, floor, byFloor[floor.floor_id].orEmpty()) }
        .filter { it.areas.isNotEmpty() || importedFloors.isNotEmpty() }
    val unassigned = byFloor.filterKeys { it == null || it !in knownFloorIds }.values.flatten()
    return if (unassigned.isNotEmpty()) sections + FloorSectionData("__rooms__", layoutOnlyFloor, unassigned) else sections
}

@Composable
private fun FloorSection(
    floor: HAFloor?,
    areas: List<HAArea>,
    configs: Map<String, HKIAreaConfig>,
    viewModel: MainViewModel,
    baseUrl: String,
    isEditMode: Boolean,
    dashboardMode: String,
    scrollState: ScrollState,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onDeleteFloor: () -> Unit,
    onSettingsFloor: () -> Unit,
    onMoveArea: (Int, Int) -> Unit,
    onDeleteArea: (String) -> Unit,
    onSettingsArea: (String) -> Unit,
    onClickArea: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { onToggleCollapsed() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (floor?.icon != "None") {
                MdiIcon(floor?.icon, tint = appColors.onMuted, size = 16.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(floor?.name ?: "Rooms", color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Expand floor" else "Collapse floor",
                tint = appColors.onMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.weight(1f))
            if (isEditMode) {
                if (dashboardMode != "auto" && floor != null && floor.floor_id != "__rooms__") {
                    IconButton(onClick = onDeleteFloor, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete floor", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onSettingsFloor, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Floor settings",
                        tint = appColors.onMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!isCollapsed) {
            Spacer(Modifier.height(12.dp))

            val gridColumns = floor?.columns?.coerceIn(1, 3) ?: 1
            val compactTiles = floor?.compactTiles ?: true
            val cardHeight = if (compactTiles) 112 else 160
            val rowHeight = if (floor?.isSquare == true) 180 else cardHeight + 12
            val rows = max(1, (areas.size + gridColumns - 1) / gridColumns)
            ReorderableGrid(
                items = areas,
                canReorder = isEditMode,
                onReorder = onMoveArea,
                key = { it.area_id },
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                isNested = true,
                autoScrollState = scrollState,
                modifier = Modifier.heightIn(min = rowHeight.dp, max = (rows * rowHeight).dp).fillMaxWidth()
            ) { area, isDragging ->
                AreaCard(
                    area = area,
                    config = configs[area.area_id] ?: HKIAreaConfig(),
                    viewModel = viewModel,
                    baseUrl = baseUrl,
                    isEditMode = isEditMode,
                    canDelete = dashboardMode != "auto",
                    isDragging = isDragging,
                    isSquare = floor?.isSquare == true,
                    compactTiles = compactTiles,
                    cornerRadius = LocalItemCornerRadius.current,
                    onDelete = { onDeleteArea(area.area_id) },
                    onSettings = { onSettingsArea(area.area_id) },
                    onClick = { onClickArea(area.area_id) }
                )
            }
        }
    }
}

@Composable
fun AreaCard(
    area: HAArea,
    config: HKIAreaConfig,
    viewModel: MainViewModel,
    baseUrl: String,
    isEditMode: Boolean,
    canDelete: Boolean,
    isDragging: Boolean,
    isSquare: Boolean = false,
    compactTiles: Boolean = true,
    cornerRadius: Int = LocalItemCornerRadius.current,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val headerColor = remember(config.headerColor) { parseRoomHeaderColor(config.headerColor) }
    // Match HKIPage: a custom header color takes precedence over the room wallpaper/picture.
    val imageSource = if (headerColor != null) null else config.wallpaper ?: area.picture
    val imageUrl = imageSource?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
    val roomCardColor = headerColor?.copy(
        alpha = if (appColors.background.luminance() < 0.5f) {
            0.45f
        } else if (headerColor.luminance() < 0.35f) {
            0.28f
        } else {
            0.18f
        }
    )
    val roomCardBrush = roomCardColor?.let { color ->
        Brush.verticalGradient(
            listOf(
                color.compositeOver(appColors.background),
                color.copy(alpha = color.alpha * 0.45f).compositeOver(appColors.background),
                appColors.background
            )
        )
    } ?: SolidColor(appColors.subtleSurface)
    val scale by animateFloatAsState(if (isDragging) 1.05f else if (isEditMode) 0.95f else 1f, label = "room-scale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSquare) Modifier.aspectRatio(1f) else Modifier.height(if (compactTiles) 112.dp else 160.dp))
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .clickable(enabled = !isEditMode) { onClick() },
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(roomCardBrush),
                    contentAlignment = Alignment.Center
                ) {
                    if (config.icon != "None") {
                        MdiIcon(config.icon ?: area.icon, tint = appColors.onMuted, size = 48.dp)
                    }
                }
            }

            val mediaPlayerIds = remember(config) { config.roomMediaPlayerIds() }
            val dependencyIds = remember(config, mediaPlayerIds) {
                (config.roomEntityIds() + mediaPlayerIds).distinct()
            }
            val dependencyFlow = remember(viewModel, dependencyIds) { viewModel.entitiesFor(dependencyIds) }
            val roomEntities by dependencyFlow.collectAsState()
            val mediaPlayers = remember(mediaPlayerIds, roomEntities) {
                val byId = roomEntities.associateBy(HAEntity::entity_id)
                mediaPlayerIds.map { id -> byId[id] ?: HAEntity(entity_id = id, state = "unavailable") }
            }
            val mediaSummary = remember(mediaPlayers) { resolveRoomMediaStatus(mediaPlayers) }
            val mediaStatus = mediaSummary.text
            val mediaIcon = mediaPlayerStateIcon(mediaSummary.representative)
            val roomSummary = remember(config, roomEntities) {
                resolveRoomStatus(config, roomEntities)
            }
            val topIndicatorKinds = roomSummary.indicators.count { it.role in ROOM_CARD_TOP_STATUS_ROLES }
            val bottomIndicatorKinds = roomSummary.indicators.count { it.role in ROOM_CARD_BOTTOM_STATUS_ROLES }
            val topIndicatorPadding = when (topIndicatorKinds) {
                0 -> 0.dp
                1 -> 42.dp
                2 -> 76.dp
                else -> 112.dp
            }
            val bottomIndicatorPadding = when (bottomIndicatorKinds) {
                0 -> 8.dp
                1 -> 42.dp
                2 -> 76.dp
                else -> 116.dp
            }
            val primaryColor = if (imageUrl != null) Color.White else appColors.onSurface
            val secondaryColor = if (imageUrl != null) Color.White.copy(alpha = 0.88f) else appColors.onMuted
            Box(
                modifier = Modifier.fillMaxSize().padding(if (compactTiles) 12.dp else 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(end = topIndicatorPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.icon != "None") {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                            MdiIcon(
                                config.icon ?: area.icon,
                                modifier = Modifier.padding(if (compactTiles) 6.dp else 8.dp),
                                tint = Color.White,
                                size = if (compactTiles) 16.dp else 18.dp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        config.name ?: area.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RoomStatusIndicators(
                    summary = roomSummary,
                    contentColor = primaryColor,
                    compact = true,
                    visibleRoles = ROOM_CARD_TOP_STATUS_ROLES,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .widthIn(max = 108.dp)
                )

                RoomStatusIndicators(
                    summary = roomSummary,
                    contentColor = primaryColor,
                    compact = true,
                    visibleRoles = ROOM_CARD_BOTTOM_STATUS_ROLES,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .widthIn(max = 108.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(end = bottomIndicatorPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (mediaStatus != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mediaIcon != null) {
                                Icon(
                                    mediaIcon,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                mediaStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    RoomEnvironmentSummary(
                        summary = roomSummary,
                        color = primaryColor,
                        compact = true
                    )
                }
            }

            if (isEditMode) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f))) {
                    EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
                    if (canDelete) {
                        EditRemoveBadge(
                            onClick = onDelete,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseRoomHeaderColor(value: String?): Color? {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() }?.let {
        if (it.startsWith("#")) it else "#$it"
    } ?: return null
    return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrNull()
}

private val ROOM_CARD_TOP_STATUS_ROLES = setOf(
    RoomStatusRoles.DOORS,
    RoomStatusRoles.WINDOWS,
    RoomStatusRoles.LIGHTS,
    RoomStatusRoles.DEVICES
)

private val ROOM_CARD_BOTTOM_STATUS_ROLES = setOf(
    RoomStatusRoles.MOTION,
    RoomStatusRoles.PRESENCE,
    RoomStatusRoles.SMOKE,
    RoomStatusRoles.GAS,
    RoomStatusRoles.FIRE
)

@Composable
fun AddAreaCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    GradientActionButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(10.dp, itemCornerShape()),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add Room")
    }
}

@Composable
fun AddFloorCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    GradientActionButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(10.dp, itemCornerShape()),
    ) {
        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Floor")
    }
}

@Composable
private fun AddRoomDialog(
    floors: List<HAFloor>,
    onDismiss: () -> Unit,
    onCreate: (roomName: String, floorId: String?, newFloorName: String) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var selectedFloorId by remember { mutableStateOf(floors.firstOrNull()?.floor_id) }
    var newFloorName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (floors.isNotEmpty()) {
                    Text("Floor", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        floors.take(3).forEach { floor ->
                            FilterChip(
                                selected = selectedFloorId == floor.floor_id && newFloorName.isBlank(),
                                onClick = {
                                    selectedFloorId = floor.floor_id
                                    newFloorName = ""
                                },
                                label = { Text(floor.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newFloorName,
                    onValueChange = { newFloorName = it },
                    label = { Text("New floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = roomName.isNotBlank(),
                onClick = { onCreate(roomName, selectedFloorId, newFloorName) }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddFloorDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Floor") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Floor name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FloorSettingsDialog(
    floor: HAFloor,
    onDismiss: () -> Unit,
    onSave: (HAFloor) -> Unit
) {
    var name by remember { mutableStateOf(floor.name) }
    var iconName by remember { mutableStateOf(floor.icon ?: "") }
    var showIconPickerFloor by remember { mutableStateOf(false) }
    var columns by remember { mutableStateOf(floor.columns.coerceIn(1, 3)) }
    var cardWidth by remember { mutableStateOf(floor.width) }
    var isSquare by remember { mutableStateOf(floor.isSquare) }
    var cornerRadius by remember { mutableStateOf(floor.cornerRadius) }
    var compactTiles by remember { mutableStateOf(floor.compactTiles) }

    if (showIconPickerFloor) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPickerFloor = false },
            onSelect = { iconName = it; showIconPickerFloor = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { com.example.hki7.ui.components.ModernSettingsDialogTitle("Floor", "Identity, layout, and card style") },
        text = {
            val settingsScroll = rememberScrollState()
            Column(
                modifier = Modifier.heightIn(max = 460.dp).fadingEdges(settingsScroll).verticalScroll(settingsScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.hki7.ui.components.SettingsSubcategory("Identity", "Name and icon shown above this floor")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iconName.isNotEmpty()) MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPickerFloor = true }) { Text(if (iconName.isEmpty()) "Choose" else "Change") }
                    if (iconName.isNotEmpty()) TextButton(onClick = { iconName = "" }) { Text("None") }
                }
                com.example.hki7.ui.components.SettingsSubcategory("Layout", "Grid density, width, and tile shape")
                Text("Columns", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { count ->
                        FilterChip(selected = columns == count, onClick = { columns = count }, label = { Text("$count") })
                    }
                }
                WidgetWidthSelector(width = cardWidth, onWidthChange = { cardWidth = it }, includeThird = false)
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = compactTiles, onCheckedChange = { compactTiles = it })
                    Text("Compact tile height")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        floor.copy(
                            name = name.trim(),
                            icon = iconName.ifEmpty { null },
                            columns = columns,
                            width = cardWidth,
                            isSquare = isSquare,
                            cornerRadius = cornerRadius,
                            compactTiles = compactTiles
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

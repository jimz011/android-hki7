@file:Suppress("UnusedBoxWithConstraintsScope")

package com.example.hki7.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import com.example.hki7.data.HAArea
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.mediaPlayerStatus
import com.example.hki7.ui.components.mediaPlayerStateIcon
import com.example.hki7.data.HAFloor
import com.example.hki7.data.HKIAreaConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.Screen
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.RoomConfigDialog
import com.example.hki7.ui.components.WidgetWidthSelector
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
    val collapsedFloorIds by viewModel.collapsedFloorIds.collectAsState()

    var showAutoInfo by remember { mutableStateOf(false) }
    var showAddRoom by remember { mutableStateOf(false) }
    var showAddFloor by remember { mutableStateOf(false) }
    var editingAreaId by remember { mutableStateOf<String?>(null) }
    var editingFloor by remember { mutableStateOf<HAFloor?>(null) }

    val groupedFloors = remember(areas, floors, configs) {
        buildFloorSections(areas, floors, configs)
    }
    val roomsScrollState = rememberScrollState()

    HKIPage(
        viewModel = viewModel,
        title = "Rooms",
        showPeople = false,
        pageKey = "rooms",
        pageSettingsTitle = "Rooms Settings"
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Widen every floor's room grid on larger screens (fold/tablet).
            val columnBonus = when {
                maxWidth >= 900.dp -> 2
                maxWidth >= 600.dp -> 1
                else -> 0
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(roomsScrollState)
                    .padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = if (isEditMode) 156.dp else 96.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
                                columnBonus = columnBonus,
                                scrollState = roomsScrollState,
                                isCollapsed = section.key in collapsedFloorIds,
                                onToggleCollapsed = { viewModel.toggleFloorCollapsed(section.key) },
                                onDeleteFloor = { section.floor?.let { viewModel.deleteFloor(it.floor_id) } },
                                onSettingsFloor = { section.floor?.let { editingFloor = it } },
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
            text = { Text("Auto mode keeps rooms and floors synced from Home Assistant. Use Settings to take over the current dashboard or start a manual config before adding rooms here.") },
            confirmButton = { Button(onClick = { showAutoInfo = false }) { Text("OK") } }
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
    val knownFloorIds = floors.map { it.floor_id }.toSet()
    val byFloor = areas.groupBy { area -> configs[area.area_id]?.floorId ?: area.floor_id }
    val sections = floors.map { floor -> FloorSectionData(floor.floor_id, floor, byFloor[floor.floor_id].orEmpty()) }
        .filter { it.areas.isNotEmpty() || floors.isNotEmpty() }
    val unassigned = byFloor.filterKeys { it == null || it !in knownFloorIds }.values.flatten()
    return if (unassigned.isNotEmpty()) sections + FloorSectionData("__rooms__", null, unassigned) else sections.ifEmpty {
        listOf(FloorSectionData("__rooms__", null, areas))
    }
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
    columnBonus: Int,
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
            if (isEditMode && dashboardMode != "auto" && floor != null) {
                IconButton(onClick = onDeleteFloor, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete floor", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettingsFloor, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Floor settings", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (!isCollapsed) {
            Spacer(Modifier.height(12.dp))

            // Card columns come from the floor config; widen on larger screens (fold/tablet).
            // Half-width floors skip the widen bonus since the whole section is already narrow.
            val bonus = if (floor?.width == "half") 0 else columnBonus
            val gridColumns = (floor?.columns?.coerceIn(1, 3) ?: 2) + bonus
            val cardHeight = if (floor?.compactTiles == true) 112 else 160
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
                    compactTiles = floor?.compactTiles == true,
                    cornerRadius = floor?.cornerRadius ?: 24,
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
    compactTiles: Boolean = false,
    cornerRadius: Int = 24,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val imageSource = config.wallpaper ?: area.picture
    val imageUrl = imageSource?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
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
                    modifier = Modifier.fillMaxSize().background(appColors.subtleSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (config.icon != "None") {
                        MdiIcon(config.icon ?: area.icon, tint = appColors.onMuted, size = 48.dp)
                    }
                }
            }

            val mediaEntityIds = remember(config.mediaPlayerEntityId) { listOfNotNull(config.mediaPlayerEntityId) }
            val mediaEntityFlow = remember(viewModel, mediaEntityIds) { viewModel.entitiesFor(mediaEntityIds) }
            val mediaEntities by mediaEntityFlow.collectAsState()
            val mediaPlayerEntity = mediaEntities.firstOrNull()
            val mediaStatus = mediaPlayerStatus(mediaPlayerEntity)
            val mediaIcon = mediaPlayerStateIcon(mediaPlayerEntity)
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (config.icon != "None") {
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                                MdiIcon(config.icon ?: area.icon, modifier = Modifier.padding(8.dp), tint = Color.White, size = 18.dp)
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(config.name ?: area.name, style = MaterialTheme.typography.titleMedium, color = if (imageUrl != null) Color.White else appColors.onSurface, fontWeight = FontWeight.Bold)
                    }
                    if (mediaStatus != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mediaIcon != null) {
                                Icon(
                                    mediaIcon,
                                    contentDescription = null,
                                    tint = if (imageUrl != null) Color.White.copy(alpha = 0.85f) else appColors.onMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                mediaStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (imageUrl != null) Color.White.copy(alpha = 0.85f) else appColors.onMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isEditMode) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f))) {
                    IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
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

@Composable
fun AddAreaCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add Room")
    }
}

@Composable
fun AddFloorCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp)
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
        title = { Text("Floor Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cornerRadius == 8, onClick = { cornerRadius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = cornerRadius == 20, onClick = { cornerRadius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = cornerRadius == 28, onClick = { cornerRadius = 28 }, label = { Text("Round") })
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

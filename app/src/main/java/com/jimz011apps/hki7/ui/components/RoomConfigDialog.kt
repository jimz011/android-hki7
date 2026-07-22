@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIAreaConfig
import com.jimz011apps.hki7.data.HKIBadgeBarConfig
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.RoomStatusRoles
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

@Composable
fun RoomConfigDialog(
    areaId: String,
    viewModel: MainViewModel,
    onHeaderColorPreview: (String?) -> Unit = {},
    onBadgeBarPreview: (HKIBadgeBarConfig?) -> Unit = {},
    onDismiss: () -> Unit
) {
    val areaConfigs by viewModel.areaConfigsMapping.collectAsState()
    val floors by viewModel.floors.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val allEntities by viewModel.entities.collectAsState()
    val config = areaConfigs[areaId] ?: HKIAreaConfig()
    val area = areas.find { it.area_id == areaId }

    var name by remember(config) { mutableStateOf(config.name ?: area?.name ?: "") }
    var mediaPlayerEntityIds by remember(config) {
        mutableStateOf(
            normalizeRoomEntityIds(
                config.mediaPlayerEntityIds.ifEmpty { listOfNotNull(config.mediaPlayerEntityId) }
            )
        )
    }
    var showMediaPicker by remember { mutableStateOf(false) }
    var iconName by remember(config) { mutableStateOf(config.icon ?: "Room") }
    var wallpaper by remember(config) { mutableStateOf(config.wallpaper ?: "") }
    var headerColor by remember(config) { mutableStateOf(config.headerColor ?: "") }
    var headerRgb by remember(config) { mutableStateOf(hexToRgb(config.headerColor) ?: listOf(155, 83, 83)) }
    var floorId by remember(config) { mutableStateOf(config.floorId) }
    var badgeBarEnabled by remember(config) { mutableStateOf(config.badgeBar?.visible ?: true) }
    var badgeAlignment by remember(config) { mutableStateOf(config.badgeBar?.alignment ?: "split") }
    var badgeSpanIcons by remember(config) { mutableStateOf(config.badgeBar?.spanIcons ?: false) }
    var badgeLeftOverflow by remember(config) { mutableStateOf(config.badgeBar?.leftOverflow ?: false) }
    var badgeRightOverflow by remember(config) { mutableStateOf(config.badgeBar?.rightOverflow ?: false) }
    var roomStatusEntityIds by remember(config) { mutableStateOf(config.roomStatusEntityIds) }
    var roomTemperatureEntityIds by remember(config) {
        mutableStateOf(
            normalizeRoomEntityIds(
                config.roomTemperatureEntityIds.ifEmpty {
                    listOfNotNull(config.roomTemperatureEntityId)
                }
            )
        )
    }
    var roomHumidityEntityIds by remember(config) {
        mutableStateOf(
            normalizeRoomEntityIds(
                config.roomHumidityEntityIds.ifEmpty {
                    listOfNotNull(config.roomHumidityEntityId)
                }
            )
        )
    }
    var roomEntityPicker by remember { mutableStateOf<String?>(null) }
    var showIconPickerRoom by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("menu") }
    var showReimport by remember { mutableStateOf(false) }
    var showClearRooms by remember { mutableStateOf(false) }

    if (showIconPickerRoom) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPickerRoom = false },
            onSelect = { slug ->
                iconName = slug.ifEmpty { "None" }
                showIconPickerRoom = false
            }
        )
    }

    if (showMediaPicker) {
        val mediaPlayers = allEntities.filter { it.entity_id.startsWith("media_player.") }
        AdvancedEntitySearchDialog(
            allEntities = mediaPlayers,
            title = "Select Media Players",
            singleSelect = false,
            preselectedIds = mediaPlayerEntityIds.toSet(),
            onDismiss = { showMediaPicker = false },
            onEntitiesSelected = { ids ->
                mediaPlayerEntityIds = normalizeRoomEntityIds(ids)
                showMediaPicker = false
            }
        )
    }

    roomEntityPicker?.let { picker ->
        val selectedIds = when (picker) {
            ROOM_TEMPERATURE_PICKER -> roomTemperatureEntityIds.toSet()
            ROOM_HUMIDITY_PICKER -> roomHumidityEntityIds.toSet()
            else -> roomStatusEntityIds[picker].orEmpty().toSet()
        }
        AdvancedEntitySearchDialog(
            allEntities = roomEntityCandidates(picker, allEntities, selectedIds),
            title = "Select ${roomEntityLabel(picker)}",
            singleSelect = false,
            preselectedIds = selectedIds,
            onDismiss = { roomEntityPicker = null },
            onEntitiesSelected = { ids ->
                when (picker) {
                    ROOM_TEMPERATURE_PICKER -> roomTemperatureEntityIds = normalizeRoomEntityIds(ids)
                    ROOM_HUMIDITY_PICKER -> roomHumidityEntityIds = normalizeRoomEntityIds(ids)
                    else -> roomStatusEntityIds = roomStatusEntityIds.toMutableMap().apply {
                        if (ids.isEmpty()) remove(picker) else put(picker, ids.distinct())
                    }
                }
                roomEntityPicker = null
            }
        )
    }

    val dismissSettings = {
        onHeaderColorPreview(null)
        onBadgeBarPreview(null)
        onDismiss()
    }
    ModernSettingsDialogFrame(
        title = if (section == "menu") "Room configuration" else section.replaceFirstChar { it.uppercase() },
        subtitle = if (section == "menu") "Choose one room area to configure" else "Focused options for this room area",
        onDismiss = dismissSettings,
        onBack = if (section == "menu") null else {{ section = "menu" }},
        content = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize().fadingEdges(scrollState).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (section == "menu") {
                    SettingsSubcategory("Room areas", "Identity, presentation, indicators, and maintenance")
                    RoomSettingsChoice(Icons.Default.Tune, "General", "Name and icon") { section = "general" }
                    RoomSettingsChoice(Icons.Default.Image, "Header", "Wallpaper and custom color") { section = "header" }
                    RoomSettingsChoice(Icons.Default.ViewStream, "Badge Bar", "Alignment and display options") { section = "badgebar" }
                    RoomSettingsChoice(Icons.Default.Home, "Floor", "Assign this room to a floor") { section = "floor" }
                    RoomSettingsChoice(Icons.Default.Sensors, "Room status", "Media, activity, safety and climate indicators") { section = "room status" }
                    RoomSettingsChoice(Icons.Default.CloudDownload, "Re-import from Home Assistant", "Import new rooms or rebuild every room") { showReimport = true }
                    RoomSettingsChoice(Icons.Default.DeleteSweep, "Clear Rooms View", "Remove imported rooms and floors") {
                        showClearRooms = true
                    }
                }

                if (section == "general") {
                    val appColors = LocalHKIAppColors.current
                    SettingsSubcategory("Identity", "Name and icon used throughout the app")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Room name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            iconName.takeUnless { it == "None" } ?: "None",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        TextButton(onClick = { showIconPickerRoom = true }) { Text("Change") }
                    }
                }

                if (section == "header") {
                    SettingsSubcategory("Header appearance", "Wallpaper and optional custom color")
                    OutlinedTextField(
                        value = wallpaper,
                        onValueChange = { wallpaper = it },
                        label = { Text("Wallpaper URL or path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerColor,
                        onValueChange = {
                            headerColor = it
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
                            headerColor = rgbToHex(rgb)
                            onHeaderColorPreview(headerColor)
                        },
                        onValueChangeFinished = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(220.dp)
                    )
                }

                if (section == "floor" && floors.isNotEmpty()) {
                    SettingsSubcategory("Floor assignment", "Place this room in the correct floor group")
                    Text("Floor", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SettingsChoiceChip(
                            selected = floorId == null,
                            onClick = { floorId = null },
                            label = { Text("None") }
                        )
                        floors.take(3).forEach { floor ->
                            SettingsChoiceChip(
                                selected = floorId == floor.floor_id,
                                onClick = { floorId = floor.floor_id },
                                label = { Text(floor.name) }
                            )
                        }
                    }
                }

                if (section == "room status") {
                    val appColors = LocalHKIAppColors.current
                    SettingsSubcategory("Media", "Players summarized by the room header and card")
                    Text("Media players", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    Text(
                        "The room header and card show a single player's media, or an active-player count when multiple players are selected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            selectedEntitySummary(mediaPlayerEntityIds, allEntities),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        if (mediaPlayerEntityIds.isNotEmpty()) {
                            TextButton(onClick = { mediaPlayerEntityIds = emptyList() }) { Text("Clear") }
                        }
                        TextButton(onClick = { showMediaPicker = true }) { Text("Change") }
                    }
                    HorizontalDivider()
                    SettingsSubcategory("Live indicators", "Entities that signal activity or safety states")
                    Text(
                        "Choose the Home Assistant entities that drive this room's live indicators. Indicators only appear while they are active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    RoomStatusRoles.ORDERED.forEach { role ->
                        val selectedIds = roomStatusEntityIds[role].orEmpty()
                        RoomEntitySelectionRow(
                            label = roomEntityLabel(role),
                            selection = selectedEntitySummary(selectedIds, allEntities),
                            hasSelection = selectedIds.isNotEmpty(),
                            onClear = {
                                roomStatusEntityIds = roomStatusEntityIds.toMutableMap().apply { remove(role) }
                            },
                            onChange = { roomEntityPicker = role }
                        )
                    }

                    HorizontalDivider()
                    SettingsSubcategory("Climate summary", "Temperature and humidity sources for the room")
                    Text("Room climate", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    Text(
                        "Climate sources take priority and use their current_temperature and current_humidity values. " +
                            "Multiple climate values are averaged. Separate sensor values are averaged only when no climate source is selected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    RoomEntitySelectionRow(
                        label = "Temperature",
                        selection = selectedEntitySummary(
                            roomTemperatureEntityIds,
                            allEntities
                        ),
                        hasSelection = roomTemperatureEntityIds.isNotEmpty(),
                        onClear = { roomTemperatureEntityIds = emptyList() },
                        onChange = { roomEntityPicker = ROOM_TEMPERATURE_PICKER }
                    )
                    RoomEntitySelectionRow(
                        label = "Humidity",
                        selection = selectedEntitySummary(
                            roomHumidityEntityIds,
                            allEntities
                        ),
                        hasSelection = roomHumidityEntityIds.isNotEmpty(),
                        onClear = { roomHumidityEntityIds = emptyList() },
                        onChange = { roomEntityPicker = ROOM_HUMIDITY_PICKER }
                    )
                }

                if (section == "badgebar") {
                    SettingsSubcategory("Badge bar layout", "Visibility, alignment, and overflow behavior")
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
                            SettingsChoiceChip(
                                selected = badgeAlignment == value,
                                onClick  = {
                                    badgeAlignment = value
                                    onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = badgeBarEnabled, alignment = value, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                                },
                                label    = { Text(label) }
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
        footer = {
            TextButton(onClick = dismissSettings) { Text("Cancel") }
            Button(onClick = {
                val normalizedMediaPlayerEntityIds = normalizeRoomEntityIds(mediaPlayerEntityIds)
                val configuredMediaPlayerEntityIds = normalizeRoomEntityIds(
                    config.mediaPlayerEntityIds.ifEmpty { listOfNotNull(config.mediaPlayerEntityId) }
                )
                val mediaPlayersChanged = normalizedMediaPlayerEntityIds != configuredMediaPlayerEntityIds
                val normalizedRoomStatusEntityIds = normalizeRoomStatusEntityIds(roomStatusEntityIds)
                val normalizedRoomTemperatureEntityIds = normalizeRoomEntityIds(roomTemperatureEntityIds)
                val normalizedRoomHumidityEntityIds = normalizeRoomEntityIds(roomHumidityEntityIds)
                val configuredRoomTemperatureEntityIds = normalizeRoomEntityIds(
                    config.roomTemperatureEntityIds.ifEmpty { listOfNotNull(config.roomTemperatureEntityId) }
                )
                val configuredRoomHumidityEntityIds = normalizeRoomEntityIds(
                    config.roomHumidityEntityIds.ifEmpty { listOfNotNull(config.roomHumidityEntityId) }
                )
                val roomEntitiesChanged =
                    normalizedRoomStatusEntityIds != normalizeRoomStatusEntityIds(config.roomStatusEntityIds) ||
                        normalizedRoomTemperatureEntityIds != configuredRoomTemperatureEntityIds ||
                        normalizedRoomHumidityEntityIds != configuredRoomHumidityEntityIds
                viewModel.updateAreaConfig(
                    areaId,
                    config.copy(
                        name        = name.trim().ifBlank { null }?.takeUnless { it == area?.name },
                        mediaPlayerEntityIds = normalizedMediaPlayerEntityIds,
                        mediaPlayerEntityId = null,
                        mediaPlayersCustomized = if (mediaPlayersChanged) true else config.mediaPlayersCustomized,
                        icon        = iconName,
                        wallpaper   = wallpaper.ifBlank { null },
                        headerColor = headerColor.ifBlank { null },
                        floorId     = floorId,
                        badgeBar    = (config.badgeBar ?: HKIBadgeBarConfig()).copy(
                            visible    = badgeBarEnabled,
                            alignment  = badgeAlignment,
                            spanIcons  = badgeSpanIcons,
                            leftOverflow = badgeLeftOverflow,
                            rightOverflow = badgeRightOverflow
                        ),
                        roomStatusEntityIds = normalizedRoomStatusEntityIds,
                        roomTemperatureEntityIds = normalizedRoomTemperatureEntityIds,
                        roomHumidityEntityIds = normalizedRoomHumidityEntityIds,
                        roomTemperatureEntityId = null,
                        roomHumidityEntityId = null,
                        roomEntitiesCustomized = if (roomEntitiesChanged) true else config.roomEntitiesCustomized
                    )
                )
                onHeaderColorPreview(null)
                onBadgeBarPreview(null)
                onDismiss()
            }) { Text("Save") }
        }
    )

    if (showReimport) {
        AlertDialog(
            onDismissRequest = { showReimport = false },
            title = { Text("Re-import rooms") },
            text = { Text("Import only rooms that have not been edited, or rebuild everything. Rebuilding removes all edited rooms and floors before importing them from scratch.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = { viewModel.reimportRooms(fromScratch = false); showReimport = false; onDismiss() }) { Text("Import unedited") }
                    TextButton(onClick = { viewModel.reimportRooms(fromScratch = true); showReimport = false; onDismiss() }) {
                        Text("Remove edits and import all", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showReimport = false }) { Text("Cancel") } }
        )
    }
    if (showClearRooms) {
        AlertDialog(
            onDismissRequest = { showClearRooms = false },
            title = { Text("Clear rooms view?") },
            text = { Text("This removes all imported rooms and floors from this view.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRoomImports(); showClearRooms = false; onDismiss() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearRooms = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RoomSettingsChoice(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ModernSettingsMenuItem(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
}

@Composable
private fun RoomEntitySelectionRow(
    label: String,
    selection: String,
    hasSelection: Boolean,
    onClear: () -> Unit,
    onChange: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                selection,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (hasSelection) appColors.onSurface else appColors.onMuted
            )
            if (hasSelection) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
            TextButton(onClick = onChange) { Text("Change") }
        }
    }
}

private const val ROOM_TEMPERATURE_PICKER = "__room_temperature__"
private const val ROOM_HUMIDITY_PICKER = "__room_humidity__"

private fun roomEntityLabel(role: String): String = when (role) {
    RoomStatusRoles.DOORS -> "Doors"
    RoomStatusRoles.WINDOWS -> "Windows"
    RoomStatusRoles.MOTION -> "Motion"
    RoomStatusRoles.PRESENCE -> "Presence"
    RoomStatusRoles.LIGHTS -> "Lights"
    RoomStatusRoles.DEVICES -> "Devices"
    RoomStatusRoles.SMOKE -> "Smoke"
    RoomStatusRoles.GAS -> "Gas"
    RoomStatusRoles.FIRE -> "Fire"
    ROOM_TEMPERATURE_PICKER -> "Temperature"
    ROOM_HUMIDITY_PICKER -> "Humidity"
    else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun roomEntityCandidates(
    picker: String,
    allEntities: List<HAEntity>,
    selectedIds: Set<String>
): List<HAEntity> = allEntities.filter { entity ->
    if (entity.entity_id in selectedIds) return@filter true
    val domain = entity.entity_id.substringBefore('.')
    val deviceClass = entity.deviceClass?.lowercase()
    when (picker) {
        RoomStatusRoles.DOORS ->
            (domain == "binary_sensor" && deviceClass in setOf("door", "garage_door")) ||
                (domain == "cover" && deviceClass in setOf("door", "garage", "garage_door", "gate"))

        RoomStatusRoles.WINDOWS ->
            (domain == "binary_sensor" || domain == "cover") && deviceClass == "window"

        RoomStatusRoles.MOTION ->
            domain == "binary_sensor" && deviceClass in setOf("motion", "moving", "vibration")

        RoomStatusRoles.PRESENCE ->
            (domain == "binary_sensor" && deviceClass in setOf("occupancy", "presence")) ||
                domain in setOf("person", "device_tracker")

        RoomStatusRoles.LIGHTS -> domain == "light"
        RoomStatusRoles.DEVICES -> domain in setOf("switch", "fan", "humidifier", "input_boolean")
        RoomStatusRoles.SMOKE -> domain == "binary_sensor" && deviceClass == "smoke"
        RoomStatusRoles.GAS ->
            domain == "binary_sensor" && deviceClass in setOf("gas", "carbon_monoxide")

        RoomStatusRoles.FIRE ->
            domain == "binary_sensor" && deviceClass in setOf("fire", "heat", "safety")

        ROOM_TEMPERATURE_PICKER ->
            domain == "climate" || (domain == "sensor" && deviceClass == "temperature")

        ROOM_HUMIDITY_PICKER ->
            domain == "climate" || (domain == "sensor" && deviceClass == "humidity")

        else -> false
    }
}

private fun selectedEntitySummary(
    selectedIds: List<String>,
    allEntities: List<HAEntity>,
    includeCount: Boolean = true
): String {
    if (selectedIds.isEmpty()) return "None"
    val names = selectedIds.map { id ->
        allEntities.firstOrNull { it.entity_id == id }?.friendlyName ?: id
    }
    if (!includeCount) return names.first()
    return when (names.size) {
        1 -> "1 selected · ${names.first()}"
        2 -> "2 selected · ${names.joinToString()}"
        else -> "${names.size} selected · ${names.first()} +${names.size - 1}"
    }
}

private fun normalizeRoomStatusEntityIds(entityIds: Map<String, List<String>>): Map<String, List<String>> =
    entityIds.mapValues { (_, ids) -> ids.filter { it.isNotBlank() }.distinct() }
        .filterValues { it.isNotEmpty() }

private fun normalizeRoomEntityIds(entityIds: List<String>): List<String> =
    entityIds.filter { it.isNotBlank() }.distinct()

/** Shared Full/Half/Third size selector used by widget and room-card settings so they stay
 *  consistent. Room cards don't support thirds (their row packing is full/half only). */
@Composable
fun WidgetWidthSelector(width: String, onWidthChange: (String) -> Unit, includeThird: Boolean = true) {
    Text("Widget width", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = buildList {
            add("full" to "Full")
            add("half" to "Half")
            if (includeThird) add("third" to "Third")
        }
        options.forEach { (value, label) ->
            SettingsChoiceChip(
                selected = width == value,
                onClick = { onWidthChange(value) },
                label = { Text(label) }
            )
        }
    }
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

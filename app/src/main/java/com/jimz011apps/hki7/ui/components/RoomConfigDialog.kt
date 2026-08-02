@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

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
    // Aesthetics-only recipients (Family Sharing) can't rebuild structure, so re-import and clear are hidden.
    val aestheticsOnly by viewModel.aestheticsOnlyEditing.collectAsState()
    val allowReimport by viewModel.allowReimport.collectAsState()
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
            title = stringResource(R.string.dlg_select_media_players),
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
            title = stringResource(R.string.dlg_select, roomEntityLabel(picker)),
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
        title = if (section == "menu") {
            stringResource(R.string.dlg_room_configuration)
        } else {
            roomSectionLabel(section)
        },
        subtitle = if (section == "menu") {
            stringResource(R.string.dlg_choose_room_area_to_configure)
        } else {
            stringResource(R.string.dlg_focused_room_options)
        },
        onDismiss = dismissSettings,
        onBack = if (section == "menu") null else {{ section = "menu" }},
        content = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize().fadingEdges(scrollState).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (section == "menu") {
                    SettingsSubcategory(stringResource(R.string.dlg_room_areas), stringResource(R.string.dlg_identity_presentation_indicators_and_maintenance))
                    RoomSettingsChoice(Icons.Default.Tune, stringResource(R.string.dlg_general), stringResource(R.string.dlg_name_and_icon)) { section = "general" }
                    RoomSettingsChoice(Icons.Default.Image, stringResource(R.string.dlg_header), stringResource(R.string.dlg_wallpaper_and_custom_color)) { section = "header" }
                    RoomSettingsChoice(Icons.Default.ViewStream, stringResource(R.string.dlg_badge_bar), stringResource(R.string.dlg_alignment_and_display_options)) { section = "badgebar" }
                    RoomSettingsChoice(Icons.Default.Home, stringResource(R.string.dlg_floor), stringResource(R.string.dlg_assign_room_to_floor)) { section = "floor" }
                    RoomSettingsChoice(Icons.Default.Sensors, stringResource(R.string.dlg_room_status), stringResource(R.string.dlg_room_status_summary)) { section = "room status" }
                    if (!aestheticsOnly && allowReimport) {
                        RoomSettingsChoice(Icons.Default.CloudDownload, stringResource(R.string.dlg_reimport_from_home_assistant), stringResource(R.string.dlg_import_or_rebuild_rooms)) { showReimport = true }
                        RoomSettingsChoice(Icons.Default.DeleteSweep, stringResource(R.string.dlg_clear_rooms_view_action), stringResource(R.string.dlg_remove_imported_rooms_and_floors)) {
                            showClearRooms = true
                        }
                    }
                }

                if (section == "general") {
                    val appColors = LocalHKIAppColors.current
                    SettingsSubcategory(stringResource(R.string.dlg_identity), stringResource(R.string.dlg_name_and_icon_used_throughout_the_app))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.dlg_room_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.dlg_icon), style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconName != "None") {
                            MdiIcon(iconName, size = 24.dp)
                        }
                        Text(
                            when (iconName) {
                                "None" -> stringResource(R.string.dlg_none)
                                "Room" -> stringResource(R.string.dlg_room)
                                else -> iconName
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        TextButton(onClick = { showIconPickerRoom = true }) { Text(stringResource(R.string.dlg_change)) }
                    }
                }

                if (section == "header") {
                    SettingsSubcategory(stringResource(R.string.dlg_header_appearance), stringResource(R.string.dlg_wallpaper_and_optional_custom_color))
                    OutlinedTextField(
                        value = wallpaper,
                        onValueChange = { wallpaper = it },
                        label = { Text(stringResource(R.string.dlg_wallpaper_url_or_path)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerColor,
                        onValueChange = {
                            headerColor = it
                            onHeaderColorPreview(it.ifBlank { null })
                        },
                        label = { Text(stringResource(R.string.dlg_header_custom_color_rrggbb)) },
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
                    SettingsSubcategory(stringResource(R.string.dlg_floor_assignment), stringResource(R.string.dlg_place_this_room_in_the_correct_floor_group))
                    Text(stringResource(R.string.dlg_floor), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SettingsChoiceChip(
                            selected = floorId == null,
                            onClick = { floorId = null },
                            label = { Text(stringResource(R.string.dlg_none)) }
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
                    SettingsSubcategory(stringResource(R.string.dlg_media), stringResource(R.string.dlg_players_summarized_by_the_room_header_and_card))
                    Text(stringResource(R.string.dlg_media_players), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    Text(
                        stringResource(R.string.dlg_the_room_header_and_card_show_a_single_player),
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
                            TextButton(onClick = { mediaPlayerEntityIds = emptyList() }) { Text(stringResource(R.string.dlg_clear)) }
                        }
                        TextButton(onClick = { showMediaPicker = true }) { Text(stringResource(R.string.dlg_change)) }
                    }
                    HorizontalDivider()
                    SettingsSubcategory(stringResource(R.string.dlg_live_indicators), stringResource(R.string.dlg_entities_that_signal_activity_or_safety_states))
                    Text(
                        stringResource(R.string.dlg_choose_the_home_assistant_entities_that_drive_this_room),
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
                    SettingsSubcategory(stringResource(R.string.dlg_climate_summary), stringResource(R.string.dlg_temperature_and_humidity_sources_for_the_room))
                    Text(stringResource(R.string.dlg_room_climate), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    Text(
                        stringResource(R.string.dlg_climate_sources_take_priority_and_use_their_current_temper) +
                            stringResource(R.string.dlg_multiple_climate_values_are_averaged_separate_sensor_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    RoomEntitySelectionRow(
                        label = stringResource(R.string.dlg_temperature),
                        selection = selectedEntitySummary(
                            roomTemperatureEntityIds,
                            allEntities
                        ),
                        hasSelection = roomTemperatureEntityIds.isNotEmpty(),
                        onClear = { roomTemperatureEntityIds = emptyList() },
                        onChange = { roomEntityPicker = ROOM_TEMPERATURE_PICKER }
                    )
                    RoomEntitySelectionRow(
                        label = stringResource(R.string.dlg_humidity),
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
                    SettingsSubcategory(stringResource(R.string.dlg_badge_bar_layout), stringResource(R.string.dlg_visibility_alignment_and_overflow_behavior))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.dlg_show_badge_bar), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = badgeBarEnabled,
                            onCheckedChange = {
                                badgeBarEnabled = it
                                onBadgeBarPreview((config.badgeBar ?: HKIBadgeBarConfig()).copy(visible = it, alignment = badgeAlignment, spanIcons = badgeSpanIcons, leftOverflow = badgeLeftOverflow, rightOverflow = badgeRightOverflow))
                            }
                        )
                    }
                    Text(stringResource(R.string.dlg_alignment), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "split" to stringResource(R.string.dlg_split),
                            "left" to stringResource(R.string.dlg_left),
                            "center" to stringResource(R.string.dlg_center),
                            "right" to stringResource(R.string.dlg_right),
                        ).forEach { (value, label) ->
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
                            Text(stringResource(R.string.dlg_span_badges), style = MaterialTheme.typography.bodyMedium)
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
                            Text(stringResource(R.string.dlg_left_side_overflows_right), style = MaterialTheme.typography.bodyMedium)
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
                            Text(stringResource(R.string.dlg_right_side_overflows_left), style = MaterialTheme.typography.bodyMedium)
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
            TextButton(onClick = dismissSettings) { Text(stringResource(R.string.dlg_cancel)) }
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
            }) { Text(stringResource(R.string.dlg_save)) }
        }
    )

    if (showReimport) {
        AlertDialog(
            onDismissRequest = { showReimport = false },
            title = { Text(stringResource(R.string.dlg_re_import_rooms)) },
            text = { Text(stringResource(R.string.dlg_import_only_rooms_that_have_not_been_edited_or)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Button(onClick = { viewModel.reimportRooms(fromScratch = false); showReimport = false; onDismiss() }) { Text(stringResource(R.string.dlg_import_unedited)) }
                    TextButton(onClick = { viewModel.reimportRooms(fromScratch = true); showReimport = false; onDismiss() }) {
                        Text(stringResource(R.string.dlg_remove_edits_and_import_all), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showReimport = false }) { Text(stringResource(R.string.dlg_cancel)) } }
        )
    }
    if (showClearRooms) {
        AlertDialog(
            onDismissRequest = { showClearRooms = false },
            title = { Text(stringResource(R.string.dlg_clear_rooms_view)) },
            text = { Text(stringResource(R.string.dlg_this_removes_all_imported_rooms_and_floors_from_this)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRoomImports(); showClearRooms = false; onDismiss() }) {
                    Text(stringResource(R.string.dlg_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearRooms = false }) { Text(stringResource(R.string.dlg_cancel)) } }
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
                TextButton(onClick = onClear) { Text(stringResource(R.string.dlg_clear)) }
            }
            TextButton(onClick = onChange) { Text(stringResource(R.string.dlg_change)) }
        }
    }
}

private const val ROOM_TEMPERATURE_PICKER = "__room_temperature__"
private const val ROOM_HUMIDITY_PICKER = "__room_humidity__"

@Composable
private fun roomEntityLabel(role: String): String = when (role) {
    RoomStatusRoles.DOORS -> stringResource(R.string.dlg_doors)
    RoomStatusRoles.WINDOWS -> stringResource(R.string.dlg_windows)
    RoomStatusRoles.MOTION -> stringResource(R.string.dlg_motion)
    RoomStatusRoles.PRESENCE -> stringResource(R.string.dlg_presence)
    RoomStatusRoles.LIGHTS -> stringResource(R.string.dlg_lights)
    RoomStatusRoles.DEVICES -> stringResource(R.string.dlg_devices)
    RoomStatusRoles.SMOKE -> stringResource(R.string.dlg_smoke)
    RoomStatusRoles.GAS -> stringResource(R.string.dlg_gas)
    RoomStatusRoles.FIRE -> stringResource(R.string.dlg_fire)
    ROOM_TEMPERATURE_PICKER -> stringResource(R.string.dlg_temperature)
    ROOM_HUMIDITY_PICKER -> stringResource(R.string.dlg_humidity)
    else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun roomSectionLabel(section: String): String = when (section) {
    "general" -> stringResource(R.string.dlg_general)
    "header" -> stringResource(R.string.dlg_header)
    "badgebar" -> stringResource(R.string.dlg_badge_bar)
    "floor" -> stringResource(R.string.dlg_floor)
    "room status" -> stringResource(R.string.dlg_room_status)
    else -> section.replaceFirstChar { it.uppercase() }
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

@Composable
private fun selectedEntitySummary(
    selectedIds: List<String>,
    allEntities: List<HAEntity>,
    includeCount: Boolean = true
): String {
    if (selectedIds.isEmpty()) return stringResource(R.string.dlg_none)
    val names = selectedIds.map { id ->
        allEntities.firstOrNull { it.entity_id == id }?.friendlyName ?: id
    }
    if (!includeCount) return names.first()
    val details = if (names.size <= 2) names.joinToString() else "${names.first()} +${names.size - 1}"
    return pluralStringResource(
        R.plurals.dlg_selected_entities_summary,
        names.size,
        names.size,
        details,
    )
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
    Text(stringResource(R.string.dlg_widget_width), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
            "full" to stringResource(R.string.dlg_full),
            "half" to stringResource(R.string.dlg_half),
        ) + if (includeThird) {
            listOf("third" to stringResource(R.string.dlg_third))
        } else {
            emptyList()
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

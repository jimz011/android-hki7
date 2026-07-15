package com.example.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HKIAreaConfig
import com.example.hki7.data.HKIBadgeBarConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.theme.LocalHKIAppColors

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
    var mediaPlayerEntityId by remember(config) { mutableStateOf(config.mediaPlayerEntityId ?: "") }
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
    var showIconPickerRoom by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("menu") }

    if (showIconPickerRoom) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPickerRoom = false },
            onSelect = { slug ->
                iconName = if (slug.isEmpty()) "None" else slug
                showIconPickerRoom = false
            }
        )
    }

    if (showMediaPicker) {
        val mediaPlayers = allEntities.filter { it.entity_id.startsWith("media_player.") }
        AdvancedEntitySearchDialog(
            allEntities = mediaPlayers,
            title = "Select Media Player",
            singleSelect = true,
            preselectedIds = setOfNotNull(mediaPlayerEntityId.takeIf { it.isNotBlank() }),
            onDismiss = { showMediaPicker = false },
            onEntitiesSelected = { ids -> mediaPlayerEntityId = ids.firstOrNull() ?: ""; showMediaPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = {
            onHeaderColorPreview(null)
            onBadgeBarPreview(null)
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false),
        title = {
            androidx.activity.compose.BackHandler {
                if (section == "menu") {
                    onHeaderColorPreview(null)
                    onBadgeBarPreview(null)
                    onDismiss()
                } else section = "menu"
            }
            Text(if (section == "menu") "Room Configuration" else section.replaceFirstChar { it.uppercase() })
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.heightIn(max = 560.dp).fadingEdges(scrollState).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (section == "menu") {
                    RoomSettingsChoice(Icons.Default.Tune, "General", "Name, icon and media player") { section = "general" }
                    RoomSettingsChoice(Icons.Default.Image, "Header", "Wallpaper and custom color") { section = "header" }
                    RoomSettingsChoice(Icons.Default.Home, "Floor", "Assign this room to a floor") { section = "floor" }
                    RoomSettingsChoice(Icons.Default.ViewStream, "Badge Bar", "Alignment and display options") { section = "badgebar" }
                } else {
                    TextButton(onClick = { section = "menu" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                }

                if (section == "general") {
                    val appColors = LocalHKIAppColors.current
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
                    Text("Media player", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "When set, the room header and card show what's playing instead of \"Room Details\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val selectedMedia = allEntities.find { it.entity_id == mediaPlayerEntityId }
                        Text(
                            selectedMedia?.friendlyName ?: mediaPlayerEntityId.ifBlank { "None" },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.onSurface
                        )
                        if (mediaPlayerEntityId.isNotBlank()) {
                            TextButton(onClick = { mediaPlayerEntityId = "" }) { Text("Clear") }
                        }
                        TextButton(onClick = { showMediaPicker = true }) { Text("Change") }
                    }
                }

                if (section == "header") {
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
                    Text("Floor", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = floorId == null,
                            onClick = { floorId = null },
                            label = { Text("None") }
                        )
                        floors.take(3).forEach { floor ->
                            FilterChip(
                                selected = floorId == floor.floor_id,
                                onClick = { floorId = floor.floor_id },
                                label = { Text(floor.name) }
                            )
                        }
                    }
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
        confirmButton = {
            Button(onClick = {
                viewModel.updateAreaConfig(
                    areaId,
                    config.copy(
                        name        = name.trim().ifBlank { null }?.takeUnless { it == area?.name },
                        mediaPlayerEntityId = mediaPlayerEntityId.ifBlank { null },
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
                        )
                    )
                )
                onHeaderColorPreview(null)
                onBadgeBarPreview(null)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                onHeaderColorPreview(null)
                onBadgeBarPreview(null)
                onDismiss()
            }) { Text("Cancel") }
        }
    )
}

@Composable
private fun RoomSettingsChoice(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
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
            FilterChip(
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

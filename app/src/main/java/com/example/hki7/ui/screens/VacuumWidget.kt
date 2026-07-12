@file:Suppress("UnusedBoxWithConstraintsScope")

package com.example.hki7.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.*
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import kotlin.time.Duration.Companion.seconds

// ─────────────────────────────────────────────────────────────────────────────
// Vacuum stack widget content (rendered inside ButtonStackItem)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VacuumStackContent(
    stack: HKIButtonStack,
    entities: List<HAEntity>,
    allEntities: List<HAEntity>,
    currentUrl: String,
    isEditMode: Boolean,
    onEntityClick: (String) -> Unit,
    onButtonSettings: (String) -> Unit,
    onRemoveEntity: (String) -> Unit,
    onReorderEntities: (Int, Int) -> Unit
) {
    if (!isEditMode && entities.isEmpty()) {
        EmptyStackHint()
        return
    }
    val columns = stack.columns.coerceIn(1, 3)

    if (isEditMode) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entities.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { entity ->
                        Box(modifier = Modifier.weight(1f)) {
                            VacuumEntityCard(
                                entity = entity,
                                config = stack.buttonConfigs[entity.entity_id],
                                allEntities = allEntities,
                                currentUrl = currentUrl,
                                isSquare = stack.isSquare,
                                cornerRadius = stack.cornerRadius,
                                aspectRatio = stack.cameraAspectRatio,
                                onClick = {}
                            )
                            IconButton(onClick = { onButtonSettings(entity.entity_id) },
                                modifier = Modifier.align(Alignment.Center).size(24.dp)) {
                                Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            EditRemoveBadge(
                                onClick = { onRemoveEntity(entity.entity_id) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                            )
                        }
                    }
                    repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entities.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { entity ->
                        VacuumEntityCard(
                            entity = entity,
                            config = stack.buttonConfigs[entity.entity_id],
                            allEntities = allEntities,
                            currentUrl = currentUrl,
                            isSquare = stack.isSquare,
                            cornerRadius = stack.cornerRadius,
                            aspectRatio = stack.cameraAspectRatio,
                            onClick = { onEntityClick(entity.entity_id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single vacuum entity card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VacuumEntityCard(
    entity: HAEntity,
    config: HKIButtonConfig?,
    allEntities: List<HAEntity>,
    currentUrl: String,
    isSquare: Boolean,
    cornerRadius: Int,
    aspectRatio: Float = 1f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMode = config?.vacuumDisplayMode ?: "static"
    val mapCameraEntity = config?.vacuumMapEntityId?.let { id -> allEntities.find { it.entity_id == id } }
    // Full URL or a path on the HA server (e.g. /local/vacuum.png), like the header wallpaper.
    val externalUrl = remember(config?.vacuumImageUrl, currentUrl) {
        resolveCameraUrl(config?.vacuumImageUrl, currentUrl)
    }

    // Map camera image URL
    val mapImageUrl = remember(mapCameraEntity, currentUrl) {
        mapCameraEntity?.let { resolveEntityCameraUrl(it, currentUrl, preferLive = false) }
    }

    val batteryLevel = run {
        val battId = config?.vacuumBatteryEntityId
        if (!battId.isNullOrBlank()) allEntities.find { it.entity_id == battId }?.state?.toIntOrNull() ?: 0
        else entity.attributes?.get("battery_level")?.jsonPrimitive?.intOrNull ?: 0
    }
    val displayName = config?.name ?: entity.friendlyName ?: entity.entity_id
    val stateTxt = entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val stateColor = when (entity.state) {
        "cleaning"  -> Color(0xFF66BB6A)
        "docked"    -> Color(0xFF42A5F5)
        "paused"    -> Color(0xFFFFB300)
        "error"     -> Color(0xFFEF5350)
        "returning" -> Color(0xFF42A5F5)
        else        -> Color.White.copy(alpha = 0.7f)
    }

    val sizeModifier = if (isSquare) Modifier.aspectRatio(1f) else Modifier.aspectRatio(aspectRatio)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(sizeModifier)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Box {
            // Background image
            when {
                displayMode == "camera" && mapImageUrl != null ->
                    AsyncImage(model = mapImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                displayMode == "external" && !externalUrl.isNullOrBlank() ->
                    AsyncImage(model = externalUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else ->
                    StaticVacuumGraphic(modifier = Modifier.fillMaxSize(), state = entity.state)
            }

            // Gradient overlay: dark at bottom (like camera stack)
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)))
            ))

            // Name + state + battery at BOTTOM-LEFT (like camera stack)
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(displayName, color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).background(stateColor, CircleShape))
                        Text(stateTxt, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f, fill = false))
                        Icon(Icons.Default.BatteryFull, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                        Text("$batteryLevel%", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// Top-down robot vacuum drawn with Canvas for the "static" (Robot image) display mode:
// round body with lid seam, lidar turret, front bumper with notches, and dustbin latch.
@Composable
private fun StaticVacuumGraphic(modifier: Modifier = Modifier, state: String) {
    val primary = MaterialTheme.colorScheme.primary
    val isActive = state == "cleaning"

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = center.x; val cy = center.y
        val r = minOf(size.width, size.height) * 0.36f
        val bodyColor = if (isActive) lerp(Color(0xFF52535A), primary, 0.30f) else Color(0xFF52535A)
        val turretColor = if (isActive) lerp(Color(0xFF5D5E66), primary, 0.25f) else Color(0xFF5D5E66)
        val outline = Color(0xFF2A2B30)
        val seamStroke = Stroke(r * 0.035f)

        // Glow while cleaning
        if (isActive) drawCircle(primary.copy(alpha = 0.16f), r * 1.3f, center = center)
        // Body with outline
        drawCircle(bodyColor, r, center = center, style = Fill)
        drawCircle(outline, r, center = center, style = Stroke(r * 0.06f))
        // Lid seam following the top rim
        drawArc(
            color = outline.copy(alpha = 0.75f),
            startAngle = 208f, sweepAngle = 124f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r * 0.82f, cy - r * 0.82f),
            size = androidx.compose.ui.geometry.Size(r * 1.64f, r * 1.64f),
            style = seamStroke
        )
        // Front bumper seam along the bottom rim…
        drawArc(
            color = outline,
            startAngle = 22f, sweepAngle = 136f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r * 0.88f, cy - r * 0.88f),
            size = androidx.compose.ui.geometry.Size(r * 1.76f, r * 1.76f),
            style = Stroke(r * 0.05f)
        )
        // …with a notch where the bumper meets the body on each side
        listOf(22f, 158f).forEach { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val cos = kotlin.math.cos(rad).toFloat(); val sin = kotlin.math.sin(rad).toFloat()
            drawLine(
                color = outline,
                start = androidx.compose.ui.geometry.Offset(cx + r * 0.88f * cos, cy + r * 0.88f * sin),
                end = androidx.compose.ui.geometry.Offset(cx + r * cos, cy + r * sin),
                strokeWidth = r * 0.05f
            )
        }
        // Lidar turret
        val turretCenter = androidx.compose.ui.geometry.Offset(cx, cy - r * 0.45f)
        drawCircle(turretColor, r * 0.27f, center = turretCenter, style = Fill)
        drawCircle(outline, r * 0.27f, center = turretCenter, style = Stroke(r * 0.05f))
        // Dustbin latch at the front
        val latchW = r * 0.18f; val latchH = r * 0.34f
        drawRoundRect(
            color = outline,
            topLeft = androidx.compose.ui.geometry.Offset(cx - latchW / 2f, cy + r * 0.48f),
            size = androidx.compose.ui.geometry.Size(latchW, latchH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(latchW / 2f),
            style = Stroke(r * 0.045f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vacuum stack dialog — shown when tapping a vacuum entity button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VacuumStackDialog(
    entities: List<HAEntity>,
    startIndex: Int = 0,
    buttonConfigs: Map<String, HKIButtonConfig>,
    allEntities: List<HAEntity>,
    currentUrl: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var page         by remember(entities) { mutableIntStateOf(startIndex.coerceIn(0, (entities.size - 1).coerceAtLeast(0))) }
    var dragAmount   by remember { mutableFloatStateOf(0f) }
    var refreshTick  by remember { mutableIntStateOf(0) }

    val entity = entities.getOrElse(page) { entities.first() }
    val config = buttonConfigs[entity.entity_id]
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }
    val resolved = remember(config, allEntities, entityRegistry) { resolveVacuumEntities(config, allEntities, entityRegistry) }
    val mapCameraEntity = resolved.map
    val isCleaning = entity.state == "cleaning"

    // Refresh map periodically
    LaunchedEffect(isCleaning) {
        while (true) {
            delay(if (isCleaning) 3.seconds else 10.seconds)
            refreshTick++
        }
    }

    val rawMapUrl = mapCameraEntity?.let { resolveEntityCameraUrl(it, currentUrl, preferLive = false) }
    val mapUrl    = rawMapUrl?.let { buildCameraRefreshModel(it, 5, refreshTick) }

    val batteryLevel = resolved.battery?.state?.toFloatOrNull()?.toInt()
        ?: entity.attributes?.get("battery_level")?.jsonPrimitive?.intOrNull
    val fanSpeed     = entity.attributes?.get("fan_speed")?.jsonPrimitive?.contentOrNull ?: ""
    val fanSpeedList = (entity.attributes?.get("fan_speed_list") as? JsonArray)
        ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    val statusText   = entity.attributes?.get("status")?.jsonPrimitive?.contentOrNull
        ?: entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val rooms        = parseVacuumRooms(entity)

    // State color
    val stateColor = when (entity.state) {
        "cleaning"  -> Color(0xFF66BB6A)
        "docked"    -> MaterialTheme.colorScheme.primary
        "returning" -> Color(0xFF42A5F5)
        "error"     -> Color(0xFFEF5350)
        "paused"    -> Color(0xFFFFB300)
        else        -> LocalHKIAppColors.current.onMuted
    }

    // Map vacuum state to active tab highlight
    val currentTab = when (entity.state) {
        "cleaning"  -> "Start"
        "paused"    -> "Pause"
        "docked"    -> "Dock"
        "returning" -> "Dock"
        else        -> null
    }

    // Control tabs shown in dialog bottom bar
    val tabs = remember(entity.entity_id) {
        listOf(
            Triple("Dock",   Icons.Default.Home,             { viewModel.vacuumCommand(entity.entity_id, "return_to_base") }),
            Triple("Start",  Icons.Default.PlayArrow,         { viewModel.vacuumCommand(entity.entity_id, "start") }),
            Triple("Pause",  Icons.Default.Pause,             { viewModel.vacuumCommand(entity.entity_id, "pause") }),
            Triple("Stop",   Icons.Default.Stop,              { viewModel.vacuumCommand(entity.entity_id, "stop") }),
            Triple("Locate", Icons.Default.LocationSearching, { viewModel.vacuumSendCommand(entity.entity_id, "locate") })
        )
    }

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.CleaningServices,
        iconTint = stateColor,
        titleOverride = config?.name,
        iconName = config?.icon,
        spinIcon = config?.spinIcon == true,
        statusText = if (entities.size > 1) "${page + 1}/${entities.size} - ${statusText.uppercase()}" else statusText.uppercase(),
        tabs = tabs,
        currentTab = currentTab
    ) {
        val appColors = LocalHKIAppColors.current

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(entities.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmount > 80f && page > 0) page--
                            if (dragAmount < -80f && page < entities.lastIndex) page++
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume(); dragAmount += amount
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Battery + status row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(stateColor, CircleShape))
                    Text(statusText, style = MaterialTheme.typography.labelMedium, color = stateColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.BatteryFull, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    Text(batteryLevel?.let { "$it%" } ?: "--", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                }
            }

            // Map view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A2E))
            ) {
                VacuumMapView(mapUrl = mapUrl)
            }

            // Fan speed
            if (fanSpeedList.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Air, "Fan speed", tint = appColors.onMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(fanSpeedList) { speed ->
                            FilterChip(
                                selected = speed == fanSpeed,
                                onClick = { viewModel.vacuumSetFanSpeed(entity.entity_id, speed) },
                                label = { Text(speed.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            resolved.water?.let { water ->
                val options = (water.attributes?.get("options") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                if (options.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, "Water level", tint = appColors.onMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(options) { option ->
                                FilterChip(
                                    selected = water.state == option,
                                    onClick = { viewModel.callService(water.entity_id.substringBefore('.'), "select_option", com.example.hki7.data.HAServiceCall(water.entity_id, option = option)) },
                                    label = { Text(option.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Rooms
            if (rooms.isNotEmpty()) {
                VacuumRoomsInDialog(rooms, entity, viewModel)
            }

            resolved.emptyBin?.let { empty ->
                FilledTonalButton(
                    onClick = {
                        val domain = empty.entity_id.substringBefore('.')
                        viewModel.callService(domain, if (domain == "button") "press" else "turn_on", com.example.hki7.data.HAServiceCall(empty.entity_id))
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Empty bin")
                }
            }

            // Page dots for multi-vacuum
            if (entities.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    entities.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == page) 8.dp else 6.dp)
                                .background(if (i == page) Color.White else Color.Gray, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VacuumMapView(mapUrl: String?) {
    var scale   by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val appColors = LocalHKIAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale   = (scale * zoom).coerceIn(0.5f, 6f)
                    offsetX += pan.x; offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!mapUrl.isNullOrBlank()) {
            // Two persistent layers: the last good map frame stays visible underneath while a
            // refresh tick loads invisibly on top, so periodic refreshes swap without flashing.
            val stableKey = mapUrl.substringBefore("hki_refresh=").trimEnd('?', '&')
            var lastGoodMap by remember(stableKey) { mutableStateOf<String?>(null) }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
            ) {
                lastGoodMap?.let { fallback ->
                    AsyncImage(fallback, "Vacuum map", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                SubcomposeAsyncImage(
                    model = mapUrl,
                    contentDescription = "Vacuum map",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    success = { lastGoodMap = mapUrl; SubcomposeAsyncImageContent() },
                    error = {
                        if (lastGoodMap == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BrokenImage, null, tint = appColors.onMuted.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                Text("Map unavailable", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Map, null, tint = appColors.onMuted.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                Text("No map camera set.\nConfigure in button settings.", color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun VacuumRoomsInDialog(rooms: Map<Int, String>, entity: HAEntity, viewModel: MainViewModel) {
    val selectedRooms = remember { mutableStateListOf<Int>() }
    val appColors = LocalHKIAppColors.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Rooms", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            if (selectedRooms.isNotEmpty()) {
                TextButton(onClick = {
                    viewModel.vacuumCleanSegments(entity.entity_id, selectedRooms.toList())
                    selectedRooms.clear()
                }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Clean selected", fontSize = 11.sp)
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rooms.entries.toList()) { (id, name) ->
                FilterChip(
                    selected = selectedRooms.contains(id),
                    onClick = { if (selectedRooms.contains(id)) selectedRooms.remove(id) else selectedRooms.add(id) },
                    label = { Text(name, fontSize = 11.sp) }
                )
            }
        }
    }
}

private fun parseVacuumRooms(vacuum: HAEntity?): Map<Int, String> {
    if (vacuum == null) return emptyMap()
    val attr = vacuum.attributes?.get("rooms") as? JsonObject ?: return emptyMap()
    return attr.entries.mapNotNull { (k, v) ->
        k.toIntOrNull()?.let { id -> id to (v.jsonPrimitive.contentOrNull ?: "Room $id") }
    }.toMap()
}

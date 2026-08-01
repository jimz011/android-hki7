@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.HKIButtonStack
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.*
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Composable
private fun vacuumStateLabel(state: String): String = when (state.lowercase(Locale.ROOT)) {
    "cleaning" -> stringResource(R.string.widgets_vacuum_state_cleaning)
    "docked" -> stringResource(R.string.widgets_vacuum_state_docked)
    "paused" -> stringResource(R.string.widgets_vacuum_state_paused)
    "error" -> stringResource(R.string.widgets_vacuum_state_error)
    "returning" -> stringResource(R.string.widgets_vacuum_state_returning)
    else -> state.replace('_', ' ')
}

private fun localizedTitlecase(value: String, locale: Locale): String =
    value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

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
                            EditSettingsButton(
                                onClick = { onButtonSettings(entity.entity_id) },
                                modifier = Modifier.align(Alignment.Center)
                            )
                            EditRemoveBadge(
                                onClick = { onRemoveEntity(entity.entity_id) },
                                modifier = Modifier.align(Alignment.TopEnd)
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
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    onClick: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
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
    val stateTxt = vacuumStateLabel(entity.state)
    val stateColor = when (entity.state) {
        "cleaning"  -> Color(0xFF66BB6A)
        "docked"    -> Color(0xFF42A5F5)
        "paused"    -> Color(0xFFFFB300)
        "error"     -> Color(0xFFEF5350)
        "returning" -> Color(0xFF42A5F5)
        else        -> appColors.onMuted
    }

    val sizeModifier = if (isSquare) Modifier.aspectRatio(1f) else Modifier.aspectRatio(aspectRatio)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(sizeModifier)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated))
            .clickable { onClick() },
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box {
            // Background image
            when (displayMode) {
                "camera" -> if (mapImageUrl != null)
                    AsyncImage(model = mapImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else StaticVacuumGraphic(modifier = Modifier.fillMaxSize(), state = entity.state)
                "external" -> if (!externalUrl.isNullOrBlank())
                    AsyncImage(model = externalUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else StaticVacuumGraphic(modifier = Modifier.fillMaxSize(), state = entity.state)
                else ->
                    StaticVacuumGraphic(modifier = Modifier.fillMaxSize(), state = entity.state)
            }

            // Gradient overlay: dark at bottom (like camera stack)
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
            ))

            // Name + state + battery at BOTTOM-LEFT (like camera stack)
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(displayName, color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).background(stateColor, CircleShape))
                        Text(stateTxt, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f, fill = false))
                        Icon(Icons.Default.BatteryFull, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                        Text(stringResource(R.string.ui_text_fc9db15, batteryLevel), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// Top-down robot vacuum drawn with Canvas for the "static" (Robot image) display mode:
// round body with lid seam, lidar turret, front bumper with notches, and debris-container latch.
@Composable
private fun StaticVacuumGraphic(modifier: Modifier = Modifier, state: String) {
    val primary = MaterialTheme.colorScheme.primary
    val isActive = state == "cleaning"

    Canvas(modifier = modifier) {
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
        // Debris-container latch at the front
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
        ?: vacuumStateLabel(entity.state)
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

    val dockLabel = stringResource(R.string.widgets_vacuum_dock)
    val startLabel = stringResource(R.string.widgets_vacuum_start)
    val pauseLabel = stringResource(R.string.widgets_vacuum_pause)
    val stopLabel = stringResource(R.string.widgets_vacuum_stop)
    val locateLabel = stringResource(R.string.widgets_vacuum_locate)

    // Map the raw vacuum state to the localized tab label used by HKIDialog.
    val currentTab = when (entity.state) {
        "cleaning"  -> startLabel
        "paused"    -> pauseLabel
        "docked"    -> dockLabel
        "returning" -> dockLabel
        else        -> null
    }

    // Control tabs shown in dialog bottom bar
    val tabs = listOf(
        Triple(dockLabel, Icons.Default.Home) { viewModel.vacuumCommand(entity.entity_id, "return_to_base") },
        Triple(startLabel, Icons.Default.PlayArrow) { viewModel.vacuumCommand(entity.entity_id, "start") },
        Triple(pauseLabel, Icons.Default.Pause) { viewModel.vacuumCommand(entity.entity_id, "pause") },
        Triple(stopLabel, Icons.Default.Stop) { viewModel.vacuumCommand(entity.entity_id, "stop") },
        Triple(locateLabel, Icons.Default.LocationSearching) { viewModel.vacuumSendCommand(entity.entity_id, "locate") }
    )

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.CleaningServices,
        iconTint = stateColor,
        titleOverride = config?.name,
        iconName = config?.icon,
        statusText = if (entities.size > 1) {
            stringResource(R.string.widgets_vacuum_page_status, page + 1, entities.size, statusText)
        } else {
            statusText
        },
        tabs = tabs,
        currentTab = currentTab
    ) {
        val appColors = LocalHKIAppColors.current
        val waterOptions = resolved.water?.let { w ->
            (w.attributes?.get("options") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        }.orEmpty()
        // Below this window height there isn't room for three stacked control rows plus a usable
        // map, so fan/water/empty collapse onto one compact dropdown row (see below).
        val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
        val compactControls = windowHeight < 680.dp &&
            (fanSpeedList.isNotEmpty() || waterOptions.isNotEmpty())

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

            if (compactControls) {
                // Short window: fan + water/mop as compact dropdowns and an inline Empty button, all
                // on one row, so the map above keeps its height instead of being pushed small.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fanSpeedList.isNotEmpty()) {
                        VacuumControlDropdown(
                            Icons.Default.Air,
                            stringResource(R.string.widgets_vacuum_fan_speed),
                            fanSpeed,
                            fanSpeedList
                        ) {
                            viewModel.vacuumSetFanSpeed(entity.entity_id, it)
                        }
                    }
                    resolved.water?.let { water ->
                        if (waterOptions.isNotEmpty()) {
                            VacuumControlDropdown(
                                Icons.Default.WaterDrop,
                                stringResource(R.string.widgets_vacuum_water_level),
                                water.state,
                                waterOptions
                            ) {
                                viewModel.callService(water.entity_id.substringBefore('.'), "select_option", com.jimz011apps.hki7.data.HAServiceCall(water.entity_id, option = it))
                            }
                        }
                    }
                    resolved.emptyBin?.let { empty ->
                        FilledTonalButton(
                            onClick = {
                                val domain = empty.entity_id.substringBefore('.')
                                viewModel.callService(domain, if (domain == "button") "press" else "turn_on", com.jimz011apps.hki7.data.HAServiceCall(empty.entity_id))
                            },
                            shape = itemCornerShape(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.ui_empty_3159fe4), fontSize = 11.sp)
                        }
                    }
                }

                if (rooms.isNotEmpty()) {
                    VacuumRoomsInDialog(rooms, entity, viewModel)
                }
            } else {
                // Fan speed
                if (fanSpeedList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Air,
                            stringResource(R.string.widgets_vacuum_fan_speed),
                            tint = appColors.onMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fanSpeedList) { speed ->
                                FilterChip(
                                    selected = speed == fanSpeed,
                                    onClick = { viewModel.vacuumSetFanSpeed(entity.entity_id, speed) },
                                    label = { Text(localizedTitlecase(speed, locale), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                resolved.water?.let { water ->
                    if (waterOptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.WaterDrop,
                                stringResource(R.string.widgets_vacuum_water_level),
                                tint = appColors.onMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(waterOptions) { option ->
                                    FilterChip(
                                        selected = water.state == option,
                                        onClick = { viewModel.callService(water.entity_id.substringBefore('.'), "select_option", com.jimz011apps.hki7.data.HAServiceCall(water.entity_id, option = option)) },
                                        label = { Text(localizedTitlecase(option, locale), fontSize = 11.sp) }
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
                            viewModel.callService(domain, if (domain == "button") "press" else "turn_on", com.jimz011apps.hki7.data.HAServiceCall(empty.entity_id))
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                        shape = itemCornerShape()
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ui_empty_bin_0cbe2d7))
                    }
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

/** Compact fan-speed / water-level selector: a chip that opens a dropdown, used when the vacuum
 *  dialog is too short to stack full chip rows. */
@Composable
private fun VacuumControlDropdown(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    Box {
        FilterChip(
            selected = false,
            onClick = { open = true },
            leadingIcon = { Icon(icon, contentDescription, modifier = Modifier.size(16.dp)) },
            label = {
                Text(
                    localizedTitlecase(selected.ifBlank { "—" }, locale),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(localizedTitlecase(option, locale)) },
                    onClick = { open = false; onSelect(option) }
                )
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
                    AsyncImage(
                        fallback,
                        stringResource(R.string.widgets_vacuum_map),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SubcomposeAsyncImage(
                    model = mapUrl,
                    contentDescription = stringResource(R.string.ui_vacuum_map_8236541),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    success = { lastGoodMap = mapUrl; SubcomposeAsyncImageContent() },
                    error = {
                        if (lastGoodMap == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BrokenImage, null, tint = appColors.onMuted.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                Text(stringResource(R.string.ui_map_unavailable_0ccd149), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Map, null, tint = appColors.onMuted.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.ui_no_map_camera_set_configure_in_button_settings_b54466c), color = appColors.onMuted,
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
            Text(stringResource(R.string.ui_rooms_3a28d6f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            if (selectedRooms.isNotEmpty()) {
                TextButton(onClick = {
                    viewModel.vacuumCleanSegments(entity.entity_id, selectedRooms.toList())
                    selectedRooms.clear()
                }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(stringResource(R.string.ui_clean_selected_43d8dd5), fontSize = 11.sp)
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

@Composable
private fun parseVacuumRooms(vacuum: HAEntity?): Map<Int, String> {
    if (vacuum == null) return emptyMap()
    val attr = vacuum.attributes?.get("rooms") as? JsonObject ?: return emptyMap()
    return attr.entries.mapNotNull { (k, v) ->
        k.toIntOrNull()?.let { id ->
            id to (v.jsonPrimitive.contentOrNull ?: stringResource(R.string.widgets_vacuum_room_number, id))
        }
    }.toMap()
}

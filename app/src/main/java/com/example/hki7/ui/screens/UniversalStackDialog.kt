@file:Suppress("UnusedBoxWithConstraintsScope")

package com.example.hki7.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.*
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

/**
 * Universal dialog shown when tapping any entity in any button/vacuum stack.
 * Shows ALL entities in the stack with animated horizontal swiping.
 * Each entity renders its domain-appropriate controls.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UniversalStackDialog(
    entities: List<HAEntity>,
    startIndex: Int = 0,
    buttonConfigs: Map<String, HKIButtonConfig> = emptyMap(),
    allEntities: List<HAEntity> = emptyList(),
    currentUrl: String = "",
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    if (entities.isEmpty()) { onDismiss(); return }

    var page       by remember(entities.map { it.entity_id }) { mutableIntStateOf(startIndex.coerceIn(0, entities.lastIndex)) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    var swipeDir   by remember { mutableIntStateOf(0) }  // -1 = left, 1 = right, for animation

    // Live entity lookup so controls stay current after service calls
    val entity = allEntities.find { it.entity_id == entities[page].entity_id } ?: entities[page]
    val domain = entity.entity_id.substringBefore(".")

    // Per-domain ephemeral state that resets when page changes
    var lightTab     by remember(page) { mutableStateOf(defaultLightTab(entity)) }
    var coverAction  by remember(page) { mutableStateOf(defaultCoverAction(entity)) }
    var climateMode  by remember(page) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { climateMode = entity.state }
    var showClimateModes by remember(page) { mutableStateOf(false) }

    // Door entity for lock domains
    val doorEntityId = buttonConfigs[entity.entity_id]?.doorEntityId
    val doorEntity   = doorEntityId?.let { id -> allEntities.find { it.entity_id == id } }

    // Vacuum config for vacuum domain
    val vacuumConfig = buttonConfigs[entity.entity_id]
    val headerConfig = buttonConfigs[entity.entity_id]

    // ── Icon + tint for header ────────────────────────────────────────────────
    // Reuses HKIBadgeBar's domainIcon() so the stack dialog header always matches the badge/tile
    // default icon for the same entity, instead of maintaining a second table that can drift.
    val icon = domainIcon(entity)
    val iconTint = when (domain) {
        "light"   -> lightStateColor(entity) ?: MaterialTheme.colorScheme.primary
        "lock"    -> when { doorEntity?.state == "on" || entity.state == "open" -> LockRed; entity.state == "locked" -> LockGreen; else -> LockOrange }
        "cover"   -> coverAccentColor(entity)
        "climate" -> hvacColor(entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull ?: entity.state)
        "vacuum"  -> when (entity.state) { "cleaning" -> Color(0xFF66BB6A); "paused" -> Color(0xFFFFB300); "error" -> Color(0xFFEF5350); else -> MaterialTheme.colorScheme.primary }
        else      -> MaterialTheme.colorScheme.primary
    }

    // ── Status text ───────────────────────────────────────────────────────────
    val statusText = buildString {
        if (entities.size > 1) append("${page + 1}/${entities.size} - ")
        when (domain) {
            "light" -> append(if (entity.state == "on") { val b = ((entity.brightness ?: 0) / 255f * 100).toInt(); "$b% • ON" } else "OFF")
            "cover" -> append("${entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0}% - ${entity.state.uppercase()}")
            "vacuum" -> append((entity.attributes?.get("status")?.jsonPrimitive?.contentOrNull ?: entity.state).uppercase())
            else -> append(entity.state.uppercase())
        }
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────
    val tabs: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> = when (domain) {
        "light" -> buildList {
            if (entity.supportsBrightness) add(Triple("Bright", Icons.Default.LightMode) { lightTab = "Bright" })
            if (entity.supportsColorTemp)  add(Triple("Temp",   Icons.Default.Thermostat) { lightTab = "Temp" })
            if (entity.supportsColor)      add(Triple("Color",  Icons.Default.Palette) { lightTab = "Color" })
            if (entity.effectList.isNotEmpty()) add(Triple("Effects", Icons.Default.AutoAwesome) { lightTab = "Effects" })
        }
        "lock" -> listOf(Triple("Open Door", Icons.Default.DoorFront) { viewModel.openLock(entity.entity_id) })
        "cover" -> listOf(
            Triple("Open",  Icons.Default.ArrowUpward)  { coverAction = "Open";  viewModel.controlCover(entity.entity_id, "open_cover") },
            Triple("Stop",  Icons.Default.Stop)          { coverAction = "Stop";  viewModel.controlCover(entity.entity_id, "stop_cover") },
            Triple("Close", Icons.Default.ArrowDownward) { coverAction = "Close"; viewModel.controlCover(entity.entity_id, "close_cover") }
        )
        "climate" -> buildList {
            entity.hvacModes.ifEmpty { listOf("cool", "heat", "auto", "off") }.forEach { mode ->
                add(Triple(climateModeLabel(mode), climateModeIcon(mode)) {
                    climateMode = mode; viewModel.setHvacMode(entity.entity_id, mode)
                })
            }
        }
        "vacuum" -> listOf(
            Triple("Dock",   Icons.Default.Home)             { viewModel.vacuumCommand(entity.entity_id, "return_to_base") },
            Triple("Start",  Icons.Default.PlayArrow)         { viewModel.vacuumCommand(entity.entity_id, "start") },
            Triple("Pause",  Icons.Default.Pause)             { viewModel.vacuumCommand(entity.entity_id, "pause") },
            Triple("Stop",   Icons.Default.Stop)              { viewModel.vacuumCommand(entity.entity_id, "stop") },
            Triple("Locate", Icons.Default.LocationSearching) { viewModel.vacuumSendCommand(entity.entity_id, "locate") }
        )
        else -> emptyList()
    }

    val currentTab = when (domain) {
        "light"   -> lightTab
        "cover"   -> coverAction
        "climate" -> climateModeLabel(climateMode)
        "vacuum"  -> when (entity.state) { "cleaning" -> "Start"; "paused" -> "Pause"; "docked" -> "Dock"; "returning" -> "Dock"; else -> null }
        else -> null
    }

    val climateConfig = buttonConfigs[entity.entity_id]
    val extraGraphEntityIds = if (domain == "climate") {
        listOfNotNull(climateConfig?.climateTempSensorEntityId, climateConfig?.climateHumiditySensorEntityId)
    } else emptyList()

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = icon,
        iconTint = iconTint,
        titleOverride = headerConfig?.name,
        iconName = headerConfig?.icon?.takeUnless { it.isBlank() }
            ?: defaultEntityIconSlug(entity, lockDoorOpen = doorEntity?.state == "on"),
        spinIcon = headerConfig?.spinIcon == true,
        statusText = statusText,
        extraGraphEntityIds = extraGraphEntityIds,
        tabs = if (domain == "climate") tabs.takeIf { it.size > 1 } ?: emptyList() else tabs,
        currentTab = currentTab
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(entities.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmount > 80f && page > 0)             { swipeDir = 1;  page-- }
                            if (dragAmount < -80f && page < entities.lastIndex) { swipeDir = -1; page++ }
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume(); dragAmount += amount
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Animated entity content ───────────────────────────────────────
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState > initialState) -1 else 1
                    (slideInHorizontally(tween(220)) { it * -dir } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { it * dir } + fadeOut(tween(150)))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "stack_page"
            ) { pg ->
                val liveEntity = allEntities.find { it.entity_id == entities[pg].entity_id } ?: entities[pg]
                val d = liveEntity.entity_id.substringBefore(".")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (d) {
                        "light"  -> UniversalLightContent(liveEntity, viewModel, lightTab)
                        "lock"   -> UniversalLockContent(liveEntity, viewModel, doorEntity)
                        "cover"  -> UniversalCoverContent(liveEntity, viewModel)
                        "climate"-> UniversalClimateContent(liveEntity, viewModel, showModes = showClimateModes, onToggleModes = { showClimateModes = it })
                        "vacuum" -> UniversalVacuumContent(liveEntity, vacuumConfig, allEntities, currentUrl, viewModel)
                        "switch", "input_boolean", "fan" -> UniversalSwitchContent(liveEntity, viewModel)
                        "camera" -> UniversalCameraContent(liveEntity, currentUrl)
                        else     -> UniversalGenericContent(liveEntity)
                    }
                }
            }

            // Page dots
            if (entities.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    entities.indices.forEach { i ->
                        Box(modifier = Modifier
                            .size(if (i == page) 8.dp else 6.dp)
                            .background(if (i == page) Color.White else Color.Gray, CircleShape))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-domain content composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UniversalLightContent(entity: HAEntity, viewModel: MainViewModel, currentTab: String) {
    val appColors = LocalHKIAppColors.current
    if (!entity.supportsBrightness && currentTab !in setOf("Temp", "Color", "Effects")) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (entity.state == "on") "On" else "Off", color = appColors.onSurface, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                VerticalMasterSwitch(isOn = entity.state == "on", onToggle = { viewModel.toggleEntity(entity.entity_id) })
            }
            Spacer(Modifier.height(16.dp))
            Text("SWITCH", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
        return
    }
    when (currentTab) {
        "Bright" -> {
            var v by remember(entity.entity_id) { mutableFloatStateOf((entity.brightness ?: 0) / 255f) }
            LaunchedEffect(entity.brightness) { v = (entity.brightness ?: 0) / 255f }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(v * 100).toInt()}%", color = appColors.onSurface, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    VerticalSlider(v, { v = it; viewModel.setOptimisticBrightness(entity.entity_id, it) }, { viewModel.setBrightness(entity.entity_id, v) }, activeColor = Color(0xFFFFA500))
                }
                Spacer(Modifier.height(16.dp))
                Text("BRIGHTNESS", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        "Temp" -> {
            val minK = (entity.minKelvin ?: 2000).toFloat(); val maxK = (entity.maxKelvin ?: 6500).toFloat()
            var k by remember(entity.entity_id) { mutableFloatStateOf(entity.colorTempKelvin?.toFloat() ?: 3000f) }
            LaunchedEffect(entity.colorTempKelvin) { k = entity.colorTempKelvin?.toFloat() ?: 3000f }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${k.toInt()}K", color = appColors.onSurface, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    VerticalSlider(((k - minK) / (maxK - minK)).coerceIn(0f, 1f), { k = minK + it * (maxK - minK) }, { viewModel.setColorTemp(entity.entity_id, k.toInt()) }, gradient = Brush.verticalGradient(listOf(Color(0xFFCCE6FF), Color(0xFFFFCC33))))
                }
                Spacer(Modifier.height(16.dp))
                Text("TEMPERATURE", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        "Color" -> {
            var rgb by remember(entity.entity_id) { mutableStateOf(entity.rgbColor) }
            LaunchedEffect(entity.rgbColor) { rgb = entity.rgbColor }
            ColorWheel(selectedRgb = rgb, onColorSelected = { rgb = it; viewModel.setRgbColor(entity.entity_id, it) }, onValueChangeFinished = {})
        }
        "Effects" -> {
            val appColors2 = LocalHKIAppColors.current
            val effectsScroll = rememberScrollState()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).fadingEdges(effectsScroll).verticalScroll(effectsScroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entity.effectList.forEach { effect ->
                    Surface(modifier = Modifier.fillMaxWidth().clickable { viewModel.setLightEffect(entity.entity_id, effect) }, shape = RoundedCornerShape(18.dp), color = if (effect == entity.effect) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else appColors2.subtleSurface) {
                        Text(effect, modifier = Modifier.padding(16.dp), color = appColors2.onSurface)
                    }
                }
            }
        }
        else -> {
            // Light is off and no control available
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Off", color = appColors.onMuted, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.toggleEntity(entity.entity_id) }) { Text("Turn On") }
            }
        }
    }
}

@Composable
private fun UniversalLockContent(entity: HAEntity, viewModel: MainViewModel, doorEntity: HAEntity?) {
    val appColors = LocalHKIAppColors.current
    val isDoorOpen = doorEntity?.state == "on"
    val accentColor = when { isDoorOpen || entity.state == "open" -> LockRed; entity.state == "locked" -> LockGreen; else -> LockOrange }
    val stateText = when { isDoorOpen || entity.state == "open" -> "Open"; entity.state == "locked" -> "Locked"; else -> "Unlocked" }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stateText, color = appColors.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VerticalMasterSwitch(isOn = entity.state == "locked", onToggle = { viewModel.toggleLock(entity.entity_id) }, accentColor = accentColor, doorOpen = isDoorOpen)
        }
        Spacer(Modifier.height(16.dp))
        Text("LOCK", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun UniversalCoverContent(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val pos = entity.attributes?.get("current_position")?.jsonPrimitive?.intOrNull ?: 0
    var local by remember(entity.entity_id) { mutableFloatStateOf(pos / 100f) }
    LaunchedEffect(pos) { local = pos / 100f }
    val hasTilt = entity.supportsTilt
    var localTilt by remember(entity.tiltPosition) { mutableFloatStateOf((entity.tiltPosition ?: 0) / 100f) }
    val accent = coverAccentColor(entity)
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${(local * 100).toInt()}%", color = appColors.onSurface, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VerticalSlider(
                    local,
                    { local = it; viewModel.setOptimisticCoverPosition(entity.entity_id, (it * 100).toInt()) },
                    { viewModel.setCoverPosition(entity.entity_id, (local * 100).toInt()) },
                    activeColor = accent,
                    trackColor = accent.copy(alpha = 0.18f)
                )
                if (hasTilt) {
                    CoverTiltSlider(
                        value = localTilt,
                        onValueChange = { localTilt = it; viewModel.setOptimisticCoverTilt(entity.entity_id, (it * 100).toInt()) },
                        onValueChangeFinished = { viewModel.setCoverTiltPosition(entity.entity_id, (localTilt * 100).toInt()) },
                        activeColor = accent
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (hasTilt) 44.dp else 0.dp)) {
            Text("POSITION", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            if (hasTilt) Text("TILT", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * The temperature slider and the fan/swing modes list share this single top-anchored,
 * full-height layout so the modes toggle button always lands in the same spot regardless of
 * which one is showing — the middle region absorbs any size difference via [Modifier.weight].
 */
@Composable
private fun UniversalClimateContent(entity: HAEntity, viewModel: MainViewModel, showModes: Boolean, onToggleModes: (Boolean) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val minT = entity.attributes?.get("min_temp")?.jsonPrimitive?.doubleOrNull ?: 15.0
    val maxT = entity.attributes?.get("max_temp")?.jsonPrimitive?.doubleOrNull ?: 30.0
    val target = entity.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull ?: 21.0
    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
    val mode = entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull ?: entity.state
    var local by remember(entity.entity_id) { mutableFloatStateOf(target.toFloat()) }
    val climateColor = hvacColor(hvacAction ?: mode)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (showModes) 32.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (showModes) "Modes" else "${"%.1f".format(local)}°",
            color = appColors.onSurface,
            style = if (showModes) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showModes) {
                ClimateModesList(entity, viewModel)
            } else {
                VerticalSlider(
                    value = ((local - minT.toFloat()) / (maxT.toFloat() - minT.toFloat())).coerceIn(0f, 1f),
                    onValueChange = { local = minT.toFloat() + it * (maxT - minT).toFloat() },
                    onValueChangeFinished = { viewModel.setClimateTemp(entity.entity_id, local) },
                    gradient = Brush.verticalGradient(listOf(climateColor.copy(alpha = 0.35f), climateColor))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (showModes) "MODES" else "TARGET", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        ClimateModesButton(entity, onClick = { onToggleModes(!showModes) }, selected = showModes)
    }
}

@Composable
private fun UniversalVacuumContent(entity: HAEntity, config: HKIButtonConfig?, allEntities: List<HAEntity>, currentUrl: String, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val mapCameraEntity = config?.vacuumMapEntityId?.let { id -> allEntities.find { it.entity_id == id } }
    var tick by remember { mutableIntStateOf(0) }
    val isCleaning = entity.state == "cleaning"
    LaunchedEffect(isCleaning) { while (true) { delay(if (isCleaning) 3000L else 10000L); tick++ } }
    val mapUrl = mapCameraEntity?.let { resolveEntityCameraUrl(it, currentUrl, false) }?.let { buildCameraRefreshModel(it, 5, tick) }
    val fanSpeedList = (entity.attributes?.get("fan_speed_list") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    val fanSpeed = entity.attributes?.get("fan_speed")?.jsonPrimitive?.contentOrNull ?: ""
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1A1A2E))) {
            if (!mapUrl.isNullOrBlank()) {
                var scale by remember { mutableFloatStateOf(1f) }
                var oX by remember { mutableFloatStateOf(0f) }
                var oY by remember { mutableFloatStateOf(0f) }
                AsyncImage(
                    model = mapUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = oX, translationY = oY)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 6f); oX += pan.x; oY += pan.y
                            }
                        }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No map camera\nconfigured", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
        if (fanSpeedList.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(fanSpeedList) { speed ->
                    FilterChip(selected = speed == fanSpeed, onClick = { viewModel.vacuumSetFanSpeed(entity.entity_id, speed) }, label = { Text(speed.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                }
            }
        }
    }
}

@Composable
private fun UniversalSwitchContent(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (entity.state == "on") "On" else "Off", color = appColors.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VerticalMasterSwitch(isOn = entity.state == "on", onToggle = { viewModel.toggleEntity(entity.entity_id) })
        }
        Spacer(Modifier.height(16.dp))
        Text("SWITCH", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun UniversalCameraContent(entity: HAEntity, currentUrl: String) {
    val appColors = LocalHKIAppColors.current
    val streamUrl = resolveEntityCameraUrl(entity, currentUrl, preferLive = false)
    if (streamUrl != null) {
        AsyncImage(model = streamUrl, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp)))
    } else {
        Text("No stream available", color = appColors.onMuted)
    }
}

@Composable
private fun UniversalGenericContent(entity: HAEntity) {
    val appColors = LocalHKIAppColors.current
    Text(entity.state.replaceFirstChar { it.uppercase() }, color = appColors.onSurface, style = MaterialTheme.typography.headlineMedium)
}

// Helper functions
private fun defaultLightTab(entity: HAEntity) = when {
    entity.supportsBrightness -> "Bright"
    entity.effectList.isNotEmpty() -> "Effects"
    entity.supportsColorTemp -> "Temp"
    entity.supportsColor -> "Color"
    else -> "Bright"
}

private fun defaultCoverAction(entity: HAEntity) = when (entity.state) {
    "opening", "open" -> "Open"; "closing", "closed" -> "Close"; else -> "Stop"
}

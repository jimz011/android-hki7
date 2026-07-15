package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors

@Composable
fun HKILightDialog(
    entity: HAEntity,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    groupContent: (@Composable () -> Unit)? = null,
    titleOverride: String? = null,
    iconName: String? = null,
    spinIcon: Boolean = false
) {
    val topAppColors = LocalHKIAppColors.current
    val supportsBrightness = entity.supportsBrightness
    val supportsColorTemp = entity.supportsColorTemp
    val supportsColor = entity.supportsColor
    // Reflect the light's real colour (rgb / colour-temp) for the header icon and brightness slider.
    val lightAccent = lightStateColor(entity) ?: Color(0xFFFFA500)
    val effects = entity.effectList
    val supportsEffects = effects.isNotEmpty()
    var localEffect by remember(entity.entity_id) { mutableStateOf(entity.effect) }
    LaunchedEffect(entity.effect) { localEffect = entity.effect }

    fun defaultTab(): String = when {
        supportsBrightness -> "Bright"
        supportsEffects -> "Effects"
        supportsColorTemp -> "Temp"
        supportsColor -> "Color"
        else -> "Bright"
    }

    var currentTab by remember(entity.entity_id) { mutableStateOf(defaultTab()) }
    LaunchedEffect(entity.state, supportsBrightness, supportsColorTemp, supportsColor, supportsEffects) {
        val availableTabs = buildSet {
            if (supportsBrightness) add("Bright")
            if (supportsColorTemp) add("Temp")
            if (supportsColor) add("Color")
            if (supportsEffects) add("Effects")
        }
        if (currentTab !in availableTabs) currentTab = defaultTab()
    }
    val brightnessPercent = ((entity.brightness ?: 0) / 255f * 100).toInt()
    val statusText = if (entity.state == "on") "$brightnessPercent% • ACTIVE" else "OFF"

    val tabs = mutableListOf<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>>()
    if (supportsBrightness) tabs.add(Triple("Bright", Icons.Default.LightMode) { currentTab = "Bright" })
    if (supportsColorTemp) tabs.add(Triple("Temp", Icons.Default.Thermostat) { currentTab = "Temp" })
    if (supportsColor) tabs.add(Triple("Color", Icons.Default.Palette) { currentTab = "Color" })
    if (supportsEffects) tabs.add(Triple("Effects", Icons.Default.AutoAwesome) { currentTab = "Effects" })

    val visibleTabs = tabs.ifEmpty { emptyList() }

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Lightbulb,
        iconTint = if (entity.state == "on") lightAccent else topAppColors.onMuted,
        titleOverride = titleOverride,
        iconName = iconName,
        spinIcon = spinIcon,
        statusText = statusText,
        groupContent = groupContent,
        tabs = visibleTabs,
        currentTab = currentTab
    ) {
        val appColors = LocalHKIAppColors.current
        val isOff = entity.state == "off"
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isOff && visibleTabs.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(64.dp), tint = appColors.onMuted.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Light is turned off", style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
                    Text("Turn the light on to access controls", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        currentTab = defaultTab()
                        viewModel.toggleEntity(entity.entity_id)
                    }) {
                        Text("Turn On")
                    }
                }
            } else if (currentTab == "Effects") {
                LightEffectsContent(
                    effects = effects,
                    activeEffect = localEffect,
                    onEffectClick = {
                        localEffect = it
                        viewModel.setLightEffect(entity.entity_id, it)
                    }
                )
            } else if (!supportsBrightness && currentTab !in setOf("Temp", "Color")) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (entity.state == "on") "On" else "Off",
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        VerticalMasterSwitch(
                            isOn = entity.state == "on",
                            onToggle = { viewModel.toggleEntity(entity.entity_id) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("SWITCH", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                when (currentTab) {
                    "Bright" -> {
                        var sliderValue by remember(entity.entity_id) { mutableFloatStateOf((entity.brightness ?: 0) / 255f) }
                        LaunchedEffect(entity.brightness) { sliderValue = (entity.brightness ?: 0) / 255f }
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(sliderValue * 100).toInt()}%", color = appColors.onSurface, style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(24.dp))
                            Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                VerticalSlider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sliderValue = it
                                        viewModel.setOptimisticBrightness(entity.entity_id, it)
                                    },
                                    onValueChangeFinished = { viewModel.setBrightness(entity.entity_id, sliderValue) },
                                    activeColor = lightAccent
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("BRIGHTNESS", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    "Temp" -> {
                        val minK = entity.minKelvin ?: 2000
                        val maxK = entity.maxKelvin ?: 6500
                        var kelvinValue by remember(entity.entity_id) { mutableFloatStateOf(entity.colorTempKelvin?.toFloat() ?: 3000f) }
                        LaunchedEffect(entity.colorTempKelvin) { kelvinValue = entity.colorTempKelvin?.toFloat() ?: 3000f }
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${kelvinValue.toInt()}K", color = appColors.onSurface, style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(24.dp))
                            Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                VerticalSlider(
                                    value = ((kelvinValue - minK) / (maxK - minK)).coerceIn(0f, 1f),
                                    onValueChange = { kelvinValue = minK + it * (maxK - minK) },
                                    onValueChangeFinished = { viewModel.setColorTemp(entity.entity_id, kelvinValue.toInt()) },
                                    gradient = Brush.verticalGradient(listOf(Color(0xFFCCE6FF), Color(0xFFFFCC33)))
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("TEMPERATURE", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    "Color" -> {
                        var localRgb by remember(entity.entity_id) { mutableStateOf(entity.rgbColor) }
                        LaunchedEffect(entity.rgbColor) { localRgb = entity.rgbColor }
                        ColorWheel(selectedRgb = localRgb, onColorSelected = { localRgb = it; viewModel.setRgbColor(entity.entity_id, it) }, onValueChangeFinished = {})
                    }
                    "Effects" -> {
                        LightEffectsContent(
                            effects = effects,
                            activeEffect = localEffect,
                            onEffectClick = {
                                localEffect = it
                                viewModel.setLightEffect(entity.entity_id, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LightEffectsContent(
    effects: List<String>,
    activeEffect: String?,
    onEffectClick: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .fadingEdges(scrollState)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Effects", color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
        Text("Select an effect to turn it on", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))

        effects.forEach { effect ->
            val selected = effect == activeEffect
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEffectClick(effect) },
                shape = itemCornerShape(),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else appColors.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else appColors.onMuted.copy(alpha = 0.16f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = effect,
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalMasterSwitch(
    modifier: Modifier = Modifier,
    isOn: Boolean,
    onToggle: () -> Unit,
    accentColor: Color? = null,
    doorOpen: Boolean = false
) {
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    val appColors = LocalHKIAppColors.current
    // Fill logic: door open → red top half; locked → colored bottom half; unlocked → no fill.
    val fillHeight   = if (doorOpen) 0.5f else if (isOn) 0.58f else 0.0f
    val fillAlign    = if (doorOpen) Alignment.TopCenter else Alignment.BottomCenter
    val fillGradient = if (doorOpen) {
        // Intensity increases downward from top when door is open
        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.82f), accent.copy(alpha = 0.35f)))
    } else {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.82f), accent))
    }
    // Use surface as outer background so the switch contrasts with the dialog card (elevated).
    // Swap knob to elevated so it's distinguishable from the surface background in all modes.
    Box(
        modifier = modifier
            .height(VerticalControlHeight)
            .width(100.dp)
            .clip(itemCornerShape())
            .background(appColors.surface)
            .clickable { onToggle() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fillHeight)
                .background(fillGradient)
                .align(fillAlign)
        )
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(116.dp)
                .align(if (isOn) Alignment.TopCenter else Alignment.BottomCenter)
                .border(1.dp, appColors.onMuted.copy(alpha = 0.18f), itemCornerShape())
                .clip(itemCornerShape())
                .background(appColors.elevated)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent, itemCornerShape())
        ) {
            Surface(
                modifier = Modifier.matchParentSize(),
                shape = itemCornerShape(),
                color = Color.Transparent,
                border = BorderStroke(2.dp, accent.copy(alpha = if (isOn || doorOpen) 0.9f else 0.55f))
            ) {}
        }
    }
}

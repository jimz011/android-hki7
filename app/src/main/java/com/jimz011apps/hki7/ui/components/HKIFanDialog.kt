package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

val FanBlue = Color(0xFF1E90FF)

@Composable
fun HKIFanDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    titleOverride: String? = null,
    iconName: String? = null
) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val supportsPercentage = entity.fanPercentage != null

    fun defaultTab() = "speed"
    var currentTab by remember(entity.entity_id) { mutableStateOf(defaultTab()) }
    var showPresets by remember(entity.entity_id) { mutableStateOf(false) }

    val tabs = buildList {
        if (supportsPercentage) {
            add(Triple(stringResource(R.string.uif_speed), Icons.Default.Air) { currentTab = "speed" })
        }
    }
    val navigationTabs = tabs.takeIf { it.size > 1 } ?: emptyList()

    val statusText = if (isOn) {
        entity.fanPercentage?.let { stringResource(R.string.dlg_percent_on, it) } ?: stringResource(R.string.dlg_on_uppercase)
    } else stringResource(R.string.dlg_off_uppercase)

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Air,
        iconTint = if (isOn) FanBlue else appColors.onMuted,
        titleOverride = titleOverride,
        iconName = iconName,
        statusText = statusText,
        tabs = navigationTabs,
        currentTab = currentTab
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FanContent(
                entity = entity,
                viewModel = viewModel,
                isOn = isOn,
                supportsPercentage = supportsPercentage,
                showPresets = showPresets,
                onTogglePresets = { showPresets = it }
            )
        }
    }
}

/**
 * The value title sits above the control, and the speed slider / on-off switch / presets list
 * centers as a block together with the label and button below it, within the space left under
 * the title — matching the original dialog layout.
 */
@Composable
private fun FanContent(
    entity: HAEntity,
    viewModel: MainViewModel,
    isOn: Boolean,
    supportsPercentage: Boolean,
    showPresets: Boolean,
    onTogglePresets: (Boolean) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var sliderValue by remember(entity.entity_id) { mutableFloatStateOf((entity.fanPercentage ?: 0) / 100f) }
    LaunchedEffect(entity.fanPercentage) { sliderValue = (entity.fanPercentage ?: 0) / 100f }
    val hasSecondaryControls = supportsPercentage && (entity.fanOscillating != null || entity.fanDirection != null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (showPresets) 32.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                showPresets -> stringResource(R.string.dlg_presets)
                supportsPercentage -> stringResource(R.string.dlg_percentage, (sliderValue * 100).toInt())
                else -> if (isOn) stringResource(R.string.dlg_on) else stringResource(R.string.dlg_off)
            },
            color = appColors.onSurface,
            style = if (showPresets || supportsPercentage) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                showPresets -> FanPresetList(entity, viewModel)
                supportsPercentage -> VerticalSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { viewModel.setFanPercentage(entity.entity_id, (sliderValue * 100).toInt()) },
                    activeColor = FanBlue
                )
                else -> VerticalMasterSwitch(
                    isOn = isOn,
                    onToggle = { viewModel.toggleEntity(entity.entity_id) },
                    accentColor = FanBlue
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (showPresets) stringResource(R.string.dlg_presets_uppercase) else if (supportsPercentage) stringResource(R.string.dlg_speed) else stringResource(R.string.dlg_switch),
            color = appColors.onMuted,
            style = MaterialTheme.typography.labelSmall
        )
        PresetsButton(entity, onClick = { onTogglePresets(!showPresets) }, selected = showPresets)
        if (!showPresets && hasSecondaryControls) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                entity.fanOscillating?.let { osc ->
                    FilterChip(
                        selected = osc,
                        onClick = { viewModel.setFanOscillating(entity.entity_id, !osc) },
                        label = { Text(stringResource(R.string.dlg_oscillate)) },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                entity.fanDirection?.let { dir ->
                    FilterChip(
                        selected = dir == "reverse",
                        onClick = { viewModel.setFanDirection(entity.entity_id, if (dir == "forward") "reverse" else "forward") },
                        label = { Text(if (dir == "forward") stringResource(R.string.dlg_forward) else stringResource(R.string.dlg_reverse)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetsButton(entity: HAEntity, onClick: () -> Unit, selected: Boolean = false) {
    if (entity.fanPresetModes.isEmpty()) return

    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(if (selected) stringResource(R.string.dlg_back) else stringResource(R.string.dlg_presets)) },
            leadingIcon = { Icon(if (selected) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun FanPresetList(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fadingEdges(scrollState)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (entity.fanPercentage != null) {
            // No active preset means the device is running a manually-set speed rather than a
            // named profile — light up "Custom" so it's clear the slider drove the current speed.
            val isCustom = entity.fanPresetMode.isNullOrBlank()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = itemCornerShape(),
                color = if (isCustom) FanBlue.copy(alpha = 0.22f) else appColors.surface,
                border = BorderStroke(1.dp, if (isCustom) FanBlue.copy(alpha = 0.7f) else appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = if (isCustom) FanBlue else appColors.onMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.dlg_custom),
                        color = if (isCustom) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    entity.fanPercentage?.let {
                        Text(stringResource(R.string.dlg_percentage, it), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (isCustom) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FanBlue)
                }
            }
        }
        entity.fanPresetModes.forEach { mode ->
            val selected = mode == entity.fanPresetMode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setFanPresetMode(entity.entity_id, mode)
                    },
                shape = itemCornerShape(),
                color = if (selected) FanBlue.copy(alpha = 0.22f) else appColors.surface,
                border = BorderStroke(1.dp, if (selected) FanBlue.copy(alpha = 0.7f) else appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = if (selected) FanBlue else appColors.onMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = localizedDeviceModeLabel(mode),
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FanBlue)
                }
            }
        }
    }
}

/** Localizes well-known HA mode tokens while leaving integration-specific catalog values intact. */
@Composable
internal fun localizedDeviceModeLabel(mode: String): String = when (mode.lowercase()) {
    "auto", "smart" -> stringResource(R.string.uif_mode_auto)
    "normal" -> stringResource(R.string.uif_mode_normal)
    "eco" -> stringResource(R.string.uif_mode_eco)
    "sleep", "night" -> stringResource(R.string.uif_mode_sleep)
    "quiet", "silent" -> stringResource(R.string.uif_mode_quiet)
    "low" -> stringResource(R.string.uif_mode_low)
    "medium" -> stringResource(R.string.uif_mode_medium)
    "high" -> stringResource(R.string.uif_mode_high)
    "boost", "turbo", "max" -> stringResource(R.string.uif_mode_boost)
    "comfort" -> stringResource(R.string.uif_mode_comfort)
    "home" -> stringResource(R.string.uif_mode_home)
    "away" -> stringResource(R.string.uif_mode_away)
    "baby" -> stringResource(R.string.uif_mode_baby)
    "continuous", "cont", "continuously" -> stringResource(R.string.uif_mode_continuous)
    "manual" -> stringResource(R.string.uif_mode_manual)
    "dry", "dryer", "drying" -> stringResource(R.string.uif_mode_dry)
    "laundry" -> stringResource(R.string.uif_mode_laundry)
    "clothes_dry", "clothes" -> stringResource(R.string.uif_mode_clothes)
    "purify", "purifier" -> stringResource(R.string.uif_mode_purify)
    else -> mode.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

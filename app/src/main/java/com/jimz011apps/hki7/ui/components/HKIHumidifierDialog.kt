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
import com.jimz011apps.hki7.data.HAServiceCall
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

val HumidifierCyan = Color(0xFF00BCD4)

/** Known humidifier/dehumidifier preset modes mapped to a distinct icon each. */
private fun namedHumidifierModeIcon(mode: String): androidx.compose.ui.graphics.vector.ImageVector? = when (mode.lowercase()) {
    "normal" -> Icons.Default.WaterDrop
    "eco" -> Icons.Default.Eco
    "away" -> Icons.Default.Luggage
    "boost", "turbo", "max", "high" -> Icons.Default.Bolt
    "comfort" -> Icons.Default.Weekend
    "home" -> Icons.Default.Home
    "sleep", "night" -> Icons.Default.Bedtime
    "auto", "smart" -> Icons.Default.AutoMode
    "baby" -> Icons.Default.ChildCare
    "quiet", "silent", "low" -> Icons.Default.VolumeOff
    "continuous", "cont", "continuously" -> Icons.Default.AllInclusive
    "manual" -> Icons.Default.PanTool
    "dry", "dryer", "drying" -> Icons.Default.DryCleaning
    "laundry" -> Icons.Default.LocalLaundryService
    "clothes_dry", "clothes" -> Icons.Default.Checkroom
    "purify", "purifier" -> Icons.Default.FilterAlt
    "medium" -> Icons.Default.Speed
    else -> null
}

/** Fallback icons cycled for modes with no named match, so unrecognized modes still look distinct
 *  from each other instead of all sharing a single generic icon. */
private val FallbackModeIcons = listOf(
    Icons.Default.Tune, Icons.Default.Star, Icons.Default.Circle, Icons.Default.Square,
    Icons.Default.Hexagon, Icons.Default.ChangeHistory, Icons.Default.Diamond, Icons.Default.Adjust
)

/** A distinct icon per humidifier preset mode across the full [allModes] list, so nav-bar tabs are
 *  visually distinguishable even for modes this app doesn't recognize by name. */
fun humidifierModeIcon(mode: String, allModes: List<String>): androidx.compose.ui.graphics.vector.ImageVector {
    namedHumidifierModeIcon(mode)?.let { return it }
    val unnamedIndex = allModes.filter { namedHumidifierModeIcon(it) == null }.indexOf(mode).coerceAtLeast(0)
    return FallbackModeIcons[unnamedIndex % FallbackModeIcons.size]
}

/**
 * Home Assistant's humidifier domain only exposes a single settable target (`humidity`), plus
 * read-only `min_humidity`/`max_humidity` capability bounds from the device — there is no native
 * two-setpoint service. The slider is bounded by those device limits with one live handle.
 */
@Composable
fun HKIHumidifierDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    titleOverride: String? = null,
    iconName: String? = null,
    fanEntity: HAEntity? = null,
    /** Auxiliary humidifier helper entities keyed by slot (see HumidifierAuxSlots). */
    auxEntities: Map<String, HAEntity> = emptyMap()
) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    // Humidifier modes are the dialog's nav-bar tabs (like the climate dialog's hvac modes), rather
    // than a modes button + list.
    val modes = entity.humidifierAvailableModes
    val navigationTabs: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> =
        if (modes.size > 1) modes.map { mode ->
            Triple(localizedDeviceModeLabel(mode), humidifierModeIcon(mode, modes)) {
                viewModel.setHumidifierMode(entity.entity_id, mode)
            }
        } else emptyList()

    val statusText = if (isOn) {
        entity.humidity?.let { stringResource(R.string.dlg_percent_on, it.toInt()) } ?: stringResource(R.string.dlg_on_uppercase)
    } else stringResource(R.string.dlg_off_uppercase)

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.WaterDrop,
        iconTint = if (isOn) HumidifierCyan else appColors.onMuted,
        titleOverride = titleOverride,
        iconName = iconName,
        statusText = statusText,
        tabs = navigationTabs,
        currentTab = entity.humidifierMode?.let { localizedDeviceModeLabel(it) }
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            HumidifierContent(entity = entity, viewModel = viewModel, isOn = isOn, fanEntity = fanEntity, auxEntities = auxEntities)
        }
    }
}

@Composable
private fun HumidifierContent(
    entity: HAEntity,
    viewModel: MainViewModel,
    isOn: Boolean,
    fanEntity: HAEntity?,
    auxEntities: Map<String, HAEntity>
) {
    val appColors = LocalHKIAppColors.current
    val minH = entity.minHumidity ?: 0
    val maxH = (entity.maxHumidity ?: 100).coerceAtLeast(minH + 1)

    fun fractionFor(target: Int) = ((target - minH).toFloat() / (maxH - minH)).coerceIn(0f, 1f)

    var sliderValue by remember(entity.entity_id) { mutableFloatStateOf(fractionFor(entity.humidity?.toInt() ?: minH)) }
    LaunchedEffect(entity.humidity) { sliderValue = fractionFor(entity.humidity?.toInt() ?: minH) }
    val displayValue = minH + (sliderValue * (maxH - minH)).toInt()

    // Prefer a configured current-humidity sensor; fall back to the humidifier's own attribute.
    val currentHumidity = auxEntities["current_humidity"]?.state?.toDoubleOrNull() ?: entity.currentHumidity
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isOn) stringResource(R.string.uif_percentage, displayValue) else stringResource(R.string.dlg_off),
            color = appColors.onSurface,
            style = if (isOn) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge
        )
        if (isOn) {
            currentHumidity?.let { current ->
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.dlg_current, current.toInt()), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isOn) VerticalSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    viewModel.setHumidifierTarget(entity.entity_id, minH + (sliderValue * (maxH - minH)).toInt())
                },
                activeColor = HumidifierCyan
            ) else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(64.dp), tint = appColors.onMuted.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.toggleEntity(entity.entity_id) }) { Text(stringResource(R.string.dlg_turn_on)) }
            }
        }
        if (isOn) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.dlg_target_range_percent, minH, maxH), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            // A linked fan / select / input_select supplies the speed options.
            if (fanEntity != null) {
                Spacer(Modifier.height(14.dp))
                HumidifierSpeedControl(fanEntity, viewModel)
            }
        }
        HumidifierAuxSection(auxEntities, viewModel)
    }
}

/** Read-only status chips (tank, PM2.5, error, filter, defrost, bucket) plus toggle chips for the
 *  switch-domain helpers (ionizer, pump, sleep, beep) configured for this humidifier. */
@Composable
private fun HumidifierAuxSection(auxEntities: Map<String, HAEntity>, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    // Info chips: value/state shown for these slots when set.
    val infoSlots = listOf(
        "tank_level" to stringResource(R.string.uif_tank),
        "pm25" to stringResource(R.string.uif_pm25),
        "error" to stringResource(R.string.uif_error),
        "bucket_full" to stringResource(R.string.uif_bucket),
        "clean_filter" to stringResource(R.string.uif_filter),
        "defrost" to stringResource(R.string.uif_defrost),
    )
    val infoEntries = infoSlots.mapNotNull { (key, label) -> auxEntities[key]?.let { label to it } }
    val toggleSlots = listOf(
        "ionizer" to stringResource(R.string.uif_ionizer),
        "pump" to stringResource(R.string.uif_pump),
        "sleep" to stringResource(R.string.uif_sleep),
        "beep" to stringResource(R.string.uif_beep),
    )
    val toggleEntries = toggleSlots.mapNotNull { (key, label) -> auxEntities[key]?.let { label to it } }
    if (infoEntries.isEmpty() && toggleEntries.isEmpty()) return

    Spacer(Modifier.height(6.dp))
    if (infoEntries.isNotEmpty()) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            infoEntries.forEach { (label, e) ->
                val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
                val value = localizedEntityStateLabel(e.state)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.dlg_labeled_value_unit, label, value, unit), style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
    if (toggleEntries.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toggleEntries.forEach { (label, e) ->
                FilterChip(
                    selected = e.state == "on",
                    onClick = { viewModel.toggleEntity(e.entity_id) },
                    label = { Text(label) }
                )
            }
        }
    }
}

/** Speed control for the humidifier's linked entity: a fan (percentage slider + preset chips) or a
 *  select / input_select (option chips set via select_option). */
@Composable
private fun HumidifierSpeedControl(fanEntity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val domain = fanEntity.entity_id.substringBefore('.')
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (domain == "fan") {
            fanEntity.fanPercentage?.let { pct ->
                var v by remember(pct) { mutableFloatStateOf(pct.toFloat()) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Speed, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    HKISlider(value = v, onValueChange = { v = it }, onValueChangeFinished = { viewModel.setFanPercentage(fanEntity.entity_id, v.toInt()) }, valueRange = 0f..100f, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.dlg_percentage, v.toInt()), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                }
            }
            SpeedChips(stringResource(R.string.uif_fan_speed), fanEntity.fanPresetModes, fanEntity.fanPresetMode) { viewModel.setFanPresetMode(fanEntity.entity_id, it) }
        } else {
            val options = (fanEntity.attributes?.get("options") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
            SpeedChips(stringResource(R.string.uif_speed), options, fanEntity.state) { viewModel.callService(domain, "select_option", HAServiceCall(entity_id = fanEntity.entity_id, option = it)) }
        }
    }
}

@Composable
private fun SpeedChips(label: String, options: List<String>, current: String?, onSelect: (String) -> Unit) {
    if (options.isEmpty()) return
    val appColors = LocalHKIAppColors.current
    Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == current,
                onClick = { onSelect(option) },
                label = { Text(localizedDeviceModeLabel(option)) }
            )
        }
    }
}

/** Common Home Assistant states used as display text; comparisons continue to use raw tokens. */
@Composable
internal fun localizedEntityStateLabel(state: String): String = when (state.lowercase()) {
    "on" -> stringResource(R.string.uif_state_on)
    "off" -> stringResource(R.string.uif_state_off)
    "open" -> stringResource(R.string.uif_state_open)
    "closed" -> stringResource(R.string.uif_state_closed)
    "locked" -> stringResource(R.string.uif_state_locked)
    "unlocked" -> stringResource(R.string.uif_state_unlocked)
    "home" -> stringResource(R.string.uif_state_home)
    "away", "not_home" -> stringResource(R.string.uif_state_away)
    "idle" -> stringResource(R.string.uif_state_idle)
    "active" -> stringResource(R.string.uif_state_active)
    "cleaning" -> stringResource(R.string.uif_state_cleaning)
    "returning" -> stringResource(R.string.uif_state_returning)
    "paused" -> stringResource(R.string.uif_state_paused)
    "docked" -> stringResource(R.string.uif_state_docked)
    "playing" -> stringResource(R.string.uif_state_playing)
    "buffering" -> stringResource(R.string.uif_state_buffering)
    "standby" -> stringResource(R.string.uif_state_standby)
    "unknown" -> stringResource(R.string.uif_state_unknown)
    "unavailable" -> stringResource(R.string.uif_state_unavailable)
    "error" -> stringResource(R.string.uif_state_error)
    else -> state.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

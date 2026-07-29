package com.jimz011apps.hki7.ui.components

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

/** A distinct icon per humidifier preset mode so the nav-bar tabs are visually distinguishable. */
fun humidifierModeIcon(mode: String): androidx.compose.ui.graphics.vector.ImageVector = when (mode.lowercase()) {
    "normal" -> Icons.Default.WaterDrop
    "eco" -> Icons.Default.Eco
    "away" -> Icons.Default.Luggage
    "boost", "turbo", "max" -> Icons.Default.Bolt
    "comfort" -> Icons.Default.Weekend
    "home" -> Icons.Default.Home
    "sleep", "night" -> Icons.Default.Bedtime
    "auto" -> Icons.Default.AutoMode
    "baby" -> Icons.Default.ChildCare
    "quiet", "silent" -> Icons.Default.VolumeOff
    "continuous" -> Icons.Default.AllInclusive
    "manual" -> Icons.Default.PanTool
    else -> Icons.Default.Tune
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
    fun label(mode: String) = mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    // Humidifier modes are the dialog's nav-bar tabs (like the climate dialog's hvac modes), rather
    // than a modes button + list.
    val modes = entity.humidifierAvailableModes
    val navigationTabs: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> =
        if (modes.size > 1) modes.map { m -> Triple(label(m), humidifierModeIcon(m)) { viewModel.setHumidifierMode(entity.entity_id, m) } } else emptyList()

    val statusText = if (isOn) {
        entity.humidity?.let { "${it.toInt()}% • ON" } ?: "ON"
    } else "OFF"

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
        currentTab = entity.humidifierMode?.let(::label)
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
            text = if (isOn) "$displayValue%" else "Off",
            color = appColors.onSurface,
            style = if (isOn) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge
        )
        if (isOn) {
            currentHumidity?.let { current ->
                Spacer(Modifier.height(4.dp))
                Text("Current ${current.toInt()}%", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
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
                Button(onClick = { viewModel.toggleEntity(entity.entity_id) }) { Text("Turn On") }
            }
        }
        if (isOn) {
            Spacer(Modifier.height(16.dp))
            Text("TARGET ($minH–$maxH%)", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
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
        "tank_level" to "Tank",
        "pm25" to "PM2.5",
        "error" to "Error",
        "bucket_full" to "Bucket",
        "clean_filter" to "Filter",
        "defrost" to "Defrost"
    )
    val infoEntries = infoSlots.mapNotNull { (key, label) -> auxEntities[key]?.let { label to it } }
    val toggleSlots = listOf("ionizer" to "Ionizer", "pump" to "Pump", "sleep" to "Sleep", "beep" to "Beep")
    val toggleEntries = toggleSlots.mapNotNull { (key, label) -> auxEntities[key]?.let { label to it } }
    if (infoEntries.isEmpty() && toggleEntries.isEmpty()) return

    Spacer(Modifier.height(18.dp))
    if (infoEntries.isNotEmpty()) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            infoEntries.forEach { (label, e) ->
                val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
                val value = e.state.replaceFirstChar(Char::uppercase)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("$label: $value$unit", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
    if (toggleEntries.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
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
                    Text("${v.toInt()}%", style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                }
            }
            SpeedChips("Fan speed", fanEntity.fanPresetModes, fanEntity.fanPresetMode) { viewModel.setFanPresetMode(fanEntity.entity_id, it) }
        } else {
            val options = (fanEntity.attributes?.get("options") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
            SpeedChips("Speed", options, fanEntity.state) { viewModel.callService(domain, "select_option", HAServiceCall(entity_id = fanEntity.entity_id, option = it)) }
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
                label = { Text(option.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }) }
            )
        }
    }
}

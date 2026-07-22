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
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

val HumidifierCyan = Color(0xFF00BCD4)

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
    spinIcon: Boolean = false
) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"

    var currentTab by remember(entity.entity_id) { mutableStateOf("Humidity") }
    var showModes by remember(entity.entity_id) { mutableStateOf(false) }
    val tabs = buildList {
        add(Triple("Humidity", Icons.Default.WaterDrop) { currentTab = "Humidity" })
    }
    val navigationTabs = tabs.takeIf { it.size > 1 } ?: emptyList()

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
        spinIcon = spinIcon,
        statusText = statusText,
        tabs = navigationTabs,
        currentTab = currentTab
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            HumidifierContent(
                entity = entity,
                viewModel = viewModel,
                isOn = isOn,
                showModes = showModes,
                onToggleModes = { showModes = it }
            )
        }
    }
}

/**
 * The value title sits above the control, and the target slider / off state / modes list centers
 * as a block together with the label and button below it, within the space left under the title —
 * matching the original dialog layout.
 */
@Composable
private fun HumidifierContent(
    entity: HAEntity,
    viewModel: MainViewModel,
    isOn: Boolean,
    showModes: Boolean,
    onToggleModes: (Boolean) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val minH = entity.minHumidity ?: 0
    val maxH = (entity.maxHumidity ?: 100).coerceAtLeast(minH + 1)

    fun fractionFor(target: Int) = ((target - minH).toFloat() / (maxH - minH)).coerceIn(0f, 1f)

    var sliderValue by remember(entity.entity_id) { mutableFloatStateOf(fractionFor(entity.humidity?.toInt() ?: minH)) }
    LaunchedEffect(entity.humidity) { sliderValue = fractionFor(entity.humidity?.toInt() ?: minH) }
    val displayValue = minH + (sliderValue * (maxH - minH)).toInt()
    val hasModes = entity.humidifierAvailableModes.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (showModes) 32.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                showModes -> "Modes"
                isOn -> "$displayValue%"
                else -> "Off"
            },
            color = appColors.onSurface,
            style = if (showModes || isOn) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge
        )
        if (isOn && !showModes) {
            entity.currentHumidity?.let { current ->
                Spacer(Modifier.height(4.dp))
                Text("Current ${current.toInt()}%", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                showModes -> HumidifierModesList(entity, viewModel)
                isOn -> VerticalSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        viewModel.setHumidifierTarget(entity.entity_id, minH + (sliderValue * (maxH - minH)).toInt())
                    },
                    activeColor = HumidifierCyan
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(64.dp), tint = appColors.onMuted.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.toggleEntity(entity.entity_id) }) { Text("Turn On") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val label = if (showModes) "MODES" else if (isOn) "TARGET ($minH–$maxH%)" else null
        if (label != null) {
            Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
        ModesButton(hasModes, onClick = { onToggleModes(!showModes) }, selected = showModes)
    }
}

@Composable
private fun ModesButton(visible: Boolean, onClick: () -> Unit, selected: Boolean = false) {
    if (!visible) return

    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(if (selected) "Back" else "Modes") },
            leadingIcon = { Icon(if (selected) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun HumidifierModesList(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fadingEdges(scrollState)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        entity.humidifierAvailableModes.forEach { mode ->
            val selected = mode == entity.humidifierMode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setHumidifierMode(entity.entity_id, mode)
                    },
                shape = itemCornerShape(),
                color = if (selected) HumidifierCyan.copy(alpha = 0.22f) else appColors.surface,
                border = BorderStroke(1.dp, if (selected) HumidifierCyan.copy(alpha = 0.7f) else appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = if (selected) HumidifierCyan else appColors.onMuted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HumidifierCyan)
                }
            }
        }
    }
}

package com.example.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors

/** Whether a climate entity has any fan/swing modes worth showing an extra tab for. */
fun HAEntity.hasClimateModes(): Boolean =
    fanModes.isNotEmpty() || swingModes.isNotEmpty() || swingHorizontalModes.isNotEmpty()

@Composable
fun ClimateModesButton(entity: HAEntity, onClick: () -> Unit, selected: Boolean = false) {
    if (!entity.hasClimateModes()) return

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

/**
 * Chip-list content for a climate entity's fan/swing/swing-horizontal modes, mirroring
 * [LightEffectsContent]'s style. Only the groups the entity actually supports are shown.
 *
 * Callers place this inside the same weighted region that otherwise hosts the temperature
 * slider, so the modes toggle button below it always lands in the same spot.
 */
@Composable
fun ClimateModesList(entity: HAEntity, viewModel: MainViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fadingEdges(scrollState)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (entity.fanModes.isNotEmpty()) {
            ClimateModeGroup(
                title = "Fan",
                modes = entity.fanModes,
                activeMode = entity.fanMode,
                onSelect = { mode ->
                    viewModel.setClimateFanMode(entity.entity_id, mode)
                }
            )
        }
        if (entity.swingModes.isNotEmpty()) {
            ClimateModeGroup(
                title = "Swing",
                modes = entity.swingModes,
                activeMode = entity.swingMode,
                onSelect = { mode ->
                    viewModel.setClimateSwingMode(entity.entity_id, mode)
                }
            )
        }
        if (entity.swingHorizontalModes.isNotEmpty()) {
            ClimateModeGroup(
                title = "Swing (horizontal)",
                modes = entity.swingHorizontalModes,
                activeMode = entity.swingHorizontalMode,
                onSelect = { mode ->
                    viewModel.setClimateSwingHorizontalMode(entity.entity_id, mode)
                }
            )
        }
    }
}

@Composable
private fun ClimateModeGroup(
    title: String,
    modes: List<String>,
    activeMode: String?,
    onSelect: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Text(title, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(2.dp))
    modes.forEach { mode ->
        val selected = mode == activeMode
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(mode) },
            shape = RoundedCornerShape(18.dp),
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
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else appColors.onMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
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
    Spacer(Modifier.height(6.dp))
}

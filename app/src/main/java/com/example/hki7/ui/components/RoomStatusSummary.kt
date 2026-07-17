package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SensorOccupied
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.ui.RoomStatusIndicator
import com.example.hki7.ui.RoomStatusRoles
import com.example.hki7.ui.RoomStatusSummary

private val RoomLightAmber = Color(0xFFFFC247)
private val RoomMotionOrange = Color(0xFFFF8A3D)
private val RoomPresencePurple = Color(0xFFB39DDB)
private val RoomSafetyRed = Color(0xFFEF5350)

/**
 * Displays only active room states as compact count-and-icon pills. The compact layout is a
 * three-column grid for room cards; the regular layout uses up to five columns in a page header.
 */
@Composable
internal fun RoomStatusIndicators(
    summary: RoomStatusSummary,
    contentColor: Color,
    compact: Boolean,
    visibleRoles: Set<String>? = null,
    modifier: Modifier = Modifier
) {
    val indicators = summary.indicators
        .asSequence()
        .filter { it.count > 0 }
        .filter { visibleRoles == null || it.role in visibleRoles }
        .sortedBy { statusOrder(it.role) }
        .toList()
    if (indicators.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) 4.dp else 6.dp,
            Alignment.End
        ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
        maxItemsInEachRow = if (compact) 3 else 5
    ) {
        indicators.forEach { indicator ->
            RoomStatusIndicatorPill(
                indicator = indicator,
                contentColor = statusColor(indicator.role, contentColor),
                compact = compact
            )
        }
    }
}

/** Displays the resolved room temperature and humidity as one concise line. */
@Composable
internal fun RoomEnvironmentSummary(
    summary: RoomStatusSummary,
    color: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val text = summary.environmentText?.takeIf { it.isNotBlank() } ?: return
    Text(
        text = text,
        modifier = modifier.semantics { contentDescription = text },
        color = color,
        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RoomStatusIndicatorPill(
    indicator: RoomStatusIndicator,
    contentColor: Color,
    compact: Boolean
) {
    val label = statusLabel(indicator.role, indicator.count)
    val horizontalPadding = if (compact) 4.dp else 6.dp
    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) { contentDescription = label }
            .background(contentColor.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = horizontalPadding, vertical = if (compact) 2.dp else 3.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = indicator.count.toString(),
            color = contentColor,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Icon(
            imageVector = statusIcon(indicator.role),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 15.dp else 18.dp)
        )
    }
}

private fun statusIcon(role: String): ImageVector = when (role) {
    RoomStatusRoles.DOORS -> Icons.Default.DoorFront
    RoomStatusRoles.WINDOWS -> Icons.Default.Window
    RoomStatusRoles.MOTION -> Icons.AutoMirrored.Filled.DirectionsWalk
    RoomStatusRoles.PRESENCE -> Icons.Default.SensorOccupied
    RoomStatusRoles.LIGHTS -> Icons.Default.Lightbulb
    RoomStatusRoles.DEVICES -> Icons.Default.Power
    RoomStatusRoles.SMOKE -> Icons.Default.SmokeFree
    RoomStatusRoles.GAS -> Icons.Default.GasMeter
    RoomStatusRoles.FIRE -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.Power
}

private fun statusColor(role: String, neutralColor: Color): Color = when (role) {
    RoomStatusRoles.LIGHTS -> RoomLightAmber
    RoomStatusRoles.MOTION -> RoomMotionOrange
    RoomStatusRoles.PRESENCE -> RoomPresencePurple
    RoomStatusRoles.SMOKE,
    RoomStatusRoles.GAS,
    RoomStatusRoles.FIRE -> RoomSafetyRed
    else -> neutralColor
}

private fun statusOrder(role: String): Int = when (role) {
    RoomStatusRoles.DOORS -> 0
    RoomStatusRoles.WINDOWS -> 1
    RoomStatusRoles.MOTION -> 2
    RoomStatusRoles.PRESENCE -> 3
    RoomStatusRoles.LIGHTS -> 4
    RoomStatusRoles.DEVICES -> 5
    RoomStatusRoles.SMOKE -> 6
    RoomStatusRoles.GAS -> 7
    RoomStatusRoles.FIRE -> 8
    else -> 9
}

private fun statusLabel(role: String, count: Int): String {
    val singular = when (role) {
        RoomStatusRoles.DOORS -> "open door"
        RoomStatusRoles.WINDOWS -> "open window"
        RoomStatusRoles.MOTION -> "motion sensor active"
        RoomStatusRoles.PRESENCE -> "presence detected"
        RoomStatusRoles.LIGHTS -> "light on"
        RoomStatusRoles.DEVICES -> "device on"
        RoomStatusRoles.SMOKE -> "smoke detected"
        RoomStatusRoles.GAS -> "gas detected"
        RoomStatusRoles.FIRE -> "fire detected"
        else -> "active device"
    }
    val plural = when (role) {
        RoomStatusRoles.DOORS -> "open doors"
        RoomStatusRoles.WINDOWS -> "open windows"
        RoomStatusRoles.MOTION -> "motion sensors active"
        RoomStatusRoles.PRESENCE -> "presence sensors active"
        RoomStatusRoles.LIGHTS -> "lights on"
        RoomStatusRoles.DEVICES -> "devices on"
        RoomStatusRoles.SMOKE -> "smoke detectors active"
        RoomStatusRoles.GAS -> "gas detectors active"
        RoomStatusRoles.FIRE -> "fire detectors active"
        else -> "active devices"
    }
    return "$count ${if (count == 1) singular else plural}"
}

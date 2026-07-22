package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.RoomStatusIndicator
import com.jimz011apps.hki7.ui.RoomStatusRoles
import com.jimz011apps.hki7.ui.RoomStatusSummary
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

private val DarkRoomLightAmber = Color(0xFFFFC247)
private val DarkRoomMotionOrange = Color(0xFFFF8A3D)
private val DarkRoomPresencePurple = Color(0xFFB39DDB)
private val DarkRoomSafetyRed = Color(0xFFFF6B67)
private val LightRoomLightAmber = Color(0xFF795400)
private val LightRoomMotionOrange = Color(0xFF8C3B00)
private val LightRoomPresencePurple = Color(0xFF65458B)
private val LightRoomSafetyRed = Color(0xFFB3261E)

/**
 * Displays only active room states as compact count-and-icon pills. The compact layout is a
 * three-column grid for room cards; the regular layout uses up to five columns in a page header.
 */
@Composable
internal fun RoomStatusIndicators(
    summary: RoomStatusSummary,
    compact: Boolean,
    modifier: Modifier = Modifier,
    visibleRoles: Set<String>? = null
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
                contentColor = statusColor(indicator.role),
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
    val appColors = LocalHKIAppColors.current
    val label = statusLabel(indicator.role, indicator.count)
    val horizontalPadding = if (compact) 6.dp else 8.dp
    val backingColor = appColors.elevated.copy(alpha = 0.90f)
    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) { contentDescription = label }
            .background(backingColor, RoundedCornerShape(50))
            .border(BorderStroke(0.5.dp, contentColor.copy(alpha = 0.72f)), RoundedCornerShape(50))
            .padding(horizontal = horizontalPadding, vertical = if (compact) 3.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
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
            modifier = Modifier.size(if (compact) 16.dp else 19.dp)
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

@Composable
private fun statusColor(role: String): Color {
    val appColors = LocalHKIAppColors.current
    val darkAppearance = appColors.background.luminance() < 0.5f
    return when (role) {
        RoomStatusRoles.LIGHTS -> if (darkAppearance) DarkRoomLightAmber else LightRoomLightAmber
        RoomStatusRoles.MOTION -> if (darkAppearance) DarkRoomMotionOrange else LightRoomMotionOrange
        RoomStatusRoles.PRESENCE -> if (darkAppearance) DarkRoomPresencePurple else LightRoomPresencePurple
        RoomStatusRoles.SMOKE,
        RoomStatusRoles.GAS,
        RoomStatusRoles.FIRE -> if (darkAppearance) DarkRoomSafetyRed else LightRoomSafetyRed
        else -> appColors.onSurface
    }
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

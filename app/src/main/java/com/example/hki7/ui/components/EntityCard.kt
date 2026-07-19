@file:Suppress("SpellCheckingInspection", "GrazieInspection")

package com.example.hki7.ui.components

import com.example.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared circular "remove" badge shown at the top-right corner of editable widgets/buttons
 * (entity tiles, cameras, vacuums, weather widgets, badges, room cards) in edit mode. Callers
 * position it, e.g. `Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)`.
 */
@Composable
fun EditRemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(20.dp)
            .zIndex(2f)
            .background(Color(0xFF3C3C3E), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
    }
}

/** Standard edit-mode cog placed on cards, matching the other configurable widgets. */
@Composable
fun EditSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .zIndex(2f)
            .shadow(5.dp, CircleShape)
            .background(Color.Black.copy(alpha = 0.58f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Card settings",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun RenameCardDialog(currentName: String, defaultName: String, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var value by androidx.compose.runtime.remember(currentName) { androidx.compose.runtime.mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ModernSettingsDialogTitle("Card", "Optional display name override") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSubcategory("Identity", "Leave empty to use the Home Assistant name")
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true,
                    label = { Text("Name") }, placeholder = { Text(defaultName) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value.trim().takeIf { it.isNotEmpty() }) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * One-line status for a room's configured media player: track/artist while playing or paused,
 * otherwise the capitalized state. Null only when no media player is configured.
 */
fun mediaPlayerStatus(entity: HAEntity?): String? {
    entity ?: return null
    val title = entity.mediaTitle
    return if ((entity.state == "playing" || entity.state == "paused") && !title.isNullOrBlank()) {
        val artist = entity.mediaArtist
        val prefix = if (entity.state == "paused") "Paused: " else ""
        if (!artist.isNullOrBlank()) "$prefix$title • $artist" else "$prefix$title"
    } else {
        entity.state.replaceFirstChar { it.uppercase() }
    }
}

fun mediaPlayerStateIcon(entity: HAEntity?): ImageVector? {
    entity ?: return null
    return when (entity.state.lowercase()) {
        "playing" -> Icons.Default.PlayArrow
        "paused" -> Icons.Default.Pause
        else -> Icons.Default.Stop
    }
}

/** Continuous 0-360 rotation while [spin] is true, otherwise a fixed 0. Shared by entity tiles and badges. */
@Composable
fun rememberIconSpinRotation(spin: Boolean): Float {
    if (!spin) return 0f
    val infiniteTransition = rememberInfiniteTransition(label = "iconSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "iconSpinRotation"
    )
    return rotation
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityCard(
    entity: HAEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleClick: () -> Unit = onLongClick,
    displayName: String? = null,
    label: String? = null,
    iconName: String? = null,
    spinIcon: Boolean = false,
    isSquare: Boolean = false,
    cornerRadius: Int = LocalItemCornerRadius.current,
    interactionsEnabled: Boolean = true,
    doorOpen: Boolean = false,
    buttonStyle: String = if (isSquare) "square" else "standard",
    showBrightnessSlider: Boolean = false,
    onBrightnessChange: (Float) -> Unit = {},
    onBrightnessChangeFinished: (Float) -> Unit = {},
    // Needed to resolve the entity's picture when the icon is set to ENTITY_PICTURE_ICON.
    currentUrl: String? = null
) {
    val appColors = LocalHKIAppColors.current
    val isActive = entity.state == "on"
    val isUnavailable = entity.state.equals("unavailable", ignoreCase = true)
    val domain = entity.entity_id.substringBefore(".")
    val isLockDoorOpen = doorOpen && domain == "lock"
    val isLockUnlocked = domain == "lock" && entity.state != "locked" && !isLockDoorOpen
    // Cover state: primary bg when not closed; door colors for icon/text if device_class is door-like
    val isCoverNotClosed = domain == "cover" && !isUnavailable && entity.state != "closed"
    val coverIsDoor = domain == "cover" && isCoverDoorLike(entity)
    // Door-like covers: icon gets state color, bg/text follow normal cover-not-closed logic (primary or elevated)
    val coverDoorIconColor: Color? = if (coverIsDoor && !isUnavailable) coverDoorColor(entity.state) else null
    val name = displayName ?: entity.friendlyName ?: entity.entity_id
    val climateColor = hvacColor(
        entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
            ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
            ?: entity.state
    )
    val isClimateNotOff = domain == "climate" && entity.state.lowercase() != "off"
    val lightColor = lightStateColor(entity)
    val primary = MaterialTheme.colorScheme.primary
    val activeContent = primary.maxContrastForeground()
    val primaryContent = primary.maxContrastForeground()
    val unavailableStateColor = if (primary.isRedShade()) primary else Color(0xFFEF5350)
    val brightnessVisible = showBrightnessSlider && domain == "light" && entity.supportsBrightness
    val brightnessEnabled = brightnessVisible && interactionsEnabled
    val entityBrightness = if (isActive) (entity.brightness ?: 255) / 255f else 0f
    var localBrightness by remember(entity.entity_id) { mutableFloatStateOf(entityBrightness) }
    LaunchedEffect(entity.brightness, entity.state) { localBrightness = entityBrightness }

    val sliderModifier = if (brightnessEnabled) {
        Modifier.pointerInput(entity.entity_id) {
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    localBrightness = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onBrightnessChange(localBrightness)
                },
                onHorizontalDrag = { change, _ ->
                    localBrightness = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onBrightnessChange(localBrightness)
                    change.consume()
                },
                onDragEnd = { onBrightnessChangeFinished(localBrightness) },
                // A parent/system gesture can cancel after optimistic updates have begun. Commit
                // the last visible value so UI state and Home Assistant cannot diverge.
                onDragCancel = { onBrightnessChangeFinished(localBrightness) }
            )
        }
    } else Modifier

    val statusText = run {
        val brightnessPercent = (
            if (brightnessVisible) localBrightness * 100f
            else (entity.brightness ?: 0) / 255f * 100f
        ).toInt().coerceIn(0, 100)
        when {
            isLockDoorOpen -> "Open"
            domain == "light" && entity.supportsBrightness && isActive -> "On - ${brightnessPercent}%"
            domain == "climate" -> {
                val mode = (entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                    ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                    ?: entity.state).split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                val temp = entity.attributes?.get("temperature")?.jsonPrimitive?.content?.toDoubleOrNull()
                if (temp != null && isClimateNotOff) "$mode - ${temp.toInt()}\u00B0C" else mode
            }
            else -> label ?: entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    if (buttonStyle == "tile") {
        val tileActive = isCoverNotClosed || isLockDoorOpen || isLockUnlocked || isActive || isClimateNotOff
        // Brightness tiles use the normal elevated/off surface as their track; the primary/on
        // surface is drawn below as the proportional fill. Other tiles keep their usual base.
        val tileBase = when {
            brightnessVisible                -> appColors.elevated
            isCoverNotClosed                 -> primary
            isLockDoorOpen || isLockUnlocked -> primary
            isActive || isClimateNotOff      -> primary
            else                             -> appColors.elevated
        }
        // Icon reflects the entity's real color — identical logic to the standard/square card.
        val tileIconTint = when {
            coverDoorIconColor != null           -> coverDoorIconColor
            isCoverNotClosed                     -> primaryContent
            isLockDoorOpen                       -> LockRed
            isLockUnlocked                       -> LockOrange
            domain == "lock"                     -> LockGreen
            domain == "climate"                  -> climateColor
            domain == "light" && isActive        -> lightColor ?: Color(0xFFB58E31)
            domain == "fan" && isActive          -> FanBlue
            domain == "humidifier" && isActive   -> HumidifierCyan
            domain == "alarm_control_panel"      -> alarmStateColor(entity.state)
            isActive                             -> activeContent
            isUnavailable                        -> appColors.onMuted
            else                                 -> primary
        }
        val contentColor = if (tileActive) activeContent else appColors.onSurface
        val mutedContent = if (tileActive) activeContent.copy(alpha = 0.72f) else appColors.onMuted
        val tileIconSlug = iconName?.takeUnless { it.isBlank() }
            ?: defaultEntityIconSlug(entity, lockDoorOpen = isLockDoorOpen)
        @Composable
        fun TileForeground(
            mainColor: Color,
            secondaryColor: Color,
            iconBackgroundColor: Color,
            foregroundModifier: Modifier = Modifier
        ) {
            Row(
                modifier = foregroundModifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(34.dp).background(
                        iconBackgroundColor.copy(alpha = if (tileActive) 0.12f else 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (tileIconSlug != null) MdiIcon(tileIconSlug, tint = tileIconTint, size = 18.dp)
                    else Icon(Icons.Default.DeviceUnknown, null, tint = tileIconTint, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.labelLarge, color = mainColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = secondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ChevronRight, null, tint = secondaryColor.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
            }
        }
        Surface(
            shape = RoundedCornerShape(cornerRadius.dp),
            color = tileBase,
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(cornerRadius.dp)).then(
                if (interactionsEnabled) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick, onDoubleClick = onDoubleClick)
                else Modifier
            )
        ) {
            Box(Modifier.fillMaxWidth()) {
                // Depth gradient (two shades of the tile's own color), same style as the card.
                Box(Modifier.matchParentSize().background(surfaceGradient(tileBase)))
                // The exact primary/on color doubles as a full-height progress fill. Keeping it
                // solid guarantees that the selected black/white foreground remains readable at
                // every vertical position, including custom colors near the contrast crossover.
                // The matchParentSize wrapper gives fillMaxHeight a bounded tile height; without it,
                // wrap-content tiles can measure the progress layer at zero pixels high.
                if (brightnessVisible && localBrightness > 0f) {
                    Box(Modifier.matchParentSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth(localBrightness.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(primary)
                        )
                    }
                }
                if (brightnessVisible) {
                    // Render foreground colors appropriate to each side of the split background.
                    // The second copy is drawing-only and clipped at the same progress boundary.
                    TileForeground(appColors.onSurface, appColors.onSurface, appColors.onSurface)
                    if (localBrightness > 0f) {
                        val fraction = localBrightness.coerceIn(0f, 1f)
                        val progressClip = GenericShape { size, _ ->
                            moveTo(0f, 0f)
                            lineTo(size.width * fraction, 0f)
                            lineTo(size.width * fraction, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(progressClip)
                                .clearAndSetSemantics { }
                        ) {
                            TileForeground(activeContent, activeContent, activeContent)
                        }
                    }
                } else {
                    TileForeground(
                        contentColor,
                        mutedContent,
                        if (tileActive) activeContent else tileIconTint
                    )
                }
                if (brightnessEnabled) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .semantics {
                                contentDescription = "$name brightness"
                                stateDescription = "${(localBrightness * 100f).toInt().coerceIn(0, 100)} percent"
                                progressBarRangeInfo = ProgressBarRangeInfo(localBrightness, 0f..1f)
                                setProgress { requested ->
                                    val value = requested.coerceIn(0f, 1f)
                                    localBrightness = value
                                    onBrightnessChange(value)
                                    onBrightnessChangeFinished(value)
                                    true
                                }
                            }
                            .then(sliderModifier)
                    )
                }
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isUnavailable) Modifier.alpha(0.6f) else Modifier)
            .then(if (isSquare) Modifier.aspectRatio(1f) else Modifier.height(110.dp))
            .then(
                if (interactionsEnabled) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onDoubleClick = onDoubleClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCoverNotClosed       -> primary           // cover not closed (incl. door covers) = primary bg
                isLockDoorOpen || isLockUnlocked -> primary
                isActive || isClimateNotOff -> primary
                else -> appColors.elevated
            }
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            // Background-derived depth gradient (two shades of the card's own color, no accent).
            val cardBg = when {
                isCoverNotClosed                 -> primary
                isLockDoorOpen || isLockUnlocked -> primary
                isActive || isClimateNotOff      -> primary
                else                             -> appColors.elevated
            }
            Box(Modifier.matchParentSize().background(surfaceGradient(cardBg)))
            if (brightnessVisible && localBrightness > 0f) {
                Box(Modifier.fillMaxWidth(localBrightness).fillMaxHeight().background(Color.White.copy(alpha = 0.18f)))
            }
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            val iconTint = when {
                coverDoorIconColor != null  -> coverDoorIconColor   // door cover: state color on icon only
                isCoverNotClosed            -> primaryContent        // open cover (non-door): readable on primary bg
                isLockDoorOpen              -> LockRed
                isLockUnlocked              -> LockOrange
                domain == "lock"            -> LockGreen
                domain == "climate"         -> climateColor
                domain == "light" && isActive -> lightColor ?: Color(0xFFB58E31)
                domain == "fan" && isActive -> FanBlue
                domain == "humidifier" && isActive -> HumidifierCyan
                domain == "alarm_control_panel" -> alarmStateColor(entity.state)
                isActive                    -> activeContent
                isUnavailable               -> appColors.onMuted
                else                        -> primary
            }
            // User icon overrides; otherwise fall back to HA-provided/custom defaults and
            // device-class defaults that mirror Home Assistant's icon choices.
            val defaultSlug = defaultEntityIconSlug(
                entity,
                lockDoorOpen = isLockDoorOpen,
            )
            val effectiveSlug = iconName?.takeUnless { it.isBlank() } ?: defaultSlug
            val spinRotation = rememberIconSpinRotation(spinIcon && !isUnavailable && entity.state.lowercase() != "off")
            val spinModifier = Modifier.rotate(spinRotation)
            // "Use entity picture": render the HA picture when available, else fall back to the icon.
            val pictureUrl = if (effectiveSlug == ENTITY_PICTURE_ICON && !currentUrl.isNullOrBlank())
                resolveEntityPictureUrl(entity, currentUrl) else null
            if (pictureUrl != null) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape).then(spinModifier)
                )
            } else {
                val slugForIcon = if (effectiveSlug == ENTITY_PICTURE_ICON) defaultSlug else effectiveSlug
                if (slugForIcon != null) {
                    MdiIcon(name = slugForIcon, tint = iconTint, modifier = spinModifier)
                } else {
                    Icon(
                        imageVector = Icons.Default.DeviceUnknown,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = spinModifier
                    )
                }
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        isCoverNotClosed -> primaryContent
                        isLockDoorOpen || isLockUnlocked -> primaryContent
                        isActive || isClimateNotOff -> activeContent
                        else            -> appColors.onSurface
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (domain == "camera" && entity.state.equals("recording", ignoreCase = true)) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(Color(0xFFEF5350), CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isCoverNotClosed -> primaryContent.copy(alpha = 0.75f)
                            isLockDoorOpen  -> primaryContent.copy(alpha = 0.75f)
                            isLockUnlocked  -> primaryContent.copy(alpha = 0.75f)
                            isUnavailable   -> unavailableStateColor
                            isActive || isClimateNotOff -> activeContent.copy(alpha = 0.68f)
                            else            -> appColors.onMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            }
            if (brightnessEnabled) Box(Modifier.matchParentSize().then(sliderModifier))
        }
    }
}

private fun Color.isRedShade(): Boolean =
    red > green * 1.25f && red > blue * 1.25f

/** Pick the WCAG-higher-contrast opaque foreground for an arbitrary theme color. */
private fun Color.maxContrastForeground(): Color {
    val relativeLuminance = luminance()
    val whiteContrast = 1.05f / (relativeLuminance + 0.05f)
    val blackContrast = (relativeLuminance + 0.05f) / 0.05f
    return if (whiteContrast > blackContrast) Color.White else Color.Black
}

fun lightStateColor(entity: HAEntity): Color? {
    entity.rgbColor
        ?.takeIf { entity.supportsColor && it.size >= 3 }
        ?.let { rgb ->
            return Color(
                android.graphics.Color.rgb(
                    rgb[0].coerceIn(0, 255),
                    rgb[1].coerceIn(0, 255),
                    rgb[2].coerceIn(0, 255)
                )
            )
        }

    val kelvin = entity.colorTempKelvin?.takeIf { entity.supportsColorTemp } ?: return null
    val min = (entity.minKelvin ?: 2000).coerceAtLeast(1000)
    val max = (entity.maxKelvin ?: 6500).coerceAtLeast(min + 1)
    val t = ((kelvin.coerceIn(min, max) - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    return lerpColor(Color(0xFFFFB35C), Color(0xFFBFD9FF), t)
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = 1f
    )
}

/** Subtle depth gradient built purely from a surface's own color (two shades of it, no accent). */
fun surfaceGradient(base: Color): Brush = Brush.verticalGradient(
    listOf(
        lerpColor(base, Color.White, 0.06f),
        base,
        lerpColor(base, Color.Black, 0.10f)
    )
)

fun coverDoorColor(state: String): Color = when (state.lowercase()) {
    "closed"  -> Color(0xFF4CAF50)   // green = closed/secure
    "open"    -> Color(0xFFEF5350)   // red = fully open
    "opening" -> Color(0xFFFF8C00)   // orange = moving
    "closing" -> Color(0xFFFF8C00)
    "stopped" -> Color(0xFFFF8C00)
    else      -> Color(0xFFFF8C00)
}

val CoverPurple = Color(0xFF8B5CF6)

/** The accent a cover dialog's icon AND slider should share: door-state color for door-like
 * covers (garage/door/gate), the standard cover purple otherwise. */
fun coverAccentColor(entity: HAEntity): Color =
    if (isCoverDoorLike(entity)) coverDoorColor(entity.state) else CoverPurple

/** Domain/state icon tint shared by entity buttons, badges, and dialog custom buttons. */
@Composable
fun entityStateIconColor(entity: HAEntity, inactive: Color = LocalHKIAppColors.current.onMuted): Color {
    val state = entity.state.lowercase()
    val primary = MaterialTheme.colorScheme.primary
    return when (entity.entity_id.substringBefore('.')) {
        "light" -> if (state == "on") lightStateColor(entity) ?: Color(0xFFB58E31) else inactive
        "climate" -> if (state == "off") inactive else hvacColor(
            entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                ?: state
        )
        "lock" -> when (state) {
            "locked" -> LockGreen
            "unlocked" -> LockOrange
            "open" -> LockRed
            else -> inactive
        }
        "cover" -> if (isCoverDoorLike(entity) && state != "unavailable") coverDoorColor(state)
            else if (state != "closed" && state != "unavailable") primary else inactive
        "vacuum" -> when (state) {
            "cleaning" -> Color(0xFF66BB6A)
            "returning", "paused" -> Color(0xFFFFB300)
            "error" -> Color(0xFFE53935)
            else -> inactive
        }
        "fan" -> if (state == "on") FanBlue else inactive
        "humidifier" -> if (state == "on") HumidifierCyan else inactive
        "alarm_control_panel" -> alarmStateColor(state)
        else -> if (state in listOf("on", "playing", "home", "open", "unlocked")) primary else inactive
    }
}

/**
 * State-aware default MDI icon slug for domains that ship custom defaults (lock, cover).
 * Returns null for other domains, which keep their Material fallback icon.
 *
 * A user-configured icon always overrides this; clearing it back to "None/Auto" falls back
 * here. Cover treatment (garage/door/curtain/blind/etc.) is read straight from the entity's
 * own `device_class` attribute — Home Assistant already reports this, so there's nothing to
 * configure manually.
 */
fun defaultEntityIconSlug(
    entity: HAEntity,
    lockDoorOpen: Boolean = false,
): String? {
    val state = entity.state.lowercase()
    entity.icon?.takeIf { it.isNotBlank() }?.let { return it.removePrefix("mdi:") }
    return when (entity.entity_id.substringBefore(".")) {
        "alarm_control_panel" -> "shield-home"
        "automation" -> "robot"
        "button", "input_button" -> "gesture-tap-button"
        "camera" -> "cctv"
        "climate" -> "thermostat"
        "device_tracker" -> "map-marker"
        "fan" -> "fan"
        "humidifier" -> "air-humidifier"
        "input_boolean" -> if (state == "on") "toggle-switch-variant" else "toggle-switch-variant-off"
        "input_datetime" -> "calendar-clock"
        "input_number", "number" -> "counter"
        "input_select", "select" -> "format-list-bulleted"
        "light" -> lightIconSlug(entity, state)
        "lock" -> when {
            lockDoorOpen                       -> "door-open"
            state == "locked"                  -> "lock"
            state == "unavailable" || state == "unknown" -> "lock"
            else                               -> "lock-open-alert"
        }
        "media_player" -> "speaker"
        "person" -> "account"
        "remote" -> "remote"
        "scene" -> "palette"
        "script" -> "script-text"
        "sensor" -> sensorDeviceClassIconSlug(entity.deviceClass)
        "binary_sensor" -> binarySensorDeviceClassIconSlug(entity.deviceClass, state)
        "sun" -> if (state == "below_horizon") "weather-night" else "weather-sunny"
        "switch" -> when (entity.deviceClass) {
            "outlet" -> "power-plug"
            "switch" -> if (state == "on") "toggle-switch-variant" else "toggle-switch-variant-off"
            else -> "power-socket"
        }
        "update" -> "package-up"
        "vacuum" -> "robot-vacuum"
        "weather" -> "weather-partly-cloudy"
        "cover" -> {
            val closed = state == "closed"
            when (entity.deviceClass) {
                "garage"  -> if (closed) "garage-variant-lock" else "garage-open-variant"
                "door"    -> if (closed) "door-closed-lock" else "door-open"
                "gate"    -> if (closed) "gate" else "gate-open"
                "curtain" -> if (closed) "curtains-closed" else "curtains"
                "blind"   -> if (closed) "blinds-horizontal-closed" else "blinds-horizontal"
                "shutter" -> if (closed) "window-shutter" else "window-shutter-open"
                "awning"  -> "awning-outline"
                else      -> if (closed) "roller-shade-closed" else "roller-shade"
            }
        }
        else -> "power"
    }
}

private fun sensorDeviceClassIconSlug(deviceClass: String?): String = when (deviceClass) {
    "apparent_power", "energy", "power", "reactive_power" -> "lightning-bolt"
    "aqi" -> "air-filter"
    "atmospheric_pressure", "pressure" -> "gauge"
    "battery" -> "battery"
    "carbon_dioxide" -> "molecule-co2"
    "carbon_monoxide" -> "molecule-co"
    "current" -> "current-ac"
    "data_rate" -> "speedometer"
    "data_size" -> "database"
    "date" -> "calendar"
    "distance" -> "arrow-expand-horizontal"
    "duration" -> "timer"
    "enum" -> "format-list-bulleted"
    "frequency", "voltage" -> "sine-wave"
    "gas" -> "meter-gas"
    "humidity" -> "water-percent"
    "illuminance" -> "brightness-5"
    "monetary" -> "cash"
    "nitrogen_dioxide", "nitrogen_monoxide", "nitrous_oxide", "ozone",
    "pm1", "pm10", "pm25", "sulphur_dioxide", "volatile_organic_compounds" -> "molecule"
    "power_factor" -> "angle-acute"
    "signal_strength" -> "wifi"
    "sound_pressure" -> "volume-high"
    "speed" -> "speedometer"
    "temperature" -> "thermometer"
    "timestamp" -> "clock"
    "volume", "volume_storage", "water" -> "water"
    "weight" -> "scale-bathroom"
    "wind_speed" -> "weather-windy"
    else -> "eye"
}

private fun lightIconSlug(entity: HAEntity, state: String): String {
    val name = listOfNotNull(entity.friendlyName, entity.entity_id.substringAfter("."))
        .joinToString(" ")
        .lowercase()
        .replace('_', ' ')
        .replace('-', ' ')

    if (entity.childEntityIds.isNotEmpty() || " group" in name || " all " in " $name ") return "lightbulb-group"

    return when {
        "strip" in name || "led" in name || "wled" in name -> "led-strip-variant"
        "string" in name || "christmas" in name || "fairy" in name -> "string-lights"
        "ceiling" in name || "plafond" in name -> "ceiling-light"
        "recessed" in name || "downlight" in name || "down light" in name -> "light-recessed"
        "track" in name -> "track-light"
        "spot" in name -> "spotlight"
        "flood" in name -> "light-flood-down"
        "chandelier" in name -> "chandelier"
        "sconce" in name -> "wall-sconce"
        "wall" in name -> "wall-sconce-flat"
        "vanity" in name || "mirror" in name -> "vanity-light"
        "floor lamp" in name || "standing lamp" in name -> "floor-lamp"
        "desk" in name || "table" in name -> "desk-lamp"
        "lamp" in name -> "lamp"
        "outdoor" in name || "outside" in name || "porch" in name || "garden" in name -> "outdoor-lamp"
        "coach" in name -> "coach-lamp"
        "dome" in name -> "dome-light"
        "bulkhead" in name -> "bulkhead-light"
        "lava" in name -> "lava-lamp"
        state == "on" -> "lightbulb-on"
        else -> "lightbulb"
    }
}

private fun binarySensorDeviceClassIconSlug(deviceClass: String?, state: String): String {
    val active = state == "on"
    return when (deviceClass) {
        "battery" -> if (active) "battery-outline" else "battery"
        "battery_charging" -> if (active) "battery-charging" else "battery"
        "carbon_monoxide", "smoke" -> "smoke-detector"
        "cold" -> "snowflake"
        "connectivity" -> if (active) "check-network" else "close-network"
        "door", "opening" -> if (active) "door-open" else "door-closed"
        "garage_door" -> if (active) "garage-open" else "garage"
        "gas" -> "gas-cylinder"
        "heat" -> "fire"
        "light" -> "brightness-5"
        "lock" -> if (active) "lock-open" else "lock"
        "moisture" -> "water-alert"
        "motion" -> "motion-sensor"
        "moving", "running" -> "run"
        "occupancy" -> "home-account"
        "plug", "power" -> "power-plug"
        "presence" -> "account"
        "problem", "safety", "tamper" -> if (active) "alert-circle" else "shield-check"
        "sound" -> "volume-high"
        "update" -> "package-up"
        "vibration" -> "vibrate"
        "window" -> if (active) "window-open" else "window-closed"
        else -> if (active) "checkbox-marked-circle" else "checkbox-blank-circle-outline"
    }
}

private val DOOR_LIKE_COVER_CLASSES = setOf("garage", "door", "gate")

/** Covers whose device_class reads as a door-like opening (green closed / red open / orange moving). */
fun isCoverDoorLike(entity: HAEntity): Boolean = entity.deviceClass in DOOR_LIKE_COVER_CLASSES

fun hvacColor(mode: String?): Color {
    return when (mode?.lowercase()) {
        "heat", "heating" -> Color(0xFFFF8C00)
        "cool", "cooling" -> Color(0xFF1E90FF)
        "heat_cool" -> Color(0xFF9C27B0)
        "auto" -> Color(0xFF4CAF50)
        "dry" -> Color(0xFFFFC107)
        "fan_only", "fan" -> Color(0xFF9E9E9E)
        "idle" -> Color(0xFF78909C)
        "off" -> Color(0xFF424242)
        else -> Color(0xFF4CAF50)
    }
}

fun hvacGradient(mode: String?): Brush {
    val color = hvacColor(mode)
    return Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color))
}

fun climateModeLabel(mode: String): String = when (mode.lowercase()) {
    "off" -> "Off"
    "heat" -> "Heat"
    "cool" -> "Cool"
    "heat_cool" -> "Heat/Cool"
    "auto" -> "Auto"
    "dry" -> "Dry"
    "fan_only" -> "Fan"
    else -> mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}

fun climateModeIcon(mode: String): ImageVector = when (mode.lowercase()) {
    "off" -> Icons.Default.PowerSettingsNew
    "heat" -> Icons.Default.WbSunny
    "cool" -> Icons.Default.AcUnit
    "heat_cool" -> Icons.Default.SyncAlt
    "auto" -> Icons.Default.AutoMode
    "dry" -> Icons.Default.WaterDrop
    "fan_only" -> Icons.Default.Air
    else -> Icons.Default.Thermostat
}

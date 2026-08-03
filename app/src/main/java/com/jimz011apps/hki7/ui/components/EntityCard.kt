@file:Suppress("SpellCheckingInspection", "GrazieInspection")

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

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
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.isActionItemId
import com.jimz011apps.hki7.ui.localizedHvacModeLabel
import com.jimz011apps.hki7.ui.localizedStateLabel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared circular "remove" badge shown at the top-right corner of editable widgets/buttons.
 * Callers align it to [Alignment.TopEnd]; this component owns the consistent outward offset.
 * It must be placed in an unclipped overlay container, as a sibling of rounded card content.
 */
@Composable
fun EditRemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(20.dp)
            .zIndex(20f)
            .background(Color(0xFF3C3C3E), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ui_remove_e963907), tint = Color.White, modifier = Modifier.size(12.dp))
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
            contentDescription = stringResource(R.string.ui_card_settings_1a3b62a),
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
        title = {
            ModernSettingsDialogTitle(
                stringResource(R.string.ui_card_4d4ce73),
                stringResource(R.string.core_optional_display_name)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_leave_empty_to_use_the_home_assistant_name_f769e92))
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true,
                    label = { Text(stringResource(R.string.ui_name_709a232)) }, placeholder = { Text(defaultName) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value.trim().takeIf { it.isNotEmpty() }) }) { Text(stringResource(R.string.ui_save_efc007a)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.ui_reset_44c57ab)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
            }
        }
    )
}

/**
 * One-line status for a room's configured media player: track/artist while playing or paused,
 * otherwise the capitalized state. Null only when no media player is configured.
 */
@Composable
fun mediaPlayerStatus(entity: HAEntity?): String? {
    entity ?: return null
    val title = entity.mediaTitle
    return if ((entity.state == "playing" || entity.state == "paused") && !title.isNullOrBlank()) {
        val artist = entity.mediaArtist
        val track = if (!artist.isNullOrBlank()) "$title • $artist" else title
        if (entity.state == "paused") {
            stringResource(R.string.core_media_paused_track, track)
        } else {
            track
        }
    } else {
        entity.localizedStateLabel()
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

/** Attribute keys that are noise in a state/attribute picker (internal, or already shown elsewhere). */
val HIDDEN_ATTRIBUTE_KEYS = setOf(
    "friendly_name", "icon", "entity_picture", "supported_features",
    "supported_color_modes", "attribution"
)

/** Attribute keys worth offering as a button/badge secondary value, in a stable display order. */
fun selectableEntityAttributes(entity: HAEntity?): List<String> =
    entity?.attributes?.keys.orEmpty().filter { it !in HIDDEN_ATTRIBUTE_KEYS }.sorted()

/** The display value of an entity attribute, or null when the attribute is absent or blank. */
fun entityAttributeDisplay(entity: HAEntity, attribute: String): String? {
    val element = entity.attributes?.get(attribute) ?: return null
    val raw = runCatching { element.jsonPrimitive.contentOrNull }.getOrNull() ?: element.toString()
    return raw.trim().ifBlank { null }
}

/** Common unit suffixes offered for a button/badge attribute value. Empty string means "no unit". */
val COMMON_STATE_UNITS = listOf(
    "", "°C", "°F", "%", "W", "kW", "Wh", "kWh", "V", "A", "Hz",
    "lx", "ppm", "µg/m³", "hPa", "km/h", "m/s", "dB", "L", "m³"
)

/** Appends a unit suffix to a value, spacing everything except percent and degree units. */
fun appendUnit(value: String, unit: String?): String {
    val u = unit?.trim().orEmpty()
    if (u.isEmpty()) return value
    val noSpace = u == "%" || u.startsWith("°")
    return if (noSpace) "$value$u" else "$value $u"
}

/** Icon an action button falls back to until the user picks one. */
const val ACTION_ITEM_DEFAULT_ICON = "gesture-tap-button"

/** The empty (transparent) button: it reserves exactly the space a real button of the same style
 *  would take, so the buttons around it land where the user wants them. It draws nothing outside
 *  edit mode; in edit mode a dashed outline makes it selectable and removable. */
@Composable
fun SpacerButtonCard(
    isSquare: Boolean,
    buttonStyle: String,
    cornerRadius: Int,
    showOutline: Boolean,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val sizeModifier = when {
        buttonStyle == "tile" -> Modifier.height(58.dp)
        isSquare -> Modifier.aspectRatio(1f)
        else -> Modifier.height(110.dp)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(sizeModifier)
            .then(
                if (showOutline) {
                    Modifier.border(1.dp, appColors.onMuted.copy(alpha = 0.45f), RoundedCornerShape(cornerRadius.dp))
                } else Modifier
            )
    )
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
    stateAttribute: String? = null,
    stateUnit: String? = null,
    stateAsTimer: Boolean = false,
    // Gated by the caller from an optional machine-state entity: false suppresses the countdown even
    // if the completion timestamp is in the future (the appliance isn't actually running).
    timerMachineRunning: Boolean = true,
    iconName: String? = null,
    iconAnimation: String = "auto",
    isSquare: Boolean = false,
    cornerRadius: Int = LocalItemCornerRadius.current,
    /** Drops the name and state so only the icon shows, centered on the button. */
    iconOnly: Boolean = false,
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
    // Timer mode: the value (attribute if set, else state) is a completion timestamp. While it's in
    // the future the entity reads as active with a live countdown; once it passes it reads as OFF.
    val timerText = if (stateAsTimer && timerMachineRunning) rememberCountdownText(stateAttribute?.let { entityAttributeDisplay(entity, it) } ?: entity.state) else null
    val timerRunning = stateAsTimer && timerMachineRunning && timerText != null
    val isActive = if (stateAsTimer) timerRunning else entity.state == "on"
    val isUnavailable = entity.state.equals("unavailable", ignoreCase = true)
    val domain = entity.entity_id.substringBefore(".")
    val isLockDoorOpen = doorOpen && domain == "lock"
    val isLockUnlocked = domain == "lock" && entity.state != "locked" && !isLockDoorOpen
    // Cover state: primary bg when not closed; door colors for icon/text if device_class is door-like
    val isCoverNotClosed = domain == "cover" && !isUnavailable && entity.state != "closed"
    val coverIsDoor = domain == "cover" && isCoverDoorLike(entity)
    // Door-like covers: icon gets state color, bg/text follow normal cover-not-closed logic (primary or elevated)
    val coverDoorIconColor: Color? = if (coverIsDoor && !isUnavailable) coverDoorColor(entity.state) else null
    // An action button has no entity behind it: no state line, and a generic name/icon until the
    // user names it.
    val isActionItem = isActionItemId(entity.entity_id)
    val name = displayName ?: entity.friendlyName
        ?: if (isActionItem) stringResource(R.string.action_button) else entity.entity_id
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
    val brightnessContentDescription = stringResource(R.string.ui_brightness_222d6d5, name)
    val entityBrightness = if (isActive) (entity.brightness ?: 255) / 255f else 0f
    var localBrightness by remember(entity.entity_id) { mutableFloatStateOf(entityBrightness) }
    val brightnessStateDescription = stringResource(
        R.string.core_percent_value,
        (localBrightness * 100f).toInt().coerceIn(0, 100)
    )
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

    // An explicit attribute (when configured and present) replaces the state on the secondary line,
    // with an optional unit suffix.
    val attributeText = stateAttribute
        ?.let { entityAttributeDisplay(entity, it) }
        ?.let { appendUnit(it, stateUnit) }
    val statusText = when {
        // Timer mode: countdown while running, plain stringResource(R.string.ui_off_e3de5ab) once it reaches zero.
        timerRunning -> timerText!!
        stateAsTimer -> stringResource(R.string.ui_off_e3de5ab)
        else -> attributeText
    } ?: run {
        val brightnessPercent = (
            if (brightnessVisible) localBrightness * 100f
            else (entity.brightness ?: 0) / 255f * 100f
        ).toInt().coerceIn(0, 100)
        when {
            isLockDoorOpen -> stringResource(R.string.ui_open_cf9b770)
            domain == "light" && entity.supportsBrightness && isActive -> stringResource(R.string.ui_on_12469c7, brightnessPercent)
            domain == "climate" -> {
                val rawMode = (entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                    ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                    ?: entity.state)
                val mode = localizedHvacModeLabel(rawMode)
                val temp = entity.attributes?.get("temperature")?.jsonPrimitive?.content?.toDoubleOrNull()
                if (temp != null && isClimateNotOff) stringResource(R.string.ui_c_3d3c16f, mode, temp.toInt()) else mode
            }
            else -> entity.localizedStateLabel()
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
            ?: if (isActionItem) ACTION_ITEM_DEFAULT_ICON
            else defaultEntityIconSlug(entity, lockDoorOpen = isLockDoorOpen)
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
                horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(10.dp)
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
                // Icon-only keeps the tile's height and just centers the glyph in it.
                if (!iconOnly) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.labelLarge, color = mainColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!isActionItem) Text(statusText, style = MaterialTheme.typography.bodySmall, color = secondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = secondaryColor.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                }
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
                                contentDescription = brightnessContentDescription
                                stateDescription = brightnessStateDescription
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
            val rawIconTint = when {
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
            val iconTint = semanticColorForBackground(rawIconTint, cardBg)
            // User icon overrides; otherwise fall back to HA-provided/custom defaults and
            // device-class defaults that mirror Home Assistant's icon choices.
            val defaultSlug = if (isActionItem) ACTION_ITEM_DEFAULT_ICON else defaultEntityIconSlug(
                entity,
                lockDoorOpen = isLockDoorOpen,
            )
            val effectiveSlug = iconName?.takeUnless { it.isBlank() } ?: defaultSlug
            // Live motion while the device is active (glow/spin/pulse), gated on the user setting
            // and the per-icon override.
            val iconEffect = iconEffectFor(entity, LocalIconAnimationsEnabled.current, iconAnimation)
                .forIconSlug(effectiveSlug)
            // "Use entity picture": render the HA picture when available, else fall back to the icon.
            val pictureUrl = if (effectiveSlug == ENTITY_PICTURE_ICON && !currentUrl.isNullOrBlank())
                resolveEntityPictureUrl(entity, currentUrl) else null
            // Icon-only buttons have the whole face to themselves, so the glyph grows to match.
            val glyphSize = if (iconOnly) 40.dp else 24.dp
            @Composable
            fun CardIcon() {
                WithIconEffect(entity, iconEffect, glowColor = iconTint) { fx ->
                    if (pictureUrl != null) {
                        AsyncImage(
                            model = pictureUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(if (iconOnly) 48.dp else 32.dp).clip(CircleShape).then(fx)
                        )
                    } else {
                        val slugForIcon = if (effectiveSlug == ENTITY_PICTURE_ICON) defaultSlug else effectiveSlug
                        if (slugForIcon != null) {
                            MdiIcon(name = slugForIcon, tint = iconTint, size = glyphSize, modifier = fx)
                        } else {
                            Icon(
                                imageVector = Icons.Default.DeviceUnknown,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(glyphSize).then(fx)
                            )
                        }
                    }
                }
            }
            if (iconOnly) {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    CardIcon()
                }
            } else {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            CardIcon()
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
                    if (!isActionItem) Text(
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
        "climate" -> when {
            // Cooling units that cannot heat (mini-split / window AC with a cool mode but no heat)
            // read as air conditioners. Dual heat+cool thermostats and heat pumps stay on the
            // thermostat icon. "cool" is required: "dry" alone is a (de)humidifier, not an AC.
            "cool" in entity.hvacModes &&
                entity.hvacModes.none { it == "heat" || it == "heat_cool" } -> "air-conditioner"
            // Humidity-only climate devices (a dehumidifier/humidifier exposed as climate: dry mode,
            // no cool and no heat) get the humidifier icon rather than a thermostat or AC icon.
            "dry" in entity.hvacModes && "cool" !in entity.hvacModes &&
                entity.hvacModes.none { it == "heat" || it == "heat_cool" } -> "air-humidifier"
            else -> "thermostat"
        }
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
            state == "locked"                  -> "door-closed-lock"
            state == "unavailable" || state == "unknown" -> "door-closed-lock"
            else                               -> "door-open"
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

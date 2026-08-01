package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Live "the device is doing something" motion for entity icons. Effects are purely programmatic —
 * they animate whatever glyph the user has chosen, in any icon pack — so there are no per-icon
 * animation assets. Everything is gated on [LocalIconAnimationsEnabled] and on the entity actually
 * being active, so nothing moves for an off/idle device (and disabled entities create no animation
 * at all, keeping it cheap).
 */
enum class IconEffect { NONE, GLOW, SPIN, PULSE }

/** Whether entity-icon animations are switched on (Settings › Appearance › Theme). Off by default
 *  so previews/tests are static; the real value is provided at the app root from preferences. */
val LocalIconAnimationsEnabled = compositionLocalOf { false }

private val INACTIVE = setOf("unavailable", "unknown", "none", "")

/** True when the entity is in a state worth animating — the "on / doing something" states per domain. */
fun isEntityActive(entity: HAEntity): Boolean {
    val s = entity.state.trim().lowercase()
    if (s in INACTIVE) return false
    return when (entity.entity_id.substringBefore('.')) {
        "light", "switch", "input_boolean", "fan", "siren", "humidifier" -> s == "on"
        "media_player" -> s == "playing" || s == "on" || s == "buffering"
        "vacuum" -> s == "cleaning" || s == "returning"
        "climate", "water_heater" -> s != "off"
        "lock" -> s == "unlocked"
        "cover", "valve" -> s == "opening" || s == "closing"
        "binary_sensor" -> s == "on"
        "automation", "script" -> s == "on"
        "alarm_control_panel" -> s.startsWith("armed") || s == "triggered"
        // Presence, sensors, weather, etc. are informational — never animated.
        "person", "device_tracker", "sensor", "weather", "sun" -> false
        else -> s == "on"
    }
}

/** Editable groups (id → label) shown in Settings › Appearance › Icons for per-type defaults. */
val IconEffectGroups: List<Pair<String, String>> = listOf(
    "lights" to "Lights & switches",
    "fans" to "Fans & vacuums",
    "media" to "Media players",
    "climate" to "Climate & humidity",
    "alerts" to "Sensors & alarms",
    "other" to "Everything else",
)

/** Built-in default effect (string id) per group, used until the user overrides it. */
val DefaultIconEffectByGroup: Map<String, String> = mapOf(
    "lights" to "glow",
    "fans" to "spin",
    "media" to "pulse",
    "climate" to "pulse",
    "alerts" to "pulse",
    "other" to "pulse",
)

/** The user's chosen default effect per group, mirrored from preferences (see MainActivity). */
object IconEffectDefaults {
    @Volatile var byGroup: Map<String, String> = emptyMap()
}

private fun groupForDomain(domain: String): String = when (domain) {
    "light", "switch", "input_boolean" -> "lights"
    "fan", "vacuum" -> "fans"
    "media_player" -> "media"
    "climate", "humidifier", "water_heater" -> "climate"
    "binary_sensor", "siren", "alarm_control_panel" -> "alerts"
    else -> "other"
}

private fun effectFromId(id: String): IconEffect = when (id) {
    "glow" -> IconEffect.GLOW
    "spin" -> IconEffect.SPIN
    "pulse" -> IconEffect.PULSE
    else -> IconEffect.NONE
}

/** The domain-appropriate effect for an active entity, honouring the user's per-group defaults. */
private fun domainEffect(entity: HAEntity): IconEffect {
    val group = groupForDomain(entity.entity_id.substringBefore('.'))
    val id = IconEffectDefaults.byGroup[group] ?: DefaultIconEffectByGroup.getValue(group)
    return effectFromId(id)
}

/**
 * The effect for an entity. [override] is a per-icon setting: "auto" follows the global [enabled]
 * flag and the domain default; "off" never animates; "glow"/"spin"/"pulse" force that effect. Any
 * effect is still suppressed while the device is idle.
 */
fun iconEffectFor(entity: HAEntity, enabled: Boolean, override: String = "auto"): IconEffect {
    if (override == "off") return IconEffect.NONE
    if (!isEntityActive(entity)) return IconEffect.NONE
    return when (override) {
        "glow" -> IconEffect.GLOW
        "spin" -> IconEffect.SPIN
        "pulse" -> IconEffect.PULSE
        else -> if (enabled) domainEffect(entity) else IconEffect.NONE
    }
}

/** Icons that depict a static appliance rather than a rotating blade. A spin animation on these
 * reads as wrong (an air purifier or humidifier glyph tumbling end over end), so spin is softened to
 * the calmer pulse used elsewhere for climate/humidity devices. */
private val NON_SPINNING_ICON_SLUGS = setOf("air-purifier", "air-humidifier", "air-conditioner")

/** Downgrades a SPIN effect to PULSE for [iconSlug] glyphs that should not rotate; every other effect
 * and every other icon is returned unchanged. Applied where the final icon slug is known, so it works
 * regardless of how the device (a purifier fan, say) was classified. */
fun IconEffect.forIconSlug(iconSlug: String?): IconEffect =
    if (this == IconEffect.SPIN && iconSlug in NON_SPINNING_ICON_SLUGS) IconEffect.PULSE else this

/** Milliseconds per full rotation for a spinning entity — faster for higher fan speeds. */
private fun spinPeriodMillis(entity: HAEntity?): Int {
    return when (entity?.entity_id?.substringBefore('.')) {
        "fan" -> {
            val pct = entity?.attributes?.get("percentage")?.jsonPrimitive?.intOrNull?.coerceIn(0, 100)
            if (pct != null) (2400 - (pct / 100f) * 1700).toInt() else 1400
        }
        "vacuum" -> 1600
        else -> 1500
    }
}

/**
 * Renders [content] with the given [effect]. [content] receives a [Modifier] it must apply to the
 * icon it draws, so the same wrapper works for a glyph, an entity picture, or a fallback vector.
 * [glowColor] tints the light "bloom" (typically the icon's own colour).
 */
@Composable
fun WithIconEffect(
    entity: HAEntity?,
    effect: IconEffect,
    glowColor: Color,
    content: @Composable (Modifier) -> Unit,
) {
    when (effect) {
        IconEffect.NONE -> content(Modifier)

        IconEffect.SPIN -> {
            val t = rememberInfiniteTransition(label = stringResource(R.string.ui_spin_a801301))
            val rotation by t.animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(spinPeriodMillis(entity), easing = LinearEasing), RepeatMode.Restart),
                label = stringResource(R.string.ui_rotation_acaf322),
            )
            content(Modifier.graphicsLayer { rotationZ = rotation })
        }

        IconEffect.PULSE -> {
            // A "heartbeat": two quick beats then a rest, so it reads as alive rather than a dull throb.
            val t = rememberInfiniteTransition(label = stringResource(R.string.ui_pulse_8439fc4))
            val scale by t.animateFloat(
                1f, 1f,
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 1600
                        1f at 0
                        1.16f at 130 using FastOutSlowInEasing
                        1f at 320 using FastOutSlowInEasing
                        1.11f at 460 using FastOutSlowInEasing
                        1f at 640 using FastOutSlowInEasing
                        1f at 1600
                    },
                    RepeatMode.Restart,
                ),
                label = stringResource(R.string.ui_pulsescale_939f5c2),
            )
            content(Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
        }

        IconEffect.GLOW -> {
            // A breathing light halo: a soft, wide bloom that swells and fades, with the glyph itself
            // subtly brightening — not a hard pulsing disc.
            val t = rememberInfiniteTransition(label = stringResource(R.string.ui_glow_6ae5a62))
            val phase by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = stringResource(R.string.ui_glowphase_748e6d6),
            )
            val glowAlpha = 0.12f + 0.5f * phase
            val scale = 1f + 0.06f * phase
            content(
                Modifier
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = glowAlpha),
                                    glowColor.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.maxDimension * (0.85f + 0.35f * phase),
                            ),
                        )
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

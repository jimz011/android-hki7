package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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

/** The effect for an entity, or [IconEffect.NONE] when animations are off or the device is idle. */
fun iconEffectFor(entity: HAEntity, enabled: Boolean): IconEffect {
    if (!enabled || !isEntityActive(entity)) return IconEffect.NONE
    return when (entity.entity_id.substringBefore('.')) {
        "light", "switch", "input_boolean" -> IconEffect.GLOW
        "fan", "vacuum" -> IconEffect.SPIN
        else -> IconEffect.PULSE  // media playing, climate heating, motion, sirens, moving covers, …
    }
}

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
            val t = rememberInfiniteTransition(label = "spin")
            val rotation by t.animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(spinPeriodMillis(entity), easing = LinearEasing), RepeatMode.Restart),
                label = "rotation",
            )
            content(Modifier.graphicsLayer { rotationZ = rotation })
        }

        IconEffect.PULSE -> {
            val t = rememberInfiniteTransition(label = "pulse")
            val scale by t.animateFloat(
                1f, 1.08f,
                infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
                label = "pulseScale",
            )
            val alpha by t.animateFloat(
                0.65f, 1f,
                infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
                label = "pulseAlpha",
            )
            content(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha })
        }

        IconEffect.GLOW -> {
            val t = rememberInfiniteTransition(label = "glow")
            val scale by t.animateFloat(
                1f, 1.05f,
                infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
                label = "glowScale",
            )
            val glowAlpha by t.animateFloat(
                0.10f, 0.42f,
                infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
                label = "glowAlpha",
            )
            content(
                Modifier
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                                center = center,
                                radius = size.maxDimension * 0.75f,
                            ),
                        )
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

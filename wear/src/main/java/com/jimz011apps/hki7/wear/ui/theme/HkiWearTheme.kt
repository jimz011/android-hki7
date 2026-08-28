package com.jimz011apps.hki7.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * HKI 7's palette on Wear OS.
 *
 * Carried over from the phone app's dark theme rather than adopting Wear's defaults, so the watch
 * reads as the same product. That the phone theme is already black-based is convenient here for a
 * reason it is not on a phone: watch displays are OLED and spend most of their life dimmed, so
 * true black is what keeps the screen cheap to light.
 *
 * The phone's theme is user-configurable (accent colour, corner radius). The watch deliberately
 * is not — there is no room for a settings screen to configure it, and the phone's choice is not
 * synced. If that changes, the accent is the one value worth sending over.
 */
object HkiWearColors {
    val Background = Color.Black
    val Surface = Color(0xFF1C1C1E)
    val Elevated = Color(0xFF2C2C2E)
    val OnSurface = Color.White
    val OnMuted = Color(0xFF9E9E9E)
    val Accent = Color(0xFFD0BCFF)
    val Danger = Color(0xFFFF6B6B)

    /** Matches the phone's default item corner radius, the most recognisable part of HKI's shape. */
    val CornerRadius = 20.dp
}

private val HkiColorScheme = ColorScheme(
    primary = HkiWearColors.Accent,
    onPrimary = Color.Black,
    surfaceContainer = HkiWearColors.Surface,
    surfaceContainerHigh = HkiWearColors.Elevated,
    onSurface = HkiWearColors.OnSurface,
    onSurfaceVariant = HkiWearColors.OnMuted,
    background = HkiWearColors.Background,
    onBackground = HkiWearColors.OnSurface,
    error = HkiWearColors.Danger,
)

val LocalHkiWearColors = staticCompositionLocalOf { HkiWearColors }

@Composable
fun HkiWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HkiColorScheme, content = content)
}

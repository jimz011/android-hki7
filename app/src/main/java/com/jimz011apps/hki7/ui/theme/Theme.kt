@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.graphics.toColorInt
import com.jimz011apps.hki7.R

/** Readable bundled families. Bundling avoids OEM aliases silently resolving to the same font. */
private val NunitoFontFamily = FontFamily(Font(R.font.nunito))
private val ComfortaaFontFamily = FontFamily(Font(R.font.comfortaa))
private val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk))
private val BreeSerifFontFamily = FontFamily(Font(R.font.bree_serif))
private val PatrickHandFontFamily = FontFamily(Font(R.font.patrick_hand))
private val AtkinsonHyperlegibleFontFamily = FontFamily(Font(R.font.atkinson_hyperlegible_next))

val AppFontFamilyOptions = listOf(
    "default" to "Default",
    "sans" to "Sans Serif",
    "serif" to "Serif",
    "monospace" to "Monospace",
    "cursive" to "Cursive",
    "nunito" to "Nunito",
    "comfortaa" to "Comfortaa",
    "space_grotesk" to "Space Grotesk",
    "bree_serif" to "Bree Serif",
    "patrick_hand" to "Patrick Hand",
    "atkinson" to "Atkinson Hyperlegible"
)

fun appFontFamily(key: String): FontFamily? = when (key) {
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    "nunito" -> NunitoFontFamily
    "comfortaa" -> ComfortaaFontFamily
    "space_grotesk" -> SpaceGroteskFontFamily
    "bree_serif" -> BreeSerifFontFamily
    "patrick_hand" -> PatrickHandFontFamily
    "atkinson" -> AtkinsonHyperlegibleFontFamily
    else -> null
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF8F5F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDE7EA),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

data class HKIAppColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val onSurface: Color,
    val onMuted: Color,
    val subtleSurface: Color,
    val headerFallbackStart: Color,
    val headerFallbackEnd: Color,
    val accent: Color
)

val LocalHKIAppColors = staticCompositionLocalOf {
    HKIAppColors(
        background = Color.Black,
        surface = Color(0xFF1C1C1E),
        elevated = Color(0xFF2C2C2E),
        onSurface = Color.White,
        onMuted = Color.Gray,
        subtleSurface = Color.White.copy(alpha = 0.08f),
        headerFallbackStart = Color(0xFF5D1029),
        headerFallbackEnd = Color.Black,
        accent = Purple80
    )
}

@Composable
fun HKI7Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String = "system",
    themeMode: String = "system",
    systemLightThemeColor: String = "auto",
    systemDarkThemeColor: String = "auto",
    fontScale: Float = 1f,
    fontWeightAdjust: Int = 0,
    fontFamily: String = "default",
    itemCornerRadius: Int = 20,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val systemOverride = if (effectiveDarkTheme) systemDarkThemeColor else systemLightThemeColor
    val colorScheme = when {
        themeColor != "system" -> themedColorScheme(themeColor, effectiveDarkTheme)
        systemOverride != "auto" -> themedColorScheme(systemOverride, effectiveDarkTheme)
        dynamicColor -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val appColors = appColors(colorScheme, effectiveDarkTheme)
    val systemBarColor = appColors.background
    val useDarkSystemBarIcons = systemBarColor.luminance() > 0.5f
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = systemBarColor.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.primary.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarDividerColor = systemBarColor.toArgb()
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkSystemBarIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkSystemBarIcons
        }
    }

    val typography = remember(fontScale, fontWeightAdjust, fontFamily) {
        adjustedTypography(Typography, fontScale, fontWeightAdjust, fontFamily)
    }
    val shapes = remember(itemCornerRadius) {
        val itemShape = RoundedCornerShape(itemCornerRadius.dp)
        Shapes(
            extraSmall = itemShape,
            small = itemShape,
            medium = itemShape,
            large = itemShape,
            extraLarge = itemShape
        )
    }
    CompositionLocalProvider(LocalHKIAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

/** Applies the user's font preferences (size multiplier, weight offset, family) to every style. */
private fun adjustedTypography(base: Typography, scale: Float, weightAdjust: Int, familyKey: String): Typography {
    val family = appFontFamily(familyKey)
    if (scale == 1f && weightAdjust == 0 && family == null) return base

    fun TextStyle.adjust(): TextStyle {
        val newWeight = if (weightAdjust == 0) fontWeight else {
            FontWeight(((fontWeight ?: FontWeight.Normal).weight + weightAdjust).coerceIn(100, 900))
        }
        return copy(
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize * scale else fontSize,
            lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight * scale else lineHeight,
            fontWeight = newWeight,
            fontFamily = family ?: fontFamily
        )
    }
    return Typography(
        displayLarge = base.displayLarge.adjust(),
        displayMedium = base.displayMedium.adjust(),
        displaySmall = base.displaySmall.adjust(),
        headlineLarge = base.headlineLarge.adjust(),
        headlineMedium = base.headlineMedium.adjust(),
        headlineSmall = base.headlineSmall.adjust(),
        titleLarge = base.titleLarge.adjust(),
        titleMedium = base.titleMedium.adjust(),
        titleSmall = base.titleSmall.adjust(),
        bodyLarge = base.bodyLarge.adjust(),
        bodyMedium = base.bodyMedium.adjust(),
        bodySmall = base.bodySmall.adjust(),
        labelLarge = base.labelLarge.adjust(),
        labelMedium = base.labelMedium.adjust(),
        labelSmall = base.labelSmall.adjust()
    )
}

private fun themedColorScheme(theme: String, darkTheme: Boolean) = when {
    theme.startsWith("custom:", ignoreCase = true) -> {
        val custom = parseThemeColor(theme.substringAfter("custom:")) ?: Color(0xFF9B5353)
        appScheme(custom, darkTheme)
    }
    theme == "rose" -> appScheme(Color(0xFF9B5353), darkTheme)
    theme == "green" -> appScheme(Color(0xFF4CAF50), darkTheme)
    theme == "blue" -> appScheme(Color(0xFF1E90FF), darkTheme)
    theme == "amber" -> appScheme(Color(0xFFFFB300), darkTheme)
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
}

private fun parseThemeColor(value: String): Color? {
    val normalized = value.trim().let { if (it.startsWith("#")) it else "#$it" }
    return runCatching { Color(normalized.toColorInt()) }.getOrNull()
}

private fun appScheme(primary: Color, darkTheme: Boolean) = if (darkTheme) {
    val onPrimary = readableOn(primary)
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = primary.copy(alpha = 0.85f),
        tertiary = primary.copy(alpha = 0.7f),
        primaryContainer = primary.copy(alpha = 0.35f),
        onPrimaryContainer = onPrimary,
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        background = Color.Black,
        onSurface = Color.White,
        onBackground = Color.White,
        secondaryContainer = primary.copy(alpha = 0.24f)
    )
} else {
    val onPrimary = readableOn(primary)
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = primary.copy(alpha = 0.85f),
        tertiary = primary.copy(alpha = 0.7f),
        primaryContainer = primary.copy(alpha = if (primary.luminance() < 0.35f) 0.28f else 0.18f),
        onPrimaryContainer = if (primary.luminance() < 0.35f) Color.White else Color(0xFF1C1B1F),
        surface = Color.White,
        surfaceVariant = Color(0xFFEDE7EA),
        background = Color(0xFFF8F5F7),
        onSurface = Color(0xFF1C1B1F),
        onBackground = Color(0xFF1C1B1F),
        secondaryContainer = primary.copy(alpha = 0.16f)
    )
}

private fun readableOn(color: Color): Color = if (color.luminance() < 0.45f) Color.White else Color(0xFF111111)

private fun appColors(colorScheme: androidx.compose.material3.ColorScheme, darkTheme: Boolean): HKIAppColors {
    return if (darkTheme) {
        HKIAppColors(
            background = Color.Black,
            surface = Color(0xFF1C1C1E),
            elevated = Color(0xFF2C2C2E),
            onSurface = Color.White,
            onMuted = Color(0xFF9A9A9A),
            subtleSurface = Color.White.copy(alpha = 0.08f),
            headerFallbackStart = colorScheme.primary.copy(alpha = 0.45f),
            headerFallbackEnd = Color.Black,
            accent = colorScheme.primary
        )
    } else {
        HKIAppColors(
            background = Color(0xFFF8F5F7),
            surface = Color.White,
            elevated = Color(0xFFEDE7EA),
            onSurface = Color(0xFF1C1B1F),
            onMuted = Color(0xFF6F676B),
            subtleSurface = colorScheme.primary.copy(alpha = 0.08f),
            headerFallbackStart = colorScheme.primaryContainer,
            headerFallbackEnd = Color(0xFFF8F5F7),
            accent = colorScheme.primary
        )
    }
}

package com.jimz011apps.hki7.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG contrast ratio after the foreground alpha is composited over [background]. */
fun colorContrastRatio(foreground: Color, background: Color): Float {
    val opaqueBackground = background.copy(alpha = 1f)
    val renderedForeground = foreground.compositeOver(opaqueBackground)
    val lighter = maxOf(renderedForeground.luminance(), opaqueBackground.luminance())
    val darker = minOf(renderedForeground.luminance(), opaqueBackground.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Keeps a semantic color's hue recognizable while making it readable on [background].
 *
 * Icons and non-text UI graphics target the WCAG 3:1 contrast ratio. Mixing toward black creates
 * a shade and mixing toward white creates a tint, so heating stays red, cooling stays blue, etc.
 * The closest passing tint or shade wins; an already-readable color is returned unchanged.
 */
fun semanticColorForBackground(
    semanticColor: Color,
    background: Color,
    minimumContrast: Float = 3f,
): Color {
    if (colorContrastRatio(semanticColor, background) >= minimumContrast) return semanticColor

    val opaqueSemantic = semanticColor.copy(alpha = 1f)
    for (step in 1..19) {
        val fraction = step / 20f
        val shade = lerp(opaqueSemantic, Color.Black, fraction).copy(alpha = semanticColor.alpha)
        val tint = lerp(opaqueSemantic, Color.White, fraction).copy(alpha = semanticColor.alpha)
        val shadeContrast = colorContrastRatio(shade, background)
        val tintContrast = colorContrastRatio(tint, background)
        val shadePasses = shadeContrast >= minimumContrast
        val tintPasses = tintContrast >= minimumContrast
        if (shadePasses || tintPasses) {
            return when {
                shadePasses && tintPasses -> if (shadeContrast >= tintContrast) shade else tint
                shadePasses -> shade
                else -> tint
            }
        }
    }

    val shade = lerp(opaqueSemantic, Color.Black, 0.95f).copy(alpha = semanticColor.alpha)
    val tint = lerp(opaqueSemantic, Color.White, 0.95f).copy(alpha = semanticColor.alpha)
    return if (colorContrastRatio(shade, background) >= colorContrastRatio(tint, background)) shade else tint
}

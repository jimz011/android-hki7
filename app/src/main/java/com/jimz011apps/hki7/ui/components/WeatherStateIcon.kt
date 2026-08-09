@file:Suppress("SpellCheckingInspection", "GrazieInspection")

package com.jimz011apps.hki7.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.ui.utils.MdiIcon

/**
 * Where a piece of weather artwork is being drawn. Each has its own animation switch because they
 * cost wildly different amounts: [FORECAST] is a dozen Lottie compositions side by side and is the
 * one that ever made anything stutter, while [PILL] is a single 20dp icon.
 */
enum class WeatherAnimationSurface { PILL, DIALOG, FORECAST, WIDGET }

/** Which weather surfaces animate. Everything on by default so previews and tests look like the
 *  real thing; the actual values are provided at the app root from preferences. */
data class WeatherAnimationSettings(
    val pill: Boolean = true,
    val dialog: Boolean = true,
    val forecast: Boolean = true,
    val widget: Boolean = true,
) {
    fun isEnabled(surface: WeatherAnimationSurface): Boolean = when (surface) {
        WeatherAnimationSurface.PILL -> pill
        WeatherAnimationSurface.DIALOG -> dialog
        WeatherAnimationSurface.FORECAST -> forecast
        WeatherAnimationSurface.WIDGET -> widget
    }
}

val LocalWeatherAnimations = compositionLocalOf { WeatherAnimationSettings() }

/**
 * Whether the weather cards are currently being drawn inside the weather dialog or inside a
 * dashboard weather widget.
 *
 * `WeatherMainCard`, `ForecastCard` and `HourlyForecastCard` are shared by both, so a call site
 * cannot tell which it is; the container declares it here instead. Only [PILL] and the forecast
 * strips are context-free — a strip is a strip wherever it appears, and it is the expensive one
 * either way.
 */
val LocalWeatherHostSurface = compositionLocalOf { WeatherAnimationSurface.DIALOG }

/**
 * The four weather-animation switches, as one reusable block.
 *
 * Rendered in Settings › Appearance › Icons and again in the header pill's own settings sheet.
 * Both read and write the same preferences, so this is one setting shown twice rather than two
 * settings that can disagree — someone looking for it at the pill finds it there, and someone
 * looking for it among the other animation controls finds it there too.
 */
@Composable
fun WeatherAnimationSwitches(
    settings: WeatherAnimationSettings,
    onChange: (WeatherAnimationSurface, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WeatherAnimationSurface.entries.forEach { surface ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(weatherSurfaceLabel(surface)),
                        color = LocalHKIAppColors.current.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(weatherSurfaceDescription(surface)),
                        color = LocalHKIAppColors.current.onMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.isEnabled(surface),
                    onCheckedChange = { onChange(surface, it) }
                )
            }
        }
    }
}

private fun weatherSurfaceLabel(surface: WeatherAnimationSurface): Int = when (surface) {
    WeatherAnimationSurface.PILL -> R.string.weather_animate_pill
    WeatherAnimationSurface.DIALOG -> R.string.weather_animate_dialog
    WeatherAnimationSurface.FORECAST -> R.string.weather_animate_forecast
    WeatherAnimationSurface.WIDGET -> R.string.weather_animate_widget
}

private fun weatherSurfaceDescription(surface: WeatherAnimationSurface): Int = when (surface) {
    WeatherAnimationSurface.PILL -> R.string.weather_animate_pill_description
    WeatherAnimationSurface.DIALOG -> R.string.weather_animate_dialog_description
    WeatherAnimationSurface.FORECAST -> R.string.weather_animate_forecast_description
    WeatherAnimationSurface.WIDGET -> R.string.weather_animate_widget_description
}

/** Writes one surface's switch. Lives here rather than on [PreferencesManager] so the data layer
 *  keeps no dependency on a UI enum; both settings screens call this instead of each re-deriving
 *  which setter belongs to which surface. */
suspend fun PreferencesManager.saveWeatherAnimation(
    surface: WeatherAnimationSurface,
    enabled: Boolean,
) = when (surface) {
    WeatherAnimationSurface.PILL -> saveWeatherAnimatePill(enabled)
    WeatherAnimationSurface.DIALOG -> saveWeatherAnimateDialog(enabled)
    WeatherAnimationSurface.FORECAST -> saveWeatherAnimateForecast(enabled)
    WeatherAnimationSurface.WIDGET -> saveWeatherAnimateWidget(enabled)
}

/**
 * Animated, full-color artwork for a Home Assistant weather condition.
 *
 * The bundled animations are the fill-style Meteocons assets. Unknown conditions and a failed or
 * still-loading animation use a state-colored MDI glyph, so an icon is always available without a
 * network dependency. Set [isDaytime] when the caller has sun/forecast-time context; otherwise
 * partly-cloudy conditions use their daytime artwork and `clear-night` remains explicitly nocturnal.
 *
 * Whether the artwork animates is decided per [surface] from the user's settings, so a lively
 * header pill can sit alongside a still forecast strip or the reverse. With a surface switched
 * off it gets the colored MDI fallback instead — which is also what a caller passing [animate]
 * `false` gets for its own reasons.
 */
@Composable
fun WeatherStateIcon(
    state: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null,
    isDaytime: Boolean? = null,
    animate: Boolean = true,
    loop: Boolean = true,
    surface: WeatherAnimationSurface = WeatherAnimationSurface.DIALOG,
    fallbackTint: Color = weatherStateColor(state)
) {
    // A grid of small Lottie icons (the forecast and hourly strips) each parsing and playing its
    // own composition is what used to make this dialog stutter mid-scroll, so animation here is
    // not free — but the cost is wildly uneven between surfaces, which is why it is a per-surface
    // choice rather than one switch or the old blanket "nothing under 40dp" rule. The remaining
    // floor is only the size at which the artwork stops being resolvable at all.
    val animationsEnabled = LocalWeatherAnimations.current.isEnabled(surface)
    val tooSmallToAnimate = size < 16.dp
    val animationResource = if (!animationsEnabled || tooSmallToAnimate) {
        null
    } else {
        weatherAnimationResource(state, isDaytime)
    }
    val descriptionModifier = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics { this.contentDescription = contentDescription }
    }

    Box(
        modifier = modifier.then(descriptionModifier).size(size),
        contentAlignment = Alignment.Center
    ) {
        if (animationResource == null) {
            WeatherStateFallbackIcon(state, isDaytime, fallbackTint, size)
        } else {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(animationResource)
            )
            if (composition == null) {
                WeatherStateFallbackIcon(state, isDaytime, fallbackTint, size)
            } else {
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.fillMaxSize(),
                    isPlaying = animate,
                    iterations = if (loop) LottieConstants.IterateForever else 1,
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun WeatherStateFallbackIcon(
    state: String?,
    isDaytime: Boolean?,
    tint: Color,
    size: Dp
) {
    MdiIcon(
        name = weatherStateMdiIcon(state, isDaytime),
        modifier = Modifier.fillMaxSize(),
        tint = tint,
        size = size,
        contentDescription = null
    )
}

/** Resource for the canonical HA condition, or null when only the colored fallback is available. */
@RawRes
fun weatherAnimationResource(state: String?, isDaytime: Boolean? = null): Int? =
    when (normaliseWeatherState(state)) {
        "sunny", "clear", "clear-day", "day" -> R.raw.weather_clear_day
        "clear-night", "night" -> R.raw.weather_clear_night
        "cloudy", "overcast" -> R.raw.weather_cloudy
        "exceptional", "extreme" -> R.raw.weather_exceptional
        "fog", "foggy", "haze", "mist", "misty" -> R.raw.weather_fog
        "hail", "ice-pellets" -> R.raw.weather_hail
        "lightning", "thunder", "thunderstorm", "thunderstorms" ->
            R.raw.weather_lightning
        "lightning-rainy", "thunderstorm-rain", "thunderstorms-rain" ->
            R.raw.weather_lightning_rainy
        "partlycloudy", "partly-cloudy", "partly-cloudy-day", "partly-cloudy-night" ->
            if (isDaytime == false || normaliseWeatherState(state) == "partly-cloudy-night") {
                R.raw.weather_partly_cloudy_night
            } else {
                R.raw.weather_partly_cloudy_day
            }
        "pouring", "heavy-rain", "extreme-rain" -> R.raw.weather_pouring
        "rainy", "rain", "drizzle", "showers" -> R.raw.weather_rainy
        "snowy", "snow" -> R.raw.weather_snowy
        "snowy-rainy", "sleet", "wintry-mix" -> R.raw.weather_snowy_rainy
        "windy", "wind", "windy-variant", "wind-alert", "gale" -> R.raw.weather_windy
        else -> null
    }

/** MDI fallback covering every canonical Home Assistant weather state. */
fun weatherStateMdiIcon(state: String?, isDaytime: Boolean? = null): String =
    when (normaliseWeatherState(state)) {
        "sunny", "clear", "clear-day", "day" -> "weather-sunny"
        "clear-night", "night" -> "weather-night"
        "cloudy", "overcast" -> "weather-cloudy"
        "exceptional", "extreme" -> "weather-cloudy-alert"
        "fog", "foggy", "haze", "mist", "misty" -> "weather-fog"
        "hail", "ice-pellets" -> "weather-hail"
        "lightning", "thunder", "thunderstorm", "thunderstorms" -> "weather-lightning"
        "lightning-rainy", "thunderstorm-rain", "thunderstorms-rain" ->
            "weather-lightning-rainy"
        "partlycloudy", "partly-cloudy", "partly-cloudy-day", "partly-cloudy-night" ->
            if (isDaytime == false || normaliseWeatherState(state) == "partly-cloudy-night") {
                "weather-night-partly-cloudy"
            } else {
                "weather-partly-cloudy"
            }
        "pouring", "heavy-rain", "extreme-rain" -> "weather-pouring"
        "rainy", "rain", "drizzle", "showers" -> "weather-rainy"
        "snowy", "snow" -> "weather-snowy"
        "snowy-rainy", "sleet", "wintry-mix" -> "weather-snowy-rainy"
        "windy", "wind" -> "weather-windy"
        "windy-variant", "wind-alert", "gale" -> "weather-windy-variant"
        else -> "weather-cloudy-alert"
    }

/** Stable state colors for static fallbacks and surfaces that deliberately disable animation. */
fun weatherStateColor(state: String?): Color = when (normaliseWeatherState(state)) {
    "sunny", "clear", "clear-day", "day" -> Color(0xFFF6C744)
    "clear-night", "night" -> Color(0xFF8C9DCE)
    "cloudy", "overcast" -> Color(0xFF90A4AE)
    "exceptional", "extreme" -> Color(0xFFEF5350)
    "fog", "foggy", "haze", "mist", "misty" -> Color(0xFFB0BEC5)
    "hail", "ice-pellets" -> Color(0xFF80DEEA)
    "lightning", "thunder", "thunderstorm", "thunderstorms" -> Color(0xFFFFCA4B)
    "lightning-rainy", "thunderstorm-rain", "thunderstorms-rain" -> Color(0xFF6574C4)
    "partlycloudy", "partly-cloudy", "partly-cloudy-day", "partly-cloudy-night" ->
        Color(0xFF64B5F6)
    "pouring", "heavy-rain", "extreme-rain" -> Color(0xFF4656B8)
    "rainy", "rain", "drizzle", "showers" -> Color(0xFF42A5F5)
    "snowy", "snow" -> Color(0xFF81D4FA)
    "snowy-rainy", "sleet", "wintry-mix" -> Color(0xFF4FC3F7)
    "windy", "wind", "windy-variant", "wind-alert", "gale" -> Color(0xFF4DB6AC)
    else -> Color(0xFF78909C)
}

private fun normaliseWeatherState(state: String?): String = state
    ?.trim()
    ?.lowercase()
    ?.replace('_', '-')
    ?.replace(' ', '-')
    ?.replace(Regex("-+"), "-")
    .orEmpty()

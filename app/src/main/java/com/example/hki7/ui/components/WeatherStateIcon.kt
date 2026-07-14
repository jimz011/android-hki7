package com.example.hki7.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.hki7.R
import com.example.hki7.ui.utils.MdiIcon

/**
 * Animated, full-colour artwork for a Home Assistant weather condition.
 *
 * The bundled animations are the fill-style Meteocons assets. Unknown conditions and a failed or
 * still-loading animation use a state-coloured MDI glyph, so an icon is always available without a
 * network dependency. Set [isDaytime] when the caller has sun/forecast-time context; otherwise
 * partly-cloudy conditions use their daytime artwork and `clear-night` remains explicitly nocturnal.
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
    fallbackTint: Color = weatherStateColor(state)
) {
    val animationResource = weatherAnimationResource(state, isDaytime)
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

/** Resource for the canonical HA condition, or null when only the coloured fallback is available. */
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

/** Stable state colours for static fallbacks and surfaces that deliberately disable animation. */
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

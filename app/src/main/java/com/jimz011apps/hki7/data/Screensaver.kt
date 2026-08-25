package com.jimz011apps.hki7.data

import kotlinx.serialization.Serializable
import java.util.UUID

const val SCREENSAVER_LAYOUT_CLOCK_PANEL = "clock_panel"
const val SCREENSAVER_LAYOUT_CLOCK_ONLY = "clock_only"
const val DEFAULT_SCREENSAVER_TIMEOUT_SECONDS = 120
const val MIN_SCREENSAVER_TIMEOUT_SECONDS = 15
const val MAX_SCREENSAVER_TIMEOUT_SECONDS = 900
const val DEFAULT_SCREENSAVER_BACKGROUND = "picsum"

@Serializable
data class ScreensaverAction(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val icon: String = "home",
    val entityId: String = "",
    /** toggle, open, or close */
    val service: String = "toggle",
)

@Serializable
data class ScreensaverSettings(
    val enabled: Boolean = false,
    val timeoutSeconds: Int = DEFAULT_SCREENSAVER_TIMEOUT_SECONDS,
    val layout: String = SCREENSAVER_LAYOUT_CLOCK_PANEL,
    val weatherEntityId: String = "",
    val temperatureEntityId: String = "",
    val humidityEntityId: String = "",
    val calendarEntityIds: List<String> = emptyList(),
    val showWeather: Boolean = true,
    val showIndoor: Boolean = true,
    val showCalendar: Boolean = true,
    val showActions: Boolean = true,
    val actions: List<ScreensaverAction> = emptyList(),
    val background: String = DEFAULT_SCREENSAVER_BACKGROUND,
    val backgroundRotationMinutes: Int = 15,
)

@Serializable
data class ScreensaverStore(
    val byInstanceId: Map<String, ScreensaverSettings> = emptyMap(),
)

fun ScreensaverSettings.clampedTimeoutSeconds(): Int =
    timeoutSeconds.coerceIn(MIN_SCREENSAVER_TIMEOUT_SECONDS, MAX_SCREENSAVER_TIMEOUT_SECONDS)

fun ScreensaverSettings.isClockPanel(): Boolean = layout != SCREENSAVER_LAYOUT_CLOCK_ONLY

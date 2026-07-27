package com.jimz011apps.hki7.data

import java.time.LocalDateTime

/**
 * Whether a button/card inside a multi-item widget is visible at [now].
 *
 * - [HKIButtonConfig.hidden] hides it outright (until unhidden).
 * - A [HKIButtonConfig.visibilityStart]/[HKIButtonConfig.visibilityEnd] window (ISO-8601 local
 *   date-time) is interpreted by [HKIButtonConfig.visibilityRangeMode]: "show" means visible only
 *   within the window (hidden outside), "hide" means hidden within the window (visible outside).
 * - An open-ended window (only a start, or only an end) is treated as a half-open range.
 * - Unparseable bounds are ignored so a bad value never permanently hides an item.
 */
fun isButtonVisibleAt(config: HKIButtonConfig, now: LocalDateTime): Boolean {
    if (config.hidden) return false
    val start = config.visibilityStart?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val end = config.visibilityEnd?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    if (start == null && end == null) return true
    val inRange = (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end))
    return if (config.visibilityRangeMode == "hide") !inRange else inRange
}

/** True when [config] has any visibility restriction (a plain hide or a scheduled window). */
fun HKIButtonConfig.hasVisibilityRule(): Boolean =
    hidden || !visibilityStart.isNullOrBlank() || !visibilityEnd.isNullOrBlank()

fun isButtonVisibleNow(config: HKIButtonConfig): Boolean = isButtonVisibleAt(config, LocalDateTime.now())

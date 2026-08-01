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
 * - [resolveEntityState] resolves the current state of [HKIButtonConfig.visibilityConditionEntityId]
 *   when a condition is configured; pass null to skip condition evaluation entirely (treated as met).
 */
fun isButtonVisibleAt(
    config: HKIButtonConfig,
    now: LocalDateTime,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean = isVisibleAt(
    config.hidden, config.visibilityStart, config.visibilityEnd, config.visibilityRangeMode, config.visibilityRecurrence, now,
    config.visibilityConditionEntityId, config.visibilityConditionState, config.visibilityConditionNegate, resolveEntityState
)

/** Raw visibility rule shared by buttons, badges, and widgets (same hide/schedule/condition semantics). */
fun isVisibleAt(
    hidden: Boolean,
    visibilityStart: String?,
    visibilityEnd: String?,
    visibilityRangeMode: String,
    visibilityRecurrence: String,
    now: LocalDateTime,
    conditionEntityId: String? = null,
    conditionState: String? = null,
    conditionNegate: Boolean = false,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean {
    if (hidden) return false
    val start = visibilityStart?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val end = visibilityEnd?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val scheduleVisible = if (start == null && end == null) {
        true
    } else {
        val inRange = when (visibilityRecurrence.ifBlank { "none" }) {
            "none" -> (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end))
            else -> {
                // Compare only the part of the timestamp that repeats (time of day, day of week/month, or
                // month-day), so a window can recur. Ranges may wrap the cycle boundary (e.g. 24 Dec–2 Jan).
                val ordinal = recurringOrdinal(visibilityRecurrence)
                val s = start?.let(ordinal)
                val e = end?.let(ordinal)
                val n = ordinal(now)
                when {
                    s != null && e != null -> if (s <= e) n in s..e else (n >= s || n <= e)
                    s != null -> n >= s
                    e != null -> n <= e
                    else -> true
                }
            }
        }
        if (visibilityRangeMode == "hide") !inRange else inRange
    }
    if (!scheduleVisible) return false

    // Conditional visibility (like a Home Assistant conditional card): only evaluated when both a
    // condition entity is configured and the caller supplied a way to resolve live entity state.
    if (conditionEntityId.isNullOrBlank() || resolveEntityState == null) return true
    val currentState = resolveEntityState(conditionEntityId)
    val matches = currentState != null && currentState.equals(conditionState, ignoreCase = true)
    return if (conditionNegate) !matches else matches
}

/** A sortable position within the repeat cycle for [recurrence]; higher fields drop out so the
 * window recurs (yearly ignores year, monthly ignores year+month, and so on). */
private fun recurringOrdinal(recurrence: String): (LocalDateTime) -> Long = when (recurrence) {
    "daily" -> { dt -> dt.hour * 100L + dt.minute }
    "weekly" -> { dt -> dt.dayOfWeek.value * 10000L + dt.hour * 100L + dt.minute }
    "monthly" -> { dt -> dt.dayOfMonth * 10000L + dt.hour * 100L + dt.minute }
    else -> { dt -> (dt.monthValue * 100L + dt.dayOfMonth) * 10000L + dt.hour * 100L + dt.minute } // yearly
}

/** True when [config] has any visibility restriction (a hide, a scheduled window, or a condition). */
fun HKIButtonConfig.hasVisibilityRule(): Boolean =
    hidden || !visibilityStart.isNullOrBlank() || !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isButtonVisibleNow(config: HKIButtonConfig, resolveEntityState: ((String) -> String?)? = null): Boolean =
    isButtonVisibleAt(config, LocalDateTime.now(), resolveEntityState)

/** True when [badge] has any visibility restriction (a hide, a scheduled window, or a condition). */
fun HKIBadge.hasVisibilityRule(): Boolean =
    hidden || !visibilityStart.isNullOrBlank() || !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isBadgeVisibleAt(badge: HKIBadge, now: LocalDateTime, resolveEntityState: ((String) -> String?)? = null): Boolean =
    isVisibleAt(
        badge.hidden, badge.visibilityStart, badge.visibilityEnd, badge.visibilityRangeMode, badge.visibilityRecurrence, now,
        badge.visibilityConditionEntityId, badge.visibilityConditionState, badge.visibilityConditionNegate, resolveEntityState
    )

fun isBadgeVisibleNow(badge: HKIBadge, resolveEntityState: ((String) -> String?)? = null): Boolean =
    isBadgeVisibleAt(badge, LocalDateTime.now(), resolveEntityState)

/** True when [widget] has any visibility restriction (a hide, a scheduled window, or a condition). */
fun HKIRoomWidget.hasVisibilityRule(): Boolean =
    isHidden || !visibilityStart.isNullOrBlank() || !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isWidgetVisibleAt(widget: HKIRoomWidget, now: LocalDateTime, resolveEntityState: ((String) -> String?)? = null): Boolean =
    isVisibleAt(
        widget.isHidden, widget.visibilityStart, widget.visibilityEnd, widget.visibilityRangeMode, widget.visibilityRecurrence, now,
        widget.visibilityConditionEntityId, widget.visibilityConditionState, widget.visibilityConditionNegate, resolveEntityState
    )

fun isWidgetVisibleNow(widget: HKIRoomWidget, resolveEntityState: ((String) -> String?)? = null): Boolean =
    isWidgetVisibleAt(widget, LocalDateTime.now(), resolveEntityState)

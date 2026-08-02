package com.jimz011apps.hki7.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDateTime

/**
 * Visibility rules for a button, badge, or widget.
 *
 * An item is hidden outright when its `hidden`/`isHidden` flag is set. Otherwise it is shown when
 * its [HKIVisibilityCondition] blocks pass. Per-block connectors support mixed AND/OR expressions;
 * the legacy global match value remains the fallback for older saved rules. No blocks means always
 * visible.
 *
 * Blocks replaced an earlier flat "one schedule plus one entity check" layout. Items saved under
 * that layout still carry the flat fields and no blocks, so [normalizedVisibilityConditions]
 * projects them into the block form on read — nothing is migrated on disk and old configs keep
 * behaving exactly as before.
 */

/** Legacy flat fields expressed as blocks, used when an item carries no explicit block list. */
fun normalizedVisibilityConditions(
    conditions: List<HKIVisibilityCondition>,
    visibilityStart: String?,
    visibilityEnd: String?,
    visibilityRangeMode: String,
    visibilityRecurrence: String,
    conditionEntityId: String?,
    conditionState: String?,
    conditionNegate: Boolean,
): List<HKIVisibilityCondition> {
    if (conditions.isNotEmpty()) return conditions
    return buildList {
        if (!visibilityStart.isNullOrBlank() || !visibilityEnd.isNullOrBlank()) {
            add(
                HKIVisibilityCondition(
                    type = VISIBILITY_TYPE_TIME,
                    start = visibilityStart,
                    end = visibilityEnd,
                    rangeMode = visibilityRangeMode.ifBlank { "show" },
                    recurrence = visibilityRecurrence.ifBlank { "none" },
                )
            )
        }
        if (!conditionEntityId.isNullOrBlank()) {
            add(
                HKIVisibilityCondition(
                    type = VISIBILITY_TYPE_ENTITY,
                    entityId = conditionEntityId,
                    state = conditionState,
                    negate = conditionNegate,
                )
            )
        }
    }
}

/** Whether one block passes at [now]. Entity blocks pass when no state resolver is available. */
private fun isConditionMet(
    condition: HKIVisibilityCondition,
    now: LocalDateTime,
    resolveEntityState: ((String) -> String?)?,
    currentUserId: String?,
): Boolean = when (condition.type) {
    VISIBILITY_TYPE_TIME -> isWithinSchedule(
        condition.start, condition.end, condition.rangeMode, condition.recurrence, now
    )
    VISIBILITY_TYPE_PERSON -> {
        if (condition.userId.isNullOrBlank() || currentUserId.isNullOrBlank()) {
            // Until identity is available, do not make content disappear. The identity refresh
            // updates snapshot state and causes readers to evaluate again.
            true
        } else {
            val matches = condition.userId == currentUserId
            if (condition.negate) !matches else matches
        }
    }
    else -> {
        if (condition.entityId.isNullOrBlank() || resolveEntityState == null) {
            true
        } else {
            val current = resolveEntityState(condition.entityId)
            val matches = current != null && current.equals(condition.state, ignoreCase = true)
            if (condition.negate) !matches else matches
        }
    }
}

/**
 * Whether a schedule window contains [now].
 *
 * - An open-ended window (only a start, or only an end) is a half-open range.
 * - [rangeMode] "show" means visible inside the window, "hide" means hidden inside it.
 * - Unparseable bounds are ignored so a bad value never permanently hides an item.
 */
private fun isWithinSchedule(
    start: String?,
    end: String?,
    rangeMode: String,
    recurrence: String,
    now: LocalDateTime,
): Boolean {
    val from = start?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val to = end?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    if (from == null && to == null) return true

    val inRange = when (recurrence.ifBlank { "none" }) {
        "none" -> (from == null || !now.isBefore(from)) && (to == null || !now.isAfter(to))
        else -> {
            // Compare only the part of the timestamp that repeats (time of day, day of week/month, or
            // month-day), so a window can recur. Ranges may wrap the cycle boundary (e.g. 24 Dec–2 Jan).
            val ordinal = recurringOrdinal(recurrence)
            val s = from?.let(ordinal)
            val e = to?.let(ordinal)
            val n = ordinal(now)
            when {
                s != null && e != null -> if (s <= e) n in s..e else (n >= s || n <= e)
                s != null -> n >= s
                e != null -> n <= e
                else -> true
            }
        }
    }
    return if (rangeMode == "hide") !inRange else inRange
}

/** Raw visibility rule shared by buttons, badges, and widgets. */
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
    currentUserId: String? = null,
    conditions: List<HKIVisibilityCondition> = emptyList(),
    match: String = VISIBILITY_MATCH_ALL,
): Boolean {
    if (hidden) return false
    val blocks = normalizedVisibilityConditions(
        conditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        conditionEntityId, conditionState, conditionNegate,
    )
    if (blocks.isEmpty()) return true
    val values = blocks.map { isConditionMet(it, now, resolveEntityState, currentUserId) }
    // AND binds tighter than OR: each OR starts a new all-must-pass group.
    var groupPasses = values.first()
    var anyCompletedGroupPasses = false
    blocks.indices.drop(1).forEach { index ->
        val connector = blocks[index].connector ?: if (match == VISIBILITY_MATCH_ANY) {
            VISIBILITY_CONNECTOR_OR
        } else {
            VISIBILITY_CONNECTOR_AND
        }
        if (connector == VISIBILITY_CONNECTOR_OR) {
            anyCompletedGroupPasses = anyCompletedGroupPasses || groupPasses
            groupPasses = values[index]
        } else {
            groupPasses = groupPasses && values[index]
        }
    }
    return anyCompletedGroupPasses || groupPasses
}

/** A sortable position within the repeat cycle for [recurrence]; higher fields drop out so the
 * window recurs (yearly ignores year, monthly ignores year+month, and so on). */
private fun recurringOrdinal(recurrence: String): (LocalDateTime) -> Long = when (recurrence) {
    "daily" -> { dt -> dt.hour * 100L + dt.minute }
    "weekly" -> { dt -> dt.dayOfWeek.value * 10000L + dt.hour * 100L + dt.minute }
    "monthly" -> { dt -> dt.dayOfMonth * 10000L + dt.hour * 100L + dt.minute }
    else -> { dt -> (dt.monthValue * 100L + dt.dayOfMonth) * 10000L + dt.hour * 100L + dt.minute } // yearly
}

fun isButtonVisibleAt(
    config: HKIButtonConfig,
    now: LocalDateTime,
    resolveEntityState: ((String) -> String?)? = null,
    currentUserId: String? = null,
): Boolean = isVisibleAt(
    config.hidden, config.visibilityStart, config.visibilityEnd, config.visibilityRangeMode, config.visibilityRecurrence, now,
    config.visibilityConditionEntityId, config.visibilityConditionState, config.visibilityConditionNegate, resolveEntityState,
    currentUserId,
    config.visibilityConditions, config.visibilityMatch,
)

/** Process-local identity used by the convenience `*VisibleNow` functions. The value is snapshot
 * state so visibility readers recompose as soon as the authenticated HA identity is resolved. */
object VisibilityUserSession {
    var currentUserId: String? by mutableStateOf(null)
        private set

    fun update(userId: String?) {
        currentUserId = userId
    }
}

/** True when [this] has any visibility restriction (a hide, a schedule, or a condition). */
fun HKIButtonConfig.hasVisibilityRule(): Boolean =
    hidden || visibilityConditions.isNotEmpty() || !visibilityStart.isNullOrBlank() ||
        !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isButtonVisibleNow(
    config: HKIButtonConfig,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean = isButtonVisibleAt(
    config, LocalDateTime.now(), resolveEntityState, VisibilityUserSession.currentUserId
)

/** True when [this] has any visibility restriction (a hide, a schedule, or a condition). */
fun HKIBadge.hasVisibilityRule(): Boolean =
    hidden || visibilityConditions.isNotEmpty() || !visibilityStart.isNullOrBlank() ||
        !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isBadgeVisibleAt(
    badge: HKIBadge,
    now: LocalDateTime,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean =
    isVisibleAt(
        badge.hidden, badge.visibilityStart, badge.visibilityEnd, badge.visibilityRangeMode, badge.visibilityRecurrence, now,
        badge.visibilityConditionEntityId, badge.visibilityConditionState, badge.visibilityConditionNegate, resolveEntityState,
        VisibilityUserSession.currentUserId,
        badge.visibilityConditions, badge.visibilityMatch,
    )

fun isBadgeVisibleNow(
    badge: HKIBadge,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean = isBadgeVisibleAt(badge, LocalDateTime.now(), resolveEntityState)

/** True when [this] has any visibility restriction (a hide, a schedule, or a condition). */
fun HKIRoomWidget.hasVisibilityRule(): Boolean =
    isHidden || visibilityConditions.isNotEmpty() || !visibilityStart.isNullOrBlank() ||
        !visibilityEnd.isNullOrBlank() || !visibilityConditionEntityId.isNullOrBlank()

fun isWidgetVisibleAt(
    widget: HKIRoomWidget,
    now: LocalDateTime,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean =
    isVisibleAt(
        widget.isHidden, widget.visibilityStart, widget.visibilityEnd, widget.visibilityRangeMode, widget.visibilityRecurrence, now,
        widget.visibilityConditionEntityId, widget.visibilityConditionState, widget.visibilityConditionNegate, resolveEntityState,
        VisibilityUserSession.currentUserId,
        widget.visibilityConditions, widget.visibilityMatch,
    )

fun isWidgetVisibleNow(
    widget: HKIRoomWidget,
    resolveEntityState: ((String) -> String?)? = null,
): Boolean = isWidgetVisibleAt(widget, LocalDateTime.now(), resolveEntityState)

/** Entity ids referenced by [this]'s visibility rules, so renderers can observe them. */
fun HKIRoomWidget.visibilityConditionEntityIds(): List<String> =
    normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ).mapNotNull { it.entityId?.takeIf(String::isNotBlank) }

/** Entity ids referenced by [this]'s visibility rules, so renderers can observe them. */
fun HKIButtonConfig.visibilityConditionEntityIds(): List<String> =
    normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ).mapNotNull { it.entityId?.takeIf(String::isNotBlank) }

/** Entity ids referenced by [this]'s visibility rules, so renderers can observe them. */
fun HKIBadge.visibilityConditionEntityIds(): List<String> =
    normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ).mapNotNull { it.entityId?.takeIf(String::isNotBlank) }

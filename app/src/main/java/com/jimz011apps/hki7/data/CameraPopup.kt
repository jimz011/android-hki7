package com.jimz011apps.hki7.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** One in-app camera popup rule. Pure data — no Android types — so it can move into shared KMP later. */
@Serializable
data class CameraPopupRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val triggerEntityId: String = "",
    val cameraEntityId: String = "",
    /** Seconds the live view stays open before it closes itself. */
    val timeoutSeconds: Int = DEFAULT_CAMERA_POPUP_TIMEOUT_SECONDS,
    /** Optional `input_boolean` (or similar) that must be on, e.g. tillat_popup_varsler. */
    val enableEntityId: String? = null,
    /** Optional local-time window, `HH:mm`. Same wrapping rules as Home Assistant's time condition. */
    val timeAfter: String? = null,
    val timeBefore: String? = null,
    /** When true, the popup is only the live stream, fitted to screen and orientation. */
    val fullscreen: Boolean = true,
)

@Serializable
data class CameraPopupSettings(
    val enabled: Boolean = true,
    val rules: List<CameraPopupRule> = emptyList(),
)

@Serializable
data class CameraPopupStore(
    val byInstanceId: Map<String, CameraPopupSettings> = emptyMap(),
)

/** A live popup the UI should show. [nonce] changes on every fire so the autoclose timer restarts. */
data class CameraPopupRequest(
    val nonce: String,
    val ruleId: String,
    val title: String,
    val cameraEntityId: String,
    val timeoutMs: Long,
    val fullscreen: Boolean = true,
)

const val DEFAULT_CAMERA_POPUP_TIMEOUT_SECONDS = 30
const val MIN_CAMERA_POPUP_TIMEOUT_SECONDS = 5
const val MAX_CAMERA_POPUP_TIMEOUT_SECONDS = 120

private val ACTIVE_STATES = setOf("on", "detected", "true", "home", "occupied", "open")
private val QUIET_STATES = setOf("off", "unavailable", "unknown", "none", "idle", "")

fun CameraPopupRule.displayName(): String =
    name.trim().ifBlank { triggerEntityId.substringAfter('.', triggerEntityId).ifBlank { "Camera popup" } }

fun CameraPopupRule.isReady(): Boolean =
    triggerEntityId.isNotBlank() && cameraEntityId.isNotBlank()

fun CameraPopupRule.clampedTimeoutSeconds(): Int =
    timeoutSeconds.coerceIn(MIN_CAMERA_POPUP_TIMEOUT_SECONDS, MAX_CAMERA_POPUP_TIMEOUT_SECONDS)

/**
 * Motion/person/doorbell "fired": a binary sensor going active, or an `event.*` entity getting a
 * new state (Home Assistant event entities change their state id on every detection).
 */
fun isCameraPopupTriggerFiring(
    triggerEntityId: String,
    changedEntityId: String,
    previousState: String?,
    nextState: String?,
): Boolean {
    if (triggerEntityId.isBlank() || changedEntityId != triggerEntityId) return false
    val next = nextState?.trim()?.lowercase() ?: return false
    if (next in QUIET_STATES) return false
    val previous = previousState?.trim()?.lowercase()
    val domain = changedEntityId.substringBefore('.', missingDelimiterValue = "")
    if (domain == "event") return previous != next
    if (next !in ACTIVE_STATES) return false
    return previous != next && (previous == null || previous !in ACTIVE_STATES)
}

fun isCameraPopupEnableSatisfied(enableEntityId: String?, enableState: String?): Boolean {
    val id = enableEntityId?.trim().orEmpty()
    if (id.isEmpty()) return true
    return enableState?.trim()?.lowercase() in ACTIVE_STATES
}

/** Minutes from midnight, 0..1439. */
fun parseHhMmToMinutes(value: String?): Int? {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return null
    val parts = text.split(':')
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].takeWhile { it.isDigit() }.toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

/**
 * Home Assistant time-condition wrapping: when [after] is later than [before], the window
 * crosses midnight (`after: 06:00`, `before: 00:00` → from 06:00 until midnight). Equal
 * after/before is an empty window, matching HA (`after > before` is the wrap test).
 */
fun isWithinCameraPopupTimeWindow(after: String?, before: String?, nowMinutes: Int): Boolean {
    val afterMin = parseHhMmToMinutes(after)
    val beforeMin = parseHhMmToMinutes(before)
    if (afterMin == null && beforeMin == null) return true
    if (afterMin != null && beforeMin == null) return nowMinutes >= afterMin
    if (afterMin == null && beforeMin != null) return nowMinutes < beforeMin
    val start = afterMin!!
    val end = beforeMin!!
    return when {
        start < end -> nowMinutes in start until end
        start > end -> nowMinutes >= start || nowMinutes < end
        else -> false
    }
}

fun matchingCameraPopupRules(
    settings: CameraPopupSettings,
    changedEntityId: String,
    previousState: String?,
    nextState: String?,
    enableStateFor: (String) -> String?,
    nowMinutes: Int,
): List<CameraPopupRule> {
    if (!settings.enabled) return emptyList()
    return settings.rules.filter { rule ->
        rule.enabled &&
            rule.isReady() &&
            isCameraPopupTriggerFiring(rule.triggerEntityId, changedEntityId, previousState, nextState) &&
            isCameraPopupEnableSatisfied(rule.enableEntityId, rule.enableEntityId?.let(enableStateFor)) &&
            isWithinCameraPopupTimeWindow(rule.timeAfter, rule.timeBefore, nowMinutes)
    }
}

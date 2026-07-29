package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Formats a remaining-time countdown as `H:MM:SS` (with hours) or `MM:SS` (under an hour), e.g.
 * `00:19` for nineteen seconds left. Non-positive durations read as "Done".
 */
fun formatCountdown(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "Done"
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** States that mean an appliance is NOT actively running, so its (possibly stale) completion time
 *  shouldn't drive a countdown. Everything else — "run", "running", "on", "washing", "drying",
 *  "baking", a numeric remaining time, … — is treated as running. */
private val machineIdleStates = setOf(
    "off", "idle", "standby", "unavailable", "unknown", "none", "", "finished", "complete",
    "completed", "ready", "ready_to_start", "end", "programmed", "paused", "pause", "docked", "0"
)

/** Whether a machine/operation-state value indicates the appliance is running (used to gate the
 *  completion-time countdown). Null/blank is treated as running so a missing state never hides it. */
fun isMachineRunning(stateValue: String?): Boolean {
    val s = stateValue?.trim()?.lowercase() ?: return true
    return s !in machineIdleStates
}

private val machineStateKeywords = listOf(
    "operation_state", "machine_state", "program_state", "program_phase", "operation", "program",
    "machine", "status", "state", "phase"
)

/** Best-effort guess of a machine/operation-state entity for [primaryId] (the entity carrying the
 *  completion time), preferring a sibling on the same Home Assistant device whose id/name looks like
 *  an operation-state, then a name-matched fallback. Returns null when nothing plausible is found so
 *  the user can pick one manually. */
fun guessMachineStateEntityId(
    primaryId: String?,
    allEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
): String? {
    val id = primaryId ?: return null
    val present = allEntities.associateBy { it.entity_id }
    val deviceId = registry.firstOrNull { it.entity_id == id }?.device_id
    val siblingIds = if (deviceId != null) {
        registry.filter { it.device_id == deviceId }.map { it.entity_id }
    } else {
        // No device info: match entities sharing the object-id stem (e.g. "washing_machine_*").
        val stem = id.substringAfter('.').substringBefore('_')
        allEntities.map { it.entity_id }.filter { it.substringAfter('.').startsWith(stem) }
    }
    fun looksLikeState(entityId: String): Boolean {
        val name = "${entityId.substringAfter('.')} ${present[entityId]?.friendlyName.orEmpty()}".lowercase()
        return machineStateKeywords.any { name.contains(it) }
    }
    return siblingIds
        .filter { it != id && it in present && looksLikeState(it) }
        // Prefer select/sensor operation-state entities over others.
        .minByOrNull { if (it.startsWith("select.") || it.startsWith("sensor.")) 0 else 1 }
}

/** Parses an appliance completion timestamp — an ISO-8601 instant (with offset) or a naive local
 *  date-time — to an [Instant], or null when it isn't a timestamp. */
fun parseTimestampToInstant(value: String?): Instant? {
    val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant() }
        .getOrNull()
}

/**
 * Live countdown text to [targetIso] (a completion timestamp), ticking every second. Returns null
 * when [targetIso] can't be parsed as a timestamp, so callers can fall back to the raw value. Used
 * by buttons/badges whose state is a "finished at" time (washer, dryer, dishwasher, …) so they can
 * show a descending "MM:SS left" instead of a raw ISO string.
 */
@Composable
fun rememberCountdownText(targetIso: String?): String? {
    val target = remember(targetIso) { parseTimestampToInstant(targetIso) } ?: return null
    var now by remember(targetIso) { mutableStateOf(Instant.now()) }
    // Stop ticking once the target has passed; the value stays at "Done" without a busy loop.
    val elapsed = !now.isBefore(target)
    androidx.compose.runtime.LaunchedEffect(targetIso, elapsed) {
        while (Instant.now().isBefore(target)) {
            now = Instant.now()
            delay(1000)
        }
        now = Instant.now()
    }
    return formatCountdown(Duration.between(now, target).seconds)
}

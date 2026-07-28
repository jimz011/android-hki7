package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

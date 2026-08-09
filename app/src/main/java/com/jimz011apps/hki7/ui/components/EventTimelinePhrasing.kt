package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HALogbookEvent

/**
 * Turns a logbook event into the one line the timeline shows for it — "Front door was opened"
 * rather than "binary_sensor.front_door: on".
 *
 * Home Assistant humanises some events itself and sends them as `message`; those are used as-is.
 * Everything else arrives as a bare state, and a bare state is exactly what makes a raw logbook
 * unreadable at a glance, so it is phrased here from the entity's domain and device class. Only
 * the wording is decided here: which entities appear at all is the household roster's business.
 */
@Composable
fun eventPhrase(event: HALogbookEvent, entity: HAEntity?): String {
    // HA already phrased it (automations, scripts, scenes, and the domains it has wording for).
    event.message?.takeIf { it.isNotBlank() }?.let { return it }
    val state = event.state?.takeIf { it.isNotBlank() } ?: return ""
    val domain = event.entityId?.substringBefore('.') ?: event.domain
    return stateVerb(domain, entity?.deviceClass, state)
}

/**
 * What kind of thing an event happened to — "Door", "Window", "Motion", "Light".
 *
 * Keyed on device class before domain for the same reason the phrasing is: `binary_sensor` covers
 * doors, windows, motion, smoke and damp alike, so grouping by domain would put all of them in one
 * heap and defeat the point of having categories at all. The returned value is a stable key, not a
 * label — [eventCategoryLabel] turns it into display text, so filter state survives a language
 * change and never depends on translated strings comparing equal.
 */
fun eventCategoryKey(event: HALogbookEvent, entity: HAEntity?): String {
    val domain = event.entityId?.substringBefore('.') ?: event.domain ?: "other"
    return when (entity?.deviceClass?.lowercase()) {
        "door", "garage_door" -> "door"
        "window", "opening" -> "window"
        "motion", "occupancy", "presence" -> "motion"
        "moisture" -> "moisture"
        "smoke", "gas", "carbon_monoxide" -> "safety"
        "problem", "safety" -> "problem"
        "battery" -> "battery"
        "connectivity" -> "connectivity"
        else -> domain
    }
}

/** Display label for a key from [eventCategoryKey]. Unknown domains are titled from the id
 *  itself rather than dropped, so a timeline never has an unnamed filter chip. */
@Composable
fun eventCategoryLabel(key: String): String = when (key) {
    "door" -> stringResource(R.string.event_category_door)
    "window" -> stringResource(R.string.event_category_window)
    "motion" -> stringResource(R.string.event_category_motion)
    "moisture" -> stringResource(R.string.event_category_moisture)
    "safety" -> stringResource(R.string.event_category_safety)
    "problem" -> stringResource(R.string.event_category_problem)
    "battery" -> stringResource(R.string.event_category_battery)
    "connectivity" -> stringResource(R.string.event_category_connectivity)
    "light" -> stringResource(R.string.event_category_light)
    "switch" -> stringResource(R.string.event_category_switch)
    "lock" -> stringResource(R.string.event_category_lock)
    "cover" -> stringResource(R.string.event_category_cover)
    "climate" -> stringResource(R.string.event_category_climate)
    "person", "device_tracker" -> stringResource(R.string.event_category_presence)
    "alarm_control_panel" -> stringResource(R.string.event_category_alarm)
    "media_player" -> stringResource(R.string.event_category_media)
    "vacuum" -> stringResource(R.string.event_category_vacuum)
    "automation", "script", "scene" -> stringResource(R.string.event_category_automation)
    "binary_sensor", "sensor" -> stringResource(R.string.event_category_sensor)
    else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * The verb for one state, chosen by device class first and domain second.
 *
 * Device class before domain because it is the more specific of the two and the one that carries
 * the meaning: `binary_sensor` on its own can only manage "detected", while the same entity with
 * a `door` class earns "was opened". An unrecognised state falls through to naming it plainly,
 * which is still better than dropping the event.
 */
@Composable
private fun stateVerb(domain: String?, deviceClass: String?, state: String): String {
    val on = state.equals("on", ignoreCase = true)
    val off = state.equals("off", ignoreCase = true)

    if (on || off) {
        when (deviceClass?.lowercase()) {
            "door", "garage_door", "window", "opening" ->
                return stringResource(if (on) R.string.event_verb_opened else R.string.event_verb_closed)
            "lock" ->
                return stringResource(if (on) R.string.event_verb_unlocked else R.string.event_verb_locked)
            "motion", "occupancy", "presence" ->
                return stringResource(if (on) R.string.event_verb_motion_detected else R.string.event_verb_motion_cleared)
            "moisture" ->
                return stringResource(if (on) R.string.event_verb_moisture_detected else R.string.event_verb_moisture_cleared)
            "smoke", "gas", "carbon_monoxide" ->
                return stringResource(if (on) R.string.event_verb_alarm_detected else R.string.event_verb_alarm_cleared)
            "problem", "safety" ->
                return stringResource(if (on) R.string.event_verb_problem_detected else R.string.event_verb_problem_cleared)
            "battery" ->
                return stringResource(if (on) R.string.event_verb_battery_low else R.string.event_verb_battery_normal)
            "connectivity" ->
                return stringResource(if (on) R.string.event_verb_connected else R.string.event_verb_disconnected)
        }
        return when (domain) {
            "light", "switch", "fan", "siren", "input_boolean", "automation", "script" ->
                stringResource(if (on) R.string.event_verb_turned_on else R.string.event_verb_turned_off)
            else ->
                stringResource(if (on) R.string.event_verb_detected else R.string.event_verb_cleared)
        }
    }

    return when (domain) {
        "lock" -> when (state.lowercase()) {
            "locked" -> stringResource(R.string.event_verb_locked)
            "unlocked" -> stringResource(R.string.event_verb_unlocked)
            else -> stringResource(R.string.event_verb_generic, state)
        }
        "cover" -> when (state.lowercase()) {
            "open" -> stringResource(R.string.event_verb_opened)
            "closed" -> stringResource(R.string.event_verb_closed)
            "opening" -> stringResource(R.string.event_verb_opening)
            "closing" -> stringResource(R.string.event_verb_closing)
            else -> stringResource(R.string.event_verb_generic, state)
        }
        "person", "device_tracker" -> when (state.lowercase()) {
            "home" -> stringResource(R.string.event_verb_arrived_home)
            "not_home" -> stringResource(R.string.event_verb_left_home)
            // Anything else is a named zone, which reads perfectly well on its own.
            else -> stringResource(R.string.event_verb_arrived_at, state)
        }
        "alarm_control_panel" -> when (state.lowercase()) {
            "disarmed" -> stringResource(R.string.event_verb_disarmed)
            "triggered" -> stringResource(R.string.event_verb_triggered)
            "arming" -> stringResource(R.string.event_verb_arming)
            "pending" -> stringResource(R.string.event_verb_pending)
            else -> stringResource(R.string.event_verb_armed)
        }
        "climate" -> stringResource(R.string.event_verb_set_to, state)
        "vacuum" -> when (state.lowercase()) {
            "cleaning" -> stringResource(R.string.event_verb_started_cleaning)
            "returning" -> stringResource(R.string.event_verb_returning_to_dock)
            "docked" -> stringResource(R.string.event_verb_docked)
            else -> stringResource(R.string.event_verb_generic, state)
        }
        "media_player" -> when (state.lowercase()) {
            "playing" -> stringResource(R.string.event_verb_started_playing)
            "paused" -> stringResource(R.string.event_verb_paused)
            "idle", "standby" -> stringResource(R.string.event_verb_stopped)
            else -> stringResource(R.string.event_verb_generic, state)
        }
        else -> stringResource(R.string.event_verb_generic, state)
    }
}

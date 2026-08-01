package com.jimz011apps.hki7.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HomeAssistantConnectionRoute
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.NumberFormat
import java.util.Locale

/** Locale-aware labels for route values that remain stable English tokens in the data layer. */
@Composable
fun HomeAssistantConnectionRoute.localizedName(): String = when (this) {
    HomeAssistantConnectionRoute.LOCAL -> stringResource(R.string.connection_route_local)
    HomeAssistantConnectionRoute.NABU_CASA -> stringResource(R.string.connection_route_nabu_casa)
    HomeAssistantConnectionRoute.EXTERNAL -> stringResource(R.string.connection_route_external)
}

/** Home Assistant keeps state values as stable English tokens; translate the common states at the
 * presentation boundary while leaving custom integration states intact. */
@Composable
fun HAEntity.localizedStateLabel(): String {
    val unit = attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
    state.toDoubleOrNull()?.let { value ->
        val formatter = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
        return listOfNotNull(formatter.format(value), unit?.takeIf(String::isNotBlank))
            .joinToString(" ")
    }

    val normalized = state.trim().lowercase(Locale.ROOT)
    if (entity_id.startsWith("binary_sensor.") && normalized in setOf("on", "off")) {
        val active = normalized == "on"
        return when (deviceClass) {
            "door", "garage_door", "window", "opening" ->
                stringResource(if (active) R.string.cr_open else R.string.cr_closed)
            "lock" -> stringResource(if (active) R.string.cr_unlocked else R.string.cr_locked)
            "moisture" -> stringResource(if (active) R.string.cr_wet else R.string.cr_dry)
            "motion", "moving", "occupancy", "presence", "smoke", "gas", "co" ->
                stringResource(if (active) R.string.cr_detected else R.string.cr_clear)
            "problem", "safety", "tamper" ->
                stringResource(if (active) R.string.cr_problem else R.string.cr_ok)
            "battery" -> stringResource(if (active) R.string.cr_low else R.string.cr_normal)
            "connectivity" ->
                stringResource(if (active) R.string.cr_connected else R.string.cr_disconnected)
            "plug", "power" ->
                stringResource(if (active) R.string.cr_plugged_in else R.string.cr_unplugged)
            else -> stringResource(if (active) R.string.cr_state_on else R.string.cr_state_off)
        }
    }

    return localizedCommonStateLabel(state)
}

/** Translate a common Home Assistant state token when no full entity/device-class context exists. */
@Composable
fun localizedCommonStateLabel(state: String): String = when (state.trim().lowercase(Locale.ROOT)) {
        "on" -> stringResource(R.string.cr_state_on)
        "off" -> stringResource(R.string.cr_state_off)
        "open" -> stringResource(R.string.cr_open)
        "closed" -> stringResource(R.string.cr_closed)
        "opening" -> stringResource(R.string.cr_opening)
        "closing" -> stringResource(R.string.cr_closing)
        "locked" -> stringResource(R.string.cr_locked)
        "unlocked" -> stringResource(R.string.cr_unlocked)
        "unavailable" -> stringResource(R.string.cr_unavailable)
        "unknown" -> stringResource(R.string.cr_unknown)
        "idle" -> stringResource(R.string.cr_idle)
        "home" -> stringResource(R.string.cr_home)
        "not_home", "away" -> stringResource(R.string.cr_away)
        "cleaning" -> stringResource(R.string.cr_cleaning)
        "returning" -> stringResource(R.string.cr_returning)
        "paused" -> stringResource(R.string.cr_paused)
        "error" -> stringResource(R.string.cr_error)
        "playing" -> stringResource(R.string.cr_playing)
        "buffering" -> stringResource(R.string.cr_buffering)
        "standby" -> stringResource(R.string.cr_standby)
        "humidifying" -> stringResource(R.string.cr_humidifying)
        "drying" -> stringResource(R.string.cr_drying)
        "detected" -> stringResource(R.string.cr_detected)
        "clear" -> stringResource(R.string.cr_clear)
        "wet" -> stringResource(R.string.cr_wet)
        "dry" -> stringResource(R.string.cr_dry)
        "active" -> stringResource(R.string.ui_active_a733b80)
        "armed_home" -> stringResource(R.string.ui_armed_home_c8d0b44)
        "armed_away" -> stringResource(R.string.ui_armed_away_6834abf)
        "armed_night" -> stringResource(R.string.ui_armed_night_80659bd)
        "armed_vacation", "armed_custom_bypass" -> stringResource(R.string.ui_armed_32caa31)
        "disarmed" -> stringResource(R.string.ui_disarmed_aaa4d9e)
        "triggered" -> stringResource(R.string.ui_alarm_triggered_641ff6f)
        "pending" -> stringResource(R.string.ui_alarm_pending_e2c04aa)
        else -> state.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

/** Localized presentation for Home Assistant HVAC mode tokens. */
@Composable
fun localizedHvacModeLabel(mode: String): String = when (mode.trim().lowercase(Locale.ROOT)) {
    "auto" -> stringResource(R.string.cr_mode_auto)
    "heat" -> stringResource(R.string.cr_mode_heat)
    "cool" -> stringResource(R.string.cr_mode_cool)
    "heat_cool" -> stringResource(R.string.cr_mode_heat_cool)
    "dry" -> stringResource(R.string.cr_mode_dry)
    "fan_only" -> stringResource(R.string.cr_mode_fan_only)
    "off" -> stringResource(R.string.cr_state_off)
    else -> mode.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

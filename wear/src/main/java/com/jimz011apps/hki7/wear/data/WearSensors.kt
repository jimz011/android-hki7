package com.jimz011apps.hki7.wear.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Reports the watch's own state to Home Assistant as `mobile_app` sensors.
 *
 * Battery only, and on purpose. A watch is full of sensors — heart rate, steps, skin temperature —
 * and none of them should leave the wrist because an app quietly decided to send them. Battery is
 * the one the user gains something from ("tell me when my watch needs charging") and the one with
 * no privacy weight, so it is the one that ships. Anything else should be its own decision, with
 * its own switch and its own reason.
 */
object WearSensors {

    private const val BATTERY_LEVEL = "watch_battery_level"
    private const val BATTERY_STATE = "watch_battery_state"

    /**
     * Registers the sensors and sends their current values.
     *
     * Registration is repeated rather than tracked: Home Assistant's `register_sensor` returns 201
     * and updates when the unique id already exists, so re-registering is free and is what recovers
     * a sensor Home Assistant forgot across a restart.
     */
    suspend fun report(context: Context, prefs: WearPreferences, session: WearSession): Boolean {
        if (!prefs.sensorsEnabledOnce()) return false
        val serverUrl = prefs.serverUrlOnce() ?: return false
        if (!session.isAuthenticated()) return false
        val webhookId = WearRegistration.ensureRegistered(context, prefs, session, serverUrl)
            ?: return false

        val battery = readBattery(context) ?: return false

        registerSensors(serverUrl, webhookId)
        return WearRegistration.postWebhook(
            serverUrl,
            webhookId,
            buildJsonObject {
                put("type", "update_sensor_states")
                put(
                    "data",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                // update_sensor_states requires the type on every entry, matching
                                // what the sensor was registered as.
                                put("type", "sensor")
                                put("unique_id", BATTERY_LEVEL)
                                put("state", battery.level)
                                put("icon", batteryIcon(battery))
                            },
                        )
                        add(
                            buildJsonObject {
                                put("type", "sensor")
                                put("unique_id", BATTERY_STATE)
                                put("state", battery.state)
                                put("icon", batteryIcon(battery))
                            },
                        )
                    },
                )
            },
        )
    }

    private suspend fun registerSensors(serverUrl: String, webhookId: String) {
        WearRegistration.postWebhook(serverUrl, webhookId, sensor(BATTERY_LEVEL, "Watch Battery Level", "battery", "%", "measurement"))
        WearRegistration.postWebhook(serverUrl, webhookId, sensor(BATTERY_STATE, "Watch Battery State", null, null, null))
    }

    private fun sensor(
        uniqueId: String,
        name: String,
        deviceClass: String?,
        unit: String?,
        stateClass: String?,
    ): JsonObject = buildJsonObject {
        put("type", "register_sensor")
        put(
            "data",
            buildJsonObject {
                put("type", "sensor")
                put("unique_id", uniqueId)
                put("name", name)
                put("state", 0)
                put("icon", "mdi:watch")
                deviceClass?.let { put("device_class", it) }
                unit?.let { put("unit_of_measurement", it) }
                stateClass?.let { put("state_class", it) }
            },
        )
    }

    private data class Battery(val level: Int, val state: String, val charging: Boolean)

    private fun readBattery(context: Context): Battery? {
        val intent: Intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val state = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        return Battery(level = level * 100 / scale, state = state, charging = charging)
    }

    private fun batteryIcon(battery: Battery): String = when {
        battery.charging -> "mdi:battery-charging"
        battery.level >= 90 -> "mdi:battery"
        battery.level <= 10 -> "mdi:battery-alert"
        else -> "mdi:battery-${battery.level / 10 * 10}"
    }
}

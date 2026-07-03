package com.example.hki7.ui.utils

/**
 * Data tables for the Material Design Icons (MDI) webfont.
 *
 * The full ~7,400-icon set ships as `res/font/mdi_icons.ttf`, with two lookup
 * tables in `assets/`:
 *   - `mdi_codepoints.txt` — `name codepointHex` per line (rendering + browse list)
 *   - `mdi_keywords.txt`   — `name<TAB>aliases tags` per line (picker search)
 *
 * Both are loaded on demand by [MdiIconStore]; rendering goes through the
 * [MdiIcon] composable. Icons are addressed by their MDI slug (e.g. `lightbulb`,
 * `weather-night`), matching the `mdi:` names Home Assistant emits.
 */

/** Icons surfaced first in the picker when the search box is empty. */
val MDI_COMMON: List<String> = listOf(
    "lightbulb", "lightbulb-group", "ceiling-light", "lamp", "led-strip-variant", "string-lights",
    "power-plug", "power-socket", "toggle-switch-variant", "thermostat", "air-conditioner", "fan",
    "radiator", "fire", "weather-sunny", "weather-night", "weather-partly-cloudy", "window-closed-variant",
    "blinds", "curtains", "garage", "door", "door-open", "lock",
    "lock-open-variant", "shield-home", "cctv", "motion-sensor", "alarm-light", "television",
    "speaker", "cast", "music", "volume-high", "home", "sofa",
    "bed", "fridge", "stove", "microwave", "washing-machine", "coffee",
    "water", "thermometer", "robot-vacuum", "broom", "vacuum", "account",
    "account-group", "car", "leak", "bell", "cog", "star",
    "heart", "wifi", "information", "flower", "tree", "grass",
    "pool", "umbrella", "weather-pouring", "walk", "run", "bike",
    "car-electric", "battery", "flash",
)

/**
 * Maps legacy icon identifiers persisted by earlier app versions — camelCase
 * Material names and custom picker slugs that are not real MDI names — to their
 * closest real MDI slug, so existing dashboards keep rendering after the switch
 * to the full MDI font. Real MDI slugs (and Home Assistant `mdi:` names) resolve
 * directly and are intentionally absent here.
 */
val LEGACY_ICON_MAP: Map<String, String> = mapOf(
    "Air" to "fan",
    "Attic" to "home",
    "Basement" to "home",
    "Blender" to "blender",
    "Blinds" to "blinds",
    "CameraAlt" to "camera",
    "CleaningServices" to "broom",
    "Cloud" to "cloud",
    "Coffee" to "coffee",
    "Curtains" to "curtains",
    "Door" to "door",
    "DoorFront" to "door",
    "Garage" to "garage",
    "Garden" to "flower",
    "GroundFloor" to "home",
    "Home" to "home",
    "Iron" to "iron",
    "Kitchen" to "stove",
    "Light" to "lightbulb-on",
    "Lightbulb" to "lightbulb",
    "LocalLaundryService" to "washing-machine",
    "Lock" to "lock",
    "Microwave" to "microwave",
    "MusicNote" to "music",
    "Nightlight" to "weather-night",
    "Outlet" to "power-plug",
    "Outside" to "weather-sunny",
    "Power" to "power",
    "Room" to "home",
    "Security" to "shield",
    "Thermometer" to "thermometer",
    "Tv" to "television",
    "Wash" to "hanger",
    "WaterDrop" to "water",
    "WbSunny" to "weather-sunny",
    "Window" to "window-closed-variant",
    "ac-unit" to "air-conditioner",
    "air" to "fan",
    "bolt" to "flash",
    "cleaning" to "broom",
    "dashboard" to "view-dashboard",
    "dryclean" to "hanger",
    "group" to "account-group",
    "incandescent" to "lightbulb",
    "info" to "information",
    "kitchen" to "stove",
    "laundry" to "washing-machine",
    "leak" to "pipe-leak",
    "light-mode" to "lightbulb-on",
    "lightning" to "lightning-bolt",
    "local-florist" to "flower",
    "nightlight" to "weather-night",
    "notifications" to "bell",
    "outlet" to "power-plug",
    "person" to "account",
    "power-settings" to "power",
    "rain" to "weather-rainy",
    "refrigerator" to "fridge",
    "robot" to "robot-vacuum",
    "sensor" to "motion-sensor",
    "sensor-door" to "door",
    "settings" to "cog",
    "smartphone" to "cellphone",
    "smoke" to "smoke-detector",
    "speed" to "speedometer",
    "tv" to "television",
    "visibility" to "eye",
    "volume" to "volume-high",
    "water-drop" to "water",
    "wb-sunny" to "weather-sunny",
    "window" to "window-closed-variant",
)

package com.example.hki7.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class HAEntity(
    val entity_id: String,
    val state: String,
    val attributes: JsonObject? = null,
    val last_changed: String? = null
) {
    val friendlyName: String?
        get() = attributes?.get("friendly_name")?.jsonPrimitive?.contentOrNull

    val temperature: Double?
        get() = attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull

    val humidity: Double?
        get() = attributes?.get("humidity")?.jsonPrimitive?.doubleOrNull

    val pressure: Double?
        get() = attributes?.get("pressure")?.jsonPrimitive?.doubleOrNull

    val windSpeed: Double?
        get() = attributes?.get("wind_speed")?.jsonPrimitive?.doubleOrNull

    val precipitation: Double?
        get() = attributes?.get("precipitation")?.jsonPrimitive?.doubleOrNull

    val entityPicture: String?
        get() = attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull

    val mediaTitle: String?
        get() = attributes?.get("media_title")?.jsonPrimitive?.contentOrNull

    val mediaArtist: String?
        get() = attributes?.get("media_artist")?.jsonPrimitive?.contentOrNull

    val brightness: Int?
        get() = attributes?.get("brightness")?.jsonPrimitive?.intOrNull

    val colorTempKelvin: Int?
        get() = attributes?.get("color_temp_kelvin")?.jsonPrimitive?.intOrNull

    val minKelvin: Int?
        get() = attributes?.get("min_color_temp_kelvin")?.jsonPrimitive?.intOrNull
        
    val maxKelvin: Int?
        get() = attributes?.get("max_color_temp_kelvin")?.jsonPrimitive?.intOrNull

    val rgbColor: List<Int>?
        get() {
            val arr = attributes?.get("rgb_color") as? JsonArray
            return arr?.mapNotNull { it.jsonPrimitive.intOrNull }
        }

    val effect: String?
        get() = attributes?.get("effect")?.jsonPrimitive?.contentOrNull

    val effectList: List<String>
        get() = (attributes?.get("effect_list") as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()

    val supportedColorModes: List<String>
        get() = (attributes?.get("supported_color_modes") as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
        
    val supportsBrightness: Boolean
        get() = brightness != null || supportedColorModes.any {
            it in listOf("brightness", "color_temp", "hs", "rgb", "rgbw", "rgbww", "xy")
        }
        
    val supportsColorTemp: Boolean
        get() = supportedColorModes.contains("color_temp")
        
    val supportsColor: Boolean
        get() = supportedColorModes.any { it in listOf("hs", "rgb", "rgbw", "rgbww", "xy") }

    val forecast: List<HAWeatherForecast>?
        get() {
            val arr = attributes?.get("forecast") as? JsonArray
            return arr?.map { it.asHAWeatherForecast() }
        }

    // Group/light-group entities (e.g. light.living_room) expose their members via an
    // `entity_id` attribute holding the list of member entity ids.
    val childEntityIds: List<String>
        get() = (attributes?.get("entity_id") as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()

    val hvacModes: List<String>
        get() = (attributes?.get("hvac_modes") as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()

    val deviceClass: String?
        get() = attributes?.get("device_class")?.jsonPrimitive?.contentOrNull

    // person.* entities carry the linked Home Assistant user id, used to match logbook actors to their avatar.
    val userId: String?
        get() = attributes?.get("user_id")?.jsonPrimitive?.contentOrNull

    // ── climate: fan / swing modes ──────────────────────────────────────────
    val fanMode: String?
        get() = attributes?.get("fan_mode")?.jsonPrimitive?.contentOrNull
    val fanModes: List<String>
        get() = (attributes?.get("fan_modes") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    val swingMode: String?
        get() = attributes?.get("swing_mode")?.jsonPrimitive?.contentOrNull
    val swingModes: List<String>
        get() = (attributes?.get("swing_modes") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    val swingHorizontalMode: String?
        get() = attributes?.get("swing_horizontal_mode")?.jsonPrimitive?.contentOrNull
    val swingHorizontalModes: List<String>
        get() = (attributes?.get("swing_horizontal_modes") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

    // ── cover: tilt ──────────────────────────────────────────────────────────
    val tiltPosition: Int?
        get() = attributes?.get("current_tilt_position")?.jsonPrimitive?.intOrNull
    val supportsTilt: Boolean
        get() = attributes?.containsKey("current_tilt_position") == true

    // ── fan domain ───────────────────────────────────────────────────────────
    val fanPercentage: Int?
        get() = attributes?.get("percentage")?.jsonPrimitive?.intOrNull
    val fanPresetMode: String?
        get() = attributes?.get("preset_mode")?.jsonPrimitive?.contentOrNull
    val fanPresetModes: List<String>
        get() = (attributes?.get("preset_modes") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    val fanOscillating: Boolean?
        get() = attributes?.get("oscillating")?.jsonPrimitive?.booleanOrNull
    val fanDirection: String?
        get() = attributes?.get("direction")?.jsonPrimitive?.contentOrNull

    // ── humidifier domain ────────────────────────────────────────────────────
    val currentHumidity: Double?
        get() = attributes?.get("current_humidity")?.jsonPrimitive?.doubleOrNull
    val minHumidity: Int?
        get() = attributes?.get("min_humidity")?.jsonPrimitive?.intOrNull
    val maxHumidity: Int?
        get() = attributes?.get("max_humidity")?.jsonPrimitive?.intOrNull
    val humidifierMode: String?
        get() = attributes?.get("mode")?.jsonPrimitive?.contentOrNull
    val humidifierAvailableModes: List<String>
        get() = (attributes?.get("available_modes") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

    // ── alarm_control_panel domain ──────────────────────────────────────────
    val supportedFeatures: Int
        get() = attributes?.get("supported_features")?.jsonPrimitive?.intOrNull ?: 0
    // null = no code needed, "number" = numeric keypad, "text" = free text
    val alarmCodeFormat: String?
        get() = attributes?.get("code_format")?.jsonPrimitive?.contentOrNull
    val alarmCodeArmRequired: Boolean
        get() = attributes?.get("code_arm_required")?.jsonPrimitive?.booleanOrNull ?: true
}

@Serializable
data class HAWeatherForecast(
    val datetime: String,
    val condition: String? = null,
    val temperature: Double? = null,
    val templow: Double? = null,
    val precipitation: Double? = null
)

fun JsonElement.asHAWeatherForecast(): HAWeatherForecast {
    val obj = this.jsonObject
    return HAWeatherForecast(
        datetime = obj["datetime"]?.jsonPrimitive?.content ?: "",
        condition = obj["condition"]?.jsonPrimitive?.contentOrNull,
        temperature = obj["temperature"]?.jsonPrimitive?.doubleOrNull,
        templow = obj["templow"]?.jsonPrimitive?.doubleOrNull,
        precipitation = obj["precipitation"]?.jsonPrimitive?.doubleOrNull
    )
}

@Serializable
data class HAArea(
    val area_id: String,
    val name: String,
    val picture: String? = null,
    val icon: String? = null,
    val floor_id: String? = null
)

@Serializable
data class HAFloor(
    val floor_id: String,
    val name: String,
    val level: Int? = null,
    val icon: String? = null,
    val columns: Int = 2,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 24,
    val compactTiles: Boolean = false
)

@Serializable
data class HAEntityRegistryEntry(
    val entity_id: String,
    val area_id: String? = null,
    val device_id: String? = null
)

@Serializable
data class HADeviceRegistryEntry(
    val id: String,
    val area_id: String? = null,
    val name: String? = null,
    val name_by_user: String? = null
)

@Serializable
data class HAServiceCall(
    val entity_id: String,
    val brightness: Int? = null,
    val color_temp_kelvin: Int? = null,
    val rgb_color: List<Int>? = null,
    val effect: String? = null,
    val temperature: Float? = null,
    val position: Int? = null,
    val tilt_position: Int? = null,
    val hvac_mode: String? = null,
    val fan_mode: String? = null,
    val swing_mode: String? = null,
    val swing_horizontal_mode: String? = null,
    val fan_speed: String? = null,
    val segments: List<Int>? = null,
    val command: String? = null,
    val percentage: Int? = null,
    val preset_mode: String? = null,
    val oscillating: Boolean? = null,
    val direction: String? = null,
    val humidity: Int? = null,
    val mode: String? = null,
    val code: String? = null
)

@Serializable
data class HATokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val token_type: String
)

@Serializable
data class HAHistoryEntry(
    val state: String,
    val last_changed: String,
    val attributes: JsonObject? = null,
    val context: HAContext? = null,
    val context_id: String? = null,
    val context_user_id: String? = null,
    val context_parent_id: String? = null,
    val actorId: String? = null,
    val actorName: String? = null
)

@Serializable
data class HAContext(
    val id: String? = null,
    val parent_id: String? = null,
    val user_id: String? = null
)

@Serializable
data class HALogbookEntry(
    val name: String? = null,
    val message: String? = null,
    val entity_id: String? = null,
    @SerialName("when") val time: String,
    val context_user_id: String? = null,
    val domain: String? = null
)

@Serializable
data class HAStateUpdate(
    val state: String,
    val attributes: JsonObject = buildJsonObject { }
)

/** A single entity change streamed from the websocket `state_changed` subscription.
 *  [newState] is null when the entity was removed. */
data class HAStateChange(
    val entityId: String,
    val newState: HAEntity?
)

/** Response from HA's `/api/mobile_app/registrations`. */
@Serializable
data class MobileAppRegistration(
    val webhook_id: String,
    val cloudhook_url: String? = null,
    val remote_ui_url: String? = null,
    val secret: String? = null
)

/** Thrown when a token refresh fails. [invalidGrant] is true only when the server explicitly
 *  rejected the refresh token (HTTP 400 invalid_grant), meaning re-login is required; transient
 *  network failures leave it false so callers can retry without logging the user out. */
class TokenRefreshException(
    val invalidGrant: Boolean,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

@Serializable
data class HAUser(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val is_admin: Boolean? = null,
    val is_active: Boolean? = null,
    val system_generated: Boolean? = null
)

@Serializable
data class HKIAreaConfig(
    val lockEntityId: String? = null,
    val climateEntityId: String? = null,
    val cameraEntityId: String? = null,
    val blindEntityId: String? = null,
    val name: String? = null,
    val mediaPlayerEntityId: String? = null,
    val icon: String? = null,
    val wallpaper: String? = null,
    val headerColor: String? = null,
    val floorId: String? = null,
    val lockEntityIds: List<String> = emptyList(),
    val climateEntityIds: List<String> = emptyList(),
    val cameraEntityIds: List<String> = emptyList(),
    val blindEntityIds: List<String> = emptyList(),
    val lockIcon: String? = null,
    val climateIcon: String? = null,
    val cameraIcon: String? = null,
    val blindIcon: String? = null,
    val badgeBar: HKIBadgeBarConfig? = null
)

@Serializable
data class HKIBadge(
    val id: String,
    val entityId: String,
    // Multiple entities aggregate into one badge. When empty, [entityId] is the sole entity.
    val entityIds: List<String> = emptyList(),
    val shape: String = "pill",        // "pill" or "circle"
    val side: String = "right",        // "left" or "right" (only used in split alignment)
    val showName: Boolean = false,
    val showState: Boolean = true,
    val showIcon: Boolean = true,
    val customIcon: String? = null,
    val spinIcon: Boolean = false,     // rotate the icon continuously while the entity isn't "off"
    val tapAction: String = "auto",    // "auto", "toggle", "more_info"
    val holdAction: String = "auto",   // "auto", "toggle", "more_info"
    // Lock: optional door/contact sensor(s). Legacy single-sensor + per-lock map.
    val doorEntityId: String? = null,
    val doorEntityIds: Map<String, String> = emptyMap(),       // lockEntityId -> door sensor
    // Vacuum: per-entity map camera + battery sensor overrides.
    val vacuumMapEntityIds: Map<String, String> = emptyMap(),
    val vacuumBatteryEntityIds: Map<String, String> = emptyMap()
) {
    /** All entity ids this badge represents (falls back to the single [entityId]). */
    val effectiveEntityIds: List<String>
        get() = entityIds.ifEmpty { listOf(entityId) }

    fun doorEntityIdFor(lockEntityId: String): String? =
        doorEntityIds[lockEntityId] ?: if (effectiveEntityIds.size == 1) doorEntityId else null
}

@Serializable
data class HKIBadgeBarConfig(
    val badges: List<HKIBadge> = emptyList(),
    val visible: Boolean = true,
    val alignment: String = "split",   // "left", "center", "right", "split"
    val spanIcons: Boolean = false,
    val leftOverflow: Boolean = false,
    val rightOverflow: Boolean = false
)

@Serializable
data class HKIPageConfig(
    val wallpaper: String? = null,
    val headerColor: String? = null,
    val showPeople: Boolean = true,
    val peopleSort: String = "changed",
    val customPeopleOrder: List<String> = emptyList(),
    val hiddenPeople: List<String> = emptyList(),
    val badgeBar: HKIBadgeBarConfig? = null,
    val energyConfig: HKIEnergyConfig? = null,
    val vacuumEntityId: String? = null,
    val vacuumMapEntityId: String? = null
)

/** Entity bindings for the Energy dashboard's power-flow visualization. All optional. */
@Serializable
data class HKIEnergyConfig(
    val solarPowerEntityId: String? = null,
    val gridPowerEntityId: String? = null,
    val homePowerEntityId: String? = null,
    val batteryPowerEntityId: String? = null,
    val solarEnergyEntityId: String? = null,
    val gridImportEntityId: String? = null,
    val gridExportEntityId: String? = null,
    val energyCostEntityId: String? = null,
    val batteryEntityId: String? = null
)

@Serializable
sealed class HKIRoomWidget {
    abstract val id: String
}

@Serializable
@SerialName("button_stack")
data class HKIButtonStack(
    override val id: String,
    val title: String? = null,
    val icon: String? = null,
    val entityIds: List<String> = emptyList(),
    val columns: Int = 2,
    val showBadge: Boolean = true,
    val isSquare: Boolean = true,
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    val stackType: String = "buttons",
    val cameraUrls: List<String> = emptyList(),
    val cameraAspectRatio: Float = 16f / 9f,
    val buttonConfigs: Map<String, HKIButtonConfig> = emptyMap()
) : HKIRoomWidget()

@Serializable
data class HKIButtonConfig(
    val name: String? = null,
    val icon: String? = null,
    val spinIcon: Boolean = false,     // rotate the icon continuously while the entity isn't "off"
    val label: String? = null,
    val cameraUrl: String? = null,
    val cameraRefreshInterval: Int = 0,
    val isCustomUrl: Boolean = false,
    val tapAction: String = "toggle",
    val doubleTapAction: String = "more_info",
    val holdAction: String = "more_info",
    // Lock buttons: an optional door/contact sensor whose open state turns the lock card red.
    val doorEntityId: String? = null,
    // Vacuum buttons: how the button renders and which map/battery entities to pull from.
    val vacuumDisplayMode: String = "static",   // "static" | "camera" | "external"
    val vacuumMapEntityId: String? = null,
    val vacuumBatteryEntityId: String? = null,
    val vacuumImageUrl: String? = null,
    // Climate buttons: optional separate temp/humidity sensors, graphed in the entity's Activity tab.
    val climateTempSensorEntityId: String? = null,
    val climateHumiditySensorEntityId: String? = null
)

@Serializable
@SerialName("subtitle")
data class HKISubtitleWidget(
    override val id: String,
    val text: String,
    val icon: String? = null
) : HKIRoomWidget()

@Serializable
@SerialName("weather")
data class HKIWeatherWidget(
    override val id: String,
    val entityId: String? = null,   // null = use the app's default weather entity
    val style: String = "current",  // "current" | "forecast" | "hourly" | "wind" | "rainmap"
    val imageUrl: String? = null,   // rainmap style: external radar/rain map image URL
    val title: String? = null
) : HKIRoomWidget()

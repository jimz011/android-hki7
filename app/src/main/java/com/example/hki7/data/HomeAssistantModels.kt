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

    val icon: String?
        get() = attributes?.get("icon")?.jsonPrimitive?.contentOrNull

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

    // ── media_player domain ──────────────────────────────────────────────────
    val mediaAlbumName: String?
        get() = attributes?.get("media_album_name")?.jsonPrimitive?.contentOrNull
    val mediaDuration: Double?
        get() = attributes?.get("media_duration")?.jsonPrimitive?.doubleOrNull
    val mediaPosition: Double?
        get() = attributes?.get("media_position")?.jsonPrimitive?.doubleOrNull
    val mediaPositionUpdatedAt: String?
        get() = attributes?.get("media_position_updated_at")?.jsonPrimitive?.contentOrNull
    val volumeLevel: Double?
        get() = attributes?.get("volume_level")?.jsonPrimitive?.doubleOrNull
    val isVolumeMuted: Boolean?
        get() = attributes?.get("is_volume_muted")?.jsonPrimitive?.booleanOrNull
    val mediaShuffle: Boolean?
        get() = attributes?.get("shuffle")?.jsonPrimitive?.booleanOrNull
    val mediaRepeat: String?
        get() = attributes?.get("repeat")?.jsonPrimitive?.contentOrNull
    val mediaSource: String?
        get() = attributes?.get("source")?.jsonPrimitive?.contentOrNull
    val sourceList: List<String>
        get() = (attributes?.get("source_list") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    val appName: String?
        get() = attributes?.get("app_name")?.jsonPrimitive?.contentOrNull

    // ── alarm_control_panel domain ──────────────────────────────────────────
    val supportedFeatures: Int
        get() = attributes?.get("supported_features")?.jsonPrimitive?.intOrNull ?: 0
    // null = no code needed, "number" = numeric keypad, "text" = free text
    val alarmCodeFormat: String?
        get() = attributes?.get("code_format")?.jsonPrimitive?.contentOrNull
    val alarmCodeArmRequired: Boolean
        get() = attributes?.get("code_arm_required")?.jsonPrimitive?.booleanOrNull ?: true
}

/** Returns a presentation-only copy with a locally configured friendly name. */
fun HAEntity.withDisplayName(name: String?): HAEntity {
    if (name.isNullOrBlank()) return this
    val updated = (attributes?.toMutableMap() ?: mutableMapOf()).apply {
        put("friendly_name", JsonPrimitive(name.trim()))
    }
    return copy(attributes = JsonObject(updated))
}

@Serializable
data class HAWeatherForecast(
    val datetime: String,
    val condition: String? = null,
    val temperature: Double? = null,
    val templow: Double? = null,
    val precipitation: Double? = null
)

@Serializable
data class HACalendarDateTime(
    val date: String? = null,
    val dateTime: String? = null
)

@Serializable
data class HACalendarEvent(
    val summary: String? = null,
    val start: HACalendarDateTime? = null,
    val end: HACalendarDateTime? = null,
    val description: String? = null,
    val location: String? = null,
    val entityId: String = ""
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
    val compactTiles: Boolean = false,
    val width: String = "full"   // "full" | "half" — size of every room card on this floor
)

@Serializable
data class HAEntityRegistryEntry(
    val entity_id: String,
    val area_id: String? = null,
    val device_id: String? = null,
    val platform: String? = null,
    /** "config" | "diagnostic" | null (= primary control/sensor). */
    val entity_category: String? = null,
    val disabled_by: String? = null,
    val hidden_by: String? = null
)

@Serializable
data class HADeviceRegistryEntry(
    val id: String,
    val area_id: String? = null,
    val name: String? = null,
    val name_by_user: String? = null,
    /** Parent hub device (e.g. inverters report their Envoy here). */
    val via_device_id: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val sw_version: String? = null
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
    val code: String? = null,
    /** select/input_select.select_option */
    val option: String? = null,
    /** number/input_number/text/input_text.set_value (HA coerces numeric strings). */
    val value: String? = null,
    // media_player services
    val volume_level: Float? = null,
    val is_volume_muted: Boolean? = null,
    val shuffle: Boolean? = null,
    val repeat: String? = null,
    val seek_position: Double? = null,
    val source: String? = null,
    val media_content_id: String? = null,
    val media_content_type: String? = null
)

/** One node of a media_player/browse_media tree (playlists, albums, favorites, …). */
@Serializable
data class HAMediaBrowseItem(
    val title: String? = null,
    val media_content_id: String? = null,
    val media_content_type: String? = null,
    val media_class: String? = null,
    val can_play: Boolean = false,
    val can_expand: Boolean = false,
    val thumbnail: String? = null,
    /** Optional provider metadata; many HA media sources omit these fields. */
    val artist: String? = null,
    val duration: Double? = null,
    val children: List<HAMediaBrowseItem> = emptyList()
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

/** One point from the recorder statistics API (per-hour or per-day aggregate). */
data class HAStatPoint(
    val startMs: Long,
    val mean: Float?,
    val change: Float?
)

/** A single entity change streamed from the websocket `state_changed` subscription.
 *  [newState] is null when the entity was removed. */
data class HAStateChange(
    val entityId: String,
    val newState: HAEntity?
)

/** One entry in the on-device notification history (delivered via the websocket push channel).
 *  Non-archived entries are purged 48h after arrival; archived entries are kept forever. */
@Serializable
data class HKINotification(
    val id: String,
    val title: String? = null,
    val message: String,
    val timestamp: Long,
    val tag: String? = null,
    val read: Boolean = false,
    val archived: Boolean = false
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

/** A configurable tap/hold/double-tap action. Modelled on Home Assistant's action config: it can
 *  toggle, open more-info, call an arbitrary service with data, navigate within the app, or open a
 *  URL. [type] "default" defers to the domain-based heuristic in the view model. */
@Serializable
data class HKIAction(
    val type: String = "default",       // default | none | toggle | more_info | call_service | navigate | url
    val service: String? = null,        // "light.turn_on"
    val targetEntityId: String? = null, // service/toggle target; null = the button's own entity
    val data: JsonObject? = null,       // arbitrary service data
    val moreInfoEntityId: String? = null, // more_info of a different entity
    val navigationTarget: String? = null, // "home"|"rooms"|"energy"|"climate"|"security"|"battery"|"room:<areaId>"
    val url: String? = null
)

/** A user-added quick-access button shown in a dialog's nav bar. Each targets an entity and carries
 *  its own tap/hold/double-tap actions. */
@Serializable
data class HKIActionButton(
    val id: String,
    val entityId: String,
    val name: String? = null,
    val icon: String? = null,
    val tapAction: HKIAction = HKIAction(type = "more_info"),
    val holdAction: HKIAction = HKIAction(type = "default"),
    val doubleTapAction: HKIAction = HKIAction(type = "default")
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
    // Structured action overrides (win over the legacy string fields above when non-null).
    val tapActionEx: HKIAction? = null,
    val holdActionEx: HKIAction? = null,
    val doubleTapActionEx: HKIAction? = null,
    // User-added quick-access buttons for this badge's dialog nav bar.
    val customButtons: List<HKIActionButton> = emptyList(),
    // Lock: optional door/contact sensor(s). Legacy single-sensor + per-lock map.
    val doorEntityId: String? = null,
    val doorEntityIds: Map<String, String> = emptyMap(),       // lockEntityId -> door sensor
    // Vacuum: per-entity map camera + battery sensor overrides.
    val vacuumDeviceIds: Map<String, String> = emptyMap(),
    val vacuumMapEntityIds: Map<String, String> = emptyMap(),
    val vacuumBatteryEntityIds: Map<String, String> = emptyMap(),
    val vacuumWaterEntityIds: Map<String, String> = emptyMap(),
    val vacuumEmptyBinEntityIds: Map<String, String> = emptyMap()
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
    val climateConfig: HKIClimateConfig? = null,
    val securityConfig: HKISecurityConfig? = null,
    val batteryConfig: HKIBatteryConfig? = null,
    val vacuumEntityId: String? = null,
    val vacuumMapEntityId: String? = null,
    /** Per-person custom nav-bar buttons for the person dialog, keyed by person entity id. */
    val personButtons: Map<String, List<HKIActionButton>> = emptyMap()
)

@Serializable
data class HKIBatteryConfig(
    val useBatteryNotes: Boolean = false,
    val hiddenEntityIds: List<String> = emptyList(),
    val extraEntityIds: List<String> = emptyList(),
    val extraDeviceIds: List<String> = emptyList(),
    val entityOrder: List<String> = emptyList(),
    val customNames: Map<String, String> = emptyMap()
)

/** Entity bindings for the Security page. Entities are normally discovered from their Home
 * Assistant domain/device class; these lists hold manual additions, removals and user ordering. */
@Serializable
data class HKISecurityConfig(
    val extraEntityIds: Map<String, List<String>> = emptyMap(),
    val hiddenEntityIds: List<String> = emptyList(),
    val entityOrder: List<String> = emptyList(),
    val customNames: Map<String, String> = emptyMap(),
    /** Per-entity MDI icon slug overriding the group's default icon. */
    val customIcons: Map<String, String> = emptyMap(),
    /** Per-camera button config (name, refresh interval), same settings as camera widgets. */
    val cameraConfigs: Map<String, HKIButtonConfig> = emptyMap()
)

/** Entity bindings for the Climate page. Sensors/devices are auto-discovered by domain and
 *  device_class; this config holds the user's manual additions and removals on top of that. */
@Serializable
data class HKIClimateConfig(
    /** Extra sensors added manually, keyed by group ("temperature", "humidity", "pressure", "co2", "air"). */
    val extraSensorIds: Map<String, List<String>> = emptyMap(),
    /** Extra thermostat/AC entities beyond the auto-discovered climate.* domain. */
    val extraClimateIds: List<String> = emptyList(),
    /** Fan entities treated as air purifiers (fans carry no device_class, so the user selects them). */
    val purifierEntityIds: List<String> = emptyList(),
    /** Extra humidifier/dehumidifier entities beyond the auto-discovered humidifier.* domain. */
    val extraHumidifierIds: List<String> = emptyList(),
    /** Entities removed via edit mode; excluded from cards, tiles, graphs and averages. */
    val hiddenEntityIds: List<String> = emptyList(),
    /** Optional user order for climate devices/sensors on detail pages. */
    val entityOrder: List<String> = emptyList(),
    val customNames: Map<String, String> = emptyMap(),
    /** Per-climate-device MDI icon slug overriding the default hvac icon. */
    val customIcons: Map<String, String> = emptyMap(),
    /** Per-climate-device card style on the main page: "card" (default) or "dial". */
    val deviceCardStyles: Map<String, String> = emptyMap(),
    /** Per-device width on the main page: "full", "half", or "third". */
    val deviceCardWidths: Map<String, String> = emptyMap(),
    /** Per-device shape on the main page: "standard" (default) or "square". */
    val deviceCardShapes: Map<String, String> = emptyMap()
)

/** Entity bindings for the Energy dashboard's power-flow visualization. All optional. */
@Serializable
data class HKIEnergyConfig(
    /** True after importing Home Assistant's Energy dashboard preferences; disables class-wide discovery. */
    val usesHomeAssistantEnergyPreferences: Boolean = false,
    val solarPowerEntityId: String? = null,
    val gridPowerEntityId: String? = null,
    val homePowerEntityId: String? = null,
    val batteryPowerEntityId: String? = null,
    val solarEnergyEntityId: String? = null,
    val gridImportEntityId: String? = null,
    val gridExportEntityId: String? = null,
    val energyCostEntityId: String? = null,
    val gridCarbonFootprintEntityId: String? = null,
    val batteryEntityId: String? = null,
    val solarForecastEntityId: String? = null,
    val gasEntityId: String? = null,
    val gasCostEntityId: String? = null,
    val waterEntityId: String? = null,
    val waterCostEntityId: String? = null,
    /** Optional user order for the cards on the main Energy page. */
    val cardOrder: List<String> = emptyList(),
    val customNames: Map<String, String> = emptyMap(),
    /** Power sensors the user tracks as individual devices (shown under Top consumers). */
    val deviceEntityIds: List<String> = emptyList(),
    /** Energy-counter sensors the user adds to Device energy. */
    val energyDeviceEntityIds: List<String> = emptyList(),
    /** Auto-discovered device_class=power sensors explicitly removed by the user. */
    val hiddenPowerDeviceEntityIds: List<String> = emptyList(),
    /** Auto-discovered device_class=energy sensors explicitly removed by the user. */
    val hiddenEnergyDeviceEntityIds: List<String> = emptyList(),
    // HA-style electricity sensors (P1 meter): per-phase power and tariff-split energy counters.
    val powerPhase1EntityId: String? = null,
    val powerPhase2EntityId: String? = null,
    val powerPhase3EntityId: String? = null,
    // Per-phase current (A) and voltage (V), shown on the Electricity tab.
    val currentPhase1EntityId: String? = null,
    val currentPhase2EntityId: String? = null,
    val currentPhase3EntityId: String? = null,
    val voltagePhase1EntityId: String? = null,
    val voltagePhase2EntityId: String? = null,
    val voltagePhase3EntityId: String? = null,
    // Live flow rates for the Gas/Water tiles (falls back to today's total when unset).
    val gasCurrentEntityId: String? = null,
    val waterCurrentEntityId: String? = null,
    val gridImportTariff1EntityId: String? = null,
    val gridImportTariff2EntityId: String? = null,
    val gridExportTariff1EntityId: String? = null,
    val gridExportTariff2EntityId: String? = null,
    // Extended solar sensors + multi-entity forecast (like HA's energy dashboard).
    val solarLast7DaysEntityId: String? = null,
    val solarLifetimeEntityId: String? = null,
    val solarForecastEntityIds: List<String> = emptyList(),
    /** HA device whose entities (per-inverter sensors) are listed on the Solar page. */
    val solarDeviceId: String? = null,
    // Source devices per category: picking one auto-fills the matching entity fields.
    val electricityDeviceId: String? = null,
    val batteryDeviceId: String? = null,
    val carbonDeviceId: String? = null,
    val gasDeviceId: String? = null,
    val waterDeviceId: String? = null
)

@Serializable
sealed class HKIRoomWidget {
    abstract val id: String
    abstract val width: String
}

@Serializable
@SerialName("button_stack")
data class HKIButtonStack(
    override val id: String,
    override val width: String = "full",
    val title: String? = null,
    val icon: String? = null,
    val entityIds: List<String> = emptyList(),
    val columns: Int = 2,
    val showBadge: Boolean = true,
    val isSquare: Boolean = true,
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    val stackType: String = "buttons",
    val cameraUrls: List<String> = emptyList(),
    val cameraAspectRatio: Float = 16f / 9f,
    /** Empty preserves the legacy isSquare behaviour; otherwise "standard", "square", or "tile". */
    val buttonStyle: String = "",
    val buttonConfigs: Map<String, HKIButtonConfig> = emptyMap()
) : HKIRoomWidget()

@Serializable
@SerialName("swiping_stack")
data class HKISwipingStack(
    override val id: String,
    override val width: String = "full",
    val widgets: List<HKIRoomWidget> = emptyList(),
    val isHidden: Boolean = false,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    val autoplay: Boolean = false,
    val autoplayIntervalSeconds: Int = 5,
    val animationDurationMs: Int = 450,
    val animation: String = "swipe"
) : HKIRoomWidget()

@Serializable
@SerialName("empty_stack")
data class HKIEmptyStack(
    override val id: String,
    override val width: String = "full",
    val widgets: List<HKIRoomWidget> = emptyList(),
    val columns: Int = 2,
    val showBadge: Boolean = true,
    val isSquare: Boolean = true,
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null
) : HKIRoomWidget()

@Serializable
@SerialName("single_entity")
data class HKISingleEntityWidget(
    override val id: String,
    override val width: String = "full",
    val entityId: String,
    val kind: String = "button",       // "button" | "camera" | "vacuum"
    val isSquare: Boolean = kind != "camera",
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val cameraAspectRatio: Float = 16f / 9f,
    /** Empty preserves the legacy isSquare behaviour; otherwise "standard", "square", or "tile". */
    val buttonStyle: String = "",
    val config: HKIButtonConfig = HKIButtonConfig()
) : HKIRoomWidget()

@Serializable
data class HKIButtonConfig(
    val name: String? = null,
    val icon: String? = null,
    val spinIcon: Boolean = false,     // rotate the icon continuously while the entity isn't "off"
    val label: String? = null,
    /** Light-only Google Home-style full-height brightness control. */
    val showBrightnessSlider: Boolean = false,
    val cameraUrl: String? = null,
    val cameraRefreshInterval: Int = 5,
    val isCustomUrl: Boolean = false,
    val tapAction: String = "toggle",
    val doubleTapAction: String = "more_info",
    val holdAction: String = "more_info",
    // Structured action overrides (win over the legacy string fields above when non-null).
    val tapActionEx: HKIAction? = null,
    val holdActionEx: HKIAction? = null,
    val doubleTapActionEx: HKIAction? = null,
    // User-added quick-access buttons for this button's dialog nav bar.
    val customButtons: List<HKIActionButton> = emptyList(),
    val lockEnabled: Boolean = false,
    val lockUnlockMode: String = "double_tap", // "double_tap" | "pin"
    val lockPin: String? = null,
    val lockRelockSeconds: Int = 30,
    // Lock buttons: an optional door/contact sensor whose open state turns the lock card red.
    val doorEntityId: String? = null,
    // Vacuum buttons: how the button renders and which map/battery entities to pull from.
    val vacuumDisplayMode: String = "static",   // "static" | "camera" | "external"
    val vacuumDeviceId: String? = null,
    val vacuumMapEntityId: String? = null,
    val vacuumBatteryEntityId: String? = null,
    val vacuumWaterEntityId: String? = null,
    val vacuumEmptyBinEntityId: String? = null,
    val vacuumImageUrl: String? = null,
    // Climate buttons: optional separate temp/humidity sensors, graphed in the entity's Activity tab.
    val climateTempSensorEntityId: String? = null,
    val climateHumiditySensorEntityId: String? = null,
    // Weather stack items: each item is a weather card with its own style/entity/image.
    val weatherStyle: String? = null,           // "current" | "forecast" | "hourly" | "wind" | "rainmap"
    val weatherEntityId: String? = null,        // null = app's default weather entity
    val weatherImageUrl: String? = null          // rainmap style: external radar/rain map image URL
)

@Serializable
@SerialName("subtitle")
data class HKISubtitleWidget(
    override val id: String,
    override val width: String = "full",
    val text: String,
    val icon: String? = null
) : HKIRoomWidget()

/** One card from the Energy view, embeddable on any page. cardKey selects the card (see
 *  energyCardCatalog in EnergyScreen). Data always reflects "today". */
@Serializable
@SerialName("energy_card")
data class HKIEnergyCardWidget(
    override val id: String,
    override val width: String = "full",
    val cardKey: String = "house",
    val title: String? = null,
    val icon: String? = null,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false,
    /** Per-card entity bindings; null inherits the Energy view's settings. */
    val energyConfig: HKIEnergyConfig? = null
) : HKIRoomWidget()

/** A stack of energy cards, collapsible like the other stacks. */
@Serializable
@SerialName("energy_stack")
data class HKIEnergyStack(
    override val id: String,
    override val width: String = "full",
    val title: String? = null,
    val icon: String? = null,
    val cardKeys: List<String> = emptyList(),
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    /** Entity bindings applied to every card in the stack; null inherits the Energy view's settings. */
    val energyConfig: HKIEnergyConfig? = null
) : HKIRoomWidget()

/** One card from the Climate view, embeddable on any page. cardKey selects the card (see
 *  climateCardCatalog in ClimateScreen). */
@Serializable
@SerialName("climate_card")
data class HKIClimateCardWidget(
    override val id: String,
    override val width: String = "full",
    val cardKey: String = "hero",
    val title: String? = null,
    val icon: String? = null,
    val cornerRadius: Int = 28,
    val isSquare: Boolean = false,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false,
    /** Per-card entities; empty inherits the Climate view's auto-discovered entities. */
    val entityIds: List<String> = emptyList()
) : HKIRoomWidget()

/** A stack of climate cards, collapsible like the energy stack. */
@Serializable
@SerialName("climate_stack")
data class HKIClimateStack(
    override val id: String,
    override val width: String = "full",
    val title: String? = null,
    val icon: String? = null,
    val cardKeys: List<String> = emptyList(),
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    /** Entities applied to every card in the stack; empty inherits the Climate view's discovery. */
    val entityIds: List<String> = emptyList()
) : HKIRoomWidget()

/** Media player tile that uses the current album art (entity_picture) as its background. */
@Serializable
@SerialName("media_player")
data class HKIMediaPlayerWidget(
    override val id: String,
    override val width: String = "full",
    val entityId: String,
    val title: String? = null,
    val icon: String? = "speaker",
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    /** Optional background image (URL or HA path); drawn behind the card content. */
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

/** History graph for one or more (numeric) sensors, drawn as lines or bars. */
@Serializable
@SerialName("sensor_graph")
data class HKISensorGraphWidget(
    override val id: String,
    override val width: String = "full",
    val entityIds: List<String> = emptyList(),
    val title: String? = null,
    val icon: String? = null,
    /** "line" = temperature-style line graph, "bar" = energy-style bars. */
    val style: String = "line",
    /** History window in hours (see HistoryRangeOptions). */
    val hours: Int = 24,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

/** A collapsible stack of sensor graphs, like the energy/climate stacks. */
@Serializable
@SerialName("sensor_graph_stack")
data class HKISensorGraphStack(
    override val id: String,
    override val width: String = "full",
    val title: String? = null,
    val icon: String? = null,
    val graphs: List<HKISensorGraphWidget> = emptyList(),
    val cornerRadius: Int = 28,
    val isHidden: Boolean = false,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null
) : HKIRoomWidget()

/** Free-form card whose contents are written in markdown (headings, lists, bold, links, ...). */
@Serializable
@SerialName("markdown")
data class HKIMarkdownWidget(
    override val id: String,
    override val width: String = "full",
    val content: String = "",
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

@Serializable
@SerialName("weather")
data class HKIWeatherWidget(
    override val id: String,
    override val width: String = "full",
    val entityId: String? = null,   // null = use the app's default weather entity
    val style: String = "current",  // "current" | "forecast" | "hourly" | "wind" | "rainmap"
    val imageUrl: String? = null,   // rainmap style: external radar/rain map image URL
    val title: String? = null,
    val icon: String? = null,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null
) : HKIRoomWidget()

@Serializable
@SerialName("calendar")
data class HKICalendarWidget(
    override val id: String,
    override val width: String = "full",
    val entityIds: List<String> = emptyList(),
    val view: String = "agenda",       // "agenda" | "week" | "month"
    val isSquare: Boolean = false,
    val title: String? = null,
    val icon: String? = "calendar-month",
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

/** Waste collection widget (e.g. Afvalbeheer): waste-type sensors whose state/attributes hold the
 *  next pickup date. The card shows the next collection; tapping opens a dialog with every category
 *  and an optional week-calendar overview. */
@Serializable
@SerialName("waste_collection")
data class HKIWasteCollectionWidget(
    override val id: String,
    override val width: String = "full",
    val entityIds: List<String> = emptyList(),
    /** Optional calendar entity shown as a week overview inside the dialog. */
    val calendarEntityId: String? = null,
    val title: String? = "Waste Collection",
    val icon: String? = "trash-can-outline",
    /** "icon" = waste-type MDI icon; "picture" = the sensor's entity_picture. */
    val imageStyle: String = "icon",
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

@Serializable
@SerialName("battery_card")
data class HKIBatteryCardWidget(
    override val id: String,
    override val width: String = "full",
    val title: String? = "Battery Levels",
    val icon: String? = "battery-alert",
    val lowThreshold: Int = 30,
    val useBatteryNotes: Boolean = false,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

/** Carrier-agnostic parcel widget for PostNL, DHL NL, DPD and GLS integration devices. */
@Serializable
@SerialName("parcels")
data class HKIParcelsWidget(
    override val id: String,
    override val width: String = "full",
    val deviceIds: List<String> = emptyList(),
    /** Optional carrier artwork override per HA device; accepts absolute URLs and HA/local paths. */
    val carrierImageUrls: Map<String, String> = emptyMap(),
    val title: String? = "Parcels",
    val icon: String? = "package-variant-closed",
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    val isHidden: Boolean = false
) : HKIRoomWidget()

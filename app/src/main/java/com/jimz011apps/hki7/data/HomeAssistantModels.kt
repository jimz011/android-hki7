@file:Suppress(
    "PropertyName",
    "SpellCheckingInspection",
    "GrazieInspection",
    "unused"
)

package com.jimz011apps.hki7.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Runtime state returned by Home Assistant's lightweight `/api/core/state` endpoint. */
enum class HACoreState {
    NOT_RUNNING,
    STARTING,
    RUNNING,
    STOPPING,
    FINAL_WRITE,
    STOPPED,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): HACoreState =
            entries.firstOrNull { it.name == value?.trim()?.uppercase() } ?: UNKNOWN
    }
}

/** A user-created bottom-navigation page backed by the same widget canvas as Home. */
@Serializable
data class HKICustomPage(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val icon: String = "view-dashboard"
)

@Serializable
@Immutable
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

/** A battery-view entity must be a percentage sensor explicitly classified as a battery by HA. */
fun HAEntity.isBatteryPercentageSensor(): Boolean =
    entity_id.startsWith("sensor.") &&
        deviceClass.equals("battery", ignoreCase = true) &&
        attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull?.trim() == "%"

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
    val columns: Int = 1,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 24,
    val compactTiles: Boolean = true,
    val width: String = "full"   // "full" | "half" — size of every room card on this floor
)

@Serializable
data class HAEntityRegistryEntry(
    val entity_id: String,
    val area_id: String? = null,
    val device_id: String? = null,
    val platform: String? = null,
    /** Config entry that owns this entity, when it was created by an integration. */
    val config_entry_id: String? = null,
    /** Stable integration-defined key; unlike an entity id this survives user renames. */
    val translation_key: String? = null,
    val unique_id: String? = null,
    /** Unit recorded in the entity registry. May be null on older Home Assistant versions. */
    val unit_of_measurement: String? = null,
    /** The user's explicit icon override, stored in the registry rather than the entity state.
     * Home Assistant only exposes this here (state attributes carry an `icon` only for
     * customize:-based overrides), so it must be merged in to honour icons set in HA. */
    val icon: String? = null,
    /** The integration-provided default icon (HA's fallback when there is no user override). */
    val original_icon: String? = null,
    /** "config" | "diagnostic" | null (= primary control/sensor). */
    val entity_category: String? = null,
    val disabled_by: String? = null,
    val hidden_by: String? = null
)

/** Lightweight response model for `config_entries/get`. */
@Serializable
data class HAConfigEntry(
    val entry_id: String,
    val domain: String,
    val title: String = "",
    val state: String? = null,
    val disabled_by: String? = null
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
    /** Source server for multi-instance delivery. Null keeps pre-migration history readable. */
    val instanceId: String? = null,
    val instanceName: String? = null,
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
    /** Media players associated with this room. Supersedes the singular legacy field. */
    val mediaPlayerEntityIds: List<String> = emptyList(),
    /** True after the user changes the room's automatically discovered media-player sources. */
    val mediaPlayersCustomized: Boolean = false,
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
    val badgeBar: HKIBadgeBarConfig? = null,
    /** Entities used for the active-state summary on room cards and room headers, keyed by role. */
    val roomStatusEntityIds: Map<String, List<String>> = emptyMap(),
    /** Legacy single source for the room's current temperature. */
    val roomTemperatureEntityId: String? = null,
    /** Legacy single source for the room's current humidity. */
    val roomHumidityEntityId: String? = null,
    /** Sources averaged for the room's current temperature. Supersedes the singular legacy field. */
    val roomTemperatureEntityIds: List<String> = emptyList(),
    /** Sources averaged for the room's current humidity. Supersedes the singular legacy field. */
    val roomHumidityEntityIds: List<String> = emptyList(),
    /** True after a user changes any of the automatically discovered room sources. */
    val roomEntitiesCustomized: Boolean = false
)

/** One editable field exposed for an action by Home Assistant's get_services command. */
data class HAActionFieldDefinition(
    val key: String,
    val name: String,
    val description: String = "",
    val required: Boolean = false,
    val selector: JsonObject? = null
) {
    val acceptsText: Boolean
        get() = key in setOf("message", "title") ||
            selector?.containsKey("text") == true || selector?.containsKey("template") == true

    val multiline: Boolean
        get() = key == "message" ||
            ((selector?.get("text") as? JsonObject)?.get("multiline") as? JsonPrimitive)
                ?.booleanOrNull == true
}

/** One native Home Assistant service action exposed by the get_services WebSocket command. */
data class HAActionDefinition(
    val key: String,
    val name: String,
    val description: String = "",
    /** Presence means Home Assistant exposes its area/device/entity target selector. */
    val target: JsonObject? = null,
    val fields: List<HAActionFieldDefinition> = emptyList()
) {
    val supportsTarget: Boolean get() = target != null
}

/** A configurable tap/hold/double-tap action. Modeled on Home Assistant's action config: it can
 *  toggle, open more-info, call an arbitrary service with data, navigate within the app, or open a
 *  URL. [type] "default" defers to the domain-based heuristic in the view model. */
@Serializable
data class HKIAction(
    val type: String = "default",       // default | none | toggle | more_info | call_service | navigate | url
    val service: String? = null,        // "light.turn_on"
    val targetEntityId: String? = null, // service/toggle target; null = the button's own entity
    /** owner = the button/badge entity when no explicit target is set; none = omit a target. */
    val targetMode: String = "owner",  // owner | entity | none
    val data: JsonObject? = null,       // arbitrary service data
    val moreInfoEntityId: String? = null, // more_info of a different entity
    val navigationTarget: String? = null, // "home"|"rooms"|"energy"|"climate"|"security"|"battery"|"room:<areaId>"
    val url: String? = null
)

internal fun buildHKIActionServicePayload(action: HKIAction, ownerEntityId: String): JsonObject =
    buildJsonObject {
        val target = when (action.targetMode) {
            "none" -> null
            else -> action.targetEntityId ?: ownerEntityId
        }
        if (!target.isNullOrBlank()) put("entity_id", target)
        action.data?.forEach { (key, value) -> put(key, value) }
    }

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
    /** Optional custom label shown in place of the entity's friendly name when [showName] is on. */
    val customName: String? = null,
    /** Hidden outright until unhidden (edit-mode still shows it so it can be restored). */
    val hidden: Boolean = false,
    /** Optional visibility schedule, same semantics as buttons (see [isVisibleAt]). */
    val visibilityStart: String? = null,
    val visibilityEnd: String? = null,
    val visibilityRangeMode: String = "show",   // "show" (visible within) or "hide" (hidden within)
    val visibilityRecurrence: String = "none",   // none | daily | weekly | monthly | yearly
    /** Optional entity-state condition, same semantics as [HKIButtonConfig.visibilityConditionEntityId]. */
    val visibilityConditionEntityId: String? = null,
    val visibilityConditionState: String? = null,
    val visibilityConditionNegate: Boolean = false,
    /** Combinable visibility rules; supersedes the flat fields above (see [normalizedVisibilityConditions]). */
    val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    val showState: Boolean = true,
    /** When set, the badge shows this attribute's value instead of the entity state. */
    val stateAttribute: String? = null,
    /** Optional unit suffix appended to the shown attribute value (e.g. "°C", "%", "W", "kW"). */
    val stateUnit: String? = null,
    /** Renders the shown value (state or attribute) as a live descending countdown, for entities
     *  whose value is a completion timestamp (washer/dryer/dishwasher "finished at" time). */
    val stateAsTimer: Boolean = false,
    /** Optional machine/operation-state entity gating [stateAsTimer] (only counts down while it reads
     *  running); null gates on the timestamp alone. */
    val timerStateEntityId: String? = null,
    val showIcon: Boolean = true,
    val customIcon: String? = null,
    // Per-icon animation override: "auto" (follow the global setting + domain default), "off",
    // or a forced effect: "glow", "spin", "pulse".
    val iconAnimation: String = "auto",
    val tapAction: String = "auto",    // "auto", "toggle", "more_info"
    val holdAction: String = "auto",   // "auto", "toggle", "more_info"
    // Structured action overrides (win over the legacy string fields above when non-null).
    val tapActionEx: HKIAction? = null,
    val holdActionEx: HKIAction? = null,
    val doubleTapActionEx: HKIAction? = null,
    // User-added quick-access buttons for this badge's dialog nav bar.
    val customButtons: List<HKIActionButton> = emptyList(),
    /** Climate dialog control: "slider" (default) or "dial". */
    val climateDialogControl: String = "slider",
    // Lock: optional door/contact sensor(s). Legacy single-sensor + per-lock map.
    val doorEntityId: String? = null,
    val doorEntityIds: Map<String, String> = emptyMap(),       // lockEntityId -> door sensor
    // Vacuum: per-entity map camera + battery sensor overrides.
    val vacuumDeviceIds: Map<String, String> = emptyMap(),
    val vacuumMapEntityIds: Map<String, String> = emptyMap(),
    val vacuumBatteryEntityIds: Map<String, String> = emptyMap(),
    val vacuumWaterEntityIds: Map<String, String> = emptyMap(),
    val vacuumEmptyBinEntityIds: Map<String, String> = emptyMap(),
    // Humidifier: linked speed control + device-autofilled auxiliary entities (see HKIButtonConfig).
    val humidifierFanEntityId: String? = null,
    val humidifierDeviceId: String? = null,
    val humidifierAuxEntityIds: Map<String, String> = emptyMap()
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
    /** When true, only explicitly selected entities/devices are shown. */
    val manualOnly: Boolean = false,
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
    /** Disable device-class/domain discovery until the user explicitly imports or adds entities. */
    val manualOnly: Boolean = false,
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
    /** Disable device-class/domain discovery until the user explicitly imports or adds entities. */
    val manualOnly: Boolean = false,
    /** Extra sensors added manually, keyed by group ("temperature", "humidity", "pressure", "co2", "air"). */
    val extraSensorIds: Map<String, List<String>> = emptyMap(),
    /** Extra thermostat/AC entities beyond the auto-discovered climate.* domain. */
    val extraClimateIds: List<String> = emptyList(),
    /** Fan entities treated as air purifiers (fans carry no device_class, so the user selects them). */
    val purifierEntityIds: List<String> = emptyList(),
    /** Extra humidifier/dehumidifier entities beyond the auto-discovered humidifier.* domain. */
    val extraHumidifierIds: List<String> = emptyList(),
    /** Optional fan entity linked to a humidifier (keyed by humidifier entity id): supplies the fan
     *  speed options shown in place of the humidifier's modes button. */
    val humidifierFanEntityIds: Map<String, String> = emptyMap(),
    /** Outside sensors (never auto-discovered — we can't tell which sensors are outdoors). These feed
     *  the "Outside" tile/detail page and the hero's outside-temperature subtitle. */
    val outsideTemperatureIds: List<String> = emptyList(),
    val outsideHumidityIds: List<String> = emptyList(),
    val outsidePressureIds: List<String> = emptyList(),
    /** Optional weather.* entity for Outside: its temperature/humidity/pressure attributes are used
     *  directly. Auto-generation fills this with the header pill's weather entity. */
    val outsideWeatherEntityId: String? = null,
    /** Fan entities captured by one-time auto generation. */
    val extraFanIds: List<String> = emptyList(),
    /** Entities removed via edit mode; excluded from cards, tiles, graphs and averages. */
    val hiddenEntityIds: List<String> = emptyList(),
    /** Optional user order for climate devices/sensors on detail pages. */
    val entityOrder: List<String> = emptyList(),
    val customNames: Map<String, String> = emptyMap(),
    /** Per-climate-device MDI icon slug overriding the default hvac icon. */
    val customIcons: Map<String, String> = emptyMap(),
    /** Page-wide thermostat style, overridden only by entries in [deviceCardStyles]. */
    val defaultDeviceCardStyle: String = "dial",
    /** Page-wide thermostat width, overridden only by entries in [deviceCardWidths]. */
    val defaultDeviceCardWidth: String = "half",
    /** Legacy per-device style overrides; the page-wide default is a thermostat dial. */
    val deviceCardStyles: Map<String, String> = emptyMap(),
    /** Per-device width on the main page: "full", "half", or "third". */
    val deviceCardWidths: Map<String, String> = emptyMap(),
    /** Per-device shape on the main page: "standard" (default) or "square". */
    val deviceCardShapes: Map<String, String> = emptyMap()
)

/** Entity bindings for the Energy dashboard's power-flow visualization. All optional. */
@Serializable
data class HKIEnergyConfig(
    /** Start with no inferred energy entities; only explicit selections are shown. */
    val manualOnly: Boolean = false,
    /** True after importing Home Assistant's Energy dashboard preferences; disables class-wide discovery. */
    val usesHomeAssistantEnergyPreferences: Boolean = false,
    /** Migration marker: the HA import also resolved source statistics to their related device entities. */
    val hasImportedRelatedEntities: Boolean = false,
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
    /** Individual water meters configured in Home Assistant's Energy dashboard. */
    val waterDeviceEntityIds: List<String> = emptyList(),
    /** Auto-discovered device_class=power sensors explicitly removed by the user. */
    val hiddenPowerDeviceEntityIds: List<String> = emptyList(),
    /** Auto-discovered device_class=energy sensors explicitly removed by the user. */
    val hiddenEnergyDeviceEntityIds: List<String> = emptyList(),
    /** Individual water meters explicitly removed by the user. */
    val hiddenWaterDeviceEntityIds: List<String> = emptyList(),
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
    /** Home Assistant solar forecast config-entry ids used by energy/solar_forecast. */
    val solarForecastConfigEntryIds: List<String> = emptyList(),
    /** HA device whose entities (per-inverter sensors) are listed on the Solar page. */
    val solarDeviceId: String? = null,
    // Source devices per category: picking one auto-fills the matching entity fields.
    val electricityDeviceId: String? = null,
    val batteryDeviceId: String? = null,
    val carbonDeviceId: String? = null,
    val gasDeviceId: String? = null,
    val waterDeviceId: String? = null,
    /** Energy sensor roles explicitly changed by the user; source-device guesses must preserve them. */
    val customizedEntityRoles: Set<String> = emptySet()
)

/**
 * One visibility rule. A button, badge, or widget holds a list of these plus a match mode
 * ("all" = every block must pass, "any" = at least one must), letting rules be combined rather than
 * limited to one schedule and one entity check.
 *
 * [type] selects which fields apply: [VISIBILITY_TYPE_TIME] uses [start]/[end]/[rangeMode]/
 * [recurrence]; [VISIBILITY_TYPE_ENTITY] uses [entityId]/[state]/[negate].
 */
@Serializable
data class HKIVisibilityCondition(
    val type: String = VISIBILITY_TYPE_ENTITY,
    /** Entity rule: passes while [entityId]'s state does (or, with [negate], doesn't) equal [state]. */
    val entityId: String? = null,
    val state: String? = null,
    val negate: Boolean = false,
    /** Time rule: ISO-8601 local date-time bounds, same semantics as the legacy flat fields. */
    val start: String? = null,
    val end: String? = null,
    val rangeMode: String = "show",
    val recurrence: String = "none",
)

const val VISIBILITY_TYPE_ENTITY = "entity"
const val VISIBILITY_TYPE_TIME = "time"
/** Every block must pass. */
const val VISIBILITY_MATCH_ALL = "all"
/** At least one block must pass. */
const val VISIBILITY_MATCH_ANY = "any"

@Serializable
sealed class HKIRoomWidget {
    abstract val id: String
    abstract val width: String
    /** Whole-widget visibility: the same hide/schedule/recurrence rule buttons and badges use (see
     * [HKIButtonConfig.hidden] and friends, evaluated by [isVisibleAt]/[isWidgetVisibleNow]). */
    abstract val isHidden: Boolean
    abstract val visibilityStart: String?
    abstract val visibilityEnd: String?
    abstract val visibilityRangeMode: String
    abstract val visibilityRecurrence: String
    /** Optional entity-state condition, same semantics as [HKIButtonConfig.visibilityConditionEntityId]. */
    abstract val visibilityConditionEntityId: String?
    abstract val visibilityConditionState: String?
    abstract val visibilityConditionNegate: Boolean
    /** Combinable visibility rules; supersedes the flat fields above, which are kept so dashboards
     * saved before blocks existed keep working (see `normalizedVisibilityConditions`). */
    abstract val visibilityConditions: List<HKIVisibilityCondition>
    abstract val visibilityMatch: String
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
    /** Used by standalone-style widgets such as Adaptive Lighting to hide their heading. */
    val showName: Boolean = true,
    val isSquare: Boolean = true,
    val cornerRadius: Int = 28,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    val stackType: String = "buttons",
    val cameraUrls: List<String> = emptyList(),
    val cameraAspectRatio: Float = 16f / 9f,
    /** Empty preserves the legacy isSquare behavior; otherwise "standard", "square", or "tile". */
    val buttonStyle: String = "",
    val buttonConfigs: Map<String, HKIButtonConfig> = emptyMap(),
    /** Adaptive Lighting config-entry ids exposed by an `adaptive_lighting` stack.
     * Empty means every installed profile; auto-generated room widgets always store an explicit
     * room-scoped list so they cannot operate profiles from another area. */
    val adaptiveLightingProfileIds: List<String> = emptyList(),
    /** Prevents auto-generated room widgets from exposing profiles outside their imported room. */
    val adaptiveLightingRoomScoped: Boolean = false,
    /** "full" shows every control; "double_row" keeps only identity and the two action buttons. */
    val adaptiveLightingLayout: String = "full",
    /** Centers the Adapt now / Pause action row in either Adaptive Lighting layout. */
    val adaptiveLightingCenterActions: Boolean = false
) : HKIRoomWidget()

@Serializable
@SerialName("swiping_stack")
data class HKISwipingStack(
    override val id: String,
    override val width: String = "full",
    /** Optional visible heading; null keeps the legacy header-free presentation outside edit mode. */
    val title: String? = null,
    /** Optional Material Design Icon slug displayed beside the heading. */
    val icon: String? = null,
    val widgets: List<HKIRoomWidget> = emptyList(),
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val collapsible: Boolean = true,
    val defaultCollapsed: Boolean = false,
    val isCollapsed: Boolean? = null,
    val autoplay: Boolean = true,
    val autoplayIntervalSeconds: Int = 3,
    val animationDurationMs: Int = 450,
    val animation: String = "swipe"
) : HKIRoomWidget()

@Serializable
@SerialName("empty_stack")
data class HKIEmptyStack(
    override val id: String,
    override val width: String = "full",
    /** Optional visible heading. Null preserves the legacy "Empty Stack" label. */
    val title: String? = null,
    /** Optional Material Design Icon slug displayed beside the heading. */
    val icon: String? = null,
    val widgets: List<HKIRoomWidget> = emptyList(),
    val columns: Int = 2,
    val showBadge: Boolean = true,
    val isSquare: Boolean = true,
    val cornerRadius: Int = 28,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    val cameraAspectRatio: Float = 16f / 9f,
    /** Empty preserves the legacy isSquare behavior; otherwise "standard", "square", or "tile". */
    val buttonStyle: String = "",
    val config: HKIButtonConfig = HKIButtonConfig()
) : HKIRoomWidget()

@Serializable
data class HKIButtonConfig(
    val name: String? = null,
    val icon: String? = null,
    // Per-icon animation override: "auto" (follow the global setting + domain default), "off",
    // or a forced effect: "glow", "spin", "pulse".
    val iconAnimation: String = "auto",
    /** When set, the button's secondary line shows this entity attribute's value instead of the
     *  state. Null shows the state (the default). */
    val stateAttribute: String? = null,
    /** Optional unit suffix appended to the shown attribute value (e.g. "°C", "%", "W", "kW"). */
    val stateUnit: String? = null,
    /** Renders the shown value (state or attribute) as a live descending countdown, for entities
     *  whose value is a completion timestamp (washer/dryer/dishwasher "finished at" time). */
    val stateAsTimer: Boolean = false,
    /** Optional machine/operation-state entity gating [stateAsTimer]: some integrations keep a stale
     *  future completion time while the appliance is off, so the timer only shows when this entity
     *  reads as running. Null means gate on the timestamp alone. */
    val timerStateEntityId: String? = null,
    /** Optional fan / select / input_select entity that supplies a humidifier's speed options in its
     *  dialog (its modes then live in the dialog's nav bar). */
    val humidifierFanEntityId: String? = null,
    /** Optional HA device whose related entities auto-fill [humidifierAuxEntityIds]. */
    val humidifierDeviceId: String? = null,
    /** Auxiliary humidifier entities shown in its dialog, keyed by slot
     *  (current_humidity, tank_level, pm25, error, bucket_full, clean_filter, defrost, ionizer,
     *  pump, sleep, beep). Auto-filled from [humidifierDeviceId] or set manually. */
    val humidifierAuxEntityIds: Map<String, String> = emptyMap(),
    /** Per-item visibility inside a multi-item widget. [hidden] hides it until unhidden. When
     * [visibilityStart]/[visibilityEnd] (ISO-8601 local date-time, e.g. "2026-12-24T00:00") are set,
     * [visibilityRangeMode] decides whether that window is when the item is shown ("show") or hidden
     * ("hide") — e.g. a Christmas button set to "show" for 24–26 Dec. */
    val hidden: Boolean = false,
    val visibilityStart: String? = null,
    val visibilityEnd: String? = null,
    val visibilityRangeMode: String = "show",
    /** How the window repeats: "none" (the exact dates), or "daily"/"weekly"/"monthly"/"yearly",
     * where only the relevant part of the bounds matters (e.g. yearly ignores the year, so a
     * Christmas button recurs every 24–26 Dec). */
    val visibilityRecurrence: String = "none",
    /** Optional entity-state condition, like a Home Assistant conditional card: when
     * [visibilityConditionEntityId] is set, this item is also gated on whether that entity's current
     * state does/doesn't equal [visibilityConditionState], per [visibilityConditionNegate].
     * Combines (AND) with the hidden/schedule rule above. */
    val visibilityConditionEntityId: String? = null,
    val visibilityConditionState: String? = null,
    val visibilityConditionNegate: Boolean = false,
    /** Combinable visibility rules; supersedes the flat fields above, which are kept so items saved
     * before blocks existed keep working (see [normalizedVisibilityConditions]). */
    val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    /** Climate dialog control: "slider" (default) or "dial". */
    val climateDialogControl: String = "slider",
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
    val icon: String? = null,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    /** Per-line visibility (hide/schedule/condition), keyed by entity id — same rule shape and
     * evaluator as [HKIButtonStack.buttonConfigs]. */
    val itemConfigs: Map<String, HKIButtonConfig> = emptyMap()
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
) : HKIRoomWidget()

@Serializable
@SerialName("iframe")
data class HKIIframeWidget(
    override val id: String,
    override val width: String = "full",
    val url: String = "",
    val title: String? = null,
    val icon: String? = null,
    /** Widget height as an aspect ratio of its width, like the camera widget (1:1, 4:3, 16:9, tall). */
    val aspectRatio: Float = 16f / 9f,
    val cornerRadius: Int = 28,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
) : HKIRoomWidget()

@Serializable
@SerialName("weather")
data class HKIWeatherWidget(
    override val id: String,
    override val width: String = "full",
    val entityId: String? = null,   // null = use the app's default weather entity
    val style: String = "current",  // "current" | "forecast" | "hourly" | "horizon" | "wind" | "rainmap"
    val imageUrl: String? = null,   // rainmap style: external radar/rain map image URL
    val title: String? = null,
    val icon: String? = null,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    /** Per-calendar visibility (hide/schedule/condition), keyed by entity id. */
    val itemConfigs: Map<String, HKIButtonConfig> = emptyMap()
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    /** Per-category visibility (hide/schedule/condition), keyed by sensor entity id. */
    val itemConfigs: Map<String, HKIButtonConfig> = emptyMap()
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
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL
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
    /** Optional display-name override per carrier device. */
    val carrierNames: Map<String, String> = emptyMap(),
    /** Merge selected accounts belonging to the same detected carrier into one carrier tab. */
    val aggregateCarriers: Boolean = false,
    val title: String? = "Parcels",
    val icon: String? = "package-variant-closed",
    val isSquare: Boolean = false,
    val cornerRadius: Int = 28,
    val backgroundUrl: String? = null,
    override val isHidden: Boolean = false,
    override val visibilityStart: String? = null,
    override val visibilityEnd: String? = null,
    override val visibilityRangeMode: String = "show",
    override val visibilityRecurrence: String = "none",
    override val visibilityConditionEntityId: String? = null,
    override val visibilityConditionState: String? = null,
    override val visibilityConditionNegate: Boolean = false,
    override val visibilityConditions: List<HKIVisibilityCondition> = emptyList(),
    override val visibilityMatch: String = VISIBILITY_MATCH_ALL,
    /** Per-carrier-account visibility (hide/schedule/condition), keyed by HA device id. */
    val itemConfigs: Map<String, HKIButtonConfig> = emptyMap()
) : HKIRoomWidget()

/** Identity of the current HA user, as reported by the `hki7/whoami` companion command. */
data class Hki7Identity(
    val userId: String,
    val name: String,
    val isAdmin: Boolean,
    val isOwner: Boolean,
)

/** Metadata for one HA-local backup stored by the `hki7` companion component. */
data class Hki7BackupMeta(
    val id: String,
    val created: String,
    val label: String,
    val size: Int,
)

/** A Home Assistant user, as reported by `hki7/users/list` (admin only). */
data class Hki7User(
    val id: String,
    val name: String,
    val isAdmin: Boolean,
)

/** Metadata for a shared dashboard stored by the `hki7` companion component. */
data class Hki7SharedDashboardMeta(
    val id: String,
    val ownerId: String,
    val name: String,
    val updated: String,
    val sharedWith: List<String>,
)

/** A parental-control policy: the view/room identifiers hidden from a given user. */
data class Hki7Policy(
    val hiddenViews: List<String> = emptyList(),
    val hiddenRooms: List<String> = emptyList(),
    /** Individual button entity ids and badge/widget ids hidden from this user, on top of
     * [hiddenViews]/[hiddenRooms]. A button's id is its entity id; a badge's or widget's id is its
     * own [HKIBadge.id]/[HKIRoomWidget.id] (visible in its Appearance settings). */
    val hiddenItemIds: List<String> = emptyList(),
    /** Whether this user may enter dashboard edit mode at all. */
    val allowEdit: Boolean = true,
    /** When editing is allowed, restrict this user to aesthetic changes (theme, colors, icons,
     * names, wallpaper) and block adding/removing widgets, buttons, and rooms. */
    val aestheticsOnly: Boolean = false,
    /** Whether this user sees the global search action. */
    val showGlobalSearch: Boolean = true,
    /** Whether this user sees the flows (automations) action. */
    val showFlows: Boolean = true,
) {
    val isEmpty: Boolean
        get() = hiddenViews.isEmpty() && hiddenRooms.isEmpty() && hiddenItemIds.isEmpty() &&
            allowEdit && !aestheticsOnly && showGlobalSearch && showFlows
}

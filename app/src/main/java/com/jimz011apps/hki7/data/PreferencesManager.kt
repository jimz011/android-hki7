@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private const val LEGACY_INSTANCE_ID = "legacy-default-instance"

// Tolerates fields removed from saved config data classes across app updates — without this,
// a stale key in a persisted blob throws on decode and the catch-all fallback wipes that store.
// The default deserializer additionally tolerates a widget `type` this app build predates (e.g. a
// family dashboard shared by someone on a newer version): it decodes to HKIUnknownWidget instead of
// failing the whole dashboard, so the rest of a shared dashboard still loads on an older app.
internal val appJson = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        polymorphic(HKIRoomWidget::class) {
            defaultDeserializer { HKIUnknownWidget.serializer() }
        }
    }
}
private inline fun <reified T> decodeBackup(value: String?, fallback: T): T =
    value?.let { runCatching { appJson.decodeFromString<T>(it) }.getOrNull() } ?: fallback

@Serializable
data class HKIDashboard(
    val id: String,
    val name: String,
    val mode: String = "manual",
    val areaOrder: List<String> = emptyList(),
    val areas: List<HAArea> = emptyList(),
    val floors: List<HAFloor> = emptyList(),
    val areaWidgets: Map<String, List<HKIRoomWidget>> = emptyMap(),
    val areaConfigs: Map<String, HKIAreaConfig> = emptyMap(),
    val pageConfigs: Map<String, HKIPageConfig> = emptyMap(),
    val customPages: List<HKICustomPage> = emptyList(),
    /** Popup dialogs this dashboard's buttons/badges can open; their widgets live in [areaWidgets]
     * under [customPopupWidgetAreaId]. */
    val customPopups: List<HKICustomPopup> = emptyList(),
    val navBarOrder: List<String> = emptyList(),
    val navBarHidden: List<String> = emptyList(),
    // Per-dashboard media-player bar config, so each (shared) dashboard carries its own selection
    // instead of one global list everyone must curate.
    val mediaPlayerNames: Map<String, String> = emptyMap(),
    val mediaPlayerBarHidden: List<String> = emptyList(),
    // For an imported shared dashboard ("shared-<id>"): the source's `updated` timestamp last merged
    // in, so auto-update can skip re-fetching an unchanged shared dashboard.
    val sharedUpdatedAt: String? = null,
    /** Header pill settings (weather/alarm/date-time display + their linked entities and rain map).
     * These are otherwise global, so bundling them per-dashboard lets them travel with family sharing.
     * Null on dashboards created before this existed — global prefs are then left untouched. */
    val headerPill: HeaderPillConfig? = null,
)

/** The header display pills (left + right) and their linked entities, captured per-dashboard so they
 * carry across cloud/family sharing (they are otherwise global preferences). */
@Serializable
data class HeaderPillConfig(
    val rightDisplayType: String = "Weather",
    val leftDisplayType: String = "None",
    /** Weather role -> entity id / value: sun, moon, aqi, season, rain, device, rainmap, rainmap_url,
     *  rainmap_aspect. */
    val weatherEntities: Map<String, String> = emptyMap(),
    val rightAlarmEntityIds: List<String> = emptyList(),
    val leftAlarmEntityIds: List<String> = emptyList(),
)

/** Outcome of pruning shared dashboards that were unpublished in the cloud. */
data class SharedPruneResult(
    val removed: Boolean,
    val activeReplaced: Boolean,
    /** No dashboards remain, so the user must choose another permitted dashboard source. */
    val needsDashboardChoice: Boolean,
)

/** One independently authenticated Home Assistant server. The existing preference keys remain the
 * active-instance compatibility layer used by the UI; this record owns the durable credentials,
 * mobile_app registration, routing preferences, and dashboard collection for every home. */
@Serializable
data class HomeAssistantInstance(
    val id: String,
    val name: String,
    val externalUrl: String? = null,
    val internalUrl: String? = null,
    val homeSsids: List<String> = emptyList(),
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val accessTokenExpiry: Long? = null,
    val mobileAppWebhookId: String? = null,
    val mobileAppCloudhookUrl: String? = null,
    val mobileAppRegisteredUrl: String? = null,
    val mobileAppDeviceId: String? = null,
    val mobileAppSensorsWebhookId: String? = null,
    val notificationsEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val dashboards: List<HKIDashboard> = emptyList(),
    val activeDashboardId: String? = null,
    val defaultDashboardId: String? = null,
    val autoGenerationPending: Boolean = false
) {
    val primaryUrl: String? get() = externalUrl?.takeIf(String::isNotBlank)
        ?: internalUrl?.takeIf(String::isNotBlank)
    val isAuthenticated: Boolean get() = !primaryUrl.isNullOrBlank() &&
        (!accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank())
}

@Serializable
private data class HKIUiBackup(
    val version: Int = 1,
    val areaOrder: List<String> = emptyList(),
    val savedAreas: List<HAArea> = emptyList(),
    val savedFloors: List<HAFloor> = emptyList(),
    val areaWidgets: Map<String, List<HKIRoomWidget>> = emptyMap(),
    val areaConfigs: Map<String, HKIAreaConfig> = emptyMap(),
    val pageConfigs: Map<String, HKIPageConfig> = emptyMap(),
    val dashboardMode: String = "auto",
    val customPages: List<HKICustomPage> = emptyList(),
    val customPopups: List<HKICustomPopup> = emptyList(),
    val navBarOrder: List<String> = emptyList(),
    val navBarHidden: List<String> = emptyList(),
    val themeColor: String = "system",
    val themeMode: String = "system",
    val systemLightThemeColor: String = "auto",
    val systemDarkThemeColor: String = "auto",
    val fontScale: Float = 1f,
    val fontWeightAdjust: Int = 0,
    val fontFamily: String = "default",
    val defaultIconPack: String = "mdi",
    val iconAnimationsEnabled: Boolean = false,
    val iconEffectDefaults: Map<String, String> = emptyMap(),
    val itemCornerRadius: Int = 20,
    val forceHighRefreshRate: Boolean = false,
    val weatherEntityId: String? = "weather.home",
    val weatherDisplayType: String = "Weather",
    val headerLeftDisplayType: String = "None",
    val headerVisible: Boolean = true,
    val use24hFormat: Boolean = true,
    val useFullDayName: Boolean = false,
    val sunEntityId: String? = "sun.sun",
    val moonEntityId: String? = "sensor.moon",
    val aqiEntityId: String? = null,
    val seasonEntityId: String? = "sensor.season",
    val rainEntityId: String? = null,
    val weatherDeviceId: String? = null,
    val weatherCardWidths: Map<String, String> = emptyMap(),
    val alarmEntityIds: List<String> = emptyList(),
    val headerLeftAlarmEntityIds: List<String> = emptyList(),
    val alarmPendingSeconds: Int = 0,
    val mediaPlayerNames: Map<String, String> = emptyMap(),
    val mediaPlayerBarHidden: List<String> = emptyList()
)

class PreferencesManager(
    private val context: Context,
    /** Non-null only for background work that must read/write one server without changing the UI. */
    private val instanceScopeId: String? = null
) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val accessTokenExpiryKey = longPreferencesKey("access_token_expiry")
    private val weatherEntityIdKey = stringPreferencesKey("weather_entity_id")
    private val displayNameKey = stringPreferencesKey("display_name")
    private val profileAvatarKey = stringPreferencesKey("profile_avatar")
    private val profileBirthdayKey = stringPreferencesKey("profile_birthday")
    private val profilePersonEntityKey = stringPreferencesKey("profile_person_entity")
    private val areaOrderKey = stringPreferencesKey("area_order")
    private val savedAreasKey = stringPreferencesKey("saved_areas")
    private val savedFloorsKey = stringPreferencesKey("saved_floors")
    private val areaStacksKey = stringPreferencesKey("area_widgets")
    private val areaConfigsKey = stringPreferencesKey("area_configs")
    private val pageConfigsKey = stringPreferencesKey("page_configs")
    private val dashboardModeKey = stringPreferencesKey("dashboard_mode")
    private val weatherDisplayKey = stringPreferencesKey("weather_display_type")
    private val headerLeftDisplayKey = stringPreferencesKey("header_left_display_type")
    private val headerVisibleKey = booleanPreferencesKey("header_visible")
    private val use24hFormatKey = booleanPreferencesKey("use_24h_format")
    private val useFullDayNameKey = booleanPreferencesKey("use_full_day_name")
    private val sunEntityKey = stringPreferencesKey("sun_entity_id")
    private val moonEntityKey = stringPreferencesKey("moon_entity_id")
    private val aqiEntityKey = stringPreferencesKey("aqi_entity_id")
    private val seasonEntityKey = stringPreferencesKey("season_entity_id")
    private val rainEntityKey = stringPreferencesKey("rain_entity_id")
    private val rainMapEntityKey = stringPreferencesKey("weather_rainmap_entity_id")
    private val rainMapUrlKey = stringPreferencesKey("weather_rainmap_url")
    private val rainMapAspectKey = stringPreferencesKey("weather_rainmap_aspect")
    private val weatherDeviceKey = stringPreferencesKey("weather_device_id")
    private val weatherCardWidthsKey = stringPreferencesKey("weather_card_widths")
    private val alarmEntityKey = stringPreferencesKey("header_alarm_entity_id")
    private val headerLeftAlarmEntityKey = stringPreferencesKey("header_left_alarm_entity_id")
    private val alarmPendingSecondsKey = intPreferencesKey("alarm_pending_seconds")
    private val mobileDeviceNameKey = stringPreferencesKey("mobile_device_name")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val systemLightThemeColorKey = stringPreferencesKey("system_light_theme_color")
    private val systemDarkThemeColorKey = stringPreferencesKey("system_dark_theme_color")
    private val forceHighRefreshRateKey = booleanPreferencesKey("force_high_refresh_rate")
    private val fontScaleKey = floatPreferencesKey("font_scale")
    private val fontWeightAdjustKey = intPreferencesKey("font_weight_adjust")
    private val fontFamilyKey = stringPreferencesKey("font_family")
    private val iconPackKey = stringPreferencesKey("default_icon_pack")
    private val iconAnimationsKey = booleanPreferencesKey("icon_animations_enabled")
    private val iconEffectDefaultsKey = stringPreferencesKey("icon_effect_defaults")
    private val itemCornerRadiusKey = intPreferencesKey("item_corner_radius")
    private val mobileAppWebhookIdKey = stringPreferencesKey("mobile_app_webhook_id")
    private val mobileAppCloudhookUrlKey = stringPreferencesKey("mobile_app_cloudhook_url")
    private val mobileAppRegisteredUrlKey = stringPreferencesKey("mobile_app_registered_url")
    private val mobileAppDeviceIdKey = stringPreferencesKey("mobile_app_device_id")
    private val mobileAppSensorsWebhookIdKey = stringPreferencesKey("mobile_app_sensors_webhook")
    private val geocodedAddressKey = stringPreferencesKey("geocoded_address")
    private val geocodedLatKey = doublePreferencesKey("geocoded_lat")
    private val geocodedLonKey = doublePreferencesKey("geocoded_lon")
    private val internalUrlKey = stringPreferencesKey("internal_url")
    private val homeSsidsKey = stringPreferencesKey("home_ssids")
    private val highAccuracyLocationKey = booleanPreferencesKey("high_accuracy_location")
    private val notificationHistoryKey = stringPreferencesKey("notification_history")
    private val roomFollowKey = stringPreferencesKey("room_follow")
    private val roomFollowRosterKey = stringPreferencesKey("room_follow_roster")
    private val backgroundPushKey = booleanPreferencesKey("background_push_enabled")
    private val navBarOrderKey = stringPreferencesKey("nav_bar_order")
    private val navBarHiddenKey = stringPreferencesKey("nav_bar_hidden")
    private val customPagesKey = stringPreferencesKey("custom_pages")
    private val customPopupsKey = stringPreferencesKey("custom_popups")
    private val mediaPlayerNamesKey = stringPreferencesKey("media_player_custom_names")
    private val mediaPlayerBarHiddenKey = stringPreferencesKey("media_player_bar_hidden")
    private val adaptiveLightingProfilesKey = stringPreferencesKey("adaptive_lighting_profiles")
    private val adaptiveLightingOptionsFormsKey = stringPreferencesKey("adaptive_lighting_options_forms_cache")
    private val dashboardsKey = stringPreferencesKey("dashboards_v2")
    private val activeDashboardIdKey = stringPreferencesKey("active_dashboard_id")
    private val defaultDashboardIdKey = stringPreferencesKey("default_dashboard_id")
    private val pendingAutoTakeoverKey = booleanPreferencesKey("pending_auto_takeover")
    private val quickStartGuidePendingKey = booleanPreferencesKey("quick_start_guide_pending")
    // Family subscription state is separate from its permissions: admins can change the latter at
    // any time, while the former remains true until the user leaves the family-dashboard flow.
    private val familyDashboardSubscribedKey = booleanPreferencesKey("family_dashboard_subscribed")
    private val familyDashboardAccessLostKey = booleanPreferencesKey("family_dashboard_access_lost")
    private val legacyFamilyDashboardCreationLockedKey = booleanPreferencesKey("family_dashboard_creation_locked")
    private val cloudBackupEnabledKey = booleanPreferencesKey("cloud_backup_enabled")
    private val haBackupEnabledKey = booleanPreferencesKey("ha_backup_enabled")
    // Wall-clock time (epoch millis) of the most recent successful backup to each destination,
    // recorded by CloudBackupWorker so the settings screen can show "Last backup …" without a
    // network round-trip. 0/absent means none has completed on this device yet.
    private val cloudBackupLastAtKey = longPreferencesKey("cloud_backup_last_at")
    private val haBackupLastAtKey = longPreferencesKey("ha_backup_last_at")
    // Parental-control policy enforced for THIS signed-in user (routes / area ids), cached from the
    // hki7 component so the UI can filter synchronously and keep hiding while briefly offline.
    private val parentalHiddenViewsKey = stringPreferencesKey("parental_hidden_views")
    private val parentalHiddenRoomsKey = stringPreferencesKey("parental_hidden_rooms")
    private val parentalHiddenItemIdsKey = stringPreferencesKey("parental_hidden_item_ids")
    private val parentalVisibleSearchDomainsKey = stringPreferencesKey("parental_visible_search_domains")
    private val parentalVisibleSearchEntityIdsKey = stringPreferencesKey("parental_visible_search_entity_ids")
    private val parentalHiddenSearchDomainsKey = stringPreferencesKey("parental_hidden_search_domains")
    private val parentalHiddenSearchEntityIdsKey = stringPreferencesKey("parental_hidden_search_entity_ids")
    private val parentalAllowEditKey = booleanPreferencesKey("parental_allow_edit")
    private val parentalAestheticsOnlyKey = booleanPreferencesKey("parental_aesthetics_only")
    private val parentalShowSearchKey = booleanPreferencesKey("parental_show_search")
    private val parentalShowFlowsKey = booleanPreferencesKey("parental_show_flows")
    private val parentalAllowDashboardSwitchKey = booleanPreferencesKey("parental_allow_dashboard_switch")
    private val parentalAllowDashboardCreateKey = booleanPreferencesKey("parental_allow_dashboard_create")
    private val parentalAllowReimportKey = booleanPreferencesKey("parental_allow_reimport")
    private val familyDeviceIdKey = stringPreferencesKey("family_device_id")
    private val lastSeenVersionCodeKey = intPreferencesKey("last_seen_version_code")
    private val homeAssistantInstancesKey = stringPreferencesKey("home_assistant_instances_v1")
    private val activeHomeAssistantInstanceIdKey = stringPreferencesKey("active_home_assistant_instance_id")

    val homeAssistantInstances: Flow<List<HomeAssistantInstance>> = context.dataStore.data.map { preferences ->
        instancesFrom(preferences)
    }
    val activeHomeAssistantInstanceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[activeHomeAssistantInstanceIdKey]
            ?: instancesFrom(preferences).firstOrNull()?.id
    }
    val activeHomeAssistantInstance: Flow<HomeAssistantInstance?> = context.dataStore.data.map { preferences ->
        val instances = instancesFrom(preferences)
        val activeId = preferences[activeHomeAssistantInstanceIdKey] ?: instances.firstOrNull()?.id
        instances.firstOrNull { it.id == activeId }
    }
    val shouldUsePushService: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val instances = instancesFrom(preferences)
        val hasEnabledInstance = instances.any { it.notificationsEnabled && it.isAuthenticated }
        hasEnabledInstance && ((preferences[backgroundPushKey] ?: false) || instances.size > 1)
    }

    val serverUrl: Flow<String?> = scopedFlow(serverUrlKey) { it.externalUrl }
    val accessToken: Flow<String?> = scopedFlow(accessTokenKey) { it.accessToken }
    val refreshToken: Flow<String?> = scopedFlow(refreshTokenKey) { it.refreshToken }
    // Absolute wall-clock time (epoch millis) when the current access token expires, if known.
    val accessTokenExpiry: Flow<Long?> = scopedFlow(accessTokenExpiryKey) { it.accessTokenExpiry }
    val weatherEntityId: Flow<String?> = context.dataStore.data.map { it[weatherEntityIdKey] ?: "weather.home" }
    val displayName: Flow<String?> = context.dataStore.data.map { it[displayNameKey] ?: "User" }
    val profileAvatar: Flow<String?> = context.dataStore.data.map { it[profileAvatarKey] }
    val profileBirthday: Flow<String?> = context.dataStore.data.map { it[profileBirthdayKey] }
    val profilePersonEntityId: Flow<String?> = context.dataStore.data.map { it[profilePersonEntityKey] }
    val areaOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[areaOrderKey]?.split(",")?.filter { areaId -> areaId.isNotBlank() } ?: emptyList()
    }
    val dashboardMode: Flow<String> = context.dataStore.data.map { it[dashboardModeKey] ?: "auto" }
    val dashboards: Flow<List<HKIDashboard>> = context.dataStore.data.map { p ->
        decodeBackup(p[dashboardsKey], emptyList())
    }
    val activeDashboardId: Flow<String?> = context.dataStore.data.map { it[activeDashboardIdKey] }
    val defaultDashboardId: Flow<String?> = context.dataStore.data.map { it[defaultDashboardIdKey] }
    val pendingAutoTakeover: Flow<Boolean> = context.dataStore.data.map { it[pendingAutoTakeoverKey] ?: false }
    val quickStartGuidePending: Flow<Boolean> = context.dataStore.data.map { it[quickStartGuidePendingKey] ?: false }
    val familyDashboardSubscribed: Flow<Boolean> = context.dataStore.data.map { p ->
        p[familyDashboardSubscribedKey] == true ||
            p[legacyFamilyDashboardCreationLockedKey] == true ||
            decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).any { it.id.startsWith("shared-") }
    }
    val familyDashboardAccessLost: Flow<Boolean> = context.dataStore.data.map { it[familyDashboardAccessLostKey] ?: false }
    val cloudBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[cloudBackupEnabledKey] ?: false }
    /** Daily backup to the user's own Home Assistant instance via the hki7 companion component. */
    val haBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[haBackupEnabledKey] ?: false }
    /** Epoch millis of the last successful Google Drive backup, or null if none yet. */
    val cloudBackupLastAt: Flow<Long?> = context.dataStore.data.map { it[cloudBackupLastAtKey]?.takeIf { at -> at > 0 } }
    /** Epoch millis of the last successful Home Assistant backup, or null if none yet. */
    val haBackupLastAt: Flow<Long?> = context.dataStore.data.map { it[haBackupLastAtKey]?.takeIf { at -> at > 0 } }
    /** Routes hidden from the current user by an admin's parental-control policy. */
    val parentalHiddenViews: Flow<List<String>> = context.dataStore.data.map {
        it[parentalHiddenViewsKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    /** Area ids hidden from the current user by an admin's parental-control policy. */
    val parentalHiddenRooms: Flow<List<String>> = context.dataStore.data.map {
        it[parentalHiddenRoomsKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    /** Individual button/badge/widget ids hidden from the current user by an admin's policy. */
    val parentalHiddenItemIds: Flow<List<String>> = context.dataStore.data.map {
        it[parentalHiddenItemIdsKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    val parentalVisibleSearchDomains: Flow<List<String>> = context.dataStore.data.map {
        it[parentalVisibleSearchDomainsKey]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
    }
    val parentalVisibleSearchEntityIds: Flow<List<String>> = context.dataStore.data.map {
        it[parentalVisibleSearchEntityIdsKey]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
    }
    val parentalHiddenSearchDomains: Flow<List<String>> = context.dataStore.data.map {
        it[parentalHiddenSearchDomainsKey]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
    }
    val parentalHiddenSearchEntityIds: Flow<List<String>> = context.dataStore.data.map {
        it[parentalHiddenSearchEntityIdsKey]?.split(",")?.filter(String::isNotBlank) ?: emptyList()
    }
    /** Whether the current user may enter dashboard edit mode (admin policy; default allowed). */
    val enforcedAllowEdit: Flow<Boolean> = context.dataStore.data.map { it[parentalAllowEditKey] ?: true }
    /** Whether the current user is restricted to aesthetic-only edits (admin policy). */
    val enforcedAestheticsOnly: Flow<Boolean> = context.dataStore.data.map { it[parentalAestheticsOnlyKey] ?: false }
    /** Whether the current user sees the global search action (admin policy; default shown). */
    val enforcedShowGlobalSearch: Flow<Boolean> = context.dataStore.data.map { it[parentalShowSearchKey] ?: true }
    /** Whether the current user sees the flows action (admin policy; default shown). */
    val enforcedShowFlows: Flow<Boolean> = context.dataStore.data.map { it[parentalShowFlowsKey] ?: true }
    val enforcedAllowDashboardSwitch: Flow<Boolean> = context.dataStore.data.map {
        it[parentalAllowDashboardSwitchKey] ?: true
    }
    val enforcedAllowDashboardCreate: Flow<Boolean> = context.dataStore.data.map {
        it[parentalAllowDashboardCreateKey] ?: true
    }
    val enforcedAllowReimport: Flow<Boolean> = context.dataStore.data.map {
        it[parentalAllowReimportKey] ?: true
    }

    /** Caches the enforced policy for the signed-in user (reset to defaults for admins/owners). */
    suspend fun saveEnforcedPolicy(policy: Hki7Policy) {
        context.dataStore.edit {
            it[parentalHiddenViewsKey] = policy.hiddenViews.joinToString(",")
            it[parentalHiddenRoomsKey] = policy.hiddenRooms.joinToString(",")
            it[parentalHiddenItemIdsKey] = policy.hiddenItemIds.joinToString(",")
            it[parentalVisibleSearchDomainsKey] = policy.visibleSearchDomains.joinToString(",")
            it[parentalVisibleSearchEntityIdsKey] = policy.visibleSearchEntityIds.joinToString(",")
            it[parentalHiddenSearchDomainsKey] = policy.hiddenSearchDomains.joinToString(",")
            it[parentalHiddenSearchEntityIdsKey] = policy.hiddenSearchEntityIds.joinToString(",")
            it[parentalAllowEditKey] = policy.allowEdit
            it[parentalAestheticsOnlyKey] = policy.aestheticsOnly
            it[parentalShowSearchKey] = policy.showGlobalSearch
            it[parentalShowFlowsKey] = policy.showFlows
            it[parentalAllowDashboardSwitchKey] = policy.allowDashboardSwitch
            it[parentalAllowDashboardCreateKey] = policy.allowDashboardCreate
            it[parentalAllowReimportKey] = policy.allowReimport
            it[roomFollowKey] = appJson.encodeToString(policy.roomFollow)
        }
    }

    /** This device owner's room-following settings, cached so the launch navigation can decide
     *  before the component has been reached (and keeps working while offline). */
    val roomFollow: Flow<Hki7RoomFollow> = context.dataStore.data.map { preferences ->
        decodeBackup(preferences[roomFollowKey], Hki7RoomFollow())
    }

    /** The household's room-presence sensors, cached from `hki7/room_follow/roster` so the
     *  people-per-room counters survive a restart before the roster is refetched. */
    val roomFollowRoster: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[roomFollowRosterKey]?.split(",")?.filter { it.isNotBlank() }.orEmpty()
    }

    suspend fun saveRoomFollowRoster(sensors: List<String>) {
        context.dataStore.edit { it[roomFollowRosterKey] = sensors.filter { s -> s.isNotBlank() }.joinToString(",") }
    }

    /** Version code whose changelog the user has already seen; 0 until one has been acknowledged. */
    val lastSeenVersionCode: Flow<Int> = context.dataStore.data.map { it[lastSeenVersionCodeKey] ?: 0 }

    suspend fun saveLastSeenVersionCode(versionCode: Int) {
        context.dataStore.edit { it[lastSeenVersionCodeKey] = versionCode }
    }

    suspend fun saveCloudBackup(enabled: Boolean) {
        context.dataStore.edit { it[cloudBackupEnabledKey] = enabled }
    }

    suspend fun saveHaBackup(enabled: Boolean) {
        context.dataStore.edit { it[haBackupEnabledKey] = enabled }
    }

    /** Records the wall-clock time a Google Drive backup last completed successfully. */
    suspend fun saveCloudBackupLastAt(epochMillis: Long) {
        context.dataStore.edit { it[cloudBackupLastAtKey] = epochMillis }
    }

    /** Records the wall-clock time a Home Assistant backup last completed successfully. */
    suspend fun saveHaBackupLastAt(epochMillis: Long) {
        context.dataStore.edit { it[haBackupLastAtKey] = epochMillis }
    }

    val savedAreas: Flow<List<HAArea>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[savedAreasKey] ?: "[]"
        try { appJson.decodeFromString<List<HAArea>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    val savedFloors: Flow<List<HAFloor>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[savedFloorsKey] ?: "[]"
        try { appJson.decodeFromString<List<HAFloor>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    val areaWidgets: Flow<Map<String, List<HKIRoomWidget>>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[areaStacksKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, List<HKIRoomWidget>>>(jsonStr) } catch (_: Exception) { emptyMap() }
    }

    val areaConfigs: Flow<Map<String, HKIAreaConfig>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[areaConfigsKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, HKIAreaConfig>>(jsonStr) } catch (_: Exception) { emptyMap() }
    }
    val pageConfigs: Flow<Map<String, HKIPageConfig>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[pageConfigsKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, HKIPageConfig>>(jsonStr) } catch (_: Exception) { emptyMap() }
    }

    val weatherDisplayType: Flow<String> = context.dataStore.data.map { it[weatherDisplayKey] ?: "Weather" }
    val headerLeftDisplayType: Flow<String> = context.dataStore.data.map { it[headerLeftDisplayKey] ?: "None" }
    val headerVisible: Flow<Boolean> = context.dataStore.data.map { it[headerVisibleKey] ?: true }
    val use24hFormat: Flow<Boolean> = context.dataStore.data.map { it[use24hFormatKey] ?: true }
    val useFullDayName: Flow<Boolean> = context.dataStore.data.map { it[useFullDayNameKey] ?: false }

    val sunEntityId: Flow<String?> = context.dataStore.data.map { it[sunEntityKey] ?: "sun.sun" }
    val moonEntityId: Flow<String?> = context.dataStore.data.map { it[moonEntityKey] ?: "sensor.moon" }
    val aqiEntityId: Flow<String?> = context.dataStore.data.map { it[aqiEntityKey] }
    val seasonEntityId: Flow<String?> = context.dataStore.data.map { it[seasonEntityKey] ?: "sensor.season" }
    val rainEntityId: Flow<String?> = context.dataStore.data.map { it[rainEntityKey] }
    /** Optional camera entity + web (iframe) URL shown as the weather dialog's rain-map card. */
    val rainMapEntityId: Flow<String?> = context.dataStore.data.map { it[rainMapEntityKey] }
    val rainMapUrl: Flow<String?> = context.dataStore.data.map { it[rainMapUrlKey] }
    val rainMapAspect: Flow<String?> = context.dataStore.data.map { it[rainMapAspectKey] }
    /** HA device from which the weather page's role entities were filled automatically. */
    val weatherDeviceId: Flow<String?> = context.dataStore.data.map { it[weatherDeviceKey] }
    /** Widths for the cards in the global weather dialog: "third", "half", or "full". */
    val weatherCardWidths: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val saved = preferences[weatherCardWidthsKey] ?: "{}"
        runCatching { appJson.decodeFromString<Map<String, String>>(saved) }.getOrDefault(emptyMap())
    }
    val alarmEntityIds: Flow<List<String>> = context.dataStore.data.map { p ->
        p[alarmEntityKey]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    val headerLeftAlarmEntityIds: Flow<List<String>> = context.dataStore.data.map { p ->
        p[headerLeftAlarmEntityKey]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    val alarmPendingSeconds: Flow<Int> = context.dataStore.data.map { it[alarmPendingSecondsKey] ?: 0 }
    val mobileDeviceName: Flow<String?> = context.dataStore.data.map { it[mobileDeviceNameKey] }
    val themeColor: Flow<String> = context.dataStore.data.map { it[themeColorKey] ?: "system" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[themeModeKey] ?: "system" }
    val systemLightThemeColor: Flow<String> = context.dataStore.data.map { it[systemLightThemeColorKey] ?: "auto" }
    val systemDarkThemeColor: Flow<String> = context.dataStore.data.map { it[systemDarkThemeColorKey] ?: "auto" }
    // When true, lock the window to the panel's highest mode regardless of the system peak-rate setting.
    val forceHighRefreshRate: Flow<Boolean> = context.dataStore.data.map { it[forceHighRefreshRateKey] ?: false }

    // App-wide typography: size multiplier, weight offset (-200..300 in steps of 100) and font
    // family key ("default", "sans", "serif", "monospace", "cursive").
    val fontScale: Flow<Float> = context.dataStore.data.map { it[fontScaleKey] ?: 1f }
    val fontWeightAdjust: Flow<Int> = context.dataStore.data.map { it[fontWeightAdjustKey] ?: 0 }
    val fontFamily: Flow<String> = context.dataStore.data.map { it[fontFamilyKey] ?: "default" }
    /** The icon pack pre-selected in the icon picker when adding a new icon (id: "mdi"/"si"). */
    val defaultIconPack: Flow<String> = context.dataStore.data.map { it[iconPackKey] ?: "mdi" }
    /** Whether entity icons animate while the device is active (glow/spin/pulse). New installs are
     * seeded on during onboarding (see saveInitialConnectionDetails); the fallback stays false so
     * pre-existing users who never opted in are not flipped on. */
    val iconAnimationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[iconAnimationsKey] ?: false }
    /** User-chosen default effect per icon group (group id → "glow"/"spin"/"pulse"/"none"). */
    val iconEffectDefaults: Flow<Map<String, String>> = context.dataStore.data.map {
        decodeBackup(it[iconEffectDefaultsKey], emptyMap())
    }
    val itemCornerRadius: Flow<Int> = context.dataStore.data.map { it[itemCornerRadiusKey] ?: 20 }

    // mobile_app integration registration (persistent device_tracker + sensors via webhook).
    val mobileAppWebhookId: Flow<String?> = scopedFlow(mobileAppWebhookIdKey) { it.mobileAppWebhookId }
    val mobileAppCloudhookUrl: Flow<String?> = scopedFlow(mobileAppCloudhookUrlKey) { it.mobileAppCloudhookUrl }
    val mobileAppRegisteredUrl: Flow<String?> = scopedFlow(mobileAppRegisteredUrlKey) { it.mobileAppRegisteredUrl }
    val mobileAppDeviceId: Flow<String?> = scopedFlow(mobileAppDeviceIdKey) { it.mobileAppDeviceId }
    // The webhook id for which the mobile_app sensors have already been registered (so we register
    // them once, not on every report). A new webhook id (re-registration) forces a re-register.
    val mobileAppSensorsWebhookId: Flow<String?> = scopedFlow(mobileAppSensorsWebhookIdKey) { it.mobileAppSensorsWebhookId }
    // Last successful reverse-geocode (address + the fix it was resolved for), persisted so a fresh
    // process reuses it instead of re-querying the Geocoder for a device that hasn't moved.
    val geocodedAddress: Flow<String?> = context.dataStore.data.map { it[geocodedAddressKey] }
    val geocodedLat: Flow<Double?> = context.dataStore.data.map { it[geocodedLatKey] }
    val geocodedLon: Flow<Double?> = context.dataStore.data.map { it[geocodedLonKey] }

    // Local HA URL used when connected to one of the configured home Wi-Fi SSIDs; otherwise serverUrl.
    val internalUrl: Flow<String?> = scopedFlow(internalUrlKey) { it.internalUrl }
    val homeSsids: Flow<List<String>> = context.dataStore.data.map { preferences ->
        if (instanceScopeId == null) {
            preferences[homeSsidsKey]?.split(",")?.map { ssid -> ssid.trim() }
                ?.filter { ssid -> ssid.isNotBlank() } ?: emptyList()
        } else {
            instancesFrom(preferences).firstOrNull { it.id == instanceScopeId }?.homeSsids.orEmpty()
        }
    }
    // When true, location updates much more frequently (every few seconds) at the cost of battery.
    val highAccuracyLocation: Flow<Boolean> = context.dataStore.data.map { it[highAccuracyLocationKey] ?: false }

    // Notification history delivered via the websocket push channel (newest first, capped).
    val notificationHistory: Flow<List<HKINotification>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[notificationHistoryKey] ?: "[]"
        try { appJson.decodeFromString<List<HKINotification>>(jsonStr) } catch (_: Exception) { emptyList() }
    }
    // When true, a foreground service keeps the push websocket alive while the app is closed
    // (the official app's "persistent connection"; uses more battery).
    val backgroundPushEnabled: Flow<Boolean> = context.dataStore.data.map { it[backgroundPushKey] ?: false }

    // Bottom navigation bar layout. Order lists the reorderable (non-fixed) tab routes; hidden lists
    // the routes the user turned off. Empty means "use defaults" (see NavBarConfig).
    val navBarOrder: Flow<List<String>> = context.dataStore.data.map {
        it[navBarOrderKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    val navBarHidden: Flow<List<String>> = context.dataStore.data.map {
        it[navBarHiddenKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    val customPages: Flow<List<HKICustomPage>> = context.dataStore.data.map { preferences ->
        val saved = preferences[customPagesKey] ?: "[]"
        runCatching { appJson.decodeFromString<List<HKICustomPage>>(saved) }.getOrDefault(emptyList())
    }
    val customPopups: Flow<List<HKICustomPopup>> = context.dataStore.data.map { preferences ->
        val saved = preferences[customPopupsKey] ?: "[]"
        runCatching { appJson.decodeFromString<List<HKICustomPopup>>(saved) }.getOrDefault(emptyList())
    }

    // Media players: local display names and which players may show the mini player bar.
    val mediaPlayerCustomNames: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[mediaPlayerNamesKey] ?: return@map emptyMap()
        try { appJson.decodeFromString<Map<String, String>>(jsonStr) } catch (_: Exception) { emptyMap() }
    }
    val mediaPlayerBarHidden: Flow<List<String>> = context.dataStore.data.map {
        it[mediaPlayerBarHiddenKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }
    /** User-confirmed light/group to Adaptive Lighting config-entry associations. Config-entry ids
     * remain stable when users rename the integration's switch entities. */
    val adaptiveLightingProfiles: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        decodeBackup(preferences[adaptiveLightingProfilesKey], emptyMap())
    }
    /** Read-only cache of Adaptive Lighting's HA-owned options forms. This is deliberately not
     * included in dashboard backups; HA remains the source of truth and refreshes it on connect. */
    val adaptiveLightingOptionsForms: Flow<Map<String, JsonObject>> = context.dataStore.data.map { preferences ->
        decodeBackup(preferences[adaptiveLightingOptionsFormsKey], emptyMap())
    }

    /** Creates a view of this store pinned to one server. Used by background services so updating a
     * webhook or refreshed token never switches (or otherwise mutates) the foreground dashboard. */
    fun forInstance(instanceId: String): PreferencesManager = PreferencesManager(context, instanceId)

    internal val scopeId: String? get() = instanceScopeId

    private fun <T> scopedFlow(
        legacyKey: Preferences.Key<T>,
        selector: (HomeAssistantInstance) -> T?
    ): Flow<T?> = context.dataStore.data.map { preferences ->
        if (instanceScopeId == null) preferences[legacyKey]
        else instancesFrom(preferences).firstOrNull { it.id == instanceScopeId }?.let(selector)
    }

    private fun storedInstances(preferences: Preferences): List<HomeAssistantInstance> =
        decodeBackup(preferences[homeAssistantInstancesKey], emptyList())

    private fun instancesFrom(preferences: Preferences): List<HomeAssistantInstance> {
        val stored = storedInstances(preferences)
        if (stored.isNotEmpty()) return stored
        return legacyInstanceFrom(preferences)?.let(::listOf).orEmpty()
    }

    private fun legacyInstanceFrom(preferences: Preferences): HomeAssistantInstance? {
        val external = preferences[serverUrlKey]?.takeIf(String::isNotBlank)
        val internal = preferences[internalUrlKey]?.takeIf(String::isNotBlank)
        if (external == null && internal == null) return null
        val primary = external ?: internal.orEmpty()
        val demo = isDemoServerUrl(primary)
        return instanceFromPreferences(
            preferences = preferences,
            base = HomeAssistantInstance(
                id = LEGACY_INSTANCE_ID,
                name = instanceNameFromUrl(primary),
                notificationsEnabled = !demo,
                locationEnabled = !demo
            )
        )
    }

    private fun instanceFromPreferences(
        preferences: Preferences,
        base: HomeAssistantInstance
    ): HomeAssistantInstance {
        val dashboards = decodeBackup<List<HKIDashboard>>(preferences[dashboardsKey], base.dashboards)
            .toMutableList()
        val activeDashboard = preferences[activeDashboardIdKey] ?: base.activeDashboardId
        val activeIndex = dashboards.indexOfFirst { it.id == activeDashboard }
        if (activeIndex >= 0) {
            dashboards[activeIndex] = dashboardFromPreferences(
                preferences,
                dashboards[activeIndex].id,
                dashboards[activeIndex].name
            )
        }
        return base.copy(
            externalUrl = preferences[serverUrlKey]?.takeIf(String::isNotBlank),
            internalUrl = preferences[internalUrlKey]?.takeIf(String::isNotBlank),
            homeSsids = preferences[homeSsidsKey]?.split(',')
                ?.map(String::trim)?.filter(String::isNotBlank).orEmpty(),
            accessToken = preferences[accessTokenKey],
            refreshToken = preferences[refreshTokenKey],
            accessTokenExpiry = preferences[accessTokenExpiryKey],
            mobileAppWebhookId = preferences[mobileAppWebhookIdKey],
            mobileAppCloudhookUrl = preferences[mobileAppCloudhookUrlKey],
            mobileAppRegisteredUrl = preferences[mobileAppRegisteredUrlKey],
            mobileAppDeviceId = preferences[mobileAppDeviceIdKey],
            mobileAppSensorsWebhookId = preferences[mobileAppSensorsWebhookIdKey],
            dashboards = dashboards,
            activeDashboardId = activeDashboard,
            defaultDashboardId = preferences[defaultDashboardIdKey] ?: base.defaultDashboardId,
            autoGenerationPending = preferences[pendingAutoTakeoverKey] ?: false
        )
    }

    private fun saveInstances(preferences: MutablePreferences, instances: List<HomeAssistantInstance>) {
        preferences[homeAssistantInstancesKey] = appJson.encodeToString(instances)
    }

    private fun updateScopedInstance(
        preferences: MutablePreferences,
        transform: (HomeAssistantInstance) -> HomeAssistantInstance
    ): Boolean {
        val id = instanceScopeId ?: return false
        val instances = instancesFrom(preferences).toMutableList()
        val index = instances.indexOfFirst { it.id == id }
        if (index < 0) return true
        instances[index] = transform(instances[index])
        saveInstances(preferences, instances)
        if (preferences[activeHomeAssistantInstanceIdKey] == id) {
            loadInstanceConnectionIntoPreferences(preferences, instances[index])
        }
        return true
    }

    private fun snapshotActiveInstance(preferences: MutablePreferences) {
        val instances = instancesFrom(preferences).toMutableList()
        val activeId = preferences[activeHomeAssistantInstanceIdKey]
            ?: instances.firstOrNull()?.id
            ?: return
        val index = instances.indexOfFirst { it.id == activeId }
        val base = instances.getOrNull(index) ?: return
        val updated = instanceFromPreferences(preferences, base)
        if (index >= 0) instances[index] = updated else instances += updated
        saveInstances(preferences, instances)
        preferences[activeHomeAssistantInstanceIdKey] = activeId
    }

    private fun loadInstanceConnectionIntoPreferences(
        preferences: MutablePreferences,
        instance: HomeAssistantInstance
    ) {
        fun setOrRemove(key: Preferences.Key<String>, value: String?) {
            if (value.isNullOrBlank()) preferences.remove(key) else preferences[key] = value
        }
        setOrRemove(serverUrlKey, instance.externalUrl)
        setOrRemove(internalUrlKey, instance.internalUrl)
        setOrRemove(accessTokenKey, instance.accessToken)
        setOrRemove(refreshTokenKey, instance.refreshToken)
        instance.accessTokenExpiry?.let { preferences[accessTokenExpiryKey] = it }
            ?: preferences.remove(accessTokenExpiryKey)
        if (instance.homeSsids.isEmpty()) preferences.remove(homeSsidsKey)
        else preferences[homeSsidsKey] = instance.homeSsids.joinToString(",")
        setOrRemove(mobileAppWebhookIdKey, instance.mobileAppWebhookId)
        setOrRemove(mobileAppCloudhookUrlKey, instance.mobileAppCloudhookUrl)
        setOrRemove(mobileAppRegisteredUrlKey, instance.mobileAppRegisteredUrl)
        setOrRemove(mobileAppDeviceIdKey, instance.mobileAppDeviceId)
        setOrRemove(mobileAppSensorsWebhookIdKey, instance.mobileAppSensorsWebhookId)
    }

    private fun loadInstanceIntoPreferences(
        preferences: MutablePreferences,
        instance: HomeAssistantInstance
    ) {
        loadInstanceConnectionIntoPreferences(preferences, instance)

        val dashboards = instance.dashboards.ifEmpty {
            val id = UUID.randomUUID().toString()
            listOf(HKIDashboard(id = id, name = "Default (auto generated)", mode = "auto"))
        }
        val activeId = instance.activeDashboardId?.takeIf { id -> dashboards.any { it.id == id } }
            ?: dashboards.first().id
        val defaultId = instance.defaultDashboardId?.takeIf { id -> dashboards.any { it.id == id } }
            ?: activeId
        preferences[dashboardsKey] = appJson.encodeToString(dashboards)
        preferences[activeDashboardIdKey] = activeId
        preferences[defaultDashboardIdKey] = defaultId
        loadDashboardIntoPreferences(preferences, dashboards.first { it.id == activeId })
        if (dashboards.any { it.id.startsWith("shared-") }) preferences[familyDashboardSubscribedKey] = true
        else preferences.remove(familyDashboardSubscribedKey)
        preferences.remove(familyDashboardAccessLostKey)
        preferences.remove(legacyFamilyDashboardCreationLockedKey)
        if (instance.autoGenerationPending) preferences[pendingAutoTakeoverKey] = true
        else preferences.remove(pendingAutoTakeoverKey)
        preferences[activeHomeAssistantInstanceIdKey] = instance.id
    }

    /** Persists the original single-server settings as the first instance without changing any
     * existing URL, token, dashboard, or onboarding state. Safe to call on every launch. */
    suspend fun ensureHomeAssistantInstanceStore() {
        if (instanceScopeId != null) return
        context.dataStore.edit { preferences ->
            if (storedInstances(preferences).isNotEmpty()) return@edit
            val legacy = legacyInstanceFrom(preferences) ?: return@edit
            saveInstances(preferences, listOf(legacy))
            preferences[activeHomeAssistantInstanceIdKey] = legacy.id
        }
    }

    /** Adds and selects a newly authenticated server. Its dashboard starts in auto-generation mode;
     * the existing instance is snapshotted first so switching back restores it exactly. */
    suspend fun addHomeAssistantInstance(
        url: String,
        token: String,
        refresh: String? = null,
        expiresInSeconds: Int? = null,
        requestedName: String? = null
    ): String {
        require(instanceScopeId == null) { "Cannot add an instance from a scoped preference store" }
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            if (storedInstances(preferences).isEmpty()) {
                legacyInstanceFrom(preferences)?.let { legacy ->
                    saveInstances(preferences, listOf(legacy))
                    preferences[activeHomeAssistantInstanceIdKey] = legacy.id
                }
            }
            snapshotActiveInstance(preferences)
            val instances = storedInstances(preferences).toMutableList()
            val urls = splitHomeAssistantConnectionUrl(url)
            val baseName = requestedName?.trim()?.takeIf(String::isNotBlank)
                ?: instanceNameFromUrl(url)
            val usedNames = instances.map { it.name.lowercase() }.toSet()
            var name = baseName
            var suffix = 2
            while (name.lowercase() in usedNames) name = "$baseName ${suffix++}"
            val dashboardId = UUID.randomUUID().toString()
            val created = HomeAssistantInstance(
                id = newId,
                name = name,
                externalUrl = urls.external,
                internalUrl = urls.internal,
                accessToken = token,
                refreshToken = refresh,
                accessTokenExpiry = expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L },
                dashboards = listOf(
                    HKIDashboard(
                        id = dashboardId,
                        name = "Default (auto generated)",
                        mode = "auto"
                    )
                ),
                activeDashboardId = dashboardId,
                defaultDashboardId = dashboardId,
                autoGenerationPending = true
            )
            instances += created
            saveInstances(preferences, instances)
            loadInstanceIntoPreferences(preferences, created)
        }
        return newId
    }

    suspend fun switchHomeAssistantInstance(instanceId: String): Boolean {
        if (instanceScopeId != null) return false
        var switched = false
        context.dataStore.edit { preferences ->
            val current = preferences[activeHomeAssistantInstanceIdKey]
            if (current == instanceId) return@edit
            if (storedInstances(preferences).isEmpty()) {
                legacyInstanceFrom(preferences)?.let { saveInstances(preferences, listOf(it)) }
            }
            snapshotActiveInstance(preferences)
            val target = storedInstances(preferences).firstOrNull { it.id == instanceId } ?: return@edit
            loadInstanceIntoPreferences(preferences, target)
            switched = true
        }
        return switched
    }

    suspend fun renameHomeAssistantInstance(instanceId: String, name: String) {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return
        context.dataStore.edit { preferences ->
            val instances = instancesFrom(preferences).map {
                if (it.id == instanceId) it.copy(name = cleaned) else it
            }
            saveInstances(preferences, instances)
        }
    }

    suspend fun setHomeAssistantInstanceCapabilities(
        instanceId: String,
        notificationsEnabled: Boolean? = null,
        locationEnabled: Boolean? = null
    ) {
        context.dataStore.edit { preferences ->
            val instances = instancesFrom(preferences).map {
                if (it.id == instanceId) it.copy(
                    notificationsEnabled = notificationsEnabled ?: it.notificationsEnabled,
                    locationEnabled = locationEnabled ?: it.locationEnabled
                ) else it
            }
            saveInstances(preferences, instances)
        }
    }

    suspend fun removeHomeAssistantInstance(instanceId: String): Boolean {
        if (instanceScopeId != null) return false
        var removed = false
        context.dataStore.edit { preferences ->
            snapshotActiveInstance(preferences)
            val instances = storedInstances(preferences).toMutableList()
            if (instances.size <= 1 || instances.none { it.id == instanceId }) return@edit
            val wasActive = preferences[activeHomeAssistantInstanceIdKey] == instanceId
            instances.removeAll { it.id == instanceId }
            saveInstances(preferences, instances)
            if (wasActive) loadInstanceIntoPreferences(preferences, instances.first())
            removed = true
        }
        return removed
    }

    private fun instanceNameFromUrl(url: String): String {
        val host = runCatching { java.net.URI(url).host }.getOrNull()
            ?.removePrefix("www.")?.substringBefore('.')
            ?.replace('-', ' ')?.replace('_', ' ')
            ?.trim()?.takeIf(String::isNotBlank)
        return host?.replaceFirstChar { it.uppercase() } ?: "Home"
    }

    suspend fun saveConnectionDetails(url: String, token: String, refresh: String? = null, expiresInSeconds: Int? = null) {
        val connectionUrls = splitHomeAssistantConnectionUrl(url)
        context.dataStore.edit { preferences ->
            if (updateScopedInstance(preferences) { instance ->
                    instance.copy(
                        externalUrl = connectionUrls.external ?: instance.externalUrl,
                        internalUrl = connectionUrls.internal ?: instance.internalUrl,
                        accessToken = token,
                        refreshToken = refresh ?: instance.refreshToken,
                        accessTokenExpiry = expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
                            ?: instance.accessTokenExpiry
                    )
                }
            ) return@edit
            connectionUrls.external?.let { external ->
                preferences[serverUrlKey] = external
                if (!preferences[mobileAppWebhookIdKey].isNullOrBlank()) {
                    preferences[mobileAppRegisteredUrlKey] = external
                }
            }
            connectionUrls.internal?.let { internal ->
                preferences[internalUrlKey] = internal
                if (preferences[serverUrlKey]?.trim()?.trimEnd('/')
                        .equals(internal, ignoreCase = true)
                ) {
                    preferences.remove(serverUrlKey)
                }
            }
            preferences[accessTokenKey] = token
            if (refresh != null) preferences[refreshTokenKey] = refresh
            if (expiresInSeconds != null) {
                preferences[accessTokenExpiryKey] = System.currentTimeMillis() + expiresInSeconds * 1000L
            }
            snapshotActiveInstance(preferences)
        }
    }

    /** Updates the auth tokens without touching the stored server URL (used by token refresh,
     *  which may be talking to the resolved internal URL at the time). */
    suspend fun saveAuthTokens(token: String, refresh: String? = null, expiresInSeconds: Int? = null) {
        context.dataStore.edit { preferences ->
            if (updateScopedInstance(preferences) { instance ->
                    instance.copy(
                        accessToken = token,
                        refreshToken = refresh ?: instance.refreshToken,
                        accessTokenExpiry = expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
                            ?: instance.accessTokenExpiry
                    )
                }
            ) return@edit
            preferences[accessTokenKey] = token
            if (refresh != null) preferences[refreshTokenKey] = refresh
            if (expiresInSeconds != null) {
                preferences[accessTokenExpiryKey] = System.currentTimeMillis() + expiresInSeconds * 1000L
            }
            snapshotActiveInstance(preferences)
        }
    }

    suspend fun saveWeatherEntity(entityId: String) { context.dataStore.edit { it[weatherEntityIdKey] = entityId } }
    suspend fun saveDisplayName(name: String) { context.dataStore.edit { it[displayNameKey] = name } }
    suspend fun saveProfileAvatar(uri: String?) { context.dataStore.edit { if (uri.isNullOrBlank()) it.remove(profileAvatarKey) else it[profileAvatarKey] = uri } }
    suspend fun saveProfileBirthday(birthday: String?) { context.dataStore.edit { if (birthday.isNullOrBlank()) it.remove(profileBirthdayKey) else it[profileBirthdayKey] = birthday } }
    suspend fun saveProfilePersonEntityId(entityId: String?) { context.dataStore.edit { if (entityId.isNullOrBlank()) it.remove(profilePersonEntityKey) else it[profilePersonEntityKey] = entityId } }

    suspend fun exportUiBackup(): String {
        val p = context.dataStore.data.first()
        fun strings(key: Preferences.Key<String>): List<String> = p[key]?.split(',')?.filter(String::isNotBlank).orEmpty()
        return appJson.encodeToString(HKIUiBackup(
            areaOrder = strings(areaOrderKey),
            savedAreas = decodeBackup(p[savedAreasKey], emptyList()),
            savedFloors = decodeBackup(p[savedFloorsKey], emptyList()),
            areaWidgets = decodeBackup(p[areaStacksKey], emptyMap()),
            areaConfigs = decodeBackup(p[areaConfigsKey], emptyMap()),
            pageConfigs = decodeBackup(p[pageConfigsKey], emptyMap()),
            dashboardMode = p[dashboardModeKey] ?: "auto",
            customPages = decodeBackup(p[customPagesKey], emptyList()),
            customPopups = decodeBackup(p[customPopupsKey], emptyList()),
            navBarOrder = strings(navBarOrderKey),
            navBarHidden = strings(navBarHiddenKey),
            themeColor = p[themeColorKey] ?: "system",
            themeMode = p[themeModeKey] ?: "system",
            systemLightThemeColor = p[systemLightThemeColorKey] ?: "auto",
            systemDarkThemeColor = p[systemDarkThemeColorKey] ?: "auto",
            fontScale = p[fontScaleKey] ?: 1f,
            fontWeightAdjust = p[fontWeightAdjustKey] ?: 0,
            fontFamily = p[fontFamilyKey] ?: "default",
            defaultIconPack = p[iconPackKey] ?: "mdi",
            iconAnimationsEnabled = p[iconAnimationsKey] ?: false,
            iconEffectDefaults = decodeBackup(p[iconEffectDefaultsKey], emptyMap()),
            itemCornerRadius = p[itemCornerRadiusKey] ?: 20,
            forceHighRefreshRate = p[forceHighRefreshRateKey] ?: false,
            weatherEntityId = p[weatherEntityIdKey],
            weatherDisplayType = p[weatherDisplayKey] ?: "Weather",
            headerLeftDisplayType = p[headerLeftDisplayKey] ?: "None",
            headerVisible = p[headerVisibleKey] ?: true,
            use24hFormat = p[use24hFormatKey] ?: true,
            useFullDayName = p[useFullDayNameKey] ?: false,
            sunEntityId = p[sunEntityKey],
            moonEntityId = p[moonEntityKey],
            aqiEntityId = p[aqiEntityKey],
            seasonEntityId = p[seasonEntityKey],
            rainEntityId = p[rainEntityKey],
            weatherDeviceId = p[weatherDeviceKey],
            weatherCardWidths = decodeBackup(p[weatherCardWidthsKey], emptyMap()),
            alarmEntityIds = strings(alarmEntityKey),
            headerLeftAlarmEntityIds = strings(headerLeftAlarmEntityKey),
            alarmPendingSeconds = p[alarmPendingSecondsKey] ?: 0,
            mediaPlayerNames = decodeBackup(p[mediaPlayerNamesKey], emptyMap()),
            mediaPlayerBarHidden = strings(mediaPlayerBarHiddenKey)
        ))
    }

    suspend fun restoreUiBackup(raw: String) {
        check(familyDashboardSwitchAllowed()) { "Dashboard restore is disabled by family permissions" }
        val backup = appJson.decodeFromString<HKIUiBackup>(raw)
        require(backup.version == 1) { "Unsupported backup version ${backup.version}" }
        context.dataStore.edit { p ->
            p[areaOrderKey] = backup.areaOrder.joinToString(",")
            p[savedAreasKey] = appJson.encodeToString(backup.savedAreas)
            p[savedFloorsKey] = appJson.encodeToString(backup.savedFloors)
            p[areaStacksKey] = appJson.encodeToString(backup.areaWidgets)
            p[areaConfigsKey] = appJson.encodeToString(backup.areaConfigs)
            p[pageConfigsKey] = appJson.encodeToString(backup.pageConfigs)
            p[dashboardModeKey] = backup.dashboardMode
            p[customPagesKey] = appJson.encodeToString(backup.customPages)
            p[customPopupsKey] = appJson.encodeToString(backup.customPopups)
            p[navBarOrderKey] = backup.navBarOrder.joinToString(",")
            p[navBarHiddenKey] = backup.navBarHidden.joinToString(",")
            p[themeColorKey] = backup.themeColor
            p[themeModeKey] = backup.themeMode
            p[systemLightThemeColorKey] = backup.systemLightThemeColor
            p[systemDarkThemeColorKey] = backup.systemDarkThemeColor
            p[fontScaleKey] = backup.fontScale
            p[fontWeightAdjustKey] = backup.fontWeightAdjust
            p[fontFamilyKey] = backup.fontFamily
            p[iconPackKey] = backup.defaultIconPack
            p[iconAnimationsKey] = backup.iconAnimationsEnabled
            p[iconEffectDefaultsKey] = appJson.encodeToString(backup.iconEffectDefaults)
            p[itemCornerRadiusKey] = backup.itemCornerRadius
            p[forceHighRefreshRateKey] = backup.forceHighRefreshRate
            fun setOptional(key: Preferences.Key<String>, value: String?) {
                if (value.isNullOrBlank()) p.remove(key) else p[key] = value
            }
            setOptional(weatherEntityIdKey, backup.weatherEntityId)
            p[weatherDisplayKey] = backup.weatherDisplayType
            p[headerLeftDisplayKey] = backup.headerLeftDisplayType
            p[headerVisibleKey] = backup.headerVisible
            p[use24hFormatKey] = backup.use24hFormat
            p[useFullDayNameKey] = backup.useFullDayName
            setOptional(sunEntityKey, backup.sunEntityId)
            setOptional(moonEntityKey, backup.moonEntityId)
            setOptional(aqiEntityKey, backup.aqiEntityId)
            setOptional(seasonEntityKey, backup.seasonEntityId)
            setOptional(rainEntityKey, backup.rainEntityId)
            setOptional(weatherDeviceKey, backup.weatherDeviceId)
            p[weatherCardWidthsKey] = appJson.encodeToString(backup.weatherCardWidths)
            p[alarmEntityKey] = backup.alarmEntityIds.joinToString(",")
            p[headerLeftAlarmEntityKey] = backup.headerLeftAlarmEntityIds.joinToString(",")
            p[alarmPendingSecondsKey] = backup.alarmPendingSeconds
            p[mediaPlayerNamesKey] = appJson.encodeToString(backup.mediaPlayerNames)
            p[mediaPlayerBarHiddenKey] = backup.mediaPlayerBarHidden.joinToString(",")
        }
    }

    /** Saves the first endpoint into exactly one slot. Local onboarding starts internal-only;
     * remote/Nabu Casa onboarding starts external-only. */
    suspend fun saveInitialConnectionDetails(
        url: String,
        token: String,
        refresh: String? = null,
        expiresInSeconds: Int? = null
    ) {
        val connectionUrls = splitHomeAssistantConnectionUrl(url)
        context.dataStore.edit { preferences ->
            connectionUrls.external?.let { preferences[serverUrlKey] = it }
                ?: preferences.remove(serverUrlKey)
            connectionUrls.internal?.let { preferences[internalUrlKey] = it }
                ?: preferences.remove(internalUrlKey)
            preferences[accessTokenKey] = token
            if (refresh != null) preferences[refreshTokenKey] = refresh
            if (expiresInSeconds != null) {
                preferences[accessTokenExpiryKey] = System.currentTimeMillis() + expiresInSeconds * 1000L
            }
            // New installs default to animated icon effects. This runs only during first-time
            // onboarding (re-login uses saveConnectionDetails), so existing users who left the
            // toggle off are never flipped on.
            if (!preferences.contains(iconAnimationsKey)) preferences[iconAnimationsKey] = true
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveAreaOrder(order: List<String>) { context.dataStore.edit { it[areaOrderKey] = order.joinToString(",") } }
    /** Prevents the eagerly-created view model from running an auto import while first-run
     * onboarding is still waiting for the user's dashboard choice. */
    suspend fun prepareForInitialDashboardChoice() {
        context.dataStore.edit { p ->
            p[dashboardModeKey] = "manual"
            p.remove(pendingAutoTakeoverKey)
            p.remove(quickStartGuidePendingKey)
        }
    }

    /** Applies the first-run choice in one DataStore transaction, so observers can never see an
     * auto mode paired with the empty-dashboard page configuration (or vice versa). */
    suspend fun configureInitialDashboard(autoGenerate: Boolean) {
        context.dataStore.edit { p ->
            p.remove(areaOrderKey)
            p[savedAreasKey] = "[]"
            p[savedFloorsKey] = "[]"
            p[areaStacksKey] = "{}"
            p[areaConfigsKey] = "{}"
            p[dashboardModeKey] = if (autoGenerate) "auto" else "manual"
            val pageConfigs = if (autoGenerate) emptyMap() else manualOnlyPageConfigs()
            p[pageConfigsKey] = appJson.encodeToString(pageConfigs)
            if (autoGenerate) p[pendingAutoTakeoverKey] = true else p.remove(pendingAutoTakeoverKey)
            p[quickStartGuidePendingKey] = true

            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val activeId = p[activeDashboardIdKey] ?: dashboards.firstOrNull()?.id ?: UUID.randomUUID().toString()
            val name = if (autoGenerate) "Default (auto generated)" else "Default"
            val configured = dashboardFromPreferences(p, activeId, name)
            val index = dashboards.indexOfFirst { it.id == activeId }
            if (index >= 0) dashboards[index] = configured else dashboards += configured
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            p[activeDashboardIdKey] = activeId
            p[defaultDashboardIdKey] = activeId
            snapshotActiveInstance(p)
        }
    }

    /** Marks the post-onboarding gesture guide as acknowledged. Authentication-only logout keeps
     * this preference; a full reset clears it along with the rest of DataStore. */
    suspend fun acknowledgeQuickStartGuide() {
        context.dataStore.edit { it.remove(quickStartGuidePendingKey) }
    }

    /** This install's stable id for family device reporting, created on first use.
     *
     * Deliberately not the mobile_app device id: that one only exists once the app has registered
     * for location or notifications, and a phone with both switched off must still be able to
     * report which HKI version it runs. A reinstall gets a new id, which is why the component caps
     * how many it remembers per user. */
    suspend fun familyDeviceId(): String {
        var id: String? = null
        context.dataStore.edit { p ->
            id = p[familyDeviceIdKey] ?: UUID.randomUUID().toString().also { p[familyDeviceIdKey] = it }
        }
        return id.orEmpty()
    }

    suspend fun markFamilyDashboardSubscribed() {
        context.dataStore.edit {
            it[familyDashboardSubscribedKey] = true
            it.remove(familyDashboardAccessLostKey)
            it.remove(legacyFamilyDashboardCreationLockedKey)
        }
    }

    /** Leaves family management after choosing a permitted standalone dashboard source. */
    suspend fun clearFamilyDashboardSubscription() {
        context.dataStore.edit {
            it.remove(familyDashboardSubscribedKey)
            it.remove(familyDashboardAccessLostKey)
            it.remove(legacyFamilyDashboardCreationLockedKey)
        }
    }

    private suspend fun familyDashboardCreateAllowed(): Boolean =
        !familyDashboardSubscribed.first() || enforcedAllowDashboardCreate.first()

    private suspend fun familyDashboardSwitchAllowed(): Boolean =
        !familyDashboardSubscribed.first() || enforcedAllowDashboardSwitch.first()

    /** Migrates the original single dashboard in place, without changing what is currently loaded. */
    suspend fun ensureDashboardStore(defaultName: String = "Default") {
        context.dataStore.edit { p ->
            if (p[familyDashboardAccessLostKey] == true) return@edit
            val existing = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList())
            if (existing.isNotEmpty()) {
                if (p[activeDashboardIdKey].isNullOrBlank()) p[activeDashboardIdKey] = existing.first().id
                if (p[defaultDashboardIdKey].isNullOrBlank()) p[defaultDashboardIdKey] = existing.first().id
                return@edit
            }
            val id = UUID.randomUUID().toString()
            val dashboard = dashboardFromPreferences(p, id, defaultName)
            p[dashboardsKey] = appJson.encodeToString(listOf(dashboard))
            p[activeDashboardIdKey] = id
            p[defaultDashboardIdKey] = id
        }
    }

    suspend fun createDashboard(name: String, autoGenerate: Boolean): String? {
        if (!familyDashboardCreateAllowed()) return null
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            saveLoadedDashboardInto(p, dashboards)
            val created = HKIDashboard(
                id = id,
                name = name.trim().ifBlank { "Dashboard" },
                mode = if (autoGenerate) "auto" else "manual",
                pageConfigs = if (autoGenerate) emptyMap() else manualOnlyPageConfigs()
            )
            dashboards += created
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            p[activeDashboardIdKey] = id
            if (p[defaultDashboardIdKey].isNullOrBlank()) p[defaultDashboardIdKey] = id
            loadDashboardIntoPreferences(p, created)
            if (autoGenerate) p[pendingAutoTakeoverKey] = true else p.remove(pendingAutoTakeoverKey)
        }
        return id
    }

    /** Onboarding: adopt an already-imported shared dashboard as the initial active + default
     * dashboard and finish first-run setup. */
    suspend fun useSharedDashboardAsInitial(localId: String, discardOtherDashboards: Boolean = false) {
        context.dataStore.edit { p ->
            var dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList())
            val target = dashboards.firstOrNull { it.id == localId } ?: return@edit
            if (discardOtherDashboards) {
                dashboards = listOf(target)
                p[dashboardsKey] = appJson.encodeToString(dashboards)
            }
            p[activeDashboardIdKey] = localId
            p[defaultDashboardIdKey] = localId
            p.remove(pendingAutoTakeoverKey)
            p[quickStartGuidePendingKey] = true
            loadDashboardIntoPreferences(p, target)
        }
    }

    /** Onboarding: commits the UI values just restored from a backup into the dashboard store and
     * makes that restored dashboard active/default. Without this, the eager blank onboarding
     * dashboard could overwrite the restored live preferences on the next dashboard switch. */
    suspend fun useRestoredBackupAsInitial(defaultName: String = "Restored dashboard") {
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val activeId = p[activeDashboardIdKey]
                ?: p[defaultDashboardIdKey]
                ?: dashboards.firstOrNull()?.id
                ?: UUID.randomUUID().toString()
            val existingName = dashboards.firstOrNull { it.id == activeId }?.name
            val restored = dashboardFromPreferences(p, activeId, existingName ?: defaultName)
            val index = dashboards.indexOfFirst { it.id == activeId }
            if (index >= 0) dashboards[index] = restored else dashboards += restored
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            p[activeDashboardIdKey] = activeId
            p[defaultDashboardIdKey] = activeId
            p.remove(pendingAutoTakeoverKey)
            p[quickStartGuidePendingKey] = true
            snapshotActiveInstance(p)
        }
    }

    /** Serialises one stored dashboard to JSON for family sharing. Snapshots the live-loaded state
     * first so the currently-open dashboard is captured up to date. Null if the id is unknown. */
    suspend fun exportDashboard(id: String): String? {
        var result: String? = null
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            saveLoadedDashboardInto(p, dashboards)
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            result = dashboards.firstOrNull { it.id == id }?.let { appJson.encodeToString(it) }
        }
        return result
    }

    /** Imports a shared dashboard JSON as a local dashboard. Keyed on [sharedId] so re-importing the
     * same shared dashboard updates it in place instead of creating duplicates. Returns the local
     * dashboard id, or null if the JSON is invalid. Never switches the active dashboard. */
    suspend fun importSharedDashboard(
        sharedId: String,
        json: String,
        nameOverride: String? = null,
        updatedAt: String? = null,
    ): String? {
        val decoded = runCatching { appJson.decodeFromString<HKIDashboard>(json) }.getOrNull() ?: return null
        val localId = "shared-$sharedId"
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            saveLoadedDashboardInto(p, dashboards)
            val imported = decoded.copy(
                id = localId,
                name = (nameOverride ?: decoded.name).trim().ifBlank { decoded.name },
                sharedUpdatedAt = updatedAt
            )
            val idx = dashboards.indexOfFirst { it.id == localId }
            if (idx >= 0) dashboards[idx] = imported else dashboards += imported
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            if (p[defaultDashboardIdKey].isNullOrBlank()) p[defaultDashboardIdKey] = localId
        }
        return localId
    }

    /** Merges a fresh shared-dashboard payload onto the local copy (preserving the recipient's
     * aesthetics) and stores it. When the merged dashboard is the active one, its live-loaded state is
     * refreshed too. Returns true when the active dashboard was the one updated, so the caller can
     * reload the in-memory view. No-op if [localId] isn't a stored dashboard or the JSON is invalid. */
    suspend fun applySharedDashboardUpdate(localId: String, json: String, updatedAt: String): Boolean {
        val incoming = runCatching { appJson.decodeFromString<HKIDashboard>(json) }.getOrNull() ?: return false
        var activeUpdated = false
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val idx = dashboards.indexOfFirst { it.id == localId }
            if (idx < 0) return@edit
            val isActive = p[activeDashboardIdKey] == localId
            // Snapshot the live-loaded active dashboard so its latest local aesthetics feed the merge.
            if (isActive) saveLoadedDashboardInto(p, dashboards)
            val local = dashboards[idx]
            val merged = mergeSharedDashboardAesthetics(local, incoming).copy(sharedUpdatedAt = updatedAt)
            // No-op when the merge changes nothing but the timestamp: just record the timestamp so we
            // stop re-fetching, without rewriting/reloading the active dashboard (which would flash a
            // needless "revert" even though the content is identical).
            if (merged.copy(sharedUpdatedAt = null) == local.copy(sharedUpdatedAt = null)) {
                dashboards[idx] = local.copy(sharedUpdatedAt = updatedAt)
                p[dashboardsKey] = appJson.encodeToString(dashboards)
                return@edit
            }
            dashboards[idx] = merged
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            if (isActive) {
                loadDashboardIntoPreferences(p, merged)
                activeUpdated = true
            }
        }
        return activeUpdated
    }

    /** Removes imported shared dashboards ("shared-*") whose source is no longer among [presentLocalIds]
     * (the admin unpublished them, or stopped sharing them with this user). If the active dashboard was
     * removed, clears the active selection and signals [SharedPruneResult.needsDashboardChoice] so the
     * host can reopen the dashboard chooser instead of switching or creating something implicitly —
     * and wipes this device's dashboard customization with it, so nothing the user layered on the
     * dashboard they just lost survives into the next one an admin shares with them. */
    suspend fun pruneUnpublishedSharedDashboards(presentLocalIds: Set<String>): SharedPruneResult {
        var result = SharedPruneResult(removed = false, activeReplaced = false, needsDashboardChoice = false)
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val removedIds = dashboards
                .filter { it.id.startsWith("shared-") && it.id !in presentLocalIds }
                .map { it.id }
                .toSet()
            if (removedIds.isEmpty()) return@edit
            val activeId = p[activeDashboardIdKey]
            // Keep a surviving active dashboard's latest local edits before rewriting the list.
            saveLoadedDashboardInto(p, dashboards)
            dashboards.removeAll { it.id in removedIds }
            val removedActive = activeId != null && activeId in removedIds
            if (dashboards.isEmpty()) {
                p.remove(activeDashboardIdKey)
                p.remove(defaultDashboardIdKey)
                p[familyDashboardAccessLostKey] = true
                clearDashboardCustomization(p)
                p[dashboardsKey] = appJson.encodeToString(dashboards)
                result = SharedPruneResult(removed = true, activeReplaced = removedActive, needsDashboardChoice = true)
                return@edit
            }
            // Losing the dashboard currently granting family access is never an implicit switch.
            // Block the app and return to the permission-aware chooser even when local dashboards
            // survive; the user must deliberately choose an allowed next source.
            if (removedActive) {
                p.remove(activeDashboardIdKey)
                p.remove(defaultDashboardIdKey)
                p[familyDashboardAccessLostKey] = true
                // The live keys still hold the revoked dashboard's contents and styling. Surviving
                // dashboards keep their own copies in `dashboards`, and loading one rewrites these.
                clearDashboardCustomization(p)
                p[dashboardsKey] = appJson.encodeToString(dashboards)
                result = SharedPruneResult(removed = true, activeReplaced = false, needsDashboardChoice = true)
                return@edit
            }
            if (p[defaultDashboardIdKey] in removedIds) p[defaultDashboardIdKey] = dashboards.first().id
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            result = SharedPruneResult(removed = true, activeReplaced = false, needsDashboardChoice = false)
        }
        return result
    }

    /** Duplicates a stored dashboard as a new one, copying every widget, area, and page config. The
     * live-loaded active dashboard is snapshotted first so duplicating it (or duplicating while it has
     * unsaved edits) captures its current state. The active dashboard is left unchanged. Returns the
     * new dashboard id, or null if the source id is unknown. */
    suspend fun copyDashboard(id: String, name: String): String? {
        if (!familyDashboardCreateAllowed()) return null
        var newId: String? = null
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            saveLoadedDashboardInto(p, dashboards)
            val source = dashboards.firstOrNull { it.id == id } ?: return@edit
            val copyId = UUID.randomUUID().toString()
            val copy = source.copy(
                id = copyId,
                name = name.trim().ifBlank { "${source.name} copy" }
            )
            dashboards += copy
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            newId = copyId
        }
        return newId
    }

    suspend fun switchDashboard(id: String): Boolean {
        if (!familyDashboardSwitchAllowed()) return false
        var switched = false
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val target = dashboards.firstOrNull { it.id == id } ?: return@edit
            saveLoadedDashboardInto(p, dashboards)
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            p[activeDashboardIdKey] = id
            loadDashboardIntoPreferences(p, target)
            p.remove(pendingAutoTakeoverKey)
            switched = true
        }
        return switched
    }

    suspend fun renameDashboard(id: String, name: String) {
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList())
            p[dashboardsKey] = appJson.encodeToString(dashboards.map {
                if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it
            })
        }
    }

    suspend fun deleteDashboard(id: String): Boolean {
        var deleted = false
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            if (dashboards.size <= 1 || dashboards.none { it.id == id }) return@edit
            saveLoadedDashboardInto(p, dashboards)
            dashboards.removeAll { it.id == id }
            val replacement = dashboards.firstOrNull { it.id == p[defaultDashboardIdKey] } ?: dashboards.first()
            if (p[defaultDashboardIdKey] == id) p[defaultDashboardIdKey] = replacement.id
            if (p[activeDashboardIdKey] == id) {
                p[activeDashboardIdKey] = replacement.id
                loadDashboardIntoPreferences(p, replacement)
            }
            p[dashboardsKey] = appJson.encodeToString(dashboards)
            deleted = true
        }
        return deleted
    }

    suspend fun setDefaultDashboard(id: String) {
        if (!familyDashboardSwitchAllowed()) return
        context.dataStore.edit { p ->
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList())
            if (dashboards.any { it.id == id }) p[defaultDashboardIdKey] = id
        }
    }

    suspend fun finishAutoGeneration(name: String = "Default (auto generated)") {
        context.dataStore.edit { p ->
            p[dashboardModeKey] = "manual"
            p.remove(pendingAutoTakeoverKey)
            val dashboards = decodeBackup<List<HKIDashboard>>(p[dashboardsKey], emptyList()).toMutableList()
            val activeId = p[activeDashboardIdKey] ?: return@edit
            val index = dashboards.indexOfFirst { it.id == activeId }
            if (index >= 0) dashboards[index] = dashboardFromPreferences(p, activeId, name).copy(mode = "manual")
            p[dashboardsKey] = appJson.encodeToString(dashboards)
        }
    }

    private fun dashboardFromPreferences(p: Preferences, id: String, name: String): HKIDashboard = HKIDashboard(
        id = id,
        name = name,
        mode = p[dashboardModeKey] ?: "manual",
        areaOrder = p[areaOrderKey]?.split(',')?.filter(String::isNotBlank).orEmpty(),
        areas = decodeBackup(p[savedAreasKey], emptyList()),
        floors = decodeBackup(p[savedFloorsKey], emptyList()),
        areaWidgets = decodeBackup(p[areaStacksKey], emptyMap()),
        areaConfigs = decodeBackup(p[areaConfigsKey], emptyMap()),
        pageConfigs = decodeBackup(p[pageConfigsKey], emptyMap()),
        customPages = decodeBackup(p[customPagesKey], emptyList()),
        customPopups = decodeBackup(p[customPopupsKey], emptyList()),
        navBarOrder = p[navBarOrderKey]?.split(',')?.filter(String::isNotBlank).orEmpty(),
        navBarHidden = p[navBarHiddenKey]?.split(',')?.filter(String::isNotBlank).orEmpty(),
        mediaPlayerNames = decodeBackup(p[mediaPlayerNamesKey], emptyMap()),
        mediaPlayerBarHidden = p[mediaPlayerBarHiddenKey]?.split(',')?.filter(String::isNotBlank).orEmpty(),
        headerPill = headerPillFromPreferences(p)
    )

    /** Role -> preference key for the header weather pill's linked entities and rain map. */
    private val headerWeatherRoleKeys: Map<String, androidx.datastore.preferences.core.Preferences.Key<String>>
        get() = mapOf(
            "sun" to sunEntityKey, "moon" to moonEntityKey, "aqi" to aqiEntityKey,
            "season" to seasonEntityKey, "rain" to rainEntityKey, "device" to weatherDeviceKey,
            "rainmap" to rainMapEntityKey, "rainmap_url" to rainMapUrlKey, "rainmap_aspect" to rainMapAspectKey
        )

    private fun headerPillFromPreferences(p: Preferences): HeaderPillConfig = HeaderPillConfig(
        rightDisplayType = p[weatherDisplayKey] ?: "Weather",
        leftDisplayType = p[headerLeftDisplayKey] ?: "None",
        weatherEntities = headerWeatherRoleKeys.mapNotNull { (role, key) -> p[key]?.let { role to it } }.toMap(),
        rightAlarmEntityIds = p[alarmEntityKey]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        leftAlarmEntityIds = p[headerLeftAlarmEntityKey]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    )

    private fun applyHeaderPill(p: MutablePreferences, hp: HeaderPillConfig) {
        p[weatherDisplayKey] = hp.rightDisplayType
        p[headerLeftDisplayKey] = hp.leftDisplayType
        headerWeatherRoleKeys.forEach { (role, key) ->
            val value = hp.weatherEntities[role]
            if (value.isNullOrBlank()) p.remove(key) else p[key] = value
        }
        p[alarmEntityKey] = hp.rightAlarmEntityIds.joinToString(",")
        p[headerLeftAlarmEntityKey] = hp.leftAlarmEntityIds.joinToString(",")
    }

    private fun saveLoadedDashboardInto(p: MutablePreferences, dashboards: MutableList<HKIDashboard>) {
        val activeId = p[activeDashboardIdKey] ?: return
        val index = dashboards.indexOfFirst { it.id == activeId }
        if (index >= 0) dashboards[index] = dashboardFromPreferences(p, activeId, dashboards[index].name)
    }

    /** Every preference key holding "what this dashboard contains and how it looks": its rooms,
     * widgets, pages and nav bar, plus the aesthetics an admin's aesthetics-only permission covers
     * (theme, colors, icons, corner radius, fonts) and the header pill. Device-level preferences —
     * refresh rate, clock format, icon animations — are not dashboard state and stay put. */
    private val dashboardCustomizationKeys: List<Preferences.Key<*>>
        get() = listOf(
            areaOrderKey, savedAreasKey, savedFloorsKey, areaStacksKey, areaConfigsKey, pageConfigsKey,
            customPagesKey, customPopupsKey, navBarOrderKey, navBarHiddenKey,
            mediaPlayerNamesKey, mediaPlayerBarHiddenKey,
            themeColorKey, themeModeKey, systemLightThemeColorKey, systemDarkThemeColorKey,
            fontScaleKey, fontWeightAdjustKey, fontFamilyKey, iconPackKey, iconEffectDefaultsKey,
            itemCornerRadiusKey, weatherEntityIdKey, weatherDisplayKey, headerLeftDisplayKey,
            headerVisibleKey, weatherCardWidthsKey, alarmEntityKey, headerLeftAlarmEntityKey,
            alarmPendingSecondsKey,
        ) + headerWeatherRoleKeys.values

    /** Wipes the local dashboard back to a blank slate. Used when an admin revokes this user's
     * access to the family dashboard they were running: the dashboard itself is deleted, and
     * anything they had layered on top of it must go with it. Otherwise the leftovers — a theme, a
     * header pill, a weather entity, a nav-bar order — survive into whatever dashboard is shared
     * next, which then looks like the new dashboard failing to import in full.
     *
     * The recipient's Home Assistant connection, profile, notification, and backup settings are
     * deliberately untouched: losing a dashboard is not being signed out. */
    private fun clearDashboardCustomization(p: MutablePreferences) {
        dashboardCustomizationKeys.forEach { key -> p -= key }
        // Not simply removed: an absent mode reads as "auto", which would offer an auto-generated
        // dashboard the user never asked for while the chooser is still deciding what comes next.
        p[dashboardModeKey] = "manual"
        p.remove(pendingAutoTakeoverKey)
    }

    private fun loadDashboardIntoPreferences(p: MutablePreferences, dashboard: HKIDashboard) {
        p[dashboardModeKey] = dashboard.mode
        p[areaOrderKey] = dashboard.areaOrder.joinToString(",")
        p[savedAreasKey] = appJson.encodeToString(dashboard.areas)
        p[savedFloorsKey] = appJson.encodeToString(dashboard.floors)
        p[areaStacksKey] = appJson.encodeToString(dashboard.areaWidgets)
        p[areaConfigsKey] = appJson.encodeToString(dashboard.areaConfigs)
        p[pageConfigsKey] = appJson.encodeToString(dashboard.pageConfigs)
        p[customPagesKey] = appJson.encodeToString(dashboard.customPages)
        p[customPopupsKey] = appJson.encodeToString(dashboard.customPopups)
        p[navBarOrderKey] = dashboard.navBarOrder.joinToString(",")
        p[navBarHiddenKey] = dashboard.navBarHidden.joinToString(",")
        p[mediaPlayerNamesKey] = appJson.encodeToString(dashboard.mediaPlayerNames)
        p[mediaPlayerBarHiddenKey] = dashboard.mediaPlayerBarHidden.joinToString(",")
        // Only dashboards that captured a header pill re-apply it; older ones leave global prefs alone.
        dashboard.headerPill?.let { applyHeaderPill(p, it) }
    }

    private fun manualOnlyPageConfigs(): Map<String, HKIPageConfig> = mapOf(
        "climate" to HKIPageConfig(climateConfig = HKIClimateConfig(manualOnly = true)),
        "security" to HKIPageConfig(securityConfig = HKISecurityConfig(manualOnly = true)),
        "battery" to HKIPageConfig(batteryConfig = HKIBatteryConfig(manualOnly = true)),
        "energy" to HKIPageConfig(energyConfig = HKIEnergyConfig(manualOnly = true))
    )
    suspend fun saveAreas(areas: List<HAArea>) { context.dataStore.edit { it[savedAreasKey] = appJson.encodeToString(areas) } }
    suspend fun saveFloors(floors: List<HAFloor>) { context.dataStore.edit { it[savedFloorsKey] = appJson.encodeToString(floors) } }
    suspend fun saveAreaWidgets(mapping: Map<String, List<HKIRoomWidget>>) { context.dataStore.edit { it[areaStacksKey] = appJson.encodeToString(mapping) } }
    suspend fun saveAreaConfigs(mapping: Map<String, HKIAreaConfig>) { context.dataStore.edit { it[areaConfigsKey] = appJson.encodeToString(mapping) } }
    suspend fun savePageConfigs(mapping: Map<String, HKIPageConfig>) { context.dataStore.edit { it[pageConfigsKey] = appJson.encodeToString(mapping) } }
    suspend fun saveMobileDeviceName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name.isNullOrBlank()) preferences.remove(mobileDeviceNameKey) else preferences[mobileDeviceNameKey] = name
        }
    }
    suspend fun saveForceHighRefreshRate(enabled: Boolean) { context.dataStore.edit { it[forceHighRefreshRateKey] = enabled } }
    suspend fun saveFontScale(scale: Float) { context.dataStore.edit { it[fontScaleKey] = scale } }
    suspend fun saveFontWeightAdjust(adjust: Int) { context.dataStore.edit { it[fontWeightAdjustKey] = adjust } }
    suspend fun saveFontFamily(family: String) { context.dataStore.edit { it[fontFamilyKey] = family } }
    suspend fun saveDefaultIconPack(pack: String) { context.dataStore.edit { it[iconPackKey] = pack } }
    suspend fun saveIconAnimationsEnabled(enabled: Boolean) { context.dataStore.edit { it[iconAnimationsKey] = enabled } }
    suspend fun saveIconEffectDefault(group: String, effectId: String) {
        context.dataStore.edit { p ->
            val current = decodeBackup<Map<String, String>>(p[iconEffectDefaultsKey], emptyMap()).toMutableMap()
            current[group] = effectId
            p[iconEffectDefaultsKey] = appJson.encodeToString(current)
        }
    }
    suspend fun saveItemCornerRadius(radius: Int) { context.dataStore.edit { it[itemCornerRadiusKey] = radius.coerceIn(0, 48) } }

    suspend fun saveInternalUrl(url: String?) {
        context.dataStore.edit { preferences ->
            val cleaned = url?.trim()?.takeIf(String::isNotBlank)
            if (updateScopedInstance(preferences) { it.copy(internalUrl = cleaned) }) return@edit
            if (cleaned == null) preferences.remove(internalUrlKey) else preferences[internalUrlKey] = cleaned
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveExternalUrl(url: String?) {
        context.dataStore.edit { preferences ->
            val external = url?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
            if (updateScopedInstance(preferences) { instance ->
                    instance.copy(
                        externalUrl = external,
                        mobileAppRegisteredUrl = if (!instance.mobileAppWebhookId.isNullOrBlank()) {
                            external ?: instance.internalUrl
                        } else instance.mobileAppRegisteredUrl
                    )
                }
            ) return@edit
            if (external == null) {
                preferences.remove(serverUrlKey)
                preferences[internalUrlKey]?.takeIf { it.isNotBlank() }?.let { internal ->
                    if (!preferences[mobileAppWebhookIdKey].isNullOrBlank()) {
                        preferences[mobileAppRegisteredUrlKey] = internal
                    }
                }
            } else {
                preferences[serverUrlKey] = external
                // Internal and external endpoints address the same HA instance; retain the
                // existing mobile_app webhook instead of creating a duplicate device solely
                // because the preferred endpoint changed.
                if (!preferences[mobileAppWebhookIdKey].isNullOrBlank()) {
                    preferences[mobileAppRegisteredUrlKey] = external
                }
            }
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveHomeSsids(ssids: List<String>) {
        context.dataStore.edit { preferences ->
            val cleaned = ssids.map { ssid -> ssid.trim() }.filter { ssid -> ssid.isNotBlank() }
            if (updateScopedInstance(preferences) { it.copy(homeSsids = cleaned) }) return@edit
            if (cleaned.isEmpty()) preferences.remove(homeSsidsKey) else preferences[homeSsidsKey] = cleaned.joinToString(",")
            snapshotActiveInstance(preferences)
        }
    }

    suspend fun saveHighAccuracyLocation(enabled: Boolean) { context.dataStore.edit { it[highAccuracyLocationKey] = enabled } }
    suspend fun saveAdaptiveLightingProfile(entityId: String, configEntryId: String?) {
        context.dataStore.edit { preferences ->
            val mappings = decodeBackup<Map<String, String>>(
                preferences[adaptiveLightingProfilesKey],
                emptyMap()
            ).toMutableMap()
            if (configEntryId.isNullOrBlank()) mappings.remove(entityId)
            else mappings[entityId] = configEntryId
            if (mappings.isEmpty()) preferences.remove(adaptiveLightingProfilesKey)
            else preferences[adaptiveLightingProfilesKey] = appJson.encodeToString(mappings)
        }
    }
    suspend fun saveAdaptiveLightingOptionsForms(forms: Map<String, JsonObject>) {
        context.dataStore.edit { preferences ->
            if (forms.isEmpty()) preferences.remove(adaptiveLightingOptionsFormsKey)
            else preferences[adaptiveLightingOptionsFormsKey] = appJson.encodeToString(forms)
        }
    }
    suspend fun saveNavBarOrder(order: List<String>) {
        context.dataStore.edit {
            val cleaned = order.filter { r -> r.isNotBlank() }
            if (cleaned.isEmpty()) it.remove(navBarOrderKey) else it[navBarOrderKey] = cleaned.joinToString(",")
        }
    }
    suspend fun saveNavBarHidden(hidden: List<String>) {
        context.dataStore.edit {
            val cleaned = hidden.filter { r -> r.isNotBlank() }
            if (cleaned.isEmpty()) it.remove(navBarHiddenKey) else it[navBarHiddenKey] = cleaned.joinToString(",")
        }
    }
    suspend fun saveMediaPlayerCustomNames(names: Map<String, String>) {
        context.dataStore.edit {
            if (names.isEmpty()) it.remove(mediaPlayerNamesKey) else it[mediaPlayerNamesKey] = appJson.encodeToString(names)
        }
    }
    suspend fun saveMediaPlayerBarHidden(hidden: List<String>) {
        context.dataStore.edit {
            val cleaned = hidden.filter { r -> r.isNotBlank() }
            if (cleaned.isEmpty()) it.remove(mediaPlayerBarHiddenKey) else it[mediaPlayerBarHiddenKey] = cleaned.joinToString(",")
        }
    }
    suspend fun saveBackgroundPushEnabled(enabled: Boolean) { context.dataStore.edit { it[backgroundPushKey] = enabled } }
    suspend fun saveNotificationHistory(history: List<HKINotification>) {
        context.dataStore.edit { it[notificationHistoryKey] = appJson.encodeToString(history) }
    }

    suspend fun saveMobileAppDeviceId(id: String) {
        context.dataStore.edit { preferences ->
            if (updateScopedInstance(preferences) { it.copy(mobileAppDeviceId = id) }) return@edit
            preferences[mobileAppDeviceIdKey] = id
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveGeocodedAddress(address: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[geocodedAddressKey] = address
            it[geocodedLatKey] = lat
            it[geocodedLonKey] = lon
        }
    }
    suspend fun saveMobileAppSensorsRegistered(webhookId: String) {
        context.dataStore.edit { preferences ->
            if (updateScopedInstance(preferences) { it.copy(mobileAppSensorsWebhookId = webhookId) }) return@edit
            preferences[mobileAppSensorsWebhookIdKey] = webhookId
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveMobileAppRegistration(
        webhookId: String,
        cloudhookUrl: String?,
        remoteUiUrl: String?,
        registeredUrl: String
    ) {
        context.dataStore.edit { preferences ->
            val remote = remoteUiUrl?.trim()?.removeSuffix("/")?.takeIf { it.isNotBlank() }
            val registered = registeredUrl.trim().removeSuffix("/")
            if (updateScopedInstance(preferences) { instance ->
                    val adoptRemote = remote != null &&
                        instance.primaryUrl?.trim()?.removeSuffix("/").equals(registered, ignoreCase = true)
                    instance.copy(
                        externalUrl = if (adoptRemote) remote else instance.externalUrl,
                        internalUrl = if (adoptRemote && !remote.equals(registered, ignoreCase = true) &&
                            isLikelyLocalHomeAssistantUrl(registered)
                        ) registered else instance.internalUrl,
                        mobileAppWebhookId = webhookId,
                        mobileAppCloudhookUrl = cloudhookUrl?.takeIf(String::isNotBlank),
                        mobileAppRegisteredUrl = if (adoptRemote) remote else registered
                    )
                }
            ) return@edit
            preferences[mobileAppWebhookIdKey] = webhookId
            if (cloudhookUrl.isNullOrBlank()) preferences.remove(mobileAppCloudhookUrlKey) else preferences[mobileAppCloudhookUrlKey] = cloudhookUrl

            val currentPrimary = preferences[serverUrlKey]?.trim()?.removeSuffix("/")

            // Home Assistant Cloud returns its Nabu Casa Remote UI address during mobile_app
            // registration. Adopt it as the stable external URL, while retaining a LAN address for
            // optional SSID-based internal switching. This is the same local-discovery -> cloud-URL
            // handoff used by companion apps and avoids asking the user to copy the cloud URL.
            if (remote != null && currentPrimary.equals(registered, ignoreCase = true)) {
                if (!remote.equals(registered, ignoreCase = true) && isLikelyLocalHomeAssistantUrl(registered)) {
                    preferences[internalUrlKey] = registered
                }
                preferences[serverUrlKey] = remote
                preferences[mobileAppRegisteredUrlKey] = remote
            } else {
                preferences[mobileAppRegisteredUrlKey] = registered
            }
            snapshotActiveInstance(preferences)
        }
    }
    suspend fun saveThemeColor(theme: String) { context.dataStore.edit { it[themeColorKey] = theme } }
    suspend fun saveThemeMode(mode: String) { context.dataStore.edit { it[themeModeKey] = mode } }
    suspend fun saveSystemLightThemeColor(theme: String) { context.dataStore.edit { it[systemLightThemeColorKey] = theme } }
    suspend fun saveSystemDarkThemeColor(theme: String) { context.dataStore.edit { it[systemDarkThemeColorKey] = theme } }

    suspend fun clearDashboardConfig(keepMode: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences.remove(areaOrderKey)
            preferences.remove(savedAreasKey)
            preferences.remove(savedFloorsKey)
            preferences.remove(areaStacksKey)
            preferences.remove(areaConfigsKey)
            if (!keepMode) preferences.remove(dashboardModeKey)
        }
    }
    suspend fun saveWeatherDisplayType(type: String) { context.dataStore.edit { it[weatherDisplayKey] = type } }
    suspend fun saveHeaderLeftDisplayType(type: String) { context.dataStore.edit { it[headerLeftDisplayKey] = type } }
    suspend fun saveHeaderVisible(visible: Boolean) { context.dataStore.edit { it[headerVisibleKey] = visible } }
    suspend fun saveUse24hFormat(use24h: Boolean) { context.dataStore.edit { it[use24hFormatKey] = use24h } }
    suspend fun saveUseFullDayName(useFullDayName: Boolean) { context.dataStore.edit { it[useFullDayNameKey] = useFullDayName } }
    // Multiple alarms per pill, stored comma-joined in the legacy single-id key so an existing
    // single selection keeps working unchanged.
    suspend fun saveHeaderAlarmEntities(entityIds: List<String>) {
        context.dataStore.edit {
            if (entityIds.isEmpty()) it.remove(alarmEntityKey) else it[alarmEntityKey] = entityIds.joinToString(",")
        }
    }
    suspend fun saveHeaderLeftAlarmEntities(entityIds: List<String>) {
        context.dataStore.edit {
            // Keep an explicit empty value so clearing the selection remains distinct from a new
            // install whose alarm entities have not been auto-imported yet.
            it[headerLeftAlarmEntityKey] = entityIds.joinToString(",")
        }
    }
    suspend fun seedHeaderLeftAlarmEntitiesIfUnset(entityIds: List<String>, showByDefault: Boolean = false) {
        if (entityIds.isEmpty()) return
        context.dataStore.edit { preferences ->
            if (headerLeftAlarmEntityKey !in preferences) {
                preferences[headerLeftAlarmEntityKey] = entityIds.distinct().joinToString(",")
            }
            if (showByDefault && headerLeftDisplayKey !in preferences) {
                preferences[headerLeftDisplayKey] = "Alarm"
            }
        }
    }
    suspend fun saveAlarmPendingSeconds(seconds: Int) {
        context.dataStore.edit { it[alarmPendingSecondsKey] = seconds }
    }

    suspend fun saveWeatherExtraEntity(role: String, entityId: String?) {
        context.dataStore.edit { preferences ->
            val key = when(role) {
                "sun" -> sunEntityKey
                "moon" -> moonEntityKey
                "aqi" -> aqiEntityKey
                "season" -> seasonEntityKey
                "rain" -> rainEntityKey
                "rainmap" -> rainMapEntityKey
                "rainmap_url" -> rainMapUrlKey
                "rainmap_aspect" -> rainMapAspectKey
                "device" -> weatherDeviceKey
                else -> return@edit
            }
            if (entityId == null) preferences.remove(key) else preferences[key] = entityId
        }
    }

    suspend fun saveWeatherCardWidth(card: String, width: String) {
        if (width !in setOf("third", "half", "full")) return
        context.dataStore.edit { preferences ->
            val current = preferences[weatherCardWidthsKey]
                ?.let { saved -> runCatching { appJson.decodeFromString<Map<String, String>>(saved) }.getOrNull() }
                .orEmpty()
                .toMutableMap()
            current[card] = width
            preferences[weatherCardWidthsKey] = appJson.encodeToString(current)
        }
    }
    suspend fun saveCustomPages(pages: List<HKICustomPage>) {
        context.dataStore.edit {
            if (pages.isEmpty()) it.remove(customPagesKey) else it[customPagesKey] = appJson.encodeToString(pages)
        }
    }
    suspend fun saveCustomPopups(popups: List<HKICustomPopup>) {
        context.dataStore.edit {
            if (popups.isEmpty()) it.remove(customPopupsKey) else it[customPopupsKey] = appJson.encodeToString(popups)
        }
    }

    /** Configures a fully offline demo session (used for Google Play review): storing the demo
     *  server URL makes the view model build the in-memory sample-home client instead of a
     *  networked one. Exit via a full reset ([clearAll]). */
    suspend fun enterDemoMode() {
        context.dataStore.edit { preferences ->
            preferences[serverUrlKey] = DEMO_SERVER_URL
            preferences.remove(internalUrlKey)
            preferences[accessTokenKey] = DEMO_ACCESS_TOKEN
            preferences.remove(refreshTokenKey)
            // Far-future expiry: the demo token never triggers a refresh.
            preferences[accessTokenExpiryKey] = System.currentTimeMillis() + 3650L * 24 * 60 * 60 * 1000
            preferences[displayNameKey] = "Demo"
            preferences[mobileDeviceNameKey] = "Demo Phone"
            snapshotActiveInstance(preferences)
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            if (updateScopedInstance(preferences) {
                    it.copy(accessToken = null, refreshToken = null, accessTokenExpiry = null)
                }
            ) return@edit
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(accessTokenExpiryKey)
            snapshotActiveInstance(preferences)
        }
    }

    suspend fun clearAll() { context.dataStore.edit { it.clear() } }
}

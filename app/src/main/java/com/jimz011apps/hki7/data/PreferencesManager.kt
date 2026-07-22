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
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private const val LEGACY_INSTANCE_ID = "legacy-default-instance"

// Tolerates fields removed from saved config data classes across app updates — without this,
// a stale key in a persisted blob throws on decode and the catch-all fallback wipes that store.
private val appJson = Json { ignoreUnknownKeys = true }
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
    val navBarOrder: List<String> = emptyList(),
    val navBarHidden: List<String> = emptyList()
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
    val navBarOrder: List<String> = emptyList(),
    val navBarHidden: List<String> = emptyList(),
    val themeColor: String = "system",
    val themeMode: String = "system",
    val systemLightThemeColor: String = "auto",
    val systemDarkThemeColor: String = "auto",
    val fontScale: Float = 1f,
    val fontWeightAdjust: Int = 0,
    val fontFamily: String = "default",
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
    private val backgroundPushKey = booleanPreferencesKey("background_push_enabled")
    private val navBarOrderKey = stringPreferencesKey("nav_bar_order")
    private val navBarHiddenKey = stringPreferencesKey("nav_bar_hidden")
    private val customPagesKey = stringPreferencesKey("custom_pages")
    private val mediaPlayerNamesKey = stringPreferencesKey("media_player_custom_names")
    private val mediaPlayerBarHiddenKey = stringPreferencesKey("media_player_bar_hidden")
    private val adaptiveLightingProfilesKey = stringPreferencesKey("adaptive_lighting_profiles")
    private val adaptiveLightingOptionsFormsKey = stringPreferencesKey("adaptive_lighting_options_forms_cache")
    private val dashboardsKey = stringPreferencesKey("dashboards_v2")
    private val activeDashboardIdKey = stringPreferencesKey("active_dashboard_id")
    private val defaultDashboardIdKey = stringPreferencesKey("default_dashboard_id")
    private val pendingAutoTakeoverKey = booleanPreferencesKey("pending_auto_takeover")
    private val quickStartGuidePendingKey = booleanPreferencesKey("quick_start_guide_pending")
    private val cloudBackupEnabledKey = booleanPreferencesKey("cloud_backup_enabled")
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
    val cloudBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[cloudBackupEnabledKey] ?: false }

    suspend fun saveCloudBackup(enabled: Boolean) {
        context.dataStore.edit { it[cloudBackupEnabledKey] = enabled }
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
            navBarOrder = strings(navBarOrderKey),
            navBarHidden = strings(navBarHiddenKey),
            themeColor = p[themeColorKey] ?: "system",
            themeMode = p[themeModeKey] ?: "system",
            systemLightThemeColor = p[systemLightThemeColorKey] ?: "auto",
            systemDarkThemeColor = p[systemDarkThemeColorKey] ?: "auto",
            fontScale = p[fontScaleKey] ?: 1f,
            fontWeightAdjust = p[fontWeightAdjustKey] ?: 0,
            fontFamily = p[fontFamilyKey] ?: "default",
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
            p[navBarOrderKey] = backup.navBarOrder.joinToString(",")
            p[navBarHiddenKey] = backup.navBarHidden.joinToString(",")
            p[themeColorKey] = backup.themeColor
            p[themeModeKey] = backup.themeMode
            p[systemLightThemeColorKey] = backup.systemLightThemeColor
            p[systemDarkThemeColorKey] = backup.systemDarkThemeColor
            p[fontScaleKey] = backup.fontScale
            p[fontWeightAdjustKey] = backup.fontWeightAdjust
            p[fontFamilyKey] = backup.fontFamily
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

    /** Migrates the original single dashboard in place, without changing what is currently loaded. */
    suspend fun ensureDashboardStore(defaultName: String = "Default") {
        context.dataStore.edit { p ->
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

    suspend fun createDashboard(name: String, autoGenerate: Boolean): String {
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

    suspend fun switchDashboard(id: String): Boolean {
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
        navBarOrder = p[navBarOrderKey]?.split(',')?.filter(String::isNotBlank).orEmpty(),
        navBarHidden = p[navBarHiddenKey]?.split(',')?.filter(String::isNotBlank).orEmpty()
    )

    private fun saveLoadedDashboardInto(p: MutablePreferences, dashboards: MutableList<HKIDashboard>) {
        val activeId = p[activeDashboardIdKey] ?: return
        val index = dashboards.indexOfFirst { it.id == activeId }
        if (index >= 0) dashboards[index] = dashboardFromPreferences(p, activeId, dashboards[index].name)
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
        p[navBarOrderKey] = dashboard.navBarOrder.joinToString(",")
        p[navBarHiddenKey] = dashboard.navBarHidden.joinToString(",")
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

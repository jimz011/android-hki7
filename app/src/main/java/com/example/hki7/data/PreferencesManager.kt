package com.example.hki7.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

class PreferencesManager(private val context: Context) {
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
    private val dashboardsKey = stringPreferencesKey("dashboards_v2")
    private val activeDashboardIdKey = stringPreferencesKey("active_dashboard_id")
    private val defaultDashboardIdKey = stringPreferencesKey("default_dashboard_id")
    private val pendingAutoTakeoverKey = booleanPreferencesKey("pending_auto_takeover")

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[serverUrlKey] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[accessTokenKey] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[refreshTokenKey] }
    // Absolute wall-clock time (epoch millis) when the current access token expires, if known.
    val accessTokenExpiry: Flow<Long?> = context.dataStore.data.map { it[accessTokenExpiryKey] }
    val weatherEntityId: Flow<String?> = context.dataStore.data.map { it[weatherEntityIdKey] ?: "weather.home" }
    val displayName: Flow<String?> = context.dataStore.data.map { it[displayNameKey] ?: "User" }
    val profileAvatar: Flow<String?> = context.dataStore.data.map { it[profileAvatarKey] }
    val profileBirthday: Flow<String?> = context.dataStore.data.map { it[profileBirthdayKey] }
    val profilePersonEntityId: Flow<String?> = context.dataStore.data.map { it[profilePersonEntityKey] }
    val areaOrder: Flow<List<String>> = context.dataStore.data.map { it[areaOrderKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList() }
    val dashboardMode: Flow<String> = context.dataStore.data.map { it[dashboardModeKey] ?: "auto" }
    val dashboards: Flow<List<HKIDashboard>> = context.dataStore.data.map { p ->
        decodeBackup(p[dashboardsKey], emptyList())
    }
    val activeDashboardId: Flow<String?> = context.dataStore.data.map { it[activeDashboardIdKey] }
    val defaultDashboardId: Flow<String?> = context.dataStore.data.map { it[defaultDashboardIdKey] }
    val pendingAutoTakeover: Flow<Boolean> = context.dataStore.data.map { it[pendingAutoTakeoverKey] ?: false }

    val savedAreas: Flow<List<HAArea>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[savedAreasKey] ?: "[]"
        try { appJson.decodeFromString<List<HAArea>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    val savedFloors: Flow<List<HAFloor>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[savedFloorsKey] ?: "[]"
        try { appJson.decodeFromString<List<HAFloor>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    val areaWidgets: Flow<Map<String, List<HKIRoomWidget>>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[areaStacksKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, List<HKIRoomWidget>>>(jsonStr) } catch (e: Exception) { emptyMap() }
    }

    val areaConfigs: Flow<Map<String, HKIAreaConfig>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[areaConfigsKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, HKIAreaConfig>>(jsonStr) } catch (e: Exception) { emptyMap() }
    }
    val pageConfigs: Flow<Map<String, HKIPageConfig>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[pageConfigsKey] ?: "{}"
        try { appJson.decodeFromString<Map<String, HKIPageConfig>>(jsonStr) } catch (e: Exception) { emptyMap() }
    }

    val weatherDisplayType: Flow<String> = context.dataStore.data.map { it[weatherDisplayKey] ?: "Weather" }
    val headerLeftDisplayType: Flow<String> = context.dataStore.data.map { it[headerLeftDisplayKey] ?: "None" }
    val use24hFormat: Flow<Boolean> = context.dataStore.data.map { it[use24hFormatKey] ?: true }
    val useFullDayName: Flow<Boolean> = context.dataStore.data.map { it[useFullDayNameKey] ?: false }

    val sunEntityId: Flow<String?> = context.dataStore.data.map { it[sunEntityKey] ?: "sun.sun" }
    val moonEntityId: Flow<String?> = context.dataStore.data.map { it[moonEntityKey] ?: "sensor.moon" }
    val aqiEntityId: Flow<String?> = context.dataStore.data.map { it[aqiEntityKey] }
    val seasonEntityId: Flow<String?> = context.dataStore.data.map { it[seasonEntityKey] ?: "sensor.season" }
    val rainEntityId: Flow<String?> = context.dataStore.data.map { it[rainEntityKey] }
    /** HA device the weather page's role entities were auto-filled from (device-first setup). */
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
    val mobileAppWebhookId: Flow<String?> = context.dataStore.data.map { it[mobileAppWebhookIdKey] }
    val mobileAppCloudhookUrl: Flow<String?> = context.dataStore.data.map { it[mobileAppCloudhookUrlKey] }
    val mobileAppRegisteredUrl: Flow<String?> = context.dataStore.data.map { it[mobileAppRegisteredUrlKey] }
    val mobileAppDeviceId: Flow<String?> = context.dataStore.data.map { it[mobileAppDeviceIdKey] }
    // The webhook id for which the mobile_app sensors have already been registered (so we register
    // them once, not on every report). A new webhook id (re-registration) forces a re-register.
    val mobileAppSensorsWebhookId: Flow<String?> = context.dataStore.data.map { it[mobileAppSensorsWebhookIdKey] }
    // Last successful reverse-geocode (address + the fix it was resolved for), persisted so a fresh
    // process reuses it instead of re-querying the Geocoder for a device that hasn't moved.
    val geocodedAddress: Flow<String?> = context.dataStore.data.map { it[geocodedAddressKey] }
    val geocodedLat: Flow<Double?> = context.dataStore.data.map { it[geocodedLatKey] }
    val geocodedLon: Flow<Double?> = context.dataStore.data.map { it[geocodedLonKey] }

    // Local HA URL used when connected to one of the configured home Wi-Fi SSIDs; otherwise serverUrl.
    val internalUrl: Flow<String?> = context.dataStore.data.map { it[internalUrlKey]?.takeIf { url -> url.isNotBlank() } }
    val homeSsids: Flow<List<String>> = context.dataStore.data.map {
        it[homeSsidsKey]?.split(",")?.map { ssid -> ssid.trim() }?.filter { ssid -> ssid.isNotBlank() } ?: emptyList()
    }
    // When true, location updates much more frequently (every few seconds) at the cost of battery.
    val highAccuracyLocation: Flow<Boolean> = context.dataStore.data.map { it[highAccuracyLocationKey] ?: false }

    // Notification history delivered via the websocket push channel (newest first, capped).
    val notificationHistory: Flow<List<HKINotification>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[notificationHistoryKey] ?: "[]"
        try { appJson.decodeFromString<List<HKINotification>>(jsonStr) } catch (e: Exception) { emptyList() }
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
        try { appJson.decodeFromString<Map<String, String>>(jsonStr) } catch (e: Exception) { emptyMap() }
    }
    val mediaPlayerBarHidden: Flow<List<String>> = context.dataStore.data.map {
        it[mediaPlayerBarHiddenKey]?.split(",")?.filter { r -> r.isNotBlank() } ?: emptyList()
    }

    suspend fun saveConnectionDetails(url: String, token: String, refresh: String? = null, expiresInSeconds: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[serverUrlKey] = url
            preferences[accessTokenKey] = token
            if (refresh != null) preferences[refreshTokenKey] = refresh
            if (expiresInSeconds != null) {
                preferences[accessTokenExpiryKey] = System.currentTimeMillis() + expiresInSeconds * 1000L
            }
        }
    }

    /** Updates the auth tokens without touching the stored server URL (used by token refresh,
     *  which may be talking to the resolved internal URL at the time). */
    suspend fun saveAuthTokens(token: String, refresh: String? = null, expiresInSeconds: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
            if (refresh != null) preferences[refreshTokenKey] = refresh
            if (expiresInSeconds != null) {
                preferences[accessTokenExpiryKey] = System.currentTimeMillis() + expiresInSeconds * 1000L
            }
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
    suspend fun saveAreaOrder(order: List<String>) { context.dataStore.edit { it[areaOrderKey] = order.joinToString(",") } }
    suspend fun saveDashboardMode(mode: String) { context.dataStore.edit { it[dashboardModeKey] = mode } }
    suspend fun savePendingAutoTakeover(pending: Boolean) {
        context.dataStore.edit { if (pending) it[pendingAutoTakeoverKey] = true else it.remove(pendingAutoTakeoverKey) }
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
            val created = HKIDashboard(id = id, name = name.trim().ifBlank { "Dashboard" }, mode = if (autoGenerate) "auto" else "manual")
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
        context.dataStore.edit { if (url.isNullOrBlank()) it.remove(internalUrlKey) else it[internalUrlKey] = url.trim() }
    }
    suspend fun saveHomeSsids(ssids: List<String>) {
        context.dataStore.edit {
            val cleaned = ssids.map { ssid -> ssid.trim() }.filter { ssid -> ssid.isNotBlank() }
            if (cleaned.isEmpty()) it.remove(homeSsidsKey) else it[homeSsidsKey] = cleaned.joinToString(",")
        }
    }

    suspend fun saveHighAccuracyLocation(enabled: Boolean) { context.dataStore.edit { it[highAccuracyLocationKey] = enabled } }
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

    suspend fun saveMobileAppDeviceId(id: String) { context.dataStore.edit { it[mobileAppDeviceIdKey] = id } }
    suspend fun saveGeocodedAddress(address: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[geocodedAddressKey] = address
            it[geocodedLatKey] = lat
            it[geocodedLonKey] = lon
        }
    }
    suspend fun saveMobileAppSensorsRegistered(webhookId: String) {
        context.dataStore.edit { it[mobileAppSensorsWebhookIdKey] = webhookId }
    }
    suspend fun saveMobileAppRegistration(webhookId: String, cloudhookUrl: String?, registeredUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[mobileAppWebhookIdKey] = webhookId
            if (cloudhookUrl.isNullOrBlank()) preferences.remove(mobileAppCloudhookUrlKey) else preferences[mobileAppCloudhookUrlKey] = cloudhookUrl
            preferences[mobileAppRegisteredUrlKey] = registeredUrl
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
            if (entityIds.isEmpty()) it.remove(headerLeftAlarmEntityKey) else it[headerLeftAlarmEntityKey] = entityIds.joinToString(",")
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

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(accessTokenExpiryKey)
        }
    }

    suspend fun clearAll() { context.dataStore.edit { it.clear() } }
}

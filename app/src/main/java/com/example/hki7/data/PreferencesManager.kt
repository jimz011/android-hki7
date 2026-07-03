package com.example.hki7.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Tolerates fields removed from saved config data classes across app updates — without this,
// a stale key in a persisted blob throws on decode and the catch-all fallback wipes that store.
private val appJson = Json { ignoreUnknownKeys = true }

class PreferencesManager(private val context: Context) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val accessTokenExpiryKey = longPreferencesKey("access_token_expiry")
    private val weatherEntityIdKey = stringPreferencesKey("weather_entity_id")
    private val displayNameKey = stringPreferencesKey("display_name")
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
    private val sunEntityKey = stringPreferencesKey("sun_entity_id")
    private val moonEntityKey = stringPreferencesKey("moon_entity_id")
    private val aqiEntityKey = stringPreferencesKey("aqi_entity_id")
    private val seasonEntityKey = stringPreferencesKey("season_entity_id")
    private val rainEntityKey = stringPreferencesKey("rain_entity_id")
    private val alarmEntityKey = stringPreferencesKey("header_alarm_entity_id")
    private val headerLeftAlarmEntityKey = stringPreferencesKey("header_left_alarm_entity_id")
    private val mobileDeviceNameKey = stringPreferencesKey("mobile_device_name")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val systemLightThemeColorKey = stringPreferencesKey("system_light_theme_color")
    private val systemDarkThemeColorKey = stringPreferencesKey("system_dark_theme_color")
    private val forceHighRefreshRateKey = booleanPreferencesKey("force_high_refresh_rate")
    private val mobileAppWebhookIdKey = stringPreferencesKey("mobile_app_webhook_id")
    private val mobileAppCloudhookUrlKey = stringPreferencesKey("mobile_app_cloudhook_url")
    private val mobileAppRegisteredUrlKey = stringPreferencesKey("mobile_app_registered_url")
    private val mobileAppDeviceIdKey = stringPreferencesKey("mobile_app_device_id")
    private val mobileAppSensorsWebhookIdKey = stringPreferencesKey("mobile_app_sensors_webhook")
    private val internalUrlKey = stringPreferencesKey("internal_url")
    private val homeSsidsKey = stringPreferencesKey("home_ssids")
    private val highAccuracyLocationKey = booleanPreferencesKey("high_accuracy_location")

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[serverUrlKey] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[accessTokenKey] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[refreshTokenKey] }
    // Absolute wall-clock time (epoch millis) when the current access token expires, if known.
    val accessTokenExpiry: Flow<Long?> = context.dataStore.data.map { it[accessTokenExpiryKey] }
    val weatherEntityId: Flow<String?> = context.dataStore.data.map { it[weatherEntityIdKey] ?: "weather.home" }
    val displayName: Flow<String?> = context.dataStore.data.map { it[displayNameKey] ?: "User" }
    val areaOrder: Flow<List<String>> = context.dataStore.data.map { it[areaOrderKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList() }
    val dashboardMode: Flow<String> = context.dataStore.data.map { it[dashboardModeKey] ?: "auto" }

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

    val sunEntityId: Flow<String?> = context.dataStore.data.map { it[sunEntityKey] ?: "sun.sun" }
    val moonEntityId: Flow<String?> = context.dataStore.data.map { it[moonEntityKey] ?: "sensor.moon" }
    val aqiEntityId: Flow<String?> = context.dataStore.data.map { it[aqiEntityKey] }
    val seasonEntityId: Flow<String?> = context.dataStore.data.map { it[seasonEntityKey] ?: "sensor.season" }
    val rainEntityId: Flow<String?> = context.dataStore.data.map { it[rainEntityKey] }
    val alarmEntityId: Flow<String?> = context.dataStore.data.map { it[alarmEntityKey] }
    val headerLeftAlarmEntityId: Flow<String?> = context.dataStore.data.map { it[headerLeftAlarmEntityKey] }
    val mobileDeviceName: Flow<String?> = context.dataStore.data.map { it[mobileDeviceNameKey] }
    val themeColor: Flow<String> = context.dataStore.data.map { it[themeColorKey] ?: "system" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[themeModeKey] ?: "system" }
    val systemLightThemeColor: Flow<String> = context.dataStore.data.map { it[systemLightThemeColorKey] ?: "auto" }
    val systemDarkThemeColor: Flow<String> = context.dataStore.data.map { it[systemDarkThemeColorKey] ?: "auto" }
    // When true, lock the window to the panel's highest mode regardless of the system peak-rate setting.
    val forceHighRefreshRate: Flow<Boolean> = context.dataStore.data.map { it[forceHighRefreshRateKey] ?: false }

    // mobile_app integration registration (persistent device_tracker + sensors via webhook).
    val mobileAppWebhookId: Flow<String?> = context.dataStore.data.map { it[mobileAppWebhookIdKey] }
    val mobileAppCloudhookUrl: Flow<String?> = context.dataStore.data.map { it[mobileAppCloudhookUrlKey] }
    val mobileAppRegisteredUrl: Flow<String?> = context.dataStore.data.map { it[mobileAppRegisteredUrlKey] }
    val mobileAppDeviceId: Flow<String?> = context.dataStore.data.map { it[mobileAppDeviceIdKey] }
    // The webhook id for which the mobile_app sensors have already been registered (so we register
    // them once, not on every report). A new webhook id (re-registration) forces a re-register.
    val mobileAppSensorsWebhookId: Flow<String?> = context.dataStore.data.map { it[mobileAppSensorsWebhookIdKey] }

    // Local HA URL used when connected to one of the configured home Wi-Fi SSIDs; otherwise serverUrl.
    val internalUrl: Flow<String?> = context.dataStore.data.map { it[internalUrlKey]?.takeIf { url -> url.isNotBlank() } }
    val homeSsids: Flow<List<String>> = context.dataStore.data.map {
        it[homeSsidsKey]?.split(",")?.map { ssid -> ssid.trim() }?.filter { ssid -> ssid.isNotBlank() } ?: emptyList()
    }
    // When true, location updates much more frequently (every few seconds) at the cost of battery.
    val highAccuracyLocation: Flow<Boolean> = context.dataStore.data.map { it[highAccuracyLocationKey] ?: false }

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

    suspend fun saveWeatherEntity(entityId: String) { context.dataStore.edit { it[weatherEntityIdKey] = entityId } }
    suspend fun saveDisplayName(name: String) { context.dataStore.edit { it[displayNameKey] = name } }
    suspend fun saveAreaOrder(order: List<String>) { context.dataStore.edit { it[areaOrderKey] = order.joinToString(",") } }
    suspend fun saveDashboardMode(mode: String) { context.dataStore.edit { it[dashboardModeKey] = mode } }
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

    suspend fun saveMobileAppDeviceId(id: String) { context.dataStore.edit { it[mobileAppDeviceIdKey] = id } }
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
    suspend fun saveHeaderAlarmEntity(entityId: String?) {
        context.dataStore.edit { if (entityId == null) it.remove(alarmEntityKey) else it[alarmEntityKey] = entityId }
    }
    suspend fun saveHeaderLeftAlarmEntity(entityId: String?) {
        context.dataStore.edit { if (entityId == null) it.remove(headerLeftAlarmEntityKey) else it[headerLeftAlarmEntityKey] = entityId }
    }

    suspend fun saveWeatherExtraEntity(role: String, entityId: String?) {
        context.dataStore.edit { preferences ->
            val key = when(role) {
                "sun" -> sunEntityKey
                "moon" -> moonEntityKey
                "aqi" -> aqiEntityKey
                "season" -> seasonEntityKey
                "rain" -> rainEntityKey
                else -> return@edit
            }
            if (entityId == null) preferences.remove(key) else preferences[key] = entityId
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

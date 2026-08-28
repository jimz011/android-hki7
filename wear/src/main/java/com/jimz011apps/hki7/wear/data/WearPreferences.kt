package com.jimz011apps.hki7.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.data.WearRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.wearDataStore: DataStore<Preferences> by preferencesDataStore(name = "hki7_wear")

internal val wearJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * What the watch knows: its own Home Assistant session, and what the phone chose to show on it.
 *
 * The session is the watch's own — a refresh token it can renew from indefinitely, obtained either
 * from the phone over the Data Layer or from its own browser-on-phone login. What is *displayed*
 * still comes from the phone, because curating a list is a phone-sized job.
 */
class WearPreferences(private val context: Context) {

    private val serverUrlKey = stringPreferencesKey("server_url")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val accessTokenExpiryKey = longPreferencesKey("access_token_expiry")
    private val quickActionsKey = stringPreferencesKey("quick_actions")
    private val roomsKey = stringPreferencesKey("rooms")
    private val deviceIdKey = stringPreferencesKey("mobile_app_device_id")
    private val webhookIdKey = stringPreferencesKey("mobile_app_webhook_id")
    private val webhookUrlKey = stringPreferencesKey("mobile_app_registered_url")
    private val sensorsEnabledKey = booleanPreferencesKey("sensors_enabled")
    private val thermostatsKey = stringPreferencesKey("thermostat_entity_ids")
    private val thermostatIndexKey = intPreferencesKey("thermostat_index")

    val serverUrl: Flow<String?> = context.wearDataStore.data.map { it[serverUrlKey] }

    /** True once the watch has a server and a refresh token it can renew from. */
    val isConfigured: Flow<Boolean> = context.wearDataStore.data.map {
        !it[serverUrlKey].isNullOrBlank() && !it[refreshTokenKey].isNullOrBlank()
    }

    val quickActions: Flow<List<HKIQuickAction>> = context.wearDataStore.data.map { prefs ->
        decode(prefs[quickActionsKey], emptyList())
    }

    val rooms: Flow<List<WearRoom>> = context.wearDataStore.data.map { prefs ->
        decode(prefs[roomsKey], emptyList())
    }

    suspend fun serverUrlOnce(): String? =
        context.wearDataStore.data.first()[serverUrlKey]?.takeIf { it.isNotBlank() }

    suspend fun refreshToken(): String? =
        context.wearDataStore.data.first()[refreshTokenKey]?.takeIf { it.isNotBlank() }

    /** The stored access token if it has not expired, otherwise null. */
    suspend fun validAccessToken(): String? {
        val prefs = context.wearDataStore.data.first()
        val token = prefs[accessTokenKey]?.takeIf { it.isNotBlank() } ?: return null
        val expiry = prefs[accessTokenExpiryKey] ?: return null
        return token.takeIf { System.currentTimeMillis() < expiry }
    }

    suspend fun quickActionsOnce(): List<HKIQuickAction> = quickActions.first()

    /** Whether the watch reports its own battery to Home Assistant. Off until asked for. */
    val sensorsEnabled: Flow<Boolean> = context.wearDataStore.data.map {
        it[sensorsEnabledKey] ?: false
    }

    suspend fun sensorsEnabledOnce(): Boolean = sensorsEnabled.first()

    /** The climate entities the thermostat tile cycles through, chosen on the phone. */
    suspend fun thermostatEntityIds(): List<String> =
        context.wearDataStore.data.first()[thermostatsKey]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    /**
     * Which of them the tile is currently showing.
     *
     * Kept here rather than in the tile's own state so it survives a system-initiated refresh:
     * a tile reset to the first thermostat every quarter of an hour would undo the user's choice
     * while they were not looking.
     */
    suspend fun thermostatIndex(): Int =
        context.wearDataStore.data.first()[thermostatIndexKey] ?: 0

    suspend fun saveThermostatIndex(index: Int) {
        context.wearDataStore.edit { it[thermostatIndexKey] = index }
    }

    suspend fun setSensorsEnabled(enabled: Boolean) {
        context.wearDataStore.edit { it[sensorsEnabledKey] = enabled }
    }

    /** Stable across re-registrations, so Home Assistant keeps one device rather than collecting one
     *  per sign-in. */
    suspend fun deviceId(): String? = context.wearDataStore.data.first()[deviceIdKey]

    suspend fun saveDeviceId(id: String) {
        context.wearDataStore.edit { it[deviceIdKey] = id }
    }

    /** The webhook id, but only if it was issued by the server currently in use. */
    suspend fun webhookIdFor(serverUrl: String): String? {
        val prefs = context.wearDataStore.data.first()
        if (prefs[webhookUrlKey] != serverUrl) return null
        return prefs[webhookIdKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveRegistration(serverUrl: String, webhookId: String) {
        context.wearDataStore.edit {
            it[webhookUrlKey] = serverUrl
            it[webhookIdKey] = webhookId
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresAt: Long) {
        context.wearDataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[accessTokenExpiryKey] = expiresAt
            // Absent means Home Assistant did not rotate it; keep the one already stored.
            refreshToken?.takeIf { it.isNotBlank() }?.let { prefs[refreshTokenKey] = it }
        }
    }

    /** Records a completed setup, from either route. */
    suspend fun saveSession(serverUrl: String, refreshToken: String) {
        context.wearDataStore.edit { prefs ->
            prefs[serverUrlKey] = serverUrl
            prefs[refreshTokenKey] = refreshToken
            // The new refresh token invalidates whatever access token was cached against the old
            // one, and the webhook was issued to whoever was signed in before.
            prefs.remove(accessTokenKey)
            prefs.remove(accessTokenExpiryKey)
            prefs.remove(webhookIdKey)
            prefs.remove(webhookUrlKey)
        }
    }

    /**
     * Stores one handover from the phone. Absent fields leave the previous value alone, so a
     * partial payload from an older phone build cannot wipe the watch's setup.
     */
    suspend fun applyHandover(
        serverUrl: String?,
        refreshToken: String?,
        quickActionsJson: String?,
        roomsJson: String?,
        thermostatEntityIds: String?,
    ) {
        context.wearDataStore.edit { prefs ->
            serverUrl?.takeIf { it.isNotBlank() }?.let { prefs[serverUrlKey] = it }
            refreshToken?.takeIf { it.isNotBlank() }?.let {
                if (prefs[refreshTokenKey] != it) {
                    prefs[refreshTokenKey] = it
                    prefs.remove(accessTokenKey)
                    prefs.remove(accessTokenExpiryKey)
                }
            }
            quickActionsJson?.let { prefs[quickActionsKey] = it }
            roomsJson?.let { prefs[roomsKey] = it }
            // Empty is meaningful here — it is how the phone says "no thermostats any more".
            thermostatEntityIds?.let {
                if (it.isBlank()) {
                    prefs.remove(thermostatsKey)
                } else {
                    prefs[thermostatsKey] = it
                }
                // The list changed, so an index into the old one may now point at a different
                // room, or past the end.
                prefs.remove(thermostatIndexKey)
            }
        }
    }

    /** Forgets the session but keeps nothing — used when the refresh token is rejected. */
    suspend fun clearSession() {
        context.wearDataStore.edit { prefs ->
            prefs.remove(refreshTokenKey)
            prefs.remove(accessTokenKey)
            prefs.remove(accessTokenExpiryKey)
        }
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T =
        raw?.let { runCatching { wearJson.decodeFromString<T>(it) }.getOrNull() } ?: fallback
}

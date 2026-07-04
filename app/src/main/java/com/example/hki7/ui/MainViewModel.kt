@file:Suppress("unused")

package com.example.hki7.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.hki7.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import android.os.SystemClock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class ConnectionStatus {
    IDLE, CONNECTING, CONNECTED, ERROR
}

/** How often the expensive dashboard rebuild (areas/floors/registries/auto-populate) runs. */
private const val DASHBOARD_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

/** Safety re-seed of full entity state. Live updates arrive via the websocket subscription (which
 *  also re-seeds on every reconnect and on foreground return), so this full re-download only needs to
 *  run rarely to self-heal any silently-missed events — not every minute. */
private const val STATE_RESEED_INTERVAL_MS = 15 * 60 * 1000L


class MainViewModel(val prefs: PreferencesManager, appCtx: Context? = null) : ViewModel() {
    private val networkMonitor = appCtx?.let { NetworkMonitor(it) }
    val currentSsid: StateFlow<String?> = networkMonitor?.currentSsid ?: MutableStateFlow(null)

    /** The HA base URL to use right now: the internal URL when on a configured home Wi-Fi, else external. */
    private val activeBaseUrl: Flow<String?> = combine(
        prefs.serverUrl,
        prefs.internalUrl,
        prefs.homeSsids,
        networkMonitor?.currentSsid ?: MutableStateFlow(null)
    ) { external, internal, homeSsids, ssid ->
        resolveBaseUrl(external, internal, homeSsids, ssid)
    }

    private fun resolveBaseUrl(external: String?, internal: String?, homeSsids: List<String>, ssid: String?): String? =
        resolveHomeAssistantUrl(external, internal, homeSsids, ssid)

    private val _entities = MutableStateFlow<List<HAEntity>>(emptyList())
    val entities: StateFlow<List<HAEntity>> = _entities

    private val _areas = MutableStateFlow<List<HAArea>>(emptyList())
    val areas: StateFlow<List<HAArea>> = _areas

    private val _floors = MutableStateFlow<List<HAFloor>>(emptyList())
    val floors: StateFlow<List<HAFloor>> = _floors

    private val _collapsedFloorIds = MutableStateFlow<Set<String>>(emptySet())
    val collapsedFloorIds: StateFlow<Set<String>> = _collapsedFloorIds

    private val _areaWidgetsMapping = MutableStateFlow<Map<String, List<HKIRoomWidget>>>(emptyMap())
    val areaWidgetsMapping: StateFlow<Map<String, List<HKIRoomWidget>>> = _areaWidgetsMapping

    private val _areaConfigsMapping = MutableStateFlow<Map<String, HKIAreaConfig>>(emptyMap())
    val areaConfigsMapping: StateFlow<Map<String, HKIAreaConfig>> = _areaConfigsMapping

    private val _pageConfigsMapping = MutableStateFlow<Map<String, HKIPageConfig>>(emptyMap())
    val pageConfigsMapping: StateFlow<Map<String, HKIPageConfig>> = _pageConfigsMapping

    private val _people = MutableStateFlow<List<HAEntity>>(emptyList())
    val people: StateFlow<List<HAEntity>> = _people

    private val _weather = MutableStateFlow<HAEntity?>(null)
    val weather: StateFlow<HAEntity?> = _weather

    private val _weatherForecast = MutableStateFlow<List<HAWeatherForecast>>(emptyList())
    val weatherForecast: StateFlow<List<HAWeatherForecast>> = _weatherForecast

    // On-demand forecast cache for weather widgets that reference a non-default weather entity,
    // keyed "$entityId:$type" (type = "daily" | "hourly").
    private val _weatherForecastCache = MutableStateFlow<Map<String, List<HAWeatherForecast>>>(emptyMap())
    val weatherForecastCache: StateFlow<Map<String, List<HAWeatherForecast>>> = _weatherForecastCache

    fun fetchWeatherForecastFor(entityId: String, type: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            val result = runCatching { currentClient.getWeatherForecast(entityId, type) }.getOrDefault(emptyList())
            val current = _weatherForecastCache.value.toMutableMap()
            current["$entityId:$type"] = result
            _weatherForecastCache.value = current
        }
    }

    private val _status = MutableStateFlow(ConnectionStatus.IDLE)
    val status: StateFlow<ConnectionStatus> = _status

    private val _displayName = MutableStateFlow("User")
    val displayName: StateFlow<String> = _displayName

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _historyMapping = MutableStateFlow<Map<String, List<HAHistoryEntry>>>(emptyMap())
    val historyMapping: StateFlow<Map<String, List<HAHistoryEntry>>> = _historyMapping

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _dashboardMode = MutableStateFlow("auto")
    val dashboardMode: StateFlow<String> = _dashboardMode

    private val _uiRevision = MutableStateFlow(0)
    val uiRevision: StateFlow<Int> = _uiRevision

    private val _forcedLogoutReason = MutableStateFlow<String?>(null)
    val forcedLogoutReason: StateFlow<String?> = _forcedLogoutReason

    private val undoStack = mutableListOf<Snapshot>()
    private val redoStack = mutableListOf<Snapshot>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private data class Snapshot(
        val widgets: Map<String, List<HKIRoomWidget>>,
        val areaOrder: List<String>,
        val areaConfigs: Map<String, HKIAreaConfig>,
        val floors: List<HAFloor>
    )

    private fun takeSnapshot() {
        undoStack.add(Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _floors.value))
        redoStack.clear()
        if (undoStack.size > 20) undoStack.removeAt(0)
        updateHistoryState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentSnapshot = Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _floors.value)
            redoStack.add(currentSnapshot)
            restoreSnapshot(undoStack.removeAt(undoStack.size - 1))
            updateHistoryState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentSnapshot = Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _floors.value)
            undoStack.add(currentSnapshot)
            restoreSnapshot(redoStack.removeAt(redoStack.size - 1))
            updateHistoryState()
        }
    }

    private fun updateHistoryState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun restoreSnapshot(snapshot: Snapshot) {
        _areaWidgetsMapping.value = snapshot.widgets
        _areaConfigsMapping.value = snapshot.areaConfigs
        _floors.value = snapshot.floors
        sortAreas(snapshot.areaOrder)
        viewModelScope.launch {
            prefs.saveAreaWidgets(snapshot.widgets)
            prefs.saveAreaConfigs(snapshot.areaConfigs)
            prefs.saveAreaOrder(snapshot.areaOrder)
            prefs.saveFloors(snapshot.floors)
        }
    }

    private val _weatherDisplayType = MutableStateFlow("Weather")
    val weatherDisplayType: StateFlow<String> = _weatherDisplayType

    private val _headerLeftDisplayType = MutableStateFlow("None")
    val headerLeftDisplayType: StateFlow<String> = _headerLeftDisplayType

    private val _use24hFormat = MutableStateFlow(true)
    val use24hFormat: StateFlow<Boolean> = _use24hFormat

    private val _useFullDayName = MutableStateFlow(false)
    val useFullDayName: StateFlow<Boolean> = _useFullDayName

    private val _weatherExtraEntities = MutableStateFlow<Map<String, String?>>(emptyMap())
    val weatherExtraEntities: StateFlow<Map<String, String?>> = _weatherExtraEntities

    private val _headerAlarmEntityId = MutableStateFlow<String?>(null)
    val headerAlarmEntityId: StateFlow<String?> = _headerAlarmEntityId

    private val _headerLeftAlarmEntityId = MutableStateFlow<String?>(null)
    val headerLeftAlarmEntityId: StateFlow<String?> = _headerLeftAlarmEntityId

    private var client: HomeAssistantClient? = null
    private var pollJob: Job? = null
    private var realtimeJob: Job? = null
    private val realtimeBuffer = java.util.concurrent.ConcurrentHashMap<String, HAStateChange>()
    private var refreshJob: Job? = null
    private var tokenRefreshJob: Job? = null
    private var highAccuracyJob: Job? = null
    private val refreshMutex = Mutex()
    private var lastTokenRefreshAt = 0L
    private var appVisible = true
    private var lastDashboardRefreshAt = 0L
    private var appContext: Context? = null
    private var batteryReceiver: android.content.BroadcastReceiver? = null
    private var lastReportedBatteryPct = -1
    private var lastReportedCharging: Boolean? = null
    private var ignoreWidgetPrefsUntil = 0L
    private var ignoreConfigPrefsUntil = 0L
    private var activeConnectionKey: String? = null
    private var startupRefreshAttemptedFor: String? = null
    // Touched from both the background poll (Dispatchers.Default) and main-thread optimistic
    // setters, so these must be concurrency-safe.
    private val pendingEntityStates = java.util.concurrent.ConcurrentHashMap<String, PendingEntityState>()
    private val pendingBrightness = java.util.concurrent.ConcurrentHashMap<String, PendingBrightness>()
    private val pendingCoverPosition = java.util.concurrent.ConcurrentHashMap<String, PendingCoverPosition>()
    private val pendingCoverTilt = java.util.concurrent.ConcurrentHashMap<String, PendingCoverPosition>()

    private data class PendingEntityState(val state: String, val expiresAt: Long, val requestedAt: Long)
    private data class PendingBrightness(val brightness: Int, val expiresAt: Long, val requestedAt: Long)
    private data class PendingCoverPosition(val position: Int, val expiresAt: Long, val requestedAt: Long)

    init {
        observeSettings()
        observeAreaOrder()
        observeAreaWidgets()
        observeAreaConfigs()
        observePageConfigs()
        observeDashboard()
        observeWeatherPrefs()
    }

    private fun observeWeatherPrefs() {
        viewModelScope.launch { prefs.weatherDisplayType.collect { _weatherDisplayType.value = it } }
        viewModelScope.launch { prefs.headerLeftDisplayType.collect { _headerLeftDisplayType.value = it } }
        viewModelScope.launch { prefs.use24hFormat.collect { _use24hFormat.value = it } }
        viewModelScope.launch { prefs.useFullDayName.collect { _useFullDayName.value = it } }
        viewModelScope.launch { prefs.alarmEntityId.collect { _headerAlarmEntityId.value = it } }
        viewModelScope.launch { prefs.headerLeftAlarmEntityId.collect { _headerLeftAlarmEntityId.value = it } }
        viewModelScope.launch {
            combine(prefs.sunEntityId, prefs.moonEntityId, prefs.aqiEntityId, prefs.seasonEntityId, prefs.rainEntityId) { args: Array<String?> ->
                mapOf("sun" to args[0], "moon" to args[1], "aqi" to args[2], "season" to args[3], "rain" to args[4])
            }.collect { _weatherExtraEntities.value = it }
        }
    }

    private fun observeAreaWidgets() {
        viewModelScope.launch {
            prefs.areaWidgets.collect { saved ->
                if (SystemClock.elapsedRealtime() < ignoreWidgetPrefsUntil && saved != _areaWidgetsMapping.value) return@collect
                _areaWidgetsMapping.value = saved
            }
        }
    }

    private fun observeAreaConfigs() {
        viewModelScope.launch {
            prefs.areaConfigs.collect { saved ->
                if (SystemClock.elapsedRealtime() < ignoreConfigPrefsUntil && saved != _areaConfigsMapping.value) return@collect
                _areaConfigsMapping.value = saved
            }
        }
    }
    private fun observePageConfigs() {
        viewModelScope.launch {
            prefs.pageConfigs.collect { saved -> _pageConfigsMapping.value = saved }
        }
    }
    private fun observeAreaOrder() {
        // No-op: area ordering is handled atomically inside observeDashboard via combine
    }
    private fun observeDashboard() {
        viewModelScope.launch { prefs.dashboardMode.collect { _dashboardMode.value = it } }
        // Combine savedAreas + areaOrder so we never read a stale order when areas change.
        // When moveArea() updates _areas.value AND writes areaOrder to DataStore, the combine
        // fires with BOTH new values simultaneously — no race condition.
        viewModelScope.launch {
            combine(prefs.savedAreas, prefs.areaOrder) { saved, order -> saved to order }
                .collect { (saved, order) ->
                    if (_dashboardMode.value != "auto" && saved.isNotEmpty()) {
                        val sorted = if (order.isNotEmpty()) {
                            saved.sortedBy { a -> order.indexOf(a.area_id).let { if (it == -1) Int.MAX_VALUE else it } }
                        } else saved
                        // Only update _areas if the sorted IDs differ from what's already in memory.
                        // This prevents overwriting a moveArea() update with stale prefs data.
                        val targetIds = sorted.map { it.area_id }
                        if (_areas.value.map { it.area_id } != targetIds) {
                            _areas.value = sorted
                        }
                    }
                }
        }
        viewModelScope.launch {
            prefs.savedFloors.collect { saved ->
                if (_dashboardMode.value != "auto") _floors.value = saved
            }
        }
    }

    private fun sortAreas(order: List<String>) {
        val currentAreas = _areas.value
        if (currentAreas.isEmpty()) return
        val sorted = currentAreas.sortedBy { area ->
            val index = order.indexOf(area.area_id)
            if (index == -1) Int.MAX_VALUE else index
        }
        if (sorted != currentAreas) _areas.value = sorted
    }

    private fun addLog(message: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _logs.value = (listOf("[$timestamp] $message") + _logs.value).take(100)
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(activeBaseUrl, prefs.accessToken, prefs.refreshToken, prefs.displayName) { url, token, refresh, name ->
                HKIAuthSettings(url, token, refresh, name)
            }.collect { settings ->
                _displayName.value = settings.name ?: "User"
                _currentUrl.value = settings.url ?: ""
                _accessToken.value = settings.token
                val connectionKey = "${settings.url}|${settings.token}"
                when {
                    settings.url.isNullOrBlank() -> {
                        addLog("Logged out or settings cleared.")
                        stopSync()
                        tokenRefreshJob?.cancel()
                        tokenRefreshJob = null
                        highAccuracyJob?.cancel()
                        highAccuracyJob = null
                        appContext?.let {
                            LocationForegroundService.stop(it)
                            LocationWork.cancel(it)
                        }
                        client = null
                        activeConnectionKey = null
                        _status.value = ConnectionStatus.IDLE
                        _entities.value = emptyList()
                        pendingEntityStates.clear()
                        pendingBrightness.clear()
                        pendingCoverPosition.clear()
                        pendingCoverTilt.clear()
                        _people.value = emptyList()
                        _weather.value = null
                        _weatherForecast.value = emptyList()
                        _areas.value = emptyList()
                    }
                    !settings.token.isNullOrBlank() -> {
                        if (activeConnectionKey != connectionKey) {
                            activeConnectionKey = connectionKey
                            addLog("Connecting to ${settings.url}")
                            _status.value = ConnectionStatus.CONNECTING
                            client = HomeAssistantClient(settings.url, settings.token)
                            if (appVisible) startSync()
                            scheduleProactiveRefreshFromStoredExpiry()
                        }
                    }
                    !settings.refresh.isNullOrBlank() && startupRefreshAttemptedFor != settings.refresh -> {
                        startupRefreshAttemptedFor = settings.refresh
                        _status.value = ConnectionStatus.CONNECTING
                        addLog("Restoring Home Assistant session...")
                        if (tryTokenRefresh()) {
                            if (appVisible) startSync()
                        } else {
                            _status.value = ConnectionStatus.ERROR
                            addLog("Session restore failed. Login is required.")
                        }
                    }
                }
            }
        }
    }

    private data class HKIAuthSettings(val url: String?, val token: String?, val refresh: String?, val name: String?)

    private fun startSync() {
        startPolling()
        startRealtimeSync()
    }

    private fun stopSync() {
        stopPolling()
        stopRealtimeSync()
        client?.closeSession()
    }

    /** Live updates come from the websocket subscription; this REST loop is now only a slow safety
     *  re-seed (in case events are missed) plus the periodic dashboard/registry rebuild. */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var firstRun = true
            while (true) {
                val needsDashboard = firstRun ||
                    (SystemClock.elapsedRealtime() - lastDashboardRefreshAt > DASHBOARD_REFRESH_INTERVAL_MS)
                refreshEntities(
                    isSilent = !firstRun,
                    includeDashboardRefresh = needsDashboard
                )
                firstRun = false
                delay(STATE_RESEED_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun stopPolling() { pollJob?.cancel(); pollJob = null }

    /** Holds one persistent websocket and applies only entity *changes* (like the official HA app),
     *  instead of re-downloading the full state list every couple of seconds. Re-seeds and
     *  re-subscribes whenever the socket drops. */
    private fun startRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch(Dispatchers.Default) {
            while (currentCoroutineContext().isActive) {
                val currentClient = client
                if (currentClient == null) {
                    delay(2.seconds)
                    continue
                }
                try {
                    // Catch up on anything missed while disconnected, then stream live changes.
                    refreshEntities(isSilent = true, includeDashboardRefresh = false)
                    // Event-driven flush: idle until a change arrives (no 8x/sec wakeups), then debounce
                    // ~120ms to coalesce a burst into a single UI update. CONFLATED so a flurry of
                    // events collapses to one pending signal.
                    val flushSignal = Channel<Unit>(Channel.CONFLATED)
                    val flusher = launch {
                        for (signal in flushSignal) {
                            delay(120)
                            flushRealtimeBuffer()
                        }
                    }
                    try {
                        currentClient.subscribeStateChanges().collect { change ->
                            realtimeBuffer[change.entityId] = change
                            flushSignal.trySend(Unit)
                        }
                    } finally {
                        flusher.cancel()
                        flushSignal.close()
                        flushRealtimeBuffer()
                    }
                } catch (e: Exception) {
                    if (e.message == "AUTH_EXPIRED") tryTokenRefresh()
                    else addLog("Realtime sync interrupted: ${e.message}")
                }
                delay(3.seconds) // backoff before reconnecting
            }
        }
    }

    private fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeBuffer.clear()
    }

    /** Applies all buffered state changes in one batch so a burst of events causes a single UI
     *  update rather than one recomposition per event. */
    private fun flushRealtimeBuffer() {
        if (realtimeBuffer.isEmpty()) return
        val snapshot = HashMap(realtimeBuffer)
        snapshot.forEach { (k, v) -> realtimeBuffer.remove(k, v) }
        val ordered = LinkedHashMap<String, HAEntity>(_entities.value.size + snapshot.size)
        _entities.value.forEach { ordered[it.entity_id] = it }
        var changed = false
        snapshot.values.forEach { change ->
            if (change.newState == null) {
                if (ordered.remove(change.entityId) != null) changed = true
            } else {
                ordered[change.entityId] = change.newState
                changed = true
            }
        }
        if (!changed) return
        val merged = applyPendingEntityStates(ordered.values.toList())
        _entities.value = merged
        if (snapshot.keys.any { it.startsWith("person.") }) {
            _people.value = merged.filter { it.entity_id.startsWith("person.") }
        }
        _weather.value?.entity_id?.let { weatherId ->
            if (snapshot.containsKey(weatherId)) {
                merged.find { it.entity_id == weatherId }?.let { _weather.value = it }
            }
        }
    }

    /** Called from the UI lifecycle: hold the websocket only while the app is visible to avoid
     *  pointless background work, and re-seed immediately on return so the user sees fresh state. */
    fun setAppVisible(visible: Boolean) {
        if (appVisible == visible) return
        appVisible = visible
        if (visible) {
            if (client != null && realtimeJob?.isActive != true) startSync()
            setBatteryMonitoring(true)
            appContext?.let { reportDeviceTelemetry(it) }
        } else {
            setBatteryMonitoring(false)
            stopSync()
        }
    }

    fun refreshEntities(
        isSilent: Boolean = false,
        allowReconnectRetry: Boolean = true,
        includeDashboardRefresh: Boolean = true
    ) {
        val now = SystemClock.elapsedRealtime()
        if (isSilent && refreshJob?.isActive == true) return
        // Run network + JSON parsing + dashboard computation off the UI thread to avoid
        // startup/scroll jank. All state writes below go through thread-safe StateFlow/DataStore.
        refreshJob = viewModelScope.launch(Dispatchers.Default) {
            val currentClient = client ?: rebuildClientFromPrefs() ?: run {
                if (!isSilent) _status.value = ConnectionStatus.ERROR
                return@launch
            }
            if (!isSilent) _status.value = ConnectionStatus.CONNECTING
            try {
                val allEntities = withTimeout(10.seconds) { currentClient.getEntities() }
                val displayEntities = applyPendingEntityStates(allEntities)
                _entities.value = displayEntities
                _people.value = displayEntities.filter { it.entity_id.startsWith("person.") }

                val weatherId = prefs.weatherEntityId.first() ?: "weather.home"

                if (_displayName.value == "User") {
                    currentClient.getCurrentUserName()?.let { realName ->
                        _displayName.value = realName
                        prefs.saveDisplayName(realName)
                    }
                }

                _weather.value = displayEntities.find { it.entity_id == weatherId } ?: displayEntities.find { it.entity_id.startsWith("weather.") }
                _weatherForecast.value = _weather.value?.let { weather ->
                    weather.forecast.takeUnless { it.isNullOrEmpty() }
                        ?: runCatching { withTimeout(10.seconds) { currentClient.getWeatherForecast(weather.entity_id) } }.getOrDefault(emptyList())
                }.orEmpty()
                if (includeDashboardRefresh && _dashboardMode.value == "auto") {
                    val allAreas = withTimeout(10.seconds) { currentClient.getAreas() }
                    val allFloors = withTimeout(10.seconds) { currentClient.getFloors() }
                    val entityRegistry = withTimeout(10.seconds) { currentClient.getEntityRegistry() }
                    val deviceRegistry = withTimeout(10.seconds) { currentClient.getDeviceRegistry() }
                    val savedOrder = prefs.areaOrder.first()
                    _areas.value = if (savedOrder.isNotEmpty()) {
                        allAreas.sortedBy { a -> savedOrder.indexOf(a.area_id).let { if (it == -1) Int.MAX_VALUE else it } }
                    } else allAreas
                    _floors.value = allFloors
                    autoPopulateDashboard(allAreas, allFloors, allEntities, entityRegistry, deviceRegistry)
                    lastDashboardRefreshAt = SystemClock.elapsedRealtime()
                }
                _status.value = ConnectionStatus.CONNECTED
            } catch (e: Exception) {
                if (e.message == "AUTH_EXPIRED") {
                    addLog("Session expired. Attempting refresh...")
                    if (tryTokenRefresh()) {
                        refreshEntities(isSilent = isSilent, includeDashboardRefresh = includeDashboardRefresh)
                        return@launch
                    }
                    // tryTokenRefresh either forced logout (auth error) or hit network issues.
                    // Status is already set to ERROR in tryTokenRefresh for network issues.
                    if (!isSilent) _status.value = ConnectionStatus.ERROR
                    return@launch
                }
                if (!isSilent) {
                    val recoveredClient = rebuildClientFromPrefs()
                    if (allowReconnectRetry && recoveredClient != null && recoveredClient !== currentClient) {
                        addLog("Reconnected. Retrying refresh...")
                        refreshEntities(
                            isSilent = false,
                            allowReconnectRetry = false,
                            includeDashboardRefresh = includeDashboardRefresh
                        )
                    } else {
                        _status.value = ConnectionStatus.ERROR
                        addLog("Refresh Error: ${e.message}")
                    }
                } else {
                    _status.value = ConnectionStatus.ERROR
                }
            }
        }
    }

    private suspend fun rebuildClientFromPrefs(): HomeAssistantClient? {
        val url = resolveBaseUrl(
            prefs.serverUrl.first(),
            prefs.internalUrl.first(),
            prefs.homeSsids.first(),
            networkMonitor?.currentSsid?.value
        )?.takeIf { it.isNotBlank() } ?: return null
        val token = prefs.accessToken.first()?.takeIf { it.isNotBlank() }
        val refresh = prefs.refreshToken.first()?.takeIf { it.isNotBlank() }
        if (token != null) {
            val rebuilt = HomeAssistantClient(url, token)
            client = rebuilt
            activeConnectionKey = "$url|$token"
            return rebuilt
        }
        if (refresh != null && tryTokenRefresh()) {
            return client
        }
        return null
    }

    private suspend fun tryTokenRefresh(): Boolean = refreshMutex.withLock {
        // If another caller refreshed moments ago, reuse that result instead of refreshing again.
        if (SystemClock.elapsedRealtime() - lastTokenRefreshAt < 5_000 && !_accessToken.value.isNullOrBlank()) {
            return@withLock true
        }
        val url = _currentUrl.value
        val refresh = prefs.refreshToken.first()
        if (url.isNotEmpty() && refresh != null) {
            val delays = listOf(0L, 1000L, 4000L, 10000L)
            for ((attempt, delayMs) in delays.withIndex()) {
                try {
                    if (attempt > 0) kotlinx.coroutines.delay(delayMs)
                    val response = HomeAssistantClient.refreshAccessToken(url, refresh)
                    prefs.saveConnectionDetails(url, response.access_token, response.refresh_token, response.expires_in)
                    client = HomeAssistantClient(url, response.access_token)
                    activeConnectionKey = "$url|${response.access_token}"
                    lastTokenRefreshAt = SystemClock.elapsedRealtime()
                    scheduleProactiveRefresh(response.expires_in)
                    addLog("Token refreshed successfully")
                    return@withLock true
                } catch (e: Exception) {
                    addLog("Token refresh attempt ${attempt + 1} failed: ${e.message}")
                    // Only force re-login when the server explicitly rejected the refresh token.
                    if (e is TokenRefreshException && e.invalidGrant) {
                        addLog("Server rejected refresh token (invalid_grant); re-login required.")
                        _forcedLogoutReason.value = "Session expired. Please log in again."
                        prefs.clearAuth()
                        return@withLock false
                    }
                }
            }
            // All attempts failed without an explicit rejection — keep auth and let polling retry.
            addLog("Token refresh failed after ${delays.size} attempts (transient). Will retry later.")
            _status.value = ConnectionStatus.ERROR
        } else {
            addLog("No refresh token available; login may be required.")
        }
        return@withLock false
    }

    /** Schedules a single token refresh shortly before the current access token expires, so the
     *  app keeps a valid token proactively rather than only reacting to 401s. */
    private fun scheduleProactiveRefresh(expiresInSeconds: Int) {
        tokenRefreshJob?.cancel()
        val leadSeconds = 60L
        val delaySeconds = (expiresInSeconds - leadSeconds).coerceAtLeast(30L)
        tokenRefreshJob = viewModelScope.launch {
            delay(delaySeconds.seconds)
            addLog("Proactively refreshing access token before expiry")
            tryTokenRefresh()
        }
    }

    /** On startup we may hold a stored token of unknown freshness; schedule a proactive refresh
     *  from the persisted expiry (or refresh immediately if it is already expired / unknown). */
    private fun scheduleProactiveRefreshFromStoredExpiry() {
        tokenRefreshJob?.cancel()
        tokenRefreshJob = viewModelScope.launch {
            val expiry = prefs.accessTokenExpiry.first()
            val remainingSeconds = if (expiry == null) 0L
                else ((expiry - System.currentTimeMillis()) / 1000L)
            val leadSeconds = 60L
            val delaySeconds = (remainingSeconds - leadSeconds).coerceAtLeast(0L)
            if (delaySeconds > 0L) delay(delaySeconds.seconds)
            addLog("Proactively refreshing access token before expiry")
            tryTokenRefresh()
        }
    }

    fun fetchEntityHistory(entityId: String, hours: Long = 24) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                val history = currentClient.getEntityHistory(entityId, hours)
                val current = _historyMapping.value.toMutableMap()
                current[entityId] = history
                _historyMapping.value = current
            } catch (e: Exception) {
                addLog("History fetch failed for $entityId: ${e.message}")
            }
        }
    }

    private suspend fun refreshAfterServiceCall() {
        delay(150.milliseconds)
        refreshEntities(isSilent = true, includeDashboardRefresh = false)
        delay(850.milliseconds)
        refreshEntities(isSilent = true, includeDashboardRefresh = false)
    }

    fun toggleEntity(entityId: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                optimisticallyToggleEntity(entityId)
                currentClient.toggleEntity(entityId)
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Toggle failed: ${e.message}")
                refreshEntities(isSilent = true)
            }
        }
    }

    private fun optimisticallyToggleEntity(entityId: String) {
        val now = SystemClock.elapsedRealtime()
        _entities.value = _entities.value.map { entity ->
            if (entity.entity_id != entityId) {
                entity
            } else {
                val desiredState = when (entity.state.lowercase()) {
                    "on", "open", "unlocked" -> "off"
                    "off", "closed", "locked" -> "on"
                    else -> entity.state
                }
                pendingEntityStates[entityId] = PendingEntityState(desiredState, now + 8_000, now)
                entity.copy(
                    state = desiredState
                )
            }
        }
    }

    private fun applyPendingEntityStates(entities: List<HAEntity>): List<HAEntity> {
        val now = SystemClock.elapsedRealtime()
        val byId = entities.associateBy { it.entity_id }
        val confirmDelay = 500L
        pendingEntityStates.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (byId[entityId]?.state?.equals(pending.state, ignoreCase = true) == true && now >= pending.requestedAt + confirmDelay)
        }
        pendingBrightness.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (byId[entityId]?.brightness == pending.brightness && now >= pending.requestedAt + confirmDelay)
        }
        pendingCoverPosition.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (byId[entityId]?.attributes?.get("current_position")?.jsonPrimitive?.intOrNull == pending.position && now >= pending.requestedAt + confirmDelay)
        }
        pendingCoverTilt.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (byId[entityId]?.attributes?.get("current_tilt_position")?.jsonPrimitive?.intOrNull == pending.position && now >= pending.requestedAt + confirmDelay)
        }
        if (pendingEntityStates.isEmpty() && pendingBrightness.isEmpty() && pendingCoverPosition.isEmpty() && pendingCoverTilt.isEmpty()) return entities
        return entities.map { entity ->
            var modified = pendingEntityStates[entity.entity_id]?.let { pending -> entity.copy(state = pending.state) } ?: entity
            pendingBrightness[entity.entity_id]?.let { pending ->
                val newAttrs = buildJsonObject {
                    // copy existing attributes
                    entity.attributes?.forEach { (k, v) -> put(k, v) }
                    put("brightness", JsonPrimitive(pending.brightness))
                }
                modified = modified.copy(attributes = newAttrs)
            }
            pendingCoverPosition[entity.entity_id]?.let { pending ->
                val newAttrs = buildJsonObject {
                    // copy existing attributes (from any prior modification)
                    (modified.attributes ?: entity.attributes)?.forEach { (k, v) -> put(k, v) }
                    put("current_position", JsonPrimitive(pending.position))
                }
                modified = modified.copy(attributes = newAttrs)
            }
            pendingCoverTilt[entity.entity_id]?.let { pending ->
                val newAttrs = buildJsonObject {
                    (modified.attributes ?: entity.attributes)?.forEach { (k, v) -> put(k, v) }
                    put("current_tilt_position", JsonPrimitive(pending.position))
                }
                modified = modified.copy(attributes = newAttrs)
            }
            modified
        }
    }

    fun setBrightness(entityId: String, brightness: Float) {
        val haBrightness = (brightness * 255).toInt().coerceIn(0, 255)
        val now = SystemClock.elapsedRealtime()
        pendingBrightness[entityId] = PendingBrightness(haBrightness, now + 8_000, now)
        // update UI optimistically
        _entities.value = applyPendingEntityStates(_entities.value)
        callLightService(entityId, "turn_on", HAServiceCall(entity_id = entityId, brightness = haBrightness))
    }

    fun setOptimisticBrightness(entityId: String, brightness: Float) {
        val haBrightness = (brightness * 255).toInt().coerceIn(0, 255)
        val now = SystemClock.elapsedRealtime()
        pendingBrightness[entityId] = PendingBrightness(haBrightness, now + 8_000, now)
        _entities.value = applyPendingEntityStates(_entities.value)
    }

    fun setColorTemp(entityId: String, kelvin: Int) {
        callLightService(entityId, "turn_on", HAServiceCall(entity_id = entityId, color_temp_kelvin = kelvin))
    }

    fun setRgbColor(entityId: String, rgb: List<Int>) {
        callLightService(entityId, "turn_on", HAServiceCall(entity_id = entityId, rgb_color = rgb))
    }

    fun setLightEffect(entityId: String, effect: String) {
        callLightService(entityId, "turn_on", HAServiceCall(entity_id = entityId, effect = effect))
    }

    fun saveWeatherEntity(entityId: String) {
        viewModelScope.launch {
            prefs.saveWeatherEntity(entityId)
            refreshEntities(isSilent = true)
        }
    }

    fun setWeatherDisplayType(type: String) { viewModelScope.launch { prefs.saveWeatherDisplayType(type) } }
    fun setHeaderLeftDisplayType(type: String) { viewModelScope.launch { prefs.saveHeaderLeftDisplayType(type) } }
    fun setUse24hFormat(use24h: Boolean) { viewModelScope.launch { prefs.saveUse24hFormat(use24h) } }
    fun setUseFullDayName(useFullDayName: Boolean) { viewModelScope.launch { prefs.saveUseFullDayName(useFullDayName) } }
    fun setWeatherExtraEntity(role: String, entityId: String?) { viewModelScope.launch { prefs.saveWeatherExtraEntity(role, entityId) } }
    fun setHeaderAlarmEntity(entityId: String?) { viewModelScope.launch { prefs.saveHeaderAlarmEntity(entityId) } }
    fun setHeaderLeftAlarmEntity(entityId: String?) { viewModelScope.launch { prefs.saveHeaderLeftAlarmEntity(entityId) } }

    fun toggleLock(entityId: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                val state = _entities.value.find { it.entity_id == entityId }?.state
                val service = if (state == "locked") "unlock" else "lock"
                currentClient.callService("lock", service, HAServiceCall(entity_id = entityId))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Lock toggle failed: ${e.message}")
            }
        }
    }

    fun openLock(entityId: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("lock", "open", HAServiceCall(entity_id = entityId))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Lock open failed: ${e.message}")
            }
        }
    }

    fun vacuumCommand(entityId: String, service: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("vacuum", service, HAServiceCall(entity_id = entityId))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Vacuum $service failed: ${e.message}")
            }
        }
    }

    fun vacuumSetFanSpeed(entityId: String, fanSpeed: String) {
        callService("vacuum", "set_fan_speed", HAServiceCall(entity_id = entityId, fan_speed = fanSpeed))
    }

    fun vacuumCleanSegments(entityId: String, segments: List<Int>) {
        callService("vacuum", "clean_segment", HAServiceCall(entity_id = entityId, segments = segments))
    }

    fun vacuumSendCommand(entityId: String, command: String) {
        callService("vacuum", "send_command", HAServiceCall(entity_id = entityId, command = command))
    }

    fun updateEnergyConfig(pageKey: String, config: com.example.hki7.data.HKIEnergyConfig) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        updatePageConfig(pageKey, current.copy(energyConfig = config))
    }

    fun updateVacuumConfig(pageKey: String, entityId: String?, mapEntityId: String?) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        updatePageConfig(pageKey, current.copy(vacuumEntityId = entityId, vacuumMapEntityId = mapEntityId))
    }

    fun controlCover(entityId: String, service: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("cover", service, HAServiceCall(entity_id = entityId))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Cover control failed: ${e.message}")
            }
        }
    }

    fun setOptimisticCoverPosition(entityId: String, position: Int) {
        val pos = position.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        pendingCoverPosition[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        _entities.value = applyPendingEntityStates(_entities.value)
    }

    fun setCoverPosition(entityId: String, position: Int) {
        val currentClient = client ?: return
        val pos = position.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        // hold the target optimistically while the blind physically moves (mirrors light brightness)
        pendingCoverPosition[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        _entities.value = applyPendingEntityStates(_entities.value)
        viewModelScope.launch {
            try {
                currentClient.callService("cover", "set_cover_position", HAServiceCall(entity_id = entityId, position = pos))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Cover position failed: ${e.message}")
            }
        }
    }

    fun setClimateTemp(entityId: String, temp: Float) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("climate", "set_temperature", HAServiceCall(entity_id = entityId, temperature = temp))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Climate set failed: ${e.message}")
            }
        }
    }

    fun setHvacMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("climate", "set_hvac_mode", HAServiceCall(entity_id = entityId, hvac_mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("HVAC mode failed: ${e.message}")
            }
        }
    }

    fun setClimateFanMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("climate", "set_fan_mode", HAServiceCall(entity_id = entityId, fan_mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Fan mode failed: ${e.message}")
            }
        }
    }

    fun setClimateSwingMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("climate", "set_swing_mode", HAServiceCall(entity_id = entityId, swing_mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Swing mode failed: ${e.message}")
            }
        }
    }

    fun setClimateSwingHorizontalMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("climate", "set_swing_horizontal_mode", HAServiceCall(entity_id = entityId, swing_horizontal_mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Swing horizontal mode failed: ${e.message}")
            }
        }
    }

    fun setOptimisticCoverTilt(entityId: String, tilt: Int) {
        val pos = tilt.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        pendingCoverTilt[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        _entities.value = applyPendingEntityStates(_entities.value)
    }

    fun setCoverTiltPosition(entityId: String, tilt: Int) {
        val currentClient = client ?: return
        val pos = tilt.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        pendingCoverTilt[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        _entities.value = applyPendingEntityStates(_entities.value)
        viewModelScope.launch {
            try {
                currentClient.callService("cover", "set_cover_tilt_position", HAServiceCall(entity_id = entityId, tilt_position = pos))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Cover tilt failed: ${e.message}")
            }
        }
    }

    fun setFanPercentage(entityId: String, percentage: Int) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("fan", "set_percentage", HAServiceCall(entity_id = entityId, percentage = percentage.coerceIn(0, 100)))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Fan speed failed: ${e.message}")
            }
        }
    }

    fun setFanPresetMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("fan", "set_preset_mode", HAServiceCall(entity_id = entityId, preset_mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Fan preset failed: ${e.message}")
            }
        }
    }

    fun setFanOscillating(entityId: String, oscillating: Boolean) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("fan", "oscillate", HAServiceCall(entity_id = entityId, oscillating = oscillating))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Fan oscillate failed: ${e.message}")
            }
        }
    }

    fun setFanDirection(entityId: String, direction: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("fan", "set_direction", HAServiceCall(entity_id = entityId, direction = direction))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Fan direction failed: ${e.message}")
            }
        }
    }

    fun setHumidifierTarget(entityId: String, humidity: Int) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("humidifier", "set_humidity", HAServiceCall(entity_id = entityId, humidity = humidity))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Humidifier target failed: ${e.message}")
            }
        }
    }

    fun setHumidifierMode(entityId: String, mode: String) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("humidifier", "set_mode", HAServiceCall(entity_id = entityId, mode = mode))
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Humidifier mode failed: ${e.message}")
            }
        }
    }

    /** Arms/disarms an alarm_control_panel. [onResult] reports success so the keypad can show
     * "incorrect code" feedback when Home Assistant rejects the code. */
    fun setAlarmState(entityId: String, service: String, code: String?, onResult: (Boolean) -> Unit) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                val beforeState = _entities.value.find { it.entity_id == entityId }?.state?.lowercase()
                currentClient.callService(
                    "alarm_control_panel",
                    service,
                    HAServiceCall(entity_id = entityId, code = code?.takeIf { it.isNotBlank() })
                )
                refreshAfterServiceCall()
                val afterState = _entities.value.find { it.entity_id == entityId }?.state?.lowercase()
                val stateAccepted = when (service) {
                    "alarm_disarm" -> afterState == "disarmed"
                    "alarm_arm_home" -> afterState in setOf("armed_home", "arming", "pending")
                    "alarm_arm_away" -> afterState in setOf("armed_away", "arming", "pending")
                    "alarm_arm_night" -> afterState in setOf("armed_night", "arming", "pending")
                    "alarm_arm_custom_bypass" -> afterState in setOf("armed_custom_bypass", "arming", "pending")
                    "alarm_arm_vacation" -> afterState in setOf("armed_vacation", "arming", "pending")
                    else -> afterState != beforeState
                }
                onResult(stateAccepted)
            } catch (e: Exception) {
                addLog("Alarm command failed: ${e.message}")
                onResult(false)
            }
        }
    }

    fun callService(domain: String, service: String, call: HAServiceCall) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService(domain, service, call)
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Service call failed: ${e.message}")
            }
        }
    }

    private fun callLightService(entityId: String, service: String, call: HAServiceCall) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callService("light", service, call)
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Service call failed: ${e.message}")
            }
        }
    }

    fun logout(keepConfig: Boolean = true) {
        viewModelScope.launch {
            if (keepConfig) prefs.clearAuth() else prefs.clearAll()
        }
    }

    fun clearForcedLogoutReason() {
        _forcedLogoutReason.value = null
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            // Save the current area order so room reorder is preserved
            val currentOrder = _areas.value.map { it.area_id }
            viewModelScope.launch { prefs.saveAreaOrder(currentOrder) }
            _areaWidgetsMapping.value = _areaWidgetsMapping.value.toMap()
            _areaConfigsMapping.value = _areaConfigsMapping.value.toMap()
            _areas.value = _areas.value.toList()
            // Don't bump uiRevision on toggle — prevents scroll reset and animation jank
        }
    }

    fun reportDeviceTelemetry(context: Context) {
        val currentClient = client ?: return
        val activeUrl = _currentUrl.value
        if (activeUrl.isBlank()) return
        viewModelScope.launch {
            val deviceName = prefs.mobileDeviceName.first()
            val registrationUrl = prefs.serverUrl.first()?.takeIf { it.isNotBlank() } ?: activeUrl
            runCatching {
                DeviceTelemetryReporter(context.applicationContext, prefs)
                    .report(currentClient, registrationUrl, activeUrl, deviceName) { msg -> addLog(msg) }
            }.onFailure { addLog("Device telemetry failed: ${it.message}") }
        }
    }

    /** Arms presence/telemetry the way the official app does: an immediate report now, zone geofences
     *  for instant arrival/departure, and a periodic WorkManager job for the background battery/location
     *  refresh — no persistent foreground service. The service is started only while the user opts into
     *  High Accuracy continuous tracking. */
    fun startLocationReporting(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        setBatteryMonitoring(true)
        // Periodic Doze-friendly battery/location + geofence refresh, plus an immediate one-shot run.
        // The worker reads serverUrl/token straight from prefs and also (re)registers the mobile_app
        // integration + syncs geofences, so the device registers right after a fresh login regardless
        // of whether the websocket client is ready yet. Using this single path (instead of also calling
        // reportDeviceTelemetry here) avoids two concurrent first-time registrations becoming duplicates.
        LocationWork.schedule(ctx)
        LocationWork.syncNow(ctx)
        observeHighAccuracyService(ctx)
    }

    /** Starts/stops the foreground service in lock-step with the High Accuracy setting: it exists only
     *  for continuous GPS, so normal mode keeps the process free to be Dozed by the OS. */
    private fun observeHighAccuracyService(context: Context) {
        if (highAccuracyJob?.isActive == true) return
        highAccuracyJob = viewModelScope.launch {
            prefs.highAccuracyLocation.collect { highAccuracy ->
                if (highAccuracy) LocationForegroundService.start(context)
                else LocationForegroundService.stop(context)
            }
        }
    }

    /** While the app is in the foreground, mirror the official app's live updates: watch
     *  ACTION_BATTERY_CHANGED and push telemetry whenever the battery level or charging state actually
     *  changes. Gated on real changes so we don't spam on every noisy micro-broadcast (temp/voltage).
     *  In the background, charging is handled by the manifest [PowerBroadcastReceiver] and level by the
     *  periodic worker, so this runs only while visible. */
    private fun setBatteryMonitoring(enabled: Boolean) {
        val ctx = appContext ?: return
        if (enabled) {
            if (batteryReceiver != null) return
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: android.content.Intent?) {
                    intent ?: return
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
                    val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    if (pct != lastReportedBatteryPct || charging != lastReportedCharging) {
                        lastReportedBatteryPct = pct
                        lastReportedCharging = charging
                        appContext?.let { reportDeviceTelemetry(it) }
                    }
                }
            }
            runCatching {
                androidx.core.content.ContextCompat.registerReceiver(
                    ctx, receiver,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            batteryReceiver = receiver
        } else {
            batteryReceiver?.let { runCatching { ctx.unregisterReceiver(it) } }
            batteryReceiver = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        setBatteryMonitoring(false)
    }

    fun saveMobileDeviceName(context: Context, name: String?) {
        viewModelScope.launch {
            prefs.saveMobileDeviceName(name)
            reportDeviceTelemetry(context)
        }
    }

    fun addStackToArea(areaId: String, title: String?, icon: String? = null) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(
            HKIButtonStack(
                id = UUID.randomUUID().toString(),
                title = title,
                icon = icon,
                columns = 3,
                isSquare = true
            )
        )
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addVacuumStackToArea(areaId: String, title: String? = "Vacuum", icon: String? = "CleaningServices") {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(
            HKIButtonStack(
                id = UUID.randomUUID().toString(),
                title = title,
                icon = icon,
                entityIds = emptyList(),
                columns = 2,
                isSquare = true,
                stackType = "vacuum"
            )
        )
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addCameraStackToArea(areaId: String, title: String? = "Cameras", icon: String? = "CameraAlt") {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(
            HKIButtonStack(
                id = UUID.randomUUID().toString(),
                title = title,
                icon = icon,
                entityIds = emptyList(),
                columns = 2,
                isSquare = false,
                stackType = "camera"
            )
        )
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun performButtonAction(areaId: String, stackId: String, entityId: String, trigger: String): String {
        val stack = _areaWidgetsMapping.value[areaId]?.filterIsInstance<HKIButtonStack>()?.firstOrNull { it.id == stackId }
        val config = stack?.buttonConfigs?.get(entityId)
        val domain = entityId.substringBefore(".")
        val defaultAction = when {
            trigger != "tap" -> "more_info"
            stack?.stackType == "camera" -> "more_info"
            domain in listOf("sensor", "binary_sensor", "camera", "climate", "cover", "lock", "fan", "humidifier", "alarm_control_panel", "person") -> "more_info"
            else -> "toggle"
        }
        val action = when (trigger) {
            "double" -> config?.doubleTapAction
            "hold" -> config?.holdAction
            else -> config?.tapAction
        } ?: defaultAction
        when (action) {
            "toggle" -> toggleEntity(entityId)
        }
        return action
    }

    fun addManualFloor(name: String): HAFloor? {
        if (_dashboardMode.value == "auto") return null
        takeSnapshot()
        val floor = HAFloor(floor_id = "manual_floor_${UUID.randomUUID()}", name = name)
        val updated = _floors.value + floor
        _floors.value = updated
        viewModelScope.launch { prefs.saveFloors(updated) }
        return floor
    }

    fun updateFloor(floor: HAFloor) {
        if (_dashboardMode.value == "auto") return
        takeSnapshot()
        val updated = _floors.value.map { if (it.floor_id == floor.floor_id) floor else it }
        _floors.value = updated
        viewModelScope.launch { prefs.saveFloors(updated) }
    }

    fun deleteFloor(floorId: String) {
        if (_dashboardMode.value == "auto") return
        takeSnapshot()
        val updatedFloors = _floors.value.filterNot { it.floor_id == floorId }
        val updatedConfigs = _areaConfigsMapping.value.mapValues { (_, config) ->
            if (config.floorId == floorId) config.copy(floorId = null) else config
        }
        _floors.value = updatedFloors
        _areaConfigsMapping.value = updatedConfigs
        viewModelScope.launch {
            prefs.saveFloors(updatedFloors)
            prefs.saveAreaConfigs(updatedConfigs)
        }
    }

    fun addManualArea(name: String, floorId: String? = null) {
        if (_dashboardMode.value == "auto") return
        takeSnapshot()
        val area = HAArea(area_id = "manual_area_${UUID.randomUUID()}", name = name, floor_id = floorId)
        val updated = _areas.value + area
        _areas.value = updated
        viewModelScope.launch {
            prefs.saveAreas(updated)
            prefs.saveAreaOrder(updated.map { it.area_id })
        }
    }

    fun takeOverCurrentDashboard() {
        viewModelScope.launch {
            prefs.saveDashboardMode("takeover")
            prefs.saveAreas(_areas.value)
            prefs.saveFloors(_floors.value)
            _dashboardMode.value = "takeover"
        }
    }

    fun startNewDashboard(auto: Boolean) {
        viewModelScope.launch {
            prefs.clearDashboardConfig(keepMode = true)
            prefs.saveDashboardMode(if (auto) "auto" else "manual")
            _dashboardMode.value = if (auto) "auto" else "manual"
            _areaWidgetsMapping.value = emptyMap()
            _areaConfigsMapping.value = emptyMap()
            if (!auto) {
                _areas.value = emptyList()
                _floors.value = emptyList()
                prefs.saveAreas(emptyList())
                prefs.saveFloors(emptyList())
            } else {
                refreshEntities(isSilent = true)
            }
        }
    }

    fun addSubtitleToArea(areaId: String, text: String, icon: String? = null) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(HKISubtitleWidget(id = UUID.randomUUID().toString(), text = text, icon = icon))
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addWeatherToArea(areaId: String, entityId: String?, style: String, title: String? = null) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(HKIWeatherWidget(id = UUID.randomUUID().toString(), entityId = entityId, style = style, title = title))
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun updateWidget(areaId: String, updatedWidget: HKIRoomWidget) {
        takeSnapshot()
        bumpWidgetUi()
        ignoreWidgetPrefsUntil = SystemClock.elapsedRealtime() + 2500
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == updatedWidget.id }
        if (index != -1) {
            currentList[index] = updatedWidget
            currentMapping[areaId] = currentList
            _areaWidgetsMapping.value = currentMapping
            viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
        }
    }

    fun deleteWidget(areaId: String, widgetId: String) {
        takeSnapshot()
        bumpWidgetUi()
        ignoreWidgetPrefsUntil = SystemClock.elapsedRealtime() + 2500
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: return
        currentList.removeAll { it.id == widgetId }
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun moveWidgetInArea(areaId: String, from: Int, to: Int) {
        takeSnapshot()
        bumpWidgetUi()
        ignoreWidgetPrefsUntil = SystemClock.elapsedRealtime() + 2500
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: return
        if (from in currentList.indices && to in currentList.indices) {
            currentList.add(to, currentList.removeAt(from))
            currentMapping[areaId] = currentList
            _areaWidgetsMapping.value = currentMapping
            viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
        }
    }

    fun updateAreaConfig(areaId: String, config: HKIAreaConfig) {
        takeSnapshot()
        _uiRevision.value += 1
        ignoreConfigPrefsUntil = SystemClock.elapsedRealtime() + 2500
        val currentMapping = _areaConfigsMapping.value.toMutableMap()
        currentMapping[areaId] = config
        _areaConfigsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaConfigs(currentMapping) }
    }

    fun updatePageConfig(pageKey: String, config: HKIPageConfig) {
        val updated = _pageConfigsMapping.value.toMutableMap()
        updated[pageKey] = config
        _pageConfigsMapping.value = updated
        viewModelScope.launch { prefs.savePageConfigs(updated) }
    }

    private fun bumpWidgetUi() {
        _uiRevision.value += 1
        ignoreWidgetPrefsUntil = SystemClock.elapsedRealtime() + 5000
    }

    private fun autoPopulateDashboard(
        areas: List<HAArea>,
        floors: List<HAFloor>,
        entities: List<HAEntity>,
        registry: List<HAEntityRegistryEntry>,
        devices: List<HADeviceRegistryEntry>
    ) {
        val existingWidgets = _areaWidgetsMapping.value.toMutableMap()
        val existingConfigs = _areaConfigsMapping.value.toMutableMap()
        var changedWidgets = false
        var changedConfigs = false
        val areaByDevice = devices.associate { it.id to it.area_id }
        val registryAreaByEntity = registry.associate { entry ->
            entry.entity_id to (entry.area_id ?: entry.device_id?.let { areaByDevice[it] })
        }
        val areaByEntity = entities.associate { entity ->
            entity.entity_id to (
                registryAreaByEntity[entity.entity_id]
                    ?: entity.attributes?.get("area_id")?.jsonPrimitive?.contentOrNull
            )
        }

        areas.forEach { area ->
            val areaEntities = entities.filter { entity -> areaByEntity[entity.entity_id] == area.area_id }
            val lockIds = areaEntities.filter { it.entity_id.startsWith("lock.") }.map { it.entity_id }
            val climateIds = areaEntities.filter { it.entity_id.startsWith("climate.") }.map { it.entity_id }
            val cameraIds = areaEntities.filter { it.entity_id.startsWith("camera.") }.map { it.entity_id }
            val blindIds = areaEntities.filter { it.entity_id.startsWith("cover.") }.map { it.entity_id }

            val current = existingConfigs[area.area_id] ?: HKIAreaConfig()
            val importedConfig = current.copy(
                floorId = area.floor_id,
                icon = current.icon ?: area.icon,
                wallpaper = current.wallpaper ?: area.picture,
                lockEntityId = lockIds.firstOrNull(),
                climateEntityId = climateIds.firstOrNull(),
                cameraEntityId = cameraIds.firstOrNull(),
                blindEntityId = blindIds.firstOrNull(),
                lockEntityIds = lockIds,
                climateEntityIds = climateIds,
                cameraEntityIds = cameraIds,
                blindEntityIds = blindIds,
                lockIcon = current.lockIcon ?: "Door",
                climateIcon = current.climateIcon ?: "Thermometer",
                cameraIcon = current.cameraIcon ?: "CameraAlt",
                blindIcon = current.blindIcon ?: "Blinds"
            )
            if (existingConfigs[area.area_id] != importedConfig) {
                existingConfigs[area.area_id] = importedConfig
                changedConfigs = true
            }

            val roleEntityIds = (lockIds + climateIds + cameraIds + blindIds).toSet()
            val autoImportExcludedDomains = setOf("automation", "time", "event", "device_tracker", "number", "update")
            val stacks = areaEntities
                .filterNot { it.entity_id in roleEntityIds }
                .filterNot { it.entity_id.substringBefore(".") in autoImportExcludedDomains }
                .groupBy { autoStackTitle(it.entity_id.substringBefore(".")) }
                .filterValues { it.isNotEmpty() }
                .map { (title, grouped) ->
                    val existingStack = existingWidgets[area.area_id]
                        ?.filterIsInstance<HKIButtonStack>()
                        ?.firstOrNull { it.title == title }
                    HKIButtonStack(
                        id = existingStack?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        icon = existingStack?.icon ?: autoStackIcon(title),
                        entityIds = existingStack?.entityIds
                            ?.filter { id -> grouped.any { it.entity_id == id } }
                            ?: grouped.map { it.entity_id },
                        columns = existingStack?.columns ?: 3,
                        showBadge = existingStack?.showBadge ?: true,
                        isSquare = existingStack?.isSquare ?: true,
                        cornerRadius = existingStack?.cornerRadius ?: 28,
                        isHidden = existingStack?.isHidden ?: false,
                        defaultCollapsed = existingStack?.defaultCollapsed ?: false,
                        isCollapsed = existingStack?.isCollapsed
                    )
                }
            if (existingWidgets[area.area_id] != stacks) {
                existingWidgets[area.area_id] = stacks
                changedWidgets = true
            }
        }

        if (_areas.value.map { it.area_id }.toSet() == areas.map { it.area_id }.toSet()) {
            sortAreas(_areas.value.map { it.area_id })
        }

        if (changedWidgets) {
            _areaWidgetsMapping.value = existingWidgets
            viewModelScope.launch { prefs.saveAreaWidgets(existingWidgets) }
        }
        if (changedConfigs) {
            _areaConfigsMapping.value = existingConfigs
            viewModelScope.launch { prefs.saveAreaConfigs(existingConfigs) }
        }
        viewModelScope.launch {
            prefs.saveAreas(areas)
            prefs.saveFloors(floors)
            prefs.saveAreaOrder(areas.map { it.area_id })
        }
    }

    private fun autoStackTitle(domain: String): String {
        return when (domain) {
            "light" -> "Lights"
            "switch" -> "Switches"
            "climate" -> "Climate"
            "cover" -> "Covers"
            "lock" -> "Locks"
            "camera" -> "Cameras"
            "sensor" -> "Sensors"
            "binary_sensor" -> "Binary Sensors"
            else -> domain.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    private fun autoStackIcon(title: String): String {
        return when (title) {
            "Lights" -> "Lightbulb"
            "Switches" -> "Power"
            "Climate" -> "Thermometer"
            "Covers" -> "Window"
            "Locks" -> "Lock"
            "Cameras" -> "CameraAlt"
            "Sensors" -> "Air"
            "Binary Sensors" -> "Security"
            else -> "Lightbulb"
        }
    }

    fun moveArea(fromIndex: Int, toIndex: Int) {
        takeSnapshot()
        val currentList = _areas.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            currentList.add(toIndex, currentList.removeAt(fromIndex))
            _areas.value = currentList
            viewModelScope.launch { prefs.saveAreaOrder(currentList.map { it.area_id }) }
        }
    }

    fun toggleFloorCollapsed(floorId: String) {
        _collapsedFloorIds.value = if (floorId in _collapsedFloorIds.value) {
            _collapsedFloorIds.value - floorId
        } else {
            _collapsedFloorIds.value + floorId
        }
    }

    fun deleteArea(areaId: String) {
        if (_dashboardMode.value == "auto") return
        takeSnapshot()
        val currentList = _areas.value.toMutableList()
        currentList.removeAll { it.area_id == areaId }
        _areas.value = currentList
        val currentMapping = _areaWidgetsMapping.value.toMutableMap().apply { remove(areaId) }
        _areaWidgetsMapping.value = currentMapping
        val configMapping = _areaConfigsMapping.value.toMutableMap().apply { remove(areaId) }
        _areaConfigsMapping.value = configMapping
        viewModelScope.launch {
            prefs.saveAreaOrder(currentList.map { it.area_id })
            prefs.saveAreas(currentList)
            prefs.saveAreaWidgets(currentMapping)
            prefs.saveAreaConfigs(configMapping)
        }
    }

    val greeting: String
        get() = when (LocalTime.now().hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
}

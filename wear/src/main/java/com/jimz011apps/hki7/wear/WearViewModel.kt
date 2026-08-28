package com.jimz011apps.hki7.wear

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.wear.data.HomeAssistantRest
import com.jimz011apps.hki7.wear.data.HomeAssistantSocket
import com.jimz011apps.hki7.wear.data.PhoneSyncService
import com.jimz011apps.hki7.wear.data.WearActions
import com.jimz011apps.hki7.wear.data.WearAuth
import com.jimz011apps.hki7.wear.data.WearPreferences
import com.jimz011apps.hki7.wear.data.WearRemoteAuth
import com.jimz011apps.hki7.wear.data.WearSensorWorker
import com.jimz011apps.hki7.wear.data.WearSensors
import com.jimz011apps.hki7.wear.data.WearSession
import com.jimz011apps.hki7.wear.tiles.TileRefresh
import com.jimz011apps.hki7.data.WearRoom
import com.jimz011apps.hki7.wear.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the watch is currently able to show. */
sealed interface WearUiState {
    data object Loading : WearUiState

    /** No session yet — either route can produce one. */
    data object NotConfigured : WearUiState

    /** Signing in through the phone's browser; the watch shows "continue on phone". */
    data object SigningIn : WearUiState

    data class Ready(
        val quickActions: List<HKIQuickAction>,
        val rooms: List<WearRoom>,
        /** Live state per entity id, for whatever the current screen has asked to be loaded. */
        val entities: Map<String, HAEntity>,
        val refreshing: Boolean = false,
        /** Set when the last Home Assistant call failed, cleared on the next success. */
        val error: String? = null,
    ) : WearUiState
}

class WearViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = WearPreferences(application)
    private val session = WearSession(prefs)

    /** Surfaced by the setup screen so a failed sign-in says why. */
    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()

    /** Whether the watch reports its own battery to Home Assistant. */
    private val _sensorsEnabled = MutableStateFlow(false)
    val sensorsEnabled: StateFlow<Boolean> = _sensorsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            _sensorsEnabled.value = prefs.sensorsEnabledOnce()
            // Re-arm on launch: WorkManager keeps periodic work across reboots, but a watch that
            // was reset or an app that was reinstalled has nothing scheduled.
            if (_sensorsEnabled.value) WearSensorWorker.schedule(getApplication())
        }
    }

    /**
     * Turns watch battery reporting on or off.
     *
     * Reports immediately on enable rather than waiting up to fifteen minutes for the first run,
     * so the entity appears in Home Assistant while the user is still looking for it.
     */
    fun setSensorsEnabled(enabled: Boolean) {
        _sensorsEnabled.value = enabled
        viewModelScope.launch {
            prefs.setSensorsEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                WearSensorWorker.schedule(context)
                WearSensors.report(context, prefs, session)
            } else {
                WearSensorWorker.cancel(context)
            }
        }
    }

    private val _state = MutableStateFlow<WearUiState>(WearUiState.Loading)
    val state: StateFlow<WearUiState> = _state.asStateFlow()

    /** Rebuilt whenever credentials change; null until the watch has a session. */
    private var client: HomeAssistantRest? = null

    /** Opened only while a screen is on, by [runLiveUpdates]. */
    private var socket: HomeAssistantSocket? = null

    /** Entity ids whose live state the current screen needs. */
    private var watching: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            if (!session.isAuthenticated()) {
                _state.value = WearUiState.NotConfigured
                // A watch paired after the phone last wrote its config has nothing waiting for it,
                // so ask before concluding there is no phone to be set up from.
                PhoneSyncService.requestConfig(getApplication())
                // Give the phone a moment to answer; if it does, carry straight on.
                delay(1500)
                if (session.isAuthenticated()) reload()
                return@launch
            }
            reload()
        }
    }

    /**
     * Streams live entity updates for as long as this is collected.
     *
     * Called from the UI inside `repeatOnLifecycle(STARTED)`, so the socket exists exactly while a
     * screen is visible and is torn down the moment the wrist drops. Returns immediately when
     * there is no session yet — the setup screen has nothing to keep live.
     */
    suspend fun runLiveUpdates() {
        val serverUrl = prefs.serverUrlOnce() ?: return
        if (!session.isAuthenticated()) return
        val live = socket ?: HomeAssistantSocket(serverUrl, session).also { socket = it }
        try {
            live.stateChanges().collect { change ->
                // Only what is on screen: a busy house reports constantly, and redrawing for an
                // entity the user cannot see is pure battery.
                if (change.entityId !in watching) return@collect
                val current = _state.value as? WearUiState.Ready ?: return@collect
                val newState = change.newState
                val merged = if (newState == null) {
                    current.entities - change.entityId
                } else {
                    current.entities + (change.entityId to newState)
                }
                _state.value = current.copy(entities = merged, error = null)
            }
        } catch (_: Exception) {
            // A dropped socket is not worth an error banner: the REST snapshot on screen is still
            // what Home Assistant last said, and the flow reconnects on its own.
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket?.dispose()
        socket = null
    }

    /** Loads live state for [entityIds] and keeps them refreshed on later [refresh] calls. */
    fun watch(entityIds: Collection<String>) {
        watching = entityIds.filter { it.isNotBlank() }.toSet()
        refresh()
    }

    fun refresh() {
        val current = _state.value as? WearUiState.Ready ?: return
        val active = client ?: return
        _state.value = current.copy(refreshing = true)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val jobs: List<Deferred<HAEntity?>> =
                        watching.map { id -> async { active.state(id) } }
                    jobs.awaitAll().filterNotNull()
                }
            }.fold(
                onSuccess = { loaded ->
                    val merged = (_state.value as? WearUiState.Ready)?.entities.orEmpty() +
                        loaded.associateBy { it.entity_id }
                    _state.value = (_state.value as? WearUiState.Ready)
                        ?.copy(entities = merged, refreshing = false, error = null)
                        ?: WearUiState.Loading
                },
                onFailure = { error ->
                    // A refresh token Home Assistant has revoked cannot be recovered from by
                    // retrying, so send the user back through setup rather than showing
                    // "can't reach Home Assistant" forever.
                    if ((error as? WearAuth.TokenException)?.invalidGrant == true) {
                        prefs.clearSession()
                        client = null
                        _state.value = WearUiState.NotConfigured
                    } else {
                        _state.value = (_state.value as? WearUiState.Ready)
                            ?.copy(refreshing = false, error = error.message)
                            ?: WearUiState.Loading
                    }
                },
            )
        }
    }

    /** Runs a quick action, then re-reads the entities on screen so the state catches up. */
    fun run(quickAction: HKIQuickAction, onResult: (WearActions.Result) -> Unit = {}) {
        viewModelScope.launch {
            val result = WearActions.run(client, quickAction)
            onResult(result)
            if (result is WearActions.Result.Success) refreshAfterCommand()
        }
    }

    fun toggle(entityId: String, onResult: (WearActions.Result) -> Unit = {}) {
        viewModelScope.launch {
            val result = WearActions.toggle(client, entityId)
            onResult(result)
            if (result is WearActions.Result.Success) refreshAfterCommand()
        }
    }

    /** Home Assistant applies a call asynchronously; reading back immediately usually returns the
     *  pre-call state and makes the tap look like it did nothing. */
    private suspend fun refreshAfterCommand() {
        delay(700)
        refresh()
        // The tiles show the same entities; leaving them stale would have the wrist disagree
        // with itself depending on which surface the user looked at.
        TileRefresh.requestAll(getApplication())
    }

    /** Asks the phone to re-send everything, then reloads from preferences. */
    fun resync(onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val asked = PhoneSyncService.requestConfig(getApplication())
            // Give the phone a moment to answer before re-reading what it wrote.
            delay(1500)
            reload()
            onDone(asked)
        }
    }

    private suspend fun reload() {
        val serverUrl = prefs.serverUrlOnce()
        if (serverUrl == null || !session.isAuthenticated()) {
            _state.value = WearUiState.NotConfigured
            return
        }
        client = HomeAssistantRest(serverUrl, session)
        // Credentials or server may have changed; the old socket was built against the old ones.
        socket?.dispose()
        socket = null
        val quickActions = prefs.quickActionsOnce().filter { it.showOnWatch }
        val rooms = prefs.rooms.first()
        _state.value = WearUiState.Ready(quickActions, rooms, emptyMap())
        watch(quickActions.map { it.entityId })
    }

    /**
     * Signs the watch in by opening Home Assistant's login on the paired phone's browser.
     *
     * The fallback for every case the Data Layer handover cannot cover: no HKI 7 on the phone, a
     * factory-reset watch, or a handover that never arrived.
     */
    fun signInManually(serverUrlInput: String) {
        val serverUrl = WearRemoteAuth.normaliseUrl(serverUrlInput)
        if (serverUrl == null) {
            _setupError.value = getApplication<Application>().getString(R.string.wear_setup_bad_url)
            return
        }
        _setupError.value = null
        _state.value = WearUiState.SigningIn
        viewModelScope.launch {
            val context = getApplication<Application>()
            when (val result = WearRemoteAuth.authenticate(context, serverUrl, prefs)) {
                is WearRemoteAuth.Result.Success -> reload()
                WearRemoteAuth.Result.PhoneUnavailable -> fail(R.string.wear_setup_phone_unavailable)
                WearRemoteAuth.Result.Unsupported -> fail(R.string.wear_setup_unsupported)
                is WearRemoteAuth.Result.Failed -> fail(R.string.wear_setup_failed, result.message)
            }
        }
    }

    private fun fail(messageRes: Int, detail: String? = null) {
        _setupError.value = getApplication<Application>().getString(messageRes)
        _state.value = WearUiState.NotConfigured
        if (detail != null) android.util.Log.w("WearViewModel", "Sign-in failed: $detail")
    }

    /** Loads every entity in a room. Rooms carry ids only, so their states are fetched on entry. */
    fun openRoom(room: WearRoom) = watch(room.entityIds)
}

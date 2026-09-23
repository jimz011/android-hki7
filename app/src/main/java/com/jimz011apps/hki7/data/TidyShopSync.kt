package com.jimz011apps.hki7.data

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** How the device currently stands with its family connector. */
enum class TidyShopStatus {
    /** No server address, or no enrolment on it yet. */
    NOT_CONNECTED,

    /** Enrolled, but nothing fetched yet this session. */
    CONNECTING,

    /** Lists are loaded; the event stream may or may not be held (it only runs in the foreground). */
    READY,

    /** Enrolled, but the last exchange failed. Cached lists are still shown. */
    ERROR,

    /** The connector says this device was removed from the family. */
    REVOKED,
}

data class TidyShopState(
    val status: TidyShopStatus = TidyShopStatus.NOT_CONNECTED,
    val lists: List<TidyShopList> = emptyList(),
    val userId: String = "",
    val householdName: String = "",
    val memberName: String = "",
    val serverUrl: String = "",
    /** True while the SSE stream is actually open, which is what "live" means in the UI. */
    val live: Boolean = false,
    val error: String? = null,
) {
    val isConnected: Boolean get() = status != TidyShopStatus.NOT_CONNECTED
}

/**
 * The device side of TidyShop family sync.
 *
 * A process-wide singleton rather than view-model state, because the enrolment belongs to the
 * phone and the same lists back every synced widget on every dashboard. It follows the same
 * battery rules as the rest of this app: the event stream is held **only while the app is in the
 * foreground** — [setAppVisible] opens and drops it — so a backgrounded phone holds no socket, no
 * wakelock and schedules no work. Whatever was missed while away arrives in the snapshot frame the
 * connector sends on connect.
 *
 * Local edits are applied optimistically and pushed with a per-list debounce, so holding a
 * checkbox does not become a burst of PUTs.
 */
object TidyShopSync {
    /** Matches TidyShop's own client: long enough to coalesce a run of taps, short enough to feel live. */
    private const val PUSH_DEBOUNCE_MS = 400L
    private const val RECONNECT_MIN_MS = 1_000L
    private const val RECONNECT_MAX_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob())
    private val pushLock = Mutex()
    private val stateLock = Any()
    private val pendingChanges = TidyShopPendingChanges()

    private val _state = MutableStateFlow(TidyShopState())
    val state: StateFlow<TidyShopState> = _state.asStateFlow()

    private var prefs: PreferencesManager? = null
    private var started = false
    private var appVisible = false

    @Volatile private var serverUrl: String = ""
    @Volatile private var deviceId: String = ""
    @Volatile private var userId: String = ""

    private var streamJob: Job? = null
    private val pushSchedulingLock = Any()
    private val pushJobs = ConcurrentHashMap<String, Job>()
    private val inFlightPushes = ConcurrentHashMap<String, Job>()

    private val connector = TidyShopConnector(
        baseUrl = { serverUrl },
        // The connector echoes this back on every frame, which is how we ignore our own edits.
        clientId = { deviceId.ifBlank { "hki7" } },
        accessToken = {
            val device = deviceId
            val user = userId
            if (device.isBlank() || user.isBlank()) null
            else TidyShopIdentity.signAssertion(device, user)
        },
    )

    /** Wired up once, from the view model. Safe to call again; only the first call takes effect. */
    fun initialize(prefs: PreferencesManager) {
        if (started) return
        started = true
        this.prefs = prefs
        scope.launch {
            combine(
                prefs.tidyShopUrl,
                prefs.tidyShopDeviceId,
                prefs.tidyShopUserId,
                prefs.tidyShopHouseholdName,
                prefs.tidyShopMemberName,
            ) { url, device, user, household, member ->
                listOf(url, device, user, household, member)
            }.collect { (url, device, user, household, member) ->
                val wasEnrolled = deviceId.isNotBlank() && userId.isNotBlank()
                serverUrl = url
                deviceId = device
                userId = user
                val enrolled = url.isNotBlank() && device.isNotBlank() && user.isNotBlank()
                _state.update { current ->
                    current.copy(
                        status = when {
                            !enrolled -> TidyShopStatus.NOT_CONNECTED
                            current.status == TidyShopStatus.NOT_CONNECTED -> TidyShopStatus.CONNECTING
                            else -> current.status
                        },
                        lists = if (enrolled) current.lists else emptyList(),
                        userId = user,
                        householdName = household,
                        memberName = member,
                        serverUrl = url,
                        live = if (enrolled) current.live else false,
                    )
                }
                if (enrolled && !wasEnrolled) restart() else if (!enrolled) stopStream()
            }
        }
    }

    /** Called from the view model's foreground transitions. */
    fun setAppVisible(visible: Boolean) {
        appVisible = visible
        if (visible) restart() else stopStream()
    }

    private fun enrolled(): Boolean =
        serverUrl.isNotBlank() && deviceId.isNotBlank() && userId.isNotBlank()

    private fun restart() {
        if (!appVisible || !enrolled()) return
        if (streamJob?.isActive == true) return
        streamJob = scope.launch {
            // One fetch up front so the dashboard fills in even if the stream cannot be held (a
            // proxy that buffers SSE, say). After that the snapshot frame keeps it current.
            refreshNow()
            var backoff = RECONNECT_MIN_MS
            while (isActive && appVisible && enrolled()) {
                var opened = false
                val failure = connector.streamEvents(
                    onOpen = {
                        opened = true
                        _state.update { it.copy(live = true, error = null) }
                    },
                    onEvent = ::onFrame,
                )
                _state.update { it.copy(live = false) }
                if (!isActive || !appVisible) break
                if (failure is TidyShopRevokedException) {
                    _state.update {
                        it.copy(status = TidyShopStatus.REVOKED, error = failure.message)
                    }
                    break
                }
                // Reset the backoff after any connection that actually opened, so a long-lived
                // stream that finally drops reconnects promptly instead of at the old ceiling.
                backoff = if (opened) RECONNECT_MIN_MS else (backoff * 2).coerceAtMost(RECONNECT_MAX_MS)
                delay(backoff)
            }
        }
    }

    private fun stopStream() {
        connector.closeEventStream()
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(live = false) }
    }

    private fun onFrame(event: String, data: org.json.JSONObject) {
        // Our own edit coming back around. The optimistic copy already on screen is newer than
        // this frame if the user has kept typing, so applying it would fight the keyboard.
        if (data.optString("origin") == deviceId) return
        when (event) {
            "sync" -> {
                val merge = synchronized(stateLock) {
                    val result = pendingChanges.mergeSnapshot(data.tidyShopListsFromSnapshot())
                    _state.value = _state.value.copy(
                        lists = result.lists.sortedBy { it.title.lowercase() },
                        status = TidyShopStatus.READY,
                        error = null,
                    )
                    result
                }
                merge.deletedPendingListIds.forEach(::cancelPush)
            }
            "list.updated" -> {
                val list = data.tidyShopListFromFrame() ?: return
                synchronized(stateLock) {
                    val merged = pendingChanges.mergeRemote(list)
                    val current = _state.value
                    _state.value = current.copy(
                        lists = (current.lists.filterNot { it.id == list.id } + merged)
                            .sortedBy { it.title.lowercase() },
                        status = TidyShopStatus.READY,
                        error = null,
                    )
                }
            }
            "list.deleted" -> {
                val listId = data.optString("listId").ifBlank { return }
                synchronized(stateLock) {
                    pendingChanges.delete(listId)
                    _state.value = _state.value.copy(
                        lists = _state.value.lists.filterNot { it.id == listId },
                    )
                }
                cancelPush(listId)
            }
        }
    }

    /** Pulls the full set of lists. Used on open, and as the fallback when the stream is down. */
    fun refresh() {
        if (!enrolled()) return
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        connector.fetchLists()
            .onSuccess { lists ->
                val merge = synchronized(stateLock) {
                    val result = pendingChanges.mergeSnapshot(lists)
                    _state.value = _state.value.copy(
                        lists = result.lists.sortedBy { it.title.lowercase() },
                        status = TidyShopStatus.READY,
                        error = null,
                    )
                    result
                }
                merge.deletedPendingListIds.forEach(::cancelPush)
            }
            .onFailure { failure -> reportFailure(failure) }
    }

    private fun reportFailure(failure: Throwable) {
        _state.update {
            it.copy(
                status = if (failure is TidyShopRevokedException) TidyShopStatus.REVOKED else TidyShopStatus.ERROR,
                error = failure.message,
            )
        }
    }

    // ── Editing ─────────────────────────────────────────────────────────

    /** Records an operation, applies it on screen straight away, then schedules the push. */
    private fun edit(listId: String, change: (TidyShopList) -> TidyShopChange?) {
        var changed = false
        synchronized(stateLock) {
            val current = _state.value
            val existing = current.lists.find { it.id == listId } ?: return@synchronized
            val operation = change(existing) ?: return@synchronized
            val updated = pendingChanges.add(existing, operation)
            _state.value = current.copy(
                lists = current.lists.map { if (it.id == listId) updated else it },
            )
            changed = true
        }
        if (changed) schedulePush(listId)
    }

    private fun schedulePush(listId: String) {
        synchronized(pushSchedulingLock) {
            // An edit during a request stays in the reconciliation queue. The running worker will
            // see it when the request completes and schedule the next debounced pass.
            if (inFlightPushes.containsKey(listId)) return
            pushJobs.remove(listId)?.cancel()
            lateinit var job: Job
            job = scope.launch(start = CoroutineStart.LAZY) {
                var enteredInFlight = false
                try {
                    delay(PUSH_DEBOUNCE_MS)
                    synchronized(pushSchedulingLock) {
                        pushJobs.remove(listId, job)
                        inFlightPushes[listId] = job
                        enteredInFlight = true
                    }
                    // Serialised so two lists changed at once cannot interleave their PUTs.
                    pushLock.withLock {
                        val attempt = synchronized(stateLock) { pendingChanges.beginPush(listId) }
                            ?: return@withLock
                        connector.updateList(attempt.list)
                            .onSuccess { saved ->
                                synchronized(stateLock) {
                                    val result = pendingChanges.completePush(listId, attempt, saved)
                                    if (result != null) {
                                        val current = _state.value
                                        _state.value = current.copy(
                                            lists = current.lists.map {
                                                if (it.id == listId) result.list else it
                                            },
                                            status = TidyShopStatus.READY,
                                            error = null,
                                        )
                                    }
                                }
                            }
                            .onFailure { failure ->
                                // Disconnect and remote deletion deliberately cancel this worker.
                                if (!currentCoroutineContext().isActive) return@onFailure
                                synchronized(stateLock) {
                                    val remote = pendingChanges.failPush(listId)
                                    if (remote != null) {
                                        val current = _state.value
                                        _state.value = current.copy(
                                            lists = current.lists.map {
                                                if (it.id == listId) remote else it
                                            },
                                        )
                                    }
                                }
                                reportFailure(failure)
                                // Permissions may have changed, so recover from an authoritative read.
                                refreshNow()
                            }
                    }
                } finally {
                    synchronized(pushSchedulingLock) {
                        pushJobs.remove(listId, job)
                        inFlightPushes.remove(listId, job)
                    }
                    // A worker cancelled only to reset its debounce has already been replaced.
                    // The worker which actually issued a request owns scheduling any rebased pass.
                    if (enteredInFlight &&
                        synchronized(stateLock) { pendingChanges.hasPending(listId) }
                    ) {
                        schedulePush(listId)
                    }
                }
            }
            pushJobs[listId] = job
            job.start()
        }
    }

    private fun cancelPush(listId: String) {
        synchronized(pushSchedulingLock) {
            pushJobs.remove(listId)?.cancel()
            inFlightPushes.remove(listId)?.cancel()
        }
    }

    private fun cancelAllPushes() {
        synchronized(pushSchedulingLock) {
            (pushJobs.values + inFlightPushes.values).forEach { it.cancel() }
            pushJobs.clear()
            inFlightPushes.clear()
        }
    }

    fun toggleItem(listId: String, itemId: String) = edit(listId) { list ->
        val item = list.items.find { it.id == itemId } ?: return@edit null
        val now = System.currentTimeMillis()
        TidyShopChange.SetChecked(itemId, !item.checked, now)
    }

    fun addItem(listId: String, name: String, quantity: Int = 0) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val itemId = UUID.randomUUID().toString()
        edit(listId) {
            TidyShopChange.Add(
                TidyShopItem(
                    id = itemId, name = trimmed, quantity = quantity,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        // Feeds the family's shared suggestion history so an item added here is offered in
        // TidyShop too. Best-effort by design: a refusal must not undo the add above.
        scope.launch {
            connector.recordSuggestion(listId, trimmed, UUID.randomUUID().toString())
        }
    }

    fun renameItem(listId: String, itemId: String, name: String, quantity: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        edit(listId) { list ->
            if (list.items.none { it.id == itemId }) null else TidyShopChange.Rename(
                itemId = itemId,
                name = trimmed,
                quantity = quantity,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    fun removeItem(listId: String, itemId: String) = edit(listId) { list ->
        if (list.items.none { it.id == itemId }) null else TidyShopChange.Remove(setOf(itemId))
    }

    fun clearChecked(listId: String) = edit(listId) { list ->
        list.items.filter { it.checked }.mapTo(mutableSetOf()) { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let(TidyShopChange::Remove)
    }

    // ── Enrolment ───────────────────────────────────────────────────────

    /**
     * Enrols this device against [url] with an invite code, a device-link code, a recovery code or
     * the master pairing key, and stores what comes back.
     *
     * The recovery code in the result is shown once and never stored — it is the only way back in
     * for a member who neither administers the server nor has a second device.
     */
    suspend fun connect(
        url: String,
        code: String,
        pairingKey: String,
        displayName: String,
    ): Result<TidyShopEnrolment> {
        val prefs = this.prefs ?: return Result.failure(IllegalStateException("Not initialised"))
        val normalized = normalizeTidyShopUrl(url)
        if (normalized.isBlank()) {
            return Result.failure(IllegalStateException("A server address is required."))
        }
        // Point the client at the new server before enrolling, since enrolment goes through it.
        serverUrl = normalized
        prefs.saveTidyShopUrl(normalized)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "An Android device" }
        return connector.enrol(code, pairingKey, deviceName, displayName)
            .onSuccess { enrolment ->
                prefs.saveTidyShopEnrolment(
                    url = normalized,
                    deviceId = enrolment.deviceId,
                    userId = enrolment.userId,
                    household = enrolment.household?.name.orEmpty(),
                    member = enrolment.household?.members
                        ?.firstOrNull { it.userId == enrolment.userId }?.displayName
                        ?: displayName,
                )
                deviceId = enrolment.deviceId
                userId = enrolment.userId
                _state.update { it.copy(status = TidyShopStatus.CONNECTING, error = null) }
                restart()
            }
            .onFailure { failure ->
                _state.update {
                    it.copy(status = TidyShopStatus.NOT_CONNECTED, error = failure.message)
                }
            }
    }

    /** Leaves the family on this device: stops the stream, forgets the enrolment, drops the key.
     *
     *  Deleting the Keystore key is what makes this final — the connector still holds the public
     *  half until an owner revokes the device there, but this phone can no longer sign for it. */
    suspend fun disconnect() {
        stopStream()
        cancelAllPushes()
        synchronized(stateLock) { pendingChanges.clear() }
        deviceId = ""
        userId = ""
        TidyShopIdentity.deleteKey()
        prefs?.clearTidyShopEnrolment()
        _state.value = TidyShopState(serverUrl = serverUrl)
    }

    /** Re-reads the household so Settings can show who is in it without waiting for a list change. */
    suspend fun refreshHousehold() {
        val prefs = this.prefs ?: return
        if (!enrolled()) return
        connector.fetchHouseholds()
            .onSuccess { households ->
                val household = households.firstOrNull() ?: return@onSuccess
                val member = household.members.firstOrNull { it.userId == userId }
                prefs.saveTidyShopEnrolment(
                    url = serverUrl,
                    deviceId = deviceId,
                    userId = userId,
                    household = household.name,
                    member = member?.displayName ?: prefs.tidyShopMemberName.first(),
                )
            }
            .onFailure { reportFailure(it) }
    }

    /** Used by tests and by a full app reset, which must not leave a stream running. */
    internal suspend fun shutdownForTest() {
        stopStream()
        streamJob?.cancelAndJoin()
        cancelAllPushes()
        started = false
        prefs = null
        synchronized(stateLock) { pendingChanges.clear() }
        _state.value = TidyShopState()
    }
}

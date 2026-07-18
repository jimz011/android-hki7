@file:Suppress("unused")

package com.example.hki7.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.hki7.data.*
import com.example.hki7.ui.screens.isAutoSecurityEntityFor
import com.example.hki7.ui.screens.isAutoClimateSensorFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import android.os.SystemClock
import java.time.LocalTime
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class ConnectionStatus {
    IDLE, CONNECTING, CONNECTED, ERROR
}

/** A route change often produces one or two failures while Android's new transport settles. Keep
 * those failures in the non-blocking banner state; the actionable error overlay is reserved for a
 * connection that failed three consecutive attempts. */
internal const val CONNECTION_FAILURES_BEFORE_ERROR = 3

internal fun connectionStatusAfterFailures(failedAttempts: Int): ConnectionStatus =
    if (failedAttempts < CONNECTION_FAILURES_BEFORE_ERROR) {
        ConnectionStatus.CONNECTING
    } else {
        ConnectionStatus.ERROR
    }

/** Result of running an [HKIAction]. Side-effecting actions execute in the view model and report
 *  [Handled]; UI-routing actions hand back an outcome the composable resolves. */
sealed interface ActionOutcome {
    /** The action ran to completion in the view model (toggle / service call). */
    data object Handled : ActionOutcome
    /** Nothing to do (type "none" or an incompletely configured action). */
    data object None : ActionOutcome
    data class OpenMoreInfo(val entityId: String) : ActionOutcome
    /** Navigate within the app. Target: "home"|"rooms"|"energy"|"climate"|"security"|"battery"|"room:<areaId>". */
    data class Navigate(val target: String) : ActionOutcome
    data class OpenUrl(val url: String) : ActionOutcome
}

/** How often the expensive dashboard rebuild (areas/floors/registries/auto-populate) runs. */
private const val DASHBOARD_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

/** Safety re-seed of full entity state. Live updates arrive via the websocket subscription (which
 *  also re-seeds on every reconnect and on foreground return), so this full re-download only needs to
 *  run rarely to self-heal any silently-missed events — not every minute. */
private const val STATE_RESEED_INTERVAL_MS = 15 * 60 * 1000L

/** Fast recovery probes after LAN fallback. Failed checks back off and then cap at ten seconds. */
private val INTERNAL_URL_RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L, 5_000L, 10_000L)
private const val INTERNAL_URL_PROBE_TIMEOUT_MS = 4_000L

/** Delays between the two automatic retries that precede the full connection-error overlay. */
private val CONNECTION_RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L)

/** How long a fetched weather forecast is served from cache before re-fetching. */
private const val WEATHER_FORECAST_TTL_MS = 10 * 60 * 1000L

/** Calendar widgets refresh often enough to feel current without refetching on every recomposition. */
private const val CALENDAR_EVENTS_TTL_MS = 2 * 60 * 1000L

/** Home Assistant floors do not own HKI's card layout; new imports always start from HKI defaults. */
internal fun HAFloor.withDefaultRoomLayout() = copy(
    columns = 1,
    isSquare = false,
    compactTiles = true,
    width = "full"
)

/** Whether a fetched registry snapshot is trustworthy enough to become the dashboard.
 *
 * A live server always exposes states, entity-registry entries, and devices — this app's own
 * mobile_app registration during onboarding guarantees at least one of each — and floors exist
 * only to group areas, so floors without any areas means the area list was dropped. Importing
 * such a snapshot yields floors without rooms or rooms without entities; during the onboarding
 * takeover the result would even be frozen permanently. Callers should skip the import (keeping
 * the previous rooms and any pending takeover) and let the next refresh retry instead. */
internal fun isCompleteDashboardRegistrySnapshot(
    entities: List<HAEntity>,
    areas: List<HAArea>,
    floors: List<HAFloor>,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>
): Boolean {
    if (entities.isEmpty() || registry.isEmpty() || devices.isEmpty()) return false
    return floors.isEmpty() || areas.isNotEmpty()
}

/** Accept a supporting Energy entity only when it belongs to the resolved source device. */
internal fun supportingEnergyEntityForDevice(
    candidateEntityId: String?,
    sourceDeviceId: String?,
    registry: List<HAEntityRegistryEntry>
): String? {
    if (candidateEntityId == null) return null
    if (sourceDeviceId == null) return candidateEntityId
    return candidateEntityId.takeIf { candidate ->
        registry.firstOrNull { it.entity_id == candidate }?.device_id == sourceDeviceId
    }
}

/** Domains that participate in automatic room discovery. Sensor and binary-sensor metadata also
 * drives each room's environmental readings and active-state summary. */
private val AUTO_ROOM_IMPORT_DOMAINS = setOf(
    "light",
    "switch",
    "lock",
    "camera",
    "cover",
    "climate",
    "humidifier",
    "fan",
    "binary_sensor",
    "sensor",
    "media_player"
)

private val AUTO_ROOM_STACK_DOMAINS = setOf("light", "switch")

/** Resolve the fossil-fuel percentage entity configured by Home Assistant's Energy Settings.
 *
 * The carbon provider is not included in `energy/get_prefs`: Energy Settings stores it as an
 * Electricity Maps config entry whose internal domain remains `co2signal`. Prefer stable registry
 * metadata tied to an enabled config entry, then retain compatibility with older registry payloads
 * using unique-id, entity-id, registry-unit, and live-state-unit fallbacks. */
internal fun resolveHomeAssistantEnergyCarbonEntity(
    configEntries: List<HAConfigEntry>,
    registry: List<HAEntityRegistryEntry>,
    liveEntities: List<HAEntity>
): String? {
    val hasCarbonConfigMetadata = configEntries.any { it.domain == "co2signal" }
    val enabledEntries = configEntries
        .asSequence()
        .filter { it.domain == "co2signal" && it.disabled_by == null }
        .sortedByDescending { it.state == "loaded" }
        .toList()
    if (hasCarbonConfigMetadata && enabledEntries.isEmpty()) return null
    val enabledEntryIds = enabledEntries.mapTo(linkedSetOf()) { it.entry_id }
    val entryPriority = enabledEntries.mapIndexed { index, entry -> entry.entry_id to index }.toMap()
    val liveById = liveEntities.associateBy { it.entity_id }

    val platformCandidates = registry.asSequence()
        .filter { it.platform == "co2signal" && it.disabled_by == null }
        // If HA returned config entries and registry ownership metadata, never select an entity
        // belonging to a disabled/different Electricity Maps entry.
        .filter { entry ->
            enabledEntryIds.isEmpty() || entry.config_entry_id == null || entry.config_entry_id in enabledEntryIds
        }
        .sortedWith(
            compareBy<HAEntityRegistryEntry> {
                entryPriority[it.config_entry_id] ?: Int.MAX_VALUE
            }.thenBy { it.entity_id }
        )
        .toList()

    fun registryUnit(entry: HAEntityRegistryEntry): String? =
        entry.unit_of_measurement?.trim()
            ?: liveById[entry.entity_id]?.attributes
                ?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull?.trim()

    return platformCandidates.firstOrNull {
        it.translation_key == "fossil_fuel_percentage"
    }?.entity_id
        ?: platformCandidates.firstOrNull {
            it.unique_id?.lowercase()?.contains("fossilfuelpercentage") == true
        }?.entity_id
        ?: platformCandidates.firstOrNull {
            it.entity_id.lowercase().contains("fossil_fuel_percentage")
        }?.entity_id
        ?: platformCandidates.firstOrNull { registryUnit(it) == "%" }?.entity_id
}

/** Enrich an Energy preferences import with useful entities belonging to the same HA devices.
 * `energy/get_prefs` only names the statistics selected by the user; the live power, phase,
 * current, voltage, and supporting source sensors remain discoverable through the registries. */
internal fun importRelatedHomeAssistantEnergyEntities(
    config: HKIEnergyConfig,
    sourceEntityIds: Map<String, List<String>>,
    registry: List<HAEntityRegistryEntry>,
    liveEntities: List<HAEntity>
): HKIEnergyConfig {
    val registryByEntity = registry.associateBy { it.entity_id }

    fun related(category: String): Pair<String, List<HAEntity>>? {
        // The first explicit HA Energy source is the anchor. Guesses must stay on its owning
        // device: including child or secondary devices can select a similarly named but unrelated
        // phase, inverter, or power sensor.
        val deviceId = sourceEntityIds[category].orEmpty()
            .firstNotNullOfOrNull { registryByEntity[it]?.device_id }
            ?: return null
        val relatedEntityIds = registry.asSequence()
            .filter { it.device_id == deviceId && it.disabled_by == null }
            .mapTo(hashSetOf()) { it.entity_id }
        return deviceId to liveEntities.filter { it.entity_id in relatedEntityIds }
    }

    fun List<HAEntity>.unit(entity: HAEntity) =
        entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
    fun HAEntity.normalizedName() = (friendlyName ?: entity_id).lowercase()
    fun List<HAEntity>.pick(predicate: (HAEntity) -> Boolean): String? = firstOrNull(predicate)?.entity_id
    fun List<HAEntity>.pickUnique(predicate: (HAEntity) -> Boolean): String? =
        filter(predicate).singleOrNull()?.entity_id
    fun List<HAEntity>.isPower(entity: HAEntity) =
        entity.deviceClass == "power" || unit(entity) in setOf("W", "kW")
    fun List<HAEntity>.isEnergy(entity: HAEntity) =
        entity.deviceClass == "energy" || unit(entity).contains("Wh", ignoreCase = true)
    fun List<HAEntity>.isCost(entity: HAEntity) =
        entity.deviceClass == "monetary" || entity.normalizedName().contains("cost")
    fun HAEntity.matchesPhase(phase: Int): Boolean {
        val name = normalizedName()
        return name.contains("phase $phase") || name.contains(" l$phase")
    }
    fun fill(role: String, current: String?, guessed: String?): String? =
        if (role in config.customizedEntityRoles) current else guessed ?: current

    var result = config
    related("electricity")?.let { (deviceId, entities) ->
        fun phasePower(phase: Int) = entities.pick { entities.isPower(it) && it.matchesPhase(phase) }
        fun phaseClass(deviceClass: String, unit: String, phase: Int) = entities.pick {
            (it.deviceClass == deviceClass || entities.unit(it) == unit) && it.matchesPhase(phase)
        }
        result = result.copy(
            electricityDeviceId = result.electricityDeviceId ?: deviceId,
            gridPowerEntityId = fill("grid_power", result.gridPowerEntityId, entities.pick {
                entities.isPower(it) && !it.normalizedName().contains("phase") &&
                    listOf("grid", "current power", "active power", "power consumption", "power import", "net power")
                        .any(it.normalizedName()::contains)
            } ?: entities.pickUnique {
                entities.isPower(it) && !it.normalizedName().contains("phase") &&
                    listOf("home", "consumption", "load").none(it.normalizedName()::contains)
            }),
            homePowerEntityId = fill("home_power", result.homePowerEntityId, entities.pick {
                entities.isPower(it) && listOf("home", "house", "load").any(it.normalizedName()::contains)
            }),
            gridImportEntityId = fill("import_kwh", result.gridImportEntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("import") && !it.normalizedName().contains("tariff")
            }),
            gridExportEntityId = fill("export_kwh", result.gridExportEntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("export") && !it.normalizedName().contains("tariff")
            }),
            energyCostEntityId = fill("cost", result.energyCostEntityId, entities.pick { entities.isCost(it) }),
            powerPhase1EntityId = fill("phase1", result.powerPhase1EntityId, phasePower(1)),
            powerPhase2EntityId = fill("phase2", result.powerPhase2EntityId, phasePower(2)),
            powerPhase3EntityId = fill("phase3", result.powerPhase3EntityId, phasePower(3)),
            currentPhase1EntityId = fill("current1", result.currentPhase1EntityId, phaseClass("current", "A", 1)),
            currentPhase2EntityId = fill("current2", result.currentPhase2EntityId, phaseClass("current", "A", 2)),
            currentPhase3EntityId = fill("current3", result.currentPhase3EntityId, phaseClass("current", "A", 3)),
            voltagePhase1EntityId = fill("voltage1", result.voltagePhase1EntityId, phaseClass("voltage", "V", 1)),
            voltagePhase2EntityId = fill("voltage2", result.voltagePhase2EntityId, phaseClass("voltage", "V", 2)),
            voltagePhase3EntityId = fill("voltage3", result.voltagePhase3EntityId, phaseClass("voltage", "V", 3)),
            gridImportTariff1EntityId = fill("import_t1", result.gridImportTariff1EntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("import") && it.normalizedName().contains("tariff 1")
            }),
            gridImportTariff2EntityId = fill("import_t2", result.gridImportTariff2EntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("import") && it.normalizedName().contains("tariff 2")
            }),
            gridExportTariff1EntityId = fill("export_t1", result.gridExportTariff1EntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("export") && it.normalizedName().contains("tariff 1")
            }),
            gridExportTariff2EntityId = fill("export_t2", result.gridExportTariff2EntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("export") && it.normalizedName().contains("tariff 2")
            })
        )
    }
    related("solar")?.let { (deviceId, entities) ->
        result = result.copy(
            solarDeviceId = result.solarDeviceId ?: deviceId,
            solarPowerEntityId = fill("solar_power", result.solarPowerEntityId, entities.pick {
                entities.isPower(it) && listOf("solar", "production", "current").any(it.normalizedName()::contains)
            } ?: entities.pick { entities.isPower(it) }),
            solarEnergyEntityId = fill("solar_kwh", result.solarEnergyEntityId, entities.pick {
                entities.isEnergy(it) && (it.normalizedName().contains("today") || it.normalizedName().contains("daily"))
            } ?: entities.pick { entities.isEnergy(it) }),
            solarLast7DaysEntityId = fill("solar_7d", result.solarLast7DaysEntityId, entities.pick {
                entities.isEnergy(it) && (it.normalizedName().contains("7 days") || it.normalizedName().contains("seven"))
            }),
            solarLifetimeEntityId = fill("solar_lifetime", result.solarLifetimeEntityId, entities.pick {
                entities.isEnergy(it) && it.normalizedName().contains("lifetime")
            })
        )
    }
    related("battery")?.let { (deviceId, entities) ->
        result = result.copy(
            batteryDeviceId = result.batteryDeviceId ?: deviceId,
            batteryPowerEntityId = fill("battery_power", result.batteryPowerEntityId, entities.pick { entities.isPower(it) }),
            batteryEntityId = fill("battery_pct", result.batteryEntityId, entities.pick {
                it.deviceClass == "battery" || (entities.unit(it) == "%" && it.normalizedName().contains("batter"))
            })
        )
    }
    related("gas")?.let { (deviceId, entities) ->
        result = result.copy(
            gasDeviceId = result.gasDeviceId ?: deviceId,
            gasEntityId = fill("gas", result.gasEntityId, entities.pick {
                it.deviceClass == "gas" || (entities.unit(it).contains("m\u00B3") && !entities.unit(it).contains("/"))
            }),
            gasCurrentEntityId = fill("gas_current", result.gasCurrentEntityId, entities.pick { entities.unit(it).contains("m\u00B3/h") }),
            gasCostEntityId = fill("gas_cost", result.gasCostEntityId, entities.pick { entities.isCost(it) })
        )
    }
    related("water")?.let { (deviceId, entities) ->
        result = result.copy(
            waterDeviceId = result.waterDeviceId ?: deviceId,
            waterEntityId = fill("water", result.waterEntityId, entities.pick {
                it.deviceClass == "water" || entities.unit(it) == "L" ||
                    (entities.unit(it).contains("m\u00B3") && !entities.unit(it).contains("/"))
            }),
            waterCurrentEntityId = fill("water_current", result.waterCurrentEntityId, entities.pick {
                entities.unit(it).contains("/min") || entities.unit(it).contains("m\u00B3/h")
            }),
            waterCostEntityId = fill("water_cost", result.waterCostEntityId, entities.pick { entities.isCost(it) })
        )
    }
    related("carbon")?.let { (deviceId, _) ->
        result = result.copy(carbonDeviceId = result.carbonDeviceId ?: deviceId)
    }
    return result
}


class MainViewModel(val prefs: PreferencesManager, appCtx: Context? = null) : ViewModel() {
    private val networkMonitor = appCtx?.let { NetworkMonitor(it) }
    val currentSsid: StateFlow<String?> = networkMonitor?.currentSsid ?: MutableStateFlow(null)

    private data class InternalUrlFallback(val networkGeneration: Long, val failedUrl: String)
    private val internalUrlFallback = MutableStateFlow<InternalUrlFallback?>(null)

    /** The HA base URL to use right now: the internal URL when on a configured home Wi-Fi, else external. */
    private val preferredBaseUrl: Flow<String?> = combine(
        prefs.serverUrl,
        prefs.internalUrl,
        prefs.homeSsids,
        networkMonitor?.currentSsid ?: MutableStateFlow(null)
    ) { external, internal, homeSsids, ssid ->
        resolveBaseUrl(external, internal, homeSsids, ssid)
    }
    private val activeBaseUrl: Flow<String?> = combine(
        preferredBaseUrl,
        prefs.serverUrl,
        networkMonitor?.networkGeneration ?: MutableStateFlow(0L),
        internalUrlFallback
    ) { preferred, external, networkGeneration, fallback ->
        if (fallback?.networkGeneration == networkGeneration &&
            urlsEqual(fallback.failedUrl, preferred) &&
            !external.isNullOrBlank()
        ) external else preferred
    }

    private fun resolveBaseUrl(external: String?, internal: String?, homeSsids: List<String>, ssid: String?): String? =
        resolveHomeAssistantUrl(external, internal, homeSsids, ssid)

    private fun urlsEqual(first: String?, second: String?): Boolean =
        first?.trim()?.trimEnd('/')?.equals(second?.trim()?.trimEnd('/'), ignoreCase = true) == true

    private val _entities = MutableStateFlow<List<HAEntity>>(emptyList())
    val entities: StateFlow<List<HAEntity>> = _entities
    private val _displayName = MutableStateFlow("User")
    val displayName: StateFlow<String> = _displayName
    private val _profileAvatar = MutableStateFlow<String?>(null)
    val profileAvatar: StateFlow<String?> = _profileAvatar
    private val _profileBirthday = MutableStateFlow<String?>(null)
    val profileBirthday: StateFlow<String?> = _profileBirthday
    private val _profilePersonEntityId = MutableStateFlow<String?>(null)
    val profilePersonEntityId: StateFlow<String?> = _profilePersonEntityId
    private val originalProfilePictures = mutableMapOf<String, String?>()
    private val originalProfileBirthdays = mutableMapOf<String, String?>()

    // Keyed mirror used by UI selectors. Screens/widgets should observe the smallest set of
    // entity ids they render instead of collecting [entities], which invalidates them for every
    // Home Assistant state_changed event.
    private val _entitiesById = MutableStateFlow<Map<String, HAEntity>>(emptyMap())
    val entitiesById: StateFlow<Map<String, HAEntity>> = _entitiesById
    private val entityStateFlows = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<HAEntity?>>()
    private val matchingEntitySelectors = java.util.concurrent.ConcurrentHashMap<String, StateFlow<List<HAEntity>>>()
    private var discoveredHeaderAlarmIds: List<String> = emptyList()

    private fun entityState(entityId: String): StateFlow<HAEntity?> =
        entityStateFlows.computeIfAbsent(entityId) {
            MutableStateFlow(_entitiesById.value[entityId])
        }

    fun entitiesFor(entityIds: Collection<String>): StateFlow<List<HAEntity>> {
        val ids = entityIds.distinct()
        if (ids.isEmpty()) return MutableStateFlow(emptyList())
        return combine(ids.map(::entityState)) { states -> states.filterNotNull() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), entitiesForNow(ids))
    }

    fun entitiesMatching(predicate: (HAEntity) -> Boolean): StateFlow<List<HAEntity>> =
        _entitiesById
            .map { byId -> byId.values.filter(predicate) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                _entitiesById.value.values.filter(predicate)
            )

    /** Shares broad/domain selectors used by multiple widgets so the global entity map is filtered
     * once per update rather than once per widget instance. The key must uniquely describe the
     * predicate and any values it captures. */
    fun entitiesMatching(selectorKey: String, predicate: (HAEntity) -> Boolean): StateFlow<List<HAEntity>> =
        matchingEntitySelectors.computeIfAbsent(selectorKey) { entitiesMatching(predicate) }

    /** A full live entity list for pickers/dialogs, or a frozen current snapshot for dashboard
     * containers whose children own narrower entity subscriptions. */
    fun entityList(live: Boolean): StateFlow<List<HAEntity>> =
        if (live) entities else MutableStateFlow(_entities.value)

    fun entitiesForNow(entityIds: Collection<String>): List<HAEntity> {
        val byId = _entitiesById.value
        return entityIds.distinct().mapNotNull(byId::get)
    }

    /** A non-updating dependency snapshot for edit-mode previews. Recreated when edit mode opens,
     * while the underlying socket remains connected and current for the eventual return to runtime. */
    fun entitySnapshotFor(entityIds: Collection<String>): StateFlow<List<HAEntity>> =
        MutableStateFlow(entitiesForNow(entityIds))

    fun entitySnapshotMatching(predicate: (HAEntity) -> Boolean): StateFlow<List<HAEntity>> =
        MutableStateFlow(_entitiesById.value.values.filter(predicate))

    private fun publishEntities(value: List<HAEntity>) {
        val profiled = value.map(::applyProfileOverride)
        val alarmIds = profiled.asSequence()
            .map { it.entity_id }
            .filter { it.startsWith("alarm_control_panel.") }
            .sorted()
            .toList()
        if (alarmIds.isNotEmpty() && alarmIds != discoveredHeaderAlarmIds) {
            discoveredHeaderAlarmIds = alarmIds
            viewModelScope.launch { prefs.seedHeaderLeftAlarmEntitiesIfUnset(alarmIds) }
        }
        val previousById = _entitiesById.value
        val nextById = profiled.associateBy { it.entity_id }
        _entities.value = profiled
        _entitiesById.value = nextById
        // Only selectors for entities whose value actually changed are notified. This avoids every
        // button/widget selector waking up for every unrelated Home Assistant event.
        entityStateFlows.forEach { (entityId, stateFlow) ->
            val next = nextById[entityId]
            if (previousById[entityId] != next) stateFlow.value = next
        }
    }

    private fun applyProfileOverride(entity: HAEntity): HAEntity {
        if (entity.entity_id != _profilePersonEntityId.value) return entity
        if (!originalProfilePictures.containsKey(entity.entity_id)) originalProfilePictures[entity.entity_id] = entity.entityPicture
        if (!originalProfileBirthdays.containsKey(entity.entity_id)) {
            originalProfileBirthdays[entity.entity_id] = entity.attributes?.get("birthday")?.jsonPrimitive?.contentOrNull
        }
        val attributes = buildJsonObject {
            entity.attributes?.forEach { (key, value) ->
                if (key !in setOf("friendly_name", "entity_picture", "birthday")) put(key, value)
            }
            put("friendly_name", JsonPrimitive(_displayName.value))
            (_profileAvatar.value?.takeIf(String::isNotBlank) ?: originalProfilePictures[entity.entity_id])
                ?.let { put("entity_picture", JsonPrimitive(it)) }
            (_profileBirthday.value?.takeIf(String::isNotBlank) ?: originalProfileBirthdays[entity.entity_id])
                ?.let { put("birthday", JsonPrimitive(it)) }
        }
        return entity.copy(attributes = attributes)
    }

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

    val dashboards: StateFlow<List<HKIDashboard>> = prefs.dashboards.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val activeDashboardId: StateFlow<String?> = prefs.activeDashboardId.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )
    val defaultDashboardId: StateFlow<String?> = prefs.defaultDashboardId.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

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
    private val weatherForecastFetchedAt = mutableMapOf<String, Long>()

    fun weatherForecastFor(cacheKey: String): StateFlow<List<HAWeatherForecast>> =
        _weatherForecastCache
            .map { it[cacheKey].orEmpty() }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                weatherForecastForNow(cacheKey)
            )

    fun weatherForecastForNow(cacheKey: String): List<HAWeatherForecast> =
        _weatherForecastCache.value[cacheKey].orEmpty()

    private val _calendarEvents = MutableStateFlow<Map<String, List<HACalendarEvent>>>(emptyMap())
    val calendarEvents: StateFlow<Map<String, List<HACalendarEvent>>> = _calendarEvents
    private val calendarEventsFetchedAt = mutableMapOf<String, Long>()

    fun calendarEventsFor(cacheKey: String): StateFlow<List<HACalendarEvent>> =
        _calendarEvents
            .map { it[cacheKey].orEmpty() }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                calendarEventsForNow(cacheKey)
            )

    fun calendarEventsForNow(cacheKey: String): List<HACalendarEvent> =
        _calendarEvents.value[cacheKey].orEmpty()

    fun calendarEventsCacheKey(entityIds: List<String>, startMillis: Long, endMillis: Long): String =
        "${entityIds.distinct().sorted().joinToString(",")}|$startMillis|$endMillis"

    fun fetchWeatherForecastFor(entityId: String, type: String, force: Boolean = false) {
        val currentClient = client ?: return
        // Widgets and the weather dialog call this on every (re)composition; forecasts change
        // slowly, so serve the cache for a while instead of hitting HA on each open (the fetch
        // round-trip was what made the weather dialog feel sluggish).
        val key = "$entityId:$type"
        val fetchedAt = weatherForecastFetchedAt[key]
        val fresh = fetchedAt != null && System.currentTimeMillis() - fetchedAt < WEATHER_FORECAST_TTL_MS
        if (fresh && !force && _weatherForecastCache.value.containsKey(key)) return
        weatherForecastFetchedAt[key] = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { currentClient.getWeatherForecast(entityId, type) }.getOrDefault(emptyList())
            if (result.isEmpty()) weatherForecastFetchedAt.remove(key)
            val current = _weatherForecastCache.value.toMutableMap()
            current[key] = result
            _weatherForecastCache.value = current
        }
    }

    fun fetchCalendarEvents(
        entityIds: List<String>,
        startMillis: Long,
        endMillis: Long,
        force: Boolean = false
    ) {
        val currentClient = client ?: return
        val ids = entityIds.distinct().filter { it.startsWith("calendar.") }
        if (ids.isEmpty()) return
        val key = calendarEventsCacheKey(ids, startMillis, endMillis)
        val fetchedAt = calendarEventsFetchedAt[key]
        val fresh = fetchedAt != null && System.currentTimeMillis() - fetchedAt < CALENDAR_EVENTS_TTL_MS
        if (fresh && !force && _calendarEvents.value.containsKey(key)) return
        calendarEventsFetchedAt[key] = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                currentClient.getCalendarEvents(ids, startMillis, endMillis)
                    .values
                    .flatten()
            }.onSuccess { events ->
                _calendarEvents.value = _calendarEvents.value + (key to events)
            }.onFailure {
                calendarEventsFetchedAt.remove(key)
                addLog("Calendar fetch failed: ${it.message}")
            }
        }
    }

    private val _status = MutableStateFlow(ConnectionStatus.IDLE)
    val status: StateFlow<ConnectionStatus> = _status

    private val _connectionRoute = MutableStateFlow<HomeAssistantConnectionRoute?>(null)
    val connectionRoute: StateFlow<HomeAssistantConnectionRoute?> = _connectionRoute
    private val _connectionRouteSwitches = MutableSharedFlow<HomeAssistantConnectionRoute>(extraBufferCapacity = 1)
    val connectionRouteSwitches: SharedFlow<HomeAssistantConnectionRoute> = _connectionRouteSwitches.asSharedFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _historyMapping = MutableStateFlow<Map<String, List<HAHistoryEntry>>>(emptyMap())
    val historyMapping: StateFlow<Map<String, List<HAHistoryEntry>>> = _historyMapping

    // Entity/device registries, fetched on demand (used by the energy view's inverter device picker).
    private val _entityRegistry = MutableStateFlow<List<HAEntityRegistryEntry>>(emptyList())
    val entityRegistry: StateFlow<List<HAEntityRegistryEntry>> = _entityRegistry
    private val _deviceRegistry = MutableStateFlow<List<HADeviceRegistryEntry>>(emptyList())
    val deviceRegistry: StateFlow<List<HADeviceRegistryEntry>> = _deviceRegistry

    // Energy statistics (recorder aggregates), keyed "entityId|period" so ranges don't mix.
    private val _energyStats = MutableStateFlow<Map<String, List<HAStatPoint>>>(emptyMap())
    val energyStats: StateFlow<Map<String, List<HAStatPoint>>> = _energyStats

    /** HA solar forecast provider id -> epoch millis -> forecast Wh for that hour. */
    private val _energySolarForecasts = MutableStateFlow<Map<String, Map<Long, Float>>>(emptyMap())
    val energySolarForecasts: StateFlow<Map<String, Map<Long, Float>>> = _energySolarForecasts

    private fun applyEnergySolarForecasts(raw: JsonObject?) {
        _energySolarForecasts.value = raw.orEmpty().mapNotNull provider@{ (providerId, value) ->
            val providerJson = runCatching { value.jsonObject }.getOrNull() ?: return@provider null
            val hours = runCatching { providerJson["wh_hours"]?.jsonObject }.getOrNull().orEmpty()
                .mapNotNull hour@{ (timestamp, amount) ->
                    val millis = runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull()
                        ?: runCatching { OffsetDateTime.parse(timestamp).toInstant().toEpochMilli() }.getOrNull()
                        ?: return@hour null
                    val wh = amount.jsonPrimitive.doubleOrNull?.toFloat() ?: return@hour null
                    millis to wh
                }.toMap()
            providerId to hours
        }.toMap()
    }

    fun fetchHomeAssistantEnergySolarForecasts() {
        val currentClient = client ?: return
        viewModelScope.launch {
            runCatching { currentClient.getEnergySolarForecasts() }
                .onSuccess(::applyEnergySolarForecasts)
                .onFailure { addLog("Energy solar forecast unavailable: ${it.message}") }
        }
    }

    fun fetchEnergyStatistics(
        ids: List<String>, startMillis: Long, period: String, keySuffix: String,
        endMillis: Long? = null
    ) {
        val currentClient = client ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val stats = currentClient.getStatistics(ids, startMillis, period, endMillis)
                _energyStats.value = _energyStats.value + stats.mapKeys { (id, _) -> "$id|$keySuffix" }
            }.onFailure { addLog("Statistics fetch failed: ${it.message}") }
        }
    }

    private var registriesLoaded = false
    fun fetchRegistries(force: Boolean = false) {
        val currentClient = client ?: return
        // Registries barely change at runtime; dialogs call this on open, so serve the cache.
        if (
            registriesLoaded &&
            _entityRegistry.value.isNotEmpty() &&
            _deviceRegistry.value.isNotEmpty() &&
            !force
        ) return
        registriesLoaded = true
        viewModelScope.launch {
            runCatching {
                _entityRegistry.value = currentClient.getEntityRegistry()
                _deviceRegistry.value = currentClient.getDeviceRegistry()
            }.onFailure {
                registriesLoaded = false
                addLog("Registry fetch failed: ${it.message}")
            }
        }
    }

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _dashboardMode = MutableStateFlow("auto")
    val dashboardMode: StateFlow<String> = _dashboardMode

    private val _uiRevision = MutableStateFlow(0)
    val uiRevision: StateFlow<Int> = _uiRevision

    private val _forcedLogoutReason = MutableStateFlow<String?>(null)
    val forcedLogoutReason: StateFlow<String?> = _forcedLogoutReason

    // Notification history (persisted; written by the push channel and the foreground service).
    // Non-archived entries expire after 48h (purged on read here and on every append).
    val notifications: StateFlow<List<com.example.hki7.data.HKINotification>> =
        prefs.notificationHistory
            .map { list ->
                val cutoff = System.currentTimeMillis() - PushNotificationHandler.RETENTION_MS
                list.filter { it.archived || it.timestamp >= cutoff }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun updateNotifications(transform: (List<com.example.hki7.data.HKINotification>) -> List<com.example.hki7.data.HKINotification>) {
        viewModelScope.launch {
            prefs.saveNotificationHistory(transform(prefs.notificationHistory.first()))
        }
    }

    fun deleteNotification(id: String) = updateNotifications { list -> list.filterNot { it.id == id } }

    /** Clears all non-archived notifications; the archive is untouched. */
    fun clearNotifications() = updateNotifications { list -> list.filter { it.archived } }

    fun setNotificationRead(id: String, read: Boolean) =
        updateNotifications { list -> list.map { if (it.id == id) it.copy(read = read) else it } }

    fun markAllNotificationsRead() =
        updateNotifications { list -> list.map { if (it.archived) it else it.copy(read = true) } }

    fun markAllNotificationsUnread() =
        updateNotifications { list -> list.map { if (it.archived) it else it.copy(read = false) } }

    fun clearArchivedNotifications() = updateNotifications { list -> list.filterNot { it.archived } }

    fun archiveNotification(id: String) =
        updateNotifications { list -> list.map { if (it.id == id) it.copy(archived = true, read = true) else it } }

    fun unarchiveNotification(id: String) =
        updateNotifications { list -> list.map { if (it.id == id) it.copy(archived = false) else it } }

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
        val pageConfigs: Map<String, HKIPageConfig>,
        val floors: List<HAFloor>
    )

    private fun takeSnapshot() {
        undoStack.add(Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _pageConfigsMapping.value, _floors.value))
        redoStack.clear()
        if (undoStack.size > 20) undoStack.removeAt(0)
        updateHistoryState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentSnapshot = Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _pageConfigsMapping.value, _floors.value)
            redoStack.add(currentSnapshot)
            restoreSnapshot(undoStack.removeAt(undoStack.size - 1))
            updateHistoryState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentSnapshot = Snapshot(_areaWidgetsMapping.value, _areas.value.map { it.area_id }, _areaConfigsMapping.value, _pageConfigsMapping.value, _floors.value)
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
        _pageConfigsMapping.value = snapshot.pageConfigs
        _floors.value = snapshot.floors
        sortAreas(snapshot.areaOrder)
        // Force UI that caches an optimistic local order (ReorderableGrid) to rebuild against the
        // restored state. Without this an undo/redo made while editing a reorderable list (e.g. the
        // Climate tabs) isn't reflected until edit mode is toggled off. Discrete user action, so the
        // resulting rebuild/scroll-reset is acceptable here (unlike per-edit, which we never bump).
        _uiRevision.value += 1
        viewModelScope.launch {
            prefs.saveAreaWidgets(snapshot.widgets)
            prefs.saveAreaConfigs(snapshot.areaConfigs)
            prefs.savePageConfigs(snapshot.pageConfigs)
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

    private val _weatherCardWidths = MutableStateFlow<Map<String, String>>(emptyMap())
    val weatherCardWidths: StateFlow<Map<String, String>> = _weatherCardWidths

    private val _headerAlarmEntityIds = MutableStateFlow<List<String>>(emptyList())
    val headerAlarmEntityIds: StateFlow<List<String>> = _headerAlarmEntityIds

    private val _headerLeftAlarmEntityIds = MutableStateFlow<List<String>>(emptyList())
    val headerLeftAlarmEntityIds: StateFlow<List<String>> = _headerLeftAlarmEntityIds

    private val _alarmPendingSeconds = MutableStateFlow(0)
    val alarmPendingSeconds: StateFlow<Int> = _alarmPendingSeconds

    private var client: HomeAssistantClient? = null
    private var pollJob: Job? = null
    private var realtimeJob: Job? = null
    private var pushJob: Job? = null
    private val pushHandler by lazy { appContext?.let { PushNotificationHandler(it, prefs) } }
    private val realtimeBuffer = java.util.concurrent.ConcurrentHashMap<String, HAStateChange>()
    private var refreshJob: Job? = null
    private var internalUrlRetryJob: Job? = null
    private var initialAutoGenerationJob: Job? = null
    private var tokenRefreshJob: Job? = null
    private var highAccuracyJob: Job? = null
    private val refreshMutex = Mutex()
    private var lastTokenRefreshAt = 0L
    private var appVisible = true.also { com.example.hki7.data.AppVisibilityTracker.isVisible = true }
    private var lastDashboardRefreshAt = 0L
    private var appContext: Context? = null
    private var batteryReceiver: android.content.BroadcastReceiver? = null
    private var lastReportedBatteryPct = -1
    private var lastReportedCharging: Boolean? = null
    private var ignoreWidgetPrefsUntil = 0L
    private var ignoreConfigPrefsUntil = 0L
    private var activeConnectionKey: String? = null
    private var startupRefreshAttemptedFor: String? = null
    private var protectExistingRoomsOnNextImport = false
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
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.networkChangeGeneration.drop(1).collect {
                    internalUrlFallback.value?.let { fallback ->
                        if (appVisible) scheduleInternalUrlRetry(fallback, retryImmediately = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            prefs.ensureDashboardStore()
            val defaultId = prefs.defaultDashboardId.first()
            val activeId = prefs.activeDashboardId.first()
            if (defaultId != null && activeId != defaultId) prefs.switchDashboard(defaultId)
        }
        observeSettings()
        observeAreaOrder()
        observeAreaWidgets()
        observeAreaConfigs()
        observePageConfigs()
        observeDashboard()
        observeWeatherPrefs()
        // Auto generation is a persisted command, not merely a UI state. Observing it here makes
        // the first import start as soon as onboarding commits the choice, even if the composable
        // handoff or activity lifecycle does not deliver another callback.
        viewModelScope.launch {
            prefs.pendingAutoTakeover.distinctUntilChanged().collect { pending ->
                if (pending) completeInitialDashboardSetup()
            }
        }
    }

    private fun observeWeatherPrefs() {
        viewModelScope.launch { prefs.weatherDisplayType.collect { _weatherDisplayType.value = it } }
        viewModelScope.launch { prefs.headerLeftDisplayType.collect { _headerLeftDisplayType.value = it } }
        viewModelScope.launch { prefs.use24hFormat.collect { _use24hFormat.value = it } }
        viewModelScope.launch { prefs.useFullDayName.collect { _useFullDayName.value = it } }
        viewModelScope.launch { prefs.alarmEntityIds.collect { _headerAlarmEntityIds.value = it } }
        viewModelScope.launch { prefs.headerLeftAlarmEntityIds.collect { _headerLeftAlarmEntityIds.value = it } }
        viewModelScope.launch { prefs.alarmPendingSeconds.collect { _alarmPendingSeconds.value = it } }
        viewModelScope.launch { prefs.weatherCardWidths.collect { _weatherCardWidths.value = it } }
        viewModelScope.launch {
            combine(prefs.sunEntityId, prefs.moonEntityId, prefs.aqiEntityId, prefs.seasonEntityId, prefs.rainEntityId, prefs.weatherDeviceId) { args: Array<String?> ->
                mapOf("sun" to args[0], "moon" to args[1], "aqi" to args[2], "season" to args[3], "rain" to args[4], "device" to args[5])
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
        // Include the persisted mode so clearing a dashboard and switching to manual is observed
        // atomically; an empty saved list must also clear any rooms left in memory by an in-flight
        // onboarding refresh.
        // When moveArea() updates _areas.value AND writes areaOrder to DataStore, the combine
        // fires with BOTH new values simultaneously — no race condition.
        viewModelScope.launch {
            combine(prefs.savedAreas, prefs.areaOrder, prefs.dashboardMode) { saved, order, mode -> Triple(saved, order, mode) }
                .collect { (saved, order, mode) ->
                    if (mode != "auto") {
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
            combine(prefs.savedFloors, prefs.dashboardMode) { saved, mode -> saved to mode }.collect { (saved, mode) ->
                // Seed auto dashboards from the last successful registry refresh as well.
                // A fresh HA response will replace this shortly, but the Rooms screen should
                // not temporarily lose its floor grouping (and floor layout settings) at startup.
                if (mode != "auto" || _floors.value.isEmpty()) {
                    _floors.value = saved
                }
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
            combine(prefs.profileAvatar, prefs.profileBirthday, prefs.profilePersonEntityId) { avatar, birthday, personId ->
                Triple(avatar, birthday, personId)
            }.collect { (avatar, birthday, personId) ->
                _profileAvatar.value = avatar
                _profileBirthday.value = birthday
                _profilePersonEntityId.value = personId
                if (_entities.value.isNotEmpty()) {
                    publishEntities(_entities.value)
                    _people.value = _entities.value.filter { it.entity_id.startsWith("person.") }
                }
            }
        }
        viewModelScope.launch {
            combine(
                activeBaseUrl,
                prefs.accessToken,
                prefs.refreshToken,
                prefs.displayName,
                networkMonitor?.networkGeneration ?: MutableStateFlow(0L)
            ) { url, token, refresh, name, networkGeneration ->
                HKIAuthSettings(url, token, refresh, name, networkGeneration)
            }.collect { settings ->
                internalUrlFallback.value?.let { fallback ->
                    val configuredInternalUrl = prefs.internalUrl.first()
                    if (fallback.networkGeneration != settings.networkGeneration ||
                        !urlsEqual(fallback.failedUrl, configuredInternalUrl)
                    ) {
                        clearInternalUrlFallback()
                    }
                }
                _displayName.value = settings.name ?: "User"
                if (_entities.value.isNotEmpty() && _profilePersonEntityId.value != null) {
                    publishEntities(_entities.value)
                    _people.value = _entities.value.filter { it.entity_id.startsWith("person.") }
                }
                _currentUrl.value = settings.url ?: ""
                _accessToken.value = settings.token
                val connectionKey = "${settings.url}|${settings.token}|${settings.networkGeneration}"
                when {
                    settings.url.isNullOrBlank() -> {
                        addLog("Logged out or settings cleared.")
                        clearInternalUrlFallback()
                        stopSync()
                        tokenRefreshJob?.cancel()
                        tokenRefreshJob = null
                        highAccuracyJob?.cancel()
                        highAccuracyJob = null
                        appContext?.let {
                            LocationForegroundService.stop(it)
                            LocationWork.cancel(it)
                            com.example.hki7.data.BackgroundLocationReceiver.unregister(it)
                        }
                        client?.dispose()
                        client = null
                        activeConnectionKey = null
                        _connectionRoute.value = null
                        _status.value = ConnectionStatus.IDLE
                        publishEntities(emptyList())
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
                            // A different URL/token or a reconnected Wi-Fi transport invalidates
                            // requests and WebSockets held by the old client. Close it first so a
                            // half-open socket cannot leave the UI permanently stale.
                            stopSync()
                            client?.dispose()
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
                    else -> {
                        // Logout (Keep Config) deliberately leaves the server URL in place but
                        // removes both tokens. Do not let the previous authenticated client keep
                        // polling behind the re-login WebView.
                        stopSync()
                        clearInternalUrlFallback()
                        client?.dispose()
                        client = null
                        activeConnectionKey = null
                        _connectionRoute.value = null
                        _status.value = ConnectionStatus.IDLE
                    }
                }
            }
        }
    }

    private data class HKIAuthSettings(
        val url: String?,
        val token: String?,
        val refresh: String?,
        val name: String?,
        val networkGeneration: Long
    )

    private fun startSync() {
        startPolling()
        startRealtimeSync()
        startPushChannel()
    }

    private fun stopSync() {
        stopPolling()
        stopRealtimeSync()
        stopPushChannel()
        refreshJob?.cancel()
        refreshJob = null
        client?.closeSession()
    }

    /** Subscribes to the mobile_app websocket push channel while the app is visible, so calls to
     *  notify.mobile_app_<device> arrive instantly. When the background-notifications toggle is on,
     *  [PushForegroundService] owns the subscription instead (it also covers the foreground). */
    private fun startPushChannel() {
        pushJob?.cancel()
        val handler = pushHandler ?: return
        pushJob = viewModelScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                if (prefs.backgroundPushEnabled.first()) { delay(30.seconds); continue }
                val webhookId = prefs.mobileAppWebhookId.first()
                val currentClient = client
                if (webhookId.isNullOrBlank() || currentClient == null) { delay(10.seconds); continue }
                try {
                    currentClient.subscribePushNotifications(webhookId).collect { event ->
                        runCatching { handler.handle(event) }
                    }
                } catch (e: Exception) {
                    if (e.message != "AUTH_EXPIRED") addLog("Push channel interrupted: ${e.message}")
                }
                delay(5.seconds) // backoff before resubscribing
            }
        }
    }

    private fun stopPushChannel() { pushJob?.cancel(); pushJob = null }

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
            var hasAttemptedSubscription = false
            while (currentCoroutineContext().isActive) {
                val currentClient = client
                if (currentClient == null) {
                    delay(2.seconds)
                    continue
                }
                try {
                    // The polling job owns the initial/foreground seed. Only re-seed here after an
                    // actual socket interruption; otherwise startup downloads and parses all states twice.
                    if (hasAttemptedSubscription) {
                        refreshEntities(isSilent = true, includeDashboardRefresh = false)
                        refreshJob?.join()
                    }
                    hasAttemptedSubscription = true
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
                    if (appVisible && _status.value != ConnectionStatus.ERROR) {
                        _status.value = ConnectionStatus.CONNECTING
                    }
                } catch (e: Exception) {
                    if (e.message == "AUTH_EXPIRED") tryTokenRefresh()
                    else {
                        if (appVisible && _status.value != ConnectionStatus.ERROR) {
                            _status.value = ConnectionStatus.CONNECTING
                        }
                        addLog("Realtime sync interrupted: ${e.message}")
                    }
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
                if (ordered[change.entityId] != change.newState) {
                    ordered[change.entityId] = change.newState
                    changed = true
                }
            }
        }
        if (!changed) return
        val merged = applyPendingEntityStates(ordered.values.toList())
        publishEntities(merged)
        if (snapshot.keys.any { it.startsWith("person.") }) {
            _people.value = _entities.value.filter { it.entity_id.startsWith("person.") }
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
        com.example.hki7.data.AppVisibilityTracker.isVisible = visible
        if (appVisible == visible) return
        appVisible = visible
        if (visible) {
            if (client != null && realtimeJob?.isActive != true) startSync()
            internalUrlFallback.value?.let { scheduleInternalUrlRetry(it, retryImmediately = true) }
            viewModelScope.launch {
                if (prefs.pendingAutoTakeover.first()) completeInitialDashboardSetup()
            }
            // Re-arm the proactive token refresh: while backgrounded the scheduled refresh may
            // have fired and failed (Doze blocks network) or drifted past expiry while the
            // process was frozen. This refreshes immediately when the stored token is already stale.
            if (client != null) scheduleProactiveRefreshFromStoredExpiry()
            setBatteryMonitoring(true)
            appContext?.let { reportDeviceTelemetry(it) }
        } else {
            internalUrlRetryJob?.cancel()
            internalUrlRetryJob = null
            setBatteryMonitoring(false)
            stopSync()
            // Battery parity with the official app: never refresh tokens on a background timer
            // (with a dead network it would retry every minute for nothing). The stored token is
            // refreshed lazily on return to foreground instead — see the re-arm above.
            tokenRefreshJob?.cancel()
            tokenRefreshJob = null
        }
    }

    /** Starts the dashboard import selected at the end of first-run onboarding.
     *
     * Authentication normally starts the entity poll before the dashboard choice is made. That
     * first poll therefore runs in manual mode and cannot perform auto generation. Explicitly run
     * the pending import now, after the dashboard-mode transaction has completed, and retry brief
     * startup races without requiring the user to open the Rooms re-import dialog. */
    fun completeInitialDashboardSetup() {
        if (initialAutoGenerationJob?.isActive == true) return
        initialAutoGenerationJob = viewModelScope.launch {
            if (!prefs.pendingAutoTakeover.first()) return@launch
            // The authentication poll usually started while onboarding deliberately held the
            // dashboard in manual mode. Supersede it; waiting for that stale refresh can defer the
            // auto import until the 15-minute reseed or the next process start.
            refreshJob?.cancelAndJoin()
            repeat(3) {
                if (!prefs.pendingAutoTakeover.first()) return@launch
                refreshEntities(
                    isSilent = false,
                    allowReconnectRetry = true,
                    includeDashboardRefresh = true
                )
                // A failed refresh can chain a reconnect retry that reassigns refreshJob. Follow
                // the chain until it settles so attempts never overlap an in-flight import.
                var generationRefresh = refreshJob
                while (generationRefresh != null) {
                    generationRefresh.join()
                    val successor = refreshJob
                    generationRefresh = successor?.takeIf { it !== generationRefresh && it.isActive }
                }
                if (!prefs.pendingAutoTakeover.first()) return@launch
                delay(1500)
            }
            addLog("Initial dashboard generation is still pending; it will retry on the next sync.")
        }
    }

    private suspend fun <T> withHaRequestTimeout(
        label: String,
        timeoutMs: Long = 10_000L,
        block: suspend () -> T
    ): T = withTimeoutOrNull(timeoutMs.milliseconds) { block() }
            ?: throw java.net.SocketTimeoutException("$label timed out")

    private suspend fun markConnectionEstablished(url: String, connectedClient: HomeAssistantClient) {
        val route = classifyHomeAssistantConnectionRoute(
            activeUrl = url,
            internalUrl = prefs.internalUrl.first(),
            connectedViaLocalAddress = connectedClient.isConnectedViaLocalAddress() == true
        )
        val previous = _connectionRoute.value
        _connectionRoute.value = route
        _status.value = ConnectionStatus.CONNECTED
        if (previous != null && previous != route) _connectionRouteSwitches.tryEmit(route)
    }

    /** If the selected LAN address cannot answer, use the external/Nabu Casa URL for the rest of
     *  this Wi-Fi connection. A separate probe periodically switches back once LAN works again. */
    private suspend fun activateExternalFallback(failedUrl: String): Boolean {
        val internal = prefs.internalUrl.first()?.takeIf { it.isNotBlank() } ?: return false
        val external = prefs.serverUrl.first()?.takeIf { it.isNotBlank() } ?: return false
        if (!urlsEqual(failedUrl, internal) || urlsEqual(internal, external)) return false

        val networkGeneration = networkMonitor?.networkGeneration?.value ?: 0L
        val fallback = InternalUrlFallback(networkGeneration, failedUrl.trim().trimEnd('/'))
        if (internalUrlFallback.value != fallback) {
            addLog("Internal connection failed; falling back to $external")
            internalUrlFallback.value = fallback
            scheduleInternalUrlRetry(fallback)
        } else if (internalUrlRetryJob?.isActive != true) {
            scheduleInternalUrlRetry(fallback)
        }
        return true
    }

    private fun clearInternalUrlFallback() {
        internalUrlFallback.value = null
        internalUrlRetryJob?.cancel()
        internalUrlRetryJob = null
    }

    private fun scheduleInternalUrlRetry(
        fallback: InternalUrlFallback,
        retryImmediately: Boolean = false
    ) {
        internalUrlRetryJob?.cancel()
        internalUrlRetryJob = viewModelScope.launch(Dispatchers.IO) {
            var retryIndex = if (retryImmediately) -1 else 0
            while (currentCoroutineContext().isActive && internalUrlFallback.value == fallback) {
                val retryDelay = if (retryIndex < 0) 0L else {
                    INTERNAL_URL_RETRY_DELAYS_MS[retryIndex.coerceAtMost(INTERNAL_URL_RETRY_DELAYS_MS.lastIndex)]
                }
                if (retryDelay > 0) delay(retryDelay.milliseconds)
                retryIndex += 1
                if (!appVisible || internalUrlFallback.value != fallback) break

                val generation = networkMonitor?.networkGeneration?.value ?: 0L
                val internal = prefs.internalUrl.first()?.takeIf { it.isNotBlank() }
                val token = prefs.accessToken.first()?.takeIf { it.isNotBlank() }
                if (generation != fallback.networkGeneration ||
                    !urlsEqual(internal, fallback.failedUrl) || token == null
                ) {
                    if (internalUrlFallback.value == fallback) internalUrlFallback.value = null
                    break
                }

                val probe = HomeAssistantClient(internal!!, token)
                val reachable = try {
                    withHaRequestTimeout("Internal connection check", INTERNAL_URL_PROBE_TIMEOUT_MS) {
                        probe.checkConnection()
                    }
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                } finally {
                    probe.dispose()
                }

                if (reachable && internalUrlFallback.value == fallback) {
                    addLog("Internal connection restored; switching back to $internal")
                    internalUrlFallback.value = null
                    break
                }
            }
        }
    }

    fun refreshEntities(
        isSilent: Boolean = false,
        allowReconnectRetry: Boolean = true,
        includeDashboardRefresh: Boolean = true,
        failedConnectionAttempts: Int = 0
    ) {
        val now = SystemClock.elapsedRealtime()
        if (isSilent && failedConnectionAttempts == 0 && refreshJob?.isActive == true) return
        // Run network + JSON parsing + dashboard computation off the UI thread to avoid
        // startup/scroll jank. All state writes below go through thread-safe StateFlow/DataStore.
        refreshJob = viewModelScope.launch(Dispatchers.Default) {
            val attemptedUrl = _currentUrl.value
            var baseConnectionEstablished = false
            val currentClient = client ?: rebuildClientFromPrefs() ?: run {
                if (!isSilent) _status.value = ConnectionStatus.ERROR
                return@launch
            }
            if (!isSilent) _status.value = ConnectionStatus.CONNECTING
            try {
                val allEntities = withHaRequestTimeout("Loading Home Assistant states") {
                    currentClient.getEntities()
                }
                baseConnectionEstablished = true
                val displayEntities = applyPendingEntityStates(allEntities)
                publishEntities(displayEntities)
                _people.value = _entities.value.filter { it.entity_id.startsWith("person.") }

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
                        ?: runCatching {
                            withHaRequestTimeout("Loading weather forecast") {
                                currentClient.getWeatherForecast(weather.entity_id)
                            }
                        }.getOrDefault(emptyList())
                }.orEmpty()
                if (includeDashboardRefresh && prefs.dashboardMode.first() == "auto") {
                    val (allAreas, allFloors, entityRegistry, deviceRegistry) = coroutineScope {
                        val areas = async { withHaRequestTimeout("Loading areas") { currentClient.getAreas() } }
                        val floors = async { withHaRequestTimeout("Loading floors") { currentClient.getFloors() } }
                        val entitiesRegistry = async { withHaRequestTimeout("Loading entity registry") { currentClient.getEntityRegistry() } }
                        val devicesRegistry = async { withHaRequestTimeout("Loading device registry") { currentClient.getDeviceRegistry() } }
                        DashboardRegistrySnapshot(
                            areas.await(), floors.await(), entitiesRegistry.await(), devicesRegistry.await()
                        )
                    }
                    // The user may have selected Start Empty while registry requests were in
                    // flight. Never apply or persist their results after that atomic mode change.
                    if (prefs.dashboardMode.first() != "auto") {
                        markConnectionEstablished(attemptedUrl, currentClient)
                        return@launch
                    }
                    if (!isCompleteDashboardRegistrySnapshot(allEntities, allAreas, allFloors, entityRegistry, deviceRegistry)) {
                        // Keep the previous dashboard and leave any pending takeover set;
                        // completeInitialDashboardSetup's retry loop or the next sync re-imports.
                        // lastDashboardRefreshAt is deliberately not advanced.
                        addLog(
                            "Dashboard import skipped: incomplete Home Assistant snapshot " +
                                "(${allAreas.size} areas, ${allFloors.size} floors, " +
                                "${entityRegistry.size} registry entries, ${deviceRegistry.size} devices)"
                        )
                        markConnectionEstablished(attemptedUrl, currentClient)
                        return@launch
                    }
                    val savedOrder = prefs.areaOrder.first()
                    _entityRegistry.value = entityRegistry
                    _deviceRegistry.value = deviceRegistry
                    registriesLoaded = true
                    _areas.value = if (savedOrder.isNotEmpty()) {
                        allAreas.sortedBy { a -> savedOrder.indexOf(a.area_id).let { if (it == -1) Int.MAX_VALUE else it } }
                    } else allAreas
                    val savedFloorsById = prefs.savedFloors.first().associateBy { it.floor_id }
                    val floorsWithLocalLayout = allFloors.map { imported ->
                        savedFloorsById[imported.floor_id]?.let { saved ->
                            imported.copy(
                                columns = saved.columns,
                                isSquare = saved.isSquare,
                                cornerRadius = saved.cornerRadius,
                                compactTiles = saved.compactTiles,
                                width = saved.width
                            )
                        } ?: imported.withDefaultRoomLayout()
                    } + listOfNotNull(savedFloorsById["__rooms__"])
                    _floors.value = floorsWithLocalLayout
                    autoPopulateDashboard(allAreas, floorsWithLocalLayout, allEntities, entityRegistry, deviceRegistry)
                    if (prefs.pendingAutoTakeover.first()) {
                        freezeAutoGeneratedEntityViews(allEntities)
                        prefs.finishAutoGeneration()
                        _dashboardMode.value = "manual"
                    }
                    lastDashboardRefreshAt = SystemClock.elapsedRealtime()
                }
                markConnectionEstablished(attemptedUrl, currentClient)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!baseConnectionEstablished && e.message != "AUTH_EXPIRED" && activateExternalFallback(attemptedUrl)) {
                    _status.value = ConnectionStatus.CONNECTING
                    return@launch
                }
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
                val failureCount = failedConnectionAttempts + 1
                val failureStatus = connectionStatusAfterFailures(failureCount)
                _status.value = failureStatus
                if (allowReconnectRetry && failureStatus == ConnectionStatus.CONNECTING) {
                    val retryDelay = CONNECTION_RETRY_DELAYS_MS[
                        (failureCount - 1).coerceIn(0, CONNECTION_RETRY_DELAYS_MS.lastIndex)
                    ]
                    addLog(
                        "Connection attempt $failureCount failed: ${e.message}. " +
                            "Retrying in ${retryDelay / 1_000}s..."
                    )
                    delay(retryDelay.milliseconds)
                    val recoveredClient = rebuildClientFromPrefs()
                    if (recoveredClient != null) {
                        refreshEntities(
                            isSilent = isSilent,
                            allowReconnectRetry = true,
                            includeDashboardRefresh = includeDashboardRefresh,
                            failedConnectionAttempts = failureCount
                        )
                    } else {
                        _status.value = ConnectionStatus.ERROR
                        addLog("Unable to rebuild the Home Assistant connection.")
                    }
                } else {
                    _status.value = ConnectionStatus.ERROR
                    addLog("Refresh failed after $failureCount attempts: ${e.message}")
                }
            }
        }
    }

    private data class DashboardRegistrySnapshot(
        val areas: List<HAArea>,
        val floors: List<HAFloor>,
        val entities: List<HAEntityRegistryEntry>,
        val devices: List<HADeviceRegistryEntry>
    )

    private suspend fun rebuildClientFromPrefs(): HomeAssistantClient? {
        val url = resolveBaseUrl(
            prefs.serverUrl.first(),
            prefs.internalUrl.first(),
            prefs.homeSsids.first(),
            networkMonitor?.currentSsid?.value
        )?.takeIf { it.isNotBlank() } ?: return null
        val token = prefs.accessToken.first()?.takeIf { it.isNotBlank() }
        val refresh = prefs.refreshToken.first()?.takeIf { it.isNotBlank() }
        // A stored token past its persisted expiry can only 401; fall through to a refresh
        // instead of rebuilding a client that is doomed to fail.
        val expiry = prefs.accessTokenExpiry.first()
        val tokenUsable = expiry == null || expiry - System.currentTimeMillis() > 60_000L
        if (token != null && tokenUsable) {
            val rebuilt = HomeAssistantClient(url, token)
            client?.dispose()
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
                    // Save only the tokens: `url` is the currently *resolved* base URL (internal
                    // when on home Wi-Fi) and must not overwrite the stored external server URL.
                    prefs.saveAuthTokens(response.access_token, response.refresh_token, response.expires_in)
                    client?.dispose()
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
            // All attempts failed without an explicit rejection — keep auth and retry in a minute.
            // Without this reschedule nothing re-triggers a refresh until a request happens to 401.
            addLog("Token refresh failed after ${delays.size} attempts (transient). Will retry later.")
            _status.value = ConnectionStatus.ERROR
            tokenRefreshJob = viewModelScope.launch {
                delay(60.seconds)
                tryTokenRefresh()
            }
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
        publishEntities(_entities.value.map { entity ->
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
        })
    }

    private fun applyPendingEntityStates(entities: List<HAEntity>): List<HAEntity> {
        val now = SystemClock.elapsedRealtime()
        val byId = entities.associateBy { it.entity_id }
        val confirmDelay = 500L
        pendingEntityStates.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (byId[entityId]?.state?.equals(pending.state, ignoreCase = true) == true && now >= pending.requestedAt + confirmDelay)
        }
        pendingBrightness.entries.removeAll { (entityId, pending) ->
            pending.expiresAt <= now || (
                byId[entityId]?.brightness == pending.brightness &&
                    byId[entityId]?.state.equals("on", ignoreCase = true) &&
                    now >= pending.requestedAt + confirmDelay
                )
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
                // Brightness is committed through light.turn_on. Mirroring that state keeps the
                // interactive tile fill visible while Home Assistant confirms an initially-off light.
                val optimisticState = if (pendingEntityStates.containsKey(entity.entity_id)) modified.state else "on"
                modified = modified.copy(state = optimisticState, attributes = newAttrs)
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
        publishEntities(applyPendingEntityStates(_entities.value))
        callLightService(entityId, "turn_on", HAServiceCall(entity_id = entityId, brightness = haBrightness))
    }

    fun setOptimisticBrightness(entityId: String, brightness: Float) {
        val haBrightness = (brightness * 255).toInt().coerceIn(0, 255)
        val now = SystemClock.elapsedRealtime()
        pendingBrightness[entityId] = PendingBrightness(haBrightness, now + 8_000, now)
        publishEntities(applyPendingEntityStates(_entities.value))
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
    fun setWeatherCardWidth(card: String, width: String) { viewModelScope.launch { prefs.saveWeatherCardWidth(card, width) } }
    fun setHeaderAlarmEntities(entityIds: List<String>) { viewModelScope.launch { prefs.saveHeaderAlarmEntities(entityIds) } }
    fun setHeaderLeftAlarmEntities(entityIds: List<String>) { viewModelScope.launch { prefs.saveHeaderLeftAlarmEntities(entityIds) } }
    fun setAlarmPendingSeconds(seconds: Int) { viewModelScope.launch { prefs.saveAlarmPendingSeconds(seconds) } }

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

    /** Imports the read-only Energy dashboard preferences maintained by Home Assistant. */
    fun importHomeAssistantEnergyPreferences(pageKey: String = "energy", force: Boolean = false) {
        val current = (_pageConfigsMapping.value[pageKey] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()
        // Re-run the lightweight import for existing HA-backed setups that predate carbon-entry
        // discovery, so Electricity Maps appears without requiring a manual re-import.
        if (
            current.usesHomeAssistantEnergyPreferences &&
            current.gridCarbonFootprintEntityId != null &&
            current.hasImportedRelatedEntities &&
            !force
        ) return
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                val prefsResult = currentClient.getEnergyPreferences() ?: return@launch
                val sources = prefsResult["energy_sources"]?.jsonArray.orEmpty().mapNotNull { runCatching { it.jsonObject }.getOrNull() }
                val devices = prefsResult["device_consumption"]?.jsonArray.orEmpty().mapNotNull { runCatching { it.jsonObject }.getOrNull() }
                val waterDevices = prefsResult["device_consumption_water"]?.jsonArray.orEmpty().mapNotNull { runCatching { it.jsonObject }.getOrNull() }

                fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                fun source(type: String): JsonObject? = sources.firstOrNull { it.text("type") == type }
                fun JsonObject.powerRate(): String? = text("stat_rate")
                    ?: this["power_config"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { power ->
                        power.text("stat_rate") ?: power.text("stat_rate_from") ?: power.text("stat_rate_to")
                    }

                val grid = source("grid")
                val solar = source("solar")
                val battery = source("battery")
                val gas = source("gas")
                val water = source("water")
                val energyIds = devices.mapNotNull { it.text("stat_consumption") }.distinct()
                val waterDeviceIds = waterDevices.mapNotNull { it.text("stat_consumption") }.distinct()
                val solarForecastConfigEntryIds = sources.asSequence()
                    .filter { it.text("type") == "solar" }
                    .flatMap { solarSource ->
                        runCatching { solarSource["config_entry_solar_forecast"]?.jsonArray }
                            .getOrNull().orEmpty().asSequence()
                    }
                    .mapNotNull { it.jsonPrimitive.contentOrNull }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
                val registry = _entityRegistry.value.ifEmpty {
                    currentClient.getEntityRegistry().also { _entityRegistry.value = it }
                }
                // Keep the device selector usable even when Energy preferences are imported before
                // the normal dashboard registry refresh has run.
                if (_deviceRegistry.value.isEmpty()) {
                    _deviceRegistry.value = currentClient.getDeviceRegistry()
                }
                val registryByEntity = registry.associateBy { it.entity_id }
                val liveById = _entities.value.associateBy { it.entity_id }
                val carbonConfigEntries = runCatching {
                    currentClient.getConfigEntries("co2signal")
                }.getOrElse {
                    // Older HA versions or restricted users may not expose config entries. The
                    // resolver still supports the same registry/live-state fallback HA uses.
                    emptyList()
                }
                val carbonEntityId = resolveHomeAssistantEnergyCarbonEntity(
                    configEntries = carbonConfigEntries,
                    registry = registry,
                    liveEntities = _entities.value
                )
                if (sources.isEmpty() && devices.isEmpty() && waterDevices.isEmpty() && carbonEntityId == null) {
                    return@launch
                }
                val powerIds = devices.mapNotNull { device ->
                    device.text("stat_rate") ?: device.text("stat_consumption")?.let { consumptionId ->
                        val deviceId = registryByEntity[consumptionId]?.device_id ?: return@let null
                        registry.firstNotNullOfOrNull { entry ->
                            entry.entity_id.takeIf { id ->
                                entry.device_id == deviceId && liveById[id]?.deviceClass == "power"
                            }
                        }
                    }
                }.distinct()
                val importedNames = buildMap {
                    devices.forEach { device ->
                        val name = device.text("name") ?: return@forEach
                        device.text("stat_consumption")?.let { put(it, name) }
                        device.text("stat_rate")?.let { put(it, name) }
                    }
                    waterDevices.forEach { device ->
                        val name = device.text("name") ?: return@forEach
                        device.text("stat_consumption")?.let { put(it, name) }
                        device.text("stat_rate")?.let { put(it, name) }
                    }
                }

                val relatedSourceEntityIds = mapOf(
                        "electricity" to sources.filter { it.text("type") == "grid" }.flatMap { source ->
                            listOfNotNull(source.text("stat_energy_from"), source.text("stat_energy_to"), source.powerRate())
                        },
                        "solar" to sources.filter { it.text("type") == "solar" }.flatMap { source ->
                            listOfNotNull(source.text("stat_energy_from"), source.powerRate())
                        },
                        "battery" to sources.filter { it.text("type") == "battery" }.flatMap { source ->
                            listOfNotNull(
                                source.text("stat_soc"), source.text("stat_energy_from"),
                                source.text("stat_energy_to"), source.powerRate()
                            )
                        },
                        "gas" to sources.filter { it.text("type") == "gas" }.flatMap { source ->
                            listOfNotNull(source.text("stat_energy_from"), source.text("stat_rate"))
                        },
                        "water" to sources.filter { it.text("type") == "water" }.flatMap { source ->
                            listOfNotNull(source.text("stat_energy_from"), source.text("stat_rate"))
                        },
                        "carbon" to listOfNotNull(carbonEntityId)
                    )
                val related = importRelatedHomeAssistantEnergyEntities(
                    config = current,
                    sourceEntityIds = relatedSourceEntityIds,
                    registry = registry,
                    liveEntities = _entities.value
                )
                fun sourceDevice(category: String): String? = relatedSourceEntityIds[category].orEmpty()
                    .firstNotNullOfOrNull { registryByEntity[it]?.device_id }
                val gridPowerRate = supportingEnergyEntityForDevice(
                    grid?.powerRate(), sourceDevice("electricity"), registry
                )
                val solarPowerRate = supportingEnergyEntityForDevice(
                    solar?.powerRate(), sourceDevice("solar"), registry
                )
                val batteryPowerRate = supportingEnergyEntityForDevice(
                    battery?.powerRate(), sourceDevice("battery"), registry
                )
                val gasRate = supportingEnergyEntityForDevice(
                    gas?.text("stat_rate"), sourceDevice("gas"), registry
                )
                val waterRate = supportingEnergyEntityForDevice(
                    water?.text("stat_rate"), sourceDevice("water"), registry
                )
                // Values explicitly selected in HA's Energy settings take precedence over the
                // related device entities inferred above. Individual app overrides remain final.
                fun roleValue(role: String, currentValue: String?, haValue: String?, inferredValue: String?): String? =
                    if (role in current.customizedEntityRoles) currentValue else haValue ?: inferredValue
                val imported = related.copy(
                    usesHomeAssistantEnergyPreferences = true,
                    hasImportedRelatedEntities = true,
                    gridImportEntityId = roleValue("import_kwh", current.gridImportEntityId, grid?.text("stat_energy_from"), related.gridImportEntityId),
                    gridExportEntityId = roleValue("export_kwh", current.gridExportEntityId, grid?.text("stat_energy_to"), related.gridExportEntityId),
                    gridPowerEntityId = roleValue("grid_power", current.gridPowerEntityId, gridPowerRate, related.gridPowerEntityId),
                    energyCostEntityId = roleValue("cost", current.energyCostEntityId, grid?.text("stat_cost"), related.energyCostEntityId),
                    solarEnergyEntityId = roleValue("solar_kwh", current.solarEnergyEntityId, solar?.text("stat_energy_from"), related.solarEnergyEntityId),
                    solarPowerEntityId = roleValue("solar_power", current.solarPowerEntityId, solarPowerRate, related.solarPowerEntityId),
                    batteryPowerEntityId = roleValue("battery_power", current.batteryPowerEntityId, batteryPowerRate, related.batteryPowerEntityId),
                    batteryEntityId = roleValue("battery_pct", current.batteryEntityId, battery?.text("stat_soc"), related.batteryEntityId),
                    gasEntityId = roleValue("gas", current.gasEntityId, gas?.text("stat_energy_from"), related.gasEntityId),
                    gasCurrentEntityId = roleValue("gas_current", current.gasCurrentEntityId, gasRate, related.gasCurrentEntityId),
                    gasCostEntityId = roleValue("gas_cost", current.gasCostEntityId, gas?.text("stat_cost"), related.gasCostEntityId),
                    waterEntityId = roleValue("water", current.waterEntityId, water?.text("stat_energy_from"), related.waterEntityId),
                    waterCurrentEntityId = roleValue("water_current", current.waterCurrentEntityId, waterRate, related.waterCurrentEntityId),
                    waterCostEntityId = roleValue("water_cost", current.waterCostEntityId, water?.text("stat_cost"), related.waterCostEntityId),
                    gridCarbonFootprintEntityId = roleValue("carbon", current.gridCarbonFootprintEntityId, null, carbonEntityId ?: related.gridCarbonFootprintEntityId),
                    deviceEntityIds = powerIds,
                    energyDeviceEntityIds = energyIds,
                    waterDeviceEntityIds = waterDeviceIds,
                    solarForecastConfigEntryIds = solarForecastConfigEntryIds,
                    hiddenPowerDeviceEntityIds = emptyList(),
                    hiddenEnergyDeviceEntityIds = emptyList(),
                    hiddenWaterDeviceEntityIds = emptyList(),
                    customNames = current.customNames + importedNames
                )
                val page = _pageConfigsMapping.value[pageKey] ?: HKIPageConfig()
                val updated = _pageConfigsMapping.value + (pageKey to page.copy(energyConfig = imported))
                _pageConfigsMapping.value = updated
                prefs.savePageConfigs(updated)
                runCatching { currentClient.getEnergySolarForecasts() }
                    .onSuccess(::applyEnergySolarForecasts)
                    .onFailure { addLog("Energy solar forecast unavailable: ${it.message}") }
                addLog("Imported ${energyIds.size} energy and ${waterDeviceIds.size} water devices from Home Assistant")
            } catch (e: Exception) {
                addLog("Energy dashboard import unavailable: ${e.message}")
            }
        }
    }

    fun updateClimateConfig(pageKey: String, config: com.example.hki7.data.HKIClimateConfig) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        updatePageConfig(pageKey, current.copy(climateConfig = config))
    }

    fun hideClimateEntity(pageKey: String, entityId: String) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        val climateConfig = current.climateConfig ?: com.example.hki7.data.HKIClimateConfig()
        updatePageConfig(
            pageKey,
            current.copy(
                climateConfig = climateConfig.copy(
                    hiddenEntityIds = (climateConfig.hiddenEntityIds + entityId).distinct()
                )
            )
        )
    }

    fun updateSecurityConfig(pageKey: String, config: com.example.hki7.data.HKISecurityConfig) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        updatePageConfig(pageKey, current.copy(securityConfig = config))
    }

    fun hideSecurityEntity(pageKey: String, entityId: String) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        val securityConfig = current.securityConfig ?: com.example.hki7.data.HKISecurityConfig()
        updatePageConfig(
            pageKey,
            current.copy(
                securityConfig = securityConfig.copy(
                    hiddenEntityIds = (securityConfig.hiddenEntityIds + entityId).distinct()
                )
            )
        )
    }

    fun updateBatteryConfig(pageKey: String, config: com.example.hki7.data.HKIBatteryConfig) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        updatePageConfig(pageKey, current.copy(batteryConfig = config))
    }

    fun updateCustomPage(page: com.example.hki7.data.HKICustomPage) {
        viewModelScope.launch {
            val pages = prefs.customPages.first()
            prefs.saveCustomPages(pages.map { existing -> if (existing.id == page.id) page else existing })
        }
    }

    fun hideBatteryEntity(pageKey: String, entityId: String) {
        val current = _pageConfigsMapping.value[pageKey] ?: com.example.hki7.data.HKIPageConfig()
        val batteryConfig = current.batteryConfig ?: com.example.hki7.data.HKIBatteryConfig()
        updatePageConfig(
            pageKey,
            current.copy(
                batteryConfig = batteryConfig.copy(
                    hiddenEntityIds = (batteryConfig.hiddenEntityIds + entityId).distinct()
                )
            )
        )
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
        publishEntities(applyPendingEntityStates(_entities.value))
    }

    fun setCoverPosition(entityId: String, position: Int) {
        val currentClient = client ?: return
        val pos = position.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        // hold the target optimistically while the blind physically moves (mirrors light brightness)
        pendingCoverPosition[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        publishEntities(applyPendingEntityStates(_entities.value))
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
        publishEntities(applyPendingEntityStates(_entities.value))
    }

    fun setCoverTiltPosition(entityId: String, tilt: Int) {
        val currentClient = client ?: return
        val pos = tilt.coerceIn(0, 100)
        val now = SystemClock.elapsedRealtime()
        pendingCoverTilt[entityId] = PendingCoverPosition(pos, now + 8_000, now)
        publishEntities(applyPendingEntityStates(_entities.value))
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

    /** Browses a media player's library (playlists, favorites, …); null when unsupported/offline. */
    suspend fun browseMedia(entityId: String, contentId: String? = null, contentType: String? = null): com.example.hki7.data.HAMediaBrowseItem? =
        runCatching { client?.browseMedia(entityId, contentId, contentType) }.getOrNull()

    /** Manual reconnect from the connection-error overlay; true when it ends up CONNECTED. */
    suspend fun retryConnection(): Boolean {
        refreshEntities()
        // Skip the replayed current (ERROR) value, then wait for this attempt to settle.
        val settled = withTimeoutOrNull(20_000) {
            status.drop(1).first { it == ConnectionStatus.CONNECTED || it == ConnectionStatus.ERROR }
        }
        return settled == ConnectionStatus.CONNECTED
    }

    /** Clears credentials (config preserved) and routes the user to the login screen. */
    fun forceRelogin(reason: String) {
        viewModelScope.launch { prefs.clearAuth() }
        _forcedLogoutReason.value = reason
    }

    /** Routes the user to the login screen while keeping the stored tokens — a successful login
     *  simply overwrites them. Blank reason means user-initiated, so no snackbar is shown. */
    fun requestRelogin() {
        _forcedLogoutReason.value = ""
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
        // Official-app-style passive background location: keeps the fused provider warm so
        // geofence transitions fire promptly, and reports the throttled background fixes to HA.
        com.example.hki7.data.BackgroundLocationReceiver.register(ctx)
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
        internalUrlRetryJob?.cancel()
        stopSync()
        client?.dispose()
        client = null
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

    fun addWeatherStackToArea(areaId: String, title: String? = "Weather", icon: String? = null) {
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
                columns = 1,
                isSquare = false,
                showBadge = false,
                cornerRadius = 24,
                stackType = "weather"
            )
        )
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addSwipingStackToArea(areaId: String) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(HKISwipingStack(id = UUID.randomUUID().toString()))
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addEmptyStackToArea(areaId: String) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(HKIEmptyStack(id = UUID.randomUUID().toString()))
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    fun addSingleEntityWidgetToArea(
        areaId: String,
        type: String,
        entityId: String,
        config: HKIButtonConfig = HKIButtonConfig()
    ) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(
            HKISingleEntityWidget(
                id = UUID.randomUUID().toString(),
                entityId = entityId,
                kind = type,
                isSquare = type != "camera",
                config = config
            )
        )
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
    }

    /** Calls an arbitrary service with a free-form JSON payload (custom actions). */
    fun callServiceRaw(domain: String, service: String, payload: JsonObject) {
        val currentClient = client ?: return
        viewModelScope.launch {
            try {
                currentClient.callServiceRaw(domain, service, payload)
                refreshAfterServiceCall()
            } catch (e: Exception) {
                addLog("Service call failed: ${e.message}")
            }
        }
    }

    /** Domain-based default behavior for a plain tap (used when an action resolves to "default"). */
    private fun defaultActionType(entityId: String, trigger: String, cameraStack: Boolean = false): String {
        val domain = entityId.substringBefore(".")
        return when {
            trigger != "tap" -> "more_info"
            cameraStack -> "more_info"
            domain in listOf("sensor", "binary_sensor", "camera", "climate", "cover", "lock", "fan", "humidifier", "alarm_control_panel", "person", "vacuum") -> "more_info"
            else -> "toggle"
        }
    }

    /** Runs a structured [HKIAction] against [ownerEntityId]. Side-effecting types (toggle,
     *  call_service) execute inline and return [ActionOutcome.Handled]; UI-routing types return an
     *  outcome the caller resolves (open more-info, navigate, open URL). */
    fun executeAction(action: HKIAction, ownerEntityId: String, trigger: String = "tap"): ActionOutcome {
        val type = action.type.takeUnless { it == "default" } ?: defaultActionType(ownerEntityId, trigger)
        return when (type) {
            "none" -> ActionOutcome.None
            "toggle" -> { toggleEntity(action.targetEntityId ?: ownerEntityId); ActionOutcome.Handled }
            "more_info" -> ActionOutcome.OpenMoreInfo(action.moreInfoEntityId ?: action.targetEntityId ?: ownerEntityId)
            "navigate" -> action.navigationTarget?.let { ActionOutcome.Navigate(it) } ?: ActionOutcome.None
            "url" -> action.url?.takeIf { it.isNotBlank() }?.let { ActionOutcome.OpenUrl(it) } ?: ActionOutcome.None
            "call_service" -> {
                val service = action.service?.takeIf { it.contains(".") }
                if (service == null) { ActionOutcome.None }
                else {
                    val domain = service.substringBefore(".")
                    val name = service.substringAfter(".")
                    val target = action.targetEntityId ?: ownerEntityId
                    val payload = buildJsonObject {
                        if (target.isNotBlank()) put("entity_id", JsonPrimitive(target))
                        action.data?.forEach { (k, v) -> put(k, v) }
                    }
                    callServiceRaw(domain, name, payload)
                    ActionOutcome.Handled
                }
            }
            else -> ActionOutcome.None
        }
    }

    /** Resolves the effective action for a button/badge trigger, preferring the structured
     *  override, then the legacy string, then the domain default. */
    fun resolveButtonAction(config: HKIButtonConfig?, entityId: String, trigger: String, cameraStack: Boolean = false): HKIAction {
        val ex = when (trigger) {
            "double" -> config?.doubleTapActionEx
            "hold" -> config?.holdActionEx
            else -> config?.tapActionEx
        }
        if (ex != null) return ex
        val legacy = when (trigger) {
            "double" -> config?.doubleTapAction
            "hold" -> config?.holdAction
            else -> config?.tapAction
        } ?: defaultActionType(entityId, trigger, cameraStack)
        // Vacuums have no meaningful toggle; their tap opens the rich dialog. The stored default is
        // "toggle" (shared by all button configs), so treat that as open-dialog. A user who really
        // wants a different action sets a structured override (handled above via [ex]).
        if (legacy == "toggle" && entityId.substringBefore(".") == "vacuum") {
            return HKIAction(type = "more_info")
        }
        return HKIAction(type = legacy)
    }

    fun performButtonAction(areaId: String, stackId: String, entityId: String, trigger: String): ActionOutcome {
        val stack = _areaWidgetsMapping.value[areaId]?.filterIsInstance<HKIButtonStack>()?.firstOrNull { it.id == stackId }
        val config = stack?.buttonConfigs?.get(entityId)
        val action = resolveButtonAction(config, entityId, trigger, cameraStack = stack?.stackType == "camera")
        return executeAction(action, entityId, trigger)
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
        takeSnapshot()
        val updated = if (_floors.value.any { it.floor_id == floor.floor_id }) {
            _floors.value.map { if (it.floor_id == floor.floor_id) floor else it }
        } else {
            _floors.value + floor
        }
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

    fun createDashboard(name: String, auto: Boolean) {
        viewModelScope.launch {
            prefs.createDashboard(name, auto)
            _dashboardMode.value = if (auto) "auto" else "manual"
            _areaWidgetsMapping.value = emptyMap()
            _areaConfigsMapping.value = emptyMap()
            _pageConfigsMapping.value = if (auto) emptyMap() else emptyManualPageConfigs()
            if (!auto) {
                _areas.value = emptyList()
                _floors.value = emptyList()
                prefs.savePageConfigs(_pageConfigsMapping.value)
            } else {
                refreshEntities(isSilent = true)
            }
        }
    }

    fun switchDashboard(id: String) {
        viewModelScope.launch {
            if (prefs.switchDashboard(id)) {
                _areas.value = prefs.savedAreas.first()
                _floors.value = prefs.savedFloors.first()
                _areaWidgetsMapping.value = prefs.areaWidgets.first()
                _areaConfigsMapping.value = prefs.areaConfigs.first()
                _pageConfigsMapping.value = prefs.pageConfigs.first()
                _dashboardMode.value = prefs.dashboardMode.first()
                if (_dashboardMode.value == "auto") refreshEntities(isSilent = true)
            }
        }
    }

    fun renameDashboard(id: String, name: String) { viewModelScope.launch { prefs.renameDashboard(id, name) } }
    fun deleteDashboard(id: String) { viewModelScope.launch { prefs.deleteDashboard(id) } }
    fun setDefaultDashboard(id: String) { viewModelScope.launch { prefs.setDefaultDashboard(id) } }

    fun reimportRooms(fromScratch: Boolean) {
        viewModelScope.launch {
            val currentClient = client ?: return@launch
            try {
                val (importedAreas, importedFloors, entityRegistry, deviceRegistry) = coroutineScope {
                    val areas = async { withTimeout(10.seconds) { currentClient.getAreas() } }
                    val floors = async { withTimeout(10.seconds) { currentClient.getFloors() } }
                    val registry = async { withTimeout(10.seconds) { currentClient.getEntityRegistry() } }
                    val devices = async { withTimeout(10.seconds) { currentClient.getDeviceRegistry() } }
                    DashboardRegistrySnapshot(areas.await(), floors.await(), registry.await(), devices.await())
                }
                if (!isCompleteDashboardRegistrySnapshot(_entities.value, importedAreas, importedFloors, entityRegistry, deviceRegistry)) {
                    // Abort before the from-scratch clear below: replacing existing rooms with a
                    // degraded snapshot would destroy them for no gain.
                    addLog("Room re-import skipped: Home Assistant returned an incomplete snapshot; try again.")
                    return@launch
                }
                val savedFloorsById = if (fromScratch) emptyMap() else _floors.value.associateBy { it.floor_id }
                val floorsWithLocalLayout = importedFloors.map { imported ->
                    savedFloorsById[imported.floor_id]?.let { saved ->
                        imported.copy(
                            columns = saved.columns,
                            isSquare = saved.isSquare,
                            cornerRadius = saved.cornerRadius,
                            compactTiles = saved.compactTiles,
                            width = saved.width
                        )
                    } ?: imported.withDefaultRoomLayout()
                }
                if (fromScratch) {
                    _areas.value = emptyList()
                    _floors.value = emptyList()
                    _areaWidgetsMapping.value = emptyMap()
                    _areaConfigsMapping.value = emptyMap()
                    prefs.clearDashboardConfig(keepMode = true)
                } else {
                    protectExistingRoomsOnNextImport = true
                }
                _entityRegistry.value = entityRegistry
                _deviceRegistry.value = deviceRegistry
                _areas.value = importedAreas
                _floors.value = floorsWithLocalLayout
                // Deliberately update only room/floor state. Page configs for climate, security,
                // energy, and battery are not touched by a Rooms re-import.
                autoPopulateDashboard(importedAreas, floorsWithLocalLayout, _entities.value, entityRegistry, deviceRegistry)
                addLog("Re-imported ${importedAreas.size} rooms from Home Assistant")
            } catch (e: Exception) {
                protectExistingRoomsOnNextImport = false
                addLog("Room re-import failed: ${e.message}")
            }
        }
    }

    fun clearRoomImports() {
        _areas.value = emptyList()
        _floors.value = emptyList()
        _areaWidgetsMapping.value = emptyMap()
        _areaConfigsMapping.value = emptyMap()
        viewModelScope.launch {
            prefs.saveAreas(emptyList())
            prefs.saveFloors(emptyList())
            prefs.saveAreaWidgets(emptyMap())
            prefs.saveAreaConfigs(emptyMap())
            prefs.saveAreaOrder(emptyList())
        }
    }

    fun reimportClimate(fromScratch: Boolean) {
        val current = _pageConfigsMapping.value["climate"] ?: HKIPageConfig()
        val old = if (fromScratch) HKIClimateConfig() else current.climateConfig ?: HKIClimateConfig()
        val all = _entities.value
        val registryById = _entityRegistry.value.associateBy { it.entity_id }
        val sensorClasses = mapOf(
            "temperature" to setOf("temperature"), "humidity" to setOf("humidity"),
            "pressure" to setOf("pressure", "atmospheric_pressure"), "co2" to setOf("carbon_dioxide", "co2"),
            "air" to setOf("aqi", "pm1", "pm10", "pm25", "volatile_organic_compounds")
        )
        val config = old.copy(
            manualOnly = true,
            extraClimateIds = (old.extraClimateIds + all.filter { it.entity_id.startsWith("climate.") }.map { it.entity_id }).distinct(),
            extraHumidifierIds = (old.extraHumidifierIds + all.filter { it.entity_id.startsWith("humidifier.") }.map { it.entity_id }).distinct(),
            extraFanIds = (old.extraFanIds + all.filter { it.entity_id.startsWith("fan.") }.map { it.entity_id }).distinct(),
            extraSensorIds = sensorClasses.mapValues { (key, classes) ->
                (old.extraSensorIds[key].orEmpty() + all.filter {
                    it.deviceClass in classes && it.isAutoClimateSensorFor(key, registryById[it.entity_id])
                }.map { it.entity_id }).distinct()
            }
        )
        updatePageConfig("climate", current.copy(climateConfig = config))
    }

    fun clearClimateImports() {
        val current = _pageConfigsMapping.value["climate"] ?: HKIPageConfig()
        updatePageConfig("climate", current.copy(climateConfig = HKIClimateConfig(manualOnly = true)))
    }

    fun reimportSecurity(fromScratch: Boolean) {
        val current = _pageConfigsMapping.value["security"] ?: HKIPageConfig()
        val old = if (fromScratch) HKISecurityConfig() else current.securityConfig ?: HKISecurityConfig()
        val keys = listOf("doors", "windows", "garage", "locks", "motion", "presence", "safety", "alarms", "cameras")
        val imported = keys.associateWith { key -> _entities.value.filter { it.isAutoSecurityEntityFor(key) }.map { it.entity_id } }
        val config = old.copy(
            manualOnly = true,
            extraEntityIds = keys.associateWith { key -> (old.extraEntityIds[key].orEmpty() + imported[key].orEmpty()).distinct() }
        )
        updatePageConfig("security", current.copy(securityConfig = config))
    }

    fun clearSecurityImports() {
        val current = _pageConfigsMapping.value["security"] ?: HKIPageConfig()
        updatePageConfig("security", current.copy(securityConfig = HKISecurityConfig(manualOnly = true)))
    }

    fun reimportEnergy(fromScratch: Boolean) {
        val current = _pageConfigsMapping.value["energy"] ?: HKIPageConfig()
        val retained = if (fromScratch) HKIEnergyConfig() else (current.energyConfig ?: HKIEnergyConfig()).copy(manualOnly = false)
        updatePageConfig("energy", current.copy(energyConfig = retained))
        importHomeAssistantEnergyPreferences(force = true)
    }

    fun clearEnergyImports() {
        val current = _pageConfigsMapping.value["energy"] ?: HKIPageConfig()
        updatePageConfig("energy", current.copy(energyConfig = HKIEnergyConfig(manualOnly = true)))
    }

    fun reimportBattery(fromScratch: Boolean) {
        val current = _pageConfigsMapping.value["battery"] ?: HKIPageConfig()
        val old = if (fromScratch) HKIBatteryConfig() else current.batteryConfig ?: HKIBatteryConfig()
        val imported = _entities.value.filter { it.isBatteryPercentageSensor() }.map { it.entity_id }
        updatePageConfig(
            "battery",
            current.copy(batteryConfig = old.copy(manualOnly = true, extraEntityIds = imported.distinct()))
        )
    }

    fun clearBatteryImports() {
        val current = _pageConfigsMapping.value["battery"] ?: HKIPageConfig()
        updatePageConfig("battery", current.copy(batteryConfig = HKIBatteryConfig(manualOnly = true)))
    }

    private fun emptyManualPageConfigs(): Map<String, HKIPageConfig> = mapOf(
        "climate" to HKIPageConfig(climateConfig = HKIClimateConfig(manualOnly = true)),
        "security" to HKIPageConfig(securityConfig = HKISecurityConfig(manualOnly = true)),
        "battery" to HKIPageConfig(batteryConfig = HKIBatteryConfig(manualOnly = true)),
        "energy" to HKIPageConfig(energyConfig = HKIEnergyConfig(manualOnly = true))
    )

    private suspend fun freezeAutoGeneratedEntityViews(allEntities: List<HAEntity>) {
        val registryById = _entityRegistry.value.associateBy { it.entity_id }
        val climateGroups = listOf(
            "temperature" to setOf("temperature"),
            "humidity" to setOf("humidity"),
            "pressure" to setOf("pressure", "atmospheric_pressure"),
            "co2" to setOf("carbon_dioxide", "co2"),
            "air" to setOf("aqi", "pm1", "pm10", "pm25", "volatile_organic_compounds")
        )
        val climate = HKIClimateConfig(
            manualOnly = true,
            extraClimateIds = allEntities.filter { it.entity_id.startsWith("climate.") }.map { it.entity_id },
            extraHumidifierIds = allEntities.filter { it.entity_id.startsWith("humidifier.") }.map { it.entity_id },
            extraFanIds = allEntities.filter { it.entity_id.startsWith("fan.") }.map { it.entity_id },
            extraSensorIds = climateGroups.associate { (key, classes) ->
                key to allEntities.filter {
                    it.deviceClass in classes && it.isAutoClimateSensorFor(key, registryById[it.entity_id])
                }.map { it.entity_id }
            }
        )
        val securityKeys = listOf("doors", "windows", "garage", "locks", "motion", "presence", "safety", "alarms", "cameras")
        val security = HKISecurityConfig(
            manualOnly = true,
            extraEntityIds = securityKeys.associateWith { key -> allEntities.filter { it.isAutoSecurityEntityFor(key) }.map { it.entity_id } }
        )
        val batteryIds = allEntities.filter { it.isBatteryPercentageSensor() }.map { it.entity_id }
        val updated = _pageConfigsMapping.value.toMutableMap().apply {
            this["climate"] = (this["climate"] ?: HKIPageConfig()).copy(climateConfig = climate)
            this["security"] = (this["security"] ?: HKIPageConfig()).copy(securityConfig = security)
            this["battery"] = (this["battery"] ?: HKIPageConfig()).copy(
                batteryConfig = HKIBatteryConfig(manualOnly = true, extraEntityIds = batteryIds)
            )
        }
        _pageConfigsMapping.value = updated
        prefs.savePageConfigs(updated)
    }

    /** Appends any prebuilt widget (used by the energy card/stack widgets). */
    fun addWidgetToArea(areaId: String, widget: HKIRoomWidget) {
        takeSnapshot()
        bumpWidgetUi()
        val currentMapping = _areaWidgetsMapping.value.toMutableMap()
        val currentList = currentMapping[areaId]?.toMutableList() ?: mutableListOf()
        currentList.add(widget)
        currentMapping[areaId] = currentList
        _areaWidgetsMapping.value = currentMapping
        viewModelScope.launch { prefs.saveAreaWidgets(currentMapping) }
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
        takeSnapshot()
        val updated = _pageConfigsMapping.value.toMutableMap()
        updated[pageKey] = config
        _pageConfigsMapping.value = updated
        viewModelScope.launch { prefs.savePageConfigs(updated) }
    }

    private fun bumpWidgetUi() {
        _uiRevision.value += 1
        ignoreWidgetPrefsUntil = SystemClock.elapsedRealtime() + 5000
    }

    private suspend fun autoPopulateDashboard(
        areas: List<HAArea>,
        floors: List<HAFloor>,
        entities: List<HAEntity>,
        registry: List<HAEntityRegistryEntry>,
        devices: List<HADeviceRegistryEntry>
    ) {
        prefs.seedHeaderLeftAlarmEntitiesIfUnset(
            entities.filter { it.entity_id.startsWith("alarm_control_panel.") }.map { it.entity_id },
            showByDefault = true
        )
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
        val entitiesByArea = entities.groupBy { areaByEntity[it.entity_id] }
        val climateWindowEntityIds = climateOwnedWindowEntityIds(entities, registry)

        areas.forEach { area ->
            val areaEntities = entitiesByArea[area.area_id]
                .orEmpty()
                .filter { it.entity_id.substringBefore(".") in AUTO_ROOM_IMPORT_DOMAINS }
            val mediaPlayerIds = areaEntities
                .filter { it.entity_id.startsWith("media_player.") }
                .map(HAEntity::entity_id)
            val discoveredRoomStatus = discoverRoomStatus(
                entities = areaEntities,
                excludedWindowEntityIds = climateWindowEntityIds
            )

            if (protectExistingRoomsOnNextImport && (existingWidgets.containsKey(area.area_id) || existingConfigs.containsKey(area.area_id))) {
                // Refresh auto-owned summary and media bindings on older imported rooms while
                // preserving every source group the user customized explicitly.
                val current = existingConfigs[area.area_id]
                if (current != null) {
                    var enriched = current
                    if (!current.roomEntitiesCustomized) {
                        enriched = enriched.copy(
                            roomStatusEntityIds = discoveredRoomStatus.entityIds,
                            roomTemperatureEntityId = null,
                            roomHumidityEntityId = null,
                            roomTemperatureEntityIds = discoveredRoomStatus.temperatureEntityIds,
                            roomHumidityEntityIds = discoveredRoomStatus.humidityEntityIds
                        )
                    }
                    if (!current.mediaPlayersCustomized) {
                        enriched = enriched.copy(
                            mediaPlayerEntityId = null,
                            mediaPlayerEntityIds = mediaPlayerIds
                        )
                    }
                    if (enriched != current) {
                        existingConfigs[area.area_id] = enriched
                        changedConfigs = true
                    }
                }
                return@forEach
            }
            val entityIdsByDomain = areaEntities.groupBy(
                keySelector = { it.entity_id.substringBefore(".") },
                valueTransform = { it.entity_id }
            )
            val lockIds = entityIdsByDomain["lock"].orEmpty()
            val climateIds = entityIdsByDomain["climate"].orEmpty()
            val cameraIds = entityIdsByDomain["camera"].orEmpty()
            val blindIds = entityIdsByDomain["cover"].orEmpty()

            fun autoBadge(domain: String, side: String, shape: String): HKIBadge? {
                val ids = entityIdsByDomain[domain].orEmpty()
                if (ids.isEmpty()) return null
                return HKIBadge(
                    id = "auto_${area.area_id}_$domain",
                    entityId = ids.first(),
                    entityIds = ids,
                    shape = shape,
                    side = side
                )
            }

            val importedBadges = buildList {
                // Left lane: cameras only.
                autoBadge("camera", side = "left", shape = "circle")?.let(::add)

                // Right lane is ordered from left to right. Climate is deliberately appended last,
                // with lock (or cover when there is no lock) immediately before it when available.
                autoBadge("fan", side = "right", shape = "circle")?.let(::add)
                autoBadge("humidifier", side = "right", shape = "circle")?.let(::add)
                autoBadge("cover", side = "right", shape = "circle")?.let(::add)
                autoBadge("lock", side = "right", shape = "circle")?.let(::add)
                autoBadge("climate", side = "right", shape = "pill")?.let(::add)
            }

            val current = existingConfigs[area.area_id] ?: HKIAreaConfig()
            val importedConfig = current.copy(
                floorId = area.floor_id,
                icon = current.icon ?: area.icon,
                wallpaper = current.wallpaper ?: area.picture,
                mediaPlayerEntityId = if (current.mediaPlayersCustomized) {
                    current.mediaPlayerEntityId
                } else {
                    null
                },
                mediaPlayerEntityIds = if (current.mediaPlayersCustomized) {
                    current.mediaPlayerEntityIds
                } else {
                    mediaPlayerIds
                },
                lockEntityId = lockIds.firstOrNull(),
                climateEntityId = climateIds.firstOrNull(),
                cameraEntityId = cameraIds.firstOrNull(),
                blindEntityId = blindIds.firstOrNull(),
                lockEntityIds = lockIds,
                climateEntityIds = climateIds,
                cameraEntityIds = cameraIds,
                blindEntityIds = blindIds,
                lockIcon = current.lockIcon ?: "Door",
                climateIcon = current.climateIcon ?: "thermostat",
                cameraIcon = current.cameraIcon ?: "CameraAlt",
                blindIcon = current.blindIcon ?: "Blinds",
                roomStatusEntityIds = if (current.roomEntitiesCustomized) {
                    current.roomStatusEntityIds
                } else {
                    discoveredRoomStatus.entityIds
                },
                roomTemperatureEntityId = if (current.roomEntitiesCustomized) {
                    current.roomTemperatureEntityId
                } else {
                    null
                },
                roomHumidityEntityId = if (current.roomEntitiesCustomized) {
                    current.roomHumidityEntityId
                } else {
                    null
                },
                roomTemperatureEntityIds = if (current.roomEntitiesCustomized) {
                    current.roomTemperatureEntityIds
                } else {
                    discoveredRoomStatus.temperatureEntityIds
                },
                roomHumidityEntityIds = if (current.roomEntitiesCustomized) {
                    current.roomHumidityEntityIds
                } else {
                    discoveredRoomStatus.humidityEntityIds
                },
                badgeBar = HKIBadgeBarConfig(
                    badges = importedBadges,
                    alignment = "split",
                    leftOverflow = false,
                    rightOverflow = true
                )
            )
            if (existingConfigs[area.area_id] != importedConfig) {
                existingConfigs[area.area_id] = importedConfig
                changedConfigs = true
            }

            val stacks = areaEntities
                .filter { it.entity_id.substringBefore(".") in AUTO_ROOM_STACK_DOMAINS }
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
                        buttonStyle = existingStack?.buttonStyle.orEmpty(),
                        cornerRadius = existingStack?.cornerRadius ?: 28,
                        isHidden = existingStack?.isHidden ?: false,
                        defaultCollapsed = existingStack?.defaultCollapsed ?: false,
                        isCollapsed = existingStack?.isCollapsed,
                        buttonConfigs = existingStack?.buttonConfigs.orEmpty()
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
            prefs.saveAreaWidgets(existingWidgets)
        }
        if (changedConfigs) {
            _areaConfigsMapping.value = existingConfigs
            prefs.saveAreaConfigs(existingConfigs)
        }
        // These writes must finish before finishAutoGeneration snapshots the active dashboard.
        // Detached saves allowed the empty onboarding configuration to win on process shutdown.
        prefs.saveAreas(areas)
        prefs.saveFloors(floors)
        prefs.saveAreaOrder(areas.map { it.area_id })
        protectExistingRoomsOnNextImport = false
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

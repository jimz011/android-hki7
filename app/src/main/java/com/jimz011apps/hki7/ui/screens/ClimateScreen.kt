@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HAServiceCall
import com.jimz011apps.hki7.data.HKIClimateCardWidget
import com.jimz011apps.hki7.data.HKIClimateConfig
import com.jimz011apps.hki7.data.HKIClimateStack
import com.jimz011apps.hki7.data.HKIPageConfig
import com.jimz011apps.hki7.data.HKISecurityConfig
import com.jimz011apps.hki7.data.withDisplayName
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.EntitySensorGraphCard
import com.jimz011apps.hki7.ui.components.HKIDialog
import com.jimz011apps.hki7.ui.components.HKIPage
import com.jimz011apps.hki7.ui.components.HKISlider
import com.jimz011apps.hki7.ui.components.HistoryRangeChips
import com.jimz011apps.hki7.ui.components.ReorderAxis
import com.jimz011apps.hki7.ui.components.ReorderableGrid
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.hvacColor
import com.jimz011apps.hki7.ui.components.hvacGradient
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.LocalItemCornerRadius
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.localizedClimateModeLabel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CLIMATE_PAGE_KEY = "climate"

// Palette in the Energy page's Homey-inspired style.
private val TempWarm    = Color(0xFFEF5350)
private val CoolBlue    = Color(0xFF1E90FF)
private val HumidBlue   = Color(0xFF29B6F6)
private val PressPurple = Color(0xFF7E57C2)
private val Co2Teal     = Color(0xFF26A69A)
private val AirGreen    = Color(0xFF66BB6A)
private val MistCyan    = Color(0xFF26C6DA)

/** One sensor tile / detail tab: a set of related HA `device_class`es shown together. */
private data class ClimateSensorGroup(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val color: Color,
    val deviceClasses: Set<String>
)

private val climateSensorGroups = listOf(
    ClimateSensorGroup("temperature", R.string.cr_climate_temperature, R.string.cr_climate_all_temperature_sensors,
        Icons.Default.Thermostat, TempWarm, setOf("temperature")),
    // Outside sits right next to Temperature. Never auto-discovered — its sensors are set manually
    // (temperature/humidity/pressure) in Climate settings.
    ClimateSensorGroup("outside", R.string.cr_climate_outside, R.string.cr_climate_outside_summary,
        Icons.Default.WbSunny, TempWarm, emptySet()),
    ClimateSensorGroup("humidity", R.string.cr_climate_humidity, R.string.cr_climate_all_humidity_sensors,
        Icons.Default.WaterDrop, HumidBlue, setOf("humidity")),
    ClimateSensorGroup("pressure", R.string.cr_climate_air_pressure, R.string.cr_climate_all_pressure_sensors,
        Icons.Default.Speed, PressPurple, setOf("pressure", "atmospheric_pressure")),
    ClimateSensorGroup("co2", R.string.cr_climate_co2, R.string.cr_climate_carbon_dioxide_sensors,
        Icons.Default.Co2, Co2Teal, setOf("carbon_dioxide")),
    ClimateSensorGroup("air", R.string.cr_climate_air_quality, R.string.cr_climate_air_quality_summary,
        Icons.Default.Air, AirGreen, setOf(
            "pm1", "pm25", "pm10", "aqi", "volatile_organic_compounds",
            "volatile_organic_compounds_parts", "nitrogen_dioxide", "carbon_monoxide"
        ))
)

private val weatherSensorPlatforms = setOf(
    "accuweather", "buienradar", "climacell", "dwd_weather", "environment_canada",
    "met", "nws", "open_meteo", "openweathermap", "pirateweather", "tomorrowio",
    "weatherflow", "weatherkit", "yr"
)

private val hardwareTemperatureTokens = setOf(
    "cpu", "disk", "drive", "gpu", "hdd", "memory", "motherboard", "nvme",
    "processor", "ram", "ssd", "storage"
)

/** Automatic Climate discovery is intentionally about room conditions, not machine health or
 * internet weather feeds. Manual additions remain unrestricted. */
internal fun HAEntity.isAutoClimateSensorFor(
    groupKey: String,
    registryEntry: HAEntityRegistryEntry? = null
): Boolean {
    val group = climateSensorGroups.firstOrNull { it.key == groupKey } ?: return false
    if (!entity_id.startsWith("sensor.") || deviceClass !in group.deviceClasses || numericState() == null) return false
    if (registryEntry?.entity_category == "diagnostic") return false

    val platform = registryEntry?.platform?.lowercase(Locale.ROOT)
    if (platform in weatherSensorPlatforms) return false
    val normalizedId = entity_id.substringAfter('.').lowercase(Locale.ROOT)
    if (weatherSensorPlatforms.any { normalizedId == it || normalizedId.startsWith("${it}_") }) return false

    if (groupKey == "temperature") {
        val searchable = "$normalizedId ${friendlyName.orEmpty().lowercase(Locale.ROOT)}"
        val tokens = searchable.split(Regex("[^a-z0-9]+"))
        if (tokens.any { it in hardwareTemperatureTokens }) return false
    }
    return true
}

private fun HAEntity.numericState(): Float? =
    if (state == "unavailable" || state == "unknown") null else state.toFloatOrNull()

private fun HAEntity.unit(): String =
    attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""

private fun formatValue(v: Float): String =
    if (kotlin.math.abs(v) >= 100f) "%.0f".format(Locale.getDefault(), v)
    else "%.1f".format(Locale.getDefault(), v)

internal data class ClimateOpeningState(
    val openDoors: Int = 0,
    val openWindows: Int = 0
)

internal fun HAEntity.isClimateOpeningCandidate(): Boolean {
    return isAutoSecurityEntityFor("doors") || isAutoSecurityEntityFor("windows")
}

internal fun Iterable<HAEntity>.climateOpeningState(
    config: HKISecurityConfig = HKISecurityConfig()
): ClimateOpeningState {
    val all = toList()
    val byId = all.associateBy { it.entity_id }
    val hidden = config.hiddenEntityIds.toSet()
    fun group(key: String): List<HAEntity> = (
        all.filter { it.isAutoSecurityEntityFor(key) } +
            config.extraEntityIds[key].orEmpty().mapNotNull(byId::get)
        ).distinctBy { it.entity_id }.filterNot { it.entity_id in hidden }
    val doors = group("doors")
    val doorIds = doors.mapTo(mutableSetOf()) { it.entity_id }
    val windows = group("windows").filterNot { it.entity_id in doorIds }
    val snapshot = mapOf(
        "doors" to doors,
        "windows" to windows
    ).toSecuritySceneState()
    return ClimateOpeningState(
        openDoors = snapshot.openDoors,
        openWindows = snapshot.openWindows
    )
}

@Composable
private fun rememberClimateOpeningState(viewModel: MainViewModel): ClimateOpeningState {
    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val config = (pageConfigs["security"] ?: HKIPageConfig()).securityConfig ?: HKISecurityConfig()
    val manualIds = remember(config) {
        (config.extraEntityIds["doors"].orEmpty() + config.extraEntityIds["windows"].orEmpty()).toSet()
    }
    val selectorKey = remember(manualIds) {
        "climate_openings:${manualIds.sorted().joinToString(",")}"
    }
    val flow = remember(viewModel, selectorKey) {
        viewModel.entitiesMatching(selectorKey) { entity ->
            entity.isClimateOpeningCandidate() || entity.entity_id in manualIds
        }
    }
    val entities by flow.collectAsState()
    return remember(entities, config) { entities.climateOpeningState(config) }
}

/** What the Climate hero can meaningfully animate for one climate entity. */
internal enum class ClimateSceneActivity { HEATING, COOLING, FAN, DRYING, DEFROSTING }

/**
 * Resolve actual HVAC activity without assuming every integration exposes `hvac_action`.
 *
 * `hvac_action` is authoritative when present, so an idle thermostat whose selected mode is
 * `cool` remains visually idle. Some AC integrations omit that optional attribute entirely;
 * for those integrations the selected `heat`, `cool`, `dry`, or `fan_only` mode is the best
 * available live signal and must drive the house scene.
 */
internal fun HAEntity.climateSceneActivity(): ClimateSceneActivity? {
    fun String.normalized() = trim().lowercase(Locale.ROOT).replace('-', '_').replace(' ', '_')
    val entityState = state.normalized()
    if (entityState in setOf("off", "idle", "unavailable", "unknown")) return null
    val mode = attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
        ?.normalized()
        ?.takeUnless(String::isBlank)
        ?: entityState

    val action = attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
        ?.normalized()
        ?.takeUnless { it.isBlank() || it == "unknown" || it == "unavailable" }
    if (action != null) {
        when (action) {
            "off", "idle" -> return null
            "heating", "heat", "preheating" -> return ClimateSceneActivity.HEATING
            "cooling", "cool" -> return ClimateSceneActivity.COOLING
            "fan", "fan_only" -> return ClimateSceneActivity.FAN
            "drying", "dry" -> return ClimateSceneActivity.DRYING
            "defrosting", "defrost" -> return ClimateSceneActivity.DEFROSTING
            // Custom integrations sometimes expose a generic action such as "running". In that
            // case their selected hvac_mode/state is still more useful than suppressing motion.
        }
    }

    return when (mode) {
        "heating", "heat", "preheating" -> ClimateSceneActivity.HEATING
        "cooling", "cool" -> ClimateSceneActivity.COOLING
        "fan", "fan_only" -> ClimateSceneActivity.FAN
        "drying", "dry" -> ClimateSceneActivity.DRYING
        "defrosting", "defrost" -> ClimateSceneActivity.DEFROSTING
        "auto", "heat_cool" -> {
            val current = attributes?.get("current_temperature")?.jsonPrimitive?.doubleOrNull
            val low = attributes?.get("target_temp_low")?.jsonPrimitive?.doubleOrNull
            val high = attributes?.get("target_temp_high")?.jsonPrimitive?.doubleOrNull
            when {
                current == null -> null
                low != null && current < low - 0.2 -> ClimateSceneActivity.HEATING
                high != null && current > high + 0.2 -> ClimateSceneActivity.COOLING
                else -> null
            }
        }
        else -> null
    }
}

private fun List<HAEntity>.applyClimateOrder(order: List<String>): List<HAEntity> =
    if (order.isEmpty()) this else sortedWith(
        compareBy<HAEntity> {
            val idx = order.indexOf(it.entity_id)
            if (idx == -1) Int.MAX_VALUE else idx
        }.thenBy { it.friendlyName ?: it.entity_id }
    )

@Composable
fun ClimateScreen(viewModel: MainViewModel) {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    // Bumped by undo/redo; keying the page content on it rebuilds the reorderable lists so a
    // restored order shows immediately instead of only after leaving edit mode.
    val uiRevision by viewModel.uiRevision.collectAsState()
    val appColors = LocalHKIAppColors.current
    val climateConfig: HKIClimateConfig =
        (pageConfigsMap[CLIMATE_PAGE_KEY] ?: HKIPageConfig()).climateConfig ?: HKIClimateConfig()
    val openingState = rememberClimateOpeningState(viewModel)
    val climateDependencyIds = remember(climateConfig) {
        buildSet {
            addAll(climateConfig.extraClimateIds)
            addAll(climateConfig.extraHumidifierIds)
            addAll(climateConfig.extraFanIds)
            addAll(climateConfig.purifierEntityIds)
            climateConfig.extraSensorIds.values.forEach(::addAll)
            climateConfig.outsideWeatherEntityId?.let(::add)
        }
    }
    val climateEntityFlow = remember(viewModel, climateDependencyIds) {
        viewModel.entitiesMatching { entity ->
            val domain = entity.entity_id.substringBefore('.')
            domain == "climate" || domain == "humidifier" || domain == "fan" || domain == "sensor" || domain == "weather" || entity.entity_id in climateDependencyIds
        }
    }
    val rawEntities by climateEntityFlow.collectAsState()
    val entities = remember(rawEntities, climateConfig.customNames) {
        rawEntities.map { it.withDisplayName(climateConfig.customNames[it.entity_id]) }
    }
    val hidden = remember(climateConfig) { climateConfig.hiddenEntityIds.toSet() }
    val entityById = remember(entities) { entities.associateBy { it.entity_id } }
    val registryById = remember(entityRegistry) { entityRegistry.associateBy { it.entity_id } }
    // Outside weather: an explicit choice wins; otherwise, when no outside sensors are configured,
    // auto-use the same weather entity as the header pill (first weather.* entity).
    val effectiveOutsideWeatherId = remember(climateConfig, entities) {
        climateConfig.outsideWeatherEntityId
            ?: entities.firstOrNull { it.entity_id.startsWith("weather.") }?.entity_id?.takeIf {
                climateConfig.outsideTemperatureIds.isEmpty() && climateConfig.outsideHumidityIds.isEmpty() && climateConfig.outsidePressureIds.isEmpty()
            }
    }

    fun hideEntity(id: String) {
        viewModel.hideClimateEntity(CLIMATE_PAGE_KEY, id)
    }
    var renameEntity by remember { mutableStateOf<HAEntity?>(null) }
    var dialDialogEntity by remember { mutableStateOf<HAEntity?>(null) }
    dialDialogEntity?.let { entity ->
        PagedRoleDialog(
            role = "climate",
            entities = listOf(entity),
            viewModel = viewModel,
            onDismiss = { dialDialogEntity = null },
            useClimateDial = true
        )
    }
    renameEntity?.let { entity ->
        ClimateDeviceSettingsDialog(
            entity = entity,
            config = climateConfig,
            onDismiss = { renameEntity = null },
            onSave = { updated ->
                viewModel.updateClimateConfig(CLIMATE_PAGE_KEY, updated)
                renameEntity = null
            }
        )
    }
    fun reorderClimateEntities(visible: List<HAEntity>, from: Int, to: Int) {
        val visibleIds = visible.map { it.entity_id }.toMutableList().apply { add(to, removeAt(from)) }
        viewModel.updateClimateConfig(
            CLIMATE_PAGE_KEY,
            climateConfig.copy(entityOrder = visibleIds + climateConfig.entityOrder.filterNot { it in visibleIds })
        )
    }
    fun applyClimateEntityOrder(newIds: List<String>) {
        viewModel.updateClimateConfig(
            CLIMATE_PAGE_KEY,
            climateConfig.copy(entityOrder = newIds + climateConfig.entityOrder.filterNot { it in newIds })
        )
    }
    var showReorderDevices by remember { mutableStateOf(false) }

    // Thermostats & air conditioners: every climate.* entity, plus manual additions, minus removed.
    val climateEntities = remember(entities, climateConfig) {
        ((if (climateConfig.manualOnly) emptyList() else entities.filter { it.entity_id.startsWith("climate.") }) +
            climateConfig.extraClimateIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Below this width a half/third device card (especially a dial) is too cramped to read, so
    // every card is forced to full width — one per row.
    val narrowDeviceCards =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } < 360.dp
    val climateDeviceRows = remember(climateEntities, climateConfig.deviceCardWidths, climateConfig.defaultDeviceCardWidth, climateConfig.defaultDeviceCardStyle, narrowDeviceCards) {
        buildList<List<HAEntity>> {
            var row = mutableListOf<HAEntity>()
            var used = 0
            climateEntities.forEach { entity ->
                val configuredWidth = climateConfig.deviceCardWidths[entity.entity_id] ?: climateConfig.defaultDeviceCardWidth
                val style = climateConfig.defaultDeviceCardStyle
                val effectiveWidth = if (style == "dial" && configuredWidth == "third") "half" else configuredWidth
                val span = if (narrowDeviceCards) 6 else when (effectiveWidth) {
                    "third" -> 2
                    "half" -> 3
                    else -> 6
                }
                if (row.isNotEmpty() && used + span > 6) {
                    add(row)
                    row = mutableListOf()
                    used = 0
                }
                row.add(entity)
                used += span
                if (used == 6) {
                    add(row)
                    row = mutableListOf()
                    used = 0
                }
            }
            if (row.isNotEmpty()) add(row)
        }
    }
    // Humidifiers/dehumidifiers: humidifier.* domain plus manual additions.
    val humidifierEntities = remember(entities, climateConfig) {
        ((if (climateConfig.manualOnly) emptyList() else entities.filter { it.entity_id.startsWith("humidifier.") }) +
            climateConfig.extraHumidifierIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Native fan.* entities are imported automatically. Air purifiers remain an optional
    // user-curated subset with their own tab.
    val fanEntities = remember(entities, climateConfig) {
        ((if (climateConfig.manualOnly) emptyList() else entities.filter { it.entity_id.startsWith("fan.") }) +
            climateConfig.extraFanIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Air purifiers: fans carry no device_class, so these come from Climate Settings.
    val purifierEntities = remember(entities, climateConfig) {
        climateConfig.purifierEntityIds.mapNotNull { entityById[it] }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Sensors per group: auto-discovered by device_class plus manual additions, minus removed.
    val groupSensors: Map<String, List<HAEntity>> = remember(entities, climateConfig, registryById) {
        climateSensorGroups.associate { group ->
            val auto = if (climateConfig.manualOnly) emptyList() else entities.filter { e ->
                e.isAutoClimateSensorFor(group.key, registryById[e.entity_id])
            }
            // Outside pulls from its three dedicated (temperature/humidity/pressure) lists, not the
            // generic extraSensorIds map (kept as a legacy fallback for any previously-added ids).
            val extras = if (group.key == "outside") {
                // The linked weather entity is graphed separately as its temperature/humidity/pressure
                // attributes (see ClimateSensorDetailPage) rather than as a plain sensor row — its own
                // `state` is a non-numeric condition string ("cloudy"), not a graphable value.
                (climateConfig.outsideTemperatureIds + climateConfig.outsideHumidityIds +
                    climateConfig.outsidePressureIds + climateConfig.extraSensorIds["outside"].orEmpty())
                    .mapNotNull { entityById[it] }
            } else {
                climateConfig.extraSensorIds[group.key].orEmpty().mapNotNull { entityById[it] }
            }
            group.key to (auto + extras)
                .distinctBy { it.entity_id }
                .filter { it.entity_id !in hidden }
                .sortedBy { it.friendlyName ?: it.entity_id }
                .applyClimateOrder(climateConfig.entityOrder)
        }
    }

    if (showReorderDevices) {
        com.jimz011apps.hki7.ui.components.ReorderItemsDialog(
            title = stringResource(R.string.ui_reorder_devices_8bcc061),
            subtitle = stringResource(R.string.ui_drag_to_set_the_order_thermostats_and_ac_units_62d49e7),
            items = climateEntities.map {
                com.jimz011apps.hki7.ui.components.ReorderItem(
                    it.entity_id,
                    climateConfig.customNames[it.entity_id]?.takeIf(String::isNotBlank) ?: it.friendlyName ?: it.entity_id,
                    climateConfig.customIcons[it.entity_id] ?: com.jimz011apps.hki7.ui.components.defaultEntityIconSlug(it)
                )
            },
            onDismiss = { showReorderDevices = false },
            onSave = { applyClimateEntityOrder(it); showReorderDevices = false }
        )
    }

    var page by rememberSaveable { mutableStateOf("climate") }
    var entitySearch by rememberSaveable { mutableStateOf("") }
    var entitySort by rememberSaveable { mutableStateOf("custom") }
    androidx.activity.compose.BackHandler(enabled = page != "climate") { page = "climate" }
    val activeGroup = climateSensorGroups.find { it.key == page }
    LaunchedEffect(page) { entitySearch = "" }

    fun filteredEntities(source: List<HAEntity>): List<HAEntity> {
        val needle = entitySearch.trim().lowercase(Locale.getDefault())
        val filtered = if (needle.isBlank()) source else source.filter { entity ->
            (entity.friendlyName ?: entity.entity_id).lowercase(Locale.getDefault()).contains(needle)
        }
        return when (entitySort) {
            "name_asc" -> filtered.sortedBy { (it.friendlyName ?: it.entity_id).lowercase(Locale.getDefault()) }
            "name_desc" -> filtered.sortedByDescending { (it.friendlyName ?: it.entity_id).lowercase(Locale.getDefault()) }
            else -> filtered
        }
    }

    val climateSettingsSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        stringResource(R.string.cr_climate_entities) to { setBack ->
            ClimateSensorSection(
                climateConfig = climateConfig,
                allEntities = entities,
                onSave = { newCfg -> viewModel.updateClimateConfig(CLIMATE_PAGE_KEY, newCfg) },
                setBack = setBack
            )
        }
    val climateAppearanceSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        stringResource(R.string.cr_appearance) to { _ ->
            ClimateAppearanceSection(climateConfig) { newCfg ->
                viewModel.updateClimateConfig(CLIMATE_PAGE_KEY, newCfg)
            }
        }
    var showClimateReimport by remember { mutableStateOf(false) }
    var showClearClimate by remember { mutableStateOf(false) }
    val climateImportSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        stringResource(R.string.cr_reimport) to { _ ->
            Text(stringResource(R.string.ui_fetch_climate_entities_from_home_assistant_again_a65f837), color = LocalHKIAppColors.current.onMuted)
            Button(onClick = { showClimateReimport = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ui_re_import_climate_f7f5678))
            }
            OutlinedButton(onClick = { showClearClimate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ui_clear_climate_view_48e8a5e), color = MaterialTheme.colorScheme.error)
            }
        }

    if (showClimateReimport) {
        AlertDialog(
            onDismissRequest = { showClimateReimport = false },
            title = { Text(stringResource(R.string.ui_re_import_climate_3ed098c)) },
            text = { Text(stringResource(R.string.ui_import_entities_that_have_not_been_edited_or_remove_6edde59)) },
            confirmButton = { Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { viewModel.reimportClimate(false); showClimateReimport = false }) { Text(stringResource(R.string.ui_import_unedited_4a58143)) }
                TextButton(onClick = { viewModel.reimportClimate(true); showClimateReimport = false }) { Text(stringResource(R.string.ui_remove_edits_and_import_all_7f0b4a1), color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(onClick = { showClimateReimport = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }
    if (showClearClimate) {
        AlertDialog(
            onDismissRequest = { showClearClimate = false },
            title = { Text(stringResource(R.string.ui_clear_climate_view_9a1339c)) },
            text = { Text(stringResource(R.string.ui_this_removes_all_imported_climate_entities_from_this_view_713f511)) },
            confirmButton = { TextButton(onClick = { viewModel.clearClimateImports(); showClearClimate = false }) { Text(stringResource(R.string.ui_clear_719ea39), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showClearClimate = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        )
    }

    val pageTitle = when (page) {
        "fans" -> stringResource(R.string.ui_fans_73d38db); "purifiers" -> stringResource(R.string.ui_air_purifiers_476147b); "humidifiers" -> stringResource(R.string.ui_humidifiers_aa38e2a)
        else -> activeGroup?.let { stringResource(it.titleRes) } ?: stringResource(R.string.ui_climate_4857860)
    }
    val pageSubtitle = when (page) {
        "fans" -> stringResource(R.string.ui_air_circulation_devices_a14e95d); "purifiers" -> stringResource(R.string.ui_air_cleaning_devices_4364277); "humidifiers" -> stringResource(R.string.ui_humidity_control_devices_c7e4846)
        else -> activeGroup?.let { stringResource(it.subtitleRes) } ?: stringResource(R.string.ui_indoor_comfort_7060319)
    }
    HKIPage(
        viewModel = viewModel,
        title = pageTitle,
        subtitle = pageSubtitle,
        pageKey = CLIMATE_PAGE_KEY,
        pageSettingsTitle = stringResource(R.string.cr_climate_settings),
        extraPageSettingsSection = climateAppearanceSection,
        additionalPageSettingsSections = listOf(climateSettingsSection, climateImportSection),
        showBadgeBar = false,
        headerBar = if (page != "climate") ({
            ClimateEntitySearchBar(
                query = entitySearch,
                onQueryChange = { entitySearch = it },
                sortMode = entitySort,
                onSortModeChange = { entitySort = it }
            )
        }) else null,
        onBack = if (page != "climate") ({ page = "climate" }) else null
    ) { padding ->
        key(uiRevision) {
        when {
            activeGroup != null -> ClimateSensorDetailPage(
                group = activeGroup,
                sensors = filteredEntities(groupSensors[activeGroup.key].orEmpty()),
                weatherEntity = if (activeGroup.key == "outside") effectiveOutsideWeatherId?.let { entityById[it] } else null,
                viewModel = viewModel,
                isEditMode = isEditMode,
                onRemove = ::hideEntity,
                onRename = { renameEntity = it },
                onReorder = { from, to -> reorderClimateEntities(filteredEntities(groupSensors[activeGroup.key].orEmpty()), from, to) },
                padding = padding
            )
            page == "fans" || page == "purifiers" || page == "humidifiers" -> ClimateDeviceListPage(
                deviceType = page,
                devices = filteredEntities(when (page) {
                    "fans" -> fanEntities
                    "purifiers" -> purifierEntities
                    else -> humidifierEntities
                }),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onRemove = ::hideEntity,
                onRename = { renameEntity = it },
                onReorder = { from, to ->
                    val visible = filteredEntities(when (page) {
                        "fans" -> fanEntities
                        "purifiers" -> purifierEntities
                        else -> humidifierEntities
                    })
                    reorderClimateEntities(visible, from, to)
                },
                padding = padding,
                humidifierFans = humidifierEntities.mapNotNull { h ->
                    climateConfig.humidifierFanEntityIds[h.entity_id]?.let { fanId -> entityById[fanId]?.let { h.entity_id to it } }
                }.toMap()
            )
            else -> if (climateConfig.manualOnly && climateEntities.isEmpty() && groupSensors.values.all { it.isEmpty() } && fanEntities.isEmpty() && humidifierEntities.isEmpty()) {
                EmptyEditHint(
                    Modifier.fillMaxSize().padding(padding),
                    stringResource(R.string.cr_empty_climate_view)
                )
            } else LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current)
            ) {
                // ── hero: the house right now ─────────────────────────────────
                item {
                    ClimateHero(
                        tempSensors = groupSensors["temperature"].orEmpty(),
                        humiditySensors = groupSensors["humidity"].orEmpty(),
                        climateEntities = climateEntities,
                        fanEntities = fanEntities,
                        humidifierEntities = humidifierEntities,
                        openingState = openingState,
                        // Outside temperature: the linked weather entity's attribute wins, else the
                        // dedicated sensor list, else any legacy outside sensor with a temp class/unit.
                        outsideTemp = run {
                            val weather = effectiveOutsideWeatherId?.let { entityById[it] }
                            weather?.temperature?.toFloat() ?: run {
                                val sensors = climateConfig.outsideTemperatureIds.mapNotNull { entityById[it] }
                                    .filter { it.entity_id !in hidden }
                                    .ifEmpty { groupSensors["outside"].orEmpty().filter { it.deviceClass == "temperature" || it.unit() in setOf("°C", "°F", "℃", "℉") } }
                                sensors.mapNotNull { it.numericState() }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
                            }
                        },
                        outsideUnit = effectiveOutsideWeatherId?.let { entityById[it]?.attributes?.get("temperature_unit")?.jsonPrimitive?.contentOrNull }
                            ?: climateConfig.outsideTemperatureIds.firstOrNull()?.let { entityById[it]?.unit() }?.ifBlank { "°C" }
                            ?: "°C"
                    )
                }

                // ── tiles: sensor classes + purifier/humidifier device tabs ───
                item {
                    data class TileSpec(
                        val icon: ImageVector, val color: Color,
                        val title: String, val status: String, val pageKey: String
                    )
                    val tiles = buildList {
                        climateSensorGroups.forEach { group ->
                            val sensors = groupSensors[group.key].orEmpty()
                            // Outside mixes temp/humidity/pressure, so summarise its temperature sensors
                            // (its dedicated list, falling back to class/unit) instead of averaging unlike units.
                            val outsideWeather = if (group.key == "outside") effectiveOutsideWeatherId?.let { entityById[it] } else null
                            // A linked weather entity is graphed as its own temperature/humidity/pressure
                            // attribute cards (see ClimateSensorDetailPage), so the tile must stay visible
                            // even when no dedicated outside sensors are configured.
                            val weatherAttrCount = listOfNotNull(
                                outsideWeather?.temperature,
                                outsideWeather?.humidity,
                                outsideWeather?.pressure
                            ).size
                            if (sensors.isEmpty() && weatherAttrCount == 0) return@forEach
                            val summarySensors = if (group.key == "outside") {
                                climateConfig.outsideTemperatureIds.mapNotNull { entityById[it] }
                                    .ifEmpty { sensors.filter { it.deviceClass == "temperature" || it.unit() in setOf("°C", "°F", "℃", "℉") } }
                                    .ifEmpty { sensors }
                            } else sensors
                            // A linked weather entity provides the outside temperature directly.
                            val values = if (outsideWeather?.temperature != null) listOf(outsideWeather.temperature!!) else summarySensors.mapNotNull { it.numericState().let { v -> v?.toDouble() } }
                            val unit = outsideWeather?.attributes?.get("temperature_unit")?.jsonPrimitive?.contentOrNull ?: summarySensors.firstOrNull()?.unit() ?: ""
                            val avg = if (values.isNotEmpty()) values.average().toFloat() else null
                            val itemCount = sensors.size + weatherAttrCount
                            val countLabel = pluralStringResource(R.plurals.cr_sensor_count, itemCount, itemCount)
                            val status = if (avg == null) countLabel else stringResource(
                                R.string.cr_count_with_average,
                                countLabel,
                                formatValue(avg),
                                if (unit.isNotBlank()) " $unit" else ""
                            )
                            add(TileSpec(group.icon, group.color, stringResource(group.titleRes), status, group.key))
                        }
                        if (fanEntities.isNotEmpty()) {
                            val on = fanEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.Air, CoolBlue, stringResource(R.string.cr_climate_fans),
                                pluralStringResource(R.plurals.cr_device_count_on, fanEntities.size, fanEntities.size, on),
                                "fans"))
                        }
                        if (purifierEntities.isNotEmpty()) {
                            val on = purifierEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.Air, AirGreen, stringResource(R.string.cr_climate_air_purifiers),
                                pluralStringResource(R.plurals.cr_device_count_on, purifierEntities.size, purifierEntities.size, on),
                                "purifiers"))
                        }
                        if (humidifierEntities.isNotEmpty()) {
                            val on = humidifierEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.WaterDrop, MistCyan, stringResource(R.string.cr_climate_humidifiers),
                                pluralStringResource(R.plurals.cr_device_count_on, humidifierEntities.size, humidifierEntities.size, on),
                                "humidifiers"))
                        }
                    }
                    BoxWithConstraints(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Auto-fit: 2 tiles across when there's room for a readable ~160dp tile,
                        // dropping to 1 on narrow windows so labels never letter-wrap.
                        val tileColumns = ((maxWidth + 10.dp) / 170.dp).toInt().coerceIn(1, 2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tiles.chunked(tileColumns).forEach { rowTiles ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowTiles.forEach { t ->
                                        ClimateLiveTile(
                                            icon = t.icon, color = t.color,
                                            title = t.title, status = t.status,
                                            modifier = Modifier.weight(1f)
                                        ) { page = t.pageKey }
                                    }
                                    repeat(tileColumns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }

                // ── thermostats & air conditioners (imported automatically) ───
                if (climateEntities.isEmpty()) {
                    item {
                        ClimateSectionHeader(null)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = itemCornerShape(), color = appColors.elevated
                        ) {
                            Text(
                                stringResource(R.string.ui_no_climate_devices_found_thermostats_and_ac_units_from_2e47541),
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    item { ClimateSectionHeader("${climateEntities.size}") }
                    if (isEditMode && climateEntities.size > 1) {
                        item {
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                OutlinedButton(onClick = { showReorderDevices = true }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.SwapVert, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ui_reorder_devices_8bcc061))
                                }
                            }
                        }
                    }
                    items(count = climateDeviceRows.size, key = { row -> climateDeviceRows[row].joinToString("|") { it.entity_id } }) { rowIndex ->
                        val row = climateDeviceRows[rowIndex]
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            var used = 0
                            row.forEach { entity ->
                                val configuredWidth = climateConfig.deviceCardWidths[entity.entity_id] ?: climateConfig.defaultDeviceCardWidth
                                val style = climateConfig.defaultDeviceCardStyle
                                val width = if (style == "dial" && configuredWidth == "third") "half" else configuredWidth
                                val span = if (narrowDeviceCards) 6 else when (width) { "third" -> 2; "half" -> 3; else -> 6 }
                                used += span
                                val iconOverride = climateConfig.customIcons[entity.entity_id]
                                val cardStyle = climateConfig.defaultDeviceCardStyle
                                val isSquare = climateConfig.deviceCardShapes[entity.entity_id] == "square"
                                Box(Modifier.weight(span.toFloat())) {
                                    if (cardStyle == "dial") ThermostatDialCard(entity, viewModel, isSquare = isSquare, iconOverride = iconOverride, onCenterClick = { dialDialogEntity = entity })
                                    else ClimateDeviceCard(entity, viewModel, isSquare = isSquare, iconOverride = iconOverride)
                                    if (isEditMode) {
                                        EditSettingsButton(onClick = { renameEntity = entity }, modifier = Modifier.align(Alignment.Center))
                                        EditRemoveBadge(onClick = { hideEntity(entity.entity_id) }, modifier = Modifier.align(Alignment.TopEnd))
                                    }
                                }
                            }
                            if (used < 6) Spacer(Modifier.weight((6 - used).toFloat()))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ClimateEntitySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val sortLabel = when (sortMode) {
        "name_asc" -> stringResource(R.string.ui_name_a_z_257c1c4)
        "name_desc" -> stringResource(R.string.ui_name_z_a_daabfb6)
        else -> stringResource(R.string.ui_custom_order_a7a5984)
    }
    val summary = buildList {
        if (query.isNotBlank()) add("\"${query.trim()}\"")
        if (sortMode != "custom") add(sortLabel)
    }.joinToString(" - ").ifBlank { stringResource(R.string.cr_no_search_active, sortLabel) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(itemCornerShape())
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_search_sort_9fe6cb4), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(summary, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.cr_collapse_search else R.string.cr_expand_search
                    ),
                tint = appColors.onMuted
            )
        }
        if (expanded) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.ui_search_by_name_28abc03)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf(
                    "custom" to stringResource(R.string.cr_custom_order),
                    "name_asc" to stringResource(R.string.cr_name_a_z),
                    "name_desc" to stringResource(R.string.cr_name_z_a)
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = sortMode == value,
                        onClick = { onSortModeChange(value) },
                        label = { Text(label) },
                        shape = itemCornerShape()
                    )
                }
            }
            if (query.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { onQueryChange("") }) { Text(stringResource(R.string.ui_clear_search_67300d0)) }
                }
            }
        }
    }
}

/** Per-device settings for a climate device on the main Climate page. Control/dial style is page-wide. */
@Composable
private fun ClimateDeviceSettingsDialog(
    entity: HAEntity,
    config: HKIClimateConfig,
    onDismiss: () -> Unit,
    onSave: (HKIClimateConfig) -> Unit
) {
    var name by remember(entity) { mutableStateOf(config.customNames[entity.entity_id].orEmpty()) }
    var icon by remember(entity) { mutableStateOf(config.customIcons[entity.entity_id].orEmpty()) }
    var width by remember(entity) { mutableStateOf(config.deviceCardWidths[entity.entity_id] ?: config.defaultDeviceCardWidth) }
    var shape by remember(entity) { mutableStateOf(config.deviceCardShapes[entity.entity_id] ?: "standard") }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(entity) { mutableStateOf("identity") }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = icon,
            onDismiss = { showIconPicker = false },
            onSelect = { icon = it; showIconPicker = false }
        )
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.cr_climate_device),
                stringResource(R.string.cr_climate_device_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                tabs = listOf(
                    "identity" to stringResource(R.string.cr_identity),
                    "appearance" to stringResource(R.string.cr_appearance)
                ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "identity") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_identity_7e5a975), stringResource(R.string.ui_optional_name_and_icon_overrides_5dfd062))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ui_name_709a232)) },
                    placeholder = { Text(entity.friendlyName ?: entity.entity_id) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_card_shape_and_width_0d513f7))
                Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = shape == "standard", onClick = { shape = "standard" }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                    FilterChip(selected = shape == "square", onClick = { shape = "square" }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = config.defaultDeviceCardStyle != "dial")
                Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon.isNotBlank()) MdiIcon(icon, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) { Text(if (icon.isBlank()) stringResource(R.string.ui_choose_78b7c9f) else stringResource(R.string.ui_change_64fbd99)) }
                    if (icon.isNotBlank()) TextButton(onClick = { icon = "" }) { Text(stringResource(R.string.ui_default_808d7dc)) }
                }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = entity.entity_id
                onSave(config.copy(
                    customNames = if (name.isBlank()) config.customNames - id else config.customNames + (id to name.trim()),
                    customIcons = if (icon.isBlank()) config.customIcons - id else config.customIcons + (id to icon),
                    deviceCardStyles = config.deviceCardStyles - id,
                    deviceCardWidths = if (width == config.defaultDeviceCardWidth) config.deviceCardWidths - id else config.deviceCardWidths + (id to width),
                    deviceCardShapes = if (shape == "standard") config.deviceCardShapes - id else config.deviceCardShapes + (id to shape)
                ))
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

// ═══ HERO ══════════════════════════════════════════════════════════════════

@Composable
private fun ClimateHero(
    tempSensors: List<HAEntity>,
    humiditySensors: List<HAEntity>,
    climateEntities: List<HAEntity>,
    fanEntities: List<HAEntity>,
    humidifierEntities: List<HAEntity>,
    openingState: ClimateOpeningState,
    outsideTemp: Float? = null,
    outsideUnit: String = "°C",
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val temps = tempSensors.mapNotNull { it.numericState() }
    val hums = humiditySensors.mapNotNull { it.numericState() }
    val avgTemp = if (temps.isNotEmpty()) temps.average().toFloat() else null
    val avgOutside = outsideTemp
    val avgHum = if (hums.isNotEmpty()) hums.average().toFloat() else null
    val tempUnit = tempSensors.firstOrNull()?.unit()?.ifBlank { "°C" } ?: "°C"
    val activities = climateEntities.mapNotNull(HAEntity::climateSceneActivity)
    val heating = ClimateSceneActivity.HEATING in activities
    val cooling = ClimateSceneActivity.COOLING in activities
    val climateFanActive = ClimateSceneActivity.FAN in activities
    val drying = ClimateSceneActivity.DRYING in activities
    val defrosting = ClimateSceneActivity.DEFROSTING in activities
    val fanActive = fanEntities.any { it.state.equals("on", ignoreCase = true) } || climateFanActive
    val humidifying = humidifierEntities.any {
        it.state.equals("on", ignoreCase = true) || it.state.equals("humidifying", ignoreCase = true)
    }
    val enabledClimateCount = climateEntities.count {
        it.state.trim().lowercase(Locale.ROOT) !in setOf("off", "unavailable", "unknown")
    }
    val enabledFanCount = fanEntities.count { it.state.equals("on", ignoreCase = true) }
    val enabledHumidifierCount = humidifierEntities.count {
        it.state.equals("on", ignoreCase = true) || it.state.equals("humidifying", ignoreCase = true)
    }
    val enabledCount = enabledClimateCount + enabledFanCount + enabledHumidifierCount
    val activeCount = activities.size + enabledFanCount + enabledHumidifierCount
    val totalDeviceCount = climateEntities.size + fanEntities.size + humidifierEntities.size
    val mixedThermal = heating && cooling
    val hasReadings = temps.isNotEmpty() || hums.isNotEmpty()
    val targets = climateEntities.mapNotNull {
        it.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull?.toFloat()
    }
    val averageTarget = targets.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val targetTolerance = if (tempUnit.contains("F", ignoreCase = true)) 1f else 0.6f
    val atTarget = avgTemp != null && averageTarget != null &&
        kotlin.math.abs(avgTemp - averageTarget) <= targetTolerance
    val sceneAccent = when {
        mixedThermal -> MixedClimateViolet
        heating -> TempWarm
        cooling -> CoolBlue
        drying -> HumidBlue
        defrosting -> MixedClimateViolet
        humidifying -> HumidBlue
        fanActive -> Co2Teal
        else -> SafeClimateGreen
    }
    val statusLabel = when {
        mixedThermal -> stringResource(R.string.ui_mixed_climate_5d22eb9)
        heating -> stringResource(R.string.ui_heating_f2a3e6e)
        cooling -> stringResource(R.string.ui_cooling_5569ac4)
        drying -> stringResource(R.string.ui_drying_7ea61a9)
        defrosting -> stringResource(R.string.ui_defrosting_c3b2817)
        humidifying -> stringResource(R.string.ui_humidifying_5d5f6c5)
        fanActive -> stringResource(R.string.ui_air_circulating_273e82b)
        atTarget && enabledCount > 0 -> stringResource(R.string.ui_at_target_6a8dbb5)
        enabledCount > 0 -> stringResource(R.string.ui_systems_idle_584a6f0)
        hasReadings -> stringResource(R.string.ui_monitoring_a814345)
        else -> stringResource(R.string.ui_no_climate_data_37d45e0)
    }
    // Match the Energy scene: the illustration sits directly on the page. Only the compact data
    // pills have surfaces; there is no opaque card or wash behind the house.
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(252.dp)) {
            ClimateHouseScene(
                heating = heating,
                cooling = cooling,
                fanActive = fanActive,
                humidifying = humidifying,
                drying = drying,
                defrosting = defrosting,
                heatPumpActive = heating || cooling || climateFanActive || drying || defrosting,
                systemsEnabled = enabledCount > 0,
                hasReadings = hasReadings,
                openDoorCount = openingState.openDoors,
                openWindowCount = openingState.openWindows,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 8.dp)
            ) {
                Text(stringResource(R.string.ui_indoor_average_53abf03), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Top) {
                    Text(avgTemp?.let { "%.1f".format(locale, it) } ?: "—", style = MaterialTheme.typography.headlineMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                    if (avgTemp != null) Text(tempUnit, style = MaterialTheme.typography.labelLarge, color = sceneAccent, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
                }
                if (avgOutside != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.ui_outside_cbbef8c), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Top) {
                        Text(stringResource(R.string.ui_1f_16b5d7d).format(locale, avgOutside), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                        Text(outsideUnit, style = MaterialTheme.typography.labelSmall, color = sceneAccent, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                    }
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 8.dp),
                color = sceneAccent.copy(alpha = 0.16f), shape = itemCornerShape()
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(sceneAccent, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(statusLabel, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat(Icons.Default.WaterDrop, HumidBlue,
                avgHum?.let { "${formatValue(it)}%" } ?: "—", stringResource(R.string.cr_climate_humidity))
            HeroStat(Icons.Default.Thermostat, TempWarm,
                "${tempSensors.size}", stringResource(R.string.cr_temperature_sensors))
            HeroStat(Icons.Default.HeatPump, Co2Teal,
                stringResource(R.string.cr_active_of_total, activeCount, totalDeviceCount),
                stringResource(R.string.cr_devices_active))
        }
    }
}

private val SafeClimateGreen = Color(0xFF4CAF73)
private val MixedClimateViolet = Color(0xFF8B7CF6)

@Composable
private fun ClimateHouseScene(
    heating: Boolean,
    cooling: Boolean,
    fanActive: Boolean,
    humidifying: Boolean,
    drying: Boolean,
    defrosting: Boolean,
    heatPumpActive: Boolean,
    systemsEnabled: Boolean,
    hasReadings: Boolean,
    openDoorCount: Int,
    openWindowCount: Int,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val dark = appColors.background.luminance() < 0.5f
    val mixedThermal = heating && cooling
    val stableAccent = if (systemsEnabled || hasReadings) SafeClimateGreen else appColors.onMuted
    val accent = when {
        mixedThermal -> MixedClimateViolet
        heating -> TempWarm
        cooling -> CoolBlue
        drying -> HumidBlue
        defrosting -> MixedClimateViolet
        humidifying -> HumidBlue
        fanActive -> Co2Teal
        else -> stableAccent
    }
    val doorOpen by animateFloatAsState(
        targetValue = if (openDoorCount > 0) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = stringResource(R.string.ui_climatedooropen_026f1c8)
    )
    val windowOpen by animateFloatAsState(
        targetValue = if (openWindowCount > 0) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = stringResource(R.string.ui_climatewindowopen_9b0794a)
    )
    val infinite = rememberInfiniteTransition(label = stringResource(R.string.ui_climatehouse_9c0637a))
    val phase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = stringResource(R.string.ui_airphase_1f2d2b6))
    val pulse by infinite.animateFloat(.62f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = stringResource(R.string.ui_comfortpulse_100b63b))

    Canvas(modifier) {
        // Use a fixed design viewport and one scale for both axes. The hero is also available as a
        // widget, so this preserves the house proportions instead of stretching it with the card.
        val designWidth = 850f
        val designHeight = 620f
        val scale = min(size.width / designWidth, size.height / designHeight)
        if (scale <= 0f) return@Canvas
        // The illustration's visual center is slightly right of its coordinate center because of
        // the outdoor unit. Center that composition and let it use more of the available card.
        val originX = size.width / 2f - 470f * scale
        val originY = size.height - designHeight * scale
        fun point(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)
        fun d(value: Float) = value * scale

        val leftWall = if (dark) Color(0xFF33424B) else Color(0xFFD9E3E8)
        val rightWall = if (dark) Color(0xFF40515B) else Color(0xFFF0F4F6)
        val gableFace = if (dark) Color(0xFF394952) else Color(0xFFE7EDF0)
        val roofFront = if (dark) Color(0xFF1D2930) else Color(0xFFAAB8C0)
        val roofBack = if (dark) Color(0xFF29363E) else Color(0xFFC1CBD1)
        val glass = if (dark) Color(0xFF162B34) else Color(0xFFAFC7D2)
        val edge = if (dark) Color(0xFF82959F) else Color.White.copy(alpha = 0.82f)
        val frame = if (dark) Color(0xFF9AAAB2) else Color(0xFF687A83)
        val furniture = if (dark) Color(0xFF111D22).copy(alpha = 0.56f) else Color(0xFF61727B).copy(alpha = 0.50f)
        val equipment = if (dark) Color(0xFF435660) else Color(0xFFB8C6CD)
        val equipmentDark = if (dark) Color(0xFF101B21) else Color(0xFF667780)
        val neutralPipe = if (dark) Color(0xFF72828B) else Color(0xFF7C8C94)
        fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
        }

        val fc = point(500f, 530f)
        val leftAxis = Offset(d(-350f), d(-55f))
        val rightAxis = Offset(d(260f), d(-70f))
        val wallHeight = d(185f)
        val fcTop = fc + Offset(0f, -wallHeight)
        val leftBottom = fc + leftAxis
        val leftTop = fcTop + leftAxis
        val rightBottom = fc + rightAxis
        val rightTop = fcTop + rightAxis
        val apex = point(630f, 225f)
        val backApex = apex + leftAxis
        val backRightTop = rightTop + leftAxis
        fun leftFace(u: Float, v: Float) = fc + leftAxis * u + Offset(0f, -wallHeight * v)
        fun rightFace(u: Float, v: Float) = fc + rightAxis * u + Offset(0f, -wallHeight * v)
        fun roof(u: Float, v: Float) = fcTop + leftAxis * u + (apex - fcTop) * v

        fun pointAlong(points: List<Offset>, value: Float): Offset {
            if (points.size < 2) return points.firstOrNull() ?: Offset.Zero
            var total = 0f
            for (index in 0 until points.lastIndex) total += (points[index + 1] - points[index]).getDistance()
            if (total <= 0f) return points.first()
            var target = value.coerceIn(0f, 1f) * total
            for (index in 0 until points.lastIndex) {
                val length = (points[index + 1] - points[index]).getDistance()
                if (target <= length || index == points.lastIndex - 1) {
                    val fraction = if (length > 0f) (target / length).coerceIn(0f, 1f) else 0f
                    return points[index] + (points[index + 1] - points[index]) * fraction
                }
                target -= length
            }
            return points.last()
        }

        // A restrained state halo and broad contact shadow ground the scene without coloring the
        // whole building. Walls and roof remain neutral in every mode.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = (if (dark) 0.10f else 0.07f) * pulse), Color.Transparent),
                center = point(500f, 365f),
                radius = d(390f)
            ),
            radius = d(390f),
            center = point(500f, 365f)
        )
        drawOval(
            color = Color.Black.copy(alpha = if (dark) 0.32f else 0.10f),
            topLeft = point(72f, 492f),
            size = Size(d(770f), d(92f))
        )

        // Back roof plane first, then the walls and gable. This is the same coherent isometric
        // construction used by the Energy house, rather than a collection of unrelated polygons.
        drawPath(quad(apex, rightTop, backRightTop, backApex), roofBack)
        drawPath(quad(fc, leftBottom, leftTop, fcTop), leftWall)
        drawPath(quad(fc, rightBottom, rightTop, fcTop), rightWall)
        drawPath(Path().apply {
            moveTo(fcTop.x, fcTop.y); lineTo(apex.x, apex.y); lineTo(rightTop.x, rightTop.y); close()
        }, gableFace)
        drawLine(if (dark) Color.Black.copy(alpha = 0.25f) else frame.copy(alpha = 0.35f), leftBottom, fc, 2.dp.toPx())
        drawLine(if (dark) Color.Black.copy(alpha = 0.22f) else frame.copy(alpha = 0.28f), fc, rightBottom, 2.dp.toPx())

        val leftZoneAccent = if (mixedThermal) TempWarm else accent
        val rightZoneAccent = if (mixedThermal) CoolBlue else accent
        val roomAlpha = when {
            heating || cooling || fanActive || humidifying || drying -> if (dark) 0.20f + 0.09f * pulse else 0.13f + 0.06f * pulse
            systemsEnabled || hasReadings -> if (dark) 0.10f + 0.03f * pulse else 0.07f + 0.02f * pulse
            else -> 0f
        }
        val windowBottom = 0.19f
        val windowTop = 0.67f
        fun roomPath(u0: Float, u1: Float) = quad(
            leftFace(u0, windowBottom), leftFace(u1, windowBottom),
            leftFace(u1, windowTop), leftFace(u0, windowTop)
        )
        val livingRoom = roomPath(0.09f, 0.44f)
        val bedroom = roomPath(0.56f, 0.91f)
        drawPath(livingRoom, glass)
        drawPath(livingRoom, leftZoneAccent.copy(alpha = roomAlpha))
        drawPath(livingRoom, frame.copy(alpha = 0.76f), style = Stroke(1.4.dp.toPx()))
        drawPath(bedroom, glass)
        drawPath(bedroom, rightZoneAccent.copy(alpha = roomAlpha))
        drawPath(bedroom, frame.copy(alpha = 0.76f), style = Stroke(1.4.dp.toPx()))
        // Proper window mullions make this read as an inhabited home rather than an open box.
        listOf(0.265f, 0.735f).forEach { u ->
            drawLine(frame.copy(alpha = 0.56f), leftFace(u, windowBottom), leftFace(u, windowTop), 1.dp.toPx())
        }
        listOf(0.09f to 0.44f, 0.56f to 0.91f).forEach { (u0, u1) ->
            drawLine(
                frame.copy(alpha = 0.48f),
                leftFace(u0, 0.43f), leftFace(u1, 0.43f), 1.dp.toPx()
            )
        }
        drawLine(frame.copy(alpha = 0.46f), leftFace(0.07f, windowBottom), leftFace(0.93f, windowBottom), 1.2.dp.toPx())

        // Restrained silhouettes behind the glass suggest a living room and bedroom without
        // competing with the animated climate state.
        drawPath(quad(leftFace(0.13f, 0.21f), leftFace(0.40f, 0.21f), leftFace(0.40f, 0.31f), leftFace(0.13f, 0.31f)), furniture)
        drawPath(quad(leftFace(0.16f, 0.30f), leftFace(0.37f, 0.30f), leftFace(0.37f, 0.39f), leftFace(0.16f, 0.39f)), furniture.copy(alpha = 0.72f))
        drawLine(furniture, leftFace(0.405f, 0.22f), leftFace(0.405f, 0.39f), 1.2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(furniture.copy(alpha = 0.82f), d(7f), leftFace(0.405f, 0.41f))

        drawPath(quad(leftFace(0.60f, 0.21f), leftFace(0.86f, 0.21f), leftFace(0.86f, 0.30f), leftFace(0.60f, 0.30f)), furniture)
        drawPath(quad(leftFace(0.82f, 0.21f), leftFace(0.87f, 0.21f), leftFace(0.87f, 0.43f), leftFace(0.82f, 0.43f)), furniture.copy(alpha = 0.76f))
        drawPath(quad(leftFace(0.61f, 0.30f), leftFace(0.70f, 0.30f), leftFace(0.70f, 0.35f), leftFace(0.61f, 0.35f)), furniture.copy(alpha = 0.52f))

        // Wall thermostat on the pier between both rooms.
        val thermostat = leftFace(0.50f, 0.50f)
        drawCircle(if (dark) Color(0xFF17242B) else Color(0xFFF5F7F8), d(17f), thermostat)
        drawCircle(frame.copy(alpha = 0.75f), d(17f), thermostat, style = Stroke(1.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.9f), d(4.5f), thermostat)

        // A visible indoor fan explains the "Air circulating" state. The outdoor heat pump only
        // spins when a climate entity itself is running.
        val roomFanCenter = leftFace(0.27f, 0.56f)
        drawCircle(if (dark) Color(0xFF152229) else Color(0xFFEEF3F5), d(15f), roomFanCenter)
        drawCircle(frame.copy(alpha = 0.48f), d(15f), roomFanCenter, style = Stroke(1.dp.toPx()))
        drawCircle(frame.copy(alpha = 0.22f), d(10f), roomFanCenter, style = Stroke(0.8.dp.toPx()))
        val roomFanRotation = if (fanActive) phase * 6.283185f else 0.25f
        repeat(4) { blade ->
            val angle = roomFanRotation + blade * 6.283185f / 4f
            val direction = Offset(cos(angle), sin(angle))
            val tangent = Offset(-direction.y, direction.x)
            val start = roomFanCenter + direction * d(3f)
            val end = roomFanCenter + direction * d(10.5f) + tangent * d(2.5f)
            drawPath(
                Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(
                        start.x + tangent.x * d(1.5f), start.y + tangent.y * d(1.5f),
                        end.x - direction.x * d(3f), end.y - direction.y * d(3f),
                        end.x, end.y
                    )
                },
                (if (fanActive) Co2Teal else frame).copy(alpha = if (fanActive) 0.90f else 0.36f),
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        drawCircle(if (fanActive) Co2Teal else frame.copy(alpha = 0.52f), d(3.5f), roomFanCenter)

        // The front/gable face gets a real hinged door. A live open contact narrows and moves the
        // leaf in perspective, exposing the dark doorway instead of merely changing its color.
        val doorway = quad(
            rightFace(0.08f, 0.02f), rightFace(0.31f, 0.02f),
            rightFace(0.31f, 0.64f), rightFace(0.08f, 0.64f)
        )
        drawPath(doorway, equipmentDark.copy(alpha = 0.94f))
        val doorHingeBottom = rightFace(0.08f, 0.02f)
        val doorHingeTop = rightFace(0.08f, 0.64f)
        val doorFarU = 0.31f - 0.14f * doorOpen
        val doorOffset = Offset(d(18f * doorOpen), d(8f * doorOpen))
        val doorLeaf = quad(
            doorHingeBottom,
            rightFace(doorFarU, 0.02f) + doorOffset,
            rightFace(doorFarU, 0.64f) + doorOffset,
            doorHingeTop
        )
        drawPath(doorLeaf, if (dark) Color(0xFF26343C) else Color(0xFFB8C6CD))
        drawPath(doorLeaf, furniture.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
        val doorPane = quad(
            rightFace(0.12f, 0.43f),
            rightFace(doorFarU - 0.04f, 0.43f) + doorOffset,
            rightFace(doorFarU - 0.04f, 0.58f) + doorOffset,
            rightFace(0.12f, 0.58f)
        )
        drawPath(doorPane, glass)
        drawPath(doorPane, accent.copy(alpha = roomAlpha * 0.9f))
        drawCircle(frame, d(3.2f), rightFace(doorFarU - 0.035f, 0.28f) + doorOffset)

        val frontWindow = quad(rightFace(0.42f, 0.23f), rightFace(0.82f, 0.23f), rightFace(0.82f, 0.70f), rightFace(0.42f, 0.70f))
        drawPath(frontWindow, glass)
        drawPath(frontWindow, rightZoneAccent.copy(alpha = roomAlpha * 0.90f))
        drawPath(frontWindow, frame.copy(alpha = 0.72f), style = Stroke(1.dp.toPx()))
        drawLine(frame.copy(alpha = 0.62f), rightFace(0.62f, 0.23f), rightFace(0.62f, 0.70f), 1.dp.toPx())
        drawLine(frame.copy(alpha = 0.62f), rightFace(0.42f, 0.465f), rightFace(0.82f, 0.465f), 1.dp.toPx())
        if (windowOpen > 0.01f) {
            val openHalf = quad(
                rightFace(0.62f, 0.23f), rightFace(0.82f, 0.23f),
                rightFace(0.82f, 0.70f), rightFace(0.62f, 0.70f)
            )
            drawPath(openHalf, equipmentDark.copy(alpha = 0.78f * windowOpen))
            val windowFarU = 0.82f - 0.10f * windowOpen
            val windowOffset = Offset(d(20f * windowOpen), d(8f * windowOpen))
            val openPane = quad(
                rightFace(0.62f, 0.23f),
                rightFace(windowFarU, 0.23f) + windowOffset,
                rightFace(windowFarU, 0.70f) + windowOffset,
                rightFace(0.62f, 0.70f)
            )
            drawPath(openPane, glass.copy(alpha = 0.88f))
            drawPath(openPane, rightZoneAccent.copy(alpha = roomAlpha * 0.72f))
            drawPath(openPane, frame.copy(alpha = 0.90f), style = Stroke(1.2.dp.toPx()))
            drawLine(
                frame.copy(alpha = 0.78f),
                rightFace(0.62f, 0.465f),
                rightFace(windowFarU, 0.465f) + windowOffset,
                1.dp.toPx()
            )
        }
        val atticVent = Offset((fcTop.x + apex.x + rightTop.x) / 3f, (fcTop.y + apex.y + rightTop.y) / 3f)
        drawCircle(if (dark) Color(0xFF17242B) else Color(0xFFB7C5CC), d(16f), atticVent)
        drawCircle(frame.copy(alpha = 0.65f), d(16f), atticVent, style = Stroke(1.dp.toPx()))
        drawLine(frame.copy(alpha = 0.7f), atticVent + Offset(d(-8f), 0f), atticVent + Offset(d(8f), 0f), 1.dp.toPx())

        // Floor/ceiling vents anchor every animated air path to something physical.
        listOf(0.27f, 0.72f).forEach { u ->
            drawLine(frame.copy(alpha = 0.65f), leftFace(u - 0.055f, 0.17f), leftFace(u + 0.055f, 0.17f), 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(frame.copy(alpha = 0.50f), leftFace(u - 0.055f, 0.71f), leftFace(u + 0.055f, 0.71f), 2.dp.toPx(), cap = StrokeCap.Round)
        }

        fun drawAirColumn(centerU: Float, color: Color, rising: Boolean, seed: Float) {
            val path = Path()
            for (step in 0..24) {
                val t = step / 24f
                val v = if (rising) 0.19f + t * 0.50f else 0.70f - t * 0.50f
                val u = centerU + sin(t * 6.283185f + seed) * 0.022f
                val p = leftFace(u, v)
                if (step == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color.copy(alpha = if (dark) 0.27f else 0.21f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
            repeat(2) { bead ->
                val head = (phase + bead * 0.48f + seed * 0.07f) % 1f
                repeat(3) { trail ->
                    val t = (head - trail * 0.045f).let { if (it < 0f) it + 1f else it }
                    val v = if (rising) 0.19f + t * 0.50f else 0.70f - t * 0.50f
                    val u = centerU + sin(t * 6.283185f + seed) * 0.022f
                    drawCircle(
                        color.copy(alpha = 0.88f - trail * 0.24f),
                        d(5.5f - trail * 0.9f),
                        leftFace(u, v)
                    )
                }
            }
        }

        when {
            mixedThermal -> {
                drawAirColumn(0.27f, TempWarm, rising = true, seed = 0.3f)
                drawAirColumn(0.72f, CoolBlue, rising = false, seed = 1.4f)
            }
            heating -> {
                drawAirColumn(0.27f, TempWarm, rising = true, seed = 0.3f)
                drawAirColumn(0.72f, TempWarm, rising = true, seed = 1.4f)
            }
            cooling -> {
                drawAirColumn(0.27f, CoolBlue, rising = false, seed = 0.3f)
                drawAirColumn(0.72f, CoolBlue, rising = false, seed = 1.4f)
            }
            drying -> {
                drawAirColumn(0.27f, HumidBlue, rising = false, seed = 0.3f)
                drawAirColumn(0.72f, HumidBlue, rising = false, seed = 1.4f)
            }
            fanActive -> {
                fun drawAirStream(u0: Float, u1: Float, v: Float, seed: Float) {
                    val stream = Path()
                    for (step in 0..24) {
                        val t = step / 24f
                        val u = u0 + (u1 - u0) * t
                        val waveV = v + sin(t * 6.283185f * 1.25f + seed) * 0.018f
                        val p = leftFace(u, waveV)
                        if (step == 0) stream.moveTo(p.x, p.y) else stream.lineTo(p.x, p.y)
                    }
                    drawPath(
                        stream,
                        Co2Teal.copy(alpha = if (dark) 0.30f else 0.23f),
                        style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val head = (phase + seed * 0.11f) % 1f
                    repeat(3) { trail ->
                        val t = (head - trail * 0.055f).let { if (it < 0f) it + 1f else it }
                        val u = u0 + (u1 - u0) * t
                        val waveV = v + sin(t * 6.283185f * 1.25f + seed) * 0.018f
                        drawCircle(
                            Co2Teal.copy(alpha = 0.88f - trail * 0.24f),
                            d(5.0f - trail * 0.8f),
                            leftFace(u, waveV)
                        )
                    }
                }
                drawAirStream(0.39f, 0.13f, 0.36f, 0.2f)
                drawAirStream(0.86f, 0.60f, 0.36f, 1.1f)
            }
        }

        // A small indoor humidifier creates mist only while that device is actually active.
        val humidifierBody = quad(leftFace(0.73f, 0.14f), leftFace(0.86f, 0.14f), leftFace(0.86f, 0.28f), leftFace(0.73f, 0.28f))
        drawPath(humidifierBody, if (dark) Color(0xFF38505A) else Color(0xFF91A9B3))
        drawLine(frame.copy(alpha = 0.55f), leftFace(0.75f, 0.25f), leftFace(0.84f, 0.25f), 1.dp.toPx())
        if (humidifying) {
            repeat(3) { mist ->
                val t = (phase * 0.72f + mist / 3f) % 1f
                val u = 0.765f + mist * 0.035f + sin(t * 6.283185f + mist) * 0.008f
                val v = 0.29f + t * 0.29f
                drawCircle(HumidBlue.copy(alpha = (1f - t) * 0.72f), d(7.5f - t * 2f), leftFace(u, v))
            }
        }

        // Pitched roof, seams, generous eaves and a small chimney finish the house silhouette.
        drawPath(quad(roof(-0.035f, -0.05f), roof(1.035f, -0.05f), roof(1.035f, 1.04f), roof(-0.035f, 1.04f)), roofFront)
        listOf(0.25f, 0.50f, 0.75f).forEach { u ->
            drawLine(edge.copy(alpha = 0.16f), roof(u, 0.02f), roof(u, 0.98f), 0.8.dp.toPx())
        }
        drawLine(edge, roof(-0.035f, -0.05f), roof(-0.035f, 1.04f), 1.5.dp.toPx())
        drawLine(edge.copy(alpha = 0.85f), roof(-0.035f, 1.04f), roof(1.035f, 1.04f), 1.2.dp.toPx())

        val chimneyBase = roof(0.67f, 0.61f)
        val chimneyLeft = chimneyBase + Offset(d(-16f), 0f)
        val chimneyRight = chimneyBase + Offset(d(15f), d(2f))
        val chimneyUp = Offset(0f, d(-48f))
        drawPath(quad(chimneyLeft, chimneyRight, chimneyRight + chimneyUp, chimneyLeft + chimneyUp), if (dark) Color(0xFF3C4B54) else Color(0xFF8798A1))
        drawPath(
            quad(chimneyLeft + chimneyUp, chimneyRight + chimneyUp, chimneyRight + chimneyUp + Offset(d(-8f), d(-6f)), chimneyLeft + chimneyUp + Offset(d(-8f), d(-6f))),
            if (dark) Color(0xFF56656D) else Color(0xFF71828B)
        )

        // Properly mounted outdoor heat-pump: feet, grille, five curved blades, status LED and
        // twin insulated pipes visibly connect it to the house.
        val unitTopLeft = point(748f, 432f)
        val unitSize = Size(d(112f), d(92f))
        val wallPipe = rightFace(0.70f, 0.17f)
        val pipePoints = listOf(
            unitTopLeft + Offset(0f, d(49f)),
            point(730f, 481f),
            point(730f, 445f),
            wallPipe
        )
        val pipePath = Path().apply {
            moveTo(pipePoints.first().x, pipePoints.first().y)
            pipePoints.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(pipePath, neutralPipe.copy(alpha = 0.86f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        val secondPipe = pipePoints.map { it + Offset(0f, d(7f)) }
        val secondPipePath = Path().apply {
            moveTo(secondPipe.first().x, secondPipe.first().y)
            secondPipe.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(secondPipePath, neutralPipe.copy(alpha = 0.58f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        if (heatPumpActive) {
            repeat(2) { bead ->
                val head = (phase + bead * 0.50f) % 1f
                repeat(3) { trail ->
                    val t = (head - trail * 0.05f).coerceAtLeast(0f)
                    drawCircle(accent.copy(alpha = 0.86f - trail * 0.24f), d(5.3f - trail * 0.8f), pointAlong(pipePoints, t))
                }
            }
        }

        drawOval(Color.Black.copy(alpha = if (dark) 0.24f else 0.09f), point(742f, 515f), Size(d(128f), d(28f)))
        drawRoundRect(
            color = equipment,
            topLeft = unitTopLeft,
            size = unitSize,
            cornerRadius = CornerRadius(d(15f), d(15f))
        )
        drawRoundRect(
            color = frame.copy(alpha = 0.34f),
            topLeft = unitTopLeft,
            size = unitSize,
            cornerRadius = CornerRadius(d(15f), d(15f)),
            style = Stroke(1.dp.toPx())
        )
        drawLine(equipmentDark.copy(alpha = 0.70f), unitTopLeft + Offset(d(18f), unitSize.height), unitTopLeft + Offset(d(18f), unitSize.height + d(12f)), 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(equipmentDark.copy(alpha = 0.70f), unitTopLeft + Offset(unitSize.width - d(18f), unitSize.height), unitTopLeft + Offset(unitSize.width - d(18f), unitSize.height + d(12f)), 3.dp.toPx(), cap = StrokeCap.Round)

        val unitCenter = unitTopLeft + Offset(unitSize.width * 0.52f, unitSize.height * 0.53f)
        val fanRadius = d(32f)
        drawCircle(equipmentDark.copy(alpha = 0.62f), fanRadius, unitCenter)
        drawCircle(frame.copy(alpha = 0.42f), fanRadius, unitCenter, style = Stroke(1.dp.toPx()))
        drawCircle(frame.copy(alpha = 0.28f), fanRadius * 0.72f, unitCenter, style = Stroke(1.dp.toPx()))
        val rotation = if (heatPumpActive) phase * 6.283185f else 0.35f
        repeat(5) { blade ->
            val angle = rotation + blade * (6.283185f / 5f)
            val direction = Offset(cos(angle), sin(angle))
            val tangent = Offset(-direction.y, direction.x)
            val start = unitCenter + direction * (fanRadius * 0.18f)
            val end = unitCenter + direction * (fanRadius * 0.70f) + tangent * (fanRadius * 0.14f)
            val bladePath = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    start.x + tangent.x * fanRadius * 0.08f,
                    start.y + tangent.y * fanRadius * 0.08f,
                    end.x - direction.x * fanRadius * 0.20f,
                    end.y - direction.y * fanRadius * 0.20f,
                    end.x,
                    end.y
                )
            }
            drawPath(
                bladePath,
                (if (heatPumpActive) accent else frame).copy(alpha = if (heatPumpActive) 0.88f else 0.38f),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        drawCircle(if (heatPumpActive) accent.copy(alpha = 0.45f + 0.55f * pulse) else frame.copy(alpha = 0.35f), d(4f), unitTopLeft + Offset(unitSize.width - d(13f), d(13f)))
        drawCircle(equipmentDark, d(6f), unitCenter)
    }
}

@Composable
private fun HeroStat(icon: ImageVector, color: Color, value: String, label: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, softWrap = false)
    }
}

// ═══ CLIMATE DEVICE CARD ═══════════════════════════════════════════════════

private fun hvacModeIcon(mode: String): ImageVector = when (mode) {
    "off" -> Icons.Default.PowerSettingsNew
    "heat" -> Icons.Default.LocalFireDepartment
    "cool" -> Icons.Default.AcUnit
    "heat_cool" -> Icons.Default.SwapVert
    "auto" -> Icons.Default.Autorenew
    "dry" -> Icons.Default.WaterDrop
    "fan_only" -> Icons.Default.Air
    else -> Icons.Default.Thermostat
}

@Composable
private fun ClimateDeviceCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = LocalItemCornerRadius.current, isSquare: Boolean = false, iconOverride: String? = null) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
    var localMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { localMode = entity.state }
    val actionColor = hvacColor(if (localMode != entity.state) localMode else hvacAction ?: entity.state)
    val currentTemp = entity.attributes?.get("current_temperature")?.jsonPrimitive?.doubleOrNull
    val targetTemp = entity.temperature
    val minTemp = (entity.attributes?.get("min_temp")?.jsonPrimitive?.doubleOrNull ?: 7.0).toFloat()
    val maxTemp = (entity.attributes?.get("max_temp")?.jsonPrimitive?.doubleOrNull ?: 35.0).toFloat()
    val step = (entity.attributes?.get("target_temp_step")?.jsonPrimitive?.doubleOrNull ?: 0.5).toFloat()
    val currentHumidity = entity.currentHumidity
    val presetMode = entity.attributes?.get("preset_mode")?.jsonPrimitive?.contentOrNull
    val presetModes = entity.fanPresetModes // same "preset_modes" attribute as fans

    // Optimistic target: the stepper responds instantly, HA state catches up on refresh.
    var localTarget by remember(targetTemp) { mutableFloatStateOf((targetTemp ?: 21.0).toFloat()) }
    var expanded by rememberSaveable(entity.entity_id) { mutableStateOf(false) }
    val hasExtras = entity.fanModes.isNotEmpty() || entity.swingModes.isNotEmpty() ||
        entity.swingHorizontalModes.isNotEmpty() || presetModes.isNotEmpty()

    val statusText = buildString {
        append(localizedClimateModeLabel(if (localMode != entity.state) localMode else hvacAction ?: entity.state))
        presetMode?.takeIf { it != "none" }?.let {
            append(stringResource(R.string.ui_text_be10035, localizedClimateModeLabel(it)))
        }
        currentHumidity?.let { append(stringResource(R.string.ui_text_4ad598c, it.toInt())) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(if (isSquare) Modifier.aspectRatio(1f) else Modifier)
            .background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)),
        shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent
    ) {
        Column(Modifier.padding(if (isSquare) 12.dp else 16.dp)) {
            if (isSquare) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(30.dp).background(actionColor.copy(alpha = 0.16f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconSlug = iconOverride?.takeUnless { it.isBlank() }
                        if (iconSlug != null) MdiIcon(iconSlug, tint = actionColor, size = 18.dp)
                        else Icon(hvacModeIcon(localMode), null, tint = actionColor, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            entity.friendlyName ?: entity.entity_id,
                            style = MaterialTheme.typography.labelLarge,
                            color = appColors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (currentTemp != null) stringResource(R.string.ui_current_88a0a02, "%.1f".format(locale, currentTemp)) else statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = actionColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (targetTemp != null && entity.state != "off") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompactTempStepButton(Icons.Default.Remove, enabled = localTarget > minTemp) {
                            localTarget = (localTarget - step).coerceAtLeast(minTemp)
                            viewModel.setClimateTemp(entity.entity_id, localTarget)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.ui_text_7f1f581, "%.1f".format(locale, localTarget)),
                                style = MaterialTheme.typography.titleLarge,
                                color = actionColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.ui_target_61ad50a), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                        }
                        CompactTempStepButton(Icons.Default.Add, enabled = localTarget < maxTemp) {
                            localTarget = (localTarget + step).coerceAtMost(maxTemp)
                            viewModel.setClimateTemp(entity.entity_id, localTarget)
                        }
                    }
                } else {
                    Text(
                        statusText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = actionColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
            // Header: name, live status, current temperature
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(actionColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconSlug = iconOverride?.takeUnless { it.isBlank() }
                    if (iconSlug != null) MdiIcon(iconSlug, tint = actionColor, size = 22.dp)
                    else Icon(hvacModeIcon(localMode), null, tint = actionColor, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entity.friendlyName ?: entity.entity_id,
                        style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = actionColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (currentTemp != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.ui_1f_b07a377).format(locale, currentTemp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = appColors.onSurface, fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.ui_current_4fc0e2b), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    }
                }
            }

            // Target temperature stepper (hidden when off or the device has no setpoint)
            if (targetTemp != null && entity.state != "off") {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TempStepButton(Icons.Default.Remove, enabled = localTarget > minTemp) {
                        localTarget = (localTarget - step).coerceAtLeast(minTemp)
                        viewModel.setClimateTemp(entity.entity_id, localTarget)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(min = 110.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                stringResource(R.string.ui_1f_16b5d7d).format(locale, localTarget),
                                style = MaterialTheme.typography.displaySmall,
                                color = actionColor, fontWeight = FontWeight.Bold
                            )
                            Text("°", style = MaterialTheme.typography.titleLarge, color = actionColor,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                        Text(stringResource(R.string.ui_target_61ad50a), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    }
                    TempStepButton(Icons.Default.Add, enabled = localTarget < maxTemp) {
                        localTarget = (localTarget + step).coerceAtMost(maxTemp)
                        viewModel.setClimateTemp(entity.entity_id, localTarget)
                    }
                }
            }

            // HVAC mode pills, centered; scrolls when there are more than fit.
            if (entity.hvacModes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HvacModePillsRow(entity, viewModel, localMode) { localMode = it }
            }

            // Fan / swing / preset modes, collapsed behind a "More" toggle
            if (hasExtras) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (expanded) stringResource(R.string.ui_less_526cb74) else stringResource(R.string.ui_fan_modes_fbaa3df),
                        style = MaterialTheme.typography.labelMedium, color = appColors.onMuted
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = appColors.onMuted, modifier = Modifier.size(18.dp)
                    )
                }
                if (expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (presetModes.isNotEmpty()) {
                            ClimateChipGroup(stringResource(R.string.cr_climate_preset), presetModes, presetMode, centered = true) { mode ->
                                viewModel.callService(
                                    "climate", "set_preset_mode",
                                    HAServiceCall(entity_id = entity.entity_id, preset_mode = mode)
                                )
                            }
                        }
                        if (entity.fanModes.isNotEmpty()) {
                            ClimateChipGroup(stringResource(R.string.cr_climate_fan), entity.fanModes, entity.fanMode) {
                                viewModel.setClimateFanMode(entity.entity_id, it)
                            }
                        }
                        if (entity.swingModes.isNotEmpty()) {
                            ClimateChipGroup(stringResource(R.string.cr_climate_vertical_airflow), entity.swingModes, entity.swingMode) {
                                viewModel.setClimateSwingMode(entity.entity_id, it)
                            }
                        }
                        if (entity.swingHorizontalModes.isNotEmpty()) {
                            ClimateChipGroup(stringResource(R.string.cr_climate_horizontal_airflow), entity.swingHorizontalModes, entity.swingHorizontalMode) {
                                viewModel.setClimateSwingHorizontalMode(entity.entity_id, it)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// ═══ THERMOSTAT DIAL (Nest-style ring, used by the "dial" climate card) ══════

private fun dimmed(color: Color, factor: Float): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, 1f)

@Composable
private fun ThermostatDialCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = LocalItemCornerRadius.current, isSquare: Boolean = false, iconOverride: String? = null, onCenterClick: (() -> Unit)? = null) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val weatherEntity by viewModel.weather.collectAsState()

    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
    var localMode by remember(entity.entity_id) { mutableStateOf(entity.state) }
    LaunchedEffect(entity.state) { localMode = entity.state }
    val optimisticMode = if (localMode != entity.state) localMode else hvacAction ?: entity.state
    val accent = hvacColor(optimisticMode)
    val accentGradient = hvacGradient(optimisticMode)
    val statusLabel = optimisticMode.replace('_', ' ').uppercase(locale)
    val currentTemp = entity.attributes?.get("current_temperature")?.jsonPrimitive?.doubleOrNull
    val outdoorTemp = weatherEntity?.attributes?.get("temperature")?.jsonPrimitive?.doubleOrNull
    val minTemp = (entity.attributes?.get("min_temp")?.jsonPrimitive?.doubleOrNull ?: 7.0).toFloat()
    val maxTemp = (entity.attributes?.get("max_temp")?.jsonPrimitive?.doubleOrNull ?: 35.0).toFloat()
    val step = (entity.attributes?.get("target_temp_step")?.jsonPrimitive?.doubleOrNull ?: 0.5).toFloat()
    val targetTemp = entity.temperature
    val hasTarget = targetTemp != null && entity.state != "off"

    // Optimistic target: the dial responds instantly, HA state catches up on refresh.
    var localTarget by remember(targetTemp) { mutableFloatStateOf((targetTemp ?: 21.0).toFloat()) }
    var modesOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().then(if (isSquare) Modifier.aspectRatio(1f) else Modifier)
            .background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)),
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box(if (isSquare) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
        Column(
            modifier = if (isSquare) Modifier.fillMaxSize().padding(12.dp) else Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isSquare) Arrangement.Center else Arrangement.Top
        ) {
            // Header: device name, then indoor / outdoor readings like the screenshot. Hidden in the
            // compact square variant, where only the dial (which carries its own status/temp) shows.
            if (!isSquare) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    iconOverride?.takeUnless { it.isBlank() }?.let { MdiIcon(it, tint = accent, size = 20.dp) }
                    Text(
                        entity.friendlyName ?: entity.entity_id,
                        style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (currentTemp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            MdiIcon("home-thermometer-outline", tint = appColors.onMuted, size = 17.dp)
                            Text(stringResource(R.string.ui_1f_c_5b83308).format(locale, currentTemp),
                                style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                        }
                    }
                    if (outdoorTemp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            MdiIcon("thermometer", tint = appColors.onMuted, size = 17.dp)
                            Text(stringResource(R.string.ui_1f_c_5b83308).format(locale, outdoorTemp),
                                style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val dialSize = if (maxWidth < 300.dp) maxWidth else 300.dp
                val ringWidth = dialSize * 0.17f
                // Scale the inner-face fonts with the dial (1.0 at the 300dp reference size) so the
                // big target value can't overflow the face and overlap the status/control zone on
                // small half-width or dense-grid cards.
                val dialScale = (dialSize.value / 300f).coerceIn(0.5f, 1f)
                Box(Modifier.size(dialSize)) {
                    // Ring: 270° range arc (active portion in the mode color, rest muted) plus a
                    // darker 90° control zone at the bottom holding the − / + buttons.
                    // Dragging along the ring sets the target, HomeKit-style; committed on release.
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(entity.entity_id, hasTarget, minTemp, maxTemp, step) {
                                if (!hasTarget) return@pointerInput
                                var dragging = false
                                fun targetFor(pos: Offset): Float {
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    var a = Math.toDegrees(
                                        atan2((pos.y - c.y).toDouble(), (pos.x - c.x).toDouble())
                                    ).toFloat()
                                    if (a < 0f) a += 360f
                                    // Dial spans 135 to 405 degrees; the bottom 45-to-135-degree zone clamps to the ends.
                                    val sweep = when {
                                        a >= 135f -> a - 135f
                                        a <= 45f -> a + 225f
                                        a < 90f -> 270f
                                        else -> 0f
                                    }
                                    val raw = minTemp + (sweep / 270f).coerceIn(0f, 1f) * (maxTemp - minTemp)
                                    return ((raw / step).roundToInt() * step).coerceIn(minTemp, maxTemp)
                                }
                                detectDragGestures(
                                    onDragStart = { pos ->
                                        val c = Offset(size.width / 2f, size.height / 2f)
                                        val distance = (pos - c).getDistance()
                                        val outer = minOf(size.width, size.height) / 2f
                                        val strokePx = ringWidth.toPx()
                                        // Only drags that start on the ring band move the target.
                                        dragging = distance >= outer - strokePx * 1.6f && distance <= outer + strokePx * 0.5f
                                        if (dragging) localTarget = targetFor(pos)
                                    },
                                    onDrag = { change, _ ->
                                        if (dragging) {
                                            change.consume()
                                            localTarget = targetFor(change.position)
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragging) viewModel.setClimateTemp(entity.entity_id, localTarget)
                                        dragging = false
                                    },
                                    onDragCancel = { dragging = false }
                                )
                            }
                    ) {
                        val strokePx = ringWidth.toPx()
                        val inset = strokePx / 2f
                        val arcSize = Size(size.width - strokePx, size.height - strokePx)
                        val topLeft = Offset(inset, inset)
                        val stroke = Stroke(width = strokePx)
                        // Range track (135° → 405°)
                        drawArc(
                            color = appColors.onMuted.copy(alpha = 0.18f),
                            startAngle = 135f, sweepAngle = 270f, useCenter = false,
                            topLeft = topLeft, size = arcSize, style = stroke
                        )
                        // Active portion up to the target temperature, with a drag handle at its end.
                        if (hasTarget) {
                            val fraction = ((localTarget - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                            drawArc(
                                brush = accentGradient,
                                startAngle = 135f, sweepAngle = 270f * fraction, useCenter = false,
                                topLeft = topLeft, size = arcSize, style = stroke
                            )
                            val handleAngle = Math.toRadians((135f + 270f * fraction).toDouble())
                            val handleRadius = (size.minDimension - strokePx) / 2f
                            val handleCenter = Offset(
                                size.width / 2f + (handleRadius * cos(handleAngle)).toFloat(),
                                size.height / 2f + (handleRadius * sin(handleAngle)).toFloat()
                            )
                            drawCircle(Color.White, radius = strokePx * 0.36f, center = handleCenter)
                            drawCircle(accent, radius = strokePx * 0.22f, center = handleCenter)
                        }
                        // Bottom control zone (45° → 135°)
                        drawArc(
                            color = if (hasTarget) dimmed(accent, 0.62f) else appColors.onMuted.copy(alpha = 0.45f),
                            startAngle = 45f, sweepAngle = 90f, useCenter = false,
                            topLeft = topLeft, size = arcSize, style = stroke
                        )
                        // Divider between − and +
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(size.width / 2f, size.height - strokePx),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Inner face: status + big target temperature.
                    Surface(
                        modifier = Modifier.align(Alignment.Center).fillMaxSize(0.58f)
                            .then(if (onCenterClick != null) Modifier.clickable { onCenterClick() } else Modifier),
                        shape = CircleShape,
                        color = appColors.surface,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Capture then scale each style by dialScale (lineHeight tied to the
                                // scaled fontSize so digits stay vertically tight inside the face).
                                val statusStyle = MaterialTheme.typography.labelMedium
                                val bigStyle = MaterialTheme.typography.displayLarge
                                val unitStyle = MaterialTheme.typography.titleSmall
                                val decStyle = MaterialTheme.typography.titleLarge
                                val fallbackStyle = MaterialTheme.typography.displayMedium
                                Text(statusLabel,
                                    style = statusStyle.copy(fontSize = statusStyle.fontSize * dialScale, lineHeight = statusStyle.fontSize * dialScale),
                                    color = appColors.onMuted, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                if (hasTarget) {
                                    val parts = "%.1f".format(Locale.US, localTarget).split(".")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(parts[0],
                                            style = bigStyle.copy(fontSize = bigStyle.fontSize * dialScale, lineHeight = bigStyle.fontSize * dialScale),
                                            color = appColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Column {
                                            Text(stringResource(R.string.ui_c_a178665),
                                                style = unitStyle.copy(fontSize = unitStyle.fontSize * dialScale, lineHeight = unitStyle.fontSize * dialScale),
                                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(stringResource(R.string.ui_text_33e0751, parts.getOrElse(1) { "0" }),
                                                style = decStyle.copy(fontSize = decStyle.fontSize * dialScale, lineHeight = decStyle.fontSize * dialScale),
                                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }
                                } else {
                                    currentTemp?.let {
                                        Text(stringResource(R.string.ui_1f_b07a377).format(locale, it),
                                            style = fallbackStyle.copy(fontSize = fallbackStyle.fontSize * dialScale, lineHeight = fallbackStyle.fontSize * dialScale),
                                            color = appColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // − / + inside the bottom control zone.
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(ringWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(ringWidth).clip(CircleShape)
                                .clickable(enabled = hasTarget && localTarget > minTemp) {
                                    localTarget = (localTarget - step).coerceAtLeast(minTemp)
                                    viewModel.setClimateTemp(entity.entity_id, localTarget)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                        Icon(
                            Icons.Default.Remove,
                            stringResource(R.string.cr_lower_target),
                            tint = Color.White,
                            modifier = Modifier.size((22.dp * dialScale).coerceAtLeast(16.dp))
                        )
                        }
                        Spacer(Modifier.width(ringWidth * 0.4f))
                        Box(
                            Modifier.size(ringWidth).clip(CircleShape)
                                .clickable(enabled = hasTarget && localTarget < maxTemp) {
                                    localTarget = (localTarget + step).coerceAtMost(maxTemp)
                                    viewModel.setClimateTemp(entity.entity_id, localTarget)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                        Icon(
                            Icons.Default.Add,
                            stringResource(R.string.cr_raise_target),
                            tint = Color.White,
                            modifier = Modifier.size((22.dp * dialScale).coerceAtLeast(16.dp))
                        )
                        }
                    }
                }
            }

            // HVAC mode pills, same as the Thermostats & AC card. Omitted in the compact square
            // variant (the dial's own mode pill still opens the full mode menu).
            if (!isSquare && entity.hvacModes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HvacModePillsRow(entity, viewModel, localMode) { localMode = it }
            }
        }

        // Mode button pinned to the card's top-right. It used to sit inside the dial face, where it
        // shifted around as the dial resized with the card; anchoring it here keeps it put.
        if (entity.hvacModes.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = appColors.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(24.dp).clip(CircleShape).clickable { modesOpen = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            hvacModeIcon(localMode), contentDescription = stringResource(R.string.ui_mode_a7b93d2),
                            tint = accent, modifier = Modifier.size(14.dp)
                        )
                    }
                }
                DropdownMenu(expanded = modesOpen, onDismissRequest = { modesOpen = false }) {
                    entity.hvacModes.forEach { mode ->
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(hvacModeIcon(mode), null, tint = hvacColor(mode), modifier = Modifier.size(18.dp))
                            },
                            text = {
                    Text(localizedClimateModeLabel(mode), fontWeight = if (mode == localMode) FontWeight.Bold else null)
                            },
                            onClick = {
                                modesOpen = false
                                localMode = mode
                                viewModel.setHvacMode(entity.entity_id, mode)
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

/** Centered, scrollable HVAC mode pills; shared by the thermostat card and the dial card. */
@Composable
private fun HvacModePillsRow(entity: HAEntity, viewModel: MainViewModel, selectedMode: String = entity.state, onModeSelected: (String) -> Unit = {}) {
    val appColors = LocalHKIAppColors.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entity.hvacModes.forEach { mode ->
                val selected = mode == selectedMode
                val modeColor = hvacColor(mode)
                Surface(
                    shape = itemCornerShape(),
                    color = if (selected) modeColor.copy(alpha = 0.22f) else appColors.subtleSurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) modeColor.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier.clip(itemCornerShape())
                        .clickable { onModeSelected(mode); viewModel.setHvacMode(entity.entity_id, mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(hvacModeIcon(mode), null,
                            tint = if (selected) modeColor else appColors.onMuted,
                            modifier = Modifier.size(16.dp))
                        Text(
                                localizedClimateModeLabel(mode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) appColors.onSurface else appColors.onMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TempStepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = CircleShape,
        color = appColors.subtleSurface,
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, null,
                tint = if (enabled) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CompactTempStepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = CircleShape,
        color = appColors.subtleSurface,
        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) appColors.onSurface else appColors.onMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun ClimateChipGroup(
    title: String, modes: List<String>, activeMode: String?, centered: Boolean = false, onSelect: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                if (centered) Alignment.CenterHorizontally else Alignment.Start
            )
        ) {
            modes.forEach { mode ->
                val selected = mode == activeMode
                Surface(
                    shape = itemCornerShape(),
                    color = if (selected) accent.copy(alpha = 0.22f) else appColors.subtleSurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) accent.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier.clip(itemCornerShape()).clickable { onSelect(mode) }
                ) {
                    Text(
                        localizedClimateModeLabel(mode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

// ═══ AIR PURIFIER / HUMIDIFIER TABS ═════════════════════════════════════════

@Composable
private fun ClimateDeviceListPage(
    deviceType: String,
    devices: List<HAEntity>,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onRename: (HAEntity) -> Unit,
    onReorder: (Int, Int) -> Unit,
    padding: PaddingValues,
    humidifierFans: Map<String, HAEntity> = emptyMap()
) {
    val appColors = LocalHKIAppColors.current
    if (isEditMode && devices.isNotEmpty()) {
        ReorderableGrid(
            items = devices,
            canReorder = true,
            onReorder = onReorder,
            key = { it.entity_id },
            columns = GridCells.Fixed(1),
            axis = ReorderAxis.Vertical,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { entity, _ ->
            Box {
                if (deviceType == "humidifiers") HumidifierCard(entity, viewModel, humidifierFans[entity.entity_id]) else FanCard(entity, viewModel)
                EditSettingsButton(onClick = { onRename(entity) }, modifier = Modifier.align(Alignment.Center))
                EditRemoveBadge(onClick = { onRemove(entity.entity_id) }, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current)
    ) {
        if (devices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = itemCornerShape(), color = appColors.elevated
                ) {
                    Text(
                        when (deviceType) {
                            "fans" -> stringResource(R.string.ui_no_fans_found_fan_entities_from_home_assistant_appear_279dab7)
                            "purifiers" -> stringResource(R.string.ui_no_air_purifiers_configured_add_fan_entities_under_climate_fd229a8)
                            else -> stringResource(R.string.ui_no_humidifiers_found_humidifier_entities_from_home_assista_eeadbd7)
                        },
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(count = devices.size, key = { devices[it].entity_id }) { idx ->
                val entity = devices[idx]
                Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    if (deviceType == "humidifiers") HumidifierCard(entity, viewModel, humidifierFans[entity.entity_id]) else FanCard(entity, viewModel)
                    if (isEditMode) {
                        EditSettingsButton(onClick = { onRename(entity) }, modifier = Modifier.align(Alignment.Center))
                        EditRemoveBadge(
                            onClick = { onRemove(entity.entity_id) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FanCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = LocalItemCornerRadius.current) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val color = if (isOn) AirGreen else appColors.onMuted
    val percentage = entity.fanPercentage
    var localPct by remember(percentage) { mutableFloatStateOf((percentage ?: 0).toFloat()) }

    Surface(modifier = Modifier.fillMaxWidth().background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)), shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Air, null, tint = color, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entity.friendlyName ?: entity.entity_id,
                        style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
            append(if (isOn) stringResource(R.string.ui_on_e0049a6) else localizedClimateModeLabel(entity.state))
            entity.fanPresetMode?.let { append(stringResource(R.string.ui_text_be10035, localizedClimateModeLabel(it))) }
                            if (isOn && percentage != null) append(stringResource(R.string.ui_text_4ad598c, percentage))
                        },
                        style = MaterialTheme.typography.bodySmall, color = color,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = isOn, onCheckedChange = { viewModel.toggleEntity(entity.entity_id) })
            }
            if (isOn && percentage != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Speed, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    HKISlider(
                        value = localPct,
                        onValueChange = { localPct = it },
                        onValueChangeFinished = { viewModel.setFanPercentage(entity.entity_id, localPct.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.ui_text_fc9db15, localPct.toInt()),
                        style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 38.dp)
                    )
                }
            }
            if (entity.fanPresetModes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ClimateChipGroup(stringResource(R.string.cr_mode), entity.fanPresetModes, entity.fanPresetMode) {
                    viewModel.setFanPresetMode(entity.entity_id, it)
                }
            }
        }
    }
}

@Composable
private fun HumidifierCard(entity: HAEntity, viewModel: MainViewModel, fanEntity: HAEntity? = null, cornerRadius: Int = LocalItemCornerRadius.current, onClick: (() -> Unit)? = null) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val isDehumidifier = entity.deviceClass == "dehumidifier"
    val color = if (isOn) MistCyan else appColors.onMuted
    val modes = entity.humidifierAvailableModes
    val target = entity.humidity  // target humidity, like climate's "temperature"
    val current = entity.currentHumidity
    val minHum = (entity.minHumidity ?: 0).toFloat()
    val maxHum = (entity.maxHumidity ?: 100).toFloat()
    var localTarget by remember(target) { mutableFloatStateOf((target ?: 50.0).toFloat()) }

    Surface(
        modifier = Modifier.fillMaxWidth()
            .background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isDehumidifier) Icons.Default.Opacity else Icons.Default.WaterDrop,
                        null, tint = color, modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entity.friendlyName ?: entity.entity_id,
                        style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
            append(if (isOn) (if (isDehumidifier) stringResource(R.string.ui_drying_7ea61a9) else stringResource(R.string.ui_humidifying_5d5f6c5)) else localizedClimateModeLabel(entity.state))
            entity.humidifierMode?.let { append(stringResource(R.string.ui_text_be10035, localizedClimateModeLabel(it))) }
                        },
                        style = MaterialTheme.typography.bodySmall, color = color,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (current != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.ui_text_fc9db15, current.toInt()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = appColors.onSurface, fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.ui_current_4fc0e2b), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    }
                }
                Switch(checked = isOn, onCheckedChange = { viewModel.toggleEntity(entity.entity_id) })
            }
            if (target != null && isOn) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TempStepButton(Icons.Default.Remove, enabled = localTarget > minHum) {
                        localTarget = (localTarget - 5f).coerceAtLeast(minHum)
                        viewModel.setHumidifierTarget(entity.entity_id, localTarget.toInt())
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(min = 110.dp)
                    ) {
                        Text(
                            stringResource(R.string.ui_text_fc9db15, localTarget.toInt()),
                            style = MaterialTheme.typography.displaySmall,
                            color = color, fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.ui_target_61ad50a), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    }
                    TempStepButton(Icons.Default.Add, enabled = localTarget < maxHum) {
                        localTarget = (localTarget + 5f).coerceAtMost(maxHum)
                        viewModel.setHumidifierTarget(entity.entity_id, localTarget.toInt())
                    }
                }
            }
            if (fanEntity != null) {
                HumidifierSpeedControl(fanEntity, viewModel)
            } else if (modes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ClimateChipGroup(stringResource(R.string.cr_mode), modes, entity.humidifierMode) {
                    viewModel.setHumidifierMode(entity.entity_id, it)
                }
            }
        }
    }
}

/** Speed control for a humidifier's linked entity — a fan (percentage slider + preset chips) or a
 *  select / input_select (option chips via select_option). Used by the card and the dialog. */
@Composable
private fun HumidifierSpeedControl(fanEntity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val domain = fanEntity.entity_id.substringBefore('.')
    if (domain == "fan") {
        val fanPct = fanEntity.fanPercentage
        if (fanPct != null) {
            var localPct by remember(fanPct) { mutableFloatStateOf(fanPct.toFloat()) }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Speed, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                HKISlider(
                    value = localPct,
                    onValueChange = { localPct = it },
                    onValueChangeFinished = { viewModel.setFanPercentage(fanEntity.entity_id, localPct.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(stringResource(R.string.ui_text_fc9db15, localPct.toInt()), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 38.dp))
            }
        }
        if (fanEntity.fanPresetModes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ClimateChipGroup(stringResource(R.string.cr_fan_speed), fanEntity.fanPresetModes, fanEntity.fanPresetMode) {
                viewModel.setFanPresetMode(fanEntity.entity_id, it)
            }
        }
    } else {
        // select / input_select: its options are the speed steps, set via select_option.
        val options = (fanEntity.attributes?.get("options") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        if (options.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            ClimateChipGroup(stringResource(R.string.cr_speed), options, fanEntity.state) {
                viewModel.callService(domain, "select_option", HAServiceCall(entity_id = fanEntity.entity_id, option = it))
            }
        }
    }
}

// ═══ SENSOR DETAIL PAGE (gradient graphs) ═══════════════════════════════════

@Composable
private fun ClimateSensorDetailPage(
    group: ClimateSensorGroup,
    sensors: List<HAEntity>,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onRename: (HAEntity) -> Unit,
    onReorder: (Int, Int) -> Unit,
    padding: PaddingValues,
    /** Optional linked weather.* entity (outside group only): graphed as separate attribute cards
     *  (temperature/humidity/pressure) instead of as a plain sensor, since its own state is a
     *  non-numeric condition string. */
    weatherEntity: HAEntity? = null
) {
    val appColors = LocalHKIAppColors.current
    var selectedHours by rememberSaveable(group.key) { mutableIntStateOf(24) }

    // Pull raw history for every sensor in the group; graphs update as results stream in. The
    // weather entity needs the unfiltered history (no significant-changes filter) since its own
    // `state` rarely changes even as the attributes we actually graph keep updating.
    LaunchedEffect(group.key, selectedHours, sensors.map { it.entity_id }.toSet(), weatherEntity?.entity_id) {
        sensors.forEach { viewModel.fetchEntityHistory(it.entity_id, selectedHours.toLong()) }
        weatherEntity?.let { viewModel.fetchEntityHistory(it.entity_id, selectedHours.toLong(), significantChangesOnly = false) }
    }

    data class WeatherAttrSpec(
        val key: String,
        @StringRes val labelRes: Int,
        val unit: String,
        val color: Color
    )
    val weatherAttrSpecs = remember(weatherEntity) {
        if (weatherEntity == null) emptyList() else buildList {
            if (weatherEntity.temperature != null) add(
                WeatherAttrSpec(
                    "temperature",
                    R.string.cr_outside_temperature,
                    weatherEntity.attributes?.get("temperature_unit")?.jsonPrimitive?.contentOrNull ?: "°C",
                    TempWarm
                )
            )
            if (weatherEntity.humidity != null) add(
                WeatherAttrSpec("humidity", R.string.cr_outside_humidity, "%", HumidBlue)
            )
            if (weatherEntity.pressure != null) add(
                WeatherAttrSpec(
                    "pressure",
                    R.string.cr_outside_pressure,
                    weatherEntity.attributes?.get("pressure_unit")?.jsonPrimitive?.contentOrNull ?: "hPa",
                    PressPurple
                )
            )
        }
    }

    val values = sensors.mapNotNull { it.numericState() }
    val unit = sensors.firstOrNull()?.unit() ?: ""

    if (isEditMode && sensors.isNotEmpty()) {
        ReorderableGrid(
            items = sensors,
            canReorder = true,
            onReorder = onReorder,
            key = { it.entity_id },
            columns = GridCells.Fixed(1),
            axis = ReorderAxis.Vertical,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { sensor, _ ->
            Box {
                EntitySensorGraphCard(
                    sensorEntity = sensor,
                    viewModel = viewModel,
                    lineColor = group.color
                )
                EditSettingsButton(onClick = { onRename(sensor) }, modifier = Modifier.align(Alignment.Center))
                EditRemoveBadge(onClick = { onRemove(sensor.entity_id) }, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current)
    ) {
        // Summary card across all sensors in the group
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(surfaceGradient(appColors.elevated), itemCornerShape()),
                shape = itemCornerShape(), color = Color.Transparent
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(34.dp).background(group.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(group.icon, null, tint = group.color, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            pluralStringResource(R.plurals.cr_sensor_count, sensors.size, sensors.size),
                            style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (values.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroStat(Icons.Default.ArrowDownward, CoolBlue,
                                "${formatValue(values.min())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_lowest))
                            HeroStat(group.icon, group.color,
                                "${formatValue(values.average().toFloat())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_average))
                            HeroStat(Icons.Default.ArrowUpward, TempWarm,
                                "${formatValue(values.max())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_highest))
                        }
                    }
                }
            }
        }

        // History window selector
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                HistoryRangeChips(selectedHours = selectedHours, onSelect = { selectedHours = it })
            }
        }

        if (weatherAttrSpecs.isNotEmpty()) {
            items(count = weatherAttrSpecs.size, key = { "weather-${weatherAttrSpecs[it].key}" }) { idx ->
                val spec = weatherAttrSpecs[idx]
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    EntitySensorGraphCard(
                        sensorEntity = weatherEntity!!,
                        viewModel = viewModel,
                        lineColor = spec.color,
                        titleOverride = stringResource(spec.labelRes),
                        attributeKey = spec.key,
                        unitOverride = spec.unit
                    )
                }
            }
        }

        if (sensors.isEmpty() && weatherAttrSpecs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = itemCornerShape(), color = appColors.elevated
                ) {
                    Text(
                        stringResource(
                            R.string.cr_no_sensors_found_for,
                            stringResource(group.titleRes).lowercase(Locale.getDefault())
                        ),
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (sensors.isNotEmpty()) {
            // One gradient graph per sensor (colors follow the sensor's device_class palette)
            items(count = sensors.size, key = { sensors[it].entity_id }) { idx ->
                val sensor = sensors[idx]
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    EntitySensorGraphCard(
                        sensorEntity = sensor,
                        viewModel = viewModel,
                        lineColor = group.color
                    )
                    if (isEditMode) {
                        EditSettingsButton(onClick = { onRename(sensor) }, modifier = Modifier.align(Alignment.Center))
                        EditRemoveBadge(
                            onClick = { onRemove(sensor.entity_id) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}

// ═══ CLIMATE SETTINGS (entity pickers, Energy-settings style) ═══════════════

@Composable
private fun ClimateAppearanceSection(
    climateConfig: HKIClimateConfig,
    onSave: (HKIClimateConfig) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var cfg by remember(climateConfig) { mutableStateOf(climateConfig) }
    Surface(modifier = Modifier.fillMaxWidth(), shape = itemCornerShape(), color = appColors.subtleSurface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.ui_all_thermostats_59c8297), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.ui_set_the_control_style_for_every_thermostat_on_this_de5cd1e),
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.ui_control_ea1d3df), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "card" to R.string.cr_climate_control,
                    "dial" to R.string.cr_thermostat_dial
                ).forEach { (value, labelRes) ->
                    FilterChip(
                        selected = cfg.defaultDeviceCardStyle == value,
                        onClick = {
                            val widths = if (value == "dial") cfg.deviceCardWidths.mapValues { (_, width) -> if (width == "third") "half" else width } else cfg.deviceCardWidths
                            cfg = cfg.copy(
                                defaultDeviceCardStyle = value,
                                defaultDeviceCardWidth = if (value == "dial" && cfg.defaultDeviceCardWidth == "third") "half" else cfg.defaultDeviceCardWidth,
                                deviceCardStyles = emptyMap(),
                                deviceCardWidths = widths
                            )
                            onSave(cfg)
                        },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
            Text(stringResource(R.string.ui_width_a58ddf5), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "third" to R.string.cr_width_third,
                    "half" to R.string.cr_width_half,
                    "full" to R.string.cr_width_full
                ).forEach { (value, labelRes) ->
                    FilterChip(
                        selected = cfg.defaultDeviceCardWidth == value,
                        enabled = value != "third" || cfg.defaultDeviceCardStyle != "dial",
                        onClick = {
                            cfg = cfg.copy(defaultDeviceCardWidth = value, deviceCardWidths = emptyMap())
                            onSave(cfg)
                        },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClimateSensorSection(
    climateConfig: HKIClimateConfig,
    allEntities: List<HAEntity>,
    onSave: (HKIClimateConfig) -> Unit,
    setBack: ((() -> Unit)?) -> Unit = {}
) {
    val appColors = LocalHKIAppColors.current
    var cfg by remember(climateConfig) { mutableStateOf(climateConfig) }
    var category by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    // Outside has three dedicated sub-lists (temperature/humidity/pressure); this holds which one the
    // picker is editing.
    var outsidePickerType by remember { mutableStateOf<String?>(null) }
    var outsideWeatherPicker by remember { mutableStateOf(false) }
    LaunchedEffect(category) { setBack(if (category != null) { { category = null } } else null) }

    fun entityName(id: String): String = allEntities.find { it.entity_id == id }?.friendlyName ?: id

    fun outsideIds(type: String): List<String> = when (type) {
        "temperature" -> cfg.outsideTemperatureIds
        "humidity" -> cfg.outsideHumidityIds
        else -> cfg.outsidePressureIds
    }
    fun saveOutsideIds(type: String, ids: List<String>) {
        cfg = when (type) {
            "temperature" -> cfg.copy(outsideTemperatureIds = ids)
            "humidity" -> cfg.copy(outsideHumidityIds = ids)
            else -> cfg.copy(outsidePressureIds = ids)
        }
        onSave(cfg)
    }

    // Optional fan linked to a humidifier (its speed options replace the humidifier's mode chips).
    var fanPickerForHumidifier by remember { mutableStateOf<String?>(null) }
    fun saveLinkedFan(humId: String, fanId: String?) {
        cfg = cfg.copy(humidifierFanEntityIds = if (fanId == null) cfg.humidifierFanEntityIds - humId else cfg.humidifierFanEntityIds + (humId to fanId))
        onSave(cfg)
    }

    // Per-category manual list accessors: which ids the picker manages and how to store them.
    fun manualIds(cat: String): List<String> = when (cat) {
        "climate" -> cfg.extraClimateIds
        "purifiers" -> cfg.purifierEntityIds
        "humidifiers" -> cfg.extraHumidifierIds
        else -> cfg.extraSensorIds[cat].orEmpty()
    }

    fun saveManualIds(cat: String, ids: List<String>) {
        cfg = when (cat) {
            "climate" -> cfg.copy(extraClimateIds = ids)
            "purifiers" -> cfg.copy(purifierEntityIds = ids)
            "humidifiers" -> cfg.copy(extraHumidifierIds = ids)
            else -> cfg.copy(extraSensorIds = cfg.extraSensorIds + (cat to ids))
        }
        onSave(cfg)
    }

    // What the picker offers per category.
    fun pickerEntities(cat: String): List<HAEntity> = when (cat) {
        "climate" -> allEntities.filter { it.entity_id.startsWith("climate.") }
        "purifiers" -> allEntities.filter { it.entity_id.startsWith("fan.") }
        "humidifiers" -> allEntities.filter { it.entity_id.startsWith("humidifier.") }
        // Extras exist to catch sensors WITHOUT the right device_class, so offer all sensors.
        else -> allEntities.filter { it.entity_id.startsWith("sensor.") }
    }

    val categoryTitles = buildMap {
        climateSensorGroups.forEach { put(it.key, stringResource(it.titleRes)) }
        put("climate", stringResource(R.string.cr_card_thermostats_ac))
        put("purifiers", stringResource(R.string.cr_climate_air_purifiers))
        put("humidifiers", stringResource(R.string.cr_climate_humidifiers))
        put("hidden", stringResource(R.string.cr_removed_entities))
    }

    if (showPicker && category != null && category != "hidden" && category != "outside") {
        val cat = category!!
        AdvancedEntitySearchDialog(
            allEntities = pickerEntities(cat),
            title = stringResource(R.string.ui_select_0fc8ec5, categoryTitles[cat].orEmpty()),
            singleSelect = false,
            preselectedIds = manualIds(cat).toSet(),
            onDismiss = { showPicker = false },
            onEntitiesSelected = { ids ->
                saveManualIds(cat, ids)
                showPicker = false
            }
        )
    }

    outsidePickerType?.let { type ->
        val outsideLabel = when (type) {
            "temperature" -> stringResource(R.string.cr_outside_temperature)
            "humidity" -> stringResource(R.string.cr_outside_humidity)
            else -> stringResource(R.string.cr_outside_air_pressure)
        }
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("sensor.") },
            title = stringResource(R.string.cr_select_item, outsideLabel),
            singleSelect = false,
            preselectedIds = outsideIds(type).toSet(),
            onDismiss = { outsidePickerType = null },
            onEntitiesSelected = { ids -> saveOutsideIds(type, ids); outsidePickerType = null }
        )
    }

    if (outsideWeatherPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("weather.") },
            title = stringResource(R.string.ui_outside_weather_entity_98e8465),
            singleSelect = true,
            preselectedIds = setOfNotNull(cfg.outsideWeatherEntityId),
            onDismiss = { outsideWeatherPicker = false },
            onEntitiesSelected = { ids -> cfg = cfg.copy(outsideWeatherEntityId = ids.firstOrNull()); onSave(cfg); outsideWeatherPicker = false }
        )
    }

    fanPickerForHumidifier?.let { humId ->
        AdvancedEntitySearchDialog(
            // A humidifier's speed source can be a fan, or a select / input_select of speed options.
            allEntities = allEntities.filter { it.entity_id.startsWith("fan.") || it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.") },
            title = stringResource(R.string.ui_link_a_speed_control_197d75a),
            singleSelect = true,
            preselectedIds = setOfNotNull(cfg.humidifierFanEntityIds[humId]),
            onDismiss = { fanPickerForHumidifier = null },
            onEntitiesSelected = { ids -> saveLinkedFan(humId, ids.firstOrNull()); fanPickerForHumidifier = null }
        )
    }

    @Composable
    fun categoryButton(key: String, title: String, subtitle: String, icon: ImageVector, color: Color) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { category = key },
            shape = itemCornerShape(),
            color = appColors.subtleSurface
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted)
            }
        }
    }

    if (category == null) {
        climateSensorGroups.forEach { group ->
            val groupTitle = stringResource(group.titleRes)
            if (group.key == "outside") {
                val count = cfg.outsideTemperatureIds.size + cfg.outsideHumidityIds.size + cfg.outsidePressureIds.size
                categoryButton(group.key, groupTitle,
                    if (count > 0) {
                        pluralStringResource(R.plurals.cr_outside_sensors_set, count, count)
                    } else {
                        stringResource(R.string.cr_set_outdoor_sensors)
                    },
                    group.icon, group.color)
            } else {
                val extraCount = cfg.extraSensorIds[group.key].orEmpty().size
                categoryButton(group.key, groupTitle,
                    if (extraCount > 0) {
                        pluralStringResource(R.plurals.cr_auto_plus_manual, extraCount, extraCount)
                    } else {
                        stringResource(R.string.cr_auto_discovered_device_class)
                    },
                    group.icon, group.color)
            }
        }
        categoryButton("climate", stringResource(R.string.cr_card_thermostats_ac),
            if (cfg.extraClimateIds.isEmpty()) {
                stringResource(R.string.cr_auto_discovered_climate_devices)
            } else {
                pluralStringResource(R.plurals.cr_auto_plus_manual, cfg.extraClimateIds.size, cfg.extraClimateIds.size)
            },
            Icons.Default.Thermostat, TempWarm)
        categoryButton("purifiers", stringResource(R.string.cr_climate_air_purifiers),
            if (cfg.purifierEntityIds.isEmpty()) {
                stringResource(R.string.cr_pick_fan_entities_as_purifiers)
            } else {
                pluralStringResource(R.plurals.cr_selected_count, cfg.purifierEntityIds.size, cfg.purifierEntityIds.size)
            },
            Icons.Default.Air, AirGreen)
        categoryButton("humidifiers", stringResource(R.string.cr_climate_humidifiers),
            if (cfg.extraHumidifierIds.isEmpty()) {
                stringResource(R.string.cr_auto_discovered_humidifier_devices)
            } else {
                pluralStringResource(R.plurals.cr_auto_plus_manual, cfg.extraHumidifierIds.size, cfg.extraHumidifierIds.size)
            },
            Icons.Default.WaterDrop, MistCyan)
        categoryButton("hidden", stringResource(R.string.cr_removed_entities),
            if (cfg.hiddenEntityIds.isEmpty()) {
                stringResource(R.string.cr_entities_removed_edit_mode)
            } else {
                pluralStringResource(R.plurals.cr_removed_count, cfg.hiddenEntityIds.size, cfg.hiddenEntityIds.size)
            },
            Icons.Default.VisibilityOff, appColors.onMuted)
        return
    }

    if (category == "hidden") {
        if (cfg.hiddenEntityIds.isEmpty()) {
            Text(
                stringResource(R.string.ui_nothing_removed_use_edit_mode_on_the_climate_page_dd07910),
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
        } else {
            Text(
                stringResource(R.string.ui_removed_entities_are_excluded_from_cards_graphs_tiles_and_628e91a),
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
            cfg.hiddenEntityIds.forEach { id ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entityName(id), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(id, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = {
                            cfg = cfg.copy(hiddenEntityIds = cfg.hiddenEntityIds - id)
                            onSave(cfg)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.cr_restore),
                            tint = appColors.onMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
        }
        return
    }

    // Outside: three dedicated sensor pickers (temperature / humidity / pressure). Never
    // auto-discovered, since we can't know which sensors are outdoors.
    if (category == "outside") {
        Text(
            stringResource(R.string.ui_outside_is_never_auto_detected_either_link_a_weather_eb8cc14),
            style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
        )
        Text(stringResource(R.string.ui_weather_entity_402cbd9), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(cfg.outsideWeatherEntityId?.let { entityName(it) } ?: stringResource(R.string.ui_none_6eef664), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = if (cfg.outsideWeatherEntityId != null) MaterialTheme.colorScheme.primary else appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { outsideWeatherPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
            if (cfg.outsideWeatherEntityId != null) TextButton(onClick = { cfg = cfg.copy(outsideWeatherEntityId = null); onSave(cfg) }) { Text(stringResource(R.string.ui_clear_719ea39)) }
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
        listOf(
            "temperature" to R.string.cr_outside_temperature,
            "humidity" to R.string.cr_outside_humidity,
            "pressure" to R.string.cr_outside_air_pressure
        ).forEach { (type, labelRes) ->
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            outsideIds(type).forEach { id ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entityName(id), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { saveOutsideIds(type, outsideIds(type) - id) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.cr_remove),
                            tint = appColors.onMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            TextButton(onClick = { outsidePickerType = type }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(if (outsideIds(type).isEmpty()) stringResource(R.string.ui_add_sensors_d6df16f) else stringResource(R.string.ui_edit_sensors_6c38d51))
            }
            HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
        }
        return
    }

    // Manual entity list for the selected category
    val cat = category!!
    Text(
        when (cat) {
            "climate" -> stringResource(R.string.ui_climate_devices_are_found_automatically_add_extra_entities_5ebd8b7)
            "purifiers" -> stringResource(R.string.ui_air_purifiers_are_fan_entities_in_home_assistant_so_3fa4fdb)
            "humidifiers" -> stringResource(R.string.ui_humidifiers_are_found_automatically_from_the_humidifier_do_0717429)
            else -> stringResource(R.string.ui_sensors_are_found_automatically_by_device_class_add_sensor_150bab5, categoryTitles[cat].orEmpty())
        },
        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
    )
    manualIds(cat).forEach { id ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(entityName(id), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { saveManualIds(cat, manualIds(cat) - id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.cr_remove),
                    tint = appColors.onMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }
    TextButton(onClick = { showPicker = true }) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (manualIds(cat).isEmpty()) stringResource(R.string.ui_add_entities_333152e) else stringResource(R.string.ui_edit_entities_485ee65))
    }

    // Per-humidifier linked fan: its speed options replace the humidifier's mode chips on the card.
    if (cat == "humidifiers") {
        val humidifiers = (allEntities.filter { it.entity_id.startsWith("humidifier.") }.map { it.entity_id } + cfg.extraHumidifierIds).distinct()
        if (humidifiers.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.ui_linked_speed_control_optional_95fd680), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
            Text(stringResource(R.string.ui_give_a_humidifier_a_fan_select_input_select_to_860be4d), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
            humidifiers.forEach { humId ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(entityName(humId), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val fanId = cfg.humidifierFanEntityIds[humId]
                        Text(fanId?.let { entityName(it) } ?: stringResource(R.string.ui_no_speed_control_linked_cb0e9ad), style = MaterialTheme.typography.bodySmall, color = if (fanId != null) MaterialTheme.colorScheme.primary else appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { fanPickerForHumidifier = humId }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    if (cfg.humidifierFanEntityIds[humId] != null) TextButton(onClick = { saveLinkedFan(humId, null) }) { Text(stringResource(R.string.ui_clear_719ea39)) }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
        }
    }
}

// ═══ SHARED BITS (Energy-page style) ════════════════════════════════════════

@Composable
private fun ClimateSectionHeader(trailing: String?) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.ui_thermostats_601da0c), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClimateLiveTile(
    icon: ImageVector, color: Color, title: String, status: String,
    modifier: Modifier = Modifier, cornerRadius: Int = LocalItemCornerRadius.current, onClick: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(34.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Climate cards as widgets: any card of the Climate view can be embedded on
// other pages, standalone or grouped in a climate stack (like the energy cards).
// ═════════════════════════════════════════════════════════════════════════════

data class ClimateCardSpec(
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val categoryRes: Int,
    val mdiIcon: String
)

val climateCardCatalog = listOf(
    ClimateCardSpec("hero", R.string.cr_card_indoor_climate_overview, R.string.cr_category_overview, "home-thermometer"),
    ClimateCardSpec("tiles", R.string.cr_card_sensor_device_tiles, R.string.cr_category_overview, "view-grid"),
    ClimateCardSpec("thermostats", R.string.cr_card_thermostats_ac, R.string.cr_category_devices, "thermostat"),
    ClimateCardSpec("dial", R.string.cr_thermostat_dial, R.string.cr_category_devices, "knob"),
    ClimateCardSpec("fans", R.string.cr_climate_fans, R.string.cr_category_devices, "fan"),
    ClimateCardSpec("purifiers", R.string.cr_climate_air_purifiers, R.string.cr_category_devices, "air-purifier"),
    ClimateCardSpec("humidifiers", R.string.cr_climate_humidifiers, R.string.cr_category_devices, "air-humidifier"),
    ClimateCardSpec("temperature", R.string.cr_card_temperature_summary, R.string.cr_category_sensors, "thermometer"),
    ClimateCardSpec("humidity", R.string.cr_card_humidity_summary, R.string.cr_category_sensors, "water-percent"),
    ClimateCardSpec("pressure", R.string.cr_card_air_pressure_summary, R.string.cr_category_sensors, "gauge"),
    ClimateCardSpec("co2", R.string.cr_card_co2_summary, R.string.cr_category_sensors, "molecule-co2"),
    ClimateCardSpec("air", R.string.cr_card_air_quality_summary, R.string.cr_category_sensors, "air-filter")
)

private data class ClimateWidgetData(
    val climateEntities: List<HAEntity>,
    val fanEntities: List<HAEntity>,
    val purifierEntities: List<HAEntity>,
    val humidifierEntities: List<HAEntity>,
    val groupSensors: Map<String, List<HAEntity>>,
    /** With a per-card override: every selected sensor.* entity, regardless of device_class.
     *  Sensor-group cards fall back to this when device-class classification finds nothing. */
    val overrideSensors: List<HAEntity> = emptyList()
)

/** With [overrideIds] the card uses exactly those entities, classified by domain/device_class. */
@Composable
private fun rememberClimateWidgetOverrideData(viewModel: MainViewModel, overrideIds: List<String>): ClimateWidgetData {
    val entityFlow = remember(viewModel, overrideIds) {
        viewModel.entitiesMatching("climate_card:${overrideIds.joinToString(",")}") { it.entity_id in overrideIds }
    }
    val entities by entityFlow.collectAsState()
    return remember(entities) {
        val fans = entities.filter { it.entity_id.startsWith("fan.") }
        ClimateWidgetData(
            climateEntities = entities.filter { it.entity_id.startsWith("climate.") },
            fanEntities = fans,
            // Fans carry no device_class, so a purifier card shows the selected fans.
            purifierEntities = fans,
            humidifierEntities = entities.filter { it.entity_id.startsWith("humidifier.") },
            groupSensors = climateSensorGroups.associate { group ->
                group.key to entities.filter { e ->
                    e.entity_id.startsWith("sensor.") && e.deviceClass in group.deviceClasses
                }
            },
            overrideSensors = entities.filter { it.entity_id.startsWith("sensor.") && it.numericState() != null }
        )
    }
}

/** Thermostat cards only need climate entities. Keeping this selector separate prevents a sensor,
 * fan, or humidifier state change from rebuilding every comparatively expensive dial widget. */
@Composable
private fun rememberClimateDeviceWidgetData(viewModel: MainViewModel): ClimateWidgetData {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val climateConfig =
        (pageConfigsMap[CLIMATE_PAGE_KEY] ?: HKIPageConfig()).climateConfig ?: HKIClimateConfig()
    val extraIds = remember(climateConfig.extraClimateIds) { climateConfig.extraClimateIds.toSet() }
    val selectorKey = remember(extraIds) {
        "climate_device_widget:${extraIds.sorted().joinToString(",")}"
    }
    val entityFlow = remember(viewModel, selectorKey) {
        viewModel.entitiesMatching(selectorKey) { entity ->
            entity.entity_id.startsWith("climate.") || entity.entity_id in extraIds
        }
    }
    val rawEntities by entityFlow.collectAsState()
    return remember(rawEntities, climateConfig) {
        val entities = rawEntities.map { it.withDisplayName(climateConfig.customNames[it.entity_id]) }
        val byId = entities.associateBy { it.entity_id }
        val hidden = climateConfig.hiddenEntityIds.toSet()
        val climateEntities = (
            (if (climateConfig.manualOnly) emptyList() else entities.filter { it.entity_id.startsWith("climate.") }) +
                climateConfig.extraClimateIds.mapNotNull(byId::get)
            )
            .distinctBy { it.entity_id }
            .filterNot { it.entity_id in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
        ClimateWidgetData(
            climateEntities = climateEntities,
            fanEntities = emptyList(),
            purifierEntities = emptyList(),
            humidifierEntities = emptyList(),
            groupSensors = emptyMap()
        )
    }
}

/** Same discovery rules as the Climate page (domain + device_class + manual config). */
@Composable
private fun rememberClimateWidgetData(viewModel: MainViewModel): ClimateWidgetData {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val climateConfig: HKIClimateConfig =
        (pageConfigsMap[CLIMATE_PAGE_KEY] ?: HKIPageConfig()).climateConfig ?: HKIClimateConfig()
    val climateDependencyIds = remember(climateConfig) {
        buildSet {
            addAll(climateConfig.extraClimateIds)
            addAll(climateConfig.extraHumidifierIds)
            addAll(climateConfig.purifierEntityIds)
            climateConfig.extraSensorIds.values.forEach(::addAll)
        }
    }
    val climateEntityFlow = remember(viewModel, climateDependencyIds) {
        viewModel.entitiesMatching { entity ->
            val domain = entity.entity_id.substringBefore('.')
            domain == "climate" || domain == "humidifier" || domain == "fan" || domain == "sensor" || entity.entity_id in climateDependencyIds
        }
    }
    val rawEntities by climateEntityFlow.collectAsState()
    return remember(rawEntities, climateConfig, entityRegistry) {
        val entities = rawEntities.map { it.withDisplayName(climateConfig.customNames[it.entity_id]) }
        val registryById = entityRegistry.associateBy { it.entity_id }
        val hidden = climateConfig.hiddenEntityIds.toSet()
        val entityById = entities.associateBy { it.entity_id }
        val climateEntities = (entities.filter { it.entity_id.startsWith("climate.") } +
            climateConfig.extraClimateIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
        val humidifierEntities = (entities.filter { it.entity_id.startsWith("humidifier.") } +
            climateConfig.extraHumidifierIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
        val fanEntities = entities.filter { it.entity_id.startsWith("fan.") }
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
        val purifierEntities = climateConfig.purifierEntityIds.mapNotNull { entityById[it] }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
        val groupSensors = climateSensorGroups.associate { group ->
            val auto = entities.filter { e ->
                e.isAutoClimateSensorFor(group.key, registryById[e.entity_id])
            }
            val extras = climateConfig.extraSensorIds[group.key].orEmpty().mapNotNull { entityById[it] }
            group.key to (auto + extras)
                .distinctBy { it.entity_id }
                .filter { it.entity_id !in hidden }
                .sortedBy { it.friendlyName ?: it.entity_id }
                .applyClimateOrder(climateConfig.entityOrder)
        }
        ClimateWidgetData(climateEntities, fanEntities, purifierEntities, humidifierEntities, groupSensors)
    }
}

/** Renders one climate card, self-contained (discovers its own entities). A non-empty
 *  [entityIdsOverride] makes the card use exactly those entities instead of the view's. */
@Composable
fun ClimateCardWidgetView(
    cardKey: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    entityIdsOverride: List<String> = emptyList(),
    isSquare: Boolean = false
) {
    val appColors = LocalHKIAppColors.current
    val data = when {
        entityIdsOverride.isNotEmpty() -> rememberClimateWidgetOverrideData(viewModel, entityIdsOverride)
        cardKey == "dial" || cardKey == "thermostats" -> rememberClimateDeviceWidgetData(viewModel)
        else -> rememberClimateWidgetData(viewModel)
    }
    val openingState = if (cardKey == "hero") rememberClimateOpeningState(viewModel)
        else ClimateOpeningState()
    var dialDialogEntityId by remember { mutableStateOf<String?>(null) }

    @Composable
    fun emptyCard(text: String) {
        Surface(modifier = modifier.fillMaxWidth().background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)), shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted, modifier = Modifier.padding(16.dp))
        }
    }

    @Composable
    fun deviceList(devices: List<HAEntity>, emptyText: String, card: @Composable (HAEntity) -> Unit) {
        if (devices.isEmpty()) { emptyCard(emptyText); return }
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            devices.forEach { card(it) }
        }
    }

    when (cardKey) {
        "hero" -> ClimateHero(
            tempSensors = data.groupSensors["temperature"].orEmpty(),
            humiditySensors = data.groupSensors["humidity"].orEmpty(),
            climateEntities = data.climateEntities,
            fanEntities = data.fanEntities,
            humidifierEntities = data.humidifierEntities,
            openingState = openingState,
            modifier = modifier.fillMaxWidth()
        )
        "tiles" -> {
            data class TileSpec(val icon: ImageVector, val color: Color, val title: String, val status: String)
            val tiles = buildList {
                climateSensorGroups.forEach { group ->
                    val sensors = data.groupSensors[group.key].orEmpty()
                    if (sensors.isEmpty()) return@forEach
                    val values = sensors.mapNotNull { it.numericState() }
                    val unit = sensors.firstOrNull()?.unit() ?: ""
                    val avg = if (values.isNotEmpty()) values.average().toFloat() else null
                    val sensorCount = pluralStringResource(R.plurals.cr_sensor_count, sensors.size, sensors.size)
                    val status = if (avg == null) {
                        sensorCount
                    } else {
                        stringResource(
                            R.string.cr_count_with_average,
                            sensorCount,
                            formatValue(avg),
                            if (unit.isNotBlank()) " $unit" else ""
                        )
                    }
                    add(TileSpec(group.icon, group.color, stringResource(group.titleRes), status))
                }
                if (data.fanEntities.isNotEmpty()) {
                    val on = data.fanEntities.count { it.state == "on" }
                    add(
                        TileSpec(
                            Icons.Default.Air,
                            CoolBlue,
                            stringResource(R.string.cr_climate_fans),
                            pluralStringResource(
                                R.plurals.cr_device_count_on,
                                data.fanEntities.size,
                                data.fanEntities.size,
                                on
                            )
                        )
                    )
                }
                if (data.purifierEntities.isNotEmpty()) {
                    val on = data.purifierEntities.count { it.state == "on" }
                    add(
                        TileSpec(
                            Icons.Default.Air,
                            AirGreen,
                            stringResource(R.string.cr_climate_air_purifiers),
                            pluralStringResource(
                                R.plurals.cr_device_count_on,
                                data.purifierEntities.size,
                                data.purifierEntities.size,
                                on
                            )
                        )
                    )
                }
                if (data.humidifierEntities.isNotEmpty()) {
                    val on = data.humidifierEntities.count { it.state == "on" }
                    add(
                        TileSpec(
                            Icons.Default.WaterDrop,
                            MistCyan,
                            stringResource(R.string.cr_climate_humidifiers),
                            pluralStringResource(
                                R.plurals.cr_device_count_on,
                                data.humidifierEntities.size,
                                data.humidifierEntities.size,
                                on
                            )
                        )
                    )
                }
            }
            if (tiles.isEmpty()) {
                emptyCard(stringResource(R.string.cr_no_climate_sensors_devices))
                return
            }
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                // Auto-fit: 2 tiles across when there's room, dropping to 1 on narrow windows.
                val tileColumns = ((maxWidth + 10.dp) / 170.dp).toInt().coerceIn(1, 2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiles.chunked(tileColumns).forEach { rowTiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowTiles.forEach { t ->
                                ClimateLiveTile(
                                    icon = t.icon, color = t.color, title = t.title, status = t.status,
                                    modifier = Modifier.weight(1f), cornerRadius = cornerRadius.coerceAtMost(18)
                                )
                            }
                            repeat(tileColumns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        "thermostats" -> deviceList(
            data.climateEntities,
            stringResource(R.string.cr_no_climate_devices)
        ) { ClimateDeviceCard(it, viewModel, cornerRadius) }
        "dial" -> deviceList(
            data.climateEntities,
            stringResource(R.string.cr_no_climate_devices)
        ) { entity ->
            ThermostatDialCard(
                entity = entity,
                viewModel = viewModel,
                cornerRadius = cornerRadius,
                isSquare = isSquare,
                onCenterClick = { dialDialogEntityId = entity.entity_id }
            )
        }
        "fans" -> deviceList(
            data.fanEntities,
            stringResource(R.string.cr_no_fans)
        ) { FanCard(it, viewModel, cornerRadius) }
        "purifiers" -> deviceList(
            data.purifierEntities,
            stringResource(R.string.cr_no_air_purifiers)
        ) { FanCard(it, viewModel, cornerRadius) }
        "humidifiers" -> deviceList(
            data.humidifierEntities,
            stringResource(R.string.cr_no_humidifiers)
        ) { HumidifierCard(it, viewModel, cornerRadius = cornerRadius) }
        else -> {
            val group = climateSensorGroups.find { it.key == cardKey }
            if (group == null) {
                emptyCard(stringResource(R.string.cr_unknown_climate_card, cardKey))
                return
            }
            // With a per-card selection, show whatever was picked even without a matching device_class.
            val sensors = data.groupSensors[group.key].orEmpty().ifEmpty { data.overrideSensors }
            val values = sensors.mapNotNull { it.numericState() }
            val unit = sensors.firstOrNull()?.unit() ?: ""
            Surface(modifier = modifier.fillMaxWidth().background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)), shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(34.dp).background(group.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(group.icon, null, tint = group.color, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(stringResource(group.titleRes), style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                            Text(
                                pluralStringResource(R.plurals.cr_sensor_count, sensors.size, sensors.size),
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                            )
                        }
                    }
                    if (values.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroStat(Icons.Default.ArrowDownward, CoolBlue,
                                "${formatValue(values.min())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_lowest))
                            HeroStat(group.icon, group.color,
                                "${formatValue(values.average().toFloat())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_average))
                            HeroStat(Icons.Default.ArrowUpward, TempWarm,
                                "${formatValue(values.max())}${if (unit.isNotBlank()) " $unit" else ""}", stringResource(R.string.cr_highest))
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(
                            R.string.cr_no_sensors_found_for,
                            stringResource(group.titleRes).lowercase(Locale.getDefault())
                        ),
                            style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                    }
                }
            }
        }
    }

    dialDialogEntityId?.let { entityId ->
        data.climateEntities.find { it.entity_id == entityId }?.let { entity ->
            PagedRoleDialog(
                role = "climate",
                entities = listOf(entity),
                viewModel = viewModel,
                onDismiss = { dialDialogEntityId = null },
                useClimateDial = true
            )
        }
    }
}

/** Standalone climate card widget with the standard edit-mode overlay. */
@Composable
fun ClimateCardWidgetItem(
    widget: HKIClimateCardWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val headerColor = LocalHKIAppColors.current.onMuted
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!widget.title.isNullOrBlank() || !widget.icon.isNullOrBlank()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!widget.icon.isNullOrBlank()) {
                        MdiIcon(widget.icon, tint = headerColor, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!widget.title.isNullOrBlank()) {
                        Text(widget.title, color = headerColor, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            ClimateCardWidgetView(widget.cardKey, viewModel, cornerRadius = widget.cornerRadius, entityIdsOverride = widget.entityIds, isSquare = widget.isSquare)
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/** A collapsible stack of climate cards. */
@Composable
fun ClimateStackWidgetItem(
    stack: HKIClimateStack,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onToggleCollapsed: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (!isWidgetVisibleNow(stack) && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val collapsed = stack.collapsible && (stack.isCollapsed ?: stack.defaultCollapsed)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                MdiIcon(stack.icon ?: "thermostat", tint = appColors.onMuted, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(stack.title ?: stringResource(R.string.ui_climate_4857860), color = appColors.onMuted,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (stack.collapsible) {
                    IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(24.dp)) {
                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!collapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stack.cardKeys.isEmpty()) {
                        Surface(shape = RoundedCornerShape(stack.cornerRadius.dp), color = appColors.elevated) {
                            Text(stringResource(R.string.ui_no_cards_yet_open_the_stack_settings_to_pick_ca8008f),
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                    stack.cardKeys.forEach { key ->
                        ClimateCardWidgetView(key, viewModel, cornerRadius = stack.cornerRadius, entityIdsOverride = stack.entityIds)
                    }
                }
            }
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/** Scrollable list over the climate card catalog, shared by the picker dialog and the embedded
 *  picker step inside AddRoomWidgetDialog. */
@Composable
fun ClimateCardPickerList(
    selected: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .heightIn(max = 420.dp)
            .fadingEdges(listState),
        state = listState
    ) {
        climateCardCatalog.groupBy { it.categoryRes }.forEach { (categoryRes, specs) ->
            item {
                Text(stringResource(categoryRes), style = MaterialTheme.typography.labelLarge,
                    color = appColors.onMuted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(specs.size) { i ->
                val spec = specs[i]
                val isSel = spec.key in selected
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(itemCornerShape())
                        .clickable { onToggle(spec.key) },
                    shape = itemCornerShape(),
                    color = appColors.subtleSurface,
                    border = if (isSel) BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.28f)) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MdiIcon(spec.mdiIcon, contentDescription = null, tint = appColors.onSurface, size = 28.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(spec.labelRes), color = appColors.onSurface,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null,
                            tint = appColors.onMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** Categorized picker over the climate card catalog. */
@Composable
fun ClimateCardPickerDialog(
    multiSelect: Boolean,
    preselected: List<String> = emptyList(),
    title: String? = null,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(preselected.toList()) }
    val resolvedTitle = title ?: stringResource(R.string.cr_select_climate_cards)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resolvedTitle) },
        text = {
            Column {
                TextButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ui_back_b52b36b))
                }
                ClimateCardPickerList(
                    selected = selected,
                    onToggle = { key ->
                        selected = when {
                            multiSelect && key in selected -> selected - key
                            multiSelect -> selected + key
                            else -> listOf(key)
                        }
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSelected(selected) }) { Text(stringResource(R.string.ui_done_e9b450d)) } }
    )
}

/** Which entities the per-card picker offers, based on what the card can display. */
private fun climateOverridePickerFilter(cardKeys: List<String>): (HAEntity) -> Boolean {
    val domains = buildSet {
        cardKeys.forEach { key ->
            when (key) {
                "thermostats", "dial" -> add("climate")
                "fans", "purifiers" -> add("fan")
                "humidifiers" -> add("humidifier")
                "temperature", "humidity", "pressure", "co2", "air" -> add("sensor")
                else -> { add("climate"); add("fan"); add("humidifier"); add("sensor") }
            }
        }
        if (isEmpty()) { add("climate"); add("fan"); add("humidifier"); add("sensor") }
    }
    return { e -> e.entity_id.substringBefore('.') in domains }
}

/** Entity picker for one climate card, offered right after adding it so the card starts with an
 *  explicit selection instead of autofilling from the Climate view. */
@Composable
fun ClimateCardEntityPickerDialog(
    cardKey: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    val allEntities by viewModel.entities.collectAsState()
    val cardLabel = climateCardCatalog.find { it.key == cardKey }
        ?.let { stringResource(it.labelRes) }
        ?: cardKey
    AdvancedEntitySearchDialog(
        allEntities = allEntities.filter(climateOverridePickerFilter(listOf(cardKey))),
        title = stringResource(R.string.cr_entities_for, cardLabel),
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = onSelected
    )
}

/** "Custom entities" rows shared by the climate card and stack settings dialogs. */
@Composable
private fun ClimateEntityOverrideSection(
    entityIds: List<String>,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_entities_f7638a2), style = MaterialTheme.typography.labelLarge)
            Text(
                if (entityIds.isEmpty()) stringResource(R.string.ui_using_the_climate_view_s_entities_8befb0f)
                else stringResource(R.string.ui_selected_for_this_card_3c4a588, entityIds.size),
                style = MaterialTheme.typography.bodySmall,
                color = if (entityIds.isEmpty()) appColors.onMuted else MaterialTheme.colorScheme.primary
            )
        }
        if (entityIds.isNotEmpty()) {
            TextButton(onClick = onReset) { Text(stringResource(R.string.ui_reset_44c57ab)) }
        }
        TextButton(onClick = onPick) { Text(stringResource(R.string.ui_change_64fbd99)) }
    }
}

@Composable
fun ClimateCardWidgetSettingsDialog(
    widget: HKIClimateCardWidget,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIClimateCardWidget) -> Unit
) {
    val allEntities by viewModel.entities.collectAsState()
    var title by remember { mutableStateOf(widget.title ?: "") }
    var width by remember { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var radius by remember { mutableIntStateOf(widget.cornerRadius) }
    var cardKey by remember { mutableStateOf(widget.cardKey) }
    var isSquare by remember { mutableStateOf(widget.isSquare) }
    var entityIds by remember { mutableStateOf(widget.entityIds) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingEntities by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("data") }
    if (showPicker) {
        ClimateCardPickerDialog(
            multiSelect = false, preselected = listOf(cardKey), title = stringResource(R.string.ui_select_climate_card_18f3b6e),
            onDismiss = { showPicker = false },
            onSelected = { sel -> sel.firstOrNull()?.let { cardKey = it }; showPicker = false }
        )
    }
    if (pickingEntities) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter(climateOverridePickerFilter(listOf(cardKey))),
            title = stringResource(R.string.ui_select_entities_affed45),
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { pickingEntities = false },
            onEntitiesSelected = { ids -> entityIds = ids; pickingEntities = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.cr_climate_card),
                stringResource(R.string.cr_climate_card_dialog_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "data" to stringResource(R.string.cr_card_and_data),
                        "appearance" to stringResource(R.string.cr_appearance)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "data") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_card_entities_e548a55), stringResource(R.string.ui_choose_the_climate_view_and_optional_entity_overrides_a69cbbc))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_card_4d4ce73), style = MaterialTheme.typography.labelLarge)
                        Text(climateCardCatalog.find { it.key == cardKey }
                            ?.let { stringResource(it.labelRes) }
                            ?: cardKey,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                }
                ClimateEntityOverrideSection(
                    entityIds = entityIds,
                    onPick = { pickingEntities = true },
                    onReset = { entityIds = emptyList() }
                )
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_optional_title_width_and_supported_shape_8b2e456))
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_optional_932fc13)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                // The thermostat dial can render as a compact 1:1 square, like the other widgets.
                if (cardKey == "dial") {
                    Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                    }
                }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(
                    cardKey = cardKey, title = title.ifBlank { null }, width = width,
                    cornerRadius = radius, entityIds = entityIds,
                    isSquare = isSquare && cardKey == "dial"
                ))
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

@Composable
fun ClimateStackSettingsDialog(
    stack: HKIClimateStack,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIClimateStack) -> Unit
) {
    val allEntities by viewModel.entities.collectAsState()
    var title by remember { mutableStateOf(stack.title ?: "") }
    var width by remember { mutableStateOf(if (stack.width == "third") "half" else stack.width) }
    var radius by remember { mutableIntStateOf(stack.cornerRadius) }
    var cardKeys by remember { mutableStateOf(stack.cardKeys) }
    var collapsible by remember { mutableStateOf(stack.collapsible) }
    var entityIds by remember { mutableStateOf(stack.entityIds) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingEntities by remember { mutableStateOf(false) }
    var settingsPage by remember(stack) { mutableStateOf("cards") }
    val selectedCardNames = cardKeys.map { key ->
        climateCardCatalog.find { it.key == key }
            ?.let { stringResource(it.labelRes) }
            ?: key
    }
    if (showPicker) {
        ClimateCardPickerDialog(
            multiSelect = true, preselected = cardKeys, title = stringResource(R.string.ui_stack_cards_8bdc343),
            onDismiss = { showPicker = false },
            onSelected = { cardKeys = it; showPicker = false }
        )
    }
    if (pickingEntities) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter(climateOverridePickerFilter(cardKeys)),
            title = stringResource(R.string.ui_select_entities_affed45),
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { pickingEntities = false },
            onEntitiesSelected = { ids -> entityIds = ids; pickingEntities = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.cr_climate_stack),
                stringResource(R.string.cr_climate_stack_dialog_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "cards" to stringResource(R.string.cr_cards_and_data),
                        "layout" to stringResource(R.string.cr_layout)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "cards") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_cards_entities_be4393a), stringResource(R.string.ui_choose_included_views_and_shared_entity_overrides_41f2e64))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_cards_0f830bc), style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (cardKeys.isEmpty()) stringResource(R.string.ui_none_selected_5798946)
                            else selectedCardNames.joinToString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                }
                ClimateEntityOverrideSection(
                    entityIds = entityIds,
                    onPick = { pickingEntities = true },
                    onReset = { entityIds = emptyList() }
                )
                }
                if (settingsPage == "layout") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_stack_layout_1623679), stringResource(R.string.ui_title_collapse_behavior_and_dashboard_width_a64a590))
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_768e0c1)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ui_collapsible_c932fac), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = collapsible, onCheckedChange = { collapsible = it })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(stack.copy(
                    title = title.ifBlank { null }, width = width, cornerRadius = radius,
                    cardKeys = cardKeys, collapsible = collapsible, entityIds = entityIds
                ))
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

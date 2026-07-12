package com.example.hki7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAServiceCall
import com.example.hki7.data.HKIClimateConfig
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.data.withDisplayName
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.RenameCardDialog
import com.example.hki7.ui.components.EntitySensorGraphCard
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.HistoryRangeChips
import com.example.hki7.ui.components.ReorderAxis
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.hvacColor
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

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
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val deviceClasses: Set<String>
)

private val climateSensorGroups = listOf(
    ClimateSensorGroup("temperature", "Temperature", "All temperature sensors",
        Icons.Default.Thermostat, TempWarm, setOf("temperature")),
    ClimateSensorGroup("humidity", "Humidity", "All humidity sensors",
        Icons.Default.WaterDrop, HumidBlue, setOf("humidity")),
    ClimateSensorGroup("pressure", "Air pressure", "All pressure sensors",
        Icons.Default.Speed, PressPurple, setOf("pressure", "atmospheric_pressure")),
    ClimateSensorGroup("co2", "CO₂", "Carbon dioxide sensors",
        Icons.Default.Co2, Co2Teal, setOf("carbon_dioxide")),
    ClimateSensorGroup("air", "Air quality", "Particulates, VOC & AQI",
        Icons.Default.Air, AirGreen, setOf(
            "pm1", "pm25", "pm10", "aqi", "volatile_organic_compounds",
            "volatile_organic_compounds_parts", "nitrogen_dioxide", "carbon_monoxide"
        ))
)

private fun HAEntity.numericState(): Float? =
    if (state == "unavailable" || state == "unknown") null else state.toFloatOrNull()

private fun HAEntity.unit(): String =
    attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""

private fun formatValue(v: Float): String =
    if (kotlin.math.abs(v) >= 100f) "%.0f".format(Locale.getDefault(), v)
    else "%.1f".format(Locale.getDefault(), v)

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
    val isEditMode by viewModel.isEditMode.collectAsState()
    // Bumped by undo/redo; keying the page content on it rebuilds the reorderable lists so a
    // restored order shows immediately instead of only after leaving edit mode.
    val uiRevision by viewModel.uiRevision.collectAsState()
    val appColors = LocalHKIAppColors.current
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
    val entities = remember(rawEntities, climateConfig.customNames) {
        rawEntities.map { it.withDisplayName(climateConfig.customNames[it.entity_id]) }
    }
    val hidden = remember(climateConfig) { climateConfig.hiddenEntityIds.toSet() }
    val entityById = remember(entities) { entities.associateBy { it.entity_id } }

    fun hideEntity(id: String) {
        viewModel.hideClimateEntity(CLIMATE_PAGE_KEY, id)
    }
    var renameEntity by remember { mutableStateOf<HAEntity?>(null) }
    renameEntity?.let { entity ->
        RenameCardDialog(
            currentName = climateConfig.customNames[entity.entity_id].orEmpty(),
            defaultName = entity.friendlyName ?: entity.entity_id,
            onDismiss = { renameEntity = null }
        ) { name ->
            val names = if (name == null) climateConfig.customNames - entity.entity_id else climateConfig.customNames + (entity.entity_id to name)
            viewModel.updateClimateConfig(CLIMATE_PAGE_KEY, climateConfig.copy(customNames = names))
            renameEntity = null
        }
    }
    fun reorderClimateEntities(visible: List<HAEntity>, from: Int, to: Int) {
        val visibleIds = visible.map { it.entity_id }.toMutableList().apply { add(to, removeAt(from)) }
        viewModel.updateClimateConfig(
            CLIMATE_PAGE_KEY,
            climateConfig.copy(entityOrder = visibleIds + climateConfig.entityOrder.filterNot { it in visibleIds })
        )
    }

    // Thermostats & air conditioners: every climate.* entity, plus manual additions, minus removed.
    val climateEntities = remember(entities, climateConfig) {
        (entities.filter { it.entity_id.startsWith("climate.") } +
            climateConfig.extraClimateIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Humidifiers/dehumidifiers: humidifier.* domain plus manual additions.
    val humidifierEntities = remember(entities, climateConfig) {
        (entities.filter { it.entity_id.startsWith("humidifier.") } +
            climateConfig.extraHumidifierIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    // Native fan.* entities are imported automatically. Air purifiers remain an optional
    // user-curated subset with their own tab.
    val fanEntities = remember(entities, climateConfig) {
        entities.filter { it.entity_id.startsWith("fan.") }
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
    val groupSensors: Map<String, List<HAEntity>> = remember(entities, climateConfig) {
        climateSensorGroups.associate { group ->
            val auto = entities.filter { e ->
                e.entity_id.startsWith("sensor.") &&
                    e.deviceClass in group.deviceClasses &&
                    e.numericState() != null
            }
            val extras = climateConfig.extraSensorIds[group.key].orEmpty().mapNotNull { entityById[it] }
            group.key to (auto + extras)
                .distinctBy { it.entity_id }
                .filter { it.entity_id !in hidden }
                .sortedBy { it.friendlyName ?: it.entity_id }
                .applyClimateOrder(climateConfig.entityOrder)
        }
    }

    var page by rememberSaveable { mutableStateOf("climate") }
    androidx.activity.compose.BackHandler(enabled = page != "climate") { page = "climate" }
    val activeGroup = climateSensorGroups.find { it.key == page }

    val climateSettingsSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Climate Entities" to { setBack ->
            ClimateSensorSection(
                viewModel = viewModel,
                climateConfig = climateConfig,
                allEntities = entities,
                onSave = { newCfg -> viewModel.updateClimateConfig(CLIMATE_PAGE_KEY, newCfg) },
                setBack = setBack
            )
        }

    val pageTitle = when (page) {
        "fans" -> "Fans"; "purifiers" -> "Air purifiers"; "humidifiers" -> "Humidifiers"
        else -> activeGroup?.title ?: "Climate"
    }
    val pageSubtitle = when (page) {
        "fans" -> "Air circulation devices"; "purifiers" -> "Air cleaning devices"; "humidifiers" -> "Humidity control devices"
        else -> activeGroup?.subtitle ?: "Indoor comfort"
    }
    HKIPage(
        viewModel = viewModel,
        title = pageTitle,
        subtitle = pageSubtitle,
        pageKey = CLIMATE_PAGE_KEY,
        pageSettingsTitle = "Climate Settings",
        extraPageSettingsSection = climateSettingsSection,
        showBadgeBar = false,
        onBack = if (page != "climate") ({ page = "climate" }) else null
    ) { padding ->
        key(uiRevision) {
        when {
            activeGroup != null -> ClimateSensorDetailPage(
                group = activeGroup,
                sensors = groupSensors[activeGroup.key].orEmpty(),
                viewModel = viewModel,
                isEditMode = isEditMode,
                onRemove = ::hideEntity,
                onRename = { renameEntity = it },
                onReorder = { from, to -> reorderClimateEntities(groupSensors[activeGroup.key].orEmpty(), from, to) },
                padding = padding
            )
            page == "fans" || page == "purifiers" || page == "humidifiers" -> ClimateDeviceListPage(
                deviceType = page,
                devices = when (page) {
                    "fans" -> fanEntities
                    "purifiers" -> purifierEntities
                    else -> humidifierEntities
                },
                viewModel = viewModel,
                isEditMode = isEditMode,
                onRemove = ::hideEntity,
                onRename = { renameEntity = it },
                onReorder = { from, to ->
                    val visible = when (page) {
                        "fans" -> fanEntities
                        "purifiers" -> purifierEntities
                        else -> humidifierEntities
                    }
                    reorderClimateEntities(visible, from, to)
                },
                padding = padding
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current)
            ) {
                // ── hero: the house right now ─────────────────────────────────
                item {
                    ClimateHero(
                        tempSensors = groupSensors["temperature"].orEmpty(),
                        humiditySensors = groupSensors["humidity"].orEmpty(),
                        climateEntities = climateEntities
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
                            if (sensors.isEmpty()) return@forEach
                            val values = sensors.mapNotNull { it.numericState() }
                            val unit = sensors.firstOrNull()?.unit() ?: ""
                            val avg = if (values.isNotEmpty()) values.average().toFloat() else null
                            val status = buildString {
                                append("${sensors.size} sensor${if (sensors.size == 1) "" else "s"}")
                                if (avg != null) append(" · avg ${formatValue(avg)}${if (unit.isNotBlank()) " $unit" else ""}")
                            }
                            add(TileSpec(group.icon, group.color, group.title, status, group.key))
                        }
                        if (fanEntities.isNotEmpty()) {
                            val on = fanEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.Air, CoolBlue, "Fans",
                                "${fanEntities.size} device${if (fanEntities.size == 1) "" else "s"} · $on on",
                                "fans"))
                        }
                        if (purifierEntities.isNotEmpty()) {
                            val on = purifierEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.Air, AirGreen, "Air purifiers",
                                "${purifierEntities.size} device${if (purifierEntities.size == 1) "" else "s"} · $on on",
                                "purifiers"))
                        }
                        if (humidifierEntities.isNotEmpty()) {
                            val on = humidifierEntities.count { it.state == "on" }
                            add(TileSpec(Icons.Default.WaterDrop, MistCyan, "Humidifiers",
                                "${humidifierEntities.size} device${if (humidifierEntities.size == 1) "" else "s"} · $on on",
                                "humidifiers"))
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tiles.chunked(2).forEach { rowTiles ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                rowTiles.forEach { t ->
                                    ClimateLiveTile(
                                        icon = t.icon, color = t.color,
                                        title = t.title, status = t.status,
                                        modifier = Modifier.weight(1f)
                                    ) { page = t.pageKey }
                                }
                                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── thermostats & air conditioners (imported automatically) ───
                if (climateEntities.isEmpty()) {
                    item {
                        ClimateSectionHeader("Thermostats", null)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp), color = appColors.elevated
                        ) {
                            Text(
                                "No climate devices found. Thermostats and AC units from Home Assistant appear here automatically.",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    item { ClimateSectionHeader("Thermostats", "${climateEntities.size}") }
                    items(count = climateEntities.size, key = { climateEntities[it].entity_id }) { idx ->
                        val entity = climateEntities[idx]
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            ClimateDeviceCard(entity, viewModel)
                            if (isEditMode) {
                                EditSettingsButton(
                                    onClick = { renameEntity = entity },
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                EditRemoveBadge(
                                    onClick = { hideEntity(entity.entity_id) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ═══ HERO ══════════════════════════════════════════════════════════════════

@Composable
private fun ClimateHero(
    tempSensors: List<HAEntity>,
    humiditySensors: List<HAEntity>,
    climateEntities: List<HAEntity>
) {
    val appColors = LocalHKIAppColors.current
    val temps = tempSensors.mapNotNull { it.numericState() }
    val hums = humiditySensors.mapNotNull { it.numericState() }
    val avgTemp = if (temps.isNotEmpty()) temps.average().toFloat() else null
    val avgHum = if (hums.isNotEmpty()) hums.average().toFloat() else null
    val tempUnit = tempSensors.firstOrNull()?.unit()?.ifBlank { "°C" } ?: "°C"
    val activeCount = climateEntities.count { it.state != "off" && it.state != "unavailable" }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp), color = appColors.elevated
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(CoolBlue.copy(alpha = 0.16f), Color.Transparent, TempWarm.copy(alpha = 0.16f))
                )
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("INDOOR CLIMATE", style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        avgTemp?.let { "%.1f".format(Locale.getDefault(), it) } ?: "—",
                        style = MaterialTheme.typography.displayMedium,
                        color = appColors.onSurface, fontWeight = FontWeight.Bold
                    )
                    if (avgTemp != null) {
                        Text(tempUnit, style = MaterialTheme.typography.titleMedium,
                            color = TempWarm, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                    }
                }
                Text("Average temperature", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HeroStat(Icons.Default.WaterDrop, HumidBlue,
                        avgHum?.let { "${formatValue(it)}%" } ?: "—", "Humidity")
                    HeroStat(Icons.Default.Thermostat, TempWarm,
                        "${tempSensors.size}", "Temp sensors")
                    HeroStat(Icons.Default.HeatPump, Co2Teal,
                        "$activeCount of ${climateEntities.size}", "Devices active")
                }
            }
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, color: Color, value: String, label: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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

private fun hvacModeLabel(mode: String): String = when (mode) {
    "heat_cool" -> "Heat/Cool"
    "fan_only" -> "Fan"
    else -> mode.replaceFirstChar(Char::uppercase)
}

@Composable
private fun ClimateDeviceCard(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val hvacAction = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
    val actionColor = hvacColor(hvacAction ?: entity.state)
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
        append((hvacAction ?: entity.state).replace('_', ' ').replaceFirstChar(Char::uppercase))
        presetMode?.takeIf { it != "none" }?.let { append(" · ${it.replaceFirstChar(Char::uppercase)}") }
        currentHumidity?.let { append(" · ${it.toInt()}%") }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), color = appColors.elevated
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: name, live status, current temperature
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(actionColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(hvacModeIcon(entity.state), null, tint = actionColor, modifier = Modifier.size(22.dp))
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
                            "%.1f°".format(locale, currentTemp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = appColors.onSurface, fontWeight = FontWeight.Bold
                        )
                        Text("Current", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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
                                "%.1f".format(locale, localTarget),
                                style = MaterialTheme.typography.displaySmall,
                                color = actionColor, fontWeight = FontWeight.Bold
                            )
                            Text("°", style = MaterialTheme.typography.titleLarge, color = actionColor,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                        Text("Target", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        entity.hvacModes.forEach { mode ->
                            val selected = mode == entity.state
                            val modeColor = hvacColor(mode)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) modeColor.copy(alpha = 0.22f) else appColors.subtleSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) modeColor.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.setHvacMode(entity.entity_id, mode) }
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
                                        hvacModeLabel(mode),
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
                        if (expanded) "Less" else "Fan & modes",
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
                            ClimateChipGroup("Preset", presetModes, presetMode) { mode ->
                                viewModel.callService(
                                    "climate", "set_preset_mode",
                                    HAServiceCall(entity_id = entity.entity_id, preset_mode = mode)
                                )
                            }
                        }
                        if (entity.fanModes.isNotEmpty()) {
                            ClimateChipGroup("Fan", entity.fanModes, entity.fanMode) {
                                viewModel.setClimateFanMode(entity.entity_id, it)
                            }
                        }
                        if (entity.swingModes.isNotEmpty()) {
                            ClimateChipGroup("Swing", entity.swingModes, entity.swingMode) {
                                viewModel.setClimateSwingMode(entity.entity_id, it)
                            }
                        }
                        if (entity.swingHorizontalModes.isNotEmpty()) {
                            ClimateChipGroup("Swing (horizontal)", entity.swingHorizontalModes, entity.swingHorizontalMode) {
                                viewModel.setClimateSwingHorizontalMode(entity.entity_id, it)
                            }
                        }
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
private fun ClimateChipGroup(
    title: String, modes: List<String>, activeMode: String?, onSelect: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                val selected = mode == activeMode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) accent.copy(alpha = 0.22f) else appColors.subtleSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) accent.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(mode) }
                ) {
                    Text(
                        mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
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
    padding: PaddingValues
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
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { entity, _ ->
            Box {
                if (deviceType == "humidifiers") HumidifierCard(entity, viewModel) else FanCard(entity, viewModel)
                EditSettingsButton(onClick = { onRename(entity) }, modifier = Modifier.align(Alignment.Center))
                EditRemoveBadge(onClick = { onRemove(entity.entity_id) }, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current)
    ) {
        if (devices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp), color = appColors.elevated
                ) {
                    Text(
                        when (deviceType) {
                            "fans" -> "No fans found. Fan entities from Home Assistant appear here automatically."
                            "purifiers" -> "No air purifiers configured. Add fan entities under Climate Settings → Climate Entities → Air purifiers."
                            else -> "No humidifiers found. Humidifier entities from Home Assistant appear here automatically; extras can be added in Climate Settings."
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
                    if (deviceType == "humidifiers") HumidifierCard(entity, viewModel) else FanCard(entity, viewModel)
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
private fun FanCard(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val color = if (isOn) AirGreen else appColors.onMuted
    val percentage = entity.fanPercentage
    var localPct by remember(percentage) { mutableFloatStateOf((percentage ?: 0).toFloat()) }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = appColors.elevated) {
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
                            append(if (isOn) "On" else entity.state.replaceFirstChar(Char::uppercase))
                            entity.fanPresetMode?.let { append(" · ${it.replaceFirstChar(Char::uppercase)}") }
                            if (isOn && percentage != null) append(" · $percentage%")
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
                    Slider(
                        value = localPct,
                        onValueChange = { localPct = it },
                        onValueChangeFinished = { viewModel.setFanPercentage(entity.entity_id, localPct.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${localPct.toInt()}%",
                        style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 38.dp)
                    )
                }
            }
            if (entity.fanPresetModes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ClimateChipGroup("Mode", entity.fanPresetModes, entity.fanPresetMode) {
                    viewModel.setFanPresetMode(entity.entity_id, it)
                }
            }
        }
    }
}

@Composable
private fun HumidifierCard(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val isDehumidifier = entity.deviceClass == "dehumidifier"
    val color = if (isOn) MistCyan else appColors.onMuted
    val target = entity.humidity  // target humidity, like climate's "temperature"
    val current = entity.currentHumidity
    val minHum = (entity.minHumidity ?: 0).toFloat()
    val maxHum = (entity.maxHumidity ?: 100).toFloat()
    var localTarget by remember(target) { mutableFloatStateOf((target ?: 50.0).toFloat()) }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = appColors.elevated) {
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
                            append(if (isOn) (if (isDehumidifier) "Drying" else "Humidifying") else entity.state.replaceFirstChar(Char::uppercase))
                            entity.humidifierMode?.let { append(" · ${it.replaceFirstChar(Char::uppercase)}") }
                        },
                        style = MaterialTheme.typography.bodySmall, color = color,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (current != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${current.toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = appColors.onSurface, fontWeight = FontWeight.Bold
                        )
                        Text("Current", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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
                            "${localTarget.toInt()}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = color, fontWeight = FontWeight.Bold
                        )
                        Text("Target", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                    }
                    TempStepButton(Icons.Default.Add, enabled = localTarget < maxHum) {
                        localTarget = (localTarget + 5f).coerceAtMost(maxHum)
                        viewModel.setHumidifierTarget(entity.entity_id, localTarget.toInt())
                    }
                }
            }
            if (entity.humidifierAvailableModes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ClimateChipGroup("Mode", entity.humidifierAvailableModes, entity.humidifierMode) {
                    viewModel.setHumidifierMode(entity.entity_id, it)
                }
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
    padding: PaddingValues
) {
    val appColors = LocalHKIAppColors.current
    var selectedHours by rememberSaveable(group.key) { mutableStateOf(24) }

    // Pull raw history for every sensor in the group; graphs update as results stream in.
    LaunchedEffect(group.key, selectedHours, sensors.map { it.entity_id }.toSet()) {
        sensors.forEach { viewModel.fetchEntityHistory(it.entity_id, selectedHours.toLong()) }
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
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { sensor, _ ->
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp), color = appColors.elevated
                ) {
                    EntitySensorGraphCard(
                        sensorEntity = sensor,
                        viewModel = viewModel,
                        selectedHours = selectedHours,
                        lineColor = group.color,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                EditSettingsButton(onClick = { onRename(sensor) }, modifier = Modifier.align(Alignment.Center))
                EditRemoveBadge(onClick = { onRemove(sensor.entity_id) }, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current)
    ) {
        // Summary card across all sensors in the group
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp), color = appColors.elevated
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
                            "${sensors.size} sensor${if (sensors.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleSmall, color = appColors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (values.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            HeroStat(Icons.Default.ArrowDownward, CoolBlue,
                                "${formatValue(values.min())}${if (unit.isNotBlank()) " $unit" else ""}", "Lowest")
                            HeroStat(group.icon, group.color,
                                "${formatValue(values.average().toFloat())}${if (unit.isNotBlank()) " $unit" else ""}", "Average")
                            HeroStat(Icons.Default.ArrowUpward, TempWarm,
                                "${formatValue(values.max())}${if (unit.isNotBlank()) " $unit" else ""}", "Highest")
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

        if (sensors.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp), color = appColors.elevated
                ) {
                    Text(
                        "No ${group.title.lowercase()} sensors found.",
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            // One gradient graph per sensor (colors follow the sensor's device_class palette)
            items(count = sensors.size, key = { sensors[it].entity_id }) { idx ->
                val sensor = sensors[idx]
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp), color = appColors.elevated
                    ) {
                        EntitySensorGraphCard(
                            sensorEntity = sensor,
                            viewModel = viewModel,
                            selectedHours = selectedHours,
                            lineColor = group.color,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
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
private fun ColumnScope.ClimateSensorSection(
    viewModel: MainViewModel,
    climateConfig: HKIClimateConfig,
    allEntities: List<HAEntity>,
    onSave: (HKIClimateConfig) -> Unit,
    setBack: ((() -> Unit)?) -> Unit = {}
) {
    val appColors = LocalHKIAppColors.current
    var cfg by remember(climateConfig) { mutableStateOf(climateConfig) }
    var category by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(category) { setBack(if (category != null) { { category = null } } else null) }

    fun entityName(id: String): String = allEntities.find { it.entity_id == id }?.friendlyName ?: id

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
        climateSensorGroups.forEach { put(it.key, it.title) }
        put("climate", "Thermostats & AC")
        put("purifiers", "Air purifiers")
        put("humidifiers", "Humidifiers")
        put("hidden", "Removed entities")
    }

    if (showPicker && category != null && category != "hidden") {
        val cat = category!!
        AdvancedEntitySearchDialog(
            allEntities = pickerEntities(cat),
            title = "Select ${categoryTitles[cat]}",
            singleSelect = false,
            preselectedIds = manualIds(cat).toSet(),
            onDismiss = { showPicker = false },
            onEntitiesSelected = { ids ->
                saveManualIds(cat, ids)
                showPicker = false
            }
        )
    }

    @Composable
    fun categoryButton(key: String, title: String, subtitle: String, icon: ImageVector, color: Color) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { category = key },
            shape = RoundedCornerShape(18.dp),
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
            val extraCount = cfg.extraSensorIds[group.key].orEmpty().size
            categoryButton(
                group.key, group.title,
                if (extraCount > 0) "Auto-discovered + $extraCount manual" else "Auto-discovered by device class",
                group.icon, group.color
            )
        }
        categoryButton("climate", "Thermostats & AC",
            if (cfg.extraClimateIds.isEmpty()) "Auto-discovered climate devices" else "Auto + ${cfg.extraClimateIds.size} manual",
            Icons.Default.Thermostat, TempWarm)
        categoryButton("purifiers", "Air purifiers",
            if (cfg.purifierEntityIds.isEmpty()) "Pick fan entities to treat as purifiers" else "${cfg.purifierEntityIds.size} selected",
            Icons.Default.Air, AirGreen)
        categoryButton("humidifiers", "Humidifiers",
            if (cfg.extraHumidifierIds.isEmpty()) "Auto-discovered humidifier devices" else "Auto + ${cfg.extraHumidifierIds.size} manual",
            Icons.Default.WaterDrop, MistCyan)
        categoryButton("hidden", "Removed entities",
            if (cfg.hiddenEntityIds.isEmpty()) "Entities removed in edit mode" else "${cfg.hiddenEntityIds.size} removed",
            Icons.Default.VisibilityOff, appColors.onMuted)
        return
    }

    if (category == "hidden") {
        if (cfg.hiddenEntityIds.isEmpty()) {
            Text(
                "Nothing removed. Use edit mode on the Climate page to remove cards; they land here and can be restored.",
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
        } else {
            Text(
                "Removed entities are excluded from cards, graphs, tiles and averages. Tap ✕ to restore.",
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
                        Icon(Icons.Default.Close, "Restore", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
        }
        return
    }

    // Manual entity list for the selected category
    val cat = category!!
    Text(
        when (cat) {
            "climate" -> "Climate devices are found automatically; add extra entities here if any are missing."
            "purifiers" -> "Air purifiers are fan entities in Home Assistant, so they can't be auto-detected. Select yours here."
            "humidifiers" -> "Humidifiers are found automatically from the humidifier domain; add extra entities here if any are missing."
            else -> "${categoryTitles[cat]} sensors are found automatically by device class; add sensors missing that class here."
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
                Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }
    TextButton(onClick = { showPicker = true }) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (manualIds(cat).isEmpty()) "Add entities" else "Edit entities")
    }
}

// ═══ SHARED BITS (Energy-page style) ════════════════════════════════════════

@Composable
private fun ClimateSectionHeader(title: String, trailing: String?) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClimateLiveTile(
    icon: ImageVector, color: Color, title: String, status: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = appColors.elevated,
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable { onClick() }
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
                Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(status, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        }
    }
}

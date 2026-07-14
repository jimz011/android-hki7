package com.example.hki7.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAServiceCall
import com.example.hki7.data.HKIClimateCardWidget
import com.example.hki7.data.HKIClimateConfig
import com.example.hki7.data.HKIClimateStack
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.data.withDisplayName
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.EntitySensorGraphCard
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.HistoryRangeChips
import com.example.hki7.ui.components.ReorderAxis
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.hvacColor
import com.example.hki7.ui.components.hvacGradient
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.components.surfaceGradient
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
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

    // Thermostats & air conditioners: every climate.* entity, plus manual additions, minus removed.
    val climateEntities = remember(entities, climateConfig) {
        (entities.filter { it.entity_id.startsWith("climate.") } +
            climateConfig.extraClimateIds.mapNotNull { entityById[it] })
            .distinctBy { it.entity_id }
            .filter { it.entity_id !in hidden }
            .sortedBy { it.friendlyName ?: it.entity_id }
            .applyClimateOrder(climateConfig.entityOrder)
    }
    val climateDeviceRows = remember(climateEntities, climateConfig.deviceCardWidths, climateConfig.deviceCardStyles, climateConfig.defaultDeviceCardWidth, climateConfig.defaultDeviceCardStyle) {
        buildList<List<HAEntity>> {
            var row = mutableListOf<HAEntity>()
            var used = 0
            climateEntities.forEach { entity ->
                val configuredWidth = climateConfig.deviceCardWidths[entity.entity_id] ?: climateConfig.defaultDeviceCardWidth
                val style = climateConfig.deviceCardStyles[entity.entity_id] ?: climateConfig.defaultDeviceCardStyle
                val effectiveWidth = if (style == "dial" && configuredWidth == "third") "half" else configuredWidth
                val span = when (effectiveWidth) {
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
                                val style = climateConfig.deviceCardStyles[entity.entity_id] ?: climateConfig.defaultDeviceCardStyle
                                val width = if (style == "dial" && configuredWidth == "third") "half" else configuredWidth
                                val span = when (width) { "third" -> 2; "half" -> 3; else -> 6 }
                                used += span
                                val iconOverride = climateConfig.customIcons[entity.entity_id]
                                val cardStyle = climateConfig.deviceCardStyles[entity.entity_id] ?: climateConfig.defaultDeviceCardStyle
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

/** Per-device settings for a climate device on the main Climate page: rename, custom icon, and the
 *  card style (the standard "current" card or the round thermostat dial). */
@Composable
private fun ClimateDeviceSettingsDialog(
    entity: HAEntity,
    config: HKIClimateConfig,
    onDismiss: () -> Unit,
    onSave: (HKIClimateConfig) -> Unit
) {
    var name by remember(entity) { mutableStateOf(config.customNames[entity.entity_id].orEmpty()) }
    var icon by remember(entity) { mutableStateOf(config.customIcons[entity.entity_id].orEmpty()) }
    var style by remember(entity) { mutableStateOf(config.deviceCardStyles[entity.entity_id] ?: config.defaultDeviceCardStyle) }
    var width by remember(entity) { mutableStateOf(config.deviceCardWidths[entity.entity_id] ?: config.defaultDeviceCardWidth) }
    var shape by remember(entity) { mutableStateOf(config.deviceCardShapes[entity.entity_id] ?: "standard") }
    var showIconPicker by remember { mutableStateOf(false) }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = icon,
            onDismiss = { showIconPicker = false },
            onSelect = { icon = it; showIconPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text(entity.friendlyName ?: entity.entity_id) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("Card style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = style == "card", onClick = { style = "card" }, label = { Text("Climate control") })
                    FilterChip(selected = style == "dial", onClick = { style = "dial"; if (width == "third") width = "half" }, label = { Text("Thermostat dial") })
                }
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = shape == "standard", onClick = { shape = "standard" }, label = { Text("Standard") })
                    FilterChip(selected = shape == "square", onClick = { shape = "square" }, label = { Text("Square") })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = style != "dial")
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon.isNotBlank()) MdiIcon(icon, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) { Text(if (icon.isBlank()) "Choose" else "Change") }
                    if (icon.isNotBlank()) TextButton(onClick = { icon = "" }) { Text("Default") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = entity.entity_id
                onSave(config.copy(
                    customNames = if (name.isBlank()) config.customNames - id else config.customNames + (id to name.trim()),
                    customIcons = if (icon.isBlank()) config.customIcons - id else config.customIcons + (id to icon),
                    deviceCardStyles = if (style == config.defaultDeviceCardStyle) config.deviceCardStyles - id else config.deviceCardStyles + (id to style),
                    deviceCardWidths = if (width == config.defaultDeviceCardWidth) config.deviceCardWidths - id else config.deviceCardWidths + (id to width),
                    deviceCardShapes = if (shape == "standard") config.deviceCardShapes - id else config.deviceCardShapes + (id to shape)
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ═══ HERO ══════════════════════════════════════════════════════════════════

@Composable
private fun ClimateHero(
    tempSensors: List<HAEntity>,
    humiditySensors: List<HAEntity>,
    climateEntities: List<HAEntity>,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    cornerRadius: Int = 24
) {
    val appColors = LocalHKIAppColors.current
    val temps = tempSensors.mapNotNull { it.numericState() }
    val hums = humiditySensors.mapNotNull { it.numericState() }
    val avgTemp = if (temps.isNotEmpty()) temps.average().toFloat() else null
    val avgHum = if (hums.isNotEmpty()) hums.average().toFloat() else null
    val tempUnit = tempSensors.firstOrNull()?.unit()?.ifBlank { "°C" } ?: "°C"
    val activeCount = climateEntities.count { it.state != "off" && it.state != "unavailable" }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp), color = appColors.elevated
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
private fun ClimateDeviceCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = 20, isSquare: Boolean = false, iconOverride: String? = null) {
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
        append((if (localMode != entity.state) localMode else hvacAction ?: entity.state).replace('_', ' ').replaceFirstChar(Char::uppercase))
        presetMode?.takeIf { it != "none" }?.let { append(" · ${it.replaceFirstChar(Char::uppercase)}") }
        currentHumidity?.let { append(" · ${it.toInt()}%") }
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
                            if (currentTemp != null) "${"%.1f".format(locale, currentTemp)}° current" else statusText,
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
                                "${"%.1f".format(locale, localTarget)}°",
                                style = MaterialTheme.typography.titleLarge,
                                color = actionColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Target", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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
            if (!isSquare && entity.hvacModes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HvacModePillsRow(entity, viewModel, localMode) { localMode = it }
            }

            // Fan / swing / preset modes, collapsed behind a "More" toggle
            if (!isSquare && hasExtras) {
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
                            ClimateChipGroup("Preset", presetModes, presetMode, centered = true) { mode ->
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
}

// ═══ THERMOSTAT DIAL (Nest-style ring, used by the "dial" climate card) ══════

private fun dimmed(color: Color, factor: Float): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, 1f)

@Composable
private fun ThermostatDialCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = 20, isSquare: Boolean = false, iconOverride: String? = null, onCenterClick: (() -> Unit)? = null) {
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
                            Text("%.1f °C".format(locale, currentTemp),
                                style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                        }
                    }
                    if (outdoorTemp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            MdiIcon("thermometer", tint = appColors.onMuted, size = 17.dp)
                            Text("%.1f °C".format(locale, outdoorTemp),
                                style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val dialSize = if (maxWidth < 300.dp) maxWidth else 300.dp
                val ringWidth = dialSize * 0.17f
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
                                    // Dial spans 135°→405°; the bottom 45°..135° zone clamps to the ends.
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
                                Text(statusLabel, style = MaterialTheme.typography.labelMedium,
                                    color = appColors.onMuted, fontWeight = FontWeight.SemiBold)
                                if (hasTarget) {
                                    val parts = "%.1f".format(Locale.US, localTarget).split(".")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(parts[0], style = MaterialTheme.typography.displayLarge,
                                            color = appColors.onSurface, fontWeight = FontWeight.Bold)
                                        Column {
                                            Text("°C", style = MaterialTheme.typography.titleSmall,
                                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                            Text(".${parts.getOrElse(1) { "0" }}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else {
                                    currentTemp?.let {
                                        Text("%.1f°".format(locale, it), style = MaterialTheme.typography.displayMedium,
                                            color = appColors.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Mode pill at the top-right of the face, opens the HVAC mode menu.
                            if (entity.hvacModes.isNotEmpty()) {
                                Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = appColors.surface,
                                        shadowElevation = 5.dp,
                                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { modesOpen = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(hvacModeIcon(localMode), contentDescription = "Mode",
                                                tint = accent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    DropdownMenu(expanded = modesOpen, onDismissRequest = { modesOpen = false }) {
                                        entity.hvacModes.forEach { mode ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(hvacModeIcon(mode), null, tint = hvacColor(mode),
                                                        modifier = Modifier.size(18.dp))
                                                },
                                                text = {
                                                    Text(hvacModeLabel(mode),
                                                        fontWeight = if (mode == localMode) FontWeight.Bold else null)
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
                            Icon(Icons.Default.Remove, "Lower target", tint = Color.White, modifier = Modifier.size(22.dp))
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
                            Icon(Icons.Default.Add, "Raise target", tint = Color.White, modifier = Modifier.size(22.dp))
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
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) modeColor.copy(alpha = 0.22f) else appColors.subtleSurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) modeColor.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
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
private fun FanCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = 20) {
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
private fun HumidifierCard(entity: HAEntity, viewModel: MainViewModel, cornerRadius: Int = 20) {
    val appColors = LocalHKIAppColors.current
    val isOn = entity.state == "on"
    val isDehumidifier = entity.deviceClass == "dehumidifier"
    val color = if (isOn) MistCyan else appColors.onMuted
    val target = entity.humidity  // target humidity, like climate's "temperature"
    val current = entity.currentHumidity
    val minHum = (entity.minHumidity ?: 0).toFloat()
    val maxHum = (entity.maxHumidity ?: 100).toFloat()
    var localTarget by remember(target) { mutableFloatStateOf((target ?: 50.0).toFloat()) }

    Surface(modifier = Modifier.fillMaxWidth().background(surfaceGradient(appColors.elevated), RoundedCornerShape(cornerRadius.dp)), shape = RoundedCornerShape(cornerRadius.dp), color = Color.Transparent) {
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = appColors.subtleSurface
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "All thermostats",
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Set the layout for every thermostat on this Climate page. You can still override individual devices in edit mode.",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Control", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("card" to "Climate control", "dial" to "Thermostat dial").forEach { (value, label) ->
                        FilterChip(
                            selected = cfg.defaultDeviceCardStyle == value,
                            onClick = {
                                val adjustedWidths = if (value == "dial") {
                                    cfg.deviceCardWidths.mapValues { (_, width) -> if (width == "third") "half" else width }
                                } else cfg.deviceCardWidths
                                cfg = cfg.copy(
                                    defaultDeviceCardStyle = value,
                                    defaultDeviceCardWidth = if (value == "dial" && cfg.defaultDeviceCardWidth == "third") "half" else cfg.defaultDeviceCardWidth,
                                    deviceCardStyles = emptyMap(),
                                    deviceCardWidths = adjustedWidths
                                )
                                onSave(cfg)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Width", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("third" to "Third", "half" to "Half", "full" to "Full").forEach { (value, label) ->
                        val enabled = value != "third" || cfg.defaultDeviceCardStyle != "dial"
                        FilterChip(
                            selected = cfg.defaultDeviceCardWidth == value,
                            enabled = enabled,
                            onClick = {
                                cfg = cfg.copy(defaultDeviceCardWidth = value, deviceCardWidths = emptyMap())
                                onSave(cfg)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
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
    modifier: Modifier = Modifier, cornerRadius: Int = 18, onClick: (() -> Unit)? = null
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
                Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
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

data class ClimateCardSpec(val key: String, val label: String, val category: String, val mdiIcon: String)

val climateCardCatalog = listOf(
    ClimateCardSpec("hero", "Indoor climate overview", "Overview", "home-thermometer"),
    ClimateCardSpec("tiles", "Sensor & device tiles", "Overview", "view-grid"),
    ClimateCardSpec("thermostats", "Thermostats & AC", "Devices", "thermostat"),
    ClimateCardSpec("dial", "Thermostat dial", "Devices", "knob"),
    ClimateCardSpec("fans", "Fans", "Devices", "fan"),
    ClimateCardSpec("purifiers", "Air purifiers", "Devices", "air-purifier"),
    ClimateCardSpec("humidifiers", "Humidifiers", "Devices", "air-humidifier"),
    ClimateCardSpec("temperature", "Temperature summary", "Sensors", "thermometer"),
    ClimateCardSpec("humidity", "Humidity summary", "Sensors", "water-percent"),
    ClimateCardSpec("pressure", "Air pressure summary", "Sensors", "gauge"),
    ClimateCardSpec("co2", "CO₂ summary", "Sensors", "molecule-co2"),
    ClimateCardSpec("air", "Air quality summary", "Sensors", "air-filter")
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

/** Same discovery rules as the Climate page (domain + device_class + manual config). */
@Composable
private fun rememberClimateWidgetData(viewModel: MainViewModel): ClimateWidgetData {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
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
    return remember(rawEntities, climateConfig) {
        val entities = rawEntities.map { it.withDisplayName(climateConfig.customNames[it.entity_id]) }
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
        ClimateWidgetData(climateEntities, fanEntities, purifierEntities, humidifierEntities, groupSensors)
    }
}

/** Renders one climate card, self-contained (discovers its own entities). A non-empty
 *  [entityIdsOverride] makes the card use exactly those entities instead of the view's. */
@Composable
fun ClimateCardWidgetView(
    cardKey: String,
    viewModel: MainViewModel,
    cornerRadius: Int = 28,
    modifier: Modifier = Modifier,
    entityIdsOverride: List<String> = emptyList(),
    isSquare: Boolean = false
) {
    val appColors = LocalHKIAppColors.current
    val data = if (entityIdsOverride.isEmpty()) rememberClimateWidgetData(viewModel)
        else rememberClimateWidgetOverrideData(viewModel, entityIdsOverride)
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
            modifier = modifier.fillMaxWidth(),
            cornerRadius = cornerRadius
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
                    val status = buildString {
                        append("${sensors.size} sensor${if (sensors.size == 1) "" else "s"}")
                        if (avg != null) append(" · avg ${formatValue(avg)}${if (unit.isNotBlank()) " $unit" else ""}")
                    }
                    add(TileSpec(group.icon, group.color, group.title, status))
                }
                if (data.fanEntities.isNotEmpty()) {
                    val on = data.fanEntities.count { it.state == "on" }
                    add(TileSpec(Icons.Default.Air, CoolBlue, "Fans", "${data.fanEntities.size} device${if (data.fanEntities.size == 1) "" else "s"} · $on on"))
                }
                if (data.purifierEntities.isNotEmpty()) {
                    val on = data.purifierEntities.count { it.state == "on" }
                    add(TileSpec(Icons.Default.Air, AirGreen, "Air purifiers", "${data.purifierEntities.size} device${if (data.purifierEntities.size == 1) "" else "s"} · $on on"))
                }
                if (data.humidifierEntities.isNotEmpty()) {
                    val on = data.humidifierEntities.count { it.state == "on" }
                    add(TileSpec(Icons.Default.WaterDrop, MistCyan, "Humidifiers", "${data.humidifierEntities.size} device${if (data.humidifierEntities.size == 1) "" else "s"} · $on on"))
                }
            }
            if (tiles.isEmpty()) { emptyCard("No climate sensors or devices found."); return }
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(2).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        rowTiles.forEach { t ->
                            ClimateLiveTile(
                                icon = t.icon, color = t.color, title = t.title, status = t.status,
                                modifier = Modifier.weight(1f), cornerRadius = cornerRadius.coerceAtMost(18)
                            )
                        }
                        if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        "thermostats" -> deviceList(
            data.climateEntities,
            "No climate devices found. Thermostats and AC units from Home Assistant appear here automatically."
        ) { ClimateDeviceCard(it, viewModel, cornerRadius) }
        "dial" -> deviceList(
            data.climateEntities,
            "No climate devices found. Thermostats and AC units from Home Assistant appear here automatically."
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
            "No fans found. Fan entities from Home Assistant appear here automatically."
        ) { FanCard(it, viewModel, cornerRadius) }
        "purifiers" -> deviceList(
            data.purifierEntities,
            "No air purifiers configured. Select fan entities under Climate Settings → Climate Entities → Air purifiers."
        ) { FanCard(it, viewModel, cornerRadius) }
        "humidifiers" -> deviceList(
            data.humidifierEntities,
            "No humidifiers found. Humidifier entities from Home Assistant appear here automatically."
        ) { HumidifierCard(it, viewModel, cornerRadius) }
        else -> {
            val group = climateSensorGroups.find { it.key == cardKey }
            if (group == null) { emptyCard("Unknown climate card: $cardKey"); return }
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
                            Text(group.title, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${sensors.size} sensor${if (sensors.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                            )
                        }
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
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("No ${group.title.lowercase()} sensors found.",
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
    if (widget.isHidden && !isEditMode) return
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
            ClimateCardWidgetView(widget.cardKey, viewModel, widget.cornerRadius, entityIdsOverride = widget.entityIds, isSquare = widget.isSquare)
        }
        if (isEditMode) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(24.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = LocalHKIAppColors.current.onSurface, modifier = Modifier.size(16.dp))
            }
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
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
    if (stack.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val collapsed = stack.collapsible && (stack.isCollapsed ?: stack.defaultCollapsed)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                MdiIcon(stack.icon ?: "thermostat", tint = appColors.onMuted, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(stack.title ?: "Climate", color = appColors.onMuted,
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
                            Text("No cards yet — open the stack settings to pick climate cards.",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                    stack.cardKeys.forEach { key ->
                        ClimateCardWidgetView(key, viewModel, stack.cornerRadius, entityIdsOverride = stack.entityIds)
                    }
                }
            }
        }
        if (isEditMode) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(24.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = appColors.onSurface, modifier = Modifier.size(16.dp))
            }
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}

/** Scrollable list over the climate card catalog, shared by the picker dialog and the embedded
 *  picker step inside AddRoomWidgetDialog. */
@Composable
fun ClimateCardPickerList(
    multiSelect: Boolean,
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
        climateCardCatalog.groupBy { it.category }.forEach { (category, specs) ->
            item {
                Text(category, style = MaterialTheme.typography.labelLarge,
                    color = appColors.onMuted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(specs.size) { i ->
                val spec = specs[i]
                val isSel = spec.key in selected
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onToggle(spec.key) },
                    shape = RoundedCornerShape(22.dp),
                    color = appColors.subtleSurface,
                    border = if (isSel) BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.28f)) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MdiIcon(spec.mdiIcon, contentDescription = null, tint = appColors.onSurface, size = 28.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(spec.label, color = appColors.onSurface,
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
    title: String = "Select Climate Cards",
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(preselected.toList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back")
                }
                ClimateCardPickerList(
                    multiSelect = multiSelect,
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
        confirmButton = { TextButton(onClick = { onSelected(selected) }) { Text("Done") } }
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
 *  explicit selection instead of auto-filling from the Climate view. */
@Composable
fun ClimateCardEntityPickerDialog(
    cardKey: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    val allEntities by viewModel.entities.collectAsState()
    AdvancedEntitySearchDialog(
        allEntities = allEntities.filter(climateOverridePickerFilter(listOf(cardKey))),
        title = "Entities · ${climateCardCatalog.find { it.key == cardKey }?.label ?: cardKey}",
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = onSelected
    )
}

/** "Custom entities" rows shared by the climate card and stack settings dialogs. */
@Composable
private fun ColumnScope.ClimateEntityOverrideSection(
    entityIds: List<String>,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Entities", style = MaterialTheme.typography.labelLarge)
            Text(
                if (entityIds.isEmpty()) "Using the Climate view's entities"
                else "${entityIds.size} selected for this card",
                style = MaterialTheme.typography.bodySmall,
                color = if (entityIds.isEmpty()) appColors.onMuted else MaterialTheme.colorScheme.primary
            )
        }
        if (entityIds.isNotEmpty()) {
            TextButton(onClick = onReset) { Text("Reset") }
        }
        TextButton(onClick = onPick) { Text("Change") }
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
    var radius by remember { mutableStateOf(widget.cornerRadius) }
    var cardKey by remember { mutableStateOf(widget.cardKey) }
    var isSquare by remember { mutableStateOf(widget.isSquare) }
    var entityIds by remember { mutableStateOf(widget.entityIds) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingEntities by remember { mutableStateOf(false) }
    if (showPicker) {
        ClimateCardPickerDialog(
            multiSelect = false, preselected = listOf(cardKey), title = "Select Climate Card",
            onDismiss = { showPicker = false },
            onSelected = { sel -> sel.firstOrNull()?.let { cardKey = it }; showPicker = false }
        )
    }
    if (pickingEntities) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter(climateOverridePickerFilter(listOf(cardKey))),
            title = "Select Entities",
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { pickingEntities = false },
            onEntitiesSelected = { ids -> entityIds = ids; pickingEntities = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Climate Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Card", style = MaterialTheme.typography.labelLarge)
                        Text(climateCardCatalog.find { it.key == cardKey }?.label ?: cardKey,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { showPicker = true }) { Text("Change") }
                }
                ClimateEntityOverrideSection(
                    entityIds = entityIds,
                    onPick = { pickingEntities = true },
                    onReset = { entityIds = emptyList() }
                )
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                // The thermostat dial can render as a compact 1:1 square, like the other widgets.
                if (cardKey == "dial") {
                    Text("Shape", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                    }
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = radius == 8, onClick = { radius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = radius == 20, onClick = { radius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = radius == 28, onClick = { radius = 28 }, label = { Text("Round") })
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
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
    var radius by remember { mutableStateOf(stack.cornerRadius) }
    var cardKeys by remember { mutableStateOf(stack.cardKeys) }
    var collapsible by remember { mutableStateOf(stack.collapsible) }
    var entityIds by remember { mutableStateOf(stack.entityIds) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingEntities by remember { mutableStateOf(false) }
    if (showPicker) {
        ClimateCardPickerDialog(
            multiSelect = true, preselected = cardKeys, title = "Stack Cards",
            onDismiss = { showPicker = false },
            onSelected = { cardKeys = it; showPicker = false }
        )
    }
    if (pickingEntities) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter(climateOverridePickerFilter(cardKeys)),
            title = "Select Entities",
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { pickingEntities = false },
            onEntitiesSelected = { ids -> entityIds = ids; pickingEntities = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Climate Stack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Cards", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (cardKeys.isEmpty()) "None selected"
                            else cardKeys.joinToString { key -> climateCardCatalog.find { it.key == key }?.label ?: key },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { showPicker = true }) { Text("Change") }
                }
                ClimateEntityOverrideSection(
                    entityIds = entityIds,
                    onPick = { pickingEntities = true },
                    onReset = { entityIds = emptyList() }
                )
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Collapsible", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = collapsible, onCheckedChange = { collapsible = it })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = radius == 8, onClick = { radius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = radius == 20, onClick = { radius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = radius == 28, onClick = { radius = 28 }, label = { Text("Round") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(stack.copy(
                    title = title.ifBlank { null }, width = width, cornerRadius = radius,
                    cardKeys = cardKeys, collapsible = collapsible, entityIds = entityIds
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

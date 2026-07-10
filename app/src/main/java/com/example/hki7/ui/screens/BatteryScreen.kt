package com.example.hki7.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HADeviceRegistryEntry
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAEntityRegistryEntry
import com.example.hki7.data.HKIBatteryCardWidget
import com.example.hki7.data.HKIBatteryConfig
import androidx.navigation.NavController
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private const val BATTERY_PAGE_KEY = "battery"

private data class BatteryInfo(
    val entity: HAEntity,
    val level: Int?,
    val deviceName: String?,
    val deviceId: String?,
    val batteryType: String?,
    val quantity: String?,
    val fromBatteryPlus: Boolean,
    val notes: List<Pair<String, String>>
)

private data class BatteryCategory(
    val title: String,
    val subtitle: String,
    val color: Color,
    val predicate: (BatteryInfo) -> Boolean
)

private data class BatteryInputSet(
    val signature: String,
    val entities: List<HAEntity>
)

private data class BatteryWidgetSummary(
    val lowCount: Int = 0,
    val criticalCount: Int = 0
)

private val batteryCategories = listOf(
    BatteryCategory("Critical", "0-10%", Color(0xFFE53935)) { (it.level ?: 101) <= 10 },
    BatteryCategory("Low", "11-30%", Color(0xFFFF9800)) { (it.level ?: 101) in 11..30 },
    BatteryCategory("Watch", "31-50%", Color(0xFFFFD54F)) { (it.level ?: 101) in 31..50 },
    BatteryCategory("Good", "Above 50%", Color(0xFF43A047)) { (it.level ?: -1) > 50 },
    BatteryCategory("Unknown", "Unavailable or unknown", Color(0xFF8E8E93)) { it.level == null }
)

private fun HAEntity.attr(name: String): String? =
    attributes?.get(name)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun HAEntity.batteryLevel(): Int? =
    state.takeUnless { it.equals("unknown", true) || it.equals("unavailable", true) }
        ?.toFloatOrNull()
        ?.toInt()
        ?.coerceIn(0, 100)

private fun HAEntity.unit(): String =
    attr("unit_of_measurement").orEmpty()

private fun HAEntity.isBatteryLevelSensor(): Boolean {
    if (!entity_id.startsWith("sensor.")) return false
    val stateValue = state.toFloatOrNull()
    val unavailable = state.equals("unknown", true) || state.equals("unavailable", true)
    val looksLikeLevel = deviceClass == "battery" || unit() == "%"
    if (!looksLikeLevel) return false
    if (stateValue != null) return stateValue in 0f..100f
    return unavailable && deviceClass == "battery"
}

private fun HAEntity.isBatteryMetadataEntity(): Boolean {
    if (!entity_id.startsWith("sensor.")) return false
    val haystack = "${entity_id} ${friendlyName.orEmpty()}".lowercase(Locale.getDefault())
    val attributeKeys = attributes?.keys.orEmpty().map { it.lowercase(Locale.getDefault()) }
    return attributeKeys.any {
        it == "battery_type" ||
            it == "battery_type_and_quantity" ||
            it == "battery_quantity" ||
            it == "battery_count" ||
            it == "battery_last_replaced" ||
            it.contains("battery_notes") ||
            it.contains("battery_plus")
    } ||
        haystack.contains("battery type") ||
        haystack.contains("battery quantity") ||
        haystack.contains("battery count") ||
        haystack.contains("battery_notes") ||
        haystack.contains("battery plus") ||
        haystack.contains("battery+")
}

private fun HAEntity.batterySignaturePart(): String {
    val attrs = listOf(
        "friendly_name",
        "unit_of_measurement",
        "device_class",
        "battery_type",
        "battery_type_and_quantity",
        "battery_quantity",
        "battery_count",
        "battery_last_replaced",
        "attribution"
    ).joinToString(",") { key -> "$key=${attr(key).orEmpty()}" }
    return "$entity_id|$state|${icon.orEmpty()}|${deviceClass.orEmpty()}|$attrs"
}

private fun batteryInputSet(entities: List<HAEntity>, manualEntityIds: Set<String> = emptySet()): BatteryInputSet {
    val relevant = entities.filter { entity ->
        entity.isBatteryLevelSensor() ||
            entity.isBatteryMetadataEntity() ||
            (entity.entity_id in manualEntityIds && entity.entity_id.startsWith("sensor.") && entity.batteryLevel() != null)
    }
    return BatteryInputSet(
        signature = relevant.joinToString(separator = "\n") { it.batterySignaturePart() },
        entities = relevant
    )
}

private fun normalizedBatteryType(type: String?): String? =
    type?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("unknown", true) && !it.equals("unavailable", true) }
        ?.replace(Regex("\\s+"), " ")

private fun levelMatches(info: BatteryInfo, filter: String): Boolean = when (filter) {
    "critical" -> (info.level ?: 101) <= 10
    "low" -> (info.level ?: 101) in 11..30
    "watch" -> (info.level ?: 101) in 31..50
    "good" -> (info.level ?: -1) > 50
    "unknown" -> info.level == null
    else -> true
}

private fun sortedBatteries(items: List<BatteryInfo>, sort: String): List<BatteryInfo> = when (sort) {
    "name" -> items.sortedBy { it.deviceName ?: it.entity.friendlyName ?: it.entity.entity_id }
    "type" -> items.sortedWith(compareBy<BatteryInfo> { it.batteryType ?: "zzzz" }.thenBy { it.level ?: 101 })
    else -> items.sortedWith(compareBy<BatteryInfo> { it.level ?: 101 }.thenBy { it.deviceName ?: it.entity.entity_id })
}

private fun batteryColor(level: Int?): Color = when {
    level == null -> Color(0xFF8E8E93)
    level <= 10 -> Color(0xFFE53935)
    level <= 25 -> Color(0xFFFF9800)
    level <= 50 -> Color(0xFFFFD54F)
    else -> Color(0xFF43A047)
}

private fun isBatteryPlusEntity(
    entity: HAEntity,
    entry: HAEntityRegistryEntry?,
    siblingEntities: List<HAEntity>,
    batteryType: String?,
    quantity: String?
): Boolean {
    val platform = entry?.platform?.lowercase(Locale.getDefault())
    if (platform == "battery_notes" || platform == "battery_plus") return true
    val ownAttributeKeys = entity.attributes?.keys.orEmpty().map { it.lowercase(Locale.getDefault()) }
    if (ownAttributeKeys.any {
            it == "battery_type" ||
                it == "battery_type_and_quantity" ||
                it == "battery_quantity" ||
                it == "battery_count" ||
                it == "battery_last_replaced" ||
                it.contains("battery_notes") ||
                it.contains("battery_plus")
        }
    ) return true
    if (!batteryType.isNullOrBlank() || !quantity.isNullOrBlank()) return true
    return siblingEntities.any { sibling ->
        val haystack = "${sibling.entity_id} ${sibling.friendlyName.orEmpty()} ${sibling.attr("attribution").orEmpty()}"
            .lowercase(Locale.getDefault())
        haystack.contains("battery_notes") ||
            haystack.contains("battery plus") ||
            haystack.contains("battery+") ||
            haystack.contains("battery type") ||
            haystack.contains("battery_type") ||
            haystack.contains("battery quantity") ||
            haystack.contains("battery_quantity")
    }
}

private fun batteryEntities(
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>,
    useBatteryNotes: Boolean,
    manualEntityIds: Set<String> = emptySet()
): List<BatteryInfo> {
    val registryById = registry.associateBy { it.entity_id }
    val entitiesById = entities.associateBy { it.entity_id }
    val deviceById = devices.associateBy { it.id }
    val entitiesByDevice = registry.groupBy { it.device_id }
    return entities.asSequence()
        .filter { e ->
            e.isBatteryLevelSensor() ||
                (e.entity_id in manualEntityIds && e.entity_id.startsWith("sensor.") && e.batteryLevel() != null)
        }
        .map { entity ->
            val entry = registryById[entity.entity_id]
            val device = entry?.device_id?.let { deviceById[it] }
            val siblingEntities = entry?.device_id
                ?.let { entitiesByDevice[it].orEmpty().mapNotNull { r -> entitiesById[r.entity_id] } }
                .orEmpty()
            val batteryType = normalizedBatteryType(entity.attr("battery_type")
                ?: entity.attr("battery_type_and_quantity")
                ?: entity.attr("battery")
                ?: siblingEntities.firstNotNullOfOrNull { s ->
                    val name = (s.friendlyName ?: s.entity_id).lowercase(Locale.getDefault())
                    if (name.contains("battery type")) s.state.takeIf { it !in listOf("unknown", "unavailable") } else null
                })
            val quantity = entity.attr("battery_quantity")
                ?: entity.attr("battery_count")
                ?: siblingEntities.firstNotNullOfOrNull { s ->
                    val name = (s.friendlyName ?: s.entity_id).lowercase(Locale.getDefault())
                    if (name.contains("battery quantity") || name.contains("battery count")) s.state else null
                }
            val notes = if (useBatteryNotes) {
                siblingEntities
                    .filter { s ->
                        val haystack = "${s.entity_id} ${s.friendlyName.orEmpty()}".lowercase(Locale.getDefault())
                        haystack.contains("battery") && s.entity_id != entity.entity_id && s.state !in listOf("unknown", "unavailable")
                    }
                    .take(4)
                    .map { s -> (s.friendlyName ?: s.entity_id.substringAfter(".")) to s.state }
            } else emptyList()
            val fromBatteryPlus = isBatteryPlusEntity(entity, entry, siblingEntities, batteryType, quantity)
            BatteryInfo(
                entity = entity,
                level = entity.batteryLevel(),
                deviceName = device?.name_by_user ?: device?.name ?: entity.friendlyName,
                deviceId = entry?.device_id,
                batteryType = batteryType,
                quantity = quantity,
                fromBatteryPlus = fromBatteryPlus,
                notes = notes
            )
        }
        .filter { !useBatteryNotes || it.fromBatteryPlus }
        .distinctBy { it.entity.entity_id }
        .sortedWith(compareBy<BatteryInfo> { it.level ?: 101 }.thenBy { it.entity.friendlyName ?: it.entity.entity_id })
        .toList()
}

@Composable
fun BatteryScreen(viewModel: MainViewModel, navController: NavController? = null) {
    val registry by viewModel.entityRegistry.collectAsState()
    val devices by viewModel.deviceRegistry.collectAsState()
    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val config = (pageConfigs[BATTERY_PAGE_KEY] ?: HKIPageConfig()).batteryConfig ?: HKIBatteryConfig()
    var selected by remember { mutableStateOf<BatteryInfo?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var levelFilter by rememberSaveable { mutableStateOf("all") }
    var sortMode by rememberSaveable { mutableStateOf("level") }
    val manualEntityIds = remember(registry, config.extraEntityIds, config.extraDeviceIds) {
        (config.extraEntityIds + registry
            .filter { it.device_id in config.extraDeviceIds }
            .map { it.entity_id })
            .toSet()
    }
    val batteryEntityFlow = remember(viewModel, manualEntityIds) {
        val selectorKey = "battery:${manualEntityIds.sorted().joinToString(",")}"
        viewModel.entitiesMatching(selectorKey) { entity ->
            entity.isBatteryLevelSensor() ||
                entity.isBatteryMetadataEntity() ||
                (entity.entity_id in manualEntityIds && entity.entity_id.startsWith("sensor.") && entity.batteryLevel() != null)
        }
    }
    val batteryEntities by batteryEntityFlow.collectAsState()
    val batteryInputs = remember(batteryEntities, manualEntityIds) {
        batteryInputSet(batteryEntities, manualEntityIds)
    }
    val batteries = remember(
        batteryInputs.signature,
        registry,
        devices,
        config.useBatteryNotes,
        config.hiddenEntityIds,
        manualEntityIds
    ) {
        batteryEntities(batteryInputs.entities, registry, devices, config.useBatteryNotes, manualEntityIds)
            .filterNot { it.entity.entity_id in config.hiddenEntityIds }
    }
    val batteryTypes = remember(batteries) {
        batteries.mapNotNull { it.batteryType }.distinct().sorted()
    }
    val filteredBatteries = remember(batteries, query, typeFilter, levelFilter, sortMode) {
        val needle = query.trim().lowercase(Locale.getDefault())
        sortedBatteries(
            batteries.filter { info ->
                val matchesSearch = needle.isBlank() ||
                    listOfNotNull(info.deviceName, info.entity.friendlyName, info.entity.entity_id, info.batteryType)
                        .any { it.lowercase(Locale.getDefault()).contains(needle) }
                val matchesType = typeFilter == null || info.batteryType == typeFilter
                matchesSearch && matchesType && levelMatches(info, levelFilter)
            },
            sortMode
        )
    }
    val lowCount = batteries.count { (it.level ?: 101) <= 30 }

    val settingsSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Battery Entities" to { _ ->
            val allEntities by viewModel.entities.collectAsState()
            BatterySettingsSection(
                config = config,
                batteries = batteries,
                allEntities = allEntities,
                registry = registry,
                devices = devices,
                onSave = { viewModel.updateBatteryConfig(BATTERY_PAGE_KEY, it) }
            )
        }

    BackHandler(enabled = selected != null) { selected = null }
    HKIPage(
        viewModel = viewModel,
        title = selected?.deviceName ?: "Batteries",
        subtitle = selected?.entity?.entity_id ?: "$lowCount low batteries",
        pageKey = BATTERY_PAGE_KEY,
        pageSettingsTitle = "Battery Settings",
        extraPageSettingsSection = settingsSection,
        showBadgeBar = false,
        headerBar = {
            BatteryFilters(
                query = query,
                onQueryChange = { query = it },
                batteryTypes = batteryTypes,
                selectedType = typeFilter,
                onTypeChange = { typeFilter = it },
                levelFilter = levelFilter,
                onLevelFilterChange = { levelFilter = it },
                sortMode = sortMode,
                onSortModeChange = { sortMode = it }
            )
        },
        onBack = when {
            // In a battery's detail view, back returns to the list.
            selected != null -> ({ selected = null })
            // Reached from the battery widget (or any deep link): offer a back button like a room page.
            navController?.previousBackStackEntry != null -> ({ navController.navigateUp(); Unit })
            else -> null
        }
    ) { padding ->
        if (selected != null) {
            BatteryDetail(selected!!, Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { BatteryHero(batteries, filteredBatteries.size, config.useBatteryNotes) }
                if (filteredBatteries.isEmpty()) {
                    item { BatteryEmptyState(config.useBatteryNotes) }
                } else {
                    batteryCategories.forEach { category ->
                        val grouped = filteredBatteries.filter(category.predicate)
                        if (grouped.isNotEmpty()) {
                            item { BatteryCategoryHeader(category, grouped.size) }
                            items(grouped, key = { it.entity.entity_id }) { info ->
                                BatteryRow(info, showNotes = config.useBatteryNotes, onClick = { selected = info })
                                if (isEditMode) {
                                    TextButton(onClick = {
                                        viewModel.hideBatteryEntity(BATTERY_PAGE_KEY, info.entity.entity_id)
                                    }) { Text("Hide") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryCardWidgetItem(
    widget: HKIBatteryCardWidget,
    viewModel: MainViewModel,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>,
    isEditMode: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val batteryEntityFlow = remember(viewModel) {
        viewModel.entitiesMatching("battery:auto") { it.isBatteryLevelSensor() || it.isBatteryMetadataEntity() }
    }
    val allEntities by batteryEntityFlow.collectAsState()
    val summary by produceState(
        initialValue = BatteryWidgetSummary(),
        allEntities,
        registry,
        devices,
        widget.useBatteryNotes,
        widget.lowThreshold
    ) {
        value = withContext(Dispatchers.Default) {
            val batteries = batteryEntities(allEntities, registry, devices, widget.useBatteryNotes)
            BatteryWidgetSummary(
                lowCount = batteries.count { (it.level ?: 101) <= widget.lowThreshold },
                criticalCount = batteries.count { (it.level ?: 101) <= 10 }
            )
        }
    }
    val lowCount = summary.lowCount
    val criticalCount = summary.criticalCount
    val accent = batteryColor(if (criticalCount > 0) 5 else if (lowCount > 0) 25 else 90)
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (widget.isSquare) Modifier.aspectRatio(1f) else Modifier.height(146.dp))
                .clip(RoundedCornerShape(widget.cornerRadius.dp))
                .clickable(enabled = !isEditMode, onClick = onOpen),
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = appColors.elevated
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.15f)) {
                        MdiIcon(widget.icon ?: "battery-alert", tint = accent, size = 28.dp, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(widget.title ?: "Battery Levels", color = appColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val source = if (widget.useBatteryNotes) "Battery+" else "battery sensors"
                        Text(source, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Text(
                        if (lowCount == 0) "OK" else "$lowCount",
                        color = accent,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    BatteryStatPill("Low", lowCount.toString(), accent, Modifier.weight(1f))
                    BatteryStatPill("Critical", criticalCount.toString(), batteryColor(5), Modifier.weight(1f))
                }
            }
        }
        if (isEditMode) {
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(28.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            }
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

@Composable
private fun BatteryStatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.12f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(5.dp))
            Text(label, color = LocalHKIAppColors.current.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BatteryHero(batteries: List<BatteryInfo>, visibleCount: Int, batteryPlusOnly: Boolean) {
    val appColors = LocalHKIAppColors.current
    val low = batteries.count { (it.level ?: 101) <= 30 }
    val critical = batteries.count { (it.level ?: 101) <= 10 }
    val average = batteries.mapNotNull { it.level }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val accent = batteryColor(if (critical > 0) 5 else if (low > 0) 25 else average ?: 90)
    Surface(shape = RoundedCornerShape(28.dp), color = appColors.elevated) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = 0.15f)) {
                    MdiIcon("battery-heart", tint = accent, size = 42.dp, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (batteryPlusOnly) "Battery+ batteries" else "Battery overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$visibleCount shown from ${batteries.size} batteries", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(average?.let { "$it%" } ?: "--", color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BatterySummaryTile("Critical", critical.toString(), batteryColor(5), Modifier.weight(1f))
                BatterySummaryTile("Low", low.toString(), batteryColor(25), Modifier.weight(1f))
                BatterySummaryTile("Types", batteries.mapNotNull { it.batteryType }.distinct().size.toString(), Color(0xFF29B6F6), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BatterySummaryTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.12f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = LocalHKIAppColors.current.onMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BatteryFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    batteryTypes: List<String>,
    selectedType: String?,
    onTypeChange: (String?) -> Unit,
    levelFilter: String,
    onLevelFilterChange: (String) -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val levelLabel = listOf(
        "all" to "All levels",
        "critical" to "Critical",
        "low" to "Low",
        "watch" to "Watch",
        "good" to "Good",
        "unknown" to "Unknown"
    ).firstOrNull { it.first == levelFilter }?.second ?: "All levels"
    val sortLabel = listOf("level" to "Level", "name" to "Name", "type" to "Type")
        .firstOrNull { it.first == sortMode }?.second ?: "Level"
    val activeSummary = buildList {
        if (query.isNotBlank()) add("\"${query.trim()}\"")
        selectedType?.let { add(it) }
        if (levelFilter != "all") add(levelLabel)
        if (sortMode != "level") add("Sort: $sortLabel")
    }.joinToString(" - ").ifBlank { "No filters active" }
    val hasFilters = query.isNotBlank() || selectedType != null || levelFilter != "all" || sortMode != "level"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Search & filters", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(activeSummary, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (hasFilters && !expanded) {
                TextButton(onClick = {
                    onQueryChange("")
                    onTypeChange(null)
                    onLevelFilterChange("all")
                    onSortModeChange("level")
                }) { Text("Clear") }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse filters" else "Expand filters",
                tint = appColors.onMuted
            )
        }

        if (expanded) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search batteries") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeChange(null) },
                    label = { Text("All types") },
                    shape = RoundedCornerShape(12.dp)
                )
                batteryTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf("all" to "All", "critical" to "Critical", "low" to "Low", "watch" to "Watch", "good" to "Good", "unknown" to "Unknown").forEach { (value, label) ->
                    FilterChip(
                        selected = levelFilter == value,
                        onClick = { onLevelFilterChange(value) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf("level" to "Level", "name" to "Name", "type" to "Type").forEach { (value, label) ->
                    FilterChip(
                        selected = sortMode == value,
                        onClick = { onSortModeChange(value) },
                        label = { Text("Sort: $label") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            if (hasFilters) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        onClick = {
                            onQueryChange("")
                            onTypeChange(null)
                            onLevelFilterChange("all")
                            onSortModeChange("level")
                        }
                    ) { Text("Clear filters") }
                }
            }
        }
    }
}

@Composable
private fun BatteryCategoryHeader(category: BatteryCategory, count: Int) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(category.color, RoundedCornerShape(50)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
        }
        Text(count.toString(), color = appColors.onMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BatteryEmptyState(useBatteryNotes: Boolean) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = RoundedCornerShape(22.dp), color = appColors.elevated) {
        Text(
            if (useBatteryNotes) "No Battery+ entities found. Turn off Battery+ filtering to show all battery sensors."
            else "No battery sensors found.",
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BatteryRow(info: BatteryInfo, showNotes: Boolean, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val color = batteryColor(info.level)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = appColors.elevated,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.14f)) {
                    MdiIcon(info.entity.icon?.removePrefix("mdi:") ?: "battery", tint = color, size = 26.dp, modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        info.deviceName ?: info.entity.friendlyName ?: info.entity.entity_id,
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        info.entity.friendlyName ?: info.entity.entity_id,
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.14f)) {
                    Text(
                        info.level?.let { "$it%" } ?: "--",
                        color = color,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))) {
                Box(Modifier.fillMaxWidth(((info.level ?: 0) / 100f).coerceIn(0.02f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(8.dp)))
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatteryMetaChip(info.batteryType ?: "Unknown type", Color(0xFF29B6F6))
                if (!info.quantity.isNullOrBlank()) BatteryMetaChip("Qty ${info.quantity}", Color(0xFF7E57C2))
                if (showNotes && info.fromBatteryPlus) BatteryMetaChip("Battery+", Color(0xFF26A69A))
            }
        }
    }
}

@Composable
private fun BatteryMetaChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun BatteryDetail(info: BatteryInfo, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BatteryRow(info, showNotes = true, onClick = {}) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BatteryDetailLine("Entity", info.entity.entity_id)
                BatteryDetailLine("Battery type", info.batteryType ?: "Unknown")
                BatteryDetailLine("Quantity", info.quantity ?: "Unknown")
                BatteryDetailLine("Device id", info.deviceId ?: "Unknown")
                info.notes.forEach { (name, value) -> BatteryDetailLine(name, value) }
            }
        }
    }
}

@Composable
private fun BatteryDetailLine(label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = LocalHKIAppColors.current.elevated) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = LocalHKIAppColors.current.onMuted)
            Text(value, color = LocalHKIAppColors.current.onSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BatterySettingsSection(
    config: HKIBatteryConfig,
    batteries: List<BatteryInfo>,
    allEntities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>,
    onSave: (HKIBatteryConfig) -> Unit
) {
    var useNotes by remember(config) { mutableStateOf(config.useBatteryNotes) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val batteryCandidates = remember(allEntities) {
        allEntities.filter { it.isBatteryLevelSensor() || (it.entity_id.startsWith("sensor.") && it.batteryLevel() != null) }
    }
    val deviceNames = remember(devices) {
        devices.associate { it.id to (it.name_by_user ?: it.name ?: it.id) }
    }
    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = batteryCandidates,
            title = "Select Battery Entities",
            singleSelect = false,
            preselectedIds = config.extraEntityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids ->
                onSave(config.copy(extraEntityIds = ids.distinct(), hiddenEntityIds = config.hiddenEntityIds - ids.toSet()))
                showEntityPicker = false
            }
        )
    }
    if (showDevicePicker) {
        val batteryDeviceIds = registry
            .filter { entry -> batteryCandidates.any { it.entity_id == entry.entity_id } }
            .mapNotNull { it.device_id }
            .distinct()
        val deviceEntities = batteryDeviceIds.map { id ->
            HAEntity(entity_id = id, state = "device", attributes = kotlinx.serialization.json.buildJsonObject {
                put("friendly_name", kotlinx.serialization.json.JsonPrimitive(deviceNames[id] ?: id))
            })
        }
        AdvancedEntitySearchDialog(
            allEntities = deviceEntities,
            title = "Select Battery Devices",
            singleSelect = false,
            preselectedIds = config.extraDeviceIds.toSet(),
            onDismiss = { showDevicePicker = false },
            onEntitiesSelected = { ids ->
                val deviceEntityIds = registry.filter { it.device_id in ids }.map { it.entity_id }.toSet()
                onSave(config.copy(extraDeviceIds = ids.distinct(), hiddenEntityIds = config.hiddenEntityIds - deviceEntityIds))
                showDevicePicker = false
            }
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Battery+ / Battery Notes only")
            Text("Only show Battery+ entities and include type, quantity, and related battery info.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = useNotes, onCheckedChange = { useNotes = it; onSave(config.copy(useBatteryNotes = it)) })
    }
    Text("${batteries.size} battery entities visible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { showEntityPicker = true }) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Entities")
        }
        TextButton(onClick = { showDevicePicker = true }) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Devices")
        }
    }
    config.extraEntityIds.forEach { id ->
        ManualBatteryRow(
            label = allEntities.find { it.entity_id == id }?.friendlyName ?: id,
            onRemove = { onSave(config.copy(extraEntityIds = config.extraEntityIds - id)) }
        )
    }
    config.extraDeviceIds.forEach { id ->
        ManualBatteryRow(
            label = deviceNames[id] ?: id,
            onRemove = { onSave(config.copy(extraDeviceIds = config.extraDeviceIds - id)) }
        )
    }
}

@Composable
private fun ManualBatteryRow(label: String, onRemove: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun BatteryCardWidgetSettingsDialog(
    widget: HKIBatteryCardWidget,
    onDismiss: () -> Unit,
    onSave: (HKIBatteryCardWidget) -> Unit
) {
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var threshold by remember(widget) { mutableStateOf(widget.lowThreshold.toString()) }
    var useNotes by remember(widget) { mutableStateOf(widget.useBatteryNotes) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var radius by remember(widget) { mutableStateOf(widget.cornerRadius) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery Levels") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(threshold, { threshold = it.filter(Char::isDigit).take(3) }, label = { Text("Low threshold (%)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Battery+ / Battery Notes only", modifier = Modifier.weight(1f))
                    Switch(checked = useNotes, onCheckedChange = { useNotes = it })
                }
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = radius == 8, onClick = { radius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = radius == 20, onClick = { radius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = radius == 28, onClick = { radius = 28 }, label = { Text("Round") })
                }
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(
                    title = title.ifBlank { "Battery Levels" },
                    lowThreshold = (threshold.toIntOrNull() ?: 30).coerceIn(1, 100),
                    useBatteryNotes = useNotes,
                    isSquare = isSquare,
                    cornerRadius = radius,
                    width = width
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

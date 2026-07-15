package com.example.hki7.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.data.HKISecurityConfig
import com.example.hki7.data.withDisplayName
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.*
import com.example.hki7.ui.components.surfaceGradient
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.min

private const val SECURITY_PAGE_KEY = "security"
private val SecurityBlue = Color(0xFF4A90E2)
private val SafeGreen = Color(0xFF43A047)
private val AlertRed = Color(0xFFEF5350)
private val WarningOrange = Color(0xFFFF9800)
private val PresencePurple = Color(0xFF7E57C2)
private val SecurityWindowWarm = Color(0xFFFFDF9E)

private data class SecurityGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val domains: Set<String> = setOf("binary_sensor"),
    val deviceClasses: Set<String> = emptySet()
)

// HA's security/safety binary classes plus security-related controllable domains. Groups only
// appear when at least one matching entity exists.
private val securityGroups = listOf(
    SecurityGroup("doors", "Doors", "Door contacts", Icons.Default.DoorFront, SecurityBlue, deviceClasses = setOf("door")),
    SecurityGroup("windows", "Windows", "Window contacts", Icons.Default.Window, SecurityBlue, deviceClasses = setOf("window")),
    SecurityGroup("openings", "Openings", "Garage doors and other openings", Icons.Default.Garage, SecurityBlue,
        deviceClasses = setOf("garage_door", "opening")),
    // Before "covers", so garage covers land here instead of the generic covers group.
    SecurityGroup("garage_doors", "Garage doors", "Garage door covers", Icons.Default.Garage, PresencePurple,
        domains = setOf("cover"), deviceClasses = setOf("garage", "garage_door")),
    SecurityGroup("covers", "Covers", "Blinds, shutters and gates", Icons.Default.Blinds, PresencePurple,
        domains = setOf("cover")),
    SecurityGroup("locks", "Locks", "Door and gate locks", Icons.Default.Lock, PresencePurple,
        domains = setOf("lock")),
    SecurityGroup("motion", "Motion", "Motion and vibration detection", Icons.AutoMirrored.Filled.DirectionsWalk, WarningOrange,
        deviceClasses = setOf("motion", "moving", "vibration")),
    SecurityGroup("occupancy", "Occupancy", "Occupancy sensors", Icons.Default.SensorOccupied, PresencePurple,
        deviceClasses = setOf("occupancy")),
    SecurityGroup("presence", "Presence", "People and presence trackers", Icons.Default.Person, PresencePurple,
        domains = setOf("person", "device_tracker")),
    SecurityGroup("fire", "Fire & temperature", "Fire, heat, cold and safety alarms", Icons.Default.LocalFireDepartment, AlertRed,
        deviceClasses = setOf("heat", "cold", "safety")),
    SecurityGroup("smoke", "Smoke", "Smoke detectors", Icons.Default.SmokeFree, AlertRed,
        deviceClasses = setOf("smoke")),
    SecurityGroup("gas", "Gas", "Gas and carbon monoxide", Icons.Default.GasMeter, AlertRed,
        deviceClasses = setOf("gas", "carbon_monoxide")),
    SecurityGroup("co2", "CO₂", "Carbon dioxide sensors", Icons.Default.Co2, WarningOrange,
        domains = setOf("sensor"), deviceClasses = setOf("carbon_dioxide")),
    SecurityGroup("moisture", "Leaks", "Moisture and water leaks", Icons.Default.WaterDrop, SecurityBlue,
        deviceClasses = setOf("moisture")),
    SecurityGroup("sound", "Sound", "Sound and glass-break sensors", Icons.Default.Hearing, WarningOrange,
        deviceClasses = setOf("sound")),
    SecurityGroup("light", "Light detection", "Light and beam sensors", Icons.Default.LightMode, WarningOrange,
        deviceClasses = setOf("light")),
    SecurityGroup("alarms", "Alarm systems", "Alarm control panels", Icons.Default.Security, AlertRed,
        domains = setOf("alarm_control_panel")),
    SecurityGroup("cameras", "Cameras", "Camera feeds", Icons.Default.Videocam, SecurityBlue,
        domains = setOf("camera"))
)

private fun SecurityGroup.autoMatches(entity: HAEntity): Boolean {
    val domain = entity.entity_id.substringBefore('.')
    if (domain !in domains) return false
    return deviceClasses.isEmpty() || entity.deviceClass in deviceClasses
}

internal fun HAEntity.isAutoSecurityEntityFor(groupKey: String): Boolean =
    securityGroups.firstOrNull { it.key == groupKey }?.autoMatches(this) == true

private fun List<HAEntity>.securityOrder(order: List<String>) = sortedWith(
    compareBy<HAEntity> { order.indexOf(it.entity_id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        .thenBy { it.friendlyName ?: it.entity_id }
)

private fun HAEntity.isSecurityActive(): Boolean = state.lowercase() in setOf(
    "on", "open", "detected", "home", "locked", "triggered", "unlocked", "opening", "closing"
)

internal enum class SecurityAlarmMode {
    TRIGGERED, PENDING, TRANSITIONING, ARMED_HOME, ARMED_AWAY, ARMED_NIGHT, ARMED_OTHER, DISARMED, NONE
}

internal data class SecuritySceneState(
    val total: Int,
    val cameras: Int,
    val onlineCameras: Int,
    val openingEntityCount: Int,
    val openDoors: Int,
    val openWindows: Int,
    val openGarageDoors: Int,
    val securityCoverCount: Int,
    val openCovers: Int,
    val lockCount: Int,
    val lockedLocks: Int,
    val unlockedLocks: Int,
    val jammedLocks: Int,
    val motionCount: Int,
    val sensorActivityCount: Int,
    val peopleHome: Int,
    val safetyAlerts: Int,
    val leakAlerts: Int,
    val unavailableCount: Int,
    val alarmCount: Int,
    val availableAlarmCount: Int,
    val alarmMode: SecurityAlarmMode
) {
    val openingCount: Int get() = openDoors + openWindows + openGarageDoors + openCovers
    val alertCount: Int get() = safetyAlerts + leakAlerts + jammedLocks + if (alarmMode == SecurityAlarmMode.TRIGGERED) 1 else 0
    val isArmed: Boolean get() = alarmMode in setOf(
        SecurityAlarmMode.ARMED_HOME,
        SecurityAlarmMode.ARMED_AWAY,
        SecurityAlarmMode.ARMED_NIGHT,
        SecurityAlarmMode.ARMED_OTHER
    )
}

private fun String.securityState() = trim().lowercase(Locale.ROOT).replace('-', '_').replace(' ', '_')

private fun HAEntity.isSecurityAvailable(): Boolean =
    state.securityState() !in setOf("unknown", "unavailable")

private fun HAEntity.isSecurityCameraOnline(): Boolean =
    isSecurityAvailable() && state.securityState() !in setOf("off", "disabled")

private fun HAEntity.isOpenSecurityEntity(): Boolean {
    if (!isSecurityAvailable()) return false
    val normalized = state.securityState()
    if (normalized in setOf("on", "open", "opening", "closing")) return true
    return attributes?.get("current_position")?.jsonPrimitive?.intOrNull?.let { it > 0 } == true
}

private fun HAEntity.isTriggeredSecurityEntity(): Boolean =
    isSecurityAvailable() && state.securityState() in setOf(
        "on", "active", "detected", "motion", "occupied",
        "triggered", "alarm", "alert", "unsafe", "problem",
        "wet", "leak", "leaking", "smoke", "fire", "gas"
    )

/** Build a semantic snapshot for the hero from the already filtered/configured Security groups. */
internal fun Map<String, List<HAEntity>>.toSecuritySceneState(): SecuritySceneState {
    fun entities(vararg keys: String): List<HAEntity> = keys.flatMap { this[it].orEmpty() }.distinctBy { it.entity_id }

    val cameras = entities("cameras")
    val nonCameraEntities = filterKeys { it != "cameras" }.values.flatten().distinctBy { it.entity_id }
    val genericOpenings = entities("openings")
    // Binary garage-door contacts are in the `openings` group, while controllable garage doors
    // have their own group. Keep both on the garage animation rather than treating the contact
    // as the illustrated front door.
    val garageDoorContacts = genericOpenings.filter { it.deviceClass == "garage_door" }
    val doors = (entities("doors") + genericOpenings.filterNot { it.deviceClass == "garage_door" })
        .distinctBy { it.entity_id }
    val windows = entities("windows")
    val garageDoorCovers = entities("garage_doors")
    // Blinds and ordinary shades are useful on the Security page but are not entry points. Only
    // door/gate covers participate in the hero's perimeter opening state.
    val covers = entities("covers").filter { it.deviceClass in setOf("door", "gate") }
    val allLocks = entities("locks")
    val locks = allLocks.filter(HAEntity::isSecurityAvailable)
    val motion = entities("motion").count(HAEntity::isTriggeredSecurityEntity)
    val sensorActivity = entities("sound", "light").count(HAEntity::isTriggeredSecurityEntity)
    val occupancy = entities("occupancy").count(HAEntity::isTriggeredSecurityEntity)
    val presence = entities("presence").count {
        it.isSecurityAvailable() && it.state.securityState() in setOf("home", "on", "present", "occupied")
    }
    val safetyAlerts = entities("fire", "smoke", "gas").count(HAEntity::isTriggeredSecurityEntity)
    val leakAlerts = entities("moisture").count(HAEntity::isTriggeredSecurityEntity)
    val alarms = entities("alarms")
    val availableAlarms = alarms.filter(HAEntity::isSecurityAvailable)
    val alarmStates = availableAlarms.map { it.state.securityState() }
    val alarmMode = when {
        alarmStates.any { it == "triggered" } -> SecurityAlarmMode.TRIGGERED
        alarmStates.any { it == "pending" } -> SecurityAlarmMode.PENDING
        alarmStates.any { it in setOf("arming", "disarming") } -> SecurityAlarmMode.TRANSITIONING
        alarmStates.any { it == "armed_away" } -> SecurityAlarmMode.ARMED_AWAY
        alarmStates.any { it == "armed_night" } -> SecurityAlarmMode.ARMED_NIGHT
        alarmStates.any { it == "armed_home" } -> SecurityAlarmMode.ARMED_HOME
        alarmStates.any { it.startsWith("armed_") || it == "armed" } -> SecurityAlarmMode.ARMED_OTHER
        alarmStates.any { it == "disarmed" } -> SecurityAlarmMode.DISARMED
        else -> SecurityAlarmMode.NONE
    }
    val lockedLocks = locks.count { it.state.securityState() == "locked" }
    val jammedLocks = locks.count { it.state.securityState() in setOf("jammed", "fault", "problem") }
    // Keep faulted locks separate from unlocked ones: both need attention, but a jam is an alert
    // rather than an ordinary unlocked state. Unknown integration-specific states are not assumed
    // unsafe unless they explicitly describe an unsecured or transitional lock.
    val unlockedLocks = locks.count {
        it.state.securityState() in setOf("unlocked", "unlocking", "locking", "open", "opening", "not_locked")
    }
    // A garage often exposes both a cover and a binary contact for the same physical door. Using
    // the larger category count avoids presenting that common pair as two separate openings.
    val openGarageDoors = maxOf(
        garageDoorContacts.count(HAEntity::isOpenSecurityEntity),
        garageDoorCovers.count(HAEntity::isOpenSecurityEntity)
    )
    val garageDoorCount = maxOf(garageDoorContacts.size, garageDoorCovers.size)

    return SecuritySceneState(
        total = nonCameraEntities.size,
        cameras = cameras.size,
        onlineCameras = cameras.count(HAEntity::isSecurityCameraOnline),
        openingEntityCount = doors.size + windows.size + garageDoorCount + covers.size,
        openDoors = doors.count(HAEntity::isOpenSecurityEntity),
        openWindows = windows.count(HAEntity::isOpenSecurityEntity),
        openGarageDoors = openGarageDoors,
        securityCoverCount = covers.size,
        openCovers = covers.count(HAEntity::isOpenSecurityEntity),
        lockCount = allLocks.size,
        lockedLocks = lockedLocks,
        unlockedLocks = unlockedLocks,
        jammedLocks = jammedLocks,
        motionCount = motion,
        sensorActivityCount = sensorActivity,
        peopleHome = presence + occupancy,
        safetyAlerts = safetyAlerts,
        leakAlerts = leakAlerts,
        unavailableCount = (nonCameraEntities + cameras).distinctBy { it.entity_id }.count { !it.isSecurityAvailable() },
        alarmCount = alarms.size,
        availableAlarmCount = availableAlarms.size,
        alarmMode = alarmMode
    )
}

@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val uiRevision by viewModel.uiRevision.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val config = (pageConfigs[SECURITY_PAGE_KEY] ?: HKIPageConfig()).securityConfig ?: HKISecurityConfig()
    val dependencies = remember(config) { config.extraEntityIds.values.flatten().toSet() }
    val flow = remember(viewModel, dependencies) {
        viewModel.entitiesMatching { entity ->
            val domain = entity.entity_id.substringBefore('.')
            domain in setOf("binary_sensor", "sensor", "cover", "lock", "person", "device_tracker", "alarm_control_panel", "camera") ||
                entity.entity_id in dependencies
        }
    }
    val rawEntities by flow.collectAsState()
    val entities = remember(rawEntities, config.customNames) {
        rawEntities.map { it.withDisplayName(config.customNames[it.entity_id]) }
    }
    val byId = remember(entities) { entities.associateBy { it.entity_id } }
    val hidden = remember(config) { config.hiddenEntityIds.toSet() }
    val grouped = remember(entities, config) {
        val claimed = mutableSetOf<String>()
        securityGroups.associate { group ->
            val automatic = if (config.manualOnly) emptyList() else entities.filter { it.isAutoSecurityEntityFor(group.key) }
            val manual = config.extraEntityIds[group.key].orEmpty().mapNotNull(byId::get)
            group.key to (automatic + manual).distinctBy { it.entity_id }
                .filterNot { it.entity_id in hidden || it.entity_id in claimed }
                .securityOrder(config.entityOrder)
                .also { list -> claimed += list.map { it.entity_id } }
        }
    }
    var page by rememberSaveable { mutableStateOf("security") }
    var entitySearch by rememberSaveable { mutableStateOf("") }
    var entitySort by rememberSaveable { mutableStateOf("custom") }
    BackHandler(page != "security") { page = "security" }
    LaunchedEffect(page) { entitySearch = "" }
    var cameraSettingsEntity by remember { mutableStateOf<HAEntity?>(null) }
    cameraSettingsEntity?.let { entity ->
        // Same settings dialog as camera widgets (name, refresh interval).
        ButtonConfigDialog(
            entity = entity,
            config = config.cameraConfigs[entity.entity_id] ?: HKIButtonConfig(),
            isCameraItem = true,
            allEntities = entities,
            onDismiss = { cameraSettingsEntity = null },
            onSave = { cfg ->
                viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, config.copy(cameraConfigs = config.cameraConfigs + (entity.entity_id to cfg)))
                cameraSettingsEntity = null
            }
        )
    }
    var renameEntity by remember { mutableStateOf<HAEntity?>(null) }
    renameEntity?.let { entity ->
        SecurityCardSettingsDialog(
            currentName = config.customNames[entity.entity_id].orEmpty(),
            defaultName = entity.friendlyName ?: entity.entity_id,
            currentIcon = config.customIcons[entity.entity_id].orEmpty(),
            onDismiss = { renameEntity = null }
        ) { name, icon ->
            val names = if (name == null) config.customNames - entity.entity_id else config.customNames + (entity.entity_id to name)
            val icons = if (icon == null) config.customIcons - entity.entity_id else config.customIcons + (entity.entity_id to icon)
            viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, config.copy(customNames = names, customIcons = icons))
            renameEntity = null
        }
    }

    fun remove(id: String) = viewModel.hideSecurityEntity(SECURITY_PAGE_KEY, id)
    fun reorder(items: List<HAEntity>, from: Int, to: Int) {
        val ids = items.map { it.entity_id }.toMutableList().apply { add(to, removeAt(from)) }
        viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, config.copy(entityOrder = ids + config.entityOrder.filterNot(ids::contains)))
    }

    val activeGroup = securityGroups.find { it.key == page }
    fun filteredEntities(source: List<HAEntity>): List<HAEntity> {
        val needle = entitySearch.trim().lowercase()
        val filtered = if (needle.isBlank()) source else source.filter { entity ->
            (entity.friendlyName ?: entity.entity_id).lowercase().contains(needle)
        }
        return when (entitySort) {
            "name_asc" -> filtered.sortedBy { (it.friendlyName ?: it.entity_id).lowercase() }
            "name_desc" -> filtered.sortedByDescending { (it.friendlyName ?: it.entity_id).lowercase() }
            else -> filtered
        }
    }
    val settings: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Security Entities" to { setBack ->
            SecurityEntitySettings(config, entities,
                onSave = { viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, it) }, setBack = setBack)
        }
    var showSecurityReimport by remember { mutableStateOf(false) }
    val importSettings: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Re-import" to { _ ->
            Text("Fetch security entities from Home Assistant again.", color = LocalHKIAppColors.current.onMuted)
            Button(onClick = { showSecurityReimport = true }, modifier = Modifier.fillMaxWidth()) { Text("Re-import Security") }
        }
    if (showSecurityReimport) {
        AlertDialog(
            onDismissRequest = { showSecurityReimport = false },
            title = { Text("Re-import security") },
            text = { Text("Import entities that have not been edited, or remove all security edits and import from scratch.") },
            confirmButton = { Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { viewModel.reimportSecurity(false); showSecurityReimport = false }) { Text("Import unedited") }
                TextButton(onClick = { viewModel.reimportSecurity(true); showSecurityReimport = false }) { Text("Remove edits and import all", color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(onClick = { showSecurityReimport = false }) { Text("Cancel") } }
        )
    }

    HKIPage(
        viewModel = viewModel,
        title = activeGroup?.title ?: "Security",
        subtitle = activeGroup?.subtitle ?: "Home status and activity",
        pageKey = SECURITY_PAGE_KEY,
        pageSettingsTitle = "Security Settings",
        extraPageSettingsSection = settings,
        additionalPageSettingsSections = listOf(importSettings),
        showBadgeBar = false,
        headerBar = if (activeGroup != null) ({
            SecurityEntitySearchBar(entitySearch, { entitySearch = it }, entitySort, { entitySort = it })
        }) else null,
        onBack = if (activeGroup != null) ({ page = "security" }) else null
    ) { padding -> key(uiRevision) {
        if (activeGroup != null) {
            SecurityGroupPage(activeGroup, filteredEntities(grouped[activeGroup.key].orEmpty()), viewModel, currentUrl, config.customIcons, config.cameraConfigs,
                isEditMode, ::remove,
                { if (it.entity_id.startsWith("camera.")) cameraSettingsEntity = it else renameEntity = it },
                { from, to -> reorder(filteredEntities(grouped[activeGroup.key].orEmpty()), from, to) }, padding)
        } else {
            SecurityOverview(grouped, viewModel, currentUrl, config.cameraConfigs, isEditMode, ::remove, { cameraSettingsEntity = it }, { page = it }, padding)
        }
    } }
}

@Composable
private fun SecurityEntitySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val sortLabel = when (sortMode) { "name_asc" -> "Name A-Z"; "name_desc" -> "Name Z-A"; else -> "Custom order" }
    val summary = buildList {
        if (query.isNotBlank()) add("\"${query.trim()}\"")
        if (sortMode != "custom") add(sortLabel)
    }.joinToString(" - ").ifBlank { "No search active · $sortLabel" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(itemCornerShape()).clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Search & sort", color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(summary, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = appColors.onMuted)
        }
        if (expanded) {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, label = { Text("Search by name") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf("custom" to "Custom order", "name_asc" to "Name A-Z", "name_desc" to "Name Z-A").forEach { (value, label) ->
                    FilterChip(selected = sortMode == value, onClick = { onSortModeChange(value) }, label = { Text(label) }, shape = itemCornerShape())
                }
            }
            if (query.isNotBlank()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { onQueryChange("") }) { Text("Clear search") }
            }
        }
    }
}

@Composable
private fun SecurityOverview(
    grouped: Map<String, List<HAEntity>>, viewModel: MainViewModel, currentUrl: String,
    cameraConfigs: Map<String, HKIButtonConfig>, isEditMode: Boolean, onRemove: (String) -> Unit,
    onCameraSettings: (HAEntity) -> Unit, onOpen: (String) -> Unit, padding: PaddingValues
) {
    val all = grouped.filterKeys { it != "cameras" }.values.flatten().distinctBy { it.entity_id }
    val cameras = grouped["cameras"].orEmpty()
    val sceneState = remember(grouped) { grouped.toSecuritySceneState() }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current)) {
        item { SecurityHero(sceneState) }
        item {
            // Cameras get their own full-width section below instead of a tile.
            val present = securityGroups.filter { it.key != "cameras" && grouped[it.key].orEmpty().isNotEmpty() }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                present.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { group ->
                            val items = grouped[group.key].orEmpty()
                            SecurityTile(group, items.size, items.count(HAEntity::isSecurityActive), Modifier.weight(1f)) { onOpen(group.key) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (cameras.isNotEmpty()) {
            item { SecuritySectionHeader("Cameras", cameras.size.toString()) }
            items(cameras.size, key = { cameras[it].entity_id }) { index ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SecurityCameraCard(cameras[index], viewModel, currentUrl, cameraConfigs[cameras[index].entity_id])
                    if (isEditMode) {
                        EditSettingsButton({ onCameraSettings(cameras[index]) }, Modifier.align(Alignment.Center))
                        EditRemoveBadge({ onRemove(cameras[index].entity_id) }, Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
        if (all.isEmpty() && cameras.isEmpty()) item { EmptyEditHint(Modifier.fillParentMaxHeight()) }
    }
}

@Composable
private fun SecurityHero(state: SecuritySceneState) {
    val colors = LocalHKIAppColors.current
    val hasData = state.total > 0 || state.cameras > 0
    val alarmUnavailable = state.alarmCount > 0 && state.availableAlarmCount == 0
    val needsAttention = state.openingCount > 0 || state.unlockedLocks > 0 ||
        (state.lockCount > 0 && state.lockedLocks < state.lockCount)
    val camerasNeedAttention = state.isArmed && state.cameras > state.onlineCameras
    val color = when {
        state.alertCount > 0 -> AlertRed
        state.alarmMode == SecurityAlarmMode.PENDING -> WarningOrange
        state.alarmMode == SecurityAlarmMode.TRANSITIONING -> WarningOrange
        needsAttention -> WarningOrange
        state.motionCount > 0 -> WarningOrange
        state.sensorActivityCount > 0 -> WarningOrange
        alarmUnavailable -> WarningOrange
        state.unavailableCount > 0 -> WarningOrange
        camerasNeedAttention -> WarningOrange
        state.isArmed -> SafeGreen
        state.alarmMode == SecurityAlarmMode.DISARMED -> SecurityBlue
        hasData -> SafeGreen
        else -> colors.onMuted
    }
    val status = when {
        state.alertCount > 0 -> "Security alert"
        state.alarmMode == SecurityAlarmMode.PENDING -> "Alarm pending"
        state.alarmMode == SecurityAlarmMode.TRANSITIONING -> "Changing alarm mode"
        needsAttention -> "Needs attention"
        state.motionCount > 0 -> "Motion detected"
        state.sensorActivityCount > 0 -> "Sensor activity"
        alarmUnavailable -> "Alarm unavailable"
        state.unavailableCount > 0 -> "Devices unavailable"
        camerasNeedAttention -> "Cameras offline"
        state.isArmed -> "All secure"
        state.alarmMode == SecurityAlarmMode.DISARMED -> "Alarm disarmed"
        hasData -> "All clear"
        else -> "No security data"
    }
    val alarmLabel = when (state.alarmMode) {
        SecurityAlarmMode.TRIGGERED -> "Alarm triggered"
        SecurityAlarmMode.PENDING -> "Alarm pending"
        SecurityAlarmMode.TRANSITIONING -> "Changing mode"
        SecurityAlarmMode.ARMED_HOME -> "Armed home"
        SecurityAlarmMode.ARMED_AWAY -> "Armed away"
        SecurityAlarmMode.ARMED_NIGHT -> "Armed night"
        SecurityAlarmMode.ARMED_OTHER -> "Armed"
        SecurityAlarmMode.DISARMED -> "Disarmed"
        SecurityAlarmMode.NONE -> if (alarmUnavailable) "Alarm unavailable" else "No alarm"
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(274.dp)) {
            SecurityHouseScene(state = state, accent = color, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.weight(1.15f),
                    color = colors.surface.copy(alpha = 0.88f),
                    shape = itemCornerShape()
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("HOME SECURITY", style = MaterialTheme.typography.labelSmall, color = colors.onMuted, fontWeight = FontWeight.SemiBold)
                        Text(
                            status,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(0.85f),
                    color = color.copy(alpha = 0.16f),
                    shape = itemCornerShape()
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            alarmLabel,
                            color = colors.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            SecurityHeroStat(
                Icons.Default.SensorDoor,
                when {
                    state.openingEntityCount == 0 -> colors.onMuted
                    state.openingCount > 0 -> WarningOrange
                    else -> SafeGreen
                },
                state.openingCount.toString(),
                "Openings",
                Modifier.weight(1f)
            )
            SecurityHeroStat(
                Icons.Default.Lock,
                when {
                    state.lockCount == 0 -> colors.onMuted
                    state.jammedLocks > 0 -> AlertRed
                    state.lockedLocks < state.lockCount -> WarningOrange
                    else -> SafeGreen
                },
                if (state.lockCount > 0) "${state.lockedLocks}/${state.lockCount}" else "—",
                "Locked",
                Modifier.weight(1f)
            )
            SecurityHeroStat(
                Icons.Default.Videocam,
                when {
                    state.cameras == 0 -> colors.onMuted
                    state.onlineCameras < state.cameras -> WarningOrange
                    else -> SafeGreen
                },
                if (state.cameras > 0) "${state.onlineCameras}/${state.cameras}" else "—",
                "Cameras online",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SecurityHeroStat(
    icon: ImageVector,
    color: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Text(value, color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            label,
            color = appColors.onMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecurityHouseScene(
    state: SecuritySceneState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val dark = appColors.background.luminance() < 0.5f
    val doorOpen by animateFloatAsState(
        if (state.openDoors > 0) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "securityDoor"
    )
    val windowOpen by animateFloatAsState(
        if (state.openWindows > 0) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "securityWindow"
    )
    val garageOpen by animateFloatAsState(
        if (state.openGarageDoors > 0) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "securityGarage"
    )
    val gateOpen by animateFloatAsState(
        if (state.openCovers > 0) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "securityGate"
    )
    val infinite = rememberInfiniteTransition(label = "securityHouse")
    val phase by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "securityPhase"
    )
    val pulse by infinite.animateFloat(
        0.58f, 1f,
        infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "securityPulse"
    )
    val scanSwing by infinite.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(3300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cameraScan"
    )

    Canvas(modifier) {
        val designWidth = 900f
        val designHeight = 620f
        val scale = min(size.width / designWidth, size.height / designHeight)
        if (scale <= 0f) return@Canvas
        val originX = (size.width - designWidth * scale) / 2f
        val originY = size.height - designHeight * scale
        fun point(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)
        fun d(value: Float) = value * scale
        fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
        }
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

        val leftWall = if (dark) Color(0xFF303E47) else Color(0xFFD7E0E5)
        val rightWall = if (dark) Color(0xFF3B4B55) else Color(0xFFEDF2F4)
        val gableFace = if (dark) Color(0xFF35444D) else Color(0xFFE5ECEF)
        val roofFront = if (dark) Color(0xFF1B272E) else Color(0xFFA9B7BF)
        val roofBack = if (dark) Color(0xFF26343C) else Color(0xFFC0CBD1)
        val glass = if (dark) Color(0xFF172C35) else Color(0xFFAEC6D0)
        val frame = if (dark) Color(0xFF91A3AC) else Color(0xFF687982)
        val edge = if (dark) Color(0xFF7C909A) else Color.White.copy(alpha = 0.82f)
        val doorColor = if (dark) Color(0xFF273840) else Color(0xFFB4C3CA)
        val garageColor = if (dark) Color(0xFF44545D) else Color(0xFFBCC8CE)
        val deep = if (dark) Color(0xFF111C21) else Color(0xFF596B74)
        val cameraBody = if (dark) Color(0xFF52636C) else Color(0xFFA9B8BF)

        val fc = point(515f, 520f)
        val leftAxis = Offset(d(-330f), d(-55f))
        val rightAxis = Offset(d(235f), d(-65f))
        val wallHeight = d(180f)
        val fcTop = fc + Offset(0f, -wallHeight)
        val leftBottom = fc + leftAxis
        val leftTop = fcTop + leftAxis
        val rightBottom = fc + rightAxis
        val rightTop = fcTop + rightAxis
        val apex = point(632f, 220f)
        val backApex = apex + leftAxis
        val backRightTop = rightTop + leftAxis
        fun leftFace(u: Float, v: Float) = fc + leftAxis * u + Offset(0f, -wallHeight * v)
        fun rightFace(u: Float, v: Float) = fc + rightAxis * u + Offset(0f, -wallHeight * v)
        fun roof(u: Float, v: Float) = fcTop + leftAxis * u + (apex - fcTop) * v

        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = (if (dark) 0.10f else 0.07f) * pulse), Color.Transparent),
                center = point(500f, 360f), radius = d(390f)
            ),
            radius = d(390f), center = point(500f, 360f)
        )

        drawOval(
            Color.Black.copy(alpha = if (dark) 0.32f else 0.10f),
            topLeft = point(85f, 488f), size = Size(d(745f), d(94f))
        )

        val perimeter = listOf(
            point(70f, 490f), point(355f, 600f), point(875f, 515f),
            point(600f, 400f), point(70f, 490f)
        )
        val perimeterPath = Path().apply {
            moveTo(perimeter.first().x, perimeter.first().y)
            perimeter.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val perimeterColor = when {
            state.alarmMode == SecurityAlarmMode.TRIGGERED || state.alertCount > 0 -> AlertRed
            state.alarmMode in setOf(SecurityAlarmMode.PENDING, SecurityAlarmMode.TRANSITIONING) -> WarningOrange
            state.alarmCount > 0 && state.availableAlarmCount == 0 -> WarningOrange
            state.isArmed -> SafeGreen
            else -> frame
        }
        val perimeterActive = state.isArmed ||
            state.alarmMode in setOf(SecurityAlarmMode.PENDING, SecurityAlarmMode.TRANSITIONING) ||
            state.alertCount > 0 ||
            (state.alarmCount > 0 && state.availableAlarmCount == 0)
        drawPath(
            perimeterPath,
            perimeterColor.copy(alpha = if (perimeterActive) 0.34f else 0.12f),
            style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
        )
        if (perimeterActive) {
            repeat(3) { index ->
                val head = (phase + index / 3f) % 1f
                repeat(3) { trail ->
                    val t = (head - trail * 0.025f).let { if (it < 0f) it + 1f else it }
                    drawCircle(
                        perimeterColor.copy(alpha = 0.88f - trail * 0.24f),
                        d(5f - trail * 0.8f),
                        pointAlong(perimeter, t)
                    )
                }
            }
        }

        // Main shell: one coherent isometric structure, shared by every security fixture.
        drawPath(quad(apex, rightTop, backRightTop, backApex), roofBack)
        drawPath(quad(fc, leftBottom, leftTop, fcTop), leftWall)
        drawPath(quad(fc, rightBottom, rightTop, fcTop), rightWall)
        drawPath(Path().apply {
            moveTo(fcTop.x, fcTop.y); lineTo(apex.x, apex.y); lineTo(rightTop.x, rightTop.y); close()
        }, gableFace)
        drawLine(frame.copy(alpha = 0.28f), leftBottom, fc, 1.2.dp.toPx())
        drawLine(frame.copy(alpha = 0.28f), fc, rightBottom, 1.2.dp.toPx())

        // Living-room window. Presence gives it a restrained warm, inhabited glow.
        val livingWindow = quad(
            leftFace(0.08f, 0.22f), leftFace(0.45f, 0.22f),
            leftFace(0.45f, 0.68f), leftFace(0.08f, 0.68f)
        )
        drawPath(livingWindow, if (state.peopleHome > 0) SecurityWindowWarm.copy(alpha = 0.72f + 0.18f * pulse) else glass)
        drawPath(livingWindow, frame.copy(alpha = 0.76f), style = Stroke(1.3.dp.toPx()))
        drawLine(frame.copy(alpha = 0.58f), leftFace(0.265f, 0.22f), leftFace(0.265f, 0.68f), 1.dp.toPx())
        drawLine(frame.copy(alpha = 0.50f), leftFace(0.08f, 0.45f), leftFace(0.45f, 0.45f), 1.dp.toPx())
        drawPath(
            quad(leftFace(0.12f, 0.23f), leftFace(0.38f, 0.23f), leftFace(0.38f, 0.32f), leftFace(0.12f, 0.32f)),
            deep.copy(alpha = 0.48f)
        )

        // Integrated garage. The slatted panel physically lifts when any configured garage opens.
        val garageBottom = 0.08f
        val garageTop = 0.70f
        val garageOpening = quad(
            leftFace(0.54f, garageBottom), leftFace(0.94f, garageBottom),
            leftFace(0.94f, garageTop), leftFace(0.54f, garageTop)
        )
        drawPath(garageOpening, deep.copy(alpha = 0.92f))
        drawPath(garageOpening, frame.copy(alpha = 0.72f), style = Stroke(1.3.dp.toPx()))
        val panelBottom = garageBottom + (garageTop - garageBottom) * garageOpen
        if (panelBottom < garageTop - 0.01f) {
            val panel = quad(
                leftFace(0.54f, panelBottom), leftFace(0.94f, panelBottom),
                leftFace(0.94f, garageTop), leftFace(0.54f, garageTop)
            )
            drawPath(panel, garageColor)
            val remaining = garageTop - panelBottom
            for (line in 1..4) {
                val v = panelBottom + remaining * line / 5f
                drawLine(deep.copy(alpha = 0.38f), leftFace(0.54f, v), leftFace(0.94f, v), 1.dp.toPx())
            }
        }

        // Stone path from the front door to the foreground.
        drawPath(
            quad(rightFace(0.10f, 0f), rightFace(0.34f, 0f), point(735f, 584f), point(615f, 604f)),
            if (dark) Color(0xFF45535B).copy(alpha = 0.70f) else Color(0xFFC5CED2).copy(alpha = 0.82f)
        )
        listOf(0.28f, 0.56f, 0.82f).forEach { t ->
            val left = rightFace(0.10f, 0f) + (point(615f, 604f) - rightFace(0.10f, 0f)) * t
            val right = rightFace(0.34f, 0f) + (point(735f, 584f) - rightFace(0.34f, 0f)) * t
            drawLine(frame.copy(alpha = 0.24f), left, right, 0.8.dp.toPx())
        }

        // Front door and smart lock. The leaf narrows and moves outward as it opens.
        val doorway = quad(
            rightFace(0.08f, 0.02f), rightFace(0.34f, 0.02f),
            rightFace(0.34f, 0.65f), rightFace(0.08f, 0.65f)
        )
        drawPath(doorway, deep.copy(alpha = 0.94f))
        val hingeBottom = rightFace(0.08f, 0.02f)
        val hingeTop = rightFace(0.08f, 0.65f)
        val farU = 0.34f - doorOpen * 0.17f
        val doorOffset = Offset(d(20f * doorOpen), d(8f * doorOpen))
        val doorLeaf = quad(
            hingeBottom, rightFace(farU, 0.02f) + doorOffset,
            rightFace(farU, 0.65f) + doorOffset, hingeTop
        )
        drawPath(doorLeaf, doorColor)
        drawPath(doorLeaf, frame.copy(alpha = 0.50f), style = Stroke(1.dp.toPx()))
        val doorPane = quad(
            rightFace(0.11f, 0.43f), rightFace(farU - 0.03f, 0.43f) + doorOffset,
            rightFace(farU - 0.03f, 0.58f) + doorOffset, rightFace(0.11f, 0.58f)
        )
        drawPath(doorPane, if (state.peopleHome > 0) SecurityWindowWarm.copy(alpha = 0.58f) else glass)
        drawCircle(frame, d(3f), rightFace(farU - 0.015f, 0.27f) + doorOffset)

        val lockStateColor = when {
            state.jammedLocks > 0 -> AlertRed
            state.unlockedLocks > 0 || state.openDoors > 0 -> WarningOrange
            state.lockCount > 0 && state.lockedLocks == state.lockCount -> SafeGreen
            state.lockCount > 0 -> WarningOrange
            else -> frame
        }
        val keypad = rightFace(0.37f, 0.43f)
        drawRoundRect(
            deep.copy(alpha = 0.88f),
            topLeft = keypad - Offset(d(10f), d(14f)),
            size = Size(d(20f), d(28f)), cornerRadius = CornerRadius(d(4f), d(4f))
        )
        drawCircle(lockStateColor.copy(alpha = 0.55f + 0.45f * pulse), d(3.5f), keypad - Offset(0f, d(6f)))
        drawLine(frame.copy(alpha = 0.70f), keypad + Offset(d(-4f), d(3f)), keypad + Offset(d(4f), d(3f)), 1.dp.toPx())

        // Framed front window; one pane visibly cracks open when a window contact is open.
        val frontWindow = quad(
            rightFace(0.48f, 0.24f), rightFace(0.84f, 0.24f),
            rightFace(0.84f, 0.68f), rightFace(0.48f, 0.68f)
        )
        drawPath(frontWindow, if (state.peopleHome > 0) SecurityWindowWarm.copy(alpha = 0.60f + 0.16f * pulse) else glass)
        drawPath(frontWindow, frame.copy(alpha = 0.76f), style = Stroke(1.3.dp.toPx()))
        drawLine(frame.copy(alpha = 0.58f), rightFace(0.66f, 0.24f), rightFace(0.66f, 0.68f), 1.dp.toPx())
        drawLine(frame.copy(alpha = 0.50f), rightFace(0.48f, 0.46f), rightFace(0.84f, 0.46f), 1.dp.toPx())
        if (windowOpen > 0.01f) {
            val paneOffset = Offset(d(18f * windowOpen), d(4f * windowOpen))
            val openPane = quad(
                rightFace(0.66f, 0.24f), rightFace(0.84f, 0.24f) + paneOffset,
                rightFace(0.84f, 0.68f) + paneOffset, rightFace(0.66f, 0.68f)
            )
            drawPath(openPane, glass.copy(alpha = 0.76f))
            drawPath(openPane, WarningOrange.copy(alpha = 0.82f), style = Stroke(1.2.dp.toPx()))
        }

        // Low landscaping helps the scene feel inhabited and grounds the path.
        listOf(point(430f, 530f), point(470f, 538f), point(770f, 520f), point(804f, 526f)).forEachIndexed { index, center ->
            drawCircle(
                (if (dark) Color(0xFF29473A) else Color(0xFF7F9E86)).copy(alpha = 0.88f),
                d(if (index % 2 == 0) 16f else 12f), center
            )
        }

        // Camera scan cone is tied to armed/motion state, never decorative idle motion.
        val cameraCenter = rightFace(0.87f, 0.76f)
        val cameraScanning = state.onlineCameras > 0 && (
            state.isArmed ||
                state.alarmMode == SecurityAlarmMode.PENDING ||
                state.motionCount > 0 ||
                state.sensorActivityCount > 0
            )
        if (cameraScanning) {
            val scanColor = if (state.motionCount > 0) WarningOrange else SecurityBlue
            val target = point(805f + scanSwing * 45f, 565f)
            val cone = Path().apply {
                moveTo(cameraCenter.x, cameraCenter.y)
                lineTo(target.x - d(58f), target.y + d(8f))
                lineTo(target.x + d(58f), target.y - d(8f))
                close()
            }
            drawPath(
                cone,
                Brush.linearGradient(
                    listOf(scanColor.copy(alpha = 0.16f), Color.Transparent),
                    start = cameraCenter, end = target
                )
            )
        }

        // Roof, eaves and chimney complete the silhouette after façade details.
        drawPath(quad(roof(-0.035f, -0.05f), roof(1.035f, -0.05f), roof(1.035f, 1.04f), roof(-0.035f, 1.04f)), roofFront)
        listOf(0.33f, 0.66f).forEach { u ->
            drawLine(edge.copy(alpha = 0.17f), roof(u, 0.02f), roof(u, 0.98f), 0.8.dp.toPx())
        }
        drawLine(edge, roof(-0.035f, -0.05f), roof(-0.035f, 1.04f), 1.5.dp.toPx())
        drawLine(edge.copy(alpha = 0.84f), roof(-0.035f, 1.04f), roof(1.035f, 1.04f), 1.2.dp.toPx())
        val chimneyBase = roof(0.68f, 0.60f)
        val chimneyLeft = chimneyBase + Offset(d(-15f), 0f)
        val chimneyRight = chimneyBase + Offset(d(14f), d(2f))
        val chimneyUp = Offset(0f, d(-45f))
        drawPath(quad(chimneyLeft, chimneyRight, chimneyRight + chimneyUp, chimneyLeft + chimneyUp), if (dark) Color(0xFF46565F) else Color(0xFF82949D))

        // Eave camera fixture and status LED.
        if (state.cameras > 0) {
            drawLine(frame, cameraCenter + Offset(d(-10f), d(-13f)), cameraCenter + Offset(d(-4f), d(-3f)), 2.dp.toPx(), cap = StrokeCap.Round)
            drawRoundRect(
                cameraBody,
                topLeft = cameraCenter - Offset(d(17f), d(9f)),
                size = Size(d(34f), d(18f)), cornerRadius = CornerRadius(d(7f), d(7f))
            )
            drawCircle(deep, d(6f), cameraCenter + Offset(d(9f), 0f))
            val cameraLed = if (state.onlineCameras == state.cameras) SafeGreen else WarningOrange
            drawCircle(cameraLed, d(2.5f), cameraCenter + Offset(d(-9f), d(-2f)))
        }

        // A configured door/gate cover belongs on the perimeter, not on the garage. Two leaves
        // swing inward when it opens, keeping generic blinds and shades out of the security count.
        if (state.securityCoverCount > 0) {
            val gateLeft = point(588f, 562f)
            val gateRight = point(708f, 542f)
            val gateMiddle = point(648f, 552f)
            val leftOpenEnd = point(604f, 526f)
            val rightOpenEnd = point(690f, 516f)
            val leftEnd = gateMiddle + (leftOpenEnd - gateMiddle) * gateOpen
            val rightEnd = gateMiddle + (rightOpenEnd - gateMiddle) * gateOpen
            val gateColor = if (state.openCovers > 0) WarningOrange else frame
            drawLine(deep.copy(alpha = 0.64f), gateLeft + Offset(d(3f), d(7f)), gateLeft - Offset(d(3f), d(31f)), d(5f), StrokeCap.Round)
            drawLine(deep.copy(alpha = 0.64f), gateRight + Offset(d(3f), d(7f)), gateRight - Offset(d(3f), d(31f)), d(5f), StrokeCap.Round)
            drawLine(gateColor, gateLeft - Offset(0f, d(8f)), leftEnd - Offset(0f, d(8f)), d(3f), StrokeCap.Round)
            drawLine(gateColor, gateRight - Offset(0f, d(8f)), rightEnd - Offset(0f, d(8f)), d(3f), StrokeCap.Round)
            drawLine(gateColor.copy(alpha = 0.68f), gateLeft - Offset(0f, d(22f)), leftEnd - Offset(0f, d(22f)), d(2f), StrokeCap.Round)
            drawLine(gateColor.copy(alpha = 0.68f), gateRight - Offset(0f, d(22f)), rightEnd - Offset(0f, d(22f)), d(2f), StrokeCap.Round)
        }

        // Motion is localized to the walkway with a radar ripple and subtle footprints.
        if (state.motionCount > 0) {
            val motionCenter = point(700f, 548f)
            repeat(3) { ring ->
                val t = (phase + ring / 3f) % 1f
                drawCircle(
                    WarningOrange.copy(alpha = (1f - t) * 0.55f),
                    d(15f + t * 48f), motionCenter,
                    style = Stroke(1.3.dp.toPx())
                )
            }
            drawOval(WarningOrange.copy(alpha = 0.72f), point(675f, 536f), Size(d(11f), d(20f)))
            drawOval(WarningOrange.copy(alpha = 0.60f), point(695f, 556f), Size(d(10f), d(18f)))
        }
        if (state.sensorActivityCount > 0) {
            val sensorCenter = leftFace(0.26f, 0.47f)
            repeat(3) { ring ->
                val t = (phase + ring / 3f) % 1f
                drawCircle(
                    WarningOrange.copy(alpha = (1f - t) * 0.48f),
                    d(8f + t * 32f),
                    sensorCenter,
                    style = Stroke(1.1.dp.toPx())
                )
            }
        }

        // Localized hazards: a roof beacon for safety/alarm and a puddle for leak sensors.
        if (state.safetyAlerts > 0 || state.alarmMode == SecurityAlarmMode.TRIGGERED) {
            val beacon = point(645f, 246f)
            repeat(3) { ring ->
                val t = (phase + ring / 3f) % 1f
                drawCircle(AlertRed.copy(alpha = (1f - t) * 0.56f), d(8f + t * 38f), beacon, style = Stroke(1.3.dp.toPx()))
            }
            drawCircle(AlertRed.copy(alpha = 0.42f + 0.58f * pulse), d(7f), beacon)
            drawCircle(Color.White.copy(alpha = 0.82f), d(2.5f), beacon)
        }
        if (state.leakAlerts > 0) {
            val puddle = point(405f, 538f)
            drawOval(SecurityBlue.copy(alpha = 0.28f + 0.12f * pulse), puddle - Offset(d(25f), d(6f)), Size(d(50f), d(15f)))
            repeat(3) { drop ->
                val t = (phase + drop / 3f) % 1f
                drawCircle(SecurityBlue.copy(alpha = (1f - t) * 0.80f), d(4.5f - t * 1.5f), puddle + Offset(d((drop - 1) * 9f), d(-30f + t * 27f)))
            }
        }
    }
}

@Composable
private fun SecurityTile(group: SecurityGroup, count: Int, active: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalHKIAppColors.current
    Surface(modifier.clip(itemCornerShape()).background(surfaceGradient(c.elevated), itemCornerShape()).clickable(onClick = onClick), itemCornerShape(), color = Color.Transparent) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(group.color.copy(.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(group.icon, null, tint = group.color, modifier = Modifier.size(18.dp)) }
            Column(Modifier.weight(1f)) { Text(group.title, color = c.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("$count · $active active", color = c.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            Icon(Icons.Default.ChevronRight, null, tint = c.onMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SecurityGroupPage(group: SecurityGroup, items: List<HAEntity>, viewModel: MainViewModel, currentUrl: String,
    customIcons: Map<String, String>, cameraConfigs: Map<String, HKIButtonConfig>,
    edit: Boolean, onRemove: (String) -> Unit, onRename: (HAEntity) -> Unit, onReorder: (Int, Int) -> Unit, padding: PaddingValues) {
    if (edit && items.isNotEmpty()) {
        ReorderableGrid(items, true, onReorder, { it.entity_id }, GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp), axis = ReorderAxis.Vertical) { entity, _ ->
            Box {
                SecurityEntityCard(entity, group, viewModel, currentUrl, edit, customIcons[entity.entity_id], cameraConfigs[entity.entity_id])
                EditSettingsButton({ onRename(entity) }, Modifier.align(Alignment.Center))
                EditRemoveBadge({ onRemove(entity.entity_id) }, Modifier.align(Alignment.TopEnd))
            }
        }
    } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 96.dp + com.example.hki7.ui.components.LocalMediaPlayerBarInset.current), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) item { EmptyEditHint(Modifier.fillParentMaxHeight()) }
        else item { SecurityGroupSummary(group, items) }
        items(items.size, key = { items[it].entity_id }) { SecurityEntityCard(items[it], group, viewModel, currentUrl, edit, customIcons[items[it].entity_id], cameraConfigs[items[it].entity_id]) }
    }
}

/** Card settings for a security entity: rename plus an MDI icon override (same picker as buttons/badges). */
@Composable
private fun SecurityCardSettingsDialog(
    currentName: String,
    defaultName: String,
    currentIcon: String,
    onDismiss: () -> Unit,
    onSave: (name: String?, icon: String?) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    var icon by remember(currentIcon) { mutableStateOf(currentIcon) }
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
        title = { Text("Card settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Name") }, placeholder = { Text(defaultName) }, modifier = Modifier.fillMaxWidth())
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (icon.isNotBlank()) MdiIcon(icon, size = 24.dp)
                    Text(icon.ifBlank { "Auto" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showIconPicker = true }) { Text("Change") }
                    if (icon.isNotBlank()) TextButton(onClick = { icon = "" }) { Text("Clear") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().takeIf { it.isNotEmpty() }, icon.trim().takeIf { it.isNotEmpty() }) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null, null) }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun SecurityGroupSummary(group: SecurityGroup, items: List<HAEntity>) {
    val c = LocalHKIAppColors.current
    val active = items.count(HAEntity::isSecurityActive)
    Surface(Modifier.fillMaxWidth().background(surfaceGradient(c.elevated), itemCornerShape()), itemCornerShape(), color = Color.Transparent) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).background(group.color.copy(.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(group.icon, null, tint = group.color, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("${items.size} ${if (items.size == 1) "entity" else "entities"}",
                        style = MaterialTheme.typography.titleSmall, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(group.subtitle, style = MaterialTheme.typography.bodySmall, color = c.onMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryStat(group.icon, group.color, items.size.toString(), "Total")
                SummaryStat(Icons.Default.NotificationsActive, if (active > 0) WarningOrange else c.onMuted, active.toString(), "Active")
                SummaryStat(Icons.Default.VerifiedUser, SafeGreen, (items.size - active).toString(), "Clear")
            }
        }
    }
}

@Composable
private fun SummaryStat(icon: ImageVector, color: Color, value: String, label: String) {
    val c = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = c.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.onMuted)
    }
}

@Composable
private fun SecurityEntityCard(entity: HAEntity, group: SecurityGroup, viewModel: MainViewModel, currentUrl: String, editMode: Boolean = false, iconOverride: String? = null, cameraConfig: HKIButtonConfig? = null) {
    if (entity.entity_id.startsWith("camera.")) { SecurityCameraCard(entity, viewModel, currentUrl, cameraConfig); return }
    val c = LocalHKIAppColors.current
    var dialog by remember { mutableStateOf(false) }
    val active = entity.isSecurityActive()
    Surface(Modifier.fillMaxWidth().clip(itemCornerShape()).background(surfaceGradient(c.elevated), itemCornerShape()).clickable(enabled = !editMode) { dialog = true }, itemCornerShape(), color = Color.Transparent) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background((if (active) group.color else c.onMuted).copy(.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                if (iconOverride != null) MdiIcon(iconOverride, contentDescription = null, tint = if (active) group.color else c.onMuted, size = 24.dp)
                else Icon(group.icon, null, tint = if (active) group.color else c.onMuted)
            }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(entity.friendlyName ?: entity.entity_id, color = c.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(entity.state.replace('_', ' ').replaceFirstChar(Char::uppercase), color = if (active) group.color else c.onMuted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Default.ChevronRight, null, tint = c.onMuted)
        }
    }
    if (dialog) SecurityEntityDialog(entity, viewModel) { dialog = false }
}

@Composable
private fun SecurityEntityDialog(entity: HAEntity, viewModel: MainViewModel, onDismiss: () -> Unit) {
    when (entity.entity_id.substringBefore('.')) {
        "lock" -> HKILockDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverrides = mapOf(entity.entity_id to entity.friendlyName),
            onDismiss = onDismiss
        )
        "cover" -> SecurityCoverDialog(entity, viewModel, onDismiss)
        "alarm_control_panel" -> HKIAlarmDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = entity.friendlyName,
            onDismiss = onDismiss
        )
        "person" -> PersonDetailDialog(entity, viewModel, onDismiss)
        else -> GenericEntityDialog(
            entity = entity,
            viewModel = viewModel,
            titleOverride = entity.friendlyName,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SecurityCoverDialog(entity: HAEntity, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var selectedAction by remember(entity.entity_id, entity.state) {
        mutableStateOf(when (entity.state) { "open", "opening" -> "Open"; "closed", "closing" -> "Close"; else -> "Stop" })
    }
    val tabs = listOf(
        Triple("Open", Icons.Default.ArrowUpward) { selectedAction = "Open"; viewModel.controlCover(entity.entity_id, "open_cover") },
        Triple("Stop", Icons.Default.Stop) { selectedAction = "Stop"; viewModel.controlCover(entity.entity_id, "stop_cover") },
        Triple("Close", Icons.Default.ArrowDownward) { selectedAction = "Close"; viewModel.controlCover(entity.entity_id, "close_cover") }
    )
    HKIDialog(
        entity = entity,
        viewModel = viewModel,
        onDismiss = onDismiss,
        icon = Icons.Default.Blinds,
        iconTint = PresencePurple,
        titleOverride = entity.friendlyName,
        statusText = entity.state.replace('_', ' ').uppercase(),
        tabs = tabs,
        currentTab = selectedAction
    ) {
        BlindControlContent(entity, viewModel, activeColor = PresencePurple)
    }
}

@Composable
private fun SecurityCameraCard(entity: HAEntity, viewModel: MainViewModel, currentUrl: String, config: HKIButtonConfig? = null) {
    val accessToken by viewModel.accessToken.collectAsState()
    val itemCornerRadius = LocalItemCornerRadius.current
    var dialog by remember { mutableStateOf(false) }
    // Reuse the regular camera stack widget (live stream, label overlay) as a single full-width card.
    val stack = remember(entity.entity_id, config, itemCornerRadius) {
        HKIButtonStack(
            id = "security_camera_${entity.entity_id}",
            entityIds = listOf(entity.entity_id),
            columns = 1,
            isSquare = false,
            cornerRadius = itemCornerRadius,
            stackType = "camera",
            cameraAspectRatio = 16f / 9f,
            buttonConfigs = config?.let { mapOf(entity.entity_id to it) } ?: emptyMap()
        )
    }
    CameraStackContent(
        stack = stack,
        entities = listOf(entity),
        currentUrl = currentUrl,
        accessToken = accessToken,
        isEditMode = false,
        onEntityClick = { dialog = true },
        onButtonSettings = {},
        onRemoveEntity = {},
        onReorderEntities = { _, _ -> }
    )
    if (dialog) {
        val interval = config?.cameraRefreshInterval ?: 5
        val image = resolveEntityCameraUrl(entity, currentUrl, preferLive = interval == 0)
        val live = if (interval == 0) resolveEntityCameraUrl(entity, currentUrl, preferLive = true) else null
        HKICameraDialog(config?.name ?: entity.friendlyName ?: entity.entity_id, image, refreshIntervalSeconds = interval,
            liveWebUrl = live, authToken = accessToken, entity = entity, viewModel = viewModel, onDismiss = { dialog = false })
    }
}

@Composable
private fun ColumnScope.SecurityEntitySettings(config: HKISecurityConfig, allEntities: List<HAEntity>, onSave: (HKISecurityConfig) -> Unit, setBack: ((() -> Unit)?) -> Unit) {
    val c = LocalHKIAppColors.current
    var cfg by remember(config) { mutableStateOf(config) }
    var category by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf(false) }
    LaunchedEffect(category) { setBack(if (category == null) null else {{ category = null }}) }
    fun save(next: HKISecurityConfig) { cfg = next; onSave(next) }
    val group = securityGroups.find { it.key == category }
    if (picker && group != null) AdvancedEntitySearchDialog(
        allEntities = allEntities,
        onDismiss = { picker = false },
        onEntitiesSelected = { ids -> save(cfg.copy(extraEntityIds = cfg.extraEntityIds + (group.key to ids))); picker = false },
        title = "Select ${group.title}",
        singleSelect = false,
        preselectedIds = cfg.extraEntityIds[group.key].orEmpty().toSet()
    )
    if (category == null) {
        securityGroups.forEach { g ->
            Surface(Modifier.fillMaxWidth().clickable { category = g.key }, itemCornerShape(), color = c.subtleSurface) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(g.icon, null, tint = g.color); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(g.title, color = c.onSurface, fontWeight = FontWeight.SemiBold); Text(if (cfg.extraEntityIds[g.key].orEmpty().isEmpty()) "Auto-discovered" else "Auto + ${cfg.extraEntityIds[g.key].orEmpty().size} manual", color = c.onMuted, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null, tint = c.onMuted) } }
        }
        Surface(Modifier.fillMaxWidth().clickable { category = "hidden" }, itemCornerShape(), color = c.subtleSurface) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VisibilityOff, null, tint = c.onMuted); Spacer(Modifier.width(12.dp)); Text("Removed entities (${cfg.hiddenEntityIds.size})", color = c.onSurface, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = c.onMuted) } }
        return
    }
    if (category == "hidden") {
        if (cfg.hiddenEntityIds.isEmpty()) Text("Nothing removed.", color = c.onMuted)
        cfg.hiddenEntityIds.forEach { id -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(allEntities.find { it.entity_id == id }?.friendlyName ?: id, Modifier.weight(1f), color = c.onSurface, maxLines = 1); IconButton({ save(cfg.copy(hiddenEntityIds = cfg.hiddenEntityIds - id)) }) { Icon(Icons.Default.Close, "Restore") } } }
        return
    }
    if (group != null) {
        Text("Matching entities are added automatically by Home Assistant device class. Add any missing or custom entities here.", color = c.onMuted, style = MaterialTheme.typography.bodySmall)
        cfg.extraEntityIds[group.key].orEmpty().forEach { id -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(allEntities.find { it.entity_id == id }?.friendlyName ?: id, Modifier.weight(1f), color = c.onSurface); IconButton({ save(cfg.copy(extraEntityIds = cfg.extraEntityIds + (group.key to (cfg.extraEntityIds[group.key].orEmpty() - id)))) }) { Icon(Icons.Default.Close, "Remove") } } }
        TextButton({ picker = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add or edit entities") }
    }
}

@Composable private fun SecuritySectionHeader(title: String, count: String) { val c = LocalHKIAppColors.current; Row(Modifier.fillMaxWidth().padding(20.dp, 22.dp, 20.dp, 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = c.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(count, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }

@Composable
fun EmptyEditHint(
    modifier: Modifier = Modifier,
    message: String = "This is an empty security view. Swipe down on the header and open Security Settings to add entities manually."
) { Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(message, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) } }

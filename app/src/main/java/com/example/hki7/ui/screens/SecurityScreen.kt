package com.example.hki7.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.data.HKISecurityConfig
import com.example.hki7.data.withDisplayName
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.*
import com.example.hki7.ui.theme.LocalHKIAppColors

private const val SECURITY_PAGE_KEY = "security"
private val SecurityBlue = Color(0xFF4A90E2)
private val SafeGreen = Color(0xFF43A047)
private val AlertRed = Color(0xFFEF5350)
private val WarningOrange = Color(0xFFFF9800)
private val PresencePurple = Color(0xFF7E57C2)

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
    SecurityGroup("covers", "Covers", "Blinds, shutters and gates", Icons.Default.Blinds, PresencePurple,
        domains = setOf("cover")),
    SecurityGroup("locks", "Locks", "Door and gate locks", Icons.Default.Lock, PresencePurple,
        domains = setOf("lock")),
    SecurityGroup("motion", "Motion", "Motion and vibration detection", Icons.Default.DirectionsWalk, WarningOrange,
        deviceClasses = setOf("motion", "moving", "vibration")),
    SecurityGroup("occupancy", "Occupancy", "Occupancy sensors", Icons.Default.SensorOccupied, PresencePurple,
        deviceClasses = setOf("occupancy")),
    SecurityGroup("presence", "Presence", "People and presence trackers", Icons.Default.Person, PresencePurple,
        domains = setOf("person", "device_tracker"), deviceClasses = setOf("presence")),
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
        domains = setOf("alarm_control_panel"))
)

private fun SecurityGroup.autoMatches(entity: HAEntity): Boolean {
    val domain = entity.entity_id.substringBefore('.')
    if (domain !in domains) return false
    return deviceClasses.isEmpty() || entity.deviceClass in deviceClasses
}

private fun List<HAEntity>.securityOrder(order: List<String>) = sortedWith(
    compareBy<HAEntity> { order.indexOf(it.entity_id).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        .thenBy { it.friendlyName ?: it.entity_id }
)

private fun HAEntity.isSecurityActive(): Boolean = state.lowercase() in setOf(
    "on", "open", "detected", "home", "locked", "triggered", "unlocked", "opening", "closing"
)

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
            domain in setOf("binary_sensor", "sensor", "cover", "lock", "person", "device_tracker", "alarm_control_panel") ||
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
            val automatic = entities.filter(group::autoMatches)
            val manual = config.extraEntityIds[group.key].orEmpty().mapNotNull(byId::get)
            group.key to (automatic + manual).distinctBy { it.entity_id }
                .filterNot { it.entity_id in hidden || it.entity_id in claimed }
                .securityOrder(config.entityOrder)
                .also { list -> claimed += list.map { it.entity_id } }
        }
    }
    var page by rememberSaveable { mutableStateOf("security") }
    BackHandler(page != "security") { page = "security" }
    var renameEntity by remember { mutableStateOf<HAEntity?>(null) }
    renameEntity?.let { entity ->
        RenameCardDialog(config.customNames[entity.entity_id].orEmpty(), entity.friendlyName ?: entity.entity_id,
            onDismiss = { renameEntity = null }) { name ->
            val names = if (name == null) config.customNames - entity.entity_id else config.customNames + (entity.entity_id to name)
            viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, config.copy(customNames = names))
            renameEntity = null
        }
    }

    fun remove(id: String) = viewModel.hideSecurityEntity(SECURITY_PAGE_KEY, id)
    fun reorder(items: List<HAEntity>, from: Int, to: Int) {
        val ids = items.map { it.entity_id }.toMutableList().apply { add(to, removeAt(from)) }
        viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, config.copy(entityOrder = ids + config.entityOrder.filterNot(ids::contains)))
    }

    val activeGroup = securityGroups.find { it.key == page }
    val settings: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> =
        "Security Entities" to { setBack ->
            SecurityEntitySettings(config, entities,
                onSave = { viewModel.updateSecurityConfig(SECURITY_PAGE_KEY, it) }, setBack = setBack)
        }

    HKIPage(
        viewModel = viewModel,
        title = activeGroup?.title ?: "Security",
        subtitle = activeGroup?.subtitle ?: "Home status and activity",
        pageKey = SECURITY_PAGE_KEY,
        pageSettingsTitle = "Security Settings",
        extraPageSettingsSection = settings,
        showBadgeBar = false,
        onBack = if (activeGroup != null) ({ page = "security" }) else null
    ) { padding -> key(uiRevision) {
        if (activeGroup != null) {
            SecurityGroupPage(activeGroup, grouped[activeGroup.key].orEmpty(), viewModel, currentUrl,
                isEditMode, ::remove, { renameEntity = it }, { from, to -> reorder(grouped[activeGroup.key].orEmpty(), from, to) }, padding)
        } else {
            SecurityOverview(grouped, viewModel, currentUrl, isEditMode, ::remove, { page = it }, padding)
        }
    } }
}

@Composable
private fun SecurityOverview(
    grouped: Map<String, List<HAEntity>>, viewModel: MainViewModel, currentUrl: String,
    isEditMode: Boolean, onRemove: (String) -> Unit, onOpen: (String) -> Unit, padding: PaddingValues
) {
    val all = grouped.filterKeys { it != "cameras" }.values.flatten().distinctBy { it.entity_id }
    val cameras = grouped["cameras"].orEmpty()
    val active = all.count(HAEntity::isSecurityActive)
    val alerts = securityGroups.filter { it.key in setOf("fire", "smoke", "gas", "moisture") }
        .sumOf { group -> grouped[group.key].orEmpty().count(HAEntity::isSecurityActive) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { SecurityHero(all.size, active, alerts, cameras.size) }
        item {
            val present = securityGroups.filter { grouped[it.key].orEmpty().isNotEmpty() }
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
                    SecurityCameraCard(cameras[index], viewModel, currentUrl)
                    if (isEditMode) EditRemoveBadge({ onRemove(cameras[index].entity_id) }, Modifier.align(Alignment.TopEnd))
                }
            }
        }
        if (all.isEmpty() && cameras.isEmpty()) item { EmptyEditHint(Modifier.fillParentMaxHeight()) }
    }
}

@Composable
private fun SecurityHero(total: Int, active: Int, alerts: Int, cameras: Int) {
    val colors = LocalHKIAppColors.current
    val color = if (alerts > 0) AlertRed else SafeGreen
    Surface(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(24.dp), color = colors.elevated) {
        Column(Modifier.background(Brush.linearGradient(listOf(color.copy(.18f), Color.Transparent))).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (alerts > 0) Icons.Default.Warning else Icons.Default.VerifiedUser, null, tint = color, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text(if (alerts > 0) "$alerts active alert${if (alerts == 1) "" else "s"}" else "All clear",
                style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.Bold)
            Text("SECURITY STATUS", style = MaterialTheme.typography.labelSmall, color = colors.onMuted)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HeroValue(total.toString(), "Sensors"); HeroValue(active.toString(), "Active"); HeroValue(cameras.toString(), "Cameras")
            }
        }
    }
}

@Composable private fun HeroValue(value: String, label: String) { val c = LocalHKIAppColors.current; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = c.onSurface, fontWeight = FontWeight.Bold); Text(label, color = c.onMuted, style = MaterialTheme.typography.labelSmall) } }

@Composable
private fun SecurityTile(group: SecurityGroup, count: Int, active: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalHKIAppColors.current
    Surface(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), RoundedCornerShape(18.dp), color = c.elevated) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(group.color.copy(.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(group.icon, null, tint = group.color, modifier = Modifier.size(18.dp)) }
            Column(Modifier.weight(1f)) { Text(group.title, color = c.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("$count · $active active", color = c.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            Icon(Icons.Default.ChevronRight, null, tint = c.onMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SecurityGroupPage(group: SecurityGroup, items: List<HAEntity>, viewModel: MainViewModel, currentUrl: String,
    edit: Boolean, onRemove: (String) -> Unit, onRename: (HAEntity) -> Unit, onReorder: (Int, Int) -> Unit, padding: PaddingValues) {
    if (edit && items.isNotEmpty()) {
        ReorderableGrid(items, true, onReorder, { it.entity_id }, GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), axis = ReorderAxis.Vertical) { entity, _ ->
            Box {
                SecurityEntityCard(entity, group, viewModel, currentUrl, edit)
                EditSettingsButton({ onRename(entity) }, Modifier.align(Alignment.Center))
                EditRemoveBadge({ onRemove(entity.entity_id) }, Modifier.align(Alignment.TopEnd))
            }
        }
    } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) item { EmptyEditHint(Modifier.fillParentMaxHeight()) }
        items(items.size, key = { items[it].entity_id }) { SecurityEntityCard(items[it], group, viewModel, currentUrl, edit) }
    }
}

@Composable
private fun SecurityEntityCard(entity: HAEntity, group: SecurityGroup, viewModel: MainViewModel, currentUrl: String, editMode: Boolean = false) {
    if (entity.entity_id.startsWith("camera.")) { SecurityCameraCard(entity, viewModel, currentUrl); return }
    val c = LocalHKIAppColors.current
    var dialog by remember { mutableStateOf(false) }
    val active = entity.isSecurityActive()
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(enabled = !editMode) { dialog = true }, RoundedCornerShape(18.dp), color = c.elevated) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background((if (active) group.color else c.onMuted).copy(.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(group.icon, null, tint = if (active) group.color else c.onMuted) }
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
private fun SecurityCameraCard(entity: HAEntity, viewModel: MainViewModel, currentUrl: String) {
    val c = LocalHKIAppColors.current
    val image = resolveEntityCameraUrl(entity, currentUrl, preferLive = false)
    val live = resolveEntityCameraUrl(entity, currentUrl, preferLive = true)
    var dialog by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { dialog = true }, RoundedCornerShape(20.dp), color = c.elevated) {
        Column { Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) { if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text("No preview", color = Color.Gray, modifier = Modifier.align(Alignment.Center)) }; Text(entity.friendlyName ?: entity.entity_id, Modifier.padding(16.dp), color = c.onSurface, fontWeight = FontWeight.SemiBold) }
    }
    if (dialog) HKICameraDialog(entity.friendlyName ?: entity.entity_id, image, liveWebUrl = live, entity = entity, viewModel = viewModel, onDismiss = { dialog = false })
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
            Surface(Modifier.fillMaxWidth().clickable { category = g.key }, RoundedCornerShape(18.dp), color = c.subtleSurface) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(g.icon, null, tint = g.color); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(g.title, color = c.onSurface, fontWeight = FontWeight.SemiBold); Text(if (cfg.extraEntityIds[g.key].orEmpty().isEmpty()) "Auto-discovered" else "Auto + ${cfg.extraEntityIds[g.key].orEmpty().size} manual", color = c.onMuted, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null, tint = c.onMuted) } }
        }
        Surface(Modifier.fillMaxWidth().clickable { category = "hidden" }, RoundedCornerShape(18.dp), color = c.subtleSurface) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VisibilityOff, null, tint = c.onMuted); Spacer(Modifier.width(12.dp)); Text("Removed entities (${cfg.hiddenEntityIds.size})", color = c.onSurface, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = c.onMuted) } }
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
fun EmptyEditHint(modifier: Modifier = Modifier) { Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("No matching entities found. Home Assistant entities appear automatically; you can also add them in Security Settings.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) } }

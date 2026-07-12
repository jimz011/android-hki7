package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HADeviceRegistryEntry
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.screens.UniversalStackDialog
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon

private val toggleableDomains = setOf(
    "light", "switch", "fan", "input_boolean", "humidifier", "group", "automation", "remote", "siren"
)
private val activeStates = setOf(
    "on", "open", "unlocked", "home", "playing", "cleaning", "heating", "cooling", "detected", "triggered"
)

/**
 * Global search opened from the header pull-down menu: find any device or entity by name,
 * narrow by domain or active state, and control results directly (quick toggle or the
 * universal entity dialog).
 */
@Composable
fun GlobalSearchDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val allEntities by viewModel.entities.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("entities") }
    var domainFilter by remember { mutableStateOf<String?>(null) }
    var activeOnly by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<HADeviceRegistryEntry?>(null) }
    var controlEntity by remember { mutableStateOf<HAEntity?>(null) }

    fun deviceName(d: HADeviceRegistryEntry) = d.name_by_user ?: d.name ?: d.id

    val entityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }
    val entityCountByDevice = remember(entityRegistry) {
        entityRegistry.filter { it.device_id != null }.groupingBy { it.device_id!! }.eachCount()
    }
    // Domain chips ordered by how common the domain is in this home.
    val domains = remember(allEntities) {
        allEntities.groupingBy { it.entity_id.substringBefore('.') }.eachCount()
            .toList().sortedByDescending { it.second }.map { it.first }
    }
    val deviceEntities = selectedDevice?.let { dev ->
        entityRegistry.filter { it.device_id == dev.id }.mapNotNull { entityById[it.entity_id] }
    }

    val entityResults = remember(allEntities, deviceEntities, query, domainFilter, activeOnly) {
        (deviceEntities ?: allEntities).asSequence()
            .filter { domainFilter == null || it.entity_id.substringBefore('.') == domainFilter }
            .filter { !activeOnly || it.state.lowercase() in activeStates }
            .filter {
                query.isBlank() || (it.friendlyName ?: "").contains(query, ignoreCase = true) ||
                    it.entity_id.contains(query, ignoreCase = true)
            }
            .sortedBy { (it.friendlyName ?: it.entity_id).lowercase() }
            .take(150)
            .toList()
    }
    val deviceResults = remember(deviceRegistry, query) {
        deviceRegistry.asSequence()
            .filter { deviceName(it).isNotBlank() }
            .filter {
                query.isBlank() || deviceName(it).contains(query, ignoreCase = true) ||
                    it.manufacturer.orEmpty().contains(query, ignoreCase = true) ||
                    it.model.orEmpty().contains(query, ignoreCase = true)
            }
            .sortedBy { deviceName(it).lowercase() }
            .take(150)
            .toList()
    }

    controlEntity?.let { picked ->
        val live = entityById[picked.entity_id] ?: picked
        if (live.entity_id.startsWith("media_player.")) {
            HKIMediaPlayerDialog(live, viewModel, currentUrl, onDismiss = { controlEntity = null })
        } else {
            UniversalStackDialog(
                entities = listOf(live),
                allEntities = allEntities,
                currentUrl = currentUrl,
                viewModel = viewModel,
                onDismiss = { controlEntity = null }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                selectedDevice?.let { dev ->
                    IconButton(onClick = { selectedDevice = null }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(deviceName(dev), maxLines = 1, overflow = TextOverflow.Ellipsis)
                } ?: Text("Search")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (mode == "devices" && selectedDevice == null) "Search devices" else "Search entities") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedDevice == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "entities", onClick = { mode = "entities" }, label = { Text("Entities") },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, null, Modifier.size(14.dp)) })
                        FilterChip(selected = mode == "devices", onClick = { mode = "devices" }, label = { Text("Devices") },
                            leadingIcon = { Icon(Icons.Default.Memory, null, Modifier.size(14.dp)) })
                    }
                }
                if (mode == "entities" || selectedDevice != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = activeOnly, onClick = { activeOnly = !activeOnly },
                            label = { Text("Active") },
                            leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(14.dp)) })
                        FilterChip(selected = domainFilter == null, onClick = { domainFilter = null }, label = { Text("All") })
                        domains.forEach { domain ->
                            FilterChip(
                                selected = domainFilter == domain,
                                onClick = { domainFilter = if (domainFilter == domain) null else domain },
                                label = { Text(domain.replace('_', ' ').replaceFirstChar(Char::uppercase)) }
                            )
                        }
                    }
                }
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(Modifier.heightIn(max = 380.dp).fadingEdges(listState), state = listState) {
                    if (mode == "devices" && selectedDevice == null) {
                        if (deviceResults.isEmpty()) item { SearchEmptyHint("No matching devices.") }
                        items(deviceResults, key = { it.id }) { device ->
                            SearchDeviceRow(
                                device = device,
                                name = deviceName(device),
                                entityCount = entityCountByDevice[device.id] ?: 0,
                                onClick = { selectedDevice = device; query = "" }
                            )
                        }
                    } else {
                        if (entityResults.isEmpty()) item { SearchEmptyHint("No matching entities.") }
                        items(entityResults, key = { it.entity_id }) { entity ->
                            SearchEntityRow(
                                entity = entity,
                                onOpen = { controlEntity = entity },
                                onToggle = if (entity.entity_id.substringBefore('.') in toggleableDomains) {
                                    { viewModel.toggleEntity(entity.entity_id) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SearchEntityRow(entity: HAEntity, onOpen: () -> Unit, onToggle: (() -> Unit)?) {
    val appColors = LocalHKIAppColors.current
    val isActive = entity.state.lowercase() in activeStates
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(34.dp).background(
                (if (isActive) MaterialTheme.colorScheme.primary else appColors.onMuted).copy(alpha = 0.14f),
                RoundedCornerShape(10.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            MdiIcon(
                name = defaultEntityIconSlug(entity) ?: "help-circle-outline",
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else appColors.onMuted,
                size = 18.dp
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                entity.friendlyName ?: entity.entity_id,
                color = appColors.onSurface, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entity.state.replace('_', ' ').replaceFirstChar(Char::uppercase)} · ${entity.entity_id}",
                color = appColors.onMuted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (onToggle != null) {
            Switch(checked = entity.state == "on", onCheckedChange = { onToggle() })
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f))
}

@Composable
private fun SearchDeviceRow(device: HADeviceRegistryEntry, name: String, entityCount: Int, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(34.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Memory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = appColors.onSurface, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    device.manufacturer?.takeIf { it.isNotBlank() },
                    device.model?.takeIf { it.isNotBlank() },
                    "$entityCount ${if (entityCount == 1) "entity" else "entities"}"
                ).joinToString(" · "),
                color = appColors.onMuted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f))
}

@Composable
private fun SearchEmptyHint(text: String) {
    val appColors = LocalHKIAppColors.current
    Text(text, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 18.dp))
}

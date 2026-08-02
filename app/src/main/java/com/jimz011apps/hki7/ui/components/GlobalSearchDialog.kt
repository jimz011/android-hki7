package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HADeviceRegistryEntry
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.Hki7Policy
import com.jimz011apps.hki7.data.canSearchEntity
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.screens.UniversalStackDialog
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

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
    val allEntities by viewModel.entities.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val visibleSearchDomains by viewModel.prefs.parentalVisibleSearchDomains.collectAsState(initial = emptyList())
    val visibleSearchEntityIds by viewModel.prefs.parentalVisibleSearchEntityIds.collectAsState(initial = emptyList())
    val hiddenSearchDomains by viewModel.prefs.parentalHiddenSearchDomains.collectAsState(initial = emptyList())
    val hiddenSearchEntityIds by viewModel.prefs.parentalHiddenSearchEntityIds.collectAsState(initial = emptyList())
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("entities") }
    var domainFilter by remember { mutableStateOf<String?>(null) }
    var activeOnly by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<HADeviceRegistryEntry?>(null) }
    var controlEntity by remember { mutableStateOf<HAEntity?>(null) }

    fun deviceName(d: HADeviceRegistryEntry) = d.name_by_user ?: d.name ?: d.id

    val searchPolicy = remember(
        visibleSearchDomains,
        visibleSearchEntityIds,
        hiddenSearchDomains,
        hiddenSearchEntityIds,
    ) {
        Hki7Policy(
            visibleSearchDomains = visibleSearchDomains,
            visibleSearchEntityIds = visibleSearchEntityIds,
            hiddenSearchDomains = hiddenSearchDomains,
            hiddenSearchEntityIds = hiddenSearchEntityIds,
        )
    }
    val searchableEntities = remember(allEntities, searchPolicy) {
        allEntities.filter { searchPolicy.canSearchEntity(it.entity_id) }
    }
    val searchableEntityIds = remember(searchableEntities) { searchableEntities.mapTo(hashSetOf()) { it.entity_id } }
    val entityById = remember(searchableEntities) { searchableEntities.associateBy { it.entity_id } }
    val entityCountByDevice = remember(entityRegistry, searchableEntityIds) {
        entityRegistry.filter { it.device_id != null && it.entity_id in searchableEntityIds }
            .groupingBy { it.device_id!! }.eachCount()
    }
    // Domain chips ordered by how common the domain is in this home.
    val domains = remember(searchableEntities) {
        searchableEntities.groupingBy { it.entity_id.substringBefore('.') }.eachCount()
            .toList().sortedByDescending { it.second }.map { it.first }
    }
    val deviceEntities = selectedDevice?.let { dev ->
        entityRegistry.filter { it.device_id == dev.id }.mapNotNull { entityById[it.entity_id] }
    }

    val entityResults = remember(searchableEntities, deviceEntities, query, domainFilter, activeOnly) {
        (deviceEntities ?: searchableEntities).asSequence()
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
    val deviceResults = remember(deviceRegistry, entityCountByDevice, query) {
        deviceRegistry.asSequence()
            .filter { (entityCountByDevice[it.id] ?: 0) > 0 }
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
                allEntities = searchableEntities,
                currentUrl = currentUrl,
                viewModel = viewModel,
                onDismiss = { controlEntity = null }
            )
        }
    }

    ModernSettingsDialogFrame(
        title = selectedDevice?.let(::deviceName) ?: stringResource(R.string.global_search_title),
        subtitle = selectedDevice?.let { device ->
            val count = entityCountByDevice[device.id] ?: 0
            pluralStringResource(R.plurals.global_search_browse_entities, count, count)
        } ?: stringResource(R.string.global_search_subtitle),
        icon = Icons.Default.Search,
        onDismiss = onDismiss,
        onBack = selectedDevice?.let {
            {
                selectedDevice = null
                query = ""
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (mode == "devices" && selectedDevice == null) stringResource(R.string.ui_search_devices_2ce0b70) else stringResource(R.string.ui_search_entities_7c70008)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, stringResource(R.string.global_search_clear), modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedDevice == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsChoiceChip(selected = mode == "entities", onClick = { mode = "entities" }, label = { Text(stringResource(R.string.ui_entities_f7638a2)) },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, null, Modifier.size(14.dp)) })
                        SettingsChoiceChip(selected = mode == "devices", onClick = { mode = "devices" }, label = { Text(stringResource(R.string.ui_devices_df485c8)) },
                            leadingIcon = { Icon(Icons.Default.Memory, null, Modifier.size(14.dp)) })
                    }
                }
                if (mode == "entities" || selectedDevice != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsChoiceChip(selected = activeOnly, onClick = { activeOnly = !activeOnly },
                            label = { Text(stringResource(R.string.ui_active_a733b80)) },
                            leadingIcon = { Icon(Icons.Default.Bolt, null, Modifier.size(14.dp)) })
                        SettingsChoiceChip(selected = domainFilter == null, onClick = { domainFilter = null }, label = { Text(stringResource(R.string.ui_all_6a72085)) })
                        domains.forEach { domain ->
                            SettingsChoiceChip(
                                selected = domainFilter == domain,
                                onClick = { domainFilter = if (domainFilter == domain) null else domain },
                                label = { Text(domain.replace('_', ' ').replaceFirstChar(Char::uppercase)) }
                            )
                        }
                    }
                }
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(Modifier.weight(1f).fadingEdges(listState), state = listState) {
                    if (mode == "devices" && selectedDevice == null) {
                        if (deviceResults.isEmpty()) item { SearchEmptyHint(stringResource(R.string.global_search_no_devices)) }
                        items(deviceResults, key = { it.id }) { device ->
                            SearchDeviceRow(
                                device = device,
                                name = deviceName(device),
                                entityCount = entityCountByDevice[device.id] ?: 0,
                                onClick = { selectedDevice = device; query = "" }
                            )
                        }
                    } else {
                        if (entityResults.isEmpty()) item { SearchEmptyHint(stringResource(R.string.global_search_no_entities)) }
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
        footer = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa773)) } }
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
                stringResource(R.string.ui_text_c1aacd9, entity.state.replace('_', ' ').replaceFirstChar(Char::uppercase), entity.entity_id),
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
                    pluralStringResource(R.plurals.global_search_entity_count, entityCount, entityCount)
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

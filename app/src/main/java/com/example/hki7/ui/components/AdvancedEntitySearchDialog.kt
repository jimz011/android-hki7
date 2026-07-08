package com.example.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.theme.LocalHKIAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedEntitySearchDialog(
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onEntitiesSelected: (List<String>) -> Unit,
    title: String = "Select Items",
    singleSelect: Boolean = false,
    preselectedIds: Set<String> = emptySet()
) {
    val appColors = LocalHKIAppColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("All") }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(preselectedIds) {
        selectedIds.clear()
        selectedIds.addAll(preselectedIds)
    }
    
    val domains = remember(allEntities) {
        listOf("All") + allEntities.map { it.entity_id.split(".").first() }.distinct().sorted()
    }

    // derivedStateOf: only re-filter/re-sort when the query, domain, or selection actually
    // changes, not on every recomposition of the dialog.
    val filteredEntities by remember(allEntities) {
        derivedStateOf {
            allEntities.filter { entity ->
                val matchesSearch = entity.friendlyName?.contains(searchQuery, ignoreCase = true) == true ||
                                    entity.entity_id.contains(searchQuery, ignoreCase = true)
                val matchesDomain = selectedDomain == "All" || entity.entity_id.startsWith("$selectedDomain.")
                matchesSearch && matchesDomain
            }.sortedWith(compareByDescending<HAEntity> { selectedIds.contains(it.entity_id) }.thenBy { it.friendlyName ?: it.entity_id })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = appColors.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Multi-Select Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, color = appColors.onSurface, modifier = Modifier.weight(1f))
                    
                    if (!singleSelect && selectedIds.isNotEmpty()) {
                        Button(
                            onClick = { onEntitiesSelected(selectedIds.toList()); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAF7AC5))
                        ) {
                            Text("Add (${selectedIds.size})")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search", color = appColors.onMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = appColors.onMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.onSurface,
                        unfocusedTextColor = appColors.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = appColors.onMuted,
                        focusedContainerColor = appColors.elevated,
                        unfocusedContainerColor = appColors.elevated,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // Domain Filter Chips
                ScrollableTabRow(
                    selectedTabIndex = domains.indexOf(selectedDomain).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    domains.forEach { domain ->
                        val isSelected = selectedDomain == domain
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDomain = domain },
                            label = { Text(domain.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = appColors.elevated,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = appColors.onMuted,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredEntities, key = { it.entity_id }) { entity ->
                        val isChecked = selectedIds.contains(entity.entity_id)
                        ListItem(
                            modifier = Modifier.clickable {
                                if (singleSelect) {
                                    onEntitiesSelected(listOf(entity.entity_id))
                                    onDismiss()
                                } else {
                                    if (isChecked) selectedIds.remove(entity.entity_id)
                                    else selectedIds.add(entity.entity_id)
                                }
                            },
                            headlineContent = { 
                                Text(entity.friendlyName ?: entity.entity_id, color = appColors.onSurface, fontWeight = FontWeight.SemiBold) 
                            },
                            supportingContent = { 
                                Text(entity.entity_id, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall) 
                            },
                            leadingContent = {
                                if (!singleSelect) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFAF7AC5))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = appColors.onMuted
                                    )
                                }
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = when(entity.entity_id.split(".").first()) {
                                        "light" -> Icons.Default.Lightbulb
                                        "switch" -> Icons.Default.Power
                                        "sensor" -> Icons.Default.Sensors
                                        "binary_sensor" -> Icons.Default.RadioButtonChecked
                                        "media_player" -> Icons.Default.PlayCircle
                                        "sun" -> Icons.Default.WbSunny
                                        "weather" -> Icons.Default.Cloud
                                        else -> Icons.Default.DeviceUnknown
                                    },
                                    contentDescription = null, 
                                    tint = if (entity.state == "on") Color(0xFFFFA500) else appColors.onMuted
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

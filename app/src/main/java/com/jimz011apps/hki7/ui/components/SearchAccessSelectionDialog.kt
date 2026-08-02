package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

data class SearchAccessSelection(
    val domains: Set<String> = emptySet(),
    val entityIds: Set<String> = emptySet(),
)

/** Multi-picker used by parental controls. A selection may contain whole HA domains and exact
 * entities; domain selection intentionally leaves entity exceptions to the opposite access list. */
@Composable
fun SearchAccessSelectionDialog(
    allEntities: List<HAEntity>,
    title: String,
    initialSelection: SearchAccessSelection,
    onDismiss: () -> Unit,
    onSave: (SearchAccessSelection) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var query by remember { mutableStateOf("") }
    var domains by remember(initialSelection) { mutableStateOf(initialSelection.domains) }
    var entityIds by remember(initialSelection) { mutableStateOf(initialSelection.entityIds) }
    val normalizedQuery = query.trim()
    val availableDomains = remember(allEntities) {
        allEntities.map { it.entity_id.substringBefore('.') }.distinct().sorted()
    }
    val filteredDomains = remember(availableDomains, normalizedQuery) {
        availableDomains.filter {
            normalizedQuery.isBlank() || it.replace('_', ' ').contains(normalizedQuery, ignoreCase = true)
        }
    }
    val filteredEntities = remember(allEntities, normalizedQuery) {
        allEntities.asSequence()
            .filter {
                normalizedQuery.isBlank() || it.entity_id.contains(normalizedQuery, ignoreCase = true) ||
                    it.friendlyName.orEmpty().contains(normalizedQuery, ignoreCase = true)
            }
            .sortedBy { (it.friendlyName ?: it.entity_id).lowercase() }
            .toList()
    }

    ModernSettingsDialogFrame(
        title = title,
        subtitle = stringResource(R.string.parental_search_picker_subtitle),
        icon = Icons.Default.Search,
        onDismiss = onDismiss,
        content = {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.ui_search_bce0641)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                )
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(Modifier.weight(1f).fadingEdges(listState), state = listState) {
                    if (filteredDomains.isNotEmpty()) {
                        item("domains-title") {
                            Text(
                                stringResource(R.string.parental_search_domains),
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(filteredDomains, key = { "domain:$it" }) { domain ->
                            val selected = domain in domains
                            SearchAccessPickerRow(
                                title = domain.replace('_', ' ').replaceFirstChar(Char::uppercase),
                                subtitle = stringResource(R.string.parental_search_entire_domain),
                                checked = selected,
                                icon = { Icon(Icons.Default.Category, null, Modifier.size(20.dp)) },
                                onClick = { domains = if (selected) domains - domain else domains + domain },
                            )
                        }
                    }
                    if (filteredEntities.isNotEmpty()) {
                        item("entities-title") {
                            Text(
                                stringResource(R.string.ui_entities_f7638a2),
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                            )
                        }
                        items(filteredEntities, key = { "entity:${it.entity_id}" }) { entity ->
                            val selected = entity.entity_id in entityIds
                            SearchAccessPickerRow(
                                title = entity.friendlyName ?: entity.entity_id,
                                subtitle = entity.entity_id,
                                checked = selected,
                                onClick = {
                                    entityIds = if (selected) entityIds - entity.entity_id else entityIds + entity.entity_id
                                },
                            )
                        }
                    }
                }
            }
        },
        footer = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
            Button(onClick = { onSave(SearchAccessSelection(domains, entityIds)); onDismiss() }) {
                Text(stringResource(R.string.ui_save_efc007a))
            }
        },
    )
}

@Composable
private fun SearchAccessPickerRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        icon?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
}

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

/** One draggable row in a [ReorderItemsDialog]. */
data class ReorderItem(val key: String, val label: String, val iconSlug: String? = null)

/**
 * A compact "drag to reorder" dialog reused by the Security, Climate and Energy views to reorder
 * cameras, thermostats and energy cards respectively. Mirrors the stack "Manage items" reorder UI:
 * a bounded [ReorderableGrid] of long-press-draggable rows, committing the new key order on Save.
 */
@Composable
fun ReorderItemsDialog(
    title: String,
    subtitle: String,
    items: List<ReorderItem>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var ordered by remember(items) { mutableStateOf(items) }
    val listHeight = ((ordered.size * 60).coerceIn(80, 460)).dp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (ordered.isEmpty()) {
                Text(stringResource(R.string.ui_nothing_to_reorder_yet_cdcbb36), color = appColors.onMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                    ReorderableGrid(
                        items = ordered,
                        canReorder = true,
                        onReorder = { from, to ->
                            ordered = ordered.toMutableList().apply { add(to.coerceIn(0, size - 1), removeAt(from)) }
                        },
                        key = { it.key },
                        columns = GridCells.Fixed(1),
                        axis = ReorderAxis.Vertical,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(listHeight)
                    ) { item, isDragging ->
                        Surface(
                            shape = itemCornerShape(),
                            color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else appColors.subtleSurface,
                            border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = if (isDragging) 0.28f else 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DragIndicator, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                if (item.iconSlug != null) {
                                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        MdiIcon(item.iconSlug, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(
                                    item.label,
                                    color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(ordered.map { it.key }) }) { Text(stringResource(R.string.ui_save_efc007a)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MDI_COMMON
import com.example.hki7.ui.utils.MdiIcon
import com.example.hki7.ui.utils.MdiIconStore

/** Category name for the curated "common" icons shown by default. */
private const val COMMON_CATEGORY = "Common"

/**
 * Picker category chips: label → MDI tag (from meta.json `tags`), or null for the
 * curated common set. Tags are matched precisely via [MdiIconStore.byCategory].
 */
private val ICON_CATEGORIES: List<Pair<String, String?>> = listOf(
    COMMON_CATEGORY to null,
    "Home" to "home automation",
    "Weather" to "weather",
    "Lock" to "lock",
    "People" to "account / user",
    "Auto" to "automotive",
    "Music" to "music",
    "Battery" to "battery",
    "Nature" to "nature",
    "Places" to "places",
    "Food" to "food / drink",
    "Navigation" to "navigation",
)

/**
 * Full-screen icon picker backed by the MDI icon registry.
 *
 * @param current   The currently selected MDI slug (empty string = none/auto).
 * @param onDismiss Called when the user dismisses without selecting.
 * @param onSelect  Called with the chosen MDI slug (empty string = none/auto).
 */
@Composable
fun MdiIconPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ICON_CATEGORIES.first().first) }
    val allNames = remember { MdiIconStore.allNames(context) }
    val filtered = remember(query, category, allNames) {
        val q = query.trim()
        when {
            // A search query overrides the category filter.
            q.isNotEmpty() -> MdiIconStore.search(context, q)
            category == COMMON_CATEGORY -> {
                // Common icons first, then the full library (deduped).
                val commonSet = MDI_COMMON.toHashSet()
                MDI_COMMON + allNames.filterNot { it in commonSet }
            }
            else -> {
                val tag = ICON_CATEGORIES.firstOrNull { it.first == category }?.second
                if (tag == null) allNames else MdiIconStore.byCategory(context, tag)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.elevated)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Choose Icon",
                        style = MaterialTheme.typography.titleLarge,
                        color = appColors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = appColors.onSurface)
                    }
                }

                // ── Search ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search…", color = appColors.onMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = appColors.onMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.onSurface,
                        unfocusedTextColor = appColors.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = appColors.onMuted.copy(alpha = 0.4f)
                    )
                )

                Spacer(Modifier.height(12.dp))

                // ── "None / Auto" chip ────────────────────────────────────────
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    FilterChip(
                        selected = current.isEmpty(),
                        onClick = { onSelect("") },
                        label = { Text("None / Auto") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Category filters ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ICON_CATEGORIES.forEach { (label, _) ->
                        FilterChip(
                            selected = query.isBlank() && category == label,
                            onClick = { category = label; query = "" },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Icon grid ─────────────────────────────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered.size, key = { filtered[it] }) { i ->
                        val slug = filtered[i]
                        val isSelected = slug == current
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                appColors.subtleSurface,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onSelect(slug) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MdiIcon(
                                    name = slug,
                                    contentDescription = slug,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        appColors.onSurface,
                                    size = 26.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

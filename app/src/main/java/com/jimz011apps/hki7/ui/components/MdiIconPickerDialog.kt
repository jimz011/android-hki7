package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.IconPack
import com.jimz011apps.hki7.ui.utils.IconPreferences
import com.jimz011apps.hki7.ui.utils.MDI_COMMON
import com.jimz011apps.hki7.ui.utils.MdiIcon
import com.jimz011apps.hki7.ui.utils.MdiIconStore
import com.jimz011apps.hki7.ui.utils.PHOSPHOR_COMMON
import com.jimz011apps.hki7.ui.utils.SI_COMMON
import com.jimz011apps.hki7.ui.utils.TABLER_COMMON

/** Category name for the curated "common" icons shown by default. */
private const val COMMON_CATEGORY = "common"

private data class IconCategory(
    val id: String,
    @param:StringRes val labelRes: Int,
    val tag: String?
)

/**
 * Picker category chips: label → MDI tag (from meta.json `tags`), or null for the
 * curated common set. Tags are matched precisely via [MdiIconStore.byCategory].
 */
private val ICON_CATEGORIES = listOf(
    IconCategory(COMMON_CATEGORY, R.string.core_icon_category_common, null),
    IconCategory("home", R.string.core_icon_category_home, "home automation"),
    IconCategory("weather", R.string.core_icon_category_weather, "weather"),
    IconCategory("lock", R.string.core_icon_category_lock, "lock"),
    IconCategory("people", R.string.core_icon_category_people, "account / user"),
    IconCategory("auto", R.string.core_icon_category_auto, "automotive"),
    IconCategory("music", R.string.core_icon_category_music, "music"),
    IconCategory("battery", R.string.core_icon_category_battery, "battery"),
    IconCategory("nature", R.string.core_icon_category_nature, "nature"),
    IconCategory("places", R.string.core_icon_category_places, "places"),
    IconCategory("food", R.string.core_icon_category_food, "food / drink"),
    IconCategory("navigation", R.string.core_icon_category_navigation, "navigation"),
)

/** Tabler category chips: label → Tabler `category` (matched as `#category`). */
private val TABLER_CATEGORIES = listOf(
    IconCategory(COMMON_CATEGORY, R.string.core_icon_category_common, null),
    IconCategory("devices", R.string.core_icon_category_devices, "devices"),
    IconCategory("weather", R.string.core_icon_category_weather, "weather"),
    IconCategory("nature", R.string.core_icon_category_nature, "nature"),
    IconCategory("map", R.string.core_icon_category_map, "map"),
    IconCategory("media", R.string.core_icon_category_media, "media"),
    IconCategory("communication", R.string.core_icon_category_communication, "communication"),
    IconCategory("food", R.string.core_icon_category_food, "food"),
    IconCategory("health", R.string.core_icon_category_health, "health"),
    IconCategory("vehicles", R.string.core_icon_category_vehicles, "vehicles"),
    IconCategory("buildings", R.string.core_icon_category_buildings, "buildings"),
    IconCategory("electrical", R.string.core_icon_category_electrical, "electrical"),
    IconCategory("sport", R.string.core_icon_category_sport, "sport"),
)

/** Phosphor category chips: label → Phosphor `IconCategory` (matched as `#category`). */
private val PHOSPHOR_CATEGORIES = listOf(
    IconCategory(COMMON_CATEGORY, R.string.core_icon_category_common, null),
    IconCategory("objects", R.string.core_icon_category_objects, "objects"),
    IconCategory("weather", R.string.core_icon_category_weather, "weather"),
    IconCategory("nature", R.string.core_icon_category_nature, "nature"),
    IconCategory("map", R.string.core_icon_category_map, "map"),
    IconCategory("media", R.string.core_icon_category_media, "media"),
    IconCategory("communication", R.string.core_icon_category_communication, "communication"),
    IconCategory("commerce", R.string.core_icon_category_commerce, "commerce"),
    IconCategory("health", R.string.core_icon_category_health, "health"),
    IconCategory("games", R.string.core_icon_category_games, "games"),
    IconCategory("people", R.string.core_icon_category_people, "people"),
    IconCategory("finance", R.string.core_icon_category_finance, "finance"),
)

/** The category chip list for [pack]; empty-of-categories packs use the common-only list. */
private fun categoriesFor(pack: IconPack): List<IconCategory> = when (pack) {
    IconPack.MDI -> ICON_CATEGORIES
    IconPack.TABLER -> TABLER_CATEGORIES
    IconPack.PHOSPHOR -> PHOSPHOR_CATEGORIES
    else -> listOf(IconCategory(COMMON_CATEGORY, R.string.core_icon_category_common, null))
}

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
    onSelect: (String) -> Unit,
    // When true, offers a "Use entity picture" option that selects the ENTITY_PICTURE_ICON sentinel.
    allowEntityPicture: Boolean = false,
    // The icon pack pre-selected when the picker opens. For an existing icon, open to its pack;
    // for a new/blank icon, open to the appearance-settings default. The user can still switch
    // packs here — the returned slug is always qualified to the pack it came from.
    initialPack: IconPack =
        if (current.isBlank()) IconPreferences.defaultPack else IconPack.parse(current).first
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var pack by remember { mutableStateOf(initialPack) }
    var category by remember { mutableStateOf(COMMON_CATEGORY) }
    val allNames = remember(pack) { MdiIconStore.allNames(context, pack) }
    val filtered = remember(query, category, pack, allNames) {
        val q = query.trim()
        val common = when (pack) {
            IconPack.SIMPLE -> SI_COMMON
            IconPack.TABLER -> TABLER_COMMON
            IconPack.PHOSPHOR -> PHOSPHOR_COMMON
            else -> MDI_COMMON
        }
        when {
            // A search query overrides the category filter.
            q.isNotEmpty() -> MdiIconStore.search(context, q, pack)
            // Packs without category tags (Simple Icons): show common-first, then the full library.
            !pack.hasCategories || category == COMMON_CATEGORY -> {
                val commonSet = common.toHashSet()
                common.filter { allNames.contains(it) } + allNames.filterNot { it in commonSet }
            }
            else -> {
                val tag = categoriesFor(pack).firstOrNull { it.id == category }?.tag
                if (tag == null) allNames else MdiIconStore.byCategory(context, tag, pack)
            }
        }
    }

    ModernSettingsDialogFrame(
        title = stringResource(R.string.ui_choose_icon_b0047ce),
        subtitle = stringResource(R.string.ui_search_the_material_design_icon_library_2a2aa8d),
        icon = Icons.Default.Search,
        onDismiss = onDismiss,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Header ──────────────────────────────────────────────────
                // ── Search ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.ui_search_f54fbca), color = appColors.onMuted) },
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

                // ── Icon pack selector ───────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconPack.entries.forEach { p ->
                        SettingsChoiceChip(
                            selected = pack == p,
                            onClick = { pack = p; category = COMMON_CATEGORY },
                            label = { Text(p.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── "None / Auto" + optional "Entity picture" chips ──────────
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsChoiceChip(
                        selected = current.isEmpty(),
                        onClick = { onSelect("") },
                        label = { Text(stringResource(R.string.ui_none_auto_cc8786f)) }
                    )
                    if (allowEntityPicture) {
                        SettingsChoiceChip(
                            selected = current == ENTITY_PICTURE_ICON,
                            onClick = { onSelect(ENTITY_PICTURE_ICON) },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(18.dp)) },
                            label = { Text(stringResource(R.string.ui_entity_picture_0569c68)) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Category filters (packs that ship category tags) ──────────
                if (pack.hasCategories) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesFor(pack).forEach { categoryOption ->
                            SettingsChoiceChip(
                                selected = query.isBlank() && category == categoryOption.id,
                                onClick = { category = categoryOption.id; query = "" },
                                label = { Text(stringResource(categoryOption.labelRes)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // ── Icon grid ─────────────────────────────────────────────────
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fadingEdges(gridState)
                ) {
                    items(filtered.size, key = { filtered[it] }) { i ->
                        val slug = filtered[i]
                        val qualified = IconPack.qualify(pack, slug)
                        val isSelected = qualified == current
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                appColors.subtleSurface,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onSelect(qualified) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MdiIcon(
                                    name = qualified,
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
        },
        footer = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimz011apps.hki7.data.HKITodoWidget
import com.jimz011apps.hki7.data.TidyShopItem
import com.jimz011apps.hki7.data.TidyShopList
import com.jimz011apps.hki7.data.TidyShopState
import com.jimz011apps.hki7.data.TidyShopStatus
import com.jimz011apps.hki7.data.TidyShopSync
import com.jimz011apps.hki7.data.canEdit
import com.jimz011apps.hki7.ui.components.LocalVisibilityFamilyContext
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import java.util.Locale

/**
 * The TidyShop half of the to-do widget.
 *
 * Kept beside the local one rather than folded into it: the two render different item models with
 * different permission rules, and the connector — not this dashboard — decides what the signed-in
 * member may change. Nothing here is written back into [HKITodoWidget]; the lists belong to the
 * family server and are only borrowed for display.
 */

/** The lists this widget is configured to show, in the order the connector reported them. */
internal fun visibleTidyShopLists(widget: HKITodoWidget, state: TidyShopState): List<TidyShopList> =
    if (widget.tidyShopListIds.isEmpty()) state.lists
    else state.lists.filter { it.id in widget.tidyShopListIds }

/**
 * Applies the widget's own sort setting to a connector list. "Manual" is the order TidyShop holds,
 * which is the order somebody arranged over there; "priority" has no counterpart in TidyShop, so
 * it falls back to that same order rather than pretending to sort by something absent.
 */
internal fun sortedTidyShopItems(widget: HKITodoWidget, items: List<TidyShopItem>): List<TidyShopItem> =
    when (widget.sortMode) {
        "alphabetical" -> items.sortedBy { it.name.lowercase(Locale.getDefault()) }
        "newest" -> items.reversed()
        else -> items
    }

private fun TidyShopList.isStoreList(): Boolean = icon.startsWith("store:", ignoreCase = true)

/** Turns TidyShop's internal `store:nl:aldi` icon key into the label people expect to see. */
internal fun tidyShopListDisplayName(list: TidyShopList): String {
    if (!list.isStoreList()) return list.title
    val country = list.storeCountryCode.ifBlank {
        list.icon.split(':').getOrNull(1).orEmpty()
    }.trim().uppercase(Locale.ROOT)
    return if (country.isBlank() || list.title.endsWith("($country)", ignoreCase = true)) {
        list.title
    } else {
        "${list.title} ($country)"
    }
}

private const val TIDYSHOP_PACKAGE = "com.jimz011apps.tidyshop"
private const val TIDYSHOP_OPEN_LIST_EXTRA = "com.jimz011apps.tidyshop.OPEN_LIST_ID"

/** Opens TidyShop itself and asks it to show [listId] when that list exists there. */
internal fun openTidyShopApp(context: Context, listId: String?): Boolean {
    val launch = runCatching {
        context.packageManager.getLaunchIntentForPackage(TIDYSHOP_PACKAGE)
    }.getOrNull() ?: return false
    listId?.takeIf { it.isNotBlank() }?.let { launch.putExtra(TIDYSHOP_OPEN_LIST_EXTRA, it) }
    return runCatching {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

@Composable
internal fun TidyShopTodoWidgetItem(
    widget: HKITodoWidget,
    /** The Home Assistant base URL in use, so a relative background image still resolves. */
    currentUrl: String,
    isEditMode: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by TidyShopSync.state.collectAsState()
    val lists = remember(widget.tidyShopListIds, state.lists) { visibleTidyShopLists(widget, state) }
    // No fetch of its own: TidyShopSync already pulls once on every return to the foreground, and
    // a dashboard can hold several of these cards — one request each would be a small stampede for
    // data they all read from the same place anyway.
    TidyShopTodoCard(
        widget = widget,
        state = state,
        lists = lists,
        currentUrl = currentUrl,
        canToggle = !isEditMode,
        modifier = modifier.clickable(enabled = !isEditMode) { onOpen() },
    )
}

@Composable
private fun TidyShopTodoCard(
    widget: HKITodoWidget,
    state: TidyShopState,
    lists: List<TidyShopList>,
    currentUrl: String,
    canToggle: Boolean,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val remaining = lists.sumOf { it.remaining }
    val total = lists.sumOf { it.items.size }
    val defaultTitle = stringResource(R.string.widgets_todo_shopping_title)
    // The featured list is named the same way a local widget names a featured category, so the
    // setting means the same thing in both modes.
    val hero = remember(lists, widget.heroCategory) {
        widget.heroCategory?.let { name -> lists.firstOrNull { it.title.equals(name, ignoreCase = true) } }
            ?: lists.firstOrNull()
    }
    val heroOpen = remember(hero, widget.sortMode) {
        sortedTidyShopItems(widget, hero?.items.orEmpty()).filter { !it.checked }
    }

    Surface(
        modifier = modifier.fillMaxWidth()
            .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
            .clip(RoundedCornerShape(widget.cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated)),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = Color.Transparent,
    ) {
        if (widget.isSquare && hero != null && heroOpen.isNotEmpty()) {
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) WidgetBackground(widget.backgroundUrl, currentUrl)
                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MdiIcon(widget.icon ?: "cart-outline", tint = accent, size = 16.dp)
                        Text(
                            widget.title ?: defaultTitle, color = appColors.onSurface,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.widgets_todo_remaining_short, remaining, total),
                            color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        hero.title, color = accent, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.12f))
                    val heroScroll = rememberScrollState()
                    Column(
                        Modifier.weight(1f).fadingEdges(heroScroll).verticalScroll(heroScroll),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        val canTick = canToggle && hero.canCheck(state.userId)
                        heroOpen.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { TidyShopSync.toggleItem(hero.id, item.id) },
                                    enabled = canTick,
                                )
                                Text(
                                    item.name, color = appColors.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val stateText = when {
                !state.isConnected -> stringResource(R.string.widgets_todo_sync_not_connected_short)
                state.status == TidyShopStatus.REVOKED -> stringResource(R.string.tidyshop_status_revoked)
                lists.isEmpty() -> stringResource(R.string.widgets_todo_sync_no_lists)
                total == 0 -> stringResource(R.string.widgets_todo_empty)
                remaining == 0 -> stringResource(R.string.widgets_todo_all_done)
                else -> stringResource(R.string.widgets_todo_remaining, remaining, total)
            }
            val dotColor = when {
                !state.isConnected || state.status == TidyShopStatus.REVOKED -> MaterialTheme.colorScheme.error
                remaining > 0 -> accent
                else -> appColors.onMuted
            }
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) {
                    WidgetBackground(widget.backgroundUrl, currentUrl)
                } else {
                    Box(
                        Modifier.align(Alignment.Center).size(84.dp)
                            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(21.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MdiIcon(widget.icon ?: "cart-outline", tint = accent, size = 44.dp)
                    }
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
                    )
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = itemCornerShape(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            widget.title ?: defaultTitle,
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(5.dp).background(dotColor, CircleShape))
                            Text(
                                stateText, color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun TidyShopTodoDialog(widget: HKITodoWidget, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val tidyShopInstalled = remember(context) {
        runCatching { context.packageManager.getLaunchIntentForPackage(TIDYSHOP_PACKAGE) != null }
            .getOrDefault(false)
    }
    val family = LocalVisibilityFamilyContext.current
    val state by TidyShopSync.state.collectAsState()
    val lists = remember(widget.tidyShopListIds, state.lists) { visibleTidyShopLists(widget, state) }
    // The dashboard's own permission still applies on top of the connector's: a household that
    // has locked this widget down for a child should not have that undone by the family server
    // handing the same phone an editable list.
    val dashboardAllows = remember(widget.editPermission, widget.editableMemberIds, family) {
        widget.canEdit(family.currentUserId, family.isAdmin)
    }

    var selectedListId by remember(widget.id) { mutableStateOf<String?>(null) }
    val selected = lists.firstOrNull { it.id == selectedListId } ?: lists.firstOrNull()
    var newItemText by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<Pair<String, TidyShopItem>?>(null) }

    LaunchedEffect(Unit) { TidyShopSync.refresh() }
    LaunchedEffect(selected?.id) {
        newItemText = ""
        newItemQuantity = ""
    }

    val canCheck = dashboardAllows && selected?.canCheck(state.userId) == true
    val canEditList = dashboardAllows && selected?.canEdit(state.userId) == true
    val remaining = selected?.remaining ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        stableHeight = true,
        dismissOnTapOutside = true,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (selected != null) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selected.isStoreList()) {
                                MdiIcon(
                                    "store-outline",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    size = 25.dp,
                                )
                            } else {
                                Text(selected.icon.ifBlank { "🛒" }, fontSize = 25.sp)
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(tidyShopListDisplayName(selected), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.widgets_todo_remaining_short, remaining, selected.items.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = appColors.onMuted,
                        )
                    }
                } else {
                    Text(
                        widget.title ?: stringResource(R.string.widgets_todo_shopping_title),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        text = {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TidyShopConnectionBanner(state)
                if (lists.isEmpty()) {
                    TidyShopEmptyState(
                        if (state.isConnected) stringResource(R.string.widgets_todo_sync_no_lists)
                        else stringResource(R.string.widgets_todo_sync_not_connected)
                    )
                } else {
                    if (lists.size > 1) {
                        val listScroll = rememberScrollState()
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(listScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            lists.forEach { list ->
                                FilterChip(
                                    selected = list.id == selected?.id,
                                    onClick = { selectedListId = list.id },
                                    label = {
                                        Text(
                                            tidyShopListDisplayName(list),
                                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    if (selected != null) {
                        if (canEditList) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                OutlinedTextField(
                                    value = newItemText, onValueChange = { newItemText = it },
                                    placeholder = { Text(stringResource(R.string.widgets_todo_add_placeholder)) },
                                    singleLine = true, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        TidyShopSync.addItem(selected.id, newItemText, newItemQuantity.toIntOrNull() ?: 0)
                                        newItemText = ""
                                        newItemQuantity = ""
                                    }),
                                )
                                val quantityDescription = stringResource(R.string.widgets_todo_quantity)
                                Surface(
                                    modifier = Modifier.width(64.dp).height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.55f)),
                                ) {
                                    BasicTextField(
                                        value = newItemQuantity,
                                        onValueChange = { entered ->
                                            newItemQuantity = entered.filter(Char::isDigit).take(3)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                            .semantics { contentDescription = quantityDescription }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = appColors.onSurface),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        decorationBox = { inner ->
                                            if (newItemQuantity.isEmpty()) {
                                                Text(
                                                    "1",
                                                    color = appColors.onMuted,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                            }
                                            inner()
                                        },
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        TidyShopSync.addItem(selected.id, newItemText, newItemQuantity.toIntOrNull() ?: 0)
                                        newItemText = ""
                                        newItemQuantity = ""
                                    },
                                    enabled = newItemText.isNotBlank(),
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.12f))
                        } else {
                            Text(
                                if (canCheck) stringResource(R.string.widgets_todo_tidyshop_check_only)
                                else stringResource(R.string.widgets_todo_tidyshop_read_only),
                                color = appColors.onMuted, style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        val ordered = remember(selected.items, widget.sortMode) {
                            sortedTidyShopItems(widget, selected.items)
                        }
                        val open = ordered.filterNot { it.checked }
                        val done = ordered.filter { it.checked }
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TidyShopSectionHeader(
                                title = stringResource(R.string.widgets_todo_filter_active),
                                count = open.size,
                            )
                            if (open.isEmpty()) {
                                TidyShopAllBoughtState()
                            }
                            open.forEach { item ->
                                TidyShopItemRow(
                                    item = item,
                                    canCheck = canCheck,
                                    canEdit = canEditList,
                                    onToggle = { TidyShopSync.toggleItem(selected.id, item.id) },
                                    onDelete = { TidyShopSync.removeItem(selected.id, item.id) },
                                    onClick = { if (canEditList) editingItem = selected.id to item },
                                )
                            }
                            if (done.isNotEmpty() && widget.showCompleted) {
                                TidyShopSectionHeader(
                                    title = stringResource(R.string.widgets_todo_completed_section, done.size),
                                    count = null,
                                    action = if (canEditList) stringResource(R.string.widgets_todo_clear_completed) else null,
                                    onAction = { TidyShopSync.clearChecked(selected.id) },
                                )
                                done.forEach { item ->
                                    TidyShopItemRow(
                                        item = item,
                                        canCheck = canCheck,
                                        canEdit = canEditList,
                                        onToggle = { TidyShopSync.toggleItem(selected.id, item.id) },
                                        onDelete = { TidyShopSync.removeItem(selected.id, item.id) },
                                        onClick = { if (canEditList) editingItem = selected.id to item },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { TidyShopSync.refresh() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.tidyshop_refresh))
                }
                TextButton(
                    onClick = { openTidyShopApp(context, selected?.id) },
                    enabled = tidyShopInstalled,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.connected_apps_tidyshop),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa773)) } },
    )

    editingItem?.let { (listId, item) ->
        TidyShopItemEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, quantity ->
                TidyShopSync.renameItem(listId, item.id, name, quantity)
                editingItem = null
            },
            onDelete = {
                TidyShopSync.removeItem(listId, item.id)
                editingItem = null
            },
        )
    }
}

@Composable
private fun TidyShopSectionHeader(
    title: String,
    count: Int?,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val appColors = LocalHKIAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (count != null) {
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                shape = CircleShape,
                color = appColors.subtleSurface,
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun TidyShopAllBoughtState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("✓", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.widgets_todo_all_done),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** One line saying whether what is on screen is current, shown only when it is not simply live. */
@Composable
private fun TidyShopConnectionBanner(state: TidyShopState) {
    val appColors = LocalHKIAppColors.current
    val message = when {
        !state.isConnected -> stringResource(R.string.widgets_todo_sync_not_connected)
        state.status == TidyShopStatus.REVOKED -> stringResource(R.string.tidyshop_status_revoked)
        state.status == TidyShopStatus.ERROR ->
            state.error?.let { stringResource(R.string.tidyshop_status_error_detail, it) }
                ?: stringResource(R.string.tidyshop_status_error)
        else -> return
    }
    val isError = state.status == TidyShopStatus.REVOKED || state.status == TidyShopStatus.ERROR || !state.isConnected
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (isError) MaterialTheme.colorScheme.error else appColors.onMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun TidyShopItemRow(
    item: TidyShopItem,
    canCheck: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = RoundedCornerShape(18.dp), color = appColors.subtleSurface,
        modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit) { onClick() },
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(checked = item.checked, onCheckedChange = { onToggle() }, enabled = canCheck)
            Text(
                buildString {
                    append(item.name)
                    item.measureLabel.takeIf { it.isNotBlank() }?.let { append("  ·  "); append(it) }
                },
                color = if (item.checked) appColors.onMuted else appColors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (canEdit) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TidyShopItemEditDialog(
    item: TidyShopItem,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableStateOf(if (item.quantity > 0) item.quantity.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widgets_todo_edit_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.widgets_todo_add_placeholder)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { entered -> quantity = entered.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.widgets_todo_quantity)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                // TidyShop's size and unit are left alone rather than surfaced here: this widget
                // has no unit picker, and blanking them would quietly rewrite the family's data.
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, quantity.toIntOrNull() ?: 0) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.widgets_todo_delete_item), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun TidyShopEmptyState(message: String) {
    val appColors = LocalHKIAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MdiIcon("cart-outline", tint = appColors.onMuted.copy(alpha = 0.4f), size = 40.dp)
        Text(message, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
    }
}

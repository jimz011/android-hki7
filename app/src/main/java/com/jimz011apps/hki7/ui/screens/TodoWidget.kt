@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimz011apps.hki7.data.HKITodoItem
import com.jimz011apps.hki7.data.HKITodoWidget
import com.jimz011apps.hki7.data.TODO_EDIT_ADMIN_ONLY
import com.jimz011apps.hki7.data.TODO_EDIT_EVERYONE
import com.jimz011apps.hki7.data.TODO_EDIT_SPECIFIC
import com.jimz011apps.hki7.data.TODO_ITEM_PRIORITY_HIGH
import com.jimz011apps.hki7.data.TODO_ITEM_PRIORITY_LOW
import com.jimz011apps.hki7.data.TODO_ITEM_PRIORITY_NORMAL
import com.jimz011apps.hki7.data.canEdit
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.LocalVisibilityFamilyContext
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle
import com.jimz011apps.hki7.ui.components.SettingsSubcategory
import com.jimz011apps.hki7.ui.components.SettingsTabRow
import com.jimz011apps.hki7.ui.components.VisibilityEditor
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodoWidgetItem(
    widget: HKITodoWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onUpdate: (HKITodoWidget) -> Unit
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val currentUrl by viewModel.currentUrl.collectAsState()
    var showDialog by remember(widget.id) { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        TodoCard(
            widget = widget,
            currentUrl = currentUrl,
            modifier = Modifier.clickable(enabled = !isEditMode) { showDialog = true }
        )
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
    if (showDialog) {
        TodoDialog(widget, onUpdate) { showDialog = false }
    }
}

@Composable
private fun TodoCard(widget: HKITodoWidget, currentUrl: String, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val remaining = widget.items.count { !it.checked }
    val total = widget.items.size
    val defaultTitle = if (widget.listMode == "shopping") {
        stringResource(R.string.widgets_todo_shopping_title)
    } else {
        stringResource(R.string.widgets_todo_title)
    }
    val stateText = when {
        total == 0 -> stringResource(R.string.widgets_todo_empty)
        remaining == 0 -> stringResource(R.string.widgets_todo_all_done)
        else -> stringResource(R.string.widgets_todo_remaining, remaining, total)
    }
    val dotColor = if (remaining > 0) accent else appColors.onMuted

    Surface(
        modifier = modifier.fillMaxWidth()
            .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
            .clip(RoundedCornerShape(widget.cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated)),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box {
            if (!widget.backgroundUrl.isNullOrBlank()) {
                WidgetBackground(widget.backgroundUrl, currentUrl)
            } else {
                Box(
                    Modifier.align(Alignment.Center).size(56.dp)
                        .background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MdiIcon(widget.icon ?: "format-list-checks", tint = accent, size = 28.dp)
                }
            }

            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
                )
            )

            // Same bottom-left pill as the F1/camera/vacuum/waste/parcel cards.
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        widget.title ?: defaultTitle,
                        color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).background(dotColor, CircleShape))
                        Text(
                            stateText, color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatting helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun priorityWeight(priority: String): Int = when (priority) {
    TODO_ITEM_PRIORITY_HIGH -> 2
    TODO_ITEM_PRIORITY_LOW -> 0
    else -> 1
}

@Composable
private fun formatTodoDueDate(iso: String): String {
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (date.year == LocalDate.now().year) "MMM d" else "MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun isoDateToEpochMillis(iso: String?): Long? =
    iso?.let { runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() }

private fun epochMillisToIsoDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

// ─────────────────────────────────────────────────────────────────────────────
// Shared bits: priority selector, due-date field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodoPrioritySelector(priority: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            TODO_ITEM_PRIORITY_LOW to R.string.widgets_todo_priority_low,
            TODO_ITEM_PRIORITY_NORMAL to R.string.widgets_todo_priority_normal,
            TODO_ITEM_PRIORITY_HIGH to R.string.widgets_todo_priority_high
        ).forEach { (value, labelRes) ->
            FilterChip(selected = priority == value, onClick = { onChange(value) }, label = { Text(stringResource(labelRes), fontSize = 12.sp) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoDueDateField(dueDate: String?, onChange: (String?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                dueDate?.let { formatTodoDueDate(it) } ?: stringResource(R.string.widgets_todo_due_date),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (dueDate != null) {
            TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.widgets_todo_clear_due_date)) }
        }
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoDateToEpochMillis(dueDate))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(epochMillisToIsoDate(it)) }
                    showPicker = false
                }) { Text(stringResource(R.string.ui_save_efc007a)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
        ) {
            DatePicker(state = state)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodoItemRow(item: HKITodoItem, canEdit: Boolean, onToggle: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val overdue = remember(item.dueDate, item.checked) {
        !item.checked && item.dueDate?.let { runCatching { LocalDate.parse(it).isBefore(LocalDate.now()) }.getOrDefault(false) } == true
    }
    Surface(
        shape = itemCornerShape(), color = appColors.subtleSurface,
        modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit) { onClick() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(checked = item.checked, onCheckedChange = { onToggle() }, enabled = canEdit)
            if (item.priority == TODO_ITEM_PRIORITY_HIGH) {
                MdiIcon("flag", tint = Color(0xFFE53935), size = 14.dp)
                Spacer(Modifier.width(2.dp))
            } else if (item.priority == TODO_ITEM_PRIORITY_LOW) {
                MdiIcon("flag-outline", tint = appColors.onMuted, size = 14.dp)
                Spacer(Modifier.width(2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(item.text)
                        item.quantity?.takeIf { it.isNotBlank() }?.let { append("  ·  "); append(it) }
                    },
                    color = if (item.checked) appColors.onMuted else appColors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    item.category?.takeIf { it.isNotBlank() },
                    item.note?.takeIf { it.isNotBlank() },
                    item.dueDate?.let { formatTodoDueDate(it) },
                    item.checkedByName?.let { stringResource(R.string.widgets_todo_checked_by, it) }
                        ?: item.addedByName?.let { stringResource(R.string.widgets_todo_added_by, it) }
                ).joinToString("  ·  ")
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = if (overdue) MaterialTheme.colorScheme.error else appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (canEdit) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TodoEmptyState(message: String) {
    val appColors = LocalHKIAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MdiIcon("format-list-checks", tint = appColors.onMuted.copy(alpha = 0.4f), size = 40.dp)
        Text(message, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodoDialog(widget: HKITodoWidget, onUpdate: (HKITodoWidget) -> Unit, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val family = LocalVisibilityFamilyContext.current
    val canEdit = remember(widget.editPermission, widget.editableMemberIds, family) {
        widget.canEdit(family.currentUserId, family.isAdmin)
    }
    var filter by remember(widget.id) { mutableStateOf(if (widget.showCompleted) "all" else "active") }
    var newItemText by remember { mutableStateOf("") }
    var showAdvancedAdd by remember { mutableStateOf(false) }
    var newQuantity by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf(TODO_ITEM_PRIORITY_NORMAL) }
    var newDueDate by remember { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<HKITodoItem?>(null) }

    fun persist(items: List<HKITodoItem>) = onUpdate(widget.copy(items = items))

    fun currentUserName(): String? = family.users.firstOrNull { it.id == family.currentUserId }?.name

    fun addItem() {
        val text = newItemText.trim()
        if (text.isEmpty()) return
        val item = HKITodoItem(
            id = UUID.randomUUID().toString(),
            text = text,
            quantity = newQuantity.trim().ifBlank { null },
            category = newCategory.trim().ifBlank { null },
            priority = newPriority,
            dueDate = newDueDate,
            addedByUserId = family.currentUserId,
            addedByName = currentUserName()
        )
        persist(widget.items + item)
        newItemText = ""; newQuantity = ""; newCategory = ""; newPriority = TODO_ITEM_PRIORITY_NORMAL; newDueDate = null
    }

    fun toggleItem(item: HKITodoItem) {
        val nowChecked = !item.checked
        persist(
            widget.items.map {
                if (it.id == item.id) {
                    it.copy(
                        checked = nowChecked,
                        checkedByUserId = if (nowChecked) family.currentUserId else null,
                        checkedByName = if (nowChecked) currentUserName() else null
                    )
                } else it
            }
        )
    }

    fun removeItem(item: HKITodoItem) = persist(widget.items.filterNot { it.id == item.id })
    fun clearCompleted() = persist(widget.items.filterNot { it.checked })

    val sorted = remember(widget.items, widget.sortMode) {
        when (widget.sortMode) {
            "alphabetical" -> widget.items.sortedBy { it.text.lowercase(Locale.getDefault()) }
            "priority" -> widget.items.sortedByDescending { priorityWeight(it.priority) }
            "newest" -> widget.items.reversed()
            else -> widget.items
        }
    }
    val filtered = when (filter) {
        "active" -> sorted.filterNot { it.checked }
        "done" -> sorted.filter { it.checked }
        else -> sorted
    }
    val remaining = widget.items.count { !it.checked }
    val defaultTitle = if (widget.listMode == "shopping") {
        stringResource(R.string.widgets_todo_shopping_title)
    } else {
        stringResource(R.string.widgets_todo_title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        stableHeight = true,
        dismissOnTapOutside = true,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(widget.title ?: defaultTitle, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.widgets_todo_remaining_short, remaining, widget.items.size),
                    style = MaterialTheme.typography.labelMedium, color = appColors.onMuted
                )
            }
        },
        text = {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canEdit) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newItemText, onValueChange = { newItemText = it },
                                placeholder = { Text(stringResource(R.string.widgets_todo_add_placeholder)) },
                                singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { addItem() })
                            )
                            IconButton(onClick = { showAdvancedAdd = !showAdvancedAdd }) {
                                Icon(if (showAdvancedAdd) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }
                            IconButton(onClick = { addItem() }, enabled = newItemText.isNotBlank()) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (showAdvancedAdd) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newQuantity, onValueChange = { newQuantity = it },
                                    label = { Text(stringResource(R.string.widgets_todo_quantity)) },
                                    singleLine = true, modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newCategory, onValueChange = { newCategory = it },
                                    label = { Text(stringResource(R.string.widgets_todo_category)) },
                                    singleLine = true, modifier = Modifier.weight(1f)
                                )
                            }
                            TodoPrioritySelector(newPriority) { newPriority = it }
                            TodoDueDateField(newDueDate) { newDueDate = it }
                        }
                    }
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.12f))
                } else {
                    Text(
                        stringResource(R.string.widgets_todo_read_only),
                        color = appColors.onMuted, style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == "active", onClick = { filter = "active" }, label = { Text(stringResource(R.string.widgets_todo_filter_active), fontSize = 12.sp) })
                    FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text(stringResource(R.string.widgets_todo_filter_all), fontSize = 12.sp) })
                    FilterChip(selected = filter == "done", onClick = { filter = "done" }, label = { Text(stringResource(R.string.widgets_todo_filter_done), fontSize = 12.sp) })
                }
                if (filtered.isEmpty()) {
                    TodoEmptyState(stringResource(R.string.widgets_todo_empty))
                } else {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtered.forEach { item ->
                            TodoItemRow(
                                item = item,
                                canEdit = canEdit,
                                onToggle = { toggleItem(item) },
                                onDelete = { removeItem(item) },
                                onClick = { if (canEdit) editingItem = item }
                            )
                        }
                    }
                }
                if (canEdit && widget.items.any { it.checked }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { clearCompleted() }) { Text(stringResource(R.string.widgets_todo_clear_completed)) }
                    }
                }
            }
        },
        confirmButton = {}
    )

    editingItem?.let { item ->
        TodoItemEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                persist(widget.items.map { if (it.id == updated.id) updated else it })
                editingItem = null
            },
            onDelete = {
                removeItem(item)
                editingItem = null
            }
        )
    }
}

@Composable
private fun TodoItemEditDialog(
    item: HKITodoItem,
    onDismiss: () -> Unit,
    onSave: (HKITodoItem) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(item.id) { mutableStateOf(item.text) }
    var note by remember(item.id) { mutableStateOf(item.note ?: "") }
    var quantity by remember(item.id) { mutableStateOf(item.quantity ?: "") }
    var category by remember(item.id) { mutableStateOf(item.category ?: "") }
    var priority by remember(item.id) { mutableStateOf(item.priority) }
    var dueDate by remember(item.id) { mutableStateOf(item.dueDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widgets_todo_edit_item)) },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text(stringResource(R.string.widgets_todo_item_text)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity, onValueChange = { quantity = it },
                        label = { Text(stringResource(R.string.widgets_todo_quantity)) },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category, onValueChange = { category = it },
                        label = { Text(stringResource(R.string.widgets_todo_category)) },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.widgets_todo_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.widgets_todo_priority), style = MaterialTheme.typography.labelLarge)
                TodoPrioritySelector(priority) { priority = it }
                TodoDueDateField(dueDate) { dueDate = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            text = text.trim(),
                            note = note.trim().ifBlank { null },
                            quantity = quantity.trim().ifBlank { null },
                            category = category.trim().ifBlank { null },
                            priority = priority,
                            dueDate = dueDate
                        )
                    )
                },
                enabled = text.isNotBlank()
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.widgets_todo_delete_item), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodoWidgetSettingsDialog(
    widget: HKITodoWidget,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKITodoWidget) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val family = LocalVisibilityFamilyContext.current
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "format-list-checks") }
    var listMode by remember(widget) { mutableStateOf(widget.listMode) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var sortMode by remember(widget) { mutableStateOf(widget.sortMode) }
    var showCompleted by remember(widget) { mutableStateOf(widget.showCompleted) }
    var editPermission by remember(widget) { mutableStateOf(widget.editPermission) }
    var editableMemberIds by remember(widget) { mutableStateOf(widget.editableMemberIds.toSet()) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("general") }
    var visSpec by remember(widget) { mutableStateOf(widget.toVisibilitySpec()) }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName = it; showIconPicker = false }
        )
    }

    val scroll = rememberScrollState()
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            ModernSettingsDialogTitle(
                if (widget.listMode == "shopping") stringResource(R.string.widgets_todo_shopping_title) else stringResource(R.string.widgets_todo_title),
                stringResource(R.string.widgets_todo_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight().fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsTabRow(
                    tabs = listOf(
                        "general" to stringResource(R.string.widgets_todo_tab_general),
                        "permissions" to stringResource(R.string.widgets_todo_tab_permissions),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "general") {
                    SettingsSubcategory(
                        stringResource(R.string.widgets_todo_tab_general),
                        stringResource(R.string.widgets_todo_subtitle)
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_title_768e0c1)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.widgets_todo_list_type), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = listMode == "todo", onClick = { listMode = "todo" }, label = { Text(stringResource(R.string.widgets_todo_type_todo)) })
                        FilterChip(selected = listMode == "shopping", onClick = { listMode = "shopping" }, label = { Text(stringResource(R.string.widgets_todo_type_shopping)) })
                    }
                    Text(stringResource(R.string.widgets_todo_sort_mode), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "manual" to R.string.widgets_todo_sort_manual,
                            "alphabetical" to R.string.widgets_todo_sort_alphabetical,
                            "priority" to R.string.widgets_todo_sort_priority,
                            "newest" to R.string.widgets_todo_sort_newest
                        ).forEach { (value, labelRes) ->
                            FilterChip(selected = sortMode == value, onClick = { sortMode = value }, label = { Text(stringResource(labelRes), fontSize = 12.sp) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.widgets_todo_show_completed), modifier = Modifier.weight(1f))
                        Switch(checked = showCompleted, onCheckedChange = { showCompleted = it })
                    }
                }
                if (settingsPage == "permissions") {
                    SettingsSubcategory(
                        stringResource(R.string.widgets_todo_tab_permissions),
                        stringResource(R.string.widgets_todo_permissions_subtitle)
                    )
                    listOf(
                        TODO_EDIT_EVERYONE to R.string.widgets_todo_perm_everyone,
                        TODO_EDIT_SPECIFIC to R.string.widgets_todo_perm_specific,
                        TODO_EDIT_ADMIN_ONLY to R.string.widgets_todo_perm_admin_only
                    ).forEach { (value, labelRes) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { editPermission = value },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = editPermission == value, onClick = { editPermission = value })
                            Text(stringResource(labelRes), modifier = Modifier.weight(1f))
                        }
                    }
                    if (editPermission == TODO_EDIT_SPECIFIC) {
                        if (!family.isAdmin) {
                            Text(
                                stringResource(R.string.widgets_todo_permissions_admin_required),
                                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                            )
                        } else if (family.users.isEmpty()) {
                            Text(
                                stringResource(R.string.widgets_todo_no_users_found),
                                color = appColors.onMuted, style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            family.users.filterNot { it.isAdmin }.forEach { user ->
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        editableMemberIds = if (user.id in editableMemberIds) editableMemberIds - user.id else editableMemberIds + user.id
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = user.id in editableMemberIds,
                                        onCheckedChange = { checked ->
                                            editableMemberIds = if (checked) editableMemberIds + user.id else editableMemberIds - user.id
                                        }
                                    )
                                    Text(user.name, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (settingsPage == "appearance") {
                    SettingsSubcategory(
                        stringResource(R.string.ui_appearance_41def7a),
                        stringResource(R.string.ui_image_style_size_shape_and_background_40c17b6)
                    )
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                    }
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdiIcon(iconName, size = 20.dp)
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                    WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
                if (settingsPage == "visibility") {
                    SettingsSubcategory(
                        stringResource(R.string.ui_visibility_7d9ff4f),
                        stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66)
                    )
                    VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        listMode = listMode,
                        width = width,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        backgroundUrl = backgroundUrl,
                        sortMode = sortMode,
                        showCompleted = showCompleted,
                        editPermission = editPermission,
                        editableMemberIds = editableMemberIds.toList(),
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

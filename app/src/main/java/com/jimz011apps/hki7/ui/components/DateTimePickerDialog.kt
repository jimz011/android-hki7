package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.staticCompositionLocalOf
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIBadge
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.HKIRoomWidget
import com.jimz011apps.hki7.data.HKIVisibilityCondition
import com.jimz011apps.hki7.data.VISIBILITY_MATCH_ALL
import com.jimz011apps.hki7.data.VISIBILITY_MATCH_ANY
import com.jimz011apps.hki7.data.VISIBILITY_TYPE_ENTITY
import com.jimz011apps.hki7.data.VISIBILITY_TYPE_TIME
import com.jimz011apps.hki7.data.normalizedVisibilityConditions
import com.jimz011apps.hki7.data.suggestedAutomationStates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A Material 3 date-and-time picker in the same style as the Energy view's date picker: the standard
 * [DatePickerDialog] with a Date/Time toggle so the caller gets a full [LocalDateTime] back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val base = remember { (initial ?: LocalDateTime.now()).withSecond(0).withNano(0) }
    // The date picker stores UTC-midnight millis, so keep the whole round-trip on UTC to avoid a
    // timezone shifting the chosen day.
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = base.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val timeState = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
    var tab by remember { mutableStateOf("date") }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = dateState.selectedDateMillis ?: return@TextButton
                val date: LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onConfirm(LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)))
            }) { Text(stringResource(R.string.dlg_done)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.dlg_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) }
            }
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tab == "date", onClick = { tab = "date" }, label = { Text(stringResource(R.string.dlg_date)) })
                FilterChip(selected = tab == "time", onClick = { tab = "time" }, label = { Text(stringResource(R.string.dlg_time)) })
            }
            if (tab == "date") {
                DatePicker(state = dateState)
            } else {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        }
    }
}

/**
 * Portable visibility rule for a button, badge, or widget (see `isVisibleAt`).
 *
 * [conditions] and [match] are the live representation. The remaining fields mirror the superseded
 * flat layout and are always left at their defaults by the editor, so the existing save sites write
 * those defaults back and the flat fields are cleared as items are edited — a rule now lives in
 * exactly one place. Reading still understands the old layout via [toVisibilitySpec].
 */
data class VisibilitySpec(
    val hidden: Boolean = false,
    val conditions: List<HKIVisibilityCondition> = emptyList(),
    val match: String = VISIBILITY_MATCH_ALL,
    val start: String? = null,
    val end: String? = null,
    val rangeMode: String = "show",
    val recurrence: String = "none",
    val conditionEntityId: String? = null,
    val conditionState: String? = null,
    val conditionNegate: Boolean = false,
)

fun HKIRoomWidget.toVisibilitySpec(): VisibilitySpec = VisibilitySpec(
    hidden = isHidden,
    conditions = normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ),
    match = visibilityMatch.ifBlank { VISIBILITY_MATCH_ALL },
)

fun HKIBadge.toVisibilitySpec(): VisibilitySpec = VisibilitySpec(
    hidden = hidden,
    conditions = normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ),
    match = visibilityMatch.ifBlank { VISIBILITY_MATCH_ALL },
)

/** Human-readable label for a scheduled bound stored as an ISO local date-time. */
@Composable
fun formatVisibilityBound(iso: String?): String {
    if (iso.isNullOrBlank()) return stringResource(R.string.dlg_any)
    return runCatching {
        LocalDateTime.parse(iso).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        )
    }.getOrDefault(iso)
}

/**
 * Supplies the full entity catalog on demand to shared editors that embed an entity picker (see
 * [VisibilityEditor]). Deliberately a lambda rather than the list itself: the value is read only
 * when a picker actually opens, so providing it never recomposes the tree as entity states change.
 */
val LocalEntityCatalogProvider = staticCompositionLocalOf<() -> List<HAEntity>> { { emptyList() } }

/**
 * Inline visibility editor embedded in a button's, badge's, or widget's settings.
 *
 * Three modes: Always, Hidden, or Conditional. Conditional holds a list of rule blocks — each either
 * a time window or an entity-state check — combined with match-all (AND) or match-any (OR). Blocks
 * can be added and removed freely, so an item can depend on several entities and/or windows at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisibilityEditor(spec: VisibilitySpec, onChange: (VisibilitySpec) -> Unit) {
    val appColors = LocalHKIAppColors.current
    var emptyConditional by remember { mutableStateOf(false) }
    val mode = when {
        spec.hidden -> "hidden"
        spec.conditions.isNotEmpty() -> "conditional"
        emptyConditional -> "conditional"
        else -> "always"
    }
    // Index of the block whose picker/date dialog is currently open.
    var pickingStartFor by remember { mutableStateOf<Int?>(null) }
    var pickingEndFor by remember { mutableStateOf<Int?>(null) }
    var pickingEntityFor by remember { mutableStateOf<Int?>(null) }

    val catalogProvider = LocalEntityCatalogProvider.current
    // Snapshotted when a picker opens rather than observed, so the editor doesn't recompose on every
    // unrelated Home Assistant state change while it is on screen.
    var catalog by remember { mutableStateOf<List<HAEntity>>(emptyList()) }

    fun updateBlock(index: Int, transform: (HKIVisibilityCondition) -> HKIVisibilityCondition) {
        onChange(spec.copy(conditions = spec.conditions.mapIndexed { i, c -> if (i == index) transform(c) else c }))
    }
    fun addBlock(block: HKIVisibilityCondition) {
        onChange(spec.copy(hidden = false, conditions = spec.conditions + block))
    }
    fun newTimeBlock() = HKIVisibilityCondition(type = VISIBILITY_TYPE_TIME)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "always", onClick = { emptyConditional = false; onChange(VisibilitySpec()) }, label = { Text(stringResource(R.string.dlg_always)) })
            FilterChip(selected = mode == "hidden", onClick = { emptyConditional = false; onChange(VisibilitySpec(hidden = true)) }, label = { Text(stringResource(R.string.dlg_hidden)) })
            FilterChip(
                selected = mode == "conditional",
                // Conditional starts empty; the user explicitly chooses a time or entity rule.
                onClick = { emptyConditional = true; onChange(spec.copy(hidden = false)) },
                label = { Text(stringResource(R.string.dlg_vis_conditional)) }
            )
        }

        if (mode == "conditional") {
            // Only meaningful once two blocks can disagree.
            if (spec.conditions.size > 1) {
                Text(stringResource(R.string.dlg_vis_match), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = spec.match != VISIBILITY_MATCH_ANY,
                        onClick = { onChange(spec.copy(match = VISIBILITY_MATCH_ALL)) },
                        label = { Text(stringResource(R.string.dlg_vis_match_all)) }
                    )
                    FilterChip(
                        selected = spec.match == VISIBILITY_MATCH_ANY,
                        onClick = { onChange(spec.copy(match = VISIBILITY_MATCH_ANY)) },
                        label = { Text(stringResource(R.string.dlg_vis_match_any)) }
                    )
                }
            }

            spec.conditions.forEachIndexed { index, block ->
                if (index > 0) {
                    Text(
                        stringResource(
                            if (spec.match == VISIBILITY_MATCH_ANY) R.string.dlg_vis_or else R.string.dlg_vis_and
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.onMuted
                    )
                }
                Surface(shape = itemCornerShape(), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    if (block.type == VISIBILITY_TYPE_TIME) R.string.dlg_vis_time_schedule
                                    else R.string.dlg_vis_entity_state
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = appColors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                onChange(spec.copy(conditions = spec.conditions.filterIndexed { i, _ -> i != index }))
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dlg_remove))
                            }
                        }

                        if (block.type == VISIBILITY_TYPE_TIME) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = block.rangeMode == "show", onClick = { updateBlock(index) { it.copy(rangeMode = "show") } }, label = { Text(stringResource(R.string.dlg_visible_during)) })
                                FilterChip(selected = block.rangeMode == "hide", onClick = { updateBlock(index) { it.copy(rangeMode = "hide") } }, label = { Text(stringResource(R.string.dlg_hidden_during)) })
                            }
                            Text(stringResource(R.string.dlg_repeat), style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "none" to stringResource(R.string.dlg_once),
                                    "daily" to stringResource(R.string.dlg_daily),
                                    "weekly" to stringResource(R.string.dlg_weekly),
                                    "monthly" to stringResource(R.string.dlg_monthly),
                                    "yearly" to stringResource(R.string.dlg_yearly),
                                ).forEach { (value, txt) ->
                                    FilterChip(selected = block.recurrence == value, onClick = { updateBlock(index) { it.copy(recurrence = value) } }, label = { Text(txt) })
                                }
                            }
                            OutlinedButton(onClick = { pickingStartFor = index }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.dlg_start, formatVisibilityBound(block.start)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(onClick = { pickingEndFor = index }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.dlg_end, formatVisibilityBound(block.end)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                when (block.recurrence) {
                                    "daily" -> stringResource(R.string.dlg_repeats_every_day_only_the_time_of_day_is)
                                    "weekly" -> stringResource(R.string.dlg_repeats_every_week_only_the_weekday_and_time_are)
                                    "monthly" -> stringResource(R.string.dlg_repeats_every_month_only_the_day_of_the_month)
                                    "yearly" -> stringResource(R.string.dlg_repeats_every_year_the_year_is_ignored_so_e)
                                    else -> stringResource(R.string.dlg_the_exact_dates_you_pick_leave_a_bound_as)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onMuted
                            )
                        } else {
                            val blockEntity = catalog.firstOrNull { it.entity_id == block.entityId }
                            OutlinedButton(
                                onClick = { catalog = catalogProvider(); pickingEntityFor = index },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    blockEntity?.friendlyName
                                        ?: block.entityId?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.uif_choose_entity),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !block.negate, onClick = { updateBlock(index) { it.copy(negate = false) } }, label = { Text(stringResource(R.string.dlg_condition_is)) })
                                FilterChip(selected = block.negate, onClick = { updateBlock(index) { it.copy(negate = true) } }, label = { Text(stringResource(R.string.dlg_condition_is_not)) })
                            }
                            // Known states for the picked entity, with a "Custom state" escape hatch
                            // for values that can't be enumerated (numbers, integration-specific text).
                            StateSelectorField(
                                label = stringResource(R.string.dlg_condition_state),
                                selected = block.state.orEmpty(),
                                options = suggestedAutomationStates(blockEntity),
                                enabled = !block.entityId.isNullOrBlank(),
                                onSelected = { value -> updateBlock(index) { it.copy(state = value.ifBlank { null }) } }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addBlock(newTimeBlock()) }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.dlg_vis_time_schedule))
                }
                OutlinedButton(onClick = {
                    catalog = catalogProvider()
                    pickingEntityFor = spec.conditions.size
                    addBlock(HKIVisibilityCondition(type = VISIBILITY_TYPE_ENTITY))
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.dlg_vis_entity_state))
                }
            }
        }
    }

    pickingEntityFor?.let { index ->
        val current = spec.conditions.getOrNull(index)
        AdvancedEntitySearchDialog(
            allEntities = catalog,
            title = stringResource(R.string.uif_choose_entity),
            singleSelect = true,
            preselectedIds = setOfNotNull(current?.entityId),
            onDismiss = { pickingEntityFor = null },
            onEntitiesSelected = { ids ->
                val picked = ids.firstOrNull()
                // Switching entity clears a state that likely doesn't apply to the new one.
                if (picked != null && picked != current?.entityId) {
                    updateBlock(index) { it.copy(entityId = picked, state = null) }
                }
                pickingEntityFor = null
            }
        )
    }
    pickingStartFor?.let { index ->
        val current = spec.conditions.getOrNull(index)
        DateTimePickerDialog(
            initial = current?.start?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingStartFor = null },
            onClear = { updateBlock(index) { it.copy(start = null) }; pickingStartFor = null },
            onConfirm = { picked -> updateBlock(index) { it.copy(start = picked.toString()) }; pickingStartFor = null }
        )
    }
    pickingEndFor?.let { index ->
        val current = spec.conditions.getOrNull(index)
        DateTimePickerDialog(
            initial = current?.end?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            onDismiss = { pickingEndFor = null },
            onClear = { updateBlock(index) { it.copy(end = null) }; pickingEndFor = null },
            onConfirm = { picked -> updateBlock(index) { it.copy(end = picked.toString()) }; pickingEndFor = null }
        )
    }
}

fun HKIButtonConfig.toVisibilitySpec(): VisibilitySpec = VisibilitySpec(
    hidden = hidden,
    conditions = normalizedVisibilityConditions(
        visibilityConditions, visibilityStart, visibilityEnd, visibilityRangeMode, visibilityRecurrence,
        visibilityConditionEntityId, visibilityConditionState, visibilityConditionNegate,
    ),
    match = visibilityMatch.ifBlank { VISIBILITY_MATCH_ALL },
)

fun HKIButtonConfig.withVisibilitySpec(spec: VisibilitySpec): HKIButtonConfig = copy(
    hidden = spec.hidden,
    visibilityConditions = spec.conditions,
    visibilityMatch = spec.match,
    // Clear the superseded flat fields so the rule lives only in the block list.
    visibilityStart = null, visibilityEnd = null,
    visibilityRangeMode = "show", visibilityRecurrence = "none",
    visibilityConditionEntityId = null, visibilityConditionState = null, visibilityConditionNegate = false,
)

/** Small standalone dialog for editing one item's hide/schedule/condition rule inside a multi-item
 * widget (e.g. one sensor line, one calendar, one carrier account) — reuses [VisibilityEditor]. */
@Composable
fun ItemVisibilityDialog(
    label: String,
    config: HKIButtonConfig,
    onDismiss: () -> Unit,
    onSave: (HKIButtonConfig) -> Unit,
) {
    var spec by remember(config) { mutableStateOf(config.toVisibilitySpec()) }
    ModernAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = { VisibilityEditor(spec) { spec = it } },
        confirmButton = { Button(onClick = { onSave(config.withVisibilitySpec(spec)); onDismiss() }) { Text(stringResource(R.string.ui_done_e9b450d)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    )
}

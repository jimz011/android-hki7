package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.AutomationRecipe
import com.jimz011apps.hki7.data.AutomationSection
import com.jimz011apps.hki7.data.HAActionDefinition
import com.jimz011apps.hki7.data.HAAutomationDocument
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.AUTOMATION_GROUP_KINDS
import com.jimz011apps.hki7.data.automationBlockKind
import com.jimz011apps.hki7.data.automationBlocksAtPath
import com.jimz011apps.hki7.data.automationGroupChildren
import com.jimz011apps.hki7.data.automationPathExists
import com.jimz011apps.hki7.data.withAutomationBlocksAtPath
import com.jimz011apps.hki7.data.automationBlockSummary
import com.jimz011apps.hki7.data.automationElements
import com.jimz011apps.hki7.data.automationsIncludingRegistry
import com.jimz011apps.hki7.data.automationItems
import com.jimz011apps.hki7.data.isSupportedAutomationBlock
import com.jimz011apps.hki7.data.newAutomationBlock
import com.jimz011apps.hki7.data.newAutomationConfig
import com.jimz011apps.hki7.data.suggestedAutomationStates
import com.jimz011apps.hki7.data.stringValue
import com.jimz011apps.hki7.data.withAutomationItems
import com.jimz011apps.hki7.data.withAutomationText
import com.jimz011apps.hki7.data.withElement
import com.jimz011apps.hki7.data.withString
import com.jimz011apps.hki7.data.without
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private enum class FlowsView { LIST, RECIPES, EDITOR }

private data class EditingBlock(
    val section: AutomationSection,
    /** Parent group path; empty for a block in the section's own list. */
    val path: List<Int> = emptyList(),
    val index: Int,
    val block: JsonObject,
    val openEntityPickerInitially: Boolean = false,
    val openActionPickerInitially: Boolean = false
)

private data class AutomationRunMode(
    val key: String,
    val title: String,
    val description: String
)

@Composable
private fun automationRunModes() = listOf(
    AutomationRunMode(
        "single",
        stringResource(R.string.uif_flow_mode_single),
        stringResource(R.string.uif_flow_mode_single_description),
    ),
    AutomationRunMode(
        "restart",
        stringResource(R.string.uif_flow_mode_restart),
        stringResource(R.string.uif_flow_mode_restart_description),
    ),
    AutomationRunMode(
        "queued",
        stringResource(R.string.uif_flow_mode_queued),
        stringResource(R.string.uif_flow_mode_queued_description),
    ),
    AutomationRunMode(
        "parallel",
        stringResource(R.string.uif_flow_mode_parallel),
        stringResource(R.string.uif_flow_mode_parallel_description),
    ),
)

@Composable
private fun localizedAutomationSection(section: AutomationSection): String = when (section) {
    AutomationSection.TRIGGER -> stringResource(R.string.uif_flow_section_when)
    AutomationSection.CONDITION -> stringResource(R.string.uif_flow_section_and_if)
    AutomationSection.ACTION -> stringResource(R.string.uif_flow_section_then)
}

@Composable
private fun localizedAutomationBlockKind(section: AutomationSection, kind: String?): String =
    when (section to kind) {
        AutomationSection.TRIGGER to "state",
        AutomationSection.CONDITION to "state" -> stringResource(R.string.uif_flow_entity_state)
        AutomationSection.TRIGGER to "time" -> stringResource(R.string.uif_time)
        AutomationSection.TRIGGER to "sun" -> stringResource(R.string.uif_flow_sunrise_or_sunset)
        AutomationSection.CONDITION to "time" -> stringResource(R.string.uif_flow_time_window)
        AutomationSection.CONDITION to "or" -> stringResource(R.string.uif_flow_group_any)
        AutomationSection.CONDITION to "and" -> stringResource(R.string.uif_flow_group_all)
        AutomationSection.CONDITION to "not" -> stringResource(R.string.uif_flow_group_none)
        AutomationSection.ACTION to "action" -> stringResource(R.string.uif_flow_ha_action)
        else -> stringResource(R.string.uif_advanced)
    }

@Composable
private fun localizedAutomationBlockSummary(section: AutomationSection, block: JsonObject): String {
    fun text(key: String) = block.stringValue(key).takeIf(String::isNotBlank)
    return when (section to automationBlockKind(section, block)) {
        AutomationSection.TRIGGER to "state" -> {
            val entity = text("entity_id") ?: stringResource(R.string.uif_choose_entity)
            text("to")?.let { stringResource(R.string.uif_flow_entity_becomes_state, entity, it) } ?: entity
        }
        AutomationSection.TRIGGER to "time" ->
            stringResource(R.string.uif_flow_at_time, text("at") ?: stringResource(R.string.uif_a_time))
        AutomationSection.TRIGGER to "sun" -> when (text("event")) {
            "sunrise" -> stringResource(R.string.uif_sunrise)
            "sunset" -> stringResource(R.string.uif_sunset)
            else -> stringResource(R.string.uif_flow_sun_event)
        }
        AutomationSection.CONDITION to "state" -> stringResource(
            R.string.uif_flow_entity_is_state,
            text("entity_id") ?: stringResource(R.string.uif_choose_entity),
            text("state") ?: stringResource(R.string.uif_a_state),
        )
        AutomationSection.CONDITION to "time" -> {
            val parts = listOfNotNull(
                text("after")?.let { stringResource(R.string.uif_flow_after_time, it) },
                text("before")?.let { stringResource(R.string.uif_flow_before_time, it) },
            )
            if (parts.isEmpty()) {
                stringResource(R.string.uif_flow_within_time_window)
            } else {
                parts.joinToString(stringResource(R.string.uif_and_separator))
            }
        }
        AutomationSection.ACTION to "action" -> {
            val action = text("action") ?: text("service") ?: stringResource(R.string.uif_choose_action)
            val target = (block["target"] as? JsonObject)?.stringValue("entity_id")
                ?.takeIf(String::isNotBlank)
                ?: text("entity_id")
            target?.let { "$action → $it" } ?: action
        }
        AutomationSection.CONDITION to "or",
        AutomationSection.CONDITION to "and",
        AutomationSection.CONDITION to "not" -> {
            val count = automationGroupChildren(section, block)?.size ?: 0
            if (count == 0) {
                stringResource(R.string.uif_flow_group_empty)
            } else {
                pluralStringResource(R.plurals.uif_flow_group_count, count, count)
            }
        }
        else -> stringResource(R.string.uif_flow_advanced_block_unchanged)
    }
}

@Composable
private fun localizedRecipeTitle(recipe: AutomationRecipe): String = stringResource(
    when (recipe) {
        AutomationRecipe.BLANK -> R.string.uif_recipe_blank_title
        AutomationRecipe.ENTITY_STATE -> R.string.uif_recipe_entity_state_title
        AutomationRecipe.SCHEDULE -> R.string.uif_recipe_schedule_title
        AutomationRecipe.SUNSET -> R.string.uif_recipe_sunset_title
        AutomationRecipe.MOTION_LIGHTS -> R.string.uif_recipe_motion_lights_title
        AutomationRecipe.SUNRISE_LIGHTS_OFF -> R.string.uif_recipe_sunrise_lights_off_title
        AutomationRecipe.ARRIVE_HOME -> R.string.uif_recipe_arrive_home_title
        AutomationRecipe.LEAVE_HOME -> R.string.uif_recipe_leave_home_title
        AutomationRecipe.BEDTIME -> R.string.uif_recipe_bedtime_title
        AutomationRecipe.MORNING_SCENE -> R.string.uif_recipe_morning_scene_title
        AutomationRecipe.LOCK_AT_NIGHT -> R.string.uif_recipe_lock_at_night_title
        AutomationRecipe.OPEN_COVERS_AT_SUNRISE -> R.string.uif_recipe_open_covers_title
        AutomationRecipe.CLOSE_COVERS_AT_SUNSET -> R.string.uif_recipe_close_covers_title
    }
)

@Composable
private fun localizedRecipeDescription(recipe: AutomationRecipe): String = stringResource(
    when (recipe) {
        AutomationRecipe.BLANK -> R.string.uif_recipe_blank_description
        AutomationRecipe.ENTITY_STATE -> R.string.uif_recipe_entity_state_description
        AutomationRecipe.SCHEDULE -> R.string.uif_recipe_schedule_description
        AutomationRecipe.SUNSET -> R.string.uif_recipe_sunset_description
        AutomationRecipe.MOTION_LIGHTS -> R.string.uif_recipe_motion_lights_description
        AutomationRecipe.SUNRISE_LIGHTS_OFF -> R.string.uif_recipe_sunrise_lights_off_description
        AutomationRecipe.ARRIVE_HOME -> R.string.uif_recipe_arrive_home_description
        AutomationRecipe.LEAVE_HOME -> R.string.uif_recipe_leave_home_description
        AutomationRecipe.BEDTIME -> R.string.uif_recipe_bedtime_description
        AutomationRecipe.MORNING_SCENE -> R.string.uif_recipe_morning_scene_description
        AutomationRecipe.LOCK_AT_NIGHT -> R.string.uif_recipe_lock_at_night_description
        AutomationRecipe.OPEN_COVERS_AT_SUNRISE -> R.string.uif_recipe_open_covers_description
        AutomationRecipe.CLOSE_COVERS_AT_SUNSET -> R.string.uif_recipe_close_covers_description
    }
)

/** Native Home Assistant automation manager. No automation is cached or stored by HKI7. */
@Composable
fun FlowsDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val automationFlow = remember(viewModel) {
        viewModel.entitiesMatching("flows:automation") { it.entity_id.startsWith("automation.") }
    }
    val liveAutomations by automationFlow.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val automations = remember(liveAutomations, entityRegistry) {
        automationsIncludingRegistry(liveAutomations, entityRegistry)
    }
    val allEntities by viewModel.entities.collectAsState()
    var currentView by remember { mutableStateOf(FlowsView.LIST) }
    var editorParentView by remember { mutableStateOf(FlowsView.LIST) }
    var document by remember { mutableStateOf<HAAutomationDocument?>(null) }
    var draft by remember { mutableStateOf<JsonObject?>(null) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var loadingEntityId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var addSection by remember { mutableStateOf<AutomationSection?>(null) }
    var addPath by remember { mutableStateOf<List<Int>>(emptyList()) }
    var conditionPath by remember { mutableStateOf<List<Int>>(emptyList()) }
    var editingBlock by remember { mutableStateOf<EditingBlock?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionDefinitions by remember { mutableStateOf<List<HAActionDefinition>>(emptyList()) }
    var actionLoadError by remember { mutableStateOf<String?>(null) }
    val actionsLoadFailed = stringResource(R.string.uif_flow_actions_load_failed)
    val automationLoadFailed = stringResource(R.string.uif_flow_automation_load_failed)
    val automationSaveFailed = stringResource(R.string.uif_flow_automation_save_failed)
    val automationDeleteFailed = stringResource(R.string.uif_flow_automation_delete_failed)
    val automationSaved = stringResource(R.string.uif_flow_automation_saved)
    val automationDeleted = stringResource(R.string.uif_flow_automation_deleted)

    LaunchedEffect(viewModel) {
        viewModel.fetchRegistries()
        runCatching { viewModel.getAutomationActions() }
            .onSuccess { actionDefinitions = it; actionLoadError = null }
            .onFailure { actionLoadError = it.message ?: actionsLoadFailed }
    }

    fun closeEditor(listMessage: String? = null) {
        currentView = FlowsView.LIST
        document = null
        draft = null
        message = listMessage
    }

    fun openRecipe(recipe: AutomationRecipe) {
        val id = System.currentTimeMillis().toString()
        val config = newAutomationConfig(recipe)
        document = HAAutomationDocument(id = id, entityId = null, config = config, editable = true)
        draft = config
        message = null
        editorParentView = FlowsView.RECIPES
        currentView = FlowsView.EDITOR
    }

    fun leaveEditor() {
        val destination = editorParentView
        document = null
        draft = null
        message = null
        currentView = destination
    }

    val title = when (currentView) {
        FlowsView.LIST -> stringResource(R.string.dlg_flows)
        FlowsView.RECIPES -> stringResource(R.string.dlg_create_a_flow)
        FlowsView.EDITOR -> draft?.stringValue("alias")?.ifBlank { stringResource(R.string.dlg_automation) } ?: stringResource(R.string.dlg_automation)
    }
    val subtitle = when (currentView) {
        FlowsView.LIST -> stringResource(R.string.dlg_native_home_assistant_automations_always_in_sync)
        FlowsView.RECIPES -> stringResource(R.string.dlg_start_with_a_useful_recipe_then_adjust_each_step)
        FlowsView.EDITOR -> if (document?.editable == true) stringResource(R.string.dlg_when_and_if_then) else stringResource(R.string.dlg_read_only_home_assistant_automation)
    }

    ModernSettingsDialogFrame(
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.AccountTree,
        onDismiss = onDismiss,
        onBack = when (currentView) {
            FlowsView.LIST -> null
            FlowsView.RECIPES -> ({ currentView = FlowsView.LIST })
            FlowsView.EDITOR -> ({ leaveEditor() })
        },
        content = {
            when (currentView) {
                FlowsView.LIST -> FlowsList(
                    automations = automations,
                    query = query,
                    onQueryChange = { query = it },
                    loadingEntityId = loadingEntityId,
                    message = message,
                    onRefresh = {
                        viewModel.fetchRegistries(force = true)
                        viewModel.refreshEntities(isSilent = true, includeDashboardRefresh = false)
                    },
                    onEnabledChange = viewModel::setAutomationEnabled,
                    onRun = viewModel::runAutomation,
                    onEdit = { entity ->
                        loadingEntityId = entity.entity_id
                        message = null
                        scope.launch {
                            runCatching { viewModel.loadAutomation(entity.entity_id) }
                                .onSuccess {
                                    document = it
                                    draft = it.config
                                    editorParentView = FlowsView.LIST
                                    currentView = FlowsView.EDITOR
                                }
                    .onFailure { message = it.message ?: automationLoadFailed }
                            loadingEntityId = null
                        }
                    }
                )
                FlowsView.RECIPES -> RecipeList(onSelected = ::openRecipe)
                FlowsView.EDITOR -> draft?.let { config ->
                    FlowEditor(
                        config = config,
                        editable = document?.editable == true,
                        message = message,
                        onConfigChange = { draft = it; message = null },
                        onAdd = { section, path ->
                            if (section == AutomationSection.ACTION) {
                                val block = newAutomationBlock(section, "action")
                                val index = automationItems(config, section).size
                                editingBlock = EditingBlock(
                                    section = section,
                                    index = index,
                                    block = block,
                                    openActionPickerInitially = true
                                )
                            } else {
                                addSection = section
                                addPath = path
                            }
                        },
                        onEdit = { section, path, index, block ->
                            editingBlock = EditingBlock(section, path, index, block)
                        },
                        onRemove = { section, path, index ->
                            val all = automationItems(config, section)
                            val siblings = automationBlocksAtPath(all, path)
                                .filterIndexed { itemIndex, _ -> itemIndex != index }
                            draft = withAutomationItems(
                                config,
                                section,
                                withAutomationBlocksAtPath(all, path, siblings)
                            )
                        },
                        conditionPath = conditionPath,
                        onConditionPathChange = { conditionPath = it }
                    )
                }
            }
        },
        footer = {
            when (currentView) {
                FlowsView.LIST -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_close)) }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { currentView = FlowsView.RECIPES }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dlg_new_flow))
                    }
                }
                FlowsView.RECIPES -> {
                    TextButton(onClick = { currentView = FlowsView.LIST }) { Text(stringResource(R.string.dlg_cancel)) }
                }
                FlowsView.EDITOR -> {
                    if (document?.editable == true && document?.entityId != null) {
                        TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dlg_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (document?.editable == true) {
                        Button(
                            enabled = !busy && !draft?.stringValue("alias").isNullOrBlank(),
                            onClick = {
                                val id = document?.id ?: return@Button
                                val config = draft ?: return@Button
                                busy = true
                                message = null
                                scope.launch {
                                    runCatching { viewModel.saveAutomation(id, config) }
                                        .onSuccess { errors ->
                            if (errors.isEmpty()) closeEditor(automationSaved)
                                            else message = errors.joinToString("\n")
                                        }
                        .onFailure { message = it.message ?: automationSaveFailed }
                                    busy = false
                                }
                            }
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.dlg_save_in_home_assistant))
                        }
                    } else {
                        Button(onClick = { closeEditor() }) { Text(stringResource(R.string.dlg_done)) }
                    }
                }
            }
        }
    )

    // Removing a group while inside it, or loading a different flow, can leave the breadcrumb
    // pointing at something that no longer exists.
    LaunchedEffect(draft, conditionPath) {
        val config = draft
        if (config != null && conditionPath.isNotEmpty() &&
            !automationPathExists(automationItems(config, AutomationSection.CONDITION), conditionPath)
        ) {
            conditionPath = conditionPath.dropLast(1)
        }
    }

    addSection?.let { section ->
        AddAutomationBlockDialog(
            section = section,
            onDismiss = { addSection = null },
            onSelected = { kind ->
                val config = draft ?: return@AddAutomationBlockDialog
                val block = newAutomationBlock(section, kind)
                val all = automationItems(config, section)
                val siblings = automationBlocksAtPath(all, addPath)
                addSection = null
                if (kind in AUTOMATION_GROUP_KINDS) {
                    // A group has nothing to fill in, so it is added straight away and opened,
                    // ready for the conditions that go inside it.
                    draft = withAutomationItems(
                        config,
                        section,
                        withAutomationBlocksAtPath(all, addPath, siblings + block)
                    )
                    conditionPath = addPath + siblings.size
                } else {
                    editingBlock = EditingBlock(
                        section = section,
                        path = addPath,
                        index = siblings.size,
                        block = block,
                        openEntityPickerInitially = kind == "state"
                    )
                }
            }
        )
    }

    editingBlock?.let { editing ->
        AutomationBlockEditorDialog(
            section = editing.section,
            block = editing.block,
            allEntities = allEntities,
            actionDefinitions = actionDefinitions,
            actionLoadError = actionLoadError,
            openEntityPickerInitially = editing.openEntityPickerInitially,
            openActionPickerInitially = editing.openActionPickerInitially,
            onDismiss = { editingBlock = null },
            onSave = { updatedBlock ->
                val config = draft ?: return@AutomationBlockEditorDialog
                val all = automationItems(config, editing.section)
                val siblings = automationBlocksAtPath(all, editing.path).toMutableList()
                if (editing.index in siblings.indices) siblings[editing.index] = updatedBlock
                else siblings.add(updatedBlock)
                draft = withAutomationItems(
                    config,
                    editing.section,
                    withAutomationBlocksAtPath(all, editing.path, siblings)
                )
                editingBlock = null
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text(stringResource(R.string.dlg_delete_automation)) },
            text = { Text(stringResource(R.string.dlg_this_removes_the_native_automation_from_home_assistant_thi)) },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text(stringResource(R.string.dlg_cancel)) } },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val id = document?.id ?: return@TextButton
                        busy = true
                        scope.launch {
                            runCatching { viewModel.deleteAutomation(id) }
                        .onSuccess { confirmDelete = false; closeEditor(automationDeleted) }
                        .onFailure { message = it.message ?: automationDeleteFailed; confirmDelete = false }
                            busy = false
                        }
                    }
                ) { Text(stringResource(R.string.dlg_delete), color = MaterialTheme.colorScheme.error) }
            }
        )
    }
}

@Composable
private fun FlowsList(
    automations: List<HAEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    loadingEntityId: String?,
    message: String?,
    onRefresh: () -> Unit,
    onEnabledChange: (String, Boolean) -> Unit,
    onRun: (String) -> Unit,
    onEdit: (HAEntity) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val filtered = remember(automations, query) {
        automations.filter {
            query.isBlank() || it.entity_id.contains(query, true) || it.friendlyName.orEmpty().contains(query, true)
        }.sortedBy { it.friendlyName ?: it.entity_id }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(stringResource(R.string.dlg_search_automations)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, stringResource(R.string.uif_refresh))
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (automations.isEmpty()) stringResource(R.string.dlg_no_native_home_assistant_automations_found) else stringResource(R.string.dlg_no_matching_automations),
                    color = appColors.onMuted
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.entity_id }) { entity ->
                    FlowAutomationRow(
                        entity = entity,
                        loading = loadingEntityId == entity.entity_id,
                        onEnabledChange = { onEnabledChange(entity.entity_id, it) },
                        onRun = { onRun(entity.entity_id) },
                        onEdit = { onEdit(entity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowAutomationRow(
    entity: HAEntity,
    loading: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val lastTriggered = entity.attributes?.get("last_triggered")?.jsonPrimitive?.contentOrNull
        ?.replace('T', ' ')?.substringBefore('.')
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !loading, onClick = onEdit),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entity.friendlyName ?: entity.entity_id,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (entity.state == "unavailable") stringResource(R.string.dlg_not_loaded_by_home_assistant)
                    else lastTriggered?.let { stringResource(R.string.dlg_last_run, it) } ?: stringResource(R.string.dlg_never_triggered),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onRun, enabled = !loading && entity.state != "unavailable") {
                Icon(Icons.Default.PlayArrow, stringResource(R.string.uif_run))
            }
            Switch(
                checked = entity.state == "on",
                onCheckedChange = onEnabledChange,
                enabled = !loading && entity.state != "unavailable"
            )
        }
    }
}

@Composable
private fun RecipeList(onSelected: (AutomationRecipe) -> Unit) {
    val recipes = listOf(
        AutomationRecipe.BLANK to Icons.Default.Add,
        AutomationRecipe.ENTITY_STATE to Icons.Default.Bolt,
        AutomationRecipe.SCHEDULE to Icons.Default.Schedule,
        AutomationRecipe.SUNSET to Icons.Default.WbTwilight,
        AutomationRecipe.MOTION_LIGHTS to Icons.Default.Lightbulb,
        AutomationRecipe.SUNRISE_LIGHTS_OFF to Icons.Default.WbTwilight,
        AutomationRecipe.ARRIVE_HOME to Icons.Default.Bolt,
        AutomationRecipe.LEAVE_HOME to Icons.Default.Bolt,
        AutomationRecipe.BEDTIME to Icons.Default.Schedule,
        AutomationRecipe.MORNING_SCENE to Icons.Default.Schedule,
        AutomationRecipe.LOCK_AT_NIGHT to Icons.Default.Schedule,
        AutomationRecipe.OPEN_COVERS_AT_SUNRISE to Icons.Default.WbTwilight,
        AutomationRecipe.CLOSE_COVERS_AT_SUNSET to Icons.Default.WbTwilight
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                stringResource(R.string.dlg_recipes_create_regular_home_assistant_automations_you_can),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(recipes, key = { it.first.name }) { (recipe, icon) ->
            RecipeCard(recipe, icon) { onSelected(recipe) }
        }
    }
}

@Composable
private fun RecipeCard(recipe: AutomationRecipe, icon: ImageVector, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    localizedRecipeTitle(recipe),
                    color = appColors.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    localizedRecipeDescription(recipe),
                    color = appColors.onSurface.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FlowEditor(
    config: JsonObject,
    editable: Boolean,
    message: String?,
    onConfigChange: (JsonObject) -> Unit,
    onAdd: (AutomationSection, List<Int>) -> Unit,
    onEdit: (AutomationSection, List<Int>, Int, JsonObject) -> Unit,
    onRemove: (AutomationSection, List<Int>, Int) -> Unit,
    conditionPath: List<Int>,
    onConditionPathChange: (List<Int>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    fun pathFor(section: AutomationSection) =
        if (section == AutomationSection.CONDITION) conditionPath else emptyList()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!editable) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        stringResource(R.string.dlg_this_automation_is_managed_outside_home_assistant_s_ui),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = config.stringValue("alias"),
                onValueChange = { onConfigChange(withAutomationText(config, "alias", it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.dlg_name)) },
                enabled = editable,
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = config.stringValue("description"),
                onValueChange = { onConfigChange(withAutomationText(config, "description", it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.dlg_description)) },
                enabled = editable,
                minLines = 2
            )
        }
        message?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        AutomationSection.entries.forEach { section ->
            item {
                FlowSectionHeader(
                    section = section,
                    editable = editable,
                    onAdd = { onAdd(section, pathFor(section)) }
                )
            }
            val allBlocks = automationItems(config, section)
            // Conditions can nest; every other section is flat and stays at the root.
            val path = pathFor(section)
            val blocks = if (section == AutomationSection.CONDITION) {
                automationBlocksAtPath(allBlocks, path)
            } else {
                allBlocks
            }
            if (section == AutomationSection.CONDITION && path.isNotEmpty()) {
                item {
                    FlowGroupBreadcrumb(
                        allBlocks = allBlocks,
                        path = path,
                        onNavigate = onConditionPathChange
                    )
                }
            }
            val shorthandCount = automationElements(config, section).size - blocks.size
            if (shorthandCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface)
                    ) {
                        Text(
                            pluralStringResource(
                                R.plurals.uif_flow_advanced_block_count,
                                shorthandCount,
                                shorthandCount,
                            ),
                            modifier = Modifier.padding(14.dp),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (blocks.isEmpty() && shorthandCount == 0) {
                item {
                    Text(
                        if (section == AutomationSection.CONDITION) stringResource(R.string.dlg_no_conditions_this_flow_always_continues) else stringResource(R.string.dlg_add_at_least_one_step),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            } else {
                itemsIndexed(blocks, key = { index, _ -> "${section.name}-${path.joinToString("-")}-$index" }) { index, block ->
                    val children = automationGroupChildren(section, block)
                    FlowBlockCard(
                        section = section,
                        block = block,
                        editable = editable,
                        // A group has no fields of its own to edit — opening it shows what it holds.
                        childCount = children?.size,
                        onEdit = {
                            if (children != null) onConditionPathChange(path + index)
                            else onEdit(section, path, index, block)
                        },
                        onRemove = { onRemove(section, path, index) }
                    )
                }
            }
        }
        item {
            val current = config.stringValue("mode").ifBlank { "single" }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.dlg_run_mode), fontWeight = FontWeight.SemiBold)
                automationRunModes().forEach { mode ->
                    val selected = current == mode.key
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = editable) {
                                onConfigChange(config.withString("mode", mode.key))
                            },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else appColors.subtleSurface
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = if (selected) stringResource(R.string.dlg_selected, mode.title) else mode.title,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.description,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                else appColors.onMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shows where you are inside nested condition groups and lets you climb back out. Each crumb is
 * the group's own label, so "And if › Any of › All of" reads as the shape of the logic.
 */
@Composable
private fun FlowGroupBreadcrumb(
    allBlocks: List<JsonObject>,
    path: List<Int>,
    onNavigate: (List<Int>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val crumbs = buildList {
        var current = allBlocks
        path.forEachIndexed { depth, index ->
            val block = current.getOrNull(index) ?: return@forEachIndexed
            add(path.take(depth + 1) to block)
            current = automationGroupChildren(AutomationSection.CONDITION, block).orEmpty()
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = { onNavigate(path.dropLast(1)) }, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.uif_flow_group_back),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            localizedAutomationSection(AutomationSection.CONDITION),
            style = MaterialTheme.typography.labelMedium,
            color = appColors.onMuted,
            modifier = Modifier.clickable { onNavigate(emptyList()) }
        )
        crumbs.forEach { (crumbPath, block) ->
            Text(" › ", style = MaterialTheme.typography.labelMedium, color = appColors.onMuted)
            val isCurrent = crumbPath.size == path.size
            Text(
                localizedAutomationBlockKind(AutomationSection.CONDITION, automationBlockKind(AutomationSection.CONDITION, block)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else appColors.onMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onNavigate(crumbPath) }
            )
        }
    }
}

@Composable
private fun FlowSectionHeader(section: AutomationSection, editable: Boolean, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(localizedAutomationSection(section).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        if (editable) FilledTonalButton(onClick = onAdd) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.dlg_add))
        }
    }
}

@Composable
private fun FlowBlockCard(
    section: AutomationSection,
    block: JsonObject,
    editable: Boolean,
    /** Non-null when this block is an and/or/not group, so the card can offer to open it. */
    childCount: Int? = null,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val supported = isSupportedAutomationBlock(section, block)
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = childCount != null || (editable && supported), onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    localizedAutomationBlockKind(section, automationBlockKind(section, block)),
                    fontWeight = FontWeight.SemiBold
                )
                Text(localizedAutomationBlockSummary(section, block), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (childCount != null) {
                // A group holds conditions rather than fields, so opening it descends into it.
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    stringResource(R.string.uif_flow_group_open),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (editable && supported) {
                Icon(Icons.Default.Edit, stringResource(R.string.uif_edit), tint = appColors.onMuted)
            }
            if (editable) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, stringResource(R.string.uif_remove))
                }
            }
        }
    }
}

@Composable
private fun AddAutomationBlockDialog(
    section: AutomationSection,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val choices = when (section) {
        AutomationSection.TRIGGER -> listOf(
            "state" to stringResource(R.string.uif_flow_entity_state),
            "time" to stringResource(R.string.uif_time),
            "sun" to stringResource(R.string.uif_flow_sunrise_or_sunset),
        )
        AutomationSection.CONDITION -> listOf(
            "state" to stringResource(R.string.uif_flow_entity_state),
            "time" to stringResource(R.string.uif_flow_time_window),
            // Groups can hold other groups, which is how Home Assistant expresses
            // "A and (B or C)" — and how its own editor writes it.
            "or" to stringResource(R.string.uif_flow_group_any),
            "and" to stringResource(R.string.uif_flow_group_all),
            "not" to stringResource(R.string.uif_flow_group_none),
        )
        AutomationSection.ACTION -> listOf(
            "action" to stringResource(R.string.uif_flow_perform_ha_action),
        )
    }
    ModernSettingsDialogFrame(
        title = stringResource(R.string.dlg_add_to, localizedAutomationSection(section)),
        subtitle = stringResource(R.string.dlg_choose_a_visual_home_assistant_block),
        icon = Icons.Default.Add,
        onDismiss = onDismiss,
        footer = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { (kind, label) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelected(kind) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(label, Modifier.padding(18.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AutomationBlockEditorDialog(
    section: AutomationSection,
    block: JsonObject,
    allEntities: List<HAEntity>,
    actionDefinitions: List<HAActionDefinition>,
    actionLoadError: String?,
    openEntityPickerInitially: Boolean,
    openActionPickerInitially: Boolean,
    onDismiss: () -> Unit,
    onSave: (JsonObject) -> Unit
) {
    var working by remember(block) { mutableStateOf(block) }
    var showEntityPicker by remember(block, openEntityPickerInitially) {
        mutableStateOf(openEntityPickerInitially)
    }
    var showActionPicker by remember(block, openActionPickerInitially) {
        mutableStateOf(openActionPickerInitially)
    }
    val kind = automationBlockKind(section, working).orEmpty()
    val title = stringResource(
        R.string.dlg_labeled_value,
        localizedAutomationSection(section),
        localizedAutomationBlockKind(section, kind),
    )
    fun updateText(key: String, value: String) { working = working.withString(key, value) }
    val directEntityId = working.stringValue("entity_id")
    val targetEntityId = (working["target"] as? JsonObject)?.stringValue("entity_id")
        ?: directEntityId
    val selectedActionKey = working.stringValue("action").ifBlank { working.stringValue("service") }
    val selectedActionDefinition = actionDefinitions.firstOrNull { it.key == selectedActionKey }
    val actionData = working["data"] as? JsonObject
    val selectedEntity = allEntities.firstOrNull {
        it.entity_id == if (section == AutomationSection.ACTION) targetEntityId else directEntityId
    }
    val stateOptions = suggestedAutomationStates(selectedEntity)
    val canApply = when (section to kind) {
        AutomationSection.TRIGGER to "state" -> directEntityId.isNotBlank()
        AutomationSection.TRIGGER to "time" -> working.stringValue("at").isNotBlank()
        AutomationSection.TRIGGER to "sun" -> working.stringValue("event").isNotBlank()
        AutomationSection.CONDITION to "state" -> directEntityId.isNotBlank() && working.stringValue("state").isNotBlank()
        AutomationSection.CONDITION to "time" -> working.stringValue("after").isNotBlank() || working.stringValue("before").isNotBlank()
        AutomationSection.ACTION to "action" -> selectedActionKey.isNotBlank() &&
            selectedActionDefinition?.fields.orEmpty()
                .filter { it.required }
                .all { actionData?.containsKey(it.key) == true }
        else -> false
    }

    ModernSettingsDialogFrame(
        title = title,
        subtitle = stringResource(R.string.dlg_saved_directly_in_the_native_automation),
        icon = Icons.Default.AccountTree,
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(working) }, enabled = canApply) { Text(stringResource(R.string.dlg_apply)) }
        }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (section to kind) {
                AutomationSection.TRIGGER to "state" -> {
                    EntityField(working.stringValue("entity_id")) { showEntityPicker = true }
                    StateSelectorField(
                        label = stringResource(R.string.dlg_from_state),
                        selected = working.stringValue("from"),
                        options = stateOptions,
                        allowAny = true,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("from", it) }
                    )
                    StateSelectorField(
                        label = stringResource(R.string.dlg_to_state),
                        selected = working.stringValue("to"),
                        options = stateOptions,
                        allowAny = true,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("to", it) }
                    )
                }
                AutomationSection.TRIGGER to "time" ->
                    FlowTextField(stringResource(R.string.uif_flow_time_format), working.stringValue("at")) { updateText("at", it) }
                AutomationSection.TRIGGER to "sun" -> {
                    Text(stringResource(R.string.dlg_event), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("sunrise", "sunset").forEach { event ->
                            val eventLabel = if (event == "sunrise") {
                                stringResource(R.string.uif_sunrise)
                            } else {
                                stringResource(R.string.uif_sunset)
                            }
                            FilledTonalButton(onClick = { updateText("event", event) }) {
                                Text(
                                    if (working.stringValue("event") == event) {
                                        stringResource(R.string.dlg_selected_value, eventLabel)
                                    } else {
                                        eventLabel
                                    }
                                )
                            }
                        }
                    }
                }
                AutomationSection.CONDITION to "state" -> {
                    EntityField(working.stringValue("entity_id")) { showEntityPicker = true }
                    StateSelectorField(
                        label = stringResource(R.string.dlg_required_state),
                        selected = working.stringValue("state"),
                        options = stateOptions,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("state", it) }
                    )
                }
                AutomationSection.CONDITION to "time" -> {
                    FlowTextField(stringResource(R.string.uif_flow_after_format), working.stringValue("after")) { updateText("after", it) }
                    FlowTextField(stringResource(R.string.uif_flow_before_format), working.stringValue("before")) { updateText("before", it) }
                }
                AutomationSection.ACTION to "action" -> {
                    ActionSelectorField(
                        selected = selectedActionKey,
                        enabled = actionDefinitions.isNotEmpty(),
                        onClick = { showActionPicker = true }
                    )
                    actionLoadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedActionKey.isNotBlank()) {
                        if (selectedActionDefinition?.supportsTarget == true || targetEntityId.isNotBlank()) {
                            Text(stringResource(R.string.dlg_target_entity_optional), style = MaterialTheme.typography.labelMedium)
                            EntityField(targetEntityId) { showEntityPicker = true }
                            if (targetEntityId.isNotBlank()) {
                                TextButton(
                                    onClick = { working = working.without("target", "entity_id") }
                                ) { Text(stringResource(R.string.dlg_no_target)) }
                            }
                        } else if (selectedActionDefinition != null) {
                            Text(
                                stringResource(R.string.dlg_this_action_does_not_use_an_entity_target),
                                color = LocalHKIAppColors.current.onMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HomeAssistantActionDataEditor(
                            definition = selectedActionDefinition,
                            data = actionData,
                            onDataChange = { data ->
                                working = if (data == null) working.without("data")
                                else working.withElement("data", data)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            title = if (section == AutomationSection.ACTION) {
                stringResource(R.string.uif_choose_target)
            } else {
                stringResource(R.string.uif_choose_entity)
            },
            singleSelect = true,
            preselectedIds = setOfNotNull(
                (working["target"] as? JsonObject)?.stringValue("entity_id")?.takeIf { it.isNotBlank() }
                    ?: working.stringValue("entity_id").takeIf { it.isNotBlank() }
            ),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids ->
                val entityId = ids.firstOrNull().orEmpty()
                working = if (section == AutomationSection.ACTION) {
                    working.without("service", "entity_id")
                        .withElement("target", buildJsonObject { put("entity_id", entityId) })
                } else {
                    val entity = allEntities.firstOrNull { it.entity_id == entityId }
                    val options = suggestedAutomationStates(entity)
                    val entityChanged = working.stringValue("entity_id") != entityId
                    var updated = working.withString("entity_id", entityId)
                    if (
                        section == AutomationSection.TRIGGER &&
                        (updated.stringValue("to").isBlank() || (entityChanged && updated.stringValue("to") !in options))
                    ) {
                        if (entityChanged) updated = updated.withString("from", "")
                        updated = updated.withString("to", options.firstOrNull().orEmpty())
                    }
                    if (
                        section == AutomationSection.CONDITION &&
                        (updated.stringValue("state").isBlank() || (entityChanged && updated.stringValue("state") !in options))
                    ) {
                        updated = updated.withString("state", options.firstOrNull().orEmpty())
                    }
                    updated
                }
                showEntityPicker = false
            }
        )
    }

    if (showActionPicker) {
        HomeAssistantActionPickerDialog(
            actions = actionDefinitions,
            selected = working.stringValue("action").ifBlank { working.stringValue("service") },
            preferredDomain = targetEntityId.substringBefore('.').takeIf { targetEntityId.contains('.') },
            onDismiss = { showActionPicker = false },
            onSelected = { action ->
                val actionChanged = selectedActionKey != action.key
                var updated = working.without("service").withString("action", action.key)
                if (actionChanged) updated = updated.without("data", "response_variable")
                if (!action.supportsTarget) updated = updated.without("target", "entity_id")
                working = updated
                showActionPicker = false
            }
        )
    }
}

@Composable
private fun FlowTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
internal fun StateSelectorField(
    label: String,
    selected: String,
    options: List<String>,
    enabled: Boolean,
    allowAny: Boolean = false,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var customMode by remember(label) { mutableStateOf(false) }
    val choices = remember(options, selected) {
        (options + selected.takeIf { it.isNotBlank() }).filterNotNull().distinct()
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        selected.isNotBlank() -> selected.replace('_', ' ')
                        allowAny -> stringResource(R.string.dlg_any_state)
                        else -> stringResource(R.string.dlg_choose_a_state)
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowAny) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dlg_any_state)) },
                        onClick = { customMode = false; onSelected(""); expanded = false }
                    )
                }
                choices.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state.replace('_', ' ')) },
                        onClick = { customMode = false; onSelected(state); expanded = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dlg_custom_state)) },
                    onClick = { customMode = true; expanded = false }
                )
            }
        }
        if (customMode) {
            OutlinedTextField(
                value = selected,
                onValueChange = onSelected,
                label = { Text(stringResource(R.string.dlg_custom_value, label.lowercase())) },
                supportingText = { Text(stringResource(R.string.dlg_exact_text_or_number_reported_by_home_assistant)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (!enabled) {
            Text(stringResource(R.string.dlg_choose_an_entity_first), color = LocalHKIAppColors.current.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ActionSelectorField(selected: String, enabled: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.dlg_action), style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Bolt, null)
            Spacer(Modifier.width(8.dp))
            Text(
                selected.ifBlank { if (enabled) stringResource(R.string.dlg_choose_an_action) else stringResource(R.string.dlg_loading_actions) },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}

@Composable
internal fun HomeAssistantActionPickerDialog(
    actions: List<HAActionDefinition>,
    selected: String,
    preferredDomain: String?,
    onDismiss: () -> Unit,
    onSelected: (HAActionDefinition) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(actions, query, preferredDomain) {
        actions.asSequence()
            .filter {
                query.isBlank() || it.key.contains(query, true) || it.name.contains(query, true) ||
                    it.description.contains(query, true)
            }
            .sortedWith(
                compareByDescending<HAActionDefinition> { it.key.substringBefore('.') == preferredDomain }
                    .thenBy { it.key.substringBefore('.') }
                    .thenBy { it.name }
            )
            .toList()
    }
    ModernSettingsDialogFrame(
        title = stringResource(R.string.dlg_choose_action),
        subtitle = preferredDomain?.let {
            stringResource(R.string.uif_flow_domain_actions_first, it)
        } ?: stringResource(R.string.uif_flow_actions_available),
        icon = Icons.Default.Bolt,
        onDismiss = onDismiss,
        footer = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) } }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(stringResource(R.string.dlg_search_actions)) },
                singleLine = true
            )
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.key }) { action ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(action) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (action.key == selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else LocalHKIAppColors.current.subtleSurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(action.name, fontWeight = FontWeight.SemiBold)
                            Text(action.key, color = LocalHKIAppColors.current.onMuted, style = MaterialTheme.typography.bodySmall)
                            if (action.description.isNotBlank()) {
                                Text(
                                    action.description,
                                    color = LocalHKIAppColors.current.onMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityField(entityId: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Lightbulb, null)
        Spacer(Modifier.width(8.dp))
        Text(entityId.ifBlank { stringResource(R.string.dlg_choose_an_entity) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

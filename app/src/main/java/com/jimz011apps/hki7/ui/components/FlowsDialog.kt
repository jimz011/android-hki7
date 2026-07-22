package com.jimz011apps.hki7.ui.components

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
import com.jimz011apps.hki7.data.automationBlockKind
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

private val automationRunModes = listOf(
    AutomationRunMode("single", "Single", "Ignore a new trigger while this flow is still running."),
    AutomationRunMode("restart", "Restart", "Stop the current run and start again when conditions still pass."),
    AutomationRunMode("queued", "Queued", "Wait, then run each trigger in the order it arrived."),
    AutomationRunMode("parallel", "Parallel", "Start every trigger as a separate run at the same time.")
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
    var editingBlock by remember { mutableStateOf<EditingBlock?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionDefinitions by remember { mutableStateOf<List<HAActionDefinition>>(emptyList()) }
    var actionLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.fetchRegistries()
        runCatching { viewModel.getAutomationActions() }
            .onSuccess { actionDefinitions = it; actionLoadError = null }
            .onFailure { actionLoadError = it.message ?: "Could not load Home Assistant actions" }
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
        FlowsView.LIST -> "Flows"
        FlowsView.RECIPES -> "Create a flow"
        FlowsView.EDITOR -> draft?.stringValue("alias")?.ifBlank { "Automation" } ?: "Automation"
    }
    val subtitle = when (currentView) {
        FlowsView.LIST -> "Native Home Assistant automations, always in sync"
        FlowsView.RECIPES -> "Start with a useful recipe, then adjust each step"
        FlowsView.EDITOR -> if (document?.editable == true) "When · And if · Then" else "Read-only Home Assistant automation"
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
                                .onFailure { message = it.message ?: "Could not load this automation" }
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
                        onAdd = { section ->
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
                            }
                        },
                        onEdit = { section, index, block -> editingBlock = EditingBlock(section, index, block) },
                        onRemove = { section, index ->
                            draft = withAutomationItems(
                                config,
                                section,
                                automationItems(config, section).filterIndexed { itemIndex, _ -> itemIndex != index }
                            )
                        }
                    )
                }
            }
        },
        footer = {
            when (currentView) {
                FlowsView.LIST -> {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { currentView = FlowsView.RECIPES }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New flow")
                    }
                }
                FlowsView.RECIPES -> {
                    TextButton(onClick = { currentView = FlowsView.LIST }) { Text("Cancel") }
                }
                FlowsView.EDITOR -> {
                    if (document?.editable == true && document?.entityId != null) {
                        TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
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
                                            if (errors.isEmpty()) closeEditor("Saved and loaded in Home Assistant")
                                            else message = errors.joinToString("\n")
                                        }
                                        .onFailure { message = it.message ?: "Could not save this automation" }
                                    busy = false
                                }
                            }
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Save in Home Assistant")
                        }
                    } else {
                        Button(onClick = { closeEditor() }) { Text("Done") }
                    }
                }
            }
        }
    )

    addSection?.let { section ->
        AddAutomationBlockDialog(
            section = section,
            onDismiss = { addSection = null },
            onSelected = { kind ->
                val config = draft ?: return@AddAutomationBlockDialog
                val block = newAutomationBlock(section, kind)
                val index = automationItems(config, section).size
                addSection = null
                editingBlock = EditingBlock(
                    section = section,
                    index = index,
                    block = block,
                    openEntityPickerInitially = kind == "state"
                )
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
                val blocks = automationItems(config, editing.section).toMutableList()
                if (editing.index in blocks.indices) blocks[editing.index] = updatedBlock
                else blocks.add(updatedBlock)
                draft = withAutomationItems(config, editing.section, blocks)
                editingBlock = null
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text("Delete automation?") },
            text = { Text("This removes the native automation from Home Assistant. This cannot be undone from HKI7.") },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("Cancel") } },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val id = document?.id ?: return@TextButton
                        busy = true
                        scope.launch {
                            runCatching { viewModel.deleteAutomation(id) }
                                .onSuccess { confirmDelete = false; closeEditor("Deleted from Home Assistant") }
                                .onFailure { message = it.message ?: "Could not delete this automation"; confirmDelete = false }
                            busy = false
                        }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
                placeholder = { Text("Search automations") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (automations.isEmpty()) "No native Home Assistant automations found" else "No matching automations",
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
                    if (entity.state == "unavailable") "Not loaded by Home Assistant"
                    else lastTriggered?.let { "Last run $it" } ?: "Never triggered",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onRun, enabled = !loading && entity.state != "unavailable") {
                Icon(Icons.Default.PlayArrow, "Run")
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
                "Recipes create regular Home Assistant automations. You can continue editing them in either HKI7 or Home Assistant.",
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
                    recipe.title,
                    color = appColors.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    recipe.description,
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
    onAdd: (AutomationSection) -> Unit,
    onEdit: (AutomationSection, Int, JsonObject) -> Unit,
    onRemove: (AutomationSection, Int) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!editable) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        "This automation is managed outside Home Assistant's UI (usually YAML). HKI7 can show it, but will not overwrite it.",
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
                label = { Text("Name") },
                enabled = editable,
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = config.stringValue("description"),
                onValueChange = { onConfigChange(withAutomationText(config, "description", it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
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
                    onAdd = { onAdd(section) }
                )
            }
            val blocks = automationItems(config, section)
            val shorthandCount = automationElements(config, section).size - blocks.size
            if (shorthandCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface)
                    ) {
                        Text(
                            if (shorthandCount == 1) "1 advanced shorthand block (kept unchanged)"
                            else "$shorthandCount advanced shorthand blocks (kept unchanged)",
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
                        if (section == AutomationSection.CONDITION) "No conditions — this flow always continues" else "Add at least one step",
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            } else {
                itemsIndexed(blocks, key = { index, _ -> "${section.name}-$index" }) { index, block ->
                    FlowBlockCard(
                        section = section,
                        block = block,
                        editable = editable,
                        onEdit = { onEdit(section, index, block) },
                        onRemove = { onRemove(section, index) }
                    )
                }
            }
        }
        item {
            val current = config.stringValue("mode").ifBlank { "single" }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Run mode", fontWeight = FontWeight.SemiBold)
                automationRunModes.forEach { mode ->
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
                                text = if (selected) "${mode.title} · Selected" else mode.title,
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

@Composable
private fun FlowSectionHeader(section: AutomationSection, editable: Boolean, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(section.title.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        if (editable) FilledTonalButton(onClick = onAdd) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add")
        }
    }
}

@Composable
private fun FlowBlockCard(
    section: AutomationSection,
    block: JsonObject,
    editable: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val supported = isSupportedAutomationBlock(section, block)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = editable && supported, onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.subtleSurface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    automationBlockKind(section, block)?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
                        ?: "Advanced",
                    fontWeight = FontWeight.SemiBold
                )
                Text(automationBlockSummary(section, block), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (editable && supported) Icon(Icons.Default.Edit, "Edit", tint = appColors.onMuted)
            if (editable) {
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove") }
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
        AutomationSection.TRIGGER -> listOf("state" to "Entity state", "time" to "Time", "sun" to "Sunrise or sunset")
        AutomationSection.CONDITION -> listOf("state" to "Entity state", "time" to "Time window")
        AutomationSection.ACTION -> listOf("action" to "Perform a Home Assistant action")
    }
    ModernSettingsDialogFrame(
        title = "Add to ${section.title}",
        subtitle = "Choose a visual Home Assistant block",
        icon = Icons.Default.Add,
        onDismiss = onDismiss,
        footer = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
    val title = "${section.title}: ${kind.replace('_', ' ').replaceFirstChar { it.uppercase() }}"
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
        subtitle = "Saved directly in the native automation",
        icon = Icons.Default.AccountTree,
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(working) }, enabled = canApply) { Text("Apply") }
        }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (section to kind) {
                AutomationSection.TRIGGER to "state" -> {
                    EntityField(working.stringValue("entity_id")) { showEntityPicker = true }
                    StateSelectorField(
                        label = "From state",
                        selected = working.stringValue("from"),
                        options = stateOptions,
                        allowAny = true,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("from", it) }
                    )
                    StateSelectorField(
                        label = "To state",
                        selected = working.stringValue("to"),
                        options = stateOptions,
                        allowAny = true,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("to", it) }
                    )
                }
                AutomationSection.TRIGGER to "time" ->
                    FlowTextField("Time (HH:MM:SS)", working.stringValue("at")) { updateText("at", it) }
                AutomationSection.TRIGGER to "sun" -> {
                    Text("Event", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("sunrise", "sunset").forEach { event ->
                            FilledTonalButton(onClick = { updateText("event", event) }) {
                                Text(if (working.stringValue("event") == event) "✓ ${event.replaceFirstChar { it.uppercase() }}" else event.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
                AutomationSection.CONDITION to "state" -> {
                    EntityField(working.stringValue("entity_id")) { showEntityPicker = true }
                    StateSelectorField(
                        label = "Required state",
                        selected = working.stringValue("state"),
                        options = stateOptions,
                        enabled = selectedEntity != null,
                        onSelected = { updateText("state", it) }
                    )
                }
                AutomationSection.CONDITION to "time" -> {
                    FlowTextField("After (HH:MM:SS)", working.stringValue("after")) { updateText("after", it) }
                    FlowTextField("Before (HH:MM:SS)", working.stringValue("before")) { updateText("before", it) }
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
                            Text("Target entity (optional)", style = MaterialTheme.typography.labelMedium)
                            EntityField(targetEntityId) { showEntityPicker = true }
                            if (targetEntityId.isNotBlank()) {
                                TextButton(
                                    onClick = { working = working.without("target", "entity_id") }
                                ) { Text("No target") }
                            }
                        } else if (selectedActionDefinition != null) {
                            Text(
                                "This action does not use an entity target.",
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
            title = if (section == AutomationSection.ACTION) "Choose target" else "Choose entity",
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
private fun StateSelectorField(
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
                        allowAny -> "Any state"
                        else -> "Choose a state"
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
                        text = { Text("Any state") },
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
                    text = { Text("Custom state...") },
                    onClick = { customMode = true; expanded = false }
                )
            }
        }
        if (customMode) {
            OutlinedTextField(
                value = selected,
                onValueChange = onSelected,
                label = { Text("Custom ${label.lowercase()}") },
                supportingText = { Text("Exact text or number reported by Home Assistant") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (!enabled) {
            Text("Choose an entity first", color = LocalHKIAppColors.current.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ActionSelectorField(selected: String, enabled: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Action", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Bolt, null)
            Spacer(Modifier.width(8.dp))
            Text(
                selected.ifBlank { if (enabled) "Choose an action" else "Loading actions…" },
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
        title = "Choose action",
        subtitle = preferredDomain?.let { "$it actions are shown first" } ?: "Actions available in Home Assistant",
        icon = Icons.Default.Bolt,
        onDismiss = onDismiss,
        footer = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search actions") },
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
        Text(entityId.ifBlank { "Choose an entity" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HKIButtonStack
import com.jimz011apps.hki7.data.expandedLightEntityIds
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal data class AdaptiveLightingProfile(
    val configEntryId: String,
    val name: String,
    val main: HAEntity,
    val sleepMode: HAEntity?,
    val adaptBrightness: HAEntity?,
    val adaptColor: HAEntity?,
    val configuredLightIds: Set<String> = emptySet()
)

private val adaptiveLightingHelperSuffixes = listOf(
    "_sleep_mode",
    "_adapt_brightness",
    "_adapt_color"
)

private fun adaptiveLightingEntityIds(value: kotlinx.serialization.json.JsonElement?): Set<String> = when (value) {
    is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
    is JsonPrimitive -> setOfNotNull(value.contentOrNull?.takeIf(String::isNotBlank))
    else -> emptySet()
}

/** Extracts the current `lights` value serialized by HA's config-entry options form. */
internal fun adaptiveLightingLightsFromOptionsForm(form: JsonObject): Set<String>? {
    val schema = form["data_schema"] as? JsonArray ?: return null
    val lightsField = schema.asSequence()
        .mapNotNull { it as? JsonObject }
        .firstOrNull { (it["name"] as? JsonPrimitive)?.contentOrNull == "lights" }
        ?: return null
    val value = lightsField["suggested_value"] ?: lightsField["default"]
    return adaptiveLightingEntityIds(value)
}

/** A profile controls a light when their direct or expanded group members overlap. */
internal fun adaptiveLightingProfilesForLight(
    light: HAEntity,
    profiles: List<AdaptiveLightingProfile>,
    entitiesById: Map<String, HAEntity>
): List<AdaptiveLightingProfile> {
    fun expanded(ids: Collection<String>): Set<String> = buildSet {
        ids.forEach { id ->
            add(id)
            addAll(entitiesById[id]?.childEntityIds.orEmpty())
        }
    }
    val lightIds = expanded(listOf(light.entity_id))
    return profiles.filter { profile ->
        profile.configuredLightIds.isNotEmpty() &&
            expanded(profile.configuredLightIds).any { it in lightIds }
    }
}

/** Builds profiles from registry ownership rather than entity names, which users are free to
 * rename. The integration's helper unique ids are derived from the main switch's unique id. */
internal fun resolveAdaptiveLightingProfiles(
    registry: List<HAEntityRegistryEntry>,
    entitiesById: Map<String, HAEntity>
): List<AdaptiveLightingProfile> {
    return registry.asSequence()
        .filter { it.platform == "adaptive_lighting" && it.entity_id.startsWith("switch.") }
        .groupBy { it.config_entry_id ?: it.device_id ?: "entity:${it.entity_id}" }
        .mapNotNull { (ownerId, entries) ->
            val mainEntry = entries.firstOrNull { candidate ->
                val uniqueId = candidate.unique_id ?: return@firstOrNull false
                adaptiveLightingHelperSuffixes.any { suffix ->
                    entries.any { it.unique_id == uniqueId + suffix }
                }
            } ?: entries.minByOrNull { it.unique_id?.length ?: Int.MAX_VALUE }
                ?: return@mapNotNull null
            val main = entitiesById[mainEntry.entity_id] ?: return@mapNotNull null
            val mainUniqueId = mainEntry.unique_id
            fun helper(suffix: String): HAEntity? = entries
                .firstOrNull { entry ->
                    mainUniqueId != null && entry.unique_id == mainUniqueId + suffix
                }
                ?.entity_id
                ?.let(entitiesById::get)

            AdaptiveLightingProfile(
                configEntryId = ownerId,
                name = main.friendlyName
                    ?.substringAfter("Adaptive Lighting: ", main.friendlyName.orEmpty())
                    ?.takeIf(String::isNotBlank)
                    ?: mainUniqueId?.replace('_', ' ')?.takeIf(String::isNotBlank)
                    ?: "Adaptive Lighting",
                main = main,
                sleepMode = helper("_sleep_mode"),
                adaptBrightness = helper("_adapt_brightness"),
                adaptColor = helper("_adapt_color"),
                configuredLightIds = ((main.attributes?.get("configuration") as? JsonObject)
                    ?.get("lights"))
                    .let(::adaptiveLightingEntityIds)
            )
        }
        .sortedBy { it.name.lowercase() }
}

/** Returns fully resolved, live profiles backed by the registry cache that is warmed at connect. */
@Composable
internal fun rememberAdaptiveLightingProfiles(viewModel: MainViewModel): List<AdaptiveLightingProfile> {
    val registry by viewModel.entityRegistry.collectAsState()
    val entitiesById by viewModel.entitiesById.collectAsState()
    val optionsForms by viewModel.adaptiveLightingOptionsForms.collectAsState()
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.fetchRegistries() }
    return remember(registry, entitiesById, optionsForms) {
        resolveAdaptiveLightingProfiles(registry, entitiesById).map { profile ->
            profile.copy(
                configuredLightIds = optionsForms[profile.configEntryId]
                    ?.let(::adaptiveLightingLightsFromOptionsForm)
                    ?: profile.configuredLightIds
            )
        }
    }
}

/** Dashboard widget variant of the dialog controls. The stored profile ids determine what the
 * selector is allowed to expose; an empty list deliberately means all installed profiles. */
@Composable
internal fun AdaptiveLightingWidget(
    stack: HKIButtonStack,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (stack.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    val entitiesById by viewModel.entitiesById.collectAsState()
    val installedProfiles = rememberAdaptiveLightingProfiles(viewModel)
    val profiles = remember(installedProfiles, stack.adaptiveLightingProfileIds) {
        if (stack.adaptiveLightingProfileIds.isEmpty()) installedProfiles
        else installedProfiles.filter { it.configEntryId in stack.adaptiveLightingProfileIds }
    }
    if (profiles.isEmpty() && !isEditMode) return

    var preferredProfileId by remember(stack.id) { mutableStateOf<String?>(null) }
    val selectedProfile = profiles.firstOrNull { it.configEntryId == preferredProfileId }
        ?: profiles.firstOrNull { it.main.state == "on" }
        ?: profiles.firstOrNull()
    var profileMenuOpen by remember { mutableStateOf(false) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val compact = stack.buttonStyle == "tile"
    val doubleRow = stack.adaptiveLightingLayout == "double_row"

    fun runCommand(label: String, command: suspend () -> Unit) {
        if (busyAction != null) return
        scope.launch {
            busyAction = label
            feedback = null
            runCatching { command() }
                .onSuccess { feedback = "$label updated" }
                .onFailure { feedback = it.message ?: "Could not update $label" }
            busyAction = null
        }
    }

    fun toggle(label: String, entity: HAEntity?) {
        entity ?: return
        runCommand(label) {
            viewModel.callServiceRawAwait(
                "switch",
                if (entity.state == "on") "turn_off" else "turn_on",
                buildJsonObject {
                    put("entity_id", buildJsonArray { add(JsonPrimitive(entity.entity_id)) })
                }
            )
        }
    }

    val manualControlIds = selectedProfile?.main?.attributes
        ?.get("manual_control")
        .let { it as? JsonArray }
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .orEmpty()
        .toSet()
    val targets = selectedProfile?.configuredLightIds.orEmpty().toList().sorted()
    val pausedCount = expandedLightEntityIds(targets, entitiesById).count { it in manualControlIds }

    fun callAction(action: String, manualControl: Boolean? = null) {
        val profile = selectedProfile ?: return
        if (targets.isEmpty()) return
        val label = when (action) {
            "apply" -> "Adapt now"
            "set_manual_control" -> if (manualControl == true) "Pause adaptation" else "Resume adaptation"
            else -> "Adaptive Lighting"
        }
        runCommand(label) {
            viewModel.callServiceRawAwait(
                "adaptive_lighting",
                action,
                buildJsonObject {
                    put("entity_id", buildJsonArray { add(JsonPrimitive(profile.main.entity_id)) })
                    put("lights", buildJsonArray { targets.forEach { add(JsonPrimitive(it)) } })
                    manualControl?.let { put("manual_control", it) }
                }
            )
        }
    }

    val sizeModifier = when {
        stack.isSquare || stack.buttonStyle == "square" -> Modifier.aspectRatio(1f)
        doubleRow -> Modifier.height(112.dp)
        compact -> Modifier.height(148.dp)
        else -> Modifier.heightIn(min = 190.dp)
    }
    Box(modifier = Modifier.fillMaxWidth().then(sizeModifier)) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(stack.cornerRadius.dp),
            color = appColors.elevated
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MdiIcon(
                    stack.icon ?: "auto-awesome",
                    tint = MaterialTheme.colorScheme.primary,
                    size = 22.dp
                )
                Column(Modifier.weight(1f)) {
                    if (stack.showName) {
                        Text(
                            stack.title ?: "Adaptive Lighting",
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        selectedProfile?.name ?: "No profile available",
                        color = appColors.onMuted,
                        style = if (stack.showName) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                        fontWeight = if (stack.showName) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isEditMode && profiles.size > 1) {
                    Box {
                        OutlinedButton(onClick = { profileMenuOpen = true }, modifier = Modifier.height(34.dp)) {
                            Text("Profile", maxLines = 1)
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(17.dp))
                        }
                        DropdownMenu(expanded = profileMenuOpen, onDismissRequest = { profileMenuOpen = false }) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        preferredProfileId = profile.configEntryId
                                        profileMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Adaptive Lighting is unavailable", color = appColors.onMuted)
                }
            } else {
                if (!doubleRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        AdaptiveLightingSwitchChip("Adaptive", Icons.Default.AutoAwesome, selectedProfile?.main, !isEditMode && busyAction == null, ::toggle)
                        AdaptiveLightingSwitchChip("Brightness", Icons.Default.LightMode, selectedProfile?.adaptBrightness, !isEditMode && busyAction == null, ::toggle)
                        AdaptiveLightingSwitchChip("Color", Icons.Default.Palette, selectedProfile?.adaptColor, !isEditMode && busyAction == null, ::toggle)
                        AdaptiveLightingSwitchChip("Sleep", Icons.Default.Bedtime, selectedProfile?.sleepMode, !isEditMode && busyAction == null, ::toggle)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (stack.adaptiveLightingCenterActions) {
                        Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)
                    } else {
                        Arrangement.spacedBy(7.dp)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { callAction("apply") },
                        enabled = !isEditMode && targets.isNotEmpty() && busyAction == null,
                        label = { Text("Adapt now") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)) }
                    )
                    AssistChip(
                        onClick = { callAction("set_manual_control", pausedCount == 0) },
                        enabled = !isEditMode && targets.isNotEmpty() && busyAction == null,
                        label = { Text(if (pausedCount > 0) "Resume" else "Pause") },
                        leadingIcon = {
                            Icon(
                                if (pausedCount > 0) Icons.Default.PlayArrow else Icons.Default.Pause,
                                null,
                                Modifier.size(17.dp)
                            )
                        }
                    )
                    if (busyAction != null) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                if (!compact && !doubleRow) {
                    Text(
                        feedback ?: "Controls ${targets.size} light${if (targets.size == 1) "" else "s"} in this profile.",
                        color = if (feedback?.contains("could not", ignoreCase = true) == true) {
                            MaterialTheme.colorScheme.error
                        } else appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        }
        if (isEditMode) {
            EditSettingsButton(
                onClick = onSettings,
                modifier = Modifier.align(Alignment.Center)
            )
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
internal fun AdaptiveLightingSection(
    light: HAEntity,
    profiles: List<AdaptiveLightingProfile>,
    viewModel: MainViewModel
) {
    if (profiles.isEmpty()) return

    val appColors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    val mappings by viewModel.prefs.adaptiveLightingProfiles.collectAsState(initial = emptyMap())
    val entitiesById by viewModel.entitiesById.collectAsState()
    val availableProfileIds = remember(profiles) { profiles.mapTo(mutableSetOf()) { it.configEntryId } }
    val directProfileId = mappings[light.entity_id]?.takeIf { it in availableProfileIds }
    val inheritedProfileId = remember(light, mappings, entitiesById, availableProfileIds) {
        val fromMembers = light.childEntityIds.mapNotNull(mappings::get)
            .filter { it in availableProfileIds }
            .distinct()
            .singleOrNull()
        val fromContainingGroups = mappings.asSequence()
            .filter { (_, profileId) -> profileId in availableProfileIds }
            .filter { (entityId, _) -> light.entity_id in entitiesById[entityId]?.childEntityIds.orEmpty() }
            .map { it.value }
            .distinct()
            .singleOrNull()
        fromMembers ?: fromContainingGroups
    }
    val selectedProfileId = directProfileId
        ?: inheritedProfileId
        ?: profiles.firstOrNull { it.main.state == "on" }?.configEntryId
        ?: profiles.first().configEntryId
    val selectedProfile = profiles.firstOrNull { it.configEntryId == selectedProfileId }
    var profileMenuOpen by remember { mutableStateOf(false) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val manualControlIds = selectedProfile?.main?.attributes
        ?.get("manual_control")
        .let { it as? JsonArray }
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .orEmpty()
        .toSet()
    val lightTargets = remember(light.entity_id, light.childEntityIds) {
        buildSet {
            add(light.entity_id)
            addAll(light.childEntityIds)
        }
    }
    val manualCount = lightTargets.count { it in manualControlIds }

    fun runCommand(label: String, command: suspend () -> Unit) {
        if (busyAction != null) return
        scope.launch {
            busyAction = label
            feedback = null
            runCatching { command() }
                .onSuccess { feedback = "$label updated" }
                .onFailure { feedback = it.message ?: "Could not update $label" }
            busyAction = null
        }
    }

    fun toggle(label: String, entity: HAEntity?) {
        entity ?: return
        runCommand(label) {
            viewModel.callServiceRawAwait(
                "switch",
                if (entity.state == "on") "turn_off" else "turn_on",
                buildJsonObject {
                    put("entity_id", buildJsonArray { add(JsonPrimitive(entity.entity_id)) })
                }
            )
        }
    }

    fun callAdaptiveLightingAction(action: String, manualControl: Boolean? = null) {
        val profile = selectedProfile ?: return
        val payload = buildJsonObject {
            put("entity_id", buildJsonArray { add(JsonPrimitive(profile.main.entity_id)) })
            put("lights", buildJsonArray { add(JsonPrimitive(light.entity_id)) })
            manualControl?.let { put("manual_control", it) }
        }
        val label = when (action) {
            "apply" -> "Adapt now"
            "set_manual_control" -> if (manualControl == true) "Pause adaptation" else "Resume adaptation"
            else -> "Adaptive Lighting"
        }
        runCommand(label) {
            viewModel.callServiceRawAwait("adaptive_lighting", action, payload)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = itemCornerShape(),
        color = appColors.subtleSurface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Adaptive Lighting",
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        selectedProfile?.name ?: "Choose the profile for this light",
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (profiles.size > 1) {
                    Box {
                        OutlinedButton(
                            onClick = { profileMenuOpen = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Profile", maxLines = 1)
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = profileMenuOpen,
                            onDismissRequest = { profileMenuOpen = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        profileMenuOpen = false
                                        scope.launch {
                                            viewModel.prefs.saveAdaptiveLightingProfile(
                                                light.entity_id,
                                                profile.configEntryId
                                            )
                                        }
                                    }
                                )
                            }
                            if (directProfileId != null) {
                                DropdownMenuItem(
                                    text = { Text("Forget selection") },
                                    onClick = {
                                        profileMenuOpen = false
                                        scope.launch {
                                            viewModel.prefs.saveAdaptiveLightingProfile(light.entity_id, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdaptiveLightingSwitchChip("Adaptive", Icons.Default.AutoAwesome, selectedProfile?.main, busyAction == null, ::toggle)
                AdaptiveLightingSwitchChip("Brightness", Icons.Default.LightMode, selectedProfile?.adaptBrightness, busyAction == null, ::toggle)
                AdaptiveLightingSwitchChip("Color", Icons.Default.Palette, selectedProfile?.adaptColor, busyAction == null, ::toggle)
                AdaptiveLightingSwitchChip("Sleep", Icons.Default.Bedtime, selectedProfile?.sleepMode, busyAction == null, ::toggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { callAdaptiveLightingAction("apply") },
                    enabled = selectedProfile != null && busyAction == null,
                    label = { Text("Adapt now") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)) }
                )
                AssistChip(
                    onClick = {
                        callAdaptiveLightingAction(
                            "set_manual_control",
                            manualControl = manualCount == 0
                        )
                    },
                    enabled = selectedProfile != null && busyAction == null,
                    label = { Text(if (manualCount > 0) "Resume adaptation" else "Pause adaptation") },
                    leadingIcon = {
                        Icon(
                            if (manualCount > 0) Icons.Default.PlayArrow else Icons.Default.Pause,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                )
            }

            Text(
                if (manualCount > 0) {
                    if (light.childEntityIds.isEmpty()) "This light is paused for manual control."
                    else "$manualCount group light${if (manualCount == 1) " is" else "s are"} paused for manual control."
                } else {
                    "Profile switches affect every light assigned to ${selectedProfile?.name ?: "that profile"}."
                },
                color = appColors.onMuted,
                style = MaterialTheme.typography.labelSmall
            )
            if (busyAction != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "$busyAction…",
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else if (feedback != null) {
                Text(
                    feedback.orEmpty(),
                    color = if (feedback?.contains("could not", ignoreCase = true) == true ||
                        feedback?.contains("failed", ignoreCase = true) == true
                    ) MaterialTheme.colorScheme.error else appColors.onMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun AdaptiveLightingSwitchChip(
    label: String,
    icon: ImageVector,
    entity: HAEntity?,
    enabled: Boolean,
    onToggle: (String, HAEntity?) -> Unit
) {
    FilterChip(
        selected = entity?.state == "on",
        enabled = enabled && entity != null && entity.state != "unavailable",
        onClick = { onToggle(label, entity) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(17.dp)) }
    )
}

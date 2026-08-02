@file:Suppress("UnusedBoxWithConstraintsScope", "SpellCheckingInspection", "GrazieInspection")

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.visibilityConditionEntityIds
import com.jimz011apps.hki7.data.HKIAction
import com.jimz011apps.hki7.data.HKIBadge
import com.jimz011apps.hki7.data.HKIBadgeBarConfig
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.localizedStateLabel
import com.jimz011apps.hki7.ui.utils.handleActionOutcome
import com.jimz011apps.hki7.ui.screens.PagedRoleDialog
import com.jimz011apps.hki7.ui.screens.AggregatedCoverDialog
import com.jimz011apps.hki7.ui.screens.VacuumStackDialog
import com.jimz011apps.hki7.ui.screens.resolveVacuumDeviceEntities
import com.jimz011apps.hki7.ui.screens.UniversalStackDialog
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Domain helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun domainRole(entityId: String): String = when {
    entityId.startsWith("light.")               -> "light"
    entityId.startsWith("climate.")              -> "climate"
    entityId.startsWith("lock.")                 -> "lock"
    entityId.startsWith("cover.")                -> "cover"
    entityId.startsWith("camera.")               -> "camera"
    entityId.startsWith("vacuum.")                -> "vacuum"
    entityId.startsWith("fan.")                  -> "fan"
    entityId.startsWith("humidifier.")           -> "humidifier"
    entityId.startsWith("alarm_control_panel.")  -> "alarm"
    entityId.startsWith("person.")               -> "person"
    else                                         -> "generic"
}

/** Picks the entity to display in a multi-entity badge: the first "active"/attention-worthy one, else the first. */
private fun representativeBadgeEntity(entities: List<HAEntity>): HAEntity? {
    if (entities.size <= 1) return entities.firstOrNull()
    return entities.firstOrNull { e ->
        val s = e.state.lowercase()
        when (e.entity_id.substringBefore(".")) {
            "lock"    -> s != "locked"
            "cover"   -> s != "closed" && s != "unavailable"
            "climate" -> s != "off"
            "person"  -> s == "home"
            else      -> s == "on"
        }
    } ?: entities.first()
}

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HKIBadgeBar(
    badgeBarConfig: HKIBadgeBarConfig?,
    isEditMode: Boolean,
    viewModel: MainViewModel,
    onConfigChange: (HKIBadgeBarConfig?) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val config = badgeBarConfig ?: HKIBadgeBarConfig()
    val badges = config.badges
    // When an admin restricts a user to aesthetic-only editing, adding/removing badges is a structural
    // change and stays locked — only visual tweaks to existing badges are allowed.
    val aestheticsOnly by viewModel.aestheticsOnlyEditing.collectAsState()
    val alignment = config.alignment
    val dependencyIds = remember(badges) {
        buildSet {
            badges.forEach { badge ->
                addAll(badge.effectiveEntityIds)
                badge.doorEntityId?.let(::add)
                addAll(badge.doorEntityIds.values)
                addAll(badge.vacuumMapEntityIds.values)
                addAll(badge.vacuumBatteryEntityIds.values)
                addAll(badge.vacuumWaterEntityIds.values)
                addAll(badge.vacuumEmptyBinEntityIds.values)
                badge.humidifierFanEntityId?.let(::add)
                addAll(badge.humidifierAuxEntityIds.values)
                addAll(badge.visibilityConditionEntityIds())
            }
        }.toList()
    }
    // The always-visible badge bar only observes entities it renders. Entity pickers obtain their
    // own full snapshot when opened, so edit mode does not need to invalidate every badge for every
    // Home Assistant state change.
    val entityFlow = remember(viewModel, dependencyIds, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(dependencyIds) else viewModel.entitiesFor(dependencyIds)
    }
    val allEntities by entityFlow.collectAsState()
    val badgeEntityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }
    val parentalHiddenItemIds by viewModel.prefs.parentalHiddenItemIds.collectAsState(initial = emptyList())
    // Hidden/scheduled/conditional/per-user-hidden badges are dropped outside edit mode; edit mode
    // keeps them so they can be restored.
    val renderBadges = if (isEditMode) badges else badges.filter {
        it.id !in parentalHiddenItemIds && com.jimz011apps.hki7.data.isBadgeVisibleNow(
            it,
            resolveEntityState = { id -> badgeEntityById[id]?.state }
        )
    }

    // ── dialog state ──────────────────────────────────────────────────────────
    var dialogRole    by remember { mutableStateOf<String?>(null) }
    var dialogList    by remember { mutableStateOf<List<HAEntity>>(emptyList()) }
    var dialogBadge   by remember { mutableStateOf<HKIBadge?>(null) }

    // ── edit-mode state ───────────────────────────────────────────────────────
    var showEntityPicker by remember { mutableStateOf(false) }
    var editingBadge     by remember { mutableStateOf<HKIBadge?>(null) }
    var pendingAddSide   by remember { mutableStateOf<String?>(null) }
    val needsEntityCatalog = showEntityPicker || editingBadge != null
    val entityCatalogFlow = remember(viewModel, needsEntityCatalog) { viewModel.entityList(live = needsEntityCatalog) }
    val entityCatalog by entityCatalogFlow.collectAsState()

    fun addBadge(side: String = "right") {
        pendingAddSide = side
        showEntityPicker = true
    }

    fun saveBadges(newBadges: List<HKIBadge>) {
        onConfigChange(config.copy(badges = newBadges))
    }

    fun openMore(badge: HKIBadge) {
        val entities = badge.effectiveEntityIds.mapNotNull { id -> allEntities.find { it.entity_id == id } }
        if (entities.isEmpty()) return
        dialogList = entities
        dialogRole = domainRole(entities.first().entity_id)
        dialogBadge = badge
    }

    // Badges default to opening the entity dialog ("more_info") rather than the domain-based
    // default; toggling and richer actions are opt-in via badge settings.
    fun resolveBadgeAction(badge: HKIBadge, trigger: String): HKIAction {
        val ex = if (trigger == "hold") badge.holdActionEx else badge.tapActionEx
        if (ex != null) return ex
        val legacy = if (trigger == "hold") badge.holdAction else badge.tapAction
        return HKIAction(type = if (legacy == "auto") "more_info" else legacy)
    }

    fun dispatchBadge(badge: HKIBadge, trigger: String) {
        val primary = badge.effectiveEntityIds.firstOrNull() ?: badge.entityId
        handleActionOutcome(
            viewModel.executeAction(resolveBadgeAction(badge, trigger), primary, trigger),
            context, navController
        ) { openMore(badge) }
    }

    fun handleTap(badge: HKIBadge) = dispatchBadge(badge, "tap")

    fun handleHold(badge: HKIBadge) = dispatchBadge(badge, "hold")

    // ── nothing to show ───────────────────────────────────────────────────────
    if (!config.visible || (!isEditMode && renderBadges.isEmpty())) return

    // ── layout ────────────────────────────────────────────────────────────────
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            badges.isEmpty() && isEditMode -> {
                // Empty bar in edit mode: + Add pill + × Remove (if config exists)
                if (!aestheticsOnly) AddBadgePill { addBadge(if (alignment == "split") "left" else "right") }
            }

            alignment == "split" -> {
                val leftBadges  = renderBadges.filter { it.side == "left" }
                val rightBadges = renderBadges.filter { it.side == "right" }

                if (isEditMode) {
                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val gap = 56.dp
                        val laneWidth = (maxWidth - gap) / 2
                        val leftScrollState = rememberScrollState()
                        val rightScrollState = rememberScrollState()
                        var leftLaneWidthPx by remember { mutableIntStateOf(0) }
                        var rightLaneWidthPx by remember { mutableIntStateOf(0) }
                        Box(
                            modifier = Modifier
                                .width(laneWidth)
                                .align(Alignment.CenterStart)
                                .onSizeChanged { leftLaneWidthPx = it.width },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.horizontalScroll(leftScrollState),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BadgeDraggableRow(
                                    badges = leftBadges,
                                    allEntities = allEntities,
                                    isEditMode = true,
                                    currentUrl = currentUrl,
                                    scrollState = leftScrollState,
                                    viewportWidthPx = leftLaneWidthPx,
                                    onTap = { b -> handleTap(b) },
                                    onHold = { b -> editingBadge = b },
                                    onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                                    onNewOrder = { ordered -> saveBadges(ordered + rightBadges) }
                                )
                                if (!aestheticsOnly) AddBadgePill { addBadge("left") }
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(laneWidth)
                                .align(Alignment.CenterEnd)
                                .onSizeChanged { rightLaneWidthPx = it.width },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier.horizontalScroll(rightScrollState),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!aestheticsOnly) AddBadgePill { addBadge("right") }
                                BadgeDraggableRow(
                                    badges = rightBadges,
                                    allEntities = allEntities,
                                    isEditMode = true,
                                    currentUrl = currentUrl,
                                    scrollState = rightScrollState,
                                    viewportWidthPx = rightLaneWidthPx,
                                    onTap = { b -> handleTap(b) },
                                    onHold = { b -> editingBadge = b },
                                    onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                                    onNewOrder = { ordered -> saveBadges(leftBadges + ordered) }
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                    }
                    return@Row
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val gap = 12.dp
                    val halfWidth = (maxWidth - gap) / 2
                    val leftWidth = when {
                        config.leftOverflow && rightBadges.isEmpty() -> maxWidth
                        config.leftOverflow -> maxWidth - halfWidth - gap
                        else -> halfWidth
                    }
                    val rightWidth = when {
                        config.rightOverflow && leftBadges.isEmpty() -> maxWidth
                        config.rightOverflow -> maxWidth - halfWidth - gap
                        else -> halfWidth
                    }

                    Box(
                        modifier = Modifier.width(leftWidth).align(Alignment.CenterStart),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BadgeDraggableRow(
                            badges = leftBadges,
                            allEntities = allEntities,
                            isEditMode = isEditMode,
                            currentUrl = currentUrl,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            onTap    = { b -> handleTap(b) },
                            onHold   = { b -> if (isEditMode) editingBadge = b else handleHold(b) },
                            onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                            onNewOrder = { ordered -> saveBadges(ordered + rightBadges) }
                        )
                    }

                    Box(
                        modifier = Modifier.width(rightWidth).align(Alignment.CenterEnd),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        BadgeDraggableRow(
                            badges = rightBadges,
                            allEntities = allEntities,
                            isEditMode = isEditMode,
                            currentUrl = currentUrl,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            onTap    = { b -> handleTap(b) },
                            onHold   = { b -> if (isEditMode) editingBadge = b else handleHold(b) },
                            onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                            onNewOrder = { ordered -> saveBadges(leftBadges + ordered) }
                        )
                    }
                }
            }

            else -> {
                // Non-split: the badge row fills the remaining width so Left/Center/Right and Span
                // (evenly-spread) actually take effect. "Span" spreads the badges across the width;
                // the other modes position the wrapped group via the outer row's arrangement.
                val groupArrangement: Arrangement.Horizontal = when (alignment) {
                    "left"  -> Arrangement.Start
                    "right" -> Arrangement.End
                    else    -> Arrangement.Center
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = groupArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeDraggableRow(
                        badges = renderBadges,
                        allEntities = allEntities,
                        isEditMode = isEditMode,
                        currentUrl = currentUrl,
                        modifier = if (config.spanIcons) Modifier.weight(1f) else Modifier,
                        arrangement = if (config.spanIcons) Arrangement.SpaceEvenly else Arrangement.spacedBy(8.dp),
                        onTap    = { b -> handleTap(b) },
                        onHold   = { b -> if (isEditMode) editingBadge = b else handleHold(b) },
                        onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                        onNewOrder = { ordered -> saveBadges(ordered) }
                    )
                    if (isEditMode) {
                        Spacer(Modifier.width(8.dp))
                        if (!aestheticsOnly) AddBadgePill { addBadge("right") }
                    }
                }
            }
        }
    }

    // ── entity picker ─────────────────────────────────────────────────────────
    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = entityCatalog,
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { selectedIds ->
                val defaultSide = if (alignment == "split") pendingAddSide ?: "right" else "right"
                val newBadges = selectedIds.map { id ->
                    HKIBadge(id = UUID.randomUUID().toString(), entityId = id, side = defaultSide)
                }
                saveBadges(badges + newBadges)
                showEntityPicker = false
            }
        )
    }

    // ── per-badge settings ────────────────────────────────────────────────────
    editingBadge?.let { badge ->
        BadgeSettingsDialog(
            badge = badge,
            allEntities = entityCatalog,
            viewModel = viewModel,
            showSidePicker = alignment == "split",
            onDismiss = { editingBadge = null },
            onSave = { updated ->
                saveBadges(badges.map { if (it.id == badge.id) updated else it })
                editingBadge = null
            },
            onRemove = {
                saveBadges(badges.filter { it.id != badge.id })
                editingBadge = null
            }
        )
    }

    // ── domain dialogs ────────────────────────────────────────────────────────
    val dr = dialogRole
    val badge = dialogBadge
    if (dr != null && dialogList.isNotEmpty()) {
        val accessToken  by viewModel.accessToken.collectAsState()
        // Always resolve live copies so the dialog reflects real-time state
        val live = dialogList.map { e -> allEntities.find { it.entity_id == e.entity_id } ?: e }
        val de = live.first()
        val dismiss = { dialogRole = null; dialogList = emptyList(); dialogBadge = null }
        CompositionLocalProvider(
            LocalDialogCustomButtons provides (badge?.customButtons ?: emptyList()),
            LocalDialogNavController provides navController
        ) {
        when (dr) {
            "light" -> {
                if (live.size == 1) {
                    HKILightDialog(
                        entity = de,
                        onDismiss = dismiss,
                        viewModel = viewModel,
                        iconName = badge?.customIcon
                    )
                } else {
                    UniversalStackDialog(
                        entities = live, allEntities = allEntities, currentUrl = currentUrl,
                        buttonConfigs = live.associate { e ->
                            e.entity_id to HKIButtonConfig(icon = badge?.customIcon)
                        },
                        viewModel = viewModel, onDismiss = dismiss
                    )
                }
            }
            "climate" -> PagedRoleDialog(
                "climate",
                live,
                viewModel,
                dismiss,
                live.associate { e ->
                    e.entity_id to HKIButtonConfig(
                        icon = badge?.customIcon,
                        climateDialogControl = badge?.climateDialogControl ?: "slider"
                    )
                }
            )
            "lock" -> {
                val doorEntities = live.associate { e ->
                    e.entity_id to (badge?.doorEntityIdFor(e.entity_id)?.let { id -> allEntities.find { it.entity_id == id } })
                }
                HKILockDialog(
                    entity = de, entities = live, doorEntities = doorEntities,
                    onDismiss = dismiss, viewModel = viewModel,
                    iconNames = live.associate { it.entity_id to badge?.customIcon }
                )
            }
            "cover" -> AggregatedCoverDialog(
                entities = live,
                viewModel = viewModel,
                onDismiss = dismiss,
                iconName = badge?.customIcon
            )
            "fan" -> HKIFanDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                onDismiss = dismiss
            )
            "humidifier" -> HKIHumidifierDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                fanEntity = badge?.humidifierFanEntityId?.let { id -> allEntities.find { it.entity_id == id } },
                auxEntities = badge?.humidifierAuxEntityIds.orEmpty().mapNotNull { (k, id) -> allEntities.find { it.entity_id == id }?.let { k to it } }.toMap(),
                onDismiss = dismiss
            )
            "alarm" -> HKIAlarmDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                onDismiss = dismiss
            )
            "person" -> PersonDetailDialog(person = de, viewModel = viewModel, onDismiss = dismiss)
            "vacuum" -> {
                val buttonConfigs = live.associate { e ->
                    e.entity_id to HKIButtonConfig(
                        icon = badge?.customIcon,
                        vacuumDeviceId = badge?.vacuumDeviceIds?.get(e.entity_id),
                        vacuumMapEntityId = badge?.vacuumMapEntityIds?.get(e.entity_id),
                        vacuumBatteryEntityId = badge?.vacuumBatteryEntityIds?.get(e.entity_id),
                        vacuumWaterEntityId = badge?.vacuumWaterEntityIds?.get(e.entity_id),
                        vacuumEmptyBinEntityId = badge?.vacuumEmptyBinEntityIds?.get(e.entity_id)
                    )
                }
                VacuumStackDialog(
                    entities = live, startIndex = 0, buttonConfigs = buttonConfigs,
                    allEntities = allEntities, currentUrl = currentUrl,
                    viewModel = viewModel, onDismiss = dismiss
                )
            }
            "camera" -> {
                // Swipeable pages with page dots (like the covers/vacuum stacks), where each page keeps
                // the full live stream + fullscreen button — instead of a stripped-down snapshot.
                CameraStackDialog(
                    cameras = live,
                    startIndex = 0,
                    currentUrl = currentUrl,
                    authToken = accessToken,
                    viewModel = viewModel,
                    onDismiss = dismiss
                )
            }
            else -> HKIDialog(
                entity = de,
                onDismiss = dismiss,
                viewModel = viewModel,
                icon = domainIcon(de),
                iconTint = MaterialTheme.colorScheme.primary,
                iconName = badge?.customIcon,
                showHistoryButton = true
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val text = de.localizedStateLabel().uppercase()
                        Text(text, style = MaterialTheme.typography.headlineMedium, color = LocalHKIAppColors.current.onSurface)
                    }
                }
            }
        }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Draggable badge row
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BadgeDraggableRow(
    badges: List<HKIBadge>,
    allEntities: List<HAEntity>,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
    currentUrl: String = "",
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    scrollState: ScrollState? = null,
    viewportWidthPx: Int = 0,
    onTap: (HKIBadge) -> Unit,
    onHold: (HKIBadge) -> Unit,
    onRemove: (HKIBadge) -> Unit,
    onNewOrder: (List<HKIBadge>) -> Unit
) {
    if (badges.isEmpty()) {
        Row(modifier = modifier.height(36.dp)) {}
        return
    }

    // Local mutable list drives the UI during a drag; committed to parent on drag-end
    var localList by remember(badges) { mutableStateOf(badges) }
    LaunchedEffect(badges) { localList = badges }

    var dragIndex   by remember { mutableIntStateOf(-1) }
    var dragDeltaX  by remember { mutableFloatStateOf(0f) }
    val itemBounds  = remember { mutableStateMapOf<Int, Rect>() }
    val autoScrollScope = rememberCoroutineScope()
    // One id→entity map per entity-list update instead of a linear scan per badge entity.
    val entityById = remember(allEntities) { allEntities.associateBy { it.entity_id } }

    Row(
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(if (isEditMode) {
            Modifier.pointerInput(localList.size, scrollState, viewportWidthPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val idx = itemBounds.entries
                            .minByOrNull { (_, r) -> abs(r.left + r.width / 2 - offset.x) }?.key
                        dragIndex  = idx ?: -1
                        dragDeltaX = 0f
                    },
                    onDrag = { _, amount ->
                        if (dragIndex < 0) return@detectDragGesturesAfterLongPress
                        dragDeltaX += amount.x
                        val fromBounds = itemBounds[dragIndex] ?: return@detectDragGesturesAfterLongPress
                        val dragCx = fromBounds.left + fromBounds.width / 2 + dragDeltaX
                        if (scrollState != null && viewportWidthPx > 0) {
                            val edge = 42f
                            val leftEdge = scrollState.value.toFloat() + edge
                            val rightEdge = scrollState.value.toFloat() + viewportWidthPx - edge
                            when {
                                dragCx < leftEdge && scrollState.value > 0 ->
                                    autoScrollScope.launch { scrollState.scrollBy(-18f) }
                                dragCx > rightEdge && scrollState.value < scrollState.maxValue ->
                                    autoScrollScope.launch { scrollState.scrollBy(18f) }
                            }
                        }
                        val target = when {
                            amount.x < 0 -> dragIndex - 1
                            amount.x > 0 -> dragIndex + 1
                            else -> dragIndex
                        }
                        val targetBounds = itemBounds[target] ?: return@detectDragGesturesAfterLongPress
                        val targetCenter = targetBounds.left + targetBounds.width / 2
                        val crossed = if (target < dragIndex) dragCx < targetCenter else dragCx > targetCenter
                        if (target in localList.indices && target != dragIndex && crossed) {
                            val newList = localList.toMutableList().apply { add(target, removeAt(dragIndex)) }
                            localList  = newList
                            itemBounds.clear()
                            dragIndex  = target
                            dragDeltaX = fromBounds.left + dragDeltaX - targetBounds.left
                        }
                    },
                    onDragEnd    = { if (dragIndex >= 0) { onNewOrder(localList); dragIndex = -1; dragDeltaX = 0f } },
                    onDragCancel = { localList = badges; dragIndex = -1; dragDeltaX = 0f }
                )
            }
        } else Modifier)
    ) {
        localList.forEachIndexed { idx, badge ->
            val entities = badge.effectiveEntityIds.mapNotNull { id -> entityById[id] }
            val isDragging = isEditMode && idx == dragIndex
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        if (isEditMode) itemBounds[idx] = coords.boundsInParent()
                    }
                    .then(
                        if (isDragging)
                            Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(dragDeltaX.roundToInt(), 0) }
                                .zIndex(10f)
                                .scale(1.08f)
                        else Modifier.zIndex(1f)
                    )
                    .graphicsLayer { alpha = if (isDragging) 0.85f else 1f }
            ) {
                BadgeItem(
                    badge = badge,
                    entities = entities,
                    allEntities = allEntities,
                    isEditMode = isEditMode,
                    currentUrl = currentUrl,
                    onTap    = { onTap(badge) },
                    onHold   = { onHold(badge) },
                    onRemove = { onRemove(badge) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single badge chip
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BadgeItem(
    badge: HKIBadge,
    entities: List<HAEntity>,
    allEntities: List<HAEntity>,
    isEditMode: Boolean,
    currentUrl: String,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onRemove: () -> Unit
) {
    val isCircle = badge.shape == "circle"
    val shape    = if (isCircle) CircleShape else itemCornerShape()
    val sizeMod  = if (isCircle) Modifier.size(36.dp) else Modifier.height(36.dp)
    // Display the most attention-worthy entity (e.g. the cover that is not closed)
    val entity = representativeBadgeEntity(entities)

    // Lock/cover door override: show "Open" text when a mapped door sensor is on
    val isDoorOpen = entity != null && entity.entity_id.startsWith("lock.") &&
        badge.doorEntityIdFor(entity.entity_id)
            ?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true

    val rawColors = badgeStateColors(badge, entities, allEntities)
    val colors = rawColors.copy(
        icon = semanticColorForBackground(rawColors.icon, rawColors.background)
    )

    // A custom MDI slug wins; otherwise use the state-aware default (lock/cover), falling
    // back to a Material domain icon for domains without a custom default.
    val customSlug = badge.customIcon?.takeIf { it.isNotBlank() }
    val defaultSlug = entity?.let {
        defaultEntityIconSlug(
            it,
            lockDoorOpen = isDoorOpen,
        )
    }
    val effectiveSlug = customSlug ?: defaultSlug
    val fallbackIcon = entity?.let { domainIcon(it) } ?: Icons.Default.Circle
    // "Use entity picture": render the HA picture when available, else fall back to the icon.
    val pictureUrl = if (customSlug == ENTITY_PICTURE_ICON && entity != null && currentUrl.isNotBlank())
        resolveEntityPictureUrl(entity, currentUrl) else null
    val iconSlug = if (effectiveSlug == ENTITY_PICTURE_ICON) defaultSlug else effectiveSlug
    val iconEffect = entity?.let {
        iconEffectFor(it, LocalIconAnimationsEnabled.current, badge.iconAnimation).forIconSlug(iconSlug)
    } ?: IconEffect.NONE
    // When configured, the badge shows an attribute value instead of the state (falling back to the
    // state if the attribute is missing), with an optional unit suffix.
    val attributeText = badge.stateAttribute
        ?.let { attr -> entity?.let { entityAttributeDisplay(it, attr) } }
        ?.let { appendUnit(it, badge.stateUnit) }
    // Timer mode: interpret the shown value (attribute if set, else the state) as a completion
    // timestamp and render a live countdown. Falls back to the raw value if it isn't a timestamp.
    val timerSource = if (badge.stateAsTimer) {
        badge.stateAttribute?.let { attr -> entity?.let { entityAttributeDisplay(it, attr) } } ?: entity?.state
    } else null
    // Some integrations keep a stale future finish time while the appliance is off, so gate on the
    // optional machine-state entity: if it isn't running, the timer reads "Off" regardless of the time.
    val timerMachineRunning = badge.timerStateEntityId
        ?.let { id -> isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true
    val rawTimer = if (badge.stateAsTimer && timerMachineRunning) rememberCountdownText(timerSource) else null
    // While counting show the countdown; once finished/off (or the value isn't a timestamp) show "Off".
    val timerText = if (badge.stateAsTimer) {
        rawTimer ?: stringResource(R.string.dlg_off)
    } else {
        null
    }

    // Outer Box: badge content + edit-mode overlays
    Box {
        // Background-derived depth gradient (two shades of the badge's own fill, no icon color).
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp,
                if (isEditMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else Color.Gray.copy(alpha = 0.25f)
            ),
            modifier = sizeMod
                .then(
                    if (isEditMode) Modifier.background(colors.background.copy(alpha = 0.65f), shape)
                    else Modifier.background(surfaceGradient(colors.background), shape)
                )
                .then(
                    if (isEditMode) Modifier
                    else Modifier.combinedClickable(
                        onClick = onTap,
                        onLongClick = onHold
                    )
                )
        ) {
            if (isCircle) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    WithIconEffect(entity, iconEffect, glowColor = colors.icon) { fx ->
                        when {
                            pictureUrl != null -> AsyncImage(model = pictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = fx.size(24.dp).clip(CircleShape))
                            iconSlug != null -> MdiIcon(iconSlug, tint = colors.icon, size = 18.dp, modifier = fx)
                            else -> Icon(fallbackIcon, null, tint = colors.icon, modifier = fx.size(18.dp))
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    if (badge.showIcon) {
                        WithIconEffect(entity, iconEffect, glowColor = colors.icon) { fx ->
                            when {
                                pictureUrl != null -> AsyncImage(model = pictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = fx.size(22.dp).clip(CircleShape))
                                iconSlug != null -> MdiIcon(iconSlug, tint = colors.icon, size = 16.dp, modifier = fx)
                                else -> Icon(fallbackIcon, null, tint = colors.icon, modifier = fx.size(16.dp))
                            }
                        }
                    }
                    val badgeName = badge.customName?.takeIf { it.isNotBlank() } ?: entity?.friendlyName
                    val showTwoLine = badge.showName && badge.showState && entity != null && badgeName != null
                    if (showTwoLine) {
                        if (badge.showIcon) Spacer(Modifier.width(4.dp))
                        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.height(34.dp)) {
                            Text(
                                badgeName!!,
                                color = colors.content,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                timerText ?: if (isDoorOpen) stringResource(R.string.dlg_open) else (attributeText ?: formatBadgeState(entity)),
                                color = colors.content,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        if (badge.showName && badgeName != null) {
                            if (badge.showIcon) Spacer(Modifier.width(4.dp))
                            Text(
                                badgeName,
                                color = colors.content,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (badge.showState && entity != null) {
                            val stateText = timerText ?: if (isDoorOpen) stringResource(R.string.dlg_open) else (attributeText ?: formatBadgeState(entity))
                            if ((badge.showIcon || badge.showName) && stateText.isNotEmpty()) Spacer(Modifier.width(4.dp))
                            Text(
                                stateText,
                                color = colors.content,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── edit mode overlays ────────────────────────────────────────────────
        if (isEditMode) {
            // Settings cog overlay (covers the whole badge, like person avatar edit)
            EditSettingsButton(
                onClick = onHold,
                modifier = Modifier.align(Alignment.Center)
            )
            // X remove button at top-right corner
            EditRemoveBadge(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Domain-aware badge state colors  (mirrors EntityCard / button-stack logic)
// ─────────────────────────────────────────────────────────────────────────────

private data class BadgeColors(
    val background: Color,
    val content: Color,
    val icon: Color
)

private val CoverGreen  = Color(0xFF4CAF50)
private val CoverOrange = Color(0xFFFF8C00)
private val CoverRed    = Color(0xFFE53935)

@Composable
private fun badgeStateColors(badge: HKIBadge, entities: List<HAEntity>, allEntities: List<HAEntity>): BadgeColors {
    val appColors = LocalHKIAppColors.current
    val offBg = appColors.elevated
    val offFg = appColors.onMuted
    val activeBg = MaterialTheme.colorScheme.primary
    val activeFg = MaterialTheme.colorScheme.onPrimary
    val defaultActive = BadgeColors(activeBg, activeFg, activeFg)
    if (entities.isEmpty()) return BadgeColors(offBg, offFg, offFg)

    // Timer mode: active while the completion timestamp is in the future, off once it passes. The
    // ticking countdown recomposes the badge every second, so this flips to off exactly at zero.
    if (badge.stateAsTimer) {
        val value = badge.stateAttribute?.let { attr -> entities.first().let { entityAttributeDisplay(it, attr) } } ?: entities.first().state
        val machineRunning = badge.timerStateEntityId
            ?.let { id -> isMachineRunning(allEntities.find { it.entity_id == id }?.state) } ?: true
        val running = machineRunning && parseTimestampToInstant(value)?.isAfter(java.time.Instant.now()) == true
        return if (running) defaultActive else BadgeColors(offBg, offFg, offFg)
    }

    // ── Multi-entity lock: aggregate with per-lock door sensors (worst state wins) ──
    if (entities.size > 1 && entities.all { it.entity_id.startsWith("lock.") }) {
        val worst = entities.maxOfOrNull { e ->
            val doorOpen = badge.doorEntityIdFor(e.entity_id)?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
            when { doorOpen || e.state == "open" -> 3; e.state == "unlocked" -> 2; else -> 0 }
        } ?: 0
        return when {
            worst >= 3 -> BadgeColors(activeBg, activeFg, CoverRed)
            worst >= 2 -> BadgeColors(activeBg, activeFg, CoverOrange)
            else       -> BadgeColors(offBg, offFg, CoverGreen)
        }
    }

    // ── Multi-entity cover, all door-like by device_class: aggregate door colors ──
    if (entities.size > 1 && entities.all { it.entity_id.startsWith("cover.") } &&
        entities.all { isCoverDoorLike(it) }) {
        val score = entities.filter { it.state.lowercase() != "unavailable" }.maxOfOrNull { e ->
            when (e.state.lowercase()) { "open" -> 3; "opening", "closing", "stopped" -> 2; else -> 0 }
        }
        return when (score) {
            null -> BadgeColors(offBg, offFg, offFg)
            3    -> BadgeColors(activeBg, activeFg, coverDoorColor("open"))
            2    -> BadgeColors(activeBg, activeFg, coverDoorColor("opening"))
            else -> BadgeColors(offBg, offFg, coverDoorColor("closed"))
        }
    }

    // ── Generic multi-entity: active if any member is "on"/attention-worthy ──
    if (entities.size > 1) {
        val anyActive = entities.any { e ->
            val s = e.state.lowercase()
            when (e.entity_id.substringBefore(".")) {
                "lock"    -> s != "locked"
                "cover"   -> s != "closed" && s != "unavailable"
                "climate" -> s != "off"
                "person"  -> s == "home"
                else      -> s == "on"
            }
        }
        return if (anyActive) defaultActive else BadgeColors(offBg, offFg, offFg)
    }

    // ── Single entity ──
    val entity = entities.first()
    val domain = entity.entity_id.substringBefore(".")
    val state  = entity.state.lowercase()

    return when (domain) {
        "light" -> {
            if (state == "on") BadgeColors(activeBg, activeFg, lightStateColor(entity) ?: activeFg)
            else BadgeColors(offBg, offFg, offFg)
        }
        "climate" -> {
            val mode = entity.attributes?.get("hvac_action")?.jsonPrimitive?.contentOrNull
                ?: entity.attributes?.get("hvac_mode")?.jsonPrimitive?.contentOrNull
                ?: state
            if (state == "off" || mode == "off") BadgeColors(offBg, offFg, offFg)
            else BadgeColors(activeBg, activeFg, hvacColor(mode))
        }
        "lock" -> {
            val doorOpen = badge.doorEntityIdFor(entity.entity_id)?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true
            when {
                doorOpen || state == "open" -> BadgeColors(activeBg, activeFg, CoverRed)
                state == "unlocked"         -> BadgeColors(activeBg, activeFg, CoverOrange)
                state == "locked"           -> BadgeColors(offBg, offFg, CoverGreen)
                else                        -> BadgeColors(offBg, offFg, offFg)
            }
        }
        "cover" -> {
            if (isCoverDoorLike(entity)) {
                val doorCol = coverDoorColor(state)
                when (state) {
                    "unavailable" -> BadgeColors(offBg, offFg, offFg)
                    "closed"      -> BadgeColors(offBg, offFg, doorCol)
                    else          -> BadgeColors(activeBg, activeFg, doorCol)
                }
            } else if (state != "closed" && state != "unavailable") defaultActive
            else BadgeColors(offBg, offFg, offFg)
        }
        "vacuum" -> when (state) {
            "cleaning" -> BadgeColors(activeBg, activeFg, Color(0xFF66BB6A))
            "returning", "paused" -> BadgeColors(activeBg, activeFg, Color(0xFFFFB300))
            "error" -> BadgeColors(activeBg, activeFg, CoverRed)
            "docked" -> BadgeColors(offBg, offFg, offFg)
            else -> BadgeColors(offBg, offFg, offFg)
        }
        "switch", "input_boolean", "automation", "media_player" ->
            if (state == "on") defaultActive else BadgeColors(offBg, offFg, offFg)
        "fan" ->
            if (state == "on") BadgeColors(activeBg, activeFg, FanBlue) else BadgeColors(offBg, offFg, offFg)
        "humidifier" ->
            if (state == "on") BadgeColors(activeBg, activeFg, HumidifierCyan) else BadgeColors(offBg, offFg, offFg)
        "alarm_control_panel" ->
            if (state == "disarmed") BadgeColors(offBg, offFg, alarmStateColor(state)) else BadgeColors(activeBg, activeFg, alarmStateColor(state))
        "binary_sensor" ->
            if (state == "on") defaultActive else BadgeColors(offBg, offFg, offFg)
        "person" ->
            if (state == "home") defaultActive else BadgeColors(offBg, offFg, offFg)
        else ->
            if (state == "on") defaultActive else BadgeColors(offBg, offFg, offFg)
    }
}

internal fun domainIcon(entity: HAEntity) = when {
    entity.entity_id.startsWith("light.")   -> Icons.Default.Lightbulb
    entity.entity_id.startsWith("climate.") -> Icons.Default.Thermostat
    entity.entity_id.startsWith("lock.")    -> if (entity.state == "locked") Icons.Default.Lock else Icons.Default.LockOpen
    entity.entity_id.startsWith("cover.")   -> Icons.Default.Window
    entity.entity_id.startsWith("camera.")  -> Icons.Default.CameraAlt
    entity.entity_id.startsWith("vacuum.")  -> Icons.Default.CleaningServices
    entity.entity_id.startsWith("switch.")  -> Icons.Default.ToggleOn
    entity.entity_id.startsWith("sensor.")  -> Icons.Default.Sensors
    entity.entity_id.startsWith("binary_sensor.") -> Icons.Default.RadioButtonChecked
    entity.entity_id.startsWith("person.")  -> Icons.Default.Person
    entity.entity_id.startsWith("fan.")     -> Icons.Default.Air
    entity.entity_id.startsWith("humidifier.") -> Icons.Default.WaterDrop
    entity.entity_id.startsWith("alarm_control_panel.") -> Icons.Default.Security
    else                                    -> Icons.Default.Power
}

@Composable
private fun formatBadgeState(entity: HAEntity): String {
    return when {
        entity.entity_id.startsWith("climate.") -> {
            val temp = entity.attributes
                ?.get("current_temperature")
                ?.let { runCatching { kotlinx.serialization.json.JsonPrimitive(it.toString()).content }.getOrNull() }
                ?: entity.temperature?.toString()
            if (temp != null) "${temp}°C" else localizedEntityStateLabel(entity.state)
        }
        entity.entity_id.startsWith("sensor.") -> {
            val unit = entity.attributes
                ?.get("unit_of_measurement")
                ?.let { runCatching { it.toString().trim('"') }.getOrNull() } ?: ""
            "${entity.state}$unit"
        }
        entity.entity_id.startsWith("binary_sensor.") -> entity.localizedStateLabel()
        else -> localizedEntityStateLabel(entity.state).take(16)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add-badge pill (dashed outline)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddBadgePill(
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val shape = itemCornerShape()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.dp, borderColor), CircleShape)
                .clickable(onClick = onClick)
        ) {
            Icon(
                Icons.Default.Add,
                null,
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }
        if (label != null) {
            Surface(
                onClick = onClick,
                shape = shape,
                color = Color.Transparent,
                modifier = Modifier
                    .height(36.dp)
                    .border(BorderStroke(1.5.dp, borderColor), shape)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(label, color = borderColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-badge settings dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeSettingsDialog(
    badge: HKIBadge,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    showSidePicker: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (HKIBadge) -> Unit,
    onRemove: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val aestheticsOnlyBadge by viewModel.aestheticsOnlyEditing.collectAsState()
    var settingsPage by remember { mutableStateOf(if (aestheticsOnlyBadge) "appearance" else "entities") }

    var shape       by remember { mutableStateOf(badge.shape) }
    var side        by remember { mutableStateOf(badge.side) }
    var showName    by remember { mutableStateOf(badge.showName) }
    var customName  by remember { mutableStateOf(badge.customName ?: "") }
    var visSpec by remember { mutableStateOf(badge.toVisibilitySpec()) }
    var showState   by remember { mutableStateOf(badge.showState) }
    var stateAttribute by remember { mutableStateOf(badge.stateAttribute) }
    var stateUnit by remember { mutableStateOf(badge.stateUnit) }
    var stateAsTimer by remember { mutableStateOf(badge.stateAsTimer) }
    var timerStateEntityId by remember { mutableStateOf(badge.timerStateEntityId) }
    var showTimerStatePickerBadge by remember { mutableStateOf(false) }
    var showIcon    by remember { mutableStateOf(badge.showIcon) }
    var customIcon  by remember { mutableStateOf(badge.customIcon ?: "") }
    var iconAnimation by remember { mutableStateOf(badge.iconAnimation) }
    // Badge "auto" preserves its dialog-first default, mapped to more_info for the structured editor.
    var tapAction   by remember { mutableStateOf(badge.tapActionEx ?: HKIAction(type = if (badge.tapAction == "auto") "more_info" else badge.tapAction)) }
    var holdAction  by remember { mutableStateOf(badge.holdActionEx ?: HKIAction(type = if (badge.holdAction == "auto") "more_info" else badge.holdAction)) }
    var customButtons by remember { mutableStateOf(badge.customButtons) }
    var climateDialogControl by remember { mutableStateOf(badge.climateDialogControl) }
    var humidifierFanEntityId by remember { mutableStateOf(badge.humidifierFanEntityId) }
    var humidifierDeviceId by remember { mutableStateOf(badge.humidifierDeviceId) }
    var humidifierAuxEntityIds by remember { mutableStateOf(badge.humidifierAuxEntityIds) }
    val areas by viewModel.areas.collectAsState()
    var editingEntityIds by remember { mutableStateOf(badge.effectiveEntityIds) }
    // Per-entity settings
    var doorEntityIds by remember { mutableStateOf(badge.doorEntityIds) }
    var vacuumMapIds  by remember { mutableStateOf(badge.vacuumMapEntityIds) }
    var vacuumBattIds by remember { mutableStateOf(badge.vacuumBatteryEntityIds) }
    var vacuumDeviceIds by remember { mutableStateOf(badge.vacuumDeviceIds) }
    var vacuumWaterIds by remember { mutableStateOf(badge.vacuumWaterEntityIds) }
    var vacuumEmptyIds by remember { mutableStateOf(badge.vacuumEmptyBinEntityIds) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPickerBadge by remember { mutableStateOf(false) }
    var doorPickerForLock by remember { mutableStateOf<String?>(null) }
    var vacuumMapPickerFor by remember { mutableStateOf<String?>(null) }
    var vacuumBattPickerFor by remember { mutableStateOf<String?>(null) }
    var vacuumDevicePickerFor by remember { mutableStateOf<String?>(null) }
    var vacuumWaterPickerFor by remember { mutableStateOf<String?>(null) }
    var vacuumEmptyPickerFor by remember { mutableStateOf<String?>(null) }
    val devices by viewModel.deviceRegistry.collectAsState()
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    val lockIds   = editingEntityIds.filter { it.startsWith("lock.") }
    val vacuumIds = editingEntityIds.filter { it.startsWith("vacuum.") }
    val climateIds = editingEntityIds.filter { it.startsWith("climate.") }
    val humidifierIds = editingEntityIds.filter { it.startsWith("humidifier.") }

    fun nameOf(id: String) = allEntities.find { it.entity_id == id }?.friendlyName ?: id

    if (showIconPickerBadge) {
        MdiIconPickerDialog(
            current = customIcon,
            onDismiss = { showIconPickerBadge = false },
            onSelect = { customIcon = it; showIconPickerBadge = false },
            allowEntityPicture = true
        )
    }

    ModernSettingsDialogFrame(
        title = stringResource(R.string.dlg_header_pill),
        subtitle = stringResource(R.string.dlg_entities_appearance_and_interactions),
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Aesthetic-only editors get just the Appearance tab (name, icon, visibility); entity
                // bindings and interactions are structural and stay locked.
                // Visibility is its own tab (and always last) so badges match every other item type.
                val badgeTabs = if (aestheticsOnlyBadge) {
                    listOf(
                        "appearance" to stringResource(R.string.uif_appearance),
                        "visibility" to stringResource(R.string.dlg_visibility),
                    )
                } else {
                    listOf(
                        "entities" to stringResource(R.string.uif_entities),
                        "appearance" to stringResource(R.string.uif_appearance),
                        "actions" to stringResource(R.string.uif_actions),
                        "visibility" to stringResource(R.string.dlg_visibility),
                    )
                }
                LaunchedEffect(aestheticsOnlyBadge) { if (aestheticsOnlyBadge) settingsPage = "appearance" }
                SettingsTabRow(
                    tabs = badgeTabs,
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "entities") {
                SettingsSubcategory(stringResource(R.string.dlg_entities), stringResource(R.string.dlg_choose_what_this_badge_summarizes))
                // Entities (multi-select)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dlg_entities_count, editingEntityIds.size), style = MaterialTheme.typography.labelLarge)
                        Text(
                            editingEntityIds.joinToString(", ") { nameOf(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.onMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.dlg_change)) }
                }

                // Aggregated badges summarize several entities; the first one drives the badge's
                // icon, its state text, and the top of the pop-up list. Let the user reorder them.
                if (editingEntityIds.size > 1) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text(stringResource(R.string.dlg_display_order), style = MaterialTheme.typography.labelLarge)
                    editingEntityIds.forEachIndexed { index, id ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.dlg_sentence_pair, index + 1, nameOf(id)),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    editingEntityIds = editingEntityIds.toMutableList()
                                        .apply { add(index - 1, removeAt(index)) }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.dlg_move_up)) }
                            IconButton(
                                enabled = index < editingEntityIds.lastIndex,
                                onClick = {
                                    editingEntityIds = editingEntityIds.toMutableList()
                                        .apply { add(index + 1, removeAt(index)) }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.dlg_move_down)) }
                        }
                    }
                }

                if (lockIds.isNotEmpty() || vacuumIds.isNotEmpty() || climateIds.isNotEmpty()) {
                    SettingsSubcategory(stringResource(R.string.dlg_entity_integrations), stringResource(R.string.dlg_optional_controls_and_sensors_for_richer_dialogs))
                }

                // Per-lock door sensors
                if (lockIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text(stringResource(R.string.dlg_door_sensors), style = MaterialTheme.typography.labelLarge)
                    lockIds.forEach { lockId ->
                        val sensor = doorEntityIds[lockId]
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(nameOf(lockId), style = MaterialTheme.typography.bodySmall, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sensor?.let { nameOf(it) } ?: stringResource(R.string.dlg_no_door_sensor), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                            }
                            TextButton(onClick = { doorPickerForLock = lockId }) { Text(stringResource(R.string.dlg_set)) }
                            if (sensor != null) TextButton(onClick = { doorEntityIds = doorEntityIds - lockId }) { Text(stringResource(R.string.dlg_clear)) }
                        }
                    }
                }

                // Per-vacuum map + battery
                if (vacuumIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text(stringResource(R.string.dlg_vacuum_entities), style = MaterialTheme.typography.labelLarge)
                    vacuumIds.forEach { vId ->
                        Text(nameOf(vId), style = MaterialTheme.typography.bodySmall, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val deviceName = vacuumDeviceIds[vId]?.let { id -> devices.find { it.id == id }?.let { it.name_by_user ?: it.name } ?: id }
                            Text(stringResource(R.string.dlg_device_value, deviceName ?: stringResource(R.string.dlg_auto_none)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumDevicePickerFor = vId }) { Text(stringResource(R.string.dlg_device)) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dlg_map_value, vacuumMapIds[vId]?.let { nameOf(it) } ?: stringResource(R.string.dlg_none)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumMapPickerFor = vId }) { Text(stringResource(R.string.dlg_map)) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dlg_water_value, vacuumWaterIds[vId]?.let { nameOf(it) } ?: stringResource(R.string.dlg_auto)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumWaterPickerFor = vId }) { Text(stringResource(R.string.dlg_water)) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dlg_empty_bin, vacuumEmptyIds[vId]?.let { nameOf(it) } ?: stringResource(R.string.dlg_auto)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumEmptyPickerFor = vId }) { Text(stringResource(R.string.dlg_set)) }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dlg_battery_value, vacuumBattIds[vId]?.let { nameOf(it) } ?: stringResource(R.string.dlg_built_in)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumBattPickerFor = vId }) { Text(stringResource(R.string.dlg_batt)) }
                        }
                    }
                }

                if (climateIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text(stringResource(R.string.dlg_climate_dialog_control), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = climateDialogControl != "dial",
                            onClick = { climateDialogControl = "slider" },
                            label = { Text(stringResource(R.string.dlg_vertical_slider)) }
                        )
                        FilterChip(
                            selected = climateDialogControl == "dial",
                            onClick = { climateDialogControl = "dial" },
                            label = { Text(stringResource(R.string.dlg_thermostat_dial)) }
                        )
                    }
                }

                if (humidifierIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    HumidifierIntegrationSettings(
                        deviceId = humidifierDeviceId,
                        fanEntityId = humidifierFanEntityId,
                        auxEntityIds = humidifierAuxEntityIds,
                        allEntities = allEntities,
                        devices = devices,
                        entityRegistry = entityRegistry,
                        onChange = { dev, fan, aux ->
                            humidifierDeviceId = dev
                            humidifierFanEntityId = fan
                            humidifierAuxEntityIds = aux
                        }
                    )
                }

                }

                // Shape
                if (settingsPage == "appearance") {
                SettingsSubcategory(stringResource(R.string.dlg_appearance), stringResource(R.string.dlg_shape_visible_information_and_icon_behavior))
                Text(stringResource(R.string.dlg_shape), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "pill" to stringResource(R.string.uif_pill),
                        "circle" to stringResource(R.string.uif_circle),
                    ).forEach { (value, label) ->
                        FilterChip(selected = shape == value, onClick = { shape = value }, label = { Text(label) })
                    }
                }

                // Display (only meaningful for pill)
                if (shape == "pill") {
                    Text(stringResource(R.string.dlg_display), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = showIcon,  onClick = { showIcon = !showIcon },   label = { Text(stringResource(R.string.dlg_icon)) })
                        FilterChip(selected = showName,  onClick = { showName = !showName },   label = { Text(stringResource(R.string.dlg_name)) })
                        FilterChip(selected = showState, onClick = { showState = !showState }, label = { Text(stringResource(R.string.dlg_state)) })
                    }
                    if (showState) {
                        val attributes = remember(editingEntityIds, allEntities) {
                            selectableEntityAttributes(allEntities.find { it.entity_id == editingEntityIds.firstOrNull() })
                        }
                        Text(stringResource(R.string.dlg_state_text), style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(R.string.dlg_show_the_entity_s_state_or_one_of_its),
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.onMuted
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = stateAttribute == null,
                                onClick = { stateAttribute = null },
                                label = { Text(stringResource(R.string.dlg_state)) }
                            )
                            attributes.forEach { attr ->
                                FilterChip(
                                    selected = stateAttribute == attr,
                                    onClick = { stateAttribute = attr },
                                    label = { Text(attr.replace('_', ' ')) }
                                )
                            }
                        }
                        if (stateAttribute != null) {
                            Text(stringResource(R.string.dlg_unit), style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                COMMON_STATE_UNITS.forEach { unit ->
                                    FilterChip(
                                        selected = (stateUnit ?: "") == unit,
                                        onClick = { stateUnit = unit.ifBlank { null } },
                                        label = { Text(if (unit.isBlank()) stringResource(R.string.dlg_none) else unit) }
                                    )
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.dlg_countdown_timer), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    stringResource(R.string.dlg_show_a_descending_timer_when_the_value_is_a),
                                    style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                                )
                            }
                            Switch(checked = stateAsTimer, onCheckedChange = {
                                stateAsTimer = it
                                if (it && timerStateEntityId == null) {
                                    timerStateEntityId = com.jimz011apps.hki7.ui.components.guessMachineStateEntityId(editingEntityIds.firstOrNull(), allEntities, entityRegistry)
                                }
                            })
                        }
                        if (stateAsTimer) {
                            val stateName = timerStateEntityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.dlg_running_state_entity), style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        stateName ?: stringResource(R.string.dlg_none_timer_follows_the_completion_time_only),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (stateName != null) MaterialTheme.colorScheme.primary else appColors.onMuted
                                    )
                                }
                                TextButton(onClick = { showTimerStatePickerBadge = true }) { Text(stringResource(R.string.dlg_change)) }
                                if (timerStateEntityId != null) TextButton(onClick = { timerStateEntityId = null }) { Text(stringResource(R.string.dlg_clear)) }
                            }
                        }
                    }
                }

                // Custom icon
                Text(stringResource(R.string.dlg_icon), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (customIcon.isNotEmpty()) {
                        MdiIcon(customIcon, size = 24.dp)
                    }
                    Text(
                        customIcon.ifEmpty { stringResource(R.string.dlg_auto) },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.onSurface
                    )
                    TextButton(onClick = { showIconPickerBadge = true }) { Text(stringResource(R.string.dlg_change)) }
                }
                Text(stringResource(R.string.dlg_icon_animation), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "auto" to stringResource(R.string.uif_auto),
                        "off" to stringResource(R.string.uif_off),
                        "glow" to stringResource(R.string.uif_glow),
                        "spin" to stringResource(R.string.uif_spin),
                        "pulse" to stringResource(R.string.uif_pulse),
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = iconAnimation == value,
                            onClick = { iconAnimation = value },
                            label = { Text(label) }
                        )
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                SettingsSubcategory(stringResource(R.string.dlg_name), stringResource(R.string.dlg_optional_label_shown_when_name_is_enabled_above))
                androidx.compose.material3.OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dlg_custom_name)) },
                    placeholder = { Text(editingEntityIds.firstOrNull()?.let { nameOf(it) } ?: stringResource(R.string.dlg_entity_name)) },
                    modifier = Modifier.fillMaxWidth()
                )


                // Side (only in split mode)
                if (showSidePicker) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    SettingsSubcategory(stringResource(R.string.dlg_placement), stringResource(R.string.dlg_choose_a_side_when_the_badge_bar_is_split))
                    Text(stringResource(R.string.dlg_side_split_alignment), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = side == "left",  onClick = { side = "left" },  label = { Text(stringResource(R.string.dlg_left)) })
                        FilterChip(selected = side == "right", onClick = { side = "right" }, label = { Text(stringResource(R.string.dlg_right)) })
                    }
                }

                }

                // Tap / Hold actions + custom nav-bar buttons for the badge's dialog.
                if (settingsPage == "actions") {
                SettingsSubcategory(stringResource(R.string.dlg_interactions), stringResource(R.string.dlg_tap_hold_and_dialog_quick_actions))
                ActionEditor(stringResource(R.string.uif_tap), tapAction, allEntities, areas, viewModel) { tapAction = it }
                ActionEditor(stringResource(R.string.uif_hold), holdAction, allEntities, areas, viewModel) { holdAction = it }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                CustomButtonsEditor(customButtons, allEntities, areas, viewModel) { customButtons = it }
                }

                if (settingsPage == "visibility") {
                SettingsSubcategory(stringResource(R.string.dlg_visibility), stringResource(R.string.dlg_hide_this_badge_or_schedule_when_it_appears))
                VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        footer = {
            if (!aestheticsOnlyBadge) TextButton(onClick = onRemove) { Text(stringResource(R.string.dlg_remove), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_cancel)) }
            Button(onClick = {
                val primary = editingEntityIds.firstOrNull() ?: badge.entityId
                onSave(badge.copy(
                    entityId   = primary,
                    entityIds  = if (editingEntityIds.size > 1) editingEntityIds else emptyList(),
                    shape      = shape,
                    side       = side,
                    showName   = showName,
                    customName = customName.trim().ifBlank { null },
                    hidden     = visSpec.hidden,
                    visibilityConditions = visSpec.conditions,
                    visibilityMatch = visSpec.match,
                    // Clear the superseded flat fields so the rule lives only in the block list.
                    visibilityStart = null,
                    visibilityEnd = null,
                    visibilityRangeMode = "show",
                    visibilityRecurrence = "none",
                    visibilityConditionEntityId = null,
                    visibilityConditionState = null,
                    visibilityConditionNegate = false,
                    showState  = showState,
                    stateAttribute = stateAttribute,
                    stateUnit  = stateUnit,
                    stateAsTimer = stateAsTimer,
                    timerStateEntityId = timerStateEntityId,
                    showIcon   = showIcon,
                    customIcon = customIcon.ifBlank { null },
                    iconAnimation = iconAnimation,
                    tapActionEx = tapAction,
                    holdActionEx = holdAction,
                    customButtons = customButtons,
                    climateDialogControl = climateDialogControl,
                    doorEntityId = null,
                    doorEntityIds = doorEntityIds.filterKeys { it in lockIds },
                    vacuumMapEntityIds = vacuumMapIds.filterKeys { it in vacuumIds },
                    vacuumBatteryEntityIds = vacuumBattIds.filterKeys { it in vacuumIds },
                    vacuumDeviceIds = vacuumDeviceIds.filterKeys { it in vacuumIds },
                    vacuumWaterEntityIds = vacuumWaterIds.filterKeys { it in vacuumIds },
                    vacuumEmptyBinEntityIds = vacuumEmptyIds.filterKeys { it in vacuumIds },
                    humidifierFanEntityId = if (humidifierIds.isNotEmpty()) humidifierFanEntityId else null,
                    humidifierDeviceId = if (humidifierIds.isNotEmpty()) humidifierDeviceId else null,
                    humidifierAuxEntityIds = if (humidifierIds.isNotEmpty()) humidifierAuxEntityIds else emptyMap()
                ))
            }) { Text(stringResource(R.string.dlg_save)) }
        }
    )

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            title = stringResource(R.string.dlg_select_entities),
            singleSelect = false,
            preselectedIds = editingEntityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids ->
                if (ids.isNotEmpty()) editingEntityIds = ids
                showEntityPicker = false
            }
        )
    }

    if (showTimerStatePickerBadge) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            title = stringResource(R.string.dlg_select_running_state_entity),
            singleSelect = true,
            preselectedIds = setOfNotNull(timerStateEntityId?.takeIf { it.isNotBlank() }),
            onDismiss = { showTimerStatePickerBadge = false },
            onEntitiesSelected = { ids -> timerStateEntityId = ids.firstOrNull(); showTimerStatePickerBadge = false }
        )
    }

    doorPickerForLock?.let { lockId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("binary_sensor.") },
            title = stringResource(R.string.dlg_select_door_sensor),
            singleSelect = true,
            preselectedIds = setOfNotNull(doorEntityIds[lockId]),
            onDismiss = { doorPickerForLock = null },
            onEntitiesSelected = { ids ->
                val sel = ids.firstOrNull()
                doorEntityIds = if (sel != null) doorEntityIds + (lockId to sel) else doorEntityIds - lockId
                doorPickerForLock = null
            }
        )
    }

    vacuumMapPickerFor?.let { vId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("camera.") },
            title = stringResource(R.string.dlg_select_map_camera),
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumMapIds[vId]),
            onDismiss = { vacuumMapPickerFor = null },
            onEntitiesSelected = { ids ->
                val sel = ids.firstOrNull()
                vacuumMapIds = if (sel != null) vacuumMapIds + (vId to sel) else vacuumMapIds - vId
                vacuumMapPickerFor = null
            }
        )
    }

    vacuumBattPickerFor?.let { vId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("sensor.") },
            title = stringResource(R.string.dlg_select_battery_sensor),
            singleSelect = true,
            preselectedIds = setOfNotNull(vacuumBattIds[vId]),
            onDismiss = { vacuumBattPickerFor = null },
            onEntitiesSelected = { ids ->
                val sel = ids.firstOrNull()
                vacuumBattIds = if (sel != null) vacuumBattIds + (vId to sel) else vacuumBattIds - vId
                vacuumBattPickerFor = null
            }
        )
    }
    vacuumDevicePickerFor?.let { vId ->
        DevicePickerDialog(
            devices = devices, currentId = vacuumDeviceIds[vId],
            onDismiss = { vacuumDevicePickerFor = null },
            onSelected = { id ->
                vacuumDeviceIds = if (id != null) vacuumDeviceIds + (vId to id) else vacuumDeviceIds - vId
                if (id != null) {
                    // Auto-fill the helper entity fields from the device, like the Energy view does.
                    val auto = resolveVacuumDeviceEntities(id, allEntities, entityRegistry)
                    auto.map?.let { vacuumMapIds = vacuumMapIds + (vId to it.entity_id) }
                    auto.battery?.let { vacuumBattIds = vacuumBattIds + (vId to it.entity_id) }
                    auto.water?.let { vacuumWaterIds = vacuumWaterIds + (vId to it.entity_id) }
                    auto.emptyBin?.let { vacuumEmptyIds = vacuumEmptyIds + (vId to it.entity_id) }
                }
                vacuumDevicePickerFor = null
            }
        )
    }
    vacuumWaterPickerFor?.let { vId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("select.") || it.entity_id.startsWith("input_select.") },
            title = stringResource(R.string.dlg_select_water_level), singleSelect = true, preselectedIds = setOfNotNull(vacuumWaterIds[vId]),
            onDismiss = { vacuumWaterPickerFor = null }, onEntitiesSelected = { ids -> vacuumWaterIds = ids.firstOrNull()?.let { vacuumWaterIds + (vId to it) } ?: (vacuumWaterIds - vId); vacuumWaterPickerFor = null }
        )
    }
    vacuumEmptyPickerFor?.let { vId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("button.") || it.entity_id.startsWith("switch.") },
            title = stringResource(R.string.dlg_select_empty_bin_control), singleSelect = true, preselectedIds = setOfNotNull(vacuumEmptyIds[vId]),
            onDismiss = { vacuumEmptyPickerFor = null }, onEntitiesSelected = { ids -> vacuumEmptyIds = ids.firstOrNull()?.let { vacuumEmptyIds + (vId to it) } ?: (vacuumEmptyIds - vId); vacuumEmptyPickerFor = null }
        )
    }
}

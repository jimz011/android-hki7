@file:Suppress("UnusedBoxWithConstraintsScope", "SpellCheckingInspection", "GrazieInspection")

package com.example.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIBadge
import com.example.hki7.data.HKIBadgeBarConfig
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.screens.PagedRoleDialog
import com.example.hki7.ui.screens.AggregatedCoverDialog
import com.example.hki7.ui.screens.VacuumStackDialog
import com.example.hki7.ui.screens.UniversalStackDialog
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Domain helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun defaultTapAction(entityId: String): String = when {
    entityId.startsWith("climate.")             ||
    entityId.startsWith("cover.")               ||
    entityId.startsWith("camera.")              ||
    entityId.startsWith("fan.")                 ||
    entityId.startsWith("humidifier.")          ||
    entityId.startsWith("alarm_control_panel.") ||
    entityId.startsWith("person.")              -> "more_info"
    else -> "toggle"
}

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
    modifier: Modifier = Modifier
) {
    val config = badgeBarConfig ?: HKIBadgeBarConfig()
    val badges = config.badges
    val alignment = config.alignment
    val dependencyIds = remember(badges) {
        buildSet {
            badges.forEach { badge ->
                addAll(badge.effectiveEntityIds)
                badge.doorEntityId?.let(::add)
                addAll(badge.doorEntityIds.values)
                addAll(badge.vacuumMapEntityIds.values)
                addAll(badge.vacuumBatteryEntityIds.values)
            }
        }.toList()
    }
    // Edit mode needs the complete list for entity pickers. Normal rendering observes only the
    // entities represented by these badges and their secondary sensors.
    val entityFlow = remember(viewModel, dependencyIds, isEditMode) {
        if (isEditMode) viewModel.entitiesMatching { true } else viewModel.entitiesFor(dependencyIds)
    }
    val allEntities by entityFlow.collectAsState()

    // ── dialog state ──────────────────────────────────────────────────────────
    var dialogRole    by remember { mutableStateOf<String?>(null) }
    var dialogList    by remember { mutableStateOf<List<HAEntity>>(emptyList()) }
    var dialogBadge   by remember { mutableStateOf<HKIBadge?>(null) }

    // ── edit-mode state ───────────────────────────────────────────────────────
    var showEntityPicker by remember { mutableStateOf(false) }
    var editingBadge     by remember { mutableStateOf<HKIBadge?>(null) }
    var pendingAddSide   by remember { mutableStateOf("right") }

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

    fun handleTap(badge: HKIBadge) {
        val primary = badge.effectiveEntityIds.firstOrNull() ?: badge.entityId
        val action = if (badge.tapAction == "auto") defaultTapAction(primary) else badge.tapAction
        if (action == "toggle") viewModel.toggleEntity(primary)
        else openMore(badge)
    }

    fun handleHold(badge: HKIBadge) {
        val primary = badge.effectiveEntityIds.firstOrNull() ?: badge.entityId
        val action = if (badge.holdAction == "auto") "more_info" else badge.holdAction
        if (action == "toggle") viewModel.toggleEntity(primary)
        else openMore(badge)
    }

    // ── nothing to show ───────────────────────────────────────────────────────
    if (!config.visible || (!isEditMode && badges.isEmpty())) return

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
                AddBadgePill { addBadge(if (alignment == "split") "left" else "right") }
            }

            alignment == "split" -> {
                val leftBadges  = badges.filter { it.side == "left" }
                val rightBadges = badges.filter { it.side == "right" }

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
                                    scrollState = leftScrollState,
                                    viewportWidthPx = leftLaneWidthPx,
                                    onTap = { b -> handleTap(b) },
                                    onHold = { b -> editingBadge = b },
                                    onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                                    onNewOrder = { ordered -> saveBadges(ordered + rightBadges) }
                                )
                                AddBadgePill { addBadge("left") }
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
                                AddBadgePill { addBadge("right") }
                                BadgeDraggableRow(
                                    badges = rightBadges,
                                    allEntities = allEntities,
                                    isEditMode = true,
                                    scrollState = rightScrollState,
                                    viewportWidthPx = rightLaneWidthPx,
                                    onTap = { b -> handleTap(b) },
                                    onHold = { b -> editingBadge = b },
                                    onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                                    onNewOrder = { ordered -> saveBadges(leftBadges + ordered) }
                                )
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
                // Non-split: badges fill the remaining width so Center/Right/SpaceEvenly work
                val hArrangement: Arrangement.Horizontal = when {
                    alignment == "left"       -> Arrangement.Start
                    alignment == "right"      -> Arrangement.End
                    config.spanIcons -> Arrangement.SpaceEvenly
                    else                      -> Arrangement.Center
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = hArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeDraggableRow(
                        badges = badges,
                        allEntities = allEntities,
                        isEditMode = isEditMode,
                        onTap    = { b -> handleTap(b) },
                        onHold   = { b -> if (isEditMode) editingBadge = b else handleHold(b) },
                        onRemove = { b -> saveBadges(badges.filter { it.id != b.id }) },
                        onNewOrder = { ordered -> saveBadges(ordered) }
                    )
                    if (isEditMode) {
                        Spacer(Modifier.width(8.dp))
                        AddBadgePill { addBadge("right") }
                    }
                }
            }
        }
    }

    // ── entity picker ─────────────────────────────────────────────────────────
    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { selectedIds ->
                val defaultSide = if (alignment == "split") pendingAddSide else "right"
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
            allEntities = allEntities,
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
        val currentUrl   by viewModel.currentUrl.collectAsState()
        val accessToken  by viewModel.accessToken.collectAsState()
        // Always resolve live copies so the dialog reflects real-time state
        val live = dialogList.map { e -> allEntities.find { it.entity_id == e.entity_id } ?: e }
        val de = live.first()
        val dismiss = { dialogRole = null; dialogList = emptyList(); dialogBadge = null }
        when (dr) {
            "light" -> {
                if (live.size == 1) {
                    HKILightDialog(
                        entity = de,
                        onDismiss = dismiss,
                        viewModel = viewModel,
                        iconName = badge?.customIcon,
                        spinIcon = badge?.spinIcon == true
                    )
                } else {
                    UniversalStackDialog(
                        entities = live, allEntities = allEntities, currentUrl = currentUrl,
                        buttonConfigs = live.associate { e ->
                            e.entity_id to HKIButtonConfig(icon = badge?.customIcon, spinIcon = badge?.spinIcon == true)
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
                live.associate { e -> e.entity_id to HKIButtonConfig(icon = badge?.customIcon, spinIcon = badge?.spinIcon == true) }
            )
            "lock" -> {
                val doorEntities = live.associate { e ->
                    e.entity_id to (badge?.doorEntityIdFor(e.entity_id)?.let { id -> allEntities.find { it.entity_id == id } })
                }
                HKILockDialog(
                    entity = de, entities = live, doorEntities = doorEntities,
                    onDismiss = dismiss, viewModel = viewModel,
                    iconNames = live.associate { it.entity_id to badge?.customIcon },
                    spinIcons = live.associate { it.entity_id to (badge?.spinIcon == true) }
                )
            }
            "cover" -> AggregatedCoverDialog(
                entities = live,
                viewModel = viewModel,
                onDismiss = dismiss,
                iconName = badge?.customIcon,
                spinIcon = badge?.spinIcon == true
            )
            "fan" -> HKIFanDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                spinIcon = badge?.spinIcon == true,
                onDismiss = dismiss
            )
            "humidifier" -> HKIHumidifierDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                spinIcon = badge?.spinIcon == true,
                onDismiss = dismiss
            )
            "alarm" -> HKIAlarmDialog(
                entity = de,
                viewModel = viewModel,
                iconName = badge?.customIcon,
                spinIcon = badge?.spinIcon == true,
                onDismiss = dismiss
            )
            "person" -> PersonDetailDialog(person = de, viewModel = viewModel, onDismiss = dismiss)
            "vacuum" -> {
                val buttonConfigs = live.associate { e ->
                    e.entity_id to HKIButtonConfig(
                        icon = badge?.customIcon,
                        spinIcon = badge?.spinIcon == true,
                        vacuumMapEntityId = badge?.vacuumMapEntityIds?.get(e.entity_id),
                        vacuumBatteryEntityId = badge?.vacuumBatteryEntityIds?.get(e.entity_id)
                    )
                }
                VacuumStackDialog(
                    entities = live, startIndex = 0, buttonConfigs = buttonConfigs,
                    allEntities = allEntities, currentUrl = currentUrl,
                    viewModel = viewModel, onDismiss = dismiss
                )
            }
            "camera" -> {
                val streamUrl = resolveEntityCameraUrl(de, currentUrl, preferLive = true)
                HKICameraDialog(
                    title = de.friendlyName ?: de.entity_id,
                    imageUrl = buildCameraRefreshModel(streamUrl, 0, 0),
                    refreshIntervalSeconds = 0,
                    liveWebUrl = streamUrl,
                    authToken = accessToken,
                    statusText = "Live",
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
                spinIcon = badge?.spinIcon == true,
                showHistoryButton = true
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(de.state.uppercase(), style = MaterialTheme.typography.headlineMedium, color = LocalHKIAppColors.current.onSurface)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    onTap: () -> Unit,
    onHold: () -> Unit,
    onRemove: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val isCircle = badge.shape == "circle"
    val shape    = if (isCircle) CircleShape else RoundedCornerShape(18.dp)
    val sizeMod  = if (isCircle) Modifier.size(36.dp) else Modifier.height(36.dp)
    // Display the most attention-worthy entity (e.g. the cover that is not closed)
    val entity = representativeBadgeEntity(entities)

    // Lock/cover door override: show "Open" text when a mapped door sensor is on
    val isDoorOpen = entity != null && entity.entity_id.startsWith("lock.") &&
        badge.doorEntityIdFor(entity.entity_id)
            ?.let { id -> allEntities.find { it.entity_id == id }?.state == "on" } == true

    val colors = badgeStateColors(badge, entities, allEntities)

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
    val spinIconModifier = Modifier.rotate(
        rememberIconSpinRotation(badge.spinIcon && entity != null && entity.state.lowercase() != "off")
    )

    // Outer Box: badge content + edit-mode overlays
    Box {
        Surface(
            shape = shape,
            color = colors.background.copy(alpha = if (isEditMode) 0.65f else 1f),
            border = BorderStroke(1.dp,
                if (isEditMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else Color.Gray.copy(alpha = 0.25f)
            ),
            modifier = sizeMod.then(
                if (isEditMode) Modifier
                else Modifier.combinedClickable(
                    onClick = onTap,
                    onLongClick = onHold
                )
            )
        ) {
            if (isCircle) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (effectiveSlug != null) MdiIcon(effectiveSlug, tint = colors.icon, size = 18.dp, modifier = spinIconModifier)
                    else Icon(fallbackIcon, null, tint = colors.icon, modifier = spinIconModifier.size(18.dp))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    if (badge.showIcon) {
                        if (effectiveSlug != null) MdiIcon(effectiveSlug, tint = colors.icon, size = 16.dp, modifier = spinIconModifier)
                        else Icon(fallbackIcon, null, tint = colors.icon, modifier = spinIconModifier.size(16.dp))
                    }
                    val showTwoLine = badge.showName && badge.showState && entity != null && entity.friendlyName != null
                    if (showTwoLine) {
                        if (badge.showIcon) Spacer(Modifier.width(4.dp))
                        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.height(34.dp)) {
                            Text(
                                entity.friendlyName!!,
                                color = colors.content,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isDoorOpen) "Open" else formatBadgeState(entity),
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
                        if (badge.showName && entity?.friendlyName != null) {
                            if (badge.showIcon) Spacer(Modifier.width(4.dp))
                            Text(
                                entity.friendlyName!!,
                                color = colors.content,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (badge.showState && entity != null) {
                            val stateText = if (isDoorOpen) "Open" else formatBadgeState(entity)
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
            IconButton(
                onClick = onHold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Badge settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
            }
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

private fun formatBadgeState(entity: HAEntity): String {
    return when {
        entity.entity_id.startsWith("climate.") -> {
            val temp = entity.attributes
                ?.get("current_temperature")
                ?.let { runCatching { kotlinx.serialization.json.JsonPrimitive(it.toString()).content }.getOrNull() }
                ?: entity.temperature?.toString()
            if (temp != null) "${temp}°C" else entity.state.uppercase()
        }
        entity.entity_id.startsWith("sensor.") -> {
            val unit = entity.attributes
                ?.get("unit_of_measurement")
                ?.let { runCatching { it.toString().trim('"') }.getOrNull() } ?: ""
            "${entity.state}$unit"
        }
        entity.state in listOf("on", "off", "locked", "unlocked", "open", "closed") ->
            entity.state.replaceFirstChar { it.uppercase() }
        else -> entity.state.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.take(16)
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
    val shape = RoundedCornerShape(18.dp)

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
    showSidePicker: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (HKIBadge) -> Unit,
    onRemove: () -> Unit
) {
    val appColors = LocalHKIAppColors.current

    var shape       by remember { mutableStateOf(badge.shape) }
    var side        by remember { mutableStateOf(badge.side) }
    var showName    by remember { mutableStateOf(badge.showName) }
    var showState   by remember { mutableStateOf(badge.showState) }
    var showIcon    by remember { mutableStateOf(badge.showIcon) }
    var customIcon  by remember { mutableStateOf(badge.customIcon ?: "") }
    var spinIcon    by remember { mutableStateOf(badge.spinIcon) }
    var tapAction   by remember { mutableStateOf(badge.tapAction) }
    var holdAction  by remember { mutableStateOf(badge.holdAction) }
    var editingEntityIds by remember { mutableStateOf(badge.effectiveEntityIds) }
    // Per-entity settings
    var doorEntityIds by remember { mutableStateOf(badge.doorEntityIds) }
    var vacuumMapIds  by remember { mutableStateOf(badge.vacuumMapEntityIds) }
    var vacuumBattIds by remember { mutableStateOf(badge.vacuumBatteryEntityIds) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPickerBadge by remember { mutableStateOf(false) }
    var doorPickerForLock by remember { mutableStateOf<String?>(null) }
    var vacuumMapPickerFor by remember { mutableStateOf<String?>(null) }
    var vacuumBattPickerFor by remember { mutableStateOf<String?>(null) }

    val lockIds   = editingEntityIds.filter { it.startsWith("lock.") }
    val vacuumIds = editingEntityIds.filter { it.startsWith("vacuum.") }

    fun nameOf(id: String) = allEntities.find { it.entity_id == id }?.friendlyName ?: id

    if (showIconPickerBadge) {
        MdiIconPickerDialog(
            current = customIcon,
            onDismiss = { showIconPickerBadge = false },
            onSelect = { customIcon = it; showIconPickerBadge = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Badge Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Entities (multi-select)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Entities (${editingEntityIds.size})", style = MaterialTheme.typography.labelLarge)
                        Text(
                            editingEntityIds.joinToString(", ") { nameOf(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.onMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text("Change") }
                }

                // Per-lock door sensors
                if (lockIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text("Door sensors", style = MaterialTheme.typography.labelLarge)
                    lockIds.forEach { lockId ->
                        val sensor = doorEntityIds[lockId]
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(nameOf(lockId), style = MaterialTheme.typography.bodySmall, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sensor?.let { nameOf(it) } ?: "No door sensor", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                            }
                            TextButton(onClick = { doorPickerForLock = lockId }) { Text("Set") }
                            if (sensor != null) TextButton(onClick = { doorEntityIds = doorEntityIds - lockId }) { Text("Clear") }
                        }
                    }
                }

                // Per-vacuum map + battery
                if (vacuumIds.isNotEmpty()) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text("Vacuum entities", style = MaterialTheme.typography.labelLarge)
                    vacuumIds.forEach { vId ->
                        Text(nameOf(vId), style = MaterialTheme.typography.bodySmall, color = appColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Map: ${vacuumMapIds[vId]?.let { nameOf(it) } ?: "None"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumMapPickerFor = vId }) { Text("Map") }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Battery: ${vacuumBattIds[vId]?.let { nameOf(it) } ?: "Built-in"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { vacuumBattPickerFor = vId }) { Text("Batt") }
                        }
                    }
                }

                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))

                // Shape
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pill" to "Pill", "circle" to "Circle").forEach { (value, label) ->
                        FilterChip(selected = shape == value, onClick = { shape = value }, label = { Text(label) })
                    }
                }

                // Display (only meaningful for pill)
                if (shape == "pill") {
                    Text("Display", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = showIcon,  onClick = { showIcon = !showIcon },   label = { Text("Icon") })
                        FilterChip(selected = showName,  onClick = { showName = !showName },   label = { Text("Name") })
                        FilterChip(selected = showState, onClick = { showState = !showState }, label = { Text("State") })
                    }
                }

                // Custom icon
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (customIcon.isNotEmpty()) {
                        MdiIcon(customIcon, size = 24.dp)
                    }
                    Text(
                        customIcon.ifEmpty { "Auto" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = appColors.onSurface
                    )
                    TextButton(onClick = { showIconPickerBadge = true }) { Text("Change") }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Spin icon", style = MaterialTheme.typography.labelLarge)
                        Text("Rotates continuously while the entity isn't off", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                    }
                    Switch(checked = spinIcon, onCheckedChange = { spinIcon = it })
                }

                // Side (only in split mode)
                if (showSidePicker) {
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                    Text("Side (split alignment)", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = side == "left",  onClick = { side = "left" },  label = { Text("Left") })
                        FilterChip(selected = side == "right", onClick = { side = "right" }, label = { Text("Right") })
                    }
                }

                // Tap / Hold actions
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.15f))
                Text("Tap action", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto", "toggle" to "Toggle", "more_info" to "Dialog").forEach { (v, l) ->
                        FilterChip(selected = tapAction == v, onClick = { tapAction = v }, label = { Text(l) })
                    }
                }
                Text("Hold action", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto", "toggle" to "Toggle", "more_info" to "Dialog").forEach { (v, l) ->
                        FilterChip(selected = holdAction == v, onClick = { holdAction = v }, label = { Text(l) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val primary = editingEntityIds.firstOrNull() ?: badge.entityId
                onSave(badge.copy(
                    entityId   = primary,
                    entityIds  = if (editingEntityIds.size > 1) editingEntityIds else emptyList(),
                    shape      = shape,
                    side       = side,
                    showName   = showName,
                    showState  = showState,
                    showIcon   = showIcon,
                    customIcon = customIcon.ifBlank { null },
                    spinIcon   = spinIcon,
                    tapAction  = tapAction,
                    holdAction = holdAction,
                    doorEntityId = null,
                    doorEntityIds = doorEntityIds.filterKeys { it in lockIds },
                    vacuumMapEntityIds = vacuumMapIds.filterKeys { it in vacuumIds },
                    vacuumBatteryEntityIds = vacuumBattIds.filterKeys { it in vacuumIds }
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities,
            title = "Select Entities",
            singleSelect = false,
            preselectedIds = editingEntityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids ->
                if (ids.isNotEmpty()) editingEntityIds = ids
                showEntityPicker = false
            }
        )
    }

    doorPickerForLock?.let { lockId ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("binary_sensor.") },
            title = "Select Door Sensor",
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
            title = "Select Map Camera",
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
            title = "Select Battery Sensor",
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
}

package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HALogbookEvent
import com.jimz011apps.hki7.data.HKINotification
import com.jimz011apps.hki7.data.HKINotificationAction
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** Opens the notification drawer from anywhere in the app (provided by MainApp). */
val LocalOpenNotifications = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Opens Settings at a named destination, for the few places that need to send someone straight to
 * a specific setting rather than to the settings menu.
 *
 * Keyed by a short route string rather than by the section enum because that enum lives in the
 * screens package, and the drawer is a component — the string keeps the dependency pointing one
 * way. Provided above the drawer (in MainApp), since the drawer sheet is a sibling of the page
 * and cannot see anything the page provides.
 */
val LocalOpenSettingsRoute = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Route for Settings › Family Sharing › Events. */
const val SETTINGS_ROUTE_FAMILY_EVENTS = "family_events"

/** Brief, inverted-theme banner for notifications received while the app is visible. */
@Composable
fun NotificationBannerHost(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val hostStartedAt = remember { System.currentTimeMillis() }
    var knownIds by remember { mutableStateOf<Set<String>?>(null) }
    var current by remember { mutableStateOf<HKINotification?>(null) }
    var visible by remember { mutableStateOf(false) }
    var exitMode by remember { mutableStateOf("auto") }

    LaunchedEffect(notifications) {
        val previous = knownIds
        if (previous == null) {
            knownIds = notifications.mapTo(mutableSetOf()) { it.id }
            return@LaunchedEffect
        }
        val incoming = notifications
            .filter { it.id !in previous && it.timestamp >= hostStartedAt }
            .maxByOrNull { it.timestamp }
        knownIds = notifications.mapTo(mutableSetOf()) { it.id }
        if (incoming != null) {
            current = incoming
            exitMode = "auto"
            visible = true
        }
    }

    LaunchedEffect(current?.id, visible) {
        if (current != null && visible) {
            // An actionable notification stays up longer: there are buttons to read and aim for.
            delay(if (current?.actions.isNullOrEmpty()) 5.seconds else 12.seconds)
            exitMode = "dismiss"
            visible = false
        }
    }

    val backgroundIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bannerBackground = if (backgroundIsLight) Color(0xFF211F24) else Color(0xFFF4F0F5)
    val bannerForeground = if (backgroundIsLight) Color(0xFFF7F2F8) else Color(0xFF211F24)
    val bannerMuted = bannerForeground.copy(alpha = 0.68f)
    val exit = when (exitMode) {
        "dismiss" -> slideOutHorizontally { -it }
        "delete" -> scaleOut(targetScale = 0.72f) + fadeOut()
        else -> slideOutVertically { -it } + fadeOut()
    }

    AnimatedVisibility(
        visible = visible && current != null,
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        enter = slideInVertically { -it },
        exit = exit
    ) {
        current?.let { notification ->
            Surface(
                shape = itemCornerShape(),
                color = bannerBackground,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = bannerForeground, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            notification.instanceName?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it.uppercase(),
                                    color = bannerMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            notification.title?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = bannerForeground, style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(notification.message, color = if (notification.title.isNullOrBlank()) bannerForeground else bannerMuted,
                                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = {
                            exitMode = "dismiss"
                            visible = false
                        }) {
                            Text(stringResource(R.string.ui_dismiss_70afe9e), color = bannerForeground)
                        }
                        IconButton(onClick = {
                            exitMode = "delete"
                            visible = false
                            viewModel.deleteNotification(notification.id)
                        }) {
                            Icon(Icons.Default.Close, stringResource(R.string.notification_delete), tint = bannerForeground)
                        }
                    }
                    // A reply needs a text field, which the banner has no room for — those hand off to
                    // the drawer instead of answering inline.
                    NotificationActionButtons(
                        notification = notification,
                        viewModel = viewModel,
                        accentColor = bannerForeground,
                        inlineReply = false,
                        modifier = Modifier.padding(start = 34.dp, top = 2.dp),
                        onActionFired = {
                            exitMode = "dismiss"
                            visible = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * The buttons from HA's `data.actions`. While HKI is visible no system notification is posted, so
 * this is the user's only way to reach them; tapping one fires the same
 * `mobile_app_notification_action` event the notification shade would.
 *
 * Only one action per notification can be fired — HA would accept a second, but a spent button in
 * a list that sticks around for 48h is a trap, not a feature.
 */
@Composable
private fun NotificationActionButtons(
    notification: HKINotification,
    viewModel: MainViewModel,
    accentColor: Color,
    inlineReply: Boolean,
    modifier: Modifier = Modifier,
    onActionFired: () -> Unit = {}
) {
    if (notification.actions.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    val openPanel = LocalOpenNotifications.current
    var replyingTo by remember(notification.id) { mutableStateOf<HKINotificationAction?>(null) }
    var replyText by remember(notification.id) { mutableStateOf("") }
    val spent = notification.firedAction != null

    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            notification.actions.forEach { action ->
                val fired = notification.firedAction == action.action
                TextButton(
                    // A link is repeatable; it changes nothing on the server.
                    enabled = action.isUri || !spent,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    onClick = {
                        when {
                            action.isUri -> {
                                action.uri?.let { runCatching { uriHandler.openUri(it) } }
                                onActionFired()
                            }
                            action.isReply && inlineReply -> replyingTo = action
                            action.isReply -> {
                                openPanel?.invoke()
                                onActionFired()
                            }
                            else -> {
                                viewModel.fireNotificationAction(notification, action)
                                onActionFired()
                            }
                        }
                    }
                ) {
                    Text(
                        action.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (fired) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (action.isUri || !spent) accentColor else accentColor.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        replyingTo?.let { action ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(action.title, style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                IconButton(
                    enabled = replyText.isNotBlank(),
                    onClick = {
                        viewModel.fireNotificationAction(notification, action, replyText.trim())
                        replyingTo = null
                        replyText = ""
                        onActionFired()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, action.title, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Round header tile matching the header-pill style: bell icon plus an unread-count badge in the
 * theme's primary color. Tapping opens the notification drawer. Hidden badge when nothing unread.
 */
@Composable
@Suppress("unused")
fun NotificationBellButton(
    viewModel: MainViewModel,
    pillColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val open = LocalOpenNotifications.current
    val notifications by viewModel.notifications.collectAsState()
    val unread = notifications.count { !it.read && !it.archived }
    Box(modifier) {
        // Plain Box, not IconButton: Material3's IconButton enforces a 48dp touch target that
        // would render larger than the 36dp header pills next to it.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(pillColor, CircleShape)
                .clip(CircleShape)
                .clickable { open?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.ui_notifications_753a22b), tint = iconTint, modifier = Modifier.size(18.dp))
        }
        if (unread > 0) {
            val label = if (unread > 99) "99+" else "$unread"
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-3).dp)
                    // Hard-pinned dimensions: the Text's intrinsic line height would otherwise
                    // inflate the box into an oval.
                    .then(
                        if (label.length == 1) Modifier.size(16.dp)
                        else Modifier.height(16.dp).widthIn(min = 16.dp)
                    )
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = if (label.length > 1) 4.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Contents of the left-edge swipe-in drawer: Home Assistant notification history with
 * Notifications/Events/Archived tabs, search, unread/history sections, swipe-left row actions
 * (mark unread / archive / delete) and mark-all-read. Non-archived entries expire after 48h.
 *
 * Events are deliberately not notifications: they are read live from Home Assistant's logbook
 * rather than stored, they carry no read/archive state, and they never touch the bell's unread
 * badge — a timeline of doors opening would keep that badge permanently lit and drain it of
 * meaning.
 */
@Composable
fun NotificationPanel(viewModel: MainViewModel, isVisible: Boolean = true) {
    val appColors = LocalHKIAppColors.current
    val notifications by viewModel.notifications.collectAsState()
    var tab by remember { mutableStateOf("inbox") }      // "inbox" | "events" | "archive"
    var query by remember { mutableStateOf("") }
    // Shorter than the history dialogs' 24h default on purpose: a timeline answers "what just
    // happened", and a day of a busy household buries that under hundreds of older rows. The
    // longer windows are one tap away for when the question really is about yesterday.
    var eventHours by remember { mutableStateOf(3L) }

    // Subscribed only while the Events tab is genuinely being looked at — [isVisible] is what
    // distinguishes that from the drawer merely being composed off-screen. Holding an extra
    // websocket subscription for the whole session would work against the app's event-driven
    // battery posture, and nothing off this tab reads it.
    val streaming = isVisible && tab == "events"
    DisposableEffect(streaming, eventHours) {
        if (streaming) viewModel.startEventTimeline(eventHours)
        onDispose { if (streaming) viewModel.stopEventTimeline() }
    }

    fun matches(n: HKINotification) =
        query.isBlank() ||
            n.message.contains(query, ignoreCase = true) ||
            n.title?.contains(query, ignoreCase = true) == true ||
            n.instanceName?.contains(query, ignoreCase = true) == true

    val unread   = notifications.filter { !it.archived && !it.read && matches(it) }
    val history  = notifications.filter { !it.archived && it.read && matches(it) }
    val archived = notifications.filter { it.archived && matches(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp)
            // No tab swiping here: this panel is an edge sheet, and a horizontal drag on it means
            // "put it back" — the gesture people reach for first on a drawer. Its three tabs are
            // always on screen a tap away, so nothing is out of reach.
    ) {
        // ── header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.ui_notifications_753a22b),
                style = MaterialTheme.typography.titleLarge,
                color = appColors.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (tab == "inbox" && notifications.any { !it.archived && !it.read }) {
                IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Icon(Icons.Default.DoneAll, stringResource(R.string.notification_mark_all_read), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "inbox" && notifications.any { !it.archived && it.read }) {
                IconButton(onClick = { viewModel.markAllNotificationsUnread() }) {
                    Icon(Icons.Default.MarkEmailUnread, stringResource(R.string.notification_mark_all_unread), tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "inbox" && notifications.any { !it.archived }) {
                IconButton(onClick = { viewModel.clearNotifications() }) {
                    Icon(Icons.Default.DeleteSweep, stringResource(R.string.notification_delete_all), tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "archive" && notifications.any { it.archived }) {
                IconButton(onClick = { viewModel.clearArchivedNotifications() }) {
                    Icon(Icons.Default.DeleteSweep, stringResource(R.string.notification_delete_all_archived), tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── search ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.ui_search_bce0641), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = appColors.onSurface,
                unfocusedTextColor = appColors.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = appColors.onMuted.copy(alpha = 0.3f),
                focusedContainerColor = appColors.elevated,
                unfocusedContainerColor = appColors.elevated,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(10.dp))

        // ── tabs ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FilterChip(
                selected = tab == "inbox",
                onClick = { tab = "inbox" },
                label = { Text(stringResource(R.string.ui_notifications_753a22b)) },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = tab == "events",
                onClick = { tab = "events" },
                label = { Text(stringResource(R.string.events_tab)) },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = tab == "archive",
                onClick = { tab = "archive" },
                label = { Text(stringResource(R.string.ui_archived_eddc813)) },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── list ────────────────────────────────────────────────────────────
        if (tab == "events") {
            EventsTab(
                viewModel = viewModel,
                query = query,
                hours = eventHours,
                onHoursChange = { eventHours = it }
            )
            return@Column
        }

        val showEmpty = if (tab == "inbox") unread.isEmpty() && history.isEmpty() else archived.isEmpty()
        if (showEmpty) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.NotificationsNone, null,
                    tint = appColors.onMuted.copy(alpha = 0.6f), modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        query.isNotBlank() -> stringResource(R.string.ui_no_matches_cd0af6c)
                        tab == "archive" -> stringResource(R.string.ui_no_archived_notifications_ed5604b)
                        else -> stringResource(R.string.ui_no_notifications_b08626f)
                    },
                    color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium
                )
                if (tab == "inbox" && query.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ui_messages_sent_to_this_device_via_home_assistant_s_87e4a32),
                        color = appColors.onMuted.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Spacer(Modifier.height(96.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tab == "inbox") {
                    if (unread.isNotEmpty()) {
                        item(key = "hdr_unread") { SectionLabel(stringResource(R.string.notification_unread)) }
                        items(unread, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = false) }
                    }
                    if (history.isNotEmpty()) {
                        item(key = "hdr_history") { SectionLabel(stringResource(R.string.notification_history)) }
                        items(history, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = false) }
                    }
                } else {
                    items(archived, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = true) }
                }
            }
        }
    }
}

/**
 * The Events tab: a Homey-style timeline of what the household's entities have been doing.
 *
 * Read live from Home Assistant's logbook rather than stored on the device — the recorder is
 * already the store, so re-reading on open is cheaper than keeping a copy and cannot drift. The
 * roster is set once by an admin for the whole family; the component has already removed whatever
 * this particular person is not allowed to see, so everything that arrives here is showable.
 */
@Composable
private fun EventsTab(
    viewModel: MainViewModel,
    query: String,
    hours: Long,
    onHoursChange: (Long) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val family = LocalVisibilityFamilyContext.current
    val events by viewModel.eventTimeline.collectAsState()
    val roster by viewModel.eventRoster.collectAsState()
    val loading by viewModel.eventTimelineLoading.collectAsState()
    val entities by viewModel.entities.collectAsState()
    val entitiesById = remember(entities) { entities.associateBy { it.entity_id } }

    fun nameFor(event: HALogbookEvent): String =
        event.name
            ?: event.entityId?.let { entitiesById[it]?.friendlyName }
            ?: event.entityId
            ?: ""

    var category by remember { mutableStateOf<String?>(null) }   // null = all

    // Built from what the timeline actually holds rather than from every category HKI knows, so
    // a household without a single water sensor is never offered a "Water" filter that can only
    // ever come back empty. Ordered by how much of the timeline each one accounts for.
    val categories = remember(events, entitiesById) {
        events.groupingBy { eventCategoryKey(it, entitiesById[it.entityId]) }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
    }
    // A filter for something that has since scrolled out of the window would silently show an
    // empty list, so it drops back to All rather than stranding the user on a dead chip.
    LaunchedEffect(categories) {
        if (category != null && category !in categories) category = null
    }

    val visible = events.filter { event ->
        val matchesQuery = query.isBlank() ||
            nameFor(event).contains(query, ignoreCase = true) ||
            event.state?.contains(query, ignoreCase = true) == true ||
            event.message?.contains(query, ignoreCase = true) == true
        val matchesCategory = category == null ||
            eventCategoryKey(event, entitiesById[event.entityId]) == category
        matchesQuery && matchesCategory
    }

    HistoryRangeChips(
        selectedHours = hours.toInt(),
        onSelect = { onHoursChange(it.toLong()) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    )

    if (categories.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = category == null,
                onClick = { category = null },
                label = { Text(stringResource(R.string.events_filter_all)) },
                shape = RoundedCornerShape(12.dp)
            )
            categories.forEach { key ->
                FilterChip(
                    selected = category == key,
                    onClick = { category = if (category == key) null else key },
                    label = { Text(eventCategoryLabel(key)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }

    if (visible.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.events_loading),
                    color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Icon(
                    Icons.Default.History, null,
                    tint = appColors.onMuted.copy(alpha = 0.6f), modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                // Three genuinely different situations, and saying "no events" for all of them
                // would send an admin looking for a fault instead of to the roster editor.
                val rosterEmpty = roster == null || roster?.isEmpty != false
                Text(
                    when {
                        query.isNotBlank() -> stringResource(R.string.events_no_matches)
                        rosterEmpty -> stringResource(R.string.events_not_configured)
                        else -> stringResource(R.string.events_empty)
                    },
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (query.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            !rosterEmpty -> stringResource(R.string.events_empty_hint)
                            family.isAdmin -> stringResource(R.string.events_not_configured_admin)
                            else -> stringResource(R.string.events_not_configured_member)
                        },
                        color = appColors.onMuted.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    // Only an admin can act on this, and only when there is nothing to act on
                    // yet — telling a family member to go and configure something they have no
                    // permission for would be a dead end.
                    val openSettings = LocalOpenSettingsRoute.current
                    if (rosterEmpty && family.isAdmin && openSettings != null) {
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { openSettings(SETTINGS_ROUTE_FAMILY_EVENTS) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.events_set_up_button))
                        }
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
        return
    }

    // Resolved out here because LazyColumn's content block is not a composable scope, so the
    // day label can't call stringResource from inside it.
    val todayLabel = stringResource(R.string.events_today)
    val yesterdayLabel = stringResource(R.string.events_yesterday)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var lastDay: String? = null
        visible.forEachIndexed { index, event ->
            val day = dayLabelFor(event.timestamp, todayLabel, yesterdayLabel)
            if (day != lastDay) {
                lastDay = day
                item(key = "day_$day") { SectionLabel(day) }
            }
            // Logbook events carry no id of their own, so the key is what actually identifies
            // one: this entity, at this instant. The index keeps it unique even then, since two
            // entities can genuinely change in the same millisecond.
            item(key = "${event.entityId}_${event.timestamp}_$index") {
                EventRow(event = event, name = nameFor(event), entity = entitiesById[event.entityId])
            }
        }
    }
}

/** "Today" / "Yesterday" / the date — the same grouping the rest of the app uses for history.
 *  Takes its labels as arguments so it can be called from a LazyColumn's non-composable scope. */
private fun dayLabelFor(timestamp: Long, todayLabel: String, yesterdayLabel: String): String {
    val dayMs = 24L * 60 * 60 * 1000
    val zone = java.util.TimeZone.getDefault()
    // Day boundaries must be local, not UTC: an epoch-modulo split puts "today" in the wrong
    // place for everyone west of Greenwich, and by a whole day for anyone far enough east.
    fun localDayIndex(millis: Long): Long = (millis + zone.getOffset(millis)) / dayMs
    val today = localDayIndex(System.currentTimeMillis())
    return when (localDayIndex(timestamp)) {
        today -> todayLabel
        today - 1 -> yesterdayLabel
        else -> formatHistoryClock(timestamp, withDate = true).substringBefore(' ')
    }
}

/** One line of the timeline: what happened, to what, when — and who did it, when HA knows. */
@Composable
private fun EventRow(event: HALogbookEvent, name: String, entity: HAEntity?) {
    val appColors = LocalHKIAppColors.current
    val phrase = eventPhrase(event, entity)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = appColors.elevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            event.icon?.takeIf { it.isNotBlank() }?.let { icon ->
                MdiIcon(icon.removePrefix("mdi:"), tint = appColors.onMuted, size = 20.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    listOf(name, phrase).filter { it.isNotBlank() }.joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        // Category first: it is what makes a mixed timeline scannable, and it is
                        // the same word as the filter chip above, so the two read as one idea.
                        eventCategoryLabel(eventCategoryKey(event, entity)),
                        formatHistoryClock(event.timestamp),
                        event.contextName?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.events_by, it) }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val appColors = LocalHKIAppColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = appColors.onMuted,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

/**
 * One notification. Swipe left to reveal actions (mark unread / archive / delete — or
 * unarchive / delete on the archive tab). Tapping an unread notification marks it read;
 * an unread entry shows a primary-colored dot on the right.
 */
@Composable
private fun NotificationRow(
    notification: HKINotification,
    viewModel: MainViewModel,
    archivedTab: Boolean
) {
    val appColors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val actionCount = 3
    val revealPx = with(density) { (actionCount * 46 + 8).dp.toPx() }
    val offsetX = remember(notification.id) { Animatable(0f) }
    fun close() = scope.launch { offsetX.animateTo(0f) }

    Box(Modifier.fillMaxWidth()) {
        // ── revealed actions ────────────────────────────────────────────────
        Row(
            modifier = Modifier.matchParentSize().padding(end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Read/unread toggle on both tabs: unread gets "mark as read", read the inverse.
            if (notification.read) {
                SwipeActionButton(Icons.Default.MarkEmailUnread, stringResource(R.string.notification_mark_unread), Color(0xFF42A5F5)) {
                    viewModel.setNotificationRead(notification.id, false); close()
                }
            } else {
                SwipeActionButton(Icons.Default.MarkEmailRead, stringResource(R.string.notification_mark_read), Color(0xFF42A5F5)) {
                    viewModel.setNotificationRead(notification.id, true); close()
                }
            }
            if (!archivedTab) {
                SwipeActionButton(Icons.Default.Archive, stringResource(R.string.notification_archive), Color(0xFF66BB6A)) {
                    viewModel.archiveNotification(notification.id); close()
                }
            } else {
                SwipeActionButton(Icons.Default.Unarchive, stringResource(R.string.notification_unarchive), Color(0xFF66BB6A)) {
                    viewModel.unarchiveNotification(notification.id); close()
                }
            }
            SwipeActionButton(Icons.Default.Delete, stringResource(R.string.notification_delete_action), Color(0xFFEF5350)) {
                viewModel.deleteNotification(notification.id)
            }
        }

        // ── foreground card ─────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = appColors.elevated,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(notification.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            scope.launch { offsetX.snapTo((offsetX.value + amount).coerceIn(-revealPx, 0f)) }
                        },
                        onDragEnd = {
                            scope.launch { offsetX.animateTo(if (offsetX.value < -revealPx / 2f) -revealPx else 0f) }
                        },
                        onDragCancel = { close() }
                    )
                }
                .clickable {
                    when {
                        offsetX.value != 0f -> close()
                        !notification.read -> viewModel.setNotificationRead(notification.id, true)
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    if (!notification.title.isNullOrBlank()) {
                        Text(
                            notification.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = appColors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            notification.instanceName?.takeIf { it.isNotBlank() },
                            formatHistoryClock(notification.timestamp, withDate = true)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.onMuted
                    )
                    NotificationActionButtons(
                        notification = notification,
                        viewModel = viewModel,
                        accentColor = MaterialTheme.colorScheme.primary,
                        inlineReply = true,
                        modifier = Modifier.padding(top = 2.dp).offset(x = (-10).dp)
                    )
                }
                if (!notification.read) {
                    Box(
                        Modifier
                            .padding(start = 10.dp)
                            .size(9.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.18f), CircleShape)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(18.dp))
    }
}

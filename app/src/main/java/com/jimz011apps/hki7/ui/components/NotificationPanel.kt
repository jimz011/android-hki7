package com.jimz011apps.hki7.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimz011apps.hki7.data.HKINotification
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** Opens the notification drawer from anywhere in the app (provided by MainApp). */
val LocalOpenNotifications = staticCompositionLocalOf<(() -> Unit)?> { null }

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
            delay(5.seconds)
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
                Row(
                    Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
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
                        Text("Dismiss", color = bannerForeground)
                    }
                    IconButton(onClick = {
                        exitMode = "delete"
                        visible = false
                        viewModel.deleteNotification(notification.id)
                    }) {
                        Icon(Icons.Default.Close, "Delete notification", tint = bannerForeground)
                    }
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
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = iconTint, modifier = Modifier.size(18.dp))
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
 * Notifications/Archived tabs, search, unread/history sections, swipe-left row actions
 * (mark unread / archive / delete) and mark-all-read. Non-archived entries expire after 48h.
 */
@Composable
fun NotificationPanel(viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val notifications by viewModel.notifications.collectAsState()
    var tab by remember { mutableStateOf("inbox") }      // "inbox" | "archive"
    var query by remember { mutableStateOf("") }

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
            .swipeToAdjacentTab(
                tabs = listOf("inbox", "archive"),
                selected = tab,
                onSelect = { tab = it }
            )
    ) {
        // ── header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleLarge,
                color = appColors.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (tab == "inbox" && notifications.any { !it.archived && !it.read }) {
                IconButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Icon(Icons.Default.DoneAll, "Mark all as read", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "inbox" && notifications.any { !it.archived && it.read }) {
                IconButton(onClick = { viewModel.markAllNotificationsUnread() }) {
                    Icon(Icons.Default.MarkEmailUnread, "Mark all as unread", tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "inbox" && notifications.any { !it.archived }) {
                IconButton(onClick = { viewModel.clearNotifications() }) {
                    Icon(Icons.Default.DeleteSweep, "Delete all", tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (tab == "archive" && notifications.any { it.archived }) {
                IconButton(onClick = { viewModel.clearArchivedNotifications() }) {
                    Icon(Icons.Default.DeleteSweep, "Delete all archived", tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── search ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall) },
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
                label = { Text("Notifications") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = tab == "archive",
                onClick = { tab = "archive" },
                label = { Text("Archived") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── list ────────────────────────────────────────────────────────────
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
                        query.isNotBlank() -> "No matches"
                        tab == "archive" -> "No archived notifications"
                        else -> "No notifications"
                    },
                    color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium
                )
                if (tab == "inbox" && query.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Messages sent to this device via\nHome Assistant's notify service appear here.",
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
                        item(key = "hdr_unread") { SectionLabel("Unread") }
                        items(unread, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = false) }
                    }
                    if (history.isNotEmpty()) {
                        item(key = "hdr_history") { SectionLabel("History") }
                        items(history, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = false) }
                    }
                } else {
                    items(archived, key = { it.id }) { n -> NotificationRow(n, viewModel, archivedTab = true) }
                }
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
                SwipeActionButton(Icons.Default.MarkEmailUnread, "Mark as unread", Color(0xFF42A5F5)) {
                    viewModel.setNotificationRead(notification.id, false); close()
                }
            } else {
                SwipeActionButton(Icons.Default.MarkEmailRead, "Mark as read", Color(0xFF42A5F5)) {
                    viewModel.setNotificationRead(notification.id, true); close()
                }
            }
            if (!archivedTab) {
                SwipeActionButton(Icons.Default.Archive, "Archive", Color(0xFF66BB6A)) {
                    viewModel.archiveNotification(notification.id); close()
                }
            } else {
                SwipeActionButton(Icons.Default.Unarchive, "Unarchive", Color(0xFF66BB6A)) {
                    viewModel.unarchiveNotification(notification.id); close()
                }
            }
            SwipeActionButton(Icons.Default.Delete, "Delete", Color(0xFFEF5350)) {
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

package com.example.hki7.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import com.example.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hki7.data.HACalendarEvent
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKICalendarWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.WidgetBackground
import com.example.hki7.ui.components.WidgetBackgroundSelector
import com.example.hki7.ui.components.surfaceGradient
import com.example.hki7.ui.components.itemCornerShape
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

val calendarWidgetViews = listOf(
    "agenda" to "Agenda",
    "week" to "Week",
    "month" to "Month"
)

private val CalendarPalette = listOf(
    Color(0xFF0A84FF),
    Color(0xFFFF9F0A),
    Color(0xFF30D158),
    Color(0xFFBF5AF2),
    Color(0xFFFF375F),
    Color(0xFF64D2FF),
    Color(0xFFFFD60A)
)

private data class CalendarWindow(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val displayStartDate: LocalDate,
    val displayEndDateExclusive: LocalDate,
    val title: String
) {
    fun startMillis(zone: ZoneId): Long = displayStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId): Long = displayEndDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
}

@Composable
fun CalendarWidgetItem(
    widget: HKICalendarWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    var showFullDialog by remember(widget.id) { mutableStateOf(false) }
    val compact = widget.width == "half"
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = compact && !isEditMode) { showFullDialog = true }
        ) {
            CalendarWidgetCard(
                widget = widget,
                viewModel = viewModel,
                freezeUpdates = isEditMode,
                interactionsEnabled = !isEditMode && !compact
            )
        }
        if (showFullDialog) {
            com.example.hki7.ui.components.ModernSettingsDialogFrame(
                title = widget.title ?: "Calendar",
                subtitle = normalizeCalendarView(widget.view).replaceFirstChar { it.uppercase() },
                icon = Icons.Default.CalendarMonth,
                onDismiss = { showFullDialog = false },
                content = {
                    CalendarWidgetCard(
                        widget = widget.copy(width = "full", isSquare = false),
                        viewModel = viewModel,
                        interactionsEnabled = true,
                        modifier = Modifier.fillMaxSize(),
                        fillHeight = true
                    )
                },
                footer = { TextButton(onClick = { showFullDialog = false }) { Text("Done") } }
            )
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun CalendarWidgetCard(
    widget: HKICalendarWidget,
    viewModel: MainViewModel,
    freezeUpdates: Boolean = false,
    interactionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    // The full-screen dialog: drop the fixed 16:9 footprint and let the content
    // use (and scroll within) all available height, so e.g. a month shows all weeks.
    fillHeight: Boolean = false
) {
    val appColors = LocalHKIAppColors.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val zone = ZoneId.systemDefault()
    val calendarEntityFlow = remember(viewModel, widget.entityIds, freezeUpdates) {
        if (widget.entityIds.isEmpty()) {
            if (freezeUpdates) viewModel.entitySnapshotMatching { it.entity_id.startsWith("calendar.") }
            else viewModel.entitiesMatching("domain:calendar") { it.entity_id.startsWith("calendar.") }
        } else {
            if (freezeUpdates) viewModel.entitySnapshotFor(widget.entityIds) else viewModel.entitiesFor(widget.entityIds)
        }
    }
    val calendarEntities by calendarEntityFlow.collectAsState()
    val entityIds = remember(widget.entityIds, calendarEntities) {
        widget.entityIds.filter { id -> calendarEntities.any { it.entity_id == id } }
            .ifEmpty { calendarEntities.map { it.entity_id } }
    }
    val calendarNames = remember(calendarEntities) {
        calendarEntities.associate { it.entity_id to (it.friendlyName ?: it.entity_id.substringAfter(".")) }
    }
    val colorsByEntity = remember(entityIds) {
        entityIds.withIndex().associate { (index, entityId) -> entityId to CalendarPalette[index % CalendarPalette.size] }
    }
    var selectedEpochDay by remember(widget.id) { mutableLongStateOf(LocalDate.now(zone).toEpochDay()) }
    var activeView by remember(widget.id, widget.view) { mutableStateOf(normalizeCalendarView(widget.view)) }
    var showDatePicker by remember(widget.id) { mutableStateOf(false) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val window = remember(activeView, selectedDate) { calendarWindow(activeView, selectedDate) }
    val startMillis = window.startMillis(zone)
    val endMillis = window.endMillis(zone)
    val cacheKey = remember(entityIds, startMillis, endMillis) {
        viewModel.calendarEventsCacheKey(entityIds, startMillis, endMillis)
    }
    val eventFlow = remember(viewModel, cacheKey) { viewModel.calendarEventsFor(cacheKey) }
    val cachedEvents by eventFlow.collectAsState()
    LaunchedEffect(entityIds, startMillis, endMillis) {
        viewModel.fetchCalendarEvents(entityIds, startMillis, endMillis)
    }
    val events by produceState(
        initialValue = emptyList<HACalendarEvent>(),
        cachedEvents,
        entityIds,
        zone
    ) {
        value = withContext(Dispatchers.Default) {
            val allowedIds = entityIds.toHashSet()
            val fallbackStart = ZonedDateTime.now(zone)
            cachedEvents.asSequence()
                .filter { it.entityId in allowedIds }
                .map { event -> event to (event.startDateTime(zone) ?: fallbackStart) }
                .sortedWith(compareBy<Pair<HACalendarEvent, ZonedDateTime>> { it.second }.thenBy { it.first.summary.orEmpty() })
                .map { it.first }
                .toList()
        }
    }
    if (widget.width == "half") {
        CompactCalendarWidgetCard(
            widget = widget,
            activeView = activeView,
            selectedDate = selectedDate,
            window = window,
            events = events,
            colorsByEntity = colorsByEntity,
            zone = zone,
            currentUrl = currentUrl
        )
        return
    }
    Card(
        modifier = modifier.fillMaxWidth().then(
            when {
                fillHeight -> Modifier.fillMaxSize()
                // "Standard" has one shared 16:9 footprint across every widget type.
                widget.isSquare -> Modifier.aspectRatio(1f)
                else -> Modifier.aspectRatio(16f / 9f)
            }
        ).then(
            if (widget.backgroundUrl.isNullOrBlank())
                Modifier.background(surfaceGradient(appColors.elevated), RoundedCornerShape(widget.cornerRadius.dp))
            else Modifier
        ),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
      Box {
        WidgetBackground(widget.backgroundUrl, currentUrl)
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(if (fillHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CalendarHeader(
                title = widget.title ?: "Calendar",
                icon = widget.icon,
                windowTitle = window.title,
                selectedDate = selectedDate,
                activeView = activeView,
                onPrevious = { selectedEpochDay = shiftDate(selectedDate, activeView, -1).toEpochDay() },
                onNext = { selectedEpochDay = shiftDate(selectedDate, activeView, 1).toEpochDay() },
                onToday = { selectedEpochDay = LocalDate.now(zone).toEpochDay() },
                onPickDate = if (interactionsEnabled) ({ showDatePicker = true }) else null
            )
            CalendarViewTabs(activeView = activeView, enabled = interactionsEnabled) { activeView = it }
            if (entityIds.isEmpty()) {
                CalendarEmptyState("No calendar entity available")
            } else {
                when (activeView) {
                    "week" -> WeekCalendarView(
                        selectedDate = selectedDate,
                        events = events,
                        colorsByEntity = colorsByEntity,
                        calendarNames = calendarNames,
                        interactionsEnabled = interactionsEnabled,
                        onSelectDate = { selectedEpochDay = it.toEpochDay() },
                        zone = zone
                    )
                    "month" -> MonthCalendarView(
                        selectedDate = selectedDate,
                        window = window,
                        events = events,
                        colorsByEntity = colorsByEntity,
                        calendarNames = calendarNames,
                        interactionsEnabled = interactionsEnabled,
                        onSelectDate = { selectedEpochDay = it.toEpochDay() },
                        zone = zone
                    )
                    else -> AgendaCalendarView(
                        startDate = window.startDate,
                        endDateExclusive = window.endDateExclusive,
                        events = events,
                        colorsByEntity = colorsByEntity,
                        calendarNames = calendarNames,
                        zone = zone
                    )
                }
            }
        }
      }
    }

    if (showDatePicker) {
        CalendarDatePickerDialog(
            selectedDate = selectedDate,
            zone = zone,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                selectedEpochDay = date.toEpochDay()
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun CompactCalendarWidgetCard(
    widget: HKICalendarWidget,
    activeView: String,
    selectedDate: LocalDate,
    window: CalendarWindow,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    zone: ZoneId,
    currentUrl: String = ""
) {
    val appColors = LocalHKIAppColors.current
    val visibleEvents = remember(events, selectedDate, zone) {
        events.filter { it.occursOn(selectedDate, zone) }.take(2)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (widget.isSquare) Modifier.aspectRatio(1f) else Modifier.aspectRatio(16f / 9f))
            .then(
                if (widget.backgroundUrl.isNullOrBlank())
                    Modifier.background(surfaceGradient(appColors.elevated), RoundedCornerShape(widget.cornerRadius.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
      Box {
        WidgetBackground(widget.backgroundUrl, currentUrl)
        when (normalizeCalendarView(activeView)) {
            "month" -> CompactMonthCalendar(
                selectedDate = selectedDate,
                window = window,
                events = events,
                colorsByEntity = colorsByEntity,
                zone = zone
            )
            "week" -> CompactWeekCalendar(
                selectedDate = selectedDate,
                events = events,
                colorsByEntity = colorsByEntity,
                zone = zone
            )
            else -> Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            selectedDate.dayOfMonth.toString(),
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            maxLines = 1
                        )
                    }
                    MdiIcon(widget.icon ?: "calendar-month", tint = appColors.onMuted.copy(alpha = 0.22f), size = 34.dp)
                }
                Spacer(Modifier.weight(1f))
                if (visibleEvents.isEmpty()) {
                    Text(
                        "No events",
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    visibleEvents.forEach { event ->
                        CompactCalendarEventPill(
                            event = event,
                            color = colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary,
                            zone = zone
                        )
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun CompactWeekCalendar(
    selectedDate: LocalDate,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = (0 until 7).map { weekStart.plusDays(it.toLong()) }
    val selectedEvents = events.filter { it.occursOn(selectedDate, zone) }.take(2)
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                selectedDate.dayOfMonth.toString(),
                color = appColors.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.forEach { day ->
                val selected = day == selectedDate
                val hasEvents = events.any { it.occursOn(day, zone) }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = itemCornerShape(),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                {
                    Column(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.format(DateTimeFormatter.ofPattern("E")).take(1),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else appColors.onMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else appColors.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            Modifier
                                .size(4.dp)
                                .background(
                                    if (hasEvents) {
                                        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    } else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (selectedEvents.isEmpty()) {
            Text("No events", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        } else {
            selectedEvents.forEach { event ->
                CompactCalendarEventPill(event, colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary, zone)
            }
        }
    }
}

@Composable
private fun CompactMonthCalendar(
    selectedDate: LocalDate,
    window: CalendarWindow,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    val today = LocalDate.now(zone)
    val days = generateSequence(window.displayStartDate) { it.plusDays(1) }
        .take(ChronoUnit.DAYS.between(window.displayStartDate, window.displayEndDateExclusive).toInt())
        .toList()
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            selectedDate.format(DateTimeFormatter.ofPattern("MMMM")).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(label, modifier = Modifier.weight(1f), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { day ->
                    val selected = day == selectedDate
                    val inMonth = day.month == selectedDate.month
                    val dayEvents = events.filter { it.occursOn(day, zone) }
                    Surface(
                        modifier = Modifier.weight(1f).height(22.dp),
                        shape = CircleShape,
                        color = when {
                            selected -> MaterialTheme.colorScheme.primary
                            day == today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                day.dayOfMonth.toString(),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    inMonth -> appColors.onSurface
                                    else -> appColors.onMuted.copy(alpha = 0.35f)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected || day == today) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            if (dayEvents.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    dayEvents.take(2).forEach { event ->
                                        Box(
                                            Modifier.size(2.dp).background(
                                                if (selected) MaterialTheme.colorScheme.onPrimary else colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarEventPill(
    event: HACalendarEvent,
    color: Color,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemCornerShape(),
        color = color.copy(alpha = 0.2f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                event.summary?.takeIf { it.isNotBlank() } ?: "Untitled event",
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                eventTimeLabel(event, zone),
                color = appColors.onMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    selectedDate: LocalDate,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    onDateSelected(date)
                } ?: onDismiss()
            }) { Text("Done") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDateSelected(LocalDate.now(zone)) }) { Text("Today") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun CalendarHeader(
    title: String,
    icon: String?,
    windowTitle: String,
    selectedDate: LocalDate,
    activeView: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickDate: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    val today = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = itemCornerShape(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = if (onPickDate != null) Modifier.clickable { onPickDate() } else Modifier
        ) {
            Column(
                modifier = Modifier.width(54.dp).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEE")).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    selectedDate.dayOfMonth.toString(),
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!icon.isNullOrBlank()) {
                    MdiIcon(icon, tint = appColors.onMuted, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.CalendarMonth, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(title, color = appColors.onMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                windowTitle,
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = appColors.onSurface, modifier = Modifier.size(20.dp))
            }
            Surface(
                shape = CircleShape,
                color = if (selectedDate == today) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else appColors.subtleSurface,
                modifier = Modifier.size(30.dp).clickable { onToday() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (activeView == "month") today.dayOfMonth.toString() else "T",
                        color = if (selectedDate == today) MaterialTheme.colorScheme.primary else appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onSurface, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CalendarViewTabs(activeView: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Row(modifier = Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            calendarWidgetViews.forEach { (value, label) ->
                val selected = activeView == value
                Surface(
                    modifier = Modifier.weight(1f).clip(itemCornerShape()).clickable(enabled = enabled) { onSelect(value) },
                    shape = itemCornerShape(),
                    color = if (selected) appColors.surface else Color.Transparent,
                    tonalElevation = if (selected) 2.dp else 0.dp
                ) {
                    Text(
                        label,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = if (selected) appColors.onSurface else appColors.onMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaCalendarView(
    startDate: LocalDate,
    endDateExclusive: LocalDate,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    calendarNames: Map<String, String>,
    zone: ZoneId
) {
    val visibleEvents = events.filter { event ->
        val day = event.startDate(zone)
        day != null && day >= startDate && day < endDateExclusive
    }
    if (visibleEvents.isEmpty()) {
        CalendarEmptyState("No events in the next days")
        return
    }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .heightIn(max = 330.dp)
            .fadingEdges(listState),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        visibleEvents.groupBy { it.startDate(zone) ?: startDate }.forEach { (date, dayEvents) ->
            item(key = "header-${date.toEpochDay()}") {
                DaySectionHeader(date)
            }
            items(dayEvents, key = { "${it.entityId}:${it.summary}:${it.start?.date}:${it.start?.dateTime}" }) { event ->
                CalendarEventRow(event, colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary, calendarNames[event.entityId], zone)
            }
        }
    }
}

@Composable
private fun WeekCalendarView(
    selectedDate: LocalDate,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    calendarNames: Map<String, String>,
    interactionsEnabled: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = (0 until 7).map { weekStart.plusDays(it.toLong()) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            val dayEvents = events.filter { it.occursOn(day, zone) }
            val selected = day == selectedDate
            Surface(
                modifier = Modifier.weight(1f).clip(itemCornerShape()).clickable(enabled = interactionsEnabled) { onSelectDate(day) },
                shape = itemCornerShape(),
                color = if (selected) MaterialTheme.colorScheme.primary else appColors.subtleSurface
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(day.format(DateTimeFormatter.ofPattern("EEE")).take(1), color = if (selected) MaterialTheme.colorScheme.onPrimary else appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    Text(day.dayOfMonth.toString(), color = if (selected) MaterialTheme.colorScheme.onPrimary else appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(8.dp)) {
                        dayEvents.take(3).forEach { event ->
                            Box(
                                modifier = Modifier.size(4.dp).background(colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
    val selectedEvents = events.filter { it.occursOn(selectedDate, zone) }
    if (selectedEvents.isEmpty()) {
        CalendarEmptyState("No events for ${selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM"))}", compact = true)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedEvents.take(5).forEach { event ->
                CalendarEventRow(event, colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary, calendarNames[event.entityId], zone)
            }
        }
    }
}

@Composable
private fun MonthCalendarView(
    selectedDate: LocalDate,
    window: CalendarWindow,
    events: List<HACalendarEvent>,
    colorsByEntity: Map<String, Color>,
    calendarNames: Map<String, String>,
    interactionsEnabled: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    val today = LocalDate.now(zone)
    val days = generateSequence(window.displayStartDate) { it.plusDays(1) }
        .take(ChronoUnit.DAYS.between(window.displayStartDate, window.displayEndDateExclusive).toInt())
        .toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(label, modifier = Modifier.weight(1f), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val inMonth = day.month == selectedDate.month
                    val selected = day == selectedDate
                    val dayEvents = events.filter { it.occursOn(day, zone) }
                    Surface(
                        modifier = Modifier.weight(1f).height(42.dp).clip(itemCornerShape()).clickable(enabled = interactionsEnabled) { onSelectDate(day) },
                        shape = itemCornerShape(),
                        color = when {
                            selected -> MaterialTheme.colorScheme.primary
                            day == today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                day.dayOfMonth.toString(),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    inMonth -> appColors.onSurface
                                    else -> appColors.onMuted.copy(alpha = 0.42f)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected || day == today) FontWeight.Bold else FontWeight.Normal
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(8.dp)) {
                                dayEvents.take(3).forEach { event ->
                                    Box(
                                        modifier = Modifier.size(4.dp).background(
                                            if (selected) MaterialTheme.colorScheme.onPrimary else colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val selectedEvents = events.filter { it.occursOn(selectedDate, zone) }
        if (selectedEvents.isEmpty()) {
            CalendarEmptyState("No events for ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}", compact = true)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedEvents.take(4).forEach { event ->
                    CalendarEventRow(event, colorsByEntity[event.entityId] ?: MaterialTheme.colorScheme.primary, calendarNames[event.entityId], zone)
                }
            }
        }
    }
}

@Composable
private fun DaySectionHeader(date: LocalDate) {
    val appColors = LocalHKIAppColors.current
    val label = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMM"))
    }
    Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun CalendarEventRow(
    event: HACalendarEvent,
    color: Color,
    calendarName: String?,
    zone: ZoneId
) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(44.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    event.summary?.takeIf { it.isNotBlank() } ?: "Untitled event",
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = appColors.onMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(eventTimeLabel(event, zone), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!event.location.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.LocationOn, null, tint = appColors.onMuted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(event.location, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (!calendarName.isNullOrBlank()) {
                Surface(shape = itemCornerShape(), color = color.copy(alpha = 0.16f)) {
                    Text(calendarName, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CalendarEmptyState(message: String, compact: Boolean = false) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Box(
            modifier = Modifier.fillMaxWidth().height(if (compact) 64.dp else 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Event, null, tint = appColors.onMuted, modifier = Modifier.size(if (compact) 18.dp else 24.dp))
                Spacer(Modifier.height(6.dp))
                Text(message, color = appColors.onMuted, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun CalendarWidgetSettingsDialog(
    widget: HKICalendarWidget,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKICalendarWidget) -> Unit
) {
    var entityIds by remember(widget) { mutableStateOf(widget.entityIds) }
    var view by remember(widget) { mutableStateOf(normalizeCalendarView(widget.view)) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "calendar-month") }
    var width by remember(widget) { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var cornerRadius by remember(widget) { mutableStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("content") }
    val calendarEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("calendar.") } }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = calendarEntities,
            title = "Select Calendars",
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids ->
                entityIds = ids.filter { it.startsWith("calendar.") }
                showEntityPicker = false
            }
        )
    }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName.takeUnless { it == "None" } ?: "",
            onDismiss = { showIconPicker = false },
            onSelect = {
                iconName = it.ifBlank { "None" }
                showIconPicker = false
            }
        )
    }

    val settingsScroll = rememberScrollState()
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = { com.example.hki7.ui.components.ModernSettingsDialogTitle("Calendar", "Calendars, default view, and appearance") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .fadingEdges(settingsScroll)
                    .verticalScroll(settingsScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.hki7.ui.components.SettingsTabRow(
                    tabs = listOf("content" to "Calendar", "appearance" to "Appearance"),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                com.example.hki7.ui.components.SettingsSubcategory("Calendar content", "Select calendars and the view shown first")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Calendars", style = MaterialTheme.typography.labelLarge)
                val selectedNames = entityIds.mapNotNull { id -> calendarEntities.find { it.entity_id == id }?.friendlyName ?: id }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "All calendar entities",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedNames.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text("Change") }
                    if (entityIds.isNotEmpty()) {
                        TextButton(onClick = { entityIds = emptyList() }) { Text("All") }
                    }
                }
                Text("Default view", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    calendarWidgetViews.forEach { (value, label) ->
                        FilterChip(selected = view == value, onClick = { view = value }, label = { Text(label) })
                    }
                }
                }
                if (settingsPage == "appearance") {
                com.example.hki7.ui.components.SettingsSubcategory("Appearance", "Card width, shape, icon, and background")
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text("Standard") })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text("Square") })
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iconName != "None") MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) { Text(if (iconName == "None") "Choose" else "Change") }
                    if (iconName != "None") TextButton(onClick = { iconName = "None" }) { Text("None") }
                }
                WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        widget.copy(
                            entityIds = entityIds,
                            view = view,
                            isSquare = isSquare,
                            title = title.ifBlank { null },
                            icon = iconName.takeUnless { it == "None" },
                            width = width,
                            cornerRadius = cornerRadius,
                            backgroundUrl = backgroundUrl
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CalendarEntityPickerDialog(
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    val calendarEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("calendar.") } }
    AdvancedEntitySearchDialog(
        allEntities = calendarEntities,
        title = "Select Calendars",
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = { ids -> onSelected(ids.filter { it.startsWith("calendar.") }) }
    )
}

private fun normalizeCalendarView(view: String): String =
    if (calendarWidgetViews.any { it.first == view }) view else "agenda"


private fun calendarWindow(view: String, selectedDate: LocalDate): CalendarWindow {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    val dayFmt = DateTimeFormatter.ofPattern("d MMM")
    return when (normalizeCalendarView(view)) {
        "week" -> {
            val start = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            CalendarWindow(
                startDate = start,
                endDateExclusive = start.plusDays(7),
                displayStartDate = start,
                displayEndDateExclusive = start.plusDays(7),
                title = "${start.format(dayFmt)} - ${start.plusDays(6).format(dayFmt)}"
            )
        }
        "month" -> {
            val start = selectedDate.withDayOfMonth(1)
            val displayStart = start.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val displayEnd = displayStart.plusDays(42)
            CalendarWindow(
                startDate = start,
                endDateExclusive = start.plusMonths(1),
                displayStartDate = displayStart,
                displayEndDateExclusive = displayEnd,
                title = selectedDate.format(monthFmt)
            )
        }
        else -> CalendarWindow(
            startDate = selectedDate,
            endDateExclusive = selectedDate.plusDays(14),
            displayStartDate = selectedDate,
            displayEndDateExclusive = selectedDate.plusDays(14),
            title = "Upcoming"
        )
    }
}

private fun shiftDate(date: LocalDate, view: String, direction: Int): LocalDate = when (normalizeCalendarView(view)) {
    "week" -> date.plusWeeks(direction.toLong())
    "month" -> date.plusMonths(direction.toLong())
    else -> date.plusDays(direction.toLong())
}

private fun HACalendarEvent.startDateTime(zone: ZoneId): ZonedDateTime? = parseCalendarDateTime(start?.dateTime, start?.date, zone)

private fun HACalendarEvent.endDateTime(zone: ZoneId): ZonedDateTime? = parseCalendarDateTime(end?.dateTime, end?.date, zone)

private fun HACalendarEvent.startDate(zone: ZoneId): LocalDate? = startDateTime(zone)?.toLocalDate()

private fun HACalendarEvent.isAllDay(): Boolean = start?.date != null && start.dateTime == null

private fun HACalendarEvent.occursOn(day: LocalDate, zone: ZoneId): Boolean {
    val startDay = startDate(zone) ?: return false
    val endDay = when {
        isAllDay() && end?.date != null -> runCatching { LocalDate.parse(end!!.date).minusDays(1) }.getOrDefault(startDay)
        else -> endDateTime(zone)?.toLocalDate() ?: startDay
    }
    return day >= startDay && day <= endDay
}

private fun parseCalendarDateTime(dateTime: String?, date: String?, zone: ZoneId): ZonedDateTime? {
    if (!dateTime.isNullOrBlank()) {
        runCatching { return OffsetDateTime.parse(dateTime).atZoneSameInstant(zone) }
        runCatching { return ZonedDateTime.parse(dateTime).withZoneSameInstant(zone) }
        runCatching { return LocalDateTime.parse(dateTime).atZone(zone) }
    }
    if (!date.isNullOrBlank()) {
        runCatching { return LocalDate.parse(date).atStartOfDay(zone) }
    }
    return null
}

private fun eventTimeLabel(event: HACalendarEvent, zone: ZoneId): String {
    if (event.isAllDay()) return "All day"
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = event.startDateTime(zone) ?: return "Time unknown"
    val end = event.endDateTime(zone)
    return if (end != null && end.toLocalDate() == start.toLocalDate()) {
        "${start.format(timeFmt)} - ${end.format(timeFmt)}"
    } else {
        start.format(timeFmt)
    }
}

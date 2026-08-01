package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.jimz011apps.hki7.data.HACalendarEvent
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.isButtonVisibleNow
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKICalendarWidget
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
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
import java.time.temporal.WeekFields
import java.util.Locale

private val calendarWidgetViews = listOf("agenda", "week", "month")

@Composable
private fun calendarViewLabel(view: String): String = when (view) {
    "week" -> stringResource(R.string.widgets_calendar_week)
    "month" -> stringResource(R.string.widgets_calendar_month)
    else -> stringResource(R.string.widgets_calendar_agenda)
}

@Composable
private fun appLocale(): Locale =
    LocalConfiguration.current.locales[0] ?: Locale.getDefault()

private fun startOfLocaleWeek(date: LocalDate, locale: Locale): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))

private fun localeWeekdayLabels(start: LocalDate, locale: Locale): List<String> =
    (0 until 7).map { start.plusDays(it.toLong()).format(DateTimeFormatter.ofPattern("EEEEE", locale)) }

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
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
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
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogFrame(
                title = widget.title ?: stringResource(R.string.widgets_calendar_title),
                subtitle = calendarViewLabel(normalizeCalendarView(widget.view)),
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
                footer = { TextButton(onClick = { showFullDialog = false }) { Text(stringResource(R.string.ui_done_e9b450d)) } }
            )
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun CalendarWidgetCard(
    widget: HKICalendarWidget,
    viewModel: MainViewModel,
    interactionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    freezeUpdates: Boolean = false,
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
    val entityIds = remember(widget.entityIds, calendarEntities, widget.itemConfigs) {
        widget.entityIds.filter { id -> calendarEntities.any { it.entity_id == id } }
            .ifEmpty { calendarEntities.map { it.entity_id } }
            .filter { isButtonVisibleNow(widget.itemConfigs[it] ?: HKIButtonConfig()) }
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
    val window = calendarWindow(activeView, selectedDate)
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
        initialValue = emptyList(),
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
        // Agenda is a lazy list that scrolls itself, so in the full-height dialog it takes the
        // remaining space rather than sitting inside the column's scroll — a lazy list in a
        // scrolling parent gets unbounded height, which is what forced its fixed cap. Week and
        // month are plain columns that can outgrow the dialog, so those keep the outer scroll.
        val agendaFillsHeight = fillHeight && activeView != "week" && activeView != "month"
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(
                    when {
                        !fillHeight -> Modifier
                        agendaFillsHeight -> Modifier.fillMaxSize()
                        else -> Modifier.verticalScroll(rememberScrollState())
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CalendarHeader(
                title = widget.title ?: stringResource(R.string.widgets_calendar_title),
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
                CalendarEmptyState(stringResource(R.string.widgets_calendar_no_entity))
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
                        zone = zone,
                        modifier = if (agendaFillsHeight) Modifier.weight(1f) else Modifier,
                        fillHeight = agendaFillsHeight
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
    val locale = appLocale()
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
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE", locale)),
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
                        stringResource(R.string.ui_no_events_e339ba7),
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
    val locale = appLocale()
    val weekStart = startOfLocaleWeek(selectedDate, locale)
    val days = (0 until 7).map { weekStart.plusDays(it.toLong()) }
    val selectedEvents = events.filter { it.occursOn(selectedDate, zone) }.take(2)
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE", locale)),
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
                            day.format(DateTimeFormatter.ofPattern("EEEEE", locale)),
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
            Text(stringResource(R.string.ui_no_events_e339ba7), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
    val locale = appLocale()
    val today = LocalDate.now(zone)
    val days = generateSequence(window.displayStartDate) { it.plusDays(1) }
        .take(ChronoUnit.DAYS.between(window.displayStartDate, window.displayEndDateExclusive).toInt())
        .toList()
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            selectedDate.format(DateTimeFormatter.ofPattern("MMMM", locale)).uppercase(locale),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            localeWeekdayLabels(window.displayStartDate, locale).forEach { label ->
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
                event.summary?.takeIf { it.isNotBlank() } ?: stringResource(R.string.ui_untitled_event_ef8d264),
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
            }) { Text(stringResource(R.string.ui_done_e9b450d)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDateSelected(LocalDate.now(zone)) }) { Text(stringResource(R.string.ui_today_24345a1)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
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
    val locale = appLocale()
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
                    selectedDate.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale),
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
                        if (activeView == "month") today.dayOfMonth.toString() else stringResource(R.string.widgets_today_initial),
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
            calendarWidgetViews.forEach { value ->
                val selected = activeView == value
                Surface(
                    modifier = Modifier.weight(1f).clip(itemCornerShape()).clickable(enabled = enabled) { onSelect(value) },
                    shape = itemCornerShape(),
                    color = if (selected) appColors.surface else Color.Transparent,
                    tonalElevation = if (selected) 2.dp else 0.dp
                ) {
                    Text(
                        calendarViewLabel(value),
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
    zone: ZoneId,
    modifier: Modifier = Modifier,
    /** Fill the height the caller allots instead of the compact card's fixed cap, so the
     *  full-screen dialog shows a long agenda over its whole height. */
    fillHeight: Boolean = false
) {
    val visibleEvents = events.filter { event ->
        val day = event.startDate(zone)
        day != null && day >= startDate && day < endDateExclusive
    }
    if (visibleEvents.isEmpty()) {
        CalendarEmptyState(stringResource(R.string.widgets_calendar_no_upcoming_events))
        return
    }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.heightIn(max = 330.dp))
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
    val locale = appLocale()
    val weekStart = startOfLocaleWeek(selectedDate, locale)
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
                    Text(day.format(DateTimeFormatter.ofPattern("EEEEE", locale)), color = if (selected) MaterialTheme.colorScheme.onPrimary else appColors.onMuted, style = MaterialTheme.typography.labelSmall)
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
        CalendarEmptyState(
            stringResource(
                R.string.widgets_calendar_no_events_for,
                selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
            ),
            compact = true
        )
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
    val locale = appLocale()
    val today = LocalDate.now(zone)
    val days = generateSequence(window.displayStartDate) { it.plusDays(1) }
        .take(ChronoUnit.DAYS.between(window.displayStartDate, window.displayEndDateExclusive).toInt())
        .toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            localeWeekdayLabels(window.displayStartDate, locale).forEach { label ->
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
            CalendarEmptyState(
                stringResource(
                    R.string.widgets_calendar_no_events_for,
                    selectedDate.format(DateTimeFormatter.ofPattern("d MMM", locale))
                ),
                compact = true
            )
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
    val locale = appLocale()
    val label = when (date) {
        LocalDate.now() -> stringResource(R.string.ui_today_24345a1)
        LocalDate.now().plusDays(1) -> stringResource(R.string.ui_tomorrow_1948bf2)
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMM", locale))
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
                    event.summary?.takeIf { it.isNotBlank() } ?: stringResource(R.string.ui_untitled_event_ef8d264),
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
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("content") }
    var itemConfigs by remember(widget) { mutableStateOf(widget.itemConfigs) }
    var editingItemVisibility by remember { mutableStateOf<String?>(null) }
    var visSpec by remember(widget) {
        mutableStateOf(
            widget.toVisibilitySpec()
        )
    }
    val calendarEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("calendar.") } }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = calendarEntities,
            title = stringResource(R.string.ui_select_calendars_e26b0da),
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
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_calendar_title),
                stringResource(R.string.widgets_calendar_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .fadingEdges(settingsScroll)
                    .verticalScroll(settingsScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "content" to stringResource(R.string.widgets_calendar_title),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_calendar_content_1f185c8), stringResource(R.string.ui_select_calendars_and_the_view_shown_first_d5c38b5))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_optional_932fc13)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.ui_calendars_9444501), style = MaterialTheme.typography.labelLarge)
                if (entityIds.isEmpty()) {
                    Text(
                        stringResource(R.string.ui_all_calendar_entities_bffb01d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entityIds.forEach { id ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            calendarEntities.find { it.entity_id == id }?.friendlyName ?: id,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { editingItemVisibility = id }) {
                            Icon(
                                if (isButtonVisibleNow(itemConfigs[id] ?: HKIButtonConfig())) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = stringResource(R.string.ui_visibility_7d9ff4f)
                            )
                        }
                        IconButton(onClick = { entityIds = entityIds - id; itemConfigs = itemConfigs - id }) {
                            Icon(Icons.Default.Close, stringResource(R.string.widgets_remove))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    if (entityIds.isNotEmpty()) {
                        TextButton(onClick = { entityIds = emptyList() }) { Text(stringResource(R.string.ui_all_6a72085)) }
                    }
                }
                Text(stringResource(R.string.ui_default_view_343256a), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    calendarWidgetViews.forEach { value ->
                        FilterChip(
                            selected = view == value,
                            onClick = { view = value },
                            label = { Text(calendarViewLabel(value)) }
                        )
                    }
                }
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_card_width_shape_icon_and_background_c3695b5))
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                    FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                }
                Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iconName != "None") MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) {
                        Text(if (iconName == "None") stringResource(R.string.ui_choose_78b7c9f) else stringResource(R.string.ui_change_64fbd99))
                    }
                    if (iconName != "None") TextButton(onClick = { iconName = "None" }) { Text(stringResource(R.string.ui_none_6eef664)) }
                }
                WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
                if (settingsPage == "visibility") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
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
                            backgroundUrl = backgroundUrl,
                            isHidden = visSpec.hidden,
                            visibilityStart = visSpec.start,
                            visibilityEnd = visSpec.end,
                            visibilityRangeMode = visSpec.rangeMode,
                            visibilityRecurrence = visSpec.recurrence,
                            visibilityConditionEntityId = visSpec.conditionEntityId,
                            visibilityConditionState = visSpec.conditionState,
                            visibilityConditionNegate = visSpec.conditionNegate,
                            visibilityConditions = visSpec.conditions,
                            visibilityMatch = visSpec.match,
                            itemConfigs = itemConfigs
                        )
                    )
                }
            ) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
    editingItemVisibility?.let { id ->
        com.jimz011apps.hki7.ui.components.ItemVisibilityDialog(
            label = calendarEntities.find { it.entity_id == id }?.friendlyName ?: id,
            config = itemConfigs[id] ?: HKIButtonConfig(),
            onDismiss = { editingItemVisibility = null },
            onSave = { itemConfigs = itemConfigs + (id to it) }
        )
    }
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
        title = stringResource(R.string.ui_select_calendars_e26b0da),
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = { ids -> onSelected(ids.filter { it.startsWith("calendar.") }) }
    )
}

private fun normalizeCalendarView(view: String): String =
    if (view in calendarWidgetViews) view else "agenda"


@Composable
private fun calendarWindow(view: String, selectedDate: LocalDate): CalendarWindow {
    val locale = appLocale()
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val dayFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    return when (normalizeCalendarView(view)) {
        "week" -> {
            val start = startOfLocaleWeek(selectedDate, locale)
            CalendarWindow(
                startDate = start,
                endDateExclusive = start.plusDays(7),
                displayStartDate = start,
                displayEndDateExclusive = start.plusDays(7),
                title = stringResource(R.string.ui_text_59f6071, start.format(dayFmt), start.plusDays(6).format(dayFmt))
            )
        }
        "month" -> {
            val start = selectedDate.withDayOfMonth(1)
            val displayStart = startOfLocaleWeek(start, locale)
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
            title = stringResource(R.string.ui_upcoming_523baab)
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
    return day in startDay..endDay
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

@Composable
private fun eventTimeLabel(event: HACalendarEvent, zone: ZoneId): String {
    if (event.isAllDay()) return stringResource(R.string.widgets_calendar_all_day)
    val locale = appLocale()
    val context = LocalContext.current
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(
        locale,
        if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm"
    )
    val timeFmt = DateTimeFormatter.ofPattern(pattern, locale)
    val start = event.startDateTime(zone) ?: return stringResource(R.string.widgets_calendar_time_unknown)
    val end = event.endDateTime(zone)
    return if (end != null && end.toLocalDate() == start.toLocalDate()) {
        "${start.format(timeFmt)} - ${end.format(timeFmt)}"
    } else {
        start.format(timeFmt)
    }
}

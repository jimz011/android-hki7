@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HKIF1Widget
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

// ─────────────────────────────────────────────────────────────────────────────
// Resolved data
// ─────────────────────────────────────────────────────────────────────────────

private class F1Bundle(
    val nextRace: F1NextRace?,
    val drivers: List<F1DriverStanding>,
    val constructors: List<F1ConstructorStanding>,
    val lastRace: F1LastRace?,
    val weather: F1Weather?,
    val flag: F1Flag,
    val sessionStatus: String?,
    val raceControl: List<F1RaceControlMessage>,
    val season: F1Season?,
    val startingGrid: F1StartingGrid?,
    val driverPredictions: List<F1PredictionRow>,
    val constructorPredictions: List<F1PredictionRow>,
    val liveDrivers: List<F1LiveDriver>,
    val lapCount: F1LapCount?,
    /** False when the f1_sensor integration was not found at all. */
    val available: Boolean
) {
    /**
     * A session is genuinely running, so the Live tab is worth offering.
     *
     * Deliberately an allow-list. Treating "anything that is not inactive/finished/unknown" as live
     * made a track-status sensor sitting at some unrecognised idle wording count as a running
     * session, which is how an empty Live tab and a "Not running" status appeared on a Tuesday.
     */
    val isLive: Boolean
        get() = flag in RACING_FLAGS ||
            sessionStatus?.trim()?.lowercase()?.replace(" ", "") in LIVE_SESSION_STATES

    private companion object {
        /** Flags that only exist while cars are on track. Chequered ends a session, so it is out. */
        val RACING_FLAGS = setOf(
            F1Flag.GREEN, F1Flag.YELLOW, F1Flag.RED,
            F1Flag.SAFETY_CAR, F1Flag.VIRTUAL_SAFETY_CAR
        )
        val LIVE_SESSION_STATES = setOf("started", "running", "active", "live", "inprogress")
    }
}

@Composable
private fun rememberF1Bundle(widget: HKIF1Widget, viewModel: MainViewModel): F1Bundle {
    val registry by viewModel.entityRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    // Every F1 sensor found, whichever config entry it belongs to: the dialog is meant to show
    // all the data there is, and picking an "instance" only ever meant hiding some of it.
    val byKey = remember(registry) { findF1Entities(registry) }
    val ids = remember(byKey) { byKey.values.toList() }
    val entityFlow = remember(viewModel, ids) { viewModel.entitiesFor(ids) }
    val entities by entityFlow.collectAsState()

    fun of(key: String) = byKey[key]?.let { id -> entities.find { it.entity_id == id } }

    return remember(entities, byKey) {
        F1Bundle(
            nextRace = parseNextRace(of(F1Keys.NEXT_RACE)),
            drivers = parseDriverStandings(of(F1Keys.DRIVER_STANDINGS)),
            constructors = parseConstructorStandings(of(F1Keys.CONSTRUCTOR_STANDINGS)),
            lastRace = parseLastRace(of(F1Keys.LAST_RACE)),
            weather = parseWeather(of(F1Keys.WEATHER)),
            flag = parseTrackFlag(of(F1Keys.TRACK_STATUS)),
            sessionStatus = of(F1Keys.SESSION_STATUS)?.state,
            raceControl = parseRaceControl(of(F1Keys.RACE_CONTROL)),
            season = parseCurrentSeason(of(F1Keys.CURRENT_SEASON)),
            startingGrid = parseStartingGrid(of(F1Keys.STARTING_GRID)),
            driverPredictions = parseChampionshipPrediction(of(F1Keys.CHAMPIONSHIP_PREDICTION_DRIVERS)),
            constructorPredictions = parseChampionshipPrediction(of(F1Keys.CHAMPIONSHIP_PREDICTION_TEAMS)),
            liveDrivers = parseLiveDrivers(of(F1Keys.DRIVER_POSITIONS), of(F1Keys.CURRENT_TYRES)),
            lapCount = parseLapCount(of(F1Keys.DRIVER_POSITIONS)),
            available = byKey.isNotEmpty()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatting
// ─────────────────────────────────────────────────────────────────────────────

private fun F1Flag.color(): Color = when (this) {
    F1Flag.GREEN -> Color(0xFF43A047)
    F1Flag.YELLOW -> Color(0xFFFDD835)
    F1Flag.RED -> Color(0xFFE53935)
    F1Flag.SAFETY_CAR -> Color(0xFFFFB300)
    F1Flag.VIRTUAL_SAFETY_CAR -> Color(0xFFFFCA28)
    F1Flag.CHEQUERED -> Color(0xFFECEFF1)
    F1Flag.UNKNOWN -> Color(0xFF90A4AE)
}

@Composable
private fun F1Flag.label(): String = when (this) {
    F1Flag.GREEN -> stringResource(R.string.widgets_f1_flag_green)
    F1Flag.YELLOW -> stringResource(R.string.widgets_f1_flag_yellow)
    F1Flag.RED -> stringResource(R.string.widgets_f1_flag_red)
    F1Flag.SAFETY_CAR -> stringResource(R.string.widgets_f1_flag_safety_car)
    F1Flag.VIRTUAL_SAFETY_CAR -> stringResource(R.string.widgets_f1_flag_vsc)
    F1Flag.CHEQUERED -> stringResource(R.string.widgets_f1_flag_chequered)
    F1Flag.UNKNOWN -> stringResource(R.string.widgets_f1_flag_unknown)
}

/** "3d 4h", "2h 15m", "8m" — coarse at the top end because nobody counts minutes four days out. */
@Composable
private fun countdownLabel(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    return when {
        days > 0 -> stringResource(R.string.widgets_f1_in_days_hours, days, hours)
        duration.toHours() > 0 -> stringResource(R.string.widgets_f1_in_hours_minutes, duration.toHours(), minutes)
        else -> stringResource(R.string.widgets_f1_in_minutes, duration.toMinutes().coerceAtLeast(0))
    }
}

/**
 * When the race is, for the card: "Today 15:00" on the day itself, "Tomorrow 15:00", otherwise the
 * date and time. Shown in the viewer's own timezone — the sensor reports UTC, and a race "at 06:00"
 * is only useful once it is the local hour someone has to be awake for.
 */
@Composable
private fun raceWhenLabel(race: F1NextRace, now: ZonedDateTime): String {
    val zone = ZoneId.systemDefault()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val start = race.raceStart?.withZoneSameInstant(zone) ?: return stringResource(R.string.widgets_f1_time_unknown)
    val time = start.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val today = now.withZoneSameInstant(zone).toLocalDate()
    return when (start.toLocalDate()) {
        today -> stringResource(R.string.widgets_f1_today_at, time)
        today.plusDays(1) -> stringResource(R.string.widgets_f1_tomorrow_at, time)
        else -> start.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", locale))
    }
}

@Composable
private fun sessionLabel(id: String): String = when (id) {
    "first_practice" -> stringResource(R.string.widgets_f1_session_fp1)
    "second_practice" -> stringResource(R.string.widgets_f1_session_fp2)
    "third_practice" -> stringResource(R.string.widgets_f1_session_fp3)
    "sprint_qualifying" -> stringResource(R.string.widgets_f1_session_sprint_quali)
    "sprint" -> stringResource(R.string.widgets_f1_session_sprint)
    "qualifying" -> stringResource(R.string.widgets_f1_session_qualifying)
    "race" -> stringResource(R.string.widgets_f1_session_race)
    else -> id.replace('_', ' ')
}

private fun formatSessionTime(at: ZonedDateTime?, zone: ZoneId, locale: Locale): String {
    val local = at?.withZoneSameInstant(zone) ?: return "—"
    return local.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", locale))
}

// ─────────────────────────────────────────────────────────────────────────────
// Widget card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun F1WidgetItem(
    widget: HKIF1Widget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onUpdate: (HKIF1Widget) -> Unit
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val bundle = rememberF1Bundle(widget, viewModel)
    val currentUrl by viewModel.currentUrl.collectAsState()
    var showDialog by remember(widget.id) { mutableStateOf(false) }

    // Re-render on a timer so the countdown ticks without the sensor having to update. A minute is
    // plenty: the card never shows seconds.
    var minuteTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60.seconds)
            minuteTick++
        }
    }
    val now = remember(minuteTick) { ZonedDateTime.now() }

    Box(Modifier.fillMaxWidth()) {
        F1Card(
            widget = widget,
            bundle = bundle,
            now = now,
            currentUrl = currentUrl,
            modifier = Modifier.clickable(enabled = !isEditMode) { showDialog = true }
        )
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
    if (showDialog) {
        F1Dialog(widget, bundle, now) { showDialog = false }
    }
}

@Composable
private fun F1Card(
    widget: HKIF1Widget,
    bundle: F1Bundle,
    now: ZonedDateTime,
    currentUrl: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val race = bundle.nextRace

    // Just the next race and when it is. Track status deliberately stays out of it: outside a
    // session that reads "Not running", which says nothing anyone opened the card to learn.
    val stateText = when {
        !bundle.available -> stringResource(R.string.widgets_f1_not_found)
        race?.raceName == null -> stringResource(R.string.widgets_f1_no_upcoming)
        else -> stringResource(R.string.widgets_f1_next_in, race.raceName, raceWhenLabel(race, now))
    }
    val dotColor = if (bundle.available) accent else appColors.onMuted

    Surface(
        modifier = modifier.fillMaxWidth()
            .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
            .clip(RoundedCornerShape(widget.cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated)),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box {
            if (!widget.backgroundUrl.isNullOrBlank()) {
                WidgetBackground(widget.backgroundUrl, currentUrl)
            } else {
                // The circuit outline is a white line drawing on transparent, sized as an accent —
                // blown up to fill the card it just reads as a thin, aliased squiggle. Used small
                // and faint on the right it does what it is for: hints at which track this is
                // without competing with the race name. Tinted to onSurface so it stays visible
                // against a light card background instead of vanishing as near-white-on-white.
                race?.circuitOutlineUrl?.let { outline ->
                    AsyncImage(
                        outline, null,
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.72f).padding(end = 12.dp)
                            .alpha(0.16f),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(appColors.onSurface)
                    )
                }
            }

            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
                )
            )

            // Country flag badge, top-right, when there is a race to show one for.
            race?.countryFlagUrl?.let { flagUrl ->
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    AsyncImage(flagUrl, race.country, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            // Same bottom-left pill as the camera, vacuum, waste and parcel cards: title on top,
            // coloured dot plus one line of state under it.
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        widget.title ?: stringResource(R.string.widgets_f1_title),
                        color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).background(dotColor, CircleShape))
                        Text(
                            stateText, color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun F1Dialog(
    widget: HKIF1Widget,
    bundle: F1Bundle,
    now: ZonedDateTime,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    // The Live tab only exists during a session, so a stored default of "live" must fall back
    // rather than opening an empty tab on a Tuesday.
    val tabs = buildList {
        add("next" to stringResource(R.string.widgets_f1_tab_next))
        add("calendar" to stringResource(R.string.widgets_f1_tab_calendar))
        add("standings" to stringResource(R.string.widgets_f1_tab_standings))
        add("grid" to stringResource(R.string.widgets_f1_tab_grid))
        add("results" to stringResource(R.string.widgets_f1_tab_results))
        if (bundle.isLive) add("live" to stringResource(R.string.widgets_f1_tab_live))
    }
    var tab by remember(widget.id, bundle.isLive) {
        mutableStateOf(widget.defaultTab.takeIf { key -> tabs.any { it.first == key } } ?: "next")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        stableHeight = true,
        dismissOnTapOutside = true,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(widget.title ?: stringResource(R.string.widgets_f1_title), modifier = Modifier.weight(1f))
                bundle.season?.year?.takeIf { it.isNotBlank() && it != "unknown" }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = appColors.onMuted)
                }
            }
        },
        text = {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = tabs,
                    selected = tab,
                    onSelect = { tab = it }
                )
                if (!bundle.available) {
                    F1EmptyState(stringResource(R.string.widgets_f1_not_found_long))
                    return@Column
                }
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (tab) {
                        "next" -> F1NextRaceTab(bundle, now)
                        "calendar" -> F1CalendarTab(bundle, now)
                        "standings" -> F1StandingsTab(bundle)
                        "grid" -> F1GridTab(bundle)
                        "results" -> F1ResultsTab(bundle)
                        "live" -> F1LiveTab(bundle)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun F1EmptyState(message: String) {
    val appColors = LocalHKIAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MdiIcon("flag-checkered", tint = appColors.onMuted.copy(alpha = 0.4f), size = 40.dp)
        Text(
            message, color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun F1SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = LocalHKIAppColors.current.onMuted,
        fontWeight = FontWeight.Bold
    )
}

// ── Next race ────────────────────────────────────────────────────────────────

@Composable
private fun F1NextRaceTab(bundle: F1Bundle, now: ZonedDateTime) {
    val appColors = LocalHKIAppColors.current
    val race = bundle.nextRace
    if (race?.raceName == null) {
        F1EmptyState(stringResource(R.string.widgets_f1_no_upcoming))
        return
    }
    val zone = ZoneId.systemDefault()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                race.countryFlagUrl?.let {
                    Surface(shape = RoundedCornerShape(6.dp), modifier = Modifier.size(32.dp)) {
                        AsyncImage(it, race.country, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        race.raceName, color = appColors.onSurface,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    val place = listOfNotNull(race.locality, race.country).joinToString(", ")
                    if (place.isNotBlank()) {
                        Text(place, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                race.round?.let {
                    Text(
                        stringResource(R.string.widgets_f1_round, it),
                        color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            race.circuitName?.let {
                Text(it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            }
            race.timeUntil(now)?.let {
                Text(
                    stringResource(R.string.widgets_f1_lights_out_in, countdownLabel(it)),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                )
            }
        }
    }

    bundle.weather?.let { weather ->
        Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weather.temperature?.let {
                    F1Stat("thermometer", "${it.toInt()}${weather.temperatureUnit ?: "°"}")
                }
                weather.precipitationProbability?.let { F1Stat("weather-rainy", "${it.toInt()}%") }
                weather.windSpeed?.let {
                    F1Stat("weather-windy", "${it.toInt()} ${weather.windSpeedUnit ?: ""}".trim())
                }
                weather.humidity?.let { F1Stat("water-percent", "${it.toInt()}%") }
            }
        }
    }

    if (race.sessions.isNotEmpty()) {
        F1SectionLabel(stringResource(R.string.widgets_f1_schedule))
        race.sessions.forEach { session ->
            val isPast = session.startsAt?.isBefore(now) == true
            Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sessionLabel(session.id),
                        color = if (isPast) appColors.onMuted else appColors.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (session.id == "race") FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatSessionTime(session.startsAt, zone, locale),
                        color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/** "▲3" in green when positions were gained, "▼2" in red when lost. Zero is not worth the ink. */
@Composable
private fun F1DeltaText(delta: Int?) {
    delta?.takeIf { it != 0 }?.let {
        Text(
            if (it > 0) "▲$it" else "▼${-it}",
            color = if (it > 0) Color(0xFF66BB6A) else Color(0xFFEF5350),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun F1Stat(icon: String, value: String) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MdiIcon(icon, tint = appColors.onMuted, size = 16.dp)
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.labelSmall)
    }
}

// ── Season calendar ──────────────────────────────────────────────────────────

@Composable
private fun F1CalendarTab(bundle: F1Bundle, now: ZonedDateTime) {
    val appColors = LocalHKIAppColors.current
    val races = bundle.season?.races.orEmpty()
    if (races.isEmpty()) {
        F1EmptyState(stringResource(R.string.widgets_f1_no_calendar))
        return
    }
    val zone = ZoneId.systemDefault()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val nextRound = bundle.nextRace?.round

    races.forEach { race ->
        val isNext = race.round != null && race.round == nextRound
        val isPast = race.raceStart?.isBefore(now) == true
        Surface(
            shape = itemCornerShape(),
            color = if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else appColors.subtleSurface
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                race.countryFlagUrl?.let {
                    Surface(shape = RoundedCornerShape(4.dp), modifier = Modifier.size(22.dp)) {
                        AsyncImage(it, race.country, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                race.round?.let {
                    Text(
                        it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(18.dp)
                    )
                }
                Text(
                    race.raceName ?: "—",
                    color = if (isPast && !isNext) appColors.onMuted else appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatSessionTime(race.raceStart, zone, locale),
                    color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ── Starting grid ────────────────────────────────────────────────────────────

@Composable
private fun F1GridTab(bundle: F1Bundle) {
    val grid = bundle.startingGrid
    if (grid == null || grid.grid.isEmpty()) {
        F1EmptyState(stringResource(R.string.widgets_f1_no_grid))
        return
    }
    val appColors = LocalHKIAppColors.current
    grid.targetSessionName?.let { F1SectionLabel(it) }
    grid.grid.forEach { row ->
        val podium = when (row.gridPosition) {
            1 -> Color(0xFFFFD54F)
            2 -> Color(0xFFB0BEC5)
            3 -> Color(0xFFBCAAA4)
            else -> null
        }
        Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(24.dp).background(
                        (podium ?: appColors.onMuted).copy(alpha = if (podium != null) 0.85f else 0.16f),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        row.gridPosition?.toString() ?: "–",
                        color = if (podium != null) Color.Black else appColors.onSurface,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        row.driverName ?: row.tla ?: "—", color = appColors.onSurface,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    row.teamName?.let {
                        Text(
                            it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                F1DeltaText(row.gridDelta)
            }
        }
    }
}

// ── Standings ────────────────────────────────────────────────────────────────

@Composable
private fun F1StandingsTab(bundle: F1Bundle) {
    var mode by remember { mutableStateOf("drivers") }
    val appColors = LocalHKIAppColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == "drivers",
            onClick = { mode = "drivers" },
            label = { Text(stringResource(R.string.widgets_f1_drivers), fontSize = 12.sp) }
        )
        FilterChip(
            selected = mode == "constructors",
            onClick = { mode = "constructors" },
            label = { Text(stringResource(R.string.widgets_f1_constructors), fontSize = 12.sp) }
        )
        FilterChip(
            selected = mode == "prediction",
            onClick = { mode = "prediction" },
            label = { Text(stringResource(R.string.widgets_f1_prediction), fontSize = 12.sp) }
        )
    }

    when (mode) {
        "drivers" -> {
            if (bundle.drivers.isEmpty()) {
                F1EmptyState(stringResource(R.string.widgets_f1_no_standings))
                return
            }
            bundle.drivers.forEach { row ->
                F1StandingRow(
                    position = row.position,
                    primary = row.driverName ?: row.driverCode ?: "—",
                    secondary = row.constructor,
                    points = row.points,
                    wins = row.wins
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.widgets_f1_points_wins_legend),
                color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
            )
        }
        "constructors" -> {
            if (bundle.constructors.isEmpty()) {
                F1EmptyState(stringResource(R.string.widgets_f1_no_standings))
                return
            }
            bundle.constructors.forEach { row ->
                F1StandingRow(
                    position = row.position,
                    primary = row.name ?: "—",
                    secondary = row.nationality,
                    points = row.points,
                    wins = row.wins
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.widgets_f1_points_wins_legend),
                color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
            )
        }
        "prediction" -> {
            val rows = bundle.driverPredictions.ifEmpty { bundle.constructorPredictions }
            if (rows.isEmpty()) {
                F1EmptyState(stringResource(R.string.widgets_f1_no_prediction))
                return
            }
            rows.forEach { row ->
                F1StandingRow(
                    position = row.position,
                    primary = row.name ?: "—",
                    secondary = row.currentPoints?.let { stringResource(R.string.widgets_f1_prediction_now, it) },
                    points = row.predictedPoints,
                    wins = null
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.widgets_f1_prediction_legend),
                color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun F1StandingRow(
    position: String?,
    primary: String,
    secondary: String?,
    points: String?,
    wins: String?
) {
    val appColors = LocalHKIAppColors.current
    // The top three get the podium treatment, which is how anyone actually scans a standings table.
    val podium = when (position) {
        "1" -> Color(0xFFFFD54F)
        "2" -> Color(0xFFB0BEC5)
        "3" -> Color(0xFFBCAAA4)
        else -> null
    }
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).background(
                    (podium ?: appColors.onMuted).copy(alpha = if (podium != null) 0.85f else 0.16f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    position ?: "–",
                    color = if (podium != null) Color.Black else appColors.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    primary, color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                secondary?.let {
                    Text(
                        it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    points ?: "0", color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                )
                wins?.takeIf { it != "0" }?.let {
                    Text(
                        stringResource(R.string.widgets_f1_wins_short, it),
                        color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ── Results ──────────────────────────────────────────────────────────────────

@Composable
private fun F1ResultsTab(bundle: F1Bundle) {
    val appColors = LocalHKIAppColors.current
    val race = bundle.lastRace
    if (race == null || race.results.isEmpty()) {
        F1EmptyState(stringResource(R.string.widgets_f1_no_results))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            race.raceName ?: stringResource(R.string.widgets_f1_tab_results),
            color = appColors.onSurface, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        race.circuitName?.let {
            Text(it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.12f))
    race.results.forEach { row ->
        F1ResultRow(row)
    }
}

@Composable
private fun F1ResultRow(row: F1RaceResult) {
    val appColors = LocalHKIAppColors.current
    val podium = when (row.position) {
        "1" -> Color(0xFFFFD54F)
        "2" -> Color(0xFFB0BEC5)
        "3" -> Color(0xFFBCAAA4)
        else -> null
    }
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).background(
                    (podium ?: appColors.onMuted).copy(alpha = if (podium != null) 0.85f else 0.16f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.position ?: "–",
                    color = if (podium != null) Color.Black else appColors.onSurface,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.driverName ?: row.driverCode ?: "—", color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.constructor?.let {
                        Text(
                            it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    F1DeltaText(row.gridDelta)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    row.points ?: "0", color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                )
                row.status?.takeIf { !it.equals("Finished", true) && !it.startsWith("+") }?.let {
                    Text(
                        it, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Live ─────────────────────────────────────────────────────────────────────

@Composable
private fun F1LiveTab(bundle: F1Bundle) {
    val appColors = LocalHKIAppColors.current

    Surface(shape = itemCornerShape(), color = bundle.flag.color().copy(alpha = 0.18f)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(12.dp).background(bundle.flag.color(), CircleShape))
            Column(Modifier.weight(1f)) {
                Text(
                    bundle.flag.label(), color = appColors.onSurface,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                )
                bundle.sessionStatus?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it.replaceFirstChar { c -> c.uppercase() },
                        color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            bundle.lapCount?.currentLap?.let { current ->
                val total = bundle.lapCount.totalLaps
                Text(
                    if (total != null) stringResource(R.string.widgets_f1_lap_of, current, total)
                    else stringResource(R.string.widgets_f1_lap_short, current.toString()),
                    color = appColors.onSurface, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (bundle.liveDrivers.isEmpty() && bundle.raceControl.isEmpty()) {
        F1EmptyState(stringResource(R.string.widgets_f1_no_race_control))
        return
    }

    if (bundle.liveDrivers.isNotEmpty()) {
        F1SectionLabel(stringResource(R.string.widgets_f1_timing))
        bundle.liveDrivers.forEach { driver -> F1LiveDriverRow(driver) }
    }

    if (bundle.raceControl.isNotEmpty()) {
        F1SectionLabel(stringResource(R.string.widgets_f1_race_control))
        bundle.raceControl.take(30).forEach { message ->
            Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    message.lap?.let {
                        Text(
                            stringResource(R.string.widgets_f1_lap_short, it),
                            color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(46.dp)
                        )
                    }
                    Text(
                        message.message, color = appColors.onSurface,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun F1LiveDriverRow(driver: F1LiveDriver) {
    val appColors = LocalHKIAppColors.current
    val statusLabel = when (driver.status?.lowercase()) {
        "pit_in", "pit_out" -> stringResource(R.string.widgets_f1_status_pit)
        "out" -> stringResource(R.string.widgets_f1_status_out)
        else -> null
    }
    Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                driver.position ?: "–", color = appColors.onSurface,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    driver.name ?: driver.tla ?: "—", color = appColors.onSurface,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                statusLabel?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            driver.tyreCompound?.let { compound ->
                Text(
                    listOfNotNull(compound, driver.tyreStintLaps?.toString()).joinToString(" · "),
                    color = appColors.onMuted, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            Text(
                driver.gapToLeader ?: driver.intervalAhead ?: "—",
                color = appColors.onMuted, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun F1WidgetSettingsDialog(
    widget: HKIF1Widget,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIF1Widget) -> Unit
) {
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "flag-checkered") }
    var width by remember(widget) { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var defaultTab by remember(widget) { mutableStateOf(widget.defaultTab) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("sources") }
    var visSpec by remember(widget) { mutableStateOf(widget.toVisibilitySpec()) }

    val registry by viewModel.entityRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }
    val found = remember(registry) { findF1Entities(registry) }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName = it; showIconPicker = false }
        )
    }

    val scroll = rememberScrollState()
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_f1_title),
                stringResource(R.string.widgets_f1_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight().fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "sources" to stringResource(R.string.widgets_tab_data_sources),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "sources") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_data_sources_dadd6ac),
                        stringResource(R.string.widgets_f1_sources_subtitle)
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_title_768e0c1)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    // There are no entity pickers here on purpose: the integration's entity ids
                    // depend on its own naming mode and language, so the widget matches on the
                    // stable translation keys instead. Report what that found.
                    Text(
                        if (found.isEmpty()) stringResource(R.string.widgets_f1_not_found_long)
                        else stringResource(R.string.widgets_f1_found, found.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (found.isEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // F1 Sensor's own setup offers a long list of optional data; say plainly which
                    // of it this widget reads, so the choice there is not a guess.
                    Text(
                        stringResource(R.string.widgets_f1_sensors_used),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(stringResource(R.string.widgets_f1_default_tab), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "next" to R.string.widgets_f1_tab_next,
                            "calendar" to R.string.widgets_f1_tab_calendar,
                            "standings" to R.string.widgets_f1_tab_standings,
                            "grid" to R.string.widgets_f1_tab_grid,
                            "results" to R.string.widgets_f1_tab_results
                        ).forEach { (value, labelRes) ->
                            FilterChip(
                                selected = defaultTab == value,
                                onClick = { defaultTab = value },
                                label = { Text(stringResource(labelRes), fontSize = 12.sp) }
                            )
                        }
                    }
                }
                if (settingsPage == "appearance") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_appearance_41def7a),
                        stringResource(R.string.ui_image_style_size_shape_and_background_40c17b6)
                    )
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                    }
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdiIcon(iconName, size = 20.dp)
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                    WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                    Text(
                        stringResource(R.string.widgets_f1_background_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (settingsPage == "visibility") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_visibility_7d9ff4f),
                        stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66)
                    )
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        width = width,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        backgroundUrl = backgroundUrl,
                        defaultTab = defaultTab,
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

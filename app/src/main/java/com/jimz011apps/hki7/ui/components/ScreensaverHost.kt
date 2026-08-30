package com.jimz011apps.hki7.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.size.Size
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HACalendarEvent
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAServiceCall
import com.jimz011apps.hki7.data.HAWeatherForecast
import com.jimz011apps.hki7.data.ScreensaverAction
import com.jimz011apps.hki7.data.ScreensaverSettings
import com.jimz011apps.hki7.data.isClockPanel
import com.jimz011apps.hki7.data.jsonPrimitiveOrNull
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ScreensaverInk = Color(0xFFFFF4C4)
private val ScreensaverMuted = Color(0xCCFFF4C4)
private val ScreensaverBar = Color(0xCC121212)

@Composable
fun ScreensaverHost(viewModel: MainViewModel) {
    val settings by viewModel.screensaverSettings.collectAsState()
    val visible by viewModel.screensaverVisible.collectAsState()
    val cameraPopup by viewModel.activeCameraPopup.collectAsState()
    LaunchedEffect(settings.enabled) {
        while (isActive && settings.enabled) {
            viewModel.tickScreensaver()
            delay(1_000)
        }
    }
    if (!visible || cameraPopup != null) return
    BackHandler { viewModel.hideScreensaver() }
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    // Hide bars after the first layout so Coil can start at a stable size. Hiding in the same
    // frame as first compose made the edge-to-edge window show the dashboard through the photo.
    LaunchedEffect(Unit) {
        val w = window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(w, w.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        delay(1)
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF101010)).zIndex(10f)) {
        ScreensaverScreen(
        viewModel = viewModel,
        settings = settings,
        onDismiss = { viewModel.hideScreensaver() },
        onAction = { action ->
            val entityId = action.entityId.trim()
            if (entityId.isBlank()) return@ScreensaverScreen
            val domain = entityId.substringBefore('.')
            when (action.service) {
                "open" -> viewModel.callService(
                    domain,
                    if (domain == "cover") "open_cover" else "turn_on",
                    HAServiceCall(entity_id = entityId),
                )
                "close" -> viewModel.callService(
                    domain,
                    if (domain == "cover") "close_cover" else "turn_off",
                    HAServiceCall(entity_id = entityId),
                )
                else -> viewModel.toggleEntity(entityId)
            }
        },
        )
    }
}

@Composable
private fun ScreensaverScreen(
    viewModel: MainViewModel,
    settings: ScreensaverSettings,
    onDismiss: () -> Unit,
    onAction: (ScreensaverAction) -> Unit,
) {
    val entities by viewModel.entities.collectAsState()
    val headerWeather by viewModel.weather.collectAsState()
    val use24h by viewModel.use24hFormat.collectAsState()
    val locale = remember { Locale.getDefault() }
    var clockNow by remember { mutableStateOf(LocalTime.now()) }
    var dateNow by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clockNow = LocalTime.now()
            dateNow = LocalDate.now()
            delay(250)
        }
    }
    val weatherId = settings.weatherEntityId.ifBlank { headerWeather?.entity_id.orEmpty() }
    LaunchedEffect(weatherId) {
        if (weatherId.isNotBlank()) viewModel.fetchWeatherForecastFor(weatherId, "daily")
    }
    val forecastCache by viewModel.weatherForecastCache.collectAsState()
    val forecast = forecastCache["$weatherId:daily"].orEmpty()
    val zone = remember { ZoneId.systemDefault() }
    val dayStart = dateNow.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = dateNow.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
    LaunchedEffect(settings.calendarEntityIds, dayStart) {
        if (settings.calendarEntityIds.isNotEmpty()) {
            viewModel.fetchCalendarEvents(settings.calendarEntityIds, dayStart, dayEnd)
        }
    }
    val calendarKey = viewModel.calendarEventsCacheKey(settings.calendarEntityIds, dayStart, dayEnd)
    val allEvents by viewModel.calendarEvents.collectAsState()
    val events = allEvents[calendarKey].orEmpty()
    val weather = entities.firstOrNull { it.entity_id == weatherId } ?: headerWeather
    val seed = screensaverBackgroundSeed(settings.backgroundRotationMinutes)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    ) {
        if (settings.background == "picsum") {
            ScreensaverPhoto(seed)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
        val panel = settings.isClockPanel()
        val barHeight = if (!panel) 0.dp else if (maxWidth > maxHeight) maxHeight * 0.44f else maxHeight * 0.52f
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ScreensaverClock(clockNow, dateNow, use24h, locale)
            }
            if (panel) {
                ScreensaverInfoBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight),
                    settings = settings,
                    weather = weather,
                    forecast = forecast,
                    entities = entities,
                    events = events,
                    locale = locale,
                    use24h = use24h,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ScreensaverPhoto(seed: Long) {
    val picsumUrl = "https://picsum.photos/seed/hki7-wall-$seed/1920/1080"
    val fallbackUrl = screensaverFallbackUrl(seed)
    var activeUrl by remember(seed) { mutableStateOf(picsumUrl) }
    var loaded by remember(seed) { mutableStateOf(false) }
    val context = LocalContext.current
    val request = remember(activeUrl) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .size(Size.ORIGINAL)
            .httpHeaders(NetworkHeaders.Builder().add("User-Agent", "HKI7 Android").build())
            .build()
    }
    LaunchedEffect(seed) {
        delay(5_000)
        if (!loaded && activeUrl == picsumUrl) activeUrl = fallbackUrl
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        onSuccess = { loaded = true },
        onError = {
            if (activeUrl != fallbackUrl) activeUrl = fallbackUrl
        },
    )
}

private fun screensaverFallbackUrl(seed: Long): String {
    val photos = listOf(
        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=1920&q=80",
    )
    val index = ((seed % photos.size) + photos.size) % photos.size
    return photos[index.toInt()]
}

@Composable
private fun ScreensaverClock(time: LocalTime, date: LocalDate, use24h: Boolean, locale: Locale) {
    val hour = if (use24h) time.hour else ((time.hour + 11) % 12) + 1
    val minutes = "%02d".format(time.minute)
    val seconds = "%02d".format(time.second)
    val dateText = date.format(
        DateTimeFormatter.ofPattern("EEEE d. MMMM", locale)
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "%02d:%s".format(hour, minutes),
                color = ScreensaverInk,
                fontSize = 92.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.shadow(8.dp, ambientColor = Color.Black, spotColor = Color.Black),
            )
            Text(
                text = seconds,
                color = ScreensaverInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 6.dp, top = 14.dp),
            )
        }
        Text(
            text = dateText,
            color = ScreensaverInk,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ScreensaverInfoBar(
    modifier: Modifier,
    settings: ScreensaverSettings,
    weather: HAEntity?,
    forecast: List<HAWeatherForecast>,
    entities: List<HAEntity>,
    events: List<HACalendarEvent>,
    locale: Locale,
    use24h: Boolean,
    onAction: (ScreensaverAction) -> Unit,
) {
    val columns = buildList {
        if (settings.showWeather) add("weather")
        if (settings.showIndoor) add("indoor")
        if (settings.showCalendar) add("calendar")
        if (settings.showActions && settings.actions.isNotEmpty()) add("actions")
    }
    if (columns.isEmpty()) return
    Row(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, ScreensaverBar, ScreensaverBar))
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        columns.forEachIndexed { index, column ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }
            Box(Modifier.weight(if (column == "calendar") 1.85f else 1f).fillMaxHeight()) {
                when (column) {
                    "weather" -> ScreensaverWeather(weather, forecast, locale)
                    "indoor" -> ScreensaverIndoor(settings, entities)
                    "calendar" -> ScreensaverCalendar(events, locale, use24h)
                    "actions" -> ScreensaverActions(settings.actions, onAction)
                }
            }
        }
    }
}

@Composable
private fun ScreensaverWeather(weather: HAEntity?, forecast: List<HAWeatherForecast>, locale: Locale) {
    val temp = weather?.temperature?.roundToInt()
    val condition = weather?.state
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeatherStateIcon(state = condition, size = 42.dp, surface = WeatherAnimationSurface.WIDGET, fallbackTint = ScreensaverInk)
            Column {
                Text(
                    text = temp?.let { "$it°" } ?: "—",
                    color = ScreensaverInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = condition?.let { localizedWeatherStateLabel(it) } ?: stringResource(R.string.settings_screensaver_weather),
                    color = ScreensaverMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            forecast.drop(1).take(6).forEach { day ->
                val label = forecastDayLabel(day.datetime, locale)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(label, color = ScreensaverMuted, fontSize = 11.sp)
                    WeatherStateIcon(state = day.condition, size = 22.dp, surface = WeatherAnimationSurface.FORECAST, fallbackTint = ScreensaverInk, loop = false)
                    Text(
                        "${day.templow?.roundToInt() ?: "–"}°/${day.temperature?.roundToInt() ?: "–"}°",
                        color = ScreensaverInk,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreensaverIndoor(settings: ScreensaverSettings, entities: List<HAEntity>) {
    val temp = entities.firstOrNull { it.entity_id == settings.temperatureEntityId }
    val humidity = entities.firstOrNull { it.entity_id == settings.humidityEntityId }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
        IndoorRow(icon = "thermometer", label = stringResource(R.string.settings_screensaver_temperature), value = formatSensor(temp, "°C"))
        Spacer(Modifier.height(16.dp))
        IndoorRow(icon = "water-percent", label = stringResource(R.string.settings_screensaver_humidity), value = formatSensor(humidity, "%"))
    }
}

@Composable
private fun IndoorRow(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MdiIcon(icon, tint = ScreensaverInk, size = 26.dp)
        Column {
            Text(label, color = ScreensaverMuted, fontSize = 13.sp)
            Text(value, color = ScreensaverInk, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ScreensaverCalendar(events: List<HACalendarEvent>, locale: Locale, use24h: Boolean) {
    val zone = remember { ZoneId.systemDefault() }
    val upcoming = remember(events) {
        events.mapNotNull { event ->
            val start = parseEventStart(event, zone) ?: return@mapNotNull null
            event to start
        }.sortedBy { it.second }
    }
    if (upcoming.isEmpty()) {
        Text(stringResource(R.string.settings_screensaver_no_events), color = ScreensaverMuted, fontSize = 14.sp)
        return
    }
    val timeFmt = DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale)
    val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        upcoming.forEach { (event, start) ->
            val end = parseEventEnd(event, zone)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    start.format(dayFmt),
                    color = ScreensaverMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.width(72.dp),
                    maxLines = 2,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        event.summary ?: "—",
                        color = ScreensaverInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val range = buildString {
                        append(start.format(timeFmt))
                        end?.let { append(" – "); append(it.format(timeFmt)) }
                    }
                    Text(range, color = ScreensaverMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ScreensaverActions(actions: List<ScreensaverAction>, onAction: (ScreensaverAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        actions.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onAction(action) }
                            .padding(8.dp),
                    ) {
                        MdiIcon(action.icon.ifBlank { "circle-outline" }, tint = ScreensaverInk, size = 32.dp)
                        Text(
                            action.label.ifBlank { action.entityId.substringAfter('.', "") },
                            color = ScreensaverInk,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun formatSensor(entity: HAEntity?, fallbackUnit: String): String {
    val raw = entity?.state?.takeUnless { it.equals("unavailable", true) || it.equals("unknown", true) }
        ?: return "—"
    val number = raw.toDoubleOrNull()
    val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitiveOrNull?.contentOrNull ?: fallbackUnit
    return if (number != null) "${if (number % 1.0 == 0.0) number.toInt() else String.format("%.1f", number)} $unit" else raw
}

private fun forecastDayLabel(datetime: String, locale: Locale): String =
    runCatching {
        OffsetDateTime.parse(datetime).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }.recoverCatching {
        Instant.parse(datetime).atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }.getOrElse { datetime.take(2) }

private fun parseEventStart(event: HACalendarEvent, zone: ZoneId): ZonedDateTime? =
    parseCalendarInstant(event.start?.dateTime, event.start?.date, zone)

private fun parseEventEnd(event: HACalendarEvent, zone: ZoneId): ZonedDateTime? =
    parseCalendarInstant(event.end?.dateTime, event.end?.date, zone)

private fun parseCalendarInstant(dateTime: String?, date: String?, zone: ZoneId): ZonedDateTime? {
    if (!dateTime.isNullOrBlank()) {
        return runCatching { OffsetDateTime.parse(dateTime).atZoneSameInstant(zone) }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(dateTime) }.getOrNull()
    }
    if (!date.isNullOrBlank()) {
        return runCatching { LocalDate.parse(date).atStartOfDay(zone) }.getOrNull()
    }
    return null
}

private fun screensaverBackgroundSeed(rotationMinutes: Int): Long {
    val bucket = rotationMinutes.coerceAtLeast(0)
    if (bucket <= 0) return 1L
    return System.currentTimeMillis() / (bucket * 60_000L)
}

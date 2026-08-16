package com.jimz011apps.hki7.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.CLOCK_ANALOG_STYLES
import com.jimz011apps.hki7.data.CLOCK_DIGITAL_STYLES
import com.jimz011apps.hki7.data.CLOCK_MODE_ANALOG
import com.jimz011apps.hki7.data.CLOCK_MODE_DIGITAL
import com.jimz011apps.hki7.data.HKIClockWidget
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.ModernAlertDialog
import com.jimz011apps.hki7.ui.components.VisibilityEditor
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * The next alarm on the device, and the app that owns it.
 *
 * This is the whole of what Android exposes: [AlarmManager.getNextAlarmClock] reports one alarm,
 * the next one due across every app, and there is no public API to enumerate alarms or to switch
 * them on and off. A list with toggles is not something a third-party app can build — which is why
 * the dialog below shows this one alarm and then hands off to the Clock app.
 */
data class DeviceAlarm(val triggerTime: Long, val packageName: String?)

fun nextDeviceAlarm(context: Context): DeviceAlarm? {
    val info = runCatching {
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock
    }.getOrNull() ?: return null
    return DeviceAlarm(
        triggerTime = info.triggerTime,
        packageName = runCatching { info.showIntent?.creatorPackage }.getOrNull()
    )
}

/** Opens whichever app owns the next alarm, falling back to the system's show-alarms intent. */
fun openAlarmApp(context: Context, alarm: DeviceAlarm?) {
    val owner = alarm?.packageName
        ?.let { runCatching { context.packageManager.getLaunchIntentForPackage(it) }.getOrNull() }
    val intent = owner ?: Intent(AlarmClock.ACTION_SHOW_ALARMS)
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private object AlarmClock {
    const val ACTION_SHOW_ALARMS = android.provider.AlarmClock.ACTION_SHOW_ALARMS
}

@Composable
fun ClockWidgetItem(
    widget: HKIClockWidget,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val zone = remember(widget.timeZoneId) {
        widget.timeZoneId?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
    }
    // Ticking once a second only when seconds are shown; otherwise every fifteen, which is enough
    // to land on the right minute without waking the composition sixty times as often for nothing.
    val period = if (widget.showSeconds || widget.mode == CLOCK_MODE_ANALOG) 1000L else 15_000L
    val now by produceState(initialValue = LocalDateTime.now(zone), zone, period) {
        while (true) {
            value = LocalDateTime.now(zone)
            delay(period)
        }
    }
    var showAlarms by remember { mutableStateOf(false) }
    // Read outside the Canvas: a DrawScope lambda is not a composable context, so the theme has
    // to be resolved here and handed in.
    val accent = MaterialTheme.colorScheme.primary
    val ink = appColors.onSurface
    val muted = appColors.onMuted

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (widget.isSquare) Modifier.aspectRatio(1f) else Modifier.aspectRatio(16f / 9f))
                .clip(RoundedCornerShape(widget.cornerRadius.dp))
                .background(surfaceGradient(appColors.elevated))
                .clickable(enabled = !isEditMode) { showAlarms = true },
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = Color.Transparent
        ) {
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) {
                    WidgetBackground(widget.backgroundUrl, "")
                }
                Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    widget.title?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (widget.mode == CLOCK_MODE_ANALOG) {
                        BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                            val side = minOf(maxWidth, maxHeight)
                            Canvas(Modifier.size(side)) {
                                drawAnalogClock(
                                    style = widget.analogStyle,
                                    hour = now.hour,
                                    minute = now.minute,
                                    second = if (widget.showSeconds) now.second else -1,
                                    ink = ink,
                                    muted = muted,
                                    accent = accent
                                )
                            }
                        }
                    } else {
                        DigitalClockFace(widget, now, ink, accent)
                    }
                    val subtitle = clockSubtitle(widget, now)
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            subtitle,
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
        }
    }

    if (showAlarms) {
        NextAlarmDialog(context = context, onDismiss = { showAlarms = false })
    }
}

/** The date line under the face, assembled from whichever parts the user asked for. */
@Composable
private fun clockSubtitle(widget: HKIClockWidget, now: LocalDateTime): String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    if (!widget.showDate && !widget.showDayName && !widget.showYear) return ""
    val pattern = buildString {
        if (widget.showDayName) append("EEEE")
        if (widget.showDate) {
            if (isNotEmpty()) append(", ")
            append("d MMMM")
        }
        if (widget.showYear) {
            if (isNotEmpty()) append(" ")
            append("yyyy")
        }
    }
    return runCatching {
        now.format(DateTimeFormatter.ofPattern(pattern, locale))
    }.getOrDefault("")
}

@Composable
private fun DigitalClockFace(widget: HKIClockWidget, now: LocalDateTime, ink: Color, accent: Color) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val hour = if (widget.use24Hour) now.hour else ((now.hour % 12).takeIf { it != 0 } ?: 12)
    val hh = "%02d".format(locale, hour)
    val mm = "%02d".format(locale, now.minute)
    val ss = "%02d".format(locale, now.second)
    val suffix = if (widget.use24Hour) "" else if (now.hour < 12) "AM" else "PM"

    when (widget.digitalStyle) {
        // Two big blocks stacked, the way a bedside clock reads across a dark room.
        "stacked" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hh, color = ink, fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp)
            Text(mm, color = accent, fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp)
            if (suffix.isNotEmpty()) Text(suffix, color = ink.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        // A seven-segment look, approximated with a wide monospace face and a faint ghost layer
        // behind it — the unlit segments of a real display.
        "segment" -> Box(contentAlignment = Alignment.Center) {
            val text = buildString {
                append("$hh:$mm")
                if (widget.showSeconds) append(":$ss")
            }
            Text(
                text.replace(Regex("[0-9]"), "8"),
                color = ink.copy(alpha = 0.08f),
                fontSize = 40.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
            Text(text, color = accent, fontSize = 40.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
        }
        "mono" -> Text(
            buildString { append("$hh:$mm"); if (widget.showSeconds) append(":$ss"); if (suffix.isNotEmpty()) append(" $suffix") },
            color = ink, fontSize = 36.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium
        )
        // Split-flap: each pair of digits on its own card.
        "flip" -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOfNotNull(hh, mm, ss.takeIf { widget.showSeconds }).forEach { part ->
                Surface(color = ink.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        part,
                        color = ink,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        "outline" -> Text(
            buildString { append("$hh:$mm"); if (widget.showSeconds) append(":$ss") },
            color = Color.Transparent,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.displayMedium.copy(
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                color = ink
            )
        )
        // Colon replaced by a stacked pair of dots that blink on the second.
        "dots" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hh, color = ink, fontSize = 42.sp, fontWeight = FontWeight.Light)
            Column(
                Modifier.padding(horizontal = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val lit = if (now.second % 2 == 0) accent else accent.copy(alpha = 0.25f)
                repeat(2) { Box(Modifier.size(6.dp).background(lit, RoundedCornerShape(3.dp))) }
            }
            Text(mm, color = ink, fontSize = 42.sp, fontWeight = FontWeight.Light)
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.padding(horizontal = 3.dp))
                Text(suffix, color = ink.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
        else -> Text(
            buildString { append("$hh:$mm"); if (widget.showSeconds) append(":$ss"); if (suffix.isNotEmpty()) append(" $suffix") },
            color = ink, fontSize = 40.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Draws one of seven analog faces.
 *
 * They share a hand-drawing routine and differ in their markings, because that is what actually
 * distinguishes a railway clock from a Bauhaus one — the hands are close to the same on both.
 * Everything is sized from the radius so a face is as legible at 90dp in a stack as at 300dp.
 */
private fun DrawScope.drawAnalogClock(
    style: String,
    hour: Int,
    minute: Int,
    second: Int,
    ink: Color,
    muted: Color,
    accent: Color,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension / 2f * 0.94f

    when (style) {
        "minimal" -> {
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90)
                val dot = Offset(center.x + r * 0.86f * cos(rad).toFloat(), center.y + r * 0.86f * sin(rad).toFloat())
                drawCircle(muted.copy(alpha = if (i % 3 == 0) 0.85f else 0.3f), radius = r * (if (i % 3 == 0) 0.035f else 0.018f), center = dot)
            }
        }
        "railway" -> {
            drawCircle(ink.copy(alpha = 0.9f), radius = r, center = center, style = Stroke(width = r * 0.05f))
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val rad = Math.toRadians(i * 6.0 - 90)
                val len = if (major) r * 0.16f else r * 0.06f
                val w = if (major) r * 0.045f else r * 0.014f
                drawLine(
                    ink.copy(alpha = if (major) 0.9f else 0.45f),
                    Offset(center.x + (r - len) * cos(rad).toFloat(), center.y + (r - len) * sin(rad).toFloat()),
                    Offset(center.x + r * 0.92f * cos(rad).toFloat(), center.y + r * 0.92f * sin(rad).toFloat()),
                    strokeWidth = w
                )
            }
        }
        "bauhaus" -> {
            drawCircle(accent.copy(alpha = 0.12f), radius = r * 0.96f, center = center)
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90)
                val len = if (i % 3 == 0) r * 0.26f else r * 0.12f
                drawLine(
                    if (i % 3 == 0) accent else muted.copy(alpha = 0.5f),
                    Offset(center.x + (r - len) * cos(rad).toFloat(), center.y + (r - len) * sin(rad).toFloat()),
                    Offset(center.x + r * 0.9f * cos(rad).toFloat(), center.y + r * 0.9f * sin(rad).toFloat()),
                    strokeWidth = if (i % 3 == 0) r * 0.075f else r * 0.03f,
                    cap = StrokeCap.Butt
                )
            }
        }
        "neon" -> {
            // Two rings, the outer one wide and faint, which is what reads as glow without a shader.
            drawCircle(accent.copy(alpha = 0.18f), radius = r * 0.97f, center = center, style = Stroke(width = r * 0.16f))
            drawCircle(accent, radius = r * 0.97f, center = center, style = Stroke(width = r * 0.035f))
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90)
                drawCircle(accent.copy(alpha = 0.8f), radius = r * 0.028f, center = Offset(center.x + r * 0.8f * cos(rad).toFloat(), center.y + r * 0.8f * sin(rad).toFloat()))
            }
        }
        "skeleton" -> {
            drawCircle(muted.copy(alpha = 0.25f), radius = r * 0.97f, center = center, style = Stroke(width = r * 0.02f))
            drawCircle(muted.copy(alpha = 0.18f), radius = r * 0.62f, center = center, style = Stroke(width = r * 0.015f))
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90)
                drawLine(
                    muted.copy(alpha = 0.4f),
                    Offset(center.x + r * 0.62f * cos(rad).toFloat(), center.y + r * 0.62f * sin(rad).toFloat()),
                    Offset(center.x + r * 0.95f * cos(rad).toFloat(), center.y + r * 0.95f * sin(rad).toFloat()),
                    strokeWidth = r * 0.012f
                )
            }
        }
        "roman" -> {
            // Roman numerals as tick groups: one bar for I, longer for V and X positions. Real
            // glyphs would need text measurement inside a DrawScope, and at widget size the
            // grouping is what the eye actually uses to find twelve o'clock anyway.
            drawCircle(ink.copy(alpha = 0.5f), radius = r * 0.97f, center = center, style = Stroke(width = r * 0.025f))
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90)
                val bars = when (i) { 0 -> 2; 3, 6, 9 -> 3; else -> 1 }
                repeat(bars) { b ->
                    val spread = (b - (bars - 1) / 2f) * r * 0.035f
                    val nx = -sin(rad).toFloat() * spread
                    val ny = cos(rad).toFloat() * spread
                    drawLine(
                        ink.copy(alpha = 0.75f),
                        Offset(center.x + r * 0.78f * cos(rad).toFloat() + nx, center.y + r * 0.78f * sin(rad).toFloat() + ny),
                        Offset(center.x + r * 0.9f * cos(rad).toFloat() + nx, center.y + r * 0.9f * sin(rad).toFloat() + ny),
                        strokeWidth = r * 0.022f
                    )
                }
            }
        }
        else -> { // classic
            drawCircle(muted.copy(alpha = 0.2f), radius = r * 0.97f, center = center, style = Stroke(width = r * 0.03f))
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val rad = Math.toRadians(i * 6.0 - 90)
                val len = if (major) r * 0.13f else r * 0.055f
                drawLine(
                    muted.copy(alpha = if (major) 0.8f else 0.35f),
                    Offset(center.x + (r - len) * cos(rad).toFloat(), center.y + (r - len) * sin(rad).toFloat()),
                    Offset(center.x + r * 0.9f * cos(rad).toFloat(), center.y + r * 0.9f * sin(rad).toFloat()),
                    strokeWidth = if (major) r * 0.035f else r * 0.014f
                )
            }
        }
    }

    // Hands. The hour hand advances with the minutes rather than jumping on the hour, which is the
    // difference between a clock that looks right at 07:59 and one that does not.
    val hourAngle = ((hour % 12) + minute / 60f) * 30f
    val minuteAngle = (minute + (if (second >= 0) second / 60f else 0f)) * 6f
    val slim = style == "minimal" || style == "skeleton"

    fun hand(angle: Float, length: Float, width: Float, color: Color, tail: Float = 0.12f) {
        rotate(degrees = angle, pivot = center) {
            drawLine(
                color,
                Offset(center.x, center.y + r * tail),
                Offset(center.x, center.y - r * length),
                strokeWidth = r * width,
                cap = StrokeCap.Round
            )
        }
    }

    hand(hourAngle, 0.52f, if (slim) 0.035f else 0.062f, ink)
    hand(minuteAngle, 0.78f, if (slim) 0.025f else 0.042f, ink)
    if (second >= 0) {
        hand(second * 6f, 0.84f, 0.014f, accent, tail = 0.2f)
    }
    drawCircle(ink, radius = r * (if (slim) 0.03f else 0.045f), center = center)
    if (second >= 0) drawCircle(accent, radius = r * 0.022f, center = center)
}

/**
 * What Android will tell us about alarms: the next one, and a way into the app that owns it.
 * There is no list and there are no toggles, so the dialog says what it knows and then gets out of
 * the way rather than pretending to be an alarm manager.
 */
@Composable
private fun NextAlarmDialog(context: Context, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val locale = context.resources.configuration.locales[0]
    val alarm = remember { nextDeviceAlarm(context) }
    val label = alarm?.let {
        runCatching {
            Instant.ofEpochMilli(it.triggerTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(locale))
        }.getOrNull()
    }
    val ownerName = alarm?.packageName?.let { pkg ->
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
    }

    com.jimz011apps.hki7.ui.components.ModernAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clock_alarms_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (label == null) {
                    Text(
                        stringResource(R.string.clock_no_alarm),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(label, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    if (ownerName != null) {
                        Text(
                            stringResource(R.string.clock_alarm_set_by, ownerName),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    stringResource(R.string.clock_alarms_platform_note),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { openAlarmApp(context, alarm); onDismiss() }) {
                Text(stringResource(R.string.clock_open_clock_app))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_close_bbfa773))
            }
        }
    )
}

@Composable
fun ClockWidgetSettingsDialog(
    widget: HKIClockWidget,
    onDismiss: () -> Unit,
    onSave: (HKIClockWidget) -> Unit
) {
    var title by remember(widget) { mutableStateOf(widget.title.orEmpty()) }
    var mode by remember(widget) { mutableStateOf(widget.mode) }
    var analogStyle by remember(widget) { mutableStateOf(widget.analogStyle) }
    var digitalStyle by remember(widget) { mutableStateOf(widget.digitalStyle) }
    var use24 by remember(widget) { mutableStateOf(widget.use24Hour) }
    var seconds by remember(widget) { mutableStateOf(widget.showSeconds) }
    var showDate by remember(widget) { mutableStateOf(widget.showDate) }
    var showDay by remember(widget) { mutableStateOf(widget.showDayName) }
    var showYear by remember(widget) { mutableStateOf(widget.showYear) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var square by remember(widget) { mutableStateOf(widget.isSquare) }
    val radius = widget.cornerRadius
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var settingsPage by remember(widget) { mutableStateOf("content") }
    var visSpec by remember(widget) { mutableStateOf(widget.toVisibilitySpec()) }

    ModernAlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.clock_widget_title),
                stringResource(R.string.clock_widget_subtitle)
            )
        },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "content" to stringResource(R.string.widgets_tab_content),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.clock_face_section),
                        stringResource(R.string.clock_face_section_subtitle)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.clock_title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mode == CLOCK_MODE_ANALOG,
                            onClick = { mode = CLOCK_MODE_ANALOG },
                            label = { Text(stringResource(R.string.clock_mode_analog)) }
                        )
                        FilterChip(
                            selected = mode == CLOCK_MODE_DIGITAL,
                            onClick = { mode = CLOCK_MODE_DIGITAL },
                            label = { Text(stringResource(R.string.clock_mode_digital)) }
                        )
                    }
                    Text(stringResource(R.string.clock_style), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val styles = if (mode == CLOCK_MODE_ANALOG) CLOCK_ANALOG_STYLES else CLOCK_DIGITAL_STYLES
                        val selected = if (mode == CLOCK_MODE_ANALOG) analogStyle else digitalStyle
                        styles.forEach { style ->
                            FilterChip(
                                selected = selected == style,
                                onClick = {
                                    if (mode == CLOCK_MODE_ANALOG) analogStyle = style else digitalStyle = style
                                },
                                label = { Text(clockStyleLabel(style)) }
                            )
                        }
                    }
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.clock_shows_section),
                        stringResource(R.string.clock_shows_section_subtitle)
                    )
                    // 12/24 is a digital-only choice: an analog face has no am/pm to place.
                    if (mode == CLOCK_MODE_DIGITAL) {
                        ClockToggleRow(stringResource(R.string.clock_24_hour), use24) { use24 = it }
                    }
                    ClockToggleRow(stringResource(R.string.clock_seconds), seconds) { seconds = it }
                    ClockToggleRow(stringResource(R.string.clock_day_name), showDay) { showDay = it }
                    ClockToggleRow(stringResource(R.string.clock_date), showDate) { showDate = it }
                    ClockToggleRow(stringResource(R.string.clock_year), showYear) { showYear = it }
                }
                if (settingsPage == "appearance") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_appearance_41def7a),
                        stringResource(R.string.ui_size_shape_and_background_24dd9b6)
                    )
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !square, onClick = { square = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = square, onClick = { square = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                    }
                    WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
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
                        title = title.takeIf { it.isNotBlank() },
                        mode = mode,
                        analogStyle = analogStyle,
                        digitalStyle = digitalStyle,
                        use24Hour = use24,
                        showSeconds = seconds,
                        showDate = showDate,
                        showDayName = showDay,
                        showYear = showYear,
                        width = width,
                        isSquare = square,
                        cornerRadius = radius,
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
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

@Composable
private fun ClockToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun clockStyleLabel(style: String): String = stringResource(
    when (style) {
        "classic" -> R.string.clock_style_classic
        "minimal" -> R.string.clock_style_minimal
        "roman" -> R.string.clock_style_roman
        "railway" -> R.string.clock_style_railway
        "bauhaus" -> R.string.clock_style_bauhaus
        "neon" -> R.string.clock_style_neon
        "skeleton" -> R.string.clock_style_skeleton
        "plain" -> R.string.clock_style_plain
        "segment" -> R.string.clock_style_segment
        "mono" -> R.string.clock_style_mono
        "flip" -> R.string.clock_style_flip
        "outline" -> R.string.clock_style_outline
        "stacked" -> R.string.clock_style_stacked
        else -> R.string.clock_style_dots
    }
)

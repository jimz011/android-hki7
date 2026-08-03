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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HACalendarEvent
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.isButtonVisibleNow
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIWasteCollectionWidget
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class WasteCategory(
    val entity: HAEntity,
    /** What the user sees — translated where a fraction is recognised. */
    val name: String,
    /**
     * The untranslated sensor name, kept because the icon and colour are chosen by looking for
     * fraction keywords like "pmd" or "gft". Matching those against [name] only works while the
     * app runs in Dutch: "PMD" translated to "Packaging" contains none of them, so every fraction
     * silently fell through to the grey-blue default bin.
     */
    val matchName: String,
    val date: LocalDate?
)

/** Resolves a waste sensor's next pickup date from Afvalbeheer-style attributes or its state. */
private fun wasteNextDate(entity: HAEntity, zone: ZoneId): LocalDate? {
    val today = LocalDate.now(zone)
    entity.attributes?.let { attrs ->
        listOf("Year_month_day_date", "year_month_day_date").forEach { key ->
            attrs[key]?.jsonPrimitive?.contentOrNull?.let { raw ->
                runCatching { return LocalDate.parse(raw.take(10)) }
            }
        }
        listOf("Days_until", "days_until").forEach { key ->
            attrs[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let { return today.plusDays(it.toLong()) }
        }
    }
    val state = entity.state.trim()
    when (state.lowercase()) {
        "vandaag", "today" -> return today
        "morgen", "tomorrow" -> return today.plusDays(1)
        "unknown", "unavailable", "geen", "none", "" -> return null
    }
    // The state may carry a leading day name ("woensdag, 15-07-2026").
    val candidates = listOf(state, state.substringAfter(", ").trim())
    val patterns = listOf("yyyy-MM-dd", "dd-MM-yyyy", "d-M-yyyy", "dd/MM/yyyy", "yyyyMMdd")
    for (candidate in candidates) for (pattern in patterns) {
        runCatching { return LocalDate.parse(candidate.take(10), DateTimeFormatter.ofPattern(pattern)) }
    }
    // Day-month without a year: assume the next occurrence.
    for (candidate in candidates) {
        runCatching {
            val parsed = DateTimeFormatter.ofPattern("dd-MM").parse(candidate)
            val date = LocalDate.of(
                today.year,
                parsed.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                parsed.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            )
            return if (date < today) date.plusYears(1) else date
        }
    }
    return null
}

/** MDI icon slug for a waste fraction, based on its (cleaned) name. */
private fun wasteCategoryIcon(name: String): String {
    val n = name.lowercase()
    return when {
        listOf("gft", "groen", "organic", "bio").any(n::contains) -> "leaf"
        listOf("papier", "paper", "karton", "carton").any(n::contains) -> "newspaper-variant-outline"
        listOf("pmd", "plastic", "pbd", "verpakking").any(n::contains) -> "recycle"
        listOf("glas", "glass").any(n::contains) -> "bottle-wine-outline"
        listOf("textiel", "textile", "kleding").any(n::contains) -> "tshirt-crew-outline"
        listOf("kerstbo", "tree").any(n::contains) -> "pine-tree"
        listOf("grofvuil", "bulky").any(n::contains) -> "sofa-outline"
        else -> "trash-can-outline"
    }
}

/** Resolves the sensor's entity_picture against the HA base URL, when it has one. */
private fun wasteEntityPicture(entity: HAEntity, currentUrl: String): String? =
    entity.entityPicture?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}$it"
    }

private fun wasteCategoryColor(name: String): Color {
    val n = name.lowercase()
    return when {
        listOf("gft", "groen", "organic", "bio").any(n::contains) -> Color(0xFF66BB6A)
        listOf("papier", "paper", "karton", "carton").any(n::contains) -> Color(0xFF42A5F5)
        listOf("pmd", "plastic", "pbd", "verpakking").any(n::contains) -> Color(0xFFFF9800)
        listOf("rest", "grijs", "grey", "residual").any(n::contains) -> Color(0xFF8E8E93)
        listOf("glas", "glass").any(n::contains) -> Color(0xFF26A69A)
        listOf("textiel", "textile", "kleding").any(n::contains) -> Color(0xFF7E57C2)
        else -> Color(0xFF29B6F6)
    }
}

@Composable
private fun wasteDateLabel(date: LocalDate, zone: ZoneId): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return when (date) {
        LocalDate.now(zone) -> stringResource(R.string.widgets_today)
        LocalDate.now(zone).plusDays(1) -> stringResource(R.string.widgets_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    }
}

/** Canonical waste-type label when the sensor name contains a known fraction keyword. */
@Composable
private fun wasteShortName(raw: String): String? {
    val n = raw.lowercase()
    val label = when {
        n.contains("gft") -> R.string.widgets_waste_organic
        n.contains("pmd") -> R.string.widgets_waste_packaging
        n.contains("papier") || n.contains("paper") -> R.string.widgets_waste_paper
        n.contains("restafval") || n.contains("rest") -> R.string.widgets_waste_residual
        n.contains("glas") || n.contains("glass") -> R.string.widgets_waste_glass
        n.contains("textiel") -> R.string.widgets_waste_textiles
        n.contains("plastic") -> R.string.widgets_waste_plastic
        n.contains("kerstbo") -> R.string.widgets_waste_christmas_trees
        n.contains("grofvuil") -> R.string.widgets_waste_bulky
        n.contains("duobak") -> R.string.widgets_waste_duo_bin
        else -> null
    }
    return label?.let { stringResource(it) }
}

/** Drops the shared leading words (the integration/collector name) from a set of sensor names. */
private fun stripCommonPrefix(names: List<String>): List<String> {
    if (names.size < 2) return names
    val tokenLists = names.map { name -> name.split(" ").filter { it.isNotBlank() } }
    val maxPrefix = tokenLists.minOf { it.size } - 1
    var prefixLen = 0
    while (prefixLen < maxPrefix && tokenLists.all { it[prefixLen].equals(tokenLists[0][prefixLen], ignoreCase = true) }) prefixLen++
    return tokenLists.map { tokens -> tokens.drop(prefixLen).joinToString(" ").ifBlank { tokens.joinToString(" ") } }
}

/** Sensors that look like waste-collection entities (Afvalbeheer etc.); all sensors when none match. */
fun wasteSensorCandidates(allEntities: List<HAEntity>): List<HAEntity> {
    val keywords = listOf("afval", "waste", "trash", "garbage", "gft", "pmd", "papier", "restafval", "recycl", "milieu", "kliko")
    val sensors = allEntities.filter { it.entity_id.startsWith("sensor.") }
    val matches = sensors.filter { entity ->
        val haystack = "${entity.entity_id} ${entity.friendlyName.orEmpty()}".lowercase()
        keywords.any(haystack::contains)
    }
    return matches.ifEmpty { sensors }
}

// ─────────────────────────────────────────────────────────────────────────────
// Widget card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WasteCollectionWidgetItem(
    widget: HKIWasteCollectionWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onUpdate: (HKIWasteCollectionWidget) -> Unit
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val zone = ZoneId.systemDefault()
    val entityFlow = remember(viewModel, widget.entityIds, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(widget.entityIds) else viewModel.entitiesFor(widget.entityIds)
    }
    val entities by entityFlow.collectAsState()
    val resolved = remember(entities, widget.entityIds, widget.itemConfigs) {
        widget.entityIds
            .filter { isButtonVisibleNow(widget.itemConfigs[it] ?: HKIButtonConfig()) }
            .mapNotNull { id -> entities.find { it.entity_id == id } }
    }
    val rawNames = resolved.map { it.friendlyName ?: it.entity_id.substringAfter('.') }
    val strippedNames = remember(rawNames) { stripCommonPrefix(rawNames) }
    val categories = resolved.mapIndexed { index, entity ->
        WasteCategory(
            entity,
            wasteShortName(rawNames[index]) ?: strippedNames[index],
            rawNames[index],
            wasteNextDate(entity, zone)
        )
    }.sortedWith(compareBy(nullsLast(naturalOrder())) { it.date })
    var showDialog by remember(widget.id) { mutableStateOf(false) }
    val currentUrl by viewModel.currentUrl.collectAsState()

    Box(Modifier.fillMaxWidth()) {
        WasteCollectionCard(
            widget = widget,
            categories = categories,
            zone = zone,
            currentUrl = currentUrl,
            modifier = Modifier.clickable(enabled = !isEditMode) { showDialog = true }
        )
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
    if (showDialog) {
        WasteCollectionDialog(widget, categories, viewModel, zone, currentUrl) { showDialog = false }
    }
}

@Composable
private fun WasteCollectionCard(
    widget: HKIWasteCollectionWidget,
    categories: List<WasteCategory>,
    zone: ZoneId,
    currentUrl: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val next = categories.firstOrNull { it.date != null }
    val nextDate = next?.date
    // Every fraction collected on the soonest upcoming day — shown as overlapping icons when > 1.
    val todays = categories.filter { it.date != null && it.date == nextDate }
    val nextNames = todays.joinToString(" · ") { it.name }
    val accent = next?.let { wasteCategoryColor(it.matchName) } ?: appColors.onMuted
    val stateText = when {
        widget.entityIds.isEmpty() -> stringResource(R.string.ui_no_waste_sensors_selected_68b51ce)
        nextDate == null -> stringResource(R.string.ui_no_upcoming_collections_feff2a6)
        else -> stringResource(R.string.ui_text_c1aacd9, nextNames, wasteDateLabel(nextDate, zone))
    }

    // Same footprint and label placement as the camera/vacuum widgets: 16:9 (or square) card
    // with the name + state overlay in the bottom-left corner.
    Surface(
        modifier = modifier.fillMaxWidth()
            .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
            .clip(RoundedCornerShape(widget.cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated)),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box {
            // A configured background image replaces the default artwork.
            if (!widget.backgroundUrl.isNullOrBlank()) {
                WidgetBackground(widget.backgroundUrl, currentUrl)
            } else if (todays.size > 1) {
                // Multiple fractions on the same day: overlap their icons, like the Parcels widget.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-16).dp), verticalAlignment = Alignment.CenterVertically) {
                        todays.take(4).forEach { WasteFractionBadge(it, widget.imageStyle == "picture", currentUrl, 68) }
                    }
                }
            } else {
                // Artwork: the upcoming fraction's entity_picture, or its type icon.
                val picture = if (widget.imageStyle == "picture") next?.entity?.let { wasteEntityPicture(it, currentUrl) } else null
                if (picture != null) {
                    AsyncImage(picture, next?.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Rounded-square tile, matching the overlapping multi-fraction badges above.
                        Box(
                            Modifier.size(84.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape(21.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            MdiIcon(next?.let { wasteCategoryIcon(it.matchName) } ?: widget.icon ?: "trash-can-outline", tint = accent, size = 44.dp)
                        }
                    }
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
                )
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        widget.title ?: stringResource(R.string.ui_waste_collection_7cdf205),
                        color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Dot carries the color of the upcoming/today's fraction.
                        Box(
                            Modifier.size(5.dp).background(
                                if (nextDate != null) accent else Color.White.copy(alpha = 0.5f), CircleShape
                            )
                        )
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

/** One overlapping fraction badge for a same-day collection: the entity picture (when using picture
 * artwork) on a white tile, otherwise the fraction's coloured type icon. Mirrors the Parcels widget's
 * carrier logos so multiple collections on one day read at a glance. */
@Composable
private fun WasteFractionBadge(category: WasteCategory, usePicture: Boolean, currentUrl: String, size: Int) {
    val color = wasteCategoryColor(category.matchName)
    val picture = if (usePicture) category.entity?.let { wasteEntityPicture(it, currentUrl) } else null
    Surface(
        shape = RoundedCornerShape((size / 4).dp),
        color = if (picture != null) Color.White else color.copy(alpha = 0.18f),
        shadowElevation = 3.dp,
        modifier = Modifier.size(size.dp)
    ) {
        if (picture != null) {
            AsyncImage(picture, category.name, Modifier.fillMaxSize().padding((size / 10).dp), contentScale = ContentScale.Fit)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MdiIcon(wasteCategoryIcon(category.matchName), tint = color, size = (size / 2).dp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog: all categories + optional week-calendar overview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WasteCollectionDialog(
    widget: HKIWasteCollectionWidget,
    categories: List<WasteCategory>,
    viewModel: MainViewModel,
    zone: ZoneId,
    currentUrl: String,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val today = LocalDate.now(zone)

    AlertDialog(
        onDismissRequest = onDismiss,
        stableHeight = true,
        dismissOnTapOutside = true,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(widget.title ?: stringResource(R.string.ui_waste_collection_7cdf205), modifier = Modifier.weight(1f))
            }
        },
        text = {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (categories.isEmpty()) {
                    Text(
                        stringResource(R.string.ui_no_waste_sensors_selected_add_them_via_the_widget_554ff3d),
                        color = appColors.onMuted, style = MaterialTheme.typography.bodySmall
                    )
                }
                categories.forEach { category ->
                    val color = wasteCategoryColor(category.matchName)
                    val isToday = category.date == today
                    Surface(shape = itemCornerShape(), color = if (isToday) color.copy(alpha = 0.14f) else appColors.subtleSurface) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val picture = if (widget.imageStyle == "picture") wasteEntityPicture(category.entity, currentUrl) else null
                            if (picture != null) {
                                Box(Modifier.size(32.dp).clip(itemCornerShape()).background(color.copy(alpha = 0.16f))) {
                                    AsyncImage(picture, category.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            } else {
                                Surface(shape = itemCornerShape(), color = color.copy(alpha = 0.16f)) {
                                    MdiIcon(wasteCategoryIcon(category.matchName), tint = color, size = 18.dp, modifier = Modifier.padding(7.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                category.name, color = appColors.onSurface, style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                category.date?.let { wasteDateLabel(it, zone) } ?: stringResource(R.string.ui_unknown_bc7819b),
                                color = if (isToday) color else appColors.onMuted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                widget.calendarEntityId?.let { calendarId ->
                    HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.12f))
                    WasteWeekCalendar(calendarId, viewModel, zone)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun WasteWeekCalendar(calendarEntityId: String, viewModel: MainViewModel, zone: ZoneId) {
    val appColors = LocalHKIAppColors.current
    val today = LocalDate.now(zone)
    val endExclusive = today.plusDays(7)
    val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    val ids = remember(calendarEntityId) { listOf(calendarEntityId) }
    val cacheKey = remember(ids, startMillis, endMillis) { viewModel.calendarEventsCacheKey(ids, startMillis, endMillis) }
    val eventFlow = remember(viewModel, cacheKey) { viewModel.calendarEventsFor(cacheKey) }
    val events by eventFlow.collectAsState()
    LaunchedEffect(cacheKey) { viewModel.fetchCalendarEvents(ids, startMillis, endMillis) }

    val byDay = remember(events, calendarEntityId, today) {
        events.asSequence().filter { it.entityId == calendarEntityId }
            .mapNotNull { event -> wasteEventDate(event, zone)?.let { it to event } }
            .filter { (date, _) -> date in today..<endExclusive }
            .sortedBy { it.first }
            .groupBy({ it.first }, { it.second })
    }
    if (byDay.isEmpty()) {
        Text(stringResource(R.string.ui_no_collections_in_the_next_7_days_6d48bf4), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val collectionLabel = stringResource(R.string.ui_collection_30c54a9)
        byDay.forEach { (date, dayEvents) ->
            Surface(shape = itemCornerShape(), color = appColors.subtleSurface) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        wasteDateLabel(date, zone),
                        color = appColors.onSurface, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp), maxLines = 1
                    )
                    Text(
                        dayEvents.joinToString(", ") { it.summary?.takeIf { s -> s.isNotBlank() } ?: collectionLabel },
                        color = appColors.onMuted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun wasteEventDate(event: HACalendarEvent, zone: ZoneId): LocalDate? {
    event.start?.date?.let { runCatching { return LocalDate.parse(it) } }
    event.start?.dateTime?.let { raw ->
        runCatching { return OffsetDateTime.parse(raw).atZoneSameInstant(zone).toLocalDate() }
        runCatching { return LocalDateTime.parse(raw).atZone(zone).toLocalDate() }
    }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
// Pickers & settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WasteEntityPickerDialog(
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    AdvancedEntitySearchDialog(
        allEntities = wasteSensorCandidates(allEntities),
        title = stringResource(R.string.ui_select_waste_sensors_6006d5f),
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = onSelected
    )
}

@Composable
fun WasteCollectionSettingsDialog(
    widget: HKIWasteCollectionWidget,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIWasteCollectionWidget) -> Unit
) {
    var entityIds by remember(widget) { mutableStateOf(widget.entityIds) }
    var calendarEntityId by remember(widget) { mutableStateOf(widget.calendarEntityId) }
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "trash-can-outline") }
    var imageStyle by remember(widget) { mutableStateOf(widget.imageStyle) }
    var width by remember(widget) { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("sources") }
    var itemConfigs by remember(widget) { mutableStateOf(widget.itemConfigs) }
    var editingItemVisibility by remember { mutableStateOf<String?>(null) }
    var visSpec by remember(widget) {
        mutableStateOf(
            widget.toVisibilitySpec()
        )
    }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = wasteSensorCandidates(allEntities),
            title = stringResource(R.string.ui_select_waste_sensors_6006d5f),
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { entityIds = it; showEntityPicker = false }
        )
    }
    if (showCalendarPicker) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("calendar.") },
            title = stringResource(R.string.ui_select_week_calendar_640f382),
            singleSelect = true,
            preselectedIds = setOfNotNull(calendarEntityId),
            onDismiss = { showCalendarPicker = false },
            onEntitiesSelected = { ids -> calendarEntityId = ids.firstOrNull(); showCalendarPicker = false }
        )
    }
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
                stringResource(R.string.widgets_waste_collection_title),
                stringResource(R.string.widgets_waste_collection_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
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
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_data_sources_dadd6ac), stringResource(R.string.ui_sensors_and_an_optional_week_calendar_399bad3))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_768e0c1)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.ui_waste_sensors_8bfd3ec), style = MaterialTheme.typography.labelLarge)
                if (entityIds.isEmpty()) {
                    Text(
                        stringResource(R.string.ui_none_selected_5798946),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entityIds.forEach { id ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            allEntities.find { it.entity_id == id }?.friendlyName ?: id,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { editingItemVisibility = id }) {
                            Icon(
                                if (isButtonVisibleNow(itemConfigs[id] ?: HKIButtonConfig())) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = stringResource(R.string.ui_visibility_7d9ff4f)
                            )
                        }
                        IconButton(onClick = { entityIds = entityIds - id; itemConfigs = itemConfigs - id }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.action_remove))
                        }
                    }
                }
                TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                Text(stringResource(R.string.ui_week_calendar_shown_in_the_dialog_6d4f28b), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        calendarEntityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id } ?: stringResource(R.string.ui_not_set_93039e6),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (calendarEntityId == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showCalendarPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    if (calendarEntityId != null) TextButton(onClick = { calendarEntityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) }
                }
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_image_style_size_shape_and_background_40c17b6))
                Text(stringResource(R.string.ui_image_50e19fd), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = imageStyle == "icon", onClick = { imageStyle = "icon" }, label = { Text(stringResource(R.string.ui_type_icon_839142a)) })
                    FilterChip(selected = imageStyle == "picture", onClick = { imageStyle = "picture" }, label = { Text(stringResource(R.string.ui_sensor_picture_c3a0ed2)) })
                }
                Text(
                    if (imageStyle == "picture") stringResource(R.string.widgets_waste_sensor_picture_help)
                    else stringResource(R.string.ui_shows_an_icon_matching_the_waste_type_gft_pmd_1966fe0),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
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
                }
                if (settingsPage == "visibility") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        entityIds = entityIds,
                        calendarEntityId = calendarEntityId,
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        imageStyle = imageStyle,
                        width = width,
                        isSquare = isSquare,
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
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
    editingItemVisibility?.let { id ->
        com.jimz011apps.hki7.ui.components.ItemVisibilityDialog(
            label = allEntities.find { it.entity_id == id }?.friendlyName ?: id,
            config = itemConfigs[id] ?: HKIButtonConfig(),
            onDismiss = { editingItemVisibility = null },
            onSave = { itemConfigs = itemConfigs + (id to it) }
        )
    }
}

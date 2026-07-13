package com.example.hki7.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.hki7.data.*
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.DevicePickerDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.WidgetBackground
import com.example.hki7.ui.components.WidgetBackgroundSelector
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ParcelCarrier(
    val key: String,
    val name: String,
    val deviceId: String,
    val entities: List<HAEntity>,
    val incoming: Int,
    val outgoing: Int,
    val logoUrl: String?,
    val baseUrl: String,
    val accessToken: String
) {
    val supportsLetters: Boolean get() = entities.any { entity ->
        val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
        !entity.entity_id.startsWith("image.") &&
            (label.contains("letters", true) || label.contains("brieven", true))
    }
    val currentLetterCount: Int get() = letters.count(::isCurrentOrFutureLetter)

    val parcels: List<JsonObject> get() {
        val individual = entities.mapNotNull { entity ->
            val attributes = entity.attributes?.takeIf { entity.parcelAttribute("barcode") != null } ?: return@mapNotNull null
            val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
            buildJsonObject {
                attributes.forEach { (key, value) -> put(key, value) }
                put("_direction", if (label.contains("outgoing", true)) "Outgoing" else "Incoming")
            }
        }
        val summaries = entities.flatMap { entity ->
            val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
            val direction = if (label.contains("outgoing", true)) "Outgoing" else "Incoming"
            (entity.attributes?.get("parcels") as? JsonArray)?.mapNotNull { item ->
                (item as? JsonObject)?.let { parcel -> buildJsonObject { parcel.forEach { (key, value) -> put(key, value) }; put("_direction", direction) } }
            }.orEmpty()
        }
        return (individual + summaries).distinctBy { parcel ->
            parcel["barcode"]?.jsonPrimitive?.contentOrNull
                ?: listOf("sender", "receiver", "planned_from", "status").joinToString("|") { parcel[it]?.jsonPrimitive?.contentOrNull.orEmpty() }
        }
    }
    val letters: List<JsonObject> get() {
        val fromSensors = entities.filter { entity ->
            val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
            label.contains("letters", true) || label.contains("brieven", true)
        }.flatMap { entity -> extractObjectList(entity.attributes) }
        val fromImages = entities.filter { it.entity_id.startsWith("image.") }.mapNotNull { entity ->
            val attrs = entity.attributes ?: return@mapNotNull null
            val id = attrs["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            buildJsonObject {
                put("id", id)
                put("title", attrs["title"]?.jsonPrimitive?.contentOrNull ?: entity.friendlyName ?: "Mail")
                attrs["date"]?.jsonPrimitive?.contentOrNull?.let { put("date", it) }
                attrs["unread"]?.jsonPrimitive?.booleanOrNull?.let { put("unread", it) }
            }
        }
        return (fromSensors + fromImages).distinctBy { it["id"]?.jsonPrimitive?.contentOrNull ?: it["title"]?.jsonPrimitive?.contentOrNull }
    }

    fun letterImage(letter: JsonObject): String? {
        val id = letter["id"]?.jsonPrimitive?.contentOrNull
        val title = letter["title"]?.jsonPrimitive?.contentOrNull
        return entities.firstOrNull { entity ->
            entity.entity_id.startsWith("image.") && (
                (id != null && entity.attributes?.get("id")?.jsonPrimitive?.contentOrNull == id) ||
                (title != null && entity.friendlyName?.contains(title, true) == true)
            )
        }?.let { entity ->
            (entity.entityPicture ?: entity.state.takeIf { state -> state.startsWith("/") || state.startsWith("http") })
                ?.let { if (it.startsWith("http")) it else "${baseUrl.removeSuffix("/")}/${it.removePrefix("/")}" }
        }
    }
}

private enum class ParcelTab(val title: String) {
    Incoming("Incoming"), Delivered("Delivered"), Outgoing("Outgoing"), Letters("Letters")
}

private fun JsonObject.parcelValue(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

private fun JsonObject.isDeliveredParcel(): Boolean {
    val status = listOfNotNull(parcelValue("status"), parcelValue("raw_status")).joinToString(" ")
    return status.contains("delivered", true) || status.contains("bezorgd", true)
}

private fun JsonObject.isOutgoingParcel(): Boolean =
    parcelValue("_direction")?.contains("outgoing", true) == true

private fun isCurrentOrFutureLetter(letter: JsonObject, today: LocalDate = LocalDate.now()): Boolean {
    val attributeDate = letter["date"]?.jsonPrimitive?.contentOrNull
    val fullDate = attributeDate?.let { value ->
        runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
    }
    if (fullDate != null) return !fullDate.isBefore(today)

    val title = letter["title"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: return false
    val match = Regex("(\\d{1,2})\\s+([\\p{L}]+)(?:\\s+(\\d{4}))?").find(title) ?: return false
    val day = match.groupValues[1].toIntOrNull() ?: return false
    val month = when (match.groupValues[2].take(3)) {
        "jan" -> 1; "feb" -> 2; "maa", "mar" -> 3; "apr" -> 4
        "mei", "may" -> 5; "jun" -> 6; "jul" -> 7; "aug" -> 8
        "sep" -> 9; "okt", "oct" -> 10; "nov" -> 11; "dec" -> 12
        else -> return false
    }
    val explicitYear = match.groupValues[3].toIntOrNull()
    var date = runCatching { LocalDate.of(explicitYear ?: today.year, month, day) }.getOrNull() ?: return false
    // A January announcement shown in December refers to the upcoming year.
    if (explicitYear == null && today.monthValue == 12 && month == 1) date = date.plusYears(1)
    return !date.isBefore(today)
}

private fun HAEntity.parcelAttribute(name: String): String? =
    attributes?.get(name)?.jsonPrimitive?.contentOrNull

private fun carrierKey(text: String): String = when {
    text.contains("postnl", true) -> "postnl"
    text.contains("dhl", true) -> "dhl_nl"
    text.contains("dpd", true) -> "dpd"
    text.contains("gls", true) -> "gls"
    else -> "parcel"
}

private fun carrierName(key: String) = when (key) {
    "postnl" -> "PostNL"; "dhl_nl" -> "DHL"; "dpd" -> "DPD"; "gls" -> "GLS"; else -> "Carrier"
}

private fun carrierLogo(key: String): String? = when (key) {
    "postnl" -> "https://raw.githubusercontent.com/jonisnet/hki-parcels-card/main/images/postnl/postnl-logo.png"
    "dhl_nl" -> "https://raw.githubusercontent.com/jonisnet/hki-parcels-card/main/images/dhl/DHL_logo.png"
    "dpd" -> "https://raw.githubusercontent.com/jonisnet/hki-parcels-card/main/images/dpd/DPD_logo.png"
    "gls" -> "https://raw.githubusercontent.com/jonisnet/hki-parcels-card/main/images/gls/GLS_logo.png"
    else -> null
}

private fun extractObjectList(attributes: JsonObject?): List<JsonObject> {
    attributes ?: return emptyList()
    val preferred = listOf("letters", "brieven", "items", "shipments", "parcels")
    preferred.forEach { key ->
        (attributes[key] as? JsonArray)?.mapNotNull { it as? JsonObject }?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return attributes.values.filterIsInstance<JsonArray>().firstNotNullOfOrNull { array ->
        array.mapNotNull { it as? JsonObject }.takeIf { it.isNotEmpty() }
    }.orEmpty()
}

private fun countEntity(entities: List<HAEntity>, word: String): Int = entities
    .firstOrNull { entity ->
        val text = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
        text.contains(word, true) && text.contains("parcel", true) && !text.contains("delivered", true) && entity.parcelAttribute("barcode") == null
    }?.state?.toIntOrNull() ?: 0

private fun resolveParcelCarriers(
    deviceIds: List<String>,
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>,
    customImages: Map<String, String>,
    currentUrl: String,
    accessToken: String
): List<ParcelCarrier> {
    val entityDevice = registry.associate { it.entity_id to it.device_id }
    return deviceIds.distinct().mapNotNull { deviceId ->
        val device = devices.firstOrNull { it.id == deviceId }
        val directEntities = entities.filter { entityDevice[it.entity_id] == deviceId }
        val letterPrefixes = directEntities.mapNotNull { entity ->
            Regex("^sensor\\.(.+)_(?:letters|brieven)$", RegexOption.IGNORE_CASE).find(entity.entity_id)?.groupValues?.getOrNull(1)
        }
        if (directEntities.isEmpty()) return@mapNotNull null
        val hint = listOfNotNull(device?.name_by_user, device?.name, device?.manufacturer, directEntities.firstOrNull()?.friendlyName).joinToString(" ")
        val key = carrierKey(hint)
        val deviceEntities = (directEntities + entities.filter { entity ->
            if (!entity.entity_id.startsWith("image.")) return@filter false
            val matchesSensorPrefix = letterPrefixes.any { prefix ->
                entity.entity_id.startsWith("image.${prefix}_letter", true)
            }
            val isPostNlLetterImage = key == "postnl" &&
                entity.attributes?.get("id")?.jsonPrimitive?.contentOrNull != null
            matchesSensorPrefix || isPostNlLetterImage
        }).distinctBy { it.entity_id }
        val configured = customImages[deviceId]?.takeIf { it.isNotBlank() }
        val image = (configured ?: carrierLogo(key))?.let { if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}/${it.removePrefix("/")}" }
        ParcelCarrier(key, carrierName(key), deviceId, deviceEntities,
            countEntity(deviceEntities, "incoming"), countEntity(deviceEntities, "outgoing"), image, currentUrl, accessToken)
    }
}

@Composable
fun ParcelsWidgetItem(
    widget: HKIParcelsWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val entities by viewModel.entities.collectAsState()
    val registry by viewModel.entityRegistry.collectAsState()
    val devices by viewModel.deviceRegistry.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val hasUnresolvedDevices = widget.deviceIds.any { deviceId ->
        devices.none { it.id == deviceId } || registry.none { it.device_id == deviceId }
    }
    LaunchedEffect(widget.deviceIds, hasUnresolvedDevices) {
        if (widget.deviceIds.isNotEmpty() && hasUnresolvedDevices) {
            viewModel.fetchRegistries(force = true)
            delay(1_500)
            val deviceRegistry = viewModel.deviceRegistry.value
            val entityRegistry = viewModel.entityRegistry.value
            if (widget.deviceIds.any { id -> deviceRegistry.none { it.id == id } || entityRegistry.none { it.device_id == id } }) {
                viewModel.fetchRegistries(force = true)
            }
        }
    }
    val carriers = remember(widget.deviceIds, widget.carrierImageUrls, entities, registry, devices, currentUrl, accessToken) {
        resolveParcelCarriers(widget.deviceIds, entities, registry, devices, widget.carrierImageUrls, currentUrl, accessToken.orEmpty())
    }
    val incoming = carriers.sumOf { it.incoming }
    val outgoing = carriers.sumOf { it.outgoing }
    val supportsLetters = carriers.any { it.supportsLetters }
    val letters = carriers.sumOf { it.currentLetterCount }
    var showDialog by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
                .clip(RoundedCornerShape(widget.cornerRadius.dp))
                .clickable(enabled = !isEditMode) { showDialog = true },
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = appColors.elevated
        ) {
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) {
                    WidgetBackground(widget.backgroundUrl, currentUrl)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
                            carriers.take(4).forEach { CarrierLogo(it, 56) }
                            if (carriers.isEmpty()) {
                                Box(Modifier.size(84.dp).background(Color(0xFF60A5FA).copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) {
                                    MdiIcon(widget.icon, tint = Color(0xFF60A5FA), size = 44.dp)
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(.88f)))))
                Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.Black.copy(.55f), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(widget.title ?: "Parcels", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        val summary = buildString {
                            append("$incoming incoming · $outgoing outgoing")
                            if (supportsLetters) append(" · $letters letters")
                        }
                        Text(summary, color = Color.White.copy(.7f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center).size(28.dp)) {
                Icon(Icons.Default.Settings, "Parcel settings", tint = appColors.onSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (showDialog) ParcelDialog(carriers, onDismiss = { showDialog = false })
}

@Composable
private fun CarrierLogo(carrier: ParcelCarrier, size: Int) {
    Surface(shape = RoundedCornerShape((size / 4).dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.size(size.dp)) {
        val logoUrl = carrier.logoUrl
        if (logoUrl != null) ParcelAsyncImage(carrier, logoUrl, carrier.name, Modifier.fillMaxSize().padding(7.dp), ContentScale.Fit)
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalShipping, null) }
    }
}

@Composable
private fun ParcelAsyncImage(
    carrier: ParcelCarrier,
    url: String,
    description: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val model = remember(url, carrier.baseUrl, carrier.accessToken) {
        val builder = ImageRequest.Builder(context).data(url)
        if (carrier.accessToken.isNotBlank() && url.startsWith(carrier.baseUrl.removeSuffix("/"), ignoreCase = true)) {
            builder.httpHeaders(NetworkHeaders.Builder().add("Authorization", "Bearer ${carrier.accessToken}").build())
        }
        builder.build()
    }
    AsyncImage(model, description, modifier, contentScale = contentScale)
}

@Composable
private fun ParcelDialog(carriers: List<ParcelCarrier>, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    var selectedCarrierId by remember(carriers) { mutableStateOf(if (carriers.size == 1) carriers.firstOrNull()?.deviceId else null) }
    val carrier = carriers.firstOrNull { it.deviceId == selectedCarrierId }
    var tab by remember(selectedCarrierId) { mutableStateOf(ParcelTab.Incoming) }
    var selectedParcel by remember(selectedCarrierId) { mutableStateOf<JsonObject?>(null) }
    var selectedHistory by remember(selectedParcel) { mutableStateOf<JsonObject?>(null) }
    var parcelDetail by remember(selectedCarrierId) { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<JsonObject?>(null) }
    LaunchedEffect(carrier?.deviceId, carrier?.parcels?.size, tab) {
        val parcelsForTab = carrier?.parcels.orEmpty().filter { parcel ->
            when (tab) {
                ParcelTab.Incoming -> !parcel.isDeliveredParcel() && !parcel.isOutgoingParcel()
                ParcelTab.Delivered -> parcel.isDeliveredParcel()
                ParcelTab.Outgoing -> !parcel.isDeliveredParcel() && parcel.isOutgoingParcel()
                ParcelTab.Letters -> false
            }
        }
        if (selectedParcel !in parcelsForTab) selectedParcel = parcelsForTab.firstOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false)
    ) {
        BackHandler {
            when {
                parcelDetail -> {
                    parcelDetail = false
                    selectedHistory = null
                }
                carrier != null && carriers.size > 1 -> selectedCarrierId = null
                else -> onDismiss()
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().height(640.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.surface, contentColor = appColors.onSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!parcelDetail && carrier != null && carriers.size > 1) IconButton(onClick = { selectedCarrierId = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Carriers")
                    }
                    Text(carrier?.name ?: "Parcels", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                when {
                    carriers.isEmpty() -> Text("Choose one or more carrier devices in widget settings.", color = appColors.onMuted)
                    carrier == null -> {
                        val scroll = rememberScrollState()
                        Column(Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            carriers.forEach { item ->
                                Surface(shape = RoundedCornerShape(18.dp), color = appColors.subtleSurface, contentColor = appColors.onSurface,
                                    modifier = Modifier.fillMaxWidth().clickable { selectedCarrierId = item.deviceId }) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        CarrierLogo(item, 56)
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Bold)
                                            val letterSummary = if (item.supportsLetters) " · ${item.currentLetterCount} letters" else ""
                                            Text("${item.incoming} incoming · ${item.outgoing} outgoing$letterSummary", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (parcelDetail) {
                            ParcelHero(carrier, selectedParcel, selectedHistory)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { parcelDetail = false; selectedHistory = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to parcels")
                                }
                                Text("Tracking history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            val historyScroll = rememberScrollState()
                            Column(Modifier.weight(1f).fadingEdges(historyScroll).verticalScroll(historyScroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedParcel?.let { parcel ->
                                    ParcelHistoryList(parcel, selectedHistory) { selectedHistory = it }
                                }
                            }
                        } else {
                        val incomingParcels = carrier.parcels.filter { !it.isDeliveredParcel() && !it.isOutgoingParcel() }
                        val deliveredParcels = carrier.parcels.filter { it.isDeliveredParcel() }
                        val outgoingParcels = carrier.parcels.filter { !it.isDeliveredParcel() && it.isOutgoingParcel() }
                        val availableTabs = if (carrier.supportsLetters) ParcelTab.entries else ParcelTab.entries.filterNot { it == ParcelTab.Letters }
                        ParcelHero(carrier, selectedParcel, selectedHistory)
                        PrimaryTabRow(selectedTabIndex = availableTabs.indexOf(tab).coerceAtLeast(0), containerColor = Color.Transparent) {
                            availableTabs.forEach { item ->
                                val count = when (item) {
                                    ParcelTab.Incoming -> incomingParcels.size
                                    ParcelTab.Delivered -> deliveredParcels.size
                                    ParcelTab.Outgoing -> outgoingParcels.size
                                    ParcelTab.Letters -> carrier.currentLetterCount
                                }
                                Tab(tab == item, {
                                    tab = item
                                    selectedHistory = null
                                }, text = { Text("${item.title} ($count)", maxLines = 1) })
                            }
                        }
                        val scroll = rememberScrollState()
                        Column(Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (tab != ParcelTab.Letters) {
                                val visibleParcels = when (tab) {
                                    ParcelTab.Incoming -> incomingParcels
                                    ParcelTab.Delivered -> deliveredParcels
                                    ParcelTab.Outgoing -> outgoingParcels
                                    ParcelTab.Letters -> emptyList()
                                }
                                if (visibleParcels.isEmpty()) Text(
                                    "No ${tab.title.lowercase()} parcels.",
                                    color = appColors.onMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                )
                                visibleParcels.forEach { parcel ->
                                    InteractiveParcelRow(parcel, parcel == selectedParcel) {
                                        selectedParcel = parcel
                                        selectedHistory = null
                                        parcelDetail = true
                                    }
                                }
                            } else {
                                if (carrier.letters.isEmpty()) Text(
                                    "No announced letters.",
                                    color = appColors.onMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                )
                                carrier.letters.forEach { letter ->
                                    val image = carrier.letterImage(letter)
                                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth().clickable { selectedLetter = letter }) {
                                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (image != null) ParcelAsyncImage(carrier, image, null, Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Crop)
                                            Column(Modifier.weight(1f)) {
                                                Text(letter["title"]?.jsonPrimitive?.contentOrNull ?: "Mail", fontWeight = FontWeight.SemiBold)
                                                Text(letter["date"]?.jsonPrimitive?.contentOrNull?.let(::formatParcelTime) ?: "Announced letter", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        }
    }
    selectedLetter?.let { letter -> LetterViewerDialog(carrier, letter) { selectedLetter = null } }
}

@Composable
private fun ParcelHistoryList(parcel: JsonObject, selected: JsonObject?, onSelected: (JsonObject) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val history = parcel["history"] as? JsonArray
    if (history.isNullOrEmpty()) {
        Text("No status history (enable parcel history in the integration options)", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        return
    }
    history.reversed().forEach { item ->
        val event = item as? JsonObject ?: return@forEach
        val eventTitle = event["raw_status"]?.jsonPrimitive?.contentOrNull ?: event["status"]?.jsonPrimitive?.contentOrNull ?: "Update"
        val eventTime = event["timestamp"]?.jsonPrimitive?.contentOrNull?.let(::formatParcelTime).orEmpty()
        val selectedColors = if (event == selected) {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = selectedColors.first,
            contentColor = selectedColors.second,
            modifier = Modifier.fillMaxWidth().clickable { onSelected(event) }
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(eventTitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(eventTime, color = if (event == selected) selectedColors.second.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ParcelHero(carrier: ParcelCarrier, parcel: JsonObject?, history: JsonObject?) {
    fun JsonObject.attr(name: String) = this[name]?.jsonPrimitive?.contentOrNull
    val status = history?.attr("status") ?: parcel?.attr("status") ?: "unknown"
    val rawStatus = history?.attr("raw_status") ?: parcel?.attr("raw_status") ?: status.replace('_', ' ')
    val moment = history?.attr("timestamp")?.let(::formatParcelTime)
        ?: listOfNotNull(parcel?.attr("planned_from")?.let(::formatParcelTime), parcel?.attr("planned_to")?.let(::formatParcelTime)).joinToString(" – ")
    val stage = when (status) { "registered" -> 0; "in_transit" -> 1; "out_for_delivery", "at_pickup_point" -> 2; "delivered" -> 3; else -> 1 }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(190.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarrierLogo(carrier, 58)
                Column(Modifier.weight(1f)) {
                    Text(parcel?.attr("sender") ?: parcel?.attr("receiver") ?: parcel?.attr("barcode") ?: "No parcel selected", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(rawStatus.replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                if (moment.isNotBlank()) Column(horizontalAlignment = Alignment.End) {
                    Text(if (history != null) "History" else "Expected delivery", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(moment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(Icons.Default.Inventory2, Icons.Default.Warehouse, Icons.Default.LocalShipping, Icons.Default.Home).forEachIndexed { index, icon ->
                    Surface(shape = CircleShape, color = if (index <= stage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (index <= stage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                    }
                    if (index < 3) Box(Modifier.weight(1f).height(3.dp).background(if (index < stage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Registered", "Sorting", "On the way", "Delivered").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun InteractiveParcelRow(parcel: JsonObject, selected: Boolean, onClick: () -> Unit) {
    fun attr(name: String) = parcel[name]?.jsonPrimitive?.contentOrNull
    val title = attr("sender") ?: attr("receiver") ?: attr("barcode") ?: "Parcel"
    val status = attr("raw_status") ?: attr("status") ?: "Unknown status"
    val schedule = listOfNotNull(attr("planned_from")?.let(::formatParcelTime), attr("planned_to")?.let(::formatParcelTime)).joinToString(" – ")
    Surface(shape = RoundedCornerShape(16.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status.replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (schedule.isNotBlank()) Text(schedule, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LetterViewerDialog(carrier: ParcelCarrier?, letter: JsonObject, onDismiss: () -> Unit) {
    val image = carrier?.letterImage(letter)
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().height(640.dp), shape = RoundedCornerShape(30.dp), elevation = CardDefaults.cardElevation(16.dp)) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(letter["title"]?.jsonPrimitive?.contentOrNull ?: "Mail", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    if (image != null) ParcelAsyncImage(carrier, image, null, Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)), ContentScale.Fit)
                    else Text("No letter image available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(letter["date"]?.jsonPrimitive?.contentOrNull?.let(::formatParcelTime).orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ParcelDialogLegacy(carriers: List<ParcelCarrier>, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    var selected by remember(carriers) { mutableStateOf(if (carriers.size == 1) carriers.firstOrNull()?.deviceId else null) }
    val carrier = carriers.firstOrNull { it.deviceId == selected }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (carrier != null && carriers.size > 1) IconButton(onClick = { selected = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Carriers") }
                Text(carrier?.name ?: "Parcels", modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
        },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.heightIn(max = 520.dp).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (carriers.isEmpty()) Text("Choose one or more carrier devices in widget settings.", color = appColors.onMuted)
                else if (carrier == null) carriers.forEach { item ->
                    Surface(shape = RoundedCornerShape(18.dp), color = appColors.subtleSurface,
                        modifier = Modifier.fillMaxWidth().clickable { selected = item.deviceId }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CarrierLogo(item, 48)
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                                Text("${item.incoming} incoming · ${item.outgoing} outgoing", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryPill("Incoming", carrier.incoming, Modifier.weight(1f))
                        SummaryPill("Outgoing", carrier.outgoing, Modifier.weight(1f))
                        if (carrier.letters.isNotEmpty()) SummaryPill("Mail", carrier.letters.size, Modifier.weight(1f))
                    }
                    if (carrier.parcels.isEmpty() && carrier.letters.isEmpty()) Text("No active parcels or mail.", color = appColors.onMuted)
                    carrier.parcels.forEach { ParcelRow(it) }
                    carrier.letters.forEach { letter ->
                        val image = carrier.letterImage(letter)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (image != null) AsyncImage(image, null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Box(Modifier.weight(1f)) {
                                DetailRow(letter["title"]?.jsonPrimitive?.contentOrNull ?: "Mail",
                                    letter["date"]?.jsonPrimitive?.contentOrNull?.let(::formatParcelTime) ?: "Announced letter")
                            }
                        }
                    }
                }
            }
        }, confirmButton = {}
    )
}

@Composable private fun SummaryPill(label: String, count: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable private fun ParcelRow(parcel: JsonObject) {
    fun attr(name: String) = parcel[name]?.jsonPrimitive?.contentOrNull
    val title = attr("sender") ?: attr("receiver") ?: attr("barcode") ?: "Parcel"
    val status = attr("raw_status") ?: attr("status") ?: "Unknown status"
    val direction = attr("_direction")
    val from = attr("planned_from")
    val to = attr("planned_to")
    val schedule = listOfNotNull(from?.let(::formatParcelTime), to?.let(::formatParcelTime)).joinToString(" – ")
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status.replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (direction != null) Text(direction, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            if (schedule.isNotBlank()) Text(schedule, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            attr("pickup_point")?.let { Text("Pickup: $it", style = MaterialTheme.typography.bodySmall) }
            val history = parcel["history"] as? JsonArray
            if (!history.isNullOrEmpty()) {
                Text("History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                history.takeLast(4).reversed().forEach { item ->
                    val obj = item as? JsonObject ?: return@forEach
                    DetailRow(obj["raw_status"]?.jsonPrimitive?.contentOrNull ?: obj["status"]?.jsonPrimitive?.contentOrNull ?: "Update",
                        obj["timestamp"]?.jsonPrimitive?.contentOrNull?.let(::formatParcelTime).orEmpty())
                }
            } else {
                Text("No status history (enable parcel history in the integration options)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun DetailRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatParcelTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm"))
}.getOrDefault(value)

/** Device picker that waits for the HA registries instead of presenting an empty search list. */
@Composable
fun ParcelDevicePickerDialog(
    viewModel: MainViewModel,
    currentId: String? = null,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    val devices by viewModel.deviceRegistry.collectAsState()
    LaunchedEffect(devices.isEmpty()) {
        if (devices.isEmpty()) {
            viewModel.fetchRegistries(force = true)
            // A second request covers a socket that was still reconnecting during the first one.
            delay(1_500)
            if (viewModel.deviceRegistry.value.isEmpty()) viewModel.fetchRegistries(force = true)
        }
    }
    if (devices.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Device") },
            text = {
                Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    } else {
        DevicePickerDialog(devices, currentId, onDismiss, onSelected)
    }
}

@Composable
fun ParcelsWidgetSettingsDialog(
    widget: HKIParcelsWidget,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIParcelsWidget) -> Unit
) {
    val devices by viewModel.deviceRegistry.collectAsState()
    var deviceIds by remember(widget) { mutableStateOf(widget.deviceIds) }
    var title by remember(widget) { mutableStateOf(widget.title.orEmpty()) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var square by remember(widget) { mutableStateOf(widget.isSquare) }
    var radius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var imageUrls by remember(widget) { mutableStateOf(widget.carrierImageUrls) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var picking by remember { mutableStateOf(false) }
    val hasUnresolvedDevices = deviceIds.any { id -> devices.none { it.id == id } }
    LaunchedEffect(deviceIds, hasUnresolvedDevices) {
        if (deviceIds.isNotEmpty() && hasUnresolvedDevices) {
            viewModel.fetchRegistries(force = true)
            delay(1_500)
            if (deviceIds.any { id -> viewModel.deviceRegistry.value.none { it.id == id } }) {
                viewModel.fetchRegistries(force = true)
            }
        }
    }
    if (picking) {
        ParcelDevicePickerDialog(viewModel, null, { picking = false }) { id ->
            if (id != null) deviceIds = (deviceIds + id).distinct()
            picking = false
        }
        // Do not compose the settings AlertDialog over the device picker.
        return
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Parcels Widget") }, text = {
        val scroll = rememberScrollState()
        Column(Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Carrier devices", style = MaterialTheme.typography.labelLarge)
            deviceIds.forEach { id ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val device = devices.firstOrNull { it.id == id }
                        Text(device?.let { it.name_by_user ?: it.name } ?: "Loading device...", modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { deviceIds = deviceIds - id; imageUrls = imageUrls - id }) { Icon(Icons.Default.Close, "Remove") }
                    }
                    OutlinedTextField(
                        value = imageUrls[id].orEmpty(),
                        onValueChange = { value -> imageUrls = if (value.isBlank()) imageUrls - id else imageUrls + (id to value) },
                        label = { Text("Logo URL or HA/local path (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TextButton(onClick = { picking = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add carrier device") }
            WidgetWidthSelector(width, { width = it })
            Text("Shape", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!square, { square = false }, label = { Text("Standard") })
                FilterChip(square, { square = true }, label = { Text("Square") })
            }
            Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8 to "Sharp", 20 to "Modern", 28 to "Round").forEach { (value, label) -> FilterChip(radius == value, { radius = value }, label = { Text(label) }) }
            }
            WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
        }
    }, confirmButton = { Button(onClick = { onSave(widget.copy(deviceIds = deviceIds, carrierImageUrls = imageUrls, title = title.ifBlank { "Parcels" }, width = width, isSquare = square, cornerRadius = radius, backgroundUrl = backgroundUrl)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

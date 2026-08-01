@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.jimz011apps.hki7.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warehouse
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
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.jimz011apps.hki7.data.*
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.DevicePickerDialog
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.swipeToAdjacentTab
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class ParcelCarrier(
    val key: String,
    val name: String,
    val deviceId: String,
    val entities: List<HAEntity>,
    val incoming: Int,
    val outgoing: Int,
    val logoUrl: String?,
    val baseUrl: String,
    val accessToken: String,
    /** The Home Assistant integration domain (e.g. "gls", "postnl"), from the entity registry
     *  platform. Determines the brand logo and whether manual parcel-adding is supported. */
    val domain: String = key,
    /** Stable Home Assistant registry translation keys indexed by entity id. */
    val translationKeys: Map<String, String> = emptyMap()
) {
    val deliveredCount: Int get() = parcels.count { it.isDeliveredParcel() }
    private val hasParcelCollections: Boolean get() = entities.any { entity ->
        listOf("parcels", "shipments", "packages", "enroute", "delivered")
            .any { key -> entity.attributes?.get(key) is JsonArray }
    }
    val incomingCount: Int get() = if (hasParcelCollections) {
        parcels.count { !it.isDeliveredParcel() && !it.isOutgoingParcel() }
    } else {
        incoming
    }
    val outgoingCount: Int get() = if (hasParcelCollections) {
        parcels.count { !it.isDeliveredParcel() && it.isOutgoingParcel() }
    } else {
        outgoing
    }
    /** True when this carrier's integration exposes `<domain>.track_parcel` (manual add by number). */
    val supportsManualAdd: Boolean get() = domain in TRACK_PARCEL_DOMAINS

    val supportsLetters: Boolean get() = entities.any { entity ->
        val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
        val role = translationKeys[entity.entity_id].orEmpty()
        !entity.entity_id.startsWith("image.") && (
            role.contains("letters", true) ||
                label.contains("letters", true) ||
                label.contains("brieven", true)
            )
    }
    val currentLetterCount: Int get() = letters.count(::isCurrentOrFutureLetter)

    val parcels: List<JsonObject> get() = collectCarrierParcels(entities, translationKeys, domain)
    val letters: List<JsonObject> get() {
        val fromSensors = entities.filter { entity ->
            val label = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
            translationKeys[entity.entity_id]?.contains("letters", true) == true ||
                label.contains("letters", true) ||
                label.contains("brieven", true)
        }.flatMap { entity -> extractObjectList(entity.attributes) }
        val fromImages = entities.filter { it.entity_id.startsWith("image.") }.mapNotNull { entity ->
            val attrs = entity.attributes ?: return@mapNotNull null
            val id = attrs["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            buildJsonObject {
                put("id", id)
                (attrs["title"]?.jsonPrimitive?.contentOrNull ?: entity.friendlyName)?.let { put("title", it) }
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

private enum class ParcelTab(@StringRes val titleRes: Int, @StringRes val emptyRes: Int?) {
    Incoming(R.string.parcel_incoming, R.string.parcel_no_incoming),
    Delivered(R.string.parcel_delivered, R.string.parcel_no_delivered),
    Outgoing(R.string.parcel_outgoing, R.string.parcel_no_outgoing),
    Letters(R.string.parcel_letters, null)
}

private fun JsonObject.parcelValue(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

@Composable
private fun localizedParcelStatus(status: String): String =
    when (status.trim().lowercase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
        "registered" -> stringResource(R.string.parcel_stage_registered)
        "in_transit" -> stringResource(R.string.parcel_status_in_transit)
        "out_for_delivery" -> stringResource(R.string.parcel_status_out_for_delivery)
        "at_pickup_point", "ready_for_pickup" ->
            stringResource(R.string.parcel_status_at_pickup_point)
        "ready_to_send" -> stringResource(R.string.parcel_status_ready_to_send)
        "delivered" -> stringResource(R.string.parcel_delivered)
        "unknown", "unavailable" -> stringResource(R.string.parcel_unknown_status)
        else -> status.replace('_', ' ').replaceFirstChar(Char::uppercase)
    }

/**
 * Carrier integrations sometimes expose `raw_status` as an untranslated machine code (e.g.
 * PostNL/DHL's `PARCEL_ARRIVED_AT_LOCAL_DEPOT`) rather than prose. Turn SCREAMING_SNAKE_CASE codes
 * into a normal sentence; leave anything that already reads like prose untouched.
 */
private fun normalizeRawStatus(text: String): String {
    val looksLikeCode = (text.contains('_') || text.contains('-')) && text.none(Char::isLowerCase)
    val normalized = if (looksLikeCode) {
        text.replace('_', ' ').replace('-', ' ').lowercase(Locale.ROOT)
    } else {
        text
    }
    return normalized.trim().replaceFirstChar(Char::uppercase)
}

private fun JsonObject.isDeliveredParcel(): Boolean {
    val explicitlyDelivered = this["delivered"]?.jsonPrimitive?.let { value ->
        value.booleanOrNull ?: (value.contentOrNull?.let { it == "1" || it.equals("true", ignoreCase = true) })
    } == true
    val status = listOfNotNull(parcelValue("status"), parcelValue("raw_status")).joinToString(" ")
    return explicitlyDelivered ||
        status.contains("delivered", true) ||
        status.contains("bezorgd", true) ||
        status.contains("afgeleverd", true)
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
    val match = Regex("""(\d{1,2})\s+(\p{L}+)(?:\s+(\d{4}))?""").find(title) ?: return false
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

private val PARCEL_PAYLOAD_MARKERS = setOf(
    "status", "raw_status", "delivered", "delivered_at", "planned_from", "planned_to",
    "pickup", "pickup_point", "url", "weight", "dimensions", "history", "raw"
)

private val PARCEL_LABEL_MARKERS = listOf(
    "parcel", "package", "shipment", "pakket", "paket", "colis", "paquete", "pacco", "kargo"
)

private fun HAEntity.isIndividualParcelEntity(translationKey: String?): Boolean {
    val attributes = attributes ?: return false
    if (parcelAttribute("barcode").isNullOrBlank()) return false

    // Current ha-parcel-integrations releases expose a stable registry translation key. It is a
    // stronger signal than an entity id or localized friendly name: summary/helper sensors such
    // as `next_delivery` can expose most of the same top-level attributes as a parcel.
    if (!translationKey.isNullOrBlank()) {
        return translationKey == "parcel"
    }

    // Summary/helper sensors may also expose a barcode. In particular, PostNL's next-delivery
    // sensor only contains a barcode and sender; treating it as the parcel shadows the complete
    // object from the incoming-parcels sensor during de-duplication.
    if (entity_id.contains("next_delivery", ignoreCase = true)) return false
    if (listOf("parcels", "shipments", "packages", "enroute", "delivered")
            .any { attributes[it] is JsonArray }
    ) return false
    val hasCanonicalPayload = PARCEL_PAYLOAD_MARKERS.any(attributes::containsKey)
    val label = "${entity_id.replace('_', ' ')} ${friendlyName.orEmpty()}"
    val hasParcelLabel = PARCEL_LABEL_MARKERS.any { label.contains(it, ignoreCase = true) }
    return hasCanonicalPayload || hasParcelLabel
}

private fun HAEntity.parcelDirection(translationKey: String?): String {
    val label = "${entity_id.replace('_', ' ')} ${friendlyName.orEmpty()}"
    return if (
        translationKey?.contains("outgoing", ignoreCase = true) == true ||
        label.contains("outgoing", ignoreCase = true) ||
        label.contains("sent parcel", ignoreCase = true) ||
        label.contains("uitgaande", ignoreCase = true) ||
        label.contains("verzonden", ignoreCase = true) ||
        label.contains("ausgehend", ignoreCase = true) ||
        label.contains("sortant", ignoreCase = true) ||
        label.contains("saliente", ignoreCase = true) ||
        label.contains("in uscita", ignoreCase = true) ||
        label.contains("giden", ignoreCase = true)
    ) {
        "Outgoing"
    } else {
        "Incoming"
    }
}

private fun JsonObject.withParcelDirection(fallback: String): JsonObject = buildJsonObject {
    this@withParcelDirection.forEach { (key, value) -> put(key, value) }
    val explicit = listOf("_direction", "direction")
        .firstNotNullOfOrNull { key -> this@withParcelDirection[key]?.jsonPrimitive?.contentOrNull }
    val contactType = (this@withParcelDirection["raw"] as? JsonObject)
        ?.get("contact_type")
        ?.jsonPrimitive
        ?.contentOrNull
    val contactDirection = when {
        contactType.equals("sender", ignoreCase = true) -> "Outgoing"
        contactType.equals("receiver", ignoreCase = true) -> "Incoming"
        else -> null
    }
    put("_direction", explicit ?: contactDirection ?: fallback)
}

private fun HAEntity.individualParcel(translationKey: String?): JsonObject? {
    if (!isIndividualParcelEntity(translationKey)) return null
    val source = attributes ?: return null
    return buildJsonObject {
        source.forEach { (key, value) -> put(key, value) }
        if (!source.containsKey("status")) {
            val entityState = state.takeIf { candidate ->
                candidate.isNotBlank() &&
                    candidate.toDoubleOrNull() == null &&
                    !candidate.equals("unknown", ignoreCase = true) &&
                    !candidate.equals("unavailable", ignoreCase = true) &&
                    !Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(candidate)
            }
            entityState?.let { put("status", it) }
        }
    }.withParcelDirection(parcelDirection(translationKey))
}

private fun HAEntity.summaryParcels(translationKey: String?): List<JsonObject> {
    val attributes = attributes ?: return emptyList()
    val arrays = listOf("parcels", "shipments", "packages", "enroute", "delivered").mapNotNull { key ->
        (attributes[key] as? JsonArray)?.let { key to it }
    }
    return arrays.flatMap { (sourceKey, array) ->
        array.filterIsInstance<JsonObject>()
            .filter { parcel ->
                listOf("barcode", "tracking_number", "tracking_code", "trackingcode", "id", "key")
                    .any { key -> parcel.parcelValue(key)?.isNotBlank() == true }
            }
            .map { parcel ->
                buildJsonObject {
                    parcel.forEach { (key, value) -> put(key, value) }
                    if (!parcel.containsKey("barcode")) {
                        listOf("key", "trackingcode", "tracking_code", "tracking_number")
                            .firstNotNullOfOrNull(parcel::parcelValue)
                            ?.let { put("barcode", it) }
                    }
                    if (!parcel.containsKey("sender")) {
                        parcel.parcelValue("name")?.let { put("sender", it) }
                    }
                    if (!parcel.containsKey("raw_status")) {
                        parcel.parcelValue("status_message")?.let { put("raw_status", it) }
                    }
                    if (!parcel.containsKey("delivered_at")) {
                        parcel.parcelValue("delivery_date")?.let { put("delivered_at", it) }
                    }
                    if (sourceKey == "delivered" && !parcel.containsKey("delivered")) {
                        put("delivered", true)
                    }
                }.withParcelDirection(parcelDirection(translationKey))
            }
    }
}

private fun JsonObject.parcelIdentity(): String {
    val identifier = listOf("barcode", "tracking_number", "tracking_code", "trackingcode", "id", "key")
        .firstNotNullOfOrNull { key -> parcelValue(key)?.takeIf(String::isNotBlank) }
    if (identifier != null) return "id:${identifier.lowercase(Locale.ROOT)}"
    return "fallback:" + listOf("sender", "receiver", "planned_from", "planned_to", "status")
        .joinToString("|") { key -> parcelValue(key).orEmpty().lowercase(Locale.ROOT) }
}

private fun JsonElement.hasMeaningfulParcelValue(): Boolean = when (this) {
    is JsonNull -> false
    is JsonPrimitive -> contentOrNull?.isNotBlank() == true
    is JsonArray -> isNotEmpty()
    is JsonObject -> isNotEmpty()
}

private fun JsonObject.parcelCompleteness(): Int =
    entries.sumOf { (key, value) ->
        if (!value.hasMeaningfulParcelValue()) {
            0
        } else {
            when (key) {
                "history", "status", "raw_status", "planned_from", "planned_to", "delivered" -> 3
                "barcode", "_direction" -> 1
                else -> 2
            }
        }
    }

private data class ParcelCandidate(val parcel: JsonObject, val fromSummary: Boolean)

private fun mergeParcelCandidates(candidates: List<ParcelCandidate>): JsonObject {
    val merged = candidates.map(ParcelCandidate::parcel)
        .sortedBy(JsonObject::parcelCompleteness)
        .fold(buildJsonObject {}) { accumulated, candidate ->
        buildJsonObject {
            accumulated.forEach { (key, value) -> put(key, value) }
            candidate.forEach { (key, value) -> put(key, value) }
        }
    }
    val summaryDirection = candidates
        .firstNotNullOfOrNull { candidate ->
            candidate.takeIf(ParcelCandidate::fromSummary)
                ?.parcel
                ?.parcelValue("_direction")
        }
    return if (summaryDirection == null) merged else buildJsonObject {
        merged.forEach { (key, value) -> put(key, value) }
        put("_direction", summaryDirection)
    }
}

private fun JsonObject.withCarrierCompatibility(domain: String?): JsonObject {
    if (domain != "postnl") return this
    val rawName = (this["raw"] as? JsonObject)
        ?.get("name")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf(String::isNotBlank)
        ?: return this
    return buildJsonObject {
        this@withCarrierCompatibility.forEach { (key, value) -> put(key, value) }
        // PostNL shared-household parcels currently expose the account contact as `sender`.
        // The actual shop remains available as raw.name until the upstream mapper is fixed.
        put("sender", rawName)
    }
}

internal fun collectCarrierParcels(
    entities: List<HAEntity>,
    translationKeys: Map<String, String> = emptyMap(),
    domain: String? = null
): List<JsonObject> {
    val candidates = entities.flatMap { entity ->
        val translationKey = translationKeys[entity.entity_id]
        listOfNotNull(entity.individualParcel(translationKey)?.let { ParcelCandidate(it, false) }) +
            entity.summaryParcels(translationKey).map { ParcelCandidate(it, true) }
    }
    return candidates
        .groupBy { candidate -> candidate.parcel.parcelIdentity() }
        .values
        .map(::mergeParcelCandidates)
        .map { parcel -> parcel.withCarrierCompatibility(domain) }
}

/** Every supported ha-parcel-integrations carrier: HA integration domain -> display name. */
private val PARCEL_CARRIERS = linkedMapOf(
    "postnl" to "PostNL", "dhl_nl" to "DHL", "dpd" to "DPD", "gls" to "GLS",
    "dragonfly" to "Dragonfly", "cainiao" to "Cainiao", "correos" to "Correos",
    "packeta" to "Packeta", "hermes" to "Hermes", "trunkrs" to "Trunkrs",
    "vinted_go" to "Vinted Go", "parcel_aggregator" to "Parcels"
)

/** Carriers whose integration exposes `<domain>.track_parcel` — a manual add-by-tracking-number
 *  service (account-based carriers like PostNL/DPD/DHL only track what's already in the account). */
internal val TRACK_PARCEL_DOMAINS = setOf(
    "gls", "dragonfly", "cainiao", "correos", "packeta", "hermes", "trunkrs"
)

/** Carriers whose `track_parcel` also accepts an optional postal_code to pick the right hub. */
internal val TRACK_PARCEL_POSTCODE_DOMAINS = setOf("gls", "trunkrs")

private fun carrierKey(text: String): String = when {
    text.contains("postnl", true) -> "postnl"
    text.contains("dhl", true) -> "dhl_nl"
    text.contains("dpd", true) -> "dpd"
    text.contains("gls", true) -> "gls"
    text.contains("dragonfly", true) -> "dragonfly"
    text.contains("cainiao", true) || text.contains("aliexpress", true) || text.contains("temu", true) -> "cainiao"
    text.contains("correos", true) -> "correos"
    text.contains("packeta", true) || text.contains("zasilkovna", true) -> "packeta"
    text.contains("hermes", true) -> "hermes"
    text.contains("trunkrs", true) -> "trunkrs"
    text.contains("vinted", true) -> "vinted_go"
    text.contains("parcel aggregator", true) -> "parcel_aggregator"
    else -> "parcel"
}

private fun carrierName(domain: String) = PARCEL_CARRIERS[domain] ?: "Carrier"

/** Each carrier's brand logo, bundled in-app (vendored from every integration's
 *  custom_components/<domain>/brand/icon.png), so logos work offline for all carriers. */
private fun carrierLogoRes(domain: String): Int? = when (domain) {
    "postnl" -> R.drawable.parcel_postnl
    "dhl_nl" -> R.drawable.parcel_dhl_nl
    "dpd" -> R.drawable.parcel_dpd
    "gls" -> R.drawable.parcel_gls
    "dragonfly" -> R.drawable.parcel_dragonfly
    "cainiao" -> R.drawable.parcel_cainiao
    "correos" -> R.drawable.parcel_correos
    "packeta" -> R.drawable.parcel_packeta
    "hermes" -> R.drawable.parcel_hermes
    "trunkrs" -> R.drawable.parcel_trunkrs
    else -> null
}

/** Combines multiple integration devices for the same known carrier while retaining first-seen
 * carrier ordering. Unknown/generic devices stay independent because sharing the fallback key does
 * not prove that they belong to the same delivery company. */
internal fun aggregateParcelCarriers(carriers: List<ParcelCarrier>): List<ParcelCarrier> {
    val grouped = linkedMapOf<String, MutableList<ParcelCarrier>>()
    carriers.forEach { carrier ->
        val groupId = if (carrier.key == "parcel") "parcel:${carrier.deviceId}" else carrier.key
        grouped.getOrPut(groupId) { mutableListOf() } += carrier
    }
    return grouped.values.map { accounts ->
        if (accounts.size == 1) return@map accounts.first()
        val first = accounts.first()
        ParcelCarrier(
            key = first.key,
            name = carrierName(first.domain),
            deviceId = "aggregate:${first.key}",
            entities = accounts.flatMap { it.entities }.distinctBy { it.entity_id },
            incoming = accounts.sumOf { it.incoming },
            outgoing = accounts.sumOf { it.outgoing },
            logoUrl = accounts.firstNotNullOfOrNull { it.logoUrl },
            baseUrl = first.baseUrl,
            accessToken = first.accessToken,
            domain = first.domain,
            translationKeys = accounts
                .flatMap { it.translationKeys.entries }
                .associate { it.toPair() }
        )
    }
}

private fun extractObjectList(attributes: JsonObject?): List<JsonObject> {
    attributes ?: return emptyList()
    val preferred = listOf("letters", "brieven", "items", "shipments", "parcels")
    preferred.forEach { key ->
        (attributes[key] as? JsonArray)?.filterIsInstance<JsonObject>()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return attributes.values.filterIsInstance<JsonArray>().firstNotNullOfOrNull { array ->
        array.filterIsInstance<JsonObject>().takeIf { it.isNotEmpty() }
    }.orEmpty()
}

private fun countEntity(
    entities: List<HAEntity>,
    translationKeys: Map<String, String>,
    word: String
): Int = entities
    .firstOrNull { entity ->
        val translationKey = translationKeys[entity.entity_id].orEmpty()
        val text = "${entity.entity_id} ${entity.friendlyName.orEmpty()}"
        val stableRoleMatches = when (word) {
            "incoming" -> translationKey == "incoming" || translationKey.startsWith("incoming_parcels")
            "outgoing" -> translationKey == "outgoing" || translationKey.startsWith("outgoing_parcels")
            else -> translationKey.contains(word, true)
        }
        val localizedRoleMatches = when (word) {
            "incoming" -> listOf("incoming", "inkomende", "eingehend", "entrant", "entrante", "in arrivo", "gelen")
            "outgoing" -> listOf("outgoing", "uitgaande", "ausgehend", "sortant", "saliente", "in uscita", "giden")
            else -> listOf(word)
        }.any { text.contains(it, true) }
        val mentionsParcel = listOf("parcel", "pakket", "paket", "colis", "paquete", "pacco", "kargo")
            .any { text.contains(it, true) }
        (stableRoleMatches || (localizedRoleMatches && mentionsParcel)) &&
            !translationKey.contains("delivered", true) &&
            !text.contains("delivered", true) &&
            !text.contains("bezorgd", true) &&
            entity.parcelAttribute("barcode") == null
    }?.state?.toIntOrNull() ?: 0

private fun resolveParcelCarriers(
    deviceIds: List<String>,
    entities: List<HAEntity>,
    registry: List<HAEntityRegistryEntry>,
    devices: List<HADeviceRegistryEntry>,
    customImages: Map<String, String>,
    customNames: Map<String, String>,
    currentUrl: String,
    accessToken: String,
    fallbackCarrierName: String
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
        // The integration domain (registry platform) is the reliable carrier identity; fall back to a
        // name hint only when the registry hasn't loaded it. This drives the logo and add support.
        val platform = directEntities.firstNotNullOfOrNull { e -> registry.firstOrNull { it.entity_id == e.entity_id }?.platform }
        val domain = platform?.takeIf { it in PARCEL_CARRIERS } ?: carrierKey(hint)
        val key = domain
        val deviceEntities = (directEntities + entities.filter { entity ->
            if (!entity.entity_id.startsWith("image.")) return@filter false
            val matchesSensorPrefix = letterPrefixes.any { prefix ->
                entity.entity_id.startsWith("image.${prefix}_letter", true)
            }
            // With one configured account the integration's unscoped PostNL image entities are
            // safe as a fallback. With multiple accounts, only direct-device/prefix matches may be
            // attached; otherwise disabling aggregation would duplicate both accounts' letters.
            val isPostNlLetterImage = deviceIds.distinct().size == 1 && key == "postnl" &&
                entity.attributes?.get("id")?.jsonPrimitive?.contentOrNull != null
            matchesSensorPrefix || isPostNlLetterImage
        }).distinctBy { it.entity_id }
        // Only a user-set custom image lives in logoUrl; the built-in per-carrier logo is a bundled
        // drawable resolved by domain in CarrierLogo.
        val image = customImages[deviceId]?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}/${it.removePrefix("/")}" }
        val translationKeys = registry
            .asSequence()
            .filter { it.device_id == deviceId && !it.translation_key.isNullOrBlank() }
            .associate { it.entity_id to it.translation_key.orEmpty() }
        val displayName = customNames[deviceId]?.takeIf { it.isNotBlank() }
            ?: PARCEL_CARRIERS[domain]
            ?: device?.name_by_user
            ?: device?.name
            ?: fallbackCarrierName
        ParcelCarrier(key, displayName, deviceId, deviceEntities,
            countEntity(deviceEntities, translationKeys, "incoming"),
            countEntity(deviceEntities, translationKeys, "outgoing"),
            image,
            currentUrl,
            accessToken,
            domain = domain,
            translationKeys = translationKeys)
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
    val fallbackCarrierName = stringResource(R.string.parcel_generic_carrier)
    val hasUnresolvedDevices = widget.deviceIds.any { deviceId ->
        devices.none { it.id == deviceId } || registry.none { it.device_id == deviceId }
    }
    LaunchedEffect(widget.deviceIds, hasUnresolvedDevices) {
        if (widget.deviceIds.isNotEmpty() && hasUnresolvedDevices) {
            viewModel.fetchRegistries(force = true)
            delay(1_500.milliseconds)
            val deviceRegistry = viewModel.deviceRegistry.value
            val entityRegistry = viewModel.entityRegistry.value
            if (widget.deviceIds.any { id -> deviceRegistry.none { it.id == id } || entityRegistry.none { it.device_id == id } }) {
                viewModel.fetchRegistries(force = true)
            }
        }
    }
    val carriers = remember(widget.deviceIds, widget.carrierImageUrls, widget.carrierNames, widget.aggregateCarriers, entities, registry, devices, currentUrl, accessToken, fallbackCarrierName) {
        val resolved = resolveParcelCarriers(widget.deviceIds, entities, registry, devices, widget.carrierImageUrls, widget.carrierNames, currentUrl, accessToken.orEmpty(), fallbackCarrierName)
        if (widget.aggregateCarriers) aggregateParcelCarriers(resolved) else resolved
    }
    val incoming = carriers.sumOf { it.incomingCount }
    val outgoing = carriers.sumOf { it.outgoingCount }
    val supportsLetters = carriers.any { it.supportsLetters }
    val letters = carriers.sumOf { it.currentLetterCount }
    var showDialog by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
                .clip(RoundedCornerShape(widget.cornerRadius.dp))
                .background(surfaceGradient(appColors.elevated))
                .clickable(enabled = !isEditMode) { showDialog = true },
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = Color.Transparent
        ) {
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) {
                    WidgetBackground(widget.backgroundUrl, currentUrl)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (carriers.isEmpty()) {
                            Box(Modifier.size(84.dp).background(Color(0xFF60A5FA).copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) {
                                MdiIcon(widget.icon, tint = Color(0xFF60A5FA), size = 44.dp)
                            }
                        } else {
                            // Overlap the carrier logos, wrapping onto extra rows when they no longer fit
                            // one line (e.g. 4 carriers show as 2 + 2 rather than a squashed row of 4).
                            val shown = carriers.take(6)
                            val perRow = when { shown.size <= 3 -> shown.size; shown.size == 4 -> 2; else -> 3 }
                            Column(verticalArrangement = Arrangement.spacedBy((-8).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                shown.chunked(perRow).forEach { rowCarriers ->
                                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
                                        rowCarriers.forEach { CarrierLogo(it, 56) }
                                    }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(.88f)))))
                Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.Black.copy(.55f), shape = itemCornerShape()) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(widget.title ?: stringResource(R.string.parcel_title), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        val summary = if (supportsLetters) {
                            stringResource(
                                R.string.parcel_summary_with_letter_count,
                                incoming,
                                outgoing,
                                pluralStringResource(R.plurals.parcel_letter_count, letters, letters)
                            )
                        } else {
                            stringResource(R.string.parcel_summary, incoming, outgoing)
                        }
                        Text(summary, color = Color.White.copy(.7f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
        }
    }
    if (showDialog) ParcelDialog(carriers, viewModel, onDismiss = { showDialog = false })
}

@Composable
private fun CarrierLogo(carrier: ParcelCarrier, size: Int) {
    Surface(shape = RoundedCornerShape((size / 4).dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.size(size.dp)) {
        val logoUrl = carrier.logoUrl
        val logoRes = carrierLogoRes(carrier.domain)
        when {
            logoUrl != null -> ParcelAsyncImage(carrier, logoUrl, carrier.name, Modifier.fillMaxSize().padding(7.dp), ContentScale.Fit)
            logoRes != null -> Image(painterResource(logoRes), carrier.name, Modifier.fillMaxSize().padding(7.dp), contentScale = ContentScale.Fit)
            else -> Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalShipping, null) }
        }
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
private fun ParcelDialog(carriers: List<ParcelCarrier>, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    var showAddParcel by remember { mutableStateOf(false) }
    val canAddParcel = remember(carriers) { carriers.any { it.supportsManualAdd } }
    var selectedCarrierId by remember(carriers) { mutableStateOf(if (carriers.size == 1) carriers.firstOrNull()?.deviceId else null) }
    val carrier = carriers.firstOrNull { it.deviceId == selectedCarrierId }
    var tab by remember(selectedCarrierId) { mutableStateOf(ParcelTab.Incoming) }
    var selectedParcel by remember(selectedCarrierId) { mutableStateOf<JsonObject?>(null) }
    var selectedHistory by remember(selectedParcel) { mutableStateOf<JsonObject?>(null) }
    var parcelDetail by remember(selectedCarrierId) { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<JsonObject?>(null) }
    val availableTabs = if (carrier?.supportsLetters == true) {
        ParcelTab.entries
    } else {
        ParcelTab.entries.filterNot { it == ParcelTab.Letters }
    }
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

    com.jimz011apps.hki7.ui.components.ModernSettingsDialogFrame(
        title = if (parcelDetail) stringResource(R.string.parcel_tracking_history) else carrier?.name ?: stringResource(R.string.parcel_title),
        subtitle = when {
            parcelDetail -> stringResource(R.string.parcel_tracking_history_subtitle)
            carrier == null -> stringResource(R.string.parcel_choose_carrier_account)
            else -> stringResource(R.string.parcel_summary, carrier.incomingCount, carrier.outgoingCount)
        },
        icon = Icons.Default.LocalShipping,
        onDismiss = onDismiss,
        onBack = when {
            parcelDetail -> {{ parcelDetail = false; selectedHistory = null }}
            carrier != null && carriers.size > 1 -> {{ selectedCarrierId = null }}
            else -> null
        },
        content = {
            Column(
                Modifier
                    .fillMaxSize()
                    .swipeToAdjacentTab(
                        tabs = availableTabs.map { it.name },
                        selected = tab.name,
                        enabled = carrier != null && !parcelDetail,
                        onSelect = { selectedName ->
                            tab = availableTabs.first { it.name == selectedName }
                            selectedHistory = null
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    carriers.isEmpty() -> Text(stringResource(R.string.ui_choose_one_or_more_carrier_devices_in_widget_settings_100a4ca), color = appColors.onMuted)
                    carrier == null -> {
                        val scroll = rememberScrollState()
                        Column(Modifier.weight(1f).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Totals across every configured carrier.
                            val anyLetters = carriers.any { it.supportsLetters }
                            Surface(shape = itemCornerShape(), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SummaryPill(stringResource(R.string.parcel_incoming), carriers.sumOf { it.incomingCount }, Modifier.weight(1f))
                                    SummaryPill(stringResource(R.string.parcel_delivered), carriers.sumOf { it.deliveredCount }, Modifier.weight(1f))
                                    SummaryPill(stringResource(R.string.parcel_outgoing), carriers.sumOf { it.outgoingCount }, Modifier.weight(1f))
                                    if (anyLetters) SummaryPill(stringResource(R.string.parcel_letters), carriers.sumOf { it.currentLetterCount }, Modifier.weight(1f))
                                }
                            }
                            carriers.forEach { item ->
                                Surface(shape = itemCornerShape(), color = appColors.subtleSurface, contentColor = appColors.onSurface,
                                    modifier = Modifier.fillMaxWidth().clickable { selectedCarrierId = item.deviceId }) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        CarrierLogo(item, 56)
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                if (item.supportsLetters) {
                                                    stringResource(
                                                        R.string.parcel_summary_with_letter_count,
                                                        item.incomingCount,
                                                        item.outgoingCount,
                                                        pluralStringResource(
                                                            R.plurals.parcel_letter_count,
                                                            item.currentLetterCount,
                                                            item.currentLetterCount
                                                        )
                                                    )
                                                } else {
                                                    stringResource(R.string.parcel_summary, item.incomingCount, item.outgoingCount)
                                                },
                                                color = appColors.onMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (parcelDetail) {
                            ParcelHero(carrier, selectedParcel, selectedHistory)
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
                                }, text = { Text(stringResource(R.string.parcel_tab_count, stringResource(item.titleRes), count), maxLines = 1) })
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
                                    stringResource(requireNotNull(tab.emptyRes)),
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
                                    stringResource(R.string.ui_no_announced_letters_4382408),
                                    color = appColors.onMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                )
                                carrier.letters.forEach { letter ->
                                    val image = carrier.letterImage(letter)
                                    Surface(shape = itemCornerShape(), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth().clickable { selectedLetter = letter }) {
                                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (image != null) ParcelAsyncImage(carrier, image, null, Modifier.size(58.dp).clip(itemCornerShape()), ContentScale.Crop)
                                            Column(Modifier.weight(1f)) {
                                                Text(letter["title"]?.jsonPrimitive?.contentOrNull ?: stringResource(R.string.ui_mail_92379cb), fontWeight = FontWeight.SemiBold)
                                                Text(letter["date"]?.jsonPrimitive?.contentOrNull?.let { formatParcelTime(it) } ?: stringResource(R.string.ui_announced_letter_3a7ac05), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        },
        footer = {
            if (canAddParcel && !parcelDetail) {
                Button(onClick = { showAddParcel = true }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ui_add_parcel_bde8afb))
                }
                Spacer(Modifier.weight(1f))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_done_e9b450d)) }
        }
    )
    selectedLetter?.let { letter -> LetterViewerDialog(carrier, letter) { selectedLetter = null } }
    if (showAddParcel) AddParcelDialog(carriers, viewModel) { showAddParcel = false }
}

@Composable
private fun AddParcelDialog(carriers: List<ParcelCarrier>, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val scope = rememberCoroutineScope()
    // Only carriers whose integration supports track_parcel; dedupe by domain since the service is
    // domain-level (it resolves the config entry itself, so aggregated/multiple accounts collapse).
    val addable = remember(carriers) { carriers.filter { it.supportsManualAdd }.distinctBy { it.domain } }
    var selectedDomain by remember { mutableStateOf(if (addable.size == 1) addable.first().domain else null) }
    val selected = addable.firstOrNull { it.domain == selectedDomain }
    var tracking by remember(selectedDomain) { mutableStateOf("") }
    var postcode by remember(selectedDomain) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember(selectedDomain) { mutableStateOf<String?>(null) }
    val needsPostcode = selected?.domain in TRACK_PARCEL_POSTCODE_DOMAINS
    val addParcelError = stringResource(R.string.ui_could_not_add_the_parcel_check_the_number_and_fd157b4)

    com.jimz011apps.hki7.ui.components.ModernSettingsDialogFrame(
        title = stringResource(R.string.ui_add_parcel_82c0bf8),
        subtitle = selected?.let { stringResource(R.string.parcel_track_new, it.name) }
            ?: stringResource(R.string.parcel_choose_carrier),
        icon = Icons.Default.Add,
        onDismiss = onDismiss,
        onBack = if (selected != null && addable.size > 1) ({ selectedDomain = null }) else null,
        content = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    addable.isEmpty() -> Text(
                        stringResource(R.string.ui_none_of_your_configured_carriers_support_adding_a_parcel_21dc0a4),
                        color = appColors.onMuted
                    )
                    selected == null -> addable.forEach { c ->
                        Surface(shape = itemCornerShape(), color = appColors.subtleSurface, contentColor = appColors.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable { selectedDomain = c.domain }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CarrierLogo(c, 48)
                                Text(c.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = tracking, onValueChange = { tracking = it; message = null },
                            label = { Text(stringResource(R.string.ui_tracking_number_4e40c54)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        if (needsPostcode) OutlinedTextField(
                            value = postcode, onValueChange = { postcode = it },
                            label = { Text(stringResource(R.string.ui_postal_code_optional_b86b6a2)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        },
        footer = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
            Spacer(Modifier.weight(1f))
            if (selected != null) Button(
                enabled = !busy && tracking.isNotBlank(),
                onClick = {
                    val domain = selected.domain
                    busy = true; message = null
                    scope.launch {
                        val payload = buildJsonObject {
                            put("tracking_code", tracking.trim())
                            if (needsPostcode && postcode.isNotBlank()) put("postal_code", postcode.trim())
                        }
                        val result = runCatching { viewModel.callServiceRawAwait(domain, "track_parcel", payload) }
                        busy = false
                        if (result.isSuccess) onDismiss()
                        else message = addParcelError
                    }
                }
            ) { Text(if (busy) stringResource(R.string.ui_adding_ffb2e62) else stringResource(R.string.ui_add_61cc55a)) }
        }
    )
}

@Composable
private fun ParcelHistoryList(parcel: JsonObject, selected: JsonObject?, onSelected: (JsonObject) -> Unit) {
    val appColors = LocalHKIAppColors.current
    val history = parcel["history"] as? JsonArray
    if (history.isNullOrEmpty()) {
        Text(stringResource(R.string.ui_no_status_history_enable_parcel_history_in_the_integration_302ca4a), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        return
    }
    history.reversed().forEach { item ->
        val event = item as? JsonObject ?: return@forEach
        val eventTitle = event["raw_status"]?.jsonPrimitive?.contentOrNull?.let(::normalizeRawStatus)
            ?: event["status"]?.jsonPrimitive?.contentOrNull?.let { localizedParcelStatus(it) }
            ?: stringResource(R.string.ui_update_fb91e24)
        val eventTime = event["timestamp"]?.jsonPrimitive?.contentOrNull?.let { formatParcelTime(it) }.orEmpty()
        val selectedColors = if (event == selected) {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        }
        Surface(
            shape = itemCornerShape(),
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
    val rawStatus = (history?.attr("raw_status") ?: parcel?.attr("raw_status"))?.let(::normalizeRawStatus)
        ?: parcel?.attr("status")?.let { localizedParcelStatus(it) }
        ?: stringResource(R.string.parcel_unknown_status)
    val delivered = parcel?.isDeliveredParcel() == true
    // Best available ETA/delivery moment. A planned window wins; otherwise any single expected-time
    // field; otherwise (for delivered parcels) the last history timestamp.
    val plannedWindow = formatParcelWindow(parcel?.attr("planned_from"), parcel?.attr("planned_to"))
    val singleEta = listOf("expected_delivery", "delivery_date", "eta", "expected", "delivery", "planned_date")
        .firstNotNullOfOrNull { parcel?.attr(it) }?.let { formatParcelTime(it) }
    val deliveredMoment = if (delivered) (parcel?.get("history") as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("timestamp")?.jsonPrimitive?.contentOrNull }?.lastOrNull()?.let { formatParcelTime(it) } else null
    val moment = history?.attr("timestamp")?.let { formatParcelTime(it) }
        ?: plannedWindow.ifBlank { null }
        ?: singleEta
        ?: deliveredMoment
        ?: ""
    val momentLabel = when { history != null -> stringResource(R.string.ui_history_90ccd64); delivered -> stringResource(R.string.ui_delivered_eea956c); else -> stringResource(R.string.ui_expected_delivery_78d54b2) }
    val stage = when (status) { "registered" -> 0; "in_transit" -> 1; "out_for_delivery", "at_pickup_point" -> 2; "delivered" -> 3; else -> 1 }
    Surface(shape = itemCornerShape(), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(190.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarrierLogo(carrier, 58)
                Column(Modifier.weight(1f)) {
                    Text(parcel?.attr("sender") ?: parcel?.attr("receiver") ?: parcel?.attr("barcode") ?: stringResource(R.string.ui_no_parcel_selected_922a402), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(rawStatus.replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                if (moment.isNotBlank()) Column(horizontalAlignment = Alignment.End) {
                    Text(momentLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(moment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
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
                listOf(
                    stringResource(R.string.parcel_stage_registered),
                    stringResource(R.string.parcel_stage_sorting),
                    stringResource(R.string.parcel_stage_on_the_way),
                    stringResource(R.string.parcel_delivered)
                ).forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun InteractiveParcelRow(parcel: JsonObject, selected: Boolean, onClick: () -> Unit) {
    fun attr(name: String) = parcel[name]?.jsonPrimitive?.contentOrNull
    val title = attr("sender") ?: attr("receiver") ?: attr("barcode") ?: stringResource(R.string.ui_parcel_9ddaaee)
    val status = attr("raw_status")?.let(::normalizeRawStatus)
        ?: attr("status")?.let { localizedParcelStatus(it) }
        ?: stringResource(R.string.parcel_unknown_status)
    val schedule = formatParcelWindow(attr("planned_from"), attr("planned_to"))
    Surface(shape = itemCornerShape(), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
    com.jimz011apps.hki7.ui.components.ModernSettingsDialogFrame(
        title = letter["title"]?.jsonPrimitive?.contentOrNull ?: stringResource(R.string.parcel_mail),
        subtitle = letter["date"]?.jsonPrimitive?.contentOrNull?.let { formatParcelTime(it) } ?: stringResource(R.string.parcel_letter_preview),
        icon = Icons.Default.Email,
        onDismiss = onDismiss,
        content = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (image != null) ParcelAsyncImage(carrier, image, null, Modifier.fillMaxSize().clip(itemCornerShape()), ContentScale.Fit)
                else Text(stringResource(R.string.ui_no_letter_image_available_eeb42c0), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        footer = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_done_e9b450d)) } }
    )
}

@Composable
@Suppress("unused")
private fun ParcelDialogLegacy(carriers: List<ParcelCarrier>, onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    var selected by remember(carriers) { mutableStateOf(if (carriers.size == 1) carriers.firstOrNull()?.deviceId else null) }
    val carrier = carriers.firstOrNull { it.deviceId == selected }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (carrier != null && carriers.size > 1) IconButton(onClick = { selected = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.parcel_carriers)) }
                Text(carrier?.name ?: stringResource(R.string.parcel_title), modifier = Modifier.weight(1f))
            }
        },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.heightIn(max = 520.dp).fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (carriers.isEmpty()) Text(stringResource(R.string.ui_choose_one_or_more_carrier_devices_in_widget_settings_100a4ca), color = appColors.onMuted)
                else if (carrier == null) carriers.forEach { item ->
                    Surface(shape = itemCornerShape(), color = appColors.subtleSurface,
                        modifier = Modifier.fillMaxWidth().clickable { selected = item.deviceId }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CarrierLogo(item, 48)
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, color = appColors.onSurface)
                                Text(stringResource(R.string.parcel_summary, item.incomingCount, item.outgoingCount), color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryPill(stringResource(R.string.parcel_incoming), carrier.incomingCount, Modifier.weight(1f))
                        SummaryPill(stringResource(R.string.parcel_outgoing), carrier.outgoingCount, Modifier.weight(1f))
                        if (carrier.letters.isNotEmpty()) SummaryPill(stringResource(R.string.parcel_mail), carrier.letters.size, Modifier.weight(1f))
                    }
                    if (carrier.parcels.isEmpty() && carrier.letters.isEmpty()) Text(stringResource(R.string.ui_no_active_parcels_or_mail_6c9d49c), color = appColors.onMuted)
                    carrier.parcels.forEach { ParcelRow(it) }
                    carrier.letters.forEach { letter ->
                        val image = carrier.letterImage(letter)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (image != null) AsyncImage(image, null, Modifier.size(52.dp).clip(itemCornerShape()), contentScale = ContentScale.Crop)
                            Box(Modifier.weight(1f)) {
                                DetailRow(letter["title"]?.jsonPrimitive?.contentOrNull ?: stringResource(R.string.parcel_mail),
                                    letter["date"]?.jsonPrimitive?.contentOrNull?.let { formatParcelTime(it) } ?: stringResource(R.string.ui_announced_letter_3a7ac05))
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
    Surface(modifier, shape = itemCornerShape(), color = colors.surfaceContainerHigh) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.ui_text_c79f712, count), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable private fun ParcelRow(parcel: JsonObject) {
    fun attr(name: String) = parcel[name]?.jsonPrimitive?.contentOrNull
    val title = attr("sender") ?: attr("receiver") ?: attr("barcode") ?: stringResource(R.string.ui_parcel_9ddaaee)
    val status = attr("raw_status")?.let(::normalizeRawStatus)
        ?: attr("status")?.let { localizedParcelStatus(it) }
        ?: stringResource(R.string.parcel_unknown_status)
    val direction = attr("_direction")
    val from = attr("planned_from")
    val to = attr("planned_to")
    val schedule = formatParcelWindow(from, to)
    Surface(shape = itemCornerShape(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status.replaceFirstChar(Char::uppercase), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (direction != null) Text(
                if (parcel.isOutgoingParcel()) stringResource(R.string.parcel_outgoing) else stringResource(R.string.parcel_incoming),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
            if (schedule.isNotBlank()) Text(schedule, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            attr("pickup_point")?.let { Text(stringResource(R.string.ui_pickup_b6da461, it), style = MaterialTheme.typography.bodySmall) }
            val history = parcel["history"] as? JsonArray
            if (!history.isNullOrEmpty()) {
                Text(stringResource(R.string.ui_history_90ccd64), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                history.takeLast(4).reversed().forEach { item ->
                    val obj = item as? JsonObject ?: return@forEach
                    DetailRow(obj["raw_status"]?.jsonPrimitive?.contentOrNull?.let(::normalizeRawStatus)
                        ?: obj["status"]?.jsonPrimitive?.contentOrNull?.let { localizedParcelStatus(it) }
                        ?: stringResource(R.string.ui_update_fb91e24),
                        obj["timestamp"]?.jsonPrimitive?.contentOrNull?.let { formatParcelTime(it) }.orEmpty())
                }
            } else {
                Text(stringResource(R.string.ui_no_status_history_enable_parcel_history_in_the_integration_302ca4a), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
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

private fun parseParcelMoment(value: String): ZonedDateTime? =
    runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()

/**
 * Date portion of a parcel timestamp: the weekday name for anything in the next 6 days (avoids
 * colliding with the same weekday a week out), otherwise a locale-ordered date that only includes
 * the year when it differs from the current year.
 */
private fun formatParcelDatePart(moment: ZonedDateTime): String {
    val daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), moment.toLocalDate())
    if (daysAhead in 0..5) {
        return moment.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
    val skeleton = if (moment.year == LocalDate.now().year) "MMMd" else "yMMMd"
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
    return moment.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/** Time portion, honoring the device's 12/24-hour clock setting rather than only the locale default. */
@Composable
private fun formatParcelTimePart(moment: ZonedDateTime): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(
        Locale.getDefault(), if (is24Hour) "Hm" else "hm"
    )
    return moment.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
private fun formatParcelTime(value: String): String {
    val moment = parseParcelMoment(value) ?: return value
    return "${formatParcelDatePart(moment)}, ${formatParcelTimePart(moment)}"
}

/** Formats a planned delivery window. Same-day windows show the date once, not on both ends. */
@Composable
private fun formatParcelWindow(from: String?, to: String?): String {
    val fromMoment = from?.let(::parseParcelMoment)
    val toMoment = to?.let(::parseParcelMoment)
    if (fromMoment != null && toMoment != null && fromMoment.toLocalDate() == toMoment.toLocalDate()) {
        return "${formatParcelDatePart(fromMoment)}, ${formatParcelTimePart(fromMoment)} – ${formatParcelTimePart(toMoment)}"
    }
    return listOfNotNull(from?.let { formatParcelTime(it) }, to?.let { formatParcelTime(it) }).joinToString(" – ")
}

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
            delay(1_500.milliseconds)
            if (viewModel.deviceRegistry.value.isEmpty()) viewModel.fetchRegistries(force = true)
        }
    }
    if (devices.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.ui_select_device_87a4751)) },
            text = {
                Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
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
    var carrierNames by remember(widget) { mutableStateOf(widget.carrierNames) }
    var aggregateCarriers by remember(widget) { mutableStateOf(widget.aggregateCarriers) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var picking by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("accounts") }
    val defaultTitle = stringResource(R.string.parcel_title)
    val hasUnresolvedDevices = deviceIds.any { id -> devices.none { it.id == id } }
    LaunchedEffect(deviceIds, hasUnresolvedDevices) {
        if (deviceIds.isNotEmpty() && hasUnresolvedDevices) {
            viewModel.fetchRegistries(force = true)
            delay(1_500.milliseconds)
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
    AlertDialog(stableHeight = true, onDismissRequest = onDismiss, title = {
        com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
            stringResource(R.string.parcel_title),
            stringResource(R.string.parcel_settings_subtitle)
        )
    }, text = {
        val scroll = rememberScrollState()
        // stableHeight gives the dialog a fixed tall frame; fill it so the scroll area spans the
        // whole body instead of capping at 480dp and leaving the lower half empty.
        Column(Modifier.fillMaxHeight().fadingEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.jimz011apps.hki7.ui.components.SettingsTabRow(
                tabs = listOf(
                    "accounts" to stringResource(R.string.parcel_settings_accounts),
                    "organization" to stringResource(R.string.parcel_settings_organization),
                    "appearance" to stringResource(R.string.parcel_settings_appearance)
                ),
                selected = settingsPage,
                onSelect = { settingsPage = it }
            )
            if (settingsPage == "accounts") {
            com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_carrier_accounts_20ad36c), stringResource(R.string.ui_add_every_integration_account_that_should_contribute_parce_ce58afb))
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.ui_title_768e0c1)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.ui_carrier_devices_9927ae1), style = MaterialTheme.typography.labelLarge)
            deviceIds.forEach { id ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val device = devices.firstOrNull { it.id == id }
                        Text(device?.let { it.name_by_user ?: it.name } ?: stringResource(R.string.ui_loading_device_a3fda88), modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { deviceIds = deviceIds - id; imageUrls = imageUrls - id; carrierNames = carrierNames - id }) { Icon(Icons.Default.Close, stringResource(R.string.action_remove)) }
                    }
                    OutlinedTextField(
                        value = carrierNames[id].orEmpty(),
                        onValueChange = { value -> carrierNames = if (value.isBlank()) carrierNames - id else carrierNames + (id to value) },
                        label = { Text(stringResource(R.string.ui_carrier_display_name_optional_5ca06fc)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrls[id].orEmpty(),
                        onValueChange = { value -> imageUrls = if (value.isBlank()) imageUrls - id else imageUrls + (id to value) },
                        label = { Text(stringResource(R.string.ui_logo_url_or_ha_local_path_optional_e795f06)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TextButton(onClick = { picking = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ui_add_carrier_device_7945283)) }
            }
            if (settingsPage == "organization") {
            com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_organization_519255a), stringResource(R.string.ui_control_how_accounts_become_tabs_and_lists_f100249))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { aggregateCarriers = !aggregateCarriers },
                shape = itemCornerShape(),
                color = LocalHKIAppColors.current.subtleSurface
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_aggregate_carriers_eae26c7), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.ui_combine_multiple_accounts_from_the_same_carrier_into_one_0006a49),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalHKIAppColors.current.onMuted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = aggregateCarriers, onCheckedChange = { aggregateCarriers = it })
                }
            }
            }
            if (settingsPage == "appearance") {
            com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_card_size_shape_and_background_df75707))
            WidgetWidthSelector(width, { width = it })
            Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!square, { square = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                FilterChip(square, { square = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
            }
            WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
            }
        }
    }, confirmButton = { Button(onClick = { onSave(widget.copy(deviceIds = deviceIds, carrierImageUrls = imageUrls, carrierNames = carrierNames, aggregateCarriers = aggregateCarriers, title = title.ifBlank { defaultTitle }, width = width, isSquare = square, cornerRadius = radius, backgroundUrl = backgroundUrl)) }) { Text(stringResource(R.string.ui_save_efc007a)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } })
}

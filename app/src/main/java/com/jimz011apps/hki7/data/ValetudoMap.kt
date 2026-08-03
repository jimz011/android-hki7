@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Valetudo publishes its map as an MQTT-autodiscovered `camera` entity. The PNG that entity serves
 * is only a *container*: the robot deliberately does not render the map, so the visible pixels are
 * blank/placeholder and the real payload is the ValetudoMap JSON, deflated inside the PNG's `zTXt`
 * chunk. Decoding it needs nothing beyond the JDK's [Inflater] and kotlinx.serialization, so no
 * extra Home Assistant component, add-on or Android dependency is involved.
 *
 * Schema mirrors `src/lib/RawMapData.ts` in Hypfer/lovelace-valetudo-map-card.
 */
@Serializable
data class ValetudoMap(
    val metaData: ValetudoMapMetaData = ValetudoMapMetaData(),
    val size: ValetudoMapSize = ValetudoMapSize(),
    /** Edge length in map units of one pixel in [ValetudoLayer.pixels]. */
    val pixelSize: Int = DEFAULT_PIXEL_SIZE,
    val layers: List<ValetudoLayer> = emptyList(),
    val entities: List<ValetudoEntity> = emptyList()
) {
    val segments: List<ValetudoLayer> get() = layers.filter { it.type == LAYER_SEGMENT }
    val walls: List<ValetudoLayer> get() = layers.filter { it.type == LAYER_WALL }
    val floors: List<ValetudoLayer> get() = layers.filter { it.type == LAYER_FLOOR }

    fun entity(type: String): ValetudoEntity? = entities.firstOrNull { it.type == type }

    companion object {
        const val DEFAULT_PIXEL_SIZE = 5

        const val LAYER_FLOOR = "floor"
        const val LAYER_SEGMENT = "segment"
        const val LAYER_WALL = "wall"

        const val ENTITY_CHARGER = "charger_location"
        const val ENTITY_ROBOT = "robot_position"
        const val ENTITY_PATH = "path"
        const val ENTITY_PREDICTED_PATH = "predicted_path"
        const val ENTITY_GO_TO_TARGET = "go_to_target"
    }
}

@Serializable
data class ValetudoMapMetaData(
    val version: Int = 0,
    /** Changes on every map update; useful to skip re-rendering an unchanged frame. */
    val nonce: String? = null
)

@Serializable
data class ValetudoMapSize(val x: Int = 0, val y: Int = 0)

@Serializable
data class ValetudoLayerDimensions(
    val x: ValetudoLayerBounds = ValetudoLayerBounds(),
    val y: ValetudoLayerBounds = ValetudoLayerBounds(),
    val pixelCount: Int = 0
)

@Serializable
data class ValetudoLayerBounds(val min: Int = 0, val max: Int = 0, val mid: Int = 0)

@Serializable
data class ValetudoLayerMetaData(
    val area: Long = 0,
    /** Only present on `segment` layers. This is the id the room-cleaning call needs. */
    @Serializable(with = LenientStringSerializer::class)
    val segmentId: String? = null,
    /** User-assigned room name from Valetudo, when the robot supports naming segments. */
    val name: String? = null,
    val active: Boolean = false
)

@Serializable
data class ValetudoLayer(
    val type: String = "",
    /**
     * Flat array of **x,y pairs** — not points, not a bitmap. `pixels[0]` and `pixels[1]` are the
     * first pixel's x and y, so consumers must step by 2. Coordinates are in `pixelSize` units.
     */
    val pixels: List<Int> = emptyList(),
    /** Optional run-length form: `(x, y, runLength)` triples running along +x. */
    val compressedPixels: List<Int> = emptyList(),
    val dimensions: ValetudoLayerDimensions = ValetudoLayerDimensions(),
    val metaData: ValetudoLayerMetaData = ValetudoLayerMetaData()
) {
    /**
     * Walks every pixel of the layer regardless of which encoding the robot sent, so callers never
     * have to branch on [pixels] vs [compressedPixels].
     */
    inline fun forEachPixel(action: (x: Int, y: Int) -> Unit) {
        if (pixels.isNotEmpty()) {
            var i = 0
            while (i + 1 < pixels.size) {
                action(pixels[i], pixels[i + 1])
                i += 2
            }
            return
        }
        var i = 0
        while (i + 2 < compressedPixels.size) {
            val x = compressedPixels[i]
            val y = compressedPixels[i + 1]
            val run = compressedPixels[i + 2]
            for (step in 0 until run) action(x + step, y)
            i += 3
        }
    }

    /** Room label for a `segment` layer: Valetudo's name when set, else "Room <id>". */
    fun segmentLabel(fallback: (String) -> String): String? {
        val id = metaData.segmentId ?: return null
        return metaData.name?.takeIf { it.isNotBlank() } ?: fallback(id)
    }
}

@Serializable
data class ValetudoEntity(
    val type: String = "",
    /** Flat array of x,y pairs, same convention as [ValetudoLayer.pixels], but in map units. */
    val points: List<Int> = emptyList(),
    val metaData: ValetudoEntityMetaData = ValetudoEntityMetaData()
)

@Serializable
data class ValetudoEntityMetaData(val angle: Float? = null)

/**
 * Accepts both `"20"` and `20` for the same field. Valetudo's own payloads use strings, but some
 * robot firmwares (and older map versions) emit segment ids as numbers, and a hard failure there
 * would take down the whole map rather than one label.
 */
internal object LenientStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("ValetudoLenientString", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return jsonDecoder.decodeJsonElement().jsonPrimitive.content
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/**
 * Extracts the ValetudoMap JSON that Valetudo hides in a PNG's `zTXt` chunk.
 *
 * Kept free of Android and Compose types so it can be unit tested on the JVM, and so the caller can
 * run it on a background dispatcher — a full map is a few hundred KB of JSON and must never be
 * inflated or parsed on the main thread.
 */
object ValetudoMapDecoder {

    /** `89 50 4E 47 0D 0A 1A 0A` — the fixed 8-byte PNG signature. */
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /** The keyword Valetudo writes; other zTXt chunks (e.g. generic metadata) are ignored. */
    private const val KEYWORD = "ValetudoMap"

    /** PNG caps a text chunk's keyword at 79 bytes, so a longer run without a NUL is malformed. */
    private const val MAX_KEYWORD_LENGTH = 79

    private const val CHUNK_LENGTH_BYTES = 4
    private const val CHUNK_TYPE_BYTES = 4
    private const val CHUNK_CRC_BYTES = 4

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Full decode: PNG bytes in, parsed map out. Returns null when [png] is not a PNG, carries no
     * Valetudo payload, or holds JSON this version cannot read — all of which are normal when the
     * configured camera entity is an ordinary camera rather than a Valetudo map.
     */
    fun decode(png: ByteArray): ValetudoMap? {
        val raw = extractMapJson(png) ?: return null
        return runCatching { json.decodeFromString<ValetudoMap>(raw) }.getOrNull()
    }

    /** The inflated JSON text, before parsing. Split out so tests can assert on the byte layer. */
    fun extractMapJson(png: ByteArray): String? {
        val deflated = findValetudoChunk(png) ?: return null
        return inflate(deflated)?.toString(Charsets.UTF_8)
    }

    /** True when the bytes start with the PNG signature. */
    fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < PNG_SIGNATURE.size) return false
        return PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] }
    }

    /**
     * Walks the PNG chunk sequence and returns the deflate stream of the Valetudo `zTXt` chunk.
     * Prefers the chunk keyed [KEYWORD] but falls back to the first `zTXt` chunk, because the
     * keyword has changed across Valetudo releases while the payload has not.
     */
    private fun findValetudoChunk(png: ByteArray): ByteArray? {
        if (!isPng(png)) return null

        var fallback: ByteArray? = null
        var offset = PNG_SIGNATURE.size

        while (offset + CHUNK_LENGTH_BYTES + CHUNK_TYPE_BYTES <= png.size) {
            val length = readIntBigEndian(png, offset)
            // A negative length means the 32-bit field overflowed Int — always corrupt input.
            if (length < 0) return fallback
            val type = String(png, offset + CHUNK_LENGTH_BYTES, CHUNK_TYPE_BYTES, Charsets.US_ASCII)
            val dataStart = offset + CHUNK_LENGTH_BYTES + CHUNK_TYPE_BYTES
            if (dataStart + length > png.size) return fallback

            if (type == "IEND") return fallback
            if (type == "zTXt") {
                val chunk = parseZTxt(png, dataStart, length)
                if (chunk != null) {
                    if (chunk.keyword == KEYWORD) return chunk.deflated
                    if (fallback == null) fallback = chunk.deflated
                }
            }

            offset = dataStart + length + CHUNK_CRC_BYTES
        }
        return fallback
    }

    private class ZTxtChunk(val keyword: String, val deflated: ByteArray)

    /**
     * `zTXt` layout: keyword bytes, then a NUL separator, then a one-byte compression method, then
     * the deflate stream. The compression byte is why the payload starts **two** bytes past the
     * NUL and not one — reading it as one byte feeds a stray 0x00 to the inflater and fails.
     */
    private fun parseZTxt(png: ByteArray, dataStart: Int, length: Int): ZTxtChunk? {
        val dataEnd = dataStart + length
        var nul = -1
        var i = dataStart
        while (i < dataEnd && i - dataStart <= MAX_KEYWORD_LENGTH) {
            if (png[i] == 0.toByte()) { nul = i; break }
            i++
        }
        if (nul < 0) return null

        val payloadStart = nul + 2
        if (payloadStart >= dataEnd) return null

        val keyword = String(png, dataStart, nul - dataStart, Charsets.ISO_8859_1)
        return ZTxtChunk(keyword, png.copyOfRange(payloadStart, dataEnd))
    }

    private fun inflate(deflated: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(deflated)
            val out = ByteArrayOutputStream(deflated.size * 4)
            val buffer = ByteArray(16 * 1024)
            while (!inflater.finished()) {
                val produced = inflater.inflate(buffer)
                if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                out.write(buffer, 0, produced)
            }
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }

    private fun readIntBigEndian(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}

package com.jimz011apps.hki7.data

import android.content.Intent
import android.net.Uri
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.core.content.IntentCompat
import java.io.IOException

private const val HA_TAG_HOST = "www.home-assistant.io"
private const val HA_TAG_PATH_PREFIX = "/tag/"

/** The exact URL shape the official Home Assistant app writes to a tag. Tapping it fires the same
 *  `tag_scanned` event regardless of which app wrote it, which is the point: HKI 7 tags and
 *  official-app tags are interchangeable. */
fun homeAssistantTagUri(tagId: String): String = "https://$HA_TAG_HOST$HA_TAG_PATH_PREFIX$tagId"

/** The raw [Tag] off a dispatched NFC intent, for the write path (which needs a live handle to
 *  open a connection, not just the parsed identifier [extractTagId] returns). */
fun extractTag(intent: Intent): Tag? =
    IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)

/** Reads the tag identifier out of a dispatched NFC intent (foreground dispatch or a cold-launch
 *  via the manifest's NDEF_DISCOVERED filter). Recognizes the Home Assistant tag URL, falls back
 *  to a plain-text NDEF payload, and finally to the tag's own hardware serial so an unformatted
 *  (never-written) tag still reads as *something* identifiable. */
fun extractTagId(intent: Intent): String? {
    val messages = IntentCompat.getParcelableArrayExtra(intent, NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        ?.filterIsInstance<NdefMessage>()
        .orEmpty()

    for (message in messages) {
        for (record in message.records) {
            homeAssistantTagIdFromRecord(record)?.let { return it }
        }
        for (record in message.records) {
            plainTextFromRecord(record)?.let { return it }
        }
    }

    val serial = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return null
    return serial.joinToString("") { "%02X".format(it) }
}

private fun homeAssistantTagIdFromRecord(record: NdefRecord): String? {
    if (record.tnf != NdefRecord.TNF_WELL_KNOWN || !record.type.contentEquals(NdefRecord.RTD_URI)) return null
    val uri = runCatching { record.toUri() }.getOrNull() ?: return null
    if (uri.host != HA_TAG_HOST) return null
    val id = uri.path?.removePrefix(HA_TAG_PATH_PREFIX)?.trim('/')
    return id?.takeIf { it.isNotBlank() }
}

private fun plainTextFromRecord(record: NdefRecord): String? {
    val isText = record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)
    val isMimeText = record.tnf == NdefRecord.TNF_MIME_MEDIA && record.type.toString(Charsets.US_ASCII) == "text/plain"
    if (!isText && !isMimeText) return null
    return runCatching {
        if (isText) {
            // RTD_TEXT payload: byte 0 is status (bit 6 = encoding, low 6 bits = language-code
            // length), followed by that many bytes of language code, then the text itself.
            val payload = record.payload
            val languageCodeLength = payload[0].toInt() and 0x3F
            val encoding = if (payload[0].toInt() and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
            String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, encoding)
        } else {
            String(record.payload, Charsets.UTF_8)
        }
    }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
}

/** Why a write attempt failed, for a specific message in the UI rather than a generic "didn't work". */
sealed class NfcWriteException(message: String) : Exception(message) {
    object ReadOnly : NfcWriteException("Tag is read-only")
    object TooSmall : NfcWriteException("Tag does not have enough storage for this link")
    object Unsupported : NfcWriteException("Tag does not support NFC Data Exchange Format (NDEF)")
    object ConnectionLost : NfcWriteException("Tag was moved away before the write finished")
}

/** Writes a single well-known URI record onto [tag], formatting it first if it has never held
 *  NDEF data before. Overwriting an already-written tag (the "edit" case) takes the same path as
 *  writing a blank one — [Ndef.writeNdefMessage] replaces whatever was there. */
fun writeUri(tag: Tag, uri: String): Result<Unit> {
    val message = NdefMessage(arrayOf(NdefRecord.createUri(Uri.parse(uri))))
    return try {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) return Result.failure(NfcWriteException.ReadOnly)
                if (message.toByteArray().size > ndef.maxSize) return Result.failure(NfcWriteException.TooSmall)
                ndef.writeNdefMessage(message)
                Result.success(Unit)
            } finally {
                runCatching { ndef.close() }
            }
        } else {
            val formatable = NdefFormatable.get(tag) ?: return Result.failure(NfcWriteException.Unsupported)
            formatable.connect()
            try {
                formatable.format(message)
                Result.success(Unit)
            } finally {
                runCatching { formatable.close() }
            }
        }
    } catch (_: TagLostException) {
        Result.failure(NfcWriteException.ConnectionLost)
    } catch (_: FormatException) {
        Result.failure(NfcWriteException.Unsupported)
    } catch (e: IOException) {
        Result.failure(NfcWriteException.ConnectionLost)
    }
}

/** The Settings › NFC tags Write tab's handoff: while it is waiting for a tap, it claims the next
 *  one here so [MainActivity]'s dispatch treats it as a write instead of the default read-and-
 *  report-to-Home-Assistant behavior. Cleared on success, cancel, or leaving the screen. */
object NfcTagManager {
    var pendingWrite: ((Tag) -> Unit)? = null

    private const val EXTRA_CONSUMED = "com.jimz011apps.hki7.extra.NFC_CONSUMED"

    /** Claims a dispatched intent exactly once. Android can recreate an activity with its original
     * intent, so clearing an in-memory queue alone is not enough to prevent the same tag scan from
     * being submitted again after a configuration change. */
    fun claim(intent: Intent): Boolean {
        if (intent.getBooleanExtra(EXTRA_CONSUMED, false)) return false
        val isNfcDispatch = intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED
        if (!isNfcDispatch) return false
        intent.putExtra(EXTRA_CONSUMED, true)
        return true
    }
}

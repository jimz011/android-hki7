package com.jimz011apps.hki7.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Reports an NFC tag read to Home Assistant the way the official app does: a `scan_tag` command
 * on this phone's mobile_app webhook. Home Assistant then fires `tag_scanned` against this
 * phone's own device, so tag triggers and "last scanned by" both work.
 *
 * Home Assistant has no `tag.scan` service, so calling one always failed. The webhook needs no
 * live connection or token either, so a tap that cold-launches the app reports straight away
 * instead of waiting for the dashboard to connect.
 */
object NfcTagReporting {

    suspend fun report(context: Context, instanceId: String, tagId: String): Boolean {
        val appContext = context.applicationContext
        val prefs = PreferencesManager(appContext).forInstance(instanceId)
        val external = prefs.serverUrl.first()?.takeIf { it.isNotBlank() }
        val internal = prefs.internalUrl.first()?.takeIf { it.isNotBlank() }
        if (isDemoServerUrl(external ?: internal)) return true

        val webhookId = prefs.mobileAppWebhookId.first()?.takeIf { it.isNotBlank() }
            ?: return reportAsEvent(appContext, instanceId, prefs, tagId)
        val payload = buildJsonObject {
            put("type", "scan_tag")
            put("data", buildJsonObject { put("tag_id", tagId) })
        }
        prefs.mobileAppCloudhookUrl.first()?.takeIf { it.isNotBlank() }?.let { cloudhook ->
            return post(cloudhook, payload)
        }
        // Same choice as notification actions and telemetry: internal on home Wi-Fi, external
        // elsewhere. If the internal one fails (home Wi-Fi still attached but the server is not
        // reachable over it), the external URL gets one try before the scan counts as failed.
        val preferred = resolveHomeAssistantUrl(
            external, internal, prefs.homeSsids.first(), currentWifiSsid(appContext)
        ) ?: return false
        val candidates = listOfNotNull(preferred, external).distinct()
        return candidates.any { base -> post("${base.removeSuffix("/")}/api/webhook/$webhookId", payload) }
    }

    /**
     * No mobile_app registration (telemetry is gated on location or notifications being on), so
     * there is no webhook. Fire the event the tag trigger listens for directly instead. Home
     * Assistant only accepts that from an administrator; for anyone else the scan reports failed.
     */
    private suspend fun reportAsEvent(
        context: Context,
        instanceId: String,
        prefs: PreferencesManager,
        tagId: String,
    ): Boolean {
        val deviceId = prefs.mobileAppDeviceId.first()
        return try {
            Hki7Endpoint.withClient(context, instanceId) { client ->
                client.fireEvent("tag_scanned", buildJsonObject {
                    put("tag_id", tagId)
                    if (!deviceId.isNullOrBlank()) put("device_id", deviceId)
                })
                true
            } ?: false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun post(url: String, payload: JsonObject): Boolean {
        // The webhook is unauthenticated, so the token is only needed to build the client.
        val client = HomeAssistantClient(url, "")
        return try {
            val (status, _) = client.postWebhook(url, payload)
            status in 200..299
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            client.dispose()
        }
    }
}

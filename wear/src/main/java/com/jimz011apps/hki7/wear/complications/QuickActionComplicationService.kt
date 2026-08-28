package com.jimz011apps.hki7.wear.complications

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.jimz011apps.hki7.wear.MainActivity
import com.jimz011apps.hki7.wear.R
import com.jimz011apps.hki7.wear.data.HomeAssistantRest
import com.jimz011apps.hki7.wear.data.WearPreferences
import com.jimz011apps.hki7.wear.data.WearSession

/**
 * One entity's state on the watch face itself.
 *
 * The cheapest possible glance — no swipe, no tap. Shows the first quick action's state, which is
 * the one the user put at the top of their own list, so it needs no configuration screen of its
 * own to be useful.
 *
 * Pull-based like the tile, and for the same reason: the watch face asks on its own schedule and
 * a socket held open for it would cost battery around the clock.
 */
class QuickActionComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type != ComplicationType.SHORT_TEXT) {
            null
        } else {
            shortText(
                text = getString(R.string.wear_complication_preview_value),
                description = getString(R.string.wear_complication_preview_label),
            )
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null

        val prefs = WearPreferences(applicationContext)
        val session = WearSession(prefs)
        val serverUrl = prefs.serverUrlOnce()
        if (serverUrl == null || !session.isAuthenticated()) {
            return shortText(
                text = getString(R.string.wear_complication_not_set_up),
                description = getString(R.string.wear_app_name),
            )
        }

        val action = prefs.quickActionsOnce().firstOrNull { it.showOnWatch }
            ?: return shortText(
                text = getString(R.string.wear_complication_none),
                description = getString(R.string.wear_app_name),
            )

        val entity = runCatching {
            HomeAssistantRest(serverUrl, session).state(action.entityId)
        }.getOrNull()

        // An unreachable Home Assistant shows a dash rather than a stale value: on a watch face a
        // number that quietly stopped being true is worse than an obvious gap.
        val value = entity?.state?.replaceFirstChar(Char::uppercase) ?: "—"
        val label = action.name?.takeIf { it.isNotBlank() }
            ?: entity?.friendlyName
            ?: action.entityId

        return shortText(text = value, description = label)
    }

    private fun shortText(text: String, description: String): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(description).build(),
        )
            .setTapAction(openApp())
            .build()

    /** Tapping opens HKI 7, where the thing can actually be controlled. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

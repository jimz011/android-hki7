package com.jimz011apps.hki7.wear.tiles

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.jimz011apps.hki7.wear.complications.QuickActionComplicationService

/**
 * Pushes fresh content to the tiles and the complication.
 *
 * A tile is not a screen the app can redraw: it belongs to the system, which asks for content on
 * its own schedule. Without this, a change made on the phone would not appear on the wrist until
 * the freshness interval lapsed — up to a quarter of an hour of showing something that is no
 * longer true, which reads as the tile being broken rather than merely slow.
 *
 * Call after anything that changes what a tile would draw: a handover from the phone, or an action
 * run from the app.
 */
object TileRefresh {

    fun requestAll(context: Context) {
        requestTiles(context)
        requestComplications(context)
    }

    fun requestTiles(context: Context) {
        // Safe to call when the user has not added the tile: the request is simply dropped.
        runCatching {
            TileService.getUpdater(context).requestUpdate(QuickActionsTileService::class.java)
            TileService.getUpdater(context).requestUpdate(ThermostatTileService::class.java)
        }
    }

    fun requestComplications(context: Context) {
        runCatching {
            ComplicationDataSourceUpdateRequester
                .create(
                    context,
                    ComponentName(context, QuickActionComplicationService::class.java),
                )
                .requestUpdateAll()
        }
    }
}

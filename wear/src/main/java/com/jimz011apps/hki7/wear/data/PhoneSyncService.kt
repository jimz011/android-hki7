package com.jimz011apps.hki7.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.jimz011apps.hki7.data.WearHandover
import com.jimz011apps.hki7.wear.tiles.TileRefresh
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * Receives the watch's configuration from the phone.
 *
 * Play services starts this even when the watch app is not running, so the watch is already set up
 * the first time the user opens it rather than showing an empty list while it fetches.
 */
class PhoneSyncService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val prefs = WearPreferences(applicationContext)
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            if (event.dataItem.uri.path != WearHandover.CONFIG_PATH) return@forEach
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            // runBlocking is correct here: WearableListenerService already delivers on a background
            // thread and the process may be torn down as soon as this returns, so the write has to
            // finish before it does.
            runBlocking {
                prefs.applyHandover(
                    serverUrl = map.getString(WearHandover.KEY_SERVER_URL),
                    refreshToken = map.getString(WearHandover.KEY_REFRESH_TOKEN),
                    quickActionsJson = map.getString(WearHandover.KEY_QUICK_ACTIONS),
                    roomsJson = map.getString(WearHandover.KEY_ROOMS),
                    thermostatEntityIds = map.getString(WearHandover.KEY_THERMOSTAT_ENTITY_IDS),
                )
            }
            // The tiles belong to the system, not to the app, so a new configuration is invisible
            // until they are asked to redraw. Without this a change made on the phone waits out
            // the freshness interval, which looks like the tile being broken rather than slow.
            TileRefresh.requestAll(applicationContext)
        }
    }

    companion object {
        /**
         * Asks the phone to re-send everything.
         *
         * Used from the watch's setup screen. A watch that was paired after the phone last wrote
         * its config, or that was factory reset, has no DataItem waiting for it — nothing arrives
         * until the phone writes again, so the watch has to be able to ask.
         */
        suspend fun requestConfig(context: android.content.Context): Boolean = runCatching {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) return false
            nodes.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, WearHandover.REQUEST_CONFIG_PATH, ByteArray(0))
                    .await()
            }
            true
        }.getOrDefault(false)
    }
}

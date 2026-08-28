package com.jimz011apps.hki7.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Answers a watch asking to be set up.
 *
 * A watch paired after the phone last wrote its configuration has no DataItem waiting for it, and
 * one that was factory reset has lost the one it had. Neither can be fixed by the phone alone,
 * because the phone has no way to know it happened — so the watch asks, and this replies.
 */
class PhoneWearListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearHandover.REQUEST_CONFIG_PATH) return
        // Play services delivers this on a background thread and may tear the process down as soon
        // as it returns, so the push has to complete before it does.
        runBlocking { WearSync.push(applicationContext) }
    }
}

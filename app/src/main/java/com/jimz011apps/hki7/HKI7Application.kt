package com.jimz011apps.hki7

import android.app.Application
import android.os.Looper
import android.util.Log

class HKI7Application : Application() {
    override fun onCreate() {
        super.onCreate()
        installBackgroundThreadSafetyNet()
    }

    /** Last-resort net for an exception that slips past every try/catch in the reconnect/sync code
     *  — history keeps showing there's always one more unguarded call site, each surfacing as the
     *  whole app hard-crashing (including on the very first connection attempt after opening the
     *  app, which reads as "the app won't even open"). A background-thread exception is now logged
     *  in full — with the real stack trace, unlike a release build's obfuscated one — and swallowed
     *  instead of taking the process down; a main-thread exception still crashes normally, since
     *  that's almost always a real, UI-breaking bug that shouldn't be silently hidden. */
    private fun installBackgroundThreadSafetyNet() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val mainThread = Looper.getMainLooper().thread
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (thread === mainThread) {
                previousHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            Log.e(
                "HKI7",
                "Unhandled exception on background thread '${thread.name}' — recovered instead of crashing",
                throwable
            )
        }
    }
}

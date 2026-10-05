package com.jimz011apps.hki7

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.jimz011apps.hki7.data.NfcTagManager
import com.jimz011apps.hki7.data.NfcTagReporting
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.extractTagId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Reports a Home Assistant NFC tag tapped while HKI 7 is not in front, without showing the app.
 *
 * This is what the official app does: the tap lands on an invisible activity that reports it and
 * finishes, so nothing opens. While HKI 7 is in front, MainActivity's foreground dispatch gets the
 * tap instead, which is also the only place a tag can be claimed for writing.
 *
 * The activity stays alive until the report is done, because finishing first would cancel it.
 * It draws nothing and lets touches through, so the second or so that takes cannot be noticed.
 */
class NfcTagReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        // A recreated activity carries the same intent; claim() makes sure it is reported once.
        val tagId = intent.takeIf(NfcTagManager::claim)?.let(::extractTagId)
        if (tagId == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val reported = withTimeoutOrNull(REPORT_TIMEOUT) {
                val prefs = PreferencesManager(applicationContext)
                prefs.ensureHomeAssistantInstanceStore()
                val instanceId = prefs.activeHomeAssistantInstanceId.first() ?: return@withTimeoutOrNull false
                NfcTagReporting.reportAndRecord(applicationContext, instanceId, tagId)
            } ?: false
            // Quiet on success, like the official app: the phone's own NFC sound already confirmed
            // the tap. A failure is worth saying, since the automation the user expects won't run.
            if (!reported) {
                Toast.makeText(applicationContext, R.string.nfc_scan_failed, Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    private companion object {
        val REPORT_TIMEOUT = 15.seconds
    }
}

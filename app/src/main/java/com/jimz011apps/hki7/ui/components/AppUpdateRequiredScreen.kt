package com.jimz011apps.hki7.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.AppUpdateGate
import com.jimz011apps.hki7.data.HaFamilyDevices
import com.jimz011apps.hki7.data.Hki7RequiredUpdate
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Full-screen "your family expects a newer HKI" gate.
 *
 * Blocking is conditional on Play actually having the update. When it does, this is the only thing
 * on screen and Update is the only way out. When it doesn't — a sideloaded build, no Play Store, or
 * a rollout that hasn't reached this device — there is nothing the person can do to comply, and
 * locking them out of the app that controls their locks and alarms over that would be worse than
 * letting them run an old version. In that case the same screen explains the situation and offers
 * a way past, once per app session.
 *
 * [onContinue] is only ever invoked from the escape path; the caller uses it to stop showing this
 * for the rest of the session, and nothing here writes it to storage — the next launch checks Play
 * again, so a device stops being let past the moment the update genuinely becomes available.
 */
@Composable
fun AppUpdateRequiredScreen(
    required: Hki7RequiredUpdate,
    prefs: PreferencesManager,
    onContinue: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    var offer by remember { mutableStateOf<AppUpdateGate.UpdateOffer?>(null) }
    var launchFailed by remember { mutableStateOf(false) }
    val updateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Play reports the outcome by relaunching the app; nothing to do here. */ }

    LaunchedEffect(Unit) { offer = AppUpdateGate.check(context) }

    // Keep asking the component what it requires while this screen is up. An admin who lifts the
    // requirement — or lowers it after realising nobody can install what they asked for — must be
    // able to reach a device that is currently sitting behind this gate, and the report is what
    // refreshes the cached requirement that decides whether the gate is shown at all.
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { HaFamilyDevices.report(context, prefs) }
            delay(20.seconds)
        }
    }

    val ready = offer as? AppUpdateGate.UpdateOffer.Ready
    val checking = offer == null

    Surface(Modifier.fillMaxSize(), color = appColors.background) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier.size(88.dp).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        MaterialTheme.shapes.large
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    MdiIcon("cellphone-arrow-down", tint = MaterialTheme.colorScheme.primary, size = 44.dp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        if (required.deviceSpecific) R.string.app_update_required_title_device
                        else R.string.app_update_required_title
                    ),
                    color = appColors.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(
                        R.string.app_update_required_message,
                        required.versionName.ifBlank { required.versionCode.toString() },
                        BuildConfig.VERSION_NAME
                    ),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                when {
                    checking -> Text(
                        stringResource(R.string.app_update_required_checking),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    ready != null -> {
                        Button(
                            onClick = {
                                if (!AppUpdateGate.start(context, ready, updateLauncher)) {
                                    // Play accepted the request but declined to show the flow; the
                                    // listing still gets them there, so this is never a dead button.
                                    launchFailed = true
                                    AppUpdateGate.openPlayListing(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text(stringResource(R.string.app_update_required_update)) }
                        if (launchFailed) {
                            Text(
                                stringResource(R.string.app_update_required_opened_store),
                                color = appColors.onMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Text(
                            stringResource(R.string.app_update_required_unavailable),
                            color = appColors.onMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { AppUpdateGate.openPlayListing(context) },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text(stringResource(R.string.app_update_required_open_store)) }
                        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.app_update_required_continue))
                        }
                    }
                }
            }
        }
    }
}

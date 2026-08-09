package com.jimz011apps.hki7.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.jimz011apps.hki7.BuildConfig

/**
 * Google Play's in-app update flow, behind the "your family is on a newer HKI" gate.
 *
 * An app cannot install a newer version of itself: silent installation needs device-owner/MDM
 * privileges or a system app holding INSTALL_PACKAGES, and Play's own auto-update setting belongs
 * to the user. Play's in-app update is the closest thing — it hands the whole update to Play in a
 * full-screen activity the user cannot dismiss.
 *
 * That flow only exists for builds Play installed. A sideloaded APK, a device with no Play Store,
 * or a staged rollout that hasn't reached this user yet all mean there is genuinely nothing to
 * install. Blocking someone out of the app that opens their front door over an update they cannot
 * obtain is worse than their being out of date, so [check] exists to tell those cases apart and
 * the gate lets them past whenever the answer is [UpdateOffer.Unavailable].
 */
object AppUpdateGate {

    sealed interface UpdateOffer {
        /** Play has an update ready, so the prompt can insist: complying is one tap away. */
        data class Ready(val info: AppUpdateInfo) : UpdateOffer

        /** Nothing installable right now. The prompt must let this person through. */
        data object Unavailable : UpdateOffer
    }

    /** Asks Play whether it can update this app right now. Never throws — a missing, disabled or
     *  broken Play Store resolves to [UpdateOffer.Unavailable], which is the permissive answer. */
    suspend fun check(context: Context): UpdateOffer {
        val info = runCatching {
            AppUpdateManagerFactory.create(context.applicationContext).requestAppUpdateInfo()
        }.getOrNull() ?: return UpdateOffer.Unavailable
        val ready = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        return if (ready) UpdateOffer.Ready(info) else UpdateOffer.Unavailable
    }

    /** Launches Play's full-screen immediate update. Returns false if Play declined to start it,
     *  which the caller treats as "fall back to the store listing" rather than a dead button. */
    fun start(
        context: Context,
        offer: UpdateOffer.Ready,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean = runCatching {
        AppUpdateManagerFactory.create(context.applicationContext).startUpdateFlowForResult(
            offer.info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )
    }.getOrDefault(false)

    /** Opens this app's Play listing — the fallback when the in-app flow isn't available. Falls
     *  back again to the browser, since `market://` needs a Play Store that may not be installed. */
    fun openPlayListing(context: Context) {
        val id = BuildConfig.APPLICATION_ID
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }.onFailure { error ->
            if (error !is ActivityNotFoundException) return@onFailure
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

/** The update this device is being asked to install, from the household minimum or from a request
 *  aimed at this one device. */
data class Hki7RequiredUpdate(
    val versionCode: Int,
    /** Human-readable version that goes with it; blank if the setter's app never sent one. */
    val versionName: String,
    /** True when it targets this device specifically rather than the whole household. */
    val deviceSpecific: Boolean,
) {
    /** Whether this build actually falls short of it. */
    val applies: Boolean get() = BuildConfig.VERSION_CODE < versionCode
}

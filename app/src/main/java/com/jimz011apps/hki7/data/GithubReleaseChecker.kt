package com.jimz011apps.hki7.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Watches the project's GitHub releases so a build that did not come from Play still learns there
 * is a newer one.
 *
 * Play's own update mechanism only reaches installs that came from Play. Everyone running a
 * sideloaded APK — which is most people who follow the repository — otherwise has no signal at all
 * short of checking the releases page by hand.
 */
object GithubReleaseChecker {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/jimz011/android-hki7/releases/latest"
    const val RELEASES_PAGE = "https://github.com/jimz011/android-hki7/releases/latest"
    private const val CHANNEL_ID = "hki7_app_updates"
    private const val NOTIFICATION_ID = 0x7C10
    /** Groups the panel entry with the app itself rather than any Home Assistant instance. */
    private const val UPDATE_TAG = "hki7_update"

    private val json = Json { ignoreUnknownKeys = true }

    /** A release newer than this build, or null when this build is current. */
    data class Available(val versionName: String, val url: String, val notes: String?)

    /**
     * How this copy of the app arrived, which decides what "update now" is even allowed to do.
     *
     * Play forbids an app distributed through Play from updating itself by any other route, so the
     * install source is not a cosmetic preference — it is what keeps the Play build compliant.
     */
    enum class InstallSource { PLAY, SIDELOAD }

    fun installSource(context: Context): InstallSource {
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
        return if (installer == "com.android.vending") InstallSource.PLAY else InstallSource.SIDELOAD
    }

    /**
     * Asks GitHub for the latest release. Returns null when the network is unavailable, the
     * response cannot be parsed, or this build is already current — a failed check is never worth
     * surfacing, since the app works perfectly well without it.
     */
    suspend fun check(): Available? = withContext(Dispatchers.IO) {
        val body = withTimeoutOrNull(15_000) {
            runCatching {
                HttpClient(OkHttp).use { client ->
                    client.get(LATEST_RELEASE_API) {
                        header("Accept", "application/vnd.github+json")
                        // Unauthenticated requests are rate-limited by IP; one call per day per
                        // device sits far inside the 60/hour that allows.
                        header("User-Agent", "HKI7/${BuildConfig.VERSION_NAME}")
                    }.bodyAsText()
                }
            }.getOrNull()
        } ?: return@withContext null

        val release = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return@withContext null
        if (release["draft"]?.jsonPrimitive?.contentOrNull == "true") return@withContext null
        if (release["prerelease"]?.jsonPrimitive?.contentOrNull == "true") return@withContext null

        val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val latest = tag.removePrefix("v").takeIf { it.isNotBlank() } ?: return@withContext null
        if (compareVersions(latest, BuildConfig.VERSION_NAME) <= 0) return@withContext null

        Available(
            versionName = latest,
            url = release["html_url"]?.jsonPrimitive?.contentOrNull ?: RELEASES_PAGE,
            notes = release["body"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Compares two dotted version strings numerically, so 1.1.10 correctly beats 1.1.9 where a
     * string comparison would not. A pre-release suffix ("1.2.0-beta.1") sorts *below* the release
     * it leads to, matching semver: anything after the first dash is stripped and, when the numeric
     * parts tie, the side carrying a suffix loses.
     *
     * Returns >0 when [a] is newer, <0 when older, 0 when they are the same release.
     */
    internal fun compareVersions(a: String, b: String): Int {
        fun parts(v: String) = v.substringBefore('-').split('.')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = (left.getOrElse(i) { 0 }) - (right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        val leftPre = a.contains('-')
        val rightPre = b.contains('-')
        return when {
            leftPre == rightPre -> 0
            leftPre -> -1
            else -> 1
        }
    }

    /** Posts the "update available" notification. No-op without the notification permission. */
    fun notify(context: Context, available: Available) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.update_channel_description) }
        )
        val intent = android.app.PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, available.url.toUri()),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hki)
            .setContentTitle(context.getString(R.string.update_available_title, available.versionName))
            .setContentText(context.getString(R.string.update_available_body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    /**
     * Mirrors an available update into HKI 7's own notification panel.
     *
     * The system notification is posted once per version and is gone once dismissed; this is the
     * copy that survives, and it carries a URI action so "see what changed" still works from the
     * panel days later. URI actions are repeatable by design, so the button keeps working however
     * often it is used.
     *
     * Safe to call repeatedly: the entry id is derived from the version, and
     * [recordLocalNotification] replaces a matching entry instead of adding another.
     */
    suspend fun record(context: Context, available: Available) {
        recordLocalNotification(
            context,
            HKINotification(
                id = "hki7_update_${available.versionName}",
                title = context.getString(R.string.update_available_title, available.versionName),
                message = context.getString(R.string.update_available_body),
                timestamp = System.currentTimeMillis(),
                tag = UPDATE_TAG,
                actions = listOf(
                    HKINotificationAction(
                        action = "URI",
                        title = context.getString(R.string.update_see_changes),
                        uri = available.url
                    )
                )
            )
        )
    }

    /**
     * Sends the user wherever this install is allowed to update from: the Play listing for a Play
     * install, the GitHub release page otherwise.
     *
     * Deliberately does not download and launch an APK itself. That needs REQUEST_INSTALL_PACKAGES,
     * a permission Play restricts and which would apply to the Play build too — handing the release
     * page to the browser gets a sideloaded user to the same APK without putting the Play build at
     * risk.
     */
    fun openUpdate(context: Context, available: Available?) {
        when (installSource(context)) {
            InstallSource.PLAY -> AppUpdateGate.openPlayListing(context)
            InstallSource.SIDELOAD -> {
                val url = available?.url ?: RELEASES_PAGE
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, url.toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { if (it is ActivityNotFoundException) AppUpdateGate.openPlayListing(context) }
            }
        }
    }
}

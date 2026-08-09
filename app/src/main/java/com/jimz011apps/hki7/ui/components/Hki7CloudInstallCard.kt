package com.jimz011apps.hki7.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

internal const val HKI7_CLOUD_GITHUB_URL = "https://github.com/jimz011/HKI7-Cloud-Component"

/** Shared setup card for features that require the optional HKI 7 Cloud component. */
@Composable
internal fun Hki7CloudInstallCard() {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val steps = listOf(
        stringResource(R.string.settings_extra_hki_cloud_step_open_custom_repositories),
        stringResource(R.string.settings_extra_hki_cloud_step_add_repository, HKI7_CLOUD_GITHUB_URL),
        stringResource(R.string.settings_extra_hki_cloud_step_install),
        stringResource(R.string.settings_extra_hki_cloud_step_add_integration),
        stringResource(R.string.settings_extra_hki_cloud_step_reopen)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemCornerShape(),
        color = appColors.subtleSurface
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.ui_this_runs_on_a_small_free_home_assistant_add_f46f2e3),
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodySmall
            )
            steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.ui_text_68fdf13, index + 1),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(step, color = appColors.onSurface, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(
                onClick = { openCloudComponentPage(context) },
                modifier = Modifier.fillMaxWidth(),
                shape = itemCornerShape()
            ) {
                MdiIcon("github", size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_open_the_component_on_github_c1e5ee5))
            }
        }
    }
}

/** One HKI 7 Cloud release and the app feature it unlocked, for the "update to unlock" list below.
 *  New entries belong here whenever a feature starts requiring a newer component — see
 *  CHANGELOG.md in the HKI7-Cloud-Component repo for the authoritative version history. */
private data class Hki7ComponentFeature(val minVersion: String, val labelRes: Int)

private val HKI7_COMPONENT_FEATURES = listOf(
    Hki7ComponentFeature("0.5.3", R.string.settings_extra_hki_cloud_feature_reimport),
    Hki7ComponentFeature("0.5.4", R.string.settings_extra_hki_cloud_feature_search_lists),
    Hki7ComponentFeature("0.6.0", R.string.settings_extra_hki_cloud_feature_room_follow),
    Hki7ComponentFeature("0.6.1", R.string.settings_extra_hki_cloud_feature_room_follow_launch_only),
    Hki7ComponentFeature("0.7.0", R.string.settings_extra_hki_cloud_feature_devices),
    Hki7ComponentFeature("0.8.0", R.string.settings_extra_hki_cloud_feature_required_version),
)

/**
 * Inline notice for a settings tab whose feature needs a newer component than the one installed.
 * Renders nothing once the installed version covers [minVersion].
 *
 * The Family Sharing header already lists every missing feature at once, but that is easy to scroll
 * past; this is the same warning where the setting actually is, so nobody configures a tab that
 * cannot take effect yet. A null [installedVersion] means the component predates version reporting
 * (0.6.1 or earlier), which is older than anything listed here, so the notice always shows.
 */
@Composable
internal fun Hki7RequiresComponent(installedVersion: String?, minVersion: String) {
    if (installedVersion != null && compareHki7ComponentVersions(installedVersion, minVersion) >= 0) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        MdiIcon("cloud-alert-outline", tint = MaterialTheme.colorScheme.error, size = 18.dp)
        Text(
            stringResource(R.string.settings_extra_hki_cloud_tab_requires, minVersion),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Dotted "major.minor.patch" compare, negative when [a] is older than [b]. Good enough for this
 *  component's plain numeric versions — it has never used a pre-release suffix. */
private fun compareHki7ComponentVersions(a: String, b: String): Int {
    val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
    val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val diff = partsA.getOrElse(i) { 0 } - partsB.getOrElse(i) { 0 }
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Shows which HKI 7 Cloud version is installed, and — when it's behind — which app features that
 * leaves unavailable. [installedVersion] is null either because the component reported no version
 * (0.6.1 or earlier, before `hki7/whoami` started returning one) or because it isn't installed at
 * all; the caller only renders this once it already knows the component is present, so here a null
 * version is treated as "assume the oldest possible install" rather than hidden.
 */
@Composable
internal fun Hki7ComponentVersionStatus(installedVersion: String?) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val missing = HKI7_COMPONENT_FEATURES.filter { feature ->
        installedVersion == null || compareHki7ComponentVersions(installedVersion, feature.minVersion) < 0
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MdiIcon(
                if (missing.isEmpty()) "cloud-check-outline" else "cloud-alert-outline",
                tint = if (missing.isEmpty()) MaterialTheme.colorScheme.primary else appColors.onMuted,
                size = 18.dp
            )
            Text(
                if (installedVersion != null) stringResource(R.string.settings_extra_hki_cloud_version, installedVersion)
                else stringResource(R.string.settings_extra_hki_cloud_version_unknown),
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (missing.isNotEmpty()) {
            Text(
                stringResource(R.string.settings_extra_hki_cloud_update_to_unlock),
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodySmall
            )
            missing.forEach { feature ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(
                            R.string.settings_extra_hki_cloud_feature_requires,
                            stringResource(feature.labelRes),
                            feature.minVersion
                        ),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedButton(
                onClick = { openCloudComponentPage(context) },
                modifier = Modifier.fillMaxWidth(),
                shape = itemCornerShape()
            ) {
                MdiIcon("github", size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_open_the_component_on_github_c1e5ee5))
            }
        }
    }
}

private fun openCloudComponentPage(context: android.content.Context) {
    val uri = HKI7_CLOUD_GITHUB_URL.toUri()
    val appIntent = Intent(Intent.ACTION_VIEW, uri)
        .setPackage("com.github.android")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(appIntent); true }.getOrDefault(false)
    if (!opened) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

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

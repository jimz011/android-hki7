package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/**
 * Changelog for the current release, newest first. Only the entry matching the running
 * [BuildConfig.VERSION_NAME] is shown, so adding the next version's notes here is all that a
 * future release needs.
 */
private val changelog: Map<String, Int> = mapOf(
    "1.0.0-beta.12" to R.array.cr_whats_new_beta_12,
    "1.0.0-beta.11" to R.array.cr_whats_new_beta_11,
    "1.0.0-beta.10" to R.array.cr_whats_new_beta_10,
    "1.0.0-beta.9" to R.array.cr_whats_new_beta_9,
    "1.0.0-beta.8" to R.array.cr_whats_new_beta_8,
    "1.0.0-beta.7" to R.array.cr_whats_new_beta_7,
    "1.0.0-beta.6" to R.array.cr_whats_new_beta_6,
    "1.0.0-beta.5" to R.array.cr_whats_new_beta_4_to_5,
    "1.0.0-beta.4" to R.array.cr_whats_new_beta_4_to_5,
    "1.0.0-beta.3" to R.array.cr_whats_new_beta_3,
    "1.0.0-beta.2" to R.array.cr_whats_new_beta_2
)

/** True when there are release notes to show for the running build. */
fun hasChangelogForCurrentVersion(): Boolean = changelog.containsKey(BuildConfig.VERSION_NAME)

/**
 * "What's new" dialog shown once after the app is updated. Dismissal is the caller's cue to record
 * the version code so it never appears again for this release.
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val entries = changelog[BuildConfig.VERSION_NAME]?.let { stringArrayResource(it).toList() }.orEmpty()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f),
                shape = RoundedCornerShape(30.dp),
                color = appColors.elevated,
                contentColor = appColors.onSurface,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                    appColors.elevated,
                                    appColors.elevated
                                )
                            )
                        )
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.ui_what_s_new_4d8dc5f),
                                style = MaterialTheme.typography.headlineSmall,
                                color = appColors.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                stringResource(R.string.ui_hki_7_v_d5594a7, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .fadingEdges(scrollState)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entries.forEach { entry ->
                            // Entries starting with "*" are footnotes: no bullet, muted style.
                            if (entry.startsWith("*")) {
                                Text(
                                    entry,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appColors.onMuted
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Text(
                                        entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = appColors.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(0.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.ui_got_it_5b8027f))
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

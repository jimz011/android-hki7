package com.jimz011apps.hki7.ui.components

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
private val changelog: Map<String, List<String>> = mapOf(
    "1.0.0-beta.2" to listOf(
        "Fixed an issue with smaller displays when either the height was too small or the width too narrow (or both), the content would look bad. It now falls back to a single column design on smaller screens. This has been changed across all elements.",
        "Dialog headers now show 2 rows on narrower screens so that the title no longer cuts off.",
        "Fixed an issue where light sliders that have adaptive lighting feature would overlap other elements.",
        "Added visual to nav bars if there is more content on the navbar than the screen can display",
        "Fixed an issue where camera's wouldn't respect screen orientation lock.",
        "Fixed an issue where we could not zoom in on camera dialogs or full-screen camera's."
    )
)

/** True when there are release notes to show for the running build. */
fun hasChangelogForCurrentVersion(): Boolean = changelog[BuildConfig.VERSION_NAME]?.isNotEmpty() == true

/**
 * "What's new" dialog shown once after the app is updated. Dismissal is the caller's cue to record
 * the version code so it never appears again for this release.
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val entries = changelog[BuildConfig.VERSION_NAME].orEmpty()
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
                                "What's new",
                                style = MaterialTheme.typography.headlineSmall,
                                color = appColors.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                "HKI 7 v${BuildConfig.VERSION_NAME}",
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

                    Spacer(Modifier.height(0.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Got it")
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

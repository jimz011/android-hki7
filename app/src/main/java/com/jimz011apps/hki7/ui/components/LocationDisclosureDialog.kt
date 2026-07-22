package com.jimz011apps.hki7.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Google Play "prominent disclosure" for (background) location, shown BEFORE any location runtime
 * permission request is launched. Play policy requires an in-app disclosure that the user must
 * affirmatively accept, stating that location is collected even when the app is closed or not in
 * use — the wording below follows Google's required template. Do not request location permissions
 * from anywhere without routing through this dialog first.
 */
@Composable
fun LocationDisclosureDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        title = { Text("Location access") },
        text = {
            Text(
                "HKI 7 collects location data to enable presence detection and zone-based " +
                    "automations, even when the app is closed or not in use.\n\n" +
                    "Your location is shared only with your own Home Assistant server. It is " +
                    "never sent to the developer or to any third party, and you can turn this " +
                    "off at any time in Android settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text("Agree") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No thanks") }
        }
    )
}

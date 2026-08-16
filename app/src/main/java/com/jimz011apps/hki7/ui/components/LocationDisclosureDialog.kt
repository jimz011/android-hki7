package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
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
        title = { Text(stringResource(R.string.ui_location_access_3b68001)) },
        text = {
            Text(
                stringResource(R.string.ui_hki_7_collects_location_data_to_enable_presence_detection_2dda033) +
                    stringResource(R.string.ui_automations_even_when_the_app_is_closed_or_not_d27e5b1) +
                    stringResource(R.string.ui_your_location_is_shared_only_with_your_own_home_c75bd6d) +
                    stringResource(R.string.ui_never_sent_to_the_developer_or_to_any_third_f0479e9) +
                    stringResource(R.string.ui_off_at_any_time_in_android_settings_4607e2c),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text(stringResource(R.string.ui_agree_ee68d34)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_no_thanks_6d745b3)) }
        }
    )
}

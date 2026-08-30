package com.jimz011apps.hki7.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.CameraStreamService
import com.jimz011apps.hki7.data.DEVICE_CAMERA_BACK
import com.jimz011apps.hki7.data.DEVICE_CAMERA_FRONT
import com.jimz011apps.hki7.data.DevicePanelSettings
import com.jimz011apps.hki7.data.cameraStreamUrlOrEmpty
import com.jimz011apps.hki7.data.clampedStreamPort
import com.jimz011apps.hki7.data.localIpv4
import com.jimz011apps.hki7.data.withStreamToken
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

@Composable
fun DevicePanelSettingsSection(
    settings: DevicePanelSettings,
    onChange: (DevicePanelSettings) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val localIp = remember { localIpv4(context) }
    val streamUrl = settings.cameraStreamUrlOrEmpty(localIp)
    val streamError by CameraStreamService.lastError.collectAsState()
    var portText by remember(settings.streamPort) { mutableStateOf(settings.clampedStreamPort().toString()) }
    var copied by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onChange(settings.copy(cameraStreamEnabled = true).withStreamToken())
    }

    SettingsGroup {
        Text(stringResource(R.string.settings_device_title), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        Text(stringResource(R.string.settings_device_hint), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
        DeviceSwitchRow(
            title = stringResource(R.string.settings_device_extra_sensors),
            subtitle = stringResource(R.string.settings_device_extra_sensors_subtitle),
            checked = settings.extraSensorsEnabled,
            onCheckedChange = { onChange(settings.copy(extraSensorsEnabled = it)) },
        )
        Text(
            stringResource(R.string.settings_device_wifi_ssid_hint),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    SettingsGroup {
        Text(stringResource(R.string.settings_device_camera), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        DeviceSwitchRow(
            title = stringResource(R.string.settings_device_camera),
            subtitle = stringResource(R.string.settings_device_camera_subtitle),
            checked = settings.cameraStreamEnabled,
            onCheckedChange = { enabled ->
                if (!enabled) {
                    onChange(settings.copy(cameraStreamEnabled = false))
                    return@DeviceSwitchRow
                }
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) onChange(settings.copy(cameraStreamEnabled = true).withStreamToken())
                else cameraPermission.launch(Manifest.permission.CAMERA)
            },
        )
        Text(stringResource(R.string.settings_device_camera_facing), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.cameraFacing != DEVICE_CAMERA_BACK,
                onClick = { onChange(settings.copy(cameraFacing = DEVICE_CAMERA_FRONT)) },
                label = { Text(stringResource(R.string.settings_device_camera_front)) },
            )
            FilterChip(
                selected = settings.cameraFacing == DEVICE_CAMERA_BACK,
                onClick = { onChange(settings.copy(cameraFacing = DEVICE_CAMERA_BACK)) },
                label = { Text(stringResource(R.string.settings_device_camera_back)) },
            )
        }
        OutlinedTextField(
            value = portText,
            onValueChange = { value ->
                val digits = value.filter { it.isDigit() }.take(5)
                portText = digits
                digits.toIntOrNull()?.let { onChange(settings.copy(streamPort = it)) }
            },
            label = { Text(stringResource(R.string.settings_device_camera_port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (streamUrl.isBlank()) {
                stringResource(R.string.settings_device_camera_url_waiting)
            } else {
                stringResource(R.string.settings_device_camera_url, streamUrl)
            },
            color = appColors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(R.string.settings_device_camera_lan_only),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.settings_device_camera_token_hint),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        if (settings.cameraStreamEnabled && streamError != null) {
            Text(
                stringResource(R.string.settings_device_camera_bind_failed, settings.clampedStreamPort()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            stringResource(R.string.settings_device_camera_ha_hint),
            color = appColors.onMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        if (streamUrl.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("camera", streamUrl))
                    copied = true
                },
            ) {
                Text(
                    if (copied) stringResource(R.string.settings_device_camera_copied)
                    else stringResource(R.string.settings_device_camera_copy_url),
                )
            }
        }
    }
}

@Composable
private fun DeviceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

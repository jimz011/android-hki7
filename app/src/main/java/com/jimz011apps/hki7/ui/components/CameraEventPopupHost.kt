package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.MainViewModel
import kotlinx.coroutines.delay

/** Live camera popup triggered by a motion/person/doorbell entity. Hosted at app root so it covers
 *  every dashboard surface the same way custom popups do. */
@Composable
fun CameraEventPopupHost(viewModel: MainViewModel) {
    val request by viewModel.activeCameraPopup.collectAsState()
    val current = request ?: return
    val entities by viewModel.entities.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val camera = entities.firstOrNull { it.entity_id == current.cameraEntityId }
    val liveUrl = resolveEntityCameraUrl(camera, currentUrl, preferLive = true)
    LaunchedEffect(current.nonce) {
        delay(current.timeoutMs)
        viewModel.dismissCameraPopup(current.nonce)
    }
    val title = current.title.ifBlank { stringResource(R.string.ui_camera_4da9c9a) }
    val onDismiss = { viewModel.dismissCameraPopup(current.nonce) }
    if (current.fullscreen) {
        CameraAlertStreamOverlay(
            title = title,
            imageUrl = liveUrl,
            liveWebUrl = liveUrl,
            authToken = accessToken,
            onDismiss = onDismiss,
        )
        return
    }
    HKICameraDialog(
        title = title,
        imageUrl = liveUrl,
        liveWebUrl = liveUrl,
        authToken = accessToken,
        statusText = stringResource(R.string.cr_live),
        entity = camera,
        viewModel = viewModel,
        onDismiss = onDismiss,
    )
}

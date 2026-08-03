package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKICustomPopup
import com.jimz011apps.hki7.data.customPopupWidgetAreaId
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.screens.HAHomeScreen

/** Edit mode for the widget canvas below, independent of the dashboard's global edit mode. A popup
 *  provides it so its contents can be built while the dashboard behind stays in normal use; null
 *  means "follow the global edit mode". */
val LocalEditModeOverride = compositionLocalOf<Boolean?> { null }

/** Highest column count a stack may use on this surface. Dashboards cap at 3 so buttons stay
 *  readable at page width; a popup is a self-contained canvas where denser icon grids make sense,
 *  so it raises the ceiling to 6. */
val LocalMaxStackColumns = compositionLocalOf { 3 }

/** Columns a popup allows. Kept here so the renderers and the settings dialogs agree. */
const val POPUP_MAX_STACK_COLUMNS = 6

/** Hosts the popup opened by a `custom_popup` action. Mounted once at the app root so a popup can be
 *  triggered from a button, a badge, or a dialog's nav bar without per-surface plumbing. */
@Composable
fun CustomPopupHost(viewModel: MainViewModel, navController: NavController) {
    val active by viewModel.activePopup.collectAsState()
    val popups by viewModel.customPopups.collectAsState()
    val current = active ?: return
    val popup = popups.find { it.id == current.popupId }
    if (popup == null) {
        // The popup was deleted while open (or by another device); drop the stale reference.
        LaunchedEffect(current.popupId) { viewModel.closeCustomPopup() }
        return
    }
    CustomPopupDialog(
        popup = popup,
        startInEditMode = current.startInEditMode,
        viewModel = viewModel,
        navController = navController,
        onDismiss = viewModel::closeCustomPopup
    )
}

@Composable
private fun CustomPopupDialog(
    popup: HKICustomPopup,
    startInEditMode: Boolean,
    viewModel: MainViewModel,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val statusIds = remember(popup.statusEntityId) { listOfNotNull(popup.statusEntityId) }
    val statusFlow = remember(viewModel, statusIds) { viewModel.entitiesFor(statusIds) }
    val statusEntities by statusFlow.collectAsState()
    val statusEntity = statusEntities.firstOrNull { it.entity_id == popup.statusEntityId }
    // Without a status entity the dialog still needs one for its header/history plumbing; a synthetic
    // entity keeps the title and close button working while the history button is hidden.
    val entity = statusEntity ?: remember(popup.id) {
        HAEntity(entity_id = "hki7_popup.${popup.id}", state = "")
    }
    var editing by remember(popup.id) { mutableStateOf(startInEditMode) }
    val genericStatus = stringResource(R.string.popup_custom_popup)

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        iconName = popup.icon,
        titleOverride = popup.name,
        statusText = if (statusEntity == null) genericStatus else null,
        // The dashboard behind is never in edit mode while a popup is open (buttons don't fire
        // actions there), and the popup drives its own canvas edit mode instead.
        allowInEditMode = true,
        showHistoryButton = statusEntity != null,
        navController = navController,
        topContent = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (editing) stringResource(R.string.dlg_done) else stringResource(R.string.dlg_edit)
                    )
                }
            }
        }
    ) { _ ->
        // weight(1f) rather than the canvas' own fillMaxSize: the edit toggle above already took
        // part of the column, so filling the whole height would push the grid past the dialog.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(
                LocalEditModeOverride provides editing,
                LocalMaxStackColumns provides POPUP_MAX_STACK_COLUMNS
            ) {
                HAHomeScreen(
                    viewModel = viewModel,
                    navController = navController,
                    widgetAreaId = customPopupWidgetAreaId(popup.id),
                    embedded = true
                )
            }
        }
    }
}

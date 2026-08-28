package com.jimz011apps.hki7.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.jimz011apps.hki7.data.WearRoom
import com.jimz011apps.hki7.wear.ui.QuickActionsScreen
import com.jimz011apps.hki7.wear.ui.RoomScreen
import com.jimz011apps.hki7.wear.ui.SettingsScreen
import com.jimz011apps.hki7.wear.ui.SetupScreen
import com.jimz011apps.hki7.wear.ui.theme.HkiWearColors
import com.jimz011apps.hki7.wear.ui.theme.HkiWearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HkiWearApp() }
    }
}

private object Routes {
    const val HOME = "home"
    const val ROOM = "room"
    const val SETTINGS = "settings"
}

@Composable
fun HkiWearApp(viewModel: WearViewModel = viewModel()) {
    HkiWearTheme {
        val navController = rememberSwipeDismissableNavController()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val setupError by viewModel.setupError.collectAsStateWithLifecycle()
        val sensorsEnabled by viewModel.sensorsEnabled.collectAsStateWithLifecycle()

        // The socket lives exactly as long as a screen is visible. repeatOnLifecycle cancels the
        // block when the watch sleeps or the wrist drops, which closes it; raising the wrist opens
        // it again. This is what makes a light switched at the wall move here without asking.
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.runLiveUpdates()
            }
        }
        // Held here rather than passed through the route: a room carries a list of entity ids,
        // which has no business being serialised into a navigation argument.
        var openRoom by remember { mutableStateOf<WearRoom?>(null) }
        var busy by remember { mutableStateOf(false) }

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .background(HkiWearColors.Background)
                .padding(horizontal = 8.dp),
        ) {
            composable(Routes.HOME) {
                when (val current = state) {
                    is WearUiState.Ready -> QuickActionsScreen(
                        quickActions = current.quickActions,
                        rooms = current.rooms,
                        entities = current.entities,
                        refreshing = current.refreshing,
                        error = current.error,
                        onRun = { viewModel.run(it) },
                        onOpenRoom = { room ->
                            openRoom = room
                            viewModel.openRoom(room)
                            navController.navigate(Routes.ROOM)
                        },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                    WearUiState.NotConfigured, WearUiState.SigningIn -> SetupScreen(
                        busy = busy,
                        signingIn = current is WearUiState.SigningIn,
                        error = setupError,
                        onResync = {
                            busy = true
                            viewModel.resync { busy = false }
                        },
                        onSignIn = { viewModel.signInManually(it) },
                    )
                    // Loading resolves to one of the others almost immediately; showing the setup
                    // copy in the meantime would flash the wrong explanation.
                    WearUiState.Loading -> Unit
                }
            }
            composable(Routes.ROOM) {
                val room = openRoom
                if (room == null) {
                    navController.popBackStack()
                } else {
                    RoomScreen(
                        room = room,
                        entities = (state as? WearUiState.Ready)?.entities.orEmpty(),
                        onToggle = { viewModel.toggle(it) },
                    )
                }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    busy = busy,
                    sensorsEnabled = sensorsEnabled,
                    onSensorsEnabledChange = { viewModel.setSensorsEnabled(it) },
                    onRefresh = { viewModel.refresh() },
                    onResync = {
                        busy = true
                        viewModel.resync { busy = false }
                    },
                )
            }
        }
    }
}

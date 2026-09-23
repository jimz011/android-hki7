package com.jimz011apps.hki7.car

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.data.Hki7Endpoint
import com.jimz011apps.hki7.data.HomeAssistantClient
import com.jimz011apps.hki7.data.PreferencesManager
import com.jimz011apps.hki7.data.QuickActions
import com.jimz011apps.hki7.ui.components.defaultEntityIconSlug
import com.jimz011apps.hki7.ui.components.entityStateLabelRes
import com.jimz011apps.hki7.ui.components.rawEntityStateLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The whole Android Auto surface: one grid of the user's quick actions.
 *
 * Deliberately a single screen with no navigation into it. Android Auto limits how deep a task may
 * go and hides content past a host-defined limit while the car is moving, so anything that needed
 * a second tap to reach would be unreachable exactly when the car surface is being used. Choosing
 * *what* is on the grid happens on the phone, in Settings, where there is room to do it properly.
 *
 * State is pushed, not polled: one Home Assistant WebSocket subscription runs for as long as the
 * screen is started, and every `state_changed` for a watched entity redraws the grid. That is why
 * there is no refresh control — a light someone switched at the wall updates here on its own, and
 * a manual refresh button on a car screen is a control the driver should never have needed.
 * (The Wear OS app deliberately does the opposite and polls over REST: a watch cannot afford to
 * hold a socket open, whereas a phone driving a car screen is usually plugged in.)
 */
class QuickActionsCarScreen(carContext: CarContext) : Screen(carContext) {

    /** A quick action with everything the template needs already resolved. */
    private data class CarItem(
        val quickAction: HKIQuickAction,
        /** Home whose state produced this item; taps stay on that home during a switch. */
        val target: CarActionTarget,
        val title: String,
        val stateLabel: String?,
        val icon: CarIcon,
    )

    private sealed interface State {
        data object Loading : State

        /** No credentials stored: the user has not connected HKI 7 to Home Assistant yet. */
        data object NotConfigured : State

        /** Connected, but the curated list is empty. */
        data object Empty : State

        /** Credentials exist, but the current connection or refresh attempt failed. */
        data object Unavailable : State

        data class Ready(val items: List<CarItem>) : State
    }

    private var state: State = State.Loading

    /**
     * Lives for as long as the screen does, rather than one client per call.
     *
     * A subscription needs a connection that outlives the request that opened it, and reusing one
     * client also keeps the socket warm so a tap does not pay for a fresh TLS handshake.
     */
    private var client: HomeAssistantClient? = null

    /** The entities currently on the grid, kept up to date by the subscription. */
    private val entities = mutableMapOf<String, HAEntity>()
    private var quickActions: List<HKIQuickAction> = emptyList()
    private var renderedInstanceId: String? = null
    private var hasRenderedInstance = false

    init {
        // Collected off the main thread, matching how MainViewModel runs the same subscription.
        lifecycleScope.launch(Dispatchers.Default) {
            // Re-runs on every return to the foreground: the phone may have been used to edit the
            // list, or an admin may have restricted an entity, since the car last showed this.
            // Leaving the foreground cancels the block, which tears the subscription down with it.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val prefs = PreferencesManager(carContext)
                prefs.ensureHomeAssistantInstanceStore()
                prefs.activeHomeAssistantInstanceId
                    .distinctUntilChanged()
                    .collectLatest(::loadAndSubscribe)
            }
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                client?.dispose()
                client = null
            }
        })
    }

    /** [invalidate] is main-thread only, and everything here runs on [Dispatchers.Default]. */
    private suspend fun redraw() = withContext(Dispatchers.Main) { invalidate() }

    private suspend fun loadAndSubscribe(instanceId: String?) {
        // Only show the spinner when there is nothing to show. Coming back to the car screen with
        // items already rendered should not blank them while the socket reconnects.
        if (state !is State.Ready || carInstanceChanged(
                hasRenderedInstance, renderedInstanceId, instanceId
            )
        ) {
            state = State.Loading
            redraw()
        }

        client?.dispose()
        client = null
        entities.clear()
        if (instanceId == null) {
            state = State.NotConfigured
            renderedInstanceId = null
            hasRenderedInstance = true
            redraw()
            return
        }
        val active = try {
            Hki7Endpoint.createClient(carContext, instanceId)?.also { client = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state = State.Unavailable
            renderedInstanceId = instanceId
            hasRenderedInstance = true
            redraw()
            return
        }
        if (active == null) {
            state = State.NotConfigured
            renderedInstanceId = instanceId
            hasRenderedInstance = true
            redraw()
            return
        }

        quickActions = QuickActions.forCar(carContext).take(gridLimit())
        if (quickActions.isEmpty()) {
            state = State.Empty
            renderedInstanceId = instanceId
            hasRenderedInstance = true
            redraw()
            return
        }

        // Seed with a snapshot; the subscription only carries changes, so without this the grid
        // would stay blank until something in the house happened to move.
        val initialEntities = try {
            coroutineScope {
                quickActions
                    .map { async { runCatching { active.getEntityState(it.entityId) }.getOrNull() } }
                    .awaitAll()
                    .filterNotNull()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        initialEntities.forEach { entities[it.entity_id] = it }

        rebuild(instanceId)

        // Runs until the screen stops, at which point repeatOnLifecycle cancels it and the
        // subscription is torn down with it.
        try {
            active.subscribeStateChanges().collect { change ->
                if (quickActions.none { it.entityId == change.entityId }) return@collect
                val newState = change.newState
                if (newState == null) entities.remove(change.entityId)
                else entities[change.entityId] = newState
                rebuild(instanceId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Keep the last snapshot visible; a lifecycle restart will reconnect.
        } finally {
            if (client === active) {
                active.dispose()
                client = null
            }
        }
    }

    /** Rebuilds the rendered items from the current entity states and redraws. */
    private suspend fun rebuild(instanceId: String) {
        // Rasterising glyphs loads the icon-font tables on first use; keep that off the main thread.
        val items = withContext(Dispatchers.Default) {
            quickActions.map { quickAction ->
                val entity = entities[quickAction.entityId]
                CarItem(
                    quickAction = quickAction,
                    target = CarActionTarget(instanceId),
                    title = title(quickAction, entity),
                    stateLabel = entity?.let(::stateLabel),
                    icon = icon(quickAction, entity),
                )
            }
        }
        state = State.Ready(items)
        renderedInstanceId = instanceId
        hasRenderedInstance = true
        redraw()
    }

    override fun onGetTemplate(): Template = when (val current = state) {
        State.Loading -> GridTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_quick_actions_title))
            .setHeaderAction(Action.APP_ICON)
            .setLoading(true)
            .build()

        State.NotConfigured -> message(carContext.getString(R.string.car_not_configured))
        State.Empty -> message(carContext.getString(R.string.car_no_actions))
        State.Unavailable -> message(carContext.getString(R.string.car_action_failed))

        is State.Ready -> {
            val list = ItemList.Builder()
            current.items.forEach { item ->
                val grid = GridItem.Builder()
                    .setTitle(item.title)
                    .setImage(item.icon)
                    .setOnClickListener { run(item) }
                item.stateLabel?.let { grid.setText(it) }
                list.addItem(grid.build())
            }
            GridTemplate.Builder()
                .setTitle(carContext.getString(R.string.car_quick_actions_title))
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(list.build())
                .build()
        }
    }

    private fun message(text: String): Template = MessageTemplate.Builder(text)
        .setTitle(carContext.getString(R.string.car_quick_actions_title))
        .setHeaderAction(Action.APP_ICON)
        .build()

    /** How many items the host will actually show. Falls back to the API's own documented default
     *  when the car service is unavailable, rather than guessing an uncapped list. */
    private fun gridLimit(): Int =
        runCatching {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
        }.getOrDefault(DEFAULT_GRID_LIMIT).coerceAtLeast(1)

    private fun title(quickAction: HKIQuickAction, entity: HAEntity?): String =
        quickAction.name?.takeIf { it.isNotBlank() }
            ?: entity?.friendlyName?.takeIf { it.isNotBlank() }
            ?: quickAction.entityId

    private fun stateLabel(entity: HAEntity): String =
        entityStateLabelRes(entity.state)?.let { carContext.getString(it) }
            ?: rawEntityStateLabel(entity.state)

    private fun icon(quickAction: HKIQuickAction, entity: HAEntity?): CarIcon {
        val slug = quickAction.icon?.takeIf { it.isNotBlank() }
            ?: entity?.let { defaultEntityIconSlug(it) }
        // A GridItem must carry an image, so every fallback has to end somewhere that cannot fail.
        return CarGlyphIcons.forSlug(carContext, slug)
            ?: CarGlyphIcons.forSlug(carContext, FALLBACK_ICON_SLUG)
            ?: CarIcon.APP_ICON
    }

    private fun run(item: CarItem) {
        lifecycleScope.launch {
            val result = QuickActions.run(carContext, item.quickAction, item.target.instanceId)
            val message = when (result) {
                QuickActions.Result.Success -> carContext.getString(R.string.car_action_sent)
                QuickActions.Result.NotConfigured -> carContext.getString(R.string.car_not_configured)
                QuickActions.Result.Unsupported -> carContext.getString(R.string.car_action_unavailable)
                is QuickActions.Result.Failed -> carContext.getString(R.string.car_action_failed)
            }
            CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
            // No re-read on success: the subscription delivers the new state, which is also what
            // makes a change made anywhere else show up here.
        }
    }

    private companion object {
        /** The Car App Library's documented default grid limit, used when the service is missing. */
        const val DEFAULT_GRID_LIMIT = 6
        const val FALLBACK_ICON_SLUG = "lightning-bolt"
    }
}

internal fun carInstanceChanged(
    hasRenderedInstance: Boolean,
    renderedInstanceId: String?,
    activeInstanceId: String?,
): Boolean = !hasRenderedInstance || renderedInstanceId != activeInstanceId

internal data class CarActionTarget(val instanceId: String)

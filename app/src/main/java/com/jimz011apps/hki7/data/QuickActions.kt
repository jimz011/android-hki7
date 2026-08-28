package com.jimz011apps.hki7.data

import android.content.Context

/**
 * Runs the curated quick-action list on behalf of a phone-side surface with no view model of its
 * own — Android Auto today.
 *
 * The list itself and what each entry *means* live in `:core` ([HKIQuickAction],
 * [resolvedKind]), because the watch resolves them too and the two must never disagree. Only the
 * phone-side plumbing — preferences, credentials, the household policy cache — is here.
 *
 * Every call builds a short-lived client and disposes it, matching how [GeofenceManager] and
 * [TelemetryReporting] already reach Home Assistant from outside the UI.
 */
object QuickActions {
    sealed interface Result {
        /** The service call went through. */
        data object Success : Result

        /** No server or token stored — the user has not finished onboarding on the phone. */
        data object NotConfigured : Result

        /** Resolved to [QuickActionKind.UNSUPPORTED], or an admin has since hidden the entity. */
        data object Unsupported : Result

        data class Failed(val message: String?) : Result
    }

    /**
     * The entries a given surface may show: curated order, minus anything the household admin has
     * hidden from this user, minus anything that would do nothing.
     *
     * The policy filter is re-applied on every read rather than only when the list is edited,
     * so revoking access to an entity takes effect on the car surface without the owner of the
     * watch or car having to touch their list.
     */
    suspend fun forCar(context: Context): List<HKIQuickAction> =
        visible(context) { it.showInCar }

    /** As [forCar], for the Wear OS surface. */
    suspend fun forWatch(context: Context): List<HKIQuickAction> =
        visible(context) { it.showOnWatch }

    private suspend fun visible(
        context: Context,
        surface: (HKIQuickAction) -> Boolean,
    ): List<HKIQuickAction> {
        val prefs = PreferencesManager(context)
        val policy = prefs.enforcedSearchPolicyNow()
        return prefs.quickActionsOnce()
            .filter(surface)
            .filter { it.resolvedKind() != QuickActionKind.UNSUPPORTED }
            .filter { policy.canSearchEntity(it.entityId) }
    }

    /** Runs one entry against Home Assistant. Safe to call from any phone-side surface. */
    suspend fun run(context: Context, quickAction: HKIQuickAction): Result {
        val resolved = quickAction.resolvedAction()
        val kind = quickAction.resolvedKind()
        if (kind == QuickActionKind.UNSUPPORTED) return Result.Unsupported
        // Re-check the household policy at the moment of the call: the list may have been curated
        // before an admin restricted this entity.
        if (!PreferencesManager(context).enforcedSearchPolicyNow().canSearchEntity(quickAction.entityId)) {
            return Result.Unsupported
        }
        // Hki7Endpoint prefers the external URL, which is the right bias here — a phone driving a
        // car screen is by definition away from the home Wi-Fi the internal URL needs.
        val outcome = Hki7Endpoint.withClient(context) { client ->
            runCatching {
                when (kind) {
                    QuickActionKind.TOGGLE -> client.toggleEntity(quickAction.targetEntityId()!!)
                    QuickActionKind.CALL_SERVICE -> {
                        val service = resolved.service!!
                        client.callServiceRaw(
                            service.substringBefore('.'),
                            service.substringAfter('.'),
                            buildHKIActionServicePayload(resolved, quickAction.entityId),
                        )
                    }
                    QuickActionKind.UNSUPPORTED -> Unit
                }
            }
        } ?: return Result.NotConfigured
        return outcome.fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failed(it.message) },
        )
    }
}

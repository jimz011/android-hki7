package com.jimz011apps.hki7.wear.data

import com.jimz011apps.hki7.data.HKIQuickAction
import com.jimz011apps.hki7.data.QuickActionKind
import com.jimz011apps.hki7.data.buildHKIActionServicePayload
import com.jimz011apps.hki7.data.resolvedAction
import com.jimz011apps.hki7.data.resolvedKind
import com.jimz011apps.hki7.data.targetEntityId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The `{"entity_id": …}` body every plain service call needs. */
internal fun buildEntityPayload(entityId: String): JsonObject = buildJsonObject {
    put("entity_id", entityId)
}

/**
 * Runs a quick action from the watch.
 *
 * The *decision* about what an entry means — whether `default` is a toggle, a scene turn-on or
 * nothing at all — comes from `:core`, the same code the phone and the car run. Only the
 * transport differs. If this file ever grows its own opinion about what a domain does, the same
 * shortcut will start behaving differently depending on which device the user reached for.
 */
object WearActions {
    sealed interface Result {
        data object Success : Result
        data object NotConfigured : Result
        data object Unsupported : Result
        data class Failed(val message: String?) : Result
    }

    suspend fun run(client: HomeAssistantRest?, quickAction: HKIQuickAction): Result {
        if (client == null) return Result.NotConfigured
        val kind = quickAction.resolvedKind()
        if (kind == QuickActionKind.UNSUPPORTED) return Result.Unsupported
        val resolved = quickAction.resolvedAction()
        return runCatching {
            when (kind) {
                QuickActionKind.TOGGLE -> client.toggle(quickAction.targetEntityId()!!)
                QuickActionKind.CALL_SERVICE -> {
                    val service = resolved.service!!
                    client.callService(
                        service.substringBefore('.'),
                        service.substringAfter('.'),
                        buildHKIActionServicePayload(resolved, quickAction.entityId),
                    )
                }
                QuickActionKind.UNSUPPORTED -> Unit
            }
        }.fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failed(it.message) },
        )
    }

    /** Toggles a plain entity from the room browser, where there is no quick action involved. */
    suspend fun toggle(client: HomeAssistantRest?, entityId: String): Result {
        if (client == null) return Result.NotConfigured
        return runCatching { client.toggle(entityId) }.fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failed(it.message) },
        )
    }
}

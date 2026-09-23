package com.jimz011apps.hki7.data

/**
 * A local change that can be replayed on top of a newer server copy of a list.
 *
 * Keeping the user's intent instead of just a replacement list is what lets a snapshot received
 * during the debounce window include edits made by another family member without losing our edit.
 */
internal sealed interface TidyShopChange {
    fun applyTo(list: TidyShopList): TidyShopList

    data class SetChecked(
        val itemId: String,
        val checked: Boolean,
        val updatedAt: Long,
    ) : TidyShopChange {
        override fun applyTo(list: TidyShopList): TidyShopList = list.copy(
            items = list.items.map { item ->
                if (item.id == itemId) item.copy(checked = checked, updatedAt = updatedAt) else item
            },
        )
    }

    data class Add(val item: TidyShopItem) : TidyShopChange {
        override fun applyTo(list: TidyShopList): TidyShopList =
            if (list.items.any { it.id == item.id }) list else list.copy(items = list.items + item)
    }

    data class Rename(
        val itemId: String,
        val name: String,
        val quantity: Int,
        val updatedAt: Long,
    ) : TidyShopChange {
        override fun applyTo(list: TidyShopList): TidyShopList = list.copy(
            items = list.items.map { item ->
                if (item.id == itemId) {
                    item.copy(name = name, quantity = quantity, updatedAt = updatedAt)
                } else item
            },
        )
    }

    data class Remove(val itemIds: Set<String>) : TidyShopChange {
        override fun applyTo(list: TidyShopList): TidyShopList =
            list.copy(items = list.items.filterNot { it.id in itemIds })
    }
}

internal data class TidyShopPushAttempt(
    val list: TidyShopList,
    internal val throughSequence: Long,
    internal val remoteRevision: Long,
)

internal data class TidyShopPushCompletion(
    val list: TidyShopList,
    val needsPush: Boolean,
)

internal data class TidyShopSnapshotMerge(
    val lists: List<TidyShopList>,
    /** Pending lists absent from a full snapshot were deleted remotely. */
    val deletedPendingListIds: Set<String>,
)

/**
 * Reconciles authoritative server copies with operations that have not been acknowledged yet.
 * All methods are called under [TidyShopSync]'s state lock.
 */
internal class TidyShopPendingChanges {
    private data class SequencedChange(val sequence: Long, val change: TidyShopChange)
    private data class PendingList(
        var remote: TidyShopList,
        var remoteRevision: Long = 0,
        val changes: MutableList<SequencedChange> = mutableListOf(),
    )

    private val pending = mutableMapOf<String, PendingList>()
    private var nextSequence = 0L

    fun add(current: TidyShopList, change: TidyShopChange): TidyShopList {
        val state = pending.getOrPut(current.id) { PendingList(remote = current) }
        state.changes += SequencedChange(++nextSequence, change)
        return state.render()
    }

    fun mergeRemote(list: TidyShopList): TidyShopList {
        val state = pending[list.id] ?: return list
        state.remote = list
        state.remoteRevision++
        return state.render()
    }

    fun mergeSnapshot(lists: List<TidyShopList>): TidyShopSnapshotMerge {
        val incomingIds = lists.mapTo(mutableSetOf()) { it.id }
        val deleted = pending.keys.filterTo(mutableSetOf()) { it !in incomingIds }
        deleted.forEach(pending::remove)
        return TidyShopSnapshotMerge(lists.map(::mergeRemote), deleted)
    }

    fun beginPush(listId: String): TidyShopPushAttempt? {
        val state = pending[listId] ?: return null
        val last = state.changes.lastOrNull() ?: return null
        return TidyShopPushAttempt(state.render(), last.sequence, state.remoteRevision)
    }

    /**
     * A response acknowledges the captured changes only if no newer server copy arrived while the
     * request was running. If one did, the same intent is retained for one rebased follow-up PUT.
     */
    fun completePush(
        listId: String,
        attempt: TidyShopPushAttempt,
        saved: TidyShopList,
    ): TidyShopPushCompletion? {
        val state = pending[listId] ?: return null
        if (state.remoteRevision == attempt.remoteRevision) {
            state.remote = saved
            state.remoteRevision++
            state.changes.removeAll { it.sequence <= attempt.throughSequence }
        }
        val rendered = state.render()
        val needsPush = state.changes.isNotEmpty()
        if (!needsPush) pending.remove(listId)
        return TidyShopPushCompletion(rendered, needsPush)
    }

    /** Drops optimistic intent after a rejected PUT and returns the newest known server copy. */
    fun failPush(listId: String): TidyShopList? = pending.remove(listId)?.remote

    fun delete(listId: String) {
        pending.remove(listId)
    }

    fun hasPending(listId: String): Boolean = pending[listId]?.changes?.isNotEmpty() == true

    fun clear() {
        pending.clear()
    }

    private fun PendingList.render(): TidyShopList =
        changes.fold(remote) { list, pendingChange -> pendingChange.change.applyTo(list) }
}

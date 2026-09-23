package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TidyShopPendingChangesTest {
    private fun item(id: String, name: String, checked: Boolean = false) =
        TidyShopItem(id = id, name = name, checked = checked)

    private fun list(vararg items: TidyShopItem) =
        TidyShopList(id = "groceries", title = "Groceries", items = items.toList())

    @Test
    fun `snapshot during debounce keeps local edit and concurrent remote edit`() {
        val changes = TidyShopPendingChanges()
        val original = list(item("milk", "Milk"))
        changes.add(original, TidyShopChange.SetChecked("milk", true, 10))

        val merged = changes.mergeSnapshot(
            listOf(list(item("milk", "Milk"), item("bread", "Bread"))),
        ).lists.single()

        assertTrue(merged.items.first { it.id == "milk" }.checked)
        assertEquals("Bread", merged.items.first { it.id == "bread" }.name)
        assertEquals(merged, changes.beginPush("groceries")?.list)
    }

    @Test
    fun `remote update during upload is rebased and retried after response`() {
        val changes = TidyShopPendingChanges()
        val original = list(item("milk", "Milk"))
        changes.add(original, TidyShopChange.SetChecked("milk", true, 10))
        val firstAttempt = changes.beginPush("groceries")!!

        changes.mergeRemote(list(item("milk", "Milk"), item("bread", "Bread")))
        val completion = changes.completePush("groceries", firstAttempt, firstAttempt.list)!!

        assertTrue(completion.needsPush)
        assertTrue(completion.list.items.first { it.id == "milk" }.checked)
        assertEquals("Bread", completion.list.items.first { it.id == "bread" }.name)
        assertEquals(completion.list, changes.beginPush("groceries")?.list)
    }

    @Test
    fun `edit made during upload remains pending after earlier edit is acknowledged`() {
        val changes = TidyShopPendingChanges()
        val original = list(item("milk", "Milk"))
        changes.add(original, TidyShopChange.SetChecked("milk", true, 10))
        val firstAttempt = changes.beginPush("groceries")!!
        changes.add(firstAttempt.list, TidyShopChange.Rename("milk", "Oat milk", 2, 11))

        val completion = changes.completePush("groceries", firstAttempt, firstAttempt.list)!!

        assertTrue(completion.needsPush)
        assertEquals("Oat milk", completion.list.items.single().name)
        assertTrue(completion.list.items.single().checked)
    }

    @Test
    fun `failed upload rolls back to newest remote state`() {
        val changes = TidyShopPendingChanges()
        val original = list(item("milk", "Milk"))
        changes.add(original, TidyShopChange.SetChecked("milk", true, 10))
        val remote = list(item("milk", "Milk"), item("bread", "Bread"))
        changes.mergeRemote(remote)

        assertEquals(remote, changes.failPush("groceries"))
        assertFalse(changes.hasPending("groceries"))
        assertNull(changes.beginPush("groceries"))
    }

    @Test
    fun `remote deletion cancels pending list and late response cannot resurrect it`() {
        val changes = TidyShopPendingChanges()
        changes.add(list(item("milk", "Milk")), TidyShopChange.SetChecked("milk", true, 10))
        val attempt = changes.beginPush("groceries")!!

        val merge = changes.mergeSnapshot(emptyList())

        assertEquals(setOf("groceries"), merge.deletedPendingListIds)
        assertTrue(merge.lists.isEmpty())
        assertNull(changes.completePush("groceries", attempt, attempt.list))
    }

    @Test
    fun `clear drops pending operations on disconnect`() {
        val changes = TidyShopPendingChanges()
        changes.add(list(item("milk", "Milk")), TidyShopChange.SetChecked("milk", true, 10))

        changes.clear()

        assertFalse(changes.hasPending("groceries"))
        assertNull(changes.beginPush("groceries"))
    }
}

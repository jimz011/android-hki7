package com.jimz011apps.hki7.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The connector serialises Go structs, so a list arrives with capitalised field names while the
 * two fields carrying json tags do not. Reading one of those wrongly is silent — the field simply
 * comes back empty — and because `PUT /v1/lists/{id}` replaces a list wholesale, a field lost on
 * the way in is a field wiped on the way out, on data belonging to the whole household.
 *
 * That is what these tests guard.
 */
class TidyShopConnectorTest {

    /** A list shaped exactly as the connector writes one, including its two lower-case fields. */
    private fun connectorPayload(): JSONObject = JSONObject(
        """
        {
          "ID": "list-1",
          "HouseholdID": "house-1",
          "OwnerID": "owner-1",
          "Title": "Groceries",
          "Icon": "🥕",
          "StoreCountryCode": "NL",
          "StoreCountryName": "Netherlands",
          "QuickAddRows": 4,
          "MaxQuickSuggestions": 60,
          "SuggestionIncludesMeasures": true,
          "UpdatedAt": 1750000000000,
          "NudgedAt": 1749000000000,
          "NudgedBy": "owner-1",
          "Items": [
            {"ID":"i1","Name":"Milk","Checked":false,"Quantity":2,"Size":500.0,"Unit":"ml","UpdatedAt":17},
            {"ID":"i2","Name":"Bread","Checked":true,"Quantity":0,"Size":0.0,"Unit":"","UpdatedAt":18}
          ],
          "permissions": {"member-1": "check", "member-2": "view"},
          "itemHistory": {"milk": 4}
        }
        """.trimIndent()
    )

    @Test
    fun `capitalised connector fields are read, not silently dropped`() {
        val list = connectorPayload().toTidyShopList()

        assertEquals("list-1", list.id)
        assertEquals("Groceries", list.title)
        assertEquals("house-1", list.householdId)
        assertEquals("owner-1", list.ownerId)
        assertEquals("NL", list.storeCountryCode)
        assertEquals("Netherlands", list.storeCountryName)
        assertEquals(4, list.quickAddRows)
        assertEquals(60, list.maxQuickSuggestions)
        assertTrue(list.suggestionIncludesMeasures)
        assertEquals(1750000000000L, list.updatedAt)

        assertEquals(2, list.items.size)
        assertEquals("Milk", list.items[0].name)
        assertEquals(2, list.items[0].quantity)
        assertEquals(500.0, list.items[0].size, 0.0)
        assertEquals("ml", list.items[0].unit)
        assertFalse(list.items[0].checked)
        assertTrue(list.items[1].checked)
        assertEquals(1, list.remaining)

        // The one field with a json tag, so it is lower-case where the rest are not.
        assertEquals("check", list.permissions["member-1"])
        assertEquals("view", list.permissions["member-2"])
    }

    @Test
    fun `a round trip preserves everything the connector overwrites on PUT`() {
        val original = connectorPayload().toTidyShopList()
        val roundTripped = original.toJson().toTidyShopList()

        // Every field updateList replaces server-side must survive, or an edit made here quietly
        // resets settings this app does not even show.
        assertEquals(original.title, roundTripped.title)
        assertEquals(original.icon, roundTripped.icon)
        assertEquals(original.storeCountryCode, roundTripped.storeCountryCode)
        assertEquals(original.storeCountryName, roundTripped.storeCountryName)
        assertEquals(original.quickAddRows, roundTripped.quickAddRows)
        assertEquals(original.maxQuickSuggestions, roundTripped.maxQuickSuggestions)
        assertEquals(original.suggestionIncludesMeasures, roundTripped.suggestionIncludesMeasures)
        assertEquals(original.items, roundTripped.items)
    }

    @Test
    fun `a check-only edit changes nothing the connector's guard inspects`() {
        // The connector refuses a check-permission member whose payload alters the title, the
        // store, the quick-add settings, the item count, or any item's name or measures. Toggling
        // through the sync controller must therefore leave all of those identical.
        val before = connectorPayload().toTidyShopList()
        val after = before.copy(
            items = before.items.map {
                if (it.id == "i1") it.copy(checked = true, updatedAt = 99) else it
            }
        )
        val sent = after.toJson().toTidyShopList()

        assertEquals(before.title, sent.title)
        assertEquals(before.icon, sent.icon)
        assertEquals(before.storeCountryCode, sent.storeCountryCode)
        assertEquals(before.quickAddRows, sent.quickAddRows)
        assertEquals(before.maxQuickSuggestions, sent.maxQuickSuggestions)
        assertEquals(before.suggestionIncludesMeasures, sent.suggestionIncludesMeasures)
        assertEquals(before.items.size, sent.items.size)
        before.items.zip(sent.items).forEach { (was, now) ->
            assertEquals(was.name, now.name)
            assertEquals(was.quantity, now.quantity)
            assertEquals(was.size, now.size, 0.0)
            assertEquals(was.unit, now.unit)
        }
        assertTrue("the tick itself is the only change", sent.items.first { it.id == "i1" }.checked)
    }

    @Test
    fun `permissions follow the connector's own rules`() {
        val list = connectorPayload().toTidyShopList()

        // The owner is never listed in permissions and always edits.
        assertEquals(TIDYSHOP_PERMISSION_EDIT, list.permissionFor("owner-1"))
        assertTrue(list.canEdit("owner-1"))

        assertEquals(TIDYSHOP_PERMISSION_CHECK, list.permissionFor("member-1"))
        assertTrue(list.canCheck("member-1"))
        assertFalse(list.canEdit("member-1"))

        assertEquals(TIDYSHOP_PERMISSION_VIEW, list.permissionFor("member-2"))
        assertFalse(list.canCheck("member-2"))

        // An unknown or absent identity gets the least, never the most.
        assertEquals(TIDYSHOP_PERMISSION_VIEW, list.permissionFor("stranger"))
        assertEquals(TIDYSHOP_PERMISSION_VIEW, list.permissionFor(null))
        assertEquals(TIDYSHOP_PERMISSION_VIEW, list.permissionFor(""))
    }

    @Test
    fun `an SSE frame is read the same way as a fetched list`() {
        val updated = JSONObject().put("origin", "some-device").put("list", connectorPayload())
        assertEquals("list-1", updated.tidyShopListFromFrame()?.id)

        val snapshot = JSONObject().put(
            "lists",
            org.json.JSONArray().put(connectorPayload()).put(connectorPayload())
        )
        assertEquals(2, snapshot.tidyShopListsFromSnapshot().size)

        // A frame with no payload must produce nothing rather than a blank list.
        assertNull(JSONObject().put("origin", "x").tidyShopListFromFrame())
        assertTrue(JSONObject().tidyShopListsFromSnapshot().isEmpty())
    }

    @Test
    fun `a typed server address becomes one this client can append paths to`() {
        assertEquals("https://tidyshop.example.com", normalizeTidyShopUrl("tidyshop.example.com"))
        assertEquals("https://tidyshop.example.com", normalizeTidyShopUrl("  https://tidyshop.example.com/  "))
        // An explicit http:// is left alone: a LAN-only connector may genuinely not have TLS.
        assertEquals("http://192.168.1.5:8787", normalizeTidyShopUrl("http://192.168.1.5:8787/"))
        assertEquals("", normalizeTidyShopUrl("   "))
    }

    @Test
    fun `measures are labelled only when there is something to say`() {
        assertEquals("", TidyShopItem(id = "a", name = "Milk").measureLabel)
        assertEquals("", TidyShopItem(id = "a", name = "Milk", quantity = 1).measureLabel)
        assertEquals("2x", TidyShopItem(id = "a", name = "Milk", quantity = 2).measureLabel)
        assertEquals("500ml", TidyShopItem(id = "a", name = "Milk", size = 500.0, unit = "ml").measureLabel)
        assertEquals(
            "2x 500ml",
            TidyShopItem(id = "a", name = "Milk", quantity = 2, size = 500.0, unit = "ml").measureLabel
        )
    }
}

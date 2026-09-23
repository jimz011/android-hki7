package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.TidyShopList
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoWidgetTidyShopTest {
    @Test
    fun `store list uses its country instead of exposing its internal icon key`() {
        val list = TidyShopList(
            id = "aldi-nl",
            title = "ALDI",
            icon = "store:nl:aldi",
            storeCountryCode = "nl",
        )

        assertEquals("ALDI (NL)", tidyShopListDisplayName(list))
    }

    @Test
    fun `country falls back to the internal store key`() {
        val list = TidyShopList(id = "aldi-de", title = "ALDI", icon = "store:de:aldi")

        assertEquals("ALDI (DE)", tidyShopListDisplayName(list))
    }

    @Test
    fun `ordinary list names are unchanged`() {
        val list = TidyShopList(id = "weekly", title = "Weekend", icon = "🛒")

        assertEquals("Weekend", tidyShopListDisplayName(list))
    }
}

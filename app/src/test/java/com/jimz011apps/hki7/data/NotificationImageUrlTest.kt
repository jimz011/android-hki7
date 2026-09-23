package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationImageUrlTest {
    @Test
    fun `relative media path resolves against Home Assistant`() {
        assertEquals(
            "https://home.example/media/local/door.jpg",
            notificationImageUrl("/media/local/door.jpg", "https://home.example/")
        )
    }

    @Test
    fun `absolute attachment remains unchanged`() {
        assertEquals(
            "https://cdn.example/door.jpg",
            notificationImageUrl("https://cdn.example/door.jpg", "https://home.example")
        )
    }

    @Test
    fun `relative attachment needs a server`() {
        assertNull(notificationImageUrl("/media/local/door.jpg", null))
    }
}

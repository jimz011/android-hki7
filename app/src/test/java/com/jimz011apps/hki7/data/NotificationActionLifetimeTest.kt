package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationActionLifetimeTest {
    private val tappedAt = 1_800_000_000_000L

    @Test fun shortOfflinePeriodKeepsOnlyTheRemainingLifetime() {
        assertEquals(240_000L, notificationActionRemainingMillis(tappedAt, tappedAt + 60_000))
    }

    @Test fun reconnectingHoursLaterExpiresEvenBeforeTheFirstAttempt() {
        assertEquals(0L, notificationActionRemainingMillis(tappedAt, tappedAt + 3_600_000))
    }

    @Test fun exactDeadlineIsExpired() {
        assertEquals(0L, notificationActionRemainingMillis(tappedAt, tappedAt + NOTIFICATION_ACTION_LIFETIME_MS))
    }

    @Test fun retryDoesNotRestartTheClock() {
        assertEquals(1_000L, notificationActionRemainingMillis(tappedAt, tappedAt + 299_000))
        assertEquals(0L, notificationActionRemainingMillis(tappedAt, tappedAt + 301_000))
    }

    @Test fun jobsFromBeforeTheUpgradeAreNotReplayed() {
        assertEquals(0L, notificationActionRemainingMillis(0, tappedAt))
    }

    @Test fun backwardsClockChangeDoesNotExtendAnAction() {
        assertEquals(0L, notificationActionRemainingMillis(tappedAt, tappedAt - 1))
    }
}

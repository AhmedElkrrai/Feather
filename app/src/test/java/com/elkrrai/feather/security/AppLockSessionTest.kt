package com.elkrrai.feather.security

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockSessionTest {

    @Before
    @After
    fun reset() {
        AppLockSession.unlocked = false
        AppLockSession.leftAt = 0L
    }

    @Test
    fun a_new_process_starts_locked() {
        assertFalse(AppLockSession.unlocked)
    }

    @Test
    fun a_quick_look_at_another_app_does_not_lock_again() {
        AppLockSession.leftAt = 1_000L
        assertFalse(AppLockSession.awayTooLong(1_000L + AppLockSession.GRACE_MILLIS))
    }

    @Test
    fun being_away_longer_than_the_grace_locks_again() {
        AppLockSession.leftAt = 1_000L
        assertTrue(AppLockSession.awayTooLong(1_000L + AppLockSession.GRACE_MILLIS + 1))
    }

    @Test
    fun never_having_left_is_not_being_away() {
        assertFalse(AppLockSession.awayTooLong(10_000_000L))
    }
}

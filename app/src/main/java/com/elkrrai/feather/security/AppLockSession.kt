package com.elkrrai.feather.security

/**
 * Whether the app has been unlocked since the process started, and when it was last left. It lives
 * in memory only: a new process (the app was killed, or the phone restarted) starts locked.
 */
object AppLockSession {

    /** After this long away from the app, it locks again; a quick look at another app does not. */
    const val GRACE_MILLIS = 30_000L

    @Volatile
    var unlocked: Boolean = false

    /** When the app was last left (the elapsed-realtime clock), or 0 if it never was. */
    @Volatile
    var leftAt: Long = 0L

    fun awayTooLong(now: Long): Boolean = leftAt != 0L && now - leftAt > GRACE_MILLIS
}

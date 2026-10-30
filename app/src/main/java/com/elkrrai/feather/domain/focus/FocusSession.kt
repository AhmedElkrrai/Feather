package com.elkrrai.feather.domain.focus

import java.util.Locale
import kotlin.math.ceil

/** A stretch of work on a task, or the rest that follows it. */
enum class FocusPhase { FOCUS, BREAK }

/**
 * One focus session on a task: a countdown that is either running (it runs out at [endsAt], a
 * wall-clock time, so it keeps going when the app is closed) or paused (with [pausedLeft] still to
 * go). It is plain data: the app keeps one at a time, and the clock is always passed in.
 */
data class FocusSession(
    val taskId: String,
    /** The task's title when the session started, for the notification. */
    val taskTitle: String,
    val phase: FocusPhase,
    val totalMillis: Long,
    /** Epoch millis when the time runs out; null while paused. */
    val endsAt: Long?,
    /** How much is left while paused; not used while running. */
    val pausedLeft: Long = 0L,
) {
    val isPaused: Boolean get() = endsAt == null

    /** Milliseconds left at [now], never below 0 or above the total. */
    fun remaining(now: Long): Long =
        if (endsAt == null) pausedLeft.coerceIn(0L, totalMillis) else (endsAt - now).coerceIn(0L, totalMillis)

    /** True once a running session has run out. A paused one never has. */
    fun isOver(now: Long): Boolean = endsAt != null && remaining(now) <= 0L

    /** How much of the time is still left, 1 at the start and 0 at the end. */
    fun fractionLeft(now: Long): Float =
        if (totalMillis <= 0L) 0f else remaining(now).toFloat() / totalMillis

    fun pause(now: Long): FocusSession = if (isPaused) this else copy(endsAt = null, pausedLeft = remaining(now))

    fun resume(now: Long): FocusSession = if (!isPaused) this else copy(endsAt = now + pausedLeft, pausedLeft = 0L)

    companion object {
        fun start(taskId: String, taskTitle: String, phase: FocusPhase, minutes: Int, now: Long): FocusSession {
            val total = minutes * MILLIS_PER_MINUTE
            return FocusSession(taskId, taskTitle, phase, total, endsAt = now + total)
        }
    }
}

const val MILLIS_PER_MINUTE = 60_000L

/** The choices for the length of a focus stretch, and the length of the rest after it. */
const val MIN_FOCUS_MINUTES = 5
const val MAX_FOCUS_MINUTES = 120
const val FOCUS_STEP_MINUTES = 5
const val DEFAULT_FOCUS_MINUTES = 25
const val BREAK_MINUTES = 5

/** [minutes] brought into the choices: from 5 to 120 in steps of 5. */
fun clampFocusMinutes(minutes: Int): Int =
    (minutes.coerceIn(MIN_FOCUS_MINUTES, MAX_FOCUS_MINUTES) / FOCUS_STEP_MINUTES) * FOCUS_STEP_MINUTES

/** The time left as "24:59" (or "1:05:00" from an hour up), rounded up to a whole second. */
fun focusClock(millisLeft: Long, locale: Locale = Locale.getDefault()): String {
    val seconds = ceil(millisLeft.coerceAtLeast(0L) / 1000.0).toLong()
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val rest = seconds % 60
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, rest)
    } else {
        String.format(locale, "%02d:%02d", minutes, rest)
    }
}

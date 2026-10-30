package com.elkrrai.feather.domain.focus

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionTest {

    private val start = 1_000_000L
    private fun session() = FocusSession.start("task", "Write", FocusPhase.FOCUS, minutes = 25, now = start)

    @Test
    fun a_new_session_has_all_its_time_left() {
        val s = session()
        assertEquals(25 * MILLIS_PER_MINUTE, s.remaining(start))
        assertEquals(1f, s.fractionLeft(start), 0.0001f)
        assertFalse(s.isPaused)
        assertFalse(s.isOver(start))
    }

    @Test
    fun the_time_runs_down_and_runs_out() {
        val s = session()
        assertEquals(20 * MILLIS_PER_MINUTE, s.remaining(start + 5 * MILLIS_PER_MINUTE))
        assertTrue(s.isOver(start + 25 * MILLIS_PER_MINUTE))
        assertEquals(0L, s.remaining(start + 40 * MILLIS_PER_MINUTE))
    }

    @Test
    fun pausing_stops_the_clock_and_resuming_carries_on() {
        val paused = session().pause(start + 10 * MILLIS_PER_MINUTE)
        assertTrue(paused.isPaused)
        // an hour later it is still 15 minutes
        assertEquals(15 * MILLIS_PER_MINUTE, paused.remaining(start + 70 * MILLIS_PER_MINUTE))
        assertFalse(paused.isOver(start + 70 * MILLIS_PER_MINUTE))

        val resumeAt = start + 70 * MILLIS_PER_MINUTE
        val resumed = paused.resume(resumeAt)
        assertEquals(resumeAt + 15 * MILLIS_PER_MINUTE, resumed.endsAt)
        assertEquals(15 * MILLIS_PER_MINUTE, resumed.remaining(resumeAt))
    }

    @Test
    fun the_length_is_kept_to_the_choices() {
        assertEquals(5, clampFocusMinutes(1))
        assertEquals(25, clampFocusMinutes(27))
        assertEquals(120, clampFocusMinutes(500))
    }

    @Test
    fun the_clock_rounds_up_to_a_whole_second() {
        assertEquals("25:00", focusClock(25 * MILLIS_PER_MINUTE, Locale.US))
        assertEquals("00:01", focusClock(1, Locale.US))
        assertEquals("1:05:00", focusClock(65 * MILLIS_PER_MINUTE, Locale.US))
        assertEquals("00:00", focusClock(-5, Locale.US))
    }
}

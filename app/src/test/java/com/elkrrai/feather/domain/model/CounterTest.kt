package com.elkrrai.feather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterTest {

    @Test
    fun adding_a_step_moves_the_count_up() {
        val counter = Counter(target = 100, step = 25).plus(25)
        assertEquals(25, counter.done)
        assertFalse(counter.isFull)
        assertEquals(0.25f, counter.fraction, 0.0001f)
    }

    @Test
    fun the_count_stops_at_the_target() {
        val counter = Counter(target = 100, step = 25, done = 90).plus(25)
        assertEquals(100, counter.done)
        assertTrue(counter.isFull)
    }

    @Test
    fun the_count_never_goes_below_zero() {
        assertEquals(0, Counter(target = 100, step = 25, done = 10).plus(-25).done)
    }

    @Test
    fun reset_goes_back_to_zero_and_keeps_the_rest() {
        val counter = Counter(target = 8, step = 1, done = 8).reset()
        assertEquals(Counter(target = 8, step = 1, done = 0), counter)
    }

    @Test
    fun no_target_means_no_counter() {
        assertNull(Counter.of(target = null, step = 5, done = 0))
        assertNull(Counter.of(target = 0, step = 5, done = 0))
    }

    @Test
    fun numbers_that_do_not_fit_are_brought_into_range() {
        val counter = Counter.of(target = 10, step = 50, done = 99)!!
        assertEquals(10, counter.step)
        assertEquals(10, counter.done)
        assertEquals(1, Counter.of(target = 10, step = null, done = -3)!!.step)
        assertEquals(0, Counter.of(target = 10, step = 1, done = -3)!!.done)
        assertEquals(Counter.MAX_TARGET, Counter.of(target = 1_000_000, step = 1, done = 0)!!.target)
    }
}

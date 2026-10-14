package com.elkrrai.feather.ui.today

import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.DayPhase
import java.time.LocalTime
import kotlin.math.roundToInt

/** Which greeting the Today header shows. */
enum class Greeting { Morning, Afternoon, Evening, Night }

/** Morning from 05:00, afternoon from 12:00, evening from 17:00, night from 21:00. */
fun greetingFor(time: LocalTime): Greeting = when (time.hour) {
    in 5..11 -> Greeting.Morning
    in 12..16 -> Greeting.Afternoon
    in 17..20 -> Greeting.Evening
    else -> Greeting.Night
}

/** The sky behind the header: dawn from 05:00, day from 10:00, dusk from 17:00, night from 20:00. */
fun dayPhaseFor(time: LocalTime): DayPhase = when (time.hour) {
    in 5..9 -> DayPhase.Dawn
    in 10..16 -> DayPhase.Day
    in 17..19 -> DayPhase.Dusk
    else -> DayPhase.Night
}

/**
 * How today is going: [left] open tasks that are due today or earlier, and [done] tasks finished
 * today.
 */
data class TodayStatus(val left: Int = 0, val done: Int = 0) {
    val planned: Int get() = left + done
    val allDone: Boolean get() = planned > 0 && left == 0
}

/**
 * What the bird is doing in the header: asleep when nothing is planned, happy when everything is
 * done, sleepy late in the evening when only a couple of things remain, and otherwise sitting
 * about.
 */
fun todayBirdMood(status: TodayStatus, phase: DayPhase): BirdMood = when {
    status.planned == 0 -> BirdMood.SLEEPING
    status.left == 0 -> BirdMood.HAPPY
    phase == DayPhase.Night && status.left <= 2 -> BirdMood.SLEEPING
    else -> BirdMood.IDLE
}

/** The number of decorative leaves that are filled, out of [total]: the share of planned tasks that are done. */
fun leavesFilled(status: TodayStatus, total: Int = 5): Int {
    if (status.planned == 0) return 0
    val filled = (status.done * total / status.planned.toFloat()).roundToInt().coerceIn(0, total)
    // Never show every leaf as filled while something is left, or none while something is done.
    return when {
        status.left > 0 -> filled.coerceAtMost(total - 1)
        else -> total
    }.let { if (status.done > 0) it.coerceAtLeast(1) else it }
}

/** Whether the header shows a moon (after dark) or a sun. */
fun showsMoon(phase: DayPhase): Boolean = phase == DayPhase.Night

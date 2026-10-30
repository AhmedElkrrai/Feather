package com.elkrrai.feather.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The week of the whole app starts on Saturday and ends on Friday: Your rhythm, habits, the weekly
 * feather, repeats every few weeks, and "next week" in quick add all count it this way. It is one
 * fixed choice, not a setting.
 */
val FIRST_DAY_OF_WEEK: DayOfWeek = DayOfWeek.SATURDAY

/** The first day of the week that this date is in. */
fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(FIRST_DAY_OF_WEEK))

/** The first day of the week after the one this date is in. */
fun LocalDate.startOfNextWeek(): LocalDate = with(TemporalAdjusters.next(FIRST_DAY_OF_WEEK))

/** Where a weekday falls in the week: 0 for the first day (Saturday) to 6 for the last (Friday). */
fun DayOfWeek.indexInWeek(): Int = Math.floorMod(value - FIRST_DAY_OF_WEEK.value, 7)

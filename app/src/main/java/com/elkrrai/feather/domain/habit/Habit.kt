package com.elkrrai.feather.domain.habit

import java.time.Instant
import java.time.LocalDate

/**
 * What a habit counts toward each week: [DAYS] a number of days ("workout 3 days a week", or "pray
 * 5 times a day, 7 days a week" when a day needs several), or [AMOUNT] a total that adds up over the
 * week ("read 20 pages").
 */
enum class HabitKind(val code: Int) {
    DAYS(0),
    AMOUNT(1),
    ;

    companion object {
        fun fromCode(code: Int): HabitKind = entries.firstOrNull { it.code == code } ?: DAYS
    }
}

/** A habit. Its goals and what was logged are in [HabitState]; habits are not tasks and never show with them. */
data class Habit(
    val id: String,
    val name: String,
    val kind: HabitKind,
    /** What an [HabitKind.AMOUNT] habit counts (pages, km, minutes); empty for the other kind. */
    val unit: String = "",
    val sortOrder: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * The goal from [effectiveFrom] on, the first day of a week. A goal never changes for a week that
 * has begun: an edit made during a week is a new goal that takes effect at the start of the next one.
 */
data class HabitGoal(
    val effectiveFrom: LocalDate,
    /** [HabitKind.DAYS]: days a week (1 to 7). [HabitKind.AMOUNT]: the total for the week. */
    val target: Int,
    /** [HabitKind.DAYS] only: how many times a day counts as done (1 is a single tap). */
    val dailyTarget: Int = 1,
)

/** A habit with its goals (oldest first) and what was logged on each day (days with nothing logged are absent). */
data class HabitState(
    val habit: Habit,
    val goals: List<HabitGoal>,
    val entries: Map<LocalDate, Int>,
)

const val MAX_HABIT_TARGET = 9_999
const val MAX_DAILY_TARGET = 99
const val DAYS_IN_WEEK = 7

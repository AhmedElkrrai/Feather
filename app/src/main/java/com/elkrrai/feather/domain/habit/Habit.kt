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
    /** Show the habit as a row in Today. Off for a habit that was made before this existed. */
    val showInToday: Boolean = false,
    /** The habit's own time of day, when it has no stages (with stages, each stage has its own). */
    val time: DayTime = DayTime(),
    /** How the stages show in Today; [StageMode.NONE] for a habit without stages. */
    val stageMode: StageMode = StageMode.NONE,
    /** Show it in Today only from its start time, not all day. */
    val todayFromStart: Boolean = false,
    /** Notify at its time (the start of each stage's time). */
    val remind: Boolean = false,
    /** The look of its weekly leaf, a code from 0 to [LEAF_LOOKS] - 1 dealt when the habit was made; negative until it is dealt. */
    val leaf: Int = -1,
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
    /** The stages that count from [effectiveFrom] on, in order (ids into [HabitState.stages]); empty for a habit without stages. */
    val stageIds: List<String> = emptyList(),
)

/**
 * A habit with its goals (oldest first) and what was logged on each day (days with nothing logged are absent). For a habit with stages,
 * [stages] are all it ever had (by id), and [stageLog] says which were done on each day; the day's amount in [entries] is how many.
 */
data class HabitState(
    val habit: Habit,
    val goals: List<HabitGoal>,
    val entries: Map<LocalDate, Int>,
    val stages: Map<String, HabitStage> = emptyMap(),
    val stageLog: Map<LocalDate, Set<String>> = emptyMap(),
    /** For a habit that counts an amount: what was logged on each day, oldest first. The day's amount in [entries] is their sum. */
    val logs: Map<LocalDate, List<HabitLog>> = emptyMap(),
)

/** One thing logged on a day. [loggedAt] is null for an amount from before logs were kept and for a log added to an earlier day. */
data class HabitLog(val id: String, val amount: Int, val loggedAt: Instant?)

const val MAX_HABIT_TARGET = 9_999
const val MAX_DAILY_TARGET = 1_000
const val DAYS_IN_WEEK = 7

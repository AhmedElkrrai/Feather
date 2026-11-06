package com.elkrrai.feather.domain.habit

import java.time.LocalDate

/** How a time of day is given: not at all, as one moment, or as a stretch of the day (a window). */
enum class TimeMode(val code: Int) {
    NONE(0),
    AT(1),
    WINDOW(2),
    ;

    companion object {
        fun fromCode(code: Int): TimeMode = entries.firstOrNull { it.code == code } ?: NONE
    }
}

/** A time of day: [startMinute] (and for a window [endMinute]) are minutes since midnight. */
data class DayTime(
    val mode: TimeMode = TimeMode.NONE,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
) {
    val isSet: Boolean get() = mode != TimeMode.NONE && startMinute != null

    /** The time with its numbers made sense of: a window that does not end after it starts is one moment, a missing start is no time. */
    fun normalized(): DayTime {
        val start = startMinute?.coerceIn(0, LAST_MINUTE) ?: return DayTime()
        return when (mode) {
            TimeMode.NONE -> DayTime()
            TimeMode.AT -> DayTime(TimeMode.AT, start)
            TimeMode.WINDOW -> {
                val end = endMinute?.coerceIn(0, LAST_MINUTE)
                if (end == null || end <= start) DayTime(TimeMode.AT, start) else DayTime(TimeMode.WINDOW, start, end)
            }
        }
    }

    private companion object {
        const val LAST_MINUTE = 24 * 60 - 1
    }
}

/** How a habit with stages shows them in Today: all at once as a checklist, or one at a time, the current one first. */
enum class StageMode(val code: Int) {
    NONE(0),
    ALL(1),
    ORDERED(2),
    ;

    companion object {
        fun fromCode(code: Int): StageMode = entries.firstOrNull { it.code == code } ?: NONE
    }
}

/**
 * One of the named times of a day of a habit, like Fajr of "Pray". A stage keeps its [id] for life, so it can be renamed or moved in time without
 * touching what was logged.
 */
data class HabitStage(
    val id: String,
    val name: String,
    val time: DayTime = DayTime(),
)

/** The most stages a habit can have. */
const val MAX_STAGES = 12

/** The stages that count for the week that starts on [weekStart], in order: the list of the goal in force that week. */
fun HabitState.stagesFor(weekStart: LocalDate): List<HabitStage> =
    goals.forWeek(weekStart)?.stageIds.orEmpty().mapNotNull { stages[it] }

/** The stages of the list that waits for the next week, or null when no change of the goal is waiting. */
fun HabitState.queuedStagesAfter(weekStart: LocalDate): List<HabitStage>? =
    goals.queuedAfter(weekStart)?.stageIds?.mapNotNull { stages[it] }

/** The ids of the stages that were done on [date]. */
fun HabitState.stagesDoneOn(date: LocalDate): Set<String> = stageLog[date].orEmpty()

/**
 * Whether [date] is finished for a habit that counts days: enough of its stages are done, or the day's count is reached. An amount habit has no
 * finished day.
 */
fun HabitState.isDayDone(date: LocalDate, weekStart: LocalDate): Boolean {
    val goal = goals.forWeek(weekStart) ?: return false
    if (habit.kind == HabitKind.AMOUNT) return false
    val needed = goal.dailyTarget.coerceAtLeast(1)
    val stages = stagesFor(weekStart)
    val done = stagesDoneOn(date)
    return if (stages.isNotEmpty()) {
        stages.count { it.id in done } >= needed.coerceAtMost(stages.size)
    } else {
        (entries[date] ?: 0) >= needed
    }
}

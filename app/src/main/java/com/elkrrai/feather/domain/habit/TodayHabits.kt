package com.elkrrai.feather.domain.habit

import java.time.LocalDate
import java.time.LocalTime

/** Where a stage of a habit stands today. A missed stage is never a failure: it is only a stage whose time has passed. */
enum class StageState { DONE, MISSED, NOW, NEXT }

data class TodayStage(val stage: HabitStage, val state: StageState)

/** A habit as one row of Today. */
data class TodayHabit(
    val id: String,
    val name: String,
    val kind: HabitKind,
    val unit: String,
    /** The stages in order, or empty for a habit without stages. */
    val stages: List<TodayStage>,
    val mode: StageMode,
    /** The stage the row names when it shows one at a time: the one it is time for, or the next; null when none is left. */
    val current: TodayStage?,
    /** The time shown on the row: the current stage's, or the habit's own. */
    val time: DayTime,
    /** Today is finished: enough stages are done, or the day's count is reached. */
    val doneToday: Boolean,
    /** Stages done of stages; for a habit without stages, the count today of the times a day it needs. */
    val doneCount: Int,
    val totalCount: Int,
    /** How far the week is: what counts so far of what the goal asks (days, or the amount). */
    val weekTotal: Int,
    val weekTarget: Int,
    /** Minutes since midnight the row sorts by, or null for a habit with no time. */
    val sortMinute: Int?,
    /** What was logged today (the amount for an amount habit). */
    val todayAmount: Int = 0,
)

/**
 * The habits that show in Today at [now], as rows: a habit it is time for right now first (the one with the "Now" badge), then the rest in the
 * order of their times (a habit without a time after those that have one), and a habit finished today after the others.
 *
 * A habit shows when it is switched on for Today, has a goal this week, and its week is not already met by other days: when the goal is met
 * the habit rests until next week, except that a habit finished today is still listed (as done) so the row can say so before it leaves. A
 * habit set to show only from its time stays out of the way until that time.
 */
fun todayHabits(habits: List<HabitState>, today: LocalDate, now: LocalTime, weekStart: LocalDate): List<TodayHabit> {
    val minute = now.hour * 60 + now.minute
    return habits.mapNotNull { todayHabit(it, today, minute, weekStart) }
        .sortedWith(compareBy<TodayHabit>({ it.doneToday }, { it.current?.state != StageState.NOW }, { it.sortMinute ?: Int.MAX_VALUE }))
}

private fun todayHabit(state: HabitState, today: LocalDate, minute: Int, weekStart: LocalDate): TodayHabit? {
    val habit = state.habit
    if (!habit.showInToday) return null
    val progress = state.week(weekStart)
    val goal = progress.goal ?: return null

    if (habit.kind == HabitKind.AMOUNT) {
        // met by the other days: it rests. Met by what was logged today: it is listed, done, so the row can celebrate before it leaves.
        if (progress.met && (state.entries[today] ?: 0) == 0) return null
        return TodayHabit(
            id = habit.id, name = habit.name, kind = habit.kind, unit = habit.unit,
            stages = emptyList(), mode = StageMode.NONE, current = null, time = habit.time,
            doneToday = progress.met, doneCount = progress.total, totalCount = progress.target,
            weekTotal = progress.total, weekTarget = progress.target,
            sortMinute = habit.time.start(),
            todayAmount = state.entries[today] ?: 0,
        )
    }

    val stages = state.stagesFor(weekStart)
    val doneIds = state.stagesDoneOn(today)
    val needed = goal.dailyTarget.coerceAtLeast(1)
    val doneStages = stages.count { it.id in doneIds }
    val doneToday = if (stages.isNotEmpty()) doneStages >= needed.coerceAtMost(stages.size) else (state.entries[today] ?: 0) >= needed
    if (progress.met && !doneToday) return null

    // From its time: out of the way until the earliest time of the habit starts.
    val earliest = (if (stages.isNotEmpty()) stages.mapNotNull { it.time.start() } else listOfNotNull(habit.time.start())).minOrNull()
    if (habit.todayFromStart && earliest != null && minute < earliest && !doneToday) return null

    if (stages.isEmpty()) {
        return TodayHabit(
            id = habit.id, name = habit.name, kind = habit.kind, unit = habit.unit,
            stages = emptyList(), mode = StageMode.NONE, current = null, time = habit.time,
            doneToday = doneToday, doneCount = state.entries[today] ?: 0, totalCount = needed,
            weekTotal = progress.total, weekTarget = progress.target,
            sortMinute = habit.time.start(),
            todayAmount = state.entries[today] ?: 0,
        )
    }

    val states = stageStates(stages, doneIds, minute)
    val rows = stages.mapIndexed { i, stage -> TodayStage(stage, states[i]) }
    val current = rows.firstOrNull { it.state == StageState.NOW }
        ?: rows.filter { it.state == StageState.NEXT }.minByOrNull { it.stage.time.start() ?: Int.MAX_VALUE }
    return TodayHabit(
        id = habit.id, name = habit.name, kind = habit.kind, unit = habit.unit,
        stages = rows, mode = habit.stageMode.takeIf { it != StageMode.NONE } ?: StageMode.ORDERED, current = current,
        time = current?.stage?.time ?: DayTime(),
        doneToday = doneToday, doneCount = doneStages, totalCount = stages.size,
        weekTotal = progress.total, weekTarget = progress.target,
        sortMinute = (current?.stage?.time ?: stages.first().time).start(),
        todayAmount = state.entries[today] ?: 0,
    )
}

/**
 * The state of each stage at [minute]. A stage that is done is done. One whose time has not come is next, as is one with no time. A window that has
 * ended is missed. Of the stages that are due (their time has come and a window is not over) only the latest is the one it is time for; the earlier
 * ones are missed.
 */
internal fun stageStates(stages: List<HabitStage>, doneIds: Set<String>, minute: Int): List<StageState> {
    val raw = stages.map { stage ->
        val time = stage.time
        val start = time.startMinute
        when {
            stage.id in doneIds -> StageState.DONE
            !time.isSet || start == null || minute < start -> StageState.NEXT
            time.mode == TimeMode.WINDOW && time.endMinute != null && minute > time.endMinute -> StageState.MISSED
            else -> StageState.NOW
        }
    }
    val latestDue = stages.indices.filter { raw[it] == StageState.NOW }.maxByOrNull { stages[it].time.startMinute ?: 0 }
    return raw.mapIndexed { i, state -> if (state == StageState.NOW && i != latestDue) StageState.MISSED else state }
}

/** The start of the time in minutes since midnight, or null when no time is set. */
private fun DayTime.start(): Int? = if (isSet) startMinute else null

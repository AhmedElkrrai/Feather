package com.elkrrai.feather.domain.habit

import java.time.LocalDate

/** A stage as typed in the form. A stage that already exists comes with its [id]; a new one has none. */
data class StageDraft(
    val id: String? = null,
    val name: String,
    val time: DayTime = DayTime(),
)

/**
 * Everything the form of a habit sets. The name, the unit, the times and the Today settings take effect at once; the goal
 * ([target], [dailyTarget] and the list of [stages]) takes effect at the start of next week when the habit already exists.
 */
data class HabitDraft(
    val name: String,
    val kind: HabitKind,
    val unit: String = "",
    val target: Int,
    /** How many times a day counts as done. For a habit with stages: how many of them, at most all. */
    val dailyTarget: Int = 1,
    val showInToday: Boolean = false,
    /** The habit's own time, for a habit without stages. */
    val time: DayTime = DayTime(),
    val stageMode: StageMode = StageMode.NONE,
    /** Show it in Today only from its start time (the first stage's time, or its own) and not all day. */
    val todayFromStart: Boolean = false,
    val remind: Boolean = false,
    val stages: List<StageDraft> = emptyList(),
)

/**
 * The habit as the form shows it now: the goal and the stage list shown are the ones waiting for next week, if there are some, else
 * this week's, so an edit that was already made is not undone by opening the form and saving.
 */
fun HabitState.toDraft(weekStart: LocalDate): HabitDraft {
    val shown = goals.queuedAfter(weekStart) ?: goals.forWeek(weekStart)
    return HabitDraft(
        name = habit.name,
        kind = habit.kind,
        unit = habit.unit,
        target = shown?.target ?: if (habit.kind == HabitKind.DAYS) 3 else 20,
        dailyTarget = shown?.dailyTarget ?: 1,
        showInToday = habit.showInToday,
        time = habit.time,
        stageMode = habit.stageMode,
        todayFromStart = habit.todayFromStart,
        remind = habit.remind,
        stages = shown?.stageIds.orEmpty().mapNotNull { stages[it] }.map { StageDraft(it.id, it.name, it.time) },
    )
}

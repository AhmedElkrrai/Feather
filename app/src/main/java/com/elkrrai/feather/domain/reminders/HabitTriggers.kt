package com.elkrrai.feather.domain.reminders

import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitStage
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.forWeek
import com.elkrrai.feather.domain.habit.isDayDone
import com.elkrrai.feather.domain.habit.stagesDoneOn
import com.elkrrai.feather.domain.habit.stagesFor
import com.elkrrai.feather.domain.habit.week
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * How long after its time a habit reminder that was missed (phone off, app killed) is still shown. Much shorter than for a task: a prayer
 * reminder that comes hours late is noise, not help.
 */
val HABIT_CATCH_UP: Duration = Duration.ofHours(2)

/** A habit reminder that is ready: the stage it is for (null for a habit without stages) and the moment it was meant for. */
data class PendingHabitReminder(
    val habit: Habit,
    val stage: HabitStage?,
    val date: LocalDate,
    val trigger: Instant,
) {
    /** What marks it as shown, so the same stage on the same day is shown once. */
    val key: String get() = habitReminderKey(habit.id, stage?.id, date)
}

/** What to show now and when to wake up next for habits, like [ReminderPlan] for tasks. */
data class HabitReminderPlan(
    val due: List<PendingHabitReminder>,
    val next: Instant?,
)

fun habitReminderKey(habitId: String, stageId: String?, date: LocalDate): String = "$habitId|${stageId ?: "-"}|$date"

/**
 * The habit reminders to show at [now] and the next moment to wake up. A reminder comes at the start of a stage's time (or of the habit's own
 * time when it has no stages), today and tomorrow, for a habit that counts days and has "Remind me" on. Nothing comes for a stage that is
 * already done, for a day that is already finished, for a week whose goal is met by other days (the habit rests), or for what was [shown] already.
 * One that is more than [catchUp] late is dropped. [shown] holds the keys of [habitReminderKey].
 */
fun planHabitReminders(
    habits: List<HabitState>,
    now: Instant,
    zone: ZoneId,
    shown: Set<String>,
    catchUp: Duration = HABIT_CATCH_UP,
    tolerance: Duration = DUE_TOLERANCE,
): HabitReminderPlan {
    val today = now.atZone(zone).toLocalDate()
    val due = mutableListOf<PendingHabitReminder>()
    var next: Instant? = null

    for (state in habits) {
        val habit = state.habit
        if (!habit.remind || habit.kind != HabitKind.DAYS) continue
        for (date in listOf(today, today.plusDays(1))) {
            val weekStart = weekStartOf(date, FIRST_DAY_OF_WEEK)
            if (state.goals.forWeek(weekStart) == null) continue
            if (state.isDayDone(date, weekStart)) continue
            // the week's goal is met by other days: the habit rests
            if (state.week(weekStart).met) continue

            val stages = state.stagesFor(weekStart)
            val candidates: List<Pair<HabitStage?, Int>> = if (stages.isNotEmpty()) {
                val done = state.stagesDoneOn(date)
                stages.filter { it.id !in done && it.time.isSet }.mapNotNull { stage -> stage.time.startMinute?.let { stage to it } }
            } else {
                listOfNotNull(habit.time.startMinute?.takeIf { habit.time.isSet }?.let { null to it })
            }
            for ((stage, minute) in candidates) {
                val pending = PendingHabitReminder(
                    habit = habit,
                    stage = stage,
                    date = date,
                    trigger = LocalDateTime.of(date, java.time.LocalTime.of(minute / 60, minute % 60)).atZone(zone).toInstant(),
                )
                if (pending.key in shown) continue
                if (pending.trigger.isAfter(now.plus(tolerance))) {
                    if (next == null || pending.trigger.isBefore(next)) next = pending.trigger
                } else if (Duration.between(pending.trigger, now) <= catchUp) {
                    due += pending
                }
            }
        }
    }
    return HabitReminderPlan(due = due.sortedBy { it.trigger }, next = next)
}

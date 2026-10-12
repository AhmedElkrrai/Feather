package com.elkrrai.feather.domain.reminders

import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Task
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** How long after its trigger time a missed reminder is still shown (phone off, app killed). */
val CATCH_UP_WINDOW: Duration = Duration.ofHours(24)

/** An alarm that fires a moment early is still treated as due. */
val DUE_TOLERANCE: Duration = Duration.ofSeconds(2)

/** A reminder that is ready to be shown, with the instant it was meant to fire. */
data class PendingReminder(
    val task: Task,
    val reminder: Reminder,
    val trigger: Instant,
)

/**
 * What the scheduler should do right now: show [due] reminders immediately and arm one alarm
 * for [next] (the earliest upcoming trigger), if any.
 */
data class ReminderPlan(
    val due: List<PendingReminder>,
    val next: Instant?,
)

/**
 * The instant this reminder should fire for [task], or null if it cannot (a "before due"
 * reminder on a task without a due date). Local times are interpreted in [zone]; a task with
 * a date but no time uses [defaultTime] as its due moment.
 */
fun Reminder.triggerAt(task: Task, defaultTime: LocalTime, zone: ZoneId): Instant? = when (kind) {
    ReminderKind.BEFORE_DUE -> task.dueDate?.let { date ->
        LocalDateTime.of(date, task.dueTime ?: defaultTime)
            .minusMinutes(offsetMinutes.toLong())
            .atZone(zone)
            .toInstant()
    }
    ReminderKind.AT_TIME, ReminderKind.SNOOZE -> at?.atZone(zone)?.toInstant()
}

/**
 * Works out what to show now and when to wake up next. Completed tasks and reminders already
 * shown for their current trigger time are skipped. A trigger in the past is shown as a
 * catch-up only if it is within [catchUp] and the reminder was not created or changed after it
 * (so a reminder the user just edited into the past stays quiet).
 *
 * The caller passes only non-deleted tasks.
 */
fun planReminders(
    tasks: List<Task>,
    now: Instant,
    defaultTime: LocalTime,
    zone: ZoneId,
    catchUp: Duration = CATCH_UP_WINDOW,
    tolerance: Duration = DUE_TOLERANCE,
): ReminderPlan {
    val due = mutableListOf<PendingReminder>()
    var next: Instant? = null

    for (task in tasks) {
        if (task.isCompleted) continue
        for (reminder in task.reminders) {
            val trigger = reminder.triggerAt(task, defaultTime, zone) ?: continue
            if (reminder.firedForTrigger == trigger) continue

            if (trigger.isAfter(now.plus(tolerance))) {
                if (next == null || trigger.isBefore(next)) next = trigger
            } else {
                val age = Duration.between(trigger, now)
                val unchangedSinceTrigger = !reminder.updatedAt.isAfter(trigger)
                if (age <= catchUp && unchangedSinceTrigger) {
                    due += PendingReminder(task, reminder, trigger)
                }
            }
        }
    }
    return ReminderPlan(due = due.sortedBy { it.trigger }, next = next)
}

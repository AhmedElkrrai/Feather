package com.elkrrai.feather.domain.model

import java.time.Instant
import java.time.LocalDateTime

enum class ReminderKind(val code: Int) {
    /** [Reminder.offsetMinutes] before the task's due moment (0 means at the due moment). */
    BEFORE_DUE(0),

    /** At the local moment [Reminder.at]. */
    AT_TIME(1),

    /** A one-shot reminder created by snoozing. Never shown in the editor. */
    SNOOZE(2);

    companion object {
        fun fromCode(code: Int): ReminderKind = entries.firstOrNull { it.code == code } ?: AT_TIME
    }
}

/**
 * A reminder of a task. Times are floating local values: the trigger instant is worked out
 * when scheduling, in the user's current time zone.
 *
 * [firedForTrigger] is the trigger instant that was last shown (so a reminder fires once per
 * trigger time) and [updatedAt] is when the reminder or the task's due moment last changed.
 * Both are managed by the repository; the editor leaves them as they were loaded.
 */
data class Reminder(
    val id: String,
    val kind: ReminderKind,
    val offsetMinutes: Int = 0,
    val at: LocalDateTime? = null,
    val firedForTrigger: Instant? = null,
    val updatedAt: Instant = Instant.EPOCH,
) {
    /** True if [other] asks for the same notification time, ignoring ids and bookkeeping. */
    fun sameSchedule(other: Reminder): Boolean =
        kind == other.kind && offsetMinutes == other.offsetMinutes && at == other.at
}

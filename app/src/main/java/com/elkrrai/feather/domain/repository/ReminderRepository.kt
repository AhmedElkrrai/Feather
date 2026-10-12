package com.elkrrai.feather.domain.repository

import java.time.Instant
import java.time.LocalDateTime

/** The parts of reminders that happen outside the task editor: snoozing and bookkeeping. */
interface ReminderRepository {
    /**
     * Replaces any pending snooze of the task with a one-shot reminder at the local moment
     * [until].
     */
    suspend fun snooze(taskId: String, until: LocalDateTime)

    /** Records that the reminder was shown for [trigger], so it does not fire twice. */
    suspend fun markFired(reminderId: String, trigger: Instant)

    /** Removes a reminder (used for snoozes once they have fired). */
    suspend fun delete(reminderId: String)
}

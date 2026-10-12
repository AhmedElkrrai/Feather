package com.elkrrai.feather.reminders

/** Decides when the next reminder is due and shows the ones that are due now. */
interface ReminderScheduler {
    /**
     * Shows every reminder that is due (including recently missed ones), then arms one alarm for
     * the next upcoming reminder. Safe to call at any time and from several places at once; calls
     * are serialized. Reads the current tasks, so it is the single entry point for "something
     * changed".
     */
    suspend fun reschedule()

    /** Posts the morning overview (tasks due today and overdue), if there is anything to say. */
    suspend fun showDailySummary()

    /** Posts a test notification after [delayMillis], through the same alarm path as real reminders. */
    fun scheduleTest(delayMillis: Long)

    /** True if exact alarms can be scheduled (always true before Android 12). */
    fun canScheduleExactAlarms(): Boolean
}

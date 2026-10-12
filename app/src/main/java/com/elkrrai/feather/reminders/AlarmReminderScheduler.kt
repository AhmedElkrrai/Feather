package com.elkrrai.feather.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.reminders.PendingReminder
import com.elkrrai.feather.domain.reminders.nextDailySummary
import com.elkrrai.feather.domain.reminders.planReminders
import com.elkrrai.feather.domain.repository.ReminderRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.openCountIn
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps a single alarm armed for the earliest pending reminder. When it fires, [reschedule]
 * shows everything that is due and arms the next one, so the number of alarms stays at one no
 * matter how many reminders exist.
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val reminders: ReminderRepository,
    private val settings: SettingsRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) : ReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Serializes runs so two callers cannot show the same reminder twice. */
    private val mutex = Mutex()

    override suspend fun reschedule() = mutex.withLock {
        val now = Instant.now(clock)
        val appSettings = settings.current()
        val active = tasks.getActiveTasks()

        val plan = planReminders(active, now, appSettings.defaultReminderTime, ZoneId.systemDefault())
        showDue(plan.due, appSettings.snoozeMinutes)
        armAlarm(plan.next)
        armSummaryAlarm(appSettings, now)

        // Notifications of tasks that were finished or deleted in the meantime go away.
        notifier.cancelStale(active.filter { !it.isCompleted }.map { it.id }.toSet())
    }

    /** One notification per task, even if several of its reminders are due together. */
    private suspend fun showDue(due: List<PendingReminder>, snoozeMinutes: Int) {
        for ((task, group) in due.groupBy { it.task }) {
            notifier.show(task, snoozeMinutes)
            for (pending in group) {
                reminders.markFired(pending.reminder.id, pending.trigger)
                // A snooze is a one-shot; once shown it is no longer needed.
                if (pending.reminder.kind == ReminderKind.SNOOZE) reminders.delete(pending.reminder.id)
            }
        }
    }

    private fun armAlarm(next: Instant?) {
        val intent = alarmIntent(ReminderReceiver.ACTION_ALARM, REQUEST_ALARM)
        if (next == null) {
            alarmManager.cancel(intent)
        } else {
            setAlarm(next.toEpochMilli(), intent)
        }
    }

    override suspend fun showDailySummary() = mutex.withLock {
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        val active = tasks.getActiveTasks()
        val overdue = active.openCountIn(TaskScope.Overdue, today)
        // "Today" includes overdue tasks; the summary reports the two separately.
        val dueToday = active.openCountIn(TaskScope.Today, today) - overdue
        notifier.showSummary(dueToday = dueToday, overdue = overdue)
    }

    /** One alarm for the next morning summary, or none if the summary is switched off. */
    private fun armSummaryAlarm(settings: AppSettings, now: Instant) {
        val intent = alarmIntent(ReminderReceiver.ACTION_SUMMARY, REQUEST_SUMMARY)
        if (!settings.dailySummaryEnabled) {
            alarmManager.cancel(intent)
            return
        }
        val next = nextDailySummary(now, settings.dailySummaryTime, ZoneId.systemDefault())
        setAlarm(next.toEpochMilli(), intent)
    }

    override fun scheduleTest(delayMillis: Long) {
        val at = clock.millis() + delayMillis
        setAlarm(at, alarmIntent(ReminderReceiver.ACTION_TEST, REQUEST_TEST))
    }

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Exact and allowed during Doze when the user granted "Alarms & reminders"; otherwise an
     * inexact alarm that may arrive a little late (the reliability helper explains the difference).
     */
    private fun setAlarm(triggerAtMillis: Long, intent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
                return
            } catch (e: SecurityException) {
                // The permission was revoked between the check and the call: fall through.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
    }

    private fun alarmIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val REQUEST_ALARM = 1
        const val REQUEST_TEST = 2
        const val REQUEST_SUMMARY = 3
    }
}

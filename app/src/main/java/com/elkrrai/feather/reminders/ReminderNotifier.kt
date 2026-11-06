package com.elkrrai.feather.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.TimeMode
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.reminders.PendingHabitReminder
import com.elkrrai.feather.feature.snooze.SnoozeActivity
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts reminder notifications. One notification per task, identified by the task id
 * as its tag, so a second reminder of the same task replaces the first instead of stacking.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val manager = NotificationManagerCompat.from(appContext)

    /**
     * Shows the reminder for [task] with Complete, a one-tap snooze of [snoozeMinutes], and a
     * "More…" button for other snooze times. Silently does nothing if notifications are off.
     */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun show(task: Task, snoozeMinutes: Int) {
        if (!manager.areNotificationsEnabled()) return
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)

        val locale = context.resources.configuration.locales[0]
        val dueText = task.dueDate?.let { date ->
            val day = formatDueDate(context, date, LocalDate.now(), locale)
            task.dueTime?.let { "$day, ${formatDueTime(it, locale)}" } ?: day
        }
        val text = dueText?.let { context.getString(R.string.reminder_due, it) }
            ?: context.getString(R.string.reminder_generic)

        val builder = NotificationCompat.Builder(context, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setContentTitle(task.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTaskIntent(context, task.id))
            .addAction(
                0,
                context.getString(R.string.action_complete),
                actionIntent(context, task.id, ReminderActionReceiver.ACTION_COMPLETE, minutes = null),
            )
            .addAction(
                0,
                context.getString(R.string.action_snooze_minutes, snoozeMinutes),
                actionIntent(context, task.id, ReminderActionReceiver.ACTION_SNOOZE, minutes = snoozeMinutes),
            )
            .addAction(
                0,
                context.getString(R.string.action_snooze_more),
                snoozeMoreIntent(context, task.id),
            )
        if (task.notes.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${task.notes}"))
        }

        manager.notify(task.id, NOTIFICATION_ID, builder.build())
    }

    /**
     * The reminder of a habit: its name and the stage it is for as the title, the time as the text, a Done button that marks it (the stage,
     * or the day of a habit without stages), and a tap that opens the app on Today. One notification for each habit, so the next stage
     * replaces the last one.
     */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showHabit(pending: PendingHabitReminder) {
        if (!manager.areNotificationsEnabled()) return
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)
        val locale = context.resources.configuration.locales[0]
        val habit = pending.habit
        val title = pending.stage?.let { "${habit.name}: ${it.name}" } ?: habit.name
        val time = pending.stage?.time ?: habit.time
        val builder = NotificationCompat.Builder(context, ReminderChannels.HABITS)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(
                0,
                context.getString(R.string.habit_notification_done),
                habitDoneIntent(context, pending),
            )
        if (time.isSet) builder.setContentText(timeText(context, time, locale))
        manager.notify(habitTag(habit.id), NOTIFICATION_ID, builder.build())
    }

    /** "4:30 to 6:00 AM" or one moment, in the app language. */
    private fun timeText(context: Context, time: DayTime, locale: java.util.Locale): String {
        fun at(minute: Int) = formatDueTime(LocalTime.of(minute / 60, minute % 60), locale)
        val start = time.startMinute ?: return ""
        val end = time.endMinute
        return if (time.mode == TimeMode.WINDOW && end != null) context.getString(R.string.habit_time_range, at(start), at(end)) else at(start)
    }

    fun cancelHabit(habitId: String) {
        manager.cancel(habitTag(habitId), NOTIFICATION_ID)
    }

    /** Removes habit notifications whose habit is not waiting for anything any more (finished for today, deleted, or switched off). */
    @SuppressLint("MissingPermission")
    fun cancelStaleHabits(waitingHabitIds: Set<String>) {
        for (posted in manager.activeNotifications) {
            val tag = posted.tag ?: continue
            if (posted.id == NOTIFICATION_ID && tag.startsWith(HABIT_TAG_PREFIX) && tag.removePrefix(HABIT_TAG_PREFIX) !in waitingHabitIds) {
                manager.cancel(tag, NOTIFICATION_ID)
            }
        }
    }

    private fun habitTag(habitId: String) = HABIT_TAG_PREFIX + habitId

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun habitDoneIntent(context: Context, pending: PendingHabitReminder): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_HABIT_DONE)
            .putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, pending.habit.id)
            .putExtra(ReminderActionReceiver.EXTRA_DATE, pending.date.toString())
        pending.stage?.let { intent.putExtra(ReminderActionReceiver.EXTRA_STAGE_ID, it.id) }
        return PendingIntent.getBroadcast(
            context,
            "habit-done:${pending.habit.id}".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** A notification the reliability helper uses to prove that reminders can reach the user. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showTest() {
        if (!manager.areNotificationsEnabled()) return
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)
        val notification = NotificationCompat.Builder(context, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(TEST_TAG, NOTIFICATION_ID, notification)
    }

    /**
     * The morning overview. Says nothing when no task is due today or overdue, and does nothing
     * if notifications are turned off.
     */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showSummary(dueToday: Int, overdue: Int) {
        if (!manager.areNotificationsEnabled() || (dueToday == 0 && overdue == 0)) return
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)
        val resources = context.resources
        val parts = buildList {
            if (dueToday > 0) add(resources.getQuantityString(R.plurals.summary_due_today, dueToday, dueToday))
            if (overdue > 0) add(resources.getQuantityString(R.plurals.summary_overdue, overdue, overdue))
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, ReminderChannels.SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setContentTitle(context.getString(R.string.summary_title))
            .setContentText(parts.joinToString(" · "))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(SUMMARY_TAG, NOTIFICATION_ID, notification)
    }

    fun cancel(taskId: String) {
        manager.cancel(taskId, NOTIFICATION_ID)
    }

    /**
     * Removes reminder notifications whose task is no longer open (completed, deleted or gone),
     * for example after the task was finished inside the app.
     */
    @SuppressLint("MissingPermission")
    fun cancelStale(openTaskIds: Set<String>) {
        for (posted in manager.activeNotifications) {
            val tag = posted.tag ?: continue
            val isTaskReminder = tag != TEST_TAG && tag != SUMMARY_TAG && !tag.startsWith(HABIT_TAG_PREFIX)
            if (posted.id == NOTIFICATION_ID && isTaskReminder && tag !in openTaskIds) {
                manager.cancel(tag, NOTIFICATION_ID)
            }
        }
    }

    /** A broadcast to [ReminderActionReceiver]; the request code keeps each task's buttons apart. */
    private fun actionIntent(context: Context, taskId: String, action: String, minutes: Int?): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
        if (minutes != null) intent.putExtra(ReminderActionReceiver.EXTRA_MINUTES, minutes)
        return PendingIntent.getBroadcast(
            context,
            "$action:$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun snoozeMoreIntent(context: Context, taskId: String): PendingIntent {
        val intent = SnoozeActivity.intent(context, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            "snooze-more:$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openTaskIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        /** All reminder notifications share this id and differ by tag (the task id). */
        const val NOTIFICATION_ID = 1001
        private const val TEST_TAG = "test"
        private const val SUMMARY_TAG = "summary"

        /** Habit notifications are tagged with this and the habit id, so they are never taken for the notification of a task. */
        private const val HABIT_TAG_PREFIX = "habit:"
    }
}

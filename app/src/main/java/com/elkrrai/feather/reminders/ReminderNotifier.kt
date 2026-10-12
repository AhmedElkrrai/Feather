package com.elkrrai.feather.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.feature.snooze.SnoozeActivity
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
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
            .setSmallIcon(R.drawable.ic_notification)
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

    /** A notification the reliability helper uses to prove that reminders can reach the user. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showTest() {
        if (!manager.areNotificationsEnabled()) return
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)
        val notification = NotificationCompat.Builder(context, ReminderChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
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
            .setSmallIcon(R.drawable.ic_notification)
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
            val isTaskReminder = tag != TEST_TAG && tag != SUMMARY_TAG
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
    }
}

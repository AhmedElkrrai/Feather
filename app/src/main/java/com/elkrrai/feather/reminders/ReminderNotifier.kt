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

    /** Shows the reminder for [task]. Silently does nothing if notifications are turned off. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun show(task: Task) {
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
            if (posted.id == NOTIFICATION_ID && tag != TEST_TAG && tag !in openTaskIds) {
                manager.cancel(tag, NOTIFICATION_ID)
            }
        }
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
    }
}

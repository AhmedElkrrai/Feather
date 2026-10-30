package com.elkrrai.feather.focus

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.focus.FocusPhase
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.domain.focus.focusClock
import com.elkrrai.feather.reminders.ReminderChannels
import com.elkrrai.feather.reminders.withAppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The notifications of a focus session: a quiet one with a live countdown while it runs (it can be
 * stopped from there), and one that makes itself heard when the time is up. Both open the focus
 * screen of the task. Nothing is shown when notifications are off; the screen still works.
 */
@Singleton
class FocusNotifier @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val manager = NotificationManagerCompat.from(appContext)

    /** The countdown, counting down in the notification itself, so nothing has to keep running. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showRunning(session: FocusSession) {
        val endsAt = session.endsAt ?: return
        val context = prepare() ?: return
        val notification = base(context, session, ReminderChannels.FOCUS_RUNNING)
            .setContentTitle(context.getString(runningTitle(session.phase), session.taskTitle))
            .setContentText(context.getString(R.string.focus_notification_left))
            .setWhen(endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()
        manager.notify(TAG, ID_RUNNING, notification)
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showPaused(session: FocusSession) {
        val context = prepare() ?: return
        val left = focusClock(session.pausedLeft, context.resources.configuration.locales[0])
        val notification = base(context, session, ReminderChannels.FOCUS_RUNNING)
            .setContentTitle(context.getString(runningTitle(session.phase), session.taskTitle))
            .setContentText(context.getString(R.string.focus_notification_paused, left))
            .build()
        manager.notify(TAG, ID_RUNNING, notification)
    }

    /** The time is up: this one vibrates and sounds, and goes away when it is tapped. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showOver(session: FocusSession) {
        val context = prepare() ?: return
        val focus = session.phase == FocusPhase.FOCUS
        val notification = NotificationCompat.Builder(context, ReminderChannels.FOCUS_DONE)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setContentTitle(context.getString(if (focus) R.string.focus_notification_over else R.string.focus_notification_break_over))
            .setContentText(
                if (focus) context.getString(R.string.focus_notification_over_text, session.taskTitle) else session.taskTitle,
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openFocusIntent(context, session.taskId))
            .build()
        manager.notify(TAG, ID_OVER, notification)
    }

    fun cancelRunning() = manager.cancel(TAG, ID_RUNNING)

    fun cancelAll() {
        manager.cancel(TAG, ID_RUNNING)
        manager.cancel(TAG, ID_OVER)
    }

    private fun prepare(): Context? {
        if (!manager.areNotificationsEnabled()) return null
        val context = appContext.withAppLocale()
        ReminderChannels.ensure(context)
        return context
    }

    private fun base(context: Context, session: FocusSession, channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_feather)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openFocusIntent(context, session.taskId))
            .addAction(0, context.getString(R.string.focus_stop), stopIntent(context))

    private fun runningTitle(phase: FocusPhase): Int =
        if (phase == FocusPhase.FOCUS) R.string.focus_notification_running else R.string.focus_notification_break_running

    private fun openFocusIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_FOCUS_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun stopIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_STOP,
        Intent(context, FocusReceiver::class.java).setAction(FocusReceiver.ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "focus"
        const val ID_RUNNING = 2001
        const val ID_OVER = 2002
        const val REQUEST_OPEN = 20
        const val REQUEST_STOP = 21
    }
}

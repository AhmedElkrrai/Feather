package com.elkrrai.feather.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.elkrrai.feather.R

/** Notification channels. Safe to create repeatedly; that also refreshes their localized names. */
object ReminderChannels {
    const val REMINDERS = "reminders"
    const val SUMMARY = "daily_summary"
    const val FOCUS_RUNNING = "focus_running"
    const val FOCUS_DONE = "focus_done"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SUMMARY,
                context.getString(R.string.channel_summary_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_summary_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FOCUS_RUNNING,
                context.getString(R.string.channel_focus_running_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_focus_running_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FOCUS_DONE,
                context.getString(R.string.channel_focus_done_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_focus_done_description)
                enableVibration(true)
            }
        )
    }
}

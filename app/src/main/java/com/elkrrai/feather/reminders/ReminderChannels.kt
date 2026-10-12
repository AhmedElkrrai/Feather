package com.elkrrai.feather.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.elkrrai.feather.R

/** Notification channels. Safe to create repeatedly; that also refreshes their localized names. */
object ReminderChannels {
    const val REMINDERS = "reminders"
    const val SUMMARY = "daily_summary"

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
    }
}

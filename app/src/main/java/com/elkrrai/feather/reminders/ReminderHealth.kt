package com.elkrrai.feather.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/**
 * What currently stands between a reminder and the user, as far as the app can tell.
 * Phone makers add their own restrictions (auto-launch, background limits) that no API reveals;
 * the reliability helper tells the user about those separately.
 */
data class ReminderHealth(
    /** The app may post notifications at all (the runtime permission on Android 13+, or the system toggle). */
    val notificationsEnabled: Boolean = true,
    /** The "Reminders" notification channel has not been switched off. */
    val reminderChannelEnabled: Boolean = true,
    /** "Alarms & reminders" is allowed, so reminders fire at the exact minute. */
    val exactAlarmsAllowed: Boolean = true,
    /** The app is exempt from battery optimization, so the phone will not delay or drop alarms. */
    val batteryUnrestricted: Boolean = true,
) {
    /** True if a reminder will reach the user, even if it might arrive a little late. */
    val canNotify: Boolean get() = notificationsEnabled && reminderChannelEnabled

    /** True if reminders will arrive at the right minute. */
    val canDeliverOnTime: Boolean get() = canNotify && exactAlarmsAllowed
}

fun readReminderHealth(context: Context): ReminderHealth {
    val notifications = NotificationManagerCompat.from(context)
    val channel = notifications.getNotificationChannel(ReminderChannels.REMINDERS)
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)

    return ReminderHealth(
        notificationsEnabled = notifications.areNotificationsEnabled(),
        // A channel that does not exist yet counts as enabled: it is created on first use.
        reminderChannelEnabled = channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE,
        exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms(),
        batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName),
    )
}

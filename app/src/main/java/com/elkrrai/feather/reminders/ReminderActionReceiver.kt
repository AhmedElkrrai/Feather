package com.elkrrai.feather.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime

/** Handles the Complete and one-tap Snooze buttons of a reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java,
        )
        when (intent.action) {
            ACTION_COMPLETE -> runAsync {
                entry.taskRepository().setCompleted(taskId, true)
                entry.notifier().cancel(taskId)
                entry.scheduler().reschedule()
            }

            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, DEFAULT_SNOOZE_MINUTES).toLong()
                runAsync {
                    entry.reminderRepository().snooze(taskId, LocalDateTime.now().plusMinutes(minutes))
                    entry.notifier().cancel(taskId)
                    entry.scheduler().reschedule()
                }
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.elkrrai.feather.action.COMPLETE_TASK"
        const val ACTION_SNOOZE = "com.elkrrai.feather.action.SNOOZE_TASK"
        const val EXTRA_TASK_ID = "com.elkrrai.feather.extra.TASK_ID"
        const val EXTRA_MINUTES = "com.elkrrai.feather.extra.MINUTES"
        private const val DEFAULT_SNOOZE_MINUTES = 10
    }
}

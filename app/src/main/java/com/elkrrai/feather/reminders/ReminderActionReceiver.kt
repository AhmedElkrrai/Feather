package com.elkrrai.feather.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elkrrai.feather.domain.habit.forWeek
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/** Handles the Complete and one-tap Snooze buttons of a reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java,
        )
        if (intent.action == ACTION_HABIT_DONE) {
            handleHabitDone(entry, intent)
            return
        }
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
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

    /** The Done button of a habit reminder: the stage it is for is done, or the day of a habit without stages. */
    private fun handleHabitDone(entry: ReminderEntryPoint, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val stageId = intent.getStringExtra(EXTRA_STAGE_ID)
        runAsync {
            val habits = entry.habitRepository()
            if (stageId != null) {
                habits.setStageDone(habitId, date, stageId, true)
            } else {
                val state = habits.observeHabits().first().firstOrNull { it.habit.id == habitId }
                val needed = state?.goals?.forWeek(weekStartOf(date, FIRST_DAY_OF_WEEK))?.dailyTarget ?: 1
                habits.setAmount(habitId, date, needed)
            }
            entry.notifier().cancelHabit(habitId)
            entry.scheduler().reschedule()
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.elkrrai.feather.action.COMPLETE_TASK"
        const val ACTION_SNOOZE = "com.elkrrai.feather.action.SNOOZE_TASK"
        const val ACTION_HABIT_DONE = "com.elkrrai.feather.action.HABIT_DONE"
        const val EXTRA_TASK_ID = "com.elkrrai.feather.extra.TASK_ID"
        const val EXTRA_HABIT_ID = "com.elkrrai.feather.extra.HABIT_ID"
        const val EXTRA_STAGE_ID = "com.elkrrai.feather.extra.STAGE_ID"
        const val EXTRA_DATE = "com.elkrrai.feather.extra.DATE"
        const val EXTRA_MINUTES = "com.elkrrai.feather.extra.MINUTES"
        private const val DEFAULT_SNOOZE_MINUTES = 10
    }
}

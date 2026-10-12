package com.elkrrai.feather.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs [block] off the main thread while keeping the receiver alive until it finishes
 * (`goAsync`), which is how a receiver may do database work.
 */
internal fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/**
 * Entry point for everything that should make reminders re-evaluate: our own alarm, a reboot, an
 * app update (which clears alarms), a clock or time zone change, and the exact-alarm permission
 * being granted or revoked. A test alarm from the reliability helper also lands here.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java,
        )
        when (intent.action) {
            ACTION_TEST -> entry.notifier().showTest()
            ACTION_SUMMARY -> runAsync {
                entry.scheduler().showDailySummary()
                // Arms tomorrow's summary (and re-checks reminders while we are awake anyway).
                entry.scheduler().reschedule()
            }
            else -> runAsync { entry.scheduler().reschedule() }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.elkrrai.feather.action.REMINDER_ALARM"
        const val ACTION_TEST = "com.elkrrai.feather.action.REMINDER_TEST"
        const val ACTION_SUMMARY = "com.elkrrai.feather.action.DAILY_SUMMARY"
    }
}

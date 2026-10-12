package com.elkrrai.feather.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

/**
 * A safety net, not the primary mechanism: twice a day it re-arms the reminder alarm, in case a
 * phone's battery manager cleared it without telling the app.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        entry.scheduler().reschedule()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "reminder-resync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

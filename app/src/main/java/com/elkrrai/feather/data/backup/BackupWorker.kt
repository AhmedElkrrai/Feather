package com.elkrrai.feather.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupEntryPoint {
    fun autoBackup(): AutoBackup
}

/** Runs the daily automatic backup. WorkManager keeps it going across restarts and reboots. */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, BackupEntryPoint::class.java)
        return when (entry.autoBackup().runNow()) {
            AutoBackupResult.DONE -> Result.success()
            // Nothing a retry can fix: the next day's run tries again.
            AutoBackupResult.NO_FOLDER, AutoBackupResult.NO_ACCESS -> Result.success()
            AutoBackupResult.FAILED -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "auto-backup"
        private const val MAX_ATTEMPTS = 3

        /** Starts the daily job when [enabled], stops it otherwise. Safe to call repeatedly. */
        fun schedule(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork(UNIQUE_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            manager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

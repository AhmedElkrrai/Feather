package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.HabitDao
import com.elkrrai.feather.data.db.HabitEntryEntity
import com.elkrrai.feather.data.db.HabitLogEntity
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Keeps the logs of habits that count an amount and the day totals in step. An amount from before logs existed, or from a backup made before
 * them, becomes one log without a time; and where a day has logs its amount is their sum.
 */
class HabitLogs @Inject constructor(
    private val dao: HabitDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    suspend fun reconcile() {
        tx.run {
            for (entry in dao.amountsWithoutLogs()) {
                dao.upsertLog(HabitLogEntity(UUID.randomUUID().toString(), entry.habitId, entry.date, entry.amount, null, entry.updatedAt))
            }
            for (day in dao.logTotals()) {
                val entry = dao.getEntry(day.habitId, day.date)
                val total = day.total.coerceIn(0, MAX_HABIT_TARGET)
                if (entry == null || entry.amount != total) {
                    dao.upsertEntry(HabitEntryEntity(day.habitId, day.date, total, clock.millis()))
                }
            }
        }
    }

    /** The day of one habit as one log, if it has an amount from before logs and none yet. */
    suspend fun ensureLogs(habitId: String, day: String) {
        if (dao.logsOn(habitId, day).isNotEmpty()) return
        val entry = dao.getEntry(habitId, day)?.takeIf { it.amount > 0 } ?: return
        dao.upsertLog(HabitLogEntity(UUID.randomUUID().toString(), habitId, day, entry.amount, null, entry.updatedAt))
    }
}

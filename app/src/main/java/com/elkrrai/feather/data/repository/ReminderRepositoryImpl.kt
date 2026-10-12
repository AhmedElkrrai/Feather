package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.ReminderDao
import com.elkrrai.feather.data.db.ReminderEntity
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : ReminderRepository {

    override suspend fun snooze(taskId: String, until: LocalDateTime) {
        val now = clock.millis()
        val moment = until.truncatedTo(ChronoUnit.SECONDS)
        tx.run {
            reminderDao.softDeleteSnoozes(taskId, deletedAt = now)
            reminderDao.upsert(
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    kind = ReminderKind.SNOOZE.code,
                    offsetMinutes = 0,
                    atDate = moment.toLocalDate().toString(),
                    atTime = moment.toLocalTime().toString(),
                    firedForTrigger = null,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                )
            )
        }
    }

    override suspend fun markFired(reminderId: String, trigger: Instant) {
        reminderDao.markFired(reminderId, trigger.toEpochMilli())
    }

    override suspend fun delete(reminderId: String) {
        reminderDao.softDelete(reminderId, deletedAt = clock.millis())
    }
}

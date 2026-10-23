package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.backup.BackupCodec
import com.elkrrai.feather.data.backup.BackupFile
import com.elkrrai.feather.data.backup.BackupMerger
import com.elkrrai.feather.data.backup.MergePlan
import com.elkrrai.feather.data.backup.toEntity
import com.elkrrai.feather.data.backup.toRecord
import com.elkrrai.feather.data.db.BackupDao
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.backup.ImportMode
import com.elkrrai.feather.domain.backup.ImportSummary
import com.elkrrai.feather.domain.backup.TaskCsv
import com.elkrrai.feather.domain.repository.InboxRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.Clock
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val dao: BackupDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
    private val taskRepository: TaskRepository,
    private val inboxRepository: InboxRepository,
) : BackupRepository {

    override suspend fun exportJson(): String = BackupCodec.encode(tx.run { readAll() })

    override suspend fun exportCsv(): String = TaskCsv.build(taskRepository.getActiveTasks())

    override suspend fun importJson(text: String, mode: ImportMode): ImportSummary {
        // Decoding comes first: a file that cannot be used must not change anything.
        val incoming = BackupCodec.decode(text)
        // Every imported task goes into the Inbox, which has to be there first.
        inboxRepository.ensureInbox()
        tx.run {
            val local = if (mode == ImportMode.REPLACE) {
                wipe()
                BackupFile()
            } else {
                readAll()
            }
            apply(BackupMerger.plan(local, incoming))
        }
        return ImportSummary(
            tasks = incoming.tasks.count { it.parentTaskId == null && it.deletedAt == null },
            tags = incoming.tags.count { it.deletedAt == null },
        )
    }

    private suspend fun readAll() = BackupFile(
        exportedAt = clock.millis(),
        tags = dao.tags().map { it.toRecord() },
        tasks = dao.tasks().map { it.toRecord() },
        taskTags = dao.tagLinks().map { it.toRecord() },
        reminders = dao.reminders().map { it.toRecord() },
        completions = dao.completions().map { it.toRecord() },
    )

    private suspend fun wipe() {
        dao.deleteAllTasks() // tag links, reminders and history go with them
        dao.deleteAllTags()
    }

    /** Parents before children, so every foreign key points at a row that already exists. */
    private suspend fun apply(plan: MergePlan) {
        dao.upsertTags(plan.tags.map { it.toEntity() })
        dao.upsertTasks(plan.tasks.map { it.toEntity() })
        // Tasks taken from the file bring their own tags, replacing the phone's.
        plan.rewrittenTaskIds.toList().chunked(SQL_VARIABLE_CHUNK).forEach { dao.clearTagLinks(it) }
        dao.insertTagLinks(plan.tagLinks.map { it.toEntity() })
        dao.upsertReminders(plan.reminders.map { it.toEntity() })
        dao.insertCompletions(plan.completions.map { it.toEntity() })
    }

    private companion object {
        /** Stays under SQLite's limit on the number of `?` in one statement. */
        const val SQL_VARIABLE_CHUNK = 500
    }
}

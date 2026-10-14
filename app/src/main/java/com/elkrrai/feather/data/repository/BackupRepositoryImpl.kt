package com.elkrrai.feather.data.repository

import android.content.Context
import com.elkrrai.feather.R
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
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.reminders.withAppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class BackupRepositoryImpl @Inject constructor(
    private val dao: BackupDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    @ApplicationContext private val context: Context,
) : BackupRepository {

    override suspend fun exportJson(): String = BackupCodec.encode(tx.run { readAll() })

    override suspend fun exportCsv(): String {
        val inboxName = context.withAppLocale().getString(R.string.inbox)
        val names = listRepository.observeLists().first()
            .associate { it.id to if (it.isInbox) inboxName else it.name }
        return TaskCsv.build(taskRepository.getActiveTasks(), names)
    }

    override suspend fun importJson(text: String, mode: ImportMode): ImportSummary {
        // Decoding comes first: a file that cannot be used must not change anything.
        val incoming = BackupCodec.decode(text)
        tx.run {
            val local = if (mode == ImportMode.REPLACE) {
                wipe()
                BackupFile()
            } else {
                readAll()
            }
            apply(BackupMerger.plan(local, incoming, TaskList.INBOX_ID))
        }
        // A replaced database may have no Inbox if the file did not carry one.
        listRepository.ensureInbox()
        return ImportSummary(
            tasks = incoming.tasks.count { it.parentTaskId == null && it.deletedAt == null },
            lists = incoming.lists.count { !it.isInbox && it.deletedAt == null },
            tags = incoming.tags.count { it.deletedAt == null },
        )
    }

    private suspend fun readAll() = BackupFile(
        exportedAt = clock.millis(),
        folders = dao.folders().map { it.toRecord() },
        lists = dao.lists().map { it.toRecord() },
        tags = dao.tags().map { it.toRecord() },
        tasks = dao.tasks().map { it.toRecord() },
        taskTags = dao.tagLinks().map { it.toRecord() },
        reminders = dao.reminders().map { it.toRecord() },
        completions = dao.completions().map { it.toRecord() },
    )

    private suspend fun wipe() {
        dao.deleteAllTasks() // tag links, reminders and history go with them
        dao.deleteAllLists()
        dao.deleteAllFolders()
        dao.deleteAllTags()
    }

    /** Parents before children, so every foreign key points at a row that already exists. */
    private suspend fun apply(plan: MergePlan) {
        dao.upsertFolders(plan.folders.map { it.toEntity() })
        dao.upsertLists(plan.lists.map { it.toEntity() })
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

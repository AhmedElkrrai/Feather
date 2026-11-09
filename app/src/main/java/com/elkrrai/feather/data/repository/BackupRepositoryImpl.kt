package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.backup.BackupCodec
import com.elkrrai.feather.data.backup.BackupFile
import com.elkrrai.feather.data.backup.BackupMerger
import com.elkrrai.feather.data.backup.MergePlan
import com.elkrrai.feather.data.backup.toEntity
import com.elkrrai.feather.data.backup.toRecord
import com.elkrrai.feather.data.db.BackupDao
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.backup.BackupPreferences
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.backup.ImportMode
import com.elkrrai.feather.domain.backup.ImportSummary
import com.elkrrai.feather.domain.backup.TaskCsv
import com.elkrrai.feather.domain.repository.InboxRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import androidx.appcompat.app.AppCompatDelegate
import java.time.Clock
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val dao: BackupDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
    private val taskRepository: TaskRepository,
    private val inboxRepository: InboxRepository,
    private val preferences: BackupPreferences,
    private val habitLogs: HabitLogs,
) : BackupRepository {

    override suspend fun exportJson(): String = BackupCodec.encode(tx.run { readAll() })

    override suspend fun exportCsv(): String = TaskCsv.build(taskRepository.getActiveTasks())

    override suspend fun importJson(text: String, mode: ImportMode, restoreSettings: Boolean): ImportSummary {
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
        // a file from before logs has only the day's amount; one with logs from two phones has to add up
        habitLogs.reconcile()
        val restored = restoreSettings && incoming.settings.isNotEmpty()
        if (restored) preferences.restoreBackedUpPreferences(incoming.settings)
        return ImportSummary(
            tasks = incoming.tasks.count { it.parentTaskId == null && it.deletedAt == null },
            tags = incoming.tags.count { it.deletedAt == null },
            settingsRestored = restored,
            language = incoming.language.takeIf { restored },
        )
    }

    private suspend fun readAll() = BackupFile(
        exportedAt = clock.millis(),
        tags = dao.tags().map { it.toRecord() },
        tasks = dao.tasks().map { it.toRecord() },
        taskTags = dao.tagLinks().map { it.toRecord() },
        reminders = dao.reminders().map { it.toRecord() },
        completions = dao.completions().map { it.toRecord() },
        habits = dao.habits().map { it.toRecord() },
        habitGoals = dao.habitGoals().map { it.toRecord() },
        habitEntries = dao.habitEntries().map { it.toRecord() },
        habitStages = dao.habitStages().map { it.toRecord() },
        habitGoalStages = dao.habitGoalStages().map { it.toRecord() },
        habitStageLog = dao.habitStageLog().map { it.toRecord() },
        habitLogs = dao.habitLogs().map { it.toRecord() },
        settings = preferences.backedUpPreferences(),
        language = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
    )

    private suspend fun wipe() {
        dao.deleteAllTasks() // tag links, reminders and history go with them
        dao.deleteAllTags()
        dao.deleteAllHabits() // goals, stages and logged days go with them
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
        dao.upsertHabits(plan.habits.map { it.toEntity() })
        dao.upsertHabitStages(plan.habitStages.map { it.toEntity() })
        dao.upsertHabitGoals(plan.habitGoals.map { it.toEntity() })
        // the stage list of a goal week is replaced whole, so the phone's rows of that week go first
        plan.rewrittenGoalStageWeeks.forEach { (habitId, from) -> dao.deleteGoalStages(habitId, from) }
        dao.upsertHabitGoalStages(plan.habitGoalStages.map { it.toEntity() })
        dao.upsertHabitEntries(plan.habitEntries.map { it.toEntity() })
        dao.upsertHabitStageLog(plan.habitStageLog.map { it.toEntity() })
        dao.upsertHabitLogs(plan.habitLogs.map { it.toEntity() })
    }

    private companion object {
        /** Stays under SQLite's limit on the number of `?` in one statement. */
        const val SQL_VARIABLE_CHUNK = 500
    }
}

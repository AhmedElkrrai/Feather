package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.HabitDao
import com.elkrrai.feather.data.db.HabitEntity
import com.elkrrai.feather.data.db.HabitEntryEntity
import com.elkrrai.feather.data.db.HabitGoalEntity
import com.elkrrai.feather.data.db.HabitGoalStageEntity
import com.elkrrai.feather.data.db.HabitStageEntity
import com.elkrrai.feather.data.db.HabitStageLogEntity
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitDraft
import com.elkrrai.feather.domain.habit.HabitGoal
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitStage
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.MAX_DAILY_TARGET
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import com.elkrrai.feather.domain.habit.MAX_STAGES
import com.elkrrai.feather.domain.habit.StageMode
import com.elkrrai.feather.domain.habit.TimeMode
import com.elkrrai.feather.domain.habit.forWeek
import com.elkrrai.feather.domain.habit.queuedAfter
import com.elkrrai.feather.domain.repository.HabitRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : HabitRepository {

    override fun observeHabits(): Flow<List<HabitState>> {
        val base = combine(dao.observeHabits(), dao.observeGoals(), dao.observeEntries()) { habits, goals, entries ->
            Triple(habits, goals, entries)
        }
        val staging = combine(dao.observeStages(), dao.observeGoalStages(), dao.observeStageLog()) { stages, goalStages, log ->
            Triple(stages, goalStages, log)
        }
        return combine(base, staging) { (habits, goals, entries), (stages, goalStages, log) ->
            val goalsByHabit = goals.groupBy { it.habitId }
            val entriesByHabit = entries.groupBy { it.habitId }
            val stagesByHabit = stages.groupBy { it.habitId }
            val stageIdsByGoal = goalStages.groupBy { it.habitId to it.effectiveFrom }
            val logByHabit = log.groupBy { it.habitId }
            habits.map { habit ->
                HabitState(
                    habit = habit.toDomain(),
                    goals = goalsByHabit[habit.id].orEmpty().map { goal ->
                        val ids = stageIdsByGoal[habit.id to goal.effectiveFrom].orEmpty().sortedBy { it.position }.map { it.stageId }
                        goal.toDomain(ids)
                    },
                    entries = entriesByHabit[habit.id].orEmpty().associate { LocalDate.parse(it.date) to it.amount },
                    stages = stagesByHabit[habit.id].orEmpty().associate { it.id to it.toDomain() },
                    stageLog = logByHabit[habit.id].orEmpty()
                        .groupBy({ LocalDate.parse(it.date) }, { it.stageId })
                        .mapValues { it.value.toSet() },
                )
            }
        }
    }

    override suspend fun saveHabit(id: String?, draft: HabitDraft, thisWeekStart: LocalDate): Habit {
        require(draft.name.isNotBlank()) { "Habit name must not be blank" }
        val now = clock.millis()
        val kind = draft.kind
        val existing = id?.let { dao.getHabit(it) }
        require(id == null || existing != null) { "There is no habit $id" }
        val habitId = existing?.id ?: UUID.randomUUID().toString()

        // A stage that comes with an id this habit knows keeps it; any other gets a new one.
        val knownStages = if (existing == null) emptySet() else dao.stagesOf(habitId).map { it.id }.toSet()
        val stages = if (kind == HabitKind.DAYS) draft.stages.filter { it.name.isNotBlank() }.take(MAX_STAGES) else emptyList()
        val stageIds = stages.map { stage -> stage.id?.takeIf { it in knownStages } ?: UUID.randomUUID().toString() }
        val staged = stageIds.isNotEmpty()

        // With stages each has its own time; the habit's own time is for a habit without them.
        val time = if (staged) DayTime() else draft.time.normalized()
        val stageMode = if (staged) draft.stageMode.takeIf { it != StageMode.NONE } ?: StageMode.ORDERED else StageMode.NONE
        val entity = HabitEntity(
            id = habitId,
            name = draft.name.trim(),
            kind = kind.code,
            unit = if (kind == HabitKind.AMOUNT) draft.unit.trim() else "",
            sortOrder = existing?.sortOrder ?: now,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deletedAt = existing?.deletedAt,
            showInToday = draft.showInToday,
            timeMode = time.mode.code,
            startMinute = time.startMinute,
            endMinute = time.endMinute,
            stageMode = stageMode.code,
            todayFromStart = draft.todayFromStart,
            remind = draft.remind,
        )
        val stageEntities = stages.mapIndexed { index, stage ->
            val stageTime = stage.time.normalized()
            HabitStageEntity(
                id = stageIds[index],
                habitId = habitId,
                name = stage.name.trim(),
                timeMode = stageTime.mode.code,
                startMinute = stageTime.startMinute,
                endMinute = stageTime.endMinute,
                updatedAt = now,
            )
        }
        val target = if (kind == HabitKind.DAYS) draft.target.coerceIn(1, DAYS_IN_WEEK) else draft.target.coerceIn(1, MAX_HABIT_TARGET)
        val dailyTarget = when {
            kind != HabitKind.DAYS -> 1
            staged -> draft.dailyTarget.coerceIn(1, stageIds.size)
            else -> draft.dailyTarget.coerceIn(1, MAX_DAILY_TARGET)
        }

        tx.run {
            dao.upsertHabit(entity)
            if (stageEntities.isNotEmpty()) dao.upsertStages(stageEntities)
            if (existing == null) {
                writeGoal(habitId, thisWeekStart, target, dailyTarget, stageIds, now)
            } else {
                // The goal in force (or the one already waiting) is compared with what the form says; only a change is queued.
                val goals = dao.goalsOf(habitId).map { it.toDomain(emptyList()) }
                val shown = goals.queuedAfter(thisWeekStart) ?: goals.forWeek(thisWeekStart)
                val shownStageIds = shown?.let { dao.goalStageIds(habitId, it.effectiveFrom.toString()) }.orEmpty()
                val changed = shown == null || shown.target != target || shown.dailyTarget != dailyTarget || shownStageIds != stageIds
                if (changed) writeGoal(habitId, thisWeekStart.plusWeeks(1), target, dailyTarget, stageIds, now)
            }
        }
        return entity.toDomain()
    }

    /** A goal and its stage list for the week that starts on [from]; a goal already written for that week is replaced. */
    private suspend fun writeGoal(habitId: String, from: LocalDate, target: Int, dailyTarget: Int, stageIds: List<String>, now: Long) {
        dao.upsertGoal(HabitGoalEntity(habitId, from.toString(), target, dailyTarget, now))
        dao.deleteGoalStages(habitId, from.toString())
        if (stageIds.isNotEmpty()) {
            dao.upsertGoalStages(stageIds.mapIndexed { position, stageId -> HabitGoalStageEntity(habitId, from.toString(), stageId, position, now) })
        }
    }

    override suspend fun setAmount(id: String, date: LocalDate, amount: Int) {
        if (dao.getHabit(id) == null) return
        dao.upsertEntry(HabitEntryEntity(id, date.toString(), amount.coerceIn(0, MAX_HABIT_TARGET), clock.millis()))
    }

    override suspend fun setStageDone(habitId: String, date: LocalDate, stageId: String, done: Boolean) {
        if (dao.getHabit(habitId) == null || dao.getStage(stageId)?.habitId != habitId) return
        val day = date.toString()
        val now = clock.millis()
        tx.run {
            dao.upsertStageLog(HabitStageLogEntity(habitId, day, stageId, done, now))
            dao.upsertEntry(HabitEntryEntity(habitId, day, dao.doneStageCount(habitId, day).coerceIn(0, MAX_HABIT_TARGET), now))
        }
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id, clock.millis())
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    kind = HabitKind.fromCode(kind),
    unit = unit,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    showInToday = showInToday,
    time = DayTime(TimeMode.fromCode(timeMode), startMinute, endMinute),
    stageMode = StageMode.fromCode(stageMode),
    todayFromStart = todayFromStart,
    remind = remind,
)

private fun HabitGoalEntity.toDomain(stageIds: List<String>) = HabitGoal(LocalDate.parse(effectiveFrom), target, dailyTarget, stageIds)

private fun HabitStageEntity.toDomain() = HabitStage(id, name, DayTime(TimeMode.fromCode(timeMode), startMinute, endMinute))

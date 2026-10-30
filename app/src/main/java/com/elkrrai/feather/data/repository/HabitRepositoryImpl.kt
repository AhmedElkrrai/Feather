package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.HabitDao
import com.elkrrai.feather.data.db.HabitEntity
import com.elkrrai.feather.data.db.HabitEntryEntity
import com.elkrrai.feather.data.db.HabitGoalEntity
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitGoal
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.MAX_DAILY_TARGET
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
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

    override fun observeHabits(): Flow<List<HabitState>> =
        combine(dao.observeHabits(), dao.observeGoals(), dao.observeEntries()) { habits, goals, entries ->
            val goalsByHabit = goals.groupBy { it.habitId }
            val entriesByHabit = entries.groupBy { it.habitId }
            habits.map { habit ->
                HabitState(
                    habit = habit.toDomain(),
                    goals = goalsByHabit[habit.id].orEmpty().map { it.toDomain() },
                    entries = entriesByHabit[habit.id].orEmpty().associate { LocalDate.parse(it.date) to it.amount },
                )
            }
        }

    override suspend fun addHabit(
        name: String,
        kind: HabitKind,
        unit: String,
        target: Int,
        dailyTarget: Int,
        thisWeekStart: LocalDate,
    ): Habit {
        require(name.isNotBlank()) { "Habit name must not be blank" }
        val now = clock.millis()
        val entity = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            kind = kind.code,
            unit = if (kind == HabitKind.AMOUNT) unit.trim() else "",
            sortOrder = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        tx.run {
            dao.upsertHabit(entity)
            dao.upsertGoal(goalEntity(entity.id, thisWeekStart, kind, target, dailyTarget, now))
        }
        return entity.toDomain()
    }

    override suspend fun rename(id: String, name: String, unit: String) {
        val habit = dao.getHabit(id) ?: return
        if (name.isBlank()) return
        val isAmount = HabitKind.fromCode(habit.kind) == HabitKind.AMOUNT
        dao.upsertHabit(
            habit.copy(name = name.trim(), unit = if (isAmount) unit.trim() else "", updatedAt = clock.millis()),
        )
    }

    override suspend fun queueGoal(id: String, target: Int, dailyTarget: Int, nextWeekStart: LocalDate) {
        val habit = dao.getHabit(id) ?: return
        val now = clock.millis()
        tx.run {
            dao.upsertGoal(goalEntity(id, nextWeekStart, HabitKind.fromCode(habit.kind), target, dailyTarget, now))
            dao.upsertHabit(habit.copy(updatedAt = now))
        }
    }

    override suspend fun setAmount(id: String, date: LocalDate, amount: Int) {
        if (dao.getHabit(id) == null) return
        dao.upsertEntry(HabitEntryEntity(id, date.toString(), amount.coerceIn(0, MAX_HABIT_TARGET), clock.millis()))
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id, clock.millis())
    }

    private fun goalEntity(
        habitId: String,
        from: LocalDate,
        kind: HabitKind,
        target: Int,
        dailyTarget: Int,
        now: Long,
    ) = HabitGoalEntity(
        habitId = habitId,
        effectiveFrom = from.toString(),
        target = if (kind == HabitKind.DAYS) target.coerceIn(1, DAYS_IN_WEEK) else target.coerceIn(1, MAX_HABIT_TARGET),
        dailyTarget = if (kind == HabitKind.DAYS) dailyTarget.coerceIn(1, MAX_DAILY_TARGET) else 1,
        updatedAt = now,
    )
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    kind = HabitKind.fromCode(kind),
    unit = unit,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

private fun HabitGoalEntity.toDomain() = HabitGoal(LocalDate.parse(effectiveFrom), target, dailyTarget)

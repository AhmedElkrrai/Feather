package com.elkrrai.feather.feature.habits

import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.TREE_STAGES
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val TreeHabitName = Regex("(Tree|Forest) \\d+")

/**
 * Habits to see every picture of the tree, each of one day a week with that many past weeks logged: "Tree 0"
 * has no met week (the seed), "Tree 1" one (stage 1), up to "Tree 13" with 13 (a full tree). "Forest 14", "27",
 * "40", "66" and "80" have a tree or more finished behind the one that is growing (80 has more than fit).
 *
 * Only a debug build offers it (a button at the end of Settings). It uses the repository like the screens
 * do, so what it makes is ordinary data: delete the habits from their menu to remove it. It runs to the end
 * even if the person leaves Settings, and it first removes any "Tree n" habits made earlier, so a second tap
 * never doubles them and a cut short one never leaves half a set.
 */
suspend fun HabitRepository.addTreeStageHabits(today: LocalDate, firstDay: DayOfWeek) = withContext(NonCancellable) {
    observeHabits().first().filter { TreeHabitName.matches(it.habit.name) }.forEach { delete(it.habit.id) }
    val thisWeek = weekStartOf(today, firstDay)
    suspend fun make(name: String, metWeeks: Int) {
        val habit = addHabit(name, HabitKind.DAYS, "", 1, 1, thisWeek.minusWeeks(maxOf(metWeeks, 1).toLong()))
        for (week in 1..metWeeks) setAmount(habit.id, thisWeek.minusWeeks(week.toLong()), 1)
    }
    for (stage in 0..TREE_STAGES) make("Tree $stage", stage)
    for (weeks in listOf(14, 27, 40, 66, 80)) make("Forest $weeks", weeks)
}

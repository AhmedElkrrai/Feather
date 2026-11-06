package com.elkrrai.feather.feature.habits

import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.HabitDraft
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.StageDraft
import com.elkrrai.feather.domain.habit.StageMode
import com.elkrrai.feather.domain.habit.TREE_STAGES
import com.elkrrai.feather.domain.habit.TimeMode
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val TreeHabitName = Regex("(Tree|Forest) \\d+|Today: .+")

/** One run at a time: a double tap waits for the first run, then replaces what it made instead of doubling it. */
private val SampleLock = Mutex()

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
suspend fun HabitRepository.addTreeStageHabits(today: LocalDate, firstDay: DayOfWeek) = withContext(NonCancellable) { SampleLock.withLock {
    observeHabits().first().filter { TreeHabitName.matches(it.habit.name) }.forEach { delete(it.habit.id) }
    val thisWeek = weekStartOf(today, firstDay)
    suspend fun make(name: String, metWeeks: Int) {
        val habit = saveHabit(null, HabitDraft(name, HabitKind.DAYS, target = 1), thisWeek.minusWeeks(maxOf(metWeeks, 1).toLong()))
        for (week in 1..metWeeks) setAmount(habit.id, thisWeek.minusWeeks(week.toLong()), 1)
    }
    for (stage in 0..TREE_STAGES) make("Tree $stage", stage)
    for (weeks in listOf(14, 27, 40, 66, 80)) make("Forest $weeks", weeks)

    // habits that show in Today: with named times (one at a time, and as a checklist), with a time window, a counted one, an amount
    fun window(from: Int, to: Int) = DayTime(TimeMode.WINDOW, from * 60, to * 60)
    fun at(hour: Int, minute: Int = 0) = DayTime(TimeMode.AT, hour * 60 + minute)
    saveHabit(
        null,
        HabitDraft(
            name = "Today: Pray", kind = HabitKind.DAYS, target = 7, dailyTarget = 5, showInToday = true, stageMode = StageMode.ORDERED,
            stages = listOf(
                StageDraft(name = "Fajr", time = DayTime(TimeMode.WINDOW, 270, 360)),
                StageDraft(name = "Dhuhr", time = window(12, 14)),
                StageDraft(name = "Asr", time = DayTime(TimeMode.WINDOW, 915, 990)),
                StageDraft(name = "Maghrib", time = at(18, 10)),
                StageDraft(name = "Isha", time = DayTime(TimeMode.WINDOW, 20 * 60, 23 * 60 + 30)),
            ),
        ),
        thisWeek,
    )
    saveHabit(
        null,
        HabitDraft(
            name = "Today: Meds", kind = HabitKind.DAYS, target = 7, dailyTarget = 3, showInToday = true, stageMode = StageMode.ALL,
            stages = listOf(StageDraft(name = "Morning", time = at(8)), StageDraft(name = "Noon", time = at(13)), StageDraft(name = "Evening", time = at(20))),
        ),
        thisWeek,
    )
    saveHabit(null, HabitDraft("Today: Workout", HabitKind.DAYS, target = 3, showInToday = true, time = window(17, 19)), thisWeek)
    saveHabit(null, HabitDraft("Today: Water", HabitKind.DAYS, target = 7, dailyTarget = 3, showInToday = true), thisWeek)
    saveHabit(null, HabitDraft("Today: Read", HabitKind.AMOUNT, unit = "pages", target = 20, showInToday = true), thisWeek)
} }

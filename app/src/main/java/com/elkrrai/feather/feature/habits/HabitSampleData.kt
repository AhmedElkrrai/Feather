package com.elkrrai.feather.feature.habits

import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.TREE_STAGES
import com.elkrrai.feather.domain.habit.TREE_WEEKS_PER_STAGE
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Sample habits with months of history, to see the all-time view without waiting for the weeks to pass.
 * Only a debug build offers it (a button at the end of Settings). It uses the repository like the
 * screens do, so what it makes is ordinary data: delete the habits from their menu to remove it.
 *
 * - "Read": pages, 26 weeks, the goal raised twice (20, 25, 30).
 * - "Workout": days, 40 weeks, 3 days a week and 4 from half way.
 * - "Pray": 5 times a day, 7 days a week, 12 weeks.
 */
suspend fun HabitRepository.addSampleHabits(today: LocalDate, firstDay: DayOfWeek) {
    val thisWeek = weekStartOf(today, firstDay)
    val random = Random(7)

    suspend fun log(habitId: String, weekStart: LocalDate, days: List<Pair<Int, Int>>) {
        for ((offset, amount) in days) {
            val date = weekStart.plusDays(offset.toLong())
            if (!date.isAfter(today)) setAmount(habitId, date, amount)
        }
    }

    // Read: 26 weeks of pages, a few short ones among them
    val readFirst = thisWeek.minusWeeks(25)
    val read = addHabit("Read", HabitKind.AMOUNT, "pages", 20, 1, readFirst)
    queueGoal(read.id, 25, 1, thisWeek.minusWeeks(15))
    queueGoal(read.id, 30, 1, thisWeek.minusWeeks(7))
    for (week in 0..25) {
        val goal = when {
            week >= 18 -> 30
            week >= 10 -> 25
            else -> 20
        }
        val total = (goal * (0.55 + random.nextDouble() * 0.75)).toInt()
        val daysUsed = (0..6).shuffled(random).take(3 + random.nextInt(3)).sorted()
        val share = total / daysUsed.size
        log(read.id, readFirst.plusWeeks(week.toLong()), daysUsed.mapIndexed { i, day -> day to share + if (i == 0) total % daysUsed.size else 0 })
    }

    // Workout: 40 weeks, three days and later four
    val workoutFirst = thisWeek.minusWeeks(39)
    val workout = addHabit("Workout", HabitKind.DAYS, "", 3, 1, workoutFirst)
    queueGoal(workout.id, 4, 1, thisWeek.minusWeeks(19))
    for (week in 0..39) {
        val goal = if (week >= 20) 4 else 3
        val done = (goal + random.nextInt(-2, 2)).coerceIn(0, 7)
        val days = (0..6).shuffled(random).take(done).sorted()
        log(workout.id, workoutFirst.plusWeeks(week.toLong()), days.map { it to 1 })
    }

    // Pray: five times a day for seven days; most days are complete
    val prayFirst = thisWeek.minusWeeks(11)
    val pray = addHabit("Pray", HabitKind.DAYS, "", 7, 5, prayFirst)
    for (week in 0..11) {
        val days = (0..6).map { day -> day to if (random.nextInt(10) < 8) 5 else 3 + random.nextInt(2) }
        log(pray.id, prayFirst.plusWeeks(week.toLong()), days)
    }
}

/**
 * Fourteen habits, one for every picture of the tree: "Tree 0" has no met week (the seed), "Tree 1" has
 * the first week of stage 1, "Tree 2" the first of stage 2, up to "Tree 13" with the first week of stage
 * 13. Each is a habit of one day a week with that many past weeks logged.
 */
suspend fun HabitRepository.addTreeStageHabits(today: LocalDate, firstDay: DayOfWeek) {
    val thisWeek = weekStartOf(today, firstDay)
    for (stage in 0..TREE_STAGES) {
        val metWeeks = if (stage == 0) 0 else (stage - 1) * TREE_WEEKS_PER_STAGE + 1
        val habit = addHabit("Tree $stage", HabitKind.DAYS, "", 1, 1, thisWeek.minusWeeks(maxOf(metWeeks, 1).toLong()))
        for (week in 1..metWeeks) setAmount(habit.id, thisWeek.minusWeeks(week.toLong()), 1)
    }
}

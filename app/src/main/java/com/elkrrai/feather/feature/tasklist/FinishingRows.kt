package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.Task
import java.time.LocalDate

/**
 * A task that was just finished and is kept in the list for a moment (see `Dur.RowLinger`), so its
 * ring, strike and feathers can play before the row leaves. The list's data is not touched: the
 * row is only kept on screen.
 *
 * @property task the task as finished (its `completedAt` is set).
 * @property index where it was in its section when it was tapped, to put it back in place.
 * @property sectionKey the date of its section, or null for a list without date sections.
 * @property stamp tells one tap from another, so an old timer never removes a newer entry.
 */
internal data class FinishingRow(
    val task: Task,
    val index: Int,
    val sectionKey: LocalDate?,
    val stamp: Long,
)

/**
 * [tasks] (one section of the list, as the database has it) with the rows that are still finishing:
 * a finishing task that has already left the list is put back at its old position, and one that is
 * still listed but not yet shown as finished (the database has not caught up with the tap) is shown
 * as finished. Returns [tasks] itself when nothing is finishing in this section.
 */
internal fun withFinishing(
    tasks: List<Task>,
    finishing: Map<String, FinishingRow>,
    sectionKey: LocalDate?,
): List<Task> {
    val mine = finishing.values.filter { it.sectionKey == sectionKey }
    if (mine.isEmpty()) return tasks

    val result = tasks.toMutableList()
    for (row in mine) {
        val at = result.indexOfFirst { it.id == row.task.id }
        if (at >= 0 && !result[at].isCompleted) result[at] = row.task
    }
    val present = result.mapTo(HashSet()) { it.id }
    for (row in mine.filter { it.task.id !in present }.sortedBy { it.index }) {
        result.add(row.index.coerceIn(0, result.size), row.task)
    }
    return result
}

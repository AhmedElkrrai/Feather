package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FinishingRowsTest {

    private val now = Instant.parse("2026-10-15T08:00:00Z")

    private fun task(id: String, done: Boolean = false) = Task(
        id = id,
        title = id,
        completedAt = if (done) now else null,
        sortOrder = 0,
        createdAt = now,
        updatedAt = now,
    )

    private fun row(id: String, index: Int, section: LocalDate? = null, stamp: Long = 1L) =
        FinishingRow(task(id, done = true), index, section, stamp)

    private fun ids(tasks: List<Task>) = tasks.map { it.id }

    @Test
    fun withNothingFinishingTheListIsReturnedAsItIs() {
        val tasks = listOf(task("a"), task("b"))

        assertSame(tasks, withFinishing(tasks, emptyMap(), null))
    }

    @Test
    fun aFinishedTaskThatLeftTheListIsPutBackWhereItWas() {
        val tasks = listOf(task("a"), task("c"))
        val finishing = mapOf("b" to row("b", index = 1))

        val shown = withFinishing(tasks, finishing, null)

        assertEquals(listOf("a", "b", "c"), ids(shown))
        assertEquals(true, shown[1].isCompleted)
    }

    @Test
    fun aPositionPastTheEndIsClamped() {
        val tasks = listOf(task("a"))
        val finishing = mapOf("z" to row("z", index = 9))

        assertEquals(listOf("a", "z"), ids(withFinishing(tasks, finishing, null)))
    }

    @Test
    fun aTaskStillListedAsOpenIsShownAsFinished() {
        // The database has not caught up with the tap yet.
        val tasks = listOf(task("a"), task("b"))
        val finishing = mapOf("a" to row("a", index = 0))

        val shown = withFinishing(tasks, finishing, null)

        assertEquals(listOf("a", "b"), ids(shown))
        assertEquals(true, shown[0].isCompleted)
        assertEquals(false, shown[1].isCompleted)
    }

    @Test
    fun aTaskTheDatabaseAlreadyShowsAsFinishedIsLeftAlone() {
        val real = task("a", done = true)
        val tasks = listOf(real, task("b"))
        val finishing = mapOf("a" to row("a", index = 0))

        assertSame(real, withFinishing(tasks, finishing, null)[0])
    }

    @Test
    fun onlyTheRowsOfThisSectionAreRestored() {
        val monday = LocalDate.of(2026, 10, 19)
        val tuesday = LocalDate.of(2026, 10, 20)
        val finishing = mapOf("x" to row("x", index = 0, section = monday))

        assertEquals(listOf("a"), ids(withFinishing(listOf(task("a")), finishing, tuesday)))
        assertEquals(listOf("x", "a"), ids(withFinishing(listOf(task("a")), finishing, monday)))
    }

    @Test
    fun severalFinishingRowsGoBackInTheirOldOrder() {
        val tasks = listOf(task("a"), task("d"))
        val finishing = mapOf(
            "c" to row("c", index = 2),
            "b" to row("b", index = 1),
        )

        assertEquals(listOf("a", "b", "c", "d"), ids(withFinishing(tasks, finishing, null)))
    }
}

package com.elkrrai.feather.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMergerTest {

    private fun task(id: String, updatedAt: Long, title: String = id) =
        TaskRecord(id = id, title = title, updatedAt = updatedAt)

    private fun tag(id: String, name: String, updatedAt: Long = 1, deletedAt: Long? = null) =
        TagRecord(id = id, name = name, updatedAt = updatedAt, deletedAt = deletedAt)

    private fun plan(local: BackupFile, incoming: BackupFile) = BackupMerger.plan(local, incoming)

    @Test
    fun anEmptyPhoneTakesEverythingInTheFile() {
        val file = BackupFile(
            tags = listOf(tag("t", "work")),
            tasks = listOf(task("a", 1)),
            taskTags = listOf(TaskTagRecord("a", "t")),
            reminders = listOf(ReminderRecord("r", "a", kind = 0)),
            completions = listOf(CompletionRecord("c", "a", completedAt = 5)),
        )

        val plan = plan(BackupFile(), file)

        assertEquals(file.tags, plan.tags)
        assertEquals(file.tasks, plan.tasks)
        assertEquals(file.taskTags, plan.tagLinks)
        assertEquals(file.reminders, plan.reminders)
        assertEquals(file.completions, plan.completions)
        assertEquals(setOf("a"), plan.rewrittenTaskIds)
    }

    @Test
    fun theNewerEditWinsAndATieKeepsThePhone() {
        val local = BackupFile(tasks = listOf(task("newer-here", 10), task("older-here", 5), task("tie", 7)))
        val incoming = BackupFile(
            tasks = listOf(
                task("newer-here", 9, title = "file"),
                task("older-here", 6, title = "file"),
                task("tie", 7, title = "file"),
                task("only-in-file", 1),
            ),
        )

        val plan = plan(local, incoming)

        assertEquals(setOf("older-here", "only-in-file"), plan.tasks.map { it.id }.toSet())
        assertEquals("file", plan.tasks.first { it.id == "older-here" }.title)
    }

    @Test
    fun aRecordListedTwiceInTheFileCountsOnceWithItsLatestCopy() {
        val incoming = BackupFile(tasks = listOf(task("a", 1, title = "old"), task("a", 3, title = "new")))

        val plan = plan(BackupFile(), incoming)

        assertEquals(listOf("new"), plan.tasks.map { it.title })
    }

    @Test
    fun aTagWithTheSameNameAsALiveTagJoinsIt() {
        val local = BackupFile(tags = listOf(tag("t1", "Work")), tasks = listOf(task("old", 1)))
        val incoming = BackupFile(
            tags = listOf(tag("t9", " work ")),
            tasks = listOf(task("a", 1)),
            taskTags = listOf(TaskTagRecord("a", "t9")),
        )

        val plan = plan(local, incoming)

        assertTrue(plan.tags.isEmpty())
        assertEquals(listOf(TaskTagRecord("a", "t1")), plan.tagLinks)
    }

    @Test
    fun aDeletedTagIsNotJoinedToALiveOne() {
        val local = BackupFile(tags = listOf(tag("t1", "work")))
        val incoming = BackupFile(tags = listOf(tag("t9", "work", deletedAt = 5)))

        assertEquals(listOf("t9"), plan(local, incoming).tags.map { it.id })
    }

    @Test
    fun tagLinksAndRemindersCameOnlyForTasksTakenFromTheFile() {
        val local = BackupFile(tasks = listOf(task("kept", 10)), tags = listOf(tag("t", "x")))
        val incoming = BackupFile(
            tasks = listOf(task("kept", 5), task("fresh", 5)),
            taskTags = listOf(TaskTagRecord("kept", "t"), TaskTagRecord("fresh", "t")),
            reminders = listOf(ReminderRecord("r1", "kept", kind = 0), ReminderRecord("r2", "fresh", kind = 0)),
        )

        val plan = plan(local, incoming)

        assertEquals(setOf("fresh"), plan.rewrittenTaskIds)
        assertEquals(listOf(TaskTagRecord("fresh", "t")), plan.tagLinks)
        assertEquals(listOf("r2"), plan.reminders.map { it.id })
    }

    @Test
    fun aLinkToAnUnknownTagIsDropped() {
        val incoming = BackupFile(tasks = listOf(task("a", 1)), taskTags = listOf(TaskTagRecord("a", "ghost")))

        assertTrue(plan(BackupFile(), incoming).tagLinks.isEmpty())
    }

    @Test
    fun completionHistoryIsAUnion() {
        val local = BackupFile(
            tasks = listOf(task("a", 10)),
            completions = listOf(CompletionRecord("c1", "a", completedAt = 1)),
        )
        val incoming = BackupFile(
            tasks = listOf(task("a", 5)),
            completions = listOf(
                CompletionRecord("c1", "a", completedAt = 1),
                CompletionRecord("c2", "a", completedAt = 2),
                CompletionRecord("c3", "unknown-task", completedAt = 3),
            ),
        )

        assertEquals(listOf("c2"), plan(local, incoming).completions.map { it.id })
    }

    @Test
    fun habitsFollowTheSameNewestWinsRule() {
        val local = BackupFile(
            habits = listOf(HabitRecord("kept", "Local", updatedAt = 10), HabitRecord("old", "Local", updatedAt = 1)),
        )
        val incoming = BackupFile(
            habits = listOf(
                HabitRecord("kept", "File", updatedAt = 5),
                HabitRecord("old", "File", updatedAt = 2),
                HabitRecord("new", "File", updatedAt = 1),
            ),
        )

        val plan = plan(local, incoming)

        assertEquals(setOf("old", "new"), plan.habits.map { it.id }.toSet())
    }
}

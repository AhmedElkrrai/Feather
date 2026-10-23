package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TagFilterTest {
    // life > work > bugfix; home on its own
    private val life = Tag("life", "life")
    private val work = Tag("work", "work", parentId = "life")
    private val bugfix = Tag("bugfix", "bugfix", parentId = "work")
    private val home = Tag("home", "home")
    private val tags = listOf(life, work, bugfix, home)

    private fun task(title: String, vararg taskTags: Tag) = Task(
        id = "id-$title",
        title = title,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        tags = taskTags.toList(),
    )

    private val tasks = listOf(
        task("Fix crash", bugfix),
        task("Write report", work),
        task("Call mum", life),
        task("Buy milk", home),
        task("Nothing special"),
    )

    private fun List<Task>.titles() = map { it.title }

    @Test
    fun theFirstTabShowsEverything() {
        assertEquals(tasks.titles(), tasks.withTagFilter(TagFilter.All, tags).titles())
    }

    @Test
    fun theLastTabShowsOnlyTasksWithoutATag() {
        assertEquals(listOf("Nothing special"), tasks.withTagFilter(TagFilter.Untagged, tags).titles())
    }

    @Test
    fun aTagTabShowsItsTasksAndTheTasksOfTagsUnderIt() {
        assertEquals(
            listOf("Fix crash", "Write report"),
            tasks.withTagFilter(TagFilter.OfTag("work"), tags).titles(),
        )
        assertEquals(
            listOf("Fix crash", "Write report", "Call mum"),
            tasks.withTagFilter(TagFilter.OfTag("life"), tags).titles(),
        )
        assertEquals(listOf("Buy milk"), tasks.withTagFilter(TagFilter.OfTag("home"), tags).titles())
    }

    @Test
    fun aTaskWithTwoTagsShowsUnderBothTabs() {
        val both = task("Both", bugfix, home)
        val all = listOf(both)

        assertEquals(listOf("Both"), all.withTagFilter(TagFilter.OfTag("work"), tags).titles())
        assertEquals(listOf("Both"), all.withTagFilter(TagFilter.OfTag("home"), tags).titles())
        assertEquals(emptyList<String>(), all.withTagFilter(TagFilter.Untagged, tags).titles())
    }
}

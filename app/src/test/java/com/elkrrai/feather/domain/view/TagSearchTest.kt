package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TagSearchTest {
    // life > work > bugfix; home is on its own
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
        task("Untagged"),
        task("Both", bugfix, home),
    )

    private fun List<Task>.titles() = map { it.title }

    @Test
    fun pickingATagFindsItsTasksAndEveryTagUnderIt() {
        val found = tasks.searchTasks("", tags, selectedTagId = "work")

        assertEquals(listOf("Fix crash", "Write report", "Both"), found.titles())
    }

    @Test
    fun theTopOfTheChainFindsTheWholeChain() {
        val found = tasks.searchTasks("", tags, selectedTagId = "life")

        assertEquals(listOf("Fix crash", "Write report", "Call mum", "Both"), found.titles())
    }

    @Test
    fun aLeafTagFindsOnlyItsOwnTasks() {
        assertEquals(listOf("Fix crash", "Both"), tasks.searchTasks("", tags, selectedTagId = "bugfix").titles())
    }

    @Test
    fun aHashQueryActsAsATagFilterIncludingSubTags() {
        assertEquals(listOf("Fix crash", "Write report", "Both"), tasks.searchTasks("#work", tags, null).titles())
        // it works on the start of the name, so it already finds tasks while the name is being typed
        assertEquals(listOf("Fix crash", "Write report", "Both"), tasks.searchTasks("#wo", tags, null).titles())
    }

    @Test
    fun aHashQueryThatMatchesNoTagIsPlainText() {
        val withHash = listOf(task("Use #hashtags in notes"))

        assertEquals(listOf("Use #hashtags in notes"), withHash.searchTasks("#hashtags", tags, null).titles())
    }

    @Test
    fun textNarrowsAPickedTagFurther() {
        val found = tasks.searchTasks("crash", tags, selectedTagId = "work")

        assertEquals(listOf("Fix crash"), found.titles())
    }

    @Test
    fun noTagAndNoTextFindsNothing() {
        assertEquals(emptyList<Task>(), tasks.searchTasks("  ", tags, null))
    }

    @Test
    fun plainTextStillSearchesTitlesAndTagNames() {
        assertEquals(listOf("Buy milk"), tasks.searchTasks("milk", tags, null).titles())
        assertEquals(listOf("Buy milk", "Both"), tasks.searchTasks("home", tags, null).titles())
    }

    @Test
    fun twoFiltersMustBothMatch() {
        val found = tasks.searchTasks("#home", tags, selectedTagId = "work")

        assertEquals(listOf("Both"), found.titles())
    }
}

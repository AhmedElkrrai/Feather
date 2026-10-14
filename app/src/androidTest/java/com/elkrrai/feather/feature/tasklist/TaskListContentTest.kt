package com.elkrrai.feather.feature.tasklist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskListContentTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val today = LocalDate.of(2026, 10, 12)

    private fun task(
        id: String,
        title: String,
        tags: List<Tag> = emptyList(),
        subtasks: List<Subtask> = emptyList(),
        pinned: Boolean = false,
    ) = Task(
        id = id,
        listId = "list",
        title = title,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        isPinned = pinned,
        tags = tags,
        subtasks = subtasks,
    )

    private var toggled: Pair<String, Boolean>? = null
    private var deleted: String? = null
    private var opened: String? = null
    private var addClicks = 0
    private var searchClicks = 0
    private var menuClicks = 0
    private var chosenSort: TaskSort? = null

    private fun show(
        tasks: List<Task> = emptyList(),
        sections: List<TaskSection> = listOf(TaskSection(null, tasks)),
        scope: TaskScope = TaskScope.InList("list"),
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                FeatherTheme(dynamicColor = false) {
                    TaskListContent(
                        state = TaskListUiState(isLoading = false, scope = scope, sections = sections),
                        snackbarHostState = SnackbarHostState(),
                        onMenuClick = { menuClicks++ },
                        onAddClick = { addClicks++ },
                        onToggle = { task, completed -> toggled = task.id to completed },
                        onDelete = { deleted = it.id },
                        onOpenTask = { opened = it },
                        onSearchClick = { searchClicks++ },
                        onSortChange = { chosenSort = it },
                        today = today,
                    )
                }
            }
        }
    }

    @Test
    fun emptyListShowsTheEmptyState() {
        show(emptyList())

        // The Inbox is where a new user starts, so it greets.
        rule.onNodeWithText(string(R.string.scene_first_launch)).assertIsDisplayed()
    }

    @Test
    fun emptyDateViewSaysNothingIsDue() {
        show(emptyList(), scope = TaskScope.Upcoming)

        rule.onNodeWithText(string(R.string.scene_nothing_due)).assertIsDisplayed()
    }

    @Test
    fun emptyTodayShowsItsHeaderInsteadOfAPicture() {
        show(emptyList(), scope = TaskScope.Today)

        rule.onNodeWithText(string(R.string.today_nothing_planned)).assertIsDisplayed()
    }

    @Test
    fun tasksAreListed() {
        show(listOf(task("1", "Buy milk"), task("2", "Call mom")))

        rule.onNodeWithText("Buy milk").assertIsDisplayed()
        rule.onNodeWithText("Call mom").assertIsDisplayed()
    }

    @Test
    fun rowsShowTagsAndSubtaskProgress() {
        show(
            listOf(
                task(
                    "1",
                    "Trip",
                    tags = listOf(Tag("t", "travel")),
                    subtasks = listOf(Subtask("a", "Passport", isCompleted = true), Subtask("b", "Tickets")),
                )
            )
        )

        rule.onNodeWithText("#travel").assertIsDisplayed()
        rule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.subtask_progress, 1, 2)
        ).assertIsDisplayed()
    }

    @Test
    fun upcomingSectionsShowDayHeaders() {
        show(
            sections = listOf(
                TaskSection(today.plusDays(1), listOf(task("1", "Tomorrow task"))),
                TaskSection(today.plusDays(3), listOf(task("2", "Later task"))),
            ),
            scope = TaskScope.Upcoming,
        )

        rule.onNodeWithText(string(R.string.due_tomorrow)).assertIsDisplayed()
        rule.onNodeWithText("Tomorrow task").assertIsDisplayed()
        rule.onNodeWithText("Later task").assertIsDisplayed()
    }

    @Test
    fun tappingTheCheckboxReportsCompletion() {
        show(listOf(task("1", "Buy milk")))

        rule.onNode(isToggleable()).performClick()

        assertEquals("1" to true, toggled)
    }

    @Test
    fun tappingARowOpensTheTask() {
        show(listOf(task("1", "Buy milk")))

        rule.onNodeWithText("Buy milk").performClick()

        assertEquals("1", opened)
    }

    @Test
    fun swipingARowReportsDelete() {
        show(listOf(task("1", "Buy milk")))

        rule.onNodeWithText("Buy milk").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertEquals("1", deleted)
    }

    @Test
    fun topBarButtonsReportClicks() {
        show(emptyList())

        rule.onNodeWithContentDescription(string(R.string.action_open_menu)).performClick()
        rule.onNodeWithContentDescription(string(R.string.action_search)).performClick()

        assertEquals(1, menuClicks)
        assertEquals(1, searchClicks)
    }

    @Test
    fun sortMenuReportsTheChosenSort() {
        show(emptyList())

        rule.onNodeWithContentDescription(string(R.string.action_more_options)).performClick()
        rule.onNodeWithText(string(R.string.sort_priority)).performClick()

        assertEquals(TaskSort.PRIORITY, chosenSort)
    }

    @Test
    fun addButtonReportsClick() {
        show(emptyList())

        rule.onNodeWithContentDescription(string(R.string.action_add_task)).performClick()

        assertEquals(1, addClicks)
    }

    @Test
    fun addButtonIsHiddenInTheCompletedView() {
        show(emptyList(), scope = TaskScope.Completed)

        rule.onAllNodesWithContentDescription(string(R.string.action_add_task)).assertCountEquals(0)
    }

    @Test
    fun addButtonSitsOnTheRightInLtr() {
        show(emptyList(), direction = LayoutDirection.Ltr)
        val rootWidth = rule.onRoot().getBoundsInRoot().width
        val ltr = rule.onNodeWithContentDescription(string(R.string.action_add_task)).getBoundsInRoot()
        assertTrue("FAB should be on the right in LTR", ltr.left > rootWidth / 2)
    }

    @Test
    fun addButtonMirrorsToTheLeftInRtl() {
        show(emptyList(), direction = LayoutDirection.Rtl)
        val rootWidth = rule.onRoot().getBoundsInRoot().width
        val rtl = rule.onNodeWithContentDescription(string(R.string.action_add_task)).getBoundsInRoot()
        assertTrue("FAB should be on the left in RTL", rtl.right < rootWidth / 2)
    }
}

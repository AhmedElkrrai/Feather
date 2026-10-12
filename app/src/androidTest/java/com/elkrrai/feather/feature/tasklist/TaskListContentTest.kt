package com.elkrrai.feather.feature.tasklist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskListContentTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun task(id: String, title: String) = Task(
        id = id,
        listId = "list",
        title = title,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private var toggled: Pair<String, Boolean>? = null
    private var deleted: String? = null
    private var opened: String? = null
    private var addClicks = 0

    private fun show(
        tasks: List<Task>,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                FeatherTheme(dynamicColor = false) {
                    TaskListContent(
                        state = TaskListUiState(tasks = tasks, isLoading = false),
                        snackbarHostState = SnackbarHostState(),
                        onAddClick = { addClicks++ },
                        onToggle = { task, completed -> toggled = task.id to completed },
                        onDelete = { deleted = it.id },
                        onOpenTask = { opened = it },
                        onLanguageClick = {},
                    )
                }
            }
        }
    }

    @Test
    fun emptyListShowsTheEmptyState() {
        show(emptyList())

        rule.onNodeWithText(string(R.string.task_list_empty_title)).assertIsDisplayed()
    }

    @Test
    fun tasksAreListed() {
        show(listOf(task("1", "Buy milk"), task("2", "Call mom")))

        rule.onNodeWithText("Buy milk").assertIsDisplayed()
        rule.onNodeWithText("Call mom").assertIsDisplayed()
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
    fun addButtonReportsClick() {
        show(emptyList())

        rule.onNodeWithContentDescription(string(R.string.action_add_task)).performClick()

        assertEquals(1, addClicks)
    }

    @Test
    fun addButtonSitsOnTheEndSideInBothDirections() {
        show(emptyList(), LayoutDirection.Ltr)
        val rootWidth = rule.onRoot().getBoundsInRoot().width
        val ltr = rule.onNodeWithContentDescription(string(R.string.action_add_task)).getBoundsInRoot()
        assertTrue("FAB should be on the right in LTR", ltr.left > rootWidth / 2)
    }

    @Test
    fun addButtonMirrorsToTheLeftInRtl() {
        show(emptyList(), LayoutDirection.Rtl)
        val rootWidth = rule.onRoot().getBoundsInRoot().width
        val rtl = rule.onNodeWithContentDescription(string(R.string.action_add_task)).getBoundsInRoot()
        assertTrue("FAB should be on the left in RTL", rtl.right < rootWidth / 2)
    }
}

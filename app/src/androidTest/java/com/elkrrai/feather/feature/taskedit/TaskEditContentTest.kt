package com.elkrrai.feather.feature.taskedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskEditContentTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private var saves = 0
    private var toggledTag: Tag? = null
    private var removedSubtask: String? = null
    private var pinned: Boolean? = null

    private fun show(state: TaskEditUiState) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                TaskEditContent(
                    state = state,
                    actions = TaskEditActions(
                        onBack = {},
                        onSave = { saves++ },
                        onTitleChange = {},
                        onNotesChange = {},
                        onDueDateChange = {},
                        onDueTimeChange = {},
                        onPriorityChange = {},
                        onListChange = {},
                        onTagToggle = { toggledTag = it },
                        onCreateTag = {},
                        onPinnedChange = { pinned = it },
                        onAddSubtask = {},
                        onSubtaskToggle = {},
                        onSubtaskTitleChange = { _, _ -> },
                        onSubtaskRemove = { removedSubtask = it },
                    ),
                )
            }
        }
    }

    @Test
    fun saveIsDisabledWhileTheTitleIsBlank() {
        show(TaskEditUiState(isLoading = false, title = ""))

        rule.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled()
    }

    @Test
    fun saveReportsClickOnceTheTitleIsFilled() {
        show(TaskEditUiState(isLoading = false, title = "Dentist"))

        rule.onNodeWithText(string(R.string.action_save)).assertIsEnabled().performClick()

        assertEquals(1, saves)
    }

    @Test
    fun timeCannotBeSetWithoutADate() {
        show(TaskEditUiState(isLoading = false, title = "Dentist", dueDate = null))

        rule.onNodeWithText(string(R.string.set_time)).assertIsNotEnabled()
    }

    @Test
    fun timeCanBeSetOnceThereIsADate() {
        show(TaskEditUiState(isLoading = false, title = "Dentist", dueDate = LocalDate.of(2026, 10, 20)))

        rule.onNodeWithText(string(R.string.set_time)).assertIsEnabled()
    }

    @Test
    fun availableTagsAreShownAndTogglingReportsTheTag() {
        val work = Tag("t1", "work")
        show(TaskEditUiState(isLoading = false, title = "Task", availableTags = listOf(work)))

        rule.onNodeWithText("#work").assertIsDisplayed().performClick()

        assertEquals(work, toggledTag)
    }

    @Test
    fun existingSubtasksAreShownAndCanBeRemoved() {
        show(
            TaskEditUiState(
                isLoading = false,
                title = "Trip",
                subtasks = listOf(Subtask("s1", "Passport")),
            )
        )

        rule.onNodeWithText("Passport").assertIsDisplayed()
        rule.onNodeWithContentDescription(string(R.string.remove_subtask)).performClick()

        assertEquals("s1", removedSubtask)
    }
}

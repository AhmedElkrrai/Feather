package com.elkrrai.feather.feature.taskedit

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
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

    private fun show(state: TaskEditUiState) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                TaskEditContent(
                    state = state,
                    onBack = {},
                    onSave = { saves++ },
                    onTitleChange = {},
                    onNotesChange = {},
                    onDueDateChange = {},
                    onDueTimeChange = {},
                    onPriorityChange = {},
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
}

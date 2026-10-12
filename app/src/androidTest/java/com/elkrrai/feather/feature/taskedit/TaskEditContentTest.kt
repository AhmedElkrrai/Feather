package com.elkrrai.feather.feature.taskedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.reminders.ReminderHealth
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskEditContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun string(id: Int) = resources.getString(id)

    private var saves = 0
    private var toggledTag: Tag? = null
    private var removedSubtask: String? = null
    private var removedReminder: String? = null
    private var addedOffset: Int? = null
    private var addedAt: LocalDateTime? = null
    private var reliabilityOpened = 0
    private var permissionRequests = 0

    private fun show(state: TaskEditUiState, health: ReminderHealth = ReminderHealth()) {
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
                        onPinnedChange = {},
                        onAddSubtask = {},
                        onSubtaskToggle = {},
                        onSubtaskTitleChange = { _, _ -> },
                        onSubtaskRemove = { removedSubtask = it },
                        onAddBeforeDue = { addedOffset = it },
                        onAddAtTime = { addedAt = it },
                        onRemoveReminder = { removedReminder = it },
                        onOpenReliability = { reliabilityOpened++ },
                    ),
                    health = health,
                    onRequestNotificationPermission = { permissionRequests++ },
                )
            }
        }
    }

    private val dueOn20th = LocalDate.of(2026, 10, 20)

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
        show(TaskEditUiState(isLoading = false, title = "Dentist", dueDate = dueOn20th))

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

    // --- Reminders ---

    @Test
    fun remindersAreListedWithReadableLabelsAndCanBeRemoved() {
        show(
            TaskEditUiState(
                isLoading = false,
                title = "Dentist",
                dueDate = dueOn20th,
                reminders = listOf(
                    Reminder("r1", ReminderKind.BEFORE_DUE, offsetMinutes = 0),
                    Reminder("r2", ReminderKind.BEFORE_DUE, offsetMinutes = 30),
                ),
            )
        )

        rule.onNodeWithText(string(R.string.reminder_at_due_time)).assertIsDisplayed()
        rule.onNodeWithText(resources.getQuantityString(R.plurals.reminder_minutes_before, 30, 30)).assertIsDisplayed()

        rule.onAllNodesWithContentDescription(string(R.string.remove_reminder))[1].performClick()
        assertEquals("r2", removedReminder)
    }

    @Test
    fun addingAReminderAsksForPermissionAndOffersPresetsRelativeToTheDueDate() {
        show(TaskEditUiState(isLoading = false, title = "Dentist", dueDate = dueOn20th))

        rule.onNodeWithText(string(R.string.add_reminder)).performClick()
        assertEquals(1, permissionRequests)

        rule.onNodeWithText(resources.getQuantityString(R.plurals.reminder_hours_before, 1, 1)).performClick()
        assertEquals(60, addedOffset)
    }

    @Test
    fun withoutADueDateOnlySpecificTimesAreOffered() {
        show(TaskEditUiState(isLoading = false, title = "Dentist", dueDate = null))

        rule.onNodeWithText(string(R.string.add_reminder)).performClick()

        rule.onNodeWithText(string(R.string.reminder_needs_due_date)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.reminder_at_due_time)).performClick()
        assertEquals(null, addedOffset) // disabled: nothing was added
        rule.onNodeWithText(string(R.string.reminder_at_specific_time)).assertIsDisplayed()
    }

    @Test
    fun aWarningWithAFixButtonAppearsWhenRemindersMightNotArrive() {
        show(
            TaskEditUiState(
                isLoading = false,
                title = "Dentist",
                dueDate = dueOn20th,
                reminders = listOf(Reminder("r1", ReminderKind.BEFORE_DUE)),
            ),
            health = ReminderHealth(notificationsEnabled = false),
        )

        rule.onNodeWithText(string(R.string.reminder_warning)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.reminder_warning_fix)).performClick()

        assertEquals(1, reliabilityOpened)
    }

    @Test
    fun noWarningWhenEverythingIsFine() {
        show(
            TaskEditUiState(
                isLoading = false,
                title = "Dentist",
                dueDate = dueOn20th,
                reminders = listOf(Reminder("r1", ReminderKind.BEFORE_DUE)),
            ),
        )

        rule.onAllNodesWithText(string(R.string.reminder_warning)).assertCountEquals(0)
    }
}

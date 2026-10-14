package com.elkrrai.feather.feature.trash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrashContentTest {

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
        deletedAt = Instant.now(),
    )

    private var restored: String? = null
    private var deletedForever: String? = null
    private var emptied = 0

    private fun show(tasks: List<Task>) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                TrashContent(
                    state = TrashUiState(tasks = tasks, isLoading = false),
                    onBack = {},
                    onRestore = { restored = it },
                    onDeleteForever = { deletedForever = it },
                    onEmptyTrash = { emptied++ },
                )
            }
        }
    }

    @Test
    fun anEmptyTrashShowsItsMessage() {
        show(emptyList())

        rule.onNodeWithText(string(R.string.scene_trash_empty)).assertIsDisplayed()
    }

    @Test
    fun restoreReportsTheTask() {
        show(listOf(task("1", "Old task")))

        rule.onNodeWithText("Old task").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.action_restore)).performClick()

        assertEquals("1", restored)
    }

    @Test
    fun deletingForeverAsksForConfirmationFirst() {
        show(listOf(task("1", "Old task")))

        rule.onNodeWithContentDescription(string(R.string.action_delete_forever)).performClick()
        assertEquals(null, deletedForever)

        rule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertEquals("1", deletedForever)
    }

    @Test
    fun emptyingTheTrashAsksForConfirmationFirst() {
        show(listOf(task("1", "Old task")))

        rule.onNodeWithContentDescription(string(R.string.action_more_options)).performClick()
        rule.onNodeWithText(string(R.string.action_empty_trash)).performClick()
        assertEquals(0, emptied)

        rule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertEquals(1, emptied)
    }
}

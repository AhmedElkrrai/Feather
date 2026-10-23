package com.elkrrai.feather.feature.trash

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.R
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.repository.InboxRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.data.TestClock
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.feature.tasklist.TaskListContent
import com.elkrrai.feather.feature.tasklist.TaskListViewModel
import com.elkrrai.feather.ui.theme.FeatherTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole trip on screen, on a real (in-memory) database: a task is created and finished, deleted from
 * the Completed screen, found in the Trash screen and restored there, and must be on the Completed
 * screen again. Covers "I restored a task from the trash and sometimes could not see it".
 */
@RunWith(AndroidJUnit4::class)
class TrashRestoreScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: FeatherDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var list: TaskListViewModel
    private lateinit var trash: TrashViewModel

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).allowMainThreadQueries().build()
        val clock = TestClock()
        val tx = RoomTransactionRunner(db)
        tasks = TaskRepositoryImpl(db.taskDao(), db.reminderDao(), db.completionDao(), tx, clock)
        InboxRepositoryImpl(db.listDao(), clock).ensureInbox()
        val tags = TagRepositoryImpl(db.tagDao(), clock)
        list = TaskListViewModel(tasks, tags, clock)
        trash = TrashViewModel(tasks)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Shows the Completed screen or the Trash screen, fed by the real view models. */
    private fun showScreens(onScreen: () -> String) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                val listState by list.uiState.collectAsState()
                val trashState by trash.uiState.collectAsState()
                if (onScreen() == "trash") {
                    TrashContent(
                        state = trashState,
                        onBack = {},
                        onRestore = trash::restore,
                        onDeleteForever = trash::deleteForever,
                        onEmptyTrash = trash::emptyTrash,
                    )
                } else {
                    TaskListContent(
                        state = listState,
                        snackbarHostState = SnackbarHostState(),
                        onMenuClick = {},
                        onAddClick = {},
                        onToggle = { _, _ -> },
                        onDelete = {},
                        onOpenTask = {},
                        onSearchClick = {},
                        onSortChange = {},
                    )
                }
            }
        }
    }

    @Test
    fun aFinishedTaskDeletedFromCompletedIsBackOnTheCompletedScreenAfterRestoringIt() {
        var screen by mutableStateOf("completed")
        val task = runBlocking { tasks.addTask("Write report") }
        runBlocking { tasks.setCompleted(task.id, true) }
        list.selectScope(TaskScope.Completed)
        showScreens { screen }

        rule.onNodeWithText("Write report").assertIsDisplayed()

        // delete it from the Completed screen, then open the Trash screen
        list.deleteTask(task.id)
        screen = "trash"
        rule.waitUntil(5_000) { runBlocking { tasks.observeTrash().first().isNotEmpty() } }
        rule.onNodeWithText("Write report").assertIsDisplayed()

        rule.onNodeWithText(ApplicationProvider.getApplicationContext<Context>().getString(R.string.action_restore)).performClick()

        // back to the Completed screen: the task must be there, in the list
        screen = "completed"
        rule.waitUntil(5_000) { runBlocking { tasks.getTask(task.id) != null } }
        rule.onNodeWithText("Write report").assertIsDisplayed()
    }
}

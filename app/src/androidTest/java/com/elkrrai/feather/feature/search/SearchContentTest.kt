package com.elkrrai.feather.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

class SearchContentTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)


    private fun task(id: String, title: String) = Task(
        id = id,
        title = title,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private var opened: String? = null
    private var typed: String? = null

    private fun show(state: SearchUiState) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                SearchContent(
                    state = state,
                    onQueryChange = { typed = it },
                    onToggle = { _, _ -> },
                    onOpenTask = { opened = it },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun beforeTypingAPromptIsShown() {
        show(SearchUiState())

        rule.onNodeWithText(string(R.string.search_prompt_title)).assertIsDisplayed()
    }

    @Test
    fun resultsAreListedAndOpenable() {
        show(SearchUiState(query = "milk", results = listOf(task("1", "Buy milk"))))

        rule.onNodeWithText("Buy milk").assertIsDisplayed().performClick()

        assertEquals("1", opened)
    }

    @Test
    fun aQueryWithNoMatchesShowsTheNoResultsMessage() {
        show(SearchUiState(query = "zzz", results = emptyList()))

        rule.onNodeWithText(string(R.string.search_no_results_body)).assertIsDisplayed()
    }
}

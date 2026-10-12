package com.elkrrai.feather.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.ui.theme.FeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun string(id: Int) = resources.getString(id)

    private var summaryToggled: Boolean? = null
    private var snoozeChosen: Int? = null
    private var reliabilityOpened = 0

    private fun show(settings: AppSettings = AppSettings(), healthy: Boolean = true) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                SettingsContent(
                    settings = settings,
                    remindersHealthy = healthy,
                    onBack = {},
                    onOpenReliability = { reliabilityOpened++ },
                    onDefaultTimeChange = {},
                    onSnoozeMinutesChange = { snoozeChosen = it },
                    onDailySummaryChange = { summaryToggled = it },
                    onDailySummaryTimeChange = {},
                )
            }
        }
    }

    @Test
    fun theReminderSettingsAreListed() {
        show()

        rule.onNodeWithText(string(R.string.settings_default_time)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_snooze_length)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_daily_summary)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_language)).assertIsDisplayed()
    }

    @Test
    fun theSnoozeLengthShowsTheCurrentValueAndCanBeChanged() {
        show(AppSettings(snoozeMinutes = 10))

        rule.onNodeWithText(resources.getQuantityString(R.plurals.duration_minutes, 10, 10)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_snooze_length)).performClick()
        rule.onNodeWithText(resources.getQuantityString(R.plurals.duration_minutes, 30, 30)).performClick()

        assertEquals(30, snoozeChosen)
    }

    @Test
    fun tappingTheDailySummaryRowTogglesIt() {
        show(AppSettings(dailySummaryEnabled = false))

        rule.onNodeWithText(string(R.string.settings_daily_summary)).performClick()

        assertEquals(true, summaryToggled)
    }

    @Test
    fun theReliabilityRowShowsItsStatusAndOpensTheHelper() {
        show(healthy = false)

        rule.onNodeWithText(string(R.string.settings_reliability_attention)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.settings_reliability)).performClick()

        assertTrue(reliabilityOpened == 1)
    }
}

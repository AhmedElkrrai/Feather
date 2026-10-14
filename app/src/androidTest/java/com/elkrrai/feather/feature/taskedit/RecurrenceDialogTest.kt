package com.elkrrai.feather.feature.taskedit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.ui.theme.FeatherTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecurrenceDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun string(id: Int) = resources.getString(id)

    /** A Wednesday. */
    private val reference = LocalDate.of(2026, 10, 14)

    private var confirmed = false
    private var result: Recurrence? = null

    private fun show(initial: Recurrence?, reference: LocalDate = this.reference) {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                RecurrenceDialog(
                    initial = initial,
                    reference = reference,
                    onConfirm = {
                        confirmed = true
                        result = it
                    },
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun aNewWeeklyRuleStartsOnTheWeekdayOfTheDueDate() {
        show(initial = null)

        rule.onNodeWithText(string(R.string.repeat_freq_weekly)).performClick()
        rule.onNodeWithText(string(R.string.action_ok)).performClick()

        assertTrue(confirmed)
        assertEquals(Recurrence(Frequency.WEEKLY, weekdays = setOf(DayOfWeek.WEDNESDAY)), result)
    }

    @Test
    fun confirmingAnExistingRuleWithoutChangesKeepsItAsItWas() {
        // 30th of the month, although the reference date is the 28th.
        val existing = Recurrence(Frequency.MONTHLY, monthDay = 30)
        show(initial = existing, reference = LocalDate.of(2026, 2, 28))

        rule.onNodeWithText(string(R.string.action_ok)).performClick()

        assertEquals(existing, result)
    }

    @Test
    fun doesNotRepeatConfirmsWithNothing() {
        result = Recurrence(Frequency.DAILY)
        show(initial = Recurrence(Frequency.DAILY))

        rule.onNodeWithText(string(R.string.repeat_none)).performClick()
        rule.onNodeWithText(string(R.string.action_ok)).performClick()

        assertTrue(confirmed)
        assertEquals(null, result)
    }
}

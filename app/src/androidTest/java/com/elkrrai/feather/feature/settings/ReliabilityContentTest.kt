package com.elkrrai.feather.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.test.platform.app.InstrumentationRegistry
import com.elkrrai.feather.R
import com.elkrrai.feather.reminders.ReminderHealth
import com.elkrrai.feather.ui.theme.FeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReliabilityContentTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val calls = mutableListOf<String>()

    private fun show(health: ReminderHealth, manufacturer: String = "Google") {
        rule.setContent {
            FeatherTheme(dynamicColor = false) {
                ReliabilityContent(
                    health = health,
                    manufacturer = manufacturer,
                    snackbarHostState = SnackbarHostState(),
                    actions = ReliabilityActions(
                        onBack = { calls += "back" },
                        onFixNotifications = { calls += "notifications" },
                        onFixChannel = { calls += "channel" },
                        onFixExactAlarms = { calls += "exact" },
                        onFixBattery = { calls += "battery" },
                        onOpenAppSettings = { calls += "app" },
                        onOpenVendorGuide = { calls += "guide" },
                        onSendTest = { calls += "test" },
                    ),
                )
            }
        }
    }

    @Test
    fun whenEverythingIsFineThereAreNoFixButtons() {
        show(ReminderHealth())

        rule.onNodeWithText(string(R.string.reliability_all_good)).assertIsDisplayed()
        rule.onAllNodesWithText(string(R.string.reliability_fix)).assertCountEquals(0)
    }

    @Test
    fun blockedNotificationsOfferAFixThatReportsTheClick() {
        show(ReminderHealth(notificationsEnabled = false))

        rule.onNodeWithText(string(R.string.reliability_notifications_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.reliability_fix)).performClick()

        assertEquals(listOf("notifications"), calls)
    }

    @Test
    fun theBatteryCardOffersItsOwnFix() {
        show(ReminderHealth(batteryUnrestricted = false))

        rule.onNodeWithText(string(R.string.reliability_fix)).performClick()

        assertEquals(listOf("battery"), calls)
    }

    @Test
    fun restrictivePhoneMakersGetTheirOwnCardWithAGuide() {
        show(ReminderHealth(), manufacturer = "OnePlus")

        rule.onNodeWithText("OnePlus phones").assertIsDisplayed()
        rule.onNodeWithText(string(R.string.reliability_open_guide)).performClick()

        assertEquals(listOf("guide"), calls)
    }

    @Test
    fun otherPhoneMakersGetNoVendorCard() {
        show(ReminderHealth(), manufacturer = "Google")

        rule.onAllNodesWithText(string(R.string.reliability_open_guide)).assertCountEquals(0)
    }

    @Test
    fun theTestButtonReportsTheClick() {
        show(ReminderHealth())

        rule.onNodeWithText(string(R.string.reliability_test_button)).performClick()

        assertEquals(listOf("test"), calls)
    }
}

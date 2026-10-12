package com.elkrrai.feather.feature.settings

import androidx.lifecycle.ViewModel
import com.elkrrai.feather.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReliabilityViewModel @Inject constructor(
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /** Posts a test notification a few seconds from now, through the same alarm path as reminders. */
    fun sendTestReminder() {
        scheduler.scheduleTest(TEST_DELAY_MILLIS)
    }

    companion object {
        const val TEST_DELAY_MILLIS = 10_000L
    }
}

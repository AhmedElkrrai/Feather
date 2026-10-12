package com.elkrrai.feather.reminders

import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Re-schedules reminders whenever tasks (including their reminders, due dates and completion)
 * or the settings that affect trigger times change, so no other code has to remember to.
 */
@Singleton
class ReminderSyncer @Inject constructor(
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(tasks.observeTasks(), settings.settings) { _, _ -> }
                .debounce(DEBOUNCE_MILLIS)
                .collect { scheduler.reschedule() }
        }
    }

    private companion object {
        /** Several writes usually arrive together (task, tags, reminders); handle them once. */
        const val DEBOUNCE_MILLIS = 300L
    }
}

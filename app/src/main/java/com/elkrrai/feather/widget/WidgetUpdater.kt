package com.elkrrai.feather.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Redraws the Today widget whenever tasks change, so a task added, finished or moved shows up
 * without waiting for the system's own refresh. (The system also refreshes it every half hour,
 * which is what makes the list roll over at midnight.)
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            tasks.observeTasks()
                .debounce(DEBOUNCE_MILLIS)
                .collect { TodayWidget().updateAll(context) }
        }
    }

    private companion object {
        /** Several writes usually arrive together; redraw once. */
        const val DEBOUNCE_MILLIS = 500L
    }
}

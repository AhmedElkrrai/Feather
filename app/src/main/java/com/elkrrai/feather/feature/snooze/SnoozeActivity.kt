package com.elkrrai.feather.feature.snooze

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.ui.theme.FeatherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A see-through screen that only shows the snooze dialog on top of whatever the user was doing,
 * opened from the "More…" button of a reminder notification.
 */
@AndroidEntryPoint
class SnoozeActivity : AppCompatActivity() {

    private val viewModel: SnoozeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatherTheme {
                val finished by viewModel.finished.collectAsStateWithLifecycle()
                LaunchedEffect(finished) {
                    if (finished) finish()
                }
                SnoozeDialog(
                    onSnoozeMinutes = viewModel::snoozeForMinutes,
                    onTomorrowMorning = viewModel::snoozeUntilTomorrowMorning,
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.elkrrai.feather.extra.SNOOZE_TASK_ID"

        fun intent(context: Context, taskId: String): Intent =
            Intent(context, SnoozeActivity::class.java).putExtra(EXTRA_TASK_ID, taskId)
    }
}

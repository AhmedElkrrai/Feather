package com.elkrrai.feather

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elkrrai.feather.ui.navigation.FeatherNavHost
import com.elkrrai.feather.ui.theme.FeatherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** A task a reminder notification asked us to open; cleared once the editor is shown. */
    private var openTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch: after a rotation the same intent must not reopen the task.
        if (savedInstanceState == null) openTaskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID)
        setContent {
            FeatherTheme {
                // Each screen owns its Scaffold (top bar, FAB, insets); this only paints the background.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FeatherNavHost(
                        openTaskId = openTaskId,
                        onOpenTaskHandled = {
                            openTaskId = null
                            intent.removeExtra(EXTRA_OPEN_TASK_ID)
                        },
                    )
                }
            }
        }
    }

    /** The app is already open (singleTop) and a notification was tapped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID)
    }

    companion object {
        /** Set by a reminder notification: the id of the task to open in the editor. */
        const val EXTRA_OPEN_TASK_ID = "com.elkrrai.feather.extra.OPEN_TASK_ID"
    }
}

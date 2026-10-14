package com.elkrrai.feather.feature.quickadd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.feature.tasklist.QuickAddSheet
import com.elkrrai.feather.ui.theme.FeatherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A see-through screen that shows only the quick-add sheet on top of whatever the user was doing.
 * It is opened by the share sheet (text from another app), the Quick Settings tile and the
 * launcher shortcut, adds the task to the Inbox (or the list named with `@`) and closes.
 */
@AndroidEntryPoint
class QuickAddActivity : AppCompatActivity() {

    private val viewModel: QuickAddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = sharedText(intent)
        setContent {
            FeatherTheme {
                val lists by viewModel.lists.collectAsStateWithLifecycle()
                var adding by remember { mutableStateOf(false) }
                QuickAddSheet(
                    lists = lists,
                    initialText = shared,
                    // Closing the sheet after adding must not end the activity before the save is done.
                    onDismiss = { if (!adding) finish() },
                    onAdd = { parsed, listId ->
                        adding = true
                        viewModel.add(parsed, listId) {
                            Toast.makeText(this@QuickAddActivity, R.string.quick_add_done, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                )
            }
        }
    }

    companion object {
        /** An intent that opens quick add on its own (tile and shortcut). */
        fun intent(context: Context): Intent = Intent(context, QuickAddActivity::class.java)

        /**
         * The text another app shared, on one line (a share of several lines still makes one
         * task), or an empty string when this was not a share.
         */
        internal fun sharedText(intent: Intent?): String {
            if (intent?.action != Intent.ACTION_SEND) return ""
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            return text.orEmpty().replace(WHITESPACE, " ").trim()
        }

        private val WHITESPACE = Regex("\\s+")
    }
}

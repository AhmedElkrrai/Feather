package com.elkrrai.feather

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.security.AppLockSession
import com.elkrrai.feather.security.BiometricGate
import com.elkrrai.feather.security.LockScreen
import com.elkrrai.feather.ui.navigation.FeatherNavHost
import com.elkrrai.feather.ui.theme.FeatherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** A task a reminder notification asked us to open; cleared once the editor is shown. */
    private var openTaskId by mutableStateOf<String?>(null)

    /** A task whose focus timer a focus notification asked us to open. */
    private var openFocusTaskId by mutableStateOf<String?>(null)

    /** The lock screen covers the app while this is true. */
    private var locked by mutableStateOf(false)

    /** The "Lock the app" setting; kept up to date while the activity lives. */
    private var lockSetting = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must come first: it swaps the launch theme for the normal one once the first frame is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // One small read of a cached settings file, so the lock is decided before the first frame.
        lockSetting = runBlocking { settingsRepository.current() }.appLock
        locked = lockRequired() && !AppLockSession.unlocked
        hideFromRecents()
        lifecycleScope.launch {
            // The first value is the one read above; only a change made while the app is open counts here.
            settingsRepository.settings.map { it.appLock }.distinctUntilChanged().drop(1).collect { on ->
                lockSetting = on
                hideFromRecents()
                // Switched in Settings, where the person has just proved who they are.
                if (on) AppLockSession.unlocked = true else locked = false
            }
        }
        // Only on a fresh launch: after a rotation the same intent must not reopen the task.
        if (savedInstanceState == null) {
            openTaskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID)
            openFocusTaskId = intent.getStringExtra(EXTRA_OPEN_FOCUS_TASK_ID)
        }
        setContent {
            FeatherTheme {
                // Each screen owns its Scaffold (top bar, FAB, insets); this only paints the background.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // The app stays composed under the lock screen, so what was open (an unsaved
                        // task, say) is still there after unlocking; it is hidden from touch and TalkBack.
                        Box(if (locked) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
                            FeatherNavHost(
                                openTaskId = openTaskId,
                                onOpenTaskHandled = {
                                    openTaskId = null
                                    intent.removeExtra(EXTRA_OPEN_TASK_ID)
                                },
                                openFocusTaskId = openFocusTaskId,
                                onOpenFocusHandled = {
                                    openFocusTaskId = null
                                    intent.removeExtra(EXTRA_OPEN_FOCUS_TASK_ID)
                                },
                            )
                        }
                        if (locked) LockScreen(onUnlock = ::promptUnlock)
                    }
                }
            }
        }
    }

    /** Coming back after more than a short while asks again. */
    override fun onStart() {
        super.onStart()
        if (AppLockSession.unlocked && AppLockSession.awayTooLong(SystemClock.elapsedRealtime())) {
            AppLockSession.unlocked = false
        }
        if (!AppLockSession.unlocked && lockRequired()) locked = true
    }

    override fun onStop() {
        super.onStop()
        AppLockSession.leftAt = SystemClock.elapsedRealtime()
    }

    /** The app is already open (singleTop) and a notification was tapped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskId = intent.getStringExtra(EXTRA_OPEN_TASK_ID)
        openFocusTaskId = intent.getStringExtra(EXTRA_OPEN_FOCUS_TASK_ID)
    }

    /** True when the lock is switched on and the phone can check who is there (a fingerprint, a face or a screen lock). */
    private fun lockRequired(): Boolean = lockSetting && BiometricGate.canAuthenticate(this)

    private fun promptUnlock() {
        BiometricGate.authenticate(
            activity = this,
            title = getString(R.string.lock_prompt_title),
            subtitle = getString(R.string.lock_prompt_subtitle),
            onSuccess = {
                AppLockSession.unlocked = true
                locked = false
            },
        )
    }

    /** While the lock is on, the picture of the app in the recents list is blank (Android 13 and up). */
    private fun hideFromRecents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!lockRequired())
        }
    }

    companion object {
        /** Set by a reminder notification: the id of the task to open in the editor. */
        const val EXTRA_OPEN_TASK_ID = "com.elkrrai.feather.extra.OPEN_TASK_ID"

        /** Set by a focus notification: the id of the task whose focus timer to open. */
        const val EXTRA_OPEN_FOCUS_TASK_ID = "com.elkrrai.feather.extra.OPEN_FOCUS_TASK_ID"
    }
}

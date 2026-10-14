package com.elkrrai.feather.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the phone's animation scale is zero ("remove animations"). */
@Composable
fun rememberSystemMotionOff(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Whether animation should be cut back: the phone's animation scale is zero, or Reduce motion is
 * switched on in Settings. Every animation has a quiet version for this case (an instant change or
 * a short fade). It does not decide about celebrations; those have their own switch.
 */
@Composable
fun rememberReduceMotion(): Boolean = DisplayPrefs.reduceMotion || rememberSystemMotionOff()

package com.elkrrai.feather.ui

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Feathers in a burst on a phone that is short of memory or battery, instead of the usual number. */
const val LOW_END_FEATHERS = 7

/**
 * Whether the phone is a low-memory device or in battery saver. The app then cuts back the things
 * that are only for delight: the bird's ambient loop, the sky's cross-fade and the size of a
 * confetti burst. Nothing that carries meaning changes.
 */
@Composable
fun rememberLowEndMode(): Boolean {
    val context = LocalContext.current
    val lowRam = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    }
    val power = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var batterySaver by remember(power) { mutableStateOf(power?.isPowerSaveMode == true) }
    DisposableEffect(context, power) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                batterySaver = power?.isPowerSaveMode == true
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return lowRam || batterySaver
}

package com.elkrrai.feather.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.elkrrai.feather.reminders.ReminderHealth
import com.elkrrai.feather.reminders.readReminderHealth

/**
 * The current [ReminderHealth], re-read every time the screen comes back to the foreground, so
 * it updates after the user returns from a system settings page.
 */
@Composable
fun rememberReminderHealth(): State<ReminderHealth> {
    val context = LocalContext.current
    val health = remember { mutableStateOf(readReminderHealth(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        health.value = readReminderHealth(context)
    }
    return health
}

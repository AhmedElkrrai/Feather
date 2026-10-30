package com.elkrrai.feather.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Lets the receiver reach the controller (an entry point, like the reminder receivers use). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusEntryPoint {
    fun focusController(): FocusController
}

/**
 * The focus session's alarm going off, the "Stop" button of its notification, and the system events
 * that clear alarms (a reboot or an update), after which a running session arms its alarm again.
 */
class FocusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val controller = EntryPointAccessors
            .fromApplication(context.applicationContext, FocusEntryPoint::class.java)
            .focusController()
        when (intent.action) {
            ACTION_END -> controller.timeIsUp()
            ACTION_STOP -> controller.stop()
            else -> controller.restore() // boot or package replaced
        }
    }

    companion object {
        const val ACTION_END = "com.elkrrai.feather.action.FOCUS_END"
        const val ACTION_STOP = "com.elkrrai.feather.action.FOCUS_STOP"
    }
}

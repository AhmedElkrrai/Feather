package com.elkrrai.feather.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.elkrrai.feather.domain.focus.FocusPhase
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.domain.focus.focusSessionEnded
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Starts, pauses and ends the focus session, and keeps its alarm and notification in step. The
 * session itself lives in [FocusSessionStore]; the screen only reads it. There is one session at
 * a time: starting another replaces it.
 *
 * The time is up when the alarm fires (or when the screen sees it, whichever is first); the session
 * then stays in the store, run out, until the person takes a break, starts another round or says
 * they are done.
 */
@Singleton
class FocusController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: FocusSessionStore,
    private val notifier: FocusNotifier,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    val session: StateFlow<FocusSession?> = store.session

    /** The length of the last focus stretch chosen. */
    var lastMinutes: Int
        get() = store.lastMinutes
        set(value) {
            store.lastMinutes = value
        }

    /** Starts a stretch of [minutes] on the task, replacing any session there was. */
    fun start(taskId: String, taskTitle: String, phase: FocusPhase, minutes: Int) {
        if (phase == FocusPhase.FOCUS) store.lastMinutes = minutes
        val started = FocusSession.start(taskId, taskTitle, phase, minutes, now())
        notifier.cancelAll()
        store.save(started)
        arm(started)
        notifier.showRunning(started)
    }

    fun pause() {
        val current = store.session.value ?: return
        if (current.isPaused) return
        val paused = current.pause(now())
        store.save(paused)
        disarm()
        notifier.showPaused(paused)
    }

    fun resume() {
        val current = store.session.value ?: return
        if (!current.isPaused) return
        val resumed = current.resume(now())
        store.save(resumed)
        arm(resumed)
        notifier.showRunning(resumed)
    }

    /** Ends the session without ceremony: no alarm, no notification. */
    fun stop() {
        store.save(null)
        disarm()
        notifier.cancelAll()
    }

    /**
     * The time may have run out: from the alarm, or because the screen saw it. Shows the "time is
     * up" notification once; a session that is still running just gets its alarm armed again.
     */
    fun timeIsUp() {
        val current = store.session.value ?: return
        if (current.isPaused) return
        if (!current.isOver(now())) {
            arm(current)
            return
        }
        disarm()
        notifier.cancelRunning()
        if (!alerted) {
            alerted = true
            notifier.showOver(current)
        }
    }

    /**
     * Ends the session when its task is finished, deleted or (a repeating one) moved to a later
     * day, however that happens: the ring, a notification button, a widget, the editor.
     */
    fun watch(tasks: TaskRepository, scope: CoroutineScope) {
        scope.launch {
            combine(store.session.map { it?.taskId }.distinctUntilChanged(), tasks.observeTasks()) { id, all ->
                id?.let { wanted -> all.firstOrNull { it.id == wanted } to true }
            }.collect { found ->
                if (found != null && focusSessionEnded(found.first, LocalDate.now())) stop()
            }
        }
    }

    /** After a reboot or an update: the alarm is gone, so arm it again (or say the time is up). */
    fun restore() {
        val current = store.session.value ?: return
        when {
            current.isPaused -> notifier.showPaused(current)
            current.isOver(now()) -> timeIsUp()
            else -> {
                arm(current)
                notifier.showRunning(current)
            }
        }
    }

    /** True once the "time is up" notification was shown for the session that is in the store. */
    private var alerted = false

    private fun arm(session: FocusSession) {
        alerted = false
        val endsAt = session.endsAt ?: return
        setAlarm(endsAt, alarmIntent())
    }

    private fun disarm() {
        alarmManager.cancel(alarmIntent())
    }

    /** Exact when the person allowed "Alarms & reminders", like reminders; otherwise a little late at worst. */
    private fun setAlarm(triggerAtMillis: Long, intent: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
                return
            } catch (e: SecurityException) {
                // The permission was revoked between the check and the call: fall through.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, FocusReceiver::class.java).setAction(FocusReceiver.ACTION_END),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val REQUEST_ALARM = 22
    }
}

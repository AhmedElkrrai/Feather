package com.elkrrai.feather.focus

import android.content.Context
import com.elkrrai.feather.domain.focus.DEFAULT_FOCUS_MINUTES
import com.elkrrai.feather.domain.focus.FocusPhase
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.domain.focus.clampFocusMinutes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one focus session that is running (or has just run out and is waiting to be dismissed), kept
 * in a small preferences file so it survives the app being closed. It is outside Settings and
 * outside backups: a session is a few minutes long and means nothing on another phone.
 */
@Singleton
class FocusSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("feather_focus", Context.MODE_PRIVATE)

    private val current = MutableStateFlow(read())

    /** The session, or null when there is none. */
    val session: StateFlow<FocusSession?> = current

    /** The length of the last focus stretch the person chose, to start from next time. */
    var lastMinutes: Int
        get() = clampFocusMinutes(prefs.getInt(KEY_MINUTES, DEFAULT_FOCUS_MINUTES))
        set(value) {
            prefs.edit().putInt(KEY_MINUTES, clampFocusMinutes(value)).apply()
        }

    fun save(session: FocusSession?) {
        prefs.edit().apply {
            if (session == null) {
                remove(KEY_TASK)
            } else {
                putString(KEY_TASK, session.taskId)
                putString(KEY_TITLE, session.taskTitle)
                putInt(KEY_PHASE, session.phase.ordinal)
                putLong(KEY_TOTAL, session.totalMillis)
                putLong(KEY_ENDS_AT, session.endsAt ?: NOT_RUNNING)
                putLong(KEY_PAUSED_LEFT, session.pausedLeft)
            }
        }.apply()
        current.value = session
    }

    private fun read(): FocusSession? {
        val taskId = prefs.getString(KEY_TASK, null) ?: return null
        val endsAt = prefs.getLong(KEY_ENDS_AT, NOT_RUNNING)
        return FocusSession(
            taskId = taskId,
            taskTitle = prefs.getString(KEY_TITLE, "").orEmpty(),
            phase = FocusPhase.entries.getOrElse(prefs.getInt(KEY_PHASE, 0)) { FocusPhase.FOCUS },
            totalMillis = prefs.getLong(KEY_TOTAL, 0L),
            endsAt = endsAt.takeIf { it != NOT_RUNNING },
            pausedLeft = prefs.getLong(KEY_PAUSED_LEFT, 0L),
        )
    }

    private companion object {
        const val KEY_TASK = "task_id"
        const val KEY_TITLE = "task_title"
        const val KEY_PHASE = "phase"
        const val KEY_TOTAL = "total_millis"
        const val KEY_ENDS_AT = "ends_at"
        const val KEY_PAUSED_LEFT = "paused_left"
        const val KEY_MINUTES = "last_minutes"
        const val NOT_RUNNING = -1L
    }
}

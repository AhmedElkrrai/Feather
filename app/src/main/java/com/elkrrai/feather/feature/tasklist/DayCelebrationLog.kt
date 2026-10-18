package com.elkrrai.feather.feature.tasklist

import android.content.Context
import java.time.LocalDate

/** Remembers the day the "all done" celebration last played, so it plays at most once a calendar day. */
internal class DayCelebrationLog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("feather_ui_hints", Context.MODE_PRIVATE)

    fun celebratedOn(day: LocalDate): Boolean = prefs.getString(KEY, null) == day.toString()

    fun mark(day: LocalDate) {
        prefs.edit().putString(KEY, day.toString()).apply()
    }

    private companion object {
        const val KEY = "all_done_celebrated_on"
    }
}

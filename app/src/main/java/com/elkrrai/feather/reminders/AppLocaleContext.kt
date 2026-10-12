package com.elkrrai.feather.reminders

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * A context whose resources follow the language chosen in the app. Notifications are built
 * outside any activity, and before Android 13 the application context does not know about the
 * per-app language, so strings would otherwise come out in the system language.
 */
fun Context.withAppLocale(): Context {
    val chosen = AppCompatDelegate.getApplicationLocales()
    if (chosen.isEmpty) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()))
    return createConfigurationContext(configuration)
}

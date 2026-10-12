package com.elkrrai.feather.reminders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** System screens the reliability helper sends the user to. */
object SettingsIntents {

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun notifications(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun reminderChannel(context: Context): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderChannels.REMINDERS)

    /** The "Alarms & reminders" switch for this app (Android 12+); app details before that. */
    fun exactAlarms(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            appDetails(context)
        }

    /** The list of apps with their battery optimization state; the user picks Feather there. */
    fun batteryOptimization(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun vendorGuide(manufacturer: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(vendorGuideUrl(manufacturer)))

    /** dontkillmyapp.com has a page per major phone maker; others get the front page. */
    fun vendorGuideUrl(manufacturer: String): String {
        val slug = manufacturer.lowercase()
        return if (slug in GUIDE_PAGES) "https://dontkillmyapp.com/$slug" else "https://dontkillmyapp.com/"
    }

    /** Phone makers known for extra background and auto-launch restrictions on top of Android's. */
    fun isRestrictiveVendor(manufacturer: String): Boolean = manufacturer.lowercase() in RESTRICTIVE_VENDORS

    private val GUIDE_PAGES = setOf("oneplus", "oppo", "xiaomi", "huawei", "samsung", "vivo")

    private val RESTRICTIVE_VENDORS = GUIDE_PAGES + setOf("realme", "honor", "tecno", "infinix", "itel", "meizu", "asus")
}

/** Starts [intent], or [fallback] if this phone has no screen for it. Never throws. */
fun Context.startActivitySafely(intent: Intent, fallback: Intent? = null) {
    val launch = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(launch)
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) {
            try {
                startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (inner: ActivityNotFoundException) {
                // Nothing can handle either intent; there is nothing more to do.
            }
        }
    }
}

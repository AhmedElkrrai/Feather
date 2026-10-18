package com.elkrrai.feather.ui.today

import java.time.LocalTime

/** The two small cards on Today: a look at the day in the morning and a wind-down after 7 PM. */
enum class DayCardKind { Morning, Evening }

/**
 * What a card says. [left] and [done] are today's open and finished tasks, [waiting] the open ones
 * whose day has already passed, and [firstTitle] the task that comes first.
 */
data class DayCard(
    val kind: DayCardKind,
    val left: Int,
    val done: Int,
    val waiting: Int,
    val firstTitle: String?,
)

/** Which cards are switched on in Settings, and which were already put away today. */
data class DayCardSwitches(
    val morning: Boolean = true,
    val evening: Boolean = true,
    val morningDismissed: Boolean = false,
    val eveningDismissed: Boolean = false,
)

/** The morning card is for 05:00 to 11:59, the evening card from 19:00 to midnight. */
internal const val MORNING_FROM = 5
internal const val MORNING_UNTIL = 12
internal const val EVENING_FROM = 19

/**
 * The card to show now, or null. The morning card needs something planned for the day; the
 * evening card needs the day to have had something in it. Each shows once a day: once put away it
 * stays away until tomorrow.
 */
fun dayCardFor(
    time: LocalTime,
    status: TodayStatus,
    waiting: Int,
    firstTitle: String?,
    switches: DayCardSwitches,
): DayCard? = when {
    time.hour in MORNING_FROM until MORNING_UNTIL ->
        if (switches.morning && !switches.morningDismissed && status.left > 0) {
            DayCard(DayCardKind.Morning, status.left, status.done, waiting, firstTitle)
        } else {
            null
        }

    time.hour >= EVENING_FROM ->
        if (switches.evening && !switches.eveningDismissed && status.planned > 0) {
            DayCard(DayCardKind.Evening, status.left, status.done, waiting, firstTitle)
        } else {
            null
        }

    else -> null
}

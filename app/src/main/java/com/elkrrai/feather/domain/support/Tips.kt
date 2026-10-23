package com.elkrrai.feather.domain.support

/**
 * One amount a person can leave as a tip. [id] is the in-app product's id in Play Console and
 * [price] is already formatted by Google Play in the person's own currency.
 */
data class TipOffer(val id: String, val price: String)

/** How a tip ended. A tip never unlocks anything; this only decides what the thank-you says. */
enum class TipResult {
    /** The tip went through. */
    THANKS,

    /** The payment is waiting for approval (a slow payment method); it counts once it clears. */
    PENDING,

    /** The person closed the payment dialog. Nothing to say. */
    CANCELLED,

    /** Something went wrong; the person was not charged. */
    FAILED,
}

/** The three tip products, smallest first. Their prices are set in Play Console, not in the app. */
val TipProductIds: List<String> = listOf("tip_small", "tip_medium", "tip_large")

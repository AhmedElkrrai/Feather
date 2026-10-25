package com.elkrrai.feather.domain.nest

/**
 * Which garment of each kind the owl wears: the number of the choice (the first is 0), or [NONE].
 * A garment only shows once its item has been earned.
 */
data class NestOutfit(
    val scarf: Int = 0,
    val glasses: Int = 0,
    val hat: Int = 0,
) {
    companion object {
        const val NONE = -1

        /** How many choices there are of each. */
        const val SCARVES = 6
        const val GLASSES = 2
        const val HATS = 4
    }
}

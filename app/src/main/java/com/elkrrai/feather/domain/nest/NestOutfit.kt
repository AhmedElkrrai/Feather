package com.elkrrai.feather.domain.nest

/**
 * Which of each kind of thing the owl wears or the nest holds: the number of the choice (the first
 * is 0), or [NONE]. An item only shows once it has been earned.
 */
data class NestOutfit(
    val scarf: Int = 0,
    val glasses: Int = 0,
    val hat: Int = 0,
    /** The flower pot beside the nest. */
    val pot: Int = 0,
) {
    companion object {
        const val NONE = -1

        /** How many choices there are of each. */
        const val SCARVES = 6
        const val GLASSES = 2
        const val HATS = 4
        const val POTS = 4
    }
}

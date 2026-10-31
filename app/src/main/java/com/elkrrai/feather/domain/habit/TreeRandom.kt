package com.elkrrai.feather.domain.habit

/**
 * The random numbers of a tree. It is the same generator as the preview script (art/tree_preview/tree_preview.py,
 * the class Rng), step for step, so a seed gives the same tree in the preview and in the app. Do not change it
 * without changing the script the same way.
 */
class TreeRandom(seed: Int) {
    private var state: Long = seed.toLong() and MASK

    /** The next 32 bits, as a number from 0 up to 4294967295. */
    fun nextUnsigned(): Long {
        state = (state + 0x6D2B79F5L) and MASK
        var t = state
        t = ((t xor (t ushr 15)) * (t or 1L)) and MASK
        t = t xor ((t + (((t xor (t ushr 7)) * (t or 61L)) and MASK)) and MASK)
        return (t xor (t ushr 14)) and MASK
    }

    /** A number from 0 up to, not including, 1. */
    fun nextDouble(): Double = nextUnsigned() / 4294967296.0

    fun nextFloat(): Float = nextDouble().toFloat()

    fun nextBits30(): Int = (nextUnsigned() ushr 2).toInt()

    /** One of [items], each as likely as its place in the list. */
    fun pick(items: IntArray): Int = items[(nextDouble() * items.size).toInt()]

    private companion object {
        const val MASK = 0xFFFFFFFFL
    }
}

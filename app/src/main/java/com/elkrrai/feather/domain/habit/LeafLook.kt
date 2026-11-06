package com.elkrrai.feather.domain.habit

import kotlin.random.Random

/** How many looks the weekly leaf has (see `LeafStyle`, whose order is these codes). */
const val LEAF_LOOKS = 6

/**
 * The look for the next habit's leaf, given the looks the habits have already: a random one of those used the fewest times. So every look
 * shows once before any shows twice: the first habit gets any, the second any of those left, and so on to the last one left, and then the
 * deal starts over. A habit without a look yet (a negative code) is not counted.
 */
fun nextLeaf(used: List<Int>, random: Random = Random.Default): Int {
    val counts = IntArray(LEAF_LOOKS) { look -> used.count { it == look } }
    val fewest = counts.min()
    val candidates = counts.indices.filter { counts[it] == fewest }
    return candidates[random.nextInt(candidates.size)]
}

package com.elkrrai.feather.domain.model

/**
 * A task that counts toward a [target], one [step] at a time ("100 pushups, 25 a set"): [done] is
 * how far it has got. Only the number is kept. The count never goes below 0 or past the target.
 */
data class Counter(val target: Int, val step: Int, val done: Int = 0) {
    init {
        require(target in 1..MAX_TARGET) { "Counter target out of range" }
        require(step in 1..target) { "Counter step out of range" }
        require(done in 0..target) { "Counter count out of range" }
    }

    /** True once the target is reached. */
    val isFull: Boolean get() = done >= target

    /** How far along it is, 0 to 1. */
    val fraction: Float get() = done.toFloat() / target

    /** The count after adding [amount] (it may be negative), kept between 0 and the target. */
    fun plus(amount: Int): Counter = copy(done = (done.toLong() + amount).coerceIn(0L, target.toLong()).toInt())

    /** The same counter back at 0, for the next time a repeating task comes round. */
    fun reset(): Counter = copy(done = 0)

    companion object {
        const val MAX_TARGET = 99_999

        /**
         * A counter from stored numbers, or null when there is none. Numbers that do not fit
         * (an edited file, an older bug) are brought into range instead of crashing.
         */
        fun of(target: Int?, step: Int?, done: Int): Counter? {
            if (target == null || target < 1) return null
            val fixedTarget = target.coerceAtMost(MAX_TARGET)
            return Counter(
                target = fixedTarget,
                step = (step ?: 1).coerceIn(1, fixedTarget),
                done = done.coerceIn(0, fixedTarget),
            )
        }
    }
}

package com.elkrrai.feather.domain.nest

/**
 * The things that can come to the bird's nest, each earned by an ordinary milestone. They are only
 * decoration: permanent once earned, never time-limited, never paid for, never random.
 */
enum class NestItem(val id: String) {
    /** A scarf, after the tenth finished task. */
    SCARF("scarf"),

    /** Fairy lights, after the first backup. */
    FAIRY_LIGHTS("fairy_lights"),

    /** Bunting, after a repeating task has been kept up for four weeks. */
    BUNTING("bunting"),

    /** A lantern, after ten reminders have been set. */
    LANTERN("lantern"),

    /** A patch of moss, after three lists of your own. */
    MOSS("moss"),

    /** Tiny glasses, after the 25th finished task. */
    GLASSES("glasses"),

    /** A flower pot, after the first task added by voice. */
    FLOWER_POT("flower_pot"),

    /** A party hat, after the 100th finished task. */
    PARTY_HAT("party_hat");

    companion object {
        fun fromId(id: String): NestItem? = entries.firstOrNull { it.id == id }
    }
}

const val SCARF_AT = 10
const val GLASSES_AT = 25
const val PARTY_HAT_AT = 100
const val LANTERN_REMINDERS = 10
const val MOSS_LISTS = 3
const val BUNTING_WEEKS = 4

/** What the person has done so far, as far as the nest cares. */
data class NestFacts(
    val tasksFinished: Int = 0,
    val backedUp: Boolean = false,
    val keptRepeaterFourWeeks: Boolean = false,
    val remindersSet: Int = 0,
    val listsMade: Int = 0,
    val voiceUsed: Boolean = false,
)

/** Whether [fact]s are enough to have earned this item. */
fun NestItem.isEarnedBy(fact: NestFacts): Boolean = when (this) {
    NestItem.SCARF -> fact.tasksFinished >= SCARF_AT
    NestItem.FAIRY_LIGHTS -> fact.backedUp
    NestItem.BUNTING -> fact.keptRepeaterFourWeeks
    NestItem.LANTERN -> fact.remindersSet >= LANTERN_REMINDERS
    NestItem.MOSS -> fact.listsMade >= MOSS_LISTS
    NestItem.GLASSES -> fact.tasksFinished >= GLASSES_AT
    NestItem.FLOWER_POT -> fact.voiceUsed
    NestItem.PARTY_HAT -> fact.tasksFinished >= PARTY_HAT_AT
}

/** Everything [fact]s have earned so far. */
fun earnedNestItems(fact: NestFacts): Set<NestItem> = NestItem.entries.filter { it.isEarnedBy(fact) }.toSet()

/** How far along a milestone is, as (done, needed), for the items that count something; null for the rest. */
fun NestItem.progress(fact: NestFacts): Pair<Int, Int>? = when (this) {
    NestItem.SCARF -> fact.tasksFinished.coerceAtMost(SCARF_AT) to SCARF_AT
    NestItem.LANTERN -> fact.remindersSet.coerceAtMost(LANTERN_REMINDERS) to LANTERN_REMINDERS
    NestItem.MOSS -> fact.listsMade.coerceAtMost(MOSS_LISTS) to MOSS_LISTS
    NestItem.GLASSES -> fact.tasksFinished.coerceAtMost(GLASSES_AT) to GLASSES_AT
    NestItem.PARTY_HAT -> fact.tasksFinished.coerceAtMost(PARTY_HAT_AT) to PARTY_HAT_AT
    NestItem.FAIRY_LIGHTS, NestItem.BUNTING, NestItem.FLOWER_POT -> null
}

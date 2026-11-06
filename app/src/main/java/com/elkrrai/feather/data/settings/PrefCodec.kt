package com.elkrrai.feather.data.settings

/** How a preference is written into a backup file: a letter for its type, a colon, then its value as text. */
internal object PrefCodec {
    fun encode(value: Any): String? = when (value) {
        is Boolean -> "b:$value"
        is Int -> "i:$value"
        is String -> "s:$value"
        is Set<*> -> "S:" + value.filterIsInstance<String>().sorted().joinToString("\n")
        else -> null
    }

    /** The value, or null when the text is not one this app wrote. */
    fun decode(text: String): Any? {
        if (text.length < 2 || text[1] != ':') return null
        val body = text.substring(2)
        return when (text[0]) {
            'b' -> when (body) {
                "true" -> true
                "false" -> false
                else -> null
            }
            'i' -> body.toIntOrNull()
            's' -> body
            'S' -> if (body.isEmpty()) emptySet<String>() else body.split('\n').toSet()
            else -> null
        }
    }
}

private const val HABIT_TREES_SEEN = "habit_trees_seen"
private const val HABIT_SWIPE_HINTS_SHOWN = "habit_swipe_hints_shown"

/** Things that were reached once stay reached: restoring a backup never turns them off. */
private val ProgressFlags = setOf("voice_used", "backed_up", "welcome_seen")

/**
 * What a restore puts in the preference [name] when this phone already has a value ([current]). What was earned
 * or counted is combined with what is here (the nest items, the rest days, the seen trees, the milestones); a
 * choice (a theme, a time) is replaced by the one in the backup.
 */
internal fun mergedPreference(name: String, current: Any?, incoming: Any): Any {
    if (current == null) return incoming
    return when {
        current is Set<*> && incoming is Set<*> -> {
            val here = current.filterIsInstance<String>()
            val there = incoming.filterIsInstance<String>()
            if (name == HABIT_TREES_SEEN) mostWeeksPerHabit(here + there) else (here + there).toSet()
        }
        name in ProgressFlags -> current == true || incoming == true
        name == HABIT_SWIPE_HINTS_SHOWN && current is Int && incoming is Int -> maxOf(current, incoming)
        else -> incoming
    }
}

/** The entries are "habit id|met weeks"; for a habit seen twice, the larger number of weeks stays. */
private fun mostWeeksPerHabit(entries: List<String>): Set<String> {
    val best = LinkedHashMap<String, Int>()
    for (entry in entries) {
        val at = entry.lastIndexOf('|')
        if (at <= 0) continue
        val weeks = entry.substring(at + 1).toIntOrNull() ?: continue
        val id = entry.substring(0, at)
        best[id] = maxOf(best[id] ?: 0, weeks)
    }
    return best.map { (id, weeks) -> "$id|$weeks" }.toSet()
}

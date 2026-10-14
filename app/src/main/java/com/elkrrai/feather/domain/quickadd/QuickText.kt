package com.elkrrai.feather.domain.quickadd

/**
 * The text being parsed, in two views: the user's original text (which the title is made from)
 * and a normalized copy that the rules match against. Normalization lower-cases Latin letters,
 * turns Arabic-Indic and Persian digits into ASCII, unifies alef, ya and ta marbuta spellings
 * and drops Arabic diacritics and tatweel, so one pattern covers every spelling.
 *
 * Rules take the words they recognize out with [consume]; whatever is left becomes the [title].
 * Consumed words are blanked in [text] so no later rule sees them.
 */
internal class QuickText(val original: String) {

    private val normalized = StringBuilder()

    /** For each character of [normalized], where it came from in [original]. */
    private val sourceIndex = ArrayList<Int>()

    init {
        original.forEachIndexed { index, char ->
            val mapped = normalizeChar(char)
            if (mapped != null) {
                normalized.append(mapped)
                sourceIndex += index
            }
        }
    }

    private val work = StringBuilder(normalized)
    private val consumed = BooleanArray(normalized.length)

    /** The normalized text with everything consumed so far blanked out. */
    val text: String get() = work.toString()

    fun consume(range: IntRange) {
        for (i in range) {
            work.setCharAt(i, ' ')
            consumed[i] = true
        }
    }

    /** The user's own spelling of the characters in [range] of [text]. */
    fun originalSlice(range: IntRange): String =
        original.substring(sourceIndex[range.first], sourceIndex[range.last] + 1)

    /**
     * The original text without the consumed words, with spaces tidied and separators left
     * dangling at the ends ("Call mom," after taking out "tomorrow") removed.
     */
    fun title(): String {
        val keep = BooleanArray(original.length) { true }
        val mapped = BooleanArray(original.length)
        sourceIndex.forEachIndexed { i, source ->
            mapped[source] = true
            if (consumed[i]) keep[source] = false
        }
        // A dropped diacritic stays with the letter it belongs to.
        for (j in original.indices) {
            if (!mapped[j]) keep[j] = j > 0 && keep[j - 1]
        }
        val kept = StringBuilder()
        for (j in original.indices) if (keep[j]) kept.append(original[j])
        return kept.toString()
            .replace(WHITESPACE, " ")
            .trim { it.isWhitespace() || it in DANGLING }
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private const val DANGLING = ",;:.-–—،"

        /** The normalized form of [value], for comparing names the same way the rules do. */
        fun normalize(value: String): String = buildString {
            for (char in value) normalizeChar(char)?.let { append(it) }
        }

        private fun normalizeChar(char: Char): Char? = when (char) {
            in 'ً'..'ٟ', 'ٰ', 'ـ' -> null // diacritics, dagger alef, tatweel
            'آ', 'أ', 'إ', 'ٱ' -> 'ا' // alef variants -> alef
            'ى' -> 'ي' // alef maqsura -> ya
            'ة' -> 'ه' // ta marbuta -> ha
            in '٠'..'٩' -> '0' + (char - '٠') // Arabic-Indic digits
            in '۰'..'۹' -> '0' + (char - '۰') // Persian digits
            '،' -> ','
            '؟' -> '?'
            else -> char.lowercaseChar()
        }
    }
}

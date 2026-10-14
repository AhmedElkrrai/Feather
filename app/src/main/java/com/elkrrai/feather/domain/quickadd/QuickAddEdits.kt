package com.elkrrai.feather.domain.quickadd

/**
 * The text edits behind the quick-add sheet's tool buttons (date, priority, tag, repeat). They only
 * change the text: the parser stays the one place that decides what the text means, so a tool can
 * never disagree with what the chips show.
 */
object QuickAddEdits {

    private val SPACES = Regex("\\s{2,}")

    /** [text] with [token] added at the end, after a space if one is needed. */
    fun append(text: String, token: String): String = when {
        text.isBlank() -> token
        text.last().isWhitespace() -> text + token
        else -> "$text $token"
    }

    /** [text] without the words that were understood as [field] (their places are in [spans]). */
    fun without(text: String, spans: List<TokenSpan>, field: QuickAddField): String {
        val ranges = spans
            .filter { it.field == field }
            .map { it.start.coerceIn(0, text.length) until it.end.coerceIn(0, text.length) }
            .sortedByDescending { it.first }
        if (ranges.isEmpty()) return text
        val kept = StringBuilder(text)
        for (range in ranges) {
            if (!range.isEmpty()) kept.delete(range.first, range.last + 1)
        }
        return kept.toString().replace(SPACES, " ").trim()
    }

    /** [text] with the words of [field] swapped for [token] (added at the end if there were none). */
    fun replacing(text: String, spans: List<TokenSpan>, field: QuickAddField, token: String): String =
        append(without(text, spans, field), token)
}

/** When the sheet still shows its "Try: ..." example: for the first few tasks added from it. */
object QuickAddHint {
    const val SHOWN_FOR_ADDS = 5

    fun isVisible(addsSoFar: Int): Boolean = addsSoFar < SHOWN_FOR_ADDS
}

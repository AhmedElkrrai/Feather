package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.LocalDate
import java.time.LocalTime

/** The parts of a quick-add text the parser can recognize; each one can be switched off. */
enum class QuickAddField { DATE, TIME, PRIORITY, TAGS, LIST, REPEAT }

/**
 * Where a recognized part of the text sits: the [field] it was understood as, and the characters
 * [start] (included) to [end] (excluded) of the text that was parsed, in the user's own spelling.
 */
data class TokenSpan(val field: QuickAddField, val start: Int, val end: Int)

/**
 * What a quick-add text means. [title] is what is left after the recognized words are taken out
 * (in the user's own spelling). [listName] is one of the list names given to the parser, never
 * a made-up one.
 *
 * [dateIsImplied] is true when the due date was not written but follows from something else:
 * a time alone ("5pm" means today or tomorrow) or a repeat rule ("every monday" starts on the
 * next Monday). An implied date has no chip of its own and cannot be switched off separately.
 */
data class ParsedQuickAdd(
    val title: String,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val listName: String? = null,
    val recurrence: Recurrence? = null,
    val dateIsImplied: Boolean = false,
) {
    /**
     * Where each recognized part sits in the text, so it can be shown in place (tinted). Set by the
     * parser; it is not part of what two results are compared by.
     */
    var spans: List<TokenSpan> = emptyList()
        internal set

    /** The fields that were recognized, for showing chips the user can tap away. */
    val fields: Set<QuickAddField>
        get() = buildSet {
            if (dueDate != null && !dateIsImplied) add(QuickAddField.DATE)
            if (dueTime != null) add(QuickAddField.TIME)
            if (priority != Priority.NONE) add(QuickAddField.PRIORITY)
            if (tags.isNotEmpty()) add(QuickAddField.TAGS)
            if (listName != null) add(QuickAddField.LIST)
            if (recurrence != null) add(QuickAddField.REPEAT)
        }
}

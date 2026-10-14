package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Turns a line like "Call mom tomorrow 5pm !high #family @home every monday" into a task:
 * title "Call mom", due tomorrow at 17:00, high priority, tag family, list home, repeating
 * every Monday. English and standard Arabic phrases are understood together; `#`, `@` and `!`
 * work the same in both. Pure Kotlin, no Android and no network.
 *
 * Words the parser does not understand stay in the title. Any [QuickAddField] in `disabled` is
 * skipped, which also keeps its words in the title (this is what tapping a chip away does).
 */
object QuickAddParser {

    private val rules: List<LanguageRules> = emptyList()

    /**
     * @param now the current local date and time, which relative phrases ("tomorrow", "in 2
     *   hours") are worked out from
     * @param listNames the names of the existing lists; `@name` is only recognized when it
     *   matches one of them (spaces written as `_`)
     */
    fun parse(
        text: String,
        now: LocalDateTime,
        listNames: Collection<String> = emptyList(),
        disabled: Set<QuickAddField> = emptySet(),
    ): ParsedQuickAdd {
        if (text.isBlank()) return ParsedQuickAdd(title = "")
        val quick = QuickText(text)
        val today = now.toLocalDate()

        val tags = if (QuickAddField.TAGS in disabled) emptyList() else takeTags(quick)
        val listName = if (QuickAddField.LIST in disabled) null else takeList(quick, listNames)
        val priority = if (QuickAddField.PRIORITY in disabled) Priority.NONE else takePriority(quick)

        val repeat = if (QuickAddField.REPEAT in disabled) null else rules.firstNotNullOfOrNull { it.repeat(quick) }
        val timeSpec = if (QuickAddField.TIME in disabled) null else rules.firstNotNullOfOrNull { it.time(quick, now) }
        val written = if (QuickAddField.DATE in disabled) null else takeIsoDate(quick) ?: rules.firstNotNullOfOrNull { it.date(quick, today) }

        val time = timeSpec?.time
        var date = written ?: timeSpec?.date
        var implied = written == null && date != null
        if (date == null && (time != null || repeat != null)) {
            date = impliedDate(repeat, time, now)
            implied = true
        }

        val recurrence = repeat?.let { spec -> date?.let { buildRecurrence(spec, it) } }
        val title = quick.title().ifBlank { text.trim() }

        return ParsedQuickAdd(
            title = title,
            dueDate = date,
            dueTime = time,
            priority = priority,
            tags = tags,
            listName = listName,
            recurrence = recurrence,
            dateIsImplied = implied,
        )
    }

    // --- #tags, @list, !priority ---

    private val TAG = Regex("(?<!\\S)#(?=[\\p{L}\\p{N}_-]*\\p{L})([\\p{L}\\p{N}_-]+)")
    private val LIST = Regex("(?<!\\S)@([\\p{L}\\p{N}_-]+)")
    private val PRIORITY_WORD = Regex(
        "(?<!\\S)!(high|medium|med|low|1|2|3|عاليه|عالي|" +
            "متوسطه|متوسط|" +
            "منخفضه|منخفض)$WORD_END"
    )
    private val PRIORITY_SHORT = Regex("(?<![\\p{L}\\p{N}!])p([123])$WORD_END")
    private val PRIORITY_BANGS = Regex("(?<!\\S)!!!(?!\\S)")

    private fun takeTags(quick: QuickText): List<String> {
        val found = LinkedHashMap<String, String>()
        for (match in TAG.findAll(quick.text).toList()) {
            val name = quick.originalSlice(match.groups[1]!!.range)
            found.putIfAbsent(QuickText.normalize(name), name)
            quick.consume(match.range)
        }
        return found.values.toList()
    }

    private fun takeList(quick: QuickText, listNames: Collection<String>): String? {
        if (listNames.isEmpty()) return null
        for (match in LIST.findAll(quick.text)) {
            val wanted = match.groupValues[1].replace('_', ' ')
            val list = listNames.firstOrNull { QuickText.normalize(it).trim() == wanted } ?: continue
            quick.consume(match.range)
            return list
        }
        return null
    }

    private fun takePriority(quick: QuickText): Priority {
        quick.takeFirst(PRIORITY_WORD) { priorityOf(it.groupValues[1]) }?.let { return it }
        quick.takeFirst(PRIORITY_SHORT) { priorityOf(it.groupValues[1]) }?.let { return it }
        return quick.takeFirst(PRIORITY_BANGS) { Priority.HIGH } ?: Priority.NONE
    }

    private fun priorityOf(word: String): Priority? = when (word) {
        "high", "1", "عاليه", "عالي" -> Priority.HIGH
        "medium", "med", "2", "متوسطه", "متوسط" -> Priority.MEDIUM
        "low", "3", "منخفضه", "منخفض" -> Priority.LOW
        else -> null
    }

    // --- Dates that are written the same in every language ---

    private val ISO_DATE = Regex("$WORD_START(\\d{4})-(\\d{2})-(\\d{2})$WORD_END")

    private fun takeIsoDate(quick: QuickText): LocalDate? = quick.takeFirst(ISO_DATE) {
        try {
            LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        } catch (e: java.time.DateTimeException) {
            null
        }
    }

    // --- Putting the pieces together ---

    /**
     * The date to use when only a time or a repeat was written: for a repeat, its first matching
     * day; for a time alone, today if it is still ahead, otherwise tomorrow.
     */
    private fun impliedDate(repeat: RepeatSpec?, time: LocalTime?, now: LocalDateTime): LocalDate {
        val today = now.toLocalDate()
        val timeLeftToday = time == null || time.isAfter(now.toLocalTime())
        val start = if (timeLeftToday) today else today.plusDays(1)
        if (repeat != null && repeat.frequency == Frequency.WEEKLY && repeat.weekdays.isNotEmpty()) {
            var date = start
            while (date.dayOfWeek !in repeat.weekdays) date = date.plusDays(1)
            return date
        }
        return start
    }

    private fun buildRecurrence(spec: RepeatSpec, reference: LocalDate): Recurrence {
        val base = Recurrence.starting(spec.frequency, reference)
        return when (spec.frequency) {
            Frequency.WEEKLY -> base.copy(
                interval = spec.interval,
                weekdays = spec.weekdays.ifEmpty { setOf(reference.dayOfWeek) },
            )
            else -> base.copy(interval = spec.interval)
        }
    }

    /** Every weekday of [Frequency.WEEKLY] "weekday" phrases. */
    internal val WORKDAYS: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    internal val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}

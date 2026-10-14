package com.elkrrai.feather.domain.quickadd

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The parser says where each recognized part sits in the text, so the sheet can tint it in place. */
class QuickAddSpansTest {

    private val now = LocalDateTime.of(2026, 10, 12, 12, 0)

    private fun slices(text: String, parsed: ParsedQuickAdd): Map<QuickAddField, String> =
        parsed.spans.associate { it.field to text.substring(it.start, it.end) }

    @Test
    fun everyRecognizedPartGetsItsOwnSpan() {
        val text = "Call mom tomorrow 5pm !high #family"

        val parsed = QuickAddParser.parse(text, now)

        assertEquals(
            mapOf(
                QuickAddField.DATE to "tomorrow",
                QuickAddField.TIME to "5pm",
                QuickAddField.PRIORITY to "!high",
                QuickAddField.TAGS to "#family",
            ),
            slices(text, parsed),
        )
    }

    @Test
    fun spansAreInOrderInsideTheTextAndNeverOverlap() {
        val text = "Pay rent every month !low #home tomorrow 9am"

        val spans = QuickAddParser.parse(text, now).spans

        assertTrue(spans.isNotEmpty())
        for (span in spans) {
            assertTrue(span.start in 0 until span.end)
            assertTrue(span.end <= text.length)
        }
        for ((before, after) in spans.zipWithNext()) {
            assertTrue("${before.field} overlaps ${after.field}", before.end <= after.start)
        }
    }

    @Test
    fun whatIsLeftOutsideTheSpansIsTheTitle() {
        for (text in listOf(
            "Call mom tomorrow 5pm !high #family",
            "Water the plants every week",
            "اتصل بأمي غداً الساعة ٥ مساءً",
        )) {
            val parsed = QuickAddParser.parse(text, now)
            val rest = StringBuilder(text)
            for (span in parsed.spans.sortedByDescending { it.start }) rest.delete(span.start, span.end)

            assertEquals(text, parsed.title, rest.toString().replace(Regex("\\s+"), " ").trim())
        }
    }

    @Test
    fun aPartThatIsSwitchedOffHasNoSpanAndStaysInTheTitle() {
        val text = "Call mom tomorrow"

        val parsed = QuickAddParser.parse(text, now, disabled = setOf(QuickAddField.DATE))

        assertTrue(parsed.spans.none { it.field == QuickAddField.DATE })
        assertEquals("Call mom tomorrow", parsed.title)
    }

    @Test
    fun plainTextHasNoSpans() {
        assertTrue(QuickAddParser.parse("Buy milk", now).spans.isEmpty())
        assertTrue(QuickAddParser.parse("   ", now).spans.isEmpty())
    }

    @Test
    fun arabicPartsGetSpansInTheUsersOwnSpelling() {
        val text = "اتصل بأمي غداً الساعة ٥ مساءً"

        val parsed = QuickAddParser.parse(text, now)

        assertTrue(parsed.spans.any { it.field == QuickAddField.DATE })
        assertTrue(parsed.spans.any { it.field == QuickAddField.TIME })
        // The tashkeel and hamza in the words that stay are untouched.
        assertTrue(text.substring(0, parsed.spans.minOf { it.start }).contains("بأمي"))
    }

    @Test
    fun theSpansDoNotChangeWhatTwoResultsAreComparedBy() {
        val parsed = QuickAddParser.parse("Call mom tomorrow", now)

        // spans are not a constructor property, so a copy forgets them and equality ignores them
        assertEquals(parsed, parsed.copy())
        assertTrue(parsed.copy().spans.isEmpty())
    }
}

package com.elkrrai.feather.domain.quickadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddEditsTest {

    private fun span(field: QuickAddField, start: Int, end: Int) = TokenSpan(field, start, end)

    // --- append ---

    @Test
    fun appendingToNothingGivesJustTheToken() {
        assertEquals("#", QuickAddEdits.append("", "#"))
        assertEquals("!high", QuickAddEdits.append("   ", "!high"))
    }

    @Test
    fun appendingAfterAWordAddsASpace() {
        assertEquals("Buy milk !high", QuickAddEdits.append("Buy milk", "!high"))
    }

    @Test
    fun appendingAfterASpaceDoesNotDoubleIt() {
        assertEquals("Buy milk #", QuickAddEdits.append("Buy milk ", "#"))
    }

    // --- without ---

    @Test
    fun theWordsOfAFieldAreTakenOutAndTheSpacesTidied() {
        val text = "Buy milk !high today"
        val spans = listOf(span(QuickAddField.PRIORITY, 9, 14))

        assertEquals("Buy milk today", QuickAddEdits.without(text, spans, QuickAddField.PRIORITY))
    }

    @Test
    fun otherFieldsAreLeftAlone() {
        val text = "Buy milk !high tomorrow"
        val spans = listOf(span(QuickAddField.PRIORITY, 9, 14), span(QuickAddField.DATE, 15, 23))

        assertEquals("Buy milk !high", QuickAddEdits.without(text, spans, QuickAddField.DATE))
    }

    @Test
    fun aFieldWithNoSpanLeavesTheTextAsItIs() {
        assertEquals("Buy milk ", QuickAddEdits.without("Buy milk ", emptyList(), QuickAddField.TAGS))
    }

    @Test
    fun severalSpansOfOneFieldAreAllTakenOut() {
        val text = "#a milk #b"
        val spans = listOf(span(QuickAddField.TAGS, 0, 2), span(QuickAddField.TAGS, 8, 10))

        assertEquals("milk", QuickAddEdits.without(text, spans, QuickAddField.TAGS))
    }

    @Test
    fun aSpanPastTheEndOfTheTextIsClamped() {
        val spans = listOf(span(QuickAddField.DATE, 4, 99))

        assertEquals("Buy", QuickAddEdits.without("Buy tomorrow", spans, QuickAddField.DATE))
    }

    // --- replacing ---

    @Test
    fun replacingSwapsTheWordsForTheToken() {
        val text = "Buy milk !low"
        val spans = listOf(span(QuickAddField.PRIORITY, 9, 13))

        assertEquals("Buy milk !high", QuickAddEdits.replacing(text, spans, QuickAddField.PRIORITY, "!high"))
    }

    @Test
    fun replacingAddsTheTokenAtTheEndWhenThereWasNone() {
        assertEquals("Buy milk every week", QuickAddEdits.replacing("Buy milk", emptyList(), QuickAddField.REPEAT, "every week"))
    }

    @Test
    fun replacingThenParsingGivesWhatWasAskedFor() {
        val now = java.time.LocalDateTime.of(2026, 10, 12, 12, 0)
        val text = "Buy milk !low"
        val parsed = QuickAddParser.parse(text, now)

        val edited = QuickAddEdits.replacing(text, parsed.spans, QuickAddField.PRIORITY, "!high")

        assertEquals(com.elkrrai.feather.domain.model.Priority.HIGH, QuickAddParser.parse(edited, now).priority)
        assertEquals("Buy milk", QuickAddParser.parse(edited, now).title)
    }

    // --- the example hint ---

    @Test
    fun theExampleHintShowsForTheFirstFiveTasks() {
        for (adds in 0..4) assertTrue(QuickAddHint.isVisible(adds))
        assertFalse(QuickAddHint.isVisible(5))
        assertFalse(QuickAddHint.isVisible(40))
    }
}

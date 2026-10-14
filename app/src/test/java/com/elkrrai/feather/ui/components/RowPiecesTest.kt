package com.elkrrai.feather.ui.components

import com.elkrrai.feather.domain.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RowPiecesTest {

    // --- priority barbs ---

    @Test
    fun priorityDrawsNoneOneTwoOrThreeBarbs() {
        assertEquals(0, Priority.NONE.barbCount())
        assertEquals(1, Priority.LOW.barbCount())
        assertEquals(2, Priority.MEDIUM.barbCount())
        assertEquals(3, Priority.HIGH.barbCount())
    }

    @Test
    fun noBarbsMeansNoStrokes() {
        assertTrue(barbTicks(0).isEmpty())
        assertTrue(barbTicks(-1).isEmpty())
    }

    @Test
    fun oneStrokePerBarbAndAllInsideTheRing() {
        for (count in 1..3) {
            val ticks = barbTicks(count)
            assertEquals(count, ticks.size)
            for ((from, to) in ticks) {
                // The ring is 26 wide with a 12 radius around (13, 13); keep the strokes well inside it.
                for (point in listOf(from, to)) {
                    val dx = point.x - 13f
                    val dy = point.y - 13f
                    assertTrue("($dx, $dy) is outside the ring", dx * dx + dy * dy < 10f * 10f)
                }
            }
        }
    }

    @Test
    fun theBarbsAreCenteredAndSideBySide() {
        val ticks = barbTicks(3)
        val centers = ticks.map { (from, to) -> (from.x + to.x) / 2f }

        assertEquals(13f, centers[1], 0.001f)
        assertEquals(13f - centers[0], centers[2] - 13f, 0.001f)
        assertTrue(centers[0] < centers[1] && centers[1] < centers[2])
    }

    // --- subtask leaves ---

    @Test
    fun noSubtasksMeansNoLeaves() {
        assertEquals(LeafProgress(0, 0), leafProgress(total = 0, done = 0))
    }

    @Test
    fun upToFiveSubtasksGetOneLeafEach() {
        assertEquals(LeafProgress(shown = 4, filled = 2), leafProgress(total = 4, done = 2))
        assertEquals(LeafProgress(shown = 5, filled = 5), leafProgress(total = 5, done = 5))
        assertEquals(LeafProgress(shown = 3, filled = 0), leafProgress(total = 3, done = 0))
    }

    @Test
    fun moreThanFiveShowTheShareThatIsDone() {
        assertEquals(LeafProgress(shown = 5, filled = 3), leafProgress(total = 10, done = 6))
    }

    @Test
    fun aLeafIsNeverFilledForNothingDoneNorAllFilledWhileSomethingIsOpen() {
        assertEquals(0, leafProgress(total = 40, done = 0).filled)
        assertEquals(1, leafProgress(total = 40, done = 1).filled)
        assertEquals(4, leafProgress(total = 40, done = 39).filled)
        assertEquals(5, leafProgress(total = 40, done = 40).filled)
    }

    @Test
    fun aDoneCountOutsideTheRangeIsClamped() {
        assertEquals(LeafProgress(3, 3), leafProgress(total = 3, done = 9))
        assertEquals(LeafProgress(3, 0), leafProgress(total = 3, done = -2))
    }

    // --- the subtask bar ---

    @Test
    fun theBarIsFilledByTheShareOfSubtasksThatAreDone() {
        assertEquals(0f, subtaskFraction(done = 0, total = 4), 0.0001f)
        assertEquals(0.5f, subtaskFraction(done = 2, total = 4), 0.0001f)
        assertEquals(1f, subtaskFraction(done = 4, total = 4), 0.0001f)
    }

    @Test
    fun theBarIsEmptyWithoutSubtasksAndNeverOverfull() {
        assertEquals(0f, subtaskFraction(done = 0, total = 0), 0.0001f)
        assertEquals(1f, subtaskFraction(done = 9, total = 4), 0.0001f)
        assertEquals(0f, subtaskFraction(done = -3, total = 4), 0.0001f)
    }
}

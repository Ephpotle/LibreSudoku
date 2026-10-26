package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.Cage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KillerCageOutlineTest {
    private fun cage(vararg cells: Pair<Int, Int>, id: Int = 0) =
        Cage(id = id, sum = 10, cells = cells.map { Cell(it.first, it.second) })

    /** Endpoints that are not shared with another segment, i.e. places where the outline is open */
    private fun openEnds(segments: List<Pair<Offset, Offset>>): List<Offset> =
        segments.flatMap { listOf(it.first, it.second) }
            .groupingBy { it }.eachCount()
            .filter { it.value % 2 != 0 }
            .keys.toList()

    @Test
    fun outline_IsClosedForConvexAndConcaveShapes() {
        val shapes = listOf(
            cage(0 to 0),
            cage(0 to 0, 0 to 1, 0 to 2),
            cage(0 to 0, 1 to 0, 1 to 1), // L: one inner corner
            cage(0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2), // U: two inner corners
            cage(0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1), // plus: four inner corners
            cage(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        )
        shapes.forEach { shape ->
            val segments = cageOutlineSegments(shape, cellSize = 100f, inset = 10f)
            assertEquals("open outline for $shape", emptyList<Offset>(), openEnds(segments))
        }
    }

    @Test
    fun outline_InnerCornerMeetsAtInsetPoint() {
        // L shape: (0,0) above (1,0), with (1,1) to the right. The inner corner is at (100, 100).
        val segments = cageOutlineSegments(cage(0 to 0, 1 to 0, 1 to 1), cellSize = 100f, inset = 10f)
        val points = segments.flatMap { listOf(it.first, it.second) }
        assertTrue(Offset(90f, 110f) in points)
    }

    @Test
    fun outline_StopsBeforeTheSumLabel() {
        val shape = cage(0 to 1, 0 to 0, 1 to 0)
        val label = Rect(Offset(6f, 6f), Size(20f, 12f))
        val segments = cageOutlineSegments(shape, cellSize = 100f, inset = 10f, labelRect = label, labelGap = 4f)
        val open = openEnds(segments).toSet()
        assertEquals(setOf(Offset(30f, 10f), Offset(10f, 22f)), open)
    }

    @Test
    fun labelCell_IsTopLeftMost() {
        val shape = cage(1 to 0, 0 to 2, 0 to 1, 1 to 1)
        assertEquals(Cell(0, 1), shape.labelCell())
    }

    @Test
    fun tints_DifferForAdjacentCages() {
        val cages = (0 until 3).flatMap { row ->
            (0 until 3).map { col -> cage(row to col, id = row * 3 + col) }
        }
        val tints = cageTintIndices(cages, colorCount = 4)
        cages.forEachIndexed { i, a ->
            cages.forEachIndexed { j, b ->
                val ca = a.cells.single()
                val cb = b.cells.single()
                val adjacent = Math.abs(ca.row - cb.row) + Math.abs(ca.col - cb.col) == 1
                if (adjacent) assertNotEquals(tints[i], tints[j])
            }
        }
        assertTrue(tints.all { it in 0 until 4 })
    }
}

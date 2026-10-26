package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.Cage

/** How far a cage outline sits inside its cells, as a fraction of the cell size */
const val CAGE_INSET_FRACTION = 0.11f

/** Gap between the sum label and the outline that stops next to it, as a fraction of the cell size */
private const val CAGE_LABEL_GAP_FRACTION = 0.06f

/** The cell that carries the cage sum: the top-most cell, left-most within that row */
fun Cage.labelCell(): Cell = cells.minWith(compareBy({ it.row }, { it.col }))

/**
 * Fills every cell of a cage with [color]. Used to tint neighbouring cages differently,
 * so cage shapes can be read even without looking at the outlines.
 */
fun DrawScope.drawKillerCageTint(
    cage: Cage,
    cellSize: Float,
    color: Color
) {
    cage.cells.forEach { cell ->
        drawRect(
            color = color,
            topLeft = Offset(cell.col * cellSize, cell.row * cellSize),
            size = Size(cellSize, cellSize)
        )
    }
}

/**
 * Draws a solid outline inset inside the cage. The outline stops short of [labelRect]
 * so the sum reads as part of the outline instead of sitting on top of it.
 */
fun DrawScope.drawKillerCage(
    cage: Cage,
    cellSize: Float,
    strokeWidth: Float,
    color: Color,
    labelRect: Rect?
) {
    val path = Path()
    cageOutlineSegments(
        cage = cage,
        cellSize = cellSize,
        inset = cellSize * CAGE_INSET_FRACTION,
        labelRect = labelRect,
        labelGap = cellSize * CAGE_LABEL_GAP_FRACTION
    ).forEach { (start, end) ->
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )
}

/**
 * Line segments of the cage outline, inset by [inset] from the cell borders.
 *
 * Each cell draws the sides that face out of the cage. A side is extended to meet the
 * matching side of the next cell, and at an inner (concave) corner it is extended past the
 * cell border by [inset], so the outline is closed at every corner.
 */
fun cageOutlineSegments(
    cage: Cage,
    cellSize: Float,
    inset: Float,
    labelRect: Rect? = null,
    labelGap: Float = 0f
): List<Pair<Offset, Offset>> {
    val cells = cage.cells.map { it.row to it.col }.toSet()
    val labelCell = cage.labelCell()
    val segments = mutableListOf<Pair<Offset, Offset>>()

    cage.cells.forEach { cell ->
        fun has(dRow: Int, dCol: Int) = (cell.row + dRow to cell.col + dCol) in cells

        val left = cell.col * cellSize
        val top = cell.row * cellSize
        val right = left + cellSize
        val bottom = top + cellSize
        val isLabelCell = labelRect != null && cell.row == labelCell.row && cell.col == labelCell.col

        // start (left or top) and end (right or bottom) of a side, given the neighbours along it
        fun startOf(min: Float, hasNext: Boolean, hasDiagonal: Boolean) =
            if (!hasNext) min + inset else if (hasDiagonal) min - inset else min

        fun endOf(max: Float, hasNext: Boolean, hasDiagonal: Boolean) =
            if (!hasNext) max - inset else if (hasDiagonal) max + inset else max

        fun add(start: Offset, end: Offset) {
            if (end.x - start.x > 0.5f || end.y - start.y > 0.5f) segments.add(start to end)
        }

        if (!has(-1, 0)) {
            var xStart = startOf(left, has(0, -1), has(-1, -1))
            if (isLabelCell) xStart = maxOf(xStart, labelRect!!.right + labelGap)
            val y = top + inset
            add(Offset(xStart, y), Offset(endOf(right, has(0, 1), has(-1, 1)), y))
        }
        if (!has(1, 0)) {
            val y = bottom - inset
            add(
                Offset(startOf(left, has(0, -1), has(1, -1)), y),
                Offset(endOf(right, has(0, 1), has(1, 1)), y)
            )
        }
        if (!has(0, -1)) {
            var yStart = startOf(top, has(-1, 0), has(-1, -1))
            if (isLabelCell) yStart = maxOf(yStart, labelRect!!.bottom + labelGap)
            val x = left + inset
            add(Offset(x, yStart), Offset(x, endOf(bottom, has(1, 0), has(1, -1))))
        }
        if (!has(0, 1)) {
            val x = right - inset
            add(
                Offset(x, startOf(top, has(-1, 0), has(-1, 1))),
                Offset(x, endOf(bottom, has(1, 0), has(1, 1)))
            )
        }
    }
    return segments
}

/**
 * Picks a tint for every cage so that cages sharing a border never share a tint.
 * Greedy colouring, most-connected cages first. Returns an index in [0, colorCount) per cage,
 * in the same order as [cages]. If no free tint is left, the one least used by neighbours is taken.
 */
fun cageTintIndices(cages: List<Cage>, colorCount: Int = 4): IntArray {
    val owner = HashMap<Pair<Int, Int>, Int>()
    cages.forEachIndexed { index, cage -> cage.cells.forEach { owner[it.row to it.col] = index } }

    val neighbours = cages.mapIndexed { index, cage ->
        cage.cells.flatMap { cell ->
            listOf(
                cell.row - 1 to cell.col,
                cell.row + 1 to cell.col,
                cell.row to cell.col - 1,
                cell.row to cell.col + 1
            ).mapNotNull { owner[it] }
        }.filter { it != index }.toSet()
    }

    val result = IntArray(cages.size) { -1 }
    cages.indices.sortedByDescending { neighbours[it].size }.forEach { index ->
        val used = IntArray(colorCount)
        neighbours[index].forEach { other -> if (result[other] >= 0) used[result[other]]++ }
        result[index] = used.indexOfFirst { it == 0 }.takeIf { it >= 0 }
            ?: used.indices.minBy { used[it] }
    }
    return result
}

package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.rating.Topology

/**
 * Cells whose total is known from the 45 rule even though they are not a cage.
 *
 * @param cells Row-major cell indices.
 * @param sum Their total.
 * @param units Rows, then columns, then boxes of the region that produced the group.
 * @param multiUnit Whether the region spans more than one unit.
 * @param distinct Whether every pair of cells shares a unit or a cage, so all digits differ.
 */
internal class KnownSumGroup(
    val cells: IntArray, val sum: Int, val units: List<Int>, val multiUnit: Boolean, val distinct: Boolean
)

internal object KnownSumGroups {
    /**
     * Innie and outie groups of every row, column and box, and of 2 or 3 adjacent rows, columns or boxes.
     * Depends only on the cages, so it is computed once per puzzle.
     *
     * @param layout Cages and sums.
     * @param t Topology of the layout's geometry.
     * @param maxCells Largest group kept; bigger groups are too vague to help a solver.
     */
    fun of(layout: KillerLayout, t: Topology, maxCells: Int): List<KnownSumGroup> {
        val n = t.n; val g = layout.geometry
        val unitTotal = n * (n + 1) / 2
        val regions = ArrayList<List<Int>>()
        for (u in 0 until 3 * n) regions += listOf(u)
        for (size in 2..3) for (first in 0..n - size) {
            regions += (first until first + size).toList()
            regions += (first until first + size).map { n + it }
        }
        val boxesPerRow = n / g.boxWidth; val boxRows = n / g.boxHeight
        for (br in 0 until boxRows) for (bc in 0 until boxesPerRow) {
            val box = br * boxesPerRow + bc
            if (bc + 1 < boxesPerRow) regions += listOf(2 * n + box, 2 * n + box + 1)
            if (br + 1 < boxRows) regions += listOf(2 * n + box, 2 * n + box + boxesPerRow)
        }
        val out = ArrayList<KnownSumGroup>(); val seen = HashSet<List<Int>>()
        for (units in regions) {
            val inRegion = BooleanArray(t.cells)
            for (u in units) for (c in t.units[u]) inRegion[c] = true
            var insideSum = 0; var partialSum = 0
            val innies = ArrayList<Int>(); val outies = ArrayList<Int>()
            for ((k, cage) in layout.cages.withIndex()) {
                when (cage.count { inRegion[it] }) {
                    0 -> {}
                    cage.size -> insideSum += layout.sums[k]
                    else -> {
                        partialSum += layout.sums[k]
                        for (c in cage) if (inRegion[c]) innies += c else outies += c
                    }
                }
            }
            val innieSum = unitTotal * units.size - insideSum
            for ((cells, sum) in listOf(innies to innieSum, outies to partialSum - innieSum)) {
                if (cells.isEmpty() || cells.size > maxCells) continue
                // The same group can come from several regions; regions are listed simplest first.
                if (!seen.add(cells.sorted())) continue
                val distinct = cells.all { a -> cells.all { b ->
                    a == b || t.sees(a, b) || layout.cageOf[a] == layout.cageOf[b]
                } }
                out += KnownSumGroup(cells.toIntArray(), sum, units, units.size > 1, distinct)
            }
        }
        return out
    }
}

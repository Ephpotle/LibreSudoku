package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.rating.Topology

/** Thrown when a Killer search exceeds its node budget; the result is unknown, never "no solution". */
class KillerSearchBudgetExceeded(val nodes: Long) : RuntimeException("Killer search node budget exhausted at $nodes")

/**
 * Exact backtracking solver for Killer Sudoku: Sudoku rules plus distinct digits and a fixed sum per cage.
 * Besides the cages it enforces the distinct innie groups implied by the 45 rule, which are redundant but
 * prune the search hard. Branches on the most constrained cell or unit digit. Not thread-safe.
 *
 * @param layout Cages and sums to solve.
 * @param givens Optional row-major givens, with zero for empty cells.
 */
class KillerSolver(private val layout: KillerLayout, givens: IntArray = IntArray(layout.geometry.cellCount)) {
    private val topology = Topology.of(layout.geometry)
    private val n = topology.n
    private val full = topology.fullMask
    private val board = IntArray(topology.cells)
    private val rowUsed = IntArray(n)
    private val colUsed = IntArray(n)
    private val boxUsed = IntArray(n)
    /** Cages first, then redundant known-sum groups. Every constraint needs distinct digits. */
    private val constraintCells: List<IntArray>
    private val constraintUsed: IntArray
    private val sumLeft: IntArray
    private val cellsLeft: IntArray
    private val constraintsOf: Array<IntArray>
    private val found = ArrayList<IntArray>()
    private var limit = 0
    private var budget = 0L
    private var checkpoint: () -> Unit = {}

    /** Nodes visited by the most recent [solve]. */
    var nodes = 0L
        private set

    init {
        require(givens.size == topology.cells)
        val groups = KnownSumGroups.of(layout, topology, maxCells = 4).filter { it.distinct }
        constraintCells = layout.cages + groups.map { it.cells }
        sumLeft = (layout.sums.toList() + groups.map { it.sum }).toIntArray()
        constraintUsed = IntArray(constraintCells.size)
        cellsLeft = IntArray(constraintCells.size) { constraintCells[it].size }
        val lists = Array(topology.cells) { ArrayList<Int>(2) }
        for ((k, cells) in constraintCells.withIndex()) for (c in cells) lists[c] += k
        constraintsOf = Array(topology.cells) { lists[it].toIntArray() }
        for (c in givens.indices) if (givens[c] != 0) {
            val v = givens[c]
            require(v in 1..n) { "Givens must be in 1..$n" }
            val masks = candidates()
            require(masks != null && masks[c] and (1 shl (v - 1)) != 0) { "Givens conflict with the cages" }
            set(c, v)
        }
    }

    /**
     * Finds up to [maxSolutions] solutions.
     *
     * @param maxSolutions Positive cap; 2 decides uniqueness.
     * @param maxNodes Positive node budget; exhaustion throws [KillerSearchBudgetExceeded].
     * @param checkpoint Cooperative cancellation or deadline callback, called periodically.
     * @return Independent copies of the solutions found, at most [maxSolutions].
     */
    fun solve(maxSolutions: Int = 2, maxNodes: Long = 5_000_000L, checkpoint: () -> Unit = {}): List<IntArray> {
        require(maxSolutions > 0 && maxNodes > 0)
        found.clear(); limit = maxSolutions; budget = maxNodes; nodes = 0; this.checkpoint = checkpoint
        search()
        return found.map { it.copyOf() }
    }

    /**
     * Candidates of every empty cell. A digit survives only if, in each constraint holding the cell, the other
     * empty cells can still reach the remaining sum with digits they can hold. Null when a cell has none.
     */
    private fun candidates(): IntArray? {
        val masks = IntArray(board.size)
        for (c in board.indices) if (board[c] == 0) {
            var blocked = rowUsed[topology.row[c]] or colUsed[topology.col[c]] or boxUsed[topology.box[c]]
            for (k in constraintsOf[c]) blocked = blocked or constraintUsed[k]
            masks[c] = full and blocked.inv()
            if (masks[c] == 0) return null
        }
        for ((k, cells) in constraintCells.withIndex()) {
            val left = cellsLeft[k]
            if (left == 0) continue
            var any = 0
            for (c in cells) if (board[c] == 0) any = any or masks[c]
            if (CageCombinations.union(any, left, sumLeft[k]) == 0) return null
            for (c in cells) if (board[c] == 0) {
                var others = 0
                for (o in cells) if (o != c && board[o] == 0) others = others or masks[o]
                var keep = 0; var m = masks[c]
                while (m != 0) {
                    val b = m and -m; m = m xor b
                    val v = Integer.numberOfTrailingZeros(b) + 1
                    if (CageCombinations.union(others and b.inv(), left - 1, sumLeft[k] - v) != 0) keep = keep or b
                }
                if (keep == 0) return null
                masks[c] = keep
            }
        }
        return masks
    }

    private fun set(c: Int, v: Int) {
        val b = 1 shl (v - 1)
        board[c] = v
        rowUsed[topology.row[c]] = rowUsed[topology.row[c]] or b
        colUsed[topology.col[c]] = colUsed[topology.col[c]] or b
        boxUsed[topology.box[c]] = boxUsed[topology.box[c]] or b
        for (k in constraintsOf[c]) { constraintUsed[k] = constraintUsed[k] or b; sumLeft[k] -= v; cellsLeft[k]-- }
    }

    private fun unset(c: Int) {
        val v = board[c]; val b = (1 shl (v - 1)).inv()
        board[c] = 0
        rowUsed[topology.row[c]] = rowUsed[topology.row[c]] and b
        colUsed[topology.col[c]] = colUsed[topology.col[c]] and b
        boxUsed[topology.box[c]] = boxUsed[topology.box[c]] and b
        for (k in constraintsOf[c]) { constraintUsed[k] = constraintUsed[k] and b; sumLeft[k] += v; cellsLeft[k]++ }
    }

    /** Returns true to stop the search once the solution cap is reached. */
    private fun search(): Boolean {
        if (++nodes > budget) throw KillerSearchBudgetExceeded(nodes)
        if (nodes and 1023L == 0L) checkpoint()
        val masks = candidates() ?: return false
        var best = -1; var bestMask = 0; var bestCount = Int.MAX_VALUE
        for (c in board.indices) {
            if (board[c] != 0) continue
            val count = Integer.bitCount(masks[c])
            if (count < bestCount) { best = c; bestMask = masks[c]; bestCount = count }
        }
        if (best < 0) {
            found += board.copyOf()
            return found.size >= limit
        }
        // A digit with few places in a unit is often a narrower branch than any single cell.
        var placeUnit = -1; var placeDigit = 0
        if (bestCount > 1) {
            for (u in topology.units.indices) {
                val cells = topology.units[u]
                var placed = 0
                for (c in cells) if (board[c] != 0) placed = placed or (1 shl (board[c] - 1))
                for (v in 1..n) {
                    val b = 1 shl (v - 1)
                    if (placed and b != 0) continue
                    var count = 0
                    for (c in cells) if (masks[c] and b != 0) count++
                    if (count == 0) return false
                    if (count < bestCount) { bestCount = count; placeUnit = u; placeDigit = v }
                }
            }
        }
        if (placeUnit >= 0) {
            val b = 1 shl (placeDigit - 1)
            for (c in topology.units[placeUnit]) {
                if (masks[c] and b == 0) continue
                set(c, placeDigit)
                val stop = search()
                unset(c)
                if (stop) return true
            }
            return false
        }
        var m = bestMask
        while (m != 0) {
            val v = Integer.numberOfTrailingZeros(m) + 1; m = m and (m - 1)
            set(best, v)
            val stop = search()
            unset(best)
            if (stop) return true
        }
        return false
    }
}

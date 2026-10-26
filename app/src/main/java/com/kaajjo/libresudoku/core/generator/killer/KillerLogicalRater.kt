package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.rating.BasicTechniques
import com.kaajjo.libresudoku.core.generator.rating.Candidate
import com.kaajjo.libresudoku.core.generator.rating.EvaluationControl
import com.kaajjo.libresudoku.core.generator.rating.EvaluationLimits
import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.LogicalState
import com.kaajjo.libresudoku.core.generator.rating.LogicalStep
import com.kaajjo.libresudoku.core.generator.rating.QqWingLogicalRater
import com.kaajjo.libresudoku.core.generator.rating.RatingPolicy
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.generator.rating.Topology
import com.kaajjo.libresudoku.core.generator.rating.bit

/**
 * Outcome of one logical Killer pass.
 *
 * @param solved Whether the pass placed every digit without guessing.
 * @param counts Applications of each technique that was used at least once.
 * @param effortScore Sum of [KillerRatingPolicy.weight] over every applied step.
 * @param techniqueFloor Most demanding tier applied, or null when no step was needed.
 */
class KillerRating(
    val solved: Boolean,
    val counts: Map<Technique, Int>,
    val effortScore: Int,
    val techniqueFloor: LogicTier?
) {
    fun uses(technique: Technique): Int = counts[technique] ?: 0
}

/**
 * Rates a Killer puzzle by solving it the way a person would: classic singles and subsets plus cage
 * combinations, the 45 rule (innies and outies) over one or several units, and digits a cage forces into
 * one unit. Every Killer deduction is exact, so it preserves all completions of the puzzle.
 * Stateless apart from its limits; each [rate] call owns its state.
 *
 * @param limits Operation budget and classic chain limits shared with the classic rater.
 * @param maxVirtualCells Largest innie or outie group that is treated as a known sum.
 */
class KillerLogicalRater(
    private val limits: EvaluationLimits = EvaluationLimits(),
    private val maxVirtualCells: Int = 5
) {
    companion object { const val RATING_VERSION = "libresudoku-killer-logic-v1" }
    private val classic = QqWingLogicalRater(limits)

    /**
     * Evaluates the puzzle from its givens (normally none) and cages.
     *
     * @param layout Cages and sums of the puzzle.
     * @param givens Row-major givens, with zero for empty cells.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @throws com.kaajjo.libresudoku.core.generator.rating.EvaluationBudgetExceeded if the budget runs out.
     */
    fun rate(
        layout: KillerLayout,
        givens: IntArray = IntArray(layout.geometry.cellCount),
        checkpoint: () -> Unit = {}
    ): KillerRating {
        val topology = Topology.of(layout.geometry)
        val s = LogicalState(topology, EvaluationControl(limits, checkpoint))
        require(s.initialize(givens)) { "Conflicting givens" }
        val groups = KnownSumGroups.of(layout, topology, maxVirtualCells)
        val counts = LinkedHashMap<Technique, Int>()
        var effort = 0
        var floor: LogicTier? = null
        while (true) {
            s.control.check()
            if (s.contradiction()) return KillerRating(false, counts, effort, floor)
            if (s.board.all { it != 0 }) return KillerRating(true, counts, effort, floor)
            val step = nextStep(s, layout, groups) ?: return KillerRating(false, counts, effort, floor)
            s.apply(step)
            counts[step.technique] = (counts[step.technique] ?: 0) + 1
            effort += KillerRatingPolicy.weight(step.technique)
            floor = RatingPolicy.stronger(floor, step.technique.tier)
        }
    }

    private fun nextStep(s: LogicalState, layout: KillerLayout, groups: List<KnownSumGroup>): LogicalStep? {
        BasicTechniques.singles(s, true)?.let { return it }
        for (k in layout.cages.indices) {
            group(s, layout, layout.cages[k], layout.sums[k], Technique.CAGE_COMBINATION, emptyList())?.let { return it }
        }
        for (g in groups) if (!g.multiUnit) {
            group(s, layout, g.cells, g.sum, Technique.INNIE_OUTIE, g.units)?.let { return it }
        }
        for (k in layout.cages.indices) cageUnitLock(s, layout, k)?.let { return it }
        classic.nextStep(s, LogicTier.BASIC)?.let { return it }
        for (g in groups) if (g.multiUnit) {
            group(s, layout, g.cells, g.sum, Technique.MULTI_UNIT_INNIE_OUTIE, g.units)?.let { return it }
        }
        return classic.nextStep(s, LogicTier.CHAINS)
    }

    /**
     * Removes candidates that appear in no valid assignment of a cage or a known-sum group.
     * Two cells must differ when they share a unit or a cage.
     */
    private fun group(
        s: LogicalState, layout: KillerLayout, cells: IntArray, sum: Int, technique: Technique, units: List<Int>
    ): LogicalStep? {
        if (cells.all { s.board[it] != 0 }) return null
        val domains = IntArray(cells.size) { i -> domainOf(s, cells[i]) }
        val support = support(s, layout, cells, domains, sum) ?: return null
        val changes = ArrayList<Candidate>()
        for ((i, c) in cells.withIndex()) {
            if (s.board[c] != 0) continue
            var removed = s.masks[c] and support[i].inv()
            while (removed != 0) {
                val v = Integer.numberOfTrailingZeros(removed) + 1; removed = removed and (removed - 1)
                changes += Candidate(c, v)
            }
        }
        val symbols = (1..s.n).filter { v -> changes.any { it.value == v } }
        return s.eliminate(technique, changes, cells.toList(), symbols, units)
    }

    /**
     * A digit that every combination of a cage contains, confined to one row, column or box inside the cage,
     * cannot appear elsewhere in that unit.
     */
    private fun cageUnitLock(s: LogicalState, layout: KillerLayout, k: Int): LogicalStep? {
        val cells = layout.cages[k]
        if (cells.all { s.board[it] != 0 }) return null
        val domains = IntArray(cells.size) { i -> domainOf(s, cells[i]) }
        var possible = 0
        for (d in domains) possible = possible or d
        for (v in 1..s.n) {
            val b = bit(v)
            if (possible and b == 0 || cells.any { s.board[it] == v }) continue
            val without = IntArray(domains.size) { domains[it] and b.inv() }
            if (support(s, layout, cells, without, layout.sums[k]) != null) continue
            val positions = cells.filter { s.board[it] == 0 && s.has(it, v) }
            if (positions.isEmpty()) continue
            for (unit in s.topology.cellUnits[positions[0]]) {
                if (positions.any { unit !in s.topology.cellUnits[it] }) continue
                val changes = s.topology.units[unit]
                    .filter { layout.cageOf[it] != k && s.has(it, v) }
                    .map { Candidate(it, v) }
                s.eliminate(Technique.CAGE_UNIT_LOCK, changes, cells.toList(), listOf(v), listOf(unit))?.let { return it }
            }
        }
        return null
    }

    private fun domainOf(s: LogicalState, c: Int): Int = if (s.board[c] != 0) bit(s.board[c]) else s.masks[c]

    /**
     * Exact per-cell support of all assignments from [domains] that sum to [sum], or null when there are none.
     * Cells that share a unit or a cage must differ.
     */
    private fun support(s: LogicalState, layout: KillerLayout, cells: IntArray, domains: IntArray, sum: Int): IntArray? {
        val m = cells.size
        if (domains.any { it == 0 }) return null
        val conflicts = Array(m) { i -> BooleanArray(m) { j ->
            i != j && (s.topology.sees(cells[i], cells[j]) || layout.cageOf[cells[i]] == layout.cageOf[cells[j]])
        } }
        val allDistinct = (0 until m).all { i -> (0 until m).all { j -> i == j || conflicts[i][j] } }
        return if (allDistinct) distinctSupport(s, domains, sum) else generalSupport(s, domains, conflicts, sum)
    }

    /**
     * For pairwise-distinct cells the used-digit mask determines the partial sum, so a memo over
     * (cell, used digits) makes the search exact and polynomial in the number of masks.
     */
    private fun distinctSupport(s: LogicalState, domains: IntArray, sum: Int): IntArray? {
        val m = domains.size; val masks = 1 shl s.n
        val memo = ByteArray((m + 1) * masks) // 0 unknown, 1 completes, 2 dead end
        fun maskSum(used: Int): Int {
            var total = 0; var u = used
            while (u != 0) { total += Integer.numberOfTrailingZeros(u) + 1; u = u and (u - 1) }
            return total
        }
        fun completes(i: Int, used: Int): Boolean {
            val key = i * masks + used
            if (memo[key] != 0.toByte()) return memo[key] == 1.toByte()
            s.control.tick()
            val total = maskSum(used)
            var ok = false
            if (i == m) ok = total == sum
            else if (total < sum) {
                var options = domains[i] and used.inv()
                while (options != 0 && !ok) {
                    val b = options and -options; options = options xor b
                    ok = completes(i + 1, used or b)
                }
            }
            memo[key] = if (ok) 1 else 2
            return ok
        }
        if (!completes(0, 0)) return null
        val support = IntArray(m)
        var layer = hashSetOf(0)
        for (i in 0 until m) {
            val next = HashSet<Int>()
            for (used in layer) {
                var options = domains[i] and used.inv()
                while (options != 0) {
                    val b = options and -options; options = options xor b
                    if (completes(i + 1, used or b)) { support[i] = support[i] or b; next += used or b }
                }
            }
            layer = next
        }
        return support
    }

    /** Plain search for small groups whose cells need not all differ, such as outies in different units. */
    private fun generalSupport(s: LogicalState, domains: IntArray, conflicts: Array<BooleanArray>, sum: Int): IntArray? {
        val m = domains.size
        val order = (0 until m).sortedBy { Integer.bitCount(domains[it]) }.toIntArray()
        val minLeft = IntArray(m + 1); val maxLeft = IntArray(m + 1)
        for (p in m - 1 downTo 0) {
            val d = domains[order[p]]
            minLeft[p] = minLeft[p + 1] + Integer.numberOfTrailingZeros(d) + 1
            maxLeft[p] = maxLeft[p + 1] + 32 - Integer.numberOfLeadingZeros(d)
        }
        val assigned = IntArray(m); val support = IntArray(m)
        fun saturated() = (0 until m).all { support[it] == domains[it] }
        fun dfs(p: Int, total: Int): Boolean {
            s.control.tick()
            if (p == m) {
                if (total != sum) return false
                for (i in 0 until m) support[i] = support[i] or bit(assigned[i])
                return saturated()
            }
            val left = sum - total
            if (left < minLeft[p] || left > maxLeft[p]) return false
            val i = order[p]
            var options = domains[i]
            while (options != 0) {
                val v = Integer.numberOfTrailingZeros(options) + 1; options = options and (options - 1)
                var clash = false
                for (q in 0 until p) if (assigned[order[q]] == v && conflicts[i][order[q]]) { clash = true; break }
                if (clash) continue
                assigned[i] = v
                if (dfs(p + 1, total + v)) return true
            }
            assigned[i] = 0
            return false
        }
        dfs(0, 0)
        return if (support.any { it != 0 }) support else null
    }
}

package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.dlx.DlxEngine
import com.kaajjo.libresudoku.core.generator.rating.EvaluationBudgetExceeded
import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry
import com.kaajjo.libresudoku.core.generator.rating.Topology
import kotlin.random.Random

/**
 * A Killer puzzle with no givens whose cages alone have exactly one solution.
 *
 * @param layout Cages and sums.
 * @param solution The unique row-major solution.
 * @param rating Logical rating of the puzzle.
 */
class GeneratedKiller(val layout: KillerLayout, solution: IntArray, val rating: KillerRating) {
    private val solutionData = solution.copyOf()
    val solution: IntArray get() = solutionData.copyOf()
}

/**
 * Cage size mix for one generation attempt. Larger cages give fewer entry points and harder puzzles.
 *
 * @param sizes Cage sizes to draw from, each at least 2.
 * @param weights Relative weight of each size, indexed like [sizes].
 */
class CageProfile(val sizes: IntArray, val weights: IntArray) {
    init { require(sizes.size == weights.size && sizes.all { it >= 2 } && weights.all { it > 0 }) }
    fun draw(random: Random): Int {
        var r = random.nextInt(weights.sum())
        for (i in sizes.indices) { r -= weights[i]; if (r < 0) return sizes[i] }
        return sizes.last()
    }
}

/**
 * Generates zero-given Killer puzzles: a random full grid, a random cage layout, then cage splits until the
 * cages admit exactly one solution. Splitting only ever adds information; the repair loop is bounded.
 */
object KillerPuzzleGenerator {
    /**
     * @param geometry Grid dimensions.
     * @param profile Cage size mix.
     * @param random Random source owned by this request.
     * @param rater Logical rater; a completed logical solve also proves uniqueness.
     * @param quickNodes Node budget of the cheap search for a second solution.
     * @param maxNodes Node budget of the longer search for a second solution when the logical rater stalls.
     * @param merges Most cage merges to try after a unique puzzle is found; each one makes it harder.
     * @param maxMergedSize Largest cage a merge may create.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @return A unique puzzle, or null when this attempt did not converge; callers retry with fresh randomness.
     */
    fun generate(
        geometry: SudokuGeometry,
        profile: CageProfile,
        random: Random = Random.Default,
        rater: KillerLogicalRater = KillerLogicalRater(),
        quickNodes: Long = 20_000L,
        maxNodes: Long = 200_000L,
        merges: Int = 0,
        maxMergedSize: Int = geometry.size - 3,
        checkpoint: () -> Unit = {}
    ): GeneratedKiller? {
        val base = unique(geometry, profile, random, rater, quickNodes, maxNodes, checkpoint) ?: return null
        if (merges <= 0 || !base.rating.solved) return base
        return harden(base, merges, maxMergedSize, rater, random, checkpoint)
    }

    private fun unique(
        geometry: SudokuGeometry, profile: CageProfile, random: Random, rater: KillerLogicalRater,
        quickNodes: Long, maxNodes: Long, checkpoint: () -> Unit
    ): GeneratedKiller? {
        val engine = DlxEngine(geometry.size, geometry.boxHeight, geometry.boxWidth, random, checkpoint)
        check(engine.solve(IntArray(geometry.cellCount), 1, checkpoint) == 1)
        val solution = engine.solvedBoard
        val topology = Topology.of(geometry)
        val cages = randomCages(topology, solution, profile, random)
        repeat(geometry.cellCount) {
            checkpoint()
            val layout = KillerLayout.fromSolution(geometry, cages, solution)
            // Ambiguous layouts usually reveal a second solution quickly. Proving uniqueness by search is
            // expensive, but a completed logical solve proves it too, and only those puzzles are kept.
            val found = try {
                KillerSolver(layout).solve(2, quickNodes, checkpoint)
            } catch (_: KillerSearchBudgetExceeded) {
                val rating = rateOrNull(rater, layout, checkpoint) ?: return null
                if (rating.solved) return GeneratedKiller(layout, solution, rating)
                // Unsolved puzzles are discarded anyway, so only look a little further for an ambiguity to repair.
                try {
                    KillerSolver(layout).solve(2, maxNodes, checkpoint)
                } catch (_: KillerSearchBudgetExceeded) {
                    return null
                }
            }
            if (found.size == 1) {
                check(found[0].contentEquals(solution))
                val rating = rateOrNull(rater, layout, checkpoint) ?: return null
                return GeneratedKiller(layout, solution, rating)
            }
            val other = found.first { !it.contentEquals(solution) }
            val differing = solution.indices.filter { solution[it] != other[it] && cages[layout.cageOf[it]].size > 1 }
            if (differing.isEmpty()) return null
            val cell = differing[random.nextInt(differing.size)]
            val k = layout.cageOf[cell]
            val parts = split(topology, cages[k], cell, random) ?: return null
            cages[k] = parts.first; cages += parts.second
        }
        return null
    }

    /**
     * Merges neighbouring cages while the puzzle stays logically solvable, which also keeps it unique.
     * One-cell cages are merged first, since they act as givens. Each round tries a few random pairs.
     */
    private fun harden(
        base: GeneratedKiller, merges: Int, maxMergedSize: Int, rater: KillerLogicalRater,
        random: Random, checkpoint: () -> Unit
    ): GeneratedKiller {
        val geometry = base.layout.geometry; val t = Topology.of(geometry); val solution = base.solution
        var layout = base.layout; var rating = base.rating
        repeat(merges) {
            checkpoint()
            val pairs = HashSet<Long>()
            for (c in 0 until t.cells) for (o in neighbours(t, c)) {
                val a = layout.cageOf[c]; val b = layout.cageOf[o]
                if (a >= b) continue
                val ca = layout.cages[a]; val cb = layout.cages[b]
                if (ca.size + cb.size > maxMergedSize) continue
                if (ca.any { x -> cb.any { solution[it] == solution[x] } }) continue
                pairs += (a.toLong() shl 32) or b.toLong()
            }
            val ordered = pairs.shuffled(random).sortedBy { minOf(layout.cages[(it shr 32).toInt()].size, layout.cages[it.toInt()].size) }
            var merged = false
            for (pair in ordered.take(10)) {
                val a = (pair shr 32).toInt(); val b = pair.toInt()
                val cages = layout.cages.filterIndexed { k, _ -> k != a && k != b } + (layout.cages[a] + layout.cages[b])
                val trial = KillerLayout.fromSolution(geometry, cages, solution)
                val trialRating = rateOrNull(rater, trial, checkpoint) ?: continue
                if (!trialRating.solved) continue
                layout = trial; rating = trialRating; merged = true
                break
            }
            if (!merged) return GeneratedKiller(layout, solution, rating)
        }
        return GeneratedKiller(layout, solution, rating)
    }

    private fun rateOrNull(rater: KillerLogicalRater, layout: KillerLayout, checkpoint: () -> Unit): KillerRating? =
        try {
            rater.rate(layout, checkpoint = checkpoint)
        } catch (_: EvaluationBudgetExceeded) {
            null
        }

    /**
     * Grows connected cages of distinct solution digits. Leftover single cells join a neighbouring cage when
     * its digits allow; the rest stay as one-cell cages.
     */
    internal fun randomCages(t: Topology, solution: IntArray, profile: CageProfile, random: Random): MutableList<IntArray> {
        val owner = IntArray(t.cells) { -1 }
        val cages = ArrayList<MutableList<Int>>()
        val order = (0 until t.cells).shuffled(random)
        for (start in order) {
            if (owner[start] != -1) continue
            val target = profile.draw(random)
            val cage = mutableListOf(start); owner[start] = cages.size
            var used = bit(solution[start])
            while (cage.size < target) {
                val options = cage.flatMap { neighbours(t, it) }.distinct()
                    .filter { owner[it] == -1 && used and bit(solution[it]) == 0 }
                if (options.isEmpty()) break
                val next = options[random.nextInt(options.size)]
                cage += next; owner[next] = cages.size; used = used or bit(solution[next])
            }
            cages += cage
        }
        for (k in cages.indices) {
            if (cages[k].size != 1) continue
            val c = cages[k][0]
            val host = neighbours(t, c).map { owner[it] }.distinct().filter { h ->
                h != k && cages[h].size in 2 until t.n && cages[h].none { solution[it] == solution[c] }
            }.minByOrNull { cages[it].size } ?: continue
            cages[host] += c; cages[k].clear(); owner[c] = host
        }
        return cages.filter { it.isNotEmpty() }.map { it.toIntArray() }.toMutableList()
    }

    /** Splits a cage into two connected parts, preferring one that separates [cell] into the smaller half. */
    internal fun split(t: Topology, cage: IntArray, cell: Int, random: Random): Pair<IntArray, IntArray>? {
        if (cage.size < 2) return null
        val members = cage.toSet()
        repeat(16) {
            // Halves of at least two cells keep one-cell cages, which act as givens, rare.
            val wanted = if (cage.size >= 4) 2 + random.nextInt(cage.size / 2 - 1) else 1
            val part = linkedSetOf(cell)
            while (part.size < wanted) {
                val options = part.flatMap { neighbours(t, it) }.filter { it in members && it !in part }
                if (options.isEmpty()) break
                part += options[random.nextInt(options.size)]
            }
            val rest = cage.filter { it !in part }
            if (rest.isNotEmpty() && connected(t, rest)) return part.toIntArray() to rest.toIntArray()
        }
        // Removing a cell whose loss keeps the cage connected always works, e.g. a spanning-tree leaf.
        val leaf = cage.filter { c -> connected(t, cage.filter { it != c }) }.let { leaves ->
            leaves.firstOrNull { it == cell } ?: leaves.firstOrNull() } ?: return null
        return intArrayOf(leaf) to cage.filter { it != leaf }.toIntArray()
    }

    private fun connected(t: Topology, cells: List<Int>): Boolean {
        val members = cells.toSet(); val seen = hashSetOf(cells[0]); val queue = ArrayDeque(listOf(cells[0]))
        while (queue.isNotEmpty()) for (next in neighbours(t, queue.removeFirst())) {
            if (next in members && seen.add(next)) queue += next
        }
        return seen.size == members.size
    }

    private fun neighbours(t: Topology, c: Int): List<Int> {
        val r = t.row[c]; val col = t.col[c]; val out = ArrayList<Int>(4)
        if (r > 0) out += c - t.n
        if (r < t.n - 1) out += c + t.n
        if (col > 0) out += c - 1
        if (col < t.n - 1) out += c + 1
        return out
    }

    private fun bit(v: Int) = 1 shl (v - 1)
}

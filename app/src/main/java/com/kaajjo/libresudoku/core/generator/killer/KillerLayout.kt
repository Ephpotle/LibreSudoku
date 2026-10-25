package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry

/**
 * Immutable cage partition of a Killer grid. Every cell belongs to exactly one cage.
 *
 * @param geometry Grid side length and rectangular box dimensions.
 * @param cages Row-major cell indices of each cage; together they must cover the grid exactly once.
 * @param sums Target sum of each cage, indexed like [cages].
 */
class KillerLayout(val geometry: SudokuGeometry, cages: List<IntArray>, sums: IntArray) {
    val cages: List<IntArray> = cages.map { it.sortedArray() }
    val sums: IntArray = sums.copyOf()
    /** Cage index of every cell. */
    val cageOf = IntArray(geometry.cellCount) { -1 }

    init {
        require(this.cages.size == this.sums.size) { "Every cage needs a sum" }
        for ((k, cage) in this.cages.withIndex()) {
            require(cage.isNotEmpty() && cage.size <= geometry.size) { "Cage $k has an invalid size" }
            for (c in cage) {
                require(c in 0 until geometry.cellCount && cageOf[c] == -1) { "Cell $c is not in exactly one cage" }
                cageOf[c] = k
            }
        }
        require(cageOf.none { it == -1 }) { "Cages must cover the whole grid" }
    }

    companion object {
        /**
         * Builds a layout whose sums are taken from a full solution.
         *
         * @param geometry Grid dimensions matching the solution.
         * @param cages Cell indices of each cage.
         * @param solution Complete row-major solution with values 1..N.
         */
        fun fromSolution(geometry: SudokuGeometry, cages: List<IntArray>, solution: IntArray): KillerLayout =
            KillerLayout(geometry, cages, IntArray(cages.size) { k -> cages[k].sumOf { solution[it] } })
    }
}

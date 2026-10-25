package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.RatingPolicy
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty

/**
 * Killer scoring. Placements are nearly free, since every Killer needs all of them; the score is driven by
 * how often the solver needed something harder than the previous technique, so it tracks the hard spots.
 */
object KillerRatingPolicy {
    const val VERSION = "killer-score-v1"

    /**
     * Points for one application of a technique in a Killer pass.
     *
     * @param technique Technique applied by the step.
     */
    fun weight(technique: Technique): Int = when (technique) {
        Technique.FULL_HOUSE, Technique.SINGLE -> 0
        Technique.HIDDEN_SINGLE_ROW, Technique.HIDDEN_SINGLE_COLUMN, Technique.HIDDEN_SINGLE_SECTION -> 1
        Technique.CAGE_COMBINATION -> 3
        else -> RatingPolicy.weight(technique)
    }

    /** Score ceilings of Easy, Moderate and Hard for 9x9; other sizes scale by cell count. */
    private val NINE_BY_NINE_CEILINGS = intArrayOf(900, 1800, 2800)

    /**
     * Label of a completed Killer rating. Chains always make a puzzle Challenge.
     *
     * @param cells Number of cells in the grid.
     * @param rating Completed logical rating.
     */
    fun difficulty(cells: Int, rating: KillerRating): GameDifficulty {
        require(rating.solved) { "Only completed ratings have a difficulty" }
        if (rating.techniqueFloor == LogicTier.CHAINS) return GameDifficulty.Challenge
        val scaled = rating.effortScore * 81 / cells
        return when {
            scaled <= NINE_BY_NINE_CEILINGS[0] -> GameDifficulty.Easy
            scaled <= NINE_BY_NINE_CEILINGS[1] -> GameDifficulty.Moderate
            scaled <= NINE_BY_NINE_CEILINGS[2] -> GameDifficulty.Hard
            else -> GameDifficulty.Challenge
        }
    }
}

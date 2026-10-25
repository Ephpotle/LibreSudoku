package com.kaajjo.libresudoku.ui.statistics

/** How a saved game ended, as the statistics screen counts it. */
enum class GameOutcome { Won, Lost, InProgress }

/**
 * Win rate over finished games. A game counts as lost when it was given up or left for a new game.
 *
 * @param finished Games that were won or lost.
 * @param won Games solved without giving up.
 */
data class WinRate(val finished: Int, val won: Int) {
    /** Percentage of finished games that were won, or null before any game finished. */
    val percent: Int? get() = if (finished == 0) null else Math.round(won * 100f / finished)

    companion object {
        fun of(outcomes: List<GameOutcome>) = WinRate(
            finished = outcomes.count { it != GameOutcome.InProgress },
            won = outcomes.count { it == GameOutcome.Won }
        )

        /**
         * Mirrors the flags used by the overall win rate and streaks.
         *
         * @param completed Whether the game ended, by solving, giving up or starting another game.
         * @param giveUp Whether the player gave up.
         * @param canContinue Whether the game can still be continued, which is true for an abandoned game.
         */
        fun outcome(completed: Boolean, giveUp: Boolean, canContinue: Boolean): GameOutcome = when {
            !completed -> GameOutcome.InProgress
            !giveUp && !canContinue -> GameOutcome.Won
            else -> GameOutcome.Lost
        }
    }
}

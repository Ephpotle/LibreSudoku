package com.kaajjo.libresudoku.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WinRateTest {
    @Test
    fun outcome_MatchesTheOverallWinRateFlags() {
        assertEquals(GameOutcome.InProgress, WinRate.outcome(completed = false, giveUp = false, canContinue = true))
        assertEquals(GameOutcome.Won, WinRate.outcome(completed = true, giveUp = false, canContinue = false))
        assertEquals(GameOutcome.Lost, WinRate.outcome(completed = true, giveUp = true, canContinue = false))
        // Starting a new game marks the old one completed but still continuable.
        assertEquals(GameOutcome.Lost, WinRate.outcome(completed = true, giveUp = false, canContinue = true))
    }

    @Test
    fun percent_CountsOnlyFinishedGames() {
        val rate = WinRate.of(listOf(GameOutcome.Won, GameOutcome.Lost, GameOutcome.Lost, GameOutcome.InProgress))
        assertEquals(3, rate.finished)
        assertEquals(1, rate.won)
        assertEquals(33, rate.percent)
        assertNull(WinRate.of(listOf(GameOutcome.InProgress)).percent)
    }
}

package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.stock.PuzzleStock
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PuzzleStockTest {
    private fun puzzle(id: Int) = QQWingResult(IntArray(1) { id }, IntArray(1) { id }, GameDifficulty.Challenge, 1)

    @Test
    fun fillsToCapacityAndRefillsAfterTake() = runTest {
        var made = 0
        val produced = mutableListOf<QQWingResult>()
        val stock = PuzzleStock(2, backgroundScope) { puzzle(made++).also(produced::add) }

        assertNull(stock.take())
        runCurrent()
        assertEquals(2, stock.size)
        assertEquals(2, made)

        assertSame(produced[0], stock.take())
        runCurrent()
        assertEquals(2, stock.size)
        assertEquals(3, made)
    }

    @Test
    fun aFailedFillStopsAndTheNextRefillTriesAgain() = runTest {
        var fail = true
        val stock = PuzzleStock(1, backgroundScope) {
            if (fail) throw IllegalStateException("no puzzle") else puzzle(1)
        }
        stock.refill()
        runCurrent()
        assertEquals(0, stock.size)

        fail = false
        stock.refill()
        runCurrent()
        assertEquals(1, stock.size)
    }
}

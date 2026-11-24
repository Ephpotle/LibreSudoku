package com.kaajjo.libresudoku.core.generator.stock

import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A small queue of ready puzzles, filled in the background so a game can start without waiting.
 * Held in memory only; it refills after each [take] and whenever [refill] is called.
 *
 * @param capacity Most puzzles kept ready.
 * @param scope Scope that runs the filling; cancelling it stops the stock.
 * @param produce Makes one puzzle; may take long. A failure ends the current fill, and the next
 *   [refill] tries again.
 */
class PuzzleStock(
    private val capacity: Int,
    private val scope: CoroutineScope,
    private val produce: suspend () -> QQWingResult
) {
    private val ready = ArrayDeque<QQWingResult>()
    private val lock = Any()
    private var filling: Job? = null

    init {
        require(capacity > 0)
    }

    /** Number of puzzles ready now. */
    val size: Int get() = synchronized(lock) { ready.size }

    /** Removes and returns a ready puzzle, or null when none is ready. Starts a refill either way. */
    fun take(): QQWingResult? {
        val puzzle = synchronized(lock) { ready.removeFirstOrNull() }
        refill()
        return puzzle
    }

    /** Starts filling up to [capacity] unless a fill is already running. */
    fun refill() {
        synchronized(lock) {
            if (filling?.isActive == true || ready.size >= capacity) return
            filling = scope.launch {
                try {
                    while (synchronized(lock) { ready.size } < capacity) {
                        val puzzle = produce()
                        synchronized(lock) { ready.addLast(puzzle) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Stop this fill; live generation still works and the next refill tries again.
                }
            }
        }
    }
}

package com.kaajjo.libresudoku.core.generator.stock

import com.kaajjo.libresudoku.core.generator.killer.KillerController
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide stock of 9x9 Killer puzzles at the top level (Challenge, shown as "Extreme"). Stocked puzzles get a
 * longer search than a live request, so they are harder, and a game starts from them at once.
 */
@Singleton
class ExtremeKillerStock @Inject constructor() {
    private val controller = KillerController(maxGenerationMillis = 30_000L, challengeSearchMillis = 15_000L)
    private val stock = PuzzleStock(
        capacity = 2,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        produce = { controller.generate(GameType.Killer9x9, GameDifficulty.Challenge) }
    )

    /** Whether this stock serves requests of this kind. */
    fun serves(type: GameType, difficulty: GameDifficulty): Boolean =
        type == GameType.Killer9x9 && difficulty == GameDifficulty.Challenge

    /** A ready puzzle, or null; starts a refill. */
    fun take(): QQWingResult? = stock.take()

    /** Starts filling the stock if it is not full. */
    fun refill() = stock.refill()
}

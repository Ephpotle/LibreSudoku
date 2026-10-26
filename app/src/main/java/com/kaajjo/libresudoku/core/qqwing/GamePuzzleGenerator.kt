package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.generator.killer.KillerController
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Routes the variants offered by the app without widening the classical solver's contract.
 * 6x6 and 9x9 Killer puzzles have no givens: their cages alone have one solution, and their difficulty
 * comes from the Killer techniques needed to solve them. 12x12 Killer, and every Killer when [classicKiller]
 * is set, uses the old format: a unique classical puzzle with givens plus cages, rated by that classical
 * base. A zero-given 12x12 search is too slow for a phone.
 *
 * @param classicalController Generator for classical puzzles, and for the base of a classic-format Killer.
 * @param killerController Generator for zero-given Killer puzzles.
 * @param classicKiller Whether Killer requests use the old classical-base format.
 */
class GamePuzzleGenerator(
    private val classicalController: QQWingControllerV2 = QQWingControllerV2(),
    private val killerController: KillerController = KillerController(),
    private val classicKiller: Boolean = false
) {
    /**
     * Generates the requested variant.
     *
     * @param type Concrete classical or Killer variant; Unspecified is rejected.
     * @param targetDifficulty Required category; Unspecified accepts any completed rating.
     */
    suspend fun generate(type: GameType, targetDifficulty: GameDifficulty): QQWingResult {
        if (!classicKiller && (type == GameType.Killer6x6 || type == GameType.Killer9x9)) {
            return killerController.generate(type, targetDifficulty)
        }
        val baseType = when (type) {
            GameType.Killer6x6 -> GameType.Default6x6
            GameType.Killer9x9 -> GameType.Default9x9
            GameType.Killer12x12 -> GameType.Default12x12
            GameType.Default6x6, GameType.Default9x9, GameType.Default12x12 -> type
            GameType.Unspecified -> throw IllegalArgumentException("Concrete game type required")
        }
        val base = classicalController.generate(baseType, targetDifficulty)
        if (baseType == type) return base

        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val solution = List(type.size) { row ->
                List(type.size) { col -> Cell(row, col, base.solution[row * type.size + col]) }
            }
            val cages = CageGenerator(solution, type).generate(2, 5)
            currentCoroutineContext().ensureActive()
            base.copy(
                killerCages = cages,
                ratingMetadata = base.ratingMetadata?.let {
                    it.copy(version = "killer-classic-base-v1:${it.version}")
                }
            )
        }
    }
}

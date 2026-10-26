package com.kaajjo.libresudoku.core.generator.killer

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.GenerationTimeBudgetExceeded
import com.kaajjo.libresudoku.core.qqwing.NoRatedPuzzleException
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Returns zero-given Killer puzzles rated by [KillerLogicalRater], retrying until one matches the request.
 *
 * @param random Source of one independent seed per request.
 * @param maxGenerationMillis Positive elapsed-time budget of one request.
 */
class KillerController(
    private val random: Random = Random.Default,
    private val maxGenerationMillis: Long = 20_000L
) {
    private class Plan(val profile: CageProfile, val merges: Int)

    private val small = CageProfile(intArrayOf(2, 3, 4), intArrayOf(4, 4, 2))
    private val medium = CageProfile(intArrayOf(2, 3, 4, 5), intArrayOf(2, 4, 3, 2))

    /** Bigger cages and more merges make harder puzzles; the rating still decides the label. */
    private fun plan(target: GameDifficulty): Plan = when (target) {
        GameDifficulty.Easy -> Plan(small, 0)
        GameDifficulty.Moderate -> Plan(small, 3)
        GameDifficulty.Hard -> Plan(small, 8)
        GameDifficulty.Challenge -> Plan(medium, 40)
        else -> Plan(small, 3)
    }

    /**
     * @param type Killer variant.
     * @param targetDifficulty Easy to Challenge, or Unspecified for any completed rating.
     * @throws NoRatedPuzzleException if the time budget runs out first.
     */
    suspend fun generate(type: GameType, targetDifficulty: GameDifficulty): QQWingResult {
        require(type == GameType.Killer6x6 || type == GameType.Killer9x9) { "Zero-given Killer supports 6x6 and 9x9" }
        require(targetDifficulty in listOf(GameDifficulty.Unspecified, GameDifficulty.Easy, GameDifficulty.Moderate,
            GameDifficulty.Hard, GameDifficulty.Challenge))
        val seed = synchronized(random) { random.nextLong() }
        return withContext(Dispatchers.Default) {
            val context = currentCoroutineContext()
            val started = System.nanoTime(); val allowed = maxGenerationMillis * 1_000_000L
            val checkpoint: () -> Unit = {
                context.ensureActive()
                if (System.nanoTime() - started >= allowed) throw GenerationTimeBudgetExceeded()
            }
            val geometry = SudokuGeometry(type.size, type.sectionHeight, type.sectionWidth)
            val rng = Random(seed); val plan = plan(targetDifficulty); val rater = KillerLogicalRater()
            var attempts = 0; var mismatched = 0
            try {
                // Only the time budget ends the search; every attempt is independent.
                repeat(Int.MAX_VALUE) {
                    checkpoint(); attempts++
                    val generated = KillerPuzzleGenerator.generate(
                        geometry, plan.profile, rng, rater, merges = plan.merges, checkpoint = checkpoint
                    )
                    if (generated == null || !generated.rating.solved) return@repeat
                    val actual = KillerRatingPolicy.difficulty(geometry.cellCount, generated.rating)
                    if (targetDifficulty == GameDifficulty.Unspecified || actual == targetDifficulty) {
                        return@withContext result(type, generated, actual)
                    }
                    mismatched++
                }
            } catch (_: GenerationTimeBudgetExceeded) {
                context.ensureActive()
            }
            throw NoRatedPuzzleException("No Killer puzzle for $targetDifficulty; attempts=$attempts, mismatched=$mismatched")
        }
    }

    private fun result(type: GameType, generated: GeneratedKiller, difficulty: GameDifficulty): QQWingResult {
        val solution = generated.solution
        val cages = generated.layout.cages.mapIndexed { k, cells ->
            Cage(id = k, sum = generated.layout.sums[k],
                cells = cells.map { Cell(it / type.size, it % type.size, solution[it]) })
        }
        val rating = generated.rating
        return QQWingResult(
            puzzle = IntArray(solution.size),
            solution = solution,
            difficulty = difficulty,
            solutionCount = 1,
            ratingMetadata = RatingMetadata(
                version = "${KillerLogicalRater.RATING_VERSION}:${KillerRatingPolicy.VERSION}",
                tier = rating.techniqueFloor ?: LogicTier.SINGLES,
                techniqueCounts = rating.counts,
                effortScore = rating.effortScore,
                scorePolicy = KillerRatingPolicy.VERSION
            ),
            killerCages = cages
        )
    }
}

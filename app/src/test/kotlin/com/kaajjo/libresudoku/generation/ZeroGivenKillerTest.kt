package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.killer.CageCombinations
import com.kaajjo.libresudoku.core.generator.killer.CageProfile
import com.kaajjo.libresudoku.core.generator.killer.KillerController
import com.kaajjo.libresudoku.core.generator.killer.KillerLayout
import com.kaajjo.libresudoku.core.generator.killer.KillerLogicalRater
import com.kaajjo.libresudoku.core.generator.killer.KillerPuzzleGenerator
import com.kaajjo.libresudoku.core.generator.killer.KillerRatingPolicy
import com.kaajjo.libresudoku.core.generator.killer.KillerSolver
import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GamePuzzleGenerator
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.utils.SudokuParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ZeroGivenKillerTest {
    private val nine = SudokuGeometry(9, 3, 3)

    @Test
    fun cageCombinations_MatchHandCountedSums() {
        val all = (1 shl 9) - 1
        fun digits(mask: Int) = (1..9).filter { mask and (1 shl (it - 1)) != 0 }
        assertEquals(listOf(1, 2), digits(CageCombinations.union(all, 2, 3)))
        assertEquals(listOf(8, 9), digits(CageCombinations.union(all, 2, 17)))
        assertEquals(listOf(1, 2, 4), digits(CageCombinations.union(all, 3, 7)))
        assertEquals((1..9).toList(), digits(CageCombinations.union(all, 9, 45)))
        assertEquals(listOf(1, 2, 3, 4, 5), digits(CageCombinations.union(all, 2, 6) or CageCombinations.union(all, 2, 4)))
        assertEquals(0, CageCombinations.union(all, 2, 18))
        assertEquals(0, CageCombinations.union(all and 1.inv(), 2, 3))
        assertEquals(CageCombinations.FEASIBLE, CageCombinations.union(all, 0, 0))
    }

    @Test
    fun solver_FindsSeveralValidSolutionsOfAnAmbiguousLayout() {
        // One 45-cage per row adds nothing to the Sudoku rules, so the empty grid stays ambiguous.
        val layout = KillerLayout(nine, (0 until 9).map { r -> IntArray(9) { r * 9 + it } }, IntArray(9) { 45 })
        val found = KillerSolver(layout).solve(3)
        assertEquals(3, found.size)
        assertEquals(3, found.map { it.toList() }.toSet().size)
        for (grid in found) for (i in 0 until 9) {
            assertEquals((1..9).toSet(), (0 until 9).map { grid[i * 9 + it] }.toSet())
            assertEquals((1..9).toSet(), (0 until 9).map { grid[it * 9 + i] }.toSet())
            assertEquals((1..9).toSet(), (0 until 9).map { grid[(i / 3 * 3 + it / 3) * 9 + i % 3 * 3 + it % 3] }.toSet())
        }
    }

    @Test
    fun solver_RejectsLayoutsWithoutSolutions() {
        // Two 2-cell cages that both need {8, 9} in the same row.
        val cages = listOf(intArrayOf(0, 1), intArrayOf(2, 3)) + (4 until 81).map { intArrayOf(it) }
        val sums = intArrayOf(17, 17) + IntArray(77) { 1 + it % 9 }
        assertEquals(0, KillerSolver(KillerLayout(nine, cages, sums)).solve(2).size)
    }

    @Test
    fun generatedPuzzles_AreUniqueAndSolvedByTheRaterAlongTheirSolution() {
        val random = Random(11)
        val profile = CageProfile(intArrayOf(2, 3, 4, 5), intArrayOf(2, 4, 3, 2))
        var checked = 0
        repeat(12) {
            val generated = KillerPuzzleGenerator.generate(nine, profile, random, merges = 10) ?: return@repeat
            if (!generated.rating.solved) return@repeat
            checked++
            val found = KillerSolver(generated.layout).solve(2, maxNodes = 50_000_000L)
            assertEquals(1, found.size)
            assertArrayEquals(generated.solution, found.single())
            // A replayed rating must reach the same verdict and score.
            val again = KillerLogicalRater().rate(generated.layout)
            assertTrue(again.solved)
            assertEquals(generated.rating.effortScore, again.effortScore)
        }
        assertTrue("Too few completed puzzles: $checked", checked >= 6)
    }

    @Test
    fun hardestLabel_NeedsMoreThanCageArithmeticOnRows() {
        val random = Random(5)
        val result = runBlocking { KillerController(random, 60_000).generate(GameType.Killer9x9, GameDifficulty.Challenge) }
        val counts = checkNotNull(result.ratingMetadata).techniqueCounts
        assertTrue(counts.keys.any { it != Technique.CAGE_COMBINATION && it != Technique.INNIE_OUTIE &&
            it != Technique.SINGLE && it != Technique.FULL_HOUSE && !it.name.startsWith("HIDDEN_SINGLE") })
    }

    @Test
    fun twelveByTwelve_KeepsTheClassicFormatWithGivens() = runBlocking {
        val result = GamePuzzleGenerator().generate(GameType.Killer12x12, GameDifficulty.Easy)
        assertTrue(result.puzzle.any { it != 0 })
        assertTrue(checkNotNull(result.ratingMetadata).version.startsWith("killer-classic-base-v1:"))
    }

    @Test
    fun everySizeAndLabel_HasNoGivensAndValidUniqueCages() = runBlocking {
        withTimeout(240_000) {
            val generator = GamePuzzleGenerator()
            for (type in listOf(GameType.Killer6x6, GameType.Killer9x9)) {
                for (difficulty in listOf(GameDifficulty.Easy, GameDifficulty.Moderate,
                    GameDifficulty.Hard, GameDifficulty.Challenge)) {
                    val result = generator.generate(type, difficulty)
                    assertEquals("$type", difficulty, result.difficulty)
                    assertTrue(result.puzzle.all { it == 0 })
                    assertEquals(1, result.solutionCount)
                    val metadata = checkNotNull(result.ratingMetadata)
                    assertTrue(metadata.logicallySolved)
                    assertTrue(metadata.version.startsWith(KillerLogicalRater.RATING_VERSION))
                    assertEquals(KillerRatingPolicy.VERSION, metadata.scorePolicy)
                    assertValidCages(result, type)
                    val parser = SudokuParser()
                    val cages = checkNotNull(result.killerCages)
                    assertEquals(cages, parser.parseKillerSudokuCages(parser.killerSudokuCagesToString(cages)))
                }
            }
        }
    }

    private fun assertValidCages(result: QQWingResult, type: GameType) {
        val cages = checkNotNull(result.killerCages)
        val n = type.size
        assertEquals(n * n, cages.sumOf { it.cells.size })
        assertEquals((0 until n * n).toSet(), cages.flatMap { cage -> cage.cells.map { it.row * n + it.col } }.toSet())
        for (cage in cages) {
            val values = cage.cells.map { result.solution[it.row * n + it.col] }
            assertEquals(cage.cells.map { it.value }, values)
            assertEquals(values.size, values.distinct().size)
            assertEquals(values.sum(), cage.sum)
            val cells = cage.cells.map { it.row to it.col }.toSet()
            val reached = mutableSetOf(cells.first())
            var grew = true
            while (grew) grew = reached.addAll(cells.filter { c ->
                c !in reached && reached.any { abs(it.first - c.first) + abs(it.second - c.second) == 1 }
            })
            assertEquals("Disconnected cage ${cage.id}", cells, reached)
        }
        val layout = KillerLayout(SudokuGeometry(n, type.sectionHeight, type.sectionWidth),
            cages.map { cage -> cage.cells.map { it.row * n + it.col }.toIntArray() }, cages.map { it.sum }.toIntArray())
        val found = KillerSolver(layout).solve(2, maxNodes = 50_000_000L)
        assertEquals(1, found.size)
        assertArrayEquals(result.solution, found.single())
    }
}

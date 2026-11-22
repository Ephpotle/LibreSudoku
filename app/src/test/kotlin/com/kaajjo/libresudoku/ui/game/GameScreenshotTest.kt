package com.kaajjo.libresudoku.ui.game

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.game.models.GameSettings
import com.kaajjo.libresudoku.ui.game.models.GameUiState
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the game screen to PNG files for design review. Skipped unless the SCREENSHOT_DIR
 * environment variable names an output folder, so it never runs in normal CI:
 * SCREENSHOT_DIR=/tmp/shots ./gradlew :app:testDebugUnitTest --tests '*GameScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class GameScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = System.getenv("SCREENSHOT_DIR")

    @Before
    fun onlyWhenAsked() = assumeTrue(outDir != null)

    private val solution = "278519436519436278436278519785194362194362785362785194851943627943627851627851943"
    private val cageCells = listOf(
        listOf(0 to 0, 0 to 1, 1 to 1, 0 to 2),
        listOf(0 to 3, 0 to 4),
        listOf(0 to 5, 1 to 5, 0 to 6, 2 to 5),
        listOf(0 to 7, 1 to 7),
        listOf(0 to 8, 1 to 8),
        listOf(1 to 0, 2 to 0, 3 to 0),
        listOf(1 to 2, 1 to 3),
        listOf(1 to 4, 2 to 4),
        listOf(1 to 6, 2 to 6, 2 to 7),
        listOf(2 to 1, 3 to 1),
        listOf(2 to 2, 2 to 3, 3 to 2),
        listOf(2 to 8, 3 to 8, 4 to 8, 4 to 7),
        listOf(3 to 3, 3 to 4),
        listOf(3 to 5, 4 to 5, 5 to 5, 5 to 6, 4 to 6),
        listOf(3 to 6, 3 to 7),
        listOf(4 to 0, 4 to 1, 5 to 1),
        listOf(4 to 2, 4 to 3, 5 to 3),
        listOf(4 to 4, 5 to 4, 6 to 4),
        listOf(5 to 0, 6 to 0, 6 to 1, 7 to 0, 8 to 0),
        listOf(5 to 2, 6 to 2, 6 to 3),
        listOf(5 to 7, 6 to 7, 5 to 8, 6 to 8, 7 to 8),
        listOf(6 to 5, 7 to 5, 8 to 5, 8 to 4),
        listOf(6 to 6, 7 to 6, 7 to 7, 8 to 6, 8 to 7),
        listOf(7 to 1, 7 to 2, 8 to 1, 8 to 2, 8 to 3),
        listOf(7 to 3, 7 to 4),
        listOf(8 to 8),
    )

    private fun state(filledCages: Int, selected: Cell, notesMode: Boolean): GameUiState {
        val cages = cageCells.mapIndexed { index, cells ->
            Cage(
                id = index,
                sum = cells.sumOf { (r, c) -> solution[r * 9 + c].digitToInt() },
                cells = cells.map { (r, c) -> Cell(r, c) }
            )
        }
        val filled = cageCells.take(filledCages).flatten().toSet() +
            setOf(6 to 7, 7 to 7, 8 to 2, 5 to 3)
        val board = List(9) { r ->
            List(9) { c ->
                Cell(r, c, value = if ((r to c) in filled) solution[r * 9 + c].digitToInt() else 0)
            }
        }
        val notes = listOf(Note(3, 4, 2), Note(3, 4, 5), Note(3, 4, 8), Note(6, 1, 4), Note(6, 1, 9))
            .filter { board[it.row][it.col].value == 0 }
        return GameUiState(
            isLoading = false,
            settings = GameSettings(mistakesLimit = true, remainingUse = false),
            gameType = GameType.Killer9x9,
            gameDifficulty = GameDifficulty.Challenge,
            gameBoard = board,
            solvedBoard = List(9) { r -> List(9) { c -> Cell(r, c, solution[r * 9 + c].digitToInt()) } },
            cages = cages,
            notes = notes,
            currCell = board[selected.row][selected.col],
            gamePlaying = true,
            timeText = "07:42",
            mistakesCount = 1,
            notesToggled = notesMode
        )
    }

    private fun shoot(name: String, dark: Boolean, uiState: GameUiState) {
        compose.setContent {
            LibreSudokuTheme(darkTheme = dark) {
                val boardColors = SudokuBoardColorsImpl(
                    foregroundColor = BoardColors.foregroundColor,
                    notesColor = BoardColors.notesColor,
                    altForegroundColor = BoardColors.altForegroundColor,
                    errorColor = BoardColors.errorColor,
                    highlightColor = BoardColors.highlightColor,
                    thickLineColor = BoardColors.thickLineColor,
                    thinLineColor = BoardColors.thinLineColor
                )
                CompositionLocalProvider(LocalBoardColors provides boardColors) {
                    GameScreenContent(uiState = uiState, onEvent = { })
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        File(outDir!!).mkdirs()
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun lightMidGame() = shoot("light-mid", dark = false, state(filledCages = 14, selected = Cell(5, 4), notesMode = false))

    @Test
    fun darkNotesMode() = shoot("dark-notes", dark = true, state(filledCages = 14, selected = Cell(4, 4), notesMode = true))

    @Test
    fun lightEarly() = shoot("light-early", dark = false, state(filledCages = 0, selected = Cell(3, 4), notesMode = false))
}

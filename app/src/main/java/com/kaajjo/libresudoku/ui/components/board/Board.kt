package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.theme.Paper
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColors
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import com.kaajjo.libresudoku.ui.util.LightDarkPreview
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Sudoku board
 *
 * @param board sudoku game
 * @param size sudoku game size (always []
 * @param notes a list of [Note] for sudoku game
 * @param mainTextSize main numbers text size (used when [autoFontSize] is set to false
 * @param autoFontSize adjust numbers font size according to cell size. Enabling this will override [mainTextSize]
 * @param noteTextSize note number text size
 * @param selectedCell currently selected [Cell]
 * @param onClick returns [Cell] that was clicked
 * @param onLongClick returns [Cell] that was long clicked
 * @param identicalNumbersHighlight highlight cells with the same value as selected cell
 * @param errorsHighlight whether to highlight mistakes
 * @param positionLines whether to highlight row and column of currently selected [Cell]
 * @param enabled
 * @param questions  if enabled, "?" will be shown instead of number in cells (used instead of blur modifier on android < 12)
 * @param renderNotes whether to show notes at all
 * @param cellsToHighlight list of [Cell] to highlight
 * @param notesToHighlight list of [Note] to highlight
 * @param zoomable whether to allow zoom and pan the board
 * @param boardColors colors of the board (see [BoardColors])
 * @param crossHighlight highlight some boxes on the board
 * @param cages a list of [Cage] for killer sudoku
 * @param notesMode whether input currently makes notes; an empty selected cell then shows where they go
 */
@Composable
fun Board(
    modifier: Modifier = Modifier,
    board: List<List<Cell>>,
    size: Int = board.size,
    notes: List<Note>? = null,
    mainTextSize: TextUnit = when (size) {
        6 -> 32.sp
        9 -> 26.sp
        12 -> 24.sp
        else -> 14.sp
    },
    autoFontSize: Boolean = false,
    noteTextSize: TextUnit = when (size) {
        6 -> 18.sp
        9 -> 12.sp
        12 -> 7.sp
        else -> 14.sp
    },
    selectedCell: Cell,
    onClick: (Cell) -> Unit,
    onLongClick: (Cell) -> Unit = { },
    identicalNumbersHighlight: Boolean = true,
    errorsHighlight: Boolean = true,
    positionLines: Boolean = true,
    enabled: Boolean = true,
    questions: Boolean = false,
    renderNotes: Boolean = true,
    cellsToHighlight: List<Cell>? = null,
    notesToHighlight: List<Note> = emptyList(),
    zoomable: Boolean = false,
    boardColors: SudokuBoardColors = LocalBoardColors.current,
    crossHighlight: Boolean = false,
    cages: List<Cage> = emptyList(),
    notesMode: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()

        // single cell size
        val cellSize by remember(size) { mutableFloatStateOf(maxWidth / size.toFloat()) }
        // div for notes in one row in cell
        val cellSizeDivWidth by remember(size) { mutableFloatStateOf(cellSize / ceil(sqrt(size.toFloat()))) }

        val errorColor = boardColors.errorColor
        val foregroundColor = boardColors.foregroundColor
        val thickLineColor = boardColors.thickLineColor
        // locked numbers
        val altForegroundColor = boardColors.altForegroundColor

        // Killer boards are drawn like a page in a puzzle book: warm paper, the puzzle's
        // structure (box lines, cages, sums) printed in graphite, notes in pencil, and only the
        // player's digits in the theme's coloured ink. Paper and print stay the same on every
        // phone; the ink follows the wallpaper colours.
        val isKiller = cages.isNotEmpty()
        val colorScheme = MaterialTheme.colorScheme
        val isDarkBoard = colorScheme.surface.luminance() < 0.5f
        val paperColor = if (isDarkBoard) Paper.Dark else Paper.Light
        val printColor = if (isDarkBoard) Paper.PrintDark else Paper.PrintLight
        val thinLineColor = if (isKiller) printColor.copy(alpha = 0.16f) else boardColors.thinLineColor
        val notesColor = if (isKiller) printColor.copy(alpha = 0.72f) else boardColors.notesColor
        val sumColor = if (isKiller) printColor else foregroundColor

        // highlight (cells)
        val highlightColor = boardColors.highlightColor

        val vertThick by remember(size) { mutableIntStateOf(floor(sqrt(size.toFloat())).toInt()) }
        val horThick by remember(size) { mutableIntStateOf(ceil(sqrt(size.toFloat())).toInt()) }

        var fontSizePx by remember { mutableFloatStateOf(1f) }
        with(LocalDensity.current) {
            LaunchedEffect(autoFontSize, size, mainTextSize) {
                fontSizePx = if (autoFontSize) {
                    (cellSize * 0.9f).toSp().toPx()
                } else {
                    mainTextSize.toPx()
                }
            }
        }
        val noteSizePx = with(LocalDensity.current) { (cellSizeDivWidth * 0.8f).toSp().toPx() }
        val killerSumSizePx = with(LocalDensity.current) { noteSizePx * 1.2f }

        val thinLineWidth = with(LocalDensity.current) { 1.3.dp.toPx() }
        // killer boards need box borders that clearly differ from the cage outlines,
        // so they are thicker and drawn in the full number colour
        val thickLineWidth = with(LocalDensity.current) { (if (isKiller) 2.4.dp else 1.3.dp).toPx() }
        // box lines are the heaviest print on the page; cage outlines are thinner and lighter
        val boxLineColor = if (isKiller) printColor else thickLineColor
        // cage outlines are solid, full-strength print (as asked for in #6); the outlines alone
        // carry the cage shapes, so cages get no background wash
        val cageLineWidth = with(LocalDensity.current) { 1.5.dp.toPx() }
        val cageLineColor = printColor

        // the cage of the selected cell is outlined more strongly, so its shape is easy to follow
        val selectedCageIndex = remember(cages, selectedCell.row, selectedCell.col) {
            cages.indexOfFirst { cage ->
                cage.cells.any { it.row == selectedCell.row && it.col == selectedCell.col }
            }
        }
        // The selected cage is outlined in ink with a light ink wash; the selected cell inside
        // it gets a stronger wash and a ring, so the cell never gets lost in its cage.
        // (Tertiary was tried first, but in many palettes it is a rose close to the mistake colour.)
        val inkColor = foregroundColor
        val selectedCageLineWidth = with(LocalDensity.current) { 2.2.dp.toPx() }
        val selectedCageLineColor = inkColor
        val selectedCageTint = inkColor.copy(alpha = if (isDarkBoard) 0.10f else 0.07f)
        val selectedCellFill = inkColor.copy(alpha = if (isDarkBoard) 0.22f else 0.15f)
        val selectedCellRingWidth = with(LocalDensity.current) { 2.dp.toPx() }
        val selectedCellRingInset = with(LocalDensity.current) { 3.dp.toPx() }

        // The selection glides from cell to cell instead of jumping
        val selectionPosition = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        var selectionVisible by remember { mutableStateOf(false) }
        LaunchedEffect(selectedCell.row, selectedCell.col) {
            if (selectedCell.row < 0 || selectedCell.col < 0) {
                selectionVisible = false
                return@LaunchedEffect
            }
            val target = Offset(selectedCell.col.toFloat(), selectedCell.row.toFloat())
            if (!selectionVisible) {
                selectionPosition.snapTo(target)
                selectionVisible = true
            } else {
                selectionPosition.animateTo(
                    target,
                    spring(dampingRatio = 0.85f, stiffness = 1500f)
                )
            }
        }

        // Placed digits are "ink": a bundled serif (DM Serif Text, OFL), while notes stay in the
        // plain system sans like pencil marks
        val inkContext = LocalContext.current
        val inkTypeface = remember(inkContext) {
            runCatching { ResourcesCompat.getFont(inkContext, R.font.ink_digits) }.getOrNull()
                ?: Typeface.DEFAULT
        }

        // A digit that was just placed pops in with a small bounce and a faint ink spread
        val inkSpread = remember { Animatable(1f) }
        val placedScale = remember { Animatable(1f) }
        var placedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val previousValues = remember { arrayOfNulls<IntArray>(1) }
        LaunchedEffect(board) {
            val values = IntArray(size * size) { board[it / size][it % size].value }
            val previous = previousValues[0]
            previousValues[0] = values
            if (previous == null || previous.size != values.size) return@LaunchedEffect
            val changed = values.indices.filter { values[it] != previous[it] }
            if (changed.size == 1 && values[changed[0]] != 0) {
                placedCell = changed[0] / size to changed[0] % size
                launch {
                    inkSpread.snapTo(0f)
                    inkSpread.animateTo(1f, tween(durationMillis = 260))
                }
                placedScale.snapTo(0.55f)
                placedScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f))
            }
        }

        val killerSumBounds by remember { mutableStateOf(android.graphics.Rect()) }

        // Cages whose cells are all filled settle into a quieter style. This says "filled",
        // never "correct", which the player can already see on the board.
        val filledCages = remember(board, cages) {
            cages.map { cage ->
                cage.cells.all { cell ->
                    (board.getOrNull(cell.row)?.getOrNull(cell.col)?.value ?: 0) != 0
                }
            }
        }
        // a filled cage steps back but stays easy to trace
        val settledCageLineColor = printColor.copy(alpha = 0.45f)
        // cells that carry a cage sum; only these give up room for it
        val sumCells = remember(cages) { cages.map { it.labelCell().let { c -> c.row to c.col } }.toSet() }

        // paints
        // numbers
        var textPaint by remember(fontSizePx) {
            mutableStateOf(
                Paint().apply {
                    color = foregroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx * INK_SIZE_FACTOR
                    typeface = inkTypeface
                }
            )
        }
        // errors
        var errorTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = errorColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx * INK_SIZE_FACTOR
                    typeface = inkTypeface
                }
            )
        }
        // locked numbers
        var lockedTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = altForegroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx * INK_SIZE_FACTOR
                    typeface = inkTypeface
                }
            )
        }

        // notes
        var notePaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = notesColor.toArgb()
                    isAntiAlias = true
                    textSize = noteSizePx
                }
            )
        }

        val noteHighlightPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = highlightColor.copy(alpha = 0.3f).toArgb()
                    isAntiAlias = true
                }
            )
        }

        var killerSumPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = sumColor.toArgb()
                    isAntiAlias = true
                    textSize = killerSumSizePx
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        }

        val context = LocalContext.current
        LaunchedEffect(mainTextSize, noteTextSize, boardColors) {
            textPaint = Paint().apply {
                color = foregroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx * INK_SIZE_FACTOR
                typeface = inkTypeface
            }
            notePaint = Paint().apply {
                color = notesColor.toArgb()
                isAntiAlias = true
                textSize = noteSizePx
            }
            errorTextPaint = Paint().apply {
                color = Color(230, 67, 83).toArgb()
                isAntiAlias = true
                textSize = fontSizePx * INK_SIZE_FACTOR
                typeface = inkTypeface
            }
            lockedTextPaint = Paint().apply {
                color = altForegroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx * INK_SIZE_FACTOR
                typeface = inkTypeface
            }
            killerSumPaint = Paint().apply {
                color = sumColor.toArgb()
                isAntiAlias = true
                textSize = killerSumSizePx
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        var zoom by remember(enabled) { mutableFloatStateOf(1f) }
        var offset by remember(enabled) { mutableStateOf(Offset.Zero) }

        val boardModifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = enabled, key2 = board) {
                detectTapGestures(
                    onTap = {
                        val totalOffset = it / zoom + offset
                        val row =
                            floor((totalOffset.y) / cellSize)
                                .toInt()
                                .coerceIn(board.indices)
                        val column =
                            floor((totalOffset.x) / cellSize)
                                .toInt()
                                .coerceIn(board.indices)
                        onClick(board[row][column])
                    },
                    onLongPress = {
                        if (enabled) {
                            val totalOffset = it / zoom + offset
                            val row = floor((totalOffset.y) / cellSize).toInt().coerceIn(board.indices)
                            val column = floor((totalOffset.x) / cellSize).toInt().coerceIn(board.indices)
                            onLongClick(board[row][column])
                        }
                    }
                )
            }

        val zoomModifier = Modifier
            .pointerInput(enabled) {
                detectTransformGestures(
                    onGesture = { gestureCentroid, gesturePan, gestureZoom, _ ->
                        if (enabled) {
                            val oldScale = zoom
                            val newScale = (zoom * gestureZoom).coerceIn(1f..3f)

                            offset = (offset + gestureCentroid / oldScale) -
                                    (gestureCentroid / newScale + gesturePan / oldScale)

                            zoom = newScale
                            if (offset.x < 0) {
                                offset = Offset(0f, offset.y)
                            } else if (maxWidth - offset.x < maxWidth / zoom) {
                                offset = offset.copy(x = maxWidth - maxWidth / zoom)
                            }
                            if (offset.y < 0) {
                                offset = Offset(offset.x, 0f)
                            } else if (maxWidth - offset.y < maxWidth / zoom) {
                                offset = offset.copy(y = maxWidth - maxWidth / zoom)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = -offset.x * zoom
                translationY = -offset.y * zoom
                scaleX = zoom
                scaleY = zoom
                TransformOrigin(0f, 0f).also { transformOrigin = it }
            }
        Canvas(
            modifier = if (zoomable) boardModifier.then(zoomModifier) else boardModifier
        ) {
            val cornerRadius = CornerRadius(15f, 15f)

            if (isKiller) {
                drawRoundRect(color = paperColor, cornerRadius = cornerRadius)
            }
            if (selectedCageIndex >= 0) {
                drawKillerCageTint(
                    cage = cages[selectedCageIndex],
                    cellSize = cellSize,
                    color = selectedCageTint
                )
            }

            if (selectedCell.row >= 0 && selectedCell.col >= 0) {
                // current cell, drawn where the gliding selection currently is
                val selection = if (selectionVisible) {
                    selectionPosition.value
                } else {
                    Offset(selectedCell.col.toFloat(), selectedCell.row.toFloat())
                }
                drawRoundCell(
                    row = selectedCell.row,
                    col = selectedCell.col,
                    gameSize = size,
                    rect = Rect(
                        offset = Offset(
                            x = selection.x * cellSize,
                            y = selection.y * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    ),
                    color = if (isKiller) selectedCellFill else highlightColor.copy(alpha = 0.2f),
                    cornerRadius = cornerRadius
                )
                if (isKiller) {
                    drawRoundRect(
                        color = inkColor,
                        topLeft = Offset(
                            selection.x * cellSize + selectedCellRingInset,
                            selection.y * cellSize + selectedCellRingInset
                        ),
                        size = Size(
                            cellSize - 2 * selectedCellRingInset,
                            cellSize - 2 * selectedCellRingInset
                        ),
                        cornerRadius = CornerRadius(cellSize * 0.12f),
                        style = Stroke(width = selectedCellRingWidth)
                    )
                }
                if (positionLines) {
                    drawPositionLines(
                        row = selectedCell.row,
                        col = selectedCell.col,
                        gameSize = size,
                        color = highlightColor.copy(alpha = 0.1f),
                        cellSize = cellSize,
                        lineLength = maxWidth,
                        cornerRadius = cornerRadius
                    )
                }
            }
            if (identicalNumbersHighlight) {
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        if (board[i][j].value == selectedCell.value && board[i][j].value != 0) {
                            drawRoundCell(
                                row = board[i][j].row,
                                col = board[i][j].col,
                                gameSize = size,
                                rect = Rect(
                                    offset = Offset(
                                        x = board[i][j].col * cellSize,
                                        y = board[i][j].row * cellSize
                                    ),
                                    size = Size(cellSize, cellSize)
                                ),
                                color = highlightColor.copy(alpha = 0.2f),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
            cellsToHighlight?.forEach {
                drawRoundCell(
                    row = it.row,
                    col = it.col,
                    gameSize = size,
                    color = highlightColor.copy(alpha = 0.3f),
                    rect = Rect(
                        Offset(
                            x = it.col * cellSize,
                            y = it.row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    ),
                    cornerRadius = cornerRadius
                )
            }

            drawBoardFrame(
                thickLineColor = boxLineColor,
                thickLineWidth = thickLineWidth,
                maxWidth = maxWidth,
                cornerRadius = CornerRadius(15f, 15f)
            )

            // horizontal line
            for (i in 1 until size) {
                val isThickLine = i % horThick == 0
                drawLine(
                    color = if (isThickLine) boxLineColor else thinLineColor,
                    start = Offset(cellSize * i.toFloat(), 0f),
                    end = Offset(cellSize * i.toFloat(), maxWidth),
                    strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                )
            }
            // vertical line
            for (i in 1 until size) {
                val isThickLine = i % vertThick == 0
                if (maxWidth >= cellSize * i) {
                    drawLine(
                        color = if (isThickLine) boxLineColor else thinLineColor,
                        start = Offset(0f, cellSize * i.toFloat()),
                        end = Offset(maxWidth, cellSize * i.toFloat()),
                        strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                    )
                }
            }

            placedCell?.let { (row, col) ->
                if (inkSpread.value < 1f && row < size && col < size) {
                    drawCircle(
                        color = foregroundColor.copy(alpha = 0.16f * (1f - inkSpread.value)),
                        radius = cellSize * (0.22f + 0.26f * inkSpread.value),
                        center = Offset((col + 0.5f) * cellSize, (row + 0.5f) * cellSize)
                    )
                }
            }

            // measure the sum label height up front, so notes and the notes cursor know where to start
            if (isKiller) killerSumPaint.getTextBounds("0", 0, 1, killerSumBounds)

            if (notesMode && selectedCell.row in 0 until size && selectedCell.col in 0 until size &&
                board[selectedCell.row][selectedCell.col].value == 0
            ) {
                drawNotesCursor(
                    row = selectedCell.row,
                    col = selectedCell.col,
                    gameSize = size,
                    cellSize = cellSize,
                    topInset = notesTopInset(
                        cellSize,
                        if ((selectedCell.row to selectedCell.col) in sumCells) killerSumBounds.height().toFloat() else 0f
                    ),
                    color = notesColor.copy(alpha = 0.85f)
                )
            }

            drawNumbers(
                size = size,
                board = board,
                highlightErrors = errorsHighlight,
                errorTextPaint = errorTextPaint,
                lockedTextPaint = lockedTextPaint,
                textPaint = textPaint,
                questions = questions,
                cellSize = cellSize,
                poppedCell = placedCell,
                poppedScale = placedScale.value
            )

            if (!notes.isNullOrEmpty() && !questions && renderNotes) {
                drawNotes(
                    size = size,
                    paint = notePaint,
                    highlightPaint = noteHighlightPaint,
                    notes = notes,
                    notesToHighlight = notesToHighlight,
                    cellSize = cellSize,
                    cellSizeDivWidth = cellSizeDivWidth,
                    killerSumBounds = killerSumBounds,
                    sumCells = sumCells
                )
            }

            // doesn't look good on 6x6
            if (crossHighlight && size != 6) {
                drawCrossSelection(
                    gameSize = size,
                    sectionWidth = getSectionWidthForSize(size),
                    sectionHeight = getSectionHeightForSize(size),
                    color = highlightColor.copy(alpha = 0.1f),
                    cellSize = cellSize
                )
            }

            if (cages.isNotEmpty()) {
                val labelPadding = cellSize * 0.06f
                val settledSumPaint = Paint(killerSumPaint).apply { alpha = 150 }
                cages.forEachIndexed { index, cage ->
                    val isSelectedCage = index == selectedCageIndex
                    val isSettled = !isSelectedCage && filledCages.getOrElse(index) { false }
                    val textToDraw = if (questions) "?" else cage.sum.toString()
                    killerSumPaint.getTextBounds(textToDraw, 0, textToDraw.length, killerSumBounds)
                    val cellWithSum = cage.labelCell()
                    val labelRect = Rect(
                        offset = Offset(
                            x = cellWithSum.col * cellSize + labelPadding,
                            y = cellWithSum.row * cellSize + labelPadding
                        ),
                        size = Size(
                            killerSumPaint.measureText(textToDraw),
                            killerSumBounds.height().toFloat()
                        )
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            textToDraw,
                            labelRect.left,
                            labelRect.bottom,
                            if (isSettled) settledSumPaint else killerSumPaint
                        )
                    }

                    drawKillerCage(
                        cage = cage,
                        cellSize = cellSize,
                        strokeWidth = if (isSelectedCage) selectedCageLineWidth else cageLineWidth,
                        color = when {
                            isSelectedCage -> selectedCageLineColor
                            isSettled -> settledCageLineColor
                            else -> cageLineColor
                        },
                        labelRect = labelRect
                    )
                }
            }
        }
    }
}


@LightDarkPreview
@Composable
private fun BoardPreviewLight() {
    LibreSudokuTheme {
        Surface {
            val sudokuParser = SudokuParser()
            val board by remember {
                mutableStateOf(
                    sudokuParser.parseBoard(
                        board = "....1........4.............7...........9........68...............5...............",
                        gameType = GameType.Default9x9,
                        emptySeparator = '.'
                    ).toList()
                )
            }
            val notes = listOf(Note(2, 3, 1), Note(2, 3, 5))
            Board(
                board = board,
                notes = notes,
                selectedCell = Cell(-1, -1),
                onClick = { },
                boardColors = SudokuBoardColorsImpl(
                    foregroundColor = BoardColors.foregroundColor,
                    notesColor = BoardColors.notesColor,
                    altForegroundColor = BoardColors.altForegroundColor,
                    errorColor = BoardColors.errorColor,
                    highlightColor = BoardColors.highlightColor,
                    thickLineColor = BoardColors.thickLineColor,
                    thinLineColor = BoardColors.thinLineColor
                )
            )
        }
    }
}

/** Placed digits are set a little smaller than the old sans: the serif's figures run larger */
private const val INK_SIZE_FACTOR = 0.8f

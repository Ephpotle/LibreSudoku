package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

fun DrawScope.drawRoundCell(
    row: Int,
    col: Int,
    gameSize: Int,
    rect: Rect,
    color: Color,
    cornerRadius: CornerRadius = CornerRadius.Zero
) {
    val path = Path().apply {
        addRoundRect(
            roundRectForCell(
                row = row,
                col = col,
                gameSize = gameSize,
                rect = rect,
                cornerRadius = cornerRadius
            )
        )
    }
    drawPath(
        path = path,
        color = color
    )
}

fun DrawScope.drawNotes(
    size: Int,
    paint: Paint,
    highlightPaint: Paint,
    notes: List<Note>,
    notesToHighlight: List<Note>,
    cellSize: Float,
    cellSizeDivWidth: Float,
    killerSumBounds: android.graphics.Rect,
) {
    val noteBounds = android.graphics.Rect()

    paint.getTextBounds("1", 0, 1, noteBounds)
    val cellDivHeight = (cellSize - killerSumBounds.height() * 1.5f) / floor(sqrt(size.toFloat()))

    drawIntoCanvas { canvas ->
        notes.forEach { note ->
            val textToDraw = note.value.toString(16).uppercase()
            val noteTextMeasure = paint.measureText(textToDraw)

            val noteCol = getNoteColumnNumber(note.value, size)
            val noteRow = getNoteRowNumber(note.value, size)



            val horizontalPadding =
                if (noteRow == 0) noteTextMeasure / 3f
                else if (noteRow == 1) 0f
                else if (noteRow == 2 && note.value > 9) 0f
                else -(noteTextMeasure / 3f)


            if (notesToHighlight.contains(note)) {
                canvas.nativeCanvas.drawCircle(
                    note.col * cellSize + cellSizeDivWidth / 2f + (cellSizeDivWidth * noteRow) + horizontalPadding,
                    note.row * cellSize + noteBounds.height() * 1.5f + killerSumBounds.height() + (cellDivHeight * noteCol) - (noteBounds.height() * 0.5f),
                    noteTextMeasure * 1.1f,
                    highlightPaint
                )
            }

            canvas.nativeCanvas.drawText(
                textToDraw,
                note.col * cellSize + cellSizeDivWidth / 2f + (cellSizeDivWidth * noteRow) - noteTextMeasure / 2f + horizontalPadding,
                note.row * cellSize + noteBounds.height() * 1.5f + killerSumBounds.height() + (cellDivHeight * noteCol),
                paint
            )
        }
    }
}

/**
 * Faint dots where notes will be written in an empty cell, shown while notes mode is on so the
 * input mode can be seen on the board itself. [topInset] leaves room for a killer cage sum.
 */
fun DrawScope.drawNotesCursor(
    row: Int,
    col: Int,
    gameSize: Int,
    cellSize: Float,
    topInset: Float,
    color: Color
) {
    val columns = ceil(sqrt(gameSize.toFloat())).toInt()
    val rows = floor(sqrt(gameSize.toFloat())).toInt()
    val left = col * cellSize
    val top = row * cellSize + topInset
    val stepX = cellSize / columns
    val stepY = (cellSize - topInset) / rows
    val radius = cellSize * 0.028f
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(left + stepX * (c + 0.5f), top + stepY * (r + 0.5f))
            )
        }
    }
}

fun DrawScope.drawNumbers(
    size: Int,
    board: List<List<Cell>>,
    highlightErrors: Boolean,
    errorTextPaint: Paint,
    lockedTextPaint: Paint,
    textPaint: Paint,
    questions: Boolean,
    cellSize: Float,
    poppedCell: Pair<Int, Int>? = null,
    poppedScale: Float = 1f
) {
    drawIntoCanvas { canvas ->
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j].value != 0) {
                    val paint = when {
                        board[i][j].error && highlightErrors -> errorTextPaint
                        board[i][j].locked -> lockedTextPaint
                        else -> textPaint
                    }

                    val textToDraw =
                        if (questions) "?" else board[i][j].value.toString(16).uppercase()
                    val textBounds = android.graphics.Rect()
                    textPaint.getTextBounds(textToDraw, 0, 1, textBounds)
                    val textWidth = paint.measureText(textToDraw)

                    val popped = poppedScale != 1f && poppedCell != null &&
                            poppedCell.first == i && poppedCell.second == j
                    if (popped) {
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.scale(
                            poppedScale,
                            poppedScale,
                            (j + 0.5f) * cellSize,
                            (i + 0.5f) * cellSize
                        )
                    }
                    canvas.nativeCanvas.drawText(
                        textToDraw,
                        board[i][j].col * cellSize + (cellSize - textWidth) / 2f,
                        board[i][j].row * cellSize + (cellSize + textBounds.height()) / 2f,
                        paint
                    )
                    if (popped) canvas.nativeCanvas.restore()
                }
            }
        }
    }
}

fun DrawScope.drawBoardFrame(
    thickLineColor: Color,
    thickLineWidth: Float,
    maxWidth: Float,
    cornerRadius: CornerRadius
) {
    drawRoundRect(
        color = thickLineColor,
        topLeft = Offset.Zero,
        size = Size(maxWidth, maxWidth),
        cornerRadius = cornerRadius,
        style = Stroke(width = thickLineWidth)
    )
}

fun roundRectForCell(
    row: Int,
    col: Int,
    gameSize: Int,
    rect: Rect,
    cornerRadius: CornerRadius
): RoundRect {
    val topLeft = if (row == 0 && col == 0) cornerRadius else CornerRadius.Zero
    val topRight = if (row == 0 && col == gameSize - 1) cornerRadius else CornerRadius.Zero
    val bottomLeft = if (row == gameSize - 1 && col == 0) cornerRadius else CornerRadius.Zero
    val bottomRight =
        if (row == gameSize - 1 && col == gameSize - 1) cornerRadius else CornerRadius.Zero

    return RoundRect(
        rect = rect,
        topLeft = topLeft,
        topRight = topRight,
        bottomLeft = bottomLeft,
        bottomRight = bottomRight
    )
}

fun DrawScope.drawCrossSelection(
    gameSize: Int,
    sectionHeight: Int,
    sectionWidth: Int,
    color: Color,
    cellSize: Float
) {
    for (i in 0 until gameSize / sectionWidth) {
        for (j in 0 until gameSize / sectionHeight) {
            if ((i % 2 == 0 && j % 2 != 0) || (i % 2 != 0 && j % 2 == 0)) {
                drawRect(
                    color = color,
                    topLeft = Offset(
                        x = i * sectionWidth * cellSize,
                        y = j * sectionHeight * cellSize
                    ),
                    size = Size(cellSize * sectionWidth, cellSize * sectionHeight)
                )
            }
        }
    }
}
package com.kaajjo.libresudoku.ui.game.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

/** The serif used for placed digits on the board, so keys and board speak the same "ink" */
val InkFontFamily = FontFamily(Font(R.font.ink_digits))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardItem(
    modifier: Modifier = Modifier,
    number: Int,
    remainingUses: Int? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit = { },
    selected: Boolean = false,
    notesMode: Boolean = false
) {
    val mutableInteractionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by mutableInteractionSource.collectIsPressedAsState()
    val colorScheme = MaterialTheme.colorScheme
    val inkColor = LocalBoardColors.current.foregroundColor
    val color by animateColorAsState(
        targetValue = when {
            selected -> colorScheme.primaryContainer
            notesMode -> colorScheme.surface
            else -> colorScheme.surfaceColorAtElevation(3.dp)
        },
        label = "Key background"
    )
    // digits on the keys are in the same ink as on the board; in notes mode they turn to
    // pencil and the key gets an outline instead of a fill
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> colorScheme.onPrimaryContainer
            notesMode -> colorScheme.onSurfaceVariant
            else -> inkColor
        },
        label = "Key text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (notesMode && !selected) colorScheme.outline else Color.Transparent,
        label = "Key outline"
    )
    // keys sink a little while pressed, so a tap is felt as well as seen
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "Key press"
    )
    val localView = LocalView.current
    val fullFontSize = if (remainingUses != null) 25f else 34f
    // in notes mode the digits shrink, like the pencil marks they will make
    val keyboardFontSize by animateFloatAsState(
        targetValue = if (notesMode) fullFontSize * 0.62f else fullFontSize,
        label = "Key digit size"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = mutableInteractionSource,
                onClick = {
                    // ink gets a firm click, pencil a light tick
                    localView.performHapticFeedback(
                        if (notesMode) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.VIRTUAL_KEY
                    )
                    onClick(number)
                },
                onLongClick = {
                    localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLongClick(number)
                },
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = number.toString(16).uppercase(),
                // ink serif for digits, plain sans "pencil" in notes mode
                fontFamily = if (notesMode) FontFamily.Default else InkFontFamily,
                fontWeight = if (notesMode) FontWeight.Medium else FontWeight.Normal,
                fontSize = keyboardFontSize.sp,
                lineHeight = fullFontSize.sp,
                color = textColor
            )
            if (remainingUses != null) {
                Text(
                    text = remainingUses.toString(),
                    fontSize = 11.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun DefaultGameKeyboard(
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    remainingUses: List<Int>? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    size: Int,
    selected: Int = 0,
    notesMode: Boolean = false
) {
    val numbers by remember(size) { mutableStateOf((1..size).toList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (size == GameType.Default12x12.size) {
            // double-height keyboard only for 12x12
            val chunkedNumbers = numbers.chunked(6)
            if (chunkedNumbers.size == 2) {
                chunkedNumbers.forEachIndexed { index, chunked ->
                    AnimatedVisibility(
                        visible =
                        (remainingUses != null && remainingUses.chunked(6)[index].any { it > 0 }) ||
                                remainingUses == null
                    ) {
                        KeyboardRow {
                            chunked.forEach { number ->
                                val hide =
                                    remainingUses != null && (remainingUses.size >= number && remainingUses[number - 1] <= 0)
                                KeyboardItem(
                                    modifier = itemModifier
                                        .weight(1f)
                                        .alpha(if (hide) 0f else 1f),
                                    number = number,
                                    onClick = {
                                        if (!hide) {
                                            onClick(number)
                                        }
                                    },
                                    onLongClick = {
                                        if (!hide) {
                                            onLongClick(number)
                                        }
                                    },
                                    remainingUses = if (remainingUses != null && remainingUses.size >= number) {
                                        remainingUses[number - 1]
                                    } else {
                                        null
                                    },
                                    selected = number == selected,
                                    notesMode = notesMode
                                )
                            }
                        }
                    }
                }
            }
        } else {
            KeyboardRow(modifier = modifier) {
                numbers.forEach { number ->
                    val hide =
                        remainingUses != null && (remainingUses.size >= number && remainingUses[number - 1] <= 0)
                    KeyboardItem(
                        modifier = itemModifier
                            .weight(1f)
                            .alpha(if (hide) 0f else 1f),
                        number = number,
                        onClick = {
                            if (!hide) {
                                onClick(number)
                            }
                        },
                        onLongClick = {
                            if (!hide) {
                                onLongClick(number)
                            }
                        },
                        remainingUses = if (remainingUses != null && remainingUses.size >= number) {
                            remainingUses[number - 1]
                        } else {
                            null
                        },
                        selected = number == selected,
                        notesMode = notesMode
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}


@LightDarkPreview
@Composable
private fun KeyboardItemPreview() {
    LibreSudokuTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                KeyboardItem(
                    number = 1,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    selected = true,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    remainingUses = 5,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    remainingUses = 5,
                    selected = true,
                    onClick = { }
                )
            }
        }
    }
}


@LightDarkPreview
@Composable
private fun KeyboardPreview9x9() {
    LibreSudokuTheme {
        Surface {
            DefaultGameKeyboard(
                onClick = { },
                onLongClick = { },
                size = 9,
                remainingUses = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun KeyboardPreview12x12() {
    LibreSudokuTheme {
        Surface {
            DefaultGameKeyboard(
                onClick = { },
                onLongClick = { },
                size = 12,
                remainingUses = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            )
        }
    }
}

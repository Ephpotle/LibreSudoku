package com.kaajjo.libresudoku.ui.game.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

enum class ToolBarItem {
    Undo,
    Hint,
    Note,
    Remove,
    Redo
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarItem(
    modifier: Modifier = Modifier,
    painter: Painter,
    toggled: Boolean = false,
    label: String? = null,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { }
) {
    val localView = LocalView.current
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (toggled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .combinedClickable(
                onClick = {
                    localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = if (toggled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (toggled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun KeyboardItemPreview() {
    LibreSudokuTheme {
        Surface {
            Row {
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_round_edit_24)
                )
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_round_edit_24),
                    toggled = true
                )
            }
        }
    }
}

package com.kaajjo.libresudoku.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Puzzle-book paper and graphite print. Killer boards are drawn in these fixed colours, and the
 * game screen around them is too, so the board, keys and pills read as one printed page whatever
 * the wallpaper. The theme's own colours stay for ink: placed digits, the selected key, toggles.
 */
object Paper {
    val Light = Color(0xFFF8F4EA)
    val Dark = Color(0xFF22201C)
    val PrintLight = Color(0xFF2B2926)
    val PrintDark = Color(0xFFDAD3C4)

    fun isDark(scheme: ColorScheme) = scheme.surface.luminance() < 0.5f
}

/** Re-skins [content] onto paper when [enabled]; otherwise leaves the theme untouched */
@Composable
fun PaperTheme(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val base = MaterialTheme.colorScheme
    val scheme = remember(base) { paperScheme(base) }
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

private fun paperScheme(base: ColorScheme): ColorScheme =
    if (Paper.isDark(base)) {
        base.copy(
            background = Paper.Dark,
            onBackground = Color(0xFFE8E2D4),
            surface = Paper.Dark,
            onSurface = Color(0xFFE8E2D4),
            surfaceVariant = Color(0xFF3A372F),
            onSurfaceVariant = Color(0xFFC3BCAD),
            // elevation tints surfaces with this, so raised parts turn a warmer grey, not violet
            surfaceTint = Color(0xFFA89F8A),
            surfaceBright = Color(0xFF3A3731),
            surfaceDim = Paper.Dark,
            surfaceContainerLowest = Color(0xFF1C1A17),
            surfaceContainerLow = Color(0xFF282621),
            surfaceContainer = Color(0xFF2C2A25),
            surfaceContainerHigh = Color(0xFF34312B),
            surfaceContainerHighest = Color(0xFF3E3B34),
            outline = Color(0xFF8E8778),
            outlineVariant = Color(0xFF4A463E)
        )
    } else {
        base.copy(
            background = Paper.Light,
            onBackground = Paper.PrintLight,
            surface = Paper.Light,
            onSurface = Paper.PrintLight,
            surfaceVariant = Color(0xFFE6DFD0),
            onSurfaceVariant = Color(0xFF5C574E),
            surfaceTint = Color(0xFF8A7F68),
            surfaceBright = Color(0xFFFCFAF4),
            surfaceDim = Color(0xFFE2DCCE),
            surfaceContainerLowest = Color(0xFFFFFDF8),
            surfaceContainerLow = Color(0xFFF3EFE4),
            surfaceContainer = Color(0xFFEFEADF),
            surfaceContainerHigh = Color(0xFFE9E3D6),
            surfaceContainerHighest = Color(0xFFE2DCCE),
            outline = Color(0xFF8A8476),
            outlineVariant = Color(0xFFCFC8B8)
        )
    }

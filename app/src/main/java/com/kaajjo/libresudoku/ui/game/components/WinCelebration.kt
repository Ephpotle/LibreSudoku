package com.kaajjo.libresudoku.ui.game.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val CONFETTI_COUNT = 140
private const val CONFETTI_SECONDS = 3.2f
private const val FADE_SECONDS = 0.8f

// Physics in screen heights per second, with linear drag so pieces drift down slowly
private const val GRAVITY = 1.6f
private const val DRAG = 1.7f

private class ConfettiPiece(
    val fromLeft: Boolean,
    val vx: Float,
    val vy: Float,
    val delay: Float,
    val spin: Float,
    val flip: Float,
    val phase: Float,
    val widthFactor: Float,
    val colorIndex: Int
)

/**
 * Two confetti bursts from the bottom corners of the screen. Draws nothing until [trigger]
 * becomes positive, and replays each time it increases. Touches pass through to the game.
 * Skipped when the system animator duration scale is 0 (animations turned off).
 */
@Composable
fun WinConfetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val context = LocalContext.current
    if (animationsDisabled(context)) return

    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.primaryContainer,
            colorScheme.tertiaryContainer,
            Color(0xFFFFC94D),
            Color(0xFFFF7A90),
            Color(0xFF6BCBFF)
        )
    }
    val pieces = remember(trigger) {
        val random = Random(trigger * 7919L + System.nanoTime())
        List(CONFETTI_COUNT) { i ->
            val fromLeft = i % 2 == 0
            // Aim up and toward the middle: 55 to 80 degrees above horizontal
            val angle = (55f + random.nextFloat() * 25f) * PI.toFloat() / 180f
            val speed = 1.5f + random.nextFloat() * 1.1f
            ConfettiPiece(
                fromLeft = fromLeft,
                vx = cos(angle) * speed * 0.55f * (if (fromLeft) 1f else -1f),
                vy = -sin(angle) * speed,
                delay = random.nextFloat() * 0.25f,
                spin = (random.nextFloat() - 0.5f) * 900f,
                flip = 4f + random.nextFloat() * 8f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                widthFactor = 0.6f + random.nextFloat() * 0.6f,
                colorIndex = random.nextInt(colors.size)
            )
        }
    }
    val time = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        time.animateTo(
            targetValue = CONFETTI_SECONDS,
            animationSpec = tween((CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing)
        )
    }
    val pieceSize = with(LocalDensity.current) { Size(7.dp.toPx(), 12.dp.toPx()) }

    Canvas(modifier = modifier) {
        val t = time.value
        if (t >= CONFETTI_SECONDS) return@Canvas
        val h = size.height
        val alpha = ((CONFETTI_SECONDS - t) / FADE_SECONDS).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val pt = t - p.delay
            if (pt <= 0f) return@forEach
            val damp = (1f - exp(-DRAG * pt)) / DRAG
            val x = (if (p.fromLeft) 0f else size.width) + p.vx * h * damp
            val y = h + ((p.vy - GRAVITY / DRAG) * damp + GRAVITY * pt / DRAG) * h
            val w = pieceSize.width * p.widthFactor
            translate(x, y) {
                rotate(p.spin * pt, pivot = Offset.Zero) {
                    // Squash horizontally over time so pieces look like they flutter
                    scale(scaleX = cos(p.phase + p.flip * pt), scaleY = 1f, pivot = Offset.Zero) {
                        drawRoundRect(
                            color = colors[p.colorIndex],
                            topLeft = Offset(-w / 2f, -pieceSize.height / 2f),
                            size = Size(w, pieceSize.height),
                            cornerRadius = CornerRadius(w / 4f),
                            alpha = alpha
                        )
                    }
                }
            }
        }
    }
}

private fun animationsDisabled(context: Context): Boolean = Settings.Global.getFloat(
    context.contentResolver,
    Settings.Global.ANIMATOR_DURATION_SCALE,
    1f
) == 0f

/**
 * Plays the short win chime at a low volume. Does nothing when the ringer is on
 * silent or vibrate, so the sound follows the phone's silent mode.
 */
fun playWinChime(context: Context) {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return
    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
    val player = MediaPlayer()
    try {
        context.resources.openRawResourceFd(R.raw.win_chime).use { fd ->
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        }
        player.setVolume(WIN_CHIME_VOLUME, WIN_CHIME_VOLUME)
        player.setOnCompletionListener { it.release() }
        player.setOnErrorListener { mp, _, _ -> mp.release(); true }
        player.prepare()
        player.start()
    } catch (e: Exception) {
        player.release()
    }
}

private const val WIN_CHIME_VOLUME = 0.4f

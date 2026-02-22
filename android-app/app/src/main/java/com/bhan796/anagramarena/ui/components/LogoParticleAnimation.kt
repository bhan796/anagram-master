package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorGreen
import com.bhan796.anagramarena.ui.theme.ColorMagenta
import com.bhan796.anagramarena.ui.theme.sdp
import kotlinx.coroutines.delay

private const val TOP_WORD = "ANAGRAM"
private const val BOTTOM_WORD = "ARENA"
private const val TOP_STAGGER_MS = 90
private const val SLIDE_DURATION_MS = 520
private const val BOTTOM_START_MS = TOP_WORD.length * TOP_STAGGER_MS - 140

@Composable
fun LogoParticleAnimation(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val palette = remember { listOf(ColorCyan, ColorGold, ColorMagenta, ColorGreen) }
    var active by remember { mutableStateOf(false) }

    val topLetters = remember {
        TOP_WORD.mapIndexed { index, letter ->
            AnimatedLogoLetter(
                char = letter,
                accentIndex = index % palette.size,
                delayMs = index * TOP_STAGGER_MS
            )
        }
    }

    val bottomLetters = remember {
        BOTTOM_WORD.mapIndexed { index, letter ->
            AnimatedLogoLetter(
                char = letter,
                accentIndex = (index + 2) % palette.size,
                delayMs = BOTTOM_START_MS + index * TOP_STAGGER_MS
            )
        }
    }

    LaunchedEffect(Unit) {
        SoundManager.playLogoAssemble()
        active = true

        val maxDelayMs = (topLetters.maxOfOrNull { it.delayMs } ?: 0).coerceAtLeast(bottomLetters.maxOfOrNull { it.delayMs } ?: 0)
        val doneAtMs = maxDelayMs + SLIDE_DURATION_MS + 70
        delay((doneAtMs - 170).coerceAtLeast(0).toLong())
        SoundManager.playMatchFound()
        delay(170)
        onComplete()
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().heightIn(min = sdp(120.dp))) {
        val density = LocalDensity.current
        val offscreenX = with(density) { maxWidth.toPx() + 120.dp.toPx() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(sdp(8.dp))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                topLetters.forEachIndexed { index, letter ->
                    SlidingTile(
                        letter = letter.char.toString(),
                        accent = palette[letter.accentIndex],
                        index = index,
                        delayMs = letter.delayMs,
                        active = active,
                        offscreenX = offscreenX
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                bottomLetters.forEachIndexed { index, letter ->
                    SlidingTile(
                        letter = letter.char.toString(),
                        accent = palette[letter.accentIndex],
                        index = index + TOP_WORD.length,
                        delayMs = letter.delayMs,
                        active = active,
                        offscreenX = offscreenX
                    )
                }
            }
        }
    }
}

private data class AnimatedLogoLetter(
    val char: Char,
    val accentIndex: Int,
    val delayMs: Int
)

@Composable
private fun SlidingTile(
    letter: String,
    accent: androidx.compose.ui.graphics.Color,
    index: Int,
    delayMs: Int,
    active: Boolean,
    offscreenX: Float
) {
    val translateX by animateFloatAsState(
        targetValue = if (active) 0f else -offscreenX,
        animationSpec = tween(
            durationMillis = SLIDE_DURATION_MS,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        label = "logoSlideX_$index"
    )

    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220, delayMillis = delayMs),
        label = "logoAlpha_$index"
    )

    LetterTile(
        letter = letter,
        revealed = true,
        index = index,
        accentColor = accent,
        modifier = Modifier
            .graphicsLayer { translationX = translateX }
            .alpha(alpha)
            .heightIn(min = sdp(34.dp))
    )
}

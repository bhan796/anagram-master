package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.online.AvatarState
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.AvatarSprite
import com.bhan796.anagramarena.ui.components.CosmeticName
import com.bhan796.anagramarena.ui.components.Facing
import com.bhan796.anagramarena.ui.components.RankBadge
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorMagenta
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.ui.theme.sdp
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class EntryDirection { LEFT, RIGHT }

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
    val decay: Float
)

@Composable
private fun ArcadeParticles() {
    var particles by remember {
        mutableStateOf(
            List(40) {
                val angle = Random.nextFloat() * (Math.PI * 2f).toFloat()
                val speed = 0.8f + Random.nextFloat() * 2.4f
                Particle(
                    x = 0.5f,
                    y = 0.5f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = 2f + Random.nextFloat() * 4f,
                    color = listOf(ColorCyan, ColorGold, ColorMagenta, Color(0xFF39FF14)).random(),
                    alpha = 1f,
                    decay = 0.012f + Random.nextFloat() * 0.012f
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        while (particles.any { it.alpha > 0.02f }) {
            particles = particles.map {
                it.copy(
                    x = it.x + it.vx / 320f,
                    y = it.y + it.vy / 320f,
                    vy = it.vy + 0.04f,
                    alpha = (it.alpha - it.decay).coerceAtLeast(0f)
                )
            }
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(size.width * p.x, size.height * p.y),
                style = Fill
            )
        }
    }
}

@Composable
private fun AnimatedRating(target: Int, color: Color) {
    val displayed = remember(target) { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        displayed.intValue = 0
        val step = ceil(target / 40f).toInt().coerceAtLeast(1)
        var current = 0
        while (current < target) {
            current = (current + step).coerceAtMost(target)
            displayed.intValue = current
            delay(16)
        }
    }
    Text(text = displayed.intValue.toString(), style = MaterialTheme.typography.headlineLarge, color = color)
}

@Composable
private fun AnimatedAvatarEntry(
    avatarId: String,
    entryDirection: EntryDirection,
    state: AvatarState,
    scale: Float,
    facing: Facing = Facing.RIGHT
) {
    val offsetX = remember { Animatable(if (entryDirection == EntryDirection.LEFT) -600f else 600f) }

    LaunchedEffect(Unit) {
        delay(200)
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        )
    }

    Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) {
        AvatarSprite(avatarId = avatarId, state = state, scale = scale, facing = facing)
    }
}

@Composable
fun MatchFoundScreen(contentPadding: PaddingValues, state: OnlineUiState, onDone: () -> Unit) {
    val countdownState = remember { mutableIntStateOf(10) }
    var showFight by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    var battleState by remember { mutableStateOf(AvatarState.BATTLE) }
    var attackTriggered by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val matchScale = when {
        config.screenWidthDp < 400 -> 3f
        config.screenWidthDp <= 600 -> 4f
        else -> 6f
    }

    LaunchedEffect(Unit) {
        SoundManager.playMatchFound()
        val endAt = System.currentTimeMillis() + 10_000
        while (!completed) {
            val remainingMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0)
            val remaining = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
            if (countdownState.intValue != remaining) {
                countdownState.intValue = remaining
            }
            if (remaining <= 0) {
                break
            }
            delay(200)
        }
        showFight = true
    }

    LaunchedEffect(Unit) {
        delay(800)
        listOf(-5f, 5f, -4f, 4f, -2f, 2f, 0f).forEach { target ->
            shakeOffset.animateTo(target, animationSpec = tween(55))
        }
    }

    LaunchedEffect(countdownState.intValue) {
        if (countdownState.intValue > 0) {
            SoundManager.playCountdownBeep()
        } else if (!completed) {
            completed = true
            SoundManager.playCountdownGo()
            delay(700)
            onDone()
        }

        if (countdownState.intValue <= 5 && countdownState.intValue > 0 && !attackTriggered) {
            attackTriggered = true
            battleState = AvatarState.ATTACK
            delay(400)
            battleState = AvatarState.BATTLE
        }
    }

    val me = state.myPlayer
    val opp = state.opponentPlayer

    val pulse = rememberInfiniteTransition(label = "matchFoundPulse")
    val titleScale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "titleScale"
    )

    ArcadeScaffold(contentPadding = contentPadding) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.14f)).alpha(0.2f))
            ArcadeParticles()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(horizontal = sdp(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedAvatarEntry(
                        avatarId = state.myAvatarId,
                        entryDirection = EntryDirection.LEFT,
                        state = battleState,
                        scale = matchScale,
                        facing = Facing.RIGHT
                    )
                }

                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(sdp(12.dp))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier) {
                        Text("MATCH", style = MaterialTheme.typography.displayMedium, color = ColorCyan, textAlign = TextAlign.Center)
                        Text("FOUND!", style = MaterialTheme.typography.displayMedium, color = ColorGold, textAlign = TextAlign.Center)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(sdp(8.dp), Alignment.CenterHorizontally)
                    ) {
                        Box(modifier = Modifier.weight(1f).size(sdp(2.dp)).background(ColorGold.copy(alpha = 0.7f)))
                        Text("VS", style = MaterialTheme.typography.headlineLarge, color = ColorGold)
                        Box(modifier = Modifier.weight(1f).size(sdp(2.dp)).background(ColorGold.copy(alpha = 0.7f)))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(8.dp)))
                                .border(sdp(1.5.dp), ColorCyan, RoundedCornerShape(sdp(8.dp)))
                                .padding(sdp(12.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(sdp(6.dp))
                        ) {
                            Text("YOU", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                            CosmeticName(me?.displayName ?: "You", me?.equippedCosmetic, style = MaterialTheme.typography.labelMedium.copy(color = ColorWhite))
                            AnimatedRating(target = me?.rating ?: 1000, color = ColorCyan)
                            RankBadge(tier = me?.rankTier ?: "silver")
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(8.dp)))
                                .border(sdp(1.5.dp), ColorGold, RoundedCornerShape(sdp(8.dp)))
                                .padding(sdp(12.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(sdp(6.dp))
                        ) {
                            Text("OPP", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                            CosmeticName(opp?.displayName ?: "Opponent", opp?.equippedCosmetic, style = MaterialTheme.typography.labelMedium.copy(color = ColorWhite))
                            AnimatedRating(target = opp?.rating ?: 1000, color = ColorGold)
                            RankBadge(tier = opp?.rankTier ?: "silver")
                        }
                    }

                    if (showFight) {
                        Text("FIGHT!", style = MaterialTheme.typography.displayMedium, color = ColorMagenta)
                    } else {
                        Text("ENTERING ARENA IN", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                        Text(
                            countdownState.intValue.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = if (countdownState.intValue <= 3) ColorRed else ColorCyan
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedAvatarEntry(
                        avatarId = state.oppAvatarId,
                        entryDirection = EntryDirection.RIGHT,
                        state = battleState,
                        scale = matchScale,
                        facing = Facing.LEFT
                    )
                }
            }
        }
    }
}

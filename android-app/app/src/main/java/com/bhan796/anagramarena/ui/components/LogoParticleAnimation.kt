package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorGreen
import com.bhan796.anagramarena.ui.theme.ColorMagenta
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.launch

private const val LOGO_TEXT_TOP = "ANAGRAM"
private const val LOGO_TEXT_BOTTOM = "ARENA"

private const val LETTER_WIDTH = 5
private const val LETTER_HEIGHT = 7
private const val LETTER_GAP_PX = 3
private const val ROW_GAP_PX = 6
private const val SUBDIV = 3

private const val SPRING_K = 0.08f
private const val DAMPING = 0.78f
private const val SNAP_DIST = 1.5f
private const val HARD_SNAP_DIST = 2f
private const val SCATTER_FADE_MS = 80L
private const val DRIFT_END_MS = 400L
private const val CONVERGE_START_MS = 200L
private const val SNAP_MS = 1400L
private const val BLOOM_END_MS = 1900L
private const val HANDOFF_END_MS = 2200L

private val BLOOM_ALPHAS = floatArrayOf(0.12f, 0.20f, 0.35f, 1.0f)
private val BLOOM_SCALES = floatArrayOf(2.5f, 2.0f, 1.5f, 1.0f)

private val LETTER_BITMAPS: Map<Char, Array<String>> = mapOf(
    'A' to arrayOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
    'N' to arrayOf("#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"),
    'G' to arrayOf(".###.", "#....", "#.###", "#...#", "#...#", "#...#", ".###."),
    'R' to arrayOf("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
    'M' to arrayOf("#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"),
    'E' to arrayOf("#####", "#....", "#....", "####.", "#....", "#....", "#####")
)

@Composable
fun LogoParticleAnimation(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 140.dp,
    onComplete: () -> Unit = {}
) {
    val density = LocalDensity.current
    val palette = remember { listOf(ColorCyan, ColorGold, ColorMagenta, ColorGreen) }
    val random = remember { Random(System.currentTimeMillis()) }

    BoxWithConstraints(modifier = modifier.height(logoHeight)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { logoHeight.toPx() }
        val subParticlePx = with(density) { 2.dp.toPx() }
        val logicalPixelPx = subParticlePx * SUBDIV

        val topRowWidthCells = LOGO_TEXT_TOP.length * LETTER_WIDTH + (LOGO_TEXT_TOP.length - 1) * LETTER_GAP_PX
        val bottomRowWidthCells = LOGO_TEXT_BOTTOM.length * LETTER_WIDTH + (LOGO_TEXT_BOTTOM.length - 1) * LETTER_GAP_PX
        val logoWidthCells = max(topRowWidthCells, bottomRowWidthCells)
        val logoHeightCells = LETTER_HEIGHT * 2 + ROW_GAP_PX

        val logoWidthPx = logoWidthCells * logicalPixelPx
        val logoHeightPx = logoHeightCells * logicalPixelPx
        val logoLeft = (widthPx - logoWidthPx) * 0.5f
        val logoTop = (heightPx - logoHeightPx) * 0.5f
        val logoCenterX = logoLeft + logoWidthPx * 0.5f
        val logoCenterY = logoTop + logoHeightPx * 0.5f

        val particles = remember(widthPx, heightPx) {
            buildParticles(
                widthPx = widthPx,
                heightPx = heightPx,
                subParticlePx = subParticlePx,
                logicalPixelPx = logicalPixelPx,
                logoLeft = logoLeft,
                logoTop = logoTop,
                logoCenterX = logoCenterX,
                logoCenterY = logoCenterY,
                palette = palette,
                random = random
            )
        }

        var frameCount by remember { mutableIntStateOf(0) }
        var elapsedMs by remember { mutableLongStateOf(0L) }
        var scatterAlpha by remember { mutableFloatStateOf(0f) }
        var snapFlashAlpha by remember { mutableFloatStateOf(0f) }
        var inBloomPhase by remember { mutableStateOf(false) }
        var snapTriggered by remember { mutableStateOf(false) }
        var handoffTriggered by remember { mutableStateOf(false) }

        val canvasAlpha = remember { Animatable(1f) }
        val tileLogoAlpha = remember { Animatable(0f) }
        val flashAnim = remember { Animatable(0f) }

        LaunchedEffect(particles.count) {
            SoundManager.playLogoAssemble()
            val t0 = withFrameNanos { it } / 1_000_000L

            while (elapsedMs < HANDOFF_END_MS) {
                val frameTime = withFrameNanos { it } / 1_000_000L
                elapsedMs = frameTime - t0
                scatterAlpha = (elapsedMs.toFloat() / SCATTER_FADE_MS).coerceIn(0f, 1f)

                updateParticles(
                    particles = particles,
                    elapsedMs = elapsedMs,
                    widthPx = widthPx,
                    heightPx = heightPx
                )

                if (!snapTriggered && elapsedMs >= SNAP_MS) {
                    snapTriggered = true
                    inBloomPhase = true
                    SoundManager.playMatchFound()
                    hardSnapCloseParticles(particles)
                    launch {
                        flashAnim.snapTo(1f)
                        flashAnim.animateTo(0f, tween(durationMillis = 150))
                    }
                }

                snapFlashAlpha = flashAnim.value
                if (inBloomPhase && elapsedMs >= BLOOM_END_MS) {
                    inBloomPhase = false
                }

                if (!handoffTriggered && elapsedMs >= BLOOM_END_MS) {
                    handoffTriggered = true
                    launch { canvasAlpha.animateTo(0f, tween(durationMillis = 300)) }
                    launch { tileLogoAlpha.animateTo(1f, tween(durationMillis = 300)) }
                }

                frameCount++
            }

            onComplete()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = canvasAlpha.value }
            ) {
                val frameTick = frameCount
                if (frameTick == Int.MIN_VALUE) return@Canvas
                val phaseAlpha = if (elapsedMs <= SCATTER_FADE_MS) scatterAlpha else 1f

                if (inBloomPhase) {
                    for (i in 0 until particles.count) {
                        val cx = particles.px[i]
                        val cy = particles.py[i]
                        val particleColor = particles.colors[i]
                        val particleSize = particles.size[i]
                        for (b in 0..3) {
                            val drawSize = particleSize * BLOOM_SCALES[b]
                            drawRect(
                                color = particleColor.copy(alpha = BLOOM_ALPHAS[b]),
                                topLeft = Offset(cx - drawSize * 0.5f, cy - drawSize * 0.5f),
                                size = Size(drawSize, drawSize)
                            )
                        }
                    }
                } else {
                    for (i in 0 until particles.count) {
                        val particleSize = particles.size[i]
                        drawRect(
                            color = particles.colors[i].copy(alpha = phaseAlpha),
                            topLeft = Offset(
                                particles.px[i] - particleSize * 0.5f,
                                particles.py[i] - particleSize * 0.5f
                            ),
                            size = Size(particleSize, particleSize)
                        )
                    }
                }

                if (snapFlashAlpha > 0f) {
                    drawRect(
                        color = Color.White.copy(alpha = snapFlashAlpha),
                        topLeft = Offset(logoLeft, logoTop),
                        size = Size(logoWidthPx, logoHeightPx)
                    )
                }
            }

            TileLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = tileLogoAlpha.value }
            )
        }
    }
}

private class ParticleState(
    val count: Int,
    val px: FloatArray,
    val py: FloatArray,
    val tx: FloatArray,
    val ty: FloatArray,
    val vx: FloatArray,
    val vy: FloatArray,
    val colors: Array<Color>,
    val delay: LongArray,
    val size: FloatArray,
    val driftAmpX: FloatArray,
    val driftAmpY: FloatArray,
    val driftPhaseX: FloatArray,
    val driftPhaseY: FloatArray
)

private fun buildParticles(
    widthPx: Float,
    heightPx: Float,
    subParticlePx: Float,
    logicalPixelPx: Float,
    logoLeft: Float,
    logoTop: Float,
    logoCenterX: Float,
    logoCenterY: Float,
    palette: List<Color>,
    random: Random
): ParticleState {
    val targetX = ArrayList<Float>(2600)
    val targetY = ArrayList<Float>(2600)
    val targetColor = ArrayList<Color>(2600)

    fun addWord(word: String, rowIndex: Int, letterBaseIndex: Int) {
        val rowWidthCells = word.length * LETTER_WIDTH + (word.length - 1) * LETTER_GAP_PX
        val maxWidthCells = max(
            LOGO_TEXT_TOP.length * LETTER_WIDTH + (LOGO_TEXT_TOP.length - 1) * LETTER_GAP_PX,
            LOGO_TEXT_BOTTOM.length * LETTER_WIDTH + (LOGO_TEXT_BOTTOM.length - 1) * LETTER_GAP_PX
        )
        val rowLeftCells = (maxWidthCells - rowWidthCells) * 0.5f
        val rowTopCells = if (rowIndex == 0) 0f else (LETTER_HEIGHT + ROW_GAP_PX).toFloat()

        word.forEachIndexed { letterIndex, letter ->
            val bitmap = LETTER_BITMAPS[letter] ?: return@forEachIndexed
            val letterColor = palette[(letterBaseIndex + letterIndex) % palette.size]
            val letterLeftCells = rowLeftCells + letterIndex * (LETTER_WIDTH + LETTER_GAP_PX)

            for (y in 0 until LETTER_HEIGHT) {
                val row = bitmap[y]
                for (x in 0 until LETTER_WIDTH) {
                    if (row[x] != '#') continue
                    val pixelLeft = logoLeft + (letterLeftCells + x) * logicalPixelPx
                    val pixelTop = logoTop + (rowTopCells + y) * logicalPixelPx

                    for (subY in 0 until SUBDIV) {
                        for (subX in 0 until SUBDIV) {
                            targetX.add(pixelLeft + (subX + 0.5f) * subParticlePx)
                            targetY.add(pixelTop + (subY + 0.5f) * subParticlePx)
                            targetColor.add(letterColor)
                        }
                    }
                }
            }
        }
    }

    addWord(LOGO_TEXT_TOP, rowIndex = 0, letterBaseIndex = 0)
    addWord(LOGO_TEXT_BOTTOM, rowIndex = 1, letterBaseIndex = LOGO_TEXT_TOP.length)

    val count = targetX.size
    val px = FloatArray(count)
    val py = FloatArray(count)
    val tx = FloatArray(count)
    val ty = FloatArray(count)
    val vx = FloatArray(count)
    val vy = FloatArray(count)
    val colors = Array(count) { Color.Cyan }
    val delay = LongArray(count)
    val size = FloatArray(count)
    val driftAmpX = FloatArray(count)
    val driftAmpY = FloatArray(count)
    val driftPhaseX = FloatArray(count)
    val driftPhaseY = FloatArray(count)

    val maxDist = sqrt((widthPx * widthPx + heightPx * heightPx).toDouble()).toFloat().coerceAtLeast(1f)

    for (i in 0 until count) {
        tx[i] = targetX[i]
        ty[i] = targetY[i]
        colors[i] = targetColor[i]

        px[i] = random.nextFloat() * (widthPx * 3f) - widthPx
        py[i] = random.nextFloat() * (heightPx * 3f) - heightPx
        vx[i] = 0f
        vy[i] = 0f

        val dx = tx[i] - logoCenterX
        val dy = ty[i] - logoCenterY
        val dist = sqrt(dx * dx + dy * dy)
        delay[i] = ((dist / maxDist) * 800f).toLong().coerceIn(0L, 800L)

        size[i] = subParticlePx * (1f + random.nextFloat())
        driftAmpX[i] = 0.06f + random.nextFloat() * 0.22f
        driftAmpY[i] = 0.06f + random.nextFloat() * 0.22f
        driftPhaseX[i] = random.nextFloat() * 6.2831855f
        driftPhaseY[i] = random.nextFloat() * 6.2831855f
    }

    return ParticleState(
        count = count,
        px = px,
        py = py,
        tx = tx,
        ty = ty,
        vx = vx,
        vy = vy,
        colors = colors,
        delay = delay,
        size = size,
        driftAmpX = driftAmpX,
        driftAmpY = driftAmpY,
        driftPhaseX = driftPhaseX,
        driftPhaseY = driftPhaseY
    )
}

private fun updateParticles(
    particles: ParticleState,
    elapsedMs: Long,
    widthPx: Float,
    heightPx: Float
) {
    val count = particles.count
    val snapDistSq = SNAP_DIST * SNAP_DIST

    for (i in 0 until count) {
        if (elapsedMs < DRIFT_END_MS) {
            val t = elapsedMs.toFloat() * 0.0045f
            particles.px[i] += sin(t + particles.driftPhaseX[i]) * particles.driftAmpX[i]
            particles.py[i] += sin(t + particles.driftPhaseY[i]) * particles.driftAmpY[i]
            particles.px[i] = particles.px[i].coerceIn(-widthPx, widthPx * 2f)
            particles.py[i] = particles.py[i].coerceIn(-heightPx, heightPx * 2f)
        }

        val startMs = CONVERGE_START_MS + particles.delay[i]
        if (elapsedMs < startMs) continue

        val dx = particles.tx[i] - particles.px[i]
        val dy = particles.ty[i] - particles.py[i]
        val distSq = dx * dx + dy * dy

        if (distSq < snapDistSq) {
            particles.px[i] = particles.tx[i]
            particles.py[i] = particles.ty[i]
            particles.vx[i] = 0f
            particles.vy[i] = 0f
        } else {
            particles.vx[i] = (particles.vx[i] + dx * SPRING_K) * DAMPING
            particles.vy[i] = (particles.vy[i] + dy * SPRING_K) * DAMPING
            particles.px[i] += particles.vx[i]
            particles.py[i] += particles.vy[i]
        }
    }
}

private fun hardSnapCloseParticles(particles: ParticleState) {
    val hardSnapSq = HARD_SNAP_DIST * HARD_SNAP_DIST
    for (i in 0 until particles.count) {
        val dx = particles.tx[i] - particles.px[i]
        val dy = particles.ty[i] - particles.py[i]
        if (dx * dx + dy * dy <= hardSnapSq || (abs(dx) + abs(dy) < 2.5f)) {
            particles.px[i] = particles.tx[i]
            particles.py[i] = particles.ty[i]
            particles.vx[i] = 0f
            particles.vy[i] = 0f
        }
    }
}

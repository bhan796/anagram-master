package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bhan796.anagramarena.online.CosmeticCatalog
import com.bhan796.anagramarena.online.CosmeticItem
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.viewmodel.ShopViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class CarouselEntry(
    val item: CosmeticItem,
    val isWinner: Boolean
)

private const val TOTAL_ITEMS = 120
private const val LANDING_MIN_INDEX = 70
private const val LANDING_MAX_INDEX = 90
private const val REVEAL_DELAY_MS = 300L

private val rarityIntensity: Map<String, Int> = mapOf(
    "common" to 1,
    "uncommon" to 2,
    "rare" to 3,
    "epic" to 4,
    "legendary" to 5,
    "mythic" to 6
)

private fun winnerGlowColor(rarity: String): Color = when (rarity) {
    "common" -> Color(0x44AAAAAA)
    "uncommon" -> Color(0xFF39FF14)
    "rare" -> Color(0xFF00F5FF)
    "epic" -> Color(0xFFCC44FF)
    "legendary", "mythic" -> Color(0xFFFFD700)
    else -> Color(0x44AAAAAA)
}

@Composable
fun ChestOpenModal(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit,
    onEquip: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var carouselItems by remember { mutableStateOf<List<CarouselEntry>>(emptyList()) }
    var viewportWidthDp by remember { mutableStateOf(360f) }
    var revealed by remember { mutableStateOf(false) }
    var flashActive by remember { mutableStateOf(false) }
    var shakeActive by remember { mutableStateOf(false) }
    var ringActive by remember { mutableStateOf(false) }
    var blackoutActive by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    val won = state.chestResult
    val revealIntensity = rarityIntensity[won?.rarity] ?: 1

    LaunchedEffect(won?.id, viewportWidthDp) {
        if (won == null) return@LaunchedEffect
        revealed = false
        flashActive = false
        shakeActive = false
        ringActive = false
        blackoutActive = false
        val fillers = CosmeticCatalog.items
        val landingIndex = Random.nextInt(LANDING_MIN_INDEX, LANDING_MAX_INDEX + 1)
        val entries = MutableList(TOTAL_ITEMS) {
            val filler = fillers[Random.nextInt(fillers.size)]
            CarouselEntry(filler, isWinner = false)
        }
        entries[landingIndex] = CarouselEntry(won, isWinner = true)
        carouselItems = entries
        val winIndex = entries.indexOfFirst { it.isWinner }.coerceAtLeast(0)
        val targetOffset = winIndex * 88f - (viewportWidthDp / 2f - 44f)
        offset.snapTo(0f)
        offset.animateTo(targetOffset, animationSpec = tween(3500, easing = FastOutSlowInEasing))
        delay(REVEAL_DELAY_MS)

        if (won.rarity == "mythic") {
            blackoutActive = true
            delay(600)
            blackoutActive = false
        }

        revealed = true
        if (revealIntensity >= 3) {
            flashActive = true
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, animationSpec = tween(400))
            flashActive = false
        }
        if (revealIntensity >= 4) {
            shakeActive = true
            repeat(4) {
                shakeX.animateTo(if (it % 2 == 0) 4f else -4f, tween(35, easing = LinearEasing))
                shakeY.animateTo(if (it % 2 == 0) -4f else 4f, tween(35, easing = LinearEasing))
            }
            shakeX.animateTo(0f, tween(25))
            shakeY.animateTo(0f, tween(25))
            shakeActive = false
        }
        if (revealIntensity >= 5) {
            if (won.rarity != "mythic") delay(400)
            ringActive = true
            ringProgress.snapTo(0f)
            ringProgress.animateTo(1f, animationSpec = tween(800))
            ringActive = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeX.value.roundToInt(), shakeY.value.roundToInt()) }
                .background(if (blackoutActive) Color.Black else ColorBackground)
                .padding(16.dp)
        ) {
            val revealColor = CosmeticCatalog.getRarityColor(won?.rarity ?: "common")
            if (flashActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(revealColor.copy(alpha = flashAlpha.value))
                )
            }
            if (blackoutActive && won?.rarity == "mythic") {
                val scanY by rememberInfiniteTransition(label = "scanline").animateFloat(
                    initialValue = -0.2f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                    label = "scanlineY"
                )
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val y = maxHeight * scanY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = y)
                            .height(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00F5FF), Color(0xFFCC44FF), Color(0xFFFFD700))
                                )
                            )
                    )
                }
            }
            if (ringActive) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val radius = (300.dp.toPx()) * ringProgress.value
                    drawCircle(
                        color = revealColor.copy(alpha = (1f - ringProgress.value) * 0.75f),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = (4.dp.toPx() * (1f - ringProgress.value.coerceAtMost(0.75f))))
                    )
                }
            }
            if (won == null) {
                Text("Opening chest...", color = ColorWhite, style = MaterialTheme.typography.headlineSmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CHEST OPEN", color = ColorGold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .onSizeChanged { size ->
                                viewportWidthDp = with(density) { size.width.toDp().value }
                            }
                            .clipToBounds()
                            .background(ColorSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(start = (-offset.value).dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            carouselItems.forEach { entry ->
                                val item = entry.item
                                val rarityColor = CosmeticCatalog.getRarityColor(item.rarity)
                                val rarityPulse by rememberInfiniteTransition(label = "rarity-${entry.item.id}").animateFloat(
                                    initialValue = 0.75f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = when (item.rarity) {
                                                "rare" -> 1500
                                                "epic" -> 1200
                                                "legendary" -> 900
                                                "mythic" -> 900
                                                else -> 1800
                                            },
                                            easing = FastOutSlowInEasing
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse-${entry.item.id}"
                                )
                                val rainbowShift by rememberInfiniteTransition(label = "rainbow-${entry.item.id}").animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                                    label = "hue-${entry.item.id}"
                                )
                                val mythicBorder = Color.hsv(rainbowShift, 0.9f, 1f)
                                val winning = entry.isWinner && revealed
                                val glowColor = if (winning) winnerGlowColor(item.rarity) else rarityColor.copy(alpha = when (item.rarity) {
                                    "rare" -> 0.35f
                                    "epic" -> 0.45f
                                    "legendary" -> 0.55f
                                    "mythic" -> 0.6f
                                    else -> 0f
                                })
                                Box(
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(120.dp)
                                        .scale(
                                            if (winning && item.rarity == "mythic") 1.2f + (0.1f * rarityPulse)
                                            else if (winning) 1.25f
                                            else 1f
                                        )
                                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                                        .border(
                                            if (entry.isWinner) 4.dp else 3.dp,
                                            if (item.rarity == "mythic") mythicBorder else rarityColor,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            if (glowColor.alpha > 0f) {
                                                drawRect(color = glowColor.copy(alpha = glowColor.alpha * rarityPulse))
                                            }
                                            if (item.rarity == "epic") {
                                                val shimmerX = size.width * rarityPulse
                                                drawRect(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                                                        start = Offset(shimmerX - size.width * 0.6f, 0f),
                                                        end = Offset(shimmerX + size.width * 0.4f, size.height)
                                                    )
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.labelSmall, color = ColorWhite)
                                }
                            }
                        }
                    }
                    if (revealed) {
                        val labelPulse by rememberInfiniteTransition(label = "label").animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
                            label = "labelPulse"
                        )
                        val mythicGlitch by rememberInfiniteTransition(label = "mythicLabel").animateFloat(
                            initialValue = -2f,
                            targetValue = 2f,
                            animationSpec = infiniteRepeatable(animation = tween(120, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "mythicGlitch"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ColorSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(2.dp, CosmeticCatalog.getRarityColor(won.rarity), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(won.name, color = CosmeticCatalog.getRarityColor(won.rarity), style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    won.rarity.uppercase(),
                                    modifier = Modifier.offset(x = if (won.rarity == "mythic") mythicGlitch.dp else 0.dp),
                                    color = CosmeticCatalog.getRarityColor(won.rarity),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        ArcadeButton("EQUIP NOW", onClick = { onEquip(won.id) }, accentColor = ColorGold)
                        ArcadeButton("CLOSE", onClick = onDismiss)
                    }
                }
            }
        }
    }
}

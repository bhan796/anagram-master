package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhan796.anagramarena.ui.theme.*

// -- Glowing neon text title -------------------------------------------------
@Composable
fun NeonTitle(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "neonAlpha"
    )
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        color = ColorCyan.copy(alpha = alpha),
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun TileLogo(modifier: Modifier = Modifier) {
    val top = "ANAGRAM".toList()
    val bottom = "ARENA".toList()
    val palette = listOf(ColorCyan, ColorGold, ColorMagenta, ColorGreen)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            top.forEachIndexed { index, letter ->
                LogoTile(letter = letter, color = palette[index % palette.size], index = index)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bottom.forEachIndexed { index, letter ->
                LogoTile(letter = letter, color = palette[(index + 2) % palette.size], index = index + top.size)
            }
        }
    }
}

@Composable
private fun LogoTile(letter: Char, color: Color, index: Int) {
    LetterTile(
        letter = letter.toString(),
        revealed = true,
        index = index,
        accentColor = color,
        modifier = Modifier.size(40.dp)
    )
}

// -- Arcade-style primary button ----------------------------------------------
@Composable
fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = ColorCyan
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnScale"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.05f)))
                else Brush.horizontalGradient(listOf(ColorDimText.copy(alpha = 0.1f), ColorDimText.copy(alpha = 0.05f)))
            )
            .border(1.5.dp, if (enabled) accentColor else ColorDimText, RoundedCornerShape(4.dp))
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = if (enabled) accentColor else ColorDimText,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = ColorDimText
            ),
            elevation = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ArcadeBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArcadeButton(
        text = "<  BACK",
        onClick = onClick,
        accentColor = ColorGold,
        modifier = modifier
    )
}

// -- Letter tile (Tetris-block inspired) -------------------------------------
@Composable
fun LetterTile(
    letter: String,
    revealed: Boolean,
    index: Int,
    accentColor: Color = ColorCyan,
    modifier: Modifier = Modifier
) {
    val offsetY by animateFloatAsState(
        targetValue = if (revealed) 0f else -60f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tileDropY_$index"
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(200),
        label = "tileAlpha_$index"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { translationY = offsetY; this.alpha = alpha }
            .size(34.dp)
            .background(ColorSurfaceVariant, RoundedCornerShape(3.dp))
            .border(
                width = if (letter != "_") 1.5.dp else 0.5.dp,
                color = if (letter != "_") accentColor else ColorDimText.copy(alpha = 0.3f),
                shape = RoundedCornerShape(3.dp)
            )
    ) {
        Text(
            text = letter.uppercase(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (letter != "_") accentColor else ColorDimText.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

// -- Neon countdown timer bar -------------------------------------------------
@Composable
fun TimerBar(secondsRemaining: Int, totalSeconds: Int, modifier: Modifier = Modifier) {
    val fraction = if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds else 0f
    val color = when {
        fraction > 0.5f -> ColorCyan
        fraction > 0.25f -> ColorGold
        else -> ColorRed
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(500),
        label = "timerFrac"
    )
    // Pulse when low
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (fraction < 0.25f) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${secondsRemaining}s",
            style = MaterialTheme.typography.labelLarge,
            color = color.copy(alpha = pulseAlpha)
        )
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color.copy(alpha = pulseAlpha),
            trackColor = ColorSurfaceVariant
        )
    }
}

// -- Score badge --------------------------------------------------------------
@Composable
fun ScoreBadge(label: String, score: Int, color: Color = ColorCyan) {
    var previousScore by remember { mutableIntStateOf(score) }
    val scale by animateFloatAsState(
        targetValue = if (score != previousScore) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { previousScore = score },
        label = "scorePop"
    )
    LaunchedEffect(score) { previousScore = score }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ColorDimText)
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            modifier = Modifier.scale(scale)
        )
    }
}

// -- Section divider ----------------------------------------------------------
@Composable
fun NeonDivider(color: Color = ColorCyan.copy(alpha = 0.3f)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent)))
    )
}

// -- Screen scaffold with dark background -------------------------------------
@Composable
fun ArcadeScaffold(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

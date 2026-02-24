package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.LetterTile
import com.bhan796.anagramarena.ui.components.NeonDivider
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.RankBadge
import com.bhan796.anagramarena.ui.components.ScoreBadge
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorGreen
import com.bhan796.anagramarena.ui.theme.ColorMagenta
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp
import kotlinx.coroutines.delay

private enum class HowToTab(val label: String, val icon: String) {
    MATCH("MATCH", "?"),
    LETTERS("LETTERS", "A"),
    CONUNDRUM("COND'M", "?"),
    WINNING("WINNING", "?")
}

@Composable
fun HowToPlayScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    var activeTab by remember { mutableStateOf(HowToTab.MATCH) }
    var revealedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeTab) {
        if (activeTab != HowToTab.LETTERS) {
            revealedCount = 0
            return@LaunchedEffect
        }
        revealedCount = 0
        repeat(9) {
            delay(80)
            revealedCount = it + 1
        }
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("HOW TO PLAY")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))
        ) {
            HowToTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(sdp(6.dp)))
                        .border(
                            sdp(1.dp),
                            if (tab == activeTab) ColorCyan else ColorDimText.copy(alpha = 0.35f),
                            RoundedCornerShape(sdp(6.dp))
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = sdp(10.dp), vertical = sdp(8.dp))
                ) {
                    Text(
                        text = "${tab.icon} ${tab.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (tab == activeTab) ColorCyan else ColorDimText
                    )
                }
            }
        }

        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(260)) + fadeIn()).togetherWith(
                    slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(220)) + fadeOut()
                )
            },
            label = "howToContent"
        ) { tab ->
            when (tab) {
                HowToTab.MATCH -> MatchCard()
                HowToTab.LETTERS -> LettersCard(revealedCount)
                HowToTab.CONUNDRUM -> ConundrumCard()
                HowToTab.WINNING -> WinningCard()
            }
        }
    }
}

@Composable
private fun MatchCard() {
    CardContainer(border = ColorMagenta.copy(alpha = 0.35f)) {
        Header(icon = "?", title = "Match Format", color = ColorMagenta)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(12.dp))) {
            Text("5", style = MaterialTheme.typography.displayLarge, color = ColorCyan)
            Text("Rounds Total", style = MaterialTheme.typography.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
            (1..4).forEach { number ->
                RoundChip(text = "$number\nLTR", color = ColorCyan)
            }
            RoundChip(text = "5\nCON", color = ColorGold)
        }
        Text(
            "Rounds 1-4 are Letters rounds. Round 5 is the Conundrum. Highest total score wins.",
            style = MaterialTheme.typography.bodySmall,
            color = ColorDimText
        )
    }
}

@Composable
private fun LettersCard(revealedCount: Int) {
    val letters = listOf("C", "A", "T", "S", "R", "O", "N", "E", "L")
    CardContainer(border = ColorCyan.copy(alpha = 0.35f)) {
        Header(icon = "A", title = "Letters Round", color = ColorCyan)
        Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
            letters.forEachIndexed { index, letter ->
                LetterTile(
                    letter = letter,
                    revealed = true,
                    index = index,
                    accentColor = if (index < revealedCount) ColorCyan else ColorDimText.copy(alpha = 0.35f)
                )
            }
        }
        listOf(
            "3 letters" to "3 pts",
            "5 letters" to "5 pts",
            "7 letters" to "7 pts",
            "9 letters" to "9 pts"
        ).forEach { (key, value) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(key, style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                Text(value, style = MaterialTheme.typography.headlineSmall, color = ColorCyan)
            }
            NeonDivider(color = ColorDimText.copy(alpha = 0.2f))
        }
        Text(
            "Each letters round has one silver tile (2 pts) and one gold tile (3 pts). Word score is the sum of tile values used.",
            style = MaterialTheme.typography.bodySmall,
            color = ColorDimText
        )
    }
}

@Composable
private fun ConundrumCard() {
    CardContainer(border = ColorGold.copy(alpha = 0.35f)) {
        Header(icon = "?", title = "Conundrum", color = ColorGold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.4f), RoundedCornerShape(sdp(6.dp)))
                .padding(sdp(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("RANALUGAM", style = MaterialTheme.typography.displaySmall, color = ColorGold)
        }
        Text(
            "?",
            style = MaterialTheme.typography.displaySmall,
            color = ColorGreen,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(sdp(4.dp))) {
            "ANAGRAM".forEachIndexed { index, c ->
                LetterTile(letter = c.toString(), revealed = true, index = index, accentColor = ColorGreen)
            }
        }
        Text(
            "Unscramble the 9-letter word in 30 seconds. First correct answer scores 10 points.",
            style = MaterialTheme.typography.bodySmall,
            color = ColorDimText
        )
    }
}

@Composable
private fun WinningCard() {
    CardContainer(border = ColorGold.copy(alpha = 0.35f)) {
        Header(icon = "?", title = "Winning", color = ColorGold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadge(label = "YOU", score = 42, color = ColorCyan)
            Text("VS", style = MaterialTheme.typography.headlineSmall, color = ColorDimText)
            ScoreBadge(label = "OPP", score = 38, color = ColorGold)
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RankBadge(tier = "gold")
        }
        Text(
            "Highest total score after 5 rounds wins. Leaving an active match forfeits the game.",
            style = MaterialTheme.typography.bodySmall,
            color = ColorDimText
        )
    }
}

@Composable
private fun CardContainer(border: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(8.dp)))
            .border(sdp(1.5.dp), border, RoundedCornerShape(sdp(8.dp)))
            .padding(sdp(12.dp)),
        verticalArrangement = Arrangement.spacedBy(sdp(10.dp)),
        content = content
    )
}

@Composable
private fun Header(icon: String, title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sdp(8.dp))
    ) {
        Text(icon, style = MaterialTheme.typography.headlineLarge, color = color)
        Text(title, style = MaterialTheme.typography.headlineSmall, color = color)
    }
}

@Composable
private fun RoundChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(sdp(34.dp))
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(4.dp)))
            .border(sdp(1.5.dp), color, RoundedCornerShape(sdp(4.dp))),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center)
    }
}

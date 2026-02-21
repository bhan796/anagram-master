package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun HowToPlayScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("HOW TO PLAY")

        RuleCard("Match Format", listOf(
            "5 rounds total.",
            "Rounds 1-4 are letters rounds.",
            "Round 5 is a conundrum round."
        ))

        RuleCard("Letters Rounds", listOf(
            "Picker chooses Vowel or Consonant until 9 letters are built.",
            "At least one vowel and one consonant are required.",
            "20 seconds to pick letters, then auto-fill if needed.",
            "Both players have 30 seconds to submit a word.",
            "Score = word length, valid 9-letter word = 12 points."
        ))

        RuleCard("Conundrum Round", listOf(
            "Unscramble the 9-letter conundrum in 30 seconds.",
            "First correct answer scores 12 points."
        ))

        RuleCard("Winning", listOf(
            "Highest total score after 5 rounds wins.",
            "Leaving an active match forfeits the game."
        ))
    }
}

@Composable
private fun RuleCard(title: String, bullets: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
            .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
            .padding(sdp(12.dp))
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        bullets.forEach { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, color = ColorDimText, modifier = Modifier.padding(top = sdp(4.dp)))
        }
    }
}

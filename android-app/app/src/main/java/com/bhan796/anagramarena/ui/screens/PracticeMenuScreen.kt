package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonDivider
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite

@Composable
fun PracticeMenuScreen(
    contentPadding: PaddingValues,
    timerEnabled: Boolean,
    onTimerToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPracticeLetters: () -> Unit,
    onPracticeConundrum: () -> Unit
) {
    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("PRACTICE")

        ArcadeButton(
            text = "PRACTICE LETTERS ROUND",
            onClick = onPracticeLetters,
            modifier = Modifier.fillMaxWidth()
        )

        ArcadeButton(
            text = "PRACTICE CONUNDRUM",
            onClick = onPracticeConundrum,
            modifier = Modifier.fillMaxWidth()
        )

        NeonDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable 30s Timer", style = MaterialTheme.typography.bodyMedium, color = ColorWhite)
            Switch(
                checked = timerEnabled,
                onCheckedChange = onTimerToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorBackground,
                    checkedTrackColor = ColorCyan,
                    uncheckedTrackColor = ColorSurfaceVariant
                )
            )
        }
    }
}

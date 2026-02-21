package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PracticeMenuScreen(
    contentPadding: PaddingValues,
    timerEnabled: Boolean,
    onTimerToggle: (Boolean) -> Unit,
    onPracticeLetters: () -> Unit,
    onPracticeConundrum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Practice Mode")

        Button(onClick = onPracticeLetters) {
            Text("Practice Letters Round")
        }

        Button(onClick = onPracticeConundrum) {
            Text("Practice Conundrum")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enable 30s Timer")
            Switch(checked = timerEnabled, onCheckedChange = onTimerToggle)
        }
    }
}
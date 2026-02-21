package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.components.*
import com.bhan796.anagramarena.ui.theme.ColorGreen
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorMagenta

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    playersOnline: Int,
    onPlayOnline: () -> Unit,
    onPracticeMode: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    ArcadeScaffold(contentPadding = contentPadding) {
        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600, easing = FastOutSlowInEasing)) { -40 }
        ) {
            TileLogo(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(32.dp))

        val delays = listOf(200, 300, 450, 600)
        val buttonVisible = delays.map { delay ->
            var v by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(delay.toLong()); v = true }
            v
        }

        AnimatedVisibility(buttonVisible[0], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PLAY ONLINE", onClick = onPlayOnline)
        }
        AnimatedVisibility(buttonVisible[1], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PRACTICE MODE", onClick = onPracticeMode)
        }
        AnimatedVisibility(buttonVisible[2], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PROFILE / STATS", onClick = onProfile, accentColor = ColorGold)
        }
        AnimatedVisibility(buttonVisible[3], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("SETTINGS", onClick = onSettings, accentColor = ColorMagenta)
        }
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Players Online: ", style = MaterialTheme.typography.headlineSmall, color = ColorWhite)
            Text(playersOnline.toString(), style = MaterialTheme.typography.headlineSmall, color = ColorGreen)
        }

        Spacer(Modifier.weight(1f))
    }
}

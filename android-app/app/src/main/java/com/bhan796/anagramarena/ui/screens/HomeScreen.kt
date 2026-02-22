package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.audio.SoundManager
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
    onHowToPlay: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    ArcadeScaffold(contentPadding = contentPadding) {
        Spacer(Modifier.weight(1f))

        LogoParticleAnimation(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(32.dp))

        val delays = listOf(1700, 1800, 1900, 2000, 2100)
        val buttonVisible = delays.map { delay ->
            var v by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(delay.toLong()); v = true }
            v
        }

        AnimatedVisibility(buttonVisible[0], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PLAY ONLINE", onClick = {
                SoundManager.playClick()
                onPlayOnline()
            })
        }
        AnimatedVisibility(buttonVisible[1], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PRACTICE MODE", onClick = {
                SoundManager.playClick()
                onPracticeMode()
            })
        }
        AnimatedVisibility(buttonVisible[2], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("HOW TO PLAY", onClick = {
                SoundManager.playClick()
                onHowToPlay()
            })
        }
        AnimatedVisibility(buttonVisible[3], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("PROFILE / STATS", onClick = {
                SoundManager.playClick()
                onProfile()
            }, accentColor = ColorGold)
        }
        AnimatedVisibility(buttonVisible[4], enter = fadeIn(tween(400)) + expandVertically()) {
            ArcadeButton("SETTINGS", onClick = {
                SoundManager.playClick()
                onSettings()
            }, accentColor = ColorMagenta)
        }
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Players Online: ", style = MaterialTheme.typography.labelLarge, color = ColorWhite)
            Text(playersOnline.toString(), style = MaterialTheme.typography.labelLarge, color = ColorGreen)
        }

        Spacer(Modifier.weight(1f))
    }
}

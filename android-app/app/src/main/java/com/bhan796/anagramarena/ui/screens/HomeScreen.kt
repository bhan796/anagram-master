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
import com.bhan796.anagramarena.ui.theme.ColorCyan

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    playersOnline: Int,
    onPlayOnline: () -> Unit,
    onPracticeMode: () -> Unit,
    onHowToPlay: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    isAuthenticated: Boolean,
    authLabel: String,
    onAuthAction: () -> Unit,
    playIntro: Boolean,
    onIntroComplete: () -> Unit
) {
    var logoComplete by remember(playIntro) { mutableStateOf(!playIntro) }

    ArcadeScaffold(contentPadding = contentPadding) {
        Spacer(Modifier.weight(1f))

        if (playIntro) {
            LogoParticleAnimation(
                modifier = Modifier.fillMaxWidth(),
                onComplete = {
                    logoComplete = true
                    onIntroComplete()
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TileLogo()
            }
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 0)) + expandVertically(tween(400, delayMillis = 0))) {
            ArcadeButton("PLAY ONLINE", onClick = {
                SoundManager.playClick()
                onPlayOnline()
            })
        }
        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 100)) + expandVertically(tween(400, delayMillis = 100))) {
            ArcadeButton("PRACTICE MODE", onClick = {
                SoundManager.playClick()
                onPracticeMode()
            })
        }
        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 200)) + expandVertically(tween(400, delayMillis = 200))) {
            ArcadeButton("HOW TO PLAY", onClick = {
                SoundManager.playClick()
                onHowToPlay()
            })
        }
        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 300)) + expandVertically(tween(400, delayMillis = 300))) {
            ArcadeButton("PROFILE / STATS", onClick = {
                SoundManager.playClick()
                onProfile()
            }, accentColor = ColorGold)
        }
        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 400)) + expandVertically(tween(400, delayMillis = 400))) {
            ArcadeButton("SETTINGS", onClick = {
                SoundManager.playClick()
                onSettings()
            }, accentColor = ColorMagenta)
        }
        AnimatedVisibility(logoComplete, enter = fadeIn(tween(400, delayMillis = 500)) + expandVertically(tween(400, delayMillis = 500))) {
            ArcadeButton(if (isAuthenticated) "LOG OUT" else "SIGN IN", onClick = {
                SoundManager.playClick()
                onAuthAction()
            }, accentColor = ColorCyan)
        }
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Players Online: ", style = MaterialTheme.typography.labelLarge, color = ColorWhite)
            Text(playersOnline.toString(), style = MaterialTheme.typography.labelLarge, color = ColorGreen)
        }
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Account: ", style = MaterialTheme.typography.labelLarge, color = ColorWhite)
            Text(
                authLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (isAuthenticated) ColorCyan else ColorGold
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

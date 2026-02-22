package com.bhan796.anagramarena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.data.AppDependencies
import com.bhan796.anagramarena.ui.screens.ConundrumPracticeScreen
import com.bhan796.anagramarena.ui.screens.HomeScreen
import com.bhan796.anagramarena.ui.screens.HowToPlayScreen
import com.bhan796.anagramarena.ui.screens.LettersPracticeScreen
import com.bhan796.anagramarena.ui.screens.MatchFoundScreen
import com.bhan796.anagramarena.ui.screens.MatchmakingScreen
import com.bhan796.anagramarena.ui.screens.OnlineMatchScreen
import com.bhan796.anagramarena.ui.screens.PracticeMenuScreen
import com.bhan796.anagramarena.ui.screens.ProfileScreen
import com.bhan796.anagramarena.ui.screens.SettingsScreen
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp
import com.bhan796.anagramarena.viewmodel.HomeStatusViewModel
import com.bhan796.anagramarena.viewmodel.OnlineMatchViewModel
import com.bhan796.anagramarena.viewmodel.PracticeSettingsViewModel
import com.bhan796.anagramarena.viewmodel.ProfileViewModel

private object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val LETTERS = "practice_letters"
    const val CONUNDRUM = "practice_conundrum"
    const val ONLINE_MATCHMAKING = "online_matchmaking"
    const val ONLINE_MATCH_FOUND = "online_match_found"
    const val ONLINE_MATCH = "online_match"
    const val HOW_TO_PLAY = "how_to_play"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun AnagramArenaApp(dependencies: AppDependencies) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settingsViewModel: PracticeSettingsViewModel = viewModel(
        factory = PracticeSettingsViewModel.factory(dependencies.settingsStore)
    )
    val onlineMatchViewModel: OnlineMatchViewModel = viewModel(
        factory = OnlineMatchViewModel.factory(dependencies.onlineMatchRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(dependencies.profileRepository, dependencies.sessionStore)
    )
    val homeStatusViewModel: HomeStatusViewModel = viewModel(
        factory = HomeStatusViewModel.factory(dependencies.profileRepository)
    )

    val settings by settingsViewModel.state.collectAsState()
    val onlineState by onlineMatchViewModel.state.collectAsState()
    val homeStatus by homeStatusViewModel.state.collectAsState()

    var activeMusicMode by remember { mutableStateOf("none") }

    LaunchedEffect(settings) {
        SoundManager.setSoundEnabled(settings.soundEnabled)
        SoundManager.setMasterMuted(settings.masterMuted)
        SoundManager.setMusicEnabled(settings.musicEnabled)
        SoundManager.setUiSfxEnabled(settings.uiSfxEnabled)
        SoundManager.setGameSfxEnabled(settings.gameSfxEnabled)
        SoundManager.setMusicVolume(settings.musicVolume)
        SoundManager.setUiSfxVolume(settings.uiSfxVolume)
        SoundManager.setGameSfxVolume(settings.gameSfxVolume)
    }

    LaunchedEffect(currentRoute, settings.masterMuted, settings.musicEnabled) {
        if (settings.masterMuted || !settings.musicEnabled) {
            if (activeMusicMode != "none") {
                SoundManager.stopMusic()
                activeMusicMode = "none"
            }
            return@LaunchedEffect
        }

        val targetMode = if (currentRoute == Routes.ONLINE_MATCH) "match" else "menu"
        if (activeMusicMode == targetMode) return@LaunchedEffect

        if (targetMode == "match") SoundManager.startMatchMusic() else SoundManager.startMenuMusic()
        activeMusicMode = targetMode
    }

    LaunchedEffect(onlineState.matchState?.matchId, onlineState.matchState?.phase, currentRoute) {
        val match = onlineState.matchState
        val hasActiveMatch = match != null && match.phase.name != "FINISHED"
        if (!hasActiveMatch) return@LaunchedEffect
        if (currentRoute == Routes.ONLINE_MATCH || currentRoute == Routes.ONLINE_MATCH_FOUND) return@LaunchedEffect
        navController.navigate(Routes.ONLINE_MATCH) {
            launchSingleTop = true
        }
    }

    Scaffold { innerPadding: PaddingValues ->
        Box {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        contentPadding = innerPadding,
                        playersOnline = homeStatus.playersOnline,
                        onPlayOnline = { navController.navigate(Routes.ONLINE_MATCHMAKING) },
                        onPracticeMode = { navController.navigate(Routes.PRACTICE) },
                        onHowToPlay = { navController.navigate(Routes.HOW_TO_PLAY) },
                        onProfile = { navController.navigate(Routes.PROFILE) },
                        onSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }

                composable(Routes.HOW_TO_PLAY) {
                    HowToPlayScreen(contentPadding = innerPadding, onBack = { navController.popBackStack() })
                }

                composable(Routes.PRACTICE) {
                    PracticeMenuScreen(
                        contentPadding = innerPadding,
                        timerEnabled = settings.timerEnabled,
                        onTimerToggle = settingsViewModel::setTimerEnabled,
                        onBack = { navController.popBackStack() },
                        onPracticeLetters = { navController.navigate(Routes.LETTERS) },
                        onPracticeConundrum = { navController.navigate(Routes.CONUNDRUM) }
                    )
                }

                composable(Routes.LETTERS) {
                    LettersPracticeScreen(
                        contentPadding = innerPadding,
                        timerEnabled = settings.timerEnabled,
                        dictionaryProvider = dependencies.dictionaryProvider,
                        dictionaryLoaded = dependencies.dictionaryProvider.isLoaded(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.CONUNDRUM) {
                    ConundrumPracticeScreen(
                        contentPadding = innerPadding,
                        timerEnabled = settings.timerEnabled,
                        provider = dependencies.conundrumProvider,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.ONLINE_MATCHMAKING) {
                    MatchmakingScreen(
                        contentPadding = innerPadding,
                        onlineState = onlineState,
                        leaderboard = homeStatus.leaderboard,
                        onBack = { navController.popBackStack() },
                        onJoinQueue = onlineMatchViewModel::startQueue,
                        onCancelQueue = onlineMatchViewModel::cancelQueue,
                        onRetryConnection = onlineMatchViewModel::retryConnect,
                        onMatchReady = { navController.navigate(Routes.ONLINE_MATCH_FOUND) }
                    )
                }

                composable(Routes.ONLINE_MATCH_FOUND) {
                    MatchFoundScreen(
                        contentPadding = innerPadding,
                        state = onlineState,
                        onDone = { navController.navigate(Routes.ONLINE_MATCH) }
                    )
                }

                composable(Routes.ONLINE_MATCH) {
                    OnlineMatchScreen(
                        contentPadding = innerPadding,
                        state = onlineState,
                        onPickVowel = onlineMatchViewModel::pickVowel,
                        onPickConsonant = onlineMatchViewModel::pickConsonant,
                        onWordChange = onlineMatchViewModel::updateWordInput,
                        onSubmitWord = onlineMatchViewModel::submitWord,
                        onConundrumGuessChange = onlineMatchViewModel::updateConundrumGuessInput,
                        onSubmitConundrumGuess = onlineMatchViewModel::submitConundrumGuess,
                        onDismissError = onlineMatchViewModel::clearError,
                        onPlayAgain = {
                            onlineMatchViewModel.queuePlayAgain()
                            navController.navigate(Routes.ONLINE_MATCHMAKING)
                        },
                        onBack = {
                            onlineMatchViewModel.leaveActiveMatch()
                            navController.popBackStack(Routes.HOME, false)
                        },
                        onBackToHome = { navController.popBackStack(Routes.HOME, false) }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(contentPadding = innerPadding, onBack = { navController.popBackStack() }, viewModel = profileViewModel)
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        contentPadding = innerPadding,
                        state = settings,
                        onBack = { navController.popBackStack() },
                        onTimerToggle = settingsViewModel::setTimerEnabled,
                        onSoundToggle = settingsViewModel::setSoundEnabled,
                        onVibrationToggle = settingsViewModel::setVibrationEnabled,
                        onMasterMuteToggle = settingsViewModel::setMasterMuted,
                        onMusicToggle = settingsViewModel::setMusicEnabled,
                        onUiSfxToggle = settingsViewModel::setUiSfxEnabled,
                        onGameSfxToggle = settingsViewModel::setGameSfxEnabled,
                        onMusicVolumeChange = settingsViewModel::setMusicVolume,
                        onUiSfxVolumeChange = settingsViewModel::setUiSfxVolume,
                        onGameSfxVolumeChange = settingsViewModel::setGameSfxVolume
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-14).dp, y = (-14).dp)
                    .size(sdp(52.dp))
                    .background(ColorSurfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(sdp(8.dp)))
                    .border(
                        sdp(1.5.dp),
                        if (settings.masterMuted) ColorRed else ColorGold,
                        androidx.compose.foundation.shape.RoundedCornerShape(sdp(8.dp))
                    )
                    .clickable { settingsViewModel.setMasterMuted(!settings.masterMuted) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(sdp(26.dp))) {
                    val iconColor = if (settings.masterMuted) ColorRed else ColorCyan
                    val bodyPath = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.40f)
                        lineTo(size.width * 0.34f, size.height * 0.40f)
                        lineTo(size.width * 0.54f, size.height * 0.24f)
                        lineTo(size.width * 0.54f, size.height * 0.76f)
                        lineTo(size.width * 0.34f, size.height * 0.60f)
                        lineTo(size.width * 0.15f, size.height * 0.60f)
                        close()
                    }
                    drawPath(path = bodyPath, color = iconColor)

                    if (settings.masterMuted) {
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.62f, size.height * 0.30f),
                            end = Offset(size.width * 0.90f, size.height * 0.70f),
                            strokeWidth = size.minDimension * 0.08f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.90f, size.height * 0.30f),
                            end = Offset(size.width * 0.62f, size.height * 0.70f),
                            strokeWidth = size.minDimension * 0.08f,
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawArc(
                            color = iconColor,
                            startAngle = -40f,
                            sweepAngle = 80f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.50f, size.height * 0.22f),
                            size = Size(size.width * 0.34f, size.height * 0.56f),
                            style = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = iconColor,
                            startAngle = -40f,
                            sweepAngle = 80f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.58f, size.height * 0.10f),
                            size = Size(size.width * 0.40f, size.height * 0.80f),
                            style = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

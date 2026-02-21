package com.bhan796.anagramarena.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhan796.anagramarena.data.AppDependencies
import com.bhan796.anagramarena.ui.screens.ConundrumPracticeScreen
import com.bhan796.anagramarena.ui.screens.HomeScreen
import com.bhan796.anagramarena.ui.screens.LettersPracticeScreen
import com.bhan796.anagramarena.ui.screens.MatchmakingScreen
import com.bhan796.anagramarena.ui.screens.OnlineMatchScreen
import com.bhan796.anagramarena.ui.screens.PracticeMenuScreen
import com.bhan796.anagramarena.ui.screens.ProfileScreen
import com.bhan796.anagramarena.ui.screens.SettingsScreen
import com.bhan796.anagramarena.viewmodel.OnlineMatchViewModel
import com.bhan796.anagramarena.viewmodel.ProfileViewModel
import com.bhan796.anagramarena.viewmodel.PracticeSettingsViewModel

private object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val LETTERS = "practice_letters"
    const val CONUNDRUM = "practice_conundrum"
    const val ONLINE_MATCHMAKING = "online_matchmaking"
    const val ONLINE_MATCH = "online_match"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun AnagramArenaApp(dependencies: AppDependencies) {
    val navController = rememberNavController()
    val settingsViewModel: PracticeSettingsViewModel = viewModel(
        factory = PracticeSettingsViewModel.factory(dependencies.settingsStore)
    )
    val onlineMatchViewModel: OnlineMatchViewModel = viewModel(
        factory = OnlineMatchViewModel.factory(dependencies.onlineMatchRepository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(dependencies.profileRepository, dependencies.sessionStore)
    )
    val settings by settingsViewModel.state.collectAsState()
    val onlineState by onlineMatchViewModel.state.collectAsState()

    Scaffold { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onPlayOnline = { navController.navigate(Routes.ONLINE_MATCHMAKING) },
                    onPracticeMode = { navController.navigate(Routes.PRACTICE) },
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
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
                    onBack = { navController.popBackStack() },
                    onJoinQueue = onlineMatchViewModel::startQueue,
                    onCancelQueue = onlineMatchViewModel::cancelQueue,
                    onRetryConnection = onlineMatchViewModel::retryConnect,
                    onMatchReady = { navController.navigate(Routes.ONLINE_MATCH) }
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
                    onBack = { navController.popBackStack() },
                    onBackToHome = {
                        navController.popBackStack(Routes.HOME, false)
                    }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    contentPadding = innerPadding,
                    onBack = { navController.popBackStack() },
                    viewModel = profileViewModel
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    state = settings,
                    onBack = { navController.popBackStack() },
                    onTimerToggle = settingsViewModel::setTimerEnabled,
                    onSoundToggle = settingsViewModel::setSoundEnabled,
                    onVibrationToggle = settingsViewModel::setVibrationEnabled
                )
            }
        }
    }
}

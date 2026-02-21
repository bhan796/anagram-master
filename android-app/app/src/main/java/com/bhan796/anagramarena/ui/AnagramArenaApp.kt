package com.bhan796.anagramarena.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhan796.anagramarena.data.AppDependencies
import com.bhan796.anagramarena.ui.screens.ConundrumPracticeScreen
import com.bhan796.anagramarena.ui.screens.HomeScreen
import com.bhan796.anagramarena.ui.screens.LettersPracticeScreen
import com.bhan796.anagramarena.ui.screens.PracticeMenuScreen
import com.bhan796.anagramarena.viewmodel.PracticeSettingsViewModel

private object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val LETTERS = "practice_letters"
    const val CONUNDRUM = "practice_conundrum"
}

@Composable
fun AnagramArenaApp(dependencies: AppDependencies) {
    val navController = rememberNavController()
    val settingsViewModel: PracticeSettingsViewModel = viewModel()
    val settings by settingsViewModel.state.collectAsState()

    Scaffold { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onPracticeMode = { navController.navigate(Routes.PRACTICE) }
                )
            }

            composable(Routes.PRACTICE) {
                PracticeMenuScreen(
                    contentPadding = innerPadding,
                    timerEnabled = settings.timerEnabled,
                    onTimerToggle = settingsViewModel::setTimerEnabled,
                    onPracticeLetters = { navController.navigate(Routes.LETTERS) },
                    onPracticeConundrum = { navController.navigate(Routes.CONUNDRUM) }
                )
            }

            composable(Routes.LETTERS) {
                LettersPracticeScreen(
                    contentPadding = innerPadding,
                    timerEnabled = settings.timerEnabled,
                    dictionaryProvider = dependencies.dictionaryProvider,
                    dictionaryLoaded = dependencies.dictionaryProvider.isLoaded()
                )
            }

            composable(Routes.CONUNDRUM) {
                ConundrumPracticeScreen(
                    contentPadding = innerPadding,
                    timerEnabled = settings.timerEnabled,
                    provider = dependencies.conundrumProvider
                )
            }
        }
    }
}
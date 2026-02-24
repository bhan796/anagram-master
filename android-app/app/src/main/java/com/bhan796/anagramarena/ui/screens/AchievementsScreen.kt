package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.viewmodel.AchievementsViewModel

@Composable
fun AchievementsScreen(navController: NavController, viewModel: AchievementsViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val tierColor = mapOf(
        "easy" to Color(0xFF39FF14),
        "medium" to Color(0xFF00F5FF),
        "hard" to Color(0xFFCC44FF),
        "legendary" to Color(0xFFFFD700)
    )

    ArcadeScaffold(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        ArcadeBackButton(onClick = { navController.popBackStack() })
        NeonTitle("ACHIEVEMENTS")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.achievements) { a ->
                val color = tierColor[a.tier] ?: Color.White
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant)
                        .border(1.dp, color.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(a.name, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(a.description, style = MaterialTheme.typography.bodySmall, color = com.bhan796.anagramarena.ui.theme.ColorDimText)
                    Text("Reward: ${a.runesReward} RUNES", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFD700))
                    Text(
                        if (a.unlocked) "Unlocked ${a.unlockedAt ?: ""}".trim() else "Locked",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
        }
    }
}

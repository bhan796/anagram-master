package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
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
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.achievements) { a ->
                val color = tierColor[a.tier] ?: Color.White
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant)
                        .border(1.dp, color.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            a.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.14f))
                                .border(1.dp, color.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                a.tier.uppercase(),
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Text(
                        a.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorDimText,
                        lineHeight = 19.sp
                    )
                    Text(
                        "REWARD: ${a.runesReward} RUNES",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (a.unlocked) "Unlocked ${formatUnlockedAt(a.unlockedAt)}" else "Locked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun formatUnlockedAt(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value.replace('T', ' ').removeSuffix("Z")
}

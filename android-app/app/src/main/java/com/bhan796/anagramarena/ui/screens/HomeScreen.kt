package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onPracticeMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Anagram Arena")

        Button(onClick = {}, enabled = false) {
            Text("Play Online (Coming Soon)")
        }

        Button(onClick = onPracticeMode) {
            Text("Practice Mode")
        }

        OutlinedButton(onClick = {}, enabled = false) {
            Text("Profile / Stats")
        }

        OutlinedButton(onClick = {}, enabled = false) {
            Text("Settings")
        }
    }
}
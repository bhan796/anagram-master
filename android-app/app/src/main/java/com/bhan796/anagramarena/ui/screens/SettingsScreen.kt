package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.viewmodel.PracticeSettingsState

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    state: PracticeSettingsState,
    onTimerToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings")

        SettingToggle("Practice Timer", state.timerEnabled, onTimerToggle)
        SettingToggle("Sound Effects (placeholder)", state.soundEnabled, onSoundToggle)
        SettingToggle("Vibration (placeholder)", state.vibrationEnabled, onVibrationToggle)
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

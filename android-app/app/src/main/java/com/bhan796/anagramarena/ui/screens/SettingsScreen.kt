package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonDivider
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorMagenta
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.ui.theme.sdp
import com.bhan796.anagramarena.viewmodel.PracticeSettingsState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    state: PracticeSettingsState,
    onBack: () -> Unit,
    onTimerToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onMasterMuteToggle: (Boolean) -> Unit,
    onMusicToggle: (Boolean) -> Unit,
    onUiSfxToggle: (Boolean) -> Unit,
    onGameSfxToggle: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onUiSfxVolumeChange: (Float) -> Unit,
    onGameSfxVolumeChange: (Float) -> Unit
) {
    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("SETTINGS", color = ColorMagenta)

        SettingsToggleCard(
            rows = listOf(
                SettingRow("Practice Timer", state.timerEnabled, onTimerToggle),
                SettingRow("Master Mute", !state.masterMuted) { onMasterMuteToggle(!it) },
                SettingRow("SFX Master", state.soundEnabled, onSoundToggle),
                SettingRow("Vibration", state.vibrationEnabled, onVibrationToggle)
            )
        )

        SoundMixCard(
            musicEnabled = state.musicEnabled,
            uiSfxEnabled = state.uiSfxEnabled,
            gameSfxEnabled = state.gameSfxEnabled,
            musicVolume = state.musicVolume,
            uiSfxVolume = state.uiSfxVolume,
            gameSfxVolume = state.gameSfxVolume,
            onMusicToggle = onMusicToggle,
            onUiSfxToggle = onUiSfxToggle,
            onGameSfxToggle = onGameSfxToggle,
            onMusicVolumeChange = onMusicVolumeChange,
            onUiSfxVolumeChange = onUiSfxVolumeChange,
            onGameSfxVolumeChange = onGameSfxVolumeChange
        )
    }
}

private data class SettingRow(val label: String, val value: Boolean, val onToggle: (Boolean) -> Unit)

@Composable
private fun SettingsToggleCard(rows: List<SettingRow>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
            .border(sdp(1.dp), ColorMagenta.copy(alpha = 0.35f), RoundedCornerShape(sdp(6.dp)))
            .padding(sdp(12.dp))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.label, style = MaterialTheme.typography.bodyMedium, color = ColorWhite)
                    Switch(
                        checked = row.value,
                        onCheckedChange = row.onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorBackground,
                            checkedTrackColor = ColorMagenta,
                            uncheckedTrackColor = ColorDimText.copy(alpha = 0.5f)
                        )
                    )
                }
                if (index < rows.lastIndex) {
                    NeonDivider(color = ColorMagenta.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
private fun SoundMixCard(
    musicEnabled: Boolean,
    uiSfxEnabled: Boolean,
    gameSfxEnabled: Boolean,
    musicVolume: Float,
    uiSfxVolume: Float,
    gameSfxVolume: Float,
    onMusicToggle: (Boolean) -> Unit,
    onUiSfxToggle: (Boolean) -> Unit,
    onGameSfxToggle: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onUiSfxVolumeChange: (Float) -> Unit,
    onGameSfxVolumeChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
            .border(sdp(1.dp), ColorCyan.copy(alpha = 0.35f), RoundedCornerShape(sdp(6.dp)))
            .padding(sdp(12.dp))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sdp(12.dp))) {
            Text("SOUND MIX", style = MaterialTheme.typography.headlineSmall, color = ColorCyan)
            SoundMixRow("Music", musicEnabled, musicVolume, onMusicToggle, onMusicVolumeChange)
            NeonDivider(color = ColorCyan.copy(alpha = 0.25f))
            SoundMixRow("UI SFX", uiSfxEnabled, uiSfxVolume, onUiSfxToggle, onUiSfxVolumeChange)
            NeonDivider(color = ColorCyan.copy(alpha = 0.25f))
            SoundMixRow("Gameplay SFX", gameSfxEnabled, gameSfxVolume, onGameSfxToggle, onGameSfxVolumeChange)
        }
    }
}

@Composable
private fun SoundMixRow(
    label: String,
    enabled: Boolean,
    volume: Float,
    onToggle: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = ColorWhite)
            ArcadeButton(
                text = if (enabled) "ON" else "OFF",
                onClick = { onToggle(!enabled) },
                modifier = Modifier.fillMaxWidth(0.28f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = ColorCyan,
                    activeTrackColor = ColorCyan,
                    inactiveTrackColor = ColorDimText.copy(alpha = 0.5f)
                )
            )
            Text(
                text = "${(volume * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = ColorCyan,
                modifier = Modifier.padding(start = sdp(10.dp))
            )
        }
    }
}

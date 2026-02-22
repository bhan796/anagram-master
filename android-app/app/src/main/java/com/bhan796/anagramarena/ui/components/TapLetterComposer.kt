package com.bhan796.anagramarena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun TapLetterComposer(
    letters: List<Char>,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedIndices = remember(letters) { mutableStateListOf<Int>() }
    var displayOrder by remember(letters) { mutableStateOf(letters.indices.toList()) }

    LaunchedEffect(value, letters) {
        if (value.isEmpty() && selectedIndices.isNotEmpty()) {
            selectedIndices.clear()
        }
    }

    fun syncValue(indices: List<Int> = selectedIndices) {
        onValueChange(indices.joinToString(separator = "") { letters[it].toString() })
    }

    fun shuffle() {
        if (!enabled || letters.size <= 1) return
        displayOrder = displayOrder.shuffled()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                .padding(sdp(12.dp))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                Text(
                    text = if (selectedIndices.isEmpty()) "Tap letters to build your word" else "Your word",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorDimText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                    repeat(9) { index ->
                        val letter = selectedIndices.getOrNull(index)?.let { letters[it].toString() } ?: "_"
                        LetterTile(
                            letter = letter,
                            revealed = true,
                            index = index,
                            accentColor = if (letter == "_") ColorDimText else ColorCyan
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
            displayOrder.forEach { sourceIndex ->
                val selected = selectedIndices.contains(sourceIndex)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            color = if (selected) ColorSurfaceVariant.copy(alpha = 0.35f) else ColorSurfaceVariant,
                            shape = RoundedCornerShape(sdp(4.dp))
                        )
                        .border(
                            width = sdp(1.dp),
                            color = if (selected) ColorDimText else ColorCyan.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(sdp(4.dp))
                        )
                        .clickable(enabled = enabled && !selected) {
                            selectedIndices.add(sourceIndex)
                            syncValue()
                        }
                        .padding(horizontal = sdp(10.dp), vertical = sdp(8.dp))
                ) {
                    Text(
                        text = letters[sourceIndex].toString().uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) ColorDimText else ColorCyan,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
            ArcadeButton(
                text = "UNDO",
                onClick = {
                    if (selectedIndices.isNotEmpty()) {
                        selectedIndices.removeAt(selectedIndices.lastIndex)
                        syncValue()
                    }
                },
                enabled = enabled && selectedIndices.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
            ArcadeButton(
                text = "SHUFFLE",
                onClick = { shuffle() },
                enabled = enabled && letters.size > 1,
                modifier = Modifier.weight(1f)
            )
            ArcadeButton(
                text = "CLEAR",
                onClick = {
                    selectedIndices.clear()
                    syncValue(emptyList())
                },
                enabled = enabled && selectedIndices.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

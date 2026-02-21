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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant

@Composable
fun TapLetterComposer(
    letters: List<Char>,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedIndices = remember(letters) { mutableStateListOf<Int>() }

    LaunchedEffect(value, letters) {
        if (value.isEmpty() && selectedIndices.isNotEmpty()) {
            selectedIndices.clear()
        }
    }

    fun syncValue() {
        onValueChange(selectedIndices.joinToString(separator = "") { letters[it].toString() })
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            if (selectedIndices.isEmpty()) {
                Text(
                    text = "Tap letters below to build your word",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorDimText
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedIndices.forEachIndexed { index, letterIndex ->
                        LetterTile(
                            letter = letters[letterIndex].toString(),
                            revealed = true,
                            index = index,
                            accentColor = ColorCyan
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            letters.forEachIndexed { index, ch ->
                val selected = selectedIndices.contains(index)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            color = if (selected) ColorSurfaceVariant.copy(alpha = 0.35f) else ColorSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) ColorDimText else ColorCyan.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable(enabled = enabled && !selected) {
                            selectedIndices.add(index)
                            syncValue()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = ch.toString().uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) ColorDimText else ColorCyan,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                text = "CLEAR",
                onClick = {
                    selectedIndices.clear()
                    syncValue()
                },
                enabled = enabled && selectedIndices.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

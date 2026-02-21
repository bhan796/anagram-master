package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.online.RoundType
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.LetterTile
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.ScoreBadge
import com.bhan796.anagramarena.ui.components.TapLetterComposer
import com.bhan796.anagramarena.ui.components.TimerBar
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant

@Composable
fun OnlineMatchScreen(
    contentPadding: PaddingValues,
    state: OnlineUiState,
    onPickVowel: () -> Unit,
    onPickConsonant: () -> Unit,
    onWordChange: (String) -> Unit,
    onSubmitWord: () -> Unit,
    onConundrumGuessChange: (String) -> Unit,
    onSubmitConundrumGuess: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onBackToHome: () -> Unit
) {
    val match = state.matchState
    var showLeaveDialog by remember { mutableStateOf(false) }
    val isFinished = match?.phase == MatchPhase.FINISHED

    ArcadeScaffold(contentPadding = contentPadding) {
        if (isFinished) {
            ArcadeButton(
                text = "BACK HOME",
                onClick = onBackToHome,
                accentColor = ColorGold,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ArcadeButton(
                text = "LEAVE GAME",
                onClick = { showLeaveDialog = true },
                accentColor = ColorGold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "If you leave now, you will forfeit the match.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorRed
            )

            if (showLeaveDialog) {
                AlertDialog(
                    onDismissRequest = { showLeaveDialog = false },
                    title = { Text("Leave game?") },
                    text = { Text("Leaving an active match counts as a forfeit and awards your opponent the win.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showLeaveDialog = false
                            onBack()
                        }) {
                            Text("Forfeit & Leave", color = ColorRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLeaveDialog = false }) {
                            Text("Stay")
                        }
                    }
                )
            }
        }

        if (match == null) {
            NeonTitle("MATCH")
            Text("Waiting for match state...", style = MaterialTheme.typography.bodyMedium)
            return@ArcadeScaffold
        }

        NeonTitle("ROUND ${match.roundNumber}")
        NeonTitle(match.phase.name.replace('_', ' '))
        TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = 30)
        Text(state.statusMessage, style = MaterialTheme.typography.labelMedium, color = ColorDimText)

        val me = state.myPlayer
        val opponent = state.opponentPlayer
        if (me != null && opponent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreBadge(label = me.displayName, score = me.score, color = ColorCyan)
                ScoreBadge(label = opponent.displayName, score = opponent.score, color = ColorGold)
            }
        }

        if (state.lastError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, ColorRed, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Error: ${state.lastError.message}", style = MaterialTheme.typography.bodyMedium)
                    ArcadeButton(
                        text = "DISMISS",
                        onClick = onDismissError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.localValidationMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Text(text = state.localValidationMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }

        when (match.phase) {
            MatchPhase.AWAITING_LETTERS_PICK -> {
                Text(
                    if (state.isMyTurnToPick) "YOUR TURN TO PICK" else "OPPONENT IS PICKING",
                    style = MaterialTheme.typography.headlineSmall
                )
                LetterSlots(match.letters)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcadeButton(
                        text = "VOWEL",
                        onClick = onPickVowel,
                        enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                        modifier = Modifier.weight(1f)
                    )
                    ArcadeButton(
                        text = "CONSONANT",
                        onClick = onPickConsonant,
                        enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            MatchPhase.LETTERS_SOLVING -> {
                val selectableLetters = match.letters.mapNotNull { it.firstOrNull() }
                val selectedIndices = remember(match.roundNumber, match.phase) { mutableStateListOf<Int>() }

                Text("Build your longest valid word", style = MaterialTheme.typography.headlineSmall)
                WordTargetRow(letters = selectedIndices.map { selectableLetters[it] })
                SelectableLetterSlots(
                    letters = match.letters,
                    selectedIndices = selectedIndices.toSet(),
                    enabled = !state.hasSubmittedWord,
                    onLetterTapped = { index ->
                        if (!selectedIndices.contains(index)) {
                            selectedIndices.add(index)
                            onWordChange(selectedIndices.joinToString(separator = "") { selectableLetters[it].toString() })
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeButton(
                        text = "UNDO",
                        onClick = {
                            if (selectedIndices.isNotEmpty()) {
                                selectedIndices.removeAt(selectedIndices.lastIndex)
                                onWordChange(selectedIndices.joinToString(separator = "") { selectableLetters[it].toString() })
                            }
                        },
                        enabled = !state.hasSubmittedWord && selectedIndices.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                    ArcadeButton(
                        text = "CLEAR",
                        onClick = {
                            selectedIndices.clear()
                            onWordChange("")
                        },
                        enabled = !state.hasSubmittedWord && selectedIndices.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }

                ArcadeButton(
                    text = if (state.hasSubmittedWord) "SUBMITTED" else "SUBMIT WORD",
                    onClick = onSubmitWord,
                    enabled = !state.hasSubmittedWord,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.hasSubmittedWord) {
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ColorCyan,
                        trackColor = ColorSurfaceVariant
                    )
                    Text("Waiting for opponent or timeout...", style = MaterialTheme.typography.labelMedium)
                }
            }

            MatchPhase.CONUNDRUM_SOLVING -> {
                NeonTitle("CONUNDRUM")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = match.scrambled?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.displaySmall,
                        color = ColorGold,
                        letterSpacing = 8.sp
                    )
                }
                TapLetterComposer(
                    letters = match.scrambled?.toList().orEmpty(),
                    value = state.conundrumGuessInput,
                    onValueChange = onConundrumGuessChange,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ArcadeButton(
                    text = "SUBMIT GUESS",
                    onClick = onSubmitConundrumGuess,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Multiple guesses allowed. Respect rate limit.", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
            }

            MatchPhase.ROUND_RESULT -> {
                NeonTitle("ROUND RESULT")
                val result = match.roundResults.lastOrNull()
                if (result != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Round ${result.roundNumber} • ${result.type.name.lowercase()}", style = MaterialTheme.typography.headlineSmall)
                            if (result.type == RoundType.LETTERS) {
                                WordTiles(
                                    label = "Letters",
                                    word = result.letters?.joinToString(separator = "").orEmpty(),
                                    accentColor = ColorGold
                                )
                                match.players.forEach { player ->
                                    val submission = result.submissions?.get(player.playerId)
                                    val word = submission?.word?.takeIf { it.isNotBlank() } ?: "-"
                                    val points = result.awardedScores[player.playerId] ?: 0
                                    val isValid = submission?.isValid ?: false
                                    RoundResultPlayerRow(
                                        name = player.displayName,
                                        word = word,
                                        points = points,
                                        extra = if (submission == null) "No submission" else if (isValid) "Valid" else "Invalid",
                                        extraColor = if (isValid) ColorCyan else ColorRed
                                    )
                                }
                            } else {
                                WordTiles(
                                    label = "Scramble",
                                    word = result.scrambled.orEmpty(),
                                    accentColor = ColorGold
                                )
                                WordTiles(
                                    label = "Answer",
                                    word = result.answer.orEmpty(),
                                    accentColor = ColorCyan
                                )
                                match.players.forEach { player ->
                                    val points = result.awardedScores[player.playerId] ?: 0
                                    val solved = result.firstCorrectPlayerId == player.playerId
                                    RoundResultPlayerRow(
                                        name = player.displayName,
                                        word = if (solved) result.answer.orEmpty() else "-",
                                        points = points,
                                        extra = if (solved) "Solved first" else "Not solved",
                                        extraColor = if (solved) ColorCyan else ColorDimText
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text("Next round starts in a few seconds...", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                }
            }

            MatchPhase.FINISHED -> {
                NeonTitle("FINAL RESULT")
                Text(
                    text = when {
                        match.winnerPlayerId == null -> "Draw"
                        match.winnerPlayerId == state.playerId -> "You Win"
                        else -> "You Lose"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                val playersById = match.players.associateBy { it.playerId }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    match.roundResults.forEach { result ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                                .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "R${result.roundNumber} ${result.type.name.lowercase()}",
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                if (result.type == RoundType.LETTERS) {
                                    match.players.forEach { player ->
                                        val submission = result.submissions?.get(player.playerId)
                                        val submittedWord = submission?.word?.takeIf { it.isNotBlank() } ?: "-"
                                        val points = result.awardedScores[player.playerId] ?: 0
                                        RoundPlayerRow(
                                            name = playersById[player.playerId]?.displayName ?: "Player",
                                            word = submittedWord,
                                            points = points
                                        )
                                    }
                                } else {
                                    WordTiles(
                                        label = "Answer",
                                        word = result.answer ?: "-",
                                        accentColor = ColorGold
                                    )
                                    match.players.forEach { player ->
                                        val points = result.awardedScores[player.playerId] ?: 0
                                        val wasSolver = result.firstCorrectPlayerId == player.playerId
                                        RoundPlayerRow(
                                            name = playersById[player.playerId]?.displayName ?: "Player",
                                            word = if (wasSolver) result.answer ?: "-" else "-",
                                            points = points
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MatchPhase.UNKNOWN -> {
                Text("Unknown match phase", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            val letter = if (index < letters.size) letters[index] else "_"
            LetterTile(letter = letter, revealed = index < letters.size, index = index)
        }
    }
}

@Composable
private fun SelectableLetterSlots(
    letters: List<String>,
    selectedIndices: Set<Int>,
    enabled: Boolean,
    onLetterTapped: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            val letter = if (index < letters.size) letters[index] else "_"
            val selectable = enabled && index < letters.size && letter != "_" && !selectedIndices.contains(index)
            Box(
                modifier = Modifier.clickable(enabled = selectable) { onLetterTapped(index) }
            ) {
                LetterTile(
                    letter = letter,
                    revealed = index < letters.size,
                    index = index,
                    accentColor = if (selectedIndices.contains(index)) ColorDimText else ColorCyan
                )
            }
        }
    }
}

@Composable
private fun WordTargetRow(letters: List<Char>) {
    val maxLetters = 9
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (letters.isEmpty()) {
                Text("Tap big letters to build your word", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(maxLetters) { index ->
                    val ch = letters.getOrNull(index)
                    if (ch != null) {
                        RisingWordTile(letter = ch.toString(), index = index)
                    } else {
                        LetterTile(letter = "_", revealed = true, index = index, accentColor = ColorDimText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RisingWordTile(letter: String, index: Int) {
    var dropped by remember(index) { mutableStateOf(false) }
    val offsetY by animateFloatAsState(
        targetValue = if (dropped) 0f else 20f,
        animationSpec = tween(durationMillis = 180),
        label = "wordTileRise_$index"
    )
    LaunchedEffect(index) { dropped = true }

    LetterTile(
        letter = letter,
        revealed = true,
        index = index,
        modifier = Modifier.graphicsLayer { translationY = offsetY }
    )
}

@Composable
private fun RoundPlayerRow(name: String, word: String, points: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = ColorDimText)
            Text("$points pts", style = MaterialTheme.typography.labelMedium, color = ColorCyan)
        }
        WordTiles(label = null, word = word, accentColor = ColorCyan)
    }
}

@Composable
private fun RoundResultPlayerRow(name: String, word: String, points: Int, extra: String, extraColor: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = ColorDimText)
            Text("$points pts", style = MaterialTheme.typography.labelMedium, color = ColorCyan)
        }
        WordTiles(label = null, word = word, accentColor = ColorCyan)
        Text(extra, style = MaterialTheme.typography.labelSmall, color = extraColor)
    }
}

@Composable
private fun WordTiles(label: String?, word: String, accentColor: androidx.compose.ui.graphics.Color) {
    val cleaned = word.uppercase().filter { it in 'A'..'Z' }.take(9)
    val letters = if (cleaned.isEmpty()) "-" else cleaned
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!label.isNullOrBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorDimText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            letters.forEachIndexed { idx, ch ->
                LetterTile(
                    letter = ch.toString(),
                    revealed = true,
                    index = idx,
                    accentColor = accentColor
                )
            }
        }
    }
}

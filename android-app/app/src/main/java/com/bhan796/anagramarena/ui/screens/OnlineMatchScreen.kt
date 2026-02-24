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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.online.RoundType
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.LetterTile
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.RankBadge
import com.bhan796.anagramarena.ui.components.ScoreBadge
import com.bhan796.anagramarena.ui.components.TapLetterComposer
import com.bhan796.anagramarena.ui.components.TimerBar
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorGreen
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurface
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp

private fun phaseTotalSeconds(phase: MatchPhase): Int = when (phase) {
    MatchPhase.AWAITING_LETTERS_PICK -> 20
    MatchPhase.ROUND_RESULT -> 5
    MatchPhase.LETTERS_SOLVING, MatchPhase.CONUNDRUM_SOLVING -> 30
    else -> 0
}

private fun tileAccent(index: Int, doubleIndex: Int?, tripleIndex: Int?): Color {
    return when (index) {
        tripleIndex -> ColorGold
        doubleIndex -> Color(0xFFC0C0C0)
        else -> ColorCyan
    }
}

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
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    onBackToHome: () -> Unit
) {
    val match = state.matchState
    var showLeaveDialog by remember { mutableStateOf(false) }
    var previousPhase by rememberSaveable { mutableStateOf<MatchPhase?>(null) }
    val isFinished = match?.phase == MatchPhase.FINISHED
    val me = state.myPlayer
    val opponent = state.opponentPlayer
    val selectableLetters = remember(match?.letters) {
        match?.letters?.mapNotNull { it.firstOrNull() } ?: emptyList()
    }
    val selectedIndices = remember(match?.roundNumber, match?.phase) { mutableStateListOf<Int>() }

    LaunchedEffect(match?.phase) {
        val phase = match?.phase ?: return@LaunchedEffect
        if (phase == previousPhase) return@LaunchedEffect

        if (phase == MatchPhase.ROUND_RESULT) {
            SoundManager.playRoundResult()
        }
        if (phase == MatchPhase.FINISHED) {
            if (match.winnerPlayerId == state.playerId) SoundManager.playWin() else SoundManager.playLose()
        }
        previousPhase = phase
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(horizontal = sdp(16.dp))
                    .padding(top = sdp(12.dp), bottom = sdp(8.dp)),
                verticalArrangement = Arrangement.spacedBy(sdp(12.dp))
            ) {
                if (isFinished) {
                    ArcadeButton("PLAY AGAIN", onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
                    ArcadeButton("BACK HOME", onClick = onBackToHome, accentColor = ColorGold, modifier = Modifier.fillMaxWidth())
                } else {
                    ArcadeButton("LEAVE GAME", onClick = { showLeaveDialog = true }, accentColor = ColorGold, modifier = Modifier.fillMaxWidth())
                    Text("If you leave now, you will forfeit the match.", style = MaterialTheme.typography.bodySmall, color = ColorRed)
                }
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

                if (match == null) {
                    NeonTitle("MATCH")
                    Text("Waiting for match state...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    NeonTitle("ROUND ${match.roundNumber}")
                    NeonTitle(match.phase.name.replace('_', ' '))
                    TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = phaseTotalSeconds(match.phase))
                    Text(state.statusMessage, style = MaterialTheme.typography.labelMedium, color = ColorDimText)

                    if (me != null && opponent != null) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            ScoreBadge(label = me.displayName, score = me.score, color = ColorCyan)
                            ScoreBadge(label = opponent.displayName, score = opponent.score, color = ColorGold)
                        }
                    }

                    if (state.lastError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                .border(sdp(1.dp), ColorRed, RoundedCornerShape(sdp(6.dp)))
                                .padding(sdp(12.dp))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
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
                                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                                .padding(sdp(12.dp))
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
                            LetterSlots(
                                letters = match.letters,
                                doubleIndex = match.bonusTiles?.doubleIndex,
                                tripleIndex = match.bonusTiles?.tripleIndex
                            )
                        }
                        MatchPhase.LETTERS_SOLVING -> {
                            WordTargetRow(
                                sourceLetters = selectableLetters,
                                selectedIndices = selectedIndices.toList(),
                                doubleIndex = match.bonusTiles?.doubleIndex,
                                tripleIndex = match.bonusTiles?.tripleIndex
                            )
                        }
                        MatchPhase.CONUNDRUM_SOLVING -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorSurfaceVariant, RoundedCornerShape(sdp(8.dp)))
                                    .border(sdp(1.dp), ColorCyan.copy(alpha = 0.4f), RoundedCornerShape(sdp(8.dp)))
                                    .padding(sdp(16.dp))
                            ) {
                                Text(
                                    text = match.scrambled?.uppercase().orEmpty(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = ColorGold,
                                    letterSpacing = 8.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        MatchPhase.ROUND_RESULT -> {
                            NeonTitle("ROUND RESULT")
                            val result = match.roundResults.lastOrNull()
                            if (result != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                        .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                                        .padding(sdp(12.dp))
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
                                        Text("Round ${result.roundNumber} - ${result.type.name.lowercase()}", style = MaterialTheme.typography.headlineSmall)
                                        if (result.type == RoundType.LETTERS) {
                                            Text("Letters", style = MaterialTheme.typography.labelSmall, color = ColorDimText)
                                            LetterSlots(
                                                letters = result.letters.orEmpty(),
                                                doubleIndex = result.bonusTiles?.doubleIndex,
                                                tripleIndex = result.bonusTiles?.tripleIndex
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
                                    .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                    .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                                    .padding(sdp(10.dp))
                            ) {
                                Text("Next round starts in a few seconds...", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                            }
                        }
                        MatchPhase.FINISHED -> {
                            NeonTitle("FINAL RESULT")
                            val mySnapshot = match.players.firstOrNull { it.playerId == state.playerId }
                            val myDelta = match.ratingChanges[state.playerId] ?: 0
                            var animatedDelta by remember(match.matchId, myDelta) { mutableStateOf(0) }
                            LaunchedEffect(match.matchId, myDelta) {
                                animatedDelta = 0
                                if (myDelta == 0) return@LaunchedEffect
                                val step = if (myDelta > 0) 1 else -1
                                while (animatedDelta != myDelta) {
                                    animatedDelta += step
                                    kotlinx.coroutines.delay(20)
                                }
                            }
                            Text(
                                text = when {
                                    match.winnerPlayerId == null -> "Draw"
                                    match.winnerPlayerId == state.playerId -> "You Win"
                                    else -> "You Lose"
                                },
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                    .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                                    .padding(sdp(12.dp))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Mode", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                                        Text(match.mode.uppercase(), style = MaterialTheme.typography.labelMedium, color = ColorGold)
                                    }
                                    if (mySnapshot != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Rank", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                                            RankBadge(tier = mySnapshot.rankTier)
                                        }
                                    }
                                    if (match.mode == "ranked") {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Rating Change", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                                            Text(
                                                text = if (animatedDelta >= 0) "+$animatedDelta" else animatedDelta.toString(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = if (animatedDelta >= 0) ColorGreen else ColorRed
                                            )
                                        }
                                    }
                                }
                            }

                            val playersById = match.players.associateBy { it.playerId }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(sdp(10.dp))
                            ) {
                                match.roundResults.forEach { result ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                                            .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                                            .padding(sdp(12.dp))
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
                                            Text(
                                                "R${result.roundNumber} ${result.type.name.lowercase()}",
                                                style = MaterialTheme.typography.headlineSmall
                                            )

                                            if (result.type == RoundType.LETTERS) {
                                                LetterSlots(
                                                    letters = result.letters.orEmpty(),
                                                    doubleIndex = result.bonusTiles?.doubleIndex,
                                                    tripleIndex = result.bonusTiles?.tripleIndex
                                                )
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

            val interactivePhase = match?.phase == MatchPhase.AWAITING_LETTERS_PICK ||
                match?.phase == MatchPhase.LETTERS_SOLVING ||
                match?.phase == MatchPhase.CONUNDRUM_SOLVING

            if (match != null && interactivePhase) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(topStart = sdp(16.dp), topEnd = sdp(16.dp)))
                        .border(
                            sdp(1.dp),
                            ColorCyan.copy(alpha = 0.25f),
                            RoundedCornerShape(topStart = sdp(16.dp), topEnd = sdp(16.dp))
                        )
                        .padding(sdp(16.dp))
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(sdp(10.dp))
                ) {
                    when (match.phase) {
                        MatchPhase.AWAITING_LETTERS_PICK -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
                                ArcadeButton(
                                    "VOWEL",
                                    onClick = {
                                        SoundManager.playTilePlace()
                                        onPickVowel()
                                    },
                                    enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                                    modifier = Modifier.weight(1f).heightIn(min = sdp(52.dp))
                                )
                                ArcadeButton(
                                    "CONSONANT",
                                    onClick = {
                                        SoundManager.playTilePlace()
                                        onPickConsonant()
                                    },
                                    enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                                    modifier = Modifier.weight(1f).heightIn(min = sdp(52.dp))
                                )
                            }
                        }
                        MatchPhase.LETTERS_SOLVING -> {
                            SelectableLetterSlots(
                                letters = match.letters,
                                selectedIndices = selectedIndices.toSet(),
                                doubleIndex = match.bonusTiles?.doubleIndex,
                                tripleIndex = match.bonusTiles?.tripleIndex,
                                enabled = !state.hasSubmittedWord,
                                onLetterTapped = { index ->
                                    if (!selectedIndices.contains(index)) {
                                        selectedIndices.add(index)
                                        onWordChange(selectedIndices.joinToString(separator = "") { selectableLetters[it].toString() })
                                    }
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
                                ArcadeButton(
                                    "UNDO",
                                    onClick = {
                                        if (selectedIndices.isNotEmpty()) {
                                            selectedIndices.removeAt(selectedIndices.lastIndex)
                                            onWordChange(selectedIndices.joinToString(separator = "") { selectableLetters[it].toString() })
                                        }
                                    },
                                    enabled = !state.hasSubmittedWord && selectedIndices.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = sdp(52.dp))
                                )
                                ArcadeButton(
                                    "CLEAR",
                                    onClick = {
                                        selectedIndices.clear()
                                        onWordChange("")
                                    },
                                    enabled = !state.hasSubmittedWord && selectedIndices.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = sdp(52.dp))
                                )
                            }
                            ArcadeButton(
                                if (state.hasSubmittedWord) "SUBMITTED" else "SUBMIT WORD",
                                onClick = {
                                    SoundManager.playWordSubmit()
                                    onSubmitWord()
                                },
                                enabled = !state.hasSubmittedWord,
                                modifier = Modifier.fillMaxWidth().heightIn(min = sdp(52.dp))
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
                            TapLetterComposer(
                                letters = match.scrambled?.toList().orEmpty(),
                                value = state.conundrumGuessInput,
                                onValueChange = onConundrumGuessChange,
                                enabled = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            ArcadeButton(
                                "SUBMIT GUESS",
                                onClick = onSubmitConundrumGuess,
                                modifier = Modifier.fillMaxWidth().heightIn(min = sdp(52.dp))
                            )
                            Text("Multiple guesses allowed. Respect rate limit.", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                        }
                        else -> Unit
                    }
                }
            } else {
                Spacer(Modifier.height(sdp(8.dp)))
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<String>, doubleIndex: Int? = null, tripleIndex: Int? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            val letter = if (index < letters.size) letters[index] else "_"
            LetterTile(
                letter = letter,
                revealed = index < letters.size,
                index = index,
                accentColor = if (letter == "_") ColorDimText.copy(alpha = 0.4f) else tileAccent(index, doubleIndex, tripleIndex)
            )
        }
    }
}

@Composable
private fun SelectableLetterSlots(
    letters: List<String>,
    selectedIndices: Set<Int>,
    doubleIndex: Int?,
    tripleIndex: Int?,
    enabled: Boolean,
    onLetterTapped: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(sdp(4.dp))
    ) {
        repeat(9) { index ->
            val letter = letters.getOrNull(index)?.firstOrNull()?.toString() ?: "_"
            val selectable = enabled && index < letters.size && letter != "_" && !selectedIndices.contains(index)
            val selected = selectedIndices.contains(index)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(
                        if (selected) ColorSurfaceVariant.copy(alpha = 0.35f) else ColorSurfaceVariant,
                        RoundedCornerShape(sdp(6.dp))
                    )
                    .border(
                        sdp(1.5.dp),
                        if (selected) ColorDimText
                        else if (letter == "_") ColorDimText.copy(alpha = 0.3f)
                        else tileAccent(index, doubleIndex, tripleIndex).copy(alpha = 0.8f),
                        RoundedCornerShape(sdp(6.dp))
                    )
                    .clickable(enabled = selectable) { onLetterTapped(index) }
            ) {
                Text(
                    text = letter.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (selected) ColorDimText
                    else if (letter == "_") ColorDimText.copy(alpha = 0.3f)
                    else tileAccent(index, doubleIndex, tripleIndex),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WordTargetRow(
    sourceLetters: List<Char>,
    selectedIndices: List<Int>,
    doubleIndex: Int?,
    tripleIndex: Int?
) {
    val maxLetters = 9
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = sdp(58.dp))
            .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
            .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
            .padding(sdp(10.dp))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selectedIndices.isEmpty()) {
                Text("Tap big letters to build your word", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                repeat(maxLetters) { index ->
                    val sourceIndex = selectedIndices.getOrNull(index)
                    val ch = sourceIndex?.let { sourceLetters.getOrNull(it) }
                    if (ch != null) {
                        RisingWordTile(
                            letter = ch.toString(),
                            index = index,
                            accentColor = tileAccent(sourceIndex, doubleIndex, tripleIndex)
                        )
                    } else {
                        LetterTile(letter = "_", revealed = true, index = index, accentColor = ColorDimText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RisingWordTile(letter: String, index: Int, accentColor: Color) {
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
        accentColor = accentColor,
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
        Row(horizontalArrangement = Arrangement.spacedBy(sdp(4.dp))) {
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

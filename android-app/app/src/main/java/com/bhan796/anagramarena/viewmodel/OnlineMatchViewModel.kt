package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineMatchReducer
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.repository.NoOpTelemetryLogger
import com.bhan796.anagramarena.repository.OnlineMatchRepository
import com.bhan796.anagramarena.repository.TelemetryLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class OnlineMatchViewModel(
    private val repository: OnlineMatchRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val telemetry: TelemetryLogger = NoOpTelemetryLogger()
) : ViewModel() {
    private val _state = MutableStateFlow(OnlineUiState())
    val state: StateFlow<OnlineUiState> = _state.asStateFlow()

    private val clockOffsetSamples = ArrayDeque<Long>()
    private var clockOffsetMs: Long = 0
    private var tickerJob: Job? = null

    init {
        repository.connect()

        viewModelScope.launch {
            combine(
                repository.connectionState,
                repository.session,
                repository.matchmaking,
                repository.matchState,
                repository.actionError
            ) { connection, session, matchmaking, matchState, actionError ->
                if (matchState != null) {
                    updateClockOffset(matchState.serverNowMs)
                }

                val previous = _state.value
                val reduced = OnlineMatchReducer.reduce(
                    previous = previous,
                    connection = connection,
                    session = session,
                    matchmaking = matchmaking,
                    matchState = matchState,
                    actionError = actionError,
                    nowMs = nowProvider(),
                    serverClockOffsetMs = clockOffsetMs
                )

                val resetWord =
                    previous.matchState?.roundNumber != reduced.matchState?.roundNumber ||
                        previous.matchState?.phase != reduced.matchState?.phase

                _state.value = reduced.copy(
                    wordInput = if (resetWord) "" else previous.wordInput,
                    conundrumGuessInput = if (resetWord) "" else previous.conundrumGuessInput,
                    hasSubmittedWord = if (resetWord) false else previous.hasSubmittedWord,
                    hasSubmittedConundrumGuess = if (resetWord) false else reduced.hasSubmittedConundrumGuess,
                    opponentSubmittedConundrumGuess = if (resetWord) false else reduced.opponentSubmittedConundrumGuess,
                    localValidationMessage = if (resetWord) null else previous.localValidationMessage
                )
            }.collect {}
        }

        startTicker()
    }

    fun startQueue(mode: String = "casual") {
        if (_state.value.matchState != null) {
            _state.value = _state.value.copy(localValidationMessage = "You are already in an active match.")
            return
        }
        if (mode == "ranked" && !_state.value.isAuthenticated) {
            _state.value = _state.value.copy(localValidationMessage = "Sign in to play ranked mode.")
            return
        }
        telemetry.log("queue_start")
        repository.identify(null)
        repository.joinQueue(mode)
    }

    fun cancelQueue() {
        telemetry.log("queue_cancel")
        repository.leaveQueue()
    }

    fun retryConnect() {
        repository.connect()
        repository.identify(null)
        repository.resume()
    }

    fun pickVowel() {
        repository.pickVowel()
    }

    fun pickConsonant() {
        repository.pickConsonant()
    }

    fun updateWordInput(value: String) {
        _state.value = _state.value.copy(wordInput = value)
    }

    fun submitWord() {
        if (_state.value.hasSubmittedWord) return
        if (_state.value.wordInput.trim().isEmpty()) {
            _state.value = _state.value.copy(localValidationMessage = "Enter a word before submitting.")
            return
        }
        _state.value = _state.value.copy(localValidationMessage = null)
        repository.submitWord(_state.value.wordInput)
        _state.value = _state.value.copy(hasSubmittedWord = true)
    }

    fun updateConundrumGuessInput(value: String) {
        _state.value = _state.value.copy(conundrumGuessInput = value)
    }

    fun submitConundrumGuess() {
        if (_state.value.hasSubmittedConundrumGuess) return
        if (_state.value.conundrumGuessInput.trim().isEmpty()) {
            _state.value = _state.value.copy(localValidationMessage = "Enter a guess before submitting.")
            return
        }
        _state.value = _state.value.copy(localValidationMessage = null)
        repository.submitConundrumGuess(_state.value.conundrumGuessInput)
        _state.value = _state.value.copy(hasSubmittedConundrumGuess = true)
    }

    fun clearError() {
        repository.clearActionError()
        _state.value = _state.value.copy(lastError = null, localValidationMessage = null)
    }

    fun leaveActiveMatch() {
        repository.forfeitActiveMatch()
        _state.value = _state.value.copy(lastError = null, localValidationMessage = null)
    }

    fun queuePlayAgain() {
        val requestedMode = _state.value.matchState?.mode ?: "casual"
        val mode = if (requestedMode == "ranked" && !_state.value.isAuthenticated) "casual" else requestedMode
        if (_state.value.matchState?.phase == MatchPhase.FINISHED) {
            _state.value = _state.value.copy(
                matchId = null,
                matchState = null,
                myPlayer = null,
                opponentPlayer = null,
                isMyTurnToPick = false,
                secondsRemaining = 0,
                hasSubmittedWord = false,
                hasSubmittedConundrumGuess = false,
                opponentSubmittedConundrumGuess = false,
                wordInput = "",
                conundrumGuessInput = "",
                localValidationMessage = null,
                lastError = null
            )
        }
        telemetry.log("queue_play_again")
        repository.identify(null)
        repository.joinQueue(mode)
        _state.value = _state.value.copy(
            queueMode = mode,
            queueState = "searching",
            isInMatchmaking = true
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                val matchState = current.matchState
                if (matchState != null && matchState.phase != MatchPhase.FINISHED) {
                    val remaining = OnlineMatchReducer.computeRemainingSeconds(matchState, nowProvider(), clockOffsetMs)
                    var nextState = current.copy(secondsRemaining = remaining)
                    if (
                        matchState.phase == MatchPhase.LETTERS_SOLVING &&
                        remaining <= 2 &&
                        !current.hasSubmittedWord
                    ) {
                        repository.submitWord(current.wordInput)
                        nextState = nextState.copy(
                            hasSubmittedWord = true,
                            localValidationMessage = null
                        )
                    }
                    _state.value = nextState
                }
            }
        }
    }

    private fun updateClockOffset(serverNowMs: Long) {
        val sample = serverNowMs - nowProvider()
        clockOffsetSamples.addLast(sample)
        if (clockOffsetSamples.size > 8) {
            clockOffsetSamples.removeFirst()
        }
        clockOffsetMs = clockOffsetSamples.average().toLong()
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        repository.disconnect()
    }

    companion object {
        fun factory(repository: OnlineMatchRepository, telemetry: TelemetryLogger = NoOpTelemetryLogger()): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnlineMatchViewModel(repository = repository, telemetry = telemetry) as T
                }
            }
        }
    }
}

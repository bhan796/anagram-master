package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineMatchReducer
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.repository.OnlineMatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class OnlineMatchViewModel(
    private val repository: OnlineMatchRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
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
                    hasSubmittedWord = if (resetWord) false else previous.hasSubmittedWord
                )
            }.collect {}
        }

        startTicker()
    }

    fun startQueue(displayName: String?) {
        repository.identify(displayName)
        repository.joinQueue()
    }

    fun cancelQueue() {
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
        repository.submitWord(_state.value.wordInput)
        _state.value = _state.value.copy(hasSubmittedWord = true)
    }

    fun updateConundrumGuessInput(value: String) {
        _state.value = _state.value.copy(conundrumGuessInput = value)
    }

    fun submitConundrumGuess() {
        repository.submitConundrumGuess(_state.value.conundrumGuessInput)
    }

    fun clearError() {
        repository.clearActionError()
        _state.value = _state.value.copy(lastError = null)
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
                    _state.value = current.copy(secondsRemaining = remaining)
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
        fun factory(repository: OnlineMatchRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnlineMatchViewModel(repository) as T
                }
            }
        }
    }
}

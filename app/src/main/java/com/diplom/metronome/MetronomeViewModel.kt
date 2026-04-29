package com.diplom.metronome

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MetronomeState(
    val bpm: Int = 120,
    val beatsPerBar: Int = 4,
    val denominator: Int = 4,
    val isPlaying: Boolean = false,
    val currentBeat: Int = -1
)

class MetronomeViewModel : ViewModel() {

    private val engine = MetronomeEngine()

    private val _state = MutableStateFlow(MetronomeState())
    val state = _state.asStateFlow()

    fun setBpm(bpm: Int) {
        _state.value = _state.value.copy(bpm = bpm.coerceIn(40, 240))
        if (_state.value.isPlaying) restartEngine()
    }

    fun setBeatsPerBar(beats: Int) {
        _state.value = _state.value.copy(beatsPerBar = beats.coerceIn(1, 16))
        if (_state.value.isPlaying) restartEngine()
    }

    fun setDenominator(denom: Int) {
        _state.value = _state.value.copy(denominator = denom)
        if (_state.value.isPlaying) restartEngine()
    }

    fun togglePlay() {
        if (_state.value.isPlaying) {
            engine.stop()
            _state.value = _state.value.copy(isPlaying = false, currentBeat = -1)
        } else {
            startEngine()
        }
    }

    fun stop() {
        engine.stop()
        _state.value = _state.value.copy(isPlaying = false, currentBeat = -1)
    }

    private fun startEngine() {
        val s = _state.value
        _state.value = s.copy(isPlaying = true)

        // Базовый интервал для четверти (знаменатель 4)
        // При знаменателе 2 — вдвое быстрее, при 8 — вдвое медленнее
        val bpmAdjusted = s.bpm * s.denominator / 4

        engine.start(bpmAdjusted, s.beatsPerBar) { beat ->
            _state.value = _state.value.copy(currentBeat = beat)
        }
    }

    private fun restartEngine() {
        engine.stop()
        startEngine()
    }

    override fun onCleared() {
        engine.release()
    }
}
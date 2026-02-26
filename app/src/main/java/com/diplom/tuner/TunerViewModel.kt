package com.diplom.tuner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import android.annotation.SuppressLint

class TunerViewModel(private val context: Context) : ViewModel() {

    private var recordingThread: Thread? = null
    @Volatile
    private var isRunning = false
    private var smoothedCents = 0.0

    // UI State
    private val _currentNote = MutableStateFlow("--")
    val currentNote: StateFlow<String> = _currentNote

    private val _currentCents = MutableStateFlow(0.0)
    val currentCents: StateFlow<Double> = _currentCents

    private val _referenceFreq = MutableStateFlow(0.0)
    val referenceFreq: StateFlow<Double> = _referenceFreq

    // Диапазон нот E1 до G6
    private val allNotes = generateAllNotes()

    private fun generateAllNotes(): List<Pair<String, Double>> {
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val list = mutableListOf<Pair<String, Double>>()
        for (oct in 1..6) {
            for ((i, n) in notes.withIndex()) {
                val midi = (oct + 1) * 12 + i
                val freq = 440.0 * 2.0.pow((midi - 69) / 12.0)
                list.add(n + oct to freq)
            }
        }
        return list
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission() || isRunning) return
        isRunning = true

        val sampleRate = 44100
        val bufferSize = 4096
        val audioBuffer = ShortArray(bufferSize)

        recordingThread = Thread {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            try {
                recorder.startRecording()
            } catch (e: SecurityException) {
                isRunning = false
                return@Thread
            }

            while (isRunning) {
                val read = recorder.read(audioBuffer, 0, bufferSize)
                if (read <= 0) continue

                val amplitude = audioBuffer.take(read).maxOf { abs(it.toInt()) }
                if (amplitude < 500) {
                    _currentNote.value = "--"
                    _currentCents.value = 0.0
                    _referenceFreq.value = 0.0
                    continue
                }

                val freq = detectPitch(audioBuffer, read, sampleRate)
                if (freq <= 0.0 || freq.isNaN() || freq.isInfinite()) continue
                if (freq < 30.0 || freq > 2000.0) continue

                val nearest = allNotes.minByOrNull { abs(1200 * log2(freq / it.second)) } ?: continue
                val noteName = nearest.first
                val refFreq = nearest.second

                val cents = (1200 * log2(freq / refFreq)).coerceIn(-50.0, 50.0)

                val alpha = 0.25
                smoothedCents = smoothedCents * (1 - alpha) + cents * alpha

                _currentNote.value = noteName
                _currentCents.value = smoothedCents
                _referenceFreq.value = refFreq
            }

            recorder.stop()
            recorder.release()
        }
        recordingThread?.start()
    }

    fun stop() {
        isRunning = false
        recordingThread?.interrupt()
        recordingThread = null
        smoothedCents = 0.0
        _currentNote.value = "--"
        _currentCents.value = 0.0
        _referenceFreq.value = 0.0
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private fun detectPitch(buffer: ShortArray, read: Int, sampleRate: Int): Double {
        var bestLag = 0
        var maxCorr = 0.0
        val minLag = sampleRate / 2000
        val maxLag = sampleRate / 30
        for (lag in minLag..maxLag) {
            var corr = 0.0
            for (i in 0 until read - lag) corr += buffer[i] * buffer[i + lag]
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }
        return if (bestLag > 0) sampleRate.toDouble() / bestLag else 0.0
    }

    // Для UI кнопки
    fun isTunerRunning(): Boolean = isRunning
}
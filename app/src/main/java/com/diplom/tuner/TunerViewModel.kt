package com.diplom.tuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.absoluteValue
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class TunerViewModel : ViewModel() {

    private var recordingThread: Thread? = null
    private var isRunning = false

    private var lastStableTime = 0L
    private val holdTimeMs = 700L

    // ===== ЛОГИКА ФИКСАЦИИ НОТЫ =====
    private var isNoteLocked = false
    private var previousRms = 0.0
    private val attackMultiplier = 1.8   // чувствительность атаки

    // ======== АДАПТИВНЫЙ NOISE GATE ========
    private var noiseFloor = 0.0
    private var isCalibrated = false
    private val calibrationFrames = 20
    private var calibrationCount = 0
    private val noiseMultiplier = 2.5   // чувствительность (2.0–3.0)

    private val _currentFrequency = MutableStateFlow(0f)
    val currentFrequency: StateFlow<Float> = _currentFrequency

    private val _currentNote = MutableStateFlow("")
    val currentNote: StateFlow<String> = _currentNote

    private val _deviation = MutableStateFlow(0f)
    val deviation: StateFlow<Float> = _deviation

    private val freqHistory = mutableListOf<Float>()
    private val freqHistorySize = 5
    private val freqThreshold = 1f

    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        val sampleRate = 22050
        val bufferSize = 1024
        val audioBuffer = ShortArray(bufferSize)

        recordingThread = Thread {
            try {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )

                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    android.media.audiofx.NoiseSuppressor.create(recorder.audioSessionId)
                }

                recorder.startRecording()

                while (isRunning) {

                    val read = recorder.read(audioBuffer, 0, bufferSize)
                    if (read <= 0) continue

                    val rms = getRMS(audioBuffer, read)

                    // ===== КАЛИБРОВКА ФОНА =====
                    if (!isCalibrated) {
                        noiseFloor += rms
                        calibrationCount++

                        if (calibrationCount >= calibrationFrames) {
                            noiseFloor /= calibrationFrames
                            isCalibrated = true
                        }
                        continue
                    }

                    // ===== ОТСЕЧЕНИЕ ШУМА =====
                    if (rms < noiseFloor * noiseMultiplier) {
                        continue
                    }

                    val freq = detectPitch(audioBuffer, read, sampleRate)

                    // Фильтр диапазона (гитара 50–1000 Hz)
                    if (freq !in 50f..1000f) continue

                    val deviationValue = computeDeviation(freq)
                    val now = System.currentTimeMillis()

                    // Стрелка обновляется всегда
                    _currentFrequency.value = freq
                    _deviation.value = deviationValue

                    // Нота только при стабильности
                    if (isPitchStable(freq)) {
                        lastStableTime = now
                        _currentNote.value = freqToNote(freq)
                    } else {
                        if (now - lastStableTime > holdTimeMs) {
                            _currentNote.value = "…"
                        }
                    }
                }

                recorder.stop()
                recorder.release()

            } catch (e: SecurityException) {
                isRunning = false
            }
        }

        recordingThread?.start()
    }

    fun stop() {
        isRunning = false
        recordingThread?.join()
        recordingThread = null
        freqHistory.clear()

        // сброс калибровки
        noiseFloor = 0.0
        calibrationCount = 0
        isCalibrated = false
    }

    fun isRunning() = isRunning

    // ================= RMS =================

    private fun getRMS(buffer: ShortArray, read: Int): Double {
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / read)
    }

    // ================= СТАБИЛЬНОСТЬ =================

    private fun isPitchStable(freq: Float): Boolean {
        freqHistory.add(freq)
        if (freqHistory.size > freqHistorySize) {
            freqHistory.removeAt(0)
        }

        val minFreq = freqHistory.minOrNull() ?: freq
        val maxFreq = freqHistory.maxOrNull() ?: freq

        return (maxFreq - minFreq) <= freqThreshold
    }

    // ================= PITCH =================

    private fun detectPitch(buffer: ShortArray, read: Int, sampleRate: Int): Float {
        var maxCorr = 0.0
        var bestLag = 0

        for (lag in 20..(sampleRate / 50)) {
            var corr = 0.0
            for (i in 0 until read - lag) {
                corr += buffer[i] * buffer[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag != 0) sampleRate.toFloat() / bestLag else 0f
    }

    private fun freqToNote(freq: Float): String {
        val A4 = 440.0
        val noteNames = arrayOf(
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
        )

        val n = (12 * log2(freq / A4)).roundToInt()
        val noteIndex = (n + 9).mod(12)
        val octave = 4 + ((n + 9) / 12)

        return noteNames[noteIndex] + octave
    }

    private fun computeDeviation(freq: Float): Float {
        val noteFreq = noteToFreq(freqToNote(freq))
        return freq - noteFreq
    }

    private fun noteToFreq(note: String): Float {
        val noteNames = mapOf(
            "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4,
            "F" to 5, "F#" to 6, "G" to 7, "G#" to 8,
            "A" to 9, "A#" to 10, "B" to 11
        )

        val name = note.dropLast(1)
        val octave = note.last().digitToInt()
        val n = noteNames[name]!! + (octave - 4) * 12

        return (440.0 * 2.0.pow(n / 12.0)).toFloat()
    }
}
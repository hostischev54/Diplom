package com.diplom.tuner

import android.Manifest
import android.annotation.SuppressLint
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

class TunerViewModel(private val context: Context) : ViewModel() {

    private var recordingThread: Thread? = null
    @Volatile
    private var isRunning = false
    private var smoothedCents = 0.0

    // ================= UI State =================

    private val _currentNote = MutableStateFlow("--")
    val currentNote: StateFlow<String> = _currentNote

    private val _currentCents = MutableStateFlow(0.0)
    val currentCents: StateFlow<Double> = _currentCents

    private val _referenceFreq = MutableStateFlow(0.0)
    val referenceFreq: StateFlow<Double> = _referenceFreq

    // A4 по умолчанию 440 Hz
    private val _referenceA = MutableStateFlow(440.0)
    val referenceA: StateFlow<Double> = _referenceA

    private val _useFlats = MutableStateFlow(false)
    val useFlats: StateFlow<Boolean> = _useFlats

    private val sharpToFlat = mapOf(
        "C#" to "Db",
        "D#" to "Eb",
        "F#" to "Gb",
        "G#" to "Ab",
        "A#" to "Bb"
    )
    private val noteToSolfege = mapOf(
        "C" to "До",
        "C#" to "До#",
        "Db" to "Реb",

        "D" to "Ре",
        "D#" to "Ре#",
        "Eb" to "Миb",

        "E" to "Ми",

        "F" to "Фа",
        "F#" to "Фа#",
        "Gb" to "Сольb",

        "G" to "Соль",
        "G#" to "Соль#",
        "Ab" to "Ляb",

        "A" to "Ля",
        "A#" to "Ля#",
        "Bb" to "Сиb",

        "B" to "Си"
    )

    // ================= НОТЫ =================
    private val _useSolfege = MutableStateFlow(false)
    val useSolfege: StateFlow<Boolean> = _useSolfege

    fun setSolfegeSystem(enabled: Boolean) {
        _useSolfege.value = enabled
    }

    private var allNotes = generateAllNotes(_referenceA.value)

    private fun generateAllNotes(referenceA: Double): List<Pair<String, Double>> {
        val notes = arrayOf(
            "C", "C#", "D", "D#", "E",
            "F", "F#", "G", "G#", "A", "A#", "B"
        )

        val list = mutableListOf<Pair<String, Double>>()

        for (oct in 1..6) {
            for ((i, n) in notes.withIndex()) {
                val midi = (oct + 1) * 12 + i
                val freq = referenceA * 2.0.pow((midi - 69) / 12.0)
                list.add(n + oct to freq)
            }
        }

        return list
    }

    // ================= Изменение #/b =================
    fun toggleNoteSystem() {
        _useFlats.value = !_useFlats.value
    }
    fun formatNoteForDisplay(note: String): String {

        val match = Regex("([A-G]#?)(\\d)").find(note) ?: return note
        var (base, octave) = match.destructured

        // сначала применяем систему #/b
        if (_useFlats.value) {
            base = sharpToFlat[base] ?: base
        }

        // потом переводим в сольфеджио
        if (_useSolfege.value) {
            base = noteToSolfege[base] ?: base
        }

        return base + octave
    }
    fun formatTuningNote(note: String): String {

        val match = Regex("([A-G]#?)(\\d)").find(note) ?: return note
        var (base, octave) = match.destructured

        if (_useFlats.value) {
            base = sharpToFlat[base] ?: base
        }

        return base + octave
    }
    // ================= Изменение A4 =================

    fun increaseReferenceA() {
        if (_referenceA.value < 455) {
            _referenceA.value += 1
            allNotes = generateAllNotes(_referenceA.value)
        }
    }

    fun setNoteSystem(useFlats: Boolean) {
        _useFlats.value = useFlats
    }

    fun decreaseReferenceA() {
        if (_referenceA.value > 415) {
            _referenceA.value -= 1
            allNotes = generateAllNotes(_referenceA.value)
        }
    }

    fun setReferenceA(value: Double) {
        if (value in 415.0..455.0) {
            _referenceA.value = value
            allNotes = generateAllNotes(_referenceA.value)
        }
    }

    // ================= Permission =================

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ================= Check string for help =================
    fun checkString(targetNote: String, targetFreq: Double): Pair<String, Boolean> {
        val currentFreq = _referenceFreq.value  // текущая частота обнаруженной ноты
        val centsDiff = _currentCents.value     // для "Готово" используется ±5 центов

        return when {
            abs(centsDiff) <= 5 -> "Готово" to true

            currentFreq < targetFreq -> "Повысить натяжение" to false
            currentFreq > targetFreq -> "Понизить натяжение" to false

            else -> "Настройте ноту $targetNote" to false
        }
    }

    // ================= START =================

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

                val nearest = allNotes.minByOrNull {
                    abs(1200 * log2(freq / it.second))
                } ?: continue

                val noteName = nearest.first
                val refFreq = nearest.second

                val cents = (1200 * log2(freq / refFreq))
                    .coerceIn(-50.0, 50.0)

                val alpha = 0.25
                smoothedCents =
                    smoothedCents * (1 - alpha) + cents * alpha

                _currentNote.value = noteName
                _currentCents.value = smoothedCents
                _referenceFreq.value = freq
            }

            recorder.stop()
            recorder.release()
        }

        recordingThread?.start()
    }

    // ================= STOP =================

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

    // ================= Pitch Detection =================

    private fun detectPitch(
        buffer: ShortArray,
        read: Int,
        sampleRate: Int
    ): Double {

        var bestLag = 0
        var maxCorr = 0.0

        val minLag = sampleRate / 2000
        val maxLag = sampleRate / 30

        for (lag in minLag..maxLag) {
            var corr = 0.0
            for (i in 0 until read - lag) {
                corr += buffer[i] * buffer[i + lag]
            }

            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        return if (bestLag > 0)
            sampleRate.toDouble() / bestLag
        else
            0.0
    }

    fun isTunerRunning(): Boolean = isRunning
}
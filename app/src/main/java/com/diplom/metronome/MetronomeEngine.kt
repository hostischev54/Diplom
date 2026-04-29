package com.diplom.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

class MetronomeEngine {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 44100

    // Генерация синусоидального клика
    private fun generateClick(frequency: Double, durationMs: Int, amplitude: Float): ShortArray {
        val samples = (sampleRate * durationMs / 1000.0).toInt()
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val envelope = exp(-5.0 * i / samples) // затухание
            val sample = amplitude * envelope * sin(2.0 * PI * frequency * i / sampleRate)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private val accentClick = generateClick(1200.0, 60, 0.9f)  // сильная доля
    private val normalClick = generateClick(800.0, 60, 0.6f)   // слабая доля

    private fun playClick(buffer: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()

        // Освобождаем после воспроизведения
        scope.launch {
            delay(200)
            track.stop()
            track.release()
        }
    }

    fun start(bpm: Int, beatsPerBar: Int, onBeat: (beat: Int) -> Unit) {
        stop()
        val intervalMs = (60_000.0 / bpm).toLong()
        var beat = 0

        job = scope.launch {
            while (isActive) {
                val isAccent = beat % beatsPerBar == 0
                playClick(if (isAccent) accentClick else normalClick)
                onBeat(beat % beatsPerBar)
                beat++
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() {
        scope.cancel()
    }
}
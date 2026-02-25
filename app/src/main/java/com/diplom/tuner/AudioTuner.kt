package com.diplom.tuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

class AudioTuner(
    private val onFrequencyDetected: (Float) -> Unit
) {

    private val sampleRate = 44100
    private val bufferSize = 2048

    private var isRunning = false
    private lateinit var audioRecord: AudioRecord

    fun start() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        isRunning = true
        audioRecord.startRecording()

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isRunning) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val freq = detectFrequency(buffer.map { it.toFloat() }.toFloatArray(), sampleRate)
                    if (freq > 0) {
                        onFrequencyDetected(freq)
                    }
                }
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        audioRecord.stop()
        audioRecord.release()
    }


    // простой метод определения частоты (по максимальному пику)
    private fun detectFrequency(signal: FloatArray, sampleRate: Int): Float {
        var maxIndex = 0
        var maxValue = 0f

        for (i in signal.indices) {
            val v = abs(signal[i])
            if (v > maxValue) {
                maxValue = v
                maxIndex = i
            }
        }

        return if (maxIndex == 0) 0f
        else sampleRate.toFloat() / maxIndex
    }
}
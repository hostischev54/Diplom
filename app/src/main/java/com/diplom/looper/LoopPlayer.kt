package com.diplom.looper

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LoopPlayer(
    private val processedPath: String,
    private val volume: Float
) {
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private var _currentPositionMs = 0L
    val currentPositionMs get() = _currentPositionMs

    private val sampleRate = AudioRecorder.SAMPLE_RATE

    fun start(scope: CoroutineScope, startPositionMs: Long = 0L) {
        stop()
        val pcm = loadPcm(processedPath) ?: return
        if (pcm.isEmpty()) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

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
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(volume)
        track.play()
        audioTrack = track

        // Стартовая позиция в сэмплах
        var sampleOffset = ((startPositionMs * sampleRate) / 1000L)
            .toInt().coerceIn(0, pcm.size)

        val loopDurationMs = (pcm.size.toLong() * 1000L) / sampleRate

        job = scope.launch(Dispatchers.IO) {
            val buf = ShortArray(bufferSize / 2)
            while (isActive) {
                var filled = 0
                while (filled < buf.size) {
                    val remaining = pcm.size - sampleOffset
                    val toCopy = minOf(buf.size - filled, remaining)
                    pcm.copyInto(buf, filled, sampleOffset, sampleOffset + toCopy)
                    filled += toCopy
                    sampleOffset += toCopy
                    if (sampleOffset >= pcm.size) {
                        sampleOffset = 0  // бесшовный переход
                    }
                }
                track.write(buf, 0, buf.size)
                // Обновляем позицию
                _currentPositionMs = (sampleOffset.toLong() * 1000L) / sampleRate
            }
        }
    }

    fun pause() {
        audioTrack?.pause()
        job?.cancel()
        // Сохраняем позицию
        _currentPositionMs = (audioTrack?.playbackHeadPosition?.toLong()
            ?.let { it * 1000L / sampleRate } ?: _currentPositionMs) %
                getDurationMs().coerceAtLeast(1L)
    }

    fun resume(scope: CoroutineScope) {
        start(scope, _currentPositionMs)
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) { /* ignore */ }
        audioTrack = null
    }

    fun seekTo(posMs: Long) {
        _currentPositionMs = posMs
    }

    fun setVolume(vol: Float) {
        audioTrack?.setVolume(vol)
    }

    fun getDurationMs(): Long {
        return try {
            val pcmBytes = (File(processedPath).length() - 44).coerceAtLeast(0)
            (pcmBytes * 1000L) / (sampleRate * 2)
        } catch (e: Exception) { 0L }
    }

    private fun loadPcm(path: String): ShortArray? {
        return try {
            val bytes = File(path).readBytes()
            val pcmBytes = bytes.size - 44
            if (pcmBytes <= 0) return null
            val count = pcmBytes / 2
            val result = ShortArray(count)
            val buf = ByteBuffer.wrap(bytes, 44, pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) result[i] = buf.short
            result
        } catch (e: Exception) { null }
    }
}
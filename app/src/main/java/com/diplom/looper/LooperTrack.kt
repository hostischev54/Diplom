package com.diplom.looper

data class LooperTrack(
    val id: Int,
    val rawPath: String,
    var processedPath: String = "",
    var isMuted: Boolean = false,
    var volume: Float = 1f,
    var trimStartMs: Long = 0,
    var trimEndMs: Long = 0,
    var waveformData: FloatArray = floatArrayOf(),
    var isProcessing: Boolean = false,
    var isImported: Boolean = false,
    var playbackPositionMs: Long = 0L,
    var isPaused: Boolean = false,
    var waveformVersion: Int = 0,
    var isPlayingAlone: Boolean = false   // воспроизводится отдельно (не в ансамбле)

)

enum class LooperState {
    IDLE, RECORDING, PLAYING, PROCESSING
}

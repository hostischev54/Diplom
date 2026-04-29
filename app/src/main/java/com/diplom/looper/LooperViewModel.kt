package com.diplom.looper

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job

class LooperViewModel(private val context: Context) : ViewModel() {

    val tracks = mutableStateListOf<LooperTrack>()
    var looperState by mutableStateOf(LooperState.IDLE)
    var errorMessage by mutableStateOf("")
    var showSavedDialog by mutableStateOf(false)
    var headphonesConnected by mutableStateOf(false)

    private val recorder = AudioRecorder(context)
    private val players  = mutableMapOf<Int, LoopPlayer>()
    private val processingJobs = mutableMapOf<Int, Job>()
    private var headphoneCallback: android.media.AudioDeviceCallback? = null

    fun startWatchingHeadphones() {
        headphonesConnected = recorder.isHeadphonesConnected(context)
        headphoneCallback = recorder.registerHeadphoneCallback(context) { connected ->
            headphonesConnected = connected
        }
    }

    fun stopWatchingHeadphones() {
        headphoneCallback?.let { recorder.unregisterHeadphoneCallback(context, it) }
        headphoneCallback = null
    }

    // ── Запись ───────────────────────────────────────────────────────────────
    fun addTrack() {
        if (tracks.size >= 5) { errorMessage = "Максимум 5 дорожек"; return }
        val id = nextId()
        looperState = LooperState.RECORDING
        viewModelScope.launch {
            try {
                val path = recorder.startRecording(id)
                val durationMs = getWavDurationMs(path)
                val track = LooperTrack(
                    id = id, rawPath = path,
                    trimStartMs = 0, trimEndMs = durationMs,
                    isProcessing = true
                )
                tracks.add(track)
                looperState = LooperState.IDLE
                processTrack(track.id, 0, durationMs)
            } catch (e: Exception) {
                errorMessage = "Ошибка записи: ${e.message}"
                looperState = LooperState.IDLE
            }
        }
    }

    fun seekTrack(trackId: Int, positionMs: Long) {
        players[trackId]?.seekTo(positionMs)
        val i = tracks.indexOfFirst { it.id == trackId }
        if (i != -1) tracks[i] = tracks[i].copy(playbackPositionMs = positionMs)
    }

    fun stopRecording() { recorder.stopRecording() }

    // ── Импорт MP3 ────────────────────────────────────────────────────────────
    fun importMp3(uri: Uri) {
        if (tracks.size >= 5) { errorMessage = "Максимум 5 дорожек"; return }
        val id = nextId()
        viewModelScope.launch {
            try {
                val mp3File = File(recorder.getOutputDir(), "track_${id}_imported.mp3")
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    mp3File.outputStream().use { inp.copyTo(it) }
                }
                val wavPath = File(recorder.getOutputDir(), "track_${id}_raw.wav").absolutePath
                val track = LooperTrack(id = id, rawPath = wavPath,
                    isProcessing = true, isImported = true)
                tracks.add(track)

                val ok = AudioProcessor.convertMp3ToWav(mp3File.absolutePath, wavPath)
                if (!ok) {
                    errorMessage = "Не удалось конвертировать файл"
                    tracks.removeAll { it.id == id }
                    return@launch
                }
                val durationMs = getWavDurationMs(wavPath)
                updateTrack(id) { it.copy(trimStartMs = 0, trimEndMs = durationMs) }
                processTrack(id, 0, durationMs)
            } catch (e: Exception) {
                errorMessage = "Ошибка импорта: ${e.message}"
                tracks.removeAll { it.id == id }
            }
        }
    }

    // ── Мягкая обрезка (только сохраняет позиции, без processLoop) ────────────
    fun saveTrimPreview(trackId: Int, startMs: Long, endMs: Long) {
        updateTrack(trackId) { it.copy(trimStartMs = startMs, trimEndMs = endMs) }
    }

    // Вызывается при отпускании ползунка — реальная перерезка + перезапуск плеера
    fun applyTrimPreview(trackId: Int, startMs: Long, endMs: Long) {
        processingJobs[trackId]?.cancel()

        val wasPlaying      = players[trackId] != null &&
                tracks.find { it.id == trackId }?.isPaused == false &&
                tracks.find { it.id == trackId }?.isPlayingAlone == true
        val wasGlobalPlaying = looperState == LooperState.PLAYING &&
                tracks.find { it.id == trackId }?.isMuted == false

        players[trackId]?.stop()
        players.remove(trackId)

        updateTrack(trackId) { it.copy(isProcessing = true, playbackPositionMs = 0L) }

        val job = viewModelScope.launch {
            val track = tracks.find { it.id == trackId } ?: return@launch
            val outPath = File(recorder.getOutputDir(), "track_${trackId}_loop.wav").absolutePath

            val success = AudioProcessor.processLoop(
                inputPath   = track.rawPath,
                outputPath  = outPath,
                trimStartMs = startMs,
                trimEndMs   = endMs
            )
            ensureActive()

            if (success) {
                updateTrack(trackId) {
                    it.copy(
                        processedPath = outPath,
                        isProcessing  = false,
                        trimStartMs   = startMs,
                        trimEndMs     = endMs
                    )
                }
                if (wasPlaying || wasGlobalPlaying) {
                    val updatedTrack = tracks.find { it.id == trackId } ?: return@launch
                    val player = LoopPlayer(updatedTrack.processedPath, updatedTrack.volume)
                    player.start(viewModelScope, 0L)
                    players[trackId] = player
                    updateTrack(trackId) {
                        it.copy(isPaused = false, isPlayingAlone = wasPlaying)
                    }
                }
            } else {
                errorMessage = "Ошибка обработки дорожки"
                updateTrack(trackId) { it.copy(isProcessing = false) }
            }
            processingJobs.remove(trackId)
        }
        processingJobs[trackId] = job
    }

    // ── Внутренняя обработка трека ────────────────────────────────────────────
    private fun processTrack(trackId: Int, startMs: Long, endMs: Long) {
        updateTrack(trackId) { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val track = tracks.find { it.id == trackId } ?: return@launch
            val outPath = File(recorder.getOutputDir(), "track_${trackId}_loop.wav").absolutePath
            val success = AudioProcessor.processLoop(
                inputPath    = track.rawPath,
                outputPath   = outPath,
                trimStartMs  = startMs,
                trimEndMs    = endMs
            )
            if (success) {
                updateTrack(trackId) {
                    it.copy(
                        processedPath = outPath,
                        isProcessing  = false,
                        trimStartMs   = startMs,
                        trimEndMs     = endMs
                    )
                }
            } else {
                errorMessage = "Ошибка обработки дорожки"
                updateTrack(trackId) { it.copy(isProcessing = false) }
            }
        }
    }

    // ── Жёсткая обрезка ножницами ─────────────────────────────────────────────
    fun applyHardTrim(trackId: Int) {
        val track = tracks.find { it.id == trackId } ?: return
        players[trackId]?.stop()
        players.remove(trackId)
        updateTrack(trackId) {
            it.copy(isProcessing = true, isPaused = false,
                playbackPositionMs = 0L, isPlayingAlone = false)
        }
        viewModelScope.launch {
            try {
                val pcmFull  = AudioProcessor.readPcm(track.rawPath)
                val totalMs  = getWavDurationMs(track.rawPath)
                if (totalMs == 0L || pcmFull.isEmpty()) {
                    updateTrack(trackId) { it.copy(isProcessing = false) }
                    return@launch
                }
                val startSample = ((track.trimStartMs * AudioRecorder.SAMPLE_RATE) / 1000)
                    .toInt().coerceIn(0, pcmFull.size)
                val endSample   = ((track.trimEndMs   * AudioRecorder.SAMPLE_RATE) / 1000)
                    .toInt().coerceIn(startSample, pcmFull.size)

                val trimmed = pcmFull.copyOfRange(startSample, endSample)
                if (trimmed.isEmpty()) {
                    errorMessage = "trim_empty"
                    updateTrack(trackId) { it.copy(isProcessing = false) }
                    return@launch
                }
                AudioProcessor.writePcmToWav(trimmed, track.rawPath)
                val newDurationMs = getWavDurationMs(track.rawPath)
                updateTrack(trackId) {
                    it.copy(trimStartMs = 0, trimEndMs = newDurationMs,
                        playbackPositionMs = 0L, isProcessing = false)
                }
                processTrack(trackId, 0, newDurationMs)
                updateTrack(trackId) { it.copy(waveformVersion = it.waveformVersion + 1) }
            } catch (e: Exception) {
                errorMessage = "trim_error:${e.message}"
                updateTrack(trackId) { it.copy(isProcessing = false) }
            }
        }
    }

    // ── Воспроизведение всех дорожек ──────────────────────────────────────────
    fun playAll() {
        if (looperState == LooperState.PLAYING) return
        stopAllSoloPlayers()
        looperState = LooperState.PLAYING

        tracks.filter { !it.isMuted && it.processedPath.isNotEmpty() && !it.isProcessing }
            .forEach { track ->
                players[track.id]?.stop()
                players.remove(track.id)

                val player = LoopPlayer(track.processedPath, track.volume)
                player.start(viewModelScope, 0L)
                players[track.id] = player

                val i = tracks.indexOfFirst { it.id == track.id }
                if (i != -1) tracks[i] = tracks[i].copy(
                    isPaused           = false,
                    playbackPositionMs = 0L
                )
            }
    }

    fun stopAll() {
        tracks.forEachIndexed { i, track ->
            val pos = players[track.id]?.currentPositionMs ?: 0L
            tracks[i] = tracks[i].copy(
                playbackPositionMs = pos,
                isPaused           = false,
                isPlayingAlone     = false
            )
        }
        players.values.forEach { it.stop() }
        players.clear()
        if (looperState == LooperState.PLAYING) looperState = LooperState.IDLE
    }

    // ── Одиночное воспроизведение трека ──────────────────────────────────────
    fun toggleSoloPlay(trackId: Int) {
        val track = tracks.find { it.id == trackId } ?: return
        if (track.processedPath.isEmpty() || track.isProcessing) return

        if (looperState == LooperState.PLAYING) stopAll()

        val existing = players[trackId]
        if (existing != null) {
            val pos = existing.currentPositionMs
            existing.stop()
            players.remove(trackId)
            updateTrack(trackId) {
                it.copy(isPlayingAlone = false, isPaused = false, playbackPositionMs = pos)
            }
        } else {
            stopAllSoloPlayers()
            val player = LoopPlayer(track.processedPath, track.volume)
            player.start(viewModelScope, track.playbackPositionMs)
            players[trackId] = player
            updateTrack(trackId) { it.copy(isPlayingAlone = true, isPaused = false) }
        }
    }

    private fun stopAllSoloPlayers() {
        val soloIds = tracks.filter { it.isPlayingAlone }.map { it.id }
        soloIds.forEach { id ->
            val pos = players[id]?.currentPositionMs ?: 0L
            players[id]?.stop()
            players.remove(id)
            updateTrack(id) {
                it.copy(isPlayingAlone = false, isPaused = false, playbackPositionMs = pos)
            }
        }
    }

    // ── Пауза / возобновление ─────────────────────────────────────────────────
    fun pauseTrack(trackId: Int) {
        val player = players[trackId] ?: return
        player.pause()
        val pos = player.currentPositionMs
        val i = tracks.indexOfFirst { it.id == trackId }
        if (i != -1) tracks[i] = tracks[i].copy(isPaused = true, playbackPositionMs = pos)
    }

    fun resumeTrack(trackId: Int) {
        players[trackId]?.resume(viewModelScope)
        val i = tracks.indexOfFirst { it.id == trackId }
        if (i != -1) tracks[i] = tracks[i].copy(isPaused = false)
    }

    fun getPlaybackPosition(trackId: Int): Long {
        return players[trackId]?.currentPositionMs
            ?: tracks.find { it.id == trackId }?.playbackPositionMs
            ?: 0L
    }

    // ── Настройки дорожек ─────────────────────────────────────────────────────
    fun setMute(trackId: Int, muted: Boolean) {
        val i = tracks.indexOfFirst { it.id == trackId }
        if (i != -1) {
            tracks[i] = tracks[i].copy(isMuted = muted)
            players[trackId]?.setVolume(if (muted) 0f else tracks[i].volume)
        }
    }

    fun setVolume(trackId: Int, volume: Float) {
        val i = tracks.indexOfFirst { it.id == trackId }
        if (i != -1) {
            tracks[i] = tracks[i].copy(volume = volume)
            players[trackId]?.setVolume(volume)
        }
    }

    fun deleteTrack(trackId: Int) {
        processingJobs[trackId]?.cancel()
        players[trackId]?.stop()
        players.remove(trackId)
        val track = tracks.find { it.id == trackId }
        track?.let {
            File(it.rawPath).delete()
            if (it.processedPath.isNotEmpty()) File(it.processedPath).delete()
        }
        tracks.removeAll { it.id == trackId }
    }

    // ── Рендер ────────────────────────────────────────────────────────────────
    fun renderAll() {
        looperState = LooperState.PROCESSING
        viewModelScope.launch {
            val outPath = File(
                context.getExternalFilesDir(null),
                "looper_mix_${System.currentTimeMillis()}.m4a"
            ).absolutePath
            val success = AudioProcessor.renderToM4a(tracks.toList(), outputPath = outPath)
            if (success) { saveToDownloads(outPath); showSavedDialog = true }
            errorMessage = "render_error"
            looperState = LooperState.IDLE
        }
    }

    private fun saveToDownloads(path: String) {
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, File(path).name)
                put(android.provider.MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(android.provider.MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return
            resolver.openOutputStream(uri)?.use { out -> File(path).inputStream().copyTo(out) }
            values.clear()
            values.put(android.provider.MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            errorMessage = "save_error:${e.message}"
        }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────
    private fun nextId() = (tracks.maxOfOrNull { it.id } ?: 0) + 1

    private fun updateTrack(id: Int, update: (LooperTrack) -> LooperTrack) {
        val i = tracks.indexOfFirst { it.id == id }
        if (i != -1) tracks[i] = update(tracks[i])
    }

    override fun onCleared() {
        super.onCleared()
        processingJobs.values.forEach { it.cancel() }
        players.values.forEach { it.stop() }
        players.clear()
        recorder.stopRecording()
        stopWatchingHeadphones()
    }
}
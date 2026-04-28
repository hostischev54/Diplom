package com.diplom.looper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diplom.tuner.ui.theme.AppColors

@Composable
fun LooperScreen() {
    val context   = LocalContext.current
    val viewModel = remember { LooperViewModel(context) }
    val Mono2     = Color(0xFF6C2E91)
    val buttonShape = RoundedCornerShape(8.dp)

    DisposableEffect(Unit) {
        viewModel.startWatchingHeadphones()
        onDispose { viewModel.stopWatchingHeadphones() }
    }

    val mp3Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importMp3(it) } }

    // Модалка «Сохранено»
    if (viewModel.showSavedDialog) {
        Dialog(onDismissRequest = { viewModel.showSavedDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight()
                    .background(Color(0xFF2A1040), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", fontSize = 40.sp, color = Color(0xFF9C27B0))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Сохранено в музыку",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Файл доступен в приложении «Музыка» вашего телефона",
                        fontSize = 13.sp, color = Color(0xFFCE93D8), textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.showSavedDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                        shape = buttonShape
                    ) { Text("Окей", color = Color.White) }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AppColors.BackgroundTop, AppColors.BackgroundBottom))
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Лупер", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(40.dp))

            if (!viewModel.headphonesConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF4A1B0C)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Подключите наушники — звук дорожек не должен попадать на микрофон",
                            fontSize = 13.sp, color = Color(0xFFF0997B)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1040)),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ", fontSize = 14.sp, color = Color(0xFF9C27B0))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Для импорта поддерживается только формат MP3. Приложение автоматически конвертирует его для работы.",
                        fontSize = 12.sp, color = Color(0xFFCE93D8), lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (viewModel.errorMessage.isNotEmpty()) {
                Text(viewModel.errorMessage, color = Color.Red, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
            }

            val isGlobalPlaying = viewModel.looperState == LooperState.PLAYING

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.tracks, key = { it.id }) { track ->
                    TrackCard(
                        track               = track,
                        isGlobalPlaying     = isGlobalPlaying,
                        onTrimPreview       = { s, e -> viewModel.saveTrimPreview(track.id, s, e) },
                        onMuteToggle        = { viewModel.setMute(track.id, !track.isMuted) },
                        onVolumeChange      = { viewModel.setVolume(track.id, it) },
                        onDelete            = { viewModel.deleteTrack(track.id) },
                        onPause             = { viewModel.pauseTrack(track.id) },
                        onResume            = { viewModel.resumeTrack(track.id) },
                        onHardTrim          = { viewModel.applyHardTrim(track.id) },
                        onToggleSoloPlay    = { viewModel.toggleSoloPlay(track.id) },
                        onSeek = { ms -> viewModel.seekTrack(track.id, ms) },
                        onTrimCommit = { s, e -> viewModel.applyTrimPreview(track.id, s, e) },
                        getPlaybackPosition = { viewModel.getPlaybackPosition(track.id) }

                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (viewModel.looperState) {
                LooperState.RECORDING -> {
                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFA32D2D)),
                        shape    = buttonShape
                    ) {
                        Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Остановить запись", color = Color.White)
                    }
                }
                LooperState.PROCESSING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.Accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Рендер...", color = AppColors.TextSecondary)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick  = { viewModel.addTrack() },
                            enabled  = viewModel.tracks.size < 5 && viewModel.looperState == LooperState.IDLE,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = Mono2),
                            shape    = buttonShape
                        ) { Text("+ Запись", color = Color.White, fontSize = 13.sp) }

                        Button(
                            onClick  = { mp3Launcher.launch("audio/mpeg") },
                            enabled  = viewModel.tracks.size < 5 && viewModel.looperState == LooperState.IDLE,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3489)),
                            shape    = buttonShape
                        ) { Text("+ MP3", color = Color.White, fontSize = 13.sp) }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isGlobalPlaying) {
                            Button(
                                onClick  = { viewModel.stopAll() },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFA32D2D)),
                                shape    = buttonShape
                            ) { Text("⏹ Стоп", color = Color.White, fontSize = 13.sp) }
                        } else {
                            Button(
                                onClick  = { viewModel.playAll() },
                                enabled  = viewModel.tracks.any {
                                    it.processedPath.isNotEmpty() && !it.isMuted && !it.isProcessing
                                },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11)),
                                shape    = buttonShape
                            ) { Text("▶ Играть всё", color = Color.White, fontSize = 13.sp) }
                        }

                        Button(
                            onClick  = { viewModel.renderAll() },
                            enabled  = viewModel.tracks.any { it.processedPath.isNotEmpty() && !it.isProcessing },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                            shape    = buttonShape
                        ) { Text("Сохранить", color = Color.White, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackCard(
    track: LooperTrack,
    isGlobalPlaying: Boolean,
    onTrimPreview: (Long, Long) -> Unit,
    onMuteToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onHardTrim: () -> Unit,
    onToggleSoloPlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onTrimCommit: (Long, Long) -> Unit,
    getPlaybackPosition: () -> Long
) {
    val Mono2 = Color(0xFF6C2E91)

    // Трек «активно играет» если:
    //   — глобальный плеер работает И трек не замьючен И не на паузе
    //   — ИЛИ трек запущен в одиночном режиме И не на паузе
    val isTrackPlaying = (!track.isMuted && !track.isPaused) &&
            (isGlobalPlaying || track.isPlayingAlone)

    // Показываем кнопку паузы/возобновления когда трек в данный момент активен
    val showPauseResume = (isGlobalPlaying || track.isPlayingAlone) &&
            !track.isMuted && !track.isProcessing && track.processedPath.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Mono2.copy(alpha = 0.25f)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Заголовок ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (track.isImported) "Дорожка ${track.id} (MP3)"
                        else "Дорожка ${track.id}",
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                    if (track.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp, color = Color(0xFFCE93D8)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Пауза / возобновление
                    if (showPauseResume) {
                        TextButton(
                            onClick = if (track.isPaused) onResume else onPause,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (track.isPaused) "▶" else "⏸",
                                color = Color(0xFFCE93D8), fontSize = 16.sp
                            )
                        }
                    }

                    // Одиночный Play/Stop — доступен когда трек готов и не идёт глобальный плеер
                    if (!isGlobalPlaying && track.processedPath.isNotEmpty() && !track.isProcessing) {
                        TextButton(
                            onClick = onToggleSoloPlay,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (track.isPlayingAlone) "⏹" else "▶",
                                color = if (track.isPlayingAlone) Color(0xFFF09595)
                                else Color(0xFF8BC34A),
                                fontSize = 16.sp
                            )
                        }
                    }

                    TextButton(onClick = onMuteToggle) {
                        Text(
                            text  = if (track.isMuted) "Вкл" else "Откл",
                            color = if (track.isMuted) Color.Gray else Color(0xFFCE93D8),
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onDelete) {
                        Text("✕", color = Color(0xFFF09595), fontSize = 14.sp)
                    }
                }
            }

            // ── Громкость ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Громкость", fontSize = 12.sp, color = Color(0xFFCE93D8))
                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9C27B0),
                        activeTrackColor = Color(0xFF9C27B0)
                    )
                )
            }

            // ── Waveform ──────────────────────────────────────────────────────
            AnimatedVisibility(visible = track.rawPath.isNotEmpty() && !track.isProcessing) {
                Column {
                    Text("Обрезка", fontSize = 12.sp, color = Color(0xFFCE93D8))
                    Spacer(Modifier.height(4.dp))
                    WaveformEditor(
                        track               = track,
                        onTrimPreview       = onTrimPreview,
                        onTrimCommit        = onTrimCommit,   // <- этот параметр теперь второй
                        onHardTrim          = onHardTrim,
                        getPlaybackPosition = getPlaybackPosition,
                        isTrackPlaying      = isTrackPlaying,
                        onSeek              = onSeek,
                        modifier            = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = track.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Mono2.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Обработка дорожки...", fontSize = 12.sp, color = Color(0xFFCE93D8))
                }
            }
        }
    }
}

package com.diplom.looper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diplom.R
import com.diplom.tuner.ui.theme.AppColors

@Composable
fun LooperScreen() {
    val context     = LocalContext.current
    val viewModel   = remember { LooperViewModel(context) }
    val Mono2       = Color(0xFF6C2E91)
    val buttonShape = RoundedCornerShape(8.dp)
    var showOnboarding       by remember { mutableStateOf(true) }
    var hasScrolledToBottom  by remember { mutableStateOf(false) }

    // Рядки для resolveError
    val strTrimEmpty   = stringResource(R.string.looper_trim_empty)
    val strTrimError   = stringResource(R.string.looper_trim_error)
    val strRenderError = stringResource(R.string.looper_render_error)
    val strSaveError   = stringResource(R.string.looper_save_error)

    fun resolveError(key: String): String = when {
        key == "trim_empty"           -> strTrimEmpty
        key.startsWith("trim_error:") -> strTrimError.format(key.removePrefix("trim_error:"))
        key == "render_error"         -> strRenderError
        key.startsWith("save_error:") -> strSaveError.format(key.removePrefix("save_error:"))
        else                          -> key
    }

    DisposableEffect(Unit) {
        viewModel.startWatchingHeadphones()
        onDispose { viewModel.stopWatchingHeadphones() }
    }

    val mp3Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importMp3(it) } }

    // Діалог «Збережено»
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
                        stringResource(R.string.looper_saved_title),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.looper_saved_subtitle),
                        fontSize = 13.sp, color = Color(0xFFCE93D8), textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.showSavedDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                        shape = buttonShape
                    ) { Text(stringResource(R.string.looper_ok), color = Color.White) }
                }
            }
        }
    }

    // Онбординг
    if (showOnboarding) {
        val ob1Title = stringResource(R.string.looper_onboarding_1_title)
        val ob1Text  = stringResource(R.string.looper_onboarding_1_text)
        val ob2Title = stringResource(R.string.looper_onboarding_2_title)
        val ob2Text  = stringResource(R.string.looper_onboarding_2_text)
        val ob3Title = stringResource(R.string.looper_onboarding_3_title)
        val ob3Text  = stringResource(R.string.looper_onboarding_3_text)
        val ob4Title = stringResource(R.string.looper_onboarding_4_title)
        val ob4Text  = stringResource(R.string.looper_onboarding_4_text)
        val ob5Title = stringResource(R.string.looper_onboarding_5_title)
        val ob5Text  = stringResource(R.string.looper_onboarding_5_text)

        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .background(Color(0xFF2A1040), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.looper_onboarding_title),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    val scrollState = rememberScrollState()
                    LaunchedEffect(scrollState.value, scrollState.maxValue) {
                        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 10)
                            hasScrolledToBottom = true
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(Color(0xFF1A1040), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
                        ) {
                            OnboardingSection(icon = "🔇", title = ob1Title, text = ob1Text)
                            Spacer(Modifier.height(16.dp))
                            OnboardingSection(icon = "🎧", title = ob2Title, text = ob2Text)
                            Spacer(Modifier.height(16.dp))
                            OnboardingSection(icon = "⏱️", title = ob3Title, text = ob3Text)
                            Spacer(Modifier.height(16.dp))
                            OnboardingSection(icon = "✂️", title = ob4Title, text = ob4Text)
                            Spacer(Modifier.height(16.dp))
                            OnboardingSection(icon = "🔊", title = ob5Title, text = ob5Text)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.looper_onboarding_scroll_done),
                                fontSize = 10.sp,
                                color = Color(0xFF6C2E91),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (!hasScrolledToBottom) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.looper_onboarding_scroll_hint),
                            fontSize = 11.sp, color = Color(0xFFCE93D8), textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showOnboarding = false },
                        enabled = hasScrolledToBottom,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Mono2,
                            disabledContainerColor = Mono2.copy(alpha = 0.3f)
                        ),
                        shape = buttonShape
                    ) {
                        Text(
                            if (hasScrolledToBottom) stringResource(R.string.looper_onboarding_confirm_ready)
                            else stringResource(R.string.looper_onboarding_confirm_read),
                            color = if (hasScrolledToBottom) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppColors.BackgroundTop, AppColors.BackgroundBottom)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.looper_title),
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(40.dp))

            if (!viewModel.headphonesConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1B0C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.looper_headphones_warning),
                            fontSize = 13.sp, color = Color(0xFFF0997B))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1040)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ", fontSize = 14.sp, color = Color(0xFF9C27B0))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.looper_mp3_info),
                        fontSize = 12.sp, color = Color(0xFFCE93D8), lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (viewModel.errorMessage.isNotEmpty()) {
                Text(resolveError(viewModel.errorMessage), color = Color.Red, fontSize = 13.sp)
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
                        onSeek              = { ms -> viewModel.seekTrack(track.id, ms) },
                        onTrimCommit        = { s, e -> viewModel.applyTrimPreview(track.id, s, e) },
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA32D2D)),
                        shape = buttonShape
                    ) {
                        Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.looper_stop_recording), color = Color.White)
                    }
                }
                LooperState.PROCESSING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.Accent)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.looper_processing), color = AppColors.TextSecondary)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addTrack() },
                            enabled = viewModel.tracks.size < 5 && viewModel.looperState == LooperState.IDLE,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                            shape = buttonShape
                        ) { Text(stringResource(R.string.looper_record), color = Color.White, fontSize = 13.sp) }

                        Button(
                            onClick = { mp3Launcher.launch("audio/mpeg") },
                            enabled = viewModel.tracks.size < 5 && viewModel.looperState == LooperState.IDLE,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3489)),
                            shape = buttonShape
                        ) { Text(stringResource(R.string.looper_import_mp3), color = Color.White, fontSize = 13.sp) }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isGlobalPlaying) {
                            Button(
                                onClick = { viewModel.stopAll() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA32D2D)),
                                shape = buttonShape
                            ) { Text(stringResource(R.string.looper_stop_all), color = Color.White, fontSize = 13.sp) }
                        } else {
                            Button(
                                onClick = { viewModel.playAll() },
                                enabled = viewModel.tracks.any {
                                    it.processedPath.isNotEmpty() && !it.isMuted && !it.isProcessing
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11)),
                                shape = buttonShape
                            ) { Text(stringResource(R.string.looper_play_all), color = Color.White, fontSize = 13.sp) }
                        }

                        Button(
                            onClick = { viewModel.renderAll() },
                            enabled = viewModel.tracks.any { it.processedPath.isNotEmpty() && !it.isProcessing },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                            shape = buttonShape
                        ) { Text(stringResource(R.string.looper_save), color = Color.White, fontSize = 13.sp) }
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
    val isTrackPlaying = (!track.isMuted && !track.isPaused) && (isGlobalPlaying || track.isPlayingAlone)
    val showPauseResume = (isGlobalPlaying || track.isPlayingAlone) &&
            !track.isMuted && !track.isProcessing && track.processedPath.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Mono2.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        text = if (track.isImported)
                            stringResource(R.string.looper_track_imported, track.id)
                        else
                            stringResource(R.string.looper_track_title, track.id),
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

                    if (!isGlobalPlaying && track.processedPath.isNotEmpty() && !track.isProcessing) {
                        TextButton(
                            onClick = onToggleSoloPlay,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (track.isPlayingAlone) "⏹" else "▶",
                                color = if (track.isPlayingAlone) Color(0xFFF09595) else Color(0xFF8BC34A),
                                fontSize = 16.sp
                            )
                        }
                    }

                    TextButton(onClick = onMuteToggle) {
                        Text(
                            text = if (track.isMuted) stringResource(R.string.looper_mute_on)
                            else stringResource(R.string.looper_mute_off),
                            color = if (track.isMuted) Color.Gray else Color(0xFFCE93D8),
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onDelete) {
                        Text("✕", color = Color(0xFFF09595), fontSize = 14.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.looper_volume), fontSize = 12.sp, color = Color(0xFFCE93D8))
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

            AnimatedVisibility(visible = track.rawPath.isNotEmpty() && !track.isProcessing) {
                Column {
                    Text(stringResource(R.string.looper_trim), fontSize = 12.sp, color = Color(0xFFCE93D8))
                    Spacer(Modifier.height(4.dp))
                    WaveformEditor(
                        track = track,
                        onTrimPreview = onTrimPreview,
                        onTrimCommit = onTrimCommit,
                        onHardTrim = onHardTrim,
                        getPlaybackPosition = getPlaybackPosition,
                        isTrackPlaying = isTrackPlaying,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxWidth()
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
                    Text(stringResource(R.string.looper_processing_track),
                        fontSize = 12.sp, color = Color(0xFFCE93D8))
                }
            }
        }
    }
}

@Composable
private fun OnboardingSection(icon: String, title: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 12.sp, color = Color(0xFFCE93D8), lineHeight = 17.sp)
        }
    }
}
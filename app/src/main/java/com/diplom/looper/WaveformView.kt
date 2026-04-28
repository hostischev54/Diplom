package com.diplom.looper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    val tenths   = (ms % 1000) / 100
    return "%d:%02d.%d".format(min, sec, tenths)
}

private fun parseMs(input: String): Long? {
    val clean = input.trim()
    if (clean.isEmpty()) return null
    val colonRegex = Regex("""^(\d+):(\d{1,2})(?:\.(\d))?$""")
    colonRegex.matchEntire(clean)?.let { m ->
        val min   = m.groupValues[1].toLongOrNull() ?: return null
        val sec   = m.groupValues[2].toLongOrNull() ?: return null
        val tenth = m.groupValues[3].toLongOrNull() ?: 0L
        if (sec >= 60) return null
        return min * 60_000L + sec * 1_000L + tenth * 100L
    }
    val dotRegex = Regex("""^(\d+)(?:\.(\d))?$""")
    dotRegex.matchEntire(clean)?.let { m ->
        val sec   = m.groupValues[1].toLongOrNull() ?: return null
        val tenth = m.groupValues[2].toLongOrNull() ?: 0L
        return sec * 1_000L + tenth * 100L
    }
    return null
}

private fun DrawScope.drawScissorsIcon(
    centerX: Float, centerY: Float, size: Float, color: Color
) {
    val s = size / 2f
    drawPath(Path().apply {
        moveTo(centerX - s, centerY - s * 0.3f)
        lineTo(centerX + s * 0.5f, centerY)
        lineTo(centerX - s, centerY + s * 0.3f)
        close()
    }, color)
    drawPath(Path().apply {
        moveTo(centerX - s, centerY + s * 0.7f)
        lineTo(centerX + s * 0.5f, centerY)
        lineTo(centerX - s, centerY + s * 1.3f)
        close()
    }, color)
    drawCircle(color, s * 0.28f, Offset(centerX - s * 0.85f, centerY))
    drawCircle(color, s * 0.28f, Offset(centerX - s * 0.85f, centerY + s))
}

// TimeField — синхронизируется с внешним значением ТОЛЬКО при resetKey,
// не реагирует на промежуточные изменения externalMs во время processLoop
@Composable
private fun TimeField(
    externalMs: Long,
    resetKey: Any,
    maxMs: Long,
    onCommit: (Long) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Инициализируем текст только при сбросе resetKey
    var text          by remember(resetKey) { mutableStateOf(formatMs(externalMs)) }
    var committed     by remember(resetKey) { mutableStateOf(externalMs) }
    var focused       by remember { mutableStateOf(false) }
    var commitPending by remember { mutableStateOf(false) }
    val focusManager  = LocalFocusManager.current

    // Синхронизация с внешним значением — только когда не в фокусе
    // И только если значение реально изменилось относительно committed
    // (защита от сброса после applyTrimPreview)
    LaunchedEffect(externalMs) {
        if (!focused && externalMs != committed) {
            committed = externalMs
            text      = formatMs(externalMs)
        }
    }

    fun commit() {
        if (commitPending) { commitPending = false; return }
        commitPending = true
        val parsed = parseMs(text)
        when {
            parsed == null -> {
                onError("Неверный формат. Примеры: 1:23.4  или  5 (сек)")
                text = formatMs(committed)
            }
            parsed > maxMs -> {
                onError("${formatMs(parsed)} > длины трека (${formatMs(maxMs)})")
                text = formatMs(committed)
            }
            else -> {
                committed = parsed
                text      = formatMs(parsed)
                onError("")
                onCommit(parsed)
            }
        }
        focusManager.clearFocus()
        commitPending = false
    }

    BasicTextField(
        value           = text,
        onValueChange   = { text = it },
        singleLine      = true,
        textStyle       = TextStyle(
            fontSize  = 10.sp,
            color     = if (focused) Color.White else Color(0xFFCE93D8),
            textAlign = TextAlign.Center
        ),
        cursorBrush     = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction    = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier
            .background(
                color = if (focused) Color(0x336C2E91) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (focused) 1.dp else 0.dp,
                color = if (focused) Color(0xFF9C27B0) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .onFocusChanged { state ->
                val wasFocused = focused
                focused = state.isFocused
                if (wasFocused && !state.isFocused && !commitPending) {
                    commit()
                }
            }
    )
}

@Composable
fun WaveformEditor(
    track: LooperTrack,
    onTrimPreview: (startMs: Long, endMs: Long) -> Unit,
    onTrimCommit: (startMs: Long, endMs: Long) -> Unit = { _, _ -> },
    onHardTrim: () -> Unit,
    getPlaybackPosition: () -> Long,
    isTrackPlaying: Boolean,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // resetKey меняется только при жёсткой обрезке или смене файла
    val resetKey = "${track.rawPath}_${track.waveformVersion}"

    val waveform = remember(resetKey) { mutableStateOf(floatArrayOf()) }
    LaunchedEffect(resetKey) {
        waveform.value = withContext(Dispatchers.IO) {
            loadWaveform(track.rawPath, samples = 300)
        }
    }

    val rawDurationMs by produceState(initialValue = 0L, resetKey) {
        value = withContext(Dispatchers.IO) { getWavDurationMs(track.rawPath) }
    }

    val processedDurationMs by produceState(
        initialValue = 0L,
        track.processedPath,
        track.trimStartMs,
        track.trimEndMs,
        resetKey
    ) {
        value = withContext(Dispatchers.IO) {
            if (track.processedPath.isNotEmpty()) getWavDurationMs(track.processedPath) else 0L
        }
    }
    // Фракции — локальное состояние, инициализируется один раз из track
    // После этого обновляется только пользователем (drag/keyboard), не из track
    var startFraction   by remember(resetKey) { mutableStateOf(0f) }
    var endFraction     by remember(resetKey) { mutableStateOf(1f) }
    var fractionsInited by remember(resetKey) { mutableStateOf(false) }

    LaunchedEffect(rawDurationMs) {
        if (rawDurationMs > 0L && !fractionsInited) {
            startFraction   = track.trimStartMs.toFloat() / rawDurationMs
            endFraction     = track.trimEndMs.toFloat()   / rawDurationMs
            fractionsInited = true
        }
    }

    var dragTarget by remember { mutableStateOf(0) }
    var timeError  by remember(resetKey) { mutableStateOf("") }

    // startMs/endMs — вычисляем локально из фракций, не берём из track
    val startMs = (startFraction * rawDurationMs).toLong()
    val endMs   = (endFraction   * rawDurationMs).toLong()

    var playheadFraction   by remember(resetKey) { mutableStateOf(0f) }
    var isDraggingPlayhead by remember { mutableStateOf(false) }

    LaunchedEffect(fractionsInited) {
        if (fractionsInited) playheadFraction = startFraction
    }

    // В LaunchedEffect(isTrackPlaying):
    LaunchedEffect(isTrackPlaying) {
        if (isTrackPlaying) {
            while (true) {
                if (!isDraggingPlayhead && processedDurationMs > 0L) {
                    val posMs      = getPlaybackPosition()
                    val safeDur    = processedDurationMs.coerceAtLeast(1L)
                    val posClamped = posMs % safeDur
                    val newFraction = startFraction +
                            posClamped.toFloat() / safeDur * (endFraction - startFraction)
                    android.util.Log.v("LOOPER_PH", "playhead: posMs=$posMs processedDur=$processedDurationMs safeDur=$safeDur posClamped=$posClamped startFr=$startFraction endFr=$endFraction -> fraction=$newFraction")
                    playheadFraction = newFraction
                }
                delay(80)
            }
        }
    }

    LaunchedEffect(track.isPaused, track.playbackPositionMs) {
        if (track.isPaused && processedDurationMs > 0L && !isDraggingPlayhead) {
            val safeDur = processedDurationMs.coerceAtLeast(1L)
            playheadFraction = startFraction +
                    (track.playbackPositionMs % safeDur).toFloat() / safeDur *
                    (endFraction - startFraction)
        }
    }

    val playheadMs   = (playheadFraction * rawDurationMs).toLong()
    val showPlayhead = isTrackPlaying || track.isPaused || isDraggingPlayhead

    val selectedColor = Color(0xFFCE93D8)
    val handleColor   = Color.White
    val dimColor      = Color(0x559C27B0)
    val playheadColor = Color(0xFFFFEB3B)
    val scissorsColor = Color(0xFFF09595)

    Column(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .pointerInput(resetKey) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (!fractionsInited) return@detectHorizontalDragGestures
                            val x            = offset.x / size.width.toFloat()
                            val distStart    = kotlin.math.abs(x - startFraction)
                            val distEnd      = kotlin.math.abs(x - endFraction)
                            val distPlayhead = if (showPlayhead)
                                kotlin.math.abs(x - playheadFraction) else Float.MAX_VALUE
                            val threshold = 0.07f
                            dragTarget = when {
                                distPlayhead < threshold &&
                                        distPlayhead <= distStart &&
                                        distPlayhead <= distEnd -> 3
                                distStart < threshold &&
                                        distStart <= distEnd    -> 1
                                distEnd < threshold             -> 2
                                else                            -> 0
                            }
                        },
                        onDragEnd = {
                            when (dragTarget) {
                                1, 2 -> {
                                    timeError = ""
                                    val s = (startFraction * rawDurationMs).toLong()
                                    val e = (endFraction   * rawDurationMs).toLong()
                                    android.util.Log.d("LOOPER", "onDragEnd trim: start=$s end=$e rawDur=$rawDurationMs startFr=$startFraction endFr=$endFraction")
                                    onTrimPreview(s, e)
                                    onTrimCommit(s, e)
                                }
                                3 -> {
                                    isDraggingPlayhead = false
                                    if (processedDurationMs > 0L) {
                                        val relFraction = ((playheadFraction - startFraction) /
                                                (endFraction - startFraction).coerceAtLeast(0.001f))
                                            .coerceIn(0f, 1f)
                                        onSeek((relFraction * processedDurationMs).toLong())
                                    }
                                }
                            }
                            dragTarget = 0
                        }
                    ) { _, dragAmount ->
                        val delta = dragAmount / size.width.toFloat()
                        when (dragTarget) {
                            1 -> {
                                startFraction = (startFraction + delta)
                                    .coerceIn(0f, endFraction - 0.02f)
                                if (playheadFraction < startFraction)
                                    playheadFraction = startFraction
                            }
                            2 -> {
                                endFraction = (endFraction + delta)
                                    .coerceIn(startFraction + 0.02f, 1f)
                                if (playheadFraction > endFraction)
                                    playheadFraction = endFraction
                            }
                            3 -> {
                                isDraggingPlayhead = true
                                playheadFraction = (playheadFraction + delta)
                                    .coerceIn(startFraction, endFraction)
                            }
                        }
                    }
                }
        ) {
            val data = waveform.value
            if (data.isEmpty()) return@Canvas

            val centerY  = size.height / 2f
            val barWidth = size.width / data.size
            val startX   = startFraction * size.width
            val endX     = endFraction   * size.width

            data.forEachIndexed { i, amplitude ->
                val x    = i * barWidth + barWidth / 2f
                val barH = amplitude * size.height * 0.85f
                drawLine(
                    color       = if (x < startX || x > endX) dimColor else selectedColor,
                    start       = Offset(x, centerY - barH / 2f),
                    end         = Offset(x, centerY + barH / 2f),
                    strokeWidth = barWidth * 0.7f
                )
            }

            drawLine(handleColor, Offset(startX, 0f), Offset(startX, size.height), 3f)
            drawLine(handleColor, Offset(endX,   0f), Offset(endX,   size.height), 3f)
            drawCircle(handleColor, 9f, Offset(startX, centerY))
            drawCircle(handleColor, 9f, Offset(endX,   centerY))

            if (showPlayhead) {
                val phX = playheadFraction.coerceIn(startFraction, endFraction) * size.width
                drawLine(playheadColor, Offset(phX, 0f), Offset(phX, size.height), 2.5f)
                val d = 6f
                drawPath(Path().apply {
                    moveTo(phX, 0f)
                    lineTo(phX + d, d)
                    lineTo(phX, d * 2)
                    lineTo(phX - d, d)
                    close()
                }, playheadColor)
            }
        }

        Row(
            modifier          = Modifier.fillMaxWidth().padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeField(
                externalMs = startMs,
                resetKey   = resetKey,
                maxMs      = (endMs - 100L).coerceAtLeast(0L),
                onCommit   = { newMs ->
                    if (rawDurationMs > 0L) {
                        startFraction = newMs.toFloat() / rawDurationMs
                        if (playheadFraction < startFraction) playheadFraction = startFraction
                    }
                    val currentEndMs = (endFraction * rawDurationMs).toLong()
                    onTrimPreview(newMs, currentEndMs)
                    onTrimCommit(newMs, currentEndMs)
                },
                onError  = { timeError = it },
                modifier = Modifier.width(56.dp)
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (showPlayhead) {
                    Text(
                        text      = "▶ ${formatMs(playheadMs)}",
                        fontSize  = 10.sp,
                        color     = playheadColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawScissorsIcon(
                        centerX = size.width / 2f,
                        centerY = size.height / 2f - 4f,
                        size    = size.width * 0.75f,
                        color   = scissorsColor
                    )
                }
                TextButton(
                    onClick        = onHardTrim,
                    modifier       = Modifier.size(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors         = ButtonDefaults.textButtonColors(contentColor = Color.Transparent)
                ) {}
            }

            TimeField(
                externalMs = endMs,
                resetKey   = resetKey,
                maxMs      = rawDurationMs,
                onCommit   = { newMs ->
                    if (rawDurationMs > 0L) {
                        endFraction = newMs.toFloat() / rawDurationMs
                        if (playheadFraction > endFraction) playheadFraction = endFraction
                    }
                    val currentStartMs = (startFraction * rawDurationMs).toLong()
                    onTrimPreview(currentStartMs, newMs)
                    onTrimCommit(currentStartMs, newMs)
                },
                onError  = { timeError = it },
                modifier = Modifier.width(56.dp)
            )
        }

        if (timeError.isNotEmpty()) {
            Text(
                text       = timeError,
                fontSize   = 10.sp,
                color      = Color(0xFFF09595),
                lineHeight = 13.sp,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 0.dp)
            )
        }
    }
}

fun loadWaveform(path: String, samples: Int): FloatArray {
    return try {
        val bytes    = File(path).readBytes()
        val pcmBytes = bytes.size - 44
        if (pcmBytes <= 0) return floatArrayOf()
        val spb    = ((pcmBytes / 2) / samples).coerceAtLeast(1)
        val result = FloatArray(samples)
        val buf    = ByteBuffer.wrap(bytes, 44, pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samples) {
            var maxAmp = 0f
            repeat(spb) {
                if (buf.remaining() >= 2) {
                    val s = buf.short.toFloat() / Short.MAX_VALUE
                    if (kotlin.math.abs(s) > maxAmp) maxAmp = kotlin.math.abs(s)
                }
            }
            result[i] = maxAmp
        }
        result
    } catch (e: Exception) { floatArrayOf() }
}

fun getWavDurationMs(path: String): Long {
    return try {
        val pcmBytes = (File(path).length() - 44).coerceAtLeast(0)
        (pcmBytes * 1000L) / (AudioRecorder.SAMPLE_RATE * 2)
    } catch (e: Exception) { 0L }
}
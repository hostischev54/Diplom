package com.diplom.autotab

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.diplom.ui.components.TabEditor
import com.diplom.ui.components.lighten

val ALLOWED_MIME_TYPES = setOf(
    "audio/mpeg", "audio/mp4", "audio/x-m4a",
    "audio/wav", "audio/x-wav", "audio/wave"
)

@Composable
fun AutoTabScreen() {
    val context = LocalContext.current
    val viewModel = remember { AutoTabViewModel(context) }
    val state by viewModel.state.collectAsState()

    val Mono2 = Color(0xFF6C2E91)
    val Mono3 = Color(0xFF5F158A)
    val ButtonTextColor = Color.White
    val buttonShape = RoundedCornerShape(8.dp)

    // Рядки
    val strFileNotSelected  = stringResource(R.string.autotab_file_not_selected)
    val strAudioFile        = stringResource(R.string.autotab_audio_file)
    val strUnsupported      = stringResource(R.string.autotab_unsupported_format)
    val strSavedTo          = stringResource(R.string.autotab_saved_to)
    val strSaveError        = stringResource(R.string.autotab_save_error)
    val strCannotCreate     = stringResource(R.string.autotab_cannot_create_file)
    val strServerError      = stringResource(R.string.autotab_server_error)
    val strErrorUnknown     = stringResource(R.string.autotab_error_unknown)
    val strLoadingUpload    = stringResource(R.string.autotab_loading_upload)
    val strLoadingGuitar    = stringResource(R.string.autotab_loading_guitar)
    val strLoadingTab       = stringResource(R.string.autotab_loading_tab)
    val strCannotOpen       = stringResource(R.string.autotab_cannot_open_file)

    fun resolveMessage(key: String): String = when (key) {
        "loading_upload"     -> strLoadingUpload
        "loading_guitar"     -> strLoadingGuitar
        "loading_tab"        -> strLoadingTab
        "error_unknown"      -> strErrorUnknown
        "cannot_open_file"   -> strCannotOpen
        "cannot_create_file" -> strCannotCreate
        "server_error"       -> strServerError
        else                 -> key
    }

    var selectedFileUri  by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf(strFileNotSelected) }
    var fileError        by remember { mutableStateOf("") }
    var showTabSheet     by remember { mutableStateOf(false) }
    var tabResult        by remember { mutableStateOf<TabApiResult?>(null) }
    var playbackUri      by remember { mutableStateOf<android.net.Uri?>(null) }
    val mediaPlayer      = remember { android.media.MediaPlayer() }
    var isPlaying        by remember { mutableStateOf(false) }
    var currentTimeMs    by remember { mutableStateOf(0L) }
    var pendingFilePick  by remember { mutableStateOf(false) }
    var saveMessage      by remember { mutableStateOf("") }
    var showDisclaimer   by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

    fun saveTabToFile(tab: Map<Int, List<String>>, fileName: String) {
        try {
            val stringNames = mapOf(1 to "e", 2 to "B", 3 to "G", 4 to "D", 5 to "A", 6 to "E")
            val maxLineLength = 80
            val rows: Map<Int, List<String>> = (1..6).associateWith { s -> tab[s] ?: emptyList() }
            val totalColumns = rows[1]?.size ?: 0
            val colWidths = (0 until totalColumns).map { col ->
                (1..6).maxOf { s ->
                    val cell = rows[s]?.getOrElse(col) { "-" } ?: "-"
                    if (cell == "-") 2 else maxOf(cell.length, 2)
                }
            }
            val blocks = mutableListOf<IntRange>()
            var blockStart = 0
            while (blockStart < totalColumns) {
                var lineLen = 0
                var blockEnd = blockStart
                while (blockEnd < totalColumns && lineLen + colWidths[blockEnd] <= maxLineLength) {
                    lineLen += colWidths[blockEnd]; blockEnd++
                }
                if (blockEnd == blockStart) blockEnd = blockStart + 1
                blocks.add(blockStart until blockEnd)
                blockStart = blockEnd
            }
            val content = buildString {
                for (block in blocks) {
                    for (s in 6 downTo 1) {
                        val name = stringNames[s] ?: "?"
                        val cells = rows[s] ?: emptyList()
                        append(name).append("|")
                        for (i in block) {
                            val cell = cells.getOrElse(i) { "-" }
                            val w = colWidths[i]
                            append(if (cell == "-") "-".repeat(w) else cell.padEnd(w, '-'))
                        }
                        appendLine("|")
                    }
                    appendLine()
                }
            }
            val cleanName = fileName.substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40).ifEmpty { "tab" }
            val outFileName = "${cleanName}_tab.txt"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, outFileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: throw Exception("cannot_create_file")
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                java.io.File(downloadsDir, outFileName).writeText(content, Charsets.UTF_8)
            }
            saveMessage = strSavedTo.format(outFileName)
        } catch (e: Exception) {
            saveMessage = strSaveError.format(resolveMessage(e.message ?: "error_unknown"))
        }
    }

    fun resetToFileSelection() {
        mediaPlayer.reset()
        isPlaying = false
        currentTimeMs = 0L
        selectedFileUri = null
        selectedFileName = strFileNotSelected
        tabResult = null
        fileError = ""
        saveMessage = ""
        viewModel.resetWithoutPing()
        showTabSheet = false
        pendingFilePick = true
    }

    LaunchedEffect(Unit) { viewModel.checkConnection() }

    LaunchedEffect(state) {
        if (state is AutoTabState.Success) {
            tabResult = (state as AutoTabState.Success).result
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        if (mimeType !in ALLOWED_MIME_TYPES) {
            fileError = strUnsupported
            selectedFileUri = null
            selectedFileName = strFileNotSelected
        } else {
            fileError = ""
            selectedFileUri = uri
            playbackUri = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: strAudioFile
            viewModel.resetWithoutPing()
        }
    }

    LaunchedEffect(showTabSheet) {
        if (!showTabSheet && pendingFilePick) {
            pendingFilePick = false
            launcher.launch("audio/*")
        }
    }

    val isReady = state is AutoTabState.Ready || state is AutoTabState.Error
    val isLoading = state is AutoTabState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(AppColors.BackgroundTop, AppColors.BackgroundBottom)
            ))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.autotab_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ButtonTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                is AutoTabState.CheckingConnection -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.autotab_checking_connection),
                            fontSize = 14.sp, color = AppColors.TextSecondary)
                    }
                }
                is AutoTabState.NoConnection -> {
                    Text(
                        stringResource(R.string.autotab_no_connection),
                        color = Color.Red, textAlign = TextAlign.Center, fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.checkConnection() },
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                    ) { Text(stringResource(R.string.autotab_retry), color = ButtonTextColor) }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Mono2.copy(alpha = 0.25f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedFileName,
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (fileError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(fileError, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDisclaimer = true },
                        enabled = isReady,
                        modifier = Modifier.fillMaxWidth(),
                        shape = buttonShape,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                    ) { Text(stringResource(R.string.autotab_select_file), color = ButtonTextColor) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { selectedFileUri?.let { viewModel.analyze(it) } },
                        enabled = isReady && selectedFileUri != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = buttonShape,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                    ) { Text(stringResource(R.string.autotab_analyze), color = ButtonTextColor) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                val message = resolveMessage((state as AutoTabState.Loading).message)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppColors.Accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, fontSize = 15.sp, color = AppColors.TextSecondary)
                }
            }

            if (state is AutoTabState.Error) {
                Text(
                    resolveMessage((state as AutoTabState.Error).message),
                    color = Color.Red, textAlign = TextAlign.Center, fontSize = 14.sp
                )
            }

            if (state is AutoTabState.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showTabSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) { Text(stringResource(R.string.autotab_open_tab), color = ButtonTextColor) }
            }
        }

        // Діалог табулатури
        if (showTabSheet && tabResult != null) {
            Dialog(
                onDismissRequest = { showTabSheet = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .background(
                            Brush.verticalGradient(colors = listOf(
                                AppColors.BackgroundTop.lighten(1.15f), AppColors.BackgroundBottom
                            )),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.autotab_result_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ButtonTextColor
                            )
                            IconButton(onClick = { showTabSheet = false }) {
                                Text("✕", fontSize = 20.sp, color = AppColors.TextSecondary)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Mono2.copy(alpha = 0.5f)
                        )

                        DisposableEffect(Unit) {
                            mediaPlayer.setOnCompletionListener {
                                isPlaying = false; currentTimeMs = 0L
                            }
                            onDispose { }
                        }

                        LaunchedEffect(isPlaying) {
                            while (isPlaying) {
                                if (mediaPlayer.isPlaying) currentTimeMs = mediaPlayer.currentPosition.toLong()
                                kotlinx.coroutines.delay(16)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer.pause(); isPlaying = false
                                    } else {
                                        try {
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(context, playbackUri!!)
                                            mediaPlayer.prepare()
                                            if (currentTimeMs > 0L) mediaPlayer.seekTo(currentTimeMs.toInt())
                                            mediaPlayer.start(); isPlaying = true
                                        } catch (e: Exception) { }
                                    }
                                },
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono2),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    if (isPlaying) stringResource(R.string.autotab_pause)
                                    else stringResource(R.string.autotab_play),
                                    color = ButtonTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val notes = tabResult!!.notes
                        var measuredTabWidth by remember { mutableStateOf(0) }
                        var trackDurationMs by remember { mutableStateOf(1) }
                        val scrollState = rememberScrollState()

                        LaunchedEffect(isPlaying) {
                            if (isPlaying && trackDurationMs == 1) {
                                trackDurationMs = mediaPlayer.duration.coerceAtLeast(1)
                            }
                        }

                        val progress = (currentTimeMs.toFloat() / trackDurationMs).coerceIn(0f, 1f)
                        val lineX = progress * measuredTabWidth.toFloat()

                        LaunchedEffect(currentTimeMs) {
                            if (isPlaying && scrollState.maxValue > 0) {
                                scrollState.scrollTo((progress * scrollState.maxValue).toInt())
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .background(Mono2.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            TabEditor(
                                notes = notes.map { it.note },
                                serverTab = tabResult!!.strings,
                                serverStringNames = tabResult!!.stringNames,
                                scrollState = scrollState,
                                onTabWidthMeasured = { totalWidth, _ ->
                                    if (totalWidth > 0) measuredTabWidth = totalWidth
                                }
                            )
                            if (isPlaying || currentTimeMs > 0L) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val x = lineX - scrollState.value.toFloat()
                                    if (x in 0f..size.width) {
                                        drawLine(
                                            color = Color(0xFF9C27B0).copy(alpha = 0.55f),
                                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                                            end = androidx.compose.ui.geometry.Offset(x, size.height),
                                            strokeWidth = 6f
                                        )
                                    }
                                }
                            }
                        }

                        if (saveMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = saveMessage,
                                fontSize = 12.sp,
                                color = if (saveMessage.startsWith(strSaveError.substringBefore("%")))
                                    Color.Red else AppColors.Accent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { resetToFileSelection() },
                                modifier = Modifier.weight(1f),
                                shape = buttonShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary)
                            ) { Text(stringResource(R.string.autotab_other_file), fontSize = 13.sp) }

                            Button(
                                onClick = { saveMessage = ""; saveTabToFile(tabResult!!.strings, selectedFileName) },
                                modifier = Modifier.weight(1f),
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                            ) { Text(stringResource(R.string.autotab_download_txt), fontSize = 13.sp, color = ButtonTextColor) }
                        }
                    }
                }
            }
        }

        // Дисклеймер
        if (showDisclaimer) {
            Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                val scrollState = rememberScrollState()
                val isScrolledToBottom by remember {
                    derivedStateOf { scrollState.value >= scrollState.maxValue - 10 }
                }

                val disclaimerItems = listOf(
                    stringResource(R.string.autotab_disclaimer_1),
                    stringResource(R.string.autotab_disclaimer_2),
                    stringResource(R.string.autotab_disclaimer_3),
                    stringResource(R.string.autotab_disclaimer_4),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .background(AppColors.BackgroundTop, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(stringResource(R.string.autotab_disclaimer_title),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.autotab_disclaimer_subtitle),
                            fontSize = 13.sp, color = AppColors.TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Mono2.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.height(300.dp).verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            disclaimerItems.forEachIndexed { index, text ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Mono2.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", fontSize = 12.sp, color = AppColors.TextSecondary)
                                    }
                                    Text(text, fontSize = 14.sp, color = Color.White, lineHeight = 22.sp)
                                }
                                if (index < disclaimerItems.lastIndex)
                                    HorizontalDivider(color = Mono2.copy(alpha = 0.3f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Mono2.copy(alpha = 0.4f))

                        if (!isScrolledToBottom) {
                            Text(
                                stringResource(R.string.autotab_disclaimer_scroll_hint),
                                fontSize = 12.sp,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showDisclaimer = false; launcher.launch("audio/*") },
                            enabled = isScrolledToBottom,
                            modifier = Modifier.fillMaxWidth(),
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                        ) { Text(stringResource(R.string.autotab_disclaimer_agree), color = Color.White) }
                    }
                }
            }
        }
    }
}
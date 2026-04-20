package com.diplom.autotab

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diplom.ui.components.TabEditor

// разрешённые MIME типы
val ALLOWED_MIME_TYPES = setOf(
    "audio/mpeg",       // mp3
    "audio/mp4",        // m4a
    "audio/x-m4a",      // m4a альтернативный
    "audio/wav",        // wav
    "audio/x-wav",      // wav альтернативный
    "audio/wave"        // wav альтернативный
)

@Composable
fun AutoTabScreen() {
    val context = LocalContext.current
    val viewModel = remember { AutoTabViewModel(context) }
    val state by viewModel.state.collectAsState()

    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("Файл не выбран") }
    var fileError by remember { mutableStateOf("") }
    var showTabSheet by remember { mutableStateOf(false) }
    var tabResult by remember { mutableStateOf<TabApiResult?>(null) }

    // при входе на экран — проверяем соединение
    LaunchedEffect(Unit) {
        viewModel.checkConnection()
    }

    // сохраняем результат когда пришёл Success
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
            fileError = "Неподдерживаемый формат. Выберите MP3, M4A или WAV"
            selectedFileUri = null
            selectedFileName = "Файл не выбран"
        } else {
            fileError = ""
            selectedFileUri = uri
            // красивое имя файла
            selectedFileName = uri.lastPathSegment
                ?.substringAfterLast("/") ?: "аудиофайл"
            viewModel.reset()
        }
    }

    val isReady = state is AutoTabState.Ready || state is AutoTabState.Error
    val isLoading = state is AutoTabState.Loading

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // --- статус соединения ---
            when (state) {
                is AutoTabState.CheckingConnection -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Проверяем соединение…", fontSize = 14.sp, color = Color.Gray)
                    }
                }
                is AutoTabState.NoConnection -> {
                    Text(
                        "Нет соединения с сервером. Проверьте интернет.",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.checkConnection() }) {
                        Text("Повторить")
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- выбор файла ---
            Button(
                onClick = { launcher.launch("audio/*") },
                enabled = isReady
            ) {
                Text("Выбрать аудиофайл")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = selectedFileName,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            // ошибка формата файла
            if (fileError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(fileError, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- кнопка анализа ---
            Button(
                onClick = {
                    selectedFileUri?.let { viewModel.analyze(it) }
                },
                enabled = isReady && selectedFileUri != null
            ) {
                Text("Анализировать")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- прогресс ---
            if (isLoading) {
                val message = (state as AutoTabState.Loading).message
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(message, fontSize = 14.sp, color = Color.Gray)
                }
            }

            // --- ошибка анализа ---
            if (state is AutoTabState.Error) {
                Text(
                    (state as AutoTabState.Error).message,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }

            // --- кнопка открыть таб ---
            if (state is AutoTabState.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showTabSheet = true }) {
                    Text("Открыть табулатуру")
                }
            }
        }

        // --- модалка с табом ---
        if (showTabSheet && tabResult != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.75f)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    // конвертируем TabApiResult в список нот для TabEditor
                    val notesList = tabResult!!.notes.map { it.note }
                    TabEditor(
                        notes = notesList,
                        onApply = { showTabSheet = false }
                    )
                }
            }
        }
    }
}
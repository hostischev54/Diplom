package com.diplom.autotab

import com.diplom.ui.components.TabEditor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTabScreen() {

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    val autoTabVM = remember { AutoTabViewModel() }
    val decoder = remember { AudioDecoder() }
    var showTabSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var detectedNotes by remember { mutableStateOf(listOf<String>()) }

    // 🔥 ВАЖНО: ВСЁ ОБЕРНУТО В BOX
    Box(modifier = Modifier.fillMaxSize()) {

        // ---------------- ОСНОВНОЙ UI ----------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Button(onClick = {
                launcher.launch("audio/*")
            }) {
                Text("Выбрать аудиофайл")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = selectedFileUri?.toString() ?: "Файл не выбран"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {

                selectedFileUri?.let { uri ->

                    scope.launch(Dispatchers.IO) {

                        val notesList = mutableListOf<String>()

                        var lastStableNote: String? = null
                        var sameNoteCount = 0
                        val bufferAccumulator = mutableListOf<Short>()
                        val freqHistory = ArrayDeque<Double>()

                        decoder.decodeToPCM(context, uri) { samples, sampleRate ->

                            bufferAccumulator.addAll(samples.toList())

                            if (bufferAccumulator.size < 8192) return@decodeToPCM

                            val chunk = bufferAccumulator.take(8192).toShortArray()
                            bufferAccumulator.subList(0, 8192).clear()

                            val volume = chunk.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                            if (volume < 1500) return@decodeToPCM

                            val rawFreq = PitchDetector.detectPitch(chunk, sampleRate)
                            if (rawFreq <= 0) return@decodeToPCM

                            if (rawFreq < 30.0 || rawFreq > 2000.0) return@decodeToPCM

                            freqHistory.add(rawFreq)
                            if (freqHistory.size > 5) freqHistory.removeFirst()

                            val smoothFreq = freqHistory.average()

                            val note = autoTabVM.mapFrequencyToNote(smoothFreq)

                            if (note != null) {

                                if (note == lastStableNote) {
                                    sameNoteCount++
                                } else {
                                    sameNoteCount = 0
                                }

                                lastStableNote = note

                                if (sameNoteCount >= 1) {

                                    if (notesList.isEmpty() || notesList.last() != note) {
                                        notesList.add(note)
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            detectedNotes = notesList
                        }
                    }
                }

            }) {
                Text("Анализировать")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showTabSheet = true },
                enabled = detectedNotes.isNotEmpty()
            ) {
                Text("Открыть табулатуру")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = detectedNotes.joinToString(" "),
                softWrap = true
            )
        }

        // ---------------- МОДАЛКА ----------------
        if (showTabSheet) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.7f)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {

                    TabEditor(
                        notes = detectedNotes,
                        onApply = {
                            showTabSheet = false
                        }
                    )
                }
            }
        }
    }
}
package com.diplom.tuner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.diplom.tuner.models.Tunings
import com.diplom.tuner.models.NoteFrequencies
import com.diplom.tuner.models.Tuning
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp

fun formatTuningText(text: String): AnnotatedString {

    return buildAnnotatedString {

        text.forEach { char ->

            if (char == '#' || char == 'b') {

                withStyle(
                    SpanStyle(
                        fontSize = 13.sp
                    )
                ) {
                    append(char)
                }

            } else {

                withStyle(
                    SpanStyle(
                        fontSize = 13.sp
                    )
                ) {
                    append(char)
                }

            }

        }
    }
}

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    // ================= Colors =================
    val Background = Color(0xFF450769)
    val Mono2 = Color(0xFF6C2E91)
    val Mono3 = Color(0xFF5F158A)
    val AccentGreen = Color(0xFF4CAF50)
    val GrayText = Color(0xFFAAAAAA)
    val ButtonTextColor = Color.White
    val SpeedometerWarning = Color(0xFFFFA500)
    val SpeedometerCenter = AccentGreen

    // ================= State =================
    val rawNote by viewModel.currentNote.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()
    val cents by viewModel.currentCents.collectAsState()
    val referenceA by viewModel.referenceA.collectAsState()
    val refFreq by viewModel.referenceFreq.collectAsState()
    val useSolfege by viewModel.useSolfege.collectAsState()
    var tempUseSolfege by remember { mutableStateOf(useSolfege) }
    val note = remember(rawNote, useFlats) { viewModel.formatNoteForDisplay(rawNote) }

    var showDialog by remember { mutableStateOf(false) }
    var limitMessage by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(viewModel.isTunerRunning()) }
    var inputValue by remember { mutableStateOf(referenceA.toInt().toString()) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var tempUseFlats by remember { mutableStateOf(useFlats) }

    // ================= Help State =================
    var showHelp by remember { mutableStateOf(false) }
    var showTuningDialog by remember { mutableStateOf(false) }
    var showStringIndicators by remember { mutableStateOf(false) }
    var selectedTuning by remember { mutableStateOf(Tuning("Выбрать строй", emptyList())) }
    var selectedStringIndex by remember { mutableStateOf(0) }
    var stringReady by remember { mutableStateOf(List(6) { false }) }
    var helpMessage by remember { mutableStateOf("") }
    var enteredFreq by remember { mutableStateOf(referenceA.toInt().toString()) }

    val buttonShape = RoundedCornerShape(8.dp)
    val buttonHeight = 48.dp
    val buttonWidth = 170.dp

    // ================= Infinite Transition для мигания =================
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        )
    )

    val textBlinkAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // --- Note info ---
            Text(
                text = "Нота: $note",
                fontSize = 32.sp,
                color = if (abs(cents) <= 5) SpeedometerCenter else GrayText
            )
            Text(
                text = "Эталон: ${refFreq.toInt()} Hz",
                fontSize = 20.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Speedometer ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Mono2.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthF = size.width
                    val heightF = size.height
                    val centerY = heightF / 2f
                    val totalRange = 100f
                    val mainStep = 10
                    val subSteps = 9
                    val clampedCents = cents.toFloat().coerceIn(-50f, 50f)

                    for (i in -50..50 step mainStep) {
                        val xMain = ((i + 50f) / totalRange) * widthF
                        val mainColor = if (i in -10..10) SpeedometerCenter else GrayText
                        val mainHeight = 40f
                        drawLine(
                            color = mainColor,
                            start = androidx.compose.ui.geometry.Offset(xMain, centerY - mainHeight / 2f),
                            end = androidx.compose.ui.geometry.Offset(xMain, centerY + mainHeight / 2f),
                            strokeWidth = 4f
                        )
                        if (i < 50) {
                            val nextI = i + mainStep
                            val stepWidth = (nextI - i).toFloat() / (subSteps + 1)
                            for (sub in 1..subSteps) {
                                val xSub = ((i + 50f + sub * stepWidth) / totalRange) * widthF
                                val subColor =
                                    if ((xSub / widthF * totalRange - 50f) in -10f..10f) SpeedometerCenter else GrayText
                                val subHeight = 20f
                                drawLine(
                                    color = subColor,
                                    start = androidx.compose.ui.geometry.Offset(xSub, centerY - subHeight / 2f),
                                    end = androidx.compose.ui.geometry.Offset(xSub, centerY + subHeight / 2f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    val indicatorX = ((clampedCents + 50f) / totalRange) * widthF
                    val indicatorColor = when {
                        abs(clampedCents) <= 5f -> SpeedometerCenter
                        abs(clampedCents) <= 10f -> SpeedometerWarning
                        else -> GrayText
                    }
                    val indicatorHeight = when {
                        abs(clampedCents) <= 5f -> 50f
                        abs(clampedCents) <= 10f -> 40f
                        else -> 35f
                    }
                    drawLine(
                        color = indicatorColor,
                        start = androidx.compose.ui.geometry.Offset(indicatorX, centerY - indicatorHeight / 2f),
                        end = androidx.compose.ui.geometry.Offset(indicatorX, centerY + indicatorHeight / 2f),
                        strokeWidth = 6f
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Scale labels ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in -50..50 step 10) {
                    Text(text = if (i > 0) "+$i" else "$i", fontSize = 16.sp, color = GrayText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Start/Stop Button ---
            Button(
                onClick = {
                    if (isRunning) viewModel.stop().also { isRunning = false }
                    else viewModel.start().also { isRunning = true }
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(if (isRunning) "Стоп" else "Старт", color = ButtonTextColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Help toggle ---
            Button(onClick = {
                showHelp = !showHelp
                if (showHelp) {
                    selectedTuning = Tuning("Выбрать строй", emptyList())
                    enteredFreq = referenceA.toInt().toString()
                    helpMessage = ""
                    showStringIndicators = false
                } else {
                    helpMessage = ""
                    showStringIndicators = false
                }
            }, shape = buttonShape) {
                Text("Помощь в настройке")
            }

            // --- Help Panel ---
            if (showHelp) {
                Spacer(modifier = Modifier.height(12.dp))

                // --- Tuning selection ---
                Button(onClick = { showTuningDialog = true }, shape = buttonShape, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedTuning.name)
                }

                if (showTuningDialog) {
                    AlertDialog(
                        onDismissRequest = { showTuningDialog = false },
                        title = { Text("Выберите строй") },
                        text = {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp), // меньше расстояние между кнопками
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // занимает доступное пространство
                            ) {
                                Tunings.byCategory.forEach { (category, tunings) ->
                                    // Заголовок категории
                                    item {
                                        Text(category, fontSize = 18.sp, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Кнопки строев
                                    items(tunings) { tuning ->
                                        Button(
                                            onClick = {
                                                selectedTuning = tuning
                                                showTuningDialog = false
                                                showStringIndicators = false
                                                helpMessage = ""
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(
                                                horizontal = 6.dp,
                                                vertical = 4.dp
                                            )
                                        ) {
                                            Text(
                                                text = formatTuningText(tuning.name),
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTuningDialog = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Frequency input ---
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { if (it.all { it.isDigit() }) inputValue = it },
                    label = { Text("Эталон для ноты А4 (Hz)", color = Color.White) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val entered = inputValue.toIntOrNull()
                            if (entered != null && entered in 415..455)
                                viewModel.setReferenceA(entered.toDouble()).also {
                                    showDialog = false
                                    limitMessage = ""
                                }
                            else
                                limitMessage = "Введите значение от 415 до 455 Hz"

                            keyboardController?.hide()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- Apply Tuning ---
                Button(
                    onClick = {
                        val freq = enteredFreq.toIntOrNull()
                        if (freq != null && freq in 415..455) {
                            viewModel.setReferenceA(freq.toDouble())
                            if (selectedTuning.strings.isEmpty()) {
                                helpMessage = "Выберите строй"
                                showStringIndicators = false
                            } else {
                                stringReady = List(selectedTuning.strings.size) { false }
                                selectedStringIndex = 0
                                showStringIndicators = true
                                helpMessage = ""
                            }
                        } else {
                            helpMessage = "Введите значение от 415 до 455 Hz"
                            showStringIndicators = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonShape
                ) { Text("Применить") }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Strings indicators ---
                if (showStringIndicators) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedTuning.strings.forEachIndexed { index, _ ->
                            val ready = stringReady.getOrElse(index) { false }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        when {
                                            ready -> Color.Green
                                            index == selectedStringIndex -> Color.Yellow.copy(alpha = blinkAlpha)
                                            else -> Color.Gray
                                        }, CircleShape
                                    )
                                    .clickable { selectedStringIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${selectedTuning.strings.size - index}",
                                    color = if (ready) Color.White else Color.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val currentFreq by viewModel.referenceFreq.collectAsState()
                    LaunchedEffect(selectedStringIndex, currentFreq, cents) {
                        if (currentFreq <= 0) return@LaunchedEffect

                        val targetNoteName = viewModel.formatTuningNote(
                            selectedTuning.strings[selectedStringIndex]
                        )
                        val targetFreq = NoteFrequencies.getTuningFrequencies(selectedTuning, referenceA)[selectedStringIndex]
                        val lowerBound = targetFreq * 0.985
                        val upperBound = targetFreq * 1.015

                        helpMessage = when {
                            abs(cents) <= 5 && currentFreq in lowerBound..upperBound -> {
                                stringReady = stringReady.mapIndexed { i, v -> if (i == selectedStringIndex) true else v }
                                "Готово"
                            }
                            currentFreq < targetFreq -> "Надо настроить на $targetNoteName: подтянуть"
                            currentFreq > targetFreq -> "Надо настроить на $targetNoteName: опустить"
                            else -> ""
                        }
                    }

                    Text(
                        helpMessage,
                        color = if (helpMessage == "Готово") Color.Green else Color.Red,
                        fontSize = 18.sp,
                        modifier = Modifier.alpha(textBlinkAlpha)
                    )
                }
            }
        }

        // ================= Top Row A4 + Cents =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { tempUseFlats = useFlats; showDialog = true },
                    modifier = Modifier.height(buttonHeight).width(buttonWidth),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                ) { Text("A4 = ${referenceA.toInt()} Hz", color = ButtonTextColor) }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.height(buttonHeight).width(buttonWidth).background(Mono3, buttonShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cents: ", color = ButtonTextColor)
                        Text(String.format(Locale.US, "%+03d", cents.roundToInt()), color = ButtonTextColor, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // ================= A4 Dialog =================
        if (showDialog) {
            AlertDialog(
                containerColor = Color(0xFF8E4FD1),
                onDismissRequest = { showDialog = false },
                title = { Text("Эталонная частота A4", color = ButtonTextColor) },

                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Система нот", color = ButtonTextColor, fontSize = 16.sp)
                            Button(onClick = { tempUseFlats = !tempUseFlats }, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Mono3)) {
                                Text(if (tempUseFlats) "♭" else "#", color = ButtonTextColor, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text("Формат нот", color = ButtonTextColor, fontSize = 16.sp)

                            Button(
                                onClick = { tempUseSolfege = !tempUseSolfege },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                            ) {
                                Text(
                                    if (tempUseSolfege) "До Ре Ми" else "C D E",
                                    color = ButtonTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { if (it.all { it.isDigit() }) inputValue = it },
                            label = { Text("Частота (Hz)", color = Color.White) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455)
                                        viewModel.setReferenceA(entered.toDouble()).also {
                                            showDialog = false
                                            limitMessage = ""
                                        }
                                    else
                                        limitMessage = "Введите значение от 415 до 455 Hz"

                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                cursorColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Button(shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Mono2), onClick = { if ((inputValue.toIntOrNull() ?: 415) > 415) inputValue = ((inputValue.toIntOrNull() ?: 415) - 1).toString() else limitMessage="Минимум 415 Hz" }) { Text("-", color = ButtonTextColor) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Mono2), onClick = { if ((inputValue.toIntOrNull() ?: 440) < 455) inputValue = ((inputValue.toIntOrNull() ?: 440) + 1).toString() else limitMessage="Максимум 455 Hz" }) { Text("+", color = ButtonTextColor) }
                        }

                        if (limitMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(limitMessage, color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Mono2), onClick = { viewModel.setReferenceA(440.0); inputValue="440"; limitMessage=""; showDialog=false; keyboardController?.hide() }) { Text("440 Hz", color = ButtonTextColor) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Mono3), onClick = {
                            val entered = inputValue.toIntOrNull()
                            if (entered != null && entered in 415..455) {
                                viewModel.setReferenceA(entered.toDouble())
                                viewModel.setNoteSystem(tempUseFlats)
                                viewModel.setSolfegeSystem(tempUseSolfege)
                                limitMessage=""
                                showDialog=false
                            } else limitMessage="Введите значение от 415 до 455 Hz"
                            keyboardController?.hide()
                        }) { Text("Применить", color = ButtonTextColor) }
                    }
                }
            )
        }
    }
}
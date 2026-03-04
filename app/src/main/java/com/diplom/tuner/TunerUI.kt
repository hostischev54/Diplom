package com.diplom.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    // ================= Colors =================
    val Background = Color(0xFF450769)
    val Mono1 = Color(0xFF81549A)
    val Mono2 = Color(0xFF6C2E91)
    val Mono3 = Color(0xFF5F158A)
    val Mono4 = Color(0xFF450769)
    val Mono5 = Color(0xFF300848)
    val AccentGreen = Color(0xFF4CAF50)
    val GrayText = Color(0xFFAAAAAA)
    val ButtonTextColor = Color.White
    val SpeedometerWarning = Color(0xFFFFA500)
    val SpeedometerCenter = AccentGreen

    // ================= State =================
    val note by viewModel.currentNote.collectAsState()
    val cents by viewModel.currentCents.collectAsState()
    val refFreq by viewModel.referenceFreq.collectAsState()
    val referenceA by viewModel.referenceA.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var limitMessage by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(viewModel.isTunerRunning()) }
    var inputValue by remember { mutableStateOf(referenceA.toInt().toString()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val buttonShape = RoundedCornerShape(8.dp)
    val buttonHeight = 48.dp
    val buttonWidth = 170.dp

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Background)
    ) {

        // ================= Main Column =================
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
                    .height(80.dp) // уменьшили высоту
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
                        val mainHeight = 40f // высота основной линии уменьшена
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
                                val subHeight = 20f // высота суб-деления уменьшена
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
                    Text(
                        text = if (i > 0) "+$i" else "$i",
                        fontSize = 16.sp,
                        color = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Start/Stop Button ---
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.stop()
                        isRunning = false
                    } else {
                        viewModel.start()
                        isRunning = true
                    }
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(if (isRunning) "Стоп" else "Старт", color = ButtonTextColor)
            }
        }

        // ================= Top Row A4 + Cents =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // --- A4 button ---
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .height(buttonHeight)
                        .width(buttonWidth),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Mono2)
                ) {
                    Text("A4 = ${referenceA.toInt()} Hz", color = ButtonTextColor)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // --- Cents display ---
                Box(
                    modifier = Modifier
                        .height(buttonHeight)
                        .width(buttonWidth)
                        .background(Mono3, buttonShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cents: ", color = ButtonTextColor)
                        Text(String.format(Locale.US, "%+03d", cents.roundToInt()),
                            color = ButtonTextColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ================= A4 Modal Dialog =================
        if (showDialog) {
            AlertDialog(
                containerColor = Color(0xFF8E4FD1),
                onDismissRequest = { showDialog = false },
                title = { Text("Эталонная частота A4", color = ButtonTextColor) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) inputValue = newValue
                            },
                            label = { Text("Частота (Hz)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455) {
                                        viewModel.setReferenceA(entered.toDouble())
                                        limitMessage = ""
                                        showDialog = false
                                    } else {
                                        limitMessage = "Введите значение от 415 до 455 Hz"
                                    }
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

                        // --- +/- buttons (adjust inputValue only) ---
                        Row {
                            Button(onClick = {
                                if (inputValue.toIntOrNull() ?: 415 > 415) {
                                    inputValue = ((inputValue.toIntOrNull() ?: 415) - 1).toString()
                                } else limitMessage = "Минимум 415 Hz"
                            }, shape = buttonShape, colors = ButtonDefaults.buttonColors(containerColor = Mono3)) {
                                Text("-", color = ButtonTextColor)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(onClick = {
                                if (inputValue.toIntOrNull() ?: 440 < 455) {
                                    inputValue = ((inputValue.toIntOrNull() ?: 440) + 1).toString()
                                } else limitMessage = "Максимум 455 Hz"
                            }, shape = buttonShape, colors = ButtonDefaults.buttonColors(containerColor = Mono3)) {
                                Text("+", color = ButtonTextColor)
                            }
                        }
                        if (limitMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = limitMessage,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    // --- Apply + 440Hz centered ---
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row {
                            Button(
                                onClick = {
                                    viewModel.setReferenceA(440.0)
                                    inputValue = "440"
                                    limitMessage = ""
                                    showDialog = false
                                    keyboardController?.hide()
                                },
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Mono3)
                            ) { Text("440 Hz", color = ButtonTextColor) }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    val entered = inputValue.toIntOrNull()
                                    if (entered != null && entered in 415..455) {
                                        viewModel.setReferenceA(entered.toDouble())
                                        limitMessage = ""
                                        showDialog = false
                                    } else {
                                        limitMessage = "Введите значение от 415 до 455 Hz"
                                    }
                                    keyboardController?.hide()
                                },
                                shape = buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                            ) { Text("Применить", color = ButtonTextColor) }

                        }
                    }
                }
            )
        }
    }
}
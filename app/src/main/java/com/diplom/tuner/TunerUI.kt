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
    val note by viewModel.currentNote.collectAsState()
    val cents by viewModel.currentCents.collectAsState()
    val refFreq by viewModel.referenceFreq.collectAsState()
    val referenceA by viewModel.referenceA.collectAsState()

    // --- Состояния ---
    var showDialog by remember { mutableStateOf(false) }
    var limitMessage by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(viewModel.isTunerRunning()) }
    var inputValue by remember { mutableStateOf(referenceA.toInt().toString()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {

        // --- Основной контент Column ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // --- Информация о ноте ---
            Text(
                text = "Нота: $note",
                fontSize = 32.sp,
                color = if (abs(cents) <= 5) Color(0xFF006400) else Color.Black
            )
            Text(
                text = "Эталон: ${refFreq.toInt()} Hz",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Спидометр ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
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
                        val mainColor = if (i in -10..10) Color(0xFF90EE90) else Color.Black
                        drawLine(
                            color = mainColor,
                            start = androidx.compose.ui.geometry.Offset(xMain, centerY - 50f / 2f),
                            end = androidx.compose.ui.geometry.Offset(xMain, centerY + 50f / 2f),
                            strokeWidth = 4f
                        )
                        if (i < 50) {
                            val nextI = i + mainStep
                            val stepWidth = (nextI - i).toFloat() / (subSteps + 1)
                            for (sub in 1..subSteps) {
                                val xSub = ((i + 50f + sub * stepWidth) / totalRange) * widthF
                                val subColor =
                                    if ((xSub / widthF * totalRange - 50f) in -10f..10f) Color(0xFF90EE90) else Color.Black
                                drawLine(
                                    color = subColor,
                                    start = androidx.compose.ui.geometry.Offset(xSub, centerY - 25f / 2f),
                                    end = androidx.compose.ui.geometry.Offset(xSub, centerY + 25f / 2f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    val indicatorX = ((clampedCents + 50f) / totalRange) * widthF
                    val indicatorColor = when {
                        abs(clampedCents) <= 5f -> Color(0xFF006400)
                        abs(clampedCents) <= 10f -> Color(0xFFFFA500)
                        else -> Color.Black
                    }
                    val indicatorHeight = when {
                        abs(clampedCents) <= 5f -> 70f
                        abs(clampedCents) <= 10f -> 60f
                        else -> 50f
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

            // --- Подписи ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in -50..50 step 10) {
                    Text(
                        text = if (i > 0) "+$i" else "$i",
                        fontSize = 16.sp,
                        color = if (i in -10..10) Color(0xFF90EE90) else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Кнопка Старт/Стоп ---
            Button(onClick = {
                if (isRunning) {
                    viewModel.stop()
                    isRunning = false
                } else {
                    viewModel.start()
                    isRunning = true
                }
            }) {
                Text(if (isRunning) "Стоп" else "Старт")
            }
        }

        // --- Строка A4 + Cents ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            val shape = RoundedCornerShape(8.dp)
            val height = 48.dp
            val width = 170.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {

                // --- Кнопка A4 ---
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .height(height)
                        .width(width),
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    )
                ) {
                    Text(
                        text = "A4 = ${referenceA.toInt()} Hz",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // --- Cents ---
                Box(
                    modifier = Modifier
                        .height(height)
                        .width(width)
                        .background(
                            color = Color(0xFF3700B3),
                            shape = shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cents: ",
                            color = Color.White
                        )
                        Text(
                            text = String.format(Locale.US, "%+03d", cents.roundToInt()),
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- Диалог A4 без кнопки "Применить" ---
        // --- Диалог A4 ---
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Эталонная частота A4") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // --- Поле ввода ---
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    inputValue = newValue
                                }
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
                                        showDialog = false // закрываем модалку
                                    } else {
                                        limitMessage = "Введите значение от 415 до 455 Hz"
                                    }
                                    keyboardController?.hide()
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Кнопки - / + ---
                        Row {
                            Button(onClick = {
                                if (viewModel.referenceA.value > 415) {
                                    viewModel.decreaseReferenceA()
                                    inputValue = viewModel.referenceA.value.toInt().toString()
                                } else limitMessage = "Минимум 415 Hz"
                            }) { Text("-") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(onClick = {
                                if (viewModel.referenceA.value < 455) {
                                    viewModel.increaseReferenceA()
                                    inputValue = viewModel.referenceA.value.toInt().toString()
                                } else limitMessage = "Максимум 455 Hz"
                            }) { Text("+") }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Кнопка 440 Hz ---
                        Button(onClick = {
                            viewModel.setReferenceA(440.0)
                            inputValue = "440"
                            limitMessage = ""
                            showDialog = false // закрываем модалку
                        }) { Text("440 Hz") }
                    }
                },
                confirmButton = {}, // удаляем кнопку "Применить"
            )
        }

        // --- Сообщение о лимите ---
        if (limitMessage.isNotEmpty()) {
            Text(
                text = limitMessage,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
            )
        }
    }
}
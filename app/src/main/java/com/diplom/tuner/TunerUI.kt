package com.diplom.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    val note by viewModel.currentNote.collectAsState()
    val cents by viewModel.currentCents.collectAsState()
    val refFreq by viewModel.referenceFreq.collectAsState()

    // Состояние кнопки сразу в "Стоп", т.к. тюнер стартует
    var isRunning by remember { mutableStateOf(viewModel.isTunerRunning()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Нота: $note",
            fontSize = 32.sp,
            color = if (abs(cents) <= 5) Color(0xFF006400) else Color.Black
        )
        Text(
            text = "Еталон: ${refFreq.toInt()} Hz", // целочисленно
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Спидометр
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

                // Основные и промежуточные деления
                for (i in -50..50 step mainStep) {
                    val xMain = ((i + 50f) / totalRange) * widthF
                    val mainColor = if (i in -10..10) Color(0xFF90EE90) else Color.Black
                    drawLine(
                        color = mainColor,
                        start = Offset(xMain, centerY - 50f / 2f),
                        end = Offset(xMain, centerY + 50f / 2f),
                        strokeWidth = 4f
                    )

                    // Малые линии между основными
                    if (i < 50) {
                        val nextI = i + mainStep
                        val stepWidth = (nextI - i).toFloat() / (subSteps + 1)
                        for (sub in 1..subSteps) {
                            val xSub = ((i + 50f + sub * stepWidth) / totalRange) * widthF
                            val subColor = if ((xSub / widthF * totalRange - 50f) in -10f..10f) Color(0xFF90EE90) else Color.Black
                            drawLine(
                                color = subColor,
                                start = Offset(xSub, centerY - 25f / 2f),
                                end = Offset(xSub, centerY + 25f / 2f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // Индикатор текущего cents
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
                    start = Offset(indicatorX, centerY - indicatorHeight / 2f),
                    end = Offset(indicatorX, centerY + indicatorHeight / 2f),
                    strokeWidth = 6f
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Подписи под шкалой
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

        // Кнопка Старт/Стоп
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
}
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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    val pitch by viewModel.currentFrequency.collectAsState()
    val note by viewModel.currentNote.collectAsState()
    val deviation by viewModel.deviation.collectAsState()
    var isRunning by remember { mutableStateOf(viewModel.isRunning()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Нота: $note",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Частота: ${"%.2f".format(pitch)} Hz",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Шкала
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.LightGray, RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val centerY = height / 2

                val maxDeviation = 50f // диапазон ±50 Hz
                val clampedDev = deviation.coerceIn(-maxDeviation, maxDeviation)
                val indicatorX = centerX + (clampedDev / maxDeviation) * (width / 2 - 20)

                // Центральная линия (0 Hz отклонения)
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, height),
                    strokeWidth = 4f
                )

                // Ползунок
                drawCircle(
                    color = if (clampedDev.absoluteValue < 1f) Color.Green else Color.Red,
                    radius = 15f,
                    center = Offset(indicatorX, centerY)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Подписи по краям
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("−50 Hz", style = MaterialTheme.typography.bodyMedium)
            Text("0 Hz", style = MaterialTheme.typography.bodyMedium)
            Text("+50 Hz", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

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
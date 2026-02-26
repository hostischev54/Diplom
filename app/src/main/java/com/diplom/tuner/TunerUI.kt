package com.diplom.tuner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

@Composable
fun TunerUI(viewModel: TunerViewModel) {
    val note by viewModel.currentNote.collectAsState()
    val cents by viewModel.currentCents.collectAsState()
    val refFreq by viewModel.referenceFreq.collectAsState()
    var isRunningState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Нота: $note",
            style = MaterialTheme.typography.titleLarge,
            color = if (abs(cents) < 1f) Color.Green else Color.Black
        )

        Text(
            text = "Еталон: ${"%.2f".format(refFreq)} Hz",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${"%.1f".format(cents)} ц",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Линейный спидометр
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.LightGray.copy(alpha=0.2f), RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val widthF = size.width
                val heightF = size.height
                val centerY = heightF / 2f
                val totalRange = 100f // -50..+50

                // Деления спидометра
                for (i in -50..50 step 10) {
                    val x = ((i + 50).toFloat() / totalRange) * widthF   // <-- приведение к Float
                    val lineHeight = when {
                        abs(i) <= 5 -> 30f
                        abs(i) <= 10 -> 25f
                        else -> 20f
                    }
                    val lineColor = when {
                        abs(i) <= 5 -> Color(0xFF006400)    // темно-зеленый
                        abs(i) <= 10 -> Color(0xFF90EE90)   // светло-зеленый
                        else -> Color.Yellow
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(x, centerY - lineHeight / 2f),   // <-- /2f
                        end = Offset(x, centerY + lineHeight / 2f),
                        strokeWidth = 4f
                    )
                }

                // Индикатор отклонения
                val clampedCents = cents.coerceIn(-50.0, 50.0)
                val indicatorX = ((clampedCents.toFloat() + 50f) / totalRange) * widthF  // <-- приведение к Float
                val indicatorColor = when {
                    abs(clampedCents) <= 5 -> Color(0xFF006400)    // темно-зеленый
                    abs(clampedCents) <= 10 -> Color(0xFF90EE90)   // светло-зеленый
                    else -> Color.Yellow
                }
                drawLine(
                    color = indicatorColor,
                    start = Offset(indicatorX, centerY - 50f / 2f),
                    end = Offset(indicatorX, centerY + 50f / 2f),
                    strokeWidth = 6f
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (isRunningState) {
                viewModel.stop()
                isRunningState = false
            } else {
                viewModel.start()
                isRunningState = true
            }
        }) {
            Text(if (isRunningState) "Стоп" else "Старт")
        }
    }
}
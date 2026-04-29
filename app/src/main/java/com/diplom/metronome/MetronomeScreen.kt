package com.diplom.metronome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diplom.tuner.ui.theme.AppColors

@Composable
fun MetronomeScreen() {
    var bpm by remember { mutableStateOf(120f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${bpm.toInt()} BPM",
            fontSize = 48.sp,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Slider(
            value = bpm,
            onValueChange = { bpm = it },
            valueRange = 40f..240f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (bpm > 40) bpm-- }) { Text("-") }
            Button(onClick = { if (bpm < 240) bpm++ }) { Text("+") }
        }
    }
}
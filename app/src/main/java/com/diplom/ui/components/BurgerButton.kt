package com.diplom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.diplom.tuner.ui.theme.AppColors

@Composable
fun BurgerButton(
    onClick: () -> Unit,
    buttonWidth: Dp = 40.dp,
    buttonHeight: Dp = 48.dp,
) {
    Box(
        modifier = Modifier
            .size(width = buttonWidth, height = buttonHeight)
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)) // сначала скругляем
            .background(AppColors.BackgroundTop.lighten(1.15f)) // затем заливка внутри скругления
            .clickable { onClick() }, // теперь кликабельная зона точно повторяет форму
        contentAlignment = Alignment.Center
    ) {
        Text("≡", color = Color.White, fontSize = 20.sp)
    }
}
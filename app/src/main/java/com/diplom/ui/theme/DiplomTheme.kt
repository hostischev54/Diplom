package com.diplom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.diplom.tuner.ui.theme.AppColors

@Composable
fun DiplomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Primary,
            secondary = AppColors.Accent,
            background = AppColors.BackgroundBottom,
            surface = AppColors.Surface,
            onPrimary = AppColors.TextPrimary,
            onBackground = AppColors.TextPrimary,
            onSurface = AppColors.TextPrimary
        ),
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
package com.diplom.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diplom.navigation.AppScreen
import com.diplom.tuner.ui.theme.AppColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource

fun Color.lighten(factor: Float = 1.1f): Color {
    return Color(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

@Composable
fun DrawerContent(
    currentScreen: AppScreen,
    onItemClick: (AppScreen) -> Unit,
    drawerWidth: Dp = 260.dp,
    buttonWidth: Dp = 40.dp,
    buttonHeight: Dp = 80.dp,
    content: @Composable () -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }

    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else -(drawerWidth - buttonWidth),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔵 ОСНОВНОЙ КОНТЕНТ
        content()

        // 🔴 OVERLAY (блокирует клики)
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)) // можно 0f если без затемнения
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            )
        }

        // 🟣 DRAWER
        Row(
            modifier = Modifier
                .offset(x = offsetX)
                .fillMaxHeight()
                .width(drawerWidth)
        ) {

            // Основная часть
            Column(
                modifier = Modifier
                    .width(drawerWidth - buttonWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.BackgroundTop.lighten(1.15f),
                                AppColors.BackgroundBottom.lighten(1.15f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text("Меню", fontSize = 22.sp, color = AppColors.TextPrimary)

                Spacer(modifier = Modifier.height(16.dp))

                DrawerItem("Тюнер", currentScreen == AppScreen.Tuner) {
                    onItemClick(AppScreen.Tuner)
                    isOpen = false
                }

                DrawerItem("Автотабулатура", currentScreen == AppScreen.AutoTab) {
                    onItemClick(AppScreen.AutoTab)
                    isOpen = false
                }

                DrawerItem("Лупер", currentScreen == AppScreen.Looper) {
                    onItemClick(AppScreen.Looper)
                    isOpen = false
                }
            }

            // 🍔 КНОПКА
            Box(
                modifier = Modifier
                    .size(width = buttonWidth, height = buttonHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                BurgerButton(
                    onClick = { isOpen = !isOpen },
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        color = if (selected) AppColors.Accent else AppColors.TextPrimary
    )
}
package com.diplom.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.diplom.R
import com.diplom.navigation.AppScreen
import com.diplom.tuner.ui.theme.AppColors

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

    var currentLang by remember {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2)
        mutableStateOf(if (tag == "uk") "uk" else "en")
    }

    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else -(drawerWidth - buttonWidth),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Box(modifier = Modifier.fillMaxSize()) {

        content()

        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            )
        }

        Row(
            modifier = Modifier
                .offset(x = offsetX)
                .fillMaxHeight()
                .width(drawerWidth)
        ) {
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
                Text(
                    stringResource(R.string.nav_menu),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                DrawerItem(stringResource(R.string.nav_tuner), currentScreen == AppScreen.Tuner) {
                    onItemClick(AppScreen.Tuner)
                    isOpen = false
                }
                DrawerItem(stringResource(R.string.nav_autotab), currentScreen == AppScreen.AutoTab) {
                    onItemClick(AppScreen.AutoTab)
                    isOpen = false
                }
                DrawerItem(stringResource(R.string.nav_looper), currentScreen == AppScreen.Looper) {
                    onItemClick(AppScreen.Looper)
                    isOpen = false
                }

                Spacer(modifier = Modifier.weight(1f))

                LanguageToggle(
                    currentLang = currentLang,
                    onToggle = { newLang ->
                        android.util.Log.d("LANG", "=== onToggle called, newLang=$newLang ===")
                        currentLang = newLang
                        val localeList = LocaleListCompat.forLanguageTags(newLang)
                        android.util.Log.d("LANG", "localeList=$localeList, isEmpty=${localeList.isEmpty}")
                        AppCompatDelegate.setApplicationLocales(localeList)
                        val after = AppCompatDelegate.getApplicationLocales()
                        android.util.Log.d("LANG", "after setApplicationLocales: $after")
                    }
                )
            }

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
fun LanguageToggle(currentLang: String, onToggle: (String) -> Unit) {
    val isUk = currentLang == "uk"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.BackgroundTop.lighten(1.3f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isUk) Color(0xFF6C2E91) else Color.Transparent)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (!isUk) onToggle("uk") }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🇺🇦 УКР",
                fontSize = 13.sp,
                fontWeight = if (isUk) FontWeight.Bold else FontWeight.Normal,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (!isUk) Color(0xFF6C2E91) else Color.Transparent)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (isUk) onToggle("en") }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🇬🇧 ENG",
                fontSize = 13.sp,
                fontWeight = if (!isUk) FontWeight.Bold else FontWeight.Normal,
                color = Color.White
            )
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
        fontSize = 16.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Color.White else AppColors.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    )
}
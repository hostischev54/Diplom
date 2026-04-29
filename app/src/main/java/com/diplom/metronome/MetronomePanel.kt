package com.diplom.metronome

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.diplom.tuner.ui.theme.AppColors
import com.diplom.ui.components.lighten
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas

@Composable
fun MetronomePanel(
    viewModel: MetronomeViewModel,
    visible: Boolean
) {
    val state by viewModel.state.collectAsState()

    val panelWidth = 260.dp
    val buttonWidth = 40.dp

    LaunchedEffect(visible) {
        if (!visible) viewModel.stop()
    }

    if (!visible) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        var isPanelOpen by remember { mutableStateOf(false) }

        val panelOffset by animateDpAsState(
            targetValue = if (isPanelOpen) 0.dp else panelWidth - buttonWidth,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )

        Row(
            modifier = Modifier
                .offset(x = panelOffset)
                .height(IntrinsicSize.Min)
                .width(panelWidth)
        ) {
            // Кнопка
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(72.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(width = buttonWidth, height = 72.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, topStart = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.BackgroundTop.lighten(1.2f),
                                    AppColors.BackgroundBottom.lighten(1.2f)
                                )
                            )
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isPanelOpen = !isPanelOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "𝄞",
                        fontSize = 28.sp,
                        color = if (state.isPlaying) AppColors.Accent else AppColors.TextPrimary
                    )
                }
            }

            // Панель
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.BackgroundTop.lighten(1.15f),
                                AppColors.BackgroundBottom.lighten(1.15f)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Метроном",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "${state.bpm} BPM",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Accent
                )

                Slider(
                    value = state.bpm.toFloat(),
                    onValueChange = { viewModel.setBpm(it.toInt()) },
                    valueRange = 40f..240f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("-5") { viewModel.setBpm(state.bpm - 5) }
                    SmallButton("-1") { viewModel.setBpm(state.bpm - 1) }
                    SmallButton("+1") { viewModel.setBpm(state.bpm + 1) }
                    SmallButton("+5") { viewModel.setBpm(state.bpm + 5) }
                }

                Spacer(Modifier.height(16.dp))

                Text("Размер такта", fontSize = 14.sp, color = AppColors.TextPrimary)
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Числитель
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmallButton("▲") { viewModel.setBeatsPerBar(state.beatsPerBar + 1) }
                        Text(
                            "${state.beatsPerBar}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        SmallButton("▼") { viewModel.setBeatsPerBar(state.beatsPerBar - 1) }
                    }

                    Text(
                        " / ",
                        fontSize = 28.sp,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Знаменатель
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val denoms = listOf(2, 4, 8, 16)
                        val currentIndex = denoms.indexOf(state.denominator).coerceAtLeast(0)

                        SmallButton("▲") {
                            viewModel.setDenominator(denoms[(currentIndex + 1) % denoms.size])
                        }
                        Text(
                            "${state.denominator}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        SmallButton("▼") {
                            viewModel.setDenominator(denoms[(currentIndex - 1 + denoms.size) % denoms.size])
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                BeatIndicator(
                    beats = state.beatsPerBar,
                    currentBeat = state.currentBeat
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.togglePlay() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isPlaying) AppColors.Accent
                        else AppColors.BackgroundTop.lighten(1.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isPlaying) "Стоп" else "Старт",
                        color = AppColors.TextPrimary
                    )
                }
            }
        }
    }
}


@Composable
fun BeatIndicator(beats: Int, currentBeat: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        repeat(beats) { index ->
            val isActive = index == currentBeat
            val isAccent = index == 0
            Box(
                modifier = Modifier
                    .size(if (isAccent) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive && isAccent -> AppColors.Accent
                            isActive -> AppColors.Accent.copy(alpha = 0.7f)
                            isAccent -> AppColors.TextPrimary.copy(alpha = 0.6f)
                            else -> AppColors.TextPrimary.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
fun SmallButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.BackgroundTop.lighten(1.3f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AppColors.TextPrimary, fontSize = 13.sp)
    }
}
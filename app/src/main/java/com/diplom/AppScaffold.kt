package com.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diplom.autotab.AutoTabScreen
import com.diplom.looper.LooperScreen
import com.diplom.metronome.MetronomePanel
import com.diplom.metronome.MetronomeViewModel
import com.diplom.navigation.AppScreen
import com.diplom.tuner.TunerUI
import com.diplom.tuner.TunerViewModel
import com.diplom.ui.components.DrawerContent
import androidx.compose.ui.Alignment

@Composable
fun AppScaffold(tunerViewModel: TunerViewModel) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Tuner) }
    val metronomeViewModel: MetronomeViewModel = viewModel()

    Box(modifier = Modifier.fillMaxSize()) {

        DrawerContent(
            currentScreen = currentScreen,
            onItemClick = { currentScreen = it }
        ) {
            when (currentScreen) {
                AppScreen.Tuner -> TunerUI(tunerViewModel)
                AppScreen.AutoTab -> AutoTabScreen()
                AppScreen.Looper -> LooperScreen()
            }
        }

        // Метроном поверх всего, скрыт на AutoTab
        MetronomePanel(
            viewModel = metronomeViewModel,
            visible = currentScreen != AppScreen.AutoTab
        )
    }
}
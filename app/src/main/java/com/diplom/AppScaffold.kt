package com.diplom

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diplom.tuner.TunerUI
import com.diplom.tuner.TunerViewModel
import com.diplom.navigation.AppScreen
import com.diplom.ui.components.DrawerContent
import com.diplom.autotab.AutoTabScreen

@Composable
fun AppScaffold(viewModel: TunerViewModel) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Tuner) }

    DrawerContent(
        currentScreen = currentScreen,
        onItemClick = { currentScreen = it }
    ) {

        when (currentScreen) {
            AppScreen.Tuner -> TunerUI(viewModel)

            AppScreen.AutoTab -> AutoTabScreen()

            AppScreen.Looper -> androidx.compose.material3.Text(
                "Лупер",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
package com.diplom.navigation

sealed class AppScreen(
    val route: String,
    val title: String
) {
    object Tuner : AppScreen("tuner", "Тюнер")
    object AutoTab : AppScreen("autotab", "Автотабулатура")
    object Looper : AppScreen("looper", "Лупер")

    companion object {
        val allScreens = listOf(Tuner, AutoTab, Looper)
    }
}
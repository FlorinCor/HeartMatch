package com.example.heartmatch.ui.navigation

sealed class Screen {
    object Splash : Screen()
    object MainMenu : Screen()
    object LevelMap : Screen()
    object Gameplay : Screen()
    object Settings : Screen()
    object Tutorial : Screen()
    object DailyReward : Screen()
}

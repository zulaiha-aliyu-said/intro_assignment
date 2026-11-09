package com.capcutmini.app.ui.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Projects : AppDestination("projects")
    data object Editor : AppDestination("editor")
    data object AiStudio : AppDestination("ai_studio")
}

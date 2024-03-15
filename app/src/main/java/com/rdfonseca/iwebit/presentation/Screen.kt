package com.rdfonseca.iwebit.presentation

sealed class Screen(val route: String) {
    object DefinitionsScreen: Screen("definitions_screen")
    object WebPlatformScreen: Screen("web_plataform_screen")
    object SupportScreen: Screen("support_screen")
    object RecentsScreen: Screen("recents_screen")
    object LogInScreen: Screen("log_in_screen")
}

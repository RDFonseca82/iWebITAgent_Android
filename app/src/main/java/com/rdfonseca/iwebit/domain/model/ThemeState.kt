package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep

@Keep
data class ThemeState(
    val darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    val isSystemInDarkTheme: Boolean = false,
)

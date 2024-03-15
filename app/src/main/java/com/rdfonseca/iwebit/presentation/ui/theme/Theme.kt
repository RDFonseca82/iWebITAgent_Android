package com.rdfonseca.iwebit.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColors
import androidx.compose.material3.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color.Companion.White
import com.rdfonseca.iwebit.presentation.LocalUserPreferences

private val DarkColorPalette = darkColors(
    primary = Blue200,
    primaryVariant = Blue500,
    secondary = BlueA200,
    background = Gray800
)

private val LightColorPalette = lightColors(
    primary = Blue500,
    primaryVariant = Blue700,
    secondary = BlueA200,
    background = White
)


@Composable
fun IWebITTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isDarkTheme(): Boolean {
    return LocalUserPreferences.current.theme.darkThemeMode.isDarkTheme(isSystemInDarkTheme())
}




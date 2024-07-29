package com.rdfonseca.iwebit.presentation.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rdfonseca.iwebit.presentation.LocalUserPreferences

private val DarkColorPalette = darkColorScheme(
    primary = Blue200,
    secondary = Blue500,
    tertiary = BlueA200,
    background = Gray800
)

private val LightColorPalette = lightColorScheme(
    primary = Blue500,
    secondary = Blue700,
    tertiary = BlueA200,
    background = White
)


@Composable
fun IWebITTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) Color.parseColor("#2A2A2A") else Color.WHITE
            window.navigationBarColor = if (darkTheme) Color.parseColor("#2A2A2A") else Color.WHITE
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isDarkTheme(): Boolean {
    return LocalUserPreferences.current.theme.darkThemeMode.isDarkTheme(isSystemInDarkTheme())
}




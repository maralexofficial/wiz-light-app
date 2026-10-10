package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = AmberGlow,
    onSecondary = Color.Black,
    secondaryContainer = AmberSurfaceDark,
    onSecondaryContainer = AmberWarm,
    tertiary = Color(0xFFA78BFA),
    background = Slate900,
    onBackground = Slate100,
    surface = SurfaceDark,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    error = ErrorRed,
    errorContainer = Color(0xFF450A0A),
    outline = CardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = AmberWarm,
    onSecondary = Color.White,
    secondaryContainer = AmberSurface,
    onSecondaryContainer = Color(0xFFB45309),
    tertiary = Color(0xFF7C3AED),
    background = Slate50,
    onBackground = Slate900,
    surface = SurfaceLight,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    outline = CardBorderLight
)

@Composable
fun MyApplicationTheme(
    themePreference: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

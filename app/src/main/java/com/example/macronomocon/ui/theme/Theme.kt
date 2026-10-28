package com.example.macronomocon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Terracotta, onPrimary = Color.White,
    primaryContainer = Peach, onPrimaryContainer = Color(0xFF432414),
    secondary = Olive, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E9D6), onSecondaryContainer = Color(0xFF29321D),
    tertiary = Color(0xFF84601E), onTertiary = Color.White,
    background = Cream, onBackground = Espresso,
    surface = Cream, onSurface = Espresso,
    surfaceVariant = Color(0xFFF0E5D8), onSurfaceVariant = Color(0xFF66584C),
    outline = Color(0xFF827366), outlineVariant = Color(0xFFDED1C2)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB596), onPrimary = Color(0xFF542612),
    primaryContainer = Color(0xFF743923), onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFC4CDAA), onSecondary = Color(0xFF2D371F),
    secondaryContainer = Color(0xFF404C30), onSecondaryContainer = Color(0xFFE1E9CA),
    background = Color(0xFF201A16), onBackground = Color(0xFFF5E5D8),
    surface = Color(0xFF201A16), onSurface = Color(0xFFF5E5D8),
    surfaceVariant = Color(0xFF40362E), onSurfaceVariant = Color(0xFFD6C5B6),
    outline = Color(0xFFA79381), outlineVariant = Color(0xFF55473B)
)

@Composable
fun MacronomOconTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

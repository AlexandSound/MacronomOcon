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
    secondaryContainer = Color(0xFFEAF2DA), onSecondaryContainer = Color(0xFF29321D),
    tertiary = Color(0xFF96701F), onTertiary = Color.White,
    background = Cream, onBackground = Espresso,
    surface = Cream, onSurface = Espresso,
    surfaceVariant = Color(0xFFF7ECDF), onSurfaceVariant = Color(0xFF66584C),
    outline = Color(0xFF827366), outlineVariant = Color(0xFFE6D9CA)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC2A8), onPrimary = Color(0xFF542612),
    primaryContainer = Color(0xFF8A4529), onPrimaryContainer = Color(0xFFFFE4D6),
    secondary = Color(0xFFD2DCB4), onSecondary = Color(0xFF2D371F),
    secondaryContainer = Color(0xFF4B5A37), onSecondaryContainer = Color(0xFFEAF2D4),
    background = Color(0xFF2A221D), onBackground = Color(0xFFFBEDE2),
    surface = Color(0xFF2A221D), onSurface = Color(0xFFFBEDE2),
    surfaceVariant = Color(0xFF4C4137), onSurfaceVariant = Color(0xFFE0D0C1),
    outline = Color(0xFFB5A190), outlineVariant = Color(0xFF62544A)
)

@Composable
fun MacronomOconTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

package com.example.macronomocon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ButtonGreen, onPrimary = Color.White,
    primaryContainer = Color.White, onPrimaryContainer = Color.Black,
    secondary = ButtonGreenHover, onSecondary = Color.White,
    secondaryContainer = Color.White, onSecondaryContainer = Color.Black,
    tertiary = Color.Black, onTertiary = Color.White,
    tertiaryContainer = Color.White, onTertiaryContainer = Color.Black,
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceDim = Color.White, surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceVariant = CardGrey, onSurfaceVariant = Color(0xFF505050),
    surfaceTint = Color.Transparent,
    inverseSurface = Color.Black, inverseOnSurface = Color.White,
    inversePrimary = ButtonGreen,
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFFCCCCCC)
)

private val DarkColors = darkColorScheme(
    primary = ButtonGreen, onPrimary = Color.White,
    primaryContainer = Color.Black, onPrimaryContainer = Color.White,
    secondary = ButtonGreenHover, onSecondary = Color.White,
    secondaryContainer = Color.Black, onSecondaryContainer = Color.White,
    tertiary = Color.White, onTertiary = Color.Black,
    tertiaryContainer = Color.Black, onTertiaryContainer = Color.White,
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceDim = Color.Black, surfaceBright = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
    surfaceVariant = DarkCardGrey, onSurfaceVariant = Color(0xFFCCCCCC),
    surfaceTint = Color.Transparent,
    inverseSurface = Color.White, inverseOnSurface = Color.Black,
    inversePrimary = ButtonGreen,
    outline = Color(0xFF999999),
    outlineVariant = Color(0xFF555555)
)

@Composable
fun MacronomOconTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

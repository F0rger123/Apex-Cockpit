package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApexDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    tertiary = NeonRed,
    background = ApexBlack,
    onBackground = Color.White,
    surface = DarkGrayJet,
    onSurface = Color.White,
    outline = OutlinesCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark mode
    dynamicColor: Boolean = false, // Disable dynamic colors to keep precise luxury theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.PhosphorTheme

fun createEcoColorScheme(
    phosphorTheme: PhosphorTheme,
    pureBlackOled: Boolean
) = darkColorScheme(
    primary = phosphorTheme.primaryColor,
    onPrimary = Color.Black,
    primaryContainer = phosphorTheme.dimColor,
    onPrimaryContainer = phosphorTheme.primaryColor,
    secondary = phosphorTheme.primaryColor,
    onSecondary = Color.Black,
    secondaryContainer = OledElevatedSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningOrange,
    background = if (pureBlackOled) OledPitchBlack else OledCarbonSurface,
    onBackground = TextPrimary,
    surface = if (pureBlackOled) OledPitchBlack else OledCarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = if (pureBlackOled) Color(0xFF060908) else OledElevatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = phosphorTheme.dimColor,
    outlineVariant = OledCardBorder,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    phosphorTheme: PhosphorTheme = PhosphorTheme.EMERALD,
    pureBlackOled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = createEcoColorScheme(phosphorTheme, pureBlackOled)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

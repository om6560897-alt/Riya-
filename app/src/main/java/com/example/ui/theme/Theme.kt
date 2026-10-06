package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = OnRosePrimaryDark,
    primaryContainer = RosePrimaryContainerDark,
    onPrimaryContainer = OnRosePrimaryContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = OnCyanSecondaryDark,
    secondaryContainer = CyanSecondaryContainerDark,
    onSecondaryContainer = OnCyanSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = OnAmberTertiaryContainerDark,
    background = VelvetBackgroundDark,
    onBackground = OnVelvetBackgroundDark,
    surface = VelvetSurfaceDark,
    onSurface = OnVelvetSurfaceDark,
    surfaceVariant = VelvetSurfaceVariantDark,
    onSurfaceVariant = OnVelvetSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = RosePrimaryLight,
    onPrimary = OnRosePrimaryLight,
    primaryContainer = RosePrimaryContainerLight,
    onPrimaryContainer = OnRosePrimaryContainerLight,
    secondary = CyanSecondaryLight,
    onSecondary = OnCyanSecondaryLight,
    secondaryContainer = CyanSecondaryContainerLight,
    onSecondaryContainer = OnCyanSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = OnAmberTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = OnAmberTertiaryContainerLight,
    background = BlushBackgroundLight,
    onBackground = OnBlushBackgroundLight,
    surface = BlushSurfaceLight,
    onSurface = OnBlushSurfaceLight,
    surfaceVariant = BlushSurfaceVariantLight,
    onSurfaceVariant = OnBlushSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

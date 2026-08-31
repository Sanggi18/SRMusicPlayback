package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CleanPrimaryDark,
    onPrimary = CleanOnPrimaryDark,
    primaryContainer = CleanPrimaryContainerDark,
    onPrimaryContainer = CleanOnPrimaryContainerDark,
    secondary = CleanPrimaryDark,
    onSecondary = CleanOnPrimaryDark,
    background = CleanBgDark,
    onBackground = CleanOnSurfaceDark,
    surface = CleanSurfaceDark,
    onSurface = CleanOnSurfaceDark,
    surfaceVariant = CleanBorderDark,
    onSurfaceVariant = CleanMutedDark,
    surfaceContainer = CleanSurfaceContainerDark,
    surfaceContainerHigh = CleanSurfaceContainerDark,
    outline = CleanBorderDark,
    outlineVariant = CleanOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CleanPrimaryLight,
    onPrimary = CleanOnPrimaryLight,
    primaryContainer = CleanPrimaryContainerLight,
    onPrimaryContainer = CleanOnPrimaryContainerLight,
    secondary = CleanPrimaryLight,
    onSecondary = CleanOnPrimaryLight,
    background = CleanBgLight,
    onBackground = CleanOnSurfaceLight,
    surface = CleanSurfaceLight,
    onSurface = CleanOnSurfaceLight,
    surfaceVariant = CleanBorderLight,
    onSurfaceVariant = CleanMutedLight,
    surfaceContainer = CleanSurfaceContainerLight,
    surfaceContainerHigh = CleanSurfaceContainerLight,
    outline = CleanBorderLight,
    outlineVariant = CleanOutlineVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize Clean Minimalism branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


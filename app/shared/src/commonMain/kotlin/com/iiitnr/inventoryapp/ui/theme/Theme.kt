package com.iiitnr.inventoryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = IIITNRPrimaryDark,
        onPrimary = IIITNROnSurface,
        primaryContainer = IIITNRPrimaryContainerDark,
        secondary = IIITNRSecondaryDark,
        tertiary = InventoryAmberDark,
        error = IIITNRErrorDark,
        background = IIITNRSurfaceDark,
        onBackground = IIITNROnSurfaceDark,
        surface = IIITNRSurfaceDark,
        onSurface = IIITNROnSurfaceDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = IIITNRPrimary,
        onPrimary = IIITNRSurface,
        primaryContainer = IIITNRPrimaryContainer,
        secondary = IIITNRSecondary,
        tertiary = InventoryAmber,
        error = IIITNRError,
        background = IIITNRSurface,
        onBackground = IIITNROnSurface,
        surface = IIITNRSurface,
        onSurface = IIITNROnSurface,
    )

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IIITNRTypography,
        content = content,
    )
}

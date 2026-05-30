package com.iiitnr.inventoryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = InventoryBlueDark,
        primaryContainer = InventoryBlueContainerDark,
        secondary = InventoryTealDark,
        tertiary = InventoryAmberDark,
        background = InventoryBackgroundDark,
        surface = InventorySurfaceDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = InventoryBlue,
        primaryContainer = InventoryBlueContainer,
        secondary = InventoryTeal,
        tertiary = InventoryAmber,
        background = InventoryBackground,
        surface = InventorySurface,
    )

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

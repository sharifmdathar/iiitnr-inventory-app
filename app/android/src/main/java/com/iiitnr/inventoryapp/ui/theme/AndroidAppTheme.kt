package com.iiitnr.inventoryapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
fun IIITNRInventoryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
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
        content = content,
    )
}

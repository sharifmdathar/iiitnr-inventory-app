package com.iiitnr.inventoryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.iiitnr.inventoryapp.utils.LocalToday
import kotlinx.datetime.LocalDateTime

@Immutable
data class InventoryColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val danger: Color,
    val neutral: Color,
    val admin: Color,
    val faculty: Color,
    val student: Color,
    val la: Color,
    val pending: Color,
    val actionPurple: Color,
    val actionCyan: Color,
)

val LocalInventoryColors =
    staticCompositionLocalOf {
        InventoryColors(
            success = Color.Unspecified,
            warning = Color.Unspecified,
            info = Color.Unspecified,
            danger = Color.Unspecified,
            neutral = Color.Unspecified,
            admin = Color.Unspecified,
            faculty = Color.Unspecified,
            student = Color.Unspecified,
            la = Color.Unspecified,
            pending = Color.Unspecified,
            actionPurple = Color.Unspecified,
            actionCyan = Color.Unspecified,
        )
    }

val MaterialTheme.inventoryColors: InventoryColors
    @Composable @ReadOnlyComposable
    get() = LocalInventoryColors.current

val DarkInventoryColors =
    InventoryColors(
        success = SemanticSuccessDark,
        warning = SemanticWarningDark,
        info = SemanticInfoDark,
        danger = SemanticDangerDark,
        neutral = SemanticNeutralDark,
        admin = RoleAdminDark,
        faculty = RoleFacultyDark,
        student = RoleStudentDark,
        la = RoleLADark,
        pending = RolePendingDark,
        actionPurple = ActionPurpleDark,
        actionCyan = ActionCyanDark,
    )

val LightInventoryColors =
    InventoryColors(
        success = SemanticSuccess,
        warning = SemanticWarning,
        info = SemanticInfo,
        danger = SemanticDanger,
        neutral = SemanticNeutral,
        admin = RoleAdmin,
        faculty = RoleFaculty,
        student = RoleStudent,
        la = RoleLA,
        pending = RolePending,
        actionPurple = ActionPurple,
        actionCyan = ActionCyan,
    )

val DarkColorScheme =
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

val LightColorScheme =
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
    val inventoryColors = if (darkTheme) DarkInventoryColors else LightInventoryColors
    val today =
        remember {
            LocalDateTime.parse("2026-08-12T00:00:00").date
        }

    CompositionLocalProvider(
        LocalInventoryColors provides inventoryColors,
        LocalToday provides today,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IIITNRTypography,
            content = content,
        )
    }
}

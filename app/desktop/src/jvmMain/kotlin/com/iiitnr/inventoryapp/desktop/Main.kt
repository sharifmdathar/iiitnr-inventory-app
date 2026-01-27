package com.iiitnr.inventoryapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.theme.AppTheme

fun main() = application {
    val tokenManager = createTokenManager()
    Window(
        onCloseRequest = ::exitApplication,
        title = "IIITNR Inventory App"
    ) {
        AppTheme {
            App(
                tokenManager = tokenManager,
                onGoogleSignInClick = { callback ->
                    callback(null)
                }
            )
        }
    }
}

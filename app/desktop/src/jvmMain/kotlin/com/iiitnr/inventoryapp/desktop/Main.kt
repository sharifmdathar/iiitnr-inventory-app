package com.iiitnr.inventoryapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App

fun main() = application {
    val tokenManager = createTokenManager()
    Window(
        onCloseRequest = ::exitApplication,
        title = "IIITNR Inventory App"
    ) {
        App(tokenManager = tokenManager)
    }
}

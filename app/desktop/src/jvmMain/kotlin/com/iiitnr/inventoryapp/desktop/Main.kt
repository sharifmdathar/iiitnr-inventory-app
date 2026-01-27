package com.iiitnr.inventoryapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.iiitnr.inventoryapp.data.auth.GoogleDesktopSignInHelper
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.theme.AppTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    val tokenManager = createTokenManager()

    val desktopClientId = System.getenv("GOOGLE_DESKTOP_CLIENT_ID") ?: ""
    val desktopClientSecret = System.getenv("GOOGLE_DESKTOP_CLIENT_SECRET")
    val redirectUri =
        System.getenv("GOOGLE_DESKTOP_REDIRECT_URI") ?: "http://127.0.0.1:5173/callback"

    val httpClient = HttpClient(CIO)
    val googleHelper = GoogleDesktopSignInHelper(
        clientId = desktopClientId,
        clientSecret = desktopClientSecret,
        redirectUri = redirectUri,
        httpClient = httpClient,
    )
    val scope = CoroutineScope(Dispatchers.IO)

    Window(
        onCloseRequest = ::exitApplication,
        title = "IIITNR Inventory App"
    ) {
        AppTheme {
            App(
                tokenManager = tokenManager,
                onGoogleSignInClick = { callback ->
                    scope.launch {
                        val idToken = googleHelper.signIn()
                        callback(idToken)
                    }
                }
            )
        }
    }
}

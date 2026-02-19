package com.iiitnr.inventoryapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.iiitnr.inventoryapp.data.auth.GoogleDesktopSignInHelper
import com.iiitnr.inventoryapp.data.cache.DriverFactory
import com.iiitnr.inventoryapp.data.cache.createComponentsCache
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.platform.exportComponentsCsvDesktop
import com.iiitnr.inventoryapp.ui.theme.AppTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    val tokenManager = createTokenManager()
    val componentsCache = createComponentsCache(DriverFactory())

    val properties = try {
        val classLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        val stream = classLoader.getResourceAsStream("google-desktop-config.properties")
        stream?.use { java.util.Properties().apply { load(it) } } ?: java.util.Properties()
    } catch (_: Exception) {
        java.util.Properties()
    }

    val desktopClientId = properties.getProperty("google.desktop.client.id")?.takeIf { it.isNotBlank() }
        ?: System.getenv("GOOGLE_DESKTOP_CLIENT_ID").orEmpty()
    val desktopClientSecret = properties.getProperty("google.desktop.client.secret")?.takeIf { it.isNotBlank() }
        ?: System.getenv("GOOGLE_DESKTOP_CLIENT_SECRET")
    val redirectUri = properties.getProperty("google.desktop.redirect.uri")?.takeIf { it.isNotBlank() }
        ?: System.getenv("GOOGLE_DESKTOP_REDIRECT_URI") ?: "http://127.0.0.1:5173/callback"

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
        title = "IIITNR Inventory App",
    ) {
        AppTheme {
            App(
                tokenManager = tokenManager,
                componentsCache = componentsCache,
                onGoogleSignInClick = { callback ->
                    scope.launch {
                        val idToken = googleHelper.signIn()
                        callback(idToken)
                    }
                },
                onExportComponentsCsv = { csvContent ->
                    exportComponentsCsvDesktop(
                        filename = "components.csv",
                        content = csvContent,
                    )
                },
            )
        }
    }
}

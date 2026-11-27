package com.iiitnr.inventoryapp.utils

import java.awt.Desktop
import java.net.URI

@Suppress("TooGenericExceptionCaught")
private class DesktopUrlLauncher : IntentLauncher {
    override fun launchUpi(uri: String): LaunchResult = LaunchResult.NoAppAvailable

    override fun launchUrl(url: String): LaunchResult {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return LaunchResult.NoAppAvailable
        }
        return try {
            Desktop.getDesktop().browse(URI(url))
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Error(e.message ?: "Could not open a browser")
        }
    }
}

actual fun createIntentLauncher(): IntentLauncher = DesktopUrlLauncher()

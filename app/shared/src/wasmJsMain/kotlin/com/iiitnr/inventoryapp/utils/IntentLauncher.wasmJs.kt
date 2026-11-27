package com.iiitnr.inventoryapp.utils

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private class WasmJsIntentLauncher : IntentLauncher {
    override fun launchUpi(uri: String): LaunchResult = LaunchResult.NoAppAvailable

    override fun launchUrl(url: String): LaunchResult =
        try {
            openInNewTab(url)
            LaunchResult.Launched
        } catch (_: Throwable) {
            LaunchResult.Error("Could not open the link")
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UnusedParameter") // used inside the js() body, which detekt cannot see
private fun openInNewTab(url: String) {
    js("window.open(url, '_blank', 'noopener')")
}

actual fun createIntentLauncher(): IntentLauncher = WasmJsIntentLauncher()

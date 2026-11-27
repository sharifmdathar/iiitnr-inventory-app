package com.iiitnr.inventoryapp.utils

sealed class LaunchResult {
    data object Launched : LaunchResult()

    data object NoAppAvailable : LaunchResult()

    data class Error(
        val message: String,
    ) : LaunchResult()
}

interface IntentLauncher {
    fun launchUpi(uri: String): LaunchResult

    fun launchUrl(url: String): LaunchResult
}

expect fun createIntentLauncher(): IntentLauncher

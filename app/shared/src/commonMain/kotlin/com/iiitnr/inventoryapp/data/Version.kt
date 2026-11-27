package com.iiitnr.inventoryapp.data

import com.iiitnr.inventoryapp.data.api.ApiClient

object Version {
    const val CURRENT_VERSION = GeneratedVersion.CURRENT_VERSION

    fun isVersionNewer(
        current: String,
        latest: String,
    ): Boolean {
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}

suspend fun checkForUpdate(): String? {
    val serverVersion = ApiClient.versionApiService.fetchServerVersion().version
    return if (Version.isVersionNewer(Version.CURRENT_VERSION, serverVersion)) serverVersion else null
}

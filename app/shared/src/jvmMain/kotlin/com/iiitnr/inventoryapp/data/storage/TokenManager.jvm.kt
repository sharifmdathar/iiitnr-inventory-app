package com.iiitnr.inventoryapp.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

class DesktopTokenManager : TokenManager {
    private val prefs = Preferences.userRoot().node("com.iiitnr.inventoryapp")
    private val TOKEN_KEY = "auth_token"

    private val _token = MutableStateFlow<String?>(prefs.get(TOKEN_KEY, null))

    override val token: Flow<String?> = _token.asStateFlow()

    override suspend fun saveToken(token: String) {
        prefs.put(TOKEN_KEY, token)
        prefs.sync()
        _token.value = token
    }

    override suspend fun clearToken() {
        prefs.remove(TOKEN_KEY)
        prefs.sync()
        _token.value = null
    }
}

actual fun createTokenManager(): TokenManager = DesktopTokenManager()

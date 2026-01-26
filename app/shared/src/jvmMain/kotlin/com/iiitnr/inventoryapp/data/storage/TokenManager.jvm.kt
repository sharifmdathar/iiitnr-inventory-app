package com.iiitnr.inventoryapp.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

class DesktopTokenManager : TokenManager {
    private val prefs = Preferences.userRoot().node("com.iiitnr.inventoryapp")
    private val _tokenKey = "auth_token"

    private val _token = MutableStateFlow<String?>(prefs.get(_tokenKey, null))

    override val token: Flow<String?> = _token.asStateFlow()

    override suspend fun saveToken(token: String) {
        prefs.put(_tokenKey, token)
        prefs.sync()
        _token.value = token
    }

    override suspend fun clearToken() {
        prefs.remove(_tokenKey)
        prefs.sync()
        _token.value = null
    }
}

actual fun createTokenManager(): TokenManager = DesktopTokenManager()

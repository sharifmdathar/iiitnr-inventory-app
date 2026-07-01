package com.iiitnr.inventoryapp.data.storage

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmTokenManager : TokenManager {
    private val _token = MutableStateFlow(window.localStorage.getItem("auth_token"))
    override val token: Flow<String?> = _token.asStateFlow()

    override suspend fun saveToken(token: String) {
        window.localStorage.setItem("auth_token", token)
        _token.value = token
    }

    override suspend fun clearToken() {
        window.localStorage.removeItem("auth_token")
        _token.value = null
    }
}

actual fun createTokenManager(): TokenManager = WasmTokenManager()

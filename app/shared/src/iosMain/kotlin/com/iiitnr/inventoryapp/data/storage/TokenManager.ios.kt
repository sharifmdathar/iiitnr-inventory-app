package com.iiitnr.inventoryapp.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosTokenManager : TokenManager {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val tokenKey = "auth_token"
    
    // We use a MutableStateFlow to emit updates.
    // Initial value is read from NSUserDefaults.
    private val _token = MutableStateFlow(userDefaults.stringForKey(tokenKey))
    override val token: Flow<String?> = _token.asStateFlow()

    override suspend fun saveToken(token: String) {
        userDefaults.setObject(token, forKey = tokenKey)
        _token.value = token
    }

    override suspend fun clearToken() {
        userDefaults.removeObjectForKey(tokenKey)
        _token.value = null
    }
}

actual fun createTokenManager(): TokenManager = IosTokenManager()

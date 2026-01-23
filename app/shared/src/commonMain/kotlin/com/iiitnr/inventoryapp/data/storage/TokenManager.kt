package com.iiitnr.inventoryapp.data.storage

import kotlinx.coroutines.flow.Flow

interface TokenManager {
    val token: Flow<String?>
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

expect fun createTokenManager(): TokenManager

package com.iiitnr.inventoryapp.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class AndroidTokenManager(
    private val context: Context,
) : TokenManager {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    override val token: Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }

    override suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    override suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}

actual fun createTokenManager(): TokenManager =
    throw NotImplementedError("TokenManager must be created with Context on Android")

fun createTokenManager(context: Context): TokenManager = AndroidTokenManager(context)

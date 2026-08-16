package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.GoogleSignInRequest
import com.iiitnr.inventoryapp.data.models.LoginRequest
import com.iiitnr.inventoryapp.data.models.RegisterRequest
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.utils.toAppError
import kotlinx.coroutines.launch

class AuthViewModel(
    private val tokenManager: TokenManager,
) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.authApiService.login(LoginRequest(email, password))
                tokenManager.saveToken(response.token)
                onSuccess()
            } catch (e: Throwable) {
                errorMessage = "Login failed: ${e.toAppError().message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response =
                    ApiClient.authApiService.register(
                        RegisterRequest(email, password, name),
                    )
                tokenManager.saveToken(response.token)
                onSuccess()
            } catch (e: Throwable) {
                errorMessage = "Registration failed: ${e.toAppError().message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun googleSignIn(
        idToken: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.authApiService.signInWithGoogle(GoogleSignInRequest(idToken))
                tokenManager.saveToken(response.token)
                onSuccess()
            } catch (e: Throwable) {
                errorMessage = "Google Sign-In failed: ${e.toAppError().message}"
            } finally {
                isLoading = false
            }
        }
    }
}

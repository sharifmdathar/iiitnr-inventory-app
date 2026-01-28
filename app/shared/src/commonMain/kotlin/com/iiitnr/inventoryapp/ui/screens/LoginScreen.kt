package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.GoogleSignInRequest
import com.iiitnr.inventoryapp.data.models.LoginRequest
import com.iiitnr.inventoryapp.data.storage.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    tokenManager: TokenManager,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleSignInClick: ((String?) -> Unit) -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val passwordFocusRequester = remember { FocusRequester() }

    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val authResponse =
                    ApiClient.authApiService.login(LoginRequest(email.trim(), password))
                tokenManager.saveToken(authResponse.token)
                onLoginSuccess()
            } catch (e: Exception) {
                errorMessage =
                    when {
                        e.message?.contains(
                            "401",
                        ) == true ||
                                e.message?.contains("Unauthorized") == true -> "Invalid email or password"

                        e.message?.contains(
                            "400",
                        ) == true ||
                                e.message?.contains("Bad Request") == true -> "Invalid request. Please check your input."

                        e.message?.contains(
                            "Network",
                        ) == true ||
                                e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                        else -> "Login failed: ${e.message ?: "Please check your credentials"}"
                    }
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "IIITNR Inventory App",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions =
                KeyboardActions(
                    onNext = {
                        if (password.isNotBlank()) {
                            performLogin()
                        } else {
                            passwordFocusRequester.requestFocus()
                        }
                    },
                ),
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .focusRequester(passwordFocusRequester),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = { performLogin() },
                ),
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        Button(
            onClick = { performLogin() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var isGoogleLoading by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = {
                isGoogleLoading = true
                onGoogleSignInClick { idToken ->
                    isGoogleLoading = false
                    if (idToken != null) {
                        scope.launch {
                            try {
                                val authResponse =
                                    ApiClient.authApiService.signInWithGoogle(
                                        GoogleSignInRequest(idToken),
                                    )
                                tokenManager.saveToken(authResponse.token)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                val errorMsg = e.message ?: "Unknown error"
                                errorMessage =
                                    when {
                                        errorMsg.contains("403") -> {
                                            when {
                                                errorMsg.contains("email addresses are allowed") -> {
                                                    errorMsg
                                                        .substringAfter(": ")
                                                        .takeIf { it.isNotBlank() }
                                                        ?: "Only @iiitnr.edu.in email addresses are allowed."
                                                }

                                                else ->
                                                    "Access denied. Only @iiitnr.edu.in email addresses are allowed."
                                            }
                                        }

                                        errorMsg.contains("401") -> "Google Sign-In failed: Unauthorized"
                                        errorMsg.contains("400") -> {
                                            when {
                                                errorMsg.contains(
                                                    "audience",
                                                ) -> "Token verification failed. Check backend configuration."

                                                errorMsg.contains(
                                                    "email not verified",
                                                ) -> "Google account email is not verified"

                                                else ->
                                                    errorMsg
                                                        .substringAfter(": ")
                                                        .takeIf { it.isNotBlank() }
                                                        ?: "Google Sign-In failed"
                                            }
                                        }

                                        else ->
                                            errorMsg.substringAfter(": ").takeIf { it.isNotBlank() }
                                                ?: "Google Sign-In failed"
                                    }
                            }
                        }
                    } else {
                        errorMessage = "Google Sign-In was cancelled"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && !isGoogleLoading,
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AsyncImage(
                        model = "https://www.google.com/images/branding/googleg/1x/googleg_standard_color_128dp.png",
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }
    }
}

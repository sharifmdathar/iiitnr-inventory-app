package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.preferences.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onNavigateToComponents: () -> Unit = {}
) {
    var userData by remember { mutableStateOf<com.iiitnr.inventoryapp.data.models.User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    try {
                        val response = ApiClient.authApiService.getMe("Bearer $token")
                        if (response.isSuccessful && response.body() != null) {
                            userData = response.body()!!.user
                            isLoading = false
                        } else {
                            errorMessage = "Failed to load user data"
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        errorMessage = "Network error: ${e.message}"
                        isLoading = false
                    }
                } else {
                    errorMessage = "No authentication token found"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        tokenManager.clearToken()
                        onLogout()
                    }
                }
            ) {
                Text("Logout")
            }
        }

        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "", color = MaterialTheme.colorScheme.error
                )
            }

            userData != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "User Information",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        HorizontalDivider()

                        InfoRow("ID", userData!!.id)
                        InfoRow("Email", userData!!.email)
                        InfoRow("Name", userData!!.name ?: "Not provided")
                        InfoRow("Role", userData!!.role)
                    }
                }

                val userRole = userData!!.role.uppercase()
                val isAdminOrTA = userRole == "TA" || userRole == "ADMIN"
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToComponents,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAdminOrTA) "Manage Components" else "View Components")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}

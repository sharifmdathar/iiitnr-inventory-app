package com.iiitnr.inventoryapp.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.screens.LoginScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun App(tokenManager: TokenManager) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                scope.launch {
                    val token = tokenManager.token.first()
                    startDestination = if (token != null) "components" else "login"
                }
            }

            startDestination?.let { destination ->
                NavHost(
                    navController = navController,
                    startDestination = destination
                ) {
                    composable("login") {
                        LoginScreen(
                            tokenManager = tokenManager,
                            onLoginSuccess = {
                                navController.navigate("components") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }
                    composable("register") {
                        // TODO: Add RegisterScreen
                        androidx.compose.material3.Text("Register Screen - Coming Soon")
                    }
                    composable("components") {
                        // TODO: Add ComponentsScreen
                        androidx.compose.material3.Text("Components Screen - Coming Soon")
                    }
                    composable("home") {
                        // TODO: Add HomeScreen
                        androidx.compose.material3.Text("Home Screen - Coming Soon")
                    }
                    composable("requests") {
                        // TODO: Add RequestsScreen
                        androidx.compose.material3.Text("Requests Screen - Coming Soon")
                    }
                }
            }
        }
    }
}

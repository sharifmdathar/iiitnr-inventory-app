package com.iiitnr.inventoryapp.shared

import androidx.compose.foundation.layout.fillMaxSize
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
import com.iiitnr.inventoryapp.ui.screens.ComponentsScreen
import com.iiitnr.inventoryapp.ui.screens.LoginScreen
import com.iiitnr.inventoryapp.ui.screens.ProfileScreen
import com.iiitnr.inventoryapp.ui.screens.RegisterScreen
import com.iiitnr.inventoryapp.ui.screens.RequestsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun App(
    tokenManager: TokenManager,
    onGoogleSignInClick: ((String?) -> Unit) -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
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
                startDestination = destination,
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
                        },
                        onGoogleSignInClick = onGoogleSignInClick,
                    )
                }
                composable("register") {
                    RegisterScreen(tokenManager = tokenManager, onRegisterSuccess = {
                        navController.navigate("components") {
                            popUpTo("register") { inclusive = true }
                        }
                    }, onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    })
                }
                composable("components") {
                    ComponentsScreen(tokenManager = tokenManager, onNavigateToRequests = {
                        navController.navigate("requests")
                    }, onNavigateToHome = {
                        navController.navigate("home")
                    })
                }
                composable("home") {
                    ProfileScreen(tokenManager = tokenManager, onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }, onNavigateBack = {
                        navController.popBackStack()
                    }, onNavigateToRequests = {
                        navController.navigate("requests")
                    })
                }
                composable("requests") {
                    RequestsScreen(tokenManager = tokenManager, onNavigateBack = {
                        navController.popBackStack()
                    }, onNavigateToComponents = {
                        navController.navigate("components") {
                            popUpTo("requests") { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}

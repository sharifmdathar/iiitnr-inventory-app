package com.iiitnr.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.iiitnr.inventoryapp.data.preferences.TokenManager
import com.iiitnr.inventoryapp.ui.screens.ComponentsScreen
import com.iiitnr.inventoryapp.ui.screens.HomeScreen
import com.iiitnr.inventoryapp.ui.screens.LoginScreen
import com.iiitnr.inventoryapp.ui.screens.RegisterScreen
import com.iiitnr.inventoryapp.ui.screens.RequestsScreen
import com.iiitnr.inventoryapp.ui.theme.IIITNRInventoryAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()
        setContent {
            IIITNRInventoryAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthNavigation(tokenManager = tokenManager)
                }
            }
        }
    }
}

@Composable
fun AuthNavigation(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = tokenManager.token.first()
            startDestination = if (token != null) "home" else "login"
        }
    }

    // Don't render NavHost until we know the start destination
    startDestination?.let { destination ->
        NavHost(
            navController = navController,
            startDestination = destination
        ) {
        composable("login") {
            LoginScreen(
                tokenManager = tokenManager,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                tokenManager = tokenManager,
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("home") {
            HomeScreen(
                tokenManager = tokenManager,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToComponents = {
                    navController.navigate("components")
                },
                onNavigateToRequests = {
                    navController.navigate("requests")
                }
            )
        }
            composable("components") {
                ComponentsScreen(
                    tokenManager = tokenManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("requests") {
                RequestsScreen(
                    tokenManager = tokenManager,
                    onNavigateBack = {
                        navController.popBackStack()
                }
            )
        }
        }
    }
}
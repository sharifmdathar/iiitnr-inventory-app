package com.iiitnr.inventoryapp.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.iiitnr.inventoryapp.data.api.AuthEvent
import com.iiitnr.inventoryapp.data.api.AuthEventManager
import com.iiitnr.inventoryapp.data.cache.ComponentsCache
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
    onExportComponentsCsv: ((String) -> Boolean)? = null,
    componentsCache: ComponentsCache? = null,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var startDestination by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            scope.launch {
                val token = tokenManager.token.first()
                startDestination = if (token != null) "components" else "login"
            }
            scope.launch {
                AuthEventManager.events.collect { event ->
                    if (event is AuthEvent.Unauthorized) {
                        tokenManager.clearToken()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Session expired. Please login again.",
                                duration = androidx.compose.material3.SnackbarDuration.Short,
                            )
                        }
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                            ComponentsScreen(
                                tokenManager = tokenManager,
                                componentsCache = componentsCache,
                                onNavigateToRequests = {
                                    navController.navigate("requests")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                },
                                onExportCsv = onExportComponentsCsv,
                            )
                        }
                        composable("profile") {
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
    }
}

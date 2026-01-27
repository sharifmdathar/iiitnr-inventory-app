package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.RequestItemPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.components.components.AddComponentFAB
import com.iiitnr.inventoryapp.ui.components.components.CartDialog
import com.iiitnr.inventoryapp.ui.components.components.CartFAB
import com.iiitnr.inventoryapp.ui.components.components.ComponentDialog
import com.iiitnr.inventoryapp.ui.components.components.ComponentsContent
import com.iiitnr.inventoryapp.ui.components.components.ComponentsTopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ComponentsScreen(
    tokenManager: TokenManager, onNavigateToRequests: () -> Unit, onNavigateToHome: () -> Unit
) {
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingComponent by remember { mutableStateOf<Component?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Component?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var cartQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showCartDialog by remember { mutableStateOf(false) }
    var cartError by remember { mutableStateOf<String?>(null) }
    var isSubmittingRequest by remember { mutableStateOf(false) }

    var facultyOptions by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedFacultyId by remember { mutableStateOf<String?>(null) }
    var projectTitle by remember { mutableStateOf("") }
    var isLoadingFaculty by remember { mutableStateOf(false) }
    var pendingRequestsCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val isFaculty = userRole?.uppercase() == "FACULTY"

    val isReadOnly = userRole?.let { role ->
        val roleUpper = role.uppercase()
        roleUpper != "TA" && roleUpper != "ADMIN"
    } ?: true

    val filteredComponents = if (searchQuery.isBlank()) {
        components
    } else {
        val query = searchQuery.lowercase().trim()
        components.filter { component ->
            component.name.lowercase().contains(query) || component.description?.lowercase()
                ?.contains(query) == true || component.category?.lowercase()
                ?.contains(query) == true || component.location?.lowercase()
                ?.contains(query) == true || component.availableQuantity.toString().contains(query)
        }
    }

    fun loadComponents() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    try {
                        val userResponse = ApiClient.authApiService.getMe("Bearer $token")
                        userRole = userResponse.user.role
                    } catch (_: Exception) {
                    }

                    val response = ApiClient.componentApiService.getComponents("Bearer $token")
                    components = response.components
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> "Session expired. Please login again."

                    e.message?.contains("Network") == true || e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                    else -> "Error: ${e.message ?: "Failed to load components"}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateCartQuantity(component: Component, delta: Int) {
        val current = cartQuantities[component.id] ?: 0
        val maxAllowed = component.availableQuantity
        val next = (current + delta).coerceIn(0, maxAllowed)
        cartQuantities = if (next == 0) {
            cartQuantities - component.id
        } else {
            cartQuantities + (component.id to next)
        }
    }

    fun submitRequest() {
        scope.launch {
            cartError = null
            isSubmittingRequest = true
            val cleanedItems =
                cartQuantities.filterValues { it > 0 }.map { (componentId, quantity) ->
                    RequestItemPayload(componentId = componentId, quantity = quantity)
                }

            if (cleanedItems.isEmpty()) {
                cartError = "Add at least one component to the request"
                isSubmittingRequest = false
                return@launch
            }
            if (selectedFacultyId == null) {
                cartError = "Please select a target faculty"
                isSubmittingRequest = false
                return@launch
            }
            if (projectTitle.isBlank()) {
                cartError = "Please enter a project title"
                isSubmittingRequest = false
                return@launch
            }

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    ApiClient.requestApiService.createRequest(
                        "Bearer $token", CreateRequestPayload(
                            items = cleanedItems,
                            targetFacultyId = selectedFacultyId!!,
                            projectTitle = projectTitle.trim()
                        )
                    )
                    showCartDialog = false
                    cartQuantities = emptyMap()
                    cartError = null
                    selectedFacultyId = null
                    projectTitle = ""
                    onNavigateToRequests.invoke()
                } else {
                    cartError = "No authentication token"
                }
            } catch (e: Exception) {
                cartError = when {
                    e.message?.contains("400") == true || e.message?.contains("Bad Request") == true -> "Invalid request. Please check your input."

                    e.message?.contains("Network") == true || e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                    else -> "Error: ${e.message ?: "Failed to create request"}"
                }
            } finally {
                isSubmittingRequest = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadComponents()
    }

    LaunchedEffect(userRole) {
        if (userRole?.uppercase() != "FACULTY") return@LaunchedEffect
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val response = ApiClient.requestApiService.getRequests("Bearer $token", "PENDING")
            pendingRequestsCount = response.requests.size
        } catch (_: Exception) {
            pendingRequestsCount = 0
        }
    }

    LaunchedEffect(showCartDialog) {
        if (!showCartDialog) return@LaunchedEffect

        scope.launch {
            isLoadingFaculty = true
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getFaculty("Bearer $token")
                    facultyOptions = response.faculty
                } else {
                    facultyOptions = emptyList()
                }
            } catch (_: Exception) {
                facultyOptions = emptyList()
            } finally {
                isLoadingFaculty = false
            }
        }
    }

    Scaffold(topBar = {
        ComponentsTopBar(
            onNavigateToHome = onNavigateToHome,
            onNavigateToRequests = onNavigateToRequests,
            pendingRequestsCount = if (isFaculty) pendingRequestsCount else null
        )
    }, floatingActionButton = {
        when {
            cartQuantities.isNotEmpty() -> {
                CartFAB(
                    itemCount = cartQuantities.values.sum(), onClick = { showCartDialog = true })
            }

            !isReadOnly -> {
                AddComponentFAB(
                    onClick = {
                        editingComponent = null
                        showDialog = true
                    })
            }
        }
    }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                placeholder = "Search components...",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ComponentsContent(
                isLoading = isLoading,
                errorMessage = errorMessage,
                components = filteredComponents,
                allComponents = components,
                searchQuery = searchQuery,
                isReadOnly = isReadOnly,
                cartQuantities = cartQuantities,
                onRetry = { loadComponents() },
                onEdit = { component ->
                    editingComponent = component
                    showDialog = true
                },
                onDelete = { component ->
                    showDeleteDialog = component
                },
                onAddToCart = { component ->
                    updateCartQuantity(component, 1)
                },
                onUpdateCartQuantity = { component, delta ->
                    updateCartQuantity(component, delta)
                })
        }
    }

    if (showDialog && !isReadOnly) {
        ComponentDialog(
            component = editingComponent,
            onDismiss = { showDialog = false },
            onSave = { request ->
                scope.launch {
                    try {
                        val token = tokenManager.token.first()
                        if (token != null) {
                            if (editingComponent != null) {
                                ApiClient.componentApiService.updateComponent(
                                    "Bearer $token", editingComponent!!.id, request
                                )
                            } else {
                                ApiClient.componentApiService.createComponent(
                                    "Bearer $token", request
                                )
                            }
                            showDialog = false
                            editingComponent = null
                            loadComponents()
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message ?: "Failed to save component"}"
                    }
                }
            })
    }

    if (!isReadOnly) {
        showDeleteDialog?.let { component ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Component") },
                text = { Text("Are you sure you want to delete \"${component.name}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val token = tokenManager.token.first()
                                    if (token != null) {
                                        ApiClient.componentApiService.deleteComponent(
                                            "Bearer $token", component.id
                                        )
                                        showDeleteDialog = null
                                        loadComponents()
                                    }
                                } catch (e: Exception) {
                                    errorMessage =
                                        "Error: ${e.message ?: "Failed to delete component"}"
                                }
                            }
                        }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                })
        }
    }

    if (showCartDialog) {
        CartDialog(
            components = components,
            cartQuantities = cartQuantities,
            cartError = cartError,
            isSubmitting = isSubmittingRequest,
            facultyOptions = facultyOptions,
            selectedFacultyId = selectedFacultyId,
            isLoadingFaculty = isLoadingFaculty,
            onSelectFaculty = { selectedFacultyId = it },
            projectTitle = projectTitle,
            onProjectTitleChange = { projectTitle = it },
            onUpdateQuantity = { component, delta ->
                updateCartQuantity(component, delta)
            },
            onRemoveItem = { component ->
                cartQuantities = cartQuantities - component.id
            },
            onDismiss = {
                showCartDialog = false
                cartError = null
            },
            onSubmit = { submitRequest() })
    }
}

package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.cache.ComponentsCache
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.RequestItemPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.data.storage.TokenManager
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ComponentsViewModel(
    val tokenManager: TokenManager,
    private val componentsCache: ComponentsCache? = null,
) : ViewModel() {
    var components by mutableStateOf<List<Component>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var userRole by mutableStateOf<UserRole?>(null)
        private set
    var cartQuantities by mutableStateOf<Map<String, Int>>(emptyMap())

    var searchQuery by mutableStateOf("")
    var categoryFilter by mutableStateOf<String?>(null)
    var locationFilter by mutableStateOf<String?>(null)

    var showCartDialog by mutableStateOf(false)
    var cartError by mutableStateOf<String?>(null)
    var isSubmittingRequest by mutableStateOf(false)
        private set

    var facultyOptions by mutableStateOf<List<User>>(emptyList())
        private set
    var selectedFacultyId by mutableStateOf<String?>(null)
    var projectTitle by mutableStateOf("")
    var isLoadingFaculty by mutableStateOf(false)
        private set
    var pendingRequestsCount by mutableStateOf(0)
        private set

    var editingComponent by mutableStateOf<Component?>(null)
    var showDeleteDialog by mutableStateOf<Component?>(null)

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    val filteredComponents: List<Component>
        get() {
            val query = searchQuery.trim().lowercase()
            return components.filter { component ->
                val matchesQuery =
                    query.isEmpty() ||
                        component.name.lowercase().contains(query) ||
                        component.description
                            ?.lowercase()
                            ?.contains(query) == true ||
                        component.category
                            ?.lowercase()
                            ?.contains(query) == true ||
                        component.location?.lowercase()?.contains(query) == true

                val matchesCategory = categoryFilter?.let { component.category == it } ?: true
                val matchesLocation = locationFilter?.let { component.location == it } ?: true

                matchesQuery && matchesCategory && matchesLocation
            }
        }

    init {
        loadInitialData()
        startPolling()
        observeCache()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadUserData()
            loadComponents(pollingMode = false)
            loadFaculty()
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(8000.milliseconds)
                if (errorMessage == null && !isLoading && !isRefreshing) {
                    loadComponents(pollingMode = true)
                }
            }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            componentsCache?.componentsFlow()?.collect { cached ->
                components = cached
                if (cached.isNotEmpty()) {
                    isLoading = false
                }
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.authApiService.getMe("Bearer $token")
                    userRole = response.user.role
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadComponents(pollingMode: Boolean = false) {
        viewModelScope.launch {
            if (pollingMode && isRefreshing) return@launch

            if (pollingMode) {
                isRefreshing = true
            } else {
                if (components.isEmpty()) {
                    isLoading = true
                } else {
                    isRefreshing = true
                }
                errorMessage = null
            }

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.componentApiService.getComponents("Bearer $token")
                    if (componentsCache != null) {
                        componentsCache.save(response.components, null)
                    } else {
                        components = response.components
                    }
                    errorMessage = null
                }
            } catch (e: Throwable) {
                val isAuthError = e is ResponseException && e.response.status == HttpStatusCode.Unauthorized
                if (isAuthError) return@launch

                if (!pollingMode) {
                    if (components.isEmpty()) {
                        errorMessage =
                            when {
                                e.message?.contains("Unable to resolve host") == true ||
                                    e.message?.contains("Network") == true ->
                                    "Network error. Please check your internet connection."

                                else -> "Error: ${e.message ?: "Failed to load components"}"
                            }
                    } else {
                        _snackbarMessages.emit("Network error: Using cached data")
                    }
                }
            } finally {
                if (pollingMode) {
                    isRefreshing = false
                } else {
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    private fun loadFaculty() {
        viewModelScope.launch {
            isLoadingFaculty = true
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getFaculty("Bearer $token")
                    facultyOptions = response.faculty
                }
            } catch (_: Exception) {
            } finally {
                isLoadingFaculty = false
            }
        }
    }

    fun updateCartQuantity(
        component: Component,
        delta: Int,
    ) {
        val currentQty = cartQuantities[component.id] ?: 0
        val newQty = (currentQty + delta).coerceIn(0, component.availableQuantity)

        cartQuantities =
            if (newQty > 0) {
                cartQuantities + (component.id to newQty)
            } else {
                cartQuantities - component.id
            }
    }

    fun removeFromCart(componentId: String) {
        cartQuantities = cartQuantities - componentId
    }

    fun clearCart() {
        cartQuantities = emptyMap()
    }

    fun submitRequest() {
        val facultyId = selectedFacultyId
        val title = projectTitle.trim()

        if (facultyId == null) {
            cartError = "Please select a faculty"
            return
        }
        if (title.isBlank()) {
            cartError = "Please enter a project title"
            return
        }

        viewModelScope.launch {
            isSubmittingRequest = true
            cartError = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val items =
                        cartQuantities.map { (id, qty) ->
                            RequestItemPayload(componentId = id, quantity = qty)
                        }
                    ApiClient.requestApiService.createRequest(
                        token = "Bearer $token",
                        payload =
                            CreateRequestPayload(
                                items = items,
                                targetFacultyId = facultyId,
                                projectTitle = title,
                            ),
                    )
                    clearCart()
                    showCartDialog = false
                    _snackbarMessages.emit("Request submitted successfully")
                    loadComponents(pollingMode = true)
                }
            } catch (e: Throwable) {
                cartError = "Failed to submit request: ${e.message}"
            } finally {
                isSubmittingRequest = false
            }
        }
    }

    fun deleteComponent(component: Component) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    ApiClient.componentApiService.deleteComponent("Bearer $token", component.id)
                    loadComponents(pollingMode = true)
                    _snackbarMessages.emit("Component deleted successfully")
                }
            } catch (e: Throwable) {
                _snackbarMessages.emit("Failed to delete component: ${e.message}")
            } finally {
                showDeleteDialog = null
            }
        }
    }

    fun saveComponent(
        request: ComponentRequest,
        onDismiss: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    if (editingComponent != null) {
                        ApiClient.componentApiService.updateComponent(
                            "Bearer $token",
                            editingComponent!!.id,
                            request,
                        )
                    } else {
                        ApiClient.componentApiService.createComponent(
                            "Bearer $token",
                            request,
                        )
                    }
                    onDismiss()
                    editingComponent = null
                    loadComponents()
                }
            } catch (e: Throwable) {
                _snackbarMessages.emit("Failed to save component: ${e.message}")
            }
        }
    }

    fun uploadImage(
        bytes: ByteArray,
        filename: String,
    ) {
        val componentId = editingComponent?.id ?: return
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response =
                        ApiClient.componentApiService.uploadImage(
                            "Bearer $token",
                            componentId,
                            bytes,
                            filename,
                        )
                    editingComponent = response.component
                    loadComponents(pollingMode = true)
                    _snackbarMessages.emit("Image uploaded successfully")
                }
            } catch (e: Throwable) {
                _snackbarMessages.emit("Failed to upload image: ${e.message}")
            }
        }
    }

    fun removeImage() {
        val componentId = editingComponent?.id ?: return
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    ApiClient.componentApiService.deleteImage("Bearer $token", componentId)
                    loadComponents()
                    _snackbarMessages.emit("Image removed successfully")
                }
            } catch (e: Throwable) {
                _snackbarMessages.emit("Failed to remove image: ${e.message}")
            }
        }
    }
}

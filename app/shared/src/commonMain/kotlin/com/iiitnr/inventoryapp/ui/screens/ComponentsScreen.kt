package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.iiitnr.inventoryapp.data.cache.ComponentsCache
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.ComponentCategory
import com.iiitnr.inventoryapp.data.models.ComponentLocation
import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.RequestItemPayload
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.components.components.AddComponentFAB
import com.iiitnr.inventoryapp.ui.components.components.CartDialog
import com.iiitnr.inventoryapp.ui.components.components.CartFAB
import com.iiitnr.inventoryapp.ui.components.components.ComponentDialog
import com.iiitnr.inventoryapp.ui.components.components.ComponentsContent
import com.iiitnr.inventoryapp.ui.components.components.ComponentsTopBar
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ComponentsScreen(
    tokenManager: TokenManager,
    componentsCache: ComponentsCache? = null,
    onNavigateToRequests: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onExportCsv: ((String) -> Boolean)? = null,
) {
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var isLoading by remember(componentsCache) { mutableStateOf(componentsCache == null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingComponent by remember { mutableStateOf<Component?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Component?>(null) }
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var locationFilter by remember { mutableStateOf<String?>(null) }
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
    val snackbarHostState = remember { SnackbarHostState() }

    val isFaculty = userRole == UserRole.FACULTY

    val canExportCsv =
        userRole?.let { role ->
            role == UserRole.ADMIN || role == UserRole.LA || role == UserRole.FACULTY
        } ?: false

    val isReadOnly =
        userRole?.let { role ->
            role != UserRole.LA && role != UserRole.ADMIN
        } ?: true

    fun exportComponentsCsv() {
        if (!canExportCsv || onExportCsv == null || components.isEmpty()) return

        val csvHeader = "Name,Description,Category,Location,Total Quantity,Available Quantity"
        val csvRows =
            components.map { c ->
                fun escapeCsv(value: String?): String {
                    if (value.isNullOrEmpty()) return ""
                    val escaped = value.replace("\"", "\"\"")
                    return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
                        "\"$escaped\""
                    } else {
                        escaped
                    }
                }
                listOf(
                    escapeCsv(c.name),
                    escapeCsv(c.description),
                    escapeCsv(c.category?.replace("_", " ")),
                    escapeCsv(c.location?.replace("_", " ")),
                    c.totalQuantity.toString(),
                    c.availableQuantity.toString(),
                ).joinToString(",")
            }
        val csvContent = (listOf(csvHeader) + csvRows).joinToString("\n")
        val success = onExportCsv.invoke(csvContent)

        scope.launch {
            snackbarHostState.showSnackbar(
                message =
                    if (success) {
                        "Exported components.csv successfully"
                    } else {
                        "Failed to export components.csv"
                    },
            )
        }
    }

    val filteredComponents =
        components.filter { component ->
            val query = searchQuery.trim().lowercase()
            val matchesSearch =
                query.isBlank() ||
                    listOfNotNull(
                        component.name,
                        component.description,
                        component.category,
                        component.location,
                    ).any { it.contains(query, ignoreCase = true) }

            val matchesCategory =
                categoryFilter?.let { filter ->
                    component.category?.replace("_", " ")?.equals(filter, ignoreCase = true) ?: false
                } ?: true

            val matchesLocation =
                locationFilter?.let { filter ->
                    component.location?.replace("_", " ")?.equals(filter, ignoreCase = true) ?: false
                } ?: true

            matchesSearch && matchesCategory && matchesLocation
        }

    fun loadComponents(pollingMode: Boolean = false) {
        scope.launch {
            if (pollingMode && isRefreshing) {
                return@launch
            }

            if (pollingMode || componentsCache != null) {
                isRefreshing = true
            } else {
                isLoading = true
            }
            if (!pollingMode) errorMessage = null

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    try {
                        val userResponse = ApiClient.authApiService.getMe("Bearer $token")
                        userRole = userResponse.user.role
                    } catch (_: Exception) {
                    }

                    val response = ApiClient.componentApiService.getComponents("Bearer $token")
                    if (componentsCache != null) {
                        componentsCache.save(response.components, response.lastModified)
                    } else {
                        components = response.components
                    }
                } else {
                    if (!pollingMode) {
                        errorMessage = "No authentication token"
                    }
                }
            } catch (e: Throwable) {
                if (!pollingMode) {
                    val isAuthError = e is ResponseException && e.response.status == HttpStatusCode.Unauthorized
                    if (isAuthError) return@launch

                    val hasCachedData = components.isNotEmpty()
                    if (!hasCachedData) {
                        errorMessage =
                            when {
                                e.message?.contains(
                                    "Network",
                                ) == true ||
                                    e.message?.contains(
                                        "timeout",
                                    ) == true -> "Network error. Please check your connection."

                                else -> "Error: ${e.message ?: "Failed to load components"}"
                            }
                    }
                }
            } finally {
                if (pollingMode || componentsCache != null) {
                    isRefreshing = false
                } else {
                    isLoading = false
                }
            }
        }
    }

    fun updateCartQuantity(
        component: Component,
        delta: Int,
    ) {
        val current = cartQuantities[component.id] ?: 0
        val maxAllowed = component.availableQuantity
        val next = (current + delta).coerceIn(0, maxAllowed)
        cartQuantities =
            if (next == 0) {
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
                        "Bearer $token",
                        CreateRequestPayload(
                            items = cleanedItems,
                            targetFacultyId = selectedFacultyId!!,
                            projectTitle = projectTitle.trim(),
                        ),
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
            } catch (e: Throwable) {
                cartError =
                    when {
                        e.message?.contains(
                            "400",
                        ) == true ||
                            e.message?.contains("Bad Request") == true -> "Invalid request. Please check your input."

                        e.message?.contains(
                            "Network",
                        ) == true ||
                            e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                        else -> "Error: ${e.message ?: "Failed to create request"}"
                    }
            } finally {
                isSubmittingRequest = false
            }
        }
    }

    LaunchedEffect(componentsCache) {
        componentsCache?.componentsFlow()?.collect { cached ->
            components = cached
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadComponents()
        while (true) {
            delay(8000.milliseconds)
            if (errorMessage == null && !isLoading && !isRefreshing) {
                loadComponents(pollingMode = true)
            }
        }
    }

    LaunchedEffect(userRole) {
        if (userRole != UserRole.FACULTY) return@LaunchedEffect
        try {
            val token = tokenManager.token.first() ?: return@LaunchedEffect
            val response = ApiClient.requestApiService.getRequests("Bearer $token", RequestStatus.PENDING.name)
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

    Scaffold(
        topBar = {
            ComponentsTopBar(
                onNavigateToHome = onNavigateToProfile,
                onNavigateToRequests = onNavigateToRequests,
                pendingRequestsCount = if (isFaculty) pendingRequestsCount else null,
                showExportCsv = canExportCsv && components.isNotEmpty(),
                onExportCsv = { exportComponentsCsv() },
            )
        },
        floatingActionButton = {
            when {
                cartQuantities.isNotEmpty() -> {
                    CartFAB(
                        itemCount = cartQuantities.values.sum(),
                        onClick = { showCartDialog = true },
                    )
                }

                !isReadOnly -> {
                    AddComponentFAB(
                        onClick = {
                            editingComponent = null
                            showDialog = true
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        ComponentsScreenBody(
            paddingValues = paddingValues,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            categoryFilter = categoryFilter,
            onCategoryFilterChange = { categoryFilter = it },
            locationFilter = locationFilter,
            onLocationFilterChange = { locationFilter = it },
            isLoading = isLoading,
            errorMessage = errorMessage,
            components = components,
            filteredComponents = filteredComponents,
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
            },
        )
    }

    ComponentsDialogs(
        showEditDialog = showDialog && !isReadOnly,
        editingComponent = editingComponent,
        onDismissEdit = { showDialog = false },
        onSaveEdit = { request ->
            scope.launch {
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
                        showDialog = false
                        editingComponent = null
                        loadComponents()
                    }
                } catch (e: Throwable) {
                    errorMessage = "Error: ${e.message ?: "Failed to save component"}"
                }
            }
        },
        onPickImage = {
            scope.launch {
                val image =
                    com.iiitnr.inventoryapp.ui.platform
                        .pickImage()
                if (image != null && editingComponent != null) {
                    try {
                        val token = tokenManager.token.first()
                        if (token != null) {
                            val extension = image.filename.substringAfterLast('.', "")
                            val uploadFilename =
                                if (extension.isNotEmpty()) {
                                    "${editingComponent!!.id}.$extension"
                                } else {
                                    editingComponent!!.id
                                }
                            val response =
                                ApiClient.componentApiService.uploadImage(
                                    "Bearer $token",
                                    editingComponent!!.id,
                                    image.bytes,
                                    uploadFilename,
                                )
                            editingComponent = response.component
                            components =
                                components.map {
                                    if (it.id == response.component.id) response.component else it
                                }
                            loadComponents(pollingMode = true)
                        }
                    } catch (e: Throwable) {
                        errorMessage = "Error uploading image: ${e.message}"
                    }
                }
            }
        },
        onRemoveImage = {
            scope.launch {
                if (editingComponent != null) {
                    try {
                        val token = tokenManager.token.first()
                        if (token != null) {
                            ApiClient.componentApiService.deleteImage(
                                "Bearer $token",
                                editingComponent!!.id,
                            )
                            loadComponents()
                        }
                    } catch (e: Throwable) {
                        errorMessage = "Error removing image: ${e.message}"
                    }
                }
            }
        },
        showDeleteDialog = if (!isReadOnly) showDeleteDialog else null,
        onDismissDelete = { showDeleteDialog = null },
        onConfirmDelete = { component ->
            scope.launch {
                try {
                    val token = tokenManager.token.first()
                    if (token != null) {
                        ApiClient.componentApiService.deleteComponent(
                            "Bearer $token",
                            component.id,
                        )
                        showDeleteDialog = null
                        loadComponents()
                    }
                } catch (e: Throwable) {
                    errorMessage = "Error: ${e.message ?: "Failed to delete component"}"
                }
            }
        },
        showCartDialog = showCartDialog,
        components = components,
        cartQuantities = cartQuantities,
        cartError = cartError,
        isSubmittingRequest = isSubmittingRequest,
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
        onDismissCart = {
            showCartDialog = false
            cartError = null
        },
        onSubmitCart = { submitRequest() },
    )
}

@Composable
private fun ComponentsScreenBody(
    paddingValues: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categoryFilter: String?,
    onCategoryFilterChange: (String?) -> Unit,
    locationFilter: String?,
    onLocationFilterChange: (String?) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    components: List<Component>,
    filteredComponents: List<Component>,
    isReadOnly: Boolean,
    cartQuantities: Map<String, Int>,
    onRetry: () -> Unit,
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit,
    onAddToCart: (Component) -> Unit,
    onUpdateCartQuantity: (Component, Int) -> Unit,
) {
    Column(modifier = Modifier.padding(paddingValues)) {
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            placeholder = "Search components...",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        ComponentFilterRow(
            options = listOf("All") + ComponentCategory.labels,
            selected = categoryFilter,
            onSelectedChange = onCategoryFilterChange,
        )

        ComponentFilterRow(
            options = listOf("All") + ComponentLocation.labels,
            selected = locationFilter,
            onSelectedChange = onLocationFilterChange,
        )

        ComponentsContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            components = filteredComponents,
            allComponents = components,
            searchQuery = searchQuery,
            isReadOnly = isReadOnly,
            cartQuantities = cartQuantities,
            onRetry = onRetry,
            onEdit = onEdit,
            onDelete = onDelete,
            onAddToCart = onAddToCart,
            onUpdateCartQuantity = onUpdateCartQuantity,
        )
    }
}

@Composable
private fun ComponentFilterRow(
    options: List<String>,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        items(options) { option ->
            val isSelected = (selected == null && option == "All") || selected.equals(option, ignoreCase = true)
            TextButton(onClick = { onSelectedChange(if (option == "All") null else option) }) {
                Text(
                    text = option,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun ComponentsDialogs(
    showEditDialog: Boolean,
    editingComponent: Component?,
    onDismissEdit: () -> Unit,
    onSaveEdit: (ComponentRequest) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    showDeleteDialog: Component?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (Component) -> Unit,
    showCartDialog: Boolean,
    components: List<Component>,
    cartQuantities: Map<String, Int>,
    cartError: String?,
    isSubmittingRequest: Boolean,
    facultyOptions: List<User>,
    selectedFacultyId: String?,
    isLoadingFaculty: Boolean,
    onSelectFaculty: (String?) -> Unit,
    projectTitle: String,
    onProjectTitleChange: (String) -> Unit,
    onUpdateQuantity: (Component, Int) -> Unit,
    onRemoveItem: (Component) -> Unit,
    onDismissCart: () -> Unit,
    onSubmitCart: () -> Unit,
) {
    if (showEditDialog) {
        ComponentDialog(
            component = editingComponent,
            onDismiss = onDismissEdit,
            onSave = { onSaveEdit(it) },
            onPickImage = onPickImage,
            onRemoveImage = onRemoveImage,
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete Component") },
            text = { Text("Are you sure you want to delete \"${showDeleteDialog.name}\"?") },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(showDeleteDialog) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
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
            onSelectFaculty = onSelectFaculty,
            projectTitle = projectTitle,
            onProjectTitleChange = onProjectTitleChange,
            onUpdateQuantity = onUpdateQuantity,
            onRemoveItem = onRemoveItem,
            onDismiss = onDismissCart,
            onSubmit = onSubmitCart,
        )
    }
}

package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.ComponentCategory
import com.iiitnr.inventoryapp.data.models.ComponentLocation
import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.RequestItemPayload
import com.iiitnr.inventoryapp.data.preferences.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun extractErrorMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val map = Gson().fromJson(raw, Map::class.java)
        map["error"]?.toString()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ComponentsScreen(
    tokenManager: TokenManager,
    onNavigateToRequests: (() -> Unit)? = null,
    onNavigateToHome: () -> Unit
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
    val scope = rememberCoroutineScope()

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
                ?.contains(query) == true || component.quantity.toString().contains(query)
        }
    }

    fun loadComponents() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val userResponse = ApiClient.authApiService.getMe("Bearer $token")
                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        userRole = userResponse.body()!!.user.role
                    }

                    val response = ApiClient.componentApiService.getComponents("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        components = response.body()!!.components
                    } else {
                        errorMessage = "Failed to load components"
                    }
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateCartQuantity(component: Component, delta: Int) {
        val current = cartQuantities[component.id] ?: 0
        val maxAllowed = component.quantity
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

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.createRequest(
                        "Bearer $token", CreateRequestPayload(items = cleanedItems)
                    )
                    if (response.isSuccessful) {
                        showCartDialog = false
                        cartQuantities = emptyMap()
                        cartError = null
                        onNavigateToRequests?.invoke()
                    } else {
                        cartError = extractErrorMessage(response.errorBody()?.string())
                            ?: "Failed to create request"
                    }
                } else {
                    cartError = "No authentication token"
                }
            } catch (e: Exception) {
                cartError = "Error: ${e.message}"
            } finally {
                isSubmittingRequest = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadComponents()
    }

    Scaffold(topBar = {
        ComponentsTopBar(
            onNavigateToHome = onNavigateToHome,
            onNavigateToRequests = onNavigateToRequests
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            val response = if (editingComponent != null) {
                                ApiClient.componentApiService.updateComponent(
                                    "Bearer $token", editingComponent!!.id, request
                                )
                            } else {
                                ApiClient.componentApiService.createComponent(
                                    "Bearer $token", request
                                )
                            }
                            if (response.isSuccessful) {
                                showDialog = false
                                editingComponent = null
                                loadComponents()
                            } else {
                                errorMessage =
                                    "Failed to ${if (editingComponent != null) "update" else "create"} component"
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
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
                                        val response =
                                            ApiClient.componentApiService.deleteComponent(
                                                "Bearer $token", component.id
                                            )
                                        if (response.isSuccessful) {
                                            showDeleteDialog = null
                                            loadComponents()
                                        } else {
                                            errorMessage = "Failed to delete component"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
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

@Composable
private fun ComponentsTopBar(
    onNavigateToHome: () -> Unit,
    onNavigateToRequests: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Components",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onNavigateToRequests?.let {
                TextButton(onClick = it) {
                    Text(
                        "Requests",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = onNavigateToHome) {
                Text(
                    "Profile",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AddComponentFAB(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = "Add Component")
    }
}

@Composable
private fun CartFAB(itemCount: Int, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Box {
            Icon(
                Icons.Default.ShoppingCart, contentDescription = "View Cart"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(
                        color = MaterialTheme.colorScheme.error, shape = CircleShape
                    )
                    .size(18.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (itemCount > 99) "99+" else itemCount.toString(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String, onSearchQueryChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier,
        placeholder = { Text("Search components...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear, contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}

@Composable
private fun ComponentsContent(
    isLoading: Boolean,
    errorMessage: String?,
    components: List<Component>,
    allComponents: List<Component>,
    searchQuery: String,
    isReadOnly: Boolean,
    cartQuantities: Map<String, Int>,
    onRetry: () -> Unit,
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit,
    onAddToCart: (Component) -> Unit,
    onUpdateCartQuantity: (Component, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading -> LoadingIndicator()
            errorMessage != null -> ErrorContent(errorMessage, onRetry)
            components.isEmpty() && allComponents.isEmpty() -> EmptyState(isReadOnly)
            components.isEmpty() && searchQuery.isNotBlank() -> SearchEmptyState()
            else -> ComponentsList(
                components = components,
                isReadOnly = isReadOnly,
                cartQuantities = cartQuantities,
                onEdit = onEdit,
                onDelete = onDelete,
                onAddToCart = onAddToCart,
                onUpdateCartQuantity = onUpdateCartQuantity
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = errorMessage, color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState(isReadOnly: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No components found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isReadOnly) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the + button to add a component",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No components match your search",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ComponentsList(
    components: List<Component>,
    isReadOnly: Boolean,
    cartQuantities: Map<String, Int>,
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit,
    onAddToCart: (Component) -> Unit,
    onUpdateCartQuantity: (Component, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(components) { component ->
            ComponentCard(
                component = component,
                isReadOnly = isReadOnly,
                cartQuantity = cartQuantities[component.id] ?: 0,
                cartQuantities = cartQuantities,
                onEdit = { onEdit(component) },
                onDelete = { onDelete(component) },
                onAddToCart = { onAddToCart(component) },
                onUpdateCartQuantity = { delta -> onUpdateCartQuantity(component, delta) })
        }
    }
}

@Composable
fun ComponentCard(
    component: Component,
    isReadOnly: Boolean = false,
    cartQuantity: Int = 0,
    cartQuantities: Map<String, Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToCart: (() -> Unit)? = null,
    onUpdateCartQuantity: ((Int) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!component.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = component.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    if (!isReadOnly && cartQuantities.isEmpty()) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (cartQuantity == 0) {
                        IconButton(
                            onClick = { onAddToCart?.invoke() }, enabled = component.quantity > 0
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = "Add to cart",
                                tint = MaterialTheme.colorScheme.primary
                            )

                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateCartQuantity?.invoke(-1) }) {
                                Icon(
                                    Icons.Default.Remove, contentDescription = "Decrease quantity"
                                )
                            }
                            Text(
                                text = cartQuantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onUpdateCartQuantity?.invoke(1) },
                                enabled = cartQuantity < component.quantity
                            ) {
                                Icon(
                                    Icons.Default.Add, contentDescription = "Increase quantity"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("Quantity", component.quantity.toString())
                if (!component.category.isNullOrBlank()) {
                    InfoChip("Category", component.category.replace('_', ' '))
                }
                if (!component.location.isNullOrBlank()) {
                    InfoChip("Location", component.location.replace('_', ' '))
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ComponentDialog(
    component: Component?, onDismiss: () -> Unit, onSave: (ComponentRequest) -> Unit
) {
    var name by remember { mutableStateOf(component?.name ?: "") }
    var description by remember { mutableStateOf(component?.description ?: "") }
    var quantity by remember { mutableStateOf(component?.quantity?.toString() ?: "0") }
    var category by remember { mutableStateOf(component?.category ?: "") }
    var location by remember { mutableStateOf(component?.location?.replace('_', ' ') ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (component != null) "Edit Component" else "Add Component") },
        text = {
            ComponentDialogFields(
                name = name,
                description = description,
                quantity = quantity,
                category = category,
                location = location,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                onQuantityChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                onCategoryChange = { category = it },
                onLocationChange = { location = it })
        },
        confirmButton = {
            ComponentDialogSaveButton(
                isLoading = isLoading, isNameValid = name.isNotBlank(), onSave = {
                    isLoading = true
                    onSave(
                        ComponentRequest(
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotBlank() },
                            quantity = quantity.toIntOrNull() ?: 0,
                            category = category.trim().takeIf { it.isNotBlank() },
                            location = location.trim().takeIf { it.isNotBlank() })
                    )
                    isLoading = false
                })
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        })
}

@Composable
private fun ComponentDialogFields(
    name: String,
    description: String,
    quantity: String,
    category: String,
    location: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val categoryOptions = ComponentCategory.labels
        val locationOptions = ComponentLocation.labels

        var isCategoryExpanded by remember { mutableStateOf(false) }
        var isLocationExpanded by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        CategoryDropdownField(
            value = category,
            options = categoryOptions,
            expanded = isCategoryExpanded,
            onExpandedChange = { isCategoryExpanded = it },
            onSelect = onCategoryChange
        )

        LocationDropdownField(
            value = location,
            options = locationOptions,
            expanded = isLocationExpanded,
            onExpandedChange = { isLocationExpanded = it },
            onSelect = onLocationChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownField(
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Category") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ),
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdownField(
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ),
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ComponentDialogSaveButton(
    isLoading: Boolean, isNameValid: Boolean, onSave: () -> Unit
) {
    TextButton(
        onClick = onSave, enabled = !isLoading && isNameValid
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
            Text("Save")
        }
    }
}

@Composable
private fun CartDialog(
    components: List<Component>,
    cartQuantities: Map<String, Int>,
    cartError: String?,
    isSubmitting: Boolean,
    onUpdateQuantity: (Component, Int) -> Unit,
    onRemoveItem: (Component) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val cartItems = cartQuantities.filterValues { it > 0 }.mapNotNull { (componentId, qty) ->
        components.firstOrNull { it.id == componentId }?.let { it to qty }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cart") }, text = {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cartItems.isEmpty()) {
                Text(
                    text = "Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                cartItems.forEach { (component, qty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = component.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Available: ${component.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateQuantity(component, -1) },
                                enabled = qty > 0 && !isSubmitting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease quantity"
                                )
                            }
                            Text(
                                text = qty.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onUpdateQuantity(component, 1) },
                                enabled = qty < component.quantity && !isSubmitting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase quantity"
                                )
                            }
                            IconButton(
                                onClick = { onRemoveItem(component) }, enabled = !isSubmitting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from cart",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (cartError != null) {
                Text(
                    text = cartError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp
                )
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = onSubmit, enabled = cartItems.isNotEmpty() && !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Submit Request")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss, enabled = !isSubmitting) {
            Text("Cancel")
        }
    })
}

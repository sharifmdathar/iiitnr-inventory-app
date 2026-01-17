package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.preferences.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ComponentsScreen(
    tokenManager: TokenManager, onNavigateBack: () -> Unit
) {
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingComponent by remember { mutableStateOf<Component?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Component?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
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
            component.name.lowercase().contains(query) ||
                    component.description?.lowercase()?.contains(query) == true ||
                    component.category?.lowercase()?.contains(query) == true ||
                    component.location?.lowercase()?.contains(query) == true ||
                    component.quantity.toString().contains(query)
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

    LaunchedEffect(Unit) {
        loadComponents()
    }

    Scaffold(topBar = {
        ComponentsTopBar(onNavigateBack = onNavigateBack)
    }, floatingActionButton = {
        if (!isReadOnly) {
            AddComponentFAB(
                onClick = {
                    editingComponent = null
                    showDialog = true
                })
        }
    }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Search bar
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
                onRetry = { loadComponents() },
                onEdit = { component ->
                    editingComponent = component
                    showDialog = true
                },
                onDelete = { component ->
                    showDeleteDialog = component
                }
            )
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
}

@Composable
private fun ComponentsTopBar(onNavigateBack: () -> Unit) {
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
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNavigateBack) {
            Text("Back")
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
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier,
        placeholder = { Text("Search components...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
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
    onRetry: () -> Unit,
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit,
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
            else -> ComponentsList(components, isReadOnly, onEdit, onDelete)
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
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit
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
                onEdit = { onEdit(component) },
                onDelete = { onDelete(component) })
        }
    }
}

@Composable
fun ComponentCard(
    component: Component, isReadOnly: Boolean = false, onEdit: () -> Unit, onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        fontWeight = FontWeight.Bold
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
                if (!isReadOnly) {
                    Row {
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("Quantity", component.quantity.toString())
                if (!component.category.isNullOrBlank()) {
                    InfoChip("Category", component.category)
                }
                if (!component.location.isNullOrBlank()) {
                    InfoChip("Location", component.location)
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
            fontWeight = FontWeight.Medium
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
    var location by remember { mutableStateOf(component?.location ?: "") }
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

        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
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

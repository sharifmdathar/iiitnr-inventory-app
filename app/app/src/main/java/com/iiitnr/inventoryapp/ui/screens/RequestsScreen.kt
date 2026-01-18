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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.google.gson.Gson
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestItem
import com.iiitnr.inventoryapp.data.models.RequestItemPayload
import com.iiitnr.inventoryapp.data.preferences.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class RequestItemInput(
    val componentId: String,
    val quantity: String
)

private fun extractErrorMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val map = Gson().fromJson(raw, Map::class.java)
        map["error"]?.toString()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun RequestsScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit
) {
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var itemInputs by remember {
        mutableStateOf(listOf(RequestItemInput(componentId = "", quantity = "")))
    }
    val scope = rememberCoroutineScope()

    fun loadRequests() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getRequests("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        requests = response.body()!!.requests
                    } else {
                        errorMessage =
                            extractErrorMessage(response.errorBody()?.string())
                                ?: "Failed to load requests"
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

    fun submitRequest() {
        scope.launch {
            dialogError = null
            val cleanedItems = itemInputs.map {
                RequestItemPayload(
                    componentId = it.componentId.trim(),
                    quantity = it.quantity.trim().toIntOrNull() ?: 0
                )
            }

            if (cleanedItems.isEmpty() || cleanedItems.any { it.componentId.isBlank() }) {
                dialogError = "All component IDs are required"
                return@launch
            }

            if (cleanedItems.any { it.quantity <= 0 }) {
                dialogError = "Quantity must be a positive number"
                return@launch
            }

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.createRequest(
                        "Bearer $token",
                        CreateRequestPayload(items = cleanedItems)
                    )
                    if (response.isSuccessful) {
                        showDialog = false
                        itemInputs = listOf(RequestItemInput(componentId = "", quantity = ""))
                        loadRequests()
                    } else {
                        dialogError =
                            extractErrorMessage(response.errorBody()?.string())
                                ?: "Failed to create request"
                    }
                } else {
                    dialogError = "No authentication token"
                }
            } catch (e: Exception) {
                dialogError = "Error: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    Scaffold(
        topBar = {
            RequestsTopBar(onNavigateBack = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Request")
            }
        }
    ) { paddingValues ->
        RequestsContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            requests = requests,
            onRetry = { loadRequests() },
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (showDialog) {
        RequestDialog(
            itemInputs = itemInputs,
            dialogError = dialogError,
            onItemChange = { index, updated ->
                itemInputs = itemInputs.toMutableList().apply { this[index] = updated }
            },
            onAddItem = {
                itemInputs = itemInputs + RequestItemInput(componentId = "", quantity = "")
            },
            onRemoveItem = { index ->
                if (itemInputs.size > 1) {
                    itemInputs = itemInputs.toMutableList().apply { removeAt(index) }
                }
            },
            onDismiss = {
                showDialog = false
                dialogError = null
            },
            onSubmit = { submitRequest() }
        )
    }
}

@Composable
private fun RequestsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

@Composable
private fun RequestsContent(
    isLoading: Boolean,
    errorMessage: String?,
    requests: List<Request>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading -> LoadingIndicator()
            errorMessage != null -> ErrorContent(errorMessage, onRetry)
            requests.isEmpty() -> EmptyRequestsState()
            else -> RequestsList(requests)
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
            text = errorMessage,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyRequestsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No requests yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create a request",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RequestsList(requests: List<Request>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestCard(request = request)
        }
    }
}

@Composable
private fun RequestCard(request: Request) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Status: ${request.status}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created: ${request.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Components",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            request.items.forEach { item ->
                RequestItemRow(item = item)
            }
        }
    }
}

@Composable
private fun RequestItemRow(item: RequestItem) {
    val itemName = item.component?.name ?: item.componentId ?: "Unknown Component"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = itemName,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "x${item.quantity}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RequestDialog(
    itemInputs: List<RequestItemInput>,
    dialogError: String?,
    onItemChange: (Int, RequestItemInput) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Request") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemInputs.forEachIndexed { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Component ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            if (itemInputs.size > 1) {
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove item"
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = item.componentId,
                            onValueChange = { onItemChange(index, item.copy(componentId = it)) },
                            label = { Text("Component ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.quantity,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) {
                                    onItemChange(index, item.copy(quantity = value))
                                }
                            },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                TextButton(onClick = onAddItem) {
                    Text("Add Another Component")
                }

                if (dialogError != null) {
                    Text(
                        text = dialogError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

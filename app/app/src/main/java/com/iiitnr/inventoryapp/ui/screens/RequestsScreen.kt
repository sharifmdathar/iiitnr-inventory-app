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
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestItem
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
fun RequestsScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
    onNavigateToComponents: () -> Unit
) {
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteRequestId by remember { mutableStateOf<String?>(null) }
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

    fun deleteRequest(requestId: String) {
        scope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response =
                        ApiClient.requestApiService.deleteRequest("Bearer $token", requestId)
                    if (response.isSuccessful) {
                        loadRequests()
                    } else {
                        errorMessage =
                            extractErrorMessage(response.errorBody()?.string())
                                ?: "Failed to delete request"
                    }
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    if (pendingDeleteRequestId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRequestId = null },
            title = { Text("Retract request?") },
            text = { Text("This will delete the request permanently") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingDeleteRequestId
                        pendingDeleteRequestId = null
                        if (id != null) deleteRequest(id)
                    }
                ) {
                    Text("Retract", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRequestId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            RequestsTopBar(onNavigateBack = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToComponents() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Request")
            }
        }
    ) { paddingValues ->
        RequestsContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            requests = requests,
            onRetry = { loadRequests() },
            onDeleteRequest = { requestId -> pendingDeleteRequestId = requestId },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun RequestsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onNavigateBack) {
            Text(
                "Back",
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "My Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RequestsContent(
    isLoading: Boolean,
    errorMessage: String?,
    requests: List<Request>,
    onRetry: () -> Unit,
    onDeleteRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading -> LoadingIndicator()
            errorMessage != null -> ErrorContent(errorMessage, onRetry)
            requests.isEmpty() -> EmptyRequestsState()
            else -> RequestsList(requests, onDeleteRequest)
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
private fun RequestsList(
    requests: List<Request>,
    onDeleteRequest: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestCard(request = request, onDeleteRequest = onDeleteRequest)
        }
    }
}

@Composable
private fun RequestCard(
    request: Request,
    onDeleteRequest: (String) -> Unit
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${request.status}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (request.status == "PENDING") {
                    IconButton(onClick = { onDeleteRequest(request.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Retract request",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
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


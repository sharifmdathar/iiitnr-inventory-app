package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.UpdateRequestStatusPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.components.requests.RequestsContent
import com.iiitnr.inventoryapp.ui.components.requests.RequestsTopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
    onNavigateToComponents: () -> Unit,
) {
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteRequestId by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isFaculty = currentUser?.role == "FACULTY"
    val isAdminOrTA =
        currentUser?.role?.uppercase() == "ADMIN" || currentUser?.role?.uppercase() == "TA"

    val query = searchQuery.trim()
    val filteredRequests =
        requests.filter { request ->
            val matchesStatus = statusFilter?.let { it == request.status } ?: true
            val textMatches =
                listOfNotNull(
                    request.projectTitle,
                    request.user?.name,
                    request.user?.email,
                    request.targetFaculty?.name,
                    request.targetFaculty?.email,
                ).any { it.contains(query, ignoreCase = true) }
            val itemMatches =
                request.items.any { it.component?.name?.contains(query, ignoreCase = true) == true }

            matchesStatus && (query.isBlank() || textMatches || itemMatches)
        }

    fun loadRequests() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getRequests("Bearer $token")
                    requests = response.requests
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage =
                    when {
                        e.message?.contains(
                            "401",
                        ) == true ||
                            e.message?.contains("Unauthorized") == true -> "Session expired. Please login again."

                        e.message?.contains(
                            "Network",
                        ) == true ||
                            e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                        else -> "Error: ${e.message ?: "Failed to load requests"}"
                    }
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
                    ApiClient.requestApiService.deleteRequest("Bearer $token", requestId)
                    loadRequests()
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message ?: "Failed to delete request"}"
            }
        }
    }

    fun updateRequestStatus(
        requestId: String,
        status: String,
    ) {
        scope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    ApiClient.requestApiService.updateRequestStatus(
                        "Bearer $token",
                        requestId,
                        UpdateRequestStatusPayload(status = status),
                    )
                    loadRequests()
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message ?: "Failed to update request status"}"
            }
        }
    }

    fun loadUserData() {
        scope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.authApiService.getMe("Bearer $token")
                    currentUser = response.user
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUserData()
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
                    },
                ) {
                    Text("Retract", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRequestId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(topBar = {
        RequestsTopBar(onNavigateBack = onNavigateBack)
    }, floatingActionButton = {
        if (!isFaculty) {
            FloatingActionButton(onClick = { onNavigateToComponents() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Request")
            }
        }
    }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                placeholder = "Search requests...",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                val statusOptions = listOf("ALL", "PENDING", "APPROVED", "REJECTED", "FULFILLED")
                items(statusOptions) { option ->
                    val isSelected =
                        (statusFilter == null && option == "ALL") || statusFilter == option
                    TextButton(
                        onClick = {
                            statusFilter = if (option == "ALL") null else option
                        },
                    ) {
                        Text(
                            text = option.lowercase().replaceFirstChar { it.uppercaseChar() },
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

            RequestsContent(
                isLoading = isLoading,
                errorMessage = errorMessage,
                requests = filteredRequests,
                onRetry = { loadRequests() },
                onDeleteRequest =
                    if (isFaculty) {
                        null
                    } else {
                        { requestId ->
                            pendingDeleteRequestId = requestId
                        }
                    },
                onApproveRequest =
                    if (isFaculty) {
                        { requestId ->
                            updateRequestStatus(
                                requestId,
                                "APPROVED",
                            )
                        }
                    } else {
                        null
                    },
                onRejectRequest =
                    if (isFaculty) {
                        { requestId ->
                            updateRequestStatus(
                                requestId,
                                "REJECTED",
                            )
                        }
                    } else {
                        null
                    },
                onFulfillRequest =
                    if (isAdminOrTA) {
                        { requestId ->
                            updateRequestStatus(
                                requestId,
                                "FULFILLED",
                            )
                        }
                    } else {
                        null
                    },
                isFaculty = isFaculty,
                modifier = Modifier.padding(),
            )
        }
    }
}

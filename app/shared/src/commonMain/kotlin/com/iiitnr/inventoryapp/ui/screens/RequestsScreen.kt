package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.UpdateRequestStatusPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.components.common.requestStatusColor
import com.iiitnr.inventoryapp.ui.components.requests.FulfillByIdDialog
import com.iiitnr.inventoryapp.ui.components.requests.REQUEST_QR_PREFIX
import com.iiitnr.inventoryapp.ui.components.requests.RenewReasonDialog
import com.iiitnr.inventoryapp.ui.components.requests.RequestItemRow
import com.iiitnr.inventoryapp.ui.components.requests.RequestQrDialog
import com.iiitnr.inventoryapp.ui.components.requests.RequestsContent
import com.iiitnr.inventoryapp.ui.components.requests.RequestsTopBar
import com.iiitnr.inventoryapp.ui.components.requests.compactUserLabel
import com.iiitnr.inventoryapp.ui.components.requests.nextScannedRequestStatus
import com.iiitnr.inventoryapp.ui.components.requests.requestStatusActionSnackbarMessage
import com.iiitnr.inventoryapp.ui.components.requests.requestStatusDisplayLabel
import com.iiitnr.inventoryapp.ui.platform.QrScannerContent
import com.iiitnr.inventoryapp.ui.platform.isQrScanAvailable
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
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
    var isRefreshing by remember { mutableStateOf(false) }
    var pendingDeleteRequestId by remember { mutableStateOf<String?>(null) }
    var pendingRenewRequestId by remember { mutableStateOf<String?>(null) }
    var renewReasonInput by remember { mutableStateOf("") }
    var requestToShowQr by remember { mutableStateOf<Request?>(null) }
    var showRequestIdDialog by remember { mutableStateOf(false) }
    var scannedRequest by remember { mutableStateOf<Request?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }
    var requestIdInput by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isFaculty = currentUser?.role == "FACULTY"
    val isAdminOrTA = currentUser?.role?.uppercase() == "ADMIN" || currentUser?.role?.uppercase() == "TA"

    val query: String = searchQuery.trim()
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
            val itemMatches = request.items.any { it.component?.name?.contains(query, ignoreCase = true) == true }

            matchesStatus && (query.isBlank() || textMatches || itemMatches)
        }

    fun loadRequests(pollingMode: Boolean = false) {
        scope.launch {
            if (pollingMode && isRefreshing) {
                return@launch
            }

            if (pollingMode) {
                isRefreshing = true
            } else {
                isLoading = true
                errorMessage = null
            }

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getRequests("Bearer $token")
                    requests = response.requests
                    if (pollingMode && errorMessage != null) {
                        errorMessage = null
                    }
                } else {
                    if (!pollingMode) {
                        errorMessage = "No authentication token"
                    }
                }
            } catch (e: Exception) {
                if (!pollingMode) {
                    val isAuthError = e is ResponseException && e.response.status == HttpStatusCode.Unauthorized
                    if (isAuthError) return@launch

                    errorMessage =
                        when {
                            e.message?.contains(
                                "Network",
                            ) == true ||
                                e.message?.contains("timeout") == true -> "Network error. Please check your connection."

                            else -> "Error: ${e.message ?: "Failed to load requests"}"
                        }
                }
            } finally {
                if (pollingMode) {
                    isRefreshing = false
                } else {
                    isLoading = false
                }
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
        lastRenewReason: String? = null,
    ) {
        scope.launch {
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    ApiClient.requestApiService.updateRequestStatus(
                        "Bearer $token",
                        requestId,
                        UpdateRequestStatusPayload(
                            status = status,
                            lastRenewReason = lastRenewReason,
                        ),
                    )
                    loadRequests()
                    requestStatusActionSnackbarMessage(status)?.let { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message ?: "Failed to update request status"}"
            }
        }
    }

    fun openScannedRequest(rawValue: String) {
        val requestId = rawValue.trim().removePrefix(REQUEST_QR_PREFIX).trim()
        if (requestId.isBlank()) {
            return
        }

        val request = requests.firstOrNull { it.id == requestId }
        if (request == null) {
            scope.launch {
                snackbarHostState.showSnackbar("Request not found. Refresh and try again.")
            }
            return
        }

        scannedRequest = request
        requestIdInput = requestId
        showRequestIdDialog = false
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
        loadRequests(pollingMode = false)
        while (true) {
            delay(8000)
            if (errorMessage == null && !isLoading && !isRefreshing) {
                loadRequests(pollingMode = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        requestToShowQr?.let { request ->
            RequestQrDialog(
                request = request,
                onDismiss = { requestToShowQr = null },
            )
        }

        if (pendingRenewRequestId != null) {
            RenewReasonDialog(
                reason = renewReasonInput,
                onReasonChange = { renewReasonInput = it },
                onConfirm = {
                    val id = pendingRenewRequestId
                    val reason = renewReasonInput.trim()
                    pendingRenewRequestId = null
                    renewReasonInput = ""
                    if (id != null && reason.isNotEmpty()) {
                        updateRequestStatus(id, "REQUESTED_RENEW", lastRenewReason = reason)
                    }
                },
                onDismiss = {
                    pendingRenewRequestId = null
                    renewReasonInput = ""
                },
            )
        }

        scannedRequest?.let { request ->
            ScannedRequestDialog(
                request = request,
                onConfirm = { nextStatus ->
                    scannedRequest = null
                    requestIdInput = ""
                    updateRequestStatus(request.id, nextStatus)
                },
                onDismiss = {
                    scannedRequest = null
                    requestIdInput = ""
                },
            )
        }

        if (showRequestIdDialog && !showQrScanner && scannedRequest == null) {
            FulfillByIdDialog(
                requestIdInput = requestIdInput,
                onRequestIdChange = { requestIdInput = it },
                dialogTitle = "Scan request QR / ID",
                confirmButtonLabel = "Review",
                onConfirm = {
                    openScannedRequest(requestIdInput)
                },
                onDismiss = {
                    showRequestIdDialog = false
                    requestIdInput = ""
                },
                onScanClick =
                    if (isAdminOrTA && isQrScanAvailable()) {
                        { showQrScanner = true }
                    } else {
                        null
                    },
            )
        }

        Scaffold(
            topBar = {
                RequestsTopBar(
                    onNavigateBack = onNavigateBack,
                    role = currentUser?.role,
                    onScanRequestClick =
                        if (isAdminOrTA) {
                            {
                                showRequestIdDialog = true
                                requestIdInput = ""
                            }
                        } else {
                            null
                        },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
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
                    val statusOptions =
                        listOf(
                            "ALL",
                            "PENDING",
                            "APPROVED",
                            "REJECTED",
                            "FULFILLED",
                            "REQUESTED_RENEW",
                            "RENEWED",
                            "EXPIRED",
                            "RETURNED",
                        )
                    items(statusOptions) { option ->
                        val isSelected = (statusFilter == null && option == "ALL") || statusFilter == option
                        TextButton(
                            onClick = {
                                statusFilter = if (option == "ALL") null else option
                            },
                        ) {
                            Text(
                                text = requestStatusDisplayLabel(option),
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
                    allRequests = requests,
                    statusFilter = statusFilter,
                    searchQuery = query,
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
                    onReturnRequest =
                        if (isAdminOrTA) {
                            { requestId ->
                                updateRequestStatus(
                                    requestId,
                                    "RETURNED",
                                )
                            }
                        } else {
                            null
                        },
                    onRequestRenew =
                        if (!isFaculty && !isAdminOrTA) {
                            { requestId ->
                                pendingRenewRequestId = requestId
                                renewReasonInput = ""
                            }
                        } else {
                            null
                        },
                    onApproveRenew =
                        if (isFaculty) {
                            { requestId ->
                                updateRequestStatus(
                                    requestId,
                                    "RENEWED",
                                )
                            }
                        } else {
                            null
                        },
                    onShowQr =
                        if (!isFaculty) {
                            { request -> requestToShowQr = request }
                        } else {
                            null
                        },
                    isFaculty = isFaculty,
                    modifier = Modifier.padding(),
                )
            }
        }

        if (showQrScanner) {
            Box(modifier = Modifier.fillMaxSize()) {
                QrScannerContent(
                    onResult = { rawValue ->
                        openScannedRequest(rawValue)
                        showQrScanner = false
                    },
                    onCancel = {
                        showQrScanner = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ScannedRequestDialog(
    request: Request,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nextStatus = nextScannedRequestStatus(request.status)
    val currentStatusColor = requestStatusColor(status = request.status)
    val nextStatusColor = nextStatus?.let { requestStatusColor(status = it) }
    val nextAction =
        when (nextStatus) {
            "FULFILLED" -> "Fulfill Request"
            "RETURNED" -> "Mark Returned"
            else -> null
        }
    val statusTransition =
        buildAnnotatedString {
            withStyle(SpanStyle(color = currentStatusColor)) {
                append(requestStatusDisplayLabel(request.status))
            }
            nextStatus?.let {
                append(" → ")
                withStyle(SpanStyle(color = nextStatusColor ?: currentStatusColor)) {
                    append(requestStatusDisplayLabel(it))
                }
            }
        }
    val requester = request.user?.let { compactUserLabel(it) }
    val faculty = request.targetFaculty?.let { it.name ?: it.email }
    val userLine =
        when {
            requester != null && faculty != null -> "$requester  ←  $faculty"
            requester != null -> requester
            faculty != null -> "From: $faculty"
            else -> null
        }
    val itemsTitle =
        when (nextStatus) {
            "FULFILLED" -> "Requesting items"
            "RETURNED" -> "Returning items"
            else -> ""
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(nextAction ?: "Request scanned") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = request.projectTitle,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = statusTransition,
                    style = MaterialTheme.typography.bodyMedium,
                )
                userLine?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = itemsTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (request.items.isEmpty()) {
                    Text(
                        text = "No items found for this request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    request.items.forEach { item ->
                        RequestItemRow(item = item)
                    }
                }
            }
        },
        confirmButton = {
            if (nextStatus != null && nextAction != null) {
                TextButton(onClick = { onConfirm(nextStatus) }) {
                    Text(nextAction)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (nextStatus != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

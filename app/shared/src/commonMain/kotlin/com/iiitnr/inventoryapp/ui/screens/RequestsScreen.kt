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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.IssueItemPayload
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.ReturnItemPayload
import com.iiitnr.inventoryapp.data.models.UpdateRequestStatusPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.components.requests.FulfillByIdDialog
import com.iiitnr.inventoryapp.ui.components.requests.REQUEST_QR_PREFIX
import com.iiitnr.inventoryapp.ui.components.requests.RenewReasonDialog
import com.iiitnr.inventoryapp.ui.components.requests.RequestQrDialog
import com.iiitnr.inventoryapp.ui.components.requests.RequestsContent
import com.iiitnr.inventoryapp.ui.components.requests.RequestsTopBar
import com.iiitnr.inventoryapp.ui.components.requests.requestStatusActionSnackbarMessage
import com.iiitnr.inventoryapp.ui.components.requests.requestStatusDisplayLabel
import com.iiitnr.inventoryapp.ui.platform.QrScannerContent
import com.iiitnr.inventoryapp.ui.platform.isQrScanAvailable
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RequestsScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
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
    var showQrScanner by remember { mutableStateOf(false) }
    var requestIdInput by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<RequestStatus?>(null) }
    var pendingActionSelectionRequest by remember { mutableStateOf<Request?>(null) }
    var pendingPartialIssueRequest by remember { mutableStateOf<Request?>(null) }
    var issueItemsInput by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingPartialReturnRequest by remember { mutableStateOf<Request?>(null) }
    var returnItemsInput by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isFaculty = currentUser?.role == UserRole.FACULTY
    val isAdminOrLA = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.LA

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
            } catch (e: Throwable) {
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
            } catch (e: Throwable) {
                errorMessage = "Error: ${e.message ?: "Failed to delete request"}"
            }
        }
    }

    fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        lastRenewReason: String? = null,
        issueItems: List<IssueItemPayload>? = null,
        returnItems: List<ReturnItemPayload>? = null,
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
                            issueItems = issueItems,
                            returnItems = returnItems,
                        ),
                    )
                    loadRequests()
                    requestStatusActionSnackbarMessage(status)?.let { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Throwable) {
                errorMessage = "Error: ${e.message ?: "Failed to update request status"}"
            }
        }
    }

    fun updateRequestStatusPartialIssue(request: Request) {
        pendingPartialIssueRequest = request
        issueItemsInput =
            request.items.associate { item ->
                val componentId = item.componentId ?: ""
                val remainingQuantity = (item.quantity - item.fulfilledQuantity).coerceAtLeast(0)
                componentId to remainingQuantity
            }
    }

    fun updateRequestStatusPartialReturn(request: Request) {
        pendingPartialReturnRequest = request
        returnItemsInput =
            request.items
                .filter { it.fulfilledQuantity > 0 }
                .associate { item ->
                    val componentId = item.componentId ?: ""
                    componentId to item.fulfilledQuantity
                }
    }

    fun confirmPartialIssue() {
        val request = pendingPartialIssueRequest
        if (request == null) return
        val items =
            issueItemsInput.filter { it.value > 0 }.map { (componentId, quantity) ->
                IssueItemPayload(componentId = componentId, quantity = quantity)
            }
        pendingPartialIssueRequest = null
        issueItemsInput = emptyMap()
        updateRequestStatus(request.id, RequestStatus.ISSUED, issueItems = items)
    }

    fun confirmPartialReturn() {
        val request = pendingPartialReturnRequest
        if (request == null) return
        val items =
            returnItemsInput.filter { it.value > 0 }.map { (componentId, quantity) ->
                ReturnItemPayload(componentId = componentId, quantity = quantity)
            }
        pendingPartialReturnRequest = null
        returnItemsInput = emptyMap()
        updateRequestStatus(request.id, RequestStatus.RETURNED, returnItems = items)
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

        requestIdInput = requestId
        showRequestIdDialog = false

        when (request.status) {
            RequestStatus.APPROVED -> updateRequestStatusPartialIssue(request)
            RequestStatus.PARTIALLY_ISSUED -> {
                pendingActionSelectionRequest = request
            }
            RequestStatus.ISSUED,
            RequestStatus.RENEWED,
            RequestStatus.EXPIRED,
            RequestStatus.PARTIALLY_RETURNED,
            -> updateRequestStatusPartialReturn(request)
            else -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "No action available for status: ${requestStatusDisplayLabel(request.status)}",
                    )
                }
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
        loadRequests(pollingMode = false)
        while (true) {
            delay(8000.milliseconds)
            if (errorMessage == null && !isLoading && !isRefreshing) {
                loadRequests(pollingMode = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RequestsDialogs(
            pendingDeleteRequestId = pendingDeleteRequestId,
            onDismissDelete = { pendingDeleteRequestId = null },
            onConfirmDelete = { id -> deleteRequest(id) },
            requestToShowQr = requestToShowQr,
            onDismissQr = { requestToShowQr = null },
            pendingRenewRequestId = pendingRenewRequestId,
            renewReasonInput = renewReasonInput,
            onRenewReasonChange = { renewReasonInput = it },
            onConfirmRenew = {
                val id = pendingRenewRequestId
                val reason = renewReasonInput.trim()
                pendingRenewRequestId = null
                renewReasonInput = ""
                if (id != null && reason.isNotEmpty()) {
                    updateRequestStatus(id, RequestStatus.REQUESTED_RENEW, lastRenewReason = reason)
                }
            },
            onDismissRenew = {
                pendingRenewRequestId = null
                renewReasonInput = ""
            },
            showRequestIdDialog = showRequestIdDialog,
            showQrScanner = showQrScanner,
            requestIdInput = requestIdInput,
            onRequestIdChange = { requestIdInput = it },
            onConfirmRequestId = { openScannedRequest(requestIdInput) },
            onDismissRequestId = {
                showRequestIdDialog = false
                requestIdInput = ""
            },
            onScanClick =
                if (isAdminOrLA && isQrScanAvailable()) {
                    { showQrScanner = true }
                } else {
                    null
                },
            pendingPartialIssueRequest = pendingPartialIssueRequest,
            issueItemsInput = issueItemsInput,
            onIssueItemQtyChange = { componentId, quantity ->
                issueItemsInput = issueItemsInput + (componentId to quantity)
            },
            onConfirmPartialIssue = { confirmPartialIssue() },
            onDismissPartialIssue = {
                pendingPartialIssueRequest = null
                issueItemsInput = emptyMap()
            },
            pendingPartialReturnRequest = pendingPartialReturnRequest,
            returnItemsInput = returnItemsInput,
            onReturnItemQtyChange = { componentId, quantity ->
                returnItemsInput = returnItemsInput + (componentId to quantity)
            },
            onConfirmPartialReturn = { confirmPartialReturn() },
            onDismissPartialReturn = {
                pendingPartialReturnRequest = null
                returnItemsInput = emptyMap()
            },
        )

        pendingActionSelectionRequest?.let { request ->
            RequestActionSelectionDialog(
                request = request,
                onIssueMore = {
                    pendingActionSelectionRequest = null
                    updateRequestStatusPartialIssue(request)
                },
                onCollectReturn = {
                    pendingActionSelectionRequest = null
                    updateRequestStatusPartialReturn(request)
                },
                onDismiss = {
                    pendingActionSelectionRequest = null
                },
            )
        }

        Scaffold(
            topBar = {
                RequestsTopBar(
                    onNavigateBack = onNavigateBack,
                    onScanRequestClick =
                        if (isAdminOrLA) {
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
            RequestsScreenBody(
                paddingValues = paddingValues,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                statusFilter = statusFilter,
                onStatusFilterChange = { statusFilter = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                requests = requests,
                filteredRequests = filteredRequests,
                onRetry = { loadRequests() },
                isFaculty = isFaculty,
                onDeleteRequest =
                    if (isFaculty) {
                        null
                    } else {
                        { requestId -> pendingDeleteRequestId = requestId }
                    },
                onApproveRequest =
                    if (isFaculty) {
                        (
                            { requestId ->
                                updateRequestStatus(requestId, RequestStatus.APPROVED)
                            }
                        )
                    } else {
                        null
                    },
                onRejectRequest =
                    if (isFaculty) {
                        (
                            { requestId ->
                                updateRequestStatus(requestId, RequestStatus.REJECTED)
                            }
                        )
                    } else {
                        null
                    },
                onFulfillRequest =
                    if (isAdminOrLA) {
                        (
                            { requestId ->
                                requests.firstOrNull { it.id == requestId }?.let(::updateRequestStatusPartialIssue)
                            }
                        )
                    } else {
                        null
                    },
                onReturnRequest =
                    if (isAdminOrLA) {
                        (
                            { requestId ->
                                requests.firstOrNull { it.id == requestId }?.let(::updateRequestStatusPartialReturn)
                            }
                        )
                    } else {
                        null
                    },
                onRequestRenew =
                    if (!isFaculty && !isAdminOrLA) {
                        { requestId ->
                            pendingRenewRequestId = requestId
                            renewReasonInput = ""
                        }
                    } else {
                        null
                    },
                onApproveRenew =
                    if (isFaculty) {
                        (
                            { requestId ->
                                updateRequestStatus(requestId, RequestStatus.RENEWED)
                            }
                        )
                    } else {
                        null
                    },
                onShowQr = if (!isFaculty) ({ request -> requestToShowQr = request }) else null,
            )
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
private fun RequestsDialogs(
    pendingDeleteRequestId: String?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    requestToShowQr: Request?,
    onDismissQr: () -> Unit,
    pendingRenewRequestId: String?,
    renewReasonInput: String,
    onRenewReasonChange: (String) -> Unit,
    onConfirmRenew: () -> Unit,
    onDismissRenew: () -> Unit,
    showRequestIdDialog: Boolean,
    showQrScanner: Boolean,
    requestIdInput: String,
    onRequestIdChange: (String) -> Unit,
    onConfirmRequestId: () -> Unit,
    onDismissRequestId: () -> Unit,
    onScanClick: (() -> Unit)?,
    // Partial issue/return dialogs
    pendingPartialIssueRequest: Request?,
    issueItemsInput: Map<String, Int>,
    onIssueItemQtyChange: (String, Int) -> Unit,
    onConfirmPartialIssue: () -> Unit,
    onDismissPartialIssue: () -> Unit,
    pendingPartialReturnRequest: Request?,
    returnItemsInput: Map<String, Int>,
    onReturnItemQtyChange: (String, Int) -> Unit,
    onConfirmPartialReturn: () -> Unit,
    onDismissPartialReturn: () -> Unit,
) {
    if (pendingDeleteRequestId != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Retract request?") },
            text = { Text("This will delete the request permanently") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissDelete()
                        onConfirmDelete(pendingDeleteRequestId)
                    },
                ) {
                    Text("Retract", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            },
        )
    }

    requestToShowQr?.let { request ->
        RequestQrDialog(
            request = request,
            onDismiss = onDismissQr,
        )
    }

    if (pendingRenewRequestId != null) {
        RenewReasonDialog(
            reason = renewReasonInput,
            onReasonChange = onRenewReasonChange,
            onConfirm = onConfirmRenew,
            onDismiss = onDismissRenew,
        )
    }

    if (showRequestIdDialog && !showQrScanner) {
        FulfillByIdDialog(
            requestIdInput = requestIdInput,
            onRequestIdChange = onRequestIdChange,
            dialogTitle = "Scan request QR / ID",
            confirmButtonLabel = "Review",
            onConfirm = onConfirmRequestId,
            onDismiss = onDismissRequestId,
            onScanClick = onScanClick,
        )
    }

    pendingPartialIssueRequest?.let { request ->
        PartialIssueDialog(
            request = request,
            itemsInput = issueItemsInput,
            onItemQtyChange = onIssueItemQtyChange,
            onConfirm = onConfirmPartialIssue,
            onDismiss = onDismissPartialIssue,
        )
    }

    pendingPartialReturnRequest?.let { request ->
        PartialReturnDialog(
            request = request,
            itemsInput = returnItemsInput,
            onItemQtyChange = onReturnItemQtyChange,
            onConfirm = onConfirmPartialReturn,
            onDismiss = onDismissPartialReturn,
        )
    }
}

@Composable
private fun RequestsScreenBody(
    paddingValues: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: RequestStatus?,
    onStatusFilterChange: (RequestStatus?) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    requests: List<Request>,
    filteredRequests: List<Request>,
    onRetry: () -> Unit,
    isFaculty: Boolean,
    onDeleteRequest: ((String) -> Unit)?,
    onApproveRequest: ((String) -> Unit)?,
    onRejectRequest: ((String) -> Unit)?,
    onFulfillRequest: ((String) -> Unit)?,
    onReturnRequest: ((String) -> Unit)?,
    onRequestRenew: ((String) -> Unit)?,
    onApproveRenew: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    Column(modifier = Modifier.padding(paddingValues)) {
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            placeholder = "Search requests...",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        RequestStatusFilterRow(
            statusFilter = statusFilter,
            onStatusFilterChange = onStatusFilterChange,
        )

        RequestsContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            requests = filteredRequests,
            allRequests = requests,
            statusFilter = statusFilter,
            searchQuery = searchQuery.trim(),
            onRetry = onRetry,
            onDeleteRequest = onDeleteRequest,
            onApproveRequest = onApproveRequest,
            onRejectRequest = onRejectRequest,
            onFulfillRequest = onFulfillRequest,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onApproveRenew = onApproveRenew,
            onShowQr = onShowQr,
            isFaculty = isFaculty,
            modifier = Modifier.padding(),
        )
    }
}

@Composable
private fun RequestStatusFilterRow(
    statusFilter: RequestStatus?,
    onStatusFilterChange: (RequestStatus?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        items(REQUEST_STATUS_OPTIONS) { option ->
            val isSelected = statusFilter == option
            TextButton(
                onClick = { onStatusFilterChange(option) },
            ) {
                Text(
                    text = if (option != null) requestStatusDisplayLabel(option) else "All",
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

private val REQUEST_STATUS_OPTIONS: List<RequestStatus?> =
    listOf(null) + RequestStatus.FILTER_OPTIONS

@Composable
fun PartialIssueDialog(
    request: Request,
    itemsInput: Map<String, Int>,
    onItemQtyChange: (String, Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Issue Items (Partial)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                Text(
                    text = request.projectTitle,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Adjust quantities to issue (0 to skip item)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                request.items.forEach { item ->
                    val compId = item.componentId ?: ""
                    val qty = itemsInput[compId] ?: item.quantity
                    val available = item.quantity
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.component?.name ?: compId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Available: $available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = qty.toString(),
                            onValueChange = { text ->
                                val newQty = text.toIntOrNull() ?: 0
                                if (newQty in 0..available) {
                                    onItemQtyChange(compId, newQty)
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Qty") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Issue", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun PartialReturnDialog(
    request: Request,
    itemsInput: Map<String, Int>,
    onItemQtyChange: (String, Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return Items (Partial)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                Text(
                    text = request.projectTitle,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Adjust quantities to return (0 to skip item)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                request.items.filter { it.fulfilledQuantity > 0 }.forEach { item ->
                    val compId = item.componentId ?: ""
                    val fulfilled = item.fulfilledQuantity
                    val qty = itemsInput[compId] ?: fulfilled
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.component?.name ?: compId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Issued: $fulfilled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = qty.toString(),
                            onValueChange = { text ->
                                val newQty = text.toIntOrNull() ?: 0
                                if (newQty in 0..fulfilled) {
                                    onItemQtyChange(compId, newQty)
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Qty") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Return", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun RequestActionSelectionDialog(
    request: Request,
    onIssueMore: () -> Unit,
    onCollectReturn: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Action") },
        text = {
            Column {
                Text(
                    text = "Request for: ${request.projectTitle}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("This request is partially issued. What would you like to do?")
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onIssueMore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Issue More Items")
                }
                OutlinedButton(
                    onClick = onCollectReturn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Collect Return")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

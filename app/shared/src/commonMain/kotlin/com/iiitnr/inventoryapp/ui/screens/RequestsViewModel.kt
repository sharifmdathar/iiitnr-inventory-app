package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.IssueItemPayload
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.ReturnItemPayload
import com.iiitnr.inventoryapp.data.models.UpdateRequestStatusPayload
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.requests.REQUEST_QR_PREFIX
import com.iiitnr.inventoryapp.ui.components.requests.requestStatusActionSnackbarMessage
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class RequestsViewModel(
    private val tokenManager: TokenManager,
) : ViewModel() {
    var requests by mutableStateOf<List<Request>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var currentUser by mutableStateOf<User?>(null)
        private set
    var searchQuery by mutableStateOf("")
    var statusFilter by mutableStateOf<RequestStatus?>(null)

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    val isFaculty get() = currentUser?.role == UserRole.FACULTY
    val isAdminOrLA get() = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.LA

    val filteredRequests: List<Request>
        get() {
            val query = searchQuery.trim()
            return requests.filter { request ->
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
        }

    init {
        loadUserData()
        loadRequests(pollingMode = false)
        startSseOrPolling()
    }

    private fun loadUserData() {
        viewModelScope.launch {
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

    fun loadRequests(pollingMode: Boolean = false) {
        viewModelScope.launch {
            if (pollingMode && isRefreshing) return@launch

            if (pollingMode) {
                isRefreshing = true
            } else {
                if (requests.isEmpty()) {
                    isLoading = true
                } else {
                    isRefreshing = true
                }
                errorMessage = null
            }

            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response = ApiClient.requestApiService.getRequests("Bearer $token")
                    requests = response.requests
                    errorMessage = null
                } else {
                    if (!pollingMode && requests.isEmpty()) {
                        errorMessage = "No authentication token"
                    }
                }
            } catch (e: Throwable) {
                val isAuthError = e is ResponseException && e.response.status == HttpStatusCode.Unauthorized
                if (isAuthError) return@launch

                if (!pollingMode) {
                    if (requests.isEmpty()) {
                        errorMessage =
                            when {
                                e.message?.contains("Unable to resolve host") == true ||
                                    e.message?.contains("Network") == true ||
                                    e.message?.contains("timeout") == true ->
                                    "Network error. Please check your connection."

                                else -> "Error: ${e.message ?: "Failed to load requests"}"
                            }
                    } else {
                        _snackbarMessages.emit("Network error: Using latest data")
                    }
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    private fun startSseOrPolling() {
        viewModelScope.launch {
            tokenManager.token.first()?.let { token ->
                try {
                    ApiClient.requestApiService.streamRequestEvents("Bearer $token").collect {
                        loadRequests(pollingMode = true)
                    }
                } catch (e: Exception) {
                    while (true) {
                        delay(10000.milliseconds)
                        if (errorMessage == null && !isLoading && !isRefreshing) {
                            loadRequests(pollingMode = true)
                        }
                    }
                }
            }
        }
    }

    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
                        _snackbarMessages.emit(message)
                    }
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Throwable) {
                errorMessage = "Error: ${e.message ?: "Failed to update request status"}"
            }
        }
    }

    fun handleQrResult(rawValue: String): Request? {
        val requestId = rawValue.trim().removePrefix(REQUEST_QR_PREFIX).trim()
        if (requestId.isBlank()) return null

        val request = requests.firstOrNull { it.id == requestId }
        if (request == null) {
            viewModelScope.launch {
                _snackbarMessages.emit("Request not found. Refresh and try again.")
            }
            return null
        }
        return request
    }
}

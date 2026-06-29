package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.ui.components.common.EmptyState
import com.iiitnr.inventoryapp.ui.components.common.ErrorContent
import com.iiitnr.inventoryapp.ui.components.common.LoadingIndicator

private fun filteredEmptyMessage(
    statusFilter: String?,
    searchQuery: String,
): String? {
    if (searchQuery.isNotBlank()) {
        return "No requests match your search"
    }
    return when (statusFilter) {
        "PENDING" -> "No pending requests"
        "APPROVED" -> "No approved requests"
        "REJECTED" -> "No rejected requests"
        "FULFILLED" -> "No fulfilled requests"
        "REQUESTED_RENEW" -> "No renewal requests"
        "RENEWED" -> "No renewed requests"
        "RETURNED" -> "No returned requests"
        "EXPIRED" -> "No expired requests"
        null -> null
        else -> "No matching requests"
    }
}

@Composable
fun RequestsContent(
    isLoading: Boolean,
    errorMessage: String?,
    requests: List<Request>,
    allRequests: List<Request> = requests,
    statusFilter: String? = null,
    searchQuery: String = "",
    onRetry: () -> Unit,
    onDeleteRequest: ((String) -> Unit)? = null,
    onApproveRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null,
    onFulfillRequest: ((String) -> Unit)? = null,
    onReturnRequest: ((String) -> Unit)? = null,
    onRequestRenew: ((String) -> Unit)? = null,
    onApproveRenew: ((String) -> Unit)? = null,
    onShowQr: ((Request) -> Unit)? = null,
    isFaculty: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingIndicator()
            errorMessage != null -> ErrorContent(errorMessage, onRetry)
            requests.isEmpty() && allRequests.isEmpty() ->
                EmptyState(
                    message = if (isFaculty) "No pending requests" else "No requests yet",
                    subtitle = if (!isFaculty) "Tap the + button to create a request" else null,
                )

            requests.isEmpty() -> {
                val message =
                    filteredEmptyMessage(statusFilter, searchQuery)
                        ?: if (isFaculty) "No pending requests" else "No matching requests"
                EmptyState(message = message)
            }

            else ->
                RequestsList(
                    requests = requests,
                    onDeleteRequest = onDeleteRequest,
                    onApproveRequest = onApproveRequest,
                    onRejectRequest = onRejectRequest,
                    onFulfillRequest = onFulfillRequest,
                    onReturnRequest = onReturnRequest,
                    onRequestRenew = onRequestRenew,
                    onApproveRenew = onApproveRenew,
                    onShowQr = onShowQr,
                    isFaculty = isFaculty,
                )
        }
    }
}

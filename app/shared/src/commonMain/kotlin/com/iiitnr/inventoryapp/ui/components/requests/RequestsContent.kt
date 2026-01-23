package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.ui.components.common.EmptyState
import com.iiitnr.inventoryapp.ui.components.common.ErrorContent
import com.iiitnr.inventoryapp.ui.components.common.LoadingIndicator

@Composable
fun RequestsContent(
    isLoading: Boolean,
    errorMessage: String?,
    requests: List<Request>,
    onRetry: () -> Unit,
    onDeleteRequest: ((String) -> Unit)? = null,
    onApproveRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null,
    isFaculty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingIndicator()
            errorMessage != null -> ErrorContent(errorMessage, onRetry)
            requests.isEmpty() -> EmptyState(
                message = if (isFaculty) "No pending requests" else "No requests yet",
                subtitle = if (!isFaculty) "Tap the + button to create a request" else null
            )
            else -> RequestsList(
                requests = requests,
                onDeleteRequest = onDeleteRequest,
                onApproveRequest = onApproveRequest,
                onRejectRequest = onRejectRequest,
                isFaculty = isFaculty
            )
        }
    }
}

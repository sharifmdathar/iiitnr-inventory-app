package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.ui.components.common.StatusChip
import com.iiitnr.inventoryapp.ui.components.common.requestStatusColor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.time.Clock

@Composable
fun RequestCard(
    request: Request,
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
    val isDark = isSystemInDarkTheme()
    val cardBackground = requestStatusColor(request.status, isDark).copy(alpha = 0.12f)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardBackground,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            RequestCardHeader(
                request = request,
                isFaculty = isFaculty,
                onDeleteRequest = onDeleteRequest,
                onApproveRequest = onApproveRequest,
                onRejectRequest = onRejectRequest,
                onFulfillRequest = onFulfillRequest,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onApproveRenew = onApproveRenew,
                onShowQr = onShowQr,
            )
            Spacer(modifier = Modifier.height(8.dp))
            RequestCardMeta(request = request)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Components",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            request.items.forEach { item ->
                RequestItemRow(item = item)
            }
        }
    }
}

@Composable
private fun RequestCardHeader(
    request: Request,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.projectTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusChip(status = request.status)
        }
        RequestCardActions(
            request = request,
            isFaculty = isFaculty,
            onDeleteRequest = onDeleteRequest,
            onApproveRequest = onApproveRequest,
            onRejectRequest = onRejectRequest,
            onFulfillRequest = onFulfillRequest,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onApproveRenew = onApproveRenew,
            onShowQr = onShowQr,
        )
    }
}

@Composable
private fun RequestCardMeta(request: Request) {
    Text(
        text = buildDatesLine(request),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (request.user != null || request.targetFaculty != null) {
        Spacer(modifier = Modifier.height(4.dp))
        val requester = request.user?.let { compactUserLabel(it) }
        val faculty = request.targetFaculty?.let { it.name ?: it.email }
        val combined =
            when {
                requester != null && faculty != null -> "$requester  ← $faculty"
                requester != null -> requester
                faculty != null -> "Requested from: $faculty"
                else -> ""
            }
        Text(
            text = combined,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RequestCardActions(
    request: Request,
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
    when (request.status) {
        RequestStatus.PENDING ->
            PendingRequestActions(
                request = request,
                isFaculty = isFaculty,
                onDeleteRequest = onDeleteRequest,
                onApproveRequest = onApproveRequest,
                onRejectRequest = onRejectRequest,
            )

        RequestStatus.APPROVED ->
            ApprovedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onFulfillRequest = onFulfillRequest,
                onShowQr = onShowQr,
            )

        RequestStatus.ISSUED ->
            IssuedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onShowQr = onShowQr,
            )

        RequestStatus.PARTIALLY_ISSUED ->
            IssuedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onShowQr = onShowQr,
                onFulfillRequest = onFulfillRequest,
            )

        RequestStatus.REQUESTED_RENEW ->
            RequestedRenewActions(
                request = request,
                isFaculty = isFaculty,
                onApproveRenew = onApproveRenew,
            )

        RequestStatus.RENEWED,
        RequestStatus.PARTIALLY_RETURNED,
        ->
            RenewedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onShowQr = onShowQr,
            )

        RequestStatus.EXPIRED ->
            ExpiredRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onShowQr = onShowQr,
            )

        else -> {}
    }
}

@Composable
private fun PendingRequestActions(
    request: Request,
    isFaculty: Boolean,
    onDeleteRequest: ((String) -> Unit)?,
    onApproveRequest: ((String) -> Unit)?,
    onRejectRequest: ((String) -> Unit)?,
) {
    if (isFaculty && onApproveRequest != null && onRejectRequest != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onRejectRequest(request.id) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Reject request",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            IconButton(onClick = { onApproveRequest(request.id) }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Approve request",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    } else if (!isFaculty && onDeleteRequest != null) {
        IconButton(onClick = { onDeleteRequest(request.id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Retract request",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ApprovedRequestActions(
    request: Request,
    isFaculty: Boolean,
    onFulfillRequest: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!isFaculty && onShowQr != null) {
            IconButton(onClick = { onShowQr(request) }) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Show QR for LA to scan",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onFulfillRequest != null) {
            IconButton(onClick = { onFulfillRequest(request.id) }) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Issue request",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun IssuedRequestActions(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onRequestRenew: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
    onFulfillRequest: ((String) -> Unit)? = null,
) {
    RequestLifecycleActionRow(
        request = request,
        isFaculty = isFaculty,
        onReturnRequest = onReturnRequest,
        onRequestRenew = onRequestRenew,
        onShowQr = onShowQr,
        returnContentDescription = "Record return to inventory",
    )
    if (onFulfillRequest != null) {
        IconButton(onClick = { onFulfillRequest(request.id) }) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Issue more items",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RequestedRenewActions(
    request: Request,
    isFaculty: Boolean,
    onApproveRenew: ((String) -> Unit)?,
) {
    if (isFaculty && onApproveRenew != null) {
        IconButton(onClick = { onApproveRenew(request.id) }) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Approve renewal request",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RenewedRequestActions(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onRequestRenew: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    RequestLifecycleActionRow(
        request = request,
        isFaculty = isFaculty,
        onReturnRequest = onReturnRequest,
        onRequestRenew = onRequestRenew,
        onShowQr = onShowQr,
        returnContentDescription = "Record return to inventory",
    )
}

@Composable
private fun ExpiredRequestActions(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    RequestLifecycleActionRow(
        request = request,
        isFaculty = isFaculty,
        onReturnRequest = onReturnRequest,
        onRequestRenew = null,
        onShowQr = onShowQr,
        returnContentDescription = "Record overdue return to inventory",
        returnTint = MaterialTheme.colorScheme.error,
        showRenew = false,
        qrContentDescription = "Show QR for LA to scan when returning overdue items",
    )
}

@Composable
private fun RequestLifecycleActionRow(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onRequestRenew: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
    returnContentDescription: String,
    returnTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    showRenew: Boolean = true,
    qrContentDescription: String = "Show QR for LA to scan when returning items",
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showRenew && !isFaculty && onRequestRenew != null) {
            IconButton(onClick = { onRequestRenew(request.id) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Request renewal",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (!isFaculty && onShowQr != null) {
            IconButton(onClick = { onShowQr(request) }) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = qrContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onReturnRequest != null) {
            IconButton(onClick = { onReturnRequest(request.id) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                    contentDescription = returnContentDescription,
                    tint = returnTint,
                )
            }
        }
    }
}

fun getRelativeDays(dateTimeString: String?): String {
    if (dateTimeString == null) return ""
    val dateTime = LocalDateTime.parse(dateTimeString.replace(' ', 'T'))
    val date = dateTime.date

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val days = today.daysUntil(date)

    return if (days == 0) {
        "Today"
    } else if (days < 0) {
        "${abs(days)}d ago"
    } else {
        "Due in ${abs(days)}d"
    }
}

fun String.toDisplayLabel(): String =
    lowercase().split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }

fun requestStatusDisplayLabel(status: RequestStatus): String =
    when (status) {
        RequestStatus.REQUESTED_RENEW -> "Renewal Requested"
        RequestStatus.RENEWED -> "Renewed"
        else -> status.name.toDisplayLabel()
    }

fun buildUserDetailsLabel(
    prefix: String,
    user: User,
): String {
    val details = mutableListOf<String>()
    user.name?.takeIf { it.isNotBlank() }?.let { details += "Name: $it" }
    user.batch?.takeIf { it.isNotBlank() }?.let { details += "Batch: $it" }
    user.branch?.takeIf { it.isNotBlank() }?.let { details += "Branch: $it" }

    val label = details.joinToString(" • ")
    return if (label.isBlank()) {
        "$prefix: ${user.email}"
    } else {
        "$prefix: $label"
    }
}

fun compactUserLabel(user: User): String {
    val displayName = user.name?.takeIf { it.isNotBlank() } ?: user.email
    val branch = user.branch?.takeIf { it.isNotBlank() }
    val batch = user.batch?.takeIf { it.isNotBlank() }?.replace("-", "–")
    val suffix = listOfNotNull(branch, batch).joinToString(" ")
    return if (suffix.isBlank()) displayName else "$displayName ($suffix)"
}

fun buildDatesLine(request: Request): String {
    val tokens = mutableListOf<String>()
    request.returnDueAt?.let {
        var s = getRelativeDays(it)
        if (s.startsWith("Due in ")) s = "Due: " + s.removePrefix("Due in ")
        tokens += s
    }
    tokens += "Created: ${getRelativeDays(request.createdAt)}"
    request.fulfilledAt?.let { tokens += "Fulfilled: ${getRelativeDays(it)}" }
    request.lastRenewDate?.let { tokens += "Renewed: ${getRelativeDays(it)}" }
    return tokens.joinToString(" · ")
}

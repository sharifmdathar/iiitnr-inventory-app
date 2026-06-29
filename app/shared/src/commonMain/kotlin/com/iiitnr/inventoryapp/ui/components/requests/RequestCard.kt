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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.ui.components.common.StatusChip
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.time.Clock

private val cardBackgroundRejectedLight = Color(0xFFFFEBEE)
private val cardBackgroundApprovedLight = Color(0xFFE8F5E9)
private val cardBackgroundFulfilledLight = Color(0xFFE3F2FD)
private val cardBackgroundReturnedLight = Color(0xFFE8EAF6)
private val cardBackgroundRequestedRenewLight = Color(0xFFFFF3E0)
private val cardBackgroundRenewedLight = Color(0xFFE0F2F1)
private val cardBackgroundExpiredLight = Color(0xFFFFE4E6)
private val cardBackgroundPendingLight = Color(0xFFFFF8E1)

private val cardBackgroundRejectedDark = Color(0xFF3D2020)
private val cardBackgroundApprovedDark = Color(0xFF1E2E20)
private val cardBackgroundFulfilledDark = Color(0xFF1A2332)
private val cardBackgroundReturnedDark = Color(0xFF1E1E2E)
private val cardBackgroundRequestedRenewDark = Color(0xFF2E241A)
private val cardBackgroundRenewedDark = Color(0xFF1A2E2C)
private val cardBackgroundExpiredDark = Color(0xFF3F1D24)
private val cardBackgroundPendingDark = Color(0xFF2E2A1A)

private val statusApprovedLight = Color(0xFF2E7D32)
private val statusApprovedDark = Color(0xFF81C784)
private val statusFulfilledLight = Color(0xFF1976D2)
private val statusFulfilledDark = Color(0xFF64B5F6)
private val statusReturnedLight = Color(0xFF5E35B1)
private val statusReturnedDark = Color(0xFFB39DDB)
private val statusRequestedRenewLight = Color(0xFFE65100)
private val statusRequestedRenewDark = Color(0xFFFFB74D)
private val statusRenewedLight = Color(0xFF00695C)
private val statusRenewedDark = Color(0xFF4DB6AC)

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
    val cardBackground =
        when (request.status) {
            "REJECTED" -> if (isDark) cardBackgroundRejectedDark else cardBackgroundRejectedLight
            "APPROVED" -> if (isDark) cardBackgroundApprovedDark else cardBackgroundApprovedLight
            "FULFILLED" -> if (isDark) cardBackgroundFulfilledDark else cardBackgroundFulfilledLight
            "RETURNED" -> if (isDark) cardBackgroundReturnedDark else cardBackgroundReturnedLight
            "REQUESTED_RENEW" -> if (isDark) cardBackgroundRequestedRenewDark else cardBackgroundRequestedRenewLight

            "RENEWED" -> if (isDark) cardBackgroundRenewedDark else cardBackgroundRenewedLight
            "EXPIRED" -> if (isDark) cardBackgroundExpiredDark else cardBackgroundExpiredLight
            else -> if (isDark) cardBackgroundPendingDark else cardBackgroundPendingLight
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardBackground,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
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
        "PENDING" ->
            PendingRequestActions(
                request = request,
                isFaculty = isFaculty,
                onDeleteRequest = onDeleteRequest,
                onApproveRequest = onApproveRequest,
                onRejectRequest = onRejectRequest,
            )

        "APPROVED" ->
            ApprovedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onFulfillRequest = onFulfillRequest,
                onShowQr = onShowQr,
            )

        "FULFILLED" ->
            FulfilledRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onShowQr = onShowQr,
            )

        "REQUESTED_RENEW" ->
            RequestedRenewActions(
                request = request,
                isFaculty = isFaculty,
                onApproveRenew = onApproveRenew,
            )

        "RENEWED" ->
            RenewedRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onRequestRenew = onRequestRenew,
                onShowQr = onShowQr,
            )

        "EXPIRED" ->
            ExpiredRequestActions(
                request = request,
                isFaculty = isFaculty,
                onReturnRequest = onReturnRequest,
                onShowQr = onShowQr,
            )
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
                    contentDescription = "Show QR for TA to scan",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onFulfillRequest != null) {
            IconButton(onClick = { onFulfillRequest(request.id) }) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Fulfill request",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FulfilledRequestActions(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onRequestRenew: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!isFaculty && onRequestRenew != null) {
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
                    contentDescription = "Show QR for TA to scan when returning items",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onReturnRequest != null) {
            IconButton(onClick = { onReturnRequest(request.id) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                    contentDescription = "Record return to inventory",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!isFaculty && onRequestRenew != null) {
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
                    contentDescription = "Show QR for TA to scan when returning items",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onReturnRequest != null) {
            IconButton(onClick = { onReturnRequest(request.id) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                    contentDescription = "Record return to inventory",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ExpiredRequestActions(
    request: Request,
    isFaculty: Boolean,
    onReturnRequest: ((String) -> Unit)?,
    onShowQr: ((Request) -> Unit)?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!isFaculty && onShowQr != null) {
            IconButton(onClick = { onShowQr(request) }) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Show QR for TA to scan when returning overdue items",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onReturnRequest != null) {
            IconButton(onClick = { onReturnRequest(request.id) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                    contentDescription = "Record overdue return to inventory",
                    tint = MaterialTheme.colorScheme.error,
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

fun requestStatusDisplayLabel(status: String): String =
    when (status) {
        "REQUESTED_RENEW" -> "Renewal Requested"
        "RENEWED" -> "Renewed"
        else -> status.toDisplayLabel()
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

fun buildDatesLine(request: com.iiitnr.inventoryapp.data.models.Request): String {
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

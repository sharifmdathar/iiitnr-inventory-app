package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestItem
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.ui.components.common.InfoChip
import com.iiitnr.inventoryapp.ui.components.common.StatusChip
import com.iiitnr.inventoryapp.ui.components.common.requestStatusColor
import com.iiitnr.inventoryapp.ui.components.components.ComponentImage
import com.iiitnr.inventoryapp.ui.theme.AppTheme
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
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        RequestDetailDialog(
            request = request,
            onDeleteRequest = onDeleteRequest,
            onApproveRequest = onApproveRequest,
            onRejectRequest = onRejectRequest,
            onFulfillRequest = onFulfillRequest,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onApproveRenew = onApproveRenew,
            onShowQr = onShowQr,
            isFaculty = isFaculty,
            onDismiss = { showDialog = false },
        )
    }

    val isDark = isSystemInDarkTheme()
    val cardBackground = requestStatusColor(request.status, isDark).copy(alpha = 0.12f)

    Card(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
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
fun RequestDetailDialog(
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
    onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss, title = {
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
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }, text = {
        RequestDetailDialogContent(
            request
        )
    })
}

@Composable
fun RequestDetailDialogContent(
    request: Request,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        RequestDetailDatesFlow(request = request)

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            request.user?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Requester",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = compactUserLabel(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            request.targetFaculty?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Faculty",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = compactUserLabel(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Components",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        request.items.forEach { item ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComponentImage(
                        imageUrl = item.component?.imageUrl,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.component?.name ?: item.componentId ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                        item.component?.description?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip("Returned", item.returnedQuantity.toString())
                    InfoChip("Issued", item.fulfilledQuantity.toString())
                    InfoChip("Requested", item.quantity.toString())
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RequestDetailDatesFlow(request: Request) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateItem(
            icon = Icons.Default.History,
            label = "Created",
            value = getRelativeDays(request.createdAt)
        )

        request.fulfilledAt?.let {
            DateItem(
                icon = Icons.Default.CheckCircle,
                label = "Fulfilled",
                value = getRelativeDays(it),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        request.returnDueAt?.let {
            if (request.status != RequestStatus.RETURNED) {
                DateItem(
                    icon = Icons.Default.Timer,
                    label = "Return Due",
                    value = getRelativeDays(it),
                    tint = if (getRelativeDays(it).contains("ago")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
        }

        request.returnedAt?.let {
            DateItem(
                icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                label = "Returned",
                value = getRelativeDays(it),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        request.lastRenewDate?.let {
            DateItem(
                icon = Icons.Default.Refresh,
                label = "Last Renewed",
                value = getRelativeDays(it),
                tint = MaterialTheme.colorScheme.tertiary
            )
            request.lastRenewReason?.takeIf { reason -> reason.isNotBlank() }?.let { reason ->
                Row(
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reason: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DateItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
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
        val combined = when {
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
        RequestStatus.PENDING -> PendingRequestActions(
            request = request,
            isFaculty = isFaculty,
            onDeleteRequest = onDeleteRequest,
            onApproveRequest = onApproveRequest,
            onRejectRequest = onRejectRequest,
        )

        RequestStatus.APPROVED -> ApprovedRequestActions(
            request = request,
            isFaculty = isFaculty,
            onFulfillRequest = onFulfillRequest,
            onShowQr = onShowQr,
        )

        RequestStatus.ISSUED -> IssuedRequestActions(
            request = request,
            isFaculty = isFaculty,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onShowQr = onShowQr,
        )

        RequestStatus.PARTIALLY_ISSUED -> IssuedRequestActions(
            request = request,
            isFaculty = isFaculty,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onShowQr = onShowQr,
            onFulfillRequest = onFulfillRequest,
        )

        RequestStatus.REQUESTED_RENEW -> RequestedRenewActions(
            request = request,
            isFaculty = isFaculty,
            onApproveRenew = onApproveRenew,
        )

        RequestStatus.RENEWED,
        RequestStatus.PARTIALLY_RETURNED,
            -> RenewedRequestActions(
            request = request,
            isFaculty = isFaculty,
            onReturnRequest = onReturnRequest,
            onRequestRenew = onRequestRenew,
            onShowQr = onShowQr,
        )

        RequestStatus.EXPIRED -> ExpiredRequestActions(
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
    returnTint: Color = MaterialTheme.colorScheme.primary,
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

fun requestStatusDisplayLabel(status: RequestStatus): String = when (status) {
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
        if (request.status != RequestStatus.RETURNED) {
            var s = getRelativeDays(it)
            if (s.startsWith("Due in ")) s = "Due: " + s.removePrefix("Due in ")
            tokens += s
        }
    }
    tokens += "Created: ${getRelativeDays(request.createdAt)}"
    request.fulfilledAt?.let { tokens += "Fulfilled: ${getRelativeDays(it)}" }
    request.lastRenewDate?.let { tokens += "Renewed: ${getRelativeDays(it)}" }
    request.returnedAt?.let { tokens += "Returned: ${getRelativeDays(it)}" }
    return tokens.joinToString(" · ")
}

@Preview
@Composable
fun RequestCardPreview() {
    val sampleUser = User(
        id = "s1",
        email = "john24100@iiitnr.edu.in",
        name = "John Doe",
        role = UserRole.STUDENT,
        batch = "2024-28",
        branch = "CSE",
    )

    val sampleFaculty = User(
        id = "f1",
        email = "faculty@iiitnr.edu.in",
        name = "Faculty A",
        role = UserRole.FACULTY,
    )

    val sampleComponent = Component(
        id = "c1",
        name = "Arduino Uno",
        totalQuantity = 10,
        availableQuantity = 5,
        createdAt = "2023-08-11T10:00:00",
        updatedAt = "2023-08-11T10:00:00",
    )

    val sampleRequest = Request(
        id = "r1",
        userId = "s1",
        targetFacultyId = "f1",
        projectTitle = "IoT Weather Station",
        status = RequestStatus.PENDING,
        createdAt = "2026-07-11T10:00:00",
        updatedAt = "2026-07-11T10:00:00",
        items = listOf(
            RequestItem(
                id = "ri1",
                requestId = "r1",
                componentId = "c1",
                quantity = 2,
                component = sampleComponent,
            ),
            RequestItem(
                id = "ri2",
                requestId = "r1",
                componentId = "c1",
                quantity = 5,
                fulfilledQuantity = 3,
                returnedQuantity = 2,
                component = sampleComponent.copy(name = "Flame Sensor"),
            ),
        ),
        user = sampleUser,
        targetFaculty = sampleFaculty,
    )

    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RequestCard(
                    request = sampleRequest,
                    onDeleteRequest = {},
                    isFaculty = false,
                )

                RequestCard(
                    request = sampleRequest.copy(
                        status = RequestStatus.ISSUED,
                        returnDueAt = "2023-08-20T10:00:00",
                    ),
                    onReturnRequest = {},
                    onShowQr = {},
                    isFaculty = false,
                )

                RequestCard(
                    request = sampleRequest.copy(
                        status = RequestStatus.PARTIALLY_RETURNED,
                    ),
                    onApproveRequest = {},
                    onRejectRequest = {},
                    isFaculty = true,
                )
            }
        }
    }
}

@Preview
@Composable
fun RequestDetailDialogContentPreview() {
    val sampleStudent = User(
        id = "s1",
        name = "John Doe",
        role = UserRole.STUDENT,
        email = "student24100@iiitnr.edu.in",
        batch = "2024-28",
        branch = "CSE"
    )

    val sampleFaculty = User(
        id = "f1", name = "Faculty A", role = UserRole.FACULTY, email = "faculty@iiitnr.edu.in"
    )

    val sampleComponent = Component(
        id = "c1",
        name = "Arduino Uno",
        description = "Classic microcontroller board for IoT projects",
        totalQuantity = 10,
        availableQuantity = 5,
        createdAt = "2023-08-11T10:00:00",
        updatedAt = "2023-08-11T10:00:00",
    )

    val sampleRequest = Request(
        id = "r1",
        userId = "s1",
        targetFacultyId = "f1",
        projectTitle = "IoT Weather Station",
        status = RequestStatus.RENEWED,
        createdAt = "2026-07-11T10:00:00",
        updatedAt = "2026-08-11T10:00:00",
        fulfilledAt = "2026-07-12T10:00:00",
        lastRenewDate = "2026-08-01T10:00:00",
        lastRenewReason = "Need more time for field testing.",
        returnedAt = "2026-08-11T15:00:00",
        items = listOf(
            RequestItem(
                id = "ri1",
                requestId = "r1",
                componentId = "c1",
                quantity = 5,
                fulfilledQuantity = 3,
                returnedQuantity = 1,
                component = sampleComponent,
            ),
            RequestItem(
                id = "ri2",
                requestId = "r1",
                componentId = "c1",
                quantity = 2,
                fulfilledQuantity = 2,
                returnedQuantity = 2,
                component = sampleComponent.copy(
                    name = "DHT11 Sensor", description = "Temperature and Humidity sensor"
                ),
            ),
            RequestItem(
                id = "ri3",
                requestId = "r1",
                componentId = "c1",
                quantity = 1,
                fulfilledQuantity = 0,
                returnedQuantity = 0,
                component = sampleComponent.copy(name = "ESP8266", description = "WiFi Module"),
            ),
        ),
        user = sampleStudent,
        targetFaculty = sampleFaculty,
        returnDueAt = "2026-08-20T10:00:00",
    )

    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RequestDetailDialogContent(
                request = sampleRequest, modifier = Modifier.padding(16.dp)
            )
        }
    }
}

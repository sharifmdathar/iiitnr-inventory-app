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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QrCode2
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

private val cardBackgroundRejectedLight = Color(0xFFFFEBEE)
private val cardBackgroundApprovedLight = Color(0xFFE8F5E9)
private val cardBackgroundFulfilledLight = Color(0xFFE3F2FD)
private val cardBackgroundPendingLight = Color(0xFFFFF8E1)

private val cardBackgroundRejectedDark = Color(0xFF3D2020)
private val cardBackgroundApprovedDark = Color(0xFF1E2E20)
private val cardBackgroundFulfilledDark = Color(0xFF1A2332)
private val cardBackgroundPendingDark = Color(0xFF2E2A1A)

private val statusApprovedLight = Color(0xFF2E7D32)
private val statusApprovedDark = Color(0xFF81C784)
private val statusFulfilledLight = Color(0xFF1976D2)
private val statusFulfilledDark = Color(0xFF64B5F6)

@Composable
fun RequestCard(
    request: Request,
    onDeleteRequest: ((String) -> Unit)? = null,
    onApproveRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null,
    onFulfillRequest: ((String) -> Unit)? = null,
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
            else -> if (isDark) cardBackgroundPendingDark else cardBackgroundPendingLight
        }
    val statusSubtitleColor =
        when (request.status) {
            "REJECTED" -> MaterialTheme.colorScheme.error
            "APPROVED" -> if (isDark) statusApprovedDark else statusApprovedLight
            "FULFILLED" -> if (isDark) statusFulfilledDark else statusFulfilledLight
            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = request.projectTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (request.status == "PENDING") {
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
                } else if (request.status == "APPROVED") {
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
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = request.status.lowercase().replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyMedium,
                color = statusSubtitleColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Created: ${request.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (request.user != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requested by: ${request.user.name ?: request.user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (request.targetFaculty != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requested from: ${request.targetFaculty.name ?: request.targetFaculty.email}",
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

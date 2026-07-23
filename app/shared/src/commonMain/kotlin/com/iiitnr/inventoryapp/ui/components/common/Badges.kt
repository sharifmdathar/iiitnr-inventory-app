package com.iiitnr.inventoryapp.ui.components.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.UserRole
import com.iiitnr.inventoryapp.ui.theme.SemanticWarning

@Composable
fun StatusChip(
    status: RequestStatus,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val color = requestStatusColor(status = status, isDark = isDark)

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .semantics { contentDescription = "Request status: ${requestStatusLabel(status)}" },
        color = color.copy(alpha = if (isDark) 0.24f else 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = requestStatusLabel(status),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun requestStatusColor(
    status: RequestStatus,
    isDark: Boolean = isSystemInDarkTheme(),
): Color =
    when (status) {
        RequestStatus.PENDING -> if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
        RequestStatus.APPROVED -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1A56DB)
        RequestStatus.ISSUED -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
        RequestStatus.REQUESTED_RENEW -> if (isDark) Color(0xFFFDBA74) else Color(0xFFEA580C)
        RequestStatus.RENEWED -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
        RequestStatus.RETURNED -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280)
        RequestStatus.EXPIRED -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        RequestStatus.REJECTED -> MaterialTheme.colorScheme.error
    }

fun requestStatusLabel(status: RequestStatus): String =
    when (status) {
        RequestStatus.REQUESTED_RENEW -> "Renewal Requested"
        else ->
            status.name
                .lowercase()
                .split('_')
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
    }

@Composable
fun userRoleColor(
    role: UserRole,
    isDark: Boolean = isSystemInDarkTheme(),
): Color =
    when (role) {
        UserRole.ADMIN -> if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
        UserRole.FACULTY -> if (isDark) Color(0xFF42A5F5) else Color(0xFF1565C0)
        UserRole.STUDENT -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
        UserRole.LA -> if (isDark) Color(0xFFFF9800) else Color(0xFFE65100)
        UserRole.PENDING -> if (isDark) Color(0xFFB0BEC5) else Color(0xFF78909C)
    }

@Composable
fun auditActionColor(
    action: String?,
    isDark: Boolean = isSystemInDarkTheme(),
): Color? =
    when (action?.uppercase()) {
        "CREATE" -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
        "UPDATE" -> if (isDark) Color(0xFF42A5F5) else Color(0xFF1565C0)
        "DELETE" -> if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
        "LOGIN" -> if (isDark) Color(0xFFCE93D8) else Color(0xFF6A1B9A)
        "LOGOUT" -> if (isDark) Color(0xFFB0BEC5) else Color(0xFF78909C)
        "REQUEST_STATUS_CHANGE" -> if (isDark) Color(0xFFFF9800) else Color(0xFFE65100)
        "INVENTORY_ADJUST" -> if (isDark) Color(0xFF26C6DA) else Color(0xFF00838F)
        else -> null
    }

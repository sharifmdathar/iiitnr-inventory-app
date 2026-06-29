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
import com.iiitnr.inventoryapp.ui.theme.SemanticWarning

@Composable
fun StatusChip(
    status: String,
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
    status: String,
    isDark: Boolean = isSystemInDarkTheme(),
): Color =
    when (status.uppercase()) {
        "PENDING" -> if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
        "APPROVED" -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1A56DB)
        "FULFILLED" -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
        "REQUESTED_RENEW" -> if (isDark) Color(0xFFFDBA74) else Color(0xFFEA580C)
        "RENEWED" -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
        "RETURNED" -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280)
        "EXPIRED" -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        "REJECTED" -> MaterialTheme.colorScheme.error
        else -> SemanticWarning
    }

fun requestStatusLabel(status: String): String =
    when (status.uppercase()) {
        "REQUESTED_RENEW" -> "Renewal Requested"
        else ->
            status
                .lowercase()
                .split('_')
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
    }

package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.AuditLogEntry
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar
import com.iiitnr.inventoryapp.ui.theme.SemanticAudit
import com.iiitnr.inventoryapp.ui.theme.SemanticDanger
import com.iiitnr.inventoryapp.ui.theme.SemanticIdentity
import com.iiitnr.inventoryapp.ui.theme.SemanticInfo
import com.iiitnr.inventoryapp.ui.theme.SemanticNeutral
import com.iiitnr.inventoryapp.ui.theme.SemanticSuccess
import com.iiitnr.inventoryapp.ui.theme.SemanticWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private val cardBackgroundApprovedLight = Color(0xFFE8F5E9)
private val cardBackgroundFulfilledLight = Color(0xFFE3F2FD)
private val cardBackgroundRejectedLight = Color(0xFFFFEBEE)
private val cardBackgroundRenewedLight = Color(0xFFE0F2F1)
private val cardBackgroundPendingLight = Color(0xFFFFF8E1)
private val cardBackgroundRequestedRenewLight = Color(0xFFFFF3E0)
private val cardBackgroundReturnedLight = Color(0xFFE8EAF6)

private val cardBackgroundApprovedDark = Color(0xFF1E2E20)
private val cardBackgroundFulfilledDark = Color(0xFF1A2332)
private val cardBackgroundRejectedDark = Color(0xFF3D2020)
private val cardBackgroundRenewedDark = Color(0xFF1A2E2C)
private val cardBackgroundPendingDark = Color(0xFF2E2A1A)
private val cardBackgroundRequestedRenewDark = Color(0xFF2E241A)
private val cardBackgroundReturnedDark = Color(0xFF1E1E2E)

private fun getActionBackground(
    action: String?,
    isDark: Boolean,
): Color =
    when (action) {
        "CREATE" -> if (isDark) cardBackgroundApprovedDark else cardBackgroundApprovedLight
        "UPDATE" -> if (isDark) cardBackgroundFulfilledDark else cardBackgroundFulfilledLight
        "DELETE" -> if (isDark) cardBackgroundRejectedDark else cardBackgroundRejectedLight
        "LOGIN" -> if (isDark) cardBackgroundReturnedDark else cardBackgroundReturnedLight
        "LOGOUT" -> if (isDark) cardBackgroundPendingDark else cardBackgroundPendingLight
        "REQUEST_STATUS_CHANGE" -> if (isDark) cardBackgroundRequestedRenewDark else cardBackgroundRequestedRenewLight
        "INVENTORY_ADJUST" -> if (isDark) cardBackgroundRenewedDark else cardBackgroundRenewedLight
        else -> if (isDark) Color(0xFF1F1F1F) else Color(0xFFFAFAFA)
    }

private val actionColors =
    mapOf(
        "CREATE" to SemanticSuccess,
        "UPDATE" to SemanticInfo,
        "DELETE" to SemanticDanger,
        "LOGIN" to SemanticIdentity,
        "LOGOUT" to SemanticNeutral,
        "REQUEST_STATUS_CHANGE" to SemanticWarning,
        "INVENTORY_ADJUST" to SemanticAudit,
    )

private val actionLabels =
    mapOf(
        "CREATE" to "Create",
        "UPDATE" to "Update",
        "DELETE" to "Delete",
        "LOGIN" to "Login",
        "LOGOUT" to "Logout",
        "REQUEST_STATUS_CHANGE" to "Status Change",
        "INVENTORY_ADJUST" to "Inventory Adj.",
    )

private val allActions =
    listOf(
        "CREATE",
        "UPDATE",
        "DELETE",
        "LOGIN",
        "LOGOUT",
        "REQUEST_STATUS_CHANGE",
        "INVENTORY_ADJUST",
    )

private val jsonParser = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
) {
    var logs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalCount by remember { mutableStateOf(0) }
    var currentOffset by remember { mutableStateOf(0) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<AuditLogEntry?>(null) }
    val pageSize = 50
    val scope = rememberCoroutineScope()

    fun loadLogs() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response =
                        ApiClient.auditLogApiService.getAuditLogs(
                            token = "Bearer $token",
                            limit = pageSize,
                            offset = currentOffset,
                            action = selectedAction,
                        )
                    logs = response.logs
                    totalCount = response.pagination.total
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Throwable) {
                errorMessage = e.message ?: "Failed to load audit logs"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentOffset, selectedAction) {
        loadLogs()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Audit Log",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            AuditFilterBar(
                selectedAction = selectedAction,
                onActionSelected = {
                    selectedAction = it
                    currentOffset = 0
                },
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { loadLogs() }) { Text("Retry") }
                        }
                    }
                }

                logs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No audit logs found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(logs, key = { it.id }) { entry ->
                            AuditLogCard(
                                entry = entry,
                                onClick = { selectedEntry = entry },
                            )
                        }
                    }

                    AuditPaginationBar(
                        currentOffset = currentOffset,
                        pageSize = pageSize,
                        totalCount = totalCount,
                        onPrevious = { currentOffset = (currentOffset - pageSize).coerceAtLeast(0) },
                        onNext = { currentOffset += pageSize },
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        AuditDetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuditFilterBar(
    selectedAction: String?,
    onActionSelected: (String?) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = selectedAction == null,
            onClick = { onActionSelected(null) },
            label = { Text("All", fontSize = 12.sp) },
        )
        allActions.forEach { action ->
            val color = actionColors[action] ?: MaterialTheme.colorScheme.primary
            FilterChip(
                selected = selectedAction == action,
                onClick = {
                    onActionSelected(if (selectedAction == action) null else action)
                },
                label = { Text(actionLabels[action] ?: action, fontSize = 12.sp) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.15f),
                        selectedLabelColor = color,
                    ),
            )
        }
    }
}

@Composable
private fun AuditLogCard(
    entry: AuditLogEntry,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val actionColor = actionColors[entry.action] ?: MaterialTheme.colorScheme.primary
    val cardBackground = getActionBackground(entry.action, isDark)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardBackground,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Action badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = actionLabels[entry.action] ?: entry.action,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = actionColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                // Timestamp
                Text(
                    text = formatAuditTimestamp(entry.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))

            // Actor
            Text(
                text = entry.userName ?: entry.userEmail ?: "System",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.userName != null && entry.userEmail != null) {
                Text(
                    text = entry.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Target
            if (entry.entityType != null) {
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = entry.entityType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (entry.entityId != null) {
                        Text(
                            text = " #${entry.entityId.take(8)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditPaginationBar(
    currentOffset: Int,
    pageSize: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val currentPage = (currentOffset / pageSize) + 1
    val totalPages = ((totalCount + pageSize - 1) / pageSize).coerceAtLeast(1)

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = currentOffset > 0,
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            Text(
                text = "Page $currentPage of $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            IconButton(
                onClick = onNext,
                enabled = currentOffset + pageSize < totalCount,
            ) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$totalCount total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuditDetailDialog(
    entry: AuditLogEntry,
    onDismiss: () -> Unit,
) {
    val actionColor = actionColors[entry.action] ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = actionLabels[entry.action] ?: entry.action,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = actionColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Audit Detail",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DetailRow("Actor", entry.userName ?: entry.userEmail ?: "System")
                if (entry.userName != null && entry.userEmail != null) {
                    DetailRow("Email", entry.userEmail)
                }
                DetailRow("Timestamp", entry.createdAt)
                if (entry.entityType != null) {
                    DetailRow("Entity Type", entry.entityType)
                }
                if (entry.entityId != null) {
                    DetailRow("Entity ID", entry.entityId)
                }
                if (entry.ipAddress != null) {
                    DetailRow("IP Address", entry.ipAddress)
                }
                if (entry.userAgent != null) {
                    DetailRow("User Agent", entry.userAgent)
                }

                // Diff viewer for old/new values
                if (entry.oldValues != null || entry.newValues != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Changes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    DiffViewer(
                        oldJson = entry.oldValues,
                        newJson = entry.newValues,
                    )
                }

                if (entry.metadata != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow("Metadata", entry.metadata)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DiffViewer(
    oldJson: String?,
    newJson: String?,
) {
    val oldMap = parseJsonMap(oldJson)
    val newMap = parseJsonMap(newJson)
    val allKeys = (oldMap.keys + newMap.keys).distinct().sorted()

    if (allKeys.isEmpty()) {
        Text(
            "No structured changes available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        allKeys.forEach { key ->
            val oldVal = oldMap[key]
            val newVal = newMap[key]
            val changed = oldVal != newVal

            if (changed) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(120.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (oldVal != null) {
                            Text(
                                text = "- $oldVal",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = SemanticDanger,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (newVal != null) {
                            Text(
                                text = "+ $newVal",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = SemanticSuccess,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Show unchanged count
        val unchangedCount =
            allKeys.count { oldMap[it] == newMap[it] && oldMap.containsKey(it) && newMap.containsKey(it) }
        if (unchangedCount > 0) {
            Text(
                text = "$unchangedCount unchanged field(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun parseJsonMap(json: String?): Map<String, String> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val obj = jsonParser.decodeFromString<JsonObject>(json)
        obj.mapValues { (_, v) -> v.jsonPrimitive.content }
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun formatAuditTimestamp(iso: String): String {
    // Simple formatting: extract date and time portion
    return try {
        val cleaned = iso.replace("T", " ").take(19)
        cleaned
    } catch (_: Exception) {
        iso
    }
}

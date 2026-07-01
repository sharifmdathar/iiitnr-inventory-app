package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.models.UpdateUserRequest
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar
import com.iiitnr.inventoryapp.ui.components.common.SearchBar
import com.iiitnr.inventoryapp.ui.theme.SemanticDanger
import com.iiitnr.inventoryapp.ui.theme.SemanticInfo
import com.iiitnr.inventoryapp.ui.theme.SemanticNeutral
import com.iiitnr.inventoryapp.ui.theme.SemanticSuccess
import com.iiitnr.inventoryapp.ui.theme.SemanticWarning
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val cardBackgroundRejectedLight = Color(0xFFFFEBEE)
private val cardBackgroundFulfilledLight = Color(0xFFE3F2FD)
private val cardBackgroundApprovedLight = Color(0xFFE8F5E9)
private val cardBackgroundRequestedRenewLight = Color(0xFFFFF3E0)
private val cardBackgroundPendingLight = Color(0xFFFFF8E1)

private val cardBackgroundRejectedDark = Color(0xFF3D2020)
private val cardBackgroundFulfilledDark = Color(0xFF1A2332)
private val cardBackgroundApprovedDark = Color(0xFF1E2E20)
private val cardBackgroundRequestedRenewDark = Color(0xFF2E241A)
private val cardBackgroundPendingDark = Color(0xFF2E2A1A)

private fun getRoleBackground(
    role: String?,
    isDark: Boolean,
): Color =
    when (role?.uppercase()) {
        "ADMIN" -> if (isDark) cardBackgroundRejectedDark else cardBackgroundRejectedLight
        "FACULTY" -> if (isDark) cardBackgroundFulfilledDark else cardBackgroundFulfilledLight
        "STUDENT" -> if (isDark) cardBackgroundApprovedDark else cardBackgroundApprovedLight
        "TA" -> if (isDark) cardBackgroundRequestedRenewDark else cardBackgroundRequestedRenewLight
        "PENDING" -> if (isDark) cardBackgroundPendingDark else cardBackgroundPendingLight
        else -> if (isDark) Color(0xFF1F1F1F) else Color(0xFFFAFAFA)
    }

private val roleColors =
    mapOf(
        "ADMIN" to SemanticDanger,
        "FACULTY" to SemanticInfo,
        "STUDENT" to SemanticSuccess,
        "TA" to SemanticWarning,
        "PENDING" to SemanticNeutral,
    )

private val allRoles = listOf("ADMIN", "FACULTY", "STUDENT", "TA", "PENDING")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalCount by remember { mutableStateOf(0) }
    var currentOffset by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var editingUser by remember { mutableStateOf<User?>(null) }
    val pageSize = 50
    val scope = rememberCoroutineScope()

    fun loadUsers() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.token.first()
                if (token != null) {
                    val response =
                        ApiClient.userApiService.getUsers(
                            token = "Bearer $token",
                            limit = pageSize,
                            offset = currentOffset,
                            search = searchQuery.trim().ifBlank { null },
                        )
                    users = response.users
                    totalCount = response.pagination.total
                } else {
                    errorMessage = "No authentication token"
                }
            } catch (e: Throwable) {
                errorMessage =
                    when {
                        e is ResponseException && e.response.status == HttpStatusCode.Unauthorized ->
                            "Session expired. Please login again."

                        e.message?.contains(
                            "Network",
                        ) == true ||
                            e.message?.contains(
                                "timeout",
                            ) == true -> "Network error. Please check your connection."

                        else -> "Error: ${e.message ?: "Failed to load users"}"
                    }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentOffset, searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300)
        }
        loadUsers()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "User Management",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    currentOffset = 0
                },
                placeholder = "Search users...",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                            TextButton(onClick = { loadUsers() }) { Text("Retry") }
                        }
                    }
                }

                users.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No users found",
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
                        items(users, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                onClick = { editingUser = user },
                            )
                        }
                    }

                    UserPaginationBar(
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

    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            onSave = { name, role, batch, branch ->
                scope.launch {
                    try {
                        val token = tokenManager.token.first()
                        if (token != null) {
                            ApiClient.userApiService.updateUser(
                                token = "Bearer $token",
                                userId = user.id,
                                request =
                                    UpdateUserRequest(
                                        name = name.ifBlank { null },
                                        role = role,
                                        batch = batch.ifBlank { null },
                                        branch = branch.ifBlank { null },
                                    ),
                            )
                            editingUser = null
                            loadUsers()
                        }
                    } catch (e: Throwable) {
                        errorMessage = "Failed to update user: ${e.message}"
                    }
                }
            },
            onDismiss = { editingUser = null },
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val roleColor = roleColors[user.role.uppercase()] ?: MaterialTheme.colorScheme.primary
    val cardBackground = getRoleBackground(user.role, isDark)

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
                Text(
                    text = user.name ?: "No name",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = user.role,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = roleColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.batch != null || user.branch != null) {
                Spacer(Modifier.height(4.dp))
                Row {
                    if (user.batch != null) {
                        Text(
                            text = user.batch,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (user.batch != null && user.branch != null) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (user.branch != null) {
                        Text(
                            text = user.branch,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPaginationBar(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(
    user: User,
    onSave: (name: String, role: String, batch: String, branch: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(user.name ?: "") }
    var role by remember { mutableStateOf(user.role) }
    var batch by remember { mutableStateOf(user.batch ?: "") }
    var branch by remember { mutableStateOf(user.branch ?: "") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                    ) {
                        allRoles.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption) },
                                onClick = {
                                    role = roleOption
                                    roleDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("Batch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, role, batch, branch) },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

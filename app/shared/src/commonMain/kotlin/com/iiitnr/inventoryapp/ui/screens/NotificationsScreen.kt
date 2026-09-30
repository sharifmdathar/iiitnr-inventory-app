package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.AppNotification
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar
import com.iiitnr.inventoryapp.ui.components.common.EmptyState
import com.iiitnr.inventoryapp.ui.components.common.ErrorContent
import com.iiitnr.inventoryapp.ui.components.common.LoadingIndicator
import com.iiitnr.inventoryapp.utils.currentToday
import com.iiitnr.inventoryapp.utils.getRelativeDays
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onOpenRequest: (String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val today = currentToday

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notifications",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (viewModel.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text("Mark all read")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            viewModel.isLoading && viewModel.notifications.isEmpty() -> {
                LoadingIndicator(modifier = Modifier.fillMaxSize().padding(paddingValues))
            }

            viewModel.errorMessage != null && viewModel.notifications.isEmpty() -> {
                ErrorContent(
                    errorMessage = viewModel.errorMessage ?: "Something went wrong",
                    onRetry = { viewModel.loadNotifications() },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }

            viewModel.notifications.isEmpty() -> {
                EmptyState(
                    message = "No notifications yet",
                    subtitle = "You'll see updates here when your requests change status",
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(viewModel.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            today = today,
                            onClick = {
                                viewModel.markRead(notification.id)
                                notification.requestId?.let(onOpenRequest)
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    today: kotlinx.datetime.LocalDate,
    onClick: () -> Unit,
) {
    val background =
        if (!notification.read) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(
                    if (!notification.read) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (!notification.read) FontWeight.SemiBold else FontWeight.Normal,
            )
            notification.body?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getRelativeDays(notification.createdAt, today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

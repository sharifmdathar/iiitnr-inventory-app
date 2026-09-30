package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiitnr.inventoryapp.data.api.NotificationApiService
import com.iiitnr.inventoryapp.data.models.AppNotification
import com.iiitnr.inventoryapp.data.storage.TokenManager
import com.iiitnr.inventoryapp.utils.toAppError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NotificationsViewModel(
    private val tokenManager: TokenManager,
    private val notificationApiService: NotificationApiService,
) : ViewModel() {
    var notifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadNotifications()
        startNotificationStream()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = notificationApiService.getNotifications("Bearer $token")
                notifications = response.notifications
                unreadCount = response.unreadCount
                errorMessage = null
            } catch (e: Throwable) {
                errorMessage = e.toAppError().message
            } finally {
                isLoading = false
            }
        }
    }

    private fun startNotificationStream() {
        viewModelScope.launch {
            while (true) {
                try {
                    val token = tokenManager.token.first() ?: return@launch
                    notificationApiService.streamNotifications("Bearer $token").collect { notification ->
                        notifications = listOf(notification) + notifications
                        unreadCount++
                    }
                    delay(STREAM_RECONNECT_DELAY_MS.milliseconds)
                } catch (_: Exception) {
                    delay(STREAM_RECONNECT_DELAY_MS.milliseconds)
                }
            }
        }
    }

    fun markRead(notificationId: String) {
        val notification = notifications.firstOrNull { it.id == notificationId } ?: return
        if (notification.read) return

        notifications = notifications.map { if (it.id == notificationId) it.copy(read = true) else it }
        unreadCount = (unreadCount - 1).coerceAtLeast(0)

        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                notificationApiService.markRead("Bearer $token", notificationId)
            } catch (_: Exception) {
                loadNotifications()
            }
        }
    }

    fun markAllRead() {
        if (unreadCount == 0) return

        notifications = notifications.map { it.copy(read = true) }
        unreadCount = 0

        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                notificationApiService.markAllRead("Bearer $token")
            } catch (_: Exception) {
                loadNotifications()
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                unreadCount = notificationApiService.getUnreadCount("Bearer $token").unreadCount
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        const val STREAM_RECONNECT_DELAY_MS = 5_000L
    }
}

package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AuditLogEntry(
    val id: String,
    val userId: String? = null,
    val action: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val oldValues: String? = null,
    val newValues: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val metadata: String? = null,
    val createdAt: String,
    val userName: String? = null,
    val userEmail: String? = null,
)

@Serializable
data class AuditLogPagination(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
)

@Serializable
data class AuditLogsResponse(
    val logs: List<AuditLogEntry>,
    val pagination: AuditLogPagination,
)

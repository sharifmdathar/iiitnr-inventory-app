package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ISSUED,
    PARTIALLY_ISSUED,
    RETURNED,
    PARTIALLY_RETURNED,
    EXPIRED,
    RENEWED,
    REQUESTED_RENEW,
}

@Serializable
enum class UserRole {
    ADMIN,
    FACULTY,
    STUDENT,
    LA,
    PENDING,
    ;

    companion object {
        val ALL: List<UserRole> = entries.toList()
    }
}

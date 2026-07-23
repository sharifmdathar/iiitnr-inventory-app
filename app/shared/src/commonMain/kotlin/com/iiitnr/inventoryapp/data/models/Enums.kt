package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ISSUED,
    RETURNED,
    EXPIRED,
    RENEWED,
    REQUESTED_RENEW,
    ;

    companion object {
        val FILTER_OPTIONS: List<RequestStatus> = entries
    }
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
        val ALL: List<UserRole> = entries
    }
}

package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RequestItem(
    val id: String,
    val requestId: String? = null,
    val componentId: String? = null,
    val quantity: Int,
    val component: Component? = null
)

@Serializable
data class Request(
    val id: String,
    val userId: String,
    val targetFacultyId: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val items: List<RequestItem> = emptyList(),
    val user: User? = null,
    val targetFaculty: User? = null
)

@Serializable
data class RequestItemPayload(
    val componentId: String,
    val quantity: Int
)

@Serializable
data class CreateRequestPayload(
    val items: List<RequestItemPayload>,
    val targetFacultyId: String? = null
)

@Serializable
data class FacultyResponse(
    val faculty: List<User>
)

@Serializable
data class RequestResponse(
    val request: Request
)

@Serializable
data class RequestsResponse(
    val requests: List<Request>
)

@Serializable
data class UpdateRequestStatusPayload(
    val status: String
)

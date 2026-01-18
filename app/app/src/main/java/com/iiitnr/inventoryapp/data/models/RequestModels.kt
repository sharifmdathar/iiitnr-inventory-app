package com.iiitnr.inventoryapp.data.models

data class RequestItem(
    val id: String,
    val requestId: String? = null,
    val componentId: String? = null,
    val quantity: Int,
    val component: Component? = null
)

data class Request(
    val id: String,
    val userId: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val items: List<RequestItem> = emptyList(),
    val user: User? = null
)

data class RequestItemPayload(
    val componentId: String,
    val quantity: Int
)

data class CreateRequestPayload(
    val items: List<RequestItemPayload>
)

data class RequestResponse(
    val request: Request
)

data class RequestsResponse(
    val requests: List<Request>
)

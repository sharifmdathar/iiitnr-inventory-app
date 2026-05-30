package com.iiitnr.inventoryapp.data.api

import com.iiitnr.inventoryapp.data.models.UpdateUserRequest
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserResponse
import com.iiitnr.inventoryapp.data.models.UsersResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class UserApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getUsers(
        token: String,
        limit: Int = 50,
        offset: Int = 0,
        search: String? = null,
    ): UsersResponse =
        client
            .get("$baseUrl/admin/users") {
                headers {
                    append(HttpHeaders.Authorization, token)
                }
                parameter("limit", limit)
                parameter("offset", offset)
                if (!search.isNullOrBlank()) parameter("search", search)
            }.body()

    suspend fun updateUser(
        token: String,
        userId: String,
        request: UpdateUserRequest,
    ): User =
        client
            .patch("$baseUrl/admin/users/$userId") {
                headers {
                    append(HttpHeaders.Authorization, token)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<UserResponse>()
            .user
}

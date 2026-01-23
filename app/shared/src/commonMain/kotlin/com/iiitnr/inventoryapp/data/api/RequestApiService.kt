package com.iiitnr.inventoryapp.data.api

import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.FacultyResponse
import com.iiitnr.inventoryapp.data.models.RequestResponse
import com.iiitnr.inventoryapp.data.models.RequestsResponse
import com.iiitnr.inventoryapp.data.models.UpdateRequestStatusPayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class RequestApiService(private val client: HttpClient, private val baseUrl: String) {
    suspend fun createRequest(token: String, payload: CreateRequestPayload): RequestResponse {
        return client.post("$baseUrl/requests") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun getRequests(token: String, status: String? = null): RequestsResponse {
        return client.get("$baseUrl/requests") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
            if (status != null) {
                parameter("status", status)
            }
        }.body()
    }

    suspend fun updateRequestStatus(
        token: String, id: String, payload: UpdateRequestStatusPayload
    ): RequestResponse {
        return client.put("$baseUrl/requests/$id") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun deleteRequest(token: String, id: String) {
        client.delete("$baseUrl/requests/$id") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
        }
    }

    suspend fun getFaculty(token: String): FacultyResponse {
        return client.get("$baseUrl/faculty") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
        }.body()
    }
}

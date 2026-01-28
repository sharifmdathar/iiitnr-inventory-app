package com.iiitnr.inventoryapp.data.api

import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.models.ComponentResponse
import com.iiitnr.inventoryapp.data.models.ComponentsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class ComponentApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getComponents(token: String): ComponentsResponse =
        client
            .get("$baseUrl/components") {
                headers {
                    append(HttpHeaders.Authorization, token)
                }
            }.body()

    suspend fun createComponent(
        token: String,
        request: ComponentRequest,
    ): ComponentResponse =
        client
            .post("$baseUrl/components") {
                headers {
                    append(HttpHeaders.Authorization, token)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

    suspend fun updateComponent(
        token: String,
        id: String,
        request: ComponentRequest,
    ): ComponentResponse =
        client
            .put("$baseUrl/components/$id") {
                headers {
                    append(HttpHeaders.Authorization, token)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

    suspend fun deleteComponent(
        token: String,
        id: String,
    ) {
        client.delete("$baseUrl/components/$id") {
            headers {
                append(HttpHeaders.Authorization, token)
            }
        }
    }
}

package com.iiitnr.inventoryapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val version: String,
)

class VersionApiService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun fetchServerVersion(): VersionResponse = client.get("$baseUrl/version").body()
}
